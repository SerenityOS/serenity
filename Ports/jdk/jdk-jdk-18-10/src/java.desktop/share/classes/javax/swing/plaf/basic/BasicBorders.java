/*
 * Copyright (c) 1997, 2015, Oracle and/or its affiliates. All rights reserved.
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
import javax.swing.border.*;
import javax.swing.plaf.*;
import javax.swing.text.JTextComponent;

import java.awt.Component;
import java.awt.Insets;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Color;
import java.awt.Graphics;

import sun.swing.SwingUtilities2;

/**
 * Factory object that can vend Borders appropriate for the basic L &amp; F.
 * @author Georges Saab
 * @author Amy Fowler
 */

public class BasicBorders {

    /**
     * Constructs a {@code BasicBorders}.
     */
    public BasicBorders() {}

    /**
     * Returns a border instance for a {@code JButton}.
     *
     * @return a border instance for a {@code JButton}
     */
    public static Border getButtonBorder() {
        UIDefaults table = UIManager.getLookAndFeelDefaults();
        Border buttonBorder = new BorderUIResource.CompoundBorderUIResource(
                           new BasicBorders.ButtonBorder(
                                           table.getColor("Button.shadow"),
                                           table.getColor("Button.darkShadow"),
                                           table.getColor("Button.light"),
                                           table.getColor("Button.highlight")),
                                     new MarginBorder());
        return buttonBorder;
    }

    /**
     * Returns a border instance for a {@code JRadioButton}.
     *
     * @return a border instance for a {@code JRadioButton}
     */
    public static Border getRadioButtonBorder() {
        UIDefaults table = UIManager.getLookAndFeelDefaults();
        Border radioButtonBorder = new BorderUIResource.CompoundBorderUIResource(
                           new BasicBorders.RadioButtonBorder(
                                           table.getColor("RadioButton.shadow"),
                                           table.getColor("RadioButton.darkShadow"),
                                           table.getColor("RadioButton.light"),
                                           table.getColor("RadioButton.highlight")),
                                     new MarginBorder());
        return radioButtonBorder;
    }

    /**
     * Returns a border instance for a {@code JToggleButton}.
     *
     * @return a border instance for a {@code JToggleButton}
     */
    public static Border getToggleButtonBorder() {
        UIDefaults table = UIManager.getLookAndFeelDefaults();
        Border toggleButtonBorder = new BorderUIResource.CompoundBorderUIResource(
                                     new BasicBorders.ToggleButtonBorder(
                                           table.getColor("ToggleButton.shadow"),
                                           table.getColor("ToggleButton.darkShadow"),
                                           table.getColor("ToggleButton.light"),
                                           table.getColor("ToggleButton.highlight")),
                                     new MarginBorder());
        return toggleButtonBorder;
    }

    /**
     * Returns a border instance for a {@code JMenuBar}.
     *
     * @return a border instance for a {@code JMenuBar}
     */
    public static Border getMenuBarBorder() {
        UIDefaults table = UIManager.getLookAndFeelDefaults();
        Border menuBarBorder = new BasicBorders.MenuBarBorder(
                                        table.getColor("MenuBar.shadow"),
                                        table.getColor("MenuBar.highlight")
                                   );
        return menuBarBorder;
    }

    /**
     * Returns a border instance for a {@code JSplitPane}.
     *
     * @return a border instance for a {@code JSplitPane}
     */
    public static Border getSplitPaneBorder() {
        UIDefaults table = UIManager.getLookAndFeelDefaults();
        Border splitPaneBorder = new BasicBorders.SplitPaneBorder(
                                     table.getColor("SplitPane.highlight"),
                                     table.getColor("SplitPane.darkShadow"));
        return splitPaneBorder;
    }

    /**
     * Returns a border instance for a {@code JSplitPane} divider.
     *
     * @return a border instance for a {@code JSplitPane} divider
     * @since 1.3
     */
    public static Border getSplitPaneDividerBorder() {
        UIDefaults table = UIManager.getLookAndFeelDefaults();
        Border splitPaneBorder = new BasicBorders.SplitPaneDividerBorder(
                                     table.getColor("SplitPane.highlight"),
                                     table.getColor("SplitPane.darkShadow"));
        return splitPaneBorder;
    }

