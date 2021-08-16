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
 * @bug     6788458
 * @summary Test verifies that PNGImageReader takes tRNS chunk values
 *          into consideration while reading non-indexed Gray PNG images.
 * @run     main ReadPngGrayImageWithTRNSChunk
 */

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.Color;
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

public class ReadPngGrayImageWithTRNSChunk {

    private static BufferedImage img;
    private static ImageWriter writer;
    private static ImageWriteParam param;
    private static IIOMetadata metadata;
    private static byte[] imageByteArray;

    private static void initialize(int type) {
        int width = 2;
        int height = 1;
        img = new BufferedImage(width, height, type);
        Graphics2D g2D = img.createGraphics();

        // transparent first pixel
        g2D.setColor(new Color(255, 255, 255));
        g2D.fillRect(0, 0, 1, 1);
        // non-transparent second pixel
        g2D.setColor(new Color(128, 128,128));
        g2D.fillRect(1, 0, 1, 1);
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

    private static boolean verifyAlphaValue(BufferedImage img) {
        Color firstPixel = new Color(img.getRGB(0, 0), true);
        Color secondPixel = new Color(img.getRGB(1, 0), true);

        return firstPixel.getAlpha() != 0 ||
        secondPixel.getAlpha() != 255;
    }

    private static boolean read8BitGrayPNGWithTRNSChunk() throws IOException {
        initialize(BufferedImage.TYPE_BYTE_GRAY);
        // Create tRNS node and merge it with default metadata
        createTRNSNode("255");

        writeImage();

        InputStream input= new ByteArrayInputStream(imageByteArray);
        // Read 8 bit PNG Gray image with tRNS chunk
        BufferedImage verify_img = ImageIO.read(input);
        input.close();
        // Verify alpha values present in first & second pixel
        return verifyAlphaValue(verify_img);
    }

    private static boolean read16BitGrayPNGWithTRNSChunk() throws IOException {
        initialize(BufferedImage.TYPE_USHORT_GRAY);
        // Create tRNS node and merge it with default metadata
        createTRNSNode("65535");

        writeImage();

        InputStream input= new ByteArrayInputStream(imageByteArray);
        // Read 16 bit PNG Gray image with tRNS chunk
        BufferedImage verify_img = ImageIO.read(input);
        input.close();
        // Verify alpha values present in first & second pixel
        return verifyAlphaValue(verify_img);
    }

    public static void main(String[] args) throws IOException {
        boolean read8BitFail, read16BitFail;
        // read 8 bit PNG Gray image with tRNS chunk
        read8BitFail = read8BitGrayPNGWithTRNSChunk();

        // read 16 bit PNG Gray image with tRNS chunk
        read16BitFail = read16BitGrayPNGWithTRNSChunk();

        if (read8BitFail || read16BitFail) {
            throw new RuntimeException("PNGImageReader is not using" +
            " transparent pixel information from tRNS chunk properly");
        }
    }
}

