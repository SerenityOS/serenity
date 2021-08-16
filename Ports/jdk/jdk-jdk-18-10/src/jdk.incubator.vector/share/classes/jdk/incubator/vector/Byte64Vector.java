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
final class Byte64Vector extends ByteVector {
    static final ByteSpecies VSPECIES =
        (ByteSpecies) ByteVector.SPECIES_64;

    static final VectorShape VSHAPE =
        VSPECIES.vectorShape();

    static final Class<Byte64Vector> VCLASS = Byte64Vector.class;

    static final int VSIZE = VSPECIES.vectorBitSize();

    static final int VLENGTH = VSPECIES.laneCount(); // used by the JVM

    static final Class<Byte> ETYPE = byte.class; // used by the JVM

    Byte64Vector(byte[] v) {
        super(v);
    }

    // For compatibility as Byte64Vector::new,
    // stored into species.vectorFactory.
    Byte64Vector(Object v) {
        this((byte[]) v);
    }

    static final Byte64Vector ZERO = new Byte64Vector(new byte[VLENGTH]);
    static final Byte64Vector IOTA = new Byte64Vector(VSPECIES.iotaArray());

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
    public ByteSpecies vspecies() {
        // ISSUE:  This should probably be a @Stable
        // field inside AbstractVector, rather than
        // a megamorphic method.
        return VSPECIES;
    }

    @ForceInline
    @Override
    public final Class<Byte> elementType() { return byte.class; }

    @ForceInline
    @Override
    public final int elementSize() { return Byte.SIZE; }

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
    byte[] vec() {
        return (byte[])getPayload();
    }

    // Virtualized constructors

    @Override
    @ForceInline
    public final Byte64Vector broadcast(byte e) {
        return (Byte64Vector) super.broadcastTemplate(e);  // specialize
    }

    @Override
    @ForceInline
    public final Byte64Vector broadcast(long e) {
        return (Byte64Vector) super.broadcastTemplate(e);  // specialize
    }

    @Override
    @ForceInline
    Byte64Mask maskFromArray(boolean[] bits) {
        return new Byte64Mask(bits);
    }

    @Override
    @ForceInline
    Byte64Shuffle iotaShuffle() { return Byte64Shuffle.IOTA; }

    @ForceInline
    Byte64Shuffle iotaShuffle(int start, int step, boolean wrap) {
      if (wrap) {
        return (Byte64Shuffle)VectorSupport.shuffleIota(ETYPE, Byte64Shuffle.class, VSPECIES, VLENGTH, start, step, 1,
                (l, lstart, lstep, s) -> s.shuffleFromOp(i -> (VectorIntrinsics.wrapToRange(i*lstep + lstart, l))));
      } else {
        return (Byte64Shuffle)VectorSupport.shuffleIota(ETYPE, Byte64Shuffle.class, VSPECIES, VLENGTH, start, step, 0,
                (l, lstart, lstep, s) -> s.shuffleFromOp(i -> (i*lstep + lstart)));
      }
    }

    @Override
    @ForceInline
    Byte64Shuffle shuffleFromBytes(byte[] reorder) { return new Byte64Shuffle(reorder); }

    @Override
    @ForceInline
    Byte64Shuffle shuffleFromArray(int[] indexes, int i) { return new Byte64Shuffle(indexes, i); }

    @Override
    @ForceInline
    Byte64Shuffle shuffleFromOp(IntUnaryOperator fn) { return new Byte64Shuffle(fn); }

