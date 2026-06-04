/*
 * Copyright 2024-2026 Dynatrace LLC
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

import static com.dynatrace.hash4j.internal.ByteArrayUtil.setChar;
import static com.dynatrace.hash4j.internal.ByteArrayUtil.setShort;
import static com.dynatrace.hash4j.testutils.TestUtils.byteArrayToHexString;
import static org.assertj.core.api.Assertions.assertThat;

import com.dynatrace.hash4j.hashing.HashMocks.TestHashStream;
import com.dynatrace.hash4j.testutils.TestUtils;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.SplittableRandom;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class AbstractHashStreamTest {

  protected void assertBytes(Consumer<HashStream> c, String hexString) {
    TestHashStream hashStream = new TestHashStream();
    c.accept(hashStream);
    assertThat(byteArrayToHexString(hashStream.getData())).isEqualTo(hexString);
  }

  @Test
  void testPutBooleanDefaultImplementation() {
    assertBytes(h -> h.putBoolean(false), "00");
    assertBytes(h -> h.putBoolean(true), "01");
  }

  private static void nextBooleans(SplittableRandom random, boolean[] b) {
    byte[] bytes = new byte[b.length];
    random.nextBytes(bytes);
    for (int i = 0; i < bytes.length; ++i) {
      b[i] = ((bytes[i] & 1) != 0);
    }
  }

  private static byte[] toBytes(boolean[] data) {
    byte[] result = new byte[data.length];
    for (int i = 0; i < data.length; ++i) {
      result[i] = (byte) (data[i] ? 1 : 0);
    }
    return result;
  }

  @Test
  void testPutBooleansDefaultImplementation() {
    SplittableRandom random = new SplittableRandom(0x4f69305aee3d185fL);
    for (int len = 0; len < 16; ++len) {
      boolean[] data = new boolean[len];
      nextBooleans(random, data);
      assertBytes(h -> h.putBooleans(data), TestUtils.byteArrayToHexString(toBytes(data)));
    }

    for (int off = 0; off < 16; ++off) {
      for (int len = 0; len < 16; ++len) {
        boolean[] data = new boolean[off + len];
        nextBooleans(random, data);
        int finalOff = off;
        int finalLen = len;
        assertBytes(
            h -> h.putBooleans(data, finalOff, finalLen),
            TestUtils.byteArrayToHexString(toBytes(Arrays.copyOfRange(data, off, off + len))));
      }
    }
  }

  @Test
  void testPutBooleanArrayDefaultImplementation() {
    assertBytes(h -> h.putBooleanArray(new boolean[] {true, false, false}), "01000003000000");
  }

  @Test
  void testPutByteDefaultImplementation() {
    assertBytes(h -> h.putByte((byte) 0x67), "67");
  }

  @Test
  void testPutBytesDefaultImplementation() {
    SplittableRandom random = new SplittableRandom(0x70a0b98f3659ee96L);
    for (int len = 0; len < 16; ++len) {
      byte[] data = new byte[len];
      random.nextBytes(data);
      assertBytes(h -> h.putBytes(data), TestUtils.byteArrayToHexString(data));
    }

    for (int off = 0; off < 16; ++off) {
      for (int len = 0; len < 16; ++len) {
        byte[] data = new byte[off + len];
        random.nextBytes(data);
        int finalOff = off;
        int finalLen = len;
        assertBytes(
            h -> h.putBytes(data, finalOff, finalLen),
            TestUtils.byteArrayToHexString(Arrays.copyOfRange(data, off, off + len)));
      }
    }
  }

  @Test
  void testPutCharDefaultImplementation() {
    assertBytes(h -> h.putChar((char) 0x5698), "9856");
  }

  @Test
  void testPutCharsDefaultImplementation() {
    SplittableRandom random = new SplittableRandom(0x9d6920f8b51c6e80L);
    for (int len = 0; len < 8; ++len) {
      char[] data = new char[len];
      for (int i = 0; i < len; ++i) {
        data[i] = (char) random.nextInt();
      }
      byte[] expected = new byte[2 * len];
      for (int i = 0; i < len; ++i) {
        setChar(expected, 2 * i, data[i]);
      }
      assertBytes(h -> h.putChars(data), TestUtils.byteArrayToHexString(expected));
      assertBytes(h -> h.putChars(new String(data)), TestUtils.byteArrayToHexString(expected));
    }

    for (int off = 0; off < 16; ++off) {
      for (int len = 0; len < 8; ++len) {
        char[] data = new char[off + len];
        for (int i = 0; i < len + off; ++i) {
          data[i] = (char) random.nextInt();
        }
        byte[] expected = new byte[2 * len];
        for (int i = 0; i < len; ++i) {
          setChar(expected, 2 * i, data[off + i]);
        }
        int finalOff = off;
        int finalLen = len;
        assertBytes(
            h -> h.putChars(data, finalOff, finalLen), TestUtils.byteArrayToHexString(expected));
      }
    }
  }

  @Test
  void testPutByteArrayDefaultImplementation() {
    assertBytes(
        h -> h.putByteArray(new byte[] {0x32, 0x54, 0x56, (byte) 0x98, 0x31, 0x66}),
        "32545698316606000000");
  }

  @Test
  void testPutCharArrayDefaultImplementation() {
    assertBytes(h -> h.putCharArray(new char[] {0x3254, 0x5698, 0x3166}), "54329856663103000000");
  }

  @Test
  void testPutStringDefaultImplementation() {
    assertBytes(h -> h.putString("" + (char) 0x5698 + (char) 0x3123), "9856233102000000");
  }

  @Test
  void testPutShortDefaultImplementation() {
    assertBytes(h -> h.putShort((short) 0x5698), "9856");
  }

  @Test
  void testPutShortsDefaultImplementation() {
    assertBytes(h -> h.putShorts(new short[] {(short) 0x5698, (short) 0x3123}), "98562331");
    assertBytes(h -> h.putShorts(new short[] {(short) 0x5698, (short) 0x3123}, 1, 1), "2331");

    SplittableRandom random = new SplittableRandom(0x6152537f989cdc12L);
    for (int len = 0; len < 8; ++len) {
      short[] data = new short[len];
      for (int i = 0; i < len; ++i) {
        data[i] = (short) random.nextInt();
      }
      byte[] expected = new byte[2 * len];
      for (int i = 0; i < len; ++i) {
        setShort(expected, 2 * i, data[i]);
      }
      assertBytes(h -> h.putShorts(data), TestUtils.byteArrayToHexString(expected));
    }

    for (int off = 0; off < 16; ++off) {
      for (int len = 0; len < 8; ++len) {
        short[] data = new short[off + len];
        for (int i = 0; i < len + off; ++i) {
          data[i] = (short) random.nextInt();
        }
        byte[] expected = new byte[2 * len];
        for (int i = 0; i < len; ++i) {
          setShort(expected, 2 * i, data[off + i]);
        }
        int finalOff = off;
        int finalLen = len;
        assertBytes(
            h -> h.putShorts(data, finalOff, finalLen), TestUtils.byteArrayToHexString(expected));
      }
    }
  }

  @Test
  void testPutShortArrayDefaultImplementation() {
    assertBytes(
        h -> h.putShortArray(new short[] {(short) 0x5698, (short) 0x3123}), "9856233102000000");
  }

  @Test
  void testPutIntDefaultImplementation() {
    assertBytes(h -> h.putInt(0x32545698), "98565432");
  }

  @Test
  void testPutIntsDefaultImplementation() {
    assertBytes(h -> h.putInts(new int[] {0x32545698, 0x31664723}), "9856543223476631");
    assertBytes(h -> h.putInts(new int[] {0x32545698, 0x31664723}, 1, 1), "23476631");
  }

  @Test
  void testPutIntArrayDefaultImplementation() {
    assertBytes(h -> h.putIntArray(new int[] {0x32545698, 0x31664723}), "985654322347663102000000");
  }

  @Test
  void testPutFloatDefaultImplementation() {
    assertBytes(h -> h.putFloat(Float.intBitsToFloat(0x32545698)), "98565432");
  }

  @Test
  void testPutFloatsDefaultImplementation() {
    assertBytes(
        h ->
            h.putFloats(
                new float[] {Float.intBitsToFloat(0x32545698), Float.intBitsToFloat(0x31664723)}),
        "9856543223476631");
    assertBytes(
        h ->
            h.putFloats(
                new float[] {Float.intBitsToFloat(0x32545698), Float.intBitsToFloat(0x31664723)},
                1,
                1),
        "23476631");
  }

  @Test
  void testPutFloatArrayDefaultImplementation() {
    assertBytes(
        h ->
            h.putFloatArray(
                new float[] {Float.intBitsToFloat(0x32545698), Float.intBitsToFloat(0x31664723)}),
        "985654322347663102000000");
  }

  @Test
  void testPutLongDefaultImplementation() {
    assertBytes(h -> h.putLong(0xa545f65632545698L), "9856543256f645a5");
  }

  @Test
  void testPutLongsDefaultImplementation() {
    assertBytes(
        h -> h.putLongs(new long[] {0xa545f65632545698L, 0x75affa68e4905345L}),
        "9856543256f645a5455390e468faaf75");
    assertBytes(
        h -> h.putLongs(new long[] {0xa545f65632545698L, 0x75affa68e4905345L}, 1, 1),
        "455390e468faaf75");
  }

  @Test
  void testPutLongArrayDefaultImplementation() {
    assertBytes(
        h -> h.putLongArray(new long[] {0xa545f65632545698L, 0x75affa68e4905345L}),
        "9856543256f645a5455390e468faaf7502000000");
  }

  @Test
  void testPutDoubleDefaultImplementation() {
    assertBytes(h -> h.putDouble(Double.longBitsToDouble(0xa545f65632545698L)), "9856543256f645a5");
  }

  @Test
  void testPutDoublesDefaultImplementation() {
    assertBytes(
        h ->
            h.putDoubles(
                new double[] {
                  Double.longBitsToDouble(0xa545f65632545698L),
                  Double.longBitsToDouble(0x75affa68e4905345L)
                }),
        "9856543256f645a5455390e468faaf75");
    assertBytes(
        h ->
            h.putDoubles(
                new double[] {
                  Double.longBitsToDouble(0xa545f65632545698L),
                  Double.longBitsToDouble(0x75affa68e4905345L)
                },
                1,
                1),
        "455390e468faaf75");
  }

  @Test
  void testPutDoubleArrayDefaultImplementation() {
    assertBytes(
        h ->
            h.putDoubleArray(
                new double[] {
                  Double.longBitsToDouble(0xa545f65632545698L),
                  Double.longBitsToDouble(0x75affa68e4905345L)
                }),
        "9856543256f645a5455390e468faaf7502000000");
  }

  @Test
  void testPutUUIDDefaultImplementation() {
    assertBytes(
        h -> h.putUUID(new UUID(0xa545f65632545698L, 0x75affa68e4905345L)),
        "455390e468faaf759856543256f645a5");
  }

  @Test
  void testPutCharsUTF8DefaultImplementation() {
    int numCases = 10_000;
    int maxNumChars = 100;

    SplittableRandom random = new SplittableRandom(0x99dc5d0644d52055L);

    TestHashStream hashStream = new TestHashStream();
    for (int i = 0; i < numCases; ++i) {

      hashStream.reset();

      int numChars = random.nextInt(maxNumChars + 1);
      char[] chars = new char[numChars];
      for (int c = 0; c < numChars; ++c) {
        chars[c] = (char) random.nextInt();
      }

      String s = String.copyValueOf(chars);
      hashStream.putCharsUTF8(s);
      byte[] actualBytes = hashStream.getData();
      byte[] expectedBytes = s.getBytes(StandardCharsets.UTF_8);

      assertThat(actualBytes).isEqualTo(expectedBytes);
    }
  }

  @Test
  void testPutCharsUTF8KnownCases() {
    // empty
    assertBytes(h -> h.putCharsUTF8(""), "");
    // ASCII (1 byte)
    assertBytes(h -> h.putCharsUTF8("A"), "41");
    assertBytes(h -> h.putCharsUTF8("AB"), "4142");
    // 2-byte sequences
    assertBytes(h -> h.putCharsUTF8("\u0080"), "c280"); // U+0080 → C2 80
    assertBytes(h -> h.putCharsUTF8("\u07FF"), "dfbf"); // U+07FF → DF BF
    // 3-byte sequences
    assertBytes(h -> h.putCharsUTF8("\u0800"), "e0a080"); // U+0800 → E0 A0 80
    assertBytes(h -> h.putCharsUTF8("\uFFFF"), "efbfbf"); // U+FFFF → EF BF BF
    // 4-byte sequences (surrogate pairs)
    assertBytes(h -> h.putCharsUTF8("\uD800\uDC00"), "f0908080"); // U+10000 → F0 90 80 80
    assertBytes(h -> h.putCharsUTF8("\uDBFF\uDFFF"), "f48fbfbf"); // U+10FFFF → F4 8F BF BF
    // lone surrogates → replaced by '?' (0x3F)
    assertBytes(h -> h.putCharsUTF8("\uD800"), "3f"); // lone high surrogate
    assertBytes(h -> h.putCharsUTF8("\uDC00"), "3f"); // lone low surrogate
    assertBytes(h -> h.putCharsUTF8("\uD800A"), "3f41"); // high surrogate not followed by low
  }

  @Test
  void testPutStringUTF8DefaultImplementation() {
    int numCases = 10_000;
    int maxNumChars = 100;

    SplittableRandom random = new SplittableRandom(0x3c8d0f6a2b1e7594L);

    TestHashStream hashStream = new TestHashStream();
    for (int i = 0; i < numCases; ++i) {
      hashStream.reset();

      int numChars = random.nextInt(maxNumChars + 1);
      char[] chars = new char[numChars];
      for (int c = 0; c < numChars; ++c) {
        chars[c] = (char) random.nextInt();
      }

      String s = String.copyValueOf(chars);
      hashStream.putStringUTF8(s);
      byte[] actual = hashStream.getData();

      byte[] utf8Bytes = s.getBytes(StandardCharsets.UTF_8);
      int byteCount = utf8Bytes.length;
      byte[] expected = new byte[utf8Bytes.length + 4];
      System.arraycopy(utf8Bytes, 0, expected, 0, utf8Bytes.length);
      expected[utf8Bytes.length] = (byte) byteCount;
      expected[utf8Bytes.length + 1] = (byte) (byteCount >>> 8);
      expected[utf8Bytes.length + 2] = (byte) (byteCount >>> 16);
      expected[utf8Bytes.length + 3] = (byte) (byteCount >>> 24);

      assertThat(actual).isEqualTo(expected);
    }
  }

  @Test
  void testPutStringUTF8KnownCases() {
    // empty string → no bytes + byte count 0
    assertBytes(h -> h.putStringUTF8(""), "00000000");
    // ASCII (1 byte)
    assertBytes(h -> h.putStringUTF8("A"), "4101000000");
    // two ASCII chars (2 bytes)
    assertBytes(h -> h.putStringUTF8("AB"), "414202000000");
    // 2-byte char U+0080
    assertBytes(h -> h.putStringUTF8("\u0080"), "c28002000000");
    // 3-byte char U+0800
    assertBytes(h -> h.putStringUTF8("\u0800"), "e0a08003000000");
    // surrogate pair U+10000 (4 bytes)
    assertBytes(h -> h.putStringUTF8("\uD800\uDC00"), "f090808004000000");
    // lone high surrogate → '?', 1 byte
    assertBytes(h -> h.putStringUTF8("\uD800"), "3f01000000");
    // lone low surrogate → '?', 1 byte
    assertBytes(h -> h.putStringUTF8("\uDC00"), "3f01000000");
    // mixed: "A" + surrogate pair U+10000 + "B" → 1 + 4 + 1 = 6 bytes
    assertBytes(h -> h.putStringUTF8("A\uD800\uDC00B"), "41f09080804206000000");
  }

  private static char[] createCompleteCharPool() {
    char[] result = new char[1 << 16];
    for (int i = 0; i < result.length; ++i) {
      result[i] = (char) i;
    }
    return result;
  }

  /**
   * A smaller char pool containing characters around branching conditions in {@link
   * AbstractHashStream#putCharsUTF8(CharSequence)}. This allows to test the handling of these
   * conditions without testing all 65536 char values.
   */
  private static char[] createLimitedCharPool(int extend) {
    int[] branchPoints =
        IntStream.of(0x0, 0x80, 0x800, 0xd800, 0xe000, 0xdc00)
            .flatMap(i -> IntStream.rangeClosed(i - extend, i + extend))
            .map(x -> x & 0xffff)
            .distinct()
            .sorted()
            .toArray();
    char[] result = new char[branchPoints.length];
    for (int i = 0; i < branchPoints.length; ++i) {
      result[i] = (char) branchPoints[i];
    }
    return result;
  }

  @Test
  void testPutCharsUTF8SingleChar() {
    char[] chars = new char[1];
    CharSequence charSequence =
        new CharSequence() {
          @Override
          public int length() {
            return 1;
          }

          @Override
          public char charAt(int index) {
            return chars[index];
          }

          @Override
          public String toString() {
            return String.valueOf(chars);
          }

          @Override
          public CharSequence subSequence(int start, int end) {
            throw new UnsupportedOperationException();
          }
        };

    TestHashStream hashStream = new TestHashStream();

    char[] charPool = createCompleteCharPool();
    for (char c : charPool) {
      chars[0] = c;

      byte[] expectedBytes = charSequence.toString().getBytes(StandardCharsets.UTF_8);

      hashStream.reset();
      int numEncodedBytes = hashStream.putCharsUTF8Internal(charSequence);
      assertThat(numEncodedBytes).isEqualTo(expectedBytes.length);

      hashStream.assertData(expectedBytes, expectedBytes.length);
    }
  }

  @Test
  void testPutCharsUTF8TwoChars() {
    char[] chars = new char[2];
    CharSequence charSequence =
        new CharSequence() {
          @Override
          public int length() {
            return 2;
          }

          @Override
          public char charAt(int index) {
            return chars[index];
          }

          @Override
          public String toString() {
            return String.valueOf(chars);
          }

          @Override
          public CharSequence subSequence(int start, int end) {
            throw new UnsupportedOperationException();
          }
        };

    TestHashStream hashStream = new TestHashStream();

    char[] charPool = createLimitedCharPool(200);
    for (char c0 : charPool) {
      for (char c1 : charPool) {
        chars[0] = c0;
        chars[1] = c1;

        byte[] expectedBytes = charSequence.toString().getBytes(StandardCharsets.UTF_8);

        hashStream.reset();
        int numEncodedBytes = hashStream.putCharsUTF8Internal(charSequence);

        assertThat(numEncodedBytes).isEqualTo(expectedBytes.length);
        hashStream.assertData(expectedBytes, expectedBytes.length);
      }
    }
  }

  @Test
  void testPutCharsUTF8FourChars() {
    char[] chars = new char[4];
    CharSequence charSequence =
        new CharSequence() {
          @Override
          public int length() {
            return 4;
          }

          @Override
          public char charAt(int index) {
            return chars[index];
          }

          @Override
          public String toString() {
            return String.valueOf(chars);
          }

          @Override
          public CharSequence subSequence(int start, int end) {
            throw new UnsupportedOperationException();
          }
        };

    TestHashStream hashStream = new TestHashStream();

    char[] charPool = createLimitedCharPool(3);
    for (char c0 : charPool) {
      for (char c1 : charPool) {
        for (char c2 : charPool) {
          for (char c3 : charPool) {

            chars[0] = c0;
            chars[1] = c1;
            chars[2] = c2;
            chars[3] = c3;

            byte[] expectedBytes = charSequence.toString().getBytes(StandardCharsets.UTF_8);

            hashStream.reset();
            int numEncodedBytes = hashStream.putCharsUTF8Internal(charSequence);

            assertThat(numEncodedBytes).isEqualTo(expectedBytes.length);
            hashStream.assertData(expectedBytes, expectedBytes.length);
          }
        }
      }
    }
  }

  @Test
  void testRandomLatin1Strings() {

    TestHashStream hashStream = new TestHashStream();

    SplittableRandom random = new SplittableRandom(0x4392cd5b27b28a4fL);

    int numIterations = 1000;
    int maxLength = 20;

    for (int i = 0; i < numIterations; ++i) {
      int len = random.nextInt(0, maxLength + 1);

      char[] chars = new char[len];
      for (int k = 0; k < len; ++k) {
        chars[k] = (char) random.nextInt(256);
      }
      String s = String.valueOf(chars);
      byte[] expectedBytes = s.getBytes(StandardCharsets.UTF_8);

      hashStream.reset();
      int numEncodedBytes = hashStream.putCharsUTF8Internal(s);
      assertThat(numEncodedBytes).isEqualTo(expectedBytes.length);
      hashStream.assertData(expectedBytes, expectedBytes.length);
    }
  }
}
