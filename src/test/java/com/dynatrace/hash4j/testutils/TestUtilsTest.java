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
package com.dynatrace.hash4j.testutils;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;
import org.junit.jupiter.api.Test;

public class TestUtilsTest {

  private static final VarHandle CHAR_HANDLE =
      MethodHandles.byteArrayViewVarHandle(char[].class, ByteOrder.LITTLE_ENDIAN);

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
    assertEquals((char) (0x2301), charSequence.charAt(0));
    assertEquals((char) (0x6745), charSequence.charAt(1));
    assertEquals((char) (0xab89), charSequence.charAt(2));
    assertEquals((char) (0xefcd), charSequence.charAt(3));
    assertEquals(
        charSequence.toString(),
        new StringBuilder()
            .append((char) 0x2301)
            .append((char) 0x6745)
            .append((char) 0xab89)
            .append((char) 0xefcd)
            .toString());

    byte[] actual = new byte[expected.length];
    for (int i = 0; i < charSequence.length(); ++i) {
      char c = charSequence.charAt(i);
      CHAR_HANDLE.set(actual, 2 * i, c);
    }
    assertArrayEquals(expected, actual);
  }
}
