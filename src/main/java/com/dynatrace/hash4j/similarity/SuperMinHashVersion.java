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

import com.dynatrace.hash4j.random.PseudoRandomGeneratorProvider;

/** Versions of SuperMinHash implementations. */
public enum SuperMinHashVersion {
  /**
   * Default version.
   *
   * <p>Not stable! Use concrete version if compatibility is important, if for example hash
   * signatures are persisted.
   */
  DEFAULT {
    @Override
    SimilarityHashPolicy create(int numberOfComponents, int bitsPerComponent) {
      return new SuperMinHashPolicy_v1(
          numberOfComponents, bitsPerComponent, PseudoRandomGeneratorProvider.splitMix64_V1());
    }
  },
  /** Version 1. */
  V1 {
    @Override
    SimilarityHashPolicy create(int numberOfComponents, int bitsPerComponent) {
      return new SuperMinHashPolicy_v1(
          numberOfComponents, bitsPerComponent, PseudoRandomGeneratorProvider.splitMix64_V1());
    }
  };

  abstract SimilarityHashPolicy create(int numberOfComponents, int bitsPerComponent);
}
