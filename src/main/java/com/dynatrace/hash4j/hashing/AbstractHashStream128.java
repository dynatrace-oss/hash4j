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

import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.UUID;
import java.util.function.ToLongFunction;

interface AbstractHashStream128 extends AbstractHashStream64, HashStream128 {

  @Override
  default long getAsLong() {
    return get().getAsLong();
  }

  @Override
  default HashStream128 putBoolean(boolean v) {
    AbstractHashStream64.super.putBoolean(v);
    return this;
  }

  @Override
  default HashStream128 putBooleans(boolean[] x) {
    AbstractHashStream64.super.putBooleans(x);
    return this;
  }

  @Override
  default HashStream128 putBooleans(boolean[] x, int off, int len) {
    AbstractHashStream64.super.putBooleans(x, off, len);
    return this;
  }

  @Override
  default HashStream128 putBooleanArray(boolean[] x) {
    AbstractHashStream64.super.putBooleanArray(x);
    return this;
  }

  @Override
  default HashStream128 putBytes(byte[] b) {
    AbstractHashStream64.super.putBytes(b);
    return this;
  }

  @Override
  default HashStream128 putBytes(byte[] b, int off, int len) {
    AbstractHashStream64.super.putBytes(b, off, len);
    return this;
  }

  @Override
  default <T> HashStream128 putBytes(T b, long off, long len, ByteAccess<T> access) {
    AbstractHashStream64.super.putBytes(b, off, len, access);
    return this;
  }

  @Override
  default HashStream128 putByteArray(byte[] x) {
    AbstractHashStream64.super.putByteArray(x);
    return this;
  }

  @Override
  default HashStream128 putChar(char v) {
    AbstractHashStream64.super.putChar(v);
    return this;
  }

  @Override
  default HashStream128 putChars(char[] x) {
    AbstractHashStream64.super.putChars(x);
    return this;
  }

  @Override
  default HashStream128 putChars(char[] x, int off, int len) {
    AbstractHashStream64.super.putChars(x, off, len);
    return this;
  }

  @Override
  default HashStream128 putChars(CharSequence s) {
    AbstractHashStream64.super.putChars(s);
    return this;
  }

  @Override
  default HashStream128 putCharArray(char[] x) {
    AbstractHashStream64.super.putCharArray(x);
    return this;
  }

  @Override
  default HashStream128 putString(String s) {
    AbstractHashStream64.super.putString(s);
    return this;
  }

  @Override
  default HashStream128 putShort(short v) {
    AbstractHashStream64.super.putShort(v);
    return this;
  }

  @Override
  default HashStream128 putShortArray(short[] x) {
    AbstractHashStream64.super.putShortArray(x);
    return this;
  }

  @Override
  default HashStream128 putShorts(short[] x) {
    AbstractHashStream64.super.putShorts(x);
    return this;
  }

  @Override
  default HashStream128 putShorts(short[] x, int off, int len) {
    AbstractHashStream64.super.putShorts(x, off, len);
    return this;
  }

  @Override
  default HashStream128 putInt(int v) {
    AbstractHashStream64.super.putInt(v);
    return this;
  }

  @Override
  default HashStream128 putIntArray(int[] x) {
    AbstractHashStream64.super.putIntArray(x);
    return this;
  }

  @Override
  default HashStream128 putInts(int[] x) {
    AbstractHashStream64.super.putInts(x);
    return this;
  }

  @Override
  default HashStream128 putInts(int[] x, int off, int len) {
    AbstractHashStream64.super.putInts(x, off, len);
    return this;
  }

  @Override
  default HashStream128 putLong(long v) {
    AbstractHashStream64.super.putLong(v);
    return this;
  }

  @Override
  default HashStream128 putLongArray(long[] x) {
    AbstractHashStream64.super.putLongArray(x);
    return this;
  }

  @Override
  default HashStream128 putLongs(long[] x) {
    AbstractHashStream64.super.putLongs(x);
    return this;
  }

  @Override
  default HashStream128 putLongs(long[] x, int off, int len) {
    AbstractHashStream64.super.putLongs(x, off, len);
    return this;
  }

  @Override
  default HashStream128 putFloat(float v) {
    AbstractHashStream64.super.putFloat(v);
    return this;
  }

  @Override
  default HashStream128 putFloats(float[] x) {
    AbstractHashStream64.super.putFloats(x);
    return this;
  }

  @Override
  default HashStream128 putFloats(float[] x, int off, int len) {
    AbstractHashStream64.super.putFloats(x, off, len);
    return this;
  }

  @Override
  default HashStream128 putFloatArray(float[] x) {
    AbstractHashStream64.super.putFloatArray(x);
    return this;
  }

  @Override
  default HashStream128 putDouble(double v) {
    AbstractHashStream64.super.putDouble(v);
    return this;
  }

  @Override
  default HashStream128 putDoubleArray(double[] x) {
    AbstractHashStream64.super.putDoubleArray(x);
    return this;
  }

  @Override
  default HashStream128 putDoubles(double[] x) {
    AbstractHashStream64.super.putDoubles(x);
    return this;
  }

  @Override
  default HashStream128 putDoubles(double[] x, int off, int len) {
    AbstractHashStream64.super.putDoubles(x, off, len);
    return this;
  }

  @Override
  default HashStream128 putUUID(UUID uuid) {
    AbstractHashStream64.super.putUUID(uuid);
    return this;
  }

  @Override
  default <T> HashStream128 put(T data, HashFunnel<T> funnel) {
    AbstractHashStream64.super.put(data, funnel);
    return this;
  }

  @Override
  default <T> HashStream128 putNullable(T data, HashFunnel<T> funnel) {
    AbstractHashStream64.super.putNullable(data, funnel);
    return this;
  }

  @Override
  default <T> HashStream128 putOrderedIterable(Iterable<T> data, HashFunnel<? super T> funnel) {
    AbstractHashStream64.super.putOrderedIterable(data, funnel);
    return this;
  }

  @Override
  default <T> HashStream128 putUnorderedIterable(
      Iterable<T> data, HashFunnel<? super T> funnel, Hasher64 hasher) {
    AbstractHashStream64.super.putUnorderedIterable(data, funnel, hasher);
    return this;
  }

  @Override
  default <T> HashStream128 putUnorderedIterable(
      Iterable<T> data, ToLongFunction<? super T> elementHashFunction) {
    AbstractHashStream64.super.putUnorderedIterable(data, elementHashFunction);
    return this;
  }

  @Override
  default <T> HashStream128 putOptional(Optional<T> obj, HashFunnel<? super T> funnel) {
    AbstractHashStream64.super.putOptional(obj, funnel);
    return this;
  }

  @Override
  default HashStream128 putOptionalInt(OptionalInt v) {
    AbstractHashStream64.super.putOptionalInt(v);
    return this;
  }

  @Override
  default HashStream128 putOptionalLong(OptionalLong v) {
    AbstractHashStream64.super.putOptionalLong(v);
    return this;
  }

  @Override
  default HashStream128 putOptionalDouble(OptionalDouble v) {
    AbstractHashStream64.super.putOptionalDouble(v);
    return this;
  }

  @Override
  default <T> HashValue128 resetAndHashTo128Bits(T obj, HashFunnel<T> funnel) {
    funnel.put(obj, reset());
    return get();
  }

  @Override
  default HashStream128 copy() {
    return getHasher().hashStreamFromState(getState());
  }
}
