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
package com.dynatrace.hash4j.hashing;

import static java.util.Objects.requireNonNull;

import java.util.*;
import java.util.function.ToLongFunction;

abstract class AbstractHashStream implements HashStream {

  @Override
  public HashStream putBoolean(boolean v) {
    putByte((byte) (v ? 1 : 0));
    return this;
  }

  @Override
  public HashStream putBooleans(boolean[] x) {
    return putBooleans(x, 0, x.length);
  }

  @Override
  public HashStream putBooleans(boolean[] x, int off, int len) {
    for (int i = 0; i < len; i++) {
      putBoolean(x[off + i]);
    }
    return this;
  }

  @Override
  public HashStream putBooleanArray(boolean[] x) {
    return putBooleans(x).putInt(x.length);
  }

  @Override
  public HashStream putBytes(byte[] b) {
    putBytes(b, 0, b.length);
    return this;
  }

  @Override
  public HashStream putBytes(byte[] b, int off, int len) {
    for (int i = 0; i < len; i++) {
      putByte(b[off + i]);
    }
    return this;
  }

  @Override
  public HashStream putByteArray(byte[] x) {
    return putBytes(x).putInt(x.length);
  }

  @Override
  public HashStream putChar(char v) {
    putShort((short) v);
    return this;
  }

  @Override
  public HashStream putChars(char[] x) {
    return putChars(x, 0, x.length);
  }

  @Override
  public HashStream putChars(char[] x, int off, int len) {
    for (int i = 0; i < len; i++) {
      putChar(x[off + i]);
    }
    return this;
  }

  @Override
  public HashStream putChars(CharSequence s) {
    int len = s.length();
    for (int i = 0; i < len; i++) {
      putChar(s.charAt(i));
    }
    return this;
  }

  @Override
  public HashStream putCharArray(char[] x) {
    return putChars(x).putInt(x.length);
  }

  @Override
  public HashStream putString(String s) {
    putChars(s);
    putInt(s.length());
    return this;
  }

  @Override
  public HashStream putShort(short v) {
    putByte((byte) v);
    putByte((byte) (v >>> 8));
    return this;
  }

  @Override
  public HashStream putShortArray(short[] x) {
    return putShorts(x).putInt(x.length);
  }

  @Override
  public HashStream putShorts(short[] x) {
    return putShorts(x, 0, x.length);
  }

  @Override
  public HashStream putShorts(short[] x, int off, int len) {
    for (int i = 0; i < len; i++) {
      putShort(x[off + i]);
    }
    return this;
  }

  @Override
  public HashStream putInt(int v) {
    putByte((byte) v);
    putByte((byte) (v >>> 8));
    putByte((byte) (v >>> 16));
    putByte((byte) (v >>> 24));
    return this;
  }

  @Override
  public HashStream putIntArray(int[] x) {
    return putInts(x).putInt(x.length);
  }

  @Override
  public HashStream putInts(int[] x) {
    return putInts(x, 0, x.length);
  }

  @Override
  public HashStream putInts(int[] x, int off, int len) {
    for (int i = 0; i < len; i++) {
      putInt(x[off + i]);
    }
    return this;
  }

  @Override
  public HashStream putLong(long v) {
    putInt((int) (v));
    putInt((int) (v >> 32));
    return this;
  }

  @Override
  public HashStream putLongArray(long[] x) {
    return putLongs(x).putInt(x.length);
  }

  @Override
  public HashStream putLongs(long[] x) {
    return putLongs(x, 0, x.length);
  }

  @Override
  public HashStream putLongs(long[] x, int off, int len) {
    for (int i = 0; i < len; ++i) {
      putLong(x[off + i]);
    }
    return this;
  }

  @Override
  public HashStream putFloat(float v) {
    putInt(Float.floatToRawIntBits(v));
    return this;
  }

  @Override
  public HashStream putFloats(float[] x) {
    return putFloats(x, 0, x.length);
  }

  @Override
  public HashStream putFloats(float[] x, int off, int len) {
    for (int i = 0; i < len; ++i) {
      putFloat(x[off + i]);
    }
    return this;
  }

  @Override
  public HashStream putFloatArray(float[] x) {
    return putFloats(x).putInt(x.length);
  }

  @Override
  public HashStream putDouble(double v) {
    putLong(Double.doubleToRawLongBits(v));
    return this;
  }

  @Override
  public HashStream putDoubleArray(double[] x) {
    return putDoubles(x).putInt(x.length);
  }

