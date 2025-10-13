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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.SplittableRandom;
import org.junit.jupiter.api.Test;

class ByteBufferUtilTest {

  @Test
  void testByteBufferByteAccessAgainstNativeByteArrayAccess() {

    SplittableRandom random = new SplittableRandom(0xa2f244e9e199555cL);

    int size = 100;
    byte[] array = new byte[size];
    random.nextBytes(array);

    int offset = 37;
    byte[] arrayWithOffset = new byte[size + offset];
    System.arraycopy(array, 0, arrayWithOffset, offset, size);

    ByteBuffer bbBig =
        ByteBuffer.wrap(arrayWithOffset, offset, size).slice().order(ByteOrder.BIG_ENDIAN);
    ByteBuffer bbBigReadOnly = bbBig.asReadOnlyBuffer().order(ByteOrder.BIG_ENDIAN);
    ByteBuffer bbLittle =
        ByteBuffer.wrap(arrayWithOffset, offset, size).slice().order(ByteOrder.LITTLE_ENDIAN);
    ByteBuffer bbLittleReadOnly = bbLittle.asReadOnlyBuffer().order(ByteOrder.LITTLE_ENDIAN);

    assertThat(bbBig.hasArray()).isTrue();
    assertThat(bbBig.arrayOffset()).isEqualTo(offset);
    assertThat(bbBigReadOnly.hasArray()).isFalse();
    assertThat(bbLittle.hasArray()).isTrue();
    assertThat(bbLittle.arrayOffset()).isEqualTo(offset);
    assertThat(bbLittleReadOnly.hasArray()).isFalse();

    ByteAccess<ByteBuffer> bbBigByteAccess = ByteAccess.forByteBuffer(ByteOrder.BIG_ENDIAN);
    ByteAccess<ByteBuffer> bbLittleByteAccess = ByteAccess.forByteBuffer(ByteOrder.LITTLE_ENDIAN);

    ByteAccess<byte[]> arrayByteAccess = ByteAccess.forByteArray();

    for (int pos = 0; pos <= size - 8; ++pos) {
      assertThat(arrayByteAccess.getLong(array, pos))
          .isEqualTo(bbBigByteAccess.getLong(bbBig, pos))
          .isEqualTo(bbBigByteAccess.getLong(bbBigReadOnly, pos))
          .isEqualTo(bbLittleByteAccess.getLong(bbLittle, pos))
          .isEqualTo(bbLittleByteAccess.getLong(bbLittleReadOnly, pos));
    }
    for (int pos = 0; pos <= size - 4; ++pos) {
      assertThat(arrayByteAccess.getInt(array, pos))
          .isEqualTo(bbBigByteAccess.getInt(bbBig, pos))
          .isEqualTo(bbBigByteAccess.getInt(bbBigReadOnly, pos))
          .isEqualTo(bbLittleByteAccess.getInt(bbLittle, pos))
          .isEqualTo(bbLittleByteAccess.getInt(bbLittleReadOnly, pos));
    }
    for (int pos = 0; pos <= size - 4; ++pos) {
      assertThat(arrayByteAccess.getIntAsUnsignedLong(array, pos))
          .isEqualTo(bbBigByteAccess.getIntAsUnsignedLong(bbBig, pos))
          .isEqualTo(bbBigByteAccess.getIntAsUnsignedLong(bbBigReadOnly, pos))
          .isEqualTo(bbLittleByteAccess.getIntAsUnsignedLong(bbLittle, pos))
          .isEqualTo(bbLittleByteAccess.getIntAsUnsignedLong(bbLittleReadOnly, pos));
    }
    for (int pos = 0; pos < size; ++pos) {
      assertThat(arrayByteAccess.getByte(array, pos))
          .isEqualTo(bbBigByteAccess.getByte(bbBig, pos))
          .isEqualTo(bbBigByteAccess.getByte(bbBigReadOnly, pos))
          .isEqualTo(bbLittleByteAccess.getByte(bbLittle, pos))
          .isEqualTo(bbLittleByteAccess.getByte(bbLittleReadOnly, pos));
    }
    for (int pos = 0; pos < size; ++pos) {
      assertThat(arrayByteAccess.getByteAsUnsignedInt(array, pos))
          .isEqualTo(bbBigByteAccess.getByteAsUnsignedInt(bbBig, pos))
          .isEqualTo(bbBigByteAccess.getByteAsUnsignedInt(bbBigReadOnly, pos))
          .isEqualTo(bbLittleByteAccess.getByteAsUnsignedInt(bbLittle, pos))
          .isEqualTo(bbLittleByteAccess.getByteAsUnsignedInt(bbLittleReadOnly, pos));
    }
    for (int pos = 0; pos < size; ++pos) {
      assertThat(arrayByteAccess.getByteAsUnsignedLong(array, pos))
          .isEqualTo(bbBigByteAccess.getByteAsUnsignedLong(bbBig, pos))
          .isEqualTo(bbBigByteAccess.getByteAsUnsignedLong(bbBigReadOnly, pos))
          .isEqualTo(bbLittleByteAccess.getByteAsUnsignedLong(bbLittle, pos))
          .isEqualTo(bbLittleByteAccess.getByteAsUnsignedLong(bbLittleReadOnly, pos));
    }

    byte[] arrayCopy = new byte[size];
    byte[] bbBigCopy = new byte[size];
    byte[] bbBigReadOnlyCopy = new byte[size];
    byte[] bbLittleCopy = new byte[size];
    byte[] bbLittleReadOnlyCopy = new byte[size];
    for (int len = 0; len <= size; ++len) {
      for (long posFrom = 0; posFrom <= size - len; ++posFrom) {
        for (int posTo = 0; posTo <= size - len; ++posTo) {
          arrayByteAccess.copyToByteArray(array, posFrom, arrayCopy, posTo, len);
          bbBigByteAccess.copyToByteArray(bbBig, posFrom, bbBigCopy, posTo, len);
          bbBigByteAccess.copyToByteArray(bbBigReadOnly, posFrom, bbBigReadOnlyCopy, posTo, len);
          bbLittleByteAccess.copyToByteArray(bbLittle, posFrom, bbLittleCopy, posTo, len);
          bbLittleByteAccess.copyToByteArray(
              bbLittleReadOnly, posFrom, bbLittleReadOnlyCopy, posTo, len);
          assertThat(arrayCopy)
              .isEqualTo(bbBigCopy)
              .isEqualTo(bbBigReadOnlyCopy)
              .isEqualTo(bbLittleCopy)
              .isEqualTo(bbLittleReadOnlyCopy);
        }
      }
    }
  }
}
