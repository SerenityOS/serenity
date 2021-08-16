/*
 * Copyright (c) 1998, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.io.Serializable;

import javax.swing.AbstractButton;
import javax.swing.ButtonModel;
import javax.swing.Icon;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JInternalFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.plaf.ButtonUI;
import javax.swing.plaf.UIResource;

import sun.swing.MenuItemCheckIconFactory;
import sun.swing.SwingUtilities2;

import static com.sun.java.swing.plaf.windows.TMSchema.Part;
import static com.sun.java.swing.plaf.windows.TMSchema.State;
import static com.sun.java.swing.plaf.windows.XPStyle.Skin;

/**
 * Factory object that can vend Icons appropriate for the Windows {@literal L & F}.
 *
 * @author David Kloba
 * @author Georges Saab
 * @author Rich Schiavi
 */
@SuppressWarnings("serial") // Same-version serialization only
public class WindowsIconFactory implements Serializable
{
    private static Icon frame_closeIcon;
    private static Icon frame_iconifyIcon;
    private static Icon frame_maxIcon;
    private static Icon frame_minIcon;
    private static Icon frame_resizeIcon;
    private static Icon checkBoxIcon;
    private static Icon radioButtonIcon;
    private static Icon checkBoxMenuItemIcon;
    private static Icon radioButtonMenuItemIcon;
    private static Icon menuItemCheckIcon;
    private static Icon menuItemArrowIcon;
    private static Icon menuArrowIcon;
    private static VistaMenuItemCheckIconFactory menuItemCheckIconFactory;

    public static Icon getMenuItemCheckIcon() {
        if (menuItemCheckIcon == null) {
            menuItemCheckIcon = new MenuItemCheckIcon();
        }
        return menuItemCheckIcon;
    }

    public static Icon getMenuItemArrowIcon() {
        if (menuItemArrowIcon == null) {
            menuItemArrowIcon = new MenuItemArrowIcon();
        }
        return menuItemArrowIcon;
    }

    public static Icon getMenuArrowIcon() {
        if (menuArrowIcon == null) {
            menuArrowIcon = new MenuArrowIcon();
        }
        return menuArrowIcon;
    }

    public static Icon getCheckBoxIcon() {
        if (checkBoxIcon == null) {
            checkBoxIcon = new CheckBoxIcon();
        }
        return checkBoxIcon;
    }

    public static Icon getRadioButtonIcon() {
        if (radioButtonIcon == null) {
            radioButtonIcon = new RadioButtonIcon();
        }
        return radioButtonIcon;
    }

    public static Icon getCheckBoxMenuItemIcon() {
        if (checkBoxMenuItemIcon == null) {
            checkBoxMenuItemIcon = new CheckBoxMenuItemIcon();
        }
        return checkBoxMenuItemIcon;
    }

    public static Icon getRadioButtonMenuItemIcon() {
        if (radioButtonMenuItemIcon == null) {
            radioButtonMenuItemIcon = new RadioButtonMenuItemIcon();
        }
        return radioButtonMenuItemIcon;
    }

    static
    synchronized VistaMenuItemCheckIconFactory getMenuItemCheckIconFactory() {
        if (menuItemCheckIconFactory == null) {
            menuItemCheckIconFactory =
                new VistaMenuItemCheckIconFactory();
        }
        return menuItemCheckIconFactory;
    }

    public static Icon createFrameCloseIcon() {
        if (frame_closeIcon == null) {
            frame_closeIcon = new FrameButtonIcon(Part.WP_CLOSEBUTTON);
        }
        return frame_closeIcon;
    }

    public static Icon createFrameIconifyIcon() {
        if (frame_iconifyIcon == null) {
            frame_iconifyIcon = new FrameButtonIcon(Part.WP_MINBUTTON);
        }
        return frame_iconifyIcon;
    }

    public static Icon createFrameMaximizeIcon() {
        if (frame_maxIcon == null) {
            frame_maxIcon = new FrameButtonIcon(Part.WP_MAXBUTTON);
        }
        return frame_maxIcon;
    }

    public static Icon createFrameMinimizeIcon() {
        if (frame_minIcon == null) {
            frame_minIcon = new FrameButtonIcon(Part.WP_RESTOREBUTTON);
        }
        return frame_minIcon;
    }

    public static Icon createFrameResizeIcon() {
        if(frame_resizeIcon == null)
            frame_resizeIcon = new ResizeIcon();
        return frame_resizeIcon;
    }


