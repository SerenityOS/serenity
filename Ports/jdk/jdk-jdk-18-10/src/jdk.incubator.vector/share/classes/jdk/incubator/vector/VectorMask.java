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

import jdk.internal.misc.Unsafe;
import jdk.internal.vm.annotation.ForceInline;
import jdk.internal.vm.vector.VectorSupport;

import java.util.Arrays;
import java.util.Objects;

/**
 * A {@code VectorMask} represents an ordered immutable sequence of {@code boolean}
 * values.
 * <p>
 * A {@code VectorMask} and {@code Vector} of the same
 * <a href="Vector.html#ETYPE">element type</a>
 * ({@code ETYPE}) and {@link VectorShape shape} have the same number of lanes,
 * and are therefore compatible (specifically, their {@link #vectorSpecies()
 * vector species} are compatible).
 * <p>
 * Some vector operations accept (compatible) masks to control the
 * selection and operation of lane elements of input vectors.
 * <p>
 * The number of values in the sequence is referred to as the {@code VectorMask}
 * {@link #length() length}. The length also corresponds to the number of
 * VectorMask lanes.  The lane element at lane index {@code N} (from {@code 0},
 * inclusive, to length, exclusive) corresponds to the {@code N + 1}'th
 * value in the sequence.
 * <p>
 * A lane is said to be <em>set</em> if the lane element is {@code true},
 * otherwise a lane is said to be <em>unset</em> if the lane element is
 * {@code false}.
 * <p>
 * VectorMask declares a limited set of unary, binary and reduction operations.
 * <ul>
 * <li>
 * A lane-wise unary operation operates on one input mask and produces a
 * result mask.
 * For each lane of the input mask the
 * lane element is operated on using the specified scalar unary operation and
 * the boolean result is placed into the mask result at the same lane.
 * The following pseudocode illustrates the behavior of this operation category:
 *
 * <pre>{@code
 * VectorMask<E> a = ...;
 * boolean[] ar = new boolean[a.length()];
 * for (int i = 0; i < a.length(); i++) {
 *     ar[i] = scalar_unary_op(a.laneIsSet(i));
 * }
 * VectorMask<E> r = VectorMask.fromArray(a.vectorSpecies(), ar, 0);
 * }</pre>
 *
 * <li>
 * A lane-wise binary operation operates on two input
 * masks to produce a result mask.
 * For each lane of the two input masks a and b,
 * the corresponding lane elements from a and b are operated on
 * using the specified scalar binary operation and the boolean result is placed
 * into the mask result at the same lane.
 * The following pseudocode illustrates the behavior of this operation category:
 *
 * <pre>{@code
 * VectorMask<E> a = ...;
 * VectorMask<E> b = ...;
 * boolean[] ar = new boolean[a.length()];
 * for (int i = 0; i < a.length(); i++) {
 *     ar[i] = scalar_binary_op(a.laneIsSet(i), b.laneIsSet(i));
 * }
 * VectorMask<E> r = VectorMask.fromArray(a.vectorSpecies(), ar, 0);
 * }</pre>
 *
 * <li>
 * A cross-lane reduction operation accepts an input mask and produces a scalar result.
 * For each lane of the input mask the lane element is operated on, together with a scalar accumulation value,
 * using the specified scalar binary operation.  The scalar result is the final value of the accumulator. The
 * following pseudocode illustrates the behaviour of this operation category:
 *
 * <pre>{@code
 * Mask<E> a = ...;
 * int acc = zero_for_scalar_binary_op;  // 0, or 1 for &
 * for (int i = 0; i < a.length(); i++) {
 *      acc = scalar_binary_op(acc, a.laneIsSet(i) ? 1 : 0);  // & | +
 * }
 * return acc;  // maybe boolean (acc != 0)
 * }</pre>
 *
 * </ul>
 * @param <E> the boxed version of {@code ETYPE},
 *           the element type of a vector
 *
 * <h2>Value-based classes and identity operations</h2>
 *
 * {@code VectorMask}, along with {@link Vector}, is a
 * <a href="{@docRoot}/java.base/java/lang/doc-files/ValueBased.html">value-based</a>
 * class.
 *
 * With {@code VectorMask}, identity-sensitive operations such as {@code ==}
 * may yield unpredictable results, or reduced performance.  Oddly
 * enough, {@link VectorMask#equals(Object) v.equals(w)} is likely to be
 * faster than {@code v==w}, since {@code equals} is <em>not</em>
 * an identity sensitive method.  (Neither is {@code toString} nor
 * {@code hashCode}.)

 * Also, vector mask objects can be stored in locals and parameters and as
 * {@code static final} constants, but storing them in other Java
 * fields or in array elements, while semantically valid, may incur
 * performance penalties.
 */
