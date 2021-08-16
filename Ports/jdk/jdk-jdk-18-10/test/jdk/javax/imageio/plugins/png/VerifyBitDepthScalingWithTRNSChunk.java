/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     8211795
 * @summary Test verifies that PNGImageReader maintains proper
 *          number of bands for scale array when PNG image
 *          has tRNS chunk.
 * @run     main VerifyBitDepthScalingWithTRNSChunk
 */

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.Color;
import java.awt.image.IndexColorModel;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriter;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.metadata.IIOInvalidTreeException;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageOutputStream;
import javax.imageio.IIOImage;

public class VerifyBitDepthScalingWithTRNSChunk {

    private static BufferedImage img;
    private static ImageWriter writer;
    private static ImageWriteParam param;
    private static IIOMetadata metadata;
    private static byte[] imageByteArray;

    private static void initialize(int type) {
        int width = 1;
        int height = 1;
        // create Palette & IndexColorModel for bitdepth 1
        int size = 2;
        int bitDepth = 1;
        byte[] r = new byte[size];
        byte[] g = new byte[size];
        byte[] b = new byte[size];

        r[0] = g[0] = b[0] = 0;
        r[1] = g[1] = b[1] = (byte)255;

        IndexColorModel cm = new IndexColorModel(bitDepth, size, r, g, b);
        img = new BufferedImage(width, height, type, cm);
        Graphics2D g2D = img.createGraphics();
        g2D.setColor(new Color(255, 255, 255));
        g2D.fillRect(0, 0, width, height);

        Iterator<ImageWriter> iterWriter =
                ImageIO.getImageWritersBySuffix("png");
        writer = iterWriter.next();

        param = writer.getDefaultWriteParam();
        ImageTypeSpecifier specifier =
                ImageTypeSpecifier.
                        createFromBufferedImageType(type);
        metadata = writer.getDefaultImageMetadata(specifier, param);
    }

    private static void createTRNSNode(String tRNS_value)
            throws IIOInvalidTreeException {
        IIOMetadataNode tRNS_gray = new IIOMetadataNode("tRNS_Grayscale");
        tRNS_gray.setAttribute("gray", tRNS_value);

        IIOMetadataNode tRNS = new IIOMetadataNode("tRNS");
        tRNS.appendChild(tRNS_gray);
        IIOMetadataNode root = new IIOMetadataNode("javax_imageio_png_1.0");
        root.appendChild(tRNS);
        metadata.mergeTree("javax_imageio_png_1.0", root);
    }

    private static void writeImage() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageOutputStream ios = ImageIO.createImageOutputStream(baos);
        writer.setOutput(ios);
        writer.write(metadata, new IIOImage(img, null, metadata), param);
        writer.dispose();

        baos.flush();
        imageByteArray = baos.toByteArray();
        baos.close();
    }

    private static void verifyBitDepthScalingWithTRNSChunk()
        throws IOException {
        initialize(BufferedImage.TYPE_BYTE_BINARY);
        // Create tRNS node with some value and merge it with default metadata
        createTRNSNode("255");

        writeImage();

        InputStream input= new ByteArrayInputStream(imageByteArray);
        /*
         * Read 1 bit PNG Gray image with tRNS chunk.
         * Since bitDepth is 1 there will be scaling of each channel,
         * and it has tRNS chunk for which we will add extra alpha channel.
         * This will result in creation of scale array in PNGImageReader.
         */
        ImageIO.read(input);
        input.close();
    }

    public static void main(String[] args) throws IOException {
        verifyBitDepthScalingWithTRNSChunk();
    }
}

