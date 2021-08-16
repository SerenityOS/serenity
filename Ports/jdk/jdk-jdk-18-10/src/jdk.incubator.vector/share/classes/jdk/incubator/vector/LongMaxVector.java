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

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.IntUnaryOperator;

import jdk.internal.vm.annotation.ForceInline;
import jdk.internal.vm.vector.VectorSupport;

import static jdk.internal.vm.vector.VectorSupport.*;

import static jdk.incubator.vector.VectorOperators.*;

// -- This file was mechanically generated: Do not edit! -- //

@SuppressWarnings("cast")  // warning: redundant cast
final class LongMaxVector extends LongVector {
    static final LongSpecies VSPECIES =
        (LongSpecies) LongVector.SPECIES_MAX;

    static final VectorShape VSHAPE =
        VSPECIES.vectorShape();

    static final Class<LongMaxVector> VCLASS = LongMaxVector.class;

    static final int VSIZE = VSPECIES.vectorBitSize();

    static final int VLENGTH = VSPECIES.laneCount(); // used by the JVM

    static final Class<Long> ETYPE = long.class; // used by the JVM

    LongMaxVector(long[] v) {
        super(v);
    }

    // For compatibility as LongMaxVector::new,
    // stored into species.vectorFactory.
    LongMaxVector(Object v) {
        this((long[]) v);
    }

    static final LongMaxVector ZERO = new LongMaxVector(new long[VLENGTH]);
    static final LongMaxVector IOTA = new LongMaxVector(VSPECIES.iotaArray());

    static {
        // Warm up a few species caches.
        // If we do this too much we will
        // get NPEs from bootstrap circularity.
        VSPECIES.dummyVector();
        VSPECIES.withLanes(LaneType.BYTE);
    }

    // Specialized extractors

    @ForceInline
    final @Override
    public LongSpecies vspecies() {
        // ISSUE:  This should probably be a @Stable
        // field inside AbstractVector, rather than
        // a megamorphic method.
        return VSPECIES;
    }

    @ForceInline
    @Override
    public final Class<Long> elementType() { return long.class; }

    @ForceInline
    @Override
    public final int elementSize() { return Long.SIZE; }

    @ForceInline
    @Override
    public final VectorShape shape() { return VSHAPE; }

    @ForceInline
    @Override
    public final int length() { return VLENGTH; }

    @ForceInline
    @Override
    public final int bitSize() { return VSIZE; }

    @ForceInline
    @Override
    public final int byteSize() { return VSIZE / Byte.SIZE; }

    /*package-private*/
    @ForceInline
    final @Override
    long[] vec() {
        return (long[])getPayload();
    }

    // Virtualized constructors

    @Override
    @ForceInline
    public final LongMaxVector broadcast(long e) {
        return (LongMaxVector) super.broadcastTemplate(e);  // specialize
    }


    @Override
    @ForceInline
    LongMaxMask maskFromArray(boolean[] bits) {
        return new LongMaxMask(bits);
    }

    @Override
    @ForceInline
    LongMaxShuffle iotaShuffle() { return LongMaxShuffle.IOTA; }

    @ForceInline
    LongMaxShuffle iotaShuffle(int start, int step, boolean wrap) {
      if (wrap) {
        return (LongMaxShuffle)VectorSupport.shuffleIota(ETYPE, LongMaxShuffle.class, VSPECIES, VLENGTH, start, step, 1,
                (l, lstart, lstep, s) -> s.shuffleFromOp(i -> (VectorIntrinsics.wrapToRange(i*lstep + lstart, l))));
      } else {
        return (LongMaxShuffle)VectorSupport.shuffleIota(ETYPE, LongMaxShuffle.class, VSPECIES, VLENGTH, start, step, 0,
                (l, lstart, lstep, s) -> s.shuffleFromOp(i -> (i*lstep + lstart)));
      }
    }

    @Override
    @ForceInline
    LongMaxShuffle shuffleFromBytes(byte[] reorder) { return new LongMaxShuffle(reorder); }

    @Override
    @ForceInline
    LongMaxShuffle shuffleFromArray(int[] indexes, int i) { return new LongMaxShuffle(indexes, i); }

