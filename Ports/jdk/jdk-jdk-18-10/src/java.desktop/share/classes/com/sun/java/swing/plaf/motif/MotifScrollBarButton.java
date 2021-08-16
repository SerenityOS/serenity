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

import javax.swing.UIManager;
import javax.swing.plaf.basic.BasicArrowButton;

/**
 * Motif scroll bar button.
 */
@SuppressWarnings("serial") // Superclass is not serializable across versions
public class MotifScrollBarButton extends BasicArrowButton
{
    private Color darkShadow = UIManager.getColor("controlShadow");
    private Color lightShadow = UIManager.getColor("controlLtHighlight");


    public MotifScrollBarButton(int direction)
    {
        super(direction);

        switch (direction) {
        case NORTH:
        case SOUTH:
        case EAST:
        case WEST:
            this.direction = direction;
            break;
        default:
            throw new IllegalArgumentException("invalid direction");
        }

        setRequestFocusEnabled(false);
        setOpaque(true);
        setBackground(UIManager.getColor("ScrollBar.background"));
        setForeground(UIManager.getColor("ScrollBar.foreground"));
    }


    public Dimension getPreferredSize() {
        switch (direction) {
        case NORTH:
        case SOUTH:
            return new Dimension(11, 12);
        case EAST:
        case WEST:
        default:
            return new Dimension(12, 11);
        }
    }

    public Dimension getMinimumSize() {
        return getPreferredSize();
    }

    public Dimension getMaximumSize() {
        return getPreferredSize();
    }

    public boolean isFocusTraversable() {
        return false;
    }

    public void paint(Graphics g)
    {
        int w = getWidth();
        int h = getHeight();

        if (isOpaque()) {
            g.setColor(getBackground());
            g.fillRect(0, 0, w, h);
        }

        boolean isPressed = getModel().isPressed();
        Color lead = (isPressed) ? darkShadow : lightShadow;
        Color trail = (isPressed) ? lightShadow : darkShadow;
        Color fill = getBackground();

        int cx = w / 2;
        int cy = h / 2;
        int s = Math.min(w, h);

        switch (direction) {
        case NORTH:
            g.setColor(fill);
            g.fillPolygon(new int[]{cx, 0, s - 1}, new int[]{0, s - 1, s - 1}, 3);
            g.setColor(trail);
            g.drawLine(cx, 0, s - 1, s - 2);
            g.drawLine(0, s - 1, s - 1, s - 1);
            g.drawLine(s - 1, s - 2, s - 1, s - 1); // corner
            g.setColor(lead);
            g.drawLine(cx, 0, 0, s - 2);
            g.drawLine(cx, 0, cx, 0); // corner
            g.drawLine(0, s - 1, 0, s - 1); // corner
            break;

        case SOUTH:
            g.setColor(fill);
            g.fillPolygon(new int[]{0, s - 1, cx}, new int[]{1, 1, s}, 3);
            g.setColor(trail);
            g.drawLine(s - 1, 2, cx, s);
            g.drawLine(s - 1, 2, s - 1, 2); // corner
            g.setColor(lead);
            g.drawLine(0, 2, cx, s);
            g.drawLine(0, 1, s - 1, 1);
            g.drawLine(0, 1, 0, 2);
            g.setColor(trail);
            g.drawLine(cx, s, cx, s); // corner
            break;

        case EAST:
            g.setColor(fill);
            g.fillPolygon(new int[]{1, s, 1}, new int[]{0, cy, s}, 3);
            g.setColor(trail);
            g.drawLine(1, s, s, cy);
            g.drawLine(2, s, 2, s); // corner
            g.setColor(lead);
            g.drawLine(1, 0, 1, s);
            g.drawLine(2, 0, s, cy);
            g.drawLine(2, 0, 2, 0); // corner
            g.drawLine(s, cy, s, cy);
            break;

        case WEST:
            g.setColor(fill);
            g.fillPolygon(new int[]{0, s - 1, s - 1}, new int[]{cy, 0, s}, 3);
            g.drawLine(s - 1, 0, s - 1, s);
            g.setColor(trail);
            g.drawLine(0, cy, s - 1, s);
            g.drawLine(s - 1, 0, s - 1, s);
            g.setColor(lead);
            g.drawLine(0, cy, s - 2, 0);
            g.drawLine(s - 2, 0, s - 1, 0); // corner
            g.setColor(trail);
            g.drawLine(0, cy, 0, cy); // corner
            break;
        }
    }
}
