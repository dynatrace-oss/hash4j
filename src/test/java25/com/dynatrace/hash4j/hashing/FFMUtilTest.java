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

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.lang.foreign.MemorySegment;
import java.util.SplittableRandom;
import org.junit.jupiter.api.Test;

class FFMUtilTest {

  @Test
  void testMemorySegmentByteAccessAgainstNativeByteArrayAccess() {
    SplittableRandom random = new SplittableRandom(0x35b0e4fd0c6cf9dfL);
    int size = 100;
    byte[] array = new byte[size];
    random.nextBytes(array);

    MemorySegment memorySegment = MemorySegment.ofArray(array);

    ByteAccess<MemorySegment> memorySegmentByteAccess =
        ByteAccess.forMemorySegment(MemorySegment.class);
    ByteAccess<byte[]> arrayByteAccess = ByteAccess.forByteArray();

    for (int pos = 0; pos <= size - 8; ++pos) {
      assertThat(arrayByteAccess.getLong(array, pos))
          .isEqualTo(memorySegmentByteAccess.getLong(memorySegment, pos));
    }
    for (int pos = 0; pos <= size - 4; ++pos) {
      assertThat(arrayByteAccess.getInt(array, pos))
          .isEqualTo(memorySegmentByteAccess.getInt(memorySegment, pos));
    }
    for (int pos = 0; pos <= size - 4; ++pos) {
      assertThat(arrayByteAccess.getIntAsUnsignedLong(array, pos))
          .isEqualTo(memorySegmentByteAccess.getIntAsUnsignedLong(memorySegment, pos));
    }
    for (int pos = 0; pos < size; ++pos) {
      assertThat(arrayByteAccess.getByte(array, pos))
          .isEqualTo(memorySegmentByteAccess.getByte(memorySegment, pos));
    }
    for (int pos = 0; pos < size; ++pos) {
      assertThat(arrayByteAccess.getByteAsUnsignedInt(array, pos))
          .isEqualTo(memorySegmentByteAccess.getByteAsUnsignedInt(memorySegment, pos));
    }
    for (int pos = 0; pos < size; ++pos) {
      assertThat(arrayByteAccess.getByteAsUnsignedLong(array, pos))
          .isEqualTo(memorySegmentByteAccess.getByteAsUnsignedLong(memorySegment, pos));
    }

    byte[] arrayCopy = new byte[size];
    byte[] memorySegmentCopy = new byte[size];
    for (int len = 0; len <= size; ++len) {
      for (long posFrom = 0; posFrom <= size - len; ++posFrom) {
        for (int posTo = 0; posTo <= size - len; ++posTo) {
          arrayByteAccess.copyToByteArray(array, posFrom, arrayCopy, posTo, len);
          memorySegmentByteAccess.copyToByteArray(
              memorySegment, posFrom, memorySegmentCopy, posTo, len);
          assertThat(arrayCopy).isEqualTo(memorySegmentCopy);
        }
      }
    }
  }
}
