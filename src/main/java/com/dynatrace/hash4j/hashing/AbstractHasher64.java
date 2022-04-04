/*
 * Copyright 2022 Dynatrace LLC
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

abstract class AbstractHasher64 extends AbstractHasher32 implements Hasher64 {

  @Override
  protected abstract HashCalculator newHashCalculator();

  @Override
  public <T> long hashToLong(T data, HashFunnel<T> funnel) {
    HashCalculator hashCalculator = newHashCalculator();
    funnel.put(data, hashCalculator);
    return hashCalculator.getAsLong();
  }

  @Override
  public long hashBytesToLong(byte[] input) {
    return hashBytesToLong(input, 0, input.length);
  }

  @Override
  public long hashBytesToLong(byte[] input, int off, int len) {
    return hashToLong(input, (b, f) -> f.putBytes(b, off, len));
  }
}