  @Override
  public HashStream putDoubles(double[] x) {
    return putDoubles(x, 0, x.length);
  }

  @Override
  public HashStream putDoubles(double[] x, int off, int len) {
    for (int i = 0; i < len; ++i) {
      putDouble(x[off + i]);
    }
    return this;
  }

  @Override
  public HashStream putUUID(UUID uuid) {
    putLong(uuid.getLeastSignificantBits());
    putLong(uuid.getMostSignificantBits());
    return this;
  }

  @Override
  public <T> HashStream put(T data, HashFunnel<T> funnel) {
    funnel.put(data, this);
    return this;
  }

  @Override
  public <T> HashStream putNullable(T data, HashFunnel<T> funnel) {
    if (data != null) {
      funnel.put(data, this);
      putBoolean(true);
    } else {
      putBoolean(false);
    }
    return this;
  }

  @Override
  public <T> HashStream putOrderedIterable(Iterable<T> data, HashFunnel<? super T> funnel) {
    int counter = 0;
    for (T d : data) {
      put(d, funnel);
      counter += 1;
    }
    putInt(counter);
    return this;
  }

  @Override
  public <T> HashStream putUnorderedIterable(
      Iterable<T> data, HashFunnel<? super T> funnel, Hasher64 hasher) {
    return putUnorderedIterable(data, funnel, hasher.hashStream());
  }

  private <T> HashStream putUnorderedIterable(
      Iterable<T> data, HashFunnel<? super T> funnel, HashStream64 hashStream) {
    return putUnorderedIterable(data, x -> hashStream.reset().put(x, funnel).getAsLong());
  }

  @Override
  public <T> HashStream putUnorderedIterable(
      final Iterable<T> data, final ToLongFunction<? super T> elementHashFunction) {
    requireNonNull(data);
    requireNonNull(elementHashFunction);

    if (data instanceof Collection) {
      if (data instanceof RandomAccess && data instanceof List) {
        putUnorderedRandomAccessList((List<T>) data, elementHashFunction);
      } else {
        putUnorderedCollection((Collection<T>) data, elementHashFunction);
      }
    } else {
      long[] elementHashes = new long[8]; // allocate with size 8 by default
      int counter = 0;
      for (T d : data) {
        if (counter >= elementHashes.length) {
          elementHashes = Arrays.copyOf(elementHashes, increaseArraySize(elementHashes.length));
        }
        elementHashes[counter] = elementHashFunction.applyAsLong(d);
        counter += 1;
      }
      Arrays.sort(elementHashes, 0, counter);
      putLongs(elementHashes, 0, counter);
      putInt(counter);
    }
    return this;
  }

  // visible for testing
  static int increaseArraySize(int currentSize) {
    if (currentSize <= 0x3fffffff) {
      return currentSize << 1; // increase by 100%
    } else {
      return Integer.MAX_VALUE;
    }
  }

  private void putSorted(long l0, long l1) {
    if (l1 <= l0) {
      long t = l0;
      l0 = l1;
      l1 = t;
    }
    putLong(l0);
    putLong(l1);
  }

  private void putSorted(long l0, long l1, long l2) {
    if (l0 > l1) {
      long t = l0;
      l0 = l1;
      l1 = t;
    }
    if (l0 > l2) {
      long t = l0;
      l0 = l2;
      l2 = t;
    }
    if (l1 > l2) {
      long t = l1;
      l1 = l2;
      l2 = t;
    }
    putLong(l0);
    putLong(l1);
    putLong(l2);
  }

  private void putSorted(long l0, long l1, long l2, long l3) {
    if (l0 > l1) {
      long t = l0;
      l0 = l1;
      l1 = t;
    }
    if (l2 > l3) {
      long t = l2;
      l2 = l3;
      l3 = t;
    }
    if (l0 > l2) {
      long t = l0;
      l0 = l2;
      l2 = t;
    }
    if (l1 > l3) {
      long t = l1;
      l1 = l3;
      l3 = t;
    }
    if (l1 > l2) {
      long t = l1;
      l1 = l2;
      l2 = t;
    }
    putLong(l0);
    putLong(l1);
    putLong(l2);
    putLong(l3);
  }

