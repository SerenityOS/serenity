/*
 * Copyright (c) 2007, 2013, Oracle and/or its affiliates. All rights reserved.
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

package sun.java2d.pipe;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.LinearGradientPaint;
import java.awt.MultipleGradientPaint;
import java.awt.MultipleGradientPaint.ColorSpaceType;
import java.awt.MultipleGradientPaint.CycleMethod;
import java.awt.Paint;
import java.awt.RadialGradientPaint;
import java.awt.TexturePaint;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import sun.awt.image.PixelConverter;
import sun.java2d.SunGraphics2D;
import sun.java2d.SurfaceData;
import sun.java2d.loops.CompositeType;
import sun.java2d.loops.SurfaceType;
import static sun.java2d.pipe.BufferedOpCodes.*;

import java.lang.annotation.Native;

public class BufferedPaints {

    static void setPaint(RenderQueue rq, SunGraphics2D sg2d,
                         Paint paint, int ctxflags)
    {
        if (sg2d.paintState <= SunGraphics2D.PAINT_ALPHACOLOR) {
            setColor(rq, sg2d.pixel);
        } else {
            boolean useMask = (ctxflags & BufferedContext.USE_MASK) != 0;
            switch (sg2d.paintState) {
            case SunGraphics2D.PAINT_GRADIENT:
                setGradientPaint(rq, sg2d,
                                 (GradientPaint)paint, useMask);
                break;
            case SunGraphics2D.PAINT_LIN_GRADIENT:
                setLinearGradientPaint(rq, sg2d,
                                       (LinearGradientPaint)paint, useMask);
                break;
            case SunGraphics2D.PAINT_RAD_GRADIENT:
                setRadialGradientPaint(rq, sg2d,
                                       (RadialGradientPaint)paint, useMask);
                break;
            case SunGraphics2D.PAINT_TEXTURE:
                setTexturePaint(rq, sg2d,
                                (TexturePaint)paint, useMask);
                break;
            default:
                break;
            }
        }
    }

    static void resetPaint(RenderQueue rq) {
        // assert rq.lock.isHeldByCurrentThread();
        rq.ensureCapacity(4);
        RenderBuffer buf = rq.getBuffer();
        buf.putInt(RESET_PAINT);
    }

/****************************** Color support *******************************/

    private static void setColor(RenderQueue rq, int pixel) {
        // assert rq.lock.isHeldByCurrentThread();
        rq.ensureCapacity(8);
        RenderBuffer buf = rq.getBuffer();
        buf.putInt(SET_COLOR);
        buf.putInt(pixel);
    }

