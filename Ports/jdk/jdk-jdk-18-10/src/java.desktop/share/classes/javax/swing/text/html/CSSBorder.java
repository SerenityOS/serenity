/*
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
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
package javax.swing.text.html;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Shape;
import java.util.HashMap;
import java.util.Map;
import javax.swing.border.AbstractBorder;
import javax.swing.text.AttributeSet;
import javax.swing.text.View;
import javax.swing.text.html.CSS.Attribute;
import javax.swing.text.html.CSS.BorderStyle;
import javax.swing.text.html.CSS.BorderWidthValue;
import javax.swing.text.html.CSS.ColorValue;
import javax.swing.text.html.CSS.CssValue;
import javax.swing.text.html.CSS.LengthValue;
import javax.swing.text.html.CSS.Value;

/**
 * CSS-style borders for HTML elements.
 *
 * @author Sergey Groznyh
 */
@SuppressWarnings("serial") // Superclass is not serializable across versions
class CSSBorder extends AbstractBorder {

    /** Indices for the attribute groups.  */
    static final int COLOR = 0, STYLE = 1, WIDTH = 2;

    /** Indices for the box sides within the attribute group.  */
    static final int TOP = 0, RIGHT = 1, BOTTOM = 2, LEFT = 3;

    /** The attribute groups.  */
    static final Attribute[][] ATTRIBUTES = {
        { Attribute.BORDER_TOP_COLOR, Attribute.BORDER_RIGHT_COLOR,
          Attribute.BORDER_BOTTOM_COLOR, Attribute.BORDER_LEFT_COLOR, },
        { Attribute.BORDER_TOP_STYLE, Attribute.BORDER_RIGHT_STYLE,
          Attribute.BORDER_BOTTOM_STYLE, Attribute.BORDER_LEFT_STYLE, },
        { Attribute.BORDER_TOP_WIDTH, Attribute.BORDER_RIGHT_WIDTH,
          Attribute.BORDER_BOTTOM_WIDTH, Attribute.BORDER_LEFT_WIDTH, },
    };

    /** Parsers for the border properties.  */
    static final CssValue[] PARSERS = {
        new ColorValue(), new BorderStyle(), new BorderWidthValue(null, 0),
    };

    /** Default values for the border properties.  */
    static final Object[] DEFAULTS = {
        Attribute.BORDER_COLOR, // marker: value will be computed on request
        PARSERS[1].parseCssValue(Attribute.BORDER_STYLE.getDefaultValue()),
        PARSERS[2].parseCssValue(Attribute.BORDER_WIDTH.getDefaultValue()),
    };

    /** Attribute set containing border properties.  */
    final AttributeSet attrs;

    /**
     * Initialize the attribute set.
     */
    CSSBorder(AttributeSet attrs) {
        this.attrs = attrs;
    }

    /**
     * Return the border color for the given side.
     */
    private Color getBorderColor(int side) {
        Object o = attrs.getAttribute(ATTRIBUTES[COLOR][side]);
        ColorValue cv;
        if (o instanceof ColorValue) {
            cv = (ColorValue) o;
        } else {
            // Marker for the default value.  Use 'color' property value as the
            // computed value of the 'border-color' property (CSS2 8.5.2)
            cv = (ColorValue) attrs.getAttribute(Attribute.COLOR);
            if (cv == null) {
                cv = (ColorValue) PARSERS[COLOR].parseCssValue(
                                            Attribute.COLOR.getDefaultValue());
            }
        }
        return cv.getValue();
    }

    /**
     * Return the border width for the given side.
     */
    private int getBorderWidth(int side) {
        int width = 0;
        BorderStyle bs = (BorderStyle) attrs.getAttribute(
                                                    ATTRIBUTES[STYLE][side]);
        if ((bs != null) && (bs.getValue() != Value.NONE)) {
            // The 'border-style' value of "none" forces the computed value
            // of 'border-width' to be 0 (CSS2 8.5.3)
            LengthValue bw = (LengthValue) attrs.getAttribute(
                                                    ATTRIBUTES[WIDTH][side]);
            if (bw == null) {
                bw = (LengthValue) DEFAULTS[WIDTH];
            }
            width = (int) bw.getValue(true);
        }
        return width;
    }

    /**
     * Return an array of border widths in the TOP, RIGHT, BOTTOM, LEFT order.
     */
    private int[] getWidths() {
        int[] widths = new int[4];
        for (int i = 0; i < widths.length; i++) {
            widths[i] = getBorderWidth(i);
        }
        return widths;
    }

    /**
     * Return the border style for the given side.
     */
    private Value getBorderStyle(int side) {
        BorderStyle style =
                    (BorderStyle) attrs.getAttribute(ATTRIBUTES[STYLE][side]);
        if (style == null) {
            style = (BorderStyle) DEFAULTS[STYLE];
        }
        return style.getValue();
    }

