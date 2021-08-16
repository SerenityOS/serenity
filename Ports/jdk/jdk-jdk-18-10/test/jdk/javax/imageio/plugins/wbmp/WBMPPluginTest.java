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
 * @bug 4641872
 * @summary Tests writing and reading abilities of WBMP plugin
 */

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.Raster;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;

import javax.imageio.IIOException;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;

public class WBMPPluginTest {

    private static final int[] types = {
        BufferedImage.TYPE_INT_RGB, // = 1;
        BufferedImage.TYPE_INT_ARGB, // = 2;
        BufferedImage.TYPE_INT_ARGB_PRE, // = 3;
        BufferedImage.TYPE_INT_BGR, // = 4;
        BufferedImage.TYPE_3BYTE_BGR, // = 5;
        BufferedImage.TYPE_4BYTE_ABGR, // = 6;
        BufferedImage.TYPE_4BYTE_ABGR_PRE, // 7
        BufferedImage.TYPE_USHORT_565_RGB, // 8
        BufferedImage.TYPE_USHORT_555_RGB, // 9
        BufferedImage.TYPE_BYTE_GRAY, // 10
        BufferedImage.TYPE_USHORT_GRAY, //11
        BufferedImage.TYPE_BYTE_BINARY, //12
        BufferedImage.TYPE_BYTE_INDEXED //13
    };

    private static String format = "WBMP";

    private static ImageReader ir = null;
    private static ImageWriter iw = null;
    private BufferedImage img;
    private ImageWriteParam param;
    private ByteArrayOutputStream baos;

    private static void init() {

        Iterator i = ImageIO.getImageWritersByFormatName(format);
        if (!i.hasNext()) {
            throw new RuntimeException("No available ImageWrites for "+format+" format!");
        }
        iw = (ImageWriter)i.next();

        i = ImageIO.getImageReadersByFormatName(format);
        if (!i.hasNext()) {
            throw new RuntimeException("No available ImageReaders for " +format+" format!");
        }

        ir = (ImageReader)i.next();
    }

    public static void main(String[] args) {
        if (args.length > 0) {
            format = args[0];
            System.out.println("Test format " + format);
        }

        init();
        ImageIO.setUseCache(false);

        for (int i=0; i<types.length; i++) {
            boolean bPassed = true;
            Object reason = null;

            try {

                BufferedImage image = createTestImage(types[i]);

                ImageWriteParam param = iw.getDefaultWriteParam();

                WBMPPluginTest t = new WBMPPluginTest(image, param);
                boolean res = false;
                res = t.test();
                if (!res) {
                    bPassed = false;
                    reason = new String("Null result");
                }
            } catch (IllegalArgumentException ex) {
                System.out.println("Expected exception type was caught: " + ex);

            } catch (Throwable ex ) {
                System.out.println("FAILED");
                ex.printStackTrace();
                bPassed = false;
                reason = ex;
                throw new RuntimeException("Test for type " + types[i] + " FAILED due to exception");
            }
/*
            System.out.println("Type " + types[i] + " result: " +
                               (bPassed ? "PASSED" : "FAILED") +
                               ((reason != null) ? (" Reason: " + reason) : ""));
*/
            System.out.println("Test for type " + types[i] + " PASSED");
        }

        System.out.println("END OF TEST");
    }

    public WBMPPluginTest(BufferedImage img, ImageWriteParam param) {

        this.img = img;
        this.param = param;
        baos = new ByteArrayOutputStream();
    }

    public boolean test() throws IIOException, IOException {

        ir.reset();
        iw.reset();

        String[] suffixes = iw.getOriginatingProvider().getFileSuffixes();

        IIOMetadata md = iw.getDefaultImageMetadata(new ImageTypeSpecifier(img), param);
        IIOImage iio_img = new IIOImage(img, null, md);

        System.out.println("Image type " + img.getType());

        String fname = "test"+img.getType()+"."+suffixes[0];

        iw.setOutput(ImageIO.createImageOutputStream(new FileOutputStream(new File(fname))));
        System.out.print("write image ... ");
        iw.write(iio_img);
        System.out.println("OK");
        System.out.print("read image ... ");

        byte[] ba_image = baos.toByteArray();

        ByteArrayInputStream bais = new ByteArrayInputStream(ba_image);

        ir.setInput(ImageIO.createImageInputStream(new FileInputStream(new File(fname))));

        BufferedImage res = ir.read(0);
        System.out.println("OK");

        System.out.print("compare images ... ");
        boolean r = compare(img,res);
        System.out.println(r?"OK":"FAILED");
        return r;
    }

    private boolean compare(BufferedImage in, BufferedImage out) {
        int width = in.getWidth();
        int height = in.getHeight();
        if (out.getWidth() != width || out.getHeight() != height) {
            throw new RuntimeException("Dimensions changed!");
        }

        Raster oldras = in.getRaster();
        ColorModel oldcm = in.getColorModel();
        Raster newras = out.getRaster();
        ColorModel newcm = out.getColorModel();

        for (int j = 0; j < height; j++) {
            for (int i = 0; i < width; i++) {
                Object oldpixel = oldras.getDataElements(i, j, null);
                int oldrgb = oldcm.getRGB(oldpixel);
                int oldalpha = oldcm.getAlpha(oldpixel);

                Object newpixel = newras.getDataElements(i, j, null);
                int newrgb = newcm.getRGB(newpixel);
                int newalpha = newcm.getAlpha(newpixel);

                if (newrgb != oldrgb ||
                    newalpha != oldalpha) {
                    throw new RuntimeException("Pixels differ at " + i +
                                               ", " + j);
                }
            }
        }
        return true;
    }


    private static BufferedImage createTestImage(int type) throws IOException {

        int w = 200;
        int h = 200;
        BufferedImage b = new BufferedImage(w, h, type);
        Graphics2D g = b.createGraphics();
        g.setColor(Color.white);
        g.fillRect(0,0, w, h);
        g.setColor(Color.black);
        g.fillOval(10, 10, w -20, h-20);

        return b;
    }

}
