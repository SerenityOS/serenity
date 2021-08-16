/*
 * Copyright (c) 2006, 2013, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.MultipleGradientPaint.CycleMethod;
import java.awt.MultipleGradientPaint.ColorSpaceType;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.ColorModel;

/**
 * Provides the actual implementation for the LinearGradientPaint.
 * This is where the pixel processing is done.
 *
 * @see java.awt.LinearGradientPaint
 * @see java.awt.PaintContext
 * @see java.awt.Paint
 * @author Nicholas Talian, Vincent Hardy, Jim Graham, Jerry Evans
 */
final class LinearGradientPaintContext extends MultipleGradientPaintContext {

    /**
     * The following invariants are used to process the gradient value from
     * a device space coordinate, (X, Y):
     *     g(X, Y) = dgdX*X + dgdY*Y + gc
     */
    private float dgdX, dgdY, gc;

    /**
     * Constructor for LinearGradientPaintContext.
     *
     * @param paint the {@code LinearGradientPaint} from which this context
     *              is created
     * @param cm {@code ColorModel} that receives
     *           the {@code Paint} data. This is used only as a hint.
     * @param deviceBounds the device space bounding box of the
     *                     graphics primitive being rendered
     * @param userBounds the user space bounding box of the
     *                   graphics primitive being rendered
     * @param t the {@code AffineTransform} from user
     *          space into device space (gradientTransform should be
     *          concatenated with this)
     * @param hints the hints that the context object uses to choose
     *              between rendering alternatives
     * @param start gradient start point, in user space
     * @param end gradient end point, in user space
     * @param fractions the fractions specifying the gradient distribution
     * @param colors the gradient colors
     * @param cycleMethod either NO_CYCLE, REFLECT, or REPEAT
     * @param colorSpace which colorspace to use for interpolation,
     *                   either SRGB or LINEAR_RGB
     */
    LinearGradientPaintContext(LinearGradientPaint paint,
                               ColorModel cm,
                               Rectangle deviceBounds,
                               Rectangle2D userBounds,
                               AffineTransform t,
                               RenderingHints hints,
                               Point2D start,
                               Point2D end,
                               float[] fractions,
                               Color[] colors,
                               CycleMethod cycleMethod,
                               ColorSpaceType colorSpace)
    {
        super(paint, cm, deviceBounds, userBounds, t, hints, fractions,
              colors, cycleMethod, colorSpace);

        // A given point in the raster should take on the same color as its
        // projection onto the gradient vector.
        // Thus, we want the projection of the current position vector
        // onto the gradient vector, then normalized with respect to the
        // length of the gradient vector, giving a value which can be mapped
        // into the range 0-1.
        //    projection =
        //        currentVector dot gradientVector / length(gradientVector)
        //    normalized = projection / length(gradientVector)

        float startx = (float)start.getX();
        float starty = (float)start.getY();
        float endx = (float)end.getX();
        float endy = (float)end.getY();

        float dx = endx - startx;  // change in x from start to end
        float dy = endy - starty;  // change in y from start to end
        float dSq = dx*dx + dy*dy; // total distance squared

        // avoid repeated calculations by doing these divides once
        float constX = dx/dSq;
        float constY = dy/dSq;

        // incremental change along gradient for +x
        dgdX = a00*constX + a10*constY;
        // incremental change along gradient for +y
        dgdY = a01*constX + a11*constY;

        // constant, incorporates the translation components from the matrix
        gc = (a02-startx)*constX + (a12-starty)*constY;
    }

    /**
     * Return a Raster containing the colors generated for the graphics
     * operation.  This is where the area is filled with colors distributed
     * linearly.
     *
     * @param x,y,w,h the area in device space for which colors are
     * generated.
     */
    protected void fillRaster(int[] pixels, int off, int adjust,
                              int x, int y, int w, int h)
    {
        // current value for row gradients
        float g = 0;

        // used to end iteration on rows
        int rowLimit = off + w;

        // constant which can be pulled out of the inner loop
        float initConst = (dgdX*x) + gc;

        for (int i = 0; i < h; i++) { // for every row

            // initialize current value to be start
            g = initConst + dgdY*(y+i);

            while (off < rowLimit) { // for every pixel in this row
                // get the color
                pixels[off++] = indexIntoGradientsArrays(g);

                // incremental change in g
                g += dgdX;
            }

            // change in off from row to row
            off += adjust;

            //rowlimit is width + offset
            rowLimit = off + w;
        }
    }
}
