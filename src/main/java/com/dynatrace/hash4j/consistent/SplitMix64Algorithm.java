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

final class SplitMix64Algorithm implements PseudoRandomAlgorithm64 {

  private SplitMix64Algorithm() {}

  private static final SplitMix64Algorithm INSTANCE = new SplitMix64Algorithm();

  static SplitMix64Algorithm get() {
    return INSTANCE;
  }

  @Override
  public long nextLong(long state) {
    long z = state;
    z = (z ^ (z >>> 30)) * 0xbf58476d1ce4e5b9L;
    z = (z ^ (z >>> 27)) * 0x94d049bb133111ebL;
    return z ^ (z >>> 31);
  }

  @Override
  public long initState(long seed) {
    return updateState(seed);
  }

  @Override
  public long updateState(long state) {
    return state + 0x9e3779b97f4a7c15L;
  }
}
