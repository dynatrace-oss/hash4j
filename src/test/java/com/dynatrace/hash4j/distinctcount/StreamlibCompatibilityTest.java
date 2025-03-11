/*
 * Copyright 2025 Dynatrace LLC
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
package com.dynatrace.hash4j.distinctcount;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Percentage.withPercentage;

import com.clearspring.analytics.hash.MurmurHash;
import com.clearspring.analytics.stream.cardinality.CardinalityMergeException;
import com.clearspring.analytics.stream.cardinality.HyperLogLogPlus;
import com.dynatrace.hash4j.util.PackedArray;
import com.dynatrace.hash4j.util.PackedArray.PackedArrayHandler;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class StreamlibCompatibilityTest {

  private static final PackedArrayHandler ARRAY_HANDLER = PackedArray.getHandler(6);

  /**
   * Converts a hash4j HyperLogLog into a StreamLib HyperLogLogPlus.
   *
   * @param hll hash4j HyperLogLog
   * @return Streamlib HyperLogLogPlus
   */
  private static HyperLogLogPlus toStreamLibHyperLogLogPlus(HyperLogLog hll) {
    byte[] state = hll.getState();
    int p = HyperLogLog.calculateP(state.length);
    HyperLogLogPlus hllPlus = new HyperLogLogPlus(p);
    for (int idx = 0; idx < (1 << p); ++idx) {
      long registerValue = ARRAY_HANDLER.get(state, idx);
      if (registerValue > 0) {
        long representativeHash =
            ((long) idx << -p) | (0xFFFFFFFFFFFFFFFFL >>> p >>> (registerValue - 1));
        hllPlus.offerHashed(representativeHash);
      }
    }
    return hllPlus;
  }

  private static List<Arguments> provideArgumentsForStreamLibCompatibilityTest() {

    long[] cardinalities = {0, 3, 10, 30, 100, 300, 1000, 3000, 10000, 30000, 100000};
    int[] pValues = {8, 10};

    List<Arguments> arguments = new ArrayList<>();
    for (int p : pValues) {
      for (long cardinalityOnlyLegacy : cardinalities) {
        for (long cardinalityOnlyNew : cardinalities) {
          for (long cardinalityBoth : cardinalities) {
            arguments.add(
                Arguments.of(p, cardinalityOnlyLegacy, cardinalityOnlyNew, cardinalityBoth));
          }
        }
      }
    }
    return arguments;
  }

  @ParameterizedTest
  @MethodSource("provideArgumentsForStreamLibCompatibilityTest")
  void streamLibCompatibilityTest(
      int p, long cardinalityOnlyLegacy, long cardinalityOnlyNew, long cardinalityBoth)
      throws CardinalityMergeException {

    HyperLogLogPlus hllLegacy = new HyperLogLogPlus(p);
    HyperLogLog hllNew = HyperLogLog.create(p);

    double relativeStandardErrorInPercent = 1.04 / Math.sqrt(1 << p) * 100;
    double relativeStandardErrorLimit = 5 * relativeStandardErrorInPercent;

    long stringCounter = 0;

    for (long i = 0; i < cardinalityOnlyLegacy; ++i) {
      String s = Long.toString(stringCounter++);
      long hash = MurmurHash.hash64(s);
      hllLegacy.offerHashed(hash);
    }

    for (long i = 0; i < cardinalityOnlyNew; ++i) {
      String s = Long.toString(stringCounter++);
      long hash = MurmurHash.hash64(s);
      hllNew.add(hash);
    }

    for (long i = 0; i < cardinalityBoth; ++i) {
      String s = Long.toString(stringCounter++);
      long hash = MurmurHash.hash64(s);
      hllLegacy.offerHashed(hash);
      hllNew.add(hash);
    }

    assertThat(hllLegacy.cardinality())
        .isCloseTo(
            cardinalityOnlyLegacy + cardinalityBoth, withPercentage(relativeStandardErrorLimit));
    assertThat(hllNew.getDistinctCountEstimate())
        .isCloseTo(
            (double) (cardinalityOnlyNew + cardinalityBoth),
            withPercentage(relativeStandardErrorLimit));

    hllLegacy.addAll(toStreamLibHyperLogLogPlus(hllNew));

    assertThat(hllLegacy.cardinality())
        .isCloseTo(
            cardinalityOnlyLegacy + cardinalityOnlyNew + cardinalityBoth,
            withPercentage(relativeStandardErrorLimit));
  }
}
