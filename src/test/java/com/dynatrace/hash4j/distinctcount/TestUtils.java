/*
 * Copyright 2022-2023 Dynatrace LLC
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

import com.dynatrace.hash4j.util.Preconditions;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TestUtils {

  private TestUtils() {}

  public static long[] getDistinctCountValues(long min, long max, double relativeIncrement) {
    List<Long> distinctCounts = new ArrayList<>();
    for (long c = max;
        c >= min;
        c = Math.min(c - 1, (long) Math.ceil(c / (1. + relativeIncrement)))) {
      distinctCounts.add(c);
    }
    Collections.reverse(distinctCounts);
    return distinctCounts.stream().mapToLong(Long::valueOf).toArray();
  }

  public static List<BigInt> getDistinctCountValues(double max, double relativeIncrement) {
    Preconditions.checkArgument(max >= 1.);
    List<BigInt> distinctCounts = new ArrayList<>();
    BigInt c = BigInt.ceil(max);
    while (c.isPositive()) {
      distinctCounts.add(c.copy());
      double d = c.asDouble();
      c.decrement();
      c.min(BigInt.ceil(d / (1. + relativeIncrement)));
    }
    Collections.reverse(distinctCounts);
    return distinctCounts;
  }
}
