/*
 * Copyright (c) 2008, 2015, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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
 *
 */
package com.sun.hotspot.igv.filterwindow;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JCheckBox;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

/**
 * @author Thomas Wuerthinger
 */
public class CheckRenderer extends JCheckBox implements ListCellRenderer<Object> {

    private JList<Object> list;
    private Color startBackground;

    public CheckRenderer(final JList<Object> list) {
        this.list = list;
        list.addMouseListener(
                new MouseAdapter() {

                    @Override
                    public void mouseClicked(MouseEvent e) {
                        int index = list.locationToIndex(e.getPoint());
                        Point p2 = list.indexToLocation(index);
                        Rectangle r = new Rectangle(p2.x, p2.y, getPreferredSize().height, getPreferredSize().height);
                        if (r.contains(e.getPoint())) {
                            CheckNode node = ((CheckNodeListModel) list.getModel()).getCheckNodeAt(index);
                            node.setSelected(!node.isSelected());
                            list.repaint();
                            e.consume();
                        }
                    }
                });

        this.setPreferredSize(new Dimension(getPreferredSize().width, getPreferredSize().height - 5));
        startBackground = this.getBackground();
    }

    @Override
    public Component getListCellRendererComponent(final JList<? extends Object> list, Object value, final int index, boolean isSelected, boolean cellHasFocus) {
        setText(value.toString());
        CheckNode node = ((CheckNodeListModel) list.getModel()).getCheckNodeAt(index);
        this.setSelected(node.isSelected());
        this.setEnabled(list.isEnabled());

        if (isSelected && list.hasFocus()) {
            this.setBackground(list.getSelectionBackground());
            this.setForeground(list.getSelectionForeground());
        } else if (isSelected) {
            assert !list.hasFocus();
            this.setBackground(startBackground);
            this.setForeground(list.getForeground());

        } else {
            this.setBackground(list.getBackground());
            this.setForeground(list.getForeground());
        }
        return this;
    }
}
