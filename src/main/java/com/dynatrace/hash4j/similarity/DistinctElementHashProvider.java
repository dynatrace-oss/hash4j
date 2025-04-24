/*
 * Copyright 2022-2025 Dynatrace LLC
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
package com.dynatrace.hash4j.similarity;

import static com.dynatrace.hash4j.internal.EmptyArray.EMPTY_LONG_ARRAY;
import static com.dynatrace.hash4j.internal.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

import com.dynatrace.hash4j.random.PseudoRandomGenerator;
import com.dynatrace.hash4j.random.PseudoRandomGeneratorProvider;
import java.util.Arrays;

final class DistinctElementHashProvider implements ElementHashProvider {

  private static final double EXTENSION_FACTOR = 1.5;

  private static final long INITIAL_SEED = 0xfbf04e77450e9094L;

  private final PseudoRandomGenerator pseudoRandomGenerator;
  private long nullConstant;
  private long[] work;

  private int distinctCount = 0;

  public DistinctElementHashProvider(PseudoRandomGeneratorProvider pseudoRandomGeneratorProvider) {
    requireNonNull(pseudoRandomGeneratorProvider);
    this.pseudoRandomGenerator = pseudoRandomGeneratorProvider.create();
    this.pseudoRandomGenerator.reset(INITIAL_SEED);
    this.work = EMPTY_LONG_ARRAY;
    this.nullConstant = pseudoRandomGenerator.nextLong();
  }

  private static int computeWorkArrayLength(int numElements) {
    return Integer.highestOneBit((int) (numElements * EXTENSION_FACTOR)) << 1;
  }

  private static boolean contains(long[] array, int len, long key) {
    for (int i = 0; i < len; ++i) {
      if (array[i] == key) return true;
    }
    return false;
  }

  private void changeNullConstant(int workLen) {
    long newNullConstant;
    do {
      newNullConstant = pseudoRandomGenerator.nextLong();
    } while (nullConstant == newNullConstant || contains(work, workLen, newNullConstant));
    for (int i = 0; i < workLen; ++i) {
      if (work[i] == nullConstant) {
        work[i] = newNullConstant;
      }
    }
    nullConstant = newNullConstant;
  }

  public void reset(ElementHashProvider elementHashProvider) {
    int numElements = elementHashProvider.getNumberOfElements();
    checkArgument(numElements <= 0x40000000L);

    int workLen = computeWorkArrayLength(numElements);
    if (work.length < workLen) {
      work = new long[workLen];
    }
    Arrays.fill(work, 0, workLen, nullConstant);
    int workLenMinus1 = workLen - 1;

    for (int elementIdx = 0; elementIdx < numElements; ++elementIdx) {
      long hash = elementHashProvider.getElementHash(elementIdx);
      if (hash == nullConstant) {
        // very unlikely case, if this happens find a new null constant
        changeNullConstant(workLen);
      }
      int pos = (int) hash & workLenMinus1;
      while (work[pos] != nullConstant && work[pos] != hash) {
        pos = (pos + 1) & workLenMinus1;
      }
      work[pos] = hash;
    }

    distinctCount = 0;
    for (int pos = 0; pos < workLen; ++pos) {
      if (work[pos] != nullConstant) {
        work[distinctCount] = work[pos];
        distinctCount += 1;
      }
    }
  }

  @Override
  public int getNumberOfElements() {
    return distinctCount;
  }

  @Override
  public long getElementHash(int elementIndex) {
    return work[elementIndex];
  }
}
