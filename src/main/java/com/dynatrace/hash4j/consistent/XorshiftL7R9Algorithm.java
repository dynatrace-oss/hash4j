/*
 * Copyright 2025-2026 Dynatrace LLC
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

final class XorshiftL7R9Algorithm implements PseudoRandomAlgorithm64 {

  private XorshiftL7R9Algorithm() {}

  private static final XorshiftL7R9Algorithm INSTANCE = new XorshiftL7R9Algorithm();

  public static XorshiftL7R9Algorithm get() {
    return INSTANCE;
  }

  @Override
  public long nextLong(long state) {
    return state;
  }

  @Override
  public long initState(long seed) {
    return seed;
  }

  @Override
  public long updateState(long state) {
    state ^= state << 7;
    state ^= state >>> 9;
    return state;
  }
}
