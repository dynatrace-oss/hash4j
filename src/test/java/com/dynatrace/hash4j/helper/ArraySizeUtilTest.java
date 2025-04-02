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
package com.dynatrace.hash4j.helper;

import static com.dynatrace.hash4j.helper.ArraySizeUtil.increaseArraySize;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import org.junit.jupiter.api.Test;

public class ArraySizeUtilTest {

  @Test
  void testIncreaseArraySize() {
    assertThat(increaseArraySize(0)).isEqualTo(1);
    assertThat(increaseArraySize(1)).isEqualTo(2);
    assertThat(increaseArraySize(2)).isEqualTo(4);
    assertThat(increaseArraySize(3)).isEqualTo(6);
    assertThat(increaseArraySize(Integer.MAX_VALUE / 2)).isEqualTo(Integer.MAX_VALUE - 8);
    assertThat(increaseArraySize(Integer.MAX_VALUE / 2 + 1)).isEqualTo(Integer.MAX_VALUE - 8);
    assertThat(increaseArraySize(Integer.MAX_VALUE / 2 + 2)).isEqualTo(Integer.MAX_VALUE - 8);
    assertThat(increaseArraySize(Integer.MAX_VALUE - 9)).isEqualTo(Integer.MAX_VALUE - 8);
    assertThat(increaseArraySize(Integer.MAX_VALUE - 8)).isEqualTo(Integer.MAX_VALUE - 7);
    assertThat(increaseArraySize(Integer.MAX_VALUE - 7)).isEqualTo(Integer.MAX_VALUE - 6);
    assertThat(increaseArraySize(Integer.MAX_VALUE - 6)).isEqualTo(Integer.MAX_VALUE - 5);
    assertThat(increaseArraySize(Integer.MAX_VALUE - 5)).isEqualTo(Integer.MAX_VALUE - 4);
    assertThat(increaseArraySize(Integer.MAX_VALUE - 4)).isEqualTo(Integer.MAX_VALUE - 3);
    assertThat(increaseArraySize(Integer.MAX_VALUE - 3)).isEqualTo(Integer.MAX_VALUE - 2);
    assertThat(increaseArraySize(Integer.MAX_VALUE - 2)).isEqualTo(Integer.MAX_VALUE - 1);
    assertThat(increaseArraySize(Integer.MAX_VALUE - 1)).isEqualTo(Integer.MAX_VALUE);
    assertThatThrownBy(() -> increaseArraySize(Integer.MAX_VALUE))
        .isInstanceOf(OutOfMemoryError.class);
  }
}
