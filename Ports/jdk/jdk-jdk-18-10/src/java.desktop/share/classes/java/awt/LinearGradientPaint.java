/*
 * Copyright (c) 2006, 2017, Oracle and/or its affiliates. All rights reserved.
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

package java.awt;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.ColorModel;
import java.beans.ConstructorProperties;

/**
 * The {@code LinearGradientPaint} class provides a way to fill
 * a {@link java.awt.Shape} with a linear color gradient pattern.  The user
 * may specify two or more gradient colors, and this paint will provide an
 * interpolation between each color.  The user also specifies start and end
 * points which define where in user space the color gradient should begin
 * and end.
 * <p>
 * The user must provide an array of floats specifying how to distribute the
 * colors along the gradient.  These values should range from 0.0 to 1.0 and
 * act like keyframes along the gradient (they mark where the gradient should
 * be exactly a particular color).
 * <p>
 * In the event that the user does not set the first keyframe value equal
 * to 0 and/or the last keyframe value equal to 1, keyframes will be created
 * at these positions and the first and last colors will be replicated there.
 * So, if a user specifies the following arrays to construct a gradient:<br>
 * <pre>
 *     {Color.BLUE, Color.RED}, {.3f, .7f}
 * </pre>
 * this will be converted to a gradient with the following keyframes:<br>
 * <pre>
 *     {Color.BLUE, Color.BLUE, Color.RED, Color.RED}, {0f, .3f, .7f, 1f}
 * </pre>
 *
 * <p>
 * The user may also select what action the {@code LinearGradientPaint} object
 * takes when it is filling the space outside the start and end points by
 * setting {@code CycleMethod} to either {@code REFLECTION} or {@code REPEAT}.
 * The distances between any two colors in any of the reflected or repeated
 * copies of the gradient are the same as the distance between those same two
 * colors between the start and end points.
 * Note that some minor variations in distances may occur due to sampling at
 * the granularity of a pixel.
 * If no cycle method is specified, {@code NO_CYCLE} will be chosen by
 * default, which means the endpoint colors will be used to fill the
 * remaining area.
 * <p>
 * The colorSpace parameter allows the user to specify in which colorspace
 * the interpolation should be performed, default sRGB or linearized RGB.
 *
 * <p>
 * The following code demonstrates typical usage of
 * {@code LinearGradientPaint}:
 * <pre>
 *     Point2D start = new Point2D.Float(0, 0);
 *     Point2D end = new Point2D.Float(50, 50);
 *     float[] dist = {0.0f, 0.2f, 1.0f};
 *     Color[] colors = {Color.RED, Color.WHITE, Color.BLUE};
 *     LinearGradientPaint p =
 *         new LinearGradientPaint(start, end, dist, colors);
 * </pre>
 * <p>
 * This code will create a {@code LinearGradientPaint} which interpolates
 * between red and white for the first 20% of the gradient and between white
 * and blue for the remaining 80%.
 *
 * <p>
 * This image demonstrates the example code above for each
 * of the three cycle methods:
 * <p style="text-align:center">
 * <img src = "doc-files/LinearGradientPaint.png"
 * alt="image showing the output of the example code">
 *
 * @see java.awt.Paint
 * @see java.awt.Graphics2D#setPaint
 * @author Nicholas Talian, Vincent Hardy, Jim Graham, Jerry Evans
 * @since 1.6
 */
public final class LinearGradientPaint extends MultipleGradientPaint {

    /** Gradient start and end points. */
    private final Point2D start, end;

    /**
     * Constructs a {@code LinearGradientPaint} with a default
     * {@code NO_CYCLE} repeating method and {@code SRGB} color space.
     *
     * @param startX the X coordinate of the gradient axis start point
     *               in user space
     * @param startY the Y coordinate of the gradient axis start point
     *               in user space
     * @param endX   the X coordinate of the gradient axis end point
     *               in user space
     * @param endY   the Y coordinate of the gradient axis end point
     *               in user space
     * @param fractions numbers ranging from 0.0 to 1.0 specifying the
     *                  distribution of colors along the gradient
     * @param colors array of colors corresponding to each fractional value
     *
     * @throws NullPointerException
     * if {@code fractions} array is null,
     * or {@code colors} array is null,
     * @throws IllegalArgumentException
     * if start and end points are the same points,
     * or {@code fractions.length != colors.length},
     * or {@code colors} is less than 2 in size,
     * or a {@code fractions} value is less than 0.0 or greater than 1.0,
     * or the {@code fractions} are not provided in strictly increasing order
     */
    public LinearGradientPaint(float startX, float startY,
                               float endX, float endY,
                               float[] fractions, Color[] colors)
    {
        this(new Point2D.Float(startX, startY),
             new Point2D.Float(endX, endY),
             fractions,
             colors,
             CycleMethod.NO_CYCLE);
    }

