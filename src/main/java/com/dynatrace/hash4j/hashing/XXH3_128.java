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
import static com.dynatrace.hash4j.internal.ByteArrayUtil.getInt;
import static com.dynatrace.hash4j.internal.ByteArrayUtil.getIntAsUnsignedLong;
import static com.dynatrace.hash4j.internal.ByteArrayUtil.getLong;
import static com.dynatrace.hash4j.internal.ByteArrayUtil.setLong;
import static com.dynatrace.hash4j.internal.UnsignedMultiplyUtil.unsignedMultiplyHigh;

final class XXH3_128 extends XXH3Base implements AbstractHasher128 {

  private final long secShiftFinalC0;
  private final long secShiftFinalC1;
  private final long secShiftFinalC2;
  private final long secShiftFinalC3;
  private final long secShiftFinalC4;
  private final long secShiftFinalC5;
  private final long secShiftFinalC6;
  private final long secShiftFinalC7;

  private final long bitflip11;
  private final long bitflip23;
  private final long bitflip45;
  private final long bitflip67;

  private final HashValue128 hash0;

  private final long seed;

  private XXH3_128(long seed) {
    super(seed, true);

    this.seed = seed;

    this.secShiftFinalC0 = secret[14] >>> 40 | secret[15] << 24;
    this.secShiftFinalC1 = secret[15] >>> 40 | secret[16] << 24;
    this.secShiftFinalC2 = secret[16] >>> 40 | secret[17] << 24;
    this.secShiftFinalC3 = secret[17] >>> 40 | secret[18] << 24;
    this.secShiftFinalC4 = secret[18] >>> 40 | secret[19] << 24;
    this.secShiftFinalC5 = secret[19] >>> 40 | secret[20] << 24;
    this.secShiftFinalC6 = secret[20] >>> 40 | secret[21] << 24;
    this.secShiftFinalC7 = secret[21] >>> 40 | secret[22] << 24;

    this.bitflip11 = ((SECRET_01 >>> 32) ^ (SECRET_01 & 0xFFFFFFFFL)) - seed;
    this.bitflip23 = (SECRET_02 ^ SECRET_03) + (seed ^ Long.reverseBytes(seed & 0xFFFFFFFFL));
    this.bitflip45 = (SECRET_04 ^ SECRET_05) - seed;
    this.bitflip67 = (SECRET_06 ^ SECRET_07) + seed;

    this.hash0 =
        new HashValue128(
            avalanche64(seed ^ SECRET_10 ^ SECRET_11), avalanche64(seed ^ SECRET_08 ^ SECRET_09));
  }

  private XXH3_128() {
    this(0);
  }

  private static final Hasher128 DEFAULT_HASHER_INSTANCE = new XXH3_128();

  public static Hasher128 create() {
    return DEFAULT_HASHER_INSTANCE;
  }

  public static Hasher128 create(long seed) {
    return new XXH3_128(seed);
  }

  @Override
  public HashStream128 hashStream() {
    return new HashStreamImpl();
  }

  private final class HashStreamImpl extends HashStreamImplBase implements AbstractHashStream128 {

    @Override
    public HashValue128 get() {
      if (byteCount >= 0 && byteCount <= BULK_SIZE) {
        return hashBytesTo128Bits(buffer, 0, (int) byteCount);
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

      long resultLo = byteCount * INIT_ACC_1;
      long resultHi = ~(byteCount * INIT_ACC_2);
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

        resultLo += mix(acc0Loc ^ secShiftFinalB0, acc1Loc ^ secShiftFinalB1);
        resultHi += mix(acc0Loc ^ secShiftFinalC0, acc1Loc ^ secShiftFinalC1);
        resultLo += mix(acc2Loc ^ secShiftFinalB2, acc3Loc ^ secShiftFinalB3);
        resultHi += mix(acc2Loc ^ secShiftFinalC2, acc3Loc ^ secShiftFinalC3);
        resultLo += mix(acc4Loc ^ secShiftFinalB4, acc5Loc ^ secShiftFinalB5);
        resultHi += mix(acc4Loc ^ secShiftFinalC4, acc5Loc ^ secShiftFinalC5);
        resultLo += mix(acc6Loc ^ secShiftFinalB6, acc7Loc ^ secShiftFinalB7);
        resultHi += mix(acc6Loc ^ secShiftFinalC6, acc7Loc ^ secShiftFinalC7);
      }
      return new HashValue128(avalanche3(resultHi), avalanche3(resultLo));
    }

