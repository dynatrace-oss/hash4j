/*
 * Copyright 2022-2024 Dynatrace LLC
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

/** A hash stream computing a 64-bit hash value. */
public interface HashStream64 extends HashStream32 {

  /**
   * Returns a 64-bit hash value.
   *
   * <p>{@code (int)getAsLong()} will be equivalent to {@link #getAsInt()}.
   *
   * @return a 64-bit hash value
   */
  long getAsLong();

  @Override
  HashStream64 putByte(byte v);

  @Override
  HashStream64 putBytes(byte[] x);

  @Override
  HashStream64 putBytes(byte[] x, int off, int len);

  @Override
  HashStream64 putByteArray(byte[] x);

  @Override
  HashStream64 putBoolean(boolean v);

  @Override
  HashStream64 putBooleans(boolean[] x);

  @Override
  HashStream64 putBooleans(boolean[] x, int off, int len);

  @Override
  HashStream64 putBooleanArray(boolean[] x);

  @Override
  HashStream64 putShort(short v);

  @Override
  HashStream64 putShorts(short[] x);

  @Override
  HashStream64 putShorts(short[] x, int off, int len);

  @Override
  HashStream64 putShortArray(short[] x);

  @Override
  HashStream64 putChar(char v);

  @Override
  HashStream64 putChars(char[] x);

  @Override
  HashStream64 putChars(char[] x, int off, int len);

  @Override
  HashStream64 putChars(CharSequence c);

  @Override
  HashStream64 putCharArray(char[] x);

  @Override
  HashStream64 putString(String s);

  @Override
  HashStream64 putInt(int v);

  @Override
  HashStream64 putInts(int[] x);

  @Override
  HashStream64 putInts(int[] x, int off, int len);

  @Override
  HashStream64 putIntArray(int[] x);

  @Override
  HashStream64 putLong(long v);

  @Override
  HashStream64 putLongs(long[] x);

  @Override
  HashStream64 putLongs(long[] x, int off, int len);

  @Override
  HashStream64 putLongArray(long[] x);

  @Override
  HashStream64 putFloat(float v);

  @Override
  HashStream64 putFloats(float[] x);

  @Override
  HashStream64 putFloats(float[] x, int off, int len);

  @Override
  HashStream64 putFloatArray(float[] x);

  @Override
  HashStream64 putDouble(double v);

  @Override
  HashStream64 putDoubles(double[] x);

  @Override
  HashStream64 putDoubles(double[] x, int off, int len);

  @Override
  HashStream64 putDoubleArray(double[] x);

  @Override
  HashStream64 putUUID(UUID uuid);

  @Override
  <T> HashStream64 put(T obj, HashFunnel<T> funnel);

  @Override
  <T> HashStream64 putNullable(T obj, HashFunnel<T> funnel);

  @Override
  <T> HashStream64 putOrderedIterable(Iterable<T> data, HashFunnel<? super T> funnel);

  @Override
  <T> HashStream64 putUnorderedIterable(
      Iterable<T> data, ToLongFunction<? super T> elementHashFunction);

  @Override
  <T> HashStream64 putUnorderedIterable(
      Iterable<T> data, HashFunnel<? super T> funnel, Hasher64 hasher);

  @Override
  <T> HashStream64 putOptional(Optional<T> obj, HashFunnel<? super T> funnel);

  @Override
  HashStream64 putOptionalInt(OptionalInt v);

  @Override
  HashStream64 putOptionalLong(OptionalLong v);

  @Override
  HashStream64 putOptionalDouble(OptionalDouble v);

  @Override
  HashStream64 reset();

  @Override
  HashStream64 copy();

  /**
   * Resets this hash stream and returns a 64-bit hash value of the given object using the given
   * funnel.
   *
   * <p>Equivalent to {@code funnel.put(obj, reset()); return getAsLong();}
   *
   * @param obj the object
   * @param funnel the funnel
   * @param <T> the type
   * @return a 64-bit hash value
   */
  <T> long resetAndHashToLong(T obj, HashFunnel<T> funnel);
}
