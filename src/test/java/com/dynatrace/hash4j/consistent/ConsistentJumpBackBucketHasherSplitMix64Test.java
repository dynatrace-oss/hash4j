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

class ConsistentJumpBackBucketHasherSplitMix64Test extends ConsistentJumpBackBucketHasherTest {

  static final long CHECKSUM = 0x23d7a0d288cd67e7L;

  @Override
  protected ConsistentBucketHasher getConsistentBucketHasher() {
    return ConsistentHashing.jumpBackHashSplitMix64();
  }

  @Override
  protected long getCheckSum() {
    return CHECKSUM;
  }
}
