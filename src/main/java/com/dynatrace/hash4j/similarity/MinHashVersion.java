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

import com.dynatrace.hash4j.random.PseudoRandomGeneratorProvider;

public enum MinHashVersion {
  DEFAULT {
    @Override
    SimilarityHashPolicy create(int numberOfComponents, int bitsPerComponent) {
      return new MinHashPolicy_v1(
          numberOfComponents, bitsPerComponent, PseudoRandomGeneratorProvider.splitMix64_V1());
    }
  },
  V1 {
    @Override
    SimilarityHashPolicy create(int numberOfComponents, int bitsPerComponent) {
      return new MinHashPolicy_v1(
          numberOfComponents, bitsPerComponent, PseudoRandomGeneratorProvider.splitMix64_V1());
    }
  };

  abstract SimilarityHashPolicy create(int numberOfComponents, int bitsPerComponent);
}
