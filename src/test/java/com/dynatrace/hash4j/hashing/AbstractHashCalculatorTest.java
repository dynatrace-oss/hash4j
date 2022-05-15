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
package com.dynatrace.hash4j.hashing;

import static com.dynatrace.hash4j.hashing.TestUtils.byteArrayToHexString;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;

abstract class AbstractHashCalculatorTest {

  private static final class TestCase {
    private final byte[] expected;
    private final Consumer<HashSink> sinkConsumer;

    public TestCase(Consumer<HashSink> sinkConsumer, String expected) {
      this.expected = TestUtils.hexStringToByteArray(expected);
      this.sinkConsumer = sinkConsumer;
    }

    public byte[] getExpected() {
      return Arrays.copyOf(expected, expected.length);
    }

    public Consumer<HashSink> getSinkConsumer() {
      return sinkConsumer;
    }
  }

  private static final byte[] BYTE_SEQ_199;
  private static final String STRING_179;
  private static final byte[] BYTES_STRING_179;

  static {
    SplittableRandom random = new SplittableRandom(0);
    BYTE_SEQ_199 = new byte[199];
    for (int i = 0; i < BYTE_SEQ_199.length; ++i) {
      BYTE_SEQ_199[i] = (byte) random.nextInt();
    }
    StringBuilder sb = new StringBuilder();
    BYTES_STRING_179 = new byte[179 * 2 + 4];
    for (int i = 0; i < 179; ++i) {
      int x = random.nextInt();
      sb.append((char) x);
      BYTES_STRING_179[2 * i] = (byte) x;
      BYTES_STRING_179[2 * i + 1] = (byte) (x >>> 8);
    }
    STRING_179 = sb.toString();
    for (int j = 0; j < 4; ++j) {
      BYTES_STRING_179[179 * 2 + j] = (byte) (179 >>> (8 * j));
    }
  }

  private static final List<TestCase> testCases =
      Arrays.asList(
          new TestCase(h -> {}, ""),
          new TestCase(h -> h.putBoolean(false), "00"),
          new TestCase(h -> h.putBoolean(true), "01"),
          new TestCase(h -> h.putByte((byte) 0x18), "18"),
          new TestCase(h -> h.putShort((short) 0xc390), "90c3"),
          new TestCase(h -> h.putInt(0x26b332d2), "d232b326"),
          new TestCase(h -> h.putLong(0xec6379d40463bc61L), "61bc6304d47963ec"),
          new TestCase(h -> h.putFloat(Float.intBitsToFloat(0x902fe005)), "05e02f90"),
          new TestCase(
              h -> h.putDouble(Double.longBitsToDouble(0x6b3ea4d75d3f4dbbL)), "bb4d3f5dd7a43e6b"),
          new TestCase(h -> h.putBytes(new byte[] {}), ""),
          new TestCase(
              h -> h.putBytes(TestUtils.hexStringToByteArray("6143f28b2b11d8")), "6143f28b2b11d8"),
          new TestCase(h -> h.putBytes(BYTE_SEQ_199), byteArrayToHexString(BYTE_SEQ_199)),
          new TestCase(
              h -> h.putBytes(TestUtils.hexStringToByteArray("c1ce762d62"), 1, 3), "ce762d"),
          new TestCase(h -> h.putChar((char) 0x1466), "6614"),
          new TestCase(h -> h.putNullable(null, (o, sink) -> {}), "00"),
          new TestCase(
              h -> h.putNullable(0x969661eaa7416bd2L, (l, sink) -> sink.putLong(l)),
              "d26b41a7ea61969601"),
          new TestCase(
              h -> h.putString(String.copyValueOf(new char[] {0x59be, 0x768b, 0x2c9a})),
              "be598b769a2c03000000"),
          new TestCase(h -> h.putString(STRING_179), byteArrayToHexString(BYTES_STRING_179)),
          new TestCase(
              h ->
                  h.putUnorderedIterable(
                      Arrays.asList(0x98da42163aed652fL, 0xe779526fe8e523b7L, 0x45c7d427a002005eL),
                      l -> l),
              "2f65ed3a1642da98b723e5e86f5279e75e0002a027d4c74503000000"),
          new TestCase(
              h ->
                  h.putOrderedIterable(
                      Arrays.asList(0x3a71a12103e646edL, 0x188978c47928f13eL),
                      (l, sink) -> sink.putLong(l)),
              "ed46e60321a1713a3ef12879c478891802000000"),
          new TestCase(
              h -> h.putUUID(new UUID(0xc12cac6b8acd63d5L, 0x8638607e753419afL)),
              "af1934757e603886d563cd8a6bac2cc1"),
          new TestCase(h -> h.putOptionalInt(OptionalInt.of(0x28c21b02)), "021bc22801"),
          new TestCase(h -> h.putOptionalInt(OptionalInt.empty()), "00"),
          new TestCase(
              h -> h.putOptionalLong(OptionalLong.of(0x42fd1b4676b3c8b1L)), "b1c8b376461bfd4201"),
          new TestCase(h -> h.putOptionalLong(OptionalLong.empty()), "00"),
          new TestCase(
              h ->
                  h.putOptionalDouble(
                      OptionalDouble.of(Double.longBitsToDouble(0x8c961e9cdfbe6d9aL))),
              "9a6dbedf9c1e968c01"),
          new TestCase(h -> h.putOptionalDouble(OptionalDouble.empty()), "00"),
          new TestCase(h -> h.putOptional(Optional.<Long>empty(), (x, g) -> g.putLong(x)), "00"),
          new TestCase(
              h -> h.putOptional(Optional.of(0x3fb29bab9f35fde8L), (x, g) -> g.putLong(x)),
              "e8fd359fab9bb23f01"));

