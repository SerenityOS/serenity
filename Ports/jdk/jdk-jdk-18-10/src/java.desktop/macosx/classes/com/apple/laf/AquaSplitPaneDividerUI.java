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
import java.beans.PropertyChangeEvent;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.plaf.basic.BasicSplitPaneDivider;

import apple.laf.*;
import apple.laf.JRSUIConstants.State;

import com.apple.laf.AquaUtils.LazyKeyedSingleton;
import com.apple.laf.AquaUtils.RecyclableSingleton;
import com.apple.laf.AquaUtils.RecyclableSingletonFromDefaultConstructor;

@SuppressWarnings("serial") // Superclass is not serializable across versions
public class AquaSplitPaneDividerUI extends BasicSplitPaneDivider {
    final AquaPainter<JRSUIState> painter = AquaPainter.create(JRSUIStateFactory.getSplitPaneDivider());

    public AquaSplitPaneDividerUI(final AquaSplitPaneUI ui) {
        super(ui);
        setLayout(new AquaSplitPaneDividerUI.DividerLayout());
    }

    /**
     * Property change event, presumably from the JSplitPane, will message
     * updateOrientation if necessary.
     */
    public void propertyChange(final PropertyChangeEvent e) {
        if (e.getSource() == splitPane) {
            final String propName = e.getPropertyName();
            if ("enabled".equals(propName)) {
                final boolean enabled = splitPane.isEnabled();
                if (leftButton != null) leftButton.setEnabled(enabled);
                if (rightButton != null) rightButton.setEnabled(enabled);
            } else if (JSplitPane.ORIENTATION_PROPERTY.equals(propName)) {
                // need to regenerate the buttons, since we bake the orientation into them
                if (rightButton  != null) {
                    remove(rightButton); rightButton = null;
                }
                if (leftButton != null) {
                    remove(leftButton); leftButton = null;
                }
                oneTouchExpandableChanged();
            }
        }
        super.propertyChange(e);
    }

    public int getMaxDividerSize() {
        return 10;
    }

    /**
     * Paints the divider.
     */
    public void paint(final Graphics g) {
        final Dimension size = getSize();
        int x = 0;
        int y = 0;

        final boolean horizontal = splitPane.getOrientation() == SwingConstants.HORIZONTAL;
        //System.err.println("Size = " + size + " orientation horiz = " + horizontal);
        // size determines orientation
        final int maxSize = getMaxDividerSize();
        boolean doPaint = true;
        if (horizontal) {
            if (size.height > maxSize) {
                final int diff = size.height - maxSize;
                y = diff / 2;
                size.height = maxSize;
            }
            if (size.height < 4) doPaint = false;
        } else {
            if (size.width > maxSize) {
                final int diff = size.width - maxSize;
                x = diff / 2;
                size.width = maxSize;
            }
            if (size.width < 4) doPaint = false;
        }

        if (doPaint) {
            painter.state.set(getState());
            painter.paint(g, splitPane, x, y, size.width, size.height);
        }

        super.paint(g); // Ends up at Container.paint, which paints our JButton children
    }

    protected State getState() {
        return splitPane.isEnabled() ? State.ACTIVE : State.DISABLED;
    }

    protected JButton createLeftOneTouchButton() {
        return createButtonForDirection(getDirection(true));
    }

    protected JButton createRightOneTouchButton() {
        return createButtonForDirection(getDirection(false));
    }

    static final LazyKeyedSingleton<Integer, Image> directionArrows = new LazyKeyedSingleton<Integer, Image>() {
        protected Image getInstance(final Integer direction) {
            final Image arrowImage = AquaImageFactory.getArrowImageForDirection(direction);
            final int h = (arrowImage.getHeight(null) * 5) / 7;
            final int w = (arrowImage.getWidth(null) * 5) / 7;
            return AquaUtils.generateLightenedImage(arrowImage.getScaledInstance(w, h, Image.SCALE_SMOOTH), 50);
        }
    };

