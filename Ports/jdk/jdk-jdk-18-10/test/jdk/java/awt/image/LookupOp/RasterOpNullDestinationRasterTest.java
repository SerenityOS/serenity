/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6427331
 * @summary Test to check there is no NullPointerException raised if
 * null raster is provided as destination in case of RasterOp.filter()
 * @run main RasterOpNullDestinationRasterTest
 */

import java.awt.Point;
import java.awt.image.ByteLookupTable;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.LookupOp;
import java.awt.image.PixelInterleavedSampleModel;
import java.awt.image.Raster;
import java.awt.image.RasterOp;
import java.awt.image.SampleModel;

public class RasterOpNullDestinationRasterTest {

    public static void main(String[] args) {

        byte[][] data = new byte[1][10];
        ByteLookupTable lut = new ByteLookupTable(0, data);
        RasterOp op = new LookupOp(lut, null);

        int[] bandOffsets = {0};
        Point location = new Point(0, 0);
        DataBuffer db = new DataBufferByte(10 * 10);
        SampleModel sm = new PixelInterleavedSampleModel(DataBuffer.TYPE_BYTE,
                                                         10, 10, 1, 10,
                                                         bandOffsets);

        Raster src = Raster.createRaster(sm, db, location);

        op.filter(src, null); // this used to result in NullPointerException
    }
}
