/*
 * Copyright 2024-2025 Dynatrace LLC
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

import org.junit.jupiter.api.Test;

public class NativeByteArrayByteAccessTest {

  @Test
  void test() {
    byte[] data = {0x01, 0x23, 0x45, 0x67, (byte) 0x89, (byte) 0xab, (byte) 0xcd, (byte) 0xef};

    assertThat(ByteArrayByteAccess.get().getByte(data, 0)).isEqualTo((byte) 0x01L);
    assertThat(ByteArrayByteAccess.get().getByte(data, 1)).isEqualTo((byte) 0x23L);
    assertThat(ByteArrayByteAccess.get().getByte(data, 2)).isEqualTo((byte) 0x45L);
    assertThat(ByteArrayByteAccess.get().getByte(data, 3)).isEqualTo((byte) 0x67L);
    assertThat(ByteArrayByteAccess.get().getByte(data, 4)).isEqualTo((byte) 0x89L);
    assertThat(ByteArrayByteAccess.get().getByte(data, 5)).isEqualTo((byte) 0xabL);
    assertThat(ByteArrayByteAccess.get().getByte(data, 6)).isEqualTo((byte) 0xcdL);
    assertThat(ByteArrayByteAccess.get().getByte(data, 7)).isEqualTo((byte) 0xefL);

    assertThat(ByteArrayByteAccess.get().getByteAsUnsignedInt(data, 0)).isEqualTo(0x01L);
    assertThat(ByteArrayByteAccess.get().getByteAsUnsignedInt(data, 1)).isEqualTo(0x23L);
    assertThat(ByteArrayByteAccess.get().getByteAsUnsignedInt(data, 2)).isEqualTo(0x45L);
    assertThat(ByteArrayByteAccess.get().getByteAsUnsignedInt(data, 3)).isEqualTo(0x67L);
    assertThat(ByteArrayByteAccess.get().getByteAsUnsignedInt(data, 4)).isEqualTo(0x89L);
    assertThat(ByteArrayByteAccess.get().getByteAsUnsignedInt(data, 5)).isEqualTo(0xabL);
    assertThat(ByteArrayByteAccess.get().getByteAsUnsignedInt(data, 6)).isEqualTo(0xcdL);
    assertThat(ByteArrayByteAccess.get().getByteAsUnsignedInt(data, 7)).isEqualTo(0xefL);

    assertThat(ByteArrayByteAccess.get().getIntAsUnsignedLong(data, 0)).isEqualTo(0x67452301L);
    assertThat(ByteArrayByteAccess.get().getIntAsUnsignedLong(data, 1)).isEqualTo(0x89674523L);
    assertThat(ByteArrayByteAccess.get().getIntAsUnsignedLong(data, 2)).isEqualTo(0xab896745L);
    assertThat(ByteArrayByteAccess.get().getIntAsUnsignedLong(data, 3)).isEqualTo(0xcdab8967L);
    assertThat(ByteArrayByteAccess.get().getIntAsUnsignedLong(data, 4)).isEqualTo(0xefcdab89L);

    assertThat(ByteArrayByteAccess.get().getInt(data, 0)).isEqualTo(0x67452301);
    assertThat(ByteArrayByteAccess.get().getInt(data, 1)).isEqualTo(0x89674523);
    assertThat(ByteArrayByteAccess.get().getInt(data, 2)).isEqualTo(0xab896745);
    assertThat(ByteArrayByteAccess.get().getInt(data, 3)).isEqualTo(0xcdab8967);
    assertThat(ByteArrayByteAccess.get().getInt(data, 4)).isEqualTo(0xefcdab89);

    assertThat(ByteArrayByteAccess.get().getLong(data, 0)).isEqualTo(0xefcdab8967452301L);

    {
      byte[] b = new byte[2];
      ByteArrayByteAccess.get().copyToByteArray(data, 3, b, 0, 2);
      assertThat(b).contains(0x67, (byte) 0x89);
    }
  }
}
