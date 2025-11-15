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
package com.dynatrace.hash4j.random;

import static com.dynatrace.hash4j.random.RandomExponentialUtil.getX;
import static com.dynatrace.hash4j.random.RandomExponentialUtil.getY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Percentage.withPercentage;

import org.junit.jupiter.api.Test;

class RandomExponentialUtilTest {

  @Test
  void testSpecialValues() {
    assertThat(getX(256)).isZero();
    assertThat(getY(256)).isOne();
    assertThat(getY(0)).isZero();
  }

  @Test
  void testXYRelationship() {
    for (int i = 1; i <= 256; ++i) {
      double xi = getX(i);
      double yi = getY(i);
      assertThat(yi).isCloseTo(StrictMath.exp(-xi), withPercentage(1e-13));
      assertThat(xi).isCloseTo(-StrictMath.log(yi), withPercentage(1e-13));
    }
  }

  @Test
  void testEqualAreaOfZigguratBlocks() {
    double expectedArea = getX(0) * getY(1);
    for (int i = 2; i <= 256; ++i) {
      assertThat(getX(i - 1) * (getY(i) - getY(i - 1)))
          .isCloseTo(expectedArea, withPercentage(1e-11));
    }
  }

  @Test
  void testTailProbability() {
    double expectedTailProbability = StrictMath.exp(-getX(1));

    double probabilityForNextIterationWithoutIncreasingShift =
        255.
            / 256.
            * (1 - (1 - StrictMath.exp(-getX(1)) - getX(1) * getY(1)) / (255. * getX(0) * getY(1)));
    double probabilityThatRandomValueIsTakenFromTail = 1. / 256. * (getX(0) - getX(1)) / getX(0);

    assertThat(
            probabilityThatRandomValueIsTakenFromTail
                / (1. - probabilityForNextIterationWithoutIncreasingShift))
        .isCloseTo(expectedTailProbability, withPercentage(1e-13));
  }
}
