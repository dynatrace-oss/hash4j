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

import static com.dynatrace.hash4j.internal.ByteArrayUtil.*;

class XXH3_64 extends XXH3Base {

  private final long secShift12;
  private final long secShift13;
  private final long secShift14;
  private final long secShift15;

  private final long bitflip00;
  private final long bitflip12;
  private final long bitflip34;
  private final long bitflip56;

  private final long hash0;

  private XXH3_64(long seed) {
    super(seed);

    this.secShift12 = (SECRET_12 >>> 24) + (SECRET_13 << 40) + seed;
    this.secShift13 = (SECRET_13 >>> 24) + (SECRET_14 << 40) - seed;
    this.secShift14 = (SECRET_14 >>> 56) + (SECRET_15 << 8) + seed;
    this.secShift15 = (SECRET_15 >>> 56) + (SECRET_16 << 8) - seed;

    this.bitflip00 = ((SECRET_00 >>> 32) ^ (SECRET_00 & 0xFFFFFFFFL)) + seed;
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
      if (byteCount <= BULK_SIZE) {
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
    public HashStream64 putBytes(byte[] b, int off, final int len) {
      putBytesImpl(b, off, len);
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
    public HashStream64 copy() {
      final HashStreamImpl hashStream = new HashStreamImpl();
      copyImpl(hashStream);
      return hashStream;
    }

    @Override
    public Hasher64 getHasher() {
      return XXH3_64.this;
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
      final byte[] input, final int offIn, final long sec0, final long sec1) {
    long lo = getLong(input, offIn);
    long hi = getLong(input, offIn + 8);
    return mix2Accs(lo, hi, sec0, sec1);
  }

  private static long mix16B(
      final CharSequence input, final int offIn, final long sec0, final long sec1) {
    long lo = getLong(input, offIn);
    long hi = getLong(input, offIn + 4);
    return mix2Accs(lo, hi, sec0, sec1);
  }

  @Override
  public long hashBytesToLong(final byte[] input, final int off, final int length) {
    if (length <= 16) {
      if (length > 8) {
        long lo = getLong(input, off) ^ bitflip34;
        long hi = getLong(input, off + length - 8) ^ bitflip56;
        long acc = length + Long.reverseBytes(lo) + hi + unsignedLongMulXorFold(lo, hi);
        return avalanche3(acc);
      }
      if (length >= 4) {
        long input1 = getInt(input, off);
        long input2 = getInt(input, off + length - 4);
        long keyed = ((input2 & 0xFFFFFFFFL) + (input1 << 32)) ^ bitflip12;
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
            acc += mix16B(input, off + 48, secret12, secret13);
            acc += mix16B(input, off + length - 64, secret14, secret15);
          }
          acc += mix16B(input, off + 32, secret08, secret09);
          acc += mix16B(input, off + length - 48, secret10, secret11);
        }
        acc += mix16B(input, off + 16, secret04, secret05);
        acc += mix16B(input, off + length - 32, secret06, secret07);
      }
      acc += mix16B(input, off, secret00, secret01);
      acc += mix16B(input, off + length - 16, secret02, secret03);

      return avalanche3(acc);
    }
    if (length <= 240) {
      long acc = length * INIT_ACC_1;
      acc += mix16B(input, off + 16 * 0, secret00, secret01);
      acc += mix16B(input, off + 16 * 1, secret02, secret03);
      acc += mix16B(input, off + 16 * 2, secret04, secret05);
      acc += mix16B(input, off + 16 * 3, secret06, secret07);
      acc += mix16B(input, off + 16 * 4, secret08, secret09);
      acc += mix16B(input, off + 16 * 5, secret10, secret11);
      acc += mix16B(input, off + 16 * 6, secret12, secret13);
      acc += mix16B(input, off + 16 * 7, secret14, secret15);

      acc = avalanche3(acc);

      if (length >= 144) {
        acc += mix16B(input, off + 128, secShift00, secShift01);
        if (length >= 160) {
          acc += mix16B(input, off + 144, secShift02, secShift03);
          if (length >= 176) {
            acc += mix16B(input, off + 160, secShift04, secShift05);
            if (length >= 192) {
              acc += mix16B(input, off + 176, secShift06, secShift07);
              if (length >= 208) {
                acc += mix16B(input, off + 192, secShift08, secShift09);
                if (length >= 224) {
                  acc += mix16B(input, off + 208, secShift10, secShift11);
                  if (length >= 240) acc += mix16B(input, off + 224, secShift12, secShift13);
                }
              }
            }
          }
        }
      }
      acc += mix16B(input, off + length - 16, secShift14, secShift15);
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

  private long finalizeHash(
      long length,
      long acc0,
      long acc1,
      long acc2,
      long acc3,
      long acc4,
      long acc5,
      long acc6,
      long acc7) {

    long result64 =
        length * INIT_ACC_1
            + mix2Accs(acc0, acc1, secShiftFinal0, secShiftFinal1)
            + mix2Accs(acc2, acc3, secShiftFinal2, secShiftFinal3)
            + mix2Accs(acc4, acc5, secShiftFinal4, secShiftFinal5)
            + mix2Accs(acc6, acc7, secShiftFinal6, secShiftFinal7);

    return avalanche3(result64);
  }

  @Override
  public long hashCharsToLong(CharSequence charSequence) {

    int len = charSequence.length();

    if (len <= 8) {
      if (len > 4) {
        long lo = getLong(charSequence, 0) ^ bitflip34;
        long hi = getLong(charSequence, len - 4) ^ bitflip56;
        long acc = (len << 1) + Long.reverseBytes(lo) + hi + unsignedLongMulXorFold(lo, hi);
        return avalanche3(acc);
      }
      if (len >= 2) {
        long input1 = getInt(charSequence, 0);
        long input2 = getInt(charSequence, len - 2);
        long keyed = ((input2 & 0xFFFFFFFFL) + (input1 << 32)) ^ bitflip12;
        return rrmxmx(keyed, len << 1);
      }
      if (len != 0) {
        long c = charSequence.charAt(0);
        long combined = (c << 16) | (c >>> 8) | 512L;
        return avalanche64(combined ^ bitflip00);
      }
      return hash0;
    }
    if (len <= 64) {
      long acc = len * (INIT_ACC_1 << 1);

      if (len > 16) {
        if (len > 32) {
          if (len > 48) {
            acc += mix16B(charSequence, 24, secret12, secret13);
            acc += mix16B(charSequence, len - 32, secret14, secret15);
          }
          acc += mix16B(charSequence, 16, secret08, secret09);
          acc += mix16B(charSequence, len - 24, secret10, secret11);
        }
        acc += mix16B(charSequence, 8, secret04, secret05);
        acc += mix16B(charSequence, len - 16, secret06, secret07);
      }
      acc += mix16B(charSequence, 0, secret00, secret01);
      acc += mix16B(charSequence, len - 8, secret02, secret03);

      return avalanche3(acc);
    }
    if (len <= 120) {
      long acc = len * (INIT_ACC_1 << 1);
      acc += mix16B(charSequence, 0, secret00, secret01);
      acc += mix16B(charSequence, 8, secret02, secret03);
      acc += mix16B(charSequence, 16, secret04, secret05);
      acc += mix16B(charSequence, 24, secret06, secret07);
      acc += mix16B(charSequence, 32, secret08, secret09);
      acc += mix16B(charSequence, 40, secret10, secret11);
      acc += mix16B(charSequence, 48, secret12, secret13);
      acc += mix16B(charSequence, 56, secret14, secret15);

      acc = avalanche3(acc);

      if (len >= 72) {
        acc += mix16B(charSequence, 64, secShift00, secShift01);
        if (len >= 80) {
          acc += mix16B(charSequence, 72, secShift02, secShift03);
          if (len >= 88) {
            acc += mix16B(charSequence, 80, secShift04, secShift05);
            if (len >= 96) {
              acc += mix16B(charSequence, 88, secShift06, secShift07);
              if (len >= 104) {
                acc += mix16B(charSequence, 96, secShift08, secShift09);
                if (len >= 112) {
                  acc += mix16B(charSequence, 104, secShift10, secShift11);
                  if (len >= 120) acc += mix16B(charSequence, 112, secShift12, secShift13);
                }
              }
            }
          }
        }
      }
      acc += mix16B(charSequence, len - 8, secShift14, secShift15);
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
    long lo = v1 ^ bitflip34;
    long hi = v2 ^ bitflip56;
    long acc = 16 + Long.reverseBytes(lo) + hi + unsignedLongMulXorFold(lo, hi);
    return avalanche3(acc);
  }

  @Override
  public long hashLongLongLongToLong(long v1, long v2, long v3) {
    long acc = 0xd53368a48e1afca8L;
    acc += mix2Accs(v1, v2, secret00, secret01);
    acc += mix2Accs(v2, v3, secret02, secret03);
    return avalanche3(acc);
  }

  @Override
  protected long finish12Bytes(long a, long b) {
    long lo = a ^ bitflip34;
    long hi = b ^ bitflip56;
    long acc = 12 + Long.reverseBytes(lo) + hi + unsignedLongMulXorFold(lo, hi);
    return avalanche3(acc);
  }
}