@SuppressWarnings("exports")
public abstract class VectorMask<E> extends jdk.internal.vm.vector.VectorSupport.VectorMask<E> {
    VectorMask(boolean[] bits) { super(bits); }

    /**
     * Returns the vector species to which this mask applies.
     * This mask applies to vectors of the same species,
     * and the same number of lanes.
     *
     * @return the vector species of this mask
     */
    public abstract VectorSpecies<E> vectorSpecies();

    /**
     * Returns the number of mask lanes.
     * This mask applies to vectors of the same number of lanes,
     * and the same species.
     *
     * @return the number of mask lanes
     */
    @ForceInline
    public final int length() {
        AbstractSpecies<E> vspecies = (AbstractSpecies<E>) vectorSpecies();
        return vspecies.laneCount();
    }

    /**
     * Returns a mask where each lane is set or unset according to given
     * {@code boolean} values.
     * <p>
     * For each mask lane, where {@code N} is the mask lane index,
     * if the given {@code boolean} value at index {@code N} is {@code true}
     * then the mask lane at index {@code N} is set, otherwise it is unset.
     * <p>
     * The given species must have a number of lanes that is compatible
     * with the given array.
     *
     * @param species vector species for the desired mask
     * @param bits the given {@code boolean} values
     * @param <E> the boxed element type
     * @return a mask where each lane is set or unset according to the given
     *         {@code boolean} value
     * @throws IllegalArgumentException
     *         if {@code bits.length != species.length()}
     * @see #fromLong(VectorSpecies, long)
     * @see #fromArray(VectorSpecies, boolean[], int)
     */
    @ForceInline
    public static <E> VectorMask<E> fromValues(VectorSpecies<E> species, boolean... bits) {
        AbstractSpecies<E> vspecies = (AbstractSpecies<E>) species;
        VectorIntrinsics.requireLength(bits.length, vspecies.laneCount());
        return fromArray(vspecies, bits, 0);
    }

    /**
     * Loads a mask from a {@code boolean} array starting at an offset.
     * <p>
     * For each mask lane, where {@code N} is the mask lane index,
     * if the array element at index {@code offset + N} is {@code true} then the
     * mask lane at index {@code N} is set, otherwise it is unset.
     *
     * @param species vector species for the desired mask
     * @param bits the {@code boolean} array
     * @param offset the offset into the array
     * @param <E> the boxed element type
     * @return the mask loaded from the {@code boolean} array
     * @throws IndexOutOfBoundsException if {@code offset < 0}, or
     * {@code offset > bits.length - species.length()}
     * @see #fromLong(VectorSpecies, long)
     * @see #fromValues(VectorSpecies, boolean...)
     */
    @ForceInline
    public static <E> VectorMask<E> fromArray(VectorSpecies<E> species, boolean[] bits, int offset) {
        AbstractSpecies<E> vsp = (AbstractSpecies<E>) species;
        int laneCount = vsp.laneCount();
        offset = VectorIntrinsics.checkFromIndexSize(offset, laneCount, bits.length);
        return VectorSupport.load(
                vsp.maskType(), vsp.elementType(), laneCount,
                bits, (long) offset + Unsafe.ARRAY_BOOLEAN_BASE_OFFSET,
                bits, offset, vsp,
                (c, idx, s)
                  -> s.opm(n -> c[idx + n]));
    }

