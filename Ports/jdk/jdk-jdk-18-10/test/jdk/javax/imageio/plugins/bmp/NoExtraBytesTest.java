/*
 * Copyright (c) 2006, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 5076878
 * @summary Test verifies that ImageIO creates BMP images with correct bpp
 */

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DirectColorModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Hashtable;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;

import org.w3c.dom.Node;

public class NoExtraBytesTest {

    private static Hashtable<Integer, Integer> tests = null;
    private static Color[] usedColors = new Color[] { Color.red, Color.green,     Color.blue, Color.yellow, Color.white, Color.black };

    private static final int TYPE_INT_GRB = 0x100;
    private static final int TYPE_INT_GBR = 0x101;
    private static final int TYPE_INT_RBG = 0x102;
    private static final int TYPE_INT_BRG = 0x103;
    private static final int TYPE_INT_555_GRB = 0x104;
    private static final int TYPE_3BYTE_RGB = 0x105;
    private static final int TYPE_3BYTE_GRB = 0x106;

    private static final int w = 300;
    private static final int h = 200;
    private static final int dx = w / usedColors.length;

    public static void main(String[] args) throws IOException {
        initTests();

        for (Integer type : tests.keySet()) {
            new NoExtraBytesTest(type.intValue(), tests.get(type).intValue()).doTest();
        }
        System.out.println("Test passed.");
    }

    private static void initTests() {
        tests = new Hashtable<Integer, Integer>();

        tests.put(new Integer(BufferedImage.TYPE_INT_RGB), new Integer(24));
        tests.put(new Integer(BufferedImage.TYPE_INT_BGR), new Integer(24));
        tests.put(new Integer(BufferedImage.TYPE_3BYTE_BGR), new Integer(24));
        tests.put(new Integer(TYPE_INT_GRB), new Integer(24));
        tests.put(new Integer(TYPE_INT_GBR), new Integer(24));
        tests.put(new Integer(TYPE_INT_RBG), new Integer(24));
        tests.put(new Integer(TYPE_INT_BRG), new Integer(24));
        tests.put(new Integer(BufferedImage.TYPE_USHORT_555_RGB), new Integer(16));
        tests.put(new Integer(BufferedImage.TYPE_USHORT_565_RGB), new Integer(16));
        tests.put(new Integer(TYPE_INT_555_GRB), new Integer(16));
        tests.put(new Integer(TYPE_3BYTE_RGB), new Integer(24));
        tests.put(new Integer(TYPE_3BYTE_GRB), new Integer(24));
    }

    private static String getImageTypeName(int t) {
        switch(t) {
            case BufferedImage.TYPE_INT_RGB:
                return "TYPE_INT_RGB";
            case BufferedImage.TYPE_INT_BGR:
                return "TYPE_INT_BGR";
            case BufferedImage.TYPE_3BYTE_BGR:
                return "TYPE_3BYTE_BGR";
            case BufferedImage.TYPE_USHORT_555_RGB:
                return "TYPE_USHORT_555_RGB";
            case BufferedImage.TYPE_USHORT_565_RGB:
                return "TYPE_USHORT_565_RGB";
            case TYPE_INT_GRB:
                return "TYPE_INT_GRB";
            case TYPE_INT_GBR:
                return "TYPE_INT_GBR";
            case TYPE_INT_RBG:
                return "TYPE_INT_RBG";
            case TYPE_INT_BRG:
                return "TYPE_INT_BRG";
            case TYPE_INT_555_GRB:
                return "TYPE_INT_555_GRB";
            case TYPE_3BYTE_RGB:
                return "TYPE_3BYTE_RGB";
            case TYPE_3BYTE_GRB:
                return "TYPE_3BYTE_GRB";
            default:
                throw new IllegalArgumentException("Unknown image type: " + t);
        }
    }
    private static BufferedImage createTestImage(int type) {
        BufferedImage dst = null;
        ColorModel colorModel = null;
        WritableRaster raster = null;
        ColorSpace cs = null;
        System.out.println("Create image for " + getImageTypeName(type));
        switch(type) {
            case TYPE_INT_GRB:
                colorModel = new DirectColorModel(24,
                    0x0000ff00,
                    0x00ff0000,
                    0x000000ff);
                break;
            case TYPE_INT_GBR:
                colorModel = new DirectColorModel(24,
                    0x000000ff,
                    0x00ff0000,
                    0x0000ff00);
                break;
            case TYPE_INT_RBG:
                colorModel = new DirectColorModel(24,
                    0x00ff0000,
                    0x000000ff,
                    0x0000ff00);
                break;
            case TYPE_INT_BRG:
                colorModel = new DirectColorModel(24,
                    0x0000ff00,
                    0x000000ff,
                    0x00ff0000);
                break;
            case TYPE_INT_555_GRB:
                colorModel = new DirectColorModel(24,
                        0x0000001F,
                        0x000003e0,
                        0x00007c00);
                break;
            case TYPE_3BYTE_RGB:
                cs = ColorSpace.getInstance(ColorSpace.CS_sRGB);
                int[] nBits = {8, 8, 8};
                int[] bOffs = {0, 1, 2};
                colorModel = new ComponentColorModel(cs, nBits, false, false,
                                                     Transparency.OPAQUE,
                                                     DataBuffer.TYPE_BYTE);
                raster = Raster.createInterleavedRaster(DataBuffer.TYPE_BYTE,
                                                        w, h,
                                                        w*3, 3,
                                                        bOffs, null);
                break;
            case TYPE_3BYTE_GRB:
                cs = ColorSpace.getInstance(ColorSpace.CS_sRGB);
                //nBits = {8, 8, 8};
                //bOffs = {0, 1, 2};
                colorModel = new ComponentColorModel(cs, new int[] { 8, 8, 8 }, false, false,
                                                     Transparency.OPAQUE,
                                                     DataBuffer.TYPE_BYTE);
                raster = Raster.createInterleavedRaster(DataBuffer.TYPE_BYTE,
                        w, h,
                        w*3, 3,
                        new int[] { 1, 0, 2}, null);
                break;
            default:
                dst = new BufferedImage(w, h, type);
                //colorModel = ImageTypeSpecifier.createFromBufferedImageType(type).getColorModel();
        }

        if (dst == null) {
            if (raster == null) {
                raster = colorModel.createCompatibleWritableRaster(w, h);
            }

            dst = new BufferedImage(colorModel, raster, false, null);
        }
        Graphics g = dst.createGraphics();
        for (int i = 0; i < usedColors.length; i ++) {
            g.setColor(usedColors[i]);
            g.fillRect(i * dx, 0, dx, h);
        }
        g.dispose();

        return dst;
    }