    /**
     * Returns a border instance for a {@code JTextField}.
     *
     * @return a border instance for a {@code JTextField}
     */
    public static Border getTextFieldBorder() {
        UIDefaults table = UIManager.getLookAndFeelDefaults();
        Border textFieldBorder = new BasicBorders.FieldBorder(
                                           table.getColor("TextField.shadow"),
                                           table.getColor("TextField.darkShadow"),
                                           table.getColor("TextField.light"),
                                           table.getColor("TextField.highlight"));
        return textFieldBorder;
    }

    /**
     * Returns a border instance for a {@code JProgressBar}.
     *
     * @return a border instance for a {@code JProgressBar}
     */
    public static Border getProgressBarBorder() {
        UIDefaults table = UIManager.getLookAndFeelDefaults();
        Border progressBarBorder = new BorderUIResource.LineBorderUIResource(Color.green, 2);
        return progressBarBorder;
    }

    /**
     * Returns a border instance for a {@code JInternalFrame}.
     *
     * @return a border instance for a {@code JInternalFrame}
     */
    public static Border getInternalFrameBorder() {
        UIDefaults table = UIManager.getLookAndFeelDefaults();
        Border internalFrameBorder = new BorderUIResource.CompoundBorderUIResource(
                                new BevelBorder(BevelBorder.RAISED,
                                        table.getColor("InternalFrame.borderLight"),
                                        table.getColor("InternalFrame.borderHighlight"),
                                        table.getColor("InternalFrame.borderDarkShadow"),
                                        table.getColor("InternalFrame.borderShadow")),
                                BorderFactory.createLineBorder(
                                        table.getColor("InternalFrame.borderColor"), 1));

        return internalFrameBorder;
    }

    /**
     * Special thin border for rollover toolbar buttons.
     * @since 1.4
     */
    @SuppressWarnings("serial") // Superclass is not serializable across versions
    public static class RolloverButtonBorder extends ButtonBorder {

        /**
         * Constructs a new instance of a {@code RolloverButtonBorder}.
         *
         * @param shadow a color of shadow
         * @param darkShadow a color of dark shadow
         * @param highlight a color of highlight
         * @param lightHighlight a color of light highlight
         */
        public RolloverButtonBorder(Color shadow, Color darkShadow,
                                  Color highlight, Color lightHighlight) {
            super(shadow, darkShadow, highlight, lightHighlight);
        }

        public void paintBorder( Component c, Graphics g, int x, int y, int w, int h ) {
            AbstractButton b = (AbstractButton) c;
            ButtonModel model = b.getModel();

            Color shade = shadow;
            Component p = b.getParent();
            if (p != null && p.getBackground().equals(shadow)) {
                shade = darkShadow;
            }

            if ((model.isRollover() && !(model.isPressed() && !model.isArmed())) ||
                model.isSelected()) {

                Color oldColor = g.getColor();
                g.translate(x, y);

                if (model.isPressed() && model.isArmed() || model.isSelected()) {
                    // Draw the pressd button
                    g.setColor(shade);
                    g.drawRect(0, 0, w-1, h-1);
                    g.setColor(lightHighlight);
                    g.drawLine(w-1, 0, w-1, h-1);
                    g.drawLine(0, h-1, w-1, h-1);
                } else {
                    // Draw a rollover button
                    g.setColor(lightHighlight);
                    g.drawRect(0, 0, w-1, h-1);
                    g.setColor(shade);
                    g.drawLine(w-1, 0, w-1, h-1);
                    g.drawLine(0, h-1, w-1, h-1);
                }
                g.translate(-x, -y);
                g.setColor(oldColor);
            }
        }
    }


