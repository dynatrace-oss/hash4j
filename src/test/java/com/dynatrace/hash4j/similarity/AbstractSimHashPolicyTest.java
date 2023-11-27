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

import org.hipparchus.distribution.discrete.BinomialDistribution;

abstract class AbstractSimHashPolicyTest extends AbstractSimilarityHasherPolicyTest {

  protected static double calculateComponentCollisionProbabilityApproximately(
      long intersectionSize, long difference1Size, long difference2Size) {

    double expectedCosineSimilarity =
        intersectionSize
            / Math.sqrt(
                (intersectionSize + difference1Size)
                    * (double) (intersectionSize + difference2Size));

    return Math.min(1., Math.max(0.5, Math.acos(-expectedCosineSimilarity) / Math.PI));
  }

  private static double calculateComponentCollisionProbabilityExactly(
      long intersectionSize, long difference1Size, long difference2Size) {
    BinomialDistribution intersectionDistribution =
        new BinomialDistribution(Math.toIntExact(intersectionSize), 0.5);
    BinomialDistribution difference1Distribution =
        new BinomialDistribution(Math.toIntExact(difference1Size), 0.5);
    BinomialDistribution difference2Distribution =
        new BinomialDistribution(Math.toIntExact(difference2Size), 0.5);

    double sum = 0;
    for (long countIntersection = 0; countIntersection <= intersectionSize; ++countIntersection) {
      double probabilityIntersection =
          intersectionDistribution.probability(Math.toIntExact(countIntersection));
      for (long countDifference1 = 0; countDifference1 <= difference1Size; ++countDifference1) {
        double probabilityDifference1 =
            difference1Distribution.probability(Math.toIntExact(countDifference1));
        for (long countDifference2 = 0; countDifference2 <= difference2Size; ++countDifference2) {
          double probabilityDifference2 =
              difference2Distribution.probability(Math.toIntExact(countDifference2));

          double probability =
              probabilityIntersection * probabilityDifference1 * probabilityDifference2;

          long setSize1 = difference1Size + intersectionSize;
          long setSize2 = difference2Size + intersectionSize;
          long count1 = countDifference1 + countIntersection;
          long count2 = countDifference2 + countIntersection;

          if (count1 * 2 == setSize1 || count2 * 2 == setSize2) {
            sum += 0.5 * probability;
          } else if ((count1 * 2 > setSize1) == (count2 * 2 > setSize2)) {
            sum += probability;
          }
        }
      }
    }
    return Math.min(1., sum);
  }

  @Override
  protected double calculateExpectedMatchProbability(
      long intersectionSize, long difference1Size, long difference2Size) {

    if (intersectionSize <= 20 && difference1Size <= 20 && difference2Size <= 20) {
      return calculateComponentCollisionProbabilityExactly(
          intersectionSize, difference1Size, difference2Size);
    } else {
      return calculateComponentCollisionProbabilityApproximately(
          intersectionSize, difference1Size, difference2Size);
    }
  }

  @Override
  protected int getMaxSizeForCheckSumTest() {
    return 300;
  }
}
