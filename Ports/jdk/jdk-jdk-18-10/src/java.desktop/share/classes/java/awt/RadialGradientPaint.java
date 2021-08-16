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
 * The {@code RadialGradientPaint} class provides a way to fill a shape with
 * a circular radial color gradient pattern. The user may specify 2 or more
 * gradient colors, and this paint will provide an interpolation between
 * each color.
 * <p>
 * The user must specify the circle controlling the gradient pattern,
 * which is described by a center point and a radius.  The user can also
 * specify a separate focus point within that circle, which controls the
 * location of the first color of the gradient.  By default the focus is
 * set to be the center of the circle.
 * <p>
 * This paint will map the first color of the gradient to the focus point,
 * and the last color to the perimeter of the circle, interpolating
 * smoothly for any in-between colors specified by the user.  Any line drawn
 * from the focus point to the circumference will thus span all the gradient
 * colors.
 * <p>
 * Specifying a focus point outside of the radius of the circle will cause
 * the rings of the gradient pattern to be centered on the point just inside
 * the edge of the circle in the direction of the focus point.
 * The rendering will internally use this modified location as if it were
 * the specified focus point.
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
 * The user may also select what action the {@code RadialGradientPaint} object
 * takes when it is filling the space outside the circle's radius by
 * setting {@code CycleMethod} to either {@code REFLECTION} or {@code REPEAT}.
 * The gradient color proportions are equal for any particular line drawn
 * from the focus point. The following figure shows that the distance AB
 * is equal to the distance BC, and the distance AD is equal to the distance DE.
 * <p style="text-align:center">
 * <img src = "doc-files/RadialGradientPaint-3.png" alt="image showing the
 * distance AB=BC, and AD=DE">
 * <p>
 * If the gradient and graphics rendering transforms are uniformly scaled and
 * the user sets the focus so that it coincides with the center of the circle,
 * the gradient color proportions are equal for any line drawn from the center.
 * The following figure shows the distances AB, BC, AD, and DE. They are all equal.
 * <p style="text-align:center">
 * <img src = "doc-files/RadialGradientPaint-4.png" alt="image showing the
 * distance of AB, BC, AD, and DE are all equal">
 * <p>
 * Note that some minor variations in distances may occur due to sampling at
 * the granularity of a pixel.
 * If no cycle method is specified, {@code NO_CYCLE} will be chosen by
 * default, which means the last keyframe color will be used to fill the
 * remaining area.
 * <p>
 * The colorSpace parameter allows the user to specify in which colorspace
 * the interpolation should be performed, default sRGB or linearized RGB.
 *
 * <p>
 * The following code demonstrates typical usage of
 * {@code RadialGradientPaint}, where the center and focus points are
 * the same:
 * <pre>
 *     Point2D center = new Point2D.Float(50, 50);
 *     float radius = 25;
 *     float[] dist = {0.0f, 0.2f, 1.0f};
 *     Color[] colors = {Color.RED, Color.WHITE, Color.BLUE};
 *     RadialGradientPaint p =
 *         new RadialGradientPaint(center, radius, dist, colors);
 * </pre>
 *
 * <p>
 * This image demonstrates the example code above, with default
 * (centered) focus for each of the three cycle methods:
 * <p style="text-align:center">
 * <img src = "doc-files/RadialGradientPaint-1.png" alt="image showing the
 * output of the sameple code">
 * <p>
 * It is also possible to specify a non-centered focus point, as
 * in the following code:
 * <pre>
 *     Point2D center = new Point2D.Float(50, 50);
 *     float radius = 25;
 *     Point2D focus = new Point2D.Float(40, 40);
 *     float[] dist = {0.0f, 0.2f, 1.0f};
 *     Color[] colors = {Color.RED, Color.WHITE, Color.BLUE};
 *     RadialGradientPaint p =
 *         new RadialGradientPaint(center, radius, focus,
 *                                 dist, colors,
 *                                 CycleMethod.NO_CYCLE);
 * </pre>
 *
 * <p>
 * This image demonstrates the previous example code, with non-centered
 * focus for each of the three cycle methods:
 * <p style="text-align:center">
 * <img src = "doc-files/RadialGradientPaint-2.png" alt="image showing the
 * output of the sample code">
 *
 * @see java.awt.Paint
 * @see java.awt.Graphics2D#setPaint
 * @author Nicholas Talian, Vincent Hardy, Jim Graham, Jerry Evans
 * @since 1.6
 */
