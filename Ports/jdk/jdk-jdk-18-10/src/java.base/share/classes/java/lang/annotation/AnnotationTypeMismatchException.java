/*
 * Copyright (c) 2003, 2019, Oracle and/or its affiliates. All rights reserved.
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

package java.lang.annotation;
import java.lang.reflect.Method;

/**
 * Thrown to indicate that a program has attempted to access an element of
 * an annotation whose type has changed after the annotation was compiled
 * (or serialized).
 * This exception can be thrown by the {@linkplain
 * java.lang.reflect.AnnotatedElement API used to read annotations
 * reflectively}.
 *
 * @author  Josh Bloch
 * @see     java.lang.reflect.AnnotatedElement
 * @since 1.5
 */
public class AnnotationTypeMismatchException extends RuntimeException {
    @java.io.Serial
    private static final long serialVersionUID = 8125925355765570191L;

    /**
     * The {@code Method} object for the annotation element.
     */
    private final transient Method element;

    /**
     * The (erroneous) type of data found in the annotation.  This string
     * may, but is not required to, contain the value as well.  The exact
     * format of the string is unspecified.
     */
    private final String foundType;

    /**
     * Constructs an AnnotationTypeMismatchException for the specified
     * annotation type element and found data type.
     *
     * @param element the {@code Method} object for the annotation
     * element, may be {@code null}
     * @param foundType the (erroneous) type of data found in the annotation.
     *        This string may, but is not required to, contain the value
     *        as well.  The exact format of the string is unspecified,
     *        may be {@code null}.
     */
    public AnnotationTypeMismatchException(Method element, String foundType) {
        super("Incorrectly typed data found for annotation element " + element
              + " (Found data of type " + foundType + ")");
        this.element = element;
        this.foundType = foundType;
    }

    /**
     * Returns the {@code Method} object for the incorrectly typed element.
     * The value may be unavailable if this exception has been
     * serialized and then read back in.
     *
     * @return the {@code Method} object for the incorrectly typed
     * element, or {@code null} if unavailable
     */
    public Method element() {
        return this.element;
    }

    /**
     * Returns the type of data found in the incorrectly typed element.
     * The returned string may, but is not required to, contain the value
     * as well.  The exact format of the string is unspecified and the string
     * may be {@code null}.
     *
     * @return the type of data found in the incorrectly typed element
     */
    public String foundType() {
        return this.foundType;
    }
}
