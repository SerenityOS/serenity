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

/**
 * @test
 * @bug 8164750
 * @summary Verify reader does not fail when the BaselineTIFFTagSet is
 *          removed via the TIFFImageReadParam both when ignoring and
 *          not ignoring metadata.
 */

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.plugins.tiff.BaselineTIFFTagSet;
import javax.imageio.plugins.tiff.TIFFImageReadParam;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;

public class ReadWithoutBaselineTagSet {
    private static final int WIDTH = 47;
    private static final int HEIGHT = 53;

    private static final Map<Integer,String[]> typeToCompression =
        Map.of(BufferedImage.TYPE_3BYTE_BGR,
            new String[] {null, "LZW", "JPEG", "ZLib", "PackBits"},
            BufferedImage.TYPE_BYTE_BINARY,
            new String[] {null, "CCITT RLE", "CCITT T.4", "CCITT T.6",
                "LZW", "PackBits"},
            BufferedImage.TYPE_BYTE_GRAY,
            new String[] {null, "LZW", "JPEG", "ZLib", "PackBits"},
            BufferedImage.TYPE_USHORT_GRAY,
            new String[] {null, "LZW", "ZLib", "PackBits"});

    public static void main(String[] args) throws IOException {
        test();
    }

    private static void test() throws IOException {
        int failures = 0;

        for (int imageType : typeToCompression.keySet()) {
            BufferedImage image = new BufferedImage(WIDTH, HEIGHT, imageType);
            System.out.println("Image: " + image.toString());

            for (String compression : typeToCompression.get(imageType)) {
                System.out.println("Compression: "
                        + (compression == null ? "None" : compression));
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                ImageOutputStream ios = new MemoryCacheImageOutputStream(output);
                ImageWriter writer =
                        ImageIO.getImageWritersByFormatName("TIFF").next();
                ImageWriteParam wparam = writer.getDefaultWriteParam();
                if (compression == null) {
                    wparam.setCompressionMode(ImageWriteParam.MODE_DEFAULT);
                } else {
                    wparam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                    wparam.setCompressionType(compression);
                }
                writer.setOutput(ios);
                writer.write(null, new IIOImage(image, null, null), wparam);
                ios.flush();

                ImageReader reader =
                        ImageIO.getImageReadersByFormatName("TIFF").next();
                ByteArrayInputStream input
                        = new ByteArrayInputStream(output.toByteArray());
                ImageInputStream iis = new MemoryCacheImageInputStream(input);
                iis.mark();

                TIFFImageReadParam rparam = new TIFFImageReadParam();
                rparam.removeAllowedTagSet(BaselineTIFFTagSet.getInstance());

                reader.setInput(iis, false, false);
                BufferedImage resultFalse = reader.read(0, rparam);
                if (resultFalse.getWidth() != WIDTH
                        || resultFalse.getHeight() != HEIGHT) {
                    System.err.printf("Read image dimensions != %d x %d%n",
                            WIDTH, HEIGHT);
                    failures++;
                } else {
                    System.out.println("ignoreMetadata == false test passed");
                }

                iis.reset();
                reader.setInput(iis, false, true);
                BufferedImage resultTrue;
                try {
                    resultTrue = reader.read(0, rparam);
                    if (resultTrue.getWidth() != WIDTH
                            || resultTrue.getHeight() != HEIGHT) {
                        System.err.printf("Read image dimensions != %d x %d%n",
                                WIDTH, HEIGHT);
                        failures++;
                    } else {
                        System.out.println("ignoreMetadata == true test passed");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    failures++;
                }
            }
        }

        if (failures != 0) {
            throw new RuntimeException("Test failed with "
                    + failures + " errors!");
        }
    }
}
