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

import java.lang.foreign.MemorySegment;

final class HashUtilSpecific {

  private HashUtilSpecific() {}

  static void putShorts(HashStream hashStream, short[] x, int off, int len) {
    hashStream.putBytes(
        MemorySegment.ofArray(x), (long) off << 1, (long) len << 1, MemorySegmentByteAccess.get());
  }

  static void putChars(HashStream hashStream, char[] x, int off, int len) {
    hashStream.putBytes(
        MemorySegment.ofArray(x), (long) off << 1, (long) len << 1, MemorySegmentByteAccess.get());
  }

  static void putFloats(HashStream hashStream, float[] x, int off, int len) {
    hashStream.putBytes(
        MemorySegment.ofArray(x), (long) off << 2, (long) len << 2, MemorySegmentByteAccess.get());
  }

  static void putInts(HashStream hashStream, int[] x, int off, int len) {
    hashStream.putBytes(
        MemorySegment.ofArray(x), (long) off << 2, (long) len << 2, MemorySegmentByteAccess.get());
  }

  static void putLongs(HashStream hashStream, long[] x, int off, int len) {
    hashStream.putBytes(
        MemorySegment.ofArray(x), (long) off << 3, (long) len << 3, MemorySegmentByteAccess.get());
  }

  static void putDoubles(HashStream hashStream, double[] x, int off, int len) {
    hashStream.putBytes(
        MemorySegment.ofArray(x), (long) off << 3, (long) len << 3, MemorySegmentByteAccess.get());
  }
}
