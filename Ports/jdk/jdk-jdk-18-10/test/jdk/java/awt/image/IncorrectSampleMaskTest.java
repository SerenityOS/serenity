/*
 * Copyright (c) 2009, 2011, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @bug     6782574
 * @summary Test verifies that incorrect sample masks are correctly handled
 *          by the constructor of the SinglePixelPackedSampleModel class
 *          and do not cause internal error in the medialib glue code.
 *
 * @run     main IncorrectSampleMaskTest
 */

import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImageOp;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.awt.image.DataBufferUShort;
import java.awt.image.Raster;
import java.awt.image.RasterOp;
import java.awt.image.WritableRaster;
import java.awt.image.SinglePixelPackedSampleModel;

public class IncorrectSampleMaskTest {
    public static void main(String[] args) {
        int[] dataTypes = new int[] {
            DataBuffer.TYPE_BYTE,
            DataBuffer.TYPE_USHORT,
            DataBuffer.TYPE_INT };

        for (int type : dataTypes) {
            doTest(type);
        }
    }

    private static final int w = 100;
    private static final int h = 100;

    private static AffineTransform at =
        AffineTransform.getScaleInstance(0.5, 0.5);

    private static RasterOp op =
        new AffineTransformOp(at, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);

    private static void doTest(int dataType) {
        int maxSize = DataBuffer.getDataTypeSize(dataType);
        System.out.println("Type size: " + maxSize);

        int theMask = (int)(1L << (maxSize + 2)) - 1;
        System.out.printf("theMask=%x\n", theMask);

        SinglePixelPackedSampleModel sm =
            new SinglePixelPackedSampleModel(dataType, w, h,
                                             new int[] { theMask });


        int[] sampleSize = sm.getSampleSize();
        for (int s : sampleSize) {
            if (s > maxSize) {
                throw new RuntimeException("Test failed: sample size is too big:" + s);
            }
        }

        System.out.println("Test medialib...");
        DataBuffer buf = createDataBuffer(dataType);

        WritableRaster wr = Raster.createWritableRaster(sm, buf, null);

        op.filter(wr, null);
        System.out.println("Test PASSED.");
    }

    private static DataBuffer createDataBuffer(int type) {
        switch (type) {
        case DataBuffer.TYPE_BYTE: {
            byte[] buf = new byte[w * h];
            return new DataBufferByte(buf, buf.length);
        }
        case DataBuffer.TYPE_USHORT: {
            short[] buf = new short[w * h];
            return new DataBufferUShort(buf, buf.length);
        }
        case DataBuffer.TYPE_INT: {
            int[] buf = new int[w * h];
            return new DataBufferInt(buf, buf.length);
        }
        default :
            throw new RuntimeException("Unsupported data type.");
        }
    }
}
