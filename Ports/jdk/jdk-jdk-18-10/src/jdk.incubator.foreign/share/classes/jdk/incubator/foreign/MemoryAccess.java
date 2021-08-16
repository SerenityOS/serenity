/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package jdk.incubator.foreign;

import jdk.internal.access.foreign.MemorySegmentProxy;
import jdk.internal.vm.annotation.ForceInline;

import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;
import java.util.Objects;

/**
 * This class defines ready-made static accessors which can be used to dereference memory segments in many ways.
 * <p>
 * The most primitive accessors (see {@link #getIntAtOffset(MemorySegment, long, ByteOrder)}) take a segment, an offset
 * (expressed in bytes) and a byte order. The final address at which the dereference will occur will be computed by offsetting
 * the base address by the specified offset, as if by calling {@link MemoryAddress#addOffset(long)} on the specified base address.
 * <p>
 * In cases where no offset is required, overloads are provided (see {@link #getInt(MemorySegment, ByteOrder)}) so that
 * clients can omit the offset coordinate.
 * <p>
 * To help dereferencing in array-like use cases (e.g. where the layout of a given memory segment is a sequence
 * layout of given size an element count), higher-level overloads are also provided (see {@link #getIntAtIndex(MemorySegment, long, ByteOrder)}),
 * which take a segment and a <em>logical</em> element index. The formula to obtain the byte offset {@code O} from an
 * index {@code I} is given by {@code O = I * S} where {@code S} is the size (expressed in bytes) of the element to
 * be dereferenced.
 * <p>
 * In cases where native byte order is preferred, overloads are provided (see {@link #getIntAtOffset(MemorySegment, long)})
 * so that clients can omit the byte order parameter.
 *
 * <p> Unless otherwise specified, passing a {@code null} argument, or an array argument containing one or more {@code null}
 * elements to a method in this class causes a {@link NullPointerException NullPointerException} to be thrown. </p>
 */
public final class MemoryAccess {

    private MemoryAccess() {
        // just the one
    }

    private static final VarHandle byte_handle = MemoryHandles.varHandle(byte.class, ByteOrder.nativeOrder());
    private static final VarHandle char_LE_handle = unalignedHandle(MemoryLayouts.BITS_16_LE, char.class);
    private static final VarHandle short_LE_handle = unalignedHandle(MemoryLayouts.BITS_16_LE, short.class);
    private static final VarHandle int_LE_handle = unalignedHandle(MemoryLayouts.BITS_32_LE, int.class);
    private static final VarHandle float_LE_handle = unalignedHandle(MemoryLayouts.BITS_32_LE, float.class);
    private static final VarHandle long_LE_handle = unalignedHandle(MemoryLayouts.BITS_64_LE, long.class);
    private static final VarHandle double_LE_handle = unalignedHandle(MemoryLayouts.BITS_64_LE, double.class);
    private static final VarHandle char_BE_handle = unalignedHandle(MemoryLayouts.BITS_16_BE, char.class);
    private static final VarHandle short_BE_handle = unalignedHandle(MemoryLayouts.BITS_16_BE, short.class);
    private static final VarHandle int_BE_handle = unalignedHandle(MemoryLayouts.BITS_32_BE, int.class);
    private static final VarHandle float_BE_handle = unalignedHandle(MemoryLayouts.BITS_32_BE, float.class);
    private static final VarHandle long_BE_handle = unalignedHandle(MemoryLayouts.BITS_64_BE, long.class);
    private static final VarHandle double_BE_handle = unalignedHandle(MemoryLayouts.BITS_64_BE, double.class);
    private static final VarHandle address_handle;

    static {
        Class<?> carrier = switch ((int) MemoryLayouts.ADDRESS.byteSize()) {
            case 4 -> int.class;
            case 8 -> long.class;
            default -> throw new ExceptionInInitializerError("Unsupported pointer size: " + MemoryLayouts.ADDRESS.byteSize());
        };
        address_handle = MemoryHandles.asAddressVarHandle(unalignedHandle(MemoryLayouts.ADDRESS, carrier));
    }

    private static VarHandle unalignedHandle(ValueLayout elementLayout, Class<?> carrier) {
        return MemoryHandles.varHandle(carrier, 1, elementLayout.order());
    }

    // Note: all the accessor methods defined below take advantage of argument type profiling
    // (see src/hotspot/share/oops/methodData.cpp) which greatly enhances performance when the same accessor
    // method is used repeatedly with different segment kinds (e.g. on-heap vs. off-heap).

    /**
     * Reads a byte from given segment and offset.
     *
     * @param segment the segment to be dereferenced.
     * @param offset offset in bytes (relative to {@code segment}). The final address of this read operation can be expressed as {@code segment.address().addOffset(offset)}.
     * @return a byte value read from {@code segment}.
     */
    @ForceInline
    public static byte getByteAtOffset(MemorySegment segment, long offset) {
        Objects.requireNonNull(segment);
        return (byte)byte_handle.get(segment, offset);
    }

    /**
     * Writes a byte at given segment and offset.
     *
     * @param segment the segment to be dereferenced.
     * @param offset offset in bytes (relative to {@code segment}). The final address of this read operation can be expressed as {@code segment.address().addOffset(offset)}.
     * @param value the byte value to be written.
     */
    @ForceInline
    public static void setByteAtOffset(MemorySegment segment, long offset, byte value) {
        Objects.requireNonNull(segment);
        byte_handle.set(segment, offset, value);
    }

    /**
     * Reads a char from given segment and offset, with byte order set to {@link ByteOrder#nativeOrder()}.
     * <p>
     * This is equivalent to the following code:
     * <blockquote><pre>{@code
    getCharAtOffset(segment, offset, ByteOrder.nativeOrder());
     * }</pre></blockquote>
     * @param segment the segment to be dereferenced.
     * @param offset offset in bytes (relative to {@code segment}). The final address of this read operation can be expressed as {@code segment.address().addOffset(offset)}.
     * @return a char value read from {@code segment}.
     */
    @ForceInline
    public static char getCharAtOffset(MemorySegment segment, long offset) {
        return getCharAtOffset(segment, offset, ByteOrder.nativeOrder());
    }

    /**
     * Writes a char at given segment and offset, with byte order set to {@link ByteOrder#nativeOrder()}.
     * <p>
     * This is equivalent to the following code:
     * <blockquote><pre>{@code
    setCharAtOffset(segment, offset, ByteOrder.nativeOrder(), value);
     * }</pre></blockquote>
     * @param segment the segment to be dereferenced.
     * @param offset offset in bytes (relative to {@code segment}). The final address of this read operation can be expressed as {@code segment.address().addOffset(offset)}.
     * @param value the char value to be written.
     */
    @ForceInline
    public static void setCharAtOffset(MemorySegment segment, long offset, char value) {
        setCharAtOffset(segment, offset, ByteOrder.nativeOrder(), value);
    }

    /**
     * Reads a short from given segment and offset, with byte order set to {@link ByteOrder#nativeOrder()}.
     * <p>
     * This is equivalent to the following code:
     * <blockquote><pre>{@code
    getShortAtOffset(segment, offset, ByteOrder.nativeOrder());
     * }</pre></blockquote>
     * @param segment the segment to be dereferenced.
     * @param offset offset in bytes (relative to {@code segment}). The final address of this read operation can be expressed as {@code segment.address().addOffset(offset)}.
     * @return a short value read from {@code segment}.
     */
    @ForceInline
    public static short getShortAtOffset(MemorySegment segment, long offset) {
        return getShortAtOffset(segment, offset, ByteOrder.nativeOrder());
    }