    // Make a vector of the same species but the given elements:
    @ForceInline
    final @Override
    Byte64Vector vectorFactory(byte[] vec) {
        return new Byte64Vector(vec);
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
    Byte64Vector uOp(FUnOp f) {
        return (Byte64Vector) super.uOpTemplate(f);  // specialize
    }

    @ForceInline
    final @Override
    Byte64Vector uOp(VectorMask<Byte> m, FUnOp f) {
        return (Byte64Vector)
            super.uOpTemplate((Byte64Mask)m, f);  // specialize
    }

    // Binary operator

    @ForceInline
    final @Override
    Byte64Vector bOp(Vector<Byte> v, FBinOp f) {
        return (Byte64Vector) super.bOpTemplate((Byte64Vector)v, f);  // specialize
    }

    @ForceInline
    final @Override
    Byte64Vector bOp(Vector<Byte> v,
                     VectorMask<Byte> m, FBinOp f) {
        return (Byte64Vector)
            super.bOpTemplate((Byte64Vector)v, (Byte64Mask)m,
                              f);  // specialize
    }

    // Ternary operator

    @ForceInline
    final @Override
    Byte64Vector tOp(Vector<Byte> v1, Vector<Byte> v2, FTriOp f) {
        return (Byte64Vector)
            super.tOpTemplate((Byte64Vector)v1, (Byte64Vector)v2,
                              f);  // specialize
    }

    @ForceInline
    final @Override
    Byte64Vector tOp(Vector<Byte> v1, Vector<Byte> v2,
                     VectorMask<Byte> m, FTriOp f) {
        return (Byte64Vector)
            super.tOpTemplate((Byte64Vector)v1, (Byte64Vector)v2,
                              (Byte64Mask)m, f);  // specialize
    }

    @ForceInline
    final @Override
    byte rOp(byte v, FBinOp f) {
        return super.rOpTemplate(v, f);  // specialize
    }

    @Override
    @ForceInline
    public final <F>
    Vector<F> convertShape(VectorOperators.Conversion<Byte,F> conv,
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
    public Byte64Vector lanewise(Unary op) {
        return (Byte64Vector) super.lanewiseTemplate(op);  // specialize
    }

    @Override
    @ForceInline
    public Byte64Vector lanewise(Binary op, Vector<Byte> v) {
        return (Byte64Vector) super.lanewiseTemplate(op, v);  // specialize
    }

    /*package-private*/
    @Override
    @ForceInline Byte64Vector
    lanewiseShift(VectorOperators.Binary op, int e) {
        return (Byte64Vector) super.lanewiseShiftTemplate(op, e);  // specialize
    }

    /*package-private*/
    @Override
    @ForceInline
    public final
    Byte64Vector
    lanewise(VectorOperators.Ternary op, Vector<Byte> v1, Vector<Byte> v2) {
        return (Byte64Vector) super.lanewiseTemplate(op, v1, v2);  // specialize
    }

    @Override
    @ForceInline
    public final
    Byte64Vector addIndex(int scale) {
        return (Byte64Vector) super.addIndexTemplate(scale);  // specialize
    }

    // Type specific horizontal reductions

    @Override
    @ForceInline
    public final byte reduceLanes(VectorOperators.Associative op) {
        return super.reduceLanesTemplate(op);  // specialized
    }

    @Override
    @ForceInline
    public final byte reduceLanes(VectorOperators.Associative op,
                                    VectorMask<Byte> m) {
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
                                        VectorMask<Byte> m) {
        return (long) super.reduceLanesTemplate(op, m);  // specialized
    }

    @ForceInline
    public VectorShuffle<Byte> toShuffle() {
        return super.toShuffleTemplate(Byte64Shuffle.class); // specialize
    }

    // Specialized unary testing

    @Override
    @ForceInline
    public final Byte64Mask test(Test op) {
        return super.testTemplate(Byte64Mask.class, op);  // specialize
    }

    // Specialized comparisons

    @Override
    @ForceInline
    public final Byte64Mask compare(Comparison op, Vector<Byte> v) {
        return super.compareTemplate(Byte64Mask.class, op, v);  // specialize
    }

    @Override
    @ForceInline
    public final Byte64Mask compare(Comparison op, byte s) {
        return super.compareTemplate(Byte64Mask.class, op, s);  // specialize
    }

    @Override
    @ForceInline
    public final Byte64Mask compare(Comparison op, long s) {
        return super.compareTemplate(Byte64Mask.class, op, s);  // specialize
    }

    @Override
    @ForceInline
    public Byte64Vector blend(Vector<Byte> v, VectorMask<Byte> m) {
        return (Byte64Vector)
            super.blendTemplate(Byte64Mask.class,
                                (Byte64Vector) v,
                                (Byte64Mask) m);  // specialize
    }

    @Override
    @ForceInline
    public Byte64Vector slice(int origin, Vector<Byte> v) {
        return (Byte64Vector) super.sliceTemplate(origin, v);  // specialize
    }

    @Override
    @ForceInline
    public Byte64Vector slice(int origin) {
        return (Byte64Vector) super.sliceTemplate(origin);  // specialize
    }

    @Override
    @ForceInline
    public Byte64Vector unslice(int origin, Vector<Byte> w, int part) {
        return (Byte64Vector) super.unsliceTemplate(origin, w, part);  // specialize
    }

    @Override
    @ForceInline
    public Byte64Vector unslice(int origin, Vector<Byte> w, int part, VectorMask<Byte> m) {
        return (Byte64Vector)
            super.unsliceTemplate(Byte64Mask.class,
                                  origin, w, part,
                                  (Byte64Mask) m);  // specialize
    }

    @Override
    @ForceInline
    public Byte64Vector unslice(int origin) {
        return (Byte64Vector) super.unsliceTemplate(origin);  // specialize
    }

    @Override
    @ForceInline
    public Byte64Vector rearrange(VectorShuffle<Byte> s) {
        return (Byte64Vector)
            super.rearrangeTemplate(Byte64Shuffle.class,
                                    (Byte64Shuffle) s);  // specialize
    }

    @Override
    @ForceInline
    public Byte64Vector rearrange(VectorShuffle<Byte> shuffle,
                                  VectorMask<Byte> m) {
        return (Byte64Vector)
            super.rearrangeTemplate(Byte64Shuffle.class,
                                    (Byte64Shuffle) shuffle,
                                    (Byte64Mask) m);  // specialize
    }

    @Override
    @ForceInline
    public Byte64Vector rearrange(VectorShuffle<Byte> s,
                                  Vector<Byte> v) {
        return (Byte64Vector)
            super.rearrangeTemplate(Byte64Shuffle.class,
                                    (Byte64Shuffle) s,
                                    (Byte64Vector) v);  // specialize
    }

    @Override
    @ForceInline
    public Byte64Vector selectFrom(Vector<Byte> v) {
        return (Byte64Vector)
            super.selectFromTemplate((Byte64Vector) v);  // specialize
    }

    @Override
    @ForceInline
    public Byte64Vector selectFrom(Vector<Byte> v,
                                   VectorMask<Byte> m) {
        return (Byte64Vector)
            super.selectFromTemplate((Byte64Vector) v,
                                     (Byte64Mask) m);  // specialize
    }


    @ForceInline
    @Override
    public byte lane(int i) {
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

    public byte laneHelper(int i) {
        return (byte) VectorSupport.extract(
                                VCLASS, ETYPE, VLENGTH,
                                this, i,
                                (vec, ix) -> {
                                    byte[] vecarr = vec.vec();
                                    return (long)vecarr[ix];
                                });
    }

    @ForceInline
    @Override
    public Byte64Vector withLane(int i, byte e) {
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

    public Byte64Vector withLaneHelper(int i, byte e) {
        return VectorSupport.insert(
                                VCLASS, ETYPE, VLENGTH,
                                this, i, (long)e,
                                (v, ix, bits) -> {
                                    byte[] res = v.vec().clone();
                                    res[ix] = (byte)bits;
                                    return v.vectorFactory(res);
                                });
    }

    // Mask

    static final class Byte64Mask extends AbstractMask<Byte> {
        static final int VLENGTH = VSPECIES.laneCount();    // used by the JVM
        static final Class<Byte> ETYPE = byte.class; // used by the JVM

        Byte64Mask(boolean[] bits) {
            this(bits, 0);
        }

        Byte64Mask(boolean[] bits, int offset) {
            super(prepare(bits, offset));
        }

        Byte64Mask(boolean val) {
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
        public ByteSpecies vspecies() {
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
        Byte64Mask uOp(MUnOp f) {
            boolean[] res = new boolean[vspecies().laneCount()];
            boolean[] bits = getBits();
            for (int i = 0; i < res.length; i++) {
                res[i] = f.apply(i, bits[i]);
            }
            return new Byte64Mask(res);
        }

        @Override
        Byte64Mask bOp(VectorMask<Byte> m, MBinOp f) {
            boolean[] res = new boolean[vspecies().laneCount()];
            boolean[] bits = getBits();
            boolean[] mbits = ((Byte64Mask)m).getBits();
            for (int i = 0; i < res.length; i++) {
                res[i] = f.apply(i, bits[i], mbits[i]);
            }
            return new Byte64Mask(res);
        }

        @ForceInline
        @Override
        public final
        Byte64Vector toVector() {
            return (Byte64Vector) super.toVectorTemplate();  // specialize
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
                    Byte64Mask::defaultMaskCast);
            }
            return this.defaultMaskCast(species);
        }

        @Override
        @ForceInline
        public Byte64Mask eq(VectorMask<Byte> mask) {
            Objects.requireNonNull(mask);
            Byte64Mask m = (Byte64Mask)mask;
            return xor(m.not());
        }

        // Unary operations

        @Override
        @ForceInline
        public Byte64Mask not() {
            return xor(maskAll(true));
        }

        // Binary operations

        @Override
        @ForceInline
        public Byte64Mask and(VectorMask<Byte> mask) {
            Objects.requireNonNull(mask);
            Byte64Mask m = (Byte64Mask)mask;
            return VectorSupport.binaryOp(VECTOR_OP_AND, Byte64Mask.class, byte.class, VLENGTH,
                                             this, m,
                                             (m1, m2) -> m1.bOp(m2, (i, a, b) -> a & b));
        }

        @Override
        @ForceInline
        public Byte64Mask or(VectorMask<Byte> mask) {
            Objects.requireNonNull(mask);
            Byte64Mask m = (Byte64Mask)mask;
            return VectorSupport.binaryOp(VECTOR_OP_OR, Byte64Mask.class, byte.class, VLENGTH,
                                             this, m,
                                             (m1, m2) -> m1.bOp(m2, (i, a, b) -> a | b));
        }

        @ForceInline
        /* package-private */
        Byte64Mask xor(VectorMask<Byte> mask) {
            Objects.requireNonNull(mask);
            Byte64Mask m = (Byte64Mask)mask;
            return VectorSupport.binaryOp(VECTOR_OP_XOR, Byte64Mask.class, byte.class, VLENGTH,
                                          this, m,
                                          (m1, m2) -> m1.bOp(m2, (i, a, b) -> a ^ b));
        }

        // Mask Query operations

        @Override
        @ForceInline
        public int trueCount() {
            return VectorSupport.maskReductionCoerced(VECTOR_OP_MASK_TRUECOUNT, Byte64Mask.class, byte.class, VLENGTH, this,
                                                      (m) -> trueCountHelper(((Byte64Mask)m).getBits()));
        }

        @Override
        @ForceInline
        public int firstTrue() {
            return VectorSupport.maskReductionCoerced(VECTOR_OP_MASK_FIRSTTRUE, Byte64Mask.class, byte.class, VLENGTH, this,
                                                      (m) -> firstTrueHelper(((Byte64Mask)m).getBits()));
        }

        @Override
        @ForceInline
        public int lastTrue() {
            return VectorSupport.maskReductionCoerced(VECTOR_OP_MASK_LASTTRUE, Byte64Mask.class, byte.class, VLENGTH, this,
                                                      (m) -> lastTrueHelper(((Byte64Mask)m).getBits()));
        }

        // Reductions

        @Override
        @ForceInline
        public boolean anyTrue() {
            return VectorSupport.test(BT_ne, Byte64Mask.class, byte.class, VLENGTH,
                                         this, vspecies().maskAll(true),
                                         (m, __) -> anyTrueHelper(((Byte64Mask)m).getBits()));
        }

        @Override
        @ForceInline
        public boolean allTrue() {
            return VectorSupport.test(BT_overflow, Byte64Mask.class, byte.class, VLENGTH,
                                         this, vspecies().maskAll(true),
                                         (m, __) -> allTrueHelper(((Byte64Mask)m).getBits()));
        }

        @ForceInline
        /*package-private*/
        static Byte64Mask maskAll(boolean bit) {
            return VectorSupport.broadcastCoerced(Byte64Mask.class, byte.class, VLENGTH,
                                                  (bit ? -1 : 0), null,
                                                  (v, __) -> (v != 0 ? TRUE_MASK : FALSE_MASK));
        }
        private static final Byte64Mask  TRUE_MASK = new Byte64Mask(true);
        private static final Byte64Mask FALSE_MASK = new Byte64Mask(false);

    }

    // Shuffle

    static final class Byte64Shuffle extends AbstractShuffle<Byte> {
        static final int VLENGTH = VSPECIES.laneCount();    // used by the JVM
        static final Class<Byte> ETYPE = byte.class; // used by the JVM

        Byte64Shuffle(byte[] reorder) {
            super(VLENGTH, reorder);
        }

        public Byte64Shuffle(int[] reorder) {
            super(VLENGTH, reorder);
        }

        public Byte64Shuffle(int[] reorder, int i) {
            super(VLENGTH, reorder, i);
        }

        public Byte64Shuffle(IntUnaryOperator fn) {
            super(VLENGTH, fn);
        }

        @Override
        public ByteSpecies vspecies() {
            return VSPECIES;
        }

        static {
            // There must be enough bits in the shuffle lanes to encode
            // VLENGTH valid indexes and VLENGTH exceptional ones.
            assert(VLENGTH < Byte.MAX_VALUE);
            assert(Byte.MIN_VALUE <= -VLENGTH);
        }
        static final Byte64Shuffle IOTA = new Byte64Shuffle(IDENTITY);

        @Override
        @ForceInline
        public Byte64Vector toVector() {
            return VectorSupport.shuffleToVector(VCLASS, ETYPE, Byte64Shuffle.class, this, VLENGTH,
                                                    (s) -> ((Byte64Vector)(((AbstractShuffle<Byte>)(s)).toVectorTemplate())));
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
        public Byte64Shuffle rearrange(VectorShuffle<Byte> shuffle) {
            Byte64Shuffle s = (Byte64Shuffle) shuffle;
            byte[] reorder1 = reorder();
            byte[] reorder2 = s.reorder();
            byte[] r = new byte[reorder1.length];
            for (int i = 0; i < reorder1.length; i++) {
                int ssi = reorder2[i];
                r[i] = reorder1[ssi];  // throws on exceptional index
            }
            return new Byte64Shuffle(r);
        }
    }

    // ================================================

    // Specialized low-level memory operations.

    @ForceInline
    @Override
    final
    ByteVector fromArray0(byte[] a, int offset) {
        return super.fromArray0Template(a, offset);  // specialize
    }


    @ForceInline
    @Override
    final
    ByteVector fromBooleanArray0(boolean[] a, int offset) {
        return super.fromBooleanArray0Template(a, offset);  // specialize
    }

    @ForceInline
    @Override
    final
    ByteVector fromByteArray0(byte[] a, int offset) {
        return super.fromByteArray0Template(a, offset);  // specialize
    }

    @ForceInline
    @Override
    final
    ByteVector fromByteBuffer0(ByteBuffer bb, int offset) {
        return super.fromByteBuffer0Template(bb, offset);  // specialize
    }

    @ForceInline
    @Override
    final
    void intoArray0(byte[] a, int offset) {
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
