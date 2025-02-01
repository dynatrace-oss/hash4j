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
package com.dynatrace.hash4j.hashing;

import static com.dynatrace.hash4j.hashing.AbstractHasher.*;
import static java.lang.Long.rotateRight;

abstract class AbstractFarmHash implements AbstractHasher64 {

  protected static final long K0 = 0xc3a5c85c97cb3127L;
  protected static final long K1 = 0xb492b66fbe98f273L;
  protected static final long K2 = 0x9ae16a3b2f90404fL;
  protected static final long K_MUL = 0x9ddfea08eb382d69L;

  protected static final long shiftMix(long val) {
    return val ^ (val >>> 47);
  }

  protected static final long hashLen16(long u, long v, long mul) {
    long a = shiftMix((u ^ v) * mul);
    return shiftMix((v ^ a) * mul) * mul;
  }

  protected static final long mul(int bufferCount) {
    return K2 - 16 + (bufferCount << 1);
  }

  protected static final long hash1To3Bytes(
      int bufferCount, int firstByte, int midOrLastByte, int lastByte) {
    int y = firstByte + (midOrLastByte << 8);
    int z = bufferCount - 8 + (lastByte << 2);
    return shiftMix((y * K2) ^ (z * K0)) * K2;
  }

  protected static final long hash4To7Bytes(int bufferCount, long first4Bytes, long last4Bytes) {
    long mul = mul(bufferCount);
    return hashLen16(bufferCount - 8 + (first4Bytes << 3), last4Bytes, mul);
  }

  protected static final long hash8To16Bytes(int bufferCount, long first8Bytes, long last8Bytes) {
    long mul = mul(bufferCount);
    long a = first8Bytes + K2;
    long c = rotateRight(last8Bytes, 37) * mul + a;
    long d = (rotateRight(a, 25) + last8Bytes) * mul;
    return hashLen16(c, d, mul);
  }

  @Override
  public long hashBytesToLong(byte[] input, int off, int len) {
    if (len <= 32) {
      if (len <= 16) {
        return finalizeHash(hashBytesToLongLength0to16(input, off, len));
      } else {
        return finalizeHash(hashBytesToLongLength17to32(input, off, len));
      }
    } else if (len <= 64) {
      return finalizeHash(hashBytesToLongLength33To64(input, off, len));
    } else {
      return hashBytesToLongLength65Plus(input, off, len);
    }
  }

  protected abstract long hashBytesToLongLength65Plus(byte[] input, int off, int len);

  protected long finalizeHash(long hash) {
    return hash;
  }

  private static long hashBytesToLongLength0to16(byte[] bytes, int offset, int length) {
    if (length >= 8) {
      long mul = K2 + (length << 1);
      long a = getLong(bytes, offset) + K2;
      long b = getLong(bytes, offset + length - 8);
      long c = rotateRight(b, 37) * mul + a;
      long d = (rotateRight(a, 25) + b) * mul;
      return hashLength16(c, d, mul);
    }
    if (length >= 4) {
      long mul = K2 + (length << 1);
      long a = getInt(bytes, offset) & 0xFFFFFFFFL;
      return hashLength16(length + (a << 3), getInt(bytes, offset + length - 4) & 0xFFFFFFFFL, mul);
    }
    if (length > 0) {
      byte a = bytes[offset];
      byte b = bytes[offset + (length >> 1)];
      byte c = bytes[offset + (length - 1)];
      int y = (a & 0xFF) + ((b & 0xFF) << 8);
      int z = length + ((c & 0xFF) << 2);
      return shiftMix(y * K2 ^ z * K0) * K2;
    }
    return K2;
  }

  private static long hashCharsToLongLength0to8(CharSequence input) {
    int len = input.length();
    if (len >= 4) {
      long mul = K2 + (len << 2);
      long b = getLong(input, 0);
      long a = b + K2;
      if (len >= 5) {
        b >>>= 16;
        b |= (long) input.charAt(4) << 48;
        if (len >= 6) {
          b >>>= 16;
          b |= (long) input.charAt(5) << 48;
          if (len >= 7) {
            b >>>= 16;
            b |= (long) input.charAt(6) << 48;
            if (len >= 8) {
              b >>>= 16;
              b |= (long) input.charAt(7) << 48;
            }
          }
        }
      }
      long c = rotateRight(b, 37) * mul + a;
      long d = (rotateRight(a, 25) + b) * mul;
      return hashLength16(c, d, mul);
    }
    if (len >= 2) {
      long mul = K2 + (len << 2);
      long a = getInt(input, 0) & 0xFFFFFFFFL;
      long b = a;
      if (len >= 3) {
        b >>>= 16;
        b |= (long) input.charAt(2) << 16;
      }
      return hashLength16((len << 1) + (a << 3), b, mul);
    }
    if (len >= 1) {
      int y = input.charAt(0);
      int z = (len << 1) + ((y >>> 8) << 2);
      return shiftMix(y * K2 ^ z * K0) * K2;
    }
    return K2;
  }

