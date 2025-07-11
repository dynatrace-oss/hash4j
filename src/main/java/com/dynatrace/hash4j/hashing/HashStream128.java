/*
 * Copyright 2022-2025 Dynatrace LLC
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

/** A hash stream computing a 128-bit hash value. */
public interface HashStream128 extends HashStream64 {

  /**
   * Returns a 128-bit hash value.
   *
   * <p>{@code get().getAsLong()} will be equivalent to {@link #getAsLong()}.
   *
   * @return a 128-bit hash value
   */
  HashValue128 get();

  @Override
  HashStream128 putByte(byte v);

  @Override
  HashStream128 putBytes(byte[] x);

  @Override
  HashStream128 putBytes(byte[] x, int off, int len);

  @Override
  HashStream128 putByteArray(byte[] x);

  @Override
  HashStream128 putBoolean(boolean v);

  @Override
  HashStream128 putBooleans(boolean[] x);

  @Override
  HashStream128 putBooleans(boolean[] x, int off, int len);

  @Override
  HashStream128 putBooleanArray(boolean[] x);

  @Override
  HashStream128 putShort(short v);

  @Override
  HashStream128 putShorts(short[] x);

  @Override
  HashStream128 putShorts(short[] x, int off, int len);

  @Override
  HashStream128 putShortArray(short[] x);

  @Override
  HashStream128 putChar(char v);

  @Override
  HashStream128 putChars(char[] x);

  @Override
  HashStream128 putChars(char[] x, int off, int len);

  @Override
  HashStream128 putChars(CharSequence c);

  @Override
  HashStream128 putCharArray(char[] x);

  @Override
  HashStream128 putString(String s);

  @Override
  HashStream128 putInt(int v);

  @Override
  HashStream128 putInts(int[] x);

  @Override
  HashStream128 putInts(int[] x, int off, int len);

  @Override
  HashStream128 putIntArray(int[] x);

  @Override
  HashStream128 putLong(long v);

  @Override
  HashStream128 putLongs(long[] x);

  @Override
  HashStream128 putLongs(long[] x, int off, int len);

  @Override
  HashStream128 putLongArray(long[] x);

  @Override
  HashStream128 putFloat(float v);

  @Override
  HashStream128 putFloats(float[] x);

  @Override
  HashStream128 putFloats(float[] x, int off, int len);

  @Override
  HashStream128 putFloatArray(float[] x);

  @Override
  HashStream128 putDouble(double v);

  @Override
  HashStream128 putDoubles(double[] x);

  @Override
  HashStream128 putDoubles(double[] x, int off, int len);

  @Override
  HashStream128 putDoubleArray(double[] x);

  @Override
  HashStream128 putUUID(UUID uuid);

  @Override
  <T> HashStream128 put(T obj, HashFunnel<T> funnel);

  @Override
  <T> HashStream128 putNullable(T obj, HashFunnel<T> funnel);

  @Override
  <T> HashStream128 putOrderedIterable(Iterable<T> data, HashFunnel<? super T> funnel);

  @Override
  <T> HashStream128 putUnorderedIterable(
      Iterable<T> data, ToLongFunction<? super T> elementHashFunction);

  @Override
  <T> HashStream128 putUnorderedIterable(
      Iterable<T> data, HashFunnel<? super T> funnel, Hasher64 hasher);

  @Override
  <T> HashStream128 putOptional(Optional<T> obj, HashFunnel<? super T> funnel);

  @Override
  HashStream128 putOptionalInt(OptionalInt v);

  @Override
  HashStream128 putOptionalLong(OptionalLong v);

  @Override
  HashStream128 putOptionalDouble(OptionalDouble v);

  /**
   * Resets the hash stream.
   *
   * <p>This allows to reuse this instance for new hash computations.
   *
   * @return this
   */
  @Override
  HashStream128 reset();

  @Override
  HashStream128 copy();

  /**
   * Resets this hash stream and returns a 128-bit hash value of the given object using the given
   * funnel.
   *
   * <p>Equivalent to {@code funnel.put(obj, reset()); return get();}
   *
   * @param obj the object
   * @param funnel the funnel
   * @param <T> the type
   * @return a 128-bit hash value
   */
  <T> HashValue128 resetAndHashTo128Bits(T obj, HashFunnel<T> funnel);

  @Override
  Hasher128 getHasher();

  @Override
  byte[] getState();

  @Override
  HashStream128 setState(byte[] state);
}
