/*
 * Copyright 2025-2026 Dynatrace LLC
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

import com.dynatrace.hash4j.internal.Preconditions;
import java.util.Arrays;

final class BinaryMatrix {

  private final int dim;
  private final int[] values;

  private BinaryMatrix(int dim, int[] values) {
    Preconditions.checkArgument(dim * dim == values.length);
    this.values = values;
    this.dim = dim;
  }

  static BinaryMatrix zero(int dim) {
    return new BinaryMatrix(dim, new int[dim * dim]);
  }

  static BinaryMatrix identity(int dim) {
    int[] a = new int[dim * dim];
    int j = 0;
    for (int i = 0; i < dim; ++i) {
      a[j] = 1;
      j += dim + 1;
    }
    return new BinaryMatrix(dim, a);
  }

  static BinaryMatrix lsh(int dim) {
    int[] a = new int[dim * dim];
    for (int i = 0; i < dim - 1; ++i) {
      a[getIndex(dim, i, i + 1)] = 1;
    }
    return new BinaryMatrix(dim, a);
  }

  static BinaryMatrix rsh(int dim) {
    int[] a = new int[dim * dim];
    for (int i = 0; i < dim - 1; ++i) {
      a[getIndex(dim, i + 1, i)] = 1;
    }
    return new BinaryMatrix(dim, a);
  }

  static int getIndex(int dim, int row, int col) {
    return dim * row + col;
  }

  int getValue(int row, int col) {
    return values[getIndex(dim, row, col)];
  }

  static BinaryMatrix mul(BinaryMatrix m1, BinaryMatrix m2) {
    Preconditions.checkArgument(m1.dim == m2.dim);
    int dim = m1.dim;
    int[] result = new int[dim * dim];
    int c = 0;
    for (int i = 0; i < dim; ++i) {
      for (int k = 0; k < dim; ++k) {
        int x = 0;
        int j1 = dim * i;
        int j2 = k;
        for (int j = 0; j < dim; ++j) {
          x ^= m1.values[j1] & m2.values[j2];
          j2 += dim;
          j1 += 1;
        }
        result[c] = x;
        c += 1;
      }
    }
    return new BinaryMatrix(dim, result);
  }

  static BinaryMatrix add(BinaryMatrix m1, BinaryMatrix m2) {
    Preconditions.checkArgument(m1.dim == m2.dim);
    int dim = m1.dim;
    int[] result = new int[dim * dim];
    for (int i = 0; i < result.length; ++i) {
      result[i] = m1.values[i] ^ m2.values[i];
    }
    return new BinaryMatrix(dim, result);
  }

  BinaryMatrix copy() {
    return new BinaryMatrix(dim, Arrays.copyOf(this.values, dim * dim));
  }

  static BinaryMatrix pow(BinaryMatrix m, long unsignedPow) {
    BinaryMatrix result = BinaryMatrix.identity(m.dim);
    while (true) {
      if ((unsignedPow & 1) != 0) {
        result = mul(result, m);
      }
      unsignedPow >>>= 1;
      if (unsignedPow == 0) break;
      m = mul(m, m);
    }
    return result;
  }

  static boolean equals(BinaryMatrix m1, BinaryMatrix m2) {
    Preconditions.checkArgument(m1.dim == m2.dim);
    int dim = m1.dim;
    for (int i = 0; i < dim * dim; ++i) {
      if ((m1.values[i] & 1) != (m2.values[i] & 1)) return false;
    }
    return true;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    sb.append('[');
    for (int r = 0; r < dim; ++r) {
      sb.append('[');
      for (int c = 0; c < dim; ++c) {
        sb.append(((getValue(r, c) & 1) == 0) ? '0' : '1');
        if (c != dim - 1) sb.append(", ");
      }
      sb.append(']');
      if (r != dim - 1) sb.append(", ");
    }
    sb.append(']');
    return sb.toString();
  }

  boolean isIdentity() {
    int c = 0;
    for (int i = 0; i < dim; ++i) {
      for (int j = 0; j < dim; ++j) {
        int val = values[c] & 1;
        c += 1;
        if (i == j) {
          if (val != 1) return false;
        } else {
          if (val != 0) return false;
        }
      }
    }
    return true;
  }

  public int dim() {
    return dim;
  }
}