    @Override
    @ForceInline
    LongMaxShuffle shuffleFromOp(IntUnaryOperator fn) { return new LongMaxShuffle(fn); }

    // Make a vector of the same species but the given elements:
    @ForceInline
    final @Override
    LongMaxVector vectorFactory(long[] vec) {
        return new LongMaxVector(vec);
    }

    @ForceInline
    final @Override
    ByteMaxVector asByteVectorRaw() {
        return (ByteMaxVector) super.asByteVectorRawTemplate();  // specialize
    }

    @ForceInline
    final @Override
    AbstractVector<?> asVectorRaw(LaneType laneType) {
        return super.asVectorRawTemplate(laneType);  // specialize
    }

    // Unary operator

    @ForceInline
    final @Override
    LongMaxVector uOp(FUnOp f) {
        return (LongMaxVector) super.uOpTemplate(f);  // specialize
    }

    @ForceInline
    final @Override
    LongMaxVector uOp(VectorMask<Long> m, FUnOp f) {
        return (LongMaxVector)
            super.uOpTemplate((LongMaxMask)m, f);  // specialize
    }

    // Binary operator

    @ForceInline
    final @Override
    LongMaxVector bOp(Vector<Long> v, FBinOp f) {
        return (LongMaxVector) super.bOpTemplate((LongMaxVector)v, f);  // specialize
    }

    @ForceInline
    final @Override
    LongMaxVector bOp(Vector<Long> v,
                     VectorMask<Long> m, FBinOp f) {
        return (LongMaxVector)
            super.bOpTemplate((LongMaxVector)v, (LongMaxMask)m,
                              f);  // specialize
    }

    // Ternary operator

    @ForceInline
    final @Override
    LongMaxVector tOp(Vector<Long> v1, Vector<Long> v2, FTriOp f) {
        return (LongMaxVector)
            super.tOpTemplate((LongMaxVector)v1, (LongMaxVector)v2,
                              f);  // specialize
    }

    @ForceInline
    final @Override
    LongMaxVector tOp(Vector<Long> v1, Vector<Long> v2,
                     VectorMask<Long> m, FTriOp f) {
        return (LongMaxVector)
            super.tOpTemplate((LongMaxVector)v1, (LongMaxVector)v2,
                              (LongMaxMask)m, f);  // specialize
    }

    @ForceInline
    final @Override
    long rOp(long v, FBinOp f) {
        return super.rOpTemplate(v, f);  // specialize
    }

    @Override
    @ForceInline
    public final <F>
    Vector<F> convertShape(VectorOperators.Conversion<Long,F> conv,
                           VectorSpecies<F> rsp, int part) {
        return super.convertShapeTemplate(conv, rsp, part);  // specialize
    }

    @Override
    @ForceInline
    public final <F>
    Vector<F> reinterpretShape(VectorSpecies<F> toSpecies, int part) {
        return super.reinterpretShapeTemplate(toSpecies, part);  // specialize
    }

    // Specialized algebraic operations:

    // The following definition forces a specialized version of this
    // crucial method into the v-table of this class.  A call to add()
    // will inline to a call to lanewise(ADD,), at which point the JIT
    // intrinsic will have the opcode of ADD, plus all the metadata
    // for this particular class, enabling it to generate precise
    // code.
    //
    // There is probably no benefit to the JIT to specialize the
    // masked or broadcast versions of the lanewise method.

    @Override
    @ForceInline
    public LongMaxVector lanewise(Unary op) {
        return (LongMaxVector) super.lanewiseTemplate(op);  // specialize
    }

    @Override
    @ForceInline
    public LongMaxVector lanewise(Binary op, Vector<Long> v) {
        return (LongMaxVector) super.lanewiseTemplate(op, v);  // specialize
    }

    /*package-private*/
    @Override
    @ForceInline LongMaxVector
    lanewiseShift(VectorOperators.Binary op, int e) {
        return (LongMaxVector) super.lanewiseShiftTemplate(op, e);  // specialize
    }

