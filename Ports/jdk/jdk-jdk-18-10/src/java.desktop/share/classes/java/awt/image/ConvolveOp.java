/*
 * Copyright (c) 1997, 2013, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.color.ICC_Profile;
import java.awt.geom.Rectangle2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.lang.annotation.Native;
import sun.awt.image.ImagingLib;

/**
 * This class implements a convolution from the source
 * to the destination.
 * Convolution using a convolution kernel is a spatial operation that
 * computes the output pixel from an input pixel by multiplying the kernel
 * with the surround of the input pixel.
 * This allows the output pixel to be affected by the immediate neighborhood
 * in a way that can be mathematically specified with a kernel.
 *<p>
 * This class operates with BufferedImage data in which color components are
 * premultiplied with the alpha component.  If the Source BufferedImage has
 * an alpha component, and the color components are not premultiplied with
 * the alpha component, then the data are premultiplied before being
 * convolved.  If the Destination has color components which are not
 * premultiplied, then alpha is divided out before storing into the
 * Destination (if alpha is 0, the color components are set to 0).  If the
 * Destination has no alpha component, then the resulting alpha is discarded
 * after first dividing it out of the color components.
 * <p>
 * Rasters are treated as having no alpha channel.  If the above treatment
 * of the alpha channel in BufferedImages is not desired, it may be avoided
 * by getting the Raster of a source BufferedImage and using the filter method
 * of this class which works with Rasters.
 * <p>
 * If a RenderingHints object is specified in the constructor, the
 * color rendering hint and the dithering hint may be used when color
 * conversion is required.
 *<p>
 * Note that the Source and the Destination may not be the same object.
 * @see Kernel
 * @see java.awt.RenderingHints#KEY_COLOR_RENDERING
 * @see java.awt.RenderingHints#KEY_DITHERING
 */
public class ConvolveOp implements BufferedImageOp, RasterOp {
    Kernel kernel;
    int edgeHint;
    RenderingHints hints;
    /**
     * Edge condition constants.
     */

    /**
     * Pixels at the edge of the destination image are set to zero.  This
     * is the default.
     */

    @Native public static final int EDGE_ZERO_FILL = 0;

    /**
     * Pixels at the edge of the source image are copied to
     * the corresponding pixels in the destination without modification.
     */
    @Native public static final int EDGE_NO_OP     = 1;

    /**
     * Constructs a ConvolveOp given a Kernel, an edge condition, and a
     * RenderingHints object (which may be null).
     * @param kernel the specified {@code Kernel}
     * @param edgeCondition the specified edge condition
     * @param hints the specified {@code RenderingHints} object
     * @see Kernel
     * @see #EDGE_NO_OP
     * @see #EDGE_ZERO_FILL
     * @see java.awt.RenderingHints
     */
    public ConvolveOp(Kernel kernel, int edgeCondition, RenderingHints hints) {
        this.kernel   = kernel;
        this.edgeHint = edgeCondition;
        this.hints    = hints;
    }

    /**
     * Constructs a ConvolveOp given a Kernel.  The edge condition
     * will be EDGE_ZERO_FILL.
     * @param kernel the specified {@code Kernel}
     * @see Kernel
     * @see #EDGE_ZERO_FILL
     */
    public ConvolveOp(Kernel kernel) {
        this.kernel   = kernel;
        this.edgeHint = EDGE_ZERO_FILL;
    }

    /**
     * Returns the edge condition.
     * @return the edge condition of this {@code ConvolveOp}.
     * @see #EDGE_NO_OP
     * @see #EDGE_ZERO_FILL
     */
    public int getEdgeCondition() {
        return edgeHint;
    }

    /**
     * Returns the Kernel.
     * @return the {@code Kernel} of this {@code ConvolveOp}.
     */
    public final Kernel getKernel() {
        return (Kernel) kernel.clone();
    }

    /**
     * Performs a convolution on BufferedImages.  Each component of the
     * source image will be convolved (including the alpha component, if
     * present).
     * If the color model in the source image is not the same as that
     * in the destination image, the pixels will be converted
     * in the destination.  If the destination image is null,
     * a BufferedImage will be created with the source ColorModel.
     * The IllegalArgumentException may be thrown if the source is the
     * same as the destination.
     * @param src the source {@code BufferedImage} to filter
     * @param dst the destination {@code BufferedImage} for the
     *        filtered {@code src}
     * @return the filtered {@code BufferedImage}
     * @throws NullPointerException if {@code src} is {@code null}
     * @throws IllegalArgumentException if {@code src} equals
     *         {@code dst}
     * @throws ImagingOpException if {@code src} cannot be filtered
     */
    public final BufferedImage filter (BufferedImage src, BufferedImage dst) {
        if (src == null) {
            throw new NullPointerException("src image is null");
        }
        if (src == dst) {
            throw new IllegalArgumentException("src image cannot be the "+
                                               "same as the dst image");
        }

        boolean needToConvert = false;
        ColorModel srcCM = src.getColorModel();
        ColorModel dstCM;
        BufferedImage origDst = dst;

        // Can't convolve an IndexColorModel.  Need to expand it
        if (srcCM instanceof IndexColorModel) {
            IndexColorModel icm = (IndexColorModel) srcCM;
            src = icm.convertToIntDiscrete(src.getRaster(), false);
            srcCM = src.getColorModel();
        }

        if (dst == null) {
            dst = createCompatibleDestImage(src, null);
            dstCM = srcCM;
            origDst = dst;
        }
        else {
            dstCM = dst.getColorModel();
            if (srcCM.getColorSpace().getType() !=
                dstCM.getColorSpace().getType())
            {
                needToConvert = true;
                dst = createCompatibleDestImage(src, null);
                dstCM = dst.getColorModel();
            }
            else if (dstCM instanceof IndexColorModel) {
                dst = createCompatibleDestImage(src, null);
                dstCM = dst.getColorModel();
            }
        }

        if (ImagingLib.filter(this, src, dst) == null) {
            throw new ImagingOpException ("Unable to convolve src image");
        }

        if (needToConvert) {
            ColorConvertOp ccop = new ColorConvertOp(hints);
            ccop.filter(dst, origDst);
        }
        else if (origDst != dst) {
            java.awt.Graphics2D g = origDst.createGraphics();
            try {
                g.drawImage(dst, 0, 0, null);
            } finally {
                g.dispose();
            }
        }

        return origDst;
    }

