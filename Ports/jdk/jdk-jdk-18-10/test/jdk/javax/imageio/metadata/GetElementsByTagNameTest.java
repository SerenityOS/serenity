/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     8167281
 * @summary Test verifies that Element.getElementsByTagName("*") is not empty
 *          for valid image.
 * @run     main GetElementsByTagNameTest
 */

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataFormatImpl;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;
import org.w3c.dom.Element;

public class GetElementsByTagNameTest {

    public static void main(String[] args) throws IOException {
        // Generate some trivial image and save it to a temporary array
        ByteArrayOutputStream tmp = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB),
                "gif", tmp);

        // Read the stream
        ImageInputStream in = new MemoryCacheImageInputStream(
                new ByteArrayInputStream(tmp.toByteArray()));
        ImageReader reader = ImageIO.getImageReaders(in).next();
        reader.setInput(in);

        // Retrieve standard image metadata tree
        IIOMetadata meta = reader.getImageMetadata(0);
        if (meta == null || !meta.isStandardMetadataFormatSupported()) {
            throw new Error("Test failure: Missing metadata");
        }
        Element root = (Element) meta.
                getAsTree(IIOMetadataFormatImpl.standardMetadataFormatName);

        // Test getElementsByTagName("*")
        if (root.getElementsByTagName("*").getLength() == 0) {
            throw new RuntimeException("getElementsByTagName(\"*\") returns"
                    + " nothing");
        }
    }
}

