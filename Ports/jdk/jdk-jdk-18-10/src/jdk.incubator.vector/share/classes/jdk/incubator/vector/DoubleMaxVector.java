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
final class DoubleMaxVector extends DoubleVector {
    static final DoubleSpecies VSPECIES =
        (DoubleSpecies) DoubleVector.SPECIES_MAX;

    static final VectorShape VSHAPE =
        VSPECIES.vectorShape();

    static final Class<DoubleMaxVector> VCLASS = DoubleMaxVector.class;

    static final int VSIZE = VSPECIES.vectorBitSize();

    static final int VLENGTH = VSPECIES.laneCount(); // used by the JVM

    static final Class<Double> ETYPE = double.class; // used by the JVM

    DoubleMaxVector(double[] v) {
        super(v);
    }

    // For compatibility as DoubleMaxVector::new,
    // stored into species.vectorFactory.
    DoubleMaxVector(Object v) {
        this((double[]) v);
    }

    static final DoubleMaxVector ZERO = new DoubleMaxVector(new double[VLENGTH]);
    static final DoubleMaxVector IOTA = new DoubleMaxVector(VSPECIES.iotaArray());

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
    public final DoubleMaxVector broadcast(double e) {
        return (DoubleMaxVector) super.broadcastTemplate(e);  // specialize
    }

    @Override
    @ForceInline
    public final DoubleMaxVector broadcast(long e) {
        return (DoubleMaxVector) super.broadcastTemplate(e);  // specialize
    }

    @Override
    @ForceInline
    DoubleMaxMask maskFromArray(boolean[] bits) {
        return new DoubleMaxMask(bits);
    }

    @Override
    @ForceInline
    DoubleMaxShuffle iotaShuffle() { return DoubleMaxShuffle.IOTA; }

    @ForceInline
    DoubleMaxShuffle iotaShuffle(int start, int step, boolean wrap) {
      if (wrap) {
        return (DoubleMaxShuffle)VectorSupport.shuffleIota(ETYPE, DoubleMaxShuffle.class, VSPECIES, VLENGTH, start, step, 1,
                (l, lstart, lstep, s) -> s.shuffleFromOp(i -> (VectorIntrinsics.wrapToRange(i*lstep + lstart, l))));
      } else {
        return (DoubleMaxShuffle)VectorSupport.shuffleIota(ETYPE, DoubleMaxShuffle.class, VSPECIES, VLENGTH, start, step, 0,
                (l, lstart, lstep, s) -> s.shuffleFromOp(i -> (i*lstep + lstart)));
      }
    }

    @Override
    @ForceInline
    DoubleMaxShuffle shuffleFromBytes(byte[] reorder) { return new DoubleMaxShuffle(reorder); }

    @Override
    @ForceInline
    DoubleMaxShuffle shuffleFromArray(int[] indexes, int i) { return new DoubleMaxShuffle(indexes, i); }

    @Override
    @ForceInline
    DoubleMaxShuffle shuffleFromOp(IntUnaryOperator fn) { return new DoubleMaxShuffle(fn); }