    private BufferedImage src;
    private int expectedColorDepth;
    private int type;

    private IIOImage iio_dst;

    public NoExtraBytesTest(int type, int expectedColorDepth) {
        this.type = type;
        this.src = createTestImage(type);
        this.expectedColorDepth = expectedColorDepth;
    }

    public void doTest() throws IOException {
        // write src as BMP
        System.out.println("Test for image: " + getImageTypeName(type));
        System.out.println("image is " + src);

        File f = File.createTempFile("sizeTest_", ".bmp", new File("."));
        System.out.println("Use file " + f.getCanonicalPath());
        ImageIO.write(src, "BMP", f);

        //read it again
        read(f);

        checkColorDepth();

        checkImageContent();
    }

    private void read(File f) throws IOException {
        ImageReader reader = ImageIO.getImageReadersByFormatName("BMP").next();

        ImageInputStream iis =
                ImageIO.createImageInputStream(new FileInputStream(f));

        reader.setInput(iis);

        iio_dst = reader.readAll(0, reader.getDefaultReadParam());
    }

    private void checkColorDepth() {
        IIOMetadata dst = iio_dst.getMetadata();

        Node data = dst.getAsTree("javax_imageio_bmp_1.0");

        Node n = data.getFirstChild();

        while (n != null && !("BitsPerPixel".equals(n.getNodeName()))) {
            System.out.println("Node " + n.getNodeName());
            n = n.getNextSibling();
        }
        if (n == null) {
            throw new RuntimeException("No BitsPerSample node!");
        }

        int bpp = 0;
        String value = n.getNodeValue();
        System.out.println("value = " + value);
        try {
            bpp = Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new RuntimeException("Wrong bpp value: " + value, e);
        }

        if (bpp != this.expectedColorDepth) {
            throw new RuntimeException("Wrong color depth: " + bpp +
                    " (should be " + this.expectedColorDepth + ")");
        }
    }

    private void checkImageContent() {
        BufferedImage dst =
                (BufferedImage)iio_dst.getRenderedImage();
        int y = h / 2;
        int x = dx / 2;

        for (int i = 0; i < usedColors.length; i++, x += dx) {
            int srcRgb = src.getRGB(x, y);
            int dstRgb = dst.getRGB(x, y);
            int rgb = usedColors[i].getRGB();

            if (dstRgb != srcRgb || dstRgb != rgb) {
                throw new RuntimeException("Wrong color at [" + x + ", " + y +
                        "] " + Integer.toHexString(dstRgb) +
                        " (srcRgb=" + Integer.toHexString(srcRgb) +
                        ", original color is " + Integer.toHexString(rgb) + ")");
            }

        }
        System.out.println("Image colors are OK.");
    }
}
