/*
 * Copyright (c) 2011, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @key headful
 * @bug     6997116
 * @summary Test verifies that rendering of images with bitmap transparency
 *          to a D3D surface does not cause an ClassCastException.
 *
 * @run main/othervm -Dsun.java2d.d3d=True DrawBitmaskToSurfaceTest
 */

import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.util.concurrent.CountDownLatch;
import javax.swing.JFrame;

public class DrawBitmaskToSurfaceTest extends JFrame {

    private final Image src;
    private static java.util.concurrent.CountDownLatch latch = null;
    private static Throwable theError = null;

    public DrawBitmaskToSurfaceTest() {
        src = createTestImage();
    }

    private static Image createTestImage() {
        byte[] r = new byte[]{(byte)0x00, (byte)0x80, (byte)0xff, (byte)0xff};
        byte[] g = new byte[]{(byte)0x00, (byte)0x80, (byte)0xff, (byte)0x00};
        byte[] b = new byte[]{(byte)0x00, (byte)0x80, (byte)0xff, (byte)0x00};

        IndexColorModel icm = new IndexColorModel(2, 4, r, g, b, 3);

        BufferedImage img = new BufferedImage(100, 100,
                                              BufferedImage.TYPE_BYTE_INDEXED,
                                              icm);
        return img;
    }

    @Override
    public void paint(final Graphics g) {
        try {
            System.err.println("paint frame....");
            g.drawImage(src, 30, 30, this);
        } catch (Throwable e) {
            theError = e;
        } finally {
            if (latch != null) {
                latch.countDown();
            }
        }
    }

    public static void main(final String[] args) throws Exception {
        final JFrame frame = new DrawBitmaskToSurfaceTest();
        frame.setBounds(10, 350, 200, 200);
        frame.setVisible(true);

        Thread.sleep(2000);

        System.err.println("Change frame bounds...");
        latch = new CountDownLatch(1);
        frame.setBounds(10, 350, 90, 90);
        frame.repaint();

        try {
            if (latch.getCount() > 0) {
                latch.await();
            }
        } catch (InterruptedException e) {
        }

        frame.dispose();

        if (theError != null) {
            throw new RuntimeException("Test failed.", theError);
        }

        System.err.println("Test passed");
    }
}
