/*
 * Copyright (c) 2003, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4893446
 * @summary Tests that we get IOException if we try to encode the incompatible
 *          image with RLE compression
 */

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;

public class RleEncodingTest {

    private static int testIdx = 1;

    public static void main(String args[]) throws Exception {
        try {
            int mode = ImageWriteParam.MODE_EXPLICIT;
            String type = "BI_RLE4";
            doTest(type, mode);

            type = "BI_RLE8";
            doTest(type, mode);

            mode = ImageWriteParam.MODE_DEFAULT;
            type = "BI_RLE4";
            doTest(type, mode);

            type = "BI_RLE8";
            doTest(type, mode);

            System.out.println("Test 4bpp image.");
            encodeRLE4Test();

            System.out.println("Test 8bpp image.");
            encodeRLE8Test();
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Unexpected exception. Test failed");
        }
    }

    private static void doTest(String compressionType,
                               int compressionMode) throws IOException
    {
        BufferedImage bimg = new BufferedImage(100, 100,
                                               BufferedImage.TYPE_INT_RGB);
        Graphics g = bimg.getGraphics();
        g.setColor(Color.green);
        g.fillRect(0, 0, 100, 100);

        doTest(bimg, compressionType, compressionMode);
    }

    private static void encodeRLE4Test() throws IOException {
        // create 4bpp image
        byte[] r = new byte[16];
        r[0] = (byte)0xff;
        byte[] g = new byte[16];
        g[1] = (byte)0xff;
        byte[] b = new byte[16];
        b[2] = (byte)0xff;
        IndexColorModel icm = new IndexColorModel(4, 16, r, g, b);

        BufferedImage bimg = new BufferedImage(100, 100,
                                               BufferedImage.TYPE_BYTE_BINARY,
                                               icm);

        Graphics gr = bimg.getGraphics();
        gr.setColor(Color.green);
        gr.fillRect(0, 0, 100, 100);

        doTest(bimg, "BI_RLE4", ImageWriteParam.MODE_EXPLICIT);
    }

    private static void encodeRLE8Test() throws IOException {
        // create 8bpp image
        byte[] r = new byte[256];
        r[0] = (byte)0xff;
        byte[] g = new byte[256];
        g[1] = (byte)0xff;
        byte[] b = new byte[256];
        b[2] = (byte)0xff;
        IndexColorModel icm = new IndexColorModel(8, 256, r, g, b);

        BufferedImage bimg = new BufferedImage(100, 100,
                                               BufferedImage.TYPE_BYTE_INDEXED,
                                               icm);
        Graphics gr = bimg.getGraphics();
        gr.setColor(Color.green);
        gr.fillRect(0, 0, 100, 100);

        doTest(bimg, "BI_RLE8", ImageWriteParam.MODE_EXPLICIT);
    }

    private static void doTest(BufferedImage src,
                               String compressionType,
                               int compressionMode) throws IOException
    {

        ImageWriter iw =  (ImageWriter)ImageIO.getImageWritersBySuffix("bmp").next();
        if (iw == null) {
            throw new RuntimeException("No available writer. Test failed.");
        }

        IIOImage iioImg = new IIOImage(src, null, null);
        ImageWriteParam param = iw.getDefaultWriteParam();


        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageOutputStream ios = ImageIO.createImageOutputStream(baos);
        iw.setOutput(ios);

        System.out.println("Compression Type is " + compressionType);
        System.out.println("Compression Mode is " + compressionMode);

        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionType(compressionType);
        if (compressionMode != ImageWriteParam.MODE_EXPLICIT) {
            param.setCompressionMode(compressionMode);
        }
        try {
            iw.write(null, iioImg, param);
        } catch (IOException e) {
            int bpp = src.getColorModel().getPixelSize();
            if (compressionMode == ImageWriteParam.MODE_EXPLICIT) {
                if ((compressionType.equals("BI_RLE4") && bpp != 4)
                    || (compressionType.equals("BI_RLE8") && bpp != 8))
                {
                    System.out.println("Can not encode "+ bpp+ "bpp image as"
                                      + compressionType);
                    return;
                } else {
                    throw new RuntimeException("Unable to encode "
                                               + bpp + "bpp image as "
                                               + compressionType
                                               + ". Test failed");
                }
            }
        }
        baos.close();

        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        ImageInputStream iis = ImageIO.createImageInputStream(bais);

        BufferedImage dst = ImageIO.read(iis);

        int w = src.getWidth();
        int h = src.getHeight();

        Object dstPixel = dst.getRaster().getDataElements(w/2, h/2, null);
        Object srcPixel = src.getRaster().getDataElements(w/2, h/2, null);

        if ( (src.getColorModel().getRed(srcPixel)
              != dst.getColorModel().getRed(dstPixel))
             || (src.getColorModel().getGreen(srcPixel)
                 != dst.getColorModel().getGreen(dstPixel))
             || (src.getColorModel().getBlue(srcPixel)
                 != dst.getColorModel().getBlue(dstPixel))
             || (src.getColorModel().getAlpha(srcPixel)
                 != dst.getColorModel().getAlpha(dstPixel)) ) {

            showPixel(src, w/2, h/2);
            showPixel(dst, w/2, h/2);

            throw new RuntimeException(
                "Colors are different: " +
                Integer.toHexString(src.getColorModel().getRGB(srcPixel))
                + " and " +
                Integer.toHexString(dst.getColorModel().getRGB(dstPixel)));
        }

    }

    private static void showPixel(BufferedImage src, int x, int y) {
        System.out.println("Img is " + src);
        Object p = src.getRaster().getDataElements(x, y, null);
        System.out.println("RGB:   " +
                           Integer.toHexString(src.getColorModel().getRGB(p)));
        System.out.println("Red:   " +
                           Integer.toHexString(src.getColorModel().getRed(p)));
        System.out.println("Green: " +
                           Integer.toHexString(src.getColorModel().getGreen(p)));
        System.out.println("Blue:  " +
                           Integer.toHexString(src.getColorModel().getBlue(p)));
        System.out.println("Alpha: " +
                           Integer.toHexString(src.getColorModel().getAlpha(p)));
    }
}