    @SuppressWarnings("serial") // Same-version serialization only
    private static class FrameButtonIcon implements Icon, Serializable {
        private Part part;

        private FrameButtonIcon(Part part) {
            this.part = part;
        }

        public void paintIcon(Component c, Graphics g, int x0, int y0) {
            int width = getIconWidth();
            int height = getIconHeight();

            XPStyle xp = XPStyle.getXP();
            if (xp != null) {
                Skin skin = xp.getSkin(c, part);
                AbstractButton b = (AbstractButton)c;
                ButtonModel model = b.getModel();

                // Find out if frame is inactive
                JInternalFrame jif = (JInternalFrame)SwingUtilities.
                                        getAncestorOfClass(JInternalFrame.class, b);
                boolean jifSelected = (jif != null && jif.isSelected());

                State state;
                if (jifSelected) {
                    if (!model.isEnabled()) {
                        state = State.DISABLED;
                    } else if (model.isArmed() && model.isPressed()) {
                        state = State.PUSHED;
                    } else if (model.isRollover()) {
                        state = State.HOT;
                    } else {
                        state = State.NORMAL;
                    }
                } else {
                    if (!model.isEnabled()) {
                        state = State.INACTIVEDISABLED;
                    } else if (model.isArmed() && model.isPressed()) {
                        state = State.INACTIVEPUSHED;
                    } else if (model.isRollover()) {
                        state = State.INACTIVEHOT;
                    } else {
                        state = State.INACTIVENORMAL;
                    }
                }
                skin.paintSkin(g, 0, 0, width, height, state);
            } else {
                g.setColor(Color.black);
                int x = width / 12 + 2;
                int y = height / 5;
                int h = height - y * 2 - 1;
                int w = width * 3/4 -3;
                int thickness2 = Math.max(height / 8, 2);
                int thickness  = Math.max(width / 15, 1);
                if (part == Part.WP_CLOSEBUTTON) {
                    int lineWidth;
                    if      (width > 47) lineWidth = 6;
                    else if (width > 37) lineWidth = 5;
                    else if (width > 26) lineWidth = 4;
                    else if (width > 16) lineWidth = 3;
                    else if (width > 12) lineWidth = 2;
                    else                 lineWidth = 1;
                    y = height / 12 + 2;
                    if (lineWidth == 1) {
                        if (w % 2 == 1) { x++; w++; }
                        g.drawLine(x,     y, x+w-2, y+w-2);
                        g.drawLine(x+w-2, y, x,     y+w-2);
                    } else if (lineWidth == 2) {
                        if (w > 6) { x++; w--; }
                        g.drawLine(x,     y, x+w-2, y+w-2);
                        g.drawLine(x+w-2, y, x,     y+w-2);
                        g.drawLine(x+1,   y, x+w-1, y+w-2);
                        g.drawLine(x+w-1, y, x+1,   y+w-2);
                    } else {
                        x += 2; y++; w -= 2;
                        g.drawLine(x,     y,   x+w-1, y+w-1);
                        g.drawLine(x+w-1, y,   x,     y+w-1);
                        g.drawLine(x+1,   y,   x+w-1, y+w-2);
                        g.drawLine(x+w-2, y,   x,     y+w-2);
                        g.drawLine(x,     y+1, x+w-2, y+w-1);
                        g.drawLine(x+w-1, y+1, x+1,   y+w-1);
                        for (int i = 4; i <= lineWidth; i++) {
                            g.drawLine(x+i-2,   y,     x+w-1,   y+w-i+1);
                            g.drawLine(x,       y+i-2, x+w-i+1, y+w-1);
                            g.drawLine(x+w-i+1, y,     x,       y+w-i+1);
                            g.drawLine(x+w-1,   y+i-2, x+i-2,   y+w-1);
                        }
                    }
                } else if (part == Part.WP_MINBUTTON) {
                    g.fillRect(x, y+h-thickness2, w-w/3, thickness2);
                } else if (part == Part.WP_MAXBUTTON) {
                    g.fillRect(x, y, w, thickness2);
                    g.fillRect(x, y, thickness, h);
                    g.fillRect(x+w-thickness, y, thickness, h);
                    g.fillRect(x, y+h-thickness, w, thickness);
                } else if (part == Part.WP_RESTOREBUTTON) {
                    g.fillRect(x+w/3, y, w-w/3, thickness2);
                    g.fillRect(x+w/3, y, thickness, h/3);
                    g.fillRect(x+w-thickness, y, thickness, h-h/3);
                    g.fillRect(x+w-w/3, y+h-h/3-thickness, w/3, thickness);

                    g.fillRect(x, y+h/3, w-w/3, thickness2);
                    g.fillRect(x, y+h/3, thickness, h-h/3);
                    g.fillRect(x+w-w/3-thickness, y+h/3, thickness, h-h/3);
                    g.fillRect(x, y+h-thickness, w-w/3, thickness);
                }
            }
        }

