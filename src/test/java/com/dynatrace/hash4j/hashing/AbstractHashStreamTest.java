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

import static com.dynatrace.hash4j.testutils.TestUtils.byteArrayToHexString;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;

import com.dynatrace.hash4j.testutils.TestUtils;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.IntStream;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

@TestInstance(PER_CLASS)
abstract class AbstractHashStreamTest {

  protected static final HashFunnel<byte[]> BYTES_FUNNEL_1 = (input, sink) -> sink.putBytes(input);
  protected static final HashFunnel<byte[]> BYTES_FUNNEL_2 =
      (input, sink) -> {
        for (byte b : input) sink.putByte(b);
      };
  protected static final HashFunnel<CharSequence> CHAR_FUNNEL =
      (input, sink) -> sink.putChars(input);

  public static class ReferenceTestRecord<T extends Hasher> {

    private final T hasher;
    private final byte[] data;

    public ReferenceTestRecord(T hasher, byte[] input) {
      this.hasher = hasher;
      this.data = Arrays.copyOf(input, input.length);
    }

    public T getHasher() {
      return hasher;
    }

    public byte[] getData() {
      return data;
    }

    @Override
    public String toString() {
      return "ReferenceTestRecord{" + "data.length=" + data.length + '}';
    }
  }

  protected abstract List<? extends Hasher> getHashers();

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

  private static HashStream asNonOptimizedHashStream(HashStream hashStream) {
    return new AbstractHashStream() {
      @Override
      public HashStream putByte(byte v) {
        return hashStream.putByte(v);
      }

      @Override
      public int getHashBitSize() {
        return hashStream.getHashBitSize();
      }

      @Override
      public int getAsInt() {
        return hashStream.getAsInt();
      }

      @Override
      public long getAsLong() {
        return hashStream.getAsLong();
      }

      @Override
      public HashValue128 get() {
        return hashStream.get();
      }
    };
  }

  @ParameterizedTest
  @MethodSource("getHashers")
  void testRandomData(Hasher hasher) {

    int numIterations = 100000;

    SplittableRandom random = new SplittableRandom(0L);

    for (int k = 0; k < numIterations; ++k) {

      HashStream rawBytesHashStream = hasher.hashStream();
      HashStream dataHashStream = hasher.hashStream();
      HashStream nonOptimizedHashStream = asNonOptimizedHashStream(hasher.hashStream());

      while (random.nextDouble() >= 0.05) {
        TestCase tc = testCases.get(random.nextInt(testCases.size()));
        tc.getSinkConsumer().accept(dataHashStream);
        tc.getSinkConsumer().accept(nonOptimizedHashStream);
        for (byte b : tc.getExpected()) {
          rawBytesHashStream.putByte(b);
        }
      }
      byte[] rawBytesHash = getBytesAndVerifyRepetitiveGetCalls(rawBytesHashStream);
      byte[] dataHash = getBytesAndVerifyRepetitiveGetCalls(dataHashStream);
      byte[] nonOptimizedHash = getBytesAndVerifyRepetitiveGetCalls(nonOptimizedHashStream);

      assertThat(dataHash).isEqualTo(rawBytesHash);
      assertThat(nonOptimizedHash).isEqualTo(rawBytesHash);
    }
  }

  @ParameterizedTest
  @MethodSource("getHashers")
  void testComposedByteSequences(Hasher hasher) {

    int maxSize = 256;
    SplittableRandom random = new SplittableRandom(0L);

    for (int size = 0; size < maxSize; ++size) {
      byte[] data = new byte[size];
      random.nextBytes(data);
      HashStream expectedHashStream = hasher.hashStream();
      expectedHashStream.putBytes(data);
      byte[] expected = getBytesAndVerifyRepetitiveGetCalls(expectedHashStream);

      for (int k = 0; k < size; ++k) {
        HashStream hashStream = hasher.hashStream();
        hashStream.putBytes(data, 0, k);
        hashStream.putBytes(data, k, size - k);
        assertThat(getBytesAndVerifyRepetitiveGetCalls(hashStream)).isEqualTo(expected);
      }

      for (int j = 0; j < size; ++j) {
        for (int k = j; k < size; ++k) {
          HashStream hashStream = hasher.hashStream();
          hashStream.putBytes(data, 0, j);
          hashStream.putBytes(data, j, k - j);
          hashStream.putBytes(data, k, size - k);
          assertThat(getBytesAndVerifyRepetitiveGetCalls(hashStream)).isEqualTo(expected);
        }
      }
    }
  }

