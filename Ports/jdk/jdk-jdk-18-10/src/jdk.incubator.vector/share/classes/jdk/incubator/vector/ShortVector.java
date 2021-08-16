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
import java.nio.ByteOrder;
import java.nio.ReadOnlyBufferException;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import jdk.internal.misc.ScopedMemoryAccess;
import jdk.internal.misc.Unsafe;
import jdk.internal.vm.annotation.ForceInline;
import jdk.internal.vm.vector.VectorSupport;

import static jdk.internal.vm.vector.VectorSupport.*;
import static jdk.incubator.vector.VectorIntrinsics.*;

import static jdk.incubator.vector.VectorOperators.*;

// -- This file was mechanically generated: Do not edit! -- //

/**
 * A specialized {@link Vector} representing an ordered immutable sequence of
 * {@code short} values.
 */
@SuppressWarnings("cast")  // warning: redundant cast
public abstract class ShortVector extends AbstractVector<Short> {

    ShortVector(short[] vec) {
        super(vec);
    }

    static final int FORBID_OPCODE_KIND = VO_ONLYFP;

    @ForceInline
    static int opCode(Operator op) {
        return VectorOperators.opCode(op, VO_OPCODE_VALID, FORBID_OPCODE_KIND);
    }
    @ForceInline
    static int opCode(Operator op, int requireKind) {
        requireKind |= VO_OPCODE_VALID;
        return VectorOperators.opCode(op, requireKind, FORBID_OPCODE_KIND);
    }
    @ForceInline
    static boolean opKind(Operator op, int bit) {
        return VectorOperators.opKind(op, bit);
    }

    // Virtualized factories and operators,
    // coded with portable definitions.
    // These are all @ForceInline in case
    // they need to be used performantly.
    // The various shape-specific subclasses
    // also specialize them by wrapping
    // them in a call like this:
    //    return (Byte128Vector)
    //       super.bOp((Byte128Vector) o);
    // The purpose of that is to forcibly inline
    // the generic definition from this file
    // into a sharply type- and size-specific
    // wrapper in the subclass file, so that
    // the JIT can specialize the code.
    // The code is only inlined and expanded
    // if it gets hot.  Think of it as a cheap
    // and lazy version of C++ templates.

    // Virtualized getter

    /*package-private*/
    abstract short[] vec();

    // Virtualized constructors

    /**
     * Build a vector directly using my own constructor.
     * It is an error if the array is aliased elsewhere.
     */
    /*package-private*/
    abstract ShortVector vectorFactory(short[] vec);

    /**
     * Build a mask directly using my species.
     * It is an error if the array is aliased elsewhere.
     */
    /*package-private*/
    @ForceInline
    final
    AbstractMask<Short> maskFactory(boolean[] bits) {
        return vspecies().maskFactory(bits);
    }

    // Constant loader (takes dummy as vector arg)
    interface FVOp {
        short apply(int i);
    }

    /*package-private*/
    @ForceInline
    final
    ShortVector vOp(FVOp f) {
        short[] res = new short[length()];
        for (int i = 0; i < res.length; i++) {
            res[i] = f.apply(i);
        }
        return vectorFactory(res);
    }

    @ForceInline
    final
    ShortVector vOp(VectorMask<Short> m, FVOp f) {
        short[] res = new short[length()];
        boolean[] mbits = ((AbstractMask<Short>)m).getBits();
        for (int i = 0; i < res.length; i++) {
            if (mbits[i]) {
                res[i] = f.apply(i);
            }
        }
        return vectorFactory(res);
    }

    // Unary operator

    /*package-private*/
    interface FUnOp {
        short apply(int i, short a);
    }

    /*package-private*/
    abstract
    ShortVector uOp(FUnOp f);
    @ForceInline
    final
    ShortVector uOpTemplate(FUnOp f) {
        short[] vec = vec();
        short[] res = new short[length()];
        for (int i = 0; i < res.length; i++) {
            res[i] = f.apply(i, vec[i]);
        }
        return vectorFactory(res);
    }

    /*package-private*/
    abstract
    ShortVector uOp(VectorMask<Short> m,
                             FUnOp f);
    @ForceInline
    final
    ShortVector uOpTemplate(VectorMask<Short> m,
                                     FUnOp f) {
        short[] vec = vec();
        short[] res = new short[length()];
        boolean[] mbits = ((AbstractMask<Short>)m).getBits();
        for (int i = 0; i < res.length; i++) {
            res[i] = mbits[i] ? f.apply(i, vec[i]) : vec[i];
        }
        return vectorFactory(res);
    }

    // Binary operator

    /*package-private*/
    interface FBinOp {
        short apply(int i, short a, short b);
    }

    /*package-private*/
    abstract
    ShortVector bOp(Vector<Short> o,
                             FBinOp f);
    @ForceInline
    final
    ShortVector bOpTemplate(Vector<Short> o,
                                     FBinOp f) {
        short[] res = new short[length()];
        short[] vec1 = this.vec();
        short[] vec2 = ((ShortVector)o).vec();
        for (int i = 0; i < res.length; i++) {
            res[i] = f.apply(i, vec1[i], vec2[i]);
        }
        return vectorFactory(res);
    }

    /*package-private*/
    abstract
    ShortVector bOp(Vector<Short> o,
                             VectorMask<Short> m,
                             FBinOp f);
    @ForceInline
    final
    ShortVector bOpTemplate(Vector<Short> o,
                                     VectorMask<Short> m,
                                     FBinOp f) {
        short[] res = new short[length()];
        short[] vec1 = this.vec();
        short[] vec2 = ((ShortVector)o).vec();
        boolean[] mbits = ((AbstractMask<Short>)m).getBits();
        for (int i = 0; i < res.length; i++) {
            res[i] = mbits[i] ? f.apply(i, vec1[i], vec2[i]) : vec1[i];
        }
        return vectorFactory(res);
    }

    // Ternary operator

    /*package-private*/
    interface FTriOp {
        short apply(int i, short a, short b, short c);
    }

    /*package-private*/
    abstract
    ShortVector tOp(Vector<Short> o1,
                             Vector<Short> o2,
                             FTriOp f);
    @ForceInline
    final
    ShortVector tOpTemplate(Vector<Short> o1,
                                     Vector<Short> o2,
                                     FTriOp f) {
        short[] res = new short[length()];
        short[] vec1 = this.vec();
        short[] vec2 = ((ShortVector)o1).vec();
        short[] vec3 = ((ShortVector)o2).vec();
        for (int i = 0; i < res.length; i++) {
            res[i] = f.apply(i, vec1[i], vec2[i], vec3[i]);
        }
        return vectorFactory(res);
    }

    /*package-private*/
    abstract
    ShortVector tOp(Vector<Short> o1,
                             Vector<Short> o2,
                             VectorMask<Short> m,
                             FTriOp f);
    @ForceInline
    final
    ShortVector tOpTemplate(Vector<Short> o1,
                                     Vector<Short> o2,
                                     VectorMask<Short> m,
                                     FTriOp f) {
        short[] res = new short[length()];
        short[] vec1 = this.vec();
        short[] vec2 = ((ShortVector)o1).vec();
        short[] vec3 = ((ShortVector)o2).vec();
        boolean[] mbits = ((AbstractMask<Short>)m).getBits();
        for (int i = 0; i < res.length; i++) {
            res[i] = mbits[i] ? f.apply(i, vec1[i], vec2[i], vec3[i]) : vec1[i];
        }
        return vectorFactory(res);
    }

    // Reduction operator

    /*package-private*/
    abstract
    short rOp(short v, FBinOp f);
    @ForceInline
    final
    short rOpTemplate(short v, FBinOp f) {
        short[] vec = vec();
        for (int i = 0; i < vec.length; i++) {
            v = f.apply(i, v, vec[i]);
        }
        return v;
    }

    // Memory reference

    /*package-private*/
    interface FLdOp<M> {
        short apply(M memory, int offset, int i);
    }

    /*package-private*/
    @ForceInline
    final
    <M> ShortVector ldOp(M memory, int offset,
                                  FLdOp<M> f) {
        //dummy; no vec = vec();
        short[] res = new short[length()];
        for (int i = 0; i < res.length; i++) {
            res[i] = f.apply(memory, offset, i);
        }
        return vectorFactory(res);
    }

    /*package-private*/
    @ForceInline
    final
    <M> ShortVector ldOp(M memory, int offset,
                                  VectorMask<Short> m,
                                  FLdOp<M> f) {
        //short[] vec = vec();
        short[] res = new short[length()];
        boolean[] mbits = ((AbstractMask<Short>)m).getBits();
        for (int i = 0; i < res.length; i++) {
            if (mbits[i]) {
                res[i] = f.apply(memory, offset, i);
            }
        }
        return vectorFactory(res);
    }

    interface FStOp<M> {
        void apply(M memory, int offset, int i, short a);
    }

    /*package-private*/
    @ForceInline
    final
    <M> void stOp(M memory, int offset,
                  FStOp<M> f) {
        short[] vec = vec();
        for (int i = 0; i < vec.length; i++) {
            f.apply(memory, offset, i, vec[i]);
        }
    }

    /*package-private*/
    @ForceInline
    final
    <M> void stOp(M memory, int offset,
                  VectorMask<Short> m,
                  FStOp<M> f) {
        short[] vec = vec();
        boolean[] mbits = ((AbstractMask<Short>)m).getBits();
        for (int i = 0; i < vec.length; i++) {
            if (mbits[i]) {
                f.apply(memory, offset, i, vec[i]);
            }
        }
    }

    // Binary test

    /*package-private*/
    interface FBinTest {
        boolean apply(int cond, int i, short a, short b);
    }

    /*package-private*/
    @ForceInline
    final
    AbstractMask<Short> bTest(int cond,
                                  Vector<Short> o,
                                  FBinTest f) {
        short[] vec1 = vec();
        short[] vec2 = ((ShortVector)o).vec();
        boolean[] bits = new boolean[length()];
        for (int i = 0; i < length(); i++){
            bits[i] = f.apply(cond, i, vec1[i], vec2[i]);
        }
        return maskFactory(bits);
    }

    /*package-private*/
    @Override
    abstract ShortSpecies vspecies();

    /*package-private*/
    @ForceInline
    static long toBits(short e) {
        return  e;
    }

    /*package-private*/
    @ForceInline
    static short fromBits(long bits) {
        return ((short)bits);
    }

    // Static factories (other than memory operations)

    // Note: A surprising behavior in javadoc
    // sometimes makes a lone /** {@inheritDoc} */
    // comment drop the method altogether,
    // apparently if the method mentions an
    // parameter or return type of Vector<Short>
    // instead of Vector<E> as originally specified.
    // Adding an empty HTML fragment appears to
    // nudge javadoc into providing the desired
    // inherited documentation.  We use the HTML
    // comment <!--workaround--> for this.

    /**
     * Returns a vector of the given species
     * where all lane elements are set to
     * zero, the default primitive value.
     *
     * @param species species of the desired zero vector
     * @return a zero vector
     */
    @ForceInline
    public static ShortVector zero(VectorSpecies<Short> species) {
        ShortSpecies vsp = (ShortSpecies) species;
        return VectorSupport.broadcastCoerced(vsp.vectorType(), short.class, species.length(),
                                0, vsp,
                                ((bits_, s_) -> s_.rvOp(i -> bits_)));
    }

    /**
     * Returns a vector of the same species as this one
     * where all lane elements are set to
     * the primitive value {@code e}.
     *
     * The contents of the current vector are discarded;
     * only the species is relevant to this operation.
     *
     * <p> This method returns the value of this expression:
     * {@code ShortVector.broadcast(this.species(), e)}.
     *
     * @apiNote
     * Unlike the similar method named {@code broadcast()}
     * in the supertype {@code Vector}, this method does not
     * need to validate its argument, and cannot throw
     * {@code IllegalArgumentException}.  This method is
     * therefore preferable to the supertype method.
     *
     * @param e the value to broadcast
     * @return a vector where all lane elements are set to
     *         the primitive value {@code e}
     * @see #broadcast(VectorSpecies,long)
     * @see Vector#broadcast(long)
     * @see VectorSpecies#broadcast(long)
     */
    public abstract ShortVector broadcast(short e);

    /**
     * Returns a vector of the given species
     * where all lane elements are set to
     * the primitive value {@code e}.
     *
     * @param species species of the desired vector
     * @param e the value to broadcast
     * @return a vector where all lane elements are set to
     *         the primitive value {@code e}
     * @see #broadcast(long)
     * @see Vector#broadcast(long)
     * @see VectorSpecies#broadcast(long)
     */
    @ForceInline
    public static ShortVector broadcast(VectorSpecies<Short> species, short e) {
        ShortSpecies vsp = (ShortSpecies) species;
        return vsp.broadcast(e);
    }

    /*package-private*/
    @ForceInline
    final ShortVector broadcastTemplate(short e) {
        ShortSpecies vsp = vspecies();
        return vsp.broadcast(e);
    }

    /**
     * {@inheritDoc} <!--workaround-->
     * @apiNote
     * When working with vector subtypes like {@code ShortVector},
     * {@linkplain #broadcast(short) the more strongly typed method}
     * is typically selected.  It can be explicitly selected
     * using a cast: {@code v.broadcast((short)e)}.
     * The two expressions will produce numerically identical results.
     */
    @Override
    public abstract ShortVector broadcast(long e);

    /**
     * Returns a vector of the given species
     * where all lane elements are set to
     * the primitive value {@code e}.
     *
     * The {@code long} value must be accurately representable
     * by the {@code ETYPE} of the vector species, so that
     * {@code e==(long)(ETYPE)e}.
     *
     * @param species species of the desired vector
     * @param e the value to broadcast
     * @return a vector where all lane elements are set to
     *         the primitive value {@code e}
     * @throws IllegalArgumentException
     *         if the given {@code long} value cannot
     *         be represented by the vector's {@code ETYPE}
     * @see #broadcast(VectorSpecies,short)
     * @see VectorSpecies#checkValue(long)
     */
    @ForceInline
    public static ShortVector broadcast(VectorSpecies<Short> species, long e) {
        ShortSpecies vsp = (ShortSpecies) species;
        return vsp.broadcast(e);
    }

    /*package-private*/
    @ForceInline
    final ShortVector broadcastTemplate(long e) {
        return vspecies().broadcast(e);
    }

    // Unary lanewise support

    /**
     * {@inheritDoc} <!--workaround-->
     */
    public abstract
    ShortVector lanewise(VectorOperators.Unary op);

    @ForceInline
    final
    ShortVector lanewiseTemplate(VectorOperators.Unary op) {
        if (opKind(op, VO_SPECIAL)) {
            if (op == ZOMO) {
                return blend(broadcast(-1), compare(NE, 0));
            }
            if (op == NOT) {
                return broadcast(-1).lanewiseTemplate(XOR, this);
            } else if (op == NEG) {
                // FIXME: Support this in the JIT.
                return broadcast(0).lanewiseTemplate(SUB, this);
            }
        }
        int opc = opCode(op);
        return VectorSupport.unaryOp(
            opc, getClass(), short.class, length(),
            this,
            UN_IMPL.find(op, opc, (opc_) -> {
              switch (opc_) {
                case VECTOR_OP_NEG: return v0 ->
                        v0.uOp((i, a) -> (short) -a);
                case VECTOR_OP_ABS: return v0 ->
                        v0.uOp((i, a) -> (short) Math.abs(a));
                default: return null;
              }}));
    }
    private static final
    ImplCache<Unary,UnaryOperator<ShortVector>> UN_IMPL
        = new ImplCache<>(Unary.class, ShortVector.class);

    /**
     * {@inheritDoc} <!--workaround-->
     */
    @ForceInline
    public final
    ShortVector lanewise(VectorOperators.Unary op,
                                  VectorMask<Short> m) {
        return blend(lanewise(op), m);
    }

    // Binary lanewise support

