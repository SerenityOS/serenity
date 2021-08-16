/*
 * Copyright (c) 2019, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.util.function.IntFunction;
import java.util.HashMap;
import java.util.ArrayList;

import jdk.internal.vm.annotation.ForceInline;
import jdk.internal.vm.annotation.Stable;

import jdk.internal.vm.vector.VectorSupport;

/**
 * This class consists solely of static constants
 * that describe lane-wise vector operations, plus nested interfaces
 * which classify them.
 * The static constants serve as tokens denoting specifically
 * requested lane operations in vector expressions, such
 * as the token {@code ADD} in
 * {@code w = v0.}{@link
 * Vector#lanewise(VectorOperators.Binary,Vector)
 * lanewise}{@code (ADD, v1)}.
 *
 * <p>
 *
 * The documentation for each individual operator token is very brief,
 * giving a symbolic Java expression for the operation that the token
 * requests.  Those symbolic expressions use the following conventional
 * elements:
 * <ul>
 * <li>{@code a}, {@code b}, {@code c} &mdash; names of lane values
 *
 * <li>Java operators like {@code +}, {@code ?:}, etc. &mdash;
 * expression operators
 *
 * <li>Java method names like {@code max}, {@code sin}, etc. &mdash;
 * methods in standard classes like {@code Math}, {@code Double}, etc.
 * Unqualified method names should be read as if in the context of a
 * static import, and with resolution of overloading.
 *
 * <li>{@code bits(x)} &mdash; a function call which produces the
 * underlying bits of the value {@code x}.  If {@code x} is a floating
 * point value, this is either {@code doubleToLongBits(x)} or
 * {@code floatToIntBits(x)}.  Otherwise, the value is just {@code x}.
 *
 * <li>{@code ESIZE} &mdash; the size in bytes of the operand type
 *
 * <li>{@code intVal}, {@code byteVal}, etc. &mdash; the operand of a
 * conversion, with the indicated type
 * </ul>
 *
 * <h2>Operations on floating point vectors</h2>
 * <ul>
 * <li>Lane-wise vector operations that apply to floating point vectors
 * follow the accuracy and monotonicity specifications of the equivalent
 * Java operation or method mentioned in its documentation unless specified otherwise.
 * If the vector element type is {@code float} and the Java operation or
 * method only accepts and returns {@code double} values, then the scalar operation
 * on each lane is adapted to cast operands and the result, specifically widening
 * {@code float} operands to {@code double} operands and narrowing the {@code double}
 * result to a {@code float}.
 *
 * <li id="fp_assoc">Certain associative operations that apply to floating point
 * vectors are not truly associative on the floating point lane values.
 * Specifically, {@link #ADD} and {@link #MUL} used with cross-lane reduction
 * operations, such as {@link FloatVector#reduceLanes(Associative)}.
 * The result of such an operation is a function both of the input
 * values (vector and mask) as well as the order of the scalar operations
 * applied to combine lane values.
 * In such cases the order is intentionally not defined.
 * This allows the JVM to generate optimal machine code for the underlying
 * platform at runtime.  If the platform supports a vector instruction
 * to add or multiply all values in the vector, or if there is some
 * other efficient machine code sequence, then the JVM has the option of
 * generating this machine code. Otherwise, the default
 * implementation is applied, which adds vector elements
 * sequentially from beginning to end.  For this reason, the
 * result of such an operation may vary for the same input values.
 * </ul>
 *
 * <p> Note that a particular operator token may apply to several
 * different lane types.  Thus, these tokens behave like overloaded
 * operators or methods, not like type-specific method handles or
 * lambdas.  Also unlike method handles or lambdas, these operators do
 * not possess operational semantics; they have no {@code apply} or
 * {@code invoke} method.  They are used only to request lane
 * operations from vector objects, and cannot (by themselves) perform
 * operations on individual lane values.
 *
 */
public abstract class VectorOperators {
    private VectorOperators() { }

    /**
     * Root type for all operator tokens, providing queries for common
     * properties such as arity, argument and return types, symbolic
     * name, and operator name.
     *
     * @see VectorOperators.Unary Unary
     * @see VectorOperators.Binary Binary
     * @see VectorOperators.Ternary Ternary
     * @see VectorOperators.Associative Associative
     * @see VectorOperators.Comparison Comparison
     * @see VectorOperators.Test Test
     * @see VectorOperators.Conversion Conversion
     *
     * @apiNote
     * User code should not implement this interface.  A future release of
     * this type may restrict implementations to be members of the same
     * package.
     */
    public interface Operator {
        /**
         * Returns the symbolic name of this operator,
         * as a constant in {@link VectorOperators}.
         *
         * The operator symbol, Java method name,
         * or example expression,
         * such as {@code "+"}, {@code "max"} or {@code "-a"},
         * is also available as {@link #operatorName()}.
         *
         * @return the symbolic name of this operator,
         *         such as {@code "ADD"}
         */
        String name();

        /**
         * Returns the Java operator symbol or method
         * name corresponding to this operator.
         * If there is no symbol or method, return a
         * string containing a representative expression
         * for the operator, using operand names
         * {@code a}, {@code b} (for non-unary operators),
         * and {@code c} (for ternary operators).
         *
         * The symbolic name of the constant,
         * such as {@code "ADD"},
         * is also available as {@link #name()}.
         *
         * @return an operator token, such as {@code "+"},
         *         or a method name, such as {@code "max"},
         *         or a representative expression, such as {@code "-a"}
         */
        String operatorName();

        /**
         * Returns the arity of this operator (1, 2, or 3).
         * @return the arity of this operator (1, 2, or 3)
         */
        int arity();

        /**
         * Reports whether this operator returns a boolean (a mask).
         * A boolean operator also reports {@code boolean} as the
         * {@code rangeType}.
         * @return whether this operator returns a boolean
         */
        boolean isBoolean();

        /**
         * Reports the special return type of this operator.
         * If this operator is a boolean, returns {@code boolean.class}.
         * If this operator is a {@code Conversion},
         * returns its {@linkplain Conversion#rangeType range type}.
         *
         * Otherwise, the operator's return value always has
         * whatever type was given as an input, and this method
         * returns {@code Object.class} to denote that fact.
         * @return the special return type, or {@code Object.class} if none
         */
        Class<?> rangeType();

        /**
         * Returns the associativity of this operator.
         * Only binary operators can be associative.
         * @return the associativity of this operator
         */
        boolean isAssociative();

        /**
         * Reports whether this operator is compatible with
         * the proposed element type.
         *
         * First, unrestricted operators are compatible with all element
         * types.
         *
         * Next, if the element type is {@code double} or {@code float}
         * and the operator is restricted to floating point types, it is
         * compatible.
         *
         * Otherwise, if the element type is neither {@code double} nor
         * {@code float} and the operator is restricted to integral
         * types, it is compatible.  Otherwise, the operator is not
         * compatible.
         *
         * @param elementType the proposed operand type for the operator
         * @return whether the proposed type is compatible with this operator
         */
        boolean compatibleWith(Class<?> elementType);

        // FIXME: Maybe add a query about architectural support.
    }

    /**
     * Type for all
     * <a href="Vector.html#lane-wise">lane-wise</a>
     * unary (one-argument) operators,
     * usable in expressions like {@code w = v0.}{@link
     * Vector#lanewise(VectorOperators.Unary)
     * lanewise}{@code (NEG)}.
     *
     * @apiNote
     * User code should not implement this interface.  A future release of
     * this type may restrict implementations to be members of the same
     * package.
     */
    public interface Unary extends Operator {
    }