  private void putSorted(long l0, long l1, long l2, long l3, long l4) {
    // generated from http://pages.ripco.net/~jgamble/nw.html
    if (l0 > l1) {
      long t = l0;
      l0 = l1;
      l1 = t;
    }
    if (l3 > l4) {
      long t = l3;
      l3 = l4;
      l4 = t;
    }
    if (l2 > l4) {
      long t = l2;
      l2 = l4;
      l4 = t;
    }
    if (l2 > l3) {
      long t = l2;
      l2 = l3;
      l3 = t;
    }
    if (l1 > l4) {
      long t = l1;
      l1 = l4;
      l4 = t;
    }
    if (l0 > l3) {
      long t = l0;
      l0 = l3;
      l3 = t;
    }
    if (l0 > l2) {
      long t = l0;
      l0 = l2;
      l2 = t;
    }
    if (l1 > l3) {
      long t = l1;
      l1 = l3;
      l3 = t;
    }
    if (l1 > l2) {
      long t = l1;
      l1 = l2;
      l2 = t;
    }
    putLong(l0);
    putLong(l1);
    putLong(l2);
    putLong(l3);
    putLong(l4);
  }

  private void putSorted(long l0, long l1, long l2, long l3, long l4, long l5) {
    // generated from http://pages.ripco.net/~jgamble/nw.html
    if (l1 > l2) {
      long t = l1;
      l1 = l2;
      l2 = t;
    }
    if (l4 > l5) {
      long t = l4;
      l4 = l5;
      l5 = t;
    }
    if (l0 > l2) {
      long t = l0;
      l0 = l2;
      l2 = t;
    }
    if (l3 > l5) {
      long t = l3;
      l3 = l5;
      l5 = t;
    }
    if (l0 > l1) {
      long t = l0;
      l0 = l1;
      l1 = t;
    }
    if (l3 > l4) {
      long t = l3;
      l3 = l4;
      l4 = t;
    }
    if (l2 > l5) {
      long t = l2;
      l2 = l5;
      l5 = t;
    }
    if (l0 > l3) {
      long t = l0;
      l0 = l3;
      l3 = t;
    }
    if (l1 > l4) {
      long t = l1;
      l1 = l4;
      l4 = t;
    }
    if (l2 > l4) {
      long t = l2;
      l2 = l4;
      l4 = t;
    }
    if (l1 > l3) {
      long t = l1;
      l1 = l3;
      l3 = t;
    }
    if (l2 > l3) {
      long t = l2;
      l2 = l3;
      l3 = t;
    }
    putLong(l0);
    putLong(l1);
    putLong(l2);
    putLong(l3);
    putLong(l4);
    putLong(l5);
  }

  private void putSorted(long l0, long l1, long l2, long l3, long l4, long l5, long l6) {
    // generated from http://pages.ripco.net/~jgamble/nw.html
    if (l1 > l2) {
      long t = l1;
      l1 = l2;
      l2 = t;
    }
    if (l3 > l4) {
      long t = l3;
      l3 = l4;
      l4 = t;
    }
    if (l5 > l6) {
      long t = l5;
      l5 = l6;
      l6 = t;
    }
    if (l0 > l2) {
      long t = l0;
      l0 = l2;
      l2 = t;
    }
    if (l3 > l5) {
      long t = l3;
      l3 = l5;
      l5 = t;
    }
    if (l4 > l6) {
      long t = l4;
      l4 = l6;
      l6 = t;
    }
    if (l0 > l1) {
      long t = l0;
      l0 = l1;
      l1 = t;
    }
    if (l4 > l5) {
      long t = l4;
      l4 = l5;
      l5 = t;
    }
    if (l2 > l6) {
      long t = l2;
      l2 = l6;
      l6 = t;
    }
    if (l0 > l4) {
      long t = l0;
      l0 = l4;
      l4 = t;
    }
    if (l1 > l5) {
      long t = l1;
      l1 = l5;
      l5 = t;
    }
    if (l0 > l3) {
      long t = l0;
      l0 = l3;
      l3 = t;
    }
    if (l2 > l5) {
      long t = l2;
      l2 = l5;
      l5 = t;
    }
    if (l1 > l3) {
      long t = l1;
      l1 = l3;
      l3 = t;
    }
    if (l2 > l4) {
      long t = l2;
      l2 = l4;
      l4 = t;
    }
    if (l2 > l3) {
      long t = l2;
      l2 = l3;
      l3 = t;
    }
    putLong(l0);
    putLong(l1);
    putLong(l2);
    putLong(l3);
    putLong(l4);
    putLong(l5);
    putLong(l6);
  }