    /**
     * Performs a convolution on Rasters.  Each band of the source Raster
     * will be convolved.
     * The source and destination must have the same number of bands.
     * If the destination Raster is null, a new Raster will be created.
     * The IllegalArgumentException may be thrown if the source is
     * the same as the destination.
     * @param src the source {@code Raster} to filter
     * @param dst the destination {@code WritableRaster} for the
     *        filtered {@code src}
     * @return the filtered {@code WritableRaster}
     * @throws NullPointerException if {@code src} is {@code null}
     * @throws ImagingOpException if {@code src} and {@code dst}
     *         do not have the same number of bands
     * @throws ImagingOpException if {@code src} cannot be filtered
     * @throws IllegalArgumentException if {@code src} equals
     *         {@code dst}
     */
    public final WritableRaster filter (Raster src, WritableRaster dst) {
        if (dst == null) {
            dst = createCompatibleDestRaster(src);
        }
        else if (src == dst) {
            throw new IllegalArgumentException("src image cannot be the "+
                                               "same as the dst image");
        }
        else if (src.getNumBands() != dst.getNumBands()) {
            throw new ImagingOpException("Different number of bands in src "+
                                         " and dst Rasters");
        }

        if (ImagingLib.filter(this, src, dst) == null) {
            throw new ImagingOpException ("Unable to convolve src image");
        }

        return dst;
    }

    /**
     * Creates a zeroed destination image with the correct size and number
     * of bands.  If destCM is null, an appropriate ColorModel will be used.
     * @param src       Source image for the filter operation.
     * @param destCM    ColorModel of the destination.  Can be null.
     * @return a destination {@code BufferedImage} with the correct
     *         size and number of bands.
     */
    public BufferedImage createCompatibleDestImage(BufferedImage src,
                                                   ColorModel destCM) {
        BufferedImage image;

        int w = src.getWidth();
        int h = src.getHeight();

        WritableRaster wr = null;

        if (destCM == null) {
            destCM = src.getColorModel();
            // Not much support for ICM
            if (destCM instanceof IndexColorModel) {
                destCM = ColorModel.getRGBdefault();
            } else {
                /* Create destination image as similar to the source
                 *  as it possible...
                 */
                wr = src.getData().createCompatibleWritableRaster(w, h);
            }
        }

        if (wr == null) {
            /* This is the case when destination color model
             * was explicitly specified (and it may be not compatible
             * with source raster structure) or source is indexed image.
             * We should use destination color model to create compatible
             * destination raster here.
             */
            wr = destCM.createCompatibleWritableRaster(w, h);
        }

        image = new BufferedImage (destCM, wr,
                                   destCM.isAlphaPremultiplied(), null);

        return image;
    }

    /**
     * Creates a zeroed destination Raster with the correct size and number
     * of bands, given this source.
     */
    public WritableRaster createCompatibleDestRaster(Raster src) {
        return src.createCompatibleWritableRaster();
    }

    /**
     * Returns the bounding box of the filtered destination image.  Since
     * this is not a geometric operation, the bounding box does not
     * change.
     */
    public final Rectangle2D getBounds2D(BufferedImage src) {
        return getBounds2D(src.getRaster());
    }

    /**
     * Returns the bounding box of the filtered destination Raster.  Since
     * this is not a geometric operation, the bounding box does not
     * change.
     */
    public final Rectangle2D getBounds2D(Raster src) {
        return src.getBounds();
    }

    /**
     * Returns the location of the destination point given a
     * point in the source.  If dstPt is non-null, it will
     * be used to hold the return value.  Since this is not a geometric
     * operation, the srcPt will equal the dstPt.
     */
    public final Point2D getPoint2D(Point2D srcPt, Point2D dstPt) {
        if (dstPt == null) {
            dstPt = new Point2D.Float();
        }
        dstPt.setLocation(srcPt.getX(), srcPt.getY());

        return dstPt;
    }

    /**
     * Returns the rendering hints for this op.
     */
    public final RenderingHints getRenderingHints() {
        return hints;
    }
}