  protected abstract HashCalculator createHashCalculator();

  protected abstract Hasher32 createHasher();

  private HashCalculator getNonOptimizedHashCalculator() {
    HashCalculator hashCalculator = createHashCalculator();

    return new AbstractHashCalculator() {
      @Override
      public HashSink putByte(byte v) {
        return hashCalculator.putByte(v);
      }

      @Override
      public int getHashBitSize() {
        return hashCalculator.getHashBitSize();
      }

      @Override
      public int getAsInt() {
        return hashCalculator.getAsInt();
      }

      @Override
      public long getAsLong() {
        return hashCalculator.getAsLong();
      }

      @Override
      public HashValue128 get() {
        return hashCalculator.get();
      }
    };
  }

  @Test
  void testRandomData() {

    int numIterations = 100000;

    SplittableRandom random = new SplittableRandom(0L);

    for (int k = 0; k < numIterations; ++k) {

      HashCalculator rawBytesHashCalculator = createHashCalculator();
      HashCalculator dataHashCalculator = createHashCalculator();
      HashCalculator nonOptimizedHashCalculator = getNonOptimizedHashCalculator();

      List<Byte> rawBytes = new ArrayList<>();

      while (random.nextDouble() >= 0.05) {
        TestCase tc = testCases.get(random.nextInt(testCases.size()));
        tc.getSinkConsumer().accept(dataHashCalculator);
        tc.getSinkConsumer().accept(nonOptimizedHashCalculator);
        for (byte b : tc.getExpected()) {
          rawBytesHashCalculator.putByte(b);
          rawBytes.add(b);
        }
      }
      byte[] rawBytesHash = getBytesAndVerifyRepetitiveGetCalls(rawBytesHashCalculator);
      byte[] dataHash = getBytesAndVerifyRepetitiveGetCalls(dataHashCalculator);
      byte[] nonOptimizedHash = getBytesAndVerifyRepetitiveGetCalls(nonOptimizedHashCalculator);

      assertArrayEquals(rawBytesHash, dataHash);
      assertArrayEquals(rawBytesHash, nonOptimizedHash);
    }
  }

