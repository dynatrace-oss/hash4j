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
   * <p>This implementations allows to specify the version of the implementation, to ensure backward
   * compatibility with similarity hash signatures computed with earlier versions.
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
   * <p>This implementations allows to specify the version of the implementation, to ensure backward
   * compatibility with similarity hash signatures computed with earlier versions.
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
}
