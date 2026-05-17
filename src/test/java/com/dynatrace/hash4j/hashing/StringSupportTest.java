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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import org.junit.jupiter.api.Test;

class StringSupportTest {

  @Test
  void testGetInternalArrayLatin1String() {
    String latin1 = "Hello, World!123";
    byte[] array = StringSupport.getInternalArray(latin1);
    assertThat(array).isNotNull();
    assertThat(array.length).isEqualTo(latin1.length());
  }

  @Test
  void testGetInternalArrayNonLatin1String() {
    String nonLatin1 =
        "\u0410\u0411\u0412\u0413\u0414\u0415\u0416\u0417"
            + "\u0418\u0419\u041A\u041B\u041C\u041D\u041E\u041F";
    byte[] array = StringSupport.getInternalArray(nonLatin1);
    assertThat(array).isNotNull();
    assertThat(array.length).isEqualTo(2 * nonLatin1.length());
  }

  @Test
  void testGetInternalArrayEmptyString() {
    byte[] array = StringSupport.getInternalArray("");
    assertThat(array).isNotNull();
    assertThat(array.length).isEqualTo(0);
  }

  @Test
  void testGetInternalArrayReturnsSameReference() {
    String s = "test string data";
    byte[] array1 = StringSupport.getInternalArray(s);
    byte[] array2 = StringSupport.getInternalArray(s);
    assertThat(array1).isSameAs(array2);
  }

  @Test
  void testFindStringValueHandleWithStringClass() {
    VarHandle result = StringSupport.findStringValueHandle(String.class);
    assertThat(result).isNotNull();
  }

  @Test
  void testFindStringValueHandleWithNoByteArrayFields() {
    VarHandle result = StringSupport.findStringValueHandle(NoByteArrayFields.class);
    assertThat(result).isNull();
  }

  @Test
  void testFindStringValueHandleWithMultipleByteArrayFields() {
    VarHandle result = StringSupport.findStringValueHandle(MultipleByteArrayFields.class);
    assertThat(result).isNull();
  }

  @Test
  void testFindStringValueHandleWithSingleByteArrayField() {
    // SingleByteArrayField has one byte[] field but verify will fail
    VarHandle result = StringSupport.findStringValueHandle(SingleByteArrayField.class);
    assertThat(result).isNull();
  }

  @Test
  void testVerifyWithValidHandle() throws Exception {
    MethodHandles.Lookup lookup =
        MethodHandles.privateLookupIn(String.class, MethodHandles.lookup());
    VarHandle handle = lookup.findVarHandle(String.class, "value", byte[].class);
    assertThat(StringSupport.verify(handle)).isTrue();
  }

  @Test
  void testVerifyWithWrongHandle() throws Exception {
    // A VarHandle for a different class's byte[] field — will throw ClassCastException
    MethodHandles.Lookup lookup =
        MethodHandles.privateLookupIn(SingleByteArrayField.class, MethodHandles.lookup());
    VarHandle handle = lookup.findVarHandle(SingleByteArrayField.class, "data", byte[].class);
    assertThatThrownBy(() -> StringSupport.verify(handle)).isInstanceOf(ClassCastException.class);
  }

  @Test
  void testVerifyLatin1Mismatch() throws Exception {
    MethodHandles.Lookup lookup =
        MethodHandles.privateLookupIn(String.class, MethodHandles.lookup());
    VarHandle handle = lookup.findVarHandle(String.class, "value", byte[].class);
    // Pass wrong expected latin1 array
    assertThat(
            StringSupport.verify(
                handle,
                "aB3$xZ7!mQ9@wP1&",
                new byte[] {0x00, 0x01, 0x02},
                "\u0410\u4E2D",
                new byte[] {0x10, 0x04, 0x2D, 0x4E}))
        .isFalse();
  }

  @Test
  void testVerifyNonLatin1Mismatch() throws Exception {
    MethodHandles.Lookup lookup =
        MethodHandles.privateLookupIn(String.class, MethodHandles.lookup());
    VarHandle handle = lookup.findVarHandle(String.class, "value", byte[].class);
    // Pass correct latin1 expected but wrong non-latin1 expected
    assertThat(
            StringSupport.verify(
                handle,
                "aB3$xZ7!mQ9@wP1&",
                new byte[] {
                  0x61, 0x42, 0x33, 0x24, 0x78, 0x5A, 0x37, 0x21,
                  0x6D, 0x51, 0x39, 0x40, 0x77, 0x50, 0x31, 0x26
                },
                "\u0410\u4E2D",
                new byte[] {0x00, 0x01, 0x02, 0x03}))
        .isFalse();
  }

