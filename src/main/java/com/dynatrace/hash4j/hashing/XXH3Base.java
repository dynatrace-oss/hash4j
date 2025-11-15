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

import static com.dynatrace.hash4j.internal.ByteArrayUtil.copyCharsToByteArray;
import static com.dynatrace.hash4j.internal.ByteArrayUtil.getLong;
import static com.dynatrace.hash4j.internal.ByteArrayUtil.setChar;
import static com.dynatrace.hash4j.internal.ByteArrayUtil.setInt;
import static com.dynatrace.hash4j.internal.ByteArrayUtil.setLong;
import static com.dynatrace.hash4j.internal.ByteArrayUtil.setShort;
import static com.dynatrace.hash4j.internal.Preconditions.checkArgument;

abstract class XXH3Base implements AbstractHasher64 {
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

  protected final long[] secret;
  protected final long[] secShift;

  protected final long secShiftFinalA0;
  protected final long secShiftFinalA1;
  protected final long secShiftFinalA2;
  protected final long secShiftFinalA3;
  protected final long secShiftFinalA4;
  protected final long secShiftFinalA5;
  protected final long secShiftFinalA6;
  protected final long secShiftFinalA7;

  protected final long secShiftFinalB0;
  protected final long secShiftFinalB1;
  protected final long secShiftFinalB2;
  protected final long secShiftFinalB3;
  protected final long secShiftFinalB4;
  protected final long secShiftFinalB5;
  protected final long secShiftFinalB6;
  protected final long secShiftFinalB7;

  protected final long bitflip00;

  protected XXH3Base(long seed, boolean is128) {
    this.secret =
        new long[] {
          SECRET_00 + seed, SECRET_01 - seed, SECRET_02 + seed, SECRET_03 - seed, SECRET_04 + seed,
              SECRET_05 - seed, SECRET_06 + seed, SECRET_07 - seed,
          SECRET_08 + seed, SECRET_09 - seed, SECRET_10 + seed, SECRET_11 - seed, SECRET_12 + seed,
              SECRET_13 - seed, SECRET_14 + seed, SECRET_15 - seed,
          SECRET_16 + seed, SECRET_17 - seed, SECRET_18 + seed, SECRET_19 - seed, SECRET_20 + seed,
              SECRET_21 - seed, SECRET_22 + seed, SECRET_23 - seed
        };

    this.secShift = new long[16];

    this.secShift[0] = (SECRET_00 >>> 24) + (SECRET_01 << 40) + seed;
    this.secShift[1] = (SECRET_01 >>> 24) + (SECRET_02 << 40) - seed;
    this.secShift[2] = (SECRET_02 >>> 24) + (SECRET_03 << 40) + seed;
    this.secShift[3] = (SECRET_03 >>> 24) + (SECRET_04 << 40) - seed;
    this.secShift[4] = (SECRET_04 >>> 24) + (SECRET_05 << 40) + seed;
    this.secShift[5] = (SECRET_05 >>> 24) + (SECRET_06 << 40) - seed;
    this.secShift[6] = (SECRET_06 >>> 24) + (SECRET_07 << 40) + seed;
    this.secShift[7] = (SECRET_07 >>> 24) + (SECRET_08 << 40) - seed;
    this.secShift[8] = (SECRET_08 >>> 24) + (SECRET_09 << 40) + seed;
    this.secShift[9] = (SECRET_09 >>> 24) + (SECRET_10 << 40) - seed;
    this.secShift[10] = (SECRET_10 >>> 24) + (SECRET_11 << 40) + seed;
    this.secShift[11] = (SECRET_11 >>> 24) + (SECRET_12 << 40) - seed;

    if (is128) {
      this.secShift[12] = (SECRET_12 >>> 56) + (SECRET_13 << 8) - seed;
      this.secShift[13] = (SECRET_13 >>> 56) + (SECRET_14 << 8) + seed;
      this.secShift[14] = (SECRET_14 >>> 56) + (SECRET_15 << 8) - seed;
      this.secShift[15] = (SECRET_15 >>> 56) + (SECRET_16 << 8) + seed;
    } else {
      this.secShift[12] = (SECRET_12 >>> 24) + (SECRET_13 << 40) + seed;
      this.secShift[13] = (SECRET_13 >>> 24) + (SECRET_14 << 40) - seed;
      this.secShift[14] = (SECRET_14 >>> 56) + (SECRET_15 << 8) + seed;
      this.secShift[15] = (SECRET_15 >>> 56) + (SECRET_16 << 8) - seed;
    }

    this.secShiftFinalA0 = secret[15] >>> 8 | secret[16] << 56;
    this.secShiftFinalA1 = secret[16] >>> 8 | secret[17] << 56;
    this.secShiftFinalA2 = secret[17] >>> 8 | secret[18] << 56;
    this.secShiftFinalA3 = secret[18] >>> 8 | secret[19] << 56;
    this.secShiftFinalA4 = secret[19] >>> 8 | secret[20] << 56;
    this.secShiftFinalA5 = secret[20] >>> 8 | secret[21] << 56;
    this.secShiftFinalA6 = secret[21] >>> 8 | secret[22] << 56;
    this.secShiftFinalA7 = secret[22] >>> 8 | secret[23] << 56;

    this.secShiftFinalB0 = secret[1] >>> 24 | secret[2] << 40;
    this.secShiftFinalB1 = secret[2] >>> 24 | secret[3] << 40;
    this.secShiftFinalB2 = secret[3] >>> 24 | secret[4] << 40;
    this.secShiftFinalB3 = secret[4] >>> 24 | secret[5] << 40;
    this.secShiftFinalB4 = secret[5] >>> 24 | secret[6] << 40;
    this.secShiftFinalB5 = secret[6] >>> 24 | secret[7] << 40;
    this.secShiftFinalB6 = secret[7] >>> 24 | secret[8] << 40;
    this.secShiftFinalB7 = secret[8] >>> 24 | secret[9] << 40;

    this.bitflip00 = ((SECRET_00 >>> 32) ^ (SECRET_00 & 0xFFFFFFFFL)) + seed;
  }

