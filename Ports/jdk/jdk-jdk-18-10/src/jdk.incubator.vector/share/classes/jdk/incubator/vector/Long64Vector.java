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
final class Long64Vector extends LongVector {
    static final LongSpecies VSPECIES =
        (LongSpecies) LongVector.SPECIES_64;

    static final VectorShape VSHAPE =
        VSPECIES.vectorShape();

    static final Class<Long64Vector> VCLASS = Long64Vector.class;

    static final int VSIZE = VSPECIES.vectorBitSize();

    static final int VLENGTH = VSPECIES.laneCount(); // used by the JVM

    static final Class<Long> ETYPE = long.class; // used by the JVM

    Long64Vector(long[] v) {
        super(v);
    }

    // For compatibility as Long64Vector::new,
    // stored into species.vectorFactory.
    Long64Vector(Object v) {
        this((long[]) v);
    }

    static final Long64Vector ZERO = new Long64Vector(new long[VLENGTH]);
    static final Long64Vector IOTA = new Long64Vector(VSPECIES.iotaArray());

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
    public final Long64Vector broadcast(long e) {
        return (Long64Vector) super.broadcastTemplate(e);  // specialize
    }


    @Override
    @ForceInline
    Long64Mask maskFromArray(boolean[] bits) {
        return new Long64Mask(bits);
    }

    @Override
    @ForceInline
    Long64Shuffle iotaShuffle() { return Long64Shuffle.IOTA; }

    @ForceInline
    Long64Shuffle iotaShuffle(int start, int step, boolean wrap) {
      if (wrap) {
        return (Long64Shuffle)VectorSupport.shuffleIota(ETYPE, Long64Shuffle.class, VSPECIES, VLENGTH, start, step, 1,
                (l, lstart, lstep, s) -> s.shuffleFromOp(i -> (VectorIntrinsics.wrapToRange(i*lstep + lstart, l))));
      } else {
        return (Long64Shuffle)VectorSupport.shuffleIota(ETYPE, Long64Shuffle.class, VSPECIES, VLENGTH, start, step, 0,
                (l, lstart, lstep, s) -> s.shuffleFromOp(i -> (i*lstep + lstart)));
      }
    }

    @Override
    @ForceInline
    Long64Shuffle shuffleFromBytes(byte[] reorder) { return new Long64Shuffle(reorder); }

    @Override
    @ForceInline
    Long64Shuffle shuffleFromArray(int[] indexes, int i) { return new Long64Shuffle(indexes, i); }

    @Override
    @ForceInline
    Long64Shuffle shuffleFromOp(IntUnaryOperator fn) { return new Long64Shuffle(fn); }

    // Make a vector of the same species but the given elements:
    @ForceInline
    final @Override
    Long64Vector vectorFactory(long[] vec) {
        return new Long64Vector(vec);
    }

    @ForceInline
    final @Override
    Byte64Vector asByteVectorRaw() {
        return (Byte64Vector) super.asByteVectorRawTemplate();  // specialize
    }

    @ForceInline
    final @Override
    AbstractVector<?> asVectorRaw(LaneType laneType) {
        return super.asVectorRawTemplate(laneType);  // specialize
    }

    // Unary operator

    @ForceInline
    final @Override
    Long64Vector uOp(FUnOp f) {
        return (Long64Vector) super.uOpTemplate(f);  // specialize
    }

    @ForceInline
    final @Override
    Long64Vector uOp(VectorMask<Long> m, FUnOp f) {
        return (Long64Vector)
            super.uOpTemplate((Long64Mask)m, f);  // specialize
    }

    // Binary operator

    @ForceInline
    final @Override
    Long64Vector bOp(Vector<Long> v, FBinOp f) {
        return (Long64Vector) super.bOpTemplate((Long64Vector)v, f);  // specialize
    }

    @ForceInline
    final @Override
    Long64Vector bOp(Vector<Long> v,
                     VectorMask<Long> m, FBinOp f) {
        return (Long64Vector)
            super.bOpTemplate((Long64Vector)v, (Long64Mask)m,
                              f);  // specialize
    }

