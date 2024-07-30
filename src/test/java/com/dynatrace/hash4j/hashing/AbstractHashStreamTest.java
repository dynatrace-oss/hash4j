/*
 * Copyright 2024 Dynatrace LLC
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

import static com.dynatrace.hash4j.testutils.TestUtils.byteArrayToHexString;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import com.dynatrace.hash4j.testutils.TestUtils;
import java.util.Arrays;
import java.util.SplittableRandom;
import java.util.UUID;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;

public class AbstractHashStreamTest {

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

  /*@Test
  void testPutBooleansDefaultImplementation() {
    assertBytes(h -> h.putBooleans(new boolean[] {true, false, true}), "010001");
    assertBytes(h -> h.putBooleans(new boolean[] {true, false, true},1,2), "0001");
  }*/

  private static final void nextBooleans(SplittableRandom random, boolean[] b) {
    byte[] bytes = new byte[b.length];
    random.nextBytes(bytes);
    for (int i = 0; i < bytes.length; ++i) {
      b[i] = ((bytes[i] & 1) != 0);
    }
  }

  private static final byte[] toBytes(boolean[] data) {
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
        AbstractHasher.setChar(expected, 2 * i, data[i]);
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
          AbstractHasher.setChar(expected, 2 * i, data[off + i]);
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
        AbstractHasher.setShort(expected, 2 * i, data[i]);
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
          AbstractHasher.setShort(expected, 2 * i, data[off + i]);
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
}
