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
package com.dynatrace.hash4j.hashing;

/**
 * A hash function.
 *
 * <p>Instances are immutable. Therefore, it is safe to use a single instance across multiple
 * threads and for multiple hash calculations.
 */
interface Hasher {

  /**
   * Starts a hash stream.
   *
   * @return a new {@link HashStream} instance
   */
  HashStream hashStream();

  /**
   * Reconstructs a hash stream from a given state.
   *
   * <p>The behavior is undefined, if the state was not previously created by a hash stream of the
   * same hasher with same seed parameters.
   *
   * @return a new {@link HashStream} instance
   */
  default HashStream hashStreamFromState(byte[] state) {
    return hashStream().setState(state);
  }

  /**
   * The size of the hash value in bits.
   *
   * @return the size of the hash value in bits
   */
  int getHashBitSize();
}
