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

class WyhashFinal3 extends AbstractHashStream {

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

  static AbstractHasher64 create() {
    return DEFAULT_HASHER_INSTANCE;
  }

  static AbstractHasher64 create(long seedForHash) {
    long[] secret = DEFAULT_SECRET;
    long seed = seedForHash ^ secret[0];
    long secret1 = secret[1];
    long secret2 = secret[2];
    long secret3 = secret[3];
    return new HasherImpl(seed, secret1, secret2, secret3);
  }

  static AbstractHasher64 create(long seedForHash, long seedForSecret) {
    long[] secret = makeSecret(seedForSecret);
    long seed = seedForHash ^ secret[0];
    long secret1 = secret[1];
    long secret2 = secret[2];
    long secret3 = secret[3];
    return new HasherImpl(seed, secret1, secret2, secret3);
  }

  private static class HasherImpl extends AbstractHasher64 {

    private final long seed;
    private final long secret1;
    private final long secret2;
    private final long secret3;

    public HasherImpl(long seed, long secret1, long secret2, long secret3) {
      this.seed = seed;
      this.secret1 = secret1;
      this.secret2 = secret2;
      this.secret3 = secret3;
    }

    @Override
    public HashStream hashStream() {
      return new WyhashFinal3(seed, secret1, secret2, secret3);
    }

