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
final class FloatMaxVector extends FloatVector {
    static final FloatSpecies VSPECIES =
        (FloatSpecies) FloatVector.SPECIES_MAX;

    static final VectorShape VSHAPE =
        VSPECIES.vectorShape();

    static final Class<FloatMaxVector> VCLASS = FloatMaxVector.class;

    static final int VSIZE = VSPECIES.vectorBitSize();

    static final int VLENGTH = VSPECIES.laneCount(); // used by the JVM

    static final Class<Float> ETYPE = float.class; // used by the JVM

    FloatMaxVector(float[] v) {
        super(v);
    }

    // For compatibility as FloatMaxVector::new,
    // stored into species.vectorFactory.
    FloatMaxVector(Object v) {
        this((float[]) v);
    }

    static final FloatMaxVector ZERO = new FloatMaxVector(new float[VLENGTH]);
    static final FloatMaxVector IOTA = new FloatMaxVector(VSPECIES.iotaArray());

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
    public FloatSpecies vspecies() {
        // ISSUE:  This should probably be a @Stable
        // field inside AbstractVector, rather than
        // a megamorphic method.
        return VSPECIES;
    }

    @ForceInline
    @Override
    public final Class<Float> elementType() { return float.class; }

    @ForceInline
    @Override
    public final int elementSize() { return Float.SIZE; }

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
    float[] vec() {
        return (float[])getPayload();
    }

    // Virtualized constructors

    @Override
    @ForceInline
    public final FloatMaxVector broadcast(float e) {
        return (FloatMaxVector) super.broadcastTemplate(e);  // specialize
    }

    @Override
    @ForceInline
    public final FloatMaxVector broadcast(long e) {
        return (FloatMaxVector) super.broadcastTemplate(e);  // specialize
    }

    @Override
    @ForceInline
    FloatMaxMask maskFromArray(boolean[] bits) {
        return new FloatMaxMask(bits);
    }

    @Override
    @ForceInline
    FloatMaxShuffle iotaShuffle() { return FloatMaxShuffle.IOTA; }

    @ForceInline
    FloatMaxShuffle iotaShuffle(int start, int step, boolean wrap) {
      if (wrap) {
        return (FloatMaxShuffle)VectorSupport.shuffleIota(ETYPE, FloatMaxShuffle.class, VSPECIES, VLENGTH, start, step, 1,
                (l, lstart, lstep, s) -> s.shuffleFromOp(i -> (VectorIntrinsics.wrapToRange(i*lstep + lstart, l))));
      } else {
        return (FloatMaxShuffle)VectorSupport.shuffleIota(ETYPE, FloatMaxShuffle.class, VSPECIES, VLENGTH, start, step, 0,
                (l, lstart, lstep, s) -> s.shuffleFromOp(i -> (i*lstep + lstart)));
      }
    }

    @Override
    @ForceInline
    FloatMaxShuffle shuffleFromBytes(byte[] reorder) { return new FloatMaxShuffle(reorder); }

    @Override
    @ForceInline
    FloatMaxShuffle shuffleFromArray(int[] indexes, int i) { return new FloatMaxShuffle(indexes, i); }

    @Override
    @ForceInline
    FloatMaxShuffle shuffleFromOp(IntUnaryOperator fn) { return new FloatMaxShuffle(fn); }

