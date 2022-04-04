/*
 * Copyright 2022 Dynatrace LLC
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

class WyhashFinal3 extends AbstractHashCalculator {

  private static final long[] DEFAULT_SECRET = {
    0xa0761d6478bd642fL, 0xe7037ed1a0b428dbL, 0x8ebc6af09c88c6e3L, 0x589965cc75374cc3L
  };

  private static final AbstractHasher64 DEFAULT_HASHER_INSTANCE = create(0L);

  private final byte[] buffer = new byte[48 + 8];
  private long byteCount = 0;
  private int offset = 0;

  private long seed;
  private long see1;
  private long see2;
  private final long secret1;
  private final long secret2;
  private final long secret3;

  static AbstractHasher64 create(long seedForHash, long seedForSecret) {
    long[] secret = makeSecret(seedForSecret);
    long seed = seedForHash ^ secret[0];
    long secret1 = secret[1];
    long secret2 = secret[2];
    long secret3 = secret[3];
    return new AbstractHasher64() {
      @Override
      protected HashCalculator newHashCalculator() {
        return new WyhashFinal3(seed, secret1, secret2, secret3);
      }
    };
  }

  static AbstractHasher64 create(long seedForHash) {
    long[] secret = DEFAULT_SECRET;
    long seed = seedForHash ^ secret[0];
    long secret1 = secret[1];
    long secret2 = secret[2];
    long secret3 = secret[3];
    return new AbstractHasher64() {
      @Override
      protected HashCalculator newHashCalculator() {
        return new WyhashFinal3(seed, secret1, secret2, secret3);
      }
    };
  }

  static AbstractHasher64 create() {
    return DEFAULT_HASHER_INSTANCE;
  }

  private WyhashFinal3(long seed, long secret1, long secret2, long secret3) {
    this.seed = seed;
    this.see1 = seed;
    this.see2 = seed;
    this.secret1 = secret1;
    this.secret2 = secret2;
    this.secret3 = secret3;
  }

  @Override
  public HashSink putByte(byte v) {
    buffer[offset] = v;
    offset += 1;
    byteCount += 1;
    if (offset > 48) {
      offset -= 48;
      processBuffer();
      buffer[0] = buffer[48];
    }
    return this;
  }

  @Override
  public HashSink putShort(short v) {
    SHORT_HANDLE.set(buffer, offset, v);
    offset += 2;
    byteCount += 2;
    if (offset > 48) {
      offset -= 48;
      processBuffer();
      SHORT_HANDLE.set(buffer, 0, (short) SHORT_HANDLE.get(buffer, 48));
    }
    return this;
  }

  @Override
  public HashSink putChar(char v) {
    CHAR_HANDLE.set(buffer, offset, v);
    offset += 2;
    byteCount += 2;
    if (offset > 48) {
      offset -= 48;
      processBuffer();
      CHAR_HANDLE.set(buffer, 0, (char) CHAR_HANDLE.get(buffer, 48));
    }
    return this;
  }

  @Override
  public HashSink putInt(int v) {
    INT_HANDLE.set(buffer, offset, v);
    offset += 4;
    byteCount += 4;
    if (offset > 48) {
      offset -= 48;
      processBuffer();
      INT_HANDLE.set(buffer, 0, (int) INT_HANDLE.get(buffer, 48));
    }
    return this;
  }

  @Override
  public HashSink putLong(long v) {
    LONG_HANDLE.set(buffer, offset, v);
    offset += 8;
    byteCount += 8;
    if (offset > 48) {
      offset -= 48;
      processBuffer();
      LONG_HANDLE.set(buffer, 0, (long) LONG_HANDLE.get(buffer, 48));
    }
    return this;
  }

  @Override
  public HashSink putBytes(byte[] b, int off, int len) {
    byteCount += len;
    int p = 48 - offset;
    if (len <= p) {
      System.arraycopy(b, off, buffer, offset, len);
      offset += len;
      return this;
    }
    System.arraycopy(b, off, buffer, offset, p);
    processBuffer();
    offset = len - p;
    p += off;
    while (offset > 48) {
      long b0 = (long) LONG_HANDLE.get(b, p + 0);
      long b1 = (long) LONG_HANDLE.get(b, p + 8);
      long b2 = (long) LONG_HANDLE.get(b, p + 16);
      long b3 = (long) LONG_HANDLE.get(b, p + 24);
      long b4 = (long) LONG_HANDLE.get(b, p + 32);
      long b5 = (long) LONG_HANDLE.get(b, p + 40);
      processBuffer(b0, b1, b2, b3, b4, b5);
      p += 48;
      offset -= 48;
    }
    System.arraycopy(b, p, buffer, 0, offset);
    if (offset < 16 && len > 48) {
      System.arraycopy(b, off + (len - 16), buffer, 32 + offset, 16 - offset);
    }
    return this;
  }

  @Override
  public HashSink putChars(CharSequence s) {
    final int len = s.length();
    byteCount += ((long) len) << 1;
    int i = 0;
    if (len > ((48 - offset) >> 1)) {
      while (offset < 41) {
        LONG_HANDLE.set(
            buffer,
            offset,
            ((long) s.charAt(i))
                | ((long) s.charAt(i + 1) << 16)
                | ((long) s.charAt(i + 2) << 32)
                | ((long) s.charAt(i + 3) << 48));
        i += 4;
        offset += 8;
      }
      while (offset < 48) {
        CHAR_HANDLE.set(buffer, offset, s.charAt(i));
        i += 1;
        offset += 2;
      }
      processBuffer();
      offset &= 1;
      if (offset == 0) {
        for (; i + 24 < len; i += 24) {
          long b0 =
              (long) s.charAt(i + 0)
                  | ((long) s.charAt(i + 1) << 16)
                  | ((long) s.charAt(i + 2) << 32)
                  | ((long) s.charAt(i + 3) << 48);
          long b1 =
              (long) s.charAt(i + 4)
                  | ((long) s.charAt(i + 5) << 16)
                  | ((long) s.charAt(i + 6) << 32)
                  | ((long) s.charAt(i + 7) << 48);
          long b2 =
              (long) s.charAt(i + 8)
                  | ((long) s.charAt(i + 9) << 16)
                  | ((long) s.charAt(i + 10) << 32)
                  | ((long) s.charAt(i + 11) << 48);
          long b3 =
              (long) s.charAt(i + 12)
                  | ((long) s.charAt(i + 13) << 16)
                  | ((long) s.charAt(i + 14) << 32)
                  | ((long) s.charAt(i + 15) << 48);
          long b4 =
              (long) s.charAt(i + 16)
                  | ((long) s.charAt(i + 17) << 16)
                  | ((long) s.charAt(i + 18) << 32)
                  | ((long) s.charAt(i + 19) << 48);
          long b5 =
              (long) s.charAt(i + 20)
                  | ((long) s.charAt(i + 21) << 16)
                  | ((long) s.charAt(i + 22) << 32)
                  | ((long) s.charAt(i + 23) << 48);
          processBuffer(b0, b1, b2, b3, b4, b5);
        }
      } else {
        long x = (long) s.charAt(i - 1) >>> 8;
        for (; i + 24 <= len; i += 24) {
          long b0 =
              x
                  | ((long) s.charAt(i + 0) << 8)
                  | ((long) s.charAt(i + 1) << 24)
                  | ((long) s.charAt(i + 2) << 40)
                  | ((long) s.charAt(i + 3) << 56);
          long b1 =
              ((long) s.charAt(i + 3) >>> 8)
                  | ((long) s.charAt(i + 4) << 8)
                  | ((long) s.charAt(i + 5) << 24)
                  | ((long) s.charAt(i + 6) << 40)
                  | ((long) s.charAt(i + 7) << 56);
          long b2 =
              ((long) s.charAt(i + 7) >>> 8)
                  | ((long) s.charAt(i + 8) << 8)
                  | ((long) s.charAt(i + 9) << 24)
                  | ((long) s.charAt(i + 10) << 40)
                  | ((long) s.charAt(i + 11) << 56);
          long b3 =
              ((long) s.charAt(i + 11) >>> 8)
                  | ((long) s.charAt(i + 12) << 8)
                  | ((long) s.charAt(i + 13) << 24)
                  | ((long) s.charAt(i + 14) << 40)
                  | ((long) s.charAt(i + 15) << 56);
          long b4 =
              ((long) s.charAt(i + 15) >>> 8)
                  | ((long) s.charAt(i + 16) << 8)
                  | ((long) s.charAt(i + 17) << 24)
                  | ((long) s.charAt(i + 18) << 40)
                  | ((long) s.charAt(i + 19) << 56);
          long b5 =
              ((long) s.charAt(i + 19) >>> 8)
                  | ((long) s.charAt(i + 20) << 8)
                  | ((long) s.charAt(i + 21) << 24)
                  | ((long) s.charAt(i + 22) << 40)
                  | ((long) s.charAt(i + 23) << 56);
          x = (long) s.charAt(i + 23) >>> 8;
          processBuffer(b0, b1, b2, b3, b4, b5);
        }
        buffer[0] = (byte) (x);
      }
      if (len > 24) {
        for (int j = 32 + offset + ((len - i) << 1), k = len - 8; j < 48; j += 2, k += 1) {
          CHAR_HANDLE.set(buffer, j, s.charAt(k));
        }
      }
    }
    while (i + 3 < len) {
      LONG_HANDLE.set(
          buffer,
          offset,
          ((long) s.charAt(i))
              | ((long) s.charAt(i + 1) << 16)
              | ((long) s.charAt(i + 2) << 32)
              | ((long) s.charAt(i + 3) << 48));
      i += 4;
      offset += 8;
    }
    while (i < len) {
      CHAR_HANDLE.set(buffer, offset, s.charAt(i));
      i += 1;
      offset += 2;
    }
    return this;
  }

  private void processBuffer() {
    long b0 = (long) LONG_HANDLE.get(buffer, 0);
    long b1 = (long) LONG_HANDLE.get(buffer, 8);
    long b2 = (long) LONG_HANDLE.get(buffer, 16);
    long b3 = (long) LONG_HANDLE.get(buffer, 24);
    long b4 = (long) LONG_HANDLE.get(buffer, 32);
    long b5 = (long) LONG_HANDLE.get(buffer, 40);
    processBuffer(b0, b1, b2, b3, b4, b5);
  }

  private static long wymix(long a, long b) {
    long x = a * b;
    long y = unsignedMultiplyHigh(a, b);
    return x ^ y;
  }

  private void processBuffer(long b0, long b1, long b2, long b3, long b4, long b5) {
    seed = wymix(b0 ^ secret1, b1 ^ seed);
    see1 = wymix(b2 ^ secret2, b3 ^ see1);
    see2 = wymix(b4 ^ secret3, b5 ^ see2);
  }

  private static long wyr8(byte[] data, int p) {
    return (long) LONG_HANDLE.get(data, p);
  }

  private static long wyr3(byte[] data, int k) {
    return ((data[0] & 0xFFL) << 16) | ((data[k >>> 1] & 0xFFL) << 8) | (data[k - 1] & 0xFFL);
  }

  private static long wyr4(byte[] data, int p) {
    return (int) INT_HANDLE.get(data, p) & 0xFFFFFFFFL;
  }

  @Override
  public long getAsLong() {
    long a, b;
    if (byteCount <= 16) {
      if (byteCount >= 4) {
        a = (wyr4(buffer, 0) << 32) | wyr4(buffer, ((offset >>> 3) << 2));
        b = (wyr4(buffer, offset - 4) << 32) | wyr4(buffer, offset - 4 - ((offset >>> 3) << 2));
      } else if (byteCount > 0) {
        a = wyr3(buffer, offset);
        b = 0;
      } else {
        a = 0;
        b = 0;
      }
    } else {
      seed ^= see1 ^ see2;
      int i = offset;
      int p = 0;
      while (i > 16) {
        seed = wymix(wyr8(buffer, p) ^ secret1, wyr8(buffer, p + 8) ^ seed);
        i -= 16;
        p += 16;
      }
      if (offset >= 16) {
        a = wyr8(buffer, offset - 16);
        b = wyr8(buffer, offset - 8);
      } else {
        b = wyr8(buffer, 0);
        a = wyr8(buffer, 40);
        int shift = (offset << 3);
        if (offset > 8) {
          a = (a >>> shift) | (b << -shift);
          b = wyr8(buffer, offset - 8);
        } else if (offset < 8) {
          b = (a >>> shift) | (b << -shift);
          a = wyr8(buffer, offset + 32);
        }
      }
    }
    return wymix(secret1 ^ byteCount, wymix(a ^ secret1, b ^ seed));
  }

  private static long[] makeSecret(long seed) {
    long[] secret = new long[4];
    byte[] c = {
      (byte) 15,
      (byte) 23,
      (byte) 27,
      (byte) 29,
      (byte) 30,
      (byte) 39,
      (byte) 43,
      (byte) 45,
      (byte) 46,
      (byte) 51,
      (byte) 53,
      (byte) 54,
      (byte) 57,
      (byte) 58,
      (byte) 60,
      (byte) 71,
      (byte) 75,
      (byte) 77,
      (byte) 78,
      (byte) 83,
      (byte) 85,
      (byte) 86,
      (byte) 89,
      (byte) 90,
      (byte) 92,
      (byte) 99,
      (byte) 101,
      (byte) 102,
      (byte) 105,
      (byte) 106,
      (byte) 108,
      (byte) 113,
      (byte) 114,
      (byte) 116,
      (byte) 120,
      (byte) 135,
      (byte) 139,
      (byte) 141,
      (byte) 142,
      (byte) 147,
      (byte) 149,
      (byte) 150,
      (byte) 153,
      (byte) 154,
      (byte) 156,
      (byte) 163,
      (byte) 165,
      (byte) 166,
      (byte) 169,
      (byte) 170,
      (byte) 172,
      (byte) 177,
      (byte) 178,
      (byte) 180,
      (byte) 184,
      (byte) 195,
      (byte) 197,
      (byte) 198,
      (byte) 201,
      (byte) 202,
      (byte) 204,
      (byte) 209,
      (byte) 210,
      (byte) 212,
      (byte) 216,
      (byte) 225,
      (byte) 226,
      (byte) 228,
      (byte) 232,
      (byte) 240
    };
    for (int i = 0; i < 4; i++) {
      boolean ok;
      do {
        ok = true;
        secret[i] = 0;
        for (int j = 0; j < 64; j += 8) {
          seed += 0xa0761d6478bd642fL;
          secret[i] |=
              (c[(int) (Long.remainderUnsigned(wymix(seed, seed ^ 0xe7037ed1a0b428dbL), c.length))]
                      & 0xFFL)
                  << j;
        }
        if (secret[i] % 2 == 0) {
          ok = false;
          continue;
        }
        for (int j = 0; j < i; j++) {
          if (Long.bitCount(secret[j] ^ secret[i]) != 32) {
            ok = false;
            break;
          }
        }
      } while (!ok);
    }
    return secret;
  }

  @Override
  public int getHashBitSize() {
    return 64;
  }
}