    /**
     * Writes a short at given segment and offset, with byte order set to {@link ByteOrder#nativeOrder()}.
     * <p>
     * This is equivalent to the following code:
     * <blockquote><pre>{@code
    setShortAtOffset(segment, offset, ByteOrder.nativeOrder(), value);
     * }</pre></blockquote>
     * @param segment the segment to be dereferenced.
     * @param offset offset in bytes (relative to {@code segment}). The final address of this read operation can be expressed as {@code segment.address().addOffset(offset)}.
     * @param value the short value to be written.
     */
    @ForceInline
    public static void setShortAtOffset(MemorySegment segment, long offset, short value) {
        setShortAtOffset(segment, offset, ByteOrder.nativeOrder(), value);
    }

    /**
     * Reads an int from given segment and offset, with byte order set to {@link ByteOrder#nativeOrder()}.
     * <p>
     * This is equivalent to the following code:
     * <blockquote><pre>{@code
    getIntAtOffset(segment, offset, ByteOrder.nativeOrder());
     * }</pre></blockquote>
     * @param segment the segment to be dereferenced.
     * @param offset offset in bytes (relative to {@code segment}). The final address of this read operation can be expressed as {@code segment.address().addOffset(offset)}.
     * @return an int value read from {@code segment}.
     */
    @ForceInline
    public static int getIntAtOffset(MemorySegment segment, long offset) {
        return getIntAtOffset(segment, offset, ByteOrder.nativeOrder());
    }

    /**
     * Writes an int at given segment and offset, with byte order set to {@link ByteOrder#nativeOrder()}.
     * <p>
     * This is equivalent to the following code:
     * <blockquote><pre>{@code
    setIntAtOffset(segment, offset, ByteOrder.nativeOrder(), value);
     * }</pre></blockquote>
     * @param segment the segment to be dereferenced.
     * @param offset offset in bytes (relative to {@code segment}). The final address of this read operation can be expressed as {@code segment.address().addOffset(offset)}.
     * @param value the int value to be written.
     */
    @ForceInline
    public static void setIntAtOffset(MemorySegment segment, long offset, int value) {
        setIntAtOffset(segment, offset, ByteOrder.nativeOrder(), value);
    }

    /**
     * Reads a float from given segment and offset, with byte order set to {@link ByteOrder#nativeOrder()}.
     * <p>
     * This is equivalent to the following code:
     * <blockquote><pre>{@code
    getFloatAtOffset(segment, offset, ByteOrder.nativeOrder());
     * }</pre></blockquote>
     * @param segment the segment to be dereferenced.
     * @param offset offset in bytes (relative to {@code segment}). The final address of this read operation can be expressed as {@code segment.address().addOffset(offset)}.
     * @return a float value read from {@code segment}.
     */
    @ForceInline
    public static float getFloatAtOffset(MemorySegment segment, long offset) {
        return getFloatAtOffset(segment, offset, ByteOrder.nativeOrder());
    }

    /**
     * Writes a float at given segment and offset, with byte order set to {@link ByteOrder#nativeOrder()}.
     * <p>
     * This is equivalent to the following code:
     * <blockquote><pre>{@code
    setFloatAtOffset(segment, offset, ByteOrder.nativeOrder(), value);
     * }</pre></blockquote>
     * @param segment the segment to be dereferenced.
     * @param offset offset in bytes (relative to {@code segment}). The final address of this read operation can be expressed as {@code segment.address().addOffset(offset)}.
     * @param value the float value to be written.
     */
    @ForceInline
    public static void setFloatAtOffset(MemorySegment segment, long offset, float value) {
        setFloatAtOffset(segment, offset, ByteOrder.nativeOrder(), value);
    }

    /**
     * Reads a long from given segment and offset, with byte order set to {@link ByteOrder#nativeOrder()}.
     * <p>
     * This is equivalent to the following code:
     * <blockquote><pre>{@code
    getLongAtOffset(segment, offset, ByteOrder.nativeOrder());
     * }</pre></blockquote>
     * @param segment the segment to be dereferenced.
     * @param offset offset in bytes (relative to {@code segment}). The final address of this read operation can be expressed as {@code segment.address().addOffset(offset)}.
     * @return a long value read from {@code segment}.
     */
    @ForceInline
    public static long getLongAtOffset(MemorySegment segment, long offset) {
        return getLongAtOffset(segment, offset, ByteOrder.nativeOrder());
    }

    /**
     * Writes a long at given segment and offset, with byte order set to {@link ByteOrder#nativeOrder()}.
     * <p>
     * This is equivalent to the following code:
     * <blockquote><pre>{@code
    setLongAtOffset(segment, offset, ByteOrder.nativeOrder(), value);
     * }</pre></blockquote>
     * @param segment the segment to be dereferenced.
     * @param offset offset in bytes (relative to {@code segment}). The final address of this read operation can be expressed as {@code segment.address().addOffset(offset)}.
     * @param value the long value to be written.
     */
    @ForceInline
    public static void setLongAtOffset(MemorySegment segment, long offset, long value) {
        setLongAtOffset(segment, offset, ByteOrder.nativeOrder(), value);
    }

    /**
     * Reads a double from given segment and offset, with byte order set to {@link ByteOrder#nativeOrder()}.
     * <p>
     * This is equivalent to the following code:
     * <blockquote><pre>{@code
    getDoubleAtOffset(segment, offset, ByteOrder.nativeOrder());
     * }</pre></blockquote>
     * @param segment the segment to be dereferenced.
     * @param offset offset in bytes (relative to {@code segment}). The final address of this read operation can be expressed as {@code segment.address().addOffset(offset)}.
     * @return a double value read from {@code segment}.
     */
    @ForceInline
    public static double getDoubleAtOffset(MemorySegment segment, long offset) {
        return getDoubleAtOffset(segment, offset, ByteOrder.nativeOrder());
    }

    /**
     * Writes a double at given segment and offset, with byte order set to {@link ByteOrder#nativeOrder()}.
     * <p>
     * This is equivalent to the following code:
     * <blockquote><pre>{@code
    setDoubleAtOffset(segment, offset, ByteOrder.nativeOrder(), value);
     * }</pre></blockquote>
     * @param segment the segment to be dereferenced.
     * @param offset offset in bytes (relative to {@code segment}). The final address of this read operation can be expressed as {@code segment.address().addOffset(offset)}.
     * @param value the double value to be written.
     */
    @ForceInline
    public static void setDoubleAtOffset(MemorySegment segment, long offset, double value) {
        setDoubleAtOffset(segment, offset, ByteOrder.nativeOrder(), value);
    }

    /**
     * Reads a memory address from given segment and offset, with byte order set to {@link ByteOrder#nativeOrder()}.
     * <p>
     * This is equivalent (e.g. on a 64-bit platform) to the following code:
     * <blockquote><pre>{@code
    VarHandle handle = MemoryHandles.asAddressHandle(MemoryHandles.varHandle(long.class, ByteOrder.nativeOrder()));
    MemoryAddress value = (MemoryAddress)handle.get(segment, offset);
     * }</pre></blockquote>
     * @param segment the segment to be dereferenced.
     * @param offset offset in bytes (relative to {@code segment}). The final address of this read operation can be expressed as {@code segment.address().addOffset(offset)}.
     * @return a memory address read from {@code segment}.
     */
    @ForceInline
    public static MemoryAddress getAddressAtOffset(MemorySegment segment, long offset) {
        Objects.requireNonNull(segment);
        return (MemoryAddress)address_handle.get(segment, offset);
    }

