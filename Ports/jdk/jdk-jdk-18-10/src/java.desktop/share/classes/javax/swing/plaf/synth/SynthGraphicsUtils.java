/*
 * Copyright (c) 2002, 2019, Oracle and/or its affiliates. All rights reserved.
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
package javax.swing.plaf.synth;

import sun.swing.SwingUtilities2;
import sun.swing.MenuItemLayoutHelper;

import java.awt.*;
import javax.swing.*;
import javax.swing.plaf.basic.BasicHTML;
import javax.swing.text.*;

/**
 * Wrapper for primitive graphics calls.
 *
 * @since 1.5
 * @author Scott Violet
 */
public class SynthGraphicsUtils {
    // These are used in the text painting code to avoid allocating a bunch of
    // garbage.
    private Rectangle paintIconR = new Rectangle();
    private Rectangle paintTextR = new Rectangle();
    private Rectangle paintViewR = new Rectangle();
    private Insets paintInsets = new Insets(0, 0, 0, 0);

    // These Rectangles/Insets are used in the text size calculation to avoid a
    // a bunch of garbage.
    private Rectangle iconR = new Rectangle();
    private Rectangle textR = new Rectangle();
    private Rectangle viewR = new Rectangle();
    private Insets viewSizingInsets = new Insets(0, 0, 0, 0);

    /**
     * Creates a <code>SynthGraphicsUtils</code>.
     */
    public SynthGraphicsUtils() {
    }

    /**
     * Draws a line between the two end points.
     *
     * @param context Identifies hosting region.
     * @param paintKey Identifies the portion of the component being asked
     *                 to paint, may be null.
     * @param g Graphics object to paint to
     * @param x1 x origin
     * @param y1 y origin
     * @param x2 x destination
     * @param y2 y destination
     */
    public void drawLine(SynthContext context, Object paintKey,
                         Graphics g, int x1, int y1, int x2, int y2) {
        g.drawLine(x1, y1, x2, y2);
    }

    /**
     * Draws a line between the two end points.
     * <p>This implementation supports only one line style key,
     * <code>"dashed"</code>. The <code>"dashed"</code> line style is applied
     * only to vertical and horizontal lines.
     * <p>Specifying <code>null</code> or any key different from
     * <code>"dashed"</code> will draw solid lines.
     *
     * @param context identifies hosting region
     * @param paintKey identifies the portion of the component being asked
     *                 to paint, may be null
     * @param g Graphics object to paint to
     * @param x1 x origin
     * @param y1 y origin
     * @param x2 x destination
     * @param y2 y destination
     * @param styleKey identifies the requested style of the line (e.g. "dashed")
     * @since 1.6
     */
    public void drawLine(SynthContext context, Object paintKey,
                         Graphics g, int x1, int y1, int x2, int y2,
                         Object styleKey) {
        if ("dashed".equals(styleKey)) {
            // draw vertical line
            if (x1 == x2) {
                y1 += (y1 % 2);

                for (int y = y1; y <= y2; y+=2) {
                    g.drawLine(x1, y, x2, y);
                }
            // draw horizontal line
            } else if (y1 == y2) {
                x1 += (x1 % 2);

                for (int x = x1; x <= x2; x+=2) {
                    g.drawLine(x, y1, x, y2);
                }
            // oblique lines are not supported
            }
        } else {
            drawLine(context, paintKey, g, x1, y1, x2, y2);
        }
    }

    /**
     * Lays out text and an icon returning, by reference, the location to
     * place the icon and text.
     *
     * @param ss SynthContext
     * @param fm FontMetrics for the Font to use, this may be ignored
     * @param text Text to layout
     * @param icon Icon to layout
     * @param hAlign horizontal alignment
     * @param vAlign vertical alignment
     * @param hTextPosition horizontal text position
     * @param vTextPosition vertical text position
     * @param viewR Rectangle to layout text and icon in.
     * @param iconR Rectangle to place icon bounds in
     * @param textR Rectangle to place text in
     * @param iconTextGap gap between icon and text
     *
     * @return by reference, the location to
     * place the icon and text.
     */
    public String layoutText(SynthContext ss, FontMetrics fm,
                         String text, Icon icon, int hAlign,
                         int vAlign, int hTextPosition,
                         int vTextPosition, Rectangle viewR,
                         Rectangle iconR, Rectangle textR, int iconTextGap) {
        if (icon instanceof SynthIcon) {
            SynthIconWrapper wrapper = SynthIconWrapper.get((SynthIcon)icon,
                                                            ss);
            String formattedText = SwingUtilities.layoutCompoundLabel(
                      ss.getComponent(), fm, text, wrapper, vAlign, hAlign,
                      vTextPosition, hTextPosition, viewR, iconR, textR,
                      iconTextGap);
            SynthIconWrapper.release(wrapper);
            return formattedText;
        }
        return SwingUtilities.layoutCompoundLabel(
                      ss.getComponent(), fm, text, icon, vAlign, hAlign,
                      vTextPosition, hTextPosition, viewR, iconR, textR,
                      iconTextGap);
    }

