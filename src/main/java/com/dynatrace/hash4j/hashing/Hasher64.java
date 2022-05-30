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
package com.dynatrace.hash4j.hashing;

/**
 * A 64-bit hash function.
 *
 * <p>Instances are immutable. Therefore, it is safe to use a single instance across multiple
 * threads and for multiple hash calculations.
 *
 * <p>Implementations must ensure that
 *
 * <p>{@code (int)}{@link #hashToLong}{@code (obj, funnel) == }{@link #hashToInt}{@code (obj,
 * funnel)}
 */
public interface Hasher64 extends Hasher32 {

  /**
   * Hashes an object to a 64-bit {@code long} value.
   *
   * @param obj the object
   * @param funnel the funnel
   * @param <T> the type
   * @return the hash value
   */
  <T> long hashToLong(T obj, HashFunnel<T> funnel);

  /**
   * Hashes a byte array to a 64-bit {@code long} value.
   *
   * <p>Equivalent to {@link #hashBytesToLong}{@code (input, 0, input.length)}.
   *
   * @param input the byte array
   * @return the hash value
   */
  long hashBytesToLong(byte[] input);

  /**
   * Hashes a byte array to a 64-bit {@code long} value.
   *
   * <p>Equivalent to {@link #hashToLong}{@code (input, (b, f) -> f.putBytes(b, off, len))}.
   *
   * @param input the byte array
   * @param off the offset
   * @param len the length
   * @return the hash value
   */
  long hashBytesToLong(byte[] input, int off, int len);

  /**
   * Hashes a {@link CharSequence} to a 64-bit {@code long} value.
   *
   * <p>Equivalent to {@link #hashToLong}{@code (input, (c, f) -> f.putChars(c))}.
   *
   * @param input the char sequence
   * @return the hash value
   */
  long hashCharsToLong(CharSequence input);
}
