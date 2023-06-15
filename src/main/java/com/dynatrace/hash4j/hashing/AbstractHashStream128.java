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
package com.dynatrace.hash4j.hashing;

import java.util.*;
import java.util.function.ToLongFunction;

abstract class AbstractHashStream128 extends AbstractHashStream64 implements HashStream128 {

  @Override
  public int getHashBitSize() {
    return 128;
  }

  @Override
  public long getAsLong() {
    return get().getAsLong();
  }

  @Override
  public HashStream128 putBoolean(boolean v) {
    super.putBoolean(v);
    return this;
  }

  @Override
  public HashStream128 putBooleans(boolean[] x) {
    super.putBooleans(x);
    return this;
  }

  @Override
  public HashStream128 putBooleans(boolean[] x, int off, int len) {
    super.putBooleans(x, off, len);
    return this;
  }

  @Override
  public HashStream128 putBooleanArray(boolean[] x) {
    super.putBooleanArray(x);
    return this;
  }

  @Override
  public HashStream128 putBytes(byte[] b) {
    super.putBytes(b);
    return this;
  }

  @Override
  public HashStream128 putBytes(byte[] b, int off, int len) {
    super.putBytes(b, off, len);
    return this;
  }

  @Override
  public HashStream128 putByteArray(byte[] x) {
    super.putByteArray(x);
    return this;
  }

  @Override
  public HashStream128 putChar(char v) {
    super.putChar(v);
    return this;
  }

  @Override
  public HashStream128 putChars(char[] x) {
    super.putChars(x);
    return this;
  }

  @Override
  public HashStream128 putChars(char[] x, int off, int len) {
    super.putChars(x, off, len);
    return this;
  }

  @Override
  public HashStream128 putChars(CharSequence s) {
    super.putChars(s);
    return this;
  }

  @Override
  public HashStream128 putCharArray(char[] x) {
    super.putCharArray(x);
    return this;
  }

  @Override
  public HashStream128 putString(String s) {
    super.putString(s);
    return this;
  }

  @Override
  public HashStream128 putShort(short v) {
    super.putShort(v);
    return this;
  }

  @Override
  public HashStream128 putShortArray(short[] x) {
    super.putShortArray(x);
    return this;
  }

  @Override
  public HashStream128 putShorts(short[] x) {
    super.putShorts(x);
    return this;
  }

  @Override
  public HashStream128 putShorts(short[] x, int off, int len) {
    super.putShorts(x, off, len);
    return this;
  }

  @Override
  public HashStream128 putInt(int v) {
    super.putInt(v);
    return this;
  }

  @Override
  public HashStream128 putIntArray(int[] x) {
    super.putIntArray(x);
    return this;
  }

  @Override
  public HashStream128 putInts(int[] x) {
    super.putInts(x);
    return this;
  }

  @Override
  public HashStream128 putInts(int[] x, int off, int len) {
    super.putInts(x, off, len);
    return this;
  }

  @Override
  public HashStream128 putLong(long v) {
    super.putLong(v);
    return this;
  }

  @Override
  public HashStream128 putLongArray(long[] x) {
    super.putLongArray(x);
    return this;
  }

  @Override
  public HashStream128 putLongs(long[] x) {
    super.putLongs(x);
    return this;
  }

  @Override
  public HashStream128 putLongs(long[] x, int off, int len) {
    super.putLongs(x, off, len);
    return this;
  }

  @Override
  public HashStream128 putFloat(float v) {
    super.putFloat(v);
    return this;
  }

  @Override
  public HashStream128 putFloats(float[] x) {
    super.putFloats(x);
    return this;
  }

  @Override
  public HashStream128 putFloats(float[] x, int off, int len) {
    super.putFloats(x, off, len);
    return this;
  }

  @Override
  public HashStream128 putFloatArray(float[] x) {
    super.putFloatArray(x);
    return this;
  }

  @Override
  public HashStream128 putDouble(double v) {
    super.putDouble(v);
    return this;
  }

  @Override
  public HashStream128 putDoubleArray(double[] x) {
    super.putDoubleArray(x);
    return this;
  }

  @Override
  public HashStream128 putDoubles(double[] x) {
    super.putDoubles(x);
    return this;
  }

  @Override
  public HashStream128 putDoubles(double[] x, int off, int len) {
    super.putDoubles(x, off, len);
    return this;
  }

  @Override
  public HashStream128 putUUID(UUID uuid) {
    super.putUUID(uuid);
    return this;
  }

  @Override
  public <T> HashStream128 put(T data, HashFunnel<T> funnel) {
    super.put(data, funnel);
    return this;
  }

  @Override
  public <T> HashStream128 putNullable(T data, HashFunnel<T> funnel) {
    super.putNullable(data, funnel);
    return this;
  }

  @Override
  public <T> HashStream128 putOrderedIterable(Iterable<T> data, HashFunnel<? super T> funnel) {
    super.putOrderedIterable(data, funnel);
    return this;
  }

  @Override
  public <T> HashStream128 putUnorderedIterable(
      Iterable<T> data, HashFunnel<? super T> funnel, Hasher64 hasher) {
    super.putUnorderedIterable(data, funnel, hasher);
    return this;
  }

  @Override
  public <T> HashStream128 putUnorderedIterable(
      Iterable<T> data, ToLongFunction<? super T> elementHashFunction) {
    super.putUnorderedIterable(data, elementHashFunction);
    return this;
  }

  @Override
  public <T> HashStream128 putOptional(Optional<T> obj, HashFunnel<? super T> funnel) {
    super.putOptional(obj, funnel);
    return this;
  }

  @Override
  public HashStream128 putOptionalInt(OptionalInt v) {
    super.putOptionalInt(v);
    return this;
  }

  @Override
  public HashStream128 putOptionalLong(OptionalLong v) {
    super.putOptionalLong(v);
    return this;
  }

  @Override
  public HashStream128 putOptionalDouble(OptionalDouble v) {
    super.putOptionalDouble(v);
    return this;
  }
}
