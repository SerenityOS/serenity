/*
 * Copyright (c) 2002, 2020, Oracle and/or its affiliates. All rights reserved.
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

package javax.swing.plaf.synth;

import java.awt.Component;
import java.awt.Graphics;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.RowSorter;
import javax.swing.border.Border;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.UIResource;
import javax.swing.plaf.basic.BasicTableHeaderUI;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;

import sun.swing.table.DefaultTableCellHeaderRenderer;

/**
 * Provides the Synth L&amp;F UI delegate for
 * {@link javax.swing.table.JTableHeader}.
 *
 * @author Alan Chung
 * @author Philip Milne
 * @since 1.7
 */
public class SynthTableHeaderUI extends BasicTableHeaderUI
                                implements PropertyChangeListener, SynthUI {

//
// Instance Variables
//

    private TableCellRenderer prevRenderer = null;

    private SynthStyle style;

    /**
     *
     * Constructs a {@code SynthTableHeaderUI}.
     */
    public SynthTableHeaderUI() {}

    /**
     * Creates a new UI object for the given component.
     *
     * @param h component to create UI object for
     * @return the UI object
     */
    public static ComponentUI createUI(JComponent h) {
        return new SynthTableHeaderUI();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void installDefaults() {
        prevRenderer = header.getDefaultRenderer();
        if (prevRenderer instanceof UIResource) {
            header.setDefaultRenderer(new HeaderRenderer());
        }
        updateStyle(header);
    }

    private void updateStyle(JTableHeader c) {
        SynthContext context = getContext(c, ENABLED);
        SynthStyle oldStyle = style;
        style = SynthLookAndFeel.updateStyle(context, this);
        if (style != oldStyle) {
            if (oldStyle != null) {
                uninstallKeyboardActions();
                installKeyboardActions();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void installListeners() {
        super.installListeners();
        header.addPropertyChangeListener(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void uninstallDefaults() {
        if (header.getDefaultRenderer() instanceof HeaderRenderer) {
            header.setDefaultRenderer(prevRenderer);
        }

        SynthContext context = getContext(header, ENABLED);

        style.uninstallDefaults(context);
        style = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void uninstallListeners() {
        header.removePropertyChangeListener(this);
        super.uninstallListeners();
    }

    /**
     * Notifies this UI delegate to repaint the specified component.
     * This method paints the component background, then calls
     * the {@link #paint(SynthContext,Graphics)} method.
     *
     * <p>In general, this method does not need to be overridden by subclasses.
     * All Look and Feel rendering code should reside in the {@code paint} method.
     *
     * @param g the {@code Graphics} object used for painting
     * @param c the component being painted
     * @see #paint(SynthContext,Graphics)
     */
    @Override
    public void update(Graphics g, JComponent c) {
        SynthContext context = getContext(c);

        SynthLookAndFeel.update(context, g);
        context.getPainter().paintTableHeaderBackground(context,
                          g, 0, 0, c.getWidth(), c.getHeight());
        paint(context, g);
    }

    /**
     * Paints the specified component according to the Look and Feel.
     * <p>This method is not used by Synth Look and Feel.
     * Painting is handled by the {@link #paint(SynthContext,Graphics)} method.
     *
     * @param g the {@code Graphics} object used for painting
     * @param c the component being painted
     * @see #paint(SynthContext,Graphics)
     */
    @Override
    public void paint(Graphics g, JComponent c) {
        SynthContext context = getContext(c);

        paint(context, g);
    }

    /**
     * Paints the specified component.
     *
     * @param context context for the component being painted
     * @param g the {@code Graphics} object used for painting
     * @see #update(Graphics,JComponent)
     */
    protected void paint(SynthContext context, Graphics g) {
        super.paint(g, context.getComponent());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void paintBorder(SynthContext context, Graphics g, int x,
                            int y, int w, int h) {
        context.getPainter().paintTableHeaderBorder(context, g, x, y, w, h);
    }
//
// SynthUI
//
    /**
     * {@inheritDoc}
     */
    @Override
    public SynthContext getContext(JComponent c) {
        return getContext(c, SynthLookAndFeel.getComponentState(c));
    }

    private SynthContext getContext(JComponent c, int state) {
        return SynthContext.getContext(c, style, state);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void rolloverColumnUpdated(int oldColumn, int newColumn) {
        header.repaint(header.getHeaderRect(oldColumn));
        header.repaint(header.getHeaderRect(newColumn));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (SynthLookAndFeel.shouldUpdateStyle(evt)) {
            updateStyle((JTableHeader)evt.getSource());
        }
    }

    /**
     * The {@code DefaultTableCellHeaderRenderer} installed by the UI.
     */
    @SuppressWarnings("serial") // Superclass is not serializable across versions
    private class HeaderRenderer extends DefaultTableCellHeaderRenderer {
        HeaderRenderer() {
            setHorizontalAlignment(JLabel.LEADING);
            setName("TableHeader.renderer");
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected,
                                                       boolean hasFocus,
                                                       int row, int column) {

            boolean hasRollover = (column == getRolloverColumn());
            if (isSelected || hasRollover || hasFocus) {
                boolean enabled = (table == null)? true : table.isEnabled();
                SynthLookAndFeel.setSelectedUI((SynthLabelUI)SynthLookAndFeel.
                             getUIOfType(getUI(), SynthLabelUI.class),
                             isSelected, hasFocus, enabled,
                             hasRollover);
            } else {
                SynthLookAndFeel.resetSelectedUI();
            }

            //stuff a variable into the client property of this renderer indicating the sort order,
            //so that different rendering can be done for the header based on sorted state.
            RowSorter<?> rs = table == null ? null : table.getRowSorter();
            java.util.List<? extends RowSorter.SortKey> sortKeys = rs == null ? null : rs.getSortKeys();
            if (sortKeys != null && sortKeys.size() > 0 && sortKeys.get(0).getColumn() ==
                    table.convertColumnIndexToModel(column)) {
                switch(sortKeys.get(0).getSortOrder()) {
                    case ASCENDING:
                        putClientProperty("Table.sortOrder", "ASCENDING");
                        break;
                    case DESCENDING:
                        putClientProperty("Table.sortOrder", "DESCENDING");
                        break;
                    case UNSORTED:
                        putClientProperty("Table.sortOrder", "UNSORTED");
                        break;
                    default:
                        throw new AssertionError("Cannot happen");
                }
            } else {
                putClientProperty("Table.sortOrder", "UNSORTED");
            }

            super.getTableCellRendererComponent(table, value, isSelected,
                                                hasFocus, row, column);

            return this;
        }

        @Override
        public void setBorder(Border border) {
            if (border instanceof SynthBorder) {
                super.setBorder(border);
            }
        }
    }
}
