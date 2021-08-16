/*
 * Copyright (c) 2009, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     6296893
 * @summary Test verifies that the isTopDown flag does not cause
 *          a writing of bmp image in wrong scanline layout.
 * @run     main TopDownTest
 */

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

import java.awt.image.IndexColorModel;
import java.io.File;
import java.io.IOException;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.plugins.bmp.BMPImageWriteParam;
import javax.imageio.stream.ImageOutputStream;
import static java.awt.image.BufferedImage.TYPE_INT_RGB;
import static java.awt.image.BufferedImage.TYPE_BYTE_INDEXED;

public class TopDownTest {

    public static void main(String[] args) throws IOException {
        BufferedImage src = createTestImage(24);

        writeWithCompression(src, "BI_BITFIELDS");

        writeWithCompression(src, "BI_RGB");

        src = createTestImage(8);
        writeWithCompression(src, "BI_RLE8");

        src = createTestImage(4);
        writeWithCompression(src, "BI_RLE4");

    }

    private static void writeWithCompression(BufferedImage src,
                                             String compression) throws IOException
    {
        System.out.println("Compression: " + compression);
        ImageWriter writer = ImageIO.getImageWritersByFormatName("BMP").next();
        if (writer == null) {
            throw new RuntimeException("Test failed: no bmp writer available");
        }
        File fout = File.createTempFile(compression + "_", ".bmp",
                                        new File("."));

        ImageOutputStream ios = ImageIO.createImageOutputStream(fout);
        writer.setOutput(ios);

        BMPImageWriteParam param = (BMPImageWriteParam)
                writer.getDefaultWriteParam();
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionType(compression);
        param.setTopDown(true);
        writer.write(null, new IIOImage(src, null, null), param);
        writer.dispose();
        ios.flush();
        ios.close();

        BufferedImage dst = ImageIO.read(fout);

        verify(dst);
    }

    private static void verify(BufferedImage dst) {
        int top_rgb = dst.getRGB(50, 25);
        System.out.printf("top_rgb: %x\n", top_rgb);
        int bot_rgb = dst.getRGB(50, 75);
        System.out.printf("bot_rgb: %x\n", bot_rgb);

        // expect to see blue color on the top of image
        if (top_rgb != 0xff0000ff) {
            throw new RuntimeException("Invaid top color: " +
                        Integer.toHexString(bot_rgb));
        }
        if (bot_rgb != 0xffff0000) {
            throw new RuntimeException("Invalid bottom color: " +
                    Integer.toHexString(bot_rgb));
        }
    }

    private static BufferedImage createTestImage(int bpp) {

        BufferedImage img = null;
        switch (bpp) {
            case 8:
                img = new BufferedImage(100, 100, TYPE_BYTE_INDEXED);
                break;
            case 4: {
                byte[] r = new byte[16];
                byte[] g = new byte[16];
                byte[] b = new byte[16];

                r[1] = (byte)0xff;
                b[0] = (byte)0xff;

                IndexColorModel icm = new IndexColorModel(4, 16, r, g, b);
                img = new BufferedImage(100, 100, TYPE_BYTE_INDEXED, icm);
                }
                break;
            case 24:
            default:
            img = new BufferedImage(100, 100, TYPE_INT_RGB);
        }
        Graphics g = img.createGraphics();
        g.setColor(Color.blue);
        g.fillRect(0, 0, 100, 50);
        g.setColor(Color.red);
        g.fillRect(0, 50, 100, 50);
        g.dispose();
        return img;
    }
}
