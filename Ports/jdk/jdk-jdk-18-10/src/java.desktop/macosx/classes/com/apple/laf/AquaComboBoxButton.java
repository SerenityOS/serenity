/*
 * Copyright (c) 2011, 2015, Oracle and/or its affiliates. All rights reserved.
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

import javax.swing.*;
import javax.swing.plaf.UIResource;

import apple.laf.JRSUIState;
import apple.laf.JRSUIConstants.*;

@SuppressWarnings("serial") // Superclass is not serializable across versions
class AquaComboBoxButton extends JButton {
    protected final JComboBox<Object> comboBox;
    protected final JList<?> list;
    protected final CellRendererPane rendererPane;
    protected final AquaComboBoxUI ui;

    protected final AquaPainter<JRSUIState> painter = AquaPainter.create(JRSUIState.getInstance());
    boolean isPopDown;
    boolean isSquare;

    @SuppressWarnings("serial") // anonymous class
    protected AquaComboBoxButton(final AquaComboBoxUI ui,
                                 final JComboBox<Object> comboBox,
                                 final CellRendererPane rendererPane,
                                 final JList<?> list) {
        super("");
        putClientProperty("JButton.buttonType", "comboboxInternal");

        this.ui = ui;
        this.comboBox = comboBox;
        this.rendererPane = rendererPane;
        this.list = list;

        setModel(new DefaultButtonModel() {
            @Override
            public void setArmed(final boolean armed) {
                super.setArmed(isPressed() ? true : armed);
            }
        });

        setEnabled(comboBox.isEnabled());
    }

    @Override
    public boolean isEnabled() {
        return comboBox == null ? true : comboBox.isEnabled();
    }

    @Override
    public boolean isFocusable() {
        return false;
    }

    protected void setIsPopDown(final boolean isPopDown) {
        this.isPopDown = isPopDown;
        repaint();
    }

    protected void setIsSquare(final boolean isSquare) {
        this.isSquare = isSquare;
        repaint();
    }

    protected State getState(final ButtonModel buttonModel) {
        if (!comboBox.isEnabled()) return State.DISABLED;
        if (!AquaFocusHandler.isActive(comboBox)) return State.INACTIVE;
        if (buttonModel.isArmed()) return State.PRESSED;
        return State.ACTIVE;
    }

    @Override
    public void paintComponent(final Graphics g) {
        // Don't Paint the button as usual
        // super.paintComponent( g );
        final boolean editable = comboBox.isEditable();

        int top = 0;
        int left = 0;
        int width = getWidth();
        int height = getHeight();

        if (comboBox.isOpaque()) {
            g.setColor(getBackground());
            g.fillRect(0, 0, width, height);
        }

        final Size size = AquaUtilControlSize.getUserSizeFrom(comboBox);
        painter.state.set(size == null ? Size.REGULAR : size);

        final ButtonModel buttonModel = getModel();
        painter.state.set(getState(buttonModel));

        painter.state.set(AlignmentVertical.CENTER);

        if (AquaComboBoxUI.isTableCellEditor(comboBox)) {
            painter.state.set(AlignmentHorizontal.RIGHT);
            painter.state.set(Widget.BUTTON_POP_UP);
            painter.state.set(ArrowsOnly.YES);
            painter.paint(g, this, left, top, width, height);
            doRendererPaint(g, buttonModel, editable, getInsets(), left, top, width, height);
            return;
        }

        painter.state.set(AlignmentHorizontal.CENTER);
        final Insets insets = getInsets();
        if (!editable) {
            top += insets.top;
            left += insets.left;
            width -= insets.left + insets.right;
            height -= insets.top + insets.bottom;
        }

        if (height <= 0 || width <= 0) {
            return;
        }

        boolean hasFocus = comboBox.hasFocus();
        if (editable) {
            painter.state.set(Widget.BUTTON_COMBO_BOX);
            painter.state.set(IndicatorOnly.YES);
            painter.state.set(AlignmentHorizontal.LEFT);
            hasFocus |= comboBox.getEditor().getEditorComponent().hasFocus();
        } else {
            painter.state.set(IndicatorOnly.NO);
            painter.state.set(AlignmentHorizontal.CENTER);
            if (isPopDown) {
                painter.state.set(isSquare ? Widget.BUTTON_POP_DOWN_SQUARE : Widget.BUTTON_POP_DOWN);
            } else {
                painter.state.set(isSquare ? Widget.BUTTON_POP_UP_SQUARE : Widget.BUTTON_POP_UP);
            }
        }
        painter.state.set(hasFocus ? Focused.YES : Focused.NO);

        if (isSquare) {
            painter.paint(g, comboBox, left + 2, top - 1, width - 4, height);
        } else {
            painter.paint(g, comboBox, left, top, width, height);
        }

        // Let the renderer paint
        if (!editable && comboBox != null) {
            doRendererPaint(g, buttonModel, editable, insets, left, top, width, height);
        }
    }

    protected void doRendererPaint(final Graphics g, final ButtonModel buttonModel, final boolean editable, final Insets insets, int left, int top, int width, int height) {
        final ListCellRenderer<Object> renderer = comboBox.getRenderer();

        // fake it out! not renderPressed
        final Component c = renderer.getListCellRendererComponent(list, comboBox.getSelectedItem(), -1, false, false);
        // System.err.println("Renderer: " + renderer);

        if (!editable && !AquaComboBoxUI.isTableCellEditor(comboBox)) {
            final int indentLeft = 10;
            final int buttonWidth = 24;

            // hardcoded for now. We should adjust as necessary.
            top += 1;
            height -= 4;
            left += indentLeft;
            width -= (indentLeft + buttonWidth);
        }

        c.setFont(rendererPane.getFont());

        if (buttonModel.isArmed() && buttonModel.isPressed()) {
            if (isOpaque()) {
                c.setBackground(UIManager.getColor("Button.select"));
            }
            c.setForeground(comboBox.getForeground());
        } else if (!comboBox.isEnabled()) {
            if (isOpaque()) {
                c.setBackground(UIManager.getColor("ComboBox.disabledBackground"));
            }
            c.setForeground(UIManager.getColor("ComboBox.disabledForeground"));
        } else {
            c.setForeground(comboBox.getForeground());
            c.setBackground(comboBox.getBackground());
        }

        // Sun Fix for 4238829: should lay out the JPanel.
        boolean shouldValidate = false;
        if (c instanceof JPanel) {
            shouldValidate = true;
        }

        final int iconWidth = 0;
        final int cWidth = width - (insets.right + iconWidth);

        // fix for 3156483 we need to crop images that are too big.
        // if (height > 18)
        // always crop.
        {
            top = height / 2 - 8;
            height = 19;
        }

        // It doesn't need to draw its background, we handled it
        final Color bg = c.getBackground();
        final boolean inhibitBackground = bg instanceof UIResource;
        if (inhibitBackground) c.setBackground(new Color(0, 0, 0, 0));

        rendererPane.paintComponent(g, c, this, left, top, cWidth, height, shouldValidate); // h - (insets.top + insets.bottom) );

        if (inhibitBackground) c.setBackground(bg);

        // Remove component from renderer pane, allowing it to be gc'ed.
        rendererPane.remove(c);
    }
}
