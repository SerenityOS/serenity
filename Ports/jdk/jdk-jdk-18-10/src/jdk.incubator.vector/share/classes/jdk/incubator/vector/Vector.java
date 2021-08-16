/*
 * Copyright (c) 2017, 2020, Oracle and/or its affiliates. All rights reserved.
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
import java.util.Arrays;

/**
 * A
 *
 * <!-- The following paragraphs are shared verbatim
 *   -- between Vector.java and package-info.java -->
 * sequence of a fixed number of <em>lanes</em>,
 * all of some fixed
 * {@linkplain Vector#elementType() <em>element type</em>}
 * such as {@code byte}, {@code long}, or {@code float}.
 * Each lane contains an independent value of the element type.
 * Operations on vectors are typically
 * <a href="Vector.html#lane-wise"><em>lane-wise</em></a>,
 * distributing some scalar operator (such as
 * {@linkplain Vector#add(Vector) addition})
 * across the lanes of the participating vectors,
 * usually generating a vector result whose lanes contain the various
 * scalar results.  When run on a supporting platform, lane-wise
 * operations can be executed in parallel by the hardware.  This style
 * of parallelism is called <em>Single Instruction Multiple Data</em>
 * (SIMD) parallelism.
 *
 * <p> In the SIMD style of programming, most of the operations within
 * a vector lane are unconditional, but the effect of conditional
 * execution may be achieved using
 * <a href="Vector.html#masking"><em>masked operations</em></a>
 * such as {@link Vector#blend(Vector,VectorMask) blend()},
 * under the control of an associated {@link VectorMask}.
 * Data motion other than strictly lane-wise flow is achieved using
 * <a href="Vector.html#cross-lane"><em>cross-lane</em></a>
 * operations, often under the control of an associated
 * {@link VectorShuffle}.
 * Lane data and/or whole vectors can be reformatted using various
 * kinds of lane-wise
 * {@linkplain Vector#convert(VectorOperators.Conversion,int) conversions},
 * and byte-wise reformatting
 * {@linkplain Vector#reinterpretShape(VectorSpecies,int) reinterpretations},
 * often under the control of a reflective {@link VectorSpecies}
 * object which selects an alternative vector format different
 * from that of the input vector.
 *
 * <p> {@code Vector<E>} declares a set of vector operations (methods)
 * that are common to all element types.  These common operations
 * include generic access to lane values, data selection and movement,
 * reformatting, and certain arithmetic and logical operations (such as addition
 * or comparison) that are common to all primitive types.
 *
 * <p> <a href="Vector.html#subtypes">Public subtypes of {@code Vector}</a>
 * correspond to specific
 * element types.  These declare further operations that are specific
 * to that element type, including unboxed access to lane values,
 * bitwise operations on values of integral element types, or
 * transcendental operations on values of floating point element
 * types.
 *
 * <p> Some lane-wise operations, such as the {@code add} operator, are defined as
 * a full-service named operation, where a corresponding method on {@code Vector}
 * comes in masked and unmasked overloadings, and (in subclasses) also comes in
 * covariant overrides (returning the subclass) and additional scalar-broadcast
 * overloadings (both masked and unmasked).
 *
 * Other lane-wise operations, such as the {@code min} operator, are defined as a
 * partially serviced (not a full-service) named operation, where a corresponding
 * method on {@code Vector} and/or a subclass provide some but all possible
 * overloadings and overrides (commonly the unmasked varient with scalar-broadcast
 * overloadings).
 *
 * Finally, all lane-wise operations (those named as previously described,
 * or otherwise unnamed method-wise) have a corresponding
 * {@link VectorOperators.Operator operator token}
 * declared as a static constant on {@link VectorOperators}.
 * Each operator token defines a symbolic Java expression for the operation,
 * such as {@code a + b} for the
 * {@link VectorOperators#ADD ADD} operator token.
 * General lane-wise operation-token accepting methods, such as for a
 * {@linkplain Vector#lanewise(VectorOperators.Unary) unary lane-wise}
 * operation, are provided on {@code Vector} and come in the same variants as
 * a full-service named operation.
 *
 * <p>This package contains a public subtype of {@link Vector}
 * corresponding to each supported element type:
 * {@link ByteVector}, {@link ShortVector},
 * {@link IntVector}, {@link LongVector},
 * {@link FloatVector}, and {@link DoubleVector}.
 *
 * <!-- The preceding paragraphs are shared verbatim
 *   -- between Vector.java and package-info.java -->
 *
 * <p><a id="ETYPE"></a> The {@linkplain #elementType element type} of a vector,
 * referred to as {@code ETYPE}, is one of the primitive types
 * {@code byte}, {@code short}, {@code int}, {@code long}, {@code
 * float}, or {@code double}.
 *
 * <p> The type {@code E} in {@code Vector<E>} is the <em>boxed</em> version
 * of {@code ETYPE}. For example, in the type {@code Vector<Integer>}, the {@code E}
 * parameter is {@code Integer} and the {@code ETYPE} is {@code int}.  In such a
 * vector, each lane carries a primitive {@code int} value.  This pattern continues
 * for the other primitive types as well. (See also sections {@jls 5.1.7} and
 * {@jls 5.1.8} of the <cite>The Java Language Specification</cite>.)
 *
 * <p><a id="VLENGTH"></a> The {@linkplain #length() length} of a vector
 * is the lane count, the number of lanes it contains.
 *
 * This number is also called {@code VLENGTH} when the context makes
 * clear which vector it belongs to.  Each vector has its own fixed
 * {@code VLENGTH} but different instances of vectors may have
 * different lengths.  {@code VLENGTH} is an important number, because
 * it estimates the SIMD performance gain of a single vector operation
 * as compared to scalar execution of the {@code VLENGTH} scalar
 * operators which underly the vector operation.
 *
 * <h2><a id="species"></a>Shapes and species</h2>
 *
 * The information capacity of a vector is determined by its
 * {@linkplain #shape() <em>vector shape</em>}, also called its
 * {@code VSHAPE}.  Each possible {@code VSHAPE} is represented by
 * a member of the {@link VectorShape} enumeration, and represents
 * an implementation format shared in common by all vectors of
 * that shape.  Thus, the {@linkplain #bitSize() size in bits} of
 * of a vector is determined by appealing to its vector shape.
 *
 * <p> Some Java platforms give special support to only one shape,
 * while others support several.  A typical platform is not likely
 * to support all the shapes described by this API.  For this reason,
 * most vector operations work on a single input shape and
 * produce the same shape on output.  Operations which change
 * shape are clearly documented as such <em>shape-changing</em>,
 * while the majority of operations are <em>shape-invariant</em>,
 * to avoid disadvantaging platforms which support only one shape.
 * There are queries to discover, for the current Java platform,
 * the {@linkplain VectorShape#preferredShape() preferred shape}
 * for general SIMD computation, or the
 * {@linkplain VectorShape#largestShapeFor(Class) largest
 * available shape} for any given lane type.  To be portable,
 * code using this API should start by querying a supported
 * shape, and then process all data with shape-invariant
 * operations, within the selected shape.
 *
 * <p> Each unique combination of element type and vector shape
 * determines a unique
 * {@linkplain #species() <em>vector species</em>}.
 * A vector species is represented by a fixed instance of
 * {@link VectorSpecies VectorSpecies&lt;E&gt;}
 * shared in common by all vectors of the same shape and
 * {@code ETYPE}.
 *
 * <p> Unless otherwise documented, lane-wise vector operations
 * require that all vector inputs have exactly the same {@code VSHAPE}
 * and {@code VLENGTH}, which is to say that they must have exactly
 * the same species.  This allows corresponding lanes to be paired
 * unambiguously.  The {@link #check(VectorSpecies) check()} method
 * provides an easy way to perform this check explicitly.
 *
 * <p> Vector shape, {@code VLENGTH}, and {@code ETYPE} are all
 * mutually constrained, so that {@code VLENGTH} times the
 * {@linkplain #elementSize() bit-size of each lane}
 * must always match the bit-size of the vector's shape.
 *
 * Thus, {@linkplain #reinterpretShape(VectorSpecies,int) reinterpreting} a
 * vector may double its length if and only if it either halves the lane size,
 * or else changes the shape.  Likewise, reinterpreting a vector may double the
 * lane size if and only if it either halves the length, or else changes the
 * shape of the vector.
 *
 * <h2><a id="subtypes"></a>Vector subtypes</h2>
 *
 * Vector declares a set of vector operations (methods) that are common to all
 * element types (such as addition).  Sub-classes of Vector with a concrete
 * element type declare further operations that are specific to that
 * element type (such as access to element values in lanes, logical operations
 * on values of integral elements types, or transcendental operations on values
 * of floating point element types).
 * There are six abstract sub-classes of Vector corresponding to the supported set
 * of element types, {@link ByteVector}, {@link ShortVector},
 * {@link IntVector}, {@link LongVector}, {@link FloatVector}, and
 * {@link DoubleVector}. Along with type-specific operations these classes
 * support creation of vector values (instances of Vector).
 * They expose static constants corresponding to the supported species,
 * and static methods on these types generally take a species as a parameter.
 * For example,
 * {@link FloatVector#fromArray(VectorSpecies, float[], int) FloatVector.fromArray}
 * creates and returns a float vector of the specified species, with elements
 * loaded from the specified float array.
 * It is recommended that Species instances be held in {@code static final}
 * fields for optimal creation and usage of Vector values by the runtime compiler.
 *
 * <p> As an example of static constants defined by the typed vector classes,
 * constant {@link FloatVector#SPECIES_256 FloatVector.SPECIES_256}
 * is the unique species whose lanes are {@code float}s and whose
 * vector size is 256 bits.  Again, the constant
 * {@link FloatVector#SPECIES_PREFERRED} is the species which
 * best supports processing of {@code float} vector lanes on
 * the currently running Java platform.
 *
 * <p> As another example, a broadcast scalar value of
 * {@code (double)0.5} can be obtained by calling
 * {@link DoubleVector#broadcast(VectorSpecies,double)
 * DoubleVector.broadcast(dsp, 0.5)}, but the argument {@code dsp} is
 * required to select the species (and hence the shape and length) of
 * the resulting vector.
 *
 * <h2><a id="lane-wise"></a>Lane-wise operations</h2>
 *
 * We use the term <em>lanes</em> when defining operations on
 * vectors. The number of lanes in a vector is the number of scalar
 * elements it holds. For example, a vector of type {@code float} and
 * shape {@code S_256_BIT} has eight lanes, since {@code 32*8=256}.
 *
 * <p> Most operations on vectors are lane-wise, which means the operation
 * is composed of an underlying scalar operator, which is repeated for
 * each distinct lane of the input vector.  If there are additional
 * vector arguments of the same type, their lanes are aligned with the
 * lanes of the first input vector.  (They must all have a common
 * {@code VLENGTH}.)  For most lane-wise operations, the output resulting
 * from a lane-wise operation will have a {@code VLENGTH} which is equal to
 * the {@code VLENGTH} of the input(s) to the operation.  Thus, such lane-wise
 * operations are <em>length-invariant</em>, in their basic definitions.
 *
 * <p> The principle of length-invariance is combined with another
 * basic principle, that most length-invariant lane-wise operations are also
 * <em>shape-invariant</em>, meaning that the inputs and the output of
 * a lane-wise operation will have a common {@code VSHAPE}.  When the
 * principles conflict, because a logical result (with an invariant
 * {@code VLENGTH}), does not fit into the invariant {@code VSHAPE},
 * the resulting expansions and contractions are handled explicitly
 * with
 * <a href="Vector.html#expansion">special conventions</a>.
 *
 * <p> Vector operations can be grouped into various categories and
 * their behavior can be generally specified in terms of underlying
 * scalar operators.  In the examples below, {@code ETYPE} is the
 * element type of the operation (such as {@code int.class}) and
 * {@code EVector} is the corresponding concrete vector type (such as
 * {@code IntVector.class}).
 *
 * <ul>
 * <li>
 * A <em>lane-wise unary</em> operation, such as
 * {@code w = v0.}{@link Vector#neg() neg}{@code ()},
 * takes one input vector,
 * distributing a unary scalar operator across the lanes,
 * and produces a result vector of the same type and shape.
 *
 * For each lane of the input vector {@code a},
 * the underlying scalar operator is applied to the lane value.
 * The result is placed into the vector result in the same lane.
 * The following pseudocode illustrates the behavior of this operation
 * category:
 *
 * <pre>{@code
 * ETYPE scalar_unary_op(ETYPE s);
 * EVector a = ...;
 * VectorSpecies<E> species = a.species();
 * ETYPE[] ar = new ETYPE[a.length()];
 * for (int i = 0; i < ar.length; i++) {
 *     ar[i] = scalar_unary_op(a.lane(i));
 * }
 * EVector r = EVector.fromArray(species, ar, 0);
 * }</pre>
 *
 * <li>
 * A <em>lane-wise binary</em> operation, such as
 * {@code w = v0.}{@link Vector#add(Vector) add}{@code (v1)},
 * takes two input vectors,
 * distributing a binary scalar operator across the lanes,
 * and produces a result vector of the same type and shape.
 *
 * For each lane of the two input vectors {@code a} and {@code b},
 * the underlying scalar operator is applied to the lane values.
 * The result is placed into the vector result in the same lane.
 * The following pseudocode illustrates the behavior of this operation
 * category:
 *
 * <pre>{@code
 * ETYPE scalar_binary_op(ETYPE s, ETYPE t);
 * EVector a = ...;
 * VectorSpecies<E> species = a.species();
 * EVector b = ...;
 * b.check(species);  // must have same species
 * ETYPE[] ar = new ETYPE[a.length()];
 * for (int i = 0; i < ar.length; i++) {
 *     ar[i] = scalar_binary_op(a.lane(i), b.lane(i));
 * }
 * EVector r = EVector.fromArray(species, ar, 0);
 * }</pre>
 * </li>
 *
 * <li>
 * Generalizing from unary and binary operations,
 * a <em>lane-wise n-ary</em> operation takes {@code N} input vectors {@code v[j]},
 * distributing an n-ary scalar operator across the lanes,
 * and produces a result vector of the same type and shape.
 * Except for a few ternary operations, such as
 * {@code w = v0.}{@link FloatVector#fma(Vector,Vector) fma}{@code (v1,v2)},
 * this API has no support for
 * lane-wise n-ary operations.
 *
 * For each lane of all of the input vectors {@code v[j]},
 * the underlying scalar operator is applied to the lane values.
 * The result is placed into the vector result in the same lane.
 * The following pseudocode illustrates the behavior of this operation
 * category:
 *
 * <pre>{@code
 * ETYPE scalar_nary_op(ETYPE... args);
 * EVector[] v = ...;
 * int N = v.length;
 * VectorSpecies<E> species = v[0].species();
 * for (EVector arg : v) {
 *     arg.check(species);  // all must have same species
 * }
 * ETYPE[] ar = new ETYPE[a.length()];
 * for (int i = 0; i < ar.length; i++) {
 *     ETYPE[] args = new ETYPE[N];
 *     for (int j = 0; j < N; j++) {
 *         args[j] = v[j].lane(i);
 *     }
 *     ar[i] = scalar_nary_op(args);
 * }
 * EVector r = EVector.fromArray(species, ar, 0);
 * }</pre>
 * </li>
 *
 * <li>
 * A <em>lane-wise conversion</em> operation, such as
 * {@code w0 = v0.}{@link
 * Vector#convert(VectorOperators.Conversion,int)
 * convert}{@code (VectorOperators.I2D, 0)},
 * takes one input vector,
 * distributing a unary scalar conversion operator across the lanes,
 * and produces a logical result of the converted values.  The logical
 * result (or at least a part of it) is presented in a vector of the
 * same shape as the input vector.
 *
 * <p> Unlike other lane-wise operations, conversions can change lane
 * type, from the input (domain) type to the output (range) type.  The
 * lane size may change along with the type.  In order to manage the
 * size changes, lane-wise conversion methods can product <em>partial
 * results</em>, under the control of a {@code part} parameter, which
 * is <a href="Vector.html#expansion">explained elsewhere</a>.
 * (Following the example above, the second group of converted lane
 * values could be obtained as
 * {@code w1 = v0.convert(VectorOperators.I2D, 1)}.)
 *
 * <p> The following pseudocode illustrates the behavior of this
 * operation category in the specific example of a conversion from
 * {@code int} to {@code double}, retaining either lower or upper
 * lanes (depending on {@code part}) to maintain shape-invariance:
 *
 * <pre>{@code
 * IntVector a = ...;
 * int VLENGTH = a.length();
 * int part = ...;  // 0 or 1
 * VectorShape VSHAPE = a.shape();
 * double[] arlogical = new double[VLENGTH];
 * for (int i = 0; i < limit; i++) {
 *     int e = a.lane(i);
 *     arlogical[i] = (double) e;
 * }
 * VectorSpecies<Double> rs = VSHAPE.withLanes(double.class);
 * int M = Double.BITS / Integer.BITS;  // expansion factor
 * int offset = part * (VLENGTH / M);
 * DoubleVector r = DoubleVector.fromArray(rs, arlogical, offset);
 * assert r.length() == VLENGTH / M;
 * }</pre>
 * </li>
 *
 * <li>
 * A <em>cross-lane reduction</em> operation, such as
 * {@code e = v0.}{@link
 * IntVector#reduceLanes(VectorOperators.Associative)
 * reduceLanes}{@code (VectorOperators.ADD)},
 * operates on all
 * the lane elements of an input vector.
 * An accumulation function is applied to all the
 * lane elements to produce a scalar result.
 * If the reduction operation is associative then the result may be accumulated
 * by operating on the lane elements in any order using a specified associative
 * scalar binary operation and identity value.  Otherwise, the reduction
 * operation specifies the order of accumulation.
 * The following pseudocode illustrates the behavior of this operation category
 * if it is associative:
 * <pre>{@code
 * ETYPE assoc_scalar_binary_op(ETYPE s, ETYPE t);
 * EVector a = ...;
 * ETYPE r = <identity value>;
 * for (int i = 0; i < a.length(); i++) {
 *     r = assoc_scalar_binary_op(r, a.lane(i));
 * }
 * }</pre>
 * </li>
 *
 * <li>
 * A <em>cross-lane movement</em> operation, such as
 * {@code w = v0.}{@link
 * Vector#rearrange(VectorShuffle) rearrange}{@code (shuffle)}
 * operates on all
 * the lane elements of an input vector and moves them
 * in a data-dependent manner into <em>different lanes</em>
 * in an output vector.
 * The movement is steered by an auxiliary datum, such as
 * a {@link VectorShuffle} or a scalar index defining the
 * origin of the movement.
 * The following pseudocode illustrates the behavior of this
 * operation category, in the case of a shuffle:
 * <pre>{@code
 * EVector a = ...;
 * Shuffle<E> s = ...;
 * ETYPE[] ar = new ETYPE[a.length()];
 * for (int i = 0; i < ar.length; i++) {
 *     int source = s.laneSource(i);
 *     ar[i] = a.lane(source);
 * }
 * EVector r = EVector.fromArray(a.species(), ar, 0);
 * }</pre>
 * </li>
 *
 * <li>
 * A <em>masked operation</em> is one which is a variation on one of the
 * previous operations (either lane-wise or cross-lane), where
 * the operation takes an extra trailing {@link VectorMask} argument.
 * In lanes the mask is set, the operation behaves as if the mask
 * argument were absent, but in lanes where the mask is unset, the
 * underlying scalar operation is suppressed.
 * Masked operations are explained in
 * <a href="Vector.html#masking">greater detail elsewhere</a>.
 * </li>
 *
 * <li>
 * A very special case of a masked lane-wise binary operation is a
 * {@linkplain #blend(Vector,VectorMask) blend}, which operates
 * lane-wise on two input vectors {@code a} and {@code b}, selecting lane
 * values from one input or the other depending on a mask {@code m}.
 * In lanes where {@code m} is set, the corresponding value from
 * {@code b} is selected into the result; otherwise the value from
 * {@code a} is selected.  Thus, a blend acts as a vectorized version
 * of Java's ternary selection expression {@code m?b:a}:
 * <pre>{@code
 * ETYPE[] ar = new ETYPE[a.length()];
 * for (int i = 0; i < ar.length; i++) {
 *     boolean isSet = m.laneIsSet(i);
 *     ar[i] = isSet ? b.lane(i) : a.lane(i);
 * }
 * EVector r = EVector.fromArray(species, ar, 0);
 * }</pre>
 * </li>
 *
 * <li>
 * A <em>lane-wise binary test</em> operation, such as
 * {@code m = v0.}{@link Vector#lt(Vector) lt}{@code (v1)},
 * takes two input vectors,
 * distributing a binary scalar comparison across the lanes,
 * and produces, not a vector of booleans, but rather a
 * {@linkplain VectorMask vector mask}.
 *
 * For each lane of the two input vectors {@code a} and {@code b},
 * the underlying scalar comparison operator is applied to the lane values.
 * The resulting boolean is placed into the vector mask result in the same lane.
 * The following pseudocode illustrates the behavior of this operation
 * category:
 * <pre>{@code
 * boolean scalar_binary_test_op(ETYPE s, ETYPE t);
 * EVector a = ...;
 * VectorSpecies<E> species = a.species();
 * EVector b = ...;
 * b.check(species);  // must have same species
 * boolean[] mr = new boolean[a.length()];
 * for (int i = 0; i < mr.length; i++) {
 *     mr[i] = scalar_binary_test_op(a.lane(i), b.lane(i));
 * }
 * VectorMask<E> m = VectorMask.fromArray(species, mr, 0);
 * }</pre>
 * </li>
 *
 * <li>
 * Similarly to a binary comparison, a <em>lane-wise unary test</em>
 * operation, such as
 * {@code m = v0.}{@link Vector#test(VectorOperators.Test)
 * test}{@code (IS_FINITE)},
 * takes one input vector, distributing a scalar predicate
 * (a test function) across the lanes, and produces a
 * {@linkplain VectorMask vector mask}.
 * </li>
 *
 * </ul>
 *
 * <p>
 * If a vector operation does not belong to one of the above categories then
 * the method documentation explicitly specifies how it processes the lanes of
 * input vectors, and where appropriate illustrates the behavior using
 * pseudocode.
 *
 * <p>
 * Most lane-wise binary and comparison operations offer convenience
 * overloadings which accept a scalar as the second input, in place of a
 * vector.  In this case the scalar value is promoted to a vector by
 * {@linkplain Vector#broadcast(long) broadcasting it}
 * into the same lane structure as the first input.
 *
 * For example, to multiply all lanes of a {@code double} vector by
 * a scalar value {@code 1.1}, the expression {@code v.mul(1.1)} is
 * easier to work with than an equivalent expression with an explicit
 * broadcast operation, such as {@code v.mul(v.broadcast(1.1))}
 * or {@code v.mul(DoubleVector.broadcast(v.species(), 1.1))}.
 *
 * Unless otherwise specified the scalar variant always behaves as if
 * each scalar value is first transformed to a vector of the same
 * species as the first vector input, using the appropriate
 * {@code broadcast} operation.
 *
 * <h2><a id="masking"></a>Masked operations</h2>
 *
 * <p> Many vector operations accept an optional
 * {@link VectorMask mask} argument, selecting which lanes participate
 * in the underlying scalar operator.  If present, the mask argument
 * appears at the end of the method argument list.
 *
 * <p> Each lane of the mask argument is a boolean which is either in
 * the <em>set</em> or <em>unset</em> state.  For lanes where the mask
 * argument is unset, the underlying scalar operator is suppressed.
 * In this way, masks allow vector operations to emulate scalar
 * control flow operations, without losing SIMD parallelism, except
 * where the mask lane is unset.
 *
 * <p> An operation suppressed by a mask will never cause an exception
 * or side effect of any sort, even if the underlying scalar operator
 * can potentially do so.  For example, an unset lane that seems to
 * access an out of bounds array element or divide an integral value
 * by zero will simply be ignored.  Values in suppressed lanes never
 * participate or appear in the result of the overall operation.
 *
 * <p> Result lanes corresponding to a suppressed operation will be
 * filled with a default value which depends on the specific
 * operation, as follows:
 *
 * <ul>
 *
 * <li>If the masked operation is a unary, binary, or n-ary arithmetic or
 * logical operation, suppressed lanes are filled from the first
 * vector operand (i.e., the vector receiving the method call), as if
 * by a {@linkplain #blend(Vector,VectorMask) blend}.</li>
 *
 * <li>If the masked operation is a memory load or a {@code slice()} from
 * another vector, suppressed lanes are not loaded, and are filled
 * with the default value for the {@code ETYPE}, which in every case
 * consists of all zero bits.  An unset lane can never cause an
 * exception, even if the hypothetical corresponding memory location
 * does not exist (because it is out of an array's index range).</li>
 *
 * <li>If the operation is a cross-lane operation with an operand
 * which supplies lane indexes (of type {@code VectorShuffle} or
 * {@code Vector}, suppressed lanes are not computed, and are filled
 * with the zero default value.  Normally, invalid lane indexes elicit
 * an {@code IndexOutOfBoundsException}, but if a lane is unset, the
 * zero value is quietly substituted, regardless of the index.  This
 * rule is similar to the previous rule, for masked memory loads.</li>
 *
 * <li>If the masked operation is a memory store or an {@code unslice()} into
 * another vector, suppressed lanes are not stored, and the
 * corresponding memory or vector locations (if any) are unchanged.
 *
 * <p> (Note: Memory effects such as race conditions never occur for
 * suppressed lanes.  That is, implementations will not secretly
 * re-write the existing value for unset lanes.  In the Java Memory
 * Model, reassigning a memory variable to its current value is not a
 * no-op; it may quietly undo a racing store from another
 * thread.)</p>
 * </li>
 *
 * <li>If the masked operation is a reduction, suppressed lanes are ignored
 * in the reduction.  If all lanes are suppressed, a suitable neutral
 * value is returned, depending on the specific reduction operation,
 * and documented by the masked variant of that method.  (This means
 * that users can obtain the neutral value programmatically by
 * executing the reduction on a dummy vector with an all-unset mask.)
 *
 * <li>If the masked operation is a comparison operation, suppressed output
 * lanes in the resulting mask are themselves unset, as if the
 * suppressed comparison operation returned {@code false} regardless
 * of the suppressed input values.  In effect, it is as if the
 * comparison operation were performed unmasked, and then the
 * result intersected with the controlling mask.</li>
 *
 * <li>In other cases, such as masked
 * <a href="Vector.html#cross-lane"><em>cross-lane movements</em></a>,
 * the specific effects of masking are documented by the masked
 * variant of the method.
 *
 * </ul>
 *
 * <p> As an example, a masked binary operation on two input vectors
 * {@code a} and {@code b} suppresses the binary operation for lanes
 * where the mask is unset, and retains the original lane value from
 * {@code a}.  The following pseudocode illustrates this behavior:
 * <pre>{@code
 * ETYPE scalar_binary_op(ETYPE s, ETYPE t);
 * EVector a = ...;
 * VectorSpecies<E> species = a.species();
 * EVector b = ...;
 * b.check(species);  // must have same species
 * VectorMask<E> m = ...;
 * m.check(species);  // must have same species
 * boolean[] ar = new boolean[a.length()];
 * for (int i = 0; i < ar.length; i++) {
 *     if (m.laneIsSet(i)) {
 *         ar[i] = scalar_binary_op(a.lane(i), b.lane(i));
 *     } else {
 *         ar[i] = a.lane(i);  // from first input
 *     }
 * }
 * EVector r = EVector.fromArray(species, ar, 0);
 * }</pre>
 *
 * <h2><a id="lane-order"></a>Lane order and byte order</h2>
 *
 * The number of lane values stored in a given vector is referred to
 * as its {@linkplain #length() vector length} or {@code VLENGTH}.
 *
 * It is useful to consider vector lanes as ordered
 * <em>sequentially</em> from first to last, with the first lane
 * numbered {@code 0}, the next lane numbered {@code 1}, and so on to
 * the last lane numbered {@code VLENGTH-1}.  This is a temporal
 * order, where lower-numbered lanes are considered earlier than
 * higher-numbered (later) lanes.  This API uses these terms
 * in preference to spatial terms such as "left", "right", "high",
 * and "low".
 *
 * <p> Temporal terminology works well for vectors because they
 * (usually) represent small fixed-sized segments in a long sequence
 * of workload elements, where the workload is conceptually traversed
 * in time order from beginning to end.  (This is a mental model: it
 * does not exclude multicore divide-and-conquer techniques.)  Thus,
 * when a scalar loop is transformed into a vector loop, adjacent
 * scalar items (one earlier, one later) in the workload end up as
 * adjacent lanes in a single vector (again, one earlier, one later).
 * At a vector boundary, the last lane item in the earlier vector is
 * adjacent to (and just before) the first lane item in the
 * immediately following vector.
 *
 * <p> Vectors are also sometimes thought of in spatial terms, where
 * the first lane is placed at an edge of some virtual paper, and
 * subsequent lanes are presented in order next to it.  When using
 * spatial terms, all directions are equally plausible: Some vector
 * notations present lanes from left to right, and others from right
 * to left; still others present from top to bottom or vice versa.
 * Using the language of time (before, after, first, last) instead of
 * space (left, right, high, low) is often more likely to avoid
 * misunderstandings.
 *
 * <p> As second reason to prefer temporal to spatial language about
 * vector lanes is the fact that the terms "left", "right", "high" and
 * "low" are widely used to describe the relations between bits in
 * scalar values.  The leftmost or highest bit in a given type is
 * likely to be a sign bit, while the rightmost or lowest bit is
 * likely to be the arithmetically least significant, and so on.
 * Applying these terms to vector lanes risks confusion, however,
 * because it is relatively rare to find algorithms where, given two
 * adjacent vector lanes, one lane is somehow more arithmetically
 * significant than its neighbor, and even in those cases, there is no
 * general way to know which neighbor is the the more significant.
 *
 * <p> Putting the terms together, we view the information structure
 * of a vector as a temporal sequence of lanes ("first", "next",
 * "earlier", "later", "last", etc.)  of bit-strings which are
 * internally ordered spatially (either "low" to "high" or "right" to
 * "left").  The primitive values in the lanes are decoded from these
 * bit-strings, in the usual way.  Most vector operations, like most
 * Java scalar operators, treat primitive values as atomic values, but
 * some operations reveal the internal bit-string structure.
 *
 * <p> When a vector is loaded from or stored into memory, the order
 * of vector lanes is <em>always consistent </em> with the inherent
 * ordering of the memory container.  This is true whether or not
 * individual lane elements are subject to "byte swapping" due to
 * details of byte order.  Thus, while the scalar lane elements of
 * vector might be "byte swapped", the lanes themselves are never
 * reordered, except by an explicit method call that performs
 * cross-lane reordering.
 *
 * <p> When vector lane values are stored to Java variables of the
 * same type, byte swapping is performed if and only if the
 * implementation of the vector hardware requires such swapping.  It
 * is therefore unconditional and invisible.
 *
 * <p> As a useful fiction, this API presents a consistent illusion
 * that vector lane bytes are composed into larger lane scalars in
 * <em>little endian order</em>.  This means that storing a vector
 * into a Java byte array will reveal the successive bytes of the
 * vector lane values in little-endian order on all platforms,
 * regardless of native memory order, and also regardless of byte
 * order (if any) within vector unit registers.
 *
 * <p> This hypothetical little-endian ordering also appears when a
 * {@linkplain #reinterpretShape(VectorSpecies,int) reinterpretation cast} is
 * applied in such a way that lane boundaries are discarded and
 * redrawn differently, while maintaining vector bits unchanged.  In
 * such an operation, two adjacent lanes will contribute bytes to a
 * single new lane (or vice versa), and the sequential order of the
 * two lanes will determine the arithmetic order of the bytes in the
 * single lane.  In this case, the little-endian convention provides
 * portable results, so that on all platforms earlier lanes tend to
 * contribute lower (rightward) bits, and later lanes tend to
 * contribute higher (leftward) bits.  The {@linkplain #reinterpretAsBytes()
 * reinterpretation casts} between {@link ByteVector}s and the
 * other non-byte vectors use this convention to clarify their
 * portable semantics.
 *
 * <p> The little-endian fiction for relating lane order to per-lane
 * byte order is slightly preferable to an equivalent big-endian
 * fiction, because some related formulas are much simpler,
 * specifically those which renumber bytes after lane structure
 * changes.  The earliest byte is invariantly earliest across all lane
 * structure changes, but only if little-endian convention are used.
 * The root cause of this is that bytes in scalars are numbered from
 * the least significant (rightmost) to the most significant
 * (leftmost), and almost never vice-versa.  If we habitually numbered
 * sign bits as zero (as on some computers) then this API would reach
 * for big-endian fictions to create unified addressing of vector
 * bytes.
 *
 * <h2><a id="memory"></a>Memory operations</h2>
 *
 * As was already mentioned, vectors can be loaded from memory and
 * stored back.  An optional mask can control which individual memory
 * locations are read from or written to.  The shape of a vector
 * determines how much memory it will occupy.
 *
 * An implementation typically has the property, in the absence of
 * masking, that lanes are stored as a dense sequence of back-to-back
 * values in memory, the same as a dense (gap-free) series of single
 * scalar values in an array of the scalar type.
 *
 * In such cases memory order corresponds exactly to lane order.  The
 * first vector lane value occupies the first position in memory, and so on,
 * up to the length of the vector. Further, the memory order of stored
 * vector lanes corresponds to increasing index values in a Java array or
 * in a {@link java.nio.ByteBuffer}.
 *
 * <p> Byte order for lane storage is chosen such that the stored
 * vector values can be read or written as single primitive values,
 * within the array or buffer that holds the vector, producing the
 * same values as the lane-wise values within the vector.
 * This fact is independent of the convenient fiction that lane values
 * inside of vectors are stored in little-endian order.
 *
 * <p> For example,
 * {@link FloatVector#fromArray(VectorSpecies, float[], int)
 *        FloatVector.fromArray(fsp,fa,i)}
 * creates and returns a float vector of some particular species {@code fsp},
 * with elements loaded from some float array {@code fa}.
 * The first lane is loaded from {@code fa[i]} and the last lane
 * is initialized loaded from {@code fa[i+VL-1]}, where {@code VL}
 * is the length of the vector as derived from the species {@code fsp}.
 * Then, {@link FloatVector#add(Vector) fv=fv.add(fv2)}
 * will produce another float vector of that species {@code fsp},
 * given a vector {@code fv2} of the same species {@code fsp}.
 * Next, {@link FloatVector#compare(VectorOperators.Comparison,float)
 * mnz=fv.compare(NE, 0.0f)} tests whether the result is zero,
 * yielding a mask {@code mnz}.  The non-zero lanes (and only those
 * lanes) can then be stored back into the original array elements
 * using the statement
 * {@link FloatVector#intoArray(float[],int,VectorMask) fv.intoArray(fa,i,mnz)}.
 *
 * <h2><a id="expansion"></a>Expansions, contractions, and partial results</h2>
 *
 * Since vectors are fixed in size, occasions often arise where the
 * logical result of an operation is not the same as the physical size
 * of the proposed output vector.  To encourage user code that is as
 * portable and predictable as possible, this API has a systematic
 * approach to the design of such <em>resizing</em> vector operations.
 *
 * <p> As a basic principle, lane-wise operations are
 * <em>length-invariant</em>, unless clearly marked otherwise.
 * Length-invariance simply means that
 * if {@code VLENGTH} lanes go into an operation, the same number
 * of lanes come out, with nothing discarded and no extra padding.
 *
 * <p> As a second principle, sometimes in tension with the first,
 * lane-wise operations are also <em>shape-invariant</em>, unless
 * clearly marked otherwise.
 *
 * Shape-invariance means that {@code VSHAPE} is constant for typical
 * computations.  Keeping the same shape throughout a computation
 * helps ensure that scarce vector resources are efficiently used.
 * (On some hardware platforms shape changes could cause unwanted
 * effects like extra data movement instructions, round trips through
 * memory, or pipeline bubbles.)
 *
 * <p> Tension between these principles arises when an operation
 * produces a <em>logical result</em> that is too large for the
 * required output {@code VSHAPE}.  In other cases, when a logical
 * result is smaller than the capacity of the output {@code VSHAPE},
 * the positioning of the logical result is open to question, since
 * the physical output vector must contain a mix of logical result and
 * padding.
 *
 * <p> In the first case, of a too-large logical result being crammed
 * into a too-small output {@code VSHAPE}, we say that data has
 * <em>expanded</em>.  In other words, an <em>expansion operation</em>
 * has caused the output shape to overflow.  Symmetrically, in the
 * second case of a small logical result fitting into a roomy output
 * {@code VSHAPE}, the data has <em>contracted</em>, and the
 * <em>contraction operation</em> has required the output shape to pad
 * itself with extra zero lanes.
 *
 * <p> In both cases we can speak of a parameter {@code M} which
 * measures the <em>expansion ratio</em> or <em>contraction ratio</em>
 * between the logical result size (in bits) and the bit-size of the
 * actual output shape.  When vector shapes are changed, and lane
 * sizes are not, {@code M} is just the integral ratio of the output
 * shape to the logical result.  (With the possible exception of
 * the {@linkplain VectorShape#S_Max_BIT maximum shape}, all vector
 * sizes are powers of two, and so the ratio {@code M} is always
 * an integer.  In the hypothetical case of a non-integral ratio,
 * the value {@code M} would be rounded up to the next integer,
 * and then the same general considerations would apply.)
 *
 * <p> If the logical result is larger than the physical output shape,
 * such a shape change must inevitably drop result lanes (all but
 * {@code 1/M} of the logical result).  If the logical size is smaller
 * than the output, the shape change must introduce zero-filled lanes
 * of padding (all but {@code 1/M} of the physical output).  The first
 * case, with dropped lanes, is an expansion, while the second, with
 * padding lanes added, is a contraction.
 *
 * <p> Similarly, consider a lane-wise conversion operation which
 * leaves the shape invariant but changes the lane size by a ratio of
 * {@code M}.  If the logical result is larger than the output (or
 * input), this conversion must reduce the {@code VLENGTH} lanes of the
 * output by {@code M}, dropping all but {@code 1/M} of the logical
 * result lanes.  As before, the dropping of lanes is the hallmark of
 * an expansion.  A lane-wise operation which contracts lane size by a
 * ratio of {@code M} must increase the {@code VLENGTH} by the same
 * factor {@code M}, filling the extra lanes with a zero padding
 * value; because padding must be added this is a contraction.
 *
 * <p> It is also possible (though somewhat confusing) to change both
 * lane size and container size in one operation which performs both
 * lane conversion <em>and</em> reshaping.  If this is done, the same
 * rules apply, but the logical result size is the product of the
 * input size times any expansion or contraction ratio from the lane
 * change size.
 *
 * <p> For completeness, we can also speak of <em>in-place
 * operations</em> for the frequent case when resizing does not occur.
 * With an in-place operation, the data is simply copied from logical
 * output to its physical container with no truncation or padding.
 * The ratio parameter {@code M} in this case is unity.
 *
 * <p> Note that the classification of contraction vs. expansion
 * depends on the relative sizes of the logical result and the
 * physical output container.  The size of the input container may be
 * larger or smaller than either of the other two values, without
 * changing the classification.  For example, a conversion from a
 * 128-bit shape to a 256-bit shape will be a contraction in many
 * cases, but it would be an expansion if it were combined with a
 * conversion from {@code byte} to {@code long}, since in that case
 * the logical result would be 1024 bits in size.  This example also
 * illustrates that a logical result does not need to correspond to
 * any particular platform-supported vector shape.
 *
 * <p> Although lane-wise masked operations can be viewed as producing
 * partial operations, they are not classified (in this API) as
 * expansions or contractions.  A masked load from an array surely
 * produces a partial vector, but there is no meaningful "logical
 * output vector" that this partial result was contracted from.
 *
 * <p> Some care is required with these terms, because it is the
 * <em>data</em>, not the <em>container size</em>, that is expanding
 * or contracting, relative to the size of its output container.
 * Thus, resizing a 128-bit input into 512-bit vector has the effect
 * of a <em>contraction</em>.  Though the 128 bits of payload hasn't
 * changed in size, we can say it "looks smaller" in its new 512-bit
 * home, and this will capture the practical details of the situation.
 *
 * <p> If a vector method might expand its data, it accepts an extra
 * {@code int} parameter called {@code part}, or the "part number".
 * The part number must be in the range {@code [0..M-1]}, where
 * {@code M} is the expansion ratio.  The part number selects one
 * of {@code M} contiguous disjoint equally-sized blocks of lanes
 * from the logical result and fills the physical output vector
 * with this block of lanes.
 *
 * <p> Specifically, the lanes selected from the logical result of an
 * expansion are numbered in the range {@code [R..R+L-1]}, where
 * {@code L} is the {@code VLENGTH} of the physical output vector, and
 * the origin of the block, {@code R}, is {@code part*L}.
 *
 * <p> A similar convention applies to any vector method that might
 * contract its data.  Such a method also accepts an extra part number
 * parameter (again called {@code part}) which steers the contracted
 * data lanes one of {@code M} contiguous disjoint equally-sized
 * blocks of lanes in the physical output vector.  The remaining lanes
 * are filled with zero, or as specified by the method.
 *
 * <p> Specifically, the data is steered into the lanes numbered in the
 * range {@code [R..R+L-1]}, where {@code L} is the {@code VLENGTH} of
 * the logical result vector, and the origin of the block, {@code R},
 * is again a multiple of {@code L} selected by the part number,
 * specifically {@code |part|*L}.
 *
 * <p> In the case of a contraction, the part number must be in the
 * non-positive range {@code [-M+1..0]}.  This convention is adopted
 * because some methods can perform both expansions and contractions,
 * in a data-dependent manner, and the extra sign on the part number
 * serves as an error check.  If vector method takes a part number and
 * is invoked to perform an in-place operation (neither contracting
 * nor expanding), the {@code part} parameter must be exactly zero.
 * Part numbers outside the allowed ranges will elicit an indexing
 * exception.  Note that in all cases a zero part number is valid, and
 * corresponds to an operation which preserves as many lanes as
 * possible from the beginning of the logical result, and places them
 * into the beginning of the physical output container.  This is
 * often a desirable default, so a part number of zero is safe
 * in all cases and useful in most cases.
 *
 * <p> The various resizing operations of this API contract or expand
 * their data as follows:
 * <ul>
 *
 * <li>
 * {@link Vector#convert(VectorOperators.Conversion,int) Vector.convert()}
 * will expand (respectively, contract) its operand by ratio
 * {@code M} if the
 * {@linkplain #elementSize() element size} of its output is
 * larger (respectively, smaller) by a factor of {@code M}.
 * If the element sizes of input and output are the same,
 * then {@code convert()} is an in-place operation.
 *
 * <li>
 * {@link Vector#convertShape(VectorOperators.Conversion,VectorSpecies,int) Vector.convertShape()}
 * will expand (respectively, contract) its operand by ratio
 * {@code M} if the bit-size of its logical result is
 * larger (respectively, smaller) than the bit-size of its
 * output shape.
 * The size of the logical result is defined as the
 * {@linkplain #elementSize() element size} of the output,
 * times the {@code VLENGTH} of its input.
 *
 * Depending on the ratio of the changed lane sizes, the logical size
 * may be (in various cases) either larger or smaller than the input
 * vector, independently of whether the operation is an expansion
 * or contraction.
 *
 * <li>
 * Since {@link Vector#castShape(VectorSpecies,int) Vector.castShape()}
 * is a convenience method for {@code convertShape()}, its classification
 * as an expansion or contraction is the same as for {@code convertShape()}.
 *
 * <li>
 * {@link Vector#reinterpretShape(VectorSpecies,int) Vector.reinterpretShape()}
 * is an expansion (respectively, contraction) by ratio {@code M} if the
 * {@linkplain #bitSize() vector bit-size} of its input is
 * crammed into a smaller (respectively, dropped into a larger)
 * output container by a factor of {@code M}.
 * Otherwise it is an in-place operation.
 *
 * Since this method is a reinterpretation cast that can erase and
 * redraw lane boundaries as well as modify shape, the input vector's
 * lane size and lane count are irrelevant to its classification as
 * expanding or contracting.
 *
 * <li>
 * The {@link #unslice(int,Vector,int) unslice()} methods expand
 * by a ratio of {@code M=2}, because the single input slice is
 * positioned and inserted somewhere within two consecutive background
 * vectors.  The part number selects the first or second background
 * vector, as updated by the inserted slice.
 * Note that the corresponding
 * {@link #slice(int,Vector) slice()} methods, although inverse
 * to the {@code unslice()} methods, do not contract their data
 * and thus require no part number.  This is because
 * {@code slice()} delivers a slice of exactly {@code VLENGTH}
 * lanes extracted from two input vectors.
 * </ul>
 *
 * The method {@link VectorSpecies#partLimit(VectorSpecies,boolean)
 * partLimit()} on {@link VectorSpecies} can be used, before any
 * expanding or contracting operation is performed, to query the
 * limiting value on a part parameter for a proposed expansion
 * or contraction.  The value returned from {@code partLimit()} is
 * positive for expansions, negative for contractions, and zero for
 * in-place operations.  Its absolute value is the parameter {@code
 * M}, and so it serves as an exclusive limit on valid part number
 * arguments for the relevant methods.  Thus, for expansions, the
 * {@code partLimit()} value {@code M} is the exclusive upper limit
 * for part numbers, while for contractions the {@code partLimit()}
 * value {@code -M} is the exclusive <em>lower</em> limit.
 *
 * <h2><a id="cross-lane"></a>Moving data across lane boundaries</h2>
 * The cross-lane methods which do not redraw lanes or change species
 * are more regularly structured and easier to reason about.
 * These operations are:
 * <ul>
 *
 * <li>The {@link #slice(int,Vector) slice()} family of methods,
 * which extract contiguous slice of {@code VLENGTH} fields from
 * a given origin point within a concatenated pair of vectors.
 *
 * <li>The {@link #unslice(int,Vector,int) unslice()} family of
 * methods, which insert a contiguous slice of {@code VLENGTH} fields
 * into a concatenated pair of vectors at a given origin point.
 *
 * <li>The {@link #rearrange(VectorShuffle) rearrange()} family of
 * methods, which select an arbitrary set of {@code VLENGTH} lanes
 * from one or two input vectors, and assemble them in an arbitrary
 * order.  The selection and order of lanes is controlled by a
 * {@code VectorShuffle} object, which acts as an routing table
 * mapping source lanes to destination lanes.  A {@code VectorShuffle}
 * can encode a mathematical permutation as well as many other
 * patterns of data movement.
 *
 * </ul>
 * <p> Some vector operations are not lane-wise, but rather move data
 * across lane boundaries.  Such operations are typically rare in SIMD
 * code, though they are sometimes necessary for specific algorithms
 * that manipulate data formats at a low level, and/or require SIMD
 * data to move in complex local patterns.  (Local movement in a small
 * window of a large array of data is relatively unusual, although
 * some highly patterned algorithms call for it.)  In this API such
 * methods are always clearly recognizable, so that simpler lane-wise
 * reasoning can be confidently applied to the rest of the code.
 *
 * <p> In some cases, vector lane boundaries are discarded and
 * "redrawn from scratch", so that data in a given input lane might
 * appear (in several parts) distributed through several output lanes,
 * or (conversely) data from several input lanes might be consolidated
 * into a single output lane.  The fundamental method which can redraw
 * lanes boundaries is
 * {@link #reinterpretShape(VectorSpecies,int) reinterpretShape()}.
 * Built on top of this method, certain convenience methods such
 * as {@link #reinterpretAsBytes() reinterpretAsBytes()} or
 * {@link #reinterpretAsInts() reinterpretAsInts()} will
 * (potentially) redraw lane boundaries, while retaining the
 * same overall vector shape.
 *
 * <p> Operations which produce or consume a scalar result can be
 * viewed as very simple cross-lane operations.  Methods in the
 * {@link #reduceLanesToLong(VectorOperators.Associative)
 * reduceLanes()} family fold together all lanes (or mask-selected
 * lanes) of a method and return a single result.  As an inverse, the
 * {@link #broadcast(long) broadcast} family of methods can be thought
 * of as crossing lanes in the other direction, from a scalar to all
 * lanes of the output vector.  Single-lane access methods such as
 * {@code lane(I)} or {@code withLane(I,E)} might also be regarded as
 * very simple cross-lane operations.
 *
 * <p> Likewise, a method which moves a non-byte vector to or from a
 * byte array could be viewed as a cross-lane operation, because the
 * vector lanes must be distributed into separate bytes, or (in the
 * other direction) consolidated from array bytes.
 *
 * @implNote
 *
 * <h2>Hardware platform dependencies and limitations</h2>
 *
 * The Vector API is to accelerate computations in style of Single
 * Instruction Multiple Data (SIMD), using available hardware
 * resources such as vector hardware registers and vector hardware
 * instructions.  The API is designed to make effective use of
 * multiple SIMD hardware platforms.
 *
 * <p> This API will also work correctly even on Java platforms which
 * do not include specialized hardware support for SIMD computations.
 * The Vector API is not likely to provide any special performance
 * benefit on such platforms.
 *
 * <p> Currently the implementation is optimized to work best on:
 *
 * <ul>
 *
 * <li> Intel x64 platforms supporting at least AVX2 up to AVX-512.
 * Masking using mask registers and mask accepting hardware
 * instructions on AVX-512 are not currently supported.
 *
 * <li> ARM AArch64 platforms supporting NEON.  Although the API has
 * been designed to ensure ARM SVE instructions can be supported
 * (vector sizes between 128 to 2048 bits) there is currently no
 * implementation of such instructions and the general masking
 * capability.
 *
 * </ul>
 * The implementation currently supports masked lane-wise operations
 * in a cross-platform manner by composing the unmasked lane-wise
 * operation with {@link #blend(Vector, VectorMask) blend} as in
 * the expression {@code a.blend(a.lanewise(op, b), m)}, where
 * {@code a} and {@code b} are vectors, {@code op} is the vector
 * operation, and {@code m} is the mask.
 *
 * <p> The implementation does not currently support optimal
 * vectorized instructions for floating point transcendental
 * functions (such as operators {@link VectorOperators#SIN SIN}
 * and {@link VectorOperators#LOG LOG}).
 *
 * <h2>No boxing of primitives</h2>
 *
 * Although a vector type like {@code Vector<Integer>} may seem to
 * work with boxed {@code Integer} values, the overheads associated
 * with boxing are avoided by having each vector subtype work
 * internally on lane values of the actual {@code ETYPE}, such as
 * {@code int}.
 *
 * <h2>Value-based classes and identity operations</h2>
 *
 * {@code Vector}, along with all of its subtypes and many of its
 * helper types like {@code VectorMask} and {@code VectorShuffle}, is a
 * <a href="{@docRoot}/java.base/java/lang/doc-files/ValueBased.html">value-based</a>
 * class.
 *
 * <p> Once created, a vector is never mutated, not even if only
 * {@linkplain IntVector#withLane(int,int) a single lane is changed}.
 * A new vector is always created to hold a new configuration
 * of lane values.  The unavailability of mutative methods is a
 * necessary consequence of suppressing the object identity of
 * all vectors, as value-based classes.
 *
 * <p> With {@code Vector},
 *
 * <!-- The following paragraph is shared verbatim
 *   -- between Vector.java and package-info.java -->
 * identity-sensitive operations such as {@code ==} may yield
 * unpredictable results, or reduced performance.  Oddly enough,
 * {@link Vector#equals(Object) v.equals(w)} is likely to be faster
 * than {@code v==w}, since {@code equals} is <em>not</em> an identity
 * sensitive method.
 *
 * Also, these objects can be stored in locals and parameters and as
 * {@code static final} constants, but storing them in other Java
 * fields or in array elements, while semantically valid, may incur
 * performance penalties.
 * <!-- The preceding paragraph is shared verbatim
 *   -- between Vector.java and package-info.java -->
 *
 * @param <E> the boxed version of {@code ETYPE},
 *           the element type of a vector
 *
 */