  @Test
  void testComposedByteSequences() {

    int maxSize = 256;
    SplittableRandom random = new SplittableRandom(0L);

    for (int size = 0; size < maxSize; ++size) {
      byte[] data = new byte[size];
      random.nextBytes(data);
      HashCalculator expectedHashCalculator = createHashCalculator();
      expectedHashCalculator.putBytes(data);
      byte[] expected = getBytesAndVerifyRepetitiveGetCalls(expectedHashCalculator);

      for (int k = 0; k < size; ++k) {
        HashCalculator hashCalculator = createHashCalculator();
        hashCalculator.putBytes(data, 0, k);
        hashCalculator.putBytes(data, k, size - k);
        assertArrayEquals(expected, getBytesAndVerifyRepetitiveGetCalls(hashCalculator));
      }

      for (int j = 0; j < size; ++j) {
        for (int k = j; k < size; ++k) {
          HashCalculator hashCalculator = createHashCalculator();
          hashCalculator.putBytes(data, 0, j);
          hashCalculator.putBytes(data, j, k - j);
          hashCalculator.putBytes(data, k, size - k);
          assertArrayEquals(expected, getBytesAndVerifyRepetitiveGetCalls(hashCalculator));
        }
      }
    }
  }

  private static String generateRandomString(SplittableRandom random, int len) {
    StringBuilder sb = new StringBuilder(len);
    for (int i = 0; i < len; ++i) {
      sb.append((char) (random.nextInt(0x0780, 0x07B1)));
    }
    return sb.toString();
  }

  @Test
  void testPutString() {

    int maxPreSize = 64;
    int maxStringSize = 256;
    int maxPostSize = 64;

    SplittableRandom random = new SplittableRandom(0L);
    for (int stringSize = 0; stringSize < maxStringSize; stringSize += 1) {
      String s = generateRandomString(random, stringSize);
      for (int preSize = 0; preSize < maxPreSize; preSize += 1) {
        byte[] pre = new byte[preSize];
        random.nextBytes(pre);
        for (int postSize = 0; postSize < maxPostSize; postSize += 1) {
          byte[] post = new byte[postSize];
          random.nextBytes(post);

          HashCalculator expectedSink = createHashCalculator();
          HashCalculator actualSink = createHashCalculator();

          expectedSink.putBytes(pre);
          actualSink.putBytes(pre);

          for (int i = 0; i < s.length(); ++i) {
            expectedSink.putChar(s.charAt(i));
          }
          expectedSink.putInt(s.length());

          actualSink.putString(s);

          expectedSink.putBytes(post);
          actualSink.putBytes(post);

          byte[] expected = getBytesAndVerifyRepetitiveGetCalls(expectedSink);
          byte[] actual = getBytesAndVerifyRepetitiveGetCalls(actualSink);
          assertArrayEquals(expected, actual);
        }
      }
    }
  }

  @Test
  void testGetCompatibility() {
    byte[] data = TestUtils.hexStringToByteArray("3011498ecb9ca21b2f6260617b55f3a7");
    HashCalculator intHashCalculator = createHashCalculator();
    HashCalculator longHashCalculator = createHashCalculator();
    HashCalculator hash128Calculator = createHashCalculator();
    intHashCalculator.putBytes(data);
    longHashCalculator.putBytes(data);
    hash128Calculator.putBytes(data);
    int intHash = intHashCalculator.getAsInt();
    try {
      long longHash = longHashCalculator.getAsLong();
      assertEquals(intHash, (int) longHash);
      HashValue128 hash128Hash = hash128Calculator.get();
      assertEquals(longHash, hash128Hash.getAsLong());
    } catch (UnsupportedOperationException e) {
    }
  }

