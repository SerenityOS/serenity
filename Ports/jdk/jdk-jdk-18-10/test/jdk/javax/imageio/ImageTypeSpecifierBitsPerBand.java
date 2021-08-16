/*
 * Copyright (c) 2000, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4392669
 * @summary Checks contract of ImageTypeSpecifier.getBitsPerBand
 */

import java.awt.image.BufferedImage;

import javax.imageio.ImageTypeSpecifier;

public class ImageTypeSpecifierBitsPerBand {

    public static void main(String[] args) {
        int biType = BufferedImage.TYPE_USHORT_565_RGB;
        ImageTypeSpecifier type =
            ImageTypeSpecifier.createFromBufferedImageType(biType);

        int b0 = type.getBitsPerBand(0);
        int b1 = type.getBitsPerBand(1);
        int b2 = type.getBitsPerBand(2);

        if (b0 != 5 || b1 != 6 || b2 != 5) {
            throw new RuntimeException("Got incorrect bits per band value!");
        }

        boolean gotIAE = false;
        try {
            int b3 = type.getBitsPerBand(3);
        } catch (IllegalArgumentException e) {
            gotIAE = true;
        }
        if (!gotIAE) {
            throw new RuntimeException
                ("Failed to get IllegalArgumentException for band == 3!");
        }
    }
}
