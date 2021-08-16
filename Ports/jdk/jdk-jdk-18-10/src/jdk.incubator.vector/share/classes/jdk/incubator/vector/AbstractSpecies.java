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
import jdk.internal.vm.annotation.Stable;
import java.nio.ByteOrder;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.function.Function;
import java.util.function.IntUnaryOperator;

abstract class AbstractSpecies<E> extends jdk.internal.vm.vector.VectorSupport.VectorSpecies<E>
                                  implements VectorSpecies<E> {
    @Stable
    final VectorShape vectorShape;
    @Stable
    final LaneType laneType;
    @Stable
    final int laneCount;
    @Stable
    final int laneCountLog2P1;
    @Stable
    final Class<? extends AbstractVector<E>> vectorType;
    @Stable
    final Class<? extends AbstractMask<E>> maskType;
    @Stable
    final Function<Object, ? extends AbstractVector<E>> vectorFactory;

    @Stable
    final VectorShape indexShape;
    @Stable
    final int maxScale, minScale;
    @Stable
    final int vectorBitSize, vectorByteSize;

    AbstractSpecies(VectorShape vectorShape,
                    LaneType laneType,
                    Class<? extends AbstractVector<E>> vectorType,
                    Class<? extends AbstractMask<E>> maskType,
                    Function<Object, ? extends AbstractVector<E>> vectorFactory) {
        this.vectorShape = vectorShape;
        this.laneType = laneType;
        this.vectorType = vectorType;
        this.maskType = maskType;
        this.vectorFactory = vectorFactory;

        // derived values:
        int bitSize = vectorShape.vectorBitSize();
        int byteSize = bitSize / Byte.SIZE;
        assert(byteSize * 8 == bitSize);
        this.vectorBitSize = bitSize;
        this.vectorByteSize = byteSize;
        int elementSize = laneType.elementSize;
        this.laneCount = bitSize / elementSize;
        assert(laneCount > 0);  // could be 1 for mono-vector (double in v64)
        this.laneCountLog2P1 = Integer.numberOfTrailingZeros(laneCount) + 1;

        // Note:  The shape might be the max-shape,
        // if there is no vector this large.
        int indexBitSize = Integer.SIZE * laneCount;
        this.indexShape = VectorShape.forIndexBitSize(indexBitSize, elementSize);

        // What are the largest and smallest scale factors that,
        // when multiplied times the elements in [0..VLENGTH],
        // inclusive, do not overflow the ETYPE?
        int precision = laneType.elementPrecision;
        if (precision >= Integer.SIZE) {
            // No overflow possible from int*int.
            this.maxScale = Integer.MAX_VALUE;
            this.minScale = Integer.MIN_VALUE;
        } else {
            boolean isfp = (laneType.elementKind == 'F');
            long x = laneCount;
            long maxScale = ((1L << precision)-(isfp?0:1)) / x;
            long minScale = (-1L << precision) / x;
            this.maxScale = (int) maxScale;
            this.minScale = (int) minScale;
        }
    }

    @Stable //lazy JIT constant
    AbstractSpecies<Integer> indexSpecies;

    @Stable //lazy JIT constant
    AbstractShuffle<Byte> swapBytesShuffle;

    @Stable //lazy JIT constant
    AbstractVector<E> dummyVector;

    @Override
    @ForceInline
    public final int length() {
        return laneCount;
    }

    // Inside the implementation we use the more descriptive
    // term laneCount:

    /*package-private*/
    @ForceInline
    final int laneCount() {
        return laneCount;
    }

    /*package-private*/
    @ForceInline
    final int laneCountLog2() {
        return laneCountLog2P1 - 1;  // subtract one from stable value
    }

    @Override
    @ForceInline
    @SuppressWarnings("unchecked")
    //NOT FINAL: SPECIALIZED
    public Class<E> elementType() {
        return (Class<E>) laneType.elementType;
    }

    // FIXME: appeal to general method (see https://bugs.openjdk.java.net/browse/JDK-6176992)
    // replace usages of this method and remove
    @ForceInline
    @SuppressWarnings("unchecked")
    //NOT FINAL: SPECIALIZED
    Class<E> genericElementType() {
        return (Class<E>) laneType.genericElementType;
    }

    @Override
    @ForceInline
    //NOT FINAL: SPECIALIZED
    public Class<? extends AbstractVector<E>> vectorType() {
        return vectorType;
    }

    @Override
    @ForceInline
    public final Class<? extends AbstractMask<E>> maskType() {
        return maskType;
    }

    @Override
    @ForceInline
    public final int elementSize() {
        return laneType.elementSize;
    }

    /*package-private*/
    @ForceInline
    final int elementByteSize() {
        return laneType.elementSize / Byte.SIZE;
    }

    @Override
    @ForceInline
    public final VectorShape vectorShape() {
        return vectorShape;
    }

    @ForceInline
    /*package-private*/
    final VectorShape indexShape() {
        return indexShape;
    }

    @Override
    @ForceInline
    public final int vectorBitSize() {
        return vectorBitSize;
    }

    @Override
    @ForceInline
    public final int vectorByteSize() {
        return vectorByteSize;
    }

    @Override
    @ForceInline
    public final int loopBound(int length) {
        return VectorIntrinsics.roundDown(length, laneCount);
    }

    @Override
    @ForceInline
    public final VectorMask<E> indexInRange(int offset, int limit) {
        return maskAll(true).indexInRange(offset, limit);
    }

    @Override
    @ForceInline
    public final <F> VectorSpecies<F> withLanes(Class<F> newType) {
        return withLanes(LaneType.of(newType)).check(newType);
    }

    @ForceInline
    /*package-private*/
    final
    AbstractSpecies<?> withLanes(LaneType newType) {
        if (newType == laneType)  return this;
        return findSpecies(newType, vectorShape);
    }

    @ForceInline
    /*package-private*/
    AbstractSpecies<?> asIntegral() {
        return withLanes(laneType.asIntegral());
    }

    @ForceInline
    /*package-private*/
    AbstractSpecies<?> asFloating() {
        return withLanes(laneType.asFloating());
    }

    @Override
    @ForceInline
    @SuppressWarnings("unchecked")
    public final VectorSpecies<E> withShape(VectorShape newShape) {
        if (newShape == vectorShape)  return this;
        return (VectorSpecies<E>) findSpecies(laneType, newShape);
    }

    @ForceInline
    /*package-private*/
    AbstractSpecies<Integer> indexSpecies() {
        // This JITs to a constant value:
        AbstractSpecies<Integer> sp = indexSpecies;
        if (sp != null)  return sp;
        return indexSpecies = findSpecies(LaneType.INT, indexShape).check0(int.class);
    }

    @ForceInline
    /*package-private*/
    @SuppressWarnings("unchecked")
    AbstractSpecies<Byte> byteSpecies() {
        // This JITs to a constant value:
        return (AbstractSpecies<Byte>) withLanes(LaneType.BYTE);
    }

    @ForceInline
    /*package-private*/
    AbstractShuffle<Byte> swapBytesShuffle() {
        // This JITs to a constant value:
        AbstractShuffle<Byte> sh = swapBytesShuffle;
        if (sh != null)  return sh;
        return swapBytesShuffle = makeSwapBytesShuffle();
    }
    private AbstractShuffle<Byte> makeSwapBytesShuffle() {
        int vbytes = vectorByteSize();
        int lbytes = elementByteSize();
        int[] sourceIndexes = new int[vbytes];
        for (int i = 0; i < vbytes; i++) {
            sourceIndexes[i] = i ^ (lbytes-1);
        }
        return (AbstractShuffle<Byte>)
            VectorShuffle.fromValues(byteSpecies(), sourceIndexes);
    }
    /*package-private*/
    abstract Vector<E> fromIntValues(int[] values);

    /**
     * Do not use a dummy except to call methods on it when you don't
     * care about the lane values.  The main benefit of it is to
     * populate the type profile, which then allows the JIT to derive
     * constant values for dummy.species(), the current species, and
     * then for all of its attributes: ETYPE, VLENGTH, VSHAPE, etc.
     */
    @ForceInline
    /*package-private*/
    AbstractVector<E> dummyVector() {
        // This JITs to a constant value:
        AbstractVector<E> dummy = dummyVector;
        if (dummy != null)  return dummy;
        // The rest of this computation is probably not JIT-ted.
        return makeDummyVector();
    }
    private AbstractVector<E> makeDummyVector() {
        Object za = Array.newInstance(elementType(), laneCount);
        return dummyVector = vectorFactory.apply(za);
        // This is the only use of vectorFactory.
        // All other factory requests are routed
        // through the dummy vector.
    }

    /**
     * Build a mask by directly calling its constructor.
     * It is an error if the array is aliased elsewhere.
     */
    @ForceInline
    /*package-private*/
    AbstractMask<E> maskFactory(boolean[] bits) {
        return dummyVector().maskFromArray(bits);
    }

    public final
    @Override
    @ForceInline
    VectorShuffle<E> shuffleFromArray(int[] sourceIndexes, int offset) {
        return dummyVector().shuffleFromArray(sourceIndexes, offset);
    }

    public final
    @Override
    @ForceInline
    VectorShuffle<E> shuffleFromValues(int... sourceIndexes) {
        return dummyVector().shuffleFromArray(sourceIndexes, 0);
    }

    public final
    @Override
    @ForceInline
    VectorShuffle<E> shuffleFromOp(IntUnaryOperator fn) {
        return dummyVector().shuffleFromOp(fn);
    }

    public final
    @Override
    @ForceInline
    VectorShuffle<E> iotaShuffle(int start, int step, boolean wrap) {
        AbstractShuffle<E> res;
        if (start == 0 && step == 1)
            return dummyVector().iotaShuffle();
        else
            return dummyVector().iotaShuffle(start, step, wrap);
    }

    @ForceInline
    @Override
    public final Vector<E> fromByteArray(byte[] a, int offset, ByteOrder bo) {
        return dummyVector()
            .fromByteArray0(a, offset)
            .maybeSwap(bo);
    }

    @Override
    public VectorMask<E> loadMask(boolean[] bits, int offset) {
        return VectorMask.fromArray(this, bits, offset);
    }

    // Define zero and iota when we know the ETYPE and VSHAPE.
    public abstract AbstractVector<E> zero();
    /*package-private*/ abstract AbstractVector<E> iota();

    // Constructing vectors from raw bits.

    /*package-private*/
    abstract long longToElementBits(long e);

    /*package-private*/
    abstract AbstractVector<E> broadcastBits(long bits);

    /*package-private*/
    final IllegalArgumentException badElementBits(long iv, Object cv) {
        String msg = String.format("Vector creation failed: "+
                                   "value %s cannot be represented in ETYPE %s"+
                                   "; result of cast is %s",
                                   iv,
                                   elementType(),
                                   cv);
        return new IllegalArgumentException(msg);
    }

    /*package-private*/
    static
    final IllegalArgumentException badArrayBits(Object iv,
                                                boolean isInt,
                                                long cv) {
        String msg = String.format("Array creation failed: "+
                                   "lane value %s cannot be represented in %s"+
                                   "; result of cast is %s",
                                   iv,
                                   (isInt ? "int" : "long"),
                                   cv);
        return new IllegalArgumentException(msg);
    }

    /*package-private*/
    Object iotaArray() {
        // Create an iota array.  It's OK if this is really slow,
        // because it happens only once per species.
        Object ia = Array.newInstance(laneType.elementType,
                                      laneCount);
        assert(ia.getClass() == laneType.arrayType);
        checkValue(laneCount-1);  // worst case
        for (int i = 0; i < laneCount; i++) {
            if ((byte)i == i)
                Array.setByte(ia, i, (byte)i);
            else if ((short)i == i)
                Array.setShort(ia, i, (short)i);
            else
                Array.setInt(ia, i, i);
            assert(Array.getDouble(ia, i) == i);
        }
        return ia;
    }

    @ForceInline
    /*package-private*/
    void checkScale(int scale) {
        if (scale > 0) {
            if (scale <= maxScale)  return;
        } else { // scale <= 0
            if (scale >= minScale)  return;
        }
        throw checkScaleFailed(scale);
    }
    private IllegalArgumentException checkScaleFailed(int scale) {
        String msg = String.format("%s: cannot represent VLENGTH*%d",
                                   this, scale);
        return new IllegalArgumentException(msg);
    }

    /*package-private*/
    interface RVOp {
        long apply(int i);  // supply raw element bits
    }

    /*package-private*/
    abstract AbstractVector<E> rvOp(RVOp f);

    /*package-private*/
    interface FOpm {
        boolean apply(int i);
    }

    AbstractMask<E> opm(FOpm f) {
        boolean[] res = new boolean[laneCount];
        for (int i = 0; i < res.length; i++) {
            res[i] = f.apply(i);
        }
        return dummyVector().maskFromArray(res);
    }

    @Override
    @ForceInline
    public final
    <F> VectorSpecies<F> check(Class<F> elementType) {
        return check0(elementType);
    }

    @ForceInline
    @SuppressWarnings("unchecked")
    /*package-private*/ final
    <F> AbstractSpecies<F> check0(Class<F> elementType) {
        if (elementType != this.elementType()) {
            throw AbstractSpecies.checkFailed(this, elementType);
        }
        return (AbstractSpecies<F>) this;
    }

    @ForceInline
    /*package-private*/
    AbstractSpecies<E> check(LaneType laneType) {
        if (laneType != this.laneType) {
            throw AbstractSpecies.checkFailed(this, laneType);
        }
        return this;
    }


    @Override
    @ForceInline
    public int partLimit(VectorSpecies<?> toSpecies, boolean lanewise) {
        AbstractSpecies<?> rsp = (AbstractSpecies<?>) toSpecies;
        int inSizeLog2 = this.vectorShape.vectorBitSizeLog2;
        int outSizeLog2 = rsp.vectorShape.vectorBitSizeLog2;
        if (lanewise) {
            inSizeLog2 += (rsp.laneType.elementSizeLog2 -
                           this.laneType.elementSizeLog2);
        }
        int diff = (inSizeLog2 - outSizeLog2);
        // Let's try a branch-free version of this.
        int sign = (diff >> -1);
        //d = Math.abs(diff);
        //d = (sign == 0 ? diff : sign == -1 ? 1 + ~diff);
        int d = (diff ^ sign) - sign;
        // Compute sgn(diff) << abs(diff), but replace 1 by 0.
        return ((sign | 1) << d) & ~1;
    }

    /**
     * Helper for throwing CheckCastExceptions,
     * used by the various Vector*.check(*) methods.
     */
    /*package-private*/
    static ClassCastException checkFailed(Object what, Object required) {
        // Find a species for the thing that's failing.
        AbstractSpecies<?> whatSpecies = null;
        String where;
        if (what instanceof VectorSpecies) {
            whatSpecies = (AbstractSpecies<?>) what;
            where = whatSpecies.toString();
        } else if (what instanceof Vector) {
            whatSpecies = (AbstractSpecies<?>) ((Vector<?>) what).species();
            where = "a Vector<"+whatSpecies.genericElementType()+">";
        } else if (what instanceof VectorMask) {
            whatSpecies = (AbstractSpecies<?>) ((VectorMask<?>) what).vectorSpecies();
            where = "a VectorMask<"+whatSpecies.genericElementType()+">";
        } else if (what instanceof VectorShuffle) {
            whatSpecies = (AbstractSpecies<?>) ((VectorShuffle<?>) what).vectorSpecies();
            where = "a VectorShuffle<"+whatSpecies.genericElementType()+">";
        } else {
            where = what.toString();
        }

        Object found = null;
        if (whatSpecies != null) {
            if (required instanceof VectorSpecies) {
                // required is a VectorSpecies; found the wrong species
                found = whatSpecies;
            } else if (required instanceof Vector) {
                // same VectorSpecies required; found the wrong species
                found = whatSpecies;
                required = ((Vector<?>)required).species();
            } else if (required instanceof Class) {
                // required is a Class; found the wrong ETYPE
                Class<?> requiredClass = (Class<?>) required;
                LaneType requiredType = LaneType.forClassOrNull(requiredClass);
                found = whatSpecies.elementType();
                if (requiredType == null) {
                    required = required + " (not a valid lane type)";
                } else if (!requiredClass.isPrimitive()) {
                    required = required + " (should be " + requiredType + ")";
                }
            } else if (required instanceof LaneType) {
                // required is a LaneType; found the wrong ETYPE
                required = ((LaneType) required).elementType;
                found = whatSpecies.elementType();
            } else if (required instanceof Integer) {
                // required is a length; species has wrong VLENGTH
                required = required + " lanes";
                found = whatSpecies.length();
            }
        }
        if (found == null)  found = "bad value";

        String msg = where+": required "+required+" but found "+found;
        return new ClassCastException(msg);
    }

    private static final @Stable AbstractSpecies<?>[][] CACHES
        = new AbstractSpecies<?>[LaneType.SK_LIMIT][VectorShape.SK_LIMIT];

    // Helper functions for finding species:

    /*package-private*/
    @ForceInline
    static <E>
    AbstractSpecies<E> findSpecies(Class<E> elementType,
                                   LaneType laneType,
                                   VectorShape shape) {
        assert(elementType == laneType.elementType);
        return findSpecies(laneType, shape).check0(elementType);
    }

    /*package-private*/
    @ForceInline
    static
    AbstractSpecies<?> findSpecies(LaneType laneType,
                                   VectorShape shape) {
        // The JIT can see into this cache.
        // Therefore it is useful to arrange for constant
        // arguments to this method.  If the cache
        // is full when the JIT runs, the cache item becomes
        // a compile-time constant.  And then all the @Stable
        // fields of the AbstractSpecies are also constants.
        AbstractSpecies<?> s = CACHES[laneType.switchKey][shape.switchKey];
        if (s != null)  return s;
        return computeSpecies(laneType, shape);
    }

    private static
    AbstractSpecies<?> computeSpecies(LaneType laneType,
                                      VectorShape shape) {
        AbstractSpecies<?> s = null;
        // enum-switches don't optimize properly JDK-8161245
        switch (laneType.switchKey) {
        case LaneType.SK_FLOAT:
            s = FloatVector.species(shape); break;
        case LaneType.SK_DOUBLE:
            s = DoubleVector.species(shape); break;
        case LaneType.SK_BYTE:
            s = ByteVector.species(shape); break;
        case LaneType.SK_SHORT:
            s = ShortVector.species(shape); break;
        case LaneType.SK_INT:
            s = IntVector.species(shape); break;
        case LaneType.SK_LONG:
            s = LongVector.species(shape); break;
        }
        if (s == null) {
            // NOTE: The result of this method is guaranteed to be
            // non-null.  Later calls to ".check" also ensure this.
            // If this method hits a NPE, it is because a helper
            // method EVector.species() has returned a null value, and
            // that is because a SPECIES_X static constant has not yet
            // been initialized.  And that, in turn, is because
            // somebody is calling this method way too early during
            // bootstrapping.
            throw new AssertionError("bootstrap problem");
        }
        assert(s.laneType == laneType) : s + "!=" + laneType;
        assert(s.vectorShape == shape) : s + "!=" + shape;
        CACHES[laneType.switchKey][shape.switchKey] = s;
        return s;
    }

    @Override
    public final String toString() {
        return "Species["+laneType+", "+laneCount+", "+vectorShape+"]";
    }

    @Override
    public final boolean equals(Object obj) {
        if (obj instanceof AbstractSpecies) {
            AbstractSpecies<?> that = (AbstractSpecies<?>) obj;
            return (this.laneType == that.laneType &&
                    this.laneCount == that.laneCount &&
                    this.vectorShape == that.vectorShape);
        }
        return this == obj;
    }

    /**
     * Returns a hash code value for the shuffle,
     * based on the lane source indexes and the vector species.
     *
     * @return  a hash code value for this shuffle
     */
    @Override
    public final int hashCode() {
        int[] a = { laneType.ordinal(), laneCount, vectorShape.ordinal() };
        return Arrays.hashCode(a);
    }
}
