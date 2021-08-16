/*
 * Copyright (c) 2005, 2006, Oracle and/or its affiliates. All rights reserved.
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
package javax.swing.plaf.nimbus;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.Transparency;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.awt.image.ColorModel;

/**
 * EffectUtils
 *
 * @author Created by Jasper Potts (Jun 18, 2007)
 */
class EffectUtils {

    /**
     * Clear a transparent image to 100% transparent
     *
     * @param img The image to clear
     */
    static void clearImage(BufferedImage img) {
        Graphics2D g2 = img.createGraphics();
        g2.setComposite(AlphaComposite.Clear);
        g2.fillRect(0, 0, img.getWidth(), img.getHeight());
        g2.dispose();
    }

    // =================================================================================================================
    // Blur

    /**
     * Apply Gaussian Blur to Image
     *
     * @param src    The image tp
     * @param dst    The destination image to draw blured src image into, null if you want a new one created
     * @param radius The blur kernel radius
     * @return The blured image
     */
    static BufferedImage gaussianBlur(BufferedImage src, BufferedImage dst, int radius) {
        int width = src.getWidth();
        int height = src.getHeight();
        if (dst == null || dst.getWidth() != width || dst.getHeight() != height || src.getType() != dst.getType()) {
            dst = createColorModelCompatibleImage(src);
        }
        float[] kernel = createGaussianKernel(radius);
        if (src.getType() == BufferedImage.TYPE_INT_ARGB) {
            int[] srcPixels = new int[width * height];
            int[] dstPixels = new int[width * height];
            getPixels(src, 0, 0, width, height, srcPixels);
            // horizontal pass
            blur(srcPixels, dstPixels, width, height, kernel, radius);
            // vertical pass
            //noinspection SuspiciousNameCombination
            blur(dstPixels, srcPixels, height, width, kernel, radius);
            // the result is now stored in srcPixels due to the 2nd pass
            setPixels(dst, 0, 0, width, height, srcPixels);
        } else if (src.getType() == BufferedImage.TYPE_BYTE_GRAY) {
            byte[] srcPixels = new byte[width * height];
            byte[] dstPixels = new byte[width * height];
            getPixels(src, 0, 0, width, height, srcPixels);
            // horizontal pass
            blur(srcPixels, dstPixels, width, height, kernel, radius);
            // vertical pass
            //noinspection SuspiciousNameCombination
            blur(dstPixels, srcPixels, height, width, kernel, radius);
            // the result is now stored in srcPixels due to the 2nd pass
            setPixels(dst, 0, 0, width, height, srcPixels);
        } else {
            throw new IllegalArgumentException("EffectUtils.gaussianBlur() src image is not a supported type, type=[" +
                    src.getType() + "]");
        }
        return dst;
    }

    /**
     * <p>Blurs the source pixels into the destination pixels. The force of the blur is specified by the radius which
     * must be greater than 0.</p> <p>The source and destination pixels arrays are expected to be in the INT_ARGB
     * format.</p> <p>After this method is executed, dstPixels contains a transposed and filtered copy of
     * srcPixels.</p>
     *
     * @param srcPixels the source pixels
     * @param dstPixels the destination pixels
     * @param width     the width of the source picture
     * @param height    the height of the source picture
     * @param kernel    the kernel of the blur effect
     * @param radius    the radius of the blur effect
     */
    private static void blur(int[] srcPixels, int[] dstPixels,
                             int width, int height,
                             float[] kernel, int radius) {
        float a;
        float r;
        float g;
        float b;

        int ca;
        int cr;
        int cg;
        int cb;

        for (int y = 0; y < height; y++) {
            int index = y;
            int offset = y * width;

            for (int x = 0; x < width; x++) {
                a = r = g = b = 0.0f;

                for (int i = -radius; i <= radius; i++) {
                    int subOffset = x + i;
                    if (subOffset < 0 || subOffset >= width) {
                        subOffset = (x + width) % width;
                    }

                    int pixel = srcPixels[offset + subOffset];
                    float blurFactor = kernel[radius + i];

                    a += blurFactor * ((pixel >> 24) & 0xFF);
                    r += blurFactor * ((pixel >> 16) & 0xFF);
                    g += blurFactor * ((pixel >> 8) & 0xFF);
                    b += blurFactor * ((pixel) & 0xFF);
                }

                ca = (int) (a + 0.5f);
                cr = (int) (r + 0.5f);
                cg = (int) (g + 0.5f);
                cb = (int) (b + 0.5f);

                dstPixels[index] = ((ca > 255 ? 255 : ca) << 24) |
                        ((cr > 255 ? 255 : cr) << 16) |
                        ((cg > 255 ? 255 : cg) << 8) |
                        (cb > 255 ? 255 : cb);
                index += height;
            }
        }
    }

