/*
 * Copyright (c) 1998, 2014, Oracle and/or its affiliates. All rights reserved.
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
/*
 *
 * (C) Copyright IBM Corp. 1998, All Rights Reserved
 */

package sun.font;

import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.Stroke;

import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;

import java.awt.font.TextAttribute;

import java.util.concurrent.ConcurrentHashMap;

/**
 * This class provides drawing and bounds-measurement of
 * underlines.  Additionally, it has a factory method for
 * obtaining underlines from values of underline attributes.
 */

abstract class Underline {

    /**
     * Draws the underline into g2d.  The thickness should be obtained
     * from a LineMetrics object.  Note that some underlines ignore the
     * thickness parameter.
     * The underline is drawn from (x1, y) to (x2, y).
     */
    abstract void drawUnderline(Graphics2D g2d,
                                float thickness,
                                float x1,
                                float x2,
                                float y);

    /**
     * Returns the bottom of the bounding rectangle for this underline.
     */
    abstract float getLowerDrawLimit(float thickness);

    /**
     * Returns a Shape representing the underline.  The thickness should be obtained
     * from a LineMetrics object.  Note that some underlines ignore the
     * thickness parameter.
     */
    abstract Shape getUnderlineShape(float thickness,
                                     float x1,
                                     float x2,
                                     float y);

     // Implementation of underline for standard and Input Method underlines.
     // These classes are private.

    // IM Underlines ignore thickness param, and instead use
    // DEFAULT_THICKNESS
    private static final float DEFAULT_THICKNESS = 1.0f;

    // StandardUnderline's constructor takes a boolean param indicating
    // whether to override the default thickness.  These values clarify
    // the semantics of the parameter.
    private static final boolean USE_THICKNESS = true;
    private static final boolean IGNORE_THICKNESS = false;

    // Implementation of standard underline and all input method underlines
    // except UNDERLINE_LOW_GRAY.
    private static final class StandardUnderline extends Underline {

        // the amount by which to move the underline
        private float shift;

        // the actual line thickness is this value times
        // the requested thickness
        private float thicknessMultiplier;

        // if non-null, underline is drawn with a BasicStroke
        // with this dash pattern
        private float[] dashPattern;

        // if false, all underlines are DEFAULT_THICKNESS thick
        // if true, use thickness param
        private boolean useThickness;

        // cached BasicStroke
        private BasicStroke cachedStroke;

        StandardUnderline(float shift,
                          float thicknessMultiplier,
                          float[] dashPattern,
                          boolean useThickness) {

            this.shift = shift;
            this.thicknessMultiplier = thicknessMultiplier;
            this.dashPattern = dashPattern;
            this.useThickness = useThickness;
            this.cachedStroke = null;
        }

        private BasicStroke createStroke(float lineThickness) {

            if (dashPattern == null) {
                return new BasicStroke(lineThickness,
                                       BasicStroke.CAP_BUTT,
                                       BasicStroke.JOIN_MITER);
            }
            else {
                return new BasicStroke(lineThickness,
                                       BasicStroke.CAP_BUTT,
                                       BasicStroke.JOIN_MITER,
                                       10.0f,
                                       dashPattern,
                                       0);
            }
        }

        private float getLineThickness(float thickness) {

            if (useThickness) {
                return thickness * thicknessMultiplier;
            }
            else {
                return DEFAULT_THICKNESS * thicknessMultiplier;
            }
        }

        private Stroke getStroke(float thickness) {

            float lineThickness = getLineThickness(thickness);
            BasicStroke stroke = cachedStroke;
            if (stroke == null ||
                    stroke.getLineWidth() != lineThickness) {

                stroke = createStroke(lineThickness);
                cachedStroke = stroke;
            }

            return stroke;
        }

        void drawUnderline(Graphics2D g2d,
                           float thickness,
                           float x1,
                           float x2,
                           float y) {


            Stroke saveStroke = g2d.getStroke();
            g2d.setStroke(getStroke(thickness));
            g2d.draw(new Line2D.Float(x1, y + shift, x2, y + shift));
            g2d.setStroke(saveStroke);
        }

        float getLowerDrawLimit(float thickness) {

            return shift + getLineThickness(thickness);
        }

