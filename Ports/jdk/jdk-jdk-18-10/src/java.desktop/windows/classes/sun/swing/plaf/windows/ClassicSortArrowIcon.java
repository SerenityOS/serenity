/*
 * Copyright (c) 2005, 2014, Oracle and/or its affiliates. All rights reserved.
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
package sun.swing.plaf.windows;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.io.Serializable;
import javax.swing.Icon;
import javax.swing.UIManager;
import javax.swing.plaf.UIResource;

/**
 * Classic sort icons.
 *
 */
@SuppressWarnings("serial") // JDK-implementation class
public class ClassicSortArrowIcon implements Icon, UIResource, Serializable{
    private static final int X_OFFSET = 9;
    private boolean ascending;

    public ClassicSortArrowIcon(boolean ascending) {
        this.ascending = ascending;
    }

    public void paintIcon(Component c, Graphics g, int x, int y) {
        x += X_OFFSET;
        if (ascending) {
            g.setColor(UIManager.getColor("Table.sortIconHighlight"));
            drawSide(g, x + 3, y, -1);

            g.setColor(UIManager.getColor("Table.sortIconLight"));
            drawSide(g, x + 4, y, 1);

            g.fillRect(x + 1, y + 6, 6, 1);
        }
        else {
            g.setColor(UIManager.getColor("Table.sortIconHighlight"));
            drawSide(g, x + 3, y + 6, -1);
            g.fillRect(x + 1, y, 6, 1);

            g.setColor(UIManager.getColor("Table.sortIconLight"));
            drawSide(g, x + 4, y + 6, 1);
        }
    }

    private void drawSide(Graphics g, int x, int y, int xIncrement) {
        int yIncrement = 2;
        if (ascending) {
            g.fillRect(x, y, 1, 2);
            y++;
        }
        else {
            g.fillRect(x, --y, 1, 2);
            yIncrement = -2;
            y -= 2;
        }
        x += xIncrement;
        for (int i = 0; i < 2; i++) {
            g.fillRect(x, y, 1, 3);
            x += xIncrement;
            y += yIncrement;
        }
        if (!ascending) {
            y++;
        }
        g.fillRect(x, y, 1, 2);
    }

    public int getIconWidth() {
        return X_OFFSET + 8;
    }
    public int getIconHeight() {
        return 9;
    }
}