    /**
     * <p>Blurs the source pixels into the destination pixels. The force of the blur is specified by the radius which
     * must be greater than 0.</p> <p>The source and destination pixels arrays are expected to be in the BYTE_GREY
     * format.</p> <p>After this method is executed, dstPixels contains a transposed and filtered copy of
     * srcPixels.</p>
     *
     * @param srcPixels the source pixels
     * @param dstPixels the destination pixels
     * @param width     the width of the source picture
     * @param height    the height of the source picture
     * @param kernel    the kernel of the blur effect
     * @param radius    the radius of the blur effect
     */
    static void blur(byte[] srcPixels, byte[] dstPixels,
                            int width, int height,
                            float[] kernel, int radius) {
        float p;
        int cp;
        for (int y = 0; y < height; y++) {
            int index = y;
            int offset = y * width;
            for (int x = 0; x < width; x++) {
                p = 0.0f;
                for (int i = -radius; i <= radius; i++) {
                    int subOffset = x + i;
//                    if (subOffset < 0) subOffset = 0;
//                    if (subOffset >= width) subOffset = width-1;
                    if (subOffset < 0 || subOffset >= width) {
                        subOffset = (x + width) % width;
                    }
                    int pixel = srcPixels[offset + subOffset] & 0xFF;
                    float blurFactor = kernel[radius + i];
                    p += blurFactor * pixel;
                }
                cp = (int) (p + 0.5f);
                dstPixels[index] = (byte) (cp > 255 ? 255 : cp);
                index += height;
            }
        }
    }

    static float[] createGaussianKernel(int radius) {
        if (radius < 1) {
            throw new IllegalArgumentException("Radius must be >= 1");
        }

        float[] data = new float[radius * 2 + 1];

        float sigma = radius / 3.0f;
        float twoSigmaSquare = 2.0f * sigma * sigma;
        float sigmaRoot = (float) Math.sqrt(twoSigmaSquare * Math.PI);
        float total = 0.0f;

        for (int i = -radius; i <= radius; i++) {
            float distance = i * i;
            int index = i + radius;
            data[index] = (float) Math.exp(-distance / twoSigmaSquare) / sigmaRoot;
            total += data[index];
        }

        for (int i = 0; i < data.length; i++) {
            data[i] /= total;
        }

        return data;
    }

    // =================================================================================================================
    // Get/Set Pixels helper methods

    /**
     * <p>Returns an array of pixels, stored as integers, from a <code>BufferedImage</code>. The pixels are grabbed from
     * a rectangular area defined by a location and two dimensions. Calling this method on an image of type different
     * from <code>BufferedImage.TYPE_INT_ARGB</code> and <code>BufferedImage.TYPE_INT_RGB</code> will unmanage the
     * image.</p>
     *
     * @param img    the source image
     * @param x      the x location at which to start grabbing pixels
     * @param y      the y location at which to start grabbing pixels
     * @param w      the width of the rectangle of pixels to grab
     * @param h      the height of the rectangle of pixels to grab
     * @param pixels a pre-allocated array of pixels of size w*h; can be null
     * @return <code>pixels</code> if non-null, a new array of integers otherwise
     * @throws IllegalArgumentException is <code>pixels</code> is non-null and of length &lt; w*h
     */
    static byte[] getPixels(BufferedImage img,
                                   int x, int y, int w, int h, byte[] pixels) {
        if (w == 0 || h == 0) {
            return new byte[0];
        }

        if (pixels == null) {
            pixels = new byte[w * h];
        } else if (pixels.length < w * h) {
            throw new IllegalArgumentException("pixels array must have a length >= w*h");
        }

        int imageType = img.getType();
        if (imageType == BufferedImage.TYPE_BYTE_GRAY) {
            Raster raster = img.getRaster();
            return (byte[]) raster.getDataElements(x, y, w, h, pixels);
        } else {
            throw new IllegalArgumentException("Only type BYTE_GRAY is supported");
        }
    }

    /**
     * <p>Writes a rectangular area of pixels in the destination <code>BufferedImage</code>. Calling this method on an
     * image of type different from <code>BufferedImage.TYPE_INT_ARGB</code> and <code>BufferedImage.TYPE_INT_RGB</code>
     * will unmanage the image.</p>
     *
     * @param img    the destination image
     * @param x      the x location at which to start storing pixels
     * @param y      the y location at which to start storing pixels
     * @param w      the width of the rectangle of pixels to store
     * @param h      the height of the rectangle of pixels to store
     * @param pixels an array of pixels, stored as integers
     * @throws IllegalArgumentException is <code>pixels</code> is non-null and of length &lt; w*h
     */
    static void setPixels(BufferedImage img,
                                 int x, int y, int w, int h, byte[] pixels) {
        if (pixels == null || w == 0 || h == 0) {
            return;
        } else if (pixels.length < w * h) {
            throw new IllegalArgumentException("pixels array must have a length >= w*h");
        }
        int imageType = img.getType();
        if (imageType == BufferedImage.TYPE_BYTE_GRAY) {
            WritableRaster raster = img.getRaster();
            raster.setDataElements(x, y, w, h, pixels);
        } else {
            throw new IllegalArgumentException("Only type BYTE_GRAY is supported");
        }
    }

