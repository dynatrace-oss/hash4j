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

/** A hash stream computing a 32-bit hash value. */
public interface HashStream32 extends HashStream {

  /**
   * Returns a 32-bit hash value.
   *
   * @return a 32-bit hash value
   */
  int getAsInt();

  @Override
  HashStream32 putByte(byte v);

  @Override
  HashStream32 putBytes(byte[] x);

  @Override
  HashStream32 putBytes(byte[] x, int off, int len);

  @Override
  HashStream32 putByteArray(byte[] x);

  @Override
  HashStream32 putBoolean(boolean v);

  @Override
  HashStream32 putBooleans(boolean[] x);

  @Override
  HashStream32 putBooleans(boolean[] x, int off, int len);

  @Override
  HashStream32 putBooleanArray(boolean[] x);

  @Override
  HashStream32 putShort(short v);

  @Override
  HashStream32 putShorts(short[] x);

  @Override
  HashStream32 putShorts(short[] x, int off, int len);

  @Override
  HashStream32 putShortArray(short[] x);

  @Override
  HashStream32 putChar(char v);

  @Override
  HashStream32 putChars(char[] x);

  @Override
  HashStream32 putChars(char[] x, int off, int len);

  @Override
  HashStream32 putChars(CharSequence c);

  @Override
  HashStream32 putCharArray(char[] x);

  @Override
  HashStream32 putString(String s);

  @Override
  HashStream32 putInt(int v);

  @Override
  HashStream32 putInts(int[] x);

  @Override
  HashStream32 putInts(int[] x, int off, int len);

  @Override
  HashStream32 putIntArray(int[] x);

  @Override
  HashStream32 putLong(long v);

  @Override
  HashStream32 putLongs(long[] x);

  @Override
  HashStream32 putLongs(long[] x, int off, int len);

  @Override
  HashStream32 putLongArray(long[] x);

  @Override
  HashStream32 putFloat(float v);

  @Override
  HashStream32 putFloats(float[] x);

  @Override
  HashStream32 putFloats(float[] x, int off, int len);

  @Override
  HashStream32 putFloatArray(float[] x);

  @Override
  HashStream32 putDouble(double v);

  @Override
  HashStream32 putDoubles(double[] x);

  @Override
  HashStream32 putDoubles(double[] x, int off, int len);

  @Override
  HashStream32 putDoubleArray(double[] x);

  @Override
  HashStream32 putUUID(UUID uuid);

  @Override
  <T> HashStream32 put(T obj, HashFunnel<T> funnel);

  @Override
  <T> HashStream32 putNullable(T obj, HashFunnel<T> funnel);

  @Override
  <T> HashStream32 putUnorderedIterable(
      Iterable<T> data, ToLongFunction<? super T> elementHashFunction);

  @Override
  <T> HashStream32 putUnorderedIterable(
      Iterable<T> data, HashFunnel<? super T> funnel, HashStream64 hashStream);

  @Override
  <T> HashStream32 putUnorderedIterable(
      Iterable<T> data, HashFunnel<? super T> funnel, Hasher64 hasher);

  @Override
  <T> HashStream32 putOptional(Optional<T> obj, HashFunnel<? super T> funnel);

  @Override
  HashStream32 putOptionalInt(OptionalInt v);

  @Override
  HashStream32 putOptionalLong(OptionalLong v);

  @Override
  HashStream32 putOptionalDouble(OptionalDouble v);

  @Override
  HashStream32 reset();

  @Override
  HashStream32 copy();

  /**
   * Resets this hash stream and returns a 32-bit hash value of the given object using the given
   * funnel.
   *
   * <p>Equivalent to {@code funnel.put(obj, reset()); return getAsInt();}
   *
   * @param obj the object
   * @param funnel the funnel
   * @param <T> the type
   * @return a 32-bit hash value
   */
  <T> int resetAndHashToInt(T obj, HashFunnel<T> funnel);
}