    @Override
    public HashStream128 putByte(byte v) {
      putByteImpl(v);
      return this;
    }

    @Override
    public HashStream128 putShort(short v) {
      putShortImpl(v);
      return this;
    }

    @Override
    public HashStream128 putChar(char v) {
      putCharImpl(v);
      return this;
    }

    @Override
    public HashStream128 putInt(int v) {
      putIntImpl(v);
      return this;
    }

    @Override
    public HashStream128 putLong(long v) {
      putLongImpl(v);
      return this;
    }

    @Override
    public HashStream128 putBytes(byte[] b, int off, int len) {
      putBytesImpl(b, off, len);
      return this;
    }

    @Override
    public <T> HashStream128 putBytes(T b, long off, long len, ByteAccess<T> access) {
      putBytesImpl(b, off, len, access);
      return this;
    }

    @Override
    public HashStream128 putChars(CharSequence c) {
      putCharsImpl(c);
      return this;
    }

    @Override
    public HashStream128 reset() {
      resetImpl();
      return this;
    }

    @Override
    public Hasher128 getHasher() {
      return XXH3_128.this;
    }

    @Override
    public HashStream128 setState(byte[] state) {
      setStateImpl(state);
      return this;
    }
  }

  @Override
  public HashValue128 hashBytesTo128Bits(byte[] input, int off, int length) {
    if (length <= 16) {
      if (length > 8) {
        long hi = getLong(input, off + length - 8);
        long lo = getLong(input, off) ^ hi ^ bitflip45;
        hi ^= bitflip67;
        long m128Hi =
            unsignedMultiplyHigh(lo, INIT_ACC_1) + hi + (hi & 0xFFFFFFFFL) * (INIT_ACC_5 - 1);
        long m128Lo = (lo * INIT_ACC_1 + ((length - 1L) << 54)) ^ Long.reverseBytes(m128Hi);
        long low = avalanche3(m128Lo * INIT_ACC_2);
        long high = avalanche3(unsignedMultiplyHigh(m128Lo, INIT_ACC_2) + m128Hi * INIT_ACC_2);
        return new HashValue128(high, low);
      }
      if (length >= 4) {
        long lo = getInt(input, off) & 0xFFFFFFFFL;
        long hi = getInt(input, off + length - 4);
        long keyed = lo ^ (hi << 32) ^ bitflip23;
        long pl = INIT_ACC_1 + (length << 2);
        long low = keyed * pl;
        long high = unsignedMultiplyHigh(keyed, pl) + (low << 1);
        low ^= high >>> 3;
        low ^= low >>> 35;
        low *= 0x9FB21C651E98DF25L;
        low ^= low >>> 28;
        high = avalanche3(high);
        return new HashValue128(high, low);
      }
      if (length != 0) {
        int c1 = input[off] & 0xFF;
        int c2 = input[off + (length >> 1)];
        int c3 = input[off + length - 1] & 0xFF;
        int combinedl = (c1 << 16) | (c2 << 24) | c3 | (length << 8);
        int combinedh = Integer.rotateLeft(Integer.reverseBytes(combinedl), 13);
        long low = avalanche64((combinedl & 0xFFFFFFFFL) ^ bitflip00);
        long high = avalanche64((combinedh & 0xFFFFFFFFL) ^ bitflip11);
        return new HashValue128(high, low);
      }
      return hash0;
    }
    if (length <= 128) {
      long acc0 = length * INIT_ACC_1;
      long acc1 = 0;
      if (length > 32) {
        if (length > 64) {
          if (length > 96) {
            long b0 = getLong(input, off + 48);
            long b1 = getLong(input, off + 56);
            long b2 = getLong(input, off + length - 64);
            long b3 = getLong(input, off + length - 56);
            acc0 = (acc0 + mix(b0 ^ secret[12], b1 ^ secret[13])) ^ (b2 + b3);
            acc1 = (acc1 + mix(b2 ^ secret[14], b3 ^ secret[15])) ^ (b0 + b1);
          }
          long b0 = getLong(input, off + 32);
          long b1 = getLong(input, off + 40);
          long b2 = getLong(input, off + length - 48);
          long b3 = getLong(input, off + length - 40);
          acc0 = (acc0 + mix(b0 ^ secret[8], b1 ^ secret[9])) ^ (b2 + b3);
          acc1 = (acc1 + mix(b2 ^ secret[10], b3 ^ secret[11])) ^ (b0 + b1);
        }
        long b0 = getLong(input, off + 16);
        long b1 = getLong(input, off + 24);
        long b2 = getLong(input, off + length - 32);
        long b3 = getLong(input, off + length - 24);
        acc0 = (acc0 + mix(b0 ^ secret[4], b1 ^ secret[5])) ^ (b2 + b3);
        acc1 = (acc1 + mix(b2 ^ secret[6], b3 ^ secret[7])) ^ (b0 + b1);
      }
      long b0 = getLong(input, off);
      long b1 = getLong(input, off + 8);
      long b2 = getLong(input, off + length - 16);
      long b3 = getLong(input, off + length - 8);
      acc0 = (acc0 + mix(b0 ^ secret[0], b1 ^ secret[1])) ^ (b2 + b3);
      acc1 = (acc1 + mix(b2 ^ secret[2], b3 ^ secret[3])) ^ (b0 + b1);

      long low = avalanche3(acc0 + acc1);
      long high = -avalanche3(acc0 * INIT_ACC_1 + acc1 * INIT_ACC_4 + (length - seed) * INIT_ACC_2);
      return new HashValue128(high, low);
    }
    if (length <= 240) {
      final int nbRounds = length >> 5;
      long acc0 = length * INIT_ACC_1;
      long acc1 = 0;
      int i = 0;
      for (; i < 4; ++i) {
        long b0 = getLong(input, off + (i << 5));
        long b1 = getLong(input, off + (i << 5) + 8);
        long b2 = getLong(input, off + (i << 5) + 16);
        long b3 = getLong(input, off + (i << 5) + 24);
        acc0 = (acc0 + mix(b0 ^ secret[i << 2], b1 ^ secret[(i << 2) + 1])) ^ (b2 + b3);
        acc1 = (acc1 + mix(b2 ^ secret[(i << 2) + 2], b3 ^ secret[(i << 2) + 3])) ^ (b0 + b1);
      }
      acc0 = avalanche3(acc0);
      acc1 = avalanche3(acc1);

      for (; i < nbRounds; ++i) {
        long b0 = getLong(input, off + (i << 5));
        long b1 = getLong(input, off + (i << 5) + 8);
        long b2 = getLong(input, off + (i << 5) + 16);
        long b3 = getLong(input, off + (i << 5) + 24);
        acc0 = (acc0 + mix(b0 ^ secShift[(i << 2) - 16], b1 ^ secShift[(i << 2) - 15])) ^ (b2 + b3);
        acc1 = (acc1 + mix(b2 ^ secShift[(i << 2) - 14], b3 ^ secShift[(i << 2) - 13])) ^ (b0 + b1);
      }

      {
        long b0 = getLong(input, off + length - 16);
        long b1 = getLong(input, off + length - 8);
        long b2 = getLong(input, off + length - 32);
        long b3 = getLong(input, off + length - 24);
        acc0 = (acc0 + mix(b0 ^ secShift[12], b1 ^ secShift[13])) ^ (b2 + b3);
        acc1 = (acc1 + mix(b2 ^ secShift[14], b3 ^ secShift[15])) ^ (b0 + b1);
      }
      long low = avalanche3(acc0 + acc1);
      long high = -avalanche3(acc0 * INIT_ACC_1 + acc1 * INIT_ACC_4 + (length - seed) * INIT_ACC_2);
      return new HashValue128(high, low);
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

    long resultLo = length * INIT_ACC_1;
    long resultHi = ~(length * INIT_ACC_2);
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

      resultLo += mix(acc0 ^ secShiftFinalB0, acc1 ^ secShiftFinalB1);
      resultHi += mix(acc0 ^ secShiftFinalC0, acc1 ^ secShiftFinalC1);
      resultLo += mix(acc2 ^ secShiftFinalB2, acc3 ^ secShiftFinalB3);
      resultHi += mix(acc2 ^ secShiftFinalC2, acc3 ^ secShiftFinalC3);
      resultLo += mix(acc4 ^ secShiftFinalB4, acc5 ^ secShiftFinalB5);
      resultHi += mix(acc4 ^ secShiftFinalC4, acc5 ^ secShiftFinalC5);
      resultLo += mix(acc6 ^ secShiftFinalB6, acc7 ^ secShiftFinalB7);
      resultHi += mix(acc6 ^ secShiftFinalC6, acc7 ^ secShiftFinalC7);
    }
    return new HashValue128(avalanche3(resultHi), avalanche3(resultLo));
  }