    /**
     * Return border shape for {@code side} as if the border has zero interior
     * length.  Shape start is at (0,0); points are added clockwise.
     */
    private Polygon getBorderShape(int side) {
        Polygon shape = null;
        int[] widths = getWidths();
        if (widths[side] != 0) {
            shape = new Polygon(new int[4], new int[4], 0);
            shape.addPoint(0, 0);
            shape.addPoint(-widths[(side + 3) % 4], -widths[side]);
            shape.addPoint(widths[(side + 1) % 4], -widths[side]);
            shape.addPoint(0, 0);
        }
        return shape;
    }

    /**
     * Return the border painter appropriate for the given side.
     */
    private BorderPainter getBorderPainter(int side) {
        Value style = getBorderStyle(side);
        return borderPainters.get(style);
    }

    /**
     * Return the color with brightness adjusted by the specified factor.
     *
     * The factor values are between 0.0 (no change) and 1.0 (turn into white).
     * Negative factor values decrease brigthness (ie, 1.0 turns into black).
     */
    static Color getAdjustedColor(Color c, double factor) {
        double f = 1 - Math.min(Math.abs(factor), 1);
        double inc = (factor > 0 ? 255 * (1 - f) : 0);
        return new Color((int) (c.getRed() * f + inc),
                         (int) (c.getGreen() * f + inc),
                         (int) (c.getBlue() * f + inc));
    }


    /* The javax.swing.border.Border methods.  */

    public Insets getBorderInsets(Component c, Insets insets) {
        int[] widths = getWidths();
        insets.set(widths[TOP], widths[LEFT], widths[BOTTOM], widths[RIGHT]);
        return insets;
    }

    public void paintBorder(Component c, Graphics g,
                                        int x, int y, int width, int height) {
        if (!(g instanceof Graphics2D)) {
            return;
        }

        Graphics2D g2 = (Graphics2D) g.create();

        int[] widths = getWidths();

        // Position and size of the border interior.
        int intX = x + widths[LEFT];
        int intY = y + widths[TOP];
        int intWidth = width - (widths[RIGHT] + widths[LEFT]);
        int intHeight = height - (widths[TOP] + widths[BOTTOM]);

        // Coordinates of the interior corners, from NW clockwise.
        int[][] intCorners = {
            { intX, intY },
            { intX + intWidth, intY },
            { intX + intWidth, intY + intHeight },
            { intX, intY + intHeight, },
        };

        // Draw the borders for all sides.
        for (int i = 0; i < 4; i++) {
            Value style = getBorderStyle(i);
            Polygon shape = getBorderShape(i);
            if ((style != Value.NONE) && (shape != null)) {
                int sideLength = (i % 2 == 0 ? intWidth : intHeight);

                // "stretch" the border shape by the interior area dimension
                shape.xpoints[2] += sideLength;
                shape.xpoints[3] += sideLength;
                Color color = getBorderColor(i);
                BorderPainter painter = getBorderPainter(i);

                double angle = i * Math.PI / 2;
                g2.setClip(g.getClip()); // Restore initial clip
                g2.translate(intCorners[i][0], intCorners[i][1]);
                g2.rotate(angle);
                g2.clip(shape);
                painter.paint(shape, g2, color, i);
                g2.rotate(-angle);
                g2.translate(-intCorners[i][0], -intCorners[i][1]);
            }
        }
        g2.dispose();
    }


    /* Border painters.  */

    interface BorderPainter {
        /**
         * The painter should paint the border as if it were at the top and the
         * coordinates of the NW corner of the interior area is (0, 0).  The
         * caller is responsible for the appropriate affine transformations.
         *
         * Clip is set by the caller to the exact border shape so it's safe to
         * simply draw into the shape's bounding rectangle.
         */
        void paint(Polygon shape, Graphics g, Color color, int side);
    }

    /**
     * Painter for the "none" and "hidden" CSS border styles.
     */
    static class NullPainter implements BorderPainter {
        public void paint(Polygon shape, Graphics g, Color color, int side) {
            // Do nothing.
        }
    }

    /**
     * Painter for the "solid" CSS border style.
     */
    static class SolidPainter implements BorderPainter {
        public void paint(Polygon shape, Graphics g, Color color, int side) {
            g.setColor(color);
            g.fillPolygon(shape);
        }
    }

