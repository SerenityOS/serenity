/*
 * Copyright (c) 2005, Oracle and/or its affiliates. All rights reserved.
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
package javax.swing.table;

/**
 * TableStringConverter is used to convert objects from the model into
 * strings.  This is useful in filtering and searching when the model returns
 * objects that do not have meaningful <code>toString</code> implementations.
 *
 * @since 1.6
 */
public abstract class TableStringConverter {
    /**
     * Constructor for subclasses to call.
     */
    protected TableStringConverter() {}

    /**
     * Returns the string representation of the value at the specified
     * location.
     *
     * @param model the <code>TableModel</code> to fetch the value from
     * @param row the row the string is being requested for
     * @param column the column the string is being requested for
     * @return the string representation.  This should never return null.
     * @throws NullPointerException if <code>model</code> is null
     * @throws IndexOutOfBoundsException if the arguments are outside the
     *         bounds of the model
     */
    public abstract String toString(TableModel model, int row, int column);
}
