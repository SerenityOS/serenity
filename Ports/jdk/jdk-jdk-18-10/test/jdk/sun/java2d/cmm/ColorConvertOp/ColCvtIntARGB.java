/*
 * Copyright (c) 2001, 2007, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4425837
 * @summary Test color conversion for TYPE_INT_ARGB images
 */

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;


public class ColCvtIntARGB {

    public static void main(String args[]) {
        //
        // Build a 1 pixel ARGB, non-premultiplied image
        //
        BufferedImage src
            = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);

        // Set src pixel value
        Color pelColor = new Color(100, 100, 100, 128);
        src.setRGB(0, 0, pelColor.getRGB());

        //
        // ColorConvertOp filter
        //
        ColorConvertOp op = new ColorConvertOp(null);

        //
        // Test ARGB -> ARGB_PRE
        //
        BufferedImage dst
            = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB_PRE);
        op.filter(src, dst);
        if (((dst.getRGB(0, 0) >> 24) & 0xff) != 128) {
            throw new RuntimeException("Incorrect destination alpha value.");
        }

        //
        // Test ARGB -> ARGB
        //
        dst = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        op.filter(src, dst);
        if (((dst.getRGB(0, 0) >> 24) & 0xff) != 128) {
            throw new RuntimeException("Incorrect destination alpha value.");
        }
    }
}
