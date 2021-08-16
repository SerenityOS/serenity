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
final class Float64Vector extends FloatVector {
    static final FloatSpecies VSPECIES =
        (FloatSpecies) FloatVector.SPECIES_64;

    static final VectorShape VSHAPE =
        VSPECIES.vectorShape();

    static final Class<Float64Vector> VCLASS = Float64Vector.class;

    static final int VSIZE = VSPECIES.vectorBitSize();

    static final int VLENGTH = VSPECIES.laneCount(); // used by the JVM

    static final Class<Float> ETYPE = float.class; // used by the JVM

    Float64Vector(float[] v) {
        super(v);
    }

    // For compatibility as Float64Vector::new,
    // stored into species.vectorFactory.
    Float64Vector(Object v) {
        this((float[]) v);
    }

    static final Float64Vector ZERO = new Float64Vector(new float[VLENGTH]);
    static final Float64Vector IOTA = new Float64Vector(VSPECIES.iotaArray());

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
    public final Float64Vector broadcast(float e) {
        return (Float64Vector) super.broadcastTemplate(e);  // specialize
    }

    @Override
    @ForceInline
    public final Float64Vector broadcast(long e) {
        return (Float64Vector) super.broadcastTemplate(e);  // specialize
    }

    @Override
    @ForceInline
    Float64Mask maskFromArray(boolean[] bits) {
        return new Float64Mask(bits);
    }

    @Override
    @ForceInline
    Float64Shuffle iotaShuffle() { return Float64Shuffle.IOTA; }

    @ForceInline
    Float64Shuffle iotaShuffle(int start, int step, boolean wrap) {
      if (wrap) {
        return (Float64Shuffle)VectorSupport.shuffleIota(ETYPE, Float64Shuffle.class, VSPECIES, VLENGTH, start, step, 1,
                (l, lstart, lstep, s) -> s.shuffleFromOp(i -> (VectorIntrinsics.wrapToRange(i*lstep + lstart, l))));
      } else {
        return (Float64Shuffle)VectorSupport.shuffleIota(ETYPE, Float64Shuffle.class, VSPECIES, VLENGTH, start, step, 0,
                (l, lstart, lstep, s) -> s.shuffleFromOp(i -> (i*lstep + lstart)));
      }
    }

    @Override
    @ForceInline
    Float64Shuffle shuffleFromBytes(byte[] reorder) { return new Float64Shuffle(reorder); }

    @Override
    @ForceInline
    Float64Shuffle shuffleFromArray(int[] indexes, int i) { return new Float64Shuffle(indexes, i); }

    @Override
    @ForceInline
    Float64Shuffle shuffleFromOp(IntUnaryOperator fn) { return new Float64Shuffle(fn); }