    /**
     * A border which is like a Margin border but it will only honor the margin
     * if the margin has been explicitly set by the developer.
     *
     * Note: This is identical to the package private class
     * MetalBorders.RolloverMarginBorder and should probably be consolidated.
     */
    @SuppressWarnings("serial") // Superclass is not serializable across versions
    static class RolloverMarginBorder extends EmptyBorder {

        public RolloverMarginBorder() {
            super(3,3,3,3); // hardcoded margin for JLF requirements.
        }

        public Insets getBorderInsets(Component c, Insets insets) {
            Insets margin = null;

            if (c instanceof AbstractButton) {
                margin = ((AbstractButton)c).getMargin();
            }
            if (margin == null || margin instanceof UIResource) {
                // default margin so replace
                insets.left = left;
                insets.top = top;
                insets.right = right;
                insets.bottom = bottom;
            } else {
                // Margin which has been explicitly set by the user.
                insets.left = margin.left;
                insets.top = margin.top;
                insets.right = margin.right;
                insets.bottom = margin.bottom;
            }
            return insets;
        }
    }

    /**
     * Draws a border around a button.
     */
    @SuppressWarnings("serial") // Superclass is not serializable across versions
   public static class ButtonBorder extends AbstractBorder implements UIResource {
        /**
         * The color of shadow.
         */
        protected Color shadow;
        /**
         * The color of dark shadow.
         */
        protected Color darkShadow;
        /**
         * The color of highlight.
         */
        protected Color highlight;
        /**
         * The color of light highlight.
         */
        protected Color lightHighlight;

        /**
         * Constructs a new instance of a {@code ButtonBorder}.
         *
         * @param shadow a color of shadow
         * @param darkShadow a color of dark shadow
         * @param highlight a color of highlight
         * @param lightHighlight a color of light highlight
         */
        public ButtonBorder(Color shadow, Color darkShadow,
                            Color highlight, Color lightHighlight) {
            this.shadow = shadow;
            this.darkShadow = darkShadow;
            this.highlight = highlight;
            this.lightHighlight = lightHighlight;
        }

        public void paintBorder(Component c, Graphics g, int x, int y,
                            int width, int height) {
            boolean isPressed = false;
            boolean isDefault = false;

            if (c instanceof AbstractButton) {
                AbstractButton b = (AbstractButton)c;
                ButtonModel model = b.getModel();

                isPressed = model.isPressed() && model.isArmed();

                if (c instanceof JButton) {
                    isDefault = ((JButton)c).isDefaultButton();
                }
            }
            BasicGraphicsUtils.drawBezel(g, x, y, width, height,
                                   isPressed, isDefault, shadow,
                                   darkShadow, highlight, lightHighlight);
        }

        public Insets getBorderInsets(Component c, Insets insets)       {
            // leave room for default visual
            insets.set(2, 3, 3, 3);
            return insets;
        }

    }

    /**
     * Draws the border around a toggle button.
     */
    @SuppressWarnings("serial") // Superclass is not serializable across versions
    public static class ToggleButtonBorder extends ButtonBorder {

        /**
         * Constructs a new instance of a {@code ToggleButtonBorder}.
         *
         * @param shadow a color of shadow
         * @param darkShadow a color of dark shadow
         * @param highlight a color of highlight
         * @param lightHighlight a color of light highlight
         */
        public ToggleButtonBorder(Color shadow, Color darkShadow,
                                  Color highlight, Color lightHighlight) {
            super(shadow, darkShadow, highlight, lightHighlight);
        }

        public void paintBorder(Component c, Graphics g, int x, int y,
                                int width, int height) {
                BasicGraphicsUtils.drawBezel(g, x, y, width, height,
                                             false, false,
                                             shadow, darkShadow,
                                             highlight, lightHighlight);
        }

        public Insets getBorderInsets(Component c, Insets insets)       {
            insets.set(2, 2, 2, 2);
            return insets;
        }
    }

    /**
     * Draws the border around a radio button.
     */
    @SuppressWarnings("serial") // Superclass is not serializable across versions
    public static class RadioButtonBorder extends ButtonBorder {