    /**
     * Writes a memory address at given segment and offset, with byte order set to {@link ByteOrder#nativeOrder()}.
     * <p>
     * This is equivalent (e.g. on a 64-bit platform) to the following code:
     * <blockquote><pre>{@code
    VarHandle handle = MemoryHandles.asAddressHandle(MemoryHandles.varHandle(long.class, ByteOrder.nativeOrder()));
    handle.set(segment, offset, value.address());
     * }</pre></blockquote>
     * @param segment the segment to be dereferenced.
     * @param offset offset in bytes (relative to {@code segment}). The final address of this read operation can be expressed as {@code segment.address().addOffset(offset)}.
     * @param value the memory address to be written (expressed as an {@link Addressable} instance).
     */
    @ForceInline
    public static void setAddressAtOffset(MemorySegment segment, long offset, Addressable value) {
        Objects.requireNonNull(segment);
        Objects.requireNonNull(value);
        address_handle.set(segment, offset, value.address());
    }

    /**
     * Reads a char from given segment and offset with given byte order.
     * <p>
     * This is equivalent to the following code:
     * <blockquote><pre>{@code
    VarHandle handle = MemoryHandles.varHandle(char.class, 1, order);
    char value = (char)handle.get(segment, offset);
     * }</pre></blockquote>
     * @param segment the segment to be dereferenced.
     * @param offset offset in bytes (relative to {@code segment}). The final address of this read operation can be expressed as {@code segment.address().addOffset(offset)}.
     * @param order the specified byte order.
     * @return a char value read from {@code segment}.
     */
    @ForceInline
    public static char getCharAtOffset(MemorySegment segment, long offset, ByteOrder order) {
        Objects.requireNonNull(segment);
        Objects.requireNonNull(order);
        return (char)((order == ByteOrder.BIG_ENDIAN) ? char_BE_handle : char_LE_handle).get(segment, offset);
    }

    /**
     * Writes a char at given segment and offset with given byte order.
     * <p>
     * This is equivalent to the following code:
     * <blockquote><pre>{@code
    VarHandle handle = MemoryHandles.varHandle(char.class, 1, order);
    handle.set(segment, offset, value);
     * }</pre></blockquote>
     * @param segment the segment to be dereferenced.
     * @param offset offset in bytes (relative to {@code segment}). The final address of this read operation can be expressed as {@code segment.address().addOffset(offset)}.
     * @param order the specified byte order.
     * @param value the char value to be written.
     */
    @ForceInline
    public static void setCharAtOffset(MemorySegment segment, long offset, ByteOrder order, char value) {
        Objects.requireNonNull(segment);
        Objects.requireNonNull(order);
        ((order == ByteOrder.BIG_ENDIAN) ? char_BE_handle : char_LE_handle).set(segment, offset, value);
    }

    /**
     * Reads a short from given segment and offset with given byte order.
     * <p>
     * This is equivalent to the following code:
     * <blockquote><pre>{@code
    VarHandle handle = MemoryHandles.varHandle(short.class, 1, order);
    short value = (short)handle.get(segment, offset);
     * }</pre></blockquote>
     * @param segment the segment to be dereferenced.
     * @param offset offset in bytes (relative to {@code segment}). The final address of this read operation can be expressed as {@code segment.address().addOffset(offset)}.
     * @param order the specified byte order.
     * @return a short value read from {@code segment}.
     */
    @ForceInline
    public static short getShortAtOffset(MemorySegment segment, long offset, ByteOrder order) {
        Objects.requireNonNull(segment);
        Objects.requireNonNull(order);
        return (short)((order == ByteOrder.BIG_ENDIAN) ? short_BE_handle : short_LE_handle).get(segment, offset);
    }

    /**
     * Writes a short at given segment and offset with given byte order.
     * <p>
     * This is equivalent to the following code:
     * <blockquote><pre>{@code
    VarHandle handle = MemoryHandles.varHandle(short.class, 1, order);
    handle.set(segment, offset, value);
     * }</pre></blockquote>
     * @param segment the segment to be dereferenced.
     * @param offset offset in bytes (relative to {@code segment}). The final address of this read operation can be expressed as {@code segment.address().addOffset(offset)}.
     * @param order the specified byte order.
     * @param value the short value to be written.
     */
    @ForceInline
    public static void setShortAtOffset(MemorySegment segment, long offset, ByteOrder order, short value) {
        Objects.requireNonNull(segment);
        Objects.requireNonNull(order);
        ((order == ByteOrder.BIG_ENDIAN) ? short_BE_handle : short_LE_handle).set(segment, offset, value);
    }

    /**
     * Reads an int from given segment and offset with given byte order.
     * <p>
     * This is equivalent to the following code:
     * <blockquote><pre>{@code
    VarHandle handle = MemoryHandles.varHandle(int.class, 1, order);
    int value = (int)handle.get(segment, offset);
     * }</pre></blockquote>
     * @param segment the segment to be dereferenced.
     * @param offset offset in bytes (relative to {@code segment}). The final address of this read operation can be expressed as {@code segment.address().addOffset(offset)}.
     * @param order the specified byte order.
     * @return an int value read from {@code segment}.
     */
    @ForceInline
    public static int getIntAtOffset(MemorySegment segment, long offset, ByteOrder order) {
        Objects.requireNonNull(segment);
        Objects.requireNonNull(order);
        return (int)((order == ByteOrder.BIG_ENDIAN) ? int_BE_handle : int_LE_handle).get(segment, offset);
    }

    /**
     * Writes an int at given segment and offset with given byte order.
     * <p>
     * This is equivalent to the following code:
     * <blockquote><pre>{@code
    VarHandle handle = MemoryHandles.varHandle(int.class, 1, order);
    handle.set(segment, offset, value);
     * }</pre></blockquote>
     * @param segment the segment to be dereferenced.
     * @param offset offset in bytes (relative to {@code segment}). The final address of this read operation can be expressed as {@code segment.address().addOffset(offset)}.
     * @param order the specified byte order.
     * @param value the int value to be written.
     */
    @ForceInline
    public static void setIntAtOffset(MemorySegment segment, long offset, ByteOrder order, int value) {
        Objects.requireNonNull(segment);
        Objects.requireNonNull(order);
        ((order == ByteOrder.BIG_ENDIAN) ? int_BE_handle : int_LE_handle).set(segment, offset, value);
    }

    /**
     * Reads a float from given segment and offset with given byte order.
     * <p>
     * This is equivalent to the following code:
     * <blockquote><pre>{@code
    VarHandle handle = MemoryHandles.varHandle(float.class, 1, order);
    float value = (float)handle.get(segment, offset);
     * }</pre></blockquote>
     * @param segment the segment to be dereferenced.
     * @param offset offset in bytes (relative to {@code segment}). The final address of this read operation can be expressed as {@code segment.address().addOffset(offset)}.
     * @param order the specified byte order.
     * @return a float value read from {@code segment}.
     */
    @ForceInline
    public static float getFloatAtOffset(MemorySegment segment, long offset, ByteOrder order) {
        Objects.requireNonNull(segment);
        Objects.requireNonNull(order);
        return (float)((order == ByteOrder.BIG_ENDIAN) ? float_BE_handle : float_LE_handle).get(segment, offset);
    }

