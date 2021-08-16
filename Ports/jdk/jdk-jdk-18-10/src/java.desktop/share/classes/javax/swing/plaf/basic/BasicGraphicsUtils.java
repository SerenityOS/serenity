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
import java.awt.Component;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.InputEvent;

import sun.swing.SwingUtilities2;


/**
 * Convenient util class.
 *
 * @author Hans Muller
 */
public class BasicGraphicsUtils
{

    private static final Insets GROOVE_INSETS = new Insets(2, 2, 2, 2);
    private static final Insets ETCHED_INSETS = new Insets(2, 2, 2, 2);

    /**
     * Constructs a {@code BasicGraphicsUtils}.
     */
    public BasicGraphicsUtils() {}

    /**
     * Draws an etched rectangle.
     *
     * @param g an instance of {@code Graphics}
     * @param x an X coordinate
     * @param y an Y coordinate
     * @param w a width
     * @param h a height
     * @param shadow a color of shadow
     * @param darkShadow a color of dark shadow
     * @param highlight a color highlighting
     * @param lightHighlight a color of light highlighting
     */
    public static void drawEtchedRect(Graphics g, int x, int y, int w, int h,
                                      Color shadow, Color darkShadow,
                                      Color highlight, Color lightHighlight)
    {
        Color oldColor = g.getColor();  // Make no net change to g
        g.translate(x, y);

        g.setColor(shadow);
        g.drawLine(0, 0, w-1, 0);      // outer border, top
        g.drawLine(0, 1, 0, h-2);      // outer border, left

        g.setColor(darkShadow);
        g.drawLine(1, 1, w-3, 1);      // inner border, top
        g.drawLine(1, 2, 1, h-3);      // inner border, left

        g.setColor(lightHighlight);
        g.drawLine(w-1, 0, w-1, h-1);  // outer border, bottom
        g.drawLine(0, h-1, w-1, h-1);  // outer border, right

        g.setColor(highlight);
        g.drawLine(w-2, 1, w-2, h-3);  // inner border, right
        g.drawLine(1, h-2, w-2, h-2);  // inner border, bottom

        g.translate(-x, -y);
        g.setColor(oldColor);
    }


    /**
     * Returns the amount of space taken up by a border drawn by
     * <code>drawEtchedRect()</code>
     *
     * @return  the inset of an etched rect
     */
    public static Insets getEtchedInsets() {
        return ETCHED_INSETS;
    }


    /**
     * Draws a groove.
     *
     * @param g an instance of {@code Graphics}
     * @param x an X coordinate
     * @param y an Y coordinate
     * @param w a width
     * @param h a height
     * @param shadow a color of shadow
     * @param highlight a color highlighting
     */
    public static void drawGroove(Graphics g, int x, int y, int w, int h,
                                  Color shadow, Color highlight)
    {
        Color oldColor = g.getColor();  // Make no net change to g
        g.translate(x, y);

        g.setColor(shadow);
        g.drawRect(0, 0, w-2, h-2);

        g.setColor(highlight);
        g.drawLine(1, h-3, 1, 1);
        g.drawLine(1, 1, w-3, 1);

        g.drawLine(0, h-1, w-1, h-1);
        g.drawLine(w-1, h-1, w-1, 0);

        g.translate(-x, -y);
        g.setColor(oldColor);
    }

    /**
     * Returns the amount of space taken up by a border drawn by
     * <code>drawGroove()</code>
     *
     * @return  the inset of a groove border
     */
    public static Insets getGrooveInsets() {
        return GROOVE_INSETS;
    }


    /**
     * Draws a bezel.
     *
     * @param g an instance of {@code Graphics}
     * @param x an X coordinate
     * @param y an Y coordinate
     * @param w a width
     * @param h a height
     * @param isPressed is component pressed
     * @param isDefault is default drawing
     * @param shadow a color of shadow
     * @param darkShadow a color of dark shadow
     * @param highlight a color highlighting
     * @param lightHighlight a color of light highlighting
     */
    public static void drawBezel(Graphics g, int x, int y, int w, int h,
                                 boolean isPressed, boolean isDefault,
                                 Color shadow, Color darkShadow,
                                 Color highlight, Color lightHighlight)
    {
        Color oldColor = g.getColor();  // Make no net change to g
        g.translate(x, y);

        if (isPressed && isDefault) {
            g.setColor(darkShadow);
            g.drawRect(0, 0, w - 1, h - 1);
            g.setColor(shadow);
            g.drawRect(1, 1, w - 3, h - 3);
        } else if (isPressed) {
            drawLoweredBezel(g, x, y, w, h,
                             shadow, darkShadow, highlight, lightHighlight);
        } else if (isDefault) {
            g.setColor(darkShadow);
            g.drawRect(0, 0, w-1, h-1);

            g.setColor(lightHighlight);
            g.drawLine(1, 1, 1, h-3);
            g.drawLine(2, 1, w-3, 1);

            g.setColor(highlight);
            g.drawLine(2, 2, 2, h-4);
            g.drawLine(3, 2, w-4, 2);

            g.setColor(shadow);
            g.drawLine(2, h-3, w-3, h-3);
            g.drawLine(w-3, 2, w-3, h-4);

            g.setColor(darkShadow);
            g.drawLine(1, h-2, w-2, h-2);
            g.drawLine(w-2, h-2, w-2, 1);
        } else {
            g.setColor(lightHighlight);
            g.drawLine(0, 0, 0, h-1);
            g.drawLine(1, 0, w-2, 0);

            g.setColor(highlight);
            g.drawLine(1, 1, 1, h-3);
            g.drawLine(2, 1, w-3, 1);

            g.setColor(shadow);
            g.drawLine(1, h-2, w-2, h-2);
            g.drawLine(w-2, 1, w-2, h-3);

            g.setColor(darkShadow);
            g.drawLine(0, h-1, w-1, h-1);
            g.drawLine(w-1, h-1, w-1, 0);
        }
        g.translate(-x, -y);
        g.setColor(oldColor);
    }

