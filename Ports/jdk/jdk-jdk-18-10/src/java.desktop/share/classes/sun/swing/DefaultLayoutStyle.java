/*
 * Copyright (c) 2005, 2011, Oracle and/or its affiliates. All rights reserved.
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
package sun.swing;

import java.awt.Container;
import java.awt.Insets;
import javax.swing.*;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.border.Border;
import javax.swing.plaf.UIResource;

/**
 * An implementation of <code>LayoutStyle</code> that returns 6 for related
 * components, otherwise 12.  This class also provides helper methods for
 * subclasses.
 *
 */
public class DefaultLayoutStyle extends LayoutStyle {
    private static final DefaultLayoutStyle INSTANCE =
            new DefaultLayoutStyle();

    public static LayoutStyle getInstance() {
        return INSTANCE;
    }

    @Override
    public int getPreferredGap(JComponent component1, JComponent component2,
            ComponentPlacement type, int position, Container parent) {
        if (component1 == null || component2 == null || type == null) {
            throw new NullPointerException();
        }

        checkPosition(position);

        if (type == ComponentPlacement.INDENT &&
                (position == SwingConstants.EAST ||
                 position == SwingConstants.WEST)) {
            int indent = getIndent(component1, position);
            if (indent > 0) {
                return indent;
            }
        }
        return (type == ComponentPlacement.UNRELATED) ? 12 : 6;
    }

    @Override
    public int getContainerGap(JComponent component, int position,
                               Container parent) {
        if (component == null) {
            throw new NullPointerException();
        }
        checkPosition(position);
        return 6;
    }

    /**
     * Returns true if the classes identify a JLabel and a non-JLabel
     * along the horizontal axis.
     */
    protected boolean isLabelAndNonlabel(JComponent c1, JComponent c2,
                                         int position) {
        if (position == SwingConstants.EAST ||
                position == SwingConstants.WEST) {
            boolean c1Label = (c1 instanceof JLabel);
            boolean c2Label = (c2 instanceof JLabel);
            return ((c1Label || c2Label) && (c1Label != c2Label));
        }
        return false;
    }

    /**
     * For some look and feels check boxs and radio buttons typically
     * don't paint the border, yet they have padding for a border.  Look
     * and feel guidelines generally don't include this space.  Use
     * this method to subtract this space from the specified
     * components.
     *
     * @param source First component
     * @param target Second component
     * @param position Position doing layout along.
     * @param offset Ideal offset, not including border/margin
     * @return offset - border/margin around the component.
     */
    protected int getButtonGap(JComponent source, JComponent target,
                               int position, int offset) {
        offset -= getButtonGap(source, position);
        if (offset > 0) {
            offset -= getButtonGap(target, flipDirection(position));
        }
        if (offset < 0) {
            return 0;
        }
        return offset;
    }

    /**
     * For some look and feels check boxs and radio buttons typically
     * don't paint the border, yet they have padding for a border.  Look
     * and feel guidelines generally don't include this space.  Use
     * this method to subtract this space from the specified
     * components.
     *
     * @param source Component
     * @param position Position doing layout along.
     * @param offset Ideal offset, not including border/margin
     * @return offset - border/margin around the component.
     */
    protected int getButtonGap(JComponent source, int position, int offset) {
        offset -= getButtonGap(source, position);
        return Math.max(offset, 0);
    }

    /**
     * If <code>c</code> is a check box or radio button, and the border is
     * not painted this returns the inset along the specified axis.
     */
    public int getButtonGap(JComponent c, int position) {
        String classID = c.getUIClassID();
        if ((classID == "CheckBoxUI" || classID == "RadioButtonUI") &&
                !((AbstractButton)c).isBorderPainted()) {
            Border border = c.getBorder();
            if (border instanceof UIResource) {
                return getInset(c, position);
            }
        }
        return 0;
    }

    private void checkPosition(int position) {
        if (position != SwingConstants.NORTH &&
                position != SwingConstants.SOUTH &&
                position != SwingConstants.WEST &&
                position != SwingConstants.EAST) {
            throw new IllegalArgumentException();
        }
    }

    protected int flipDirection(int position) {
        switch(position) {
        case SwingConstants.NORTH:
            return SwingConstants.SOUTH;
        case SwingConstants.SOUTH:
            return SwingConstants.NORTH;
        case SwingConstants.EAST:
            return SwingConstants.WEST;
        case SwingConstants.WEST:
            return SwingConstants.EAST;
        }
        assert false;
        return 0;
    }

    /**
     * Returns the amount to indent the specified component if it's
     * a JCheckBox or JRadioButton.  If the component is not a JCheckBox or
     * JRadioButton, 0 will be returned.
     */
    protected int getIndent(JComponent c, int position) {
        String classID = c.getUIClassID();
        if (classID == "CheckBoxUI" || classID == "RadioButtonUI") {
            AbstractButton button = (AbstractButton)c;
            Insets insets = c.getInsets();
            Icon icon = getIcon(button);
            int gap = button.getIconTextGap();
            if (isLeftAligned(button, position)) {
                return insets.left + icon.getIconWidth() + gap;
            } else if (isRightAligned(button, position)) {
                return insets.right + icon.getIconWidth() + gap;
            }
        }
        return 0;
    }

    private Icon getIcon(AbstractButton button) {
        Icon icon = button.getIcon();
        if (icon != null) {
            return icon;
        }
        String key = null;
        if (button instanceof JCheckBox) {
            key = "CheckBox.icon";
        } else if (button instanceof JRadioButton) {
            key = "RadioButton.icon";
        }
        if (key != null) {
            Object oIcon = UIManager.get(key);
            if (oIcon instanceof Icon) {
                return (Icon)oIcon;
            }
        }
        return null;
    }

    private boolean isLeftAligned(AbstractButton button, int position) {
        if (position == SwingConstants.WEST) {
            boolean ltr = button.getComponentOrientation().isLeftToRight();
            int hAlign = button.getHorizontalAlignment();
            return ((ltr && (hAlign == SwingConstants.LEFT ||
                             hAlign == SwingConstants.LEADING)) ||
                    (!ltr && (hAlign == SwingConstants.TRAILING)));
        }
        return false;
    }

    private boolean isRightAligned(AbstractButton button, int position) {
        if (position == SwingConstants.EAST) {
            boolean ltr = button.getComponentOrientation().isLeftToRight();
            int hAlign = button.getHorizontalAlignment();
            return ((ltr && (hAlign == SwingConstants.RIGHT ||
                             hAlign == SwingConstants.TRAILING)) ||
                    (!ltr && (hAlign == SwingConstants.LEADING)));
        }
        return false;
    }

    private int getInset(JComponent c, int position) {
        return getInset(c.getInsets(), position);
    }

    private int getInset(Insets insets, int position) {
        if (insets == null) {
            return 0;
        }
        switch(position) {
        case SwingConstants.NORTH:
            return insets.top;
        case SwingConstants.SOUTH:
            return insets.bottom;
        case SwingConstants.EAST:
            return insets.right;
        case SwingConstants.WEST:
            return insets.left;
        }
        assert false;
        return 0;
    }
}
