/*
 * Copyright 2022-2023 Dynatrace LLC
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

import java.io.FileWriter;
import java.io.IOException;
import java.util.SplittableRandom;
import java.util.function.IntFunction;
import java.util.function.IntToDoubleFunction;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;

public final class EstimationErrorSimulationUtil {

  private EstimationErrorSimulationUtil() {}

  public static <T extends DistinctCounter<T>> void doSimulation(
      int p,
      String sketchName,
      IntFunction<T> supplier,
      IntToDoubleFunction theoreticalRelativeStandardErrorDefaultEstimatorCalculator,
      IntToDoubleFunction theoreticalRelativeStandardErrorMartingaleEstimatorCalculator) {
    int numCycles = 100000;
    String resultFolder = "test-results/";

    SplittableRandom seedRandom = new SplittableRandom(0x891ea7f506edfc35L);

    long[] seeds = seedRandom.longs(numCycles).toArray();
    long[] trueDistinctCounts = TestUtils.getDistinctCountValues(0L, 1L << 24, 0.05);

    double[][] estimatedDistinctCountsDefault = new double[trueDistinctCounts.length][];
    double[][] estimatedDistinctCountsMartingale = new double[trueDistinctCounts.length][];
    for (int i = 0; i < trueDistinctCounts.length; ++i) {
      estimatedDistinctCountsDefault[i] = new double[numCycles];
      estimatedDistinctCountsMartingale[i] = new double[numCycles];
    }

    ThreadLocal<T> sketches = ThreadLocal.withInitial(() -> supplier.apply(p));

    IntStream.range(0, numCycles)
        .parallel()
        .forEach(
            i -> {
              SplittableRandom random = new SplittableRandom(seeds[i]);
              T sketch = sketches.get().reset();
              MartingaleEstimator martingaleEstimator = new MartingaleEstimator();
              long trueDistinctCount = 0;
              int distinctCountIndex = 0;
              while (distinctCountIndex < trueDistinctCounts.length) {
                if (trueDistinctCount == trueDistinctCounts[distinctCountIndex]) {
                  estimatedDistinctCountsDefault[distinctCountIndex][i] =
                      sketch.getDistinctCountEstimate();
                  estimatedDistinctCountsMartingale[distinctCountIndex][i] =
                      martingaleEstimator.getDistinctCountEstimate();
                  distinctCountIndex += 1;
                }
                sketch.add(random.nextLong(), martingaleEstimator);
                trueDistinctCount += 1;
              }
            });
    String fileName = resultFolder + sketchName + "-estimation-error-p" + p + ".csv";
    double theoreticalRelativeStandardErrorDefault =
        theoreticalRelativeStandardErrorDefaultEstimatorCalculator.applyAsDouble(p);
    double theoreticalRelativeStandardErrorMartingale =
        theoreticalRelativeStandardErrorMartingaleEstimatorCalculator.applyAsDouble(p);
    try (FileWriter writer = new FileWriter(fileName)) {
      writer.write("sketch_name=" + sketchName + "; p=" + p + "; num_cycles=" + numCycles + "\n");
      writer.write(
          "distinct count; relative bias default; relative rmse default; theoretical relative standard error default; relative bias martingale; relative rmse martingale; theoretical relative standard error martingale\n");
      for (int distinctCountIndex = 0;
          distinctCountIndex < trueDistinctCounts.length;
          ++distinctCountIndex) {
        double[] estimatesDefault = estimatedDistinctCountsDefault[distinctCountIndex];
        double[] estimatesMartingale = estimatedDistinctCountsMartingale[distinctCountIndex];
        double trueDistinctCount = trueDistinctCounts[distinctCountIndex];
        double relativeBiasDefault =
            DoubleStream.of(estimatesDefault).map(d -> d - trueDistinctCount).sum()
                / numCycles
                / trueDistinctCount;
        double relativeBiasMartingale =
            DoubleStream.of(estimatesMartingale).map(d -> d - trueDistinctCount).sum()
                / numCycles
                / trueDistinctCount;
        double relativeRmseDefault =
            Math.sqrt(
                    DoubleStream.of(estimatesDefault)
                            .map(d -> (d - trueDistinctCount) * (d - trueDistinctCount))
                            .sum()
                        / numCycles)
                / trueDistinctCount;
        double relativeRmseMartingale =
            Math.sqrt(
                    DoubleStream.of(estimatesMartingale)
                            .map(d -> (d - trueDistinctCount) * (d - trueDistinctCount))
                            .sum()
                        / numCycles)
                / trueDistinctCount;

        writer.write(
            trueDistinctCount
                + ";"
                + relativeBiasDefault
                + ";"
                + relativeRmseDefault
                + ";"
                + theoreticalRelativeStandardErrorDefault
                + ";"
                + relativeBiasMartingale
                + ";"
                + relativeRmseMartingale
                + ";"
                + theoreticalRelativeStandardErrorMartingale
                + "\n");
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
