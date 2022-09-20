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

public class UltraLogLogPerformanceTest {

  private static UltraLogLog generate(SplittableRandom random, long numElements, int precision) {
    UltraLogLog ultraLogLog = UltraLogLog.create(precision);
    random.longs(numElements).forEach(ultraLogLog::add);
    return ultraLogLog;
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
    final UltraLogLog ultraLogLog = UltraLogLog.create(addState.precision);
    for (int i = 0; i < addState.numElements; ++i) {
      ultraLogLog.add(randomState.random.nextLong());
    }
    byte[] ultraLogLogState = ultraLogLog.getState();
    blackhole.consume(ultraLogLogState[randomState.random.nextInt(ultraLogLogState.length)]);
  }

  @State(Scope.Benchmark)
  public static class EstimationState {

    private static final int NUM_EXAMPLES = 1000;

    List<UltraLogLog> ultraLogLogs = null;

    @Param({"1", "10", "100", "1000", "10000", "100000", "1000000"})
    public int numElements;

    // @Param({"8", "10", "12", "14"})
    @Param({"10", "14"})
    public int precision;

    @Setup
    public void init() {
      SplittableRandom random = new SplittableRandom(0xdf12f8a7a8569e6aL);
      ultraLogLogs =
          Stream.generate(() -> generate(random, numElements, precision))
              .limit(NUM_EXAMPLES)
              .collect(toList());
    }

    @TearDown
    public void finish() {
      ultraLogLogs = null;
    }
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  public void distinctCountEstimation(EstimationState estimationState, Blackhole blackhole) {
    double sum = 0;
    for (UltraLogLog ultraLogLog : estimationState.ultraLogLogs) {
      sum += ultraLogLog.getDistinctCountEstimate();
    }
    blackhole.consume(sum);
  }

  @State(Scope.Benchmark)
  public static class MergeState {

    static final int NUM_EXAMPLES = 1000;

    List<UltraLogLog> ultraLogLogs1 = null;
    List<UltraLogLog> ultraLogLogs2 = null;

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
      ultraLogLogs1 =
          Stream.generate(() -> generate(random, numElements1, precision1))
              .limit(NUM_EXAMPLES)
              .collect(toList());
      ultraLogLogs2 =
          Stream.generate(() -> generate(random, numElements2, precision2))
              .limit(NUM_EXAMPLES)
              .collect(toList());
    }

    @TearDown
    public void finish() {
      ultraLogLogs1 = null;
      ultraLogLogs2 = null;
    }
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  public void distinctCountMerge(
      MergeState mergeState, RandomState randomState, Blackhole blackhole) {
    long sum = 0;
    for (int i = 0; i < mergeState.NUM_EXAMPLES; ++i) {
      UltraLogLog mergedUltraLogLog =
          UltraLogLog.merge(mergeState.ultraLogLogs1.get(i), mergeState.ultraLogLogs2.get(i));
      sum +=
          mergedUltraLogLog
              .getState()[randomState.random.nextInt(mergedUltraLogLog.getState().length)];
    }
    blackhole.consume(sum);
  }
}
