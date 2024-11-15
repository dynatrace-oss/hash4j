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
package com.dynatrace.hash4j.util;

import static java.util.Objects.requireNonNull;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.function.LongBinaryOperator;

/** A utility class for compactly storing fixed bit size components in a byte array. */
public final class PackedArray {

  private PackedArray() {}

  private static final VarHandle LONG_HANDLE =
      MethodHandles.byteArrayViewVarHandle(long[].class, ByteOrder.LITTLE_ENDIAN);
  private static final VarHandle INT_HANDLE =
      MethodHandles.byteArrayViewVarHandle(int[].class, ByteOrder.LITTLE_ENDIAN);
  private static final VarHandle SHORT_HANDLE =
      MethodHandles.byteArrayViewVarHandle(short[].class, ByteOrder.LITTLE_ENDIAN);

  private static short getShort(byte[] b, int off) {
    return (short) SHORT_HANDLE.get(b, off);
  }

  private static int getInt(byte[] b, int off) {
    return (int) INT_HANDLE.get(b, off);
  }

  private static long getLong(byte[] b, int off) {
    return (long) LONG_HANDLE.get(b, off);
  }

  private static void setLong(byte[] b, int off, long v) {
    LONG_HANDLE.set(b, off, v);
  }

  private static void setInt(byte[] b, int off, int v) {
    INT_HANDLE.set(b, off, v);
  }

  private static void setShort(byte[] b, int off, short v) {
    SHORT_HANDLE.set(b, off, v);
  }

  private static final byte[] ZERO_BYTES = new byte[0];

  /** Gives access to indexed long values. */
  @FunctionalInterface
  public interface IndexedLongValueProvider {
    /**
     * Returns the long value associated with the given index.
     *
     * @param index the index
     * @return the long value
     */
    long getValue(int index);
  }

  /** A handler for a packed array with predefined element size. */
  public interface PackedArrayHandler {

    /**
     * Returns the number of bytes needed to store a packed array having the given number of
     * components.
     *
     * @param length the length of the packed array
     * @return the number of bytes
     */
    int numBytes(int length);

    /**
     * Creates a {@code byte} array that can store a packed array having the given number of
     * components.
     *
     * <p>All components of the byte array are initially 0, which also corresponds to 0 values in
     * the packed array.
     *
     * @param length the length of the packed array
     * @return a byte array representing the packed array
     */
    byte[] create(int length);

    /**
     * Returns a {@code byte} array that represents a packed array having the given number of
     * components. The initial values are taken from the given {@link IndexedLongValueProvider}.
     *
     * @param valueProvider a value provider
     * @param length the length of the packed array
     * @return a byte array representing the packed array
     */
    byte[] create(IndexedLongValueProvider valueProvider, int length);

    /**
     * Returns the value of a component in a packed array.
     *
     * <p>If the given index is not valid, the behavior of this function is undefined.
     *
     * @param array a byte array representing the packed array
     * @param idx the index of the component
     * @return the value of the component
     */
    long get(byte[] array, int idx);

    /**
     * Sets a component in a packed array to the given value and returns the previous value.
     *
     * <p>Only the lowest {@link #getBitSize()} bits of the given value are used for setting the new
     * value.
     *
     * <p>If the given index is not valid, the behavior of this function is undefined.
     *
     * @param array a byte array representing the packed array
     * @param idx the index of the component
     * @param value the new value
     * @return the previous value of the component
     */
    long set(byte[] array, int idx, long value);

    /**
     * Updates a component in a packed array using the given value and a binary operator that
     * computes the update value from the previous value and the given value.
     *
     * <p>The given operator will get the old value of the component as first argument and the given
     * value as second argument. Only the lowest {@link #getBitSize()} bits of the result are used
     * for setting the new value.
     *
     * <p>If the given index is not valid, the behavior of this function is undefined.
     *
     * @param array a byte array representing the packed array
     * @param idx the index of the component
     * @param value a value
     * @param operator a binary operator
     * @return the previous value of the component
     * @throws NullPointerException if operator is {@code null}
     */
    long update(byte[] array, int idx, long value, LongBinaryOperator operator);

    /**
     * The number of bits of a single component.
     *
     * @return the number of bits
     */
    int getBitSize();

    /**
     * Returns the number of equal components of two packed arrays with given length.
     *
     * <p>If the arrays have been initialized using {@link #create} with a different length than the
     * given length, the behavior of this method is not defined.
     *
     * @param array1 the first packed array
     * @param array2 the second packed array
     * @param length the total number of components
     * @return the number of equal components
     */
    int numEqualComponents(byte[] array1, byte[] array2, int length);

    /**
     * Sets all components of the given packed array to 0.
     *
     * @param array a packed array
     */
    void clear(byte[] array);

    /**
     * Returns a {@link PackedArrayReadIterator} to read the values of all components in sequential
     * order.
     *
     * @param array the packed array
     * @param length the number of components of the packed array
     * @return a read iterator
     * @throws NullPointerException if array is null
     */
    PackedArrayReadIterator readIterator(byte[] array, int length);
  }

  /** A read iterator to read the values of all components in sequential order. */
  public interface PackedArrayReadIterator {

    /**
     * Returns {@code true} if the iteration has more components.
     *
     * @return {@code true} if the iteration has more components
     */
    boolean hasNext();

    /**
     * Returns the value of the next component in the iteration.
     *
     * <p>The behavior is undefined if there are no further components.
     *
     * @return the value of the next component in the iteration
     */
    long next();
  }

  private static final long MAX_NUM_BITS = Integer.MAX_VALUE * 8L;

  private static void checkArrayLength(int length, int bitSize) {
    if (length < 0 || Math.multiplyFull(length, bitSize) > MAX_NUM_BITS) {
      throw new IllegalArgumentException("Invalid array length");
    }
  }

  private abstract static class AbstractPackedArrayHandler implements PackedArrayHandler {
    protected final int bitSize;
    protected final long mask;

    private void checkArrayLength(int length) {
      PackedArray.checkArrayLength(length, bitSize);
    }

    private AbstractPackedArrayHandler(int bitSize) {
      this.bitSize = bitSize;
      this.mask = (1L << 1 << (bitSize - 1)) - 1;
    }

    @Override
    public byte[] create(int length) {
      checkArrayLength(length);
      return new byte[numBytes(length)];
    }

    @Override
    public final int getBitSize() {
      return bitSize;
    }

    @Override
    public int numEqualComponents(byte[] array1, byte[] array2, int length) {
      ParallelXorPackedArrayReadIterator x =
          new ParallelXorPackedArrayReadIterator(array1, array2, length);
      int c = 0;
      for (int i = 0; i < length; ++i) {
        if (x.next() == 0) c += 1;
      }
      return c;
    }

    @Override
    public int numBytes(int length) {
      return (bitSize * length + 7) >>> 3;
    }

    @Override
    public void clear(byte[] array) {
      Arrays.fill(array, (byte) 0);
    }

    protected abstract int getOffset(int idx);

    protected long get1(byte[] array, int idx, int off, int shift) {
      int offset = getOffset(idx) + off;
      return (array[offset] >>> shift) & mask;
    }

    protected long set1(byte[] array, int idx, int off, int shift, long value) {
      int offset = getOffset(idx) + off;
      long current = array[offset];
      long ret = current >>> shift;
      current ^= (((ret ^ value) & mask) << shift);
      array[offset] = (byte) current;
      return ret & mask;
    }

    protected long update1(
        byte[] array, int idx, int off, int shift, long value, LongBinaryOperator operator) {
      int offset = getOffset(idx) + off;
      long current = array[offset];
      long ret = (current >>> shift) & mask;
      current ^= ((ret ^ (operator.applyAsLong(ret, value) & mask)) << shift);
      array[offset] = (byte) current;
      return ret;
    }

    protected long get2(byte[] array, int idx, int off, int shift) {
      int offset = getOffset(idx) + off;
      return (getShort(array, offset) >>> shift) & mask;
    }

    protected long set2(byte[] array, int idx, int off, int shift, long value) {
      int offset = getOffset(idx) + off;
      long current = getShort(array, offset);
      long ret = current >>> shift;
      current ^= ((ret ^ value) & mask) << shift;
      setShort(array, offset, (short) current);
      return ret & mask;
    }

    protected long update2(
        byte[] array, int idx, int off, int shift, long value, LongBinaryOperator operator) {
      int offset = getOffset(idx) + off;
      long current = getShort(array, offset);
      long ret = (current >>> shift) & mask;
      current ^= ((ret ^ (operator.applyAsLong(ret, value) & mask)) << shift);
      setShort(array, offset, (short) current);
      return ret;
    }

    protected long get3(byte[] array, int idx, int off, int shift) {
      if (idx > 0) {
        return get4(array, idx, off - 1, shift + 8);
      } else {
        int offset = getOffset(idx) + off;
        return (((getShort(array, offset) & 0xFFFFL) | (array[offset + 2] << 16)) >>> shift) & mask;
      }
    }

    protected long set3(byte[] array, int idx, int off, int shift, long value) {
      if (idx > 0) {
        return set4(array, idx, off - 1, shift + 8, value);
      } else {
        // shift is always equal to 0 in this case
        int offset = getOffset(idx) + off;
        long current = ((getShort(array, offset) & 0xFFFFL) | (array[offset + 2] << 16));
        long ret = current >>> shift;
        current ^= ((ret ^ value) & mask) << shift;
        setShort(array, offset, (short) current);
        array[offset + 2] = (byte) (current >>> 16);
        return ret & mask;
      }
    }

    protected long update3(
        byte[] array, int idx, int off, int shift, long value, LongBinaryOperator operator) {
      if (idx > 0) {
        return update4(array, idx, off - 1, shift + 8, value, operator);
      } else {
        // shift is always equal to 0 in this case
        int offset = getOffset(idx) + off;
        long current = ((getShort(array, offset) & 0xFFFFL) | (array[offset + 2] << 16));
        long ret = (current >>> shift) & mask;
        current ^= ((ret ^ (operator.applyAsLong(ret, value) & mask)) << shift);
        setShort(array, offset, (short) current);
        array[offset + 2] = (byte) (current >>> 16);
        return ret;
      }
    }

    protected long get4(byte[] array, int idx, int off, int shift) {
      int offset = getOffset(idx) + off;
      return (getInt(array, offset) >>> shift) & mask;
    }

    protected long set4(byte[] array, int idx, int off, int shift, long value) {
      int offset = getOffset(idx) + off;
      long current = getInt(array, offset);
      long ret = current >>> shift;
      current ^= ((ret ^ value) & mask) << shift;
      setInt(array, offset, (int) current);
      return ret & mask;
    }

    protected long update4(
        byte[] array, int idx, int off, int shift, long value, LongBinaryOperator operator) {
      int offset = getOffset(idx) + off;
      long current = getInt(array, offset);
      long ret = (current >>> shift) & mask;
      current ^= ((ret ^ (operator.applyAsLong(ret, value) & mask)) << shift);
      setInt(array, offset, (int) current);
      return ret;
    }

    protected long get5(byte[] array, int idx, int off, int shift) {
      if (idx > 0) {
        return get8(array, idx, off - 3, shift + 24);
      } else {
        int offset = getOffset(idx) + off;
        return (((getInt(array, offset) & 0xFFFFFFFFL) | ((long) array[offset + 4] << 32))
                >>> shift)
            & mask;
      }
    }

    protected long set5(byte[] array, int idx, int off, int shift, long value) {
      if (idx > 0) {
        return set8(array, idx, off - 3, shift + 24, value);
      } else {
        // shift is always equal to 0 in this case
        int offset = getOffset(idx) + off;
        long current = ((getInt(array, offset) & 0xFFFFFFFFL) | ((long) array[offset + 4] << 32));
        long ret = current >>> shift;
        current ^= ((ret ^ value) & mask) << shift;
        setInt(array, offset, (int) current);
        array[offset + 4] = (byte) (current >>> 32);
        return ret & mask;
      }
    }

    protected long update5(
        byte[] array, int idx, int off, int shift, long value, LongBinaryOperator operator) {
      if (idx > 0) {
        return update8(array, idx, off - 3, shift + 24, value, operator);
      } else {
        // shift is always equal to 0 in this case
        int offset = getOffset(idx) + off;
        long current = ((getInt(array, offset) & 0xFFFFFFFFL) | ((long) array[offset + 4] << 32));
        long ret = (current >>> shift) & mask;
        current ^= ((ret ^ (operator.applyAsLong(ret, value) & mask)) << shift);
        setInt(array, offset, (int) current);
        array[offset + 4] = (byte) (current >>> 32);
        return ret;
      }
    }

    protected long get6(byte[] array, int idx, int off, int shift) {
      if (idx > 0) {
        return get8(array, idx, off - 2, shift + 16);
      } else {
        int offset = getOffset(idx) + off;
        return (((getInt(array, offset) & 0xFFFFFFFFL) | ((long) getShort(array, offset + 4) << 32))
                >>> shift)
            & mask;
      }
    }

    protected long set6(byte[] array, int idx, int off, int shift, long value) {
      if (idx > 0) {
        return set8(array, idx, off - 2, shift + 16, value);
      } else {
        // shift is always equal to 0 in this case
        int offset = getOffset(idx) + off;
        long current =
            ((getInt(array, offset) & 0xFFFFFFFFL) | (((long) getShort(array, offset + 4)) << 32));
        long ret = current >>> shift;
        current ^= ((ret ^ value) & mask) << shift;
        setInt(array, offset, (int) current);
        setShort(array, offset + 4, (short) (current >>> 32));
        return ret & mask;
      }
    }