@SuppressWarnings("exports")
public abstract class Vector<E> extends jdk.internal.vm.vector.VectorSupport.Vector<E> {

    // This type is sealed within its package.
    // Users cannot roll their own vector types.
    Vector(Object bits) {
        super(bits);
    }

    /**
     * Returns the species of this vector.
     *
     * @return the species of this vector
     */
    public abstract VectorSpecies<E> species();

    /**
     * Returns the primitive <a href="Vector.html#ETYPE">element type</a>
     * ({@code ETYPE}) of this vector.
     *
     * @implSpec
     * This is the same value as {@code this.species().elementType()}.
     *
     * @return the primitive element type of this vector
     */
    public abstract Class<E> elementType();

    /**
     * Returns the size of each lane, in bits, of this vector.
     *
     * @implSpec
     * This is the same value as {@code this.species().elementSize()}.
     *
     * @return the lane size, in bits, of this vector
     */
    public abstract int elementSize();

    /**
     * Returns the shape of this vector.
     *
     * @implSpec
     * This is the same value as {@code this.species().vectorShape()}.
     *
     * @return the shape of this vector
     */
    public abstract VectorShape shape();

    /**
     * Returns the lane count, or <a href="Vector.html#VLENGTH">vector length</a>
     * ({@code VLENGTH}).
     *
     * @return the lane count
     */
    public abstract int length();