    // Ternary operator

    @ForceInline
    final @Override
    Long64Vector tOp(Vector<Long> v1, Vector<Long> v2, FTriOp f) {
        return (Long64Vector)
            super.tOpTemplate((Long64Vector)v1, (Long64Vector)v2,
                              f);  // specialize
    }

    @ForceInline
    final @Override
    Long64Vector tOp(Vector<Long> v1, Vector<Long> v2,
                     VectorMask<Long> m, FTriOp f) {
        return (Long64Vector)
            super.tOpTemplate((Long64Vector)v1, (Long64Vector)v2,
                              (Long64Mask)m, f);  // specialize
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
    public Long64Vector lanewise(Unary op) {
        return (Long64Vector) super.lanewiseTemplate(op);  // specialize
    }

    @Override
    @ForceInline
    public Long64Vector lanewise(Binary op, Vector<Long> v) {
        return (Long64Vector) super.lanewiseTemplate(op, v);  // specialize
    }

    /*package-private*/
    @Override
    @ForceInline Long64Vector
    lanewiseShift(VectorOperators.Binary op, int e) {
        return (Long64Vector) super.lanewiseShiftTemplate(op, e);  // specialize
    }

    /*package-private*/
    @Override
    @ForceInline
    public final
    Long64Vector
    lanewise(VectorOperators.Ternary op, Vector<Long> v1, Vector<Long> v2) {
        return (Long64Vector) super.lanewiseTemplate(op, v1, v2);  // specialize
    }

    @Override
    @ForceInline
    public final
    Long64Vector addIndex(int scale) {
        return (Long64Vector) super.addIndexTemplate(scale);  // specialize
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
        return super.toShuffleTemplate(Long64Shuffle.class); // specialize
    }

    // Specialized unary testing

    @Override
    @ForceInline
    public final Long64Mask test(Test op) {
        return super.testTemplate(Long64Mask.class, op);  // specialize
    }

    // Specialized comparisons

    @Override
    @ForceInline
    public final Long64Mask compare(Comparison op, Vector<Long> v) {
        return super.compareTemplate(Long64Mask.class, op, v);  // specialize
    }

    @Override
    @ForceInline
    public final Long64Mask compare(Comparison op, long s) {
        return super.compareTemplate(Long64Mask.class, op, s);  // specialize
    }


    @Override
    @ForceInline
    public Long64Vector blend(Vector<Long> v, VectorMask<Long> m) {
        return (Long64Vector)
            super.blendTemplate(Long64Mask.class,
                                (Long64Vector) v,
                                (Long64Mask) m);  // specialize
    }

    @Override
    @ForceInline
    public Long64Vector slice(int origin, Vector<Long> v) {
        return (Long64Vector) super.sliceTemplate(origin, v);  // specialize
    }

    @Override
    @ForceInline
    public Long64Vector slice(int origin) {
        return (Long64Vector) super.sliceTemplate(origin);  // specialize
    }

    @Override
    @ForceInline
    public Long64Vector unslice(int origin, Vector<Long> w, int part) {
        return (Long64Vector) super.unsliceTemplate(origin, w, part);  // specialize
    }

    @Override
    @ForceInline
    public Long64Vector unslice(int origin, Vector<Long> w, int part, VectorMask<Long> m) {
        return (Long64Vector)
            super.unsliceTemplate(Long64Mask.class,
                                  origin, w, part,
                                  (Long64Mask) m);  // specialize
    }

    @Override
    @ForceInline
    public Long64Vector unslice(int origin) {
        return (Long64Vector) super.unsliceTemplate(origin);  // specialize
    }

    @Override
    @ForceInline
    public Long64Vector rearrange(VectorShuffle<Long> s) {
        return (Long64Vector)
            super.rearrangeTemplate(Long64Shuffle.class,
                                    (Long64Shuffle) s);  // specialize
    }

    @Override
    @ForceInline
    public Long64Vector rearrange(VectorShuffle<Long> shuffle,
                                  VectorMask<Long> m) {
        return (Long64Vector)
            super.rearrangeTemplate(Long64Shuffle.class,
                                    (Long64Shuffle) shuffle,
                                    (Long64Mask) m);  // specialize
    }

    @Override
    @ForceInline
    public Long64Vector rearrange(VectorShuffle<Long> s,
                                  Vector<Long> v) {
        return (Long64Vector)
            super.rearrangeTemplate(Long64Shuffle.class,
                                    (Long64Shuffle) s,
                                    (Long64Vector) v);  // specialize
    }

    @Override
    @ForceInline
    public Long64Vector selectFrom(Vector<Long> v) {
        return (Long64Vector)
            super.selectFromTemplate((Long64Vector) v);  // specialize
    }

    @Override
    @ForceInline
    public Long64Vector selectFrom(Vector<Long> v,
                                   VectorMask<Long> m) {
        return (Long64Vector)
            super.selectFromTemplate((Long64Vector) v,
                                     (Long64Mask) m);  // specialize
    }


    @ForceInline
    @Override
    public long lane(int i) {
        switch(i) {
            case 0: return laneHelper(0);
            default: throw new IllegalArgumentException("Index " + i + " must be zero or positive, and less than " + VLENGTH);
        }
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
    public Long64Vector withLane(int i, long e) {
        switch (i) {
            case 0: return withLaneHelper(0, e);
            default: throw new IllegalArgumentException("Index " + i + " must be zero or positive, and less than " + VLENGTH);
        }
    }

    public Long64Vector withLaneHelper(int i, long e) {
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

    static final class Long64Mask extends AbstractMask<Long> {
        static final int VLENGTH = VSPECIES.laneCount();    // used by the JVM
        static final Class<Long> ETYPE = long.class; // used by the JVM

        Long64Mask(boolean[] bits) {
            this(bits, 0);
        }

        Long64Mask(boolean[] bits, int offset) {
            super(prepare(bits, offset));
        }

        Long64Mask(boolean val) {
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
        Long64Mask uOp(MUnOp f) {
            boolean[] res = new boolean[vspecies().laneCount()];
            boolean[] bits = getBits();
            for (int i = 0; i < res.length; i++) {
                res[i] = f.apply(i, bits[i]);
            }
            return new Long64Mask(res);
        }

        @Override
        Long64Mask bOp(VectorMask<Long> m, MBinOp f) {
            boolean[] res = new boolean[vspecies().laneCount()];
            boolean[] bits = getBits();
            boolean[] mbits = ((Long64Mask)m).getBits();
            for (int i = 0; i < res.length; i++) {
                res[i] = f.apply(i, bits[i], mbits[i]);
            }
            return new Long64Mask(res);
        }

        @ForceInline
        @Override
        public final
        Long64Vector toVector() {
            return (Long64Vector) super.toVectorTemplate();  // specialize
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
                    Long64Mask::defaultMaskCast);
            }
            return this.defaultMaskCast(species);
        }

        @Override
        @ForceInline
        public Long64Mask eq(VectorMask<Long> mask) {
            Objects.requireNonNull(mask);
            Long64Mask m = (Long64Mask)mask;
            return xor(m.not());
        }

        // Unary operations

        @Override
        @ForceInline
        public Long64Mask not() {
            return xor(maskAll(true));
        }

        // Binary operations

        @Override
        @ForceInline
        public Long64Mask and(VectorMask<Long> mask) {
            Objects.requireNonNull(mask);
            Long64Mask m = (Long64Mask)mask;
            return VectorSupport.binaryOp(VECTOR_OP_AND, Long64Mask.class, long.class, VLENGTH,
                                             this, m,
                                             (m1, m2) -> m1.bOp(m2, (i, a, b) -> a & b));
        }

        @Override
        @ForceInline
        public Long64Mask or(VectorMask<Long> mask) {
            Objects.requireNonNull(mask);
            Long64Mask m = (Long64Mask)mask;
            return VectorSupport.binaryOp(VECTOR_OP_OR, Long64Mask.class, long.class, VLENGTH,
                                             this, m,
                                             (m1, m2) -> m1.bOp(m2, (i, a, b) -> a | b));
        }

        @ForceInline
        /* package-private */
        Long64Mask xor(VectorMask<Long> mask) {
            Objects.requireNonNull(mask);
            Long64Mask m = (Long64Mask)mask;
            return VectorSupport.binaryOp(VECTOR_OP_XOR, Long64Mask.class, long.class, VLENGTH,
                                          this, m,
                                          (m1, m2) -> m1.bOp(m2, (i, a, b) -> a ^ b));
        }

        // Mask Query operations

        @Override
        @ForceInline
        public int trueCount() {
            return VectorSupport.maskReductionCoerced(VECTOR_OP_MASK_TRUECOUNT, Long64Mask.class, long.class, VLENGTH, this,
                                                      (m) -> trueCountHelper(((Long64Mask)m).getBits()));
        }

        @Override
        @ForceInline
        public int firstTrue() {
            return VectorSupport.maskReductionCoerced(VECTOR_OP_MASK_FIRSTTRUE, Long64Mask.class, long.class, VLENGTH, this,
                                                      (m) -> firstTrueHelper(((Long64Mask)m).getBits()));
        }

        @Override
        @ForceInline
        public int lastTrue() {
            return VectorSupport.maskReductionCoerced(VECTOR_OP_MASK_LASTTRUE, Long64Mask.class, long.class, VLENGTH, this,
                                                      (m) -> lastTrueHelper(((Long64Mask)m).getBits()));
        }

        // Reductions

        @Override
        @ForceInline
        public boolean anyTrue() {
            return VectorSupport.test(BT_ne, Long64Mask.class, long.class, VLENGTH,
                                         this, vspecies().maskAll(true),
                                         (m, __) -> anyTrueHelper(((Long64Mask)m).getBits()));
        }

        @Override
        @ForceInline
        public boolean allTrue() {
            return VectorSupport.test(BT_overflow, Long64Mask.class, long.class, VLENGTH,
                                         this, vspecies().maskAll(true),
                                         (m, __) -> allTrueHelper(((Long64Mask)m).getBits()));
        }

        @ForceInline
        /*package-private*/
        static Long64Mask maskAll(boolean bit) {
            return VectorSupport.broadcastCoerced(Long64Mask.class, long.class, VLENGTH,
                                                  (bit ? -1 : 0), null,
                                                  (v, __) -> (v != 0 ? TRUE_MASK : FALSE_MASK));
        }
        private static final Long64Mask  TRUE_MASK = new Long64Mask(true);
        private static final Long64Mask FALSE_MASK = new Long64Mask(false);

    }

    // Shuffle

    static final class Long64Shuffle extends AbstractShuffle<Long> {
        static final int VLENGTH = VSPECIES.laneCount();    // used by the JVM
        static final Class<Long> ETYPE = long.class; // used by the JVM

        Long64Shuffle(byte[] reorder) {
            super(VLENGTH, reorder);
        }

        public Long64Shuffle(int[] reorder) {
            super(VLENGTH, reorder);
        }

        public Long64Shuffle(int[] reorder, int i) {
            super(VLENGTH, reorder, i);
        }

        public Long64Shuffle(IntUnaryOperator fn) {
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
        static final Long64Shuffle IOTA = new Long64Shuffle(IDENTITY);

        @Override
        @ForceInline
        public Long64Vector toVector() {
            return VectorSupport.shuffleToVector(VCLASS, ETYPE, Long64Shuffle.class, this, VLENGTH,
                                                    (s) -> ((Long64Vector)(((AbstractShuffle<Long>)(s)).toVectorTemplate())));
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
        public Long64Shuffle rearrange(VectorShuffle<Long> shuffle) {
            Long64Shuffle s = (Long64Shuffle) shuffle;
            byte[] reorder1 = reorder();
            byte[] reorder2 = s.reorder();
            byte[] r = new byte[reorder1.length];
            for (int i = 0; i < reorder1.length; i++) {
                int ssi = reorder2[i];
                r[i] = reorder1[ssi];  // throws on exceptional index
            }
            return new Long64Shuffle(r);
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
