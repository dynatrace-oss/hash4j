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

import static com.dynatrace.hash4j.hashing.AbstractHasher.*;
import static com.dynatrace.hash4j.hashing.UnsignedMultiplyUtil.unsignedMultiplyHigh;

class XXH3_128 extends XXH3Base implements AbstractHasher128 {

  private final long secShift12;
  private final long secShift13;
  private final long secShift14;
  private final long secShift15;

  private final long secShiftFinal8;
  private final long secShiftFinal9;
  private final long secShiftFinal10;
  private final long secShiftFinal11;
  private final long secShiftFinal12;
  private final long secShiftFinal13;
  private final long secShiftFinal14;
  private final long secShiftFinal15;

  private final long bitflip00;
  private final long bitflip11;
  private final long bitflip23;
  private final long bitflip45;
  private final long bitflip67;

  private final HashValue128 hash0;

  private final long seed;

  private XXH3_128(long seed) {
    super(seed);

    this.secShift12 = (SECRET_12 >>> 56) + (SECRET_13 << 8) - seed;
    this.secShift13 = (SECRET_13 >>> 56) + (SECRET_14 << 8) + seed;
    this.secShift14 = (SECRET_14 >>> 56) + (SECRET_15 << 8) - seed;
    this.secShift15 = (SECRET_15 >>> 56) + (SECRET_16 << 8) + seed;

    this.seed = seed;

    this.secShiftFinal8 = secret14 >>> 40 | secret15 << 24;
    this.secShiftFinal9 = secret15 >>> 40 | secret16 << 24;
    this.secShiftFinal10 = secret16 >>> 40 | secret17 << 24;
    this.secShiftFinal11 = secret17 >>> 40 | secret18 << 24;
    this.secShiftFinal12 = secret18 >>> 40 | secret19 << 24;
    this.secShiftFinal13 = secret19 >>> 40 | secret20 << 24;
    this.secShiftFinal14 = secret20 >>> 40 | secret21 << 24;
    this.secShiftFinal15 = secret21 >>> 40 | secret22 << 24;

    this.bitflip00 = ((SECRET_00 >>> 32) ^ (SECRET_00 & 0xFFFFFFFFL)) + seed;
    this.bitflip11 = ((SECRET_01 ^ (SECRET_01 >>> 32)) & 0xFFFFFFFFL) - seed;
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
      if (byteCount <= BULK_SIZE) {
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

      for (int off = 0, s = (((int) byteCount - 1) >>> 6) & 12;
          off + 64 <= (((int) byteCount - 1) & BULK_SIZE_MASK);
          off += 64, s += 1) {

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

      {
        long b0 = getLong(buffer, (offset - (64 - 8 * 0)) & BULK_SIZE_MASK);
        long b1 = getLong(buffer, (offset - (64 - 8 * 1)) & BULK_SIZE_MASK);
        long b2 = getLong(buffer, (offset - (64 - 8 * 2)) & BULK_SIZE_MASK);
        long b3 = getLong(buffer, (offset - (64 - 8 * 3)) & BULK_SIZE_MASK);
        long b4 = getLong(buffer, (offset - (64 - 8 * 4)) & BULK_SIZE_MASK);
        long b5 = getLong(buffer, (offset - (64 - 8 * 5)) & BULK_SIZE_MASK);
        long b6 = getLong(buffer, (offset - (64 - 8 * 6)) & BULK_SIZE_MASK);
        long b7 = getLong(buffer, (offset - (64 - 8 * 7)) & BULK_SIZE_MASK);

        acc0Loc += b1 + contrib(b0, secShift16);
        acc1Loc += b0 + contrib(b1, secShift17);
        acc2Loc += b3 + contrib(b2, secShift18);
        acc3Loc += b2 + contrib(b3, secShift19);
        acc4Loc += b5 + contrib(b4, secShift20);
        acc5Loc += b4 + contrib(b5, secShift21);
        acc6Loc += b7 + contrib(b6, secShift22);
        acc7Loc += b6 + contrib(b7, secShift23);
      }

      return finalizeHash(
          byteCount, acc0Loc, acc1Loc, acc2Loc, acc3Loc, acc4Loc, acc5Loc, acc6Loc, acc7Loc);
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
    public HashStream128 putBytes(byte[] b, int off, final int len) {
      putBytesImpl(b, off, len);
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
    public HashStream128 copy() {
      final HashStreamImpl hashStream = new HashStreamImpl();
      copyImpl(hashStream);
      return hashStream;
    }
  }

  @Override
  public HashValue128 hashBytesTo128Bits(final byte[] input, int off, int length) {
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
        long keyed = (lo + (hi << 32)) ^ bitflip23;
        long pl = INIT_ACC_1 + (length << 2);
        long low = keyed * pl;
        long high = unsignedMultiplyHigh(keyed, pl) + (low << 1);
        low ^= (high >>> 3);
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
            acc0 = (acc0 + mix2Accs(b0, b1, secret12, secret13)) ^ (b2 + b3);
            acc1 = (acc1 + mix2Accs(b2, b3, secret14, secret15)) ^ (b0 + b1);
          }
          long b0 = getLong(input, off + 32);
          long b1 = getLong(input, off + 40);
          long b2 = getLong(input, off + length - 48);
          long b3 = getLong(input, off + length - 40);
          acc0 = (acc0 + mix2Accs(b0, b1, secret08, secret09)) ^ (b2 + b3);
          acc1 = (acc1 + mix2Accs(b2, b3, secret10, secret11)) ^ (b0 + b1);
        }
        long b0 = getLong(input, off + 16);
        long b1 = getLong(input, off + 24);
        long b2 = getLong(input, off + length - 32);
        long b3 = getLong(input, off + length - 24);
        acc0 = (acc0 + mix2Accs(b0, b1, secret04, secret05)) ^ (b2 + b3);
        acc1 = (acc1 + mix2Accs(b2, b3, secret06, secret07)) ^ (b0 + b1);
      }
      long b0 = getLong(input, off);
      long b1 = getLong(input, off + 8);
      long b2 = getLong(input, off + length - 16);
      long b3 = getLong(input, off + length - 8);
      acc0 = (acc0 + mix2Accs(b0, b1, secret00, secret01)) ^ (b2 + b3);
      acc1 = (acc1 + mix2Accs(b2, b3, secret02, secret03)) ^ (b0 + b1);

      long low = avalanche3(acc0 + acc1);
      long high = -avalanche3(acc0 * INIT_ACC_1 + acc1 * INIT_ACC_4 + (length - seed) * INIT_ACC_2);
      return new HashValue128(high, low);
    }
    if (length <= 240) {
      long acc0 = length * INIT_ACC_1;
      long acc1 = 0;
      {
        long b0 = getLong(input, off);
        long b1 = getLong(input, off + 8);
        long b2 = getLong(input, off + 16);
        long b3 = getLong(input, off + 24);
        acc0 = (acc0 + mix2Accs(b0, b1, secret00, secret01)) ^ (b2 + b3);
        acc1 = (acc1 + mix2Accs(b2, b3, secret02, secret03)) ^ (b0 + b1);
      }
      {
        long b0 = getLong(input, off + 32);
        long b1 = getLong(input, off + 40);
        long b2 = getLong(input, off + 48);
        long b3 = getLong(input, off + 56);
        acc0 = (acc0 + mix2Accs(b0, b1, secret04, secret05)) ^ (b2 + b3);
        acc1 = (acc1 + mix2Accs(b2, b3, secret06, secret07)) ^ (b0 + b1);
      }
      {
        long b0 = getLong(input, off + 64);
        long b1 = getLong(input, off + 72);
        long b2 = getLong(input, off + 80);
        long b3 = getLong(input, off + 88);
        acc0 = (acc0 + mix2Accs(b0, b1, secret08, secret09)) ^ (b2 + b3);
        acc1 = (acc1 + mix2Accs(b2, b3, secret10, secret11)) ^ (b0 + b1);
      }
      {
        long b0 = getLong(input, off + 96);
        long b1 = getLong(input, off + 104);
        long b2 = getLong(input, off + 112);
        long b3 = getLong(input, off + 120);
        acc0 = (acc0 + mix2Accs(b0, b1, secret12, secret13)) ^ (b2 + b3);
        acc1 = (acc1 + mix2Accs(b2, b3, secret14, secret15)) ^ (b0 + b1);
      }
      acc0 = avalanche3(acc0);
      acc1 = avalanche3(acc1);
      if (160 <= length) {
        long b0 = getLong(input, off + 128);
        long b1 = getLong(input, off + 136);
        long b2 = getLong(input, off + 144);
        long b3 = getLong(input, off + 152);
        acc0 = (acc0 + mix2Accs(b0, b1, secShift00, secShift01)) ^ (b2 + b3);
        acc1 = (acc1 + mix2Accs(b2, b3, secShift02, secShift03)) ^ (b0 + b1);
      }
      if (192 <= length) {
        long b0 = getLong(input, off + 160);
        long b1 = getLong(input, off + 168);
        long b2 = getLong(input, off + 176);
        long b3 = getLong(input, off + 184);
        acc0 = (acc0 + mix2Accs(b0, b1, secShift04, secShift05)) ^ (b2 + b3);
        acc1 = (acc1 + mix2Accs(b2, b3, secShift06, secShift07)) ^ (b0 + b1);
      }
      if (224 <= length) {
        long b0 = getLong(input, off + 192);
        long b1 = getLong(input, off + 200);
        long b2 = getLong(input, off + 208);
        long b3 = getLong(input, off + 216);
        acc0 = (acc0 + mix2Accs(b0, b1, secShift08, secShift09)) ^ (b2 + b3);
        acc1 = (acc1 + mix2Accs(b2, b3, secShift10, secShift11)) ^ (b0 + b1);
      }
      {
        long b0 = getLong(input, off + length - 16);
        long b1 = getLong(input, off + length - 8);
        long b2 = getLong(input, off + length - 32);
        long b3 = getLong(input, off + length - 24);
        acc0 = (acc0 + mix2Accs(b0, b1, secShift12, secShift13)) ^ (b2 + b3);
        acc1 = (acc1 + mix2Accs(b2, b3, secShift14, secShift15)) ^ (b0 + b1);
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

      acc0 = mixAcc(acc0, secret16);
      acc1 = mixAcc(acc1, secret17);
      acc2 = mixAcc(acc2, secret18);
      acc3 = mixAcc(acc3, secret19);
      acc4 = mixAcc(acc4, secret20);
      acc5 = mixAcc(acc5, secret21);
      acc6 = mixAcc(acc6, secret22);
      acc7 = mixAcc(acc7, secret23);
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

      acc0 += b1 + contrib(b0, secShift16);
      acc1 += b0 + contrib(b1, secShift17);
      acc2 += b3 + contrib(b2, secShift18);
      acc3 += b2 + contrib(b3, secShift19);
      acc4 += b5 + contrib(b4, secShift20);
      acc5 += b4 + contrib(b5, secShift21);
      acc6 += b7 + contrib(b6, secShift22);
      acc7 += b6 + contrib(b7, secShift23);
    }

    return finalizeHash(length, acc0, acc1, acc2, acc3, acc4, acc5, acc6, acc7);
  }

