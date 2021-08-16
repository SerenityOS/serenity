/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
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
package javax.swing.border;

import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Color;
import java.awt.Component;
import java.beans.ConstructorProperties;

/**
 * A class which implements a simple two-line bevel border.
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
 */
@SuppressWarnings("serial") // Same-version serialization only
public class BevelBorder extends AbstractBorder
{
    /** Raised bevel type. */
    public static final int RAISED  = 0;
    /** Lowered bevel type. */
    public static final int LOWERED = 1;

    /**
     * The bevel type.
     */
    protected int bevelType;
    /**
     * The color to use for the bevel outer highlight.
     */
    protected Color highlightOuter;
    /**
     * The color to use for the bevel inner highlight.
     */
    protected Color highlightInner;
    /**
     * The color to use for the bevel inner shadow.
     */
    protected Color shadowInner;
    /**
     * the color to use for the bevel outer shadow
     */
    protected Color shadowOuter;

    /**
     * Creates a bevel border with the specified type and whose
     * colors will be derived from the background color of the
     * component passed into the paintBorder method.
     * @param bevelType the type of bevel for the border
     */
    public BevelBorder(int bevelType) {
        this.bevelType = bevelType;
    }

    /**
     * Creates a bevel border with the specified type, highlight and
     * shadow colors.
     * The bevel outer highlight color and bevel inner highlight color
     * will be derived from specified highlight color and
     * bevel outer shadow color and bevel inner shadow color
     * will be derived from specified shadow color.
     * @param bevelType the type of bevel for the border
     * @param highlight the color to use for the bevel highlight
     * @param shadow the color to use for the bevel shadow
     */
    public BevelBorder(int bevelType, Color highlight, Color shadow) {
        this(bevelType, highlight.brighter(), highlight, shadow, shadow.brighter());
    }

    /**
     * Creates a bevel border with the specified type, highlight and
     * shadow colors.
     *
     * @param bevelType the type of bevel for the border
     * @param highlightOuterColor the color to use for the bevel outer highlight
     * @param highlightInnerColor the color to use for the bevel inner highlight
     * @param shadowOuterColor the color to use for the bevel outer shadow
     * @param shadowInnerColor the color to use for the bevel inner shadow
     */
    @ConstructorProperties({"bevelType", "highlightOuterColor", "highlightInnerColor", "shadowOuterColor", "shadowInnerColor"})
    public BevelBorder(int bevelType, Color highlightOuterColor,
                       Color highlightInnerColor, Color shadowOuterColor,
                       Color shadowInnerColor) {
        this(bevelType);
        this.highlightOuter = highlightOuterColor;
        this.highlightInner = highlightInnerColor;
        this.shadowOuter = shadowOuterColor;
        this.shadowInner = shadowInnerColor;
    }

