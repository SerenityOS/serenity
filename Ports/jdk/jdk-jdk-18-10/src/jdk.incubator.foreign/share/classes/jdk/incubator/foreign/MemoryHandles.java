/*
 *  Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *  This code is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License version 2 only, as
 *  published by the Free Software Foundation.  Oracle designates this
 *  particular file as subject to the "Classpath" exception as provided
 *  by Oracle in the LICENSE file that accompanied this code.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  version 2 for more details (a copy is included in the LICENSE file that
 *  accompanied this code).
 *
 *  You should have received a copy of the GNU General Public License version
 *  2 along with this work; if not, write to the Free Software Foundation,
 *  Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 *   Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 *  or visit www.oracle.com if you need additional information or have any
 *  questions.
 *
 */
package jdk.incubator.foreign;

import jdk.internal.access.JavaLangInvokeAccess;
import jdk.internal.access.SharedSecrets;
import jdk.internal.foreign.Utils;
import sun.invoke.util.Wrapper;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;
import java.util.List;
import java.util.Objects;

/**
 * This class defines several factory methods for constructing and combining memory access var handles.
 * To obtain a memory access var handle, clients must start from one of the <em>leaf</em> methods
 * (see {@link MemoryHandles#varHandle(Class, ByteOrder)},
 * {@link MemoryHandles#varHandle(Class, long, ByteOrder)}). This determines the variable type
 * (all primitive types but {@code void} and {@code boolean} are supported), as well as the alignment constraint and the
 * byte order associated with a memory access var handle. The resulting memory access var handle can then be combined in various ways
 * to emulate different addressing modes. The var handles created by this class feature a <em>mandatory</em> coordinate type
 * (of type {@link MemorySegment}), and one {@code long} coordinate type, which represents the offset, in bytes, relative
 * to the segment, at which dereference should occur.
 * <p>
 * As an example, consider the memory layout expressed by a {@link GroupLayout} instance constructed as follows:
 * <blockquote><pre>{@code
GroupLayout seq = MemoryLayout.structLayout(
        MemoryLayout.paddingLayout(32),
        MemoryLayout.valueLayout(32, ByteOrder.BIG_ENDIAN).withName("value")
);
 * }</pre></blockquote>
 * To access the member layout named {@code value}, we can construct a memory access var handle as follows:
 * <blockquote><pre>{@code
VarHandle handle = MemoryHandles.varHandle(int.class, ByteOrder.BIG_ENDIAN); //(MemorySegment, long) -> int
handle = MemoryHandles.insertCoordinates(handle, 1, 4); //(MemorySegment) -> int
 * }</pre></blockquote>
 *
 * <p> Unless otherwise specified, passing a {@code null} argument, or an array argument containing one or more {@code null}
 * elements to a method in this class causes a {@link NullPointerException NullPointerException} to be thrown. </p>
 *
 * <h2><a id="memaccess-mode"></a>Alignment and access modes</h2>
 *
 * A memory access var handle is associated with an access size {@code S} and an alignment constraint {@code B}
 * (both expressed in bytes). We say that a memory access operation is <em>fully aligned</em> if it occurs
 * at a memory address {@code A} which is compatible with both alignment constraints {@code S} and {@code B}.
 * If access is fully aligned then following access modes are supported and are
 * guaranteed to support atomic access:
 * <ul>
 * <li>read write access modes for all {@code T}, with the exception of
 *     access modes {@code get} and {@code set} for {@code long} and
 *     {@code double} on 32-bit platforms.
 * <li>atomic update access modes for {@code int}, {@code long},
 *     {@code float} or {@code double}.
 *     (Future major platform releases of the JDK may support additional
 *     types for certain currently unsupported access modes.)
 * <li>numeric atomic update access modes for {@code int} and {@code long}.
 *     (Future major platform releases of the JDK may support additional
 *     numeric types for certain currently unsupported access modes.)
 * <li>bitwise atomic update access modes for {@code int} and {@code long}.
 *     (Future major platform releases of the JDK may support additional
 *     numeric types for certain currently unsupported access modes.)
 * </ul>
 *
 * If {@code T} is {@code float} or {@code double} then atomic
 * update access modes compare values using their bitwise representation
 * (see {@link Float#floatToRawIntBits} and
 * {@link Double#doubleToRawLongBits}, respectively).
 * <p>
 * Alternatively, a memory access operation is <em>partially aligned</em> if it occurs at a memory address {@code A}
 * which is only compatible with the alignment constraint {@code B}; in such cases, access for anything other than the
 * {@code get} and {@code set} access modes will result in an {@code IllegalStateException}. If access is partially aligned,
 * atomic access is only guaranteed with respect to the largest power of two that divides the GCD of {@code A} and {@code S}.
 * <p>
 * Finally, in all other cases, we say that a memory access operation is <em>misaligned</em>; in such cases an
 * {@code IllegalStateException} is thrown, irrespective of the access mode being used.
 */
