/*
 * Copyright 2023-2025 Dynatrace LLC
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

import com.dynatrace.hash4j.random.PseudoRandomGeneratorProvider;

class ConsistentJumpBucketHasherTest extends AbstractConsistentBucketHasherTest {

  @Override
  protected ConsistentBucketHasher getConsistentBucketHasher(
      PseudoRandomGeneratorProvider pseudoRandomGeneratorProvider) {
    return ConsistentHashing.jumpHash(pseudoRandomGeneratorProvider);
  }

  @Override
  protected long getCheckSum() {
    return 0xfd5390c955b998f7L;
  }
}
