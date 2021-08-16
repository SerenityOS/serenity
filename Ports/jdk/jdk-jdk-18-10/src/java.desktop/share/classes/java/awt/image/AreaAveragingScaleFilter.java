/*
 * Copyright (c) 1996, 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.image.ImageConsumer;
import java.awt.image.ColorModel;
import java.util.Hashtable;
import java.awt.Rectangle;

/**
 * An ImageFilter class for scaling images using a simple area averaging
 * algorithm that produces smoother results than the nearest neighbor
 * algorithm.
 * <p>This class extends the basic ImageFilter Class to scale an existing
 * image and provide a source for a new image containing the resampled
 * image.  The pixels in the source image are blended to produce pixels
 * for an image of the specified size.  The blending process is analogous
 * to scaling up the source image to a multiple of the destination size
 * using pixel replication and then scaling it back down to the destination
 * size by simply averaging all the pixels in the supersized image that
 * fall within a given pixel of the destination image.  If the data from
 * the source is not delivered in TopDownLeftRight order then the filter
 * will back off to a simple pixel replication behavior and utilize the
 * requestTopDownLeftRightResend() method to refilter the pixels in a
 * better way at the end.
 * <p>It is meant to be used in conjunction with a FilteredImageSource
 * object to produce scaled versions of existing images.  Due to
 * implementation dependencies, there may be differences in pixel values
 * of an image filtered on different platforms.
 *
 * @see FilteredImageSource
 * @see ReplicateScaleFilter
 * @see ImageFilter
 *
 * @author      Jim Graham
 */
public class AreaAveragingScaleFilter extends ReplicateScaleFilter {
    private static final ColorModel rgbmodel = ColorModel.getRGBdefault();
    private static final int neededHints = (TOPDOWNLEFTRIGHT
                                            | COMPLETESCANLINES);

    private boolean passthrough;
    private float[] reds, greens, blues, alphas;
    private int savedy;
    private int savedyrem;

    /**
     * Constructs an AreaAveragingScaleFilter that scales the pixels from
     * its source Image as specified by the width and height parameters.
     * @param width the target width to scale the image
     * @param height the target height to scale the image
     * @throws IllegalArgumentException if {@code width} equals
     *         zero or {@code height} equals zero
     */
    public AreaAveragingScaleFilter(int width, int height) {
        super(width, height);
    }

    /**
     * Detect if the data is being delivered with the necessary hints
     * to allow the averaging algorithm to do its work.
     * <p>
     * Note: This method is intended to be called by the
     * {@code ImageProducer} of the {@code Image} whose
     * pixels are being filtered.  Developers using
     * this class to filter pixels from an image should avoid calling
     * this method directly since that operation could interfere
     * with the filtering operation.
     * @see ImageConsumer#setHints
     */
    public void setHints(int hints) {
        passthrough = ((hints & neededHints) != neededHints);
        super.setHints(hints);
    }

    private void makeAccumBuffers() {
        reds = new float[destWidth];
        greens = new float[destWidth];
        blues = new float[destWidth];
        alphas = new float[destWidth];
    }

    private int[] calcRow() {
        float origmult = ((float) srcWidth) * srcHeight;
        if (outpixbuf == null || !(outpixbuf instanceof int[])) {
            outpixbuf = new int[destWidth];
        }
        int[] outpix = (int[]) outpixbuf;
        for (int x = 0; x < destWidth; x++) {
            float mult = origmult;
            int a = Math.round(alphas[x] / mult);
            if (a <= 0) {
                a = 0;
            } else if (a >= 255) {
                a = 255;
            } else {
                // un-premultiply the components (by modifying mult here, we
                // are effectively doing the divide by mult and divide by
                // alpha in the same step)
                mult = alphas[x] / 255;
            }
            int r = Math.round(reds[x] / mult);
            int g = Math.round(greens[x] / mult);
            int b = Math.round(blues[x] / mult);
            if (r < 0) {r = 0;} else if (r > 255) {r = 255;}
            if (g < 0) {g = 0;} else if (g > 255) {g = 255;}
            if (b < 0) {b = 0;} else if (b > 255) {b = 255;}
            outpix[x] = (a << 24 | r << 16 | g << 8 | b);
        }
        return outpix;
    }

