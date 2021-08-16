/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.JEditorPane;
import javax.swing.text.html.HTMLEditorKit;

/**
 * @test
 * @bug 8134256
 * @summary Different "background-position" should produce different pages
 */
public final class BackgroundImagePosition {

    static final List<String> pos = List.of("", "-2", "-1", "1px", "2px", "3px",
                                            "1em", "2em", "3em", "10%", "55%",
                                            "-1em", "-2em", "-3em", "-10%");
    static final int SIZE = 300;

    public static void main(final String[] args) throws Exception {
        // The image which we will place on the page
        final BufferedImage bi = new BufferedImage(50, 50,
                                                   BufferedImage.TYPE_INT_ARGB);
        final Graphics2D g = bi.createGraphics();
        g.setColor(Color.GREEN);
        g.fillRect(0, 0, 50, 50);
        g.dispose();
        final File file = new File("file.png");
        ImageIO.write(bi, "png", file);
        for (final String x : pos) {
            final BufferedImage img1 = test(x, x);
            for (final String y : pos) {
                // Different positions should produce different pages
                if (!x.equals(y)) {
                    compareImages(img1, test(x, y));
                    compareImages(img1, test(y, x));
                }
            }
        }
    }

    /**
     * Throws an exception if the images are the same.
     */
    static void compareImages(final BufferedImage img1,
                              final BufferedImage img2) throws IOException {
        for (int imgX = 0; imgX < SIZE; ++imgX) {
            for (int imgY = 0; imgY < SIZE; ++imgY) {
                if (img1.getRGB(imgX, imgY) != img2.getRGB(imgX, imgY)) {
                    return;
                }
            }
        }
        ImageIO.write(img1, "png", new File("img1.png"));
        ImageIO.write(img2, "png", new File("img2.png"));
        throw new RuntimeException("Same images for different size");
    }

    static BufferedImage test(final String x, final String y) throws Exception {
        final BufferedImage bi = new BufferedImage(SIZE, SIZE,
                                                   BufferedImage.TYPE_INT_ARGB);
        EventQueue.invokeAndWait(() -> {
            try {
                final JEditorPane jep = new JEditorPane();

                final HTMLEditorKit kit = new HTMLEditorKit();
                jep.setEditorKit(kit);
                jep.setText(
                        "<style>body {"
                                + "background-image: url(file:./file.png);"
                                + "background-repeat: no-repeat;"
                                + "background-position: " + x + " " + y + ";"
                                + "background-color: #FF0000;"
                                + "}</style><body></body>");
                jep.setSize(SIZE, SIZE);

                final Graphics2D graphics = bi.createGraphics();
                jep.paint(graphics);
                graphics.dispose();
            } catch (final Exception e) {
                throw new RuntimeException(e);
            }
        });
        return bi;
    }
}