    /**
     * {@inheritDoc} <!--workaround-->
     * @see #lanewise(VectorOperators.Binary,short)
     * @see #lanewise(VectorOperators.Binary,short,VectorMask)
     */
    @Override
    public abstract
    ShortVector lanewise(VectorOperators.Binary op,
                                  Vector<Short> v);
    @ForceInline
    final
    ShortVector lanewiseTemplate(VectorOperators.Binary op,
                                          Vector<Short> v) {
        ShortVector that = (ShortVector) v;
        that.check(this);
        if (opKind(op, VO_SPECIAL  | VO_SHIFT)) {
            if (op == FIRST_NONZERO) {
                // FIXME: Support this in the JIT.
                VectorMask<Short> thisNZ
                    = this.viewAsIntegralLanes().compare(NE, (short) 0);
                that = that.blend((short) 0, thisNZ.cast(vspecies()));
                op = OR_UNCHECKED;
            }
            if (opKind(op, VO_SHIFT)) {
                // As per shift specification for Java, mask the shift count.
                // This allows the JIT to ignore some ISA details.
                that = that.lanewise(AND, SHIFT_MASK);
            }
            if (op == ROR || op == ROL) {  // FIXME: JIT should do this
                ShortVector neg = that.lanewise(NEG);
                ShortVector hi = this.lanewise(LSHL, (op == ROR) ? neg : that);
                ShortVector lo = this.lanewise(LSHR, (op == ROR) ? that : neg);
                return hi.lanewise(OR, lo);
            } else if (op == AND_NOT) {
                // FIXME: Support this in the JIT.
                that = that.lanewise(NOT);
                op = AND;
            } else if (op == DIV) {
                VectorMask<Short> eqz = that.eq((short)0);
                if (eqz.anyTrue()) {
                    throw that.divZeroException();
                }
            }
        }
        int opc = opCode(op);
        return VectorSupport.binaryOp(
            opc, getClass(), short.class, length(),
            this, that,
            BIN_IMPL.find(op, opc, (opc_) -> {
              switch (opc_) {
                case VECTOR_OP_ADD: return (v0, v1) ->
                        v0.bOp(v1, (i, a, b) -> (short)(a + b));
                case VECTOR_OP_SUB: return (v0, v1) ->
                        v0.bOp(v1, (i, a, b) -> (short)(a - b));
                case VECTOR_OP_MUL: return (v0, v1) ->
                        v0.bOp(v1, (i, a, b) -> (short)(a * b));
                case VECTOR_OP_DIV: return (v0, v1) ->
                        v0.bOp(v1, (i, a, b) -> (short)(a / b));
                case VECTOR_OP_MAX: return (v0, v1) ->
                        v0.bOp(v1, (i, a, b) -> (short)Math.max(a, b));
                case VECTOR_OP_MIN: return (v0, v1) ->
                        v0.bOp(v1, (i, a, b) -> (short)Math.min(a, b));
                case VECTOR_OP_AND: return (v0, v1) ->
                        v0.bOp(v1, (i, a, b) -> (short)(a & b));
                case VECTOR_OP_OR: return (v0, v1) ->
                        v0.bOp(v1, (i, a, b) -> (short)(a | b));
                case VECTOR_OP_XOR: return (v0, v1) ->
                        v0.bOp(v1, (i, a, b) -> (short)(a ^ b));
                case VECTOR_OP_LSHIFT: return (v0, v1) ->
                        v0.bOp(v1, (i, a, n) -> (short)(a << n));
                case VECTOR_OP_RSHIFT: return (v0, v1) ->
                        v0.bOp(v1, (i, a, n) -> (short)(a >> n));
                case VECTOR_OP_URSHIFT: return (v0, v1) ->
                        v0.bOp(v1, (i, a, n) -> (short)((a & LSHR_SETUP_MASK) >>> n));
                default: return null;
                }}));
    }
    private static final
    ImplCache<Binary,BinaryOperator<ShortVector>> BIN_IMPL
        = new ImplCache<>(Binary.class, ShortVector.class);

    /**
     * {@inheritDoc} <!--workaround-->
     * @see #lanewise(VectorOperators.Binary,short,VectorMask)
     */
    @ForceInline
    public final
    ShortVector lanewise(VectorOperators.Binary op,
                                  Vector<Short> v,
                                  VectorMask<Short> m) {
        ShortVector that = (ShortVector) v;
        if (op == DIV) {
            VectorMask<Short> eqz = that.eq((short)0);
            if (eqz.and(m).anyTrue()) {
                throw that.divZeroException();
            }
            // suppress div/0 exceptions in unset lanes
            that = that.lanewise(NOT, eqz);
            return blend(lanewise(DIV, that), m);
        }
        return blend(lanewise(op, v), m);
    }
    // FIXME: Maybe all of the public final methods in this file (the
    // simple ones that just call lanewise) should be pushed down to
    // the X-VectorBits template.  They can't optimize properly at
    // this level, and must rely on inlining.  Does it work?
    // (If it works, of course keep the code here.)

    /**
     * Combines the lane values of this vector
     * with the value of a broadcast scalar.
     *
     * This is a lane-wise binary operation which applies
     * the selected operation to each lane.
     * The return value will be equal to this expression:
     * {@code this.lanewise(op, this.broadcast(e))}.
     *
     * @param op the operation used to process lane values
     * @param e the input scalar
     * @return the result of applying the operation lane-wise
     *         to the two input vectors
     * @throws UnsupportedOperationException if this vector does
     *         not support the requested operation
     * @see #lanewise(VectorOperators.Binary,Vector)
     * @see #lanewise(VectorOperators.Binary,short,VectorMask)
     */
    @ForceInline
    public final
    ShortVector lanewise(VectorOperators.Binary op,
                                  short e) {
        if (opKind(op, VO_SHIFT) && (short)(int)e == e) {
            return lanewiseShift(op, (int) e);
        }
        if (op == AND_NOT) {
            op = AND; e = (short) ~e;
        }
        return lanewise(op, broadcast(e));
    }

    /**
     * Combines the lane values of this vector
     * with the value of a broadcast scalar,
     * with selection of lane elements controlled by a mask.
     *
     * This is a masked lane-wise binary operation which applies
     * the selected operation to each lane.
     * The return value will be equal to this expression:
     * {@code this.lanewise(op, this.broadcast(e), m)}.
     *
     * @param op the operation used to process lane values
     * @param e the input scalar
     * @param m the mask controlling lane selection
     * @return the result of applying the operation lane-wise
     *         to the input vector and the scalar
     * @throws UnsupportedOperationException if this vector does
     *         not support the requested operation
     * @see #lanewise(VectorOperators.Binary,Vector,VectorMask)
     * @see #lanewise(VectorOperators.Binary,short)
     */
    @ForceInline
    public final
    ShortVector lanewise(VectorOperators.Binary op,
                                  short e,
                                  VectorMask<Short> m) {
        return blend(lanewise(op, e), m);
    }

    /**
     * {@inheritDoc} <!--workaround-->
     * @apiNote
     * When working with vector subtypes like {@code ShortVector},
     * {@linkplain #lanewise(VectorOperators.Binary,short)
     * the more strongly typed method}
     * is typically selected.  It can be explicitly selected
     * using a cast: {@code v.lanewise(op,(short)e)}.
     * The two expressions will produce numerically identical results.
     */
    @ForceInline
    public final
    ShortVector lanewise(VectorOperators.Binary op,
                                  long e) {
        short e1 = (short) e;
        if ((long)e1 != e
            // allow shift ops to clip down their int parameters
            && !(opKind(op, VO_SHIFT) && (int)e1 == e)
            ) {
            vspecies().checkValue(e);  // for exception
        }
        return lanewise(op, e1);
    }

    /**
     * {@inheritDoc} <!--workaround-->
     * @apiNote
     * When working with vector subtypes like {@code ShortVector},
     * {@linkplain #lanewise(VectorOperators.Binary,short,VectorMask)
     * the more strongly typed method}
     * is typically selected.  It can be explicitly selected
     * using a cast: {@code v.lanewise(op,(short)e,m)}.
     * The two expressions will produce numerically identical results.
     */
    @ForceInline
    public final
    ShortVector lanewise(VectorOperators.Binary op,
                                  long e, VectorMask<Short> m) {
        return blend(lanewise(op, e), m);
    }

    /*package-private*/
    abstract ShortVector
    lanewiseShift(VectorOperators.Binary op, int e);

    /*package-private*/
    @ForceInline
    final ShortVector
    lanewiseShiftTemplate(VectorOperators.Binary op, int e) {
        // Special handling for these.  FIXME: Refactor?
        assert(opKind(op, VO_SHIFT));
        // As per shift specification for Java, mask the shift count.
        e &= SHIFT_MASK;
        if (op == ROR || op == ROL) {  // FIXME: JIT should do this
            ShortVector hi = this.lanewise(LSHL, (op == ROR) ? -e : e);
            ShortVector lo = this.lanewise(LSHR, (op == ROR) ? e : -e);
            return hi.lanewise(OR, lo);
        }
        int opc = opCode(op);
        return VectorSupport.broadcastInt(
            opc, getClass(), short.class, length(),
            this, e,
            BIN_INT_IMPL.find(op, opc, (opc_) -> {
              switch (opc_) {
                case VECTOR_OP_LSHIFT: return (v, n) ->
                        v.uOp((i, a) -> (short)(a << n));
                case VECTOR_OP_RSHIFT: return (v, n) ->
                        v.uOp((i, a) -> (short)(a >> n));
                case VECTOR_OP_URSHIFT: return (v, n) ->
                        v.uOp((i, a) -> (short)((a & LSHR_SETUP_MASK) >>> n));
                default: return null;
                }}));
    }
    private static final
    ImplCache<Binary,VectorBroadcastIntOp<ShortVector>> BIN_INT_IMPL
        = new ImplCache<>(Binary.class, ShortVector.class);

    // As per shift specification for Java, mask the shift count.
    // We mask 0X3F (long), 0X1F (int), 0x0F (short), 0x7 (byte).
    // The latter two maskings go beyond the JLS, but seem reasonable
    // since our lane types are first-class types, not just dressed
    // up ints.
    private static final int SHIFT_MASK = (Short.SIZE - 1);
    // Also simulate >>> on sub-word variables with a mask.
    private static final int LSHR_SETUP_MASK = ((1 << Short.SIZE) - 1);

    // Ternary lanewise support

    // Ternary operators come in eight variations:
    //   lanewise(op, [broadcast(e1)|v1], [broadcast(e2)|v2])
    //   lanewise(op, [broadcast(e1)|v1], [broadcast(e2)|v2], mask)

    // It is annoying to support all of these variations of masking
    // and broadcast, but it would be more surprising not to continue
    // the obvious pattern started by unary and binary.

   /**
     * {@inheritDoc} <!--workaround-->
     * @see #lanewise(VectorOperators.Ternary,short,short,VectorMask)
     * @see #lanewise(VectorOperators.Ternary,Vector,short,VectorMask)
     * @see #lanewise(VectorOperators.Ternary,short,Vector,VectorMask)
     * @see #lanewise(VectorOperators.Ternary,short,short)
     * @see #lanewise(VectorOperators.Ternary,Vector,short)
     * @see #lanewise(VectorOperators.Ternary,short,Vector)
     */
    @Override
    public abstract
    ShortVector lanewise(VectorOperators.Ternary op,
                                                  Vector<Short> v1,
                                                  Vector<Short> v2);
    @ForceInline
    final
    ShortVector lanewiseTemplate(VectorOperators.Ternary op,
                                          Vector<Short> v1,
                                          Vector<Short> v2) {
        ShortVector that = (ShortVector) v1;
        ShortVector tother = (ShortVector) v2;
        // It's a word: https://www.dictionary.com/browse/tother
        // See also Chapter 11 of Dickens, Our Mutual Friend:
        // "Totherest Governor," replied Mr Riderhood...
        that.check(this);
        tother.check(this);
        if (op == BITWISE_BLEND) {
            // FIXME: Support this in the JIT.
            that = this.lanewise(XOR, that).lanewise(AND, tother);
            return this.lanewise(XOR, that);
        }
        int opc = opCode(op);
        return VectorSupport.ternaryOp(
            opc, getClass(), short.class, length(),
            this, that, tother,
            TERN_IMPL.find(op, opc, (opc_) -> {
              switch (opc_) {
                default: return null;
                }}));
    }
    private static final
    ImplCache<Ternary,TernaryOperation<ShortVector>> TERN_IMPL
        = new ImplCache<>(Ternary.class, ShortVector.class);

    /**
     * {@inheritDoc} <!--workaround-->
     * @see #lanewise(VectorOperators.Ternary,short,short,VectorMask)
     * @see #lanewise(VectorOperators.Ternary,Vector,short,VectorMask)
     * @see #lanewise(VectorOperators.Ternary,short,Vector,VectorMask)
     */
    @ForceInline
    public final
    ShortVector lanewise(VectorOperators.Ternary op,
                                  Vector<Short> v1,
                                  Vector<Short> v2,
                                  VectorMask<Short> m) {
        return blend(lanewise(op, v1, v2), m);
    }

    /**
     * Combines the lane values of this vector
     * with the values of two broadcast scalars.
     *
     * This is a lane-wise ternary operation which applies
     * the selected operation to each lane.
     * The return value will be equal to this expression:
     * {@code this.lanewise(op, this.broadcast(e1), this.broadcast(e2))}.
     *
     * @param op the operation used to combine lane values
     * @param e1 the first input scalar
     * @param e2 the second input scalar
     * @return the result of applying the operation lane-wise
     *         to the input vector and the scalars
     * @throws UnsupportedOperationException if this vector does
     *         not support the requested operation
     * @see #lanewise(VectorOperators.Ternary,Vector,Vector)
     * @see #lanewise(VectorOperators.Ternary,short,short,VectorMask)
     */
    @ForceInline
    public final
    ShortVector lanewise(VectorOperators.Ternary op, //(op,e1,e2)
                                  short e1,
                                  short e2) {
        return lanewise(op, broadcast(e1), broadcast(e2));
    }

    /**
     * Combines the lane values of this vector
     * with the values of two broadcast scalars,
     * with selection of lane elements controlled by a mask.
     *
     * This is a masked lane-wise ternary operation which applies
     * the selected operation to each lane.
     * The return value will be equal to this expression:
     * {@code this.lanewise(op, this.broadcast(e1), this.broadcast(e2), m)}.
     *
     * @param op the operation used to combine lane values
     * @param e1 the first input scalar
     * @param e2 the second input scalar
     * @param m the mask controlling lane selection
     * @return the result of applying the operation lane-wise
     *         to the input vector and the scalars
     * @throws UnsupportedOperationException if this vector does
     *         not support the requested operation
     * @see #lanewise(VectorOperators.Ternary,Vector,Vector,VectorMask)
     * @see #lanewise(VectorOperators.Ternary,short,short)
     */
    @ForceInline
    public final
    ShortVector lanewise(VectorOperators.Ternary op, //(op,e1,e2,m)
                                  short e1,
                                  short e2,
                                  VectorMask<Short> m) {
        return blend(lanewise(op, e1, e2), m);
    }

    /**
     * Combines the lane values of this vector
     * with the values of another vector and a broadcast scalar.
     *
     * This is a lane-wise ternary operation which applies
     * the selected operation to each lane.
     * The return value will be equal to this expression:
     * {@code this.lanewise(op, v1, this.broadcast(e2))}.
     *
     * @param op the operation used to combine lane values
     * @param v1 the other input vector
     * @param e2 the input scalar
     * @return the result of applying the operation lane-wise
     *         to the input vectors and the scalar
     * @throws UnsupportedOperationException if this vector does
     *         not support the requested operation
     * @see #lanewise(VectorOperators.Ternary,short,short)
     * @see #lanewise(VectorOperators.Ternary,Vector,short,VectorMask)
     */
    @ForceInline
    public final
    ShortVector lanewise(VectorOperators.Ternary op, //(op,v1,e2)
                                  Vector<Short> v1,
                                  short e2) {
        return lanewise(op, v1, broadcast(e2));
    }

    /**
     * Combines the lane values of this vector
     * with the values of another vector and a broadcast scalar,
     * with selection of lane elements controlled by a mask.
     *
     * This is a masked lane-wise ternary operation which applies
     * the selected operation to each lane.
     * The return value will be equal to this expression:
     * {@code this.lanewise(op, v1, this.broadcast(e2), m)}.
     *
     * @param op the operation used to combine lane values
     * @param v1 the other input vector
     * @param e2 the input scalar
     * @param m the mask controlling lane selection
     * @return the result of applying the operation lane-wise
     *         to the input vectors and the scalar
     * @throws UnsupportedOperationException if this vector does
     *         not support the requested operation
     * @see #lanewise(VectorOperators.Ternary,Vector,Vector)
     * @see #lanewise(VectorOperators.Ternary,short,short,VectorMask)
     * @see #lanewise(VectorOperators.Ternary,Vector,short)
     */
    @ForceInline
    public final
    ShortVector lanewise(VectorOperators.Ternary op, //(op,v1,e2,m)
                                  Vector<Short> v1,
                                  short e2,
                                  VectorMask<Short> m) {
        return blend(lanewise(op, v1, e2), m);
    }

    /**
     * Combines the lane values of this vector
     * with the values of another vector and a broadcast scalar.
     *
     * This is a lane-wise ternary operation which applies
     * the selected operation to each lane.
     * The return value will be equal to this expression:
     * {@code this.lanewise(op, this.broadcast(e1), v2)}.
     *
     * @param op the operation used to combine lane values
     * @param e1 the input scalar
     * @param v2 the other input vector
     * @return the result of applying the operation lane-wise
     *         to the input vectors and the scalar
     * @throws UnsupportedOperationException if this vector does
     *         not support the requested operation
     * @see #lanewise(VectorOperators.Ternary,Vector,Vector)
     * @see #lanewise(VectorOperators.Ternary,short,Vector,VectorMask)
     */
    @ForceInline
    public final
    ShortVector lanewise(VectorOperators.Ternary op, //(op,e1,v2)
                                  short e1,
                                  Vector<Short> v2) {
        return lanewise(op, broadcast(e1), v2);
    }

