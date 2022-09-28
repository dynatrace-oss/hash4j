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
package com.dynatrace.hash4j.similarity;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.dynatrace.hash4j.random.PseudoRandomGenerator;
import com.dynatrace.hash4j.random.PseudoRandomGeneratorProvider;
import java.util.SplittableRandom;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import org.junit.jupiter.api.Test;

public class DistinctElementHashProviderTest {

  @Test
  void testDeduplication() {

    int distinctValues = 100;
    int numIterations = 100;
    int maxSize = 10000;

    DistinctElementHashProvider distinctElementHashProvider =
        new DistinctElementHashProvider(PseudoRandomGeneratorProvider.splitMix64_V1());
    SplittableRandom random = new SplittableRandom(0xa911e208996d3573L);

    for (int k = 0; k < numIterations; ++k) {

      int size = random.nextInt(maxSize);

      long[] values = random.longs(distinctValues).toArray();
      long[] input = random.ints(0, values.length).mapToLong(i -> values[i]).limit(size).toArray();
      long[] expectedDistinctSorted = LongStream.of(input).sorted().distinct().toArray();

      distinctElementHashProvider.reset(
          ElementHashProvider.ofFunction(i -> input[i], input.length));
      long[] actualDistinctSorted =
          IntStream.range(0, distinctElementHashProvider.getNumberOfElements())
              .mapToLong(distinctElementHashProvider::getElementHash)
              .sorted()
              .toArray();
      assertArrayEquals(expectedDistinctSorted, actualDistinctSorted);
    }
  }

  @Test
  void testInvalidInputSize() {
    DistinctElementHashProvider distinctElementHashProvider =
        new DistinctElementHashProvider(PseudoRandomGeneratorProvider.splitMix64_V1());

    assertThrows(
        IllegalArgumentException.class,
        () ->
            distinctElementHashProvider.reset(ElementHashProvider.ofFunction(i -> 0L, 0x40000001)));
  }

  @Test
  void testCollidingNullConstant() {
    DistinctElementHashProvider distinctElementHashProvider =
        new DistinctElementHashProvider(
            withFirstLongsDefined(
                PseudoRandomGeneratorProvider.splitMix64_V1(),
                0x0dc70133dcf3c1f4L,
                0x335bf97fcd04ad02L,
                0xb252396aeda53915L));

    long[] input = {
      0xb252396aeda53915L, 0x335bf97fcd04ad02L, 0x0dc70133dcf3c1f4L, 0xb252396aeda53915L
    };

    distinctElementHashProvider.reset(ElementHashProvider.ofFunction(i -> input[i], input.length));

    long[] expectedDistinctSorted = LongStream.of(input).sorted().distinct().toArray();

    long[] actualDistinctSorted =
        IntStream.range(0, distinctElementHashProvider.getNumberOfElements())
            .mapToLong(distinctElementHashProvider::getElementHash)
            .sorted()
            .toArray();
    assertArrayEquals(expectedDistinctSorted, actualDistinctSorted);
  }

  private static PseudoRandomGeneratorProvider withFirstLongsDefined(
      PseudoRandomGeneratorProvider pseudoRandomGeneratorProvider, long... firstRandomLongs) {
    return () ->
        new PseudoRandomGenerator() {
          private final PseudoRandomGenerator prg = pseudoRandomGeneratorProvider.create();
          private int count;

          @Override
          public long nextLong() {
            long result;
            if (count < firstRandomLongs.length) {
              result = firstRandomLongs[count];
            } else {
              result = prg.nextLong();
            }
            count += 1;
            return result;
          }

          @Override
          public int nextInt() {
            return prg.nextInt();
          }

          @Override
          public int uniformInt(int exclusiveBound) {
            return prg.uniformInt(exclusiveBound);
          }

          @Override
          public void reset(long seed) {
            prg.reset(seed);
            count = 0;
          }

          @Override
          public double nextDouble() {
            return prg.nextDouble();
          }

          @Override
          public double nextExponential() {
            return prg.nextExponential();
          }
        };
  }

  @Test
  void testSameNullConstant() {
    DistinctElementHashProvider distinctElementHashProvider =
        new DistinctElementHashProvider(
            withFirstLongsDefined(
                PseudoRandomGeneratorProvider.splitMix64_V1(),
                0x0fbbba603712dd2aL,
                0x0fbbba603712dd2aL,
                0xa20e5521a1bcde58L));
    long[] input = {0x0fbbba603712dd2aL};

    distinctElementHashProvider.reset(ElementHashProvider.ofFunction(i -> input[i], input.length));

    long[] expectedDistinctSorted = LongStream.of(input).sorted().distinct().toArray();

    long[] actualDistinctSorted =
        IntStream.range(0, distinctElementHashProvider.getNumberOfElements())
            .mapToLong(distinctElementHashProvider::getElementHash)
            .sorted()
            .toArray();
    assertArrayEquals(expectedDistinctSorted, actualDistinctSorted);
  }
}
