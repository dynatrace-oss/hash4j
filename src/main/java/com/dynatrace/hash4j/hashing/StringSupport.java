/*
 * Copyright 2026 Dynatrace LLC
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
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;

/**
 * Provides reflective access to the internal byte array of {@link String} instances.
 *
 * <p>This class attempts to locate the private {@code value} field of {@link String} via reflection
 * and exposes it through a {@link VarHandle}. This enables zero-copy hashing of string contents by
 * avoiding the allocation overhead of {@code String.getBytes()}.
 *
 * <p>Verification is performed against known test strings to ensure the discovered field actually
 * holds the expected UTF-16 or Latin-1 encoded bytes.
 */
final class StringSupport {

  private StringSupport() {}

  private static final String LATIN1_TEST_STRING = "aB3$xZ7!mQ9@wP1&";
  private static final byte[] EXPECTED_LATIN1_ARRAY = {
    0x61, 0x42, 0x33, 0x24, 0x78, 0x5A, 0x37, 0x21,
    0x6D, 0x51, 0x39, 0x40, 0x77, 0x50, 0x31, 0x26
  };

  private static final String NON_LATIN1_TEST_STRING =
      "\u0410\u4E2D\u03B1\u0E01\u30A2\u05D0\u0627\u0905\u0416\u4E09\u03B3\u0E02\u30AB\u05D1\u0628\u0906";
  private static final byte[] EXPECTED_NON_LATIN1_ARRAY = {
    0x10,
    0x04,
    0x2D,
    0x4E,
    (byte) 0xB1,
    0x03,
    0x01,
    0x0E,
    (byte) 0xA2,
    0x30,
    (byte) 0xD0,
    0x05,
    0x27,
    0x06,
    0x05,
    0x09,
    0x16,
    0x04,
    0x09,
    0x4E,
    (byte) 0xB3,
    0x03,
    0x02,
    0x0E,
    (byte) 0xAB,
    0x30,
    (byte) 0xD1,
    0x05,
    0x28,
    0x06,
    0x06,
    0x09
  };

  private static final VarHandle VALUE_HANDLE = findStringValueHandle(String.class);

  /**
   * Finds a {@link VarHandle} for the single non-static {@code byte[]} field in the given class,
   * using the default test strings for verification.
   *
   * @param clazz the class to inspect (typically {@link String})
   * @return the {@link VarHandle} if found and verified, or {@code null} otherwise
   */
  static VarHandle findStringValueHandle(Class<?> clazz) {
    return findStringValueHandle(
        clazz,
        LATIN1_TEST_STRING,
        EXPECTED_LATIN1_ARRAY,
        NON_LATIN1_TEST_STRING,
        EXPECTED_NON_LATIN1_ARRAY);
  }

  /**
   * Finds a {@link VarHandle} for the single non-static {@code byte[]} field in the given class,
   * using the provided test strings and expected byte arrays for verification.
   *
   * @param clazz the class to inspect
   * @param latin1TestString a Latin-1 string for verification
   * @param expectedLatin1Array the expected internal byte array of the Latin-1 test string
   * @param nonLatin1TestString a non-Latin-1 string for verification
   * @param expectedNonLatin1Array the expected internal byte array of the non-Latin-1 test string
   * @return the {@link VarHandle} if found and verified, or {@code null} otherwise
   */
  static VarHandle findStringValueHandle(
      Class<?> clazz,
      String latin1TestString,
      byte[] expectedLatin1Array,
      String nonLatin1TestString,
      byte[] expectedNonLatin1Array) {
    try {
      // Find the single non-static array field in the given class
      Field nonStaticArrayField = null;
      for (Field field : clazz.getDeclaredFields()) {
        if (Modifier.isStatic(field.getModifiers())) {
          continue;
        }
        Class<?> type = field.getType();
        if (type.isArray()) {
          if (nonStaticArrayField != null) {
            // Found more than one non-static array field; cannot determine which holds the value
            return null;
          }
          nonStaticArrayField = field;
        }
      }
      if (nonStaticArrayField == null) {
        // No non-static array field found, or multiple were present
        return null;
      }
      Class<?> type = nonStaticArrayField.getType();
      if (type != byte[].class) {
        // The single array field is not of type byte[]; incompatible layout
        return null;
      }

      // Use MethodHandles.privateLookupIn to obtain a trusted lookup for the target class
      MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(clazz, MethodHandles.lookup());
      VarHandle handle = lookup.findVarHandle(clazz, nonStaticArrayField.getName(), byte[].class);

      if (!verify(
          handle,
          latin1TestString,
          expectedLatin1Array,
          nonLatin1TestString,
          expectedNonLatin1Array)) {
        return null;
      }
      return handle;
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * Verifies that the given handle correctly retrieves the internal byte arrays of the default test
   * strings.
   *
   * @param handle the {@link VarHandle} to verify
   * @return {@code true} if both Latin-1 and non-Latin-1 verifications pass
   */
  static boolean verify(VarHandle handle) {
    return verify(
        handle,
        LATIN1_TEST_STRING,
        EXPECTED_LATIN1_ARRAY,
        NON_LATIN1_TEST_STRING,
        EXPECTED_NON_LATIN1_ARRAY);
  }

  /**
   * Verifies that the given handle retrieves byte arrays matching the expected values for the
   * provided test strings.
   *
   * @param handle the {@link VarHandle} to verify
   * @param latin1TestString a Latin-1 encoded test string
   * @param expectedLatin1Array the expected byte array for the Latin-1 test string
   * @param nonLatin1TestString a non-Latin-1 encoded test string
   * @param expectedNonLatin1Array the expected byte array for the non-Latin-1 test string
   * @return {@code true} if both verifications pass, {@code false} otherwise
   */
  static boolean verify(
      VarHandle handle,
      String latin1TestString,
      byte[] expectedLatin1Array,
      String nonLatin1TestString,
      byte[] expectedNonLatin1Array) {
    // Verify with a Latin-1 only string
    byte[] latin1Array = (byte[]) handle.get(latin1TestString);
    if (!Arrays.equals(latin1Array, expectedLatin1Array)) {
      return false;
    }

    // Verify with a non-Latin-1 string
    byte[] nonLatin1Array = (byte[]) handle.get(nonLatin1TestString);
    if (!Arrays.equals(nonLatin1Array, expectedNonLatin1Array)) {
      return false;
    }
    return true;
  }

  /**
   * Returns the internal byte array of the given string using the statically resolved handle.
   *
   * @param s the string whose internal array to retrieve
   * @return the internal byte array, or {@code null} if the handle is unavailable
   */
  static byte[] getInternalArray(String s) {
    return getInternalArray(s, VALUE_HANDLE);
  }

  /**
   * Returns the internal byte array of the given string using the provided handle.
   *
   * @param s the string whose internal array to retrieve
   * @param handle the {@link VarHandle} to use, or {@code null} if unavailable
   * @return the internal byte array, or {@code null} if the handle is {@code null}
   */
  static byte[] getInternalArray(String s, VarHandle handle) {
    if (handle != null) {
      return (byte[]) handle.get(s);
    }
    return null;
  }
}