    /**
     * Constructs a {@code LinearGradientPaint} with a default {@code SRGB}
     * color space.
     *
     * @param startX the X coordinate of the gradient axis start point
     *               in user space
     * @param startY the Y coordinate of the gradient axis start point
     *               in user space
     * @param endX   the X coordinate of the gradient axis end point
     *               in user space
     * @param endY   the Y coordinate of the gradient axis end point
     *               in user space
     * @param fractions numbers ranging from 0.0 to 1.0 specifying the
     *                  distribution of colors along the gradient
     * @param colors array of colors corresponding to each fractional value
     * @param cycleMethod either {@code NO_CYCLE}, {@code REFLECT},
     *                    or {@code REPEAT}
     *
     * @throws NullPointerException
     * if {@code fractions} array is null,
     * or {@code colors} array is null,
     * or {@code cycleMethod} is null
     * @throws IllegalArgumentException
     * if start and end points are the same points,
     * or {@code fractions.length != colors.length},
     * or {@code colors} is less than 2 in size,
     * or a {@code fractions} value is less than 0.0 or greater than 1.0,
     * or the {@code fractions} are not provided in strictly increasing order
     */
    public LinearGradientPaint(float startX, float startY,
                               float endX, float endY,
                               float[] fractions, Color[] colors,
                               CycleMethod cycleMethod)
    {
        this(new Point2D.Float(startX, startY),
             new Point2D.Float(endX, endY),
             fractions,
             colors,
             cycleMethod);
    }

    /**
     * Constructs a {@code LinearGradientPaint} with a default
     * {@code NO_CYCLE} repeating method and {@code SRGB} color space.
     *
     * @param start the gradient axis start {@code Point2D} in user space
     * @param end the gradient axis end {@code Point2D} in user space
     * @param fractions numbers ranging from 0.0 to 1.0 specifying the
     *                  distribution of colors along the gradient
     * @param colors array of colors corresponding to each fractional value
     *
     * @throws NullPointerException
     * if one of the points is null,
     * or {@code fractions} array is null,
     * or {@code colors} array is null
     * @throws IllegalArgumentException
     * if start and end points are the same points,
     * or {@code fractions.length != colors.length},
     * or {@code colors} is less than 2 in size,
     * or a {@code fractions} value is less than 0.0 or greater than 1.0,
     * or the {@code fractions} are not provided in strictly increasing order
     */
    public LinearGradientPaint(Point2D start, Point2D end,
                               float[] fractions, Color[] colors)
    {
        this(start, end,
             fractions, colors,
             CycleMethod.NO_CYCLE);
    }

    /**
     * Constructs a {@code LinearGradientPaint} with a default {@code SRGB}
     * color space.
     *
     * @param start the gradient axis start {@code Point2D} in user space
     * @param end the gradient axis end {@code Point2D} in user space
     * @param fractions numbers ranging from 0.0 to 1.0 specifying the
     *                  distribution of colors along the gradient
     * @param colors array of colors corresponding to each fractional value
     * @param cycleMethod either {@code NO_CYCLE}, {@code REFLECT},
     *                    or {@code REPEAT}
     *
     * @throws NullPointerException
     * if one of the points is null,
     * or {@code fractions} array is null,
     * or {@code colors} array is null,
     * or {@code cycleMethod} is null
     * @throws IllegalArgumentException
     * if start and end points are the same points,
     * or {@code fractions.length != colors.length},
     * or {@code colors} is less than 2 in size,
     * or a {@code fractions} value is less than 0.0 or greater than 1.0,
     * or the {@code fractions} are not provided in strictly increasing order
     */
    public LinearGradientPaint(Point2D start, Point2D end,
                               float[] fractions, Color[] colors,
                               CycleMethod cycleMethod)
    {
        this(start, end,
             fractions, colors,
             cycleMethod,
             ColorSpaceType.SRGB,
             new AffineTransform());
    }