    protected long update6(
        byte[] array, int idx, int off, int shift, long value, LongBinaryOperator operator) {
      if (idx > 0) {
        return update8(array, idx, off - 2, shift + 16, value, operator);
      } else {
        // shift is always equal to 0 in this case
        int offset = getOffset(idx) + off;
        long current =
            ((getInt(array, offset) & 0xFFFFFFFFL) | (((long) getShort(array, offset + 4)) << 32));
        long ret = (current >>> shift) & mask;
        current ^= ((ret ^ (operator.applyAsLong(ret, value) & mask)) << shift);
        setInt(array, offset, (int) current);
        setShort(array, offset + 4, (short) (current >>> 32));
        return ret;
      }
    }

    protected long get7(byte[] array, int idx, int off, int shift) {
      if (idx > 0) {
        return get8(array, idx, off - 1, shift + 8);
      } else {
        int offset = getOffset(idx) + off;
        return (((getInt(array, offset) & 0xFFFFFFFFL)
                    | ((getShort(array, offset + 4) & 0xFFFFL) << 32)
                    | ((long) array[offset + 6] << 48))
                >>> shift)
            & mask;
      }
    }

    protected long set7(byte[] array, int idx, int off, int shift, long value) {
      if (idx > 0) {
        return set8(array, idx, off - 1, shift + 8, value);
      } else {
        // shift is always equal to 0 in this case
        int offset = getOffset(idx) + off;
        long current =
            ((getInt(array, offset) & 0xFFFFFFFFL)
                | ((getShort(array, offset + 4) & 0xFFFFL) << 32)
                | ((long) array[offset + 6] << 48));
        long ret = current >>> shift;
        current ^= ((ret ^ value) & mask) << shift;
        setInt(array, offset, (int) current);
        setShort(array, offset + 4, (short) (current >>> 32));
        array[offset + 6] = (byte) (current >>> 48);
        return ret & mask;
      }
    }

    protected long update7(
        byte[] array, int idx, int off, int shift, long value, LongBinaryOperator operator) {
      if (idx > 0) {
        return update8(array, idx, off - 1, shift + 8, value, operator);
      } else {
        // shift is always equal to 0 in this case
        int offset = getOffset(idx) + off;
        long current =
            ((getInt(array, offset) & 0xFFFFFFFFL)
                | ((getShort(array, offset + 4) & 0xFFFFL) << 32)
                | ((long) array[offset + 6] << 48));
        long ret = (current >>> shift) & mask;
        current ^= ((ret ^ (operator.applyAsLong(ret, value) & mask)) << shift);
        setInt(array, offset, (int) current);
        setShort(array, offset + 4, (short) (current >>> 32));
        array[offset + 6] = (byte) (current >>> 48);
        return ret;
      }
    }

    protected long get8(byte[] array, int idx, int off, int shift) {
      int offset = getOffset(idx) + off;
      return (getLong(array, offset) >>> shift) & mask;
    }

    protected long set8(byte[] array, int idx, int off, int shift, long value) {
      int offset = getOffset(idx) + off;
      long current = getLong(array, offset);
      long ret = current >>> shift;
      current ^= ((ret ^ value) & mask) << shift;
      setLong(array, offset, current);
      return ret & mask;
    }

    protected long update8(
        byte[] array, int idx, int off, int shift, long value, LongBinaryOperator operator) {
      int offset = getOffset(idx) + off;
      long current = getLong(array, offset);
      long ret = (current >>> shift) & mask;
      current ^= ((ret ^ (operator.applyAsLong(ret, value) & mask)) << shift);
      setLong(array, offset, current);
      return ret;
    }

    protected long get9(byte[] array, int idx, int off, int shift) {
      int offset = getOffset(idx) + off;
      return ((getLong(array, offset) >>> shift) | (((long) array[offset + 8]) << -shift)) & mask;
    }

    protected long set9(byte[] array, int idx, int off, int shift, long value) {
      int offset = getOffset(idx) + off;
      long currentLong = getLong(array, offset);
      long currentByte = array[offset + 8];
      long ret = ((currentLong >>> shift) | (currentByte << -shift));
      long changeMask = (ret ^ value) & mask;
      setLong(array, offset, currentLong ^ (changeMask << shift));
      array[offset + 8] = (byte) (currentByte ^ (changeMask >>> -shift));
      return ret & mask;
    }

    protected long update9(
        byte[] array, int idx, int off, int shift, long value, LongBinaryOperator operator) {
      int offset = getOffset(idx) + off;
      long currentLong = getLong(array, offset);
      long currentByte = array[offset + 8];
      long ret = ((currentLong >>> shift) | (currentByte << -shift)) & mask;
      long changeMask = ret ^ (operator.applyAsLong(ret, value) & mask);
      setLong(array, offset, currentLong ^ (changeMask << shift));
      array[offset + 8] = (byte) (currentByte ^ (changeMask >>> -shift));
      return ret;
    }

    @Override
    public byte[] create(IndexedLongValueProvider valueProvider, int length) {
      byte[] array = create(length);
      long buffer = 0;
      int bitsInBuffer = 0;
      int arrayOffset = 0;
      for (int idx = 0; idx < length; ++idx) {
        long value = valueProvider.getValue(idx) & mask;
        buffer |= value << bitsInBuffer;
        bitsInBuffer += bitSize;
        if (bitsInBuffer >= 64) {
          setLong(array, arrayOffset, buffer);
          arrayOffset += 8;
          bitsInBuffer -= 64;
          buffer = value >>> 1 >>> (bitSize - bitsInBuffer - 1);
        }
      }
      if (bitsInBuffer > 56) {
        setLong(array, arrayOffset, buffer);
      } else {
        if (bitsInBuffer > 24) {
          setInt(array, arrayOffset, (int) buffer);
          bitsInBuffer -= 32;
          arrayOffset += 4;
          buffer >>>= 32;
        }
        if (bitsInBuffer > 8) {
          setShort(array, arrayOffset, (short) buffer);
          bitsInBuffer -= 16;
          arrayOffset += 2;
          buffer >>>= 16;
        }
        if (bitsInBuffer > 0) {
          array[arrayOffset] = (byte) buffer;
        }
      }
      return array;
    }

    private class PackedArrayReadIteratorImpl implements PackedArrayReadIterator {
      private int idx = 0;
      private long buffer;
      private int availableBits;

      private int readPos;

      private final int length;

      private final byte[] array;

      public PackedArrayReadIteratorImpl(byte[] array, int length) {
        requireNonNull(array);

        this.length = length;
        this.array = array;
        int remainingBytes = numBytes(length) & 0x7;
        this.availableBits = remainingBytes << 3;

        if (array.length >= 8) {
          buffer = (long) LONG_HANDLE.get(array, 0);
          readPos = remainingBytes;
        } else {
          readPos = 0;
          buffer = 0;
          if (remainingBytes >= 4) {
            buffer |= (int) INT_HANDLE.get(array, readPos) & 0xFFFFFFFFL;
            readPos += 4;
            remainingBytes -= 4;
          }
          if (remainingBytes >= 2) {
            buffer |= ((short) SHORT_HANDLE.get(array, readPos) & 0xFFFFL) << (readPos << 3);
            readPos += 2;
            remainingBytes -= 2;
          }
          if (remainingBytes >= 1) {
            buffer |= (array[readPos] & 0xFFL) << (readPos << 3);
            readPos += 1;
          }
        }
      }

      @Override
      public boolean hasNext() {
        return idx < length;
      }

      @Override
      public long next() {
        idx += 1;
        long result = buffer;
        buffer >>>= bitSize;
        if (availableBits < bitSize) {
          buffer = (long) LONG_HANDLE.get(array, readPos);
          result |= buffer << availableBits;
          buffer >>>= 1;
          buffer >>>= ~(availableBits - bitSize);
          readPos += 8;
          availableBits += 64;
        }
        availableBits -= bitSize;
        return result & mask;
      }
    }

    @Override
    public PackedArrayReadIterator readIterator(byte[] array, int length) {
      return new PackedArrayReadIteratorImpl(array, length);
    }

    private class ParallelXorPackedArrayReadIterator {
      private long buffer;
      private int availableBits;

      private int readPos;

      private final byte[] array1;
      private final byte[] array2;

      public ParallelXorPackedArrayReadIterator(byte[] array1, byte[] array2, int length) {
        requireNonNull(array1);

        this.array1 = array1;
        this.array2 = array2;
        int remainingBytes = numBytes(length) & 0x7;
        this.availableBits = remainingBytes << 3;

        if (array1.length >= 8) {
          buffer = ((long) LONG_HANDLE.get(array1, 0)) ^ ((long) LONG_HANDLE.get(array2, 0));
          readPos = remainingBytes;
        } else {
          readPos = 0;
          buffer = 0;
          if (remainingBytes >= 4) {
            buffer |=
                (((int) INT_HANDLE.get(array1, readPos)) ^ ((int) INT_HANDLE.get(array2, readPos)))
                    & 0xFFFFFFFFL;
            readPos += 4;
            remainingBytes -= 4;
          }
          if (remainingBytes >= 2) {
            buffer |=
                ((((short) SHORT_HANDLE.get(array1, readPos))
                            ^ ((short) SHORT_HANDLE.get(array2, readPos)))
                        & 0xFFFFL)
                    << (readPos << 3);
            readPos += 2;
            remainingBytes -= 2;
          }
          if (remainingBytes >= 1) {
            buffer |= ((array1[readPos] ^ array2[readPos]) & 0xFFL) << (readPos << 3);
            readPos += 1;
          }
        }
      }

      public long next() {
        long result = buffer;
        buffer >>>= bitSize;
        if (availableBits < bitSize) {
          buffer =
              ((long) LONG_HANDLE.get(array1, readPos)) ^ ((long) LONG_HANDLE.get(array2, readPos));
          result |= buffer << availableBits;
          buffer >>>= 1;
          buffer >>>= ~(availableBits - bitSize);
          readPos += 8;
          availableBits += 64;
        }
        availableBits -= bitSize;
        return result & mask;
      }
    }
  }

  private abstract static class AbstractPackedArrayHandlerPeriod1
      extends AbstractPackedArrayHandler {

    public AbstractPackedArrayHandlerPeriod1(int bitSize) {
      super(bitSize);
    }

    @Override
    protected int getOffset(int idx) {
      return idx * (bitSize >>> 3);
    }
  }

  private abstract static class AbstractPackedArrayHandlerPeriod2
      extends AbstractPackedArrayHandler {

    public AbstractPackedArrayHandlerPeriod2(int bitSize) {
      super(bitSize);
    }

    @Override
    protected int getOffset(int idx) {
      return (idx >>> 1) * (bitSize >>> 2);
    }
  }

  private abstract static class AbstractPackedArrayHandlerPeriod4
      extends AbstractPackedArrayHandler {

    public AbstractPackedArrayHandlerPeriod4(int bitSize) {
      super(bitSize);
    }

    @Override
    protected int getOffset(int idx) {
      return (idx >>> 2) * (bitSize >>> 1);
    }
  }

  private abstract static class AbstractPackedArrayHandlerPeriod8
      extends AbstractPackedArrayHandler {

    public AbstractPackedArrayHandlerPeriod8(int bitSize) {
      super(bitSize);
    }

    @Override
    protected int getOffset(int idx) {
      return (idx >>> 3) * bitSize;
    }
  }

  private static final PackedArrayHandler HANDLER_0 =
      new PackedArrayHandler() {

        @Override
        public int numBytes(int length) {
          return 0;
        }

        @Override
        public byte[] create(int length) {
          PackedArray.checkArrayLength(length, 0);
          return ZERO_BYTES;
        }

        @Override
        public byte[] create(IndexedLongValueProvider valueProvider, int length) {
          return create(length);
        }

        @Override
        public long get(byte[] array, int idx) {
          return 0;
        }

        @Override
        public long set(byte[] array, int idx, long value) {
          return 0;
        }

        @Override
        public long update(byte[] array, int idx, long value, LongBinaryOperator operator) {
          requireNonNull(operator);
          return 0;
        }

        @Override
        public int getBitSize() {
          return 0;
        }

        @Override
        public int numEqualComponents(byte[] array1, byte[] array2, int length) {
          return length;
        }

        @Override
        public void clear(byte[] array) {
          // do nothing
        }

        @Override
        public PackedArrayReadIterator readIterator(byte[] array, int length) {
          requireNonNull(array);
          return new PackedArrayReadIterator() {

            private int idx = 0;

            @Override
            public boolean hasNext() {
              return idx < length;
            }

            @Override
            public long next() {
              idx += 1;
              return 0;
            }
          };
        }
      };