    /**
     * Returns the total size, in bits, of this vector.
     *
     * @implSpec
     * This is the same value as {@code this.shape().vectorBitSize()}.
     *
     * @return the total size, in bits, of this vector
     */
    public abstract int bitSize();

    /**
     * Returns the total size, in bytes, of this vector.
     *
     * @implSpec
     * This is the same value as {@code this.bitSize()/Byte.SIZE}.
     *
     * @return the total size, in bytes, of this vector
     */
    public abstract int byteSize();

    /// Arithmetic

    /**
     * Operates on the lane values of this vector.
     *
     * This is a <a href="Vector.html#lane-wise">lane-wise</a>
     * unary operation which applies
     * the selected operation to each lane.
     *
     * @apiNote
     * Subtypes improve on this method by sharpening
     * the method return type.
     *
     * @param op the operation used to process lane values
     * @return the result of applying the operation lane-wise
     *         to the input vector
     * @throws UnsupportedOperationException if this vector does
     *         not support the requested operation
     * @see VectorOperators#NEG
     * @see VectorOperators#NOT
     * @see VectorOperators#SIN
     * @see #lanewise(VectorOperators.Unary,VectorMask)
     * @see #lanewise(VectorOperators.Binary,Vector)
     * @see #lanewise(VectorOperators.Ternary,Vector,Vector)
     */
    public abstract Vector<E> lanewise(VectorOperators.Unary op);