  private static final HashFunnel<byte[]> BYTES_FUNNEL =
      (input, sink) -> {
        for (byte b : input) {
          sink.putByte(b);
        }
      };

  @Test
  void testHashBytesOffset() {
    int numCycles = 100000;
    int maxByteLength = 100;
    int maxOffset = 100;
    int maxExtraLength = 100;
    SplittableRandom random = new SplittableRandom(0);

    for (int i = 0; i < numCycles; ++i) {
      int length = random.nextInt(maxByteLength + 1);
      int offset = random.nextInt(maxOffset + 1);
      int extraLength = random.nextInt(maxExtraLength + 1);
      byte[] data = new byte[length];
      byte[] dataWithOffset = new byte[length + offset + extraLength];

      random.nextBytes(data);
      System.arraycopy(data, 0, dataWithOffset, offset, length);

      var hasher = createHasher();
      if (hasher instanceof Hasher32) {
        Hasher32 hasher32 = (Hasher32) hasher;
        long hash32Reference = hasher32.hashToInt(data, BYTES_FUNNEL);
        long hash32 = hasher32.hashBytesToInt(data);
        long hash32WithOffset = hasher32.hashBytesToInt(dataWithOffset, offset, length);
        assertEquals(hash32Reference, hash32);
        assertEquals(hash32Reference, hash32WithOffset);
      }
      if (hasher instanceof Hasher64) {
        Hasher64 hasher64 = (Hasher64) hasher;
        long hash64Reference = hasher64.hashToLong(data, BYTES_FUNNEL);
        long hash64 = hasher64.hashBytesToLong(data);
        long hash64WithOffset = hasher64.hashBytesToLong(dataWithOffset, offset, length);
        assertEquals(hash64Reference, hash64);
        assertEquals(hash64Reference, hash64WithOffset);
      }
      if (hasher instanceof Hasher128) {
        Hasher128 hasher128 = (Hasher128) hasher;
        HashValue128 hash128Reference = hasher128.hashTo128Bits(data, BYTES_FUNNEL);
        HashValue128 hash128 = hasher128.hashBytesTo128Bits(data);
        HashValue128 hash128WithOffset =
            hasher128.hashBytesTo128Bits(dataWithOffset, offset, length);
        assertEquals(hash128Reference, hash128);
        assertEquals(hash128Reference, hash128WithOffset);
      }
    }
  }

  private final byte[] getBytes(HashCalculator hashCalculator) {
    byte[] result = new byte[hashCalculator.getHashBitSize() / 8];
    if (hashCalculator.getHashBitSize() == 32) {
      int x = hashCalculator.getAsInt();
      for (int i = 24, j = 0; i >= 0; i -= 8, j += 1) {
        result[j] = (byte) ((x >>> i) & 0xFFL);
      }
      return result;
    } else if (hashCalculator.getHashBitSize() == 64) {
      long hash = hashCalculator.getAsLong();
      for (int i = 56, j = 0; i >= 0; i -= 8, j += 1) {
        result[j] = (byte) ((hash >>> i) & 0xFFL);
      }
    } else if (hashCalculator.getHashBitSize() == 128) {
      HashValue128 x = hashCalculator.get();
      for (int i = 56, j = 0; i >= 0; i -= 8, j += 1) {
        result[j] = (byte) ((x.getMostSignificantBits() >>> i) & 0xFFL);
      }
      for (int i = 56, j = 8; i >= 0; i -= 8, j += 1) {
        result[j] = (byte) ((x.getLeastSignificantBits() >>> i) & 0xFFL);
      }
    } else {
      fail();
    }
    return result;
  }

  private final byte[] getBytesAndVerifyRepetitiveGetCalls(HashCalculator hashCalculator) {
    byte[] result = getBytes(hashCalculator);

    int numRecalculations = 5;
    for (int i = 0; i < numRecalculations; ++i) {
      assertArrayEquals(result, getBytes(hashCalculator));
    }
    return result;
  }
}
