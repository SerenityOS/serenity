/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.IndexColorModel;
import java.awt.image.MultiPixelPackedSampleModel;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;

/*
 * @test
 * @bug 8201433
 * @summary This test may throw OOME or NPE from native code,
 *          it should not crash
 * @requires os.maxMemory >= 2G
 * @run main/othervm/timeout=300000 -Xms1000m -Xmx1000m ICMColorDataTest
 */
public class ICMColorDataTest {
    private static final int WIDTH  = 90;
    private static final int HEIGHT = 90;
    private static final int BITS_PER_PIXEL = 1;
    private static final int PIXELS_IN_BYTE = 8;

    // Color model components
    private static final byte[] RED   = { (byte) 255, 0 };
    private static final byte[] GREEN = { (byte) 255, 0 };
    private static final byte[] BLUE  = { (byte) 255, 0 };

    public static void main(String[] args) {
        try {
            for (long i = 0; i < 300_000; i++) {
                makeImage();
            }
        } catch (OutOfMemoryError | NullPointerException e) {
            System.err.println("Caught expected exception:\n" +
                    e.getClass() + ": " + e.getMessage());
        }
        System.err.println("Test passed");
    }

    private static void makeImage() {
        int scanLineBytes = WIDTH / PIXELS_IN_BYTE;
        if ((WIDTH & (PIXELS_IN_BYTE - 1)) != 0) {
            // Make sure all the pixels in a scan line fit
            scanLineBytes += 1;
        }

        byte[]     bits    = new byte[scanLineBytes * HEIGHT];
        DataBuffer dataBuf = new DataBufferByte(bits, bits.length, 0);
        SampleModel sampleModel = new MultiPixelPackedSampleModel(DataBuffer.TYPE_BYTE,
                                                                  WIDTH, HEIGHT, BITS_PER_PIXEL);
        WritableRaster raster = Raster.createWritableRaster(sampleModel, dataBuf, null);
        IndexColorModel indexModel = new IndexColorModel(2, 2, RED, GREEN, BLUE);
        BufferedImage bufImage = new BufferedImage(indexModel, raster,
                                                   indexModel.isAlphaPremultiplied(), null);

        Graphics g = bufImage.getGraphics();
        g.drawRect(0, 0, WIDTH - 1, HEIGHT - 1);
        g.dispose();
    }
}
