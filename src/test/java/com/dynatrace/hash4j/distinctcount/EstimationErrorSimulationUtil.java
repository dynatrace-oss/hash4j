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
import java.util.List;
import java.util.SplittableRandom;
import java.util.function.IntFunction;
import java.util.function.IntToDoubleFunction;
import java.util.function.ToDoubleBiFunction;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;

public final class EstimationErrorSimulationUtil {

  private EstimationErrorSimulationUtil() {}

  public static final class EstimatorConfig<T> {
    private final ToDoubleBiFunction<T, MartingaleEstimator> estimator;
    private final String label;

    private final IntToDoubleFunction pToAsymptoticRelativeStandardError;

    public EstimatorConfig(
        ToDoubleBiFunction<T, MartingaleEstimator> estimator,
        String label,
        IntToDoubleFunction pToAsymptoticRelativeStandardError) {
      this.estimator = estimator;
      this.label = label;
      this.pToAsymptoticRelativeStandardError = pToAsymptoticRelativeStandardError;
    }

    public String getLabel() {
      return label;
    }

    public IntToDoubleFunction getpToAsymptoticRelativeStandardError() {
      return pToAsymptoticRelativeStandardError;
    }
  }

  public static <T extends DistinctCounter<T, R>, R extends DistinctCounter.Estimator<T>>
      void doSimulation(
          int p,
          String sketchName,
          IntFunction<T> supplier,
          List<EstimatorConfig<T>> estimatorConfigs,
          String outputFile) {
    int numCycles = 100000;

    SplittableRandom seedRandom = new SplittableRandom(0x891ea7f506edfc35L);

    long[] seeds = seedRandom.longs(numCycles).toArray();
    long[] trueDistinctCounts = TestUtils.getDistinctCountValues(0L, 1L << 24, 0.05);

    double[][][] estimatedDistinctCounts = new double[estimatorConfigs.size() + 1][][];

    for (int k = 0; k < estimatorConfigs.size() + 1; ++k) {
      estimatedDistinctCounts[k] = new double[trueDistinctCounts.length][];
      for (int i = 0; i < trueDistinctCounts.length; ++i) {
        estimatedDistinctCounts[k][i] = new double[numCycles];
      }
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
                  for (int k = 0; k < estimatorConfigs.size(); ++k) {
                    estimatedDistinctCounts[k][distinctCountIndex][i] =
                        estimatorConfigs
                            .get(k)
                            .estimator
                            .applyAsDouble(sketch, martingaleEstimator);
                  }
                  distinctCountIndex += 1;
                }
                sketch.add(random.nextLong(), martingaleEstimator);
                trueDistinctCount += 1;
              }
            });
    double[] theoreticalRelativeStandardErrors =
        estimatorConfigs.stream()
            .mapToDouble(c -> c.getpToAsymptoticRelativeStandardError().applyAsDouble(p))
            .toArray();

    try (FileWriter writer = new FileWriter(outputFile)) {
      writer.write("sketch_name=" + sketchName + "; p=" + p + "; num_cycles=" + numCycles + "\n");
      writer.write("distinct count");

      for (EstimatorConfig<T> estimatorConfig : estimatorConfigs) {
        writer.write("; relative bias " + estimatorConfig.getLabel());
        writer.write("; relative rmse " + estimatorConfig.getLabel());
        writer.write("; theoretical relative standard error " + estimatorConfig.getLabel());
      }
      writer.write('\n');

      for (int distinctCountIndex = 0;
          distinctCountIndex < trueDistinctCounts.length;
          ++distinctCountIndex) {

        double trueDistinctCount = trueDistinctCounts[distinctCountIndex];
        writer.write("" + trueDistinctCount);

        for (int k = 0; k < estimatorConfigs.size(); ++k) {

          double relativeBias =
              DoubleStream.of(estimatedDistinctCounts[k][distinctCountIndex])
                      .map(d -> d - trueDistinctCount)
                      .sum()
                  / numCycles
                  / trueDistinctCount;
          double relativeRmse =
              Math.sqrt(
                      DoubleStream.of(estimatedDistinctCounts[k][distinctCountIndex])
                              .map(d -> (d - trueDistinctCount) * (d - trueDistinctCount))
                              .sum()
                          / numCycles)
                  / trueDistinctCount;

          writer.write("; " + relativeBias);
          writer.write("; " + relativeRmse);
          writer.write("; " + theoreticalRelativeStandardErrors[k]);
        }
        writer.write('\n');
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
