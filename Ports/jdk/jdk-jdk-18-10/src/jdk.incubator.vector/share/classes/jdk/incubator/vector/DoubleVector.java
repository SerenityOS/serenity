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
 * {@code double} values.
 */
@SuppressWarnings("cast")  // warning: redundant cast
public abstract class DoubleVector extends AbstractVector<Double> {

    DoubleVector(double[] vec) {
        super(vec);
    }

    static final int FORBID_OPCODE_KIND = VO_NOFP;

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
    abstract double[] vec();

    // Virtualized constructors

    /**
     * Build a vector directly using my own constructor.
     * It is an error if the array is aliased elsewhere.
     */
    /*package-private*/
    abstract DoubleVector vectorFactory(double[] vec);

    /**
     * Build a mask directly using my species.
     * It is an error if the array is aliased elsewhere.
     */
    /*package-private*/
    @ForceInline
    final
    AbstractMask<Double> maskFactory(boolean[] bits) {
        return vspecies().maskFactory(bits);
    }

    // Constant loader (takes dummy as vector arg)
    interface FVOp {
        double apply(int i);
    }

    /*package-private*/
    @ForceInline
    final
    DoubleVector vOp(FVOp f) {
        double[] res = new double[length()];
        for (int i = 0; i < res.length; i++) {
            res[i] = f.apply(i);
        }
        return vectorFactory(res);
    }

    @ForceInline
    final
    DoubleVector vOp(VectorMask<Double> m, FVOp f) {
        double[] res = new double[length()];
        boolean[] mbits = ((AbstractMask<Double>)m).getBits();
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
        double apply(int i, double a);
    }

    /*package-private*/
    abstract
    DoubleVector uOp(FUnOp f);
    @ForceInline
    final
    DoubleVector uOpTemplate(FUnOp f) {
        double[] vec = vec();
        double[] res = new double[length()];
        for (int i = 0; i < res.length; i++) {
            res[i] = f.apply(i, vec[i]);
        }
        return vectorFactory(res);
    }

    /*package-private*/
    abstract
    DoubleVector uOp(VectorMask<Double> m,
                             FUnOp f);
    @ForceInline
    final
    DoubleVector uOpTemplate(VectorMask<Double> m,
                                     FUnOp f) {
        double[] vec = vec();
        double[] res = new double[length()];
        boolean[] mbits = ((AbstractMask<Double>)m).getBits();
        for (int i = 0; i < res.length; i++) {
            res[i] = mbits[i] ? f.apply(i, vec[i]) : vec[i];
        }
        return vectorFactory(res);
    }

    // Binary operator

    /*package-private*/
    interface FBinOp {
        double apply(int i, double a, double b);
    }

    /*package-private*/
    abstract
    DoubleVector bOp(Vector<Double> o,
                             FBinOp f);
    @ForceInline
    final
    DoubleVector bOpTemplate(Vector<Double> o,
                                     FBinOp f) {
        double[] res = new double[length()];
        double[] vec1 = this.vec();
        double[] vec2 = ((DoubleVector)o).vec();
        for (int i = 0; i < res.length; i++) {
            res[i] = f.apply(i, vec1[i], vec2[i]);
        }
        return vectorFactory(res);
    }

    /*package-private*/
    abstract
    DoubleVector bOp(Vector<Double> o,
                             VectorMask<Double> m,
                             FBinOp f);
    @ForceInline
    final
    DoubleVector bOpTemplate(Vector<Double> o,
                                     VectorMask<Double> m,
                                     FBinOp f) {
        double[] res = new double[length()];
        double[] vec1 = this.vec();
        double[] vec2 = ((DoubleVector)o).vec();
        boolean[] mbits = ((AbstractMask<Double>)m).getBits();
        for (int i = 0; i < res.length; i++) {
            res[i] = mbits[i] ? f.apply(i, vec1[i], vec2[i]) : vec1[i];
        }
        return vectorFactory(res);
    }

    // Ternary operator

    /*package-private*/
    interface FTriOp {
        double apply(int i, double a, double b, double c);
    }

    /*package-private*/
    abstract
    DoubleVector tOp(Vector<Double> o1,
                             Vector<Double> o2,
                             FTriOp f);
    @ForceInline
    final
    DoubleVector tOpTemplate(Vector<Double> o1,
                                     Vector<Double> o2,
                                     FTriOp f) {
        double[] res = new double[length()];
        double[] vec1 = this.vec();
        double[] vec2 = ((DoubleVector)o1).vec();
        double[] vec3 = ((DoubleVector)o2).vec();
        for (int i = 0; i < res.length; i++) {
            res[i] = f.apply(i, vec1[i], vec2[i], vec3[i]);
        }
        return vectorFactory(res);
    }

    /*package-private*/
    abstract
    DoubleVector tOp(Vector<Double> o1,
                             Vector<Double> o2,
                             VectorMask<Double> m,
                             FTriOp f);
    @ForceInline
    final
    DoubleVector tOpTemplate(Vector<Double> o1,
                                     Vector<Double> o2,
                                     VectorMask<Double> m,
                                     FTriOp f) {
        double[] res = new double[length()];
        double[] vec1 = this.vec();
        double[] vec2 = ((DoubleVector)o1).vec();
        double[] vec3 = ((DoubleVector)o2).vec();
        boolean[] mbits = ((AbstractMask<Double>)m).getBits();
        for (int i = 0; i < res.length; i++) {
            res[i] = mbits[i] ? f.apply(i, vec1[i], vec2[i], vec3[i]) : vec1[i];
        }
        return vectorFactory(res);
    }

    // Reduction operator

    /*package-private*/
    abstract
    double rOp(double v, FBinOp f);
    @ForceInline
    final
    double rOpTemplate(double v, FBinOp f) {
        double[] vec = vec();
        for (int i = 0; i < vec.length; i++) {
            v = f.apply(i, v, vec[i]);
        }
        return v;
    }

    // Memory reference

    /*package-private*/
    interface FLdOp<M> {
        double apply(M memory, int offset, int i);
    }

    /*package-private*/
    @ForceInline
    final
    <M> DoubleVector ldOp(M memory, int offset,
                                  FLdOp<M> f) {
        //dummy; no vec = vec();
        double[] res = new double[length()];
        for (int i = 0; i < res.length; i++) {
            res[i] = f.apply(memory, offset, i);
        }
        return vectorFactory(res);
    }

    /*package-private*/
    @ForceInline
    final
    <M> DoubleVector ldOp(M memory, int offset,
                                  VectorMask<Double> m,
                                  FLdOp<M> f) {
        //double[] vec = vec();
        double[] res = new double[length()];
        boolean[] mbits = ((AbstractMask<Double>)m).getBits();
        for (int i = 0; i < res.length; i++) {
            if (mbits[i]) {
                res[i] = f.apply(memory, offset, i);
            }
        }
        return vectorFactory(res);
    }

    interface FStOp<M> {
        void apply(M memory, int offset, int i, double a);
    }

    /*package-private*/
    @ForceInline
    final
    <M> void stOp(M memory, int offset,
                  FStOp<M> f) {
        double[] vec = vec();
        for (int i = 0; i < vec.length; i++) {
            f.apply(memory, offset, i, vec[i]);
        }
    }

    /*package-private*/
    @ForceInline
    final
    <M> void stOp(M memory, int offset,
                  VectorMask<Double> m,
                  FStOp<M> f) {
        double[] vec = vec();
        boolean[] mbits = ((AbstractMask<Double>)m).getBits();
        for (int i = 0; i < vec.length; i++) {
            if (mbits[i]) {
                f.apply(memory, offset, i, vec[i]);
            }
        }
    }

    // Binary test

    /*package-private*/
    interface FBinTest {
        boolean apply(int cond, int i, double a, double b);
    }

    /*package-private*/
    @ForceInline
    final
    AbstractMask<Double> bTest(int cond,
                                  Vector<Double> o,
                                  FBinTest f) {
        double[] vec1 = vec();
        double[] vec2 = ((DoubleVector)o).vec();
        boolean[] bits = new boolean[length()];
        for (int i = 0; i < length(); i++){
            bits[i] = f.apply(cond, i, vec1[i], vec2[i]);
        }
        return maskFactory(bits);
    }

    /*package-private*/
    @Override
    abstract DoubleSpecies vspecies();

    /*package-private*/
    @ForceInline
    static long toBits(double e) {
        return  Double.doubleToRawLongBits(e);
    }

    /*package-private*/
    @ForceInline
    static double fromBits(long bits) {
        return Double.longBitsToDouble((long)bits);
    }

    // Static factories (other than memory operations)

