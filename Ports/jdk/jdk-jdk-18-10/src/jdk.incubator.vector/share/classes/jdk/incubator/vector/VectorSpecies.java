/*
 * Copyright (c) 2017, 2020, Oracle and/or its affiliates. All rights reserved.
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
package jdk.incubator.vector;

import java.nio.ByteOrder;
import java.util.function.IntUnaryOperator;

/**
 * Interface for managing all vectors of the same combination
 * of <a href="Vector.html#ETYPE">element type</a> ({@code ETYPE})
 * and {@link VectorShape shape}.
 *
 * @apiNote
 * User code should not implement this interface.  A future release of
 * this type may restrict implementations to be members of the same
 * package.
 *
 * @implNote
 * The string representation of an instance of this interface will
 * be of the form "Species[ETYPE, VLENGTH, SHAPE]", where {@code
 * ETYPE} is the primitive {@linkplain #elementType() lane type},
 * {@code VLENGTH} is the {@linkplain #length() vector lane count}
 * associated with the species, and {@code SHAPE} is the {@linkplain
 * #vectorShape() vector shape} associated with the species.
 *
 * <p>Vector species objects can be stored in locals and parameters and as
 * {@code static final} constants, but storing them in other Java
 * fields or in array elements, while semantically valid, may incur
 * performance penalties.
 *
 * @param <E> the boxed version of {@code ETYPE},
 *           the element type of a vector
 */
public interface VectorSpecies<E> {
    /**
     * Returns the primitive element type of vectors of this
     * species.
     *
     * @return the primitive element type ({@code ETYPE})
     * @see Class#arrayType()
     */
    Class<E> elementType();

    /**
     * Returns the vector type of this species.
     * A vector is of this species if and only if
     * it is of the corresponding vector type.
     *
     * @return the vector type of this species
     */
    Class<? extends Vector<E>> vectorType();

    /**
     * Returns the vector mask type for this species.
     *
     * @return the mask type
     */
    Class<? extends VectorMask<E>> maskType();

    /**
     * Returns the lane size, in bits, of vectors of this
     * species.
     *
     * @return the element size, in bits
     */
    int elementSize();

    /**
     * Returns the shape of vectors produced by this
     * species.
     *
     * @return the shape of any vectors of this species
     */
    VectorShape vectorShape();

    /**
     * Returns the number of lanes in a vector of this species.
     *
     * @apiNote This is also the number of lanes in a mask or
     * shuffle associated with a vector of this species.
     *
     * @return the number of vector lanes
     */
    int length();

    /**
     * Returns the total vector size, in bits, of any vector
     * of this species.
     * This is the same value as {@code this.vectorShape().vectorBitSize()}.
     *
     * @apiNote This size may be distinct from the size in bits
     * of a mask or shuffle of this species.
     *
     * @return the total vector size, in bits
     */
    int vectorBitSize();

    /**
     * Returns the total vector size, in bytes, of any vector
     * of this species.
     * This is the same value as {@code this.vectorShape().vectorBitSize() / Byte.SIZE}.
     *
     * @apiNote This size may be distinct from the size in bits
     * of a mask or shuffle of this species.
     *
     * @return the total vector size, in bytes
     */
    int vectorByteSize();

    /**
     * Loop control function which returns the largest multiple of
     * {@code VLENGTH} that is less than or equal to the given
     * {@code length} value.
     * Here, {@code VLENGTH} is the result of {@code this.length()},
     * and {@code length} is interpreted as a number of lanes.
     * The resulting value {@code R} satisfies this inequality:
     * <pre>{@code R <= length < R+VLENGTH}
     * </pre>
     * <p> Specifically, this method computes
     * {@code length - floorMod(length, VLENGTH)}, where
     * {@link Math#floorMod(int,int) floorMod} computes a remainder
     * value by rounding its quotient toward negative infinity.
     * As long as {@code VLENGTH} is a power of two, then the result
     * is also equal to {@code length & ~(VLENGTH - 1)}.
     *
     * @param length the input length
     * @return the largest multiple of the vector length not greater
     *         than the given length
     * @throws IllegalArgumentException if the {@code length} is
               negative and the result would overflow to a positive value
     * @see Math#floorMod(int, int)
     */
    int loopBound(int length);

