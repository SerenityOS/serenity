/*
 * Copyright (c) 1997, 2006, Oracle and/or its affiliates. All rights reserved.
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

package java.awt.image;

import java.awt.geom.Rectangle2D;
import java.awt.geom.Point2D;
import java.awt.RenderingHints;

/**
 * This interface describes single-input/single-output
 * operations performed on {@code BufferedImage} objects.
 * It is implemented by {@code AffineTransformOp},
 * {@code ConvolveOp}, {@code ColorConvertOp}, {@code RescaleOp},
 * and {@code LookupOp}.  These objects can be passed into
 * a {@code BufferedImageFilter} to operate on a
 * {@code BufferedImage} in the
 * ImageProducer-ImageFilter-ImageConsumer paradigm.
 * <p>
 * Classes that implement this
 * interface must specify whether or not they allow in-place filtering--
 * filter operations where the source object is equal to the destination
 * object.
 * <p>
 * This interface cannot be used to describe more sophisticated operations
 * such as those that take multiple sources. Note that this restriction also
 * means that the values of the destination pixels prior to the operation are
 * not used as input to the filter operation.

 * @see BufferedImage
 * @see BufferedImageFilter
 * @see AffineTransformOp
 * @see BandCombineOp
 * @see ColorConvertOp
 * @see ConvolveOp
 * @see LookupOp
 * @see RescaleOp
 */
public interface BufferedImageOp {
    /**
     * Performs a single-input/single-output operation on a
     * {@code BufferedImage}.
     * If the color models for the two images do not match, a color
     * conversion into the destination color model is performed.
     * If the destination image is null,
     * a {@code BufferedImage} with an appropriate {@code ColorModel}
     * is created.
     * <p>
     * An {@code IllegalArgumentException} may be thrown if the source
     * and/or destination image is incompatible with the types of images       $
     * allowed by the class implementing this filter.
     *
     * @param src The {@code BufferedImage} to be filtered
     * @param dest The {@code BufferedImage} in which to store the results$
     *
     * @return The filtered {@code BufferedImage}.
     *
     * @throws IllegalArgumentException If the source and/or destination
     * image is not compatible with the types of images allowed by the class
     * implementing this filter.
     */
    public BufferedImage filter(BufferedImage src, BufferedImage dest);

    /**
     * Returns the bounding box of the filtered destination image.
     * An {@code IllegalArgumentException} may be thrown if the source
     * image is incompatible with the types of images allowed
     * by the class implementing this filter.
     *
     * @param src The {@code BufferedImage} to be filtered
     *
     * @return The {@code Rectangle2D} representing the destination
     * image's bounding box.
     */
    public Rectangle2D getBounds2D (BufferedImage src);

    /**
     * Creates a zeroed destination image with the correct size and number of
     * bands.
     * An {@code IllegalArgumentException} may be thrown if the source
     * image is incompatible with the types of images allowed
     * by the class implementing this filter.
     *
     * @param src The {@code BufferedImage} to be filtered
     * @param destCM {@code ColorModel} of the destination.  If null,
     * the {@code ColorModel} of the source is used.
     *
     * @return The zeroed destination image.
     */
    public BufferedImage createCompatibleDestImage (BufferedImage src,
                                                    ColorModel destCM);

    /**
     * Returns the location of the corresponding destination point given a
     * point in the source image.  If {@code dstPt} is specified, it
     * is used to hold the return value.
     * @param srcPt the {@code Point2D} that represents the point in
     * the source image
     * @param dstPt The {@code Point2D} in which to store the result
     *
     * @return The {@code Point2D} in the destination image that
     * corresponds to the specified point in the source image.
     */
    public Point2D getPoint2D (Point2D srcPt, Point2D dstPt);

    /**
     * Returns the rendering hints for this operation.
     *
     * @return The {@code RenderingHints} object for this
     * {@code BufferedImageOp}.  Returns
     * null if no hints have been set.
     */
    public RenderingHints getRenderingHints();
}