    // Make a vector of the same species but the given elements:
    @ForceInline
    final @Override
    DoubleMaxVector vectorFactory(double[] vec) {
        return new DoubleMaxVector(vec);
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
    DoubleMaxVector uOp(FUnOp f) {
        return (DoubleMaxVector) super.uOpTemplate(f);  // specialize
    }

    @ForceInline
    final @Override
    DoubleMaxVector uOp(VectorMask<Double> m, FUnOp f) {
        return (DoubleMaxVector)
            super.uOpTemplate((DoubleMaxMask)m, f);  // specialize
    }

    // Binary operator

    @ForceInline
    final @Override
    DoubleMaxVector bOp(Vector<Double> v, FBinOp f) {
        return (DoubleMaxVector) super.bOpTemplate((DoubleMaxVector)v, f);  // specialize
    }

    @ForceInline
    final @Override
    DoubleMaxVector bOp(Vector<Double> v,
                     VectorMask<Double> m, FBinOp f) {
        return (DoubleMaxVector)
            super.bOpTemplate((DoubleMaxVector)v, (DoubleMaxMask)m,
                              f);  // specialize
    }

    // Ternary operator

    @ForceInline
    final @Override
    DoubleMaxVector tOp(Vector<Double> v1, Vector<Double> v2, FTriOp f) {
        return (DoubleMaxVector)
            super.tOpTemplate((DoubleMaxVector)v1, (DoubleMaxVector)v2,
                              f);  // specialize
    }

    @ForceInline
    final @Override
    DoubleMaxVector tOp(Vector<Double> v1, Vector<Double> v2,
                     VectorMask<Double> m, FTriOp f) {
        return (DoubleMaxVector)
            super.tOpTemplate((DoubleMaxVector)v1, (DoubleMaxVector)v2,
                              (DoubleMaxMask)m, f);  // specialize
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
    public DoubleMaxVector lanewise(Unary op) {
        return (DoubleMaxVector) super.lanewiseTemplate(op);  // specialize
    }

    @Override
    @ForceInline
    public DoubleMaxVector lanewise(Binary op, Vector<Double> v) {
        return (DoubleMaxVector) super.lanewiseTemplate(op, v);  // specialize
    }


    /*package-private*/
    @Override
    @ForceInline
    public final
    DoubleMaxVector
    lanewise(VectorOperators.Ternary op, Vector<Double> v1, Vector<Double> v2) {
        return (DoubleMaxVector) super.lanewiseTemplate(op, v1, v2);  // specialize
    }

    @Override
    @ForceInline
    public final
    DoubleMaxVector addIndex(int scale) {
        return (DoubleMaxVector) super.addIndexTemplate(scale);  // specialize
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
        return super.toShuffleTemplate(DoubleMaxShuffle.class); // specialize
    }

    // Specialized unary testing

    @Override
    @ForceInline
    public final DoubleMaxMask test(Test op) {
        return super.testTemplate(DoubleMaxMask.class, op);  // specialize
    }

    // Specialized comparisons

    @Override
    @ForceInline
    public final DoubleMaxMask compare(Comparison op, Vector<Double> v) {
        return super.compareTemplate(DoubleMaxMask.class, op, v);  // specialize
    }

    @Override
    @ForceInline
    public final DoubleMaxMask compare(Comparison op, double s) {
        return super.compareTemplate(DoubleMaxMask.class, op, s);  // specialize
    }

    @Override
    @ForceInline
    public final DoubleMaxMask compare(Comparison op, long s) {
        return super.compareTemplate(DoubleMaxMask.class, op, s);  // specialize
    }

    @Override
    @ForceInline
    public DoubleMaxVector blend(Vector<Double> v, VectorMask<Double> m) {
        return (DoubleMaxVector)
            super.blendTemplate(DoubleMaxMask.class,
                                (DoubleMaxVector) v,
                                (DoubleMaxMask) m);  // specialize
    }

    @Override
    @ForceInline
    public DoubleMaxVector slice(int origin, Vector<Double> v) {
        return (DoubleMaxVector) super.sliceTemplate(origin, v);  // specialize
    }

    @Override
    @ForceInline
    public DoubleMaxVector slice(int origin) {
        return (DoubleMaxVector) super.sliceTemplate(origin);  // specialize
    }

    @Override
    @ForceInline
    public DoubleMaxVector unslice(int origin, Vector<Double> w, int part) {
        return (DoubleMaxVector) super.unsliceTemplate(origin, w, part);  // specialize
    }

    @Override
    @ForceInline
    public DoubleMaxVector unslice(int origin, Vector<Double> w, int part, VectorMask<Double> m) {
        return (DoubleMaxVector)
            super.unsliceTemplate(DoubleMaxMask.class,
                                  origin, w, part,
                                  (DoubleMaxMask) m);  // specialize
    }

    @Override
    @ForceInline
    public DoubleMaxVector unslice(int origin) {
        return (DoubleMaxVector) super.unsliceTemplate(origin);  // specialize
    }

    @Override
    @ForceInline
    public DoubleMaxVector rearrange(VectorShuffle<Double> s) {
        return (DoubleMaxVector)
            super.rearrangeTemplate(DoubleMaxShuffle.class,
                                    (DoubleMaxShuffle) s);  // specialize
    }

    @Override
    @ForceInline
    public DoubleMaxVector rearrange(VectorShuffle<Double> shuffle,
                                  VectorMask<Double> m) {
        return (DoubleMaxVector)
            super.rearrangeTemplate(DoubleMaxShuffle.class,
                                    (DoubleMaxShuffle) shuffle,
                                    (DoubleMaxMask) m);  // specialize
    }

    @Override
    @ForceInline
    public DoubleMaxVector rearrange(VectorShuffle<Double> s,
                                  Vector<Double> v) {
        return (DoubleMaxVector)
            super.rearrangeTemplate(DoubleMaxShuffle.class,
                                    (DoubleMaxShuffle) s,
                                    (DoubleMaxVector) v);  // specialize
    }

    @Override
    @ForceInline
    public DoubleMaxVector selectFrom(Vector<Double> v) {
        return (DoubleMaxVector)
            super.selectFromTemplate((DoubleMaxVector) v);  // specialize
    }

    @Override
    @ForceInline
    public DoubleMaxVector selectFrom(Vector<Double> v,
                                   VectorMask<Double> m) {
        return (DoubleMaxVector)
            super.selectFromTemplate((DoubleMaxVector) v,
                                     (DoubleMaxMask) m);  // specialize
    }


    @ForceInline
    @Override
    public double lane(int i) {
        if (i < 0 || i >= VLENGTH) {
            throw new IllegalArgumentException("Index " + i + " must be zero or positive, and less than " + VLENGTH);
        }
        long bits = laneHelper(i);
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
    public DoubleMaxVector withLane(int i, double e) {
        if (i < 0 || i >= VLENGTH) {
            throw new IllegalArgumentException("Index " + i + " must be zero or positive, and less than " + VLENGTH);
        }
        return withLaneHelper(i, e);
    }

    public DoubleMaxVector withLaneHelper(int i, double e) {
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

    static final class DoubleMaxMask extends AbstractMask<Double> {
        static final int VLENGTH = VSPECIES.laneCount();    // used by the JVM
        static final Class<Double> ETYPE = double.class; // used by the JVM

        DoubleMaxMask(boolean[] bits) {
            this(bits, 0);
        }

        DoubleMaxMask(boolean[] bits, int offset) {
            super(prepare(bits, offset));
        }

        DoubleMaxMask(boolean val) {
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
        DoubleMaxMask uOp(MUnOp f) {
            boolean[] res = new boolean[vspecies().laneCount()];
            boolean[] bits = getBits();
            for (int i = 0; i < res.length; i++) {
                res[i] = f.apply(i, bits[i]);
            }
            return new DoubleMaxMask(res);
        }

        @Override
        DoubleMaxMask bOp(VectorMask<Double> m, MBinOp f) {
            boolean[] res = new boolean[vspecies().laneCount()];
            boolean[] bits = getBits();
            boolean[] mbits = ((DoubleMaxMask)m).getBits();
            for (int i = 0; i < res.length; i++) {
                res[i] = f.apply(i, bits[i], mbits[i]);
            }
            return new DoubleMaxMask(res);
        }

        @ForceInline
        @Override
        public final
        DoubleMaxVector toVector() {
            return (DoubleMaxVector) super.toVectorTemplate();  // specialize
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
                    DoubleMaxMask::defaultMaskCast);
            }
            return this.defaultMaskCast(species);
        }

        @Override
        @ForceInline
        public DoubleMaxMask eq(VectorMask<Double> mask) {
            Objects.requireNonNull(mask);
            DoubleMaxMask m = (DoubleMaxMask)mask;
            return xor(m.not());
        }

        // Unary operations

        @Override
        @ForceInline
        public DoubleMaxMask not() {
            return xor(maskAll(true));
        }

        // Binary operations

        @Override
        @ForceInline
        public DoubleMaxMask and(VectorMask<Double> mask) {
            Objects.requireNonNull(mask);
            DoubleMaxMask m = (DoubleMaxMask)mask;
            return VectorSupport.binaryOp(VECTOR_OP_AND, DoubleMaxMask.class, long.class, VLENGTH,
                                             this, m,
                                             (m1, m2) -> m1.bOp(m2, (i, a, b) -> a & b));
        }

        @Override
        @ForceInline
        public DoubleMaxMask or(VectorMask<Double> mask) {
            Objects.requireNonNull(mask);
            DoubleMaxMask m = (DoubleMaxMask)mask;
            return VectorSupport.binaryOp(VECTOR_OP_OR, DoubleMaxMask.class, long.class, VLENGTH,
                                             this, m,
                                             (m1, m2) -> m1.bOp(m2, (i, a, b) -> a | b));
        }

        @ForceInline
        /* package-private */
        DoubleMaxMask xor(VectorMask<Double> mask) {
            Objects.requireNonNull(mask);
            DoubleMaxMask m = (DoubleMaxMask)mask;
            return VectorSupport.binaryOp(VECTOR_OP_XOR, DoubleMaxMask.class, long.class, VLENGTH,
                                          this, m,
                                          (m1, m2) -> m1.bOp(m2, (i, a, b) -> a ^ b));
        }

        // Mask Query operations

        @Override
        @ForceInline
        public int trueCount() {
            return VectorSupport.maskReductionCoerced(VECTOR_OP_MASK_TRUECOUNT, DoubleMaxMask.class, long.class, VLENGTH, this,
                                                      (m) -> trueCountHelper(((DoubleMaxMask)m).getBits()));
        }

        @Override
        @ForceInline
        public int firstTrue() {
            return VectorSupport.maskReductionCoerced(VECTOR_OP_MASK_FIRSTTRUE, DoubleMaxMask.class, long.class, VLENGTH, this,
                                                      (m) -> firstTrueHelper(((DoubleMaxMask)m).getBits()));
        }

        @Override
        @ForceInline
        public int lastTrue() {
            return VectorSupport.maskReductionCoerced(VECTOR_OP_MASK_LASTTRUE, DoubleMaxMask.class, long.class, VLENGTH, this,
                                                      (m) -> lastTrueHelper(((DoubleMaxMask)m).getBits()));
        }

        // Reductions

        @Override
        @ForceInline
        public boolean anyTrue() {
            return VectorSupport.test(BT_ne, DoubleMaxMask.class, long.class, VLENGTH,
                                         this, vspecies().maskAll(true),
                                         (m, __) -> anyTrueHelper(((DoubleMaxMask)m).getBits()));
        }

        @Override
        @ForceInline
        public boolean allTrue() {
            return VectorSupport.test(BT_overflow, DoubleMaxMask.class, long.class, VLENGTH,
                                         this, vspecies().maskAll(true),
                                         (m, __) -> allTrueHelper(((DoubleMaxMask)m).getBits()));
        }

        @ForceInline
        /*package-private*/
        static DoubleMaxMask maskAll(boolean bit) {
            return VectorSupport.broadcastCoerced(DoubleMaxMask.class, long.class, VLENGTH,
                                                  (bit ? -1 : 0), null,
                                                  (v, __) -> (v != 0 ? TRUE_MASK : FALSE_MASK));
        }
        private static final DoubleMaxMask  TRUE_MASK = new DoubleMaxMask(true);
        private static final DoubleMaxMask FALSE_MASK = new DoubleMaxMask(false);

    }

    // Shuffle

    static final class DoubleMaxShuffle extends AbstractShuffle<Double> {
        static final int VLENGTH = VSPECIES.laneCount();    // used by the JVM
        static final Class<Double> ETYPE = double.class; // used by the JVM

        DoubleMaxShuffle(byte[] reorder) {
            super(VLENGTH, reorder);
        }

        public DoubleMaxShuffle(int[] reorder) {
            super(VLENGTH, reorder);
        }

        public DoubleMaxShuffle(int[] reorder, int i) {
            super(VLENGTH, reorder, i);
        }

        public DoubleMaxShuffle(IntUnaryOperator fn) {
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
        static final DoubleMaxShuffle IOTA = new DoubleMaxShuffle(IDENTITY);

        @Override
        @ForceInline
        public DoubleMaxVector toVector() {
            return VectorSupport.shuffleToVector(VCLASS, ETYPE, DoubleMaxShuffle.class, this, VLENGTH,
                                                    (s) -> ((DoubleMaxVector)(((AbstractShuffle<Double>)(s)).toVectorTemplate())));
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
        public DoubleMaxShuffle rearrange(VectorShuffle<Double> shuffle) {
            DoubleMaxShuffle s = (DoubleMaxShuffle) shuffle;
            byte[] reorder1 = reorder();
            byte[] reorder2 = s.reorder();
            byte[] r = new byte[reorder1.length];
            for (int i = 0; i < reorder1.length; i++) {
                int ssi = reorder2[i];
                r[i] = reorder1[ssi];  // throws on exceptional index
            }
            return new DoubleMaxShuffle(r);
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