    /**
     * Paints the border for the specified component with the specified
     * position and size.
     * @param c the component for which this border is being painted
     * @param g the paint graphics
     * @param x the x position of the painted border
     * @param y the y position of the painted border
     * @param width the width of the painted border
     * @param height the height of the painted border
     */
    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        if (bevelType == RAISED) {
             paintRaisedBevel(c, g, x, y, width, height);

        } else if (bevelType == LOWERED) {
             paintLoweredBevel(c, g, x, y, width, height);
        }
    }

    /**
     * Reinitialize the insets parameter with this Border's current Insets.
     * @param c the component for which this border insets value applies
     * @param insets the object to be reinitialized
     */
    public Insets getBorderInsets(Component c, Insets insets) {
        insets.set(2, 2, 2, 2);
        return insets;
    }

    /**
     * Returns the outer highlight color of the bevel border
     * when rendered on the specified component.  If no highlight
     * color was specified at instantiation, the highlight color
     * is derived from the specified component's background color.
     *
     * @param c the component for which the highlight may be derived
     * @return the outer highlight {@code Color}
     * @since 1.3
     */
    public Color getHighlightOuterColor(Component c)   {
        Color highlight = getHighlightOuterColor();
        return highlight != null? highlight :
                                       c.getBackground().brighter().brighter();
    }

    /**
     * Returns the inner highlight color of the bevel border
     * when rendered on the specified component.  If no highlight
     * color was specified at instantiation, the highlight color
     * is derived from the specified component's background color.
     *
     * @param c the component for which the highlight may be derived
     * @return the inner highlight {@code Color}
     * @since 1.3
     */
    public Color getHighlightInnerColor(Component c)   {
        Color highlight = getHighlightInnerColor();
        return highlight != null? highlight :
                                       c.getBackground().brighter();
    }

    /**
     * Returns the inner shadow color of the bevel border
     * when rendered on the specified component.  If no shadow
     * color was specified at instantiation, the shadow color
     * is derived from the specified component's background color.
     *
     * @param c the component for which the shadow may be derived
     * @return the inner shadow {@code Color}
     * @since 1.3
     */
    public Color getShadowInnerColor(Component c)      {
        Color shadow = getShadowInnerColor();
        return shadow != null? shadow :
                                    c.getBackground().darker();
    }

    /**
     * Returns the outer shadow color of the bevel border
     * when rendered on the specified component.  If no shadow
     * color was specified at instantiation, the shadow color
     * is derived from the specified component's background color.
     *
     * @param c the component for which the shadow may be derived
     * @return the outer shadow {@code Color}
     * @since 1.3
     */
    public Color getShadowOuterColor(Component c)      {
        Color shadow = getShadowOuterColor();
        return shadow != null? shadow :
                                    c.getBackground().darker().darker();
    }

    /**
     * Returns the outer highlight color of the bevel border.
     * Will return null if no highlight color was specified
     * at instantiation.
     *
     * @return the outer highlight {@code Color} or {@code null} if no highlight
     *         color was specified
     * @since 1.3
     */
    public Color getHighlightOuterColor()   {
        return highlightOuter;
    }

    /**
     * Returns the inner highlight color of the bevel border.
     * Will return null if no highlight color was specified
     * at instantiation.
     *
     * @return the inner highlight {@code Color} or {@code null} if no highlight
     *         color was specified
     * @since 1.3
     */
    public Color getHighlightInnerColor()   {
        return highlightInner;
    }

    /**
     * Returns the inner shadow color of the bevel border.
     * Will return null if no shadow color was specified
     * at instantiation.
     *
     * @return the inner shadow {@code Color} or {@code null} if no shadow color
     *         was specified
     * @since 1.3
     */
    public Color getShadowInnerColor()      {
        return shadowInner;
    }

    /**
     * Returns the outer shadow color of the bevel border.
     * Will return null if no shadow color was specified
     * at instantiation.
     *
     * @return the outer shadow {@code Color} or {@code null} if no shadow color
     *         was specified
     * @since 1.3
     */
    public Color getShadowOuterColor()      {
        return shadowOuter;
    }

    /**
     * Returns the type of the bevel border.
     *
     * @return the bevel border type, either {@code RAISED} or {@code LOWERED}
     */
    public int getBevelType()       {
        return bevelType;
    }

    /**
     * Returns whether or not the border is opaque. This implementation
     * returns {@code true}.
     *
     * @return true
     */
    public boolean isBorderOpaque() { return true; }

    /**
     * Paints a raised bevel for the specified component with the specified
     * position and size.
     *
     * @param c the component for which the raised bevel is being painted
     * @param g the paint graphics
     * @param x the x position of the raised bevel
     * @param y the y position of the raised bevel
     * @param width the width of the raised bevel
     * @param height the height of the raised bevel
     */
    protected void paintRaisedBevel(Component c, Graphics g, int x, int y,
                                    int width, int height)  {
        Color oldColor = g.getColor();
        int h = height;
        int w = width;

        g.translate(x, y);

        g.setColor(getHighlightOuterColor(c));
        g.drawLine(0, 0, 0, h-2);
        g.drawLine(1, 0, w-2, 0);

        g.setColor(getHighlightInnerColor(c));
        g.drawLine(1, 1, 1, h-3);
        g.drawLine(2, 1, w-3, 1);

        g.setColor(getShadowOuterColor(c));
        g.drawLine(0, h-1, w-1, h-1);
        g.drawLine(w-1, 0, w-1, h-2);

        g.setColor(getShadowInnerColor(c));
        g.drawLine(1, h-2, w-2, h-2);
        g.drawLine(w-2, 1, w-2, h-3);

        g.translate(-x, -y);
        g.setColor(oldColor);

    }

    /**
     * Paints a lowered bevel for the specified component with the specified
     * position and size.
     *
     * @param c the component for which the lowered bevel is being painted
     * @param g the paint graphics
     * @param x the x position of the lowered bevel
     * @param y the y position of the lowered bevel
     * @param width the width of the lowered bevel
     * @param height the height of the lowered bevel
     */
    protected void paintLoweredBevel(Component c, Graphics g, int x, int y,
                                        int width, int height)  {
        Color oldColor = g.getColor();
        int h = height;
        int w = width;

        g.translate(x, y);

        g.setColor(getShadowInnerColor(c));
        g.drawLine(0, 0, 0, h-1);
        g.drawLine(1, 0, w-1, 0);

        g.setColor(getShadowOuterColor(c));
        g.drawLine(1, 1, 1, h-2);
        g.drawLine(2, 1, w-2, 1);

        g.setColor(getHighlightOuterColor(c));
        g.drawLine(1, h-1, w-1, h-1);
        g.drawLine(w-1, 1, w-1, h-2);

        g.setColor(getHighlightInnerColor(c));
        g.drawLine(2, h-2, w-2, h-2);
        g.drawLine(w-2, 2, w-2, h-3);

        g.translate(-x, -y);
        g.setColor(oldColor);

    }

}
