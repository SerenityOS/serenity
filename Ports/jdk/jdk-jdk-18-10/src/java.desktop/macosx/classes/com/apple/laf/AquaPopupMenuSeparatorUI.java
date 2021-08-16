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

import java.awt.*;

import javax.swing.*;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicSeparatorUI;

import com.apple.laf.AquaUtils.RecyclableSingletonFromDefaultConstructor;

public class AquaPopupMenuSeparatorUI extends BasicSeparatorUI {
    private static final RecyclableSingletonFromDefaultConstructor<AquaPopupMenuSeparatorUI> instance = new RecyclableSingletonFromDefaultConstructor<AquaPopupMenuSeparatorUI>(AquaPopupMenuSeparatorUI.class);

    public static ComponentUI createUI(final JComponent c) {
        return instance.get();
    }

    public void update(final Graphics g, final JComponent c) {
        paint(g, c);
    }

    public void paint(final Graphics g, final JComponent c) {
        final Dimension s = c.getSize();

        if (((JSeparator)c).getOrientation() == SwingConstants.VERTICAL) {
            g.setColor(c.getForeground());
            g.drawLine(5, 1, 5, s.height - 2);

            g.setColor(c.getBackground());
            g.drawLine(6, 1, 6, s.height - 2);
        } else {
            g.setColor(c.getForeground());
            g.drawLine(1, 5, s.width - 2, 5);

            g.setColor(c.getBackground());
            g.drawLine(1, 6, s.width - 2, 6);
        }
    }

    public Dimension getPreferredSize(final JComponent c) {
        if (((JSeparator)c).getOrientation() == SwingConstants.VERTICAL) {
            return new Dimension(12, 0);
        }

        return new Dimension(0, 12);
    }
}
