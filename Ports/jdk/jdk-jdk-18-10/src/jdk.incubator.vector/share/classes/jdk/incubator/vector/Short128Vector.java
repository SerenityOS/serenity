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
final class Short128Vector extends ShortVector {
    static final ShortSpecies VSPECIES =
        (ShortSpecies) ShortVector.SPECIES_128;

    static final VectorShape VSHAPE =
        VSPECIES.vectorShape();

    static final Class<Short128Vector> VCLASS = Short128Vector.class;

    static final int VSIZE = VSPECIES.vectorBitSize();

    static final int VLENGTH = VSPECIES.laneCount(); // used by the JVM

    static final Class<Short> ETYPE = short.class; // used by the JVM

    Short128Vector(short[] v) {
        super(v);
    }

    // For compatibility as Short128Vector::new,
    // stored into species.vectorFactory.
    Short128Vector(Object v) {
        this((short[]) v);
    }

    static final Short128Vector ZERO = new Short128Vector(new short[VLENGTH]);
    static final Short128Vector IOTA = new Short128Vector(VSPECIES.iotaArray());

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
    public ShortSpecies vspecies() {
        // ISSUE:  This should probably be a @Stable
        // field inside AbstractVector, rather than
        // a megamorphic method.
        return VSPECIES;
    }

    @ForceInline
    @Override
    public final Class<Short> elementType() { return short.class; }

    @ForceInline
    @Override
    public final int elementSize() { return Short.SIZE; }

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
    short[] vec() {
        return (short[])getPayload();
    }

    // Virtualized constructors

    @Override
    @ForceInline
    public final Short128Vector broadcast(short e) {
        return (Short128Vector) super.broadcastTemplate(e);  // specialize
    }

    @Override
    @ForceInline
    public final Short128Vector broadcast(long e) {
        return (Short128Vector) super.broadcastTemplate(e);  // specialize
    }

    @Override
    @ForceInline
    Short128Mask maskFromArray(boolean[] bits) {
        return new Short128Mask(bits);
    }

    @Override
    @ForceInline
    Short128Shuffle iotaShuffle() { return Short128Shuffle.IOTA; }

    @ForceInline
    Short128Shuffle iotaShuffle(int start, int step, boolean wrap) {
      if (wrap) {
        return (Short128Shuffle)VectorSupport.shuffleIota(ETYPE, Short128Shuffle.class, VSPECIES, VLENGTH, start, step, 1,
                (l, lstart, lstep, s) -> s.shuffleFromOp(i -> (VectorIntrinsics.wrapToRange(i*lstep + lstart, l))));
      } else {
        return (Short128Shuffle)VectorSupport.shuffleIota(ETYPE, Short128Shuffle.class, VSPECIES, VLENGTH, start, step, 0,
                (l, lstart, lstep, s) -> s.shuffleFromOp(i -> (i*lstep + lstart)));
      }
    }

    @Override
    @ForceInline
    Short128Shuffle shuffleFromBytes(byte[] reorder) { return new Short128Shuffle(reorder); }

    @Override
    @ForceInline
    Short128Shuffle shuffleFromArray(int[] indexes, int i) { return new Short128Shuffle(indexes, i); }

    @Override
    @ForceInline
    Short128Shuffle shuffleFromOp(IntUnaryOperator fn) { return new Short128Shuffle(fn); }