  private static final PackedArrayHandler HANDLER_1 =
      new AbstractPackedArrayHandlerPeriod8(1) {

        @Override
        public long get(byte[] array, int idx) {
          return get1(array, idx, 0, idx & 0x7);
        }

        @Override
        public long set(byte[] array, int idx, long value) {
          return set1(array, idx, 0, idx & 0x7, value);
        }

        @Override
        public long update(byte[] array, int idx, long value, LongBinaryOperator operator) {
          return update1(array, idx, 0, idx & 0x7, value, operator);
        }

        @Override
        public int numEqualComponents(byte[] array1, byte[] array2, int length) {
          int result = 0;
          int end = (length & 0xFFFFFFC0) >>> 3;
          for (int bytePos = 0; bytePos < end; bytePos += 8) {
            long l1 = getLong(array1, bytePos);
            long l2 = getLong(array2, bytePos);
            result += Long.bitCount(l1 ^ l2);
          }
          if ((length & 0x00000020) != 0) {
            int bytePos = end;
            int l1 = getInt(array1, bytePos);
            int l2 = getInt(array2, bytePos);
            result += Integer.bitCount(l1 ^ l2);
          }
          if ((length & 0x00000010) != 0) {
            int bytePos = (length & 0xFFFFFFE0) >>> 3;
            int l1 = getShort(array1, bytePos);
            int l2 = getShort(array2, bytePos);
            result += Integer.bitCount((l1 ^ l2) & 0xFFFF);
          }
          if ((length & 0x00000008) != 0) {
            int bytePos = (length & 0xFFFFFFF0) >>> 3;
            int l1 = array1[bytePos];
            int l2 = array2[bytePos];
            result += Integer.bitCount((l1 ^ l2) & 0xFF);
          }
          if ((length & 0x00000007) != 0) {
            int bytePos = length >>> 3;
            int l1 = array1[bytePos];
            int l2 = array2[bytePos];
            result += Integer.bitCount((l1 ^ l2) & (0xFFFFFFFF >>> -(length & 7)));
          }
          return length - result;
        }
      };

  private static final PackedArrayHandler HANDLER_2 =
      new AbstractPackedArrayHandlerPeriod4(2) {

        @Override
        public long get(byte[] array, int idx) {
          return get1(array, idx, 0, ((idx & 3) << 1));
        }

        @Override
        public long set(byte[] array, int idx, long value) {
          return set1(array, idx, 0, ((idx & 3) << 1), value);
        }

        @Override
        public long update(byte[] array, int idx, long value, LongBinaryOperator operator) {
          return update1(array, idx, 0, ((idx & 3) << 1), value, operator);
        }
      };

  private static final PackedArrayHandler HANDLER_3 =
      new AbstractPackedArrayHandlerPeriod8(3) {

        @Override
        public long get(byte[] array, int idx) {
          switch (idx & 0x7) {
            case 0:
              return get1(array, idx, 0, 0);
            case 1:
              return get1(array, idx, 0, 3);
            case 2:
              return get2(array, idx, 0, 6);
            case 3:
              return get1(array, idx, 1, 1);
            case 4:
              return get1(array, idx, 1, 4);
            case 5:
              return get2(array, idx, 1, 7);
            case 6:
              return get1(array, idx, 2, 2);
            default:
              return get1(array, idx, 2, 5);
          }
        }

        @Override
        public long set(byte[] array, int idx, long value) {
          switch (idx & 0x7) {
            case 0:
              return set1(array, idx, 0, 0, value);
            case 1:
              return set1(array, idx, 0, 3, value);
            case 2:
              return set2(array, idx, 0, 6, value);
            case 3:
              return set1(array, idx, 1, 1, value);
            case 4:
              return set1(array, idx, 1, 4, value);
            case 5:
              return set2(array, idx, 1, 7, value);
            case 6:
              return set1(array, idx, 2, 2, value);
            default:
              return set1(array, idx, 2, 5, value);
          }
        }

        @Override
        public long update(byte[] array, int idx, long value, LongBinaryOperator operator) {
          switch (idx & 0x7) {
            case 0:
              return update1(array, idx, 0, 0, value, operator);
            case 1:
              return update1(array, idx, 0, 3, value, operator);
            case 2:
              return update2(array, idx, 0, 6, value, operator);
            case 3:
              return update1(array, idx, 1, 1, value, operator);
            case 4:
              return update1(array, idx, 1, 4, value, operator);
            case 5:
              return update2(array, idx, 1, 7, value, operator);
            case 6:
              return update1(array, idx, 2, 2, value, operator);
            default:
              return update1(array, idx, 2, 5, value, operator);
          }
        }
      };

  private static final PackedArrayHandler HANDLER_4 =
      new AbstractPackedArrayHandlerPeriod2(4) {

        @Override
        public long get(byte[] array, int idx) {
          int k = idx & 0x1;
          return get1(array, idx, 0, k << 2);
        }

        @Override
        public long set(byte[] array, int idx, long value) {
          int k = idx & 0x1;
          return set1(array, idx, 0, k << 2, value);
        }

        @Override
        public long update(byte[] array, int idx, long value, LongBinaryOperator operator) {
          int k = idx & 0x1;
          return update1(array, idx, 0, k << 2, value, operator);
        }
      };

  private static final PackedArrayHandler HANDLER_5 =
      new AbstractPackedArrayHandlerPeriod8(5) {

        @Override
        public long get(byte[] array, int idx) {
          switch (idx & 0x7) {
            case 0:
              return get1(array, idx, 0, 0);
            case 1:
              return get2(array, idx, 0, 5);
            case 2:
              return get1(array, idx, 1, 2);
            case 3:
              return get2(array, idx, 1, 7);
            case 4:
              return get2(array, idx, 2, 4);
            case 5:
              return get1(array, idx, 3, 1);
            case 6:
              return get2(array, idx, 3, 6);
            default:
              return get1(array, idx, 4, 3);
          }
        }

        @Override
        public long set(byte[] array, int idx, long value) {
          switch (idx & 0x7) {
            case 0:
              return set1(array, idx, 0, 0, value);
            case 1:
              return set2(array, idx, 0, 5, value);
            case 2:
              return set1(array, idx, 1, 2, value);
            case 3:
              return set2(array, idx, 1, 7, value);
            case 4:
              return set2(array, idx, 2, 4, value);
            case 5:
              return set1(array, idx, 3, 1, value);
            case 6:
              return set2(array, idx, 3, 6, value);
            default:
              return set1(array, idx, 4, 3, value);
          }
        }

        @Override
        public long update(byte[] array, int idx, long value, LongBinaryOperator operator) {
          switch (idx & 0x7) {
            case 0:
              return update1(array, idx, 0, 0, value, operator);
            case 1:
              return update2(array, idx, 0, 5, value, operator);
            case 2:
              return update1(array, idx, 1, 2, value, operator);
            case 3:
              return update2(array, idx, 1, 7, value, operator);
            case 4:
              return update2(array, idx, 2, 4, value, operator);
            case 5:
              return update1(array, idx, 3, 1, value, operator);
            case 6:
              return update2(array, idx, 3, 6, value, operator);
            default:
              return update1(array, idx, 4, 3, value, operator);
          }
        }
      };

  private static final PackedArrayHandler HANDLER_6 =
      new AbstractPackedArrayHandlerPeriod4(6) {

        @Override
        public long get(byte[] array, int idx) {
          switch (idx & 0x3) {
            case 0:
              return get1(array, idx, 0, 0);
            case 1:
              return get2(array, idx, 0, 6);
            case 2:
              return get2(array, idx, 1, 4);
            default:
              return get1(array, idx, 2, 2);
          }
        }

        @Override
        public long set(byte[] array, int idx, long value) {
          switch (idx & 0x3) {
            case 0:
              return set1(array, idx, 0, 0, value);
            case 1:
              return set2(array, idx, 0, 6, value);
            case 2:
              return set2(array, idx, 1, 4, value);
            default:
              return set1(array, idx, 2, 2, value);
          }
        }

        @Override
        public long update(byte[] array, int idx, long value, LongBinaryOperator operator) {
          switch (idx & 0x3) {
            case 0:
              return update1(array, idx, 0, 0, value, operator);
            case 1:
              return update2(array, idx, 0, 6, value, operator);
            case 2:
              return update2(array, idx, 1, 4, value, operator);
            default:
              return update1(array, idx, 2, 2, value, operator);
          }
        }
      };

  private static final PackedArrayHandler HANDLER_7 =
      new AbstractPackedArrayHandlerPeriod8(7) {

        @Override
        public long get(byte[] array, int idx) {
          switch (idx & 0x7) {
            case 0:
              return get1(array, idx, 0, 0);
            case 1:
              return get2(array, idx, 0, 7);
            case 2:
              return get2(array, idx, 1, 6);
            case 3:
              return get2(array, idx, 2, 5);
            case 4:
              return get2(array, idx, 3, 4);
            case 5:
              return get2(array, idx, 4, 3);
            case 6:
              return get2(array, idx, 5, 2);
            default:
              return get1(array, idx, 6, 1);
          }
        }

        @Override
        public long set(byte[] array, int idx, long value) {
          switch (idx & 0x7) {
            case 0:
              return set1(array, idx, 0, 0, value);
            case 1:
              return set2(array, idx, 0, 7, value);
            case 2:
              return set2(array, idx, 1, 6, value);
            case 3:
              return set2(array, idx, 2, 5, value);
            case 4:
              return set2(array, idx, 3, 4, value);
            case 5:
              return set2(array, idx, 4, 3, value);
            case 6:
              return set2(array, idx, 5, 2, value);
            default:
              return set1(array, idx, 6, 1, value);
          }
        }

        @Override
        public long update(byte[] array, int idx, long value, LongBinaryOperator operator) {
          switch (idx & 0x7) {
            case 0:
              return update1(array, idx, 0, 0, value, operator);
            case 1:
              return update2(array, idx, 0, 7, value, operator);
            case 2:
              return update2(array, idx, 1, 6, value, operator);
            case 3:
              return update2(array, idx, 2, 5, value, operator);
            case 4:
              return update2(array, idx, 3, 4, value, operator);
            case 5:
              return update2(array, idx, 4, 3, value, operator);
            case 6:
              return update2(array, idx, 5, 2, value, operator);
            default:
              return update1(array, idx, 6, 1, value, operator);
          }
        }
      };

  private static final PackedArrayHandler HANDLER_8 =
      new AbstractPackedArrayHandlerPeriod1(8) {

        @Override
        public long get(byte[] array, int idx) {
          return get1(array, idx, 0, 0);
        }

        @Override
        public long set(byte[] array, int idx, long value) {
          return set1(array, idx, 0, 0, value);
        }

        @Override
        public long update(byte[] array, int idx, long value, LongBinaryOperator operator) {
          return update1(array, idx, 0, 0, value, operator);
        }
      };

  private static final PackedArrayHandler HANDLER_9 =
      new AbstractPackedArrayHandlerPeriod8(9) {

        @Override
        public long get(byte[] array, int idx) {
          int off = idx & 0x7;
          return get2(array, idx, off, off);
        }

        @Override
        public long set(byte[] array, int idx, long value) {
          int off = idx & 0x7;
          return set2(array, idx, off, off, value);
        }

        @Override
        public long update(byte[] array, int idx, long value, LongBinaryOperator operator) {
          int off = idx & 0x7;
          return update2(array, idx, off, off, value, operator);
        }
      };

  private static final PackedArrayHandler HANDLER_10 =
      new AbstractPackedArrayHandlerPeriod4(10) {

        @Override
        public long get(byte[] array, int idx) {
          int off = idx & 0x3;
          return get2(array, idx, off, off << 1);
        }

        @Override
        public long set(byte[] array, int idx, long value) {
          int off = idx & 0x3;
          return set2(array, idx, off, off << 1, value);
        }

        @Override
        public long update(byte[] array, int idx, long value, LongBinaryOperator operator) {
          int off = idx & 0x3;
          return update2(array, idx, off, off << 1, value, operator);
        }
      };

  private static final PackedArrayHandler HANDLER_11 =
      new AbstractPackedArrayHandlerPeriod8(11) {

        @Override
        public long get(byte[] array, int idx) {
          switch (idx & 0x7) {
            case 0:
              return get2(array, idx, 0, 0);
            case 1:
              return get2(array, idx, 1, 3);
            case 2:
              return get4(array, idx, 1, 14);
            case 3:
              return get2(array, idx, 4, 1);
            case 4:
              return get2(array, idx, 5, 4);
            case 5:
              return get4(array, idx, 5, 15);
            case 6:
              return get2(array, idx, 8, 2);
            default:
              return get2(array, idx, 9, 5);
          }
        }

        @Override
        public long set(byte[] array, int idx, long value) {
          switch (idx & 0x7) {
            case 0:
              return set2(array, idx, 0, 0, value);
            case 1:
              return set2(array, idx, 1, 3, value);
            case 2:
              return set4(array, idx, 1, 14, value);
            case 3:
              return set2(array, idx, 4, 1, value);
            case 4:
              return set2(array, idx, 5, 4, value);
            case 5:
              return set4(array, idx, 5, 15, value);
            case 6:
              return set2(array, idx, 8, 2, value);
            default:
              return set2(array, idx, 9, 5, value);
          }
        }

        @Override
        public long update(byte[] array, int idx, long value, LongBinaryOperator operator) {
          switch (idx & 0x7) {
            case 0:
              return update2(array, idx, 0, 0, value, operator);
            case 1:
              return update2(array, idx, 1, 3, value, operator);
            case 2:
              return update4(array, idx, 1, 14, value, operator);
            case 3:
              return update2(array, idx, 4, 1, value, operator);
            case 4:
              return update2(array, idx, 5, 4, value, operator);
            case 5:
              return update4(array, idx, 5, 15, value, operator);
            case 6:
              return update2(array, idx, 8, 2, value, operator);
            default:
              return update2(array, idx, 9, 5, value, operator);
          }
        }
      };

