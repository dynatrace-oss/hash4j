/*
 * Copyright 2022-2026 Dynatrace LLC
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
package com.dynatrace.hash4j.distinctcount;

import static com.dynatrace.hash4j.internal.Preconditions.checkArgument;
import static java.util.Comparator.comparing;
import static org.assertj.core.api.Assertions.assertThat;

import com.dynatrace.hash4j.random.PseudoRandomGenerator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class TestUtils {

  private TestUtils() {}

  public static strictfp long[] getDistinctCountValues(
      long min, long max, double relativeIncrement) {
    List<Long> distinctCounts = new ArrayList<>();
    final double factor = 1. / (1. + relativeIncrement);
    for (long c = max; c >= min; c = Math.min(c - 1, (long) Math.ceil(c * factor))) {
      distinctCounts.add(c);
    }
    Collections.reverse(distinctCounts);
    return distinctCounts.stream().mapToLong(Long::valueOf).toArray();
  }

  public static strictfp List<BigInt> getDistinctCountValues(double max, double relativeIncrement) {
    checkArgument(max >= 1.);
    List<BigInt> distinctCounts = new ArrayList<>();
    BigInt c = BigInt.ceil(max);
    final double factor = 1. / (1. + relativeIncrement);
    while (c.isPositive()) {
      distinctCounts.add(c.copy());
      double d = c.asDouble();
      c.decrement();
      c.min(BigInt.ceil(d * factor));
    }
    Collections.reverse(distinctCounts);
    return distinctCounts;
  }

  public interface HashGenerator {

    double getProbability();

    long generateHashValue(int registerIndex);
  }

  // used for HyperLogLog and UltraLogLog
  public static List<HashGenerator> getHashGenerators1(int p) {
    List<HashGenerator> generators = new ArrayList<>();

    for (int updateValue = 1; updateValue <= 65 - p; ++updateValue) {

      final double probability =
          Double.longBitsToDouble((0x3ffL - Math.min(updateValue, 64 - p)) << 52);
      int nlz = updateValue - 1;
      final long z = (nlz < 64) ? 0xFFFFFFFFFFFFFFFFL >>> p >>> nlz : 0L;

      generators.add(
          new HashGenerator() {
            @Override
            public double getProbability() {
              return probability;
            }

            @Override
            public long generateHashValue(int registerIndex) {
              return z | (((long) registerIndex) << -p);
            }
          });
    }
    return generators;
  }

  public static final class Transition {
    private final BigInt distinctCount;
    private final long hash;

    public BigInt getDistinctCount() {
      return distinctCount;
    }

    public long getHash() {
      return hash;
    }

    public Transition(BigInt distinctCount, long hash) {
      this.distinctCount = distinctCount;
      this.hash = hash;
    }
  }

  public static void generateTransitions(
      Transition[] transitions,
      BigInt distinctCountOffset,
      List<HashGenerator> hashGenerators,
      int p,
      PseudoRandomGenerator prg) {

    assertThat(transitions).hasSize(hashGenerators.size() * (1 << p));

    int counter = 0;
    for (HashGenerator hashGenerator : hashGenerators) {
      double factor = (1 << p) / hashGenerator.getProbability();
      for (int idx = 0; idx < (1 << p); ++idx) {
        BigInt transitionDistinctCount = BigInt.floor(prg.nextExponential() * factor);
        transitionDistinctCount.increment(); // 1-based geometric distribution
        transitionDistinctCount.add(distinctCountOffset);
        long hash = hashGenerator.generateHashValue(idx);
        transitions[counter++] = new Transition(transitionDistinctCount, hash);
      }
    }
    Arrays.sort(transitions, comparing(Transition::getDistinctCount));
  }
}