    /**
     * Returns the size of the passed in string.
     *
     * @param ss SynthContext
     * @param font Font to use
     * @param metrics FontMetrics, may be ignored
     * @param text Text to get size of.
     *
     * @return the size of the passed in string.
     */
    public int computeStringWidth(SynthContext ss, Font font,
                                  FontMetrics metrics, String text) {
        return SwingUtilities2.stringWidth(ss.getComponent(), metrics,
                                          text);
    }

    /**
     * Returns the minimum size needed to properly render an icon and text.
     *
     * @param ss SynthContext
     * @param font Font to use
     * @param text Text to layout
     * @param icon Icon to layout
     * @param hAlign horizontal alignment
     * @param vAlign vertical alignment
     * @param hTextPosition horizontal text position
     * @param vTextPosition vertical text position
     * @param iconTextGap gap between icon and text
     * @param mnemonicIndex Index into text to render the mnemonic at, -1
     *        indicates no mnemonic.
     *
     * @return the minimum size needed to properly render an icon and text.
     */
    public Dimension getMinimumSize(SynthContext ss, Font font, String text,
                      Icon icon, int hAlign, int vAlign, int hTextPosition,
                      int vTextPosition, int iconTextGap, int mnemonicIndex) {
        JComponent c = ss.getComponent();
        Dimension size = getPreferredSize(ss, font, text, icon, hAlign,
                                          vAlign, hTextPosition, vTextPosition,
                                          iconTextGap, mnemonicIndex);
        View v = (View) c.getClientProperty(BasicHTML.propertyKey);

        if (v != null) {
            size.width -= v.getPreferredSpan(View.X_AXIS) -
                          v.getMinimumSpan(View.X_AXIS);
        }
        return size;
    }

    /**
     * Returns the maximum size needed to properly render an icon and text.
     *
     * @param ss SynthContext
     * @param font Font to use
     * @param text Text to layout
     * @param icon Icon to layout
     * @param hAlign horizontal alignment
     * @param vAlign vertical alignment
     * @param hTextPosition horizontal text position
     * @param vTextPosition vertical text position
     * @param iconTextGap gap between icon and text
     * @param mnemonicIndex Index into text to render the mnemonic at, -1
     *        indicates no mnemonic.
     *
     * @return the maximum size needed to properly render an icon and text.
     */
    public Dimension getMaximumSize(SynthContext ss, Font font, String text,
                      Icon icon, int hAlign, int vAlign, int hTextPosition,
                      int vTextPosition, int iconTextGap, int mnemonicIndex) {
        JComponent c = ss.getComponent();
        Dimension size = getPreferredSize(ss, font, text, icon, hAlign,
                                          vAlign, hTextPosition, vTextPosition,
                                          iconTextGap, mnemonicIndex);
        View v = (View) c.getClientProperty(BasicHTML.propertyKey);

        if (v != null) {
            size.width += v.getMaximumSpan(View.X_AXIS) -
                          v.getPreferredSpan(View.X_AXIS);
        }
        return size;
    }

    /**
     * Returns the maximum height of the Font from the passed in
     * SynthContext.
     *
     * @param context SynthContext used to determine font.
     * @return maximum height of the characters for the font from the passed
     *         in context.
     */
    public int getMaximumCharHeight(SynthContext context) {
        FontMetrics fm = context.getComponent().getFontMetrics(
            context.getStyle().getFont(context));
        return (fm.getAscent() + fm.getDescent());
    }

