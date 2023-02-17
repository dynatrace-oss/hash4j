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

import static java.util.Comparator.comparing;

import com.dynatrace.hash4j.random.PseudoRandomGenerator;
import com.dynatrace.hash4j.random.PseudoRandomGeneratorProvider;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
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

  private static final class Transition {
    private final BigInt distinctCount;
    private final long hash;

    public Transition(BigInt distinctCount, long hash) {
      this.distinctCount = distinctCount;
      this.hash = hash;
    }
  }

  private static final class LocalState<T> {
    private final T sketch;
    private final Transition[] transitions;
    private final int p;
    private final PseudoRandomGenerator prg;

    public LocalState(T sketch, int p, PseudoRandomGenerator prg) {
      int transitionListLen = (1 << p) * (65 - p);
      this.sketch = sketch;
      this.transitions = new Transition[transitionListLen];
      this.p = p;
      this.prg = prg;
    }

    public void generateTransitions(BigInt distinctCountOffset) {
      int counter = 0;
      for (int nlz = 0; nlz <= 64 - p; ++nlz) {
        double factor = Math.pow(2., Math.min(64, 1 + p + nlz));
        for (int registerIdx = 0; registerIdx < (1 << p); ++registerIdx) {
          BigInt transitionDistinctCount = BigInt.floor(prg.nextExponential() * factor);
          transitionDistinctCount.increment(); // 1-based geometric distribution
          transitionDistinctCount.add(distinctCountOffset);
          long hash = DistinctCounterTest.createUpdateValue(p, registerIdx, nlz);
          transitions[counter++] = new Transition(transitionDistinctCount, hash);
        }
      }
      Arrays.sort(transitions, comparing(transition -> transition.distinctCount));
    }
  }

  public static <T extends DistinctCounter<T, R>, R extends DistinctCounter.Estimator<T>>
      void doSimulation(
          int p,
          String sketchName,
          IntFunction<T> supplier,
          List<EstimatorConfig<T>> estimatorConfigs,
          String outputFile) {

    // parameters
    int numCycles = 100000;
    final BigInt largeScaleSimulationModeDistinctCountLimit = BigInt.fromLong(1000000);
    List<BigInt> targetDistinctCounts = TestUtils.getDistinctCountValues(1e21, 0.05);

    SplittableRandom seedRandom = new SplittableRandom(0x891ea7f506edfc35L);
    long[] seeds = seedRandom.longs(numCycles).toArray();

    double[][][] estimatedDistinctCounts = new double[estimatorConfigs.size() + 1][][];
    for (int k = 0; k < estimatorConfigs.size() + 1; ++k) {
      estimatedDistinctCounts[k] = new double[targetDistinctCounts.size()][];
      for (int i = 0; i < targetDistinctCounts.size(); ++i) {
        estimatedDistinctCounts[k][i] = new double[numCycles];
      }
    }

    PseudoRandomGeneratorProvider prgProvider = PseudoRandomGeneratorProvider.splitMix64_V1();
    ThreadLocal<LocalState<T>> localStates =
        ThreadLocal.withInitial(() -> new LocalState<>(supplier.apply(p), p, prgProvider.create()));

    IntStream.range(0, numCycles)
        .parallel()
        .forEach(
            i -> {
              LocalState<T> state = localStates.get();
              final PseudoRandomGenerator prg = state.prg;
              prg.reset(seeds[i]);
              final T sketch = state.sketch;
              sketch.reset();
              MartingaleEstimator martingaleEstimator = new MartingaleEstimator();
              final Transition[] transitions = state.transitions;
              state.generateTransitions(largeScaleSimulationModeDistinctCountLimit);

              BigInt trueDistinctCount = BigInt.createZero();
              int transitionIndex = 0;
              for (int distinctCountIndex = 0;
                  distinctCountIndex < targetDistinctCounts.size();
                  ++distinctCountIndex) {
                BigInt targetDistinctCount = targetDistinctCounts.get(distinctCountIndex);
                BigInt limit = targetDistinctCount.copy();
                limit.min(largeScaleSimulationModeDistinctCountLimit);

                while (trueDistinctCount.compareTo(limit) < 0) {
                  sketch.add(prg.nextLong(), martingaleEstimator);
                  trueDistinctCount.increment();
                }
                if (trueDistinctCount.compareTo(targetDistinctCount) < 0) {
                  while (transitionIndex < transitions.length
                      && transitions[transitionIndex].distinctCount.compareTo(targetDistinctCount)
                          < 0) {
                    sketch.add(transitions[transitionIndex].hash, martingaleEstimator);
                    transitionIndex += 1;
                  }
                  trueDistinctCount.set(targetDistinctCount);
                }

                for (int k = 0; k < estimatorConfigs.size(); ++k) {
                  estimatedDistinctCounts[k][distinctCountIndex][i] =
                      estimatorConfigs.get(k).estimator.applyAsDouble(sketch, martingaleEstimator);
                }
              }
            });

    double[] theoreticalRelativeStandardErrors =
        estimatorConfigs.stream()
            .mapToDouble(c -> c.getpToAsymptoticRelativeStandardError().applyAsDouble(p))
            .toArray();

    try (FileWriter writer = new FileWriter(outputFile)) {
      writer.write(
          "sketch_name="
              + sketchName
              + "; p="
              + p
              + "; num_cycles="
              + numCycles
              + "; large_scale_simulation_mode_distinct_count_limit="
              + largeScaleSimulationModeDistinctCountLimit
              + "\n");
      writer.write("distinct count");

      for (EstimatorConfig<T> estimatorConfig : estimatorConfigs) {
        writer.write("; relative bias " + estimatorConfig.getLabel());
        writer.write("; relative rmse " + estimatorConfig.getLabel());
        writer.write("; theoretical relative standard error " + estimatorConfig.getLabel());
      }
      writer.write('\n');

      for (int distinctCountIndex = 0;
          distinctCountIndex < targetDistinctCounts.size();
          ++distinctCountIndex) {

        double trueDistinctCount = targetDistinctCounts.get(distinctCountIndex).asDouble();
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