  private void putSorted(long l0, long l1, long l2, long l3, long l4, long l5, long l6, long l7) {
    // generated from http://pages.ripco.net/~jgamble/nw.html
    if (l0 > l1) {
      long t = l0;
      l0 = l1;
      l1 = t;
    }
    if (l2 > l3) {
      long t = l2;
      l2 = l3;
      l3 = t;
    }
    if (l4 > l5) {
      long t = l4;
      l4 = l5;
      l5 = t;
    }
    if (l6 > l7) {
      long t = l6;
      l6 = l7;
      l7 = t;
    }
    if (l0 > l2) {
      long t = l0;
      l0 = l2;
      l2 = t;
    }
    if (l1 > l3) {
      long t = l1;
      l1 = l3;
      l3 = t;
    }
    if (l4 > l6) {
      long t = l4;
      l4 = l6;
      l6 = t;
    }
    if (l5 > l7) {
      long t = l5;
      l5 = l7;
      l7 = t;
    }
    if (l1 > l2) {
      long t = l1;
      l1 = l2;
      l2 = t;
    }
    if (l5 > l6) {
      long t = l5;
      l5 = l6;
      l6 = t;
    }
    if (l0 > l4) {
      long t = l0;
      l0 = l4;
      l4 = t;
    }
    if (l3 > l7) {
      long t = l3;
      l3 = l7;
      l7 = t;
    }
    if (l1 > l5) {
      long t = l1;
      l1 = l5;
      l5 = t;
    }
    if (l2 > l6) {
      long t = l2;
      l2 = l6;
      l6 = t;
    }
    if (l1 > l4) {
      long t = l1;
      l1 = l4;
      l4 = t;
    }
    if (l3 > l6) {
      long t = l3;
      l3 = l6;
      l6 = t;
    }
    if (l2 > l4) {
      long t = l2;
      l2 = l4;
      l4 = t;
    }
    if (l3 > l5) {
      long t = l3;
      l3 = l5;
      l5 = t;
    }
    if (l3 > l4) {
      long t = l3;
      l3 = l4;
      l4 = t;
    }
    putLong(l0);
    putLong(l1);
    putLong(l2);
    putLong(l3);
    putLong(l4);
    putLong(l5);
    putLong(l6);
    putLong(l7);
  }

  private void putSorted(
      long l0, long l1, long l2, long l3, long l4, long l5, long l6, long l7, long l8) {
    // generated from http://pages.ripco.net/~jgamble/nw.html
    if (l0 > l1) {
      long t = l0;
      l0 = l1;
      l1 = t;
    }
    if (l3 > l4) {
      long t = l3;
      l3 = l4;
      l4 = t;
    }
    if (l6 > l7) {
      long t = l6;
      l6 = l7;
      l7 = t;
    }
    if (l1 > l2) {
      long t = l1;
      l1 = l2;
      l2 = t;
    }
    if (l4 > l5) {
      long t = l4;
      l4 = l5;
      l5 = t;
    }
    if (l7 > l8) {
      long t = l7;
      l7 = l8;
      l8 = t;
    }
    if (l0 > l1) {
      long t = l0;
      l0 = l1;
      l1 = t;
    }
    if (l3 > l4) {
      long t = l3;
      l3 = l4;
      l4 = t;
    }
    if (l6 > l7) {
      long t = l6;
      l6 = l7;
      l7 = t;
    }
    if (l2 > l5) {
      long t = l2;
      l2 = l5;
      l5 = t;
    }
    if (l0 > l3) {
      long t = l0;
      l0 = l3;
      l3 = t;
    }
    if (l1 > l4) {
      long t = l1;
      l1 = l4;
      l4 = t;
    }
    if (l5 > l8) {
      long t = l5;
      l5 = l8;
      l8 = t;
    }
    if (l3 > l6) {
      long t = l3;
      l3 = l6;
      l6 = t;
    }
    if (l4 > l7) {
      long t = l4;
      l4 = l7;
      l7 = t;
    }
    if (l2 > l5) {
      long t = l2;
      l2 = l5;
      l5 = t;
    }
    if (l0 > l3) {
      long t = l0;
      l0 = l3;
      l3 = t;
    }
    if (l1 > l4) {
      long t = l1;
      l1 = l4;
      l4 = t;
    }
    if (l5 > l7) {
      long t = l5;
      l5 = l7;
      l7 = t;
    }
    if (l2 > l6) {
      long t = l2;
      l2 = l6;
      l6 = t;
    }
    if (l1 > l3) {
      long t = l1;
      l1 = l3;
      l3 = t;
    }
    if (l4 > l6) {
      long t = l4;
      l4 = l6;
      l6 = t;
    }
    if (l2 > l4) {
      long t = l2;
      l2 = l4;
      l4 = t;
    }
    if (l5 > l6) {
      long t = l5;
      l5 = l6;
      l6 = t;
    }
    if (l2 > l3) {
      long t = l2;
      l2 = l3;
      l3 = t;
    }
    putLong(l0);
    putLong(l1);
    putLong(l2);
    putLong(l3);
    putLong(l4);
    putLong(l5);
    putLong(l6);
    putLong(l7);
    putLong(l8);
  }

