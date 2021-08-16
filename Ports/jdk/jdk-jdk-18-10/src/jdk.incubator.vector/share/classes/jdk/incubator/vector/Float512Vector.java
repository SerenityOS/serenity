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
final class Float512Vector extends FloatVector {
    static final FloatSpecies VSPECIES =
        (FloatSpecies) FloatVector.SPECIES_512;

    static final VectorShape VSHAPE =
        VSPECIES.vectorShape();

    static final Class<Float512Vector> VCLASS = Float512Vector.class;

    static final int VSIZE = VSPECIES.vectorBitSize();

    static final int VLENGTH = VSPECIES.laneCount(); // used by the JVM

    static final Class<Float> ETYPE = float.class; // used by the JVM

    Float512Vector(float[] v) {
        super(v);
    }

    // For compatibility as Float512Vector::new,
    // stored into species.vectorFactory.
    Float512Vector(Object v) {
        this((float[]) v);
    }

    static final Float512Vector ZERO = new Float512Vector(new float[VLENGTH]);
    static final Float512Vector IOTA = new Float512Vector(VSPECIES.iotaArray());

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
    public final Float512Vector broadcast(float e) {
        return (Float512Vector) super.broadcastTemplate(e);  // specialize
    }

    @Override
    @ForceInline
    public final Float512Vector broadcast(long e) {
        return (Float512Vector) super.broadcastTemplate(e);  // specialize
    }

    @Override
    @ForceInline
    Float512Mask maskFromArray(boolean[] bits) {
        return new Float512Mask(bits);
    }

    @Override
    @ForceInline
    Float512Shuffle iotaShuffle() { return Float512Shuffle.IOTA; }

    @ForceInline
    Float512Shuffle iotaShuffle(int start, int step, boolean wrap) {
      if (wrap) {
        return (Float512Shuffle)VectorSupport.shuffleIota(ETYPE, Float512Shuffle.class, VSPECIES, VLENGTH, start, step, 1,
                (l, lstart, lstep, s) -> s.shuffleFromOp(i -> (VectorIntrinsics.wrapToRange(i*lstep + lstart, l))));
      } else {
        return (Float512Shuffle)VectorSupport.shuffleIota(ETYPE, Float512Shuffle.class, VSPECIES, VLENGTH, start, step, 0,
                (l, lstart, lstep, s) -> s.shuffleFromOp(i -> (i*lstep + lstart)));
      }
    }

    @Override
    @ForceInline
    Float512Shuffle shuffleFromBytes(byte[] reorder) { return new Float512Shuffle(reorder); }

    @Override
    @ForceInline
    Float512Shuffle shuffleFromArray(int[] indexes, int i) { return new Float512Shuffle(indexes, i); }

    @Override
    @ForceInline
    Float512Shuffle shuffleFromOp(IntUnaryOperator fn) { return new Float512Shuffle(fn); }

    // Make a vector of the same species but the given elements:
    @ForceInline
    final @Override
    Float512Vector vectorFactory(float[] vec) {
        return new Float512Vector(vec);
    }

    @ForceInline
    final @Override
    Byte512Vector asByteVectorRaw() {
        return (Byte512Vector) super.asByteVectorRawTemplate();  // specialize
    }

    @ForceInline
    final @Override
    AbstractVector<?> asVectorRaw(LaneType laneType) {
        return super.asVectorRawTemplate(laneType);  // specialize
    }

    // Unary operator

    @ForceInline
    final @Override
    Float512Vector uOp(FUnOp f) {
        return (Float512Vector) super.uOpTemplate(f);  // specialize
    }

    @ForceInline
    final @Override
    Float512Vector uOp(VectorMask<Float> m, FUnOp f) {
        return (Float512Vector)
            super.uOpTemplate((Float512Mask)m, f);  // specialize
    }

    // Binary operator

    @ForceInline
    final @Override
    Float512Vector bOp(Vector<Float> v, FBinOp f) {
        return (Float512Vector) super.bOpTemplate((Float512Vector)v, f);  // specialize
    }

    @ForceInline
    final @Override
    Float512Vector bOp(Vector<Float> v,
                     VectorMask<Float> m, FBinOp f) {
        return (Float512Vector)
            super.bOpTemplate((Float512Vector)v, (Float512Mask)m,
                              f);  // specialize
    }

    // Ternary operator

