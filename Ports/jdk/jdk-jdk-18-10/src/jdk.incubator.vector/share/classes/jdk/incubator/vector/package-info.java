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

/**
 * {@Incubating}
 * <p>
 * This package provides
 * classes to express vector computations that, given suitable hardware
 * and runtime ability, are accelerated using vector hardware instructions.
 *
 * <p> A {@linkplain Vector <em>vector</em>} is a
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
 * <p>
 * Here is an example of multiplying elements of two float arrays
 * {@code a} and {@code b} using vector computation
 * and storing result in array {@code c}.
 *
 * <pre>{@code
 * static final VectorSpecies<Float> SPECIES = FloatVector.SPECIES_PREFERRED;
 *
 * void vectorMultiply(float[] a, float[] b, float[] c) {
 *   // It is assumed array arguments are of the same size
 *   for (int i = 0; i < a.length; i += SPECIES.length()) {
 *         VectorMask<Float> m = SPECIES.indexInRange(i, a.length);
 *         FloatVector va = FloatVector.fromArray(SPECIES, a, i, m);
 *         FloatVector vb = FloatVector.fromArray(SPECIES, b, i, m);
 *         FloatVector vc = va.mul(vb)
 *         vc.intoArray(c, i, m);
 *   }
 * }
 * }</pre>
 *
 * In the above example, we use masks, generated by
 * {@link VectorSpecies#indexInRange indexInRange()},
 * to prevent reading/writing past the array length.
 * The first {@code a.length / SPECIES.length()} iterations will have a mask
 * with all lanes set. Only the final iteration (if {@code a.length}
 * is not a multiple of {@code SPECIES.length()} will have a mask with
 * the first {@code a.length % SPECIES.length()} lanes set.
 *
 * Since a mask is used in all iterations, the above implementation
 * may not achieve optimal performance (for large array lengths). The
 * same computation can be implemented without masks as follows:
 *
 * <pre>{@code
 * static final VectorSpecies<Float> SPECIES = FloatVector.SPECIES_PREFERRED;
 *
 * void vectorMultiply(float[] a, float[] b, float[] c) {
 *   int i = 0;
 *   // It is assumed array arguments are of the same size
 *   for (; i < SPECIES.loopBound(a.length); i += SPECIES.length()) {
 *         FloatVector va = FloatVector.fromArray(SPECIES, a, i);
 *         FloatVector vb = FloatVector.fromArray(SPECIES, b, i);
 *         FloatVector vc = va.mul(vb)
 *         vc.intoArray(c, i);
 *   }
 *
 *   for (; i < a.length; i++) {
 *     c[i] = a[i] * b[i];
 *   }
 * }
 * }</pre>
 *
 * The scalar computation after the vector computation is required to
 * process a <em>tail</em> of {@code TLENGTH} array elements, where
 * {@code TLENGTH < SPECIES.length()} for the vector species.
 *
 * The examples above use the preferred species ({@code FloatVector.SPECIES_PREFERRED}),
 * ensuring code dynamically adapts to optimal shape for the platform
 * on which it runs.
 *
 * <p> The helper method {@link VectorSpecies#loopBound(int) loopBound()}
 * is used in the above code to find the end of the vector loop.
 * A primitive masking expression such as
 * {@code (a.length & ~(SPECIES.length() - 1))} might also be used
 * here, since {@code SPECIES.length()} is known to be 8, which
 * is a power of two.  But this is not always a correct assumption.
 * For example, if the {@code FloatVector.SPECIES_PREFERRED} turns
 * out to have the platform-dependent shape
 * {@link VectorShape#S_Max_BIT S_Max_BIT},
 * and that shape has some odd hypothetical size such as 384 (which is
 * a valid vector size according to some architectures), then the
 * hand-tweaked primitive masking expression may produce surprising
 * results.
 *
 * <h2> Performance notes </h2>
 *
 * This package depends on the runtime's ability to dynamically
 * compile vector operations into optimal vector hardware
 * instructions. There is a default scalar implementation for each
 * operation which is used if the operation cannot be compiled to
 * vector instructions.
 *
 * <p>There are certain things users need to pay attention to for
 * generating optimal vector machine code:
 *
 * <ul>

 * <li> The shape of vectors used should be supported by the underlying
 * platform. For example, code written using {@link IntVector} of
 * {@link VectorShape} {@link VectorShape#S_512_BIT S_512_BIT} will not be
 * compiled to vector instructions on a platform which supports only
 * 256 bit vectors. Instead, the default scalar implementation will be
 * used.  For this reason, it is recommended to use the preferred
 * species as shown above to write generically sized vector
 * computations.
 *
 * <li> Most classes defined in this package should be treated as
 * <a href="{@docRoot}/java.base/java/lang/doc-files/ValueBased.html">value-based</a> classes.
 * This classification applies to {@link Vector} and its subtypes,
 * {@link VectorMask}, {@link VectorShuffle}, and {@link VectorSpecies}.
 *
 * With these types,
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
 * fields or in array elements, while semantically valid, will may
 * incur performance risks.
 * <!-- The preceding paragraph is shared verbatim
 *   -- between Vector.java and package-info.java -->
 *
 * </ul>
 *
 * <p>
 * For every class in this package,
 * unless specified otherwise, any method arguments of reference
 * type must not be null, and any null argument will elicit a
 * {@code NullPointerException}.  This fact is not individually
 * documented for methods of this API.
 */
package jdk.incubator.vector;