    /**
     * Returns a mask of this species where only
     * the lanes at index N such that the adjusted index
     * {@code N+offset} is in the range {@code [0..limit-1]}
     * are set.
     *
     * <p>
     * This method returns the value of the expression
     * {@code maskAll(true).indexInRange(offset, limit)}
     *
     * @param offset the starting index
     * @param limit the upper-bound (exclusive) of index range
     * @return a mask with out-of-range lanes unset
     * @see VectorMask#indexInRange(int, int)
     */
    VectorMask<E> indexInRange(int offset, int limit);

    /**
     * Checks that this species has the given element type,
     * and returns this species unchanged.
     * The effect is similar to this pseudocode:
     * {@code elementType == elementType()
     *        ? this
     *        : throw new ClassCastException()}.
     *
     * @param elementType the required lane type
     * @param <F> the boxed element type of the required lane type
     * @return the same species
     * @throws ClassCastException if the species has the wrong element type
     * @see Vector#check(Class)
     * @see Vector#check(VectorSpecies)
     */
    <F> VectorSpecies<F> check(Class<F> elementType);

    /**
     * Given this species and a second one, reports the net
     * expansion or contraction of a (potentially) resizing
     * {@linkplain Vector#reinterpretShape(VectorSpecies,int) reinterpretation cast}
     * or
     * {@link Vector#convertShape(VectorOperators.Conversion,VectorSpecies,int) lane-wise conversion}
     * from this species to the second.
     *
     * The sign and magnitude of the return value depends on the size
     * difference between the proposed input and output
     * <em>shapes</em>, and (optionally, if {@code lanewise} is true)
     * also on the size difference between the proposed input and
     * output <em>lanes</em>.
     *
     * <ul>
     * <li> First, a logical result size is determined.
     *
     * If {@code lanewise} is false, this size that of the input
     * {@code VSHAPE}.  If {@code lanewise} is true, the logical
     * result size is the product of the input {@code VLENGTH}
     * times the size of the <em>output</em> {@code ETYPE}.
     *
     * <li> Next, the logical result size is compared against
     * the size of the proposed output shape, to see how it
     * will fit.
     *
     * <li> If the logical result fits precisely in the
     * output shape, the return value is zero, signifying
     * no net expansion or contraction.
     *
     * <li> If the logical result would overflow the output shape, the
     * return value is the ratio (greater than one) of the logical
     * result size to the (smaller) output size.  This ratio can be
     * viewed as measuring the proportion of "dropped input bits"
     * which must be deleted from the input in order for the result to
     * fit in the output vector.  It is also the <em>part limit</em>,
     * a upper exclusive limit on the {@code part} parameter to a
     * method that would transform the input species to the output
     * species.
     *
     * <li> If the logical result would drop into the output shape
     * with room to spare, the return value is a negative number whose
     * absolute value the ratio (greater than one) between the output
     * size and the (smaller) logical result size.  This ratio can be
     * viewed as measuring the proportion of "extra padding bits"
     * which must be added to the logical result to fill up the output
     * vector.  It is also the <em>part limit</em>, an exclusive lower
     * limit on the {@code part} parameter to a method that would
     * transform the input species to the output species.
     *
     * </ul>
     *
     * @param outputSpecies the proposed output species
     * @param lanewise whether to take lane sizes into account
     * @return an indication of the size change, as a signed ratio or zero
     *
     * @see Vector#reinterpretShape(VectorSpecies,int)
     * @see Vector#convertShape(VectorOperators.Conversion,VectorSpecies,int)
     */
    int partLimit(VectorSpecies<?> outputSpecies, boolean lanewise);

    // Factories

    /**
     * Finds a species with the given element type and the
     * same shape as this species.
     * Returns the same value as
     * {@code VectorSpecies.of(newType, this.vectorShape())}.
     *
     * @param newType the new element type
     * @param <F> the boxed element type
     * @return a species for the new element type and the same shape
     * @throws IllegalArgumentException if no such species exists for the
     *         given combination of element type and shape
     *         or if the given type is not a valid {@code ETYPE}
     * @see #withShape(VectorShape)
     * @see VectorSpecies#of(Class, VectorShape)
     */
    <F> VectorSpecies<F> withLanes(Class<F> newType);

