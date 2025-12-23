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
package com.dynatrace.hash4j.consistent;

import static org.assertj.core.api.Assertions.assertThat;

import com.dynatrace.hash4j.internal.Preconditions;
import java.math.BigDecimal;
import java.util.SplittableRandom;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class XorshiftL7R9AlgorithmTest {

  // JumpBackHash could run endlessly if a certain bit position in the random sequence
  // would never change. However, for maximum xorshift generators it is known, that no single
  // bit can remain constant for more than n consecutive steps, if n is the state size.
  // If it did, the linear independence of successive states would fail,
  // and the generator could not have maximal period.
  @Test
  void testXorShiftBitFlips() {
    SplittableRandom random = new SplittableRandom(0x84f038a18907e829L);
    long numIterations = 10_000_000;

    long maxCounter = 0;
    int n = Long.SIZE;
    for (long l = 0; l < numIterations; ++l) {
      long counter = 0;
      long a = 0xFFFFFFFFFFFFFFFFL;
      long b = 0x0000000000000000L;

      long hash = random.nextLong();
      if (hash == 0) continue;
      do {
        hash = XorshiftL7R9Algorithm.get().updateState(hash);
        a &= hash;
        b |= hash;
        counter += 1;
      } while (a != 0x0000000000000000L || b != 0xFFFFFFFFFFFFFFFFL);
      if (counter > maxCounter) maxCounter = counter;
    }

    assertThat(maxCounter).isLessThanOrEqualTo(n);
  }

  private static final long TWO_POW_64_MINUS_1 = 0xFFFFFFFFFFFFFFFFL;
  private static final long[] PRIMES_OF_TWO_POW_64_MINUS_1 = {3, 5, 17, 257, 641, 65537, 6700417};

  @Test
  void testPrimes() {
    BigDecimal r = BigDecimal.ONE;
    for (long p : PRIMES_OF_TWO_POW_64_MINUS_1) {
      r = r.multiply(BigDecimal.valueOf(p));
    }
    assertThat(r).isEqualTo(BigDecimal.valueOf(2).pow(64).subtract(BigDecimal.ONE));
  }

  @Test
  void testMaximalCycle() {
    int dim = 64;
    BinaryMatrix m =
        BinaryMatrix.mul(
            BinaryMatrix.add(
                BinaryMatrix.identity(dim), BinaryMatrix.pow(BinaryMatrix.lsh(dim), 7)),
            BinaryMatrix.add(
                BinaryMatrix.identity(dim), BinaryMatrix.pow(BinaryMatrix.rsh(dim), 9)));
    assertThat(hasMaximumCycle(m)).isTrue();
  }

  private static boolean hasMaximumCycle(BinaryMatrix m) {
    Preconditions.checkArgument(m.dim() == 64);
    if (!BinaryMatrix.pow(m, TWO_POW_64_MINUS_1).isIdentity()) return false;
    for (long p : PRIMES_OF_TWO_POW_64_MINUS_1) {
      if (BinaryMatrix.pow(m, Long.divideUnsigned(TWO_POW_64_MINUS_1, p)).isIdentity())
        return false;
    }
    return true;
  }

  // finds parameters as listed
  // at http://isaku-wada.my.coocan.jp/rand/rand.html
  void findMaximumCycleParameters2() {
    int dim = 64;
    BinaryMatrix identity = BinaryMatrix.identity(dim);
    BinaryMatrix lsh = BinaryMatrix.lsh(dim);
    BinaryMatrix rsh = BinaryMatrix.rsh(dim);
    BinaryMatrix[] lshPowPlusIdentity =
        IntStream.range(0, 64)
            .mapToObj(i -> BinaryMatrix.add(identity, BinaryMatrix.pow(lsh, i)))
            .toArray(BinaryMatrix[]::new);
    BinaryMatrix[] rshPowPlusIdentity =
        IntStream.range(0, 64)
            .mapToObj(i -> BinaryMatrix.add(identity, BinaryMatrix.pow(rsh, i)))
            .toArray(BinaryMatrix[]::new);

    for (int a = 0; a < 64; ++a) {
      for (int b = 0; b < 64; ++b) {
        BinaryMatrix m = BinaryMatrix.mul(lshPowPlusIdentity[a], rshPowPlusIdentity[b]);
        if (hasMaximumCycle(m)) {
          System.out.println("a = " + a + ", b = " + b);
        }
      }
    }
  }

  // finds parameters as listed in
  // https://doi.org/10.18637/jss.v008.i14
  void findMaximumCycleParameters3() {
    int dim = 64;
    BinaryMatrix identity = BinaryMatrix.identity(dim);
    BinaryMatrix lsh = BinaryMatrix.lsh(dim);
    BinaryMatrix rsh = BinaryMatrix.rsh(dim);
    BinaryMatrix[] lshPowPlusIdentity =
        IntStream.range(0, 64)
            .mapToObj(i -> BinaryMatrix.add(identity, BinaryMatrix.pow(lsh, i)))
            .toArray(BinaryMatrix[]::new);
    BinaryMatrix[] rshPowPlusIdentity =
        IntStream.range(0, 64)
            .mapToObj(i -> BinaryMatrix.add(identity, BinaryMatrix.pow(rsh, i)))
            .toArray(BinaryMatrix[]::new);

    for (int a = 0; a < 64; ++a) {
      for (int b = 0; b < 64; ++b) {
        BinaryMatrix mab = BinaryMatrix.mul(lshPowPlusIdentity[a], rshPowPlusIdentity[b]);
        for (int c = a + 1; c < 64; ++c) { // require a < c
          BinaryMatrix m = BinaryMatrix.mul(mab, lshPowPlusIdentity[c]);
          if (hasMaximumCycle(m)) {
            System.out.println("a = " + a + ", b = " + b + ", c = " + c);
          }
        }
      }
    }
  }
}
