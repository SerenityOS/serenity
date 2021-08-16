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
final class Double512Vector extends DoubleVector {
    static final DoubleSpecies VSPECIES =
        (DoubleSpecies) DoubleVector.SPECIES_512;

    static final VectorShape VSHAPE =
        VSPECIES.vectorShape();

    static final Class<Double512Vector> VCLASS = Double512Vector.class;

    static final int VSIZE = VSPECIES.vectorBitSize();

    static final int VLENGTH = VSPECIES.laneCount(); // used by the JVM

    static final Class<Double> ETYPE = double.class; // used by the JVM

    Double512Vector(double[] v) {
        super(v);
    }

    // For compatibility as Double512Vector::new,
    // stored into species.vectorFactory.
    Double512Vector(Object v) {
        this((double[]) v);
    }

    static final Double512Vector ZERO = new Double512Vector(new double[VLENGTH]);
    static final Double512Vector IOTA = new Double512Vector(VSPECIES.iotaArray());

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
    public DoubleSpecies vspecies() {
        // ISSUE:  This should probably be a @Stable
        // field inside AbstractVector, rather than
        // a megamorphic method.
        return VSPECIES;
    }

    @ForceInline
    @Override
    public final Class<Double> elementType() { return double.class; }

    @ForceInline
    @Override
    public final int elementSize() { return Double.SIZE; }

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
    double[] vec() {
        return (double[])getPayload();
    }

    // Virtualized constructors

    @Override
    @ForceInline
    public final Double512Vector broadcast(double e) {
        return (Double512Vector) super.broadcastTemplate(e);  // specialize
    }

    @Override
    @ForceInline
    public final Double512Vector broadcast(long e) {
        return (Double512Vector) super.broadcastTemplate(e);  // specialize
    }

    @Override
    @ForceInline
    Double512Mask maskFromArray(boolean[] bits) {
        return new Double512Mask(bits);
    }

    @Override
    @ForceInline
    Double512Shuffle iotaShuffle() { return Double512Shuffle.IOTA; }

    @ForceInline
    Double512Shuffle iotaShuffle(int start, int step, boolean wrap) {
      if (wrap) {
        return (Double512Shuffle)VectorSupport.shuffleIota(ETYPE, Double512Shuffle.class, VSPECIES, VLENGTH, start, step, 1,
                (l, lstart, lstep, s) -> s.shuffleFromOp(i -> (VectorIntrinsics.wrapToRange(i*lstep + lstart, l))));
      } else {
        return (Double512Shuffle)VectorSupport.shuffleIota(ETYPE, Double512Shuffle.class, VSPECIES, VLENGTH, start, step, 0,
                (l, lstart, lstep, s) -> s.shuffleFromOp(i -> (i*lstep + lstart)));
      }
    }

    @Override
    @ForceInline
    Double512Shuffle shuffleFromBytes(byte[] reorder) { return new Double512Shuffle(reorder); }

    @Override
    @ForceInline
    Double512Shuffle shuffleFromArray(int[] indexes, int i) { return new Double512Shuffle(indexes, i); }

    @Override
    @ForceInline
    Double512Shuffle shuffleFromOp(IntUnaryOperator fn) { return new Double512Shuffle(fn); }