    /**
     * Type for all
     * <a href="Vector.html#lane-wise">lane-wise</a>
     * binary (two-argument) operators,
     * usable in expressions like {@code w = v0.}{@link
     * Vector#lanewise(VectorOperators.Binary,Vector)
     * lanewise}{@code (ADD, v1)}.
     *
     * @apiNote
     * User code should not implement this interface.  A future release of
     * this type may restrict implementations to be members of the same
     * package.
     */
    public interface Binary extends Operator {
    }

    /**
     * Type for all
     * <a href="Vector.html#lane-wise">lane-wise</a>
     * ternary (three-argument) operators,
     * usable in expressions like {@code w = v0.}{@link
     * Vector#lanewise(VectorOperators.Ternary,Vector,Vector)
     * lanewise}{@code (FMA, v1, v2)}.
     *
     * @apiNote
     * User code should not implement this interface.  A future release of
     * this type may restrict implementations to be members of the same
     * package.
     */
    public interface Ternary extends Operator {
    }

    /**
     * Type for all reassociating
     * <a href="Vector.html#lane-wise">lane-wise</a>
     * binary operators,
     * usable in expressions like {@code e = v0.}{@link
     * IntVector#reduceLanes(VectorOperators.Associative)
     * reduceLanes}{@code (ADD)}.
     *
     * @apiNote
     * User code should not implement this interface.  A future release of
     * this type may restrict implementations to be members of the same
     * package.
     */
    public interface Associative extends Binary {
    }

    /**
     * Type for all unary
     * <a href="Vector.html#lane-wise">lane-wise</a>
     * boolean tests on lane values,
     * usable in expressions like {@code m = v0.}{@link
     * FloatVector#test(VectorOperators.Test)
     * test}{@code (IS_FINITE)}.
     *
     * @apiNote
     * User code should not implement this interface.  A future release of
     * this type may restrict implementations to be members of the same
     * package.
     */
    public interface Test extends Operator {
    }

    /**
     * Type for all binary
     * <a href="Vector.html#lane-wise">lane-wise</a>
     * boolean comparisons on lane values,
     * usable in expressions like {@code m = v0.}{@link
     * Vector#compare(VectorOperators.Comparison,Vector)
     * compare}{@code (LT, v1)}.
     *
     * @apiNote
     * User code should not implement this interface.  A future release of
     * this type may restrict implementations to be members of the same
     * package.
     */
    public interface Comparison extends Operator {
    }

    /**
     * Type for all
     * <a href="Vector.html#lane-wise">lane-wise</a>
     * conversions on lane values,
     * usable in expressions like {@code w1 = v0.}{@link
     * Vector#convert(VectorOperators.Conversion,int)
     * convert}{@code (I2D, 1)}.
     *
     * @param <E> the boxed element type for the conversion
     *        domain type (the input lane type)
     * @param <F> the boxed element type for the conversion
     *        range type (the output lane type)
     *
     * @apiNote
     * User code should not implement this interface.  A future release of
     * this type may restrict implementations to be members of the same
     * package.
     */
    public interface Conversion<E,F> extends Operator {
        /**
         * The domain of this conversion, a primitive type.
         * @return the domain of this conversion
         */
        Class<E> domainType();

        /**
         * The range of this conversion, a primitive type.
         * @return the range of this conversion
         */
        @Override
        Class<F> rangeType();

        /**
         * Ensures that this conversion has the
         * desired domain and range types.
         * @param from the desired domain type
         * @param to the desired range type
         * @param <E> the desired domain type
         * @param <F> the desired range type
         * @return this conversion object, with validated domain and range
         */
        <E,F> Conversion<E,F> check(Class<E> from, Class<F> to);

        /// Factories.

        /**
         * The Java language assignment or casting conversion between two types.
         * @param <E> the domain type (boxed version of a lane type)
         * @param <F> the range type (boxed version of a lane type)
         * @param from the type of the value to convert
         * @param to the desired type after conversion
         * @return a Java assignment or casting conversion
         */
        @ForceInline
        static <E,F> Conversion<E,F> ofCast(Class<E> from, Class<F> to) {
            LaneType dom = LaneType.of(from);
            LaneType ran = LaneType.of(to);
            return ConversionImpl.ofCast(dom, ran).check(from, to);
        }

        /**
         * The bitwise reinterpretation between two types.
         * @param <E> the domain type (boxed version of a lane type)
         * @param <F> the range type (boxed version of a lane type)
         * @param from the type of the value to reinterpret
         * @param to the desired type after reinterpretation
         * @return a bitwise reinterpretation conversion
         */
        @ForceInline
        static <E,F> Conversion<E,F> ofReinterpret(Class<E> from, Class<F> to) {
            LaneType dom = LaneType.of(from);
            LaneType ran = LaneType.of(to);
            return ConversionImpl.ofReinterpret(dom, ran).check(from, to);
        }

    }

    /*package-private*/
    @ForceInline
    static int opCode(Operator op, int requireKind, int forbidKind) {
        return ((OperatorImpl)op).opCode(requireKind, forbidKind);
    }

    /*package-private*/
    @ForceInline
    static boolean opKind(Operator op, int bit) {
        return ((OperatorImpl)op).opKind(bit);
    }

    /*package-private*/
    static final int
        VO_ALL                     = 0,
        VO_UNARY                   = 0x001,
        VO_BINARY                  = 0x002,
        VO_TERNARY                 = 0x003,
        VO_ARITY_MASK              = (VO_UNARY|VO_BINARY|VO_TERNARY),
        VO_ASSOC                   = 0x004,
        VO_SHIFT                   = 0x008,
        VO_BOOL                    = 0x010,
        VO_CONV                    = 0x020,
        VO_PRIVATE                 = 0x040, // not public, invisible
        VO_SPECIAL                 = 0x080, // random special handling
        VO_NOFP                    = 0x100,
        VO_ONLYFP                  = 0x200,
        VO_OPCODE_VALID            = 0x800,
        VO_OPCODE_SHIFT            = 12,
        VO_OPCODE_LIMIT            = 0x400,
        VO_RAN_SHIFT               = 0,
        VO_DOM_SHIFT               = 4,
        VO_DOM_RAN_MASK            = 0x0FF,
        VO_KIND_CAST               = 0x000,
        VO_KIND_BITWISE            = 0x100;

    private static final HashMap<Integer, String> OPC_NAME
        = new HashMap<>();
    private static final HashMap<Integer, String> CMP_OPC_NAME
        = new HashMap<>();
    private static final HashMap<Integer, String> CONV_OPC_NAME
        = new HashMap<>();

    // Unary operators

    /** Produce {@code ~a}.  Integral only. */
    public static final /*bitwise*/ Unary NOT = unary("NOT", "~", -1 /*VectorSupport.VECTOR_OP_NOT*/, VO_NOFP | VO_SPECIAL);
    /** Produce {@code a==0?0:-1} (zero or minus one).  Integral only. */
    public static final /*bitwise*/ Unary ZOMO = unary("ZOMO", "a==0?0:-1", -1 /*VectorSupport.VECTOR_OP_ZOMO*/, VO_NOFP);
    /** Produce {@code abs(a)}. */
    public static final Unary ABS = unary("ABS", "abs", VectorSupport.VECTOR_OP_ABS, VO_ALL);
    /** Produce {@code -a}. */
    public static final Unary NEG = unary("NEG", "-a", VectorSupport.VECTOR_OP_NEG, VO_ALL|VO_SPECIAL);