/************************* GradientPaint support ****************************/

    /**
     * Note: This code is factored out into a separate static method
     * so that it can be shared by both the Gradient and LinearGradient
     * implementations.  LinearGradient uses this code (for the
     * two-color sRGB case only) because it can be much faster than the
     * equivalent implementation that uses fragment shaders.
     *
     * We use OpenGL's texture coordinate generator to automatically
     * apply a smooth gradient (either cyclic or acyclic) to the geometry
     * being rendered.  This technique is almost identical to the one
     * described in the comments for BufferedPaints.setTexturePaint(),
     * except the calculations take place in one dimension instead of two.
     * Instead of an anchor rectangle in the TexturePaint case, we use
     * the vector between the two GradientPaint end points in our
     * calculations.  The generator uses a single plane equation that
     * takes the (x,y) location (in device space) of the fragment being
     * rendered to calculate a (u) texture coordinate for that fragment:
     *     u = Ax + By + Cz + Dw
     *
     * The gradient renderer uses a two-pixel 1D texture where the first
     * pixel contains the first GradientPaint color, and the second pixel
     * contains the second GradientPaint color.  (Note that we use the
     * GL_CLAMP_TO_EDGE wrapping mode for acyclic gradients so that we
     * clamp the colors properly at the extremes.)  The following diagram
     * attempts to show the layout of the texture containing the two
     * GradientPaint colors (C1 and C2):
     *
     *                        +-----------------+
     *                        |   C1   |   C2   |
     *                        |        |        |
     *                        +-----------------+
     *                      u=0  .25  .5   .75  1
     *
     * We calculate our plane equation constants (A,B,D) such that u=0.25
     * corresponds to the first GradientPaint end point in user space and
     * u=0.75 corresponds to the second end point.  This is somewhat
     * non-obvious, but since the gradient colors are generated by
     * interpolating between C1 and C2, we want the pure color at the
     * end points, and we will get the pure color only when u correlates
     * to the center of a texel.  The following chart shows the expected
     * color for some sample values of u (where C' is the color halfway
     * between C1 and C2):
     *
     *       u value      acyclic (GL_CLAMP)      cyclic (GL_REPEAT)
     *       -------      ------------------      ------------------
     *        -0.25              C1                       C2
     *         0.0               C1                       C'
     *         0.25              C1                       C1
     *         0.5               C'                       C'
     *         0.75              C2                       C2
     *         1.0               C2                       C'
     *         1.25              C2                       C1
     *
     * Original inspiration for this technique came from UMD's Agile2D
     * project (GradientManager.java).
     */
    private static void setGradientPaint(RenderQueue rq, AffineTransform at,
                                         Color c1, Color c2,
                                         Point2D pt1, Point2D pt2,
                                         boolean isCyclic, boolean useMask)
    {
        // convert gradient colors to IntArgbPre format
        PixelConverter pc = PixelConverter.ArgbPre.instance;
        int pixel1 = pc.rgbToPixel(c1.getRGB(), null);
        int pixel2 = pc.rgbToPixel(c2.getRGB(), null);

        // calculate plane equation constants
        double x = pt1.getX();
        double y = pt1.getY();
        at.translate(x, y);
        // now gradient point 1 is at the origin
        x = pt2.getX() - x;
        y = pt2.getY() - y;
        double len = Math.sqrt(x * x + y * y);
        at.rotate(x, y);
        // now gradient point 2 is on the positive x-axis
        at.scale(2*len, 1);
        // now gradient point 2 is at (0.5, 0)
        at.translate(-0.25, 0);
        // now gradient point 1 is at (0.25, 0), point 2 is at (0.75, 0)

        double p0, p1, p3;
        try {
            at.invert();
            p0 = at.getScaleX();
            p1 = at.getShearX();
            p3 = at.getTranslateX();
        } catch (java.awt.geom.NoninvertibleTransformException e) {
            p0 = p1 = p3 = 0.0;
        }

        // assert rq.lock.isHeldByCurrentThread();
        rq.ensureCapacityAndAlignment(44, 12);
        RenderBuffer buf = rq.getBuffer();
        buf.putInt(SET_GRADIENT_PAINT);
        buf.putInt(useMask ? 1 : 0);
        buf.putInt(isCyclic ? 1 : 0);
        buf.putDouble(p0).putDouble(p1).putDouble(p3);
        buf.putInt(pixel1).putInt(pixel2);
    }

    private static void setGradientPaint(RenderQueue rq,
                                         SunGraphics2D sg2d,
                                         GradientPaint paint,
                                         boolean useMask)
    {
        setGradientPaint(rq, (AffineTransform)sg2d.transform.clone(),
                         paint.getColor1(), paint.getColor2(),
                         paint.getPoint1(), paint.getPoint2(),
                         paint.isCyclic(), useMask);
    }