    /**
     * Writes a float at given segment and offset with given byte order.
     * <p>
     * This is equivalent to the following code:
     * <blockquote><pre>{@code
    VarHandle handle = MemoryHandles.varHandle(float.class, 1, order);
    handle.set(segment, offset, value);
     * }</pre></blockquote>
     * @param segment the segment to be dereferenced.
     * @param offset offset in bytes (relative to {@code segment}). The final address of this read operation can be expressed as {@code segment.address().addOffset(offset)}.
     * @param order the specified byte order.
     * @param value the float value to be written.
     */
    @ForceInline
    public static void setFloatAtOffset(MemorySegment segment, long offset, ByteOrder order, float value) {
        Objects.requireNonNull(segment);
        Objects.requireNonNull(order);
        ((order == ByteOrder.BIG_ENDIAN) ? float_BE_handle : float_LE_handle).set(segment, offset, value);
    }

    /**
     * Reads a long from given segment and offset with given byte order.
     * <p>
     * This is equivalent to the following code:
     * <blockquote><pre>{@code
    VarHandle handle = MemoryHandles.varHandle(long.class, 1, order);
    long value = (long)handle.get(segment, offset);
     * }</pre></blockquote>
     * @param segment the segment to be dereferenced.
     * @param offset offset in bytes (relative to {@code segment}). The final address of this read operation can be expressed as {@code segment.address().addOffset(offset)}.
     * @param order the specified byte order.
     * @return a long value read from {@code segment}.
     */
    @ForceInline
    public static long getLongAtOffset(MemorySegment segment, long offset, ByteOrder order) {
        Objects.requireNonNull(segment);
        Objects.requireNonNull(order);
        return (long)((order == ByteOrder.BIG_ENDIAN) ? long_BE_handle : long_LE_handle).get(segment, offset);
    }

    /**
     * Writes a long at given segment and offset with given byte order.
     * <p>
     * This is equivalent to the following code:
     * <blockquote><pre>{@code
    VarHandle handle = MemoryHandles.varHandle(long.class, 1, order);
    handle.set(segment, offset, value);
     * }</pre></blockquote>
     * @param segment the segment to be dereferenced.
     * @param offset offset in bytes (relative to {@code segment}). The final address of this read operation can be expressed as {@code segment.address().addOffset(offset)}.
     * @param order the specified byte order.
     * @param value the long value to be written.
     */
    @ForceInline
    public static void setLongAtOffset(MemorySegment segment, long offset, ByteOrder order, long value) {
        Objects.requireNonNull(segment);
        Objects.requireNonNull(order);
        ((order == ByteOrder.BIG_ENDIAN) ? long_BE_handle : long_LE_handle).set(segment, offset, value);
    }

    /**
     * Reads a double from given segment and offset with given byte order.
     * <p>
     * This is equivalent to the following code:
     * <blockquote><pre>{@code
    VarHandle handle = MemoryHandles.varHandle(double.class, 1, order);
    double value = (double)handle.get(segment, offset);
     * }</pre></blockquote>
     * @param segment the segment to be dereferenced.
     * @param offset offset in bytes (relative to {@code segment}). The final address of this read operation can be expressed as {@code segment.address().addOffset(offset)}.
     * @param order the specified byte order.
     * @return a double value read from {@code segment}.
     */
    @ForceInline
    public static double getDoubleAtOffset(MemorySegment segment, long offset, ByteOrder order) {
        Objects.requireNonNull(segment);
        Objects.requireNonNull(order);
        return (double)((order == ByteOrder.BIG_ENDIAN) ? double_BE_handle : double_LE_handle).get(segment, offset);
    }

    /**
     * Writes a double at given segment and offset with given byte order.
     * <p>
     * This is equivalent to the following code:
     * <blockquote><pre>{@code
    VarHandle handle = MemoryHandles.varHandle(double.class, 1, order);
    handle.set(segment, offset, value);
     * }</pre></blockquote>
     * @param segment the segment to be dereferenced.
     * @param offset offset in bytes (relative to {@code segment}). The final address of this read operation can be expressed as {@code segment.address().addOffset(offset)}.
     * @param order the specified byte order.
     * @param value the double value to be written.
     */
    @ForceInline
    public static void setDoubleAtOffset(MemorySegment segment, long offset, ByteOrder order, double value) {
        Objects.requireNonNull(segment);
        Objects.requireNonNull(order);
        ((order == ByteOrder.BIG_ENDIAN) ? double_BE_handle : double_LE_handle).set(segment, offset, value);
    }

    /**
     * Reads a byte from given segment.
     * <p>
     * This is equivalent to the following code:
     * <blockquote><pre>{@code
    byte value = getByteAtOffset(segment, 0L);
     * }</pre></blockquote>
     *
     * @param segment the segment to be dereferenced.
     * @return a byte value read from {@code segment}.
     */
    @ForceInline
    public static byte getByte(MemorySegment segment) {
        return getByteAtOffset(segment, 0L);
    }

    /**
     * Writes a byte at given segment.
     * <p>
     * This is equivalent to the following code:
     * <blockquote><pre>{@code
    setByteAtOffset(segment, 0L, value);
     * }</pre></blockquote>
     * @param segment the segment to be dereferenced.
     * @param value the byte value to be written.
     */
    @ForceInline
    public static void setByte(MemorySegment segment, byte value) {
        setByteAtOffset(segment, 0L, value);
    }

    /**
     * Reads a char from given segment, with byte order set to {@link ByteOrder#nativeOrder()}.
     * <p>
     * This is equivalent to the following code:
     * <blockquote><pre>{@code
    char value = getCharAtOffset(segment, 0L);
     * }</pre></blockquote>
     * @param segment the segment to be dereferenced.
     * @return a char value read from {@code segment}.
     */
    @ForceInline
    public static char getChar(MemorySegment segment) {
        return getCharAtOffset(segment, 0L);
    }

    /**
     * Writes a char at given segment, with byte order set to {@link ByteOrder#nativeOrder()}.
     * <p>
     * This is equivalent to the following code:
     * <blockquote><pre>{@code
    setCharAtOffset(segment, 0L, value);
     * }</pre></blockquote>
     * @param segment the segment to be dereferenced.
     * @param value the char value to be written.
     */
    @ForceInline
    public static void setChar(MemorySegment segment, char value) {
        setCharAtOffset(segment, 0L, value);
    }

    /**
     * Reads a short from given segment, with byte order set to {@link ByteOrder#nativeOrder()}.
     * <p>
     * This is equivalent to the following code:
     * <blockquote><pre>{@code
    short value = getShortAtOffset(segment, 0L);
     * }</pre></blockquote>
     * @param segment the segment to be dereferenced.
     * @return a short value read from {@code segment}.
     */
    @ForceInline
    public static short getShort(MemorySegment segment) {
        return getShortAtOffset(segment, 0L);
    }

    /**
     * Writes a short at given segment, with byte order set to {@link ByteOrder#nativeOrder()}.
     * <p>
     * This is equivalent to the following code:
     * <blockquote><pre>{@code
    setShortAtOffset(segment, 0L, value);
     * }</pre></blockquote>
     * @param segment the segment to be dereferenced.
     * @param value the short value to be written.
     */
    @ForceInline
    public static void setShort(MemorySegment segment, short value) {
        setShortAtOffset(segment, 0L, value);
    }

