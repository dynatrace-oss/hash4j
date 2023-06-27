/*
 * Copyright 2023 Dynatrace LLC
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

import static com.dynatrace.hash4j.util.Preconditions.checkArgument;

import com.dynatrace.hash4j.util.Preconditions;
import java.math.BigInteger;
import java.util.Objects;

public class BigInt implements Comparable<BigInt> {

  private static final BigInteger TWO_POW_63 = BigInteger.valueOf(2).pow(63);
  private static final double TWO_POW_PLUS_63_DOUBLE = Math.pow(2., 63);

  private static final double TWO_POW_PLUS_126_DOUBLE = Math.pow(2., 126);
  private static final double TWO_POW_MINUS_63_DOUBLE = Math.pow(2., -63);

  private long low;
  private long high;

  public void increment() {
    low += 1;
    if (low < 0) {
      high += 1;
      low = 0;
    }
  }

  public void decrement() {
    low -= 1;
    if (low < 0) {
      high -= 1;
      low = Long.MAX_VALUE;
    }
  }

  public void add(BigInt other) {
    low += other.low;
    if (low < 0) {
      high += 1;
      low += 0x8000000000000000L;
    }
    high += other.high;
  }

  public void add(long x) {
    Preconditions.checkArgument(x >= 0);
    low += x;
    if (low < 0) {
      high += 1;
      low += 0x8000000000000000L;
    }
  }

  private BigInt(long high, long low) {
    checkArgument(high >= 0 && low >= 0);
    this.low = low;
    this.high = high;
  }

  public static BigInt floor(double d) {
    checkArgument(d >= 0.);
    return fromIntDouble(Math.floor(d));
  }

  public static BigInt ceil(double d) {
    checkArgument(d >= 0.);
    return fromIntDouble(Math.ceil(d));
  }

  private static BigInt fromIntDouble(double d) {
    checkArgument(d < TWO_POW_PLUS_126_DOUBLE);
    if (d >= TWO_POW_PLUS_63_DOUBLE) {
      long high = (long) (d * TWO_POW_MINUS_63_DOUBLE);
      long low = (long) (d - high * TWO_POW_PLUS_63_DOUBLE);
      return new BigInt(high, low);
    } else {
      return new BigInt(0L, (long) d);
    }
  }

  public double asDouble() {
    return high * TWO_POW_PLUS_63_DOUBLE + low;
  }

  public static BigInt fromBigInt(BigInteger i) {
    long low = i.mod(TWO_POW_63).longValueExact();
    long high = i.divide(TWO_POW_63).longValueExact();
    return new BigInt(high, low);
  }

  public static BigInt fromLong(long l) {
    checkArgument(l >= 0);
    return new BigInt(0, l);
  }

  public static BigInt createZero() {
    return new BigInt(0, 0);
  }

  @Override
  public String toString() {
    return BigInteger.valueOf(high).multiply(TWO_POW_63).add(BigInteger.valueOf(low)).toString();
  }

  @Override
  public int compareTo(BigInt o) {
    if (high > o.high) return 1;
    if (high < o.high) return -1;
    return Long.compare(low, o.low);
  }

  public boolean isPositive() {
    return low > 0 || high > 0;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    BigInt bigInt = (BigInt) o;
    return low == bigInt.low && high == bigInt.high;
  }

  @Override
  public int hashCode() {
    return Objects.hash(low, high);
  }

  public void min(BigInt x) {
    if (high > x.high) {
      high = x.high;
      low = x.low;
    } else if (high == x.high && low > x.low) {
      low = x.low;
    }
  }

  public BigInt copy() {
    return new BigInt(high, low);
  }

  public void set(BigInt x) {
    this.high = x.high;
    this.low = x.low;
  }
}
