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

/** A 32-bit hash function. */
public interface Hasher32 {

  /**
   * Hashes an object to a 32-bit integer value.
   *
   * @param obj the object
   * @param funnel the funnel
   * @param <T> the type
   * @return the hash value
   */
  <T> int hashToInt(T obj, HashFunnel<T> funnel);

  /**
   * Hashes a byte array to a 32-bit integer value.
   *
   * <p>Equivalent to {@code hashBytesToInt(input, 0, input.length)}.
   *
   * @param input the byte array
   * @return the hash value
   */
  int hashBytesToInt(byte[] input);

  /**
   * Hashes a byte array to a 32-bit integer value.
   *
   * <p>Equivalent to {@code hashBytesToInt(input, (b, f) -> f.putBytes(b, off, len))}.
   *
   * @param input the byte array
   * @param off the offset
   * @param len the length
   * @return the hash value
   */
  int hashBytesToInt(byte[] input, int off, int len);
}