    /**
     * Reads an int from given segment, with byte order set to {@link ByteOrder#nativeOrder()}.
     * <p>
     * This is equivalent to the following code:
     * <blockquote><pre>{@code
    int value = getIntAtOffset(segment, 0L);
     * }</pre></blockquote>
     * @param segment the segment to be dereferenced.
     * @return an int value read from {@code segment}.
     */
    @ForceInline
    public static int getInt(MemorySegment segment) {
        return getIntAtOffset(segment, 0L);
    }

    /**
     * Writes an int at given segment, with byte order set to {@link ByteOrder#nativeOrder()}.
     * <p>
     * This is equivalent to the following code:
     * <blockquote><pre>{@code
    setIntAtOffset(segment, 0L, value);
     * }</pre></blockquote>
     * @param segment the segment to be dereferenced.
     * @param value the int value to be written.
     */
    @ForceInline
    public static void setInt(MemorySegment segment, int value) {
        setIntAtOffset(segment, 0L, value);
    }

    /**
     * Reads a float from given segment, with byte order set to {@link ByteOrder#nativeOrder()}.
     * <p>
     * This is equivalent to the following code:
     * <blockquote><pre>{@code
    float value = getFloatAtOffset(segment, 0L);
     * }</pre></blockquote>
     * @param segment the segment to be dereferenced.
     * @return a float value read from {@code segment}.
     */
    @ForceInline
    public static float getFloat(MemorySegment segment) {
        return getFloatAtOffset(segment, 0L);
    }

    /**
     * Writes a float at given segment, with byte order set to {@link ByteOrder#nativeOrder()}.
     * <p>
     * This is equivalent to the following code:
     * <blockquote><pre>{@code
    setFloatAtOffset(segment, 0L, value);
     * }</pre></blockquote>
     * @param segment the segment to be dereferenced.
     * @param value the float value to be written.
     */
    @ForceInline
    public static void setFloat(MemorySegment segment, float value) {
        setFloatAtOffset(segment, 0L, value);
    }

    /**
     * Reads a long from given segment, with byte order set to {@link ByteOrder#nativeOrder()}.
     * <p>
     * This is equivalent to the following code:
     * <blockquote><pre>{@code
    long value = getLongAtOffset(segment, 0L);
     * }</pre></blockquote>
     * @param segment the segment to be dereferenced.
     * @return a long value read from {@code segment}.
     */
    @ForceInline
    public static long getLong(MemorySegment segment) {
        return getLongAtOffset(segment, 0L);
    }

    /**
     * Writes a long at given segment, with byte order set to {@link ByteOrder#nativeOrder()}.
     * <p>
     * This is equivalent to the following code:
     * <blockquote><pre>{@code
    setLongAtOffset(segment, 0L, value);
     * }</pre></blockquote>
     * @param segment the segment to be dereferenced.
     * @param value the long value to be written.
     */
    @ForceInline
    public static void setLong(MemorySegment segment, long value) {
        setLongAtOffset(segment, 0L, value);
    }

    /**
     * Reads a double from given segment, with byte order set to {@link ByteOrder#nativeOrder()}.
     * <p>
     * This is equivalent to the following code:
     * <blockquote><pre>{@code
    double value = getDoubleAtOffset(segment, 0L);
     * }</pre></blockquote>
     * @param segment the segment to be dereferenced.
     * @return a double value read from {@code segment}.
     */
    @ForceInline
    public static double getDouble(MemorySegment segment) {
        return getDoubleAtOffset(segment, 0L);
    }

    /**
     * Writes a double at given segment, with byte order set to {@link ByteOrder#nativeOrder()}.
     * <p>
     * This is equivalent to the following code:
     * <blockquote><pre>{@code
    setDoubleAtOffset(segment, 0L, value);
     * }</pre></blockquote>
     * @param segment the segment to be dereferenced.
     * @param value the double value to be written.
     */
    @ForceInline
    public static void setDouble(MemorySegment segment, double value) {
        setDoubleAtOffset(segment, 0L, value);
    }

    /**
     * Reads a memory address from given segment, with byte order set to {@link ByteOrder#nativeOrder()}.
     * <p>
     * This is equivalent to the following code:
     * <blockquote><pre>{@code
    MemoryAddress value = getAddressAtOffset(segment, 0L);
     * }</pre></blockquote>
     * @param segment the segment to be dereferenced.
     * @return a memory address read from {@code segment}.
     */
    @ForceInline
    public static MemoryAddress getAddress(MemorySegment segment) {
        return getAddressAtOffset(segment, 0L);
    }

    /**
     * Writes a memory address at given segment, with byte order set to {@link ByteOrder#nativeOrder()}.
     * <p>
     * This is equivalent to the following code:
     * <blockquote><pre>{@code
    setAddressAtOffset(segment, 0L, value);
     * }</pre></blockquote>
     * @param segment the segment to be dereferenced.
     * @param value the memory address to be written (expressed as an {@link Addressable} instance).
     */
    @ForceInline
    public static void setAddress(MemorySegment segment, Addressable value) {
        setAddressAtOffset(segment, 0L, value);
    }

    /**
     * Reads a char from given segment, with given byte order.
     * <p>
     * This is equivalent to the following code:
     * <blockquote><pre>{@code
    char value = getCharAtOffset(segment, 0L, order);
     * }</pre></blockquote>
     * @param segment the segment to be dereferenced.
     * @param order the specified byte order.
     * @return a char value read from {@code segment}.
     */
    @ForceInline
    public static char getChar(MemorySegment segment, ByteOrder order) {
        return getCharAtOffset(segment, 0L, order);
    }

    /**
     * Writes a char at given segment, with given byte order.
     * <p>
     * This is equivalent to the following code:
     * <blockquote><pre>{@code
    setCharAtOffset(segment, 0L, order, value);
     * }</pre></blockquote>
     * @param segment the segment to be dereferenced.
     * @param order the specified byte order.
     * @param value the char value to be written.
     */
    @ForceInline
    public static void setChar(MemorySegment segment, ByteOrder order, char value) {
        setCharAtOffset(segment, 0L, order, value);
    }

    /**
     * Reads a short from given segment, with given byte order.
     * <p>
     * This is equivalent to the following code:
     * <blockquote><pre>{@code
    short value = getShortAtOffset(segment, 0L, order);
     * }</pre></blockquote>
     * @param segment the segment to be dereferenced.
     * @param order the specified byte order.
     * @return a short value read from {@code segment}.
     */
    @ForceInline
    public static short getShort(MemorySegment segment, ByteOrder order) {
        return getShortAtOffset(segment, 0L, order);
    }

    /**
     * Writes a short at given segment, with given byte order.
     * <p>
     * This is equivalent to the following code:
     * <blockquote><pre>{@code
    setShortAtOffset(segment, 0L, order, value);
     * }</pre></blockquote>
     * @param segment the segment to be dereferenced.
     * @param order the specified byte order.
     * @param value the short value to be written.
     */
    @ForceInline
    public static void setShort(MemorySegment segment, ByteOrder order, short value) {
        setShortAtOffset(segment, 0L, order, value);
    }

    /**
     * Reads an int from given segment, with given byte order.
     * <p>
     * This is equivalent to the following code:
     * <blockquote><pre>{@code
    int value = getIntAtOffset(segment, 0L, order);
     * }</pre></blockquote>
     * @param segment the segment to be dereferenced.
     * @param order the specified byte order.
     * @return an int value read from {@code segment}.
     */
    @ForceInline
    public static int getInt(MemorySegment segment, ByteOrder order) {
        return getIntAtOffset(segment, 0L, order);
    }

