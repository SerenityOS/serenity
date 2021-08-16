/*
 * Copyright (c) 2007, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 5009033 6603000 6666362 8159142 8198613
 * @summary Verifies that images transformed with bilinear filtering do not
 * leave artifacts at the edges.
 * @run main/othervm -Dsun.java2d.uiScale=2.5 DrawImageBilinear
 * @run main/othervm -Dsun.java2d.uiScale=2.5 -Dsun.java2d.d3d=false DrawImageBilinear
 * @author campbelc
 */

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.awt.image.VolatileImage;

public class DrawImageBilinear extends Canvas {

    private static final int SIZE = 5;

    private static boolean done;
    private BufferedImage bimg1, bimg2;
    private VolatileImage vimg;
    private static volatile BufferedImage capture;
    private static void doCapture(Component test) {
        try {
            Thread.sleep(2000);
        } catch (InterruptedException ex) {}
        // Grab the screen region
        try {
            Robot robot = new Robot();
            Point pt1 = test.getLocationOnScreen();
            Rectangle rect =
                new Rectangle(pt1.x, pt1.y, test.getWidth(), test.getHeight());
            capture = robot.createScreenCapture(rect);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void renderPattern(Graphics g) {
        g.setColor(Color.red);
        g.fillRect(0, 0, SIZE, SIZE);
        //g.setColor(Color.green);
        //g.drawRect(0, 0, SIZE-1, SIZE-1);
        g.dispose();
    }

    public void paint(Graphics g) {
        Graphics2D g2d = (Graphics2D)g;

        if (bimg1 == null) {
            bimg1 = (BufferedImage)createImage(SIZE, SIZE);
            bimg1.setAccelerationPriority(0.0f);
            renderPattern(bimg1.createGraphics());

            bimg2 = (BufferedImage)createImage(SIZE, SIZE);
            renderPattern(bimg2.createGraphics());

            vimg = createVolatileImage(SIZE, SIZE);
            vimg.validate(getGraphicsConfiguration());
            renderPattern(vimg.createGraphics());
        }

        do {
            g2d.setColor(Color.white);
            g2d.fillRect(0, 0, getWidth(), getHeight());

            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                                 RenderingHints.VALUE_INTERPOLATION_BILINEAR);

            // first time will be a sw->surface blit
            g2d.drawImage(bimg1, 10, 10, 40, 40, null);

            // second time will be a texture->surface blit
            g2d.drawImage(bimg2, 80, 10, 40, 40, null);
            if (!skipOglTextureTest) {
                g2d.drawImage(bimg2, 80, 10, 40, 40, null);
            }

            // third time will be a pbuffer->surface blit
            if (vimg.validate(getGraphicsConfiguration()) != VolatileImage.IMAGE_OK) {
                renderPattern(vimg.createGraphics());
            }
            g2d.drawImage(vimg, 150, 10, 40, 40, null);

            Toolkit.getDefaultToolkit().sync();
        } while (vimg.contentsLost());

        synchronized (this) {
            if (!done) {
                doCapture(this);
                done = true;
            }
            notifyAll();
        }
    }

    public Dimension getPreferredSize() {
        return new Dimension(200, 100);
    }

    private static void testRegion(BufferedImage bi,
                                   Rectangle affectedRegion)
    {
        int x1 = affectedRegion.x;
        int y1 = affectedRegion.y;
        int x2 = x1 + affectedRegion.width;
        int y2 = y1 + affectedRegion.height;

        for (int y = y1; y < y2; y++) {
            for (int x = x1; x < x2; x++) {
                int actual = bi.getRGB(x, y);
                if ((actual != 0xfffe0000) && (actual != 0xffff0000)) {
                    throw new RuntimeException("Test failed at x="+x+" y="+y+
                                               " (expected=0xffff0000"+
                                               " actual=0x"+
                                               Integer.toHexString(actual) +
                                               ")");
                }
            }
        }
    }

    private static boolean skipOglTextureTest = false;

    public static void main(String[] args) {
        boolean show = false;
        for (String arg : args) {
            if ("-show".equals(arg)) {
                show = true;
            }
        }

        String arch = System.getProperty("os.arch");
        boolean isOglEnabled = Boolean.getBoolean("sun.java2d.opengl");
        skipOglTextureTest = false;
        System.out.println("Skip OpenGL texture test: " + skipOglTextureTest);

        DrawImageBilinear test = new DrawImageBilinear();
        Frame frame = new Frame();
        frame.add(test);
        frame.pack();
        frame.setVisible(true);

        // Wait until the component's been painted
        synchronized (test) {
            while (!done) {
                try {
                    test.wait();
                } catch (InterruptedException e) {
                    throw new RuntimeException("Failed: Interrupted");
                }
            }
        }

        GraphicsConfiguration gc = frame.getGraphicsConfiguration();
        if (gc.getColorModel() instanceof IndexColorModel) {
            System.out.println("IndexColorModel detected: " +
                               "test considered PASSED");
            frame.dispose();
            return;
        }

        if (!show) {
            frame.dispose();
        }
        if (capture == null) {
            throw new RuntimeException("Failed: capture is null");
        }

        // Test background color
        int pixel = capture.getRGB(5, 5);
        if (pixel != 0xffffffff) {
            throw new RuntimeException("Failed: Incorrect color for " +
                                       "background");
        }

        // Test pixels
        testRegion(capture, new Rectangle(10, 10, 40, 40));
        testRegion(capture, new Rectangle(80, 10, 40, 40));
        testRegion(capture, new Rectangle(150, 10, 40, 40));
    }
}
