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
 * @bug  6185114
 * @summary test SampleModel constructor for different combinations of
 *          width and height
 */

import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.SampleModel;


public class SampleModelConstructorTest {

    public static void main(String[] a) throws RuntimeException {

        SampleModel model = Raster.createBandedRaster(DataBuffer.TYPE_INT,
                10, 5, 4, null).getSampleModel();

        final int inputWidths[]
                = {Integer.MIN_VALUE, -1000, -1, 0, 1, 1000, Integer.MAX_VALUE};

        final int inputHeights[]
                = {Integer.MIN_VALUE, -1000, -1, 0, 1, 1000, Integer.MAX_VALUE};

        // There are 49 combinations of (width, height) possible using above two
        // arrays

        // Only 6 valid combinations of (width, height) that do not throw
        // exception are :
        // (1, 1)
        // (1, 1000)
        // (1, Integer.MAX_VALUE)
        // (1000, 1)
        // (1000, 1000)
        // (Integer.MAX_VALUE, 1)
        final int expectedCount = 43;
        int count = 0;

        for (int i : inputWidths) {
            for (int j : inputHeights) {
                try {
                    SampleModel model2 = model.createCompatibleSampleModel(i, j);
                } catch (IllegalArgumentException e) {
                    count++;
                }
            }
        }

        if (count != expectedCount) {
            throw new RuntimeException(
                "Test Failed. Expected IllegalArgumentException Count = " +
                expectedCount + " Got Count = " + count);
        }
    }
}

