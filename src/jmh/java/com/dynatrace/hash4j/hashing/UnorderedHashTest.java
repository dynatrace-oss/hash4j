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

import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.infra.Blackhole;

public class UnorderedHashTest {

  private static final int NUM_TEST_DATA_SETS = 100;
  private static final int NUM_VALUES = 1000;
  private static final List<List<Long>> TEST_DATA_LONG = new ArrayList<>(NUM_TEST_DATA_SETS);

  static {
    final SplittableRandom random = new SplittableRandom(0);
    for (int j = 0; j < NUM_TEST_DATA_SETS; ++j) {
      TEST_DATA_LONG.add(random.longs(NUM_VALUES).boxed().collect(Collectors.toList()));
    }
  }

  private static final HashFunnel<Long> LONG_FUNNEL = (l, h) -> h.putLong(l);

  private static long long2Hash(long x) {
    return Hashing.murmur3_128().hashToLong(x, LONG_FUNNEL);
  }

  private static final HashFunnel<List<Long>> LIST_LONG_FUNNEL =
      (l, h) -> h.putUnorderedIterable(l, UnorderedHashTest::long2Hash);

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  public void testUnordered(Blackhole blackhole) {

    List<Long> testData =
        TEST_DATA_LONG.get(ThreadLocalRandom.current().nextInt(NUM_TEST_DATA_SETS));
    long hash = Hashing.murmur3_128().hashToLong(testData, LIST_LONG_FUNNEL);
    blackhole.consume(hash);
  }
}