  private static long hashBytesToLongLength17to32(byte[] bytes, int offset, int length) {
    long mul = K2 + (length << 1);
    long a = getLong(bytes, offset) * K1;
    long b = getLong(bytes, offset + 8);
    long c = getLong(bytes, offset + length - 8) * mul;
    long d = getLong(bytes, offset + length - 16) * K2;
    return hashLength16(
        rotateRight(a + b, 43) + rotateRight(c, 30) + d, a + rotateRight(b + K2, 18) + c, mul);
  }

  private static long hashCharsToLongLength9to16(CharSequence input) {
    int len = input.length();
    long mul = K2 + (len << 2);
    long a = getLong(input, 0) * K1;
    long b = getLong(input, 4);
    long c = getLong(input, len - 4) * mul;
    long d = getLong(input, len - 8) * K2;
    return hashLength16(
        rotateRight(a + b, 43) + rotateRight(c, 30) + d, a + rotateRight(b + K2, 18) + c, mul);
  }

  private static long hashBytesToLongLength33To64(byte[] bytes, int offset, int length) {
    long mul = K2 + (length << 1);
    long a = getLong(bytes, offset) * K2;
    long b = getLong(bytes, offset + 8);
    long c = getLong(bytes, offset + length - 8) * mul;
    long d = getLong(bytes, offset + length - 16) * K2;
    long y = rotateRight(a + b, 43) + rotateRight(c, 30) + d;
    long z = hashLength16(y, a + rotateRight(b + K2, 18) + c, mul);
    long e = getLong(bytes, offset + 16) * mul;
    long f = getLong(bytes, offset + 24);
    long g = (y + getLong(bytes, offset + length - 32)) * mul;
    long h = (z + getLong(bytes, offset + length - 24)) * mul;
    return hashLength16(
        rotateRight(e + f, 43) + rotateRight(g, 30) + h, e + rotateRight(f + a, 18) + g, mul);
  }

  private static long hashCharsToLongLength17To32(CharSequence input) {
    int len = input.length();
    long mul = K2 + (len << 2);
    long a = getLong(input, 0) * K2;
    long b = getLong(input, 4);
    long c = getLong(input, len - 4) * mul;
    long d = getLong(input, len - 8) * K2;
    long y = rotateRight(a + b, 43) + rotateRight(c, 30) + d;
    long z = hashLength16(y, a + rotateRight(b + K2, 18) + c, mul);
    long e = getLong(input, 8) * mul;
    long f = getLong(input, 12);
    long g = (y + getLong(input, len - 16)) * mul;
    long h = (z + getLong(input, len - 12)) * mul;
    return hashLength16(
        rotateRight(e + f, 43) + rotateRight(g, 30) + h, e + rotateRight(f + a, 18) + g, mul);
  }

  private static long hashLength16(long u, long v, long mul) {
    long a = (u ^ v) * mul;
    a ^= (a >>> 47);
    long b = (v ^ a) * mul;
    b ^= (b >>> 47);
    b *= mul;
    return b;
  }

  @Override
  public final long hashCharsToLong(CharSequence input) {
    long len = input.length();
    if (len <= 16) {
      if (len <= 8) {
        return finalizeHash(hashCharsToLongLength0to8(input));
      } else {
        return finalizeHash(hashCharsToLongLength9to16(input));
      }
    } else if (len <= 32) {
      return finalizeHash(hashCharsToLongLength17To32(input));
    } else {
      return hashCharsToLongLength33Plus(input);
    }
  }

  protected abstract long hashCharsToLongLength33Plus(CharSequence input);

  @Override
  public final long hashLongLongToLong(long v1, long v2) {
    long mul = K2 + 32;
    long a = v1 + K2;
    return finalizeHash(
        hashLength16(rotateRight(v2, 37) * mul + a, (rotateRight(a, 25) + v2) * mul, mul));
  }

