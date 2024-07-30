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

public abstract class AbstactHasher64PerformanceTest extends AbstractPerformanceTest {

  protected static final HashFunnel<CharSequence> CHARS_FUNNEL = (s, sink) -> sink.putChars(s);
  protected static final HashFunnel<byte[]> BYTES_FUNNEL = (s, sink) -> sink.putBytes(s);

  @Override
  protected long hashObject(TestObject testObject) {
    return getHasherInstance().hashToLong(testObject, TestObject::contributeToHash);
  }

  @Override
  protected long hashBytesDirect(byte[] b) {
    return getHasherInstance().hashBytesToLong(b);
  }

  @Override
  protected long hashBytesIndirect(byte[] b) {
    return getHasherInstance().hashToLong(b, BYTES_FUNNEL);
  }

  @Override
  protected long hashCharsDirect(String s) {
    return getHasherInstance().hashCharsToLong(s);
  }

  @Override
  protected long hashCharsIndirect(String s) {
    return getHasherInstance().hashToLong(s, CHARS_FUNNEL);
  }

  protected abstract Hasher64 getHasherInstance();
}
