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
package com.dynatrace.hash4j.random;

/**
 * A {@link PseudoRandomGenerator} that counts the number of consumed random {@code long} values.
 */
public class CountingPseudoRandomGenerator extends AbstractPseudoRandomGenerator {

  private final PseudoRandomGenerator pseudoRandomGenerator;
  private long count;

  public CountingPseudoRandomGenerator(PseudoRandomGenerator pseudoRandomGenerator) {
    this.pseudoRandomGenerator = pseudoRandomGenerator;
    this.count = 0;
  }

  @Override
  public long nextLong() {
    count += 1;
    return pseudoRandomGenerator.nextLong();
  }

  @Override
  public PseudoRandomGenerator reset(long seed) {
    count = 0;
    pseudoRandomGenerator.reset(seed);
    return this;
  }

  public long getCount() {
    return count;
  }
}
