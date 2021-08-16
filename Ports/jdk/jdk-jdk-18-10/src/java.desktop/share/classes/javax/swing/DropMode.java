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
package javax.swing;

/**
 * Drop modes, used to determine the method by which a component
 * tracks and indicates a drop location during drag and drop.
 *
 * @author Shannon Hickey
 * @see JTable#setDropMode
 * @see JList#setDropMode
 * @see JTree#setDropMode
 * @see javax.swing.text.JTextComponent#setDropMode
 * @since 1.6
 */
public enum DropMode {

    /**
     * A component's own internal selection mechanism (or caret for text
     * components) should be used to track the drop location.
     */
    USE_SELECTION,

    /**
     * The drop location should be tracked in terms of the index of
     * existing items. Useful for dropping on items in tables, lists,
     * and trees.
     */
    ON,

    /**
     * The drop location should be tracked in terms of the position
     * where new data should be inserted. For components that manage
     * a list of items (list and tree for example), the drop location
     * should indicate the index where new data should be inserted.
     * For text components the location should represent a position
     * between characters. For components that manage tabular data
     * (table for example), the drop location should indicate
     * where to insert new rows, columns, or both, to accommodate
     * the dropped data.
     */
    INSERT,

    /**
     * The drop location should be tracked in terms of the row index
     * where new rows should be inserted to accommodate the dropped
     * data. This is useful for components that manage tabular data.
     */
    INSERT_ROWS,

    /**
     * The drop location should be tracked in terms of the column index
     * where new columns should be inserted to accommodate the dropped
     * data. This is useful for components that manage tabular data.
     */
    INSERT_COLS,

    /**
     * This mode is a combination of <code>ON</code>
     * and <code>INSERT</code>, specifying that data can be
     * dropped on existing items, or in insert locations
     * as specified by <code>INSERT</code>.
     */
    ON_OR_INSERT,

    /**
     * This mode is a combination of <code>ON</code>
     * and <code>INSERT_ROWS</code>, specifying that data can be
     * dropped on existing items, or as insert rows
     * as specified by <code>INSERT_ROWS</code>.
     */
    ON_OR_INSERT_ROWS,

    /**
     * This mode is a combination of <code>ON</code>
     * and <code>INSERT_COLS</code>, specifying that data can be
     * dropped on existing items, or as insert columns
     * as specified by <code>INSERT_COLS</code>.
     */
    ON_OR_INSERT_COLS
}
