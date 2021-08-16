/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     8074967
 * @summary Test verifies if there is no JFIF & EXIF header
 *          and sampling factor is same of JPEG image, then
 *          JPEG colorspace should not be RGB.
 * @run     main JpegMetadataColorSpaceTest
 */

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataFormatImpl;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;

public class JpegMetadataColorSpaceTest {
    public static void main(String[] args) throws IOException {
        String fileName = "nomarkers.jpg";
        String sep = System.getProperty("file.separator");
        String dir = System.getProperty("test.src", ".");
        String filePath = dir+sep+fileName;
        System.out.println("Test file: " + filePath);
        File file = new File(filePath);
        ImageInputStream stream = ImageIO.createImageInputStream(file);
        Iterator<ImageReader> readers = ImageIO.getImageReaders(stream);

        if(readers.hasNext()) {
            ImageReader reader = readers.next();
            reader.setInput(stream);
            IIOMetadata metadata = reader.getImageMetadata(0);

            IIOMetadataNode standardTree = (IIOMetadataNode)
                metadata.getAsTree
                (IIOMetadataFormatImpl.standardMetadataFormatName);
            IIOMetadataNode colorSpaceType = (IIOMetadataNode)
                standardTree.getElementsByTagName("ColorSpaceType").item(0);
            String colorSpaceName = colorSpaceType.getAttribute("name");
            if(colorSpaceName.equals("RGB"))
                throw new RuntimeException("Identified incorrect ColorSpace");
        }
    }
}
