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

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.dynatrace.hash4j.random.PseudoRandomGeneratorProvider;
import com.dynatrace.hash4j.testutils.TestUtils;
import org.junit.jupiter.api.Test;

public class FastSimHashPolicy_v1Test extends AbstractSimilarityHasherPolicyTest {

  private static double calculateComponentCollisionProbability(double cosineSimilarity) {
    return Math.min(1., Math.max(0.5, Math.acos(-cosineSimilarity) / Math.PI));
  }

  @Test
  public void testStability() {

    int numberOfComponents = 30;

    SimilarityHashPolicy policy = getSimilarityHashPolicy(numberOfComponents);
    SimilarityHasher hasher = policy.createHasher();

    assertEquals(
        "5c378e22",
        TestUtils.byteArrayToHexString(
            hasher.compute(ElementHashProvider.ofValues(0xb8583da3ea9931faL))));
    assertEquals(
        "a5c2fb3f",
        TestUtils.byteArrayToHexString(
            hasher.compute(ElementHashProvider.ofValues(0x5b6acf5022cfa644L))));
    assertEquals(
        "6aeb883b",
        TestUtils.byteArrayToHexString(
            hasher.compute(
                ElementHashProvider.ofValues(0x14fccc4459b81a17L, 0x6540fea72ebf8598L))));
    assertEquals(
        "6eef0033",
        TestUtils.byteArrayToHexString(
            hasher.compute(
                ElementHashProvider.ofValues(
                    0x14fccc4459b81a17L, 0x6540fea72ebf8598L, 0x14fccc4459b81a17L))));
    assertEquals(
        "86bb8229",
        TestUtils.byteArrayToHexString(
            hasher.compute(
                ElementHashProvider.ofValues(
                    0x1f3271beee750ae1L,
                    0x9f8aaaec2d1dad05L,
                    0x42c23a533205bdafL,
                    0x1b990d3329aa0644L,
                    0xe7b85af3207824f8L,
                    0x2b6d7cc88681f07bL))));
  }

  @Test
  void testCalculateBulkMask() {
    assertEquals(0x0000000000000001L, FastSimHashPolicy_v1.calculateBulkMask(0));
    assertEquals(0x0000000100000001L, FastSimHashPolicy_v1.calculateBulkMask(1));
    assertEquals(0x0001000100010001L, FastSimHashPolicy_v1.calculateBulkMask(2));
    assertEquals(0x0101010101010101L, FastSimHashPolicy_v1.calculateBulkMask(3));
    assertEquals(0x1111111111111111L, FastSimHashPolicy_v1.calculateBulkMask(4));
    assertEquals(0x5555555555555555L, FastSimHashPolicy_v1.calculateBulkMask(5));
    assertEquals(0xffffffffffffffffL, FastSimHashPolicy_v1.calculateBulkMask(6));
  }

  @Test
  void testCalculateTemporaryCounterLimit() {
    assertEquals(0xffffffffffffffffL, FastSimHashPolicy_v1.calculateTemporaryCounterLimit(0));
    assertEquals(0x00000000ffffffffL, FastSimHashPolicy_v1.calculateTemporaryCounterLimit(1));
    assertEquals(0x000000000000ffffL, FastSimHashPolicy_v1.calculateTemporaryCounterLimit(2));
    assertEquals(0x00000000000000ffL, FastSimHashPolicy_v1.calculateTemporaryCounterLimit(3));
    assertEquals(0x000000000000000fL, FastSimHashPolicy_v1.calculateTemporaryCounterLimit(4));
    assertEquals(0x0000000000000003L, FastSimHashPolicy_v1.calculateTemporaryCounterLimit(5));
    assertEquals(0x0000000000000001L, FastSimHashPolicy_v1.calculateTemporaryCounterLimit(6));
  }

  @Override
  protected double calculateExpectedMatchProbability(
      long intersectionSize, long difference1Size, long difference2Size) {

    double expectedCosineSimilarity =
        intersectionSize
            / Math.sqrt(
                (intersectionSize + difference1Size)
                    * (double) (intersectionSize + difference2Size));

    return calculateComponentCollisionProbability(expectedCosineSimilarity);
  }

  @Override
  protected SimilarityHashPolicy getSimilarityHashPolicy(int numberOfComponents) {
    return new FastSimHashPolicy_v1(
        numberOfComponents, PseudoRandomGeneratorProvider.splitMix64_V1());
  }

  @Override
  protected long getCheckSum() {
    return -4724860042207891761L;
  }

  @Override
  protected int getMaxSizeForCheckSumTest() {
    return 300;
  }
}
