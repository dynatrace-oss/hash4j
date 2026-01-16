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
package com.dynatrace.hash4j.random;

/** A pseudo-random generator. */
public interface PseudoRandomGenerator {

  /**
   * Returns a random uniformly distributed 64-bit {@code long} value.
   *
   * @return a random value
   */
  long nextLong();

  /**
   * Returns a random uniformly distributed 32-bit {@code int} value.
   *
   * @return a random value
   */
  int nextInt();

  /**
   * Returns a random uniformly distributed 32-bit {code int} value greater than or equal to 0 and
   * less than the given upper bound.
   *
   * <p>The behavior is undefined, if the given upper bound is non-positive.
   *
   * @param exclusiveBound the (exclusive) upper bound (must be positive)
   * @return a random value
   */
  int uniformInt(int exclusiveBound);

  /**
   * Returns a random uniformly distributed 64-bit {code long} value greater than or equal to 0 and
   * less than the given upper bound.
   *
   * <p>The behavior is undefined, if the given upper bound is non-positive.
   *
   * @param exclusiveBound the (exclusive) upper bound (must be positive)
   * @return a random value
   */
  long uniformLong(long exclusiveBound);

  /**
   * Resets the pseudo-random generator using the given 64-bit seed value.
   *
   * @param seed the seed value
   * @return this
   */
  PseudoRandomGenerator reset(long seed);

  /**
   * Returns a random uniformly distributed {@code double} value in the range [0, 1).
   *
   * @return a random value
   */
  double nextDouble();

  /**
   * Returns an exponentially distributed {@code double} value with mean 1.
   *
   * @return a random value
   */
  double nextExponential();
}
