/*
 * Copyright (c) 2007, 2016, Oracle and/or its affiliates. All rights reserved.
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
 */
package com.sun.swingset3.demos;

import java.awt.*;
import javax.swing.*;

/**
 * JGridPanel
 *
 * @author Pavel Porvatov
 */
public class JGridPanel extends JPanel {

    public static final int DEFAULT_GAP = 5;

    public enum Layout {
        FIRST,
        CENTER,
        LAST,
        FILL
    }

    private enum ComponentType {
        NO_RESIZABLE,
        HORIZONTAL_FILL,
        FULL
    }

    private final int cols;

    private int bigCol = -1;

    private int bigRow = -1;

    private int vGap = -1;

    private int hGap = -1;

    public JGridPanel(int cols) {
        this(cols, -1, -1);
    }

    public JGridPanel(int cols, int bigCol) {
        this(cols, bigCol, -1);
    }

    public JGridPanel(int cols, int bigCol, int bigRow) {
        super(new GridBagLayout());

        assert cols > 0;
        assert bigCol >= -1 && bigCol < cols;
        assert bigRow >= -1;

        this.cols = cols;
        this.bigCol = bigCol;
        this.bigRow = bigRow;
    }

    public void setVGap(int vGap) {
        this.vGap = vGap;
    }

    public void setHGap(int hGap) {
        this.hGap = hGap;
    }

    public JGridPanel cell() {
        return cell(null, getHLayout(null), getVLayout(null), null);
    }

    public JGridPanel cell(Component component) {
        return cell(component, getHLayout(component), getVLayout(component), null);
    }

    public JGridPanel cell(Component component, Layout hLayout) {
        return cell(component, hLayout, getVLayout(component), null);
    }

    public JGridPanel cell(Component component, Layout hLayout, Layout vLayout) {
        return cell(component, hLayout, vLayout, null);
    }

    public JGridPanel cell(Component component, Insets insets) {
        assert insets != null;

        return cell(component, getHLayout(component), getVLayout(component), insets);
    }

    private JGridPanel cell(Component component, Layout hLayout, Layout vLayout, Insets insets) {
        int componentCount = getComponentCount();
        int x = componentCount % cols;
        int y = componentCount / cols;

        int weightx = x == bigCol || (bigCol < 0 && hLayout == Layout.FILL) ? 1 : 0;
        int weighty = y == bigRow || (bigRow < 0 && vLayout == Layout.FILL) ? 1 : 0;

        if (insets == null) {
            int topGap = y == 0 ? 0 : vGap;
            int leftGap = x == 0 ? 0 : hGap;

            insets = new Insets(topGap < 0 ? DEFAULT_GAP : topGap,
                    leftGap < 0 ? DEFAULT_GAP : leftGap, 0, 0);
        }

        add(component == null ? createSeparator() : component,
                new GridBagConstraints(x, y,
                        1, 1,
                        weightx, weighty,
                        getAnchor(hLayout, vLayout), getFill(hLayout, vLayout),
                        insets, 0, 0));

        return this;
    }

    public void setComponent(Component component, int col, int row) {
        assert col >= 0 && col < cols;
        assert row >= 0;

        GridBagLayout layout = (GridBagLayout) getLayout();

        for (int i = 0; i < getComponentCount(); i++) {
            Component oldComponent = getComponent(i);

            GridBagConstraints constraints = layout.getConstraints(oldComponent);

            if (constraints.gridx == col && constraints.gridy == row) {
                remove(i);

                add(component == null ? createSeparator() : component, constraints);

                validate();
                repaint();

                return;
            }
        }

        // Cell not found
        assert false;
    }

    private static JComponent createSeparator() {
        return new JLabel();
    }

    private static int getFill(Layout hLayout, Layout vLayout) {
        if (hLayout == Layout.FILL) {
            return vLayout == Layout.FILL ? GridBagConstraints.BOTH : GridBagConstraints.HORIZONTAL;
        }

        return vLayout == Layout.FILL ? GridBagConstraints.VERTICAL : GridBagConstraints.NONE;
    }

    private static ComponentType getComponentType(Component component) {
        if (component == null
                || component instanceof JLabel
                || component instanceof JRadioButton
                || component instanceof JCheckBox
                || component instanceof JButton) {
            return ComponentType.NO_RESIZABLE;
        }

        if (component instanceof JComboBox
                || component instanceof JTextField) {
            return ComponentType.HORIZONTAL_FILL;
        }

        return ComponentType.FULL;
    }

    private static Layout getHLayout(Component component) {
        if (getComponentType(component) == ComponentType.NO_RESIZABLE) {
            return Layout.FIRST;
        } else {
            return Layout.FILL;
        }
    }

    private static Layout getVLayout(Component component) {
        if (getComponentType(component) == ComponentType.FULL) {
            return Layout.FILL;
        } else {
            return Layout.CENTER;
        }
    }

    private static final int[][] ANCHORS = new int[][]{
        {GridBagConstraints.NORTHWEST, GridBagConstraints.NORTH, GridBagConstraints.NORTHEAST},
        {GridBagConstraints.WEST, GridBagConstraints.CENTER, GridBagConstraints.EAST},
        {GridBagConstraints.SOUTHWEST, GridBagConstraints.SOUTH, GridBagConstraints.SOUTHEAST}
    };

    private static int getAnchorIndex(Layout layout) {
        if (layout == Layout.CENTER) {
            return 1;
        } else if (layout == Layout.LAST) {
            return 2;
        } else {
            return 0;
        }
    }

    private static int getAnchor(Layout hLayout, Layout vLayout) {
        return ANCHORS[getAnchorIndex(vLayout)][getAnchorIndex(hLayout)];
    }

    public void setBorderEqual(int border) {
        setBorder(BorderFactory.createEmptyBorder(border, border, border, border));
    }
}
