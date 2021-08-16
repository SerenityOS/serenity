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

import java.util.Map;
import javax.lang.model.type.DeclaredType;

/**
 * Represents an annotation.  An annotation associates a value with
 * each element of an annotation interface.
 *
 * <p> Annotations should be compared using the {@code equals}
 * method.  There is no guarantee that any particular annotation will
 * always be represented by the same object.
 *
 * @author Joseph D. Darcy
 * @author Scott Seligman
 * @author Peter von der Ah&eacute;
 * @since 1.6
 */
public interface AnnotationMirror {

    /**
     * {@return the type of this annotation}
     */
    DeclaredType getAnnotationType();

    /**
     * Returns the values of this annotation's elements.
     * This is returned in the form of a map that associates elements
     * with their corresponding values.
     * Only those elements with values explicitly present in the
     * annotation are included, not those that are implicitly assuming
     * their default values.
     * The order of the map matches the order in which the
     * values appear in the annotation's source.
     *
     * <p>Note that an annotation mirror of a marker annotation type
     * will by definition have an empty map.
     *
     * <p>To fill in default values, use {@link
     * javax.lang.model.util.Elements#getElementValuesWithDefaults
     * getElementValuesWithDefaults}.
     *
     * @return the values of this annotation's elements,
     *          or an empty map if there are none
     */
    Map<? extends ExecutableElement, ? extends AnnotationValue> getElementValues();
}
