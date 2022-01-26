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
package com.dynatrace.hashlib.hashing;

import java.util.Arrays;
import java.util.Objects;

public class TestHashSink extends AbstractHashSink {
  private int size = 0;
  private byte[] data = new byte[1];

  @Override
  public HashSink putByte(byte v) {
    if (size == data.length) {
      data = Arrays.copyOf(data, data.length * 2);
    }
    data[size] = (byte) v;
    size += 1;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    TestHashSink that = (TestHashSink) o;
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
