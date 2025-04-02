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

public final class TestUtils {

  private TestUtils() {}

  public static Hasher32 createHasherWithFixedHash(int hash) {
    return new AbstractHasher32() {
      @Override
      public int hashBytesToInt(byte[] input, int off, int len) {
        return hash;
      }

      @Override
      public int hashCharsToInt(CharSequence input) {
        return hash;
      }

      @Override
      public HashStream32 hashStream() {
        return new AbstractHashStream32() {

          @Override
          public HashStream32 putByte(byte v) {
            return this;
          }

          @Override
          public HashStream32 reset() {
            return this;
          }

          @Override
          public HashStream32 copy() {
            return this;
          }

          @Override
          public int getAsInt() {
            return hash;
          }

          @Override
          public int getHashBitSize() {
            return 32;
          }
        };
      }
    };
  }

  public static Hasher64 createHasherWithFixedHash(long hash) {
    return new AbstractHasher64() {
      @Override
      public long hashBytesToLong(byte[] input, int off, int len) {
        return hash;
      }

      @Override
      public long hashCharsToLong(CharSequence input) {
        return hash;
      }

      @Override
      public HashStream64 hashStream() {
        return new AbstractHashStream64() {

          @Override
          public HashStream64 putByte(byte v) {
            return this;
          }

          @Override
          public HashStream64 reset() {
            return this;
          }

          @Override
          public HashStream64 copy() {
            return this;
          }

          @Override
          public long getAsLong() {
            return hash;
          }

          @Override
          public int getHashBitSize() {
            return 64;
          }
        };
      }
    };
  }

  public static Hasher128 createHasherWithFixedHash(HashValue128 hash) {
    return new AbstractHasher128() {
      @Override
      public HashValue128 hashBytesTo128Bits(byte[] input, int off, int len) {
        return hash;
      }

      @Override
      public HashValue128 hashCharsTo128Bits(CharSequence input) {
        return hash;
      }

      @Override
      public HashStream128 hashStream() {
        return new AbstractHashStream128() {

          @Override
          public HashStream128 putByte(byte v) {
            return this;
          }

          @Override
          public HashStream128 reset() {
            return this;
          }

          @Override
          public HashStream128 copy() {
            return this;
          }

          @Override
          public HashValue128 get() {
            return hash;
          }

          @Override
          public int getHashBitSize() {
            return 128;
          }
        };
      }
    };
  }
}
