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
import java.util.stream.DoubleStream;
import java.util.stream.LongStream;
import org.hipparchus.distribution.continuous.ExponentialDistribution;
import org.hipparchus.distribution.continuous.UniformRealDistribution;
import org.hipparchus.stat.inference.ChiSquareTest;
import org.hipparchus.stat.inference.KolmogorovSmirnovTest;
import org.junit.jupiter.api.Test;

public abstract class AbstractPseudoRandomGeneratorTest {

  protected abstract PseudoRandomGenerator createPseudoRandomGenerator();

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

    PseudoRandomGenerator prg = createPseudoRandomGenerator();
    prg.reset(seed);

    long numValues = avgValuesPerBucket * numBuckets;
    long[] counts1 = new long[numBuckets];
    long[] counts2 = new long[numBuckets];
    double[] expected = new double[numBuckets];
    Arrays.fill(expected, 1);

    int upperBound = bucketSize * numBuckets;
    assertThat(upperBound).isNotNegative();

    for (int i = 0; i < numValues; ++i) {
      int value = prg.uniformInt(upperBound);
      assertThat(value).isNotNegative();
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
    PseudoRandomGenerator prg = createPseudoRandomGenerator();
    prg.reset(0x674b0446e48aa471L);
    assertThat(prg.uniformInt(0)).isZero();
    assertThat(prg.uniformInt(1)).isZero();
    assertThat(prg.uniformInt(-1)).isNegative();
    assertThat(prg.uniformInt(-2)).isNegative();
    assertThat(prg.uniformInt(Integer.MIN_VALUE)).isNegative();
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

  @Test
  public void testNextDouble() {

    int dataSize = 1000000;
    PseudoRandomGenerator prg = createPseudoRandomGenerator();
    prg.reset(0xf6f4612e7fa10323L);
    double[] data = DoubleStream.generate(prg::nextDouble).limit(dataSize).toArray();

    assertThat(
            new KolmogorovSmirnovTest().kolmogorovSmirnovTest(new UniformRealDistribution(), data))
        .isGreaterThan(0.01);
  }

  @Test
  public void testNextExponential() {

    int dataSize = 1000000;
    PseudoRandomGenerator prg = createPseudoRandomGenerator();
    prg.reset(0xf6f4612e7fa10323L);
    double[] data = DoubleStream.generate(prg::nextExponential).limit(dataSize).toArray();

    assertThat(
            new KolmogorovSmirnovTest().kolmogorovSmirnovTest(new ExponentialDistribution(1), data))
        .isGreaterThan(0.01);
  }

  protected abstract long getExpectedStabilityCheckSum();
}
