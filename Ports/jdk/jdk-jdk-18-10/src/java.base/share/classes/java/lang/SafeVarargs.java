/*
 * Copyright (c) 2010, 2015, Oracle and/or its affiliates. All rights reserved.
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

package java.lang;

import java.lang.annotation.*;

/**
 * A programmer assertion that the body of the annotated method or
 * constructor does not perform potentially unsafe operations on its
 * varargs parameter.  Applying this annotation to a method or
 * constructor suppresses unchecked warnings about a
 * <i>non-reifiable</i> variable arity (vararg) type and suppresses
 * unchecked warnings about parameterized array creation at call
 * sites.
 *
 * <p> In addition to the usage restrictions imposed by its {@link
 * Target @Target} meta-annotation, compilers are required to implement
 * additional usage restrictions on this annotation type; it is a
 * compile-time error if a method or constructor declaration is
 * annotated with a {@code @SafeVarargs} annotation, and either:
 * <ul>
 * <li>  the declaration is a fixed arity method or constructor
 *
 * <li> the declaration is a variable arity method that is neither
 * {@code static} nor {@code final} nor {@code private}.
 *
 * </ul>
 *
 * <p> Compilers are encouraged to issue warnings when this annotation
 * type is applied to a method or constructor declaration where:
 *
 * <ul>
 *
 * <li> The variable arity parameter has a reifiable element type,
 * which includes primitive types, {@code Object}, and {@code String}.
 * (The unchecked warnings this annotation type suppresses already do
 * not occur for a reifiable element type.)
 *
 * <li> The body of the method or constructor declaration performs
 * potentially unsafe operations, such as an assignment to an element
 * of the variable arity parameter's array that generates an unchecked
 * warning.  Some unsafe operations do not trigger an unchecked
 * warning.  For example, the aliasing in
 *
 * <blockquote><pre>
 * &#64;SafeVarargs // Not actually safe!
 * static void m(List&lt;String&gt;... stringLists) {
 *   Object[] array = stringLists;
 *   List&lt;Integer&gt; tmpList = Arrays.asList(42);
 *   array[0] = tmpList; // Semantically invalid, but compiles without warnings
 *   String s = stringLists[0].get(0); // Oh no, ClassCastException at runtime!
 * }
 * </pre></blockquote>
 *
 * leads to a {@code ClassCastException} at runtime.
 *
 * <p>Future versions of the platform may mandate compiler errors for
 * such unsafe operations.
 *
 * </ul>
 *
 * @since 1.7
 * @jls 4.7 Reifiable Types
 * @jls 8.4.1 Formal Parameters
 * @jls 9.6.4.7 @SafeVarargs
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.CONSTRUCTOR, ElementType.METHOD})
public @interface SafeVarargs {}
