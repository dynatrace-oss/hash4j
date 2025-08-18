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

/*
 * This implementation was derived from
 *
 * https://github.com/OpenHFT/Zero-Allocation-Hashing/blob/zero-allocation-hashing-0.16/src/main/java/net/openhft/hashing/XXH3.java
 *
 * which was published under the license below:
 *
 * Copyright 2015 Higher Frequency Trading http://www.higherfrequencytrading.com
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

import static com.dynatrace.hash4j.hashing.HashUtil.mix;
import static com.dynatrace.hash4j.internal.ByteArrayUtil.*;

final class XXH3_64 extends XXH3Base {

  private final long bitflip12;
  private final long bitflip34;
  private final long bitflip56;

  private final long hash0;

  private XXH3_64(long seed) {
    super(seed, false);

    this.bitflip12 = (SECRET_01 ^ SECRET_02) - (seed ^ Long.reverseBytes(seed & 0xFFFFFFFFL));
    this.bitflip34 = (SECRET_03 ^ SECRET_04) + seed;
    this.bitflip56 = (SECRET_05 ^ SECRET_06) - seed;

    this.hash0 = avalanche64(seed ^ (SECRET_07 ^ SECRET_08));
  }

  private XXH3_64() {
    this(0);
  }

  private static final Hasher64 DEFAULT_HASHER_INSTANCE = new XXH3_64();

  public static Hasher64 create() {
    return DEFAULT_HASHER_INSTANCE;
  }

  public static Hasher64 create(long seed) {
    return new XXH3_64(seed);
  }

  @Override
  public HashStream64 hashStream() {
    return new HashStreamImpl();
  }

  private final class HashStreamImpl extends HashStreamImplBase implements AbstractHashStream64 {

    @Override
    public long getAsLong() {
      if (byteCount >= 0 && byteCount <= BULK_SIZE) {
        return hashBytesToLong(buffer, 0, (int) byteCount);
      }
      setLong(buffer, BULK_SIZE, getLong(buffer, 0));

      long acc0Loc = acc0;
      long acc1Loc = acc1;
      long acc2Loc = acc2;
      long acc3Loc = acc3;
      long acc4Loc = acc4;
      long acc5Loc = acc5;
      long acc6Loc = acc6;
      long acc7Loc = acc7;

      int end = (((int) byteCount - 1) & BULK_SIZE_MASK) - 64;
      for (int off = 0, s = (((int) byteCount - 1) >>> 6) & 12; off <= end; off += 64, s += 1) {

        long b0 = getLong(buffer, off + 8 * 0);
        long b1 = getLong(buffer, off + 8 * 1);
        long b2 = getLong(buffer, off + 8 * 2);
        long b3 = getLong(buffer, off + 8 * 3);
        long b4 = getLong(buffer, off + 8 * 4);
        long b5 = getLong(buffer, off + 8 * 5);
        long b6 = getLong(buffer, off + 8 * 6);
        long b7 = getLong(buffer, off + 8 * 7);

        acc0Loc += b1 + contrib(b0, secret[s + 0]);
        acc1Loc += b0 + contrib(b1, secret[s + 1]);
        acc2Loc += b3 + contrib(b2, secret[s + 2]);
        acc3Loc += b2 + contrib(b3, secret[s + 3]);
        acc4Loc += b5 + contrib(b4, secret[s + 4]);
        acc5Loc += b4 + contrib(b5, secret[s + 5]);
        acc6Loc += b7 + contrib(b6, secret[s + 6]);
        acc7Loc += b6 + contrib(b7, secret[s + 7]);
      }

      long result = byteCount * INIT_ACC_1;
      {
        long b0 = getLong(buffer, (offset - (64 - 8 * 0)) & BULK_SIZE_MASK);
        long b1 = getLong(buffer, (offset - (64 - 8 * 1)) & BULK_SIZE_MASK);
        long b2 = getLong(buffer, (offset - (64 - 8 * 2)) & BULK_SIZE_MASK);
        long b3 = getLong(buffer, (offset - (64 - 8 * 3)) & BULK_SIZE_MASK);
        long b4 = getLong(buffer, (offset - (64 - 8 * 4)) & BULK_SIZE_MASK);
        long b5 = getLong(buffer, (offset - (64 - 8 * 5)) & BULK_SIZE_MASK);
        long b6 = getLong(buffer, (offset - (64 - 8 * 6)) & BULK_SIZE_MASK);
        long b7 = getLong(buffer, (offset - (64 - 8 * 7)) & BULK_SIZE_MASK);

        acc0Loc += b1 + contrib(b0, secShiftFinalA0);
        acc1Loc += b0 + contrib(b1, secShiftFinalA1);
        acc2Loc += b3 + contrib(b2, secShiftFinalA2);
        acc3Loc += b2 + contrib(b3, secShiftFinalA3);
        acc4Loc += b5 + contrib(b4, secShiftFinalA4);
        acc5Loc += b4 + contrib(b5, secShiftFinalA5);
        acc6Loc += b7 + contrib(b6, secShiftFinalA6);
        acc7Loc += b6 + contrib(b7, secShiftFinalA7);

        result += mix(acc0Loc ^ secShiftFinalB0, acc1Loc ^ secShiftFinalB1);
        result += mix(acc2Loc ^ secShiftFinalB2, acc3Loc ^ secShiftFinalB3);
        result += mix(acc4Loc ^ secShiftFinalB4, acc5Loc ^ secShiftFinalB5);
        result += mix(acc6Loc ^ secShiftFinalB6, acc7Loc ^ secShiftFinalB7);
      }

      return avalanche3(result);
    }

    @Override
    public HashStream64 putByte(byte v) {
      putByteImpl(v);
      return this;
    }

    @Override
    public HashStream64 putShort(short v) {
      putShortImpl(v);
      return this;
    }

    @Override
    public HashStream64 putChar(char v) {
      putCharImpl(v);
      return this;
    }

    @Override
    public HashStream64 putInt(int v) {
      putIntImpl(v);
      return this;
    }

    @Override
    public HashStream64 putLong(long v) {
      putLongImpl(v);
      return this;
    }

    @Override
    public HashStream64 putBytes(byte[] b, int off, int len) {
      putBytesImpl(b, off, len);
      return this;
    }

    @Override
    public <T> HashStream64 putBytes(T b, long off, long len, ByteAccess<T> access) {
      putBytesImpl(b, off, len, access);
      return this;
    }

    @Override
    public HashStream64 putChars(CharSequence c) {
      putCharsImpl(c);
      return this;
    }

    @Override
    public HashStream64 reset() {
      resetImpl();
      return this;
    }

    @Override
    public Hasher64 getHasher() {
      return XXH3_64.this;
    }

    @Override
    public HashStream64 setState(byte[] state) {
      setStateImpl(state);
      return this;
    }
  }

  private static long rrmxmx(long h64, final long length) {
    h64 ^= Long.rotateLeft(h64, 49) ^ Long.rotateLeft(h64, 24);
    h64 *= 0x9FB21C651E98DF25L;
    h64 ^= (h64 >>> 35) + length;
    h64 *= 0x9FB21C651E98DF25L;
    return h64 ^ (h64 >>> 28);
  }

  private static long mix16B(
      final byte[] input, final int offIn, final long[] sec, final int offSec) {
    long lo = getLong(input, offIn);
    long hi = getLong(input, offIn + 8);
    return mix(lo ^ sec[offSec], hi ^ sec[offSec + 1]);
  }

  private static <T> long mix16B(
      final T input, final long offIn, final long[] sec, final int offSec, ByteAccess<T> access) {
    long lo = access.getLong(input, offIn);
    long hi = access.getLong(input, offIn + 8);
    return mix(lo ^ sec[offSec], hi ^ sec[offSec + 1]);
  }

  private static long mix16B(
      final CharSequence input, final int offIn, final long[] sec, final int offSec) {
    long lo = getLong(input, offIn);
    long hi = getLong(input, offIn + 4);
    return mix(lo ^ sec[offSec], hi ^ sec[offSec + 1]);
  }

  @Override
  public long hashBytesToLong(byte[] input, int off, int length) {
    if (length <= 16) {
      if (length > 8) {
        long lo = getLong(input, off) ^ bitflip34;
        long hi = getLong(input, off + length - 8) ^ bitflip56;
        long acc = length + Long.reverseBytes(lo) + hi + mix(lo, hi);
        return avalanche3(acc);
      }
      if (length >= 4) {
        long input1 = getInt(input, off);
        long input2 = getInt(input, off + length - 4) & 0xFFFFFFFFL;
        long keyed = input2 ^ (input1 << 32) ^ bitflip12;
        return rrmxmx(keyed, length);
      }
      if (length != 0) {
        int c1 = input[off] & 0xFF;
        int c2 = input[off + (length >> 1)];
        int c3 = input[off + length - 1] & 0xFF;
        long combined = ((c1 << 16) | (c2 << 24) | c3 | (length << 8)) & 0xFFFFFFFFL;
        return avalanche64(combined ^ bitflip00);
      }
      return hash0;
    }
    if (length <= 128) {
      long acc = length * INIT_ACC_1;

      if (length > 32) {
        if (length > 64) {
          if (length > 96) {
            acc += mix16B(input, off + 48, secret, 12);
            acc += mix16B(input, off + length - 64, secret, 14);
          }
          acc += mix16B(input, off + 32, secret, 8);
          acc += mix16B(input, off + length - 48, secret, 10);
        }
        acc += mix16B(input, off + 16, secret, 4);
        acc += mix16B(input, off + length - 32, secret, 6);
      }
      acc += mix16B(input, off, secret, 0);
      acc += mix16B(input, off + length - 16, secret, 2);

      return avalanche3(acc);
    }
    if (length <= 240) {
      long acc = length * INIT_ACC_1;
      final int nbRounds = length >>> 4;
      int i = 0;
      for (; i < 8; ++i) {
        acc += mix16B(input, off + (i << 4), secret, i << 1);
      }

      acc = avalanche3(acc);

      for (; i < nbRounds; ++i) {
        acc += mix16B(input, off + (i << 4), secShift, (i << 1) - 16);
      }

      acc += mix16B(input, off + length - 16, secShift, 14);
      return avalanche3(acc);
    }

    long acc0 = INIT_ACC_0;
    long acc1 = INIT_ACC_1;
    long acc2 = INIT_ACC_2;
    long acc3 = INIT_ACC_3;
    long acc4 = INIT_ACC_4;
    long acc5 = INIT_ACC_5;
    long acc6 = INIT_ACC_6;
    long acc7 = INIT_ACC_7;

    final int nbBlocks = (length - 1) >>> BLOCK_LEN_EXP;
    for (int n = 0; n < nbBlocks; n++) {
      final int offBlock = off + (n << BLOCK_LEN_EXP);
      for (int s = 0; s < 16; s += 1) {
        int offStripe = offBlock + (s << 6);

        long b0 = getLong(input, offStripe + 8 * 0);
        long b1 = getLong(input, offStripe + 8 * 1);
        long b2 = getLong(input, offStripe + 8 * 2);
        long b3 = getLong(input, offStripe + 8 * 3);
        long b4 = getLong(input, offStripe + 8 * 4);
        long b5 = getLong(input, offStripe + 8 * 5);
        long b6 = getLong(input, offStripe + 8 * 6);
        long b7 = getLong(input, offStripe + 8 * 7);

        acc0 += b1 + contrib(b0, secret[s + 0]);
        acc1 += b0 + contrib(b1, secret[s + 1]);
        acc2 += b3 + contrib(b2, secret[s + 2]);
        acc3 += b2 + contrib(b3, secret[s + 3]);
        acc4 += b5 + contrib(b4, secret[s + 4]);
        acc5 += b4 + contrib(b5, secret[s + 5]);
        acc6 += b7 + contrib(b6, secret[s + 6]);
        acc7 += b6 + contrib(b7, secret[s + 7]);
      }

      acc0 = mixAcc(acc0, secret[16]);
      acc1 = mixAcc(acc1, secret[17]);
      acc2 = mixAcc(acc2, secret[18]);
      acc3 = mixAcc(acc3, secret[19]);
      acc4 = mixAcc(acc4, secret[20]);
      acc5 = mixAcc(acc5, secret[21]);
      acc6 = mixAcc(acc6, secret[22]);
      acc7 = mixAcc(acc7, secret[23]);
    }

    final int nbStripes = ((length - 1) - (nbBlocks << BLOCK_LEN_EXP)) >>> 6;
    final int offBlock = off + (nbBlocks << BLOCK_LEN_EXP);
    for (int s = 0; s < nbStripes; s++) {
      int offStripe = offBlock + (s << 6);

      long b0 = getLong(input, offStripe + 8 * 0);
      long b1 = getLong(input, offStripe + 8 * 1);
      long b2 = getLong(input, offStripe + 8 * 2);
      long b3 = getLong(input, offStripe + 8 * 3);
      long b4 = getLong(input, offStripe + 8 * 4);
      long b5 = getLong(input, offStripe + 8 * 5);
      long b6 = getLong(input, offStripe + 8 * 6);
      long b7 = getLong(input, offStripe + 8 * 7);

      acc0 += b1 + contrib(b0, secret[s + 0]);
      acc1 += b0 + contrib(b1, secret[s + 1]);
      acc2 += b3 + contrib(b2, secret[s + 2]);
      acc3 += b2 + contrib(b3, secret[s + 3]);
      acc4 += b5 + contrib(b4, secret[s + 4]);
      acc5 += b4 + contrib(b5, secret[s + 5]);
      acc6 += b7 + contrib(b6, secret[s + 6]);
      acc7 += b6 + contrib(b7, secret[s + 7]);
    }

    long result = length * INIT_ACC_1;
    {
      int offStripe = off + length - 64;

      long b0 = getLong(input, offStripe + 8 * 0);
      long b1 = getLong(input, offStripe + 8 * 1);
      long b2 = getLong(input, offStripe + 8 * 2);
      long b3 = getLong(input, offStripe + 8 * 3);
      long b4 = getLong(input, offStripe + 8 * 4);
      long b5 = getLong(input, offStripe + 8 * 5);
      long b6 = getLong(input, offStripe + 8 * 6);
      long b7 = getLong(input, offStripe + 8 * 7);

      acc0 += b1 + contrib(b0, secShiftFinalA0);
      acc1 += b0 + contrib(b1, secShiftFinalA1);
      acc2 += b3 + contrib(b2, secShiftFinalA2);
      acc3 += b2 + contrib(b3, secShiftFinalA3);
      acc4 += b5 + contrib(b4, secShiftFinalA4);
      acc5 += b4 + contrib(b5, secShiftFinalA5);
      acc6 += b7 + contrib(b6, secShiftFinalA6);
      acc7 += b6 + contrib(b7, secShiftFinalA7);

      result += mix(acc0 ^ secShiftFinalB0, acc1 ^ secShiftFinalB1);
      result += mix(acc2 ^ secShiftFinalB2, acc3 ^ secShiftFinalB3);
      result += mix(acc4 ^ secShiftFinalB4, acc5 ^ secShiftFinalB5);
      result += mix(acc6 ^ secShiftFinalB6, acc7 ^ secShiftFinalB7);
    }

    return avalanche3(result);
  }

  @Override
  public <T> long hashBytesToLong(T input, long off, long length, ByteAccess<T> access) {
    if (length <= 16) {
      if (length > 8) {
        long lo = access.getLong(input, off) ^ bitflip34;
        long hi = access.getLong(input, off + length - 8) ^ bitflip56;
        long acc = length + Long.reverseBytes(lo) + hi + mix(lo, hi);
        return avalanche3(acc);
      }
      if (length >= 4) {
        long input1 = access.getInt(input, off);
        long input2 = access.getIntAsUnsignedLong(input, off + length - 4);
        long keyed = input2 ^ (input1 << 32) ^ bitflip12;
        return rrmxmx(keyed, length);
      }
      if (length != 0) {
        int c1 = access.getByteAsUnsignedInt(input, off);
        int c2 = access.getByte(input, off + (length >> 1));
        int c3 = access.getByteAsUnsignedInt(input, off + length - 1);
        long combined = ((c1 << 16) | (c2 << 24) | c3 | (length << 8)) & 0xFFFFFFFFL;
        return avalanche64(combined ^ bitflip00);
      }
      return hash0;
    }
    if (length <= 128) {
      long acc = length * INIT_ACC_1;

      if (length > 32) {
        if (length > 64) {
          if (length > 96) {
            acc += mix16B(input, off + 48, secret, 12, access);
            acc += mix16B(input, off + length - 64, secret, 14, access);
          }
          acc += mix16B(input, off + 32, secret, 8, access);
          acc += mix16B(input, off + length - 48, secret, 10, access);
        }
        acc += mix16B(input, off + 16, secret, 4, access);
        acc += mix16B(input, off + length - 32, secret, 6, access);
      }
      acc += mix16B(input, off, secret, 0, access);
      acc += mix16B(input, off + length - 16, secret, 2, access);

      return avalanche3(acc);
    }
    if (length <= 240) {
      long acc = length * INIT_ACC_1;
      final int nbRounds = (int) length >>> 4;
      int i = 0;
      for (; i < 8; ++i) {
        acc += mix16B(input, off + (i << 4), secret, i << 1, access);
      }

      acc = avalanche3(acc);

      for (; i < nbRounds; ++i) {
        acc += mix16B(input, off + (i << 4), secShift, (i << 1) - 16, access);
      }

      acc += mix16B(input, off + length - 16, secShift, 14, access);
      return avalanche3(acc);
    }

    long acc0 = INIT_ACC_0;
    long acc1 = INIT_ACC_1;
    long acc2 = INIT_ACC_2;
    long acc3 = INIT_ACC_3;
    long acc4 = INIT_ACC_4;
    long acc5 = INIT_ACC_5;
    long acc6 = INIT_ACC_6;
    long acc7 = INIT_ACC_7;

    final long nbBlocks = (length - 1) >>> BLOCK_LEN_EXP;
    for (long n = 0; n < nbBlocks; n++) {
      final long offBlock = off + (n << BLOCK_LEN_EXP);
      for (int s = 0; s < 16; s += 1) {
        long offStripe = offBlock + (s << 6);

        long b0 = access.getLong(input, offStripe + 8 * 0);
        long b1 = access.getLong(input, offStripe + 8 * 1);
        long b2 = access.getLong(input, offStripe + 8 * 2);
        long b3 = access.getLong(input, offStripe + 8 * 3);
        long b4 = access.getLong(input, offStripe + 8 * 4);
        long b5 = access.getLong(input, offStripe + 8 * 5);
        long b6 = access.getLong(input, offStripe + 8 * 6);
        long b7 = access.getLong(input, offStripe + 8 * 7);

        acc0 += b1 + contrib(b0, secret[s + 0]);
        acc1 += b0 + contrib(b1, secret[s + 1]);
        acc2 += b3 + contrib(b2, secret[s + 2]);
        acc3 += b2 + contrib(b3, secret[s + 3]);
        acc4 += b5 + contrib(b4, secret[s + 4]);
        acc5 += b4 + contrib(b5, secret[s + 5]);
        acc6 += b7 + contrib(b6, secret[s + 6]);
        acc7 += b6 + contrib(b7, secret[s + 7]);
      }

      acc0 = mixAcc(acc0, secret[16]);
      acc1 = mixAcc(acc1, secret[17]);
      acc2 = mixAcc(acc2, secret[18]);
      acc3 = mixAcc(acc3, secret[19]);
      acc4 = mixAcc(acc4, secret[20]);
      acc5 = mixAcc(acc5, secret[21]);
      acc6 = mixAcc(acc6, secret[22]);
      acc7 = mixAcc(acc7, secret[23]);
    }

    final long nbStripes = ((length - 1) - (nbBlocks << BLOCK_LEN_EXP)) >>> 6;
    final long offBlock = off + (nbBlocks << BLOCK_LEN_EXP);
    for (int s = 0; s < nbStripes; s++) {
      long offStripe = offBlock + (s << 6);

      long b0 = access.getLong(input, offStripe + 8 * 0);
      long b1 = access.getLong(input, offStripe + 8 * 1);
      long b2 = access.getLong(input, offStripe + 8 * 2);
      long b3 = access.getLong(input, offStripe + 8 * 3);
      long b4 = access.getLong(input, offStripe + 8 * 4);
      long b5 = access.getLong(input, offStripe + 8 * 5);
      long b6 = access.getLong(input, offStripe + 8 * 6);
      long b7 = access.getLong(input, offStripe + 8 * 7);

      acc0 += b1 + contrib(b0, secret[s + 0]);
      acc1 += b0 + contrib(b1, secret[s + 1]);
      acc2 += b3 + contrib(b2, secret[s + 2]);
      acc3 += b2 + contrib(b3, secret[s + 3]);
      acc4 += b5 + contrib(b4, secret[s + 4]);
      acc5 += b4 + contrib(b5, secret[s + 5]);
      acc6 += b7 + contrib(b6, secret[s + 6]);
      acc7 += b6 + contrib(b7, secret[s + 7]);
    }

    long result = length * INIT_ACC_1;
    {
      long offStripe = off + length - 64;

      long b0 = access.getLong(input, offStripe + 8 * 0);
      long b1 = access.getLong(input, offStripe + 8 * 1);
      long b2 = access.getLong(input, offStripe + 8 * 2);
      long b3 = access.getLong(input, offStripe + 8 * 3);
      long b4 = access.getLong(input, offStripe + 8 * 4);
      long b5 = access.getLong(input, offStripe + 8 * 5);
      long b6 = access.getLong(input, offStripe + 8 * 6);
      long b7 = access.getLong(input, offStripe + 8 * 7);

      acc0 += b1 + contrib(b0, secShiftFinalA0);
      acc1 += b0 + contrib(b1, secShiftFinalA1);
      acc2 += b3 + contrib(b2, secShiftFinalA2);
      acc3 += b2 + contrib(b3, secShiftFinalA3);
      acc4 += b5 + contrib(b4, secShiftFinalA4);
      acc5 += b4 + contrib(b5, secShiftFinalA5);
      acc6 += b7 + contrib(b6, secShiftFinalA6);
      acc7 += b6 + contrib(b7, secShiftFinalA7);

      result += mix(acc0 ^ secShiftFinalB0, acc1 ^ secShiftFinalB1);
      result += mix(acc2 ^ secShiftFinalB2, acc3 ^ secShiftFinalB3);
      result += mix(acc4 ^ secShiftFinalB4, acc5 ^ secShiftFinalB5);
      result += mix(acc6 ^ secShiftFinalB6, acc7 ^ secShiftFinalB7);
    }

    return avalanche3(result);
  }

  @Override
  public long hashCharsToLong(CharSequence charSequence) {

    int len = charSequence.length();

    if (len <= 8) {
      if (len > 4) {
        long lo = getLong(charSequence, 0) ^ bitflip34;
        long hi = getLong(charSequence, len - 4) ^ bitflip56;
        long acc = (len << 1) + Long.reverseBytes(lo) + hi + mix(lo, hi);
        return avalanche3(acc);
      }
      if (len >= 2) {
        long input1 = getInt(charSequence, 0);
        long input2 = getInt(charSequence, len - 2);
        long keyed = (input2 & 0xFFFFFFFFL) ^ (input1 << 32) ^ bitflip12;
        return rrmxmx(keyed, len << 1);
      }
      if (len != 0) {
        long c = charSequence.charAt(0);
        return avalanche64((c << 16) ^ (c >>> 8) ^ 0x200L ^ bitflip00);
      }
      return hash0;
    }
    if (len <= 64) {
      long acc = len * (INIT_ACC_1 << 1);

      if (len > 16) {
        if (len > 32) {
          if (len > 48) {
            acc += mix16B(charSequence, 24, secret, 12);
            acc += mix16B(charSequence, len - 32, secret, 14);
          }
          acc += mix16B(charSequence, 16, secret, 8);
          acc += mix16B(charSequence, len - 24, secret, 10);
        }
        acc += mix16B(charSequence, 8, secret, 4);
        acc += mix16B(charSequence, len - 16, secret, 6);
      }
      acc += mix16B(charSequence, 0, secret, 0);
      acc += mix16B(charSequence, len - 8, secret, 2);

      return avalanche3(acc);
    }

    if (len <= 120) {
      long acc = len * (INIT_ACC_1 << 1);
      final int nbRounds = len >>> 3;
      int i = 0;
      for (; i < 8; ++i) {
        acc += mix16B(charSequence, i << 3, secret, i << 1);
      }

      acc = avalanche3(acc);

      for (; i < nbRounds; ++i) {
        acc += mix16B(charSequence, i << 3, secShift, (i - 8) << 1);
      }

      acc += mix16B(charSequence, len - 8, secShift, 14);

      return avalanche3(acc);
    }

    long acc0 = INIT_ACC_0;
    long acc1 = INIT_ACC_1;
    long acc2 = INIT_ACC_2;
    long acc3 = INIT_ACC_3;
    long acc4 = INIT_ACC_4;
    long acc5 = INIT_ACC_5;
    long acc6 = INIT_ACC_6;
    long acc7 = INIT_ACC_7;

    final int nbBlocks = (len - 1) >>> (BLOCK_LEN_EXP - 1);
    for (int n = 0; n < nbBlocks; n++) {
      final int offBlock = n << (BLOCK_LEN_EXP - 1);
      for (int s = 0; s < 16; s += 1) {
        int offStripe = offBlock + (s << 5);

        long b0 = getLong(charSequence, offStripe + 4 * 0);
        long b1 = getLong(charSequence, offStripe + 4 * 1);
        long b2 = getLong(charSequence, offStripe + 4 * 2);
        long b3 = getLong(charSequence, offStripe + 4 * 3);
        long b4 = getLong(charSequence, offStripe + 4 * 4);
        long b5 = getLong(charSequence, offStripe + 4 * 5);
        long b6 = getLong(charSequence, offStripe + 4 * 6);
        long b7 = getLong(charSequence, offStripe + 4 * 7);

        acc0 += b1 + contrib(b0, secret[s + 0]);
        acc1 += b0 + contrib(b1, secret[s + 1]);
        acc2 += b3 + contrib(b2, secret[s + 2]);
        acc3 += b2 + contrib(b3, secret[s + 3]);
        acc4 += b5 + contrib(b4, secret[s + 4]);
        acc5 += b4 + contrib(b5, secret[s + 5]);
        acc6 += b7 + contrib(b6, secret[s + 6]);
        acc7 += b6 + contrib(b7, secret[s + 7]);
      }

      acc0 = mixAcc(acc0, secret[16]);
      acc1 = mixAcc(acc1, secret[17]);
      acc2 = mixAcc(acc2, secret[18]);
      acc3 = mixAcc(acc3, secret[19]);
      acc4 = mixAcc(acc4, secret[20]);
      acc5 = mixAcc(acc5, secret[21]);
      acc6 = mixAcc(acc6, secret[22]);
      acc7 = mixAcc(acc7, secret[23]);
    }

    final int nbStripes = ((len - 1) - (nbBlocks << (BLOCK_LEN_EXP - 1))) >>> 5;
    final int offBlock = nbBlocks << (BLOCK_LEN_EXP - 1);
    for (int s = 0; s < nbStripes; s++) {
      int offStripe = offBlock + (s << 5);

      long b0 = getLong(charSequence, offStripe + 4 * 0);
      long b1 = getLong(charSequence, offStripe + 4 * 1);
      long b2 = getLong(charSequence, offStripe + 4 * 2);
      long b3 = getLong(charSequence, offStripe + 4 * 3);
      long b4 = getLong(charSequence, offStripe + 4 * 4);
      long b5 = getLong(charSequence, offStripe + 4 * 5);
      long b6 = getLong(charSequence, offStripe + 4 * 6);
      long b7 = getLong(charSequence, offStripe + 4 * 7);

      acc0 += b1 + contrib(b0, secret[s + 0]);
      acc1 += b0 + contrib(b1, secret[s + 1]);
      acc2 += b3 + contrib(b2, secret[s + 2]);
      acc3 += b2 + contrib(b3, secret[s + 3]);
      acc4 += b5 + contrib(b4, secret[s + 4]);
      acc5 += b4 + contrib(b5, secret[s + 5]);
      acc6 += b7 + contrib(b6, secret[s + 6]);
      acc7 += b6 + contrib(b7, secret[s + 7]);
    }

    long result = len * (INIT_ACC_1 << 1);
    {
      int offStripe = len - 32;

      long b0 = getLong(charSequence, offStripe + 4 * 0);
      long b1 = getLong(charSequence, offStripe + 4 * 1);
      long b2 = getLong(charSequence, offStripe + 4 * 2);
      long b3 = getLong(charSequence, offStripe + 4 * 3);
      long b4 = getLong(charSequence, offStripe + 4 * 4);
      long b5 = getLong(charSequence, offStripe + 4 * 5);
      long b6 = getLong(charSequence, offStripe + 4 * 6);
      long b7 = getLong(charSequence, offStripe + 4 * 7);

      acc0 += b1 + contrib(b0, secShiftFinalA0);
      acc1 += b0 + contrib(b1, secShiftFinalA1);
      acc2 += b3 + contrib(b2, secShiftFinalA2);
      acc3 += b2 + contrib(b3, secShiftFinalA3);
      acc4 += b5 + contrib(b4, secShiftFinalA4);
      acc5 += b4 + contrib(b5, secShiftFinalA5);
      acc6 += b7 + contrib(b6, secShiftFinalA6);
      acc7 += b6 + contrib(b7, secShiftFinalA7);

      result += mix(acc0 ^ secShiftFinalB0, acc1 ^ secShiftFinalB1);
      result += mix(acc2 ^ secShiftFinalB2, acc3 ^ secShiftFinalB3);
      result += mix(acc4 ^ secShiftFinalB4, acc5 ^ secShiftFinalB5);
      result += mix(acc6 ^ secShiftFinalB6, acc7 ^ secShiftFinalB7);
    }

    return avalanche3(result);
  }

  @Override
  public long hashIntIntToLong(int v1, int v2) {
    return rrmxmx(((long) v1 << 32) ^ (v2 & 0xFFFFFFFFL) ^ bitflip12, 8);
  }

  @Override
  public long hashIntToLong(int v) {
    return rrmxmx((v & 0xFFFFFFFFL) ^ ((long) v << 32) ^ bitflip12, 4);
  }

  @Override
  public long hashLongToLong(long v) {
    return rrmxmx((v << 32) ^ (v >>> 32) ^ bitflip12, 8);
  }

  @Override
  public long hashLongLongToLong(long v1, long v2) {
    long lo = v1 ^ bitflip34;
    long hi = v2 ^ bitflip56;
    long acc = 16 + Long.reverseBytes(lo) + hi + mix(lo, hi);
    return avalanche3(acc);
  }

  @Override
  public long hashLongLongLongToLong(long v1, long v2, long v3) {
    long acc = 0xd53368a48e1afca8L;
    acc += mix(v1 ^ secret[0], v2 ^ secret[1]);
    acc += mix(v2 ^ secret[2], v3 ^ secret[3]);
    return avalanche3(acc);
  }

  @Override
  protected long finish12Bytes(long a, long b) {
    long lo = a ^ bitflip34;
    long hi = b ^ bitflip56;
    long acc = 12 + Long.reverseBytes(lo) + hi + mix(lo, hi);
    return avalanche3(acc);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (!(obj instanceof XXH3_64)) return false;
    XXH3_64 that = (XXH3_64) obj;
    return getSeed() == that.getSeed();
  }

  @Override
  public int hashCode() {
    return Long.hashCode(getSeed());
  }
}
