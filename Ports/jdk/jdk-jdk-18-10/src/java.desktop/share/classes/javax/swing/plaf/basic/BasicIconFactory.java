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

package javax.swing.plaf.basic;

import javax.swing.*;
import javax.swing.plaf.UIResource;

import java.awt.Graphics;
import java.awt.Color;
import java.awt.Component;
import java.awt.Polygon;
import java.io.Serializable;

/**
 * Factory object that can vend Icons appropriate for the basic L &amp; F.
 * <p>
 * <strong>Warning:</strong>
 * Serialized objects of this class will not be compatible with
 * future Swing releases. The current serialization support is
 * appropriate for short term storage or RMI between applications running
 * the same version of Swing.  As of 1.4, support for long term storage
 * of all JavaBeans
 * has been added to the <code>java.beans</code> package.
 * Please see {@link java.beans.XMLEncoder}.
 *
 * @author David Kloba
 * @author Georges Saab
 */
@SuppressWarnings("serial") // Same-version serialization only
public class BasicIconFactory implements Serializable
{
    private static Icon frame_icon;
    private static Icon checkBoxIcon;
    private static Icon radioButtonIcon;
    private static Icon checkBoxMenuItemIcon;
    private static Icon radioButtonMenuItemIcon;
    private static Icon menuItemCheckIcon;
    private static Icon menuItemArrowIcon;
    private static Icon menuArrowIcon;

    /**
     * Constructs a {@code BasicIconFactory}.
     */
    public BasicIconFactory() {}

    /**
     * Returns a menu item check icon.
     *
     * @return a menu item check icon
     */
    public static Icon getMenuItemCheckIcon() {
        if (menuItemCheckIcon == null) {
            menuItemCheckIcon = new MenuItemCheckIcon();
        }
        return menuItemCheckIcon;
    }

    /**
     * Returns a menu item arrow icon.
     *
     * @return a menu item arrow icon
     */
    public static Icon getMenuItemArrowIcon() {
        if (menuItemArrowIcon == null) {
            menuItemArrowIcon = new MenuItemArrowIcon();
        }
        return menuItemArrowIcon;
    }

    /**
     * Returns a menu arrow icon.
     *
     * @return a menu arrow icon
     */
    public static Icon getMenuArrowIcon() {
        if (menuArrowIcon == null) {
            menuArrowIcon = new MenuArrowIcon();
        }
        return menuArrowIcon;
    }

    /**
     * Returns a check box icon.
     *
     * @return a check box icon
     */
    public static Icon getCheckBoxIcon() {
        if (checkBoxIcon == null) {
            checkBoxIcon = new CheckBoxIcon();
        }
        return checkBoxIcon;
    }

    /**
     * Returns a radio button icon.
     *
     * @return a radio button icon
     */
    public static Icon getRadioButtonIcon() {
        if (radioButtonIcon == null) {
            radioButtonIcon = new RadioButtonIcon();
        }
        return radioButtonIcon;
    }

    /**
     * Returns a check box menu item icon.
     *
     * @return a check box menu item icon
     */
    public static Icon getCheckBoxMenuItemIcon() {
        if (checkBoxMenuItemIcon == null) {
            checkBoxMenuItemIcon = new CheckBoxMenuItemIcon();
        }
        return checkBoxMenuItemIcon;
    }

    /**
     * Returns a radio button menu item icon.
     *
     * @return a radio button menu item icon
     */
    public static Icon getRadioButtonMenuItemIcon() {
        if (radioButtonMenuItemIcon == null) {
            radioButtonMenuItemIcon = new RadioButtonMenuItemIcon();
        }
        return radioButtonMenuItemIcon;
    }

    /**
     * Returns an empty frame icon.
     *
     * @return an empty frame icon
     */
    public static Icon createEmptyFrameIcon() {
        if(frame_icon == null)
            frame_icon = new EmptyFrameIcon();
        return frame_icon;
    }

    private static class EmptyFrameIcon implements Icon, Serializable {
        int height = 16;
        int width = 14;
        public void paintIcon(Component c, Graphics g, int x, int y) {
        }
        public int getIconWidth() { return width; }
        public int getIconHeight() { return height; }
    };

    private static class CheckBoxIcon implements Icon, Serializable
    {
        static final int csize = 13;
        public void paintIcon(Component c, Graphics g, int x, int y) {
        }

        public int getIconWidth() {
            return csize;
        }

        public int getIconHeight() {
            return csize;
        }
    }

    private static class RadioButtonIcon implements Icon, UIResource, Serializable
    {
        public void paintIcon(Component c, Graphics g, int x, int y) {
        }

        public int getIconWidth() {
            return 13;
        }

        public int getIconHeight() {
            return 13;
        }
    } // end class RadioButtonIcon


    private static class CheckBoxMenuItemIcon implements Icon, UIResource, Serializable
    {
        public void paintIcon(Component c, Graphics g, int x, int y) {
            AbstractButton b = (AbstractButton) c;
            ButtonModel model = b.getModel();
            boolean isSelected = model.isSelected();
            if (isSelected) {
                g.drawLine(x+7, y+1, x+7, y+3);
                g.drawLine(x+6, y+2, x+6, y+4);
                g.drawLine(x+5, y+3, x+5, y+5);
                g.drawLine(x+4, y+4, x+4, y+6);
                g.drawLine(x+3, y+5, x+3, y+7);
                g.drawLine(x+2, y+4, x+2, y+6);
                g.drawLine(x+1, y+3, x+1, y+5);
            }
        }
        public int getIconWidth() { return 9; }
        public int getIconHeight() { return 9; }

    } // End class CheckBoxMenuItemIcon


    private static class RadioButtonMenuItemIcon implements Icon, UIResource, Serializable
    {
        public void paintIcon(Component c, Graphics g, int x, int y) {
            AbstractButton b = (AbstractButton) c;
            ButtonModel model = b.getModel();
            if (b.isSelected() == true) {
                g.fillOval(x+1, y+1, getIconWidth(), getIconHeight());
            }
        }
        public int getIconWidth() { return 6; }
        public int getIconHeight() { return 6; }

    } // End class RadioButtonMenuItemIcon


    private static class MenuItemCheckIcon implements Icon, UIResource, Serializable{
        public void paintIcon(Component c, Graphics g, int x, int y) {
        }
        public int getIconWidth() { return 9; }
        public int getIconHeight() { return 9; }

    } // End class MenuItemCheckIcon

    private static class MenuItemArrowIcon implements Icon, UIResource, Serializable {
        public void paintIcon(Component c, Graphics g, int x, int y) {
        }
        public int getIconWidth() { return 4; }
        public int getIconHeight() { return 8; }

    } // End class MenuItemArrowIcon

    private static class MenuArrowIcon implements Icon, UIResource, Serializable {
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Polygon p = new Polygon();
            p.addPoint(x, y);
            p.addPoint(x+getIconWidth(), y+getIconHeight()/2);
            p.addPoint(x, y+getIconHeight());
            g.fillPolygon(p);

        }
        public int getIconWidth() { return 4; }
        public int getIconHeight() { return 8; }
    } // End class MenuArrowIcon
}