  @Override
  public <T> HashValue128 hashBytesTo128Bits(T input, long off, long length, ByteAccess<T> access) {
    if (length <= 16) {
      if (length > 8) {
        long hi = access.getLong(input, off + length - 8);
        long lo = access.getLong(input, off) ^ hi ^ bitflip45;
        hi ^= bitflip67;
        long m128Hi =
            unsignedMultiplyHigh(lo, INIT_ACC_1) + hi + (hi & 0xFFFFFFFFL) * (INIT_ACC_5 - 1);
        long m128Lo = (lo * INIT_ACC_1 + ((length - 1L) << 54)) ^ Long.reverseBytes(m128Hi);
        long low = avalanche3(m128Lo * INIT_ACC_2);
        long high = avalanche3(unsignedMultiplyHigh(m128Lo, INIT_ACC_2) + m128Hi * INIT_ACC_2);
        return new HashValue128(high, low);
      }
      if (length >= 4) {
        long lo = access.getIntAsUnsignedLong(input, off);
        long hi = access.getInt(input, off + length - 4);
        long keyed = lo ^ (hi << 32) ^ bitflip23;
        long pl = INIT_ACC_1 + (length << 2);
        long low = keyed * pl;
        long high = unsignedMultiplyHigh(keyed, pl) + (low << 1);
        low ^= high >>> 3;
        low ^= low >>> 35;
        low *= 0x9FB21C651E98DF25L;
        low ^= low >>> 28;
        high = avalanche3(high);
        return new HashValue128(high, low);
      }
      if (length != 0) {
        int c1 = access.getByteAsUnsignedInt(input, off);
        int c2 = access.getByte(input, off + (length >> 1));
        int c3 = access.getByteAsUnsignedInt(input, off + length - 1);
        int combinedl = (c1 << 16) | (c2 << 24) | c3 | ((int) length << 8);
        int combinedh = Integer.rotateLeft(Integer.reverseBytes(combinedl), 13);
        long low = avalanche64((combinedl & 0xFFFFFFFFL) ^ bitflip00);
        long high = avalanche64((combinedh & 0xFFFFFFFFL) ^ bitflip11);
        return new HashValue128(high, low);
      }
      return hash0;
    }
    if (length <= 128) {
      long acc0 = length * INIT_ACC_1;
      long acc1 = 0;
      if (length > 32) {
        if (length > 64) {
          if (length > 96) {
            long b0 = access.getLong(input, off + 48);
            long b1 = access.getLong(input, off + 56);
            long b2 = access.getLong(input, off + length - 64);
            long b3 = access.getLong(input, off + length - 56);
            acc0 = (acc0 + mix(b0 ^ secret[12], b1 ^ secret[13])) ^ (b2 + b3);
            acc1 = (acc1 + mix(b2 ^ secret[14], b3 ^ secret[15])) ^ (b0 + b1);
          }
          long b0 = access.getLong(input, off + 32);
          long b1 = access.getLong(input, off + 40);
          long b2 = access.getLong(input, off + length - 48);
          long b3 = access.getLong(input, off + length - 40);
          acc0 = (acc0 + mix(b0 ^ secret[8], b1 ^ secret[9])) ^ (b2 + b3);
          acc1 = (acc1 + mix(b2 ^ secret[10], b3 ^ secret[11])) ^ (b0 + b1);
        }
        long b0 = access.getLong(input, off + 16);
        long b1 = access.getLong(input, off + 24);
        long b2 = access.getLong(input, off + length - 32);
        long b3 = access.getLong(input, off + length - 24);
        acc0 = (acc0 + mix(b0 ^ secret[4], b1 ^ secret[5])) ^ (b2 + b3);
        acc1 = (acc1 + mix(b2 ^ secret[6], b3 ^ secret[7])) ^ (b0 + b1);
      }
      long b0 = access.getLong(input, off);
      long b1 = access.getLong(input, off + 8);
      long b2 = access.getLong(input, off + length - 16);
      long b3 = access.getLong(input, off + length - 8);
      acc0 = (acc0 + mix(b0 ^ secret[0], b1 ^ secret[1])) ^ (b2 + b3);
      acc1 = (acc1 + mix(b2 ^ secret[2], b3 ^ secret[3])) ^ (b0 + b1);

      long low = avalanche3(acc0 + acc1);
      long high = -avalanche3(acc0 * INIT_ACC_1 + acc1 * INIT_ACC_4 + (length - seed) * INIT_ACC_2);
      return new HashValue128(high, low);
    }
    if (length <= 240) {
      final int nbRounds = (int) length >> 5;
      long acc0 = length * INIT_ACC_1;
      long acc1 = 0;
      int i = 0;
      for (; i < 4; ++i) {
        long b0 = access.getLong(input, off + (i << 5));
        long b1 = access.getLong(input, off + (i << 5) + 8);
        long b2 = access.getLong(input, off + (i << 5) + 16);
        long b3 = access.getLong(input, off + (i << 5) + 24);
        acc0 = (acc0 + mix(b0 ^ secret[i << 2], b1 ^ secret[(i << 2) + 1])) ^ (b2 + b3);
        acc1 = (acc1 + mix(b2 ^ secret[(i << 2) + 2], b3 ^ secret[(i << 2) + 3])) ^ (b0 + b1);
      }
      acc0 = avalanche3(acc0);
      acc1 = avalanche3(acc1);

      for (; i < nbRounds; ++i) {
        long b0 = access.getLong(input, off + (i << 5));
        long b1 = access.getLong(input, off + (i << 5) + 8);
        long b2 = access.getLong(input, off + (i << 5) + 16);
        long b3 = access.getLong(input, off + (i << 5) + 24);
        acc0 = (acc0 + mix(b0 ^ secShift[(i << 2) - 16], b1 ^ secShift[(i << 2) - 15])) ^ (b2 + b3);
        acc1 = (acc1 + mix(b2 ^ secShift[(i << 2) - 14], b3 ^ secShift[(i << 2) - 13])) ^ (b0 + b1);
      }

      {
        long b0 = access.getLong(input, off + length - 16);
        long b1 = access.getLong(input, off + length - 8);
        long b2 = access.getLong(input, off + length - 32);
        long b3 = access.getLong(input, off + length - 24);
        acc0 = (acc0 + mix(b0 ^ secShift[12], b1 ^ secShift[13])) ^ (b2 + b3);
        acc1 = (acc1 + mix(b2 ^ secShift[14], b3 ^ secShift[15])) ^ (b0 + b1);
      }
      long low = avalanche3(acc0 + acc1);
      long high = -avalanche3(acc0 * INIT_ACC_1 + acc1 * INIT_ACC_4 + (length - seed) * INIT_ACC_2);
      return new HashValue128(high, low);
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

    long resultLo = length * INIT_ACC_1;
    long resultHi = ~(length * INIT_ACC_2);
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

      resultLo += mix(acc0 ^ secShiftFinalB0, acc1 ^ secShiftFinalB1);
      resultHi += mix(acc0 ^ secShiftFinalC0, acc1 ^ secShiftFinalC1);
      resultLo += mix(acc2 ^ secShiftFinalB2, acc3 ^ secShiftFinalB3);
      resultHi += mix(acc2 ^ secShiftFinalC2, acc3 ^ secShiftFinalC3);
      resultLo += mix(acc4 ^ secShiftFinalB4, acc5 ^ secShiftFinalB5);
      resultHi += mix(acc4 ^ secShiftFinalC4, acc5 ^ secShiftFinalC5);
      resultLo += mix(acc6 ^ secShiftFinalB6, acc7 ^ secShiftFinalB7);
      resultHi += mix(acc6 ^ secShiftFinalC6, acc7 ^ secShiftFinalC7);
    }
    return new HashValue128(avalanche3(resultHi), avalanche3(resultLo));
  }

