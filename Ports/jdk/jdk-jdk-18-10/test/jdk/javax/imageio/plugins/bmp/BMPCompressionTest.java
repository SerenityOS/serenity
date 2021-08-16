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
 * @modules java.desktop/com.sun.imageio.plugins.bmp
 */

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DirectColorModel;
import java.awt.image.IndexColorModel;
import java.awt.image.PixelInterleavedSampleModel;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.SinglePixelPackedSampleModel;
import java.awt.image.WritableRaster;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.plugins.bmp.BMPImageWriteParam;
import javax.imageio.stream.ImageOutputStream;
import javax.swing.JComponent;
import javax.swing.JFrame;

import com.sun.imageio.plugins.bmp.BMPMetadata;

public class BMPCompressionTest {

    static final String format = "BMP";

    public static void main(String[] args) {

        ImageWriter iw = null;
        Iterator writers = ImageIO.getImageWritersByFormatName(format);
        if (!writers.hasNext()) {
            throw new RuntimeException("No available Image writer for "+format);
        }
        iw = (ImageWriter)writers.next();


        Iterator tests = Test.createTestSet(iw);

        while(tests.hasNext()) {

            Test t = (Test)tests.next();
            System.out.println(t.getDescription());
            t.doTest();
        }

    }


    static class Test {
        static ImageWriter iw;
        private BufferedImage img;
        private String description;
        private BMPImageWriteParam param;
        private IIOMetadata meta;


        public static Iterator createTestSet(ImageWriter w) {
            List l = new LinkedList();

            Test.iw = w;

            // variate compression types
            BMPImageWriteParam param = (BMPImageWriteParam)iw.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionType("BI_RGB");
            if (param.canWriteCompressed()) {
                String[] cTypes = param.getCompressionTypes();
                String[] cDescr = param.getCompressionQualityDescriptions();
                float[] cValues = param.getCompressionQualityValues();

                if (cDescr == null) {
                    System.out.println("There are no compression quality description!");
                } else {
                    for(int i=0; i<cDescr.length; i++) {
                        System.out.println("Quality[" + i + "]=\""+cDescr[i]+"\"");
                    }
                }
                if (cValues == null) {
                    System.out.println("There are no compression quality values!");
                } else {
                    for(int i=0; i<cValues.length; i++) {
                        System.out.println("Value["+i+"]=\""+cValues[i]+"\"");
                    }
                }

                for(int i=0; i<cTypes.length; i++) {
                    String compressionType = cTypes[i];
                    BufferedImage img = null;

                    int type = BufferedImage.TYPE_INT_BGR;
                    try {
                        img = createTestImage(type);
                        if (compressionType.equals("BI_RLE8")) {
                            img = createTestImage2(8, DataBuffer.TYPE_BYTE);
                        } else if (compressionType.equals("BI_RLE4")) {
                            img = createTestImage3(4, DataBuffer.TYPE_BYTE);
                        } else if (compressionType.equals("BI_BITFIELDS")) {
                            img = createTestImage4(32);
                        }

                    } catch (IOException ex) {
                        throw new RuntimeException("Unable to create test image");
                    }
                    BMPImageWriteParam p = (BMPImageWriteParam)iw.getDefaultWriteParam();
                    System.out.println("Current compression type is \""+cTypes[i]+"\"");
                    p.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                    p.setCompressionType(compressionType);

                    IIOMetadata md = iw.getDefaultImageMetadata(new ImageTypeSpecifier(img), p);

                    l.add( new Test(p, md, img));
                }
            }
            //     }
            return l.iterator();

        }

        private Test(BMPImageWriteParam p, IIOMetadata md, BufferedImage i) {
            param = p;
            meta = md;
            img = i;


            description = "Compression type is " + p.getCompressionType();
        }

        public String getDescription() {
            return description;
        }

        public void doTest() {
            try {
                System.out.println(this.getDescription());
                if (param.getCompressionMode() != ImageWriteParam.MODE_EXPLICIT) {
                    System.out.println("Warning: compression mode is not MODE_EXPLICIT");
                }
                // change metadata according to ImageWriteParam
                IIOMetadata new_meta = iw.convertImageMetadata(meta, new ImageTypeSpecifier(img), param);

                IIOImage iio_img = new IIOImage(img, null, new_meta);

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageOutputStream ios = ImageIO.createImageOutputStream(baos);
                iw.setOutput(ios);
                System.out.print("write image...");
                System.out.println("Current compression Type is \""+param.getCompressionType()+"\"");
                iw.write(new_meta, iio_img, param);
                //iw.write(iio_img);
                System.out.println("OK");
                System.out.print("read image ... ");
                ios.flush();

                byte[] ba_image = baos.toByteArray();

                System.out.println("Array length=" + ba_image.length);
                FileOutputStream fos = new FileOutputStream(new File(param.getCompressionType()+".bmp"));
                fos.write(ba_image);
                fos.flush();
                fos = null;
                ByteArrayInputStream bais = new ByteArrayInputStream(ba_image);

                ImageReader ir = ImageIO.getImageReader(iw);
                ir.setInput(ImageIO.createImageInputStream(bais));

                BufferedImage res = ir.read(0);
                System.out.println("OK");

                if (!param.getCompressionType().equals("BI_JPEG")) {
                    System.out.print("compare images ... ");
                    boolean r = compare(img,res);
                    System.out.println(r?"OK":"FAILED");
                    if (!r) {
                        throw new RuntimeException("Compared images are not equals. Test failed.");
                    }
                }


                BMPMetadata mdata = (BMPMetadata)ir.getImageMetadata(0);

                if (!param.getCompressionType().equals(param.getCompressionTypes()[mdata.compression])) {
                    throw new RuntimeException("Different compression value");
                }

            } catch (Exception ex) {
                ex.printStackTrace();
                throw new RuntimeException("Unexpected exception: " + ex);
            }

        }