    private void accumPixels(int x, int y, int w, int h,
                             ColorModel model, Object pixels, int off,
                             int scansize) {
        if (reds == null) {
            makeAccumBuffers();
        }
        int sy = y;
        int syrem = destHeight;
        int dy, dyrem;
        if (sy == 0) {
            dy = 0;
            dyrem = 0;
        } else {
            dy = savedy;
            dyrem = savedyrem;
        }
        while (sy < y + h) {
            int amty;
            if (dyrem == 0) {
                for (int i = 0; i < destWidth; i++) {
                    alphas[i] = reds[i] = greens[i] = blues[i] = 0f;
                }
                dyrem = srcHeight;
            }
            if (syrem < dyrem) {
                amty = syrem;
            } else {
                amty = dyrem;
            }
            int sx = 0;
            int dx = 0;
            int sxrem = 0;
            int dxrem = srcWidth;
            float a = 0f, r = 0f, g = 0f, b = 0f;
            while (sx < w) {
                if (sxrem == 0) {
                    sxrem = destWidth;
                    int rgb;
                    if (pixels instanceof byte[]) {
                        rgb = ((byte[]) pixels)[off + sx] & 0xff;
                    } else {
                        rgb = ((int[]) pixels)[off + sx];
                    }
                    // getRGB() always returns non-premultiplied components
                    rgb = model.getRGB(rgb);
                    a = rgb >>> 24;
                    r = (rgb >> 16) & 0xff;
                    g = (rgb >>  8) & 0xff;
                    b = rgb & 0xff;
                    // premultiply the components if necessary
                    if (a != 255.0f) {
                        float ascale = a / 255.0f;
                        r *= ascale;
                        g *= ascale;
                        b *= ascale;
                    }
                }
                int amtx;
                if (sxrem < dxrem) {
                    amtx = sxrem;
                } else {
                    amtx = dxrem;
                }
                float mult = ((float) amtx) * amty;
                alphas[dx] += mult * a;
                reds[dx] += mult * r;
                greens[dx] += mult * g;
                blues[dx] += mult * b;
                if ((sxrem -= amtx) == 0) {
                    sx++;
                }
                if ((dxrem -= amtx) == 0) {
                    dx++;
                    dxrem = srcWidth;
                }
            }
            if ((dyrem -= amty) == 0) {
                int[] outpix = calcRow();
                do {
                    consumer.setPixels(0, dy, destWidth, 1,
                                       rgbmodel, outpix, 0, destWidth);
                    dy++;
                } while ((syrem -= amty) >= amty && amty == srcHeight);
            } else {
                syrem -= amty;
            }
            if (syrem == 0) {
                syrem = destHeight;
                sy++;
                off += scansize;
            }
        }
        savedyrem = dyrem;
        savedy = dy;
    }

    /**
     * Combine the components for the delivered byte pixels into the
     * accumulation arrays and send on any averaged data for rows of
     * pixels that are complete.  If the correct hints were not
     * specified in the setHints call then relay the work to our
     * superclass which is capable of scaling pixels regardless of
     * the delivery hints.
     * <p>
     * Note: This method is intended to be called by the
     * {@code ImageProducer} of the {@code Image}
     * whose pixels are being filtered.  Developers using
     * this class to filter pixels from an image should avoid calling
     * this method directly since that operation could interfere
     * with the filtering operation.
     * @see ReplicateScaleFilter
     */
    public void setPixels(int x, int y, int w, int h,
                          ColorModel model, byte[] pixels, int off,
                          int scansize) {
        if (passthrough) {
            super.setPixels(x, y, w, h, model, pixels, off, scansize);
        } else {
            accumPixels(x, y, w, h, model, pixels, off, scansize);
        }
    }

    /**
     * Combine the components for the delivered int pixels into the
     * accumulation arrays and send on any averaged data for rows of
     * pixels that are complete.  If the correct hints were not
     * specified in the setHints call then relay the work to our
     * superclass which is capable of scaling pixels regardless of
     * the delivery hints.
     * <p>
     * Note: This method is intended to be called by the
     * {@code ImageProducer} of the {@code Image}
     * whose pixels are being filtered.  Developers using
     * this class to filter pixels from an image should avoid calling
     * this method directly since that operation could interfere
     * with the filtering operation.
     * @see ReplicateScaleFilter
     */
    public void setPixels(int x, int y, int w, int h,
                          ColorModel model, int[] pixels, int off,
                          int scansize) {
        if (passthrough) {
            super.setPixels(x, y, w, h, model, pixels, off, scansize);
        } else {
            accumPixels(x, y, w, h, model, pixels, off, scansize);
        }
    }
}
