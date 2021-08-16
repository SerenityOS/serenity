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
final class Byte512Vector extends ByteVector {
    static final ByteSpecies VSPECIES =
        (ByteSpecies) ByteVector.SPECIES_512;

    static final VectorShape VSHAPE =
        VSPECIES.vectorShape();

    static final Class<Byte512Vector> VCLASS = Byte512Vector.class;

    static final int VSIZE = VSPECIES.vectorBitSize();

    static final int VLENGTH = VSPECIES.laneCount(); // used by the JVM

    static final Class<Byte> ETYPE = byte.class; // used by the JVM

    Byte512Vector(byte[] v) {
        super(v);
    }

    // For compatibility as Byte512Vector::new,
    // stored into species.vectorFactory.
    Byte512Vector(Object v) {
        this((byte[]) v);
    }

    static final Byte512Vector ZERO = new Byte512Vector(new byte[VLENGTH]);
    static final Byte512Vector IOTA = new Byte512Vector(VSPECIES.iotaArray());

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
    public final Byte512Vector broadcast(byte e) {
        return (Byte512Vector) super.broadcastTemplate(e);  // specialize
    }

    @Override
    @ForceInline
    public final Byte512Vector broadcast(long e) {
        return (Byte512Vector) super.broadcastTemplate(e);  // specialize
    }

    @Override
    @ForceInline
    Byte512Mask maskFromArray(boolean[] bits) {
        return new Byte512Mask(bits);
    }

    @Override
    @ForceInline
    Byte512Shuffle iotaShuffle() { return Byte512Shuffle.IOTA; }

    @ForceInline
    Byte512Shuffle iotaShuffle(int start, int step, boolean wrap) {
      if (wrap) {
        return (Byte512Shuffle)VectorSupport.shuffleIota(ETYPE, Byte512Shuffle.class, VSPECIES, VLENGTH, start, step, 1,
                (l, lstart, lstep, s) -> s.shuffleFromOp(i -> (VectorIntrinsics.wrapToRange(i*lstep + lstart, l))));
      } else {
        return (Byte512Shuffle)VectorSupport.shuffleIota(ETYPE, Byte512Shuffle.class, VSPECIES, VLENGTH, start, step, 0,
                (l, lstart, lstep, s) -> s.shuffleFromOp(i -> (i*lstep + lstart)));
      }
    }

    @Override
    @ForceInline
    Byte512Shuffle shuffleFromBytes(byte[] reorder) { return new Byte512Shuffle(reorder); }

    @Override
    @ForceInline
    Byte512Shuffle shuffleFromArray(int[] indexes, int i) { return new Byte512Shuffle(indexes, i); }

    @Override
    @ForceInline
    Byte512Shuffle shuffleFromOp(IntUnaryOperator fn) { return new Byte512Shuffle(fn); }

