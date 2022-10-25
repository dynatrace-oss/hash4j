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

import java.util.List;
import java.util.SplittableRandom;
import java.util.stream.Stream;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

public class HyperLogLogPerformanceTest {

  private static HyperLogLog generate(SplittableRandom random, long numElements, int precision) {
    HyperLogLog hyperLogLog = HyperLogLog.create(precision);
    random.longs(numElements).forEach(hyperLogLog::add);
    return hyperLogLog;
  }

  @State(Scope.Thread)
  public static class AddState {

    @Param({"1", "10", "100", "1000", "10000", "100000", "1000000"})
    public int numElements;

    // @Param({"8", "10", "12", "14"})
    @Param({"10", "14"})
    public int precision;
  }

  @State(Scope.Thread)
  public static class RandomState {
    public final SplittableRandom random = new SplittableRandom();
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  public void distinctCountAdd(AddState addState, RandomState randomState, Blackhole blackhole) {
    final HyperLogLog hyperLogLog = HyperLogLog.create(addState.precision);
    for (int i = 0; i < addState.numElements; ++i) {
      hyperLogLog.add(randomState.random.nextLong());
    }
    byte[] hyperLogLogState = hyperLogLog.getState();
    blackhole.consume(hyperLogLogState[randomState.random.nextInt(hyperLogLogState.length)]);
  }

  @State(Scope.Benchmark)
  public static class EstimationState {

    private static final int NUM_EXAMPLES = 1000;

    List<HyperLogLog> hyperLogLogs = null;

    @Param({"1", "10", "100", "1000", "10000", "100000", "1000000"})
    public int numElements;

    // @Param({"8", "10", "12", "14"})
    @Param({"10", "14"})
    public int precision;

    @Setup
    public void init() {
      SplittableRandom random = new SplittableRandom(0xdf12f8a7a8569e6aL);
      hyperLogLogs =
          Stream.generate(() -> generate(random, numElements, precision))
              .limit(NUM_EXAMPLES)
              .collect(toList());
    }

    @TearDown
    public void finish() {
      hyperLogLogs = null;
    }
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  public void distinctCountEstimation(EstimationState estimationState, Blackhole blackhole) {
    double sum = 0;
    for (HyperLogLog HyperLogLog : estimationState.hyperLogLogs) {
      sum += HyperLogLog.getDistinctCountEstimate();
    }
    blackhole.consume(sum);
  }

  @State(Scope.Benchmark)
  public static class MergeState {

    static final int NUM_EXAMPLES = 1000;

    List<HyperLogLog> hyperLogLogs1 = null;
    List<HyperLogLog> hyperLogLogs2 = null;

    @Param({"30000"})
    public int numElements1;

    @Param({"50000"})
    public int numElements2;

    @Param({"10", "14"})
    public int precision1;

    @Param({"10", "14"})
    public int precision2;

    @Setup
    public void init() {
      SplittableRandom random = new SplittableRandom(0x247bb0c84ca4bf78L);
      hyperLogLogs1 =
          Stream.generate(() -> generate(random, numElements1, precision1))
              .limit(NUM_EXAMPLES)
              .collect(toList());
      hyperLogLogs2 =
          Stream.generate(() -> generate(random, numElements2, precision2))
              .limit(NUM_EXAMPLES)
              .collect(toList());
    }

    @TearDown
    public void finish() {
      hyperLogLogs1 = null;
      hyperLogLogs2 = null;
    }
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  public void distinctCountMerge(
      MergeState mergeState, RandomState randomState, Blackhole blackhole) {
    long sum = 0;
    for (int i = 0; i < mergeState.NUM_EXAMPLES; ++i) {
      HyperLogLog mergedHyperLogLog =
          HyperLogLog.merge(mergeState.hyperLogLogs1.get(i), mergeState.hyperLogLogs2.get(i));
      sum +=
          mergedHyperLogLog
              .getState()[randomState.random.nextInt(mergedHyperLogLog.getState().length)];
    }
    blackhole.consume(sum);
  }
}