public final class RadialGradientPaint extends MultipleGradientPaint {

    /** Focus point which defines the 0% gradient stop X coordinate. */
    private final Point2D focus;

    /** Center of the circle defining the 100% gradient stop X coordinate. */
    private final Point2D center;

    /** Radius of the outermost circle defining the 100% gradient stop. */
    private final float radius;

    /**
     * Constructs a {@code RadialGradientPaint} with a default
     * {@code NO_CYCLE} repeating method and {@code SRGB} color space,
     * using the center as the focus point.
     *
     * @param cx the X coordinate in user space of the center point of the
     *           circle defining the gradient.  The last color of the
     *           gradient is mapped to the perimeter of this circle.
     * @param cy the Y coordinate in user space of the center point of the
     *           circle defining the gradient.  The last color of the
     *           gradient is mapped to the perimeter of this circle.
     * @param radius the radius of the circle defining the extents of the
     *               color gradient
     * @param fractions numbers ranging from 0.0 to 1.0 specifying the
     *                  distribution of colors along the gradient
     * @param colors array of colors to use in the gradient.  The first color
     *               is used at the focus point, the last color around the
     *               perimeter of the circle.
     *
     * @throws NullPointerException
     * if {@code fractions} array is null,
     * or {@code colors} array is null
     * @throws IllegalArgumentException
     * if {@code radius} is non-positive,
     * or {@code fractions.length != colors.length},
     * or {@code colors} is less than 2 in size,
     * or a {@code fractions} value is less than 0.0 or greater than 1.0,
     * or the {@code fractions} are not provided in strictly increasing order
     */
    public RadialGradientPaint(float cx, float cy, float radius,
                               float[] fractions, Color[] colors)
    {
        this(cx, cy,
             radius,
             cx, cy,
             fractions,
             colors,
             CycleMethod.NO_CYCLE);
    }

    /**
     * Constructs a {@code RadialGradientPaint} with a default
     * {@code NO_CYCLE} repeating method and {@code SRGB} color space,
     * using the center as the focus point.
     *
     * @param center the center point, in user space, of the circle defining
     *               the gradient
     * @param radius the radius of the circle defining the extents of the
     *               color gradient
     * @param fractions numbers ranging from 0.0 to 1.0 specifying the
     *                  distribution of colors along the gradient
     * @param colors array of colors to use in the gradient.  The first color
     *               is used at the focus point, the last color around the
     *               perimeter of the circle.
     *
     * @throws NullPointerException
     * if {@code center} point is null,
     * or {@code fractions} array is null,
     * or {@code colors} array is null
     * @throws IllegalArgumentException
     * if {@code radius} is non-positive,
     * or {@code fractions.length != colors.length},
     * or {@code colors} is less than 2 in size,
     * or a {@code fractions} value is less than 0.0 or greater than 1.0,
     * or the {@code fractions} are not provided in strictly increasing order
     */
    public RadialGradientPaint(Point2D center, float radius,
                               float[] fractions, Color[] colors)
    {
        this(center,
             radius,
             center,
             fractions,
             colors,
             CycleMethod.NO_CYCLE);
    }

