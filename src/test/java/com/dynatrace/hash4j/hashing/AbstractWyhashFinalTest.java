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
package com.dynatrace.hash4j.hashing;

import static com.dynatrace.hash4j.hashing.AbstractWyhashFinal.equalsHelper;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AbstractWyhashFinalTest {

  private static final byte[] B0 = {0};
  private static final byte[] B1 = {1};

  @Test
  void testEqualsHelper() {
    assertThat(equalsHelper(0L, 0L, 0L, 0L, 0L, 0L, 0, 0, 0L, 0L, B0, B0)).isTrue();
    assertThat(equalsHelper(1L, 0L, 0L, 0L, 0L, 0L, 0, 0, 0L, 0L, B0, B0)).isFalse();
    assertThat(equalsHelper(0L, 0L, 1L, 0L, 0L, 0L, 0, 0, 0L, 0L, B0, B0)).isFalse();
    assertThat(equalsHelper(0L, 0L, 0L, 0L, 1L, 0L, 0, 0, 0L, 0L, B0, B0)).isFalse();
    assertThat(equalsHelper(0L, 0L, 0L, 0L, 0L, 0L, 1, 0, 0L, 0L, B0, B0)).isFalse();
    assertThat(equalsHelper(0L, 0L, 0L, 0L, 0L, 0L, 0, 0, 1L, 0L, B0, B0)).isFalse();
    assertThat(equalsHelper(0L, 0L, 0L, 0L, 0L, 0L, 1, 1, 0L, 0L, B1, B0)).isFalse();
  }
}