        private boolean compare(final BufferedImage in, final BufferedImage out) {

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

        private static BufferedImage createTestImage2(int nbits, int transfertype) {
            final int colorShift = 2;
            int SIZE = 256;
            BufferedImage image = null;

            ColorSpace colorSpace =
                ColorSpace.getInstance(ColorSpace.CS_GRAY);
            ColorModel colorModel =
                new ComponentColorModel(colorSpace,
                                        new int[] {nbits},
                                        false,
                                        false,
                                        Transparency.OPAQUE,
                                        transfertype);

            SampleModel sampleModel =
                new PixelInterleavedSampleModel(transfertype,
                                                SIZE,
                                                SIZE,
                                                1,
                                                SIZE,
                                                new int[] {0});

            image =
                new BufferedImage(colorModel,
                                  Raster.createWritableRaster(sampleModel, null),
                                  false, null);
            WritableRaster raster = image.getWritableTile(0, 0);
            int[] samples = raster.getSamples(0, 0, SIZE, SIZE, 0, (int[])null);
            int off = 0;
            int[] row = new int[SIZE];
            for(int i = 0; i < SIZE; i++) {
                Arrays.fill(row, i << colorShift);
                System.arraycopy(row, 0, samples, off, SIZE);
                off += SIZE;
            }
            raster.setSamples(0, 0, SIZE, SIZE, 0, samples);

            return image;
        }


        private static BufferedImage createTestImage3(int nbits, int transfertype) {
            final int colorShift = 2;
            int SIZE = 256;
            BufferedImage image = null;

            ColorSpace colorSpace =
                ColorSpace.getInstance(ColorSpace.CS_sRGB);
            ColorModel colorModel =
                new IndexColorModel(nbits,
                                    4,
                                    new byte[] { (byte)255,   0,   0, (byte)255},
                                    new byte[] {   0, (byte)255,   0, (byte)255},
                                    new byte[] {   0,   0, (byte)255, (byte)255});

            SampleModel sampleModel =
                new PixelInterleavedSampleModel(transfertype,
                                                SIZE,
                                                SIZE,
                                                1,
                                                SIZE,
                                                new int[] {0});

            image =
                new BufferedImage(colorModel,
                                  Raster.createWritableRaster(sampleModel, null),

                                  false, null);

            Graphics2D g = image.createGraphics();
            g.setColor(Color.white);
            g.fillRect(0,0, SIZE, SIZE);
            g.setColor(Color.red);
            g.fillOval(10, 10, SIZE -20, SIZE-20);

            return image;
        }

        private static BufferedImage createTestImage4(int nbits) {
            int SIZE = 10;


            BufferedImage image = null;

            ColorSpace colorSpace =
                ColorSpace.getInstance(ColorSpace.CS_sRGB);
            ColorModel colorModel =
                new DirectColorModel(colorSpace,
                                     nbits, 0xff0000, 0x00ff00, 0x0000ff, 0x000000, false, DataBuffer.TYPE_INT);

            SampleModel sampleModel =
                new SinglePixelPackedSampleModel(DataBuffer.TYPE_INT,
                                                SIZE,
                                                SIZE,
                                      new int[] { 0xff0000, 0x00ff00, 0x0000ff} );


            image =
                new BufferedImage(colorModel,
                                  Raster.createWritableRaster(sampleModel, null),

                                  false, null);

            Graphics2D g = image.createGraphics();
            g.setColor(Color.red);
            g.fillRect(0,0, SIZE, SIZE);
            g.setColor(Color.green);
            //g.fillOval(10, 10, SIZE -20, SIZE-20);
            g.drawLine(7, 0, 7, SIZE);
            g.setColor(Color.blue);
            g.drawLine(1, 0, 1, SIZE);
            g.setColor(Color.white);
            g.drawLine(3, 0, 3, SIZE);
            g.setColor(Color.yellow);
            g.drawLine(5, 0, 5, SIZE);
            return image;
        }

        private static BufferedImage createTestImage(int type)
          throws IOException {

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

    private static void showDiff(final BufferedImage in,
                                 final BufferedImage out) {
        final int width = in.getWidth();
        final int height = in.getHeight();

        JFrame f = new JFrame("");
        f.getContentPane().add( new JComponent() {
                public Dimension getPreferredSize() {
                    return new Dimension(2*width+2, height);
                }
                public void paintComponent(Graphics g) {
                    g.setColor(Color.black);
                    g.drawImage(in, 0,0, null);

                    g.drawImage(out, width+2, 0, null);
                }
            });
        f.pack();
        f.setVisible(true);
    }

}
