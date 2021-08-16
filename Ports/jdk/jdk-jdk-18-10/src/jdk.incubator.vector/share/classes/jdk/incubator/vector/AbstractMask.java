/*
 * Copyright (c) 2017, 2021, Oracle and/or its affiliates. All rights reserved.
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

import static jdk.incubator.vector.VectorOperators.*;

abstract class AbstractMask<E> extends VectorMask<E> {
    AbstractMask(boolean[] bits) {
        super(bits);
    }

    /*package-private*/
    abstract boolean[] getBits();

    // Unary operator

    interface MUnOp {
        boolean apply(int i, boolean a);
    }

    abstract AbstractMask<E> uOp(MUnOp f);

    // Binary operator

    interface MBinOp {
        boolean apply(int i, boolean a, boolean b);
    }

    abstract AbstractMask<E> bOp(VectorMask<E> o, MBinOp f);

    /*package-private*/
    abstract AbstractSpecies<E> vspecies();

    @Override
    @ForceInline
    public final VectorSpecies<E> vectorSpecies() {
        return vspecies();
    }

    @Override
    public boolean laneIsSet(int i) {
        return getBits()[i];
    }

    @Override
    public long toLong() {
        // FIXME: This should be an intrinsic.
        if (length() > Long.SIZE) {
            throw new UnsupportedOperationException("too many lanes for one long");
        }
        long res = 0;
        long set = 1;
        boolean[] bits = getBits();
        for (int i = 0; i < bits.length; i++) {
            res = bits[i] ? res | set : res;
            set = set << 1;
        }
        return res;
    }

    @Override
    public void intoArray(boolean[] bits, int i) {
        System.arraycopy(getBits(), 0, bits, i, length());
    }

    @Override
    public boolean[] toArray() {
        return getBits().clone();
    }

    @Override
    @ForceInline
    @SuppressWarnings("unchecked")
    public
    <F> VectorMask<F> check(Class<F> elementType) {
        if (vectorSpecies().elementType() != elementType) {
            throw AbstractSpecies.checkFailed(this, elementType);
        }
        return (VectorMask<F>) this;
    }

    @Override
    @ForceInline
    @SuppressWarnings("unchecked")
    public
    <F> VectorMask<F> check(VectorSpecies<F> species) {
        if (species != vectorSpecies()) {
            throw AbstractSpecies.checkFailed(this, species);
        }
        return (VectorMask<F>) this;
    }

    @Override
    public VectorMask<E> andNot(VectorMask<E> m) {
        return and(m.not());
    }

    /*package-private*/
    static boolean anyTrueHelper(boolean[] bits) {
        // FIXME: Maybe use toLong() != 0 here.
        for (boolean i : bits) {
            if (i) return true;
        }
        return false;
    }

    /*package-private*/
    static boolean allTrueHelper(boolean[] bits) {
        // FIXME: Maybe use not().toLong() == 0 here.
        for (boolean i : bits) {
            if (!i) return false;
        }
        return true;
    }

    /*package-private*/
    static int trueCountHelper(boolean[] bits) {
        int c = 0;
        for (boolean i : bits) {
            if (i) c++;
        }
        return c;
    }

    /*package-private*/
    static int firstTrueHelper(boolean[] bits) {
        for (int i = 0; i < bits.length; i++) {
            if (bits[i])  return i;
        }
        return bits.length;
    }

    /*package-private*/
    static int lastTrueHelper(boolean[] bits) {
        for (int i = bits.length-1; i >= 0; i--) {
            if (bits[i])  return i;
        }
        return -1;
    }

    @Override
    @ForceInline
    public VectorMask<E> indexInRange(int offset, int limit) {
        int vlength = length();
        Vector<E> iota = vectorSpecies().zero().addIndex(1);
        VectorMask<E> badMask = checkIndex0(offset, limit, iota, vlength);
        return this.andNot(badMask);
    }

    /*package-private*/
    @ForceInline
    AbstractVector<E>
    toVectorTemplate() {
        AbstractSpecies<E> vsp = vspecies();
        Vector<E> zero = vsp.broadcast(0);
        Vector<E> mone = vsp.broadcast(-1);
        // -1 will result in the most significant bit being set in
        // addition to some or all other lane bits.
        // For integral types, *all* lane bits will be set.
        // The bits for -1.0 are like {0b10111*0000*}.
        // FIXME: Use a conversion intrinsic for this operation.
        // https://bugs.openjdk.java.net/browse/JDK-8225740
        return (AbstractVector<E>) zero.blend(mone, this);
    }

    /**
     * Test if a masked memory access at a given offset into an array
     * of the given length will stay within the array.
     * The per-lane offsets are iota*esize.
     */
    /*package-private*/
    @ForceInline
    void checkIndexByLane(int offset, int alength,
                          Vector<E> iota,
                          int esize) {
        if (VectorIntrinsics.VECTOR_ACCESS_OOB_CHECK == 0) {
            return;
        }
        // Although the specification is simple, the implementation is
        // tricky, because the value iota*esize might possibly
        // overflow.  So we calculate our test values as scalars,
        // clipping to the range [-1..VLENGTH], and test them against
        // the unscaled iota vector, whose values are in [0..VLENGTH-1].
        int vlength = length();
        VectorMask<E> badMask;
        if (esize == 1) {
            badMask = checkIndex0(offset, alength, iota, vlength);
        } else if (offset >= 0) {
            // Masked access to multi-byte lanes in byte array.
            // It could be aligned anywhere.
            int elemCount = Math.min(vlength, (alength - offset) / esize);
            badMask = checkIndex0(0, elemCount, iota, vlength);
        } else {
            // This requires a split test.
            int clipOffset = Math.max(offset, -(vlength * esize));
            int elemCount = Math.min(vlength, (alength - clipOffset) / esize);
            badMask = checkIndex0(0, elemCount, iota, vlength);
            clipOffset &= (esize - 1);  // power of two, so OK
            VectorMask<E> badMask2 = checkIndex0(clipOffset / esize, vlength,
                                                 iota, vlength);
            badMask = badMask.or(badMask2);
        }
        badMask = badMask.and(this);
        if (badMask.anyTrue()) {
            int badLane = badMask.firstTrue();
            throw ((AbstractMask<E>)badMask)
                   .checkIndexFailed(offset, badLane, alength, esize);
        }
    }

    private
    @ForceInline
    VectorMask<E> checkIndex0(int offset, int alength,
                              Vector<E> iota, int vlength) {
        // An active lane is bad if its number is greater than
        // alength-offset, since when added to offset it will step off
        // of the end of the array.  To avoid overflow when
        // converting, clip the comparison value to [0..vlength]
        // inclusive.
        int indexLimit = Math.max(0, Math.min(alength - offset, vlength));
        VectorMask<E> badMask =
            iota.compare(GE, iota.broadcast(indexLimit));
        if (offset < 0) {
            // An active lane is bad if its number is less than
            // -offset, because when added to offset it will then
            // address an array element at a negative index.  To avoid
            // overflow when converting, clip the comparison value at
            // vlength.  This specific expression works correctly even
            // when offset is Integer.MIN_VALUE.
            int firstGoodIndex = -Math.max(offset, -vlength);
            VectorMask<E> badMask2 =
                iota.compare(LT, iota.broadcast(firstGoodIndex));
            if (indexLimit >= vlength) {
                badMask = badMask2;  // 1st badMask is all true
            } else {
                badMask = badMask.or(badMask2);
            }
        }
        return badMask;
    }

    private IndexOutOfBoundsException checkIndexFailed(int offset, int lane,
                                                       int alength, int esize) {
        String msg = String.format("Masked range check failed: "+
                                   "vector mask %s out of bounds at "+
                                   "index %d+%d in array of length %d",
                                   this, offset, lane * esize, alength);
        if (esize != 1) {
            msg += String.format(" (each lane spans %d array elements)", esize);
        }
        throw new IndexOutOfBoundsException(msg);
    }

}
