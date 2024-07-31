/*
 * Copyright 2022-2024 Dynatrace LLC
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
package com.dynatrace.hash4j.hashing;

public class XXH3_64PerformanceTest extends AbstactHasher64PerformanceTest {

  private static final Hasher64 HASHER_INSTANCE = Hashing.xxh3_64();

  @Override
  protected Hasher64 getHasherInstance() {
    return HASHER_INSTANCE;
  }
}
