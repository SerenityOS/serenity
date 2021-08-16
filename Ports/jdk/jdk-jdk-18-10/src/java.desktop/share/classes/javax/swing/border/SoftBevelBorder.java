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
package javax.swing.border;

import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Color;
import java.awt.Component;
import java.beans.ConstructorProperties;

/**
 * A class which implements a raised or lowered bevel with
 * softened corners.
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
 * @author Amy Fowler
 * @author Chester Rose
 */
@SuppressWarnings("serial") // Same-version serialization only
public class SoftBevelBorder extends BevelBorder
{

    /**
     * Creates a bevel border with the specified type and whose
     * colors will be derived from the background color of the
     * component passed into the paintBorder method.
     * @param bevelType the type of bevel for the border
     */
    public SoftBevelBorder(int bevelType) {
        super(bevelType);
    }

    /**
     * Creates a bevel border with the specified type, highlight and
     * shadow colors.
     * @param bevelType the type of bevel for the border
     * @param highlight the color to use for the bevel highlight
     * @param shadow the color to use for the bevel shadow
     */
    public SoftBevelBorder(int bevelType, Color highlight, Color shadow) {
        super(bevelType, highlight, shadow);
    }

    /**
     * Creates a bevel border with the specified type, highlight
     * shadow colors.
     * @param bevelType the type of bevel for the border
     * @param highlightOuterColor the color to use for the bevel outer highlight
     * @param highlightInnerColor the color to use for the bevel inner highlight
     * @param shadowOuterColor the color to use for the bevel outer shadow
     * @param shadowInnerColor the color to use for the bevel inner shadow
     */
    @ConstructorProperties({"bevelType", "highlightOuterColor", "highlightInnerColor", "shadowOuterColor", "shadowInnerColor"})
    public SoftBevelBorder(int bevelType, Color highlightOuterColor,
                        Color highlightInnerColor, Color shadowOuterColor,
                        Color shadowInnerColor) {
        super(bevelType, highlightOuterColor, highlightInnerColor,
              shadowOuterColor, shadowInnerColor);
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
        Color oldColor = g.getColor();
        g.translate(x, y);

        if (bevelType == RAISED) {
            g.setColor(getHighlightOuterColor(c));
            g.drawLine(0, 0, width-2, 0);
            g.drawLine(0, 0, 0, height-2);
            g.drawLine(1, 1, 1, 1);

            g.setColor(getHighlightInnerColor(c));
            g.drawLine(2, 1, width-2, 1);
            g.drawLine(1, 2, 1, height-2);
            g.drawLine(2, 2, 2, 2);
            g.drawLine(0, height-1, 0, height-2);
            g.drawLine(width-1, 0, width-1, 0);

            g.setColor(getShadowOuterColor(c));
            g.drawLine(2, height-1, width-1, height-1);
            g.drawLine(width-1, 2, width-1, height-1);

            g.setColor(getShadowInnerColor(c));
            g.drawLine(width-2, height-2, width-2, height-2);


        } else if (bevelType == LOWERED) {
            g.setColor(getShadowOuterColor(c));
            g.drawLine(0, 0, width-2, 0);
            g.drawLine(0, 0, 0, height-2);
            g.drawLine(1, 1, 1, 1);

            g.setColor(getShadowInnerColor(c));
            g.drawLine(2, 1, width-2, 1);
            g.drawLine(1, 2, 1, height-2);
            g.drawLine(2, 2, 2, 2);
            g.drawLine(0, height-1, 0, height-2);
            g.drawLine(width-1, 0, width-1, 0);

            g.setColor(getHighlightOuterColor(c));
            g.drawLine(2, height-1, width-1, height-1);
            g.drawLine(width-1, 2, width-1, height-1);

            g.setColor(getHighlightInnerColor(c));
            g.drawLine(width-2, height-2, width-2, height-2);
        }
        g.translate(-x, -y);
        g.setColor(oldColor);
    }

    /**
     * Reinitialize the insets parameter with this Border's current Insets.
     * @param c the component for which this border insets value applies
     * @param insets the object to be reinitialized
     */
    public Insets getBorderInsets(Component c, Insets insets)       {
        insets.set(3, 3, 3, 3);
        return insets;
    }

    /**
     * Returns whether or not the border is opaque.
     */
    public boolean isBorderOpaque() { return false; }

}
