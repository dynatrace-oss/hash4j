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
package com.dynatrace.hash4j.consistent;

import org.junit.jupiter.api.Test;

class ConsistentJumpBackBucketHasherXorshiftL7R9Test extends AbstractConsistentBucketHasherTest {

  @Override
  protected ConsistentBucketHasher getConsistentBucketHasher() {
    return ConsistentHashing.jumpBackHashXorshiftL7R9();
  }

  @Override
  protected long getCheckSum() {
    return 0x4230b4d46f271c99L;
  }

  @Test
  void testZeroKey() {
    // xor-shift generators are known to have period 1 when the state is equal to 0
    // therefore make sure that we do not run into any infinite loop
    long hash = 0;
    for (int numBuckets = Integer.MAX_VALUE; numBuckets > 0; --numBuckets) {
      ConsistentHashing.jumpBackHashXorshiftL7R9().getBucket(hash, numBuckets);
    }
  }
}
