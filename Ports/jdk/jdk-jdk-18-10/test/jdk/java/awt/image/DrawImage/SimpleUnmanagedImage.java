/*
 * Copyright (c) 2014, 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.awt.image.DataBufferShort;
import java.awt.image.Raster;
import java.awt.image.VolatileImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import static java.awt.Transparency.BITMASK;
import static java.awt.Transparency.OPAQUE;
import static java.awt.Transparency.TRANSLUCENT;
import static java.awt.image.BufferedImage.TYPE_3BYTE_BGR;
import static java.awt.image.BufferedImage.TYPE_4BYTE_ABGR;
import static java.awt.image.BufferedImage.TYPE_4BYTE_ABGR_PRE;
import static java.awt.image.BufferedImage.TYPE_BYTE_BINARY;
import static java.awt.image.BufferedImage.TYPE_BYTE_INDEXED;
import static java.awt.image.BufferedImage.TYPE_CUSTOM;
import static java.awt.image.BufferedImage.TYPE_INT_ARGB;
import static java.awt.image.BufferedImage.TYPE_INT_ARGB_PRE;
import static java.awt.image.BufferedImage.TYPE_INT_BGR;
import static java.awt.image.BufferedImage.TYPE_INT_RGB;

/**
 * @test
 * @key headful
 * @bug 8029253 6207877
 * @summary Tests the case when unmanaged image is drawn to VI.
 *          Results of the blit to compatibleImage are used for comparison.
 * @author Sergey Bylokhov
 * @run main/othervm SimpleUnmanagedImage
 * @run main/othervm -Dsun.java2d.uiScale=1 SimpleUnmanagedImage
 * @run main/othervm -Dsun.java2d.uiScale=2 SimpleUnmanagedImage
 */
public final class SimpleUnmanagedImage {

    // See the same test for managed images: SimpleManagedImage

    private static final int[] TYPES = {TYPE_INT_RGB, TYPE_INT_ARGB,
                                        TYPE_INT_ARGB_PRE, TYPE_INT_BGR,
                                        TYPE_3BYTE_BGR, TYPE_4BYTE_ABGR,
                                        TYPE_4BYTE_ABGR_PRE,
                                        /*TYPE_USHORT_565_RGB,
                                        TYPE_USHORT_555_RGB, TYPE_BYTE_GRAY,
                                        TYPE_USHORT_GRAY,*/ TYPE_BYTE_BINARY,
                                        TYPE_BYTE_INDEXED, TYPE_CUSTOM};
    private static final int[] TRANSPARENCIES = {OPAQUE, BITMASK, TRANSLUCENT};

    public static void main(final String[] args) throws IOException {
        for (final int viType : TRANSPARENCIES) {
            for (final int biType : TYPES) {
                BufferedImage bi = makeUnmanagedBI(biType);
                fill(bi);
                test(bi, viType);
            }
        }
    }

    private static void test(BufferedImage bi, int type)
            throws IOException {
        GraphicsEnvironment ge = GraphicsEnvironment
                .getLocalGraphicsEnvironment();
        GraphicsConfiguration gc = ge.getDefaultScreenDevice()
                                     .getDefaultConfiguration();
        VolatileImage vi = gc.createCompatibleVolatileImage(1000, 1000, type);
        BufferedImage gold = gc.createCompatibleImage(1000, 1000, type);
        // draw to compatible Image
        init(gold);
        Graphics2D big = gold.createGraphics();
        big.drawImage(bi, 7, 11, null);
        big.dispose();
        // draw to volatile image
        BufferedImage snapshot;
        while (true) {
            vi.validate(gc);
            if (vi.validate(gc) != VolatileImage.IMAGE_OK) {
                try {
                    Thread.sleep(100);
                } catch (final InterruptedException ignored) {
                }
                continue;
            }
            init(vi);
            Graphics2D vig = vi.createGraphics();
            vig.drawImage(bi, 7, 11, null);
            vig.dispose();
            snapshot = vi.getSnapshot();
            if (vi.contentsLost()) {
                try {
                    Thread.sleep(100);
                } catch (final InterruptedException ignored) {
                }
                continue;
            }
            break;
        }
        // validate images
        for (int x = 0; x < 1000; ++x) {
            for (int y = 0; y < 1000; ++y) {
                if (gold.getRGB(x, y) != snapshot.getRGB(x, y)) {
                    ImageIO.write(gold, "png", new File("gold.png"));
                    ImageIO.write(snapshot, "png", new File("bi.png"));
                    throw new RuntimeException("Test failed.");
                }
            }
        }
    }

    private static BufferedImage makeUnmanagedBI(final int type) {
        final BufferedImage bi;
        if (type == TYPE_CUSTOM) {
            bi = makeCustomUnmanagedBI();
        } else {
            bi = new BufferedImage(511, 255, type);
        }
        final DataBuffer db = bi.getRaster().getDataBuffer();
        if (db instanceof DataBufferInt) {
            ((DataBufferInt) db).getData();
        } else if (db instanceof DataBufferShort) {
            ((DataBufferShort) db).getData();
        } else if (db instanceof DataBufferByte) {
            ((DataBufferByte) db).getData();
        }
        bi.setAccelerationPriority(0.0f);
        return bi;
    }

    /**
     * Returns the custom buffered image, which mostly identical to
     * BufferedImage.(w,h,TYPE_3BYTE_BGR), but uses the bigger scanlineStride.
     * This means that the raster will have gaps, between the rows.
     */
    private static BufferedImage makeCustomUnmanagedBI() {
        int w = 511, h = 255;
        ColorSpace cs = ColorSpace.getInstance(ColorSpace.CS_sRGB);
        int[] nBits = {8, 8, 8};
        int[] bOffs = {2, 1, 0};
        ColorModel colorModel = new ComponentColorModel(cs, nBits, false, false,
                                                        Transparency.OPAQUE,
                                                        DataBuffer.TYPE_BYTE);
        WritableRaster raster =
                Raster.createInterleavedRaster(DataBuffer.TYPE_BYTE, w, h,
                                               w * 3 + 2, 3, bOffs, null);
        return new BufferedImage(colorModel, raster, true, null);
    }

    private static void init(final Image image) {
        final Graphics2D graphics = (Graphics2D) image.getGraphics();
        graphics.setComposite(AlphaComposite.Src);
        graphics.setColor(new Color(0, 0, 0, 0));
        graphics.fillRect(0, 0, image.getWidth(null), image.getHeight(null));
        graphics.dispose();
    }

    private static void fill(final Image image) {
        final Graphics2D graphics = (Graphics2D) image.getGraphics();
        graphics.setComposite(AlphaComposite.Src);
        for (int i = 0; i < image.getHeight(null); ++i) {
            graphics.setColor(new Color(i, 0, 0));
            graphics.fillRect(0, i, image.getWidth(null), 1);
        }
        graphics.dispose();
    }
}