    /** Produce {@code sin(a)}.  Floating only.
     *  Not guaranteed to be semi-monotonic. See section "Operations on floating point vectors" above
     */
    public static final /*float*/ Unary SIN = unary("SIN", "sin", VectorSupport.VECTOR_OP_SIN, VO_ONLYFP);
    /** Produce {@code cos(a)}.  Floating only.
     *  Not guaranteed to be semi-monotonic. See section "Operations on floating point vectors" above
     */
    public static final /*float*/ Unary COS = unary("COS", "cos", VectorSupport.VECTOR_OP_COS, VO_ONLYFP);
    /** Produce {@code tan(a)}.  Floating only.
     *  Not guaranteed to be semi-monotonic. See section "Operations on floating point vectors" above
     */
    public static final /*float*/ Unary TAN = unary("TAN", "tan", VectorSupport.VECTOR_OP_TAN, VO_ONLYFP);
    /** Produce {@code asin(a)}.  Floating only.
     *  Not guaranteed to be semi-monotonic. See section "Operations on floating point vectors" above
     */
    public static final /*float*/ Unary ASIN = unary("ASIN", "asin", VectorSupport.VECTOR_OP_ASIN, VO_ONLYFP);
    /** Produce {@code acos(a)}.  Floating only.
     *  Not guaranteed to be semi-monotonic. See section "Operations on floating point vectors" above
     */
    public static final /*float*/ Unary ACOS = unary("ACOS", "acos", VectorSupport.VECTOR_OP_ACOS, VO_ONLYFP);
    /** Produce {@code atan(a)}.  Floating only.
     *  Not guaranteed to be semi-monotonic. See section "Operations on floating point vectors" above
     */
    public static final /*float*/ Unary ATAN = unary("ATAN", "atan", VectorSupport.VECTOR_OP_ATAN, VO_ONLYFP);

    /** Produce {@code exp(a)}.  Floating only.
     *  Not guaranteed to be semi-monotonic. See section "Operations on floating point vectors" above
     */
    public static final /*float*/ Unary EXP = unary("EXP", "exp", VectorSupport.VECTOR_OP_EXP, VO_ONLYFP);
    /** Produce {@code log(a)}.  Floating only.
     *  Not guaranteed to be semi-monotonic. See section "Operations on floating point vectors" above
     */
    public static final /*float*/ Unary LOG = unary("LOG", "log", VectorSupport.VECTOR_OP_LOG, VO_ONLYFP);
    /** Produce {@code log10(a)}.  Floating only.
     *  Not guaranteed to be semi-monotonic. See section "Operations on floating point vectors" above
     */
    public static final /*float*/ Unary LOG10 = unary("LOG10", "log10", VectorSupport.VECTOR_OP_LOG10, VO_ONLYFP);
    /** Produce {@code sqrt(a)}.  Floating only.  See section "Operations on floating point vectors" above */
    public static final /*float*/ Unary SQRT = unary("SQRT", "sqrt", VectorSupport.VECTOR_OP_SQRT, VO_ONLYFP);
    /** Produce {@code cbrt(a)}.  Floating only.
     *  Not guaranteed to be semi-monotonic. See section "Operations on floating point vectors" above
     */
    public static final /*float*/ Unary CBRT = unary("CBRT", "cbrt", VectorSupport.VECTOR_OP_CBRT, VO_ONLYFP);

    /** Produce {@code sinh(a)}.  Floating only.
     *  Not guaranteed to be semi-monotonic. See section "Operations on floating point vectors" above
     */
    public static final /*float*/ Unary SINH = unary("SINH", "sinh", VectorSupport.VECTOR_OP_SINH, VO_ONLYFP);
    /** Produce {@code cosh(a)}.  Floating only.
     *  Not guaranteed to be semi-monotonic. See section "Operations on floating point vectors" above
     */
    public static final /*float*/ Unary COSH = unary("COSH", "cosh", VectorSupport.VECTOR_OP_COSH, VO_ONLYFP);
    /** Produce {@code tanh(a)}.  Floating only.
     *  Not guaranteed to be semi-monotonic. See section "Operations on floating point vectors" above
     */
    public static final /*float*/ Unary TANH = unary("TANH", "tanh", VectorSupport.VECTOR_OP_TANH, VO_ONLYFP);
    /** Produce {@code expm1(a)}.  Floating only.
     *  Not guaranteed to be semi-monotonic. See section "Operations on floating point vectors" above
     */
    public static final /*float*/ Unary EXPM1 = unary("EXPM1", "expm1", VectorSupport.VECTOR_OP_EXPM1, VO_ONLYFP);
    /** Produce {@code log1p(a)}.  Floating only.
     *  Not guaranteed to be semi-monotonic. See section "Operations on floating point vectors" above
     */
    public static final /*float*/ Unary LOG1P = unary("LOG1P", "log1p", VectorSupport.VECTOR_OP_LOG1P, VO_ONLYFP);

    // Binary operators

    /** Produce {@code a+b}. */
    public static final Associative ADD = assoc("ADD", "+", VectorSupport.VECTOR_OP_ADD, VO_ALL+VO_ASSOC);
    /** Produce {@code a-b}. */
    public static final Binary SUB = binary("SUB", "-", VectorSupport.VECTOR_OP_SUB, VO_ALL);
    /** Produce {@code a*b}. */
    public static final Associative MUL = assoc("MUL", "*", VectorSupport.VECTOR_OP_MUL, VO_ALL+VO_ASSOC);
    /** Produce {@code a/b}. Floating only. */
    public static final Binary DIV = binary("DIV", "/", VectorSupport.VECTOR_OP_DIV, VO_ALL| VO_SPECIAL);
    /** Produce {@code min(a,b)}. */
    public static final Associative MIN = assoc("MIN", "min", VectorSupport.VECTOR_OP_MIN, VO_ALL+VO_ASSOC);
    /** Produce {@code max(a,b)}. */
    public static final Associative MAX = assoc("MAX", "max", VectorSupport.VECTOR_OP_MAX, VO_ALL+VO_ASSOC);
    /** Produce {@code bits(a)!=0?a:b}. */
    public static final Associative FIRST_NONZERO = assoc("FIRST_NONZERO", "a!=0?a:b", -1 /*VectorSupport.VECTOR_OP_FIRST_NONZERO*/, VO_ALL+VO_ASSOC);

    /** Produce {@code a&b}.  Integral only. */
    public static final Associative AND = assoc("AND", "&", VectorSupport.VECTOR_OP_AND, VO_NOFP+VO_ASSOC);
    /** Produce {@code a&~b}.  Integral only. */
    public static final /*bitwise*/ Binary AND_NOT = binary("AND_NOT", "&~", -1 /*VectorSupport.VECTOR_OP_AND_NOT*/, VO_NOFP); // FIXME
    /** Produce {@code a|b}.  Integral only. */
    public static final /*bitwise*/ Associative OR = assoc("OR", "|", VectorSupport.VECTOR_OP_OR, VO_NOFP+VO_ASSOC);
    /*package-private*/ /** Version of OR which works on float and double too. */
    static final Associative OR_UNCHECKED = assoc("OR_UNCHECKED", "|", VectorSupport.VECTOR_OP_OR, VO_ASSOC+VO_PRIVATE);
    /** Produce {@code a^b}.  Integral only. */
    public static final /*bitwise*/ Associative XOR = assoc("XOR", "^", VectorSupport.VECTOR_OP_XOR, VO_NOFP+VO_ASSOC);

    /** Produce {@code a<<(n&(ESIZE*8-1))}.  Integral only. */
    public static final /*bitwise*/ Binary LSHL = binary("LSHL", "<<", VectorSupport.VECTOR_OP_LSHIFT, VO_SHIFT);
    /** Produce {@code a>>(n&(ESIZE*8-1))}.  Integral only. */
    public static final /*bitwise*/ Binary ASHR = binary("ASHR", ">>", VectorSupport.VECTOR_OP_RSHIFT, VO_SHIFT);
    /** Produce {@code a>>>(n&(ESIZE*8-1))}.  Integral only. */
    public static final /*bitwise*/ Binary LSHR = binary("LSHR", ">>>", VectorSupport.VECTOR_OP_URSHIFT, VO_SHIFT);
    /** Produce {@code rotateLeft(a,n)}.  Integral only. */
    public static final /*bitwise*/ Binary ROL = binary("ROL", "rotateLeft", -1 /*VectorSupport.VECTOR_OP_LROTATE*/, VO_SHIFT | VO_SPECIAL);
    /** Produce {@code rotateRight(a,n)}.  Integral only. */
    public static final /*bitwise*/ Binary ROR = binary("ROR", "rotateRight", -1 /*VectorSupport.VECTOR_OP_RROTATE*/, VO_SHIFT | VO_SPECIAL);

