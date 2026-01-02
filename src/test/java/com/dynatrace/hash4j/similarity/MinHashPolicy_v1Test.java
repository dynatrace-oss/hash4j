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
import com.dynatrace.hash4j.testutils.TestUtils;
import org.junit.jupiter.api.Test;

class MinHashPolicy_v1Test extends AbstractMinHashPolicyTest {

  @Test
  void testStability() {

    int numberOfComponents = 30;

    SimilarityHashPolicy policy = getSimilarityHashPolicy(numberOfComponents, 3);
    SimilarityHasher hasher = policy.createHasher();

    assertThat(
            TestUtils.byteArrayToHexString(
                hasher.compute(ElementHashProvider.ofValues(0xb8583da3ea9931faL))))
        .isEqualTo("125a2771daa5b68bf2d4e400");
    assertThat(
            TestUtils.byteArrayToHexString(
                hasher.compute(ElementHashProvider.ofValues(0x5b6acf5022cfa644L))))
        .isEqualTo("ad30f5738cf13b536a6cd700");
    assertThat(
            TestUtils.byteArrayToHexString(
                hasher.compute(
                    ElementHashProvider.ofValues(0x14fccc4459b81a17L, 0x6540fea72ebf8598L))))
        .isEqualTo("0e6cef9dab3a7815892bd801");
    assertThat(
            TestUtils.byteArrayToHexString(
                hasher.compute(
                    ElementHashProvider.ofValues(
                        0x14fccc4459b81a17L, 0x6540fea72ebf8598L, 0x14fccc4459b81a17L))))
        .isEqualTo("0e6cef9dab3a7815892bd801");
    assertThat(
            TestUtils.byteArrayToHexString(
                hasher.compute(
                    ElementHashProvider.ofValues(
                        0x1f3271beee750ae1L,
                        0x9f8aaaec2d1dad05L,
                        0x42c23a533205bdafL,
                        0x1b990d3329aa0644L,
                        0xe7b85af3207824f8L,
                        0x2b6d7cc88681f07bL))))
        .isEqualTo("22ad5e6e9830da3abf844703");
  }

  @Override
  protected SimilarityHashPolicy getSimilarityHashPolicy(
      int numberOfComponents, int bitsPerComponent) {
    return new MinHashPolicy_v1(
        numberOfComponents, bitsPerComponent, PseudoRandomGeneratorProvider.splitMix64_V1());
  }

  @Override
  protected long getCheckSum() {
    return -8084016319187376043L;
  }
}
