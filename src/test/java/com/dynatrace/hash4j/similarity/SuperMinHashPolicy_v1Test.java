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
package com.dynatrace.hash4j.similarity;

import static org.assertj.core.api.Assertions.assertThat;

import com.dynatrace.hash4j.random.PseudoRandomGeneratorProvider;
import org.junit.jupiter.api.Test;

class SuperMinHashPolicy_v1Test extends AbstractSuperMinHashPolicyTest {

  @Override
  protected SimilarityHashPolicy getSimilarityHashPolicy(
      int numberOfComponents, int bitsPerComponent) {
    return new SuperMinHashPolicy_v1(
        numberOfComponents, bitsPerComponent, PseudoRandomGeneratorProvider.splitMix64_V1());
  }

  @Test
  void testInitCycleLimits() {
    assertThat(SuperMinHashPolicy_v1.initCycleLimits(1, 0.99)).isEqualTo(new int[] {1});
    assertThat(SuperMinHashPolicy_v1.initCycleLimits(10, 0.99))
        .isEqualTo(new int[] {2, 3, 5, 6, 8, 10, 10, 10, 10, 10});
    assertThat(SuperMinHashPolicy_v1.initCycleLimits(10, 0.9))
        .isEqualTo(new int[] {2, 2, 3, 4, 5, 7, 9, 10, 10, 10});
    assertThat(SuperMinHashPolicy_v1.initCycleLimits(50, 0.99))
        .isEqualTo(
            new int[] {
              2, 3, 3, 4, 4, 4, 5, 5, 5, 5, 6, 6, 6, 7, 7, 8, 8, 8, 9, 9, 10, 10, 11, 11, 12, 13,
              14, 14, 15, 16, 17, 18, 20, 21, 23, 24, 26, 29, 32, 35, 39, 43, 49, 50, 50, 50, 50,
              50, 50, 50
            });
    assertThat(SuperMinHashPolicy_v1.initCycleLimits(100, 0.99))
        .isEqualTo(
            new int[] {
              2, 2, 3, 3, 3, 4, 4, 4, 4, 4, 4, 5, 5, 5, 5, 5, 6, 6, 6, 6, 6, 6, 7, 7, 7, 7, 7, 8, 8,
              8, 8, 8, 9, 9, 9, 9, 10, 10, 10, 10, 11, 11, 11, 11, 12, 12, 12, 13, 13, 13, 14, 14,
              15, 15, 15, 16, 16, 17, 17, 18, 19, 19, 20, 20, 21, 22, 23, 23, 24, 25, 26, 27, 29,
              30, 31, 32, 34, 36, 38, 40, 42, 44, 47, 50, 53, 57, 62, 67, 73, 79, 88, 98, 100, 100,
              100, 100, 100, 100, 100, 100
            });
  }
}