    /**
     * Writes an int at given segment, with given byte order.
     * <p>
     * This is equivalent to the following code:
     * <blockquote><pre>{@code
    setIntAtOffset(segment, 0L, order, value);
     * }</pre></blockquote>
     * @param segment the segment to be dereferenced.
     * @param order the specified byte order.
     * @param value the int value to be written.
     */
    @ForceInline
    public static void setInt(MemorySegment segment, ByteOrder order, int value) {
        setIntAtOffset(segment, 0L, order, value);
    }

    /**
     * Reads a float from given segment, with given byte order.
     * <p>
     * This is equivalent to the following code:
     * <blockquote><pre>{@code
    float value = getFloatAtOffset(segment, 0L, order);
     * }</pre></blockquote>
     * @param segment the segment to be dereferenced.
     * @param order the specified byte order.
     * @return a float value read from {@code segment}.
     */
    @ForceInline
    public static float getFloat(MemorySegment segment, ByteOrder order) {
        return getFloatAtOffset(segment, 0L, order);
    }

    /**
     * Writes a float at given segment, with given byte order.
     * <p>
     * This is equivalent to the following code:
     * <blockquote><pre>{@code
    setFloatAtOffset(segment, 0L, order, value);
     * }</pre></blockquote>
     * @param segment the segment to be dereferenced.
     * @param order the specified byte order.
     * @param value the float value to be written.
     */
    @ForceInline
    public static void setFloat(MemorySegment segment, ByteOrder order, float value) {
        setFloatAtOffset(segment, 0L, order, value);
    }

    /**
     * Reads a long from given segment, with given byte order.
     * <p>
     * This is equivalent to the following code:
     * <blockquote><pre>{@code
    long value = getLongAtOffset(segment, 0L, order);
     * }</pre></blockquote>
     * @param segment the segment to be dereferenced.
     * @param order the specified byte order.
     * @return a long value read from {@code segment}.
     */
    @ForceInline
    public static long getLong(MemorySegment segment, ByteOrder order) {
        return getLongAtOffset(segment, 0L, order);
    }

    /**
     * Writes a long at given segment, with given byte order.
     * <p>
     * This is equivalent to the following code:
     * <blockquote><pre>{@code
    setLongAtOffset(segment, 0L, order, value);
     * }</pre></blockquote>
     * @param segment the segment to be dereferenced.
     * @param order the specified byte order.
     * @param value the long value to be written.
     */
    @ForceInline
    public static void setLong(MemorySegment segment, ByteOrder order, long value) {
        setLongAtOffset(segment, 0L, order, value);
    }

    /**
     * Reads a double from given segment, with given byte order.
     * <p>
     * This is equivalent to the following code:
     * <blockquote><pre>{@code
    double value = getDoubleAtOffset(segment, 0L, order);
     * }</pre></blockquote>
     * @param segment the segment to be dereferenced.
     * @param order the specified byte order.
     * @return a double value read from {@code segment}.
     */
    @ForceInline
    public static double getDouble(MemorySegment segment, ByteOrder order) {
        return getDoubleAtOffset(segment, 0L, order);
    }

    /**
     * Writes a double at given segment, with given byte order.
     * <p>
     * This is equivalent to the following code:
     * <blockquote><pre>{@code
    setDoubleAtOffset(segment, 0L, order, value);
     * }</pre></blockquote>
     * @param segment the segment to be dereferenced.
     * @param order the specified byte order.
     * @param value the double value to be written.
     */
    @ForceInline
    public static void setDouble(MemorySegment segment, ByteOrder order, double value) {
        setDoubleAtOffset(segment, 0L, order, value);
    }

    /**
     * Reads a char from given segment and element index, with byte order set to {@link ByteOrder#nativeOrder()}.
     * <p>
     * This is equivalent to the following code:
     * <blockquote><pre>{@code
    char value = getCharAtOffset(segment, 2 * index);
     * }</pre></blockquote>
     * @param segment the segment to be dereferenced.
     * @param index element index (relative to {@code segment}). The final address of this read operation can be expressed as {@code segment.address().addOffset(index * 2)}.
     * @return a char value read from {@code segment} at the element index specified by {@code index}.
     */
    @ForceInline
    public static char getCharAtIndex(MemorySegment segment, long index) {
        return getCharAtOffset(segment, scale(segment, index, 2));
    }

    /**
     * Writes a char at given segment and element index, with byte order set to {@link ByteOrder#nativeOrder()}.
     * <p>
     * This is equivalent to the following code:
     * <blockquote><pre>{@code
    setCharAtOffset(segment, 2 * index, value);
     * }</pre></blockquote>
     * @param segment the segment to be dereferenced.
     * @param index element index (relative to {@code segment}). The final address of this read operation can be expressed as {@code segment.address().addOffset(index * 2)}.
     * @param value the char value to be written.
     */
    @ForceInline
    public static void setCharAtIndex(MemorySegment segment, long index, char value) {
        setCharAtOffset(segment, scale(segment, index, 2), value);
    }

    /**
     * Reads a short from given segment and element index, with byte order set to {@link ByteOrder#nativeOrder()}.
     * <p>
     * This is equivalent to the following code:
     * <blockquote><pre>{@code
    short value = getShortAtOffset(segment, 2 * index);
     * }</pre></blockquote>
     * @param segment the segment to be dereferenced.
     * @param index element index (relative to {@code segment}). The final address of this read operation can be expressed as {@code segment.address().addOffset(index * 2)}.
     * @return a short value read from {@code segment} at the element index specified by {@code index}.
     */
    @ForceInline
    public static short getShortAtIndex(MemorySegment segment, long index) {
        return getShortAtOffset(segment, scale(segment, index, 2));
    }

    /**
     * Writes a short at given segment and element index, with byte order set to {@link ByteOrder#nativeOrder()}.
     * <p>
     * This is equivalent to the following code:
     * <blockquote><pre>{@code
    setShortAtOffset(segment, 2 * index, value);
     * }</pre></blockquote>
     * @param segment the segment to be dereferenced.
     * @param index element index (relative to {@code segment}). The final address of this read operation can be expressed as {@code segment.address().addOffset(index * 2)}.
     * @param value the short value to be written.
     */
    @ForceInline
    public static void setShortAtIndex(MemorySegment segment, long index, short value) {
        setShortAtOffset(segment, scale(segment, index, 2), value);
    }

    /**
     * Reads an int from given segment and element index, with byte order set to {@link ByteOrder#nativeOrder()}.
     * <p>
     * This is equivalent to the following code:
     * <blockquote><pre>{@code
    int value = getIntAtOffset(segment, 4 * index);
     * }</pre></blockquote>
     * @param segment the segment to be dereferenced.
     * @param index element index (relative to {@code segment}). The final address of this read operation can be expressed as {@code segment.address().addOffset(index * 4)}.
     * @return an int value read from {@code segment} at the element index specified by {@code index}.
     */
    @ForceInline
    public static int getIntAtIndex(MemorySegment segment, long index) {
        return getIntAtOffset(segment, scale(segment, index, 4));
    }