    /**
     * Finds a species with the given shape and the same
     * elementType as this species.
     * Returns the same value as
     * {@code VectorSpecies.of(this.elementType(), newShape)}.
     *
     * @param newShape the new shape
     * @return a species for the same element type and the new shape
     * @throws IllegalArgumentException if no such species exists for the
     *         given combination of element type and shape
     * @see #withLanes(Class)
     * @see VectorSpecies#of(Class, VectorShape)
     */
    VectorSpecies<E> withShape(VectorShape newShape);

    /**
     * Finds a species for an element type and shape.
     *
     * @param elementType the element type
     * @param shape the shape
     * @param <E> the boxed element type
     * @return a species for the given element type and shape
     * @throws IllegalArgumentException if no such species exists for the
     *         given combination of element type and shape
     *         or if the given type is not a valid {@code ETYPE}
     * @see #withLanes(Class)
     * @see #withShape(VectorShape)
     */
    static <E> VectorSpecies<E> of(Class<E> elementType, VectorShape shape) {
        LaneType laneType = LaneType.of(elementType);
        return AbstractSpecies.findSpecies(elementType, laneType, shape);
    }

    /**
     * Finds the largest vector species of the given element type.
     * <p>
     * The returned species is a species chosen by the platform that has a
     * shape with the largest possible bit-size for the given element type.
     * The underlying vector shape might not support other lane types
     * on some platforms, which may limit the applicability of
     * {@linkplain Vector#reinterpretShape(VectorSpecies,int) reinterpretation casts}.
     * Vector algorithms which require reinterpretation casts will
     * be more portable if they use the platform's
     * {@linkplain #ofPreferred(Class) preferred species}.
     *
     * @param etype the element type
     * @param <E> the boxed element type
     * @return a preferred species for an element type
     * @throws IllegalArgumentException if no such species exists for the
     *         element type
     *         or if the given type is not a valid {@code ETYPE}
     * @see VectorSpecies#ofPreferred(Class)
     */
    static <E> VectorSpecies<E> ofLargestShape(Class<E> etype) {
        return VectorSpecies.of(etype, VectorShape.largestShapeFor(etype));
    }

    /**
     * Finds the species preferred by the current platform
     * for a given vector element type.
     * This is the same value as
     * {@code VectorSpecies.of(etype, VectorShape.preferredShape())}.
     *
     * <p> This species is chosen by the platform so that it has the
     * largest possible shape that supports all lane element types.
     * This has the following implications:
     * <ul>
     * <li>The various preferred species for different element types
     * will have the same underlying shape.
     * <li>All vectors created from preferred species will have a
     * common bit-size and information capacity.
     * <li>{@linkplain Vector#reinterpretShape(VectorSpecies, int) Reinterpretation casts}
     * between vectors of preferred species will neither truncate
     * lanes nor fill them with default values.
     * <li>For any particular element type, some platform might possibly
     * provide a {@linkplain #ofLargestShape(Class) larger vector shape}
     * that (as a trade-off) does not support all possible element types.
     * </ul>
     *
     * @implNote On many platforms there is no behavioral difference
     * between {@link #ofLargestShape(Class) ofLargestShape} and
     * {@code ofPreferred}, because the preferred shape is usually
     * also the largest available shape for every lane type.
     * Therefore, most vector algorithms will perform well without
     * {@code ofLargestShape}.
     *
     * @param etype the element type
     * @param <E> the boxed element type
     * @return a preferred species for this element type
     * @throws IllegalArgumentException if no such species exists for the
     *         element type
     *         or if the given type is not a valid {@code ETYPE}
     * @see Vector#reinterpretShape(VectorSpecies,int)
     * @see VectorShape#preferredShape()
     * @see VectorSpecies#ofLargestShape(Class)
     */
    public static <E> VectorSpecies<E> ofPreferred(Class<E> etype) {
        return of(etype, VectorShape.preferredShape());
    }