    /** Produce {@code atan2(a,b)}. See  Floating only.
     *  Not guaranteed to be semi-monotonic. See section "Operations on floating point vectors" above
     */
    public static final /*float*/ Binary ATAN2 = binary("ATAN2", "atan2", VectorSupport.VECTOR_OP_ATAN2, VO_ONLYFP);
    /** Produce {@code pow(a,b)}.  Floating only.
     *  Not guaranteed to be semi-monotonic. See section "Operations on floating point vectors" above
     */
    public static final /*float*/ Binary POW = binary("POW", "pow", VectorSupport.VECTOR_OP_POW, VO_ONLYFP);
    /** Produce {@code hypot(a,b)}.  Floating only.
     *  Not guaranteed to be semi-monotonic. See section "Operations on floating point vectors" above
     */
    public static final /*float*/ Binary HYPOT = binary("HYPOT", "hypot", VectorSupport.VECTOR_OP_HYPOT, VO_ONLYFP);

    // Ternary operators

    /** Produce {@code a^((a^b)&c)}.  (Bitwise {@code (c(i)?b(i):a(i))}.)  Integral only. */
    public static final /*float*/ Ternary BITWISE_BLEND = ternary("BITWISE_BLEND", "a^((a^b)&c)", -1 /*VectorSupport.VECTOR_OP_BITWISE_BLEND*/, VO_NOFP);
    /** Produce {@code fma(a,b,c)}.  Floating only. */
    public static final /*float*/ Ternary FMA = ternary("FMA", "fma", VectorSupport.VECTOR_OP_FMA, VO_ONLYFP);

    // Unary boolean operators
    /** Test {@code bits(a)==0}.  (Not true of {@code -0.0}.) */
    public static final Test IS_DEFAULT = predicate("IS_DEFAULT", "bits(a)==0", -1 /*VectorSupport.VECTOR_OP_TEST_DEFAULT*/, VO_ALL);
    /** Test {@code bits(a)<0}.  (True of {@code -0.0}.) */
    public static final Test IS_NEGATIVE = predicate("IS_NEGATIVE", "bits(a)<0", -1 /*VectorSupport.VECTOR_OP_TEST_NEGATIVE*/, VO_ALL);
    /** Test {@code isFinite(a)}.  Floating only. */
    public static final Test IS_FINITE = predicate("IS_FINITE", "isFinite", -1 /*VectorSupport.VECTOR_OP_TEST_FINITE*/, VO_ONLYFP);
    /** Test {@code isNaN(a)}.  Floating only. */
    public static final Test IS_NAN = predicate("IS_NAN", "isNaN", -1 /*VectorSupport.VECTOR_OP_TEST_NAN*/, VO_ONLYFP);
    /** Test {@code isInfinite(a)}.  Floating only. */
    public static final Test IS_INFINITE = predicate("IS_INFINITE", "isInfinite", -1 /*VectorSupport.VECTOR_OP_TEST_INFINITE*/, VO_ONLYFP);

    // Binary boolean operators

    /** Compare {@code a==b}. */
    public static final Comparison EQ = compare("EQ", "==", VectorSupport.BT_eq, VO_ALL);
    /** Compare {@code a!=b}. */
    public static final Comparison NE = compare("NE", "!=", VectorSupport.BT_ne, VO_ALL);
    /** Compare {@code a<b}. */
    public static final Comparison LT = compare("LT", "<",  VectorSupport.BT_lt, VO_ALL);
    /** Compare {@code a<=b}. */
    public static final Comparison LE = compare("LE", "<=", VectorSupport.BT_le, VO_ALL);
    /** Compare {@code a>b}. */
    public static final Comparison GT = compare("GT", ">",  VectorSupport.BT_gt, VO_ALL);
    /** Compare {@code a>=b}. */
    public static final Comparison GE = compare("GE", ">=", VectorSupport.BT_ge, VO_ALL);
    /** Unsigned compare {@code a<b}.  Integral only.
     * @see java.lang.Integer#compareUnsigned
     * @see java.lang.Long#compareUnsigned
     */
    public static final Comparison UNSIGNED_LT = compare("UNSIGNED_LT", "<",  VectorSupport.BT_ult, VO_NOFP);
    /** Unsigned compare {@code a<=b}.  Integral only.
     * @see java.lang.Integer#compareUnsigned
     * @see java.lang.Long#compareUnsigned
     */
    public static final Comparison UNSIGNED_LE = compare("UNSIGNED_LE", "<=", VectorSupport.BT_ule, VO_NOFP);
    /** Unsigned compare {@code a>b}.  Integral only.
     * @see java.lang.Integer#compareUnsigned
     * @see java.lang.Long#compareUnsigned
     */
    public static final Comparison UNSIGNED_GT = compare("UNSIGNED_GT", ">",  VectorSupport.BT_ugt, VO_NOFP);
    /** Unsigned compare {@code a>=b}.  Integral only.
     * @see java.lang.Integer#compareUnsigned
     * @see java.lang.Long#compareUnsigned
     */
    public static final Comparison UNSIGNED_GE = compare("UNSIGNED_GE", ">=", VectorSupport.BT_uge, VO_NOFP);

    // Conversion operators

