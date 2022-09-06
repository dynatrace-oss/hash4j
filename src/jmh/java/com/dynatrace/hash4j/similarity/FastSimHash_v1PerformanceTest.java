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
package com.dynatrace.hash4j.similarity;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

public class FastSimHash_v1PerformanceTest extends SimilarityHashingPerformanceTest {

  public static class State_64 extends StateBase {
    public State_64() {
      super(new FastSimHashPolicy_v1(64, getPseudoRandomGeneratorProvider()).createHasher());
    }
  }

  public static class State_256 extends StateBase {
    public State_256() {
      super(new FastSimHashPolicy_v1(256, getPseudoRandomGeneratorProvider()).createHasher());
    }
  }

  public static class State_1024 extends StateBase {
    public State_1024() {
      super(new FastSimHashPolicy_v1(1024, getPseudoRandomGeneratorProvider()).createHasher());
    }
  }

  public static class State_4096 extends StateBase {
    public State_4096() {
      super(new FastSimHashPolicy_v1(4096, getPseudoRandomGeneratorProvider()).createHasher());
    }
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  public void testSimilarityHashing_64_1(State_64 state, Blackhole blackhole) {
    testSimilarityHashing(state, blackhole);
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  public void testSimilarityHashing_256_1(State_256 state, Blackhole blackhole) {
    testSimilarityHashing(state, blackhole);
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  public void testSimilarityHashing_1024_1(State_1024 state, Blackhole blackhole) {
    testSimilarityHashing(state, blackhole);
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  public void testSimilarityHashing_4096_1(State_4096 state, Blackhole blackhole) {
    testSimilarityHashing(state, blackhole);
  }
}