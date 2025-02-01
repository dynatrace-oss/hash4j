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
import static com.dynatrace.hash4j.hashing.XXH3Util.*;

class XXH3_128 implements AbstractHasher128 {

  private static final int BLOCK_LEN_EXP = 10;

  private final long secShift00;
  private final long secShift01;
  private final long secShift02;
  private final long secShift03;
  private final long secShift04;
  private final long secShift05;
  private final long secShift06;
  private final long secShift07;
  private final long secShift08;
  private final long secShift09;
  private final long secShift10;
  private final long secShift11;
  private final long secShift12;
  private final long secShift13;
  private final long secShift14;
  private final long secShift15;
  private final long secShift16;
  private final long secShift17;
  private final long secShift18;
  private final long secShift19;
  private final long secShift20;
  private final long secShift21;
  private final long secShift22;
  private final long secShift23;

  private final long secShiftFinal0;
  private final long secShiftFinal1;
  private final long secShiftFinal2;
  private final long secShiftFinal3;
  private final long secShiftFinal4;
  private final long secShiftFinal5;
  private final long secShiftFinal6;
  private final long secShiftFinal7;
  private final long secShiftFinal8;
  private final long secShiftFinal9;
  private final long secShiftFinal10;
  private final long secShiftFinal11;
  private final long secShiftFinal12;
  private final long secShiftFinal13;
  private final long secShiftFinal14;
  private final long secShiftFinal15;

  private final long secret00;
  private final long secret01;
  private final long secret02;
  private final long secret03;
  private final long secret04;
  private final long secret05;
  private final long secret06;
  private final long secret07;
  private final long secret08;
  private final long secret09;
  private final long secret10;
  private final long secret11;
  private final long secret12;
  private final long secret13;
  private final long secret14;
  private final long secret15;
  private final long secret16;
  private final long secret17;
  private final long secret18;
  private final long secret19;
  private final long secret20;
  private final long secret21;
  private final long secret22;
  private final long secret23;

  private final long bitflip00;
  private final long bitflip11;
  private final long bitflip23;
  private final long bitflip45;
  private final long bitflip67;

  private final HashValue128 hash0;

  private final long seed;

  private final long secret[];

