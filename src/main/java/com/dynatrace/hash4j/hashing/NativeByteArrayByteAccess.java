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

/** An {@link ByteAccess} for a native {@code byte[]} array. */
final class NativeByteArrayByteAccess implements ByteAccess<byte[]> {

  private NativeByteArrayByteAccess() {}

  private static final NativeByteArrayByteAccess INSTANCE = new NativeByteArrayByteAccess();

  /**
   * Returns a {@link NativeByteArrayByteAccess} instance.
   *
   * @return a {@link NativeByteArrayByteAccess} instance
   */
  public static NativeByteArrayByteAccess get() {
    return INSTANCE;
  }

  @Override
  public byte getByte(byte[] data, long idx) {
    return data[(int) idx];
  }

  @Override
  public int getByteAsUnsignedInt(byte[] data, long idx) {
    return data[(int) idx] & 0xFF;
  }

  @Override
  public long getByteAsUnsignedLong(byte[] data, long idx) {
    return data[(int) idx] & 0xFFL;
  }

  @Override
  public int getInt(byte[] data, long idx) {
    return ByteArrayUtil.getInt(data, (int) idx);
  }

  @Override
  public long getIntAsUnsignedLong(byte[] data, long idx) {
    return ByteArrayUtil.getInt(data, (int) idx) & 0xFFFFFFFFL;
  }

  @Override
  public long getLong(byte[] data, long idx) {
    return ByteArrayUtil.getLong(data, (int) idx);
  }

  @Override
  public void copyToByteArray(byte[] data, long idx, byte[] array, int off, int len) {
    System.arraycopy(data, (int) idx, array, off, len);
  }
}
