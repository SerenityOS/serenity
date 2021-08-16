/*
 * Copyright (c) 1997, 2014, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.Component;
import javax.swing.*;

/**
 * This interface defines the method required by any object that
 * would like to be a renderer for cells in a <code>JTable</code>.
 *
 * @author Alan Chung
 */

public interface TableCellRenderer {

    /**
     * Returns the component used for drawing the cell.  This method is
     * used to configure the renderer appropriately before drawing.
     * <p>
     * The <code>TableCellRenderer</code> is also responsible for rendering the
     * the cell representing the table's current DnD drop location if
     * it has one. If this renderer cares about rendering
     * the DnD drop location, it should query the table directly to
     * see if the given row and column represent the drop location:
     * <pre>
     *     JTable.DropLocation dropLocation = table.getDropLocation();
     *     if (dropLocation != null
     *             &amp;&amp; !dropLocation.isInsertRow()
     *             &amp;&amp; !dropLocation.isInsertColumn()
     *             &amp;&amp; dropLocation.getRow() == row
     *             &amp;&amp; dropLocation.getColumn() == column) {
     *
     *         // this cell represents the current drop location
     *         // so render it specially, perhaps with a different color
     *     }
     * </pre>
     * <p>
     * During a printing operation, this method will be called with
     * <code>isSelected</code> and <code>hasFocus</code> values of
     * <code>false</code> to prevent selection and focus from appearing
     * in the printed output. To do other customization based on whether
     * or not the table is being printed, check the return value from
     * {@link javax.swing.JComponent#isPaintingForPrint()}.
     *
     * @param   table           the <code>JTable</code> that is asking the
     *                          renderer to draw; can be <code>null</code>
     * @param   value           the value of the cell to be rendered.  It is
     *                          up to the specific renderer to interpret
     *                          and draw the value.  For example, if
     *                          <code>value</code>
     *                          is the string "true", it could be rendered as a
     *                          string or it could be rendered as a check
     *                          box that is checked.  <code>null</code> is a
     *                          valid value
     * @param   isSelected      true if the cell is to be rendered with the
     *                          selection highlighted; otherwise false
     * @param   hasFocus        if true, render cell appropriately.  For
     *                          example, put a special border on the cell, if
     *                          the cell can be edited, render in the color used
     *                          to indicate editing
     * @param   row             the row index of the cell being drawn.  When
     *                          drawing the header, the value of
     *                          <code>row</code> is -1
     * @param   column          the column index of the cell being drawn
     *
     * @return                  the component used for drawing the cell.
     *
     * @see javax.swing.JComponent#isPaintingForPrint()
     */
    Component getTableCellRendererComponent(JTable table, Object value,
                                            boolean isSelected, boolean hasFocus,
                                            int row, int column);
}
