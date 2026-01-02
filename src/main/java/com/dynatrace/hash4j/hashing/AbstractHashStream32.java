/*
 * Copyright 2022-2026 Dynatrace LLC
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

interface AbstractHashStream32 extends AbstractHashStream, HashStream32 {

  @Override
  default HashStream32 putBoolean(boolean v) {
    AbstractHashStream.super.putBoolean(v);
    return this;
  }

  @Override
  default HashStream32 putBooleans(boolean[] x) {
    AbstractHashStream.super.putBooleans(x);
    return this;
  }

  @Override
  default HashStream32 putBooleans(boolean[] x, int off, int len) {
    AbstractHashStream.super.putBooleans(x, off, len);
    return this;
  }

  @Override
  default HashStream32 putBooleanArray(boolean[] x) {
    AbstractHashStream.super.putBooleanArray(x);
    return this;
  }

  @Override
  default HashStream32 putBytes(byte[] b) {
    AbstractHashStream.super.putBytes(b);
    return this;
  }

  @Override
  default HashStream32 putBytes(byte[] b, int off, int len) {
    AbstractHashStream.super.putBytes(b, off, len);
    return this;
  }

  @Override
  default <T> HashStream32 putBytes(T b, long off, long len, ByteAccess<T> access) {
    AbstractHashStream.super.putBytes(b, off, len, access);
    return this;
  }

  @Override
  default HashStream32 putByteArray(byte[] x) {
    AbstractHashStream.super.putByteArray(x);
    return this;
  }

  @Override
  default HashStream32 putChar(char v) {
    AbstractHashStream.super.putChar(v);
    return this;
  }

  @Override
  default HashStream32 putChars(char[] x) {
    AbstractHashStream.super.putChars(x);
    return this;
  }

  @Override
  default HashStream32 putChars(char[] x, int off, int len) {
    AbstractHashStream.super.putChars(x, off, len);
    return this;
  }

  @Override
  default HashStream32 putChars(CharSequence s) {
    AbstractHashStream.super.putChars(s);
    return this;
  }

  @Override
  default HashStream32 putCharArray(char[] x) {
    AbstractHashStream.super.putCharArray(x);
    return this;
  }

  @Override
  default HashStream32 putString(String s) {
    AbstractHashStream.super.putString(s);
    return this;
  }

  @Override
  default HashStream32 putShort(short v) {
    AbstractHashStream.super.putShort(v);
    return this;
  }

  @Override
  default HashStream32 putShortArray(short[] x) {
    AbstractHashStream.super.putShortArray(x);
    return this;
  }

  @Override
  default HashStream32 putShorts(short[] x) {
    AbstractHashStream.super.putShorts(x);
    return this;
  }

  @Override
  default HashStream32 putShorts(short[] x, int off, int len) {
    AbstractHashStream.super.putShorts(x, off, len);
    return this;
  }

  @Override
  default HashStream32 putInt(int v) {
    AbstractHashStream.super.putInt(v);
    return this;
  }

  @Override
  default HashStream32 putIntArray(int[] x) {
    AbstractHashStream.super.putIntArray(x);
    return this;
  }

  @Override
  default HashStream32 putInts(int[] x) {
    AbstractHashStream.super.putInts(x);
    return this;
  }

  @Override
  default HashStream32 putInts(int[] x, int off, int len) {
    AbstractHashStream.super.putInts(x, off, len);
    return this;
  }

  @Override
  default HashStream32 putLong(long v) {
    AbstractHashStream.super.putLong(v);
    return this;
  }

  @Override
  default HashStream32 putLongArray(long[] x) {
    AbstractHashStream.super.putLongArray(x);
    return this;
  }

  @Override
  default HashStream32 putLongs(long[] x) {
    AbstractHashStream.super.putLongs(x);
    return this;
  }

  @Override
  default HashStream32 putLongs(long[] x, int off, int len) {
    AbstractHashStream.super.putLongs(x, off, len);
    return this;
  }

  @Override
  default HashStream32 putFloat(float v) {
    AbstractHashStream.super.putFloat(v);
    return this;
  }

  @Override
  default HashStream32 putFloats(float[] x) {
    AbstractHashStream.super.putFloats(x);
    return this;
  }

  @Override
  default HashStream32 putFloats(float[] x, int off, int len) {
    AbstractHashStream.super.putFloats(x, off, len);
    return this;
  }

  @Override
  default HashStream32 putFloatArray(float[] x) {
    AbstractHashStream.super.putFloatArray(x);
    return this;
  }

  @Override
  default HashStream32 putDouble(double v) {
    AbstractHashStream.super.putDouble(v);
    return this;
  }

  @Override
  default HashStream32 putDoubleArray(double[] x) {
    AbstractHashStream.super.putDoubleArray(x);
    return this;
  }

  @Override
  default HashStream32 putDoubles(double[] x) {
    AbstractHashStream.super.putDoubles(x);
    return this;
  }

  @Override
  default HashStream32 putDoubles(double[] x, int off, int len) {
    AbstractHashStream.super.putDoubles(x, off, len);
    return this;
  }

  @Override
  default HashStream32 putUUID(UUID uuid) {
    AbstractHashStream.super.putUUID(uuid);
    return this;
  }

  @Override
  default <T> HashStream32 put(T data, HashFunnel<T> funnel) {
    AbstractHashStream.super.put(data, funnel);
    return this;
  }

  @Override
  default <T> HashStream32 putNullable(T data, HashFunnel<T> funnel) {
    AbstractHashStream.super.putNullable(data, funnel);
    return this;
  }

  @Override
  default <T> HashStream32 putOrderedIterable(Iterable<T> data, HashFunnel<? super T> funnel) {
    AbstractHashStream.super.putOrderedIterable(data, funnel);
    return this;
  }

  @Override
  default <T> HashStream32 putUnorderedIterable(
      Iterable<T> data, HashFunnel<? super T> funnel, Hasher64 hasher) {
    AbstractHashStream.super.putUnorderedIterable(data, funnel, hasher);
    return this;
  }

  @Override
  default <T> HashStream32 putUnorderedIterable(
      Iterable<T> data, HashFunnel<? super T> funnel, HashStream64 hashStream) {
    AbstractHashStream.super.putUnorderedIterable(data, funnel, hashStream);
    return this;
  }

  @Override
  default <T> HashStream32 putUnorderedIterable(
      Iterable<T> data, ToLongFunction<? super T> elementHashFunction) {
    AbstractHashStream.super.putUnorderedIterable(data, elementHashFunction);
    return this;
  }

  @Override
  default <T> HashStream32 putOptional(Optional<T> obj, HashFunnel<? super T> funnel) {
    AbstractHashStream.super.putOptional(obj, funnel);
    return this;
  }

  @Override
  default HashStream32 putOptionalInt(OptionalInt v) {
    AbstractHashStream.super.putOptionalInt(v);
    return this;
  }

  @Override
  default HashStream32 putOptionalLong(OptionalLong v) {
    AbstractHashStream.super.putOptionalLong(v);
    return this;
  }

  @Override
  default HashStream32 putOptionalDouble(OptionalDouble v) {
    AbstractHashStream.super.putOptionalDouble(v);
    return this;
  }

  @Override
  default <T> int resetAndHashToInt(T obj, HashFunnel<T> funnel) {
    funnel.put(obj, reset());
    return getAsInt();
  }

  @Override
  default HashStream32 copy() {
    return getHasher().hashStreamFromState(getState());
  }
}
