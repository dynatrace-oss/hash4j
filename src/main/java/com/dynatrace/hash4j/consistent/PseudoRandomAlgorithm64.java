/*
 * Copyright 2025-2026 Dynatrace LLC
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

/**
 * A 64-bit pseudo-random number generation algorithm with 64-bit state.
 *
 * <p>This class is stateless and thread-safe.
 *
 * <p>Given some random 64-bit seed the algorithm produces a sequence of 64-bit pseudo-random values
 * according to the following scheme:
 *
 * <pre>
 *     long state = algorithm.initState(seed);
 *     long randomValue1 = algorithm.nextLong(state);
 *     state = algorithm.updateState(state);
 *     long randomValue2 = algorithm.nextLong(state);
 *     state = algorithm.updateState(state);
 *     long randomValue3 = algorithm.nextLong(state);
 *     ...
 * </pre>
 */
interface PseudoRandomAlgorithm64 {

  /**
   * Initializes the state for the first random number generation based on the given seed.
   *
   * <p>This function must be called before the first call to {@link #nextLong(long)} to set up the
   * state with the given seed.
   *
   * @param seed the seed
   * @return the initialized state
   */
  long initState(long seed);

  /**
   * Updates the state for the next random number generation.
   *
   * <p>This function must be called before each (except for the first) call to {@link
   * #nextLong(long)} to advance the state.
   *
   * @param state the state
   * @return the updated state
   */
  long updateState(long state);

  /**
   * Returns the next pseudo-random long value based on the given state.
   *
   * <p>This function call must always be preceded by a function call of {@link #initState(long)} or
   * {@link #updateState(long)}.
   *
   * @param state the current state
   * @return the next pseudo-random long value
   */
  long nextLong(long state);

  /**
   * Returns the SplitMix64 pseudo-random number generation algorithm.
   *
   * <p>This algorithm allows to reproduce the same sequence of pseudo-random numbers as {@link
   * java.util.SplittableRandom}.
   *
   * @return the SplitMix64 algorithm
   */
  static PseudoRandomAlgorithm64 getSplitMix64() {
    return SplitMix64Algorithm.get();
  }

  /**
   * Returns a xor-shift based pseudo-random number generation algorithm that uses left shift by 7
   * and right shift by 9.
   *
   * <p>This is a very fast, minimalistic but simple random algorithm with low quality failing many
   * statistical tests. Its use is only recommended after appropriate statistical testing for the
   * respective application.
   *
   * <p>A seed of zero will produce a sequence of zeros only. All other seeds are guaranteed to
   * produce a periodic sequence with period length 2^64 - 1.
   *
   * <p>For maximum xorshift generators with non-zero seed it is known, that no single bit can
   * remain constant for more than n consecutive steps, if n is the state size. If it did, the
   * linear independence of successive states would fail, and the generator could not have maximal
   * period. This property is relevant for the JumpBackHash algorithm to guarantee termination of
   * the inner loop.
   *
   * @see <a
   *     href="https://en.wikipedia.org/w/index.php?title=Xorshift&oldid=1242199929#Example_implementation">Wikipedia</a>
   *     and <a href="http://isaku-wada.my.coocan.jp/rand/rand.html">Isaku Wada's homepage</a>
   * @return a xor-shift based algorithm
   */
  static PseudoRandomAlgorithm64 getXorshiftL7R9() {
    return XorshiftL7R9Algorithm.get();
  }
}
