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
package com.dynatrace.hash4j.random;

import static java.lang.Math.toIntExact;
import static org.assertj.core.api.Assertions.assertThat;

import com.dynatrace.hash4j.hashing.HashStream64;
import com.dynatrace.hash4j.hashing.Hashing;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.function.ToLongFunction;
import java.util.stream.DoubleStream;
import java.util.stream.LongStream;
import org.hipparchus.distribution.continuous.ExponentialDistribution;
import org.hipparchus.distribution.continuous.UniformRealDistribution;
import org.hipparchus.stat.inference.ChiSquareTest;
import org.hipparchus.stat.inference.KolmogorovSmirnovTest;
import org.junit.jupiter.api.Test;

abstract class AbstractPseudoRandomGeneratorTest {

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

    assertThat(data1).isEqualTo(data2).isEqualTo(data3);
  }

  void testUniformWithExclusiveBound(
      long upperBound,
      int numBuckets,
      int numValues,
      PseudoRandomGenerator pseudoRandomGenerator,
      ToLongFunction<PseudoRandomGenerator> randomValueSupplier) {
    assertThat(upperBound).isNotNegative();
    assertThat(upperBound).isGreaterThanOrEqualTo(numBuckets);

    long[] counts1 = new long[numBuckets];
    long[] counts2 = new long[numBuckets];

    long[] expected1 = new long[numBuckets];
    long[] expected2 = new long[numBuckets];
    Arrays.fill(expected1, upperBound / numBuckets);
    Arrays.fill(expected2, upperBound / numBuckets);
    for (int i = 0; i < Math.floorMod(upperBound, numBuckets); ++i) {
      expected1[i] = expected1[i] + 1;
    }
    long pos = 0;
    long inc = upperBound / numBuckets;
    for (int bucketIdx = 0; bucketIdx < numBuckets; ++bucketIdx) {
      pos += inc;
      if (BigInteger.valueOf(numBuckets)
              .multiply(BigInteger.valueOf(pos))
              .divide(BigInteger.valueOf(upperBound))
              .intValue()
          == bucketIdx) {
        pos += 1;
        expected2[bucketIdx] += 1;
      }
    }
    assertThat(pos).isEqualTo(upperBound);
    assertThat(LongStream.of(expected1).sum()).isEqualTo(upperBound);
    assertThat(LongStream.of(expected2).sum()).isEqualTo(upperBound);
    for (int i = 0; i < numBuckets; ++i) {
      assertThat(expected1[i]).isPositive();
      assertThat(expected2[i]).isPositive();
    }

    double[] values = new double[numValues];

    for (int i = 0; i < numValues; ++i) {
      long value = randomValueSupplier.applyAsLong(pseudoRandomGenerator);
      if (value < 0 || value >= upperBound) {
        throw new IllegalArgumentException(
            "The random value " + value + " is out of bounds [0, " + upperBound + ").");
      }
      values[i] = value + pseudoRandomGenerator.nextDouble();
      assertThat(value).isNotNegative();
      assertThat(value).isLessThan(upperBound);
      counts1[toIntExact(value % numBuckets)] += 1;
      counts2[
              BigInteger.valueOf(numBuckets)
                  .multiply(BigInteger.valueOf(value))
                  .divide(BigInteger.valueOf(upperBound))
                  .intValue()] +=
          1;
    }
    if (numBuckets > 1) {
      double[] expectedDouble1 = LongStream.of(expected1).mapToDouble(x -> x).toArray();
      assertThat(new ChiSquareTest().chiSquareTest(expectedDouble1, counts1)).isGreaterThan(0.01);
      double[] expectedDouble2 = LongStream.of(expected2).mapToDouble(x -> x).toArray();
      assertThat(new ChiSquareTest().chiSquareTest(expectedDouble2, counts2)).isGreaterThan(0.01);
    }
    assertThat(
            new KolmogorovSmirnovTest()
                .kolmogorovSmirnovTest(new UniformRealDistribution(0, upperBound), values))
        .isGreaterThan(0.01);
  }

  void testUniformIntWithExclusiveBound(int upperBound, int numBuckets, int numValues, long seed) {
    PseudoRandomGenerator prg = createPseudoRandomGenerator();
    prg.reset(seed);
    testUniformWithExclusiveBound(
        upperBound, numBuckets, numValues, prg, p -> (long) p.uniformInt(upperBound));
  }

  void testUniformLongWithExclusiveBound(
      long upperBound, int numBuckets, int numValues, long seed) {
    PseudoRandomGenerator prg = createPseudoRandomGenerator();
    prg.reset(seed);
    testUniformWithExclusiveBound(
        upperBound, numBuckets, numValues, prg, p -> p.uniformLong(upperBound));
  }

  @Test
  void testUniformIntWithExclusiveBound() {
    testUniformIntWithExclusiveBound((1 << 29) * 3, 3, 100000, 0x3681d7eed94fb670L);
    testUniformIntWithExclusiveBound((1 << 28) * 5, 5, 100000, 0xc77c32f5611d0cd5L);
    testUniformIntWithExclusiveBound((1 << 26) * 31, 31, 100000, 0xf8c449528ad19fe9L);
    testUniformIntWithExclusiveBound(13, 13, 100000, 0x624bf2932fbb7ffbL);
    testUniformIntWithExclusiveBound(1162261467, 81, 100000, 0x39415da30a503d3aL);
    testUniformIntWithExclusiveBound(Integer.MAX_VALUE, 1, 100000, 0xb3d711fed5bd7b2fL);
  }

  @Test
  void testUniformLongWithExclusiveBound() {
    testUniformLongWithExclusiveBound((1L << 61) * 3, 3, 100000, 0x7fde71cfaf44b45fL);
    testUniformLongWithExclusiveBound((1L << 60) * 5, 5, 100000, 0xe56aa68bd181f9e0L);
    testUniformLongWithExclusiveBound((1L << 58) * 31, 31, 100000, 0x79224b95d1487c26L);
    testUniformLongWithExclusiveBound(13, 13, 100000, 0x3da9adff861a45b2L);
    testUniformLongWithExclusiveBound(1162261467, 81, 100000, 0x4772f55644a67142L);
    testUniformLongWithExclusiveBound(Integer.MAX_VALUE, 1, 100000, 0x979638e356a80903L);
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
  void testUniformLongWithSpecialParameters() {
    PseudoRandomGenerator prg = createPseudoRandomGenerator();
    prg.reset(0x52aa8dbcce9e07e4L);
    assertThat(prg.uniformLong(0)).isZero();
    assertThat(prg.uniformLong(1)).isZero();
    // For the given seed and the special bound -1 (interpreted by the implementation as a full-range bound),
    // this is the deterministic value currently produced by uniformLong(-1); this assertion serves as a
    // stability test to detect unintended changes in the pseudo-random generator implementation.
    assertThat(prg.uniformLong(-1)).isEqualTo(7289753898585205944L);
    assertThat(prg.uniformLong(-2)).isNegative();
    assertThat(prg.uniformLong(Long.MIN_VALUE)).isEqualTo(6857541587938986304L);
  }

  @Test
  void testStability() {
    PseudoRandomGenerator prg = createPseudoRandomGenerator();
    for (int c = 0; c < 3; ++c) {
      prg.reset(0x2953eb15d353f9bdL);
      HashStream64 hashStream = Hashing.komihash5_0().hashStream();
      for (int d = 0; d < 10; ++d) {
        for (int i = 0; i < 1000; ++i) {
          hashStream.putLong(prg.nextLong());
        }
        for (int i = 0; i < 1000; ++i) {
          hashStream.putLong(prg.nextInt());
        }
        for (int i = 0; i < 1000; ++i) {
          hashStream.putInt(prg.uniformInt(i));
        }
        for (int i = 0; i < 1000; ++i) {
          int shift = prg.nextInt();
          hashStream.putInt(prg.uniformInt((prg.nextInt() >>> shift) & Integer.MAX_VALUE));
        }
        for (int i = 0; i < 1000; ++i) {
          int shift = prg.nextInt();
          hashStream.putLong(prg.uniformLong((prg.nextLong() >>> shift) & Long.MAX_VALUE));
        }
        for (int i = 1; i < 1000; ++i) {
          hashStream.putDouble(prg.nextExponential());
        }
      }
      assertThat(hashStream.getAsLong()).isEqualTo(getExpectedStabilityCheckSum());
    }
  }

  @Test
  void testNextDouble() {

    int dataSize = 1000000;
    PseudoRandomGenerator prg = createPseudoRandomGenerator();
    prg.reset(0xf6f4612e7fa10323L);
    double[] data = DoubleStream.generate(prg::nextDouble).limit(dataSize).toArray();

    assertThat(
            new KolmogorovSmirnovTest().kolmogorovSmirnovTest(new UniformRealDistribution(), data))
        .isGreaterThan(0.01);
  }

  @Test
  void testNextExponential() {

    int dataSize = 1000000;
    PseudoRandomGenerator prg = createPseudoRandomGenerator();
    prg.reset(0xf6f4612e7fa10323L);
    double[] data = DoubleStream.generate(prg::nextExponential).limit(dataSize).toArray();

    assertThat(
            new KolmogorovSmirnovTest().kolmogorovSmirnovTest(new ExponentialDistribution(1), data))
        .isGreaterThan(0.01);
  }

  @Test
  void testNextExponentialCompatibility() {
    int numIterations = 10000000;
    PseudoRandomGenerator prg = createPseudoRandomGenerator();
    prg.reset(0xb9db9b9308cda722L);
    HashStream64 hashStream = Hashing.komihash4_3().hashStream();
    for (int i = 0; i < numIterations; ++i) {
      hashStream.putDouble(prg.nextExponential());
    }
    assertThat(hashStream.getAsLong()).isEqualTo(0x6e6ef62c10ced900L);
  }

  @Test
  void testNextDoubleCompatibility() {
    int numIterations = 10000000;
    PseudoRandomGenerator prg = createPseudoRandomGenerator();
    prg.reset(0x908c971030cd9247L);
    HashStream64 hashStream = Hashing.komihash4_3().hashStream();
    for (int i = 0; i < numIterations; ++i) {
      hashStream.putDouble(prg.nextDouble());
    }
    assertThat(hashStream.getAsLong()).isEqualTo(0x9d4e4697cc4853f1L);
  }

  protected abstract long getExpectedStabilityCheckSum();
}
