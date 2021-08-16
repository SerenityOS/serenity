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
package sun.swing.icon;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.io.Serializable;
import javax.swing.Icon;
import javax.swing.UIManager;
import javax.swing.plaf.UIResource;

/**
 * Sorting icon.
 *
 */
@SuppressWarnings("serial") // JDK-implementation class
public class SortArrowIcon implements Icon, UIResource, Serializable {
    // Height of the arrow, the width is ARROW_HEIGHT
    private static final int ARROW_HEIGHT = 5;

    // Amount of pad to left of arrow
    private static final int X_PADDING = 7;

    // Sort direction
    private boolean ascending;

    // The Color to use, may be null indicating colorKey should be used
    private Color color;

    // If non-null indicates the color should come from the UIManager with
    // this key.
    private String colorKey;

    /**
     * Creates a <code>SortArrowIcon</code>.
     *
     * @param ascending if true, icon respresenting ascending sort, otherwise
     *        descending
     * @param color the color to render the icon
     */
    public SortArrowIcon(boolean ascending, Color color) {
        this.ascending = ascending;
        this.color = color;
        if (color == null) {
            throw new IllegalArgumentException();
        }
    }

    /**
     * Creates a <code>SortArrowIcon</code>.
     *
     * @param ascending if true, icon respresenting ascending sort, otherwise
     *        descending
     * @param colorKey the key used to find color in UIManager
     */
    public SortArrowIcon(boolean ascending, String colorKey) {
        this.ascending = ascending;
        this.colorKey = colorKey;
        if (colorKey == null) {
            throw new IllegalArgumentException();
        }
    }

    public void paintIcon(Component c, Graphics g, int x, int y) {
        g.setColor(getColor());
        int startX = X_PADDING + x + ARROW_HEIGHT / 2;
        if (ascending) {
            int startY = y;
            g.fillRect(startX, startY, 1, 1);
            for (int line = 1; line < ARROW_HEIGHT; line++) {
                g.fillRect(startX - line, startY + line,
                           line + line + 1, 1);
            }
        }
        else {
            int startY = y + ARROW_HEIGHT - 1;
            g.fillRect(startX, startY, 1, 1);
            for (int line = 1; line < ARROW_HEIGHT; line++) {
                g.fillRect(startX - line, startY - line,
                           line + line + 1, 1);
            }
        }
    }

    public int getIconWidth() {
        return X_PADDING + ARROW_HEIGHT * 2;
    }

    public int getIconHeight() {
        return ARROW_HEIGHT + 2;
    }

    private Color getColor() {
        if (color != null) {
            return color;
        }
        return UIManager.getColor(colorKey);
    }
}
