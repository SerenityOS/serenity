/*
 * Copyright (c) 2000, 2017, Oracle and/or its affiliates. All rights reserved.
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

package javax.print.attribute;

import java.io.Serializable;

/**
 * Interface {@code Attribute} is the base interface implemented by any and
 * every printing attribute class to indicate that the class represents a
 * printing attribute. All printing attributes are serializable.
 *
 * @author David Mendenhall
 * @author Alan Kaminsky
 */
public interface Attribute extends Serializable {

    /**
     * Get the printing attribute class which is to be used as the "category"
     * for this printing attribute value when it is added to an attribute set.
     *
     * @return printing attribute class (category), an instance of class
     *         {@link Class java.lang.Class}
     */
    public Class<? extends Attribute> getCategory();

    /**
     * Get the name of the category of which this attribute value is an
     * instance.
     * <p>
     * <i>Note:</i> This method is intended to provide a default, nonlocalized
     * string for the attribute's category. If two attribute objects return the
     * same category from the {@code getCategory()} method, they should return
     * the same name from the {@code getName()} method.
     *
     * @return attribute category name
     */
    public String getName();
}