    /**
     * Returns the preferred size needed to properly render an icon and text.
     *
     * @param ss SynthContext
     * @param font Font to use
     * @param text Text to layout
     * @param icon Icon to layout
     * @param hAlign horizontal alignment
     * @param vAlign vertical alignment
     * @param hTextPosition horizontal text position
     * @param vTextPosition vertical text position
     * @param iconTextGap gap between icon and text
     * @param mnemonicIndex Index into text to render the mnemonic at, -1
     *        indicates no mnemonic.
     *
     * @return the preferred size needed to properly render an icon and text.
     */
    public Dimension getPreferredSize(SynthContext ss, Font font, String text,
                      Icon icon, int hAlign, int vAlign, int hTextPosition,
                      int vTextPosition, int iconTextGap, int mnemonicIndex) {
        JComponent c = ss.getComponent();
        Insets insets = c.getInsets(viewSizingInsets);
        int dx = insets.left + insets.right;
        int dy = insets.top + insets.bottom;

        if (icon == null && (text == null || font == null)) {
            return new Dimension(dx, dy);
        }
        else if ((text == null) || ((icon != null) && (font == null))) {
            return new Dimension(getIconWidth(icon, ss) + dx,
                                 getIconHeight(icon, ss) + dy);
        }
        else {
            FontMetrics fm = c.getFontMetrics(font);

            iconR.x = iconR.y = iconR.width = iconR.height = 0;
            textR.x = textR.y = textR.width = textR.height = 0;
            viewR.x = dx;
            viewR.y = dy;
            viewR.width = viewR.height = Short.MAX_VALUE;

            layoutText(ss, fm, text, icon, hAlign, vAlign,
                   hTextPosition, vTextPosition, viewR, iconR, textR,
                   iconTextGap);
            int x1 = Math.min(iconR.x, textR.x);
            int x2 = Math.max(iconR.x + iconR.width, textR.x + textR.width);
            int y1 = Math.min(iconR.y, textR.y);
            int y2 = Math.max(iconR.y + iconR.height, textR.y + textR.height);
            Dimension rv = new Dimension(x2 - x1, y2 - y1);

            rv.width += dx;
            rv.height += dy;
            return rv;
        }
    }

    /**
     * Paints text at the specified location. This will not attempt to
     * render the text as html nor will it offset by the insets of the
     * component.
     *
     * @param ss SynthContext
     * @param g Graphics used to render string in.
     * @param text Text to render
     * @param bounds Bounds of the text to be drawn.
     * @param mnemonicIndex Index to draw string at.
     */
    public void paintText(SynthContext ss, Graphics g, String text,
                          Rectangle bounds, int mnemonicIndex) {
        paintText(ss, g, text, bounds.x, bounds.y, mnemonicIndex);
    }

    /**
     * Paints text at the specified location. This will not attempt to
     * render the text as html nor will it offset by the insets of the
     * component.
     *
     * @param ss SynthContext
     * @param g Graphics used to render string in.
     * @param text Text to render
     * @param x X location to draw text at.
     * @param y Upper left corner to draw text at.
     * @param mnemonicIndex Index to draw string at.
     */
    public void paintText(SynthContext ss, Graphics g, String text,
                          int x, int y, int mnemonicIndex) {
        if (text != null) {
            JComponent c = ss.getComponent();
            FontMetrics fm = SwingUtilities2.getFontMetrics(c, g);
            y += fm.getAscent();
            SwingUtilities2.drawStringUnderlineCharAt(c, g, text,
                                                      mnemonicIndex, x, y);
        }
    }