    // Note: A surprising behavior in javadoc
    // sometimes makes a lone /** {@inheritDoc} */
    // comment drop the method altogether,
    // apparently if the method mentions an
    // parameter or return type of Vector<Double>
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
    public static DoubleVector zero(VectorSpecies<Double> species) {
        DoubleSpecies vsp = (DoubleSpecies) species;
        return VectorSupport.broadcastCoerced(vsp.vectorType(), double.class, species.length(),
                        toBits(0.0f), vsp,
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
     * {@code DoubleVector.broadcast(this.species(), e)}.
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
    public abstract DoubleVector broadcast(double e);

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
    public static DoubleVector broadcast(VectorSpecies<Double> species, double e) {
        DoubleSpecies vsp = (DoubleSpecies) species;
        return vsp.broadcast(e);
    }

    /*package-private*/
    @ForceInline
    final DoubleVector broadcastTemplate(double e) {
        DoubleSpecies vsp = vspecies();
        return vsp.broadcast(e);
    }

    /**
     * {@inheritDoc} <!--workaround-->
     * @apiNote
     * When working with vector subtypes like {@code DoubleVector},
     * {@linkplain #broadcast(double) the more strongly typed method}
     * is typically selected.  It can be explicitly selected
     * using a cast: {@code v.broadcast((double)e)}.
     * The two expressions will produce numerically identical results.
     */
    @Override
    public abstract DoubleVector broadcast(long e);

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
     * @see #broadcast(VectorSpecies,double)
     * @see VectorSpecies#checkValue(long)
     */
    @ForceInline
    public static DoubleVector broadcast(VectorSpecies<Double> species, long e) {
        DoubleSpecies vsp = (DoubleSpecies) species;
        return vsp.broadcast(e);
    }

    /*package-private*/
    @ForceInline
    final DoubleVector broadcastTemplate(long e) {
        return vspecies().broadcast(e);
    }

    // Unary lanewise support

    /**
     * {@inheritDoc} <!--workaround-->
     */
    public abstract
    DoubleVector lanewise(VectorOperators.Unary op);

    @ForceInline
    final
    DoubleVector lanewiseTemplate(VectorOperators.Unary op) {
        if (opKind(op, VO_SPECIAL)) {
            if (op == ZOMO) {
                return blend(broadcast(-1), compare(NE, 0));
            }
        }
        int opc = opCode(op);
        return VectorSupport.unaryOp(
            opc, getClass(), double.class, length(),
            this,
            UN_IMPL.find(op, opc, (opc_) -> {
              switch (opc_) {
                case VECTOR_OP_NEG: return v0 ->
                        v0.uOp((i, a) -> (double) -a);
                case VECTOR_OP_ABS: return v0 ->
                        v0.uOp((i, a) -> (double) Math.abs(a));
                case VECTOR_OP_SIN: return v0 ->
                        v0.uOp((i, a) -> (double) Math.sin(a));
                case VECTOR_OP_COS: return v0 ->
                        v0.uOp((i, a) -> (double) Math.cos(a));
                case VECTOR_OP_TAN: return v0 ->
                        v0.uOp((i, a) -> (double) Math.tan(a));
                case VECTOR_OP_ASIN: return v0 ->
                        v0.uOp((i, a) -> (double) Math.asin(a));
                case VECTOR_OP_ACOS: return v0 ->
                        v0.uOp((i, a) -> (double) Math.acos(a));
                case VECTOR_OP_ATAN: return v0 ->
                        v0.uOp((i, a) -> (double) Math.atan(a));
                case VECTOR_OP_EXP: return v0 ->
                        v0.uOp((i, a) -> (double) Math.exp(a));
                case VECTOR_OP_LOG: return v0 ->
                        v0.uOp((i, a) -> (double) Math.log(a));
                case VECTOR_OP_LOG10: return v0 ->
                        v0.uOp((i, a) -> (double) Math.log10(a));
                case VECTOR_OP_SQRT: return v0 ->
                        v0.uOp((i, a) -> (double) Math.sqrt(a));
                case VECTOR_OP_CBRT: return v0 ->
                        v0.uOp((i, a) -> (double) Math.cbrt(a));
                case VECTOR_OP_SINH: return v0 ->
                        v0.uOp((i, a) -> (double) Math.sinh(a));
                case VECTOR_OP_COSH: return v0 ->
                        v0.uOp((i, a) -> (double) Math.cosh(a));
                case VECTOR_OP_TANH: return v0 ->
                        v0.uOp((i, a) -> (double) Math.tanh(a));
                case VECTOR_OP_EXPM1: return v0 ->
                        v0.uOp((i, a) -> (double) Math.expm1(a));
                case VECTOR_OP_LOG1P: return v0 ->
                        v0.uOp((i, a) -> (double) Math.log1p(a));
                default: return null;
              }}));
    }
    private static final
    ImplCache<Unary,UnaryOperator<DoubleVector>> UN_IMPL
        = new ImplCache<>(Unary.class, DoubleVector.class);

    /**
     * {@inheritDoc} <!--workaround-->
     */
    @ForceInline
    public final
    DoubleVector lanewise(VectorOperators.Unary op,
                                  VectorMask<Double> m) {
        return blend(lanewise(op), m);
    }

    // Binary lanewise support

    /**
     * {@inheritDoc} <!--workaround-->
     * @see #lanewise(VectorOperators.Binary,double)
     * @see #lanewise(VectorOperators.Binary,double,VectorMask)
     */
    @Override
    public abstract
    DoubleVector lanewise(VectorOperators.Binary op,
                                  Vector<Double> v);
    @ForceInline
    final
    DoubleVector lanewiseTemplate(VectorOperators.Binary op,
                                          Vector<Double> v) {
        DoubleVector that = (DoubleVector) v;
        that.check(this);
        if (opKind(op, VO_SPECIAL )) {
            if (op == FIRST_NONZERO) {
                // FIXME: Support this in the JIT.
                VectorMask<Long> thisNZ
                    = this.viewAsIntegralLanes().compare(NE, (long) 0);
                that = that.blend((double) 0, thisNZ.cast(vspecies()));
                op = OR_UNCHECKED;
                // FIXME: Support OR_UNCHECKED on float/double also!
                return this.viewAsIntegralLanes()
                    .lanewise(op, that.viewAsIntegralLanes())
                    .viewAsFloatingLanes();
            }
        }
        int opc = opCode(op);
        return VectorSupport.binaryOp(
            opc, getClass(), double.class, length(),
            this, that,
            BIN_IMPL.find(op, opc, (opc_) -> {
              switch (opc_) {
                case VECTOR_OP_ADD: return (v0, v1) ->
                        v0.bOp(v1, (i, a, b) -> (double)(a + b));
                case VECTOR_OP_SUB: return (v0, v1) ->
                        v0.bOp(v1, (i, a, b) -> (double)(a - b));
                case VECTOR_OP_MUL: return (v0, v1) ->
                        v0.bOp(v1, (i, a, b) -> (double)(a * b));
                case VECTOR_OP_DIV: return (v0, v1) ->
                        v0.bOp(v1, (i, a, b) -> (double)(a / b));
                case VECTOR_OP_MAX: return (v0, v1) ->
                        v0.bOp(v1, (i, a, b) -> (double)Math.max(a, b));
                case VECTOR_OP_MIN: return (v0, v1) ->
                        v0.bOp(v1, (i, a, b) -> (double)Math.min(a, b));
                case VECTOR_OP_ATAN2: return (v0, v1) ->
                        v0.bOp(v1, (i, a, b) -> (double) Math.atan2(a, b));
                case VECTOR_OP_POW: return (v0, v1) ->
                        v0.bOp(v1, (i, a, b) -> (double) Math.pow(a, b));
                case VECTOR_OP_HYPOT: return (v0, v1) ->
                        v0.bOp(v1, (i, a, b) -> (double) Math.hypot(a, b));
                default: return null;
                }}));
    }
    private static final
    ImplCache<Binary,BinaryOperator<DoubleVector>> BIN_IMPL
        = new ImplCache<>(Binary.class, DoubleVector.class);

    /**
     * {@inheritDoc} <!--workaround-->
     * @see #lanewise(VectorOperators.Binary,double,VectorMask)
     */
    @ForceInline
    public final
    DoubleVector lanewise(VectorOperators.Binary op,
                                  Vector<Double> v,
                                  VectorMask<Double> m) {
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
     * @see #lanewise(VectorOperators.Binary,double,VectorMask)
     */
    @ForceInline
    public final
    DoubleVector lanewise(VectorOperators.Binary op,
                                  double e) {
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
     * @see #lanewise(VectorOperators.Binary,double)
     */
    @ForceInline
    public final
    DoubleVector lanewise(VectorOperators.Binary op,
                                  double e,
                                  VectorMask<Double> m) {
        return blend(lanewise(op, e), m);
    }

    /**
     * {@inheritDoc} <!--workaround-->
     * @apiNote
     * When working with vector subtypes like {@code DoubleVector},
     * {@linkplain #lanewise(VectorOperators.Binary,double)
     * the more strongly typed method}
     * is typically selected.  It can be explicitly selected
     * using a cast: {@code v.lanewise(op,(double)e)}.
     * The two expressions will produce numerically identical results.
     */
    @ForceInline
    public final
    DoubleVector lanewise(VectorOperators.Binary op,
                                  long e) {
        double e1 = (double) e;
        if ((long)e1 != e
            ) {
            vspecies().checkValue(e);  // for exception
        }
        return lanewise(op, e1);
    }

    /**
     * {@inheritDoc} <!--workaround-->
     * @apiNote
     * When working with vector subtypes like {@code DoubleVector},
     * {@linkplain #lanewise(VectorOperators.Binary,double,VectorMask)
     * the more strongly typed method}
     * is typically selected.  It can be explicitly selected
     * using a cast: {@code v.lanewise(op,(double)e,m)}.
     * The two expressions will produce numerically identical results.
     */
    @ForceInline
    public final
    DoubleVector lanewise(VectorOperators.Binary op,
                                  long e, VectorMask<Double> m) {
        return blend(lanewise(op, e), m);
    }


    // Ternary lanewise support

    // Ternary operators come in eight variations:
    //   lanewise(op, [broadcast(e1)|v1], [broadcast(e2)|v2])
    //   lanewise(op, [broadcast(e1)|v1], [broadcast(e2)|v2], mask)

    // It is annoying to support all of these variations of masking
    // and broadcast, but it would be more surprising not to continue
    // the obvious pattern started by unary and binary.

   /**
     * {@inheritDoc} <!--workaround-->
     * @see #lanewise(VectorOperators.Ternary,double,double,VectorMask)
     * @see #lanewise(VectorOperators.Ternary,Vector,double,VectorMask)
     * @see #lanewise(VectorOperators.Ternary,double,Vector,VectorMask)
     * @see #lanewise(VectorOperators.Ternary,double,double)
     * @see #lanewise(VectorOperators.Ternary,Vector,double)
     * @see #lanewise(VectorOperators.Ternary,double,Vector)
     */
    @Override
    public abstract
    DoubleVector lanewise(VectorOperators.Ternary op,
                                                  Vector<Double> v1,
                                                  Vector<Double> v2);
    @ForceInline
    final
    DoubleVector lanewiseTemplate(VectorOperators.Ternary op,
                                          Vector<Double> v1,
                                          Vector<Double> v2) {
        DoubleVector that = (DoubleVector) v1;
        DoubleVector tother = (DoubleVector) v2;
        // It's a word: https://www.dictionary.com/browse/tother
        // See also Chapter 11 of Dickens, Our Mutual Friend:
        // "Totherest Governor," replied Mr Riderhood...
        that.check(this);
        tother.check(this);
        int opc = opCode(op);
        return VectorSupport.ternaryOp(
            opc, getClass(), double.class, length(),
            this, that, tother,
            TERN_IMPL.find(op, opc, (opc_) -> {
              switch (opc_) {
                case VECTOR_OP_FMA: return (v0, v1_, v2_) ->
                        v0.tOp(v1_, v2_, (i, a, b, c) -> Math.fma(a, b, c));
                default: return null;
                }}));
    }
    private static final
    ImplCache<Ternary,TernaryOperation<DoubleVector>> TERN_IMPL
        = new ImplCache<>(Ternary.class, DoubleVector.class);

    /**
     * {@inheritDoc} <!--workaround-->
     * @see #lanewise(VectorOperators.Ternary,double,double,VectorMask)
     * @see #lanewise(VectorOperators.Ternary,Vector,double,VectorMask)
     * @see #lanewise(VectorOperators.Ternary,double,Vector,VectorMask)
     */
    @ForceInline
    public final
    DoubleVector lanewise(VectorOperators.Ternary op,
                                  Vector<Double> v1,
                                  Vector<Double> v2,
                                  VectorMask<Double> m) {
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
     * @see #lanewise(VectorOperators.Ternary,double,double,VectorMask)
     */
    @ForceInline
    public final
    DoubleVector lanewise(VectorOperators.Ternary op, //(op,e1,e2)
                                  double e1,
                                  double e2) {
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
     * @see #lanewise(VectorOperators.Ternary,double,double)
     */
    @ForceInline
    public final
    DoubleVector lanewise(VectorOperators.Ternary op, //(op,e1,e2,m)
                                  double e1,
                                  double e2,
                                  VectorMask<Double> m) {
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
     * @see #lanewise(VectorOperators.Ternary,double,double)
     * @see #lanewise(VectorOperators.Ternary,Vector,double,VectorMask)
     */
    @ForceInline
    public final
    DoubleVector lanewise(VectorOperators.Ternary op, //(op,v1,e2)
                                  Vector<Double> v1,
                                  double e2) {
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
     * @see #lanewise(VectorOperators.Ternary,double,double,VectorMask)
     * @see #lanewise(VectorOperators.Ternary,Vector,double)
     */
    @ForceInline
    public final
    DoubleVector lanewise(VectorOperators.Ternary op, //(op,v1,e2,m)
                                  Vector<Double> v1,
                                  double e2,
                                  VectorMask<Double> m) {
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
     * @see #lanewise(VectorOperators.Ternary,double,Vector,VectorMask)
     */
    @ForceInline
    public final
    DoubleVector lanewise(VectorOperators.Ternary op, //(op,e1,v2)
                                  double e1,
                                  Vector<Double> v2) {
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
     * @see #lanewise(VectorOperators.Ternary,double,Vector)
     */
    @ForceInline
    public final
    DoubleVector lanewise(VectorOperators.Ternary op, //(op,e1,v2,m)
                                  double e1,
                                  Vector<Double> v2,
                                  VectorMask<Double> m) {
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
     * @see #add(double)
     */
    @Override
    @ForceInline
    public final DoubleVector add(Vector<Double> v) {
        return lanewise(ADD, v);
    }

    /**
     * Adds this vector to the broadcast of an input scalar.
     *
     * This is a lane-wise binary operation which applies
     * the primitive addition operation ({@code +}) to each lane.
     *
     * This method is also equivalent to the expression
     * {@link #lanewise(VectorOperators.Binary,double)
     *    lanewise}{@code (}{@link VectorOperators#ADD
     *    ADD}{@code , e)}.
     *
     * @param e the input scalar
     * @return the result of adding each lane of this vector to the scalar
     * @see #add(Vector)
     * @see #broadcast(double)
     * @see #add(double,VectorMask)
     * @see VectorOperators#ADD
     * @see #lanewise(VectorOperators.Binary,Vector)
     * @see #lanewise(VectorOperators.Binary,double)
     */
    @ForceInline
    public final
    DoubleVector add(double e) {
        return lanewise(ADD, e);
    }

    /**
     * {@inheritDoc} <!--workaround-->
     * @see #add(double,VectorMask)
     */
    @Override
    @ForceInline
    public final DoubleVector add(Vector<Double> v,
                                          VectorMask<Double> m) {
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
     * {@link #lanewise(VectorOperators.Binary,double,VectorMask)
     *    lanewise}{@code (}{@link VectorOperators#ADD
     *    ADD}{@code , s, m)}.
     *
     * @param e the input scalar
     * @param m the mask controlling lane selection
     * @return the result of adding each lane of this vector to the scalar
     * @see #add(Vector,VectorMask)
     * @see #broadcast(double)
     * @see #add(double)
     * @see VectorOperators#ADD
     * @see #lanewise(VectorOperators.Binary,Vector)
     * @see #lanewise(VectorOperators.Binary,double)
     */
    @ForceInline
    public final DoubleVector add(double e,
                                          VectorMask<Double> m) {
        return lanewise(ADD, e, m);
    }

    /**
     * {@inheritDoc} <!--workaround-->
     * @see #sub(double)
     */
    @Override
    @ForceInline
    public final DoubleVector sub(Vector<Double> v) {
        return lanewise(SUB, v);
    }

    /**
     * Subtracts an input scalar from this vector.
     *
     * This is a masked lane-wise binary operation which applies
     * the primitive subtraction operation ({@code -}) to each lane.
     *
     * This method is also equivalent to the expression
     * {@link #lanewise(VectorOperators.Binary,double)
     *    lanewise}{@code (}{@link VectorOperators#SUB
     *    SUB}{@code , e)}.
     *
     * @param e the input scalar
     * @return the result of subtracting the scalar from each lane of this vector
     * @see #sub(Vector)
     * @see #broadcast(double)
     * @see #sub(double,VectorMask)
     * @see VectorOperators#SUB
     * @see #lanewise(VectorOperators.Binary,Vector)
     * @see #lanewise(VectorOperators.Binary,double)
     */
    @ForceInline
    public final DoubleVector sub(double e) {
        return lanewise(SUB, e);
    }

    /**
     * {@inheritDoc} <!--workaround-->
     * @see #sub(double,VectorMask)
     */
    @Override
    @ForceInline
    public final DoubleVector sub(Vector<Double> v,
                                          VectorMask<Double> m) {
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
     * {@link #lanewise(VectorOperators.Binary,double,VectorMask)
     *    lanewise}{@code (}{@link VectorOperators#SUB
     *    SUB}{@code , s, m)}.
     *
     * @param e the input scalar
     * @param m the mask controlling lane selection
     * @return the result of subtracting the scalar from each lane of this vector
     * @see #sub(Vector,VectorMask)
     * @see #broadcast(double)
     * @see #sub(double)
     * @see VectorOperators#SUB
     * @see #lanewise(VectorOperators.Binary,Vector)
     * @see #lanewise(VectorOperators.Binary,double)
     */
    @ForceInline
    public final DoubleVector sub(double e,
                                          VectorMask<Double> m) {
        return lanewise(SUB, e, m);
    }

    /**
     * {@inheritDoc} <!--workaround-->
     * @see #mul(double)
     */
    @Override
    @ForceInline
    public final DoubleVector mul(Vector<Double> v) {
        return lanewise(MUL, v);
    }

    /**
     * Multiplies this vector by the broadcast of an input scalar.
     *
     * This is a lane-wise binary operation which applies
     * the primitive multiplication operation ({@code *}) to each lane.
     *
     * This method is also equivalent to the expression
     * {@link #lanewise(VectorOperators.Binary,double)
     *    lanewise}{@code (}{@link VectorOperators#MUL
     *    MUL}{@code , e)}.
     *
     * @param e the input scalar
     * @return the result of multiplying this vector by the given scalar
     * @see #mul(Vector)
     * @see #broadcast(double)
     * @see #mul(double,VectorMask)
     * @see VectorOperators#MUL
     * @see #lanewise(VectorOperators.Binary,Vector)
     * @see #lanewise(VectorOperators.Binary,double)
     */
    @ForceInline
    public final DoubleVector mul(double e) {
        return lanewise(MUL, e);
    }

    /**
     * {@inheritDoc} <!--workaround-->
     * @see #mul(double,VectorMask)
     */
    @Override
    @ForceInline
    public final DoubleVector mul(Vector<Double> v,
                                          VectorMask<Double> m) {
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
     * {@link #lanewise(VectorOperators.Binary,double,VectorMask)
     *    lanewise}{@code (}{@link VectorOperators#MUL
     *    MUL}{@code , s, m)}.
     *
     * @param e the input scalar
     * @param m the mask controlling lane selection
     * @return the result of muling each lane of this vector to the scalar
     * @see #mul(Vector,VectorMask)
     * @see #broadcast(double)
     * @see #mul(double)
     * @see VectorOperators#MUL
     * @see #lanewise(VectorOperators.Binary,Vector)
     * @see #lanewise(VectorOperators.Binary,double)
     */
    @ForceInline
    public final DoubleVector mul(double e,
                                          VectorMask<Double> m) {
        return lanewise(MUL, e, m);
    }

    /**
     * {@inheritDoc} <!--workaround-->
     * @apiNote Because the underlying scalar operator is an IEEE
     * floating point number, division by zero in fact will
     * not throw an exception, but will yield a signed
     * infinity or NaN.
     */
    @Override
    @ForceInline
    public final DoubleVector div(Vector<Double> v) {
        return lanewise(DIV, v);
    }

    /**
     * Divides this vector by the broadcast of an input scalar.
     *
     * This is a lane-wise binary operation which applies
     * the primitive division operation ({@code /}) to each lane.
     *
     * This method is also equivalent to the expression
     * {@link #lanewise(VectorOperators.Binary,double)
     *    lanewise}{@code (}{@link VectorOperators#DIV
     *    DIV}{@code , e)}.
     *
     * @apiNote Because the underlying scalar operator is an IEEE
     * floating point number, division by zero in fact will
     * not throw an exception, but will yield a signed
     * infinity or NaN.
     *
     * @param e the input scalar
     * @return the result of dividing each lane of this vector by the scalar
     * @see #div(Vector)
     * @see #broadcast(double)
     * @see #div(double,VectorMask)
     * @see VectorOperators#DIV
     * @see #lanewise(VectorOperators.Binary,Vector)
     * @see #lanewise(VectorOperators.Binary,double)
     */
    @ForceInline
    public final DoubleVector div(double e) {
        return lanewise(DIV, e);
    }

    /**
     * {@inheritDoc} <!--workaround-->
     * @see #div(double,VectorMask)
     * @apiNote Because the underlying scalar operator is an IEEE
     * floating point number, division by zero in fact will
     * not throw an exception, but will yield a signed
     * infinity or NaN.
     */
    @Override
    @ForceInline
    public final DoubleVector div(Vector<Double> v,
                                          VectorMask<Double> m) {
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
     * {@link #lanewise(VectorOperators.Binary,double,VectorMask)
     *    lanewise}{@code (}{@link VectorOperators#DIV
     *    DIV}{@code , s, m)}.
     *
     * @apiNote Because the underlying scalar operator is an IEEE
     * floating point number, division by zero in fact will
     * not throw an exception, but will yield a signed
     * infinity or NaN.
     *
     * @param e the input scalar
     * @param m the mask controlling lane selection
     * @return the result of dividing each lane of this vector by the scalar
     * @see #div(Vector,VectorMask)
     * @see #broadcast(double)
     * @see #div(double)
     * @see VectorOperators#DIV
     * @see #lanewise(VectorOperators.Binary,Vector)
     * @see #lanewise(VectorOperators.Binary,double)
     */
    @ForceInline
    public final DoubleVector div(double e,
                                          VectorMask<Double> m) {
        return lanewise(DIV, e, m);
    }

    /// END OF FULL-SERVICE BINARY METHODS

    /// SECOND-TIER BINARY METHODS
    //
    // There are no masked versions.

    /**
     * {@inheritDoc} <!--workaround-->
     * @apiNote
     * For this method, floating point negative
     * zero {@code -0.0} is treated as a value distinct from, and less
     * than the default value (positive zero).
     */
    @Override
    @ForceInline
    public final DoubleVector min(Vector<Double> v) {
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
     * {@link #lanewise(VectorOperators.Binary,double)
     *    lanewise}{@code (}{@link VectorOperators#MIN
     *    MIN}{@code , e)}.
     *
     * @param e the input scalar
     * @return the result of multiplying this vector by the given scalar
     * @see #min(Vector)
     * @see #broadcast(double)
     * @see VectorOperators#MIN
     * @see #lanewise(VectorOperators.Binary,double,VectorMask)
     * @apiNote
     * For this method, floating point negative
     * zero {@code -0.0} is treated as a value distinct from, and less
     * than the default value (positive zero).
     */
    @ForceInline
    public final DoubleVector min(double e) {
        return lanewise(MIN, e);
    }

    /**
     * {@inheritDoc} <!--workaround-->
     * @apiNote
     * For this method, floating point negative
     * zero {@code -0.0} is treated as a value distinct from, and less
     * than the default value (positive zero).
     */
    @Override
    @ForceInline
    public final DoubleVector max(Vector<Double> v) {
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
     * {@link #lanewise(VectorOperators.Binary,double)
     *    lanewise}{@code (}{@link VectorOperators#MAX
     *    MAX}{@code , e)}.
     *
     * @param e the input scalar
     * @return the result of multiplying this vector by the given scalar
     * @see #max(Vector)
     * @see #broadcast(double)
     * @see VectorOperators#MAX
     * @see #lanewise(VectorOperators.Binary,double,VectorMask)
     * @apiNote
     * For this method, floating point negative
     * zero {@code -0.0} is treated as a value distinct from, and less
     * than the default value (positive zero).
     */
    @ForceInline
    public final DoubleVector max(double e) {
        return lanewise(MAX, e);
    }


    // common FP operator: pow
    /**
     * Raises this vector to the power of a second input vector.
     *
     * This is a lane-wise binary operation which applies an operation
     * conforming to the specification of
     * {@link Math#pow Math.pow(a,b)}
     * to each pair of corresponding lane values.
     *
     * This method is also equivalent to the expression
     * {@link #lanewise(VectorOperators.Binary,Vector)
     *    lanewise}{@code (}{@link VectorOperators#POW
     *    POW}{@code , b)}.
     *
     * <p>
     * This is not a full-service named operation like
     * {@link #add(Vector) add}.  A masked version of
     * this operation is not directly available
     * but may be obtained via the masked version of
     * {@code lanewise}.
     *
     * @param b a vector exponent by which to raise this vector
     * @return the {@code b}-th power of this vector
     * @see #pow(double)
     * @see VectorOperators#POW
     * @see #lanewise(VectorOperators.Binary,Vector,VectorMask)
     */
    @ForceInline
    public final DoubleVector pow(Vector<Double> b) {
        return lanewise(POW, b);
    }

    /**
     * Raises this vector to a scalar power.
     *
     * This is a lane-wise binary operation which applies an operation
     * conforming to the specification of
     * {@link Math#pow Math.pow(a,b)}
     * to each pair of corresponding lane values.
     *
     * This method is also equivalent to the expression
     * {@link #lanewise(VectorOperators.Binary,Vector)
     *    lanewise}{@code (}{@link VectorOperators#POW
     *    POW}{@code , b)}.
     *
     * @param b a scalar exponent by which to raise this vector
     * @return the {@code b}-th power of this vector
     * @see #pow(Vector)
     * @see VectorOperators#POW
     * @see #lanewise(VectorOperators.Binary,double,VectorMask)
     */
    @ForceInline
    public final DoubleVector pow(double b) {
        return lanewise(POW, b);
    }

    /// UNARY METHODS

    /**
     * {@inheritDoc} <!--workaround-->
     */
    @Override
    @ForceInline
    public final
    DoubleVector neg() {
        return lanewise(NEG);
    }

    /**
     * {@inheritDoc} <!--workaround-->
     */
    @Override
    @ForceInline
    public final
    DoubleVector abs() {
        return lanewise(ABS);
    }


    // sqrt
    /**
     * Computes the square root of this vector.
     *
     * This is a lane-wise unary operation which applies an operation
     * conforming to the specification of
     * {@link Math#sqrt Math.sqrt(a)}
     * to each lane value.
     *
     * This method is also equivalent to the expression
     * {@link #lanewise(VectorOperators.Unary)
     *    lanewise}{@code (}{@link VectorOperators#SQRT
     *    SQRT}{@code )}.
     *
     * @return the square root of this vector
     * @see VectorOperators#SQRT
     * @see #lanewise(VectorOperators.Unary,VectorMask)
     */
    @ForceInline
    public final DoubleVector sqrt() {
        return lanewise(SQRT);
    }

    /// COMPARISONS

    /**
     * {@inheritDoc} <!--workaround-->
     */
    @Override
    @ForceInline
    public final
    VectorMask<Double> eq(Vector<Double> v) {
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
     * @see #compare(VectorOperators.Comparison,double)
     */
    @ForceInline
    public final
    VectorMask<Double> eq(double e) {
        return compare(EQ, e);
    }

    /**
     * {@inheritDoc} <!--workaround-->
     */
    @Override
    @ForceInline
    public final
    VectorMask<Double> lt(Vector<Double> v) {
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
     * @see #compare(VectorOperators.Comparison,double)
     */
    @ForceInline
    public final
    VectorMask<Double> lt(double e) {
        return compare(LT, e);
    }

    /**
     * {@inheritDoc} <!--workaround-->
     */
    @Override
    public abstract
    VectorMask<Double> test(VectorOperators.Test op);

    /*package-private*/
    @ForceInline
    final
    <M extends VectorMask<Double>>
    M testTemplate(Class<M> maskType, Test op) {
        DoubleSpecies vsp = vspecies();
        if (opKind(op, VO_SPECIAL)) {
            LongVector bits = this.viewAsIntegralLanes();
            VectorMask<Long> m;
            if (op == IS_DEFAULT) {
                m = bits.compare(EQ, (long) 0);
            } else if (op == IS_NEGATIVE) {
                m = bits.compare(LT, (long) 0);
            }
            else if (op == IS_FINITE ||
                     op == IS_NAN ||
                     op == IS_INFINITE) {
                // first kill the sign:
                bits = bits.and(Long.MAX_VALUE);
                // next find the bit pattern for infinity:
                long infbits = (long) toBits(Double.POSITIVE_INFINITY);
                // now compare:
                if (op == IS_FINITE) {
                    m = bits.compare(LT, infbits);
                } else if (op == IS_NAN) {
                    m = bits.compare(GT, infbits);
                } else {
                    m = bits.compare(EQ, infbits);
                }
            }
            else {
                throw new AssertionError(op);
            }
            return maskType.cast(m.cast(this.vspecies()));
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
    VectorMask<Double> test(VectorOperators.Test op,
                                  VectorMask<Double> m) {
        return test(op).and(m);
    }

    /**
     * {@inheritDoc} <!--workaround-->
     */
    @Override
    public abstract
    VectorMask<Double> compare(VectorOperators.Comparison op, Vector<Double> v);

    /*package-private*/
    @ForceInline
    final
    <M extends VectorMask<Double>>
    M compareTemplate(Class<M> maskType, Comparison op, Vector<Double> v) {
        Objects.requireNonNull(v);
        DoubleSpecies vsp = vspecies();
        DoubleVector that = (DoubleVector) v;
        that.check(this);
        int opc = opCode(op);
        return VectorSupport.compare(
            opc, getClass(), maskType, double.class, length(),
            this, that,
            (cond, v0, v1) -> {
                AbstractMask<Double> m
                    = v0.bTest(cond, v1, (cond_, i, a, b)
                               -> compareWithOp(cond, a, b));
                @SuppressWarnings("unchecked")
                M m2 = (M) m;
                return m2;
            });
    }

    @ForceInline
    private static boolean compareWithOp(int cond, double a, double b) {
        return switch (cond) {
            case BT_eq -> a == b;
            case BT_ne -> a != b;
            case BT_lt -> a < b;
            case BT_le -> a <= b;
            case BT_gt -> a > b;
            case BT_ge -> a >= b;
            default -> throw new AssertionError();
        };
    }

    /**
     * {@inheritDoc} <!--workaround-->
     */
    @Override
    @ForceInline
    public final
    VectorMask<Double> compare(VectorOperators.Comparison op,
                                  Vector<Double> v,
                                  VectorMask<Double> m) {
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
     * @see DoubleVector#compare(VectorOperators.Comparison,Vector)
     * @see #eq(double)
     * @see #lt(double)
     */
    public abstract
    VectorMask<Double> compare(Comparison op, double e);

    /*package-private*/
    @ForceInline
    final
    <M extends VectorMask<Double>>
    M compareTemplate(Class<M> maskType, Comparison op, double e) {
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
     * @see DoubleVector#compare(VectorOperators.Comparison,Vector,VectorMask)
     */
    @ForceInline
    public final VectorMask<Double> compare(VectorOperators.Comparison op,
                                               double e,
                                               VectorMask<Double> m) {
        return compare(op, e).and(m);
    }

    /**
     * {@inheritDoc} <!--workaround-->
     */
    @Override
    public abstract
    VectorMask<Double> compare(Comparison op, long e);

    /*package-private*/
    @ForceInline
    final
    <M extends VectorMask<Double>>
    M compareTemplate(Class<M> maskType, Comparison op, long e) {
        return compareTemplate(maskType, op, broadcast(e));
    }

    /**
     * {@inheritDoc} <!--workaround-->
     */
    @Override
    @ForceInline
    public final
    VectorMask<Double> compare(Comparison op, long e, VectorMask<Double> m) {
        return compare(op, broadcast(e), m);
    }



    /**
     * {@inheritDoc} <!--workaround-->
     */
    @Override public abstract
    DoubleVector blend(Vector<Double> v, VectorMask<Double> m);

    /*package-private*/
    @ForceInline
    final
    <M extends VectorMask<Double>>
    DoubleVector
    blendTemplate(Class<M> maskType, DoubleVector v, M m) {
        v.check(this);
        return VectorSupport.blend(
            getClass(), maskType, double.class, length(),
            this, v, m,
            (v0, v1, m_) -> v0.bOp(v1, m_, (i, a, b) -> b));
    }

    /**
     * {@inheritDoc} <!--workaround-->
     */
    @Override public abstract DoubleVector addIndex(int scale);

    /*package-private*/
    @ForceInline
    final DoubleVector addIndexTemplate(int scale) {
        DoubleSpecies vsp = vspecies();
        // make sure VLENGTH*scale doesn't overflow:
        vsp.checkScale(scale);
        return VectorSupport.indexVector(
            getClass(), double.class, length(),
            this, scale, vsp,
            (v, scale_, s)
            -> {
                // If the platform doesn't support an INDEX
                // instruction directly, load IOTA from memory
                // and multiply.
                DoubleVector iota = s.iota();
                double sc = (double) scale_;
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
    public final DoubleVector blend(double e,
                                            VectorMask<Double> m) {
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
    public final DoubleVector blend(long e,
                                            VectorMask<Double> m) {
        return blend(broadcast(e), m);
    }

    /**
     * {@inheritDoc} <!--workaround-->
     */
    @Override
    public abstract
    DoubleVector slice(int origin, Vector<Double> v1);

    /*package-private*/
    final
    @ForceInline
    DoubleVector sliceTemplate(int origin, Vector<Double> v1) {
        DoubleVector that = (DoubleVector) v1;
        that.check(this);
        Objects.checkIndex(origin, length() + 1);
        VectorShuffle<Double> iota = iotaShuffle();
        VectorMask<Double> blendMask = iota.toVector().compare(VectorOperators.LT, (broadcast((double)(length() - origin))));
        iota = iotaShuffle(origin, 1, true);
        return that.rearrange(iota).blend(this.rearrange(iota), blendMask);
    }

    /**
     * {@inheritDoc} <!--workaround-->
     */
    @Override
    @ForceInline
    public final
    DoubleVector slice(int origin,
                               Vector<Double> w,
                               VectorMask<Double> m) {
        return broadcast(0).blend(slice(origin, w), m);
    }

    /**
     * {@inheritDoc} <!--workaround-->
     */
    @Override
    public abstract
    DoubleVector slice(int origin);

    /*package-private*/
    final
    @ForceInline
    DoubleVector sliceTemplate(int origin) {
        Objects.checkIndex(origin, length() + 1);
        VectorShuffle<Double> iota = iotaShuffle();
        VectorMask<Double> blendMask = iota.toVector().compare(VectorOperators.LT, (broadcast((double)(length() - origin))));
        iota = iotaShuffle(origin, 1, true);
        return vspecies().zero().blend(this.rearrange(iota), blendMask);
    }

    /**
     * {@inheritDoc} <!--workaround-->
     */
    @Override
    public abstract
    DoubleVector unslice(int origin, Vector<Double> w, int part);

    /*package-private*/
    final
    @ForceInline
    DoubleVector
    unsliceTemplate(int origin, Vector<Double> w, int part) {
        DoubleVector that = (DoubleVector) w;
        that.check(this);
        Objects.checkIndex(origin, length() + 1);
        VectorShuffle<Double> iota = iotaShuffle();
        VectorMask<Double> blendMask = iota.toVector().compare((part == 0) ? VectorOperators.GE : VectorOperators.LT,
                                                                  (broadcast((double)(origin))));
        iota = iotaShuffle(-origin, 1, true);
        return that.blend(this.rearrange(iota), blendMask);
    }

    /*package-private*/
    final
    @ForceInline
    <M extends VectorMask<Double>>
    DoubleVector
    unsliceTemplate(Class<M> maskType, int origin, Vector<Double> w, int part, M m) {
        DoubleVector that = (DoubleVector) w;
        that.check(this);
        DoubleVector slice = that.sliceTemplate(origin, that);
        slice = slice.blendTemplate(maskType, this, m);
        return slice.unsliceTemplate(origin, w, part);
    }

    /**
     * {@inheritDoc} <!--workaround-->
     */
    @Override
    public abstract
    DoubleVector unslice(int origin, Vector<Double> w, int part, VectorMask<Double> m);

    /**
     * {@inheritDoc} <!--workaround-->
     */
    @Override
    public abstract
    DoubleVector unslice(int origin);

    /*package-private*/
    final
    @ForceInline
    DoubleVector
    unsliceTemplate(int origin) {
        Objects.checkIndex(origin, length() + 1);
        VectorShuffle<Double> iota = iotaShuffle();
        VectorMask<Double> blendMask = iota.toVector().compare(VectorOperators.GE,
                                                                  (broadcast((double)(origin))));
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
    DoubleVector rearrange(VectorShuffle<Double> m);

    /*package-private*/
    @ForceInline
    final
    <S extends VectorShuffle<Double>>
    DoubleVector rearrangeTemplate(Class<S> shuffletype, S shuffle) {
        shuffle.checkIndexes();
        return VectorSupport.rearrangeOp(
            getClass(), shuffletype, double.class, length(),
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
    DoubleVector rearrange(VectorShuffle<Double> s,
                                   VectorMask<Double> m);

    /*package-private*/
    @ForceInline
    final
    <S extends VectorShuffle<Double>>
    DoubleVector rearrangeTemplate(Class<S> shuffletype,
                                           S shuffle,
                                           VectorMask<Double> m) {
        DoubleVector unmasked =
            VectorSupport.rearrangeOp(
                getClass(), shuffletype, double.class, length(),
                this, shuffle,
                (v1, s_) -> v1.uOp((i, a) -> {
                    int ei = s_.laneSource(i);
                    return ei < 0 ? 0 : v1.lane(ei);
                }));
        VectorMask<Double> valid = shuffle.laneIsValid();
        if (m.andNot(valid).anyTrue()) {
            shuffle.checkIndexes();
            throw new AssertionError();
        }
        return broadcast((double)0).blend(unmasked, m);
    }

    /**
     * {@inheritDoc} <!--workaround-->
     */
    @Override
    public abstract
    DoubleVector rearrange(VectorShuffle<Double> s,
                                   Vector<Double> v);

    /*package-private*/
    @ForceInline
    final
    <S extends VectorShuffle<Double>>
    DoubleVector rearrangeTemplate(Class<S> shuffletype,
                                           S shuffle,
                                           DoubleVector v) {
        VectorMask<Double> valid = shuffle.laneIsValid();
        @SuppressWarnings("unchecked")
        S ws = (S) shuffle.wrapIndexes();
        DoubleVector r0 =
            VectorSupport.rearrangeOp(
                getClass(), shuffletype, double.class, length(),
                this, ws,
                (v0, s_) -> v0.uOp((i, a) -> {
                    int ei = s_.laneSource(i);
                    return v0.lane(ei);
                }));
        DoubleVector r1 =
            VectorSupport.rearrangeOp(
                getClass(), shuffletype, double.class, length(),
                v, ws,
                (v1, s_) -> v1.uOp((i, a) -> {
                    int ei = s_.laneSource(i);
                    return v1.lane(ei);
                }));
        return r1.blend(r0, valid);
    }

    @ForceInline
    private final
    VectorShuffle<Double> toShuffle0(DoubleSpecies dsp) {
        double[] a = toArray();
        int[] sa = new int[a.length];
        for (int i = 0; i < a.length; i++) {
            sa[i] = (int) a[i];
        }
        return VectorShuffle.fromArray(dsp, sa, 0);
    }

    /*package-private*/
    @ForceInline
    final
    VectorShuffle<Double> toShuffleTemplate(Class<?> shuffleType) {
        DoubleSpecies vsp = vspecies();
        return VectorSupport.convert(VectorSupport.VECTOR_OP_CAST,
                                     getClass(), double.class, length(),
                                     shuffleType, byte.class, length(),
                                     this, vsp,
                                     DoubleVector::toShuffle0);
    }

    /**
     * {@inheritDoc} <!--workaround-->
     */
    @Override
    public abstract
    DoubleVector selectFrom(Vector<Double> v);

    /*package-private*/
    @ForceInline
    final DoubleVector selectFromTemplate(DoubleVector v) {
        return v.rearrange(this.toShuffle());
    }

    /**
     * {@inheritDoc} <!--workaround-->
     */
    @Override
    public abstract
    DoubleVector selectFrom(Vector<Double> s, VectorMask<Double> m);

    /*package-private*/
    @ForceInline
    final DoubleVector selectFromTemplate(DoubleVector v,
                                                  AbstractMask<Double> m) {
        return v.rearrange(this.toShuffle(), m);
    }

    /// Ternary operations


    /**
     * Multiplies this vector by a second input vector, and sums
     * the result with a third.
     *
     * Extended precision is used for the intermediate result,
     * avoiding possible loss of precision from rounding once
     * for each of the two operations.
     * The result is numerically close to {@code this.mul(b).add(c)},
     * and is typically closer to the true mathematical result.
     *
     * This is a lane-wise ternary operation which applies an operation
     * conforming to the specification of
     * {@link Math#fma(double,double,double) Math.fma(a,b,c)}
     * to each lane.
     *
     * This method is also equivalent to the expression
     * {@link #lanewise(VectorOperators.Ternary,Vector,Vector)
     *    lanewise}{@code (}{@link VectorOperators#FMA
     *    FMA}{@code , b, c)}.
     *
     * @param b the second input vector, supplying multiplier values
     * @param c the third input vector, supplying addend values
     * @return the product of this vector and the second input vector
     *         summed with the third input vector, using extended precision
     *         for the intermediate result
     * @see #fma(double,double)
     * @see VectorOperators#FMA
     * @see #lanewise(VectorOperators.Ternary,Vector,Vector,VectorMask)
     */
    @ForceInline
    public final
    DoubleVector fma(Vector<Double> b, Vector<Double> c) {
        return lanewise(FMA, b, c);
    }

    /**
     * Multiplies this vector by a scalar multiplier, and sums
     * the result with a scalar addend.
     *
     * Extended precision is used for the intermediate result,
     * avoiding possible loss of precision from rounding once
     * for each of the two operations.
     * The result is numerically close to {@code this.mul(b).add(c)},
     * and is typically closer to the true mathematical result.
     *
     * This is a lane-wise ternary operation which applies an operation
     * conforming to the specification of
     * {@link Math#fma(double,double,double) Math.fma(a,b,c)}
     * to each lane.
     *
     * This method is also equivalent to the expression
     * {@link #lanewise(VectorOperators.Ternary,Vector,Vector)
     *    lanewise}{@code (}{@link VectorOperators#FMA
     *    FMA}{@code , b, c)}.
     *
     * @param b the scalar multiplier
     * @param c the scalar addend
     * @return the product of this vector and the scalar multiplier
     *         summed with scalar addend, using extended precision
     *         for the intermediate result
     * @see #fma(Vector,Vector)
     * @see VectorOperators#FMA
     * @see #lanewise(VectorOperators.Ternary,double,double,VectorMask)
     */
    @ForceInline
    public final
    DoubleVector fma(double b, double c) {
        return lanewise(FMA, b, c);
    }

    // Don't bother with (Vector,double) and (double,Vector) overloadings.

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
     * (As with {@code MAX} and {@code MIN}, floating point negative
     * zero {@code -0.0} is treated as a value distinct from
     * the default value, positive zero. So a first-nonzero lane reduction
     * might return {@code -0.0} even in the presence of non-zero
     * lane values.)
     * <li>
     * In the case of {@code ADD} and {@code MUL}, the
     * precise result will reflect the choice of an arbitrary order
     * of operations, which may even vary over time.
     * For further details see the section
     * <a href="VectorOperators.html#fp_assoc">Operations on floating point vectors</a>.
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
     * @see VectorOperators#FIRST_NONZERO
     */
    public abstract double reduceLanes(VectorOperators.Associative op);

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
     *  {@code ADD}
     * or {@code FIRST_NONZERO},
     * then the identity value is positive zero, the default {@code double} value.
     * <li>
     * If the operation is {@code MUL},
     * then the identity value is one.
     * <li>
     * If the operation is {@code MAX},
     * then the identity value is {@code Double.NEGATIVE_INFINITY}.
     * <li>
     * If the operation is {@code MIN},
     * then the identity value is {@code Double.POSITIVE_INFINITY}.
     * </ul>
     * <p>
     * A few reduction operations do not support arbitrary reordering
     * of their operands, yet are included here because of their
     * usefulness.
     * <ul>
     * <li>
     * In the case of {@code FIRST_NONZERO}, the reduction returns
     * the value from the lowest-numbered non-zero lane.
     * (As with {@code MAX} and {@code MIN}, floating point negative
     * zero {@code -0.0} is treated as a value distinct from
     * the default value, positive zero. So a first-nonzero lane reduction
     * might return {@code -0.0} even in the presence of non-zero
     * lane values.)
     * <li>
     * In the case of {@code ADD} and {@code MUL}, the
     * precise result will reflect the choice of an arbitrary order
     * of operations, which may even vary over time.
     * For further details see the section
     * <a href="VectorOperators.html#fp_assoc">Operations on floating point vectors</a>.
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
    public abstract double reduceLanes(VectorOperators.Associative op,
                                       VectorMask<Double> m);

    /*package-private*/
    @ForceInline
    final
    double reduceLanesTemplate(VectorOperators.Associative op,
                               VectorMask<Double> m) {
        DoubleVector v = reduceIdentityVector(op).blend(this, m);
        return v.reduceLanesTemplate(op);
    }

    /*package-private*/
    @ForceInline
    final
    double reduceLanesTemplate(VectorOperators.Associative op) {
        if (op == FIRST_NONZERO) {
            // FIXME:  The JIT should handle this, and other scan ops alos.
            VectorMask<Long> thisNZ
                = this.viewAsIntegralLanes().compare(NE, (long) 0);
            return this.lane(thisNZ.firstTrue());
        }
        int opc = opCode(op);
        return fromBits(VectorSupport.reductionCoerced(
            opc, getClass(), double.class, length(),
            this,
            REDUCE_IMPL.find(op, opc, (opc_) -> {
              switch (opc_) {
              case VECTOR_OP_ADD: return v ->
                      toBits(v.rOp((double)0, (i, a, b) -> (double)(a + b)));
              case VECTOR_OP_MUL: return v ->
                      toBits(v.rOp((double)1, (i, a, b) -> (double)(a * b)));
              case VECTOR_OP_MIN: return v ->
                      toBits(v.rOp(MAX_OR_INF, (i, a, b) -> (double) Math.min(a, b)));
              case VECTOR_OP_MAX: return v ->
                      toBits(v.rOp(MIN_OR_INF, (i, a, b) -> (double) Math.max(a, b)));
              default: return null;
              }})));
    }
    private static final
    ImplCache<Associative,Function<DoubleVector,Long>> REDUCE_IMPL
        = new ImplCache<>(Associative.class, DoubleVector.class);

    private
    @ForceInline
    DoubleVector reduceIdentityVector(VectorOperators.Associative op) {
        int opc = opCode(op);
        UnaryOperator<DoubleVector> fn
            = REDUCE_ID_IMPL.find(op, opc, (opc_) -> {
                switch (opc_) {
                case VECTOR_OP_ADD:
                    return v -> v.broadcast(0);
                case VECTOR_OP_MUL:
                    return v -> v.broadcast(1);
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
    ImplCache<Associative,UnaryOperator<DoubleVector>> REDUCE_ID_IMPL
        = new ImplCache<>(Associative.class, DoubleVector.class);

    private static final double MIN_OR_INF = Double.NEGATIVE_INFINITY;
    private static final double MAX_OR_INF = Double.POSITIVE_INFINITY;

    public @Override abstract long reduceLanesToLong(VectorOperators.Associative op);
    public @Override abstract long reduceLanesToLong(VectorOperators.Associative op,
                                                     VectorMask<Double> m);

    // Type specific accessors

    /**
     * Gets the lane element at lane index {@code i}
     *
     * @param i the lane index
     * @return the lane element at lane index {@code i}
     * @throws IllegalArgumentException if the index is is out of range
     * ({@code < 0 || >= length()})
     */
    public abstract double lane(int i);

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
    public abstract DoubleVector withLane(int i, double e);

    // Memory load operations

    /**
     * Returns an array of type {@code double[]}
     * containing all the lane values.
     * The array length is the same as the vector length.
     * The array elements are stored in lane order.
     * <p>
     * This method behaves as if it stores
     * this vector into an allocated array
     * (using {@link #intoArray(double[], int) intoArray})
     * and returns the array as follows:
     * <pre>{@code
     *   double[] a = new double[this.length()];
     *   this.intoArray(a, 0);
     *   return a;
     * }</pre>
     *
     * @return an array containing the lane values of this vector
     */
    @ForceInline
    @Override
    public final double[] toArray() {
        double[] a = new double[vspecies().laneCount()];
        intoArray(a, 0);
        return a;
    }

    /** {@inheritDoc} <!--workaround-->
     */
    @ForceInline
    @Override
    public final int[] toIntArray() {
        double[] a = toArray();
        int[] res = new int[a.length];
        for (int i = 0; i < a.length; i++) {
            double e = a[i];
            res[i] = (int) DoubleSpecies.toIntegralChecked(e, true);
        }
        return res;
    }

    /** {@inheritDoc} <!--workaround-->
     */
    @ForceInline
    @Override
    public final long[] toLongArray() {
        double[] a = toArray();
        long[] res = new long[a.length];
        for (int i = 0; i < a.length; i++) {
            double e = a[i];
            res[i] = DoubleSpecies.toIntegralChecked(e, false);
        }
        return res;
    }

    /** {@inheritDoc} <!--workaround-->
     * @implNote
     * This is an alias for {@link #toArray()}
     * When this method is used on used on vectors
     * of type {@code DoubleVector},
     * there will be no loss of precision.
     */
    @ForceInline
    @Override
    public final double[] toDoubleArray() {
        return toArray();
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
    DoubleVector fromByteArray(VectorSpecies<Double> species,
                                       byte[] a, int offset,
                                       ByteOrder bo) {
        offset = checkFromIndexSize(offset, species.vectorByteSize(), a.length);
        DoubleSpecies vsp = (DoubleSpecies) species;
        return vsp.dummyVector().fromByteArray0(a, offset).maybeSwap(bo);
    }

    /**
     * Loads a vector from a byte array starting at an offset
     * and using a mask.
     * Lanes where the mask is unset are filled with the default
     * value of {@code double} (positive zero).
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
    DoubleVector fromByteArray(VectorSpecies<Double> species,
                                       byte[] a, int offset,
                                       ByteOrder bo,
                                       VectorMask<Double> m) {
        DoubleSpecies vsp = (DoubleSpecies) species;
        if (offset >= 0 && offset <= (a.length - species.vectorByteSize())) {
            DoubleVector zero = vsp.zero();
            DoubleVector v = zero.fromByteArray0(a, offset);
            return zero.blend(v.maybeSwap(bo), m);
        }

        // FIXME: optimize
        checkMaskFromIndexSize(offset, vsp, m, 8, a.length);
        ByteBuffer wb = wrapper(a, bo);
        return vsp.ldOp(wb, offset, (AbstractMask<Double>)m,
                   (wb_, o, i)  -> wb_.getDouble(o + i * 8));
    }

    /**
     * Loads a vector from an array of type {@code double[]}
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
    DoubleVector fromArray(VectorSpecies<Double> species,
                                   double[] a, int offset) {
        offset = checkFromIndexSize(offset, species.length(), a.length);
        DoubleSpecies vsp = (DoubleSpecies) species;
        return vsp.dummyVector().fromArray0(a, offset);
    }

    /**
     * Loads a vector from an array of type {@code double[]}
     * starting at an offset and using a mask.
     * Lanes where the mask is unset are filled with the default
     * value of {@code double} (positive zero).
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
    DoubleVector fromArray(VectorSpecies<Double> species,
                                   double[] a, int offset,
                                   VectorMask<Double> m) {
        DoubleSpecies vsp = (DoubleSpecies) species;
        if (offset >= 0 && offset <= (a.length - species.length())) {
            DoubleVector zero = vsp.zero();
            return zero.blend(zero.fromArray0(a, offset), m);
        }

        // FIXME: optimize
        checkMaskFromIndexSize(offset, vsp, m, 1, a.length);
        return vsp.vOp(m, i -> a[offset + i]);
    }

    /**
     * Gathers a new vector composed of elements from an array of type
     * {@code double[]},
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
     * @see DoubleVector#toIntArray()
     */
    @ForceInline
    public static
    DoubleVector fromArray(VectorSpecies<Double> species,
                                   double[] a, int offset,
                                   int[] indexMap, int mapOffset) {
        DoubleSpecies vsp = (DoubleSpecies) species;
        IntVector.IntSpecies isp = IntVector.species(vsp.indexShape());
        Objects.requireNonNull(a);
        Objects.requireNonNull(indexMap);
        Class<? extends DoubleVector> vectorType = vsp.vectorType();

        if (vsp.laneCount() == 1) {
          return DoubleVector.fromArray(vsp, a, offset + indexMap[mapOffset]);
        }

        // Index vector: vix[0:n] = k -> offset + indexMap[mapOffset + k]
        IntVector vix;
        if (isp.laneCount() != vsp.laneCount()) {
            // For DoubleMaxVector,  if vector length is non-power-of-two or
            // 2048 bits, indexShape of Double species is S_MAX_BIT.
            // Assume that vector length is 2048, then the lane count of Double
            // vector is 32. When converting Double species to int species,
            // indexShape is still S_MAX_BIT, but the lane count of int vector
            // is 64. So when loading index vector (IntVector), only lower half
            // of index data is needed.
            vix = IntVector
                .fromArray(isp, indexMap, mapOffset, IntMaxVector.IntMaxMask.LOWER_HALF_TRUE_MASK)
                .add(offset);
        } else {
            vix = IntVector
                .fromArray(isp, indexMap, mapOffset)
                .add(offset);
        }

        vix = VectorIntrinsics.checkIndex(vix, a.length);

        return VectorSupport.loadWithMap(
            vectorType, double.class, vsp.laneCount(),
            IntVector.species(vsp.indexShape()).vectorType(),
            a, ARRAY_BASE, vix,
            a, offset, indexMap, mapOffset, vsp,
            (double[] c, int idx, int[] iMap, int idy, DoubleSpecies s) ->
            s.vOp(n -> c[idx + iMap[idy+n]]));
        }

    /**
     * Gathers a new vector composed of elements from an array of type
     * {@code double[]},
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
     * @see DoubleVector#toIntArray()
     */
    @ForceInline
    public static
    DoubleVector fromArray(VectorSpecies<Double> species,
                                   double[] a, int offset,
                                   int[] indexMap, int mapOffset,
                                   VectorMask<Double> m) {
        if (m.allTrue()) {
            return fromArray(species, a, offset, indexMap, mapOffset);
        }
        else {
            // FIXME: Cannot vectorize yet, if there's a mask.
            DoubleSpecies vsp = (DoubleSpecies) species;
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
     *         if {@code offset+N*8 < 0}
     *         or {@code offset+N*8 >= bb.limit()}
     *         for any lane {@code N} in the vector
     */
    @ForceInline
    public static
    DoubleVector fromByteBuffer(VectorSpecies<Double> species,
                                        ByteBuffer bb, int offset,
                                        ByteOrder bo) {
        offset = checkFromIndexSize(offset, species.vectorByteSize(), bb.limit());
        DoubleSpecies vsp = (DoubleSpecies) species;
        return vsp.dummyVector().fromByteBuffer0(bb, offset).maybeSwap(bo);
    }

    /**
     * Loads a vector from a {@linkplain ByteBuffer byte buffer}
     * starting at an offset into the byte buffer
     * and using a mask.
     * Lanes where the mask is unset are filled with the default
     * value of {@code double} (positive zero).
     * Bytes are composed into primitive lane elements according
     * to the specified byte order.
     * The vector is arranged into lanes according to
     * <a href="Vector.html#lane-order">memory ordering</a>.
     * <p>
     * The following pseudocode illustrates the behavior:
     * <pre>{@code
     * DoubleBuffer eb = bb.duplicate()
     *     .position(offset)
     *     .order(bo).asDoubleBuffer();
     * double[] ar = new double[species.length()];
     * for (int n = 0; n < ar.length; n++) {
     *     if (m.laneIsSet(n)) {
     *         ar[n] = eb.get(n);
     *     }
     * }
     * DoubleVector r = DoubleVector.fromArray(species, ar, 0);
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
     *         if {@code offset+N*8 < 0}
     *         or {@code offset+N*8 >= bb.limit()}
     *         for any lane {@code N} in the vector
     *         where the mask is set
     */
    @ForceInline
    public static
    DoubleVector fromByteBuffer(VectorSpecies<Double> species,
                                        ByteBuffer bb, int offset,
                                        ByteOrder bo,
                                        VectorMask<Double> m) {
        DoubleSpecies vsp = (DoubleSpecies) species;
        if (offset >= 0 && offset <= (bb.limit() - species.vectorByteSize())) {
            DoubleVector zero = vsp.zero();
            DoubleVector v = zero.fromByteBuffer0(bb, offset);
            return zero.blend(v.maybeSwap(bo), m);
        }

        // FIXME: optimize
        checkMaskFromIndexSize(offset, vsp, m, 8, bb.limit());
        ByteBuffer wb = wrapper(bb, bo);
        return vsp.ldOp(wb, offset, (AbstractMask<Double>)m,
                   (wb_, o, i)  -> wb_.getDouble(o + i * 8));
    }

    // Memory store operations

    /**
     * Stores this vector into an array of type {@code double[]}
     * starting at an offset.
     * <p>
     * For each vector lane, where {@code N} is the vector lane index,
     * the lane element at index {@code N} is stored into the array
     * element {@code a[offset+N]}.
     *
     * @param a the array, of type {@code double[]}
     * @param offset the offset into the array
     * @throws IndexOutOfBoundsException
     *         if {@code offset+N < 0} or {@code offset+N >= a.length}
     *         for any lane {@code N} in the vector
     */
    @ForceInline
    public final
    void intoArray(double[] a, int offset) {
        offset = checkFromIndexSize(offset, length(), a.length);
        DoubleSpecies vsp = vspecies();
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
     * Stores this vector into an array of type {@code double[]}
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
     * @param a the array, of type {@code double[]}
     * @param offset the offset into the array
     * @param m the mask controlling lane storage
     * @throws IndexOutOfBoundsException
     *         if {@code offset+N < 0} or {@code offset+N >= a.length}
     *         for any lane {@code N} in the vector
     *         where the mask is set
     */
    @ForceInline
    public final
    void intoArray(double[] a, int offset,
                   VectorMask<Double> m) {
        if (m.allTrue()) {
            intoArray(a, offset);
        } else {
            // FIXME: optimize
            DoubleSpecies vsp = vspecies();
            checkMaskFromIndexSize(offset, vsp, m, 1, a.length);
            stOp(a, offset, m, (arr, off, i, v) -> arr[off+i] = v);
        }
    }

    /**
     * Scatters this vector into an array of type {@code double[]}
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
     * @see DoubleVector#toIntArray()
     */
    @ForceInline
    public final
    void intoArray(double[] a, int offset,
                   int[] indexMap, int mapOffset) {
        DoubleSpecies vsp = vspecies();
        IntVector.IntSpecies isp = IntVector.species(vsp.indexShape());
        if (vsp.laneCount() == 1) {
            intoArray(a, offset + indexMap[mapOffset]);
            return;
        }

        // Index vector: vix[0:n] = i -> offset + indexMap[mo + i]
        IntVector vix;
        if (isp.laneCount() != vsp.laneCount()) {
            // For DoubleMaxVector,  if vector length  is 2048 bits, indexShape
            // of Double species is S_MAX_BIT. and the lane count of Double
            // vector is 32. When converting Double species to int species,
            // indexShape is still S_MAX_BIT, but the lane count of int vector
            // is 64. So when loading index vector (IntVector), only lower half
            // of index data is needed.
            vix = IntVector
                .fromArray(isp, indexMap, mapOffset, IntMaxVector.IntMaxMask.LOWER_HALF_TRUE_MASK)
                .add(offset);
        } else {
            vix = IntVector
                .fromArray(isp, indexMap, mapOffset)
                .add(offset);
        }


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
     * Scatters this vector into an array of type {@code double[]},
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
     * @see DoubleVector#toIntArray()
     */
    @ForceInline
    public final
    void intoArray(double[] a, int offset,
                   int[] indexMap, int mapOffset,
                   VectorMask<Double> m) {
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
                       VectorMask<Double> m) {
        if (m.allTrue()) {
            intoByteArray(a, offset, bo);
        } else {
            // FIXME: optimize
            DoubleSpecies vsp = vspecies();
            checkMaskFromIndexSize(offset, vsp, m, 8, a.length);
            ByteBuffer wb = wrapper(a, bo);
            this.stOp(wb, offset, m,
                    (wb_, o, i, e) -> wb_.putDouble(o + i * 8, e));
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
                        VectorMask<Double> m) {
        if (m.allTrue()) {
            intoByteBuffer(bb, offset, bo);
        } else {
            // FIXME: optimize
            if (bb.isReadOnly()) {
                throw new ReadOnlyBufferException();
            }
            DoubleSpecies vsp = vspecies();
            checkMaskFromIndexSize(offset, vsp, m, 8, bb.limit());
            ByteBuffer wb = wrapper(bb, bo);
            this.stOp(wb, offset, m,
                    (wb_, o, i, e) -> wb_.putDouble(o + i * 8, e));
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
    DoubleVector fromArray0(double[] a, int offset);
    @ForceInline
    final
    DoubleVector fromArray0Template(double[] a, int offset) {
        DoubleSpecies vsp = vspecies();
        return VectorSupport.load(
            vsp.vectorType(), vsp.elementType(), vsp.laneCount(),
            a, arrayAddress(a, offset),
            a, offset, vsp,
            (arr, off, s) -> s.ldOp(arr, off,
                                    (arr_, off_, i) -> arr_[off_ + i]));
    }



    @Override
    abstract
    DoubleVector fromByteArray0(byte[] a, int offset);
    @ForceInline
    final
    DoubleVector fromByteArray0Template(byte[] a, int offset) {
        DoubleSpecies vsp = vspecies();
        return VectorSupport.load(
            vsp.vectorType(), vsp.elementType(), vsp.laneCount(),
            a, byteArrayAddress(a, offset),
            a, offset, vsp,
            (arr, off, s) -> {
                ByteBuffer wb = wrapper(arr, NATIVE_ENDIAN);
                return s.ldOp(wb, off,
                        (wb_, o, i) -> wb_.getDouble(o + i * 8));
            });
    }

    abstract
    DoubleVector fromByteBuffer0(ByteBuffer bb, int offset);
    @ForceInline
    final
    DoubleVector fromByteBuffer0Template(ByteBuffer bb, int offset) {
        DoubleSpecies vsp = vspecies();
        return ScopedMemoryAccess.loadFromByteBuffer(
                vsp.vectorType(), vsp.elementType(), vsp.laneCount(),
                bb, offset, vsp,
                (buf, off, s) -> {
                    ByteBuffer wb = wrapper(buf, NATIVE_ENDIAN);
                    return s.ldOp(wb, off,
                            (wb_, o, i) -> wb_.getDouble(o + i * 8));
                });
    }

    // Unchecked storing operations in native byte order.
    // Caller is responsible for applying index checks, masking, and
    // byte swapping.

    abstract
    void intoArray0(double[] a, int offset);
    @ForceInline
    final
    void intoArray0Template(double[] a, int offset) {
        DoubleSpecies vsp = vspecies();
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
        DoubleSpecies vsp = vspecies();
        VectorSupport.store(
            vsp.vectorType(), vsp.elementType(), vsp.laneCount(),
            a, byteArrayAddress(a, offset),
            this, a, offset,
            (arr, off, v) -> {
                ByteBuffer wb = wrapper(arr, NATIVE_ENDIAN);
                v.stOp(wb, off,
                        (tb_, o, i, e) -> tb_.putDouble(o + i * 8, e));
            });
    }

    @ForceInline
    final
    void intoByteBuffer0(ByteBuffer bb, int offset) {
        DoubleSpecies vsp = vspecies();
        ScopedMemoryAccess.storeIntoByteBuffer(
                vsp.vectorType(), vsp.elementType(), vsp.laneCount(),
                this, bb, offset,
                (buf, off, v) -> {
                    ByteBuffer wb = wrapper(buf, NATIVE_ENDIAN);
                    v.stOp(wb, off,
                            (wb_, o, i, e) -> wb_.putDouble(o + i * 8, e));
                });
    }

    // End of low-level memory operations.

    private static
    void checkMaskFromIndexSize(int offset,
                                DoubleSpecies vsp,
                                VectorMask<Double> m,
                                int scale,
                                int limit) {
        ((AbstractMask<Double>)m)
            .checkIndexByLane(offset, limit, vsp.iota(), scale);
    }

    @ForceInline
    private void conditionalStoreNYI(int offset,
                                     DoubleSpecies vsp,
                                     VectorMask<Double> m,
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
    DoubleVector maybeSwap(ByteOrder bo) {
        if (bo != NATIVE_ENDIAN) {
            return this.reinterpretAsBytes()
                .rearrange(swapBytesShuffle())
                .reinterpretAsDoubles();
        }
        return this;
    }

    static final int ARRAY_SHIFT =
        31 - Integer.numberOfLeadingZeros(Unsafe.ARRAY_DOUBLE_INDEX_SCALE);
    static final long ARRAY_BASE =
        Unsafe.ARRAY_DOUBLE_BASE_OFFSET;

    @ForceInline
    static long arrayAddress(double[] a, int index) {
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
    public final LongVector viewAsIntegralLanes() {
        LaneType ilt = LaneType.DOUBLE.asIntegral();
        return (LongVector) asVectorRaw(ilt);
    }

    /**
     * {@inheritDoc} <!--workaround-->
     */
    @ForceInline
    @Override
    public final
    DoubleVector
    viewAsFloatingLanes() {
        return this;
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
     * java.util.Arrays#toString(double[]) Arrays.toString()},
     * as appropriate to the {@code double} array returned by
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
     * Class representing {@link DoubleVector}'s of the same {@link VectorShape VectorShape}.
     */
    /*package-private*/
    static final class DoubleSpecies extends AbstractSpecies<Double> {
        private DoubleSpecies(VectorShape shape,
                Class<? extends DoubleVector> vectorType,
                Class<? extends AbstractMask<Double>> maskType,
                Function<Object, DoubleVector> vectorFactory) {
            super(shape, LaneType.of(double.class),
                  vectorType, maskType,
                  vectorFactory);
            assert(this.elementSize() == Double.SIZE);
        }

        // Specializing overrides:

        @Override
        @ForceInline
        public final Class<Double> elementType() {
            return double.class;
        }

        @Override
        @ForceInline
        final Class<Double> genericElementType() {
            return Double.class;
        }

        @SuppressWarnings("unchecked")
        @Override
        @ForceInline
        public final Class<? extends DoubleVector> vectorType() {
            return (Class<? extends DoubleVector>) vectorType;
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
        final DoubleVector broadcastBits(long bits) {
            return (DoubleVector)
                VectorSupport.broadcastCoerced(
                    vectorType, double.class, laneCount,
                    bits, this,
                    (bits_, s_) -> s_.rvOp(i -> bits_));
        }

        /*package-private*/
        @ForceInline
        final DoubleVector broadcast(double e) {
            return broadcastBits(toBits(e));
        }

        @Override
        @ForceInline
        public final DoubleVector broadcast(long e) {
            return broadcastBits(longToElementBits(e));
        }

        /*package-private*/
        final @Override
        @ForceInline
        long longToElementBits(long value) {
            // Do the conversion, and then test it for failure.
            double e = (double) value;
            if ((long) e != value) {
                throw badElementBits(value, e);
            }
            return toBits(e);
        }

        /*package-private*/
        @ForceInline
        static long toIntegralChecked(double e, boolean convertToInt) {
            long value = convertToInt ? (int) e : (long) e;
            if ((double) value != e) {
                throw badArrayBits(e, convertToInt, value);
            }
            return value;
        }

        /* this non-public one is for internal conversions */
        @Override
        @ForceInline
        final DoubleVector fromIntValues(int[] values) {
            VectorIntrinsics.requireLength(values.length, laneCount);
            double[] va = new double[laneCount()];
            for (int i = 0; i < va.length; i++) {
                int lv = values[i];
                double v = (double) lv;
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
        public DoubleVector fromArray(Object a, int offset) {
            // User entry point:  Be careful with inputs.
            return DoubleVector
                .fromArray(this, (double[]) a, offset);
        }

        @ForceInline
        @Override final
        DoubleVector dummyVector() {
            return (DoubleVector) super.dummyVector();
        }

        /*package-private*/
        final @Override
        @ForceInline
        DoubleVector rvOp(RVOp f) {
            double[] res = new double[laneCount()];
            for (int i = 0; i < res.length; i++) {
                long bits = (long) f.apply(i);
                res[i] = fromBits(bits);
            }
            return dummyVector().vectorFactory(res);
        }

        DoubleVector vOp(FVOp f) {
            double[] res = new double[laneCount()];
            for (int i = 0; i < res.length; i++) {
                res[i] = f.apply(i);
            }
            return dummyVector().vectorFactory(res);
        }

        DoubleVector vOp(VectorMask<Double> m, FVOp f) {
            double[] res = new double[laneCount()];
            boolean[] mbits = ((AbstractMask<Double>)m).getBits();
            for (int i = 0; i < res.length; i++) {
                if (mbits[i]) {
                    res[i] = f.apply(i);
                }
            }
            return dummyVector().vectorFactory(res);
        }

        /*package-private*/
        @ForceInline
        <M> DoubleVector ldOp(M memory, int offset,
                                      FLdOp<M> f) {
            return dummyVector().ldOp(memory, offset, f);
        }

        /*package-private*/
        @ForceInline
        <M> DoubleVector ldOp(M memory, int offset,
                                      AbstractMask<Double> m,
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
                      AbstractMask<Double> m,
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
        public final DoubleVector zero() {
            if ((Class<?>) vectorType() == DoubleMaxVector.class)
                return DoubleMaxVector.ZERO;
            switch (vectorBitSize()) {
                case 64: return Double64Vector.ZERO;
                case 128: return Double128Vector.ZERO;
                case 256: return Double256Vector.ZERO;
                case 512: return Double512Vector.ZERO;
            }
            throw new AssertionError();
        }

        @Override
        @ForceInline
        public final DoubleVector iota() {
            if ((Class<?>) vectorType() == DoubleMaxVector.class)
                return DoubleMaxVector.IOTA;
            switch (vectorBitSize()) {
                case 64: return Double64Vector.IOTA;
                case 128: return Double128Vector.IOTA;
                case 256: return Double256Vector.IOTA;
                case 512: return Double512Vector.IOTA;
            }
            throw new AssertionError();
        }

        // Mask access
        @Override
        @ForceInline
        public final VectorMask<Double> maskAll(boolean bit) {
            if ((Class<?>) vectorType() == DoubleMaxVector.class)
                return DoubleMaxVector.DoubleMaxMask.maskAll(bit);
            switch (vectorBitSize()) {
                case 64: return Double64Vector.Double64Mask.maskAll(bit);
                case 128: return Double128Vector.Double128Mask.maskAll(bit);
                case 256: return Double256Vector.Double256Mask.maskAll(bit);
                case 512: return Double512Vector.Double512Mask.maskAll(bit);
            }
            throw new AssertionError();
        }
    }

    /**
     * Finds a species for an element type of {@code double} and shape.
     *
     * @param s the shape
     * @return a species for an element type of {@code double} and shape
     * @throws IllegalArgumentException if no such species exists for the shape
     */
    static DoubleSpecies species(VectorShape s) {
        Objects.requireNonNull(s);
        switch (s) {
            case S_64_BIT: return (DoubleSpecies) SPECIES_64;
            case S_128_BIT: return (DoubleSpecies) SPECIES_128;
            case S_256_BIT: return (DoubleSpecies) SPECIES_256;
            case S_512_BIT: return (DoubleSpecies) SPECIES_512;
            case S_Max_BIT: return (DoubleSpecies) SPECIES_MAX;
            default: throw new IllegalArgumentException("Bad shape: " + s);
        }
    }

    /** Species representing {@link DoubleVector}s of {@link VectorShape#S_64_BIT VectorShape.S_64_BIT}. */
    public static final VectorSpecies<Double> SPECIES_64
        = new DoubleSpecies(VectorShape.S_64_BIT,
                            Double64Vector.class,
                            Double64Vector.Double64Mask.class,
                            Double64Vector::new);

    /** Species representing {@link DoubleVector}s of {@link VectorShape#S_128_BIT VectorShape.S_128_BIT}. */
    public static final VectorSpecies<Double> SPECIES_128
        = new DoubleSpecies(VectorShape.S_128_BIT,
                            Double128Vector.class,
                            Double128Vector.Double128Mask.class,
                            Double128Vector::new);

    /** Species representing {@link DoubleVector}s of {@link VectorShape#S_256_BIT VectorShape.S_256_BIT}. */
    public static final VectorSpecies<Double> SPECIES_256
        = new DoubleSpecies(VectorShape.S_256_BIT,
                            Double256Vector.class,
                            Double256Vector.Double256Mask.class,
                            Double256Vector::new);

    /** Species representing {@link DoubleVector}s of {@link VectorShape#S_512_BIT VectorShape.S_512_BIT}. */
    public static final VectorSpecies<Double> SPECIES_512
        = new DoubleSpecies(VectorShape.S_512_BIT,
                            Double512Vector.class,
                            Double512Vector.Double512Mask.class,
                            Double512Vector::new);

    /** Species representing {@link DoubleVector}s of {@link VectorShape#S_Max_BIT VectorShape.S_Max_BIT}. */
    public static final VectorSpecies<Double> SPECIES_MAX
        = new DoubleSpecies(VectorShape.S_Max_BIT,
                            DoubleMaxVector.class,
                            DoubleMaxVector.DoubleMaxMask.class,
                            DoubleMaxVector::new);

    /**
     * Preferred species for {@link DoubleVector}s.
     * A preferred species is a species of maximal bit-size for the platform.
     */
    public static final VectorSpecies<Double> SPECIES_PREFERRED
        = (DoubleSpecies) VectorSpecies.ofPreferred(double.class);
}