  private static final PackedArrayHandler HANDLER_12 =
      new AbstractPackedArrayHandlerPeriod2(12) {

        @Override
        public long get(byte[] array, int idx) {
          int k = idx & 0x1;
          return get2(array, idx, k, k << 2);
        }

        @Override
        public long set(byte[] array, int idx, long value) {
          int k = idx & 0x1;
          return set2(array, idx, k, k << 2, value);
        }

        @Override
        public long update(byte[] array, int idx, long value, LongBinaryOperator operator) {
          int k = idx & 0x1;
          return update2(array, idx, k, k << 2, value, operator);
        }
      };

  private static final PackedArrayHandler HANDLER_13 =
      new AbstractPackedArrayHandlerPeriod8(13) {

        @Override
        public long get(byte[] array, int idx) {
          switch (idx & 0x7) {
            case 0:
              return get2(array, idx, 0, 0);
            case 1:
              return get4(array, idx, 0, 13);
            case 2:
              return get2(array, idx, 3, 2);
            case 3:
              return get4(array, idx, 3, 15);
            case 4:
              return get4(array, idx, 5, 12);
            case 5:
              return get2(array, idx, 8, 1);
            case 6:
              return get4(array, idx, 8, 14);
            default:
              return get2(array, idx, 11, 3);
          }
        }

        @Override
        public long set(byte[] array, int idx, long value) {
          switch (idx & 0x7) {
            case 0:
              return set2(array, idx, 0, 0, value);
            case 1:
              return set4(array, idx, 0, 13, value);
            case 2:
              return set2(array, idx, 3, 2, value);
            case 3:
              return set4(array, idx, 3, 15, value);
            case 4:
              return set4(array, idx, 5, 12, value);
            case 5:
              return set2(array, idx, 8, 1, value);
            case 6:
              return set4(array, idx, 8, 14, value);
            default:
              return set2(array, idx, 11, 3, value);
          }
        }

        @Override
        public long update(byte[] array, int idx, long value, LongBinaryOperator operator) {
          switch (idx & 0x7) {
            case 0:
              return update2(array, idx, 0, 0, value, operator);
            case 1:
              return update4(array, idx, 0, 13, value, operator);
            case 2:
              return update2(array, idx, 3, 2, value, operator);
            case 3:
              return update4(array, idx, 3, 15, value, operator);
            case 4:
              return update4(array, idx, 5, 12, value, operator);
            case 5:
              return update2(array, idx, 8, 1, value, operator);
            case 6:
              return update4(array, idx, 8, 14, value, operator);
            default:
              return update2(array, idx, 11, 3, value, operator);
          }
        }
      };

  private static final PackedArrayHandler HANDLER_14 =
      new AbstractPackedArrayHandlerPeriod4(14) {

        @Override
        public long get(byte[] array, int idx) {
          switch (idx & 0x3) {
            case 0:
              return get2(array, idx, 0, 0);
            case 1:
              return get4(array, idx, 0, 14);
            case 2:
              return get4(array, idx, 2, 12);
            default:
              return get2(array, idx, 5, 2);
          }
        }

        @Override
        public long set(byte[] array, int idx, long value) {
          switch (idx & 0x3) {
            case 0:
              return set2(array, idx, 0, 0, value);
            case 1:
              return set4(array, idx, 0, 14, value);
            case 2:
              return set4(array, idx, 2, 12, value);
            default:
              return set2(array, idx, 5, 2, value);
          }
        }

        @Override
        public long update(byte[] array, int idx, long value, LongBinaryOperator operator) {
          switch (idx & 0x3) {
            case 0:
              return update2(array, idx, 0, 0, value, operator);
            case 1:
              return update4(array, idx, 0, 14, value, operator);
            case 2:
              return update4(array, idx, 2, 12, value, operator);
            default:
              return update2(array, idx, 5, 2, value, operator);
          }
        }
      };

  private static final PackedArrayHandler HANDLER_15 =
      new AbstractPackedArrayHandlerPeriod8(15) {

        @Override
        public long get(byte[] array, int idx) {
          switch (idx & 0x7) {
            case 0:
              return get2(array, idx, 0, 0);
            case 1:
              return get4(array, idx, 0, 15);
            case 2:
              return get4(array, idx, 2, 14);
            case 3:
              return get4(array, idx, 4, 13);
            case 4:
              return get4(array, idx, 6, 12);
            case 5:
              return get4(array, idx, 8, 11);
            case 6:
              return get4(array, idx, 10, 10);
            default:
              return get2(array, idx, 13, 1);
          }
        }

        @Override
        public long set(byte[] array, int idx, long value) {
          switch (idx & 0x7) {
            case 0:
              return set2(array, idx, 0, 0, value);
            case 1:
              return set4(array, idx, 0, 15, value);
            case 2:
              return set4(array, idx, 2, 14, value);
            case 3:
              return set4(array, idx, 4, 13, value);
            case 4:
              return set4(array, idx, 6, 12, value);
            case 5:
              return set4(array, idx, 8, 11, value);
            case 6:
              return set4(array, idx, 10, 10, value);
            default:
              return set2(array, idx, 13, 1, value);
          }
        }

        @Override
        public long update(byte[] array, int idx, long value, LongBinaryOperator operator) {
          switch (idx & 0x7) {
            case 0:
              return update2(array, idx, 0, 0, value, operator);
            case 1:
              return update4(array, idx, 0, 15, value, operator);
            case 2:
              return update4(array, idx, 2, 14, value, operator);
            case 3:
              return update4(array, idx, 4, 13, value, operator);
            case 4:
              return update4(array, idx, 6, 12, value, operator);
            case 5:
              return update4(array, idx, 8, 11, value, operator);
            case 6:
              return update4(array, idx, 10, 10, value, operator);
            default:
              return update2(array, idx, 13, 1, value, operator);
          }
        }
      };

  private static final PackedArrayHandler HANDLER_16 =
      new AbstractPackedArrayHandlerPeriod1(16) {

        @Override
        public long get(byte[] array, int idx) {
          return get2(array, idx, 0, 0);
        }

        @Override
        public long set(byte[] array, int idx, long value) {
          return set2(array, idx, 0, 0, value);
        }

        @Override
        public long update(byte[] array, int idx, long value, LongBinaryOperator operator) {
          return update2(array, idx, 0, 0, value, operator);
        }
      };

  private static final PackedArrayHandler HANDLER_17 =
      new AbstractPackedArrayHandlerPeriod8(17) {

        @Override
        public long get(byte[] array, int idx) {
          int k = idx & 0x7;
          return get3(array, idx, k << 1, k);
        }

        @Override
        public long set(byte[] array, int idx, long value) {
          int k = idx & 0x7;
          return set3(array, idx, k << 1, k, value);
        }

        @Override
        public long update(byte[] array, int idx, long value, LongBinaryOperator operator) {
          int k = idx & 0x7;
          return update3(array, idx, k << 1, k, value, operator);
        }
      };

  private static final PackedArrayHandler HANDLER_18 =
      new AbstractPackedArrayHandlerPeriod4(18) {

        @Override
        public long get(byte[] array, int idx) {
          int k = (idx & 0x3) << 1;
          return get3(array, idx, k, k);
        }

        @Override
        public long set(byte[] array, int idx, long value) {
          int k = (idx & 0x3) << 1;
          return set3(array, idx, k, k, value);
        }

        @Override
        public long update(byte[] array, int idx, long value, LongBinaryOperator operator) {
          int k = (idx & 0x3) << 1;
          return update3(array, idx, k, k, value, operator);
        }
      };

  private static final PackedArrayHandler HANDLER_19 =
      new AbstractPackedArrayHandlerPeriod8(19) {

        @Override
        public long get(byte[] array, int idx) {
          switch (idx & 0x7) {
            case 0:
              return get3(array, idx, 0, 0);
            case 1:
              return get4(array, idx, 1, 11);
            case 2:
              return get4(array, idx, 4, 6);
            case 3:
              return get4(array, idx, 6, 9);
            case 4:
              return get4(array, idx, 8, 12);
            case 5:
              return get4(array, idx, 11, 7);
            case 6:
              return get4(array, idx, 13, 10);
            default:
              return get4(array, idx, 15, 13);
          }
        }

        @Override
        public long set(byte[] array, int idx, long value) {
          switch (idx & 0x7) {
            case 0:
              return set3(array, idx, 0, 0, value);
            case 1:
              return set4(array, idx, 1, 11, value);
            case 2:
              return set4(array, idx, 4, 6, value);
            case 3:
              return set4(array, idx, 6, 9, value);
            case 4:
              return set4(array, idx, 8, 12, value);
            case 5:
              return set4(array, idx, 11, 7, value);
            case 6:
              return set4(array, idx, 13, 10, value);
            default:
              return set4(array, idx, 15, 13, value);
          }
        }

        @Override
        public long update(byte[] array, int idx, long value, LongBinaryOperator operator) {
          switch (idx & 0x7) {
            case 0:
              return update3(array, idx, 0, 0, value, operator);
            case 1:
              return update4(array, idx, 1, 11, value, operator);
            case 2:
              return update4(array, idx, 4, 6, value, operator);
            case 3:
              return update4(array, idx, 6, 9, value, operator);
            case 4:
              return update4(array, idx, 8, 12, value, operator);
            case 5:
              return update4(array, idx, 11, 7, value, operator);
            case 6:
              return update4(array, idx, 13, 10, value, operator);
            default:
              return update4(array, idx, 15, 13, value, operator);
          }
        }
      };

  private static final PackedArrayHandler HANDLER_20 =
      new AbstractPackedArrayHandlerPeriod2(20) {

        @Override
        public long get(byte[] array, int idx) {
          int k = idx & 0x1;
          return get3(array, idx, k << 1, k << 2);
        }

        @Override
        public long set(byte[] array, int idx, long value) {
          int k = idx & 0x1;
          return set3(array, idx, k << 1, k << 2, value);
        }

        @Override
        public long update(byte[] array, int idx, long value, LongBinaryOperator operator) {
          int k = idx & 0x1;
          return update3(array, idx, k << 1, k << 2, value, operator);
        }
      };

  private static final PackedArrayHandler HANDLER_21 =
      new AbstractPackedArrayHandlerPeriod8(21) {

        @Override
        public long get(byte[] array, int idx) {
          switch (idx & 0x7) {
            case 0:
              return get3(array, idx, 0, 0);
            case 1:
              return get4(array, idx, 2, 5);
            case 2:
              return get4(array, idx, 4, 10);
            case 3:
              return get4(array, idx, 7, 7);
            case 4:
              return get4(array, idx, 10, 4);
            case 5:
              return get4(array, idx, 12, 9);
            case 6:
              return get4(array, idx, 15, 6);
            default:
              return get4(array, idx, 17, 11);
          }
        }

        @Override
        public long set(byte[] array, int idx, long value) {
          switch (idx & 0x7) {
            case 0:
              return set3(array, idx, 0, 0, value);
            case 1:
              return set4(array, idx, 2, 5, value);
            case 2:
              return set4(array, idx, 4, 10, value);
            case 3:
              return set4(array, idx, 7, 7, value);
            case 4:
              return set4(array, idx, 10, 4, value);
            case 5:
              return set4(array, idx, 12, 9, value);
            case 6:
              return set4(array, idx, 15, 6, value);
            default:
              return set4(array, idx, 17, 11, value);
          }
        }

        @Override
        public long update(byte[] array, int idx, long value, LongBinaryOperator operator) {
          switch (idx & 0x7) {
            case 0:
              return update3(array, idx, 0, 0, value, operator);
            case 1:
              return update4(array, idx, 2, 5, value, operator);
            case 2:
              return update4(array, idx, 4, 10, value, operator);
            case 3:
              return update4(array, idx, 7, 7, value, operator);
            case 4:
              return update4(array, idx, 10, 4, value, operator);
            case 5:
              return update4(array, idx, 12, 9, value, operator);
            case 6:
              return update4(array, idx, 15, 6, value, operator);
            default:
              return update4(array, idx, 17, 11, value, operator);
          }
        }
      };

  private static final PackedArrayHandler HANDLER_22 =
      new AbstractPackedArrayHandlerPeriod4(22) {

        @Override
        public long get(byte[] array, int idx) {
          switch (idx & 0x3) {
            case 0:
              return get3(array, idx, 0, 0);
            case 1:
              return get4(array, idx, 2, 6);
            case 2:
              return get4(array, idx, 5, 4);
            default:
              return get4(array, idx, 7, 10);
          }
        }

        @Override
        public long set(byte[] array, int idx, long value) {
          switch (idx & 0x3) {
            case 0:
              return set3(array, idx, 0, 0, value);
            case 1:
              return set4(array, idx, 2, 6, value);
            case 2:
              return set4(array, idx, 5, 4, value);
            default:
              return set4(array, idx, 7, 10, value);
          }
        }

        @Override
        public long update(byte[] array, int idx, long value, LongBinaryOperator operator) {
          switch (idx & 0x3) {
            case 0:
              return update3(array, idx, 0, 0, value, operator);
            case 1:
              return update4(array, idx, 2, 6, value, operator);
            case 2:
              return update4(array, idx, 5, 4, value, operator);
            default:
              return update4(array, idx, 7, 10, value, operator);
          }
        }
      };

