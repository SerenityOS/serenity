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

import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.type.*;
import static javax.lang.model.SourceVersion.*;

/**
 * A visitor of types based on their {@linkplain TypeKind kind} with
 * default behavior appropriate for the {@link SourceVersion#RELEASE_6
 * RELEASE_6} source version.  For {@linkplain
 * TypeMirror types} <code><i>Xyz</i></code> that may have more than one
 * kind, the <code>visit<i>Xyz</i></code> methods in this class delegate
 * to the <code>visit<i>Xyz</i>As<i>Kind</i></code> method corresponding to the
 * first argument's kind.  The <code>visit<i>Xyz</i>As<i>Kind</i></code> methods
 * call {@link #defaultAction defaultAction}, passing their arguments
 * to {@code defaultAction}'s corresponding parameters.
 *
 * @apiNote
 * Methods in this class may be overridden subject to their general
 * contract.
 *
 * <p id=note_for_subclasses><strong>WARNING:</strong> The {@code
 * TypeVisitor} interface implemented by this class may have methods
 * added to it or the {@link TypeKind TypeKind enum} used in this
 * class may have constants added to it in the future to accommodate
 * new, currently unknown, language structures added to future
 * versions of the Java programming language.  Therefore,
 * methods whose names begin with {@code "visit"} may be added to this
 * class in the future; to avoid incompatibilities, classes and
 * subclasses which extend this class should not declare any instance
 * methods with names beginning with {@code "visit"}.</p>
 *
 * <p>When such a new visit method is added, the default
 * implementation in this class will be to directly or indirectly call
 * the {@link #visitUnknown visitUnknown} method.  A new type kind
 * visitor class will also be introduced to correspond to the new
 * language level; this visitor will have different default behavior
 * for the visit method in question.  When a new visitor is
 * introduced, portions of this visitor class may be deprecated,
 * including its constructors.
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
 *
 * @see TypeKindVisitor7
 * @see TypeKindVisitor8
 * @see TypeKindVisitor9
 * @see TypeKindVisitor14
 * @since 1.6
 */
@SupportedSourceVersion(RELEASE_6)
public class TypeKindVisitor6<R, P> extends SimpleTypeVisitor6<R, P> {
    /**
     * Constructor for concrete subclasses to call; uses {@code null}
     * for the default value.
     * @deprecated Release 6 is obsolete; update to a visitor for a newer
     * release level.
     */
    @Deprecated(since="9")
    protected TypeKindVisitor6() {
        super(null);
    }