    /**
     * Constructs a {@code RadialGradientPaint} with a default
     * {@code SRGB} color space, using the center as the focus point.
     *
     * @param cx the X coordinate in user space of the center point of the
     *           circle defining the gradient.  The last color of the
     *           gradient is mapped to the perimeter of this circle.
     * @param cy the Y coordinate in user space of the center point of the
     *           circle defining the gradient.  The last color of the
     *           gradient is mapped to the perimeter of this circle.
     * @param radius the radius of the circle defining the extents of the
     *               color gradient
     * @param fractions numbers ranging from 0.0 to 1.0 specifying the
     *                  distribution of colors along the gradient
     * @param colors array of colors to use in the gradient.  The first color
     *               is used at the focus point, the last color around the
     *               perimeter of the circle.
     * @param cycleMethod either {@code NO_CYCLE}, {@code REFLECT},
     *                    or {@code REPEAT}
     *
     * @throws NullPointerException
     * if {@code fractions} array is null,
     * or {@code colors} array is null,
     * or {@code cycleMethod} is null
     * @throws IllegalArgumentException
     * if {@code radius} is non-positive,
     * or {@code fractions.length != colors.length},
     * or {@code colors} is less than 2 in size,
     * or a {@code fractions} value is less than 0.0 or greater than 1.0,
     * or the {@code fractions} are not provided in strictly increasing order
     */
    public RadialGradientPaint(float cx, float cy, float radius,
                               float[] fractions, Color[] colors,
                               CycleMethod cycleMethod)
    {
        this(cx, cy,
             radius,
             cx, cy,
             fractions,
             colors,
             cycleMethod);
    }

    /**
     * Constructs a {@code RadialGradientPaint} with a default
     * {@code SRGB} color space, using the center as the focus point.
     *
     * @param center the center point, in user space, of the circle defining
     *               the gradient
     * @param radius the radius of the circle defining the extents of the
     *               color gradient
     * @param fractions numbers ranging from 0.0 to 1.0 specifying the
     *                  distribution of colors along the gradient
     * @param colors array of colors to use in the gradient.  The first color
     *               is used at the focus point, the last color around the
     *               perimeter of the circle.
     * @param cycleMethod either {@code NO_CYCLE}, {@code REFLECT},
     *                    or {@code REPEAT}
     *
     * @throws NullPointerException
     * if {@code center} point is null,
     * or {@code fractions} array is null,
     * or {@code colors} array is null,
     * or {@code cycleMethod} is null
     * @throws IllegalArgumentException
     * if {@code radius} is non-positive,
     * or {@code fractions.length != colors.length},
     * or {@code colors} is less than 2 in size,
     * or a {@code fractions} value is less than 0.0 or greater than 1.0,
     * or the {@code fractions} are not provided in strictly increasing order
     */
    public RadialGradientPaint(Point2D center, float radius,
                               float[] fractions, Color[] colors,
                               CycleMethod cycleMethod)
    {
        this(center,
             radius,
             center,
             fractions,
             colors,
             cycleMethod);
    }

    /**
     * Constructs a {@code RadialGradientPaint} with a default
     * {@code SRGB} color space.
     *
     * @param cx the X coordinate in user space of the center point of the
     *           circle defining the gradient.  The last color of the
     *           gradient is mapped to the perimeter of this circle.
     * @param cy the Y coordinate in user space of the center point of the
     *           circle defining the gradient.  The last color of the
     *           gradient is mapped to the perimeter of this circle.
     * @param radius the radius of the circle defining the extents of the
     *               color gradient
     * @param fx the X coordinate of the point in user space to which the
     *           first color is mapped
     * @param fy the Y coordinate of the point in user space to which the
     *           first color is mapped
     * @param fractions numbers ranging from 0.0 to 1.0 specifying the
     *                  distribution of colors along the gradient
     * @param colors array of colors to use in the gradient.  The first color
     *               is used at the focus point, the last color around the
     *               perimeter of the circle.
     * @param cycleMethod either {@code NO_CYCLE}, {@code REFLECT},
     *                    or {@code REPEAT}
     *
     * @throws NullPointerException
     * if {@code fractions} array is null,
     * or {@code colors} array is null,
     * or {@code cycleMethod} is null
     * @throws IllegalArgumentException
     * if {@code radius} is non-positive,
     * or {@code fractions.length != colors.length},
     * or {@code colors} is less than 2 in size,
     * or a {@code fractions} value is less than 0.0 or greater than 1.0,
     * or the {@code fractions} are not provided in strictly increasing order
     */
    public RadialGradientPaint(float cx, float cy, float radius,
                               float fx, float fy,
                               float[] fractions, Color[] colors,
                               CycleMethod cycleMethod)
    {
        this(new Point2D.Float(cx, cy),
             radius,
             new Point2D.Float(fx, fy),
             fractions,
             colors,
             cycleMethod);
    }