    /*package-private*/
    @Override
    @ForceInline
    public final
    LongMaxVector
    lanewise(VectorOperators.Ternary op, Vector<Long> v1, Vector<Long> v2) {
        return (LongMaxVector) super.lanewiseTemplate(op, v1, v2);  // specialize
    }

    @Override
    @ForceInline
    public final
    LongMaxVector addIndex(int scale) {
        return (LongMaxVector) super.addIndexTemplate(scale);  // specialize
    }

    // Type specific horizontal reductions

    @Override
    @ForceInline
    public final long reduceLanes(VectorOperators.Associative op) {
        return super.reduceLanesTemplate(op);  // specialized
    }

    @Override
    @ForceInline
    public final long reduceLanes(VectorOperators.Associative op,
                                    VectorMask<Long> m) {
        return super.reduceLanesTemplate(op, m);  // specialized
    }

    @Override
    @ForceInline
    public final long reduceLanesToLong(VectorOperators.Associative op) {
        return (long) super.reduceLanesTemplate(op);  // specialized
    }

    @Override
    @ForceInline
    public final long reduceLanesToLong(VectorOperators.Associative op,
                                        VectorMask<Long> m) {
        return (long) super.reduceLanesTemplate(op, m);  // specialized
    }

    @ForceInline
    public VectorShuffle<Long> toShuffle() {
        return super.toShuffleTemplate(LongMaxShuffle.class); // specialize
    }

    // Specialized unary testing

    @Override
    @ForceInline
    public final LongMaxMask test(Test op) {
        return super.testTemplate(LongMaxMask.class, op);  // specialize
    }

    // Specialized comparisons

    @Override
    @ForceInline
    public final LongMaxMask compare(Comparison op, Vector<Long> v) {
        return super.compareTemplate(LongMaxMask.class, op, v);  // specialize
    }

    @Override
    @ForceInline
    public final LongMaxMask compare(Comparison op, long s) {
        return super.compareTemplate(LongMaxMask.class, op, s);  // specialize
    }


    @Override
    @ForceInline
    public LongMaxVector blend(Vector<Long> v, VectorMask<Long> m) {
        return (LongMaxVector)
            super.blendTemplate(LongMaxMask.class,
                                (LongMaxVector) v,
                                (LongMaxMask) m);  // specialize
    }

    @Override
    @ForceInline
    public LongMaxVector slice(int origin, Vector<Long> v) {
        return (LongMaxVector) super.sliceTemplate(origin, v);  // specialize
    }

    @Override
    @ForceInline
    public LongMaxVector slice(int origin) {
        return (LongMaxVector) super.sliceTemplate(origin);  // specialize
    }

    @Override
    @ForceInline
    public LongMaxVector unslice(int origin, Vector<Long> w, int part) {
        return (LongMaxVector) super.unsliceTemplate(origin, w, part);  // specialize
    }

    @Override
    @ForceInline
    public LongMaxVector unslice(int origin, Vector<Long> w, int part, VectorMask<Long> m) {
        return (LongMaxVector)
            super.unsliceTemplate(LongMaxMask.class,
                                  origin, w, part,
                                  (LongMaxMask) m);  // specialize
    }

    @Override
    @ForceInline
    public LongMaxVector unslice(int origin) {
        return (LongMaxVector) super.unsliceTemplate(origin);  // specialize
    }

    @Override
    @ForceInline
    public LongMaxVector rearrange(VectorShuffle<Long> s) {
        return (LongMaxVector)
            super.rearrangeTemplate(LongMaxShuffle.class,
                                    (LongMaxShuffle) s);  // specialize
    }

    @Override
    @ForceInline
    public LongMaxVector rearrange(VectorShuffle<Long> shuffle,
                                  VectorMask<Long> m) {
        return (LongMaxVector)
            super.rearrangeTemplate(LongMaxShuffle.class,
                                    (LongMaxShuffle) shuffle,
                                    (LongMaxMask) m);  // specialize
    }

    @Override
    @ForceInline
    public LongMaxVector rearrange(VectorShuffle<Long> s,
                                  Vector<Long> v) {
        return (LongMaxVector)
            super.rearrangeTemplate(LongMaxShuffle.class,
                                    (LongMaxShuffle) s,
                                    (LongMaxVector) v);  // specialize
    }

