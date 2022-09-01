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
package com.dynatrace.hash4j.random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import com.dynatrace.hash4j.hashing.HashStream;
import com.dynatrace.hash4j.hashing.Hashing;
import java.util.Arrays;
import java.util.stream.LongStream;
import org.hipparchus.stat.inference.ChiSquareTest;
import org.junit.jupiter.api.Test;

public abstract class AbstractPseudoRandomGeneratorTest {

  protected abstract PseudoRandomGenerator createPseudoRandomGenerator();
  /*
    @Test
    void testCompatibility() {

      // byte[] data = {(byte) 0x4c, (byte) 0x50, (byte) 0xeb, (byte) 0xc4};

      long data = 0x684419835ea4ec8dL;
      PseudoRandomGenerator prg1 = createPseudoRandomGenerator();
      prg1.reset(data);
      PseudoRandomGenerator prg2 = createPseudoRandomGenerator();
      prg2.reset(data);

      int n = 100000;
      Random rnd = new Random(0);
      for (int i = 0; i < n; ++i) {
        int numBits = rnd.nextInt(65);

        long r1 = 0;
        for (int j = 0; j < numBits; ++j) {
          r1 <<= 1;
          if (prg1.nextBit()) {
            r1 |= 1;
          }
        }

        long r2 = prg2.nextBits(numBits);
        assertEquals(r1, r2);
      }
    }

    @Test
    void testUniformity() {

      long data = 0x67d8835e206dd94dL;

      int numBits = 4;
      long n = 100000;

      int numDifferentValues = 1 << numBits;
      long[] counts = new long[numDifferentValues];
      double[] expectedCounts = new double[numDifferentValues];
      Arrays.fill(expectedCounts, n / (double) numDifferentValues);

      PseudoRandomGenerator prg = createPseudoRandomGenerator();
      prg.reset(data);
      for (long i = 0; i < n; ++i) {
        counts[(int) prg.nextBits(numBits)] += 1;
      }

      assertThat(new GTest().gTest(expectedCounts, counts)).isGreaterThan(0.01);
    }

    @Test
    void testBias() {

      long data = 0xbb0bc9ae3999000fL;

      int n = 100000;
      int c = 0;
      PseudoRandomGenerator prg = createPseudoRandomGenerator();
      prg.reset(data);
      for (int i = 0; i < n; ++i) {
        if (prg.nextBit()) {
          c += 1;
        }
      }

      assertThat(new BinomialTest().binomialTest(n, c, 0.5, AlternativeHypothesis.TWO_SIDED))
          .isGreaterThan(0.01);
    }

    @Test
    void testZeroBits() {

      long data = 0xbb0bc9ae3999000fL;
      PseudoRandomGenerator prg = createPseudoRandomGenerator();
      prg.reset(data);

      for (int i = 0; i < 200; ++i) {
        assertThat(prg.nextBits(0)).isEqualTo(0);
        prg.nextBit();
      }
    }
  */
  @Test
  void testReproducibility() {
    PseudoRandomGenerator prg = createPseudoRandomGenerator();
    int numValues = 1000;
    prg.reset(0xa4acac19eed0e757L);
    long[] data1 = LongStream.generate(prg::nextLong).limit(numValues).toArray();
    prg.reset(0xa4acac19eed0e757L);
    long[] data2 = LongStream.generate(prg::nextLong).limit(numValues).toArray();
    prg.reset(0xa4acac19eed0e757L);
    long[] data3 = LongStream.generate(prg::nextLong).limit(numValues).toArray();

    assertArrayEquals(data1, data2);
    assertArrayEquals(data1, data3);
  }

  void testUniformIntWithExclusiveBound(int bucketSize, int numBuckets, long seed) {
    long avgValuesPerBucket = 10000;

    PseudoRandomGenerator rng = createPseudoRandomGenerator();
    rng.reset(seed);

    long numValues = avgValuesPerBucket * numBuckets;
    long[] counts1 = new long[numBuckets];
    long[] counts2 = new long[numBuckets];
    double[] expected = new double[numBuckets];
    Arrays.fill(expected, 1);

    int upperBound = bucketSize * numBuckets;
    assertThat(upperBound).isGreaterThanOrEqualTo(0);

    for (int i = 0; i < numValues; ++i) {
      int value = rng.uniformInt(upperBound);
      assertThat(value).isGreaterThanOrEqualTo(0);
      assertThat(value).isLessThan(upperBound);
      counts1[value % numBuckets] += 1;
      counts2[value / bucketSize] += 1;
    }
    assertThat(new ChiSquareTest().chiSquareTest(expected, counts1)).isGreaterThan(0.01);
    assertThat(new ChiSquareTest().chiSquareTest(expected, counts2)).isGreaterThan(0.01);
  }

  @Test
  void testUniformIntWithExclusiveBound() {
    testUniformIntWithExclusiveBound(1, 13, 0x298fdda4620c0c00L);
    testUniformIntWithExclusiveBound(14348907, 81, 0x8b6bdccd40aee5bdL);
  }

  @Test
  void testUniformIntWithSpecialParameters() {
    PseudoRandomGenerator rng = createPseudoRandomGenerator();
    rng.reset(0x674b0446e48aa471L);
    assertThat(rng.uniformInt(0)).isEqualTo(0);
    assertThat(rng.uniformInt(1)).isEqualTo(0);
    assertThat(rng.uniformInt(-1)).isLessThan(0);
    assertThat(rng.uniformInt(-2)).isLessThan(0);
    assertThat(rng.uniformInt(Integer.MIN_VALUE)).isLessThan(0);
  }

  @Test
  void testStability() {
    PseudoRandomGenerator prg = createPseudoRandomGenerator();
    for (int c = 0; c < 3; ++c) {
      HashStream hashStream = Hashing.komihash4_3().hashStream();
      prg.reset(0x2953eb15d353f9bdL);
      for (int i = 0; i < 1000; ++i) {
        hashStream.putLong(prg.nextLong());
      }
      for (int i = 0; i < 1000; ++i) {
        hashStream.putLong(prg.nextInt());
      }
      for (int i = 0; i < 1000; ++i) {
        hashStream.putInt(prg.uniformInt(i));
      }
      for (int i = 1; i < 1000; ++i) {
        hashStream.putInt(prg.uniformInt(Integer.MAX_VALUE / i));
      }
      assertThat(hashStream.getAsLong()).isEqualTo(getExpectedStabilityCheckSum());
    }
  }

  protected abstract long getExpectedStabilityCheckSum();
}
