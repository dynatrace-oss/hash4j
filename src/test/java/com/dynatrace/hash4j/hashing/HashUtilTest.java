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

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import com.dynatrace.hash4j.hashing.HashMocks.TestHashStream;
import java.nio.charset.StandardCharsets;
import java.util.SplittableRandom;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class HashUtilTest {

  private static char[] createCompleteCharPool() {
    char[] result = new char[1 << 16];
    for (int i = 0; i < result.length; ++i) {
      result[i] = (char) i;
    }
    return result;
  }

  /**
   * A smaller char pool containing characters around branching conditions in {@link
   * HashUtil#putCharsUTF8(HashStream, CharSequence)}. This allows to test the handling of these
   * conditions without testing all 65536 char values.
   */
  private static char[] createLimitedCharPool(int extend) {
    int[] branchPoints =
        IntStream.of(0x0, 0x80, 0x800, 0xd800, 0xe000, 0xdc00)
            .flatMap(i -> IntStream.rangeClosed(i - extend, i + extend))
            .map(x -> x & 0xffff)
            .distinct()
            .sorted()
            .toArray();
    char[] result = new char[branchPoints.length];
    for (int i = 0; i < branchPoints.length; ++i) {
      result[i] = (char) branchPoints[i];
    }
    return result;
  }

  @Test
  void testPutCharsUTF8SingleChar() {
    char[] chars = new char[1];
    CharSequence charSequence =
        new CharSequence() {
          @Override
          public int length() {
            return 1;
          }

          @Override
          public char charAt(int index) {
            return chars[index];
          }

          @Override
          public String toString() {
            return String.valueOf(chars);
          }

          @Override
          public CharSequence subSequence(int start, int end) {
            throw new UnsupportedOperationException();
          }
        };

    TestHashStream hashStream = new TestHashStream();

    char[] charPool = createCompleteCharPool();
    for (char c : charPool) {
      chars[0] = c;

      byte[] expectedBytes = charSequence.toString().getBytes(StandardCharsets.UTF_8);

      hashStream.reset();
      int numCodePoints = HashUtil.putCharsUTF8(hashStream, charSequence);
      assertThat(numCodePoints).isEqualTo(1);

      hashStream.assertData(expectedBytes, expectedBytes.length);
    }
  }

  @Test
  void testPutCharsUTF8TwoChars() {
    char[] chars = new char[2];
    CharSequence charSequence =
        new CharSequence() {
          @Override
          public int length() {
            return 2;
          }

          @Override
          public char charAt(int index) {
            return chars[index];
          }

          @Override
          public String toString() {
            return String.valueOf(chars);
          }

          @Override
          public CharSequence subSequence(int start, int end) {
            throw new UnsupportedOperationException();
          }
        };

    TestHashStream hashStream = new TestHashStream();

    char[] charPool = createLimitedCharPool(200);
    for (char c0 : charPool) {
      for (char c1 : charPool) {
        chars[0] = c0;
        chars[1] = c1;

        byte[] expectedBytes = charSequence.toString().getBytes(StandardCharsets.UTF_8);

        hashStream.reset();
        int numCodePoints = HashUtil.putCharsUTF8(hashStream, charSequence);

        assertThat(numCodePoints).isBetween(1, 2);
        hashStream.assertData(expectedBytes, expectedBytes.length);
      }
    }
  }

  @Test
  void testPutCharsUTF8FourChars() {
    char[] chars = new char[4];
    CharSequence charSequence =
        new CharSequence() {
          @Override
          public int length() {
            return 4;
          }

          @Override
          public char charAt(int index) {
            return chars[index];
          }

          @Override
          public String toString() {
            return String.valueOf(chars);
          }

          @Override
          public CharSequence subSequence(int start, int end) {
            throw new UnsupportedOperationException();
          }
        };

    TestHashStream hashStream = new TestHashStream();

    char[] charPool = createLimitedCharPool(3);
    for (char c0 : charPool) {
      for (char c1 : charPool) {
        for (char c2 : charPool) {
          for (char c3 : charPool) {

            chars[0] = c0;
            chars[1] = c1;
            chars[2] = c2;
            chars[3] = c3;

            byte[] expectedBytes = charSequence.toString().getBytes(StandardCharsets.UTF_8);

            hashStream.reset();
            int numCodePoints = HashUtil.putCharsUTF8(hashStream, charSequence);

            assertThat(numCodePoints).isBetween(2, 4);
            hashStream.assertData(expectedBytes, expectedBytes.length);
          }
        }
      }
    }
  }

  @Test
  void testRandomLatin1Strings() {

    TestHashStream hashStream = new TestHashStream();

    SplittableRandom random = new SplittableRandom(0x4392cd5b27b28a4fL);

    int numIterations = 1000;
    int maxLength = 20;

    for (int i = 0; i < numIterations; ++i) {
      int len = random.nextInt(0, maxLength + 1);

      char[] chars = new char[len];
      for (int k = 0; k < len; ++k) {
        chars[k] = (char) random.nextInt(256);
      }
      String s = String.valueOf(chars);
      byte[] expectedBytes = s.getBytes(StandardCharsets.UTF_8);

      hashStream.reset();
      int numCodePoints = HashUtil.putCharsUTF8(hashStream, s);
      assertThat(numCodePoints).isEqualTo(len);
      hashStream.assertData(expectedBytes, expectedBytes.length);
    }
  }
}
