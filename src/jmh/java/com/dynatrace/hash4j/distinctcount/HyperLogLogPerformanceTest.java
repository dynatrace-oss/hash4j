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

import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.SplittableRandom;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

public class HyperLogLogPerformanceTest {

  private static HyperLogLog generate(SplittableRandom random, long numElements, int precision) {
    HyperLogLog sketch = HyperLogLog.create(precision);
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
    final HyperLogLog sketch = HyperLogLog.create(addState.precision);
    for (long i = 0; i < addState.numElements; ++i) {
      sketch.add(addState.random.nextLong());
    }
    blackhole.consume(sketch);
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  public void distinctCountAddWithMartingaleEstimator(AddState addState, Blackhole blackhole) {
    final HyperLogLog sketch = HyperLogLog.create(addState.precision);
    final MartingaleEstimator martingaleEstimator = new MartingaleEstimator();
    for (long i = 0; i < addState.numElements; ++i) {
      sketch.add(addState.random.nextLong(), martingaleEstimator);
    }
    blackhole.consume(martingaleEstimator.getDistinctCountEstimate());
  }

  public enum Estimator {
    MAXIMUM_LIKELIHOOD_ESTIMATOR(HyperLogLog.MAXIMUM_LIKELIHOOD_ESTIMATOR),
    CORRECTED_RAW_ESTIMATOR(HyperLogLog.CORRECTED_RAW_ESTIMATOR);

    private final HyperLogLog.Estimator estimator;

    Estimator(HyperLogLog.Estimator estimator) {
      this.estimator = estimator;
    }
  }

  @State(Scope.Benchmark)
  public static class EstimationState {

    HyperLogLog[] sketches = null;

    @Param({"1", "10", "100", "1000", "10000", "100000", "1000000"})
    public int numElements;

    @Param({"6", "8", "10", "12", "14"})
    public int precision;

    @Param public Estimator estimator;

    @Param({"1000"})
    public int numExamples;

    @Setup(Level.Trial)
    public void init() {
      SplittableRandom random = new SplittableRandom(ThreadLocalRandom.current().nextLong());
      sketches =
          Stream.generate(() -> generate(random, numElements, precision))
              .limit(numExamples)
              .toArray(i -> new HyperLogLog[i]);
    }

    @TearDown(Level.Trial)
    public void finish() {
      sketches = null;
    }
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  public void distinctCountEstimation(EstimationState estimationState, Blackhole blackhole) {
    HyperLogLog.Estimator estimator = estimationState.estimator.estimator;
    for (int i = 0; i < estimationState.sketches.length; ++i) {
      double estimate = estimator.estimate(estimationState.sketches[i]);
      blackhole.consume(estimate);
    }
  }

  @State(Scope.Benchmark)
  public static class MergeState {

    List<HyperLogLog> sketches1 = null;
    List<HyperLogLog> sketches2 = null;

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
      SplittableRandom random = new SplittableRandom(ThreadLocalRandom.current().nextLong());
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
      HyperLogLog mergedSketch =
          HyperLogLog.merge(mergeState.sketches1.get(i), mergeState.sketches2.get(i));
      blackhole.consume(mergedSketch);
    }
  }
}
