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

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import javax.swing.JComponent;
import javax.swing.UIManager;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.UIResource;
import javax.swing.text.View;

import sun.swing.SwingUtilities2;

import apple.laf.JRSUIConstants.*;

public class AquaTabbedPaneContrastUI extends AquaTabbedPaneUI {
    public static ComponentUI createUI(final JComponent c) {
        return new AquaTabbedPaneContrastUI();
    }

    public AquaTabbedPaneContrastUI() { }

    protected void paintTitle(final Graphics2D g2d, final Font font, final FontMetrics metrics, final Rectangle textRect, final int tabIndex, final String title) {
        final View v = getTextViewForTab(tabIndex);
        if (v != null) {
            v.paint(g2d, textRect);
            return;
        }

        if (title == null) return;

        final Color color = tabPane.getForegroundAt(tabIndex);
        if (color instanceof UIResource) {
            g2d.setColor(getNonSelectedTabTitleColor());
            if (tabPane.getSelectedIndex() == tabIndex) {
                boolean pressed = isPressedAt(tabIndex);
                boolean enabled = tabPane.isEnabled() && tabPane.isEnabledAt(tabIndex);
                Color textColor = getSelectedTabTitleColor(enabled, pressed);
                Color shadowColor = getSelectedTabTitleShadowColor(enabled);
                AquaUtils.paintDropShadowText(g2d, tabPane, font, metrics, textRect.x, textRect.y, 0, 1, textColor, shadowColor, title);
                return;
            }
        } else {
            g2d.setColor(color);
        }
        g2d.setFont(font);
        SwingUtilities2.drawString(tabPane, g2d, title, textRect.x, textRect.y + metrics.getAscent());
    }

    protected static Color getSelectedTabTitleColor(boolean enabled, boolean pressed) {
        if (enabled && pressed) {
            return UIManager.getColor("TabbedPane.selectedTabTitlePressedColor");
        } else if (!enabled) {
            return UIManager.getColor("TabbedPane.selectedTabTitleDisabledColor");
        } else {
            return UIManager.getColor("TabbedPane.selectedTabTitleNormalColor");
        }
    }

    protected static Color getSelectedTabTitleShadowColor(boolean enabled) {
        return enabled ? UIManager.getColor("TabbedPane.selectedTabTitleShadowNormalColor") : UIManager.getColor("TabbedPane.selectedTabTitleShadowDisabledColor");
    }

    protected static Color getNonSelectedTabTitleColor() {
        return UIManager.getColor("TabbedPane.nonSelectedTabTitleNormalColor");
    }

    protected boolean isPressedAt(int index) {
        return ((MouseHandler)mouseListener).trackingTab == index;
    }

    protected boolean shouldRepaintSelectedTabOnMouseDown() {
        return true;
    }

    protected State getState(final int index, final boolean frameActive, final boolean isSelected) {
        if (!frameActive) return State.INACTIVE;
        if (!tabPane.isEnabled()) return State.DISABLED;
        if (pressedTab == index) return State.PRESSED;
        return State.ACTIVE;
    }

    protected SegmentTrailingSeparator getSegmentTrailingSeparator(final int index, final int selectedIndex, final boolean isLeftToRight) {
        if (isTabBeforeSelectedTab(index, selectedIndex, isLeftToRight)) return SegmentTrailingSeparator.NO;
        return SegmentTrailingSeparator.YES;
    }

    protected SegmentLeadingSeparator getSegmentLeadingSeparator(final int index, final int selectedIndex, final boolean isLeftToRight) {
        if (index == selectedIndex) return SegmentLeadingSeparator.YES;
        return SegmentLeadingSeparator.NO;
    }
}