    /**
     * Constructs a {@code RadialGradientPaint} with a default
     * {@code SRGB} color space.
     *
     * @param center the center point, in user space, of the circle defining
     *               the gradient.  The last color of the gradient is mapped
     *               to the perimeter of this circle.
     * @param radius the radius of the circle defining the extents of the color
     *               gradient
     * @param focus the point in user space to which the first color is mapped
     * @param fractions numbers ranging from 0.0 to 1.0 specifying the
     *                  distribution of colors along the gradient
     * @param colors array of colors to use in the gradient. The first color
     *               is used at the focus point, the last color around the
     *               perimeter of the circle.
     * @param cycleMethod either {@code NO_CYCLE}, {@code REFLECT},
     *                    or {@code REPEAT}
     *
     * @throws NullPointerException
     * if one of the points is null,
     * or {@code fractions} array is null,
     * or {@code colors} array is null,
     * or {@code cycleMethod} is null
     * @throws IllegalArgumentException
     * if {@code radius} is non-positive,
     * or {@code fractions.length != colors.length},
     * or {@code colors} is less than 2 in size,
     * or a {@code fractions} value is less than 0.0 or greater than 1.0,
     * or the {@code fractions} are not provided in strictly increasing order
     */
    public RadialGradientPaint(Point2D center, float radius,
                               Point2D focus,
                               float[] fractions, Color[] colors,
                               CycleMethod cycleMethod)
    {
        this(center,
             radius,
             focus,
             fractions,
             colors,
             cycleMethod,
             ColorSpaceType.SRGB,
             new AffineTransform());
    }

    /**
     * Constructs a {@code RadialGradientPaint}.
     *
     * @param center the center point in user space of the circle defining the
     *               gradient.  The last color of the gradient is mapped to
     *               the perimeter of this circle.
     * @param radius the radius of the circle defining the extents of the
     *               color gradient
     * @param focus the point in user space to which the first color is mapped
     * @param fractions numbers ranging from 0.0 to 1.0 specifying the
     *                  distribution of colors along the gradient
     * @param colors array of colors to use in the gradient.  The first color
     *               is used at the focus point, the last color around the
     *               perimeter of the circle.
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
     * if {@code radius} is non-positive,
     * or {@code fractions.length != colors.length},
     * or {@code colors} is less than 2 in size,
     * or a {@code fractions} value is less than 0.0 or greater than 1.0,
     * or the {@code fractions} are not provided in strictly increasing order
     */
    @ConstructorProperties({ "centerPoint", "radius", "focusPoint", "fractions", "colors", "cycleMethod", "colorSpace", "transform" })
    public RadialGradientPaint(Point2D center,
                               float radius,
                               Point2D focus,
                               float[] fractions, Color[] colors,
                               CycleMethod cycleMethod,
                               ColorSpaceType colorSpace,
                               AffineTransform gradientTransform)
    {
        super(fractions, colors, cycleMethod, colorSpace, gradientTransform);

        // check input arguments
        if (center == null) {
            throw new NullPointerException("Center point must be non-null");
        }

        if (focus == null) {
            throw new NullPointerException("Focus point must be non-null");
        }

        if (radius <= 0) {
            throw new IllegalArgumentException("Radius must be greater " +
                                               "than zero");
        }

        // copy parameters
        this.center = new Point2D.Double(center.getX(), center.getY());
        this.focus = new Point2D.Double(focus.getX(), focus.getY());
        this.radius = radius;
    }

