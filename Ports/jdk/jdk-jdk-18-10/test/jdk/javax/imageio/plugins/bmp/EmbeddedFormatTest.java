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
 * @bug 6294920 6294926
 * @summary Test verifies that BMP images with compression types BI_JPEG and
 *          BI_PNG are read correctly in case of 1, 8, 16, 24 and 32 bpp
 */

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;

public class EmbeddedFormatTest {
    ImageWriter writer;
    ImageReader reader;

    static int[] bi_types = {
        BufferedImage.TYPE_INT_RGB,
        BufferedImage.TYPE_3BYTE_BGR,
        BufferedImage.TYPE_USHORT_555_RGB,
        BufferedImage.TYPE_BYTE_GRAY,
        BufferedImage.TYPE_BYTE_BINARY
    };

    public EmbeddedFormatTest() {
        writer = ImageIO.getImageWritersByFormatName("BMP").next();
        reader = ImageIO.getImageReadersByFormatName("BMP").next();
    }

    public void doTest(String compression, int bi_type) throws IOException {
        System.out.println("Test " + compression + " on " + getImageTypeName(bi_type));
        BufferedImage src = createTestImage(bi_type);
        writer.reset();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageOutputStream ios =
                ImageIO.createImageOutputStream(baos);
        writer.setOutput(ios);

        ImageWriteParam wparam = prepareWriteParam(compression);
        writer.write(null, new IIOImage(src, null, null), wparam);
        ios.flush();
        ios.close();

        // read result
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        ImageInputStream iis = ImageIO.createImageInputStream(bais);
        reader.reset();
        reader.setInput(iis);

        BufferedImage dst = reader.read(0);

        checkResult(dst);
    }

    protected BufferedImage createTestImage(int type) {
        BufferedImage img = new BufferedImage(200, 200, type);
        Graphics g = img.createGraphics();
        g.setColor(Color.black);
        g.fillRect(0, 0, 200, 200);
        g.setColor(Color.white);
        g.fillRect(50, 50, 100, 100);

        return img;
    }

    protected void  checkResult(BufferedImage img) {
        int imgBlack = img.getRGB(25, 25);
        if (imgBlack != 0xff000000) {
            throw new RuntimeException("Wrong black color: " +
                    Integer.toHexString(imgBlack));
        }

        int imgWhite = img.getRGB(100, 100);
        if (imgWhite != 0xffffffff) {
            throw new RuntimeException("Wrong white color: " +
                    Integer.toHexString(imgWhite));
        }
    }

    protected ImageWriteParam prepareWriteParam(String compression) {
        ImageWriteParam imageWriteParam = writer.getDefaultWriteParam();
        imageWriteParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        imageWriteParam.setCompressionType(compression);
        return imageWriteParam;
    }


    public static void main(String[] args) throws IOException {
        EmbeddedFormatTest t = new EmbeddedFormatTest();

        for (int i = 0; i < bi_types.length; i++) {
            t.doTest("BI_JPEG", bi_types[i]);
            t.doTest("BI_PNG",  bi_types[i]);
        }
    }

    static String getImageTypeName(int type) {
        switch(type) {
            case BufferedImage.TYPE_INT_RGB:
                return "TYPE_INT_RGB";
            case BufferedImage.TYPE_3BYTE_BGR:
                return "TYPE_3BYTE_BGR";
            case BufferedImage.TYPE_USHORT_555_RGB:
                return "TYPE_USHORT_555_RGB";
            case BufferedImage.TYPE_BYTE_GRAY:
                return "TYPE_BYTE_GRAY";
            case BufferedImage.TYPE_BYTE_BINARY:
                return "TYPE_BYTE_BINARY";
            default:
                return "TBD";
        }
    }
}
