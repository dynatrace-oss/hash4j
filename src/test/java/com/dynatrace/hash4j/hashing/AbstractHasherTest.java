/*
 * Copyright 2022-2024 Dynatrace LLC
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
import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;

import com.dynatrace.hash4j.random.PseudoRandomGenerator;
import com.dynatrace.hash4j.random.PseudoRandomGeneratorProvider;
import com.dynatrace.hash4j.testutils.TestUtils;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.IntStream;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

@TestInstance(PER_CLASS)
abstract class AbstractHasherTest {

  protected static final HashFunnel<byte[]> BYTES_FUNNEL_1 = (input, sink) -> sink.putBytes(input);
  protected static final HashFunnel<byte[]> BYTES_FUNNEL_2 =
      (input, sink) -> {
        for (byte b : input) sink.putByte(b);
      };
  protected static final HashFunnel<CharSequence> CHAR_FUNNEL =
      (input, sink) -> sink.putChars(input);

  protected abstract HashStream createNonOptimizedHashStream(Hasher hasher);

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

  private static final List<TestCase> TEST_CASES =
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

  @ParameterizedTest
  @MethodSource("getHashers")
  void testRandomData(Hasher hasher) {

    int numIterations = 100000;

    SplittableRandom random = new SplittableRandom(0L);

    HashStream resettedRawBytesHashStream = hasher.hashStream();
    HashStream resettedDataHashStream = hasher.hashStream();
    HashStream resettedNonOptimizedHashStream = createNonOptimizedHashStream(hasher);

    for (int k = 0; k < numIterations; ++k) {

      resettedRawBytesHashStream.reset();
      resettedDataHashStream.reset();
      resettedNonOptimizedHashStream.reset();

      HashStream rawBytesHashStream = hasher.hashStream();
      HashStream dataHashStream = hasher.hashStream();
      HashStream nonOptimizedHashStream = createNonOptimizedHashStream(hasher);

      while (random.nextDouble() >= 0.05) {
        TestCase tc = TEST_CASES.get(random.nextInt(TEST_CASES.size()));
        tc.getSinkConsumer().accept(dataHashStream);
        tc.getSinkConsumer().accept(nonOptimizedHashStream);
        for (byte b : tc.getExpected()) {
          rawBytesHashStream.putByte(b);
        }
        tc.getSinkConsumer().accept(resettedDataHashStream);
        tc.getSinkConsumer().accept(resettedNonOptimizedHashStream);
        for (byte b : tc.getExpected()) {
          resettedRawBytesHashStream.putByte(b);
        }
      }
      byte[] rawBytesHash = getBytesAndVerifyRepetitiveGetCalls(rawBytesHashStream);
      byte[] dataHash = getBytesAndVerifyRepetitiveGetCalls(dataHashStream);
      byte[] nonOptimizedHash = getBytesAndVerifyRepetitiveGetCalls(nonOptimizedHashStream);

      byte[] resettedRawBytesHash = getBytesAndVerifyRepetitiveGetCalls(resettedRawBytesHashStream);
      byte[] resettedDataHash = getBytesAndVerifyRepetitiveGetCalls(resettedDataHashStream);
      byte[] resettedNonOptimizedHash =
          getBytesAndVerifyRepetitiveGetCalls(resettedNonOptimizedHashStream);

      assertThat(dataHash)
          .isEqualTo(rawBytesHash)
          .isEqualTo(nonOptimizedHash)
          .isEqualTo(resettedRawBytesHash)
          .isEqualTo(resettedDataHash)
          .isEqualTo(resettedNonOptimizedHash);
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
      int x = ((HashStream32) hashStream).getAsInt();
      for (int i = 24, j = 0; i >= 0; i -= 8, j += 1) {
        result[j] = (byte) ((x >>> i) & 0xFFL);
      }
    } else if (hashStream.getHashBitSize() == 64) {
      long hash = ((HashStream64) hashStream).getAsLong();
      for (int i = 56, j = 0; i >= 0; i -= 8, j += 1) {
        result[j] = (byte) ((hash >>> i) & 0xFFL);
      }
    } else if (hashStream.getHashBitSize() == 128) {
      HashValue128 x = ((HashStream128) hashStream).get();
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

  private void test(
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
          HashStream hashStreamExpected = createNonOptimizedHashStream(hasher);
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
      assertThat(((HashStream128) hashStreamActual).get())
          .isEqualTo(((HashStream128) hashStreamExpected).get());
    }
    if (hashBitSize >= 64) {
      assertThat(((HashStream64) hashStreamActual).getAsLong())
          .isEqualTo(((HashStream64) hashStreamExpected).getAsLong());
    }
    if (hashBitSize >= 32) {
      assertThat(((HashStream32) hashStreamActual).getAsInt())
          .isEqualTo(((HashStream32) hashStreamExpected).getAsInt());
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
            IntStream.range(0, maxArraySize).forEach(i -> array[i] = (short) r.nextInt());
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
            IntStream.range(0, maxArraySize).forEach(i -> array[i] = (char) r.nextInt());
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
              createNonOptimizedHashStream(hasher)
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
              createNonOptimizedHashStream(hasher)
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
              createNonOptimizedHashStream(hasher)
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
              createNonOptimizedHashStream(hasher)
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
              createNonOptimizedHashStream(hasher)
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
              createNonOptimizedHashStream(hasher)
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
              createNonOptimizedHashStream(hasher)
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
              createNonOptimizedHashStream(hasher)
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

    testHashStream1.putUnorderedIterable(data, l -> hasher64.hashStream().putLong(l).getAsLong());
    testHashStream2.putUnorderedIterable(data, (l, sink) -> sink.putLong(l), hasher64);

    assertThat(testHashStream1.getData()).isEqualTo(testHashStream2.getData());
  }

  @ParameterizedTest
  @MethodSource("getHashers")
  void testPutUnorderedIterableConsistency2(Hasher hasher) {
    SplittableRandom random = new SplittableRandom(0xbfae2703ff6a9a24L);
    List<Long> data = random.longs(200).boxed().collect(toList());

    if (hasher instanceof Hasher64) {

      Hasher64 hasher64 = (Hasher64) hasher;

      HashStream64 hashStream1 = hasher64.hashStream();
      HashStream64 hashStream2 = hasher64.hashStream();

      hashStream1.putUnorderedIterable(data, l -> hasher64.hashStream().putLong(l).getAsLong());
      hashStream2.putUnorderedIterable(data, (l, sink) -> sink.putLong(l), hasher64);

      assertThat(hashStream1.getAsLong()).isEqualTo(hashStream2.getAsLong());
    }
  }

  @Test
  void testGetIntFromCharSequence() {
    int numIterations = 1000;
    SplittableRandom random = new SplittableRandom(0L);
    int len = 100;
    char[] chars = new char[1000];
    for (int i = 0; i < numIterations; ++i) {
      int k = random.nextInt();
      int off = random.nextInt(len - 1);
      chars[off] = (char) k;
      chars[off + 1] = (char) (k >>> 16);
      assertThat(AbstractHasher.getInt(new String(chars), off)).isEqualTo(k);
    }
  }

  @Test
  void testGetLongFromCharSequence() {
    int numIterations = 1000;
    SplittableRandom random = new SplittableRandom(0L);
    int len = 100;
    char[] chars = new char[1000];
    for (int i = 0; i < numIterations; ++i) {
      long k = random.nextLong();
      int off = random.nextInt(len - 3);
      chars[off] = (char) k;
      chars[off + 1] = (char) (k >>> 16);
      chars[off + 2] = (char) (k >>> 32);
      chars[off + 3] = (char) (k >>> 48);
      assertThat(AbstractHasher.getLong(new String(chars), off)).isEqualTo(k);
    }
  }

  protected abstract void calculateHashForChecksum(
      byte[] seedBytes, byte[] hashBytes, byte[] dataBytes);

  abstract int getSeedSizeForChecksum();

  abstract int getHashSizeForChecksum();

  abstract String getExpectedChecksum();

  protected static final VarHandle LONG_HANDLE =
      MethodHandles.byteArrayViewVarHandle(long[].class, ByteOrder.LITTLE_ENDIAN);
  protected static final VarHandle INT_HANDLE =
      MethodHandles.byteArrayViewVarHandle(int[].class, ByteOrder.LITTLE_ENDIAN);

  @Test
  void testCheckSum() throws NoSuchAlgorithmException {

    long maxDataLength = 200;
    long numCycles = 10000;

    PseudoRandomGenerator pseudoRandomGenerator =
        PseudoRandomGeneratorProvider.splitMix64_V1().create();

    MessageDigest md = MessageDigest.getInstance("SHA-256");

    int effectiveSeedLength = ((getSeedSizeForChecksum() + 7) >>> 3);

    byte[] seedBytesTemp = new byte[effectiveSeedLength * 8];
    byte[] seedBytes = new byte[getSeedSizeForChecksum()];
    byte[] hashBytes = new byte[getHashSizeForChecksum()];

    for (int dataLength = 0; dataLength <= maxDataLength; ++dataLength) {
      int effectiveDataLength = (dataLength + 7) >> 3;

      byte[] dataBytesTemp = new byte[effectiveDataLength * 8];
      byte[] dataBytes = new byte[dataLength];

      for (long cycle = 0; cycle < numCycles; ++cycle) {
        for (int i = 0; i < effectiveDataLength; ++i) {
          LONG_HANDLE.set(dataBytesTemp, 8 * i, pseudoRandomGenerator.nextLong());
        }
        for (int i = 0; i < effectiveSeedLength; ++i) {
          LONG_HANDLE.set(seedBytesTemp, 8 * i, pseudoRandomGenerator.nextLong());
        }

        System.arraycopy(dataBytesTemp, 0, dataBytes, 0, dataLength);
        System.arraycopy(seedBytesTemp, 0, seedBytes, 0, getSeedSizeForChecksum());

        calculateHashForChecksum(seedBytes, hashBytes, dataBytes);

        md.update(hashBytes);
      }
    }

    String checksum = byteArrayToHexString(md.digest());
    assertThat(checksum).isEqualTo(getExpectedChecksum());
  }

  private static Hasher64 getHasherUsingDefaultImplementations(Hasher64 referenceHasher) {
    return new AbstractHasher64() {

      @Override
      public HashStream64 hashStream() {

        return new AbstractHashStream64() {

          private final HashStream64 referenceHashStream = referenceHasher.hashStream();

          @Override
          public long getAsLong() {
            return referenceHashStream.getAsLong();
          }

          @Override
          public HashStream64 putByte(byte v) {
            return referenceHashStream.putByte(v);
          }

          @Override
          public HashStream64 reset() {
            return referenceHashStream.reset();
          }

          @Override
          public HashStream64 copy() {
            return referenceHashStream.copy();
          }
        };
      }

      @Override
      public long hashBytesToLong(byte[] input, int off, int len) {
        return referenceHasher.hashBytesToLong(input, off, len);
      }

      @Override
      public long hashCharsToLong(CharSequence input) {
        return referenceHasher.hashCharsToLong(input);
      }
    };
  }

  @ParameterizedTest
  @MethodSource("getHashers")
  void testHashLongLongToLong(Hasher hasher) {
    if (!(hasher instanceof Hasher64)) return;
    final Hasher64 hasher64 = (Hasher64) hasher;
    final Hasher64 hasherUsingDefaultImplementation =
        getHasherUsingDefaultImplementations(hasher64);
    int numCycles = 100;
    SplittableRandom random = new SplittableRandom(0x983c79631cff1b49L);
    for (int i = 0; i < numCycles; ++i) {
      long v1 = random.nextLong();
      long v2 = random.nextLong();
      assertThat(hasher64.hashLongLongToLong(v1, v2))
          .isEqualTo(hasher64.hashStream().putLong(v1).putLong(v2).getAsLong())
          .isEqualTo(hasherUsingDefaultImplementation.hashLongLongToLong(v1, v2));
    }
  }

  @ParameterizedTest
  @MethodSource("getHashers")
  void testHashLongLongLongToLong(Hasher hasher) {
    if (!(hasher instanceof Hasher64)) return;
    final Hasher64 hasher64 = (Hasher64) hasher;
    final Hasher64 hasherUsingDefaultImplementation =
        getHasherUsingDefaultImplementations(hasher64);
    int numCycles = 100;
    SplittableRandom random = new SplittableRandom(0xcbc1a1e7856cc27eL);
    for (int i = 0; i < numCycles; ++i) {
      long v1 = random.nextLong();
      long v2 = random.nextLong();
      long v3 = random.nextLong();
      assertThat(hasher64.hashLongLongLongToLong(v1, v2, v3))
          .isEqualTo(hasher64.hashStream().putLong(v1).putLong(v2).putLong(v3).getAsLong())
          .isEqualTo(hasherUsingDefaultImplementation.hashLongLongLongToLong(v1, v2, v3));
    }
  }

  private static CharSequence createCharSequence(int len) {
    return new CharSequence() {
      @Override
      public int length() {
        return len;
      }

      @Override
      public char charAt(int index) {
        return (char) ((index * 0x5851f42d4c957f2dL) >>> 48);
      }

      @NotNull
      @Override
      public CharSequence subSequence(int start, int end) {
        throw new UnsupportedOperationException();
      }
    };
  }

  @Disabled("long running unit test")
  @ParameterizedTest
  @MethodSource("getHashers")
  void testVeryLongCharSequence(Hasher hasher) {
    for (int len = Integer.MAX_VALUE; len > Integer.MAX_VALUE - 16; --len) {
      CharSequence data = createCharSequence(len);

      if (hasher instanceof Hasher128) {
        Hasher128 hasher128 = (Hasher128) hasher;
        HashStream128 hashStream1 = hasher128.hashStream();
        HashStream128 hashStream2 = hasher128.hashStream();
        for (int i = 0; i < data.length(); ++i) {
          hashStream1.putChar(data.charAt(i));
        }
        hashStream2.putChars(data);
        HashValue128 hash1 = hashStream1.get();
        HashValue128 hash2 = hashStream2.get();
        HashValue128 hash3 = hasher128.hashCharsTo128Bits(data);
        assertThat(hash2).isEqualTo(hash1);
        assertThat(hash3).isEqualTo(hash1);
      } else if (hasher instanceof Hasher64) {
        Hasher64 hasher64 = (Hasher64) hasher;
        HashStream64 hashStream1 = hasher64.hashStream();
        HashStream64 hashStream2 = hasher64.hashStream();
        for (int i = 0; i < data.length(); ++i) {
          hashStream1.putChar(data.charAt(i));
        }
        hashStream2.putChars(data);
        long hash1 = hashStream1.getAsLong();
        long hash2 = hashStream2.getAsLong();
        long hash3 = hasher64.hashCharsToLong(data);
        assertThat(hash2).isEqualTo(hash1);
        assertThat(hash3).isEqualTo(hash1);
      } else if (hasher instanceof Hasher32) {
        Hasher32 hasher32 = (Hasher32) hasher;
        HashStream32 hashStream1 = hasher32.hashStream();
        HashStream32 hashStream2 = hasher32.hashStream();
        for (int i = 0; i < data.length(); ++i) {
          hashStream1.putChar(data.charAt(i));
        }
        hashStream2.putChars(data);
        int hash1 = hashStream1.getAsInt();
        int hash2 = hashStream2.getAsInt();
        int hash3 = hasher32.hashCharsToInt(data);
        assertThat(hash2).isEqualTo(hash1);
        assertThat(hash3).isEqualTo(hash1);
      }
    }
  }

  @Disabled("long running unit test")
  @ParameterizedTest
  @MethodSource("getHashers")
  void testVeryLongCharSequenceAfterFirstByte(Hasher hasher) {
    final byte firstByte = 79;
    for (int len = Integer.MAX_VALUE; len > Integer.MAX_VALUE - 16; --len) {
      CharSequence data = createCharSequence(len);
      if (hasher instanceof Hasher128) {
        Hasher128 hasher128 = (Hasher128) hasher;
        HashStream128 hashStream1 = hasher128.hashStream().putByte(firstByte);
        HashStream128 hashStream2 = hasher128.hashStream().putByte(firstByte);
        for (int i = 0; i < data.length(); ++i) {
          hashStream1.putChar(data.charAt(i));
        }
        hashStream2.putChars(data);
        HashValue128 hash1 = hashStream1.get();
        HashValue128 hash2 = hashStream2.get();
        assertThat(hash1).isEqualTo(hash2);
      } else if (hasher instanceof Hasher64) {
        Hasher64 hasher64 = (Hasher64) hasher;
        HashStream64 hashStream1 = hasher64.hashStream().putByte(firstByte);
        HashStream64 hashStream2 = hasher64.hashStream().putByte(firstByte);
        for (int i = 0; i < data.length(); ++i) {
          hashStream1.putChar(data.charAt(i));
        }
        hashStream2.putChars(data);
        long hash1 = hashStream1.getAsLong();
        long hash2 = hashStream2.getAsLong();
        assertThat(hash1).isEqualTo(hash2);
      } else if (hasher instanceof Hasher32) {
        Hasher32 hasher32 = (Hasher32) hasher;
        HashStream32 hashStream1 = hasher32.hashStream().putByte(firstByte);
        HashStream32 hashStream2 = hasher32.hashStream().putByte(firstByte);
        for (int i = 0; i < data.length(); ++i) {
          hashStream1.putChar(data.charAt(i));
        }
        hashStream2.putChars(data);
        int hash1 = hashStream1.getAsInt();
        int hash2 = hashStream2.getAsInt();
        assertThat(hash1).isEqualTo(hash2);
      }
    }
  }

  private static final int ARRAY_MAX_SIZE =
      Integer.MAX_VALUE - 2; // see https://www.baeldung.com/java-arrays-max-size

  @Disabled("long running unit test allocating a lot of memory")
  @ParameterizedTest
  @MethodSource("getHashers")
  void testVeryLongByteSequence(Hasher hasher) {
    for (int len = ARRAY_MAX_SIZE; len > ARRAY_MAX_SIZE - 16; --len) {
      byte[] data = new byte[len];
      SplittableRandom random = new SplittableRandom(0L);
      random.nextBytes(data);

      if (hasher instanceof Hasher128) {
        Hasher128 hasher128 = (Hasher128) hasher;
        HashStream128 hashStream1 = hasher128.hashStream();
        HashStream128 hashStream2 = hasher128.hashStream();
        for (byte b : data) {
          hashStream1.putByte(b);
        }
        hashStream2.putBytes(data);
        HashValue128 hash1 = hashStream1.get();
        HashValue128 hash2 = hashStream2.get();
        HashValue128 hash3 = hasher128.hashBytesTo128Bits(data);
        assertThat(hash2).isEqualTo(hash1);
        assertThat(hash3).isEqualTo(hash1);
      } else if (hasher instanceof Hasher64) {
        Hasher64 hasher64 = (Hasher64) hasher;
        HashStream64 hashStream1 = hasher64.hashStream();
        HashStream64 hashStream2 = hasher64.hashStream();
        for (byte b : data) {
          hashStream1.putByte(b);
        }
        hashStream2.putBytes(data);
        long hash1 = hashStream1.getAsLong();
        long hash2 = hashStream2.getAsLong();
        long hash3 = hasher64.hashBytesToLong(data);
        assertThat(hash2).isEqualTo(hash1);
        assertThat(hash3).isEqualTo(hash1);
      } else if (hasher instanceof Hasher32) {
        Hasher32 hasher32 = (Hasher32) hasher;
        HashStream32 hashStream1 = hasher32.hashStream();
        HashStream32 hashStream2 = hasher32.hashStream();
        for (byte b : data) {
          hashStream1.putByte(b);
        }
        hashStream2.putBytes(data);
        int hash1 = hashStream1.getAsInt();
        int hash2 = hashStream2.getAsInt();
        int hash3 = hasher32.hashBytesToInt(data);
        assertThat(hash2).isEqualTo(hash1);
        assertThat(hash3).isEqualTo(hash1);
      }
    }
  }

  @ParameterizedTest
  @MethodSource("getHashers")
  void testCopy(Hasher hasher) {
    final int numIterations = 10;
    final byte[] bytes = new byte[100];
    final SplittableRandom random = new SplittableRandom();
    random.nextBytes(bytes);

    final HashStream checkPoint = hasher.hashStream().putBytes(bytes);

    for (int i = 0; i < numIterations; i++) {
      final int index = random.nextInt();
      final HashStream expected = hasher.hashStream().putBytes(bytes).putInt(index);
      final HashStream actual = checkPoint.copy().putInt(index);
      assertHashStreamEquals(expected, actual);
    }
  }
}
