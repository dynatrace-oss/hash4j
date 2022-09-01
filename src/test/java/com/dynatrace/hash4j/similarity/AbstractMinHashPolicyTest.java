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

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

import org.junit.jupiter.api.Test;

public abstract class AbstractMinHashPolicyTest extends AbstractSimilarityHasherPolicyTest {

  @Override
  protected final SimilarityHashPolicy getSimilarityHashPolicy(int numberOfComponents) {
    return getSimilarityHashPolicy(numberOfComponents, 1);
  }

  protected abstract SimilarityHashPolicy getSimilarityHashPolicy(
      int numberOfComponents, int bitsPerComponent);

  @Test
  void testInvalidBitsPerComponent() {
    assertThatThrownBy(() -> getSimilarityHashPolicy(5, -1))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> getSimilarityHashPolicy(5, 0))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> getSimilarityHashPolicy(5, 65))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void testGetComponentSizeInBits() {
    for (int bitsPerComponent = 1; bitsPerComponent <= 64; ++bitsPerComponent) {
      SimilarityHashPolicy policy = getSimilarityHashPolicy(3, bitsPerComponent);
      assertThat(policy.getComponentSizeInBits()).isEqualTo(bitsPerComponent);
    }
  }

  @Override
  protected double calculateExpectedMatchProbability(
      long intersectionSize, long difference1Size, long difference2Size) {
    long unionSize = intersectionSize + difference1Size + difference2Size;
    double expectedJaccardSimilarity = intersectionSize / (double) unionSize;
    return (1. + expectedJaccardSimilarity) * 0.5;
  }
}
