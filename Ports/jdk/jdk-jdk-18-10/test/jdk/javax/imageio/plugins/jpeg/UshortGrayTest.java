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
 * @bug 4450894
 * @summary Tests if the JPEGImageWriter allows images with > 8-bit samples to
 *          be written. Also tests the JPEGImageWriterSpi.canEncodeImage()
 *          mechanism for this same behavior.
 */

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

public class UshortGrayTest {

    public static void main(String[] args) {
        Iterator iter;
        BufferedImage bi = new BufferedImage(10, 10,
                                             BufferedImage.TYPE_USHORT_GRAY);

        // Part 1: ensure that JPEGImageWriter throws an exception if it
        // encounters an image with 16-bit samples
        ImageWriter writer = null;
        iter = ImageIO.getImageWritersByFormatName("jpeg");
        if (iter.hasNext()) {
            writer = (ImageWriter)iter.next();
        } else {
            throw new RuntimeException("No JPEG reader found");
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageOutputStream ios = null;
        boolean exceptionThrown = false;

        try {
            ios = ImageIO.createImageOutputStream(baos);
        } catch (IOException ioe) {
            throw new RuntimeException("Could not create ImageOutputStream");
        }

        try {
            writer.setOutput(ios);
            writer.write(bi);
        } catch (IOException ioe) {
            exceptionThrown = true;
        }

        if (!exceptionThrown) {
            throw new RuntimeException("JPEG writer should not be able to " +
                                       "write USHORT_GRAY images");
        }

        // Part 2: ensure that JPEGImageWriterSpi.canEncodeImage() returns
        // false for images with 16-bit samples
        ImageTypeSpecifier its =
            ImageTypeSpecifier.createFromRenderedImage(bi);

        iter = ImageIO.getImageWriters(its, "jpeg");
        if (iter.hasNext()) {
            throw new RuntimeException("JPEG writer should not be available" +
                                       " for USHORT_GRAY images");
        }
    }
}
