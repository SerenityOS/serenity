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
final class Int64Vector extends IntVector {
    static final IntSpecies VSPECIES =
        (IntSpecies) IntVector.SPECIES_64;

    static final VectorShape VSHAPE =
        VSPECIES.vectorShape();

    static final Class<Int64Vector> VCLASS = Int64Vector.class;

    static final int VSIZE = VSPECIES.vectorBitSize();

    static final int VLENGTH = VSPECIES.laneCount(); // used by the JVM

    static final Class<Integer> ETYPE = int.class; // used by the JVM

    Int64Vector(int[] v) {
        super(v);
    }

    // For compatibility as Int64Vector::new,
    // stored into species.vectorFactory.
    Int64Vector(Object v) {
        this((int[]) v);
    }

    static final Int64Vector ZERO = new Int64Vector(new int[VLENGTH]);
    static final Int64Vector IOTA = new Int64Vector(VSPECIES.iotaArray());

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
    public IntSpecies vspecies() {
        // ISSUE:  This should probably be a @Stable
        // field inside AbstractVector, rather than
        // a megamorphic method.
        return VSPECIES;
    }

    @ForceInline
    @Override
    public final Class<Integer> elementType() { return int.class; }

    @ForceInline
    @Override
    public final int elementSize() { return Integer.SIZE; }

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
    int[] vec() {
        return (int[])getPayload();
    }

    // Virtualized constructors

    @Override
    @ForceInline
    public final Int64Vector broadcast(int e) {
        return (Int64Vector) super.broadcastTemplate(e);  // specialize
    }

    @Override
    @ForceInline
    public final Int64Vector broadcast(long e) {
        return (Int64Vector) super.broadcastTemplate(e);  // specialize
    }

    @Override
    @ForceInline
    Int64Mask maskFromArray(boolean[] bits) {
        return new Int64Mask(bits);
    }

    @Override
    @ForceInline
    Int64Shuffle iotaShuffle() { return Int64Shuffle.IOTA; }

    @ForceInline
    Int64Shuffle iotaShuffle(int start, int step, boolean wrap) {
      if (wrap) {
        return (Int64Shuffle)VectorSupport.shuffleIota(ETYPE, Int64Shuffle.class, VSPECIES, VLENGTH, start, step, 1,
                (l, lstart, lstep, s) -> s.shuffleFromOp(i -> (VectorIntrinsics.wrapToRange(i*lstep + lstart, l))));
      } else {
        return (Int64Shuffle)VectorSupport.shuffleIota(ETYPE, Int64Shuffle.class, VSPECIES, VLENGTH, start, step, 0,
                (l, lstart, lstep, s) -> s.shuffleFromOp(i -> (i*lstep + lstart)));
      }
    }

    @Override
    @ForceInline
    Int64Shuffle shuffleFromBytes(byte[] reorder) { return new Int64Shuffle(reorder); }

    @Override
    @ForceInline
    Int64Shuffle shuffleFromArray(int[] indexes, int i) { return new Int64Shuffle(indexes, i); }

    @Override
    @ForceInline
    Int64Shuffle shuffleFromOp(IntUnaryOperator fn) { return new Int64Shuffle(fn); }