    /**
     * Constructs a {@code RadialGradientPaint} with a default
     * {@code SRGB} color space.
     * The gradient circle of the {@code RadialGradientPaint} is defined
     * by the given bounding box.
     * <p>
     * This constructor is a more convenient way to express the
     * following (equivalent) code:<br>
     *
     * <pre>
     *     double gw = gradientBounds.getWidth();
     *     double gh = gradientBounds.getHeight();
     *     double cx = gradientBounds.getCenterX();
     *     double cy = gradientBounds.getCenterY();
     *     Point2D center = new Point2D.Double(cx, cy);
     *
     *     AffineTransform gradientTransform = new AffineTransform();
     *     gradientTransform.translate(cx, cy);
     *     gradientTransform.scale(gw / 2, gh / 2);
     *     gradientTransform.translate(-cx, -cy);
     *
     *     RadialGradientPaint gp =
     *         new RadialGradientPaint(center, 1.0f, center,
     *                                 fractions, colors,
     *                                 cycleMethod,
     *                                 ColorSpaceType.SRGB,
     *                                 gradientTransform);
     * </pre>
     *
     * @param gradientBounds the bounding box, in user space, of the circle
     *                       defining the outermost extent of the gradient
     * @param fractions numbers ranging from 0.0 to 1.0 specifying the
     *                  distribution of colors along the gradient
     * @param colors array of colors to use in the gradient.  The first color
     *               is used at the focus point, the last color around the
     *               perimeter of the circle.
     * @param cycleMethod either {@code NO_CYCLE}, {@code REFLECT},
     *                    or {@code REPEAT}
     *
     * @throws NullPointerException
     * if {@code gradientBounds} is null,
     * or {@code fractions} array is null,
     * or {@code colors} array is null,
     * or {@code cycleMethod} is null
     * @throws IllegalArgumentException
     * if {@code gradientBounds} is empty,
     * or {@code fractions.length != colors.length},
     * or {@code colors} is less than 2 in size,
     * or a {@code fractions} value is less than 0.0 or greater than 1.0,
     * or the {@code fractions} are not provided in strictly increasing order
     */
    public RadialGradientPaint(Rectangle2D gradientBounds,
                               float[] fractions, Color[] colors,
                               CycleMethod cycleMethod)
    {
        // gradient center/focal point is the center of the bounding box,
        // radius is set to 1.0, and then we set a scale transform
        // to achieve an elliptical gradient defined by the bounding box
        this(new Point2D.Double(gradientBounds.getCenterX(),
                                gradientBounds.getCenterY()),
             1.0f,
             new Point2D.Double(gradientBounds.getCenterX(),
                                gradientBounds.getCenterY()),
             fractions,
             colors,
             cycleMethod,
             ColorSpaceType.SRGB,
             createGradientTransform(gradientBounds));

        if (gradientBounds.isEmpty()) {
            throw new IllegalArgumentException("Gradient bounds must be " +
                                               "non-empty");
        }
    }

    private static AffineTransform createGradientTransform(Rectangle2D r) {
        double cx = r.getCenterX();
        double cy = r.getCenterY();
        AffineTransform xform = AffineTransform.getTranslateInstance(cx, cy);
        xform.scale(r.getWidth()/2, r.getHeight()/2);
        xform.translate(-cx, -cy);
        return xform;
    }

    /**
     * Creates and returns a {@link PaintContext} used to
     * generate a circular radial color gradient pattern.
     * See the description of the {@link Paint#createContext createContext} method
     * for information on null parameter handling.
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

        return new RadialGradientPaintContext(this, cm,
                                              deviceBounds, userBounds,
                                              transform, hints,
                                              (float)center.getX(),
                                              (float)center.getY(),
                                              radius,
                                              (float)focus.getX(),
                                              (float)focus.getY(),
                                              fractions, colors,
                                              cycleMethod, colorSpace);
    }

    /**
     * Returns a copy of the center point of the radial gradient.
     *
     * @return a {@code Point2D} object that is a copy of the center point
     */
    public Point2D getCenterPoint() {
        return new Point2D.Double(center.getX(), center.getY());
    }

    /**
     * Returns a copy of the focus point of the radial gradient.
     * Note that if the focus point specified when the radial gradient
     * was constructed lies outside of the radius of the circle, this
     * method will still return the original focus point even though
     * the rendering may center the rings of color on a different
     * point that lies inside the radius.
     *
     * @return a {@code Point2D} object that is a copy of the focus point
     */
    public Point2D getFocusPoint() {
        return new Point2D.Double(focus.getX(), focus.getY());
    }

    /**
     * Returns the radius of the circle defining the radial gradient.
     *
     * @return the radius of the circle defining the radial gradient
     */
    public float getRadius() {
        return radius;
    }
}
