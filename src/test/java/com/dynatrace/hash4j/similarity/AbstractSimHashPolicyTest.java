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

abstract class AbstractSimHashPolicyTest extends AbstractSimilarityHasherPolicyTest {

  protected static double calculateComponentCollisionProbability(double cosineSimilarity) {
    return Math.min(1., Math.max(0.5, Math.acos(-cosineSimilarity) / Math.PI));
  }

  @Override
  protected double calculateExpectedMatchProbability(
      long intersectionSize, long difference1Size, long difference2Size) {

    double expectedCosineSimilarity =
        intersectionSize
            / Math.sqrt(
                (intersectionSize + difference1Size)
                    * (double) (intersectionSize + difference2Size));

    return calculateComponentCollisionProbability(expectedCosineSimilarity);
  }

  @Override
  protected int getMaxSizeForCheckSumTest() {
    return 300;
  }
}
