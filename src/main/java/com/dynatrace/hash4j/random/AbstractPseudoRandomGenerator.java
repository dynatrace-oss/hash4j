/*
 * Copyright 2022 Dynatrace LLC
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
package com.dynatrace.hash4j.random;

abstract class AbstractPseudoRandomGenerator implements PseudoRandomGenerator {

  @Override
  public int nextInt() {
    return (int) nextLong();
  }

  // see algorithm 5 with L=32 in Lemire, Daniel. "Fast random integer generation in an interval."
  // ACM Transactions on Modeling and Computer Simulation (TOMACS) 29.1 (2019): 1-12.
  @Override
  public int uniformInt(int exclusiveUpperBound) {
    long s = exclusiveUpperBound;
    long x = nextInt() & 0xFFFFFFFFL;
    long m = x * s; // is always positive as 0 <= s < 2^31 and 0 <= x < 2^32 => 0 <= m < 2^63
    long l = m & 0xFFFFFFFFL;
    if (l < s) {
      long t = 0x100000000L % s;
      while (l < t) {
        x = nextInt() & 0xFFFFFFFFL;
        m = x * s; // is always positive as 0 <= s < 2^31 and 0 <= x < 2^32 => 0 <= m < 2^63
        l = m & 0xFFFFFFFFL;
      }
    }
    return (int) (m >>> 32);
  }
}