  private void putSorted(
      long l0, long l1, long l2, long l3, long l4, long l5, long l6, long l7, long l8, long l9) {
    // generated from http://pages.ripco.net/~jgamble/nw.html
    if (l4 > l9) {
      long t = l4;
      l4 = l9;
      l9 = t;
    }
    if (l3 > l8) {
      long t = l3;
      l3 = l8;
      l8 = t;
    }
    if (l2 > l7) {
      long t = l2;
      l2 = l7;
      l7 = t;
    }
    if (l1 > l6) {
      long t = l1;
      l1 = l6;
      l6 = t;
    }
    if (l0 > l5) {
      long t = l0;
      l0 = l5;
      l5 = t;
    }
    if (l1 > l4) {
      long t = l1;
      l1 = l4;
      l4 = t;
    }
    if (l6 > l9) {
      long t = l6;
      l6 = l9;
      l9 = t;
    }
    if (l0 > l3) {
      long t = l0;
      l0 = l3;
      l3 = t;
    }
    if (l5 > l8) {
      long t = l5;
      l5 = l8;
      l8 = t;
    }
    if (l0 > l2) {
      long t = l0;
      l0 = l2;
      l2 = t;
    }
    if (l3 > l6) {
      long t = l3;
      l3 = l6;
      l6 = t;
    }
    if (l7 > l9) {
      long t = l7;
      l7 = l9;
      l9 = t;
    }
    if (l0 > l1) {
      long t = l0;
      l0 = l1;
      l1 = t;
    }
    if (l2 > l4) {
      long t = l2;
      l2 = l4;
      l4 = t;
    }
    if (l5 > l7) {
      long t = l5;
      l5 = l7;
      l7 = t;
    }
    if (l8 > l9) {
      long t = l8;
      l8 = l9;
      l9 = t;
    }
    if (l1 > l2) {
      long t = l1;
      l1 = l2;
      l2 = t;
    }
    if (l4 > l6) {
      long t = l4;
      l4 = l6;
      l6 = t;
    }
    if (l7 > l8) {
      long t = l7;
      l7 = l8;
      l8 = t;
    }
    if (l3 > l5) {
      long t = l3;
      l3 = l5;
      l5 = t;
    }
    if (l2 > l5) {
      long t = l2;
      l2 = l5;
      l5 = t;
    }
    if (l6 > l8) {
      long t = l6;
      l6 = l8;
      l8 = t;
    }
    if (l1 > l3) {
      long t = l1;
      l1 = l3;
      l3 = t;
    }
    if (l4 > l7) {
      long t = l4;
      l4 = l7;
      l7 = t;
    }
    if (l2 > l3) {
      long t = l2;
      l2 = l3;
      l3 = t;
    }
    if (l6 > l7) {
      long t = l6;
      l6 = l7;
      l7 = t;
    }
    if (l3 > l4) {
      long t = l3;
      l3 = l4;
      l4 = t;
    }
    if (l5 > l6) {
      long t = l5;
      l5 = l6;
      l6 = t;
    }
    if (l4 > l5) {
      long t = l4;
      l4 = l5;
      l5 = t;
    }
    putLong(l0);
    putLong(l1);
    putLong(l2);
    putLong(l3);
    putLong(l4);
    putLong(l5);
    putLong(l6);
    putLong(l7);
    putLong(l8);
    putLong(l9);
  }