    // separate static, because the divider needs to be serializable
    // see <rdar://problem/7590946> JSplitPane is not serializable when using Aqua look and feel
    static JButton createButtonForDirection(final int direction) {
        final JButton button = new JButton(new ImageIcon(directionArrows.get(Integer.valueOf(direction))));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        button.setFocusPainted(false);
        button.setRequestFocusEnabled(false);
        button.setFocusable(false);
        button.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
        return button;
    }

    int getDirection(final boolean isLeft) {
        if (splitPane.getOrientation() == JSplitPane.HORIZONTAL_SPLIT) {
            return isLeft ? SwingConstants.WEST : SwingConstants.EAST;
        }

        return isLeft ? SwingConstants.NORTH : SwingConstants.SOUTH;
    }

    static final int kMaxPopupArrowSize = 9;
    protected class DividerLayout extends BasicSplitPaneDivider.DividerLayout {
        public void layoutContainer(final Container c) {
            final int maxSize = getMaxDividerSize();
            final Dimension size = getSize();

            if (leftButton == null || rightButton == null || c != AquaSplitPaneDividerUI.this) return;

            if (!splitPane.isOneTouchExpandable()) {
                leftButton.setBounds(-5, -5, 1, 1);
                rightButton.setBounds(-5, -5, 1, 1);
                return;
            }

            final int blockSize = Math.min(getDividerSize(), kMaxPopupArrowSize); // make it 1 less than divider, or kMaxPopupArrowSize

            // put them at the right or the bottom
            if (orientation == JSplitPane.VERTICAL_SPLIT) {
                int yPosition = 0;
                if (size.height > maxSize) {
                    final int diff = size.height - maxSize;
                    yPosition = diff / 2;
                }
                int xPosition = kMaxPopupArrowSize + ONE_TOUCH_OFFSET;

                rightButton.setBounds(xPosition, yPosition, kMaxPopupArrowSize, blockSize);

                xPosition -= (kMaxPopupArrowSize + ONE_TOUCH_OFFSET);
                leftButton.setBounds(xPosition, yPosition, kMaxPopupArrowSize, blockSize);
            } else {
                int xPosition = 0;
                if (size.width > maxSize) {
                    final int diff = size.width - maxSize;
                    xPosition = diff / 2;
                }
                int yPosition = kMaxPopupArrowSize + ONE_TOUCH_OFFSET;

                rightButton.setBounds(xPosition, yPosition, blockSize, kMaxPopupArrowSize);

                yPosition -= (kMaxPopupArrowSize + ONE_TOUCH_OFFSET);
                leftButton.setBounds(xPosition, yPosition, blockSize, kMaxPopupArrowSize);
            }
        }
    }

    public static Border getHorizontalSplitDividerGradientVariant() {
        return HorizontalSplitDividerGradientPainter.instance();
    }

    static class HorizontalSplitDividerGradientPainter implements Border {
        private static final RecyclableSingleton<HorizontalSplitDividerGradientPainter> instance = new RecyclableSingletonFromDefaultConstructor<HorizontalSplitDividerGradientPainter>(HorizontalSplitDividerGradientPainter.class);
        static HorizontalSplitDividerGradientPainter instance() {
            return instance.get();
        }

        final Color startColor = Color.white;
        final Color endColor = new Color(217, 217, 217);
        final Color borderLines = Color.lightGray;

        public Insets getBorderInsets(final Component c) {
            return new Insets(0, 0, 0, 0);
        }

        public boolean isBorderOpaque() {
            return true;
        }

        public void paintBorder(final Component c, final Graphics g, final int x, final int y, final int width, final int height) {
            if (!(g instanceof Graphics2D)) return;

            final Graphics2D g2d = (Graphics2D)g;
            final Color oldColor = g2d.getColor();

            g2d.setPaint(new GradientPaint(0, 0, startColor, 0, height, endColor));
            g2d.fillRect(x, y, width, height);
            g2d.setColor(borderLines);
            g2d.drawLine(x, y, x + width, y);
            g2d.drawLine(x, y + height - 1, x + width, y + height - 1);

            g2d.setColor(oldColor);
        }
    }
}
