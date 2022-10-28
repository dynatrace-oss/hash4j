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
package com.dynatrace.hash4j.distinctcount;

import static com.dynatrace.hash4j.distinctcount.DistinctCountUtil.checkPrecisionParameter;
import static com.dynatrace.hash4j.distinctcount.DistinctCountUtil.isUnsignedPowerOfTwo;
import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;

class DistinctCountUtilTest {

  @Test
  void testIsUnsignedPowerOfTwo() {
    for (int exponent = 0; exponent < 32; exponent++) {
      assertThat(isUnsignedPowerOfTwo(1 << exponent)).isTrue();
    }
    assertThat(isUnsignedPowerOfTwo(0)).isTrue();
    for (int i = -1000; i < 0; ++i) {
      assertThat(isUnsignedPowerOfTwo(i)).isFalse();
    }
    assertThat(isUnsignedPowerOfTwo(Integer.MIN_VALUE)).isTrue();
    assertThat(isUnsignedPowerOfTwo(Integer.MAX_VALUE)).isFalse();
  }

  @Test
  void testCheckPrecisionParameter() {
    assertThatIllegalArgumentException().isThrownBy(() -> checkPrecisionParameter(1, 3, 5));
    assertThatIllegalArgumentException().isThrownBy(() -> checkPrecisionParameter(2, 3, 5));
    assertThatNoException().isThrownBy(() -> checkPrecisionParameter(3, 3, 5));
    assertThatNoException().isThrownBy(() -> checkPrecisionParameter(4, 3, 5));
    assertThatNoException().isThrownBy(() -> checkPrecisionParameter(5, 3, 5));
    assertThatIllegalArgumentException().isThrownBy(() -> checkPrecisionParameter(6, 3, 5));
    assertThatIllegalArgumentException().isThrownBy(() -> checkPrecisionParameter(7, 3, 5));
  }
}