    /**
     * Combines the lane values of this vector
     * with the values of another vector and a broadcast scalar,
     * with selection of lane elements controlled by a mask.
     *
     * This is a masked lane-wise ternary operation which applies
     * the selected operation to each lane.
     * The return value will be equal to this expression:
     * {@code this.lanewise(op, this.broadcast(e1), v2, m)}.
     *
     * @param op the operation used to combine lane values
     * @param e1 the input scalar
     * @param v2 the other input vector
     * @param m the mask controlling lane selection
     * @return the result of applying the operation lane-wise
     *         to the input vectors and the scalar
     * @throws UnsupportedOperationException if this vector does
     *         not support the requested operation
     * @see #lanewise(VectorOperators.Ternary,Vector,Vector,VectorMask)
     * @see #lanewise(VectorOperators.Ternary,short,Vector)
     */
    @ForceInline
    public final
    ShortVector lanewise(VectorOperators.Ternary op, //(op,e1,v2,m)
                                  short e1,
                                  Vector<Short> v2,
                                  VectorMask<Short> m) {
        return blend(lanewise(op, e1, v2), m);
    }

    // (Thus endeth the Great and Mighty Ternary Ogdoad.)
    // https://en.wikipedia.org/wiki/Ogdoad

    /// FULL-SERVICE BINARY METHODS: ADD, SUB, MUL, DIV
    //
    // These include masked and non-masked versions.
    // This subclass adds broadcast (masked or not).

    /**
     * {@inheritDoc} <!--workaround-->
     * @see #add(short)
     */
    @Override
    @ForceInline
    public final ShortVector add(Vector<Short> v) {
        return lanewise(ADD, v);
    }

    /**
     * Adds this vector to the broadcast of an input scalar.
     *
     * This is a lane-wise binary operation which applies
     * the primitive addition operation ({@code +}) to each lane.
     *
     * This method is also equivalent to the expression
     * {@link #lanewise(VectorOperators.Binary,short)
     *    lanewise}{@code (}{@link VectorOperators#ADD
     *    ADD}{@code , e)}.
     *
     * @param e the input scalar
     * @return the result of adding each lane of this vector to the scalar
     * @see #add(Vector)
     * @see #broadcast(short)
     * @see #add(short,VectorMask)
     * @see VectorOperators#ADD
     * @see #lanewise(VectorOperators.Binary,Vector)
     * @see #lanewise(VectorOperators.Binary,short)
     */
    @ForceInline
    public final
    ShortVector add(short e) {
        return lanewise(ADD, e);
    }

    /**
     * {@inheritDoc} <!--workaround-->
     * @see #add(short,VectorMask)
     */
    @Override
    @ForceInline
    public final ShortVector add(Vector<Short> v,
                                          VectorMask<Short> m) {
        return lanewise(ADD, v, m);
    }

    /**
     * Adds this vector to the broadcast of an input scalar,
     * selecting lane elements controlled by a mask.
     *
     * This is a masked lane-wise binary operation which applies
     * the primitive addition operation ({@code +}) to each lane.
     *
     * This method is also equivalent to the expression
     * {@link #lanewise(VectorOperators.Binary,short,VectorMask)
     *    lanewise}{@code (}{@link VectorOperators#ADD
     *    ADD}{@code , s, m)}.
     *
     * @param e the input scalar
     * @param m the mask controlling lane selection
     * @return the result of adding each lane of this vector to the scalar
     * @see #add(Vector,VectorMask)
     * @see #broadcast(short)
     * @see #add(short)
     * @see VectorOperators#ADD
     * @see #lanewise(VectorOperators.Binary,Vector)
     * @see #lanewise(VectorOperators.Binary,short)
     */
    @ForceInline
    public final ShortVector add(short e,
                                          VectorMask<Short> m) {
        return lanewise(ADD, e, m);
    }

    /**
     * {@inheritDoc} <!--workaround-->
     * @see #sub(short)
     */
    @Override
    @ForceInline
    public final ShortVector sub(Vector<Short> v) {
        return lanewise(SUB, v);
    }

    /**
     * Subtracts an input scalar from this vector.
     *
     * This is a masked lane-wise binary operation which applies
     * the primitive subtraction operation ({@code -}) to each lane.
     *
     * This method is also equivalent to the expression
     * {@link #lanewise(VectorOperators.Binary,short)
     *    lanewise}{@code (}{@link VectorOperators#SUB
     *    SUB}{@code , e)}.
     *
     * @param e the input scalar
     * @return the result of subtracting the scalar from each lane of this vector
     * @see #sub(Vector)
     * @see #broadcast(short)
     * @see #sub(short,VectorMask)
     * @see VectorOperators#SUB
     * @see #lanewise(VectorOperators.Binary,Vector)
     * @see #lanewise(VectorOperators.Binary,short)
     */
    @ForceInline
    public final ShortVector sub(short e) {
        return lanewise(SUB, e);
    }

    /**
     * {@inheritDoc} <!--workaround-->
     * @see #sub(short,VectorMask)
     */
    @Override
    @ForceInline
    public final ShortVector sub(Vector<Short> v,
                                          VectorMask<Short> m) {
        return lanewise(SUB, v, m);
    }

    /**
     * Subtracts an input scalar from this vector
     * under the control of a mask.
     *
     * This is a masked lane-wise binary operation which applies
     * the primitive subtraction operation ({@code -}) to each lane.
     *
     * This method is also equivalent to the expression
     * {@link #lanewise(VectorOperators.Binary,short,VectorMask)
     *    lanewise}{@code (}{@link VectorOperators#SUB
     *    SUB}{@code , s, m)}.
     *
     * @param e the input scalar
     * @param m the mask controlling lane selection
     * @return the result of subtracting the scalar from each lane of this vector
     * @see #sub(Vector,VectorMask)
     * @see #broadcast(short)
     * @see #sub(short)
     * @see VectorOperators#SUB
     * @see #lanewise(VectorOperators.Binary,Vector)
     * @see #lanewise(VectorOperators.Binary,short)
     */
    @ForceInline
    public final ShortVector sub(short e,
                                          VectorMask<Short> m) {
        return lanewise(SUB, e, m);
    }

    /**
     * {@inheritDoc} <!--workaround-->
     * @see #mul(short)
     */
    @Override
    @ForceInline
    public final ShortVector mul(Vector<Short> v) {
        return lanewise(MUL, v);
    }

    /**
     * Multiplies this vector by the broadcast of an input scalar.
     *
     * This is a lane-wise binary operation which applies
     * the primitive multiplication operation ({@code *}) to each lane.
     *
     * This method is also equivalent to the expression
     * {@link #lanewise(VectorOperators.Binary,short)
     *    lanewise}{@code (}{@link VectorOperators#MUL
     *    MUL}{@code , e)}.
     *
     * @param e the input scalar
     * @return the result of multiplying this vector by the given scalar
     * @see #mul(Vector)
     * @see #broadcast(short)
     * @see #mul(short,VectorMask)
     * @see VectorOperators#MUL
     * @see #lanewise(VectorOperators.Binary,Vector)
     * @see #lanewise(VectorOperators.Binary,short)
     */
    @ForceInline
    public final ShortVector mul(short e) {
        return lanewise(MUL, e);
    }

    /**
     * {@inheritDoc} <!--workaround-->
     * @see #mul(short,VectorMask)
     */
    @Override
    @ForceInline
    public final ShortVector mul(Vector<Short> v,
                                          VectorMask<Short> m) {
        return lanewise(MUL, v, m);
    }

    /**
     * Multiplies this vector by the broadcast of an input scalar,
     * selecting lane elements controlled by a mask.
     *
     * This is a masked lane-wise binary operation which applies
     * the primitive multiplication operation ({@code *}) to each lane.
     *
     * This method is also equivalent to the expression
     * {@link #lanewise(VectorOperators.Binary,short,VectorMask)
     *    lanewise}{@code (}{@link VectorOperators#MUL
     *    MUL}{@code , s, m)}.
     *
     * @param e the input scalar
     * @param m the mask controlling lane selection
     * @return the result of muling each lane of this vector to the scalar
     * @see #mul(Vector,VectorMask)
     * @see #broadcast(short)
     * @see #mul(short)
     * @see VectorOperators#MUL
     * @see #lanewise(VectorOperators.Binary,Vector)
     * @see #lanewise(VectorOperators.Binary,short)
     */
    @ForceInline
    public final ShortVector mul(short e,
                                          VectorMask<Short> m) {
        return lanewise(MUL, e, m);
    }

    /**
     * {@inheritDoc} <!--workaround-->
     * @apiNote If there is a zero divisor, {@code
     * ArithmeticException} will be thrown.
     */
    @Override
    @ForceInline
    public final ShortVector div(Vector<Short> v) {
        return lanewise(DIV, v);
    }

    /**
     * Divides this vector by the broadcast of an input scalar.
     *
     * This is a lane-wise binary operation which applies
     * the primitive division operation ({@code /}) to each lane.
     *
     * This method is also equivalent to the expression
     * {@link #lanewise(VectorOperators.Binary,short)
     *    lanewise}{@code (}{@link VectorOperators#DIV
     *    DIV}{@code , e)}.
     *
     * @apiNote If there is a zero divisor, {@code
     * ArithmeticException} will be thrown.
     *
     * @param e the input scalar
     * @return the result of dividing each lane of this vector by the scalar
     * @see #div(Vector)
     * @see #broadcast(short)
     * @see #div(short,VectorMask)
     * @see VectorOperators#DIV
     * @see #lanewise(VectorOperators.Binary,Vector)
     * @see #lanewise(VectorOperators.Binary,short)
     */
    @ForceInline
    public final ShortVector div(short e) {
        return lanewise(DIV, e);
    }

    /**
     * {@inheritDoc} <!--workaround-->
     * @see #div(short,VectorMask)
     * @apiNote If there is a zero divisor, {@code
     * ArithmeticException} will be thrown.
     */
    @Override
    @ForceInline
    public final ShortVector div(Vector<Short> v,
                                          VectorMask<Short> m) {
        return lanewise(DIV, v, m);
    }

    /**
     * Divides this vector by the broadcast of an input scalar,
     * selecting lane elements controlled by a mask.
     *
     * This is a masked lane-wise binary operation which applies
     * the primitive division operation ({@code /}) to each lane.
     *
     * This method is also equivalent to the expression
     * {@link #lanewise(VectorOperators.Binary,short,VectorMask)
     *    lanewise}{@code (}{@link VectorOperators#DIV
     *    DIV}{@code , s, m)}.
     *
     * @apiNote If there is a zero divisor, {@code
     * ArithmeticException} will be thrown.
     *
     * @param e the input scalar
     * @param m the mask controlling lane selection
     * @return the result of dividing each lane of this vector by the scalar
     * @see #div(Vector,VectorMask)
     * @see #broadcast(short)
     * @see #div(short)
     * @see VectorOperators#DIV
     * @see #lanewise(VectorOperators.Binary,Vector)
     * @see #lanewise(VectorOperators.Binary,short)
     */
    @ForceInline
    public final ShortVector div(short e,
                                          VectorMask<Short> m) {
        return lanewise(DIV, e, m);
    }

    /// END OF FULL-SERVICE BINARY METHODS

    /// SECOND-TIER BINARY METHODS
    //
    // There are no masked versions.

    /**
     * {@inheritDoc} <!--workaround-->
     */
    @Override
    @ForceInline
    public final ShortVector min(Vector<Short> v) {
        return lanewise(MIN, v);
    }

    // FIXME:  "broadcast of an input scalar" is really wordy.  Reduce?
    /**
     * Computes the smaller of this vector and the broadcast of an input scalar.
     *
     * This is a lane-wise binary operation which applies the
     * operation {@code Math.min()} to each pair of
     * corresponding lane values.
     *
     * This method is also equivalent to the expression
     * {@link #lanewise(VectorOperators.Binary,short)
     *    lanewise}{@code (}{@link VectorOperators#MIN
     *    MIN}{@code , e)}.
     *
     * @param e the input scalar
     * @return the result of multiplying this vector by the given scalar
     * @see #min(Vector)
     * @see #broadcast(short)
     * @see VectorOperators#MIN
     * @see #lanewise(VectorOperators.Binary,short,VectorMask)
     */
    @ForceInline
    public final ShortVector min(short e) {
        return lanewise(MIN, e);
    }

    /**
     * {@inheritDoc} <!--workaround-->
     */
    @Override
    @ForceInline
    public final ShortVector max(Vector<Short> v) {
        return lanewise(MAX, v);
    }

    /**
     * Computes the larger of this vector and the broadcast of an input scalar.
     *
     * This is a lane-wise binary operation which applies the
     * operation {@code Math.max()} to each pair of
     * corresponding lane values.
     *
     * This method is also equivalent to the expression
     * {@link #lanewise(VectorOperators.Binary,short)
     *    lanewise}{@code (}{@link VectorOperators#MAX
     *    MAX}{@code , e)}.
     *
     * @param e the input scalar
     * @return the result of multiplying this vector by the given scalar
     * @see #max(Vector)
     * @see #broadcast(short)
     * @see VectorOperators#MAX
     * @see #lanewise(VectorOperators.Binary,short,VectorMask)
     */
    @ForceInline
    public final ShortVector max(short e) {
        return lanewise(MAX, e);
    }

    // common bitwise operators: and, or, not (with scalar versions)
    /**
     * Computes the bitwise logical conjunction ({@code &})
     * of this vector and a second input vector.
     *
     * This is a lane-wise binary operation which applies the
     * the primitive bitwise "and" operation ({@code &})
     * to each pair of corresponding lane values.
     *
     * This method is also equivalent to the expression
     * {@link #lanewise(VectorOperators.Binary,Vector)
     *    lanewise}{@code (}{@link VectorOperators#AND
     *    AND}{@code , v)}.
     *
     * <p>
     * This is not a full-service named operation like
     * {@link #add(Vector) add}.  A masked version of
     * this operation is not directly available
     * but may be obtained via the masked version of
     * {@code lanewise}.
     *
     * @param v a second input vector
     * @return the bitwise {@code &} of this vector and the second input vector
     * @see #and(short)
     * @see #or(Vector)
     * @see #not()
     * @see VectorOperators#AND
     * @see #lanewise(VectorOperators.Binary,Vector,VectorMask)
     */
    @ForceInline
    public final ShortVector and(Vector<Short> v) {
        return lanewise(AND, v);
    }

    /**
     * Computes the bitwise logical conjunction ({@code &})
     * of this vector and a scalar.
     *
     * This is a lane-wise binary operation which applies the
     * the primitive bitwise "and" operation ({@code &})
     * to each pair of corresponding lane values.
     *
     * This method is also equivalent to the expression
     * {@link #lanewise(VectorOperators.Binary,Vector)
     *    lanewise}{@code (}{@link VectorOperators#AND
     *    AND}{@code , e)}.
     *
     * @param e an input scalar
     * @return the bitwise {@code &} of this vector and scalar
     * @see #and(Vector)
     * @see VectorOperators#AND
     * @see #lanewise(VectorOperators.Binary,Vector,VectorMask)
     */
    @ForceInline
    public final ShortVector and(short e) {
        return lanewise(AND, e);
    }

    /**
     * Computes the bitwise logical disjunction ({@code |})
     * of this vector and a second input vector.
     *
     * This is a lane-wise binary operation which applies the
     * the primitive bitwise "or" operation ({@code |})
     * to each pair of corresponding lane values.
     *
     * This method is also equivalent to the expression
     * {@link #lanewise(VectorOperators.Binary,Vector)
     *    lanewise}{@code (}{@link VectorOperators#OR
     *    AND}{@code , v)}.
     *
     * <p>
     * This is not a full-service named operation like
     * {@link #add(Vector) add}.  A masked version of
     * this operation is not directly available
     * but may be obtained via the masked version of
     * {@code lanewise}.
     *
     * @param v a second input vector
     * @return the bitwise {@code |} of this vector and the second input vector
     * @see #or(short)
     * @see #and(Vector)
     * @see #not()
     * @see VectorOperators#OR
     * @see #lanewise(VectorOperators.Binary,Vector,VectorMask)
     */
    @ForceInline
    public final ShortVector or(Vector<Short> v) {
        return lanewise(OR, v);
    }