        Shape getUnderlineShape(float thickness,
                                float x1,
                                float x2,
                                float y) {

            Stroke ulStroke = getStroke(thickness);
            Line2D line = new Line2D.Float(x1, y + shift, x2, y + shift);
            return ulStroke.createStrokedShape(line);
        }
    }

    // Implementation of UNDERLINE_LOW_GRAY.
    private static class IMGrayUnderline extends Underline {

        private BasicStroke stroke;

        IMGrayUnderline() {
            stroke = new BasicStroke(DEFAULT_THICKNESS,
                                     BasicStroke.CAP_BUTT,
                                     BasicStroke.JOIN_MITER,
                                     10.0f,
                                     new float[] {1, 1},
                                     0);
        }

        void drawUnderline(Graphics2D g2d,
                           float thickness,
                           float x1,
                           float x2,
                           float y) {

            Stroke saveStroke = g2d.getStroke();
            g2d.setStroke(stroke);

            Line2D.Float drawLine = new Line2D.Float(x1, y, x2, y);
            g2d.draw(drawLine);

            drawLine.y1 += DEFAULT_THICKNESS;
            drawLine.y2 += DEFAULT_THICKNESS;
            drawLine.x1 += DEFAULT_THICKNESS;

            g2d.draw(drawLine);

            g2d.setStroke(saveStroke);
        }

        float getLowerDrawLimit(float thickness) {

            return DEFAULT_THICKNESS * 2;
        }

        Shape getUnderlineShape(float thickness,
                                float x1,
                                float x2,
                                float y) {

            GeneralPath gp = new GeneralPath();

            Line2D.Float line = new Line2D.Float(x1, y, x2, y);
            gp.append(stroke.createStrokedShape(line), false);

            line.y1 += DEFAULT_THICKNESS;
            line.y2 += DEFAULT_THICKNESS;
            line.x1 += DEFAULT_THICKNESS;

            gp.append(stroke.createStrokedShape(line), false);

            return gp;
        }
    }

     // Keep a map of underlines, one for each type
     // of underline.  The Underline objects are Flyweights
     // (shared across multiple clients), so they should be immutable.
     // If this implementation changes then clone underline
     // instances in getUnderline before returning them.
    private static final ConcurrentHashMap<Object, Underline>
        UNDERLINES = new ConcurrentHashMap<Object, Underline>(6);
    private static final Underline[] UNDERLINE_LIST;

    static {
        Underline[] uls = new Underline[6];

        uls[0] = new StandardUnderline(0, 1, null, USE_THICKNESS);
        UNDERLINES.put(TextAttribute.UNDERLINE_ON, uls[0]);

        uls[1] = new StandardUnderline(1, 1, null, IGNORE_THICKNESS);
        UNDERLINES.put(TextAttribute.UNDERLINE_LOW_ONE_PIXEL, uls[1]);

        uls[2] = new StandardUnderline(1, 2, null, IGNORE_THICKNESS);
        UNDERLINES.put(TextAttribute.UNDERLINE_LOW_TWO_PIXEL, uls[2]);

        uls[3] = new StandardUnderline(1, 1, new float[] { 1, 1 }, IGNORE_THICKNESS);
        UNDERLINES.put(TextAttribute.UNDERLINE_LOW_DOTTED, uls[3]);

        uls[4] = new IMGrayUnderline();
        UNDERLINES.put(TextAttribute.UNDERLINE_LOW_GRAY, uls[4]);

        uls[5] = new StandardUnderline(1, 1, new float[] { 4, 4 }, IGNORE_THICKNESS);
        UNDERLINES.put(TextAttribute.UNDERLINE_LOW_DASHED, uls[5]);

        UNDERLINE_LIST = uls;
    }

    /**
     * Return the Underline for the given value of
     * TextAttribute.INPUT_METHOD_UNDERLINE or
     * TextAttribute.UNDERLINE.
     * If value is not an input method underline value or
     * TextAttribute.UNDERLINE_ON, null is returned.
     */
    static Underline getUnderline(Object value) {

        if (value == null) {
            return null;
        }

        return UNDERLINES.get(value);
    }

    static Underline getUnderline(int index) {
        return index < 0 ? null : UNDERLINE_LIST[index];
    }
}
