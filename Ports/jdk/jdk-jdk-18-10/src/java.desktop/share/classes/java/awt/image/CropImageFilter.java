/*
 * Copyright (c) 1995, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * An ImageFilter class for cropping images.
 * This class extends the basic ImageFilter Class to extract a given
 * rectangular region of an existing Image and provide a source for a
 * new image containing just the extracted region.  It is meant to
 * be used in conjunction with a FilteredImageSource object to produce
 * cropped versions of existing images.
 *
 * @see FilteredImageSource
 * @see ImageFilter
 *
 * @author      Jim Graham
 */
public class CropImageFilter extends ImageFilter {
    int cropX;
    int cropY;
    int cropW;
    int cropH;

    /**
     * Constructs a CropImageFilter that extracts the absolute rectangular
     * region of pixels from its source Image as specified by the x, y,
     * w, and h parameters.
     * @param x the x location of the top of the rectangle to be extracted
     * @param y the y location of the top of the rectangle to be extracted
     * @param w the width of the rectangle to be extracted
     * @param h the height of the rectangle to be extracted
     */
    public CropImageFilter(int x, int y, int w, int h) {
        cropX = x;
        cropY = y;
        cropW = w;
        cropH = h;
    }

    /**
     * Passes along  the properties from the source object after adding a
     * property indicating the cropped region.
     * This method invokes {@code super.setProperties},
     * which might result in additional properties being added.
     * <p>
     * Note: This method is intended to be called by the
     * {@code ImageProducer} of the {@code Image} whose pixels
     * are being filtered. Developers using
     * this class to filter pixels from an image should avoid calling
     * this method directly since that operation could interfere
     * with the filtering operation.
     */
    public void setProperties(Hashtable<?,?> props) {
        @SuppressWarnings("unchecked")
        Hashtable<Object,Object> p = (Hashtable<Object,Object>)props.clone();
        p.put("croprect", new Rectangle(cropX, cropY, cropW, cropH));
        super.setProperties(p);
    }

    /**
     * Override the source image's dimensions and pass the dimensions
     * of the rectangular cropped region to the ImageConsumer.
     * <p>
     * Note: This method is intended to be called by the
     * {@code ImageProducer} of the {@code Image} whose
     * pixels are being filtered. Developers using
     * this class to filter pixels from an image should avoid calling
     * this method directly since that operation could interfere
     * with the filtering operation.
     * @see ImageConsumer
     */
    public void setDimensions(int w, int h) {
        consumer.setDimensions(cropW, cropH);
    }

    /**
     * Determine whether the delivered byte pixels intersect the region to
     * be extracted and passes through only that subset of pixels that
     * appear in the output region.
     * <p>
     * Note: This method is intended to be called by the
     * {@code ImageProducer} of the {@code Image} whose
     * pixels are being filtered. Developers using
     * this class to filter pixels from an image should avoid calling
     * this method directly since that operation could interfere
     * with the filtering operation.
     */
    public void setPixels(int x, int y, int w, int h,
                          ColorModel model, byte[] pixels, int off,
                          int scansize) {
        int x1 = x;
        if (x1 < cropX) {
            x1 = cropX;
        }
    int x2 = addWithoutOverflow(x, w);
        if (x2 > cropX + cropW) {
            x2 = cropX + cropW;
        }
        int y1 = y;
        if (y1 < cropY) {
            y1 = cropY;
        }

    int y2 = addWithoutOverflow(y, h);
        if (y2 > cropY + cropH) {
            y2 = cropY + cropH;
        }
        if (x1 >= x2 || y1 >= y2) {
            return;
        }
        consumer.setPixels(x1 - cropX, y1 - cropY, (x2 - x1), (y2 - y1),
                           model, pixels,
                           off + (y1 - y) * scansize + (x1 - x), scansize);
    }

    /**
     * Determine if the delivered int pixels intersect the region to
     * be extracted and pass through only that subset of pixels that
     * appear in the output region.
     * <p>
     * Note: This method is intended to be called by the
     * {@code ImageProducer} of the {@code Image} whose
     * pixels are being filtered. Developers using
     * this class to filter pixels from an image should avoid calling
     * this method directly since that operation could interfere
     * with the filtering operation.
     */
    public void setPixels(int x, int y, int w, int h,
                          ColorModel model, int[] pixels, int off,
                          int scansize) {
        int x1 = x;
        if (x1 < cropX) {
            x1 = cropX;
        }
    int x2 = addWithoutOverflow(x, w);
        if (x2 > cropX + cropW) {
            x2 = cropX + cropW;
        }
        int y1 = y;
        if (y1 < cropY) {
            y1 = cropY;
        }

    int y2 = addWithoutOverflow(y, h);
        if (y2 > cropY + cropH) {
            y2 = cropY + cropH;
        }
        if (x1 >= x2 || y1 >= y2) {
            return;
        }
        consumer.setPixels(x1 - cropX, y1 - cropY, (x2 - x1), (y2 - y1),
                           model, pixels,
                           off + (y1 - y) * scansize + (x1 - x), scansize);
    }

    //check for potential overflow (see bug 4801285)
    private int addWithoutOverflow(int x, int w) {
        int x2 = x + w;
        if ( x > 0 && w > 0 && x2 < 0 ) {
            x2 = Integer.MAX_VALUE;
        } else if( x < 0 && w < 0 && x2 > 0 ) {
            x2 = Integer.MIN_VALUE;
        }
        return x2;
    }
}
