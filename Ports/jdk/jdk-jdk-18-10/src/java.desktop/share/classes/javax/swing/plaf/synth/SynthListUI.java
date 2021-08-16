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

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.border.Border;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.UIResource;
import javax.swing.plaf.basic.BasicListUI;

/**
 * Provides the Synth L&amp;F UI delegate for
 * {@link javax.swing.JList}.
 *
 * @author Scott Violet
 * @since 1.7
 */
public class SynthListUI extends BasicListUI
                         implements PropertyChangeListener, SynthUI {
    private SynthStyle style;
    private boolean useListColors;
    private boolean useUIBorder;

    /**
     *
     * Constructs a {@code SynthListUI}.
     */
    public SynthListUI() {}

    /**
     * Creates a new UI object for the given component.
     *
     * @param list component to create UI object for
     * @return the UI object
     */
    public static ComponentUI createUI(JComponent list) {
        return new SynthListUI();
    }

    /**
     * Notifies this UI delegate to repaint the specified component.
     * This method paints the component background, then calls
     * the {@link #paint} method.
     *
     * <p>In general, this method does not need to be overridden by subclasses.
     * All Look and Feel rendering code should reside in the {@code paint} method.
     *
     * @param g the {@code Graphics} object used for painting
     * @param c the component being painted
     * @see #paint
     */
    @Override
    public void update(Graphics g, JComponent c) {
        SynthContext context = getContext(c);

        SynthLookAndFeel.update(context, g);
        context.getPainter().paintListBackground(context,
                          g, 0, 0, c.getWidth(), c.getHeight());
        paint(g, c);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void paintBorder(SynthContext context, Graphics g, int x,
                            int y, int w, int h) {
        context.getPainter().paintListBorder(context, g, x, y, w, h);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void installListeners() {
        super.installListeners();
        list.addPropertyChangeListener(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void propertyChange(PropertyChangeEvent e) {
        if (SynthLookAndFeel.shouldUpdateStyle(e)) {
            updateStyle((JList)e.getSource());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void uninstallListeners() {
        super.uninstallListeners();
        list.removePropertyChangeListener(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void installDefaults() {
        if (list.getCellRenderer() == null ||
                 (list.getCellRenderer() instanceof UIResource)) {
            list.setCellRenderer(new SynthListCellRenderer());
        }
        updateStyle(list);
    }

    private void updateStyle(JComponent c) {
        SynthContext context = getContext(list, ENABLED);
        SynthStyle oldStyle = style;

        style = SynthLookAndFeel.updateStyle(context, this);

        if (style != oldStyle) {
            context.setComponentState(SELECTED);
            Color sbg = list.getSelectionBackground();
            if (sbg == null || sbg instanceof UIResource) {
                list.setSelectionBackground(style.getColor(
                                 context, ColorType.TEXT_BACKGROUND));
            }

            Color sfg = list.getSelectionForeground();
            if (sfg == null || sfg instanceof UIResource) {
                list.setSelectionForeground(style.getColor(
                                 context, ColorType.TEXT_FOREGROUND));
            }

            useListColors = style.getBoolean(context,
                                  "List.rendererUseListColors", true);
            useUIBorder = style.getBoolean(context,
                                  "List.rendererUseUIBorder", true);

            int height = style.getInt(context, "List.cellHeight", -1);
            if (height != -1) {
                list.setFixedCellHeight(height);
            }
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
    protected void uninstallDefaults() {
        super.uninstallDefaults();

        SynthContext context = getContext(list, ENABLED);

        style.uninstallDefaults(context);
        style = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SynthContext getContext(JComponent c) {
        return getContext(c, getComponentState(c));
    }

    private SynthContext getContext(JComponent c, int state) {
        return SynthContext.getContext(c, style, state);
    }

    private int getComponentState(JComponent c) {
        return SynthLookAndFeel.getComponentState(c);
    }

    /**
     * The {@code DefaultListCellRenderer} installed by the UI.
     */
    @SuppressWarnings("serial") // Superclass is not serializable across versions
    private class SynthListCellRenderer extends DefaultListCellRenderer.UIResource {
        @Override public String getName() {
            return "List.cellRenderer";
        }

        @Override public void setBorder(Border b) {
            if (useUIBorder || b instanceof SynthBorder) {
                super.setBorder(b);
            }
        }

        @Override public Component getListCellRendererComponent(JList<?> list, Object value,
                  int index, boolean isSelected, boolean cellHasFocus) {
            if (!useListColors && (isSelected || cellHasFocus)) {
                SynthLookAndFeel.setSelectedUI((SynthLabelUI)SynthLookAndFeel.
                             getUIOfType(getUI(), SynthLabelUI.class),
                                   isSelected, cellHasFocus, list.isEnabled(), false);
            }
            else {
                SynthLookAndFeel.resetSelectedUI();
            }

            super.getListCellRendererComponent(list, value, index,
                                               isSelected, cellHasFocus);
            return this;
        }

        @Override public void paint(Graphics g) {
            super.paint(g);
            SynthLookAndFeel.resetSelectedUI();
        }
    }
}
