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
import java.awt.Rectangle;

import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.UIManager;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicRadioButtonUI;

import sun.awt.AppContext;

/**
 * RadioButtonUI implementation for MotifRadioButtonUI
 *
 * @author Rich Schiavi
 */
public class MotifRadioButtonUI extends BasicRadioButtonUI {

    private static final Object MOTIF_RADIO_BUTTON_UI_KEY = new Object();

    protected Color focusColor;

    private boolean defaults_initialized = false;

    // ********************************
    //         Create PLAF
    // ********************************
    public static ComponentUI createUI(JComponent c) {
        AppContext appContext = AppContext.getAppContext();
        MotifRadioButtonUI motifRadioButtonUI =
                (MotifRadioButtonUI) appContext.get(MOTIF_RADIO_BUTTON_UI_KEY);
        if (motifRadioButtonUI == null) {
            motifRadioButtonUI = new MotifRadioButtonUI();
            appContext.put(MOTIF_RADIO_BUTTON_UI_KEY, motifRadioButtonUI);
        }
        return motifRadioButtonUI;
    }

    // ********************************
    //          Install Defaults
    // ********************************
    public void installDefaults(AbstractButton b) {
        super.installDefaults(b);
        if(!defaults_initialized) {
            focusColor = UIManager.getColor(getPropertyPrefix() + "focus");
            defaults_initialized = true;
        }
    }

    protected void uninstallDefaults(AbstractButton b) {
        super.uninstallDefaults(b);
        defaults_initialized = false;
    }

    // ********************************
    //          Default Accessors
    // ********************************

    protected Color getFocusColor() {
        return focusColor;
    }

    // ********************************
    //         Paint Methods
    // ********************************
    protected void paintFocus(Graphics g, Rectangle t, Dimension d){
        g.setColor(getFocusColor());
        g.drawRect(0,0,d.width-1,d.height-1);
    }

}