    /**
     * Operates on the lane values of this vector,
     * with selection of lane elements controlled by a mask.
     *
     * This is a lane-wise unary operation which applies
     * the selected operation to each lane.
     *
     * @apiNote
     * Subtypes improve on this method by sharpening
     * the method return type.
     *
     * @param op the operation used to process lane values
     * @param m the mask controlling lane selection
     * @return the result of applying the operation lane-wise
     *         to the input vector
     * @throws UnsupportedOperationException if this vector does
     *         not support the requested operation
     * @see #lanewise(VectorOperators.Unary)
     */
    public abstract Vector<E> lanewise(VectorOperators.Unary op,
                                       VectorMask<E> m);

    /**
     * Combines the corresponding lane values of this vector
     * with those of a second input vector.
     *
     * This is a <a href="Vector.html#lane-wise">lane-wise</a>
     * binary operation which applies
     * the selected operation to each lane.
     *
     * @apiNote
     * Subtypes improve on this method by sharpening
     * the method return type.
     *
     * @param op the operation used to combine lane values
     * @param v the input vector
     * @return the result of applying the operation lane-wise
     *         to the two input vectors
     * @throws UnsupportedOperationException if this vector does
     *         not support the requested operation
     * @see VectorOperators#ADD
     * @see VectorOperators#XOR
     * @see VectorOperators#ATAN2
     * @see #lanewise(VectorOperators.Binary,Vector,VectorMask)
     * @see #lanewise(VectorOperators.Unary)
     * @see #lanewise(VectorOperators.Ternary,Vector, Vector)
     */
    public abstract Vector<E> lanewise(VectorOperators.Binary op,
                                       Vector<E> v);

    /**
     * Combines the corresponding lane values of this vector
     * with those of a second input vector,
     * with selection of lane elements controlled by a mask.
     *
     * This is a lane-wise binary operation which applies
     * the selected operation to each lane.
     *
     * @apiNote
     * Subtypes improve on this method by sharpening
     * the method return type.
     *
     * @param op the operation used to combine lane values
     * @param v the second input vector
     * @param m the mask controlling lane selection
     * @return the result of applying the operation lane-wise
     *         to the two input vectors
     * @throws UnsupportedOperationException if this vector does
     *         not support the requested operation
     * @see #lanewise(VectorOperators.Binary,Vector)
     */
    public abstract Vector<E> lanewise(VectorOperators.Binary op,
                                       Vector<E> v, VectorMask<E> m);

    /**
     * Combines the lane values of this vector
     * with the value of a broadcast scalar.
     *
     * This is a lane-wise binary operation which applies
     * the selected operation to each lane.
     * The return value will be equal to this expression:
     * {@code this.lanewise(op, this.broadcast(e))}.
     *
     * @apiNote
     * The {@code long} value {@code e} must be accurately
     * representable by the {@code ETYPE} of this vector's species,
     * so that {@code e==(long)(ETYPE)e}.  This rule is enforced
     * by the implicit call to {@code broadcast()}.
     * <p>
     * Subtypes improve on this method by sharpening
     * the method return type and
     * the type of the scalar parameter {@code e}.
     *
     * @param op the operation used to combine lane values
     * @param e the input scalar
     * @return the result of applying the operation lane-wise
     *         to the input vector and the scalar
     * @throws UnsupportedOperationException if this vector does
     *         not support the requested operation
     * @throws IllegalArgumentException
     *         if the given {@code long} value cannot
     *         be represented by the right operand type
     *         of the vector operation
     * @see #broadcast(long)
     * @see #lanewise(VectorOperators.Binary,long,VectorMask)
     */
    public abstract Vector<E> lanewise(VectorOperators.Binary op,
                                       long e);

    /**
     * Combines the corresponding lane values of this vector
     * with those of a second input vector,
     * with selection of lane elements controlled by a mask.
     *
     * This is a lane-wise binary operation which applies
     * the selected operation to each lane.
     * The second operand is a broadcast integral value.
     * The return value will be equal to this expression:
     * {@code this.lanewise(op, this.broadcast(e), m)}.
     *
     * @apiNote
     * The {@code long} value {@code e} must be accurately
     * representable by the {@code ETYPE} of this vector's species,
     * so that {@code e==(long)(ETYPE)e}.  This rule is enforced
     * by the implicit call to {@code broadcast()}.
     * <p>
     * Subtypes improve on this method by sharpening
     * the method return type and
     * the type of the scalar parameter {@code e}.
     *
     * @param op the operation used to combine lane values
     * @param e the input scalar
     * @param m the mask controlling lane selection
     * @return the result of applying the operation lane-wise
     *         to the input vector and the scalar
     * @throws UnsupportedOperationException if this vector does
     *         not support the requested operation
     * @throws IllegalArgumentException
     *         if the given {@code long} value cannot
     *         be represented by the right operand type
     *         of the vector operation
     * @see #broadcast(long)
     * @see #lanewise(VectorOperators.Binary,Vector,VectorMask)
     */
    public abstract Vector<E> lanewise(VectorOperators.Binary op,
                                       long e, VectorMask<E> m);

    /**
     * Combines the corresponding lane values of this vector
     * with the lanes of a second and a third input vector.
     *
     * This is a <a href="Vector.html#lane-wise">lane-wise</a>
     * ternary operation which applies
     * the selected operation to each lane.
     *
     * @apiNote
     * Subtypes improve on this method by sharpening
     * the method return type.
     *
     * @param op the operation used to combine lane values
     * @param v1 the second input vector
     * @param v2 the third input vector
     * @return the result of applying the operation lane-wise
     *         to the three input vectors
     * @throws UnsupportedOperationException if this vector does
     *         not support the requested operation
     * @see VectorOperators#BITWISE_BLEND
     * @see VectorOperators#FMA
     * @see #lanewise(VectorOperators.Unary)
     * @see #lanewise(VectorOperators.Binary,Vector)
     * @see #lanewise(VectorOperators.Ternary,Vector,Vector,VectorMask)
     */
    public abstract Vector<E> lanewise(VectorOperators.Ternary op,
                                       Vector<E> v1,
                                       Vector<E> v2);

    /**
     * Combines the corresponding lane values of this vector
     * with the lanes of a second and a third input vector,
     * with selection of lane elements controlled by a mask.
     *
     * This is a lane-wise ternary operation which applies
     * the selected operation to each lane.
     *
     * @apiNote
     * Subtypes improve on this method by sharpening
     * the method return type.
     *
     * @param op the operation used to combine lane values
     * @param v1 the second input vector
     * @param v2 the third input vector
     * @param m the mask controlling lane selection
     * @return the result of applying the operation lane-wise
     *         to the three input vectors
     * @throws UnsupportedOperationException if this vector does
     *         not support the requested operation
     * @see #lanewise(VectorOperators.Ternary,Vector,Vector)
     */
    public abstract Vector<E> lanewise(VectorOperators.Ternary op,
                                       Vector<E> v1, Vector<E> v2,
                                       VectorMask<E> m);

    // Note:  lanewise(Binary) has two rudimentary broadcast
    // operations from an approximate scalar type (long).
    // We do both with that, here, for lanewise(Ternary).
    // The vector subtypes supply a full suite of
    // broadcasting and masked lanewise operations
    // for their specific ETYPEs:
    //   lanewise(Unary, [mask])
    //   lanewise(Binary, [e | v], [mask])
    //   lanewise(Ternary, [e1 | v1], [e2 | v2], [mask])

    /// Full-service binary ops: ADD, SUB, MUL, DIV

    // Full-service functions support all four variations
    // of vector vs. broadcast scalar, and mask vs. not.
    // The lanewise generic operator is (by this definition)
    // also a full-service function.

    // Other named functions handle just the one named
    // variation.  Most lanewise operations are *not* named,
    // and are reached only by lanewise.

    /**
     * Adds this vector to a second input vector.
     *
     * This is a lane-wise binary operation which applies
     * the primitive addition operation ({@code +})
     * to each pair of corresponding lane values.
     *
     * This method is also equivalent to the expression
     * {@link #lanewise(VectorOperators.Binary,Vector)
     *    lanewise}{@code (}{@link VectorOperators#ADD
     *    ADD}{@code , v)}.
     *
     * <p>
     * As a full-service named operation, this method
     * comes in masked and unmasked overloadings, and
     * (in subclasses) also comes in scalar-broadcast
     * overloadings (both masked and unmasked).
     *
     * @param v a second input vector
     * @return the result of adding this vector to the second input vector
     * @see #add(Vector,VectorMask)
     * @see IntVector#add(int)
     * @see VectorOperators#ADD
     * @see #lanewise(VectorOperators.Binary,Vector)
     * @see IntVector#lanewise(VectorOperators.Binary,int)
     */
    public abstract Vector<E> add(Vector<E> v);

    /**
     * Adds this vector to a second input vector, selecting lanes
     * under the control of a mask.
     *
     * This is a masked lane-wise binary operation which applies
     * the primitive addition operation ({@code +})
     * to each pair of corresponding lane values.
     *
     * For any lane unset in the mask, the primitive operation is
     * suppressed and this vector retains the original value stored in
     * that lane.
     *
     * This method is also equivalent to the expression
     * {@link #lanewise(VectorOperators.Binary,Vector,VectorMask)
     *    lanewise}{@code (}{@link VectorOperators#ADD
     *    ADD}{@code , v, m)}.
     *
     * <p>
     * As a full-service named operation, this method
     * comes in masked and unmasked overloadings, and
     * (in subclasses) also comes in scalar-broadcast
     * overloadings (both masked and unmasked).
     *
     * @param v the second input vector
     * @param m the mask controlling lane selection
     * @return the result of adding this vector to the given vector
     * @see #add(Vector)
     * @see IntVector#add(int,VectorMask)
     * @see VectorOperators#ADD
     * @see #lanewise(VectorOperators.Binary,Vector,VectorMask)
     * @see IntVector#lanewise(VectorOperators.Binary,int,VectorMask)
     */
    public abstract Vector<E> add(Vector<E> v, VectorMask<E> m);

    /**
     * Subtracts a second input vector from this vector.
     *
     * This is a lane-wise binary operation which applies
     * the primitive subtraction operation ({@code -})
     * to each pair of corresponding lane values.
     *
     * This method is also equivalent to the expression
     * {@link #lanewise(VectorOperators.Binary,Vector)
     *    lanewise}{@code (}{@link VectorOperators#SUB
     *    SUB}{@code , v)}.
     *
     * <p>
     * As a full-service named operation, this method
     * comes in masked and unmasked overloadings, and
     * (in subclasses) also comes in scalar-broadcast
     * overloadings (both masked and unmasked).
     *
     * @param v a second input vector
     * @return the result of subtracting the second input vector from this vector
     * @see #sub(Vector,VectorMask)
     * @see IntVector#sub(int)
     * @see VectorOperators#SUB
     * @see #lanewise(VectorOperators.Binary,Vector)
     * @see IntVector#lanewise(VectorOperators.Binary,int)
     */
    public abstract Vector<E> sub(Vector<E> v);

    /**
     * Subtracts a second input vector from this vector
     * under the control of a mask.
     *
     * This is a masked lane-wise binary operation which applies
     * the primitive subtraction operation ({@code -})
     * to each pair of corresponding lane values.
     *
     * For any lane unset in the mask, the primitive operation is
     * suppressed and this vector retains the original value stored in
     * that lane.
     *
     * This method is also equivalent to the expression
     * {@link #lanewise(VectorOperators.Binary,Vector,VectorMask)
     *    lanewise}{@code (}{@link VectorOperators#SUB
     *    SUB}{@code , v, m)}.
     *
     * <p>
     * As a full-service named operation, this method
     * comes in masked and unmasked overloadings, and
     * (in subclasses) also comes in scalar-broadcast
     * overloadings (both masked and unmasked).
     *
     * @param v the second input vector
     * @param m the mask controlling lane selection
     * @return the result of subtracting the second input vector from this vector
     * @see #sub(Vector)
     * @see IntVector#sub(int,VectorMask)
     * @see VectorOperators#SUB
     * @see #lanewise(VectorOperators.Binary,Vector,VectorMask)
     * @see IntVector#lanewise(VectorOperators.Binary,int,VectorMask)
     */
    public abstract Vector<E> sub(Vector<E> v, VectorMask<E> m);

    /**
     * Multiplies this vector by a second input vector.
     *
     * This is a lane-wise binary operation which applies
     * the primitive multiplication operation ({@code *})
     * to each pair of corresponding lane values.
     *
     * This method is also equivalent to the expression
     * {@link #lanewise(VectorOperators.Binary,Vector)
     *    lanewise}{@code (}{@link VectorOperators#MUL
     *    MUL}{@code , v)}.
     *
     * <p>
     * As a full-service named operation, this method
     * comes in masked and unmasked overloadings, and
     * (in subclasses) also comes in scalar-broadcast
     * overloadings (both masked and unmasked).
     *
     * @param v a second input vector
     * @return the result of multiplying this vector by the second input vector
     * @see #mul(Vector,VectorMask)
     * @see IntVector#mul(int)
     * @see VectorOperators#MUL
     * @see #lanewise(VectorOperators.Binary,Vector)
     * @see IntVector#lanewise(VectorOperators.Binary,int)
     */
    public abstract Vector<E> mul(Vector<E> v);

    /**
     * Multiplies this vector by a second input vector
     * under the control of a mask.
     *
     * This is a lane-wise binary operation which applies
     * the primitive multiplication operation ({@code *})
     * to each pair of corresponding lane values.
     *
     * For any lane unset in the mask, the primitive operation is
     * suppressed and this vector retains the original value stored in
     * that lane.
     *
     * This method is also equivalent to the expression
     * {@link #lanewise(VectorOperators.Binary,Vector,VectorMask)
     *    lanewise}{@code (}{@link VectorOperators#MUL
     *    MUL}{@code , v, m)}.
     *
     * <p>
     * As a full-service named operation, this method
     * comes in masked and unmasked overloadings, and
     * (in subclasses) also comes in scalar-broadcast
     * overloadings (both masked and unmasked).
     *
     * @param v the second input vector
     * @param m the mask controlling lane selection
     * @return the result of multiplying this vector by the given vector
     * @see #mul(Vector)
     * @see IntVector#mul(int,VectorMask)
     * @see VectorOperators#MUL
     * @see #lanewise(VectorOperators.Binary,Vector,VectorMask)
     * @see IntVector#lanewise(VectorOperators.Binary,int,VectorMask)
     */
    public abstract Vector<E> mul(Vector<E> v, VectorMask<E> m);

    /**
     * Divides this vector by a second input vector.
     *
     * This is a lane-wise binary operation which applies
     * the primitive division operation ({@code /})
     * to each pair of corresponding lane values.
     *
     * This method is also equivalent to the expression
     * {@link #lanewise(VectorOperators.Binary,Vector)
     *    lanewise}{@code (}{@link VectorOperators#DIV
     *    DIV}{@code , v)}.
     *
     * <p>
     * As a full-service named operation, this method
     * comes in masked and unmasked overloadings, and
     * (in subclasses) also comes in scalar-broadcast
     * overloadings (both masked and unmasked).
     *
     * @apiNote If the underlying scalar operator does not support
     * division by zero, but is presented with a zero divisor,
     * an {@code ArithmeticException} will be thrown.
     *
     * @param v a second input vector
     * @return the result of dividing this vector by the second input vector
     * @throws ArithmeticException if any lane
     *         in {@code v} is zero
     *         and {@code ETYPE} is not {@code float} or {@code double}.
     * @see #div(Vector,VectorMask)
     * @see DoubleVector#div(double)
     * @see VectorOperators#DIV
     * @see #lanewise(VectorOperators.Binary,Vector)
     * @see IntVector#lanewise(VectorOperators.Binary,int)
     */
    public abstract Vector<E> div(Vector<E> v);

    /**
     * Divides this vector by a second input vector
     * under the control of a mask.
     *
     * This is a lane-wise binary operation which applies
     * the primitive division operation ({@code /})
     * to each pair of corresponding lane values.
     *
     * For any lane unset in the mask, the primitive operation is
     * suppressed and this vector retains the original value stored in
     * that lane.
     *
     * This method is also equivalent to the expression
     * {@link #lanewise(VectorOperators.Binary,Vector,VectorMask)
     *    lanewise}{@code (}{@link VectorOperators#DIV
     *    DIV}{@code , v, m)}.
     *
     * <p>
     * As a full-service named operation, this method
     * comes in masked and unmasked overloadings, and
     * (in subclasses) also comes in scalar-broadcast
     * overloadings (both masked and unmasked).
     *
     * @apiNote If the underlying scalar operator does not support
     * division by zero, but is presented with a zero divisor,
     * an {@code ArithmeticException} will be thrown.
     *
     * @param v a second input vector
     * @param m the mask controlling lane selection
     * @return the result of dividing this vector by the second input vector
     * @throws ArithmeticException if any lane selected by {@code m}
     *         in {@code v} is zero
     *         and {@code ETYPE} is not {@code float} or {@code double}.
     * @see #div(Vector)
     * @see DoubleVector#div(double,VectorMask)
     * @see VectorOperators#DIV
     * @see #lanewise(VectorOperators.Binary,Vector,VectorMask)
     * @see DoubleVector#lanewise(VectorOperators.Binary,double,VectorMask)
     */
    public abstract Vector<E> div(Vector<E> v, VectorMask<E> m);

