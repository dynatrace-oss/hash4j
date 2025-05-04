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

/*
 * The implementation was partially derived from
 *
 * https://github.com/anchorhash/cpp-anchorhash
 *
 * which was published under the following license:
 *
 *
 * MIT License
 *
 * Copyright (c) 2020 anchorhash
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.dynatrace.hash4j.consistent;

import static com.dynatrace.hash4j.internal.ArraySizeUtil.increaseArraySize;
import static com.dynatrace.hash4j.internal.ByteArrayUtil.getInt;
import static com.dynatrace.hash4j.internal.ByteArrayUtil.setInt;
import static com.dynatrace.hash4j.internal.EmptyArray.EMPTY_INT_ARRAY;
import static com.dynatrace.hash4j.internal.EmptyArray.EMPTY_LONG_ARRAY;
import static java.util.Objects.requireNonNull;

import com.dynatrace.hash4j.random.PseudoRandomGenerator;
import com.dynatrace.hash4j.random.PseudoRandomGeneratorProvider;
import java.util.Arrays;

class ConsistentJumpBackAnchorBucketSetHasher implements ConsistentBucketSetHasher {

  private static final String ILLEGAL_STATE_EXCEPTION_MESSAGE = "Illegal state!";
  private static final String NO_BUCKETS_AVAILABLE_EXCEPTION_MESSAGE = "No buckets available!";

  private int historicMaxNumBuckets = 0; // corresponds to "a" in the paper
  private int numRemovedBuckets = 0;
  private int[] removedBuckets; // stack of removed buckets

  /**
   * Stores (k[i] - i) in the most significant 4 bytes and a[i] in the least significant 4 bytes,
   * where k and a correspond to the arrays used by the <a
   * href="https://github.com/anchorhash/cpp-anchorhash/blob/3ef98f05cbfe1a449f92b97cdfb1363317db85e1/mem/README.md">memory-optimized
   * implementation of AnchorHash</a>. By storing (k[i] - i) instead of k[i] the initial values will
   * be zero. The data-locality is improved by replacing integer arrays a and k by a combined long
   * array.
   */
  private long ka[];

  private final PseudoRandomGenerator pseudoRandomGenerator;

  ConsistentJumpBackAnchorBucketSetHasher(
      PseudoRandomGeneratorProvider pseudoRandomGeneratorProvider) {
    requireNonNull(pseudoRandomGeneratorProvider);
    this.pseudoRandomGenerator = pseudoRandomGeneratorProvider.create();
    this.removedBuckets = EMPTY_INT_ARRAY;
    this.ka = EMPTY_LONG_ARRAY;
  }

  @Override
  public int addBucket() {
    final int b;
    if (numRemovedBuckets <= 0) {
      b = historicMaxNumBuckets;
      historicMaxNumBuckets += 1;
      if (historicMaxNumBuckets < removedBuckets.length) removedBuckets[historicMaxNumBuckets] = 0;
    } else {
      numRemovedBuckets -= 1;
      b = removedBuckets[numRemovedBuckets];
      ka[b] = 0;
    }
    return b;
  }

  @Override
  public boolean removeBucket(int b) {
    if (b < 0 || b >= historicMaxNumBuckets || isRemoved(b)) return false;
    if (historicMaxNumBuckets - 1 == numRemovedBuckets) {
      historicMaxNumBuckets = 0;
      numRemovedBuckets = 0;
      removedBuckets = EMPTY_INT_ARRAY;
      ka = EMPTY_LONG_ARRAY;
    } else if (b == historicMaxNumBuckets - 1 && numRemovedBuckets == 0) {
      historicMaxNumBuckets -= 1;
    } else {
      if (removedBuckets.length <= numRemovedBuckets) {
        removedBuckets =
            Arrays.copyOf(
                removedBuckets,
                Math.min(
                    historicMaxNumBuckets - 1,
                    increaseArraySize(removedBuckets.length, numRemovedBuckets)));
      }
      removedBuckets[numRemovedBuckets] = b;
      numRemovedBuckets += 1;
      int n = historicMaxNumBuckets - numRemovedBuckets;
      int h = bucketAtView(n, n + 1, null);
      if (ka.length <= b) {
        ka = Arrays.copyOf(ka, Math.min(historicMaxNumBuckets, increaseArraySize(ka.length, b)));
      }
      ka[b] = ((long) (h - b) << 32) | (0xFFFFFFFFL & n);
    }
    return true;
  }

  private boolean isRemoved(int b) {
    return b < ka.length && ka[b] != 0;
  }

  @Override
  public int getBucket(long hash) {
    return getBucket(hash, null);
  }

  static class Debug {
    int counter;
  }

  int getBucket(long hash, Debug debug) {
    if (historicMaxNumBuckets <= numRemovedBuckets) {
      throw new IllegalStateException(NO_BUCKETS_AVAILABLE_EXCEPTION_MESSAGE);
    }
    if (historicMaxNumBuckets <= 1) return 0;
    pseudoRandomGenerator.reset(hash);
    int b = ConsistentJumpBackBucketHasher.getBucket(historicMaxNumBuckets, pseudoRandomGenerator);
    while (b < ka.length) {
      int ab = (int) ka[b];
      if (ab == 0) break;
      b = bucketAtView(pseudoRandomGenerator.uniformInt(ab), ab, debug);
    }
    return b;
  }

  @Override
  public int[] getBuckets() {
    int[] result = new int[getNumBuckets()];
    int pos = 0;
    for (int b = 0; b < historicMaxNumBuckets; ++b) {
      if (!isRemoved(b)) {
        result[pos] = b;
        pos += 1;
      }
    }
    return result;
  }

  @Override
  public int getNumBuckets() {
    return historicMaxNumBuckets - numRemovedBuckets;
  }

  @Override
  public byte[] getState() {
    int serializationSize = Math.toIntExact(4L + 4L * numRemovedBuckets);
    byte[] result = new byte[serializationSize];
    setInt(result, 0, historicMaxNumBuckets);
    for (int r = 0, pos = 4; r < numRemovedBuckets; r += 1, pos += 4) {
      int b = removedBuckets[r];
      setInt(result, pos, b);
    }
    return result;
  }

  @Override
  public ConsistentBucketSetHasher setState(byte[] state) {
    return setState(state, null);
  }

  private int bucketAtView(int b, int v, Debug debug) {
    while (b < ka.length) {
      long kah = ka[b];
      if ((int) kah < v) break;
      b += (int) (kah >>> 32);
      if (debug != null) debug.counter += 1;
    }
    return b;
  }

  ConsistentBucketSetHasher setState(byte[] state, Debug debug) {
    requireNonNull(state);
    if (state.length < 4 || (state.length & 0x3) != 0)
      throw new IllegalArgumentException(ILLEGAL_STATE_EXCEPTION_MESSAGE);
    historicMaxNumBuckets = getInt(state, 0);
    if (historicMaxNumBuckets < 0)
      throw new IllegalArgumentException(ILLEGAL_STATE_EXCEPTION_MESSAGE);
    numRemovedBuckets = (state.length - 4) >>> 2;
    if (removedBuckets.length < numRemovedBuckets) {
      removedBuckets = new int[numRemovedBuckets];
    }

    int maxRemovedBucket = -1;
    for (int r = 0, pos = 4; r < numRemovedBuckets; r += 1, pos += 4) {
      int b = getInt(state, pos);
      if (b < 0 || b >= historicMaxNumBuckets) {
        throw new IllegalArgumentException(ILLEGAL_STATE_EXCEPTION_MESSAGE);
      }
      removedBuckets[r] = b;
      if (maxRemovedBucket < b) maxRemovedBucket = b;
    }

    if (ka.length <= maxRemovedBucket) {
      ka = new long[maxRemovedBucket + 1];
    } else {
      Arrays.fill(ka, 0);
    }

    for (int r = 0; r < numRemovedBuckets; r += 1) {
      int b = removedBuckets[r];
      if (ka[b] != 0) throw new IllegalArgumentException(ILLEGAL_STATE_EXCEPTION_MESSAGE);
      int n = historicMaxNumBuckets - 1 - r;
      int h = bucketAtView(n, n + 1, debug);
      ka[b] = ((long) (h - b) << 32) | (0xFFFFFFFFL & n);
    }
    return this;
  }
}