    @Override
    @ForceInline
    public LongMaxVector selectFrom(Vector<Long> v) {
        return (LongMaxVector)
            super.selectFromTemplate((LongMaxVector) v);  // specialize
    }

    @Override
    @ForceInline
    public LongMaxVector selectFrom(Vector<Long> v,
                                   VectorMask<Long> m) {
        return (LongMaxVector)
            super.selectFromTemplate((LongMaxVector) v,
                                     (LongMaxMask) m);  // specialize
    }


    @ForceInline
    @Override
    public long lane(int i) {
        if (i < 0 || i >= VLENGTH) {
            throw new IllegalArgumentException("Index " + i + " must be zero or positive, and less than " + VLENGTH);
        }
        return laneHelper(i);
    }

    public long laneHelper(int i) {
        return (long) VectorSupport.extract(
                                VCLASS, ETYPE, VLENGTH,
                                this, i,
                                (vec, ix) -> {
                                    long[] vecarr = vec.vec();
                                    return (long)vecarr[ix];
                                });
    }

    @ForceInline
    @Override
    public LongMaxVector withLane(int i, long e) {
        if (i < 0 || i >= VLENGTH) {
            throw new IllegalArgumentException("Index " + i + " must be zero or positive, and less than " + VLENGTH);
        }
        return withLaneHelper(i, e);
    }

    public LongMaxVector withLaneHelper(int i, long e) {
        return VectorSupport.insert(
                                VCLASS, ETYPE, VLENGTH,
                                this, i, (long)e,
                                (v, ix, bits) -> {
                                    long[] res = v.vec().clone();
                                    res[ix] = (long)bits;
                                    return v.vectorFactory(res);
                                });
    }

    // Mask

    static final class LongMaxMask extends AbstractMask<Long> {
        static final int VLENGTH = VSPECIES.laneCount();    // used by the JVM
        static final Class<Long> ETYPE = long.class; // used by the JVM

        LongMaxMask(boolean[] bits) {
            this(bits, 0);
        }

        LongMaxMask(boolean[] bits, int offset) {
            super(prepare(bits, offset));
        }

        LongMaxMask(boolean val) {
            super(prepare(val));
        }

        private static boolean[] prepare(boolean[] bits, int offset) {
            boolean[] newBits = new boolean[VSPECIES.laneCount()];
            for (int i = 0; i < newBits.length; i++) {
                newBits[i] = bits[offset + i];
            }
            return newBits;
        }

        private static boolean[] prepare(boolean val) {
            boolean[] bits = new boolean[VSPECIES.laneCount()];
            Arrays.fill(bits, val);
            return bits;
        }

        @ForceInline
        final @Override
        public LongSpecies vspecies() {
            // ISSUE:  This should probably be a @Stable
            // field inside AbstractMask, rather than
            // a megamorphic method.
            return VSPECIES;
        }

        @ForceInline
        boolean[] getBits() {
            return (boolean[])getPayload();
        }

        @Override
        LongMaxMask uOp(MUnOp f) {
            boolean[] res = new boolean[vspecies().laneCount()];
            boolean[] bits = getBits();
            for (int i = 0; i < res.length; i++) {
                res[i] = f.apply(i, bits[i]);
            }
            return new LongMaxMask(res);
        }

        @Override
        LongMaxMask bOp(VectorMask<Long> m, MBinOp f) {
            boolean[] res = new boolean[vspecies().laneCount()];
            boolean[] bits = getBits();
            boolean[] mbits = ((LongMaxMask)m).getBits();
            for (int i = 0; i < res.length; i++) {
                res[i] = f.apply(i, bits[i], mbits[i]);
            }
            return new LongMaxMask(res);
        }

        @ForceInline
        @Override
        public final
        LongMaxVector toVector() {
            return (LongMaxVector) super.toVectorTemplate();  // specialize
        }