    /**
     * Writes an int at given segment and element index, with byte order set to {@link ByteOrder#nativeOrder()}.
     * <p>
     * This is equivalent to the following code:
     * <blockquote><pre>{@code
    setIntAtOffset(segment, 4 * index, value);
     * }</pre></blockquote>
     * @param segment the segment to be dereferenced.
     * @param index element index (relative to {@code segment}). The final address of this read operation can be expressed as {@code segment.address().addOffset(index * 4)}.
     * @param value the int value to be written.
     */
    @ForceInline
    public static void setIntAtIndex(MemorySegment segment, long index, int value) {
        setIntAtOffset(segment, scale(segment, index, 4), value);
    }

    /**
     * Reads a float from given segment and element index, with byte order set to {@link ByteOrder#nativeOrder()}.
     * <p>
     * This is equivalent to the following code:
     * <blockquote><pre>{@code
    float value = getFloatAtOffset(segment, 4 * index);
     * }</pre></blockquote>
     * @param segment the segment to be dereferenced.
     * @param index element index (relative to {@code segment}). The final address of this read operation can be expressed as {@code segment.address().addOffset(index * 4)}.
     * @return a float value read from {@code segment} at the element index specified by {@code index}.
     */
    @ForceInline
    public static float getFloatAtIndex(MemorySegment segment, long index) {
        return getFloatAtOffset(segment, scale(segment, index, 4));
    }

    /**
     * Writes a float at given segment and element index, with byte order set to {@link ByteOrder#nativeOrder()}.
     * <p>
     * This is equivalent to the following code:
     * <blockquote><pre>{@code
    setFloatAtOffset(segment, 4 * index, value);
     * }</pre></blockquote>
     * @param segment the segment to be dereferenced.
     * @param index element index (relative to {@code segment}). The final address of this read operation can be expressed as {@code segment.address().addOffset(index * 4)}.
     * @param value the float value to be written.
     */
    @ForceInline
    public static void setFloatAtIndex(MemorySegment segment, long index, float value) {
        setFloatAtOffset(segment, scale(segment, index, 4), value);
    }

    /**
     * Reads a long from given segment and element index, with byte order set to {@link ByteOrder#nativeOrder()}.
     * <p>
     * This is equivalent to the following code:
     * <blockquote><pre>{@code
    return getLongAtOffset(segment, 8 * index);
     * }</pre></blockquote>
     * @param segment the segment to be dereferenced.
     * @param index element index (relative to {@code segment}). The final address of this read operation can be expressed as {@code segment.address().addOffset(index * 8)}.
     * @return a long value read from {@code segment} at the element index specified by {@code index}.
     */
    @ForceInline
    public static long getLongAtIndex(MemorySegment segment, long index) {
        return getLongAtOffset(segment, scale(segment, index, 8));
    }

    /**
     * Writes a long at given segment and element index, with byte order set to {@link ByteOrder#nativeOrder()}.
     * <p>
     * This is equivalent to the following code:
     * <blockquote><pre>{@code
    setLongAtOffset(segment, 8 * index, value);
     * }</pre></blockquote>
     * @param segment the segment to be dereferenced.
     * @param index element index (relative to {@code segment}). The final address of this read operation can be expressed as {@code segment.address().addOffset(index * 8)}.
     * @param value the long value to be written.
     */
    @ForceInline
    public static void setLongAtIndex(MemorySegment segment, long index, long value) {
        setLongAtOffset(segment, scale(segment, index, 8), value);
    }

    /**
     * Reads a double from given segment and element index, with byte order set to {@link ByteOrder#nativeOrder()}.
     * <p>
     * This is equivalent to the following code:
     * <blockquote><pre>{@code
    return getDoubleAtOffset(segment, 8 * index);
     * }</pre></blockquote>
     * @param segment the segment to be dereferenced.
     * @param index element index (relative to {@code segment}). The final address of this read operation can be expressed as {@code segment.address().addOffset(index * 8)}.
     * @return a double value read from {@code segment} at the element index specified by {@code index}.
     */
    @ForceInline
    public static double getDoubleAtIndex(MemorySegment segment, long index) {
        return getDoubleAtOffset(segment, scale(segment, index, 8));
    }

    /**
     * Writes a double at given segment and element index, with byte order set to {@link ByteOrder#nativeOrder()}.
     * <p>
     * This is equivalent to the following code:
     * <blockquote><pre>{@code
    setDoubleAtOffset(segment, 8 * index, value);
     * }</pre></blockquote>
     * @param segment the segment to be dereferenced.
     * @param index element index (relative to {@code segment}). The final address of this read operation can be expressed as {@code segment.address().addOffset(index * 8)}.
     * @param value the double value to be written.
     */
    @ForceInline
    public static void setDoubleAtIndex(MemorySegment segment, long index, double value) {
        setDoubleAtOffset(segment, scale(segment, index, 8), value);
    }

    /**
     * Reads a memory address from given segment and element index, with byte order set to {@link ByteOrder#nativeOrder()}.
     * <p>
     * This is equivalent to the following code:
     * <blockquote><pre>{@code
    return getAddressAtOffset(segment, index * MemoryLayouts.ADDRESS.byteSize());
     * }</pre></blockquote>
     * @param segment the segment to be dereferenced.
     * @param index element index (relative to {@code segment}). The final address of this read operation can be expressed as {@code segment.address().addOffset(index * 8)}.
     * @return a memory address read from {@code segment} at the element index specified by {@code index}.
     */
    @ForceInline
    public static MemoryAddress getAddressAtIndex(MemorySegment segment, long index) {
        return getAddressAtOffset(segment, scale(segment, index, (int)MemoryLayouts.ADDRESS.byteSize()));
    }

    /**
     * Writes a memory address at given segment and element index, with byte order set to {@link ByteOrder#nativeOrder()}.
     * <p>
     * This is equivalent to the following code:
     * <blockquote><pre>{@code
    setAddressAtOffset(segment, index * MemoryLayouts.ADDRESS.byteSize(), value);
     * }</pre></blockquote>
     * @param segment the segment to be dereferenced.
     * @param index element index (relative to {@code segment}). The final address of this read operation can be expressed as {@code segment.address().addOffset(index * 8)}.
     * @param value the memory address to be written (expressed as an {@link Addressable} instance).
     */
    @ForceInline
    public static void setAddressAtIndex(MemorySegment segment, long index, Addressable value) {
        setAddressAtOffset(segment, scale(segment, index, (int)MemoryLayouts.ADDRESS.byteSize()), value);
    }

    /**
     * Reads a char from given segment and element index, with given byte order.
     * <p>
     * This is equivalent to the following code:
     * <blockquote><pre>{@code
    char value = getCharAtOffset(segment, 2 * index, order);
     * }</pre></blockquote>
     * @param segment the segment to be dereferenced.
     * @param index element index (relative to {@code segment}). The final address of this read operation can be expressed as {@code segment.address().addOffset(index * 2)}.
     * @param order the specified byte order.
     * @return a char value read from {@code segment} at the element index specified by {@code index}.
     */
    @ForceInline
    public static char getCharAtIndex(MemorySegment segment, long index, ByteOrder order) {
        return getCharAtOffset(segment, scale(segment, index, 2), order);
    }

    /**
     * Writes a char at given segment and element index, with given byte order.
     * <p>
     * This is equivalent to the following code:
     * <blockquote><pre>{@code
    setCharAtOffset(segment, 2 * index, order, value);
     * }</pre></blockquote>
     * @param segment the segment to be dereferenced.
     * @param index element index (relative to {@code segment}). The final address of this read operation can be expressed as {@code segment.address().addOffset(index * 2)}.
     * @param order the specified byte order.
     * @param value the char value to be written.
     */
    @ForceInline
    public static void setCharAtIndex(MemorySegment segment, long index, ByteOrder order, char value) {
        setCharAtOffset(segment, scale(segment, index, 2), order, value);
    }

