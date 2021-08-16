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
 * @bug     6795544
 *
 * @summary Test verifes that Image I/O gif writer correctly handles
 *          buffered images based on translated reasters (typically
 *          produced by getSubImage() method).
 *
 * @run     main EncodeSubImageTest gif
 */

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.io.File;
import java.io.IOException;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

public class EncodeSubImageTest {
    private static String format = "gif";
    private static ImageWriter writer;
    private static String file_suffix;
    private static final int subSampleX = 2;
    private static final int subSampleY = 2;

    public static void main(String[] args) throws IOException {
        if (args.length > 0) {
            format = args[0];
        }

        writer = ImageIO.getImageWritersByFormatName(format).next();

        file_suffix =writer.getOriginatingProvider().getFileSuffixes()[0];

        BufferedImage src = createTestImage();
        EncodeSubImageTest m1 = new EncodeSubImageTest(src);
        m1.doTest("test_src");

        BufferedImage sub = src.getSubimage(subImageOffset, subImageOffset,
                src.getWidth() - 2 * subImageOffset,
                src.getHeight() - 2 * subImageOffset);
        EncodeSubImageTest m2 = new EncodeSubImageTest(sub);
        m2.doTest("test_sub");
    }

    BufferedImage img;

    public EncodeSubImageTest(BufferedImage img) {
        this.img = img;
    }

    public void doTest(String prefix) throws IOException {
        System.out.println(prefix);
        File f = new File(prefix + file_suffix);
        write(f, false);
        verify(f, false);

        System.out.println(prefix + "_subsampled");
        f = new File(prefix + "_subsampled");
        write(f, true);
        verify(f, true);

        System.out.println(prefix + ": Test PASSED.");
    }

    private static final int subImageOffset = 10;

    private void verify(File f, boolean isSubsampled) {
        BufferedImage dst = null;
        try {
            dst = ImageIO.read(f);
        } catch (IOException e) {
            throw new RuntimeException("Test FAILED: can't readin test image " +
                f.getAbsolutePath(), e);
        }
        if (dst == null) {
            throw new RuntimeException("Test FAILED: no dst image available.");
        }

        checkPixel(dst, 0, 0, isSubsampled);

        checkPixel(dst, img.getWidth() / 2, img.getHeight() / 2, isSubsampled);
    }

    private void checkPixel(BufferedImage dst, int x, int y,
                            boolean isSubsampled)
    {
        int dx = isSubsampled ? x / subSampleX : x;
        int dy = isSubsampled ? y / subSampleY : y;
        int src_rgb = img.getRGB(x, y);
        System.out.printf("src_rgb: %x\n", src_rgb);

        int dst_rgb = dst.getRGB(dx, dy);
        System.out.printf("dst_rgb: %x\n", dst_rgb);

        if (src_rgb != dst_rgb) {
            throw new RuntimeException("Test FAILED: invalid color in dst");
        }
    }

    private static BufferedImage createTestImage() {
        int w = 100;
        int h = 100;

        BufferedImage src = new BufferedImage(w, h,
                BufferedImage.TYPE_BYTE_INDEXED);
        Graphics g = src.createGraphics();
        g.setColor(Color.red);
        g.fillRect(0, 0, w, h);
        g.setColor(Color.green);
        g.fillRect(subImageOffset, subImageOffset,
                w - 2 * subImageOffset, h - 2* subImageOffset);
        g.setColor(Color.blue);
        g.fillRect(2 * subImageOffset, 2 * subImageOffset,
                w - 4 * subImageOffset, h - 4 * subImageOffset);
        g.dispose();

        return src;
    }

    private void write(File f, boolean subsample) throws IOException {
        ImageOutputStream ios = ImageIO.createImageOutputStream(f);

        writer.setOutput(ios);
        ImageWriteParam p = writer.getDefaultWriteParam();
        if (subsample) {
            p.setSourceSubsampling(subSampleX, subSampleY, 0, 0);
        }
        writer.write(null, new IIOImage(img, null, null), p);
        ios.close();
        writer.reset();
    }
}