    /**
     * Returns a mask where each lane is set or unset according to
     * the bits in the given bitmask, starting with the least
     * significant bit, and continuing up to the sign bit.
     * <p>
     * For each mask lane, where {@code N} is the mask lane index,
     * if the expression {@code (bits>>min(63,N))&1} is non-zero,
     * then the mask lane at index {@code N} is set, otherwise it is unset.
     * <p>
     * If the given species has fewer than 64 lanes, the high
     * {@code 64-VLENGTH} bits of the bit-mask are ignored.
     * If the given species has more than 64 lanes, the sign
     * bit is replicated into lane 64 and beyond.
     *
     * @param species vector species for the desired mask
     * @param bits the given mask bits, as a 64-bit signed integer
     * @param <E> the boxed element type
     * @return a mask where each lane is set or unset according to
     *         the bits in the given integer value
     * @see #fromValues(VectorSpecies, boolean...)
     * @see #fromArray(VectorSpecies, boolean[], int)
     */
    @ForceInline
    public static <E> VectorMask<E> fromLong(VectorSpecies<E> species, long bits) {
        AbstractSpecies<E> vspecies = (AbstractSpecies<E>) species;
        int laneCount = vspecies.laneCount();
        if (laneCount < Long.SIZE) {
            int extraSignBits = Long.SIZE - laneCount;
            bits <<= extraSignBits;
            bits >>= extraSignBits;
        }
        if (bits == (bits >> 1)) {
            // Special case.
            assert(bits == 0 || bits == -1);
            return vspecies.maskAll(bits != 0);
        }
        // FIXME: Intrinsify this.
        long shifted = bits;
        boolean[] a = new boolean[laneCount];
        for (int i = 0; i < a.length; i++) {
            a[i] = ((shifted & 1) != 0);
            shifted >>= 1;  // replicate sign bit
        }
        return fromValues(vspecies, a);
    }

    /**
     * Converts this mask to a mask of the given species of
     * element type {@code F}.
     * The {@code species.length()} must be equal to the
     * mask length.
     * The various mask lane bits are unmodified.
     * <p>
     * For each mask lane, where {@code N} is the lane index, if the
     * mask lane at index {@code N} is set, then the mask lane at index
     * {@code N} of the resulting mask is set, otherwise that mask lane is
     * not set.
     *
     * @param species vector species for the desired mask
     * @param <F> the boxed element type of the species
     * @return a mask converted by shape and element type
     * @throws IllegalArgumentException if this mask length and the species
     *         length differ
     */
    public abstract <F> VectorMask<F> cast(VectorSpecies<F> species);

    /**
     * Returns the lane elements of this mask packed into a {@code long}
     * value for at most the first 64 lane elements.
     * <p>
     * The lane elements are packed in the order of least significant bit
     * to most significant bit.
     * For each mask lane where {@code N} is the mask lane index, if the
     * mask lane is set then the {@code N}th bit is set to one in the
     * resulting {@code long} value, otherwise the {@code N}th bit is set
     * to zero.
     * The mask must have no more than 64 lanes.
     *
     * @return the lane elements of this mask packed into a {@code long}
     *         value.
     * @throws UnsupportedOperationException if there are more than 64 lanes
     *         in this mask
     */
    // FIXME: Consider changing method to accept part locating where to extract
    // out a 64bit value (in effect a contracting operation)
    public abstract long toLong();

    /**
     * Returns an {@code boolean} array containing the lane elements of this
     * mask.
     * <p>
     * This method behaves as if it stores
     * this mask into an allocated array
     * (using {@link #intoArray(boolean[], int)})
     * and returns that array as
     * follows:
     * <pre>{@code
     * boolean[] a = new boolean[this.length()];
     * this.intoArray(a, 0);
     * return a;
     * }</pre>
     *
     * @return an array containing the the lane elements of this vector
     */
    public abstract boolean[] toArray();

    /**
     * Stores this mask into a {@code boolean} array starting at offset.
     * <p>
     * For each mask lane, where {@code N} is the mask lane index,
     * the lane element at index {@code N} is stored into the array
     * element {@code a[offset+N]}.
     *
     * @param a the array, of type boolean[]
     * @param offset the offset into the array
     * @throws IndexOutOfBoundsException if {@code offset < 0} or
     *         {@code offset > a.length - this.length()}
     */
    public abstract void intoArray(boolean[] a, int offset);

    /**
     * Returns {@code true} if any of the mask lanes are set.
     *
     * @return {@code true} if any of the mask lanes are set, otherwise
     * {@code false}.
     */
    public abstract boolean anyTrue();

    /**
     * Returns {@code true} if all of the mask lanes are set.
     *
     * @return {@code true} if all of the mask lanes are set, otherwise
     * {@code false}.
     */
    public abstract boolean allTrue();

    /**
     * Returns the number of mask lanes that are set.
     *
     * @return the number of mask lanes that are set.
     */
    public abstract int trueCount();

    /**
     * Returns the index of the first mask lane that is set.
     * Returns {@code VLENGTH} if none of them are set.
     *
     * @return the index of the first mask lane that is set, or {@code VLENGTH}
     */
    public abstract int firstTrue();

