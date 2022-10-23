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

abstract class AbstractHasher128 extends AbstractHasher64 implements Hasher128 {

  @Override
  public <T> HashValue128 hashTo128Bits(T data, HashFunnel<T> funnel) {
    return hashStream().put(data, funnel).get();
  }

  @Override
  public HashValue128 hashBytesTo128Bits(byte[] input) {
    return hashBytesTo128Bits(input, 0, input.length);
  }

  @Override
  public long hashBytesToLong(byte[] input, int off, int len) {
    return hashBytesTo128Bits(input, off, len).getAsLong();
  }

  @Override
  public long hashCharsToLong(CharSequence input) {
    return hashCharsTo128Bits(input).getAsLong();
  }
}
