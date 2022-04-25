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

abstract class AbstractHasher32 implements Hasher32 {

  protected abstract HashCalculator newHashCalculator();

  @Override
  public <T> int hashToInt(T data, HashFunnel<T> funnel) {
    HashCalculator hashCalculator = newHashCalculator();
    funnel.put(data, hashCalculator);
    return hashCalculator.getAsInt();
  }

  @Override
  public int hashBytesToInt(byte[] input) {
    return hashBytesToInt(input, 0, input.length);
  }

  @Override
  public int hashBytesToInt(byte[] input, int off, int len) {
    return hashToInt(input, (b, f) -> f.putBytes(b, off, len));
  }
}