        /**
         * Helper function for lane-wise mask conversions.
         * This function kicks in after intrinsic failure.
         */
        @ForceInline
        private final <E>
        VectorMask<E> defaultMaskCast(AbstractSpecies<E> dsp) {
            if (length() != dsp.laneCount())
                throw new IllegalArgumentException("VectorMask length and species length differ");
            boolean[] maskArray = toArray();
            return  dsp.maskFactory(maskArray).check(dsp);
        }

        @Override
        @ForceInline
        public <E> VectorMask<E> cast(VectorSpecies<E> dsp) {
            AbstractSpecies<E> species = (AbstractSpecies<E>) dsp;
            if (length() != species.laneCount())
                throw new IllegalArgumentException("VectorMask length and species length differ");
            if (VSIZE == species.vectorBitSize()) {
                Class<?> dtype = species.elementType();
                Class<?> dmtype = species.maskType();
                return VectorSupport.convert(VectorSupport.VECTOR_OP_REINTERPRET,
                    this.getClass(), ETYPE, VLENGTH,
                    dmtype, dtype, VLENGTH,
                    this, species,
                    LongMaxMask::defaultMaskCast);
            }
            return this.defaultMaskCast(species);
        }

        @Override
        @ForceInline
        public LongMaxMask eq(VectorMask<Long> mask) {
            Objects.requireNonNull(mask);
            LongMaxMask m = (LongMaxMask)mask;
            return xor(m.not());
        }

        // Unary operations

        @Override
        @ForceInline
        public LongMaxMask not() {
            return xor(maskAll(true));
        }

        // Binary operations

        @Override
        @ForceInline
        public LongMaxMask and(VectorMask<Long> mask) {
            Objects.requireNonNull(mask);
            LongMaxMask m = (LongMaxMask)mask;
            return VectorSupport.binaryOp(VECTOR_OP_AND, LongMaxMask.class, long.class, VLENGTH,
                                             this, m,
                                             (m1, m2) -> m1.bOp(m2, (i, a, b) -> a & b));
        }

        @Override
        @ForceInline
        public LongMaxMask or(VectorMask<Long> mask) {
            Objects.requireNonNull(mask);
            LongMaxMask m = (LongMaxMask)mask;
            return VectorSupport.binaryOp(VECTOR_OP_OR, LongMaxMask.class, long.class, VLENGTH,
                                             this, m,
                                             (m1, m2) -> m1.bOp(m2, (i, a, b) -> a | b));
        }

        @ForceInline
        /* package-private */
        LongMaxMask xor(VectorMask<Long> mask) {
            Objects.requireNonNull(mask);
            LongMaxMask m = (LongMaxMask)mask;
            return VectorSupport.binaryOp(VECTOR_OP_XOR, LongMaxMask.class, long.class, VLENGTH,
                                          this, m,
                                          (m1, m2) -> m1.bOp(m2, (i, a, b) -> a ^ b));
        }

        // Mask Query operations

        @Override
        @ForceInline
        public int trueCount() {
            return VectorSupport.maskReductionCoerced(VECTOR_OP_MASK_TRUECOUNT, LongMaxMask.class, long.class, VLENGTH, this,
                                                      (m) -> trueCountHelper(((LongMaxMask)m).getBits()));
        }

        @Override
        @ForceInline
        public int firstTrue() {
            return VectorSupport.maskReductionCoerced(VECTOR_OP_MASK_FIRSTTRUE, LongMaxMask.class, long.class, VLENGTH, this,
                                                      (m) -> firstTrueHelper(((LongMaxMask)m).getBits()));
        }

        @Override
        @ForceInline
        public int lastTrue() {
            return VectorSupport.maskReductionCoerced(VECTOR_OP_MASK_LASTTRUE, LongMaxMask.class, long.class, VLENGTH, this,
                                                      (m) -> lastTrueHelper(((LongMaxMask)m).getBits()));
        }

        // Reductions

        @Override
        @ForceInline
        public boolean anyTrue() {
            return VectorSupport.test(BT_ne, LongMaxMask.class, long.class, VLENGTH,
                                         this, vspecies().maskAll(true),
                                         (m, __) -> anyTrueHelper(((LongMaxMask)m).getBits()));
        }