    /**
     * <p>Returns an array of pixels, stored as integers, from a
     * <code>BufferedImage</code>. The pixels are grabbed from a rectangular
     * area defined by a location and two dimensions. Calling this method on
     * an image of type different from <code>BufferedImage.TYPE_INT_ARGB</code>
     * and <code>BufferedImage.TYPE_INT_RGB</code> will unmanage the image.</p>
     *
     * @param img the source image
     * @param x the x location at which to start grabbing pixels
     * @param y the y location at which to start grabbing pixels
     * @param w the width of the rectangle of pixels to grab
     * @param h the height of the rectangle of pixels to grab
     * @param pixels a pre-allocated array of pixels of size w*h; can be null
     * @return <code>pixels</code> if non-null, a new array of integers
     *   otherwise
     * @throws IllegalArgumentException is <code>pixels</code> is non-null and
     *   of length &lt; w*h
     */
    public static int[] getPixels(BufferedImage img,
                                  int x, int y, int w, int h, int[] pixels) {
        if (w == 0 || h == 0) {
            return new int[0];
        }

        if (pixels == null) {
            pixels = new int[w * h];
        } else if (pixels.length < w * h) {
            throw new IllegalArgumentException("pixels array must have a length" +
                                               " >= w*h");
        }

        int imageType = img.getType();
        if (imageType == BufferedImage.TYPE_INT_ARGB ||
            imageType == BufferedImage.TYPE_INT_RGB) {
            Raster raster = img.getRaster();
            return (int[]) raster.getDataElements(x, y, w, h, pixels);
        }

        // Unmanages the image
        return img.getRGB(x, y, w, h, pixels, 0, w);
    }

    /**
     * <p>Writes a rectangular area of pixels in the destination
     * <code>BufferedImage</code>. Calling this method on
     * an image of type different from <code>BufferedImage.TYPE_INT_ARGB</code>
     * and <code>BufferedImage.TYPE_INT_RGB</code> will unmanage the image.</p>
     *
     * @param img the destination image
     * @param x the x location at which to start storing pixels
     * @param y the y location at which to start storing pixels
     * @param w the width of the rectangle of pixels to store
     * @param h the height of the rectangle of pixels to store
     * @param pixels an array of pixels, stored as integers
     * @throws IllegalArgumentException is <code>pixels</code> is non-null and
     *   of length &lt; w*h
     */
    public static void setPixels(BufferedImage img,
                                 int x, int y, int w, int h, int[] pixels) {
        if (pixels == null || w == 0 || h == 0) {
            return;
        } else if (pixels.length < w * h) {
            throw new IllegalArgumentException("pixels array must have a length" +
                                               " >= w*h");
        }

        int imageType = img.getType();
        if (imageType == BufferedImage.TYPE_INT_ARGB ||
            imageType == BufferedImage.TYPE_INT_RGB) {
            WritableRaster raster = img.getRaster();
            raster.setDataElements(x, y, w, h, pixels);
        } else {
            // Unmanages the image
            img.setRGB(x, y, w, h, pixels, 0, w);
        }
    }

    /**
     * <p>Returns a new <code>BufferedImage</code> using the same color model
     * as the image passed as a parameter. The returned image is only compatible
     * with the image passed as a parameter. This does not mean the returned
     * image is compatible with the hardware.</p>
     *
     * @param image the reference image from which the color model of the new
     *   image is obtained
     * @return a new <code>BufferedImage</code>, compatible with the color model
     *   of <code>image</code>
     */
    public static BufferedImage createColorModelCompatibleImage(BufferedImage image) {
        ColorModel cm = image.getColorModel();
        return new BufferedImage(cm,
            cm.createCompatibleWritableRaster(image.getWidth(),
                                              image.getHeight()),
            cm.isAlphaPremultiplied(), null);
    }

    /**
     * <p>Returns a new translucent compatible image of the specified width and
     * height. That is, the returned <code>BufferedImage</code> is compatible with
     * the graphics hardware. If the method is called in a headless
     * environment, then the returned BufferedImage will be compatible with
     * the source image.</p>
     *
     * @param width the width of the new image
     * @param height the height of the new image
     * @return a new translucent compatible <code>BufferedImage</code> of the
     *   specified width and height
     */
    public static BufferedImage createCompatibleTranslucentImage(int width,
                                                                 int height) {
        return isHeadless() ?
                new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB) :
                getGraphicsConfiguration().createCompatibleImage(width, height,
                                                   Transparency.TRANSLUCENT);
    }

    private static boolean isHeadless() {
        return GraphicsEnvironment.isHeadless();
    }

    // Returns the graphics configuration for the primary screen
    private static GraphicsConfiguration getGraphicsConfiguration() {
        return GraphicsEnvironment.getLocalGraphicsEnvironment().
                    getDefaultScreenDevice().getDefaultConfiguration();
    }

}
