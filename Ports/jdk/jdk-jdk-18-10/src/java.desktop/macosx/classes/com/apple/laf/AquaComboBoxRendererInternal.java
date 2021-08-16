/*
 * Copyright (c) 2013, 2019, Oracle and/or its affiliates. All rights reserved.
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

import sun.swing.SwingUtilities2;

import javax.swing.*;
import java.awt.*;

@SuppressWarnings("serial") // Superclass is not serializable across versions
class AquaComboBoxRendererInternal<E> extends JLabel implements ListCellRenderer<E> {
    final JComboBox<?> fComboBox;
    boolean fSelected;
    boolean fChecked;
    boolean fInList;
    boolean fEditable;
    boolean fDrawCheckedItem = true;

    // Provides space for a checkbox, and is translucent
    public AquaComboBoxRendererInternal(final JComboBox<?> comboBox) {
        super();
        fComboBox = comboBox;
    }

    // Don't include checkIcon space, because this is also used for button size calculations
    // - the popup-size calc will get checkIcon space from getInsets
    public Dimension getPreferredSize() {
        // From BasicComboBoxRenderer - trick to avoid zero-height items
        final Dimension size;

        final String text = getText();
        if (text == null || text.isEmpty()) {
            setText(" ");
            size = super.getPreferredSize();
            setText("");
        } else {
            size = super.getPreferredSize();
        }
        return size;
    }

    // Don't paint the border here, it gets painted by the UI
    protected void paintBorder(final Graphics g) {

    }

    public int getBaseline(int width, int height) {
        return super.getBaseline(width, height) - 1;
    }

    // Really means is the one with the mouse over it
    public Component getListCellRendererComponent(final JList<? extends E> list,
                                                  final E value, int index,
                                                  final boolean isSelected,
                                                  final boolean cellHasFocus) {
        fInList = (index >= 0); // When the button wants the item painted, it passes in -1
        fSelected = isSelected;
        if (index < 0) {
            index = fComboBox.getSelectedIndex();
        }

        // changed this to not ask for selected index but directly compare the current item and selected item
        // different from basic because basic has no concept of checked, just has the last one selected,
        // and the user changes selection. We have selection and a check mark.
        // we used to call fComboBox.getSelectedIndex which ends up being a very bad call for large checkboxes
        // it does a linear compare of every object in the checkbox until it finds the selected one, so if
        // we have a 5000 element list we will 5000 * (selected index) .equals() of objects.
        // See Radar #3141307

        // Fix for Radar # 3204287 where we ask for an item at a negative index!
        if (index >= 0) {
            final Object item = fComboBox.getItemAt(index);
            fChecked = fInList && item != null && item.equals(fComboBox.getSelectedItem());
        } else {
            fChecked = false;
        }

        fEditable = fComboBox.isEditable();
        if (isSelected) {
            if (fEditable) {
                setBackground(UIManager.getColor("List.selectionBackground"));
                setForeground(UIManager.getColor("List.selectionForeground"));
            } else {
                setBackground(list.getSelectionBackground());
                setForeground(list.getSelectionForeground());
            }
        } else {
            if (fEditable) {
                setBackground(UIManager.getColor("List.background"));
                setForeground(UIManager.getColor("List.foreground"));
            } else {
                setBackground(list.getBackground());
                setForeground(list.getForeground());
            }
        }

        setFont(list.getFont());

        if (value instanceof Icon) {
            setIcon((Icon)value);
        } else {
            setText((value == null) ? " " : value.toString());
        }
        return this;
    }

    public Insets getInsets(Insets insets) {
        if (insets == null) insets = new Insets(0, 0, 0, 0);
        insets.top = 1;
        insets.bottom = 1;
        insets.right = 5;
        insets.left = (fInList && !fEditable ? 16 + 7 : 5);
        return insets;
    }

    protected void setDrawCheckedItem(final boolean drawCheckedItem) {
        this.fDrawCheckedItem = drawCheckedItem;
    }

    // Paint this component, and a checkbox if it's the selected item and not in the button
    protected void paintComponent(final Graphics g) {
        if (fInList) {
            if (fSelected && !fEditable) {
                AquaMenuPainter.instance().paintSelectedMenuItemBackground(g, getWidth(), getHeight());
            } else {
                g.setColor(getBackground());
                g.fillRect(0, 0, getWidth(), getHeight());
            }

            if (fChecked && !fEditable && fDrawCheckedItem) {
                final int y = getHeight() - 4;
                g.setColor(getForeground());
                SwingUtilities2.drawString(fComboBox, g, "\u2713", 6, y);
            }
        }
        super.paintComponent(g);
    }
}