/************************** TexturePaint support ****************************/

    /**
     * We use OpenGL's texture coordinate generator to automatically
     * map the TexturePaint image to the geometry being rendered.  The
     * generator uses two separate plane equations that take the (x,y)
     * location (in device space) of the fragment being rendered to
     * calculate (u,v) texture coordinates for that fragment:
     *     u = Ax + By + Cz + Dw
     *     v = Ex + Fy + Gz + Hw
     *
     * Since we use a 2D orthographic projection, we can assume that z=0
     * and w=1 for any fragment.  So we need to calculate appropriate
     * values for the plane equation constants (A,B,D) and (E,F,H) such
     * that {u,v}=0 for the top-left of the TexturePaint's anchor
     * rectangle and {u,v}=1 for the bottom-right of the anchor rectangle.
     * We can easily make the texture image repeat for {u,v} values
     * outside the range [0,1] by specifying the GL_REPEAT texture wrap
     * mode.
     *
     * Calculating the plane equation constants is surprisingly simple.
     * We can think of it as an inverse matrix operation that takes
     * device space coordinates and transforms them into user space
     * coordinates that correspond to a location relative to the anchor
     * rectangle.  First, we translate and scale the current user space
     * transform by applying the anchor rectangle bounds.  We then take
     * the inverse of this affine transform.  The rows of the resulting
     * inverse matrix correlate nicely to the plane equation constants
     * we were seeking.
     */
    private static void setTexturePaint(RenderQueue rq,
                                        SunGraphics2D sg2d,
                                        TexturePaint paint,
                                        boolean useMask)
    {
        BufferedImage bi = paint.getImage();
        SurfaceData dstData = sg2d.surfaceData;
        SurfaceData srcData =
            dstData.getSourceSurfaceData(bi, SunGraphics2D.TRANSFORM_ISIDENT,
                                         CompositeType.SrcOver, null);
        boolean filter =
            (sg2d.interpolationType !=
             AffineTransformOp.TYPE_NEAREST_NEIGHBOR);

        // calculate plane equation constants
        AffineTransform at = (AffineTransform)sg2d.transform.clone();
        Rectangle2D anchor = paint.getAnchorRect();
        at.translate(anchor.getX(), anchor.getY());
        at.scale(anchor.getWidth(), anchor.getHeight());

        double xp0, xp1, xp3, yp0, yp1, yp3;
        try {
            at.invert();
            xp0 = at.getScaleX();
            xp1 = at.getShearX();
            xp3 = at.getTranslateX();
            yp0 = at.getShearY();
            yp1 = at.getScaleY();
            yp3 = at.getTranslateY();
        } catch (java.awt.geom.NoninvertibleTransformException e) {
            xp0 = xp1 = xp3 = yp0 = yp1 = yp3 = 0.0;
        }

        // assert rq.lock.isHeldByCurrentThread();
        rq.ensureCapacityAndAlignment(68, 12);
        RenderBuffer buf = rq.getBuffer();
        buf.putInt(SET_TEXTURE_PAINT);
        buf.putInt(useMask ? 1 : 0);
        buf.putInt(filter ? 1 : 0);
        buf.putLong(srcData.getNativeOps());
        buf.putDouble(xp0).putDouble(xp1).putDouble(xp3);
        buf.putDouble(yp0).putDouble(yp1).putDouble(yp3);
    }