  private XXH3_128(long seed) {
    this.seed = seed;
    this.secret00 = SECRET_00 + seed;
    this.secret01 = SECRET_01 - seed;
    this.secret02 = SECRET_02 + seed;
    this.secret03 = SECRET_03 - seed;
    this.secret04 = SECRET_04 + seed;
    this.secret05 = SECRET_05 - seed;
    this.secret06 = SECRET_06 + seed;
    this.secret07 = SECRET_07 - seed;
    this.secret08 = SECRET_08 + seed;
    this.secret09 = SECRET_09 - seed;
    this.secret10 = SECRET_10 + seed;
    this.secret11 = SECRET_11 - seed;
    this.secret12 = SECRET_12 + seed;
    this.secret13 = SECRET_13 - seed;
    this.secret14 = SECRET_14 + seed;
    this.secret15 = SECRET_15 - seed;
    this.secret16 = SECRET_16 + seed;
    this.secret17 = SECRET_17 - seed;
    this.secret18 = SECRET_18 + seed;
    this.secret19 = SECRET_19 - seed;
    this.secret20 = SECRET_20 + seed;
    this.secret21 = SECRET_21 - seed;
    this.secret22 = SECRET_22 + seed;
    this.secret23 = SECRET_23 - seed;

    this.secShift00 = (SECRET_00 >>> 24) + (SECRET_01 << 40) + seed;
    this.secShift01 = (SECRET_01 >>> 24) + (SECRET_02 << 40) - seed;
    this.secShift02 = (SECRET_02 >>> 24) + (SECRET_03 << 40) + seed;
    this.secShift03 = (SECRET_03 >>> 24) + (SECRET_04 << 40) - seed;
    this.secShift04 = (SECRET_04 >>> 24) + (SECRET_05 << 40) + seed;
    this.secShift05 = (SECRET_05 >>> 24) + (SECRET_06 << 40) - seed;
    this.secShift06 = (SECRET_06 >>> 24) + (SECRET_07 << 40) + seed;
    this.secShift07 = (SECRET_07 >>> 24) + (SECRET_08 << 40) - seed;
    this.secShift08 = (SECRET_08 >>> 24) + (SECRET_09 << 40) + seed;
    this.secShift09 = (SECRET_09 >>> 24) + (SECRET_10 << 40) - seed;
    this.secShift10 = (SECRET_10 >>> 24) + (SECRET_11 << 40) + seed;
    this.secShift11 = (SECRET_11 >>> 24) + (SECRET_12 << 40) - seed;
    this.secShift12 = ((SECRET_12 >>> 56) | (SECRET_13 << 8)) - seed;
    this.secShift13 = ((SECRET_13 >>> 56) | (SECRET_14 << 8)) + seed;
    this.secShift14 = ((SECRET_14 >>> 56) | (SECRET_15 << 8)) - seed;
    this.secShift15 = ((SECRET_15 >>> 56) | (SECRET_16 << 8)) + seed;

    this.secShift16 = secret15 >>> 8 | secret16 << 56;
    this.secShift17 = secret16 >>> 8 | secret17 << 56;
    this.secShift18 = secret17 >>> 8 | secret18 << 56;
    this.secShift19 = secret18 >>> 8 | secret19 << 56;
    this.secShift20 = secret19 >>> 8 | secret20 << 56;
    this.secShift21 = secret20 >>> 8 | secret21 << 56;
    this.secShift22 = secret21 >>> 8 | secret22 << 56;
    this.secShift23 = secret22 >>> 8 | secret23 << 56;

    this.secShiftFinal0 = secret01 >>> 24 | secret02 << 40;
    this.secShiftFinal1 = secret02 >>> 24 | secret03 << 40;
    this.secShiftFinal2 = secret03 >>> 24 | secret04 << 40;
    this.secShiftFinal3 = secret04 >>> 24 | secret05 << 40;
    this.secShiftFinal4 = secret05 >>> 24 | secret06 << 40;
    this.secShiftFinal5 = secret06 >>> 24 | secret07 << 40;
    this.secShiftFinal6 = secret07 >>> 24 | secret08 << 40;
    this.secShiftFinal7 = secret08 >>> 24 | secret09 << 40;

    this.secShiftFinal8 = secret14 >>> 40 | secret15 << 24;
    this.secShiftFinal9 = secret15 >>> 40 | secret16 << 24;
    this.secShiftFinal10 = secret16 >>> 40 | secret17 << 24;
    this.secShiftFinal11 = secret17 >>> 40 | secret18 << 24;
    this.secShiftFinal12 = secret18 >>> 40 | secret19 << 24;
    this.secShiftFinal13 = secret19 >>> 40 | secret20 << 24;
    this.secShiftFinal14 = secret20 >>> 40 | secret21 << 24;
    this.secShiftFinal15 = secret21 >>> 40 | secret22 << 24;

    this.bitflip00 = ((SECRET_00 ^ (SECRET_00 >>> 32)) & 0xFFFFFFFFL) + seed;
    this.bitflip11 = ((SECRET_01 ^ (SECRET_01 >>> 32)) & 0xFFFFFFFFL) - seed;
    this.bitflip23 = (SECRET_02 ^ SECRET_03) + (seed ^ Long.reverseBytes(seed & 0xFFFFFFFFL));
    this.bitflip45 = (SECRET_04 ^ SECRET_05) - seed;
    this.bitflip67 = (SECRET_06 ^ SECRET_07) + seed;

    this.hash0 =
        new HashValue128(
            avalanche64(seed ^ SECRET_10 ^ SECRET_11), avalanche64(seed ^ SECRET_08 ^ SECRET_09));

    this.secret =
        new long[] {
          secret00, secret01, secret02, secret03, secret04, secret05, secret06, secret07,
          secret08, secret09, secret10, secret11, secret12, secret13, secret14, secret15,
          secret16, secret17, secret18, secret19, secret20, secret21, secret22, secret23
        };
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

  private final class HashStreamImpl implements AbstractHashStream128 {

    private static final int BULK_SIZE = 256;
    private static final int BULK_SIZE_HALF = 128;
    private static final int BULK_SIZE_MASK = BULK_SIZE - 1;

    private long acc0 = INIT_ACC_0;
    private long acc1 = INIT_ACC_1;
    private long acc2 = INIT_ACC_2;
    private long acc3 = INIT_ACC_3;
    private long acc4 = INIT_ACC_4;
    private long acc5 = INIT_ACC_5;
    private long acc6 = INIT_ACC_6;
    private long acc7 = INIT_ACC_7;
    private final byte[] buffer = new byte[BULK_SIZE + 8];
    private int offset = 0;
    private long byteCount = 0;

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
      if (offset >= BULK_SIZE) {
        processBuffer();
        offset -= BULK_SIZE;
      }
      buffer[offset] = v;
      offset += 1;
      byteCount += 1;
      return this;
    }

    @Override
    public HashStream128 putShort(short v) {
      setShort(buffer, offset, v);
      if (offset >= BULK_SIZE - 1) {
        processBuffer();
        offset -= BULK_SIZE;
        setShort(buffer, 0, (short) (v >>> (-offset << 3)));
      }
      offset += 2;
      byteCount += 2;
      return this;
    }

    @Override
    public HashStream128 putChar(char v) {
      setChar(buffer, offset, v);
      if (offset >= BULK_SIZE - 1) {
        processBuffer();
        offset -= BULK_SIZE;
        setChar(buffer, 0, (char) (v >>> (-offset << 3)));
      }
      offset += 2;
      byteCount += 2;
      return this;
    }

    @Override
    public HashStream128 putInt(int v) {
      setInt(buffer, offset, v);
      if (offset >= BULK_SIZE - 3) {
        processBuffer();
        offset -= BULK_SIZE;
        setInt(buffer, 0, v >>> (-offset << 3));
      }
      offset += 4;
      byteCount += 4;
      return this;
    }

    @Override
    public HashStream128 putLong(long v) {
      setLong(buffer, offset, v);
      if (offset >= BULK_SIZE - 7) {
        processBuffer();
        offset -= BULK_SIZE;
        setLong(buffer, 0, v >>> (-offset << 3));
      }
      offset += 8;
      byteCount += 8;
      return this;
    }

    @Override
    public HashStream128 putBytes(byte[] b, int off, final int len) {
      int remaining = len;
      final int x = BULK_SIZE - offset;
      if (len > x) {
        int s = (int) ((byteCount - 1) >>> 6) & 12;
        if (offset > 0) {
          System.arraycopy(b, off, buffer, offset, x);
          processBuffer(0, buffer, s);
          offset = 0;
          off += x;
          remaining -= x;
        }
        if (remaining > BULK_SIZE) {
          do {
            s += 4;
            s &= 12;
            processBuffer(off, b, s);
            off += BULK_SIZE;
            remaining -= BULK_SIZE;
          } while (remaining > BULK_SIZE);
          if (remaining < 64) {
            int l = 64 - remaining;
            System.arraycopy(b, off - l, buffer, BULK_SIZE - l, l);
          }
        }
      }
      System.arraycopy(b, off, buffer, offset, remaining);
      offset += remaining;
      byteCount += len;
      return this;
    }

    @Override
    public HashStream128 putChars(CharSequence c) {
      int off = 0;
      int remaining = c.length();
      final int x = BULK_SIZE_HALF - (offset >>> 1);
      if ((offset & 1) == 0) {
        if (c.length() > x) {
          int s = (int) ((byteCount - 1) >>> 6) & 12;
          if (offset > 0) {
            copyCharsToByteArray(c, 0, buffer, offset, x);
            processBuffer(0, buffer, s);
            offset = 0;
            off += x;
            remaining -= x;
          }
          if (remaining > BULK_SIZE_HALF) {
            do {
              s += 4;
              s &= 12;
              processBuffer(off, c, s);
              off += BULK_SIZE_HALF;
              remaining -= BULK_SIZE_HALF;
            } while (remaining > BULK_SIZE_HALF);
            if (remaining < 32) {
              int l = 32 - remaining;
              copyCharsToByteArray(c, off - l, buffer, BULK_SIZE - (l << 1), l);
            }
          }
        }
      } else {
        if (c.length() >= x) {
          long extraByte;
          int s = (int) ((byteCount - 1) >>> 6) & 12;
          copyCharsToByteArray(c, 0, buffer, offset, x);
          extraByte = buffer[BULK_SIZE] & 0xFFL;
          processBuffer(0, buffer, s);
          offset = 1;
          off += x;
          remaining -= x;
          if (remaining >= BULK_SIZE_HALF) {
            do {
              s += 4;
              s &= 12;
              extraByte = processBuffer(off, c, s, extraByte);
              off += BULK_SIZE_HALF;
              remaining -= BULK_SIZE_HALF;
            } while (remaining >= BULK_SIZE_HALF);
            if (remaining < 32) {
              int l = 32 - remaining;
              copyCharsToByteArray(c, off - l, buffer, BULK_SIZE + 1 - (l << 1), l);
            }
          }
          buffer[0] = (byte) extraByte;
        }
      }
      copyCharsToByteArray(c, off, buffer, offset, remaining);
      offset += remaining << 1;
      byteCount += (long) c.length() << 1;
      return this;
    }

    private void processBuffer() {
      int s = (int) ((byteCount - 1) >>> 6) & 12;
      processBuffer(0, buffer, s);
    }

    private void mixAcc() {
      acc0 = XXH3Util.mixAcc(acc0, secret16);
      acc1 = XXH3Util.mixAcc(acc1, secret17);
      acc2 = XXH3Util.mixAcc(acc2, secret18);
      acc3 = XXH3Util.mixAcc(acc3, secret19);
      acc4 = XXH3Util.mixAcc(acc4, secret20);
      acc5 = XXH3Util.mixAcc(acc5, secret21);
      acc6 = XXH3Util.mixAcc(acc6, secret22);
      acc7 = XXH3Util.mixAcc(acc7, secret23);
    }

    private void processBuffer(int off, byte[] buffer, int s) {
      for (int i = 0; i < 4; ++i) {
        int o = off + (i << 6);
        long b0 = getLong(buffer, o + 8 * 0);
        long b1 = getLong(buffer, o + 8 * 1);
        long b2 = getLong(buffer, o + 8 * 2);
        long b3 = getLong(buffer, o + 8 * 3);
        long b4 = getLong(buffer, o + 8 * 4);
        long b5 = getLong(buffer, o + 8 * 5);
        long b6 = getLong(buffer, o + 8 * 6);
        long b7 = getLong(buffer, o + 8 * 7);
        processBuffer(b0, b1, b2, b3, b4, b5, b6, b7, s + i);
      }
      if (s == 12) {
        mixAcc();
      }
    }

    private void processBuffer(int off, CharSequence c, int s) {
      for (int i = 0; i < 4; ++i) {
        int o = off + (i << 5);
        long b0 = getLong(c, o + 4 * 0);
        long b1 = getLong(c, o + 4 * 1);
        long b2 = getLong(c, o + 4 * 2);
        long b3 = getLong(c, o + 4 * 3);
        long b4 = getLong(c, o + 4 * 4);
        long b5 = getLong(c, o + 4 * 5);
        long b6 = getLong(c, o + 4 * 6);
        long b7 = getLong(c, o + 4 * 7);
        processBuffer(b0, b1, b2, b3, b4, b5, b6, b7, s + i);
      }
      if (s == 12) {
        mixAcc();
      }
    }

    private long processBuffer(int off, CharSequence c, int s, long extraByte) {

      for (int i = 0; i < 4; ++i) {
        int o = off + (i << 5);

        long b0 = getLong(c, o + 4 * 0);
        long b1 = getLong(c, o + 4 * 1);
        long b2 = getLong(c, o + 4 * 2);
        long b3 = getLong(c, o + 4 * 3);
        long b4 = getLong(c, o + 4 * 4);
        long b5 = getLong(c, o + 4 * 5);
        long b6 = getLong(c, o + 4 * 6);
        long b7 = getLong(c, o + 4 * 7);

        long y = b7 >>> 56;
        b7 = (b6 >>> 56) | (b7 << 8);
        b6 = (b5 >>> 56) | (b6 << 8);
        b5 = (b4 >>> 56) | (b5 << 8);
        b4 = (b3 >>> 56) | (b4 << 8);
        b3 = (b2 >>> 56) | (b3 << 8);
        b2 = (b1 >>> 56) | (b2 << 8);
        b1 = (b0 >>> 56) | (b1 << 8);
        b0 = extraByte | (b0 << 8);
        extraByte = y;

        processBuffer(b0, b1, b2, b3, b4, b5, b6, b7, s + i);
      }
      if (s == 12) {
        mixAcc();
      }

      return extraByte;
    }

    private void processBuffer(
        long b0, long b1, long b2, long b3, long b4, long b5, long b6, long b7, int s) {
      acc0 += b1 + contrib(b0, secret[s + 0]);
      acc1 += b0 + contrib(b1, secret[s + 1]);
      acc2 += b3 + contrib(b2, secret[s + 2]);
      acc3 += b2 + contrib(b3, secret[s + 3]);
      acc4 += b5 + contrib(b4, secret[s + 4]);
      acc5 += b4 + contrib(b5, secret[s + 5]);
      acc6 += b7 + contrib(b6, secret[s + 6]);
      acc7 += b6 + contrib(b7, secret[s + 7]);
    }

    @Override
    public HashStream128 reset() {
      acc0 = INIT_ACC_0;
      acc1 = INIT_ACC_1;
      acc2 = INIT_ACC_2;
      acc3 = INIT_ACC_3;
      acc4 = INIT_ACC_4;
      acc5 = INIT_ACC_5;
      acc6 = INIT_ACC_6;
      acc7 = INIT_ACC_7;
      offset = 0;
      byteCount = 0;
      return this;
    }

    @Override
    public HashStream128 copy() {
      final HashStreamImpl hashStream = new HashStreamImpl();
      hashStream.acc0 = acc0;
      hashStream.acc1 = acc1;
      hashStream.acc2 = acc2;
      hashStream.acc3 = acc3;
      hashStream.acc4 = acc4;
      hashStream.acc5 = acc5;
      hashStream.acc6 = acc6;
      hashStream.acc7 = acc7;
      hashStream.offset = offset;
      hashStream.byteCount = byteCount;
      System.arraycopy(buffer, 0, hashStream.buffer, 0, buffer.length);
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
}
