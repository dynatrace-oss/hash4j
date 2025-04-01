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
package com.dynatrace.hash4j.testutils;

import static com.dynatrace.hash4j.helper.ByteArrayUtil.setChar;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TestUtilsTest {

  @Test
  void testByteArrayToCharSequence() {
    byte[] expected = {
      (byte) 0x01,
      (byte) 0x23,
      (byte) 0x45,
      (byte) 0x67,
      (byte) 0x89,
      (byte) 0xab,
      (byte) 0xcd,
      (byte) 0xef
    };
    CharSequence charSequence = TestUtils.byteArrayToCharSequence(expected);
    assertThat(charSequence.charAt(0)).isEqualTo((char) 0x2301);
    assertThat(charSequence.charAt(1)).isEqualTo((char) 0x6745);
    assertThat(charSequence.charAt(2)).isEqualTo((char) 0xab89);
    assertThat(charSequence.charAt(3)).isEqualTo((char) 0xefcd);
    assertThat(charSequence)
        .hasToString(String.valueOf((char) 0x2301) + (char) 0x6745 + (char) 0xab89 + (char) 0xefcd);

    byte[] actual = new byte[expected.length];
    for (int i = 0; i < charSequence.length(); ++i) {
      char c = charSequence.charAt(i);
      setChar(actual, 2 * i, c);
    }
    assertThat(actual).isEqualTo(expected);
  }

  @Test
  void testByteArrayToLong() {
    byte[] b = {
      (byte) 0xef,
      (byte) 0xcd,
      (byte) 0xab,
      (byte) 0x89,
      (byte) 0x67,
      (byte) 0x45,
      (byte) 0x23,
      (byte) 0x01
    };
    long l = 0x0123456789abcdefL;
    assertThat(TestUtils.byteArrayToLong(b)).isEqualTo(l);
  }

  @Test
  void testLongToByteArray() {
    byte[] b = {
      (byte) 0xef,
      (byte) 0xcd,
      (byte) 0xab,
      (byte) 0x89,
      (byte) 0x67,
      (byte) 0x45,
      (byte) 0x23,
      (byte) 0x01
    };
    long l = 0x0123456789abcdefL;
    assertThat(TestUtils.longToByteArray(l)).isEqualTo(b);
  }
}