  @Override
  public final long hashLongLongLongToLong(long v1, long v2, long v3) {
    long mul = K2 + 48;
    long a = v1 * K1;
    long c = v3 * mul;
    return finalizeHash(
        hashLength16(
            rotateRight(a + v2, 43) + rotateRight(c, 30) + v2 * K2,
            a + rotateRight(v2 + K2, 18) + c,
            mul));
  }

  protected abstract static class FarmHashStreamImpl implements AbstractHashStream64 {

    protected final byte[] buffer = new byte[64 + 8 + 8];
    protected int bufferCount = 8;
    protected boolean init = true;

    protected abstract void processBuffer(
        long b0, long b1, long b2, long b3, long b4, long b5, long b6, long b7);

    private void processBuffer() {
      long b0 = getLong(buffer, 8);
      long b1 = getLong(buffer, 16);
      long b2 = getLong(buffer, 24);
      long b3 = getLong(buffer, 32);
      long b4 = getLong(buffer, 40);
      long b5 = getLong(buffer, 48);
      long b6 = getLong(buffer, 56);
      long b7 = getLong(buffer, 64);

      processBuffer(b0, b1, b2, b3, b4, b5, b6, b7);
    }

    @Override
    public final HashStream64 putByte(byte v) {
      if (bufferCount >= 72) {
        processBuffer();
        bufferCount = 8;
      }
      buffer[bufferCount] = v;
      bufferCount += 1;
      return this;
    }

    @Override
    public final HashStream64 putShort(short v) {
      setShort(buffer, bufferCount, v);
      if (bufferCount >= 71) {
        processBuffer();
        bufferCount -= 64;
        setShort(buffer, bufferCount, v);
      }
      bufferCount += 2;
      return this;
    }

    @Override
    public final HashStream64 putChar(char v) {
      setChar(buffer, bufferCount, v);
      if (bufferCount >= 71) {
        processBuffer();
        bufferCount -= 64;
        setChar(buffer, bufferCount, v);
      }
      bufferCount += 2;
      return this;
    }

    @Override
    public final HashStream64 putInt(int v) {
      setInt(buffer, bufferCount, v);
      if (bufferCount >= 69) {
        processBuffer();
        bufferCount -= 64;
        setInt(buffer, bufferCount, v);
      }
      bufferCount += 4;
      return this;
    }

    @Override
    public final HashStream64 putLong(long v) {
      setLong(buffer, bufferCount, v);
      if (bufferCount >= 65) {
        processBuffer();
        bufferCount -= 64;
        setLong(buffer, bufferCount, v);
      }
      bufferCount += 8;
      return this;
    }

    @Override
    public final HashStream64 putBytes(byte[] b, int off, int len) {

      final int regularBlockStartIdx = (8 - bufferCount) & 0x3F;
      final int regularBlockEndIdx = len - 64 + ((-len + regularBlockStartIdx) & 0x3F);

      if (regularBlockEndIdx < regularBlockStartIdx) {
        System.arraycopy(b, off, buffer, bufferCount, len);
        bufferCount += len;
        return this;
      }

      System.arraycopy(b, off, buffer, bufferCount, regularBlockStartIdx);

      if (bufferCount > 8) {
        long b0 = getLong(buffer, 8);
        long b1 = getLong(buffer, 16);
        long b2 = getLong(buffer, 24);
        long b3 = getLong(buffer, 32);
        long b4 = getLong(buffer, 40);
        long b5 = getLong(buffer, 48);
        long b6 = getLong(buffer, 56);
        long b7 = getLong(buffer, 64);

        processBuffer(b0, b1, b2, b3, b4, b5, b6, b7);
      }

      int remainingBytes = len - regularBlockEndIdx;

      if (regularBlockEndIdx > regularBlockStartIdx) {

        for (int i = off + regularBlockStartIdx; i < off + regularBlockEndIdx; i += 64) {
          long b0 = getLong(b, i);
          long b1 = getLong(b, i + 8);
          long b2 = getLong(b, i + 16);
          long b3 = getLong(b, i + 24);
          long b4 = getLong(b, i + 32);
          long b5 = getLong(b, i + 40);
          long b6 = getLong(b, i + 48);
          long b7 = getLong(b, i + 56);
          processBuffer(b0, b1, b2, b3, b4, b5, b6, b7);
        }

        System.arraycopy(b, off - 64 + len, buffer, 8 + remainingBytes, 64 - remainingBytes);
      }
      System.arraycopy(b, off + regularBlockEndIdx, buffer, 8, remainingBytes);
      bufferCount = 8 + remainingBytes;
      return this;
    }

