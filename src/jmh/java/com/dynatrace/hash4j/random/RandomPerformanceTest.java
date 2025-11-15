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
package com.dynatrace.hash4j.random;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;

public class RandomPerformanceTest {
  @State(Scope.Thread)
  public static class RandomGeneratorState {
    public final PseudoRandomGenerator prng =
        PseudoRandomGeneratorProvider.splitMix64_V1().create();
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  public void randomLong(RandomGeneratorState randomGeneratorState, Blackhole blackhole) {
    blackhole.consume(randomGeneratorState.prng.nextLong());
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  public void randomInt(RandomGeneratorState randomGeneratorState, Blackhole blackhole) {
    blackhole.consume(randomGeneratorState.prng.nextInt());
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  public void randomExponential(RandomGeneratorState randomGeneratorState, Blackhole blackhole) {
    blackhole.consume(randomGeneratorState.prng.nextExponential());
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  public void randomDouble(RandomGeneratorState randomGeneratorState, Blackhole blackhole) {
    blackhole.consume(randomGeneratorState.prng.nextDouble());
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  public void randomExponentialAlternative(
      RandomGeneratorState randomGeneratorState, Blackhole blackhole) {
    blackhole.consume(StrictMath.log1p(-randomGeneratorState.prng.nextDouble()));
  }
}
