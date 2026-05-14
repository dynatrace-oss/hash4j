/*
 * Copyright 2026 Dynatrace LLC
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

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class StringByteAccessTest {

  // =======================================================================
  // Shared test data
  // =======================================================================

  // 5 chars whose little-endian byte pairs produce the familiar test sequence:
  //   char 0: 0x2301  -> bytes  0x01  0x23
  //   char 1: 0x6745  -> bytes  0x45  0x67
  //   char 2: 0xAB89  -> bytes  0x89  0xAB   (char value > 0x7F, exercises sign-extension paths)
  //   char 3: 0xEFCD  -> bytes  0xCD  0xEF   (bit-15 set, makes the long negative mid-computation)
  //   char 4: 0x1234  -> bytes  0x34  0x12
  // Complete byte sequence (10 bytes): 01 23 45 67 89 AB CD EF 34 12
  private static final String UTF16_DATA =
      new String(new char[] {'\u2301', '\u6745', '\uAB89', '\uEFCD', '\u1234'});

  private static final byte[] UTF16_BYTES = {
    0x01, 0x23, 0x45, 0x67, (byte) 0x89, (byte) 0xAB, (byte) 0xCD, (byte) 0xEF, 0x34, 0x12
  };

  // Latin-1 string: all chars have values <= 0xFF.
  // The ByteAccess contract exposes the UTF-16 LE byte view (2 bytes per char),
  // so each char c produces [c, 0x00].
  //
  //   char 0: 0x61 ('a') -> bytes 0x61 0x00
  //   char 1: 0xA2       -> bytes 0xA2 0x00
  //   char 2: 0xB3       -> bytes 0xB3 0x00
  //   char 3: 0xC4       -> bytes 0xC4 0x00
  //   char 4: 0xD5       -> bytes 0xD5 0x00
  //
  // Complete UTF-16 LE byte sequence (10 bytes): 61 00 A2 00 B3 00 C4 00 D5 00
  private static final String LATIN1_DATA = "a\u00A2\u00B3\u00C4\u00D5";

  private static final byte[] LATIN1_BYTES = {
    0x61, 0x00, (byte) 0xA2, 0x00, (byte) 0xB3, 0x00, (byte) 0xC4, 0x00, (byte) 0xD5, 0x00
  };

  // =======================================================================
  // StringByteAccess routing tests
  // =======================================================================

  @Test
  void testGetReturnsNonNullForAsciiString() {
    String s = "hello";
    ByteAccess<String> access = StringByteAccess.get(s);
    assertThat(access).isNotNull();
  }

  @Test
  void testGetReturnsNonNullForNonLatin1String() {
    String s = "\u4E2D\u6587"; // Chinese characters
    ByteAccess<String> access = StringByteAccess.get(s);
    assertThat(access).isNotNull();
  }

  @Test
  void testGetForInternalByteArrayReturnsLatin1() {
    String s = "abc";
    byte[] latin1Array = new byte[s.length()]; // length matches -> Latin1
    ByteAccess<String> access = StringByteAccess.getForInternalByteArray(s, latin1Array);
    assertThat(access).isSameAs(StringByteAccess.Latin1.get());
  }

  @Test
  void testGetForInternalByteArrayReturnsUTF16() {
    String s = "abc";
    byte[] utf16Array = new byte[s.length() * 2]; // length doesn't match -> UTF16
    ByteAccess<String> access = StringByteAccess.getForInternalByteArray(s, utf16Array);
    assertThat(access).isSameAs(StringByteAccess.UTF16.get());
  }

  @Test
  void testGetForInternalByteArrayReturnsDefaultWhenNull() {
    String s = "abc";
    ByteAccess<String> access = StringByteAccess.getForInternalByteArray(s, null);
    assertThat(access).isSameAs(StringByteAccess.Default.get());
  }

  @Test
  void testForStringDelegatesToGet() {
    String latin1 = "hello";
    ByteAccess<String> access = StringByteAccess.get(latin1);
    assertThat(access).isNotNull();

    String nonLatin1 = "\u4E2D\u6587";
    ByteAccess<String> access2 = StringByteAccess.get(nonLatin1);
    assertThat(access2).isNotNull();
  }

  // =======================================================================
  // Default implementation tests
  // =======================================================================

  @Test
  void testDefaultGetByte() {
    ByteAccess<String> access = StringByteAccess.Default.get();
    for (int i = 0; i < UTF16_BYTES.length; i++) {
      assertThat(access.getByte(UTF16_DATA, i)).as("getByte(%d)", i).isEqualTo(UTF16_BYTES[i]);
    }
  }

  @Test
  void testDefaultGetByteAsUnsignedInt() {
    ByteAccess<String> access = StringByteAccess.Default.get();
    for (int i = 0; i < UTF16_BYTES.length; i++) {
      assertThat(access.getByteAsUnsignedInt(UTF16_DATA, i))
          .as("getByteAsUnsignedInt(%d)", i)
          .isEqualTo(UTF16_BYTES[i] & 0xFF);
    }
  }

  @Test
  void testDefaultGetByteAsUnsignedLong() {
    ByteAccess<String> access = StringByteAccess.Default.get();
    for (int i = 0; i < UTF16_BYTES.length; i++) {
      assertThat(access.getByteAsUnsignedLong(UTF16_DATA, i))
          .as("getByteAsUnsignedLong(%d)", i)
          .isEqualTo(UTF16_BYTES[i] & 0xFFL);
    }
  }

  @Test
  void testDefaultGetInt() {
    ByteAccess<String> access = StringByteAccess.Default.get();

    // even (char-aligned) positions
    assertThat(access.getInt(UTF16_DATA, 0)).isEqualTo(0x67452301);
    assertThat(access.getInt(UTF16_DATA, 2)).isEqualTo(0xAB896745);
    assertThat(access.getInt(UTF16_DATA, 4)).isEqualTo(0xEFCDAB89);
    assertThat(access.getInt(UTF16_DATA, 6)).isEqualTo(0x1234EFCD);

    // odd (unaligned) positions — exercises the >>> shift path
    assertThat(access.getInt(UTF16_DATA, 1)).isEqualTo(0x89674523);
    assertThat(access.getInt(UTF16_DATA, 3)).isEqualTo(0xCDAB8967);
    assertThat(access.getInt(UTF16_DATA, 5)).isEqualTo(0x34EFCDAB);
  }

  @Test
  void testDefaultGetIntConsistency() {
    // getInt must match 4 individual getByte calls assembled as little-endian
    ByteAccess<String> access = StringByteAccess.Default.get();
    for (int i = 0; i <= UTF16_BYTES.length - 4; i++) {
      int expected =
          (UTF16_BYTES[i] & 0xFF)
              | ((UTF16_BYTES[i + 1] & 0xFF) << 8)
              | ((UTF16_BYTES[i + 2] & 0xFF) << 16)
              | (UTF16_BYTES[i + 3] << 24);
      assertThat(access.getInt(UTF16_DATA, i)).as("getInt(%d)", i).isEqualTo(expected);
    }
  }

  @Test
  void testDefaultGetIntAsUnsignedLong() {
    ByteAccess<String> access = StringByteAccess.Default.get();
    for (int i = 0; i <= UTF16_BYTES.length - 4; i++) {
      assertThat(access.getIntAsUnsignedLong(UTF16_DATA, i))
          .as("getIntAsUnsignedLong(%d)", i)
          .isEqualTo(access.getInt(UTF16_DATA, i) & 0xFFFFFFFFL);
    }
  }

  @Test
  void testDefaultGetLong() {
    ByteAccess<String> access = StringByteAccess.Default.get();

    // idx=0: even, reads bytes 0..7
    assertThat(access.getLong(UTF16_DATA, 0)).isEqualTo(0xEFCDAB8967452301L);
    // idx=1: odd — exercises the >>> 8 path on a negative long
    assertThat(access.getLong(UTF16_DATA, 1)).isEqualTo(0x34EFCDAB89674523L);
    // idx=2: even, reads bytes 2..9
    assertThat(access.getLong(UTF16_DATA, 2)).isEqualTo(0x1234EFCDAB896745L);
  }

  @Test
  void testDefaultGetLongConsistency() {
    // getLong must equal two adjacent getInt values assembled as little-endian 64-bit
    ByteAccess<String> access = StringByteAccess.Default.get();
    for (int i = 0; i <= UTF16_BYTES.length - 8; i++) {
      long expectedLow = access.getInt(UTF16_DATA, i) & 0xFFFFFFFFL;
      long expectedHigh = access.getInt(UTF16_DATA, i + 4) & 0xFFFFFFFFL;
      long expected = expectedLow | (expectedHigh << 32);
      assertThat(access.getLong(UTF16_DATA, i)).as("getLong(%d)", i).isEqualTo(expected);
    }
  }

  @Test
  void testDefaultCopyToByteArray() {
    ByteAccess<String> access = StringByteAccess.Default.get();
    int srcLen = UTF16_BYTES.length;
    byte[] actual = new byte[srcLen];
    byte[] expected = new byte[srcLen];

    for (int srcPos = 0; srcPos < srcLen; srcPos++) {
      for (int destPos = 0; destPos < srcLen; destPos++) {
        for (int copyLen = 0;
            srcPos + copyLen <= srcLen && destPos + copyLen <= srcLen;
            copyLen++) {
          for (int j = 0; j < srcLen; j++) {
            actual[j] = (byte) j;
            expected[j] = (byte) j;
          }

          access.copyToByteArray(UTF16_DATA, srcPos, actual, destPos, copyLen);
          System.arraycopy(UTF16_BYTES, srcPos, expected, destPos, copyLen);

          assertThat(actual)
              .as("copyToByteArray(srcPos=%d, destPos=%d, len=%d)", srcPos, destPos, copyLen)
              .isEqualTo(expected);
        }
      }
    }
  }

  // =======================================================================
  // Latin1 implementation tests
  // =======================================================================

  @Test
  void testLatin1GetByte() {
    ByteAccess<String> access = StringByteAccess.Latin1.get();
    for (int i = 0; i < LATIN1_BYTES.length; i++) {
      assertThat(access.getByte(LATIN1_DATA, i)).as("getByte(%d)", i).isEqualTo(LATIN1_BYTES[i]);
    }
  }

  @Test
  void testLatin1GetByteAsUnsignedInt() {
    ByteAccess<String> access = StringByteAccess.Latin1.get();
    for (int i = 0; i < LATIN1_BYTES.length; i++) {
      assertThat(access.getByteAsUnsignedInt(LATIN1_DATA, i))
          .as("getByteAsUnsignedInt(%d)", i)
          .isEqualTo(LATIN1_BYTES[i] & 0xFF);
    }
  }

  @Test
  void testLatin1GetByteAsUnsignedLong() {
    ByteAccess<String> access = StringByteAccess.Latin1.get();
    for (int i = 0; i < LATIN1_BYTES.length; i++) {
      assertThat(access.getByteAsUnsignedLong(LATIN1_DATA, i))
          .as("getByteAsUnsignedLong(%d)", i)
          .isEqualTo(LATIN1_BYTES[i] & 0xFFL);
    }
  }

  @Test
  void testLatin1GetInt() {
    ByteAccess<String> access = StringByteAccess.Latin1.get();

    // even (char-aligned) positions
    assertThat(access.getInt(LATIN1_DATA, 0)).isEqualTo(0x00A20061);
    assertThat(access.getInt(LATIN1_DATA, 2)).isEqualTo(0x00B300A2);
    assertThat(access.getInt(LATIN1_DATA, 4)).isEqualTo(0x00C400B3);
    assertThat(access.getInt(LATIN1_DATA, 6)).isEqualTo(0x00D500C4);

    // odd (unaligned) positions
    assertThat(access.getInt(LATIN1_DATA, 1)).isEqualTo(0xB300A200);
    assertThat(access.getInt(LATIN1_DATA, 3)).isEqualTo(0xC400B300);
    assertThat(access.getInt(LATIN1_DATA, 5)).isEqualTo(0xD500C400);
  }

  @Test
  void testLatin1GetIntConsistency() {
    // getInt must match 4 individual getByte calls assembled as little-endian
    ByteAccess<String> access = StringByteAccess.Latin1.get();
    for (int i = 0; i <= LATIN1_BYTES.length - 4; i++) {
      int expected =
          (LATIN1_BYTES[i] & 0xFF)
              | ((LATIN1_BYTES[i + 1] & 0xFF) << 8)
              | ((LATIN1_BYTES[i + 2] & 0xFF) << 16)
              | (LATIN1_BYTES[i + 3] << 24);
      assertThat(access.getInt(LATIN1_DATA, i)).as("getInt(%d)", i).isEqualTo(expected);
    }
  }

  @Test
  void testLatin1GetIntAsUnsignedLong() {
    ByteAccess<String> access = StringByteAccess.Latin1.get();
    for (int i = 0; i <= LATIN1_BYTES.length - 4; i++) {
      assertThat(access.getIntAsUnsignedLong(LATIN1_DATA, i))
          .as("getIntAsUnsignedLong(%d)", i)
          .isEqualTo(access.getInt(LATIN1_DATA, i) & 0xFFFFFFFFL);
    }
  }

  @Test
  void testLatin1GetLong() {
    ByteAccess<String> access = StringByteAccess.Latin1.get();

    // idx 0 (even): bytes [0x61, 0x00, 0xA2, 0x00, 0xB3, 0x00, 0xC4, 0x00]
    assertThat(access.getLong(LATIN1_DATA, 0)).isEqualTo(0x00C400B300A20061L);
    // idx 1 (odd): bytes [0x00, 0xA2, 0x00, 0xB3, 0x00, 0xC4, 0x00, 0xD5]
    assertThat(access.getLong(LATIN1_DATA, 1)).isEqualTo(0xD500C400B300A200L);
    // idx 2 (even): bytes [0xA2, 0x00, 0xB3, 0x00, 0xC4, 0x00, 0xD5, 0x00]
    assertThat(access.getLong(LATIN1_DATA, 2)).isEqualTo(0x00D500C400B300A2L);
  }

  @Test
  void testLatin1GetLongConsistency() {
    // getLong must equal two adjacent getInt values assembled as little-endian 64-bit
    ByteAccess<String> access = StringByteAccess.Latin1.get();
    for (int i = 0; i <= LATIN1_BYTES.length - 8; i++) {
      long expectedLow = access.getInt(LATIN1_DATA, i) & 0xFFFFFFFFL;
      long expectedHigh = access.getInt(LATIN1_DATA, i + 4) & 0xFFFFFFFFL;
      long expected = expectedLow | (expectedHigh << 32);
      assertThat(access.getLong(LATIN1_DATA, i)).as("getLong(%d)", i).isEqualTo(expected);
    }
  }

  @Test
  void testLatin1CopyToByteArray() {
    ByteAccess<String> access = StringByteAccess.Latin1.get();
    int srcLen = LATIN1_BYTES.length;
    byte[] actual = new byte[srcLen];
    byte[] expected = new byte[srcLen];

    for (int srcPos = 0; srcPos < srcLen; srcPos++) {
      for (int destPos = 0; destPos < srcLen; destPos++) {
        for (int copyLen = 0;
            srcPos + copyLen <= srcLen && destPos + copyLen <= srcLen;
            copyLen++) {
          for (int j = 0; j < srcLen; j++) {
            actual[j] = (byte) j;
            expected[j] = (byte) j;
          }

          access.copyToByteArray(LATIN1_DATA, srcPos, actual, destPos, copyLen);
          System.arraycopy(LATIN1_BYTES, srcPos, expected, destPos, copyLen);

          assertThat(actual)
              .as("copyToByteArray(srcPos=%d, destPos=%d, len=%d)", srcPos, destPos, copyLen)
              .isEqualTo(expected);
        }
      }
    }
  }

  @Test
  void testLatin1ConsistencyWithDefault() {
    // For a pure Latin-1 string both implementations must produce identical results
    ByteAccess<String> latin1 = StringByteAccess.Latin1.get();
    ByteAccess<String> def = StringByteAccess.Default.get();

    for (int i = 0; i < LATIN1_BYTES.length; i++) {
      assertThat(latin1.getByte(LATIN1_DATA, i))
          .as("getByte(%d)", i)
          .isEqualTo(def.getByte(LATIN1_DATA, i));
      assertThat(latin1.getByteAsUnsignedInt(LATIN1_DATA, i))
          .as("getByteAsUnsignedInt(%d)", i)
          .isEqualTo(def.getByteAsUnsignedInt(LATIN1_DATA, i));
      assertThat(latin1.getByteAsUnsignedLong(LATIN1_DATA, i))
          .as("getByteAsUnsignedLong(%d)", i)
          .isEqualTo(def.getByteAsUnsignedLong(LATIN1_DATA, i));
    }
    for (int i = 0; i <= LATIN1_BYTES.length - 4; i++) {
      assertThat(latin1.getInt(LATIN1_DATA, i))
          .as("getInt(%d)", i)
          .isEqualTo(def.getInt(LATIN1_DATA, i));
      assertThat(latin1.getIntAsUnsignedLong(LATIN1_DATA, i))
          .as("getIntAsUnsignedLong(%d)", i)
          .isEqualTo(def.getIntAsUnsignedLong(LATIN1_DATA, i));
    }
    for (int i = 0; i <= LATIN1_BYTES.length - 8; i++) {
      assertThat(latin1.getLong(LATIN1_DATA, i))
          .as("getLong(%d)", i)
          .isEqualTo(def.getLong(LATIN1_DATA, i));
    }
  }

  // =======================================================================
  // UTF16 implementation tests
  // =======================================================================

  @Test
  void testUTF16GetByte() {
    ByteAccess<String> access = StringByteAccess.UTF16.get();
    for (int i = 0; i < UTF16_BYTES.length; i++) {
      assertThat(access.getByte(UTF16_DATA, i)).as("getByte(%d)", i).isEqualTo(UTF16_BYTES[i]);
    }
  }

  @Test
  void testUTF16GetByteAsUnsignedInt() {
    ByteAccess<String> access = StringByteAccess.UTF16.get();
    for (int i = 0; i < UTF16_BYTES.length; i++) {
      assertThat(access.getByteAsUnsignedInt(UTF16_DATA, i))
          .as("getByteAsUnsignedInt(%d)", i)
          .isEqualTo(UTF16_BYTES[i] & 0xFF);
    }
  }

  @Test
  void testUTF16GetByteAsUnsignedLong() {
    ByteAccess<String> access = StringByteAccess.UTF16.get();
    for (int i = 0; i < UTF16_BYTES.length; i++) {
      assertThat(access.getByteAsUnsignedLong(UTF16_DATA, i))
          .as("getByteAsUnsignedLong(%d)", i)
          .isEqualTo(UTF16_BYTES[i] & 0xFFL);
    }
  }

  @Test
  void testUTF16GetInt() {
    ByteAccess<String> access = StringByteAccess.UTF16.get();

    assertThat(access.getInt(UTF16_DATA, 0)).isEqualTo(0x67452301);
    assertThat(access.getInt(UTF16_DATA, 1)).isEqualTo(0x89674523);
    assertThat(access.getInt(UTF16_DATA, 2)).isEqualTo(0xAB896745);
    assertThat(access.getInt(UTF16_DATA, 3)).isEqualTo(0xCDAB8967);
    assertThat(access.getInt(UTF16_DATA, 4)).isEqualTo(0xEFCDAB89);
    assertThat(access.getInt(UTF16_DATA, 5)).isEqualTo(0x34EFCDAB);
    assertThat(access.getInt(UTF16_DATA, 6)).isEqualTo(0x1234EFCD);
  }

  @Test
  void testUTF16GetIntConsistency() {
    // getInt must match 4 individual getByte calls assembled as little-endian
    ByteAccess<String> access = StringByteAccess.UTF16.get();
    for (int i = 0; i <= UTF16_BYTES.length - 4; i++) {
      int expected =
          (UTF16_BYTES[i] & 0xFF)
              | ((UTF16_BYTES[i + 1] & 0xFF) << 8)
              | ((UTF16_BYTES[i + 2] & 0xFF) << 16)
              | (UTF16_BYTES[i + 3] << 24);
      assertThat(access.getInt(UTF16_DATA, i)).as("getInt(%d)", i).isEqualTo(expected);
    }
  }

  @Test
  void testUTF16GetIntAsUnsignedLong() {
    ByteAccess<String> access = StringByteAccess.UTF16.get();
    for (int i = 0; i <= UTF16_BYTES.length - 4; i++) {
      assertThat(access.getIntAsUnsignedLong(UTF16_DATA, i))
          .as("getIntAsUnsignedLong(%d)", i)
          .isEqualTo(access.getInt(UTF16_DATA, i) & 0xFFFFFFFFL);
    }
  }

  @Test
  void testUTF16GetLong() {
    ByteAccess<String> access = StringByteAccess.UTF16.get();

    // idx 0: bytes 0..7
    assertThat(access.getLong(UTF16_DATA, 0)).isEqualTo(0xEFCDAB8967452301L);
    // idx 1: bytes 1..8
    assertThat(access.getLong(UTF16_DATA, 1)).isEqualTo(0x34EFCDAB89674523L);
    // idx 2: bytes 2..9
    assertThat(access.getLong(UTF16_DATA, 2)).isEqualTo(0x1234EFCDAB896745L);
  }

  @Test
  void testUTF16GetLongConsistency() {
    // getLong must equal two adjacent getInt values assembled as little-endian 64-bit
    ByteAccess<String> access = StringByteAccess.UTF16.get();
    for (int i = 0; i <= UTF16_BYTES.length - 8; i++) {
      long expectedLow = access.getInt(UTF16_DATA, i) & 0xFFFFFFFFL;
      long expectedHigh = access.getInt(UTF16_DATA, i + 4) & 0xFFFFFFFFL;
      long expected = expectedLow | (expectedHigh << 32);
      assertThat(access.getLong(UTF16_DATA, i)).as("getLong(%d)", i).isEqualTo(expected);
    }
  }

  @Test
  void testUTF16CopyToByteArray() {
    ByteAccess<String> access = StringByteAccess.UTF16.get();
    int srcLen = UTF16_BYTES.length;
    byte[] actual = new byte[srcLen];
    byte[] expected = new byte[srcLen];

    for (int srcPos = 0; srcPos < srcLen; srcPos++) {
      for (int destPos = 0; destPos < srcLen; destPos++) {
        for (int copyLen = 0;
            srcPos + copyLen <= srcLen && destPos + copyLen <= srcLen;
            copyLen++) {
          for (int j = 0; j < srcLen; j++) {
            actual[j] = (byte) j;
            expected[j] = (byte) j;
          }

          access.copyToByteArray(UTF16_DATA, srcPos, actual, destPos, copyLen);
          System.arraycopy(UTF16_BYTES, srcPos, expected, destPos, copyLen);

          assertThat(actual)
              .as("copyToByteArray(srcPos=%d, destPos=%d, len=%d)", srcPos, destPos, copyLen)
              .isEqualTo(expected);
        }
      }
    }
  }

  @Test
  void testUTF16ConsistencyWithDefault() {
    // UTF-16 and Default must agree on every non-Latin-1 string
    ByteAccess<String> utf16 = StringByteAccess.UTF16.get();
    ByteAccess<String> def = StringByteAccess.Default.get();

    for (int i = 0; i < UTF16_BYTES.length; i++) {
      assertThat(utf16.getByte(UTF16_DATA, i))
          .as("getByte(%d)", i)
          .isEqualTo(def.getByte(UTF16_DATA, i));
    }
    for (int i = 0; i <= UTF16_BYTES.length - 4; i++) {
      assertThat(utf16.getInt(UTF16_DATA, i))
          .as("getInt(%d)", i)
          .isEqualTo(def.getInt(UTF16_DATA, i));
    }
    for (int i = 0; i <= UTF16_BYTES.length - 8; i++) {
      assertThat(utf16.getLong(UTF16_DATA, i))
          .as("getLong(%d)", i)
          .isEqualTo(def.getLong(UTF16_DATA, i));
    }
  }
}