    /**
     * Draws a lowered bezel.
     *
     * @param g an instance of {@code Graphics}
     * @param x an X coordinate
     * @param y an Y coordinate
     * @param w a width
     * @param h a height
     * @param shadow a color of shadow
     * @param darkShadow a color of dark shadow
     * @param highlight a color highlighting
     * @param lightHighlight a color of light highlighting
     */
    public static void drawLoweredBezel(Graphics g, int x, int y, int w, int h,
                                        Color shadow, Color darkShadow,
                                        Color highlight, Color lightHighlight)  {
        g.setColor(darkShadow);
        g.drawLine(0, 0, 0, h-1);
        g.drawLine(1, 0, w-2, 0);

        g.setColor(shadow);
        g.drawLine(1, 1, 1, h-2);
        g.drawLine(1, 1, w-3, 1);

        g.setColor(lightHighlight);
        g.drawLine(0, h-1, w-1, h-1);
        g.drawLine(w-1, h-1, w-1, 0);

        g.setColor(highlight);
        g.drawLine(1, h-2, w-2, h-2);
        g.drawLine(w-2, h-2, w-2, 1);
     }


    /**
     * Draw a string with the graphics {@code g} at location (x,y)
     * just like {@code g.drawString} would. The first occurrence
     * of {@code underlineChar} in text will be underlined.
     * The matching algorithm is not case sensitive.
     *
     * @param g an instance of {@code Graphics}
     * @param text a text
     * @param underlinedChar an underlined char
     * @param x an X coordinate
     * @param y an Y coordinate
     */
    public static void drawString(Graphics g,String text,int underlinedChar,int x,int y) {
        int index=-1;

        if (underlinedChar != '\0') {
            char uc = Character.toUpperCase((char)underlinedChar);
            char lc = Character.toLowerCase((char)underlinedChar);
            int uci = text.indexOf(uc);
            int lci = text.indexOf(lc);

            if(uci == -1) {
                index = lci;
            }
            else if(lci == -1) {
                index = uci;
            }
            else {
                index = (lci < uci) ? lci : uci;
            }
        }
        drawStringUnderlineCharAt(g, text, index, x, y);
    }

    /**
     * Draw a string with the graphics <code>g</code> at location
     * (<code>x</code>, <code>y</code>)
     * just like <code>g.drawString</code> would.
     * The character at index <code>underlinedIndex</code>
     * in text will be underlined. If <code>index</code> is beyond the
     * bounds of <code>text</code> (including &lt; 0), nothing will be
     * underlined.
     *
     * @param g Graphics to draw with
     * @param text String to draw
     * @param underlinedIndex Index of character in text to underline
     * @param x x coordinate to draw at
     * @param y y coordinate to draw at
     * @since 1.4
     */
    public static void drawStringUnderlineCharAt(Graphics g, String text,
                           int underlinedIndex, int x,int y) {
        SwingUtilities2.drawStringUnderlineCharAt(null, g, text,
                underlinedIndex, x, y);
    }

    /**
     * Draws dashed rectangle.
     *
     * @param g an instance of {@code Graphics}
     * @param x an X coordinate
     * @param y an Y coordinate
     * @param width a width of rectangle
     * @param height a height of rectangle
     */
    public static void drawDashedRect(Graphics g,int x,int y,int width,int height) {
        int vx,vy;

        // draw upper and lower horizontal dashes
        for (vx = x; vx < (x + width); vx+=2) {
            g.fillRect(vx, y, 1, 1);
            g.fillRect(vx, y + height-1, 1, 1);
        }

        // draw left and right vertical dashes
        for (vy = y; vy < (y + height); vy+=2) {
            g.fillRect(x, vy, 1, 1);
            g.fillRect(x+width-1, vy, 1, 1);
        }
    }

