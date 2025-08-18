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

import static com.dynatrace.hash4j.internal.ByteArrayUtil.getChar;
import static com.dynatrace.hash4j.internal.ByteArrayUtil.setLong;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import com.dynatrace.hash4j.internal.ByteArrayUtil;
import com.google.common.base.MoreObjects;
import java.util.function.Supplier;

final class HashTestUtils {

  private HashTestUtils() {}

  static byte[] getHashAsByteArray(HashStream hashStream) {
    byte[] result = new byte[hashStream.getHashBitSize() / 8];
    if (hashStream.getHashBitSize() == 32) {
      return HashValues.toByteArray(((HashStream32) hashStream).getAsInt());
    } else if (hashStream.getHashBitSize() == 64) {
      return HashValues.toByteArray(((HashStream64) hashStream).getAsLong());
    } else if (hashStream.getHashBitSize() == 128) {
      return HashValues.toByteArray(((HashStream128) hashStream).get());
    } else {
      fail("hash stream has unexpected bit size");
    }
    return result;
  }

  static void assertHashStreamEquals(
      HashStream hashStreamExpected,
      HashStream hashStreamActual,
      Supplier<String> descriptionSupplier) {
    assertThat(hashStreamActual.getHashBitSize()).isEqualTo(hashStreamExpected.getHashBitSize());
    assertThat(hashStreamActual).isEqualTo(hashStreamExpected);
    int hashBitSize = hashStreamExpected.getHashBitSize();
    if (hashBitSize == 128) {
      assertThat(((HashStream128) hashStreamActual).get())
          .describedAs(descriptionSupplier)
          .isEqualTo(((HashStream128) hashStreamExpected).get());
    } else if (hashBitSize == 64) {
      assertThat(((HashStream64) hashStreamActual).getAsLong())
          .describedAs(descriptionSupplier)
          .isEqualTo(((HashStream64) hashStreamExpected).getAsLong());
    } else if (hashBitSize == 32) {
      assertThat(((HashStream32) hashStreamActual).getAsInt())
          .describedAs(descriptionSupplier)
          .isEqualTo(((HashStream32) hashStreamExpected).getAsInt());
    } else {
      fail();
    }
  }

  static CharSequence asCharSequence(byte[] data, int off, int len) {
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

  static void generateRandomBytes(byte[] data, SplitMix64 pseudoRandomGenerator) {
    int i = 0;
    for (; i <= data.length - 8; i += 8) {
      setLong(data, i, pseudoRandomGenerator.nextLong());
    }
    if (i < data.length) {
      long l = pseudoRandomGenerator.nextLong();
      do {
        data[i] = (byte) (l >>> (8 * i));
        i += 1;
      } while (i < data.length);
    }
  }

  static final class SplitMix64 {

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

  static final class RandomOnDemandCharSequence implements CharSequence {

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
      return ByteArrayUtil.getChar(buffer, (index & (NUM_CHARS_IN_BUFFER - 1)) << 1);
    }

    @Override
    public CharSequence subSequence(int start, int end) {
      throw new UnsupportedOperationException();
    }
  }

  static final class RandomOnDemandByteAccess implements ByteAccess<Object> {

    private static final int NUM_BYTES_IN_BUFFER_EXPONENT = 12;
    private static final int NUM_BYTES_IN_BUFFER = 1 << NUM_BYTES_IN_BUFFER_EXPONENT;

    private final byte[] buffer = new byte[NUM_BYTES_IN_BUFFER];
    private SplitMix64 pseudoRandomGenerator;
    private long randomResetState;
    private long maxByteIdx;

    public void reset(SplitMix64 pseudoRandomGenerator) {
      this.pseudoRandomGenerator = pseudoRandomGenerator;
      this.randomResetState = pseudoRandomGenerator.getState();
      this.maxByteIdx = 0;
    }

    private void ensure(long minIdxIncl, long maxIdxExcl) {
      if (minIdxIncl < maxByteIdx - NUM_BYTES_IN_BUFFER) {
        this.maxByteIdx = 0;
        this.pseudoRandomGenerator.reset(randomResetState);
      }
      while (maxIdxExcl > maxByteIdx) {
        setLong(
            buffer,
            ((int) maxByteIdx & (NUM_BYTES_IN_BUFFER - 1)),
            pseudoRandomGenerator.nextLong());
        maxByteIdx += 8;
      }
    }

    @Override
    public byte getByte(Object data, long idx) {
      ensure(idx, idx + 1);
      int bufferIdx = (int) (idx & (NUM_BYTES_IN_BUFFER - 1));
      return buffer[bufferIdx];
    }

    @Override
    public int getInt(Object data, long idx) {
      ensure(idx, idx + 4);
      int bufferIdx = (int) (idx & (NUM_BYTES_IN_BUFFER - 1));
      if (bufferIdx > NUM_BYTES_IN_BUFFER - 4) {
        int a = ByteArrayUtil.getInt(buffer, 0);
        int b = ByteArrayUtil.getInt(buffer, NUM_BYTES_IN_BUFFER - 4);
        return (a << -(bufferIdx << 3)) | (b >>> (bufferIdx << 3));
      }
      return ByteArrayUtil.getInt(buffer, bufferIdx);
    }

    @Override
    public long getLong(Object data, long idx) {
      ensure(idx, idx + 8);
      int bufferIdx = (int) (idx & (NUM_BYTES_IN_BUFFER - 1));
      if (bufferIdx > NUM_BYTES_IN_BUFFER - 8) {
        long a = ByteArrayUtil.getLong(buffer, 0);
        long b = ByteArrayUtil.getLong(buffer, NUM_BYTES_IN_BUFFER - 8);
        return (a << -(bufferIdx << 3)) | (b >>> (bufferIdx << 3));
      }
      return ByteArrayUtil.getLong(buffer, bufferIdx);
    }
  }

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
}