    /** Convert {@code byteVal} to {@code (double)byteVal}. */
    public static final Conversion<Byte,Double> B2D = convert("B2D", 'C', byte.class, double.class, VO_KIND_CAST, VO_ALL);
    /** Convert {@code byteVal} to {@code (float)byteVal}. */
    public static final Conversion<Byte,Float> B2F = convert("B2F", 'C', byte.class, float.class, VO_KIND_CAST, VO_ALL);
    /** Convert {@code byteVal} to {@code (int)byteVal}. */
    public static final Conversion<Byte,Integer> B2I = convert("B2I", 'C', byte.class, int.class, VO_KIND_CAST, VO_ALL);
    /** Convert {@code byteVal} to {@code (long)byteVal}. */
    public static final Conversion<Byte,Long> B2L = convert("B2L", 'C', byte.class, long.class, VO_KIND_CAST, VO_ALL);
    /** Convert {@code byteVal} to {@code (short)byteVal}. */
    public static final Conversion<Byte,Short> B2S = convert("B2S", 'C', byte.class, short.class, VO_KIND_CAST, VO_ALL);
    /** Convert {@code doubleVal} to {@code (byte)doubleVal}. */
    public static final Conversion<Double,Byte> D2B = convert("D2B", 'C', double.class, byte.class, VO_KIND_CAST, VO_ALL);
    /** Convert {@code doubleVal} to {@code (float)doubleVal}. */
    public static final Conversion<Double,Float> D2F = convert("D2F", 'C', double.class, float.class, VO_KIND_CAST, VO_ALL);
    /** Convert {@code doubleVal} to {@code (int)doubleVal}. */
    public static final Conversion<Double,Integer> D2I = convert("D2I", 'C', double.class, int.class, VO_KIND_CAST, VO_ALL);
    /** Convert {@code doubleVal} to {@code (long)doubleVal}. */
    public static final Conversion<Double,Long> D2L = convert("D2L", 'C', double.class, long.class, VO_KIND_CAST, VO_ALL);
    /** Convert {@code doubleVal} to {@code (short)doubleVal}. */
    public static final Conversion<Double,Short> D2S = convert("D2S", 'C', double.class, short.class, VO_KIND_CAST, VO_ALL);
    /** Convert {@code floatVal} to {@code (byte)floatVal}. */
    public static final Conversion<Float,Byte> F2B = convert("F2B", 'C', float.class, byte.class, VO_KIND_CAST, VO_ALL);
    /** Convert {@code floatVal} to {@code (double)floatVal}. */
    public static final Conversion<Float,Double> F2D = convert("F2D", 'C', float.class, double.class, VO_KIND_CAST, VO_ALL);
    /** Convert {@code floatVal} to {@code (int)floatVal}. */
    public static final Conversion<Float,Integer> F2I = convert("F2I", 'C', float.class, int.class, VO_KIND_CAST, VO_ALL);
    /** Convert {@code floatVal} to {@code (long)floatVal}. */
    public static final Conversion<Float,Long> F2L = convert("F2L", 'C', float.class, long.class, VO_KIND_CAST, VO_ALL);
    /** Convert {@code floatVal} to {@code (short)floatVal}. */
    public static final Conversion<Float,Short> F2S = convert("F2S", 'C', float.class, short.class, VO_KIND_CAST, VO_ALL);
    /** Convert {@code intVal} to {@code (byte)intVal}. */
    public static final Conversion<Integer,Byte> I2B = convert("I2B", 'C', int.class, byte.class, VO_KIND_CAST, VO_ALL);
    /** Convert {@code intVal} to {@code (double)intVal}. */
    public static final Conversion<Integer,Double> I2D = convert("I2D", 'C', int.class, double.class, VO_KIND_CAST, VO_ALL);
    /** Convert {@code intVal} to {@code (float)intVal}. */
    public static final Conversion<Integer,Float> I2F = convert("I2F", 'C', int.class, float.class, VO_KIND_CAST, VO_ALL);
    /** Convert {@code intVal} to {@code (long)intVal}. */
    public static final Conversion<Integer,Long> I2L = convert("I2L", 'C', int.class, long.class, VO_KIND_CAST, VO_ALL);
    /** Convert {@code intVal} to {@code (short)intVal}. */
    public static final Conversion<Integer,Short> I2S = convert("I2S", 'C', int.class, short.class, VO_KIND_CAST, VO_ALL);
    /** Convert {@code longVal} to {@code (byte)longVal}. */
    public static final Conversion<Long,Byte> L2B = convert("L2B", 'C', long.class, byte.class, VO_KIND_CAST, VO_ALL);
    /** Convert {@code longVal} to {@code (double)longVal}. */
    public static final Conversion<Long,Double> L2D = convert("L2D", 'C', long.class, double.class, VO_KIND_CAST, VO_ALL);
    /** Convert {@code longVal} to {@code (float)longVal}. */
    public static final Conversion<Long,Float> L2F = convert("L2F", 'C', long.class, float.class, VO_KIND_CAST, VO_ALL);
    /** Convert {@code longVal} to {@code (int)longVal}. */
    public static final Conversion<Long,Integer> L2I = convert("L2I", 'C', long.class, int.class, VO_KIND_CAST, VO_ALL);
    /** Convert {@code longVal} to {@code (short)longVal}. */
    public static final Conversion<Long,Short> L2S = convert("L2S", 'C', long.class, short.class, VO_KIND_CAST, VO_ALL);
    /** Convert {@code shortVal} to {@code (byte)shortVal}. */
    public static final Conversion<Short,Byte> S2B = convert("S2B", 'C', short.class, byte.class, VO_KIND_CAST, VO_ALL);
    /** Convert {@code shortVal} to {@code (double)shortVal}. */
    public static final Conversion<Short,Double> S2D = convert("S2D", 'C', short.class, double.class, VO_KIND_CAST, VO_ALL);
    /** Convert {@code shortVal} to {@code (float)shortVal}. */
    public static final Conversion<Short,Float> S2F = convert("S2F", 'C', short.class, float.class, VO_KIND_CAST, VO_ALL);
    /** Convert {@code shortVal} to {@code (int)shortVal}. */
    public static final Conversion<Short,Integer> S2I = convert("S2I", 'C', short.class, int.class, VO_KIND_CAST, VO_ALL);
    /** Convert {@code shortVal} to {@code (long)shortVal}. */
    public static final Conversion<Short,Long> S2L = convert("S2L", 'C', short.class, long.class, VO_KIND_CAST, VO_ALL);
    /** Reinterpret bits of {@code doubleVal} as {@code long}. As if by {@link Double#doubleToRawLongBits(double)} */
    public static final Conversion<Double,Long> REINTERPRET_D2L = convert("REINTERPRET_D2L", 'R', double.class, long.class, VO_KIND_BITWISE, VO_ALL);
    /** Reinterpret bits of {@code floatVal} as {@code int}. As if by {@link Float#floatToRawIntBits(float)} */
    public static final Conversion<Float,Integer> REINTERPRET_F2I = convert("REINTERPRET_F2I", 'R', float.class, int.class, VO_KIND_BITWISE, VO_ALL);
    /** Reinterpret bits of {@code intVal} as {@code float}. As if by {@link Float#intBitsToFloat(int)} */
    public static final Conversion<Integer,Float> REINTERPRET_I2F = convert("REINTERPRET_I2F", 'R', int.class, float.class, VO_KIND_BITWISE, VO_ALL);
    /** Reinterpret bits of {@code longVal} as {@code double}. As if by {@link Double#longBitsToDouble(long)} */
    public static final Conversion<Long,Double> REINTERPRET_L2D = convert("REINTERPRET_L2D", 'R', long.class, double.class, VO_KIND_BITWISE, VO_ALL);
    /** Zero-extend {@code byteVal} to {@code int}. */
    public static final Conversion<Byte,Integer> ZERO_EXTEND_B2I = convert("ZERO_EXTEND_B2I", 'Z', byte.class, int.class, VO_KIND_BITWISE, VO_ALL);
    /** Zero-extend {@code byteVal} to {@code long}. */
    public static final Conversion<Byte,Long> ZERO_EXTEND_B2L = convert("ZERO_EXTEND_B2L", 'Z', byte.class, long.class, VO_KIND_BITWISE, VO_ALL);
    /** Zero-extend {@code byteVal} to {@code short}. */
    public static final Conversion<Byte,Short> ZERO_EXTEND_B2S = convert("ZERO_EXTEND_B2S", 'Z', byte.class, short.class, VO_KIND_BITWISE, VO_ALL);
    /** Zero-extend {@code intVal} to {@code long}. */
    public static final Conversion<Integer,Long> ZERO_EXTEND_I2L = convert("ZERO_EXTEND_I2L", 'Z', int.class, long.class, VO_KIND_BITWISE, VO_ALL);
    /** Zero-extend {@code shortVal} to {@code int}. */
    public static final Conversion<Short,Integer> ZERO_EXTEND_S2I = convert("ZERO_EXTEND_S2I", 'Z', short.class, int.class, VO_KIND_BITWISE, VO_ALL);
    /** Zero-extend {@code shortVal} to {@code long}. */
    public static final Conversion<Short,Long> ZERO_EXTEND_S2L = convert("ZERO_EXTEND_S2L", 'Z', short.class, long.class, VO_KIND_BITWISE, VO_ALL);

    // (End of conversion operators)