    /**
     * Returns the index of the last mask lane that is set.
     * Returns {@code -1} if none of them are set.
     *
     * @return the index of the last mask lane that is set, or {@code -1}
     */
    public abstract int lastTrue();

    /**
     * Computes the logical intersection (as {@code a&b})
     * between this mask and a second input mask.
     * <p>
     * This is a lane-wise binary operation which applies
     * the logical {@code AND} operation
     * ({@code &}) to each corresponding pair of mask bits.
     *
     * @param m the second input mask
     * @return the result of logically conjoining the two input masks
     */
    public abstract VectorMask<E> and(VectorMask<E> m);

    /**
     * Computes the logical union (as {@code a|b}) of this mask
     * and a second input mask.
     * <p>
     * This is a lane-wise binary operation which applies
     * the logical {@code OR} operation
     * ({@code |}) to each corresponding pair of mask bits.
     *
     * @param m the input mask
     * @return the result of logically disjoining the two input masks
     */
    public abstract VectorMask<E> or(VectorMask<E> m);

    /**
     * Determines logical equivalence of this mask
     * to a second input mask (as boolean {@code a==b}
     * or {@code a^~b}).
     * <p>
     * This is a lane-wise binary operation tests each
     * corresponding pair of mask bits for equality.
     * It is also equivalent to a inverse {@code XOR}
     * operation ({@code ^~}) on the mask bits.
     *
     * @param m the input mask
     * @return a mask showing where the two input masks were equal
     * @see #equals
     */
    public abstract VectorMask<E> eq(VectorMask<E> m);

    /**
     * Logically subtracts a second input mask
     * from this mask (as {@code a&~b}).
     * <p>
     * This is a lane-wise binary operation which applies
     * the logical {@code ANDC} operation
     * ({@code &~}) to each corresponding pair of mask bits.
     *
     * @param m the second input mask
     * @return the result of logically subtracting the second mask from this mask
     */
    public abstract VectorMask<E> andNot(VectorMask<E> m);

    /**
     * Logically negates this mask.
     * <p>
     * This is a lane-wise binary operation which applies
     * the logical {@code NOT} operation
     * ({@code ~}) to each mask bit.
     *
     * @return the result of logically negating this mask
     */
    public abstract VectorMask<E> not();

    // FIXME: Consider blend, slice, rearrange operations.

    /**
     * Removes lanes numbered {@code N} from this mask where the
     * adjusted index {@code N+offset}, is not in the range
     * {@code [0..limit-1]}.
     *
     * <p> In all cases the series of set and unset lanes is assigned
     * as if by using infinite precision or {@code VLENGTH-}saturating
     * additions or subtractions, without overflow or wrap-around.
     *
     * @apiNote
     *
     * This method performs a SIMD emulation of the check performed by
     * {@link Objects#checkIndex(int,int)}, on the index numbers in
     * the range {@code [offset..offset+VLENGTH-1]}.  If an exception
     * is desired, the resulting mask can be compared with the
     * original mask; if they are not equal, then at least one lane
     * was out of range, and exception processing can be performed.
     *
     * <p> A mask which is a series of {@code N} set lanes followed by
     * a series of unset lanes can be obtained by calling
     * {@code allTrue.indexInRange(0, N)}, where {@code allTrue} is a
     * mask of all true bits.  A mask of {@code N1} unset lanes
     * followed by {@code N2} set lanes can be obtained by calling
     * {@code allTrue.indexInRange(-N1, N2)}.
     *
     * @param offset the starting index
     * @param limit the upper-bound (exclusive) of index range
     * @return the original mask, with out-of-range lanes unset
     * @see VectorSpecies#indexInRange(int, int)
     */
    public abstract VectorMask<E> indexInRange(int offset, int limit);