    @Override
    public final HashStream64 putChars(CharSequence s) {
      int idx = 0;
      if (s.length() >= ((74 - bufferCount) >> 1)) {
        idx = ((73 - bufferCount) >>> 1);
        copyCharsToByteArray(s, 0, buffer, bufferCount, idx);
        processBuffer();
        int a = bufferCount & 1;
        bufferCount = 8 - a;
        idx -= a;
        int lenMinus32 = s.length() - 32;
        if (idx < lenMinus32) {
          while (true) {

            long b0 = getLong(s, idx);
            long b1 = getLong(s, idx + 4);
            long b2 = getLong(s, idx + 8);
            long b3 = getLong(s, idx + 12);
            long b4 = getLong(s, idx + 16);
            long b5 = getLong(s, idx + 20);
            long b6 = getLong(s, idx + 24);
            long b7 = getLong(s, idx + 28);

            if (a != 0) {
              b0 = (b0 >>> 8) | (b1 << 56);
              b1 = (b1 >>> 8) | (b2 << 56);
              b2 = (b2 >>> 8) | (b3 << 56);
              b3 = (b3 >>> 8) | (b4 << 56);
              b4 = (b4 >>> 8) | (b5 << 56);
              b5 = (b5 >>> 8) | (b6 << 56);
              b6 = (b6 >>> 8) | (b7 << 56);
              b7 = (b7 >>> 8) | ((long) s.charAt(idx + 32) << 56);
            }

            processBuffer(b0, b1, b2, b3, b4, b5, b6, b7);
            idx += 32;
            if (idx >= lenMinus32) {
              setLong(buffer, 8, b0);
              setLong(buffer, 16, b1);
              setLong(buffer, 24, b2);
              setLong(buffer, 32, b3);
              setLong(buffer, 40, b4);
              setLong(buffer, 48, b5);
              setLong(buffer, 56, b6);
              setLong(buffer, 64, b7);
              break;
            }
          }
        }
      }
      copyCharsToByteArray(s, idx, buffer, bufferCount, s.length() - idx);
      bufferCount += (s.length() - idx) << 1;
      return this;
    }

    protected final long hashLen0To16(int bufferCount) {
      if (bufferCount >= 16) {
        long a = getLong(buffer, 8);
        long b = getLong(buffer, bufferCount - 8);
        return hash8To16Bytes(bufferCount, a, b);
      } else if (bufferCount >= 12) {
        long a = getInt(buffer, 8) & 0xFFFFFFFFL;
        long b = getInt(buffer, bufferCount - 4) & 0xFFFFFFFFL;
        return hash4To7Bytes(bufferCount, a, b);
      } else if (bufferCount > 8) {
        int a = buffer[8] & 0xFF;
        int b = buffer[(bufferCount >>> 1) + 4] & 0xFF;
        int c = buffer[bufferCount - 1] & 0xFF;
        return hash1To3Bytes(bufferCount, a, b, c);
      }
      return K2;
    }

    protected final long hashLen17To32(int bufferCount) {
      long mul = mul(bufferCount);
      long a = getLong(buffer, 8) * K1;
      long b = getLong(buffer, 16);
      long c = getLong(buffer, bufferCount - 8) * mul;
      long d = getLong(buffer, bufferCount - 16) * K2;
      return hashLen16(
          rotateRight(a + b, 43) + rotateRight(c, 30) + d, a + rotateRight(b + K2, 18) + c, mul);
    }

    protected final long hashLen33To64(int bufferCount) {
      long mul = mul(bufferCount);
      long a = getLong(buffer, 8) * K2;
      long b = getLong(buffer, 16);
      long c = getLong(buffer, bufferCount - 8) * mul;
      long d = getLong(buffer, bufferCount - 16) * K2;
      long yy = rotateRight(a + b, 43) + rotateRight(c, 30) + d;
      long zz = hashLen16(yy, a + rotateRight(b + K2, 18) + c, mul);
      long e = getLong(buffer, 24) * mul;
      long f = getLong(buffer, 32);
      long g = (yy + getLong(buffer, bufferCount - 32)) * mul;
      long h = (zz + getLong(buffer, bufferCount - 24)) * mul;
      return hashLen16(
          rotateRight(e + f, 43) + rotateRight(g, 30) + h, e + rotateRight(f + a, 18) + g, mul);
    }
  }
}
