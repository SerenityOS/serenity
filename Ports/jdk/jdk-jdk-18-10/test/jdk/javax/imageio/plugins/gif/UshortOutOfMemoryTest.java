/*
 * Copyright (c) 2005, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6294363
 * @summary Test verifies that creation of tree representation of the native
 *          image metadata for USHORT_GRAY images does not cause the
 *          OutOfMemoryError
 * @run main/othervm -Xms32M -Xmx32M UshortOutOfMemoryTest
 */

import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;

public class UshortOutOfMemoryTest {
    private int type;
    private ImageWriter w;

    public UshortOutOfMemoryTest(int type) {
        this.type = type;
        w = ImageIO.getImageWritersByFormatName("GIF").next();
    }

    public void testGetAsTree() {
        ImageWriteParam p = w.getDefaultWriteParam();
        IIOMetadata m =
            w.getDefaultImageMetadata(ImageTypeSpecifier.createFromBufferedImageType(type), p);

        String format = m.getNativeMetadataFormatName();
        System.out.println("native format: " + format);

        int count = 0;
        try {
            while (count < 100) {
                System.out.println(" test " + count++);
                m.getAsTree(format);
            }
        } catch (OutOfMemoryError e) {
            System.gc();
            throw new RuntimeException("Test failed. Number of performed operations: " + count, e);
        }
    }


    public static void main(String[] args) throws IOException {
        UshortOutOfMemoryTest t = new UshortOutOfMemoryTest(
                BufferedImage.TYPE_USHORT_GRAY);
        t.testGetAsTree();
    }
}
