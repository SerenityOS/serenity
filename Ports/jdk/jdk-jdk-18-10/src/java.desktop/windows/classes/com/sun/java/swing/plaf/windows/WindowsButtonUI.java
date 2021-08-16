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
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Rectangle;

import javax.swing.AbstractButton;
import javax.swing.ButtonModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JRadioButton;
import javax.swing.JToolBar;
import javax.swing.LookAndFeel;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.plaf.basic.BasicGraphicsUtils;

import sun.awt.AppContext;

import static com.sun.java.swing.plaf.windows.TMSchema.Part;
import static com.sun.java.swing.plaf.windows.TMSchema.State;
import static com.sun.java.swing.plaf.windows.XPStyle.Skin;

/**
 * Windows button.
 *
 * @author Jeff Dinkins
 */
public class WindowsButtonUI extends BasicButtonUI
{
    protected int dashedRectGapX;
    protected int dashedRectGapY;
    protected int dashedRectGapWidth;
    protected int dashedRectGapHeight;

    protected Color focusColor;

    private boolean defaults_initialized = false;

    private static final Object WINDOWS_BUTTON_UI_KEY = new Object();

    // ********************************
    //          Create PLAF
    // ********************************
    public static ComponentUI createUI(JComponent c) {
        AppContext appContext = AppContext.getAppContext();
        WindowsButtonUI windowsButtonUI =
                (WindowsButtonUI) appContext.get(WINDOWS_BUTTON_UI_KEY);
        if (windowsButtonUI == null) {
            windowsButtonUI = new WindowsButtonUI();
            appContext.put(WINDOWS_BUTTON_UI_KEY, windowsButtonUI);
        }
        return windowsButtonUI;
    }


