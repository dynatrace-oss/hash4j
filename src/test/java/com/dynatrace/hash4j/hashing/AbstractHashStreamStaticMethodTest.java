/*
 * Copyright 2022-2023 Dynatrace LLC
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
package com.dynatrace.hash4j.hashing;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;

class AbstractHashStreamStaticMethodTest {

  @Test
  void testIncreaseArraySize() {
    assertThat(AbstractHashStream.increaseArraySize(1)).isEqualTo(2);
    assertThat(AbstractHashStream.increaseArraySize(2)).isEqualTo(4);
    assertThat(AbstractHashStream.increaseArraySize(3)).isEqualTo(6);
    assertThat(AbstractHashStream.increaseArraySize(Integer.MAX_VALUE / 2))
        .isEqualTo(Integer.MAX_VALUE - 8);
    assertThat(AbstractHashStream.increaseArraySize(Integer.MAX_VALUE / 2 + 1))
        .isEqualTo(Integer.MAX_VALUE - 8);
    assertThat(AbstractHashStream.increaseArraySize(Integer.MAX_VALUE / 2 + 2))
        .isEqualTo(Integer.MAX_VALUE - 8);
    assertThat(AbstractHashStream.increaseArraySize(Integer.MAX_VALUE - 9))
        .isEqualTo(Integer.MAX_VALUE - 8);
    assertThat(AbstractHashStream.increaseArraySize(Integer.MAX_VALUE - 8))
        .isEqualTo(Integer.MAX_VALUE - 7);
    assertThat(AbstractHashStream.increaseArraySize(Integer.MAX_VALUE - 7))
        .isEqualTo(Integer.MAX_VALUE - 6);
    assertThat(AbstractHashStream.increaseArraySize(Integer.MAX_VALUE - 6))
        .isEqualTo(Integer.MAX_VALUE - 5);
    assertThat(AbstractHashStream.increaseArraySize(Integer.MAX_VALUE - 5))
        .isEqualTo(Integer.MAX_VALUE - 4);
    assertThat(AbstractHashStream.increaseArraySize(Integer.MAX_VALUE - 4))
        .isEqualTo(Integer.MAX_VALUE - 3);
    assertThat(AbstractHashStream.increaseArraySize(Integer.MAX_VALUE - 3))
        .isEqualTo(Integer.MAX_VALUE - 2);
    assertThat(AbstractHashStream.increaseArraySize(Integer.MAX_VALUE - 2))
        .isEqualTo(Integer.MAX_VALUE - 1);
    assertThat(AbstractHashStream.increaseArraySize(Integer.MAX_VALUE - 1))
        .isEqualTo(Integer.MAX_VALUE);
    assertThatThrownBy(() -> AbstractHashStream.increaseArraySize(Integer.MAX_VALUE))
        .isInstanceOf(OutOfMemoryError.class);
  }
}
