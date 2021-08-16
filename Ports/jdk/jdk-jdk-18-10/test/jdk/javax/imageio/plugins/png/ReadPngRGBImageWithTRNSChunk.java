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
 *          into consideration while reading non-indexed RGB PNG images.
 * @run     main ReadPngRGBImageWithTRNSChunk
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
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferUShort;
import java.awt.image.WritableRaster;
import java.awt.image.Raster;
import java.awt.color.ColorSpace;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.Transparency;

public class ReadPngRGBImageWithTRNSChunk {

    private static BufferedImage img;
    private static IIOMetadata metadata;
    private static ImageWriteParam param;
    private static ImageWriter writer;
    private static byte[] imageByteArray;

    private static void createTRNSNode(String tRNS_value)
        throws IIOInvalidTreeException {
        IIOMetadataNode tRNS_rgb = new IIOMetadataNode("tRNS_RGB");
        tRNS_rgb.setAttribute("red", tRNS_value);
        tRNS_rgb.setAttribute("green", tRNS_value);
        tRNS_rgb.setAttribute("blue", tRNS_value);

        IIOMetadataNode tRNS = new IIOMetadataNode("tRNS");
        tRNS.appendChild(tRNS_rgb);
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

    private static boolean read8BitRGBPNGWithTRNSChunk() throws IOException {
        int width = 2;
        int height = 1;
        // Create 8 bit PNG image
        img = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
        Graphics2D g2D = img.createGraphics();

        // transparent first pixel
        g2D.setColor(Color.WHITE);
        g2D.fillRect(0, 0, 1, 1);
        // non-transparent second pixel
        g2D.setColor(Color.RED);
        g2D.fillRect(1, 0, 1, 1);
        g2D.dispose();

        Iterator<ImageWriter> iterWriter =
            ImageIO.getImageWritersBySuffix("png");
        writer = iterWriter.next();

        param = writer.getDefaultWriteParam();
        ImageTypeSpecifier specifier =
            ImageTypeSpecifier.
                createFromBufferedImageType(BufferedImage.TYPE_3BYTE_BGR);
        metadata = writer.getDefaultImageMetadata(specifier, param);

        // Create tRNS node and merge it with default metadata
        createTRNSNode("255");

        writeImage();

        InputStream input= new ByteArrayInputStream(imageByteArray);
        // Read 8 bit PNG RGB image with tRNS chunk
        BufferedImage verify_img = ImageIO.read(input);
        input.close();
        // Verify alpha values present in first & second pixel
        return verifyAlphaValue(verify_img);
    }

    private static boolean read16BitRGBPNGWithTRNSChunk() throws IOException {
        // Create 16 bit PNG image
        int height = 1;
        int width = 2;
        int numBands = 3;
        int shortArrayLength = width * height * numBands;
        short[] pixelData = new short[shortArrayLength];
        // transparent first pixel
        pixelData[0] = (short)0xffff;
        pixelData[1] = (short)0xffff;
        pixelData[2] = (short)0xffff;
        // non-transparent second pixel
        pixelData[3] = (short)0xffff;
        pixelData[4] = (short)0xffff;
        pixelData[5] = (short)0xfffe;

        DataBuffer buffer = new DataBufferUShort(pixelData, shortArrayLength);

        int[] bandOffset = {0, 1 ,2};
        WritableRaster ras =
            Raster.createInterleavedRaster(buffer, width, height,
            width * numBands, numBands, bandOffset, null);

        int nBits[] = {16, 16 ,16};
        ColorModel colorModel = new
            ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB),
                                nBits, false, false, Transparency.OPAQUE,
                                DataBuffer.TYPE_USHORT);
        img = new BufferedImage(colorModel, ras, false, null);

        Iterator<ImageWriter> iterWriter =
            ImageIO.getImageWritersBySuffix("png");
        writer = iterWriter.next();

        param = writer.getDefaultWriteParam();
        ImageTypeSpecifier specifier = new ImageTypeSpecifier(img);
        metadata = writer.getDefaultImageMetadata(specifier, param);

        // Create tRNS node and merge it with default metadata
        createTRNSNode("65535");

        writeImage();

        InputStream input= new ByteArrayInputStream(imageByteArray);
        // Read 16 bit PNG RGB image with tRNS chunk
        BufferedImage verify_img = ImageIO.read(input);
        input.close();
        // Verify alpha values present in first & second pixel
        return verifyAlphaValue(verify_img);
    }

    public static void main(String[] args) throws IOException {
        boolean read8BitFail, read16BitFail;
        // read 8 bit PNG RGB image with tRNS chunk
        read8BitFail = read8BitRGBPNGWithTRNSChunk();

        // read 16 bit PNG RGB image with tRNS chunk
        read16BitFail = read16BitRGBPNGWithTRNSChunk();

        if (read8BitFail || read16BitFail) {
            throw new RuntimeException("PNGImageReader is not using" +
            " transparent pixel information from tRNS chunk properly");
        }
    }
}