        /**
         * Constructs a new instance of a {@code RadioButtonBorder}.
         *
         * @param shadow a color of shadow
         * @param darkShadow a color of dark shadow
         * @param highlight a color of highlight
         * @param lightHighlight a color of light highlight
         */
        public RadioButtonBorder(Color shadow, Color darkShadow,
                                 Color highlight, Color lightHighlight) {
            super(shadow, darkShadow, highlight, lightHighlight);
        }


        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {

            if (c instanceof AbstractButton) {
                AbstractButton b = (AbstractButton)c;
                ButtonModel model = b.getModel();

                if (model.isArmed() && model.isPressed() || model.isSelected()) {
                    BasicGraphicsUtils.drawLoweredBezel(g, x, y, width, height,
                                                        shadow, darkShadow,
                                                        highlight, lightHighlight);
                } else {
                    BasicGraphicsUtils.drawBezel(g, x, y, width, height,
                                               false, b.isFocusPainted() && b.hasFocus(),
                                                 shadow, darkShadow,
                                                 highlight, lightHighlight);
                }
            } else {
                BasicGraphicsUtils.drawBezel(g, x, y, width, height, false, false,
                                             shadow, darkShadow, highlight, lightHighlight);
            }
        }

        public Insets getBorderInsets(Component c, Insets insets)       {
            insets.set(2, 2, 2, 2);
            return insets;
        }
    }

    /**
     * Draws the border around a menu bar.
     */
    @SuppressWarnings("serial") // Superclass is not serializable across versions
    public static class MenuBarBorder extends AbstractBorder implements UIResource {
        /**
         * The color of shadow.
         */
        private Color shadow;
        /**
         * The color of highlight.
         */
        private Color highlight;

        /**
         * Constructs a new instance of a {@code MenuBarBorder}.
         *
         * @param shadow a color of shadow
         * @param highlight a color of highlight
         */
        public MenuBarBorder(Color shadow, Color highlight) {
            this.shadow = shadow;
            this.highlight = highlight;
        }

        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Color oldColor = g.getColor();
            g.translate(x, y);
            g.setColor(shadow);
            SwingUtilities2.drawHLine(g, 0, width - 1, height - 2);
            g.setColor(highlight);
            SwingUtilities2.drawHLine(g, 0, width - 1, height - 1);
            g.translate(-x, -y);
            g.setColor(oldColor);
        }