  @Override
  public HashValue128 hashCharsTo128Bits(CharSequence charSequence) {
    int len = charSequence.length();
    if (len <= 8) {
      if (len > 4) {

        long hi = getLong(charSequence, len - 4);
        long lo = getLong(charSequence, 0) ^ hi ^ bitflip45;
        hi ^= bitflip67;
        long m128Hi =
            unsignedMultiplyHigh(lo, INIT_ACC_1) + hi + (hi & 0xFFFFFFFFL) * (INIT_ACC_5 - 1);
        long m128Lo = (lo * INIT_ACC_1 + (((len << 1) - 1L) << 54)) ^ Long.reverseBytes(m128Hi);
        long low = avalanche3(m128Lo * INIT_ACC_2);
        long high = avalanche3(unsignedMultiplyHigh(m128Lo, INIT_ACC_2) + m128Hi * INIT_ACC_2);
        return new HashValue128(high, low);
      }
      if (len >= 2) {
        long lo = getIntAsUnsignedLong(charSequence, 0);
        long hi = getIntAsUnsignedLong(charSequence, len - 2);
        long keyed = lo ^ (hi << 32) ^ bitflip23;
        long pl = INIT_ACC_1 + (len << 3);
        long low = keyed * pl;
        long high = unsignedMultiplyHigh(keyed, pl) + (low << 1);
        low ^= high >>> 3;
        low ^= low >>> 35;
        low *= 0x9FB21C651E98DF25L;
        low ^= low >>> 28;
        high = avalanche3(high);
        return new HashValue128(high, low);
      }
      if (len != 0) {
        int c = charSequence.charAt(0);
        int combinedl = (c << 16) | (c >>> 8) | 0x200;
        int combinedh = Integer.rotateLeft(Integer.reverseBytes(combinedl), 13);
        long low = avalanche64((combinedl & 0xFFFFFFFFL) ^ bitflip00);
        long high = avalanche64((combinedh & 0xFFFFFFFFL) ^ bitflip11);
        return new HashValue128(high, low);
      }
      return hash0;
    }
    if (len <= 64) {
      long acc0 = len * (INIT_ACC_1 << 1);
      long acc1 = 0;
      if (len > 16) {
        if (len > 32) {
          if (len > 48) {
            long b0 = getLong(charSequence, 24);
            long b1 = getLong(charSequence, 28);
            long b2 = getLong(charSequence, len - 32);
            long b3 = getLong(charSequence, len - 28);
            acc0 = (acc0 + mix(b0 ^ secret[12], b1 ^ secret[13])) ^ (b2 + b3);
            acc1 = (acc1 + mix(b2 ^ secret[14], b3 ^ secret[15])) ^ (b0 + b1);
          }
          long b0 = getLong(charSequence, 16);
          long b1 = getLong(charSequence, 20);
          long b2 = getLong(charSequence, len - 24);
          long b3 = getLong(charSequence, len - 20);
          acc0 = (acc0 + mix(b0 ^ secret[8], b1 ^ secret[9])) ^ (b2 + b3);
          acc1 = (acc1 + mix(b2 ^ secret[10], b3 ^ secret[11])) ^ (b0 + b1);
        }
        long b0 = getLong(charSequence, 8);
        long b1 = getLong(charSequence, 12);
        long b2 = getLong(charSequence, len - 16);
        long b3 = getLong(charSequence, len - 12);
        acc0 = (acc0 + mix(b0 ^ secret[4], b1 ^ secret[5])) ^ (b2 + b3);
        acc1 = (acc1 + mix(b2 ^ secret[6], b3 ^ secret[7])) ^ (b0 + b1);
      }
      long b0 = getLong(charSequence, 0);
      long b1 = getLong(charSequence, 4);
      long b2 = getLong(charSequence, len - 8);
      long b3 = getLong(charSequence, len - 4);
      acc0 = (acc0 + mix(b0 ^ secret[0], b1 ^ secret[1])) ^ (b2 + b3);
      acc1 = (acc1 + mix(b2 ^ secret[2], b3 ^ secret[3])) ^ (b0 + b1);

      long low = avalanche3(acc0 + acc1);
      long high =
          -avalanche3(acc0 * INIT_ACC_1 + acc1 * INIT_ACC_4 + ((len << 1) - seed) * INIT_ACC_2);
      return new HashValue128(high, low);
    }
    if (len <= 120) {
      long acc0 = len * (INIT_ACC_1 << 1);
      long acc1 = 0;
      {
        long b0 = getLong(charSequence, 0);
        long b1 = getLong(charSequence, 4);
        long b2 = getLong(charSequence, 8);
        long b3 = getLong(charSequence, 12);
        acc0 = (acc0 + mix(b0 ^ secret[0], b1 ^ secret[1])) ^ (b2 + b3);
        acc1 = (acc1 + mix(b2 ^ secret[2], b3 ^ secret[3])) ^ (b0 + b1);
      }
      {
        long b0 = getLong(charSequence, 16);
        long b1 = getLong(charSequence, 20);
        long b2 = getLong(charSequence, 24);
        long b3 = getLong(charSequence, 28);
        acc0 = (acc0 + mix(b0 ^ secret[4], b1 ^ secret[5])) ^ (b2 + b3);
        acc1 = (acc1 + mix(b2 ^ secret[6], b3 ^ secret[7])) ^ (b0 + b1);
      }
      {
        long b0 = getLong(charSequence, 32);
        long b1 = getLong(charSequence, 36);
        long b2 = getLong(charSequence, 40);
        long b3 = getLong(charSequence, 44);
        acc0 = (acc0 + mix(b0 ^ secret[8], b1 ^ secret[9])) ^ (b2 + b3);
        acc1 = (acc1 + mix(b2 ^ secret[10], b3 ^ secret[11])) ^ (b0 + b1);
      }
      {
        long b0 = getLong(charSequence, 48);
        long b1 = getLong(charSequence, 52);
        long b2 = getLong(charSequence, 56);
        long b3 = getLong(charSequence, 60);
        acc0 = (acc0 + mix(b0 ^ secret[12], b1 ^ secret[13])) ^ (b2 + b3);
        acc1 = (acc1 + mix(b2 ^ secret[14], b3 ^ secret[15])) ^ (b0 + b1);
      }
      acc0 = avalanche3(acc0);
      acc1 = avalanche3(acc1);
      if (80 <= len) {
        long b0 = getLong(charSequence, 64);
        long b1 = getLong(charSequence, 68);
        long b2 = getLong(charSequence, 72);
        long b3 = getLong(charSequence, 76);
        acc0 = (acc0 + mix(b0 ^ secShift[0], b1 ^ secShift[1])) ^ (b2 + b3);
        acc1 = (acc1 + mix(b2 ^ secShift[2], b3 ^ secShift[3])) ^ (b0 + b1);
      }
      if (96 <= len) {
        long b0 = getLong(charSequence, 80);
        long b1 = getLong(charSequence, 84);
        long b2 = getLong(charSequence, 88);
        long b3 = getLong(charSequence, 92);
        acc0 = (acc0 + mix(b0 ^ secShift[4], b1 ^ secShift[5])) ^ (b2 + b3);
        acc1 = (acc1 + mix(b2 ^ secShift[6], b3 ^ secShift[7])) ^ (b0 + b1);
      }
      if (112 <= len) {
        long b0 = getLong(charSequence, 96);
        long b1 = getLong(charSequence, 100);
        long b2 = getLong(charSequence, 104);
        long b3 = getLong(charSequence, 108);
        acc0 = (acc0 + mix(b0 ^ secShift[8], b1 ^ secShift[9])) ^ (b2 + b3);
        acc1 = (acc1 + mix(b2 ^ secShift[10], b3 ^ secShift[11])) ^ (b0 + b1);
      }
      {
        long b0 = getLong(charSequence, len - 8);
        long b1 = getLong(charSequence, len - 4);
        long b2 = getLong(charSequence, len - 16);
        long b3 = getLong(charSequence, len - 12);
        acc0 = (acc0 + mix(b0 ^ secShift[12], b1 ^ secShift[13])) ^ (b2 + b3);
        acc1 = (acc1 + mix(b2 ^ secShift[14], b3 ^ secShift[15])) ^ (b0 + b1);
      }
      long low = avalanche3(acc0 + acc1);
      long high =
          -avalanche3(acc0 * INIT_ACC_1 + acc1 * INIT_ACC_4 + ((len << 1) - seed) * INIT_ACC_2);
      return new HashValue128(high, low);
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

    long resultLo = len * (INIT_ACC_1 << 1);
    long resultHi = ~(len * (INIT_ACC_2 << 1));
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

      resultLo += mix(acc0 ^ secShiftFinalB0, acc1 ^ secShiftFinalB1);
      resultHi += mix(acc0 ^ secShiftFinalC0, acc1 ^ secShiftFinalC1);
      resultLo += mix(acc2 ^ secShiftFinalB2, acc3 ^ secShiftFinalB3);
      resultHi += mix(acc2 ^ secShiftFinalC2, acc3 ^ secShiftFinalC3);
      resultLo += mix(acc4 ^ secShiftFinalB4, acc5 ^ secShiftFinalB5);
      resultHi += mix(acc4 ^ secShiftFinalC4, acc5 ^ secShiftFinalC5);
      resultLo += mix(acc6 ^ secShiftFinalB6, acc7 ^ secShiftFinalB7);
      resultHi += mix(acc6 ^ secShiftFinalC6, acc7 ^ secShiftFinalC7);
    }

    return new HashValue128(avalanche3(resultHi), avalanche3(resultLo));
  }