    /// END OF FULL-SERVICE BINARY METHODS

    /// Non-full-service unary ops: NEG, ABS

    /**
     * Negates this vector.
     *
     * This is a lane-wise unary operation which applies
     * the primitive negation operation ({@code -x})
     * to each input lane.
     *
     * This method is also equivalent to the expression
     * {@link #lanewise(VectorOperators.Unary)
     *    lanewise}{@code (}{@link VectorOperators#NEG
     *    NEG}{@code )}.
     *
     * @apiNote
     * This method has no masked variant, but the corresponding
     * masked operation can be obtained from the
     * {@linkplain #lanewise(VectorOperators.Unary,VectorMask)
     * lanewise method}.
     *
     * @return the negation of this vector
     * @see VectorOperators#NEG
     * @see #lanewise(VectorOperators.Unary)
     * @see #lanewise(VectorOperators.Unary,VectorMask)
     */
    public abstract Vector<E> neg();

    /**
     * Returns the absolute value of this vector.
     *
     * This is a lane-wise unary operation which applies
     * the method {@code Math.abs}
     * to each input lane.
     *
     * This method is also equivalent to the expression
     * {@link #lanewise(VectorOperators.Unary)
     *    lanewise}{@code (}{@link VectorOperators#ABS
     *    ABS}{@code )}.
     *
     * @apiNote
     * This method has no masked variant, but the corresponding
     * masked operation can be obtained from the
     * {@linkplain #lanewise(VectorOperators.Unary,VectorMask)
     * lanewise method}.
     *
     * @return the absolute value of this vector
     * @see VectorOperators#ABS
     * @see #lanewise(VectorOperators.Unary)
     * @see #lanewise(VectorOperators.Unary,VectorMask)
     */
    public abstract Vector<E> abs();

    /// Non-full-service binary ops: MIN, MAX

    /**
     * Computes the smaller of this vector and a second input vector.
     *
     * This is a lane-wise binary operation which applies the
     * operation {@code Math.min()} to each pair of
     * corresponding lane values.
     *
     * This method is also equivalent to the expression
     * {@link #lanewise(VectorOperators.Binary,Vector)
     *    lanewise}{@code (}{@link VectorOperators#MIN
     *    MIN}{@code , v)}.
     *
     * @apiNote
     * This is not a full-service named operation like
     * {@link #add(Vector) add()}.  A masked version of
     * this operation is not directly available
     * but may be obtained via the masked version of
     * {@code lanewise}.  Subclasses define an additional
     * scalar-broadcast overloading of this method.
     *
     * @param v a second input vector
     * @return the lanewise minimum of this vector and the second input vector
     * @see IntVector#min(int)
     * @see VectorOperators#MIN
     * @see #lanewise(VectorOperators.Binary,Vector)
     * @see #lanewise(VectorOperators.Binary,Vector,VectorMask)
     */
    public abstract Vector<E> min(Vector<E> v);

    /**
     * Computes the larger of this vector and a second input vector.
     *
     * This is a lane-wise binary operation which applies the
     * operation {@code Math.max()} to each pair of
     * corresponding lane values.
     *
     * This method is also equivalent to the expression
     * {@link #lanewise(VectorOperators.Binary,Vector)
     *    lanewise}{@code (}{@link VectorOperators#MAX
     *    MAX}{@code , v)}.
     *
     * <p>
     * This is not a full-service named operation like
     * {@link #add(Vector) add()}.  A masked version of
     * this operation is not directly available
     * but may be obtained via the masked version of
     * {@code lanewise}.  Subclasses define an additional
     * scalar-broadcast overloading of this method.
     *
     * @param v a second input vector
     * @return the lanewise maximum of this vector and the second input vector
     * @see IntVector#max(int)
     * @see VectorOperators#MAX
     * @see #lanewise(VectorOperators.Binary,Vector)
     * @see #lanewise(VectorOperators.Binary,Vector,VectorMask)
     */
    public abstract Vector<E> max(Vector<E> v);

    // Reductions

    /**
     * Returns a value accumulated from all the lanes of this vector.
     *
     * This is an associative cross-lane reduction operation which
     * applies the specified operation to all the lane elements.
     * The return value will be equal to this expression:
     * {@code (long) ((EVector)this).reduceLanes(op)}, where {@code EVector}
     * is the vector class specific to this vector's element type
     * {@code ETYPE}.
     * <p>
     * In the case of operations {@code ADD} and {@code MUL},
     * when {@code ETYPE} is {@code float} or {@code double},
     * the precise result, before casting, will reflect the choice
     * of an arbitrary order of operations, which may even vary over time.
     * For further details see the section
     * <a href="VectorOperators.html#fp_assoc">Operations on floating point vectors</a>.
     *
     * @apiNote
     * If the {@code ETYPE} is {@code float} or {@code double},
     * this operation can lose precision and/or range, as a
     * normal part of casting the result down to {@code long}.
     *
     * Usually
     * {@linkplain IntVector#reduceLanes(VectorOperators.Associative)
     * strongly typed access}
     * is preferable, if you are working with a vector
     * subtype that has a known element type.
     *
     * @param op the operation used to combine lane values
     * @return the accumulated result, cast to {@code long}
     * @throws UnsupportedOperationException if this vector does
     *         not support the requested operation
     * @see #reduceLanesToLong(VectorOperators.Associative,VectorMask)
     * @see IntVector#reduceLanes(VectorOperators.Associative)
     * @see FloatVector#reduceLanes(VectorOperators.Associative)
     */
    public abstract long reduceLanesToLong(VectorOperators.Associative op);

    /**
     * Returns a value accumulated from selected lanes of this vector,
     * controlled by a mask.
     *
     * This is an associative cross-lane reduction operation which
     * applies the specified operation to the selected lane elements.
     * The return value will be equal to this expression:
     * {@code (long) ((EVector)this).reduceLanes(op, m)}, where {@code EVector}
     * is the vector class specific to this vector's element type
     * {@code ETYPE}.
     * <p>
     * If no elements are selected, an operation-specific identity
     * value is returned.
     * <ul>
     * <li>
     * If the operation is {@code ADD}, {@code XOR}, or {@code OR},
     * then the identity value is zero.
     * <li>
     * If the operation is {@code MUL},
     * then the identity value is one.
     * <li>
     * If the operation is {@code AND},
     * then the identity value is minus one (all bits set).
     * <li>
     * If the operation is {@code MAX},
     * then the identity value is the {@code MIN_VALUE}
     * of the vector's native {@code ETYPE}.
     * (In the case of floating point types, the value
     * {@code NEGATIVE_INFINITY} is used, and will appear
     * after casting as {@code Long.MIN_VALUE}.
     * <li>
     * If the operation is {@code MIN},
     * then the identity value is the {@code MAX_VALUE}
     * of the vector's native {@code ETYPE}.
     * (In the case of floating point types, the value
     * {@code POSITIVE_INFINITY} is used, and will appear
     * after casting as {@code Long.MAX_VALUE}.
     * </ul>
     * <p>
     * In the case of operations {@code ADD} and {@code MUL},
     * when {@code ETYPE} is {@code float} or {@code double},
     * the precise result, before casting, will reflect the choice
     * of an arbitrary order of operations, which may even vary over time.
     * For further details see the section
     * <a href="VectorOperators.html#fp_assoc">Operations on floating point vectors</a>.
     *
     * @apiNote
     * If the {@code ETYPE} is {@code float} or {@code double},
     * this operation can lose precision and/or range, as a
     * normal part of casting the result down to {@code long}.
     *
     * Usually
     * {@linkplain IntVector#reduceLanes(VectorOperators.Associative,VectorMask)
     * strongly typed access}
     * is preferable, if you are working with a vector
     * subtype that has a known element type.
     *
     * @param op the operation used to combine lane values
     * @param m the mask controlling lane selection
     * @return the reduced result accumulated from the selected lane values
     * @throws UnsupportedOperationException if this vector does
     *         not support the requested operation
     * @see #reduceLanesToLong(VectorOperators.Associative)
     * @see IntVector#reduceLanes(VectorOperators.Associative,VectorMask)
     * @see FloatVector#reduceLanes(VectorOperators.Associative,VectorMask)
     */
    public abstract long reduceLanesToLong(VectorOperators.Associative op,
                                           VectorMask<E> m);

    // Lanewise unary tests

    /**
     * Tests the lanes of this vector
     * according to the given operation.
     *
     * This is a lane-wise unary test operation which applies
     * the given test operation
     * to each lane value.
     * @param op the operation used to test lane values
     * @return the mask result of testing the lanes of this vector,
     *         according to the selected test operator
     * @see VectorOperators.Comparison
     * @see #test(VectorOperators.Test, VectorMask)
     * @see #compare(VectorOperators.Comparison, Vector)
     */
    public abstract VectorMask<E> test(VectorOperators.Test op);

    /**
     * Test selected lanes of this vector,
     * according to the given operation.
     *
     * This is a masked lane-wise unary test operation which applies
     * the given test operation
     * to each lane value.
     *
     * The returned result is equal to the expression
     * {@code test(op).and(m)}.
     *
     * @param op the operation used to test lane values
     * @param m the mask controlling lane selection
     * @return the mask result of testing the lanes of this vector,
     *         according to the selected test operator,
     *         and only in the lanes selected by the mask
     * @see #test(VectorOperators.Test)
     */
    public abstract VectorMask<E> test(VectorOperators.Test op,
                                       VectorMask<E> m);

    // Comparisons

    /**
     * Tests if this vector is equal to another input vector.
     *
     * This is a lane-wise binary test operation which applies
     * the primitive equals operation ({@code ==})
     * to each pair of corresponding lane values.
     * The result is the same as {@code compare(VectorOperators.EQ, v)}.
     *
     * @param v a second input vector
     * @return the mask result of testing lane-wise if this vector
     *         equal to the second input vector
     * @see #compare(VectorOperators.Comparison,Vector)
     * @see VectorOperators#EQ
     * @see #equals
     */
    public abstract VectorMask<E> eq(Vector<E> v);

    /**
     * Tests if this vector is less than another input vector.
     *
     * This is a lane-wise binary test operation which applies
     * the primitive less-than operation ({@code <}) to each lane.
     * The result is the same as {@code compare(VectorOperators.LT, v)}.
     *
     * @param v a second input vector
     * @return the mask result of testing lane-wise if this vector
     *         is less than the second input vector
     * @see #compare(VectorOperators.Comparison,Vector)
     * @see VectorOperators#LT
     */
    public abstract VectorMask<E> lt(Vector<E> v);

    /**
     * Tests this vector by comparing it with another input vector,
     * according to the given comparison operation.
     *
     * This is a lane-wise binary test operation which applies
     * the given comparison operation
     * to each pair of corresponding lane values.
     *
     * @param op the operation used to compare lane values
     * @param v a second input vector
     * @return the mask result of testing lane-wise if this vector
     *         compares to the input, according to the selected
     *         comparison operator
     * @see #eq(Vector)
     * @see #lt(Vector)
     * @see VectorOperators.Comparison
     * @see #compare(VectorOperators.Comparison, Vector, VectorMask)
     * @see #test(VectorOperators.Test)
     */
    public abstract VectorMask<E> compare(VectorOperators.Comparison op,
                                          Vector<E> v);

    /**
     * Tests this vector by comparing it with another input vector,
     * according to the given comparison operation,
     * in lanes selected by a mask.
     *
     * This is a masked lane-wise binary test operation which applies
     * the given comparison operation
     * to each pair of corresponding lane values.
     *
     * The returned result is equal to the expression
     * {@code compare(op,v).and(m)}.
     *
     * @param op the operation used to compare lane values
     * @param v a second input vector
     * @param m the mask controlling lane selection
     * @return the mask result of testing lane-wise if this vector
     *         compares to the input, according to the selected
     *         comparison operator,
     *         and only in the lanes selected by the mask
     * @see #compare(VectorOperators.Comparison, Vector)
     */
    public abstract VectorMask<E> compare(VectorOperators.Comparison op,
                                          Vector<E> v,
                                          VectorMask<E> m);

    /**
     * Tests this vector by comparing it with an input scalar,
     * according to the given comparison operation.
     *
     * This is a lane-wise binary test operation which applies
     * the given comparison operation
     * to each lane value, paired with the broadcast value.
     *
     * <p>
     * The result is the same as
     * {@code this.compare(op, this.broadcast(e))}.
     * That is, the scalar may be regarded as broadcast to
     * a vector of the same species, and then compared
     * against the original vector, using the selected
     * comparison operation.
     *
     * @apiNote
     * The {@code long} value {@code e} must be accurately
     * representable by the {@code ETYPE} of this vector's species,
     * so that {@code e==(long)(ETYPE)e}.  This rule is enforced
     * by the implicit call to {@code broadcast()}.
     * <p>
     * Subtypes improve on this method by sharpening
     * the type of the scalar parameter {@code e}.
     *
     * @param op the operation used to compare lane values
     * @param e the input scalar
     * @return the mask result of testing lane-wise if this vector
     *         compares to the input, according to the selected
     *         comparison operator
     * @throws IllegalArgumentException
     *         if the given {@code long} value cannot
     *         be represented by the vector's {@code ETYPE}
     * @see #broadcast(long)
     * @see #compare(VectorOperators.Comparison,Vector)
     */
    public abstract VectorMask<E> compare(VectorOperators.Comparison op,
                                          long e);

    /**
     * Tests this vector by comparing it with an input scalar,
     * according to the given comparison operation,
     * in lanes selected by a mask.
     *
     * This is a masked lane-wise binary test operation which applies
     * the given comparison operation
     * to each lane value, paired with the broadcast value.
     *
     * The returned result is equal to the expression
     * {@code compare(op,e).and(m)}.
     *
     * @apiNote
     * The {@code long} value {@code e} must be accurately
     * representable by the {@code ETYPE} of this vector's species,
     * so that {@code e==(long)(ETYPE)e}.  This rule is enforced
     * by the implicit call to {@code broadcast()}.
     * <p>
     * Subtypes improve on this method by sharpening
     * the type of the scalar parameter {@code e}.
     *
     * @param op the operation used to compare lane values
     * @param e the input scalar
     * @param m the mask controlling lane selection
     * @return the mask result of testing lane-wise if this vector
     *         compares to the input, according to the selected
     *         comparison operator,
     *         and only in the lanes selected by the mask
     * @throws IllegalArgumentException
     *         if the given {@code long} value cannot
     *         be represented by the vector's {@code ETYPE}
     * @see #broadcast(long)
     * @see #compare(VectorOperators.Comparison,Vector)
     */
    public abstract VectorMask<E> compare(VectorOperators.Comparison op,
                                          long e,
                                          VectorMask<E> m);

    /**
     * Replaces selected lanes of this vector with
     * corresponding lanes from a second input vector
     * under the control of a mask.
     *
     * This is a masked lane-wise binary operation which
     * selects each lane value from one or the other input.
     *
     * <ul>
     * <li>
     * For any lane <em>set</em> in the mask, the new lane value
     * is taken from the second input vector, and replaces
     * whatever value was in the that lane of this vector.
     * <li>
     * For any lane <em>unset</em> in the mask, the replacement is
     * suppressed and this vector retains the original value stored in
     * that lane.
     * </ul>
     *
     * The following pseudocode illustrates this behavior:
     * <pre>{@code
     * Vector<E> a = ...;
     * VectorSpecies<E> species = a.species();
     * Vector<E> b = ...;
     * b.check(species);
     * VectorMask<E> m = ...;
     * ETYPE[] ar = a.toArray();
     * for (int i = 0; i < ar.length; i++) {
     *     if (m.laneIsSet(i)) {
     *         ar[i] = b.lane(i);
     *     }
     * }
     * return EVector.fromArray(s, ar, 0);
     * }</pre>
     *
     * @param v the second input vector, containing replacement lane values
     * @param m the mask controlling lane selection from the second input vector
     * @return the result of blending the lane elements of this vector with
     *         those of the second input vector
     */
    public abstract Vector<E> blend(Vector<E> v, VectorMask<E> m);

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
     * @apiNote
     * The {@code long} value {@code e} must be accurately
     * representable by the {@code ETYPE} of this vector's species,
     * so that {@code e==(long)(ETYPE)e}.  This rule is enforced
     * by the implicit call to {@code broadcast()}.
     * <p>
     * Subtypes improve on this method by sharpening
     * the type of the scalar parameter {@code e}.
     *
     * @param e the input scalar, containing the replacement lane value
     * @param m the mask controlling lane selection of the scalar
     * @return the result of blending the lane elements of this vector with
     *         the scalar value
     */
    public abstract Vector<E> blend(long e, VectorMask<E> m);