    // Make a vector of the same species but the given elements:
    @ForceInline
    final @Override
    Float64Vector vectorFactory(float[] vec) {
        return new Float64Vector(vec);
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
    Float64Vector uOp(FUnOp f) {
        return (Float64Vector) super.uOpTemplate(f);  // specialize
    }

    @ForceInline
    final @Override
    Float64Vector uOp(VectorMask<Float> m, FUnOp f) {
        return (Float64Vector)
            super.uOpTemplate((Float64Mask)m, f);  // specialize
    }

    // Binary operator

    @ForceInline
    final @Override
    Float64Vector bOp(Vector<Float> v, FBinOp f) {
        return (Float64Vector) super.bOpTemplate((Float64Vector)v, f);  // specialize
    }

    @ForceInline
    final @Override
    Float64Vector bOp(Vector<Float> v,
                     VectorMask<Float> m, FBinOp f) {
        return (Float64Vector)
            super.bOpTemplate((Float64Vector)v, (Float64Mask)m,
                              f);  // specialize
    }

    // Ternary operator

    @ForceInline
    final @Override
    Float64Vector tOp(Vector<Float> v1, Vector<Float> v2, FTriOp f) {
        return (Float64Vector)
            super.tOpTemplate((Float64Vector)v1, (Float64Vector)v2,
                              f);  // specialize
    }

    @ForceInline
    final @Override
    Float64Vector tOp(Vector<Float> v1, Vector<Float> v2,
                     VectorMask<Float> m, FTriOp f) {
        return (Float64Vector)
            super.tOpTemplate((Float64Vector)v1, (Float64Vector)v2,
                              (Float64Mask)m, f);  // specialize
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
    public Float64Vector lanewise(Unary op) {
        return (Float64Vector) super.lanewiseTemplate(op);  // specialize
    }

    @Override
    @ForceInline
    public Float64Vector lanewise(Binary op, Vector<Float> v) {
        return (Float64Vector) super.lanewiseTemplate(op, v);  // specialize
    }


    /*package-private*/
    @Override
    @ForceInline
    public final
    Float64Vector
    lanewise(VectorOperators.Ternary op, Vector<Float> v1, Vector<Float> v2) {
        return (Float64Vector) super.lanewiseTemplate(op, v1, v2);  // specialize
    }

    @Override
    @ForceInline
    public final
    Float64Vector addIndex(int scale) {
        return (Float64Vector) super.addIndexTemplate(scale);  // specialize
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
        return super.toShuffleTemplate(Float64Shuffle.class); // specialize
    }

    // Specialized unary testing

    @Override
    @ForceInline
    public final Float64Mask test(Test op) {
        return super.testTemplate(Float64Mask.class, op);  // specialize
    }

    // Specialized comparisons

    @Override
    @ForceInline
    public final Float64Mask compare(Comparison op, Vector<Float> v) {
        return super.compareTemplate(Float64Mask.class, op, v);  // specialize
    }

    @Override
    @ForceInline
    public final Float64Mask compare(Comparison op, float s) {
        return super.compareTemplate(Float64Mask.class, op, s);  // specialize
    }

    @Override
    @ForceInline
    public final Float64Mask compare(Comparison op, long s) {
        return super.compareTemplate(Float64Mask.class, op, s);  // specialize
    }

    @Override
    @ForceInline
    public Float64Vector blend(Vector<Float> v, VectorMask<Float> m) {
        return (Float64Vector)
            super.blendTemplate(Float64Mask.class,
                                (Float64Vector) v,
                                (Float64Mask) m);  // specialize
    }

    @Override
    @ForceInline
    public Float64Vector slice(int origin, Vector<Float> v) {
        return (Float64Vector) super.sliceTemplate(origin, v);  // specialize
    }

    @Override
    @ForceInline
    public Float64Vector slice(int origin) {
        return (Float64Vector) super.sliceTemplate(origin);  // specialize
    }

    @Override
    @ForceInline
    public Float64Vector unslice(int origin, Vector<Float> w, int part) {
        return (Float64Vector) super.unsliceTemplate(origin, w, part);  // specialize
    }

    @Override
    @ForceInline
    public Float64Vector unslice(int origin, Vector<Float> w, int part, VectorMask<Float> m) {
        return (Float64Vector)
            super.unsliceTemplate(Float64Mask.class,
                                  origin, w, part,
                                  (Float64Mask) m);  // specialize
    }

    @Override
    @ForceInline
    public Float64Vector unslice(int origin) {
        return (Float64Vector) super.unsliceTemplate(origin);  // specialize
    }

    @Override
    @ForceInline
    public Float64Vector rearrange(VectorShuffle<Float> s) {
        return (Float64Vector)
            super.rearrangeTemplate(Float64Shuffle.class,
                                    (Float64Shuffle) s);  // specialize
    }

    @Override
    @ForceInline
    public Float64Vector rearrange(VectorShuffle<Float> shuffle,
                                  VectorMask<Float> m) {
        return (Float64Vector)
            super.rearrangeTemplate(Float64Shuffle.class,
                                    (Float64Shuffle) shuffle,
                                    (Float64Mask) m);  // specialize
    }

    @Override
    @ForceInline
    public Float64Vector rearrange(VectorShuffle<Float> s,
                                  Vector<Float> v) {
        return (Float64Vector)
            super.rearrangeTemplate(Float64Shuffle.class,
                                    (Float64Shuffle) s,
                                    (Float64Vector) v);  // specialize
    }

    @Override
    @ForceInline
    public Float64Vector selectFrom(Vector<Float> v) {
        return (Float64Vector)
            super.selectFromTemplate((Float64Vector) v);  // specialize
    }

    @Override
    @ForceInline
    public Float64Vector selectFrom(Vector<Float> v,
                                   VectorMask<Float> m) {
        return (Float64Vector)
            super.selectFromTemplate((Float64Vector) v,
                                     (Float64Mask) m);  // specialize
    }


    @ForceInline
    @Override
    public float lane(int i) {
        int bits;
        switch(i) {
            case 0: bits = laneHelper(0); break;
            case 1: bits = laneHelper(1); break;
            default: throw new IllegalArgumentException("Index " + i + " must be zero or positive, and less than " + VLENGTH);
        }
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
    public Float64Vector withLane(int i, float e) {
        switch(i) {
            case 0: return withLaneHelper(0, e);
            case 1: return withLaneHelper(1, e);
            default: throw new IllegalArgumentException("Index " + i + " must be zero or positive, and less than " + VLENGTH);
        }
    }

    public Float64Vector withLaneHelper(int i, float e) {
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

    static final class Float64Mask extends AbstractMask<Float> {
        static final int VLENGTH = VSPECIES.laneCount();    // used by the JVM
        static final Class<Float> ETYPE = float.class; // used by the JVM

        Float64Mask(boolean[] bits) {
            this(bits, 0);
        }

        Float64Mask(boolean[] bits, int offset) {
            super(prepare(bits, offset));
        }

        Float64Mask(boolean val) {
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
        Float64Mask uOp(MUnOp f) {
            boolean[] res = new boolean[vspecies().laneCount()];
            boolean[] bits = getBits();
            for (int i = 0; i < res.length; i++) {
                res[i] = f.apply(i, bits[i]);
            }
            return new Float64Mask(res);
        }

        @Override
        Float64Mask bOp(VectorMask<Float> m, MBinOp f) {
            boolean[] res = new boolean[vspecies().laneCount()];
            boolean[] bits = getBits();
            boolean[] mbits = ((Float64Mask)m).getBits();
            for (int i = 0; i < res.length; i++) {
                res[i] = f.apply(i, bits[i], mbits[i]);
            }
            return new Float64Mask(res);
        }

        @ForceInline
        @Override
        public final
        Float64Vector toVector() {
            return (Float64Vector) super.toVectorTemplate();  // specialize
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
                    Float64Mask::defaultMaskCast);
            }
            return this.defaultMaskCast(species);
        }

        @Override
        @ForceInline
        public Float64Mask eq(VectorMask<Float> mask) {
            Objects.requireNonNull(mask);
            Float64Mask m = (Float64Mask)mask;
            return xor(m.not());
        }

        // Unary operations

        @Override
        @ForceInline
        public Float64Mask not() {
            return xor(maskAll(true));
        }

        // Binary operations

        @Override
        @ForceInline
        public Float64Mask and(VectorMask<Float> mask) {
            Objects.requireNonNull(mask);
            Float64Mask m = (Float64Mask)mask;
            return VectorSupport.binaryOp(VECTOR_OP_AND, Float64Mask.class, int.class, VLENGTH,
                                             this, m,
                                             (m1, m2) -> m1.bOp(m2, (i, a, b) -> a & b));
        }

        @Override
        @ForceInline
        public Float64Mask or(VectorMask<Float> mask) {
            Objects.requireNonNull(mask);
            Float64Mask m = (Float64Mask)mask;
            return VectorSupport.binaryOp(VECTOR_OP_OR, Float64Mask.class, int.class, VLENGTH,
                                             this, m,
                                             (m1, m2) -> m1.bOp(m2, (i, a, b) -> a | b));
        }

        @ForceInline
        /* package-private */
        Float64Mask xor(VectorMask<Float> mask) {
            Objects.requireNonNull(mask);
            Float64Mask m = (Float64Mask)mask;
            return VectorSupport.binaryOp(VECTOR_OP_XOR, Float64Mask.class, int.class, VLENGTH,
                                          this, m,
                                          (m1, m2) -> m1.bOp(m2, (i, a, b) -> a ^ b));
        }

        // Mask Query operations

        @Override
        @ForceInline
        public int trueCount() {
            return VectorSupport.maskReductionCoerced(VECTOR_OP_MASK_TRUECOUNT, Float64Mask.class, int.class, VLENGTH, this,
                                                      (m) -> trueCountHelper(((Float64Mask)m).getBits()));
        }

        @Override
        @ForceInline
        public int firstTrue() {
            return VectorSupport.maskReductionCoerced(VECTOR_OP_MASK_FIRSTTRUE, Float64Mask.class, int.class, VLENGTH, this,
                                                      (m) -> firstTrueHelper(((Float64Mask)m).getBits()));
        }

        @Override
        @ForceInline
        public int lastTrue() {
            return VectorSupport.maskReductionCoerced(VECTOR_OP_MASK_LASTTRUE, Float64Mask.class, int.class, VLENGTH, this,
                                                      (m) -> lastTrueHelper(((Float64Mask)m).getBits()));
        }

        // Reductions

        @Override
        @ForceInline
        public boolean anyTrue() {
            return VectorSupport.test(BT_ne, Float64Mask.class, int.class, VLENGTH,
                                         this, vspecies().maskAll(true),
                                         (m, __) -> anyTrueHelper(((Float64Mask)m).getBits()));
        }

        @Override
        @ForceInline
        public boolean allTrue() {
            return VectorSupport.test(BT_overflow, Float64Mask.class, int.class, VLENGTH,
                                         this, vspecies().maskAll(true),
                                         (m, __) -> allTrueHelper(((Float64Mask)m).getBits()));
        }

        @ForceInline
        /*package-private*/
        static Float64Mask maskAll(boolean bit) {
            return VectorSupport.broadcastCoerced(Float64Mask.class, int.class, VLENGTH,
                                                  (bit ? -1 : 0), null,
                                                  (v, __) -> (v != 0 ? TRUE_MASK : FALSE_MASK));
        }
        private static final Float64Mask  TRUE_MASK = new Float64Mask(true);
        private static final Float64Mask FALSE_MASK = new Float64Mask(false);

    }

    // Shuffle

    static final class Float64Shuffle extends AbstractShuffle<Float> {
        static final int VLENGTH = VSPECIES.laneCount();    // used by the JVM
        static final Class<Float> ETYPE = float.class; // used by the JVM

        Float64Shuffle(byte[] reorder) {
            super(VLENGTH, reorder);
        }

        public Float64Shuffle(int[] reorder) {
            super(VLENGTH, reorder);
        }

        public Float64Shuffle(int[] reorder, int i) {
            super(VLENGTH, reorder, i);
        }

        public Float64Shuffle(IntUnaryOperator fn) {
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
        static final Float64Shuffle IOTA = new Float64Shuffle(IDENTITY);

        @Override
        @ForceInline
        public Float64Vector toVector() {
            return VectorSupport.shuffleToVector(VCLASS, ETYPE, Float64Shuffle.class, this, VLENGTH,
                                                    (s) -> ((Float64Vector)(((AbstractShuffle<Float>)(s)).toVectorTemplate())));
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
        public Float64Shuffle rearrange(VectorShuffle<Float> shuffle) {
            Float64Shuffle s = (Float64Shuffle) shuffle;
            byte[] reorder1 = reorder();
            byte[] reorder2 = s.reorder();
            byte[] r = new byte[reorder1.length];
            for (int i = 0; i < reorder1.length; i++) {
                int ssi = reorder2[i];
                r[i] = reorder1[ssi];  // throws on exceptional index
            }
            return new Float64Shuffle(r);
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