  private static final PackedArrayHandler HANDLER_23 =
      new AbstractPackedArrayHandlerPeriod8(23) {

        @Override
        public long get(byte[] array, int idx) {
          switch (idx & 0x7) {
            case 0:
              return get3(array, idx, 0, 0);
            case 1:
              return get4(array, idx, 2, 7);
            case 2:
              return get4(array, idx, 5, 6);
            case 3:
              return get4(array, idx, 8, 5);
            case 4:
              return get4(array, idx, 11, 4);
            case 5:
              return get4(array, idx, 14, 3);
            case 6:
              return get4(array, idx, 17, 2);
            default:
              return get4(array, idx, 19, 9);
          }
        }

        @Override
        public long set(byte[] array, int idx, long value) {
          switch (idx & 0x7) {
            case 0:
              return set3(array, idx, 0, 0, value);
            case 1:
              return set4(array, idx, 2, 7, value);
            case 2:
              return set4(array, idx, 5, 6, value);
            case 3:
              return set4(array, idx, 8, 5, value);
            case 4:
              return set4(array, idx, 11, 4, value);
            case 5:
              return set4(array, idx, 14, 3, value);
            case 6:
              return set4(array, idx, 17, 2, value);
            default:
              return set4(array, idx, 19, 9, value);
          }
        }

        @Override
        public long update(byte[] array, int idx, long value, LongBinaryOperator operator) {
          switch (idx & 0x7) {
            case 0:
              return update3(array, idx, 0, 0, value, operator);
            case 1:
              return update4(array, idx, 2, 7, value, operator);
            case 2:
              return update4(array, idx, 5, 6, value, operator);
            case 3:
              return update4(array, idx, 8, 5, value, operator);
            case 4:
              return update4(array, idx, 11, 4, value, operator);
            case 5:
              return update4(array, idx, 14, 3, value, operator);
            case 6:
              return update4(array, idx, 17, 2, value, operator);
            default:
              return update4(array, idx, 19, 9, value, operator);
          }
        }
      };

  private static final PackedArrayHandler HANDLER_24 =
      new AbstractPackedArrayHandlerPeriod1(24) {

        @Override
        public long get(byte[] array, int idx) {
          return get3(array, idx, 0, 0);
        }

        @Override
        public long set(byte[] array, int idx, long value) {
          return set3(array, idx, 0, 0, value);
        }

        @Override
        public long update(byte[] array, int idx, long value, LongBinaryOperator operator) {
          return update3(array, idx, 0, 0, value, operator);
        }
      };

  private static final PackedArrayHandler HANDLER_25 =
      new AbstractPackedArrayHandlerPeriod8(25) {

        @Override
        public long get(byte[] array, int idx) {
          int k = idx & 0x7;
          return get4(array, idx, 3 * k, k);
        }

        @Override
        public long set(byte[] array, int idx, long value) {
          int k = idx & 0x7;
          return set4(array, idx, 3 * k, k, value);
        }

        @Override
        public long update(byte[] array, int idx, long value, LongBinaryOperator operator) {
          int k = idx & 0x7;
          return update4(array, idx, 3 * k, k, value, operator);
        }
      };

  private static final PackedArrayHandler HANDLER_26 =
      new AbstractPackedArrayHandlerPeriod4(26) {

        @Override
        public long get(byte[] array, int idx) {
          int k = idx & 0x3;
          return get4(array, idx, 3 * k, k << 1);
        }

        @Override
        public long set(byte[] array, int idx, long value) {
          int k = idx & 0x3;
          return set4(array, idx, 3 * k, k << 1, value);
        }

        @Override
        public long update(byte[] array, int idx, long value, LongBinaryOperator operator) {
          int k = idx & 0x3;
          return update4(array, idx, 3 * k, k << 1, value, operator);
        }
      };

  private static final PackedArrayHandler HANDLER_27 =
      new AbstractPackedArrayHandlerPeriod8(27) {

        @Override
        public long get(byte[] array, int idx) {
          switch (idx & 0x7) {
            case 0:
              return get4(array, idx, 0, 0);
            case 1:
              return get4(array, idx, 3, 3);
            case 2:
              return get8(array, idx, 3, 30);
            case 3:
              return get4(array, idx, 10, 1);
            case 4:
              return get4(array, idx, 13, 4);
            case 5:
              return get8(array, idx, 13, 31);
            case 6:
              return get4(array, idx, 20, 2);
            default:
              return get4(array, idx, 23, 5);
          }
        }

        @Override
        public long set(byte[] array, int idx, long value) {
          switch (idx & 0x7) {
            case 0:
              return set4(array, idx, 0, 0, value);
            case 1:
              return set4(array, idx, 3, 3, value);
            case 2:
              return set8(array, idx, 3, 30, value);
            case 3:
              return set4(array, idx, 10, 1, value);
            case 4:
              return set4(array, idx, 13, 4, value);
            case 5:
              return set8(array, idx, 13, 31, value);
            case 6:
              return set4(array, idx, 20, 2, value);
            default:
              return set4(array, idx, 23, 5, value);
          }
        }

        @Override
        public long update(byte[] array, int idx, long value, LongBinaryOperator operator) {
          switch (idx & 0x7) {
            case 0:
              return update4(array, idx, 0, 0, value, operator);
            case 1:
              return update4(array, idx, 3, 3, value, operator);
            case 2:
              return update8(array, idx, 3, 30, value, operator);
            case 3:
              return update4(array, idx, 10, 1, value, operator);
            case 4:
              return update4(array, idx, 13, 4, value, operator);
            case 5:
              return update8(array, idx, 13, 31, value, operator);
            case 6:
              return update4(array, idx, 20, 2, value, operator);
            default:
              return update4(array, idx, 23, 5, value, operator);
          }
        }
      };

  private static final PackedArrayHandler HANDLER_28 =
      new AbstractPackedArrayHandlerPeriod2(28) {

        @Override
        public long get(byte[] array, int idx) {
          int k = idx & 0x1;
          return get4(array, idx, 3 * k, k << 2);
        }

        @Override
        public long set(byte[] array, int idx, long value) {
          int k = idx & 0x1;
          return set4(array, idx, 3 * k, k << 2, value);
        }

        @Override
        public long update(byte[] array, int idx, long value, LongBinaryOperator operator) {
          int k = idx & 0x1;
          return update4(array, idx, 3 * k, k << 2, value, operator);
        }
      };

  private static final PackedArrayHandler HANDLER_29 =
      new AbstractPackedArrayHandlerPeriod8(29) {

        @Override
        public long get(byte[] array, int idx) {
          switch (idx & 0x7) {
            case 0:
              return get4(array, idx, 0, 0);
            case 1:
              return get8(array, idx, 0, 29);
            case 2:
              return get4(array, idx, 7, 2);
            case 3:
              return get8(array, idx, 7, 31);
            case 4:
              return get8(array, idx, 11, 28);
            case 5:
              return get4(array, idx, 18, 1);
            case 6:
              return get8(array, idx, 18, 30);
            default:
              return get4(array, idx, 25, 3);
          }
        }

        @Override
        public long set(byte[] array, int idx, long value) {
          switch (idx & 0x7) {
            case 0:
              return set4(array, idx, 0, 0, value);
            case 1:
              return set8(array, idx, 0, 29, value);
            case 2:
              return set4(array, idx, 7, 2, value);
            case 3:
              return set8(array, idx, 7, 31, value);
            case 4:
              return set8(array, idx, 11, 28, value);
            case 5:
              return set4(array, idx, 18, 1, value);
            case 6:
              return set8(array, idx, 18, 30, value);
            default:
              return set4(array, idx, 25, 3, value);
          }
        }

        @Override
        public long update(byte[] array, int idx, long value, LongBinaryOperator operator) {
          switch (idx & 0x7) {
            case 0:
              return update4(array, idx, 0, 0, value, operator);
            case 1:
              return update8(array, idx, 0, 29, value, operator);
            case 2:
              return update4(array, idx, 7, 2, value, operator);
            case 3:
              return update8(array, idx, 7, 31, value, operator);
            case 4:
              return update8(array, idx, 11, 28, value, operator);
            case 5:
              return update4(array, idx, 18, 1, value, operator);
            case 6:
              return update8(array, idx, 18, 30, value, operator);
            default:
              return update4(array, idx, 25, 3, value, operator);
          }
        }
      };

  private static final PackedArrayHandler HANDLER_30 =
      new AbstractPackedArrayHandlerPeriod4(30) {

        @Override
        public long get(byte[] array, int idx) {
          switch (idx & 0x3) {
            case 0:
              return get4(array, idx, 0, 0);
            case 1:
              return get8(array, idx, 0, 30);
            case 2:
              return get8(array, idx, 4, 28);
            default:
              return get4(array, idx, 11, 2);
          }
        }

        @Override
        public long set(byte[] array, int idx, long value) {
          switch (idx & 0x3) {
            case 0:
              return set4(array, idx, 0, 0, value);
            case 1:
              return set8(array, idx, 0, 30, value);
            case 2:
              return set8(array, idx, 4, 28, value);
            default:
              return set4(array, idx, 11, 2, value);
          }
        }

        @Override
        public long update(byte[] array, int idx, long value, LongBinaryOperator operator) {
          switch (idx & 0x3) {
            case 0:
              return update4(array, idx, 0, 0, value, operator);
            case 1:
              return update8(array, idx, 0, 30, value, operator);
            case 2:
              return update8(array, idx, 4, 28, value, operator);
            default:
              return update4(array, idx, 11, 2, value, operator);
          }
        }
      };

  private static final PackedArrayHandler HANDLER_31 =
      new AbstractPackedArrayHandlerPeriod8(31) {

        @Override
        public long get(byte[] array, int idx) {
          switch (idx & 0x7) {
            case 0:
              return get4(array, idx, 0, 0);
            case 1:
              return get8(array, idx, 0, 31);
            case 2:
              return get8(array, idx, 4, 30);
            case 3:
              return get8(array, idx, 8, 29);
            case 4:
              return get8(array, idx, 12, 28);
            case 5:
              return get8(array, idx, 16, 27);
            case 6:
              return get8(array, idx, 20, 26);
            default:
              return get4(array, idx, 27, 1);
          }
        }

        @Override
        public long set(byte[] array, int idx, long value) {
          switch (idx & 0x7) {
            case 0:
              return set4(array, idx, 0, 0, value);
            case 1:
              return set8(array, idx, 0, 31, value);
            case 2:
              return set8(array, idx, 4, 30, value);
            case 3:
              return set8(array, idx, 8, 29, value);
            case 4:
              return set8(array, idx, 12, 28, value);
            case 5:
              return set8(array, idx, 16, 27, value);
            case 6:
              return set8(array, idx, 20, 26, value);
            default:
              return set4(array, idx, 27, 1, value);
          }
        }

        @Override
        public long update(byte[] array, int idx, long value, LongBinaryOperator operator) {
          switch (idx & 0x7) {
            case 0:
              return update4(array, idx, 0, 0, value, operator);
            case 1:
              return update8(array, idx, 0, 31, value, operator);
            case 2:
              return update8(array, idx, 4, 30, value, operator);
            case 3:
              return update8(array, idx, 8, 29, value, operator);
            case 4:
              return update8(array, idx, 12, 28, value, operator);
            case 5:
              return update8(array, idx, 16, 27, value, operator);
            case 6:
              return update8(array, idx, 20, 26, value, operator);
            default:
              return update4(array, idx, 27, 1, value, operator);
          }
        }
      };

  private static final PackedArrayHandler HANDLER_32 =
      new AbstractPackedArrayHandlerPeriod1(32) {

        @Override
        public long get(byte[] array, int idx) {
          return get4(array, idx, 0, 0);
        }

        @Override
        public long set(byte[] array, int idx, long value) {
          return set4(array, idx, 0, 0, value);
        }

        @Override
        public long update(byte[] array, int idx, long value, LongBinaryOperator operator) {
          return update4(array, idx, 0, 0, value, operator);
        }
      };

  private static final PackedArrayHandler HANDLER_33 =
      new AbstractPackedArrayHandlerPeriod8(33) {

        @Override
        public long get(byte[] array, int idx) {
          int k = (idx & 0x7);
          return get5(array, idx, k << 2, k);
        }

        @Override
        public long set(byte[] array, int idx, long value) {
          int k = (idx & 0x7);
          return set5(array, idx, k << 2, k, value);
        }

        @Override
        public long update(byte[] array, int idx, long value, LongBinaryOperator operator) {
          int k = (idx & 0x7);
          return update5(array, idx, k << 2, k, value, operator);
        }
      };

  private static final PackedArrayHandler HANDLER_34 =
      new AbstractPackedArrayHandlerPeriod4(34) {

        @Override
        public long get(byte[] array, int idx) {
          int k = (idx & 0x3);
          return get5(array, idx, k << 2, k << 1);
        }

        @Override
        public long set(byte[] array, int idx, long value) {
          int k = (idx & 0x3);
          return set5(array, idx, k << 2, k << 1, value);
        }

        @Override
        public long update(byte[] array, int idx, long value, LongBinaryOperator operator) {
          int k = (idx & 0x3);
          return update5(array, idx, k << 2, k << 1, value, operator);
        }
      };