    /**
     * Adds the lanes of this vector to their corresponding
     * lane numbers, scaled by a given constant.
     *
     * This is a lane-wise unary operation which, for
     * each lane {@code N}, computes the scaled index value
     * {@code N*scale} and adds it to the value already
     * in lane {@code N} of the current vector.
     *
     * <p> The scale must not be so large, and the element size must
     * not be so small, that that there would be an overflow when
     * computing any of the {@code N*scale} or {@code VLENGTH*scale},
     * when the the result is represented using the vector
     * lane type {@code ETYPE}.
     *
     * <p>
     * The following pseudocode illustrates this behavior:
     * <pre>{@code
     * Vector<E> a = ...;
     * VectorSpecies<E> species = a.species();
     * ETYPE[] ar = a.toArray();
     * for (int i = 0; i < ar.length; i++) {
     *     long d = (long)i * scale;
     *     if (d != (ETYPE) d)  throw ...;
     *     ar[i] += (ETYPE) d;
     * }
     * long d = (long)ar.length * scale;
     * if (d != (ETYPE) d)  throw ...;
     * return EVector.fromArray(s, ar, 0);
     * }</pre>
     *
     * @param scale the number to multiply by each lane index
     *        {@code N}, typically {@code 1}
     * @return the result of incrementing each lane element by its
     *         corresponding lane index {@code N}, scaled by {@code scale}
     * @throws IllegalArgumentException
     *         if the values in the interval
     *         {@code [0..VLENGTH*scale]}
     *         are not representable by the {@code ETYPE}
     */
    public abstract Vector<E> addIndex(int scale);

    // Slicing segments of adjacent lanes

    /**
     * Slices a segment of adjacent lanes, starting at a given
     * {@code origin} lane in the current vector, and continuing (as
     * needed) into an immediately following vector.  The block of
     * {@code VLENGTH} lanes is extracted into its own vector and
     * returned.
     *
     * <p> This is a cross-lane operation that shifts lane elements
     * to the front, from the current vector and the second vector.
     * Both vectors can be viewed as a combined "background" of length
     * {@code 2*VLENGTH}, from which a slice is extracted.
     *
     * The lane numbered {@code N} in the output vector is copied
     * from lane {@code origin+N} of the input vector, if that
     * lane exists, else from lane {@code origin+N-VLENGTH} of
     * the second vector (which is guaranteed to exist).
     *
     * <p> The {@code origin} value must be in the inclusive range
     * {@code 0..VLENGTH}.  As limiting cases, {@code v.slice(0,w)}
     * and {@code v.slice(VLENGTH,w)} return {@code v} and {@code w},
     * respectively.
     *
     * @apiNote
     *
     * This method may be regarded as the inverse of
     * {@link #unslice(int,Vector,int) unslice()},
     * in that the sliced value could be unsliced back into its
     * original position in the two input vectors, without
     * disturbing unrelated elements, as in the following
     * pseudocode:
     * <pre>{@code
     * EVector slice = v1.slice(origin, v2);
     * EVector w1 = slice.unslice(origin, v1, 0);
     * EVector w2 = slice.unslice(origin, v2, 1);
     * assert v1.equals(w1);
     * assert v2.equals(w2);
     * }</pre>
     *
     * <p> This method also supports a variety of cross-lane shifts and
     * rotates as follows:
     * <ul>
     *
     * <li>To shift lanes forward to the front of the vector, supply a
     * zero vector for the second operand and specify the shift count
     * as the origin.  For example: {@code v.slice(shift, v.broadcast(0))}.
     *
     * <li>To shift lanes backward to the back of the vector, supply a
     * zero vector for the <em>first</em> operand, and specify the
     * negative shift count as the origin (modulo {@code VLENGTH}.
     * For example: {@code v.broadcast(0).slice(v.length()-shift, v)}.
     *
     * <li>To rotate lanes forward toward the front end of the vector,
     * cycling the earliest lanes around to the back, supply the same
     * vector for both operands and specify the rotate count as the
     * origin.  For example: {@code v.slice(rotate, v)}.
     *
     * <li>To rotate lanes backward toward the back end of the vector,
     * cycling the latest lanes around to the front, supply the same
     * vector for both operands and specify the negative of the rotate
     * count (modulo {@code VLENGTH}) as the origin.  For example:
     * {@code v.slice(v.length() - rotate, v)}.
     *
     * <li>
     * Since {@code origin} values less then zero or more than
     * {@code VLENGTH} will be rejected, if you need to rotate
     * by an unpredictable multiple of {@code VLENGTH}, be sure
     * to reduce the origin value into the required range.
     * The {@link VectorSpecies#loopBound(int) loopBound()}
     * method can help with this.  For example:
     * {@code v.slice(rotate - v.species().loopBound(rotate), v)}.
     *
     * </ul>
     *
     * @param origin the first input lane to transfer into the slice
     * @param v1 a second vector logically concatenated with the first,
     *        before the slice is taken (if omitted it defaults to zero)
     * @return a contiguous slice of {@code VLENGTH} lanes, taken from
     *         this vector starting at the indicated origin, and
     *         continuing (as needed) into the second vector
     * @throws ArrayIndexOutOfBoundsException if {@code origin}
     *         is negative or greater than {@code VLENGTH}
     * @see #slice(int,Vector,VectorMask)
     * @see #slice(int)
     * @see #unslice(int,Vector,int)
     */
    public abstract Vector<E> slice(int origin, Vector<E> v1);

    /**
     * Slices a segment of adjacent lanes
     * under the control of a mask,
     * starting at a given
     * {@code origin} lane in the current vector, and continuing (as
     * needed) into an immediately following vector.  The block of
     * {@code VLENGTH} lanes is extracted into its own vector and
     * returned.
     *
     * The resulting vector will be zero in all lanes unset in the
     * given mask.  Lanes set in the mask will contain data copied
     * from selected lanes of {@code this} or {@code v1}.
     *
     * <p> This is a cross-lane operation that shifts lane elements
     * to the front, from the current vector and the second vector.
     * Both vectors can be viewed as a combined "background" of length
     * {@code 2*VLENGTH}, from which a slice is extracted.
     *
     * The returned result is equal to the expression
     * {@code broadcast(0).blend(slice(origin,v1),m)}.
     *
     * @apiNote
     * This method may be regarded as the inverse of
     * {@code #unslice(int,Vector,int,VectorMask) unslice()},
     * in that the sliced value could be unsliced back into its
     * original position in the two input vectors, without
     * disturbing unrelated elements, as in the following
     * pseudocode:
     * <pre>{@code
     * EVector slice = v1.slice(origin, v2, m);
     * EVector w1 = slice.unslice(origin, v1, 0, m);
     * EVector w2 = slice.unslice(origin, v2, 1, m);
     * assert v1.equals(w1);
     * assert v2.equals(w2);
     * }</pre>
     *
     * @param origin the first input lane to transfer into the slice
     * @param v1 a second vector logically concatenated with the first,
     *        before the slice is taken (if omitted it defaults to zero)
     * @param m the mask controlling lane selection into the resulting vector
     * @return a contiguous slice of {@code VLENGTH} lanes, taken from
     *         this vector starting at the indicated origin, and
     *         continuing (as needed) into the second vector
     * @throws ArrayIndexOutOfBoundsException if {@code origin}
     *         is negative or greater than {@code VLENGTH}
     * @see #slice(int,Vector)
     * @see #unslice(int,Vector,int,VectorMask)
     */
    // This doesn't pull its weight, but its symmetrical with
    // masked unslice, and might cause questions if missing.
    // It could make for clearer code.
    public abstract Vector<E> slice(int origin, Vector<E> v1, VectorMask<E> m);

    /**
     * Slices a segment of adjacent lanes, starting at a given
     * {@code origin} lane in the current vector.  A block of
     * {@code VLENGTH} lanes, possibly padded with zero lanes, is
     * extracted into its own vector and returned.
     *
     * This is a convenience method which slices from a single
     * vector against an extended background of zero lanes.
     * It is equivalent to
     * {@link #slice(int,Vector) slice}{@code
     * (origin, }{@link #broadcast(long) broadcast}{@code (0))}.
     * It may also be viewed simply as a cross-lane shift
     * from later to earlier lanes, with zeroes filling
     * in the vacated lanes at the end of the vector.
     * In this view, the shift count is {@code origin}.
     *
     * @param origin the first input lane to transfer into the slice
     * @return the last {@code VLENGTH-origin} input lanes,
     *         placed starting in the first lane of the ouput,
     *         padded at the end with zeroes
     * @throws ArrayIndexOutOfBoundsException if {@code origin}
     *         is negative or greater than {@code VLENGTH}
     * @see #slice(int,Vector)
     * @see #unslice(int,Vector,int)
     */
    // This API point pulls its weight as a teaching aid,
    // though it's a one-off and broadcast(0) is easy.
    public abstract Vector<E> slice(int origin);

    /**
     * Reverses a {@linkplain #slice(int,Vector) slice()}, inserting
     * the current vector as a slice within another "background" input
     * vector, which is regarded as one or the other input to a
     * hypothetical subsequent {@code slice()} operation.
     *
     * <p> This is a cross-lane operation that permutes the lane
     * elements of the current vector toward the back and inserts them
     * into a logical pair of background vectors.  Only one of the
     * pair will be returned, however.  The background is formed by
     * duplicating the second input vector.  (However, the output will
     * never contain two duplicates from the same input lane.)
     *
     * The lane numbered {@code N} in the input vector is copied into
     * lane {@code origin+N} of the first background vector, if that
     * lane exists, else into lane {@code origin+N-VLENGTH} of the
     * second background vector (which is guaranteed to exist).
     *
     * The first or second background vector, updated with the
     * inserted slice, is returned.  The {@code part} number of zero
     * or one selects the first or second updated background vector.
     *
     * <p> The {@code origin} value must be in the inclusive range
     * {@code 0..VLENGTH}.  As limiting cases, {@code v.unslice(0,w,0)}
     * and {@code v.unslice(VLENGTH,w,1)} both return {@code v}, while
     * {@code v.unslice(0,w,1)} and {@code v.unslice(VLENGTH,w,0)}
     * both return {@code w}.
     *
     * @apiNote
     * This method supports a variety of cross-lane insertion
     * operations as follows:
     * <ul>
     *
     * <li>To insert near the end of a background vector {@code w}
     * at some offset, specify the offset as the origin and
     * select part zero. For example: {@code v.unslice(offset, w, 0)}.
     *
     * <li>To insert near the end of a background vector {@code w},
     * but capturing the overflow into the next vector {@code x},
     * specify the offset as the origin and select part one.
     * For example: {@code v.unslice(offset, x, 1)}.
     *
     * <li>To insert the last {@code N} items near the beginning
     * of a background vector {@code w}, supply a {@code VLENGTH-N}
     * as the origin and select part one.
     * For example: {@code v.unslice(v.length()-N, w)}.
     *
     * </ul>
     *
     * @param origin the first output lane to receive the slice
     * @param w the background vector that (as two copies) will receive
     *        the inserted slice
     * @param part the part number of the result (either zero or one)
     * @return either the first or second part of a pair of
     *         background vectors {@code w}, updated by inserting
     *         this vector at the indicated origin
     * @throws ArrayIndexOutOfBoundsException if {@code origin}
     *         is negative or greater than {@code VLENGTH},
     *         or if {@code part} is not zero or one
     * @see #slice(int,Vector)
     * @see #unslice(int,Vector,int,VectorMask)
     */
    public abstract Vector<E> unslice(int origin, Vector<E> w, int part);

    /**
     * Reverses a {@linkplain #slice(int,Vector) slice()}, inserting
     * (under the control of a mask)
     * the current vector as a slice within another "background" input
     * vector, which is regarded as one or the other input to a
     * hypothetical subsequent {@code slice()} operation.
     *
     * <p> This is a cross-lane operation that permutes the lane
     * elements of the current vector forward and inserts its lanes
     * (when selected by the mask) into a logical pair of background
     * vectors.  As with the
     * {@linkplain #unslice(int,Vector,int) unmasked version} of this method,
     * only one of the pair will be returned, as selected by the
     * {@code part} number.
     *
     * For each lane {@code N} selected by the mask, the lane value
     * is copied into
     * lane {@code origin+N} of the first background vector, if that
     * lane exists, else into lane {@code origin+N-VLENGTH} of the
     * second background vector (which is guaranteed to exist).
     * Background lanes retain their original values if the
     * corresponding input lanes {@code N} are unset in the mask.
     *
     * The first or second background vector, updated with set lanes
     * of the inserted slice, is returned.  The {@code part} number of
     * zero or one selects the first or second updated background
     * vector.
     *
     * @param origin the first output lane to receive the slice
     * @param w the background vector that (as two copies) will receive
     *        the inserted slice, if they are set in {@code m}
     * @param part the part number of the result (either zero or one)
     * @param m the mask controlling lane selection from the current vector
     * @return either the first or second part of a pair of
     *         background vectors {@code w}, updated by inserting
     *         selected lanes of this vector at the indicated origin
     * @throws ArrayIndexOutOfBoundsException if {@code origin}
     *         is negative or greater than {@code VLENGTH},
     *         or if {@code part} is not zero or one
     * @see #unslice(int,Vector,int)
     * @see #slice(int,Vector)
     */
    public abstract Vector<E> unslice(int origin, Vector<E> w, int part, VectorMask<E> m);

    /**
     * Reverses a {@linkplain #slice(int) slice()}, inserting
     * the current vector as a slice within a "background" input
     * of zero lane values.  Compared to other {@code unslice()}
     * methods, this method only returns the first of the
     * pair of background vectors.
     *
     * This is a convenience method which returns the result of
     * {@link #unslice(int,Vector,int) unslice}{@code
     * (origin, }{@link #broadcast(long) broadcast}{@code (0), 0)}.
     * It may also be viewed simply as a cross-lane shift
     * from earlier to later lanes, with zeroes filling
     * in the vacated lanes at the beginning of the vector.
     * In this view, the shift count is {@code origin}.
     *
     * @param origin the first output lane to receive the slice
     * @return the first {@code VLENGTH-origin} input lanes,
     *         placed starting at the given origin,
     *         padded at the beginning with zeroes
     * @throws ArrayIndexOutOfBoundsException if {@code origin}
     *         is negative or greater than {@code VLENGTH}
     * @see #unslice(int,Vector,int)
     * @see #slice(int)
     */
    // This API point pulls its weight as a teaching aid,
    // though it's a one-off and broadcast(0) is easy.
    public abstract Vector<E> unslice(int origin);

    // ISSUE: Add a slice which uses a mask instead of an origin?
    //public abstract Vector<E> slice(VectorMask<E> support);

    // ISSUE: Add some more options for questionable edge conditions?
    // We might define enum EdgeOption { ERROR, ZERO, WRAP } for the
    // default of throwing AIOOBE, or substituting zeroes, or just
    // reducing the out-of-bounds index modulo VLENGTH.  Similar
    // concerns also apply to general Shuffle operations.  For now,
    // just support ERROR, since that is safest.

    /**
     * Rearranges the lane elements of this vector, selecting lanes
     * under the control of a specific shuffle.
     *
     * This is a cross-lane operation that rearranges the lane
     * elements of this vector.
     *
     * For each lane {@code N} of the shuffle, and for each lane
     * source index {@code I=s.laneSource(N)} in the shuffle,
     * the output lane {@code N} obtains the value from
     * the input vector at lane {@code I}.
     *
     * @param s the shuffle controlling lane index selection
     * @return the rearrangement of the lane elements of this vector
     * @throws IndexOutOfBoundsException if there are any exceptional
     *        source indexes in the shuffle
     * @see #rearrange(VectorShuffle,VectorMask)
     * @see #rearrange(VectorShuffle,Vector)
     * @see VectorShuffle#laneIsValid()
     */
    public abstract Vector<E> rearrange(VectorShuffle<E> s);

    /**
     * Rearranges the lane elements of this vector, selecting lanes
     * under the control of a specific shuffle and a mask.
     *
     * This is a cross-lane operation that rearranges the lane
     * elements of this vector.
     *
     * For each lane {@code N} of the shuffle, and for each lane
     * source index {@code I=s.laneSource(N)} in the shuffle,
     * the output lane {@code N} obtains the value from
     * the input vector at lane {@code I} if the mask is set.
     * Otherwise the output lane {@code N} is set to zero.
     *
     * <p> This method returns the value of this pseudocode:
     * <pre>{@code
     * Vector<E> r = this.rearrange(s.wrapIndexes());
     * VectorMask<E> valid = s.laneIsValid();
     * if (m.andNot(valid).anyTrue()) throw ...;
     * return broadcast(0).blend(r, m);
     * }</pre>
     *
     * @param s the shuffle controlling lane index selection
     * @param m the mask controlling application of the shuffle
     * @return the rearrangement of the lane elements of this vector
     * @throws IndexOutOfBoundsException if there are any exceptional
     *        source indexes in the shuffle where the mask is set
     * @see #rearrange(VectorShuffle)
     * @see #rearrange(VectorShuffle,Vector)
     * @see VectorShuffle#laneIsValid()
     */
    public abstract Vector<E> rearrange(VectorShuffle<E> s, VectorMask<E> m);

    /**
     * Rearranges the lane elements of two vectors, selecting lanes
     * under the control of a specific shuffle, using both normal and
     * exceptional indexes in the shuffle to steer data.
     *
     * This is a cross-lane operation that rearranges the lane
     * elements of the two input vectors (the current vector
     * and a second vector {@code v}).
     *
     * For each lane {@code N} of the shuffle, and for each lane
     * source index {@code I=s.laneSource(N)} in the shuffle,
     * the output lane {@code N} obtains the value from
     * the first vector at lane {@code I} if {@code I>=0}.
     * Otherwise, the exceptional index {@code I} is wrapped
     * by adding {@code VLENGTH} to it and used to index
     * the <em>second</em> vector, at index {@code I+VLENGTH}.
     *
     * <p> This method returns the value of this pseudocode:
     * <pre>{@code
     * Vector<E> r1 = this.rearrange(s.wrapIndexes());
     * // or else: r1 = this.rearrange(s, s.laneIsValid());
     * Vector<E> r2 = v.rearrange(s.wrapIndexes());
     * return r2.blend(r1,s.laneIsValid());
     * }</pre>
     *
     * @param s the shuffle controlling lane selection from both input vectors
     * @param v the second input vector
     * @return the rearrangement of lane elements of this vector and
     *         a second input vector
     * @see #rearrange(VectorShuffle)
     * @see #rearrange(VectorShuffle,VectorMask)
     * @see VectorShuffle#laneIsValid()
     * @see #slice(int,Vector)
     */
    public abstract Vector<E> rearrange(VectorShuffle<E> s, Vector<E> v);

