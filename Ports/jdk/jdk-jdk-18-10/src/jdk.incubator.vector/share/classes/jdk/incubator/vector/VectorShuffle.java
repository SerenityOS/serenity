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

import jdk.internal.vm.annotation.ForceInline;
import java.util.Objects;
import java.util.Arrays;
import java.util.function.IntUnaryOperator;

/**
 * A {@code VectorShuffle} represents an ordered immutable sequence of
 * {@code int} values called <em>source indexes</em>, where each source
 * index numerically selects a source lane from a compatible {@link Vector}.
 * <p>
 * A {@code VectorShuffle} and {@code Vector} of the same
 * <a href="Vector.html#ETYPE">element type</a>
 * ({@code ETYPE}) and {@link VectorShape shape} have the same number of lanes,
 * and are therefore compatible (specifically, their {@link #vectorSpecies()
 * vector species} are compatible).
 * <p>
 * A shuffle is applied to a (compatible) source vector with the
 * {@link Vector#rearrange(VectorShuffle) rearrange}
 * method.
 * <p>
 * A shuffle has a lane structure derived from its vector
 * species, but it stores lane indexes, as {@code int}s,
 * rather than lane values.
 * <p>
 * This method gathers lane values by random access to the source
 * vector, selecting lanes by consulting the source indexes.  If a
 * source index appears more than once in a shuffle, then the selected
 * lane's value is copied more than once into the result.  If a
 * particular lane is never selected by a source index, that lane's
 * value is ignored.  The resulting vector contains all the source
 * lane values selected by the source indexes of the shuffle.  The
 * resulting lane values are ordered according to the shuffle's source
 * indexes, not according to the original vector's lane order.
 * <p>
 * Each shuffle has a {@link #vectorSpecies() vectorSpecies()}
 * property which determines the compatibility of vectors the shuffle
 * operates on.  This ensures that the {@link #length() length()} of a
 * shuffle is always equal to the {@linkplain Vector#length() VLENGTH}
 * of any vector it operates on.
 *
 * The element type and shape of the shuffle's species are not
 * directly relevant to the behavior of the shuffle.  Shuffles can
 * easily be {@linkplain #cast(VectorSpecies) converted} to other lane
 * types, as long as the lane count stays constant.
 *
 * <p>
 * In its internal state, a shuffle always holds integral values
 * in a narrow range from {@code [-VLENGTH..VLENGTH-1]}.
 * The positive numbers are self-explanatory; they are lane
 * numbers applied to any source vector.  The negative numbers,
 * when present, are a sign that the shuffle was created from
 * a raw integer value which was not a valid lane index.
 * <p>
 * An invalid source index, represented in a shuffle by a
 * negative number, is called an <em>exceptional index</em>.
 * <p>
 * Exceptional indexes are processed in a variety of ways:
 * <ul>
 *
 * <li> Unless documented otherwise, shuffle-using methods will throw
 * {@code ArrayIndexOutOfBoundsException} when a lane is processed by
 * an exceptional index.
 *
 * <li> When an invalid source index (negative or not) is first loaded
 * into a shuffle, it is partially normalized to the negative range of
 * {@code [-VLENGTH..-1]} as if by {@link #wrapIndex(int) wrapIndex()}.
 *
 * This treatment of exceptional indexes is called <em>partial
 * wrapping</em>, because it preserves the distinction between normal
 * and exceptional indexes, while wrapping them into adjacent ranges
 * of positive and non-positive numbers.  A partially wrapped index
 * can later on be fully wrapped into the positive range by adding
 * a final offset of {@code VLENGTH}.
 *
 * <li> In some applications, exceptional indexes used to "steer"
 * access to a second source vector.  In those cases, the exception
 * index values, which are in the range {@code [-VLENGTH..-1]}, are
 * cycled up to the valid range {@code [0..VLENGTH-1]} and used on the
 * second source vector.
 *
 * <li> When a shuffle is cast from another shuffle species with a
 * smaller {@code VLENGTH}, all indexes are re-validated against the
 * new {@code VLENGTH}, and some may be converted to exceptional
 * indexes.  In any case, shuffle casting never converts exceptional
 * indexes to normal ones.
 *
 * </ul>

 * <h2>Value-based classes and identity operations</h2>
 *
 * {@code VectorShuffle}, along with {@code Vector} is a
 * <a href="{@docRoot}/java.base/java/lang/doc-files/ValueBased.html">value-based</a>
 * class.  Identity-sensitive operations such as {@code ==}
 * may yield unpredictable results, or reduced performance.
 *
 * Also, vector shuffle objects can be stored in locals and parameters and as
 * {@code static final} constants, but storing them in other Java
 * fields or in array elements, while semantically valid, may incur
 * performance penalties.
 *
 * Finally, vector shuffles should not be computed in loops, when
 * possible, but instead should be stored in loop-invariant locals or
 * as {@code static final} constants.
 *
 * @param <E> the boxed version of {@code ETYPE},
 *           the element type of a vector
 */
