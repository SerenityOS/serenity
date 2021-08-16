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
 * {@code int} values.
 */
@SuppressWarnings("cast")  // warning: redundant cast
public abstract class IntVector extends AbstractVector<Integer> {

    IntVector(int[] vec) {
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
    abstract int[] vec();

    // Virtualized constructors

    /**
     * Build a vector directly using my own constructor.
     * It is an error if the array is aliased elsewhere.
     */
    /*package-private*/
    abstract IntVector vectorFactory(int[] vec);

    /**
     * Build a mask directly using my species.
     * It is an error if the array is aliased elsewhere.
     */
    /*package-private*/
    @ForceInline
    final
    AbstractMask<Integer> maskFactory(boolean[] bits) {
        return vspecies().maskFactory(bits);
    }

    // Constant loader (takes dummy as vector arg)
    interface FVOp {
        int apply(int i);
    }

    /*package-private*/
    @ForceInline
    final
    IntVector vOp(FVOp f) {
        int[] res = new int[length()];
        for (int i = 0; i < res.length; i++) {
            res[i] = f.apply(i);
        }
        return vectorFactory(res);
    }

    @ForceInline
    final
    IntVector vOp(VectorMask<Integer> m, FVOp f) {
        int[] res = new int[length()];
        boolean[] mbits = ((AbstractMask<Integer>)m).getBits();
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
        int apply(int i, int a);
    }

    /*package-private*/
    abstract
    IntVector uOp(FUnOp f);
    @ForceInline
    final
    IntVector uOpTemplate(FUnOp f) {
        int[] vec = vec();
        int[] res = new int[length()];
        for (int i = 0; i < res.length; i++) {
            res[i] = f.apply(i, vec[i]);
        }
        return vectorFactory(res);
    }

    /*package-private*/
    abstract
    IntVector uOp(VectorMask<Integer> m,
                             FUnOp f);
    @ForceInline
    final
    IntVector uOpTemplate(VectorMask<Integer> m,
                                     FUnOp f) {
        int[] vec = vec();
        int[] res = new int[length()];
        boolean[] mbits = ((AbstractMask<Integer>)m).getBits();
        for (int i = 0; i < res.length; i++) {
            res[i] = mbits[i] ? f.apply(i, vec[i]) : vec[i];
        }
        return vectorFactory(res);
    }

    // Binary operator

    /*package-private*/
    interface FBinOp {
        int apply(int i, int a, int b);
    }

    /*package-private*/
    abstract
    IntVector bOp(Vector<Integer> o,
                             FBinOp f);
    @ForceInline
    final
    IntVector bOpTemplate(Vector<Integer> o,
                                     FBinOp f) {
        int[] res = new int[length()];
        int[] vec1 = this.vec();
        int[] vec2 = ((IntVector)o).vec();
        for (int i = 0; i < res.length; i++) {
            res[i] = f.apply(i, vec1[i], vec2[i]);
        }
        return vectorFactory(res);
    }

    /*package-private*/
    abstract
    IntVector bOp(Vector<Integer> o,
                             VectorMask<Integer> m,
                             FBinOp f);
    @ForceInline
    final
    IntVector bOpTemplate(Vector<Integer> o,
                                     VectorMask<Integer> m,
                                     FBinOp f) {
        int[] res = new int[length()];
        int[] vec1 = this.vec();
        int[] vec2 = ((IntVector)o).vec();
        boolean[] mbits = ((AbstractMask<Integer>)m).getBits();
        for (int i = 0; i < res.length; i++) {
            res[i] = mbits[i] ? f.apply(i, vec1[i], vec2[i]) : vec1[i];
        }
        return vectorFactory(res);
    }

    // Ternary operator

    /*package-private*/
    interface FTriOp {
        int apply(int i, int a, int b, int c);
    }

    /*package-private*/
    abstract
    IntVector tOp(Vector<Integer> o1,
                             Vector<Integer> o2,
                             FTriOp f);
    @ForceInline
    final
    IntVector tOpTemplate(Vector<Integer> o1,
                                     Vector<Integer> o2,
                                     FTriOp f) {
        int[] res = new int[length()];
        int[] vec1 = this.vec();
        int[] vec2 = ((IntVector)o1).vec();
        int[] vec3 = ((IntVector)o2).vec();
        for (int i = 0; i < res.length; i++) {
            res[i] = f.apply(i, vec1[i], vec2[i], vec3[i]);
        }
        return vectorFactory(res);
    }

    /*package-private*/
    abstract
    IntVector tOp(Vector<Integer> o1,
                             Vector<Integer> o2,
                             VectorMask<Integer> m,
                             FTriOp f);
    @ForceInline
    final
    IntVector tOpTemplate(Vector<Integer> o1,
                                     Vector<Integer> o2,
                                     VectorMask<Integer> m,
                                     FTriOp f) {
        int[] res = new int[length()];
        int[] vec1 = this.vec();
        int[] vec2 = ((IntVector)o1).vec();
        int[] vec3 = ((IntVector)o2).vec();
        boolean[] mbits = ((AbstractMask<Integer>)m).getBits();
        for (int i = 0; i < res.length; i++) {
            res[i] = mbits[i] ? f.apply(i, vec1[i], vec2[i], vec3[i]) : vec1[i];
        }
        return vectorFactory(res);
    }

    // Reduction operator

    /*package-private*/
    abstract
    int rOp(int v, FBinOp f);
    @ForceInline
    final
    int rOpTemplate(int v, FBinOp f) {
        int[] vec = vec();
        for (int i = 0; i < vec.length; i++) {
            v = f.apply(i, v, vec[i]);
        }
        return v;
    }

    // Memory reference

    /*package-private*/
    interface FLdOp<M> {
        int apply(M memory, int offset, int i);
    }

    /*package-private*/
    @ForceInline
    final
    <M> IntVector ldOp(M memory, int offset,
                                  FLdOp<M> f) {
        //dummy; no vec = vec();
        int[] res = new int[length()];
        for (int i = 0; i < res.length; i++) {
            res[i] = f.apply(memory, offset, i);
        }
        return vectorFactory(res);
    }

    /*package-private*/
    @ForceInline
    final
    <M> IntVector ldOp(M memory, int offset,
                                  VectorMask<Integer> m,
                                  FLdOp<M> f) {
        //int[] vec = vec();
        int[] res = new int[length()];
        boolean[] mbits = ((AbstractMask<Integer>)m).getBits();
        for (int i = 0; i < res.length; i++) {
            if (mbits[i]) {
                res[i] = f.apply(memory, offset, i);
            }
        }
        return vectorFactory(res);
    }

    interface FStOp<M> {
        void apply(M memory, int offset, int i, int a);
    }

    /*package-private*/
    @ForceInline
    final
    <M> void stOp(M memory, int offset,
                  FStOp<M> f) {
        int[] vec = vec();
        for (int i = 0; i < vec.length; i++) {
            f.apply(memory, offset, i, vec[i]);
        }
    }

    /*package-private*/
    @ForceInline
    final
    <M> void stOp(M memory, int offset,
                  VectorMask<Integer> m,
                  FStOp<M> f) {
        int[] vec = vec();
        boolean[] mbits = ((AbstractMask<Integer>)m).getBits();
        for (int i = 0; i < vec.length; i++) {
            if (mbits[i]) {
                f.apply(memory, offset, i, vec[i]);
            }
        }
    }

    // Binary test

    /*package-private*/
    interface FBinTest {
        boolean apply(int cond, int i, int a, int b);
    }

    /*package-private*/
    @ForceInline
    final
    AbstractMask<Integer> bTest(int cond,
                                  Vector<Integer> o,
                                  FBinTest f) {
        int[] vec1 = vec();
        int[] vec2 = ((IntVector)o).vec();
        boolean[] bits = new boolean[length()];
        for (int i = 0; i < length(); i++){
            bits[i] = f.apply(cond, i, vec1[i], vec2[i]);
        }
        return maskFactory(bits);
    }

    /*package-private*/
    @Override
    abstract IntSpecies vspecies();

    /*package-private*/
    @ForceInline
    static long toBits(int e) {
        return  e;
    }

    /*package-private*/
    @ForceInline
    static int fromBits(long bits) {
        return ((int)bits);
    }

    // Static factories (other than memory operations)

    // Note: A surprising behavior in javadoc
    // sometimes makes a lone /** {@inheritDoc} */
    // comment drop the method altogether,
    // apparently if the method mentions an
    // parameter or return type of Vector<Integer>
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
    public static IntVector zero(VectorSpecies<Integer> species) {
        IntSpecies vsp = (IntSpecies) species;
        return VectorSupport.broadcastCoerced(vsp.vectorType(), int.class, species.length(),
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
     * {@code IntVector.broadcast(this.species(), e)}.
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
    public abstract IntVector broadcast(int e);

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
    public static IntVector broadcast(VectorSpecies<Integer> species, int e) {
        IntSpecies vsp = (IntSpecies) species;
        return vsp.broadcast(e);
    }

    /*package-private*/
    @ForceInline
    final IntVector broadcastTemplate(int e) {
        IntSpecies vsp = vspecies();
        return vsp.broadcast(e);
    }

    /**
     * {@inheritDoc} <!--workaround-->
     * @apiNote
     * When working with vector subtypes like {@code IntVector},
     * {@linkplain #broadcast(int) the more strongly typed method}
     * is typically selected.  It can be explicitly selected
     * using a cast: {@code v.broadcast((int)e)}.
     * The two expressions will produce numerically identical results.
     */
    @Override
    public abstract IntVector broadcast(long e);

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
     * @see #broadcast(VectorSpecies,int)
     * @see VectorSpecies#checkValue(long)
     */
    @ForceInline
    public static IntVector broadcast(VectorSpecies<Integer> species, long e) {
        IntSpecies vsp = (IntSpecies) species;
        return vsp.broadcast(e);
    }

    /*package-private*/
    @ForceInline
    final IntVector broadcastTemplate(long e) {
        return vspecies().broadcast(e);
    }

    // Unary lanewise support

    /**
     * {@inheritDoc} <!--workaround-->
     */
    public abstract
    IntVector lanewise(VectorOperators.Unary op);

    @ForceInline
    final
    IntVector lanewiseTemplate(VectorOperators.Unary op) {
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
            opc, getClass(), int.class, length(),
            this,
            UN_IMPL.find(op, opc, (opc_) -> {
              switch (opc_) {
                case VECTOR_OP_NEG: return v0 ->
                        v0.uOp((i, a) -> (int) -a);
                case VECTOR_OP_ABS: return v0 ->
                        v0.uOp((i, a) -> (int) Math.abs(a));
                default: return null;
              }}));
    }
    private static final
    ImplCache<Unary,UnaryOperator<IntVector>> UN_IMPL
        = new ImplCache<>(Unary.class, IntVector.class);

    /**
     * {@inheritDoc} <!--workaround-->
     */
    @ForceInline
    public final
    IntVector lanewise(VectorOperators.Unary op,
                                  VectorMask<Integer> m) {
        return blend(lanewise(op), m);
    }

    // Binary lanewise support

    /**
     * {@inheritDoc} <!--workaround-->
     * @see #lanewise(VectorOperators.Binary,int)
     * @see #lanewise(VectorOperators.Binary,int,VectorMask)
     */
    @Override
    public abstract
    IntVector lanewise(VectorOperators.Binary op,
                                  Vector<Integer> v);
    @ForceInline
    final
    IntVector lanewiseTemplate(VectorOperators.Binary op,
                                          Vector<Integer> v) {
        IntVector that = (IntVector) v;
        that.check(this);
        if (opKind(op, VO_SPECIAL  | VO_SHIFT)) {
            if (op == FIRST_NONZERO) {
                // FIXME: Support this in the JIT.
                VectorMask<Integer> thisNZ
                    = this.viewAsIntegralLanes().compare(NE, (int) 0);
                that = that.blend((int) 0, thisNZ.cast(vspecies()));
                op = OR_UNCHECKED;
            }
            if (opKind(op, VO_SHIFT)) {
                // As per shift specification for Java, mask the shift count.
                // This allows the JIT to ignore some ISA details.
                that = that.lanewise(AND, SHIFT_MASK);
            }
            if (op == ROR || op == ROL) {  // FIXME: JIT should do this
                IntVector neg = that.lanewise(NEG);
                IntVector hi = this.lanewise(LSHL, (op == ROR) ? neg : that);
                IntVector lo = this.lanewise(LSHR, (op == ROR) ? that : neg);
                return hi.lanewise(OR, lo);
            } else if (op == AND_NOT) {
                // FIXME: Support this in the JIT.
                that = that.lanewise(NOT);
                op = AND;
            } else if (op == DIV) {
                VectorMask<Integer> eqz = that.eq((int)0);
                if (eqz.anyTrue()) {
                    throw that.divZeroException();
                }
            }
        }
        int opc = opCode(op);
        return VectorSupport.binaryOp(
            opc, getClass(), int.class, length(),
            this, that,
            BIN_IMPL.find(op, opc, (opc_) -> {
              switch (opc_) {
                case VECTOR_OP_ADD: return (v0, v1) ->
                        v0.bOp(v1, (i, a, b) -> (int)(a + b));
                case VECTOR_OP_SUB: return (v0, v1) ->
                        v0.bOp(v1, (i, a, b) -> (int)(a - b));
                case VECTOR_OP_MUL: return (v0, v1) ->
                        v0.bOp(v1, (i, a, b) -> (int)(a * b));
                case VECTOR_OP_DIV: return (v0, v1) ->
                        v0.bOp(v1, (i, a, b) -> (int)(a / b));
                case VECTOR_OP_MAX: return (v0, v1) ->
                        v0.bOp(v1, (i, a, b) -> (int)Math.max(a, b));
                case VECTOR_OP_MIN: return (v0, v1) ->
                        v0.bOp(v1, (i, a, b) -> (int)Math.min(a, b));
                case VECTOR_OP_AND: return (v0, v1) ->
                        v0.bOp(v1, (i, a, b) -> (int)(a & b));
                case VECTOR_OP_OR: return (v0, v1) ->
                        v0.bOp(v1, (i, a, b) -> (int)(a | b));
                case VECTOR_OP_XOR: return (v0, v1) ->
                        v0.bOp(v1, (i, a, b) -> (int)(a ^ b));
                case VECTOR_OP_LSHIFT: return (v0, v1) ->
                        v0.bOp(v1, (i, a, n) -> (int)(a << n));
                case VECTOR_OP_RSHIFT: return (v0, v1) ->
                        v0.bOp(v1, (i, a, n) -> (int)(a >> n));
                case VECTOR_OP_URSHIFT: return (v0, v1) ->
                        v0.bOp(v1, (i, a, n) -> (int)((a & LSHR_SETUP_MASK) >>> n));
                default: return null;
                }}));
    }
    private static final
    ImplCache<Binary,BinaryOperator<IntVector>> BIN_IMPL
        = new ImplCache<>(Binary.class, IntVector.class);

    /**
     * {@inheritDoc} <!--workaround-->
     * @see #lanewise(VectorOperators.Binary,int,VectorMask)
     */
    @ForceInline
    public final
    IntVector lanewise(VectorOperators.Binary op,
                                  Vector<Integer> v,
                                  VectorMask<Integer> m) {
        IntVector that = (IntVector) v;
        if (op == DIV) {
            VectorMask<Integer> eqz = that.eq((int)0);
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
     * @see #lanewise(VectorOperators.Binary,int,VectorMask)
     */
    @ForceInline
    public final
    IntVector lanewise(VectorOperators.Binary op,
                                  int e) {
        if (opKind(op, VO_SHIFT) && (int)(int)e == e) {
            return lanewiseShift(op, (int) e);
        }
        if (op == AND_NOT) {
            op = AND; e = (int) ~e;
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
     * @see #lanewise(VectorOperators.Binary,int)
     */
    @ForceInline
    public final
    IntVector lanewise(VectorOperators.Binary op,
                                  int e,
                                  VectorMask<Integer> m) {
        return blend(lanewise(op, e), m);
    }

    /**
     * {@inheritDoc} <!--workaround-->
     * @apiNote
     * When working with vector subtypes like {@code IntVector},
     * {@linkplain #lanewise(VectorOperators.Binary,int)
     * the more strongly typed method}
     * is typically selected.  It can be explicitly selected
     * using a cast: {@code v.lanewise(op,(int)e)}.
     * The two expressions will produce numerically identical results.
     */
    @ForceInline
    public final
    IntVector lanewise(VectorOperators.Binary op,
                                  long e) {
        int e1 = (int) e;
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
     * When working with vector subtypes like {@code IntVector},
     * {@linkplain #lanewise(VectorOperators.Binary,int,VectorMask)
     * the more strongly typed method}
     * is typically selected.  It can be explicitly selected
     * using a cast: {@code v.lanewise(op,(int)e,m)}.
     * The two expressions will produce numerically identical results.
     */
    @ForceInline
    public final
    IntVector lanewise(VectorOperators.Binary op,
                                  long e, VectorMask<Integer> m) {
        return blend(lanewise(op, e), m);
    }

    /*package-private*/
    abstract IntVector
    lanewiseShift(VectorOperators.Binary op, int e);

    /*package-private*/
    @ForceInline
    final IntVector
    lanewiseShiftTemplate(VectorOperators.Binary op, int e) {
        // Special handling for these.  FIXME: Refactor?
        assert(opKind(op, VO_SHIFT));
        // As per shift specification for Java, mask the shift count.
        e &= SHIFT_MASK;
        if (op == ROR || op == ROL) {  // FIXME: JIT should do this
            IntVector hi = this.lanewise(LSHL, (op == ROR) ? -e : e);
            IntVector lo = this.lanewise(LSHR, (op == ROR) ? e : -e);
            return hi.lanewise(OR, lo);
        }
        int opc = opCode(op);
        return VectorSupport.broadcastInt(
            opc, getClass(), int.class, length(),
            this, e,
            BIN_INT_IMPL.find(op, opc, (opc_) -> {
              switch (opc_) {
                case VECTOR_OP_LSHIFT: return (v, n) ->
                        v.uOp((i, a) -> (int)(a << n));
                case VECTOR_OP_RSHIFT: return (v, n) ->
                        v.uOp((i, a) -> (int)(a >> n));
                case VECTOR_OP_URSHIFT: return (v, n) ->
                        v.uOp((i, a) -> (int)((a & LSHR_SETUP_MASK) >>> n));
                default: return null;
                }}));
    }
    private static final
    ImplCache<Binary,VectorBroadcastIntOp<IntVector>> BIN_INT_IMPL
        = new ImplCache<>(Binary.class, IntVector.class);

    // As per shift specification for Java, mask the shift count.
    // We mask 0X3F (long), 0X1F (int), 0x0F (short), 0x7 (byte).
    // The latter two maskings go beyond the JLS, but seem reasonable
    // since our lane types are first-class types, not just dressed
    // up ints.
    private static final int SHIFT_MASK = (Integer.SIZE - 1);
    private static final int LSHR_SETUP_MASK = -1;

    // Ternary lanewise support

    // Ternary operators come in eight variations:
    //   lanewise(op, [broadcast(e1)|v1], [broadcast(e2)|v2])
    //   lanewise(op, [broadcast(e1)|v1], [broadcast(e2)|v2], mask)

    // It is annoying to support all of these variations of masking
    // and broadcast, but it would be more surprising not to continue
    // the obvious pattern started by unary and binary.

   /**
     * {@inheritDoc} <!--workaround-->
     * @see #lanewise(VectorOperators.Ternary,int,int,VectorMask)
     * @see #lanewise(VectorOperators.Ternary,Vector,int,VectorMask)
     * @see #lanewise(VectorOperators.Ternary,int,Vector,VectorMask)
     * @see #lanewise(VectorOperators.Ternary,int,int)
     * @see #lanewise(VectorOperators.Ternary,Vector,int)
     * @see #lanewise(VectorOperators.Ternary,int,Vector)
     */
    @Override
    public abstract
    IntVector lanewise(VectorOperators.Ternary op,
                                                  Vector<Integer> v1,
                                                  Vector<Integer> v2);
    @ForceInline
    final
    IntVector lanewiseTemplate(VectorOperators.Ternary op,
                                          Vector<Integer> v1,
                                          Vector<Integer> v2) {
        IntVector that = (IntVector) v1;
        IntVector tother = (IntVector) v2;
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
            opc, getClass(), int.class, length(),
            this, that, tother,
            TERN_IMPL.find(op, opc, (opc_) -> {
              switch (opc_) {
                default: return null;
                }}));
    }
    private static final
    ImplCache<Ternary,TernaryOperation<IntVector>> TERN_IMPL
        = new ImplCache<>(Ternary.class, IntVector.class);

    /**
     * {@inheritDoc} <!--workaround-->
     * @see #lanewise(VectorOperators.Ternary,int,int,VectorMask)
     * @see #lanewise(VectorOperators.Ternary,Vector,int,VectorMask)
     * @see #lanewise(VectorOperators.Ternary,int,Vector,VectorMask)
     */
    @ForceInline
    public final
    IntVector lanewise(VectorOperators.Ternary op,
                                  Vector<Integer> v1,
                                  Vector<Integer> v2,
                                  VectorMask<Integer> m) {
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
     * @see #lanewise(VectorOperators.Ternary,int,int,VectorMask)
     */
    @ForceInline
    public final
    IntVector lanewise(VectorOperators.Ternary op, //(op,e1,e2)
                                  int e1,
                                  int e2) {
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
     * @see #lanewise(VectorOperators.Ternary,int,int)
     */
    @ForceInline
    public final
    IntVector lanewise(VectorOperators.Ternary op, //(op,e1,e2,m)
                                  int e1,
                                  int e2,
                                  VectorMask<Integer> m) {
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
     * @see #lanewise(VectorOperators.Ternary,int,int)
     * @see #lanewise(VectorOperators.Ternary,Vector,int,VectorMask)
     */
    @ForceInline
    public final
    IntVector lanewise(VectorOperators.Ternary op, //(op,v1,e2)
                                  Vector<Integer> v1,
                                  int e2) {
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
     * @see #lanewise(VectorOperators.Ternary,int,int,VectorMask)
     * @see #lanewise(VectorOperators.Ternary,Vector,int)
     */
    @ForceInline
    public final
    IntVector lanewise(VectorOperators.Ternary op, //(op,v1,e2,m)
                                  Vector<Integer> v1,
                                  int e2,
                                  VectorMask<Integer> m) {
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
     * @see #lanewise(VectorOperators.Ternary,int,Vector,VectorMask)
     */
    @ForceInline
    public final
    IntVector lanewise(VectorOperators.Ternary op, //(op,e1,v2)
                                  int e1,
                                  Vector<Integer> v2) {
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
     * @see #lanewise(VectorOperators.Ternary,int,Vector)
     */
    @ForceInline
    public final
    IntVector lanewise(VectorOperators.Ternary op, //(op,e1,v2,m)
                                  int e1,
                                  Vector<Integer> v2,
                                  VectorMask<Integer> m) {
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
     * @see #add(int)
     */
    @Override
    @ForceInline
    public final IntVector add(Vector<Integer> v) {
        return lanewise(ADD, v);
    }

    /**
     * Adds this vector to the broadcast of an input scalar.
     *
     * This is a lane-wise binary operation which applies
     * the primitive addition operation ({@code +}) to each lane.
     *
     * This method is also equivalent to the expression
     * {@link #lanewise(VectorOperators.Binary,int)
     *    lanewise}{@code (}{@link VectorOperators#ADD
     *    ADD}{@code , e)}.
     *
     * @param e the input scalar
     * @return the result of adding each lane of this vector to the scalar
     * @see #add(Vector)
     * @see #broadcast(int)
     * @see #add(int,VectorMask)
     * @see VectorOperators#ADD
     * @see #lanewise(VectorOperators.Binary,Vector)
     * @see #lanewise(VectorOperators.Binary,int)
     */
    @ForceInline
    public final
    IntVector add(int e) {
        return lanewise(ADD, e);
    }

    /**
     * {@inheritDoc} <!--workaround-->
     * @see #add(int,VectorMask)
     */
    @Override
    @ForceInline
    public final IntVector add(Vector<Integer> v,
                                          VectorMask<Integer> m) {
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
     * {@link #lanewise(VectorOperators.Binary,int,VectorMask)
     *    lanewise}{@code (}{@link VectorOperators#ADD
     *    ADD}{@code , s, m)}.
     *
     * @param e the input scalar
     * @param m the mask controlling lane selection
     * @return the result of adding each lane of this vector to the scalar
     * @see #add(Vector,VectorMask)
     * @see #broadcast(int)
     * @see #add(int)
     * @see VectorOperators#ADD
     * @see #lanewise(VectorOperators.Binary,Vector)
     * @see #lanewise(VectorOperators.Binary,int)
     */
    @ForceInline
    public final IntVector add(int e,
                                          VectorMask<Integer> m) {
        return lanewise(ADD, e, m);
    }

    /**
     * {@inheritDoc} <!--workaround-->
     * @see #sub(int)
     */
    @Override
    @ForceInline
    public final IntVector sub(Vector<Integer> v) {
        return lanewise(SUB, v);
    }

    /**
     * Subtracts an input scalar from this vector.
     *
     * This is a masked lane-wise binary operation which applies
     * the primitive subtraction operation ({@code -}) to each lane.
     *
     * This method is also equivalent to the expression
     * {@link #lanewise(VectorOperators.Binary,int)
     *    lanewise}{@code (}{@link VectorOperators#SUB
     *    SUB}{@code , e)}.
     *
     * @param e the input scalar
     * @return the result of subtracting the scalar from each lane of this vector
     * @see #sub(Vector)
     * @see #broadcast(int)
     * @see #sub(int,VectorMask)
     * @see VectorOperators#SUB
     * @see #lanewise(VectorOperators.Binary,Vector)
     * @see #lanewise(VectorOperators.Binary,int)
     */
    @ForceInline
    public final IntVector sub(int e) {
        return lanewise(SUB, e);
    }

    /**
     * {@inheritDoc} <!--workaround-->
     * @see #sub(int,VectorMask)
     */
    @Override
    @ForceInline
    public final IntVector sub(Vector<Integer> v,
                                          VectorMask<Integer> m) {
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
     * {@link #lanewise(VectorOperators.Binary,int,VectorMask)
     *    lanewise}{@code (}{@link VectorOperators#SUB
     *    SUB}{@code , s, m)}.
     *
     * @param e the input scalar
     * @param m the mask controlling lane selection
     * @return the result of subtracting the scalar from each lane of this vector
     * @see #sub(Vector,VectorMask)
     * @see #broadcast(int)
     * @see #sub(int)
     * @see VectorOperators#SUB
     * @see #lanewise(VectorOperators.Binary,Vector)
     * @see #lanewise(VectorOperators.Binary,int)
     */
    @ForceInline
    public final IntVector sub(int e,
                                          VectorMask<Integer> m) {
        return lanewise(SUB, e, m);
    }

    /**
     * {@inheritDoc} <!--workaround-->
     * @see #mul(int)
     */
    @Override
    @ForceInline
    public final IntVector mul(Vector<Integer> v) {
        return lanewise(MUL, v);
    }

    /**
     * Multiplies this vector by the broadcast of an input scalar.
     *
     * This is a lane-wise binary operation which applies
     * the primitive multiplication operation ({@code *}) to each lane.
     *
     * This method is also equivalent to the expression
     * {@link #lanewise(VectorOperators.Binary,int)
     *    lanewise}{@code (}{@link VectorOperators#MUL
     *    MUL}{@code , e)}.
     *
     * @param e the input scalar
     * @return the result of multiplying this vector by the given scalar
     * @see #mul(Vector)
     * @see #broadcast(int)
     * @see #mul(int,VectorMask)
     * @see VectorOperators#MUL
     * @see #lanewise(VectorOperators.Binary,Vector)
     * @see #lanewise(VectorOperators.Binary,int)
     */
    @ForceInline
    public final IntVector mul(int e) {
        return lanewise(MUL, e);
    }

    /**
     * {@inheritDoc} <!--workaround-->
     * @see #mul(int,VectorMask)
     */
    @Override
    @ForceInline
    public final IntVector mul(Vector<Integer> v,
                                          VectorMask<Integer> m) {
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
     * {@link #lanewise(VectorOperators.Binary,int,VectorMask)
     *    lanewise}{@code (}{@link VectorOperators#MUL
     *    MUL}{@code , s, m)}.
     *
     * @param e the input scalar
     * @param m the mask controlling lane selection
     * @return the result of muling each lane of this vector to the scalar
     * @see #mul(Vector,VectorMask)
     * @see #broadcast(int)
     * @see #mul(int)
     * @see VectorOperators#MUL
     * @see #lanewise(VectorOperators.Binary,Vector)
     * @see #lanewise(VectorOperators.Binary,int)
     */
    @ForceInline
    public final IntVector mul(int e,
                                          VectorMask<Integer> m) {
        return lanewise(MUL, e, m);
    }

    /**
     * {@inheritDoc} <!--workaround-->
     * @apiNote If there is a zero divisor, {@code
     * ArithmeticException} will be thrown.
     */
    @Override
    @ForceInline
    public final IntVector div(Vector<Integer> v) {
        return lanewise(DIV, v);
    }

    /**
     * Divides this vector by the broadcast of an input scalar.
     *
     * This is a lane-wise binary operation which applies
     * the primitive division operation ({@code /}) to each lane.
     *
     * This method is also equivalent to the expression
     * {@link #lanewise(VectorOperators.Binary,int)
     *    lanewise}{@code (}{@link VectorOperators#DIV
     *    DIV}{@code , e)}.
     *
     * @apiNote If there is a zero divisor, {@code
     * ArithmeticException} will be thrown.
     *
     * @param e the input scalar
     * @return the result of dividing each lane of this vector by the scalar
     * @see #div(Vector)
     * @see #broadcast(int)
     * @see #div(int,VectorMask)
     * @see VectorOperators#DIV
     * @see #lanewise(VectorOperators.Binary,Vector)
     * @see #lanewise(VectorOperators.Binary,int)
     */
    @ForceInline
    public final IntVector div(int e) {
        return lanewise(DIV, e);
    }

    /**
     * {@inheritDoc} <!--workaround-->
     * @see #div(int,VectorMask)
     * @apiNote If there is a zero divisor, {@code
     * ArithmeticException} will be thrown.
     */
    @Override
    @ForceInline
    public final IntVector div(Vector<Integer> v,
                                          VectorMask<Integer> m) {
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
     * {@link #lanewise(VectorOperators.Binary,int,VectorMask)
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
     * @see #broadcast(int)
     * @see #div(int)
     * @see VectorOperators#DIV
     * @see #lanewise(VectorOperators.Binary,Vector)
     * @see #lanewise(VectorOperators.Binary,int)
     */
    @ForceInline
    public final IntVector div(int e,
                                          VectorMask<Integer> m) {
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
    public final IntVector min(Vector<Integer> v) {
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
     * {@link #lanewise(VectorOperators.Binary,int)
     *    lanewise}{@code (}{@link VectorOperators#MIN
     *    MIN}{@code , e)}.
     *
     * @param e the input scalar
     * @return the result of multiplying this vector by the given scalar
     * @see #min(Vector)
     * @see #broadcast(int)
     * @see VectorOperators#MIN
     * @see #lanewise(VectorOperators.Binary,int,VectorMask)
     */
    @ForceInline
    public final IntVector min(int e) {
        return lanewise(MIN, e);
    }

    /**
     * {@inheritDoc} <!--workaround-->
     */
    @Override
    @ForceInline
    public final IntVector max(Vector<Integer> v) {
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
     * {@link #lanewise(VectorOperators.Binary,int)
     *    lanewise}{@code (}{@link VectorOperators#MAX
     *    MAX}{@code , e)}.
     *
     * @param e the input scalar
     * @return the result of multiplying this vector by the given scalar
     * @see #max(Vector)
     * @see #broadcast(int)
     * @see VectorOperators#MAX
     * @see #lanewise(VectorOperators.Binary,int,VectorMask)
     */
    @ForceInline
    public final IntVector max(int e) {
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
     * @see #and(int)
     * @see #or(Vector)
     * @see #not()
     * @see VectorOperators#AND
     * @see #lanewise(VectorOperators.Binary,Vector,VectorMask)
     */
    @ForceInline
    public final IntVector and(Vector<Integer> v) {
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
    public final IntVector and(int e) {
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
     * @see #or(int)
     * @see #and(Vector)
     * @see #not()
     * @see VectorOperators#OR
     * @see #lanewise(VectorOperators.Binary,Vector,VectorMask)
     */
    @ForceInline
    public final IntVector or(Vector<Integer> v) {
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
    public final IntVector or(int e) {
        return lanewise(OR, e);
    }



    /// UNARY METHODS

    /**
     * {@inheritDoc} <!--workaround-->
     */
    @Override
    @ForceInline
    public final
    IntVector neg() {
        return lanewise(NEG);
    }

    /**
     * {@inheritDoc} <!--workaround-->
     */
    @Override
    @ForceInline
    public final
    IntVector abs() {
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
    public final IntVector not() {
        return lanewise(NOT);
    }


    /// COMPARISONS

    /**
     * {@inheritDoc} <!--workaround-->
     */
    @Override
    @ForceInline
    public final
    VectorMask<Integer> eq(Vector<Integer> v) {
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
     * @see #compare(VectorOperators.Comparison,int)
     */
    @ForceInline
    public final
    VectorMask<Integer> eq(int e) {
        return compare(EQ, e);
    }

    /**
     * {@inheritDoc} <!--workaround-->
     */
    @Override
    @ForceInline
    public final
    VectorMask<Integer> lt(Vector<Integer> v) {
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
     * @see #compare(VectorOperators.Comparison,int)
     */
    @ForceInline
    public final
    VectorMask<Integer> lt(int e) {
        return compare(LT, e);
    }

    /**
     * {@inheritDoc} <!--workaround-->
     */
    @Override
    public abstract
    VectorMask<Integer> test(VectorOperators.Test op);

    /*package-private*/
    @ForceInline
    final
    <M extends VectorMask<Integer>>
    M testTemplate(Class<M> maskType, Test op) {
        IntSpecies vsp = vspecies();
        if (opKind(op, VO_SPECIAL)) {
            IntVector bits = this.viewAsIntegralLanes();
            VectorMask<Integer> m;
            if (op == IS_DEFAULT) {
                m = bits.compare(EQ, (int) 0);
            } else if (op == IS_NEGATIVE) {
                m = bits.compare(LT, (int) 0);
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
    VectorMask<Integer> test(VectorOperators.Test op,
                                  VectorMask<Integer> m) {
        return test(op).and(m);
    }

    /**
     * {@inheritDoc} <!--workaround-->
     */
    @Override
    public abstract
    VectorMask<Integer> compare(VectorOperators.Comparison op, Vector<Integer> v);

    /*package-private*/
    @ForceInline
    final
    <M extends VectorMask<Integer>>
    M compareTemplate(Class<M> maskType, Comparison op, Vector<Integer> v) {
        Objects.requireNonNull(v);
        IntSpecies vsp = vspecies();
        IntVector that = (IntVector) v;
        that.check(this);
        int opc = opCode(op);
        return VectorSupport.compare(
            opc, getClass(), maskType, int.class, length(),
            this, that,
            (cond, v0, v1) -> {
                AbstractMask<Integer> m
                    = v0.bTest(cond, v1, (cond_, i, a, b)
                               -> compareWithOp(cond, a, b));
                @SuppressWarnings("unchecked")
                M m2 = (M) m;
                return m2;
            });
    }

    @ForceInline
    private static boolean compareWithOp(int cond, int a, int b) {
        return switch (cond) {
            case BT_eq -> a == b;
            case BT_ne -> a != b;
            case BT_lt -> a < b;
            case BT_le -> a <= b;
            case BT_gt -> a > b;
            case BT_ge -> a >= b;
            case BT_ult -> Integer.compareUnsigned(a, b) < 0;
            case BT_ule -> Integer.compareUnsigned(a, b) <= 0;
            case BT_ugt -> Integer.compareUnsigned(a, b) > 0;
            case BT_uge -> Integer.compareUnsigned(a, b) >= 0;
            default -> throw new AssertionError();
        };
    }

    /**
     * {@inheritDoc} <!--workaround-->
     */
    @Override
    @ForceInline
    public final
    VectorMask<Integer> compare(VectorOperators.Comparison op,
                                  Vector<Integer> v,
                                  VectorMask<Integer> m) {
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
     * @see IntVector#compare(VectorOperators.Comparison,Vector)
     * @see #eq(int)
     * @see #lt(int)
     */
    public abstract
    VectorMask<Integer> compare(Comparison op, int e);

    /*package-private*/
    @ForceInline
    final
    <M extends VectorMask<Integer>>
    M compareTemplate(Class<M> maskType, Comparison op, int e) {
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
     * @see IntVector#compare(VectorOperators.Comparison,Vector,VectorMask)
     */
    @ForceInline
    public final VectorMask<Integer> compare(VectorOperators.Comparison op,
                                               int e,
                                               VectorMask<Integer> m) {
        return compare(op, e).and(m);
    }

    /**
     * {@inheritDoc} <!--workaround-->
     */
    @Override
    public abstract
    VectorMask<Integer> compare(Comparison op, long e);

    /*package-private*/
    @ForceInline
    final
    <M extends VectorMask<Integer>>
    M compareTemplate(Class<M> maskType, Comparison op, long e) {
        return compareTemplate(maskType, op, broadcast(e));
    }

    /**
     * {@inheritDoc} <!--workaround-->
     */
    @Override
    @ForceInline
    public final
    VectorMask<Integer> compare(Comparison op, long e, VectorMask<Integer> m) {
        return compare(op, broadcast(e), m);
    }



    /**
     * {@inheritDoc} <!--workaround-->
     */
    @Override public abstract
    IntVector blend(Vector<Integer> v, VectorMask<Integer> m);

    /*package-private*/
    @ForceInline
    final
    <M extends VectorMask<Integer>>
    IntVector
    blendTemplate(Class<M> maskType, IntVector v, M m) {
        v.check(this);
        return VectorSupport.blend(
            getClass(), maskType, int.class, length(),
            this, v, m,
            (v0, v1, m_) -> v0.bOp(v1, m_, (i, a, b) -> b));
    }

    /**
     * {@inheritDoc} <!--workaround-->
     */
    @Override public abstract IntVector addIndex(int scale);

    /*package-private*/
    @ForceInline
    final IntVector addIndexTemplate(int scale) {
        IntSpecies vsp = vspecies();
        // make sure VLENGTH*scale doesn't overflow:
        vsp.checkScale(scale);
        return VectorSupport.indexVector(
            getClass(), int.class, length(),
            this, scale, vsp,
            (v, scale_, s)
            -> {
                // If the platform doesn't support an INDEX
                // instruction directly, load IOTA from memory
                // and multiply.
                IntVector iota = s.iota();
                int sc = (int) scale_;
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
    public final IntVector blend(int e,
                                            VectorMask<Integer> m) {
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
    public final IntVector blend(long e,
                                            VectorMask<Integer> m) {
        return blend(broadcast(e), m);
    }

    /**
     * {@inheritDoc} <!--workaround-->
     */
    @Override
    public abstract
    IntVector slice(int origin, Vector<Integer> v1);

    /*package-private*/
    final
    @ForceInline
    IntVector sliceTemplate(int origin, Vector<Integer> v1) {
        IntVector that = (IntVector) v1;
        that.check(this);
        Objects.checkIndex(origin, length() + 1);
        VectorShuffle<Integer> iota = iotaShuffle();
        VectorMask<Integer> blendMask = iota.toVector().compare(VectorOperators.LT, (broadcast((int)(length() - origin))));
        iota = iotaShuffle(origin, 1, true);
        return that.rearrange(iota).blend(this.rearrange(iota), blendMask);
    }

    /**
     * {@inheritDoc} <!--workaround-->
     */
    @Override
    @ForceInline
    public final
    IntVector slice(int origin,
                               Vector<Integer> w,
                               VectorMask<Integer> m) {
        return broadcast(0).blend(slice(origin, w), m);
    }

    /**
     * {@inheritDoc} <!--workaround-->
     */
    @Override
    public abstract
    IntVector slice(int origin);

    /*package-private*/
    final
    @ForceInline
    IntVector sliceTemplate(int origin) {
        Objects.checkIndex(origin, length() + 1);
        VectorShuffle<Integer> iota = iotaShuffle();
        VectorMask<Integer> blendMask = iota.toVector().compare(VectorOperators.LT, (broadcast((int)(length() - origin))));
        iota = iotaShuffle(origin, 1, true);
        return vspecies().zero().blend(this.rearrange(iota), blendMask);
    }

    /**
     * {@inheritDoc} <!--workaround-->
     */
    @Override
    public abstract
    IntVector unslice(int origin, Vector<Integer> w, int part);

    /*package-private*/
    final
    @ForceInline
    IntVector
    unsliceTemplate(int origin, Vector<Integer> w, int part) {
        IntVector that = (IntVector) w;
        that.check(this);
        Objects.checkIndex(origin, length() + 1);
        VectorShuffle<Integer> iota = iotaShuffle();
        VectorMask<Integer> blendMask = iota.toVector().compare((part == 0) ? VectorOperators.GE : VectorOperators.LT,
                                                                  (broadcast((int)(origin))));
        iota = iotaShuffle(-origin, 1, true);
        return that.blend(this.rearrange(iota), blendMask);
    }

    /*package-private*/
    final
    @ForceInline
    <M extends VectorMask<Integer>>
    IntVector
    unsliceTemplate(Class<M> maskType, int origin, Vector<Integer> w, int part, M m) {
        IntVector that = (IntVector) w;
        that.check(this);
        IntVector slice = that.sliceTemplate(origin, that);
        slice = slice.blendTemplate(maskType, this, m);
        return slice.unsliceTemplate(origin, w, part);
    }

    /**
     * {@inheritDoc} <!--workaround-->
     */
    @Override
    public abstract
    IntVector unslice(int origin, Vector<Integer> w, int part, VectorMask<Integer> m);

    /**
     * {@inheritDoc} <!--workaround-->
     */
    @Override
    public abstract
    IntVector unslice(int origin);

    /*package-private*/
    final
    @ForceInline
    IntVector
    unsliceTemplate(int origin) {
        Objects.checkIndex(origin, length() + 1);
        VectorShuffle<Integer> iota = iotaShuffle();
        VectorMask<Integer> blendMask = iota.toVector().compare(VectorOperators.GE,
                                                                  (broadcast((int)(origin))));
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
    IntVector rearrange(VectorShuffle<Integer> m);

    /*package-private*/
    @ForceInline
    final
    <S extends VectorShuffle<Integer>>
    IntVector rearrangeTemplate(Class<S> shuffletype, S shuffle) {
        shuffle.checkIndexes();
        return VectorSupport.rearrangeOp(
            getClass(), shuffletype, int.class, length(),
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
    IntVector rearrange(VectorShuffle<Integer> s,
                                   VectorMask<Integer> m);

    /*package-private*/
    @ForceInline
    final
    <S extends VectorShuffle<Integer>>
    IntVector rearrangeTemplate(Class<S> shuffletype,
                                           S shuffle,
                                           VectorMask<Integer> m) {
        IntVector unmasked =
            VectorSupport.rearrangeOp(
                getClass(), shuffletype, int.class, length(),
                this, shuffle,
                (v1, s_) -> v1.uOp((i, a) -> {
                    int ei = s_.laneSource(i);
                    return ei < 0 ? 0 : v1.lane(ei);
                }));
        VectorMask<Integer> valid = shuffle.laneIsValid();
        if (m.andNot(valid).anyTrue()) {
            shuffle.checkIndexes();
            throw new AssertionError();
        }
        return broadcast((int)0).blend(unmasked, m);
    }

    /**
     * {@inheritDoc} <!--workaround-->
     */
    @Override
    public abstract
    IntVector rearrange(VectorShuffle<Integer> s,
                                   Vector<Integer> v);

    /*package-private*/
    @ForceInline
    final
    <S extends VectorShuffle<Integer>>
    IntVector rearrangeTemplate(Class<S> shuffletype,
                                           S shuffle,
                                           IntVector v) {
        VectorMask<Integer> valid = shuffle.laneIsValid();
        @SuppressWarnings("unchecked")
        S ws = (S) shuffle.wrapIndexes();
        IntVector r0 =
            VectorSupport.rearrangeOp(
                getClass(), shuffletype, int.class, length(),
                this, ws,
                (v0, s_) -> v0.uOp((i, a) -> {
                    int ei = s_.laneSource(i);
                    return v0.lane(ei);
                }));
        IntVector r1 =
            VectorSupport.rearrangeOp(
                getClass(), shuffletype, int.class, length(),
                v, ws,
                (v1, s_) -> v1.uOp((i, a) -> {
                    int ei = s_.laneSource(i);
                    return v1.lane(ei);
                }));
        return r1.blend(r0, valid);
    }

    @ForceInline
    private final
    VectorShuffle<Integer> toShuffle0(IntSpecies dsp) {
        int[] a = toArray();
        int[] sa = new int[a.length];
        for (int i = 0; i < a.length; i++) {
            sa[i] = (int) a[i];
        }
        return VectorShuffle.fromArray(dsp, sa, 0);
    }

    /*package-private*/
    @ForceInline
    final
    VectorShuffle<Integer> toShuffleTemplate(Class<?> shuffleType) {
        IntSpecies vsp = vspecies();
        return VectorSupport.convert(VectorSupport.VECTOR_OP_CAST,
                                     getClass(), int.class, length(),
                                     shuffleType, byte.class, length(),
                                     this, vsp,
                                     IntVector::toShuffle0);
    }

    /**
     * {@inheritDoc} <!--workaround-->
     */
    @Override
    public abstract
    IntVector selectFrom(Vector<Integer> v);

    /*package-private*/
    @ForceInline
    final IntVector selectFromTemplate(IntVector v) {
        return v.rearrange(this.toShuffle());
    }

    /**
     * {@inheritDoc} <!--workaround-->
     */
    @Override
    public abstract
    IntVector selectFrom(Vector<Integer> s, VectorMask<Integer> m);

    /*package-private*/
    @ForceInline
    final IntVector selectFromTemplate(IntVector v,
                                                  AbstractMask<Integer> m) {
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
     * @see #bitwiseBlend(int,int)
     * @see #bitwiseBlend(int,Vector)
     * @see #bitwiseBlend(Vector,int)
     * @see VectorOperators#BITWISE_BLEND
     * @see #lanewise(VectorOperators.Ternary,Vector,Vector,VectorMask)
     */
    @ForceInline
    public final
    IntVector bitwiseBlend(Vector<Integer> bits, Vector<Integer> mask) {
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
     * @see #lanewise(VectorOperators.Ternary,int,int,VectorMask)
     */
    @ForceInline
    public final
    IntVector bitwiseBlend(int bits, int mask) {
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
     * @see #lanewise(VectorOperators.Ternary,int,Vector,VectorMask)
     */
    @ForceInline
    public final
    IntVector bitwiseBlend(int bits, Vector<Integer> mask) {
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
     * @see #lanewise(VectorOperators.Ternary,Vector,int,VectorMask)
     */
    @ForceInline
    public final
    IntVector bitwiseBlend(Vector<Integer> bits, int mask) {
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
    public abstract int reduceLanes(VectorOperators.Associative op);

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
     * then the identity value is zero, the default {@code int} value.
     * <li>
     * If the operation is {@code MUL},
     * then the identity value is one.
     * <li>
     * If the operation is {@code AND},
     * then the identity value is minus one (all bits set).
     * <li>
     * If the operation is {@code MAX},
     * then the identity value is {@code Integer.MIN_VALUE}.
     * <li>
     * If the operation is {@code MIN},
     * then the identity value is {@code Integer.MAX_VALUE}.
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
    public abstract int reduceLanes(VectorOperators.Associative op,
                                       VectorMask<Integer> m);

    /*package-private*/
    @ForceInline
    final
    int reduceLanesTemplate(VectorOperators.Associative op,
                               VectorMask<Integer> m) {
        IntVector v = reduceIdentityVector(op).blend(this, m);
        return v.reduceLanesTemplate(op);
    }

    /*package-private*/
    @ForceInline
    final
    int reduceLanesTemplate(VectorOperators.Associative op) {
        if (op == FIRST_NONZERO) {
            // FIXME:  The JIT should handle this, and other scan ops alos.
            VectorMask<Integer> thisNZ
                = this.viewAsIntegralLanes().compare(NE, (int) 0);
            return this.lane(thisNZ.firstTrue());
        }
        int opc = opCode(op);
        return fromBits(VectorSupport.reductionCoerced(
            opc, getClass(), int.class, length(),
            this,
            REDUCE_IMPL.find(op, opc, (opc_) -> {
              switch (opc_) {
              case VECTOR_OP_ADD: return v ->
                      toBits(v.rOp((int)0, (i, a, b) -> (int)(a + b)));
              case VECTOR_OP_MUL: return v ->
                      toBits(v.rOp((int)1, (i, a, b) -> (int)(a * b)));
              case VECTOR_OP_MIN: return v ->
                      toBits(v.rOp(MAX_OR_INF, (i, a, b) -> (int) Math.min(a, b)));
              case VECTOR_OP_MAX: return v ->
                      toBits(v.rOp(MIN_OR_INF, (i, a, b) -> (int) Math.max(a, b)));
              case VECTOR_OP_AND: return v ->
                      toBits(v.rOp((int)-1, (i, a, b) -> (int)(a & b)));
              case VECTOR_OP_OR: return v ->
                      toBits(v.rOp((int)0, (i, a, b) -> (int)(a | b)));
              case VECTOR_OP_XOR: return v ->
                      toBits(v.rOp((int)0, (i, a, b) -> (int)(a ^ b)));
              default: return null;
              }})));
    }
    private static final
    ImplCache<Associative,Function<IntVector,Long>> REDUCE_IMPL
        = new ImplCache<>(Associative.class, IntVector.class);

    private
    @ForceInline
    IntVector reduceIdentityVector(VectorOperators.Associative op) {
        int opc = opCode(op);
        UnaryOperator<IntVector> fn
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
    ImplCache<Associative,UnaryOperator<IntVector>> REDUCE_ID_IMPL
        = new ImplCache<>(Associative.class, IntVector.class);

    private static final int MIN_OR_INF = Integer.MIN_VALUE;
    private static final int MAX_OR_INF = Integer.MAX_VALUE;

    public @Override abstract long reduceLanesToLong(VectorOperators.Associative op);
    public @Override abstract long reduceLanesToLong(VectorOperators.Associative op,
                                                     VectorMask<Integer> m);

    // Type specific accessors

    /**
     * Gets the lane element at lane index {@code i}
     *
     * @param i the lane index
     * @return the lane element at lane index {@code i}
     * @throws IllegalArgumentException if the index is is out of range
     * ({@code < 0 || >= length()})
     */
    public abstract int lane(int i);

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
    public abstract IntVector withLane(int i, int e);

    // Memory load operations

    /**
     * Returns an array of type {@code int[]}
     * containing all the lane values.
     * The array length is the same as the vector length.
     * The array elements are stored in lane order.
     * <p>
     * This method behaves as if it stores
     * this vector into an allocated array
     * (using {@link #intoArray(int[], int) intoArray})
     * and returns the array as follows:
     * <pre>{@code
     *   int[] a = new int[this.length()];
     *   this.intoArray(a, 0);
     *   return a;
     * }</pre>
     *
     * @return an array containing the lane values of this vector
     */
    @ForceInline
    @Override
    public final int[] toArray() {
        int[] a = new int[vspecies().laneCount()];
        intoArray(a, 0);
        return a;
    }

    /**
     * {@inheritDoc} <!--workaround-->
     * This is an alias for {@link #toArray()}
     * When this method is used on used on vectors
     * of type {@code IntVector},
     * there will be no loss of range or precision.
     */
    @ForceInline
    @Override
    public final int[] toIntArray() {
        return toArray();
    }

    /** {@inheritDoc} <!--workaround-->
     * @implNote
     * When this method is used on used on vectors
     * of type {@code IntVector},
     * there will be no loss of precision or range,
     * and so no {@code UnsupportedOperationException} will
     * be thrown.
     */
    @ForceInline
    @Override
    public final long[] toLongArray() {
        int[] a = toArray();
        long[] res = new long[a.length];
        for (int i = 0; i < a.length; i++) {
            int e = a[i];
            res[i] = IntSpecies.toIntegralChecked(e, false);
        }
        return res;
    }

    /** {@inheritDoc} <!--workaround-->
     * @implNote
     * When this method is used on used on vectors
     * of type {@code IntVector},
     * there will be no loss of precision.
     */
    @ForceInline
    @Override
    public final double[] toDoubleArray() {
        int[] a = toArray();
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
    IntVector fromByteArray(VectorSpecies<Integer> species,
                                       byte[] a, int offset,
                                       ByteOrder bo) {
        offset = checkFromIndexSize(offset, species.vectorByteSize(), a.length);
        IntSpecies vsp = (IntSpecies) species;
        return vsp.dummyVector().fromByteArray0(a, offset).maybeSwap(bo);
    }

    /**
     * Loads a vector from a byte array starting at an offset
     * and using a mask.
     * Lanes where the mask is unset are filled with the default
     * value of {@code int} (zero).
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
    IntVector fromByteArray(VectorSpecies<Integer> species,
                                       byte[] a, int offset,
                                       ByteOrder bo,
                                       VectorMask<Integer> m) {
        IntSpecies vsp = (IntSpecies) species;
        if (offset >= 0 && offset <= (a.length - species.vectorByteSize())) {
            IntVector zero = vsp.zero();
            IntVector v = zero.fromByteArray0(a, offset);
            return zero.blend(v.maybeSwap(bo), m);
        }

        // FIXME: optimize
        checkMaskFromIndexSize(offset, vsp, m, 4, a.length);
        ByteBuffer wb = wrapper(a, bo);
        return vsp.ldOp(wb, offset, (AbstractMask<Integer>)m,
                   (wb_, o, i)  -> wb_.getInt(o + i * 4));
    }

    /**
     * Loads a vector from an array of type {@code int[]}
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
    IntVector fromArray(VectorSpecies<Integer> species,
                                   int[] a, int offset) {
        offset = checkFromIndexSize(offset, species.length(), a.length);
        IntSpecies vsp = (IntSpecies) species;
        return vsp.dummyVector().fromArray0(a, offset);
    }

    /**
     * Loads a vector from an array of type {@code int[]}
     * starting at an offset and using a mask.
     * Lanes where the mask is unset are filled with the default
     * value of {@code int} (zero).
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
    IntVector fromArray(VectorSpecies<Integer> species,
                                   int[] a, int offset,
                                   VectorMask<Integer> m) {
        IntSpecies vsp = (IntSpecies) species;
        if (offset >= 0 && offset <= (a.length - species.length())) {
            IntVector zero = vsp.zero();
            return zero.blend(zero.fromArray0(a, offset), m);
        }

        // FIXME: optimize
        checkMaskFromIndexSize(offset, vsp, m, 1, a.length);
        return vsp.vOp(m, i -> a[offset + i]);
    }

    /**
     * Gathers a new vector composed of elements from an array of type
     * {@code int[]},
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
     * @see IntVector#toIntArray()
     */
    @ForceInline
    public static
    IntVector fromArray(VectorSpecies<Integer> species,
                                   int[] a, int offset,
                                   int[] indexMap, int mapOffset) {
        IntSpecies vsp = (IntSpecies) species;
        IntVector.IntSpecies isp = IntVector.species(vsp.indexShape());
        Objects.requireNonNull(a);
        Objects.requireNonNull(indexMap);
        Class<? extends IntVector> vectorType = vsp.vectorType();

        // Index vector: vix[0:n] = k -> offset + indexMap[mapOffset + k]
        IntVector vix = IntVector
            .fromArray(isp, indexMap, mapOffset)
            .add(offset);

        vix = VectorIntrinsics.checkIndex(vix, a.length);

        return VectorSupport.loadWithMap(
            vectorType, int.class, vsp.laneCount(),
            IntVector.species(vsp.indexShape()).vectorType(),
            a, ARRAY_BASE, vix,
            a, offset, indexMap, mapOffset, vsp,
            (int[] c, int idx, int[] iMap, int idy, IntSpecies s) ->
            s.vOp(n -> c[idx + iMap[idy+n]]));
        }

    /**
     * Gathers a new vector composed of elements from an array of type
     * {@code int[]},
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
     * @see IntVector#toIntArray()
     */
    @ForceInline
    public static
    IntVector fromArray(VectorSpecies<Integer> species,
                                   int[] a, int offset,
                                   int[] indexMap, int mapOffset,
                                   VectorMask<Integer> m) {
        if (m.allTrue()) {
            return fromArray(species, a, offset, indexMap, mapOffset);
        }
        else {
            // FIXME: Cannot vectorize yet, if there's a mask.
            IntSpecies vsp = (IntSpecies) species;
            return vsp.vOp(m, n -> a[offset + indexMap[mapOffset + n]]);
        }
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
     *         if {@code offset+N*4 < 0}
     *         or {@code offset+N*4 >= bb.limit()}
     *         for any lane {@code N} in the vector
     */
    @ForceInline
    public static
    IntVector fromByteBuffer(VectorSpecies<Integer> species,
                                        ByteBuffer bb, int offset,
                                        ByteOrder bo) {
        offset = checkFromIndexSize(offset, species.vectorByteSize(), bb.limit());
        IntSpecies vsp = (IntSpecies) species;
        return vsp.dummyVector().fromByteBuffer0(bb, offset).maybeSwap(bo);
    }

    /**
     * Loads a vector from a {@linkplain ByteBuffer byte buffer}
     * starting at an offset into the byte buffer
     * and using a mask.
     * Lanes where the mask is unset are filled with the default
     * value of {@code int} (zero).
     * Bytes are composed into primitive lane elements according
     * to the specified byte order.
     * The vector is arranged into lanes according to
     * <a href="Vector.html#lane-order">memory ordering</a>.
     * <p>
     * The following pseudocode illustrates the behavior:
     * <pre>{@code
     * IntBuffer eb = bb.duplicate()
     *     .position(offset)
     *     .order(bo).asIntBuffer();
     * int[] ar = new int[species.length()];
     * for (int n = 0; n < ar.length; n++) {
     *     if (m.laneIsSet(n)) {
     *         ar[n] = eb.get(n);
     *     }
     * }
     * IntVector r = IntVector.fromArray(species, ar, 0);
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
     *         if {@code offset+N*4 < 0}
     *         or {@code offset+N*4 >= bb.limit()}
     *         for any lane {@code N} in the vector
     *         where the mask is set
     */
    @ForceInline
    public static
    IntVector fromByteBuffer(VectorSpecies<Integer> species,
                                        ByteBuffer bb, int offset,
                                        ByteOrder bo,
                                        VectorMask<Integer> m) {
        IntSpecies vsp = (IntSpecies) species;
        if (offset >= 0 && offset <= (bb.limit() - species.vectorByteSize())) {
            IntVector zero = vsp.zero();
            IntVector v = zero.fromByteBuffer0(bb, offset);
            return zero.blend(v.maybeSwap(bo), m);
        }

        // FIXME: optimize
        checkMaskFromIndexSize(offset, vsp, m, 4, bb.limit());
        ByteBuffer wb = wrapper(bb, bo);
        return vsp.ldOp(wb, offset, (AbstractMask<Integer>)m,
                   (wb_, o, i)  -> wb_.getInt(o + i * 4));
    }

    // Memory store operations

    /**
     * Stores this vector into an array of type {@code int[]}
     * starting at an offset.
     * <p>
     * For each vector lane, where {@code N} is the vector lane index,
     * the lane element at index {@code N} is stored into the array
     * element {@code a[offset+N]}.
     *
     * @param a the array, of type {@code int[]}
     * @param offset the offset into the array
     * @throws IndexOutOfBoundsException
     *         if {@code offset+N < 0} or {@code offset+N >= a.length}
     *         for any lane {@code N} in the vector
     */
    @ForceInline
    public final
    void intoArray(int[] a, int offset) {
        offset = checkFromIndexSize(offset, length(), a.length);
        IntSpecies vsp = vspecies();
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
     * Stores this vector into an array of type {@code int[]}
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
     * @param a the array, of type {@code int[]}
     * @param offset the offset into the array
     * @param m the mask controlling lane storage
     * @throws IndexOutOfBoundsException
     *         if {@code offset+N < 0} or {@code offset+N >= a.length}
     *         for any lane {@code N} in the vector
     *         where the mask is set
     */
    @ForceInline
    public final
    void intoArray(int[] a, int offset,
                   VectorMask<Integer> m) {
        if (m.allTrue()) {
            intoArray(a, offset);
        } else {
            // FIXME: optimize
            IntSpecies vsp = vspecies();
            checkMaskFromIndexSize(offset, vsp, m, 1, a.length);
            stOp(a, offset, m, (arr, off, i, v) -> arr[off+i] = v);
        }
    }

    /**
     * Scatters this vector into an array of type {@code int[]}
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
     * @see IntVector#toIntArray()
     */
    @ForceInline
    public final
    void intoArray(int[] a, int offset,
                   int[] indexMap, int mapOffset) {
        IntSpecies vsp = vspecies();
        IntVector.IntSpecies isp = IntVector.species(vsp.indexShape());
        // Index vector: vix[0:n] = i -> offset + indexMap[mo + i]
        IntVector vix = IntVector
            .fromArray(isp, indexMap, mapOffset)
            .add(offset);

        vix = VectorIntrinsics.checkIndex(vix, a.length);

        VectorSupport.storeWithMap(
            vsp.vectorType(), vsp.elementType(), vsp.laneCount(),
            isp.vectorType(),
            a, arrayAddress(a, 0), vix,
            this,
            a, offset, indexMap, mapOffset,
            (arr, off, v, map, mo)
            -> v.stOp(arr, off,
                      (arr_, off_, i, e) -> {
                          int j = map[mo + i];
                          arr[off + j] = e;
                      }));
    }

    /**
     * Scatters this vector into an array of type {@code int[]},
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
     * @see IntVector#toIntArray()
     */
    @ForceInline
    public final
    void intoArray(int[] a, int offset,
                   int[] indexMap, int mapOffset,
                   VectorMask<Integer> m) {
        if (m.allTrue()) {
            intoArray(a, offset, indexMap, mapOffset);
        }
        else {
            // FIXME: Cannot vectorize yet, if there's a mask.
            stOp(a, offset, m,
                 (arr, off, i, e) -> {
                     int j = indexMap[mapOffset + i];
                     arr[off + j] = e;
                 });
        }
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
                       VectorMask<Integer> m) {
        if (m.allTrue()) {
            intoByteArray(a, offset, bo);
        } else {
            // FIXME: optimize
            IntSpecies vsp = vspecies();
            checkMaskFromIndexSize(offset, vsp, m, 4, a.length);
            ByteBuffer wb = wrapper(a, bo);
            this.stOp(wb, offset, m,
                    (wb_, o, i, e) -> wb_.putInt(o + i * 4, e));
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
                        VectorMask<Integer> m) {
        if (m.allTrue()) {
            intoByteBuffer(bb, offset, bo);
        } else {
            // FIXME: optimize
            if (bb.isReadOnly()) {
                throw new ReadOnlyBufferException();
            }
            IntSpecies vsp = vspecies();
            checkMaskFromIndexSize(offset, vsp, m, 4, bb.limit());
            ByteBuffer wb = wrapper(bb, bo);
            this.stOp(wb, offset, m,
                    (wb_, o, i, e) -> wb_.putInt(o + i * 4, e));
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
    IntVector fromArray0(int[] a, int offset);
    @ForceInline
    final
    IntVector fromArray0Template(int[] a, int offset) {
        IntSpecies vsp = vspecies();
        return VectorSupport.load(
            vsp.vectorType(), vsp.elementType(), vsp.laneCount(),
            a, arrayAddress(a, offset),
            a, offset, vsp,
            (arr, off, s) -> s.ldOp(arr, off,
                                    (arr_, off_, i) -> arr_[off_ + i]));
    }



    @Override
    abstract
    IntVector fromByteArray0(byte[] a, int offset);
    @ForceInline
    final
    IntVector fromByteArray0Template(byte[] a, int offset) {
        IntSpecies vsp = vspecies();
        return VectorSupport.load(
            vsp.vectorType(), vsp.elementType(), vsp.laneCount(),
            a, byteArrayAddress(a, offset),
            a, offset, vsp,
            (arr, off, s) -> {
                ByteBuffer wb = wrapper(arr, NATIVE_ENDIAN);
                return s.ldOp(wb, off,
                        (wb_, o, i) -> wb_.getInt(o + i * 4));
            });
    }

    abstract
    IntVector fromByteBuffer0(ByteBuffer bb, int offset);
    @ForceInline
    final
    IntVector fromByteBuffer0Template(ByteBuffer bb, int offset) {
        IntSpecies vsp = vspecies();
        return ScopedMemoryAccess.loadFromByteBuffer(
                vsp.vectorType(), vsp.elementType(), vsp.laneCount(),
                bb, offset, vsp,
                (buf, off, s) -> {
                    ByteBuffer wb = wrapper(buf, NATIVE_ENDIAN);
                    return s.ldOp(wb, off,
                            (wb_, o, i) -> wb_.getInt(o + i * 4));
                });
    }

    // Unchecked storing operations in native byte order.
    // Caller is responsible for applying index checks, masking, and
    // byte swapping.

    abstract
    void intoArray0(int[] a, int offset);
    @ForceInline
    final
    void intoArray0Template(int[] a, int offset) {
        IntSpecies vsp = vspecies();
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
        IntSpecies vsp = vspecies();
        VectorSupport.store(
            vsp.vectorType(), vsp.elementType(), vsp.laneCount(),
            a, byteArrayAddress(a, offset),
            this, a, offset,
            (arr, off, v) -> {
                ByteBuffer wb = wrapper(arr, NATIVE_ENDIAN);
                v.stOp(wb, off,
                        (tb_, o, i, e) -> tb_.putInt(o + i * 4, e));
            });
    }

    @ForceInline
    final
    void intoByteBuffer0(ByteBuffer bb, int offset) {
        IntSpecies vsp = vspecies();
        ScopedMemoryAccess.storeIntoByteBuffer(
                vsp.vectorType(), vsp.elementType(), vsp.laneCount(),
                this, bb, offset,
                (buf, off, v) -> {
                    ByteBuffer wb = wrapper(buf, NATIVE_ENDIAN);
                    v.stOp(wb, off,
                            (wb_, o, i, e) -> wb_.putInt(o + i * 4, e));
                });
    }

    // End of low-level memory operations.

    private static
    void checkMaskFromIndexSize(int offset,
                                IntSpecies vsp,
                                VectorMask<Integer> m,
                                int scale,
                                int limit) {
        ((AbstractMask<Integer>)m)
            .checkIndexByLane(offset, limit, vsp.iota(), scale);
    }

    @ForceInline
    private void conditionalStoreNYI(int offset,
                                     IntSpecies vsp,
                                     VectorMask<Integer> m,
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
    IntVector maybeSwap(ByteOrder bo) {
        if (bo != NATIVE_ENDIAN) {
            return this.reinterpretAsBytes()
                .rearrange(swapBytesShuffle())
                .reinterpretAsInts();
        }
        return this;
    }

    static final int ARRAY_SHIFT =
        31 - Integer.numberOfLeadingZeros(Unsafe.ARRAY_INT_INDEX_SCALE);
    static final long ARRAY_BASE =
        Unsafe.ARRAY_INT_BASE_OFFSET;

    @ForceInline
    static long arrayAddress(int[] a, int index) {
        return ARRAY_BASE + (((long)index) << ARRAY_SHIFT);
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
    public final IntVector viewAsIntegralLanes() {
        return this;
    }

    /**
     * {@inheritDoc} <!--workaround-->
     */
    @ForceInline
    @Override
    public final
    FloatVector
    viewAsFloatingLanes() {
        LaneType flt = LaneType.INT.asFloating();
        return (FloatVector) asVectorRaw(flt);
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
     * java.util.Arrays#toString(int[]) Arrays.toString()},
     * as appropriate to the {@code int} array returned by
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
     * Class representing {@link IntVector}'s of the same {@link VectorShape VectorShape}.
     */
    /*package-private*/
    static final class IntSpecies extends AbstractSpecies<Integer> {
        private IntSpecies(VectorShape shape,
                Class<? extends IntVector> vectorType,
                Class<? extends AbstractMask<Integer>> maskType,
                Function<Object, IntVector> vectorFactory) {
            super(shape, LaneType.of(int.class),
                  vectorType, maskType,
                  vectorFactory);
            assert(this.elementSize() == Integer.SIZE);
        }

        // Specializing overrides:

        @Override
        @ForceInline
        public final Class<Integer> elementType() {
            return int.class;
        }

        @Override
        @ForceInline
        final Class<Integer> genericElementType() {
            return Integer.class;
        }

        @SuppressWarnings("unchecked")
        @Override
        @ForceInline
        public final Class<? extends IntVector> vectorType() {
            return (Class<? extends IntVector>) vectorType;
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
        final IntVector broadcastBits(long bits) {
            return (IntVector)
                VectorSupport.broadcastCoerced(
                    vectorType, int.class, laneCount,
                    bits, this,
                    (bits_, s_) -> s_.rvOp(i -> bits_));
        }

        /*package-private*/
        @ForceInline
        final IntVector broadcast(int e) {
            return broadcastBits(toBits(e));
        }

        @Override
        @ForceInline
        public final IntVector broadcast(long e) {
            return broadcastBits(longToElementBits(e));
        }

        /*package-private*/
        final @Override
        @ForceInline
        long longToElementBits(long value) {
            // Do the conversion, and then test it for failure.
            int e = (int) value;
            if ((long) e != value) {
                throw badElementBits(value, e);
            }
            return toBits(e);
        }

        /*package-private*/
        @ForceInline
        static long toIntegralChecked(int e, boolean convertToInt) {
            long value = convertToInt ? (int) e : (long) e;
            if ((int) value != e) {
                throw badArrayBits(e, convertToInt, value);
            }
            return value;
        }

        /* this non-public one is for internal conversions */
        @Override
        @ForceInline
        final IntVector fromIntValues(int[] values) {
            VectorIntrinsics.requireLength(values.length, laneCount);
            int[] va = new int[laneCount()];
            for (int i = 0; i < va.length; i++) {
                int lv = values[i];
                int v = (int) lv;
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
        public IntVector fromArray(Object a, int offset) {
            // User entry point:  Be careful with inputs.
            return IntVector
                .fromArray(this, (int[]) a, offset);
        }

        @ForceInline
        @Override final
        IntVector dummyVector() {
            return (IntVector) super.dummyVector();
        }

        /*package-private*/
        final @Override
        @ForceInline
        IntVector rvOp(RVOp f) {
            int[] res = new int[laneCount()];
            for (int i = 0; i < res.length; i++) {
                int bits = (int) f.apply(i);
                res[i] = fromBits(bits);
            }
            return dummyVector().vectorFactory(res);
        }

        IntVector vOp(FVOp f) {
            int[] res = new int[laneCount()];
            for (int i = 0; i < res.length; i++) {
                res[i] = f.apply(i);
            }
            return dummyVector().vectorFactory(res);
        }

        IntVector vOp(VectorMask<Integer> m, FVOp f) {
            int[] res = new int[laneCount()];
            boolean[] mbits = ((AbstractMask<Integer>)m).getBits();
            for (int i = 0; i < res.length; i++) {
                if (mbits[i]) {
                    res[i] = f.apply(i);
                }
            }
            return dummyVector().vectorFactory(res);
        }

        /*package-private*/
        @ForceInline
        <M> IntVector ldOp(M memory, int offset,
                                      FLdOp<M> f) {
            return dummyVector().ldOp(memory, offset, f);
        }

        /*package-private*/
        @ForceInline
        <M> IntVector ldOp(M memory, int offset,
                                      AbstractMask<Integer> m,
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
                      AbstractMask<Integer> m,
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
        public final IntVector zero() {
            if ((Class<?>) vectorType() == IntMaxVector.class)
                return IntMaxVector.ZERO;
            switch (vectorBitSize()) {
                case 64: return Int64Vector.ZERO;
                case 128: return Int128Vector.ZERO;
                case 256: return Int256Vector.ZERO;
                case 512: return Int512Vector.ZERO;
            }
            throw new AssertionError();
        }

        @Override
        @ForceInline
        public final IntVector iota() {
            if ((Class<?>) vectorType() == IntMaxVector.class)
                return IntMaxVector.IOTA;
            switch (vectorBitSize()) {
                case 64: return Int64Vector.IOTA;
                case 128: return Int128Vector.IOTA;
                case 256: return Int256Vector.IOTA;
                case 512: return Int512Vector.IOTA;
            }
            throw new AssertionError();
        }

        // Mask access
        @Override
        @ForceInline
        public final VectorMask<Integer> maskAll(boolean bit) {
            if ((Class<?>) vectorType() == IntMaxVector.class)
                return IntMaxVector.IntMaxMask.maskAll(bit);
            switch (vectorBitSize()) {
                case 64: return Int64Vector.Int64Mask.maskAll(bit);
                case 128: return Int128Vector.Int128Mask.maskAll(bit);
                case 256: return Int256Vector.Int256Mask.maskAll(bit);
                case 512: return Int512Vector.Int512Mask.maskAll(bit);
            }
            throw new AssertionError();
        }
    }

    /**
     * Finds a species for an element type of {@code int} and shape.
     *
     * @param s the shape
     * @return a species for an element type of {@code int} and shape
     * @throws IllegalArgumentException if no such species exists for the shape
     */
    static IntSpecies species(VectorShape s) {
        Objects.requireNonNull(s);
        switch (s) {
            case S_64_BIT: return (IntSpecies) SPECIES_64;
            case S_128_BIT: return (IntSpecies) SPECIES_128;
            case S_256_BIT: return (IntSpecies) SPECIES_256;
            case S_512_BIT: return (IntSpecies) SPECIES_512;
            case S_Max_BIT: return (IntSpecies) SPECIES_MAX;
            default: throw new IllegalArgumentException("Bad shape: " + s);
        }
    }

    /** Species representing {@link IntVector}s of {@link VectorShape#S_64_BIT VectorShape.S_64_BIT}. */
    public static final VectorSpecies<Integer> SPECIES_64
        = new IntSpecies(VectorShape.S_64_BIT,
                            Int64Vector.class,
                            Int64Vector.Int64Mask.class,
                            Int64Vector::new);

    /** Species representing {@link IntVector}s of {@link VectorShape#S_128_BIT VectorShape.S_128_BIT}. */
    public static final VectorSpecies<Integer> SPECIES_128
        = new IntSpecies(VectorShape.S_128_BIT,
                            Int128Vector.class,
                            Int128Vector.Int128Mask.class,
                            Int128Vector::new);

    /** Species representing {@link IntVector}s of {@link VectorShape#S_256_BIT VectorShape.S_256_BIT}. */
    public static final VectorSpecies<Integer> SPECIES_256
        = new IntSpecies(VectorShape.S_256_BIT,
                            Int256Vector.class,
                            Int256Vector.Int256Mask.class,
                            Int256Vector::new);

    /** Species representing {@link IntVector}s of {@link VectorShape#S_512_BIT VectorShape.S_512_BIT}. */
    public static final VectorSpecies<Integer> SPECIES_512
        = new IntSpecies(VectorShape.S_512_BIT,
                            Int512Vector.class,
                            Int512Vector.Int512Mask.class,
                            Int512Vector::new);

    /** Species representing {@link IntVector}s of {@link VectorShape#S_Max_BIT VectorShape.S_Max_BIT}. */
    public static final VectorSpecies<Integer> SPECIES_MAX
        = new IntSpecies(VectorShape.S_Max_BIT,
                            IntMaxVector.class,
                            IntMaxVector.IntMaxMask.class,
                            IntMaxVector::new);

    /**
     * Preferred species for {@link IntVector}s.
     * A preferred species is a species of maximal bit-size for the platform.
     */
    public static final VectorSpecies<Integer> SPECIES_PREFERRED
        = (IntSpecies) VectorSpecies.ofPreferred(int.class);
}
