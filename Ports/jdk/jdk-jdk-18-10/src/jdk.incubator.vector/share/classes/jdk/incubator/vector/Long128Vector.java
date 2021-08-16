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
final class Long128Vector extends LongVector {
    static final LongSpecies VSPECIES =
        (LongSpecies) LongVector.SPECIES_128;

    static final VectorShape VSHAPE =
        VSPECIES.vectorShape();

    static final Class<Long128Vector> VCLASS = Long128Vector.class;

    static final int VSIZE = VSPECIES.vectorBitSize();

    static final int VLENGTH = VSPECIES.laneCount(); // used by the JVM

    static final Class<Long> ETYPE = long.class; // used by the JVM

    Long128Vector(long[] v) {
        super(v);
    }

    // For compatibility as Long128Vector::new,
    // stored into species.vectorFactory.
    Long128Vector(Object v) {
        this((long[]) v);
    }

    static final Long128Vector ZERO = new Long128Vector(new long[VLENGTH]);
    static final Long128Vector IOTA = new Long128Vector(VSPECIES.iotaArray());

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
    public final Long128Vector broadcast(long e) {
        return (Long128Vector) super.broadcastTemplate(e);  // specialize
    }


    @Override
    @ForceInline
    Long128Mask maskFromArray(boolean[] bits) {
        return new Long128Mask(bits);
    }

    @Override
    @ForceInline
    Long128Shuffle iotaShuffle() { return Long128Shuffle.IOTA; }

    @ForceInline
    Long128Shuffle iotaShuffle(int start, int step, boolean wrap) {
      if (wrap) {
        return (Long128Shuffle)VectorSupport.shuffleIota(ETYPE, Long128Shuffle.class, VSPECIES, VLENGTH, start, step, 1,
                (l, lstart, lstep, s) -> s.shuffleFromOp(i -> (VectorIntrinsics.wrapToRange(i*lstep + lstart, l))));
      } else {
        return (Long128Shuffle)VectorSupport.shuffleIota(ETYPE, Long128Shuffle.class, VSPECIES, VLENGTH, start, step, 0,
                (l, lstart, lstep, s) -> s.shuffleFromOp(i -> (i*lstep + lstart)));
      }
    }

    @Override
    @ForceInline
    Long128Shuffle shuffleFromBytes(byte[] reorder) { return new Long128Shuffle(reorder); }

    @Override
    @ForceInline
    Long128Shuffle shuffleFromArray(int[] indexes, int i) { return new Long128Shuffle(indexes, i); }

    @Override
    @ForceInline
    Long128Shuffle shuffleFromOp(IntUnaryOperator fn) { return new Long128Shuffle(fn); }

    // Make a vector of the same species but the given elements:
    @ForceInline
    final @Override
    Long128Vector vectorFactory(long[] vec) {
        return new Long128Vector(vec);
    }

    @ForceInline
    final @Override
    Byte128Vector asByteVectorRaw() {
        return (Byte128Vector) super.asByteVectorRawTemplate();  // specialize
    }

    @ForceInline
    final @Override
    AbstractVector<?> asVectorRaw(LaneType laneType) {
        return super.asVectorRawTemplate(laneType);  // specialize
    }

    // Unary operator

    @ForceInline
    final @Override
    Long128Vector uOp(FUnOp f) {
        return (Long128Vector) super.uOpTemplate(f);  // specialize
    }

    @ForceInline
    final @Override
    Long128Vector uOp(VectorMask<Long> m, FUnOp f) {
        return (Long128Vector)
            super.uOpTemplate((Long128Mask)m, f);  // specialize
    }

    // Binary operator

    @ForceInline
    final @Override
    Long128Vector bOp(Vector<Long> v, FBinOp f) {
        return (Long128Vector) super.bOpTemplate((Long128Vector)v, f);  // specialize
    }

    @ForceInline
    final @Override
    Long128Vector bOp(Vector<Long> v,
                     VectorMask<Long> m, FBinOp f) {
        return (Long128Vector)
            super.bOpTemplate((Long128Vector)v, (Long128Mask)m,
                              f);  // specialize
    }

