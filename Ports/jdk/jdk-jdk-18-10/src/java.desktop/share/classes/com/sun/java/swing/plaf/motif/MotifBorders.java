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
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;

import javax.swing.AbstractButton;
import javax.swing.ButtonModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JInternalFrame;
import javax.swing.JMenuBar;
import javax.swing.JPopupMenu;
import javax.swing.UIManager;
import javax.swing.border.AbstractBorder;
import javax.swing.plaf.UIResource;

import sun.swing.SwingUtilities2;

/**
 * Factory object that can vend Icons appropriate for the basic {@literal L & F}.
 *
 * @author Amy Fowler
 */
public class MotifBorders {

    @SuppressWarnings("serial") // Superclass is not serializable across versions
    public static class BevelBorder extends AbstractBorder implements UIResource {
        private Color darkShadow = UIManager.getColor("controlShadow");
        private Color lightShadow = UIManager.getColor("controlLtHighlight");
        private boolean isRaised;

        public BevelBorder(boolean isRaised, Color darkShadow, Color lightShadow) {
            this.isRaised = isRaised;
            this.darkShadow = darkShadow;
            this.lightShadow = lightShadow;
        }

        public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            g.setColor((isRaised) ? lightShadow : darkShadow);
            g.drawLine(x, y, x+w-1, y);           // top
            g.drawLine(x, y+h-1, x, y+1);         // left

            g.setColor((isRaised) ? darkShadow : lightShadow);
            g.drawLine(x+1, y+h-1, x+w-1, y+h-1); // bottom
            g.drawLine(x+w-1, y+h-1, x+w-1, y+1); // right
        }

        public Insets getBorderInsets(Component c, Insets insets) {
            insets.set(1, 1, 1, 1);
            return insets;
        }