@SuppressWarnings("exports")
public abstract class VectorShuffle<E> extends jdk.internal.vm.vector.VectorSupport.VectorShuffle<E> {
    VectorShuffle(byte[] reorder) {
        super(reorder);
    }

    /**
     * Returns the species of this shuffle.
     *
     * @return the species of this shuffle
     */
    public abstract VectorSpecies<E> vectorSpecies();

    /**
     * Returns the number of lanes processed by this shuffle.
     * This is the same as the {@code VLENGTH} of any vector
     * it operates on.
     *
     * @return the number of shuffle lanes
     */
    @ForceInline
    public final int length() {
        AbstractSpecies<E> vspecies = (AbstractSpecies<E>) vectorSpecies();
        return vspecies.laneCount();
    }

    /**
     * Converts this shuffle to a shuffle of the given species of
     * element type {@code F}.
     *
     * The various lane source indexes are unmodified.  Exceptional
     * source indexes remain exceptional and valid indexes remain
     * valid.
     *
     * @param species the species of desired shuffle
     * @param <F> the boxed element type of the species
     * @return a shuffle converted by shape and element type
     * @throws IllegalArgumentException if this shuffle length and the
     *         species length differ
     */
    public abstract <F> VectorShuffle<F> cast(VectorSpecies<F> species);

    /**
     * Checks that this shuffle has the given species,
     * and returns this shuffle unchanged.
     * The effect is similar to this pseudocode:
     * {@code species == vectorSpecies()
     *        ? this
     *        : throw new ClassCastException()}.
     *
     * @param species the required species
     * @param <F> the boxed element type of the required species
     * @return the same shuffle
     * @throws ClassCastException if the shuffle species is wrong
     * @see Vector#check(Class)
     * @see Vector#check(VectorSpecies)
     */
    public abstract <F> VectorShuffle<F> check(VectorSpecies<F> species);

    /**
     * Validation function for lane indexes which may be out of the
     * valid range of {@code [0..VLENGTH-1]}.  If {@code index} is in
     * this range, it is returned unchanged.
     *
     * Otherwise, an {@code IndexOutOfBoundsException} is thrown.
     *
     * @param index the lane index
     * @return {@code index}
     * @throws IndexOutOfBoundsException if the {@code index} is
     *         not less than {@code VLENGTH}, or is negative
     * @see #wrapIndex(int)
     * @see #checkIndexes()
     */
    public abstract int checkIndex(int index);

    /**
     * Validation function for lane indexes which may be out of the
     * valid range of {@code [0..VLENGTH-1]}.
     *
     * The {@code index} is forced into this range by adding or
     * subtracting a suitable multiple of {@code VLENGTH}.
     * Specifically, the index is reduced into the required range
     * by computing the value of {@code length-floor}, where
     * {@code floor=vectorSpecies().loopBound(length)} is the
     * next lower multiple of {@code VLENGTH}.
     * As long as {@code VLENGTH} is a power of two, then the
     * reduced index also equal to {@code index & (VLENGTH - 1)}.
     *
     * @param index the lane index
     * @return {@code index}, adjusted to the range {@code [0..VLENGTH-1}}
     *         by an appropriate multiple of {@code VLENGTH}
     * @see VectorSpecies#loopBound(int)
     * @see #checkIndex(int)
     * @see #wrapIndexes()
     */
    public abstract int wrapIndex(int index);

    /**
     * Apply the {@link #checkIndex(int) checkIndex()} validation
     * function to all lanes, throwing
     * {@code IndexOutOfBoundsException} if there are any exceptional
     * indexes in this shuffle.
     *
     * @return the current shuffle, unchanged
     * @throws IndexOutOfBoundsException if any lanes in this shuffle
     *         contain exceptional indexes
     * @see #checkIndex(int)
     * @see #wrapIndexes()
     */
    public abstract VectorShuffle<E> checkIndexes();