    /**
     * Computes the bitwise logical disjunction ({@code |})
     * of this vector and a scalar.
     *
     * This is a lane-wise binary operation which applies the
     * the primitive bitwise "or" operation ({@code |})
     * to each pair of corresponding lane values.
     *
     * This method is also equivalent to the expression
     * {@link #lanewise(VectorOperators.Binary,Vector)
     *    lanewise}{@code (}{@link VectorOperators#OR
     *    OR}{@code , e)}.
     *
     * @param e an input scalar
     * @return the bitwise {@code |} of this vector and scalar
     * @see #or(Vector)
     * @see VectorOperators#OR
     * @see #lanewise(VectorOperators.Binary,Vector,VectorMask)
     */
    @ForceInline
    public final ShortVector or(short e) {
        return lanewise(OR, e);
    }



    /// UNARY METHODS

    /**
     * {@inheritDoc} <!--workaround-->
     */
    @Override
    @ForceInline
    public final
    ShortVector neg() {
        return lanewise(NEG);
    }

    /**
     * {@inheritDoc} <!--workaround-->
     */
    @Override
    @ForceInline
    public final
    ShortVector abs() {
        return lanewise(ABS);
    }

    // not (~)
    /**
     * Computes the bitwise logical complement ({@code ~})
     * of this vector.
     *
     * This is a lane-wise binary operation which applies the
     * the primitive bitwise "not" operation ({@code ~})
     * to each lane value.
     *
     * This method is also equivalent to the expression
     * {@link #lanewise(VectorOperators.Unary)
     *    lanewise}{@code (}{@link VectorOperators#NOT
     *    NOT}{@code )}.
     *
     * <p>
     * This is not a full-service named operation like
     * {@link #add(Vector) add}.  A masked version of
     * this operation is not directly available
     * but may be obtained via the masked version of
     * {@code lanewise}.
     *
     * @return the bitwise complement {@code ~} of this vector
     * @see #and(Vector)
     * @see VectorOperators#NOT
     * @see #lanewise(VectorOperators.Unary,VectorMask)
     */
    @ForceInline
    public final ShortVector not() {
        return lanewise(NOT);
    }


    /// COMPARISONS

    /**
     * {@inheritDoc} <!--workaround-->
     */
    @Override
    @ForceInline
    public final
    VectorMask<Short> eq(Vector<Short> v) {
        return compare(EQ, v);
    }

    /**
     * Tests if this vector is equal to an input scalar.
     *
     * This is a lane-wise binary test operation which applies
     * the primitive equals operation ({@code ==}) to each lane.
     * The result is the same as {@code compare(VectorOperators.Comparison.EQ, e)}.
     *
     * @param e the input scalar
     * @return the result mask of testing if this vector
     *         is equal to {@code e}
     * @see #compare(VectorOperators.Comparison,short)
     */
    @ForceInline
    public final
    VectorMask<Short> eq(short e) {
        return compare(EQ, e);
    }

    /**
     * {@inheritDoc} <!--workaround-->
     */
    @Override
    @ForceInline
    public final
    VectorMask<Short> lt(Vector<Short> v) {
        return compare(LT, v);
    }

    /**
     * Tests if this vector is less than an input scalar.
     *
     * This is a lane-wise binary test operation which applies
     * the primitive less than operation ({@code <}) to each lane.
     * The result is the same as {@code compare(VectorOperators.LT, e)}.
     *
     * @param e the input scalar
     * @return the mask result of testing if this vector
     *         is less than the input scalar
     * @see #compare(VectorOperators.Comparison,short)
     */
    @ForceInline
    public final
    VectorMask<Short> lt(short e) {
        return compare(LT, e);
    }

    /**
     * {@inheritDoc} <!--workaround-->
     */
    @Override
    public abstract
    VectorMask<Short> test(VectorOperators.Test op);

    /*package-private*/
    @ForceInline
    final
    <M extends VectorMask<Short>>
    M testTemplate(Class<M> maskType, Test op) {
        ShortSpecies vsp = vspecies();
        if (opKind(op, VO_SPECIAL)) {
            ShortVector bits = this.viewAsIntegralLanes();
            VectorMask<Short> m;
            if (op == IS_DEFAULT) {
                m = bits.compare(EQ, (short) 0);
            } else if (op == IS_NEGATIVE) {
                m = bits.compare(LT, (short) 0);
            }
            else {
                throw new AssertionError(op);
            }
            return maskType.cast(m);
        }
        int opc = opCode(op);
        throw new AssertionError(op);
    }

    /**
     * {@inheritDoc} <!--workaround-->
     */
    @Override
    @ForceInline
    public final
    VectorMask<Short> test(VectorOperators.Test op,
                                  VectorMask<Short> m) {
        return test(op).and(m);
    }

    /**
     * {@inheritDoc} <!--workaround-->
     */
    @Override
    public abstract
    VectorMask<Short> compare(VectorOperators.Comparison op, Vector<Short> v);

    /*package-private*/
    @ForceInline
    final
    <M extends VectorMask<Short>>
    M compareTemplate(Class<M> maskType, Comparison op, Vector<Short> v) {
        Objects.requireNonNull(v);
        ShortSpecies vsp = vspecies();
        ShortVector that = (ShortVector) v;
        that.check(this);
        int opc = opCode(op);
        return VectorSupport.compare(
            opc, getClass(), maskType, short.class, length(),
            this, that,
            (cond, v0, v1) -> {
                AbstractMask<Short> m
                    = v0.bTest(cond, v1, (cond_, i, a, b)
                               -> compareWithOp(cond, a, b));
                @SuppressWarnings("unchecked")
                M m2 = (M) m;
                return m2;
            });
    }

    @ForceInline
    private static boolean compareWithOp(int cond, short a, short b) {
        return switch (cond) {
            case BT_eq -> a == b;
            case BT_ne -> a != b;
            case BT_lt -> a < b;
            case BT_le -> a <= b;
            case BT_gt -> a > b;
            case BT_ge -> a >= b;
            case BT_ult -> Short.compareUnsigned(a, b) < 0;
            case BT_ule -> Short.compareUnsigned(a, b) <= 0;
            case BT_ugt -> Short.compareUnsigned(a, b) > 0;
            case BT_uge -> Short.compareUnsigned(a, b) >= 0;
            default -> throw new AssertionError();
        };
    }

    /**
     * {@inheritDoc} <!--workaround-->
     */
    @Override
    @ForceInline
    public final
    VectorMask<Short> compare(VectorOperators.Comparison op,
                                  Vector<Short> v,
                                  VectorMask<Short> m) {
        return compare(op, v).and(m);
    }

    /**
     * Tests this vector by comparing it with an input scalar,
     * according to the given comparison operation.
     *
     * This is a lane-wise binary test operation which applies
     * the comparison operation to each lane.
     * <p>
     * The result is the same as
     * {@code compare(op, broadcast(species(), e))}.
     * That is, the scalar may be regarded as broadcast to
     * a vector of the same species, and then compared
     * against the original vector, using the selected
     * comparison operation.
     *
     * @param op the operation used to compare lane values
     * @param e the input scalar
     * @return the mask result of testing lane-wise if this vector
     *         compares to the input, according to the selected
     *         comparison operator
     * @see ShortVector#compare(VectorOperators.Comparison,Vector)
     * @see #eq(short)
     * @see #lt(short)
     */
    public abstract
    VectorMask<Short> compare(Comparison op, short e);

    /*package-private*/
    @ForceInline
    final
    <M extends VectorMask<Short>>
    M compareTemplate(Class<M> maskType, Comparison op, short e) {
        return compareTemplate(maskType, op, broadcast(e));
    }

    /**
     * Tests this vector by comparing it with an input scalar,
     * according to the given comparison operation,
     * in lanes selected by a mask.
     *
     * This is a masked lane-wise binary test operation which applies
     * to each pair of corresponding lane values.
     *
     * The returned result is equal to the expression
     * {@code compare(op,s).and(m)}.
     *
     * @param op the operation used to compare lane values
     * @param e the input scalar
     * @param m the mask controlling lane selection
     * @return the mask result of testing lane-wise if this vector
     *         compares to the input, according to the selected
     *         comparison operator,
     *         and only in the lanes selected by the mask
     * @see ShortVector#compare(VectorOperators.Comparison,Vector,VectorMask)
     */
    @ForceInline
    public final VectorMask<Short> compare(VectorOperators.Comparison op,
                                               short e,
                                               VectorMask<Short> m) {
        return compare(op, e).and(m);
    }

    /**
     * {@inheritDoc} <!--workaround-->
     */
    @Override
    public abstract
    VectorMask<Short> compare(Comparison op, long e);

    /*package-private*/
    @ForceInline
    final
    <M extends VectorMask<Short>>
    M compareTemplate(Class<M> maskType, Comparison op, long e) {
        return compareTemplate(maskType, op, broadcast(e));
    }

    /**
     * {@inheritDoc} <!--workaround-->
     */
    @Override
    @ForceInline
    public final
    VectorMask<Short> compare(Comparison op, long e, VectorMask<Short> m) {
        return compare(op, broadcast(e), m);
    }



    /**
     * {@inheritDoc} <!--workaround-->
     */
    @Override public abstract
    ShortVector blend(Vector<Short> v, VectorMask<Short> m);

    /*package-private*/
    @ForceInline
    final
    <M extends VectorMask<Short>>
    ShortVector
    blendTemplate(Class<M> maskType, ShortVector v, M m) {
        v.check(this);
        return VectorSupport.blend(
            getClass(), maskType, short.class, length(),
            this, v, m,
            (v0, v1, m_) -> v0.bOp(v1, m_, (i, a, b) -> b));
    }

    /**
     * {@inheritDoc} <!--workaround-->
     */
    @Override public abstract ShortVector addIndex(int scale);

    /*package-private*/
    @ForceInline
    final ShortVector addIndexTemplate(int scale) {
        ShortSpecies vsp = vspecies();
        // make sure VLENGTH*scale doesn't overflow:
        vsp.checkScale(scale);
        return VectorSupport.indexVector(
            getClass(), short.class, length(),
            this, scale, vsp,
            (v, scale_, s)
            -> {
                // If the platform doesn't support an INDEX
                // instruction directly, load IOTA from memory
                // and multiply.
                ShortVector iota = s.iota();
                short sc = (short) scale_;
                return v.add(sc == 1 ? iota : iota.mul(sc));
            });
    }

    /**
     * Replaces selected lanes of this vector with
     * a scalar value
     * under the control of a mask.
     *
     * This is a masked lane-wise binary operation which
     * selects each lane value from one or the other input.
     *
     * The returned result is equal to the expression
     * {@code blend(broadcast(e),m)}.
     *
     * @param e the input scalar, containing the replacement lane value
     * @param m the mask controlling lane selection of the scalar
     * @return the result of blending the lane elements of this vector with
     *         the scalar value
     */
    @ForceInline
    public final ShortVector blend(short e,
                                            VectorMask<Short> m) {
        return blend(broadcast(e), m);
    }

    /**
     * Replaces selected lanes of this vector with
     * a scalar value
     * under the control of a mask.
     *
     * This is a masked lane-wise binary operation which
     * selects each lane value from one or the other input.
     *
     * The returned result is equal to the expression
     * {@code blend(broadcast(e),m)}.
     *
     * @param e the input scalar, containing the replacement lane value
     * @param m the mask controlling lane selection of the scalar
     * @return the result of blending the lane elements of this vector with
     *         the scalar value
     */
    @ForceInline
    public final ShortVector blend(long e,
                                            VectorMask<Short> m) {
        return blend(broadcast(e), m);
    }

    /**
     * {@inheritDoc} <!--workaround-->
     */
    @Override
    public abstract
    ShortVector slice(int origin, Vector<Short> v1);

    /*package-private*/
    final
    @ForceInline
    ShortVector sliceTemplate(int origin, Vector<Short> v1) {
        ShortVector that = (ShortVector) v1;
        that.check(this);
        Objects.checkIndex(origin, length() + 1);
        VectorShuffle<Short> iota = iotaShuffle();
        VectorMask<Short> blendMask = iota.toVector().compare(VectorOperators.LT, (broadcast((short)(length() - origin))));
        iota = iotaShuffle(origin, 1, true);
        return that.rearrange(iota).blend(this.rearrange(iota), blendMask);
    }

    /**
     * {@inheritDoc} <!--workaround-->
     */
    @Override
    @ForceInline
    public final
    ShortVector slice(int origin,
                               Vector<Short> w,
                               VectorMask<Short> m) {
        return broadcast(0).blend(slice(origin, w), m);
    }

    /**
     * {@inheritDoc} <!--workaround-->
     */
    @Override
    public abstract
    ShortVector slice(int origin);

    /*package-private*/
    final
    @ForceInline
    ShortVector sliceTemplate(int origin) {
        Objects.checkIndex(origin, length() + 1);
        VectorShuffle<Short> iota = iotaShuffle();
        VectorMask<Short> blendMask = iota.toVector().compare(VectorOperators.LT, (broadcast((short)(length() - origin))));
        iota = iotaShuffle(origin, 1, true);
        return vspecies().zero().blend(this.rearrange(iota), blendMask);
    }

    /**
     * {@inheritDoc} <!--workaround-->
     */
    @Override
    public abstract
    ShortVector unslice(int origin, Vector<Short> w, int part);

    /*package-private*/
    final
    @ForceInline
    ShortVector
    unsliceTemplate(int origin, Vector<Short> w, int part) {
        ShortVector that = (ShortVector) w;
        that.check(this);
        Objects.checkIndex(origin, length() + 1);
        VectorShuffle<Short> iota = iotaShuffle();
        VectorMask<Short> blendMask = iota.toVector().compare((part == 0) ? VectorOperators.GE : VectorOperators.LT,
                                                                  (broadcast((short)(origin))));
        iota = iotaShuffle(-origin, 1, true);
        return that.blend(this.rearrange(iota), blendMask);
    }

    /*package-private*/
    final
    @ForceInline
    <M extends VectorMask<Short>>
    ShortVector
    unsliceTemplate(Class<M> maskType, int origin, Vector<Short> w, int part, M m) {
        ShortVector that = (ShortVector) w;
        that.check(this);
        ShortVector slice = that.sliceTemplate(origin, that);
        slice = slice.blendTemplate(maskType, this, m);
        return slice.unsliceTemplate(origin, w, part);
    }

    /**
     * {@inheritDoc} <!--workaround-->
     */
    @Override
    public abstract
    ShortVector unslice(int origin, Vector<Short> w, int part, VectorMask<Short> m);

    /**
     * {@inheritDoc} <!--workaround-->
     */
    @Override
    public abstract
    ShortVector unslice(int origin);

    /*package-private*/
    final
    @ForceInline
    ShortVector
    unsliceTemplate(int origin) {
        Objects.checkIndex(origin, length() + 1);
        VectorShuffle<Short> iota = iotaShuffle();
        VectorMask<Short> blendMask = iota.toVector().compare(VectorOperators.GE,
                                                                  (broadcast((short)(origin))));
        iota = iotaShuffle(-origin, 1, true);
        return vspecies().zero().blend(this.rearrange(iota), blendMask);
    }

    private ArrayIndexOutOfBoundsException
    wrongPartForSlice(int part) {
        String msg = String.format("bad part number %d for slice operation",
                                   part);
        return new ArrayIndexOutOfBoundsException(msg);
    }

    /**
     * {@inheritDoc} <!--workaround-->
     */
    @Override
    public abstract
    ShortVector rearrange(VectorShuffle<Short> m);

    /*package-private*/
    @ForceInline
    final
    <S extends VectorShuffle<Short>>
    ShortVector rearrangeTemplate(Class<S> shuffletype, S shuffle) {
        shuffle.checkIndexes();
        return VectorSupport.rearrangeOp(
            getClass(), shuffletype, short.class, length(),
            this, shuffle,
            (v1, s_) -> v1.uOp((i, a) -> {
                int ei = s_.laneSource(i);
                return v1.lane(ei);
            }));
    }

    /**
     * {@inheritDoc} <!--workaround-->
     */
    @Override
    public abstract
    ShortVector rearrange(VectorShuffle<Short> s,
                                   VectorMask<Short> m);

    /*package-private*/
    @ForceInline
    final
    <S extends VectorShuffle<Short>>
    ShortVector rearrangeTemplate(Class<S> shuffletype,
                                           S shuffle,
                                           VectorMask<Short> m) {
        ShortVector unmasked =
            VectorSupport.rearrangeOp(
                getClass(), shuffletype, short.class, length(),
                this, shuffle,
                (v1, s_) -> v1.uOp((i, a) -> {
                    int ei = s_.laneSource(i);
                    return ei < 0 ? 0 : v1.lane(ei);
                }));
        VectorMask<Short> valid = shuffle.laneIsValid();
        if (m.andNot(valid).anyTrue()) {
            shuffle.checkIndexes();
            throw new AssertionError();
        }
        return broadcast((short)0).blend(unmasked, m);
    }

    /**
     * {@inheritDoc} <!--workaround-->
     */
    @Override
    public abstract
    ShortVector rearrange(VectorShuffle<Short> s,
                                   Vector<Short> v);