    // Ternary operator

    @ForceInline
    final @Override
    Long128Vector tOp(Vector<Long> v1, Vector<Long> v2, FTriOp f) {
        return (Long128Vector)
            super.tOpTemplate((Long128Vector)v1, (Long128Vector)v2,
                              f);  // specialize
    }

    @ForceInline
    final @Override
    Long128Vector tOp(Vector<Long> v1, Vector<Long> v2,
                     VectorMask<Long> m, FTriOp f) {
        return (Long128Vector)
            super.tOpTemplate((Long128Vector)v1, (Long128Vector)v2,
                              (Long128Mask)m, f);  // specialize
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
    public Long128Vector lanewise(Unary op) {
        return (Long128Vector) super.lanewiseTemplate(op);  // specialize
    }

    @Override
    @ForceInline
    public Long128Vector lanewise(Binary op, Vector<Long> v) {
        return (Long128Vector) super.lanewiseTemplate(op, v);  // specialize
    }

    /*package-private*/
    @Override
    @ForceInline Long128Vector
    lanewiseShift(VectorOperators.Binary op, int e) {
        return (Long128Vector) super.lanewiseShiftTemplate(op, e);  // specialize
    }

    /*package-private*/
    @Override
    @ForceInline
    public final
    Long128Vector
    lanewise(VectorOperators.Ternary op, Vector<Long> v1, Vector<Long> v2) {
        return (Long128Vector) super.lanewiseTemplate(op, v1, v2);  // specialize
    }

    @Override
    @ForceInline
    public final
    Long128Vector addIndex(int scale) {
        return (Long128Vector) super.addIndexTemplate(scale);  // specialize
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
        return super.toShuffleTemplate(Long128Shuffle.class); // specialize
    }

    // Specialized unary testing

    @Override
    @ForceInline
    public final Long128Mask test(Test op) {
        return super.testTemplate(Long128Mask.class, op);  // specialize
    }

    // Specialized comparisons

    @Override
    @ForceInline
    public final Long128Mask compare(Comparison op, Vector<Long> v) {
        return super.compareTemplate(Long128Mask.class, op, v);  // specialize
    }

    @Override
    @ForceInline
    public final Long128Mask compare(Comparison op, long s) {
        return super.compareTemplate(Long128Mask.class, op, s);  // specialize
    }


    @Override
    @ForceInline
    public Long128Vector blend(Vector<Long> v, VectorMask<Long> m) {
        return (Long128Vector)
            super.blendTemplate(Long128Mask.class,
                                (Long128Vector) v,
                                (Long128Mask) m);  // specialize
    }

    @Override
    @ForceInline
    public Long128Vector slice(int origin, Vector<Long> v) {
        return (Long128Vector) super.sliceTemplate(origin, v);  // specialize
    }

    @Override
    @ForceInline
    public Long128Vector slice(int origin) {
        return (Long128Vector) super.sliceTemplate(origin);  // specialize
    }

    @Override
    @ForceInline
    public Long128Vector unslice(int origin, Vector<Long> w, int part) {
        return (Long128Vector) super.unsliceTemplate(origin, w, part);  // specialize
    }

    @Override
    @ForceInline
    public Long128Vector unslice(int origin, Vector<Long> w, int part, VectorMask<Long> m) {
        return (Long128Vector)
            super.unsliceTemplate(Long128Mask.class,
                                  origin, w, part,
                                  (Long128Mask) m);  // specialize
    }

    @Override
    @ForceInline
    public Long128Vector unslice(int origin) {
        return (Long128Vector) super.unsliceTemplate(origin);  // specialize
    }

    @Override
    @ForceInline
    public Long128Vector rearrange(VectorShuffle<Long> s) {
        return (Long128Vector)
            super.rearrangeTemplate(Long128Shuffle.class,
                                    (Long128Shuffle) s);  // specialize
    }

    @Override
    @ForceInline
    public Long128Vector rearrange(VectorShuffle<Long> shuffle,
                                  VectorMask<Long> m) {
        return (Long128Vector)
            super.rearrangeTemplate(Long128Shuffle.class,
                                    (Long128Shuffle) shuffle,
                                    (Long128Mask) m);  // specialize
    }

    @Override
    @ForceInline
    public Long128Vector rearrange(VectorShuffle<Long> s,
                                  Vector<Long> v) {
        return (Long128Vector)
            super.rearrangeTemplate(Long128Shuffle.class,
                                    (Long128Shuffle) s,
                                    (Long128Vector) v);  // specialize
    }

    @Override
    @ForceInline
    public Long128Vector selectFrom(Vector<Long> v) {
        return (Long128Vector)
            super.selectFromTemplate((Long128Vector) v);  // specialize
    }

    @Override
    @ForceInline
    public Long128Vector selectFrom(Vector<Long> v,
                                   VectorMask<Long> m) {
        return (Long128Vector)
            super.selectFromTemplate((Long128Vector) v,
                                     (Long128Mask) m);  // specialize
    }


    @ForceInline
    @Override
    public long lane(int i) {
        switch(i) {
            case 0: return laneHelper(0);
            case 1: return laneHelper(1);
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
    public Long128Vector withLane(int i, long e) {
        switch (i) {
            case 0: return withLaneHelper(0, e);
            case 1: return withLaneHelper(1, e);
            default: throw new IllegalArgumentException("Index " + i + " must be zero or positive, and less than " + VLENGTH);
        }
    }

    public Long128Vector withLaneHelper(int i, long e) {
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

    static final class Long128Mask extends AbstractMask<Long> {
        static final int VLENGTH = VSPECIES.laneCount();    // used by the JVM
        static final Class<Long> ETYPE = long.class; // used by the JVM

        Long128Mask(boolean[] bits) {
            this(bits, 0);
        }

        Long128Mask(boolean[] bits, int offset) {
            super(prepare(bits, offset));
        }

        Long128Mask(boolean val) {
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
        Long128Mask uOp(MUnOp f) {
            boolean[] res = new boolean[vspecies().laneCount()];
            boolean[] bits = getBits();
            for (int i = 0; i < res.length; i++) {
                res[i] = f.apply(i, bits[i]);
            }
            return new Long128Mask(res);
        }

        @Override
        Long128Mask bOp(VectorMask<Long> m, MBinOp f) {
            boolean[] res = new boolean[vspecies().laneCount()];
            boolean[] bits = getBits();
            boolean[] mbits = ((Long128Mask)m).getBits();
            for (int i = 0; i < res.length; i++) {
                res[i] = f.apply(i, bits[i], mbits[i]);
            }
            return new Long128Mask(res);
        }

        @ForceInline
        @Override
        public final
        Long128Vector toVector() {
            return (Long128Vector) super.toVectorTemplate();  // specialize
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
                    Long128Mask::defaultMaskCast);
            }
            return this.defaultMaskCast(species);
        }

        @Override
        @ForceInline
        public Long128Mask eq(VectorMask<Long> mask) {
            Objects.requireNonNull(mask);
            Long128Mask m = (Long128Mask)mask;
            return xor(m.not());
        }

        // Unary operations

        @Override
        @ForceInline
        public Long128Mask not() {
            return xor(maskAll(true));
        }

        // Binary operations

        @Override
        @ForceInline
        public Long128Mask and(VectorMask<Long> mask) {
            Objects.requireNonNull(mask);
            Long128Mask m = (Long128Mask)mask;
            return VectorSupport.binaryOp(VECTOR_OP_AND, Long128Mask.class, long.class, VLENGTH,
                                             this, m,
                                             (m1, m2) -> m1.bOp(m2, (i, a, b) -> a & b));
        }

        @Override
        @ForceInline
        public Long128Mask or(VectorMask<Long> mask) {
            Objects.requireNonNull(mask);
            Long128Mask m = (Long128Mask)mask;
            return VectorSupport.binaryOp(VECTOR_OP_OR, Long128Mask.class, long.class, VLENGTH,
                                             this, m,
                                             (m1, m2) -> m1.bOp(m2, (i, a, b) -> a | b));
        }

        @ForceInline
        /* package-private */
        Long128Mask xor(VectorMask<Long> mask) {
            Objects.requireNonNull(mask);
            Long128Mask m = (Long128Mask)mask;
            return VectorSupport.binaryOp(VECTOR_OP_XOR, Long128Mask.class, long.class, VLENGTH,
                                          this, m,
                                          (m1, m2) -> m1.bOp(m2, (i, a, b) -> a ^ b));
        }

        // Mask Query operations

        @Override
        @ForceInline
        public int trueCount() {
            return VectorSupport.maskReductionCoerced(VECTOR_OP_MASK_TRUECOUNT, Long128Mask.class, long.class, VLENGTH, this,
                                                      (m) -> trueCountHelper(((Long128Mask)m).getBits()));
        }

        @Override
        @ForceInline
        public int firstTrue() {
            return VectorSupport.maskReductionCoerced(VECTOR_OP_MASK_FIRSTTRUE, Long128Mask.class, long.class, VLENGTH, this,
                                                      (m) -> firstTrueHelper(((Long128Mask)m).getBits()));
        }

        @Override
        @ForceInline
        public int lastTrue() {
            return VectorSupport.maskReductionCoerced(VECTOR_OP_MASK_LASTTRUE, Long128Mask.class, long.class, VLENGTH, this,
                                                      (m) -> lastTrueHelper(((Long128Mask)m).getBits()));
        }

        // Reductions

        @Override
        @ForceInline
        public boolean anyTrue() {
            return VectorSupport.test(BT_ne, Long128Mask.class, long.class, VLENGTH,
                                         this, vspecies().maskAll(true),
                                         (m, __) -> anyTrueHelper(((Long128Mask)m).getBits()));
        }

        @Override
        @ForceInline
        public boolean allTrue() {
            return VectorSupport.test(BT_overflow, Long128Mask.class, long.class, VLENGTH,
                                         this, vspecies().maskAll(true),
                                         (m, __) -> allTrueHelper(((Long128Mask)m).getBits()));
        }

        @ForceInline
        /*package-private*/
        static Long128Mask maskAll(boolean bit) {
            return VectorSupport.broadcastCoerced(Long128Mask.class, long.class, VLENGTH,
                                                  (bit ? -1 : 0), null,
                                                  (v, __) -> (v != 0 ? TRUE_MASK : FALSE_MASK));
        }
        private static final Long128Mask  TRUE_MASK = new Long128Mask(true);
        private static final Long128Mask FALSE_MASK = new Long128Mask(false);

    }

    // Shuffle

    static final class Long128Shuffle extends AbstractShuffle<Long> {
        static final int VLENGTH = VSPECIES.laneCount();    // used by the JVM
        static final Class<Long> ETYPE = long.class; // used by the JVM

        Long128Shuffle(byte[] reorder) {
            super(VLENGTH, reorder);
        }

        public Long128Shuffle(int[] reorder) {
            super(VLENGTH, reorder);
        }

        public Long128Shuffle(int[] reorder, int i) {
            super(VLENGTH, reorder, i);
        }

        public Long128Shuffle(IntUnaryOperator fn) {
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
        static final Long128Shuffle IOTA = new Long128Shuffle(IDENTITY);

        @Override
        @ForceInline
        public Long128Vector toVector() {
            return VectorSupport.shuffleToVector(VCLASS, ETYPE, Long128Shuffle.class, this, VLENGTH,
                                                    (s) -> ((Long128Vector)(((AbstractShuffle<Long>)(s)).toVectorTemplate())));
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
        public Long128Shuffle rearrange(VectorShuffle<Long> shuffle) {
            Long128Shuffle s = (Long128Shuffle) shuffle;
            byte[] reorder1 = reorder();
            byte[] reorder2 = s.reorder();
            byte[] r = new byte[reorder1.length];
            for (int i = 0; i < reorder1.length; i++) {
                int ssi = reorder2[i];
                r[i] = reorder1[ssi];  // throws on exceptional index
            }
            return new Long128Shuffle(r);
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
