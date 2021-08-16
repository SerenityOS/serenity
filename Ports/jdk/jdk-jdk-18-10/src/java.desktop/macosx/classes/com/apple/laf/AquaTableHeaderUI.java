/*
 * Copyright (c) 2011, 2014, Oracle and/or its affiliates. All rights reserved.
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

package com.apple.laf;

import java.awt.*;
import java.util.Enumeration;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.plaf.*;
import javax.swing.plaf.basic.BasicTableHeaderUI;
import javax.swing.table.*;
import com.apple.laf.ClientPropertyApplicator;
import com.apple.laf.ClientPropertyApplicator.Property;
import com.apple.laf.AquaUtils.RecyclableSingleton;

public class AquaTableHeaderUI extends BasicTableHeaderUI {
    private int originalHeaderAlignment;
    protected int sortColumn;
    protected int sortOrder;

    public static ComponentUI createUI(final JComponent c) {
        return new AquaTableHeaderUI();
    }

    public void installDefaults() {
        super.installDefaults();

        final TableCellRenderer renderer = header.getDefaultRenderer();
        if (renderer instanceof UIResource && renderer instanceof DefaultTableCellRenderer) {
            final DefaultTableCellRenderer defaultRenderer = (DefaultTableCellRenderer)renderer;
            originalHeaderAlignment = defaultRenderer.getHorizontalAlignment();
            defaultRenderer.setHorizontalAlignment(SwingConstants.LEADING);
        }
    }

    public void uninstallDefaults() {
        final TableCellRenderer renderer = header.getDefaultRenderer();
        if (renderer instanceof UIResource && renderer instanceof DefaultTableCellRenderer) {
            final DefaultTableCellRenderer defaultRenderer = (DefaultTableCellRenderer)renderer;
            defaultRenderer.setHorizontalAlignment(originalHeaderAlignment);
        }

        super.uninstallDefaults();
    }

    private static final RecyclableSingleton<ClientPropertyApplicator<JTableHeader, JTableHeader>> TABLE_HEADER_APPLICATORS = new RecyclableSingleton<ClientPropertyApplicator<JTableHeader, JTableHeader>>() {
        @Override
        @SuppressWarnings("unchecked")
        protected ClientPropertyApplicator<JTableHeader, JTableHeader> getInstance() {
            return new ClientPropertyApplicator<JTableHeader, JTableHeader>(
                    new Property<JTableHeader>("JTableHeader.selectedColumn") {
                        public void applyProperty(final JTableHeader target, final Object value) {
                            tickle(target, value, target.getClientProperty("JTableHeader.sortDirection"));
                        }
                    },
                    new Property<JTableHeader>("JTableHeader.sortDirection") {
                        public void applyProperty(final JTableHeader target, final Object value) {
                            tickle(target, target.getClientProperty("JTableHeader.selectedColumn"), value);
                        }
                    }
            );
        }
    };
    static ClientPropertyApplicator<JTableHeader, JTableHeader> getTableHeaderApplicators() {
        return TABLE_HEADER_APPLICATORS.get();
    }

    static void tickle(final JTableHeader target, final Object selectedColumn, final Object direction) {
        final TableColumn tableColumn = getTableColumn(target, selectedColumn);
        if (tableColumn == null) return;

        int sortDirection = 0;
        if ("ascending".equalsIgnoreCase(direction+"")) {
            sortDirection = 1;
        } else if ("descending".equalsIgnoreCase(direction+"")) {
            sortDirection = -1;
        } else if ("decending".equalsIgnoreCase(direction+"")) {
            sortDirection = -1; // stupid misspelling that GM'ed in 10.5.0
        }

        final TableHeaderUI headerUI = target.getUI();
        if (headerUI == null || !(headerUI instanceof AquaTableHeaderUI)) return;

        final AquaTableHeaderUI aquaHeaderUI = (AquaTableHeaderUI)headerUI;
        aquaHeaderUI.sortColumn = tableColumn.getModelIndex();
        aquaHeaderUI.sortOrder = sortDirection;
        final AquaTableCellRenderer renderer = aquaHeaderUI.new AquaTableCellRenderer();
        tableColumn.setHeaderRenderer(renderer);
    }

    @SuppressWarnings("serial") // Superclass is not serializable across versions
    class AquaTableCellRenderer extends DefaultTableCellRenderer implements UIResource {
        public Component getTableCellRendererComponent(final JTable localTable, final Object value, final boolean isSelected, final boolean hasFocus, final int row, final int column) {
            if (localTable != null) {
                if (header != null) {
                    setForeground(header.getForeground());
                    setBackground(header.getBackground());
                    setFont(UIManager.getFont("TableHeader.font"));
                }
            }

            setText((value == null) ? "" : value.toString());

            // Modify the table "border" to draw smaller, and with the titles in the right position
            // and sort indicators, just like an NSSave/Open panel.
            final AquaTableHeaderBorder cellBorder = AquaTableHeaderBorder.getListHeaderBorder();
            cellBorder.setSortOrder(AquaTableHeaderBorder.SORT_NONE);

            if (localTable != null) {
                final boolean thisColumnSelected = localTable.getColumnModel().getColumn(column).getModelIndex() == sortColumn;

                cellBorder.setSelected(thisColumnSelected);
                if (thisColumnSelected) {
                    cellBorder.setSortOrder(sortOrder);
               }
            }

            setBorder(cellBorder);
            return this;
        }
    }

    protected static TableColumn getTableColumn(final JTableHeader target, final Object value) {
        if (value == null || !(value instanceof Integer)) return null;
        final int columnIndex = ((Integer)value).intValue();

        final TableColumnModel columnModel = target.getColumnModel();
        if (columnIndex < 0 || columnIndex >= columnModel.getColumnCount()) return null;

        return columnModel.getColumn(columnIndex);
    }

    protected static AquaTableHeaderBorder getAquaBorderFrom(final JTableHeader header, final TableColumn column) {
        final TableCellRenderer renderer = column.getHeaderRenderer();
        if (renderer == null) return null;

        final Component c = renderer.getTableCellRendererComponent(header.getTable(), column.getHeaderValue(), false, false, -1, column.getModelIndex());
        if (!(c instanceof JComponent)) return null;

        final Border border = ((JComponent)c).getBorder();
        if (!(border instanceof AquaTableHeaderBorder)) return null;

        return (AquaTableHeaderBorder)border;
    }

    protected void installListeners() {
        super.installListeners();
        getTableHeaderApplicators().attachAndApplyClientProperties(header);
    }

    protected void uninstallListeners() {
        getTableHeaderApplicators().removeFrom(header);
        super.uninstallListeners();
    }

    private int getHeaderHeightAqua() {
        int height = 0;
        boolean accomodatedDefault = false;

        final TableColumnModel columnModel = header.getColumnModel();
        for (int column = 0; column < columnModel.getColumnCount(); column++) {
            final TableColumn aColumn = columnModel.getColumn(column);
            // Configuring the header renderer to calculate its preferred size is expensive.
            // Optimise this by assuming the default renderer always has the same height.
            if (aColumn.getHeaderRenderer() != null || !accomodatedDefault) {
                final Component comp = getHeaderRendererAqua(column);
                final int rendererHeight = comp.getPreferredSize().height;
                height = Math.max(height, rendererHeight);
                // If the header value is empty (== "") in the
                // first column (and this column is set up
                // to use the default renderer) we will
                // return zero from this routine and the header
                // will disappear altogether. Avoiding the calculation
                // of the preferred size is such a performance win for
                // most applications that we will continue to
                // use this cheaper calculation, handling these
                // issues as `edge cases'.

                // Mac OS X Change - since we have a border on our renderers
                // it is possible the height of an empty header could be > 0,
                // so we chose the relatively safe number of 4 to handle this case.
                // Now if we get a size of 4 or less we assume it is empty and measure
                // a different header.
                if (rendererHeight > 4) {
                    accomodatedDefault = true;
                }
            }
        }
        return height;
    }

    private Component getHeaderRendererAqua(final int columnIndex) {
        final TableColumn aColumn = header.getColumnModel().getColumn(columnIndex);
        TableCellRenderer renderer = aColumn.getHeaderRenderer();
        if (renderer == null) {
            renderer = header.getDefaultRenderer();
        }
        return renderer.getTableCellRendererComponent(header.getTable(), aColumn.getHeaderValue(), false, false, -1, columnIndex);
    }

    private Dimension createHeaderSizeAqua(long width) {
        // None of the callers include the intercell spacing, do it here.
        if (width > Integer.MAX_VALUE) {
            width = Integer.MAX_VALUE;
        }
        return new Dimension((int)width, getHeaderHeightAqua());
    }

    /**
     * Return the minimum size of the header. The minimum width is the sum of the minimum widths of each column (plus
     * inter-cell spacing).
     */
    public Dimension getMinimumSize(final JComponent c) {
        long width = 0;
        final Enumeration<TableColumn> enumeration = header.getColumnModel().getColumns();
        while (enumeration.hasMoreElements()) {
            final TableColumn aColumn = enumeration.nextElement();
            width = width + aColumn.getMinWidth();
        }
        return createHeaderSizeAqua(width);
    }

    /**
     * Return the preferred size of the header. The preferred height is the maximum of the preferred heights of all of
     * the components provided by the header renderers. The preferred width is the sum of the preferred widths of each
     * column (plus inter-cell spacing).
     */
    public Dimension getPreferredSize(final JComponent c) {
        long width = 0;
        final Enumeration<TableColumn> enumeration = header.getColumnModel().getColumns();
        while (enumeration.hasMoreElements()) {
            final TableColumn aColumn = enumeration.nextElement();
            width = width + aColumn.getPreferredWidth();
        }
        return createHeaderSizeAqua(width);
    }
}