    /**
     * Paints an icon and text. This will render the text as html, if
     * necessary, and offset the location by the insets of the component.
     *
     * @param ss SynthContext
     * @param g Graphics to render string and icon into
     * @param text Text to layout
     * @param icon Icon to layout
     * @param hAlign horizontal alignment
     * @param vAlign vertical alignment
     * @param hTextPosition horizontal text position
     * @param vTextPosition vertical text position
     * @param iconTextGap gap between icon and text
     * @param mnemonicIndex Index into text to render the mnemonic at, -1
     *        indicates no mnemonic.
     * @param textOffset Amount to offset the text when painting
     */
    public void paintText(SynthContext ss, Graphics g, String text,
                      Icon icon, int hAlign, int vAlign, int hTextPosition,
                      int vTextPosition, int iconTextGap, int mnemonicIndex,
                      int textOffset) {
        if ((icon == null) && (text == null)) {
            return;
        }
        JComponent c = ss.getComponent();
        FontMetrics fm = SwingUtilities2.getFontMetrics(c, g);
        Insets insets = SynthLookAndFeel.getPaintingInsets(ss, paintInsets);

        paintViewR.x = insets.left;
        paintViewR.y = insets.top;
        paintViewR.width = c.getWidth() - (insets.left + insets.right);
        paintViewR.height = c.getHeight() - (insets.top + insets.bottom);

        paintIconR.x = paintIconR.y = paintIconR.width = paintIconR.height = 0;
        paintTextR.x = paintTextR.y = paintTextR.width = paintTextR.height = 0;

        String clippedText =
            layoutText(ss, fm, text, icon, hAlign, vAlign,
                   hTextPosition, vTextPosition, paintViewR, paintIconR,
                   paintTextR, iconTextGap);

        if (icon != null) {
            Color color = g.getColor();

            if (ss.getStyle().getBoolean(ss, "TableHeader.alignSorterArrow", false) &&
                "TableHeader.renderer".equals(c.getName())) {
                paintIconR.x = paintViewR.width - paintIconR.width;
            } else {
                paintIconR.x += textOffset;
            }
            paintIconR.y += textOffset;
            paintIcon(icon, ss, g, paintIconR.x, paintIconR.y,
                                paintIconR.width, paintIconR.height);
            g.setColor(color);
        }

        if (text != null) {
            View v = (View) c.getClientProperty(BasicHTML.propertyKey);

            if (v != null) {
                v.paint(g, paintTextR);
            } else {
                paintTextR.x += textOffset;
                paintTextR.y += textOffset;

                paintText(ss, g, clippedText, paintTextR, mnemonicIndex);
            }
        }
    }

    /**
     * Returns the icon's width.
     * The {@code getIconWidth(context)} method is called for {@code SynthIcon}.
     *
     * @param icon the icon
     * @param context {@code SynthContext} requesting the icon, may be null.
     * @return an int specifying the width of the icon.
     */
    public static int getIconWidth(Icon icon, SynthContext context) {
        if (icon == null) {
            return 0;
        }
        if (icon instanceof SynthIcon) {
            return ((SynthIcon) icon).getIconWidth(context);
        }
        return icon.getIconWidth();
    }

    /**
     * Returns the icon's height.
     * The {@code getIconHeight(context)} method is called for {@code SynthIcon}.
     *
     * @param icon the icon
     * @param context {@code SynthContext} requesting the icon, may be null.
     * @return an int specifying the height of the icon.
     */
    public static int getIconHeight(Icon icon, SynthContext context) {
        if (icon == null) {
            return 0;
        }
        if (icon instanceof SynthIcon) {
            return ((SynthIcon) icon).getIconHeight(context);
        }
        return icon.getIconHeight();
    }

    /**
     * Paints the icon. The {@code paintIcon(context, g, x, y, width, height)}
     * method is called for {@code SynthIcon}.
     *
     * @param icon the icon
     * @param context identifies hosting region, may be null.
     * @param g the graphics context
     * @param x the x location to paint to
     * @param y the y location to paint to
     * @param width the width of the region to paint to, may be 0
     * @param height the height of the region to paint to, may be 0
     */
    public static void paintIcon(Icon icon, SynthContext context, Graphics g,
            int x, int y, int width, int height) {
        if (icon instanceof SynthIcon) {
            ((SynthIcon) icon).paintIcon(context, g, x, y, width, height);
        } else if (icon != null) {
            icon.paintIcon(context.getComponent(), g, x, y);
        }
    }

