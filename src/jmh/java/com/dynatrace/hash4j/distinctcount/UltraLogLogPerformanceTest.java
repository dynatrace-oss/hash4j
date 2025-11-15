/*
 * Copyright 2022-2025 Dynatrace LLC
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

import java.util.List;
import java.util.SplittableRandom;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.infra.Blackhole;

public class UltraLogLogPerformanceTest {

  private static UltraLogLog generate(SplittableRandom random, long numElements, int precision) {
    UltraLogLog sketch = UltraLogLog.create(precision);
    random.longs(numElements).forEach(sketch::add);
    return sketch;
  }

  @State(Scope.Thread)
  public static class AddState {

    @Param({"1", "10", "100", "1000", "10000", "100000", "1000000"})
    public int numElements;

    @Param({"6", "8", "10", "12", "14"})
    public int precision;

    public SplittableRandom random;

    @Setup(Level.Trial)
    public void init() {
      random = new SplittableRandom();
    }
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  public void distinctCountAdd(AddState addState, Blackhole blackhole) {
    final UltraLogLog sketch = UltraLogLog.create(addState.precision);
    for (long i = 0; i < addState.numElements; ++i) {
      sketch.add(addState.random.nextLong());
    }
    blackhole.consume(sketch);
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  public void distinctCountAddWithMartingaleEstimator(AddState addState, Blackhole blackhole) {
    final UltraLogLog sketch = UltraLogLog.create(addState.precision);
    final MartingaleEstimator martingaleEstimator = new MartingaleEstimator();
    for (long i = 0; i < addState.numElements; ++i) {
      sketch.add(addState.random.nextLong(), martingaleEstimator);
    }
    blackhole.consume(martingaleEstimator.getDistinctCountEstimate());
  }

  public enum Estimator {
    MAXIMUM_LIKELIHOOD_ESTIMATOR(UltraLogLog.MAXIMUM_LIKELIHOOD_ESTIMATOR),
    OPTIMAL_FGRA_ESTIMATOR(UltraLogLog.OPTIMAL_FGRA_ESTIMATOR);

    @SuppressWarnings("ImmutableEnumChecker")
    private final UltraLogLog.Estimator estimator;

    Estimator(UltraLogLog.Estimator estimator) {
      this.estimator = estimator;
    }
  }

  @State(Scope.Benchmark)
  public static class EstimationState {

    UltraLogLog[] sketches = null;

    @Param({"1", "10", "100", "1000", "10000", "100000", "1000000"})
    public int numElements;

    @Param({"6", "8", "10", "12", "14"})
    public int precision;

    @Param public Estimator estimator;

    @Param({"1000"})
    public int numExamples;

    @Setup(Level.Trial)
    public void init() {
      SplittableRandom random = new SplittableRandom();
      sketches =
          Stream.generate(() -> generate(random, numElements, precision))
              .limit(numExamples)
              .toArray(i -> new UltraLogLog[i]);
    }

    @TearDown(Level.Trial)
    public void finish() {
      sketches = null;
    }
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  public void distinctCountEstimation(EstimationState estimationState, Blackhole blackhole) {
    UltraLogLog.Estimator estimator = estimationState.estimator.estimator;
    for (int i = 0; i < estimationState.sketches.length; ++i) {
      double estimate = estimator.estimate(estimationState.sketches[i]);
      blackhole.consume(estimate);
    }
  }

  @State(Scope.Benchmark)
  public static class EstimationStateMixed {

    UltraLogLog[] sketches = null;

    @Param({"6", "8", "10", "12", "14"})
    public int precision;

    @Param public Estimator estimator;

    @Param({"1000"})
    public int numExamples;

    @Setup(Level.Trial)
    public void init() {
      SplittableRandom random = new SplittableRandom();
      sketches =
          IntStream.range(0, numExamples)
              .mapToObj(i -> generate(random, 1L << (i % 25), precision))
              .toArray(i -> new UltraLogLog[i]);
    }

    @TearDown(Level.Trial)
    public void finish() {
      sketches = null;
    }
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  public void distinctCountEstimationMixed(
      EstimationStateMixed estimationState, Blackhole blackhole) {
    UltraLogLog.Estimator estimator = estimationState.estimator.estimator;
    for (int i = 0; i < estimationState.sketches.length; ++i) {
      double estimate = estimator.estimate(estimationState.sketches[i]);
      blackhole.consume(estimate);
    }
  }

  @State(Scope.Benchmark)
  public static class MergeState {

    List<UltraLogLog> sketches1 = null;
    List<UltraLogLog> sketches2 = null;

    @Param({"30000"})
    public int numElements1;

    @Param({"50000"})
    public int numElements2;

    @Param({"10", "14"})
    public int precision1;

    @Param({"10", "14"})
    public int precision2;

    @Param({"1000"})
    public int numExamples;

    @Setup(Level.Trial)
    public void init() {
      SplittableRandom random = new SplittableRandom();
      sketches1 =
          Stream.generate(() -> generate(random, numElements1, precision1))
              .limit(numExamples)
              .collect(toList());
      sketches2 =
          Stream.generate(() -> generate(random, numElements2, precision2))
              .limit(numExamples)
              .collect(toList());
    }

    @TearDown
    public void finish() {
      sketches1 = null;
      sketches2 = null;
    }
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  public void distinctCountMerge(MergeState mergeState, Blackhole blackhole) {
    for (int i = 0; i < mergeState.numExamples; ++i) {
      UltraLogLog mergedSketch =
          UltraLogLog.merge(mergeState.sketches1.get(i), mergeState.sketches2.get(i));
      blackhole.consume(mergedSketch);
    }
  }
}
