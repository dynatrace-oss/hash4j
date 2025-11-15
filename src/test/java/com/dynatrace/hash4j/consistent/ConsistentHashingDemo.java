/*
 * Copyright 2023-2025 Dynatrace LLC
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

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

import com.dynatrace.hash4j.random.PseudoRandomGeneratorProvider;
import java.util.List;
import org.junit.jupiter.api.Test;

class ConsistentHashingDemo {

  @Test
  void demoJumpBackHash() {

    // list of 64-bit hash values of the keys
    List<Long> keys = asList(0x7f7487ee708c8a96L, 0x6265648fbc797f25L, 0x85ef23a0b545d53bL);

    // create a consistent bucket hasher
    var consistentBucketHasher =
        ConsistentHashing.jumpBackHash(PseudoRandomGeneratorProvider.splitMix64_V1());

    // determine mapping of keys to 2 buckets
    var mapping2 =
        keys.stream()
            .collect(
                groupingBy(
                    k -> consistentBucketHasher.getBucket(k, 2),
                    mapping(Long::toHexString, toList())));
    // gives {0=[6265648fbc797f25, 85ef23a0b545d53b], 1=[7f7487ee708c8a96]}

    // determine mapping of keys to 3 buckets
    var mapping3 =
        keys.stream()
            .collect(
                groupingBy(
                    k -> consistentBucketHasher.getBucket(k, 3),
                    mapping(Long::toHexString, toList())));
    // gives {0=[6265648fbc797f25], 1=[7f7487ee708c8a96], 2=[85ef23a0b545d53b]}
    // key 85ef23a0b545d53b got reassigned from bucket 0 to bucket 2
    // probability of reassignment is equal to 1/3

    assertThat(mapping2)
        .hasToString("{0=[6265648fbc797f25, 85ef23a0b545d53b], 1=[7f7487ee708c8a96]}");
    assertThat(mapping3)
        .hasToString("{0=[6265648fbc797f25], 1=[7f7487ee708c8a96], 2=[85ef23a0b545d53b]}");
  }

  @Test
  void demoJumpBackHashSet() {

    // list of 64-bit hash values of the keys
    List<Long> keys = asList(0x48ac502166f761a8L, 0x9b7193f97ec9cb79L, 0x6ce88bf7de8c06c2L);

    // create a consistent bucket set hasher
    var hasher =
        ConsistentHashing.jumpBackAnchorHash(PseudoRandomGeneratorProvider.splitMix64_V1());

    // add 3 buckets
    int bucket1 = hasher.addBucket(); // == 0
    int bucket2 = hasher.addBucket(); // == 1
    int bucket3 = hasher.addBucket(); // == 2

    // determine mapping of keys to the 3 buckets
    var mapping3 =
        keys.stream()
            .collect(groupingBy(k -> hasher.getBucket(k), mapping(Long::toHexString, toList())));
    // gives {0=[9b7193f97ec9cb79], 1=[48ac502166f761a8], 2=[6ce88bf7de8c06c2]}

    // remove bucket 2
    hasher.removeBucket(bucket2);

    // determine mapping of keys to remaining 2 buckets
    var mapping2 =
        keys.stream()
            .collect(groupingBy(k -> hasher.getBucket(k), mapping(Long::toHexString, toList())));
    // gives {0=[9b7193f97ec9cb79], 2=[48ac502166f761a8, 6ce88bf7de8c06c2]}
    // key 48ac502166f761a8 got reassigned from bucket 1 to bucket 2

    // get state of hasher
    byte[] state = hasher.getState();

    // create another instance with same mapping
    var otherHasher =
        ConsistentHashing.jumpBackAnchorHash(PseudoRandomGeneratorProvider.splitMix64_V1())
            .setState(state);

    // determine mapping of keys using other instance
    var otherMapping2 =
        keys.stream()
            .collect(
                groupingBy(k -> otherHasher.getBucket(k), mapping(Long::toHexString, toList())));
    // gives again {0=[9b7193f97ec9cb79], 2=[48ac502166f761a8, 6ce88bf7de8c06c2]}

    assertThat(mapping3)
        .hasToString("{0=[9b7193f97ec9cb79], 1=[48ac502166f761a8], 2=[6ce88bf7de8c06c2]}");
    assertThat(mapping2)
        .hasToString("{0=[9b7193f97ec9cb79], 2=[48ac502166f761a8, 6ce88bf7de8c06c2]}");
    assertThat(otherMapping2)
        .hasToString("{0=[9b7193f97ec9cb79], 2=[48ac502166f761a8, 6ce88bf7de8c06c2]}");
    assertThat(bucket1).isEqualTo(0);
    assertThat(bucket2).isEqualTo(1);
    assertThat(bucket3).isEqualTo(2);
  }
}
