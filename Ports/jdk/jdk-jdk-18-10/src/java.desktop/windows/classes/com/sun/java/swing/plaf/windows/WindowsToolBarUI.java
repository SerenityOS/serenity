/*
 * Copyright (c) 1997, 2006, Oracle and/or its affiliates. All rights reserved.
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

import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.JToggleButton;
import javax.swing.UIDefaults;
import javax.swing.UIManager;

import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;

import javax.swing.plaf.*;

import javax.swing.plaf.basic.BasicBorders;
import javax.swing.plaf.basic.BasicToolBarUI;

import static com.sun.java.swing.plaf.windows.TMSchema.Part;


public class WindowsToolBarUI extends BasicToolBarUI {

    public static ComponentUI createUI(JComponent c) {
        return new WindowsToolBarUI();
    }

    protected void installDefaults() {
        if (XPStyle.getXP() != null) {
            setRolloverBorders(true);
        }
        super.installDefaults();
    }

    protected Border createRolloverBorder() {
        if (XPStyle.getXP() != null) {
            return new EmptyBorder(3, 3, 3, 3);
        } else {
            return super.createRolloverBorder();
        }
    }

    protected Border createNonRolloverBorder() {
        if (XPStyle.getXP() != null) {
            return new EmptyBorder(3, 3, 3, 3);
        } else {
            return super.createNonRolloverBorder();
        }
    }

    public void paint(Graphics g, JComponent c) {
        XPStyle xp = XPStyle.getXP();
        if (xp != null) {
            xp.getSkin(c, Part.TP_TOOLBAR).paintSkin(g, 0, 0,
                        c.getWidth(), c.getHeight(), null, true);
        } else {
            super.paint(g, c);
        }
    }

    /**
     * {@inheritDoc}
     * @since 1.6
     */
    protected Border getRolloverBorder(AbstractButton b) {
        XPStyle xp = XPStyle.getXP();
        if (xp != null) {
            return xp.getBorder(b, WindowsButtonUI.getXPButtonType(b));
        } else {
            return super.getRolloverBorder(b);
        }
    }
}
