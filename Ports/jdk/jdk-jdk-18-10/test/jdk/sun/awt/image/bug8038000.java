/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug     8038000 8047066
 *
 * @summary Verifies that we could create different type of Rasters with height 1
 * and strideline which exceeds raster width.
 * Also checks that a set of RasterOp work correctly with such kind of Rasters.
 * For 8047066 verifies that ColorConvertOp could process
 * Raster (ByteBuffer + SinglePixelPackedSampleModel)
 *
 * @run     main bug8038000
 */

import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.geom.AffineTransform;
import java.awt.image.*;
import java.util.Arrays;

public class bug8038000 {

    public static void main(String[] args) throws Exception {
        new bug8038000().checkOps();

        // No exceptions - Passed
    }

    private void checkOps() throws Exception {

        RasterOp[] ops = new RasterOp[] {
                new ColorConvertOp(ColorSpace.getInstance(ColorSpace.CS_sRGB),
                        ColorSpace.getInstance(ColorSpace.CS_LINEAR_RGB), null),
                new AffineTransformOp(AffineTransform.getScaleInstance(1, 1.1), null)
        };


        for (RasterOp op: ops) {
            // Banded rasters
            checkOp(Raster.createBandedRaster(DataBuffer.TYPE_BYTE, 10, 1, 10,
                            new int[] {0, 1, 2}, new int[]{2,1,0}, null),
                    Raster.createBandedRaster(DataBuffer.TYPE_BYTE, 10, 1, 1001,
                            new int[] {0, 1, 2}, new int[]{2,1,0}, null), op);
            checkOp(Raster.createBandedRaster(DataBuffer.TYPE_USHORT, 10, 1, 10,
                    new int[] {0, 1, 2}, new int[]{2,1,0}, null),
                    Raster.createBandedRaster(DataBuffer.TYPE_USHORT, 10, 1, 1001,
                            new int[] {0, 1, 2}, new int[]{2,1,0}, null), op);
            checkOp(Raster.createBandedRaster(DataBuffer.TYPE_INT, 10, 1, 10,
                    new int[] {0, 1, 2}, new int[]{2,1,0}, null),
                    Raster.createBandedRaster(DataBuffer.TYPE_INT, 10, 1, 1001,
                            new int[] {0, 1, 2}, new int[]{2,1,0}, null), op);

            // Interleaved rasters
            checkOp(Raster.createInterleavedRaster(DataBuffer.TYPE_BYTE,
                            10, 1, 30, 3, new int[]{0, 1, 2}, null),
                    Raster.createInterleavedRaster(DataBuffer.TYPE_BYTE,
                            10, 1, 1001, 3, new int[]{0, 1, 2}, null),
                    op);

            checkOp(Raster.createInterleavedRaster(DataBuffer.TYPE_USHORT,
                            10, 1, 30, 3, new int[]{0, 1, 2}, null),
                    Raster.createInterleavedRaster(DataBuffer.TYPE_USHORT,
                            10, 1, 1001, 3, new int[]{0, 1, 2}, null),
                    op);

            // Packed rasters
            checkOp(Raster.createPackedRaster(new DataBufferByte(10), 10, 1, 10,
                            new int[] {0x01, 0x02, 0x04}, null),
                    Raster.createPackedRaster(new DataBufferByte(10), 10, 1, 2000,
                            new int[] {0x01, 0x02, 0x04}, null),
                    op);
            checkOp(Raster.createPackedRaster(new DataBufferInt(10), 10, 1, 10,
                        new int[] {0xff0000, 0x00ff00, 0x0000ff}, null),
                    Raster.createPackedRaster(new DataBufferInt(10), 10, 1, 20,
                            new int[] {0xff0000, 0x00ff00, 0x0000ff}, null),
                    op);

        }
    }

    /**
     *  Takes two identical rasters (identical with the exception of scanline stride)
     *  fills their pixels with identical data, applies the RasterOp to both rasters
     *  and checks that the result is the same
     */
    private void checkOp(WritableRaster wr1, WritableRaster wr2, RasterOp op) {
        System.out.println("Checking " + op + " with rasters: \n    " + wr1 +
                "\n    " + wr2);
        try {
            WritableRaster r1 = op.filter(fillRaster(wr1), null);
            WritableRaster r2 = op.filter(fillRaster(wr2), null);
            compareRasters(r1, r2);
        } catch (ImagingOpException e) {
            System.out.println("    Skip: Op is not supported: " + e);
        }
    }

    private WritableRaster fillRaster(WritableRaster wr) {
        int c = 0;
        for(int x = wr.getMinX(); x < wr.getMinX() + wr.getWidth(); x++) {
            for(int y = wr.getMinY(); y < wr.getMinY() + wr.getHeight(); y++) {
                for (int b = 0; b < wr.getNumBands(); b++) {
                    wr.setSample(x, y, b, c++);
                }
            }
        }
        return wr;
    }

    private void compareRasters(Raster r1, Raster r2) {
        Rectangle bounds = r1.getBounds();
        if (!bounds.equals(r2.getBounds())) {
            throw new RuntimeException("Bounds differ.");
        }

        if (r1.getNumBands() != r2.getNumBands()) {
            throw new RuntimeException("Bands differ.");
        }

        int[] b1 = new int[r1.getNumBands()];
        int[] b2 = new int[r1.getNumBands()];

        for (int x = (int) bounds.getX(); x < bounds.getMaxX(); x++) {
            for (int y = (int) bounds.getY(); y < bounds.getMaxY(); y++) {
                r1.getPixel(x,y, b1);
                r2.getPixel(x,y, b2);
                if (!Arrays.equals(b1, b2)) {
                    throw new RuntimeException("Pixels differ.");
                }
            }
        }
    }
}