  @ParameterizedTest
  @MethodSource("getHashers")
  void testGetCompatibility(Hasher hasher) {
    byte[] data = TestUtils.hexStringToByteArray("3011498ecb9ca21b2f6260617b55f3a7");
    HashStream intHashStream = hasher.hashStream();
    HashStream longHashStream = hasher.hashStream();
    HashStream hash128Calculator = hasher.hashStream();
    intHashStream.putBytes(data);
    longHashStream.putBytes(data);
    hash128Calculator.putBytes(data);
    int intHash = intHashStream.getAsInt();
    try {
      long longHash = longHashStream.getAsLong();
      assertThat((int) longHash).isEqualTo(intHash);
      HashValue128 hash128Hash = hash128Calculator.get();
      assertThat(hash128Hash.getAsLong()).isEqualTo(longHash);
    } catch (UnsupportedOperationException e) {
      // no compatibility check necessary, if 128-bit hash value is not supported
    }
  }

  @ParameterizedTest
  @MethodSource("getHashers")
  void testHashBytesOffset(Hasher hasher) {
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

      if (hasher instanceof Hasher32) {
        Hasher32 hasher32 = (Hasher32) hasher;
        long hash32Reference = hasher32.hashToInt(data, BYTES_FUNNEL_1);
        long hash32 = hasher32.hashBytesToInt(data);
        long hash32WithOffset = hasher32.hashBytesToInt(dataWithOffset, offset, length);
        assertThat(hash32).isEqualTo(hash32Reference);
        assertThat(hash32WithOffset).isEqualTo(hash32Reference);
      }
      if (hasher instanceof Hasher64) {
        Hasher64 hasher64 = (Hasher64) hasher;
        long hash64Reference = hasher64.hashToLong(data, BYTES_FUNNEL_1);
        long hash64 = hasher64.hashBytesToLong(data);
        long hash64WithOffset = hasher64.hashBytesToLong(dataWithOffset, offset, length);
        assertThat(hash64).isEqualTo(hash64Reference);
        assertThat(hash64WithOffset).isEqualTo(hash64Reference);
      }
      if (hasher instanceof Hasher128) {
        Hasher128 hasher128 = (Hasher128) hasher;
        HashValue128 hash128Reference = hasher128.hashTo128Bits(data, BYTES_FUNNEL_1);
        HashValue128 hash128 = hasher128.hashBytesTo128Bits(data);
        HashValue128 hash128WithOffset =
            hasher128.hashBytesTo128Bits(dataWithOffset, offset, length);
        assertThat(hash128).isEqualTo(hash128Reference);
        assertThat(hash128WithOffset).isEqualTo(hash128Reference);
      }
    }
  }

  private byte[] getBytes(HashStream hashStream) {
    byte[] result = new byte[hashStream.getHashBitSize() / 8];
    if (hashStream.getHashBitSize() == 32) {
      int x = hashStream.getAsInt();
      for (int i = 24, j = 0; i >= 0; i -= 8, j += 1) {
        result[j] = (byte) ((x >>> i) & 0xFFL);
      }
    } else if (hashStream.getHashBitSize() == 64) {
      long hash = hashStream.getAsLong();
      for (int i = 56, j = 0; i >= 0; i -= 8, j += 1) {
        result[j] = (byte) ((hash >>> i) & 0xFFL);
      }
    } else if (hashStream.getHashBitSize() == 128) {
      HashValue128 x = hashStream.get();
      for (int i = 56, j = 0; i >= 0; i -= 8, j += 1) {
        result[j] = (byte) ((x.getMostSignificantBits() >>> i) & 0xFFL);
      }
      for (int i = 56, j = 8; i >= 0; i -= 8, j += 1) {
        result[j] = (byte) ((x.getLeastSignificantBits() >>> i) & 0xFFL);
      }
    } else {
      fail("hash stream has unexpected bit size");
    }
    return result;
  }

  private byte[] getBytesAndVerifyRepetitiveGetCalls(HashStream hashStream) {
    byte[] result = getBytes(hashStream);

    int numRecalculations = 5;
    for (int i = 0; i < numRecalculations; ++i) {
      assertThat(getBytes(hashStream)).isEqualTo(result);
    }
    return result;
  }

  private static void test(
      Hasher hasher,
      long seed,
      Function<SplittableRandom, Consumer<HashStream>> contributorGenerator) {
    int maxPreSize = 72;
    int maxPostSize = 72;
    int numCycles = 1;
    byte[] preData = new byte[maxPreSize];
    byte[] postData = new byte[maxPostSize];
    SplittableRandom random = new SplittableRandom(seed);
    for (int i = 0; i < numCycles; ++i) {
      Consumer<HashStream> contributor = contributorGenerator.apply(random);
      random.nextBytes(preData);
      random.nextBytes(postData);
      for (int preSize = 0; preSize <= maxPreSize; preSize += 1) {
        for (int postSize = 0; postSize <= maxPostSize; postSize += 1) {
          HashStream hashStreamActual = hasher.hashStream();
          HashStream hashStreamExpected = asNonOptimizedHashStream(hasher.hashStream());
          for (int j = 0; j < preSize; ++j) {
            hashStreamActual.putByte(preData[j]);
            hashStreamExpected.putByte(preData[j]);
          }
          contributor.accept(hashStreamActual);
          contributor.accept(hashStreamExpected);
          for (int j = 0; j < postSize; ++j) {
            hashStreamActual.putByte(postData[j]);
            hashStreamExpected.putByte(postData[j]);
          }
          assertHashStreamEquals(hashStreamExpected, hashStreamActual);
        }
      }
    }
  }

  private static void assertHashStreamEquals(
      HashStream hashStreamExpected, HashStream hashStreamActual) {
    assertThat(hashStreamActual.getHashBitSize()).isEqualTo(hashStreamExpected.getHashBitSize());
    int hashBitSize = hashStreamExpected.getHashBitSize();
    if (hashBitSize >= 128) {
      assertThat(hashStreamActual.get()).isEqualTo(hashStreamExpected.get());
    }
    if (hashBitSize >= 64) {
      assertThat(hashStreamActual.getAsLong()).isEqualTo(hashStreamExpected.getAsLong());
    }
    if (hashBitSize >= 32) {
      assertThat(hashStreamActual.getAsInt()).isEqualTo(hashStreamExpected.getAsInt());
    }
  }

  @ParameterizedTest
  @MethodSource("getHashers")
  void testPutLong(Hasher hasher) {
    test(
        hasher,
        0x2110f54508226dfdL,
        r -> {
          long v = r.nextLong();
          return h -> h.putLong(v);
        });
  }

  @ParameterizedTest
  @MethodSource("getHashers")
  void testPutInt(Hasher hasher) {
    test(
        hasher,
        0xb5e6f6ede8e45164L,
        r -> {
          int v = r.nextInt();
          return h -> h.putInt(v);
        });
  }

  @ParameterizedTest
  @MethodSource("getHashers")
  void testPutShort(Hasher hasher) {
    test(
        hasher,
        0x6bcb0bf5add61f51L,
        r -> {
          short v = (short) r.nextInt();
          return h -> h.putShort(v);
        });
  }

  @ParameterizedTest
  @MethodSource("getHashers")
  void testPutDouble(Hasher hasher) {
    test(
        hasher,
        0xc5650ab3b1cc8aeeL,
        r -> {
          double v = Double.longBitsToDouble(r.nextLong());
          return h -> h.putDouble(v);
        });
  }

  @ParameterizedTest
  @MethodSource("getHashers")
  void testPutFloat(Hasher hasher) {
    test(
        hasher,
        0x03fe0c781f9f5a03L,
        r -> {
          float v = Float.intBitsToFloat(r.nextInt());
          return h -> h.putFloat(v);
        });
  }

  @ParameterizedTest
  @MethodSource("getHashers")
  void testPutChar(Hasher hasher) {
    test(
        hasher,
        0x744cb1b9d34f582aL,
        r -> {
          char v = (char) r.nextInt();
          return h -> h.putChar(v);
        });
  }

  @ParameterizedTest
  @MethodSource("getHashers")
  void testPutBoolean(Hasher hasher) {
    test(
        hasher,
        0x2ee660051da0b48cL,
        r -> {
          boolean v = r.nextBoolean();
          return h -> h.putBoolean(v);
        });
  }

  @ParameterizedTest
  @MethodSource("getHashers")
  void testPutOptionalInt(Hasher hasher) {
    test(
        hasher,
        0x53112c25f749193eL,
        r -> {
          int v = r.nextInt();
          return h -> h.putOptionalInt(OptionalInt.of(v));
        });
    test(hasher, 0x82a1bb214f21091cL, r -> h -> h.putOptionalInt(OptionalInt.empty()));
  }

  @ParameterizedTest
  @MethodSource("getHashers")
  void testPutOptionalLong(Hasher hasher) {
    test(
        hasher,
        0xaf89096c116f4a83L,
        r -> {
          long v = r.nextLong();
          return h -> h.putOptionalLong(OptionalLong.of(v));
        });
    test(hasher, 0x59c4905ecdcd55e9L, r -> h -> h.putOptionalLong(OptionalLong.empty()));
  }

  @ParameterizedTest
  @MethodSource("getHashers")
  void testPutBooleanArray(Hasher hasher) {
    int maxArraySize = TEST_SIZE;
    SplittableRandom random = new SplittableRandom(0x27084e22c7d4afd7L);
    for (int arraySize = 0; arraySize <= maxArraySize; ++arraySize) {
      test(
          hasher,
          random.nextLong(),
          r -> {
            boolean[] array = new boolean[maxArraySize];
            IntStream.range(0, maxArraySize).forEach(i -> array[i] = r.nextBoolean());
            return h -> h.putBooleanArray(array);
          });
    }
  }

  @ParameterizedTest
  @MethodSource("getHashers")
  void testPutByteArray(Hasher hasher) {
    int maxArraySize = TEST_SIZE / Byte.BYTES;
    SplittableRandom random = new SplittableRandom(0x27084e22c7d4afd7L);
    for (int arraySize = 0; arraySize <= maxArraySize; ++arraySize) {
      test(
          hasher,
          random.nextLong(),
          r -> {
            byte[] array = new byte[maxArraySize];
            r.nextBytes(array);
            return h -> h.putByteArray(array);
          });
    }
  }

  @ParameterizedTest
  @MethodSource("getHashers")
  void testPutShortArray(Hasher hasher) {
    int maxArraySize = TEST_SIZE / Short.BYTES;
    SplittableRandom random = new SplittableRandom(0xdb6f595912d26216L);
    for (int arraySize = 0; arraySize <= maxArraySize; ++arraySize) {
      test(
          hasher,
          random.nextLong(),
          r -> {
            short[] array = new short[maxArraySize];
            IntStream.range(0, maxArraySize).forEach(i -> array[i] = (short) (r.nextInt()));
            return h -> h.putShortArray(array);
          });
    }
  }

  @ParameterizedTest
  @MethodSource("getHashers")
  void testPutCharArray(Hasher hasher) {
    int maxArraySize = TEST_SIZE / Character.BYTES;
    SplittableRandom random = new SplittableRandom(0x356457d83f575a54L);
    for (int arraySize = 0; arraySize <= maxArraySize; ++arraySize) {
      test(
          hasher,
          random.nextLong(),
          r -> {
            char[] array = new char[maxArraySize];
            IntStream.range(0, maxArraySize).forEach(i -> array[i] = (char) (r.nextInt()));
            return h -> h.putCharArray(array);
          });
    }
  }

  @ParameterizedTest
  @MethodSource("getHashers")
  void testPutIntArray(Hasher hasher) {
    int maxArraySize = TEST_SIZE / Integer.BYTES;
    SplittableRandom random = new SplittableRandom(0x2c0e5e78b79b5677L);
    for (int arraySize = 0; arraySize <= maxArraySize; ++arraySize) {
      test(
          hasher,
          random.nextLong(),
          r -> {
            int[] array = r.ints(maxArraySize).toArray();
            return h -> h.putIntArray(array);
          });
    }
  }

  @ParameterizedTest
  @MethodSource("getHashers")
  void testPutLongArray(Hasher hasher) {
    int maxArraySize = TEST_SIZE / Long.BYTES;
    SplittableRandom random = new SplittableRandom(0x4d42b1eee554090eL);
    for (int arraySize = 0; arraySize <= maxArraySize; ++arraySize) {
      test(
          hasher,
          random.nextLong(),
          r -> {
            long[] array = r.longs(maxArraySize).toArray();
            return h -> h.putLongArray(array);
          });
    }
  }

  @ParameterizedTest
  @MethodSource("getHashers")
  void testPutFloatArray(Hasher hasher) {
    int maxArraySize = TEST_SIZE / Float.BYTES;
    SplittableRandom random = new SplittableRandom(0xfa5a6278658eb1c8L);
    for (int arraySize = 0; arraySize <= maxArraySize; ++arraySize) {
      test(
          hasher,
          random.nextLong(),
          r -> {
            float[] array = new float[maxArraySize];
            IntStream.range(0, maxArraySize)
                .forEach(i -> array[i] = Float.intBitsToFloat(r.nextInt()));
            return h -> h.putFloatArray(array);
          });
    }
  }

  @ParameterizedTest
  @MethodSource("getHashers")
  void testPutDoubleArray(Hasher hasher) {
    int maxArraySize = TEST_SIZE / Double.BYTES;
    SplittableRandom random = new SplittableRandom(0x2d7df0f019e2b8deL);
    for (int arraySize = 0; arraySize <= maxArraySize; ++arraySize) {
      test(
          hasher,
          random.nextLong(),
          r -> {
            double[] array = r.longs(maxArraySize).mapToDouble(Double::longBitsToDouble).toArray();
            return h -> h.putDoubleArray(array);
          });
    }
  }

  private static final int TEST_SIZE = 144;

  private static boolean[] copyArray(boolean[] data, int off, int len) {
    boolean[] ret = new boolean[len];
    System.arraycopy(data, off, ret, 0, len);
    return ret;
  }

  private static byte[] copyArray(byte[] data, int off, int len) {
    byte[] ret = new byte[len];
    System.arraycopy(data, off, ret, 0, len);
    return ret;
  }

  private static char[] copyArray(char[] data, int off, int len) {
    char[] ret = new char[len];
    System.arraycopy(data, off, ret, 0, len);
    return ret;
  }

  private static CharSequence asCharSequence(char[] data, int off, int len) {
    return new CharSequence() {
      @Override
      public int length() {
        return len;
      }

      @Override
      public char charAt(int index) {
        return data[off + index];
      }

      @Override
      public CharSequence subSequence(int start, int end) {
        return asCharSequence(data, off + start, end - start);
      }
    };
  }

  private static short[] copyArray(short[] data, int off, int len) {
    short[] ret = new short[len];
    System.arraycopy(data, off, ret, 0, len);
    return ret;
  }

  private static int[] copyArray(int[] data, int off, int len) {
    int[] ret = new int[len];
    System.arraycopy(data, off, ret, 0, len);
    return ret;
  }

  private static long[] copyArray(long[] data, int off, int len) {
    long[] ret = new long[len];
    System.arraycopy(data, off, ret, 0, len);
    return ret;
  }

  private static float[] copyArray(float[] data, int off, int len) {
    float[] ret = new float[len];
    System.arraycopy(data, off, ret, 0, len);
    return ret;
  }

  private static double[] copyArray(double[] data, int off, int len) {
    double[] ret = new double[len];
    System.arraycopy(data, off, ret, 0, len);
    return ret;
  }

  @ParameterizedTest
  @MethodSource("getHashers")
  void testPutBytes(Hasher hasher) {
    int maxPreSize = 72;
    int dataSize = TEST_SIZE / Byte.BYTES;
    byte[] bytes = new byte[maxPreSize];
    SplittableRandom random = new SplittableRandom(0x556f717a1d830e74L);
    random.nextBytes(bytes);
    byte[] data = new byte[dataSize];
    random.nextBytes(data);
    for (int preSize = 0; preSize <= maxPreSize; ++preSize) {
      for (int off = 0; off <= data.length; ++off) {
        for (int len = 0; len <= data.length - off; ++len) {
          HashStream hashStreamActual1 =
              hasher.hashStream().putBytes(bytes, 0, preSize).putBytes(data, off, len);
          HashStream hashStreamActual2 =
              hasher.hashStream().putBytes(bytes, 0, preSize).putBytes(copyArray(data, off, len));
          HashStream hashStreamExpected =
              asNonOptimizedHashStream(hasher.hashStream())
                  .putBytes(bytes, 0, preSize)
                  .putBytes(data, off, len);
          assertHashStreamEquals(hashStreamExpected, hashStreamActual1);
          assertHashStreamEquals(hashStreamExpected, hashStreamActual2);
        }
      }
    }
  }

  @ParameterizedTest
  @MethodSource("getHashers")
  void testPutBooleans(Hasher hasher) {
    int maxPreSize = 72;
    int dataSize = TEST_SIZE;
    byte[] bytes = new byte[maxPreSize];
    SplittableRandom random = new SplittableRandom(0x28a7176d3efd835aL);
    random.nextBytes(bytes);
    boolean[] data = new boolean[dataSize];
    IntStream.range(0, dataSize).forEach(i -> data[i] = random.nextBoolean());
    for (int preSize = 0; preSize <= maxPreSize; ++preSize) {
      for (int off = 0; off <= data.length; ++off) {
        for (int len = 0; len <= data.length - off; ++len) {
          HashStream hashStreamActual1 =
              hasher.hashStream().putBytes(bytes, 0, preSize).putBooleans(data, off, len);
          HashStream hashStreamActual2 =
              hasher
                  .hashStream()
                  .putBytes(bytes, 0, preSize)
                  .putBooleans(copyArray(data, off, len));
          HashStream hashStreamExpected =
              asNonOptimizedHashStream(hasher.hashStream())
                  .putBytes(bytes, 0, preSize)
                  .putBooleans(data, off, len);
          assertHashStreamEquals(hashStreamExpected, hashStreamActual1);
          assertHashStreamEquals(hashStreamExpected, hashStreamActual2);
        }
      }
    }
  }

  @ParameterizedTest
  @MethodSource("getHashers")
  void testPutShorts(Hasher hasher) {
    int maxPreSize = 72;
    int dataSize = TEST_SIZE / Short.BYTES;
    byte[] bytes = new byte[maxPreSize];
    SplittableRandom random = new SplittableRandom(0xf8313a60de96138cL);
    random.nextBytes(bytes);
    short[] data = new short[dataSize];
    IntStream.range(0, dataSize).forEach(i -> data[i] = (short) random.nextInt());
    for (int preSize = 0; preSize <= maxPreSize; ++preSize) {
      for (int off = 0; off <= data.length; ++off) {
        for (int len = 0; len <= data.length - off; ++len) {
          HashStream hashStreamActual1 =
              hasher.hashStream().putBytes(bytes, 0, preSize).putShorts(data, off, len);
          HashStream hashStreamActual2 =
              hasher.hashStream().putBytes(bytes, 0, preSize).putShorts(copyArray(data, off, len));
          HashStream hashStreamExpected =
              asNonOptimizedHashStream(hasher.hashStream())
                  .putBytes(bytes, 0, preSize)
                  .putShorts(data, off, len);
          assertHashStreamEquals(hashStreamExpected, hashStreamActual1);
          assertHashStreamEquals(hashStreamExpected, hashStreamActual2);
        }
      }
    }
  }

  @ParameterizedTest
  @MethodSource("getHashers")
  void testPutChars(Hasher hasher) {
    int maxPreSize = 72;
    int dataSize = TEST_SIZE / Character.BYTES;
    byte[] bytes = new byte[maxPreSize];
    SplittableRandom random = new SplittableRandom(0x37745952fc2af6a0L);
    random.nextBytes(bytes);
    char[] data = new char[dataSize];
    IntStream.range(0, dataSize).forEach(i -> data[i] = (char) random.nextInt());
    for (int preSize = 0; preSize <= maxPreSize; ++preSize) {
      for (int off = 0; off <= data.length; ++off) {
        for (int len = 0; len <= data.length - off; ++len) {
          HashStream hashStreamActual1 =
              hasher.hashStream().putBytes(bytes, 0, preSize).putChars(data, off, len);
          HashStream hashStreamActual2 =
              hasher.hashStream().putBytes(bytes, 0, preSize).putChars(copyArray(data, off, len));
          HashStream hashStreamActual3 =
              hasher
                  .hashStream()
                  .putBytes(bytes, 0, preSize)
                  .putChars(asCharSequence(data, off, len));
          HashStream hashStreamExpected =
              asNonOptimizedHashStream(hasher.hashStream())
                  .putBytes(bytes, 0, preSize)
                  .putChars(data, off, len);
          assertHashStreamEquals(hashStreamExpected, hashStreamActual1);
          assertHashStreamEquals(hashStreamExpected, hashStreamActual2);
          assertHashStreamEquals(hashStreamExpected, hashStreamActual3);
        }
      }
    }
  }

  @ParameterizedTest
  @MethodSource("getHashers")
  void testPutInts(Hasher hasher) {
    int maxPreSize = 72;
    int dataSize = TEST_SIZE / Integer.BYTES;
    byte[] bytes = new byte[maxPreSize];
    SplittableRandom random = new SplittableRandom(0x7a0207a4385ed397L);
    random.nextBytes(bytes);
    int[] data = random.ints(dataSize).toArray();
    for (int preSize = 0; preSize <= maxPreSize; ++preSize) {
      for (int off = 0; off <= data.length; ++off) {
        for (int len = 0; len <= data.length - off; ++len) {
          HashStream hashStreamActual1 =
              hasher.hashStream().putBytes(bytes, 0, preSize).putInts(data, off, len);
          HashStream hashStreamActual2 =
              hasher.hashStream().putBytes(bytes, 0, preSize).putInts(copyArray(data, off, len));
          HashStream hashStreamExpected =
              asNonOptimizedHashStream(hasher.hashStream())
                  .putBytes(bytes, 0, preSize)
                  .putInts(data, off, len);
          assertHashStreamEquals(hashStreamExpected, hashStreamActual1);
          assertHashStreamEquals(hashStreamExpected, hashStreamActual2);
        }
      }
    }
  }

  @ParameterizedTest
  @MethodSource("getHashers")
  void testPutLongs(Hasher hasher) {
    int maxPreSize = 72;
    int dataSize = TEST_SIZE / Long.BYTES;
    byte[] bytes = new byte[maxPreSize];
    SplittableRandom random = new SplittableRandom(0x7a0207a4385ed397L);
    random.nextBytes(bytes);
    long[] data = random.longs(dataSize).toArray();
    for (int preSize = 0; preSize <= maxPreSize; ++preSize) {
      for (int off = 0; off <= data.length; ++off) {
        for (int len = 0; len <= data.length - off; ++len) {
          HashStream hashStreamActual1 =
              hasher.hashStream().putBytes(bytes, 0, preSize).putLongs(data, off, len);
          HashStream hashStreamActual2 =
              hasher.hashStream().putBytes(bytes, 0, preSize).putLongs(copyArray(data, off, len));
          HashStream hashStreamExpected =
              asNonOptimizedHashStream(hasher.hashStream())
                  .putBytes(bytes, 0, preSize)
                  .putLongs(data, off, len);
          assertHashStreamEquals(hashStreamExpected, hashStreamActual1);
          assertHashStreamEquals(hashStreamExpected, hashStreamActual2);
        }
      }
    }
  }

  @ParameterizedTest
  @MethodSource("getHashers")
  void testPutFloats(Hasher hasher) {
    int maxPreSize = 72;
    int dataSize = TEST_SIZE / Float.BYTES;
    byte[] bytes = new byte[maxPreSize];
    SplittableRandom random = new SplittableRandom(0xfa855387c081d64eL);
    random.nextBytes(bytes);
    float[] data = new float[dataSize];
    IntStream.range(0, dataSize).forEach(i -> data[i] = Float.intBitsToFloat(random.nextInt()));
    for (int preSize = 0; preSize <= maxPreSize; ++preSize) {
      for (int off = 0; off <= data.length; ++off) {
        for (int len = 0; len <= data.length - off; ++len) {
          HashStream hashStreamActual1 =
              hasher.hashStream().putBytes(bytes, 0, preSize).putFloats(data, off, len);
          HashStream hashStreamActual2 =
              hasher.hashStream().putBytes(bytes, 0, preSize).putFloats(copyArray(data, off, len));
          HashStream hashStreamExpected =
              asNonOptimizedHashStream(hasher.hashStream())
                  .putBytes(bytes, 0, preSize)
                  .putFloats(data, off, len);
          assertHashStreamEquals(hashStreamExpected, hashStreamActual1);
          assertHashStreamEquals(hashStreamExpected, hashStreamActual2);
        }
      }
    }
  }

  @ParameterizedTest
  @MethodSource("getHashers")
  void testPutDoubles(Hasher hasher) {
    int maxPreSize = 72;
    int dataSize = TEST_SIZE / Long.BYTES;
    byte[] bytes = new byte[maxPreSize];
    SplittableRandom random = new SplittableRandom(0x7a0207a4385ed397L);
    random.nextBytes(bytes);
    double[] data = random.longs(dataSize).mapToDouble(Double::longBitsToDouble).toArray();
    for (int preSize = 0; preSize <= maxPreSize; ++preSize) {
      for (int off = 0; off <= data.length; ++off) {
        for (int len = 0; len <= data.length - off; ++len) {
          HashStream hashStreamActual1 =
              hasher.hashStream().putBytes(bytes, 0, preSize).putDoubles(data, off, len);
          HashStream hashStreamActual2 =
              hasher.hashStream().putBytes(bytes, 0, preSize).putDoubles(copyArray(data, off, len));
          HashStream hashStreamExpected =
              asNonOptimizedHashStream(hasher.hashStream())
                  .putBytes(bytes, 0, preSize)
                  .putDoubles(data, off, len);
          assertHashStreamEquals(hashStreamExpected, hashStreamActual1);
          assertHashStreamEquals(hashStreamExpected, hashStreamActual2);
        }
      }
    }
  }

  @ParameterizedTest
  @MethodSource("getHashers")
  void testPutUnorderedIterableConsistency(Hasher hasher) {
    if (!(hasher instanceof Hasher64)) {
      return;
    }

    SplittableRandom random = new SplittableRandom(0xb3998b48b526dc9cL);
    List<Long> data = random.longs(200).boxed().collect(toList());

    Hasher64 hasher64 = (Hasher64) hasher;

    TestHashStream testHashStream1 = new TestHashStream();
    TestHashStream testHashStream2 = new TestHashStream();
    TestHashStream testHashStream3 = new TestHashStream();

    testHashStream1.putUnorderedIterable(data, l -> hasher64.hashStream().putLong(l).getAsLong());
    testHashStream2.putUnorderedIterable(data, (l, sink) -> sink.putLong(l), () -> hasher64);
    testHashStream3.putUnorderedIterable(data, (l, sink) -> sink.putLong(l), hasher64);

    assertThat(testHashStream1.getData())
        .isEqualTo(testHashStream2.getData())
        .isEqualTo(testHashStream3.getData());
  }
}
