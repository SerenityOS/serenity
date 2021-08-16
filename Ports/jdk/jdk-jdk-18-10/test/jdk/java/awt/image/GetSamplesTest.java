/*
 * Copyright (c) 2010, 2011, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     6735275 6993561
 * @summary Test verifies that SampleModel.getSamples() SampleModel.setSamples()
 *          throw an appropriate exception if coordinates are not in bounds.
 *
 * @run     main GetSamplesTest
 */

import java.awt.image.BandedSampleModel;
import java.awt.image.ComponentSampleModel;
import java.awt.image.DataBuffer;
import java.awt.image.MultiPixelPackedSampleModel;
import java.awt.image.PixelInterleavedSampleModel;
import java.awt.image.SampleModel;
import java.awt.image.SinglePixelPackedSampleModel;
import java.util.Vector;

public class GetSamplesTest {

    public static int width = 100;
    public static int height = 100;
    public static int dataType = DataBuffer.TYPE_BYTE;
    public static int numBands = 4;

    public static void main(String[] args) {
        Vector<Class<? extends SampleModel>> classes = new Vector<Class<? extends SampleModel>>();

        classes.add(ComponentSampleModel.class);
        classes.add(MultiPixelPackedSampleModel.class);
        classes.add(SinglePixelPackedSampleModel.class);
        classes.add(BandedSampleModel.class);
        classes.add(PixelInterleavedSampleModel.class);

        for (Class<? extends SampleModel> c : classes) {
            doTest(c);
        }
    }
    private static void doTest(Class<? extends SampleModel> c) {
        System.out.println("Test for: " + c.getName());
        SampleModel sm = createSampleModel(c);

        DataBuffer db = sm.createDataBuffer();

        int[] iArray = new int[ width * height + numBands];
        float[] fArray = new float[ width * height + numBands];
        double[] dArray = new double[ width * height + numBands];

        boolean iOk = false;
        boolean fOk = false;
        boolean dOk = false;

        try {
            sm.getSamples(Integer.MAX_VALUE, 0, 1, 1, 0, iArray, db);
            sm.setSamples(Integer.MAX_VALUE, 0, 1, 1, 0, iArray, db);
        } catch (ArrayIndexOutOfBoundsException e) {
            System.out.println(e.getMessage());
            iOk = true;
        }

        try {
            sm.getSamples(Integer.MAX_VALUE, 0, 1, 1, 0, fArray, db);
            sm.setSamples(Integer.MAX_VALUE, 0, 1, 1, 0, fArray, db);
        } catch (ArrayIndexOutOfBoundsException e) {
            System.out.println(e.getMessage());
            fOk = true;
        }

        try {
            sm.getSamples(0, Integer.MAX_VALUE, 1, 1, 0, dArray, db);
            sm.setSamples(0, Integer.MAX_VALUE, 1, 1, 0, dArray, db);
        } catch (ArrayIndexOutOfBoundsException e) {
            System.out.println(e.getMessage());
            dOk = true;
        }
        if (!iOk || !fOk || !dOk) {
            throw new RuntimeException("Test for " + c.getSimpleName() +
                    " failed: iOk=" + iOk + "; fOk=" + fOk + "; dOk=" + dOk);
        }
    }

    private static SampleModel createSampleModel(Class<? extends SampleModel> cls) {
        SampleModel res = null;

        if (cls == ComponentSampleModel.class) {
            res = new ComponentSampleModel(dataType, width, height, 4, width * 4, new int[] { 0, 1, 2, 3 } );
        } else if (cls == MultiPixelPackedSampleModel.class) {
            res = new MultiPixelPackedSampleModel(dataType, width, height, 4);
        } else if (cls == SinglePixelPackedSampleModel.class) {
            res = new SinglePixelPackedSampleModel(dataType, width, height,
                    new int[]{ 0xff000000, 0x00ff0000, 0x0000ff00, 0x000000ff });
        } else if (cls == BandedSampleModel.class) {
            res = new BandedSampleModel(dataType, width, height, numBands);
        } else if (cls == PixelInterleavedSampleModel.class) {
            res = new PixelInterleavedSampleModel(dataType, width, height, 4, width * 4, new int[] { 0, 1, 2, 3 });
        } else {
            throw new RuntimeException("Unknown class " + cls);
        }
        return res;
    }
}
