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

import static com.dynatrace.hash4j.hashing.AbstractHasher.*;
import static com.dynatrace.hash4j.hashing.AbstractHasher.copyCharsToByteArray;

class XXH3Base {
  protected static final int BLOCK_LEN_EXP = 10;

  protected static final long SECRET_00 = 0xbe4ba423396cfeb8L;
  protected static final long SECRET_01 = 0x1cad21f72c81017cL;
  protected static final long SECRET_02 = 0xdb979083e96dd4deL;
  protected static final long SECRET_03 = 0x1f67b3b7a4a44072L;
  protected static final long SECRET_04 = 0x78e5c0cc4ee679cbL;
  protected static final long SECRET_05 = 0x2172ffcc7dd05a82L;
  protected static final long SECRET_06 = 0x8e2443f7744608b8L;
  protected static final long SECRET_07 = 0x4c263a81e69035e0L;
  protected static final long SECRET_08 = 0xcb00c391bb52283cL;
  protected static final long SECRET_09 = 0xa32e531b8b65d088L;
  protected static final long SECRET_10 = 0x4ef90da297486471L;
  protected static final long SECRET_11 = 0xd8acdea946ef1938L;
  protected static final long SECRET_12 = 0x3f349ce33f76faa8L;
  protected static final long SECRET_13 = 0x1d4f0bc7c7bbdcf9L;
  protected static final long SECRET_14 = 0x3159b4cd4be0518aL;
  protected static final long SECRET_15 = 0x647378d9c97e9fc8L;
  protected static final long SECRET_16 = 0xc3ebd33483acc5eaL;
  protected static final long SECRET_17 = 0xeb6313faffa081c5L;
  protected static final long SECRET_18 = 0x49daf0b751dd0d17L;
  protected static final long SECRET_19 = 0x9e68d429265516d3L;
  protected static final long SECRET_20 = 0xfca1477d58be162bL;
  protected static final long SECRET_21 = 0xce31d07ad1b8f88fL;
  protected static final long SECRET_22 = 0x280416958f3acb45L;
  protected static final long SECRET_23 = 0x7e404bbbcafbd7afL;

  protected static final long INIT_ACC_0 = 0x00000000C2B2AE3DL;
  protected static final long INIT_ACC_1 = 0x9E3779B185EBCA87L;
  protected static final long INIT_ACC_2 = 0xC2B2AE3D27D4EB4FL;
  protected static final long INIT_ACC_3 = 0x165667B19E3779F9L;
  protected static final long INIT_ACC_4 = 0x85EBCA77C2B2AE63L;
  protected static final long INIT_ACC_5 = 0x0000000085EBCA77L;
  protected static final long INIT_ACC_6 = 0x27D4EB2F165667C5L;
  protected static final long INIT_ACC_7 = 0x000000009E3779B1L;

  protected final long secret00;
  protected final long secret01;
  protected final long secret02;
  protected final long secret03;
  protected final long secret04;
  protected final long secret05;
  protected final long secret06;
  protected final long secret07;
  protected final long secret08;
  protected final long secret09;
  protected final long secret10;
  protected final long secret11;
  protected final long secret12;
  protected final long secret13;
  protected final long secret14;
  protected final long secret15;
  protected final long secret16;
  protected final long secret17;
  protected final long secret18;
  protected final long secret19;
  protected final long secret20;
  protected final long secret21;
  protected final long secret22;
  protected final long secret23;

  protected final long secret[];

  protected final long secShift00;
  protected final long secShift01;
  protected final long secShift02;
  protected final long secShift03;
  protected final long secShift04;
  protected final long secShift05;
  protected final long secShift06;
  protected final long secShift07;
  protected final long secShift08;
  protected final long secShift09;
  protected final long secShift10;
  protected final long secShift11;

  protected final long secShift16;
  protected final long secShift17;
  protected final long secShift18;
  protected final long secShift19;
  protected final long secShift20;
  protected final long secShift21;
  protected final long secShift22;
  protected final long secShift23;

  protected final long secShiftFinal0;
  protected final long secShiftFinal1;
  protected final long secShiftFinal2;
  protected final long secShiftFinal3;
  protected final long secShiftFinal4;
  protected final long secShiftFinal5;
  protected final long secShiftFinal6;
  protected final long secShiftFinal7;

  protected XXH3Base(long seed) {
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

    this.secret =
        new long[] {
          secret00, secret01, secret02, secret03, secret04, secret05, secret06, secret07,
          secret08, secret09, secret10, secret11, secret12, secret13, secret14, secret15,
          secret16, secret17, secret18, secret19, secret20, secret21, secret22, secret23
        };
  }