  private static final PackedArrayHandler HANDLER_35 =
      new AbstractPackedArrayHandlerPeriod8(35) {

        @Override
        public long get(byte[] array, int idx) {
          switch (idx & 0x7) {
            case 0:
              return get5(array, idx, 0, 0);
            case 1:
              return get8(array, idx, 1, 27);
            case 2:
              return get8(array, idx, 6, 22);
            case 3:
              return get8(array, idx, 10, 25);
            case 4:
              return get8(array, idx, 14, 28);
            case 5:
              return get8(array, idx, 19, 23);
            case 6:
              return get8(array, idx, 23, 26);
            default:
              return get8(array, idx, 27, 29);
          }
        }

        @Override
        public long set(byte[] array, int idx, long value) {
          switch (idx & 0x7) {
            case 0:
              return set5(array, idx, 0, 0, value);
            case 1:
              return set8(array, idx, 1, 27, value);
            case 2:
              return set8(array, idx, 6, 22, value);
            case 3:
              return set8(array, idx, 10, 25, value);
            case 4:
              return set8(array, idx, 14, 28, value);
            case 5:
              return set8(array, idx, 19, 23, value);
            case 6:
              return set8(array, idx, 23, 26, value);
            default:
              return set8(array, idx, 27, 29, value);
          }
        }

        @Override
        public long update(byte[] array, int idx, long value, LongBinaryOperator operator) {
          switch (idx & 0x7) {
            case 0:
              return update5(array, idx, 0, 0, value, operator);
            case 1:
              return update8(array, idx, 1, 27, value, operator);
            case 2:
              return update8(array, idx, 6, 22, value, operator);
            case 3:
              return update8(array, idx, 10, 25, value, operator);
            case 4:
              return update8(array, idx, 14, 28, value, operator);
            case 5:
              return update8(array, idx, 19, 23, value, operator);
            case 6:
              return update8(array, idx, 23, 26, value, operator);
            default:
              return update8(array, idx, 27, 29, value, operator);
          }
        }
      };

  private static final PackedArrayHandler HANDLER_36 =
      new AbstractPackedArrayHandlerPeriod2(36) {

        @Override
        public long get(byte[] array, int idx) {
          int k = (idx & 0x1) << 2;
          return get5(array, idx, k, k);
        }

        @Override
        public long set(byte[] array, int idx, long value) {
          int k = (idx & 0x1) << 2;
          return set5(array, idx, k, k, value);
        }

        @Override
        public long update(byte[] array, int idx, long value, LongBinaryOperator operator) {
          int k = (idx & 0x1) << 2;
          return update5(array, idx, k, k, value, operator);
        }
      };

  private static final PackedArrayHandler HANDLER_37 =
      new AbstractPackedArrayHandlerPeriod8(37) {

        @Override
        public long get(byte[] array, int idx) {
          switch (idx & 0x7) {
            case 0:
              return get5(array, idx, 0, 0);
            case 1:
              return get8(array, idx, 2, 21);
            case 2:
              return get8(array, idx, 6, 26);
            case 3:
              return get8(array, idx, 11, 23);
            case 4:
              return get8(array, idx, 16, 20);
            case 5:
              return get8(array, idx, 20, 25);
            case 6:
              return get8(array, idx, 25, 22);
            default:
              return get8(array, idx, 29, 27);
          }
        }

        @Override
        public long set(byte[] array, int idx, long value) {
          switch (idx & 0x7) {
            case 0:
              return set5(array, idx, 0, 0, value);
            case 1:
              return set8(array, idx, 2, 21, value);
            case 2:
              return set8(array, idx, 6, 26, value);
            case 3:
              return set8(array, idx, 11, 23, value);
            case 4:
              return set8(array, idx, 16, 20, value);
            case 5:
              return set8(array, idx, 20, 25, value);
            case 6:
              return set8(array, idx, 25, 22, value);
            default:
              return set8(array, idx, 29, 27, value);
          }
        }

        @Override
        public long update(byte[] array, int idx, long value, LongBinaryOperator operator) {
          switch (idx & 0x7) {
            case 0:
              return update5(array, idx, 0, 0, value, operator);
            case 1:
              return update8(array, idx, 2, 21, value, operator);
            case 2:
              return update8(array, idx, 6, 26, value, operator);
            case 3:
              return update8(array, idx, 11, 23, value, operator);
            case 4:
              return update8(array, idx, 16, 20, value, operator);
            case 5:
              return update8(array, idx, 20, 25, value, operator);
            case 6:
              return update8(array, idx, 25, 22, value, operator);
            default:
              return update8(array, idx, 29, 27, value, operator);
          }
        }
      };

  private static final PackedArrayHandler HANDLER_38 =
      new AbstractPackedArrayHandlerPeriod4(38) {

        @Override
        public long get(byte[] array, int idx) {
          switch (idx & 0x3) {
            case 0:
              return get5(array, idx, 0, 0);
            case 1:
              return get8(array, idx, 2, 22);
            case 2:
              return get8(array, idx, 7, 20);
            default:
              return get8(array, idx, 11, 26);
          }
        }

        @Override
        public long set(byte[] array, int idx, long value) {
          switch (idx & 0x3) {
            case 0:
              return set5(array, idx, 0, 0, value);
            case 1:
              return set8(array, idx, 2, 22, value);
            case 2:
              return set8(array, idx, 7, 20, value);
            default:
              return set8(array, idx, 11, 26, value);
          }
        }

        @Override
        public long update(byte[] array, int idx, long value, LongBinaryOperator operator) {
          switch (idx & 0x3) {
            case 0:
              return update5(array, idx, 0, 0, value, operator);
            case 1:
              return update8(array, idx, 2, 22, value, operator);
            case 2:
              return update8(array, idx, 7, 20, value, operator);
            default:
              return update8(array, idx, 11, 26, value, operator);
          }
        }
      };

  private static final PackedArrayHandler HANDLER_39 =
      new AbstractPackedArrayHandlerPeriod8(39) {

        @Override
        public long get(byte[] array, int idx) {
          switch (idx & 0x7) {
            case 0:
              return get5(array, idx, 0, 0);
            case 1:
              return get8(array, idx, 2, 23);
            case 2:
              return get8(array, idx, 7, 22);
            case 3:
              return get8(array, idx, 12, 21);
            case 4:
              return get8(array, idx, 17, 20);
            case 5:
              return get8(array, idx, 22, 19);
            case 6:
              return get8(array, idx, 27, 18);
            default:
              return get8(array, idx, 31, 25);
          }
        }

        @Override
        public long set(byte[] array, int idx, long value) {
          switch (idx & 0x7) {
            case 0:
              return set5(array, idx, 0, 0, value);
            case 1:
              return set8(array, idx, 2, 23, value);
            case 2:
              return set8(array, idx, 7, 22, value);
            case 3:
              return set8(array, idx, 12, 21, value);
            case 4:
              return set8(array, idx, 17, 20, value);
            case 5:
              return set8(array, idx, 22, 19, value);
            case 6:
              return set8(array, idx, 27, 18, value);
            default:
              return set8(array, idx, 31, 25, value);
          }
        }

        @Override
        public long update(byte[] array, int idx, long value, LongBinaryOperator operator) {
          switch (idx & 0x7) {
            case 0:
              return update5(array, idx, 0, 0, value, operator);
            case 1:
              return update8(array, idx, 2, 23, value, operator);
            case 2:
              return update8(array, idx, 7, 22, value, operator);
            case 3:
              return update8(array, idx, 12, 21, value, operator);
            case 4:
              return update8(array, idx, 17, 20, value, operator);
            case 5:
              return update8(array, idx, 22, 19, value, operator);
            case 6:
              return update8(array, idx, 27, 18, value, operator);
            default:
              return update8(array, idx, 31, 25, value, operator);
          }
        }
      };

  private static final PackedArrayHandler HANDLER_40 =
      new AbstractPackedArrayHandlerPeriod1(40) {

        @Override
        public long get(byte[] array, int idx) {
          return get5(array, idx, 0, 0);
        }

        @Override
        public long set(byte[] array, int idx, long value) {
          return set5(array, idx, 0, 0, value);
        }

        @Override
        public long update(byte[] array, int idx, long value, LongBinaryOperator operator) {
          return update5(array, idx, 0, 0, value, operator);
        }
      };

  private static final PackedArrayHandler HANDLER_41 =
      new AbstractPackedArrayHandlerPeriod8(41) {

        @Override
        public long get(byte[] array, int idx) {
          int k = idx & 0x7;
          return get6(array, idx, 5 * k, k);
        }

        @Override
        public long set(byte[] array, int idx, long value) {
          int k = idx & 0x7;
          return set6(array, idx, 5 * k, k, value);
        }

        @Override
        public long update(byte[] array, int idx, long value, LongBinaryOperator operator) {
          int k = idx & 0x7;
          return update6(array, idx, 5 * k, k, value, operator);
        }
      };

  private static final PackedArrayHandler HANDLER_42 =
      new AbstractPackedArrayHandlerPeriod4(42) {

        @Override
        public long get(byte[] array, int idx) {
          int k = idx & 0x3;
          return get6(array, idx, 5 * k, k << 1);
        }

        @Override
        public long set(byte[] array, int idx, long value) {
          int k = idx & 0x3;
          return set6(array, idx, 5 * k, k << 1, value);
        }

        @Override
        public long update(byte[] array, int idx, long value, LongBinaryOperator operator) {
          int k = idx & 0x3;
          return update6(array, idx, 5 * k, k << 1, value, operator);
        }
      };

  private static final PackedArrayHandler HANDLER_43 =
      new AbstractPackedArrayHandlerPeriod8(43) {

        @Override
        public long get(byte[] array, int idx) {
          switch (idx & 0x7) {
            case 0:
              return get6(array, idx, 0, 0);
            case 1:
              return get8(array, idx, 3, 19);
            case 2:
              return get8(array, idx, 9, 14);
            case 3:
              return get8(array, idx, 14, 17);
            case 4:
              return get8(array, idx, 19, 20);
            case 5:
              return get8(array, idx, 25, 15);
            case 6:
              return get8(array, idx, 30, 18);
            default:
              return get8(array, idx, 35, 21);
          }
        }

        @Override
        public long set(byte[] array, int idx, long value) {
          switch (idx & 0x7) {
            case 0:
              return set6(array, idx, 0, 0, value);
            case 1:
              return set8(array, idx, 3, 19, value);
            case 2:
              return set8(array, idx, 9, 14, value);
            case 3:
              return set8(array, idx, 14, 17, value);
            case 4:
              return set8(array, idx, 19, 20, value);
            case 5:
              return set8(array, idx, 25, 15, value);
            case 6:
              return set8(array, idx, 30, 18, value);
            default:
              return set8(array, idx, 35, 21, value);
          }
        }

        @Override
        public long update(byte[] array, int idx, long value, LongBinaryOperator operator) {
          switch (idx & 0x7) {
            case 0:
              return update6(array, idx, 0, 0, value, operator);
            case 1:
              return update8(array, idx, 3, 19, value, operator);
            case 2:
              return update8(array, idx, 9, 14, value, operator);
            case 3:
              return update8(array, idx, 14, 17, value, operator);
            case 4:
              return update8(array, idx, 19, 20, value, operator);
            case 5:
              return update8(array, idx, 25, 15, value, operator);
            case 6:
              return update8(array, idx, 30, 18, value, operator);
            default:
              return update8(array, idx, 35, 21, value, operator);
          }
        }
      };

  private static final PackedArrayHandler HANDLER_44 =
      new AbstractPackedArrayHandlerPeriod2(44) {

        @Override
        public long get(byte[] array, int idx) {
          int k = idx & 0x1;
          return get6(array, idx, 5 * k, k << 2);
        }

        @Override
        public long set(byte[] array, int idx, long value) {
          int k = idx & 0x1;
          return set6(array, idx, 5 * k, k << 2, value);
        }

        @Override
        public long update(byte[] array, int idx, long value, LongBinaryOperator operator) {
          int k = idx & 0x1;
          return update6(array, idx, 5 * k, k << 2, value, operator);
        }
      };

  private static final PackedArrayHandler HANDLER_45 =
      new AbstractPackedArrayHandlerPeriod8(45) {

        @Override
        public long get(byte[] array, int idx) {
          switch (idx & 0x7) {
            case 0:
              return get6(array, idx, 0, 0);
            case 1:
              return get8(array, idx, 4, 13);
            case 2:
              return get8(array, idx, 9, 18);
            case 3:
              return get8(array, idx, 15, 15);
            case 4:
              return get8(array, idx, 21, 12);
            case 5:
              return get8(array, idx, 26, 17);
            case 6:
              return get8(array, idx, 32, 14);
            default:
              return get8(array, idx, 37, 19);
          }
        }

        @Override
        public long set(byte[] array, int idx, long value) {
          switch (idx & 0x7) {
            case 0:
              return set6(array, idx, 0, 0, value);
            case 1:
              return set8(array, idx, 4, 13, value);
            case 2:
              return set8(array, idx, 9, 18, value);
            case 3:
              return set8(array, idx, 15, 15, value);
            case 4:
              return set8(array, idx, 21, 12, value);
            case 5:
              return set8(array, idx, 26, 17, value);
            case 6:
              return set8(array, idx, 32, 14, value);
            default:
              return set8(array, idx, 37, 19, value);
          }
        }

        @Override
        public long update(byte[] array, int idx, long value, LongBinaryOperator operator) {
          switch (idx & 0x7) {
            case 0:
              return update6(array, idx, 0, 0, value, operator);
            case 1:
              return update8(array, idx, 4, 13, value, operator);
            case 2:
              return update8(array, idx, 9, 18, value, operator);
            case 3:
              return update8(array, idx, 15, 15, value, operator);
            case 4:
              return update8(array, idx, 21, 12, value, operator);
            case 5:
              return update8(array, idx, 26, 17, value, operator);
            case 6:
              return update8(array, idx, 32, 14, value, operator);
            default:
              return update8(array, idx, 37, 19, value, operator);
          }
        }
      };

