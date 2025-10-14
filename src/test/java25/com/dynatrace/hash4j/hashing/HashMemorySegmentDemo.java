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
package com.dynatrace.hash4j.hashing;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.foreign.MemorySegment;
import org.junit.jupiter.api.Test;

class HashMemorySegmentDemo {

  @Test
  void testHashMemorySegment() {

    var byteAccess = ByteAccess.forMemorySegment(MemorySegment.class); // can be static

    MemorySegment memorySegment =
        MemorySegment.ofArray(new double[] {1., 2., 3.}); // some memory segment

    // hash all bytes of the memory segment
    long hash =
        Hashing.komihash5_0()
            .hashBytesToLong(memorySegment, 0, memorySegment.byteSize(), byteAccess);

    assertThat(hash).isEqualTo(0xc4ddb4cfd71dca0bL);
  }
}