    private static int opInfo(int opCode, int bits) {
        if (opCode >= 0) {
            bits |= VO_OPCODE_VALID;
        } else {
            opCode &= (VO_OPCODE_LIMIT - 1);  // not a valid op
            bits |= VO_SPECIAL;  // mark for special handling
        }
        assert((bits >> VO_OPCODE_SHIFT) == 0);
        assert(opCode >= 0 && opCode < VO_OPCODE_LIMIT);
        return (opCode << VO_OPCODE_SHIFT) + bits;
    }

    private static Unary unary(String name, String opName, int opCode, int flags) {
        if (opCode >= 0 && (flags & VO_PRIVATE) == 0)
            OPC_NAME.put(opCode, name);
        return new UnaryImpl(name, opName, opInfo(opCode, flags | VO_UNARY));
    }

    private static Binary binary(String name, String opName, int opCode, int flags) {
        if (opCode >= 0 && (flags & VO_PRIVATE) == 0)
            OPC_NAME.put(opCode, name);
        return new BinaryImpl(name, opName, opInfo(opCode, flags | VO_BINARY));
    }

    private static Ternary ternary(String name, String opName, int opCode, int flags) {
        if (opCode >= 0 && (flags & VO_PRIVATE) == 0)
            OPC_NAME.put(opCode, name);
        return new TernaryImpl(name, opName, opInfo(opCode, flags | VO_TERNARY));
    }

    private static Associative assoc(String name, String opName, int opCode, int flags) {
        if (opCode >= 0 && (flags & VO_PRIVATE) == 0)
            OPC_NAME.put(opCode, name);
        return new AssociativeImpl(name, opName, opInfo(opCode, flags | VO_BINARY | VO_ASSOC));
    }

    private static Test predicate(String name, String opName, int opCode, int flags) {
        if (opCode >= 0 && (flags & VO_PRIVATE) == 0)
            CMP_OPC_NAME.put(opCode, name);
        return new TestImpl(name, opName, opInfo(opCode, flags | VO_UNARY | VO_BOOL));
    }

    private static Comparison compare(String name, String opName, int opCode, int flags) {
        if (opCode >= 0 && (flags & VO_PRIVATE) == 0)
            CMP_OPC_NAME.put(opCode, name);
        return new ComparisonImpl(name, opName, opInfo(opCode, flags | VO_BINARY | VO_BOOL));
    }

    private static <E,F> ConversionImpl<E,F>
    convert(String name, char kind, Class<E> dom, Class<F> ran, int opCode, int flags) {
        int domran = ((LaneType.of(dom).basicType << VO_DOM_SHIFT) +
                      (LaneType.of(ran).basicType << VO_RAN_SHIFT));
        if (opCode >= 0) {
            if ((opCode & VO_DOM_RAN_MASK) == 0) {
                opCode += domran;
            }
            if ((flags & VO_PRIVATE) == 0)
                CONV_OPC_NAME.put(opCode, name);
        }
        String opName = dom+"-"+kind+"-"+ran; //??
        return new ConversionImpl<>(name, opName, opInfo(opCode, flags | VO_UNARY | VO_CONV),
                                    kind, dom, ran);
    }

    private static abstract class OperatorImpl implements Operator {
        private final String symName;
        private final String opName;
        private final int opInfo;

        OperatorImpl(String symName, String opName, int opInfo) {
            this.symName = symName;
            this.opName = opName;
            this.opInfo = opInfo;
            assert(opInfo != 0);
        }

        @Override
        public final String name() {
            return symName;
        }
        @Override
        public final String operatorName() {
            return opName;
        }
        @Override
        public final String toString() {
            return name();
        }
        @Override
        public final int arity() {
            return opInfo & VO_ARITY_MASK;
        }
        @Override
        public final boolean isBoolean() {
            return opKind(VO_BOOL);
        }
        @Override
        public Class<?> rangeType() {
            return Object.class;
        }
        @Override
        public final boolean isAssociative() {
            return opKind(VO_ASSOC);
        }

        @ForceInline
        public boolean compatibleWith(Class<?> elementType) {
            return compatibleWith(LaneType.of(elementType));
        }

        /*package-private*/
        @ForceInline
        int opInfo() {
            return opInfo;
        }

        /*package-private*/
        @ForceInline
        int opCode(int requireKind, int forbidKind) {
            int opc = opCodeRaw();
            if ((opInfo & requireKind) != requireKind ||
                (forbidKind != 0 &&
                 (opInfo & forbidKind)  == forbidKind)) {
                throw illegalOperation(requireKind, forbidKind);
            }
            return opc;
        }

        /*package-private*/
        @ForceInline
        int opCodeRaw() {
            return (opInfo >> VO_OPCODE_SHIFT);
        }

        /*package-private*/
        UnsupportedOperationException
        illegalOperation(int requireKind, int forbidKind) {
            String msg1 = "";
            requireKind &=~ VO_OPCODE_VALID;
            switch (requireKind) {
            case VO_ONLYFP:  msg1 = "floating point operator required here"; break;
            case VO_NOFP:    msg1 = "integral/bitwise operator required here"; break;
            case VO_ASSOC:   msg1 = "associative operator required here"; break;
            }
            String msg2 = "";
            switch (forbidKind) {
            case VO_ONLYFP:  msg2 = "inapplicable floating point operator"; break;
            case VO_NOFP:    msg2 = "inapplicable integral/bitwise operator"; break;
            }
            if ((opInfo & VO_OPCODE_VALID) == 0) {
                msg2 = "operator is not implemented";
            }
            return illegalOperation(msg1, msg2);
        }

        /*package-private*/
        UnsupportedOperationException
        illegalOperation(String msg1, String msg2) {
            String dot = "";
            if (!msg1.isEmpty() && !msg2.isEmpty()) {
                dot = "; ";
            } else if (msg1.isEmpty() && msg2.isEmpty()) {
                // Couldn't decode the *kind bits.
                msg1 = "illegal operator";
            }
            String msg = String.format("%s: %s%s%s", this, msg1, dot, msg2);
            return new UnsupportedOperationException(msg);
        }


        /*package-private*/
        @ForceInline
        boolean opKind(int kindBit) {
            return (opInfo & kindBit) != 0;
        }

        @ForceInline
        /*package-private*/
        boolean compatibleWith(LaneType laneType) {
            if (laneType.elementKind == 'F') {
                return !opKind(VO_NOFP);
            } else if (laneType.elementKind == 'I') {
                return !opKind(VO_ONLYFP);
            } else {
                throw new AssertionError();
            }
        }
    }

    private static class UnaryImpl extends OperatorImpl implements Unary {
        private UnaryImpl(String symName, String opName, int opInfo) {
            super(symName, opName, opInfo);
            assert((opInfo & VO_ARITY_MASK) == VO_UNARY);
        }
    }

    private static class BinaryImpl extends OperatorImpl implements Binary {
        private BinaryImpl(String symName, String opName, int opInfo) {
            super(symName, opName, opInfo);
            assert((opInfo & VO_ARITY_MASK) == VO_BINARY);
        }
    }

    private static class TernaryImpl extends OperatorImpl implements Ternary {
        private TernaryImpl(String symName, String opName, int opInfo) {
            super(symName, opName, opInfo);
            assert((opInfo & VO_ARITY_MASK) == VO_TERNARY);
        }
    }

    private static class AssociativeImpl extends BinaryImpl implements Associative {
        private AssociativeImpl(String symName, String opName, int opInfo) {
            super(symName, opName, opInfo);
        }
    }