    @Override
    public long hashBytesToLong(byte[] input, int off, int len) {
      long seed = this.seed;
      long a, b;
      if (len <= 16) {
        if (len >= 4) {
          a = (wyr4(input, off) << 32) | wyr4(input, off + ((len >>> 3) << 2));
          b = (wyr4(input, off + len - 4) << 32) | wyr4(input, off + len - 4 - ((len >>> 3) << 2));
        } else if (len > 0) {
          a = wyr3(input, off, len);
          b = 0;
        } else {
          a = 0;
          b = 0;
        }
      } else {
        int i = len;
        int p = off;
        if (i > 48) {
          long see1 = this.seed;
          long see2 = this.seed;
          do {
            seed = wymix(getLong(input, p) ^ this.secret1, getLong(input, p + 8) ^ seed);
            see1 = wymix(getLong(input, p + 16) ^ this.secret2, getLong(input, p + 24) ^ see1);
            see2 = wymix(getLong(input, p + 32) ^ this.secret3, getLong(input, p + 40) ^ see2);
            p += 48;
            i -= 48;
          } while (i > 48);
          seed ^= see1 ^ see2;
        }
        while (i > 16) {
          seed = wymix(getLong(input, p) ^ this.secret1, getLong(input, p + 8) ^ seed);
          i -= 16;
          p += 16;
        }
        a = getLong(input, p + i - 16);
        b = getLong(input, p + i - 8);
      }
      return wymix(this.secret1 ^ len, wymix(a ^ this.secret1, b ^ seed));
    }
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
  public HashStream putByte(byte v) {
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
  public HashStream putShort(short v) {
    setShort(buffer, offset, v);
    offset += 2;
    byteCount += 2;
    if (offset > 48) {
      offset -= 48;
      processBuffer();
      setShort(buffer, 0, getShort(buffer, 48));
    }
    return this;
  }

  @Override
  public HashStream putChar(char v) {
    setChar(buffer, offset, v);
    offset += 2;
    byteCount += 2;
    if (offset > 48) {
      offset -= 48;
      processBuffer();
      setChar(buffer, 0, getChar(buffer, 48));
    }
    return this;
  }

  @Override
  public HashStream putInt(int v) {
    setInt(buffer, offset, v);
    offset += 4;
    byteCount += 4;
    if (offset > 48) {
      offset -= 48;
      processBuffer();
      setInt(buffer, 0, getInt(buffer, 48));
    }
    return this;
  }

  @Override
  public HashStream putLong(long v) {
    setLong(buffer, offset, v);
    offset += 8;
    byteCount += 8;
    if (offset > 48) {
      offset -= 48;
      processBuffer();
      setLong(buffer, 0, getLong(buffer, 48));
    }
    return this;
  }

  @Override
  public HashStream putBytes(byte[] b, int off, int len) {
    byteCount += len;
    int x = 48 - offset;
    if (len > x) {
      System.arraycopy(b, off, buffer, offset, x);
      processBuffer();
      len -= x;
      off += x;
      while (len > 48) {
        long b0 = getLong(b, off);
        long b1 = getLong(b, off + 8);
        long b2 = getLong(b, off + 16);
        long b3 = getLong(b, off + 24);
        long b4 = getLong(b, off + 32);
        long b5 = getLong(b, off + 40);
        processBuffer(b0, b1, b2, b3, b4, b5);
        off += 48;
        len -= 48;
      }
      int y = 16 - len;
      if (y > 0 && off - y >= 0) {
        System.arraycopy(b, off - y, buffer, 32 + len, y);
      }
      offset = 0;
    }
    System.arraycopy(b, off, buffer, offset, len);
    offset += len;
    return this;
  }

  @Override
  public HashStream putChars(CharSequence s) {
    int remainingChars = s.length();
    byteCount += ((long) remainingChars) << 1;
    int off = 0;
    if (remainingChars > ((48 - offset) >>> 1)) {
      if (offset > 1) {
        while (offset < 42) {
          setLong(buffer, offset, getLong(s, off));
          off += 4;
          offset += 8;
        }
        if (offset < 46) {
          setInt(buffer, offset, getInt(s, off));
          off += 2;
          offset += 4;
        }
        if (offset < 48) {
          setChar(buffer, offset, s.charAt(off));
          off += 1;
          offset += 2;
        }
        remainingChars -= off;
        processBuffer();
        offset &= 1;
      }
      if (offset == 0) {
        if (remainingChars > 24) {
          long b0, b1, b2, b3, b4, b5;
          do {
            b0 = getLong(s, off);
            b1 = getLong(s, off + 4);
            b2 = getLong(s, off + 8);
            b3 = getLong(s, off + 12);
            b4 = getLong(s, off + 16);
            b5 = getLong(s, off + 20);
            processBuffer(b0, b1, b2, b3, b4, b5);
            off += 24;
            remainingChars -= 24;
          } while (remainingChars > 24);
          setLong(buffer, 32, b4);
          setLong(buffer, 40, b5);
        }
      } else {
        long z = buffer[(off == 0) ? 0 : 48] & 0xFFL;
        if (remainingChars >= 24) {
          long b0, b1, b2, b3, b4, b5;
          do {
            b0 = getLong(s, off);
            b1 = getLong(s, off + 4);
            b2 = getLong(s, off + 8);
            b3 = getLong(s, off + 12);
            b4 = getLong(s, off + 16);
            b5 = getLong(s, off + 20);
            long y = b5 >>> 56;
            b5 = (b4 >>> 56) | (b5 << 8);
            b4 = (b3 >>> 56) | (b4 << 8);
            b3 = (b2 >>> 56) | (b3 << 8);
            b2 = (b1 >>> 56) | (b2 << 8);
            b1 = (b0 >>> 56) | (b1 << 8);
            b0 = z | (b0 << 8);
            z = y;
            processBuffer(b0, b1, b2, b3, b4, b5);
            off += 24;
            remainingChars -= 24;
          } while (remainingChars >= 24);
          setLong(buffer, 32, b4);
          setLong(buffer, 40, b5);
        }
        buffer[0] = (byte) (z);
      }
    }
    while (remainingChars >= 4) {
      setLong(buffer, offset, getLong(s, off));
      off += 4;
      offset += 8;
      remainingChars -= 4;
    }
    if (remainingChars >= 2) {
      setInt(buffer, offset, getInt(s, off));
      off += 2;
      offset += 4;
      remainingChars -= 2;
    }
    if (remainingChars != 0) {
      setChar(buffer, offset, s.charAt(off));
      offset += 2;
    }
    return this;
  }

  private void processBuffer() {
    long b0 = getLong(buffer, 0);
    long b1 = getLong(buffer, 8);
    long b2 = getLong(buffer, 16);
    long b3 = getLong(buffer, 24);
    long b4 = getLong(buffer, 32);
    long b5 = getLong(buffer, 40);
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

  private static long wyr3(byte[] data, int off, int k) {
    return ((data[off] & 0xFFL) << 16)
        | ((data[off + (k >>> 1)] & 0xFFL) << 8)
        | (data[off + k - 1] & 0xFFL);
  }

  private static long wyr4(byte[] data, int p) {
    return getInt(data, p) & 0xFFFFFFFFL;
  }

  @Override
  public long getAsLong() {
    long a, b;
    long s = seed;
    if (byteCount <= 16) {
      if (byteCount >= 4) {
        a = (wyr4(buffer, 0) << 32) | wyr4(buffer, ((offset >>> 3) << 2));
        b = (wyr4(buffer, offset - 4) << 32) | wyr4(buffer, offset - 4 - ((offset >>> 3) << 2));
      } else if (byteCount > 0) {
        a = wyr3(buffer, 0, offset);
        b = 0;
      } else {
        a = 0;
        b = 0;
      }
    } else {
      s ^= see1 ^ see2;
      int i = offset;
      int p = 0;
      while (i > 16) {
        s = wymix(getLong(buffer, p) ^ secret1, getLong(buffer, p + 8) ^ s);
        i -= 16;
        p += 16;
      }
      if (offset >= 16) {
        a = getLong(buffer, offset - 16);
        b = getLong(buffer, offset - 8);
      } else {
        b = getLong(buffer, 0);
        a = getLong(buffer, 40);
        int shift = (offset << 3);
        if (offset > 8) {
          a = (a >>> shift) | (b << -shift);
          b = getLong(buffer, offset - 8);
        } else if (offset < 8) {
          b = (a >>> shift) | (b << -shift);
          a = getLong(buffer, offset + 32);
        }
      }
    }
    return wymix(secret1 ^ byteCount, wymix(a ^ secret1, b ^ s));
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