    // Make a vector of the same species but the given elements:
    @ForceInline
    final @Override
    Int64Vector vectorFactory(int[] vec) {
        return new Int64Vector(vec);
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
    Int64Vector uOp(FUnOp f) {
        return (Int64Vector) super.uOpTemplate(f);  // specialize
    }

    @ForceInline
    final @Override
    Int64Vector uOp(VectorMask<Integer> m, FUnOp f) {
        return (Int64Vector)
            super.uOpTemplate((Int64Mask)m, f);  // specialize
    }

    // Binary operator

    @ForceInline
    final @Override
    Int64Vector bOp(Vector<Integer> v, FBinOp f) {
        return (Int64Vector) super.bOpTemplate((Int64Vector)v, f);  // specialize
    }

    @ForceInline
    final @Override
    Int64Vector bOp(Vector<Integer> v,
                     VectorMask<Integer> m, FBinOp f) {
        return (Int64Vector)
            super.bOpTemplate((Int64Vector)v, (Int64Mask)m,
                              f);  // specialize
    }

    // Ternary operator

    @ForceInline
    final @Override
    Int64Vector tOp(Vector<Integer> v1, Vector<Integer> v2, FTriOp f) {
        return (Int64Vector)
            super.tOpTemplate((Int64Vector)v1, (Int64Vector)v2,
                              f);  // specialize
    }

    @ForceInline
    final @Override
    Int64Vector tOp(Vector<Integer> v1, Vector<Integer> v2,
                     VectorMask<Integer> m, FTriOp f) {
        return (Int64Vector)
            super.tOpTemplate((Int64Vector)v1, (Int64Vector)v2,
                              (Int64Mask)m, f);  // specialize
    }

    @ForceInline
    final @Override
    int rOp(int v, FBinOp f) {
        return super.rOpTemplate(v, f);  // specialize
    }

    @Override
    @ForceInline
    public final <F>
    Vector<F> convertShape(VectorOperators.Conversion<Integer,F> conv,
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
    public Int64Vector lanewise(Unary op) {
        return (Int64Vector) super.lanewiseTemplate(op);  // specialize
    }

    @Override
    @ForceInline
    public Int64Vector lanewise(Binary op, Vector<Integer> v) {
        return (Int64Vector) super.lanewiseTemplate(op, v);  // specialize
    }

    /*package-private*/
    @Override
    @ForceInline Int64Vector
    lanewiseShift(VectorOperators.Binary op, int e) {
        return (Int64Vector) super.lanewiseShiftTemplate(op, e);  // specialize
    }

    /*package-private*/
    @Override
    @ForceInline
    public final
    Int64Vector
    lanewise(VectorOperators.Ternary op, Vector<Integer> v1, Vector<Integer> v2) {
        return (Int64Vector) super.lanewiseTemplate(op, v1, v2);  // specialize
    }

    @Override
    @ForceInline
    public final
    Int64Vector addIndex(int scale) {
        return (Int64Vector) super.addIndexTemplate(scale);  // specialize
    }

    // Type specific horizontal reductions

    @Override
    @ForceInline
    public final int reduceLanes(VectorOperators.Associative op) {
        return super.reduceLanesTemplate(op);  // specialized
    }

    @Override
    @ForceInline
    public final int reduceLanes(VectorOperators.Associative op,
                                    VectorMask<Integer> m) {
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
                                        VectorMask<Integer> m) {
        return (long) super.reduceLanesTemplate(op, m);  // specialized
    }

    @ForceInline
    public VectorShuffle<Integer> toShuffle() {
        return super.toShuffleTemplate(Int64Shuffle.class); // specialize
    }

    // Specialized unary testing

    @Override
    @ForceInline
    public final Int64Mask test(Test op) {
        return super.testTemplate(Int64Mask.class, op);  // specialize
    }

    // Specialized comparisons

    @Override
    @ForceInline
    public final Int64Mask compare(Comparison op, Vector<Integer> v) {
        return super.compareTemplate(Int64Mask.class, op, v);  // specialize
    }

    @Override
    @ForceInline
    public final Int64Mask compare(Comparison op, int s) {
        return super.compareTemplate(Int64Mask.class, op, s);  // specialize
    }

    @Override
    @ForceInline
    public final Int64Mask compare(Comparison op, long s) {
        return super.compareTemplate(Int64Mask.class, op, s);  // specialize
    }

    @Override
    @ForceInline
    public Int64Vector blend(Vector<Integer> v, VectorMask<Integer> m) {
        return (Int64Vector)
            super.blendTemplate(Int64Mask.class,
                                (Int64Vector) v,
                                (Int64Mask) m);  // specialize
    }

    @Override
    @ForceInline
    public Int64Vector slice(int origin, Vector<Integer> v) {
        return (Int64Vector) super.sliceTemplate(origin, v);  // specialize
    }

    @Override
    @ForceInline
    public Int64Vector slice(int origin) {
        return (Int64Vector) super.sliceTemplate(origin);  // specialize
    }

    @Override
    @ForceInline
    public Int64Vector unslice(int origin, Vector<Integer> w, int part) {
        return (Int64Vector) super.unsliceTemplate(origin, w, part);  // specialize
    }

    @Override
    @ForceInline
    public Int64Vector unslice(int origin, Vector<Integer> w, int part, VectorMask<Integer> m) {
        return (Int64Vector)
            super.unsliceTemplate(Int64Mask.class,
                                  origin, w, part,
                                  (Int64Mask) m);  // specialize
    }

    @Override
    @ForceInline
    public Int64Vector unslice(int origin) {
        return (Int64Vector) super.unsliceTemplate(origin);  // specialize
    }

    @Override
    @ForceInline
    public Int64Vector rearrange(VectorShuffle<Integer> s) {
        return (Int64Vector)
            super.rearrangeTemplate(Int64Shuffle.class,
                                    (Int64Shuffle) s);  // specialize
    }

    @Override
    @ForceInline
    public Int64Vector rearrange(VectorShuffle<Integer> shuffle,
                                  VectorMask<Integer> m) {
        return (Int64Vector)
            super.rearrangeTemplate(Int64Shuffle.class,
                                    (Int64Shuffle) shuffle,
                                    (Int64Mask) m);  // specialize
    }

    @Override
    @ForceInline
    public Int64Vector rearrange(VectorShuffle<Integer> s,
                                  Vector<Integer> v) {
        return (Int64Vector)
            super.rearrangeTemplate(Int64Shuffle.class,
                                    (Int64Shuffle) s,
                                    (Int64Vector) v);  // specialize
    }

    @Override
    @ForceInline
    public Int64Vector selectFrom(Vector<Integer> v) {
        return (Int64Vector)
            super.selectFromTemplate((Int64Vector) v);  // specialize
    }

    @Override
    @ForceInline
    public Int64Vector selectFrom(Vector<Integer> v,
                                   VectorMask<Integer> m) {
        return (Int64Vector)
            super.selectFromTemplate((Int64Vector) v,
                                     (Int64Mask) m);  // specialize
    }


    @ForceInline
    @Override
    public int lane(int i) {
        switch(i) {
            case 0: return laneHelper(0);
            case 1: return laneHelper(1);
            default: throw new IllegalArgumentException("Index " + i + " must be zero or positive, and less than " + VLENGTH);
        }
    }

    public int laneHelper(int i) {
        return (int) VectorSupport.extract(
                                VCLASS, ETYPE, VLENGTH,
                                this, i,
                                (vec, ix) -> {
                                    int[] vecarr = vec.vec();
                                    return (long)vecarr[ix];
                                });
    }

    @ForceInline
    @Override
    public Int64Vector withLane(int i, int e) {
        switch (i) {
            case 0: return withLaneHelper(0, e);
            case 1: return withLaneHelper(1, e);
            default: throw new IllegalArgumentException("Index " + i + " must be zero or positive, and less than " + VLENGTH);
        }
    }

    public Int64Vector withLaneHelper(int i, int e) {
        return VectorSupport.insert(
                                VCLASS, ETYPE, VLENGTH,
                                this, i, (long)e,
                                (v, ix, bits) -> {
                                    int[] res = v.vec().clone();
                                    res[ix] = (int)bits;
                                    return v.vectorFactory(res);
                                });
    }

    // Mask

    static final class Int64Mask extends AbstractMask<Integer> {
        static final int VLENGTH = VSPECIES.laneCount();    // used by the JVM
        static final Class<Integer> ETYPE = int.class; // used by the JVM

        Int64Mask(boolean[] bits) {
            this(bits, 0);
        }

        Int64Mask(boolean[] bits, int offset) {
            super(prepare(bits, offset));
        }

        Int64Mask(boolean val) {
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
        public IntSpecies vspecies() {
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
        Int64Mask uOp(MUnOp f) {
            boolean[] res = new boolean[vspecies().laneCount()];
            boolean[] bits = getBits();
            for (int i = 0; i < res.length; i++) {
                res[i] = f.apply(i, bits[i]);
            }
            return new Int64Mask(res);
        }

        @Override
        Int64Mask bOp(VectorMask<Integer> m, MBinOp f) {
            boolean[] res = new boolean[vspecies().laneCount()];
            boolean[] bits = getBits();
            boolean[] mbits = ((Int64Mask)m).getBits();
            for (int i = 0; i < res.length; i++) {
                res[i] = f.apply(i, bits[i], mbits[i]);
            }
            return new Int64Mask(res);
        }

        @ForceInline
        @Override
        public final
        Int64Vector toVector() {
            return (Int64Vector) super.toVectorTemplate();  // specialize
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
                    Int64Mask::defaultMaskCast);
            }
            return this.defaultMaskCast(species);
        }

        @Override
        @ForceInline
        public Int64Mask eq(VectorMask<Integer> mask) {
            Objects.requireNonNull(mask);
            Int64Mask m = (Int64Mask)mask;
            return xor(m.not());
        }

        // Unary operations

        @Override
        @ForceInline
        public Int64Mask not() {
            return xor(maskAll(true));
        }

        // Binary operations

        @Override
        @ForceInline
        public Int64Mask and(VectorMask<Integer> mask) {
            Objects.requireNonNull(mask);
            Int64Mask m = (Int64Mask)mask;
            return VectorSupport.binaryOp(VECTOR_OP_AND, Int64Mask.class, int.class, VLENGTH,
                                             this, m,
                                             (m1, m2) -> m1.bOp(m2, (i, a, b) -> a & b));
        }

        @Override
        @ForceInline
        public Int64Mask or(VectorMask<Integer> mask) {
            Objects.requireNonNull(mask);
            Int64Mask m = (Int64Mask)mask;
            return VectorSupport.binaryOp(VECTOR_OP_OR, Int64Mask.class, int.class, VLENGTH,
                                             this, m,
                                             (m1, m2) -> m1.bOp(m2, (i, a, b) -> a | b));
        }

        @ForceInline
        /* package-private */
        Int64Mask xor(VectorMask<Integer> mask) {
            Objects.requireNonNull(mask);
            Int64Mask m = (Int64Mask)mask;
            return VectorSupport.binaryOp(VECTOR_OP_XOR, Int64Mask.class, int.class, VLENGTH,
                                          this, m,
                                          (m1, m2) -> m1.bOp(m2, (i, a, b) -> a ^ b));
        }

        // Mask Query operations

        @Override
        @ForceInline
        public int trueCount() {
            return VectorSupport.maskReductionCoerced(VECTOR_OP_MASK_TRUECOUNT, Int64Mask.class, int.class, VLENGTH, this,
                                                      (m) -> trueCountHelper(((Int64Mask)m).getBits()));
        }

        @Override
        @ForceInline
        public int firstTrue() {
            return VectorSupport.maskReductionCoerced(VECTOR_OP_MASK_FIRSTTRUE, Int64Mask.class, int.class, VLENGTH, this,
                                                      (m) -> firstTrueHelper(((Int64Mask)m).getBits()));
        }

        @Override
        @ForceInline
        public int lastTrue() {
            return VectorSupport.maskReductionCoerced(VECTOR_OP_MASK_LASTTRUE, Int64Mask.class, int.class, VLENGTH, this,
                                                      (m) -> lastTrueHelper(((Int64Mask)m).getBits()));
        }

        // Reductions

        @Override
        @ForceInline
        public boolean anyTrue() {
            return VectorSupport.test(BT_ne, Int64Mask.class, int.class, VLENGTH,
                                         this, vspecies().maskAll(true),
                                         (m, __) -> anyTrueHelper(((Int64Mask)m).getBits()));
        }

        @Override
        @ForceInline
        public boolean allTrue() {
            return VectorSupport.test(BT_overflow, Int64Mask.class, int.class, VLENGTH,
                                         this, vspecies().maskAll(true),
                                         (m, __) -> allTrueHelper(((Int64Mask)m).getBits()));
        }

        @ForceInline
        /*package-private*/
        static Int64Mask maskAll(boolean bit) {
            return VectorSupport.broadcastCoerced(Int64Mask.class, int.class, VLENGTH,
                                                  (bit ? -1 : 0), null,
                                                  (v, __) -> (v != 0 ? TRUE_MASK : FALSE_MASK));
        }
        private static final Int64Mask  TRUE_MASK = new Int64Mask(true);
        private static final Int64Mask FALSE_MASK = new Int64Mask(false);

    }

    // Shuffle

    static final class Int64Shuffle extends AbstractShuffle<Integer> {
        static final int VLENGTH = VSPECIES.laneCount();    // used by the JVM
        static final Class<Integer> ETYPE = int.class; // used by the JVM

        Int64Shuffle(byte[] reorder) {
            super(VLENGTH, reorder);
        }

        public Int64Shuffle(int[] reorder) {
            super(VLENGTH, reorder);
        }

        public Int64Shuffle(int[] reorder, int i) {
            super(VLENGTH, reorder, i);
        }

        public Int64Shuffle(IntUnaryOperator fn) {
            super(VLENGTH, fn);
        }

        @Override
        public IntSpecies vspecies() {
            return VSPECIES;
        }

        static {
            // There must be enough bits in the shuffle lanes to encode
            // VLENGTH valid indexes and VLENGTH exceptional ones.
            assert(VLENGTH < Byte.MAX_VALUE);
            assert(Byte.MIN_VALUE <= -VLENGTH);
        }
        static final Int64Shuffle IOTA = new Int64Shuffle(IDENTITY);

        @Override
        @ForceInline
        public Int64Vector toVector() {
            return VectorSupport.shuffleToVector(VCLASS, ETYPE, Int64Shuffle.class, this, VLENGTH,
                                                    (s) -> ((Int64Vector)(((AbstractShuffle<Integer>)(s)).toVectorTemplate())));
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
        public Int64Shuffle rearrange(VectorShuffle<Integer> shuffle) {
            Int64Shuffle s = (Int64Shuffle) shuffle;
            byte[] reorder1 = reorder();
            byte[] reorder2 = s.reorder();
            byte[] r = new byte[reorder1.length];
            for (int i = 0; i < reorder1.length; i++) {
                int ssi = reorder2[i];
                r[i] = reorder1[ssi];  // throws on exceptional index
            }
            return new Int64Shuffle(r);
        }
    }

    // ================================================

    // Specialized low-level memory operations.

    @ForceInline
    @Override
    final
    IntVector fromArray0(int[] a, int offset) {
        return super.fromArray0Template(a, offset);  // specialize
    }



    @ForceInline
    @Override
    final
    IntVector fromByteArray0(byte[] a, int offset) {
        return super.fromByteArray0Template(a, offset);  // specialize
    }

    @ForceInline
    @Override
    final
    IntVector fromByteBuffer0(ByteBuffer bb, int offset) {
        return super.fromByteBuffer0Template(bb, offset);  // specialize
    }

    @ForceInline
    @Override
    final
    void intoArray0(int[] a, int offset) {
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
