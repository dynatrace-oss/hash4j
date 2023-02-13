/*
 * Copyright 2023 Dynatrace LLC
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

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;

/** Utility class for hash values. */
public final class HashValues {

  private HashValues() {}

  private static final VarHandle LONG_HANDLE =
      MethodHandles.byteArrayViewVarHandle(long[].class, ByteOrder.LITTLE_ENDIAN);
  private static final VarHandle INT_HANDLE =
      MethodHandles.byteArrayViewVarHandle(int[].class, ByteOrder.LITTLE_ENDIAN);

  private static final char[] HEX_DIGITS_LOWER_CASE = {
    '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'
  };

  private static char getHexForLeastSignificant4Bits(long l) {
    return HEX_DIGITS_LOWER_CASE[0xF & (int) l];
  }

  /**
   * Returns the given hash value as byte array.
   *
   * <p>The given integer hash value is mapped to the returned byte array in little-endian order.
   *
   * @param hashValue a 32-bit hash value
   * @return a byte array of length 4
   */
  public static byte[] toByteArray(int hashValue) {
    byte[] b = new byte[4];
    INT_HANDLE.set(b, 0, hashValue);
    return b;
  }

  /**
   * Returns the given hash value as byte array.
   *
   * <p>The given long hash value is mapped to the returned byte array in little-endian order.
   *
   * @param hashValue a 32-bit hash value
   * @return a byte array of length 4
   */
  public static byte[] toByteArray(long hashValue) {
    byte[] b = new byte[8];
    LONG_HANDLE.set(b, 0, hashValue);
    return b;
  }

  /**
   * Returns the given hash value as byte array.
   *
   * @param hashValue a 128-bit hash value
   * @return a byte array of length 16
   */
  public static byte[] toByteArray(HashValue128 hashValue) {
    byte[] b = new byte[16];
    LONG_HANDLE.set(b, 0, hashValue.getLeastSignificantBits());
    LONG_HANDLE.set(b, 8, hashValue.getMostSignificantBits());
    return b;
  }

  /**
   * Returns the hex-code representation of the hash value interpreted as numeric value.
   *
   * <p>The returned hex-codes are given in reverse order with respect to the byte sequence as given
   * by {@link #toByteArray(int)}.
   *
   * @param hashValue a 32-bit hash value
   * @return a hex-string of length 8
   */
  public static String toHexString(int hashValue) {
    return new String(
        new char[] {
          getHexForLeastSignificant4Bits(hashValue >>> 28),
          getHexForLeastSignificant4Bits(hashValue >>> 24),
          getHexForLeastSignificant4Bits(hashValue >>> 20),
          getHexForLeastSignificant4Bits(hashValue >>> 16),
          getHexForLeastSignificant4Bits(hashValue >>> 12),
          getHexForLeastSignificant4Bits(hashValue >>> 8),
          getHexForLeastSignificant4Bits(hashValue >>> 4),
          getHexForLeastSignificant4Bits(hashValue)
        });
  }

  /**
   * Returns the hex-code representation of the hash value interpreted as numeric value.
   *
   * <p>The returned hex-codes are given in reverse order with respect to the byte sequence as given
   * by {@link #toByteArray(long)}.
   *
   * @param hashValue a 64-bit hash value
   * @return a hex-string of length 16
   */
  public static String toHexString(long hashValue) {
    return new String(
        new char[] {
          getHexForLeastSignificant4Bits(hashValue >>> 60),
          getHexForLeastSignificant4Bits(hashValue >>> 56),
          getHexForLeastSignificant4Bits(hashValue >>> 52),
          getHexForLeastSignificant4Bits(hashValue >>> 48),
          getHexForLeastSignificant4Bits(hashValue >>> 44),
          getHexForLeastSignificant4Bits(hashValue >>> 40),
          getHexForLeastSignificant4Bits(hashValue >>> 36),
          getHexForLeastSignificant4Bits(hashValue >>> 32),
          getHexForLeastSignificant4Bits(hashValue >>> 28),
          getHexForLeastSignificant4Bits(hashValue >>> 24),
          getHexForLeastSignificant4Bits(hashValue >>> 20),
          getHexForLeastSignificant4Bits(hashValue >>> 16),
          getHexForLeastSignificant4Bits(hashValue >>> 12),
          getHexForLeastSignificant4Bits(hashValue >>> 8),
          getHexForLeastSignificant4Bits(hashValue >>> 4),
          getHexForLeastSignificant4Bits(hashValue)
        });
  }

  /**
   * Returns the hex-code representation of the hash value interpreted as numeric value.
   *
   * <p>The returned hex-codes are given in reverse order with respect to the byte sequence as given
   * by {@link #toByteArray(HashValue128)}.
   *
   * @param hashValue a 128-bit hash value
   * @return a hex-string of length 32
   */
  public static String toHexString(HashValue128 hashValue) {
    long mostSignificantBits = hashValue.getMostSignificantBits();
    long leastSignificantBits = hashValue.getLeastSignificantBits();
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
}