    @ForceInline
    final @Override
    Float512Vector tOp(Vector<Float> v1, Vector<Float> v2, FTriOp f) {
        return (Float512Vector)
            super.tOpTemplate((Float512Vector)v1, (Float512Vector)v2,
                              f);  // specialize
    }

    @ForceInline
    final @Override
    Float512Vector tOp(Vector<Float> v1, Vector<Float> v2,
                     VectorMask<Float> m, FTriOp f) {
        return (Float512Vector)
            super.tOpTemplate((Float512Vector)v1, (Float512Vector)v2,
                              (Float512Mask)m, f);  // specialize
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
    public Float512Vector lanewise(Unary op) {
        return (Float512Vector) super.lanewiseTemplate(op);  // specialize
    }

    @Override
    @ForceInline
    public Float512Vector lanewise(Binary op, Vector<Float> v) {
        return (Float512Vector) super.lanewiseTemplate(op, v);  // specialize
    }


    /*package-private*/
    @Override
    @ForceInline
    public final
    Float512Vector
    lanewise(VectorOperators.Ternary op, Vector<Float> v1, Vector<Float> v2) {
        return (Float512Vector) super.lanewiseTemplate(op, v1, v2);  // specialize
    }

    @Override
    @ForceInline
    public final
    Float512Vector addIndex(int scale) {
        return (Float512Vector) super.addIndexTemplate(scale);  // specialize
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
        return super.toShuffleTemplate(Float512Shuffle.class); // specialize
    }

    // Specialized unary testing

    @Override
    @ForceInline
    public final Float512Mask test(Test op) {
        return super.testTemplate(Float512Mask.class, op);  // specialize
    }

    // Specialized comparisons

    @Override
    @ForceInline
    public final Float512Mask compare(Comparison op, Vector<Float> v) {
        return super.compareTemplate(Float512Mask.class, op, v);  // specialize
    }

    @Override
    @ForceInline
    public final Float512Mask compare(Comparison op, float s) {
        return super.compareTemplate(Float512Mask.class, op, s);  // specialize
    }

    @Override
    @ForceInline
    public final Float512Mask compare(Comparison op, long s) {
        return super.compareTemplate(Float512Mask.class, op, s);  // specialize
    }

    @Override
    @ForceInline
    public Float512Vector blend(Vector<Float> v, VectorMask<Float> m) {
        return (Float512Vector)
            super.blendTemplate(Float512Mask.class,
                                (Float512Vector) v,
                                (Float512Mask) m);  // specialize
    }

    @Override
    @ForceInline
    public Float512Vector slice(int origin, Vector<Float> v) {
        return (Float512Vector) super.sliceTemplate(origin, v);  // specialize
    }

    @Override
    @ForceInline
    public Float512Vector slice(int origin) {
        return (Float512Vector) super.sliceTemplate(origin);  // specialize
    }

    @Override
    @ForceInline
    public Float512Vector unslice(int origin, Vector<Float> w, int part) {
        return (Float512Vector) super.unsliceTemplate(origin, w, part);  // specialize
    }

    @Override
    @ForceInline
    public Float512Vector unslice(int origin, Vector<Float> w, int part, VectorMask<Float> m) {
        return (Float512Vector)
            super.unsliceTemplate(Float512Mask.class,
                                  origin, w, part,
                                  (Float512Mask) m);  // specialize
    }

    @Override
    @ForceInline
    public Float512Vector unslice(int origin) {
        return (Float512Vector) super.unsliceTemplate(origin);  // specialize
    }

    @Override
    @ForceInline
    public Float512Vector rearrange(VectorShuffle<Float> s) {
        return (Float512Vector)
            super.rearrangeTemplate(Float512Shuffle.class,
                                    (Float512Shuffle) s);  // specialize
    }

    @Override
    @ForceInline
    public Float512Vector rearrange(VectorShuffle<Float> shuffle,
                                  VectorMask<Float> m) {
        return (Float512Vector)
            super.rearrangeTemplate(Float512Shuffle.class,
                                    (Float512Shuffle) shuffle,
                                    (Float512Mask) m);  // specialize
    }

    @Override
    @ForceInline
    public Float512Vector rearrange(VectorShuffle<Float> s,
                                  Vector<Float> v) {
        return (Float512Vector)
            super.rearrangeTemplate(Float512Shuffle.class,
                                    (Float512Shuffle) s,
                                    (Float512Vector) v);  // specialize
    }

    @Override
    @ForceInline
    public Float512Vector selectFrom(Vector<Float> v) {
        return (Float512Vector)
            super.selectFromTemplate((Float512Vector) v);  // specialize
    }

    @Override
    @ForceInline
    public Float512Vector selectFrom(Vector<Float> v,
                                   VectorMask<Float> m) {
        return (Float512Vector)
            super.selectFromTemplate((Float512Vector) v,
                                     (Float512Mask) m);  // specialize
    }


    @ForceInline
    @Override
    public float lane(int i) {
        int bits;
        switch(i) {
            case 0: bits = laneHelper(0); break;
            case 1: bits = laneHelper(1); break;
            case 2: bits = laneHelper(2); break;
            case 3: bits = laneHelper(3); break;
            case 4: bits = laneHelper(4); break;
            case 5: bits = laneHelper(5); break;
            case 6: bits = laneHelper(6); break;
            case 7: bits = laneHelper(7); break;
            case 8: bits = laneHelper(8); break;
            case 9: bits = laneHelper(9); break;
            case 10: bits = laneHelper(10); break;
            case 11: bits = laneHelper(11); break;
            case 12: bits = laneHelper(12); break;
            case 13: bits = laneHelper(13); break;
            case 14: bits = laneHelper(14); break;
            case 15: bits = laneHelper(15); break;
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
    public Float512Vector withLane(int i, float e) {
        switch(i) {
            case 0: return withLaneHelper(0, e);
            case 1: return withLaneHelper(1, e);
            case 2: return withLaneHelper(2, e);
            case 3: return withLaneHelper(3, e);
            case 4: return withLaneHelper(4, e);
            case 5: return withLaneHelper(5, e);
            case 6: return withLaneHelper(6, e);
            case 7: return withLaneHelper(7, e);
            case 8: return withLaneHelper(8, e);
            case 9: return withLaneHelper(9, e);
            case 10: return withLaneHelper(10, e);
            case 11: return withLaneHelper(11, e);
            case 12: return withLaneHelper(12, e);
            case 13: return withLaneHelper(13, e);
            case 14: return withLaneHelper(14, e);
            case 15: return withLaneHelper(15, e);
            default: throw new IllegalArgumentException("Index " + i + " must be zero or positive, and less than " + VLENGTH);
        }
    }

    public Float512Vector withLaneHelper(int i, float e) {
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

    static final class Float512Mask extends AbstractMask<Float> {
        static final int VLENGTH = VSPECIES.laneCount();    // used by the JVM
        static final Class<Float> ETYPE = float.class; // used by the JVM

        Float512Mask(boolean[] bits) {
            this(bits, 0);
        }

        Float512Mask(boolean[] bits, int offset) {
            super(prepare(bits, offset));
        }

        Float512Mask(boolean val) {
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
        Float512Mask uOp(MUnOp f) {
            boolean[] res = new boolean[vspecies().laneCount()];
            boolean[] bits = getBits();
            for (int i = 0; i < res.length; i++) {
                res[i] = f.apply(i, bits[i]);
            }
            return new Float512Mask(res);
        }

        @Override
        Float512Mask bOp(VectorMask<Float> m, MBinOp f) {
            boolean[] res = new boolean[vspecies().laneCount()];
            boolean[] bits = getBits();
            boolean[] mbits = ((Float512Mask)m).getBits();
            for (int i = 0; i < res.length; i++) {
                res[i] = f.apply(i, bits[i], mbits[i]);
            }
            return new Float512Mask(res);
        }

        @ForceInline
        @Override
        public final
        Float512Vector toVector() {
            return (Float512Vector) super.toVectorTemplate();  // specialize
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
                    Float512Mask::defaultMaskCast);
            }
            return this.defaultMaskCast(species);
        }

        @Override
        @ForceInline
        public Float512Mask eq(VectorMask<Float> mask) {
            Objects.requireNonNull(mask);
            Float512Mask m = (Float512Mask)mask;
            return xor(m.not());
        }

        // Unary operations

        @Override
        @ForceInline
        public Float512Mask not() {
            return xor(maskAll(true));
        }

        // Binary operations

        @Override
        @ForceInline
        public Float512Mask and(VectorMask<Float> mask) {
            Objects.requireNonNull(mask);
            Float512Mask m = (Float512Mask)mask;
            return VectorSupport.binaryOp(VECTOR_OP_AND, Float512Mask.class, int.class, VLENGTH,
                                             this, m,
                                             (m1, m2) -> m1.bOp(m2, (i, a, b) -> a & b));
        }

        @Override
        @ForceInline
        public Float512Mask or(VectorMask<Float> mask) {
            Objects.requireNonNull(mask);
            Float512Mask m = (Float512Mask)mask;
            return VectorSupport.binaryOp(VECTOR_OP_OR, Float512Mask.class, int.class, VLENGTH,
                                             this, m,
                                             (m1, m2) -> m1.bOp(m2, (i, a, b) -> a | b));
        }

        @ForceInline
        /* package-private */
        Float512Mask xor(VectorMask<Float> mask) {
            Objects.requireNonNull(mask);
            Float512Mask m = (Float512Mask)mask;
            return VectorSupport.binaryOp(VECTOR_OP_XOR, Float512Mask.class, int.class, VLENGTH,
                                          this, m,
                                          (m1, m2) -> m1.bOp(m2, (i, a, b) -> a ^ b));
        }

        // Mask Query operations

        @Override
        @ForceInline
        public int trueCount() {
            return VectorSupport.maskReductionCoerced(VECTOR_OP_MASK_TRUECOUNT, Float512Mask.class, int.class, VLENGTH, this,
                                                      (m) -> trueCountHelper(((Float512Mask)m).getBits()));
        }

        @Override
        @ForceInline
        public int firstTrue() {
            return VectorSupport.maskReductionCoerced(VECTOR_OP_MASK_FIRSTTRUE, Float512Mask.class, int.class, VLENGTH, this,
                                                      (m) -> firstTrueHelper(((Float512Mask)m).getBits()));
        }

        @Override
        @ForceInline
        public int lastTrue() {
            return VectorSupport.maskReductionCoerced(VECTOR_OP_MASK_LASTTRUE, Float512Mask.class, int.class, VLENGTH, this,
                                                      (m) -> lastTrueHelper(((Float512Mask)m).getBits()));
        }

        // Reductions

        @Override
        @ForceInline
        public boolean anyTrue() {
            return VectorSupport.test(BT_ne, Float512Mask.class, int.class, VLENGTH,
                                         this, vspecies().maskAll(true),
                                         (m, __) -> anyTrueHelper(((Float512Mask)m).getBits()));
        }

        @Override
        @ForceInline
        public boolean allTrue() {
            return VectorSupport.test(BT_overflow, Float512Mask.class, int.class, VLENGTH,
                                         this, vspecies().maskAll(true),
                                         (m, __) -> allTrueHelper(((Float512Mask)m).getBits()));
        }

        @ForceInline
        /*package-private*/
        static Float512Mask maskAll(boolean bit) {
            return VectorSupport.broadcastCoerced(Float512Mask.class, int.class, VLENGTH,
                                                  (bit ? -1 : 0), null,
                                                  (v, __) -> (v != 0 ? TRUE_MASK : FALSE_MASK));
        }
        private static final Float512Mask  TRUE_MASK = new Float512Mask(true);
        private static final Float512Mask FALSE_MASK = new Float512Mask(false);

    }

    // Shuffle

    static final class Float512Shuffle extends AbstractShuffle<Float> {
        static final int VLENGTH = VSPECIES.laneCount();    // used by the JVM
        static final Class<Float> ETYPE = float.class; // used by the JVM

        Float512Shuffle(byte[] reorder) {
            super(VLENGTH, reorder);
        }

        public Float512Shuffle(int[] reorder) {
            super(VLENGTH, reorder);
        }

        public Float512Shuffle(int[] reorder, int i) {
            super(VLENGTH, reorder, i);
        }

        public Float512Shuffle(IntUnaryOperator fn) {
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
        static final Float512Shuffle IOTA = new Float512Shuffle(IDENTITY);

        @Override
        @ForceInline
        public Float512Vector toVector() {
            return VectorSupport.shuffleToVector(VCLASS, ETYPE, Float512Shuffle.class, this, VLENGTH,
                                                    (s) -> ((Float512Vector)(((AbstractShuffle<Float>)(s)).toVectorTemplate())));
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
        public Float512Shuffle rearrange(VectorShuffle<Float> shuffle) {
            Float512Shuffle s = (Float512Shuffle) shuffle;
            byte[] reorder1 = reorder();
            byte[] reorder2 = s.reorder();
            byte[] r = new byte[reorder1.length];
            for (int i = 0; i < reorder1.length; i++) {
                int ssi = reorder2[i];
                r[i] = reorder1[ssi];  // throws on exceptional index
            }
            return new Float512Shuffle(r);
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
