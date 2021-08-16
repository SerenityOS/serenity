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
 * @bug 6287880
 * @summary Test verifies that default metadata for stream and image returned by
 *          GIFImageWriter can be modified by the tree representation
 */

import java.awt.image.BufferedImage;

import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOInvalidTreeException;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;

public class WriteMetadataTest {
    private static String format = "GIF";

    public static void main(String[] args) {
        ImageWriter w = ImageIO.getImageWritersByFormatName(format).next();
        if (w == null) {
            throw new RuntimeException("No available writers for format " + format);
        }
        ImageWriteParam p = w.getDefaultWriteParam();

        ImageTypeSpecifier t =
                ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_INT_RGB);

        IIOMetadata m = w.getDefaultImageMetadata(t, p);
        System.out.println("Default image metadata is " + m);
        testWritableMetadata(m);

        IIOMetadata sm = w.getDefaultStreamMetadata(p);
        System.out.println("Default stream metadata is " + sm);
        testWritableMetadata(sm);
    }

    public static void testWritableMetadata(IIOMetadata m) {
        String nativeFormatName =
                m.getNativeMetadataFormatName();
        System.out.println("Format: " + nativeFormatName);
        IIOMetadataNode root = (IIOMetadataNode)m.getAsTree(nativeFormatName);
        if (m.isReadOnly()) {
            throw new RuntimeException("Metadata is read only!");
        }
        try {
            m.setFromTree(nativeFormatName, root);
        } catch (IIOInvalidTreeException e) {
            e.printStackTrace();
            throw new RuntimeException("Test failed!", e);
        } catch (IllegalStateException e) {
            throw new RuntimeException("Test failed!", e);
        }
        System.out.println("Test passed.\n\n");
    }
}
