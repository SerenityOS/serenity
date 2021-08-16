/*
 * Copyright (c) 1997, 2005, Oracle and/or its affiliates. All rights reserved.
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

import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.*;
import javax.swing.*;

import static com.sun.java.swing.plaf.windows.TMSchema.Part;
import static com.sun.java.swing.plaf.windows.XPStyle.Skin;


/**
 * Draws Windows toolbar separators.
 *
 * @author Mark Davidson
 */
public class WindowsToolBarSeparatorUI extends BasicToolBarSeparatorUI {

    public static ComponentUI createUI( JComponent c ) {
        return new WindowsToolBarSeparatorUI();
    }

    public Dimension getPreferredSize(JComponent c) {
        Dimension size = ((JToolBar.Separator)c).getSeparatorSize();

        if (size != null) {
            size = size.getSize();
        } else {
            size = new Dimension(6, 6);
            XPStyle xp = XPStyle.getXP();
            if (xp != null) {
                boolean vertical = ((JSeparator)c).getOrientation() == SwingConstants.VERTICAL;
                Part part = vertical ? Part.TP_SEPARATOR : Part.TP_SEPARATORVERT;
                Skin skin = xp.getSkin(c, part);
                size.width = skin.getWidth();
                size.height = skin.getHeight();
            }

            if (((JSeparator)c).getOrientation() == SwingConstants.VERTICAL) {
                size.height = 0;
            } else {
                size.width = 0;
            }
        }
        return size;
    }

    public Dimension getMaximumSize(JComponent c) {
        Dimension pref = getPreferredSize(c);
        if (((JSeparator)c).getOrientation() == SwingConstants.VERTICAL) {
            return new Dimension(pref.width, Short.MAX_VALUE);
        } else {
            return new Dimension(Short.MAX_VALUE, pref.height);
        }
    }

    public void paint( Graphics g, JComponent c ) {
        boolean vertical = ((JSeparator)c).getOrientation() == SwingConstants.VERTICAL;
        Dimension size = c.getSize();

        XPStyle xp = XPStyle.getXP();
        if (xp != null) {
            Part part = vertical ? Part.TP_SEPARATOR : Part.TP_SEPARATORVERT;
            Skin skin = xp.getSkin(c, part);

            int dx = vertical ? (size.width - skin.getWidth()) / 2 : 0;
            int dy = vertical ? 0 : (size.height - skin.getHeight()) / 2;
            int dw = vertical ? skin.getWidth() : size.width;
            int dh = vertical ? size.height : skin.getHeight();
            skin.paintSkin(g, dx, dy, dw, dh, null);
        } else {

        Color temp = g.getColor();

        UIDefaults table = UIManager.getLookAndFeelDefaults();

        Color shadow = table.getColor("ToolBar.shadow");
        Color highlight = table.getColor("ToolBar.highlight");

        if (vertical) {
            int x = (size.width / 2) - 1;
            g.setColor(shadow);
            g.drawLine(x, 2, x, size.height - 2);

            g.setColor(highlight);
            g.drawLine(x + 1, 2, x + 1, size.height - 2);
        } else {
            int y = (size.height / 2) - 1;
            g.setColor(shadow);
            g.drawLine(2, y, size.width - 2, y);
            g.setColor(highlight);
            g.drawLine(2, y + 1, size.width - 2, y + 1);
        }
        g.setColor(temp);
        }
    }
}
