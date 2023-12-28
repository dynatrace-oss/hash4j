/*
 * Copyright 2022-2023 Dynatrace LLC
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

final class SimHashPolicy_v1 extends AbstractSimilarityHashPolicy {

  public SimHashPolicy_v1(
      int numberOfComponents, PseudoRandomGeneratorProvider pseudoRandomGeneratorProvider) {
    super(numberOfComponents, 1, pseudoRandomGeneratorProvider);
  }

  @Override
  public SimilarityHasher createHasher() {
    return new Hasher();
  }

  private class Hasher implements SimilarityHasher {

    private final int[] counts = new int[numberOfComponents];

    private final PseudoRandomGenerator pseudoRandomGenerator =
        pseudoRandomGeneratorProvider.create();

    @Override
    public byte[] compute(ElementHashProvider elementHashProvider) {

      requireNonNull(elementHashProvider);
      int numberOfElements = elementHashProvider.getNumberOfElements();
      checkArgument(numberOfElements > 0, "Number of elements must be positive!");

      Arrays.fill(counts, 0);

      int numChunks = numberOfComponents >>> 6;
      int numRemaining = numberOfComponents & 0x3F;

      for (int k = 0; k < numberOfElements; ++k) {

        long elementHash = elementHashProvider.getElementHash(k);
        pseudoRandomGenerator.reset(elementHash);

        for (int j = 0; j < numChunks; j++) {
          long randomValue = pseudoRandomGenerator.nextLong();
          int off = j << 6;
          for (int h = 0; h < 64; ++h) {
            counts[off + h] += (((int) (randomValue >>> h)) & 1);
          }
        }

        if (numRemaining > 0) {
          long randomValue = pseudoRandomGenerator.nextLong();
          int off = numChunks << 6;
          for (int h = 0; h < numRemaining; ++h) {
            counts[off + h] += (((int) (randomValue >>> h)) & 1);
          }
        }
      }

      final long limit = (long) (numberOfElements >>> 1);
      return packedArrayHandler.create(
          i -> (counts[i] + (i & (~numberOfElements & 1)) > limit) ? 1L : 0L, numberOfComponents);
    }
  }
}