    /**
     * Returns the bit-size of the given vector element type ({@code ETYPE}).
     * The element type must be a valid {@code ETYPE}, not a
     * wrapper type or other object type.
     *
     * The element type argument must be a mirror for a valid vector
     * {@code ETYPE}, such as {@code byte.class}, {@code int.class},
     * or {@code double.class}.  The bit-size of such a type is the
     * {@code SIZE} constant for the corresponding wrapper class, such
     * as {@code Byte.SIZE}, or {@code Integer.SIZE}, or
     * {@code Double.SIZE}.
     *
     * @param elementType a vector element type (an {@code ETYPE})
     * @return the bit-size of {@code elementType}, such as 32 for {@code int.class}
     * @throws IllegalArgumentException
     *         if the given {@code elementType} argument is not
     *         a valid vector {@code ETYPE}
     */
    static int elementSize(Class<?> elementType) {
        return LaneType.of(elementType).elementSize;
    }

    /// Convenience factories:

    /**
     * Returns a vector of this species
     * where all lane elements are set to
     * the default primitive value, {@code (ETYPE)0}.
     *
     * Equivalent to {@code IntVector.zero(this)}
     * or an equivalent {@code zero} method,
     * on the vector type corresponding to
     * this species.
     *
     * @return a zero vector of the given species
     * @see IntVector#zero(VectorSpecies)
     * @see FloatVector#zero(VectorSpecies)
     */
    Vector<E> zero();

    /**
     * Returns a vector of this species
     * where lane elements are initialized
     * from the given array at the given offset.
     * The array must be of the the correct {@code ETYPE}.
     *
     * Equivalent to
     * {@code IntVector.fromArray(this,a,offset)}
     * or an equivalent {@code fromArray} method,
     * on the vector type corresponding to
     * this species.
     *
     * @param a an array of the {@code ETYPE} for this species
     * @param offset the index of the first lane value to load
     * @return a vector of the given species filled from the array
     * @throws IndexOutOfBoundsException
     *         if {@code offset+N < 0} or {@code offset+N >= a.length}
     *         for any lane {@code N} in the vector
     * @see IntVector#fromArray(VectorSpecies,int[],int)
     * @see FloatVector#fromArray(VectorSpecies,float[],int)
     */
    Vector<E> fromArray(Object a, int offset);
    // Defined when ETYPE is known.

    /**
     * Loads a vector of this species from a byte array starting
     * at an offset.
     * Bytes are composed into primitive lane elements according
     * to the specified byte order.
     * The vector is arranged into lanes according to
     * <a href="Vector.html#lane-order">memory ordering</a>.
     * <p>
     * Equivalent to
     * {@code IntVector.fromByteArray(this,a,offset,bo)}
     * or an equivalent {@code fromByteArray} method,
     * on the vector type corresponding to
     * this species.
     *
     * @param a a byte array
     * @param offset the index of the first byte to load
     * @param bo the intended byte order
     * @return a vector of the given species filled from the byte array
     * @throws IndexOutOfBoundsException
     *         if {@code offset+N*ESIZE < 0}
     *         or {@code offset+(N+1)*ESIZE > a.length}
     *         for any lane {@code N} in the vector
     * @see IntVector#fromByteArray(VectorSpecies,byte[],int,ByteOrder)
     * @see FloatVector#fromByteArray(VectorSpecies,byte[],int,ByteOrder)
     */
    Vector<E> fromByteArray(byte[] a, int offset, ByteOrder bo);

    /**
     * Returns a mask of this species
     * where lane elements are initialized
     * from the given array at the given offset.
     *
     * Equivalent to
     * {@code VectorMask.fromArray(this,a,offset)}.
     *
     * @param bits the {@code boolean} array
     * @param offset the offset into the array
     * @return the mask loaded from the {@code boolean} array
     * @throws IndexOutOfBoundsException
     *         if {@code offset+N < 0} or {@code offset+N >= a.length}
     *         for any lane {@code N} in the vector mask
     * @see VectorMask#fromArray(VectorSpecies,boolean[],int)
     */
    VectorMask<E> loadMask(boolean[] bits, int offset);