  private <T> void putUnorderedRandomAccessList(
      final List<T> data, final ToLongFunction<? super T> elementHasher) {

    int size = data.size();

    // for data sizes up to 10 there are fast implementations to avoid the allocation of an array
    // used for sorting
    switch (size) {
      case 0:
        break;
      case 1:
        {
          long elementHash0 = elementHasher.applyAsLong(data.get(0));
          putLong(elementHash0);
        }
        break;
      case 2:
        {
          long elementHash0 = elementHasher.applyAsLong(data.get(0));
          long elementHash1 = elementHasher.applyAsLong(data.get(1));
          putSorted(elementHash0, elementHash1);
        }
        break;
      case 3:
        {
          long elementHash0 = elementHasher.applyAsLong(data.get(0));
          long elementHash1 = elementHasher.applyAsLong(data.get(1));
          long elementHash2 = elementHasher.applyAsLong(data.get(2));
          putSorted(elementHash0, elementHash1, elementHash2);
        }
        break;
      case 4:
        {
          long elementHash0 = elementHasher.applyAsLong(data.get(0));
          long elementHash1 = elementHasher.applyAsLong(data.get(1));
          long elementHash2 = elementHasher.applyAsLong(data.get(2));
          long elementHash3 = elementHasher.applyAsLong(data.get(3));
          putSorted(elementHash0, elementHash1, elementHash2, elementHash3);
        }
        break;
      case 5:
        {
          long elementHash0 = elementHasher.applyAsLong(data.get(0));
          long elementHash1 = elementHasher.applyAsLong(data.get(1));
          long elementHash2 = elementHasher.applyAsLong(data.get(2));
          long elementHash3 = elementHasher.applyAsLong(data.get(3));
          long elementHash4 = elementHasher.applyAsLong(data.get(4));
          putSorted(elementHash0, elementHash1, elementHash2, elementHash3, elementHash4);
        }
        break;
      case 6:
        {
          long elementHash0 = elementHasher.applyAsLong(data.get(0));
          long elementHash1 = elementHasher.applyAsLong(data.get(1));
          long elementHash2 = elementHasher.applyAsLong(data.get(2));
          long elementHash3 = elementHasher.applyAsLong(data.get(3));
          long elementHash4 = elementHasher.applyAsLong(data.get(4));
          long elementHash5 = elementHasher.applyAsLong(data.get(5));
          putSorted(
              elementHash0, elementHash1, elementHash2, elementHash3, elementHash4, elementHash5);
        }
        break;
      case 7:
        {
          long elementHash0 = elementHasher.applyAsLong(data.get(0));
          long elementHash1 = elementHasher.applyAsLong(data.get(1));
          long elementHash2 = elementHasher.applyAsLong(data.get(2));
          long elementHash3 = elementHasher.applyAsLong(data.get(3));
          long elementHash4 = elementHasher.applyAsLong(data.get(4));
          long elementHash5 = elementHasher.applyAsLong(data.get(5));
          long elementHash6 = elementHasher.applyAsLong(data.get(6));
          putSorted(
              elementHash0,
              elementHash1,
              elementHash2,
              elementHash3,
              elementHash4,
              elementHash5,
              elementHash6);
        }
        break;
      case 8:
        {
          long elementHash0 = elementHasher.applyAsLong(data.get(0));
          long elementHash1 = elementHasher.applyAsLong(data.get(1));
          long elementHash2 = elementHasher.applyAsLong(data.get(2));
          long elementHash3 = elementHasher.applyAsLong(data.get(3));
          long elementHash4 = elementHasher.applyAsLong(data.get(4));
          long elementHash5 = elementHasher.applyAsLong(data.get(5));
          long elementHash6 = elementHasher.applyAsLong(data.get(6));
          long elementHash7 = elementHasher.applyAsLong(data.get(7));
          putSorted(
              elementHash0,
              elementHash1,
              elementHash2,
              elementHash3,
              elementHash4,
              elementHash5,
              elementHash6,
              elementHash7);
        }
        break;
      case 9:
        {
          long elementHash0 = elementHasher.applyAsLong(data.get(0));
          long elementHash1 = elementHasher.applyAsLong(data.get(1));
          long elementHash2 = elementHasher.applyAsLong(data.get(2));
          long elementHash3 = elementHasher.applyAsLong(data.get(3));
          long elementHash4 = elementHasher.applyAsLong(data.get(4));
          long elementHash5 = elementHasher.applyAsLong(data.get(5));
          long elementHash6 = elementHasher.applyAsLong(data.get(6));
          long elementHash7 = elementHasher.applyAsLong(data.get(7));
          long elementHash8 = elementHasher.applyAsLong(data.get(8));
          putSorted(
              elementHash0,
              elementHash1,
              elementHash2,
              elementHash3,
              elementHash4,
              elementHash5,
              elementHash6,
              elementHash7,
              elementHash8);
        }
        break;
      case 10:
        {
          long elementHash0 = elementHasher.applyAsLong(data.get(0));
          long elementHash1 = elementHasher.applyAsLong(data.get(1));
          long elementHash2 = elementHasher.applyAsLong(data.get(2));
          long elementHash3 = elementHasher.applyAsLong(data.get(3));
          long elementHash4 = elementHasher.applyAsLong(data.get(4));
          long elementHash5 = elementHasher.applyAsLong(data.get(5));
          long elementHash6 = elementHasher.applyAsLong(data.get(6));
          long elementHash7 = elementHasher.applyAsLong(data.get(7));
          long elementHash8 = elementHasher.applyAsLong(data.get(8));
          long elementHash9 = elementHasher.applyAsLong(data.get(9));
          putSorted(
              elementHash0,
              elementHash1,
              elementHash2,
              elementHash3,
              elementHash4,
              elementHash5,
              elementHash6,
              elementHash7,
              elementHash8,
              elementHash9);
        }
        break;
      default:
        {
          long[] elementHashes = new long[size];
          for (int i = 0; i < size; ++i) {
            elementHashes[i] = elementHasher.applyAsLong(data.get(i));
          }
          Arrays.sort(elementHashes, 0, size);
          putLongs(elementHashes, 0, size);
        }
    }
    putInt(size);
  }

