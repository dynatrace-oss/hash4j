/*
 * Copyright 2025-2026 Dynatrace LLC
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
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

final class ByteBufferUtil {

  private ByteBufferUtil() {}

  static void getLittleEndian(
      ByteBuffer byteBuffer, int index, byte[] dst, int offset, int length) {
    if (byteBuffer.hasArray()) {
      System.arraycopy(byteBuffer.array(), byteBuffer.arrayOffset() + index, dst, offset, length);
    } else {
      while (length >= 8) {
        ByteArrayUtil.setLong(dst, offset, byteBuffer.getLong(index));
        length -= 8;
        offset += 8;
        index += 8;
      }
      if (length >= 4) {
        ByteArrayUtil.setInt(dst, offset, byteBuffer.getInt(index));
        length -= 4;
        offset += 4;
        index += 4;
      }
      if (length != 0) {
        dst[offset] = byteBuffer.get(index);
        if (length != 1) {
          dst[offset + 1] = byteBuffer.get(index + 1);
          if (length != 2) {
            dst[offset + 2] = byteBuffer.get(index + 2);
          }
        }
      }
    }
  }

  private static final VarHandle LONG_HANDLE_BIG =
      MethodHandles.byteArrayViewVarHandle(long[].class, ByteOrder.BIG_ENDIAN);
  private static final VarHandle INT_HANDLE_BIG =
      MethodHandles.byteArrayViewVarHandle(int[].class, ByteOrder.BIG_ENDIAN);

  static void getBigEndian(ByteBuffer byteBuffer, int index, byte[] dst, int offset, int length) {
    if (byteBuffer.hasArray()) {
      System.arraycopy(byteBuffer.array(), byteBuffer.arrayOffset() + index, dst, offset, length);
    } else {
      while (length >= 8) {
        LONG_HANDLE_BIG.set(dst, offset, byteBuffer.getLong(index));
        length -= 8;
        offset += 8;
        index += 8;
      }
      if (length >= 4) {
        INT_HANDLE_BIG.set(dst, offset, byteBuffer.getInt(index));
        length -= 4;
        offset += 4;
        index += 4;
      }
      if (length != 0) {
        dst[offset] = byteBuffer.get(index);
        if (length != 1) {
          dst[offset + 1] = byteBuffer.get(index + 1);
          if (length != 2) {
            dst[offset + 2] = byteBuffer.get(index + 2);
          }
        }
      }
    }
  }
}
