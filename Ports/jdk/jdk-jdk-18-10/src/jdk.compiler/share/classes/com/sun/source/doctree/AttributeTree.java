/*
 * Copyright (c) 2011, 2020, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.source.doctree;

import java.util.List;
import javax.lang.model.element.Name;

/**
 * A tree node for an attribute in an HTML element.
 *
 * @since 1.8
 */
public interface AttributeTree extends DocTree {
    /**
     * The kind of an attribute value.
     */
    enum ValueKind {
        /** The attribute value is empty. */
        EMPTY,
        /** The attribute value is not enclosed in quotes. */
        UNQUOTED,
        /** The attribute value is enclosed in single quotation marks. */
        SINGLE,
        /** The attribute value is enclosed in double quotation marks. */
        DOUBLE
    }

    /**
     * Returns the name of the attribute.
     * @return the name of the attribute
     */
    Name getName();

    /**
     * Returns the kind of the attribute value.
     * @return the kind of the attribute value
     */
    ValueKind getValueKind();

    /**
     * Returns the value of the attribute, or {@code null} if the
     * {@linkplain #getValueKind() kind of this attribute} is {@code EMPTY}.
     * @return the value of the attribute
     */
    List<? extends DocTree> getValue();
}
