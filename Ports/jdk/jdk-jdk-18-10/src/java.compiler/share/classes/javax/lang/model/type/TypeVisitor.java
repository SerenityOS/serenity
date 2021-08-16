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

package javax.lang.model.type;

import javax.lang.model.element.*;
import javax.lang.model.util.*;

/**
 * A visitor of types, in the style of the
 * visitor design pattern.  Classes implementing this
 * interface are used to operate on a type when the kind of
 * type is unknown at compile time.  When a visitor is passed to a
 * type's {@link TypeMirror#accept accept} method, the <code>visit<i>Xyz</i></code>
 * method most applicable to that type is invoked.
 *
 * <p> Classes implementing this interface may or may not throw a
 * {@code NullPointerException} if the additional parameter {@code p}
 * is {@code null}; see documentation of the implementing class for
 * details.
 *
 * @apiNote
 * <strong>WARNING:</strong> It is possible that methods will be added
 * to this interface to accommodate new, currently unknown, language
 * structures added to future versions of the Java programming
 * language.
 *
 * Such additions have already occurred to support language features
 * added after this API was introduced.
 *
 * Visitor classes directly implementing this interface may be source
 * incompatible with future versions of the platform.  To avoid this
 * source incompatibility, visitor implementations are encouraged to
 * instead extend the appropriate abstract visitor class that
 * implements this interface.  However, an API should generally use
 * this visitor interface as the type for parameters, return type,
 * etc. rather than one of the abstract classes.
 *
 * <p>Methods to accommodate new language constructs are expected to
 * be added as default methods to provide strong source
 * compatibility. The implementations of the default methods will in
 * turn call {@link visitUnknown visitUnknown}, behavior that will be
 * overridden in concrete visitors supporting the source version with
 * the new language construct.
 *
 * <p>There are several families of classes implementing this visitor
 * interface in the {@linkplain javax.lang.model.util util
 * package}. The families follow a naming pattern along the lines of
 * {@code FooVisitor}<i>N</i> where <i>N</i> indicates the
 * {@linkplain javax.lang.model.SourceVersion source version} the
 * visitor is appropriate for.
 *
 * In particular, a {@code FooVisitor}<i>N</i> is expected to handle
 * all language constructs present in source version <i>N</i>. If
 * there are no new language constructs added in version
 * <i>N</i>&nbsp;+&nbsp;1 (or subsequent releases), {@code
 * FooVisitor}<i>N</i> may also handle that later source version; in
 * that case, the {@link
 * javax.annotation.processing.SupportedSourceVersion
 * SupportedSourceVersion} annotation on the {@code
 * FooVisitor}<i>N</i> class will indicate a later version.
 *
 * When visiting a type representing a language construct
 * introduced <strong>after</strong> source version <i>N</i>, a {@code
 * FooVisitor}<i>N</i> will throw an {@link UnknownTypeException}
 * unless that behavior is overridden.
 *
 * <p>When choosing which member of a visitor family to subclass,
 * subclassing the most recent one increases the range of source
 * versions covered. When choosing which visitor family to subclass,
 * consider their built-in capabilities:
 *
 * <ul>
 *
 * <li>{@link AbstractTypeVisitor6 AbstractTypeVisitor}s:
 * Skeletal visitor implementations.
 *
 * <li>{@link SimpleTypeVisitor6 SimpleTypeVisitor}s: Support
 * default actions and a default return value.
 *
 * <li>{@link TypeKindVisitor6 TypeKindVisitor}s: Visit methods
 * provided on a {@linkplain TypeMirror#getKind per-kind} granularity
 * as some categories of types can have more than one kind.
 *
 * </ul>
 *
 * @param <R> the return type of this visitor's methods.  Use {@link
 *            Void} for visitors that do not need to return results.
 * @param <P> the type of the additional parameter to this visitor's
 *            methods.  Use {@code Void} for visitors that do not need an
 *            additional parameter.
 *
 * @author Joseph D. Darcy
 * @author Scott Seligman
 * @author Peter von der Ah&eacute;
 * @since 1.6
 */
public interface TypeVisitor<R, P> {
    /**
     * Visits a type.
     * @param t the type to visit
     * @param p a visitor-specified parameter
     * @return  a visitor-specified result
     */
    R visit(TypeMirror t, P p);

    /**
     * A convenience method equivalent to {@code visit(t, null)}.
     *
     * @implSpec The default implementation is {@code visit(t, null)}.
     *
     * @param t the element to visit
     * @return  a visitor-specified result
     */
    default R visit(TypeMirror t) {
        return visit(t, null);
    }

    /**
     * Visits a primitive type.
     * @param t the type to visit
     * @param p a visitor-specified parameter
     * @return  a visitor-specified result
     */
    R visitPrimitive(PrimitiveType t, P p);

    /**
     * Visits the null type.
     * @param t the type to visit
     * @param p a visitor-specified parameter
     * @return  a visitor-specified result
     */
    R visitNull(NullType t, P p);

    /**
     * Visits an array type.
     * @param t the type to visit
     * @param p a visitor-specified parameter
     * @return  a visitor-specified result
     */
    R visitArray(ArrayType t, P p);

    /**
     * Visits a declared type.
     * @param t the type to visit
     * @param p a visitor-specified parameter
     * @return  a visitor-specified result
     */
    R visitDeclared(DeclaredType t, P p);

    /**
     * Visits an error type.
     * @param t the type to visit
     * @param p a visitor-specified parameter
     * @return  a visitor-specified result
     */
    R visitError(ErrorType t, P p);

    /**
     * Visits a type variable.
     * @param t the type to visit
     * @param p a visitor-specified parameter
     * @return  a visitor-specified result
     */
    R visitTypeVariable(TypeVariable t, P p);

    /**
     * Visits a wildcard type.
     * @param t the type to visit
     * @param p a visitor-specified parameter
     * @return  a visitor-specified result
     */
    R visitWildcard(WildcardType t, P p);

    /**
     * Visits an executable type.
     * @param t the type to visit
     * @param p a visitor-specified parameter
     * @return  a visitor-specified result
     */
    R visitExecutable(ExecutableType t, P p);

    /**
     * Visits a {@link NoType} instance.
     * @param t the type to visit
     * @param p a visitor-specified parameter
     * @return  a visitor-specified result
     */
    R visitNoType(NoType t, P p);

    /**
     * Visits an unknown kind of type.
     * This can occur if the language evolves and new kinds
     * of types are added to the {@code TypeMirror} hierarchy.
     * @param t the type to visit
     * @param p a visitor-specified parameter
     * @return  a visitor-specified result
     * @throws UnknownTypeException
     *  a visitor implementation may optionally throw this exception
     */
    R visitUnknown(TypeMirror t, P p);

    /**
     * Visits a union type.
     *
     * @param t the type to visit
     * @param p a visitor-specified parameter
     * @return  a visitor-specified result
     * @since 1.7
     */
    R visitUnion(UnionType t, P p);

    /**
     * Visits an intersection type.
     *
     * @param t the type to visit
     * @param p a visitor-specified parameter
     * @return  a visitor-specified result
     * @since 1.8
     */
    R visitIntersection(IntersectionType t, P p);
}