        public int getIconWidth() {
            int width;
            if (XPStyle.getXP() != null) {
                // Fix for XP bug where sometimes these sizes aren't updated properly
                // Assume for now that height is correct and derive width using the
                // ratio from the uxtheme part
                width = UIManager.getInt("InternalFrame.titleButtonHeight") -2;
                Dimension d = XPStyle.getPartSize(Part.WP_CLOSEBUTTON, State.NORMAL);
                if (d != null && d.width != 0 && d.height != 0) {
                    width = (int) ((float) width * d.width / d.height);
                }
            } else {
                width = UIManager.getInt("InternalFrame.titleButtonWidth") -2;
            }
            if (XPStyle.getXP() != null) {
                width -= 2;
            }
            return width;
        }

        public int getIconHeight() {
            int height = UIManager.getInt("InternalFrame.titleButtonHeight")-4;
            return height;
        }
    }



        @SuppressWarnings("serial") // Same-version serialization only
        private static class ResizeIcon implements Icon, Serializable {
            public void paintIcon(Component c, Graphics g, int x, int y) {
                g.setColor(UIManager.getColor("InternalFrame.resizeIconHighlight"));
                g.drawLine(0, 11, 11, 0);
                g.drawLine(4, 11, 11, 4);
                g.drawLine(8, 11, 11, 8);

                g.setColor(UIManager.getColor("InternalFrame.resizeIconShadow"));
                g.drawLine(1, 11, 11, 1);
                g.drawLine(2, 11, 11, 2);
                g.drawLine(5, 11, 11, 5);
                g.drawLine(6, 11, 11, 6);
                g.drawLine(9, 11, 11, 9);
                g.drawLine(10, 11, 11, 10);
            }
            public int getIconWidth() { return 13; }
            public int getIconHeight() { return 13; }
        };

    @SuppressWarnings("serial") // Same-version serialization only
    private static class CheckBoxIcon implements Icon, Serializable
    {
        static final int csize = 13;
        public void paintIcon(Component c, Graphics g, int x, int y) {
            JCheckBox cb = (JCheckBox) c;
            ButtonModel model = cb.getModel();
            XPStyle xp = XPStyle.getXP();

            if (xp != null) {
                State state;
                if (model.isSelected()) {
                    state = State.CHECKEDNORMAL;
                    if (!model.isEnabled()) {
                        state = State.CHECKEDDISABLED;
                    } else if (model.isPressed() && model.isArmed()) {
                        state = State.CHECKEDPRESSED;
                    } else if (model.isRollover()) {
                        state = State.CHECKEDHOT;
                    }
                } else {
                    state = State.UNCHECKEDNORMAL;
                    if (!model.isEnabled()) {
                        state = State.UNCHECKEDDISABLED;
                    } else if (model.isPressed() && model.isArmed()) {
                        state = State.UNCHECKEDPRESSED;
                    } else if (model.isRollover()) {
                        state = State.UNCHECKEDHOT;
                    }
                }
                Part part = Part.BP_CHECKBOX;
                xp.getSkin(c, part).paintSkin(g, x, y, state);
            } else {
                // outer bevel
                if(!cb.isBorderPaintedFlat()) {
                    // Outer top/left
                    g.setColor(UIManager.getColor("CheckBox.shadow"));
                    g.drawLine(x, y, x+11, y);
                    g.drawLine(x, y+1, x, y+11);

                    // Outer bottom/right
                    g.setColor(UIManager.getColor("CheckBox.highlight"));
                    g.drawLine(x+12, y, x+12, y+12);
                    g.drawLine(x, y+12, x+11, y+12);

                    // Inner top.left
                    g.setColor(UIManager.getColor("CheckBox.darkShadow"));
                    g.drawLine(x+1, y+1, x+10, y+1);
                    g.drawLine(x+1, y+2, x+1, y+10);

                    // Inner bottom/right
                    g.setColor(UIManager.getColor("CheckBox.light"));
                    g.drawLine(x+1, y+11, x+11, y+11);
                    g.drawLine(x+11, y+1, x+11, y+10);

                    // inside box
                    if((model.isPressed() && model.isArmed()) || !model.isEnabled()) {
                        g.setColor(UIManager.getColor("CheckBox.background"));
                    } else {
                        g.setColor(UIManager.getColor("CheckBox.interiorBackground"));
                    }
                    g.fillRect(x+2, y+2, csize-4, csize-4);
                } else {
                    g.setColor(UIManager.getColor("CheckBox.shadow"));
                    g.drawRect(x+1, y+1, csize-3, csize-3);

                    if((model.isPressed() && model.isArmed()) || !model.isEnabled()) {
                        g.setColor(UIManager.getColor("CheckBox.background"));
                    } else {
                        g.setColor(UIManager.getColor("CheckBox.interiorBackground"));
                    }
                    g.fillRect(x+2, y+2, csize-4, csize-4);
                }

                if(model.isEnabled()) {
                    g.setColor(UIManager.getColor("CheckBox.foreground"));
                } else {
                    g.setColor(UIManager.getColor("CheckBox.shadow"));
                }

                // paint check
                if (model.isSelected()) {
                    if (SwingUtilities2.isScaledGraphics(g)) {
                        int[] xPoints = {3, 5, 9, 9, 5, 3};
                        int[] yPoints = {5, 7, 3, 5, 9, 7};
                        g.translate(x, y);
                        g.fillPolygon(xPoints, yPoints, 6);
                        g.drawPolygon(xPoints, yPoints, 6);
                        g.translate(-x, -y);
                    } else {
                        g.drawLine(x + 9, y + 3, x + 9, y + 3);
                        g.drawLine(x + 8, y + 4, x + 9, y + 4);
                        g.drawLine(x + 7, y + 5, x + 9, y + 5);
                        g.drawLine(x + 6, y + 6, x + 8, y + 6);
                        g.drawLine(x + 3, y + 7, x + 7, y + 7);
                        g.drawLine(x + 4, y + 8, x + 6, y + 8);
                        g.drawLine(x + 5, y + 9, x + 5, y + 9);
                        g.drawLine(x + 3, y + 5, x + 3, y + 5);
                        g.drawLine(x + 3, y + 6, x + 4, y + 6);
                    }
                }
            }
        }

