/*
 * Copyright 2022-2023 Dynatrace LLC
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

/** Represents a 128-bit hash value. */
public final class HashValue128 {

  private static final char[] HEX_DIGITS_LOWER_CASE = {
    '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'
  };

  private static char getHexForLeastSignificant4Bits(long l) {
    return HEX_DIGITS_LOWER_CASE[0xF & (int) l];
  }

  private final long mostSignificantBits;
  private final long leastSignificantBits;

  public HashValue128(long mostSignificantBits, long leastSignificantBits) {
    this.mostSignificantBits = mostSignificantBits;
    this.leastSignificantBits = leastSignificantBits;
  }

  public int getAsInt() {
    return (int) getLeastSignificantBits();
  }

  public long getMostSignificantBits() {
    return mostSignificantBits;
  }

  public long getLeastSignificantBits() {
    return leastSignificantBits;
  }

  public long getAsLong() {
    return getLeastSignificantBits();
  }

  @Override
  public int hashCode() {
    return getAsInt();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    HashValue128 other = (HashValue128) obj;
    if (leastSignificantBits != other.leastSignificantBits) {
      return false;
    }
    if (mostSignificantBits != other.mostSignificantBits) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return new String(
        new char[] {
          getHexForLeastSignificant4Bits(mostSignificantBits >>> 60),
          getHexForLeastSignificant4Bits(mostSignificantBits >>> 56),
          getHexForLeastSignificant4Bits(mostSignificantBits >>> 52),
          getHexForLeastSignificant4Bits(mostSignificantBits >>> 48),
          getHexForLeastSignificant4Bits(mostSignificantBits >>> 44),
          getHexForLeastSignificant4Bits(mostSignificantBits >>> 40),
          getHexForLeastSignificant4Bits(mostSignificantBits >>> 36),
          getHexForLeastSignificant4Bits(mostSignificantBits >>> 32),
          getHexForLeastSignificant4Bits(mostSignificantBits >>> 28),
          getHexForLeastSignificant4Bits(mostSignificantBits >>> 24),
          getHexForLeastSignificant4Bits(mostSignificantBits >>> 20),
          getHexForLeastSignificant4Bits(mostSignificantBits >>> 16),
          getHexForLeastSignificant4Bits(mostSignificantBits >>> 12),
          getHexForLeastSignificant4Bits(mostSignificantBits >>> 8),
          getHexForLeastSignificant4Bits(mostSignificantBits >>> 4),
          getHexForLeastSignificant4Bits(mostSignificantBits),
          getHexForLeastSignificant4Bits(leastSignificantBits >>> 60),
          getHexForLeastSignificant4Bits(leastSignificantBits >>> 56),
          getHexForLeastSignificant4Bits(leastSignificantBits >>> 52),
          getHexForLeastSignificant4Bits(leastSignificantBits >>> 48),
          getHexForLeastSignificant4Bits(leastSignificantBits >>> 44),
          getHexForLeastSignificant4Bits(leastSignificantBits >>> 40),
          getHexForLeastSignificant4Bits(leastSignificantBits >>> 36),
          getHexForLeastSignificant4Bits(leastSignificantBits >>> 32),
          getHexForLeastSignificant4Bits(leastSignificantBits >>> 28),
          getHexForLeastSignificant4Bits(leastSignificantBits >>> 24),
          getHexForLeastSignificant4Bits(leastSignificantBits >>> 20),
          getHexForLeastSignificant4Bits(leastSignificantBits >>> 16),
          getHexForLeastSignificant4Bits(leastSignificantBits >>> 12),
          getHexForLeastSignificant4Bits(leastSignificantBits >>> 8),
          getHexForLeastSignificant4Bits(leastSignificantBits >>> 4),
          getHexForLeastSignificant4Bits(leastSignificantBits)
        });
  }

  /**
   * Returns the bytes of the hash value in little-endian order.
   *
   * @return a byte array
   */
  public byte[] toByteArray() {
    return new byte[] {
      (byte) (leastSignificantBits),
      (byte) (leastSignificantBits >>> 8),
      (byte) (leastSignificantBits >>> 16),
      (byte) (leastSignificantBits >>> 24),
      (byte) (leastSignificantBits >>> 32),
      (byte) (leastSignificantBits >>> 40),
      (byte) (leastSignificantBits >>> 48),
      (byte) (leastSignificantBits >>> 56),
      (byte) (mostSignificantBits),
      (byte) (mostSignificantBits >>> 8),
      (byte) (mostSignificantBits >>> 16),
      (byte) (mostSignificantBits >>> 24),
      (byte) (mostSignificantBits >>> 32),
      (byte) (mostSignificantBits >>> 40),
      (byte) (mostSignificantBits >>> 48),
      (byte) (mostSignificantBits >>> 56),
    };
  }
}
