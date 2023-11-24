/*
 * Copyright 2023 Dynatrace LLC
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
 * This file includes a modified version of the consistentHash method
 * (see https://github.com/google/guava/blob/0a17f4a429323589396c38d8ce75ca058faa6c64/guava/src/com/google/common/hash/Hashing.java#L559)
 * from Google's Guava library which was published with following copyright notes and under the
 * license given below:
 *
 *
 * Copyright (C) 2011 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.dynatrace.hash4j.consistent;

import static com.dynatrace.hash4j.util.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

import com.dynatrace.hash4j.random.PseudoRandomGenerator;
import com.dynatrace.hash4j.random.PseudoRandomGeneratorProvider;

class ConsistentJumpBucketHasher implements ConsistentBucketHasher {

  private final PseudoRandomGenerator pseudoRandomGenerator;

  ConsistentJumpBucketHasher(PseudoRandomGeneratorProvider pseudoRandomGeneratorProvider) {
    requireNonNull(pseudoRandomGeneratorProvider);
    this.pseudoRandomGenerator = pseudoRandomGeneratorProvider.create();
  }

  // based on Google's Guava implementation
  // see
  // https://github.com/google/guava/blob/0a17f4a429323589396c38d8ce75ca058faa6c64/guava/src/com/google/common/hash/Hashing.java#L559
  @Override
  public strictfp int getBucket(long hash, int numBuckets) {
    checkArgument(numBuckets > 0, "buckets must be positive");
    pseudoRandomGenerator.reset(hash);

    int candidate = 0;
    int next;

    // Jump from bucket to bucket until we go out of range
    while (true) {
      next = (int) ((candidate + 1) / pseudoRandomGenerator.nextDouble());
      if (next >= numBuckets || next <= candidate)
        return candidate; // second condition protects against infinite loops caused by bad random
      // values such as NaN or values outside of [0,1)
      candidate = next;
    }
  }
}
