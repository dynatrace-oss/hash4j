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
package com.dynatrace.hash4j.hashing;

import net.openhft.hashing.LongHashFunction;

public class XXH3_64ZeroAllocationHashingPerformanceTest
    extends AbstractZeroAllocationHashing64BitPerformanceTest {

  private static final LongHashFunction HASH_FUNCTION = LongHashFunction.xx3();

  @Override
  protected LongHashFunction createHashFunction() {
    return HASH_FUNCTION;
  }
}
