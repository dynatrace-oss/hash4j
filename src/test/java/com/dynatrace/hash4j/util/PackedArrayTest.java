/*
 * Copyright 2022-2024 Dynatrace LLC
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
package com.dynatrace.hash4j.util;

import static org.assertj.core.api.Assertions.*;

import com.dynatrace.hash4j.testutils.TestUtils;
import com.dynatrace.hash4j.util.PackedArray.PackedArrayHandler;
import com.dynatrace.hash4j.util.PackedArray.PackedArrayReadIterator;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class PackedArrayTest {

  private static List<Integer> getBitSizes() {
    List<Integer> bitSizes = new ArrayList<>();
    for (int i = 0; i <= 64; ++i) {
      bitSizes.add(i);
    }
    return bitSizes;
  }

  private static void assertEquals(long[] expected, byte[] actual, PackedArrayHandler handler) {
    int bitSize = handler.getBitSize();
    long mask = (bitSize > 0) ? (0xFFFFFFFFFFFFFFFFL >>> -bitSize) : 0;
    for (int i = 0; i < expected.length; ++i) {
      long expectedValue = expected[i] & mask;
      long actualValue = handler.get(actual, i);
      assertThat(actualValue).isEqualTo(expectedValue);
    }
  }

  @ParameterizedTest
  @MethodSource("getBitSizes")
  void test(int bitSize) {
    SplittableRandom random = new SplittableRandom(0L);
    PackedArrayHandler handler = PackedArray.getHandler(bitSize);
    int maxLength = 2 * bitSize + 8;
    int numCycles1 = 50;
    int numCycles2 = 50;
    int numRandomValues = 10;
    for (int len = 1; len < maxLength; ++len) {
      long[] expected = new long[len];
      byte[] actual = handler.create(len);
      for (int c = 0; c < numCycles1; ++c) {
        for (int idx = 0; idx < len; ++idx) {
          long value = random.nextLong();
          if (random.nextBoolean()) {
            expected[idx] = value;
            long oldValue1 = handler.get(actual, idx);
            long oldValue2 = handler.set(actual, idx, value);
            assertThat(oldValue2).isEqualTo(oldValue1);
          } else {
            expected[idx] -= value;
            long oldValue1 = handler.get(actual, idx);
            long oldValue2 = handler.update(actual, idx, value, (o, n) -> o - n);
            assertThat(oldValue2).isEqualTo(oldValue1);
          }
        }
        assertEquals(expected, actual, handler);
      }
      for (int c = 0; c < numCycles2; ++c) {
        for (int j = 0; j < numRandomValues; ++j) {
          int idx = random.nextInt(len);
          long value = random.nextLong();
          if (random.nextBoolean()) {
            expected[idx] = value;
            long oldValue1 = handler.get(actual, idx);
            long oldValue2 = handler.set(actual, idx, value);
            assertThat(oldValue2).isEqualTo(oldValue1);
          } else {
            expected[idx] -= value;
            long oldValue1 = handler.get(actual, idx);
            long oldValue2 = handler.update(actual, idx, value, (o, n) -> o - n);
            assertThat(oldValue2).isEqualTo(oldValue1);
          }
        }
        assertEquals(expected, actual, handler);
      }
    }
  }

  @ParameterizedTest
  @MethodSource("getBitSizes")
  void testNumberOfEqualComponents(int bitSize) {
    SplittableRandom random = new SplittableRandom(0L);
    PackedArrayHandler handler = PackedArray.getHandler(bitSize);
    int maxLength = 200;
    int numCycles = 10;
    for (int len = 1; len < maxLength; ++len) {
      for (int c = 0; c < numCycles; ++c) {
        byte[] array1 = handler.create(len);
        byte[] array2 = handler.create(len);
        for (int i = 0; i < len; ++i) {
          long value1 = random.nextLong();
          long value2 = random.nextBoolean() ? random.nextLong() : value1;
          handler.set(array1, i, value1);
          handler.set(array2, i, value2);
        }

        int numEquals = 0;
        for (int i = 0; i < len; ++i) {
          if (handler.get(array1, i) == handler.get(array2, i)) {
            numEquals += 1;
          }
        }
        assertThat(handler.numEqualComponents(array1, array2, len))
            .describedAs("length = %d", len)
            .isEqualTo(numEquals);
      }
    }
  }

  @ParameterizedTest
  @MethodSource("getBitSizes")
  void testCreateWithValues(int bitSize) {
    PackedArrayHandler handler = PackedArray.getHandler(bitSize);
    int maxLength = 100;
    SplittableRandom random = new SplittableRandom(0x82458c58313f2d9aL);
    for (int len = 0; len < maxLength; ++len) {
      long[] values = random.longs(len).toArray();
      byte[] expected = handler.create(len);
      for (int k = 0; k < len; ++k) {
        handler.set(expected, k, values[k]);
      }
      byte[] actual = handler.create(i -> values[i], len);

      assertThat(actual).isEqualTo(expected);
    }
  }

  @ParameterizedTest
  @MethodSource("getBitSizes")
  void testByteArraySize(int bitSize) {
    if (bitSize == 0) return;
    PackedArrayHandler handler = PackedArray.getHandler(bitSize);
    int maxLength = 1000;
    for (int len = 1; len < maxLength; ++len) {
      byte[] data = handler.create(len);
      handler.set(data, len - 1, 0xFFFFFFFFFFFFFFFFL);
      assertThat(data[data.length - 1]).isNotZero();
    }
  }

  @ParameterizedTest
  @MethodSource("getBitSizes")
  void testNegativeArraySize(int bitSize) {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> PackedArray.getHandler(bitSize).create(-1));
  }

  @ParameterizedTest
  @MethodSource("getBitSizes")
  void testTooLargeArraySize(int bitSize) {
    if (bitSize >= 8) {
      assertThatIllegalArgumentException()
          .isThrownBy(
              () ->
                  PackedArray.getHandler(bitSize)
                      .create((int) (((Integer.MAX_VALUE * 8L) / bitSize) + 1)));
    }
    if (bitSize > 8) {
      assertThatIllegalArgumentException()
          .isThrownBy(() -> PackedArray.getHandler(bitSize).create(Integer.MAX_VALUE));
    }
  }

  @Test
  void testBitsUsed4() {
    PackedArrayHandler handler = PackedArray.getHandler(4);
    {
      byte[] array = handler.create(16);
      handler.set(array, 0, 0xf);
      assertThat(TestUtils.byteArrayToHexString(array)).isEqualTo("0f00000000000000");
    }
    {
      byte[] array = handler.create(16);
      handler.set(array, 1, 0xf);
      assertThat(TestUtils.byteArrayToHexString(array)).isEqualTo("f000000000000000");
    }
    {
      byte[] array = handler.create(16);
      handler.set(array, 2, 0xf);
      assertThat(TestUtils.byteArrayToHexString(array)).isEqualTo("000f000000000000");
    }
    {
      byte[] array = handler.create(16);
      handler.set(array, 3, 0xf);
      assertThat(TestUtils.byteArrayToHexString(array)).isEqualTo("00f0000000000000");
    }
    {
      byte[] array = handler.create(16);
      handler.set(array, 4, 0xf);
      assertThat(TestUtils.byteArrayToHexString(array)).isEqualTo("00000f0000000000");
    }
    {
      byte[] array = handler.create(16);
      handler.set(array, 5, 0xf);
      assertThat(TestUtils.byteArrayToHexString(array)).isEqualTo("0000f00000000000");
    }
    {
      byte[] array = handler.create(16);
      handler.set(array, 6, 0xf);
      assertThat(TestUtils.byteArrayToHexString(array)).isEqualTo("0000000f00000000");
    }
    {
      byte[] array = handler.create(16);
      handler.set(array, 7, 0xf);
      assertThat(TestUtils.byteArrayToHexString(array)).isEqualTo("000000f000000000");
    }
    {
      byte[] array = handler.create(16);
      handler.set(array, 8, 0xf);
      assertThat(TestUtils.byteArrayToHexString(array)).isEqualTo("000000000f000000");
    }
    {
      byte[] array = handler.create(16);
      handler.set(array, 9, 0xf);
      assertThat(TestUtils.byteArrayToHexString(array)).isEqualTo("00000000f0000000");
    }
    {
      byte[] array = handler.create(16);
      handler.set(array, 10, 0xf);
      assertThat(TestUtils.byteArrayToHexString(array)).isEqualTo("00000000000f0000");
    }
    {
      byte[] array = handler.create(16);
      handler.set(array, 11, 0xf);
      assertThat(TestUtils.byteArrayToHexString(array)).isEqualTo("0000000000f00000");
    }
    {
      byte[] array = handler.create(16);
      handler.set(array, 12, 0xf);
      assertThat(TestUtils.byteArrayToHexString(array)).isEqualTo("0000000000000f00");
    }
    {
      byte[] array = handler.create(16);
      handler.set(array, 13, 0xf);
      assertThat(TestUtils.byteArrayToHexString(array)).isEqualTo("000000000000f000");
    }
    {
      byte[] array = handler.create(16);
      handler.set(array, 14, 0xf);
      assertThat(TestUtils.byteArrayToHexString(array)).isEqualTo("000000000000000f");
    }
    {
      byte[] array = handler.create(16);
      handler.set(array, 15, 0xf);
      assertThat(TestUtils.byteArrayToHexString(array)).isEqualTo("00000000000000f0");
    }
  }

  @Test
  void testBitsUsed10() {
    PackedArrayHandler handler = PackedArray.getHandler(10);
    {
      byte[] array = handler.create(4);
      handler.set(array, 0, 0x3FFL);
      assertThat(TestUtils.byteArrayToHexString(array)).isEqualTo("ff03000000");
    }
    {
      byte[] array = handler.create(4);
      handler.set(array, 1, 0x3FFL);
      assertThat(TestUtils.byteArrayToHexString(array)).isEqualTo("00fc0f0000");
    }
    {
      byte[] array = handler.create(4);
      handler.set(array, 2, 0x3FFL);
      assertThat(TestUtils.byteArrayToHexString(array)).isEqualTo("0000f03f00");
    }
    {
      byte[] array = handler.create(4);
      handler.set(array, 3, 0x3FFL);
      assertThat(TestUtils.byteArrayToHexString(array)).isEqualTo("000000c0ff");
    }
    {
      byte[] array = handler.create(i -> (i == 0) ? 0x3FFL : 0L, 4);
      assertThat(TestUtils.byteArrayToHexString(array)).isEqualTo("ff03000000");
    }
    {
      byte[] array = handler.create(i -> (i == 1) ? 0x3FFL : 0L, 4);
      assertThat(TestUtils.byteArrayToHexString(array)).isEqualTo("00fc0f0000");
    }
    {
      byte[] array = handler.create(i -> (i == 2) ? 0x3FFL : 0L, 4);
      assertThat(TestUtils.byteArrayToHexString(array)).isEqualTo("0000f03f00");
    }
    {
      byte[] array = handler.create(i -> (i == 3) ? 0x3FFL : 0L, 4);
      assertThat(TestUtils.byteArrayToHexString(array)).isEqualTo("000000c0ff");
    }
  }

  @Test
  void testBitsUsed18() {
    PackedArrayHandler handler = PackedArray.getHandler(18);
    {
      byte[] array = handler.create(4);
      handler.set(array, 0, 0x3FF77L);
      assertThat(TestUtils.byteArrayToHexString(array)).isEqualTo("77ff03000000000000");
    }
    {
      byte[] array = handler.create(4);
      handler.set(array, 1, 0x3FF77L);
      assertThat(TestUtils.byteArrayToHexString(array)).isEqualTo("0000dcfd0f00000000");
    }
    {
      byte[] array = handler.create(4);
      handler.set(array, 2, 0x3FF77L);
      assertThat(TestUtils.byteArrayToHexString(array)).isEqualTo("0000000070f73f0000");
    }
    {
      byte[] array = handler.create(4);
      handler.set(array, 3, 0x3FF77L);
      assertThat(TestUtils.byteArrayToHexString(array)).isEqualTo("000000000000c0ddff");
    }
    {
      byte[] array = handler.create(i -> (i == 0) ? 0x3FF77L : 0L, 4);
      assertThat(TestUtils.byteArrayToHexString(array)).isEqualTo("77ff03000000000000");
    }
    {
      byte[] array = handler.create(i -> (i == 1) ? 0x3FF77L : 0L, 4);
      assertThat(TestUtils.byteArrayToHexString(array)).isEqualTo("0000dcfd0f00000000");
    }
    {
      byte[] array = handler.create(i -> (i == 2) ? 0x3FF77L : 0L, 4);
      assertThat(TestUtils.byteArrayToHexString(array)).isEqualTo("0000000070f73f0000");
    }
    {
      byte[] array = handler.create(i -> (i == 3) ? 0x3FF77L : 0L, 4);
      assertThat(TestUtils.byteArrayToHexString(array)).isEqualTo("000000000000c0ddff");
    }
  }

  @ParameterizedTest
  @MethodSource("getBitSizes")
  void testClear(int bitSize) {
    PackedArrayHandler handler = PackedArray.getHandler(bitSize);
    int maxLength = 200;
    for (int len = 0; len < maxLength; ++len) {
      byte[] data = handler.create(len);
      Arrays.fill(data, (byte) 0xFF);
      handler.clear(data);
      assertThat(data).isEqualTo(new byte[(bitSize * len + 7) / 8]);
    }
  }

  @ParameterizedTest
  @MethodSource("getBitSizes")
  void testNumBytes(int bitSize) {
    PackedArrayHandler handler = PackedArray.getHandler(bitSize);
    int maxLength = 200;
    for (int len = 0; len < maxLength; ++len) {
      assertThat((bitSize * len + 7) / 8).isEqualTo(handler.numBytes(len));
    }
  }

  @ParameterizedTest
  @MethodSource("getBitSizes")
  void testNullBinaryOperator(int bitSize) {
    PackedArrayHandler handler = PackedArray.getHandler(bitSize);
    byte[] data = handler.create(1);
    assertThatNullPointerException().isThrownBy(() -> handler.update(data, 0, 0, null));
  }

  @ParameterizedTest
  @MethodSource("getBitSizes")
  void testReadIterator(int bitSize) {
    PackedArrayHandler handler = PackedArray.getHandler(bitSize);
    int maxLength = 200;
    SplittableRandom random = new SplittableRandom(0L);
    for (int len = 0; len < maxLength; ++len) {
      byte[] array = handler.create(len);
      random.nextBytes(array);
      PackedArrayReadIterator iterator = handler.readIterator(array, len);
      for (int k = 0; k < len; ++k) {
        assertThat(iterator.hasNext()).isTrue();
        assertThat(iterator.next()).isEqualTo(handler.get(array, k));
      }
      assertThat(iterator.hasNext()).isFalse();
    }
  }

  @ParameterizedTest
  @MethodSource("getBitSizes")
  void testReadIteratorNullArray(int bitSize) {
    PackedArrayHandler handler = PackedArray.getHandler(bitSize);
    assertThatNullPointerException().isThrownBy(() -> handler.readIterator(null, 1));
  }
}
