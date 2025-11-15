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
package com.dynatrace.hash4j.similarity;

import static org.assertj.core.api.Assertions.assertThat;

import com.dynatrace.hash4j.random.PseudoRandomGeneratorProvider;
import org.junit.jupiter.api.Test;

class SuperMinHashPolicy_v1bTest extends AbstractSuperMinHashPolicyTest {

  @Override
  protected SimilarityHashPolicy getSimilarityHashPolicy(
      int numberOfComponents, int bitsPerComponent) {
    return new SuperMinHashPolicy_v1b(
        numberOfComponents, bitsPerComponent, PseudoRandomGeneratorProvider.splitMix64_V1());
  }

  @Test
  void testCycleLimitEstimation() {

    int numberOfComponents = 10;
    SuperMinHashPolicy_v1b policy =
        new SuperMinHashPolicy_v1b(
            numberOfComponents, 3, PseudoRandomGeneratorProvider.splitMix64_V1());
    assertThat(policy.estimateCycleLimit(1)).isEqualTo(10);
    assertThat(policy.estimateCycleLimit(2)).isEqualTo(10);
    assertThat(policy.estimateCycleLimit(3)).isEqualTo(9);
    assertThat(policy.estimateCycleLimit(4)).isEqualTo(9);
    assertThat(policy.estimateCycleLimit(5)).isEqualTo(8);
    assertThat(policy.estimateCycleLimit(1000)).isEqualTo(1);
  }
}