    // Make a vector of the same species but the given elements:
    @ForceInline
    final @Override
    Short128Vector vectorFactory(short[] vec) {
        return new Short128Vector(vec);
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
    Short128Vector uOp(FUnOp f) {
        return (Short128Vector) super.uOpTemplate(f);  // specialize
    }

    @ForceInline
    final @Override
    Short128Vector uOp(VectorMask<Short> m, FUnOp f) {
        return (Short128Vector)
            super.uOpTemplate((Short128Mask)m, f);  // specialize
    }

    // Binary operator

    @ForceInline
    final @Override
    Short128Vector bOp(Vector<Short> v, FBinOp f) {
        return (Short128Vector) super.bOpTemplate((Short128Vector)v, f);  // specialize
    }

    @ForceInline
    final @Override
    Short128Vector bOp(Vector<Short> v,
                     VectorMask<Short> m, FBinOp f) {
        return (Short128Vector)
            super.bOpTemplate((Short128Vector)v, (Short128Mask)m,
                              f);  // specialize
    }

    // Ternary operator

    @ForceInline
    final @Override
    Short128Vector tOp(Vector<Short> v1, Vector<Short> v2, FTriOp f) {
        return (Short128Vector)
            super.tOpTemplate((Short128Vector)v1, (Short128Vector)v2,
                              f);  // specialize
    }

    @ForceInline
    final @Override
    Short128Vector tOp(Vector<Short> v1, Vector<Short> v2,
                     VectorMask<Short> m, FTriOp f) {
        return (Short128Vector)
            super.tOpTemplate((Short128Vector)v1, (Short128Vector)v2,
                              (Short128Mask)m, f);  // specialize
    }

    @ForceInline
    final @Override
    short rOp(short v, FBinOp f) {
        return super.rOpTemplate(v, f);  // specialize
    }

    @Override
    @ForceInline
    public final <F>
    Vector<F> convertShape(VectorOperators.Conversion<Short,F> conv,
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
    public Short128Vector lanewise(Unary op) {
        return (Short128Vector) super.lanewiseTemplate(op);  // specialize
    }

    @Override
    @ForceInline
    public Short128Vector lanewise(Binary op, Vector<Short> v) {
        return (Short128Vector) super.lanewiseTemplate(op, v);  // specialize
    }

    /*package-private*/
    @Override
    @ForceInline Short128Vector
    lanewiseShift(VectorOperators.Binary op, int e) {
        return (Short128Vector) super.lanewiseShiftTemplate(op, e);  // specialize
    }

    /*package-private*/
    @Override
    @ForceInline
    public final
    Short128Vector
    lanewise(VectorOperators.Ternary op, Vector<Short> v1, Vector<Short> v2) {
        return (Short128Vector) super.lanewiseTemplate(op, v1, v2);  // specialize
    }

    @Override
    @ForceInline
    public final
    Short128Vector addIndex(int scale) {
        return (Short128Vector) super.addIndexTemplate(scale);  // specialize
    }

    // Type specific horizontal reductions

    @Override
    @ForceInline
    public final short reduceLanes(VectorOperators.Associative op) {
        return super.reduceLanesTemplate(op);  // specialized
    }

    @Override
    @ForceInline
    public final short reduceLanes(VectorOperators.Associative op,
                                    VectorMask<Short> m) {
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
                                        VectorMask<Short> m) {
        return (long) super.reduceLanesTemplate(op, m);  // specialized
    }

    @ForceInline
    public VectorShuffle<Short> toShuffle() {
        return super.toShuffleTemplate(Short128Shuffle.class); // specialize
    }

    // Specialized unary testing

    @Override
    @ForceInline
    public final Short128Mask test(Test op) {
        return super.testTemplate(Short128Mask.class, op);  // specialize
    }

    // Specialized comparisons

    @Override
    @ForceInline
    public final Short128Mask compare(Comparison op, Vector<Short> v) {
        return super.compareTemplate(Short128Mask.class, op, v);  // specialize
    }

    @Override
    @ForceInline
    public final Short128Mask compare(Comparison op, short s) {
        return super.compareTemplate(Short128Mask.class, op, s);  // specialize
    }

    @Override
    @ForceInline
    public final Short128Mask compare(Comparison op, long s) {
        return super.compareTemplate(Short128Mask.class, op, s);  // specialize
    }

    @Override
    @ForceInline
    public Short128Vector blend(Vector<Short> v, VectorMask<Short> m) {
        return (Short128Vector)
            super.blendTemplate(Short128Mask.class,
                                (Short128Vector) v,
                                (Short128Mask) m);  // specialize
    }

    @Override
    @ForceInline
    public Short128Vector slice(int origin, Vector<Short> v) {
        return (Short128Vector) super.sliceTemplate(origin, v);  // specialize
    }

    @Override
    @ForceInline
    public Short128Vector slice(int origin) {
        return (Short128Vector) super.sliceTemplate(origin);  // specialize
    }

    @Override
    @ForceInline
    public Short128Vector unslice(int origin, Vector<Short> w, int part) {
        return (Short128Vector) super.unsliceTemplate(origin, w, part);  // specialize
    }

    @Override
    @ForceInline
    public Short128Vector unslice(int origin, Vector<Short> w, int part, VectorMask<Short> m) {
        return (Short128Vector)
            super.unsliceTemplate(Short128Mask.class,
                                  origin, w, part,
                                  (Short128Mask) m);  // specialize
    }

    @Override
    @ForceInline
    public Short128Vector unslice(int origin) {
        return (Short128Vector) super.unsliceTemplate(origin);  // specialize
    }

    @Override
    @ForceInline
    public Short128Vector rearrange(VectorShuffle<Short> s) {
        return (Short128Vector)
            super.rearrangeTemplate(Short128Shuffle.class,
                                    (Short128Shuffle) s);  // specialize
    }

    @Override
    @ForceInline
    public Short128Vector rearrange(VectorShuffle<Short> shuffle,
                                  VectorMask<Short> m) {
        return (Short128Vector)
            super.rearrangeTemplate(Short128Shuffle.class,
                                    (Short128Shuffle) shuffle,
                                    (Short128Mask) m);  // specialize
    }

    @Override
    @ForceInline
    public Short128Vector rearrange(VectorShuffle<Short> s,
                                  Vector<Short> v) {
        return (Short128Vector)
            super.rearrangeTemplate(Short128Shuffle.class,
                                    (Short128Shuffle) s,
                                    (Short128Vector) v);  // specialize
    }

    @Override
    @ForceInline
    public Short128Vector selectFrom(Vector<Short> v) {
        return (Short128Vector)
            super.selectFromTemplate((Short128Vector) v);  // specialize
    }

    @Override
    @ForceInline
    public Short128Vector selectFrom(Vector<Short> v,
                                   VectorMask<Short> m) {
        return (Short128Vector)
            super.selectFromTemplate((Short128Vector) v,
                                     (Short128Mask) m);  // specialize
    }


    @ForceInline
    @Override
    public short lane(int i) {
        switch(i) {
            case 0: return laneHelper(0);
            case 1: return laneHelper(1);
            case 2: return laneHelper(2);
            case 3: return laneHelper(3);
            case 4: return laneHelper(4);
            case 5: return laneHelper(5);
            case 6: return laneHelper(6);
            case 7: return laneHelper(7);
            default: throw new IllegalArgumentException("Index " + i + " must be zero or positive, and less than " + VLENGTH);
        }
    }

    public short laneHelper(int i) {
        return (short) VectorSupport.extract(
                                VCLASS, ETYPE, VLENGTH,
                                this, i,
                                (vec, ix) -> {
                                    short[] vecarr = vec.vec();
                                    return (long)vecarr[ix];
                                });
    }

    @ForceInline
    @Override
    public Short128Vector withLane(int i, short e) {
        switch (i) {
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

    public Short128Vector withLaneHelper(int i, short e) {
        return VectorSupport.insert(
                                VCLASS, ETYPE, VLENGTH,
                                this, i, (long)e,
                                (v, ix, bits) -> {
                                    short[] res = v.vec().clone();
                                    res[ix] = (short)bits;
                                    return v.vectorFactory(res);
                                });
    }

    // Mask

    static final class Short128Mask extends AbstractMask<Short> {
        static final int VLENGTH = VSPECIES.laneCount();    // used by the JVM
        static final Class<Short> ETYPE = short.class; // used by the JVM

        Short128Mask(boolean[] bits) {
            this(bits, 0);
        }

        Short128Mask(boolean[] bits, int offset) {
            super(prepare(bits, offset));
        }

        Short128Mask(boolean val) {
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
        public ShortSpecies vspecies() {
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
        Short128Mask uOp(MUnOp f) {
            boolean[] res = new boolean[vspecies().laneCount()];
            boolean[] bits = getBits();
            for (int i = 0; i < res.length; i++) {
                res[i] = f.apply(i, bits[i]);
            }
            return new Short128Mask(res);
        }

        @Override
        Short128Mask bOp(VectorMask<Short> m, MBinOp f) {
            boolean[] res = new boolean[vspecies().laneCount()];
            boolean[] bits = getBits();
            boolean[] mbits = ((Short128Mask)m).getBits();
            for (int i = 0; i < res.length; i++) {
                res[i] = f.apply(i, bits[i], mbits[i]);
            }
            return new Short128Mask(res);
        }

        @ForceInline
        @Override
        public final
        Short128Vector toVector() {
            return (Short128Vector) super.toVectorTemplate();  // specialize
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
                    Short128Mask::defaultMaskCast);
            }
            return this.defaultMaskCast(species);
        }

        @Override
        @ForceInline
        public Short128Mask eq(VectorMask<Short> mask) {
            Objects.requireNonNull(mask);
            Short128Mask m = (Short128Mask)mask;
            return xor(m.not());
        }

        // Unary operations

        @Override
        @ForceInline
        public Short128Mask not() {
            return xor(maskAll(true));
        }

        // Binary operations

        @Override
        @ForceInline
        public Short128Mask and(VectorMask<Short> mask) {
            Objects.requireNonNull(mask);
            Short128Mask m = (Short128Mask)mask;
            return VectorSupport.binaryOp(VECTOR_OP_AND, Short128Mask.class, short.class, VLENGTH,
                                             this, m,
                                             (m1, m2) -> m1.bOp(m2, (i, a, b) -> a & b));
        }

        @Override
        @ForceInline
        public Short128Mask or(VectorMask<Short> mask) {
            Objects.requireNonNull(mask);
            Short128Mask m = (Short128Mask)mask;
            return VectorSupport.binaryOp(VECTOR_OP_OR, Short128Mask.class, short.class, VLENGTH,
                                             this, m,
                                             (m1, m2) -> m1.bOp(m2, (i, a, b) -> a | b));
        }

        @ForceInline
        /* package-private */
        Short128Mask xor(VectorMask<Short> mask) {
            Objects.requireNonNull(mask);
            Short128Mask m = (Short128Mask)mask;
            return VectorSupport.binaryOp(VECTOR_OP_XOR, Short128Mask.class, short.class, VLENGTH,
                                          this, m,
                                          (m1, m2) -> m1.bOp(m2, (i, a, b) -> a ^ b));
        }

        // Mask Query operations

        @Override
        @ForceInline
        public int trueCount() {
            return VectorSupport.maskReductionCoerced(VECTOR_OP_MASK_TRUECOUNT, Short128Mask.class, short.class, VLENGTH, this,
                                                      (m) -> trueCountHelper(((Short128Mask)m).getBits()));
        }

        @Override
        @ForceInline
        public int firstTrue() {
            return VectorSupport.maskReductionCoerced(VECTOR_OP_MASK_FIRSTTRUE, Short128Mask.class, short.class, VLENGTH, this,
                                                      (m) -> firstTrueHelper(((Short128Mask)m).getBits()));
        }

        @Override
        @ForceInline
        public int lastTrue() {
            return VectorSupport.maskReductionCoerced(VECTOR_OP_MASK_LASTTRUE, Short128Mask.class, short.class, VLENGTH, this,
                                                      (m) -> lastTrueHelper(((Short128Mask)m).getBits()));
        }

        // Reductions

        @Override
        @ForceInline
        public boolean anyTrue() {
            return VectorSupport.test(BT_ne, Short128Mask.class, short.class, VLENGTH,
                                         this, vspecies().maskAll(true),
                                         (m, __) -> anyTrueHelper(((Short128Mask)m).getBits()));
        }

        @Override
        @ForceInline
        public boolean allTrue() {
            return VectorSupport.test(BT_overflow, Short128Mask.class, short.class, VLENGTH,
                                         this, vspecies().maskAll(true),
                                         (m, __) -> allTrueHelper(((Short128Mask)m).getBits()));
        }

        @ForceInline
        /*package-private*/
        static Short128Mask maskAll(boolean bit) {
            return VectorSupport.broadcastCoerced(Short128Mask.class, short.class, VLENGTH,
                                                  (bit ? -1 : 0), null,
                                                  (v, __) -> (v != 0 ? TRUE_MASK : FALSE_MASK));
        }
        private static final Short128Mask  TRUE_MASK = new Short128Mask(true);
        private static final Short128Mask FALSE_MASK = new Short128Mask(false);

    }

    // Shuffle

    static final class Short128Shuffle extends AbstractShuffle<Short> {
        static final int VLENGTH = VSPECIES.laneCount();    // used by the JVM
        static final Class<Short> ETYPE = short.class; // used by the JVM

        Short128Shuffle(byte[] reorder) {
            super(VLENGTH, reorder);
        }

        public Short128Shuffle(int[] reorder) {
            super(VLENGTH, reorder);
        }

        public Short128Shuffle(int[] reorder, int i) {
            super(VLENGTH, reorder, i);
        }

        public Short128Shuffle(IntUnaryOperator fn) {
            super(VLENGTH, fn);
        }

        @Override
        public ShortSpecies vspecies() {
            return VSPECIES;
        }

        static {
            // There must be enough bits in the shuffle lanes to encode
            // VLENGTH valid indexes and VLENGTH exceptional ones.
            assert(VLENGTH < Byte.MAX_VALUE);
            assert(Byte.MIN_VALUE <= -VLENGTH);
        }
        static final Short128Shuffle IOTA = new Short128Shuffle(IDENTITY);

        @Override
        @ForceInline
        public Short128Vector toVector() {
            return VectorSupport.shuffleToVector(VCLASS, ETYPE, Short128Shuffle.class, this, VLENGTH,
                                                    (s) -> ((Short128Vector)(((AbstractShuffle<Short>)(s)).toVectorTemplate())));
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
        public Short128Shuffle rearrange(VectorShuffle<Short> shuffle) {
            Short128Shuffle s = (Short128Shuffle) shuffle;
            byte[] reorder1 = reorder();
            byte[] reorder2 = s.reorder();
            byte[] r = new byte[reorder1.length];
            for (int i = 0; i < reorder1.length; i++) {
                int ssi = reorder2[i];
                r[i] = reorder1[ssi];  // throws on exceptional index
            }
            return new Short128Shuffle(r);
        }
    }

    // ================================================

    // Specialized low-level memory operations.

    @ForceInline
    @Override
    final
    ShortVector fromArray0(short[] a, int offset) {
        return super.fromArray0Template(a, offset);  // specialize
    }

    @ForceInline
    @Override
    final
    ShortVector fromCharArray0(char[] a, int offset) {
        return super.fromCharArray0Template(a, offset);  // specialize
    }


    @ForceInline
    @Override
    final
    ShortVector fromByteArray0(byte[] a, int offset) {
        return super.fromByteArray0Template(a, offset);  // specialize
    }

    @ForceInline
    @Override
    final
    ShortVector fromByteBuffer0(ByteBuffer bb, int offset) {
        return super.fromByteBuffer0Template(bb, offset);  // specialize
    }

    @ForceInline
    @Override
    final
    void intoArray0(short[] a, int offset) {
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