    /**
     * Returns a vector representation of this mask, the
     * lane bits of which are set or unset in correspondence
     * to the mask bits.
     *
     * For each mask lane, where {@code N} is the mask lane index, if
     * the mask lane is set at {@code N} then the specific non-default
     * value {@code -1} is placed into the resulting vector at lane
     * index {@code N}.  Otherwise the default element value {@code 0}
     * is placed into the resulting vector at lane index {@code N}.
     *
     * Whether the element type ({@code ETYPE}) of this mask is
     * floating point or integral, the lane value, as selected by the
     * mask, will be one of the two arithmetic values {@code 0} or
     * {@code -1}.  For every {@code ETYPE} the most significant bit
     * of the vector lane is set if and only if the mask lane is set.
     * In addition, for integral types, <em>all</em> lane bits are set
     * in lanes where the mask is set.
     *
     * <p> The vector returned is the same as would be computed by
     * {@code ZERO.blend(MINUS_ONE, this)}, where {@code ZERO} and
     * {@code MINUS_ONE} are vectors which replicate the default
     * {@code ETYPE} value and the {@code ETYPE} value representing
     * {@code -1}, respectively.
     *
     * @apiNote For the sake of static type checking, users may wish
     * to check the resulting vector against the expected integral
     * lane type or species.  If the mask is for a float-point
     * species, then the resulting vector will have the same shape and
     * lane size, but an integral type.  If the mask is for an
     * integral species, the resulting vector will be of exactly that
     * species.
     *
     * @return a vector representation of this mask
     * @see Vector#check(Class)
     * @see Vector#check(VectorSpecies)
     */
    public abstract Vector<E> toVector();

    /**
     * Tests if the lane at index {@code i} is set
     * @param i the lane index
     *
     * @return true if the lane at index {@code i} is set, otherwise false
     */
    public abstract boolean laneIsSet(int i);

    /**
     * Checks that this mask applies to vectors with the given element type,
     * and returns this mask unchanged.
     * The effect is similar to this pseudocode:
     * {@code elementType == vectorSpecies().elementType()
     *        ? this
     *        : throw new ClassCastException()}.
     *
     * @param elementType the required lane type
     * @param <F> the boxed element type of the required lane type
     * @return the same mask
     * @throws ClassCastException if the element type is wrong
     * @see Vector#check(Class)
     * @see VectorMask#check(VectorSpecies)
     */
    public abstract <F> VectorMask<F> check(Class<F> elementType);

    /**
     * Checks that this mask has the given species,
     * and returns this mask unchanged.
     * The effect is similar to this pseudocode:
     * {@code species == vectorSpecies()
     *        ? this
     *        : throw new ClassCastException()}.
     *
     * @param species vector species required for this mask
     * @param <F> the boxed element type of the required species
     * @return the same mask
     * @throws ClassCastException if the species is wrong
     * @see Vector#check(Class)
     * @see Vector#check(VectorSpecies)
     */
    public abstract <F> VectorMask<F> check(VectorSpecies<F> species);

    /**
     * Returns a string representation of this mask, of the form
     * {@code "Mask[T.TT...]"}, reporting the mask bit
     * settings (as 'T' or '.' characters) in lane order.
     *
     * @return a string of the form {@code "Mask[T.TT...]"}
     */
    @Override
    public final String toString() {
        StringBuilder buf = new StringBuilder(length());
        buf.append("Mask[");
        for (boolean isSet : toArray()) {
            buf.append(isSet ? 'T' : '.');
        }
        return buf.append(']').toString();
    }

    /**
     * Indicates whether this mask is identical to some other object.
     * Two masks are identical only if they have the same species
     * and same source indexes, in the same order.
     *
     * @return whether this vector is identical to some other object
     * @see #eq
     */
    @Override
    public final boolean equals(Object obj) {
        if (obj instanceof VectorMask) {
            VectorMask<?> that = (VectorMask<?>) obj;
            if (this.vectorSpecies().equals(that.vectorSpecies())) {
                @SuppressWarnings("unchecked")
                VectorMask<E> that2 = (VectorMask<E>) that;
                return this.eq(that2).allTrue();
            }
        }
        return false;
    }

    /**
     * Returns a hash code value for the mask,
     * based on the mask bit settings and the vector species.
     *
     * @return  a hash code value for this mask
     */
    @Override
    public final int hashCode() {
        return Objects.hash(vectorSpecies(), Arrays.hashCode(toArray()));
    }

    // ==== JROSE NAME CHANGES ====

    // TYPE CHANGED
    // * toVector() return type is Vector<?> not Vector<E>
    // ADDED
    // * indexInRange(int,int,int) (SIMD range check, no overflow)
    // * fromLong(VectorSpecies, long) (inverse of toLong)
    // * check(VectorSpecies) (static type-safety check)
    // * toString(), equals(Object), hashCode() (documented)
    // * added <E> (not <?>) to toVector

}
