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

package javax.lang.model.util;


import javax.lang.model.element.*;

import static javax.lang.model.SourceVersion.*;
import javax.lang.model.SourceVersion;
import javax.annotation.processing.SupportedSourceVersion;

/**
 * A skeletal visitor for annotation values with default behavior
 * appropriate for the {@link SourceVersion#RELEASE_6 RELEASE_6}
 * source version.
 *
 * @apiNote
 * <p id=note_for_subclasses><strong>WARNING:</strong> The {@code
 * AnnotationValueVisitor} interface implemented by this class may
 * have methods added to it in the future to accommodate new,
 * currently unknown, language structures added to future versions of
 * the Java programming language.  Therefore, methods whose
 * names begin with {@code "visit"} may be added to this class in the
 * future; to avoid incompatibilities, classes and subclasses which
 * extend this class should not declare any instance methods with
 * names beginning with {@code "visit"}.</p>
 *
 * <p>When such a new visit method is added, the default
 * implementation in this class will be to directly or indirectly call
 * the {@link #visitUnknown visitUnknown} method.  A new abstract
 * annotation value visitor class will also be introduced to
 * correspond to the new language level; this visitor will have
 * different default behavior for the visit method in question.  When
 * a new visitor is introduced, portions of this visitor class may be
 * deprecated, including its constructors.
 *
 * @param <R> the return type of this visitor's methods
 * @param <P> the type of the additional parameter to this visitor's methods.
 *
 * @author Joseph D. Darcy
 * @author Scott Seligman
 * @author Peter von der Ah&eacute;
 *
 * @see AbstractAnnotationValueVisitor7
 * @see AbstractAnnotationValueVisitor8
 * @see AbstractAnnotationValueVisitor9
 * @see AbstractAnnotationValueVisitor14
 * @since 1.6
 */
@SupportedSourceVersion(RELEASE_6)
public abstract class AbstractAnnotationValueVisitor6<R, P>
    implements AnnotationValueVisitor<R, P> {

    /**
     * Constructor for concrete subclasses to call.
     * @deprecated Release 6 is obsolete; update to a visitor for a newer
     * release level.
     */
    @Deprecated(since="9")
    protected AbstractAnnotationValueVisitor6() {}

    /**
     * Visits any annotation value as if by passing itself to that
     * value's {@link AnnotationValue#accept accept}.  The invocation
     * {@code v.visit(av, p)} is equivalent to {@code av.accept(v, p)}.
     * @param av {@inheritDoc}
     * @param p  {@inheritDoc}
     * @return   {@inheritDoc}
     */
    public final R visit(AnnotationValue av, P p) {
        return av.accept(this, p);
    }

    /**
     * Visits an annotation value as if by passing itself to that
     * value's {@link AnnotationValue#accept accept} method passing
     * {@code null} for the additional parameter.  The invocation
     * {@code v.visit(av)} is equivalent to {@code av.accept(v,
     * null)}.
     * @param av {@inheritDoc}
     * @return   {@inheritDoc}
     */
    public final R visit(AnnotationValue av) {
        return av.accept(this, null);
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec The default implementation of this method in {@code
     * AbstractAnnotationValueVisitor6} will always throw {@code
     * new UnknownAnnotationValueException(av, p)}.  This behavior is not
     * required of a subclass.
     *
     * @param av {@inheritDoc}
     * @param p  {@inheritDoc}
     * @return   {@inheritDoc}
     */
    @Override
    public R visitUnknown(AnnotationValue av, P p) {
        throw new UnknownAnnotationValueException(av, p);
    }
}
