/*
 * Copyright 2022 Dynatrace LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.dynatrace.hash4j.distinctcount;

import static java.util.stream.Collectors.toList;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.SplittableRandom;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;

public class HyperLogLogEstimationErrorSimulation {

  public static void main(String[] args) {
    int minP = 3;
    int maxP = 16;
    int numCycles = 100000;
    String resultFolder = "test-results/";

    int[] pVals = IntStream.range(minP, maxP + 1).toArray();
    SplittableRandom seedRandom = new SplittableRandom(0x891ea7f506edfc35L);
    long[] seeds = seedRandom.longs(numCycles).toArray();
    long[] trueDistinctCounts = TestUtils.getDistinctCountValues(0L, 1L << 24, 0.05);
    double[][][] estimatedDistinctCounts = new double[pVals.length][][];
    for (int pIdx = 0; pIdx < pVals.length; ++pIdx) {
      estimatedDistinctCounts[pIdx] = new double[trueDistinctCounts.length][];
      for (int i = 0; i < trueDistinctCounts.length; ++i) {
        estimatedDistinctCounts[pIdx][i] = new double[numCycles];
      }
    }
    ThreadLocal<List<HyperLogLog>> sketches =
        ThreadLocal.withInitial(
            () -> IntStream.of(pVals).mapToObj(HyperLogLog::create).collect(toList()));

    IntStream.range(0, numCycles)
        .parallel()
        .forEach(
            i -> {
              SplittableRandom random = new SplittableRandom(seeds[i]);
              List<HyperLogLog> sketchesRef = sketches.get();
              HyperLogLog sketch = sketchesRef.get(sketchesRef.size() - 1).reset();
              long trueDistinctCount = 0;
              int distinctCountIndex = 0;
              while (distinctCountIndex < trueDistinctCounts.length) {
                if (trueDistinctCount == trueDistinctCounts[distinctCountIndex]) {
                  int pIdx = pVals.length - 1;
                  estimatedDistinctCounts[pIdx][distinctCountIndex][i] =
                      sketch.getDistinctCountEstimate();
                  HyperLogLog sketchCopy = sketch;
                  while (pIdx > 0) {
                    pIdx -= 1;
                    sketchCopy = sketchesRef.get(pIdx).reset().add(sketchCopy);
                    estimatedDistinctCounts[pIdx][distinctCountIndex][i] =
                        sketchCopy.getDistinctCountEstimate();
                  }
                  distinctCountIndex += 1;
                }
                sketch.add(random.nextLong());
                trueDistinctCount += 1;
              }
            });
    for (int pIdx = 0; pIdx < pVals.length; ++pIdx) {
      int p = pVals[pIdx];
      String fileName = resultFolder + "hyperloglog-estimation-error-p" + p + ".csv";
      double theoreticalRelativeStandardError =
          HyperLogLog.calculateTheoreticalRelativeStandardError(p);
      try (FileWriter writer = new FileWriter(fileName)) {
        writer.write("sketch_name=hyperloglog; p=" + p + "; num_cycles=" + numCycles + "\n");
        writer.write(
            "distinct count; relative bias; relative rmse; theoretical relative standard error\n");
        for (int distinctCountIndex = 0;
            distinctCountIndex < trueDistinctCounts.length;
            ++distinctCountIndex) {
          double[] estimates = estimatedDistinctCounts[pIdx][distinctCountIndex];
          double trueDistinctCount = trueDistinctCounts[distinctCountIndex];
          double relativeBias =
              DoubleStream.of(estimates).map(d -> d - trueDistinctCount).sum()
                  / numCycles
                  / trueDistinctCount;
          double relativeRmse =
              Math.sqrt(
                      DoubleStream.of(estimates)
                              .map(d -> (d - trueDistinctCount) * (d - trueDistinctCount))
                              .sum()
                          / numCycles)
                  / trueDistinctCount;

          writer.write(
              trueDistinctCount
                  + ";"
                  + relativeBias
                  + ";"
                  + relativeRmse
                  + ";"
                  + theoreticalRelativeStandardError
                  + "\n");
        }
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }
}
