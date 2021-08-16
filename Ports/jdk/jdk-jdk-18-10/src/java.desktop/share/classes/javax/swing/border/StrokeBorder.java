/*
 * Copyright (c) 2010, 2014, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.BasicStroke;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Paint;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.beans.ConstructorProperties;

/**
 * A class which implements a border of an arbitrary stroke.
 * <p>
 * <strong>Warning:</strong>
 * Serialized objects of this class will not be compatible with
 * future Swing releases. The current serialization support is
 * appropriate for short term storage or RMI
 * between applications running the same version of Swing.
 * As of 1.4, support for long term storage of all JavaBeans
 * has been added to the <code>java.beans</code> package.
 * Please see {@link java.beans.XMLEncoder}.
 *
 * @author Sergey A. Malenkov
 *
 * @since 1.7
 */
@SuppressWarnings("serial") // Same-version serialization only
public class StrokeBorder extends AbstractBorder {
    private final BasicStroke stroke;
    private final Paint paint;

    /**
     * Creates a border of the specified {@code stroke}.
     * The component's foreground color will be used to render the border.
     *
     * @param stroke  the {@link BasicStroke} object used to stroke a shape
     *
     * @throws NullPointerException if the specified {@code stroke} is {@code null}
     */
    public StrokeBorder(BasicStroke stroke) {
        this(stroke, null);
    }

    /**
     * Creates a border of the specified {@code stroke} and {@code paint}.
     * If the specified {@code paint} is {@code null},
     * the component's foreground color will be used to render the border.
     *
     * @param stroke  the {@link BasicStroke} object used to stroke a shape
     * @param paint   the {@link Paint} object used to generate a color
     *
     * @throws NullPointerException if the specified {@code stroke} is {@code null}
     */
    @ConstructorProperties({ "stroke", "paint" })
    public StrokeBorder(BasicStroke stroke, Paint paint) {
        if (stroke == null) {
            throw new NullPointerException("border's stroke");
        }
        this.stroke = stroke;
        this.paint = paint;
    }

    /**
     * Paints the border for the specified component
     * with the specified position and size.
     * If the border was not specified with a {@link Paint} object,
     * the component's foreground color will be used to render the border.
     * If the component's foreground color is not available,
     * the default color of the {@link Graphics} object will be used.
     *
     * @param c       the component for which this border is being painted
     * @param g       the paint graphics
     * @param x       the x position of the painted border
     * @param y       the y position of the painted border
     * @param width   the width of the painted border
     * @param height  the height of the painted border
     *
     * @throws NullPointerException if the specified {@code g} is {@code null}
     */
    @Override
    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        float size = this.stroke.getLineWidth();
        if (size > 0.0f) {
            g = g.create();
            if (g instanceof Graphics2D) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setStroke(this.stroke);
                g2d.setPaint(this.paint != null ? this.paint : c == null ? null : c.getForeground());
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                     RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.draw(new Rectangle2D.Float(x + size / 2, y + size / 2, width - size, height - size));
            }
            g.dispose();
        }
    }

    /**
     * Reinitializes the {@code insets} parameter
     * with this border's current insets.
     * Every inset is the smallest (closest to negative infinity) integer value
     * that is greater than or equal to the line width of the stroke
     * that is used to paint the border.
     *
     * @param c       the component for which this border insets value applies
     * @param insets  the {@code Insets} object to be reinitialized
     * @return the reinitialized {@code insets} parameter
     *
     * @throws NullPointerException if the specified {@code insets} is {@code null}
     *
     * @see Math#ceil
     */
    @Override
    public Insets getBorderInsets(Component c, Insets insets) {
        int size = (int) Math.ceil(this.stroke.getLineWidth());
        insets.set(size, size, size, size);
        return insets;
    }

    /**
     * Returns the {@link BasicStroke} object used to stroke a shape
     * during the border rendering.
     *
     * @return the {@link BasicStroke} object
     */
    public BasicStroke getStroke() {
        return this.stroke;
    }

    /**
     * Returns the {@link Paint} object used to generate a color
     * during the border rendering.
     *
     * @return the {@link Paint} object or {@code null}
     *         if the {@code paint} parameter is not set
     */
    public Paint getPaint() {
        return this.paint;
    }
}