    /**
     * Using index values stored in the lanes of this vector,
     * assemble values stored in second vector {@code v}.
     * The second vector thus serves as a table, whose
     * elements are selected by indexes in the current vector.
     *
     * This is a cross-lane operation that rearranges the lane
     * elements of the argument vector, under the control of
     * this vector.
     *
     * For each lane {@code N} of this vector, and for each lane
     * value {@code I=this.lane(N)} in this vector,
     * the output lane {@code N} obtains the value from
     * the argument vector at lane {@code I}.
     *
     * In this way, the result contains only values stored in the
     * argument vector {@code v}, but presented in an order which
     * depends on the index values in {@code this}.
     *
     * The result is the same as the expression
     * {@code v.rearrange(this.toShuffle())}.
     *
     * @param v the vector supplying the result values
     * @return the rearrangement of the lane elements of {@code v}
     * @throws IndexOutOfBoundsException if any invalid
     *         source indexes are found in {@code this}
     * @see #rearrange(VectorShuffle)
     */
    public abstract Vector<E> selectFrom(Vector<E> v);

    /**
     * Using index values stored in the lanes of this vector,
     * assemble values stored in second vector, under the control
     * of a mask.
     * Using index values stored in the lanes of this vector,
     * assemble values stored in second vector {@code v}.
     * The second vector thus serves as a table, whose
     * elements are selected by indexes in the current vector.
     * Lanes that are unset in the mask receive a
     * zero rather than a value from the table.
     *
     * This is a cross-lane operation that rearranges the lane
     * elements of the argument vector, under the control of
     * this vector and the mask.
     *
     * The result is the same as the expression
     * {@code v.rearrange(this.toShuffle(), m)}.
     *
     * @param v the vector supplying the result values
     * @param m the mask controlling selection from {@code v}
     * @return the rearrangement of the lane elements of {@code v}
     * @throws IndexOutOfBoundsException if any invalid
     *         source indexes are found in {@code this},
     *         in a lane which is set in the mask
     * @see #selectFrom(Vector)
     * @see #rearrange(VectorShuffle,VectorMask)
     */
    public abstract Vector<E> selectFrom(Vector<E> v, VectorMask<E> m);

    // Conversions

    /**
     * Returns a vector of the same species as this one
     * where all lane elements are set to
     * the primitive value {@code e}.
     *
     * The contents of the current vector are discarded;
     * only the species is relevant to this operation.
     *
     * <p> This method returns the value of this expression:
     * {@code EVector.broadcast(this.species(), (ETYPE)e)}, where
     * {@code EVector} is the vector class specific to this
     * vector's element type {@code ETYPE}.
     *
     * <p>
     * The {@code long} value {@code e} must be accurately
     * representable by the {@code ETYPE} of this vector's species,
     * so that {@code e==(long)(ETYPE)e}.
     *
     * If this rule is violated the problem is not detected
     * statically, but an {@code IllegalArgumentException} is thrown
     * at run-time.  Thus, this method somewhat weakens the static
     * type checking of immediate constants and other scalars, but it
     * makes up for this by improving the expressiveness of the
     * generic API.  Note that an {@code e} value in the range
     * {@code [-128..127]} is always acceptable, since every
     * {@code ETYPE} will accept every {@code byte} value.
     *
     * @apiNote
     * Subtypes improve on this method by sharpening
     * the method return type and
     * and the type of the scalar parameter {@code e}.
     *
     * @param e the value to broadcast
     * @return a vector where all lane elements are set to
     *         the primitive value {@code e}
     * @throws IllegalArgumentException
     *         if the given {@code long} value cannot
     *         be represented by the vector's {@code ETYPE}
     * @see VectorSpecies#broadcast(long)
     * @see IntVector#broadcast(int)
     * @see FloatVector#broadcast(float)
     */
    public abstract Vector<E> broadcast(long e);

    /**
     * Returns a mask of same species as this vector,
     * where each lane is set or unset according to given
     * single boolean, which is broadcast to all lanes.
     * <p>
     * This method returns the value of this expression:
     * {@code species().maskAll(bit)}.
     *
     * @param bit the given mask bit to be replicated
     * @return a mask where each lane is set or unset according to
     *         the given bit
     * @see VectorSpecies#maskAll(boolean)
     */
    public abstract VectorMask<E> maskAll(boolean bit);

    /**
     * Converts this vector into a shuffle, converting the lane values
     * to {@code int} and regarding them as source indexes.
     * <p>
     * This method behaves as if it returns the result of creating a shuffle
     * given an array of the vector elements, as follows:
     * <pre>{@code
     * long[] a = this.toLongArray();
     * int[] sa = new int[a.length];
     * for (int i = 0; i < a.length; i++) {
     *     sa[i] = (int) a[i];
     * }
     * return VectorShuffle.fromValues(this.species(), sa);
     * }</pre>
     *
     * @return a shuffle representation of this vector
     * @see VectorShuffle#fromValues(VectorSpecies,int...)
     */
    public abstract VectorShuffle<E> toShuffle();

    // Bitwise preserving

    /**
     * Transforms this vector to a vector of the given species of
     * element type {@code F}, reinterpreting the bytes of this
     * vector without performing any value conversions.
     *
     * <p> Depending on the selected species, this operation may
     * either <a href="Vector.html#expansion">expand or contract</a>
     * its logical result, in which case a non-zero {@code part}
     * number can further control the selection and steering of the
     * logical result into the physical output vector.
     *
     * <p>
     * The underlying bits of this vector are copied to the resulting
     * vector without modification, but those bits, before copying,
     * may be truncated if the this vector's bit-size is greater than
     * desired vector's bit size, or filled with zero bits if this
     * vector's bit-size is less than desired vector's bit-size.
     *
     * <p> If the old and new species have different shape, this is a
     * <em>shape-changing</em> operation, and may have special
     * implementation costs.
     *
     * <p> The method behaves as if this vector is stored into a byte
     * buffer or array using little-endian byte ordering and then the
     * desired vector is loaded from the same byte buffer or array
     * using the same ordering.
     *
     * <p> The following pseudocode illustrates the behavior:
     * <pre>{@code
     * int domSize = this.byteSize();
     * int ranSize = species.vectorByteSize();
     * int M = (domSize > ranSize ? domSize / ranSize : ranSize / domSize);
     * assert Math.abs(part) < M;
     * assert (part == 0) || (part > 0) == (domSize > ranSize);
     * byte[] ra = new byte[Math.max(domSize, ranSize)];
     * if (domSize > ranSize) {  // expansion
     *     this.intoByteArray(ra, 0, ByteOrder.native());
     *     int origin = part * ranSize;
     *     return species.fromByteArray(ra, origin, ByteOrder.native());
     * } else {  // contraction or size-invariant
     *     int origin = (-part) * domSize;
     *     this.intoByteArray(ra, origin, ByteOrder.native());
     *     return species.fromByteArray(ra, 0, ByteOrder.native());
     * }
     * }</pre>
     *
     * @apiNote Although this method is defined as if the vectors in
     * question were loaded or stored into memory, memory semantics
     * has little to do or nothing with the actual implementation.
     * The appeal to little-endian ordering is simply a shorthand
     * for what could otherwise be a large number of detailed rules
     * concerning the mapping between lane-structured vectors and
     * byte-structured vectors.
     *
     * @param species the desired vector species
     * @param part the <a href="Vector.html#expansion">part number</a>
     *        of the result, or zero if neither expanding nor contracting
     * @param <F> the boxed element type of the species
     * @return a vector transformed, by shape and element type, from this vector
     * @see Vector#convertShape(VectorOperators.Conversion,VectorSpecies,int)
     * @see Vector#castShape(VectorSpecies,int)
     * @see VectorSpecies#partLimit(VectorSpecies,boolean)
     */
    public abstract <F> Vector<F> reinterpretShape(VectorSpecies<F> species, int part);

    /**
     * Views this vector as a vector of the same shape
     * and contents but a lane type of {@code byte},
     * where the bytes are extracted from the lanes
     * according to little-endian order.
     * It is a convenience method for the expression
     * {@code reinterpretShape(species().withLanes(byte.class))}.
     * It may be considered an inverse to the various
     * methods which consolidate bytes into larger lanes
     * within the same vector, such as
     * {@link Vector#reinterpretAsInts()}.
     *
     * @return a {@code ByteVector} with the same shape and information content
     * @see Vector#reinterpretShape(VectorSpecies,int)
     * @see IntVector#intoByteArray(byte[], int, ByteOrder)
     * @see FloatVector#intoByteArray(byte[], int, ByteOrder)
     * @see VectorSpecies#withLanes(Class)
     */
    public abstract ByteVector reinterpretAsBytes();

    /**
     * Reinterprets this vector as a vector of the same shape
     * and contents but a lane type of {@code short},
     * where the lanes are assembled from successive bytes
     * according to little-endian order.
     * It is a convenience method for the expression
     * {@code reinterpretShape(species().withLanes(short.class))}.
     * It may be considered an inverse to {@link Vector#reinterpretAsBytes()}.
     *
     * @return a {@code ShortVector} with the same shape and information content
     */
    public abstract ShortVector reinterpretAsShorts();

    /**
     * Reinterprets this vector as a vector of the same shape
     * and contents but a lane type of {@code int},
     * where the lanes are assembled from successive bytes
     * according to little-endian order.
     * It is a convenience method for the expression
     * {@code reinterpretShape(species().withLanes(int.class))}.
     * It may be considered an inverse to {@link Vector#reinterpretAsBytes()}.
     *
     * @return a {@code IntVector} with the same shape and information content
     */
    public abstract IntVector reinterpretAsInts();

    /**
     * Reinterprets this vector as a vector of the same shape
     * and contents but a lane type of {@code long},
     * where the lanes are assembled from successive bytes
     * according to little-endian order.
     * It is a convenience method for the expression
     * {@code reinterpretShape(species().withLanes(long.class))}.
     * It may be considered an inverse to {@link Vector#reinterpretAsBytes()}.
     *
     * @return a {@code LongVector} with the same shape and information content
     */
    public abstract LongVector reinterpretAsLongs();

    /**
     * Reinterprets this vector as a vector of the same shape
     * and contents but a lane type of {@code float},
     * where the lanes are assembled from successive bytes
     * according to little-endian order.
     * It is a convenience method for the expression
     * {@code reinterpretShape(species().withLanes(float.class))}.
     * It may be considered an inverse to {@link Vector#reinterpretAsBytes()}.
     *
     * @return a {@code FloatVector} with the same shape and information content
     */
    public abstract FloatVector reinterpretAsFloats();

    /**
     * Reinterprets this vector as a vector of the same shape
     * and contents but a lane type of {@code double},
     * where the lanes are assembled from successive bytes
     * according to little-endian order.
     * It is a convenience method for the expression
     * {@code reinterpretShape(species().withLanes(double.class))}.
     * It may be considered an inverse to {@link Vector#reinterpretAsBytes()}.
     *
     * @return a {@code DoubleVector} with the same shape and information content
     */
    public abstract DoubleVector reinterpretAsDoubles();

    /**
     * Views this vector as a vector of the same shape, length, and
     * contents, but a lane type that is not a floating-point type.
     *
     * This is a lane-wise reinterpretation cast on the lane values.
     * As such, this method does not change {@code VSHAPE} or
     * {@code VLENGTH}, and there is no change to the bitwise contents
     * of the vector.  If the vector's {@code ETYPE} is already an
     * integral type, the same vector is returned unchanged.
     *
     * This method returns the value of this expression:
     * {@code convert(conv,0)}, where {@code conv} is
     * {@code VectorOperators.Conversion.ofReinterpret(E.class,F.class)},
     * and {@code F} is the non-floating-point type of the
     * same size as {@code E}.
     *
     * @apiNote
     * Subtypes improve on this method by sharpening
     * the return type.
     *
     * @return the original vector, reinterpreted as non-floating point
     * @see VectorOperators.Conversion#ofReinterpret(Class,Class)
     * @see Vector#convert(VectorOperators.Conversion,int)
     */
    public abstract Vector<?> viewAsIntegralLanes();

    /**
     * Views this vector as a vector of the same shape, length, and
     * contents, but a lane type that is a floating-point type.
     *
     * This is a lane-wise reinterpretation cast on the lane values.
     * As such, there this method does not change {@code VSHAPE} or
     * {@code VLENGTH}, and there is no change to the bitwise contents
     * of the vector.  If the vector's {@code ETYPE} is already a
     * float-point type, the same vector is returned unchanged.
     *
     * If the vector's element size does not match any floating point
     * type size, an {@code IllegalArgumentException} is thrown.
     *
     * This method returns the value of this expression:
     * {@code convert(conv,0)}, where {@code conv} is
     * {@code VectorOperators.Conversion.ofReinterpret(E.class,F.class)},
     * and {@code F} is the floating-point type of the
     * same size as {@code E}, if any.
     *
     * @apiNote
     * Subtypes improve on this method by sharpening
     * the return type.
     *
     * @return the original vector, reinterpreted as floating point
     * @throws UnsupportedOperationException if there is no floating point
     *         type the same size as the lanes of this vector
     * @see VectorOperators.Conversion#ofReinterpret(Class,Class)
     * @see Vector#convert(VectorOperators.Conversion,int)
     */
    public abstract Vector<?> viewAsFloatingLanes();

    /**
     * Convert this vector to a vector of the same shape and a new
     * element type, converting lane values from the current {@code ETYPE}
     * to a new lane type (called {@code FTYPE} here) according to the
     * indicated {@linkplain VectorOperators.Conversion conversion}.
     *
     * This is a lane-wise shape-invariant operation which copies
     * {@code ETYPE} values from the input vector to corresponding
     * {@code FTYPE} values in the result.  Depending on the selected
     * conversion, this operation may either
     * <a href="Vector.html#expansion">expand or contract</a> its
     * logical result, in which case a non-zero {@code part} number
     * can further control the selection and steering of the logical
     * result into the physical output vector.
     *
     * <p> Each specific conversion is described by a conversion
     * constant in the class {@link VectorOperators}.  Each conversion
     * operator has a specified {@linkplain
     * VectorOperators.Conversion#domainType() domain type} and
     * {@linkplain VectorOperators.Conversion#rangeType() range type}.
     * The domain type must exactly match the lane type of the input
     * vector, while the range type determines the lane type of the
     * output vectors.
     *
     * <p> A conversion operator may be classified as (respectively)
     * in-place, expanding, or contracting, depending on whether the
     * bit-size of its domain type is (respectively) equal, less than,
     * or greater than the bit-size of its range type.
     *
     * <p> Independently, conversion operations can also be classified
     * as reinterpreting or value-transforming, depending on whether
     * the conversion copies representation bits unchanged, or changes
     * the representation bits in order to retain (part or all of)
     * the logical value of the input value.
     *
     * <p> If a reinterpreting conversion contracts, it will truncate the
     * upper bits of the input.  If it expands, it will pad upper bits
     * of the output with zero bits, when there are no corresponding
     * input bits.
     *
     * <p> An expanding conversion such as {@code S2I} ({@code short}
     * value to {@code int}) takes a scalar value and represents it
     * in a larger format (always with some information redundancy).
     *
     * A contracting conversion such as {@code D2F} ({@code double}
     * value to {@code float}) takes a scalar value and represents it
     * in a smaller format (always with some information loss).
     *
     * Some in-place conversions may also include information loss,
     * such as {@code L2D} ({@code long} value to {@code double})
     * or {@code F2I}  ({@code float} value to {@code int}).
     *
     * Reinterpreting in-place conversions are not lossy, unless the
     * bitwise value is somehow not legal in the output type.
     * Converting the bit-pattern of a {@code NaN} may discard bits
     * from the {@code NaN}'s significand.
     *
     * <p> This classification is important, because, unless otherwise
     * documented, conversion operations <em>never change vector
     * shape</em>, regardless of how they may change <em>lane sizes</em>.
     *
     * Therefore an <em>expanding</em> conversion cannot store all of its
     * results in its output vector, because the output vector has fewer
     * lanes of larger size, in order to have the same overall bit-size as
     * its input.
     *
     * Likewise, a contracting conversion must store its relatively small
     * results into a subset of the lanes of the output vector, defaulting
     * the unused lanes to zero.
     *
     * <p> As an example, a conversion from {@code byte} to {@code long}
     * ({@code M=8}) will discard 87.5% of the input values in order to
     * convert the remaining 12.5% into the roomy {@code long} lanes of
     * the output vector. The inverse conversion will convert back all of
     * the large results, but will waste 87.5% of the lanes in the output
     * vector.
     *
     * <em>In-place</em> conversions ({@code M=1}) deliver all of
     * their results in one output vector, without wasting lanes.
     *
     * <p> To manage the details of these
     * <a href="Vector.html#expansion">expansions and contractions</a>,
     * a non-zero {@code part} parameter selects partial results from
     * expansions, or steers the results of contractions into
     * corresponding locations, as follows:
     *
     * <ul>
     * <li> expanding by {@code M}: {@code part} must be in the range
     * {@code [0..M-1]}, and selects the block of {@code VLENGTH/M} input
     * lanes starting at the <em>origin lane</em> at {@code part*VLENGTH/M}.

     * <p> The {@code VLENGTH/M} output lanes represent a partial
     * slice of the whole logical result of the conversion, filling
     * the entire physical output vector.
     *
     * <li> contracting by {@code M}: {@code part} must be in the range
     * {@code [-M+1..0]}, and steers all {@code VLENGTH} input lanes into
     * the output located at the <em>origin lane</em> {@code -part*VLENGTH}.
     * There is a total of {@code VLENGTH*M} output lanes, and those not
     * holding converted input values are filled with zeroes.
     *
     * <p> A group of such output vectors, with logical result parts
     * steered to disjoint blocks, can be reassembled using the
     * {@linkplain VectorOperators#OR bitwise or} or (for floating
     * point) the {@link VectorOperators#FIRST_NONZERO FIRST_NONZERO}
     * operator.
     *
     * <li> in-place ({@code M=1}): {@code part} must be zero.
     * Both vectors have the same {@code VLENGTH}.  The result is
     * always positioned at the <em>origin lane</em> of zero.
     *
     * </ul>
     *
     * <p> This method is a restricted version of the more general
     * but less frequently used <em>shape-changing</em> method
     * {@link #convertShape(VectorOperators.Conversion,VectorSpecies,int)
     * convertShape()}.
     * The result of this method is the same as the expression
     * {@code this.convertShape(conv, rsp, this.broadcast(part))},
     * where the output species is
     * {@code rsp=this.species().withLanes(FTYPE.class)}.
     *
     * @param conv the desired scalar conversion to apply lane-wise
     * @param part the <a href="Vector.html#expansion">part number</a>
     *        of the result, or zero if neither expanding nor contracting
     * @param <F> the boxed element type of the species
     * @return a vector converted by shape and element type from this vector
     * @throws ArrayIndexOutOfBoundsException unless {@code part} is zero,
     *         or else the expansion ratio is {@code M} and
     *         {@code part} is positive and less than {@code M},
     *         or else the contraction ratio is {@code M} and
     *         {@code part} is negative and greater {@code -M}
     *
     * @see VectorOperators#I2L
     * @see VectorOperators.Conversion#ofCast(Class,Class)
     * @see VectorSpecies#partLimit(VectorSpecies,boolean)
     * @see #viewAsFloatingLanes()
     * @see #viewAsIntegralLanes()
     * @see #convertShape(VectorOperators.Conversion,VectorSpecies,int)
     * @see #reinterpretShape(VectorSpecies,int)
     */
    public abstract <F> Vector<F> convert(VectorOperators.Conversion<E,F> conv, int part);