    /**
     * Returns a mask of this species,
     * where each lane is set or unset according to given
     * single boolean, which is broadcast to all lanes.
     *
     * @param bit the given mask bit to be replicated
     * @return a mask where each lane is set or unset according to
     *         the given bit
     * @see Vector#maskAll(boolean)
     */
    VectorMask<E> maskAll(boolean bit);

    /**
     * Returns a vector of the given species
     * where all lane elements are set to
     * the primitive value {@code e}.
     *
     * <p> This method returns the value of this expression:
     * {@code EVector.broadcast(this, (ETYPE)e)}, where
     * {@code EVector} is the vector class specific to the
     * the {@code ETYPE} of this species.
     * The {@code long} value must be accurately representable
     * by {@code ETYPE}, so that {@code e==(long)(ETYPE)e}.
     *
     * @param e the value to broadcast
     * @return a vector where all lane elements are set to
     *         the primitive value {@code e}
     * @throws IllegalArgumentException
     *         if the given {@code long} value cannot
     *         be represented by the vector species {@code ETYPE}
     * @see Vector#broadcast(long)
     * @see #checkValue(long)
     */
    Vector<E> broadcast(long e);

    /**
     * Checks that this species can represent the given element value,
     * and returns the value unchanged.
     *
     * The {@code long} value must be accurately representable
     * by the {@code ETYPE} of the vector species, so that
     * {@code e==(long)(ETYPE)e}.
     *
     * The effect is similar to this pseudocode:
     * {@code e == (long)(ETYPE)e
     *        ? e
     *        : throw new IllegalArgumentException()}.
     *
     * @param e the value to be checked
     * @return {@code e}
     * @throws IllegalArgumentException
     *         if the given {@code long} value cannot
     *         be represented by the vector species {@code ETYPE}
     * @see #broadcast(long)
     */
    long checkValue(long e);

    /**
     * Creates a shuffle for this species from
     * a series of source indexes.
     *
     * <p> For each shuffle lane, where {@code N} is the shuffle lane
     * index, the {@code N}th index value is validated
     * against the species {@code VLENGTH}, and (if invalid)
     * is partially wrapped to an exceptional index in the
     * range {@code [-VLENGTH..-1]}.
     *
     * @param sourceIndexes the source indexes which the shuffle will draw from
     * @return a shuffle where each lane's source index is set to the given
     *         {@code int} value, partially wrapped if exceptional
     * @throws IndexOutOfBoundsException if {@code sourceIndexes.length != VLENGTH}
     * @see VectorShuffle#fromValues(VectorSpecies,int...)
     */
    VectorShuffle<E> shuffleFromValues(int... sourceIndexes);

    /**
     * Creates a shuffle for this species from
     * an {@code int} array starting at an offset.
     *
     * <p> For each shuffle lane, where {@code N} is the shuffle lane
     * index, the array element at index {@code i + N} is validated
     * against the species {@code VLENGTH}, and (if invalid)
     * is partially wrapped to an exceptional index in the
     * range {@code [-VLENGTH..-1]}.
     *
     * @param sourceIndexes the source indexes which the shuffle will draw from
     * @param offset the offset into the array
     * @return a shuffle where each lane's source index is set to the given
     *         {@code int} value, partially wrapped if exceptional
     * @throws IndexOutOfBoundsException if {@code offset < 0}, or
     *         {@code offset > sourceIndexes.length - VLENGTH}
     * @see VectorShuffle#fromArray(VectorSpecies,int[],int)
     */
    VectorShuffle<E> shuffleFromArray(int[] sourceIndexes, int offset);

