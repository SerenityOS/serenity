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
 * @summary Tests writing compression modes of BMP plugin
 */

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.Raster;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageOutputStream;

public class BMPWriteParamTest {

    static final String format = "BMP";

    public static void main(String[] args) {

        ImageWriter iw = null;
        Iterator writers = ImageIO.getImageWritersByFormatName(format);
        if (!writers.hasNext()) {
            throw new RuntimeException("No available Image writer for "+format);
        }
        iw = (ImageWriter)writers.next();

        try {
            BufferedImage img = createTestImage();

            BufferedImage bmp_res = getWriteResult(img, "BMP");
            BufferedImage png_res = getWriteResult(img, "PNG");

            compare(bmp_res, png_res);
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException("Unexpected exception: " + ex);
        }
    }

    private static BufferedImage getWriteResult(BufferedImage img,
                                                String format
                                                ) throws IOException {
        ImageWriter iw = null;
        Iterator writers = ImageIO.getImageWritersByFormatName(format);
        while (writers.hasNext()) {
            iw = (ImageWriter)writers.next();
            System.out.println(format + " -> " + iw.toString());
        }
        if (iw==null) {
            throw new RuntimeException("No available Image writer for "+format);
        }
        ImageWriteParam param = iw.getDefaultWriteParam();

        param.setSourceRegion(new Rectangle(10, 10, 31, 31));
        param.setSourceSubsampling(3, 3, 0, 0);

        IIOMetadata meta = iw.getDefaultImageMetadata(new ImageTypeSpecifier(img), param);

        IIOImage iio_img = new IIOImage(img, null, meta);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageOutputStream ios = ImageIO.createImageOutputStream(baos);
        iw.setOutput(ios);
        iw.write(meta, iio_img, param);
        ios.flush();

        byte[] ba_image = baos.toByteArray();

        ByteArrayInputStream bais = new ByteArrayInputStream(ba_image);

        ImageReader ir = null;

        Iterator readers = ImageIO.getImageReadersByFormatName(format);
        while (readers.hasNext()) {
            ir = (ImageReader)readers.next();
            System.out.println(format + " -> " + ir.toString());
        }
        if (ir==null) {
            throw new RuntimeException("No available Image reader for "+format);
        }

        ir.setInput(ImageIO.createImageInputStream(bais));

        BufferedImage res = ir.read(0);
        return res;
    }

    private static BufferedImage createTestImage()
      throws IOException {

        int w = 50;
        int h = 50;
        BufferedImage b = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = b.createGraphics();
        g.setColor(Color.red);
        g.fillRect(0,0, w, h);
        g.setColor(Color.white);
        for (int i=10; i<=10+30; i+= 3) {
            g.drawLine(i, 10, i, 40);
            g.drawLine(10, i, 40, i);
        }
        return b;
    }

    private static boolean compare(final BufferedImage in,
                                   final BufferedImage out)
    {
        final int width = in.getWidth();
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
                    // showDiff(in, out);
                    throw new RuntimeException("Pixels differ at " + i +
                                               ", " + j + " new = " + Integer.toHexString(newrgb) + " old = " + Integer.toHexString(oldrgb));
                }
            }
        }
        return true;
    }
}
