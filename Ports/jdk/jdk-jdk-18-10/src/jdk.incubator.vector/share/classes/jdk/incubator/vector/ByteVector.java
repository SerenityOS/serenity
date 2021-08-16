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
 * {@code byte} values.
 */
@SuppressWarnings("cast")  // warning: redundant cast
public abstract class ByteVector extends AbstractVector<Byte> {

    ByteVector(byte[] vec) {
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
    abstract byte[] vec();

    // Virtualized constructors

    /**
     * Build a vector directly using my own constructor.
     * It is an error if the array is aliased elsewhere.
     */
    /*package-private*/
    abstract ByteVector vectorFactory(byte[] vec);

    /**
     * Build a mask directly using my species.
     * It is an error if the array is aliased elsewhere.
     */
    /*package-private*/
    @ForceInline
    final
    AbstractMask<Byte> maskFactory(boolean[] bits) {
        return vspecies().maskFactory(bits);
    }

    // Constant loader (takes dummy as vector arg)
    interface FVOp {
        byte apply(int i);
    }

    /*package-private*/
    @ForceInline
    final
    ByteVector vOp(FVOp f) {
        byte[] res = new byte[length()];
        for (int i = 0; i < res.length; i++) {
            res[i] = f.apply(i);
        }
        return vectorFactory(res);
    }

    @ForceInline
    final
    ByteVector vOp(VectorMask<Byte> m, FVOp f) {
        byte[] res = new byte[length()];
        boolean[] mbits = ((AbstractMask<Byte>)m).getBits();
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
        byte apply(int i, byte a);
    }

    /*package-private*/
    abstract
    ByteVector uOp(FUnOp f);
    @ForceInline
    final
    ByteVector uOpTemplate(FUnOp f) {
        byte[] vec = vec();
        byte[] res = new byte[length()];
        for (int i = 0; i < res.length; i++) {
            res[i] = f.apply(i, vec[i]);
        }
        return vectorFactory(res);
    }

    /*package-private*/
    abstract
    ByteVector uOp(VectorMask<Byte> m,
                             FUnOp f);
    @ForceInline
    final
    ByteVector uOpTemplate(VectorMask<Byte> m,
                                     FUnOp f) {
        byte[] vec = vec();
        byte[] res = new byte[length()];
        boolean[] mbits = ((AbstractMask<Byte>)m).getBits();
        for (int i = 0; i < res.length; i++) {
            res[i] = mbits[i] ? f.apply(i, vec[i]) : vec[i];
        }
        return vectorFactory(res);
    }

    // Binary operator

    /*package-private*/
    interface FBinOp {
        byte apply(int i, byte a, byte b);
    }

    /*package-private*/
    abstract
    ByteVector bOp(Vector<Byte> o,
                             FBinOp f);
    @ForceInline
    final
    ByteVector bOpTemplate(Vector<Byte> o,
                                     FBinOp f) {
        byte[] res = new byte[length()];
        byte[] vec1 = this.vec();
        byte[] vec2 = ((ByteVector)o).vec();
        for (int i = 0; i < res.length; i++) {
            res[i] = f.apply(i, vec1[i], vec2[i]);
        }
        return vectorFactory(res);
    }

    /*package-private*/
    abstract
    ByteVector bOp(Vector<Byte> o,
                             VectorMask<Byte> m,
                             FBinOp f);
    @ForceInline
    final
    ByteVector bOpTemplate(Vector<Byte> o,
                                     VectorMask<Byte> m,
                                     FBinOp f) {
        byte[] res = new byte[length()];
        byte[] vec1 = this.vec();
        byte[] vec2 = ((ByteVector)o).vec();
        boolean[] mbits = ((AbstractMask<Byte>)m).getBits();
        for (int i = 0; i < res.length; i++) {
            res[i] = mbits[i] ? f.apply(i, vec1[i], vec2[i]) : vec1[i];
        }
        return vectorFactory(res);
    }

    // Ternary operator

    /*package-private*/
    interface FTriOp {
        byte apply(int i, byte a, byte b, byte c);
    }

    /*package-private*/
    abstract
    ByteVector tOp(Vector<Byte> o1,
                             Vector<Byte> o2,
                             FTriOp f);
    @ForceInline
    final
    ByteVector tOpTemplate(Vector<Byte> o1,
                                     Vector<Byte> o2,
                                     FTriOp f) {
        byte[] res = new byte[length()];
        byte[] vec1 = this.vec();
        byte[] vec2 = ((ByteVector)o1).vec();
        byte[] vec3 = ((ByteVector)o2).vec();
        for (int i = 0; i < res.length; i++) {
            res[i] = f.apply(i, vec1[i], vec2[i], vec3[i]);
        }
        return vectorFactory(res);
    }

    /*package-private*/
    abstract
    ByteVector tOp(Vector<Byte> o1,
                             Vector<Byte> o2,
                             VectorMask<Byte> m,
                             FTriOp f);
    @ForceInline
    final
    ByteVector tOpTemplate(Vector<Byte> o1,
                                     Vector<Byte> o2,
                                     VectorMask<Byte> m,
                                     FTriOp f) {
        byte[] res = new byte[length()];
        byte[] vec1 = this.vec();
        byte[] vec2 = ((ByteVector)o1).vec();
        byte[] vec3 = ((ByteVector)o2).vec();
        boolean[] mbits = ((AbstractMask<Byte>)m).getBits();
        for (int i = 0; i < res.length; i++) {
            res[i] = mbits[i] ? f.apply(i, vec1[i], vec2[i], vec3[i]) : vec1[i];
        }
        return vectorFactory(res);
    }

    // Reduction operator

    /*package-private*/
    abstract
    byte rOp(byte v, FBinOp f);
    @ForceInline
    final
    byte rOpTemplate(byte v, FBinOp f) {
        byte[] vec = vec();
        for (int i = 0; i < vec.length; i++) {
            v = f.apply(i, v, vec[i]);
        }
        return v;
    }

    // Memory reference

    /*package-private*/
    interface FLdOp<M> {
        byte apply(M memory, int offset, int i);
    }

    /*package-private*/
    @ForceInline
    final
    <M> ByteVector ldOp(M memory, int offset,
                                  FLdOp<M> f) {
        //dummy; no vec = vec();
        byte[] res = new byte[length()];
        for (int i = 0; i < res.length; i++) {
            res[i] = f.apply(memory, offset, i);
        }
        return vectorFactory(res);
    }

    /*package-private*/
    @ForceInline
    final
    <M> ByteVector ldOp(M memory, int offset,
                                  VectorMask<Byte> m,
                                  FLdOp<M> f) {
        //byte[] vec = vec();
        byte[] res = new byte[length()];
        boolean[] mbits = ((AbstractMask<Byte>)m).getBits();
        for (int i = 0; i < res.length; i++) {
            if (mbits[i]) {
                res[i] = f.apply(memory, offset, i);
            }
        }
        return vectorFactory(res);
    }

    interface FStOp<M> {
        void apply(M memory, int offset, int i, byte a);
    }

    /*package-private*/
    @ForceInline
    final
    <M> void stOp(M memory, int offset,
                  FStOp<M> f) {
        byte[] vec = vec();
        for (int i = 0; i < vec.length; i++) {
            f.apply(memory, offset, i, vec[i]);
        }
    }

    /*package-private*/
    @ForceInline
    final
    <M> void stOp(M memory, int offset,
                  VectorMask<Byte> m,
                  FStOp<M> f) {
        byte[] vec = vec();
        boolean[] mbits = ((AbstractMask<Byte>)m).getBits();
        for (int i = 0; i < vec.length; i++) {
            if (mbits[i]) {
                f.apply(memory, offset, i, vec[i]);
            }
        }
    }

    // Binary test

    /*package-private*/
    interface FBinTest {
        boolean apply(int cond, int i, byte a, byte b);
    }

    /*package-private*/
    @ForceInline
    final
    AbstractMask<Byte> bTest(int cond,
                                  Vector<Byte> o,
                                  FBinTest f) {
        byte[] vec1 = vec();
        byte[] vec2 = ((ByteVector)o).vec();
        boolean[] bits = new boolean[length()];
        for (int i = 0; i < length(); i++){
            bits[i] = f.apply(cond, i, vec1[i], vec2[i]);
        }
        return maskFactory(bits);
    }

    /*package-private*/
    @Override
    abstract ByteSpecies vspecies();

    /*package-private*/
    @ForceInline
    static long toBits(byte e) {
        return  e;
    }

    /*package-private*/
    @ForceInline
    static byte fromBits(long bits) {
        return ((byte)bits);
    }

    // Static factories (other than memory operations)

    // Note: A surprising behavior in javadoc
    // sometimes makes a lone /** {@inheritDoc} */
    // comment drop the method altogether,
    // apparently if the method mentions an
    // parameter or return type of Vector<Byte>
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
    public static ByteVector zero(VectorSpecies<Byte> species) {
        ByteSpecies vsp = (ByteSpecies) species;
        return VectorSupport.broadcastCoerced(vsp.vectorType(), byte.class, species.length(),
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
     * {@code ByteVector.broadcast(this.species(), e)}.
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
    public abstract ByteVector broadcast(byte e);

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
    public static ByteVector broadcast(VectorSpecies<Byte> species, byte e) {
        ByteSpecies vsp = (ByteSpecies) species;
        return vsp.broadcast(e);
    }

    /*package-private*/
    @ForceInline
    final ByteVector broadcastTemplate(byte e) {
        ByteSpecies vsp = vspecies();
        return vsp.broadcast(e);
    }

    /**
     * {@inheritDoc} <!--workaround-->
     * @apiNote
     * When working with vector subtypes like {@code ByteVector},
     * {@linkplain #broadcast(byte) the more strongly typed method}
     * is typically selected.  It can be explicitly selected
     * using a cast: {@code v.broadcast((byte)e)}.
     * The two expressions will produce numerically identical results.
     */
    @Override
    public abstract ByteVector broadcast(long e);

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
     * @see #broadcast(VectorSpecies,byte)
     * @see VectorSpecies#checkValue(long)
     */
    @ForceInline
    public static ByteVector broadcast(VectorSpecies<Byte> species, long e) {
        ByteSpecies vsp = (ByteSpecies) species;
        return vsp.broadcast(e);
    }

    /*package-private*/
    @ForceInline
    final ByteVector broadcastTemplate(long e) {
        return vspecies().broadcast(e);
    }

    // Unary lanewise support

    /**
     * {@inheritDoc} <!--workaround-->
     */
    public abstract
    ByteVector lanewise(VectorOperators.Unary op);

    @ForceInline
    final
    ByteVector lanewiseTemplate(VectorOperators.Unary op) {
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
            opc, getClass(), byte.class, length(),
            this,
            UN_IMPL.find(op, opc, (opc_) -> {
              switch (opc_) {
                case VECTOR_OP_NEG: return v0 ->
                        v0.uOp((i, a) -> (byte) -a);
                case VECTOR_OP_ABS: return v0 ->
                        v0.uOp((i, a) -> (byte) Math.abs(a));
                default: return null;
              }}));
    }
    private static final
    ImplCache<Unary,UnaryOperator<ByteVector>> UN_IMPL
        = new ImplCache<>(Unary.class, ByteVector.class);

    /**
     * {@inheritDoc} <!--workaround-->
     */
    @ForceInline
    public final
    ByteVector lanewise(VectorOperators.Unary op,
                                  VectorMask<Byte> m) {
        return blend(lanewise(op), m);
    }

    // Binary lanewise support

    /**
     * {@inheritDoc} <!--workaround-->
     * @see #lanewise(VectorOperators.Binary,byte)
     * @see #lanewise(VectorOperators.Binary,byte,VectorMask)
     */
    @Override
    public abstract
    ByteVector lanewise(VectorOperators.Binary op,
                                  Vector<Byte> v);
    @ForceInline
    final
    ByteVector lanewiseTemplate(VectorOperators.Binary op,
                                          Vector<Byte> v) {
        ByteVector that = (ByteVector) v;
        that.check(this);
        if (opKind(op, VO_SPECIAL  | VO_SHIFT)) {
            if (op == FIRST_NONZERO) {
                // FIXME: Support this in the JIT.
                VectorMask<Byte> thisNZ
                    = this.viewAsIntegralLanes().compare(NE, (byte) 0);
                that = that.blend((byte) 0, thisNZ.cast(vspecies()));
                op = OR_UNCHECKED;
            }
            if (opKind(op, VO_SHIFT)) {
                // As per shift specification for Java, mask the shift count.
                // This allows the JIT to ignore some ISA details.
                that = that.lanewise(AND, SHIFT_MASK);
            }
            if (op == ROR || op == ROL) {  // FIXME: JIT should do this
                ByteVector neg = that.lanewise(NEG);
                ByteVector hi = this.lanewise(LSHL, (op == ROR) ? neg : that);
                ByteVector lo = this.lanewise(LSHR, (op == ROR) ? that : neg);
                return hi.lanewise(OR, lo);
            } else if (op == AND_NOT) {
                // FIXME: Support this in the JIT.
                that = that.lanewise(NOT);
                op = AND;
            } else if (op == DIV) {
                VectorMask<Byte> eqz = that.eq((byte)0);
                if (eqz.anyTrue()) {
                    throw that.divZeroException();
                }
            }
        }
        int opc = opCode(op);
        return VectorSupport.binaryOp(
            opc, getClass(), byte.class, length(),
            this, that,
            BIN_IMPL.find(op, opc, (opc_) -> {
              switch (opc_) {
                case VECTOR_OP_ADD: return (v0, v1) ->
                        v0.bOp(v1, (i, a, b) -> (byte)(a + b));
                case VECTOR_OP_SUB: return (v0, v1) ->
                        v0.bOp(v1, (i, a, b) -> (byte)(a - b));
                case VECTOR_OP_MUL: return (v0, v1) ->
                        v0.bOp(v1, (i, a, b) -> (byte)(a * b));
                case VECTOR_OP_DIV: return (v0, v1) ->
                        v0.bOp(v1, (i, a, b) -> (byte)(a / b));
                case VECTOR_OP_MAX: return (v0, v1) ->
                        v0.bOp(v1, (i, a, b) -> (byte)Math.max(a, b));
                case VECTOR_OP_MIN: return (v0, v1) ->
                        v0.bOp(v1, (i, a, b) -> (byte)Math.min(a, b));
                case VECTOR_OP_AND: return (v0, v1) ->
                        v0.bOp(v1, (i, a, b) -> (byte)(a & b));
                case VECTOR_OP_OR: return (v0, v1) ->
                        v0.bOp(v1, (i, a, b) -> (byte)(a | b));
                case VECTOR_OP_XOR: return (v0, v1) ->
                        v0.bOp(v1, (i, a, b) -> (byte)(a ^ b));
                case VECTOR_OP_LSHIFT: return (v0, v1) ->
                        v0.bOp(v1, (i, a, n) -> (byte)(a << n));
                case VECTOR_OP_RSHIFT: return (v0, v1) ->
                        v0.bOp(v1, (i, a, n) -> (byte)(a >> n));
                case VECTOR_OP_URSHIFT: return (v0, v1) ->
                        v0.bOp(v1, (i, a, n) -> (byte)((a & LSHR_SETUP_MASK) >>> n));
                default: return null;
                }}));
    }
    private static final
    ImplCache<Binary,BinaryOperator<ByteVector>> BIN_IMPL
        = new ImplCache<>(Binary.class, ByteVector.class);

    /**
     * {@inheritDoc} <!--workaround-->
     * @see #lanewise(VectorOperators.Binary,byte,VectorMask)
     */
    @ForceInline
    public final
    ByteVector lanewise(VectorOperators.Binary op,
                                  Vector<Byte> v,
                                  VectorMask<Byte> m) {
        ByteVector that = (ByteVector) v;
        if (op == DIV) {
            VectorMask<Byte> eqz = that.eq((byte)0);
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
     * @see #lanewise(VectorOperators.Binary,byte,VectorMask)
     */
    @ForceInline
    public final
    ByteVector lanewise(VectorOperators.Binary op,
                                  byte e) {
        if (opKind(op, VO_SHIFT) && (byte)(int)e == e) {
            return lanewiseShift(op, (int) e);
        }
        if (op == AND_NOT) {
            op = AND; e = (byte) ~e;
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
     * @see #lanewise(VectorOperators.Binary,byte)
     */
    @ForceInline
    public final
    ByteVector lanewise(VectorOperators.Binary op,
                                  byte e,
                                  VectorMask<Byte> m) {
        return blend(lanewise(op, e), m);
    }

    /**
     * {@inheritDoc} <!--workaround-->
     * @apiNote
     * When working with vector subtypes like {@code ByteVector},
     * {@linkplain #lanewise(VectorOperators.Binary,byte)
     * the more strongly typed method}
     * is typically selected.  It can be explicitly selected
     * using a cast: {@code v.lanewise(op,(byte)e)}.
     * The two expressions will produce numerically identical results.
     */
    @ForceInline
    public final
    ByteVector lanewise(VectorOperators.Binary op,
                                  long e) {
        byte e1 = (byte) e;
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
     * When working with vector subtypes like {@code ByteVector},
     * {@linkplain #lanewise(VectorOperators.Binary,byte,VectorMask)
     * the more strongly typed method}
     * is typically selected.  It can be explicitly selected
     * using a cast: {@code v.lanewise(op,(byte)e,m)}.
     * The two expressions will produce numerically identical results.
     */
    @ForceInline
    public final
    ByteVector lanewise(VectorOperators.Binary op,
                                  long e, VectorMask<Byte> m) {
        return blend(lanewise(op, e), m);
    }

    /*package-private*/
    abstract ByteVector
    lanewiseShift(VectorOperators.Binary op, int e);

    /*package-private*/
    @ForceInline
    final ByteVector
    lanewiseShiftTemplate(VectorOperators.Binary op, int e) {
        // Special handling for these.  FIXME: Refactor?
        assert(opKind(op, VO_SHIFT));
        // As per shift specification for Java, mask the shift count.
        e &= SHIFT_MASK;
        if (op == ROR || op == ROL) {  // FIXME: JIT should do this
            ByteVector hi = this.lanewise(LSHL, (op == ROR) ? -e : e);
            ByteVector lo = this.lanewise(LSHR, (op == ROR) ? e : -e);
            return hi.lanewise(OR, lo);
        }
        int opc = opCode(op);
        return VectorSupport.broadcastInt(
            opc, getClass(), byte.class, length(),
            this, e,
            BIN_INT_IMPL.find(op, opc, (opc_) -> {
              switch (opc_) {
                case VECTOR_OP_LSHIFT: return (v, n) ->
                        v.uOp((i, a) -> (byte)(a << n));
                case VECTOR_OP_RSHIFT: return (v, n) ->
                        v.uOp((i, a) -> (byte)(a >> n));
                case VECTOR_OP_URSHIFT: return (v, n) ->
                        v.uOp((i, a) -> (byte)((a & LSHR_SETUP_MASK) >>> n));
                default: return null;
                }}));
    }
    private static final
    ImplCache<Binary,VectorBroadcastIntOp<ByteVector>> BIN_INT_IMPL
        = new ImplCache<>(Binary.class, ByteVector.class);

    // As per shift specification for Java, mask the shift count.
    // We mask 0X3F (long), 0X1F (int), 0x0F (short), 0x7 (byte).
    // The latter two maskings go beyond the JLS, but seem reasonable
    // since our lane types are first-class types, not just dressed
    // up ints.
    private static final int SHIFT_MASK = (Byte.SIZE - 1);
    // Also simulate >>> on sub-word variables with a mask.
    private static final int LSHR_SETUP_MASK = ((1 << Byte.SIZE) - 1);

    // Ternary lanewise support

    // Ternary operators come in eight variations:
    //   lanewise(op, [broadcast(e1)|v1], [broadcast(e2)|v2])
    //   lanewise(op, [broadcast(e1)|v1], [broadcast(e2)|v2], mask)

    // It is annoying to support all of these variations of masking
    // and broadcast, but it would be more surprising not to continue
    // the obvious pattern started by unary and binary.

   /**
     * {@inheritDoc} <!--workaround-->
     * @see #lanewise(VectorOperators.Ternary,byte,byte,VectorMask)
     * @see #lanewise(VectorOperators.Ternary,Vector,byte,VectorMask)
     * @see #lanewise(VectorOperators.Ternary,byte,Vector,VectorMask)
     * @see #lanewise(VectorOperators.Ternary,byte,byte)
     * @see #lanewise(VectorOperators.Ternary,Vector,byte)
     * @see #lanewise(VectorOperators.Ternary,byte,Vector)
     */
    @Override
    public abstract
    ByteVector lanewise(VectorOperators.Ternary op,
                                                  Vector<Byte> v1,
                                                  Vector<Byte> v2);
    @ForceInline
    final
    ByteVector lanewiseTemplate(VectorOperators.Ternary op,
                                          Vector<Byte> v1,
                                          Vector<Byte> v2) {
        ByteVector that = (ByteVector) v1;
        ByteVector tother = (ByteVector) v2;
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
            opc, getClass(), byte.class, length(),
            this, that, tother,
            TERN_IMPL.find(op, opc, (opc_) -> {
              switch (opc_) {
                default: return null;
                }}));
    }
    private static final
    ImplCache<Ternary,TernaryOperation<ByteVector>> TERN_IMPL
        = new ImplCache<>(Ternary.class, ByteVector.class);

    /**
     * {@inheritDoc} <!--workaround-->
     * @see #lanewise(VectorOperators.Ternary,byte,byte,VectorMask)
     * @see #lanewise(VectorOperators.Ternary,Vector,byte,VectorMask)
     * @see #lanewise(VectorOperators.Ternary,byte,Vector,VectorMask)
     */
    @ForceInline
    public final
    ByteVector lanewise(VectorOperators.Ternary op,
                                  Vector<Byte> v1,
                                  Vector<Byte> v2,
                                  VectorMask<Byte> m) {
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
     * @see #lanewise(VectorOperators.Ternary,byte,byte,VectorMask)
     */
    @ForceInline
    public final
    ByteVector lanewise(VectorOperators.Ternary op, //(op,e1,e2)
                                  byte e1,
                                  byte e2) {
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
     * @see #lanewise(VectorOperators.Ternary,byte,byte)
     */
    @ForceInline
    public final
    ByteVector lanewise(VectorOperators.Ternary op, //(op,e1,e2,m)
                                  byte e1,
                                  byte e2,
                                  VectorMask<Byte> m) {
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
     * @see #lanewise(VectorOperators.Ternary,byte,byte)
     * @see #lanewise(VectorOperators.Ternary,Vector,byte,VectorMask)
     */
    @ForceInline
    public final
    ByteVector lanewise(VectorOperators.Ternary op, //(op,v1,e2)
                                  Vector<Byte> v1,
                                  byte e2) {
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
     * @see #lanewise(VectorOperators.Ternary,byte,byte,VectorMask)
     * @see #lanewise(VectorOperators.Ternary,Vector,byte)
     */
    @ForceInline
    public final
    ByteVector lanewise(VectorOperators.Ternary op, //(op,v1,e2,m)
                                  Vector<Byte> v1,
                                  byte e2,
                                  VectorMask<Byte> m) {
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
     * @see #lanewise(VectorOperators.Ternary,byte,Vector,VectorMask)
     */
    @ForceInline
    public final
    ByteVector lanewise(VectorOperators.Ternary op, //(op,e1,v2)
                                  byte e1,
                                  Vector<Byte> v2) {
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
     * @see #lanewise(VectorOperators.Ternary,byte,Vector)
     */
    @ForceInline
    public final
    ByteVector lanewise(VectorOperators.Ternary op, //(op,e1,v2,m)
                                  byte e1,
                                  Vector<Byte> v2,
                                  VectorMask<Byte> m) {
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
     * @see #add(byte)
     */
    @Override
    @ForceInline
    public final ByteVector add(Vector<Byte> v) {
        return lanewise(ADD, v);
    }

    /**
     * Adds this vector to the broadcast of an input scalar.
     *
     * This is a lane-wise binary operation which applies
     * the primitive addition operation ({@code +}) to each lane.
     *
     * This method is also equivalent to the expression
     * {@link #lanewise(VectorOperators.Binary,byte)
     *    lanewise}{@code (}{@link VectorOperators#ADD
     *    ADD}{@code , e)}.
     *
     * @param e the input scalar
     * @return the result of adding each lane of this vector to the scalar
     * @see #add(Vector)
     * @see #broadcast(byte)
     * @see #add(byte,VectorMask)
     * @see VectorOperators#ADD
     * @see #lanewise(VectorOperators.Binary,Vector)
     * @see #lanewise(VectorOperators.Binary,byte)
     */
    @ForceInline
    public final
    ByteVector add(byte e) {
        return lanewise(ADD, e);
    }

    /**
     * {@inheritDoc} <!--workaround-->
     * @see #add(byte,VectorMask)
     */
    @Override
    @ForceInline
    public final ByteVector add(Vector<Byte> v,
                                          VectorMask<Byte> m) {
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
     * {@link #lanewise(VectorOperators.Binary,byte,VectorMask)
     *    lanewise}{@code (}{@link VectorOperators#ADD
     *    ADD}{@code , s, m)}.
     *
     * @param e the input scalar
     * @param m the mask controlling lane selection
     * @return the result of adding each lane of this vector to the scalar
     * @see #add(Vector,VectorMask)
     * @see #broadcast(byte)
     * @see #add(byte)
     * @see VectorOperators#ADD
     * @see #lanewise(VectorOperators.Binary,Vector)
     * @see #lanewise(VectorOperators.Binary,byte)
     */
    @ForceInline
    public final ByteVector add(byte e,
                                          VectorMask<Byte> m) {
        return lanewise(ADD, e, m);
    }

    /**
     * {@inheritDoc} <!--workaround-->
     * @see #sub(byte)
     */
    @Override
    @ForceInline
    public final ByteVector sub(Vector<Byte> v) {
        return lanewise(SUB, v);
    }

    /**
     * Subtracts an input scalar from this vector.
     *
     * This is a masked lane-wise binary operation which applies
     * the primitive subtraction operation ({@code -}) to each lane.
     *
     * This method is also equivalent to the expression
     * {@link #lanewise(VectorOperators.Binary,byte)
     *    lanewise}{@code (}{@link VectorOperators#SUB
     *    SUB}{@code , e)}.
     *
     * @param e the input scalar
     * @return the result of subtracting the scalar from each lane of this vector
     * @see #sub(Vector)
     * @see #broadcast(byte)
     * @see #sub(byte,VectorMask)
     * @see VectorOperators#SUB
     * @see #lanewise(VectorOperators.Binary,Vector)
     * @see #lanewise(VectorOperators.Binary,byte)
     */
    @ForceInline
    public final ByteVector sub(byte e) {
        return lanewise(SUB, e);
    }

    /**
     * {@inheritDoc} <!--workaround-->
     * @see #sub(byte,VectorMask)
     */
    @Override
    @ForceInline
    public final ByteVector sub(Vector<Byte> v,
                                          VectorMask<Byte> m) {
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
     * {@link #lanewise(VectorOperators.Binary,byte,VectorMask)
     *    lanewise}{@code (}{@link VectorOperators#SUB
     *    SUB}{@code , s, m)}.
     *
     * @param e the input scalar
     * @param m the mask controlling lane selection
     * @return the result of subtracting the scalar from each lane of this vector
     * @see #sub(Vector,VectorMask)
     * @see #broadcast(byte)
     * @see #sub(byte)
     * @see VectorOperators#SUB
     * @see #lanewise(VectorOperators.Binary,Vector)
     * @see #lanewise(VectorOperators.Binary,byte)
     */
    @ForceInline
    public final ByteVector sub(byte e,
                                          VectorMask<Byte> m) {
        return lanewise(SUB, e, m);
    }

    /**
     * {@inheritDoc} <!--workaround-->
     * @see #mul(byte)
     */
    @Override
    @ForceInline
    public final ByteVector mul(Vector<Byte> v) {
        return lanewise(MUL, v);
    }

    /**
     * Multiplies this vector by the broadcast of an input scalar.
     *
     * This is a lane-wise binary operation which applies
     * the primitive multiplication operation ({@code *}) to each lane.
     *
     * This method is also equivalent to the expression
     * {@link #lanewise(VectorOperators.Binary,byte)
     *    lanewise}{@code (}{@link VectorOperators#MUL
     *    MUL}{@code , e)}.
     *
     * @param e the input scalar
     * @return the result of multiplying this vector by the given scalar
     * @see #mul(Vector)
     * @see #broadcast(byte)
     * @see #mul(byte,VectorMask)
     * @see VectorOperators#MUL
     * @see #lanewise(VectorOperators.Binary,Vector)
     * @see #lanewise(VectorOperators.Binary,byte)
     */
    @ForceInline
    public final ByteVector mul(byte e) {
        return lanewise(MUL, e);
    }

    /**
     * {@inheritDoc} <!--workaround-->
     * @see #mul(byte,VectorMask)
     */
    @Override
    @ForceInline
    public final ByteVector mul(Vector<Byte> v,
                                          VectorMask<Byte> m) {
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
     * {@link #lanewise(VectorOperators.Binary,byte,VectorMask)
     *    lanewise}{@code (}{@link VectorOperators#MUL
     *    MUL}{@code , s, m)}.
     *
     * @param e the input scalar
     * @param m the mask controlling lane selection
     * @return the result of muling each lane of this vector to the scalar
     * @see #mul(Vector,VectorMask)
     * @see #broadcast(byte)
     * @see #mul(byte)
     * @see VectorOperators#MUL
     * @see #lanewise(VectorOperators.Binary,Vector)
     * @see #lanewise(VectorOperators.Binary,byte)
     */
    @ForceInline
    public final ByteVector mul(byte e,
                                          VectorMask<Byte> m) {
        return lanewise(MUL, e, m);
    }

    /**
     * {@inheritDoc} <!--workaround-->
     * @apiNote If there is a zero divisor, {@code
     * ArithmeticException} will be thrown.
     */
    @Override
    @ForceInline
    public final ByteVector div(Vector<Byte> v) {
        return lanewise(DIV, v);
    }

    /**
     * Divides this vector by the broadcast of an input scalar.
     *
     * This is a lane-wise binary operation which applies
     * the primitive division operation ({@code /}) to each lane.
     *
     * This method is also equivalent to the expression
     * {@link #lanewise(VectorOperators.Binary,byte)
     *    lanewise}{@code (}{@link VectorOperators#DIV
     *    DIV}{@code , e)}.
     *
     * @apiNote If there is a zero divisor, {@code
     * ArithmeticException} will be thrown.
     *
     * @param e the input scalar
     * @return the result of dividing each lane of this vector by the scalar
     * @see #div(Vector)
     * @see #broadcast(byte)
     * @see #div(byte,VectorMask)
     * @see VectorOperators#DIV
     * @see #lanewise(VectorOperators.Binary,Vector)
     * @see #lanewise(VectorOperators.Binary,byte)
     */
    @ForceInline
    public final ByteVector div(byte e) {
        return lanewise(DIV, e);
    }

    /**
     * {@inheritDoc} <!--workaround-->
     * @see #div(byte,VectorMask)
     * @apiNote If there is a zero divisor, {@code
     * ArithmeticException} will be thrown.
     */
    @Override
    @ForceInline
    public final ByteVector div(Vector<Byte> v,
                                          VectorMask<Byte> m) {
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
     * {@link #lanewise(VectorOperators.Binary,byte,VectorMask)
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
     * @see #broadcast(byte)
     * @see #div(byte)
     * @see VectorOperators#DIV
     * @see #lanewise(VectorOperators.Binary,Vector)
     * @see #lanewise(VectorOperators.Binary,byte)
     */
    @ForceInline
    public final ByteVector div(byte e,
                                          VectorMask<Byte> m) {
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
    public final ByteVector min(Vector<Byte> v) {
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
     * {@link #lanewise(VectorOperators.Binary,byte)
     *    lanewise}{@code (}{@link VectorOperators#MIN
     *    MIN}{@code , e)}.
     *
     * @param e the input scalar
     * @return the result of multiplying this vector by the given scalar
     * @see #min(Vector)
     * @see #broadcast(byte)
     * @see VectorOperators#MIN
     * @see #lanewise(VectorOperators.Binary,byte,VectorMask)
     */
    @ForceInline
    public final ByteVector min(byte e) {
        return lanewise(MIN, e);
    }

    /**
     * {@inheritDoc} <!--workaround-->
     */
    @Override
    @ForceInline
    public final ByteVector max(Vector<Byte> v) {
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
     * {@link #lanewise(VectorOperators.Binary,byte)
     *    lanewise}{@code (}{@link VectorOperators#MAX
     *    MAX}{@code , e)}.
     *
     * @param e the input scalar
     * @return the result of multiplying this vector by the given scalar
     * @see #max(Vector)
     * @see #broadcast(byte)
     * @see VectorOperators#MAX
     * @see #lanewise(VectorOperators.Binary,byte,VectorMask)
     */
    @ForceInline
    public final ByteVector max(byte e) {
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
     * @see #and(byte)
     * @see #or(Vector)
     * @see #not()
     * @see VectorOperators#AND
     * @see #lanewise(VectorOperators.Binary,Vector,VectorMask)
     */
    @ForceInline
    public final ByteVector and(Vector<Byte> v) {
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
    public final ByteVector and(byte e) {
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
     * @see #or(byte)
     * @see #and(Vector)
     * @see #not()
     * @see VectorOperators#OR
     * @see #lanewise(VectorOperators.Binary,Vector,VectorMask)
     */
    @ForceInline
    public final ByteVector or(Vector<Byte> v) {
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
    public final ByteVector or(byte e) {
        return lanewise(OR, e);
    }



    /// UNARY METHODS

    /**
     * {@inheritDoc} <!--workaround-->
     */
    @Override
    @ForceInline
    public final
    ByteVector neg() {
        return lanewise(NEG);
    }

    /**
     * {@inheritDoc} <!--workaround-->
     */
    @Override
    @ForceInline
    public final
    ByteVector abs() {
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
    public final ByteVector not() {
        return lanewise(NOT);
    }


    /// COMPARISONS

    /**
     * {@inheritDoc} <!--workaround-->
     */
    @Override
    @ForceInline
    public final
    VectorMask<Byte> eq(Vector<Byte> v) {
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
     * @see #compare(VectorOperators.Comparison,byte)
     */
    @ForceInline
    public final
    VectorMask<Byte> eq(byte e) {
        return compare(EQ, e);
    }

    /**
     * {@inheritDoc} <!--workaround-->
     */
    @Override
    @ForceInline
    public final
    VectorMask<Byte> lt(Vector<Byte> v) {
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
     * @see #compare(VectorOperators.Comparison,byte)
     */
    @ForceInline
    public final
    VectorMask<Byte> lt(byte e) {
        return compare(LT, e);
    }

    /**
     * {@inheritDoc} <!--workaround-->
     */
    @Override
    public abstract
    VectorMask<Byte> test(VectorOperators.Test op);

    /*package-private*/
    @ForceInline
    final
    <M extends VectorMask<Byte>>
    M testTemplate(Class<M> maskType, Test op) {
        ByteSpecies vsp = vspecies();
        if (opKind(op, VO_SPECIAL)) {
            ByteVector bits = this.viewAsIntegralLanes();
            VectorMask<Byte> m;
            if (op == IS_DEFAULT) {
                m = bits.compare(EQ, (byte) 0);
            } else if (op == IS_NEGATIVE) {
                m = bits.compare(LT, (byte) 0);
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
    VectorMask<Byte> test(VectorOperators.Test op,
                                  VectorMask<Byte> m) {
        return test(op).and(m);
    }

    /**
     * {@inheritDoc} <!--workaround-->
     */
    @Override
    public abstract
    VectorMask<Byte> compare(VectorOperators.Comparison op, Vector<Byte> v);

    /*package-private*/
    @ForceInline
    final
    <M extends VectorMask<Byte>>
    M compareTemplate(Class<M> maskType, Comparison op, Vector<Byte> v) {
        Objects.requireNonNull(v);
        ByteSpecies vsp = vspecies();
        ByteVector that = (ByteVector) v;
        that.check(this);
        int opc = opCode(op);
        return VectorSupport.compare(
            opc, getClass(), maskType, byte.class, length(),
            this, that,
            (cond, v0, v1) -> {
                AbstractMask<Byte> m
                    = v0.bTest(cond, v1, (cond_, i, a, b)
                               -> compareWithOp(cond, a, b));
                @SuppressWarnings("unchecked")
                M m2 = (M) m;
                return m2;
            });
    }

    @ForceInline
    private static boolean compareWithOp(int cond, byte a, byte b) {
        return switch (cond) {
            case BT_eq -> a == b;
            case BT_ne -> a != b;
            case BT_lt -> a < b;
            case BT_le -> a <= b;
            case BT_gt -> a > b;
            case BT_ge -> a >= b;
            case BT_ult -> Byte.compareUnsigned(a, b) < 0;
            case BT_ule -> Byte.compareUnsigned(a, b) <= 0;
            case BT_ugt -> Byte.compareUnsigned(a, b) > 0;
            case BT_uge -> Byte.compareUnsigned(a, b) >= 0;
            default -> throw new AssertionError();
        };
    }

    /**
     * {@inheritDoc} <!--workaround-->
     */
    @Override
    @ForceInline
    public final
    VectorMask<Byte> compare(VectorOperators.Comparison op,
                                  Vector<Byte> v,
                                  VectorMask<Byte> m) {
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
     * @see ByteVector#compare(VectorOperators.Comparison,Vector)
     * @see #eq(byte)
     * @see #lt(byte)
     */
    public abstract
    VectorMask<Byte> compare(Comparison op, byte e);

    /*package-private*/
    @ForceInline
    final
    <M extends VectorMask<Byte>>
    M compareTemplate(Class<M> maskType, Comparison op, byte e) {
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
     * @see ByteVector#compare(VectorOperators.Comparison,Vector,VectorMask)
     */
    @ForceInline
    public final VectorMask<Byte> compare(VectorOperators.Comparison op,
                                               byte e,
                                               VectorMask<Byte> m) {
        return compare(op, e).and(m);
    }

    /**
     * {@inheritDoc} <!--workaround-->
     */
    @Override
    public abstract
    VectorMask<Byte> compare(Comparison op, long e);

    /*package-private*/
    @ForceInline
    final
    <M extends VectorMask<Byte>>
    M compareTemplate(Class<M> maskType, Comparison op, long e) {
        return compareTemplate(maskType, op, broadcast(e));
    }

    /**
     * {@inheritDoc} <!--workaround-->
     */
    @Override
    @ForceInline
    public final
    VectorMask<Byte> compare(Comparison op, long e, VectorMask<Byte> m) {
        return compare(op, broadcast(e), m);
    }



    /**
     * {@inheritDoc} <!--workaround-->
     */
    @Override public abstract
    ByteVector blend(Vector<Byte> v, VectorMask<Byte> m);

    /*package-private*/
    @ForceInline
    final
    <M extends VectorMask<Byte>>
    ByteVector
    blendTemplate(Class<M> maskType, ByteVector v, M m) {
        v.check(this);
        return VectorSupport.blend(
            getClass(), maskType, byte.class, length(),
            this, v, m,
            (v0, v1, m_) -> v0.bOp(v1, m_, (i, a, b) -> b));
    }

    /**
     * {@inheritDoc} <!--workaround-->
     */
    @Override public abstract ByteVector addIndex(int scale);

    /*package-private*/
    @ForceInline
    final ByteVector addIndexTemplate(int scale) {
        ByteSpecies vsp = vspecies();
        // make sure VLENGTH*scale doesn't overflow:
        vsp.checkScale(scale);
        return VectorSupport.indexVector(
            getClass(), byte.class, length(),
            this, scale, vsp,
            (v, scale_, s)
            -> {
                // If the platform doesn't support an INDEX
                // instruction directly, load IOTA from memory
                // and multiply.
                ByteVector iota = s.iota();
                byte sc = (byte) scale_;
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
    public final ByteVector blend(byte e,
                                            VectorMask<Byte> m) {
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
    public final ByteVector blend(long e,
                                            VectorMask<Byte> m) {
        return blend(broadcast(e), m);
    }

    /**
     * {@inheritDoc} <!--workaround-->
     */
    @Override
    public abstract
    ByteVector slice(int origin, Vector<Byte> v1);

    /*package-private*/
    final
    @ForceInline
    ByteVector sliceTemplate(int origin, Vector<Byte> v1) {
        ByteVector that = (ByteVector) v1;
        that.check(this);
        Objects.checkIndex(origin, length() + 1);
        VectorShuffle<Byte> iota = iotaShuffle();
        VectorMask<Byte> blendMask = iota.toVector().compare(VectorOperators.LT, (broadcast((byte)(length() - origin))));
        iota = iotaShuffle(origin, 1, true);
        return that.rearrange(iota).blend(this.rearrange(iota), blendMask);
    }

    /**
     * {@inheritDoc} <!--workaround-->
     */
    @Override
    @ForceInline
    public final
    ByteVector slice(int origin,
                               Vector<Byte> w,
                               VectorMask<Byte> m) {
        return broadcast(0).blend(slice(origin, w), m);
    }

    /**
     * {@inheritDoc} <!--workaround-->
     */
    @Override
    public abstract
    ByteVector slice(int origin);

    /*package-private*/
    final
    @ForceInline
    ByteVector sliceTemplate(int origin) {
        Objects.checkIndex(origin, length() + 1);
        VectorShuffle<Byte> iota = iotaShuffle();
        VectorMask<Byte> blendMask = iota.toVector().compare(VectorOperators.LT, (broadcast((byte)(length() - origin))));
        iota = iotaShuffle(origin, 1, true);
        return vspecies().zero().blend(this.rearrange(iota), blendMask);
    }

    /**
     * {@inheritDoc} <!--workaround-->
     */
    @Override
    public abstract
    ByteVector unslice(int origin, Vector<Byte> w, int part);

    /*package-private*/
    final
    @ForceInline
    ByteVector
    unsliceTemplate(int origin, Vector<Byte> w, int part) {
        ByteVector that = (ByteVector) w;
        that.check(this);
        Objects.checkIndex(origin, length() + 1);
        VectorShuffle<Byte> iota = iotaShuffle();
        VectorMask<Byte> blendMask = iota.toVector().compare((part == 0) ? VectorOperators.GE : VectorOperators.LT,
                                                                  (broadcast((byte)(origin))));
        iota = iotaShuffle(-origin, 1, true);
        return that.blend(this.rearrange(iota), blendMask);
    }

    /*package-private*/
    final
    @ForceInline
    <M extends VectorMask<Byte>>
    ByteVector
    unsliceTemplate(Class<M> maskType, int origin, Vector<Byte> w, int part, M m) {
        ByteVector that = (ByteVector) w;
        that.check(this);
        ByteVector slice = that.sliceTemplate(origin, that);
        slice = slice.blendTemplate(maskType, this, m);
        return slice.unsliceTemplate(origin, w, part);
    }

    /**
     * {@inheritDoc} <!--workaround-->
     */
    @Override
    public abstract
    ByteVector unslice(int origin, Vector<Byte> w, int part, VectorMask<Byte> m);

    /**
     * {@inheritDoc} <!--workaround-->
     */
    @Override
    public abstract
    ByteVector unslice(int origin);

    /*package-private*/
    final
    @ForceInline
    ByteVector
    unsliceTemplate(int origin) {
        Objects.checkIndex(origin, length() + 1);
        VectorShuffle<Byte> iota = iotaShuffle();
        VectorMask<Byte> blendMask = iota.toVector().compare(VectorOperators.GE,
                                                                  (broadcast((byte)(origin))));
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
    ByteVector rearrange(VectorShuffle<Byte> m);

    /*package-private*/
    @ForceInline
    final
    <S extends VectorShuffle<Byte>>
    ByteVector rearrangeTemplate(Class<S> shuffletype, S shuffle) {
        shuffle.checkIndexes();
        return VectorSupport.rearrangeOp(
            getClass(), shuffletype, byte.class, length(),
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
    ByteVector rearrange(VectorShuffle<Byte> s,
                                   VectorMask<Byte> m);

    /*package-private*/
    @ForceInline
    final
    <S extends VectorShuffle<Byte>>
    ByteVector rearrangeTemplate(Class<S> shuffletype,
                                           S shuffle,
                                           VectorMask<Byte> m) {
        ByteVector unmasked =
            VectorSupport.rearrangeOp(
                getClass(), shuffletype, byte.class, length(),
                this, shuffle,
                (v1, s_) -> v1.uOp((i, a) -> {
                    int ei = s_.laneSource(i);
                    return ei < 0 ? 0 : v1.lane(ei);
                }));
        VectorMask<Byte> valid = shuffle.laneIsValid();
        if (m.andNot(valid).anyTrue()) {
            shuffle.checkIndexes();
            throw new AssertionError();
        }
        return broadcast((byte)0).blend(unmasked, m);
    }

    /**
     * {@inheritDoc} <!--workaround-->
     */
    @Override
    public abstract
    ByteVector rearrange(VectorShuffle<Byte> s,
                                   Vector<Byte> v);

    /*package-private*/
    @ForceInline
    final
    <S extends VectorShuffle<Byte>>
    ByteVector rearrangeTemplate(Class<S> shuffletype,
                                           S shuffle,
                                           ByteVector v) {
        VectorMask<Byte> valid = shuffle.laneIsValid();
        @SuppressWarnings("unchecked")
        S ws = (S) shuffle.wrapIndexes();
        ByteVector r0 =
            VectorSupport.rearrangeOp(
                getClass(), shuffletype, byte.class, length(),
                this, ws,
                (v0, s_) -> v0.uOp((i, a) -> {
                    int ei = s_.laneSource(i);
                    return v0.lane(ei);
                }));
        ByteVector r1 =
            VectorSupport.rearrangeOp(
                getClass(), shuffletype, byte.class, length(),
                v, ws,
                (v1, s_) -> v1.uOp((i, a) -> {
                    int ei = s_.laneSource(i);
                    return v1.lane(ei);
                }));
        return r1.blend(r0, valid);
    }

    @ForceInline
    private final
    VectorShuffle<Byte> toShuffle0(ByteSpecies dsp) {
        byte[] a = toArray();
        int[] sa = new int[a.length];
        for (int i = 0; i < a.length; i++) {
            sa[i] = (int) a[i];
        }
        return VectorShuffle.fromArray(dsp, sa, 0);
    }

    /*package-private*/
    @ForceInline
    final
    VectorShuffle<Byte> toShuffleTemplate(Class<?> shuffleType) {
        ByteSpecies vsp = vspecies();
        return VectorSupport.convert(VectorSupport.VECTOR_OP_CAST,
                                     getClass(), byte.class, length(),
                                     shuffleType, byte.class, length(),
                                     this, vsp,
                                     ByteVector::toShuffle0);
    }

    /**
     * {@inheritDoc} <!--workaround-->
     */
    @Override
    public abstract
    ByteVector selectFrom(Vector<Byte> v);

    /*package-private*/
    @ForceInline
    final ByteVector selectFromTemplate(ByteVector v) {
        return v.rearrange(this.toShuffle());
    }

    /**
     * {@inheritDoc} <!--workaround-->
     */
    @Override
    public abstract
    ByteVector selectFrom(Vector<Byte> s, VectorMask<Byte> m);

    /*package-private*/
    @ForceInline
    final ByteVector selectFromTemplate(ByteVector v,
                                                  AbstractMask<Byte> m) {
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
     * @see #bitwiseBlend(byte,byte)
     * @see #bitwiseBlend(byte,Vector)
     * @see #bitwiseBlend(Vector,byte)
     * @see VectorOperators#BITWISE_BLEND
     * @see #lanewise(VectorOperators.Ternary,Vector,Vector,VectorMask)
     */
    @ForceInline
    public final
    ByteVector bitwiseBlend(Vector<Byte> bits, Vector<Byte> mask) {
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
     * @see #lanewise(VectorOperators.Ternary,byte,byte,VectorMask)
     */
    @ForceInline
    public final
    ByteVector bitwiseBlend(byte bits, byte mask) {
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
     * @see #lanewise(VectorOperators.Ternary,byte,Vector,VectorMask)
     */
    @ForceInline
    public final
    ByteVector bitwiseBlend(byte bits, Vector<Byte> mask) {
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
     * @see #lanewise(VectorOperators.Ternary,Vector,byte,VectorMask)
     */
    @ForceInline
    public final
    ByteVector bitwiseBlend(Vector<Byte> bits, byte mask) {
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
    public abstract byte reduceLanes(VectorOperators.Associative op);

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
     * then the identity value is zero, the default {@code byte} value.
     * <li>
     * If the operation is {@code MUL},
     * then the identity value is one.
     * <li>
     * If the operation is {@code AND},
     * then the identity value is minus one (all bits set).
     * <li>
     * If the operation is {@code MAX},
     * then the identity value is {@code Byte.MIN_VALUE}.
     * <li>
     * If the operation is {@code MIN},
     * then the identity value is {@code Byte.MAX_VALUE}.
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
    public abstract byte reduceLanes(VectorOperators.Associative op,
                                       VectorMask<Byte> m);

    /*package-private*/
    @ForceInline
    final
    byte reduceLanesTemplate(VectorOperators.Associative op,
                               VectorMask<Byte> m) {
        ByteVector v = reduceIdentityVector(op).blend(this, m);
        return v.reduceLanesTemplate(op);
    }

    /*package-private*/
    @ForceInline
    final
    byte reduceLanesTemplate(VectorOperators.Associative op) {
        if (op == FIRST_NONZERO) {
            // FIXME:  The JIT should handle this, and other scan ops alos.
            VectorMask<Byte> thisNZ
                = this.viewAsIntegralLanes().compare(NE, (byte) 0);
            return this.lane(thisNZ.firstTrue());
        }
        int opc = opCode(op);
        return fromBits(VectorSupport.reductionCoerced(
            opc, getClass(), byte.class, length(),
            this,
            REDUCE_IMPL.find(op, opc, (opc_) -> {
              switch (opc_) {
              case VECTOR_OP_ADD: return v ->
                      toBits(v.rOp((byte)0, (i, a, b) -> (byte)(a + b)));
              case VECTOR_OP_MUL: return v ->
                      toBits(v.rOp((byte)1, (i, a, b) -> (byte)(a * b)));
              case VECTOR_OP_MIN: return v ->
                      toBits(v.rOp(MAX_OR_INF, (i, a, b) -> (byte) Math.min(a, b)));
              case VECTOR_OP_MAX: return v ->
                      toBits(v.rOp(MIN_OR_INF, (i, a, b) -> (byte) Math.max(a, b)));
              case VECTOR_OP_AND: return v ->
                      toBits(v.rOp((byte)-1, (i, a, b) -> (byte)(a & b)));
              case VECTOR_OP_OR: return v ->
                      toBits(v.rOp((byte)0, (i, a, b) -> (byte)(a | b)));
              case VECTOR_OP_XOR: return v ->
                      toBits(v.rOp((byte)0, (i, a, b) -> (byte)(a ^ b)));
              default: return null;
              }})));
    }
    private static final
    ImplCache<Associative,Function<ByteVector,Long>> REDUCE_IMPL
        = new ImplCache<>(Associative.class, ByteVector.class);

    private
    @ForceInline
    ByteVector reduceIdentityVector(VectorOperators.Associative op) {
        int opc = opCode(op);
        UnaryOperator<ByteVector> fn
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
    ImplCache<Associative,UnaryOperator<ByteVector>> REDUCE_ID_IMPL
        = new ImplCache<>(Associative.class, ByteVector.class);

    private static final byte MIN_OR_INF = Byte.MIN_VALUE;
    private static final byte MAX_OR_INF = Byte.MAX_VALUE;

    public @Override abstract long reduceLanesToLong(VectorOperators.Associative op);
    public @Override abstract long reduceLanesToLong(VectorOperators.Associative op,
                                                     VectorMask<Byte> m);

    // Type specific accessors

    /**
     * Gets the lane element at lane index {@code i}
     *
     * @param i the lane index
     * @return the lane element at lane index {@code i}
     * @throws IllegalArgumentException if the index is is out of range
     * ({@code < 0 || >= length()})
     */
    public abstract byte lane(int i);

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
    public abstract ByteVector withLane(int i, byte e);

    // Memory load operations

    /**
     * Returns an array of type {@code byte[]}
     * containing all the lane values.
     * The array length is the same as the vector length.
     * The array elements are stored in lane order.
     * <p>
     * This method behaves as if it stores
     * this vector into an allocated array
     * (using {@link #intoArray(byte[], int) intoArray})
     * and returns the array as follows:
     * <pre>{@code
     *   byte[] a = new byte[this.length()];
     *   this.intoArray(a, 0);
     *   return a;
     * }</pre>
     *
     * @return an array containing the lane values of this vector
     */
    @ForceInline
    @Override
    public final byte[] toArray() {
        byte[] a = new byte[vspecies().laneCount()];
        intoArray(a, 0);
        return a;
    }

    /** {@inheritDoc} <!--workaround-->
     * @implNote
     * When this method is used on used on vectors
     * of type {@code ByteVector},
     * there will be no loss of precision or range,
     * and so no {@code UnsupportedOperationException} will
     * be thrown.
     */
    @ForceInline
    @Override
    public final int[] toIntArray() {
        byte[] a = toArray();
        int[] res = new int[a.length];
        for (int i = 0; i < a.length; i++) {
            byte e = a[i];
            res[i] = (int) ByteSpecies.toIntegralChecked(e, true);
        }
        return res;
    }

    /** {@inheritDoc} <!--workaround-->
     * @implNote
     * When this method is used on used on vectors
     * of type {@code ByteVector},
     * there will be no loss of precision or range,
     * and so no {@code UnsupportedOperationException} will
     * be thrown.
     */
    @ForceInline
    @Override
    public final long[] toLongArray() {
        byte[] a = toArray();
        long[] res = new long[a.length];
        for (int i = 0; i < a.length; i++) {
            byte e = a[i];
            res[i] = ByteSpecies.toIntegralChecked(e, false);
        }
        return res;
    }

    /** {@inheritDoc} <!--workaround-->
     * @implNote
     * When this method is used on used on vectors
     * of type {@code ByteVector},
     * there will be no loss of precision.
     */
    @ForceInline
    @Override
    public final double[] toDoubleArray() {
        byte[] a = toArray();
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
    ByteVector fromByteArray(VectorSpecies<Byte> species,
                                       byte[] a, int offset,
                                       ByteOrder bo) {
        offset = checkFromIndexSize(offset, species.vectorByteSize(), a.length);
        ByteSpecies vsp = (ByteSpecies) species;
        return vsp.dummyVector().fromByteArray0(a, offset).maybeSwap(bo);
    }

    /**
     * Loads a vector from a byte array starting at an offset
     * and using a mask.
     * Lanes where the mask is unset are filled with the default
     * value of {@code byte} (zero).
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
    ByteVector fromByteArray(VectorSpecies<Byte> species,
                                       byte[] a, int offset,
                                       ByteOrder bo,
                                       VectorMask<Byte> m) {
        ByteSpecies vsp = (ByteSpecies) species;
        if (offset >= 0 && offset <= (a.length - species.vectorByteSize())) {
            ByteVector zero = vsp.zero();
            ByteVector v = zero.fromByteArray0(a, offset);
            return zero.blend(v.maybeSwap(bo), m);
        }

        // FIXME: optimize
        checkMaskFromIndexSize(offset, vsp, m, 1, a.length);
        ByteBuffer wb = wrapper(a, bo);
        return vsp.ldOp(wb, offset, (AbstractMask<Byte>)m,
                   (wb_, o, i)  -> wb_.get(o + i * 1));
    }

    /**
     * Loads a vector from an array of type {@code byte[]}
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
    ByteVector fromArray(VectorSpecies<Byte> species,
                                   byte[] a, int offset) {
        offset = checkFromIndexSize(offset, species.length(), a.length);
        ByteSpecies vsp = (ByteSpecies) species;
        return vsp.dummyVector().fromArray0(a, offset);
    }

    /**
     * Loads a vector from an array of type {@code byte[]}
     * starting at an offset and using a mask.
     * Lanes where the mask is unset are filled with the default
     * value of {@code byte} (zero).
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
    ByteVector fromArray(VectorSpecies<Byte> species,
                                   byte[] a, int offset,
                                   VectorMask<Byte> m) {
        ByteSpecies vsp = (ByteSpecies) species;
        if (offset >= 0 && offset <= (a.length - species.length())) {
            ByteVector zero = vsp.zero();
            return zero.blend(zero.fromArray0(a, offset), m);
        }

        // FIXME: optimize
        checkMaskFromIndexSize(offset, vsp, m, 1, a.length);
        return vsp.vOp(m, i -> a[offset + i]);
    }

    /**
     * Gathers a new vector composed of elements from an array of type
     * {@code byte[]},
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
     * @see ByteVector#toIntArray()
     */
    @ForceInline
    public static
    ByteVector fromArray(VectorSpecies<Byte> species,
                                   byte[] a, int offset,
                                   int[] indexMap, int mapOffset) {
        ByteSpecies vsp = (ByteSpecies) species;
        return vsp.vOp(n -> a[offset + indexMap[mapOffset + n]]);
    }

    /**
     * Gathers a new vector composed of elements from an array of type
     * {@code byte[]},
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
     * @see ByteVector#toIntArray()
     */
    @ForceInline
    public static
    ByteVector fromArray(VectorSpecies<Byte> species,
                                   byte[] a, int offset,
                                   int[] indexMap, int mapOffset,
                                   VectorMask<Byte> m) {
        ByteSpecies vsp = (ByteSpecies) species;
        return vsp.vOp(m, n -> a[offset + indexMap[mapOffset + n]]);
    }


    /**
     * Loads a vector from an array of type {@code boolean[]}
     * starting at an offset.
     * For each vector lane, where {@code N} is the vector lane index, the
     * array element at index {@code offset + N}
     * is first converted to a {@code byte} value and then
     * placed into the resulting vector at lane index {@code N}.
     * <p>
     * A {@code boolean} value is converted to a {@code byte} value by applying the
     * expression {@code (byte) (b ? 1 : 0)}, where {@code b} is the {@code boolean} value.
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
    ByteVector fromBooleanArray(VectorSpecies<Byte> species,
                                          boolean[] a, int offset) {
        offset = checkFromIndexSize(offset, species.length(), a.length);
        ByteSpecies vsp = (ByteSpecies) species;
        return vsp.dummyVector().fromBooleanArray0(a, offset);
    }

    /**
     * Loads a vector from an array of type {@code boolean[]}
     * starting at an offset and using a mask.
     * Lanes where the mask is unset are filled with the default
     * value of {@code byte} (zero).
     * For each vector lane, where {@code N} is the vector lane index,
     * if the mask lane at index {@code N} is set then the array element at
     * index {@code offset + N}
     * is first converted to a {@code byte} value and then
     * placed into the resulting vector at lane index
     * {@code N}, otherwise the default element value is placed into the
     * resulting vector at lane index {@code N}.
     * <p>
     * A {@code boolean} value is converted to a {@code byte} value by applying the
     * expression {@code (byte) (b ? 1 : 0)}, where {@code b} is the {@code boolean} value.
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
    ByteVector fromBooleanArray(VectorSpecies<Byte> species,
                                          boolean[] a, int offset,
                                          VectorMask<Byte> m) {
        ByteSpecies vsp = (ByteSpecies) species;
        if (offset >= 0 && offset <= (a.length - species.length())) {
            ByteVector zero = vsp.zero();
            return zero.blend(zero.fromBooleanArray0(a, offset), m);
        }

        // FIXME: optimize
        checkMaskFromIndexSize(offset, vsp, m, 1, a.length);
        return vsp.vOp(m, i -> (byte) (a[offset + i] ? 1 : 0));
    }

    /**
     * Gathers a new vector composed of elements from an array of type
     * {@code boolean[]},
     * using indexes obtained by adding a fixed {@code offset} to a
     * series of secondary offsets from an <em>index map</em>.
     * The index map is a contiguous sequence of {@code VLENGTH}
     * elements in a second array of {@code int}s, starting at a given
     * {@code mapOffset}.
     * <p>
     * For each vector lane, where {@code N} is the vector lane index,
     * the lane is loaded from the expression
     * {@code (byte) (a[f(N)] ? 1 : 0)}, where {@code f(N)} is the
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
     * @see ByteVector#toIntArray()
     */
    @ForceInline
    public static
    ByteVector fromBooleanArray(VectorSpecies<Byte> species,
                                          boolean[] a, int offset,
                                          int[] indexMap, int mapOffset) {
        // FIXME: optimize
        ByteSpecies vsp = (ByteSpecies) species;
        return vsp.vOp(n -> (byte) (a[offset + indexMap[mapOffset + n]] ? 1 : 0));
    }

    /**
     * Gathers a new vector composed of elements from an array of type
     * {@code boolean[]},
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
     * {@code (byte) (a[f(N)] ? 1 : 0)}, where {@code f(N)} is the
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
     * @see ByteVector#toIntArray()
     */
    @ForceInline
    public static
    ByteVector fromBooleanArray(VectorSpecies<Byte> species,
                                          boolean[] a, int offset,
                                          int[] indexMap, int mapOffset,
                                          VectorMask<Byte> m) {
        // FIXME: optimize
        ByteSpecies vsp = (ByteSpecies) species;
        return vsp.vOp(m, n -> (byte) (a[offset + indexMap[mapOffset + n]] ? 1 : 0));
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
     *         if {@code offset+N*1 < 0}
     *         or {@code offset+N*1 >= bb.limit()}
     *         for any lane {@code N} in the vector
     */
    @ForceInline
    public static
    ByteVector fromByteBuffer(VectorSpecies<Byte> species,
                                        ByteBuffer bb, int offset,
                                        ByteOrder bo) {
        offset = checkFromIndexSize(offset, species.vectorByteSize(), bb.limit());
        ByteSpecies vsp = (ByteSpecies) species;
        return vsp.dummyVector().fromByteBuffer0(bb, offset).maybeSwap(bo);
    }

    /**
     * Loads a vector from a {@linkplain ByteBuffer byte buffer}
     * starting at an offset into the byte buffer
     * and using a mask.
     * Lanes where the mask is unset are filled with the default
     * value of {@code byte} (zero).
     * Bytes are composed into primitive lane elements according
     * to the specified byte order.
     * The vector is arranged into lanes according to
     * <a href="Vector.html#lane-order">memory ordering</a>.
     * <p>
     * The following pseudocode illustrates the behavior:
     * <pre>{@code
     * ByteBuffer eb = bb.duplicate()
     *     .position(offset);
     * byte[] ar = new byte[species.length()];
     * for (int n = 0; n < ar.length; n++) {
     *     if (m.laneIsSet(n)) {
     *         ar[n] = eb.get(n);
     *     }
     * }
     * ByteVector r = ByteVector.fromArray(species, ar, 0);
     * }</pre>
     * @implNote
     * The byte order argument is ignored.
     *
     * @param species species of desired vector
     * @param bb the byte buffer
     * @param offset the offset into the byte buffer
     * @param bo the intended byte order
     * @param m the mask controlling lane selection
     * @return a vector loaded from a byte buffer
     * @throws IndexOutOfBoundsException
     *         if {@code offset+N*1 < 0}
     *         or {@code offset+N*1 >= bb.limit()}
     *         for any lane {@code N} in the vector
     *         where the mask is set
     */
    @ForceInline
    public static
    ByteVector fromByteBuffer(VectorSpecies<Byte> species,
                                        ByteBuffer bb, int offset,
                                        ByteOrder bo,
                                        VectorMask<Byte> m) {
        ByteSpecies vsp = (ByteSpecies) species;
        if (offset >= 0 && offset <= (bb.limit() - species.vectorByteSize())) {
            ByteVector zero = vsp.zero();
            ByteVector v = zero.fromByteBuffer0(bb, offset);
            return zero.blend(v.maybeSwap(bo), m);
        }

        // FIXME: optimize
        checkMaskFromIndexSize(offset, vsp, m, 1, bb.limit());
        ByteBuffer wb = wrapper(bb, bo);
        return vsp.ldOp(wb, offset, (AbstractMask<Byte>)m,
                   (wb_, o, i)  -> wb_.get(o + i * 1));
    }

    // Memory store operations

    /**
     * Stores this vector into an array of type {@code byte[]}
     * starting at an offset.
     * <p>
     * For each vector lane, where {@code N} is the vector lane index,
     * the lane element at index {@code N} is stored into the array
     * element {@code a[offset+N]}.
     *
     * @param a the array, of type {@code byte[]}
     * @param offset the offset into the array
     * @throws IndexOutOfBoundsException
     *         if {@code offset+N < 0} or {@code offset+N >= a.length}
     *         for any lane {@code N} in the vector
     */
    @ForceInline
    public final
    void intoArray(byte[] a, int offset) {
        offset = checkFromIndexSize(offset, length(), a.length);
        ByteSpecies vsp = vspecies();
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
     * Stores this vector into an array of type {@code byte[]}
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
     * @param a the array, of type {@code byte[]}
     * @param offset the offset into the array
     * @param m the mask controlling lane storage
     * @throws IndexOutOfBoundsException
     *         if {@code offset+N < 0} or {@code offset+N >= a.length}
     *         for any lane {@code N} in the vector
     *         where the mask is set
     */
    @ForceInline
    public final
    void intoArray(byte[] a, int offset,
                   VectorMask<Byte> m) {
        if (m.allTrue()) {
            intoArray(a, offset);
        } else {
            // FIXME: optimize
            ByteSpecies vsp = vspecies();
            checkMaskFromIndexSize(offset, vsp, m, 1, a.length);
            stOp(a, offset, m, (arr, off, i, v) -> arr[off+i] = v);
        }
    }

    /**
     * Scatters this vector into an array of type {@code byte[]}
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
     * @see ByteVector#toIntArray()
     */
    @ForceInline
    public final
    void intoArray(byte[] a, int offset,
                   int[] indexMap, int mapOffset) {
        stOp(a, offset,
             (arr, off, i, e) -> {
                 int j = indexMap[mapOffset + i];
                 arr[off + j] = e;
             });
    }

    /**
     * Scatters this vector into an array of type {@code byte[]},
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
     * @see ByteVector#toIntArray()
     */
    @ForceInline
    public final
    void intoArray(byte[] a, int offset,
                   int[] indexMap, int mapOffset,
                   VectorMask<Byte> m) {
        stOp(a, offset, m,
             (arr, off, i, e) -> {
                 int j = indexMap[mapOffset + i];
                 arr[off + j] = e;
             });
    }


    /**
     * Stores this vector into an array of type {@code boolean[]}
     * starting at an offset.
     * <p>
     * For each vector lane, where {@code N} is the vector lane index,
     * the lane element at index {@code N}
     * is first converted to a {@code boolean} value and then
     * stored into the array element {@code a[offset+N]}.
     * <p>
     * A {@code byte} value is converted to a {@code boolean} value by applying the
     * expression {@code (b & 1) != 0} where {@code b} is the byte value.
     *
     * @param a the array
     * @param offset the offset into the array
     * @throws IndexOutOfBoundsException
     *         if {@code offset+N < 0} or {@code offset+N >= a.length}
     *         for any lane {@code N} in the vector
     */
    @ForceInline
    public final
    void intoBooleanArray(boolean[] a, int offset) {
        offset = checkFromIndexSize(offset, length(), a.length);
        ByteSpecies vsp = vspecies();
        ByteVector normalized = this.and((byte) 1);
        VectorSupport.store(
            vsp.vectorType(), vsp.elementType(), vsp.laneCount(),
            a, booleanArrayAddress(a, offset),
            normalized,
            a, offset,
            (arr, off, v)
            -> v.stOp(arr, off,
                      (arr_, off_, i, e) -> arr_[off_ + i] = (e & 1) != 0));
    }

    /**
     * Stores this vector into an array of type {@code boolean[]}
     * starting at offset and using a mask.
     * <p>
     * For each vector lane, where {@code N} is the vector lane index,
     * the lane element at index {@code N}
     * is first converted to a {@code boolean} value and then
     * stored into the array element {@code a[offset+N]}.
     * If the mask lane at {@code N} is unset then the corresponding
     * array element {@code a[offset+N]} is left unchanged.
     * <p>
     * A {@code byte} value is converted to a {@code boolean} value by applying the
     * expression {@code (b & 1) != 0} where {@code b} is the byte value.
     * <p>
     * Array range checking is done for lanes where the mask is set.
     * Lanes where the mask is unset are not stored and do not need
     * to correspond to legitimate elements of {@code a}.
     * That is, unset lanes may correspond to array indexes less than
     * zero or beyond the end of the array.
     *
     * @param a the array
     * @param offset the offset into the array
     * @param m the mask controlling lane storage
     * @throws IndexOutOfBoundsException
     *         if {@code offset+N < 0} or {@code offset+N >= a.length}
     *         for any lane {@code N} in the vector
     *         where the mask is set
     */
    @ForceInline
    public final
    void intoBooleanArray(boolean[] a, int offset,
                          VectorMask<Byte> m) {
        if (m.allTrue()) {
            intoBooleanArray(a, offset);
        } else {
            // FIXME: optimize
            ByteSpecies vsp = vspecies();
            checkMaskFromIndexSize(offset, vsp, m, 1, a.length);
            stOp(a, offset, m, (arr, off, i, e) -> arr[off+i] = (e & 1) != 0);
        }
    }

    /**
     * Scatters this vector into an array of type {@code boolean[]}
     * using indexes obtained by adding a fixed {@code offset} to a
     * series of secondary offsets from an <em>index map</em>.
     * The index map is a contiguous sequence of {@code VLENGTH}
     * elements in a second array of {@code int}s, starting at a given
     * {@code mapOffset}.
     * <p>
     * For each vector lane, where {@code N} is the vector lane index,
     * the lane element at index {@code N}
     * is first converted to a {@code boolean} value and then
     * stored into the array
     * element {@code a[f(N)]}, where {@code f(N)} is the
     * index mapping expression
     * {@code offset + indexMap[mapOffset + N]]}.
     * <p>
     * A {@code byte} value is converted to a {@code boolean} value by applying the
     * expression {@code (b & 1) != 0} where {@code b} is the byte value.
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
     * @see ByteVector#toIntArray()
     */
    @ForceInline
    public final
    void intoBooleanArray(boolean[] a, int offset,
                          int[] indexMap, int mapOffset) {
        // FIXME: optimize
        stOp(a, offset,
             (arr, off, i, e) -> {
                 int j = indexMap[mapOffset + i];
                 arr[off + j] = (e & 1) != 0;
             });
    }

    /**
     * Scatters this vector into an array of type {@code boolean[]},
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
     * is first converted to a {@code boolean} value and then
     * stored into the array
     * element {@code a[f(N)]}, where {@code f(N)} is the
     * index mapping expression
     * {@code offset + indexMap[mapOffset + N]]}.
     * <p>
     * A {@code byte} value is converted to a {@code boolean} value by applying the
     * expression {@code (b & 1) != 0} where {@code b} is the byte value.
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
     * @see ByteVector#toIntArray()
     */
    @ForceInline
    public final
    void intoBooleanArray(boolean[] a, int offset,
                          int[] indexMap, int mapOffset,
                          VectorMask<Byte> m) {
        // FIXME: optimize
        stOp(a, offset, m,
             (arr, off, i, e) -> {
                 int j = indexMap[mapOffset + i];
                 arr[off + j] = (e & 1) != 0;
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
                       VectorMask<Byte> m) {
        if (m.allTrue()) {
            intoByteArray(a, offset, bo);
        } else {
            // FIXME: optimize
            ByteSpecies vsp = vspecies();
            checkMaskFromIndexSize(offset, vsp, m, 1, a.length);
            ByteBuffer wb = wrapper(a, bo);
            this.stOp(wb, offset, m,
                    (wb_, o, i, e) -> wb_.put(o + i * 1, e));
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
                        VectorMask<Byte> m) {
        if (m.allTrue()) {
            intoByteBuffer(bb, offset, bo);
        } else {
            // FIXME: optimize
            if (bb.isReadOnly()) {
                throw new ReadOnlyBufferException();
            }
            ByteSpecies vsp = vspecies();
            checkMaskFromIndexSize(offset, vsp, m, 1, bb.limit());
            ByteBuffer wb = wrapper(bb, bo);
            this.stOp(wb, offset, m,
                    (wb_, o, i, e) -> wb_.put(o + i * 1, e));
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
    ByteVector fromArray0(byte[] a, int offset);
    @ForceInline
    final
    ByteVector fromArray0Template(byte[] a, int offset) {
        ByteSpecies vsp = vspecies();
        return VectorSupport.load(
            vsp.vectorType(), vsp.elementType(), vsp.laneCount(),
            a, arrayAddress(a, offset),
            a, offset, vsp,
            (arr, off, s) -> s.ldOp(arr, off,
                                    (arr_, off_, i) -> arr_[off_ + i]));
    }


    /*package-private*/
    abstract
    ByteVector fromBooleanArray0(boolean[] a, int offset);
    @ForceInline
    final
    ByteVector fromBooleanArray0Template(boolean[] a, int offset) {
        ByteSpecies vsp = vspecies();
        return VectorSupport.load(
            vsp.vectorType(), vsp.elementType(), vsp.laneCount(),
            a, booleanArrayAddress(a, offset),
            a, offset, vsp,
            (arr, off, s) -> s.ldOp(arr, off,
                                    (arr_, off_, i) -> (byte) (arr_[off_ + i] ? 1 : 0)));
    }

    @Override
    abstract
    ByteVector fromByteArray0(byte[] a, int offset);
    @ForceInline
    final
    ByteVector fromByteArray0Template(byte[] a, int offset) {
        ByteSpecies vsp = vspecies();
        return VectorSupport.load(
            vsp.vectorType(), vsp.elementType(), vsp.laneCount(),
            a, byteArrayAddress(a, offset),
            a, offset, vsp,
            (arr, off, s) -> {
                ByteBuffer wb = wrapper(arr, NATIVE_ENDIAN);
                return s.ldOp(wb, off,
                        (wb_, o, i) -> wb_.get(o + i * 1));
            });
    }

    abstract
    ByteVector fromByteBuffer0(ByteBuffer bb, int offset);
    @ForceInline
    final
    ByteVector fromByteBuffer0Template(ByteBuffer bb, int offset) {
        ByteSpecies vsp = vspecies();
        return ScopedMemoryAccess.loadFromByteBuffer(
                vsp.vectorType(), vsp.elementType(), vsp.laneCount(),
                bb, offset, vsp,
                (buf, off, s) -> {
                    ByteBuffer wb = wrapper(buf, NATIVE_ENDIAN);
                    return s.ldOp(wb, off,
                            (wb_, o, i) -> wb_.get(o + i * 1));
                });
    }

    // Unchecked storing operations in native byte order.
    // Caller is responsible for applying index checks, masking, and
    // byte swapping.

    abstract
    void intoArray0(byte[] a, int offset);
    @ForceInline
    final
    void intoArray0Template(byte[] a, int offset) {
        ByteSpecies vsp = vspecies();
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
        ByteSpecies vsp = vspecies();
        VectorSupport.store(
            vsp.vectorType(), vsp.elementType(), vsp.laneCount(),
            a, byteArrayAddress(a, offset),
            this, a, offset,
            (arr, off, v) -> {
                ByteBuffer wb = wrapper(arr, NATIVE_ENDIAN);
                v.stOp(wb, off,
                        (tb_, o, i, e) -> tb_.put(o + i * 1, e));
            });
    }

    @ForceInline
    final
    void intoByteBuffer0(ByteBuffer bb, int offset) {
        ByteSpecies vsp = vspecies();
        ScopedMemoryAccess.storeIntoByteBuffer(
                vsp.vectorType(), vsp.elementType(), vsp.laneCount(),
                this, bb, offset,
                (buf, off, v) -> {
                    ByteBuffer wb = wrapper(buf, NATIVE_ENDIAN);
                    v.stOp(wb, off,
                            (wb_, o, i, e) -> wb_.put(o + i * 1, e));
                });
    }

    // End of low-level memory operations.

    private static
    void checkMaskFromIndexSize(int offset,
                                ByteSpecies vsp,
                                VectorMask<Byte> m,
                                int scale,
                                int limit) {
        ((AbstractMask<Byte>)m)
            .checkIndexByLane(offset, limit, vsp.iota(), scale);
    }

    @ForceInline
    private void conditionalStoreNYI(int offset,
                                     ByteSpecies vsp,
                                     VectorMask<Byte> m,
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
    ByteVector maybeSwap(ByteOrder bo) {
        return this;
    }

    static final int ARRAY_SHIFT =
        31 - Integer.numberOfLeadingZeros(Unsafe.ARRAY_BYTE_INDEX_SCALE);
    static final long ARRAY_BASE =
        Unsafe.ARRAY_BYTE_BASE_OFFSET;

    @ForceInline
    static long arrayAddress(byte[] a, int index) {
        return ARRAY_BASE + (((long)index) << ARRAY_SHIFT);
    }


    static final int ARRAY_BOOLEAN_SHIFT =
            31 - Integer.numberOfLeadingZeros(Unsafe.ARRAY_BOOLEAN_INDEX_SCALE);
    static final long ARRAY_BOOLEAN_BASE =
            Unsafe.ARRAY_BOOLEAN_BASE_OFFSET;

    @ForceInline
    static long booleanArrayAddress(boolean[] a, int index) {
        return ARRAY_BOOLEAN_BASE + (((long)index) << ARRAY_BOOLEAN_SHIFT);
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
        return this;
    }

    /**
     * {@inheritDoc} <!--workaround-->
     */
    @ForceInline
    @Override
    public final ByteVector viewAsIntegralLanes() {
        return this;
    }

    /**
     * {@inheritDoc} <!--workaround-->
     *
     * @implNote This method always throws
     * {@code UnsupportedOperationException}, because there is no floating
     * point type of the same size as {@code byte}.  The return type
     * of this method is arbitrarily designated as
     * {@code Vector<?>}.  Future versions of this API may change the return
     * type if additional floating point types become available.
     */
    @ForceInline
    @Override
    public final
    Vector<?>
    viewAsFloatingLanes() {
        LaneType flt = LaneType.BYTE.asFloating();
        // asFloating() will throw UnsupportedOperationException for the unsupported type byte
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
     * java.util.Arrays#toString(byte[]) Arrays.toString()},
     * as appropriate to the {@code byte} array returned by
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
     * Class representing {@link ByteVector}'s of the same {@link VectorShape VectorShape}.
     */
    /*package-private*/
    static final class ByteSpecies extends AbstractSpecies<Byte> {
        private ByteSpecies(VectorShape shape,
                Class<? extends ByteVector> vectorType,
                Class<? extends AbstractMask<Byte>> maskType,
                Function<Object, ByteVector> vectorFactory) {
            super(shape, LaneType.of(byte.class),
                  vectorType, maskType,
                  vectorFactory);
            assert(this.elementSize() == Byte.SIZE);
        }

        // Specializing overrides:

        @Override
        @ForceInline
        public final Class<Byte> elementType() {
            return byte.class;
        }

        @Override
        @ForceInline
        final Class<Byte> genericElementType() {
            return Byte.class;
        }

        @SuppressWarnings("unchecked")
        @Override
        @ForceInline
        public final Class<? extends ByteVector> vectorType() {
            return (Class<? extends ByteVector>) vectorType;
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
        final ByteVector broadcastBits(long bits) {
            return (ByteVector)
                VectorSupport.broadcastCoerced(
                    vectorType, byte.class, laneCount,
                    bits, this,
                    (bits_, s_) -> s_.rvOp(i -> bits_));
        }

        /*package-private*/
        @ForceInline
        final ByteVector broadcast(byte e) {
            return broadcastBits(toBits(e));
        }

        @Override
        @ForceInline
        public final ByteVector broadcast(long e) {
            return broadcastBits(longToElementBits(e));
        }

        /*package-private*/
        final @Override
        @ForceInline
        long longToElementBits(long value) {
            // Do the conversion, and then test it for failure.
            byte e = (byte) value;
            if ((long) e != value) {
                throw badElementBits(value, e);
            }
            return toBits(e);
        }

        /*package-private*/
        @ForceInline
        static long toIntegralChecked(byte e, boolean convertToInt) {
            long value = convertToInt ? (int) e : (long) e;
            if ((byte) value != e) {
                throw badArrayBits(e, convertToInt, value);
            }
            return value;
        }

        /* this non-public one is for internal conversions */
        @Override
        @ForceInline
        final ByteVector fromIntValues(int[] values) {
            VectorIntrinsics.requireLength(values.length, laneCount);
            byte[] va = new byte[laneCount()];
            for (int i = 0; i < va.length; i++) {
                int lv = values[i];
                byte v = (byte) lv;
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
        public ByteVector fromArray(Object a, int offset) {
            // User entry point:  Be careful with inputs.
            return ByteVector
                .fromArray(this, (byte[]) a, offset);
        }

        @ForceInline
        @Override final
        ByteVector dummyVector() {
            return (ByteVector) super.dummyVector();
        }

        /*package-private*/
        final @Override
        @ForceInline
        ByteVector rvOp(RVOp f) {
            byte[] res = new byte[laneCount()];
            for (int i = 0; i < res.length; i++) {
                byte bits = (byte) f.apply(i);
                res[i] = fromBits(bits);
            }
            return dummyVector().vectorFactory(res);
        }

        ByteVector vOp(FVOp f) {
            byte[] res = new byte[laneCount()];
            for (int i = 0; i < res.length; i++) {
                res[i] = f.apply(i);
            }
            return dummyVector().vectorFactory(res);
        }

        ByteVector vOp(VectorMask<Byte> m, FVOp f) {
            byte[] res = new byte[laneCount()];
            boolean[] mbits = ((AbstractMask<Byte>)m).getBits();
            for (int i = 0; i < res.length; i++) {
                if (mbits[i]) {
                    res[i] = f.apply(i);
                }
            }
            return dummyVector().vectorFactory(res);
        }

        /*package-private*/
        @ForceInline
        <M> ByteVector ldOp(M memory, int offset,
                                      FLdOp<M> f) {
            return dummyVector().ldOp(memory, offset, f);
        }

        /*package-private*/
        @ForceInline
        <M> ByteVector ldOp(M memory, int offset,
                                      AbstractMask<Byte> m,
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
                      AbstractMask<Byte> m,
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
        public final ByteVector zero() {
            if ((Class<?>) vectorType() == ByteMaxVector.class)
                return ByteMaxVector.ZERO;
            switch (vectorBitSize()) {
                case 64: return Byte64Vector.ZERO;
                case 128: return Byte128Vector.ZERO;
                case 256: return Byte256Vector.ZERO;
                case 512: return Byte512Vector.ZERO;
            }
            throw new AssertionError();
        }

        @Override
        @ForceInline
        public final ByteVector iota() {
            if ((Class<?>) vectorType() == ByteMaxVector.class)
                return ByteMaxVector.IOTA;
            switch (vectorBitSize()) {
                case 64: return Byte64Vector.IOTA;
                case 128: return Byte128Vector.IOTA;
                case 256: return Byte256Vector.IOTA;
                case 512: return Byte512Vector.IOTA;
            }
            throw new AssertionError();
        }

        // Mask access
        @Override
        @ForceInline
        public final VectorMask<Byte> maskAll(boolean bit) {
            if ((Class<?>) vectorType() == ByteMaxVector.class)
                return ByteMaxVector.ByteMaxMask.maskAll(bit);
            switch (vectorBitSize()) {
                case 64: return Byte64Vector.Byte64Mask.maskAll(bit);
                case 128: return Byte128Vector.Byte128Mask.maskAll(bit);
                case 256: return Byte256Vector.Byte256Mask.maskAll(bit);
                case 512: return Byte512Vector.Byte512Mask.maskAll(bit);
            }
            throw new AssertionError();
        }
    }

    /**
     * Finds a species for an element type of {@code byte} and shape.
     *
     * @param s the shape
     * @return a species for an element type of {@code byte} and shape
     * @throws IllegalArgumentException if no such species exists for the shape
     */
    static ByteSpecies species(VectorShape s) {
        Objects.requireNonNull(s);
        switch (s) {
            case S_64_BIT: return (ByteSpecies) SPECIES_64;
            case S_128_BIT: return (ByteSpecies) SPECIES_128;
            case S_256_BIT: return (ByteSpecies) SPECIES_256;
            case S_512_BIT: return (ByteSpecies) SPECIES_512;
            case S_Max_BIT: return (ByteSpecies) SPECIES_MAX;
            default: throw new IllegalArgumentException("Bad shape: " + s);
        }
    }

    /** Species representing {@link ByteVector}s of {@link VectorShape#S_64_BIT VectorShape.S_64_BIT}. */
    public static final VectorSpecies<Byte> SPECIES_64
        = new ByteSpecies(VectorShape.S_64_BIT,
                            Byte64Vector.class,
                            Byte64Vector.Byte64Mask.class,
                            Byte64Vector::new);

    /** Species representing {@link ByteVector}s of {@link VectorShape#S_128_BIT VectorShape.S_128_BIT}. */
    public static final VectorSpecies<Byte> SPECIES_128
        = new ByteSpecies(VectorShape.S_128_BIT,
                            Byte128Vector.class,
                            Byte128Vector.Byte128Mask.class,
                            Byte128Vector::new);

    /** Species representing {@link ByteVector}s of {@link VectorShape#S_256_BIT VectorShape.S_256_BIT}. */
    public static final VectorSpecies<Byte> SPECIES_256
        = new ByteSpecies(VectorShape.S_256_BIT,
                            Byte256Vector.class,
                            Byte256Vector.Byte256Mask.class,
                            Byte256Vector::new);

    /** Species representing {@link ByteVector}s of {@link VectorShape#S_512_BIT VectorShape.S_512_BIT}. */
    public static final VectorSpecies<Byte> SPECIES_512
        = new ByteSpecies(VectorShape.S_512_BIT,
                            Byte512Vector.class,
                            Byte512Vector.Byte512Mask.class,
                            Byte512Vector::new);

    /** Species representing {@link ByteVector}s of {@link VectorShape#S_Max_BIT VectorShape.S_Max_BIT}. */
    public static final VectorSpecies<Byte> SPECIES_MAX
        = new ByteSpecies(VectorShape.S_Max_BIT,
                            ByteMaxVector.class,
                            ByteMaxVector.ByteMaxMask.class,
                            ByteMaxVector::new);

    /**
     * Preferred species for {@link ByteVector}s.
     * A preferred species is a species of maximal bit-size for the platform.
     */
    public static final VectorSpecies<Byte> SPECIES_PREFERRED
        = (ByteSpecies) VectorSpecies.ofPreferred(byte.class);
}
