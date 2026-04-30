/*
 * Copyright 2022-2026 Dynatrace LLC
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

import com.dynatrace.hash4j.random.PseudoRandomGeneratorProvider;
import java.util.SplittableRandom;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

@Fork(value = 1)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS, batchSize = 1)
@Measurement(iterations = 20, time = 1, timeUnit = TimeUnit.SECONDS)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
public class SimilarityHashingPerformanceTest {

  @State(Scope.Thread)
  public static class StateBase implements ElementHashProvider {
    public final SplittableRandom random = new SplittableRandom();
    public final SimilarityHasher similarityHasher;

    @Param({"1", "10", "100", "1000", "10000", "100000"})
    public int numElements;

    public StateBase(SimilarityHasher similarityHasher) {
      this.similarityHasher = similarityHasher;
    }

    public final long[] elementHashes = new long[100000]; // maximum number of elements

    @Override
    public long getElementHash(int elementIndex) {
      return elementHashes[elementIndex];
    }

    @Override
    public int getNumberOfElements() {
      return numElements;
    }
  }

  protected void testSimilarityHashing(StateBase state, Blackhole blackhole) {
    for (int i = 0; i < state.numElements; ++i) {
      state.elementHashes[i] = state.random.nextLong();
    }
    byte[] signature = state.similarityHasher.compute(state);
    blackhole.consume(signature);
  }

  protected static PseudoRandomGeneratorProvider getPseudoRandomGeneratorProvider() {
    return PseudoRandomGeneratorProvider.splitMix64_V1();
  }
}