  @Test
  void testFindStringValueHandleWithNullClass() {
    VarHandle result = StringSupport.findStringValueHandle(null);
    assertThat(result).isNull();
  }

  @Test
  void testFindStringValueHandleVerifyReturnsFalse() {
    // Pass String.class but with wrong expected arrays so verify returns false
    VarHandle result =
        StringSupport.findStringValueHandle(
            String.class,
            "aB3$xZ7!mQ9@wP1&",
            new byte[] {0x00, 0x01, 0x02},
            "\u0410\u4E2D",
            new byte[] {0x10, 0x04, 0x2D, 0x4E});
    assertThat(result).isNull();
  }

  @Test
  void testGetInternalArrayWithNullHandle() {
    byte[] result = StringSupport.getInternalArray("test", null);
    assertThat(result).isNull();
  }

  @Test
  void testFindStringValueHandleWithNonByteArrayField() {
    // Class has a single non-static array field of type int[] (not byte[])
    VarHandle result = StringSupport.findStringValueHandle(NonByteArrayField.class);
    assertThat(result).isNull();
  }

  @Test
  void testFindStringValueHandleWithStaticArrayFieldsOnly() {
    // Class has static array fields but no non-static array fields
    VarHandle result = StringSupport.findStringValueHandle(StaticArrayFieldsOnly.class);
    assertThat(result).isNull();
  }

  @Test
  void testFindStringValueHandleWithMixedArrayTypes() {
    // Class has two non-static array fields of different types (int[] and byte[])
    VarHandle result = StringSupport.findStringValueHandle(MixedArrayTypes.class);
    assertThat(result).isNull();
  }

  @Test
  void testFindStringValueHandleWithNonArrayNonStaticFields() {
    // Class has non-static fields that are not arrays, plus static array fields
    VarHandle result = StringSupport.findStringValueHandle(NonArrayFieldsWithStaticArrays.class);
    assertThat(result).isNull();
  }

  @Test
  void testPrivateConstructor() throws Exception {
    // Ensure the private constructor is accessible (for coverage of the utility class pattern)
    java.lang.reflect.Constructor<StringSupport> constructor =
        StringSupport.class.getDeclaredConstructor();
    assertThat(java.lang.reflect.Modifier.isPrivate(constructor.getModifiers())).isTrue();
    constructor.setAccessible(true);
    StringSupport instance = constructor.newInstance();
    assertThat(instance).isNotNull();
  }

  @Test
  void testGetInternalArrayWithSurrogatePairs() {
    // String containing characters outside BMP (surrogate pairs in UTF-16)
    String surrogate = "\uD83D\uDE00\uD83D\uDE01\uD83D\uDE02\uD83D\uDE03";
    byte[] array = StringSupport.getInternalArray(surrogate);
    assertThat(array).isNotNull();
    // Each surrogate pair is 2 chars, each char is 2 bytes in UTF-16 encoding
    assertThat(array.length).isEqualTo(2 * surrogate.length());
  }

  @Test
  void testGetInternalArraySingleCharLatin1() {
    byte[] array = StringSupport.getInternalArray("A");
    assertThat(array).isNotNull();
    assertThat(array).containsExactly((byte) 0x41);
  }

  @Test
  void testGetInternalArraySingleCharNonLatin1() {
    byte[] array = StringSupport.getInternalArray("\u0410");
    assertThat(array).isNotNull();
    // UTF-16LE encoding of U+0410: 0x10, 0x04
    assertThat(array).containsExactly((byte) 0x10, (byte) 0x04);
  }

  @SuppressWarnings("unused")
  private static class NoByteArrayFields {
    private int x;
    private String name;
  }

  @SuppressWarnings("unused")
  private static class MultipleByteArrayFields {
    private byte[] data;
    private byte[] other;
  }

  @SuppressWarnings("unused")
  static class SingleByteArrayField {
    byte[] data;
  }

  @SuppressWarnings("unused")
  private static class NonByteArrayField {
    private int[] values;
  }

  @SuppressWarnings("unused")
  private static class StaticArrayFieldsOnly {
    private static byte[] staticData = new byte[0];
    private int count;
  }

  @SuppressWarnings("unused")
  private static class MixedArrayTypes {
    private int[] numbers;
    private byte[] data;
  }

  @SuppressWarnings("unused")
  private static class NonArrayFieldsWithStaticArrays {
    private static byte[] staticArray = new byte[0];
    private int x;
    private String name;
  }
}