    // Make a vector of the same species but the given elements:
    @ForceInline
    final @Override
    Byte512Vector vectorFactory(byte[] vec) {
        return new Byte512Vector(vec);
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
    Byte512Vector uOp(FUnOp f) {
        return (Byte512Vector) super.uOpTemplate(f);  // specialize
    }

    @ForceInline
    final @Override
    Byte512Vector uOp(VectorMask<Byte> m, FUnOp f) {
        return (Byte512Vector)
            super.uOpTemplate((Byte512Mask)m, f);  // specialize
    }

    // Binary operator

    @ForceInline
    final @Override
    Byte512Vector bOp(Vector<Byte> v, FBinOp f) {
        return (Byte512Vector) super.bOpTemplate((Byte512Vector)v, f);  // specialize
    }

    @ForceInline
    final @Override
    Byte512Vector bOp(Vector<Byte> v,
                     VectorMask<Byte> m, FBinOp f) {
        return (Byte512Vector)
            super.bOpTemplate((Byte512Vector)v, (Byte512Mask)m,
                              f);  // specialize
    }

    // Ternary operator

    @ForceInline
    final @Override
    Byte512Vector tOp(Vector<Byte> v1, Vector<Byte> v2, FTriOp f) {
        return (Byte512Vector)
            super.tOpTemplate((Byte512Vector)v1, (Byte512Vector)v2,
                              f);  // specialize
    }

    @ForceInline
    final @Override
    Byte512Vector tOp(Vector<Byte> v1, Vector<Byte> v2,
                     VectorMask<Byte> m, FTriOp f) {
        return (Byte512Vector)
            super.tOpTemplate((Byte512Vector)v1, (Byte512Vector)v2,
                              (Byte512Mask)m, f);  // specialize
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
    public Byte512Vector lanewise(Unary op) {
        return (Byte512Vector) super.lanewiseTemplate(op);  // specialize
    }

    @Override
    @ForceInline
    public Byte512Vector lanewise(Binary op, Vector<Byte> v) {
        return (Byte512Vector) super.lanewiseTemplate(op, v);  // specialize
    }

    /*package-private*/
    @Override
    @ForceInline Byte512Vector
    lanewiseShift(VectorOperators.Binary op, int e) {
        return (Byte512Vector) super.lanewiseShiftTemplate(op, e);  // specialize
    }

    /*package-private*/
    @Override
    @ForceInline
    public final
    Byte512Vector
    lanewise(VectorOperators.Ternary op, Vector<Byte> v1, Vector<Byte> v2) {
        return (Byte512Vector) super.lanewiseTemplate(op, v1, v2);  // specialize
    }

    @Override
    @ForceInline
    public final
    Byte512Vector addIndex(int scale) {
        return (Byte512Vector) super.addIndexTemplate(scale);  // specialize
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
        return super.toShuffleTemplate(Byte512Shuffle.class); // specialize
    }

    // Specialized unary testing

    @Override
    @ForceInline
    public final Byte512Mask test(Test op) {
        return super.testTemplate(Byte512Mask.class, op);  // specialize
    }

    // Specialized comparisons

    @Override
    @ForceInline
    public final Byte512Mask compare(Comparison op, Vector<Byte> v) {
        return super.compareTemplate(Byte512Mask.class, op, v);  // specialize
    }

    @Override
    @ForceInline
    public final Byte512Mask compare(Comparison op, byte s) {
        return super.compareTemplate(Byte512Mask.class, op, s);  // specialize
    }

    @Override
    @ForceInline
    public final Byte512Mask compare(Comparison op, long s) {
        return super.compareTemplate(Byte512Mask.class, op, s);  // specialize
    }

    @Override
    @ForceInline
    public Byte512Vector blend(Vector<Byte> v, VectorMask<Byte> m) {
        return (Byte512Vector)
            super.blendTemplate(Byte512Mask.class,
                                (Byte512Vector) v,
                                (Byte512Mask) m);  // specialize
    }

    @Override
    @ForceInline
    public Byte512Vector slice(int origin, Vector<Byte> v) {
        return (Byte512Vector) super.sliceTemplate(origin, v);  // specialize
    }

    @Override
    @ForceInline
    public Byte512Vector slice(int origin) {
        return (Byte512Vector) super.sliceTemplate(origin);  // specialize
    }

    @Override
    @ForceInline
    public Byte512Vector unslice(int origin, Vector<Byte> w, int part) {
        return (Byte512Vector) super.unsliceTemplate(origin, w, part);  // specialize
    }

    @Override
    @ForceInline
    public Byte512Vector unslice(int origin, Vector<Byte> w, int part, VectorMask<Byte> m) {
        return (Byte512Vector)
            super.unsliceTemplate(Byte512Mask.class,
                                  origin, w, part,
                                  (Byte512Mask) m);  // specialize
    }

    @Override
    @ForceInline
    public Byte512Vector unslice(int origin) {
        return (Byte512Vector) super.unsliceTemplate(origin);  // specialize
    }

    @Override
    @ForceInline
    public Byte512Vector rearrange(VectorShuffle<Byte> s) {
        return (Byte512Vector)
            super.rearrangeTemplate(Byte512Shuffle.class,
                                    (Byte512Shuffle) s);  // specialize
    }

    @Override
    @ForceInline
    public Byte512Vector rearrange(VectorShuffle<Byte> shuffle,
                                  VectorMask<Byte> m) {
        return (Byte512Vector)
            super.rearrangeTemplate(Byte512Shuffle.class,
                                    (Byte512Shuffle) shuffle,
                                    (Byte512Mask) m);  // specialize
    }

    @Override
    @ForceInline
    public Byte512Vector rearrange(VectorShuffle<Byte> s,
                                  Vector<Byte> v) {
        return (Byte512Vector)
            super.rearrangeTemplate(Byte512Shuffle.class,
                                    (Byte512Shuffle) s,
                                    (Byte512Vector) v);  // specialize
    }

    @Override
    @ForceInline
    public Byte512Vector selectFrom(Vector<Byte> v) {
        return (Byte512Vector)
            super.selectFromTemplate((Byte512Vector) v);  // specialize
    }

    @Override
    @ForceInline
    public Byte512Vector selectFrom(Vector<Byte> v,
                                   VectorMask<Byte> m) {
        return (Byte512Vector)
            super.selectFromTemplate((Byte512Vector) v,
                                     (Byte512Mask) m);  // specialize
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
            case 8: return laneHelper(8);
            case 9: return laneHelper(9);
            case 10: return laneHelper(10);
            case 11: return laneHelper(11);
            case 12: return laneHelper(12);
            case 13: return laneHelper(13);
            case 14: return laneHelper(14);
            case 15: return laneHelper(15);
            case 16: return laneHelper(16);
            case 17: return laneHelper(17);
            case 18: return laneHelper(18);
            case 19: return laneHelper(19);
            case 20: return laneHelper(20);
            case 21: return laneHelper(21);
            case 22: return laneHelper(22);
            case 23: return laneHelper(23);
            case 24: return laneHelper(24);
            case 25: return laneHelper(25);
            case 26: return laneHelper(26);
            case 27: return laneHelper(27);
            case 28: return laneHelper(28);
            case 29: return laneHelper(29);
            case 30: return laneHelper(30);
            case 31: return laneHelper(31);
            case 32: return laneHelper(32);
            case 33: return laneHelper(33);
            case 34: return laneHelper(34);
            case 35: return laneHelper(35);
            case 36: return laneHelper(36);
            case 37: return laneHelper(37);
            case 38: return laneHelper(38);
            case 39: return laneHelper(39);
            case 40: return laneHelper(40);
            case 41: return laneHelper(41);
            case 42: return laneHelper(42);
            case 43: return laneHelper(43);
            case 44: return laneHelper(44);
            case 45: return laneHelper(45);
            case 46: return laneHelper(46);
            case 47: return laneHelper(47);
            case 48: return laneHelper(48);
            case 49: return laneHelper(49);
            case 50: return laneHelper(50);
            case 51: return laneHelper(51);
            case 52: return laneHelper(52);
            case 53: return laneHelper(53);
            case 54: return laneHelper(54);
            case 55: return laneHelper(55);
            case 56: return laneHelper(56);
            case 57: return laneHelper(57);
            case 58: return laneHelper(58);
            case 59: return laneHelper(59);
            case 60: return laneHelper(60);
            case 61: return laneHelper(61);
            case 62: return laneHelper(62);
            case 63: return laneHelper(63);
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
    public Byte512Vector withLane(int i, byte e) {
        switch (i) {
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
            case 16: return withLaneHelper(16, e);
            case 17: return withLaneHelper(17, e);
            case 18: return withLaneHelper(18, e);
            case 19: return withLaneHelper(19, e);
            case 20: return withLaneHelper(20, e);
            case 21: return withLaneHelper(21, e);
            case 22: return withLaneHelper(22, e);
            case 23: return withLaneHelper(23, e);
            case 24: return withLaneHelper(24, e);
            case 25: return withLaneHelper(25, e);
            case 26: return withLaneHelper(26, e);
            case 27: return withLaneHelper(27, e);
            case 28: return withLaneHelper(28, e);
            case 29: return withLaneHelper(29, e);
            case 30: return withLaneHelper(30, e);
            case 31: return withLaneHelper(31, e);
            case 32: return withLaneHelper(32, e);
            case 33: return withLaneHelper(33, e);
            case 34: return withLaneHelper(34, e);
            case 35: return withLaneHelper(35, e);
            case 36: return withLaneHelper(36, e);
            case 37: return withLaneHelper(37, e);
            case 38: return withLaneHelper(38, e);
            case 39: return withLaneHelper(39, e);
            case 40: return withLaneHelper(40, e);
            case 41: return withLaneHelper(41, e);
            case 42: return withLaneHelper(42, e);
            case 43: return withLaneHelper(43, e);
            case 44: return withLaneHelper(44, e);
            case 45: return withLaneHelper(45, e);
            case 46: return withLaneHelper(46, e);
            case 47: return withLaneHelper(47, e);
            case 48: return withLaneHelper(48, e);
            case 49: return withLaneHelper(49, e);
            case 50: return withLaneHelper(50, e);
            case 51: return withLaneHelper(51, e);
            case 52: return withLaneHelper(52, e);
            case 53: return withLaneHelper(53, e);
            case 54: return withLaneHelper(54, e);
            case 55: return withLaneHelper(55, e);
            case 56: return withLaneHelper(56, e);
            case 57: return withLaneHelper(57, e);
            case 58: return withLaneHelper(58, e);
            case 59: return withLaneHelper(59, e);
            case 60: return withLaneHelper(60, e);
            case 61: return withLaneHelper(61, e);
            case 62: return withLaneHelper(62, e);
            case 63: return withLaneHelper(63, e);
            default: throw new IllegalArgumentException("Index " + i + " must be zero or positive, and less than " + VLENGTH);
        }
    }

    public Byte512Vector withLaneHelper(int i, byte e) {
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

    static final class Byte512Mask extends AbstractMask<Byte> {
        static final int VLENGTH = VSPECIES.laneCount();    // used by the JVM
        static final Class<Byte> ETYPE = byte.class; // used by the JVM

        Byte512Mask(boolean[] bits) {
            this(bits, 0);
        }

        Byte512Mask(boolean[] bits, int offset) {
            super(prepare(bits, offset));
        }

        Byte512Mask(boolean val) {
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
        Byte512Mask uOp(MUnOp f) {
            boolean[] res = new boolean[vspecies().laneCount()];
            boolean[] bits = getBits();
            for (int i = 0; i < res.length; i++) {
                res[i] = f.apply(i, bits[i]);
            }
            return new Byte512Mask(res);
        }

        @Override
        Byte512Mask bOp(VectorMask<Byte> m, MBinOp f) {
            boolean[] res = new boolean[vspecies().laneCount()];
            boolean[] bits = getBits();
            boolean[] mbits = ((Byte512Mask)m).getBits();
            for (int i = 0; i < res.length; i++) {
                res[i] = f.apply(i, bits[i], mbits[i]);
            }
            return new Byte512Mask(res);
        }

        @ForceInline
        @Override
        public final
        Byte512Vector toVector() {
            return (Byte512Vector) super.toVectorTemplate();  // specialize
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
                    Byte512Mask::defaultMaskCast);
            }
            return this.defaultMaskCast(species);
        }

        @Override
        @ForceInline
        public Byte512Mask eq(VectorMask<Byte> mask) {
            Objects.requireNonNull(mask);
            Byte512Mask m = (Byte512Mask)mask;
            return xor(m.not());
        }

        // Unary operations

        @Override
        @ForceInline
        public Byte512Mask not() {
            return xor(maskAll(true));
        }

        // Binary operations

        @Override
        @ForceInline
        public Byte512Mask and(VectorMask<Byte> mask) {
            Objects.requireNonNull(mask);
            Byte512Mask m = (Byte512Mask)mask;
            return VectorSupport.binaryOp(VECTOR_OP_AND, Byte512Mask.class, byte.class, VLENGTH,
                                             this, m,
                                             (m1, m2) -> m1.bOp(m2, (i, a, b) -> a & b));
        }

        @Override
        @ForceInline
        public Byte512Mask or(VectorMask<Byte> mask) {
            Objects.requireNonNull(mask);
            Byte512Mask m = (Byte512Mask)mask;
            return VectorSupport.binaryOp(VECTOR_OP_OR, Byte512Mask.class, byte.class, VLENGTH,
                                             this, m,
                                             (m1, m2) -> m1.bOp(m2, (i, a, b) -> a | b));
        }

        @ForceInline
        /* package-private */
        Byte512Mask xor(VectorMask<Byte> mask) {
            Objects.requireNonNull(mask);
            Byte512Mask m = (Byte512Mask)mask;
            return VectorSupport.binaryOp(VECTOR_OP_XOR, Byte512Mask.class, byte.class, VLENGTH,
                                          this, m,
                                          (m1, m2) -> m1.bOp(m2, (i, a, b) -> a ^ b));
        }

        // Mask Query operations

        @Override
        @ForceInline
        public int trueCount() {
            return VectorSupport.maskReductionCoerced(VECTOR_OP_MASK_TRUECOUNT, Byte512Mask.class, byte.class, VLENGTH, this,
                                                      (m) -> trueCountHelper(((Byte512Mask)m).getBits()));
        }

        @Override
        @ForceInline
        public int firstTrue() {
            return VectorSupport.maskReductionCoerced(VECTOR_OP_MASK_FIRSTTRUE, Byte512Mask.class, byte.class, VLENGTH, this,
                                                      (m) -> firstTrueHelper(((Byte512Mask)m).getBits()));
        }

        @Override
        @ForceInline
        public int lastTrue() {
            return VectorSupport.maskReductionCoerced(VECTOR_OP_MASK_LASTTRUE, Byte512Mask.class, byte.class, VLENGTH, this,
                                                      (m) -> lastTrueHelper(((Byte512Mask)m).getBits()));
        }

        // Reductions

        @Override
        @ForceInline
        public boolean anyTrue() {
            return VectorSupport.test(BT_ne, Byte512Mask.class, byte.class, VLENGTH,
                                         this, vspecies().maskAll(true),
                                         (m, __) -> anyTrueHelper(((Byte512Mask)m).getBits()));
        }

        @Override
        @ForceInline
        public boolean allTrue() {
            return VectorSupport.test(BT_overflow, Byte512Mask.class, byte.class, VLENGTH,
                                         this, vspecies().maskAll(true),
                                         (m, __) -> allTrueHelper(((Byte512Mask)m).getBits()));
        }

        @ForceInline
        /*package-private*/
        static Byte512Mask maskAll(boolean bit) {
            return VectorSupport.broadcastCoerced(Byte512Mask.class, byte.class, VLENGTH,
                                                  (bit ? -1 : 0), null,
                                                  (v, __) -> (v != 0 ? TRUE_MASK : FALSE_MASK));
        }
        private static final Byte512Mask  TRUE_MASK = new Byte512Mask(true);
        private static final Byte512Mask FALSE_MASK = new Byte512Mask(false);

    }

    // Shuffle

    static final class Byte512Shuffle extends AbstractShuffle<Byte> {
        static final int VLENGTH = VSPECIES.laneCount();    // used by the JVM
        static final Class<Byte> ETYPE = byte.class; // used by the JVM

        Byte512Shuffle(byte[] reorder) {
            super(VLENGTH, reorder);
        }

        public Byte512Shuffle(int[] reorder) {
            super(VLENGTH, reorder);
        }

        public Byte512Shuffle(int[] reorder, int i) {
            super(VLENGTH, reorder, i);
        }

        public Byte512Shuffle(IntUnaryOperator fn) {
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
        static final Byte512Shuffle IOTA = new Byte512Shuffle(IDENTITY);

        @Override
        @ForceInline
        public Byte512Vector toVector() {
            return VectorSupport.shuffleToVector(VCLASS, ETYPE, Byte512Shuffle.class, this, VLENGTH,
                                                    (s) -> ((Byte512Vector)(((AbstractShuffle<Byte>)(s)).toVectorTemplate())));
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
        public Byte512Shuffle rearrange(VectorShuffle<Byte> shuffle) {
            Byte512Shuffle s = (Byte512Shuffle) shuffle;
            byte[] reorder1 = reorder();
            byte[] reorder2 = s.reorder();
            byte[] r = new byte[reorder1.length];
            for (int i = 0; i < reorder1.length; i++) {
                int ssi = reorder2[i];
                r[i] = reorder1[ssi];  // throws on exceptional index
            }
            return new Byte512Shuffle(r);
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