    /*package-private*/
    @ForceInline
    final
    <S extends VectorShuffle<Short>>
    ShortVector rearrangeTemplate(Class<S> shuffletype,
                                           S shuffle,
                                           ShortVector v) {
        VectorMask<Short> valid = shuffle.laneIsValid();
        @SuppressWarnings("unchecked")
        S ws = (S) shuffle.wrapIndexes();
        ShortVector r0 =
            VectorSupport.rearrangeOp(
                getClass(), shuffletype, short.class, length(),
                this, ws,
                (v0, s_) -> v0.uOp((i, a) -> {
                    int ei = s_.laneSource(i);
                    return v0.lane(ei);
                }));
        ShortVector r1 =
            VectorSupport.rearrangeOp(
                getClass(), shuffletype, short.class, length(),
                v, ws,
                (v1, s_) -> v1.uOp((i, a) -> {
                    int ei = s_.laneSource(i);
                    return v1.lane(ei);
                }));
        return r1.blend(r0, valid);
    }

    @ForceInline
    private final
    VectorShuffle<Short> toShuffle0(ShortSpecies dsp) {
        short[] a = toArray();
        int[] sa = new int[a.length];
        for (int i = 0; i < a.length; i++) {
            sa[i] = (int) a[i];
        }
        return VectorShuffle.fromArray(dsp, sa, 0);
    }

    /*package-private*/
    @ForceInline
    final
    VectorShuffle<Short> toShuffleTemplate(Class<?> shuffleType) {
        ShortSpecies vsp = vspecies();
        return VectorSupport.convert(VectorSupport.VECTOR_OP_CAST,
                                     getClass(), short.class, length(),
                                     shuffleType, byte.class, length(),
                                     this, vsp,
                                     ShortVector::toShuffle0);
    }

    /**
     * {@inheritDoc} <!--workaround-->
     */
    @Override
    public abstract
    ShortVector selectFrom(Vector<Short> v);

    /*package-private*/
    @ForceInline
    final ShortVector selectFromTemplate(ShortVector v) {
        return v.rearrange(this.toShuffle());
    }

    /**
     * {@inheritDoc} <!--workaround-->
     */
    @Override
    public abstract
    ShortVector selectFrom(Vector<Short> s, VectorMask<Short> m);

    /*package-private*/
    @ForceInline
    final ShortVector selectFromTemplate(ShortVector v,
                                                  AbstractMask<Short> m) {
        return v.rearrange(this.toShuffle(), m);
    }

    /// Ternary operations

    /**
     * Blends together the bits of two vectors under
     * the control of a third, which supplies mask bits.
     *
     * This is a lane-wise ternary operation which performs
     * a bitwise blending operation {@code (a&~c)|(b&c)}
     * to each lane.
     *
     * This method is also equivalent to the expression
     * {@link #lanewise(VectorOperators.Ternary,Vector,Vector)
     *    lanewise}{@code (}{@link VectorOperators#BITWISE_BLEND
     *    BITWISE_BLEND}{@code , bits, mask)}.
     *
     * @param bits input bits to blend into the current vector
     * @param mask a bitwise mask to enable blending of the input bits
     * @return the bitwise blend of the given bits into the current vector,
     *         under control of the bitwise mask
     * @see #bitwiseBlend(short,short)
     * @see #bitwiseBlend(short,Vector)
     * @see #bitwiseBlend(Vector,short)
     * @see VectorOperators#BITWISE_BLEND
     * @see #lanewise(VectorOperators.Ternary,Vector,Vector,VectorMask)
     */
    @ForceInline
    public final
    ShortVector bitwiseBlend(Vector<Short> bits, Vector<Short> mask) {
        return lanewise(BITWISE_BLEND, bits, mask);
    }

    /**
     * Blends together the bits of a vector and a scalar under
     * the control of another scalar, which supplies mask bits.
     *
     * This is a lane-wise ternary operation which performs
     * a bitwise blending operation {@code (a&~c)|(b&c)}
     * to each lane.
     *
     * This method is also equivalent to the expression
     * {@link #lanewise(VectorOperators.Ternary,Vector,Vector)
     *    lanewise}{@code (}{@link VectorOperators#BITWISE_BLEND
     *    BITWISE_BLEND}{@code , bits, mask)}.
     *
     * @param bits input bits to blend into the current vector
     * @param mask a bitwise mask to enable blending of the input bits
     * @return the bitwise blend of the given bits into the current vector,
     *         under control of the bitwise mask
     * @see #bitwiseBlend(Vector,Vector)
     * @see VectorOperators#BITWISE_BLEND
     * @see #lanewise(VectorOperators.Ternary,short,short,VectorMask)
     */
    @ForceInline
    public final
    ShortVector bitwiseBlend(short bits, short mask) {
        return lanewise(BITWISE_BLEND, bits, mask);
    }

    /**
     * Blends together the bits of a vector and a scalar under
     * the control of another vector, which supplies mask bits.
     *
     * This is a lane-wise ternary operation which performs
     * a bitwise blending operation {@code (a&~c)|(b&c)}
     * to each lane.
     *
     * This method is also equivalent to the expression
     * {@link #lanewise(VectorOperators.Ternary,Vector,Vector)
     *    lanewise}{@code (}{@link VectorOperators#BITWISE_BLEND
     *    BITWISE_BLEND}{@code , bits, mask)}.
     *
     * @param bits input bits to blend into the current vector
     * @param mask a bitwise mask to enable blending of the input bits
     * @return the bitwise blend of the given bits into the current vector,
     *         under control of the bitwise mask
     * @see #bitwiseBlend(Vector,Vector)
     * @see VectorOperators#BITWISE_BLEND
     * @see #lanewise(VectorOperators.Ternary,short,Vector,VectorMask)
     */
    @ForceInline
    public final
    ShortVector bitwiseBlend(short bits, Vector<Short> mask) {
        return lanewise(BITWISE_BLEND, bits, mask);
    }

    /**
     * Blends together the bits of two vectors under
     * the control of a scalar, which supplies mask bits.
     *
     * This is a lane-wise ternary operation which performs
     * a bitwise blending operation {@code (a&~c)|(b&c)}
     * to each lane.
     *
     * This method is also equivalent to the expression
     * {@link #lanewise(VectorOperators.Ternary,Vector,Vector)
     *    lanewise}{@code (}{@link VectorOperators#BITWISE_BLEND
     *    BITWISE_BLEND}{@code , bits, mask)}.
     *
     * @param bits input bits to blend into the current vector
     * @param mask a bitwise mask to enable blending of the input bits
     * @return the bitwise blend of the given bits into the current vector,
     *         under control of the bitwise mask
     * @see #bitwiseBlend(Vector,Vector)
     * @see VectorOperators#BITWISE_BLEND
     * @see #lanewise(VectorOperators.Ternary,Vector,short,VectorMask)
     */
    @ForceInline
    public final
    ShortVector bitwiseBlend(Vector<Short> bits, short mask) {
        return lanewise(BITWISE_BLEND, bits, mask);
    }


    // Type specific horizontal reductions

    /**
     * Returns a value accumulated from all the lanes of this vector.
     *
     * This is an associative cross-lane reduction operation which
     * applies the specified operation to all the lane elements.
     * <p>
     * A few reduction operations do not support arbitrary reordering
     * of their operands, yet are included here because of their
     * usefulness.
     * <ul>
     * <li>
     * In the case of {@code FIRST_NONZERO}, the reduction returns
     * the value from the lowest-numbered non-zero lane.
     * <li>
     * All other reduction operations are fully commutative and
     * associative.  The implementation can choose any order of
     * processing, yet it will always produce the same result.
     * </ul>
     *
     * @param op the operation used to combine lane values
     * @return the accumulated result
     * @throws UnsupportedOperationException if this vector does
     *         not support the requested operation
     * @see #reduceLanes(VectorOperators.Associative,VectorMask)
     * @see #add(Vector)
     * @see #mul(Vector)
     * @see #min(Vector)
     * @see #max(Vector)
     * @see #and(Vector)
     * @see #or(Vector)
     * @see VectorOperators#XOR
     * @see VectorOperators#FIRST_NONZERO
     */
    public abstract short reduceLanes(VectorOperators.Associative op);

    /**
     * Returns a value accumulated from selected lanes of this vector,
     * controlled by a mask.
     *
     * This is an associative cross-lane reduction operation which
     * applies the specified operation to the selected lane elements.
     * <p>
     * If no elements are selected, an operation-specific identity
     * value is returned.
     * <ul>
     * <li>
     * If the operation is
     *  {@code ADD}, {@code XOR}, {@code OR},
     * or {@code FIRST_NONZERO},
     * then the identity value is zero, the default {@code short} value.
     * <li>
     * If the operation is {@code MUL},
     * then the identity value is one.
     * <li>
     * If the operation is {@code AND},
     * then the identity value is minus one (all bits set).
     * <li>
     * If the operation is {@code MAX},
     * then the identity value is {@code Short.MIN_VALUE}.
     * <li>
     * If the operation is {@code MIN},
     * then the identity value is {@code Short.MAX_VALUE}.
     * </ul>
     * <p>
     * A few reduction operations do not support arbitrary reordering
     * of their operands, yet are included here because of their
     * usefulness.
     * <ul>
     * <li>
     * In the case of {@code FIRST_NONZERO}, the reduction returns
     * the value from the lowest-numbered non-zero lane.
     * <li>
     * All other reduction operations are fully commutative and
     * associative.  The implementation can choose any order of
     * processing, yet it will always produce the same result.
     * </ul>
     *
     * @param op the operation used to combine lane values
     * @param m the mask controlling lane selection
     * @return the reduced result accumulated from the selected lane values
     * @throws UnsupportedOperationException if this vector does
     *         not support the requested operation
     * @see #reduceLanes(VectorOperators.Associative)
     */
    public abstract short reduceLanes(VectorOperators.Associative op,
                                       VectorMask<Short> m);

    /*package-private*/
    @ForceInline
    final
    short reduceLanesTemplate(VectorOperators.Associative op,
                               VectorMask<Short> m) {
        ShortVector v = reduceIdentityVector(op).blend(this, m);
        return v.reduceLanesTemplate(op);
    }

    /*package-private*/
    @ForceInline
    final
    short reduceLanesTemplate(VectorOperators.Associative op) {
        if (op == FIRST_NONZERO) {
            // FIXME:  The JIT should handle this, and other scan ops alos.
            VectorMask<Short> thisNZ
                = this.viewAsIntegralLanes().compare(NE, (short) 0);
            return this.lane(thisNZ.firstTrue());
        }
        int opc = opCode(op);
        return fromBits(VectorSupport.reductionCoerced(
            opc, getClass(), short.class, length(),
            this,
            REDUCE_IMPL.find(op, opc, (opc_) -> {
              switch (opc_) {
              case VECTOR_OP_ADD: return v ->
                      toBits(v.rOp((short)0, (i, a, b) -> (short)(a + b)));
              case VECTOR_OP_MUL: return v ->
                      toBits(v.rOp((short)1, (i, a, b) -> (short)(a * b)));
              case VECTOR_OP_MIN: return v ->
                      toBits(v.rOp(MAX_OR_INF, (i, a, b) -> (short) Math.min(a, b)));
              case VECTOR_OP_MAX: return v ->
                      toBits(v.rOp(MIN_OR_INF, (i, a, b) -> (short) Math.max(a, b)));
              case VECTOR_OP_AND: return v ->
                      toBits(v.rOp((short)-1, (i, a, b) -> (short)(a & b)));
              case VECTOR_OP_OR: return v ->
                      toBits(v.rOp((short)0, (i, a, b) -> (short)(a | b)));
              case VECTOR_OP_XOR: return v ->
                      toBits(v.rOp((short)0, (i, a, b) -> (short)(a ^ b)));
              default: return null;
              }})));
    }
    private static final
    ImplCache<Associative,Function<ShortVector,Long>> REDUCE_IMPL
        = new ImplCache<>(Associative.class, ShortVector.class);

    private
    @ForceInline
    ShortVector reduceIdentityVector(VectorOperators.Associative op) {
        int opc = opCode(op);
        UnaryOperator<ShortVector> fn
            = REDUCE_ID_IMPL.find(op, opc, (opc_) -> {
                switch (opc_) {
                case VECTOR_OP_ADD:
                case VECTOR_OP_OR:
                case VECTOR_OP_XOR:
                    return v -> v.broadcast(0);
                case VECTOR_OP_MUL:
                    return v -> v.broadcast(1);
                case VECTOR_OP_AND:
                    return v -> v.broadcast(-1);
                case VECTOR_OP_MIN:
                    return v -> v.broadcast(MAX_OR_INF);
                case VECTOR_OP_MAX:
                    return v -> v.broadcast(MIN_OR_INF);
                default: return null;
                }
            });
        return fn.apply(this);
    }
    private static final
    ImplCache<Associative,UnaryOperator<ShortVector>> REDUCE_ID_IMPL
        = new ImplCache<>(Associative.class, ShortVector.class);

    private static final short MIN_OR_INF = Short.MIN_VALUE;
    private static final short MAX_OR_INF = Short.MAX_VALUE;

    public @Override abstract long reduceLanesToLong(VectorOperators.Associative op);
    public @Override abstract long reduceLanesToLong(VectorOperators.Associative op,
                                                     VectorMask<Short> m);

    // Type specific accessors

    /**
     * Gets the lane element at lane index {@code i}
     *
     * @param i the lane index
     * @return the lane element at lane index {@code i}
     * @throws IllegalArgumentException if the index is is out of range
     * ({@code < 0 || >= length()})
     */
    public abstract short lane(int i);

    /**
     * Replaces the lane element of this vector at lane index {@code i} with
     * value {@code e}.
     *
     * This is a cross-lane operation and behaves as if it returns the result
     * of blending this vector with an input vector that is the result of
     * broadcasting {@code e} and a mask that has only one lane set at lane
     * index {@code i}.
     *
     * @param i the lane index of the lane element to be replaced
     * @param e the value to be placed
     * @return the result of replacing the lane element of this vector at lane
     * index {@code i} with value {@code e}.
     * @throws IllegalArgumentException if the index is is out of range
     * ({@code < 0 || >= length()})
     */
    public abstract ShortVector withLane(int i, short e);

    // Memory load operations

    /**
     * Returns an array of type {@code short[]}
     * containing all the lane values.
     * The array length is the same as the vector length.
     * The array elements are stored in lane order.
     * <p>
     * This method behaves as if it stores
     * this vector into an allocated array
     * (using {@link #intoArray(short[], int) intoArray})
     * and returns the array as follows:
     * <pre>{@code
     *   short[] a = new short[this.length()];
     *   this.intoArray(a, 0);
     *   return a;
     * }</pre>
     *
     * @return an array containing the lane values of this vector
     */
    @ForceInline
    @Override
    public final short[] toArray() {
        short[] a = new short[vspecies().laneCount()];
        intoArray(a, 0);
        return a;
    }

    /** {@inheritDoc} <!--workaround-->
     * @implNote
     * When this method is used on used on vectors
     * of type {@code ShortVector},
     * there will be no loss of precision or range,
     * and so no {@code UnsupportedOperationException} will
     * be thrown.
     */
    @ForceInline
    @Override
    public final int[] toIntArray() {
        short[] a = toArray();
        int[] res = new int[a.length];
        for (int i = 0; i < a.length; i++) {
            short e = a[i];
            res[i] = (int) ShortSpecies.toIntegralChecked(e, true);
        }
        return res;
    }

    /** {@inheritDoc} <!--workaround-->
     * @implNote
     * When this method is used on used on vectors
     * of type {@code ShortVector},
     * there will be no loss of precision or range,
     * and so no {@code UnsupportedOperationException} will
     * be thrown.
     */
    @ForceInline
    @Override
    public final long[] toLongArray() {
        short[] a = toArray();
        long[] res = new long[a.length];
        for (int i = 0; i < a.length; i++) {
            short e = a[i];
            res[i] = ShortSpecies.toIntegralChecked(e, false);
        }
        return res;
    }

    /** {@inheritDoc} <!--workaround-->
     * @implNote
     * When this method is used on used on vectors
     * of type {@code ShortVector},
     * there will be no loss of precision.
     */
    @ForceInline
    @Override
    public final double[] toDoubleArray() {
        short[] a = toArray();
        double[] res = new double[a.length];
        for (int i = 0; i < a.length; i++) {
            res[i] = (double) a[i];
        }
        return res;
    }

