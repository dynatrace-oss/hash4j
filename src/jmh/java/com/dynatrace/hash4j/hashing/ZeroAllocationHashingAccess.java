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

import static java.nio.ByteOrder.LITTLE_ENDIAN;

import com.dynatrace.hash4j.internal.ByteArrayUtil;
import java.nio.ByteOrder;
import net.openhft.hashing.Access;

public final class ZeroAllocationHashingAccess extends Access<byte[]> {

  private ZeroAllocationHashingAccess() {}

  private static final ZeroAllocationHashingAccess INSTANCE = new ZeroAllocationHashingAccess();

  public static ZeroAllocationHashingAccess get() {
    return INSTANCE;
  }

  @Override
  public int getByte(byte[] input, long offset) {
    return input[(int) offset];
  }

  @Override
  public int getUnsignedByte(byte[] input, long offset) {
    return input[(int) offset] & 0xFF;
  }

  @Override
  public long getLong(byte[] input, long offset) {
    return ByteArrayUtil.getLong(input, (int) offset);
  }

  @Override
  public int getShort(byte[] input, long offset) {
    return ByteArrayUtil.getShort(input, (int) offset);
  }

  @Override
  public int getUnsignedShort(byte[] input, long offset) {
    return ByteArrayUtil.getChar(input, (int) offset);
  }

  @Override
  public int getInt(byte[] input, long offset) {
    return ByteArrayUtil.getInt(input, (int) offset);
  }

  @Override
  public long getUnsignedInt(byte[] input, long offset) {
    return ByteArrayUtil.getInt(input, (int) offset) & 0xFFFFFFFFL;
  }

  @Override
  public ByteOrder byteOrder(byte[] input) {
    return LITTLE_ENDIAN;
  }

  @Override
  protected Access<byte[]> reverseAccess() {
    throw new UnsupportedOperationException();
  }
}