  @Override
  public long hashIntToLong(int v) {
    long keyed = (v & 0xFFFFFFFFL) ^ ((long) v << 32) ^ bitflip23;
    long pl = INIT_ACC_1 + 16;
    long low = keyed * pl;
    long high = unsignedMultiplyHigh(keyed, pl) + (low << 1);
    low ^= high >>> 3;
    low ^= low >>> 35;
    low *= 0x9FB21C651E98DF25L;
    low ^= low >>> 28;
    return low;
  }

  @Override
  public long hashLongToLong(long v) {
    long keyed = v ^ bitflip23;
    long pl = INIT_ACC_1 + 32;
    long low = keyed * pl;
    long high = unsignedMultiplyHigh(keyed, pl) + (low << 1);
    low ^= high >>> 3;
    low ^= low >>> 35;
    low *= 0x9FB21C651E98DF25L;
    low ^= low >>> 28;
    return low;
  }

  @Override
  public long hashLongLongToLong(long v1, long v2) {
    long lo = v1 ^ v2 ^ bitflip45;
    long hi = v2 ^ bitflip67;
    long m128Hi = unsignedMultiplyHigh(lo, INIT_ACC_1) + hi + (hi & 0xFFFFFFFFL) * (INIT_ACC_5 - 1);
    long m128Lo = (lo * INIT_ACC_1 + 0x3C0000000000000L) ^ Long.reverseBytes(m128Hi);
    return avalanche3(m128Lo * INIT_ACC_2);
  }

  @Override
  public long hashLongLongLongToLong(long v1, long v2, long v3) {
    @SuppressWarnings("ConstantOverflow")
    long acc0 = (24L * INIT_ACC_1 + mix(v1 ^ secret[0], v2 ^ secret[1])) ^ (v2 + v3);
    long acc1 = mix(v2 ^ secret[2], v3 ^ secret[3]) ^ (v1 + v2);
    return avalanche3(acc0 + acc1);
  }

  @Override
  protected long finish12Bytes(long a, long b) {
    long lo = b ^ bitflip45 ^ a;
    long hi = b ^ bitflip67;
    long m128Hi = unsignedMultiplyHigh(lo, INIT_ACC_1) + hi + (hi & 0xFFFFFFFFL) * (INIT_ACC_5 - 1);
    long m128Lo = (lo * INIT_ACC_1 + 0x2c0000000000000L) ^ Long.reverseBytes(m128Hi);
    return avalanche3(m128Lo * INIT_ACC_2);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (!(obj instanceof XXH3_128)) return false;
    XXH3_128 that = (XXH3_128) obj;
    return getSeed() == that.getSeed();
  }

  @Override
  public int hashCode() {
    return Long.hashCode(getSeed());
  }
}
