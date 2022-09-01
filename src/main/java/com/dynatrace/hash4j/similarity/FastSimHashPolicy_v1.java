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
package com.dynatrace.hash4j.similarity;

import static com.dynatrace.hash4j.util.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

import com.dynatrace.hash4j.random.PseudoRandomGenerator;
import com.dynatrace.hash4j.random.PseudoRandomGeneratorProvider;
import java.util.Arrays;

/** Used for test and comparison purposes only. */
final class FastSimHashPolicy_v1 extends AbstractSimilarityHashPolicy {

  public FastSimHashPolicy_v1(
      int numberOfComponents, PseudoRandomGeneratorProvider pseudoRandomGeneratorProvider) {
    super(numberOfComponents, 1, pseudoRandomGeneratorProvider);
  }

  @Override
  public SimilarityHasher createHasher() {
    return new Hasher();
  }

  // visible for testing
  static long calculateBulkMask(int bulkConstant) {
    long mask = 1;
    for (int i = 0; i < bulkConstant; ++i) {
      mask |= (mask << (1 << (5 - i)));
    }
    return mask;
  }

  // visible for testing
  static long calculateTemporaryCounterLimit(int bulkConstant) {
    return (1L << 1 << ((1 << (6 - bulkConstant)) - 1)) - 1;
  }

  private static final int BULK_CONSTANT =
      3; // 2^BULK_CONSTANT components are updated at once, must be in the range [0, 6]
  private static final long TEMPORARY_COUNTER_LIMIT = calculateTemporaryCounterLimit(BULK_CONSTANT);
  private static final long BULK_MASK = calculateBulkMask(BULK_CONSTANT);

  private class Hasher implements SimilarityHasher {

    private final int[] counts = new int[numberOfComponents];
    private final long[] tmpCounts =
        new long[(numberOfComponents + (63 >>> (6 - BULK_CONSTANT))) >>> BULK_CONSTANT];

    private final PseudoRandomGenerator pseudoRandomGenerator =
        pseudoRandomGeneratorProvider.create();

    public byte[] compute(ElementHashProvider elementHashProvider) {

      requireNonNull(elementHashProvider);
      int numberOfElements = elementHashProvider.getNumberOfElements();
      checkArgument(numberOfElements > 0, "Number of elements must be positive!");

      Arrays.fill(counts, 0);
      Arrays.fill(tmpCounts, 0);
      int numTmpCountChunks = tmpCounts.length >>> (6 - BULK_CONSTANT);
      int numTmpCountRemaining = tmpCounts.length & (0x3f >>> BULK_CONSTANT);

      long c = 0;
      for (int k = 0; k < numberOfElements; ++k) {
        long elementHash = elementHashProvider.getElementHash(k);

        pseudoRandomGenerator.reset(elementHash);

        for (int h = 0; h < numTmpCountChunks; ++h) {
          long randomValue = pseudoRandomGenerator.nextLong();
          for (int j = 0; j < (1 << (6 - BULK_CONSTANT)); ++j) {
            tmpCounts[(h << (6 - BULK_CONSTANT)) + j] += (randomValue >>> j) & BULK_MASK;
          }
        }
        if (numTmpCountRemaining > 0) {
          long randomValue = pseudoRandomGenerator.nextLong();
          for (int j = 0; j < numTmpCountRemaining; ++j) {
            tmpCounts[(numTmpCountChunks << (6 - BULK_CONSTANT)) + j] +=
                (randomValue >>> j) & BULK_MASK;
          }
        }
        c += 1;
        if (c == TEMPORARY_COUNTER_LIMIT || k == numberOfElements - 1) {
          // add temporary counts to final counts and reset temporary counts
          c = 0;
          for (int h = 0; h < (counts.length >>> BULK_CONSTANT); ++h) {
            long tmp = tmpCounts[h];
            tmpCounts[h] = 0;
            for (int g = 0; g < (1 << BULK_CONSTANT); ++g) {
              counts[g + (h << BULK_CONSTANT)] +=
                  (tmp >>> (g << (6 - BULK_CONSTANT))) & TEMPORARY_COUNTER_LIMIT;
            }
          }
          for (int h = (counts.length >>> BULK_CONSTANT); h < tmpCounts.length; ++h) {
            long tmp = tmpCounts[h];
            tmpCounts[h] = 0;
            for (int g = 0; g < counts.length - (h << BULK_CONSTANT); ++g) {
              counts[g + (h << BULK_CONSTANT)] +=
                  (tmp >>> (g << (6 - BULK_CONSTANT))) & TEMPORARY_COUNTER_LIMIT;
            }
          }
        }
      }

      final long limit = numberOfElements >>> 1;
      return packedArrayHandler.create(
          i -> (counts[i] + (i & (~numberOfElements & 1)) > limit) ? 1L : 0L, numberOfComponents);
    }
  }
}