    /**
     * Loads a vector from a byte array starting at an offset.
     * Bytes are composed into primitive lane elements according
     * to the specified byte order.
     * The vector is arranged into lanes according to
     * <a href="Vector.html#lane-order">memory ordering</a>.
     * <p>
     * This method behaves as if it returns the result of calling
     * {@link #fromByteBuffer(VectorSpecies,ByteBuffer,int,ByteOrder,VectorMask)
     * fromByteBuffer()} as follows:
     * <pre>{@code
     * var bb = ByteBuffer.wrap(a);
     * var m = species.maskAll(true);
     * return fromByteBuffer(species, bb, offset, bo, m);
     * }</pre>
     *
     * @param species species of desired vector
     * @param a the byte array
     * @param offset the offset into the array
     * @param bo the intended byte order
     * @return a vector loaded from a byte array
     * @throws IndexOutOfBoundsException
     *         if {@code offset+N*ESIZE < 0}
     *         or {@code offset+(N+1)*ESIZE > a.length}
     *         for any lane {@code N} in the vector
     */
    @ForceInline
    public static
    ShortVector fromByteArray(VectorSpecies<Short> species,
                                       byte[] a, int offset,
                                       ByteOrder bo) {
        offset = checkFromIndexSize(offset, species.vectorByteSize(), a.length);
        ShortSpecies vsp = (ShortSpecies) species;
        return vsp.dummyVector().fromByteArray0(a, offset).maybeSwap(bo);
    }

    /**
     * Loads a vector from a byte array starting at an offset
     * and using a mask.
     * Lanes where the mask is unset are filled with the default
     * value of {@code short} (zero).
     * Bytes are composed into primitive lane elements according
     * to the specified byte order.
     * The vector is arranged into lanes according to
     * <a href="Vector.html#lane-order">memory ordering</a>.
     * <p>
     * This method behaves as if it returns the result of calling
     * {@link #fromByteBuffer(VectorSpecies,ByteBuffer,int,ByteOrder,VectorMask)
     * fromByteBuffer()} as follows:
     * <pre>{@code
     * var bb = ByteBuffer.wrap(a);
     * return fromByteBuffer(species, bb, offset, bo, m);
     * }</pre>
     *
     * @param species species of desired vector
     * @param a the byte array
     * @param offset the offset into the array
     * @param bo the intended byte order
     * @param m the mask controlling lane selection
     * @return a vector loaded from a byte array
     * @throws IndexOutOfBoundsException
     *         if {@code offset+N*ESIZE < 0}
     *         or {@code offset+(N+1)*ESIZE > a.length}
     *         for any lane {@code N} in the vector
     *         where the mask is set
     */
    @ForceInline
    public static
    ShortVector fromByteArray(VectorSpecies<Short> species,
                                       byte[] a, int offset,
                                       ByteOrder bo,
                                       VectorMask<Short> m) {
        ShortSpecies vsp = (ShortSpecies) species;
        if (offset >= 0 && offset <= (a.length - species.vectorByteSize())) {
            ShortVector zero = vsp.zero();
            ShortVector v = zero.fromByteArray0(a, offset);
            return zero.blend(v.maybeSwap(bo), m);
        }

        // FIXME: optimize
        checkMaskFromIndexSize(offset, vsp, m, 2, a.length);
        ByteBuffer wb = wrapper(a, bo);
        return vsp.ldOp(wb, offset, (AbstractMask<Short>)m,
                   (wb_, o, i)  -> wb_.getShort(o + i * 2));
    }

    /**
     * Loads a vector from an array of type {@code short[]}
     * starting at an offset.
     * For each vector lane, where {@code N} is the vector lane index, the
     * array element at index {@code offset + N} is placed into the
     * resulting vector at lane index {@code N}.
     *
     * @param species species of desired vector
     * @param a the array
     * @param offset the offset into the array
     * @return the vector loaded from an array
     * @throws IndexOutOfBoundsException
     *         if {@code offset+N < 0} or {@code offset+N >= a.length}
     *         for any lane {@code N} in the vector
     */
    @ForceInline
    public static
    ShortVector fromArray(VectorSpecies<Short> species,
                                   short[] a, int offset) {
        offset = checkFromIndexSize(offset, species.length(), a.length);
        ShortSpecies vsp = (ShortSpecies) species;
        return vsp.dummyVector().fromArray0(a, offset);
    }

    /**
     * Loads a vector from an array of type {@code short[]}
     * starting at an offset and using a mask.
     * Lanes where the mask is unset are filled with the default
     * value of {@code short} (zero).
     * For each vector lane, where {@code N} is the vector lane index,
     * if the mask lane at index {@code N} is set then the array element at
     * index {@code offset + N} is placed into the resulting vector at lane index
     * {@code N}, otherwise the default element value is placed into the
     * resulting vector at lane index {@code N}.
     *
     * @param species species of desired vector
     * @param a the array
     * @param offset the offset into the array
     * @param m the mask controlling lane selection
     * @return the vector loaded from an array
     * @throws IndexOutOfBoundsException
     *         if {@code offset+N < 0} or {@code offset+N >= a.length}
     *         for any lane {@code N} in the vector
     *         where the mask is set
     */
    @ForceInline
    public static
    ShortVector fromArray(VectorSpecies<Short> species,
                                   short[] a, int offset,
                                   VectorMask<Short> m) {
        ShortSpecies vsp = (ShortSpecies) species;
        if (offset >= 0 && offset <= (a.length - species.length())) {
            ShortVector zero = vsp.zero();
            return zero.blend(zero.fromArray0(a, offset), m);
        }

        // FIXME: optimize
        checkMaskFromIndexSize(offset, vsp, m, 1, a.length);
        return vsp.vOp(m, i -> a[offset + i]);
    }

    /**
     * Gathers a new vector composed of elements from an array of type
     * {@code short[]},
     * using indexes obtained by adding a fixed {@code offset} to a
     * series of secondary offsets from an <em>index map</em>.
     * The index map is a contiguous sequence of {@code VLENGTH}
     * elements in a second array of {@code int}s, starting at a given
     * {@code mapOffset}.
     * <p>
     * For each vector lane, where {@code N} is the vector lane index,
     * the lane is loaded from the array
     * element {@code a[f(N)]}, where {@code f(N)} is the
     * index mapping expression
     * {@code offset + indexMap[mapOffset + N]]}.
     *
     * @param species species of desired vector
     * @param a the array
     * @param offset the offset into the array, may be negative if relative
     * indexes in the index map compensate to produce a value within the
     * array bounds
     * @param indexMap the index map
     * @param mapOffset the offset into the index map
     * @return the vector loaded from the indexed elements of the array
     * @throws IndexOutOfBoundsException
     *         if {@code mapOffset+N < 0}
     *         or if {@code mapOffset+N >= indexMap.length},
     *         or if {@code f(N)=offset+indexMap[mapOffset+N]}
     *         is an invalid index into {@code a},
     *         for any lane {@code N} in the vector
     * @see ShortVector#toIntArray()
     */
    @ForceInline
    public static
    ShortVector fromArray(VectorSpecies<Short> species,
                                   short[] a, int offset,
                                   int[] indexMap, int mapOffset) {
        ShortSpecies vsp = (ShortSpecies) species;
        return vsp.vOp(n -> a[offset + indexMap[mapOffset + n]]);
    }

    /**
     * Gathers a new vector composed of elements from an array of type
     * {@code short[]},
     * under the control of a mask, and
     * using indexes obtained by adding a fixed {@code offset} to a
     * series of secondary offsets from an <em>index map</em>.
     * The index map is a contiguous sequence of {@code VLENGTH}
     * elements in a second array of {@code int}s, starting at a given
     * {@code mapOffset}.
     * <p>
     * For each vector lane, where {@code N} is the vector lane index,
     * if the lane is set in the mask,
     * the lane is loaded from the array
     * element {@code a[f(N)]}, where {@code f(N)} is the
     * index mapping expression
     * {@code offset + indexMap[mapOffset + N]]}.
     * Unset lanes in the resulting vector are set to zero.
     *
     * @param species species of desired vector
     * @param a the array
     * @param offset the offset into the array, may be negative if relative
     * indexes in the index map compensate to produce a value within the
     * array bounds
     * @param indexMap the index map
     * @param mapOffset the offset into the index map
     * @param m the mask controlling lane selection
     * @return the vector loaded from the indexed elements of the array
     * @throws IndexOutOfBoundsException
     *         if {@code mapOffset+N < 0}
     *         or if {@code mapOffset+N >= indexMap.length},
     *         or if {@code f(N)=offset+indexMap[mapOffset+N]}
     *         is an invalid index into {@code a},
     *         for any lane {@code N} in the vector
     *         where the mask is set
     * @see ShortVector#toIntArray()
     */
    @ForceInline
    public static
    ShortVector fromArray(VectorSpecies<Short> species,
                                   short[] a, int offset,
                                   int[] indexMap, int mapOffset,
                                   VectorMask<Short> m) {
        ShortSpecies vsp = (ShortSpecies) species;
        return vsp.vOp(m, n -> a[offset + indexMap[mapOffset + n]]);
    }

    /**
     * Loads a vector from an array of type {@code char[]}
     * starting at an offset.
     * For each vector lane, where {@code N} is the vector lane index, the
     * array element at index {@code offset + N}
     * is first cast to a {@code short} value and then
     * placed into the resulting vector at lane index {@code N}.
     *
     * @param species species of desired vector
     * @param a the array
     * @param offset the offset into the array
     * @return the vector loaded from an array
     * @throws IndexOutOfBoundsException
     *         if {@code offset+N < 0} or {@code offset+N >= a.length}
     *         for any lane {@code N} in the vector
     */
    @ForceInline
    public static
    ShortVector fromCharArray(VectorSpecies<Short> species,
                                       char[] a, int offset) {
        offset = checkFromIndexSize(offset, species.length(), a.length);
        ShortSpecies vsp = (ShortSpecies) species;
        return vsp.dummyVector().fromCharArray0(a, offset);
    }

    /**
     * Loads a vector from an array of type {@code char[]}
     * starting at an offset and using a mask.
     * Lanes where the mask is unset are filled with the default
     * value of {@code short} (zero).
     * For each vector lane, where {@code N} is the vector lane index,
     * if the mask lane at index {@code N} is set then the array element at
     * index {@code offset + N}
     * is first cast to a {@code short} value and then
     * placed into the resulting vector at lane index
     * {@code N}, otherwise the default element value is placed into the
     * resulting vector at lane index {@code N}.
     *
     * @param species species of desired vector
     * @param a the array
     * @param offset the offset into the array
     * @param m the mask controlling lane selection
     * @return the vector loaded from an array
     * @throws IndexOutOfBoundsException
     *         if {@code offset+N < 0} or {@code offset+N >= a.length}
     *         for any lane {@code N} in the vector
     *         where the mask is set
     */
    @ForceInline
    public static
    ShortVector fromCharArray(VectorSpecies<Short> species,
                                       char[] a, int offset,
                                       VectorMask<Short> m) {
        ShortSpecies vsp = (ShortSpecies) species;
        if (offset >= 0 && offset <= (a.length - species.length())) {
            ShortVector zero = vsp.zero();
            return zero.blend(zero.fromCharArray0(a, offset), m);
        }

        // FIXME: optimize
        checkMaskFromIndexSize(offset, vsp, m, 1, a.length);
        return vsp.vOp(m, i -> (short) a[offset + i]);
    }

    /**
     * Gathers a new vector composed of elements from an array of type
     * {@code char[]},
     * using indexes obtained by adding a fixed {@code offset} to a
     * series of secondary offsets from an <em>index map</em>.
     * The index map is a contiguous sequence of {@code VLENGTH}
     * elements in a second array of {@code int}s, starting at a given
     * {@code mapOffset}.
     * <p>
     * For each vector lane, where {@code N} is the vector lane index,
     * the lane is loaded from the expression
     * {@code (short) a[f(N)]}, where {@code f(N)} is the
     * index mapping expression
     * {@code offset + indexMap[mapOffset + N]]}.
     *
     * @param species species of desired vector
     * @param a the array
     * @param offset the offset into the array, may be negative if relative
     * indexes in the index map compensate to produce a value within the
     * array bounds
     * @param indexMap the index map
     * @param mapOffset the offset into the index map
     * @return the vector loaded from the indexed elements of the array
     * @throws IndexOutOfBoundsException
     *         if {@code mapOffset+N < 0}
     *         or if {@code mapOffset+N >= indexMap.length},
     *         or if {@code f(N)=offset+indexMap[mapOffset+N]}
     *         is an invalid index into {@code a},
     *         for any lane {@code N} in the vector
     * @see ShortVector#toIntArray()
     */
    @ForceInline
    public static
    ShortVector fromCharArray(VectorSpecies<Short> species,
                                       char[] a, int offset,
                                       int[] indexMap, int mapOffset) {
        // FIXME: optimize
        ShortSpecies vsp = (ShortSpecies) species;
        return vsp.vOp(n -> (short) a[offset + indexMap[mapOffset + n]]);
    }

    /**
     * Gathers a new vector composed of elements from an array of type
     * {@code char[]},
     * under the control of a mask, and
     * using indexes obtained by adding a fixed {@code offset} to a
     * series of secondary offsets from an <em>index map</em>.
     * The index map is a contiguous sequence of {@code VLENGTH}
     * elements in a second array of {@code int}s, starting at a given
     * {@code mapOffset}.
     * <p>
     * For each vector lane, where {@code N} is the vector lane index,
     * if the lane is set in the mask,
     * the lane is loaded from the expression
     * {@code (short) a[f(N)]}, where {@code f(N)} is the
     * index mapping expression
     * {@code offset + indexMap[mapOffset + N]]}.
     * Unset lanes in the resulting vector are set to zero.
     *
     * @param species species of desired vector
     * @param a the array
     * @param offset the offset into the array, may be negative if relative
     * indexes in the index map compensate to produce a value within the
     * array bounds
     * @param indexMap the index map
     * @param mapOffset the offset into the index map
     * @param m the mask controlling lane selection
     * @return the vector loaded from the indexed elements of the array
     * @throws IndexOutOfBoundsException
     *         if {@code mapOffset+N < 0}
     *         or if {@code mapOffset+N >= indexMap.length},
     *         or if {@code f(N)=offset+indexMap[mapOffset+N]}
     *         is an invalid index into {@code a},
     *         for any lane {@code N} in the vector
     *         where the mask is set
     * @see ShortVector#toIntArray()
     */
    @ForceInline
    public static
    ShortVector fromCharArray(VectorSpecies<Short> species,
                                       char[] a, int offset,
                                       int[] indexMap, int mapOffset,
                                       VectorMask<Short> m) {
        // FIXME: optimize
        ShortSpecies vsp = (ShortSpecies) species;
        return vsp.vOp(m, n -> (short) a[offset + indexMap[mapOffset + n]]);
    }


    /**
     * Loads a vector from a {@linkplain ByteBuffer byte buffer}
     * starting at an offset into the byte buffer.
     * Bytes are composed into primitive lane elements according
     * to the specified byte order.
     * The vector is arranged into lanes according to
     * <a href="Vector.html#lane-order">memory ordering</a>.
     * <p>
     * This method behaves as if it returns the result of calling
     * {@link #fromByteBuffer(VectorSpecies,ByteBuffer,int,ByteOrder,VectorMask)
     * fromByteBuffer()} as follows:
     * <pre>{@code
     * var m = species.maskAll(true);
     * return fromByteBuffer(species, bb, offset, bo, m);
     * }</pre>
     *
     * @param species species of desired vector
     * @param bb the byte buffer
     * @param offset the offset into the byte buffer
     * @param bo the intended byte order
     * @return a vector loaded from a byte buffer
     * @throws IndexOutOfBoundsException
     *         if {@code offset+N*2 < 0}
     *         or {@code offset+N*2 >= bb.limit()}
     *         for any lane {@code N} in the vector
     */
    @ForceInline
    public static
    ShortVector fromByteBuffer(VectorSpecies<Short> species,
                                        ByteBuffer bb, int offset,
                                        ByteOrder bo) {
        offset = checkFromIndexSize(offset, species.vectorByteSize(), bb.limit());
        ShortSpecies vsp = (ShortSpecies) species;
        return vsp.dummyVector().fromByteBuffer0(bb, offset).maybeSwap(bo);
    }

