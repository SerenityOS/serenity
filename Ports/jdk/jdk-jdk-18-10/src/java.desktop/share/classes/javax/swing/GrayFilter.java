/*
 * Copyright (c) 1997, 2014, Oracle and/or its affiliates. All rights reserved.
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
package javax.swing;

import java.awt.*;
import java.awt.image.*;
import sun.awt.image.MultiResolutionCachedImage;

/**
 * An image filter that "disables" an image by turning
 * it into a grayscale image, and brightening the pixels
 * in the image. Used by buttons to create an image for
 * a disabled button.
 *
 * @author      Jeff Dinkins
 * @author      Tom Ball
 * @author      Jim Graham
 * @since 1.2
 */
public class GrayFilter extends RGBImageFilter {
    private boolean brighter;
    private int percent;

    /**
     * Creates a disabled image
     *
     * @param i  an {@code Image} to be created as disabled
     * @return  the new grayscale image created from {@code i}
     */
    public static Image createDisabledImage(Image i) {
        if (i instanceof MultiResolutionImage) {
            return MultiResolutionCachedImage
                    .map((MultiResolutionImage) i,
                         (img) -> createDisabledImageImpl(img));
        }
        return createDisabledImageImpl(i);
    }

    private static Image createDisabledImageImpl(Image i) {
        GrayFilter filter = new GrayFilter(true, 50);
        ImageProducer prod = new FilteredImageSource(i.getSource(), filter);
        Image grayImage = Toolkit.getDefaultToolkit().createImage(prod);
        return grayImage;
    }

    /**
     * Constructs a GrayFilter object that filters a color image to a
     * grayscale image. Used by buttons to create disabled ("grayed out")
     * button images.
     *
     * @param b  a boolean -- true if the pixels should be brightened
     * @param p  an int in the range 0..100 that determines the percentage
     *           of gray, where 100 is the darkest gray, and 0 is the lightest
     */
    public GrayFilter(boolean b, int p) {
        brighter = b;
        percent = p;

        // canFilterIndexColorModel indicates whether or not it is acceptable
        // to apply the color filtering of the filterRGB method to the color
        // table entries of an IndexColorModel object in lieu of pixel by pixel
        // filtering.
        canFilterIndexColorModel = true;
    }

    /**
     * Overrides <code>RGBImageFilter.filterRGB</code>.
     */
    public int filterRGB(int x, int y, int rgb) {
        // Use NTSC conversion formula.
        int gray = (int)((0.30 * ((rgb >> 16) & 0xff) +
                         0.59 * ((rgb >> 8) & 0xff) +
                         0.11 * (rgb & 0xff)) / 3);

        if (brighter) {
            gray = (255 - ((255 - gray) * (100 - percent) / 100));
        } else {
            gray = (gray * (100 - percent) / 100);
        }

        if (gray < 0) gray = 0;
        if (gray > 255) gray = 255;
        return (rgb & 0xff000000) | (gray << 16) | (gray << 8) | (gray << 0);
    }
}
