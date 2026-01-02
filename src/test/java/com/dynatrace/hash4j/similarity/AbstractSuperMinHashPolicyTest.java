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

import com.dynatrace.hash4j.testutils.TestUtils;
import org.junit.jupiter.api.Test;

abstract class AbstractSuperMinHashPolicyTest extends AbstractMinHashPolicyTest {

  @Test
  public void testStability() {

    int numberOfComponents = 30;

    SimilarityHashPolicy policy = getSimilarityHashPolicy(numberOfComponents, 3);
    SimilarityHasher hasher = policy.createHasher();

    assertThat(
            TestUtils.byteArrayToHexString(
                hasher.compute(ElementHashProvider.ofValues(0xb8583da3ea9931faL))))
        .isEqualTo("bf5716cc30f0b18cb0095d01");
    assertThat(
            TestUtils.byteArrayToHexString(
                hasher.compute(ElementHashProvider.ofValues(0x5b6acf5022cfa644L))))
        .isEqualTo("5a356a62b741525834a89e02");
    assertThat(
            TestUtils.byteArrayToHexString(
                hasher.compute(
                    ElementHashProvider.ofValues(0x14fccc4459b81a17L, 0x6540fea72ebf8598L))))
        .isEqualTo("1eb3af955701ebbcd88b0d02");
    assertThat(
            TestUtils.byteArrayToHexString(
                hasher.compute(
                    ElementHashProvider.ofValues(
                        0x14fccc4459b81a17L, 0x6540fea72ebf8598L, 0x14fccc4459b81a17L))))
        .isEqualTo("1eb3af955701ebbcd88b0d02");
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
        .isEqualTo("ed3991e6ac15ba594972f402");
  }

  @Override
  protected long getCheckSum() {
    return 313093255749283787L;
  }
}
