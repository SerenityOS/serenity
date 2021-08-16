/*
 * Copyright (c) 2008, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     6726779
 * @summary Test verifies that ConvolveOp with the EDGE_NO_OP edge condition
 *          does not cause JVM crash if size of source raster elements is
 *          greather than size of the destination raster element.
 *
 * @run     main EdgeNoOpCrash
 */
import java.awt.Point;
import java.awt.image.ConvolveOp;
import java.awt.image.DataBuffer;
import java.awt.image.ImagingOpException;
import java.awt.image.Kernel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.util.Arrays;

public class EdgeNoOpCrash {
    private static final int w = 3000;
    private static final int h = 200;

    public static void main(String[] args) {
        crashTest();
    }

    private static void crashTest() {
        Raster src = createSrcRaster();
        WritableRaster dst = createDstRaster();
        ConvolveOp op = createConvolveOp(ConvolveOp.EDGE_NO_OP);
        try {
            op.filter(src, dst);
        } catch (ImagingOpException e) {
            /*
             * The test pair of source and destination rasters
             * may cause failure of the medialib convolution routine,
             * so this exception is expected.
             *
             * The JVM crash is the only manifestation of this
             * test failure.
             */
        }
        System.out.println("Test PASSED.");
    }

    private static Raster createSrcRaster() {
        WritableRaster r = Raster.createInterleavedRaster(DataBuffer.TYPE_USHORT,
                w, h, 4, new Point(0, 0));

        return r;
    }

    private static WritableRaster createDstRaster() {
        WritableRaster r = Raster.createInterleavedRaster(DataBuffer.TYPE_BYTE,
                w, h, 4, new Point(0, 0));

        return r;
    }

    private static ConvolveOp createConvolveOp(int edgeHint) {
        final int kw = 3;
        final int kh = 3;
        float[] kdata = new float[kw * kh];
        float v = 1f / kdata.length;
        Arrays.fill(kdata, v);

        Kernel k = new Kernel(kw, kh, kdata);
        ConvolveOp op = new ConvolveOp(k, edgeHint, null);

        return op;
    }
}
