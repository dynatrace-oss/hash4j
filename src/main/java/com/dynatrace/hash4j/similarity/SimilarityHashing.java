/*
 * Copyright 2022-2023 Dynatrace LLC
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

/** Various implementations of similarity hash algorithms. */
public interface SimilarityHashing {

  /**
   * Returns a {@link SimilarityHashPolicy} for a modified version of b-bit minwise hashing
   * described in <a href="https://doi.org/10.1145/1772690.1772759">Ping Li and Christian König,
   * B-Bit minwise hashing, 2010.</a>.
   *
   * @param numberOfComponents the number of components of the signature
   * @param bitsPerComponent the number of bits per component in the range [1, 64]
   * @return a policy
   */
  static SimilarityHashPolicy minHash(int numberOfComponents, int bitsPerComponent) {
    return minHash(numberOfComponents, bitsPerComponent, MinHashVersion.DEFAULT);
  }

  /**
   * Returns a {@link SimilarityHashPolicy} for a modified version of b-bit minwise hashing
   * described in <a href="https://doi.org/10.1145/1772690.1772759">Ping Li and Christian König,
   * B-Bit minwise hashing, 2010.</a>.
   *
   * <p>Specifying the version of the implementation ensures compatibility with later hash4j
   * versions that may change the default implementation. This is especially important if the
   * signatures are persisted.
   *
   * @param numberOfComponents the number of components of the signature
   * @param bitsPerComponent the number of bits per component in the range [1, 64]
   * @param minHashVersion the version of the implementation
   * @return a policy
   */
  static SimilarityHashPolicy minHash(
      int numberOfComponents, int bitsPerComponent, MinHashVersion minHashVersion) {
    return minHashVersion.create(numberOfComponents, bitsPerComponent);
  }

  /**
   * Returns a {@link SimilarityHashPolicy} for SuperMinHash described in <a
   * href="https://arxiv.org/abs/1706.05698">Otmar Ertl, SuperMinHash - A New Minwise Hashing
   * Algorithm for Jaccard Similarity Estimation, 2017.</a>.
   *
   * @param numberOfComponents the number of components of the similarity hash
   * @param bitsPerComponent the number of bits per component in the range [1, 64]
   * @return a policy
   */
  static SimilarityHashPolicy superMinHash(int numberOfComponents, int bitsPerComponent) {
    return superMinHash(numberOfComponents, bitsPerComponent, SuperMinHashVersion.DEFAULT);
  }

  /**
   * Returns a {@link SimilarityHashPolicy} for SuperMinHash described in <a
   * href="https://arxiv.org/abs/1706.05698">Otmar Ertl, SuperMinHash - A New Minwise Hashing
   * Algorithm for Jaccard Similarity Estimation, 2017.</a>.
   *
   * <p>Specifying the version of the implementation ensures compatibility with later hash4j
   * versions that may change the default implementation. This is especially important if the
   * signatures are persisted.
   *
   * @param numberOfComponents the number of components of the similarity hash
   * @param bitsPerComponent the number of bits per component in the range [1, 64]
   * @param superMinHashVersion the version of the implementation
   * @return a policy
   */
  static SimilarityHashPolicy superMinHash(
      int numberOfComponents, int bitsPerComponent, SuperMinHashVersion superMinHashVersion) {
    return superMinHashVersion.create(numberOfComponents, bitsPerComponent);
  }

  /**
   * Returns a {@link SimilarityHashPolicy} for FastSimHash, which is a fast implementation of the
   * SimHash algorithm as introduced in <a
   * href="https://dl.acm.org/doi/abs/10.1145/509907.509965?casa_token=LO2phP3daHEAAAAA%3Ad2zE2ktXOGP8JqCsSo0jqsQcfOx8-Jclq7_katfP_FRpXWJMPU3OuDE8QZATbYdePl7VRbibDUqWdQ">Moses
   * S. Charikar, Similarity estimation techniques from rounding algorithms, 2002.</a>
   *
   * <p>To compute the SimHash signature, a counter is used for each signature component to count
   * how many times a corresponding bit is set in the hash values of all elements of the given set.
   * Unlike other SimHash implementations that iterate over all the individual bits of all the hash
   * values of the elements, FastSimHash processes 8 bits at once thanks to some bit tricks, which
   * results in a significant speedup.
   *
   * @param numberOfComponents the number of components of the similarity hash
   * @return a policy
   */
  static SimilarityHashPolicy fastSimHash(int numberOfComponents) {
    return fastSimHash(numberOfComponents, FastSimHashVersion.DEFAULT);
  }

  /**
   * Returns a {@link SimilarityHashPolicy} for FastSimHash, which is a fast implementation of the
   * SimHash algorithm as introduced in <a
   * href="https://dl.acm.org/doi/abs/10.1145/509907.509965?casa_token=LO2phP3daHEAAAAA%3Ad2zE2ktXOGP8JqCsSo0jqsQcfOx8-Jclq7_katfP_FRpXWJMPU3OuDE8QZATbYdePl7VRbibDUqWdQ">Moses
   * S. Charikar, Similarity estimation techniques from rounding algorithms, 2002.</a>
   *
   * <p>To compute the SimHash signature, a counter is used for each signature component to count
   * how many times a corresponding bit is set in the hash values of all elements of the given set.
   * Unlike other SimHash implementations that iterate over all the individual bits of all the hash
   * values of the elements, FastSimHash processes 8 bits at once thanks to some bit tricks, which
   * results in a significant speedup.
   *
   * <p>Specifying the version of the implementation ensures compatibility with later hash4j
   * versions that may change the default implementation. This is especially important if the
   * signatures are persisted.
   *
   * @param numberOfComponents the number of components of the similarity hash
   * @param fastSimHashVersion the version of the implementation
   * @return a policy
   */
  static SimilarityHashPolicy fastSimHash(
      int numberOfComponents, FastSimHashVersion fastSimHashVersion) {
    return fastSimHashVersion.create(numberOfComponents);
  }
}
