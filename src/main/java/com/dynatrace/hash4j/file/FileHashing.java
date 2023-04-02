/*
 * Copyright 2023 Dynatrace LLC
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
package com.dynatrace.hash4j.file;

/** Various implementations of hash functions for files. */
public interface FileHashing {

  /**
   * Returns a {@link FileHasher128} implementing version 1.0.2 of the Imohash algorithm using
   * default parameters.
   *
   * <p>This implementation is compatible with the Go reference implementation <a
   * href="https://github.com/kalafut/imohash/blob/v1.0.2/imohash.go">imohash.go</a>.
   *
   * <p>For a description of the algorithm see <a
   * href="https://github.com/kalafut/imohash/blob/v1.0.2/algorithm.md">here</a>.
   *
   * <p>This algorithm does not return a uniformly distributed hash value.
   *
   * @return a file hasher instance
   */
  static FileHasher128 imohash1_0_2() {
    return Imohash1_0_2.create();
  }

  /**
   * Returns a {@link FileHasher128} implementing version 1.0.2 of the Imohash algorithm using
   * default parameters.
   *
   * <p>This implementation is compatible with the Go reference implementation <a
   * href="https://github.com/kalafut/imohash/blob/v1.0.2/imohash.go">imohash.go</a>.
   *
   * <p>For a description of the algorithm and the parameters see <a
   * href="https://github.com/kalafut/imohash/blob/v1.0.2/algorithm.md">here</a>.
   *
   * <p>This algorithm does not return a uniformly distributed hash value.
   *
   * @param sampleSize the sample size
   * @param sampleThreshold the sample threshold
   * @return a file hasher instance
   */
  static FileHasher128 imohash1_0_2(int sampleSize, long sampleThreshold) {
    return Imohash1_0_2.create(sampleSize, sampleThreshold);
  }
}
