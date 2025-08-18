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

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.util.SplittableRandom;
import org.junit.jupiter.api.Test;

public class ByteAccessTest {

  @Test
  void testGet() {
    byte[] data = {0x01, 0x23, 0x45, 0x67, (byte) 0x89, (byte) 0xab, (byte) 0xcd, (byte) 0xef};

    ByteAccess<byte[]> byteAccess = (d, idx) -> d[(int) idx];

    assertThat(byteAccess.getByte(data, 0)).isEqualTo((byte) 0x01L);
    assertThat(byteAccess.getByte(data, 1)).isEqualTo((byte) 0x23L);
    assertThat(byteAccess.getByte(data, 2)).isEqualTo((byte) 0x45L);
    assertThat(byteAccess.getByte(data, 3)).isEqualTo((byte) 0x67L);
    assertThat(byteAccess.getByte(data, 4)).isEqualTo((byte) 0x89L);
    assertThat(byteAccess.getByte(data, 5)).isEqualTo((byte) 0xabL);
    assertThat(byteAccess.getByte(data, 6)).isEqualTo((byte) 0xcdL);
    assertThat(byteAccess.getByte(data, 7)).isEqualTo((byte) 0xefL);

    assertThat(byteAccess.getByteAsUnsignedInt(data, 0)).isEqualTo(0x01L);
    assertThat(byteAccess.getByteAsUnsignedInt(data, 1)).isEqualTo(0x23L);
    assertThat(byteAccess.getByteAsUnsignedInt(data, 2)).isEqualTo(0x45L);
    assertThat(byteAccess.getByteAsUnsignedInt(data, 3)).isEqualTo(0x67L);
    assertThat(byteAccess.getByteAsUnsignedInt(data, 4)).isEqualTo(0x89L);
    assertThat(byteAccess.getByteAsUnsignedInt(data, 5)).isEqualTo(0xabL);
    assertThat(byteAccess.getByteAsUnsignedInt(data, 6)).isEqualTo(0xcdL);
    assertThat(byteAccess.getByteAsUnsignedInt(data, 7)).isEqualTo(0xefL);

    assertThat(byteAccess.getIntAsUnsignedLong(data, 0)).isEqualTo(0x67452301L);
    assertThat(byteAccess.getIntAsUnsignedLong(data, 1)).isEqualTo(0x89674523L);
    assertThat(byteAccess.getIntAsUnsignedLong(data, 2)).isEqualTo(0xab896745L);
    assertThat(byteAccess.getIntAsUnsignedLong(data, 3)).isEqualTo(0xcdab8967L);
    assertThat(byteAccess.getIntAsUnsignedLong(data, 4)).isEqualTo(0xefcdab89L);

    assertThat(byteAccess.getInt(data, 0)).isEqualTo(0x67452301);
    assertThat(byteAccess.getInt(data, 1)).isEqualTo(0x89674523);
    assertThat(byteAccess.getInt(data, 2)).isEqualTo(0xab896745);
    assertThat(byteAccess.getInt(data, 3)).isEqualTo(0xcdab8967);
    assertThat(byteAccess.getInt(data, 4)).isEqualTo(0xefcdab89);

    assertThat(byteAccess.getLong(data, 0)).isEqualTo(0xefcdab8967452301L);
  }

  @Test
  void testCopy() {
    ByteAccess<byte[]> byteAccess = (d, idx) -> d[(int) idx];
    SplittableRandom random = new SplittableRandom(0x134c4aab2d8fd924L);
    int len = 27;
    byte[] actual = new byte[len];
    byte[] expected = new byte[len];
    byte[] source = new byte[len];

    random.nextBytes(expected);
    System.arraycopy(expected, 0, actual, 0, len);

    for (int srcPos = 0; srcPos < len; ++srcPos) {
      for (int destPos = 0; destPos < len; ++destPos) {
        for (int copyLen = 0; srcPos + copyLen <= len && destPos + copyLen <= len; ++copyLen) {
          random.nextBytes(source);

          byteAccess.copyToByteArray(source, srcPos, actual, destPos, copyLen);
          System.arraycopy(source, srcPos, expected, destPos, copyLen);

          assertThat(actual).isEqualTo(expected);
        }
      }
    }
  }
}
