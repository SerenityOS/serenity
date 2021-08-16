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

package jdk.javadoc.internal.doclets.toolkit;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ElementVisitor;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.PackageElement;
import javax.lang.model.type.TypeMirror;
import javax.tools.FileObject;
import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Set;

public interface DocletElement extends Element {

    @Override
    default TypeMirror asType() {
        throw new UnsupportedOperationException("Unsupported method");
    }

    @Override
    default ElementKind getKind() {
        return ElementKind.OTHER;
    }

    @Override
    default Set<Modifier> getModifiers() {
        throw new UnsupportedOperationException("Unsupported method");
    }

    @Override
    default Name getSimpleName() {
        throw new UnsupportedOperationException("Unsupported method");
    }

    @Override
    default Element getEnclosingElement() {
        throw new UnsupportedOperationException("Unsupported method");
    }

    @Override
    default List<? extends Element> getEnclosedElements() {
        throw new UnsupportedOperationException("Unsupported method");
    }

    @Override
    default List<? extends AnnotationMirror> getAnnotationMirrors() {
        throw new UnsupportedOperationException("Unsupported method");
    }

    @Override
    default <A extends Annotation> A getAnnotation(Class<A> annotationType) {
        throw new UnsupportedOperationException("Unsupported method");
    }

    @Override
    default <R, P> R accept(ElementVisitor<R, P> v, P p) {
        return v.visitUnknown(this, p);
    }

    @Override
    default <A extends Annotation> A[] getAnnotationsByType(Class<A> annotationType) {
        throw new UnsupportedOperationException("Unsupported method");
    }

    /**
     * Returns the anchoring package element, in the case of a
     * module element, this is the module's unnamed package.
     *
     * @return the anchor element.
     */
    PackageElement getPackageElement();

    /**
     * Returns the file object associated with this special
     * element such as {@code overview.html}, {@code doc-files/foo.html}.
     * @return the file object
     */
    FileObject getFileObject();

    /**
     * Returns the subkind of this element.
     * @return a subkind
     */
    Kind getSubKind();

    /**
     * Sub kind enums that this element supports.
     */
    enum Kind {
        OVERVIEW, DOCFILE;
    }
}
