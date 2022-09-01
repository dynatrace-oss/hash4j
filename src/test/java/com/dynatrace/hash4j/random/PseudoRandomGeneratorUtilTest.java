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

public class PseudoRandomGeneratorUtilTest {

  /*
  @Test
  public void testGetExponential() {

    int dataSize = 10000;
    PseudoRandomGenerator pseudoRandomGenerator = new PseudoRandomGeneratorForTesting();
    pseudoRandomGenerator.reset(0);
    double[] data =
        DoubleStream.generate(() -> RandomBitStreamUtil.getExponential(pseudoRandomGenerator))
            .limit(dataSize)
            .toArray();

    assertThat(
            new KolmogorovSmirnovTest().kolmogorovSmirnovTest(new ExponentialDistribution(1), data))
        .isGreaterThan(0.01);
  }

  @Test
  public void testGetExponentialZiggurat() {

    int dataSize = 10000;
    PseudoRandomGenerator pseudoRandomGenerator = new PseudoRandomGeneratorForTesting();
    pseudoRandomGenerator.reset(0);
    double[] data =
        DoubleStream.generate(
                () -> RandomBitStreamUtil.getExponentialZiggurat(pseudoRandomGenerator))
            .limit(dataSize)
            .toArray();

    assertThat(
            new KolmogorovSmirnovTest().kolmogorovSmirnovTest(new ExponentialDistribution(1), data))
        .isGreaterThan(0.01);
  }


  @Test
  public void testGetUniformDouble() {

    int dataSize = 10000;
    PseudoRandomGenerator pseudoRandomGenerator = new PseudoRandomGeneratorForTesting();
    pseudoRandomGenerator.reset(0);
    double[] data =
        DoubleStream.generate(() -> RandomBitStreamUtil.getUniformDouble(pseudoRandomGenerator))
            .limit(dataSize)
            .toArray();
    assertThat(
            new KolmogorovSmirnovTest().kolmogorovSmirnovTest(new UniformRealDistribution(), data))
        .isGreaterThan(0.01);
  }

  @Test
  public void testGetUniformLongLumbroso1() {

    int dataSize = 10000;

    PseudoRandomGenerator pseudoRandomGenerator = new PseudoRandomGeneratorForTesting();
    pseudoRandomGenerator.reset(0);
    IntStream.generate(
            () -> (int) RandomBitStreamUtil.getUniformLongLumbroso(pseudoRandomGenerator, 1))
        .limit(dataSize)
        .forEach(i -> assertEquals(0, i));
  }


  @Test
  public void testGetUniformLongLumbroso2() {

    int dataSize = 10000;

    int[] maxValues = {2, 3, 5, 7, 11};

    PseudoRandomGenerator pseudoRandomGenerator = new PseudoRandomGeneratorForTesting();
    pseudoRandomGenerator.reset(0);

    for (int max : maxValues) {

      double[] expectedCounts =
          DoubleStream.generate(() -> dataSize / (double) max).limit(max).toArray();
      long[] counts = new long[max];
      IntStream.generate(
              () -> (int) RandomBitStreamUtil.getUniformLongLumbroso(pseudoRandomGenerator, max))
          .limit(dataSize)
          .forEach(i -> counts[i] += 1);

      assertThat(new GTest().gTest(expectedCounts, counts)).isGreaterThan(0.01);
    }
  }

  @Test
  public void testGetUniformLongLumbroso3() {

    int dataSize = 100000;
    PseudoRandomGenerator pseudoRandomGenerator = new PseudoRandomGeneratorForTesting();
    pseudoRandomGenerator.reset(0);
    double[] data =
        LongStream.generate(
                () ->
                    RandomBitStreamUtil.getUniformLongLumbroso(
                        pseudoRandomGenerator, Long.MAX_VALUE))
            .limit(dataSize)
            .mapToDouble(x -> x)
            .toArray();
    assertThat(
            new KolmogorovSmirnovTest()
                .kolmogorovSmirnovTest(
                    new UniformRealDistribution(-0.5, Long.MAX_VALUE - 0.5), data))
        .isGreaterThan(0.01);
  }

  @Test
  public void testGetUniformIntLumbroso1() {

    int dataSize = 10000;

    PseudoRandomGenerator pseudoRandomGenerator = new PseudoRandomGeneratorForTesting();
    pseudoRandomGenerator.reset(0);
    IntStream.generate(() -> RandomBitStreamUtil.getUniformIntLumbroso(pseudoRandomGenerator, 1))
        .limit(dataSize)
        .forEach(i -> assertEquals(0, i));
  }

  @Test
  public void testGetUniformIntLumbroso2() {

    int dataSize = 10000;

    int[] maxValues = {2, 3, 5, 7, 11};

    PseudoRandomGenerator pseudoRandomGenerator = new PseudoRandomGeneratorForTesting();
    pseudoRandomGenerator.reset(0);

    for (int max : maxValues) {

      double[] expectedCounts =
          DoubleStream.generate(() -> dataSize / (double) max).limit(max).toArray();
      long[] counts = new long[max];
      IntStream.generate(
              () -> RandomBitStreamUtil.getUniformIntLumbroso(pseudoRandomGenerator, max))
          .limit(dataSize)
          .forEach(i -> counts[i] += 1);

      assertThat(new GTest().gTest(expectedCounts, counts)).isGreaterThan(0.01);
    }
  }

  @Test
  public void testGetUniformIntLumbroso3() {

    int dataSize = 100000;
    PseudoRandomGenerator pseudoRandomGenerator = new PseudoRandomGeneratorForTesting();
    pseudoRandomGenerator.reset(1);
    double[] data =
        IntStream.generate(
                () ->
                    RandomBitStreamUtil.getUniformIntLumbroso(
                        pseudoRandomGenerator, Integer.MAX_VALUE))
            .limit(dataSize)
            .mapToDouble(x -> x)
            .toArray();
    assertThat(
            new KolmogorovSmirnovTest()
                .kolmogorovSmirnovTest(
                    new UniformRealDistribution(-0.5, Integer.MAX_VALUE - 0.5), data))
        .isGreaterThan(0.01);
  }

  @Test
  public void testGetUniformIntLemire1() {

    int dataSize = 10000;

    PseudoRandomGenerator pseudoRandomGenerator = new PseudoRandomGeneratorForTesting();
    pseudoRandomGenerator.reset(0);
    IntStream.generate(() -> RandomBitStreamUtil.getUniformIntLemire(pseudoRandomGenerator, 1))
        .limit(dataSize)
        .forEach(i -> assertEquals(0, i));
  }

  @Test
  public void testGetUniformIntLemire2() {

    int dataSize = 10000;

    int[] maxValues = {2, 3, 5, 7, 11, 15489783};

    PseudoRandomGenerator pseudoRandomGenerator = new PseudoRandomGeneratorForTesting();
    pseudoRandomGenerator.reset(1);

    for (int max : maxValues) {

      double[] expectedCounts =
          DoubleStream.generate(() -> dataSize / (double) max).limit(max).toArray();
      long[] counts = new long[max];
      IntStream.generate(() -> RandomBitStreamUtil.getUniformIntLemire(pseudoRandomGenerator, max))
          .limit(dataSize)
          .forEach(i -> counts[i] += 1);

      assertThat(new GTest().gTest(expectedCounts, counts)).isGreaterThan(0.01);
    }
  }

  @Test
  public void testGetUniformIntLemire3() {

    int dataSize = 100000;
    PseudoRandomGenerator pseudoRandomGenerator = new PseudoRandomGeneratorForTesting();
    pseudoRandomGenerator.reset(1);
    double[] data =
        IntStream.generate(
                () ->
                    RandomBitStreamUtil.getUniformIntLemire(
                        pseudoRandomGenerator, Integer.MAX_VALUE))
            .limit(dataSize)
            .mapToDouble(x -> x)
            .toArray();
    assertThat(
            new KolmogorovSmirnovTest()
                .kolmogorovSmirnovTest(
                    new UniformRealDistribution(-0.5, Integer.MAX_VALUE - 0.5), data))
        .isGreaterThan(0.01);
  }

  // see Lumbroso, Jeremie. "Optimal discrete uniform generation from coin flips, and applications."
  // arXiv preprint arXiv:1304.1916 (2013).
  // generate an integer in the range [0, n)
  private static long getUniformLongLumbrosoReferenceImplementation(
      PseudoRandomGenerator bitstream, long n) {
    requireNonNull(n > 0);
    long v = 1;
    long c = 0;
    while (true) {
      v <<= 1;
      c <<= 1;
      if (bitstream.nextBit()) {
        c |= 1;
      }
      if (v >= n || v < 0) {
        if (c < n && c >= 0) {
          return c;
        } else {
          v -= n;
          c -= n;
        }
      }
    }
  }

  @Test
  public void testModifiedLumbrosoMethodLong() {

    int N = 10000;

    RandomGenerator rng = new Well1024a(0x29c9b455b500b235L);
    PseudoRandomGeneratorForTesting h1 = new PseudoRandomGeneratorForTesting();
    h1.reset(0x1c0bb3cfb2837470L);
    PseudoRandomGeneratorForTesting h2 = new PseudoRandomGeneratorForTesting();
    h2.reset(0x1c0bb3cfb2837470L);

    for (int i = 0; i < N; ++i) {
      long x = rng.nextLong(Long.MAX_VALUE) + 1;
      long val1 = RandomBitStreamUtil.getUniformLongLumbroso(h1, x);
      long val2 = getUniformLongLumbrosoReferenceImplementation(h2, x);
      assertEquals(val2, val1);
    }
  }

  @Test
  public void testModifiedLumbrosoMethodInt() {

    int N = 10000;

    SplittableRandom rng = new SplittableRandom(0x29c9b455b500b235L);
    PseudoRandomGeneratorForTesting h1 = new PseudoRandomGeneratorForTesting();
    h1.reset(0x1c0bb3cfb2837470L);
    PseudoRandomGeneratorForTesting h2 = new PseudoRandomGeneratorForTesting();
    h2.reset(0x1c0bb3cfb2837470L);
    PseudoRandomGeneratorForTesting h3 = new PseudoRandomGeneratorForTesting();
    h3.reset(0x1c0bb3cfb2837470L);

    for (int i = 0; i < N; ++i) {
      int x = rng.nextInt(Integer.MAX_VALUE) + 1;
      int val1 = (int) RandomBitStreamUtil.getUniformLongLumbroso(h1, x);
      int val2 = RandomBitStreamUtil.getUniformIntLumbroso(h2, x);
      int val3 = (int) getUniformLongLumbrosoReferenceImplementation(h3, x);
      assertEquals(val2, val1);
      assertEquals(val3, val1);
    }
  }

  @Test
  public void testTableValues() {

    for (int i = 1; i <= 256; ++i) {
      double xi = getExponentialTableX(i);
      double yi = getExponentialTableY(i);
      assertThat(yi).isCloseTo(StrictMath.exp(-xi), Offset.offset(1e-15));
      assertThat(xi).isCloseTo(-StrictMath.log(yi), Offset.offset(1e-15));
    }

    for (int i = 1; i < 256; ++i) {
      double xi = getExponentialTableX(i);
      assertThat(StrictMath.exp(-xi) * (1. + xi)).isCloseTo(i / 256., Offset.offset(5e-3));
    }
    assertThat(getExponentialTableX(256)).isEqualTo(0.);

    assertThat(getExponentialTableY(0)).isEqualTo(0.);
    for (int i = 1; i < 256; ++i) {
      double yi = getExponentialTableY(i);
      assertThat(yi * (1. - StrictMath.log(yi))).isCloseTo(i / 256., Offset.offset(5e-3));
    }
    assertThat(getExponentialTableY(256)).isEqualTo(1.);

    // assertThat(getExponentialTableX(0)).isEqualTo(1./256 / getExponentialTableY(1));

    //for(int i = 1; i < 256; ++i) {
    //  double xi = RandomBitStreamUtil.getExponentialTableX(i);
    //  assertThat(yi * (1. - StrictMath.log(yi))).isCloseTo(i / 256., Offset.offset(5e-3));
    //}

  }
  */

}
