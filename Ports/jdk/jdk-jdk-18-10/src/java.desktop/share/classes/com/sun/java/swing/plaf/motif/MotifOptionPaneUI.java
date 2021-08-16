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
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicOptionPaneUI;

/**
 * Provides the CDE/Motif look and feel for a JOptionPane.
 *
 * @author Scott Violet
 */
public class MotifOptionPaneUI extends BasicOptionPaneUI
{
    /**
      * Creates a new MotifOptionPaneUI instance.
      */
    public static ComponentUI createUI(JComponent x) {
        return new MotifOptionPaneUI();
    }

    /**
     * Creates and returns a Container containin the buttons. The buttons
     * are created by calling <code>getButtons</code>.
     */
    protected Container createButtonArea() {
        Container          b = super.createButtonArea();

        if(b != null && b.getLayout() instanceof ButtonAreaLayout) {
            ((ButtonAreaLayout)b.getLayout()).setCentersChildren(false);
        }
        return b;
    }

    /**
     * Returns null, CDE/Motif does not impose a minimum size.
     */
    public Dimension getMinimumOptionPaneSize() {
        return null;
    }

    @SuppressWarnings("serial") // anonymous class
    protected Container createSeparator() {
        return new JPanel() {

            public Dimension getPreferredSize() {
                return new Dimension(10, 2);
            }

            public void paint(Graphics g) {
                int width = getWidth();
                g.setColor(Color.darkGray);
                g.drawLine(0, 0, width, 0);
                g.setColor(Color.white);
                g.drawLine(0, 1, width, 1);
            }
        };
    }

    /**
     * Creates and adds a JLabel representing the icon returned from
     * <code>getIcon</code> to <code>top</code>. This is messaged from
     * <code>createMessageArea</code>
     */
    protected void addIcon(Container top) {
        /* Create the icon. */
        Icon                  sideIcon = getIcon();

        if (sideIcon != null) {
            JLabel            iconLabel = new JLabel(sideIcon);

            iconLabel.setVerticalAlignment(SwingConstants.CENTER);
            top.add(iconLabel, "West");
        }
    }

}