     /**
      * A quick note about how preferred sizes are calculated... Generally
      * speaking, SynthPopupMenuUI will run through the list of its children
      * (from top to bottom) and ask each for its preferred size.  Each menu
      * item will add up the max width of each element (icons, text,
      * accelerator spacing, accelerator text or arrow icon) encountered thus
      * far, so by the time all menu items have been calculated, we will
      * know the maximum (preferred) menu item size for that popup menu.
      * Later when it comes time to paint each menu item, we can use those
      * same accumulated max element sizes in order to layout the item.
      */
    static Dimension getPreferredMenuItemSize(SynthContext context,
           SynthContext accContext, JComponent c,
           Icon checkIcon, Icon arrowIcon, int defaultTextIconGap,
           String acceleratorDelimiter, boolean useCheckAndArrow,
           String propertyPrefix) {

         JMenuItem mi = (JMenuItem) c;
         SynthMenuItemLayoutHelper lh = new SynthMenuItemLayoutHelper(
                 context, accContext, mi, checkIcon, arrowIcon,
                 MenuItemLayoutHelper.createMaxRect(), defaultTextIconGap,
                 acceleratorDelimiter, SynthLookAndFeel.isLeftToRight(mi),
                 useCheckAndArrow, propertyPrefix);

         Dimension result = new Dimension();

         // Calculate the result width
         int gap = lh.getGap();
         result.width = 0;
         MenuItemLayoutHelper.addMaxWidth(lh.getCheckSize(), gap, result);
         MenuItemLayoutHelper.addMaxWidth(lh.getLabelSize(), gap, result);
         MenuItemLayoutHelper.addWidth(lh.getMaxAccOrArrowWidth(), 5 * gap, result);
         // The last gap is unnecessary
         result.width -= gap;

         // Calculate the result height
         result.height = MenuItemLayoutHelper.max(lh.getCheckSize().getHeight(),
                 lh.getLabelSize().getHeight(), lh.getAccSize().getHeight(),
                 lh.getArrowSize().getHeight());

         // Take into account menu item insets
         Insets insets = lh.getMenuItem().getInsets();
         if (insets != null) {
             result.width += insets.left + insets.right;
             result.height += insets.top + insets.bottom;
         }

         // if the width is even, bump it up one. This is critical
         // for the focus dash lhne to draw properly
         if (result.width % 2 == 0) {
             result.width++;
         }

         // if the height is even, bump it up one. This is critical
         // for the text to center properly
         if (result.height % 2 == 0) {
             result.height++;
         }

         return result;
     }

    static void applyInsets(Rectangle rect, Insets insets, boolean leftToRight) {
        if (insets != null) {
            rect.x += (leftToRight ? insets.left : insets.right);
            rect.y += insets.top;
            rect.width -= (leftToRight ? insets.right : insets.left) + rect.x;
            rect.height -= (insets.bottom + rect.y);
        }
    }

    static void paint(SynthContext context, SynthContext accContext, Graphics g,
               Icon checkIcon, Icon arrowIcon, String acceleratorDelimiter,
               int defaultTextIconGap, String propertyPrefix) {
        JMenuItem mi = (JMenuItem) context.getComponent();
        SynthStyle style = context.getStyle();
        g.setFont(style.getFont(context));

        Rectangle viewRect = new Rectangle(0, 0, mi.getWidth(), mi.getHeight());
        boolean leftToRight = SynthLookAndFeel.isLeftToRight(mi);
        applyInsets(viewRect, mi.getInsets(), leftToRight);

        SynthMenuItemLayoutHelper lh = new SynthMenuItemLayoutHelper(
                context, accContext, mi, checkIcon, arrowIcon, viewRect,
                defaultTextIconGap, acceleratorDelimiter, leftToRight,
                MenuItemLayoutHelper.useCheckAndArrow(mi), propertyPrefix);
        MenuItemLayoutHelper.LayoutResult lr = lh.layoutMenuItem();

        paintMenuItem(g, lh, lr);
    }

    static void paintMenuItem(Graphics g, SynthMenuItemLayoutHelper lh,
                              MenuItemLayoutHelper.LayoutResult lr) {
        // Save original graphics font and color
        Font holdf = g.getFont();
        Color holdc = g.getColor();

        paintCheckIcon(g, lh, lr);
        paintIcon(g, lh, lr);
        paintText(g, lh, lr);
        paintAccText(g, lh, lr);
        paintArrowIcon(g, lh, lr);

        // Restore original graphics font and color
        g.setColor(holdc);
        g.setFont(holdf);
    }

    static void paintBackground(Graphics g, SynthMenuItemLayoutHelper lh) {
        paintBackground(lh.getContext(), g, lh.getMenuItem());
    }

    static void paintBackground(SynthContext context, Graphics g, JComponent c) {
        context.getPainter().paintMenuItemBackground(context, g, 0, 0,
                c.getWidth(), c.getHeight());
    }