  protected long getSeed() {
    return secret[0] - SECRET_00;
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

  protected static long contrib(long a, long b) {
    long k = a ^ b;
    return (0xFFFFFFFFL & k) * (k >>> 32);
  }

  protected static long mixAcc(long acc, long sec) {
    return (acc ^ (acc >>> 47) ^ sec) * INIT_ACC_7;
  }

  protected abstract class HashStreamImplBase implements HashStream64 {

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

    @Override
    public int hashCode() {
      return getAsInt();
    }

    @Override
    public boolean equals(Object o) {
      return HashUtil.equalsHelper(this, o);
    }

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

    protected void putBytesImpl(byte[] input, int off, int len) {
      int remaining = len;
      final int x = BULK_SIZE - offset;
      if (len > x) {
        int s = (int) ((byteCount - 1) >>> 6) & 12;
        if (offset > 0) {
          System.arraycopy(input, off, buffer, offset, x);
          processBuffer(0, buffer, s);
          offset = 0;
          off += x;
          remaining -= x;
        }
        if (remaining > BULK_SIZE) {
          do {
            s += 4;
            s &= 12;
            processBuffer(off, input, s);
            off += BULK_SIZE;
            remaining -= BULK_SIZE;
          } while (remaining > BULK_SIZE);
          if (remaining < 64) {
            int l = 64 - remaining;
            System.arraycopy(input, off - l, buffer, BULK_SIZE - l, l);
          }
        }
      }
      System.arraycopy(input, off, buffer, offset, remaining);
      offset += remaining;
      byteCount += len;
    }

    protected <T> void putBytesImpl(T input, long off, long len, ByteAccess<T> access) {
      long remaining = len;
      final int x = BULK_SIZE - offset;
      if (len > x) {
        int s = (int) ((byteCount - 1) >>> 6) & 12;
        if (offset > 0) {
          access.copyToByteArray(input, off, buffer, offset, x);
          processBuffer(0, buffer, s);
          offset = 0;
          off += x;
          remaining -= x;
        }
        if (remaining > BULK_SIZE) {
          do {
            s += 4;
            s &= 12;
            processBuffer(off, input, s, access);
            off += BULK_SIZE;
            remaining -= BULK_SIZE;
          } while (remaining > BULK_SIZE);
          if (remaining < 64) {
            int l = 64 - (int) remaining;
            access.copyToByteArray(input, off - l, buffer, BULK_SIZE - l, l);
          }
        }
      }
      access.copyToByteArray(input, off, buffer, offset, (int) remaining);
      offset += (int) remaining;
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

    private static final byte SERIAL_VERSION_V0 = 0;

    @Override
    public byte[] getState() {
      int numBufferBytes = ((byteCount > BULK_SIZE || byteCount < 0) && offset < 64) ? 64 : offset;
      byte[] state =
          new byte[9 + ((byteCount > BULK_SIZE || byteCount < 0) ? 64 : 0) + numBufferBytes];
      state[0] = SERIAL_VERSION_V0;
      int off = 1;

      setLong(state, off, byteCount);
      off += 8;

      if (byteCount > BULK_SIZE || byteCount < 0) {
        setLong(state, off, acc0);
        off += 8;

        setLong(state, off, acc1);
        off += 8;

        setLong(state, off, acc2);
        off += 8;

        setLong(state, off, acc3);
        off += 8;

        setLong(state, off, acc4);
        off += 8;

        setLong(state, off, acc5);
        off += 8;

        setLong(state, off, acc6);
        off += 8;

        setLong(state, off, acc7);
        off += 8;
      }

      if ((byteCount > BULK_SIZE || byteCount < 0) && offset < 64) {
        System.arraycopy(buffer, BULK_SIZE - 64 + offset, state, off, 64 - offset);
        off += 64 - offset;
      }

      System.arraycopy(buffer, 0, state, off, offset);

      return state;
    }

    protected void setStateImpl(byte[] state) {
      checkArgument(state != null);
      checkArgument(state.length >= 9);
      checkArgument(state[0] == SERIAL_VERSION_V0);
      int off = 1;

      byteCount = getLong(state, off);
      off += 8;

      offset = (byteCount != 0) ? (((int) byteCount - 1) & BULK_SIZE_MASK) + 1 : 0;
      int numBufferBytes = ((byteCount > BULK_SIZE || byteCount < 0) && offset < 64) ? 64 : offset;
      checkArgument(
          state.length == 9 + ((byteCount > BULK_SIZE || byteCount < 0) ? 64 : 0) + numBufferBytes);

      if (byteCount > BULK_SIZE || byteCount < 0) {
        acc0 = getLong(state, off);
        off += 8;

        acc1 = getLong(state, off);
        off += 8;

        acc2 = getLong(state, off);
        off += 8;

        acc3 = getLong(state, off);
        off += 8;

        acc4 = getLong(state, off);
        off += 8;

        acc5 = getLong(state, off);
        off += 8;

        acc6 = getLong(state, off);
        off += 8;

        acc7 = getLong(state, off);
        off += 8;
      } else {
        acc0 = INIT_ACC_0;
        acc1 = INIT_ACC_1;
        acc2 = INIT_ACC_2;
        acc3 = INIT_ACC_3;
        acc4 = INIT_ACC_4;
        acc5 = INIT_ACC_5;
        acc6 = INIT_ACC_6;
        acc7 = INIT_ACC_7;
      }

      if ((byteCount > BULK_SIZE || byteCount < 0) && offset < 64) {
        System.arraycopy(state, off, buffer, BULK_SIZE - 64 + offset, 64 - offset);
        off += 64 - offset;
      }

      System.arraycopy(state, off, buffer, 0, offset);
    }

    private void processBuffer() {
      int s = (int) ((byteCount - 1) >>> 6) & 12;
      processBuffer(0, buffer, s);
    }

    private void mixAcc() {
      acc0 = XXH3Base.mixAcc(acc0, secret[16]);
      acc1 = XXH3Base.mixAcc(acc1, secret[17]);
      acc2 = XXH3Base.mixAcc(acc2, secret[18]);
      acc3 = XXH3Base.mixAcc(acc3, secret[19]);
      acc4 = XXH3Base.mixAcc(acc4, secret[20]);
      acc5 = XXH3Base.mixAcc(acc5, secret[21]);
      acc6 = XXH3Base.mixAcc(acc6, secret[22]);
      acc7 = XXH3Base.mixAcc(acc7, secret[23]);
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

    private <T> void processBuffer(long off, T buffer, int s, ByteAccess<T> access) {
      for (int i = 0; i < 4; ++i) {
        long o = off + (i << 6);
        long b0 = access.getLong(buffer, o + 8 * 0);
        long b1 = access.getLong(buffer, o + 8 * 1);
        long b2 = access.getLong(buffer, o + 8 * 2);
        long b3 = access.getLong(buffer, o + 8 * 3);
        long b4 = access.getLong(buffer, o + 8 * 4);
        long b5 = access.getLong(buffer, o + 8 * 5);
        long b6 = access.getLong(buffer, o + 8 * 6);
        long b7 = access.getLong(buffer, o + 8 * 7);
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

  protected abstract long finish12Bytes(long a, long b);

  @Override
  public long hashIntIntIntToLong(int v1, int v2, int v3) {
    return finish12Bytes(
        (v1 & 0xFFFFFFFFL) ^ ((long) v2 << 32), ((long) v3 << 32) ^ (v2 & 0xFFFFFFFFL));
  }

  @Override
  public long hashIntLongToLong(int v1, long v2) {
    return finish12Bytes((v1 & 0xFFFFFFFFL) ^ (v2 << 32), v2);
  }

  @Override
  public long hashLongIntToLong(long v1, int v2) {
    return finish12Bytes(v1, ((long) v2 << 32) ^ (v1 >>> 32));
  }
}