        public boolean isOpaque(Component c) {
            return true;
        }

    }


    @SuppressWarnings("serial") // Superclass is not serializable across versions
    public static class FocusBorder extends AbstractBorder implements UIResource {
        private Color focus;
        private Color control;

        public FocusBorder(Color control, Color focus) {
            this.control = control;
            this.focus = focus;
        }

        public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            if (c.hasFocus()) {
                g.setColor(focus);
                g.drawRect(x, y, w-1, h-1);
            } else {
                g.setColor(control);
                g.drawRect(x, y, w-1, h-1);
            }
        }

        public Insets getBorderInsets(Component c, Insets insets) {
            insets.set(1, 1, 1, 1);
            return insets;
        }
    }


    @SuppressWarnings("serial") // Superclass is not serializable across versions
    public static class ButtonBorder extends AbstractBorder implements UIResource {
        protected Color focus = UIManager.getColor("activeCaptionBorder");
        protected Color shadow = UIManager.getColor("Button.shadow");
        protected Color highlight = UIManager.getColor("Button.light");
        protected Color darkShadow;

        public ButtonBorder(Color shadow, Color highlight, Color darkShadow, Color focus) {
            this.shadow = shadow;
            this.highlight = highlight;
            this.darkShadow = darkShadow;
            this.focus = focus;
        }

        public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            boolean isPressed = false;
            boolean hasFocus = false;
            boolean canBeDefault = false;
            boolean isDefault = false;

            if (c instanceof AbstractButton) {
                AbstractButton b = (AbstractButton)c;
                ButtonModel model = b.getModel();

                isPressed = (model.isArmed() && model.isPressed());
                hasFocus = (model.isArmed() && isPressed) ||
                           (b.isFocusPainted() && b.hasFocus());
                if (b instanceof JButton) {
                    canBeDefault = ((JButton)b).isDefaultCapable();
                    isDefault = ((JButton)b).isDefaultButton();
                }
            }
            int bx1 = x+1;
            int by1 = y+1;
            int bx2 = x+w-2;
            int by2 = y+h-2;

            if (canBeDefault) {
                if (isDefault) {
                    g.setColor(shadow);
                    g.drawLine(x+3, y+3, x+3, y+h-4);
                    g.drawLine(x+3, y+3, x+w-4, y+3);

                    g.setColor(highlight);
                    g.drawLine(x+4, y+h-4, x+w-4, y+h-4);
                    g.drawLine(x+w-4, y+3, x+w-4, y+h-4);
                }
                bx1 +=6;
                by1 += 6;
                bx2 -= 6;
                by2 -= 6;
            }

            if (hasFocus) {
                g.setColor(focus);
                if (isDefault) {
                    g.drawRect(x, y, w-1, h-1);
                } else {
                    g.drawRect(bx1-1, by1-1, bx2-bx1+2, by2-by1+2);
                }
            }

            g.setColor(isPressed? shadow : highlight);
            g.drawLine(bx1, by1, bx2, by1);
            g.drawLine(bx1, by1, bx1, by2);

            g.setColor(isPressed? highlight : shadow);
            g.drawLine(bx2, by1+1, bx2, by2);
            g.drawLine(bx1+1, by2, bx2, by2);
        }

        public Insets getBorderInsets(Component c, Insets insets) {
            int thickness = (c instanceof JButton && ((JButton)c).isDefaultCapable())? 8 : 2;
            insets.set(thickness, thickness, thickness, thickness);
            return insets;
        }

    }

    @SuppressWarnings("serial") // Superclass is not serializable across versions
    public static class ToggleButtonBorder extends ButtonBorder {

        public ToggleButtonBorder(Color shadow, Color highlight, Color darkShadow, Color focus) {
             super(shadow, highlight, darkShadow, focus);
        }

        public void paintBorder(Component c, Graphics g, int x, int y,
                            int width, int height) {
            if (c instanceof AbstractButton) {
                AbstractButton b = (AbstractButton)c;
                ButtonModel model = b.getModel();

                if (model.isArmed() && model.isPressed() || model.isSelected()) {
                    drawBezel(g, x, y, width, height,
                              (model.isPressed() || model.isSelected()),
                              b.isFocusPainted() && b.hasFocus(), shadow, highlight, darkShadow, focus);
                } else {
                    drawBezel(g, x, y, width, height,
                              false, b.isFocusPainted() && b.hasFocus(),
                              shadow, highlight, darkShadow, focus);
                }
            } else {
                drawBezel(g, x, y, width, height, false, false,
                          shadow, highlight, darkShadow, focus);
            }
        }

        public Insets getBorderInsets(Component c, Insets insets) {
            insets.set(2, 2, 3, 3);
            return insets;
        }
    }

    @SuppressWarnings("serial") // Superclass is not serializable across versions
    public static class MenuBarBorder extends ButtonBorder {

        public MenuBarBorder(Color shadow, Color highlight, Color darkShadow, Color focus) {
            super(shadow, highlight, darkShadow, focus);
        }

        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            if (!(c instanceof JMenuBar)) {
                return;
            }
            JMenuBar menuBar = (JMenuBar)c;
            if (menuBar.isBorderPainted() == true) {
                // this draws the MenuBar border
                Dimension size = menuBar.getSize();
                drawBezel(g,x,y,size.width,size.height,false,false,
                          shadow, highlight, darkShadow, focus);
            }
        }

        public Insets getBorderInsets(Component c, Insets insets) {
            insets.set(6, 6, 6, 6);
            return insets;
        }
    }

    @SuppressWarnings("serial") // Superclass is not serializable across versions
    public static class FrameBorder extends AbstractBorder implements UIResource {

        JComponent jcomp;
        Color frameHighlight;
        Color frameColor;
        Color frameShadow;

        // The width of the border
        public static final int BORDER_SIZE = 5;

        /** Constructs an FrameBorder for the JComponent <b>comp</b>.
        */
        public FrameBorder(JComponent comp) {
            jcomp = comp;
        }

        /** Sets the FrameBorder's JComponent.
      */
        public void setComponent(JComponent comp) {
            jcomp = comp;
        }

        /** Returns the FrameBorder's JComponent.
          * @see #setComponent
          */
        public JComponent component() {
            return jcomp;
        }

        protected Color getFrameHighlight() {
            return frameHighlight;
        }

        protected Color getFrameColor() {
            return frameColor;
        }

        protected Color getFrameShadow() {
            return frameShadow;
        }

        public Insets getBorderInsets(Component c, Insets newInsets) {
            newInsets.set(BORDER_SIZE, BORDER_SIZE, BORDER_SIZE, BORDER_SIZE);
            return newInsets;
        }

       /** Draws the FrameBorder's top border.
         */
        protected boolean drawTopBorder(Component c, Graphics g,
                                    int x, int y, int width, int height) {
            Rectangle titleBarRect = new Rectangle(x, y, width, BORDER_SIZE);
            if (!g.getClipBounds().intersects(titleBarRect)) {
                return false;
            }

            int maxX = width - 1;
            int maxY = BORDER_SIZE - 1;

            // Draw frame
            g.setColor(frameColor);
            g.drawLine(x, y + 2, maxX - 2, y + 2);
            g.drawLine(x, y + 3, maxX - 2, y + 3);
            g.drawLine(x, y + 4, maxX - 2, y + 4);

            // Draw highlights
            g.setColor(frameHighlight);
            g.drawLine(x, y, maxX, y);
            g.drawLine(x, y + 1, maxX, y + 1);
            g.drawLine(x, y + 2, x, y + 4);
            g.drawLine(x + 1, y + 2, x + 1, y + 4);

            // Draw shadows
            g.setColor(frameShadow);
            g.drawLine(x + 4, y + 4, maxX - 4, y + 4);
            g.drawLine(maxX, y + 1, maxX, maxY);
            g.drawLine(maxX - 1, y + 2, maxX - 1, maxY);

            return true;
        }

        /** Draws the FrameBorder's left border.
          */
        protected boolean drawLeftBorder(Component c, Graphics g, int x, int y,
                               int width, int height) {
            Rectangle borderRect =
                new Rectangle(0, 0, getBorderInsets(c).left, height);
            if (!g.getClipBounds().intersects(borderRect)) {
                return false;
            }

            int startY = BORDER_SIZE;

            g.setColor(frameHighlight);
            g.drawLine(x, startY, x, height - 1);
            g.drawLine(x + 1, startY, x + 1, height - 2);

            g.setColor(frameColor);
            g.fillRect(x + 2, startY, x + 2, height - 3);

            g.setColor(frameShadow);
            g.drawLine(x + 4, startY, x + 4, height - 5);

            return true;
        }

        /** Draws the FrameBorder's right border.
          */
        protected boolean drawRightBorder(Component c, Graphics g, int x, int y,
                                int width, int height) {
            Rectangle borderRect = new Rectangle(
                width - getBorderInsets(c).right, 0,
                getBorderInsets(c).right, height);
            if (!g.getClipBounds().intersects(borderRect)) {
                return false;
            }

            int startX = width - getBorderInsets(c).right;
            int startY = BORDER_SIZE;

            g.setColor(frameColor);
            g.fillRect(startX + 1, startY, 2, height - 1);

            g.setColor(frameShadow);
            g.fillRect(startX + 3, startY, 2, height - 1);

            g.setColor(frameHighlight);
            g.drawLine(startX, startY, startX, height - 1);

            return true;
        }

        /** Draws the FrameBorder's bottom border.
          */
        protected boolean drawBottomBorder(Component c, Graphics g, int x, int y,
                                 int width, int height) {
            Rectangle    borderRect;
            int     marginHeight, startY;

            borderRect = new Rectangle(0, height - getBorderInsets(c).bottom,
                                  width, getBorderInsets(c).bottom);
            if (!g.getClipBounds().intersects(borderRect)) {
                return false;
            }

            startY = height - getBorderInsets(c).bottom;

            g.setColor(frameShadow);
            g.drawLine(x + 1, height - 1, width - 1, height - 1);
            g.drawLine(x + 2, height - 2, width - 2, height - 2);

            g.setColor(frameColor);
            g.fillRect(x + 2, startY + 1, width - 4, 2);

            g.setColor(frameHighlight);
            g.drawLine(x + 5, startY, width - 5, startY);

            return true;
        }

        // Returns true if the associated component has focus.
        protected boolean isActiveFrame() {
            return jcomp.hasFocus();
        }

        /** Draws the FrameBorder in the given Rect.  Calls
          * <b>drawTitleBar</b>, <b>drawLeftBorder</b>, <b>drawRightBorder</b> and
          * <b>drawBottomBorder</b>.
          */
        public void paintBorder(Component c, Graphics g,
                            int x, int y, int width, int height) {
            if (isActiveFrame()) {
                frameColor = UIManager.getColor("activeCaptionBorder");
            } else {
                frameColor = UIManager.getColor("inactiveCaptionBorder");
            }
            frameHighlight = frameColor.brighter();
            frameShadow = frameColor.darker().darker();

            drawTopBorder(c, g, x, y, width, height);
            drawLeftBorder(c, g, x, y, width, height);
            drawRightBorder(c, g, x, y, width, height);
            drawBottomBorder(c, g, x, y, width, height);
        }
    }

    @SuppressWarnings("serial") // Superclass is not serializable across versions
    public static class InternalFrameBorder extends FrameBorder {

        JInternalFrame frame;

        // The size of the bounding box for Motif frame corners.
        public static final int CORNER_SIZE = 24;

        /** Constructs an InternalFrameBorder for the InternalFrame
          * <b>aFrame</b>.
          */
        public InternalFrameBorder(JInternalFrame aFrame) {
            super(aFrame);
            frame = aFrame;
        }

        /** Sets the InternalFrameBorder's InternalFrame.
          */
        public void setFrame(JInternalFrame aFrame) {
            frame = aFrame;
        }

        /** Returns the InternalFrameBorder's InternalFrame.
          * @see #setFrame
          */
        public JInternalFrame frame() {
            return frame;
        }

        /** Returns the width of the InternalFrameBorder's resize controls,
          * appearing along the InternalFrameBorder's bottom border.  Clicking
          * and dragging within these controls lets the user change both the
          * InternalFrame's width and height, while dragging between the controls
          * constrains resizing to just the vertical dimension.  Override this
          * method if you implement your own bottom border painting and use a
          * resize control with a different size.
          */
        public int resizePartWidth() {
            if (!frame.isResizable()) {
                return 0;
            }
            return FrameBorder.BORDER_SIZE;
        }

        /** Draws the InternalFrameBorder's top border.
         */
        protected boolean drawTopBorder(Component c, Graphics g,
                                    int x, int y, int width, int height) {
            if (super.drawTopBorder(c, g, x, y, width, height) &&
                frame.isResizable()) {
                g.setColor(getFrameShadow());
                g.drawLine(CORNER_SIZE - 1, y + 1, CORNER_SIZE - 1, y + 4);
                g.drawLine(width - CORNER_SIZE - 1, y + 1,
                       width - CORNER_SIZE - 1, y + 4);

                g.setColor(getFrameHighlight());
                g.drawLine(CORNER_SIZE, y, CORNER_SIZE, y + 4);
                g.drawLine(width - CORNER_SIZE, y, width - CORNER_SIZE, y + 4);
                return true;
            }
            return false;
        }

        /** Draws the InternalFrameBorder's left border.
          */
        protected boolean drawLeftBorder(Component c, Graphics g, int x, int y,
                                     int width, int height) {
            if (super.drawLeftBorder(c, g, x, y, width, height) &&
                frame.isResizable()) {
                g.setColor(getFrameHighlight());
                int topY = y + CORNER_SIZE;
                g.drawLine(x, topY, x + 4, topY);
                int bottomY = height - CORNER_SIZE;
                g.drawLine(x + 1, bottomY, x + 5, bottomY);
                g.setColor(getFrameShadow());
                g.drawLine(x + 1, topY - 1, x + 5, topY - 1);
                g.drawLine(x + 1, bottomY - 1, x + 5, bottomY - 1);
                return true;
            }
            return false;
        }

        /** Draws the InternalFrameBorder's right border.
          */
        protected boolean drawRightBorder(Component c, Graphics g, int x, int y,
                                      int width, int height) {
            if (super.drawRightBorder(c, g, x, y, width, height) &&
                frame.isResizable()) {
                int startX = width - getBorderInsets(c).right;
                g.setColor(getFrameHighlight());
                int topY = y + CORNER_SIZE;
                g.drawLine(startX, topY, width - 2, topY);
                int bottomY = height - CORNER_SIZE;
                g.drawLine(startX + 1, bottomY, startX + 3, bottomY);
                g.setColor(getFrameShadow());
                g.drawLine(startX + 1, topY - 1, width - 2, topY - 1);
                g.drawLine(startX + 1, bottomY - 1, startX + 3, bottomY - 1);
                return true;
            }
            return false;
        }

        /** Draws the InternalFrameBorder's bottom border.
          */
        protected boolean drawBottomBorder(Component c, Graphics g, int x, int y,
                                       int width, int height) {
            if (super.drawBottomBorder(c, g, x, y, width, height) &&
                frame.isResizable()) {
                int startY = height - getBorderInsets(c).bottom;

                g.setColor(getFrameShadow());
                g.drawLine(CORNER_SIZE - 1, startY + 1,
                       CORNER_SIZE - 1, height - 1);
                g.drawLine(width - CORNER_SIZE, startY + 1,
                       width - CORNER_SIZE, height - 1);

                g.setColor(getFrameHighlight());
                g.drawLine(CORNER_SIZE, startY, CORNER_SIZE, height - 2);
                g.drawLine(width - CORNER_SIZE + 1, startY,
                       width - CORNER_SIZE + 1, height - 2);
                return true;
            }
            return false;
        }

        // Returns true if the associated internal frame has focus.
        protected boolean isActiveFrame() {
            return frame.isSelected();
        }
    }

    public static void drawBezel(Graphics g, int x, int y, int w, int h,
                               boolean isPressed, boolean hasFocus,
                               Color shadow, Color highlight,
                               Color darkShadow, Color focus)  {

        Color oldColor = g.getColor();
        g.translate(x, y);

        if (isPressed) {
            if (hasFocus){
                g.setColor(focus);
                g.drawRect(0, 0, w-1, h-1);
            }
            g.setColor(shadow);         // inner border
            g.drawRect(1, 1, w-3, h-3);

            g.setColor(highlight);    // inner 3D border
            g.drawLine(2, h-3, w-3, h-3);
            g.drawLine(w-3, 2, w-3, h-4);

        } else {
            if (hasFocus) {
                g.setColor(focus);
                g.drawRect(0, 0, w-1, h-1);

                g.setColor(highlight);   // inner 3D border
                g.drawLine(1, 1, 1, h-3);
                g.drawLine(2, 1, w-4, 1);

                g.setColor(shadow);
                g.drawLine(2, h-3, w-3, h-3);
                g.drawLine(w-3, 1, w-3, h-4);

                g.setColor(darkShadow);        // black drop shadow  __|
                g.drawLine(1, h-2, w-2, h-2);
                g.drawLine(w-2, h-2, w-2, 1);
            } else {
                g.setColor(highlight);    // inner 3D border
                g.drawLine(1,1,1,h-3);
                g.drawLine(2,1,w-4,1);
                g.setColor(shadow);
                g.drawLine(2,h-3,w-3,h-3);
                g.drawLine(w-3,1,w-3,h-4);

                g.setColor(darkShadow);         // black drop shadow  __|
                g.drawLine(1,h-2,w-2,h-2);
                g.drawLine(w-2,h-2,w-2,0);

            }
            g.translate(-x, -y);
        }
        g.setColor(oldColor);
    }

    @SuppressWarnings("serial") // Superclass is not serializable across versions
    public static class MotifPopupMenuBorder extends AbstractBorder implements UIResource {
        protected Font   font;
        protected Color  background;
        protected Color  foreground;
        protected Color  shadowColor;
        protected Color  highlightColor;

        // Space between the border and text
        protected static final int TEXT_SPACING = 2;

        // Space for the separator under the title
        protected static final int GROOVE_HEIGHT = 2;

        /**
         * Creates a MotifPopupMenuBorder instance
         *
         */
        public MotifPopupMenuBorder(
                                    Font titleFont,
                                    Color bgColor,
                                    Color fgColor,
                                    Color shadow,
                                    Color highlight)       {
            this.font = titleFont;
            this.background = bgColor;
            this.foreground = fgColor;
            this.shadowColor = shadow;
            this.highlightColor = highlight;
        }

        /**
         * Paints the border for the specified component with the
         * specified position and size.
         * @param c the component for which this border is being painted
         * @param g the paint graphics
         * @param x the x position of the painted border
         * @param y the y position of the painted border
         * @param width the width of the painted border
         * @param height the height of the painted border
         */
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            if (!(c instanceof JPopupMenu)) {
                return;
            }

            Font origFont = g.getFont();
            Color origColor = g.getColor();
            JPopupMenu popup = (JPopupMenu)c;

            String title = popup.getLabel();
            if (title == null) {
                return;
            }

            g.setFont(font);

            FontMetrics fm = SwingUtilities2.getFontMetrics(popup, g, font);
            int         fontHeight = fm.getHeight();
            int         descent = fm.getDescent();
            int         ascent = fm.getAscent();
            Point       textLoc = new Point();
            int         stringWidth = SwingUtilities2.stringWidth(popup, fm,
                                                                  title);

            textLoc.y = y + ascent + TEXT_SPACING;
            textLoc.x = x + ((width - stringWidth) / 2);

            g.setColor(background);
            g.fillRect(textLoc.x - TEXT_SPACING, textLoc.y - (fontHeight-descent),
                       stringWidth + (2 * TEXT_SPACING), fontHeight - descent);
            g.setColor(foreground);
            SwingUtilities2.drawString(popup, g, title, textLoc.x, textLoc.y);

            MotifGraphicsUtils.drawGroove(g, x, textLoc.y + TEXT_SPACING,
                                          width, GROOVE_HEIGHT,
                                          shadowColor, highlightColor);

            g.setFont(origFont);
            g.setColor(origColor);
        }

        /**
         * Reinitialize the insets parameter with this Border's current Insets.
         * @param c the component for which this border insets value applies
         * @param insets the object to be reinitialized
         */
        public Insets getBorderInsets(Component c, Insets insets) {
            if (!(c instanceof JPopupMenu)) {
                return insets;
            }
            FontMetrics fm;
            int         descent = 0;
            int         ascent = 16;

            String title = ((JPopupMenu)c).getLabel();
            if (title == null) {
                insets.left = insets.top = insets.right = insets.bottom = 0;
                return insets;
            }

            fm = c.getFontMetrics(font);

            if(fm != null) {
                descent = fm.getDescent();
                ascent = fm.getAscent();
            }

            insets.top += ascent + descent + TEXT_SPACING + GROOVE_HEIGHT;
            return insets;
        }

    }

}