    /**
     * Loads a vector from a {@linkplain ByteBuffer byte buffer}
     * starting at an offset into the byte buffer
     * and using a mask.
     * Lanes where the mask is unset are filled with the default
     * value of {@code short} (zero).
     * Bytes are composed into primitive lane elements according
     * to the specified byte order.
     * The vector is arranged into lanes according to
     * <a href="Vector.html#lane-order">memory ordering</a>.
     * <p>
     * The following pseudocode illustrates the behavior:
     * <pre>{@code
     * ShortBuffer eb = bb.duplicate()
     *     .position(offset)
     *     .order(bo).asShortBuffer();
     * short[] ar = new short[species.length()];
     * for (int n = 0; n < ar.length; n++) {
     *     if (m.laneIsSet(n)) {
     *         ar[n] = eb.get(n);
     *     }
     * }
     * ShortVector r = ShortVector.fromArray(species, ar, 0);
     * }</pre>
     * @implNote
     * This operation is likely to be more efficient if
     * the specified byte order is the same as
     * {@linkplain ByteOrder#nativeOrder()
     * the platform native order},
     * since this method will not need to reorder
     * the bytes of lane values.
     *
     * @param species species of desired vector
     * @param bb the byte buffer
     * @param offset the offset into the byte buffer
     * @param bo the intended byte order
     * @param m the mask controlling lane selection
     * @return a vector loaded from a byte buffer
     * @throws IndexOutOfBoundsException
     *         if {@code offset+N*2 < 0}
     *         or {@code offset+N*2 >= bb.limit()}
     *         for any lane {@code N} in the vector
     *         where the mask is set
     */
    @ForceInline
    public static
    ShortVector fromByteBuffer(VectorSpecies<Short> species,
                                        ByteBuffer bb, int offset,
                                        ByteOrder bo,
                                        VectorMask<Short> m) {
        ShortSpecies vsp = (ShortSpecies) species;
        if (offset >= 0 && offset <= (bb.limit() - species.vectorByteSize())) {
            ShortVector zero = vsp.zero();
            ShortVector v = zero.fromByteBuffer0(bb, offset);
            return zero.blend(v.maybeSwap(bo), m);
        }

        // FIXME: optimize
        checkMaskFromIndexSize(offset, vsp, m, 2, bb.limit());
        ByteBuffer wb = wrapper(bb, bo);
        return vsp.ldOp(wb, offset, (AbstractMask<Short>)m,
                   (wb_, o, i)  -> wb_.getShort(o + i * 2));
    }

    // Memory store operations

    /**
     * Stores this vector into an array of type {@code short[]}
     * starting at an offset.
     * <p>
     * For each vector lane, where {@code N} is the vector lane index,
     * the lane element at index {@code N} is stored into the array
     * element {@code a[offset+N]}.
     *
     * @param a the array, of type {@code short[]}
     * @param offset the offset into the array
     * @throws IndexOutOfBoundsException
     *         if {@code offset+N < 0} or {@code offset+N >= a.length}
     *         for any lane {@code N} in the vector
     */
    @ForceInline
    public final
    void intoArray(short[] a, int offset) {
        offset = checkFromIndexSize(offset, length(), a.length);
        ShortSpecies vsp = vspecies();
        VectorSupport.store(
            vsp.vectorType(), vsp.elementType(), vsp.laneCount(),
            a, arrayAddress(a, offset),
            this,
            a, offset,
            (arr, off, v)
            -> v.stOp(arr, off,
                      (arr_, off_, i, e) -> arr_[off_ + i] = e));
    }

    /**
     * Stores this vector into an array of type {@code short[]}
     * starting at offset and using a mask.
     * <p>
     * For each vector lane, where {@code N} is the vector lane index,
     * the lane element at index {@code N} is stored into the array
     * element {@code a[offset+N]}.
     * If the mask lane at {@code N} is unset then the corresponding
     * array element {@code a[offset+N]} is left unchanged.
     * <p>
     * Array range checking is done for lanes where the mask is set.
     * Lanes where the mask is unset are not stored and do not need
     * to correspond to legitimate elements of {@code a}.
     * That is, unset lanes may correspond to array indexes less than
     * zero or beyond the end of the array.
     *
     * @param a the array, of type {@code short[]}
     * @param offset the offset into the array
     * @param m the mask controlling lane storage
     * @throws IndexOutOfBoundsException
     *         if {@code offset+N < 0} or {@code offset+N >= a.length}
     *         for any lane {@code N} in the vector
     *         where the mask is set
     */
    @ForceInline
    public final
    void intoArray(short[] a, int offset,
                   VectorMask<Short> m) {
        if (m.allTrue()) {
            intoArray(a, offset);
        } else {
            // FIXME: optimize
            ShortSpecies vsp = vspecies();
            checkMaskFromIndexSize(offset, vsp, m, 1, a.length);
            stOp(a, offset, m, (arr, off, i, v) -> arr[off+i] = v);
        }
    }

    /**
     * Scatters this vector into an array of type {@code short[]}
     * using indexes obtained by adding a fixed {@code offset} to a
     * series of secondary offsets from an <em>index map</em>.
     * The index map is a contiguous sequence of {@code VLENGTH}
     * elements in a second array of {@code int}s, starting at a given
     * {@code mapOffset}.
     * <p>
     * For each vector lane, where {@code N} is the vector lane index,
     * the lane element at index {@code N} is stored into the array
     * element {@code a[f(N)]}, where {@code f(N)} is the
     * index mapping expression
     * {@code offset + indexMap[mapOffset + N]]}.
     *
     * @param a the array
     * @param offset an offset to combine with the index map offsets
     * @param indexMap the index map
     * @param mapOffset the offset into the index map
     * @throws IndexOutOfBoundsException
     *         if {@code mapOffset+N < 0}
     *         or if {@code mapOffset+N >= indexMap.length},
     *         or if {@code f(N)=offset+indexMap[mapOffset+N]}
     *         is an invalid index into {@code a},
     *         for any lane {@code N} in the vector
     * @see ShortVector#toIntArray()
     */
    @ForceInline
    public final
    void intoArray(short[] a, int offset,
                   int[] indexMap, int mapOffset) {
        stOp(a, offset,
             (arr, off, i, e) -> {
                 int j = indexMap[mapOffset + i];
                 arr[off + j] = e;
             });
    }

    /**
     * Scatters this vector into an array of type {@code short[]},
     * under the control of a mask, and
     * using indexes obtained by adding a fixed {@code offset} to a
     * series of secondary offsets from an <em>index map</em>.
     * The index map is a contiguous sequence of {@code VLENGTH}
     * elements in a second array of {@code int}s, starting at a given
     * {@code mapOffset}.
     * <p>
     * For each vector lane, where {@code N} is the vector lane index,
     * if the mask lane at index {@code N} is set then
     * the lane element at index {@code N} is stored into the array
     * element {@code a[f(N)]}, where {@code f(N)} is the
     * index mapping expression
     * {@code offset + indexMap[mapOffset + N]]}.
     *
     * @param a the array
     * @param offset an offset to combine with the index map offsets
     * @param indexMap the index map
     * @param mapOffset the offset into the index map
     * @param m the mask
     * @throws IndexOutOfBoundsException
     *         if {@code mapOffset+N < 0}
     *         or if {@code mapOffset+N >= indexMap.length},
     *         or if {@code f(N)=offset+indexMap[mapOffset+N]}
     *         is an invalid index into {@code a},
     *         for any lane {@code N} in the vector
     *         where the mask is set
     * @see ShortVector#toIntArray()
     */
    @ForceInline
    public final
    void intoArray(short[] a, int offset,
                   int[] indexMap, int mapOffset,
                   VectorMask<Short> m) {
        stOp(a, offset, m,
             (arr, off, i, e) -> {
                 int j = indexMap[mapOffset + i];
                 arr[off + j] = e;
             });
    }

    /**
     * Stores this vector into an array of type {@code char[]}
     * starting at an offset.
     * <p>
     * For each vector lane, where {@code N} is the vector lane index,
     * the lane element at index {@code N}
     * is first cast to a {@code char} value and then
     * stored into the array element {@code a[offset+N]}.
     *
     * @param a the array, of type {@code char[]}
     * @param offset the offset into the array
     * @throws IndexOutOfBoundsException
     *         if {@code offset+N < 0} or {@code offset+N >= a.length}
     *         for any lane {@code N} in the vector
     */
    @ForceInline
    public final
    void intoCharArray(char[] a, int offset) {
        offset = checkFromIndexSize(offset, length(), a.length);
        ShortSpecies vsp = vspecies();
        VectorSupport.store(
            vsp.vectorType(), vsp.elementType(), vsp.laneCount(),
            a, charArrayAddress(a, offset),
            this,
            a, offset,
            (arr, off, v)
            -> v.stOp(arr, off,
                      (arr_, off_, i, e) -> arr_[off_ + i] = (char) e));
    }

    /**
     * Stores this vector into an array of type {@code char[]}
     * starting at offset and using a mask.
     * <p>
     * For each vector lane, where {@code N} is the vector lane index,
     * the lane element at index {@code N}
     * is first cast to a {@code char} value and then
     * stored into the array element {@code a[offset+N]}.
     * If the mask lane at {@code N} is unset then the corresponding
     * array element {@code a[offset+N]} is left unchanged.
     * <p>
     * Array range checking is done for lanes where the mask is set.
     * Lanes where the mask is unset are not stored and do not need
     * to correspond to legitimate elements of {@code a}.
     * That is, unset lanes may correspond to array indexes less than
     * zero or beyond the end of the array.
     *
     * @param a the array, of type {@code char[]}
     * @param offset the offset into the array
     * @param m the mask controlling lane storage
     * @throws IndexOutOfBoundsException
     *         if {@code offset+N < 0} or {@code offset+N >= a.length}
     *         for any lane {@code N} in the vector
     *         where the mask is set
     */
    @ForceInline
    public final
    void intoCharArray(char[] a, int offset,
                       VectorMask<Short> m) {
        if (m.allTrue()) {
            intoCharArray(a, offset);
        } else {
            // FIXME: optimize
            ShortSpecies vsp = vspecies();
            checkMaskFromIndexSize(offset, vsp, m, 1, a.length);
            stOp(a, offset, m, (arr, off, i, v) -> arr[off+i] = (char) v);
        }
    }

    /**
     * Scatters this vector into an array of type {@code char[]}
     * using indexes obtained by adding a fixed {@code offset} to a
     * series of secondary offsets from an <em>index map</em>.
     * The index map is a contiguous sequence of {@code VLENGTH}
     * elements in a second array of {@code int}s, starting at a given
     * {@code mapOffset}.
     * <p>
     * For each vector lane, where {@code N} is the vector lane index,
     * the lane element at index {@code N}
     * is first cast to a {@code char} value and then
     * stored into the array
     * element {@code a[f(N)]}, where {@code f(N)} is the
     * index mapping expression
     * {@code offset + indexMap[mapOffset + N]]}.
     *
     * @param a the array
     * @param offset an offset to combine with the index map offsets
     * @param indexMap the index map
     * @param mapOffset the offset into the index map
     * @throws IndexOutOfBoundsException
     *         if {@code mapOffset+N < 0}
     *         or if {@code mapOffset+N >= indexMap.length},
     *         or if {@code f(N)=offset+indexMap[mapOffset+N]}
     *         is an invalid index into {@code a},
     *         for any lane {@code N} in the vector
     * @see ShortVector#toIntArray()
     */
    @ForceInline
    public final
    void intoCharArray(char[] a, int offset,
                       int[] indexMap, int mapOffset) {
        // FIXME: optimize
        stOp(a, offset,
             (arr, off, i, e) -> {
                 int j = indexMap[mapOffset + i];
                 arr[off + j] = (char) e;
             });
    }

    /**
     * Scatters this vector into an array of type {@code char[]},
     * under the control of a mask, and
     * using indexes obtained by adding a fixed {@code offset} to a
     * series of secondary offsets from an <em>index map</em>.
     * The index map is a contiguous sequence of {@code VLENGTH}
     * elements in a second array of {@code int}s, starting at a given
     * {@code mapOffset}.
     * <p>
     * For each vector lane, where {@code N} is the vector lane index,
     * if the mask lane at index {@code N} is set then
     * the lane element at index {@code N}
     * is first cast to a {@code char} value and then
     * stored into the array
     * element {@code a[f(N)]}, where {@code f(N)} is the
     * index mapping expression
     * {@code offset + indexMap[mapOffset + N]]}.
     *
     * @param a the array
     * @param offset an offset to combine with the index map offsets
     * @param indexMap the index map
     * @param mapOffset the offset into the index map
     * @param m the mask
     * @throws IndexOutOfBoundsException
     *         if {@code mapOffset+N < 0}
     *         or if {@code mapOffset+N >= indexMap.length},
     *         or if {@code f(N)=offset+indexMap[mapOffset+N]}
     *         is an invalid index into {@code a},
     *         for any lane {@code N} in the vector
     *         where the mask is set
     * @see ShortVector#toIntArray()
     */
    @ForceInline
    public final
    void intoCharArray(char[] a, int offset,
                       int[] indexMap, int mapOffset,
                       VectorMask<Short> m) {
        // FIXME: optimize
        stOp(a, offset, m,
             (arr, off, i, e) -> {
                 int j = indexMap[mapOffset + i];
                 arr[off + j] = (char) e;
             });
    }


    /**
     * {@inheritDoc} <!--workaround-->
     */
    @Override
    @ForceInline
    public final
    void intoByteArray(byte[] a, int offset,
                       ByteOrder bo) {
        offset = checkFromIndexSize(offset, byteSize(), a.length);
        maybeSwap(bo).intoByteArray0(a, offset);
    }

    /**
     * {@inheritDoc} <!--workaround-->
     */
    @Override
    @ForceInline
    public final
    void intoByteArray(byte[] a, int offset,
                       ByteOrder bo,
                       VectorMask<Short> m) {
        if (m.allTrue()) {
            intoByteArray(a, offset, bo);
        } else {
            // FIXME: optimize
            ShortSpecies vsp = vspecies();
            checkMaskFromIndexSize(offset, vsp, m, 2, a.length);
            ByteBuffer wb = wrapper(a, bo);
            this.stOp(wb, offset, m,
                    (wb_, o, i, e) -> wb_.putShort(o + i * 2, e));
        }
    }

    /**
     * {@inheritDoc} <!--workaround-->
     */
    @Override
    @ForceInline
    public final
    void intoByteBuffer(ByteBuffer bb, int offset,
                        ByteOrder bo) {
        if (bb.isReadOnly()) {
            throw new ReadOnlyBufferException();
        }
        offset = checkFromIndexSize(offset, byteSize(), bb.limit());
        maybeSwap(bo).intoByteBuffer0(bb, offset);
    }

    /**
     * {@inheritDoc} <!--workaround-->
     */
    @Override
    @ForceInline
    public final
    void intoByteBuffer(ByteBuffer bb, int offset,
                        ByteOrder bo,
                        VectorMask<Short> m) {
        if (m.allTrue()) {
            intoByteBuffer(bb, offset, bo);
        } else {
            // FIXME: optimize
            if (bb.isReadOnly()) {
                throw new ReadOnlyBufferException();
            }
            ShortSpecies vsp = vspecies();
            checkMaskFromIndexSize(offset, vsp, m, 2, bb.limit());
            ByteBuffer wb = wrapper(bb, bo);
            this.stOp(wb, offset, m,
                    (wb_, o, i, e) -> wb_.putShort(o + i * 2, e));
        }
    }

    // ================================================

    // Low-level memory operations.
    //
    // Note that all of these operations *must* inline into a context
    // where the exact species of the involved vector is a
    // compile-time constant.  Otherwise, the intrinsic generation
    // will fail and performance will suffer.
    //
    // In many cases this is achieved by re-deriving a version of the
    // method in each concrete subclass (per species).  The re-derived
    // method simply calls one of these generic methods, with exact
    // parameters for the controlling metadata, which is either a
    // typed vector or constant species instance.

    // Unchecked loading operations in native byte order.
    // Caller is responsible for applying index checks, masking, and
    // byte swapping.

    /*package-private*/
    abstract
    ShortVector fromArray0(short[] a, int offset);
    @ForceInline
    final
    ShortVector fromArray0Template(short[] a, int offset) {
        ShortSpecies vsp = vspecies();
        return VectorSupport.load(
            vsp.vectorType(), vsp.elementType(), vsp.laneCount(),
            a, arrayAddress(a, offset),
            a, offset, vsp,
            (arr, off, s) -> s.ldOp(arr, off,
                                    (arr_, off_, i) -> arr_[off_ + i]));
    }

    /*package-private*/
    abstract
    ShortVector fromCharArray0(char[] a, int offset);
    @ForceInline
    final
    ShortVector fromCharArray0Template(char[] a, int offset) {
        ShortSpecies vsp = vspecies();
        return VectorSupport.load(
            vsp.vectorType(), vsp.elementType(), vsp.laneCount(),
            a, charArrayAddress(a, offset),
            a, offset, vsp,
            (arr, off, s) -> s.ldOp(arr, off,
                                    (arr_, off_, i) -> (short) arr_[off_ + i]));
    }


    @Override
    abstract
    ShortVector fromByteArray0(byte[] a, int offset);
    @ForceInline
    final
    ShortVector fromByteArray0Template(byte[] a, int offset) {
        ShortSpecies vsp = vspecies();
        return VectorSupport.load(
            vsp.vectorType(), vsp.elementType(), vsp.laneCount(),
            a, byteArrayAddress(a, offset),
            a, offset, vsp,
            (arr, off, s) -> {
                ByteBuffer wb = wrapper(arr, NATIVE_ENDIAN);
                return s.ldOp(wb, off,
                        (wb_, o, i) -> wb_.getShort(o + i * 2));
            });
    }