    /*package-private*/
    static
    class ConversionImpl<E,F> extends OperatorImpl
                              implements Conversion<E,F> {
        private ConversionImpl(String symName, String opName, int opInfo,
                               char kind, Class<E> dom, Class<F> ran) {
            super(symName, opName, opInfo);
            assert((opInfo & VO_ARITY_MASK) == VO_UNARY);
            this.kind = kind;
            this.dom = LaneType.of(dom);
            this.ran = LaneType.of(ran);
            check(dom, ran);  // make sure it all lines up
        }
        private final char kind;
        private final LaneType dom;
        private final LaneType ran;

        // non-overrides are all package-private

        char kind()  { return kind; }
        LaneType domain() { return dom; }
        LaneType range()  { return ran; }

        int sizeChangeLog2() {
            return ran.elementSizeLog2 - dom.elementSizeLog2;
        }

        @SuppressWarnings("unchecked")
        @Override
        public Class<E> domainType() {
            return (Class<E>) dom.elementType;
        }
        @SuppressWarnings("unchecked")
        @Override
        public Class<F> rangeType() {
            return (Class<F>) ran.elementType;
        }

        @SuppressWarnings("unchecked")
        @ForceInline
        public
        <E,F> Conversion<E,F>
        check(Class<E> dom, Class<F> ran) {
            if (this.dom.elementType != dom ||
                this.ran.elementType != ran)
                throw checkFailed(dom, ran);
            return (Conversion<E,F>) this;
        }
        private RuntimeException checkFailed(Class<?> dom, Class<?> ran) {
            return new ClassCastException(toString()+": not "+dom+" -> "+ran);
        }

        static ConversionImpl<?,?> ofCopy(LaneType dom) {
            return findConv('I', dom, dom);
        }
        static ConversionImpl<?,?> ofCast(LaneType dom, LaneType ran) {
            if (dom == ran)  return ofCopy(dom);
            return findConv('C', dom, ran);
        }
        static ConversionImpl<?,?> ofReinterpret(LaneType dom, LaneType ran) {
            if (dom == ran)  return ofCopy(dom);
            if (dom.elementKind == 'I' &&
                ran.elementKind == 'I' &&
                dom.elementSize < ran.elementSize) {
                // Zero extension of field (unsigned semantics).
                return findConv('Z', dom, ran);
            }
            // if (dom.elementSize != ran.elementSize) {
            //     throw new IllegalArgumentException("bad reinterpret");
            // }
            return findConv('R', dom, ran);
        }

        @ForceInline
        private static ConversionImpl<?,?>
        findConv(char kind, LaneType dom, LaneType ran) {
            ConversionImpl<?,?>[] cache = cacheOf(kind, dom);
            int ranKey = ran.switchKey;
            ConversionImpl<?,?> conv = cache[ranKey];
            if (conv != null) {
                return conv;
            }
            return makeConv(kind, dom, ran);
        }

        static String a2b(LaneType dom, LaneType ran) {
            return dom.typeChar + "2" + ran.typeChar;
        }

        static ConversionImpl<?,?>
        makeConv(char kind, LaneType dom, LaneType ran) {
            String name;
            Class<?> domType = dom.elementType;
            Class<?> ranType = ran.elementType;
            int domCode = (dom.basicType << VO_DOM_SHIFT);
            int ranCode = (ran.basicType << VO_RAN_SHIFT);
            int opCode = domCode + ranCode;
            switch (kind) {
            case 'I':
                assert(dom == ran);
                name = "COPY_"+a2b(dom, ran);
                opCode = VO_KIND_CAST;
                break;
            case 'C':
                name = ""+a2b(dom, ran);
                opCode = VO_KIND_CAST;
                break;
            case 'R':
                name = "REINTERPRET_"+a2b(dom, ran);
                opCode = VO_KIND_BITWISE;
                break;
            case 'Z':
                name = "ZERO_EXTEND_"+a2b(dom, ran);
                opCode = VO_KIND_BITWISE;
                break;
            default:  throw new AssertionError();
            }
            ConversionImpl<?,?> conv = convert(name, kind, domType, ranType, opCode, VO_ALL);
            // Put into the cache for next time.
            // The JIT can see into this cache
            // when kind/dom/ran are all constant.
            ConversionImpl<?,?>[] cache = cacheOf(kind, dom);
            int ranKey = ran.switchKey;
            // The extra "check" calls help validate that
            // we aren't cross-wiring the cache.
            conv.check(domType, ranType);
            synchronized (ConversionImpl.class) {
                if (cache[ranKey] == null) {
                    cache[ranKey] = conv;
                } else {
                    conv = cache[ranKey];
                    conv.check(domType, ranType);
                }
            }
            return conv;
        }
        private final void check(char kind, LaneType dom, LaneType ran) {
            if (this.kind != kind || this.dom != dom || this.ran != ran) {
                throw new AssertionError(this + " != " + dom + kind + ran);
            }
        }

        /** Helper for cache probes. */
        @ForceInline
        private static ConversionImpl<?,?>[]
        cacheOf(char kind, LaneType dom) {
            assert("CIRZWN".indexOf(kind) >= 0);
            int k = (kind <= 'I' ? KIND_CI :
                     (kind == 'R' || kind == 'Z') ? KIND_RZ :
                     KIND_WN);
            return CACHES[k][dom.switchKey];
        }
        private static final int
            LINE_LIMIT = LaneType.SK_LIMIT,
            KIND_CI = 0, KIND_RZ = 1, KIND_WN = 2, KIND_LIMIT = 3;
        private static final @Stable ConversionImpl<?,?>[][][]
            CACHES = new ConversionImpl<?,?>[KIND_LIMIT][LINE_LIMIT][LINE_LIMIT];

        private synchronized static void initCaches() {
            for (var f : VectorOperators.class.getFields()) {
                if (f.getType() != Conversion.class)  continue;
                ConversionImpl<?,?> conv;
                try {
                    conv = (ConversionImpl) f.get(null);
                } catch (ReflectiveOperationException ex) {
                    throw new AssertionError(ex);
                }
                LaneType dom = conv.dom;
                LaneType ran = conv.ran;
                int opc = conv.opCodeRaw();
                switch (conv.kind) {
                case 'W':
                    int domCode = (opc >> VO_DOM_SHIFT) & 0xF;
                    dom = LaneType.ofBasicType(domCode);
                    break;
                case 'N':
                    int ranCode = (opc >> VO_RAN_SHIFT) & 0xF;
                    ran = LaneType.ofBasicType(ranCode);
                    break;
                }
                assert((opc & VO_DOM_RAN_MASK) ==
                       ((dom.basicType << VO_DOM_SHIFT) +
                        (ran.basicType << VO_RAN_SHIFT)));
                ConversionImpl<?,?>[] cache = cacheOf(conv.kind, dom);
                int ranKey = ran.switchKey;
                if (cache[ranKey] != conv) {
                    assert(cache[ranKey] == null ||
                           cache[ranKey].name().equals(conv.name()))
                        : conv + " vs. " + cache[ranKey];
                    cache[ranKey] = conv;
                }
            }
        }

        // hack for generating static field defs
        static { assert(genCode()); }
        private static boolean genCode() {
            if (true)  return true;  // remove to enable code
            ArrayList<String> defs = new ArrayList<>();
            for (LaneType l1 : LaneType.values()) {
                for (LaneType l2 : LaneType.values()) {
                    for (int i = 0; i <= 1; i++) {
                        ConversionImpl<?,?> c;
                        try {
                            c = ((i == 0) ? ofCast(l1, l2) : ofReinterpret(l1, l2));
                        } catch (IllegalArgumentException ex) {
                            assert((i == 1 && l1.elementSize != l2.elementSize) ||
                                   (i == 2 && l1.elementSize == l2.elementSize));
                            continue;  // ignore this combo
                        }
                        if (c.kind == 'C' ||
                            c.kind == 'Z' ||
                            (c.kind == 'R' &&
                             l1.elementKind+l2.elementKind == 'F'+'I' &&
                             l1.elementSize == l2.elementSize) ||
                            (c.kind == 'N' || c.kind == 'W')) {
                            int opc = c.opCodeRaw();
                            String opcs;
                            switch (opc & ~VO_DOM_RAN_MASK) {
                            case VO_KIND_CAST: opcs = "VO_KIND_CAST"; break;
                            case VO_KIND_BITWISE: opcs = "VO_KIND_BITWISE"; break;
                            default: opcs = Integer.toHexString(opc);
                            }
                            String code = c.genCode(opcs);
                            if (!defs.contains(code))  defs.add(code);
                        }
                    }
                }
            }
            java.util.Collections.sort(defs);
            for (String def : defs)  System.out.println(def);
            return true;
        }
        private String genCode(String opcs) {
            if (true)  return null;  // remove to enable code
            int domran = opCodeRaw() & VO_DOM_RAN_MASK;
            switch (kind()) {
            case 'W': case 'N':
                opcs += " + 0x" + Integer.toHexString(domran);
            }
            String doc;
            switch (kind()) {
            case 'R':
                doc = "Reinterpret bits of {@code _domVal} as {@code _ran}";
                break;
            case 'Z':
                doc = "Zero-extend {@code _domVal} to {@code _ran}";
                break;
            case 'W':
                doc = "In-place widen {@code _domVal} inside _ran to {@code (_ran)_domVal}";
                LaneType logdom = LaneType.ofBasicType(domran >> VO_DOM_SHIFT & 0xF);
                doc = doc.replace("_dom", logdom.elementType.getSimpleName());
                break;
            case 'N':
                doc = "In-place narrow {@code _domVal} to {@code (_ran)_domVal} inside _dom";
                LaneType logran = LaneType.ofBasicType(domran >> VO_RAN_SHIFT & 0xF);
                doc = doc.replace("_ran", logran.elementType.getSimpleName());
                break;
            default:
                doc = "Convert {@code _domVal} to {@code (_ran)_domVal}";
            }
            String code = (
                    "    /** _Doc. */" + "\n" +
                    "    public static final Conversion<_Dom,_Ran> _N" +
                    " = convert(\"_N\", '_K', _dom.class, _ran.class, _opc, VO_ALL);");
            return code
                .replace("_Doc", doc)
                .replace("_dom", dom.elementType.getSimpleName())
                .replace("_ran", ran.elementType.getSimpleName())
                .replace("_Dom", dom.genericElementType.getSimpleName())
                .replace("_Ran", ran.genericElementType.getSimpleName())
                .replace("_N", name())
                .replace("_K", ""+kind())
                .replace("_opc", ""+opcs);
        }
    }