    /**
     * Defines a method for painting strokes in the specified direction using
     * the given length and color patterns.
     */
    abstract static class StrokePainter implements BorderPainter {
        /**
         * Paint strokes repeatedly using the given length and color patterns.
         */
        void paintStrokes(Rectangle r, Graphics g, int axis,
                                int[] lengthPattern, Color[] colorPattern) {
            boolean xAxis = (axis == View.X_AXIS);
            int start = 0;
            int end = (xAxis ? r.width : r.height);
            while (start < end) {
                for (int i = 0; i < lengthPattern.length; i++) {
                    if (start >= end) {
                        break;
                    }
                    int length = lengthPattern[i];
                    Color c = colorPattern[i];
                    if (c != null) {
                        int x = r.x + (xAxis ? start : 0);
                        int y = r.y + (xAxis ? 0 : start);
                        int width = xAxis ? length : r.width;
                        int height = xAxis ? r.height : length;
                        g.setColor(c);
                        g.fillRect(x, y, width, height);
                    }
                    start += length;
                }
            }
        }
    }

    /**
     * Painter for the "double" CSS border style.
     */
    static class DoublePainter extends StrokePainter {
        public void paint(Polygon shape, Graphics g, Color color, int side) {
            Rectangle r = shape.getBounds();
            int length = Math.max(r.height / 3, 1);
            int[] lengthPattern = { length, length };
            Color[] colorPattern = { color, null };
            paintStrokes(r, g, View.Y_AXIS, lengthPattern, colorPattern);
        }
    }

    /**
     * Painter for the "dotted" and "dashed" CSS border styles.
     */
    static class DottedDashedPainter extends StrokePainter {
        final int factor;

        DottedDashedPainter(int factor) {
            this.factor = factor;
        }

        public void paint(Polygon shape, Graphics g, Color color, int side) {
            Rectangle r = shape.getBounds();
            int length = r.height * factor;
            int[] lengthPattern = { length, length };
            Color[] colorPattern = { color, null };
            paintStrokes(r, g, View.X_AXIS, lengthPattern, colorPattern);
        }
    }

    /**
     * Painter that defines colors for "shadow" and "light" border sides.
     */
    abstract static class ShadowLightPainter extends StrokePainter {
        /**
         * Return the "shadow" border side color.
         */
        static Color getShadowColor(Color c) {
            return CSSBorder.getAdjustedColor(c, -0.3);
        }

        /**
         * Return the "light" border side color.
         */
        static Color getLightColor(Color c) {
            return CSSBorder.getAdjustedColor(c, 0.7);
        }
    }

    /**
     * Painter for the "groove" and "ridge" CSS border styles.
     */
    static class GrooveRidgePainter extends ShadowLightPainter {
        final Value type;

        GrooveRidgePainter(Value type) {
            this.type = type;
        }

        public void paint(Polygon shape, Graphics g, Color color, int side) {
            Rectangle r = shape.getBounds();
            int length = Math.max(r.height / 2, 1);
            int[] lengthPattern = { length, length };
            Color[] colorPattern =
                             ((side + 1) % 4 < 2) == (type == Value.GROOVE) ?
                new Color[] { getShadowColor(color), getLightColor(color) } :
                new Color[] { getLightColor(color), getShadowColor(color) };
            paintStrokes(r, g, View.Y_AXIS, lengthPattern, colorPattern);
        }
    }

    /**
     * Painter for the "inset" and "outset" CSS border styles.
     */
    static class InsetOutsetPainter extends ShadowLightPainter {
        Value type;

        InsetOutsetPainter(Value type) {
            this.type = type;
        }

        public void paint(Polygon shape, Graphics g, Color color, int side) {
            g.setColor(((side + 1) % 4 < 2) == (type == Value.INSET) ?
                                getShadowColor(color) : getLightColor(color));
            g.fillPolygon(shape);
        }
    }

    /**
     * Add the specified painter to the painters map.
     */
    static void registerBorderPainter(Value style, BorderPainter painter) {
        borderPainters.put(style, painter);
    }

    /** Map the border style values to the border painter objects.  */
    static Map<Value, BorderPainter> borderPainters =
                                        new HashMap<Value, BorderPainter>();

    /* Initialize the border painters map with the pre-defined values.  */
    static {
        registerBorderPainter(Value.NONE, new NullPainter());
        registerBorderPainter(Value.HIDDEN, new NullPainter());
        registerBorderPainter(Value.SOLID, new SolidPainter());
        registerBorderPainter(Value.DOUBLE, new DoublePainter());
        registerBorderPainter(Value.DOTTED, new DottedDashedPainter(1));
        registerBorderPainter(Value.DASHED, new DottedDashedPainter(3));
        registerBorderPainter(Value.GROOVE, new GrooveRidgePainter(Value.GROOVE));
        registerBorderPainter(Value.RIDGE, new GrooveRidgePainter(Value.RIDGE));
        registerBorderPainter(Value.INSET, new InsetOutsetPainter(Value.INSET));
        registerBorderPainter(Value.OUTSET, new InsetOutsetPainter(Value.OUTSET));
    }
}
