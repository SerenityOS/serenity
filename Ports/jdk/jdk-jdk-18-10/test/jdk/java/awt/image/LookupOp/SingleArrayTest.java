/*
 * Copyright (c) 2007, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     4886506
 * @summary Test verifies that byte lookup table with single lookup array
 *          does not cause medialib routine crsh.
 * @run     main SingleArrayTest
 */

import java.awt.image.BufferedImage;
import java.awt.image.ByteLookupTable;
import java.awt.image.LookupOp;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;

public class SingleArrayTest {
    public static void main(String[] args) {
        SingleArrayTest t = new SingleArrayTest();
        t.doTest(BufferedImage.TYPE_3BYTE_BGR);
        t.doTest(BufferedImage.TYPE_4BYTE_ABGR);
        t.doTest(BufferedImage.TYPE_INT_RGB);
        t.doTest(BufferedImage.TYPE_INT_ARGB);
        t.doTest(BufferedImage.TYPE_INT_BGR);
        t.doTest(BufferedImage.TYPE_BYTE_GRAY);
    }

    private LookupOp op;

    public SingleArrayTest() {

        byte[] array = new byte[256];
        for (int i = 0; i < 256; i++) {
            array[i] = (byte)i;
        }
        ByteLookupTable table = new ByteLookupTable(0, array);

        op = new LookupOp(table, null);
    }

    public void doTest(int bi_type) {
        System.out.println("Test for type: " + bi_type);
        BufferedImage src = new BufferedImage(2, 2, bi_type);

        BufferedImage dst = new BufferedImage(2, 2, bi_type);

        doTest(src.getData(), dst.getRaster());

        doTest(src, dst);

        System.out.println("Test passed.");
    }

    public void doTest(Raster src, WritableRaster dst) {
        System.out.println("Test for raster:" + src);
        try {
            dst = op.filter(src, dst);
        } catch (Exception e) {
            throw new RuntimeException("Test failed.", e);
        }
    }

    public void doTest(BufferedImage src, BufferedImage dst) {
        System.out.println("Test for image: " + src);
        try {
            dst = op.filter(src, dst);
        } catch (Exception e) {
            throw new RuntimeException("Test failed.", e);
        }
    }
}
