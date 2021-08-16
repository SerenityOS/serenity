/*
 * Copyright (c) 2005, 2006, Oracle and/or its affiliates. All rights reserved.
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

package sun.tools.jconsole;

import java.awt.*;

import javax.accessibility.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.tree.*;

import sun.tools.jconsole.inspector.*;

import static java.lang.Math.*;

/**
 * Miscellaneous utility methods for JConsole
 */
public class Utilities {
    private static final String windowsLaF =
        "com.sun.java.swing.plaf.windows.WindowsLookAndFeel";

    public static void updateTransparency(JComponent comp) {
        LookAndFeel laf = UIManager.getLookAndFeel();
        boolean transparent = laf.getClass().getName().equals(windowsLaF);
        setTabbedPaneTransparency(comp, transparent);
    }

    private static void setTabbedPaneTransparency(JComponent comp, boolean transparent) {
        for (Component child : comp.getComponents()) {
            if (comp instanceof JTabbedPane) {
                setTransparency((JComponent)child, transparent);
            } else if (child instanceof JComponent) {
                setTabbedPaneTransparency((JComponent)child, transparent);
            }
        }
    }

    private static void setTransparency(JComponent comp, boolean transparent) {
        comp.setOpaque(!transparent);
        for (Component child : comp.getComponents()) {
            if (child instanceof JPanel ||
                child instanceof JSplitPane ||
                child instanceof JScrollPane ||
                child instanceof JViewport ||
                child instanceof JCheckBox) {

                setTransparency((JComponent)child, transparent);
            }
            if (child instanceof XTree) {
                XTree t = (XTree)child;
                DefaultTreeCellRenderer cr = (DefaultTreeCellRenderer)t.getCellRenderer();

                cr.setBackground(null);
                cr.setBackgroundNonSelectionColor(new Color(0, 0, 0, 1));
                t.setCellRenderer(cr);
                setTransparency((JComponent)child, transparent);
            }
        }
    }


    /**
     * A slightly modified border for JScrollPane to be used with a JTable inside
     * a JTabbedPane. It has only top part and the rest is clipped to make the
     * overall border less thick.
     * The top border helps differentiating the containing table from its container.
     */
    public static JScrollPane newTableScrollPane(JComponent comp) {
        return new TableScrollPane(comp);
    }

    @SuppressWarnings("serial")
    private static class TableScrollPane extends JScrollPane {
        public TableScrollPane(JComponent comp) {
            super(comp);
        }

        protected void paintBorder(Graphics g) {
            Border border = getBorder();
            if (border != null) {
                Insets insets = border.getBorderInsets(this);
                if (insets != null) {
                    Shape oldClip = g.getClip();
                    g.clipRect(0, 0, getWidth(), insets.top);
                    super.paintBorder(g);
                    g.setClip(oldClip);
                }
            }
        }
    }

    public static void setAccessibleName(Accessible comp, String name) {
        comp.getAccessibleContext().setAccessibleName(name);
    }

    public static void setAccessibleDescription(Accessible comp, String description) {
        comp.getAccessibleContext().setAccessibleDescription(description);
    }


    /**
     * Modifies color c1 to ensure it has acceptable contrast
     * relative to color c2.
     *
     * http://www.w3.org/TR/AERT#color-contrast
     * http://www.cs.rit.edu/~ncs/color/t_convert.html#RGB%20to%20YIQ%20&%20YIQ%20to%20RGB
     */
    public static Color ensureContrast(Color c1, Color c2) {
        double y1 = getColorBrightness(c1);
        double y2 = getColorBrightness(c2);

        if (abs(y1 - y2) < 125.0) {
            if (y2 < 128.0) {
                c1 = setColorBrightness(c1, y2 + 125.0);
            } else {
                c1 = setColorBrightness(c1, y2 - 125.0);
            }
        }

        return c1;
    }

    public static double getColorBrightness(Color c) {
        // Convert RGB -> YIQ and return the Y value
        return (c.getRed() * 0.299 + c.getGreen() * 0.587 + c.getBlue() * 0.114);
    }

    private static Color setColorBrightness(Color c, double y) {
        // Convert YIQ -> RGB
        double i = (c.getRed() * 0.596 - c.getGreen() * 0.275 - c.getBlue() * 0.321);
        double q = (c.getRed() * 0.212 - c.getGreen() * 0.523 + c.getBlue() * 0.311);

        // Keep values in legal range. This may reduce the
        // achieved contrast somewhat.
        int r = max(0, min(255, (int)round(y + i * 0.956 + q * 0.621)));
        int g = max(0, min(255, (int)round(y - i * 0.272 - q * 0.647)));
        int b = max(0, min(255, (int)round(y - i * 1.105 + q * 1.702)));

        return new Color(r, g, b);
    }

}