    /**
     * Converts this vector to a vector of the given species, shape and
     * element type, converting lane values from the current {@code ETYPE}
     * to a new lane type (called {@code FTYPE} here) according to the
     * indicated {@linkplain VectorOperators.Conversion conversion}.
     *
     * This is a lane-wise operation which copies {@code ETYPE} values
     * from the input vector to corresponding {@code FTYPE} values in
     * the result.
     *
     * <p> If the old and new species have the same shape, the behavior
     * is exactly the same as the simpler, shape-invariant method
     * {@link #convert(VectorOperators.Conversion,int) convert()}.
     * In such cases, the simpler method {@code convert()} should be
     * used, to make code easier to reason about.
     * Otherwise, this is a <em>shape-changing</em> operation, and may
     * have special implementation costs.
     *
     * <p> As a combined effect of shape changes and lane size changes,
     * the input and output species may have different lane counts, causing
     * <a href="Vector.html#expansion">expansion or contraction</a>.
     * In this case a non-zero {@code part} parameter selects
     * partial results from an expanded logical result, or steers
     * the results of a contracted logical result into a physical
     * output vector of the required output species.
     *
     * <p >The following pseudocode illustrates the behavior of this
     * method for in-place, expanding, and contracting conversions.
     * (This pseudocode also applies to the shape-invariant method,
     * but with shape restrictions on the output species.)
     * Note that only one of the three code paths is relevant to any
     * particular combination of conversion operator and shapes.
     *
     * <pre>{@code
     * FTYPE scalar_conversion_op(ETYPE s);
     * EVector a = ...;
     * VectorSpecies<F> rsp = ...;
     * int part = ...;
     * VectorSpecies<E> dsp = a.species();
     * int domlen = dsp.length();
     * int ranlen = rsp.length();
     * FTYPE[] logical = new FTYPE[domlen];
     * for (int i = 0; i < domlen; i++) {
     *   logical[i] = scalar_conversion_op(a.lane(i));
     * }
     * FTYPE[] physical;
     * if (domlen == ranlen) { // in-place
     *     assert part == 0; //else AIOOBE
     *     physical = logical;
     * } else if (domlen > ranlen) { // expanding
     *     int M = domlen / ranlen;
     *     assert 0 <= part && part < M; //else AIOOBE
     *     int origin = part * ranlen;
     *     physical = Arrays.copyOfRange(logical, origin, origin + ranlen);
     * } else { // (domlen < ranlen) // contracting
     *     int M = ranlen / domlen;
     *     assert 0 >= part && part > -M; //else AIOOBE
     *     int origin = -part * domlen;
     *     System.arraycopy(logical, 0, physical, origin, domlen);
     * }
     * return FVector.fromArray(ran, physical, 0);
     * }</pre>
     *
     * @param conv the desired scalar conversion to apply lane-wise
     * @param rsp the desired output species
     * @param part the <a href="Vector.html#expansion">part number</a>
     *        of the result, or zero if neither expanding nor contracting
     * @param <F> the boxed element type of the output species
     * @return a vector converted by element type from this vector
     * @see #convert(VectorOperators.Conversion,int)
     * @see #castShape(VectorSpecies,int)
     * @see #reinterpretShape(VectorSpecies,int)
     */
    public abstract <F> Vector<F> convertShape(VectorOperators.Conversion<E,F> conv, VectorSpecies<F> rsp, int part);

    /**
     * Convenience method for converting a vector from one lane type
     * to another, reshaping as needed when lane sizes change.
     *
     * This method returns the value of this expression:
     * {@code convertShape(conv,rsp,part)}, where {@code conv} is
     * {@code VectorOperators.Conversion.ofCast(E.class,F.class)}.
     *
     * <p> If the old and new species have different shape, this is a
     * <em>shape-changing</em> operation, and may have special
     * implementation costs.
     *
     * @param rsp the desired output species
     * @param part the <a href="Vector.html#expansion">part number</a>
     *        of the result, or zero if neither expanding nor contracting
     * @param <F> the boxed element type of the output species
     * @return a vector converted by element type from this vector
     * @see VectorOperators.Conversion#ofCast(Class,Class)
     * @see Vector#convertShape(VectorOperators.Conversion,VectorSpecies,int)
     */
    // Does this carry its weight?
    public abstract <F> Vector<F> castShape(VectorSpecies<F> rsp, int part);

    /**
     * Checks that this vector has the given element type,
     * and returns this vector unchanged.
     * The effect is similar to this pseudocode:
     * {@code elementType == species().elementType()
     *        ? this
     *        : throw new ClassCastException()}.
     *
     * @param elementType the required lane type
     * @param <F> the boxed element type of the required lane type
     * @return the same vector
     * @throws ClassCastException if the vector has the wrong element type
     * @see VectorSpecies#check(Class)
     * @see VectorMask#check(Class)
     * @see Vector#check(VectorSpecies)
     * @see VectorShuffle#check(VectorSpecies)
     */
    public abstract <F> Vector<F> check(Class<F> elementType);

    /**
     * Checks that this vector has the given species,
     * and returns this vector unchanged.
     * The effect is similar to this pseudocode:
     * {@code species == species()
     *        ? this
     *        : throw new ClassCastException()}.
     *
     * @param species the required species
     * @param <F> the boxed element type of the required species
     * @return the same vector
     * @throws ClassCastException if the vector has the wrong species
     * @see Vector#check(Class)
     * @see VectorMask#check(VectorSpecies)
     * @see VectorShuffle#check(VectorSpecies)
     */
    public abstract <F> Vector<F> check(VectorSpecies<F> species);

    //Array stores

    /**
     * Stores this vector into a byte array starting at an offset
     * using explicit byte order.
     * <p>
     * Bytes are extracted from primitive lane elements according
     * to the specified byte ordering.
     * The lanes are stored according to their
     * <a href="Vector.html#lane-order">memory ordering</a>.
     * <p>
     * This method behaves as if it calls
     * {@link #intoByteBuffer(ByteBuffer,int,ByteOrder,VectorMask)
     * intoByteBuffer()} as follows:
     * <pre>{@code
     * var bb = ByteBuffer.wrap(a);
     * var m = maskAll(true);
     * intoByteBuffer(bb, offset, bo, m);
     * }</pre>
     *
     * @param a the byte array
     * @param offset the offset into the array
     * @param bo the intended byte order
     * @throws IndexOutOfBoundsException
     *         if {@code offset+N*ESIZE < 0}
     *         or {@code offset+(N+1)*ESIZE > a.length}
     *         for any lane {@code N} in the vector
     */
    public abstract void intoByteArray(byte[] a, int offset,
                                       ByteOrder bo);

    /**
     * Stores this vector into a byte array starting at an offset
     * using explicit byte order and a mask.
     * <p>
     * Bytes are extracted from primitive lane elements according
     * to the specified byte ordering.
     * The lanes are stored according to their
     * <a href="Vector.html#lane-order">memory ordering</a>.
     * <p>
     * This method behaves as if it calls
     * {@link #intoByteBuffer(ByteBuffer,int,ByteOrder,VectorMask)
     * intoByteBuffer()} as follows:
     * <pre>{@code
     * var bb = ByteBuffer.wrap(a);
     * intoByteBuffer(bb, offset, bo, m);
     * }</pre>
     *
     * @param a the byte array
     * @param offset the offset into the array
     * @param bo the intended byte order
     * @param m the mask controlling lane selection
     * @throws IndexOutOfBoundsException
     *         if {@code offset+N*ESIZE < 0}
     *         or {@code offset+(N+1)*ESIZE > a.length}
     *         for any lane {@code N} in the vector
     *         where the mask is set
     */
    public abstract void intoByteArray(byte[] a, int offset,
                                       ByteOrder bo,
                                       VectorMask<E> m);

    /**
     * Stores this vector into a byte buffer starting at an offset
     * using explicit byte order.
     * <p>
     * Bytes are extracted from primitive lane elements according
     * to the specified byte ordering.
     * The lanes are stored according to their
     * <a href="Vector.html#lane-order">memory ordering</a>.
     * <p>
     * This method behaves as if it calls
     * {@link #intoByteBuffer(ByteBuffer,int,ByteOrder,VectorMask)
     * intoByteBuffer()} as follows:
     * <pre>{@code
     * var m = maskAll(true);
     * intoByteBuffer(bb, offset, bo, m);
     * }</pre>
     *
     * @param bb the byte buffer
     * @param offset the offset into the array
     * @param bo the intended byte order
     * @throws IndexOutOfBoundsException
     *         if {@code offset+N*ESIZE < 0}
     *         or {@code offset+(N+1)*ESIZE > bb.limit()}
     *         for any lane {@code N} in the vector
     * @throws java.nio.ReadOnlyBufferException
     *         if the byte buffer is read-only
     */
    public abstract void intoByteBuffer(ByteBuffer bb, int offset, ByteOrder bo);

    /**
     * Stores this vector into a byte buffer starting at an offset
     * using explicit byte order and a mask.
     * <p>
     * Bytes are extracted from primitive lane elements according
     * to the specified byte ordering.
     * The lanes are stored according to their
     * <a href="Vector.html#lane-order">memory ordering</a>.
     * <p>
     * The following pseudocode illustrates the behavior, where
     * the primitive element type is not of {@code byte},
     * {@code EBuffer} is the primitive buffer type, {@code ETYPE} is the
     * primitive element type, and {@code EVector} is the primitive
     * vector type for this vector:
     * <pre>{@code
     * EBuffer eb = bb.duplicate()
     *     .position(offset)
     *     .order(bo).asEBuffer();
     * ETYPE[] a = this.toArray();
     * for (int n = 0; n < a.length; n++) {
     *     if (m.laneIsSet(n)) {
     *         eb.put(n, a[n]);
     *     }
     * }
     * }</pre>
     * When the primitive element type is of {@code byte} the primitive
     * byte buffer is obtained as follows, where operation on the buffer
     * remains the same as in the prior pseudocode:
     * <pre>{@code
     * ByteBuffer eb = bb.duplicate()
     *     .position(offset);
     * }</pre>
     *
     * @implNote
     * This operation is likely to be more efficient if
     * the specified byte order is the same as
     * {@linkplain ByteOrder#nativeOrder()
     * the platform native order},
     * since this method will not need to reorder
     * the bytes of lane values.
     * In the special case where {@code ETYPE} is
     * {@code byte}, the byte order argument is
     * ignored.
     *
     * @param bb the byte buffer
     * @param offset the offset into the array
     * @param bo the intended byte order
     * @param m the mask controlling lane selection
     * @throws IndexOutOfBoundsException
     *         if {@code offset+N*ESIZE < 0}
     *         or {@code offset+(N+1)*ESIZE > bb.limit()}
     *         for any lane {@code N} in the vector
     *         where the mask is set
     * @throws java.nio.ReadOnlyBufferException
     *         if the byte buffer is read-only
     */
    public abstract void intoByteBuffer(ByteBuffer bb, int offset,
                                        ByteOrder bo, VectorMask<E> m);

    /**
     * Returns a packed array containing all the lane values.
     * The array length is the same as the vector length.
     * The element type of the array is the same as the element
     * type of the vector.
     * The array elements are stored in lane order.
     * Overrides of this method on subtypes of {@code Vector}
     * which specify the element type have an accurately typed
     * array result.
     *
     * @apiNote
     * Usually {@linkplain FloatVector#toArray() strongly typed access}
     * is preferable, if you are working with a vector
     * subtype that has a known element type.
     *
     * @return an accurately typed array containing
     *         the lane values of this vector
     * @see ByteVector#toArray()
     * @see IntVector#toArray()
     * @see DoubleVector#toArray()
     */
    public abstract Object toArray();

    /**
     * Returns an {@code int[]} array containing all
     * the lane values, converted to the type {@code int}.
     * The array length is the same as the vector length.
     * The array elements are converted as if by casting
     * and stored in lane order.
     *
     * This operation may fail if the vector element type is {@code
     * float} or {@code double}, when lanes contain fractional or
     * out-of-range values.  If any vector lane value is not
     * representable as an {@code int}, an exception is thrown.
     *
     * @apiNote
     * Usually {@linkplain FloatVector#toArray() strongly typed access}
     * is preferable, if you are working with a vector
     * subtype that has a known element type.
     *
     * @return an {@code int[]} array containing
     *         the lane values of this vector
     * @throws UnsupportedOperationException
     *         if any lane value cannot be represented as an
     *         {@code int} array element
     * @see #toArray()
     * @see #toLongArray()
     * @see #toDoubleArray()
     * @see IntVector#toArray()
     */
    public abstract int[] toIntArray();

    /**
     * Returns a {@code long[]} array containing all
     * the lane values, converted to the type {@code long}.
     * The array length is the same as the vector length.
     * The array elements are converted as if by casting
     * and stored in lane order.
     *
     * This operation may fail if the vector element type is {@code
     * float} or {@code double}, when lanes contain fractional or
     * out-of-range values.  If any vector lane value is not
     * representable as a {@code long}, an exception is thrown.
     *
     * @apiNote
     * Usually {@linkplain FloatVector#toArray() strongly typed access}
     * is preferable, if you are working with a vector
     * subtype that has a known element type.
     *
     * @return a {@code long[]} array containing
     *         the lane values of this vector
     * @throws UnsupportedOperationException
     *         if any lane value cannot be represented as a
     *         {@code long} array element
     * @see #toArray()
     * @see #toIntArray()
     * @see #toDoubleArray()
     * @see LongVector#toArray()
     */
    public abstract long[] toLongArray();

    /**
     * Returns a {@code double[]} array containing all
     * the lane values, converted to the type {@code double}.
     * The array length is the same as the vector length.
     * The array elements are converted as if by casting
     * and stored in lane order.
     * This operation can lose precision
     * if the vector element type is {@code long}.
     *
     * @apiNote
     * Usually {@link FloatVector#toArray() strongly typed access}
     * is preferable, if you are working with a vector
     * subtype that has a known element type.
     *
     * @return a {@code double[]} array containing
     *         the lane values of this vector,
     *         possibly rounded to representable
     *         {@code double} values
     * @see #toArray()
     * @see #toIntArray()
     * @see #toLongArray()
     * @see DoubleVector#toArray()
     */
    public abstract double[] toDoubleArray();

    /**
     * Returns a string representation of this vector, of the form
     * {@code "[0,1,2...]"}, reporting the lane values of this
     * vector, in lane order.
     *
     * The string is produced as if by a call to
     * {@link Arrays#toString(int[]) Arrays.toString()},
     * as appropriate to the array returned by
     * {@link #toArray() this.toArray()}.
     *
     * @return a string of the form {@code "[0,1,2...]"}
     * reporting the lane values of this vector
     */
    @Override
    public abstract String toString();

    /**
     * Indicates whether this vector is identical to some other object.
     * Two vectors are identical only if they have the same species
     * and same lane values, in the same order.
     * <p>The comparison of lane values is produced as if by a call to
     * {@link Arrays#equals(int[],int[]) Arrays.equals()},
     * as appropriate to the arrays returned by
     * {@link #toArray toArray()} on both vectors.
     *
     * @return whether this vector is identical to some other object
     * @see #eq
     */
    @Override
    public abstract boolean equals(Object obj);

    /**
     * Returns a hash code value for the vector.
     * based on the lane values and the vector species.
     *
     * @return  a hash code value for this vector
     */
    @Override
    public abstract int hashCode();

    // ==== JROSE NAME CHANGES ====

    // RAISED FROM SUBCLASSES (with generalized type)
    // * toArray() -> ETYPE[] <: Object (erased return type for interop)
    // * toString(), equals(Object), hashCode() (documented)
    // ADDED
    // * compare(OP,v) to replace most of the comparison methods
    // * maskAll(boolean) to replace maskAllTrue/False
    // * toLongArray(), toDoubleArray() (generic unboxed access)
    // * check(Class), check(VectorSpecies) (static type-safety checks)
    // * enum Comparison (enum of EQ, NE, GT, LT, GE, LE)
    // * zero(VS), broadcast(long) (basic factories)
    // * reinterpretAsEs(), viewAsXLanes (bytewise reinterpreting views)
    // * addIndex(int) (iota function)

}