/****************** Shared MultipleGradientPaint support ********************/

    /**
     * The maximum number of gradient "stops" supported by our native
     * fragment shader implementations.
     *
     * This value has been empirically determined and capped to allow
     * our native shaders to run on all shader-level graphics hardware,
     * even on the older, more limited GPUs.  Even the oldest Nvidia
     * hardware could handle 16, or even 32 fractions without any problem.
     * But the first-generation boards from ATI would fall back into
     * software mode (which is unusably slow) for values larger than 12;
     * it appears that those boards do not have enough native registers
     * to support the number of array accesses required by our gradient
     * shaders.  So for now we will cap this value at 12, but we can
     * re-evaluate this in the future as hardware becomes more capable.
     */
    @Native public static final int MULTI_MAX_FRACTIONS = 12;

    /**
     * Helper function to convert a color component in sRGB space to
     * linear RGB space.  Copied directly from the
     * MultipleGradientPaintContext class.
     */
    public static int convertSRGBtoLinearRGB(int color) {
        float input, output;

        input = color / 255.0f;
        if (input <= 0.04045f) {
            output = input / 12.92f;
        } else {
            output = (float)Math.pow((input + 0.055) / 1.055, 2.4);
        }

        return Math.round(output * 255.0f);
    }

    /**
     * Helper function to convert a (non-premultiplied) Color in sRGB
     * space to an IntArgbPre pixel value, optionally in linear RGB space.
     * Based on the PixelConverter.ArgbPre.rgbToPixel() method.
     */
    private static int colorToIntArgbPrePixel(Color c, boolean linear) {
        int rgb = c.getRGB();
        if (!linear && ((rgb >> 24) == -1)) {
            return rgb;
        }
        int a = rgb >>> 24;
        int r = (rgb >> 16) & 0xff;
        int g = (rgb >>  8) & 0xff;
        int b = (rgb      ) & 0xff;
        if (linear) {
            r = convertSRGBtoLinearRGB(r);
            g = convertSRGBtoLinearRGB(g);
            b = convertSRGBtoLinearRGB(b);
        }
        int a2 = a + (a >> 7);
        r = (r * a2) >> 8;
        g = (g * a2) >> 8;
        b = (b * a2) >> 8;
        return ((a << 24) | (r << 16) | (g << 8) | (b));
    }

    /**
     * Converts the given array of Color objects into an int array
     * containing IntArgbPre pixel values.  If the linear parameter
     * is true, the Color values will be converted into a linear RGB
     * color space before being returned.
     */
    private static int[] convertToIntArgbPrePixels(Color[] colors,
                                                   boolean linear)
    {
        int[] pixels = new int[colors.length];
        for (int i = 0; i < colors.length; i++) {
            pixels[i] = colorToIntArgbPrePixel(colors[i], linear);
        }
        return pixels;
    }

/********************** LinearGradientPaint support *************************/

    /**
     * This method uses techniques that are nearly identical to those
     * employed in setGradientPaint() above.  The primary difference
     * is that at the native level we use a fragment shader to manually
     * apply the plane equation constants to the current fragment position
     * to calculate the gradient position in the range [0,1] (the native
     * code for GradientPaint does the same, except that it uses OpenGL's
     * automatic texture coordinate generation facilities).
     *
     * One other minor difference worth mentioning is that
     * setGradientPaint() calculates the plane equation constants
     * such that the gradient end points are positioned at 0.25 and 0.75
     * (for reasons discussed in the comments for that method).  In
     * contrast, for LinearGradientPaint we setup the equation constants
     * such that the gradient end points fall at 0.0 and 1.0.  The
     * reason for this difference is that in the fragment shader we
     * have more control over how the gradient values are interpreted
     * (depending on the paint's CycleMethod).
     */
    private static void setLinearGradientPaint(RenderQueue rq,
                                               SunGraphics2D sg2d,
                                               LinearGradientPaint paint,
                                               boolean useMask)
    {
        boolean linear =
            (paint.getColorSpace() == ColorSpaceType.LINEAR_RGB);
        Color[] colors = paint.getColors();
        int numStops = colors.length;
        Point2D pt1 = paint.getStartPoint();
        Point2D pt2 = paint.getEndPoint();
        AffineTransform at = paint.getTransform();
        at.preConcatenate(sg2d.transform);

        if (!linear && numStops == 2 &&
            paint.getCycleMethod() != CycleMethod.REPEAT)
        {
            // delegate to the optimized two-color gradient codepath
            boolean isCyclic =
                (paint.getCycleMethod() != CycleMethod.NO_CYCLE);
            setGradientPaint(rq, at,
                             colors[0], colors[1],
                             pt1, pt2,
                             isCyclic, useMask);
            return;
        }

        int cycleMethod = paint.getCycleMethod().ordinal();
        float[] fractions = paint.getFractions();
        int[] pixels = convertToIntArgbPrePixels(colors, linear);

        // calculate plane equation constants
        double x = pt1.getX();
        double y = pt1.getY();
        at.translate(x, y);
        // now gradient point 1 is at the origin
        x = pt2.getX() - x;
        y = pt2.getY() - y;
        double len = Math.sqrt(x * x + y * y);
        at.rotate(x, y);
        // now gradient point 2 is on the positive x-axis
        at.scale(len, 1);
        // now gradient point 1 is at (0.0, 0), point 2 is at (1.0, 0)

        float p0, p1, p3;
        try {
            at.invert();
            p0 = (float)at.getScaleX();
            p1 = (float)at.getShearX();
            p3 = (float)at.getTranslateX();
        } catch (java.awt.geom.NoninvertibleTransformException e) {
            p0 = p1 = p3 = 0.0f;
        }

        // assert rq.lock.isHeldByCurrentThread();
        rq.ensureCapacity(20 + 12 + (numStops*4*2));
        RenderBuffer buf = rq.getBuffer();
        buf.putInt(SET_LINEAR_GRADIENT_PAINT);
        buf.putInt(useMask ? 1 : 0);
        buf.putInt(linear  ? 1 : 0);
        buf.putInt(cycleMethod);
        buf.putInt(numStops);
        buf.putFloat(p0);
        buf.putFloat(p1);
        buf.putFloat(p3);
        buf.put(fractions);
        buf.put(pixels);
    }