  private HashValue128 finalizeHash(
      long length,
      long acc0,
      long acc1,
      long acc2,
      long acc3,
      long acc4,
      long acc5,
      long acc6,
      long acc7) {

    long low =
        avalanche3(
            length * INIT_ACC_1
                + mix2Accs(acc0, acc1, secShiftFinal0, secShiftFinal1)
                + mix2Accs(acc2, acc3, secShiftFinal2, secShiftFinal3)
                + mix2Accs(acc4, acc5, secShiftFinal4, secShiftFinal5)
                + mix2Accs(acc6, acc7, secShiftFinal6, secShiftFinal7));
    long high =
        avalanche3(
            ~(length * INIT_ACC_2)
                + mix2Accs(acc0, acc1, secShiftFinal8, secShiftFinal9)
                + mix2Accs(acc2, acc3, secShiftFinal10, secShiftFinal11)
                + mix2Accs(acc4, acc5, secShiftFinal12, secShiftFinal13)
                + mix2Accs(acc6, acc7, secShiftFinal14, secShiftFinal15));
    return new HashValue128(high, low);
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
        long lo = getInt(charSequence, 0) & 0xFFFFFFFFL;
        long hi = getInt(charSequence, len - 2);
        long keyed = (lo + (hi << 32)) ^ bitflip23;
        long pl = INIT_ACC_1 + (len << 3);
        long low = keyed * pl;
        long high = unsignedMultiplyHigh(keyed, pl) + (low << 1);
        low ^= (high >>> 3);
        low ^= low >>> 35;
        low *= 0x9FB21C651E98DF25L;
        low ^= low >>> 28;
        high = avalanche3(high);
        return new HashValue128(high, low);
      }
      if (len != 0) {
        int c = charSequence.charAt(0);
        int combinedl = (c << 16) | (c >>> 8) | 512;
        int combinedh = Integer.rotateLeft(Integer.reverseBytes(combinedl), 13);
        long low = avalanche64((combinedl & 0xFFFFFFFFL) ^ bitflip00);
        long high = avalanche64((combinedh & 0xFFFFFFFFL) ^ bitflip11);
        return new HashValue128(high, low);
      }
      return hash0;
    }
    if (len <= 64) {
      long acc0 = (len * INIT_ACC_1) << 1;
      long acc1 = 0;
      if (len > 16) {
        if (len > 32) {
          if (len > 48) {
            long b0 = getLong(charSequence, 24);
            long b1 = getLong(charSequence, 28);
            long b2 = getLong(charSequence, len - 32);
            long b3 = getLong(charSequence, len - 28);
            acc0 = (acc0 + mix2Accs(b0, b1, secret12, secret13)) ^ (b2 + b3);
            acc1 = (acc1 + mix2Accs(b2, b3, secret14, secret15)) ^ (b0 + b1);
          }
          long b0 = getLong(charSequence, 16);
          long b1 = getLong(charSequence, 20);
          long b2 = getLong(charSequence, len - 24);
          long b3 = getLong(charSequence, len - 20);
          acc0 = (acc0 + mix2Accs(b0, b1, secret08, secret09)) ^ (b2 + b3);
          acc1 = (acc1 + mix2Accs(b2, b3, secret10, secret11)) ^ (b0 + b1);
        }
        long b0 = getLong(charSequence, 8);
        long b1 = getLong(charSequence, 12);
        long b2 = getLong(charSequence, len - 16);
        long b3 = getLong(charSequence, len - 12);
        acc0 = (acc0 + mix2Accs(b0, b1, secret04, secret05)) ^ (b2 + b3);
        acc1 = (acc1 + mix2Accs(b2, b3, secret06, secret07)) ^ (b0 + b1);
      }
      long b0 = getLong(charSequence, 0);
      long b1 = getLong(charSequence, 4);
      long b2 = getLong(charSequence, len - 8);
      long b3 = getLong(charSequence, len - 4);
      acc0 = (acc0 + mix2Accs(b0, b1, secret00, secret01)) ^ (b2 + b3);
      acc1 = (acc1 + mix2Accs(b2, b3, secret02, secret03)) ^ (b0 + b1);

      long low = avalanche3(acc0 + acc1);
      long high =
          -avalanche3(acc0 * INIT_ACC_1 + acc1 * INIT_ACC_4 + ((len << 1) - seed) * INIT_ACC_2);
      return new HashValue128(high, low);
    }
    if (len <= 120) {
      long acc0 = (len * INIT_ACC_1) << 1;
      long acc1 = 0;
      {
        long b0 = getLong(charSequence, 0);
        long b1 = getLong(charSequence, 4);
        long b2 = getLong(charSequence, 8);
        long b3 = getLong(charSequence, 12);
        acc0 = (acc0 + mix2Accs(b0, b1, secret00, secret01)) ^ (b2 + b3);
        acc1 = (acc1 + mix2Accs(b2, b3, secret02, secret03)) ^ (b0 + b1);
      }
      {
        long b0 = getLong(charSequence, 16);
        long b1 = getLong(charSequence, 20);
        long b2 = getLong(charSequence, 24);
        long b3 = getLong(charSequence, 28);
        acc0 = (acc0 + mix2Accs(b0, b1, secret04, secret05)) ^ (b2 + b3);
        acc1 = (acc1 + mix2Accs(b2, b3, secret06, secret07)) ^ (b0 + b1);
      }
      {
        long b0 = getLong(charSequence, 32);
        long b1 = getLong(charSequence, 36);
        long b2 = getLong(charSequence, 40);
        long b3 = getLong(charSequence, 44);
        acc0 = (acc0 + mix2Accs(b0, b1, secret08, secret09)) ^ (b2 + b3);
        acc1 = (acc1 + mix2Accs(b2, b3, secret10, secret11)) ^ (b0 + b1);
      }
      {
        long b0 = getLong(charSequence, 48);
        long b1 = getLong(charSequence, 52);
        long b2 = getLong(charSequence, 56);
        long b3 = getLong(charSequence, 60);
        acc0 = (acc0 + mix2Accs(b0, b1, secret12, secret13)) ^ (b2 + b3);
        acc1 = (acc1 + mix2Accs(b2, b3, secret14, secret15)) ^ (b0 + b1);
      }
      acc0 = avalanche3(acc0);
      acc1 = avalanche3(acc1);
      if (80 <= len) {
        long b0 = getLong(charSequence, 64);
        long b1 = getLong(charSequence, 68);
        long b2 = getLong(charSequence, 72);
        long b3 = getLong(charSequence, 76);
        acc0 = (acc0 + mix2Accs(b0, b1, secShift00, secShift01)) ^ (b2 + b3);
        acc1 = (acc1 + mix2Accs(b2, b3, secShift02, secShift03)) ^ (b0 + b1);
      }
      if (96 <= len) {
        long b0 = getLong(charSequence, 80);
        long b1 = getLong(charSequence, 84);
        long b2 = getLong(charSequence, 88);
        long b3 = getLong(charSequence, 92);
        acc0 = (acc0 + mix2Accs(b0, b1, secShift04, secShift05)) ^ (b2 + b3);
        acc1 = (acc1 + mix2Accs(b2, b3, secShift06, secShift07)) ^ (b0 + b1);
      }
      if (112 <= len) {
        long b0 = getLong(charSequence, 96);
        long b1 = getLong(charSequence, 100);
        long b2 = getLong(charSequence, 104);
        long b3 = getLong(charSequence, 108);
        acc0 = (acc0 + mix2Accs(b0, b1, secShift08, secShift09)) ^ (b2 + b3);
        acc1 = (acc1 + mix2Accs(b2, b3, secShift10, secShift11)) ^ (b0 + b1);
      }
      {
        long b0 = getLong(charSequence, len - 8);
        long b1 = getLong(charSequence, len - 4);
        long b2 = getLong(charSequence, len - 16);
        long b3 = getLong(charSequence, len - 12);
        acc0 = (acc0 + mix2Accs(b0, b1, secShift12, secShift13)) ^ (b2 + b3);
        acc1 = (acc1 + mix2Accs(b2, b3, secShift14, secShift15)) ^ (b0 + b1);
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

      acc0 = mixAcc(acc0, secret16);
      acc1 = mixAcc(acc1, secret17);
      acc2 = mixAcc(acc2, secret18);
      acc3 = mixAcc(acc3, secret19);
      acc4 = mixAcc(acc4, secret20);
      acc5 = mixAcc(acc5, secret21);
      acc6 = mixAcc(acc6, secret22);
      acc7 = mixAcc(acc7, secret23);
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

      acc0 += b1 + contrib(b0, secShift16);
      acc1 += b0 + contrib(b1, secShift17);
      acc2 += b3 + contrib(b2, secShift18);
      acc3 += b2 + contrib(b3, secShift19);
      acc4 += b5 + contrib(b4, secShift20);
      acc5 += b4 + contrib(b5, secShift21);
      acc6 += b7 + contrib(b6, secShift22);
      acc7 += b6 + contrib(b7, secShift23);
    }

    return finalizeHash((long) len << 1, acc0, acc1, acc2, acc3, acc4, acc5, acc6, acc7);
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
    long acc0 = (24L * INIT_ACC_1 + mix2Accs(v1, v2, secret00, secret01)) ^ (v2 + v3);
    long acc1 = mix2Accs(v2, v3, secret02, secret03) ^ (v1 + v2);
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
}