    /**
     * Apply the {@link #wrapIndex(int) wrapIndex()} validation
     * function to all lanes, replacing any exceptional indexes
     * with wrapped normal indexes.
     *
     * @return the current shuffle, with all exceptional indexes wrapped
     * @see #wrapIndex(int)
     * @see #checkIndexes()
     */
    public abstract VectorShuffle<E> wrapIndexes();

    /**
     * Find all lanes containing valid indexes (non-negative values)
     * and return a mask where exactly those lanes are set.
     *
     * @return a mask of lanes containing valid source indexes
     * @see #checkIndexes()
     */
    public abstract VectorMask<E> laneIsValid();

    /**
     * Creates a shuffle for a given species from
     * a series of source indexes.
     *
     * <p> For each shuffle lane, where {@code N} is the shuffle lane
     * index, the {@code N}th index value is validated
     * against the species {@code VLENGTH}, and (if invalid)
     * is partially wrapped to an exceptional index in the
     * range {@code [-VLENGTH..-1]}.
     *
     * @param species shuffle species
     * @param sourceIndexes the source indexes which the shuffle will draw from
     * @param <E> the boxed element type
     * @return a shuffle where each lane's source index is set to the given
     *         {@code int} value, partially wrapped if exceptional
     * @throws IndexOutOfBoundsException if {@code sourceIndexes.length != VLENGTH}
     * @see VectorSpecies#shuffleFromValues(int...)
     */
    @ForceInline
    public static <E> VectorShuffle<E> fromValues(VectorSpecies<E> species,
                                                  int... sourceIndexes) {
        AbstractSpecies<E> vsp = (AbstractSpecies<E>) species;
        VectorIntrinsics.requireLength(sourceIndexes.length, vsp.laneCount());
        return vsp.shuffleFromArray(sourceIndexes, 0);
    }

    /**
     * Creates a shuffle for a given species from
     * an {@code int} array starting at an offset.
     *
     * <p> For each shuffle lane, where {@code N} is the shuffle lane
     * index, the array element at index {@code offset + N} is validated
     * against the species {@code VLENGTH}, and (if invalid)
     * is partially wrapped to an exceptional index in the
     * range {@code [-VLENGTH..-1]}.
     *
     * @param species shuffle species
     * @param sourceIndexes the source indexes which the shuffle will draw from
     * @param offset the offset into the array
     * @param <E> the boxed element type
     * @return a shuffle where each lane's source index is set to the given
     *         {@code int} value, partially wrapped if exceptional
     * @throws IndexOutOfBoundsException if {@code offset < 0}, or
     *         {@code offset > sourceIndexes.length - VLENGTH}
     * @see VectorSpecies#shuffleFromArray(int[], int)
     */
    @ForceInline
    public static <E> VectorShuffle<E> fromArray(VectorSpecies<E> species, int[] sourceIndexes, int offset) {
        AbstractSpecies<E> vsp = (AbstractSpecies<E>) species;
        return vsp.shuffleFromArray(sourceIndexes, offset);
    }

    /**
     * Creates a shuffle for a given species from
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
     *   int[] a = new int[species.length()];
     *   for (int i = 0; i < a.length; i++) {
     *       a[i] = fn.applyAsInt(i);
     *   }
     *   return VectorShuffle.fromArray(a, 0);
     * }</pre>
     *
     * @param species shuffle species
     * @param fn the lane index mapping function
     * @param <E> the boxed element type
     * @return a shuffle of mapped indexes
     * @see VectorSpecies#shuffleFromOp(IntUnaryOperator)
     */
    @ForceInline
    public static <E> VectorShuffle<E> fromOp(VectorSpecies<E> species, IntUnaryOperator fn) {
        AbstractSpecies<E> vsp = (AbstractSpecies<E>) species;
        return vsp.shuffleFromOp(fn);
    }

    /**
     * Creates a shuffle using source indexes set to sequential
     * values starting from {@code start} and stepping
     * by the given {@code step}.
     * <p>
     * This method returns the value of the expression
     * {@code VectorShuffle.fromOp(species, i -> R(start + i * step))},
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
     * @param species shuffle species
     * @param start the starting value of the source index sequence
     * @param step the difference between adjacent source indexes
     * @param wrap whether to wrap resulting indexes
     * @param <E> the boxed element type
     * @return a shuffle of sequential lane indexes, possibly wrapped
     * @see VectorSpecies#iotaShuffle(int,int,boolean)
     */
    @ForceInline
    public static <E> VectorShuffle<E> iota(VectorSpecies<E> species,
                                            int start, int step,
                                            boolean wrap) {
        AbstractSpecies<E> vsp = (AbstractSpecies<E>) species;
        return vsp.iotaShuffle(start, step, wrap);
    }

