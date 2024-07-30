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

import com.google.common.hash.Funnel;
import com.google.common.hash.HashFunction;

public abstract class AbstractGuava64BitPerformanceTest extends AbstractPerformanceTest {

  protected static final Funnel<String> CHARS_FUNNEL = (s, sink) -> sink.putUnencodedChars(s);
  protected static final Funnel<byte[]> BYTES_FUNNEL = (s, sink) -> sink.putBytes(s);

  @Override
  protected long hashObject(TestObject testObject) {
    return createHashFunction().hashObject(testObject, TestObject::contributeToHash).asLong();
  }

  @Override
  protected long hashBytesDirect(byte[] b) {
    return createHashFunction().hashBytes(b).asLong();
  }

  @Override
  protected long hashCharsDirect(String s) {
    return createHashFunction().hashUnencodedChars(s).asLong();
  }

  @Override
  protected long hashCharsIndirect(String s) {
    return createHashFunction().hashObject(s, CHARS_FUNNEL).asLong();
  }

  @Override
  protected long hashBytesIndirect(byte[] b) {
    return createHashFunction().hashObject(b, BYTES_FUNNEL).asLong();
  }

  protected abstract HashFunction createHashFunction();
}