    /**
     * Creates a shuffle for this species from
     * the successive values of an operator applied to
     * the range {@code [0..VLENGTH-1]}.
     *
     * <p> For each shuffle lane, where {@code N} is the shuffle lane
     * index, the {@code N}th index value is validated
     * against the species {@code VLENGTH}, and (if invalid)
     * is partially wrapped to an exceptional index in the
     * range {@code [-VLENGTH..-1]}.
     *
     * <p> Care should be taken to ensure {@code VectorShuffle} values
     * produced from this method are consumed as constants to ensure
     * optimal generation of code.  For example, shuffle values can be
     * held in {@code static final} fields or loop-invariant local variables.
     *
     * <p> This method behaves as if a shuffle is created from an array of
     * mapped indexes as follows:
     * <pre>{@code
     *   int[] a = new int[VLENGTH];
     *   for (int i = 0; i < a.length; i++) {
     *       a[i] = fn.applyAsInt(i);
     *   }
     *   return VectorShuffle.fromArray(this, a, 0);
     * }</pre>
     *
     * @param fn the lane index mapping function
     * @return a shuffle of mapped indexes
     * @see VectorShuffle#fromOp(VectorSpecies,IntUnaryOperator)
     */
    VectorShuffle<E> shuffleFromOp(IntUnaryOperator fn);

    /**
     * Creates a shuffle using source indexes set to sequential
     * values starting from {@code start} and stepping
     * by the given {@code step}.
     * <p>
     * This method returns the value of the expression
     * {@code VectorSpecies.shuffleFromOp(i -> R(start + i * step))},
     * where {@code R} is {@link VectorShuffle#wrapIndex(int) wrapIndex}
     * if {@code wrap} is true, and is the identity function otherwise.
     * <p>
     * If {@code wrap} is false each index is validated
     * against the species {@code VLENGTH}, and (if invalid)
     * is partially wrapped to an exceptional index in the
     * range {@code [-VLENGTH..-1]}.
     * Otherwise, if {@code wrap} is true, also reduce each index, as if
     * by {@link VectorShuffle#wrapIndex(int) wrapIndex},
     * to the valid range {@code [0..VLENGTH-1]}.
     *
     * @apiNote The {@code wrap} parameter should be set to {@code
     * true} if invalid source indexes should be wrapped.  Otherwise,
     * setting it to {@code false} allows invalid source indexes to be
     * range-checked by later operations such as
     * {@link Vector#rearrange(VectorShuffle) unary rearrange}.
     *
     * @param start the starting value of the source index sequence, typically {@code 0}
     * @param step the difference between adjacent source indexes, typically {@code 1}
     * @param wrap whether to wrap resulting indexes modulo {@code VLENGTH}
     * @return a shuffle of sequential lane indexes
     * @see VectorShuffle#iota(VectorSpecies,int,int,boolean)
     */
    VectorShuffle<E> iotaShuffle(int start, int step, boolean wrap);

    /**
     * Returns a string of the form "Species[ETYPE, VLENGTH, SHAPE]",
     * where {@code ETYPE} is the primitive {@linkplain #elementType()
     * lane type}, {@code VLENGTH} is the {@linkplain #length()
     * vector lane count} associated with the species, and {@code
     * SHAPE} is the {@linkplain #vectorShape() vector shape}
     * associated with the species.
     *
     * @return a string of the form "Species[ETYPE, VLENGTH, SHAPE]"
     */
    @Override
    String toString();

    /**
     * Indicates whether this species is identical to some other object.
     * Two species are identical only if they have the same shape
     * and same element type.
     *
     * @return whether this species is identical to some other object
     */
    @Override
    boolean equals(Object obj);

    /**
     * Returns a hash code value for the species,
     * based on the vector shape and element type.
     *
     * @return  a hash code value for this species
     */
    @Override
    int hashCode();

    // ==== JROSE NAME CHANGES ====

    // ADDED:
    // * genericElementType()-> E.class (interop)
    // * arrayType()-> ETYPE[].class (interop)
    // * withLanes(Class), withShape(VectorShape) strongly typed reinterpret casting
    // * static ofLargestShape(Class<E> etype) -> possibly non-preferred
    // * static preferredShape() -> common shape of all preferred species
    // * toString(), equals(Object), hashCode() (documented)
    // * elementSize(e) replaced bitSizeForVectorLength
    // * zero(), broadcast(long), from[Byte]Array(), loadMask() (convenience constructors)
    // * lanewise(op, [v], [m]), reduceLanesToLong(op, [m])

}