  private <T> void putUnorderedCollection(
      final Collection<T> data, final ToLongFunction<? super T> elementHasher) {

    int size = data.size();

    // for data sizes up to 10 there are fast implementations to avoid the allocation of an array
    // used for sorting
    switch (size) {
      case 0:
        break;
      case 1:
        {
          Iterator<T> it = data.iterator();
          long elementHash0 = elementHasher.applyAsLong(it.next());
          putLong(elementHash0);
        }
        break;
      case 2:
        {
          Iterator<T> it = data.iterator();
          long elementHash0 = elementHasher.applyAsLong(it.next());
          long elementHash1 = elementHasher.applyAsLong(it.next());
          putSorted(elementHash0, elementHash1);
        }
        break;
      case 3:
        {
          Iterator<T> it = data.iterator();
          long elementHash0 = elementHasher.applyAsLong(it.next());
          long elementHash1 = elementHasher.applyAsLong(it.next());
          long elementHash2 = elementHasher.applyAsLong(it.next());
          putSorted(elementHash0, elementHash1, elementHash2);
        }
        break;
      case 4:
        {
          Iterator<T> it = data.iterator();
          long elementHash0 = elementHasher.applyAsLong(it.next());
          long elementHash1 = elementHasher.applyAsLong(it.next());
          long elementHash2 = elementHasher.applyAsLong(it.next());
          long elementHash3 = elementHasher.applyAsLong(it.next());
          putSorted(elementHash0, elementHash1, elementHash2, elementHash3);
        }
        break;
      case 5:
        {
          Iterator<T> it = data.iterator();
          long elementHash0 = elementHasher.applyAsLong(it.next());
          long elementHash1 = elementHasher.applyAsLong(it.next());
          long elementHash2 = elementHasher.applyAsLong(it.next());
          long elementHash3 = elementHasher.applyAsLong(it.next());
          long elementHash4 = elementHasher.applyAsLong(it.next());
          putSorted(elementHash0, elementHash1, elementHash2, elementHash3, elementHash4);
        }
        break;
      case 6:
        {
          Iterator<T> it = data.iterator();
          long elementHash0 = elementHasher.applyAsLong(it.next());
          long elementHash1 = elementHasher.applyAsLong(it.next());
          long elementHash2 = elementHasher.applyAsLong(it.next());
          long elementHash3 = elementHasher.applyAsLong(it.next());
          long elementHash4 = elementHasher.applyAsLong(it.next());
          long elementHash5 = elementHasher.applyAsLong(it.next());
          putSorted(
              elementHash0, elementHash1, elementHash2, elementHash3, elementHash4, elementHash5);
        }
        break;
      case 7:
        {
          Iterator<T> it = data.iterator();
          long elementHash0 = elementHasher.applyAsLong(it.next());
          long elementHash1 = elementHasher.applyAsLong(it.next());
          long elementHash2 = elementHasher.applyAsLong(it.next());
          long elementHash3 = elementHasher.applyAsLong(it.next());
          long elementHash4 = elementHasher.applyAsLong(it.next());
          long elementHash5 = elementHasher.applyAsLong(it.next());
          long elementHash6 = elementHasher.applyAsLong(it.next());
          putSorted(
              elementHash0,
              elementHash1,
              elementHash2,
              elementHash3,
              elementHash4,
              elementHash5,
              elementHash6);
        }
        break;
      case 8:
        {
          Iterator<T> it = data.iterator();
          long elementHash0 = elementHasher.applyAsLong(it.next());
          long elementHash1 = elementHasher.applyAsLong(it.next());
          long elementHash2 = elementHasher.applyAsLong(it.next());
          long elementHash3 = elementHasher.applyAsLong(it.next());
          long elementHash4 = elementHasher.applyAsLong(it.next());
          long elementHash5 = elementHasher.applyAsLong(it.next());
          long elementHash6 = elementHasher.applyAsLong(it.next());
          long elementHash7 = elementHasher.applyAsLong(it.next());
          putSorted(
              elementHash0,
              elementHash1,
              elementHash2,
              elementHash3,
              elementHash4,
              elementHash5,
              elementHash6,
              elementHash7);
        }
        break;
      case 9:
        {
          Iterator<T> it = data.iterator();
          long elementHash0 = elementHasher.applyAsLong(it.next());
          long elementHash1 = elementHasher.applyAsLong(it.next());
          long elementHash2 = elementHasher.applyAsLong(it.next());
          long elementHash3 = elementHasher.applyAsLong(it.next());
          long elementHash4 = elementHasher.applyAsLong(it.next());
          long elementHash5 = elementHasher.applyAsLong(it.next());
          long elementHash6 = elementHasher.applyAsLong(it.next());
          long elementHash7 = elementHasher.applyAsLong(it.next());
          long elementHash8 = elementHasher.applyAsLong(it.next());
          putSorted(
              elementHash0,
              elementHash1,
              elementHash2,
              elementHash3,
              elementHash4,
              elementHash5,
              elementHash6,
              elementHash7,
              elementHash8);
        }
        break;
      case 10:
        {
          Iterator<T> it = data.iterator();
          long elementHash0 = elementHasher.applyAsLong(it.next());
          long elementHash1 = elementHasher.applyAsLong(it.next());
          long elementHash2 = elementHasher.applyAsLong(it.next());
          long elementHash3 = elementHasher.applyAsLong(it.next());
          long elementHash4 = elementHasher.applyAsLong(it.next());
          long elementHash5 = elementHasher.applyAsLong(it.next());
          long elementHash6 = elementHasher.applyAsLong(it.next());
          long elementHash7 = elementHasher.applyAsLong(it.next());
          long elementHash8 = elementHasher.applyAsLong(it.next());
          long elementHash9 = elementHasher.applyAsLong(it.next());
          putSorted(
              elementHash0,
              elementHash1,
              elementHash2,
              elementHash3,
              elementHash4,
              elementHash5,
              elementHash6,
              elementHash7,
              elementHash8,
              elementHash9);
        }
        break;
      default:
        {
          Iterator<T> it = data.iterator();
          long[] elementHashes = new long[size];
          for (int i = 0; i < size; ++i) {
            elementHashes[i] = elementHasher.applyAsLong(it.next());
          }
          Arrays.sort(elementHashes, 0, size);
          putLongs(elementHashes, 0, size);
        }
    }
    putInt(size);
  }

  @Override
  public <T> HashStream putOptional(Optional<T> obj, HashFunnel<? super T> funnel) {
    if (obj.isPresent()) {
      put(obj.get(), funnel);
      putBoolean(true);
    } else {
      putBoolean(false);
    }
    return this;
  }

  @Override
  public HashStream putOptionalInt(OptionalInt v) {
    if (v.isPresent()) {
      putInt(v.getAsInt());
      putBoolean(true);
    } else {
      putBoolean(false);
    }
    return this;
  }

  @Override
  public HashStream putOptionalLong(OptionalLong v) {
    if (v.isPresent()) {
      putLong(v.getAsLong());
      putBoolean(true);
    } else {
      putBoolean(false);
    }
    return this;
  }

  @Override
  public HashStream putOptionalDouble(OptionalDouble v) {
    if (v.isPresent()) {
      putDouble(v.getAsDouble());
      putBoolean(true);
    } else {
      putBoolean(false);
    }
    return this;
  }
}
