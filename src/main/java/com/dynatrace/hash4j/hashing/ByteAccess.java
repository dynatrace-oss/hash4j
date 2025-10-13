/*
 * Copyright 2024-2025 Dynatrace LLC
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

import com.dynatrace.hash4j.internal.ByteArrayUtil;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Strategy to access contiguous bytes of a data object.
 *
 * <p>It is recommended to override the default implementations of at least {@link #getInt(Object,
 * long)} and {@link #getLong(Object, long)} for best performance.
 *
 * @param <T> the type of the data object
 */
public interface ByteAccess<T> {

  /**
   * Returns the byte at a certain position from the given data object as {@code byte}.
   *
   * @param data the data object
   * @param idx the index
   * @return the byte
   */
  byte getByte(T data, long idx);

  /**
   * Returns the byte at a certain position from the given data object as non-negative {@code int}.
   *
   * <p>This is equivalent to {@code getByte(data, idx) & 0xFF}.
   *
   * @param data the data object
   * @param idx the index
   * @return the byte as non-negative long
   */
  default int getByteAsUnsignedInt(T data, long idx) {
    return getByte(data, idx) & 0xFF;
  }

  /**
   * Returns the byte at a certain position from the given data object as non-negative {@code long}.
   *
   * <p>This is equivalent to {@code getByte(data, idx) & 0xFFL}.
   *
   * @param data the data object
   * @param idx the index
   * @return the byte as non-negative long
   */
  default long getByteAsUnsignedLong(T data, long idx) {
    return getByte(data, idx) & 0xFFL;
  }

  /**
   * Returns 4 subsequent bytes starting at a certain position from the given data object as {@code
   * int}.
   *
   * <p>This is equivalent to {@code (getByte(data, idx) & 0xFF) | ((getByte(data, idx + 1) & 0xFF)
   * << 8) | ((getByte(data, idx + 2) & 0xFF) << 16) | ((int)getByte(data, idx+3) << 24)}.
   *
   * @param data the data object
   * @param idx the index of the first byte
   * @return 4 bytes as integer
   */
  default int getInt(T data, long idx) {
    return (getByte(data, idx) & 0xFF)
        | ((getByte(data, idx + 1) & 0xFF) << 8)
        | ((getByte(data, idx + 2) & 0xFF) << 16)
        | (getByte(data, idx + 3) << 24);
  }

  /**
   * Returns 4 subsequent bytes starting at a certain position from the given data object as
   * non-negative {@code long}.
   *
   * <p>This is equivalent to {@code getInt(data, idx) & 0xFFFFFFFFL}.
   *
   * @param data the data object
   * @param idx the index of the first byte
   * @return 4 bytes as non-negative long
   */
  default long getIntAsUnsignedLong(T data, long idx) {
    return getInt(data, idx) & 0xFFFFFFFFL;
  }

  /**
   * Returns 8 subsequent bytes starting at a certain position from the given data object as {@code
   * long}.
   *
   * <p>This is equivalent to {@code getIntUnsigned(data, idx) | ((long) getInt(data, idx + 4) <<
   * 32)}.
   *
   * @param data the data object
   * @param idx the index of the first byte
   * @return 8 bytes as long
   */
  default long getLong(T data, long idx) {
    return getIntAsUnsignedLong(data, idx) | ((long) getInt(data, idx + 4) << 32);
  }

  /**
   * Copies a given number of bytes from the data object into a given {@code byte[]} array.
   *
   * <p>This is equivalent to {@code for(int i = 0; i < len; ++i) array[off + i] = getByte(data, idx
   * + i)}.
   *
   * @param data the data object
   * @param idx the index of the first byte
   * @param array the target byte array
   * @param off the target offset
   * @param len the number of bytes
   */
  default void copyToByteArray(T data, long idx, byte[] array, int off, int len) {
    while (len >= 8) {
      ByteArrayUtil.setLong(array, off, getLong(data, idx));
      len -= 8;
      off += 8;
      idx += 8;
    }
    if (len >= 4) {
      ByteArrayUtil.setInt(array, off, getInt(data, idx));
      len -= 4;
      off += 4;
      idx += 4;
    }
    if (len != 0) {
      array[off] = getByte(data, idx);
      if (len != 1) {
        array[off + 1] = getByte(data, idx + 1);
        if (len != 2) array[off + 2] = getByte(data, idx + 2);
      }
    }
  }

  /**
   * Returns a {@link ByteAccess} instance for native {@code byte[]} arrays.
   *
   * @return a {@link ByteAccess} instance
   */
  static ByteAccess<byte[]> forByteArray() {
    return ByteArrayByteAccess.get();
  }

  /**
   * Returns a {@link ByteAccess} instance for a {@code java.lang.foreign.MemorySegment} for Java
   * versions 25 and beyond.
   *
   * <p>Passing the class as argument is a workaround to make the interface compatible with older
   * Java versions not supporting {@code java.lang.foreign.MemorySegment}. The argument might get
   * dropped in future releases support only Java version 25 and beyond.
   *
   * @param clazz must be MemorySegment.class
   * @param <T> the type, must be {@code MemorySegment}
   * @return a {@link ByteAccess} instance
   * @throws UnsupportedOperationException if this function is called by Java versions smaller than
   *     25.
   * @throws IllegalArgumentException if {@code clazz} is not {@code
   *     java.lang.foreign.MemorySegment.class}.
   */
  @Generated(reason = "FFMUtil.getByteAccessForMemorySegment might throw exceptions")
  static <T> ByteAccess<T> forMemorySegment(Class<T> clazz) {
    return FFMUtil.getByteAccessForMemorySegment(clazz);
  }

  /**
   * Returns a {@link ByteAccess} instance for byte buffers with specified byte order.
   *
   * @param byteOrder the byte order, must be either {@link ByteOrder#BIG_ENDIAN} or {@link
   *     ByteOrder#LITTLE_ENDIAN}
   * @return a {@link ByteAccess} instance
   * @throws IllegalArgumentException if {@code byteOrder} is neither {@link ByteOrder#BIG_ENDIAN}
   *     nor {@link ByteOrder#LITTLE_ENDIAN}
   */
  static ByteAccess<ByteBuffer> forByteBuffer(ByteOrder byteOrder) {
    return ByteBufferByteAccess.get(byteOrder);
  }
}