    private static class TestImpl extends OperatorImpl implements Test {
        private TestImpl(String symName, String opName, int opInfo) {
            super(symName, opName, opInfo);
            assert((opInfo & VO_ARITY_MASK) == VO_UNARY);
            assert((opInfo & VO_BOOL) == VO_BOOL);
        }
        @Override
        public Class<?> rangeType() {
            return boolean.class;
        }
    }

    private static class ComparisonImpl extends OperatorImpl implements Comparison {
        private ComparisonImpl(String symName, String opName, int opInfo) {
            super(symName, opName, opInfo);
            assert((opInfo & VO_ARITY_MASK) == VO_BINARY);
            assert((opInfo & VO_BOOL) == VO_BOOL);
        }
        @Override
        public Class<?> rangeType() {
            return boolean.class;
        }
        /* --- *
        boolean test(long a, long b) {
            switch (opInfo() >> VO_OPCODE_SHIFT) {
            case VectorSupport.BT_eq:  return a == b;
            case VectorSupport.BT_ne:  return a != b;
            case VectorSupport.BT_lt:  return a <  b;
            case VectorSupport.BT_le:  return a <= b;
            case VectorSupport.BT_gt:  return a >  b;
            case VectorSupport.BT_ge:  return a >= b;
            }
            throw new AssertionError();
        }
        * --- */
    }

    static {
        ConversionImpl.initCaches();
        assert(checkConstants());
    }

    private static boolean checkConstants() {
        // Check uniqueness of opcodes, to prevent dumb aliasing errors.
        OperatorImpl[] ops = new OperatorImpl[VO_OPCODE_LIMIT << VO_OPCODE_SHIFT];
        for (var f : VectorOperators.class.getFields()) {
            Class<?> ft = f.getType();
            OperatorImpl op;
            try {
                op = (OperatorImpl) f.get(null);
            } catch (ReflectiveOperationException ex) {
                throw new AssertionError(ex);
            }
            assert(op.name().equals(f.getName())) : op;
            assert(op.isAssociative() == (ft == Associative.class)) : op;
            if (op.isBoolean()) {
                assert(ft == (op.arity() == 2 ? Comparison.class : Test.class)) : op;
            }
            if (ft == Unary.class || ft == Conversion.class || ft == Test.class) {
                assert(op.arity() == 1) : op;
            } else if (ft == Ternary.class) {
                assert(op.arity() == 3) : op;
            } else {
                assert(op.arity() == 2) : op;
                if (ft != Associative.class &&
                    ft != Comparison.class) {
                    assert(ft == Binary.class) : op;
                }
            }
            if (op.opKind(VO_OPCODE_VALID)) {
                int opsMask = (((VO_OPCODE_LIMIT-1) << VO_OPCODE_SHIFT)
                               | VO_BOOL | VO_CONV
                               | VO_ARITY_MASK);
                int opsIndex = op.opInfo & opsMask;
                OperatorImpl op0 = ops[opsIndex];
                assert(op0 == null)
                    : java.util.Arrays.asList(op0, Integer.toHexString(op0.opInfo), op, Integer.toHexString(op.opInfo));
                ops[opsIndex] = op;
            } else {
                // These are all the "-1" opcode guys we know about:
                assert(op == ZOMO ||
                       op == FIRST_NONZERO ||
                       op == AND_NOT || op == NOT ||
                       op == ROL ||
                       op == ROR ||
                       op == IS_DEFAULT || op == IS_NEGATIVE ||
                       op == IS_FINITE || op == IS_NAN || op == IS_INFINITE ||
                       op == BITWISE_BLEND) : op;
            }
        }
        return true;
    }

    // Managing behavioral information on slow paths:
    /*package-private*/
    static class ImplCache<OP extends Operator,T> {
        public ImplCache(Class<OP> whatKind,
                         Class<? extends Vector<?>> whatVec) {
            this.whatKind = whatKind;
            this.whatVec = whatVec;
        }

        // These are used only for forming diagnostics:
        private final Class<OP> whatKind;
        private final Class<? extends Vector<?>> whatVec;

        private final @Stable
        Object[] cache = new Object[VO_OPCODE_LIMIT];

        @ForceInline
        public T find(OP op, int opc, IntFunction<T> supplier) {
            @SuppressWarnings("unchecked")
            T fn = (T) cache[opc];
            if (fn != null)  return fn;
            fn = supplier.apply(opc);
            if (fn == null)  throw badOp(op);
            assert(VectorSupport.isNonCapturingLambda(fn)) : fn;
            // The JIT can see into this cache:
            cache[opc] = fn;
            return fn;
        }

        private UnsupportedOperationException badOp(Operator op) {
            String msg = String.format("%s: illegal %s in %s",
                                       op,
                                       whatKind.getSimpleName().toLowerCase(),
                                       whatVec.getSimpleName());
            return new UnsupportedOperationException(msg);
        }

        @Override public String toString() {
            ArrayList<String> entries = new ArrayList<>();
            for (int i = 0; i < cache.length; i++) {
                Object fn = cache[i];
                if (fn != null)  entries.add(i+": "+fn);
            }
            return String.format("ImplCache<%s,%s>[%s]",
                                 whatKind.getSimpleName(),
                                 whatVec.getSimpleName(),
                                 String.join(", ", entries));
        }
    }
}
