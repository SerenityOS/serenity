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
import javax.swing.plaf.basic.BasicToolBarSeparatorUI;

import com.apple.laf.AquaUtils.*;

public class AquaToolBarSeparatorUI extends BasicToolBarSeparatorUI {
    private static final RecyclableSingleton<AquaToolBarSeparatorUI> instance = new RecyclableSingletonFromDefaultConstructor<AquaToolBarSeparatorUI>(AquaToolBarSeparatorUI.class);

    public static ComponentUI createUI(final JComponent c) {
        return instance.get();
    }

    public AquaToolBarSeparatorUI() {
        super();
    }

    BasicStroke dashedStroke = new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0.0f, new float[] { 1.0f, 2.0f }, 0.0f);

    public void paint(final Graphics g, final JComponent c) {
        g.setColor(c.getForeground());
        ((Graphics2D)g).setStroke(dashedStroke);

        final int width = c.getWidth();
        final int height = c.getHeight();
        if (((JToolBar.Separator)c).getOrientation() == SwingConstants.HORIZONTAL) {
            g.drawLine(2, height / 2, width - 3, height / 2);
        } else {
            g.drawLine(width / 2, 2, width / 2, height - 3);
        }
    }

    public Dimension getMinimumSize(final JComponent c) {
        final JToolBar.Separator sep = (JToolBar.Separator)c;
        if (sep.getOrientation() == SwingConstants.HORIZONTAL) {
            return new Dimension(1, 11);
        }
        return new Dimension(11, 1);
    }

    public Dimension getPreferredSize(final JComponent c) {
        final JToolBar.Separator sep = (JToolBar.Separator)c;
        if (sep.getOrientation() == SwingConstants.HORIZONTAL) {
            return new Dimension(1, 11);
        }
        return new Dimension(11, 1);
    }

    public Dimension getMaximumSize(final JComponent c) {
        final JToolBar.Separator sep = (JToolBar.Separator)c;
        if (sep.getOrientation() == SwingConstants.HORIZONTAL) {
            return new Dimension(Integer.MAX_VALUE, 11);
        }
        return new Dimension(11, Integer.MAX_VALUE);
    }
}
