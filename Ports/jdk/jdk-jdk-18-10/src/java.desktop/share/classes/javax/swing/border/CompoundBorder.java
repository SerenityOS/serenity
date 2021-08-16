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
package javax.swing.border;

import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Component;
import java.beans.ConstructorProperties;

/**
 * A composite Border class used to compose two Border objects
 * into a single border by nesting an inside Border object within
 * the insets of an outside Border object.
 *
 * For example, this class may be used to add blank margin space
 * to a component with an existing decorative border:
 *
 * <pre>
 *    Border border = comp.getBorder();
 *    Border margin = new EmptyBorder(10,10,10,10);
 *    comp.setBorder(new CompoundBorder(border, margin));
 * </pre>
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
@SuppressWarnings("serial")
public class CompoundBorder extends AbstractBorder {
    /**
     * The outside border.
     */
    protected Border outsideBorder;
    /**
     * The inside border.
     */
    protected Border insideBorder;

    /**
     * Creates a compound border with null outside and inside borders.
     */
    public CompoundBorder() {
        this.outsideBorder = null;
        this.insideBorder = null;
    }

    /**
     * Creates a compound border with the specified outside and
     * inside borders.  Either border may be null.
     * @param outsideBorder the outside border
     * @param insideBorder the inside border to be nested
     */
    @ConstructorProperties({"outsideBorder", "insideBorder"})
    public CompoundBorder(Border outsideBorder, Border insideBorder) {
        this.outsideBorder = outsideBorder;
        this.insideBorder = insideBorder;
    }

    /**
     * Returns whether or not the compound border is opaque.
     *
     * @return {@code true} if the inside and outside borders
     *         are each either {@code null} or opaque;
     *         or {@code false} otherwise
     */
    @Override
    public boolean isBorderOpaque() {
        return (outsideBorder == null || outsideBorder.isBorderOpaque()) &&
               (insideBorder == null || insideBorder.isBorderOpaque());
    }

    /**
     * Paints the compound border by painting the outside border
     * with the specified position and size and then painting the
     * inside border at the specified position and size offset by
     * the insets of the outside border.
     * @param c the component for which this border is being painted
     * @param g the paint graphics
     * @param x the x position of the painted border
     * @param y the y position of the painted border
     * @param width the width of the painted border
     * @param height the height of the painted border
     */
    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        Insets  nextInsets;
        int px, py, pw, ph;

        px = x;
        py = y;
        pw = width;
        ph = height;

        if(outsideBorder != null) {
            outsideBorder.paintBorder(c, g, px, py, pw, ph);

            nextInsets = outsideBorder.getBorderInsets(c);
            px += nextInsets.left;
            py += nextInsets.top;
            pw = pw - nextInsets.right - nextInsets.left;
            ph = ph - nextInsets.bottom - nextInsets.top;
        }
        if(insideBorder != null)
            insideBorder.paintBorder(c, g, px, py, pw, ph);

    }

    /**
     * Reinitialize the insets parameter with this Border's current Insets.
     * @param c the component for which this border insets value applies
     * @param insets the object to be reinitialized
     */
    public Insets getBorderInsets(Component c, Insets insets) {
        Insets  nextInsets;

        insets.top = insets.left = insets.right = insets.bottom = 0;
        if(outsideBorder != null) {
            nextInsets = outsideBorder.getBorderInsets(c);
            insets.top += nextInsets.top;
            insets.left += nextInsets.left;
            insets.right += nextInsets.right;
            insets.bottom += nextInsets.bottom;
        }
        if(insideBorder != null) {
            nextInsets = insideBorder.getBorderInsets(c);
            insets.top += nextInsets.top;
            insets.left += nextInsets.left;
            insets.right += nextInsets.right;
            insets.bottom += nextInsets.bottom;
        }
        return insets;
    }

    /**
     * Returns the outside border object.
     *
     * @return the outside {@code Border} object
     */
    public Border getOutsideBorder() {
        return outsideBorder;
    }

    /**
     * Returns the inside border object.
     *
     * @return the inside {@code Border} object
     */
    public Border getInsideBorder() {
        return insideBorder;
    }
}