    /**
     * Constructs a {@code LinearGradientPaint}.
     *
     * @param start the gradient axis start {@code Point2D} in user space
     * @param end the gradient axis end {@code Point2D} in user space
     * @param fractions numbers ranging from 0.0 to 1.0 specifying the
     *                  distribution of colors along the gradient
     * @param colors array of colors corresponding to each fractional value
     * @param cycleMethod either {@code NO_CYCLE}, {@code REFLECT},
     *                    or {@code REPEAT}
     * @param colorSpace which color space to use for interpolation,
     *                   either {@code SRGB} or {@code LINEAR_RGB}
     * @param gradientTransform transform to apply to the gradient
     *
     * @throws NullPointerException
     * if one of the points is null,
     * or {@code fractions} array is null,
     * or {@code colors} array is null,
     * or {@code cycleMethod} is null,
     * or {@code colorSpace} is null,
     * or {@code gradientTransform} is null
     * @throws IllegalArgumentException
     * if start and end points are the same points,
     * or {@code fractions.length != colors.length},
     * or {@code colors} is less than 2 in size,
     * or a {@code fractions} value is less than 0.0 or greater than 1.0,
     * or the {@code fractions} are not provided in strictly increasing order
     */
    @ConstructorProperties({ "startPoint", "endPoint", "fractions", "colors", "cycleMethod", "colorSpace", "transform" })
    public LinearGradientPaint(Point2D start, Point2D end,
                               float[] fractions, Color[] colors,
                               CycleMethod cycleMethod,
                               ColorSpaceType colorSpace,
                               AffineTransform gradientTransform)
    {
        super(fractions, colors, cycleMethod, colorSpace, gradientTransform);

        // check input parameters
        if (start == null || end == null) {
            throw new NullPointerException("Start and end points must be" +
                                           "non-null");
        }

        if (start.equals(end)) {
            throw new IllegalArgumentException("Start point cannot equal" +
                                               "endpoint");
        }

        // copy the points...
        this.start = new Point2D.Double(start.getX(), start.getY());
        this.end = new Point2D.Double(end.getX(), end.getY());
    }

    /**
     * Creates and returns a {@link PaintContext} used to
     * generate a linear color gradient pattern.
     * See the {@link Paint#createContext specification} of the
     * method in the {@link Paint} interface for information
     * on null parameter handling.
     *
     * @param cm the preferred {@link ColorModel} which represents the most convenient
     *           format for the caller to receive the pixel data, or {@code null}
     *           if there is no preference.
     * @param deviceBounds the device space bounding box
     *                     of the graphics primitive being rendered.
     * @param userBounds the user space bounding box
     *                   of the graphics primitive being rendered.
     * @param transform the {@link AffineTransform} from user
     *              space into device space.
     * @param hints the set of hints that the context object can use to
     *              choose between rendering alternatives.
     * @return the {@code PaintContext} for
     *         generating color patterns.
     * @see Paint
     * @see PaintContext
     * @see ColorModel
     * @see Rectangle
     * @see Rectangle2D
     * @see AffineTransform
     * @see RenderingHints
     */
    public PaintContext createContext(ColorModel cm,
                                      Rectangle deviceBounds,
                                      Rectangle2D userBounds,
                                      AffineTransform transform,
                                      RenderingHints hints)
    {
        // avoid modifying the user's transform...
        transform = new AffineTransform(transform);
        // incorporate the gradient transform
        transform.concatenate(gradientTransform);

        if ((fractions.length == 2) &&
            (cycleMethod != CycleMethod.REPEAT) &&
            (colorSpace == ColorSpaceType.SRGB))
        {
            // faster to use the basic GradientPaintContext for this
            // common case
            boolean cyclic = (cycleMethod != CycleMethod.NO_CYCLE);
            return new GradientPaintContext(cm, start, end,
                                            transform,
                                            colors[0], colors[1],
                                            cyclic);
        } else {
            return new LinearGradientPaintContext(this, cm,
                                                  deviceBounds, userBounds,
                                                  transform, hints,
                                                  start, end,
                                                  fractions, colors,
                                                  cycleMethod, colorSpace);
        }
    }

    /**
     * Returns a copy of the start point of the gradient axis.
     *
     * @return a {@code Point2D} object that is a copy of the point
     * that anchors the first color of this {@code LinearGradientPaint}
     */
    public Point2D getStartPoint() {
        return new Point2D.Double(start.getX(), start.getY());
    }

    /**
     * Returns a copy of the end point of the gradient axis.
     *
     * @return a {@code Point2D} object that is a copy of the point
     * that anchors the last color of this {@code LinearGradientPaint}
     */
    public Point2D getEndPoint() {
        return new Point2D.Double(end.getX(), end.getY());
    }
}
