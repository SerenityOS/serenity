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

package javax.lang.model.element;


import java.util.List;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.*;

/**
 * A visitor of the values of annotation interface elements, using a
 * variant of the visitor design pattern.  Unlike a standard visitor
 * which dispatches based on the concrete type of a member of a type
 * hierarchy, this visitor dispatches based on the type of data
 * stored; there are no distinct subclasses for storing, for example,
 * {@code boolean} values versus {@code int} values.  Classes
 * implementing this interface are used to operate on a value when the
 * type of that value is unknown at compile time.  When a visitor is
 * passed to a value's {@link AnnotationValue#accept accept} method,
 * the <code>visit<i>Xyz</i></code> method applicable to that value is
 * invoked.
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
 * Such additions have already occurred in another visitor interface in
 * this package to support language features added after this API was
 * introduced.
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
 * be added as default methods to provide strong source compatibility,
 * as done for {@link ElementVisitor#visitModule visitModule} in
 * {@code ElementVisitor}. The implementations of the default methods
 * in this interface will in turn call {@link visitUnknown
 * visitUnknown}, behavior that will be overridden in concrete
 * visitors supporting the source version with the new language
 * construct.
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
 * When visiting an annotation value representing a language construct
 * introduced <strong>after</strong> source version <i>N</i>, a {@code
 * FooVisitor}<i>N</i> will throw an {@link
 * UnknownAnnotationValueException} unless that behavior is overridden.
 *
 * <p>When choosing which member of a visitor family to subclass,
 * subclassing the most recent one increases the range of source
 * versions covered. When choosing which visitor family to subclass,
 * consider their built-in capabilities:
 *
 * <ul>
 *
 * <li>{@link AbstractAnnotationValueVisitor6
 * AbstractAnnotationValueVisitor}s: Skeletal visitor implementations.
 *
 * <li>{@link SimpleAnnotationValueVisitor6
 * SimpleAnnotationValueVisitor}s: Support default actions and a
 * default return value.
 *
 * </ul>
 *
 * @param <R> the return type of this visitor's methods
 * @param <P> the type of the additional parameter to this visitor's methods.
 * @author Joseph D. Darcy
 * @author Scott Seligman
 * @author Peter von der Ah&eacute;
 * @since 1.6
 */
public interface AnnotationValueVisitor<R, P> {
    /**
     * Visits an annotation value.
     * @param av the value to visit
     * @param p a visitor-specified parameter
     * @return  a visitor-specified result
     */
    R visit(AnnotationValue av, P p);

    /**
     * A convenience method equivalent to {@code visit(av, null)}.
     *
     * @implSpec The default implementation is {@code visit(av, null)}.
     *
     * @param av the value to visit
     * @return  a visitor-specified result
     */
    default R visit(AnnotationValue av) {
        return visit(av, null);
    }

    /**
     * Visits a {@code boolean} value in an annotation.
     * @param b the value being visited
     * @param p a visitor-specified parameter
     * @return the result of the visit
     */
    R visitBoolean(boolean b, P p);

    /**
     * Visits a {@code byte} value in an annotation.
     * @param  b the value being visited
     * @param  p a visitor-specified parameter
     * @return the result of the visit
     */
    R visitByte(byte b, P p);

    /**
     * Visits a {@code char} value in an annotation.
     * @param  c the value being visited
     * @param  p a visitor-specified parameter
     * @return the result of the visit
     */
    R visitChar(char c, P p);

    /**
     * Visits a {@code double} value in an annotation.
     * @param  d the value being visited
     * @param  p a visitor-specified parameter
     * @return the result of the visit
     */
    R visitDouble(double d, P p);

    /**
     * Visits a {@code float} value in an annotation.
     * @param  f the value being visited
     * @param  p a visitor-specified parameter
     * @return the result of the visit
     */
    R visitFloat(float f, P p);

    /**
     * Visits an {@code int} value in an annotation.
     * @param  i the value being visited
     * @param  p a visitor-specified parameter
     * @return the result of the visit
     */
    R visitInt(int i, P p);

    /**
     * Visits a {@code long} value in an annotation.
     * @param  i the value being visited
     * @param  p a visitor-specified parameter
     * @return the result of the visit
     */
    R visitLong(long i, P p);

    /**
     * Visits a {@code short} value in an annotation.
     * @param  s the value being visited
     * @param  p a visitor-specified parameter
     * @return the result of the visit
     */
    R visitShort(short s, P p);

    /**
     * Visits a string value in an annotation.
     * @param  s the value being visited
     * @param  p a visitor-specified parameter
     * @return the result of the visit
     */
    R visitString(String s, P p);

    /**
     * Visits a type value in an annotation.
     * @param  t the value being visited
     * @param  p a visitor-specified parameter
     * @return the result of the visit
     */
    R visitType(TypeMirror t, P p);

    /**
     * Visits an {@code enum} value in an annotation.
     * @param  c the value being visited
     * @param  p a visitor-specified parameter
     * @return the result of the visit
     */
    R visitEnumConstant(VariableElement c, P p);

    /**
     * Visits an annotation value in an annotation.
     * @param  a the value being visited
     * @param  p a visitor-specified parameter
     * @return the result of the visit
     */
    R visitAnnotation(AnnotationMirror a, P p);

    /**
     * Visits an array value in an annotation.
     * @param  vals the value being visited
     * @param  p a visitor-specified parameter
     * @return the result of the visit
     */
    R visitArray(List<? extends AnnotationValue> vals, P p);

    /**
     * Visits an unknown kind of annotation value.
     * This can occur if the language evolves and new kinds
     * of value can be stored in an annotation.
     * @param  av the unknown value being visited
     * @param  p a visitor-specified parameter
     * @return the result of the visit
     * @throws UnknownAnnotationValueException
     *  a visitor implementation may optionally throw this exception
     */
    R visitUnknown(AnnotationValue av, P p);
}