    // Make a vector of the same species but the given elements:
    @ForceInline
    final @Override
    FloatMaxVector vectorFactory(float[] vec) {
        return new FloatMaxVector(vec);
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
    FloatMaxVector uOp(FUnOp f) {
        return (FloatMaxVector) super.uOpTemplate(f);  // specialize
    }

    @ForceInline
    final @Override
    FloatMaxVector uOp(VectorMask<Float> m, FUnOp f) {
        return (FloatMaxVector)
            super.uOpTemplate((FloatMaxMask)m, f);  // specialize
    }

    // Binary operator

    @ForceInline
    final @Override
    FloatMaxVector bOp(Vector<Float> v, FBinOp f) {
        return (FloatMaxVector) super.bOpTemplate((FloatMaxVector)v, f);  // specialize
    }

    @ForceInline
    final @Override
    FloatMaxVector bOp(Vector<Float> v,
                     VectorMask<Float> m, FBinOp f) {
        return (FloatMaxVector)
            super.bOpTemplate((FloatMaxVector)v, (FloatMaxMask)m,
                              f);  // specialize
    }

    // Ternary operator

    @ForceInline
    final @Override
    FloatMaxVector tOp(Vector<Float> v1, Vector<Float> v2, FTriOp f) {
        return (FloatMaxVector)
            super.tOpTemplate((FloatMaxVector)v1, (FloatMaxVector)v2,
                              f);  // specialize
    }

    @ForceInline
    final @Override
    FloatMaxVector tOp(Vector<Float> v1, Vector<Float> v2,
                     VectorMask<Float> m, FTriOp f) {
        return (FloatMaxVector)
            super.tOpTemplate((FloatMaxVector)v1, (FloatMaxVector)v2,
                              (FloatMaxMask)m, f);  // specialize
    }

    @ForceInline
    final @Override
    float rOp(float v, FBinOp f) {
        return super.rOpTemplate(v, f);  // specialize
    }

    @Override
    @ForceInline
    public final <F>
    Vector<F> convertShape(VectorOperators.Conversion<Float,F> conv,
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
    public FloatMaxVector lanewise(Unary op) {
        return (FloatMaxVector) super.lanewiseTemplate(op);  // specialize
    }

    @Override
    @ForceInline
    public FloatMaxVector lanewise(Binary op, Vector<Float> v) {
        return (FloatMaxVector) super.lanewiseTemplate(op, v);  // specialize
    }


    /*package-private*/
    @Override
    @ForceInline
    public final
    FloatMaxVector
    lanewise(VectorOperators.Ternary op, Vector<Float> v1, Vector<Float> v2) {
        return (FloatMaxVector) super.lanewiseTemplate(op, v1, v2);  // specialize
    }

    @Override
    @ForceInline
    public final
    FloatMaxVector addIndex(int scale) {
        return (FloatMaxVector) super.addIndexTemplate(scale);  // specialize
    }

    // Type specific horizontal reductions

    @Override
    @ForceInline
    public final float reduceLanes(VectorOperators.Associative op) {
        return super.reduceLanesTemplate(op);  // specialized
    }

    @Override
    @ForceInline
    public final float reduceLanes(VectorOperators.Associative op,
                                    VectorMask<Float> m) {
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
                                        VectorMask<Float> m) {
        return (long) super.reduceLanesTemplate(op, m);  // specialized
    }

    @ForceInline
    public VectorShuffle<Float> toShuffle() {
        return super.toShuffleTemplate(FloatMaxShuffle.class); // specialize
    }

    // Specialized unary testing

    @Override
    @ForceInline
    public final FloatMaxMask test(Test op) {
        return super.testTemplate(FloatMaxMask.class, op);  // specialize
    }

    // Specialized comparisons

    @Override
    @ForceInline
    public final FloatMaxMask compare(Comparison op, Vector<Float> v) {
        return super.compareTemplate(FloatMaxMask.class, op, v);  // specialize
    }

    @Override
    @ForceInline
    public final FloatMaxMask compare(Comparison op, float s) {
        return super.compareTemplate(FloatMaxMask.class, op, s);  // specialize
    }

    @Override
    @ForceInline
    public final FloatMaxMask compare(Comparison op, long s) {
        return super.compareTemplate(FloatMaxMask.class, op, s);  // specialize
    }

    @Override
    @ForceInline
    public FloatMaxVector blend(Vector<Float> v, VectorMask<Float> m) {
        return (FloatMaxVector)
            super.blendTemplate(FloatMaxMask.class,
                                (FloatMaxVector) v,
                                (FloatMaxMask) m);  // specialize
    }

    @Override
    @ForceInline
    public FloatMaxVector slice(int origin, Vector<Float> v) {
        return (FloatMaxVector) super.sliceTemplate(origin, v);  // specialize
    }

    @Override
    @ForceInline
    public FloatMaxVector slice(int origin) {
        return (FloatMaxVector) super.sliceTemplate(origin);  // specialize
    }

    @Override
    @ForceInline
    public FloatMaxVector unslice(int origin, Vector<Float> w, int part) {
        return (FloatMaxVector) super.unsliceTemplate(origin, w, part);  // specialize
    }

    @Override
    @ForceInline
    public FloatMaxVector unslice(int origin, Vector<Float> w, int part, VectorMask<Float> m) {
        return (FloatMaxVector)
            super.unsliceTemplate(FloatMaxMask.class,
                                  origin, w, part,
                                  (FloatMaxMask) m);  // specialize
    }

    @Override
    @ForceInline
    public FloatMaxVector unslice(int origin) {
        return (FloatMaxVector) super.unsliceTemplate(origin);  // specialize
    }

    @Override
    @ForceInline
    public FloatMaxVector rearrange(VectorShuffle<Float> s) {
        return (FloatMaxVector)
            super.rearrangeTemplate(FloatMaxShuffle.class,
                                    (FloatMaxShuffle) s);  // specialize
    }

    @Override
    @ForceInline
    public FloatMaxVector rearrange(VectorShuffle<Float> shuffle,
                                  VectorMask<Float> m) {
        return (FloatMaxVector)
            super.rearrangeTemplate(FloatMaxShuffle.class,
                                    (FloatMaxShuffle) shuffle,
                                    (FloatMaxMask) m);  // specialize
    }

    @Override
    @ForceInline
    public FloatMaxVector rearrange(VectorShuffle<Float> s,
                                  Vector<Float> v) {
        return (FloatMaxVector)
            super.rearrangeTemplate(FloatMaxShuffle.class,
                                    (FloatMaxShuffle) s,
                                    (FloatMaxVector) v);  // specialize
    }

    @Override
    @ForceInline
    public FloatMaxVector selectFrom(Vector<Float> v) {
        return (FloatMaxVector)
            super.selectFromTemplate((FloatMaxVector) v);  // specialize
    }

    @Override
    @ForceInline
    public FloatMaxVector selectFrom(Vector<Float> v,
                                   VectorMask<Float> m) {
        return (FloatMaxVector)
            super.selectFromTemplate((FloatMaxVector) v,
                                     (FloatMaxMask) m);  // specialize
    }


    @ForceInline
    @Override
    public float lane(int i) {
        if (i < 0 || i >= VLENGTH) {
            throw new IllegalArgumentException("Index " + i + " must be zero or positive, and less than " + VLENGTH);
        }
        int bits = laneHelper(i);
        return Float.intBitsToFloat(bits);
    }

    public int laneHelper(int i) {
        return (int) VectorSupport.extract(
                     VCLASS, ETYPE, VLENGTH,
                     this, i,
                     (vec, ix) -> {
                     float[] vecarr = vec.vec();
                     return (long)Float.floatToIntBits(vecarr[ix]);
                     });
    }

    @ForceInline
    @Override
    public FloatMaxVector withLane(int i, float e) {
        if (i < 0 || i >= VLENGTH) {
            throw new IllegalArgumentException("Index " + i + " must be zero or positive, and less than " + VLENGTH);
        }
        return withLaneHelper(i, e);
    }

    public FloatMaxVector withLaneHelper(int i, float e) {
        return VectorSupport.insert(
                                VCLASS, ETYPE, VLENGTH,
                                this, i, (long)Float.floatToIntBits(e),
                                (v, ix, bits) -> {
                                    float[] res = v.vec().clone();
                                    res[ix] = Float.intBitsToFloat((int)bits);
                                    return v.vectorFactory(res);
                                });
    }

    // Mask

    static final class FloatMaxMask extends AbstractMask<Float> {
        static final int VLENGTH = VSPECIES.laneCount();    // used by the JVM
        static final Class<Float> ETYPE = float.class; // used by the JVM

        FloatMaxMask(boolean[] bits) {
            this(bits, 0);
        }

        FloatMaxMask(boolean[] bits, int offset) {
            super(prepare(bits, offset));
        }

        FloatMaxMask(boolean val) {
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
        public FloatSpecies vspecies() {
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
        FloatMaxMask uOp(MUnOp f) {
            boolean[] res = new boolean[vspecies().laneCount()];
            boolean[] bits = getBits();
            for (int i = 0; i < res.length; i++) {
                res[i] = f.apply(i, bits[i]);
            }
            return new FloatMaxMask(res);
        }

        @Override
        FloatMaxMask bOp(VectorMask<Float> m, MBinOp f) {
            boolean[] res = new boolean[vspecies().laneCount()];
            boolean[] bits = getBits();
            boolean[] mbits = ((FloatMaxMask)m).getBits();
            for (int i = 0; i < res.length; i++) {
                res[i] = f.apply(i, bits[i], mbits[i]);
            }
            return new FloatMaxMask(res);
        }

        @ForceInline
        @Override
        public final
        FloatMaxVector toVector() {
            return (FloatMaxVector) super.toVectorTemplate();  // specialize
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
                    FloatMaxMask::defaultMaskCast);
            }
            return this.defaultMaskCast(species);
        }

        @Override
        @ForceInline
        public FloatMaxMask eq(VectorMask<Float> mask) {
            Objects.requireNonNull(mask);
            FloatMaxMask m = (FloatMaxMask)mask;
            return xor(m.not());
        }

        // Unary operations

        @Override
        @ForceInline
        public FloatMaxMask not() {
            return xor(maskAll(true));
        }

        // Binary operations

        @Override
        @ForceInline
        public FloatMaxMask and(VectorMask<Float> mask) {
            Objects.requireNonNull(mask);
            FloatMaxMask m = (FloatMaxMask)mask;
            return VectorSupport.binaryOp(VECTOR_OP_AND, FloatMaxMask.class, int.class, VLENGTH,
                                             this, m,
                                             (m1, m2) -> m1.bOp(m2, (i, a, b) -> a & b));
        }

        @Override
        @ForceInline
        public FloatMaxMask or(VectorMask<Float> mask) {
            Objects.requireNonNull(mask);
            FloatMaxMask m = (FloatMaxMask)mask;
            return VectorSupport.binaryOp(VECTOR_OP_OR, FloatMaxMask.class, int.class, VLENGTH,
                                             this, m,
                                             (m1, m2) -> m1.bOp(m2, (i, a, b) -> a | b));
        }

        @ForceInline
        /* package-private */
        FloatMaxMask xor(VectorMask<Float> mask) {
            Objects.requireNonNull(mask);
            FloatMaxMask m = (FloatMaxMask)mask;
            return VectorSupport.binaryOp(VECTOR_OP_XOR, FloatMaxMask.class, int.class, VLENGTH,
                                          this, m,
                                          (m1, m2) -> m1.bOp(m2, (i, a, b) -> a ^ b));
        }

        // Mask Query operations

        @Override
        @ForceInline
        public int trueCount() {
            return VectorSupport.maskReductionCoerced(VECTOR_OP_MASK_TRUECOUNT, FloatMaxMask.class, int.class, VLENGTH, this,
                                                      (m) -> trueCountHelper(((FloatMaxMask)m).getBits()));
        }

        @Override
        @ForceInline
        public int firstTrue() {
            return VectorSupport.maskReductionCoerced(VECTOR_OP_MASK_FIRSTTRUE, FloatMaxMask.class, int.class, VLENGTH, this,
                                                      (m) -> firstTrueHelper(((FloatMaxMask)m).getBits()));
        }

        @Override
        @ForceInline
        public int lastTrue() {
            return VectorSupport.maskReductionCoerced(VECTOR_OP_MASK_LASTTRUE, FloatMaxMask.class, int.class, VLENGTH, this,
                                                      (m) -> lastTrueHelper(((FloatMaxMask)m).getBits()));
        }

        // Reductions

        @Override
        @ForceInline
        public boolean anyTrue() {
            return VectorSupport.test(BT_ne, FloatMaxMask.class, int.class, VLENGTH,
                                         this, vspecies().maskAll(true),
                                         (m, __) -> anyTrueHelper(((FloatMaxMask)m).getBits()));
        }

        @Override
        @ForceInline
        public boolean allTrue() {
            return VectorSupport.test(BT_overflow, FloatMaxMask.class, int.class, VLENGTH,
                                         this, vspecies().maskAll(true),
                                         (m, __) -> allTrueHelper(((FloatMaxMask)m).getBits()));
        }

        @ForceInline
        /*package-private*/
        static FloatMaxMask maskAll(boolean bit) {
            return VectorSupport.broadcastCoerced(FloatMaxMask.class, int.class, VLENGTH,
                                                  (bit ? -1 : 0), null,
                                                  (v, __) -> (v != 0 ? TRUE_MASK : FALSE_MASK));
        }
        private static final FloatMaxMask  TRUE_MASK = new FloatMaxMask(true);
        private static final FloatMaxMask FALSE_MASK = new FloatMaxMask(false);

    }

    // Shuffle

    static final class FloatMaxShuffle extends AbstractShuffle<Float> {
        static final int VLENGTH = VSPECIES.laneCount();    // used by the JVM
        static final Class<Float> ETYPE = float.class; // used by the JVM

        FloatMaxShuffle(byte[] reorder) {
            super(VLENGTH, reorder);
        }

        public FloatMaxShuffle(int[] reorder) {
            super(VLENGTH, reorder);
        }

        public FloatMaxShuffle(int[] reorder, int i) {
            super(VLENGTH, reorder, i);
        }

        public FloatMaxShuffle(IntUnaryOperator fn) {
            super(VLENGTH, fn);
        }

        @Override
        public FloatSpecies vspecies() {
            return VSPECIES;
        }

        static {
            // There must be enough bits in the shuffle lanes to encode
            // VLENGTH valid indexes and VLENGTH exceptional ones.
            assert(VLENGTH < Byte.MAX_VALUE);
            assert(Byte.MIN_VALUE <= -VLENGTH);
        }
        static final FloatMaxShuffle IOTA = new FloatMaxShuffle(IDENTITY);

        @Override
        @ForceInline
        public FloatMaxVector toVector() {
            return VectorSupport.shuffleToVector(VCLASS, ETYPE, FloatMaxShuffle.class, this, VLENGTH,
                                                    (s) -> ((FloatMaxVector)(((AbstractShuffle<Float>)(s)).toVectorTemplate())));
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
        public FloatMaxShuffle rearrange(VectorShuffle<Float> shuffle) {
            FloatMaxShuffle s = (FloatMaxShuffle) shuffle;
            byte[] reorder1 = reorder();
            byte[] reorder2 = s.reorder();
            byte[] r = new byte[reorder1.length];
            for (int i = 0; i < reorder1.length; i++) {
                int ssi = reorder2[i];
                r[i] = reorder1[ssi];  // throws on exceptional index
            }
            return new FloatMaxShuffle(r);
        }
    }

    // ================================================

    // Specialized low-level memory operations.

    @ForceInline
    @Override
    final
    FloatVector fromArray0(float[] a, int offset) {
        return super.fromArray0Template(a, offset);  // specialize
    }



    @ForceInline
    @Override
    final
    FloatVector fromByteArray0(byte[] a, int offset) {
        return super.fromByteArray0Template(a, offset);  // specialize
    }

    @ForceInline
    @Override
    final
    FloatVector fromByteBuffer0(ByteBuffer bb, int offset) {
        return super.fromByteBuffer0Template(bb, offset);  // specialize
    }

    @ForceInline
    @Override
    final
    void intoArray0(float[] a, int offset) {
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
