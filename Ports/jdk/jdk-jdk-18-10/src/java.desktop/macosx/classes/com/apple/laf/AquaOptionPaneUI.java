/*
 * Copyright (c) 2011, 2012, Oracle and/or its affiliates. All rights reserved.
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
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicOptionPaneUI;

public class AquaOptionPaneUI extends BasicOptionPaneUI {
    private static final int kOKCancelButtonWidth = 79;
    private static final int kButtonHeight = 23;

    private static final int kDialogSmallPadding = 4;
    private static final int kDialogLargePadding = 23;

    /**
     * Creates a new BasicOptionPaneUI instance.
     */
    public static ComponentUI createUI(final JComponent x) {
        return new AquaOptionPaneUI();
    }

    /**
     * Creates and returns a Container containin the buttons. The buttons
     * are created by calling {@code getButtons}.
     */
    protected Container createButtonArea() {
        final Container bottom = super.createButtonArea();
        // Now replace the Layout
        bottom.setLayout(new AquaButtonAreaLayout(true, kDialogSmallPadding));
        return bottom;
    }

    /**
     * Messaged from installComponents to create a Container containing the
     * body of the message.
     * The icon and body should be aligned on their top edges
     */
    protected Container createMessageArea() {
        final JPanel top = new JPanel();
        top.setBorder(UIManager.getBorder("OptionPane.messageAreaBorder"));
        top.setLayout(new BoxLayout(top, BoxLayout.X_AXIS));

        /* Fill the body. */
        final Container body = new JPanel();

        final Icon sideIcon = getIcon();

        if (sideIcon != null) {
            final JLabel iconLabel = new JLabel(sideIcon);
            iconLabel.setVerticalAlignment(SwingConstants.TOP);

            final JPanel iconPanel = new JPanel();
            iconPanel.add(iconLabel);
            top.add(iconPanel);
            top.add(Box.createHorizontalStrut(kDialogLargePadding));
        }

        body.setLayout(new GridBagLayout());
        final GridBagConstraints cons = new GridBagConstraints();
        cons.gridx = cons.gridy = 0;
        cons.gridwidth = GridBagConstraints.REMAINDER;
        cons.gridheight = 1;
        cons.anchor = GridBagConstraints.WEST;
        cons.insets = new Insets(0, 0, 3, 0);

        addMessageComponents(body, cons, getMessage(), getMaxCharactersPerLineCount(), false);
        top.add(body);

        return top;
    }

    /**
     * AquaButtonAreaLayout lays out all
     *   components according to the HI Guidelines:
     * The most important button is always on the far right
     * The group of buttons is on the right for left-to-right,
     *         left for right-to-left
     * The widths of each component will be set to the largest preferred size width.
     *
     *
     * This inner class is marked &quot;public&quot; due to a compiler bug.
     * This class should be treated as a &quot;protected&quot; inner class.
     * Instantiate it only within subclasses of BasicOptionPaneUI.
     *
     * BasicOptionPaneUI expects that its buttons are layed out with
     * a subclass of ButtonAreaLayout
     */
    public static class AquaButtonAreaLayout extends ButtonAreaLayout {
        public AquaButtonAreaLayout(final boolean syncAllWidths, final int padding) {
            super(true, padding);
        }

        public void layoutContainer(final Container container) {
            final Component[] children = container.getComponents();
            if (children == null || 0 >= children.length) return;

            final int numChildren = children.length;
            final int yLocation = container.getInsets().top;

            // Always syncAllWidths - and heights!
            final Dimension maxSize = new Dimension(kOKCancelButtonWidth, kButtonHeight);
            for (int i = 0; i < numChildren; i++) {
                final Dimension sizes = children[i].getPreferredSize();
                maxSize.width = Math.max(maxSize.width, sizes.width);
                maxSize.height = Math.max(maxSize.height, sizes.height);
            }

            // ignore getCentersChildren, because we don't
            int xLocation = container.getSize().width - (maxSize.width * numChildren + (numChildren - 1) * padding);
            final int xOffset = maxSize.width + padding;

            // most important button (button zero) on far right
            for (int i = numChildren - 1; i >= 0; i--) {
                children[i].setBounds(xLocation, yLocation, maxSize.width, maxSize.height);
                xLocation += xOffset;
            }
        }

        @Override
        public Dimension minimumLayoutSize(Container c) {
            if (c != null) {
                Component[] children = c.getComponents();
                if (children != null && children.length > 0) {
                    int numChildren = children.length;
                    Insets cInsets = c.getInsets();
                    int extraHeight = cInsets.top + cInsets.bottom;
                    int extraWidth = cInsets.left + cInsets.right;
                    int okCancelButtonWidth = extraWidth
                            + (kOKCancelButtonWidth * numChildren)
                            + (numChildren - 1) * padding;
                    int okbuttonHeight = extraHeight + kButtonHeight;
                    Dimension minSize = super.minimumLayoutSize(c);
                    return new Dimension(Math.max(minSize.width,
                            okCancelButtonWidth),
                            Math.max(minSize.height, okbuttonHeight));
                }
            }
            return new Dimension(0, 0);
        }
    }
}