    /**
     * Creates a shuffle which will zip together two vectors,
     * alternatively selecting lanes from one or the other.
     * The logical result of a zip is twice the size of either
     * input, and so the
     * <a href="Vector.html#expansion">expanded result</a>
     * is broken into two physical parts, selected by
     * a part number.
     * For example, zipping two vectors {@code [a,b,c,d]} and
     * {@code [1,2,3,4]} will yield the expanded logical result
     * {@code [a,1,b,2,c,3,d,4]} which must be obtained in two
     * parts, {@code [a,1,b,2]} and {@code [c,3,d,4]}.
     * <p>
     * This method returns the value of the expression
     * {@code VectorShuffle.fromOp(species, i -> i/2 + (i%2)*VLENGTH + P},
     * where {@code P} is {@code part*VLENGTH/2}.
     * <p>s
     * Note that the source indexes in the odd lanes of the shuffle
     * will be invalid indexes ({@code >= VLENGTH}, or {@code < 0}
     * after partial normalization), which will select from the second
     * vector.
     *
     * @param species the shuffle species
     * @param part the part number of the result (either zero or one)
     * @param <E> the boxed element type
     * @return a shuffle which zips two vectors into {@code 2*VLENGTH} lanes, returning the selected part
     * @throws ArrayIndexOutOfBoundsException if {@code part} is not zero or one
     * @see #makeUnzip(VectorSpecies, int)
     * @see Vector#rearrange(VectorShuffle,Vector)
     */
    public static <E> VectorShuffle<E> makeZip(VectorSpecies<E> species,
                                               int part) {
        if ((part & 1) != part)
            throw wrongPartForZip(part, false);
        AbstractSpecies<E> vsp = (AbstractSpecies<E>) species;
        return vsp.shuffleFromOp(i -> zipIndex(i, vsp.laneCount(), part));
    }

    /**
     * Creates a shuffle which will unzip the concatenation of two
     * vectors, alternatively storing input lanes into one or the
     * other output vector.
     * Since the logical result of an unzip is twice the size of
     * either input, the
     * <a href="Vector.html#expansion">expanded result</a>
     * is broken into two physical parts, selected by
     * a part number.
     * For example, unzipping two vectors {@code [a,1,b,2][c,3,d,4]}
     * will yield a result in two parts, {@code [a,b,c,d]} and
     * {@code [1,2,3,4]}.
     * <p>
     * This method returns the value of the expression
     * {@code VectorShuffle.fromOp(species, i -> i*2+part}.
     * <p>
     * Note that the source indexes in upper half of the shuffle will
     * be invalid indexes ({@code >= VLENGTH}, or {@code < 0} after
     * partial normalization), which will select from the second
     * vector.
     *
     * @param species the shuffle species
     * @param part the part number of the result (either zero or one)
     * @param <E> the boxed element type
     * @return a shuffle which unzips {@code 2*VLENGTH} lanes into two vectors, returning the selected part
     * @throws ArrayIndexOutOfBoundsException if {@code part} is not zero or one
     * @see #makeZip(VectorSpecies,int)
     * @see Vector#rearrange(VectorShuffle,Vector)
     */
    public static <E> VectorShuffle<E> makeUnzip(VectorSpecies<E> species,
                                                 int part) {
        if ((part & 1) != part)
            throw wrongPartForZip(part, true);
        AbstractSpecies<E> vsp = (AbstractSpecies<E>) species;
        return vsp.shuffleFromOp(i -> unzipIndex(i, vsp.laneCount(), part));
    }

    private static int zipIndex(int i, int vlen, int part) {
        int offset = part * ((vlen+1) >> 1);
        return (i/2) + ((i&1) * vlen) + offset;
    }
    private static int unzipIndex(int i, int vlen, int part) {
        return (i*2) + part;
    }
    private static ArrayIndexOutOfBoundsException
    wrongPartForZip(int part, boolean unzip) {
        String msg = String.format("bad part number %d for %szip",
                                   part, unzip ? "un" : "");
        return new ArrayIndexOutOfBoundsException(msg);
    }