    // ********************************
    //            Defaults
    // ********************************
    protected void installDefaults(AbstractButton b) {
        super.installDefaults(b);
        if(!defaults_initialized) {
            String pp = getPropertyPrefix();
            dashedRectGapX = UIManager.getInt(pp + "dashedRectGapX");
            dashedRectGapY = UIManager.getInt(pp + "dashedRectGapY");
            dashedRectGapWidth = UIManager.getInt(pp + "dashedRectGapWidth");
            dashedRectGapHeight = UIManager.getInt(pp + "dashedRectGapHeight");
            focusColor = UIManager.getColor(pp + "focus");
            defaults_initialized = true;
        }

        XPStyle xp = XPStyle.getXP();
        if (xp != null) {
            b.setBorder(xp.getBorder(b, getXPButtonType(b)));
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

    /**
     * Overridden method to render the text without the mnemonic
     */
    protected void paintText(Graphics g, AbstractButton b, Rectangle textRect, String text) {
        WindowsGraphicsUtils.paintText(g, b, textRect, text, getTextShiftOffset());
    }

    protected void paintFocus(Graphics g, AbstractButton b, Rectangle viewRect, Rectangle textRect, Rectangle iconRect){

        // focus painted same color as text on Basic??
        int width = b.getWidth();
        int height = b.getHeight();
        g.setColor(getFocusColor());
        BasicGraphicsUtils.drawDashedRect(g, dashedRectGapX, dashedRectGapY,
                                          width - dashedRectGapWidth, height - dashedRectGapHeight);
    }

    protected void paintButtonPressed(Graphics g, AbstractButton b){
        setTextShiftOffset();
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


    /* These rectangles/insets are allocated once for all
     * ButtonUI.paint() calls.  Re-using rectangles rather than
     * allocating them in each paint call substantially reduced the time
     * it took paint to run.  Obviously, this method can't be re-entered.
     */
    private Rectangle viewRect = new Rectangle();

    public void paint(Graphics g, JComponent c) {
        if (XPStyle.getXP() != null) {
            WindowsButtonUI.paintXPButtonBackground(g, c);
        }
        super.paint(g, c);
    }

    static Part getXPButtonType(AbstractButton b) {
        if(b instanceof JCheckBox) {
            return Part.BP_CHECKBOX;
        }
        if(b instanceof JRadioButton) {
            return Part.BP_RADIOBUTTON;
        }
        boolean toolbar = (b.getParent() instanceof JToolBar);
        return toolbar ? Part.TP_BUTTON : Part.BP_PUSHBUTTON;
    }

    static State getXPButtonState(AbstractButton b) {
        Part part = getXPButtonType(b);
        ButtonModel model = b.getModel();
        State state = State.NORMAL;
        switch (part) {
        case BP_RADIOBUTTON:
            /* falls through */
        case BP_CHECKBOX:
            if (! model.isEnabled()) {
                state = (model.isSelected()) ? State.CHECKEDDISABLED
                    : State.UNCHECKEDDISABLED;
            } else if (model.isPressed() && model.isArmed()) {
                state = (model.isSelected()) ? State.CHECKEDPRESSED
                    : State.UNCHECKEDPRESSED;
            } else if (model.isRollover()) {
                state = (model.isSelected()) ? State.CHECKEDHOT
                    : State.UNCHECKEDHOT;
            } else {
                state = (model.isSelected()) ? State.CHECKEDNORMAL
                    : State.UNCHECKEDNORMAL;
            }
            break;
        case BP_PUSHBUTTON:
            /* falls through */
        case TP_BUTTON:
            boolean toolbar = (b.getParent() instanceof JToolBar);
            if (toolbar) {
                if (model.isArmed() && model.isPressed()) {
                    state = State.PRESSED;
                } else if (!model.isEnabled()) {
                    state = State.DISABLED;
                } else if (model.isSelected() && model.isRollover()) {
                    state = State.HOTCHECKED;
                } else if (model.isSelected()) {
                    state = State.CHECKED;
                } else if (model.isRollover()) {
                    state = State.HOT;
                } else if (b.hasFocus()) {
                    state = State.HOT;
                }
            } else {
                if ((model.isArmed() && model.isPressed())
                      || model.isSelected()) {
                    state = State.PRESSED;
                } else if (!model.isEnabled()) {
                    state = State.DISABLED;
                } else if (model.isRollover() || model.isPressed()) {
                    state = State.HOT;
                } else if (b instanceof JButton
                           && ((JButton)b).isDefaultButton()) {
                    state = State.DEFAULTED;
                } else if (b.hasFocus()) {
                    state = State.HOT;
                }
            }
            break;
        default :
            state = State.NORMAL;
        }

        return state;
    }

    static void paintXPButtonBackground(Graphics g, JComponent c) {
        AbstractButton b = (AbstractButton)c;

        XPStyle xp = XPStyle.getXP();

        Part part = getXPButtonType(b);

        if (b.isContentAreaFilled() && b.getBorder() != null
                && b.isBorderPainted() && xp != null) {

            Skin skin = xp.getSkin(b, part);

            State state = getXPButtonState(b);
            Dimension d = c.getSize();
            int dx = 0;
            int dy = 0;
            int dw = d.width;
            int dh = d.height;

            Border border = c.getBorder();
            Insets insets;
            if (border != null) {
                // Note: The border may be compound, containing an outer
                // opaque border (supplied by the application), plus an
                // inner transparent margin border. We want to size the
                // background to fill the transparent part, but stay
                // inside the opaque part.
                insets = WindowsButtonUI.getOpaqueInsets(border, c);
            } else {
                insets = c.getInsets();
            }
            if (insets != null) {
                dx += insets.left;
                dy += insets.top;
                dw -= (insets.left + insets.right);
                dh -= (insets.top + insets.bottom);
            }
            skin.paintSkin(g, dx, dy, dw, dh, state);
        }
    }

    /**
     * returns - b.getBorderInsets(c) if border is opaque
     *         - null if border is completely non-opaque
     *         - somewhere inbetween if border is compound and
     *              outside border is opaque and inside isn't
     */
    private static Insets getOpaqueInsets(Border b, Component c) {
        if (b == null) {
            return null;
        }
        if (b.isBorderOpaque()) {
            return b.getBorderInsets(c);
        } else if (b instanceof CompoundBorder) {
            CompoundBorder cb = (CompoundBorder)b;
            Insets iOut = getOpaqueInsets(cb.getOutsideBorder(), c);
            if (iOut != null && iOut.equals(cb.getOutsideBorder().getBorderInsets(c))) {
                // Outside border is opaque, keep looking
                Insets iIn = getOpaqueInsets(cb.getInsideBorder(), c);
                if (iIn == null) {
                    // Inside is non-opaque, use outside insets
                    return iOut;
                } else {
                    // Found non-opaque somewhere in the inside (which is
                    // also compound).
                    return new Insets(iOut.top + iIn.top, iOut.left + iIn.left,
                                      iOut.bottom + iIn.bottom, iOut.right + iIn.right);
                }
            } else {
                // Outside is either all non-opaque or has non-opaque
                // border inside another compound border
                return iOut;
            }
        } else {
            return null;
        }
    }
}