  private static final PackedArrayHandler HANDLER_46 =
      new AbstractPackedArrayHandlerPeriod4(46) {

        @Override
        public long get(byte[] array, int idx) {
          switch (idx & 0x3) {
            case 0:
              return get6(array, idx, 0, 0);
            case 1:
              return get8(array, idx, 4, 14);
            case 2:
              return get8(array, idx, 10, 12);
            default:
              return get8(array, idx, 15, 18);
          }
        }

        @Override
        public long set(byte[] array, int idx, long value) {
          switch (idx & 0x3) {
            case 0:
              return set6(array, idx, 0, 0, value);
            case 1:
              return set8(array, idx, 4, 14, value);
            case 2:
              return set8(array, idx, 10, 12, value);
            default:
              return set8(array, idx, 15, 18, value);
          }
        }

        @Override
        public long update(byte[] array, int idx, long value, LongBinaryOperator operator) {
          switch (idx & 0x3) {
            case 0:
              return update6(array, idx, 0, 0, value, operator);
            case 1:
              return update8(array, idx, 4, 14, value, operator);
            case 2:
              return update8(array, idx, 10, 12, value, operator);
            default:
              return update8(array, idx, 15, 18, value, operator);
          }
        }
      };

  private static final PackedArrayHandler HANDLER_47 =
      new AbstractPackedArrayHandlerPeriod8(47) {

        @Override
        public long get(byte[] array, int idx) {
          switch (idx & 0x7) {
            case 0:
              return get6(array, idx, 0, 0);
            case 1:
              return get8(array, idx, 4, 15);
            case 2:
              return get8(array, idx, 10, 14);
            case 3:
              return get8(array, idx, 16, 13);
            case 4:
              return get8(array, idx, 22, 12);
            case 5:
              return get8(array, idx, 28, 11);
            case 6:
              return get8(array, idx, 34, 10);
            default:
              return get8(array, idx, 39, 17);
          }
        }

        @Override
        public long set(byte[] array, int idx, long value) {
          switch (idx & 0x7) {
            case 0:
              return set6(array, idx, 0, 0, value);
            case 1:
              return set8(array, idx, 4, 15, value);
            case 2:
              return set8(array, idx, 10, 14, value);
            case 3:
              return set8(array, idx, 16, 13, value);
            case 4:
              return set8(array, idx, 22, 12, value);
            case 5:
              return set8(array, idx, 28, 11, value);
            case 6:
              return set8(array, idx, 34, 10, value);
            default:
              return set8(array, idx, 39, 17, value);
          }
        }

        @Override
        public long update(byte[] array, int idx, long value, LongBinaryOperator operator) {
          switch (idx & 0x7) {
            case 0:
              return update6(array, idx, 0, 0, value, operator);
            case 1:
              return update8(array, idx, 4, 15, value, operator);
            case 2:
              return update8(array, idx, 10, 14, value, operator);
            case 3:
              return update8(array, idx, 16, 13, value, operator);
            case 4:
              return update8(array, idx, 22, 12, value, operator);
            case 5:
              return update8(array, idx, 28, 11, value, operator);
            case 6:
              return update8(array, idx, 34, 10, value, operator);
            default:
              return update8(array, idx, 39, 17, value, operator);
          }
        }
      };

  private static final PackedArrayHandler HANDLER_48 =
      new AbstractPackedArrayHandlerPeriod1(48) {

        @Override
        public long get(byte[] array, int idx) {
          return get6(array, idx, 0, 0);
        }

        @Override
        public long set(byte[] array, int idx, long value) {
          return set6(array, idx, 0, 0, value);
        }

        @Override
        public long update(byte[] array, int idx, long value, LongBinaryOperator operator) {
          return update6(array, idx, 0, 0, value, operator);
        }
      };

  private static final PackedArrayHandler HANDLER_49 =
      new AbstractPackedArrayHandlerPeriod8(49) {

        @Override
        public long get(byte[] array, int idx) {
          int k = idx & 0x7;
          return get7(array, idx, 6 * k, k);
        }

        @Override
        public long set(byte[] array, int idx, long value) {
          int k = idx & 0x7;
          return set7(array, idx, 6 * k, k, value);
        }

        @Override
        public long update(byte[] array, int idx, long value, LongBinaryOperator operator) {
          int k = idx & 0x7;
          return update7(array, idx, 6 * k, k, value, operator);
        }
      };

  private static final PackedArrayHandler HANDLER_50 =
      new AbstractPackedArrayHandlerPeriod4(50) {

        @Override
        public long get(byte[] array, int idx) {
          int k = idx & 0x3;
          return get7(array, idx, 6 * k, k << 1);
        }

        @Override
        public long set(byte[] array, int idx, long value) {
          int k = idx & 0x3;
          return set7(array, idx, 6 * k, k << 1, value);
        }

        @Override
        public long update(byte[] array, int idx, long value, LongBinaryOperator operator) {
          int k = idx & 0x3;
          return update7(array, idx, 6 * k, k << 1, value, operator);
        }
      };

  private static final PackedArrayHandler HANDLER_51 =
      new AbstractPackedArrayHandlerPeriod8(51) {

        @Override
        public long get(byte[] array, int idx) {
          switch (idx & 0x7) {
            case 0:
              return get7(array, idx, 0, 0);
            case 1:
              return get8(array, idx, 5, 11);
            case 2:
              return get8(array, idx, 12, 6);
            case 3:
              return get8(array, idx, 18, 9);
            case 4:
              return get8(array, idx, 24, 12);
            case 5:
              return get8(array, idx, 31, 7);
            case 6:
              return get8(array, idx, 37, 10);
            default:
              return get8(array, idx, 43, 13);
          }
        }

        @Override
        public long set(byte[] array, int idx, long value) {
          switch (idx & 0x7) {
            case 0:
              return set7(array, idx, 0, 0, value);
            case 1:
              return set8(array, idx, 5, 11, value);
            case 2:
              return set8(array, idx, 12, 6, value);
            case 3:
              return set8(array, idx, 18, 9, value);
            case 4:
              return set8(array, idx, 24, 12, value);
            case 5:
              return set8(array, idx, 31, 7, value);
            case 6:
              return set8(array, idx, 37, 10, value);
            default:
              return set8(array, idx, 43, 13, value);
          }
        }

        @Override
        public long update(byte[] array, int idx, long value, LongBinaryOperator operator) {
          switch (idx & 0x7) {
            case 0:
              return update7(array, idx, 0, 0, value, operator);
            case 1:
              return update8(array, idx, 5, 11, value, operator);
            case 2:
              return update8(array, idx, 12, 6, value, operator);
            case 3:
              return update8(array, idx, 18, 9, value, operator);
            case 4:
              return update8(array, idx, 24, 12, value, operator);
            case 5:
              return update8(array, idx, 31, 7, value, operator);
            case 6:
              return update8(array, idx, 37, 10, value, operator);
            default:
              return update8(array, idx, 43, 13, value, operator);
          }
        }
      };

  private static final PackedArrayHandler HANDLER_52 =
      new AbstractPackedArrayHandlerPeriod2(52) {

        @Override
        public long get(byte[] array, int idx) {
          int k = idx & 0x1;
          return get7(array, idx, 6 * k, k << 2);
        }

        @Override
        public long set(byte[] array, int idx, long value) {
          int k = idx & 0x1;
          return set7(array, idx, 6 * k, k << 2, value);
        }

        @Override
        public long update(byte[] array, int idx, long value, LongBinaryOperator operator) {
          int k = idx & 0x1;
          return update7(array, idx, 6 * k, k << 2, value, operator);
        }
      };

  private static final PackedArrayHandler HANDLER_53 =
      new AbstractPackedArrayHandlerPeriod8(53) {

        @Override
        public long get(byte[] array, int idx) {
          switch (idx & 0x7) {
            case 0:
              return get7(array, idx, 0, 0);
            case 1:
              return get8(array, idx, 6, 5);
            case 2:
              return get8(array, idx, 12, 10);
            case 3:
              return get8(array, idx, 19, 7);
            case 4:
              return get8(array, idx, 26, 4);
            case 5:
              return get8(array, idx, 32, 9);
            case 6:
              return get8(array, idx, 39, 6);
            default:
              return get8(array, idx, 45, 11);
          }
        }

        @Override
        public long set(byte[] array, int idx, long value) {
          switch (idx & 0x7) {
            case 0:
              return set7(array, idx, 0, 0, value);
            case 1:
              return set8(array, idx, 6, 5, value);
            case 2:
              return set8(array, idx, 12, 10, value);
            case 3:
              return set8(array, idx, 19, 7, value);
            case 4:
              return set8(array, idx, 26, 4, value);
            case 5:
              return set8(array, idx, 32, 9, value);
            case 6:
              return set8(array, idx, 39, 6, value);
            default:
              return set8(array, idx, 45, 11, value);
          }
        }

        @Override
        public long update(byte[] array, int idx, long value, LongBinaryOperator operator) {
          switch (idx & 0x7) {
            case 0:
              return update7(array, idx, 0, 0, value, operator);
            case 1:
              return update8(array, idx, 6, 5, value, operator);
            case 2:
              return update8(array, idx, 12, 10, value, operator);
            case 3:
              return update8(array, idx, 19, 7, value, operator);
            case 4:
              return update8(array, idx, 26, 4, value, operator);
            case 5:
              return update8(array, idx, 32, 9, value, operator);
            case 6:
              return update8(array, idx, 39, 6, value, operator);
            default:
              return update8(array, idx, 45, 11, value, operator);
          }
        }
      };

  private static final PackedArrayHandler HANDLER_54 =
      new AbstractPackedArrayHandlerPeriod4(54) {

        @Override
        public long get(byte[] array, int idx) {
          switch (idx & 0x3) {
            case 0:
              return get7(array, idx, 0, 0);
            case 1:
              return get8(array, idx, 6, 6);
            case 2:
              return get8(array, idx, 13, 4);
            default:
              return get8(array, idx, 19, 10);
          }
        }

        @Override
        public long set(byte[] array, int idx, long value) {
          switch (idx & 0x3) {
            case 0:
              return set7(array, idx, 0, 0, value);
            case 1:
              return set8(array, idx, 6, 6, value);
            case 2:
              return set8(array, idx, 13, 4, value);
            default:
              return set8(array, idx, 19, 10, value);
          }
        }

        @Override
        public long update(byte[] array, int idx, long value, LongBinaryOperator operator) {
          switch (idx & 0x3) {
            case 0:
              return update7(array, idx, 0, 0, value, operator);
            case 1:
              return update8(array, idx, 6, 6, value, operator);
            case 2:
              return update8(array, idx, 13, 4, value, operator);
            default:
              return update8(array, idx, 19, 10, value, operator);
          }
        }
      };

  private static final PackedArrayHandler HANDLER_55 =
      new AbstractPackedArrayHandlerPeriod8(55) {

        @Override
        public long get(byte[] array, int idx) {
          switch (idx & 0x7) {
            case 0:
              return get7(array, idx, 0, 0);
            case 1:
              return get8(array, idx, 6, 7);
            case 2:
              return get8(array, idx, 13, 6);
            case 3:
              return get8(array, idx, 20, 5);
            case 4:
              return get8(array, idx, 27, 4);
            case 5:
              return get8(array, idx, 34, 3);
            case 6:
              return get8(array, idx, 41, 2);
            default:
              return get8(array, idx, 47, 9);
          }
        }

        @Override
        public long set(byte[] array, int idx, long value) {
          switch (idx & 0x7) {
            case 0:
              return set7(array, idx, 0, 0, value);
            case 1:
              return set8(array, idx, 6, 7, value);
            case 2:
              return set8(array, idx, 13, 6, value);
            case 3:
              return set8(array, idx, 20, 5, value);
            case 4:
              return set8(array, idx, 27, 4, value);
            case 5:
              return set8(array, idx, 34, 3, value);
            case 6:
              return set8(array, idx, 41, 2, value);
            default:
              return set8(array, idx, 47, 9, value);
          }
        }

        @Override
        public long update(byte[] array, int idx, long value, LongBinaryOperator operator) {
          switch (idx & 0x7) {
            case 0:
              return update7(array, idx, 0, 0, value, operator);
            case 1:
              return update8(array, idx, 6, 7, value, operator);
            case 2:
              return update8(array, idx, 13, 6, value, operator);
            case 3:
              return update8(array, idx, 20, 5, value, operator);
            case 4:
              return update8(array, idx, 27, 4, value, operator);
            case 5:
              return update8(array, idx, 34, 3, value, operator);
            case 6:
              return update8(array, idx, 41, 2, value, operator);
            default:
              return update8(array, idx, 47, 9, value, operator);
          }
        }
      };