/********************** RadialGradientPaint support *************************/

    /**
     * This method calculates six m** values and a focusX value that
     * are used by the native fragment shader.  These techniques are
     * based on a whitepaper by Daniel Rice on radial gradient performance
     * (attached to the bug report for 6521533).  One can refer to that
     * document for the complete set of formulas and calculations, but
     * the basic goal is to compose a transform that will convert an
     * (x,y) position in device space into a "u" value that represents
     * the relative distance to the gradient focus point.  The resulting
     * value can be used to look up the appropriate color by linearly
     * interpolating between the two nearest colors in the gradient.
     */
    private static void setRadialGradientPaint(RenderQueue rq,
                                               SunGraphics2D sg2d,
                                               RadialGradientPaint paint,
                                               boolean useMask)
    {
        boolean linear =
            (paint.getColorSpace() == ColorSpaceType.LINEAR_RGB);
        int cycleMethod = paint.getCycleMethod().ordinal();
        float[] fractions = paint.getFractions();
        Color[] colors = paint.getColors();
        int numStops = colors.length;
        int[] pixels = convertToIntArgbPrePixels(colors, linear);
        Point2D center = paint.getCenterPoint();
        Point2D focus = paint.getFocusPoint();
        float radius = paint.getRadius();

        // save original (untransformed) center and focus points
        double cx = center.getX();
        double cy = center.getY();
        double fx = focus.getX();
        double fy = focus.getY();

        // transform from gradient coords to device coords
        AffineTransform at = paint.getTransform();
        at.preConcatenate(sg2d.transform);
        focus = at.transform(focus, focus);

        // transform unit circle to gradient coords; we start with the
        // unit circle (center=(0,0), focus on positive x-axis, radius=1)
        // and then transform into gradient space
        at.translate(cx, cy);
        at.rotate(fx - cx, fy - cy);
        at.scale(radius, radius);

        // invert to get mapping from device coords to unit circle
        try {
            at.invert();
        } catch (Exception e) {
            at.setToScale(0.0, 0.0);
        }
        focus = at.transform(focus, focus);

        // clamp the focus point so that it does not rest on, or outside
        // of, the circumference of the gradient circle
        fx = Math.min(focus.getX(), 0.99);

        // assert rq.lock.isHeldByCurrentThread();
        rq.ensureCapacity(20 + 28 + (numStops*4*2));
        RenderBuffer buf = rq.getBuffer();
        buf.putInt(SET_RADIAL_GRADIENT_PAINT);
        buf.putInt(useMask ? 1 : 0);
        buf.putInt(linear  ? 1 : 0);
        buf.putInt(numStops);
        buf.putInt(cycleMethod);
        buf.putFloat((float)at.getScaleX());
        buf.putFloat((float)at.getShearX());
        buf.putFloat((float)at.getTranslateX());
        buf.putFloat((float)at.getShearY());
        buf.putFloat((float)at.getScaleY());
        buf.putFloat((float)at.getTranslateY());
        buf.putFloat((float)fx);
        buf.put(fractions);
        buf.put(pixels);
    }
}
