/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     8004801
 * @summary Test verifies that byte lookup table with single lookup
 *          affects only color components in buffered images with
 *          integer data type, and that this operation does not distort
 *          colors in the destination image.
 * @run     main IntImageReverseTest
 */

import java.awt.image.BufferedImage;
import java.awt.image.ByteLookupTable;
import java.awt.image.LookupOp;
import java.awt.image.LookupTable;

public class IntImageReverseTest {

    public static void main(String[] args) {
        LookupTable tbl = createReverseTable();
        LookupOp op = new LookupOp(tbl, null);

        for (ImageType t : ImageType.values()) {
            System.out.print(t);

            BufferedImage src = createSourceImage(t);

            BufferedImage dst = op.filter(src, null);

            int rgb = dst.getRGB(0, 0);

            System.out.printf(" Result: 0x%X ", rgb);

            if (rgb != argbReverse) {
                throw new RuntimeException("Test failed.");
            }
            System.out.println("Passed.");
        }
    }

    /**
     * Reverse image color components, leave alpha unchanged.
     */
    private static LookupTable createReverseTable() {
        byte[] data = new byte[256];

        for (int i = 0; i < 256; i++) {
            data[i] = (byte) (255 - i);
        }


        return new ByteLookupTable(0, data);
    }

    private static BufferedImage createSourceImage(ImageType type) {
        BufferedImage img = new BufferedImage(1, 1, type.bi_type);

        img.setRGB(0, 0, argbTest);

        return img;
    }
    private static final int argbTest = 0xFFDDAA77;
    private static final int argbReverse = 0xFF225588;

    private static enum ImageType {

        INT_ARGB(BufferedImage.TYPE_INT_ARGB),
        INT_ARGB_PRE(BufferedImage.TYPE_INT_ARGB_PRE),
        INT_RGB(BufferedImage.TYPE_INT_BGR),
        INT_BGR(BufferedImage.TYPE_INT_BGR);

        private ImageType(int bi_type) {
            this.bi_type = bi_type;
        }
        public final int bi_type;
    }
}