    /**
     * Returns an {@code int} array containing the lane
     * source indexes of this shuffle.
     * <p>
     * This method behaves as if it stores
     * this shuffle into an allocated array
     * (using {@link #intoArray(int[], int) intoArray})
     * and returns that array as
     * follows:
     * <pre>{@code
     *   int[] a = new int[this.length()];
     *   VectorShuffle.intoArray(a, 0);
     *   return a;
     * }</pre>
     *
     * @apiNote Shuffle source indexes are always in the
     * range from {@code -VLENGTH} to {@code VLENGTH-1}.
     * A source index is exceptional if and only if it is
     * negative.
     *
     * @return an array containing the lane source indexes
     *         of this shuffle
     */
    public abstract int[] toArray();

    /**
     * Stores this shuffle into an {@code int} array starting at offset.
     * <p>
     * For each shuffle lane {@code N}, the lane source index
     * stored for that lane element is stored into the array
     * element {@code a[offset+N]}.
     *
     * @apiNote Shuffle source indexes are always in the
     * range from {@code -VLENGTH} to {@code VLENGTH-1}.
     *
     * @param a the array, of type {@code int[]}
     * @param offset the offset into the array
     * @throws IndexOutOfBoundsException if {@code offset < 0} or
     *         {@code offset > a.length - this.length()}
     */
    public abstract void intoArray(int[] a, int offset);

    /**
     * Converts this shuffle into a vector, creating a vector
     * of integral values corresponding to the lane source
     * indexes of the shuffle.
     * <p>
     * This method behaves as if it returns the result of creating a
     * vector given an {@code int} array obtained from this shuffle's
     * lane elements, as follows:
     * <pre>{@code
     *   int[] sa = this.toArray();
     *   $type$[] va = new $type$[a.length];
     *   for (int i = 0; i < a.length; i++) {
     *       va[i] = ($type$) sa[i];
     *   }
     *   return IntVector.fromArray(va, 0);
     * }</pre>
     *
     * @apiNote Shuffle source indexes are always in the
     * range from {@code -VLENGTH} to {@code VLENGTH-1}.
     * These values are converted to the {@code ETYPE}
     * of the resulting vector, even if it is a floating
     * point type.
     *
     * @return a vector representation of this shuffle
     */
    public abstract Vector<E> toVector();

    /**
     * Gets the {@code int} lane element at lane index {@code i}
     *
     * @param i the lane index
     * @return the {@code int} lane element at lane index {@code i}
     */
    public int laneSource(int i) { return toArray()[i]; }

    /**
     * Rearranges the lane elements of this shuffle selecting lane indexes
     * controlled by another shuffle.
     * <p>
     * For each lane of the specified shuffle, at lane index {@code N} with lane
     * element {@code I}, the lane element at {@code I} from this shuffle is
     * selected and placed into the resulting shuffle at {@code N}.
     *
     * @param s the shuffle controlling lane index selection
     * @return the rearrangement of the lane elements of this shuffle
     */
    public abstract VectorShuffle<E> rearrange(VectorShuffle<E> s);

    /**
     * Returns a string representation of this shuffle, of the form
     * {@code "Shuffle[0,1,2...]"}, reporting the source indexes
     * in lane order.
     *
     * @return a string of the form {@code "Shuffle[0,1,2...]"}
     */
    @Override
    public final String toString() {
        return "Shuffle" + Arrays.toString(toArray());
    }

    /**
     * Indicates whether this shuffle is identical to some other object.
     * Two shuffles are identical only if they have the same species
     * and same source indexes, in the same order.

     * @return whether this vector is identical to some other object
     */
    @Override
    public final boolean equals(Object obj) {
        if (obj instanceof VectorShuffle) {
            VectorShuffle<?> that = (VectorShuffle<?>) obj;
            if (this.vectorSpecies().equals(that.vectorSpecies())) {
                return Arrays.equals(this.toArray(), that.toArray());
            }
        }
        return false;
    }

    /**
     * Returns a hash code value for the shuffle,
     * based on the lane source indexes and the vector species.
     *
     * @return  a hash code value for this shuffle
     */
    @Override
    public final int hashCode() {
        return Objects.hash(vectorSpecies(), Arrays.hashCode(toArray()));
    }

    // ==== JROSE NAME CHANGES ====

    // ADDED:
    // * check(VectorSpecies) (static type-safety check)
    // * toString(), equals(Object), hashCode() (documented)
    // * checkIndex(int,byte), lane-index validator similar to loopBound()
    //FIXME: maybe add inversion, mask generation, index normalization

}
