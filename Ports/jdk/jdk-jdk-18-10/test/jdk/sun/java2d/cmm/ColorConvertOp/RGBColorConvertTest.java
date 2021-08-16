/*
 * Copyright (c) 2006, 2007, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6279846
 * @summary Verifies that transform between the same ICC color spaces does not
 * change pixels
 */

import java.awt.image.*;
import java.awt.color.ColorSpace;

public class RGBColorConvertTest {

    public static void main(String [] args) {
        BufferedImage src =
            new BufferedImage(256,3,BufferedImage.TYPE_INT_RGB);
        BufferedImage dst =
            new BufferedImage(256,3,BufferedImage.TYPE_INT_RGB);

        for (int i = 0; i < 256; i++) {
            src.setRGB(i,0,i);
            src.setRGB(i,1,i << 8);
            src.setRGB(i,2,i << 16);
        }

        ColorSpace srcColorSpace = src.getColorModel().getColorSpace();

        ColorConvertOp op = new ColorConvertOp(srcColorSpace, srcColorSpace,
                                               null);
        op.filter(src, dst);

        int errCount = 0;
        for (int i = 0; i < src.getWidth(); i++) {
            for (int j = 0; j < src.getHeight(); j++) {
                int scol = src.getRGB(i,j);
                int dcol = dst.getRGB(i,j);
                if (scol != dcol) {
                    System.err.println("(" + i + "," + j + ") : " +
                                       Integer.toHexString(scol) + "!=" +
                                       Integer.toHexString(dcol));
                    errCount++;
                }
            }
        }

        if (errCount > 0) {
            throw new RuntimeException(errCount + " pixels are changed by " +
                                       "transform between the same ICC color " +
                                       "spaces");
        }
    }
}