    /**
     * Reads a short from given segment and element index, with given byte order.
     * <p>
     * This is equivalent to the following code:
     * <blockquote><pre>{@code
    short value = getShortAtOffset(segment, 2 * index, order);
     * }</pre></blockquote>
     * @param segment the segment to be dereferenced.
     * @param index element index (relative to {@code segment}). The final address of this read operation can be expressed as {@code segment.address().addOffset(index * 2)}.
     * @param order the specified byte order.
     * @return a short value read from {@code segment} at the element index specified by {@code index}.
     */
    @ForceInline
    public static short getShortAtIndex(MemorySegment segment, long index, ByteOrder order) {
        return getShortAtOffset(segment, scale(segment, index, 2), order);
    }

    /**
     * Writes a short at given segment and element index, with given byte order.
     * <p>
     * This is equivalent to the following code:
     * <blockquote><pre>{@code
    setShortAtOffset(segment, 2 * index, order, value);
     * }</pre></blockquote>
     * @param segment the segment to be dereferenced.
     * @param index element index (relative to {@code segment}). The final address of this read operation can be expressed as {@code segment.address().addOffset(index * 2)}.
     * @param order the specified byte order.
     * @param value the short value to be written.
     */
    @ForceInline
    public static void setShortAtIndex(MemorySegment segment, long index, ByteOrder order, short value) {
        setShortAtOffset(segment, scale(segment, index, 2), order, value);
    }

    /**
     * Reads an int from given segment and element index, with given byte order.
     * <p>
     * This is equivalent to the following code:
     * <blockquote><pre>{@code
    int value = getIntAtOffset(segment, 4 * index, order);
     * }</pre></blockquote>
     * @param segment the segment to be dereferenced.
     * @param index element index (relative to {@code segment}). The final address of this read operation can be expressed as {@code segment.address().addOffset(index * 4)}.
     * @param order the specified byte order.
     * @return an int value read from {@code segment} at the element index specified by {@code index}.
     */
    @ForceInline
    public static int getIntAtIndex(MemorySegment segment, long index, ByteOrder order) {
        return getIntAtOffset(segment, scale(segment, index, 4), order);
    }

    /**
     * Writes an int at given segment and element index, with given byte order.
     * <p>
     * This is equivalent to the following code:
     * <blockquote><pre>{@code
    setIntAtOffset(segment, 4 * index, order, value);
     * }</pre></blockquote>
     * @param segment the segment to be dereferenced.
     * @param index element index (relative to {@code segment}). The final address of this read operation can be expressed as {@code segment.address().addOffset(index * 4)}.
     * @param order the specified byte order.
     * @param value the int value to be written.
     */
    @ForceInline
    public static void setIntAtIndex(MemorySegment segment, long index, ByteOrder order, int value) {
        setIntAtOffset(segment, scale(segment, index, 4), order, value);
    }

    /**
     * Reads a float from given segment and element index, with given byte order.
     * <p>
     * This is equivalent to the following code:
     * <blockquote><pre>{@code
    float value = getFloatAtOffset(segment, 4 * index, order);
     * }</pre></blockquote>
     * @param segment the segment to be dereferenced.
     * @param index element index (relative to {@code segment}). The final address of this read operation can be expressed as {@code segment.address().addOffset(index * 4)}.
     * @param order the specified byte order.
     * @return a float value read from {@code segment} at the element index specified by {@code index}.
     */
    @ForceInline
    public static float getFloatAtIndex(MemorySegment segment, long index, ByteOrder order) {
        return getFloatAtOffset(segment, scale(segment, index, 4), order);
    }

    /**
     * Writes a float at given segment and element index, with given byte order.
     * <p>
     * This is equivalent to the following code:
     * <blockquote><pre>{@code
    setFloatAtOffset(segment, 4 * index, order, value);
     * }</pre></blockquote>
     * @param segment the segment to be dereferenced.
     * @param index element index (relative to {@code segment}). The final address of this read operation can be expressed as {@code segment.address().addOffset(index * 4)}.
     * @param order the specified byte order.
     * @param value the float value to be written.
     */
    @ForceInline
    public static void setFloatAtIndex(MemorySegment segment, long index, ByteOrder order, float value) {
        setFloatAtOffset(segment, scale(segment, index, 4), order, value);
    }

    /**
     * Reads a long from given segment and element index, with given byte order.
     * <p>
     * This is equivalent to the following code:
     * <blockquote><pre>{@code
    return getLongAtOffset(segment, 8 * index, order);
     * }</pre></blockquote>
     * @param segment the segment to be dereferenced.
     * @param index element index (relative to {@code segment}). The final address of this read operation can be expressed as {@code segment.address().addOffset(index * 8)}.
     * @param order the specified byte order.
     * @return a long value read from {@code segment} at the element index specified by {@code index}.
     */
    @ForceInline
    public static long getLongAtIndex(MemorySegment segment, long index, ByteOrder order) {
        return getLongAtOffset(segment, scale(segment, index, 8), order);
    }

    /**
     * Writes a long at given segment and element index, with given byte order.
     * <p>
     * This is equivalent to the following code:
     * <blockquote><pre>{@code
    setLongAtOffset(segment, 8 * index, order, value);
     * }</pre></blockquote>
     * @param segment the segment to be dereferenced.
     * @param index element index (relative to {@code segment}). The final address of this read operation can be expressed as {@code segment.address().addOffset(index * 8)}.
     * @param order the specified byte order.
     * @param value the long value to be written.
     */
    @ForceInline
    public static void setLongAtIndex(MemorySegment segment, long index, ByteOrder order, long value) {
        setLongAtOffset(segment, scale(segment, index, 8), order, value);
    }

    /**
     * Reads a double from given segment and element index, with given byte order.
     * <p>
     * This is equivalent to the following code:
     * <blockquote><pre>{@code
    return getDoubleAtOffset(segment, 8 * index, order);
     * }</pre></blockquote>
     * @param segment the segment to be dereferenced.
     * @param index element index (relative to {@code segment}). The final address of this read operation can be expressed as {@code segment.address().addOffset(index * 8)}.
     * @param order the specified byte order.
     * @return a double value read from {@code segment} at the element index specified by {@code index}.
     */
    @ForceInline
    public static double getDoubleAtIndex(MemorySegment segment, long index, ByteOrder order) {
        return getDoubleAtOffset(segment, scale(segment, index, 8), order);
    }

    /**
     * Writes a double at given segment and element index, with given byte order.
     * <p>
     * This is equivalent to the following code:
     * <blockquote><pre>{@code
    setDoubleAtOffset(segment, 8 * index, order, value);
     * }</pre></blockquote>
     * @param segment the segment to be dereferenced.
     * @param index element index (relative to {@code segment}). The final address of this read operation can be expressed as {@code segment.address().addOffset(index * 8)}.
     * @param order the specified byte order.
     * @param value the double value to be written.
     */
    @ForceInline
    public static void setDoubleAtIndex(MemorySegment segment, long index, ByteOrder order, double value) {
        setDoubleAtOffset(segment, scale(segment, index, 8), order, value);
    }

    @ForceInline
    private static long scale(MemorySegment address, long index, int size) {
        return MemorySegmentProxy.multiplyOffsets(index, size, (MemorySegmentProxy)address);
    }
}
