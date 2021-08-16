/*
 * Copyright (c) 1997, 2014, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.*;
import java.awt.event.*;

import javax.swing.plaf.basic.*;
import javax.swing.plaf.*;
import javax.swing.*;

import static com.sun.java.swing.plaf.windows.TMSchema.Part;
import static com.sun.java.swing.plaf.windows.TMSchema.State;
import static com.sun.java.swing.plaf.windows.XPStyle.Skin;


public class WindowsSpinnerUI extends BasicSpinnerUI {
    public static ComponentUI createUI(JComponent c) {
        return new WindowsSpinnerUI();
    }

    /**
     * {@inheritDoc}
     * @since 1.6
     */
    public void paint(Graphics g, JComponent c) {
        if (XPStyle.getXP() != null) {
            paintXPBackground(g, c);
        }
        super.paint(g,c);
    }

    private State getXPState(JComponent c) {
        State state = State.NORMAL;
        if (!c.isEnabled()) {
            state = State.DISABLED;
        }
        return state;
    }

    private void paintXPBackground(Graphics g, JComponent c) {
        XPStyle xp = XPStyle.getXP();
        if (xp == null) {
            return;
        }
        Skin skin = xp.getSkin(c, Part.EP_EDIT);
        State state = getXPState(c);
        skin.paintSkin(g, 0, 0, c.getWidth(), c.getHeight(), state);
    }

    protected Component createPreviousButton() {
        if (XPStyle.getXP() != null) {
            JButton xpButton = new XPStyle.GlyphButton(spinner, Part.SPNP_DOWN);
            Dimension size = UIManager.getDimension("Spinner.arrowButtonSize");
            xpButton.setPreferredSize(size);
            xpButton.setRequestFocusEnabled(false);
            installPreviousButtonListeners(xpButton);
            return xpButton;
        }
        return super.createPreviousButton();
    }

    protected Component createNextButton() {
        if (XPStyle.getXP() != null) {
            JButton xpButton = new XPStyle.GlyphButton(spinner, Part.SPNP_UP);
            Dimension size = UIManager.getDimension("Spinner.arrowButtonSize");
            xpButton.setPreferredSize(size);
            xpButton.setRequestFocusEnabled(false);
            installNextButtonListeners(xpButton);
            return xpButton;
        }
        return super.createNextButton();
    }

    private UIResource getUIResource(Object[] listeners) {
        for (int counter = 0; counter < listeners.length; counter++) {
            if (listeners[counter] instanceof UIResource) {
                return (UIResource)listeners[counter];
            }
        }
        return null;
    }
}
