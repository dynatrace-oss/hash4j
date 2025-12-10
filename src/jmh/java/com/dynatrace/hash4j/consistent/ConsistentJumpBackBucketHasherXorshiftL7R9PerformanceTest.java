/*
 * Copyright 2025 Dynatrace LLC
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
package com.dynatrace.hash4j.consistent;

import java.util.SplittableRandom;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;

public class ConsistentJumpBackBucketHasherXorshiftL7R9PerformanceTest {

  private static final ConsistentBucketHasher CONSISTENT_BUCKET_HASHER =
      ConsistentHashing.jumpBackHashXorshiftL7R9();

  @State(Scope.Thread)
  public static class TestState {

    @Param({"1", "10", "100", "1000", "10000", "100000", "1000000"})
    int numBuckets;

    SplittableRandom random;

    @Setup
    public void init() {
      random = new SplittableRandom(0x87c5950e6677341eL);
    }
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  public void getBucket(TestState testState, Blackhole blackhole) {
    int bucket =
        CONSISTENT_BUCKET_HASHER.getBucket(testState.random.nextLong(), testState.numBuckets);
    blackhole.consume(bucket);
  }
}
