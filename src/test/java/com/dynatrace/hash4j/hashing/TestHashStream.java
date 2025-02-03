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

import java.util.Arrays;
import java.util.Objects;

class TestHashStream implements AbstractHashStream {
  private int size = 0;
  private byte[] data = new byte[1];

  @Override
  public HashStream putByte(byte v) {
    if (size == data.length) {
      data = Arrays.copyOf(data, data.length * 2);
    }
    data[size] = v;
    size += 1;
    return this;
  }

  @Override
  public HashStream reset() {
    size = 0;
    return this;
  }

  @Override
  public TestHashStream copy() {
    final TestHashStream hashStream = new TestHashStream();
    hashStream.size = size;
    System.arraycopy(data, 0, hashStream.data, 0, data.length);
    return hashStream;
  }

  @Override
  public int getHashBitSize() {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof TestHashStream)) return false;
    TestHashStream that = (TestHashStream) o;
    return size == that.size && Arrays.equals(data, that.data);
  }

  public byte[] getData() {
    return Arrays.copyOf(data, size);
  }

  @Override
  public int hashCode() {
    int result = Objects.hash(size);
    result = 31 * result + Arrays.hashCode(data);
    return result;
  }
}