  protected static long unsignedLongMulXorFold(final long lhs, final long rhs) {
    long upper = UnsignedMultiplyUtil.unsignedMultiplyHigh(lhs, rhs);
    long lower = lhs * rhs;
    return lower ^ upper;
  }

  protected static long avalanche64(long h64) {
    h64 ^= h64 >>> 33;
    h64 *= INIT_ACC_2;
    h64 ^= h64 >>> 29;
    h64 *= INIT_ACC_3;
    return h64 ^ (h64 >>> 32);
  }

  protected static long avalanche3(long h64) {
    h64 ^= h64 >>> 37;
    h64 *= 0x165667919E3779F9L;
    return h64 ^ (h64 >>> 32);
  }

  protected static long mix2Accs(final long lh, final long rh, long sec0, long sec8) {
    return unsignedLongMulXorFold(lh ^ sec0, rh ^ sec8);
  }

  protected static long contrib(long a, long b) {
    long k = a ^ b;
    return (0xFFFFFFFFL & k) * (k >>> 32);
  }

  protected static long mixAcc(long acc, long sec) {
    return (acc ^ (acc >>> 47) ^ sec) * INIT_ACC_7;
  }

  protected abstract class HashStreamImplBase {

    protected static final int BULK_SIZE = 256;
    protected static final int BULK_SIZE_HALF = 128;
    protected static final int BULK_SIZE_MASK = BULK_SIZE - 1;

    protected long acc0 = INIT_ACC_0;
    protected long acc1 = INIT_ACC_1;
    protected long acc2 = INIT_ACC_2;
    protected long acc3 = INIT_ACC_3;
    protected long acc4 = INIT_ACC_4;
    protected long acc5 = INIT_ACC_5;
    protected long acc6 = INIT_ACC_6;
    protected long acc7 = INIT_ACC_7;
    protected final byte[] buffer = new byte[BULK_SIZE + 8];
    protected int offset = 0;
    protected long byteCount = 0;

    protected void putByteImpl(byte v) {
      if (offset >= BULK_SIZE) {
        processBuffer();
        offset -= BULK_SIZE;
      }
      buffer[offset] = v;
      offset += 1;
      byteCount += 1;
    }

    protected void putShortImpl(short v) {
      setShort(buffer, offset, v);
      if (offset >= BULK_SIZE - 1) {
        processBuffer();
        offset -= BULK_SIZE;
        setShort(buffer, 0, (short) (v >>> (-offset << 3)));
      }
      offset += 2;
      byteCount += 2;
    }

    protected void putCharImpl(char v) {
      setChar(buffer, offset, v);
      if (offset >= BULK_SIZE - 1) {
        processBuffer();
        offset -= BULK_SIZE;
        setChar(buffer, 0, (char) (v >>> (-offset << 3)));
      }
      offset += 2;
      byteCount += 2;
    }

    protected void putIntImpl(int v) {
      setInt(buffer, offset, v);
      if (offset >= BULK_SIZE - 3) {
        processBuffer();
        offset -= BULK_SIZE;
        setInt(buffer, 0, v >>> (-offset << 3));
      }
      offset += 4;
      byteCount += 4;
    }

    protected void putLongImpl(long v) {
      setLong(buffer, offset, v);
      if (offset >= BULK_SIZE - 7) {
        processBuffer();
        offset -= BULK_SIZE;
        setLong(buffer, 0, v >>> (-offset << 3));
      }
      offset += 8;
      byteCount += 8;
    }

    protected void putBytesImpl(byte[] b, int off, final int len) {
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
    }

    protected void putCharsImpl(CharSequence c) {
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
    }

    protected void resetImpl() {
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
    }

    protected void copyImpl(HashStreamImplBase hashStream) {
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
    }

    private void processBuffer() {
      int s = (int) ((byteCount - 1) >>> 6) & 12;
      processBuffer(0, buffer, s);
    }

    private void mixAcc() {
      acc0 = XXH3Base.mixAcc(acc0, secret16);
      acc1 = XXH3Base.mixAcc(acc1, secret17);
      acc2 = XXH3Base.mixAcc(acc2, secret18);
      acc3 = XXH3Base.mixAcc(acc3, secret19);
      acc4 = XXH3Base.mixAcc(acc4, secret20);
      acc5 = XXH3Base.mixAcc(acc5, secret21);
      acc6 = XXH3Base.mixAcc(acc6, secret22);
      acc7 = XXH3Base.mixAcc(acc7, secret23);
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
  }
}
