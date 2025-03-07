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
package com.dynatrace.hash4j.hashing;

import static com.dynatrace.hash4j.hashing.AbstractHasher.*;
import static com.dynatrace.hash4j.testutils.TestUtils.byteArrayToHexString;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;

import com.dynatrace.hash4j.testutils.TestUtils;
import com.google.common.base.MoreObjects;
import com.google.common.base.Splitter;
import java.io.File;
import java.io.FileNotFoundException;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.net.URISyntaxException;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

@TestInstance(PER_CLASS)
abstract class AbstractHasherTest {

  protected static final HashFunnel<byte[]> BYTES_FUNNEL = (input, sink) -> sink.putBytes(input);

  protected abstract HashStream createNonOptimizedHashStream(Hasher hasher);

  protected abstract List<? extends Hasher> getHashers();

  protected abstract int getBlockLengthInBytes();

  public static final class ChecksumRecord {
    private final long dataSize;
    private final int numCycles;
    private final long seed;
    private final String checksum;

    public ChecksumRecord(long dataSize, int numCycles, long seed, String checksumString) {
      this.dataSize = dataSize;
      this.numCycles = numCycles;
      this.seed = seed;
      this.checksum = checksumString;
    }

    public String getChecksum() {
      return checksum;
    }

    public long getDataSize() {
      return dataSize;
    }

    public int getNumCycles() {
      return numCycles;
    }