    static void paintIcon(Graphics g, SynthMenuItemLayoutHelper lh,
                          MenuItemLayoutHelper.LayoutResult lr) {
        if (lh.getIcon() != null) {
            Icon icon;
            JMenuItem mi = lh.getMenuItem();
            ButtonModel model = mi.getModel();
            if (!model.isEnabled()) {
                icon = mi.getDisabledIcon();
            } else if (model.isPressed() && model.isArmed()) {
                icon = mi.getPressedIcon();
                if (icon == null) {
                    // Use default icon
                    icon = mi.getIcon();
                }
            } else {
                icon = mi.getIcon();
            }

            if (icon != null) {
                Rectangle iconRect = lr.getIconRect();
                paintIcon(icon, lh.getContext(), g, iconRect.x,
                        iconRect.y, iconRect.width, iconRect.height);
            }
        }
    }

    static void paintCheckIcon(Graphics g, SynthMenuItemLayoutHelper lh,
                               MenuItemLayoutHelper.LayoutResult lr) {
        if (lh.getCheckIcon() != null) {
            Rectangle checkRect = lr.getCheckRect();
            paintIcon(lh.getCheckIcon(), lh.getContext(), g,
                    checkRect.x, checkRect.y, checkRect.width, checkRect.height);
        }
    }

    static void paintAccText(Graphics g, SynthMenuItemLayoutHelper lh,
                             MenuItemLayoutHelper.LayoutResult lr) {
        String accText = lh.getAccText();
        if (accText != null && !accText.isEmpty()) {
            g.setColor(lh.getAccStyle().getColor(lh.getAccContext(),
                    ColorType.TEXT_FOREGROUND));
            g.setFont(lh.getAccStyle().getFont(lh.getAccContext()));
            lh.getAccGraphicsUtils().paintText(lh.getAccContext(), g, accText,
                    lr.getAccRect().x, lr.getAccRect().y, -1);
        }
    }

    static void paintText(Graphics g, SynthMenuItemLayoutHelper lh,
                          MenuItemLayoutHelper.LayoutResult lr) {
        if (!lh.getText().isEmpty()) {
            if (lh.getHtmlView() != null) {
                // Text is HTML
                lh.getHtmlView().paint(g, lr.getTextRect());
            } else {
                // Text isn't HTML
                g.setColor(lh.getStyle().getColor(
                        lh.getContext(), ColorType.TEXT_FOREGROUND));
                g.setFont(lh.getStyle().getFont(lh.getContext()));
                lh.getGraphicsUtils().paintText(lh.getContext(), g, lh.getText(),
                        lr.getTextRect().x, lr.getTextRect().y,
                        lh.getMenuItem().getDisplayedMnemonicIndex());
            }
        }
    }

    static void paintArrowIcon(Graphics g, SynthMenuItemLayoutHelper lh,
                               MenuItemLayoutHelper.LayoutResult lr) {
        if (lh.getArrowIcon() != null) {
            Rectangle arrowRect = lr.getArrowRect();
            paintIcon(lh.getArrowIcon(), lh.getContext(), g,
                    arrowRect.x, arrowRect.y, arrowRect.width, arrowRect.height);
        }
    }

    /**
     * Wraps a SynthIcon around the Icon interface, forwarding calls to
     * the SynthIcon with a given SynthContext.
     */
    private static class SynthIconWrapper implements Icon {
        private static final java.util.List<SynthIconWrapper> CACHE = new java.util.ArrayList<SynthIconWrapper>(1);

        private SynthIcon synthIcon;
        private SynthContext context;

        static SynthIconWrapper get(SynthIcon icon, SynthContext context) {
            synchronized(CACHE) {
                int size = CACHE.size();
                if (size > 0) {
                    SynthIconWrapper wrapper = CACHE.remove(size - 1);
                    wrapper.reset(icon, context);
                    return wrapper;
                }
            }
            return new SynthIconWrapper(icon, context);
        }

        static void release(SynthIconWrapper wrapper) {
            wrapper.reset(null, null);
            synchronized(CACHE) {
                CACHE.add(wrapper);
            }
        }

        SynthIconWrapper(SynthIcon icon, SynthContext context) {
            reset(icon, context);
        }

        void reset(SynthIcon icon, SynthContext context) {
            synthIcon = icon;
            this.context = context;
        }

        public void paintIcon(Component c, Graphics g, int x, int y) {
            // This is a noop as this should only be for sizing calls.
        }

        public int getIconWidth() {
            return synthIcon.getIconWidth(context);
        }

        public int getIconHeight() {
            return synthIcon.getIconHeight(context);
        }
    }
}
