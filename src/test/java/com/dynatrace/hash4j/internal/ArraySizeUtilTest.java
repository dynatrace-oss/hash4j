/*
 * Copyright 2025-2026 Dynatrace LLC
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
package com.dynatrace.hash4j.internal;

import static com.dynatrace.hash4j.internal.ArraySizeUtil.increaseArraySize;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import org.junit.jupiter.api.Test;

class ArraySizeUtilTest {

  @Test
  void testIncreaseArraySize() {
    assertThat(increaseArraySize(Integer.MIN_VALUE, Integer.MIN_VALUE)).isEqualTo(8);
    assertThat(increaseArraySize(-1, Integer.MIN_VALUE)).isEqualTo(8);
    assertThat(increaseArraySize(0, Integer.MIN_VALUE)).isEqualTo(8);
    assertThat(increaseArraySize(1, Integer.MIN_VALUE)).isEqualTo(8);
    assertThat(increaseArraySize(2, Integer.MIN_VALUE)).isEqualTo(8);
    assertThat(increaseArraySize(3, Integer.MIN_VALUE)).isEqualTo(8);
    assertThat(increaseArraySize(4, Integer.MIN_VALUE)).isEqualTo(8);
    assertThat(increaseArraySize(5, Integer.MIN_VALUE)).isEqualTo(10);
    assertThat(increaseArraySize(6, Integer.MIN_VALUE)).isEqualTo(12);
    assertThat(increaseArraySize(Integer.MAX_VALUE / 2, Integer.MIN_VALUE))
        .isEqualTo(Integer.MAX_VALUE - 8);
    assertThat(increaseArraySize(Integer.MAX_VALUE / 2 + 1, Integer.MIN_VALUE))
        .isEqualTo(Integer.MAX_VALUE - 8);
    assertThat(increaseArraySize(Integer.MAX_VALUE / 2 + 2, Integer.MIN_VALUE))
        .isEqualTo(Integer.MAX_VALUE - 8);
    assertThat(increaseArraySize(Integer.MAX_VALUE - 9, Integer.MIN_VALUE))
        .isEqualTo(Integer.MAX_VALUE - 8);
    assertThat(increaseArraySize(Integer.MAX_VALUE - 8, Integer.MIN_VALUE))
        .isEqualTo(Integer.MAX_VALUE - 7);
    assertThat(increaseArraySize(Integer.MAX_VALUE - 7, Integer.MIN_VALUE))
        .isEqualTo(Integer.MAX_VALUE - 6);
    assertThat(increaseArraySize(Integer.MAX_VALUE - 6, Integer.MIN_VALUE))
        .isEqualTo(Integer.MAX_VALUE - 5);
    assertThat(increaseArraySize(Integer.MAX_VALUE - 5, Integer.MIN_VALUE))
        .isEqualTo(Integer.MAX_VALUE - 4);
    assertThat(increaseArraySize(Integer.MAX_VALUE - 4, Integer.MIN_VALUE))
        .isEqualTo(Integer.MAX_VALUE - 3);
    assertThat(increaseArraySize(Integer.MAX_VALUE - 3, Integer.MIN_VALUE))
        .isEqualTo(Integer.MAX_VALUE - 2);
    assertThat(increaseArraySize(Integer.MAX_VALUE - 2, Integer.MIN_VALUE))
        .isEqualTo(Integer.MAX_VALUE - 1);
    assertThat(increaseArraySize(Integer.MAX_VALUE - 1, Integer.MIN_VALUE))
        .isEqualTo(Integer.MAX_VALUE);
    assertThatThrownBy(() -> increaseArraySize(Integer.MAX_VALUE, Integer.MIN_VALUE))
        .isInstanceOf(OutOfMemoryError.class);

    assertThat(increaseArraySize(3, 100)).isEqualTo(101);
    assertThat(increaseArraySize(102, 100)).isEqualTo(204);
  }
}
