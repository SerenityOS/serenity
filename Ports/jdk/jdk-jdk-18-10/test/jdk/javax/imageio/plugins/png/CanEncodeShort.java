/*
 * Copyright (c) 2002, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4474819
 * @summary Tests whether the PNGImageWriterSpi advertises that it is capable of
 *          writing images of TYPE_USHORT_565_RGB and TYPE_USHORT_555_RGB. The
 *          test fails if an exception is thrown.
 */

import java.awt.image.BufferedImage;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;

public class CanEncodeShort {

    private static final int[] types = new int[] {
        BufferedImage.TYPE_USHORT_565_RGB,
        BufferedImage.TYPE_USHORT_555_RGB,
    };

    private static final String[] typeNames = new String[] {
        "TYPE_USHORT_565_RGB",
        "TYPE_USHORT_555_RGB",
    };

    public static void main(String[] args) {
        for (int i = 0; i < types.length; i++) {
            BufferedImage img = new BufferedImage(32, 32, types[i]);

            ImageTypeSpecifier spec =
                ImageTypeSpecifier.createFromRenderedImage(img);

            Iterator writers = ImageIO.getImageWriters(spec, "png");

            if (!writers.hasNext()) {
                throw new RuntimeException("Test failed: " +
                                           "no PNG writer found for type " +
                                           typeNames[i]);
            }
        }
    }
}
