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

package com.sun.java.swing.plaf.motif;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Insets;

import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.LookAndFeel;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.UIResource;
import javax.swing.plaf.basic.BasicToggleButtonUI;

import sun.awt.AppContext;

/**
 * BasicToggleButton implementation.
 *
 * @author Rich Schiavi
 */
public class MotifToggleButtonUI extends BasicToggleButtonUI
{
    private static final Object MOTIF_TOGGLE_BUTTON_UI_KEY = new Object();

    protected Color selectColor;

    private boolean defaults_initialized = false;

    // ********************************
    //         Create PLAF
    // ********************************
    public static ComponentUI createUI(JComponent b) {
        AppContext appContext = AppContext.getAppContext();
        MotifToggleButtonUI motifToggleButtonUI =
                (MotifToggleButtonUI) appContext.get(MOTIF_TOGGLE_BUTTON_UI_KEY);
        if (motifToggleButtonUI == null) {
            motifToggleButtonUI = new MotifToggleButtonUI();
            appContext.put(MOTIF_TOGGLE_BUTTON_UI_KEY, motifToggleButtonUI);
        }
        return motifToggleButtonUI;
    }

    // ********************************
    //          Install Defaults
    // ********************************
    public void installDefaults(AbstractButton b) {
        super.installDefaults(b);
        if(!defaults_initialized) {
            selectColor = UIManager.getColor(getPropertyPrefix() + "select");
            defaults_initialized = true;
        }
        LookAndFeel.installProperty(b, "opaque", Boolean.FALSE);
    }

    protected void uninstallDefaults(AbstractButton b) {
        super.uninstallDefaults(b);
        defaults_initialized = false;
    }

    // ********************************
    //          Default Accessors
    // ********************************

    protected Color getSelectColor() {
        return selectColor;
    }

    // ********************************
    //         Paint Methods
    // ********************************
    protected void paintButtonPressed(Graphics g, AbstractButton b) {
        if (b.isContentAreaFilled()) {
            Color oldColor = g.getColor();
            Dimension size = b.getSize();
            Insets insets = b.getInsets();
            Insets margin = b.getMargin();

            if(b.getBackground() instanceof UIResource) {
                g.setColor(getSelectColor());
            }
            g.fillRect(insets.left - margin.left,
                       insets.top - margin.top,
                       size.width - (insets.left-margin.left) - (insets.right - margin.right),
                       size.height - (insets.top-margin.top) - (insets.bottom - margin.bottom));
            g.setColor(oldColor);
        }
    }

    public Insets getInsets(JComponent c) {
        Border border = c.getBorder();
        Insets i = border != null? border.getBorderInsets(c) : new Insets(0,0,0,0);
        return i;
    }

}
