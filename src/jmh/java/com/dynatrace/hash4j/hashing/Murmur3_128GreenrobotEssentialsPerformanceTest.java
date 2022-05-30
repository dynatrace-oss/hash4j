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

import org.greenrobot.essentials.hash.Murmur3F;

public class Murmur3_128GreenrobotEssentialsPerformanceTest extends AbstractPerformanceTest {

  @Override
  protected long hashObject(TestObject testObject) {
    throw new UnsupportedOperationException();
  }

  @Override
  protected long hashBytesDirect(byte[] b) {
    throw new UnsupportedOperationException();
  }

  @Override
  protected long hashCharsDirect(String s) {
    throw new UnsupportedOperationException();
  }

  @Override
  protected long hashBytesIndirect(byte[] b) {
    Murmur3F murmur = new Murmur3F();
    murmur.update(b);
    return murmur.getValue();
  }

  @Override
  protected long hashCharsIndirect(String s) {
    throw new UnsupportedOperationException();
  }
}