    /**
     * Returns the preferred size of the button.
     *
     * @param b an instance of {@code AbstractButton}
     * @param textIconGap a gap between text and icon
     * @return the preferred size of the button
     */
    public static Dimension getPreferredButtonSize(AbstractButton b, int textIconGap)
    {
        if(b.getComponentCount() > 0) {
            return null;
        }

        Icon icon = b.getIcon();
        String text = b.getText();

        Font font = b.getFont();
        FontMetrics fm = b.getFontMetrics(font);

        Rectangle iconR = new Rectangle();
        Rectangle textR = new Rectangle();
        Rectangle viewR = new Rectangle(Short.MAX_VALUE, Short.MAX_VALUE);

        SwingUtilities.layoutCompoundLabel(
            b, fm, text, icon,
            b.getVerticalAlignment(), b.getHorizontalAlignment(),
            b.getVerticalTextPosition(), b.getHorizontalTextPosition(),
            viewR, iconR, textR, (text == null ? 0 : textIconGap)
        );

        /* The preferred size of the button is the size of
         * the text and icon rectangles plus the buttons insets.
         */

        Rectangle r = iconR.union(textR);

        Insets insets = b.getInsets();
        r.width += insets.left + insets.right;
        r.height += insets.top + insets.bottom;

        return r.getSize();
    }

    /*
     * Convenience function for determining ComponentOrientation.  Helps us
     * avoid having Munge directives throughout the code.
     */
    static boolean isLeftToRight( Component c ) {
        return c.getComponentOrientation().isLeftToRight();
    }

    static boolean isMenuShortcutKeyDown(InputEvent event) {
        return (event.getModifiersEx() &
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()) != 0;
    }

    /**
     * Draws the given string at the specified location using text properties
     * and anti-aliasing hints from the provided component.
     * Nothing is drawn for the null string.
     *
     * @param c the component that will display the string, may be null
     * @param g the graphics context, must not be null
     * @param string the string to display, may be null
     * @param x the x coordinate to draw the text at
     * @param y the y coordinate to draw the text at
     * @throws NullPointerException if the specified {@code g} is {@code null}
     *
     * @since 9
     */
    public static void drawString(JComponent c, Graphics2D g, String string,
                                  float x, float y) {
        SwingUtilities2.drawString(c, g, string, x, y, true);
    }

    /**
     * Draws the given string at the specified location underlining
     * the specified character. The provided component is used to query text
     * properties and anti-aliasing hints.
     * <p>
     * The {@code underlinedIndex} parameter points to a char value
     * (Unicode code unit) in the given string.
     * If the char value specified at the underlined index is in
     * the high-surrogate range and the char value at the following index is in
     * the low-surrogate range then the supplementary character corresponding
     * to this surrogate pair is underlined.
     * <p>
     * No character is underlined if the index is negative or greater
     * than the string length {@code (index < 0 || index >= string.length())}
     * or if the char value specified at the given index
     * is in the low-surrogate range.
     *
     * @param c the component that will display the string, may be null
     * @param g the graphics context, must not be null
     * @param string the string to display, may be null
     * @param underlinedIndex index of a a char value (Unicode code unit)
     *        in the string to underline
     * @param x the x coordinate to draw the text at
     * @param y the y coordinate to draw the text at
     * @throws NullPointerException if the specified {@code g} is {@code null}
     *
     * @see #getStringWidth
     *
     * @since 9
     */
    public static void drawStringUnderlineCharAt(JComponent c, Graphics2D g,
            String string, int underlinedIndex, float x, float y) {
        SwingUtilities2.drawStringUnderlineCharAt(c, g, string, underlinedIndex,
                                                  x, y, true);
    }

    /**
     * Clips the passed in string to the space provided.
     * The provided component is used to query text properties and anti-aliasing hints.
     * The unchanged string is returned if the space provided is greater than
     * the string width.
     *
     * @param c the component, may be null
     * @param fm the FontMetrics used to measure the string width, must be
     *           obtained from the correct font and graphics. Must not be null.
     * @param string the string to clip, may be null
     * @param availTextWidth the amount of space that the string can be drawn in
     * @return the clipped string that fits in the provided space, an empty
     *         string if the given string argument is {@code null} or empty
     * @throws NullPointerException if the specified {@code fm} is {@code null}
     *
     * @see #getStringWidth
     *
     * @since 9
     */
    public static String getClippedString(JComponent c, FontMetrics fm,
                                          String string, int availTextWidth) {
        return SwingUtilities2.clipStringIfNecessary(c, fm, string, availTextWidth);
    }

    /**
     * Returns the width of the passed in string using text properties
     * and anti-aliasing hints from the provided component.
     * If the passed string is {@code null}, returns zero.
     *
     * @param c the component, may be null
     * @param fm the FontMetrics used to measure the advance string width, must
     *           be obtained from the correct font and graphics. Must not be null.
     * @param string the string to get the advance width of, may be null
     * @return the advance width of the specified string, zero is returned for
     *         {@code null} string
     * @throws NullPointerException if the specified {@code fm} is {@code null}
     *
     * @since 9
     */
    public static float getStringWidth(JComponent c, FontMetrics fm, String string) {
        return SwingUtilities2.stringWidth(c, fm, string, true);
    }
}
