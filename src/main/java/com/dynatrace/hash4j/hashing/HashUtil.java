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
}