        public Insets getBorderInsets(Component c, Insets insets)       {
            insets.set(0, 0, 2, 0);
            return insets;
        }
    }

    /**
     * Draws the border around components which support margins.
     */
    @SuppressWarnings("serial") // Superclass is not serializable across versions
    public static class MarginBorder extends AbstractBorder implements UIResource {
        /**
         * Constructs a {@code MarginBorder}.
         */
        public MarginBorder() {}

        public Insets getBorderInsets(Component c, Insets insets)       {
            Insets margin = null;
            //
            // Ideally we'd have an interface defined for classes which
            // support margins (to avoid this hackery), but we've
            // decided against it for simplicity
            //
           if (c instanceof AbstractButton) {
               AbstractButton b = (AbstractButton)c;
               margin = b.getMargin();
           } else if (c instanceof JToolBar) {
               JToolBar t = (JToolBar)c;
               margin = t.getMargin();
           } else if (c instanceof JTextComponent) {
               JTextComponent t = (JTextComponent)c;
               margin = t.getMargin();
           }
           insets.top = margin != null? margin.top : 0;
           insets.left = margin != null? margin.left : 0;
           insets.bottom = margin != null? margin.bottom : 0;
           insets.right = margin != null? margin.right : 0;

           return insets;
        }
    }

    /**
     * Draws the border around a field.
     */
    @SuppressWarnings("serial") // Superclass is not serializable across versions
    public static class FieldBorder extends AbstractBorder implements UIResource {
        /**
         * The color of shadow.
         */
        protected Color shadow;
        /**
         * The color of dark shadow.
         */
        protected Color darkShadow;
        /**
         * The color of highlight.
         */
        protected Color highlight;
        /**
         * The color of light highlight.
         */
        protected Color lightHighlight;

        /**
         * Constructs a new instance of a {@code FieldBorder}.
         *
         * @param shadow a color of shadow
         * @param darkShadow a color of dark shadow
         * @param highlight a color of highlight
         * @param lightHighlight a color of light highlight
         */
        public FieldBorder(Color shadow, Color darkShadow,
                           Color highlight, Color lightHighlight) {
            this.shadow = shadow;
            this.highlight = highlight;
            this.darkShadow = darkShadow;
            this.lightHighlight = lightHighlight;
        }

        public void paintBorder(Component c, Graphics g, int x, int y,
                            int width, int height) {
            BasicGraphicsUtils.drawEtchedRect(g, x, y, width, height,
                                              shadow, darkShadow,
                                              highlight, lightHighlight);
        }

        public Insets getBorderInsets(Component c, Insets insets) {
            Insets margin = null;
            if (c instanceof JTextComponent) {
                margin = ((JTextComponent)c).getMargin();
            }
            insets.top = margin != null? 2+margin.top : 2;
            insets.left = margin != null? 2+margin.left : 2;
            insets.bottom = margin != null? 2+margin.bottom : 2;
            insets.right = margin != null? 2+margin.right : 2;

            return insets;
        }
    }


    /**
     * Draws the border around the divider in a splitpane
     * (when BasicSplitPaneUI is used). To get the appropriate effect, this
     * needs to be used with a SplitPaneBorder.
     */
    static class SplitPaneDividerBorder implements Border, UIResource {
        Color highlight;
        Color shadow;

        SplitPaneDividerBorder(Color highlight, Color shadow) {
            this.highlight = highlight;
            this.shadow = shadow;
        }

        public void paintBorder(Component c, Graphics g, int x, int y,
                                int width, int height) {
            if (!(c instanceof BasicSplitPaneDivider)) {
                return;
            }
            Component          child;
            Rectangle          cBounds;
            JSplitPane         splitPane = ((BasicSplitPaneDivider)c).
                                         getBasicSplitPaneUI().getSplitPane();
            Dimension          size = c.getSize();

            child = splitPane.getLeftComponent();
            // This is needed for the space between the divider and end of
            // splitpane.
            g.setColor(c.getBackground());
            g.drawRect(x, y, width - 1, height - 1);
            if(splitPane.getOrientation() == JSplitPane.HORIZONTAL_SPLIT) {
                if(child != null) {
                    g.setColor(highlight);
                    g.drawLine(0, 0, 0, size.height);
                }
                child = splitPane.getRightComponent();
                if(child != null) {
                    g.setColor(shadow);
                    g.drawLine(size.width - 1, 0, size.width - 1, size.height);
                }
            } else {
                if(child != null) {
                    g.setColor(highlight);
                    g.drawLine(0, 0, size.width, 0);
                }
                child = splitPane.getRightComponent();
                if(child != null) {
                    g.setColor(shadow);
                    g.drawLine(0, size.height - 1, size.width,
                               size.height - 1);
                }
            }
        }
        public Insets getBorderInsets(Component c) {
            Insets insets = new Insets(0,0,0,0);
            if (c instanceof BasicSplitPaneDivider) {
                BasicSplitPaneUI bspui = ((BasicSplitPaneDivider)c).
                                         getBasicSplitPaneUI();

                if (bspui != null) {
                    JSplitPane splitPane = bspui.getSplitPane();

                    if (splitPane != null) {
                        if (splitPane.getOrientation() ==
                            JSplitPane.HORIZONTAL_SPLIT) {
                            insets.top = insets.bottom = 0;
                            insets.left = insets.right = 1;
                            return insets;
                        }
                        // VERTICAL_SPLIT
                        insets.top = insets.bottom = 1;
                        insets.left = insets.right = 0;
                        return insets;
                    }
                }
            }
            insets.top = insets.bottom = insets.left = insets.right = 1;
            return insets;
        }
        public boolean isBorderOpaque() { return true; }
    }


    /**
     * Draws the border around the splitpane. To work correctly you should
     * also install a border on the divider (property SplitPaneDivider.border).
     */
    public static class SplitPaneBorder implements Border, UIResource {
        /**
         * The color of highlight
         */
        protected Color highlight;
        /**
         * The color of shadow
         */
        protected Color shadow;

        /**
         * Constructs a new instance of a {@code SplitPaneBorder}.
         *
         * @param highlight a color of highlight
         * @param shadow a color of shadow
         */
        public SplitPaneBorder(Color highlight, Color shadow) {
            this.highlight = highlight;
            this.shadow = shadow;
        }

        public void paintBorder(Component c, Graphics g, int x, int y,
                                int width, int height) {
            if (!(c instanceof JSplitPane)) {
                return;
            }
            // The only tricky part with this border is that the divider is
            // not positioned at the top (for horizontal) or left (for vert),
            // so this border draws to where the divider is:
            // -----------------
            // |xxxxxxx xxxxxxx|
            // |x     ---     x|
            // |x     | |     x|
            // |x     |D|     x|
            // |x     | |     x|
            // |x     ---     x|
            // |xxxxxxx xxxxxxx|
            // -----------------
            // The above shows (rather excessively) what this looks like for
            // a horizontal orientation. This border then draws the x's, with
            // the SplitPaneDividerBorder drawing its own border.

            Component          child;
            Rectangle          cBounds;

            JSplitPane splitPane = (JSplitPane)c;

            child = splitPane.getLeftComponent();
            // This is needed for the space between the divider and end of
            // splitpane.
            g.setColor(c.getBackground());
            g.drawRect(x, y, width - 1, height - 1);
            if(splitPane.getOrientation() == JSplitPane.HORIZONTAL_SPLIT) {
                if(child != null) {
                    cBounds = child.getBounds();
                    g.setColor(shadow);
                    g.drawLine(0, 0, cBounds.width + 1, 0);
                    g.drawLine(0, 1, 0, cBounds.height + 1);

                    g.setColor(highlight);
                    g.drawLine(0, cBounds.height + 1, cBounds.width + 1,
                               cBounds.height + 1);
                }
                child = splitPane.getRightComponent();
                if(child != null) {
                    cBounds = child.getBounds();

                    int             maxX = cBounds.x + cBounds.width;
                    int             maxY = cBounds.y + cBounds.height;

                    g.setColor(shadow);
                    g.drawLine(cBounds.x - 1, 0, maxX, 0);
                    g.setColor(highlight);
                    g.drawLine(cBounds.x - 1, maxY, maxX, maxY);
                    g.drawLine(maxX, 0, maxX, maxY + 1);
                }
            } else {
                if(child != null) {
                    cBounds = child.getBounds();
                    g.setColor(shadow);
                    g.drawLine(0, 0, cBounds.width + 1, 0);
                    g.drawLine(0, 1, 0, cBounds.height);
                    g.setColor(highlight);
                    g.drawLine(1 + cBounds.width, 0, 1 + cBounds.width,
                               cBounds.height + 1);
                    g.drawLine(0, cBounds.height + 1, 0, cBounds.height + 1);
                }
                child = splitPane.getRightComponent();
                if(child != null) {
                    cBounds = child.getBounds();

                    int             maxX = cBounds.x + cBounds.width;
                    int             maxY = cBounds.y + cBounds.height;

                    g.setColor(shadow);
                    g.drawLine(0, cBounds.y - 1, 0, maxY);
                    g.drawLine(maxX, cBounds.y - 1, maxX, cBounds.y - 1);
                    g.setColor(highlight);
                    g.drawLine(0, maxY, cBounds.width + 1, maxY);
                    g.drawLine(maxX, cBounds.y, maxX, maxY);
                }
            }
        }
        public Insets getBorderInsets(Component c) {
            return new Insets(1, 1, 1, 1);
        }
        public boolean isBorderOpaque() { return true; }
    }

}
