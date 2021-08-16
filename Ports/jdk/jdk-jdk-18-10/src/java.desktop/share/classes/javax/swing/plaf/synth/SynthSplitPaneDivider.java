/*
 * Copyright (c) 2003, 2014, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.*;
import java.beans.*;
import javax.swing.*;
import javax.swing.plaf.basic.*;
import sun.swing.DefaultLookup;

/**
 * Synth's SplitPaneDivider.
 *
 * @author Scott Violet
 */
@SuppressWarnings("serial") // Superclass is not serializable across versions
class SynthSplitPaneDivider extends BasicSplitPaneDivider {
    public SynthSplitPaneDivider(BasicSplitPaneUI ui) {
        super(ui);
    }

    protected void setMouseOver(boolean mouseOver) {
        if (isMouseOver() != mouseOver) {
            repaint();
        }
        super.setMouseOver(mouseOver);
    }

    public void propertyChange(PropertyChangeEvent e) {
        super.propertyChange(e);
        if (e.getSource() == splitPane) {
            if (e.getPropertyName() == JSplitPane.ORIENTATION_PROPERTY) {
                if (leftButton instanceof SynthArrowButton) {
                    ((SynthArrowButton)leftButton).setDirection(
                                       mapDirection(true));
                }
                if (rightButton instanceof SynthArrowButton) {
                    ((SynthArrowButton)rightButton).setDirection(
                                       mapDirection(false));
                }
            }
        }
    }

    public void paint(Graphics g) {
        Graphics g2 = g.create();

        SynthContext context = ((SynthSplitPaneUI)splitPaneUI).getContext(
                               splitPane, Region.SPLIT_PANE_DIVIDER);
        Rectangle bounds = getBounds();
        bounds.x = bounds.y = 0;
        SynthLookAndFeel.updateSubregion(context, g, bounds);
        context.getPainter().paintSplitPaneDividerBackground(context,
                          g, 0, 0, bounds.width, bounds.height,
                          splitPane.getOrientation());

        SynthPainter foreground = null;

        context.getPainter().paintSplitPaneDividerForeground(context, g, 0, 0,
                getWidth(), getHeight(), splitPane.getOrientation());

        // super.paint(g2);
        for (int counter = 0; counter < getComponentCount(); counter++) {
            Component child = getComponent(counter);
            Rectangle childBounds = child.getBounds();
            Graphics childG = g.create(childBounds.x, childBounds.y,
                                       childBounds.width, childBounds.height);
            child.paint(childG);
            childG.dispose();
        }
        g2.dispose();
    }

    private int mapDirection(boolean isLeft) {
        if (isLeft) {
            if (splitPane.getOrientation() == JSplitPane.HORIZONTAL_SPLIT){
                return SwingConstants.WEST;
            }
            return SwingConstants.NORTH;
        }
        if (splitPane.getOrientation() == JSplitPane.HORIZONTAL_SPLIT){
            return SwingConstants.EAST;
        }
        return SwingConstants.SOUTH;
    }


    /**
     * Creates and return an instance of JButton that can be used to
     * collapse the left component in the split pane.
     */
    protected JButton createLeftOneTouchButton() {
        SynthArrowButton b = new SynthArrowButton(SwingConstants.NORTH);
        int oneTouchSize = lookupOneTouchSize();

        b.setName("SplitPaneDivider.leftOneTouchButton");
        b.setMinimumSize(new Dimension(oneTouchSize, oneTouchSize));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setRequestFocusEnabled(false);
        b.setDirection(mapDirection(true));
        return b;
    }

    private int lookupOneTouchSize() {
        return DefaultLookup.getInt(splitPaneUI.getSplitPane(), splitPaneUI,
              "SplitPaneDivider.oneTouchButtonSize", ONE_TOUCH_SIZE);
    }

    /**
     * Creates and return an instance of JButton that can be used to
     * collapse the right component in the split pane.
     */
    protected JButton createRightOneTouchButton() {
        SynthArrowButton b = new SynthArrowButton(SwingConstants.NORTH);
        int oneTouchSize = lookupOneTouchSize();

        b.setName("SplitPaneDivider.rightOneTouchButton");
        b.setMinimumSize(new Dimension(oneTouchSize, oneTouchSize));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setRequestFocusEnabled(false);
        b.setDirection(mapDirection(false));
        return b;
    }
}