  private static final PackedArrayHandler HANDLER_56 =
      new AbstractPackedArrayHandlerPeriod1(56) {

        @Override
        public long get(byte[] array, int idx) {
          return get7(array, idx, 0, 0);
        }

        @Override
        public long set(byte[] array, int idx, long value) {
          return set7(array, idx, 0, 0, value);
        }

        @Override
        public long update(byte[] array, int idx, long value, LongBinaryOperator operator) {
          return update7(array, idx, 0, 0, value, operator);
        }
      };

  private static final PackedArrayHandler HANDLER_57 =
      new AbstractPackedArrayHandlerPeriod8(57) {

        @Override
        public long get(byte[] array, int idx) {
          int k = idx & 0x7;
          return get8(array, idx, 7 * k, k);
        }

        @Override
        public long set(byte[] array, int idx, long value) {
          int k = idx & 0x7;
          return set8(array, idx, 7 * k, k, value);
        }

        @Override
        public long update(byte[] array, int idx, long value, LongBinaryOperator operator) {
          int k = idx & 0x7;
          return update8(array, idx, 7 * k, k, value, operator);
        }
      };

  private static final PackedArrayHandler HANDLER_58 =
      new AbstractPackedArrayHandlerPeriod4(58) {

        @Override
        public long get(byte[] array, int idx) {
          int k = idx & 0x3;
          return get8(array, idx, 7 * k, k << 1);
        }

        @Override
        public long set(byte[] array, int idx, long value) {
          int k = idx & 0x3;
          return set8(array, idx, 7 * k, k << 1, value);
        }

        @Override
        public long update(byte[] array, int idx, long value, LongBinaryOperator operator) {
          int k = idx & 0x3;
          return update8(array, idx, 7 * k, k << 1, value, operator);
        }
      };

  private static final PackedArrayHandler HANDLER_59 =
      new AbstractPackedArrayHandlerPeriod8(59) {

        @Override
        public long get(byte[] array, int idx) {
          switch (idx & 0x7) {
            case 0:
              return get8(array, idx, 0, 0);
            case 1:
              return get8(array, idx, 7, 3);
            case 2:
              return get9(array, idx, 14, 6);
            case 3:
              return get8(array, idx, 22, 1);
            case 4:
              return get8(array, idx, 29, 4);
            case 5:
              return get9(array, idx, 36, 7);
            case 6:
              return get8(array, idx, 44, 2);
            default:
              return get8(array, idx, 51, 5);
          }
        }

        @Override
        public long set(byte[] array, int idx, long value) {
          switch (idx & 0x7) {
            case 0:
              return set8(array, idx, 0, 0, value);
            case 1:
              return set8(array, idx, 7, 3, value);
            case 2:
              return set9(array, idx, 14, 6, value);
            case 3:
              return set8(array, idx, 22, 1, value);
            case 4:
              return set8(array, idx, 29, 4, value);
            case 5:
              return set9(array, idx, 36, 7, value);
            case 6:
              return set8(array, idx, 44, 2, value);
            default:
              return set8(array, idx, 51, 5, value);
          }
        }

        @Override
        public long update(byte[] array, int idx, long value, LongBinaryOperator operator) {
          switch (idx & 0x7) {
            case 0:
              return update8(array, idx, 0, 0, value, operator);
            case 1:
              return update8(array, idx, 7, 3, value, operator);
            case 2:
              return update9(array, idx, 14, 6, value, operator);
            case 3:
              return update8(array, idx, 22, 1, value, operator);
            case 4:
              return update8(array, idx, 29, 4, value, operator);
            case 5:
              return update9(array, idx, 36, 7, value, operator);
            case 6:
              return update8(array, idx, 44, 2, value, operator);
            default:
              return update8(array, idx, 51, 5, value, operator);
          }
        }
      };

  private static final PackedArrayHandler HANDLER_60 =
      new AbstractPackedArrayHandlerPeriod2(60) {

        @Override
        public long get(byte[] array, int idx) {
          int k = idx & 0x1;
          return get8(array, idx, 7 * k, k << 2);
        }

        @Override
        public long set(byte[] array, int idx, long value) {
          int k = idx & 0x1;
          return set8(array, idx, 7 * k, k << 2, value);
        }

        @Override
        public long update(byte[] array, int idx, long value, LongBinaryOperator operator) {
          int k = idx & 0x1;
          return update8(array, idx, 7 * k, k << 2, value, operator);
        }
      };

  private static final PackedArrayHandler HANDLER_61 =
      new AbstractPackedArrayHandlerPeriod8(61) {

        @Override
        public long get(byte[] array, int idx) {
          switch (idx & 0x7) {
            case 0:
              return get8(array, idx, 0, 0);
            case 1:
              return get9(array, idx, 7, 5);
            case 2:
              return get8(array, idx, 15, 2);
            case 3:
              return get9(array, idx, 22, 7);
            case 4:
              return get9(array, idx, 30, 4);
            case 5:
              return get8(array, idx, 38, 1);
            case 6:
              return get9(array, idx, 45, 6);
            default:
              return get8(array, idx, 53, 3);
          }
        }

        @Override
        public long set(byte[] array, int idx, long value) {
          switch (idx & 0x7) {
            case 0:
              return set8(array, idx, 0, 0, value);
            case 1:
              return set9(array, idx, 7, 5, value);
            case 2:
              return set8(array, idx, 15, 2, value);
            case 3:
              return set9(array, idx, 22, 7, value);
            case 4:
              return set9(array, idx, 30, 4, value);
            case 5:
              return set8(array, idx, 38, 1, value);
            case 6:
              return set9(array, idx, 45, 6, value);
            default:
              return set8(array, idx, 53, 3, value);
          }
        }

        @Override
        public long update(byte[] array, int idx, long value, LongBinaryOperator operator) {
          switch (idx & 0x7) {
            case 0:
              return update8(array, idx, 0, 0, value, operator);
            case 1:
              return update9(array, idx, 7, 5, value, operator);
            case 2:
              return update8(array, idx, 15, 2, value, operator);
            case 3:
              return update9(array, idx, 22, 7, value, operator);
            case 4:
              return update9(array, idx, 30, 4, value, operator);
            case 5:
              return update8(array, idx, 38, 1, value, operator);
            case 6:
              return update9(array, idx, 45, 6, value, operator);
            default:
              return update8(array, idx, 53, 3, value, operator);
          }
        }
      };

  private static final PackedArrayHandler HANDLER_62 =
      new AbstractPackedArrayHandlerPeriod4(62) {

        @Override
        public long get(byte[] array, int idx) {
          switch (idx & 0x3) {
            case 0:
              return get8(array, idx, 0, 0);
            case 1:
              return get9(array, idx, 7, 6);
            case 2:
              return get9(array, idx, 15, 4);
            default:
              return get8(array, idx, 23, 2);
          }
        }

        @Override
        public long set(byte[] array, int idx, long value) {
          switch (idx & 0x3) {
            case 0:
              return set8(array, idx, 0, 0, value);
            case 1:
              return set9(array, idx, 7, 6, value);
            case 2:
              return set9(array, idx, 15, 4, value);
            default:
              return set8(array, idx, 23, 2, value);
          }
        }

        @Override
        public long update(byte[] array, int idx, long value, LongBinaryOperator operator) {
          switch (idx & 0x3) {
            case 0:
              return update8(array, idx, 0, 0, value, operator);
            case 1:
              return update9(array, idx, 7, 6, value, operator);
            case 2:
              return update9(array, idx, 15, 4, value, operator);
            default:
              return update8(array, idx, 23, 2, value, operator);
          }
        }
      };

  private static final PackedArrayHandler HANDLER_63 =
      new AbstractPackedArrayHandlerPeriod8(63) {

        @Override
        public long get(byte[] array, int idx) {
          switch (idx & 0x7) {
            case 0:
              return get8(array, idx, 0, 0);
            case 1:
              return get9(array, idx, 7, 7);
            case 2:
              return get9(array, idx, 15, 6);
            case 3:
              return get9(array, idx, 23, 5);
            case 4:
              return get9(array, idx, 31, 4);
            case 5:
              return get9(array, idx, 39, 3);
            case 6:
              return get9(array, idx, 47, 2);
            default:
              return get8(array, idx, 55, 1);
          }
        }

        @Override
        public long set(byte[] array, int idx, long value) {
          switch (idx & 0x7) {
            case 0:
              return set8(array, idx, 0, 0, value);
            case 1:
              return set9(array, idx, 7, 7, value);
            case 2:
              return set9(array, idx, 15, 6, value);
            case 3:
              return set9(array, idx, 23, 5, value);
            case 4:
              return set9(array, idx, 31, 4, value);
            case 5:
              return set9(array, idx, 39, 3, value);
            case 6:
              return set9(array, idx, 47, 2, value);
            default:
              return set8(array, idx, 55, 1, value);
          }
        }

        @Override
        public long update(byte[] array, int idx, long value, LongBinaryOperator operator) {
          switch (idx & 0x7) {
            case 0:
              return update8(array, idx, 0, 0, value, operator);
            case 1:
              return update9(array, idx, 7, 7, value, operator);
            case 2:
              return update9(array, idx, 15, 6, value, operator);
            case 3:
              return update9(array, idx, 23, 5, value, operator);
            case 4:
              return update9(array, idx, 31, 4, value, operator);
            case 5:
              return update9(array, idx, 39, 3, value, operator);
            case 6:
              return update9(array, idx, 47, 2, value, operator);
            default:
              return update8(array, idx, 55, 1, value, operator);
          }
        }
      };

  private static final PackedArrayHandler HANDLER_64 =
      new AbstractPackedArrayHandlerPeriod1(64) {

        @Override
        public long get(byte[] array, int idx) {
          return get8(array, idx, 0, 0);
        }

        @Override
        public long set(byte[] array, int idx, long value) {
          return set8(array, idx, 0, 0, value);
        }

        @Override
        public long update(byte[] array, int idx, long value, LongBinaryOperator operator) {
          return update8(array, idx, 0, 0, value, operator);
        }
      };

  private static final PackedArrayHandler[] HANDLER_INSTANCES = createHandlerInstances();

  private static PackedArrayHandler[] createHandlerInstances() {
    PackedArrayHandler[] instances = new PackedArrayHandler[65];
    instances[0] = HANDLER_0;
    instances[1] = HANDLER_1;
    instances[2] = HANDLER_2;
    instances[3] = HANDLER_3;
    instances[4] = HANDLER_4;
    instances[5] = HANDLER_5;
    instances[6] = HANDLER_6;
    instances[7] = HANDLER_7;
    instances[8] = HANDLER_8;
    instances[9] = HANDLER_9;
    instances[10] = HANDLER_10;
    instances[11] = HANDLER_11;
    instances[12] = HANDLER_12;
    instances[13] = HANDLER_13;
    instances[14] = HANDLER_14;
    instances[15] = HANDLER_15;
    instances[16] = HANDLER_16;
    instances[17] = HANDLER_17;
    instances[18] = HANDLER_18;
    instances[19] = HANDLER_19;
    instances[20] = HANDLER_20;
    instances[21] = HANDLER_21;
    instances[22] = HANDLER_22;
    instances[23] = HANDLER_23;
    instances[24] = HANDLER_24;
    instances[25] = HANDLER_25;
    instances[26] = HANDLER_26;
    instances[27] = HANDLER_27;
    instances[28] = HANDLER_28;
    instances[29] = HANDLER_29;
    instances[30] = HANDLER_30;
    instances[31] = HANDLER_31;
    instances[32] = HANDLER_32;
    instances[33] = HANDLER_33;
    instances[34] = HANDLER_34;
    instances[35] = HANDLER_35;
    instances[36] = HANDLER_36;
    instances[37] = HANDLER_37;
    instances[38] = HANDLER_38;
    instances[39] = HANDLER_39;
    instances[40] = HANDLER_40;
    instances[41] = HANDLER_41;
    instances[42] = HANDLER_42;
    instances[43] = HANDLER_43;
    instances[44] = HANDLER_44;
    instances[45] = HANDLER_45;
    instances[46] = HANDLER_46;
    instances[47] = HANDLER_47;
    instances[48] = HANDLER_48;
    instances[49] = HANDLER_49;
    instances[50] = HANDLER_50;
    instances[51] = HANDLER_51;
    instances[52] = HANDLER_52;
    instances[53] = HANDLER_53;
    instances[54] = HANDLER_54;
    instances[55] = HANDLER_55;
    instances[56] = HANDLER_56;
    instances[57] = HANDLER_57;
    instances[58] = HANDLER_58;
    instances[59] = HANDLER_59;
    instances[60] = HANDLER_60;
    instances[61] = HANDLER_61;
    instances[62] = HANDLER_62;
    instances[63] = HANDLER_63;
    instances[64] = HANDLER_64;
    return instances;
  }

  /**
   * Returns a packed array handler that provides methods for compactly storing fixed bit size
   * components in a byte array.
   *
   * @param bitSize the bit size
   * @return a handler
   */
  public static PackedArrayHandler getHandler(int bitSize) {
    return HANDLER_INSTANCES[bitSize];
  }
}
