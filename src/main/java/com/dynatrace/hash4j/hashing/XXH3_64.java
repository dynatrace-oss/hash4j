/*
 * Copyright 2024 Dynatrace LLC
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

public class XXH3_64 extends AbstractHasher64 {

  private static final long[] SECRET = {
    0xbe4ba423396cfeb8L,
    0x1cad21f72c81017cL,
    0xdb979083e96dd4deL,
    0x1f67b3b7a4a44072L,
    0x78e5c0cc4ee679cbL,
    0x2172ffcc7dd05a82L,
    0x8e2443f7744608b8L,
    0x4c263a81e69035e0L,
    0xcb00c391bb52283cL,
    0xa32e531b8b65d088L,
    0x4ef90da297486471L,
    0xd8acdea946ef1938L,
    0x3f349ce33f76faa8L,
    0x1d4f0bc7c7bbdcf9L,
    0x3159b4cd4be0518aL,
    0x647378d9c97e9fc8L,
    0xc3ebd33483acc5eaL,
    0xeb6313faffa081c5L,
    0x49daf0b751dd0d17L,
    0x9e68d429265516d3L,
    0xfca1477d58be162bL,
    0xce31d07ad1b8f88fL,
    0x280416958f3acb45L,
    0x7e404bbbcafbd7afL
  };

  private static final long SEC_SHIFT_0 = (SECRET[0] >>> 24) + (SECRET[1] << 40);
  private static final long SEC_SHIFT_1 = (SECRET[1] >>> 24) + (SECRET[2] << 40);
  private static final long SEC_SHIFT_2 = (SECRET[2] >>> 24) + (SECRET[3] << 40);
  private static final long SEC_SHIFT_3 = (SECRET[3] >>> 24) + (SECRET[4] << 40);
  private static final long SEC_SHIFT_4 = (SECRET[4] >>> 24) + (SECRET[5] << 40);
  private static final long SEC_SHIFT_5 = (SECRET[5] >>> 24) + (SECRET[6] << 40);
  private static final long SEC_SHIFT_6 = (SECRET[6] >>> 24) + (SECRET[7] << 40);
  private static final long SEC_SHIFT_7 = (SECRET[7] >>> 24) + (SECRET[8] << 40);
  private static final long SEC_SHIFT_8 = (SECRET[8] >>> 24) + (SECRET[9] << 40);
  private static final long SEC_SHIFT_9 = (SECRET[9] >>> 24) + (SECRET[10] << 40);
  private static final long SEC_SHIFT_10 = (SECRET[10] >>> 24) + (SECRET[11] << 40);
  private static final long SEC_SHIFT_11 = (SECRET[11] >>> 24) + (SECRET[12] << 40);
  private static final long SEC_SHIFT_12 = (SECRET[12] >>> 24) + (SECRET[13] << 40);
  private static final long SEC_SHIFT_13 = (SECRET[13] >>> 24) + (SECRET[14] << 40);
  private static final long SEC_SHIFT_14 = (SECRET[14] >>> 56) + (SECRET[15] << 8);
  private static final long SEC_SHIFT_15 = (SECRET[15] >>> 56) + (SECRET[16] << 8);

  private static final long XXH_PRIME32_1 = 0x9E3779B1L;
  private static final long XXH_PRIME32_2 = 0x85EBCA77L;
  private static final long XXH_PRIME32_3 = 0xC2B2AE3DL;

  private static final long XXH_PRIME64_1 = 0x9E3779B185EBCA87L;
  private static final long XXH_PRIME64_2 = 0xC2B2AE3D27D4EB4FL;
  private static final long XXH_PRIME64_3 = 0x165667B19E3779F9L;
  private static final long XXH_PRIME64_4 = 0x85EBCA77C2B2AE63L;
  private static final long XXH_PRIME64_5 = 0x27D4EB2F165667C5L;

  private static final long BITFLIP_00 = ((SECRET[0] >>> 32) ^ (SECRET[0] & 0xFFFFFFFFL));
  private static final long BITFLIP_12 = SECRET[1] ^ SECRET[2];
  private static final long BITFLIP_34 = SECRET[3] ^ SECRET[4];
  private static final long BITFLIP_56 = SECRET[5] ^ SECRET[6];
  private static final long BITFLIP_78 = SECRET[7] ^ SECRET[8];

  private static final int BLOCK_LEN_EXP = 10;

  private final long seed;
  private final long[] secret;

  private static long[] initCustomSecret(long seed) {
    return new long[] {
      SECRET[0] + seed,
      SECRET[1] - seed,
      SECRET[2] + seed,
      SECRET[3] - seed,
      SECRET[4] + seed,
      SECRET[5] - seed,
      SECRET[6] + seed,
      SECRET[7] - seed,
      SECRET[8] + seed,
      SECRET[9] - seed,
      SECRET[10] + seed,
      SECRET[11] - seed,
      SECRET[12] + seed,
      SECRET[13] - seed,
      SECRET[14] + seed,
      SECRET[15] - seed,
      SECRET[16] + seed,
      SECRET[17] - seed,
      SECRET[18] + seed,
      SECRET[19] - seed,
      SECRET[20] + seed,
      SECRET[21] - seed,
      SECRET[22] + seed,
      SECRET[23] - seed
    };
  }

  private XXH3_64(long seed) {
    this.seed = seed;
    this.secret = initCustomSecret(seed);
  }

  private XXH3_64() {
    this.seed = 0;
    this.secret = SECRET;
  }

  public static Hasher64 create() {
    return new XXH3_64();
  }

  public static Hasher64 create(long seed) {
    return new XXH3_64(seed);
  }

  @Override
  public HashStream64 hashStream() {
    return new HashStreamImpl();
  }

  private final class HashStreamImpl extends AbstractHashStream64 {

    private static final int BULK_SIZE = 256;
    private static final int BULK_SIZE_HALF = 128;
    private static final int BULK_SIZE_MASK = BULK_SIZE - 1;

    private long acc0 = XXH_PRIME32_3;
    private long acc1 = XXH_PRIME64_1;
    private long acc2 = XXH_PRIME64_2;
    private long acc3 = XXH_PRIME64_3;
    private long acc4 = XXH_PRIME64_4;
    private long acc5 = XXH_PRIME32_2;
    private long acc6 = XXH_PRIME64_5;
    private long acc7 = XXH_PRIME32_1;
    private final byte[] buffer = new byte[BULK_SIZE + 8];
    private int offset = 0;
    private long byteCount = 0;

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

        acc0Loc += b1 + contrib(b0, secret[15 + 0] >>> 8 | secret[16 + 0] << 56);
        acc1Loc += b0 + contrib(b1, secret[15 + 1] >>> 8 | secret[16 + 1] << 56);
        acc2Loc += b3 + contrib(b2, secret[15 + 2] >>> 8 | secret[16 + 2] << 56);
        acc3Loc += b2 + contrib(b3, secret[15 + 3] >>> 8 | secret[16 + 3] << 56);
        acc4Loc += b5 + contrib(b4, secret[15 + 4] >>> 8 | secret[16 + 4] << 56);
        acc5Loc += b4 + contrib(b5, secret[15 + 5] >>> 8 | secret[16 + 5] << 56);
        acc6Loc += b7 + contrib(b6, secret[15 + 6] >>> 8 | secret[16 + 6] << 56);
        acc7Loc += b6 + contrib(b7, secret[15 + 7] >>> 8 | secret[16 + 7] << 56);
      }

      return finalizeHash(
          byteCount, acc0Loc, acc1Loc, acc2Loc, acc3Loc, acc4Loc, acc5Loc, acc6Loc, acc7Loc);
    }

    @Override
    public HashStream64 putByte(byte v) {
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
    public HashStream64 putShort(short v) {
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
    public HashStream64 putChar(char v) {
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
    public HashStream64 putInt(int v) {
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
    public HashStream64 putLong(long v) {
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
    public HashStream64 putBytes(byte[] b, int off, final int len) {
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
    public HashStream64 putChars(CharSequence c) {
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
      acc0 = XXH3_64.mixAcc(acc0, secret[16 + 0]);
      acc1 = XXH3_64.mixAcc(acc1, secret[16 + 1]);
      acc2 = XXH3_64.mixAcc(acc2, secret[16 + 2]);
      acc3 = XXH3_64.mixAcc(acc3, secret[16 + 3]);
      acc4 = XXH3_64.mixAcc(acc4, secret[16 + 4]);
      acc5 = XXH3_64.mixAcc(acc5, secret[16 + 5]);
      acc6 = XXH3_64.mixAcc(acc6, secret[16 + 6]);
      acc7 = XXH3_64.mixAcc(acc7, secret[16 + 7]);
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
    public HashStream64 reset() {
      acc0 = XXH_PRIME32_3;
      acc1 = XXH_PRIME64_1;
      acc2 = XXH_PRIME64_2;
      acc3 = XXH_PRIME64_3;
      acc4 = XXH_PRIME64_4;
      acc5 = XXH_PRIME32_2;
      acc6 = XXH_PRIME64_5;
      acc7 = XXH_PRIME32_1;
      offset = 0;
      byteCount = 0;
      return this;
    }

    @Override
    public HashStream64 copy() {
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

  private static long unsignedLongMulXorFold(final long lhs, final long rhs) {
    long upper = UnsignedMultiplyUtil.unsignedMultiplyHigh(lhs, rhs);
    long lower = lhs * rhs;
    return lower ^ upper;
  }

  private static long avalanche3(long h64) {
    h64 ^= h64 >>> 37;
    h64 *= 0x165667919E3779F9L;
    return h64 ^ (h64 >>> 32);
  }

  private static long rrmxmx(long h64, final long length) {
    h64 ^= Long.rotateLeft(h64, 49) ^ Long.rotateLeft(h64, 24);
    h64 *= 0x9FB21C651E98DF25L;
    h64 ^= (h64 >>> 35) + length;
    h64 *= 0x9FB21C651E98DF25L;
    return h64 ^ (h64 >>> 28);
  }

  private static long avalanche64(long h64) {
    h64 ^= h64 >>> 33;
    h64 *= XXH_PRIME64_2;
    h64 ^= h64 >>> 29;
    h64 *= XXH_PRIME64_3;
    return h64 ^ (h64 >>> 32);
  }

  private static long mix16B(
      final long seed, final byte[] input, final int offIn, final long sec0, final long sec1) {
    long lo = getLong(input, offIn);
    long hi = getLong(input, offIn + 8);
    return mix2Accs(lo, hi, sec0 + seed, sec1 - seed);
  }

  private static long mix16B(
      final long seed,
      final CharSequence input,
      final int offIn,
      final long sec0,
      final long sec1) {
    long lo = getLong(input, offIn);
    long hi = getLong(input, offIn + 4);
    return mix2Accs(lo, hi, sec0 + seed, sec1 - seed);
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

  private static long mix2Accs(final long lh, final long rh, long sec0, long sec8) {
    return unsignedLongMulXorFold(lh ^ sec0, rh ^ sec8);
  }

  private static long mixAcc(long acc, long sec) {
    return (acc ^ (acc >>> 47) ^ sec) * XXH_PRIME32_1;
  }

  @Override
  public long hashBytesToLong(final byte[] input, final int off, final int length) {
    if (length <= 16) {
      if (length > 8) {
        long lo = getLong(input, off) ^ (BITFLIP_34 + seed);
        long hi = getLong(input, off + length - 8) ^ (BITFLIP_56 - seed);
        long acc = length + Long.reverseBytes(lo) + hi + unsignedLongMulXorFold(lo, hi);
        return avalanche3(acc);
      }
      if (length >= 4) {
        long input1 = getInt(input, off);
        long input2 = getInt(input, off + length - 4);
        long keyed =
            ((input2 & 0xFFFFFFFFL) + (input1 << 32))
                ^ (BITFLIP_12 - (seed ^ Long.reverseBytes(seed & 0xFFFFFFFFL)));
        return rrmxmx(keyed, length);
      }
      if (length != 0) {
        int c1 = input[off] & 0xFF;
        int c2 = input[off + (length >> 1)];
        int c3 = input[off + length - 1] & 0xFF;
        long combined = ((c1 << 16) | (c2 << 24) | c3 | (length << 8)) & 0xFFFFFFFFL;
        return avalanche64(combined ^ (BITFLIP_00 + seed));
      }
      return avalanche64(seed ^ BITFLIP_78);
    }
    if (length <= 128) {
      long acc = length * XXH_PRIME64_1;

      if (length > 32) {
        if (length > 64) {
          if (length > 96) {
            acc += mix16B(input, off + 48, secret[12], secret[13]);
            acc += mix16B(input, off + length - 64, secret[14], secret[15]);
          }
          acc += mix16B(input, off + 32, secret[8], secret[9]);
          acc += mix16B(input, off + length - 48, secret[10], secret[11]);
        }
        acc += mix16B(input, off + 16, secret[4], secret[5]);
        acc += mix16B(input, off + length - 32, secret[6], secret[7]);
      }
      acc += mix16B(input, off, secret[0], secret[1]);
      acc += mix16B(input, off + length - 16, secret[2], secret[3]);

      return avalanche3(acc);
    }
    if (length <= 240) {
      long acc = length * XXH_PRIME64_1;
      acc += mix16B(input, off + 16 * 0, secret[0], secret[1]);
      acc += mix16B(input, off + 16 * 1, secret[2], secret[3]);
      acc += mix16B(input, off + 16 * 2, secret[4], secret[5]);
      acc += mix16B(input, off + 16 * 3, secret[6], secret[7]);
      acc += mix16B(input, off + 16 * 4, secret[8], secret[9]);
      acc += mix16B(input, off + 16 * 5, secret[10], secret[11]);
      acc += mix16B(input, off + 16 * 6, secret[12], secret[13]);
      acc += mix16B(input, off + 16 * 7, secret[14], secret[15]);

      acc = avalanche3(acc);

      if (length >= 144) {
        acc += mix16B(seed, input, off + 128, SEC_SHIFT_0, SEC_SHIFT_1);
        if (length >= 160) {
          acc += mix16B(seed, input, off + 144, SEC_SHIFT_2, SEC_SHIFT_3);
          if (length >= 176) {
            acc += mix16B(seed, input, off + 160, SEC_SHIFT_4, SEC_SHIFT_5);
            if (length >= 192) {
              acc += mix16B(seed, input, off + 176, SEC_SHIFT_6, SEC_SHIFT_7);
              if (length >= 208) {
                acc += mix16B(seed, input, off + 192, SEC_SHIFT_8, SEC_SHIFT_9);
                if (length >= 224) {
                  acc += mix16B(seed, input, off + 208, SEC_SHIFT_10, SEC_SHIFT_11);
                  if (length >= 240)
                    acc += mix16B(seed, input, off + 224, SEC_SHIFT_12, SEC_SHIFT_13);
                }
              }
            }
          }
        }
      }
      acc += mix16B(seed, input, off + length - 16, SEC_SHIFT_14, SEC_SHIFT_15);
      return avalanche3(acc);
    }

    long acc0 = XXH_PRIME32_3;
    long acc1 = XXH_PRIME64_1;
    long acc2 = XXH_PRIME64_2;
    long acc3 = XXH_PRIME64_3;
    long acc4 = XXH_PRIME64_4;
    long acc5 = XXH_PRIME32_2;
    long acc6 = XXH_PRIME64_5;
    long acc7 = XXH_PRIME32_1;

    final int nbBlocks = (length - 1) >>> BLOCK_LEN_EXP;
    for (int n = 0; n < nbBlocks; n++) {
      final int offBlock = off + (n << BLOCK_LEN_EXP);
      for (int s = 0; s < 16; s++) {
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

      acc0 = mixAcc(acc0, secret[16 + 0]);
      acc1 = mixAcc(acc1, secret[16 + 1]);
      acc2 = mixAcc(acc2, secret[16 + 2]);
      acc3 = mixAcc(acc3, secret[16 + 3]);
      acc4 = mixAcc(acc4, secret[16 + 4]);
      acc5 = mixAcc(acc5, secret[16 + 5]);
      acc6 = mixAcc(acc6, secret[16 + 6]);
      acc7 = mixAcc(acc7, secret[16 + 7]);
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

      acc0 += b1 + contrib(b0, secret[15 + 0] >>> 8 | secret[16 + 0] << 56);
      acc1 += b0 + contrib(b1, secret[15 + 1] >>> 8 | secret[16 + 1] << 56);
      acc2 += b3 + contrib(b2, secret[15 + 2] >>> 8 | secret[16 + 2] << 56);
      acc3 += b2 + contrib(b3, secret[15 + 3] >>> 8 | secret[16 + 3] << 56);
      acc4 += b5 + contrib(b4, secret[15 + 4] >>> 8 | secret[16 + 4] << 56);
      acc5 += b4 + contrib(b5, secret[15 + 5] >>> 8 | secret[16 + 5] << 56);
      acc6 += b7 + contrib(b6, secret[15 + 6] >>> 8 | secret[16 + 6] << 56);
      acc7 += b6 + contrib(b7, secret[15 + 7] >>> 8 | secret[16 + 7] << 56);
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
        length * XXH_PRIME64_1
            + mix2Accs(
                acc0,
                acc1,
                (secret[1] >>> 24) | (secret[2] << 40),
                (secret[2] >>> 24) | (secret[3] << 40))
            + mix2Accs(
                acc2,
                acc3,
                (secret[3] >>> 24) | (secret[4] << 40),
                (secret[4] >>> 24) | (secret[5] << 40))
            + mix2Accs(
                acc4,
                acc5,
                (secret[5] >>> 24) | (secret[6] << 40),
                (secret[6] >>> 24) | (secret[7] << 40))
            + mix2Accs(
                acc6,
                acc7,
                (secret[7] >>> 24) | (secret[8] << 40),
                (secret[8] >>> 24) | (secret[9] << 40));

    return avalanche3(result64);
  }

  private static long contrib(long a, long b) {
    long k = a ^ b;
    return (0xFFFFFFFFL & k) * (k >>> 32);
  }

  @Override
  public long hashCharsToLong(CharSequence charSequence) {

    int len = charSequence.length();

    if (len <= 8) {
      if (len > 4) {
        long lo = getLong(charSequence, 0) ^ (BITFLIP_34 + seed);
        long hi = getLong(charSequence, len - 4) ^ (BITFLIP_56 - seed);
        long acc = (len << 1) + Long.reverseBytes(lo) + hi + unsignedLongMulXorFold(lo, hi);
        return avalanche3(acc);
      }
      if (len >= 2) {
        long input1 = getInt(charSequence, 0);
        long input2 = getInt(charSequence, len - 2);
        long keyed =
            ((input2 & 0xFFFFFFFFL) + (input1 << 32))
                ^ (BITFLIP_12 - (seed ^ Long.reverseBytes(seed & 0xFFFFFFFFL)));
        return rrmxmx(keyed, len << 1);
      }
      if (len != 0) {
        long c = charSequence.charAt(0);
        long combined = (c << 16) | (c >>> 8) | 512L;
        return avalanche64(combined ^ (BITFLIP_00 + seed));
      }
      return avalanche64(seed ^ BITFLIP_78);
    }
    if (len <= 64) {
      long acc = len * (XXH_PRIME64_1 << 1);

      if (len > 16) {
        if (len > 32) {
          if (len > 48) {
            acc += mix16B(charSequence, 24, secret[12], secret[13]);
            acc += mix16B(charSequence, len - 32, secret[14], secret[15]);
          }
          acc += mix16B(charSequence, 16, secret[8], secret[9]);
          acc += mix16B(charSequence, len - 24, secret[10], secret[11]);
        }
        acc += mix16B(charSequence, 8, secret[4], secret[5]);
        acc += mix16B(charSequence, len - 16, secret[6], secret[7]);
      }
      acc += mix16B(charSequence, 0, secret[0], secret[1]);
      acc += mix16B(charSequence, len - 8, secret[2], secret[3]);

      return avalanche3(acc);
    }
    if (len <= 120) {
      long acc = len * (XXH_PRIME64_1 << 1);
      acc += mix16B(charSequence, 0, secret[0], secret[1]);
      acc += mix16B(charSequence, 8, secret[2], secret[3]);
      acc += mix16B(charSequence, 16, secret[4], secret[5]);
      acc += mix16B(charSequence, 24, secret[6], secret[7]);
      acc += mix16B(charSequence, 32, secret[8], secret[9]);
      acc += mix16B(charSequence, 40, secret[10], secret[11]);
      acc += mix16B(charSequence, 48, secret[12], secret[13]);
      acc += mix16B(charSequence, 56, secret[14], secret[15]);

      acc = avalanche3(acc);

      if (len >= 72) {
        acc += mix16B(seed, charSequence, 64, SEC_SHIFT_0, SEC_SHIFT_1);
        if (len >= 80) {
          acc += mix16B(seed, charSequence, 72, SEC_SHIFT_2, SEC_SHIFT_3);
          if (len >= 88) {
            acc += mix16B(seed, charSequence, 80, SEC_SHIFT_4, SEC_SHIFT_5);
            if (len >= 96) {
              acc += mix16B(seed, charSequence, 88, SEC_SHIFT_6, SEC_SHIFT_7);
              if (len >= 104) {
                acc += mix16B(seed, charSequence, 96, SEC_SHIFT_8, SEC_SHIFT_9);
                if (len >= 112) {
                  acc += mix16B(seed, charSequence, 104, SEC_SHIFT_10, SEC_SHIFT_11);
                  if (len >= 120)
                    acc += mix16B(seed, charSequence, 112, SEC_SHIFT_12, SEC_SHIFT_13);
                }
              }
            }
          }
        }
      }
      acc += mix16B(seed, charSequence, len - 8, SEC_SHIFT_14, SEC_SHIFT_15);
      return avalanche3(acc);
    }

    long acc0 = XXH_PRIME32_3;
    long acc1 = XXH_PRIME64_1;
    long acc2 = XXH_PRIME64_2;
    long acc3 = XXH_PRIME64_3;
    long acc4 = XXH_PRIME64_4;
    long acc5 = XXH_PRIME32_2;
    long acc6 = XXH_PRIME64_5;
    long acc7 = XXH_PRIME32_1;

    final int nbBlocks = (len - 1) >>> (BLOCK_LEN_EXP - 1);
    for (int n = 0; n < nbBlocks; n++) {
      final int offBlock = n << (BLOCK_LEN_EXP - 1);
      for (int s = 0; s < 16; s++) {
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

      acc0 = mixAcc(acc0, secret[16 + 0]);
      acc1 = mixAcc(acc1, secret[16 + 1]);
      acc2 = mixAcc(acc2, secret[16 + 2]);
      acc3 = mixAcc(acc3, secret[16 + 3]);
      acc4 = mixAcc(acc4, secret[16 + 4]);
      acc5 = mixAcc(acc5, secret[16 + 5]);
      acc6 = mixAcc(acc6, secret[16 + 6]);
      acc7 = mixAcc(acc7, secret[16 + 7]);
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

      acc0 += b1 + contrib(b0, secret[15 + 0] >>> 8 | secret[16 + 0] << 56);
      acc1 += b0 + contrib(b1, secret[15 + 1] >>> 8 | secret[16 + 1] << 56);
      acc2 += b3 + contrib(b2, secret[15 + 2] >>> 8 | secret[16 + 2] << 56);
      acc3 += b2 + contrib(b3, secret[15 + 3] >>> 8 | secret[16 + 3] << 56);
      acc4 += b5 + contrib(b4, secret[15 + 4] >>> 8 | secret[16 + 4] << 56);
      acc5 += b4 + contrib(b5, secret[15 + 5] >>> 8 | secret[16 + 5] << 56);
      acc6 += b7 + contrib(b6, secret[15 + 6] >>> 8 | secret[16 + 6] << 56);
      acc7 += b6 + contrib(b7, secret[15 + 7] >>> 8 | secret[16 + 7] << 56);
    }

    return finalizeHash((long) len << 1, acc0, acc1, acc2, acc3, acc4, acc5, acc6, acc7);
  }

  @Override
  public long hashLongLongToLong(long v1, long v2) {
    long lo = v1 ^ (BITFLIP_34 + seed);
    long hi = v2 ^ (BITFLIP_56 - seed);
    long acc = 16 + Long.reverseBytes(lo) + hi + unsignedLongMulXorFold(lo, hi);
    return avalanche3(acc);
  }

  @Override
  public long hashLongLongLongToLong(long v1, long v2, long v3) {
    long acc = 0xd53368a48e1afca8L;
    acc += mix2Accs(v1, v2, secret[0], secret[1]);
    acc += mix2Accs(v2, v3, secret[2], secret[3]);
    return avalanche3(acc);
  }
}
