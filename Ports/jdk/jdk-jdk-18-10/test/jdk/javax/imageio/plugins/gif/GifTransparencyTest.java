/*
 * Copyright (c) 2007, 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     6276653 6287936
 *
 * @summary Test verifes that Image I/O gif writer correctly handles
 *          image what supports tranclucent transparency type but contains
 *          picture with opaque or bitmask transparecy (i.e. each image pixel
 *          is ether opaque or fully transparent).
 *
 * @run     main GifTransparencyTest
 */


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Area;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.spi.ImageWriterSpi;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;


public class GifTransparencyTest {

    BufferedImage src;
    BufferedImage dst;

    public GifTransparencyTest() {
        src = createTestImage();
    }

    public void doTest() {
        File pwd = new File(".");
        try {
            File f = File.createTempFile("transparency_test_", ".gif", pwd);
            System.out.println("file: " + f.getCanonicalPath());

            ImageWriter w = ImageIO.getImageWritersByFormatName("GIF").next();

            ImageWriterSpi spi = w.getOriginatingProvider();

            boolean succeed_write = ImageIO.write(src, "gif", f);

            if (!succeed_write) {
                throw new RuntimeException("Test failed: failed to write src.");
            }

            dst = ImageIO.read(f);

            checkResult(src, dst);

        } catch (IOException e) {
            throw new RuntimeException("Test failed.", e);
        }
    }

    /*
     * Failure criteria:
     *  - src and dst have different dimension
     *  - any transparent pixel was lost
     */
    protected void checkResult(BufferedImage src, BufferedImage dst) {
        int w = src.getWidth();
        int h = src.getHeight();


        if (dst.getWidth() != w || dst.getHeight() != h) {
            throw new RuntimeException("Test failed: wrong result dimension");
        }

        BufferedImage bg = new BufferedImage(2 * w, h, BufferedImage.TYPE_INT_RGB);
        Graphics g = bg.createGraphics();
        g.setColor(Color.white);
        g.fillRect(0, 0, 2 * w, h);

        g.drawImage(src, 0, 0, null);
        g.drawImage(dst, w, 0, null);

        g.dispose();

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int src_rgb = bg.getRGB(x, y);
                int dst_rgb = bg.getRGB(x + w, y);

                if (dst_rgb != src_rgb) {
                    throw new RuntimeException("Test failed: wrong color " +
                            Integer.toHexString(dst_rgb) + " at " + x + ", " +
                            y + " (instead of " + Integer.toHexString(src_rgb) +
                            ")");
                }
            }
        }
        System.out.println("Test passed.");
    }

    public void show() {
        JPanel p = new JPanel(new BorderLayout()) {
            public void paintComponent(Graphics g) {
                g.setColor(Color.blue);
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        p.add(new ImageComponent(src), BorderLayout.WEST);
        if (dst != null) {
        p.add(new ImageComponent(dst), BorderLayout.EAST);
        }

        JFrame f = new JFrame("Transparency");
        f.add(p);

        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.pack();
        f.setVisible(true);
    }

    public static class ImageComponent extends JComponent {
        BufferedImage img;

        public ImageComponent(BufferedImage img) {
            this.img = img;
        }

        public Dimension getPreferredSize() {
            return new Dimension(img.getWidth() + 2, img.getHeight() + 2);
        }

        public void paintComponent(Graphics g) {
            g.drawImage(img, 1, 1, this);
        }
    }

    protected BufferedImage createTestImage() {
        BufferedImage img = new BufferedImage(200, 200,
                                              BufferedImage.TYPE_INT_ARGB);
        Graphics g = img.createGraphics();

        g.setColor(Color.red);
        g.fillRect(50, 50, 100, 100);
        g.dispose();

        return img;
    }

    public static class Empty extends GifTransparencyTest {
        protected BufferedImage createTestImage() {
            return new BufferedImage(200, 200, BufferedImage.TYPE_INT_ARGB);
        }
    }

    public static class Opaque extends GifTransparencyTest {
        protected BufferedImage createTestImage() {
            BufferedImage img = new BufferedImage(200, 200,
                                                  BufferedImage.TYPE_INT_ARGB);
            Graphics g = img.createGraphics();
            g.setColor(Color.cyan);
            g.fillRect(0, 0, 200, 200);

            g.setColor(Color.red);
            g.fillRect(50, 50, 100, 100);
            g.dispose();

            return img;
        }
    }

    public static void main(String[] args) {
        System.out.println("Test bitmask...");
        new GifTransparencyTest().doTest();

        System.out.println("Test opaque...");
        new GifTransparencyTest.Opaque().doTest();

        System.out.println("Test empty...");
        new GifTransparencyTest.Empty().doTest();
    }
}