        @Override
        @ForceInline
        public boolean allTrue() {
            return VectorSupport.test(BT_overflow, LongMaxMask.class, long.class, VLENGTH,
                                         this, vspecies().maskAll(true),
                                         (m, __) -> allTrueHelper(((LongMaxMask)m).getBits()));
        }

        @ForceInline
        /*package-private*/
        static LongMaxMask maskAll(boolean bit) {
            return VectorSupport.broadcastCoerced(LongMaxMask.class, long.class, VLENGTH,
                                                  (bit ? -1 : 0), null,
                                                  (v, __) -> (v != 0 ? TRUE_MASK : FALSE_MASK));
        }
        private static final LongMaxMask  TRUE_MASK = new LongMaxMask(true);
        private static final LongMaxMask FALSE_MASK = new LongMaxMask(false);

    }

    // Shuffle

    static final class LongMaxShuffle extends AbstractShuffle<Long> {
        static final int VLENGTH = VSPECIES.laneCount();    // used by the JVM
        static final Class<Long> ETYPE = long.class; // used by the JVM

        LongMaxShuffle(byte[] reorder) {
            super(VLENGTH, reorder);
        }

        public LongMaxShuffle(int[] reorder) {
            super(VLENGTH, reorder);
        }

        public LongMaxShuffle(int[] reorder, int i) {
            super(VLENGTH, reorder, i);
        }

        public LongMaxShuffle(IntUnaryOperator fn) {
            super(VLENGTH, fn);
        }

        @Override
        public LongSpecies vspecies() {
            return VSPECIES;
        }

        static {
            // There must be enough bits in the shuffle lanes to encode
            // VLENGTH valid indexes and VLENGTH exceptional ones.
            assert(VLENGTH < Byte.MAX_VALUE);
            assert(Byte.MIN_VALUE <= -VLENGTH);
        }
        static final LongMaxShuffle IOTA = new LongMaxShuffle(IDENTITY);

        @Override
        @ForceInline
        public LongMaxVector toVector() {
            return VectorSupport.shuffleToVector(VCLASS, ETYPE, LongMaxShuffle.class, this, VLENGTH,
                                                    (s) -> ((LongMaxVector)(((AbstractShuffle<Long>)(s)).toVectorTemplate())));
        }

        @Override
        @ForceInline
        public <F> VectorShuffle<F> cast(VectorSpecies<F> s) {
            AbstractSpecies<F> species = (AbstractSpecies<F>) s;
            if (length() != species.laneCount())
                throw new IllegalArgumentException("VectorShuffle length and species length differ");
            int[] shuffleArray = toArray();
            return s.shuffleFromArray(shuffleArray, 0).check(s);
        }

        @ForceInline
        @Override
        public LongMaxShuffle rearrange(VectorShuffle<Long> shuffle) {
            LongMaxShuffle s = (LongMaxShuffle) shuffle;
            byte[] reorder1 = reorder();
            byte[] reorder2 = s.reorder();
            byte[] r = new byte[reorder1.length];
            for (int i = 0; i < reorder1.length; i++) {
                int ssi = reorder2[i];
                r[i] = reorder1[ssi];  // throws on exceptional index
            }
            return new LongMaxShuffle(r);
        }
    }

    // ================================================

    // Specialized low-level memory operations.

    @ForceInline
    @Override
    final
    LongVector fromArray0(long[] a, int offset) {
        return super.fromArray0Template(a, offset);  // specialize
    }



    @ForceInline
    @Override
    final
    LongVector fromByteArray0(byte[] a, int offset) {
        return super.fromByteArray0Template(a, offset);  // specialize
    }

    @ForceInline
    @Override
    final
    LongVector fromByteBuffer0(ByteBuffer bb, int offset) {
        return super.fromByteBuffer0Template(bb, offset);  // specialize
    }

    @ForceInline
    @Override
    final
    void intoArray0(long[] a, int offset) {
        super.intoArray0Template(a, offset);  // specialize
    }

    @ForceInline
    @Override
    final
    void intoByteArray0(byte[] a, int offset) {
        super.intoByteArray0Template(a, offset);  // specialize
    }

    // End of specialized low-level memory operations.

    // ================================================

}
