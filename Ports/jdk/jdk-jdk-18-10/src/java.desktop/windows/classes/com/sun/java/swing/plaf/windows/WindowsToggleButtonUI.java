/*
 * Copyright (c) 1997, 2020, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.java.swing.plaf.windows;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;

import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.LookAndFeel;
import javax.swing.UIManager;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicGraphicsUtils;
import javax.swing.plaf.basic.BasicToggleButtonUI;

import sun.awt.AppContext;

/**
 * A Windows toggle button.
 *
 * @author Jeff Dinkins
 */
public class WindowsToggleButtonUI extends BasicToggleButtonUI
{
    protected int dashedRectGapX;
    protected int dashedRectGapY;
    protected int dashedRectGapWidth;
    protected int dashedRectGapHeight;

    protected Color focusColor;

    private static final Object WINDOWS_TOGGLE_BUTTON_UI_KEY = new Object();

    private boolean defaults_initialized = false;

    public static ComponentUI createUI(JComponent b) {
        AppContext appContext = AppContext.getAppContext();
        WindowsToggleButtonUI windowsToggleButtonUI =
                (WindowsToggleButtonUI) appContext.get(WINDOWS_TOGGLE_BUTTON_UI_KEY);
        if (windowsToggleButtonUI == null) {
            windowsToggleButtonUI = new WindowsToggleButtonUI();
            appContext.put(WINDOWS_TOGGLE_BUTTON_UI_KEY, windowsToggleButtonUI);
        }
        return windowsToggleButtonUI;
    }


    // ********************************
    //            Defaults
    // ********************************
    protected void installDefaults(AbstractButton b) {
        super.installDefaults(b);
        if(!defaults_initialized) {
            String pp = getPropertyPrefix();
            dashedRectGapX = ((Integer)UIManager.get("Button.dashedRectGapX")).intValue();
            dashedRectGapY = ((Integer)UIManager.get("Button.dashedRectGapY")).intValue();
            dashedRectGapWidth = ((Integer)UIManager.get("Button.dashedRectGapWidth")).intValue();
            dashedRectGapHeight = ((Integer)UIManager.get("Button.dashedRectGapHeight")).intValue();
            focusColor = UIManager.getColor(pp + "focus");
            defaults_initialized = true;
        }

        XPStyle xp = XPStyle.getXP();
        if (xp != null) {
            b.setBorder(xp.getBorder(b, WindowsButtonUI.getXPButtonType(b)));
            LookAndFeel.installProperty(b, "opaque", Boolean.FALSE);
            LookAndFeel.installProperty(b, "rolloverEnabled", Boolean.TRUE);
        }
    }

    protected void uninstallDefaults(AbstractButton b) {
        super.uninstallDefaults(b);
        defaults_initialized = false;
    }


    protected Color getFocusColor() {
        return focusColor;
    }


    // ********************************
    //         Paint Methods
    // ********************************

    private transient Color cachedSelectedColor = null;
    private transient Color cachedBackgroundColor = null;
    private transient Color cachedHighlightColor = null;

    protected void paintButtonPressed(Graphics g, AbstractButton b) {
        if (XPStyle.getXP() == null && b.isContentAreaFilled()) {
            Color oldColor = g.getColor();
            Color c1 = b.getBackground();
            Color c2 = UIManager.getColor("ToggleButton.highlight");
            if (c1 != cachedBackgroundColor || c2 != cachedHighlightColor) {
                int r1 = c1.getRed(), r2 = c2.getRed();
                int g1 = c1.getGreen(), g2 = c2.getGreen();
                int b1 = c1.getBlue(), b2 = c2.getBlue();
                cachedSelectedColor = new Color(
                        Math.min(r1, r2) + Math.abs(r1 - r2) / 2,
                        Math.min(g1, g2) + Math.abs(g1 - g2) / 2,
                        Math.min(b1, b2) + Math.abs(b1 - b2) / 2
                );
                cachedBackgroundColor = c1;
                cachedHighlightColor = c2;
            }
            g.setColor(cachedSelectedColor);
            g.fillRect(0, 0, b.getWidth(), b.getHeight());
            g.setColor(oldColor);
        }
    }

    public void paint(Graphics g, JComponent c) {
        if (XPStyle.getXP() != null) {
            WindowsButtonUI.paintXPButtonBackground(g, c);
        }
        super.paint(g, c);
    }


    /**
     * Overridden method to render the text without the mnemonic
     */
    protected void paintText(Graphics g, AbstractButton b, Rectangle textRect, String text) {
        WindowsGraphicsUtils.paintText(g, b, textRect, text, getTextShiftOffset());
    }

    protected void paintFocus(Graphics g, AbstractButton b,
                              Rectangle viewRect, Rectangle textRect, Rectangle iconRect) {
        g.setColor(getFocusColor());
        BasicGraphicsUtils.drawDashedRect(g, dashedRectGapX, dashedRectGapY,
                                          b.getWidth() - dashedRectGapWidth,
                                          b.getHeight() - dashedRectGapHeight);
    }

    // ********************************
    //          Layout Methods
    // ********************************
    public Dimension getPreferredSize(JComponent c) {
        Dimension d = super.getPreferredSize(c);

        /* Ensure that the width and height of the button is odd,
         * to allow for the focus line if focus is painted
         */
        AbstractButton b = (AbstractButton)c;
        if (d != null && b.isFocusPainted()) {
            if(d.width % 2 == 0) { d.width += 1; }
            if(d.height % 2 == 0) { d.height += 1; }
        }
        return d;
    }
}
