/*
 * Copyright 2023-2024 Dynatrace LLC
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
package com.dynatrace.hash4j.consistent;

import static java.util.stream.Collectors.groupingBy;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

import com.dynatrace.hash4j.random.PseudoRandomGeneratorProvider;
import java.util.List;
import java.util.Map;
import java.util.stream.LongStream;
import org.junit.jupiter.api.Test;

class ConsistentHashingDemo {

  @Test
  void demoJumphash() {

    // create a consistent bucket hasher
    ConsistentBucketHasher consistentBucketHasher =
        ConsistentHashing.jumpBackHash(PseudoRandomGeneratorProvider.splitMix64_V1());

    long[] hashValues = {9184114998275508886L, 7090183756869893925L, -8795772374088297157L};

    // determine assignment of hash value to 2 buckets
    Map<Integer, List<Long>> assignment2Buckets =
        LongStream.of(hashValues)
            .boxed()
            .collect(groupingBy(hash -> consistentBucketHasher.getBucket(hash, 2)));
    // gives {0=[7090183756869893925, -8795772374088297157], 1=[9184114998275508886]}

    // determine assignment of hash value to 3 buckets
    Map<Integer, List<Long>> assignment3Buckets =
        LongStream.of(hashValues)
            .boxed()
            .collect(groupingBy(hash -> consistentBucketHasher.getBucket(hash, 3)));
    // gives {0=[7090183756869893925], 1=[9184114998275508886], 2=[-8795772374088297157]}
    // hash value 7090183756869893925 got reassigned from bucket 0 to bucket 2
    // probability of reassignment is equal to 1/3

    assertThat(assignment2Buckets)
        .hasToString("{0=[7090183756869893925, -8795772374088297157], 1=[9184114998275508886]}");
    assertThat(assignment3Buckets)
        .hasToString(
            "{0=[7090183756869893925], 1=[9184114998275508886], 2=[-8795772374088297157]}");
  }
}
