/*
 * Copyright (c) 2005, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * Interfaces used to model elements of the Java programming language.
 *
 * The term "element" in this package is used to refer to program
 * elements, the declared entities that make up a program.  Elements
 * include classes, interfaces, methods, constructors, and fields.
 * The interfaces in this package do not model the structure of a
 * program inside a method body; for example there is no
 * representation of a {@code for} loop or {@code try}-{@code finally}
 * block.  However, the interfaces can model some structures only
 * appearing inside method bodies, such as local variables and
 * anonymous classes.
 *
 * <p>When used in the context of annotation processing, an accurate
 * model of the element being represented must be returned.  As this
 * is a language model, the source code provides the fiducial
 * (reference) representation of the construct in question rather than
 * a representation in an executable output like a class file.
 * Executable output may serve as the basis for creating a modeling
 * element.  However, the process of translating source code to
 * executable output may not permit recovering some aspects of the
 * source code representation.  For example, annotations with
 * {@linkplain java.lang.annotation.RetentionPolicy#SOURCE source}
 * {@linkplain java.lang.annotation.Retention retention} cannot be
 * recovered from class files and class files might not be able to
 * provide source position information.
 *
 * Names of {@linkplain
 * javax.lang.model.element.ExecutableElement#getParameters()
 * parameters} may not be recoverable from class files.
 *
 * The {@linkplain javax.lang.model.element.Modifier modifiers} on an
 * element created from a class file may differ in some cases from an
 * element for the same declaration created from a source file
 * including:
 *
 * <ul>
 * <li> {@code strictfp} on a class or interface
 * <li> {@code final} on a parameter
 * <li> {@code protected}, {@code private}, and {@code static} on classes and interfaces
 * </ul>
 *
 * Some elements which are {@linkplain
 * javax.lang.model.util.Elements.Origin#MANDATED mandated} may not be
 * marked as such when created from class files.
 *
 * Additionally, {@linkplain
 * javax.lang.model.util.Elements.Origin#SYNTHETIC synthetic}
 * constructs in a class file, such as accessor methods used in
 * implementing nested classes and {@linkplain
 * javax.lang.model.util.Elements.Origin#isBridge(ExecutableElement)
 * bridge methods} used in implementing covariant returns, are
 * translation artifacts strictly outside of this model. However, when
 * operating on class files, it is helpful be able to operate on such
 * elements, screening them out when appropriate.
 *
 * <p>During annotation processing, operating on incomplete or
 * erroneous programs is necessary; however, there are fewer
 * guarantees about the nature of the resulting model.  If the source
 * code is not syntactically well-formed or has some other
 * irrecoverable error that could not be removed by the generation of
 * new classes or interfaces, a model may or may not be provided as a
 * quality of implementation issue.  If a program is syntactically
 * valid but erroneous in some other fashion, any returned model must
 * have no less information than if all the method bodies in the
 * program were replaced by {@code "throw new RuntimeException();"}.
 * If a program refers to a missing class or interface Xyz, the
 * returned model must contain no less information than if the
 * declaration of class or interface Xyz were assumed to be {@code
 * "class Xyz {}"}, {@code "interface Xyz {}"}, {@code "enum Xyz {}"},
 * {@code "@interface Xyz {}"}, or {@code "record Xyz {}"}. If a
 * program refers to a missing class or interface {@code Xyz<K1,
 * ... ,Kn>}, the returned model must contain no less information than
 * if the declaration of Xyz were assumed to be {@code "class Xyz<T1,
 * ... ,Tn> {}"} or {@code "interface Xyz<T1, ... ,Tn> {}"}
 *
 * <p> Unless otherwise specified in a particular implementation, the
 * collections returned by methods in this package should be expected
 * to be unmodifiable by the caller and unsafe for concurrent access.
 *
 * <p> Unless otherwise specified, methods in this package will throw
 * a {@code NullPointerException} if given a {@code null} argument.
 *
 * @author Joseph D. Darcy
 * @author Scott Seligman
 * @author Peter von der Ah&eacute;
 * @see javax.lang.model.util.Elements
 * @jls 6.1 Declarations
 * @jls 7.4 Package Declarations
 * @jls 7.7 Module Declarations
 * @jls 8.1 Class Declarations
 * @jls 8.3 Field Declarations
 * @jls 8.4 Method Declarations
 * @jls 8.5 Member Class and Interface Declarations
 * @jls 8.8 Constructor Declarations
 * @jls 9.1 Interface Declarations
 * @since 1.6
 */
package javax.lang.model.element;
