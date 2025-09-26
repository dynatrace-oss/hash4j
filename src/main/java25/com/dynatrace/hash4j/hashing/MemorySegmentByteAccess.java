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

import static java.lang.foreign.ValueLayout.*;

import java.lang.foreign.MemorySegment;

public final class MemorySegmentByteAccess implements ByteAccess<MemorySegment> {

  private static final MemorySegmentByteAccess INSTANCE = new MemorySegmentByteAccess();

  private MemorySegmentByteAccess() {}

  public static MemorySegmentByteAccess get() {
    return INSTANCE;
  }

  @Override
  public byte getByte(MemorySegment data, long idx) {
    return data.get(JAVA_BYTE, idx);
  }

  @Override
  public void copyToByteArray(MemorySegment data, long idx, byte[] array, int off, int len) {
    MemorySegment.copy(data, JAVA_BYTE, idx, array, off, len);
  }

  @Override
  public int getByteAsUnsignedInt(MemorySegment data, long idx) {
    return data.get(JAVA_BYTE, idx) & 0XFF;
  }

  @Override
  public long getByteAsUnsignedLong(MemorySegment data, long idx) {
    return data.get(JAVA_BYTE, idx) & 0XFFL;
  }

  @Override
  public int getInt(MemorySegment data, long idx) {
    return data.get(JAVA_INT_UNALIGNED, idx);
  }

  @Override
  public long getIntAsUnsignedLong(MemorySegment data, long idx) {
    return data.get(JAVA_INT_UNALIGNED, idx) & 0xFFFFFFFFL;
  }

  @Override
  public long getLong(MemorySegment data, long idx) {
    return data.get(JAVA_LONG_UNALIGNED, idx);
  }
}
