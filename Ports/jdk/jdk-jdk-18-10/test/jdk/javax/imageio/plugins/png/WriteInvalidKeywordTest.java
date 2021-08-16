/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     8242557
 * @summary Test verifies that PNGImageWriter does not write
 *          longer than 79 length null terminated strings.
 * @run     main WriteInvalidKeywordTest
 */

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import javax.imageio.*;
import javax.imageio.metadata.IIOInvalidTreeException;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageOutputStream;

public class WriteInvalidKeywordTest {

    private static BufferedImage img;
    private static ImageWriter writer;
    private static ImageWriteParam param;
    private static IIOMetadata metadata;

    private static void initialize(int type) {
        int width = 1;
        int height = 1;
        img = new BufferedImage(width, height, type);
        Graphics2D g2D = img.createGraphics();
        g2D.setColor(new Color(255, 255, 255));
        g2D.fillRect(0, 0, width, width);
        g2D.dispose();

        Iterator<ImageWriter> iterWriter =
                ImageIO.getImageWritersBySuffix("png");
        writer = iterWriter.next();

        param = writer.getDefaultWriteParam();
        ImageTypeSpecifier specifier =
                ImageTypeSpecifier.
                        createFromBufferedImageType(type);
        metadata = writer.getDefaultImageMetadata(specifier, param);
    }

    private static void createTEXTNode()
            throws IIOInvalidTreeException {
        IIOMetadataNode tEXt_Entry = new IIOMetadataNode("tEXtEntry");
        // Keyword length greater than 79
        tEXt_Entry.setAttribute("keyword", "Authored" +
                "AuthoredAuthoredAuthoredAuthoredAuthoredAuthored" +
                "AuthoredAuthoredAuthoredAuthored");
        tEXt_Entry.setAttribute("value", "");

        IIOMetadataNode tEXt = new IIOMetadataNode("tEXt");
        tEXt.appendChild(tEXt_Entry);
        IIOMetadataNode root = new IIOMetadataNode("javax_imageio_png_1.0");
        root.appendChild(tEXt);
        metadata.mergeTree("javax_imageio_png_1.0", root);
    }

    private static void writeImage() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageOutputStream ios = ImageIO.createImageOutputStream(baos);
        writer.setOutput(ios);
        writer.write(metadata, new IIOImage(img, null, metadata), param);
        writer.dispose();
        baos.close();
        ios.close();
    }

    private static void writePNGTEXTChunk() throws IOException {
        initialize(BufferedImage.TYPE_BYTE_GRAY);
        createTEXTNode();
        writeImage();
    }

    public static void main(String[] args) throws IOException {
        // write PNG image with tEXT chunk having keyword length
        // greater than 79.
        boolean failed = true;
        try {
            writePNGTEXTChunk();
        } catch (IIOException e) {
            // we expect it to throw IIOException
            if (e.getCause().getMessage() ==
                    "tEXt keyword is longer than 79") {
                failed = false;
            }
        }
        if (failed) {
            throw new RuntimeException("Test failed, did not throw " +
                    "expected exception");
        }
    }
}

