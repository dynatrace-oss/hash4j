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

import static com.dynatrace.hash4j.internal.UnsignedMultiplyUtil.unsignedMultiplyHigh;

import java.util.Arrays;

final class HashUtil {

  private HashUtil() {}

  static boolean equalsHelper(HashStream hashStream, Object obj) {
    if (hashStream == obj) return true;
    if (obj == null) return false;
    if (hashStream.getClass() != obj.getClass()) return false;
    HashStream that = (HashStream) obj;
    return hashStream.getHasher().equals(that.getHasher())
        && Arrays.equals(hashStream.getState(), that.getState());
  }

  static long mix(long a, long b) {
    long x = a * b;
    long y = unsignedMultiplyHigh(a, b);
    return x ^ y;
  }

  static int putCharsUTF8(HashStream stream, CharSequence c) {
    int pos = 0;
    int len = c.length();
    while (pos < len) {
      // ascii fast loop
      char ch = c.charAt(pos);
      if (ch >= 0x80) {
        break;
      }
      stream.putByte((byte) ch);
      pos++;
    }
    int charCount = pos;
    while (pos < len) {
      char ch = c.charAt(pos++);
      if (ch < 0x80) {
        stream.putByte((byte) ch);
      } else if (ch < 0x800) {
        stream.putChar((char) (0x80c0 | (((ch >>> 6) | (ch << 8)) & 0x3fff)));
      } else if (ch >= 0xd800 && ch < 0xe000) {
        int uc = 0xfca02400;
        if (ch < 0xdc00 && pos < len) {
          char ch2 = c.charAt(pos);
          if (ch2 >= 0xdc00 && ch2 < 0xe000) {
            uc += (ch << 10) + ch2;
          }
        }
        if (uc < 0) {
          stream.putByte((byte) '?');
        } else {
          stream.putInt(
              0x808080f0
                  | (uc >>> 18)
                  | ((uc >>> 4) & 0x3f00)
                  | ((uc << 10) & 0x3f0000)
                  | ((uc << 24) & 0x3f000000));
          pos++; // 2 chars
        }
      } else {
        stream.putByte((byte) (0xe0 | (ch >>> 12)));
        stream.putChar((char) ((0x8080 | ((ch >>> 6) & 0x3f)) | ((ch << 8) & 0x3fff)));
      }
      charCount++;
    }
    return charCount;
  }
}