        public int getIconWidth() {
            XPStyle xp = XPStyle.getXP();
            if (xp != null) {
                return xp.getSkin(null, Part.BP_CHECKBOX).getWidth();
            } else {
                return csize;
            }
        }

        public int getIconHeight() {
            XPStyle xp = XPStyle.getXP();
            if (xp != null) {
                return xp.getSkin(null, Part.BP_CHECKBOX).getHeight();
            } else {
                return csize;
            }
        }
    }

    @SuppressWarnings("serial") // Same-version serialization only
    private static class RadioButtonIcon implements Icon, UIResource, Serializable
    {
        public void paintIcon(Component c, Graphics g, int x, int y) {
            AbstractButton b = (AbstractButton) c;
            ButtonModel model = b.getModel();
            XPStyle xp = XPStyle.getXP();

            if (xp != null) {
                Part part = Part.BP_RADIOBUTTON;
                Skin skin = xp.getSkin(b, part);
                State state;
                int index = 0;
                if (model.isSelected()) {
                    state = State.CHECKEDNORMAL;
                    if (!model.isEnabled()) {
                        state = State.CHECKEDDISABLED;
                    } else if (model.isPressed() && model.isArmed()) {
                        state = State.CHECKEDPRESSED;
                    } else if (model.isRollover()) {
                        state = State.CHECKEDHOT;
                    }
                } else {
                    state = State.UNCHECKEDNORMAL;
                    if (!model.isEnabled()) {
                        state = State.UNCHECKEDDISABLED;
                    } else if (model.isPressed() && model.isArmed()) {
                        state = State.UNCHECKEDPRESSED;
                    } else if (model.isRollover()) {
                        state = State.UNCHECKEDHOT;
                    }
                }
                skin.paintSkin(g, x, y, state);
            } else {
                // fill interior
                if((model.isPressed() && model.isArmed()) || !model.isEnabled()) {
                    g.setColor(UIManager.getColor("RadioButton.background"));
                } else {
                    g.setColor(UIManager.getColor("RadioButton.interiorBackground"));
                }
                g.fillRect(x+2, y+2, 8, 8);


                boolean isScaledGraphics = SwingUtilities2.isScaledGraphics(g);

                if (isScaledGraphics) {

                    Graphics2D g2d = (Graphics2D) g;
                    Stroke oldStroke = g2d.getStroke();
                    g2d.setStroke(new BasicStroke(1.03f, BasicStroke.CAP_ROUND,
                                                  BasicStroke.JOIN_ROUND));
                    Object aaHint = g2d.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
                    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                         RenderingHints.VALUE_ANTIALIAS_ON);

                    // outter left arc
                    g.setColor(UIManager.getColor("RadioButton.shadow"));
                    g.drawArc(x, y, 11, 11, 45, 180);
                    // outter right arc
                    g.setColor(UIManager.getColor("RadioButton.highlight"));
                    g.drawArc(x, y, 11, 11, 45, -180);
                    // inner left arc
                    g.setColor(UIManager.getColor("RadioButton.darkShadow"));
                    g.drawArc(x + 1, y + 1, 9, 9, 45, 180);
                    // inner right arc
                    g.setColor(UIManager.getColor("RadioButton.light"));
                    g.drawArc(x + 1, y + 1, 9, 9, 45, -180);

                    g2d.setStroke(oldStroke);

                    if (model.isSelected()) {
                        if (model.isEnabled()) {
                            g.setColor(UIManager.getColor("RadioButton.foreground"));
                        } else {
                            g.setColor(UIManager.getColor("RadioButton.shadow"));
                        }
                        g.fillOval(x + 3, y + 3, 5, 5);
                    }
                    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, aaHint);

                } else {

                    // outter left arc
                    g.setColor(UIManager.getColor("RadioButton.shadow"));
                    g.drawLine(x+4, y+0, x+7, y+0);
                    g.drawLine(x+2, y+1, x+3, y+1);
                    g.drawLine(x+8, y+1, x+9, y+1);
                    g.drawLine(x+1, y+2, x+1, y+3);
                    g.drawLine(x+0, y+4, x+0, y+7);
                    g.drawLine(x+1, y+8, x+1, y+9);

                    // outter right arc
                    g.setColor(UIManager.getColor("RadioButton.highlight"));
                    g.drawLine(x+2, y+10, x+3, y+10);
                    g.drawLine(x+4, y+11, x+7, y+11);
                    g.drawLine(x+8, y+10, x+9, y+10);
                    g.drawLine(x+10, y+9, x+10, y+8);
                    g.drawLine(x+11, y+7, x+11, y+4);
                    g.drawLine(x+10, y+3, x+10, y+2);


                    // inner left arc
                    g.setColor(UIManager.getColor("RadioButton.darkShadow"));
                    g.drawLine(x+4, y+1, x+7, y+1);
                    g.drawLine(x+2, y+2, x+3, y+2);
                    g.drawLine(x+8, y+2, x+9, y+2);
                    g.drawLine(x+2, y+3, x+2, y+3);
                    g.drawLine(x+1, y+4, x+1, y+7);
                    g.drawLine(x+2, y+8, x+2, y+8);


                    // inner right arc
                    g.setColor(UIManager.getColor("RadioButton.light"));
                    g.drawLine(x+2,  y+9,  x+3,  y+9);
                    g.drawLine(x+4,  y+10, x+7,  y+10);
                    g.drawLine(x+8,  y+9,  x+9,  y+9);
                    g.drawLine(x+9,  y+8,  x+9,  y+8);
                    g.drawLine(x+10, y+7,  x+10, y+4);
                    g.drawLine(x+9,  y+3,  x+9,  y+3);


                     // indicate whether selected or not
                    if (model.isSelected()) {
                        if (model.isEnabled()) {
                            g.setColor(UIManager.getColor("RadioButton.foreground"));
                        } else {
                            g.setColor(UIManager.getColor("RadioButton.shadow"));
                        }
                        g.fillRect(x+4, y+5, 4, 2);
                        g.fillRect(x+5, y+4, 2, 4);
                    }
                }
            }
        }

        public int getIconWidth() {
            XPStyle xp = XPStyle.getXP();
            if (xp != null) {
                return xp.getSkin(null, Part.BP_RADIOBUTTON).getWidth();
            } else {
                return 13;
            }
        }

        public int getIconHeight() {
            XPStyle xp = XPStyle.getXP();
            if (xp != null) {
                return xp.getSkin(null, Part.BP_RADIOBUTTON).getHeight();
            } else {
                return 13;
            }
        }
    } // end class RadioButtonIcon


    @SuppressWarnings("serial") // Same-version serialization only
    private static class CheckBoxMenuItemIcon implements Icon, UIResource, Serializable
    {
        public void paintIcon(Component c, Graphics g, int x, int y) {
            AbstractButton b = (AbstractButton) c;
            ButtonModel model = b.getModel();
            boolean isSelected = model.isSelected();
            if (isSelected) {
                y = y - getIconHeight() / 2;
                g.drawLine(x+9, y+3, x+9, y+3);
                g.drawLine(x+8, y+4, x+9, y+4);
                g.drawLine(x+7, y+5, x+9, y+5);
                g.drawLine(x+6, y+6, x+8, y+6);
                g.drawLine(x+3, y+7, x+7, y+7);
                g.drawLine(x+4, y+8, x+6, y+8);
                g.drawLine(x+5, y+9, x+5, y+9);
                g.drawLine(x+3, y+5, x+3, y+5);
                g.drawLine(x+3, y+6, x+4, y+6);
            }
        }
        public int getIconWidth() { return 9; }
        public int getIconHeight() { return 9; }

    } // End class CheckBoxMenuItemIcon


    @SuppressWarnings("serial") // Same-version serialization only
    private static class RadioButtonMenuItemIcon implements Icon, UIResource, Serializable
    {
        public void paintIcon(Component c, Graphics g, int x, int y) {
            AbstractButton b = (AbstractButton) c;
            ButtonModel model = b.getModel();
            if (b.isSelected() == true) {
               g.fillRoundRect(x+3,y+3, getIconWidth()-6, getIconHeight()-6,
                               4, 4);
            }
        }
        public int getIconWidth() { return 12; }
        public int getIconHeight() { return 12; }

    } // End class RadioButtonMenuItemIcon


    @SuppressWarnings("serial") // Same-version serialization only
    private static class MenuItemCheckIcon implements Icon, UIResource, Serializable{
        public void paintIcon(Component c, Graphics g, int x, int y) {
            /* For debugging:
               Color oldColor = g.getColor();
            g.setColor(Color.orange);
            g.fill3DRect(x,y,getIconWidth(), getIconHeight(), true);
            g.setColor(oldColor);
            */
        }
        public int getIconWidth() { return 9; }
        public int getIconHeight() { return 9; }

    } // End class MenuItemCheckIcon

    @SuppressWarnings("serial") // Same-version serialization only
    private static class MenuItemArrowIcon implements Icon, UIResource, Serializable {
        public void paintIcon(Component c, Graphics g, int x, int y) {
            /* For debugging:
            Color oldColor = g.getColor();
            g.setColor(Color.green);
            g.fill3DRect(x,y,getIconWidth(), getIconHeight(), true);
            g.setColor(oldColor);
            */
        }
        public int getIconWidth() { return 4; }
        public int getIconHeight() { return 8; }

    } // End class MenuItemArrowIcon

    @SuppressWarnings("serial") // Same-version serialization only
    private static class MenuArrowIcon implements Icon, UIResource, Serializable {
        public void paintIcon(Component c, Graphics g, int x, int y) {
            XPStyle xp = XPStyle.getXP();
            if (WindowsMenuItemUI.isVistaPainting(xp)) {
                State state = State.NORMAL;
                if (c instanceof JMenuItem) {
                    state = ((JMenuItem) c).getModel().isEnabled()
                    ? State.NORMAL : State.DISABLED;
                }
                Skin skin = xp.getSkin(c, Part.MP_POPUPSUBMENU);
                if (WindowsGraphicsUtils.isLeftToRight(c)) {
                    skin.paintSkin(g, x, y, state);
                } else {
                    Graphics2D g2d = (Graphics2D)g.create();
                    g2d.translate(x + skin.getWidth(), y);
                    g2d.scale(-1, 1);
                    skin.paintSkin(g2d, 0, 0, state);
                    g2d.dispose();
                }
            } else {
                g.translate(x,y);
                if( WindowsGraphicsUtils.isLeftToRight(c) ) {
                    g.drawLine( 0, 0, 0, 7 );
                    g.drawLine( 1, 1, 1, 6 );
                    g.drawLine( 2, 2, 2, 5 );
                    g.drawLine( 3, 3, 3, 4 );
                } else {
                    g.drawLine( 4, 0, 4, 7 );
                    g.drawLine( 3, 1, 3, 6 );
                    g.drawLine( 2, 2, 2, 5 );
                    g.drawLine( 1, 3, 1, 4 );
                }
                g.translate(-x,-y);
            }
        }
        public int getIconWidth() {
            XPStyle xp = XPStyle.getXP();
            if (WindowsMenuItemUI.isVistaPainting(xp)) {
                Skin skin = xp.getSkin(null, Part.MP_POPUPSUBMENU);
                return skin.getWidth();
            } else {
                return 4;
            }
        }
        public int getIconHeight() {
            XPStyle xp = XPStyle.getXP();
            if (WindowsMenuItemUI.isVistaPainting(xp)) {
                Skin skin = xp.getSkin(null, Part.MP_POPUPSUBMENU);
                return skin.getHeight();
            } else {
                return 8;
            }
        }
    } // End class MenuArrowIcon

    static class VistaMenuItemCheckIconFactory
           implements MenuItemCheckIconFactory {
        private static final int OFFSET = 3;

        public Icon getIcon(JMenuItem component) {
            return new VistaMenuItemCheckIcon(component);
        }

        public boolean isCompatible(Object icon, String prefix) {
            return icon instanceof VistaMenuItemCheckIcon
              && ((VistaMenuItemCheckIcon) icon).type == getType(prefix);
        }

        public Icon getIcon(String type) {
            return new VistaMenuItemCheckIcon(type);
        }

        static int getIconWidth() {
            XPStyle xp = XPStyle.getXP();
            return ((xp != null) ? xp.getSkin(null, Part.MP_POPUPCHECK).getWidth() : 16)
                + 2 * OFFSET;
        }

        private static Class<? extends JMenuItem> getType(Component c) {
            Class<? extends JMenuItem> rv = null;
            if (c instanceof JCheckBoxMenuItem) {
                rv = JCheckBoxMenuItem.class;
            } else if (c instanceof JRadioButtonMenuItem) {
                rv = JRadioButtonMenuItem.class;
            } else if (c instanceof JMenu) {
                rv = JMenu.class;
            } else if (c instanceof JMenuItem) {
                rv = JMenuItem.class;
            }
            return rv;
        }

        private static Class<? extends JMenuItem> getType(String type) {
            Class<? extends JMenuItem> rv = null;
            if (type == "CheckBoxMenuItem") {
                rv = JCheckBoxMenuItem.class;
            } else if (type == "RadioButtonMenuItem") {
                rv = JRadioButtonMenuItem.class;
            } else if (type == "Menu") {
                rv = JMenu.class;
            } else if (type == "MenuItem") {
                rv = JMenuItem.class;
            } else {
                // this should never happen
                rv = JMenuItem.class;
            }
            return rv;
        }

        /**
         * CheckIcon for JMenuItem, JMenu, JCheckBoxMenuItem and
         * JRadioButtonMenuItem.
         * Note: to be used on Vista only.
         */
        @SuppressWarnings("serial") // Same-version serialization only
        private static class VistaMenuItemCheckIcon
              implements Icon, UIResource, Serializable {

            private final JMenuItem menuItem;
            private final Class<? extends JMenuItem> type;

            VistaMenuItemCheckIcon(JMenuItem menuItem) {
                this.type = getType(menuItem);
                this.menuItem = menuItem;
            }
            VistaMenuItemCheckIcon(String type) {
                this.type = getType(type);
                this.menuItem = null;
            }

            public int getIconHeight() {
                Icon lafIcon = getLaFIcon();
                if (lafIcon != null) {
                    return lafIcon.getIconHeight();
                }
                Icon icon = getIcon();
                int height = 0;
                if (icon != null) {
                    height = icon.getIconHeight();
                } else {
                    XPStyle xp = XPStyle.getXP();
                    if (xp != null) {
                        Skin skin = xp.getSkin(null, Part.MP_POPUPCHECK);
                        height = skin.getHeight();
                    } else {
                        height = 16;
                    }
                }
                height +=  2 * OFFSET;
                return height;
            }

            public int getIconWidth() {
                Icon lafIcon = getLaFIcon();
                if (lafIcon != null) {
                    return lafIcon.getIconWidth();
                }
                Icon icon = getIcon();
                int width = 0;
                if (icon != null) {
                    width = icon.getIconWidth() + 2 * OFFSET;
                } else {
                    width = VistaMenuItemCheckIconFactory.getIconWidth();
                }
                return width;
            }

            public void paintIcon(Component c, Graphics g, int x, int y) {
                Icon lafIcon = getLaFIcon();
                if (lafIcon != null) {
                    lafIcon.paintIcon(c, g, x, y);
                    return;
                }
                assert menuItem == null || c == menuItem;
                Icon icon = getIcon();
                if (type == JCheckBoxMenuItem.class
                      || type == JRadioButtonMenuItem.class) {
                    AbstractButton b = (AbstractButton) c;
                    if (b.isSelected()) {
                        Part backgroundPart = Part.MP_POPUPCHECKBACKGROUND;
                        Part part = Part.MP_POPUPCHECK;
                        State backgroundState;
                        State state;
                        if (isEnabled(c, null)) {
                            backgroundState =
                                (icon != null) ? State.BITMAP : State.NORMAL;
                            state = (type == JRadioButtonMenuItem.class)
                              ? State.BULLETNORMAL
                              : State.CHECKMARKNORMAL;
                        } else {
                            backgroundState = State.DISABLEDPUSHED;
                            state =
                                (type == JRadioButtonMenuItem.class)
                                  ? State.BULLETDISABLED
                                  : State.CHECKMARKDISABLED;
                        }
                        XPStyle xp = XPStyle.getXP();
                        if (xp != null) {
                            Skin skin;
                            skin =  xp.getSkin(c, backgroundPart);
                            skin.paintSkin(g, x, y,
                                getIconWidth(), getIconHeight(), backgroundState);
                            if (icon == null) {
                                skin = xp.getSkin(c, part);
                                skin.paintSkin(g, x + OFFSET, y + OFFSET, state);
                            }
                        }
                    }
                }
                if (icon != null) {
                    icon.paintIcon(c, g, x + OFFSET, y + OFFSET);
                }
            }
            private static WindowsMenuItemUIAccessor getAccessor(
                    JMenuItem menuItem) {
                WindowsMenuItemUIAccessor rv = null;
                ButtonUI uiObject = (menuItem != null) ? menuItem.getUI()
                        : null;
                if (uiObject instanceof WindowsMenuItemUI) {
                    rv = ((WindowsMenuItemUI) uiObject).accessor;
                } else if (uiObject instanceof WindowsMenuUI) {
                    rv = ((WindowsMenuUI) uiObject).accessor;
                } else if (uiObject instanceof WindowsCheckBoxMenuItemUI) {
                    rv = ((WindowsCheckBoxMenuItemUI) uiObject).accessor;
                } else if (uiObject instanceof WindowsRadioButtonMenuItemUI) {
                    rv = ((WindowsRadioButtonMenuItemUI) uiObject).accessor;
                }
                return rv;
            }

            private static boolean isEnabled(Component  c, State state) {
                if (state == null && c instanceof JMenuItem) {
                    WindowsMenuItemUIAccessor accessor =
                        getAccessor((JMenuItem) c);
                    if (accessor != null) {
                        state = accessor.getState((JMenuItem) c);
                    }
                }
                if (state == null) {
                    if (c != null) {
                        return c.isEnabled();
                    } else {
                        return true;
                    }
                } else {
                    return (state != State.DISABLED)
                        && (state != State.DISABLEDHOT)
                        && (state != State.DISABLEDPUSHED);
                }
            }
            private Icon getIcon() {
                Icon rv = null;
                if (menuItem == null) {
                    return rv;
                }
                WindowsMenuItemUIAccessor accessor =
                    getAccessor(menuItem);
                State state = (accessor != null) ? accessor.getState(menuItem)
                        : null;
                if (isEnabled(menuItem, null)) {
                    if (state == State.PUSHED) {
                        rv = menuItem.getPressedIcon();
                    } else {
                        rv = menuItem.getIcon();
                    }
                } else {
                    rv = menuItem.getDisabledIcon();
                }
                return rv;
            }
            /**
             * Check if developer changed icon in the UI table.
             *
             * @return the icon to use or {@code null} if the current one is to
             * be used
             */
            private Icon getLaFIcon() {
                // use icon from the UI table if it does not match this one.
                Icon rv = (Icon) UIManager.getDefaults().get(typeToString(type));
                if (rv instanceof VistaMenuItemCheckIcon
                      && ((VistaMenuItemCheckIcon) rv).type == type) {
                    rv = null;
                }
                return rv;
            }

            private static String typeToString(
                    Class<? extends JMenuItem> type) {
                assert type == JMenuItem.class
                    || type == JMenu.class
                    || type == JCheckBoxMenuItem.class
                    || type == JRadioButtonMenuItem.class;
                StringBuilder sb = new StringBuilder(type.getName());
                // remove package name, dot and the first character
                sb.delete(0, sb.lastIndexOf("J") + 1);
                sb.append(".checkIcon");
                return sb.toString();
            }
        }
    } // End class VistaMenuItemCheckIconFactory
}