    public long getSeed() {
      return seed;
    }

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this)
          .add("dataSize", dataSize)
          .add("numCycles", numCycles)
          .toString();
    }
  }

  private List<ChecksumRecord> getChecksumRecords() {

    ClassLoader classLoader = getClass().getClassLoader();
    File file;
    try {
      file = new File(classLoader.getResource(getChecksumResourceFileName()).toURI());
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
    List<ChecksumRecord> checksumRecords = new ArrayList<>();

    try (Scanner scanner = new Scanner(file, StandardCharsets.UTF_8.name())) {
      while (scanner.hasNextLine()) {
        List<String> s = Splitter.on(',').splitToList(scanner.nextLine());

        long dataSize = Long.parseLong(s.get(0));
        int numCycles = Integer.parseInt(s.get(1));
        long seed = TestUtils.hexStringToLong(s.get(2));
        String checksumString = s.get(3);
        checksumRecords.add(new ChecksumRecord(dataSize, numCycles, seed, checksumString));
      }

    } catch (FileNotFoundException e) {
      throw new RuntimeException(e);
    }
    return checksumRecords;
  }

  protected abstract String getChecksumResourceFileName();

  @ParameterizedTest
  @MethodSource("getHashers")
  void testRepetitiveGet(Hasher hasher) {
    int maxLen = 3 * getBlockLengthInBytes() + 1;
    SplittableRandom random = new SplittableRandom(0xba77cd23c7b2d080L);
    HashStream hashStream = hasher.hashStream();
    for (int len = 0; len <= maxLen; ++len) {
      byte[] result = getBytes(hashStream);

      int numRecalculations = 3;
      for (int i = 0; i < numRecalculations; ++i) {
        assertThat(getBytes(hashStream)).isEqualTo(result);
      }
      hashStream.putByte((byte) random.nextInt());
    }
  }

  @ParameterizedTest
  @MethodSource("getHashers")
  void testReset1(Hasher hasher) {
    int maxLen = 3 * getBlockLengthInBytes() + 1;
    SplittableRandom random = new SplittableRandom(0xba77cd23c7b2d080L);
    for (int len = 0; len <= maxLen; ++len) {
      int finalLen = len;
      Supplier<String> descriptionSupplier = () -> "len = " + finalLen;
      byte[] data = new byte[len];
      random.nextBytes(data);
      assertHashStreamEquals(
          hasher.hashStream(), hasher.hashStream().putBytes(data).reset(), descriptionSupplier);
    }
  }

  @ParameterizedTest
  @MethodSource("getHashers")
  void testReset2(Hasher hasher) {
    int maxLen = 3 * getBlockLengthInBytes() / 8 + 1;
    int maxOffset = 8;
    SplittableRandom random = new SplittableRandom(0xba77cd23c7b2d080L);

    long[] regularData = random.longs(maxLen).toArray();
    byte[] offsetData = new byte[maxOffset];
    random.nextBytes(offsetData);

    for (int off = 0; off <= maxOffset; ++off) {
      HashStream reference = hasher.hashStream();
      reference.putBytes(offsetData, 0, off);
      for (int len = 0; len <= maxLen; ++len) {
        int finalLen = len;
        int finalOff = off;
        Supplier<String> descriptionSupplier = () -> "off = " + finalOff + ", len = " + finalLen;
        HashStream other = reference.copy().reset();
        other.putBytes(offsetData, 0, off);
        for (int len1 = 0; len1 < len; ++len1) {
          other.putLong(regularData[len1]);
        }
        assertHashStreamEquals(reference, other, descriptionSupplier);
        if (len < maxLen) reference.putLong(regularData[len]);
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
        long hash32Reference = hasher32.hashToInt(data, BYTES_FUNNEL);
        long hash32 = hasher32.hashBytesToInt(data);
        long hash32WithOffset = hasher32.hashBytesToInt(dataWithOffset, offset, length);
        assertThat(hash32).isEqualTo(hash32Reference);
        assertThat(hash32WithOffset).isEqualTo(hash32Reference);
      }
      if (hasher instanceof Hasher64) {
        Hasher64 hasher64 = (Hasher64) hasher;
        long hash64Reference = hasher64.hashToLong(data, BYTES_FUNNEL);
        long hash64 = hasher64.hashBytesToLong(data);
        long hash64WithOffset = hasher64.hashBytesToLong(dataWithOffset, offset, length);
        assertThat(hash64).isEqualTo(hash64Reference);
        assertThat(hash64WithOffset).isEqualTo(hash64Reference);
      }
      if (hasher instanceof Hasher128) {
        Hasher128 hasher128 = (Hasher128) hasher;
        HashValue128 hash128Reference = hasher128.hashTo128Bits(data, BYTES_FUNNEL);
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

  private void testPut(
      Hasher hasher,
      long seed,
      Function<SplittableRandom, Consumer<HashStream>> contributorGenerator) {
    int maxPreSize = getBlockLengthInBytes() + 1;
    int maxPostSize = 10;
    int numCycles = 1;
    byte[] preData = new byte[maxPreSize];
    byte[] postData = new byte[maxPostSize];
    SplittableRandom random = new SplittableRandom(seed);
    for (int i = 0; i < numCycles; ++i) {
      Consumer<HashStream> contributor = contributorGenerator.apply(random);
      random.nextBytes(preData);
      random.nextBytes(postData);
      HashStream hashStreamActualBase = hasher.hashStream();
      HashStream hashStreamExpectedBase = createNonOptimizedHashStream(hasher);
      for (int preSize = 0; preSize <= maxPreSize; preSize += 1) {
        int finalPreSize = preSize;
        Supplier<String> descriptionSupplier = () -> "preSize = " + finalPreSize;
        HashStream hashStreamActual = hashStreamActualBase.copy();
        HashStream hashStreamExpected = hashStreamExpectedBase.copy();
        contributor.accept(hashStreamActual);
        contributor.accept(hashStreamExpected);
        assertHashStreamEquals(hashStreamExpected, hashStreamActual, descriptionSupplier);
        for (int j = 0; j < maxPostSize; ++j) {
          hashStreamActual.putByte(postData[j]);
          hashStreamExpected.putByte(postData[j]);
          assertHashStreamEquals(hashStreamExpected, hashStreamActual, descriptionSupplier);
        }
        if (preSize < maxPreSize) {
          hashStreamActualBase.putByte(preData[preSize]);
          hashStreamExpectedBase.putByte(preData[preSize]);
        }
      }
    }
  }

  private static void assertHashStreamEquals(
      HashStream hashStreamExpected,
      HashStream hashStreamActual,
      Supplier<String> descriptionSupplier) {
    assertThat(hashStreamActual.getHashBitSize()).isEqualTo(hashStreamExpected.getHashBitSize());
    int hashBitSize = hashStreamExpected.getHashBitSize();
    if (hashBitSize >= 128) {
      assertThat(((HashStream128) hashStreamActual).get())
          .describedAs(descriptionSupplier)
          .isEqualTo(((HashStream128) hashStreamExpected).get());
    }
    if (hashBitSize >= 64) {
      assertThat(((HashStream64) hashStreamActual).getAsLong())
          .describedAs(descriptionSupplier)
          .isEqualTo(((HashStream64) hashStreamExpected).getAsLong());
    }
    if (hashBitSize >= 32) {
      assertThat(((HashStream32) hashStreamActual).getAsInt())
          .describedAs(descriptionSupplier)
          .isEqualTo(((HashStream32) hashStreamExpected).getAsInt());
    }
  }

  @ParameterizedTest
  @MethodSource("getHashers")
  void testPutByte(Hasher hasher) {
    testPut(
        hasher,
        0x513d67321a2dd796L,
        r -> {
          byte v = (byte) r.nextInt();
          return h -> h.putByte(v);
        });
  }

  @ParameterizedTest
  @MethodSource("getHashers")
  void testPutLong(Hasher hasher) {
    testPut(
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
    testPut(
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
    testPut(
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
    testPut(
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
    testPut(
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
    testPut(
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
    testPut(
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
    testPut(
        hasher,
        0x53112c25f749193eL,
        r -> {
          int v = r.nextInt();
          return h -> h.putOptionalInt(OptionalInt.of(v));
        });
    testPut(hasher, 0x82a1bb214f21091cL, r -> h -> h.putOptionalInt(OptionalInt.empty()));
  }

  @ParameterizedTest
  @MethodSource("getHashers")
  void testPutOptionalLong(Hasher hasher) {
    testPut(
        hasher,
        0xaf89096c116f4a83L,
        r -> {
          long v = r.nextLong();
          return h -> h.putOptionalLong(OptionalLong.of(v));
        });
    testPut(hasher, 0x59c4905ecdcd55e9L, r -> h -> h.putOptionalLong(OptionalLong.empty()));
  }

  @ParameterizedTest
  @MethodSource("getHashers")
  void testPutOptionalDouble(Hasher hasher) {
    testPut(
        hasher,
        0xadc5bcff67337ef9L,
        r -> {
          long v = r.nextLong();
          return h -> h.putOptionalDouble(OptionalDouble.of(Double.longBitsToDouble(v)));
        });
    testPut(hasher, 0x27d92043db8c65daL, r -> h -> h.putOptionalDouble(OptionalDouble.empty()));
  }

  @ParameterizedTest
  @MethodSource("getHashers")
  void testPutUUID(Hasher hasher) {
    testPut(
        hasher,
        0x47938a6c7d9ccbe2L,
        r -> {
          UUID uuid = new UUID(r.nextLong(), r.nextLong());
          return h -> h.putUUID(uuid);
        });
  }

  @ParameterizedTest
  @MethodSource("getHashers")
  void testPutNullable(Hasher hasher) {
    testPut(
        hasher,
        0xae02075417034c1cL,
        r -> {
          Long o = Long.valueOf(r.nextLong());
          return h -> h.putNullable(o, (obj, sink) -> sink.putLong(obj.longValue()));
        });
    testPut(hasher, 0xf872fde629e6f4f4L, r -> h -> h.putNullable(null, (obj, sink) -> fail()));
  }

  @Disabled
  @ParameterizedTest
  @MethodSource("getHashers")
  void testPutBooleanArray(Hasher hasher) {
    int maxArraySize = TEST_SIZE;
    SplittableRandom random = new SplittableRandom(0x27084e22c7d4afd7L);
    for (int arraySize = 0; arraySize <= maxArraySize; ++arraySize) {
      int finalArraySize = arraySize;
      boolean[] array = new boolean[finalArraySize];
      testPut(
          hasher,
          random.nextLong(),
          r -> {
            for (int i = 0; i < finalArraySize; ++i) array[i] = r.nextBoolean();
            return h -> h.putBooleanArray(array);
          });
    }
  }

  @Disabled
  @ParameterizedTest
  @MethodSource("getHashers")
  void testPutByteArray(Hasher hasher) {
    int maxArraySize = TEST_SIZE / Byte.BYTES + 1;
    SplittableRandom random = new SplittableRandom(0x27084e22c7d4afd7L);
    for (int arraySize = 0; arraySize <= maxArraySize; ++arraySize) {
      int finalArraySize = arraySize;
      byte[] array = new byte[finalArraySize];
      testPut(
          hasher,
          random.nextLong(),
          r -> {
            r.nextBytes(array);
            return h -> h.putByteArray(array);
          });
    }
  }

  @Disabled
  @ParameterizedTest
  @MethodSource("getHashers")
  void testPutShortArray(Hasher hasher) {
    int maxArraySize = TEST_SIZE / Short.BYTES;
    SplittableRandom random = new SplittableRandom(0xdb6f595912d26216L);
    for (int arraySize = 0; arraySize <= maxArraySize; ++arraySize) {
      int finalArraySize = arraySize;
      short[] array = new short[finalArraySize];
      testPut(
          hasher,
          random.nextLong(),
          r -> {
            for (int i = 0; i < finalArraySize; ++i) array[i] = (short) r.nextInt();
            return h -> h.putShortArray(array);
          });
    }
  }

  @Disabled
  @ParameterizedTest
  @MethodSource("getHashers")
  void testPutCharArray(Hasher hasher) {
    int maxArraySize = TEST_SIZE / Character.BYTES;
    SplittableRandom random = new SplittableRandom(0x356457d83f575a54L);
    for (int arraySize = 0; arraySize <= maxArraySize; ++arraySize) {
      int finalArraySize = arraySize;
      char[] array = new char[finalArraySize];
      testPut(
          hasher,
          random.nextLong(),
          r -> {
            for (int i = 0; i < finalArraySize; ++i) array[i] = (char) r.nextInt();
            return h -> h.putCharArray(array);
          });
    }
  }

  @Disabled
  @ParameterizedTest
  @MethodSource("getHashers")
  void testPutIntArray(Hasher hasher) {
    int maxArraySize = TEST_SIZE / Integer.BYTES;
    SplittableRandom random = new SplittableRandom(0x2c0e5e78b79b5677L);
    for (int arraySize = 0; arraySize <= maxArraySize; ++arraySize) {
      int finalArraySize = arraySize;
      int[] array = new int[finalArraySize];
      testPut(
          hasher,
          random.nextLong(),
          r -> {
            for (int i = 0; i < finalArraySize; ++i) array[i] = r.nextInt();
            return h -> h.putIntArray(array);
          });
    }
  }

  @Disabled
  @ParameterizedTest
  @MethodSource("getHashers")
  void testPutLongArray(Hasher hasher) {
    int maxArraySize = TEST_SIZE / Long.BYTES;
    SplittableRandom random = new SplittableRandom(0x4d42b1eee554090eL);
    for (int arraySize = 0; arraySize <= maxArraySize; ++arraySize) {
      int finalArraySize = arraySize;
      long[] array = new long[finalArraySize];
      testPut(
          hasher,
          random.nextLong(),
          r -> {
            for (int i = 0; i < finalArraySize; ++i) array[i] = r.nextLong();
            return h -> h.putLongArray(array);
          });
    }
  }

  @Disabled
  @ParameterizedTest
  @MethodSource("getHashers")
  void testPutFloatArray(Hasher hasher) {
    int maxArraySize = TEST_SIZE / Float.BYTES;
    SplittableRandom random = new SplittableRandom(0xfa5a6278658eb1c8L);
    for (int arraySize = 0; arraySize <= maxArraySize; ++arraySize) {
      int finalArraySize = arraySize;
      float[] array = new float[finalArraySize];
      testPut(
          hasher,
          random.nextLong(),
          r -> {
            for (int i = 0; i < finalArraySize; ++i) array[i] = Float.intBitsToFloat(r.nextInt());
            return h -> h.putFloatArray(array);
          });
    }
  }

  @Disabled
  @ParameterizedTest
  @MethodSource("getHashers")
  void testPutDoubleArray(Hasher hasher) {
    int maxArraySize = TEST_SIZE / Double.BYTES;
    SplittableRandom random = new SplittableRandom(0x2d7df0f019e2b8deL);
    for (int arraySize = 0; arraySize <= maxArraySize; ++arraySize) {
      int finalArraySize = arraySize;
      double[] array = new double[finalArraySize];
      testPut(
          hasher,
          random.nextLong(),
          r -> {
            for (int i = 0; i < finalArraySize; ++i)
              array[i] = Double.longBitsToDouble(r.nextLong());
            return h -> h.putDoubleArray(array);
          });
    }
  }

  private static final int TEST_SIZE = 144;

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

  private static CharSequence asCharSequence(byte[] data, int off, int len) {
    return new CharSequence() {
      @Override
      public int length() {
        return len;
      }

      @Override
      public char charAt(int index) {
        return getChar(data, off + 2 * index);
      }

      @Override
      public CharSequence subSequence(int start, int end) {
        return asCharSequence(data, off + 2 * start, end - start);
      }
    };
  }

  private static CharSequence asCharSequence(byte[][] data, int blockSize, int off, int len) {
    return new CharSequence() {
      @Override
      public int length() {
        return len;
      }

      @Override
      public char charAt(int index) {
        long i = 2L * (off + index);
        return getChar(data[(int) (i / blockSize)], (int) (i % blockSize));
      }

      @Override
      public CharSequence subSequence(int start, int end) {
        return asCharSequence(data, blockSize, off + start, end - start);
      }
    };
  }

  @Disabled
  @ParameterizedTest
  @MethodSource("getHashers")
  void testPutBytesVsPutByteVsHashBytes(Hasher hasher) {
    int maxDataSize = 3 * getBlockLengthInBytes();
    byte[] bytes = new byte[maxDataSize];
    int numIterations = 3;
    SplittableRandom random = new SplittableRandom(0x25a5bebe01a9ba17L);
    HashStream hashStreamPutByte = hasher.hashStream();
    HashStream hashStreamPutBytes = hasher.hashStream();
    for (int i = 0; i < numIterations; ++i) {
      hashStreamPutByte.reset();
      hashStreamPutBytes.reset();
      random.nextBytes(bytes);
      assertHashStream(hashStreamPutByte, hasher, bytes, 0, 0);
      assertHashStream(hashStreamPutBytes, hasher, bytes, 0, 0);
      for (int size = 1; size <= maxDataSize; ++size) {
        hashStreamPutByte.putByte(bytes[size - 1]);
        hashStreamPutBytes.reset();
        hashStreamPutBytes.putBytes(bytes, 0, size);
        assertHashStream(hashStreamPutByte, hasher, bytes, 0, size);
        assertHashStream(hashStreamPutBytes, hasher, bytes, 0, size);
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
    testHashStream2.putUnorderedIterable(data, (l, sink) -> sink.putLong(l), hasher64);
    testHashStream3.putUnorderedIterable(data, (l, sink) -> sink.putLong(l), hasher64.hashStream());

    assertThat(testHashStream1.getData())
        .isEqualTo(testHashStream2.getData())
        .isEqualTo(testHashStream3.getData());
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
      HashStream64 hashStream3 = hasher64.hashStream();

      hashStream1.putUnorderedIterable(data, l -> hasher64.hashStream().putLong(l).getAsLong());
      hashStream2.putUnorderedIterable(data, (l, sink) -> sink.putLong(l), hasher64);
      hashStream3.putUnorderedIterable(data, (l, sink) -> sink.putLong(l), hasher64.hashStream());

      assertThat(hashStream1.getAsLong())
          .isEqualTo(hashStream2.getAsLong())
          .isEqualTo(hashStream3.getAsLong());
    }
  }

  @ParameterizedTest
  @MethodSource("getHashers")
  void testPutUnorderedIterableIllegalArgumentException(Hasher hasher) {

    if (hasher instanceof Hasher64) {
      Hasher64 hasher64 = (Hasher64) hasher;
      HashStream64 hashStream = hasher64.hashStream();
      List<Long> data = Collections.emptyList();
      assertThatIllegalArgumentException()
          .isThrownBy(
              () ->
                  hashStream.putUnorderedIterable(data, (l, sink) -> sink.putLong(l), hashStream));
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
      assertThat(getInt(new String(chars), off)).isEqualTo(k);
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
      assertThat(getLong(new String(chars), off)).isEqualTo(k);
    }
  }

  protected abstract void calculateHashForChecksum(
      byte[] seedBytes, byte[] hashBytes, byte[] dataBytes);

  protected abstract void calculateHashForChecksum(
      byte[] seedBytes, byte[] hashBytes, CharSequence charSequence);

  abstract int getSeedSizeForChecksum();

  abstract int getHashSizeForChecksum();

  protected static final VarHandle LONG_HANDLE =
      MethodHandles.byteArrayViewVarHandle(long[].class, ByteOrder.LITTLE_ENDIAN);
  protected static final VarHandle INT_HANDLE =
      MethodHandles.byteArrayViewVarHandle(int[].class, ByteOrder.LITTLE_ENDIAN);

  private void generateRandomBytes(byte[] data, SplitMix64 pseudoRandomGenerator) {
    int i = 0;
    for (; i <= data.length - 8; i += 8) {
      LONG_HANDLE.set(data, i, pseudoRandomGenerator.nextLong());
    }
    if (i < data.length) {
      long l = pseudoRandomGenerator.nextLong();
      do {
        data[i] = (byte) (l >>> (8 * i));
        i += 1;
      } while (i < data.length);
    }
  }

  protected abstract List<HashStream> getHashStreams(byte[] seedBytes);

  protected abstract void getHashBytes(List<HashStream> hashStreams, byte[] hashBytes);

  private static final int ARRAY_MAX_SIZE =
      Integer.MAX_VALUE - 8; // see https://www.baeldung.com/java-arrays-max-size

  private static final class SplitMix64 {

    private long state;

    public long nextLong() {
      state += 0x9e3779b97f4a7c15L;
      long z = state;
      z = (z ^ (z >>> 30)) * 0xbf58476d1ce4e5b9L;
      z = (z ^ (z >>> 27)) * 0x94d049bb133111ebL;
      return z ^ (z >>> 31);
    }

    public void reset(long seed) {
      this.state = seed;
    }

    public long getState() {
      return state;
    }
  }

  private static boolean testLength(long len) {
    return len <= 1_000_000L;
  }

  @ResourceLock(value = "memory-intensive")
  @Test
  void testCheckSumsHashBytes() throws NoSuchAlgorithmException {

    byte[] seedBytes = new byte[getSeedSizeForChecksum()];
    byte[] hashBytes = new byte[getHashSizeForChecksum()];

    for (ChecksumRecord checksumRecord : getChecksumRecords()) {

      long dataLength = checksumRecord.getDataSize();

      if (dataLength > ARRAY_MAX_SIZE || !testLength(dataLength)) {
        continue;
      }
      int dataLengthInt = Math.toIntExact(checksumRecord.getDataSize());
      long numCycles = checksumRecord.getNumCycles();

      SplitMix64 pseudoRandomGenerator = new SplitMix64();
      pseudoRandomGenerator.reset(checksumRecord.getSeed());

      MessageDigest md = MessageDigest.getInstance("SHA-256");

      byte[] dataBytes = new byte[dataLengthInt];

      for (long cycle = 0; cycle < numCycles; ++cycle) {
        generateRandomBytes(seedBytes, pseudoRandomGenerator);
        generateRandomBytes(dataBytes, pseudoRandomGenerator);
        calculateHashForChecksum(seedBytes, hashBytes, dataBytes);
        md.update(hashBytes);
      }
      String checksum = byteArrayToHexString(md.digest());
      assertThat(checksum)
          .describedAs(() -> checksumRecord.toString())
          .isEqualTo(checksumRecord.getChecksum());
    }
  }

  private static final class GeneratedBufferingCharSequence implements CharSequence {

    private static final int NUM_CHARS_IN_BUFFER_EXPONENT = 11;
    private static final int NUM_CHARS_IN_BUFFER = 1 << NUM_CHARS_IN_BUFFER_EXPONENT;

    private final byte[] buffer = new byte[NUM_CHARS_IN_BUFFER << 1];
    private SplitMix64 pseudoRandomGenerator;
    private long randomResetState;
    private int length;
    private long maxCharIdx;

    public void reset(int length, SplitMix64 pseudoRandomGenerator) {
      this.length = length;
      this.pseudoRandomGenerator = pseudoRandomGenerator;
      this.randomResetState = pseudoRandomGenerator.getState();
      this.maxCharIdx = 0;
    }

    @Override
    public int length() {
      return length;
    }

    @Override
    public char charAt(int index) {
      if (index < maxCharIdx - NUM_CHARS_IN_BUFFER) {
        this.maxCharIdx = 0;
        this.pseudoRandomGenerator.reset(randomResetState);
      }
      while (index >= maxCharIdx) {
        setLong(
            buffer,
            ((int) maxCharIdx & (NUM_CHARS_IN_BUFFER - 1)) << 1,
            pseudoRandomGenerator.nextLong());
        maxCharIdx += 4;
      }
      return getChar(buffer, (index & (NUM_CHARS_IN_BUFFER - 1)) << 1);
    }

    @Override
    public CharSequence subSequence(int start, int end) {
      throw new UnsupportedOperationException();
    }
  }

  @Test
  void testCheckSumsHashChars() throws NoSuchAlgorithmException {
    byte[] seedBytes = new byte[getSeedSizeForChecksum()];
    byte[] hashBytes = new byte[getHashSizeForChecksum()];

    GeneratedBufferingCharSequence charSequence = new GeneratedBufferingCharSequence();
    SplitMix64 pseudoRandomGenerator = new SplitMix64();

    for (ChecksumRecord checksumRecord : getChecksumRecords()) {

      long dataLength = checksumRecord.getDataSize();

      if (dataLength % 2 != 0 || dataLength / 2 > Integer.MAX_VALUE || !testLength(dataLength)) {
        continue;
      }
      int len = Math.toIntExact(checksumRecord.getDataSize() / 2);
      long numCycles = checksumRecord.getNumCycles();

      pseudoRandomGenerator.reset(checksumRecord.getSeed());

      MessageDigest md = MessageDigest.getInstance("SHA-256");

      for (long cycle = 0; cycle < numCycles; ++cycle) {
        generateRandomBytes(seedBytes, pseudoRandomGenerator);

        charSequence.reset(len, pseudoRandomGenerator);
        calculateHashForChecksum(seedBytes, hashBytes, charSequence);
        md.update(hashBytes);
      }
      String checksum = byteArrayToHexString(md.digest());
      assertThat(checksum)
          .describedAs(() -> checksumRecord.toString())
          .isEqualTo(checksumRecord.getChecksum());
    }
  }

  @Test
  void testCheckSumsPutBytes() throws NoSuchAlgorithmException {

    SplittableRandom random = new SplittableRandom(0x07d1cadabc2405d3L);

    byte[] seedBytes = new byte[getSeedSizeForChecksum()];
    byte[] hashBytes = new byte[getHashSizeForChecksum()];

    for (ChecksumRecord checksumRecord : getChecksumRecords()) {

      long dataLength = checksumRecord.getDataSize();
      if (!testLength(dataLength)) continue;

      int maxIncrement = (int) Math.max(1, Math.min(1 << 16, dataLength / 4));

      long numCycles = checksumRecord.getNumCycles();

      SplitMix64 pseudoRandomGenerator = new SplitMix64();
      pseudoRandomGenerator.reset(checksumRecord.getSeed());

      MessageDigest md = MessageDigest.getInstance("SHA-256");

      byte[] data = new byte[maxIncrement + 8];

      for (long cycle = 0; cycle < numCycles; ++cycle) {
        generateRandomBytes(seedBytes, pseudoRandomGenerator);
        List<HashStream> hashStreams = getHashStreams(seedBytes);

        long remaining = dataLength;
        int availableBytes = 0;
        while (remaining > 0) {
          int increment = (int) Math.min(remaining, random.nextLong(maxIncrement + 1));
          while (availableBytes < increment) {
            setLong(data, availableBytes, pseudoRandomGenerator.nextLong());
            availableBytes += 8;
          }
          for (int i = 0; i < hashStreams.size(); ++i) {
            hashStreams.get(i).putBytes(data, 0, increment);
          }
          setLong(data, 0, getLong(data, increment));
          availableBytes -= increment;
          remaining -= increment;
        }
        getHashBytes(hashStreams, hashBytes);
        md.update(hashBytes);
      }
      String checksum = byteArrayToHexString(md.digest());
      assertThat(checksum)
          .describedAs(() -> checksumRecord.toString())
          .isEqualTo(checksumRecord.getChecksum());
    }
  }

  @Test
  void testCheckSumsPutChars() throws NoSuchAlgorithmException {

    SplittableRandom random = new SplittableRandom(0xf234c9e987e251e8L);

    byte[] seedBytes = new byte[getSeedSizeForChecksum()];
    byte[] hashBytes = new byte[getHashSizeForChecksum()];

    for (ChecksumRecord checksumRecord : getChecksumRecords()) {

      long dataLength = checksumRecord.getDataSize();
      if (!testLength(dataLength)) continue;

      int maxIncrement = (int) Math.max(1, Math.min(1 << 15, dataLength / 8));

      long numCycles = checksumRecord.getNumCycles();

      SplitMix64 pseudoRandomGenerator = new SplitMix64();
      pseudoRandomGenerator.reset(checksumRecord.getSeed());

      MessageDigest md = MessageDigest.getInstance("SHA-256");

      byte[] data = new byte[2 * maxIncrement + 8];

      for (long cycle = 0; cycle < numCycles; ++cycle) {
        generateRandomBytes(seedBytes, pseudoRandomGenerator);
        List<HashStream> hashStreams = getHashStreams(seedBytes);

        long remaining = dataLength / 2;
        int availableBytes;
        if ((dataLength & 1) == 0) {
          availableBytes = 0;
        } else {
          long l = pseudoRandomGenerator.nextLong();
          for (int i = 0; i < hashStreams.size(); ++i) {
            hashStreams.get(i).putByte((byte) l);
          }
          setLong(data, 0, l >>> 8);
          availableBytes = 7;
        }
        while (remaining > 0) {
          int increment = (int) Math.min(remaining, random.nextLong(maxIncrement + 1));
          while (availableBytes < (increment << 1)) {
            setLong(data, availableBytes, pseudoRandomGenerator.nextLong());
            availableBytes += 8;
          }
          for (int i = 0; i < hashStreams.size(); ++i) {
            hashStreams.get(i).putChars(asCharSequence(data, 0, increment));
          }
          setLong(data, 0, getLong(data, increment << 1));
          availableBytes -= (increment << 1);
          remaining -= increment;
        }
        getHashBytes(hashStreams, hashBytes);
        md.update(hashBytes);
      }
      String checksum = byteArrayToHexString(md.digest());
      assertThat(checksum)
          .describedAs(() -> checksumRecord.toString())
          .isEqualTo(checksumRecord.getChecksum());
    }
  }

  private static Hasher32 getHasher64UsingDefaultImplementations(Hasher32 referenceHasher) {
    return new AbstractHasher32() {

      @Override
      public HashStream32 hashStream() {

        return new AbstractHashStream32() {

          private final HashStream32 referenceHashStream = referenceHasher.hashStream();

          @Override
          public int getAsInt() {
            return referenceHashStream.getAsInt();
          }

          @Override
          public HashStream32 putByte(byte v) {
            return referenceHashStream.putByte(v);
          }

          @Override
          public HashStream32 reset() {
            return referenceHashStream.reset();
          }

          @Override
          public HashStream32 copy() {
            return referenceHashStream.copy();
          }
        };
      }

      @Override
      public int hashBytesToInt(byte[] input, int off, int len) {
        return referenceHasher.hashBytesToInt(input, off, len);
      }

      @Override
      public int hashCharsToInt(CharSequence input) {
        return referenceHasher.hashCharsToInt(input);
      }
    };
  }

  private static Hasher64 getHasher64UsingDefaultImplementations(Hasher64 referenceHasher) {
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
        getHasher64UsingDefaultImplementations(hasher64);
    int numCycles = 30;
    SplittableRandom random = new SplittableRandom(0x983c79631cff1b49L);
    byte[] data = new byte[16];
    for (int i = 0; i < numCycles; ++i) {
      random.nextBytes(data);
      long v1 = getLong(data, 0);
      long v2 = getLong(data, 8);
      assertThat(hasher64.hashLongLongToLong(v1, v2))
          .isEqualTo(hasher64.hashStream().putLong(v1).putLong(v2).getAsLong())
          .isEqualTo(hasher64.hashBytesToLong(data))
          .isEqualTo(hasherUsingDefaultImplementation.hashLongLongToLong(v1, v2));
    }
  }

  @ParameterizedTest
  @MethodSource("getHashers")
  void testHashLongLongLongToLong(Hasher hasher) {
    if (!(hasher instanceof Hasher64)) return;
    final Hasher64 hasher64 = (Hasher64) hasher;
    final Hasher64 hasherUsingDefaultImplementation =
        getHasher64UsingDefaultImplementations(hasher64);
    int numCycles = 30;
    SplittableRandom random = new SplittableRandom(0xcbc1a1e7856cc27eL);
    byte[] data = new byte[24];
    for (int i = 0; i < numCycles; ++i) {
      random.nextBytes(data);
      long v1 = getLong(data, 0);
      long v2 = getLong(data, 8);
      long v3 = getLong(data, 16);
      assertThat(hasher64.hashLongLongLongToLong(v1, v2, v3))
          .isEqualTo(hasher64.hashStream().putLong(v1).putLong(v2).putLong(v3).getAsLong())
          .isEqualTo(hasher64.hashBytesToLong(data))
          .isEqualTo(hasherUsingDefaultImplementation.hashLongLongLongToLong(v1, v2, v3));
    }
  }

  @ParameterizedTest
  @MethodSource("getHashers")
  void testHashLongIntToLong(Hasher hasher) {
    if (!(hasher instanceof Hasher64)) return;
    final Hasher64 hasher64 = (Hasher64) hasher;
    final Hasher64 hasherUsingDefaultImplementation =
        getHasher64UsingDefaultImplementations(hasher64);
    int numCycles = 30;
    SplittableRandom random = new SplittableRandom(0xc96c7abc2271f116L);
    byte[] data = new byte[12];
    for (int i = 0; i < numCycles; ++i) {
      random.nextBytes(data);
      long v1 = getLong(data, 0);
      int v2 = getInt(data, 8);
      assertThat(hasher64.hashLongIntToLong(v1, v2))
          .isEqualTo(hasher64.hashStream().putLong(v1).putInt(v2).getAsLong())
          .isEqualTo(hasher64.hashBytesToLong(data))
          .isEqualTo(hasherUsingDefaultImplementation.hashLongIntToLong(v1, v2));
    }
  }

  @ParameterizedTest
  @MethodSource("getHashers")
  void testHashIntLongToLong(Hasher hasher) {
    if (!(hasher instanceof Hasher64)) return;
    final Hasher64 hasher64 = (Hasher64) hasher;
    final Hasher64 hasherUsingDefaultImplementation =
        getHasher64UsingDefaultImplementations(hasher64);
    int numCycles = 30;
    SplittableRandom random = new SplittableRandom(0xd8bcfbc67ca54c67L);
    byte[] data = new byte[12];
    for (int i = 0; i < numCycles; ++i) {
      random.nextBytes(data);
      int v1 = getInt(data, 0);
      long v2 = getLong(data, 4);
      assertThat(hasher64.hashIntLongToLong(v1, v2))
          .isEqualTo(hasher64.hashStream().putInt(v1).putLong(v2).getAsLong())
          .isEqualTo(hasher64.hashBytesToLong(data))
          .isEqualTo(hasherUsingDefaultImplementation.hashIntLongToLong(v1, v2));
    }
  }

  @ParameterizedTest
  @MethodSource("getHashers")
  void testHashIntIntIntToLong(Hasher hasher) {
    if (!(hasher instanceof Hasher64)) return;
    final Hasher64 hasher64 = (Hasher64) hasher;
    final Hasher64 hasherUsingDefaultImplementation =
        getHasher64UsingDefaultImplementations(hasher64);
    int numCycles = 30;
    SplittableRandom random = new SplittableRandom(0x95ecb09b9622c9efL);
    byte[] data = new byte[12];
    for (int i = 0; i < numCycles; ++i) {
      random.nextBytes(data);
      int v1 = getInt(data, 0);
      int v2 = getInt(data, 4);
      int v3 = getInt(data, 8);
      assertThat(hasher64.hashIntIntIntToLong(v1, v2, v3))
          .isEqualTo(hasher64.hashStream().putInt(v1).putInt(v2).putInt(v3).getAsLong())
          .isEqualTo(hasher64.hashBytesToLong(data))
          .isEqualTo(hasherUsingDefaultImplementation.hashIntIntIntToLong(v1, v2, v3));
    }
  }

  @ParameterizedTest
  @MethodSource("getHashers")
  void testHashLongLongToInt(Hasher hasher) {
    if (!(hasher instanceof Hasher32)) return;
    final Hasher32 hasher32 = (Hasher32) hasher;
    final Hasher32 hasherUsingDefaultImplementation =
        getHasher64UsingDefaultImplementations(hasher32);
    int numCycles = 30;
    SplittableRandom random = new SplittableRandom(0x53253d7cb8c96234L);
    byte[] data = new byte[16];
    for (int i = 0; i < numCycles; ++i) {
      random.nextBytes(data);
      long v1 = getLong(data, 0);
      long v2 = getLong(data, 8);
      assertThat(hasher32.hashLongLongToInt(v1, v2))
          .isEqualTo(hasher32.hashStream().putLong(v1).putLong(v2).getAsInt())
          .isEqualTo(hasher32.hashBytesToInt(data))
          .isEqualTo(hasherUsingDefaultImplementation.hashLongLongToInt(v1, v2));
    }
  }

  @ParameterizedTest
  @MethodSource("getHashers")
  void testHashLongLongLongToInt(Hasher hasher) {
    if (!(hasher instanceof Hasher32)) return;
    final Hasher32 hasher32 = (Hasher32) hasher;
    final Hasher32 hasherUsingDefaultImplementation =
        getHasher64UsingDefaultImplementations(hasher32);
    int numCycles = 30;
    SplittableRandom random = new SplittableRandom(0xd10b9582e4d02105L);
    byte[] data = new byte[24];
    for (int i = 0; i < numCycles; ++i) {
      random.nextBytes(data);
      long v1 = getLong(data, 0);
      long v2 = getLong(data, 8);
      long v3 = getLong(data, 16);
      assertThat(hasher32.hashLongLongLongToInt(v1, v2, v3))
          .isEqualTo(hasher32.hashStream().putLong(v1).putLong(v2).putLong(v3).getAsInt())
          .isEqualTo(hasher32.hashBytesToInt(data))
          .isEqualTo(hasherUsingDefaultImplementation.hashLongLongLongToInt(v1, v2, v3));
    }
  }

  @ParameterizedTest
  @MethodSource("getHashers")
  void testHashLongIntToInt(Hasher hasher) {
    if (!(hasher instanceof Hasher32)) return;
    final Hasher32 hasher32 = (Hasher32) hasher;
    final Hasher32 hasherUsingDefaultImplementation =
        getHasher64UsingDefaultImplementations(hasher32);
    int numCycles = 30;
    SplittableRandom random = new SplittableRandom(0x05624bb5d1bec733L);
    byte[] data = new byte[12];
    for (int i = 0; i < numCycles; ++i) {
      random.nextBytes(data);
      long v1 = getLong(data, 0);
      int v2 = getInt(data, 8);
      assertThat(hasher32.hashLongIntToInt(v1, v2))
          .isEqualTo(hasher32.hashStream().putLong(v1).putInt(v2).getAsInt())
          .isEqualTo(hasher32.hashBytesToInt(data))
          .isEqualTo(hasherUsingDefaultImplementation.hashLongIntToInt(v1, v2));
    }
  }

  @ParameterizedTest
  @MethodSource("getHashers")
  void testHashIntLongToInt(Hasher hasher) {
    if (!(hasher instanceof Hasher32)) return;
    final Hasher32 hasher32 = (Hasher32) hasher;
    final Hasher32 hasherUsingDefaultImplementation =
        getHasher64UsingDefaultImplementations(hasher32);
    int numCycles = 30;
    SplittableRandom random = new SplittableRandom(0xad336777bf507094L);
    byte[] data = new byte[12];
    for (int i = 0; i < numCycles; ++i) {
      random.nextBytes(data);
      int v1 = getInt(data, 0);
      long v2 = getLong(data, 4);
      assertThat(hasher32.hashIntLongToInt(v1, v2))
          .isEqualTo(hasher32.hashStream().putInt(v1).putLong(v2).getAsInt())
          .isEqualTo(hasher32.hashBytesToInt(data))
          .isEqualTo(hasherUsingDefaultImplementation.hashIntLongToInt(v1, v2));
    }
  }

  @ParameterizedTest
  @MethodSource("getHashers")
  void testHashIntIntIntToInt(Hasher hasher) {
    if (!(hasher instanceof Hasher32)) return;
    final Hasher32 hasher32 = (Hasher32) hasher;
    final Hasher32 hasherUsingDefaultImplementation =
        getHasher64UsingDefaultImplementations(hasher32);
    int numCycles = 30;
    SplittableRandom random = new SplittableRandom(0x17bbe71e06c336e1L);
    byte[] data = new byte[12];
    for (int i = 0; i < numCycles; ++i) {
      random.nextBytes(data);
      int v1 = getInt(data, 0);
      int v2 = getInt(data, 4);
      int v3 = getInt(data, 8);
      assertThat(hasher32.hashIntIntIntToInt(v1, v2, v3))
          .isEqualTo(hasher32.hashStream().putInt(v1).putInt(v2).putInt(v3).getAsInt())
          .isEqualTo(hasher32.hashBytesToInt(data))
          .isEqualTo(hasherUsingDefaultImplementation.hashIntIntIntToInt(v1, v2, v3));
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
      assertHashStreamEquals(expected, actual, () -> "");
    }
  }

  private static void assertHashStream(
      HashStream hashStream, Hasher hasher, byte[] data, int off, int len) {
    Supplier<String> description = () -> "input length = " + len + " bytes";
    if (hasher instanceof Hasher128) {
      assertThat(((HashStream128) hashStream).get())
          .describedAs(description)
          .isEqualTo(((Hasher128) hasher).hashBytesTo128Bits(data, off, len));
    } else if (hasher instanceof Hasher64) {
      assertThat(((HashStream64) hashStream).getAsLong())
          .describedAs(description)
          .isEqualTo(((Hasher64) hasher).hashBytesToLong(data, off, len));
    } else if (hasher instanceof Hasher32) {
      assertThat(((HashStream32) hashStream).getAsInt())
          .describedAs(description)
          .isEqualTo(((Hasher32) hasher).hashBytesToInt(data, off, len));
    } else {
      fail();
    }
  }

  @Disabled
  @ParameterizedTest
  @MethodSource("getHashers")
  void testHashChars(Hasher hasher) {
    final int numIterations = 5;
    final int maxChars = (2 * getBlockLengthInBytes()) / 2 + 1;
    SplittableRandom random = new SplittableRandom(0x669a16245ed1f438L);
    byte[] data = new byte[2 * maxChars];
    for (int i = 0; i < numIterations; ++i) {
      random.nextBytes(data);
      for (int numChars = 0; numChars <= maxChars; ++numChars) {
        CharSequence charSequence = asCharSequence(data, 0, numChars);
        if (hasher instanceof Hasher128) {
          Hasher128 hasher128 = (Hasher128) hasher;
          assertThat(hasher128.hashBytesTo128Bits(data, 0, 2 * numChars))
              .isEqualTo(hasher128.hashCharsTo128Bits(charSequence));
        } else if (hasher instanceof Hasher64) {
          Hasher64 hasher64 = (Hasher64) hasher;
          assertThat(hasher64.hashBytesToLong(data, 0, 2 * numChars))
              .isEqualTo(hasher64.hashCharsToLong(charSequence));
        } else if (hasher instanceof Hasher32) {
          Hasher32 hasher32 = (Hasher32) hasher;
          assertThat(hasher32.hashBytesToInt(data, 0, 2 * numChars))
              .isEqualTo(hasher32.hashCharsToInt(charSequence));
        } else {
          fail();
        }
      }
    }
  }

  private static byte[] generateRandomBytes(SplittableRandom random, int maxLen) {
    byte[] data = new byte[random.nextInt(maxLen + 1)];
    random.nextBytes(data);
    return data;
  }

  @ParameterizedTest
  @MethodSource("getHashers")
  void testHashStreamResetAndHash(Hasher hasher) {

    SplittableRandom random = new SplittableRandom(0x58529d9ddfe2ce36L);

    HashStream hashStream = hasher.hashStream();

    int maxDataLen = 200;

    for (int i = 0; i < 20; ++i) {
      if (hasher instanceof Hasher32) {
        byte[] data = generateRandomBytes(random, maxDataLen);
        Hasher32 hasher32 = (Hasher32) hasher;
        assertThat(
                ((HashStream32) hashStream)
                    .resetAndHashToInt(data, (obj, sink) -> sink.putBytes(obj)))
            .isEqualTo(hasher32.hashBytesToInt(data));
      }
      if (hasher instanceof Hasher64) {
        byte[] data = generateRandomBytes(random, maxDataLen);
        Hasher64 hasher64 = (Hasher64) hasher;
        assertThat(
                ((HashStream64) hashStream)
                    .resetAndHashToLong(data, (obj, sink) -> sink.putBytes(obj)))
            .isEqualTo(hasher64.hashBytesToLong(data));
      }
      if (hasher instanceof Hasher128) {
        byte[] data = generateRandomBytes(random, maxDataLen);
        Hasher128 hasher128 = (Hasher128) hasher;
        assertThat(
                ((HashStream128) hashStream)
                    .resetAndHashTo128Bits(data, (obj, sink) -> sink.putBytes(obj)))
            .isEqualTo(hasher128.hashBytesTo128Bits(data));
      }
    }
  }
}
