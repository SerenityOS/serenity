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
import java.awt.Graphics;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.UIManager;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicLabelUI;

import sun.awt.AppContext;
import sun.swing.SwingUtilities2;

/**
 * Windows rendition of the component.
 */
public class WindowsLabelUI extends BasicLabelUI {

    private static final Object WINDOWS_LABEL_UI_KEY = new Object();

    // ********************************
    //          Create PLAF
    // ********************************
    public static ComponentUI createUI(JComponent c) {
        AppContext appContext = AppContext.getAppContext();
        WindowsLabelUI windowsLabelUI =
                (WindowsLabelUI) appContext.get(WINDOWS_LABEL_UI_KEY);
        if (windowsLabelUI == null) {
            windowsLabelUI = new WindowsLabelUI();
            appContext.put(WINDOWS_LABEL_UI_KEY, windowsLabelUI);
        }
        return windowsLabelUI;
    }

    protected void paintEnabledText(JLabel l, Graphics g, String s,
                                    int textX, int textY) {
        int mnemonicIndex = l.getDisplayedMnemonicIndex();
        // W2K Feature: Check to see if the Underscore should be rendered.
        if (WindowsLookAndFeel.isMnemonicHidden() == true) {
            mnemonicIndex = -1;
        }

        g.setColor(l.getForeground());
        SwingUtilities2.drawStringUnderlineCharAt(l, g, s, mnemonicIndex,
                                                     textX, textY);
    }

    protected void paintDisabledText(JLabel l, Graphics g, String s,
                                     int textX, int textY) {
        int mnemonicIndex = l.getDisplayedMnemonicIndex();
        // W2K Feature: Check to see if the Underscore should be rendered.
        if (WindowsLookAndFeel.isMnemonicHidden() == true) {
            mnemonicIndex = -1;
        }
        if ( UIManager.getColor("Label.disabledForeground") instanceof Color &&
             UIManager.getColor("Label.disabledShadow") instanceof Color) {
            g.setColor( UIManager.getColor("Label.disabledShadow") );
            SwingUtilities2.drawStringUnderlineCharAt(l, g, s,
                                                         mnemonicIndex,
                                                         textX + 1, textY + 1);
            g.setColor( UIManager.getColor("Label.disabledForeground") );
            SwingUtilities2.drawStringUnderlineCharAt(l, g, s,
                                                         mnemonicIndex,
                                                         textX, textY);
        } else {
            Color background = l.getBackground();
            g.setColor(background.brighter());
            SwingUtilities2.drawStringUnderlineCharAt(l,g, s, mnemonicIndex,
                                                         textX + 1, textY + 1);
            g.setColor(background.darker());
            SwingUtilities2.drawStringUnderlineCharAt(l,g, s, mnemonicIndex,
                                                         textX, textY);
        }
    }
}