    abstract
    ShortVector fromByteBuffer0(ByteBuffer bb, int offset);
    @ForceInline
    final
    ShortVector fromByteBuffer0Template(ByteBuffer bb, int offset) {
        ShortSpecies vsp = vspecies();
        return ScopedMemoryAccess.loadFromByteBuffer(
                vsp.vectorType(), vsp.elementType(), vsp.laneCount(),
                bb, offset, vsp,
                (buf, off, s) -> {
                    ByteBuffer wb = wrapper(buf, NATIVE_ENDIAN);
                    return s.ldOp(wb, off,
                            (wb_, o, i) -> wb_.getShort(o + i * 2));
                });
    }

    // Unchecked storing operations in native byte order.
    // Caller is responsible for applying index checks, masking, and
    // byte swapping.

    abstract
    void intoArray0(short[] a, int offset);
    @ForceInline
    final
    void intoArray0Template(short[] a, int offset) {
        ShortSpecies vsp = vspecies();
        VectorSupport.store(
            vsp.vectorType(), vsp.elementType(), vsp.laneCount(),
            a, arrayAddress(a, offset),
            this, a, offset,
            (arr, off, v)
            -> v.stOp(arr, off,
                      (arr_, off_, i, e) -> arr_[off_+i] = e));
    }

    abstract
    void intoByteArray0(byte[] a, int offset);
    @ForceInline
    final
    void intoByteArray0Template(byte[] a, int offset) {
        ShortSpecies vsp = vspecies();
        VectorSupport.store(
            vsp.vectorType(), vsp.elementType(), vsp.laneCount(),
            a, byteArrayAddress(a, offset),
            this, a, offset,
            (arr, off, v) -> {
                ByteBuffer wb = wrapper(arr, NATIVE_ENDIAN);
                v.stOp(wb, off,
                        (tb_, o, i, e) -> tb_.putShort(o + i * 2, e));
            });
    }

    @ForceInline
    final
    void intoByteBuffer0(ByteBuffer bb, int offset) {
        ShortSpecies vsp = vspecies();
        ScopedMemoryAccess.storeIntoByteBuffer(
                vsp.vectorType(), vsp.elementType(), vsp.laneCount(),
                this, bb, offset,
                (buf, off, v) -> {
                    ByteBuffer wb = wrapper(buf, NATIVE_ENDIAN);
                    v.stOp(wb, off,
                            (wb_, o, i, e) -> wb_.putShort(o + i * 2, e));
                });
    }

    // End of low-level memory operations.

    private static
    void checkMaskFromIndexSize(int offset,
                                ShortSpecies vsp,
                                VectorMask<Short> m,
                                int scale,
                                int limit) {
        ((AbstractMask<Short>)m)
            .checkIndexByLane(offset, limit, vsp.iota(), scale);
    }

    @ForceInline
    private void conditionalStoreNYI(int offset,
                                     ShortSpecies vsp,
                                     VectorMask<Short> m,
                                     int scale,
                                     int limit) {
        if (offset < 0 || offset + vsp.laneCount() * scale > limit) {
            String msg =
                String.format("unimplemented: store @%d in [0..%d), %s in %s",
                              offset, limit, m, vsp);
            throw new AssertionError(msg);
        }
    }

    /*package-private*/
    @Override
    @ForceInline
    final
    ShortVector maybeSwap(ByteOrder bo) {
        if (bo != NATIVE_ENDIAN) {
            return this.reinterpretAsBytes()
                .rearrange(swapBytesShuffle())
                .reinterpretAsShorts();
        }
        return this;
    }

    static final int ARRAY_SHIFT =
        31 - Integer.numberOfLeadingZeros(Unsafe.ARRAY_SHORT_INDEX_SCALE);
    static final long ARRAY_BASE =
        Unsafe.ARRAY_SHORT_BASE_OFFSET;

    @ForceInline
    static long arrayAddress(short[] a, int index) {
        return ARRAY_BASE + (((long)index) << ARRAY_SHIFT);
    }

    static final int ARRAY_CHAR_SHIFT =
            31 - Integer.numberOfLeadingZeros(Unsafe.ARRAY_CHAR_INDEX_SCALE);
    static final long ARRAY_CHAR_BASE =
            Unsafe.ARRAY_CHAR_BASE_OFFSET;

    @ForceInline
    static long charArrayAddress(char[] a, int index) {
        return ARRAY_CHAR_BASE + (((long)index) << ARRAY_CHAR_SHIFT);
    }


    @ForceInline
    static long byteArrayAddress(byte[] a, int index) {
        return Unsafe.ARRAY_BYTE_BASE_OFFSET + index;
    }

    // ================================================

    /// Reinterpreting view methods:
    //   lanewise reinterpret: viewAsXVector()
    //   keep shape, redraw lanes: reinterpretAsEs()

    /**
     * {@inheritDoc} <!--workaround-->
     */
    @ForceInline
    @Override
    public final ByteVector reinterpretAsBytes() {
         // Going to ByteVector, pay close attention to byte order.
         assert(REGISTER_ENDIAN == ByteOrder.LITTLE_ENDIAN);
         return asByteVectorRaw();
         //return asByteVectorRaw().rearrange(swapBytesShuffle());
    }

    /**
     * {@inheritDoc} <!--workaround-->
     */
    @ForceInline
    @Override
    public final ShortVector viewAsIntegralLanes() {
        return this;
    }

    /**
     * {@inheritDoc} <!--workaround-->
     *
     * @implNote This method always throws
     * {@code UnsupportedOperationException}, because there is no floating
     * point type of the same size as {@code short}.  The return type
     * of this method is arbitrarily designated as
     * {@code Vector<?>}.  Future versions of this API may change the return
     * type if additional floating point types become available.
     */
    @ForceInline
    @Override
    public final
    Vector<?>
    viewAsFloatingLanes() {
        LaneType flt = LaneType.SHORT.asFloating();
        // asFloating() will throw UnsupportedOperationException for the unsupported type short
        throw new AssertionError("Cannot reach here");
    }

    // ================================================

    /// Object methods: toString, equals, hashCode
    //
    // Object methods are defined as if via Arrays.toString, etc.,
    // is applied to the array of elements.  Two equal vectors
    // are required to have equal species and equal lane values.

    /**
     * Returns a string representation of this vector, of the form
     * {@code "[0,1,2...]"}, reporting the lane values of this vector,
     * in lane order.
     *
     * The string is produced as if by a call to {@link
     * java.util.Arrays#toString(short[]) Arrays.toString()},
     * as appropriate to the {@code short} array returned by
     * {@link #toArray this.toArray()}.
     *
     * @return a string of the form {@code "[0,1,2...]"}
     * reporting the lane values of this vector
     */
    @Override
    @ForceInline
    public final
    String toString() {
        // now that toArray is strongly typed, we can define this
        return Arrays.toString(toArray());
    }

    /**
     * {@inheritDoc} <!--workaround-->
     */
    @Override
    @ForceInline
    public final
    boolean equals(Object obj) {
        if (obj instanceof Vector) {
            Vector<?> that = (Vector<?>) obj;
            if (this.species().equals(that.species())) {
                return this.eq(that.check(this.species())).allTrue();
            }
        }
        return false;
    }

    /**
     * {@inheritDoc} <!--workaround-->
     */
    @Override
    @ForceInline
    public final
    int hashCode() {
        // now that toArray is strongly typed, we can define this
        return Objects.hash(species(), Arrays.hashCode(toArray()));
    }

    // ================================================

    // Species

    /**
     * Class representing {@link ShortVector}'s of the same {@link VectorShape VectorShape}.
     */
    /*package-private*/
    static final class ShortSpecies extends AbstractSpecies<Short> {
        private ShortSpecies(VectorShape shape,
                Class<? extends ShortVector> vectorType,
                Class<? extends AbstractMask<Short>> maskType,
                Function<Object, ShortVector> vectorFactory) {
            super(shape, LaneType.of(short.class),
                  vectorType, maskType,
                  vectorFactory);
            assert(this.elementSize() == Short.SIZE);
        }

        // Specializing overrides:

        @Override
        @ForceInline
        public final Class<Short> elementType() {
            return short.class;
        }

        @Override
        @ForceInline
        final Class<Short> genericElementType() {
            return Short.class;
        }

        @SuppressWarnings("unchecked")
        @Override
        @ForceInline
        public final Class<? extends ShortVector> vectorType() {
            return (Class<? extends ShortVector>) vectorType;
        }

        @Override
        @ForceInline
        public final long checkValue(long e) {
            longToElementBits(e);  // only for exception
            return e;
        }

        /*package-private*/
        @Override
        @ForceInline
        final ShortVector broadcastBits(long bits) {
            return (ShortVector)
                VectorSupport.broadcastCoerced(
                    vectorType, short.class, laneCount,
                    bits, this,
                    (bits_, s_) -> s_.rvOp(i -> bits_));
        }

        /*package-private*/
        @ForceInline
        final ShortVector broadcast(short e) {
            return broadcastBits(toBits(e));
        }

        @Override
        @ForceInline
        public final ShortVector broadcast(long e) {
            return broadcastBits(longToElementBits(e));
        }

        /*package-private*/
        final @Override
        @ForceInline
        long longToElementBits(long value) {
            // Do the conversion, and then test it for failure.
            short e = (short) value;
            if ((long) e != value) {
                throw badElementBits(value, e);
            }
            return toBits(e);
        }

        /*package-private*/
        @ForceInline
        static long toIntegralChecked(short e, boolean convertToInt) {
            long value = convertToInt ? (int) e : (long) e;
            if ((short) value != e) {
                throw badArrayBits(e, convertToInt, value);
            }
            return value;
        }

        /* this non-public one is for internal conversions */
        @Override
        @ForceInline
        final ShortVector fromIntValues(int[] values) {
            VectorIntrinsics.requireLength(values.length, laneCount);
            short[] va = new short[laneCount()];
            for (int i = 0; i < va.length; i++) {
                int lv = values[i];
                short v = (short) lv;
                va[i] = v;
                if ((int)v != lv) {
                    throw badElementBits(lv, v);
                }
            }
            return dummyVector().fromArray0(va, 0);
        }

        // Virtual constructors

        @ForceInline
        @Override final
        public ShortVector fromArray(Object a, int offset) {
            // User entry point:  Be careful with inputs.
            return ShortVector
                .fromArray(this, (short[]) a, offset);
        }

        @ForceInline
        @Override final
        ShortVector dummyVector() {
            return (ShortVector) super.dummyVector();
        }

        /*package-private*/
        final @Override
        @ForceInline
        ShortVector rvOp(RVOp f) {
            short[] res = new short[laneCount()];
            for (int i = 0; i < res.length; i++) {
                short bits = (short) f.apply(i);
                res[i] = fromBits(bits);
            }
            return dummyVector().vectorFactory(res);
        }

        ShortVector vOp(FVOp f) {
            short[] res = new short[laneCount()];
            for (int i = 0; i < res.length; i++) {
                res[i] = f.apply(i);
            }
            return dummyVector().vectorFactory(res);
        }

        ShortVector vOp(VectorMask<Short> m, FVOp f) {
            short[] res = new short[laneCount()];
            boolean[] mbits = ((AbstractMask<Short>)m).getBits();
            for (int i = 0; i < res.length; i++) {
                if (mbits[i]) {
                    res[i] = f.apply(i);
                }
            }
            return dummyVector().vectorFactory(res);
        }

        /*package-private*/
        @ForceInline
        <M> ShortVector ldOp(M memory, int offset,
                                      FLdOp<M> f) {
            return dummyVector().ldOp(memory, offset, f);
        }

        /*package-private*/
        @ForceInline
        <M> ShortVector ldOp(M memory, int offset,
                                      AbstractMask<Short> m,
                                      FLdOp<M> f) {
            return dummyVector().ldOp(memory, offset, m, f);
        }

        /*package-private*/
        @ForceInline
        <M> void stOp(M memory, int offset, FStOp<M> f) {
            dummyVector().stOp(memory, offset, f);
        }

        /*package-private*/
        @ForceInline
        <M> void stOp(M memory, int offset,
                      AbstractMask<Short> m,
                      FStOp<M> f) {
            dummyVector().stOp(memory, offset, m, f);
        }

        // N.B. Make sure these constant vectors and
        // masks load up correctly into registers.
        //
        // Also, see if we can avoid all that switching.
        // Could we cache both vectors and both masks in
        // this species object?

        // Zero and iota vector access
        @Override
        @ForceInline
        public final ShortVector zero() {
            if ((Class<?>) vectorType() == ShortMaxVector.class)
                return ShortMaxVector.ZERO;
            switch (vectorBitSize()) {
                case 64: return Short64Vector.ZERO;
                case 128: return Short128Vector.ZERO;
                case 256: return Short256Vector.ZERO;
                case 512: return Short512Vector.ZERO;
            }
            throw new AssertionError();
        }

        @Override
        @ForceInline
        public final ShortVector iota() {
            if ((Class<?>) vectorType() == ShortMaxVector.class)
                return ShortMaxVector.IOTA;
            switch (vectorBitSize()) {
                case 64: return Short64Vector.IOTA;
                case 128: return Short128Vector.IOTA;
                case 256: return Short256Vector.IOTA;
                case 512: return Short512Vector.IOTA;
            }
            throw new AssertionError();
        }

        // Mask access
        @Override
        @ForceInline
        public final VectorMask<Short> maskAll(boolean bit) {
            if ((Class<?>) vectorType() == ShortMaxVector.class)
                return ShortMaxVector.ShortMaxMask.maskAll(bit);
            switch (vectorBitSize()) {
                case 64: return Short64Vector.Short64Mask.maskAll(bit);
                case 128: return Short128Vector.Short128Mask.maskAll(bit);
                case 256: return Short256Vector.Short256Mask.maskAll(bit);
                case 512: return Short512Vector.Short512Mask.maskAll(bit);
            }
            throw new AssertionError();
        }
    }

    /**
     * Finds a species for an element type of {@code short} and shape.
     *
     * @param s the shape
     * @return a species for an element type of {@code short} and shape
     * @throws IllegalArgumentException if no such species exists for the shape
     */
    static ShortSpecies species(VectorShape s) {
        Objects.requireNonNull(s);
        switch (s) {
            case S_64_BIT: return (ShortSpecies) SPECIES_64;
            case S_128_BIT: return (ShortSpecies) SPECIES_128;
            case S_256_BIT: return (ShortSpecies) SPECIES_256;
            case S_512_BIT: return (ShortSpecies) SPECIES_512;
            case S_Max_BIT: return (ShortSpecies) SPECIES_MAX;
            default: throw new IllegalArgumentException("Bad shape: " + s);
        }
    }

    /** Species representing {@link ShortVector}s of {@link VectorShape#S_64_BIT VectorShape.S_64_BIT}. */
    public static final VectorSpecies<Short> SPECIES_64
        = new ShortSpecies(VectorShape.S_64_BIT,
                            Short64Vector.class,
                            Short64Vector.Short64Mask.class,
                            Short64Vector::new);

    /** Species representing {@link ShortVector}s of {@link VectorShape#S_128_BIT VectorShape.S_128_BIT}. */
    public static final VectorSpecies<Short> SPECIES_128
        = new ShortSpecies(VectorShape.S_128_BIT,
                            Short128Vector.class,
                            Short128Vector.Short128Mask.class,
                            Short128Vector::new);

    /** Species representing {@link ShortVector}s of {@link VectorShape#S_256_BIT VectorShape.S_256_BIT}. */
    public static final VectorSpecies<Short> SPECIES_256
        = new ShortSpecies(VectorShape.S_256_BIT,
                            Short256Vector.class,
                            Short256Vector.Short256Mask.class,
                            Short256Vector::new);

    /** Species representing {@link ShortVector}s of {@link VectorShape#S_512_BIT VectorShape.S_512_BIT}. */
    public static final VectorSpecies<Short> SPECIES_512
        = new ShortSpecies(VectorShape.S_512_BIT,
                            Short512Vector.class,
                            Short512Vector.Short512Mask.class,
                            Short512Vector::new);

    /** Species representing {@link ShortVector}s of {@link VectorShape#S_Max_BIT VectorShape.S_Max_BIT}. */
    public static final VectorSpecies<Short> SPECIES_MAX
        = new ShortSpecies(VectorShape.S_Max_BIT,
                            ShortMaxVector.class,
                            ShortMaxVector.ShortMaxMask.class,
                            ShortMaxVector::new);

    /**
     * Preferred species for {@link ShortVector}s.
     * A preferred species is a species of maximal bit-size for the platform.
     */
    public static final VectorSpecies<Short> SPECIES_PREFERRED
        = (ShortSpecies) VectorSpecies.ofPreferred(short.class);
}