    // Make a vector of the same species but the given elements:
    @ForceInline
    final @Override
    Double512Vector vectorFactory(double[] vec) {
        return new Double512Vector(vec);
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
    Double512Vector uOp(FUnOp f) {
        return (Double512Vector) super.uOpTemplate(f);  // specialize
    }

    @ForceInline
    final @Override
    Double512Vector uOp(VectorMask<Double> m, FUnOp f) {
        return (Double512Vector)
            super.uOpTemplate((Double512Mask)m, f);  // specialize
    }

    // Binary operator

    @ForceInline
    final @Override
    Double512Vector bOp(Vector<Double> v, FBinOp f) {
        return (Double512Vector) super.bOpTemplate((Double512Vector)v, f);  // specialize
    }

    @ForceInline
    final @Override
    Double512Vector bOp(Vector<Double> v,
                     VectorMask<Double> m, FBinOp f) {
        return (Double512Vector)
            super.bOpTemplate((Double512Vector)v, (Double512Mask)m,
                              f);  // specialize
    }

    // Ternary operator

    @ForceInline
    final @Override
    Double512Vector tOp(Vector<Double> v1, Vector<Double> v2, FTriOp f) {
        return (Double512Vector)
            super.tOpTemplate((Double512Vector)v1, (Double512Vector)v2,
                              f);  // specialize
    }

    @ForceInline
    final @Override
    Double512Vector tOp(Vector<Double> v1, Vector<Double> v2,
                     VectorMask<Double> m, FTriOp f) {
        return (Double512Vector)
            super.tOpTemplate((Double512Vector)v1, (Double512Vector)v2,
                              (Double512Mask)m, f);  // specialize
    }

    @ForceInline
    final @Override
    double rOp(double v, FBinOp f) {
        return super.rOpTemplate(v, f);  // specialize
    }

    @Override
    @ForceInline
    public final <F>
    Vector<F> convertShape(VectorOperators.Conversion<Double,F> conv,
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
    public Double512Vector lanewise(Unary op) {
        return (Double512Vector) super.lanewiseTemplate(op);  // specialize
    }

    @Override
    @ForceInline
    public Double512Vector lanewise(Binary op, Vector<Double> v) {
        return (Double512Vector) super.lanewiseTemplate(op, v);  // specialize
    }


    /*package-private*/
    @Override
    @ForceInline
    public final
    Double512Vector
    lanewise(VectorOperators.Ternary op, Vector<Double> v1, Vector<Double> v2) {
        return (Double512Vector) super.lanewiseTemplate(op, v1, v2);  // specialize
    }

    @Override
    @ForceInline
    public final
    Double512Vector addIndex(int scale) {
        return (Double512Vector) super.addIndexTemplate(scale);  // specialize
    }

    // Type specific horizontal reductions

    @Override
    @ForceInline
    public final double reduceLanes(VectorOperators.Associative op) {
        return super.reduceLanesTemplate(op);  // specialized
    }

    @Override
    @ForceInline
    public final double reduceLanes(VectorOperators.Associative op,
                                    VectorMask<Double> m) {
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
                                        VectorMask<Double> m) {
        return (long) super.reduceLanesTemplate(op, m);  // specialized
    }

    @ForceInline
    public VectorShuffle<Double> toShuffle() {
        return super.toShuffleTemplate(Double512Shuffle.class); // specialize
    }

    // Specialized unary testing

    @Override
    @ForceInline
    public final Double512Mask test(Test op) {
        return super.testTemplate(Double512Mask.class, op);  // specialize
    }

    // Specialized comparisons

    @Override
    @ForceInline
    public final Double512Mask compare(Comparison op, Vector<Double> v) {
        return super.compareTemplate(Double512Mask.class, op, v);  // specialize
    }

    @Override
    @ForceInline
    public final Double512Mask compare(Comparison op, double s) {
        return super.compareTemplate(Double512Mask.class, op, s);  // specialize
    }

    @Override
    @ForceInline
    public final Double512Mask compare(Comparison op, long s) {
        return super.compareTemplate(Double512Mask.class, op, s);  // specialize
    }

    @Override
    @ForceInline
    public Double512Vector blend(Vector<Double> v, VectorMask<Double> m) {
        return (Double512Vector)
            super.blendTemplate(Double512Mask.class,
                                (Double512Vector) v,
                                (Double512Mask) m);  // specialize
    }

    @Override
    @ForceInline
    public Double512Vector slice(int origin, Vector<Double> v) {
        return (Double512Vector) super.sliceTemplate(origin, v);  // specialize
    }

    @Override
    @ForceInline
    public Double512Vector slice(int origin) {
        return (Double512Vector) super.sliceTemplate(origin);  // specialize
    }

    @Override
    @ForceInline
    public Double512Vector unslice(int origin, Vector<Double> w, int part) {
        return (Double512Vector) super.unsliceTemplate(origin, w, part);  // specialize
    }

    @Override
    @ForceInline
    public Double512Vector unslice(int origin, Vector<Double> w, int part, VectorMask<Double> m) {
        return (Double512Vector)
            super.unsliceTemplate(Double512Mask.class,
                                  origin, w, part,
                                  (Double512Mask) m);  // specialize
    }

    @Override
    @ForceInline
    public Double512Vector unslice(int origin) {
        return (Double512Vector) super.unsliceTemplate(origin);  // specialize
    }

    @Override
    @ForceInline
    public Double512Vector rearrange(VectorShuffle<Double> s) {
        return (Double512Vector)
            super.rearrangeTemplate(Double512Shuffle.class,
                                    (Double512Shuffle) s);  // specialize
    }

    @Override
    @ForceInline
    public Double512Vector rearrange(VectorShuffle<Double> shuffle,
                                  VectorMask<Double> m) {
        return (Double512Vector)
            super.rearrangeTemplate(Double512Shuffle.class,
                                    (Double512Shuffle) shuffle,
                                    (Double512Mask) m);  // specialize
    }

    @Override
    @ForceInline
    public Double512Vector rearrange(VectorShuffle<Double> s,
                                  Vector<Double> v) {
        return (Double512Vector)
            super.rearrangeTemplate(Double512Shuffle.class,
                                    (Double512Shuffle) s,
                                    (Double512Vector) v);  // specialize
    }

    @Override
    @ForceInline
    public Double512Vector selectFrom(Vector<Double> v) {
        return (Double512Vector)
            super.selectFromTemplate((Double512Vector) v);  // specialize
    }

    @Override
    @ForceInline
    public Double512Vector selectFrom(Vector<Double> v,
                                   VectorMask<Double> m) {
        return (Double512Vector)
            super.selectFromTemplate((Double512Vector) v,
                                     (Double512Mask) m);  // specialize
    }


    @ForceInline
    @Override
    public double lane(int i) {
        long bits;
        switch(i) {
            case 0: bits = laneHelper(0); break;
            case 1: bits = laneHelper(1); break;
            case 2: bits = laneHelper(2); break;
            case 3: bits = laneHelper(3); break;
            case 4: bits = laneHelper(4); break;
            case 5: bits = laneHelper(5); break;
            case 6: bits = laneHelper(6); break;
            case 7: bits = laneHelper(7); break;
            default: throw new IllegalArgumentException("Index " + i + " must be zero or positive, and less than " + VLENGTH);
        }
        return Double.longBitsToDouble(bits);
    }

    public long laneHelper(int i) {
        return (long) VectorSupport.extract(
                     VCLASS, ETYPE, VLENGTH,
                     this, i,
                     (vec, ix) -> {
                     double[] vecarr = vec.vec();
                     return (long)Double.doubleToLongBits(vecarr[ix]);
                     });
    }

    @ForceInline
    @Override
    public Double512Vector withLane(int i, double e) {
        switch(i) {
            case 0: return withLaneHelper(0, e);
            case 1: return withLaneHelper(1, e);
            case 2: return withLaneHelper(2, e);
            case 3: return withLaneHelper(3, e);
            case 4: return withLaneHelper(4, e);
            case 5: return withLaneHelper(5, e);
            case 6: return withLaneHelper(6, e);
            case 7: return withLaneHelper(7, e);
            default: throw new IllegalArgumentException("Index " + i + " must be zero or positive, and less than " + VLENGTH);
        }
    }

    public Double512Vector withLaneHelper(int i, double e) {
        return VectorSupport.insert(
                                VCLASS, ETYPE, VLENGTH,
                                this, i, (long)Double.doubleToLongBits(e),
                                (v, ix, bits) -> {
                                    double[] res = v.vec().clone();
                                    res[ix] = Double.longBitsToDouble((long)bits);
                                    return v.vectorFactory(res);
                                });
    }

    // Mask

    static final class Double512Mask extends AbstractMask<Double> {
        static final int VLENGTH = VSPECIES.laneCount();    // used by the JVM
        static final Class<Double> ETYPE = double.class; // used by the JVM

        Double512Mask(boolean[] bits) {
            this(bits, 0);
        }

        Double512Mask(boolean[] bits, int offset) {
            super(prepare(bits, offset));
        }

        Double512Mask(boolean val) {
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
        public DoubleSpecies vspecies() {
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
        Double512Mask uOp(MUnOp f) {
            boolean[] res = new boolean[vspecies().laneCount()];
            boolean[] bits = getBits();
            for (int i = 0; i < res.length; i++) {
                res[i] = f.apply(i, bits[i]);
            }
            return new Double512Mask(res);
        }

        @Override
        Double512Mask bOp(VectorMask<Double> m, MBinOp f) {
            boolean[] res = new boolean[vspecies().laneCount()];
            boolean[] bits = getBits();
            boolean[] mbits = ((Double512Mask)m).getBits();
            for (int i = 0; i < res.length; i++) {
                res[i] = f.apply(i, bits[i], mbits[i]);
            }
            return new Double512Mask(res);
        }

        @ForceInline
        @Override
        public final
        Double512Vector toVector() {
            return (Double512Vector) super.toVectorTemplate();  // specialize
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
                    Double512Mask::defaultMaskCast);
            }
            return this.defaultMaskCast(species);
        }

        @Override
        @ForceInline
        public Double512Mask eq(VectorMask<Double> mask) {
            Objects.requireNonNull(mask);
            Double512Mask m = (Double512Mask)mask;
            return xor(m.not());
        }

        // Unary operations

        @Override
        @ForceInline
        public Double512Mask not() {
            return xor(maskAll(true));
        }

        // Binary operations

        @Override
        @ForceInline
        public Double512Mask and(VectorMask<Double> mask) {
            Objects.requireNonNull(mask);
            Double512Mask m = (Double512Mask)mask;
            return VectorSupport.binaryOp(VECTOR_OP_AND, Double512Mask.class, long.class, VLENGTH,
                                             this, m,
                                             (m1, m2) -> m1.bOp(m2, (i, a, b) -> a & b));
        }

        @Override
        @ForceInline
        public Double512Mask or(VectorMask<Double> mask) {
            Objects.requireNonNull(mask);
            Double512Mask m = (Double512Mask)mask;
            return VectorSupport.binaryOp(VECTOR_OP_OR, Double512Mask.class, long.class, VLENGTH,
                                             this, m,
                                             (m1, m2) -> m1.bOp(m2, (i, a, b) -> a | b));
        }

        @ForceInline
        /* package-private */
        Double512Mask xor(VectorMask<Double> mask) {
            Objects.requireNonNull(mask);
            Double512Mask m = (Double512Mask)mask;
            return VectorSupport.binaryOp(VECTOR_OP_XOR, Double512Mask.class, long.class, VLENGTH,
                                          this, m,
                                          (m1, m2) -> m1.bOp(m2, (i, a, b) -> a ^ b));
        }

        // Mask Query operations

        @Override
        @ForceInline
        public int trueCount() {
            return VectorSupport.maskReductionCoerced(VECTOR_OP_MASK_TRUECOUNT, Double512Mask.class, long.class, VLENGTH, this,
                                                      (m) -> trueCountHelper(((Double512Mask)m).getBits()));
        }

        @Override
        @ForceInline
        public int firstTrue() {
            return VectorSupport.maskReductionCoerced(VECTOR_OP_MASK_FIRSTTRUE, Double512Mask.class, long.class, VLENGTH, this,
                                                      (m) -> firstTrueHelper(((Double512Mask)m).getBits()));
        }

        @Override
        @ForceInline
        public int lastTrue() {
            return VectorSupport.maskReductionCoerced(VECTOR_OP_MASK_LASTTRUE, Double512Mask.class, long.class, VLENGTH, this,
                                                      (m) -> lastTrueHelper(((Double512Mask)m).getBits()));
        }

        // Reductions

        @Override
        @ForceInline
        public boolean anyTrue() {
            return VectorSupport.test(BT_ne, Double512Mask.class, long.class, VLENGTH,
                                         this, vspecies().maskAll(true),
                                         (m, __) -> anyTrueHelper(((Double512Mask)m).getBits()));
        }

        @Override
        @ForceInline
        public boolean allTrue() {
            return VectorSupport.test(BT_overflow, Double512Mask.class, long.class, VLENGTH,
                                         this, vspecies().maskAll(true),
                                         (m, __) -> allTrueHelper(((Double512Mask)m).getBits()));
        }

        @ForceInline
        /*package-private*/
        static Double512Mask maskAll(boolean bit) {
            return VectorSupport.broadcastCoerced(Double512Mask.class, long.class, VLENGTH,
                                                  (bit ? -1 : 0), null,
                                                  (v, __) -> (v != 0 ? TRUE_MASK : FALSE_MASK));
        }
        private static final Double512Mask  TRUE_MASK = new Double512Mask(true);
        private static final Double512Mask FALSE_MASK = new Double512Mask(false);

    }

    // Shuffle

    static final class Double512Shuffle extends AbstractShuffle<Double> {
        static final int VLENGTH = VSPECIES.laneCount();    // used by the JVM
        static final Class<Double> ETYPE = double.class; // used by the JVM

        Double512Shuffle(byte[] reorder) {
            super(VLENGTH, reorder);
        }

        public Double512Shuffle(int[] reorder) {
            super(VLENGTH, reorder);
        }

        public Double512Shuffle(int[] reorder, int i) {
            super(VLENGTH, reorder, i);
        }

        public Double512Shuffle(IntUnaryOperator fn) {
            super(VLENGTH, fn);
        }

        @Override
        public DoubleSpecies vspecies() {
            return VSPECIES;
        }

        static {
            // There must be enough bits in the shuffle lanes to encode
            // VLENGTH valid indexes and VLENGTH exceptional ones.
            assert(VLENGTH < Byte.MAX_VALUE);
            assert(Byte.MIN_VALUE <= -VLENGTH);
        }
        static final Double512Shuffle IOTA = new Double512Shuffle(IDENTITY);

        @Override
        @ForceInline
        public Double512Vector toVector() {
            return VectorSupport.shuffleToVector(VCLASS, ETYPE, Double512Shuffle.class, this, VLENGTH,
                                                    (s) -> ((Double512Vector)(((AbstractShuffle<Double>)(s)).toVectorTemplate())));
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
        public Double512Shuffle rearrange(VectorShuffle<Double> shuffle) {
            Double512Shuffle s = (Double512Shuffle) shuffle;
            byte[] reorder1 = reorder();
            byte[] reorder2 = s.reorder();
            byte[] r = new byte[reorder1.length];
            for (int i = 0; i < reorder1.length; i++) {
                int ssi = reorder2[i];
                r[i] = reorder1[ssi];  // throws on exceptional index
            }
            return new Double512Shuffle(r);
        }
    }

    // ================================================

    // Specialized low-level memory operations.

    @ForceInline
    @Override
    final
    DoubleVector fromArray0(double[] a, int offset) {
        return super.fromArray0Template(a, offset);  // specialize
    }



    @ForceInline
    @Override
    final
    DoubleVector fromByteArray0(byte[] a, int offset) {
        return super.fromByteArray0Template(a, offset);  // specialize
    }

    @ForceInline
    @Override
    final
    DoubleVector fromByteBuffer0(ByteBuffer bb, int offset) {
        return super.fromByteBuffer0Template(bb, offset);  // specialize
    }

    @ForceInline
    @Override
    final
    void intoArray0(double[] a, int offset) {
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
