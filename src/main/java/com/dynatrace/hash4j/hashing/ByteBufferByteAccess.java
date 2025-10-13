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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

final class ByteBufferByteAccess {

  private ByteBufferByteAccess() {}

  static ByteAccess<ByteBuffer> get(ByteOrder byteOrder) {
    if (ByteOrder.LITTLE_ENDIAN.equals(byteOrder)) {
      return ByteBufferByteAccessLittleEndian.get();
    } else if (ByteOrder.BIG_ENDIAN.equals(byteOrder)) {
      return ByteBufferByteAccessBigEndian.get();
    } else {
      throw new IllegalArgumentException(); // TODO
    }
  }

  /** An {@link ByteAccess} for a {@code ByteBuffer}. */
  static final class ByteBufferByteAccessBigEndian implements ByteAccess<ByteBuffer> {

    private ByteBufferByteAccessBigEndian() {}

    private static final ByteBufferByteAccessBigEndian INSTANCE =
        new ByteBufferByteAccessBigEndian();

    /**
     * Returns a {@link ByteBufferByteAccessBigEndian} instance.
     *
     * @return a {@link ByteBufferByteAccessBigEndian} instance
     */
    public static ByteBufferByteAccessBigEndian get() {
      return INSTANCE;
    }

    @Override
    public byte getByte(ByteBuffer data, long idx) {
      return data.get((int) idx);
    }

    @Override
    public int getByteAsUnsignedInt(ByteBuffer data, long idx) {
      return data.get((int) idx) & 0xFF;
    }

    @Override
    public long getByteAsUnsignedLong(ByteBuffer data, long idx) {
      return data.get((int) idx) & 0xFFL;
    }

    @Override
    public int getInt(ByteBuffer data, long idx) {
      return Integer.reverseBytes(data.getInt((int) idx));
    }

    @Override
    public long getIntAsUnsignedLong(ByteBuffer data, long idx) {
      return Integer.reverseBytes(data.getInt((int) idx)) & 0xFFFFFFFFL;
    }

    @Override
    public long getLong(ByteBuffer data, long idx) {
      return Long.reverseBytes(data.getLong((int) idx));
    }

    @Override
    public void copyToByteArray(ByteBuffer data, long idx, byte[] array, int off, int len) {
      ByteBufferUtil.getBigEndian(data, (int) idx, array, off, len);
    }
  }

  /** An {@link ByteAccess} for a {@code ByteBuffer}. */
  static final class ByteBufferByteAccessLittleEndian implements ByteAccess<ByteBuffer> {

    private ByteBufferByteAccessLittleEndian() {}

    private static final ByteBufferByteAccessLittleEndian INSTANCE =
        new ByteBufferByteAccessLittleEndian();

    /**
     * Returns a {@link ByteBufferByteAccessLittleEndian} instance.
     *
     * @return a {@link ByteBufferByteAccessLittleEndian} instance
     */
    public static ByteBufferByteAccessLittleEndian get() {
      return INSTANCE;
    }

    @Override
    public byte getByte(ByteBuffer data, long idx) {
      return data.get((int) idx);
    }

    @Override
    public int getByteAsUnsignedInt(ByteBuffer data, long idx) {
      return data.get((int) idx) & 0xFF;
    }

    @Override
    public long getByteAsUnsignedLong(ByteBuffer data, long idx) {
      return data.get((int) idx) & 0xFFL;
    }

    @Override
    public int getInt(ByteBuffer data, long idx) {
      return data.getInt((int) idx);
    }

    @Override
    public long getIntAsUnsignedLong(ByteBuffer data, long idx) {
      return data.getInt((int) idx) & 0xFFFFFFFFL;
    }

    @Override
    public long getLong(ByteBuffer data, long idx) {
      return data.getLong((int) idx);
    }

    @Override
    public void copyToByteArray(ByteBuffer data, long idx, byte[] array, int off, int len) {
      ByteBufferUtil.getLittleEndian(data, (int) idx, array, off, len);
    }
  }
}
