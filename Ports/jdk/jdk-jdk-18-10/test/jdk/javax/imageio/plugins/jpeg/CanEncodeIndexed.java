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
 * @bug 4528585
 * @summary Tests whether the JPEGImageWriterSpi advertises that it is capable
 *          of writing images using an IndexColorModel. The test fails if an
 *          exception is thrown.
 */

import java.awt.image.BufferedImage;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;

public class CanEncodeIndexed {

    public static void main(String[] args) {
        BufferedImage img = new BufferedImage(32, 32,
                                              BufferedImage.TYPE_BYTE_INDEXED);

        ImageTypeSpecifier spec =
            ImageTypeSpecifier.createFromRenderedImage(img);

        Iterator writers = ImageIO.getImageWriters(spec, "jpeg");

        if (!writers.hasNext()) {
            throw new RuntimeException("Test failed: " +
                                       "no JPEG writer found for " +
                                       "image with IndexColorModel");
        }
    }
}