    /**
     * Constructor for concrete subclasses to call; uses the argument
     * for the default value.
     *
     * @param defaultValue the value to assign to {@link #DEFAULT_VALUE}
     * @deprecated Release 6 is obsolete; update to a visitor for a newer
     * release level.
     */
    @Deprecated(since="9")
    protected TypeKindVisitor6(R defaultValue) {
        super(defaultValue);
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation dispatches to the visit method for
     * the specific {@linkplain TypeKind kind} of primitive type:
     * {@code BOOLEAN}, {@code BYTE}, etc.
     *
     * @param t {@inheritDoc}
     * @param p {@inheritDoc}
     * @return  the result of the kind-specific visit method
     */
    @Override
    public R visitPrimitive(PrimitiveType t, P p) {
        TypeKind k = t.getKind();
        switch (k) {
        case BOOLEAN:
            return visitPrimitiveAsBoolean(t, p);

        case BYTE:
            return visitPrimitiveAsByte(t, p);

        case SHORT:
            return visitPrimitiveAsShort(t, p);

        case INT:
            return visitPrimitiveAsInt(t, p);

        case LONG:
            return visitPrimitiveAsLong(t, p);

        case CHAR:
            return visitPrimitiveAsChar(t, p);

        case FLOAT:
            return visitPrimitiveAsFloat(t, p);

        case DOUBLE:
            return visitPrimitiveAsDouble(t, p);

        default:
            throw new AssertionError("Bad kind " + k + " for PrimitiveType" + t);
        }
    }

    /**
     * Visits a {@code BOOLEAN} primitive type.
     *
     * @implSpec This implementation calls {@code defaultAction}.
     *
     * @param t the type to visit
     * @param p a visitor-specified parameter
     * @return  the result of {@code defaultAction}
     */
    public R visitPrimitiveAsBoolean(PrimitiveType t, P p) {
        return defaultAction(t, p);
    }

    /**
     * Visits a {@code BYTE} primitive type.
     *
     * @implSpec This implementation calls {@code defaultAction}.
     *
     * @param t the type to visit
     * @param p a visitor-specified parameter
     * @return  the result of {@code defaultAction}
     */
    public R visitPrimitiveAsByte(PrimitiveType t, P p) {
        return defaultAction(t, p);
    }

    /**
     * Visits a {@code SHORT} primitive type.
     *
     * @implSpec This implementation calls {@code defaultAction}.
     *
     * @param t the type to visit
     * @param p a visitor-specified parameter
     * @return  the result of {@code defaultAction}
     */
    public R visitPrimitiveAsShort(PrimitiveType t, P p) {
        return defaultAction(t, p);
    }

    /**
     * Visits an {@code INT} primitive type.
     *
     * @implSpec This implementation calls {@code defaultAction}.
     *
     * @param t the type to visit
     * @param p a visitor-specified parameter
     * @return  the result of {@code defaultAction}
     */
    public R visitPrimitiveAsInt(PrimitiveType t, P p) {
        return defaultAction(t, p);
    }

    /**
     * Visits a {@code LONG} primitive type.
     *
     * @implSpec This implementation calls {@code defaultAction}.
     *
     * @param t the type to visit
     * @param p a visitor-specified parameter
     * @return  the result of {@code defaultAction}
     */
    public R visitPrimitiveAsLong(PrimitiveType t, P p) {
        return defaultAction(t, p);
    }

    /**
     * Visits a {@code CHAR} primitive type.
     *
     * @implSpec This implementation calls {@code defaultAction}.
     *
     * @param t the type to visit
     * @param p a visitor-specified parameter
     * @return  the result of {@code defaultAction}
     */
    public R visitPrimitiveAsChar(PrimitiveType t, P p) {
        return defaultAction(t, p);
    }

    /**
     * Visits a {@code FLOAT} primitive type.
     *
     * @implSpec This implementation calls {@code defaultAction}.
     *
     * @param t the type to visit
     * @param p a visitor-specified parameter
     * @return  the result of {@code defaultAction}
     */
    public R visitPrimitiveAsFloat(PrimitiveType t, P p) {
        return defaultAction(t, p);
    }

    /**
     * Visits a {@code DOUBLE} primitive type.
     *
     * @implSpec This implementation calls {@code defaultAction}.
     *
     * @param t the type to visit
     * @param p a visitor-specified parameter
     * @return  the result of {@code defaultAction}
     */
    public R visitPrimitiveAsDouble(PrimitiveType t, P p) {
        return defaultAction(t, p);
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation dispatches to the visit method for
     * the specific {@linkplain TypeKind kind} of pseudo-type:
     * {@code VOID}, {@code PACKAGE}, {@code MODULE}, or {@code NONE}.
     *
     * @param t {@inheritDoc}
     * @param p {@inheritDoc}
     * @return  the result of the kind-specific visit method
     */
    @Override
    public R visitNoType(NoType t, P p) {
        TypeKind k = t.getKind();
        switch (k) {
        case VOID:
            return visitNoTypeAsVoid(t, p);

        case PACKAGE:
            return visitNoTypeAsPackage(t, p);

        case MODULE:
            return visitNoTypeAsModule(t, p);

        case NONE:
            return visitNoTypeAsNone(t, p);

        default:
            throw new AssertionError("Bad kind " + k + " for NoType" + t);
        }
    }

    /**
     * Visits a {@link TypeKind#VOID VOID} pseudo-type.
     *
     * @implSpec This implementation calls {@code defaultAction}.
     *
     * @param t the type to visit
     * @param p a visitor-specified parameter
     * @return  the result of {@code defaultAction}
     */
    public R visitNoTypeAsVoid(NoType t, P p) {
        return defaultAction(t, p);
    }

    /**
     * Visits a {@link TypeKind#PACKAGE PACKAGE} pseudo-type.
     *
     * @implSpec This implementation calls {@code defaultAction}.
     *
     * @param t the type to visit
     * @param p a visitor-specified parameter
     * @return  the result of {@code defaultAction}
     */
    public R visitNoTypeAsPackage(NoType t, P p) {
        return defaultAction(t, p);
    }

    /**
     * Visits a {@link TypeKind#MODULE MODULE} pseudo-type.
     *
     * @implSpec This implementation calls {@code visitUnknown}.
     *
     * @param t the type to visit
     * @param p a visitor-specified parameter
     * @return  the result of {@code visitUnknown}
     *
     * @since 10
     */
    public R visitNoTypeAsModule(NoType t, P p) {
        return visitUnknown(t, p);
    }

    /**
     * Visits a {@link TypeKind#NONE NONE} pseudo-type.
     *
     * @implSpec This implementation calls {@code defaultAction}.
     *
     * @param t the type to visit
     * @param p a visitor-specified parameter
     * @return  the result of {@code defaultAction}
     */
    public R visitNoTypeAsNone(NoType t, P p) {
        return defaultAction(t, p);
    }
}