public final class MemoryHandles {

    private static final JavaLangInvokeAccess JLI = SharedSecrets.getJavaLangInvokeAccess();

    private MemoryHandles() {
        //sorry, just the one!
    }

    private static final MethodHandle LONG_TO_ADDRESS;
    private static final MethodHandle ADDRESS_TO_LONG;
    private static final MethodHandle INT_TO_BYTE;
    private static final MethodHandle BYTE_TO_UNSIGNED_INT;
    private static final MethodHandle INT_TO_SHORT;
    private static final MethodHandle SHORT_TO_UNSIGNED_INT;
    private static final MethodHandle LONG_TO_BYTE;
    private static final MethodHandle BYTE_TO_UNSIGNED_LONG;
    private static final MethodHandle LONG_TO_SHORT;
    private static final MethodHandle SHORT_TO_UNSIGNED_LONG;
    private static final MethodHandle LONG_TO_INT;
    private static final MethodHandle INT_TO_UNSIGNED_LONG;

    static {
        try {
            LONG_TO_ADDRESS = MethodHandles.lookup().findStatic(MemoryHandles.class, "longToAddress",
                    MethodType.methodType(MemoryAddress.class, long.class));
            ADDRESS_TO_LONG = MethodHandles.lookup().findStatic(MemoryHandles.class, "addressToLong",
                    MethodType.methodType(long.class, MemoryAddress.class));
            INT_TO_BYTE = MethodHandles.explicitCastArguments(MethodHandles.identity(byte.class),
                    MethodType.methodType(byte.class, int.class));
            BYTE_TO_UNSIGNED_INT = MethodHandles.lookup().findStatic(Byte.class, "toUnsignedInt",
                    MethodType.methodType(int.class, byte.class));
            INT_TO_SHORT = MethodHandles.explicitCastArguments(MethodHandles.identity(short.class),
                    MethodType.methodType(short.class, int.class));
            SHORT_TO_UNSIGNED_INT = MethodHandles.lookup().findStatic(Short.class, "toUnsignedInt",
                    MethodType.methodType(int.class, short.class));
            LONG_TO_BYTE = MethodHandles.explicitCastArguments(MethodHandles.identity(byte.class),
                    MethodType.methodType(byte.class, long.class));
            BYTE_TO_UNSIGNED_LONG = MethodHandles.lookup().findStatic(Byte.class, "toUnsignedLong",
                    MethodType.methodType(long.class, byte.class));
            LONG_TO_SHORT = MethodHandles.explicitCastArguments(MethodHandles.identity(short.class),
                    MethodType.methodType(short.class, long.class));
            SHORT_TO_UNSIGNED_LONG = MethodHandles.lookup().findStatic(Short.class, "toUnsignedLong",
                    MethodType.methodType(long.class, short.class));
            LONG_TO_INT = MethodHandles.explicitCastArguments(MethodHandles.identity(int.class),
                    MethodType.methodType(int.class, long.class));
            INT_TO_UNSIGNED_LONG = MethodHandles.lookup().findStatic(Integer.class, "toUnsignedLong",
                    MethodType.methodType(long.class, int.class));
        } catch (Throwable ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }

    /**
     * Creates a memory access var handle with the given carrier type and byte order.
     *
     * The returned var handle's type is {@code carrier} and the list of coordinate types is
     * {@code (MemorySegment, long)}, where the {@code long} coordinate type corresponds to byte offset into
     * a given memory segment. The returned var handle accesses bytes at an offset in a given
     * memory segment, composing bytes to or from a value of the type {@code carrier} according to the given endianness;
     * the alignment constraint (in bytes) for the resulting memory access var handle is the same as the size (in bytes) of the
     * carrier type {@code carrier}.
     *
     * @apiNote the resulting var handle features certain <a href="#memaccess-mode">access mode restrictions</a>,
     * which are common to all memory access var handles.
     *
     * @param carrier the carrier type. Valid carriers are {@code byte}, {@code short}, {@code char}, {@code int},
     * {@code float}, {@code long}, and {@code double}.
     * @param byteOrder the required byte order.
     * @return the new memory access var handle.
     * @throws IllegalArgumentException when an illegal carrier type is used
     */
    public static VarHandle varHandle(Class<?> carrier, ByteOrder byteOrder) {
        Objects.requireNonNull(carrier);
        Objects.requireNonNull(byteOrder);
        return varHandle(carrier,
                carrierSize(carrier),
                byteOrder);
    }

    /**
     * Creates a memory access var handle with the given carrier type, alignment constraint, and byte order.
     *
     * The returned var handle's type is {@code carrier} and the list of coordinate types is
     * {@code (MemorySegment, long)}, where the {@code long} coordinate type corresponds to byte offset into
     * a given memory segment. The returned var handle accesses bytes at an offset in a given
     * memory segment, composing bytes to or from a value of the type {@code carrier} according to the given endianness;
     * the alignment constraint (in bytes) for the resulting memory access var handle is given by {@code alignmentBytes}.
     *
     * @apiNote the resulting var handle features certain <a href="#memaccess-mode">access mode restrictions</a>,
     * which are common to all memory access var handles.
     *
     * @param carrier the carrier type. Valid carriers are {@code byte}, {@code short}, {@code char}, {@code int},
     * {@code float}, {@code long}, and {@code double}.
     * @param alignmentBytes the alignment constraint (in bytes). Must be a power of two.
     * @param byteOrder the required byte order.
     * @return the new memory access var handle.
     * @throws IllegalArgumentException if an illegal carrier type is used, or if {@code alignmentBytes} is not a power of two.
     */
    public static VarHandle varHandle(Class<?> carrier, long alignmentBytes, ByteOrder byteOrder) {
        Objects.requireNonNull(carrier);
        Objects.requireNonNull(byteOrder);
        checkCarrier(carrier);

        if (alignmentBytes <= 0
                || (alignmentBytes & (alignmentBytes - 1)) != 0) { // is power of 2?
            throw new IllegalArgumentException("Bad alignment: " + alignmentBytes);
        }

        return Utils.fixUpVarHandle(JLI.memoryAccessVarHandle(carrier, false, alignmentBytes - 1, byteOrder));
    }

    /**
     * Adapt an existing var handle into a new var handle whose carrier type is {@link MemorySegment}.
     * That is, when calling {@link VarHandle#get(Object...)} on the returned var handle,
     * the read numeric value will be turned into a memory address (as if by calling {@link MemoryAddress#ofLong(long)});
     * similarly, when calling {@link VarHandle#set(Object...)}, the memory address to be set will be converted
     * into a numeric value, and then written into memory. The amount of bytes read (resp. written) from (resp. to)
     * memory depends on the carrier of the original memory access var handle.
     *
     * @param target the memory access var handle to be adapted
     * @return the adapted var handle.
     * @throws IllegalArgumentException if the carrier type of {@code varHandle} is either {@code boolean},
     * {@code float}, or {@code double}, or is not a primitive type.
     */
    public static VarHandle asAddressVarHandle(VarHandle target) {
        Objects.requireNonNull(target);
        Class<?> carrier = target.varType();
        if (!carrier.isPrimitive() || carrier == boolean.class ||
                carrier == float.class || carrier == double.class) {
            throw new IllegalArgumentException("Unsupported carrier type: " + carrier.getName());
        }

        if (carrier != long.class) {
            // slow-path, we need to adapt
            return filterValue(target,
                    MethodHandles.explicitCastArguments(ADDRESS_TO_LONG, MethodType.methodType(carrier, MemoryAddress.class)),
                    MethodHandles.explicitCastArguments(LONG_TO_ADDRESS, MethodType.methodType(MemoryAddress.class, carrier)));
        } else {
            // fast-path
            return filterValue(target, ADDRESS_TO_LONG, LONG_TO_ADDRESS);
        }
    }

    /**
     * Adapts a target var handle by narrowing incoming values and widening
     * outgoing values, to and from the given type, respectively.
     * <p>
     * The returned var handle can be used to conveniently treat unsigned
     * primitive data types as if they were a wider signed primitive type. For
     * example, it is often convenient to model an <i>unsigned short</i> as a
     * Java {@code int} to avoid dealing with negative values, which would be
     * the case if modeled as a Java {@code short}. This is illustrated in the following example:
     * <blockquote><pre>{@code
    MemorySegment segment = MemorySegment.allocateNative(2, ResourceScope.newImplicitScope());
    VarHandle SHORT_VH = MemoryLayouts.JAVA_SHORT.varHandle(short.class);
    VarHandle INT_VH = MemoryHandles.asUnsigned(SHORT_VH, int.class);
    SHORT_VH.set(segment, (short)-1);
    INT_VH.get(segment); // returns 65535
     * }</pre></blockquote>
     * <p>
     * When calling e.g. {@link VarHandle#set(Object...)} on the resulting var
     * handle, the incoming value (of type {@code adaptedType}) is converted by a
     * <i>narrowing primitive conversion</i> and then passed to the {@code
     * target} var handle. A narrowing primitive conversion may lose information
     * about the overall magnitude of a numeric value. Conversely, when calling
     * e.g. {@link VarHandle#get(Object...)} on the resulting var handle, the
     * returned value obtained from the {@code target} var handle is converted
     * by a <i>unsigned widening conversion</i> before being returned to the
     * caller. In an unsigned widening conversion the high-order bits greater
     * than that of the {@code target} carrier type are zero, and the low-order
     * bits (equal to the width of the {@code target} carrier type) are equal to
     * the bits of the value obtained from the {@code target} var handle.
     * <p>
     * The returned var handle will feature the variable type {@code adaptedType},
     * and the same access coordinates, the same access modes (see {@link
     * java.lang.invoke.VarHandle.AccessMode}, and the same atomic access
     * guarantees, as those featured by the {@code target} var handle.
     *
     * @param target the memory access var handle to be adapted
     * @param adaptedType the adapted type
     * @return the adapted var handle.
     * @throws IllegalArgumentException if the carrier type of {@code target}
     * is not one of {@code byte}, {@code short}, or {@code int}; if {@code
     * adaptedType} is not one of {@code int}, or {@code long}; if the bitwidth
     * of the {@code adaptedType} is not greater than that of the {@code target}
     * carrier type.
     *
     * @jls 5.1.3 Narrowing Primitive Conversion
     */
    public static VarHandle asUnsigned(VarHandle target, final Class<?> adaptedType) {
        Objects.requireNonNull(target);
        Objects.requireNonNull(adaptedType);
        final Class<?> carrier = target.varType();
        checkWidenable(carrier);
        checkNarrowable(adaptedType);
        checkTargetWiderThanCarrier(carrier, adaptedType);

        if (adaptedType == int.class && carrier == byte.class) {
            return filterValue(target, INT_TO_BYTE, BYTE_TO_UNSIGNED_INT);
        } else if (adaptedType == int.class && carrier == short.class) {
            return filterValue(target, INT_TO_SHORT, SHORT_TO_UNSIGNED_INT);
        } else if (adaptedType == long.class && carrier == byte.class) {
            return filterValue(target, LONG_TO_BYTE, BYTE_TO_UNSIGNED_LONG);
        } else if (adaptedType == long.class && carrier == short.class) {
            return filterValue(target, LONG_TO_SHORT, SHORT_TO_UNSIGNED_LONG);
        } else if (adaptedType == long.class && carrier == int.class) {
            return filterValue(target, LONG_TO_INT, INT_TO_UNSIGNED_LONG);
        } else {
            throw new InternalError("should not reach here");
        }
    }

    /**
     * Adapts a target var handle by pre-processing incoming and outgoing values using a pair of filter functions.
     * <p>
     * When calling e.g. {@link VarHandle#set(Object...)} on the resulting var handle, the incoming value (of type {@code T}, where
     * {@code T} is the <em>last</em> parameter type of the first filter function) is processed using the first filter and then passed
     * to the target var handle.
     * Conversely, when calling e.g. {@link VarHandle#get(Object...)} on the resulting var handle, the return value obtained from
     * the target var handle (of type {@code T}, where {@code T} is the <em>last</em> parameter type of the second filter function)
     * is processed using the second filter and returned to the caller. More advanced access mode types, such as
     * {@link java.lang.invoke.VarHandle.AccessMode#COMPARE_AND_EXCHANGE} might apply both filters at the same time.
     * <p>
     * For the boxing and unboxing filters to be well formed, their types must be of the form {@code (A... , S) -> T} and
     * {@code (A... , T) -> S}, respectively, where {@code T} is the type of the target var handle. If this is the case,
     * the resulting var handle will have type {@code S} and will feature the additional coordinates {@code A...} (which
     * will be appended to the coordinates of the target var handle).
     * <p>
     * The resulting var handle will feature the same access modes (see {@link java.lang.invoke.VarHandle.AccessMode} and
     * atomic access guarantees as those featured by the target var handle.
     *
     * @param target the target var handle
     * @param filterToTarget a filter to convert some type {@code S} into the type of {@code target}
     * @param filterFromTarget a filter to convert the type of {@code target} to some type {@code S}
     * @return an adapter var handle which accepts a new type, performing the provided boxing/unboxing conversions.
     * @throws IllegalArgumentException if {@code filterFromTarget} and {@code filterToTarget} are not well-formed, that is, they have types
     * other than {@code (A... , S) -> T} and {@code (A... , T) -> S}, respectively, where {@code T} is the type of the target var handle,
     * or if either {@code filterFromTarget} or {@code filterToTarget} throws any checked exceptions.
     */
    public static VarHandle filterValue(VarHandle target, MethodHandle filterToTarget, MethodHandle filterFromTarget) {
        return JLI.filterValue(target, filterToTarget, filterFromTarget);
    }

    /**
     * Adapts a target var handle by pre-processing incoming coordinate values using unary filter functions.
     * <p>
     * When calling e.g. {@link VarHandle#get(Object...)} on the resulting var handle, the incoming coordinate values
     * starting at position {@code pos} (of type {@code C1, C2 ... Cn}, where {@code C1, C2 ... Cn} are the return type
     * of the unary filter functions) are transformed into new values (of type {@code S1, S2 ... Sn}, where {@code S1, S2 ... Sn} are the
     * parameter types of the unary filter functions), and then passed (along with any coordinate that was left unaltered
     * by the adaptation) to the target var handle.
     * <p>
     * For the coordinate filters to be well formed, their types must be of the form {@code S1 -> T1, S2 -> T1 ... Sn -> Tn},
     * where {@code T1, T2 ... Tn} are the coordinate types starting at position {@code pos} of the target var handle.
     * <p>
     * The resulting var handle will feature the same access modes (see {@link java.lang.invoke.VarHandle.AccessMode}) and
     * atomic access guarantees as those featured by the target var handle.
     *
     * @param target the target var handle
     * @param pos the position of the first coordinate to be transformed
     * @param filters the unary functions which are used to transform coordinates starting at position {@code pos}
     * @return an adapter var handle which accepts new coordinate types, applying the provided transformation
     * to the new coordinate values.
     * @throws IllegalArgumentException if the handles in {@code filters} are not well-formed, that is, they have types
     * other than {@code S1 -> T1, S2 -> T2, ... Sn -> Tn} where {@code T1, T2 ... Tn} are the coordinate types starting
     * at position {@code pos} of the target var handle, if {@code pos} is not between 0 and the target var handle coordinate arity, inclusive,
     * or if more filters are provided than the actual number of coordinate types available starting at {@code pos},
     * or if any of the filters throws any checked exceptions.
     */
    public static VarHandle filterCoordinates(VarHandle target, int pos, MethodHandle... filters) {
        return JLI.filterCoordinates(target, pos, filters);
    }

    /**
     * Provides a target var handle with one or more <em>bound coordinates</em>
     * in advance of the var handle's invocation. As a consequence, the resulting var handle will feature less
     * coordinate types than the target var handle.
     * <p>
     * When calling e.g. {@link VarHandle#get(Object...)} on the resulting var handle, incoming coordinate values
     * are joined with bound coordinate values, and then passed to the target var handle.
     * <p>
     * For the bound coordinates to be well formed, their types must be {@code T1, T2 ... Tn },
     * where {@code T1, T2 ... Tn} are the coordinate types starting at position {@code pos} of the target var handle.
     * <p>
     * The resulting var handle will feature the same access modes (see {@link java.lang.invoke.VarHandle.AccessMode}) and
     * atomic access guarantees as those featured by the target var handle.
     *
     * @param target the var handle to invoke after the bound coordinates are inserted
     * @param pos the position of the first coordinate to be inserted
     * @param values the series of bound coordinates to insert
     * @return an adapter var handle which inserts an additional coordinates,
     *         before calling the target var handle
     * @throws IllegalArgumentException if {@code pos} is not between 0 and the target var handle coordinate arity, inclusive,
     * or if more values are provided than the actual number of coordinate types available starting at {@code pos}.
     * @throws ClassCastException if the bound coordinates in {@code values} are not well-formed, that is, they have types
     * other than {@code T1, T2 ... Tn }, where {@code T1, T2 ... Tn} are the coordinate types starting at position {@code pos}
     * of the target var handle.
     */
    public static VarHandle insertCoordinates(VarHandle target, int pos, Object... values) {
        return JLI.insertCoordinates(target, pos, values);
    }

    /**
     * Provides a var handle which adapts the coordinate values of the target var handle, by re-arranging them
     * so that the new coordinates match the provided ones.
     * <p>
     * The given array controls the reordering.
     * Call {@code #I} the number of incoming coordinates (the value
     * {@code newCoordinates.size()}, and call {@code #O} the number
     * of outgoing coordinates (the number of coordinates associated with the target var handle).
     * Then the length of the reordering array must be {@code #O},
     * and each element must be a non-negative number less than {@code #I}.
     * For every {@code N} less than {@code #O}, the {@code N}-th
     * outgoing coordinate will be taken from the {@code I}-th incoming
     * coordinate, where {@code I} is {@code reorder[N]}.
     * <p>
     * No coordinate value conversions are applied.
     * The type of each incoming coordinate, as determined by {@code newCoordinates},
     * must be identical to the type of the corresponding outgoing coordinate
     * in the target var handle.
     * <p>
     * The reordering array need not specify an actual permutation.
     * An incoming coordinate will be duplicated if its index appears
     * more than once in the array, and an incoming coordinate will be dropped
     * if its index does not appear in the array.
     * <p>
     * The resulting var handle will feature the same access modes (see {@link java.lang.invoke.VarHandle.AccessMode}) and
     * atomic access guarantees as those featured by the target var handle.
     * @param target the var handle to invoke after the coordinates have been reordered
     * @param newCoordinates the new coordinate types
     * @param reorder an index array which controls the reordering
     * @return an adapter var handle which re-arranges the incoming coordinate values,
     * before calling the target var handle
     * @throws IllegalArgumentException if the index array length is not equal to
     * the number of coordinates of the target var handle, or if any index array element is not a valid index for
     * a coordinate of {@code newCoordinates}, or if two corresponding coordinate types in
     * the target var handle and in {@code newCoordinates} are not identical.
     */
    public static VarHandle permuteCoordinates(VarHandle target, List<Class<?>> newCoordinates, int... reorder) {
        return JLI.permuteCoordinates(target, newCoordinates, reorder);
    }

    /**
     * Adapts a target var handle handle by pre-processing
     * a sub-sequence of its coordinate values with a filter (a method handle).
     * The pre-processed coordinates are replaced by the result (if any) of the
     * filter function and the target var handle is then called on the modified (usually shortened)
     * coordinate list.
     * <p>
     * If {@code R} is the return type of the filter (which cannot be void), the target var handle must accept a value of
     * type {@code R} as its coordinate in position {@code pos}, preceded and/or followed by
     * any coordinate not passed to the filter.
     * No coordinates are reordered, and the result returned from the filter
     * replaces (in order) the whole subsequence of coordinates originally
     * passed to the adapter.
     * <p>
     * The argument types (if any) of the filter
     * replace zero or one coordinate types of the target var handle, at position {@code pos},
     * in the resulting adapted var handle.
     * The return type of the filter must be identical to the
     * coordinate type of the target var handle at position {@code pos}, and that target var handle
     * coordinate is supplied by the return value of the filter.
     * <p>
     * The resulting var handle will feature the same access modes (see {@link java.lang.invoke.VarHandle.AccessMode}) and
     * atomic access guarantees as those featured by the target var handle.
     *
     * @param target the var handle to invoke after the coordinates have been filtered
     * @param pos the position of the coordinate to be filtered
     * @param filter the filter method handle
     * @return an adapter var handle which filters the incoming coordinate values,
     * before calling the target var handle
     * @throws IllegalArgumentException if the return type of {@code filter}
     * is void, or it is not the same as the {@code pos} coordinate of the target var handle,
     * if {@code pos} is not between 0 and the target var handle coordinate arity, inclusive,
     * if the resulting var handle's type would have <a href="MethodHandle.html#maxarity">too many coordinates</a>,
     * or if {@code filter} throws any checked exceptions.
     */
    public static VarHandle collectCoordinates(VarHandle target, int pos, MethodHandle filter) {
        return JLI.collectCoordinates(target, pos, filter);
    }

    /**
     * Returns a var handle which will discard some dummy coordinates before delegating to the
     * target var handle. As a consequence, the resulting var handle will feature more
     * coordinate types than the target var handle.
     * <p>
     * The {@code pos} argument may range between zero and <i>N</i>, where <i>N</i> is the arity of the
     * target var handle's coordinate types. If {@code pos} is zero, the dummy coordinates will precede
     * the target's real arguments; if {@code pos} is <i>N</i> they will come after.
     * <p>
     * The resulting var handle will feature the same access modes (see {@link java.lang.invoke.VarHandle.AccessMode}) and
     * atomic access guarantees as those featured by the target var handle.
     *
     * @param target the var handle to invoke after the dummy coordinates are dropped
     * @param pos position of first coordinate to drop (zero for the leftmost)
     * @param valueTypes the type(s) of the coordinate(s) to drop
     * @return an adapter var handle which drops some dummy coordinates,
     *         before calling the target var handle
     * @throws IllegalArgumentException if {@code pos} is not between 0 and the target var handle coordinate arity, inclusive.
     */
    public static VarHandle dropCoordinates(VarHandle target, int pos, Class<?>... valueTypes) {
        return JLI.dropCoordinates(target, pos, valueTypes);
    }

    private static void checkAddressFirstCoordinate(VarHandle handle) {
        if (handle.coordinateTypes().size() < 1 ||
                handle.coordinateTypes().get(0) != MemorySegment.class) {
            throw new IllegalArgumentException("Expected var handle with leading coordinate of type MemorySegment");
        }
    }

    private static void checkCarrier(Class<?> carrier) {
        if (!carrier.isPrimitive() || carrier == void.class || carrier == boolean.class) {
            throw new IllegalArgumentException("Illegal carrier: " + carrier.getSimpleName());
        }
    }

    private static long carrierSize(Class<?> carrier) {
        long bitsAlignment = Math.max(8, Wrapper.forPrimitiveType(carrier).bitWidth());
        return Utils.bitsToBytesOrThrow(bitsAlignment, IllegalStateException::new);
    }

    private static void checkWidenable(Class<?> carrier) {
        if (!(carrier == byte.class || carrier == short.class || carrier == int.class)) {
            throw new IllegalArgumentException("illegal carrier:" + carrier.getSimpleName());
        }
    }

    private static void checkNarrowable(Class<?> type) {
        if (!(type == int.class || type == long.class)) {
            throw new IllegalArgumentException("illegal adapter type: " + type.getSimpleName());
        }
    }

    private static void checkTargetWiderThanCarrier(Class<?> carrier, Class<?> target) {
        if (Wrapper.forPrimitiveType(target).bitWidth() <= Wrapper.forPrimitiveType(carrier).bitWidth()) {
            throw new IllegalArgumentException(
                    target.getSimpleName() + " is not wider than: " + carrier.getSimpleName());
        }
    }

    private static MemoryAddress longToAddress(long value) {
        return MemoryAddress.ofLong(value);
    }

    private static long addressToLong(MemoryAddress value) {
        return value.toRawLongValue();
    }
}
