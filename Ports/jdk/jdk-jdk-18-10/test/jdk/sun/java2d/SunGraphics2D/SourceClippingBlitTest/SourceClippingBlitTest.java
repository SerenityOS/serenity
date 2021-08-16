/*
 * Copyright (c) 2005, 2010, Oracle and/or its affiliates. All rights reserved.
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
  @test
  @key headful
  @bug 6244574
  @bug 6258142
  @bug 6395165
  @bug 6588884
  @summary Tests that source is clipped correctly when blitting
  different types of images to the screen
  @author Dmitri.Trembovetski: area=Graphics2D
  @run main SourceClippingBlitTest
*/

import java.awt.AWTException;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.GraphicsConfiguration;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.Transparency;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.awt.image.VolatileImage;

public class SourceClippingBlitTest extends Canvas {
    static final int TESTW = 300;
    static final int TESTH = 300;
    static final int IMAGEW = 50;
    static final int IMAGEH = 50;

    static final Rectangle IMAGE_BOUNDS = new Rectangle(0, 0, IMAGEW, IMAGEH);
    static Robot robot;
    static private boolean showErrors;

    private static final Object lock = new Object();
    private static volatile boolean done = false;

    BufferedImage grabbedBI;

    public static void main(String[] args) {
        // allow user to override the properties if needed
        if (System.getProperty("sun.java2d.pmoffscreen") == null) {
            System.setProperty("sun.java2d.pmoffscreen", "true");
        }

        if (args.length > 0 && args[0].equals("-showerrors")) {
            showErrors = true;
        }

        try {
            robot = new Robot();
        } catch (AWTException e) {
            throw new RuntimeException(e);
        }

        Frame f = new Frame(SourceClippingBlitTest.class.getName());
        final SourceClippingBlitTest test = new SourceClippingBlitTest();
        f.add(test);
        f.addWindowListener(new WindowAdapter() {
            public void windowActivated(WindowEvent e) {
                if (!done) {
                    test.runTests();
                }
            }

        });
        f.pack();
        f.setLocation(100, 100);
        f.setVisible(true);
        synchronized (lock) {
            while (!done) {
                try {
                    lock.wait();
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            }
        }
        if (!showErrors) {
            f.dispose();
        }
    }

    public Dimension getPreferredSize() {
        return new Dimension(TESTW, TESTH);
    }

    public void paint(Graphics g) {
        if (showErrors && done && grabbedBI != null) {
            g.drawImage(grabbedBI, 0, 0, null);
        }
    }

    public void runTests() {
        GraphicsConfiguration gc = getGraphicsConfiguration();
        for (Image srcIm :
            new Image[] {
                getBufferedImage(gc, IMAGEW, IMAGEH,
                        BufferedImage.TYPE_INT_RGB, true),
                getBufferedImage(gc, IMAGEW, IMAGEH,
                        BufferedImage.TYPE_INT_RGB, false),
                // commented out due to 6593406
//                getBMImage(gc, IMAGEW, IMAGEH),
//                getBufferedImage(gc, IMAGEW, IMAGEH,
//                        BufferedImage.TYPE_INT_ARGB, true),
//                getBufferedImage(gc, IMAGEW, IMAGEH,
//                        BufferedImage.TYPE_INT_ARGB, false),
                getVImage(gc, IMAGEW, IMAGEH),
            })
        {
            System.out.println("Testing source: " + srcIm);
            // wiggle the source and dest rectangles
            try {
                for (int locationVar = -10; locationVar < 20; locationVar += 10)
                {
                    for (int sizeVar = -10; sizeVar < 20; sizeVar += 10) {
                        Rectangle srcRect = (Rectangle)IMAGE_BOUNDS.clone();
                        srcRect.translate(locationVar, locationVar);
                        srcRect.grow(sizeVar, sizeVar);

                        Rectangle dstRect =
                                new Rectangle(sizeVar, sizeVar,
                                srcRect.width, srcRect.height);
                        System.out.println("testing blit rect src: " + srcRect);
                        System.out.println("                  dst: " + dstRect);
                        render(getGraphics(), srcIm, srcRect, dstRect);
                        test(srcRect, dstRect);
                    }
                }
                System.out.println("Test passed.");
            } finally {
                synchronized (lock) {
                    done = true;
                    lock.notifyAll();
                }
            }
        }
    }

    public void render(Graphics g, Image image,
                       Rectangle srcRect, Rectangle dstRect)
    {
        int w = getWidth();
        int h = getHeight();
        g.setColor(Color.green);
        g.fillRect(0, 0, w, h);

        int bltWidth = srcRect.width;
        int bltHeight = srcRect.height;
        VolatileImage vi = null;
        if (image instanceof VolatileImage) {
            vi = (VolatileImage)image;
        }
        do {
            if (vi != null) {
                GraphicsConfiguration gc = getGraphicsConfiguration();
                if (vi.validate(gc) != VolatileImage.IMAGE_OK) {
                    initImage(gc, vi);
                }
            }
            g.drawImage(image,
                    dstRect.x, dstRect.y,
                    dstRect.x + bltWidth, dstRect.y + bltHeight,
                    srcRect.x, srcRect.y,
                    srcRect.x + bltWidth, srcRect.y + bltHeight,
                    Color.red,
                    null);
        } while (vi != null && vi.contentsLost());
    }


    public void test(Rectangle srcRect, Rectangle dstRect) {
        int w = getWidth();
        int h = getHeight();
        Toolkit.getDefaultToolkit().sync();
        try {
            Thread.sleep(2000);
        } catch (InterruptedException ex) {}
        Point p = getLocationOnScreen();
        grabbedBI = robot.createScreenCapture(new Rectangle(p.x, p.y, w, h));

        // calculate the destination rectangle
        Rectangle srcBounds = srcRect.intersection(IMAGE_BOUNDS);
        int trX = dstRect.x - srcRect.x;
        int trY = dstRect.y - srcRect.y;
        Rectangle newDstRect = (Rectangle)dstRect.clone();
        newDstRect.translate(-trX, -trY);
        Rectangle.intersect(newDstRect, srcBounds, newDstRect);
        newDstRect.translate(trX, trY);
        Rectangle.intersect(newDstRect, new Rectangle(0, 0, w, h), newDstRect);

        System.out.println("calculated dest rect:" + newDstRect);

        // we do implicit clipping of the destination surface
        // by only checking pixels within its bounds
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int rgb = 0;
                if (newDstRect.contains(x, y)) {
                    rgb = Color.red.getRGB();
                } else {
                    rgb = Color.green.getRGB();
                }
                if (grabbedBI.getRGB(x, y) != rgb) {
                    String msg1 = "Test failed at x="+x+" y="+y;
                    System.out.println(msg1);
                    System.out.println(" expected: "+Integer.toHexString(rgb)+
                            " got:"+Integer.toHexString(grabbedBI.getRGB(x, y)));
                    throw new RuntimeException(msg1);
                }
            }
        }
        System.out.println("subtest passed");
    }

    static VolatileImage dstImage;
    static void initImage(GraphicsConfiguration gc, Image image) {
        Graphics g = image.getGraphics();
        g.setColor(Color.RED);
        int w = image.getWidth(null);
        int h = image.getHeight(null);
        g.fillRect(0, 0, w, h);
        g.dispose();

        // need to 'accelerate' the image
        if (dstImage == null) {
            dstImage =
                gc.createCompatibleVolatileImage(TESTW, TESTH,
                                                 Transparency.OPAQUE);
        }
        dstImage.validate(gc);
        g = dstImage.getGraphics();
        g.drawImage(image, 0, 0, null);
        g.drawImage(image, 0, 0, null);
        g.drawImage(image, 0, 0, null);
    }

    static VolatileImage getVImage(GraphicsConfiguration gc,
                                   int w, int h)
    {
        VolatileImage image =
            gc.createCompatibleVolatileImage(w, h, Transparency.OPAQUE);
        image.validate(gc);
        initImage(gc, image);
        return image;
    }

    static Image getBMImage(GraphicsConfiguration gc,
                            int w, int h)
    {
        Image image =
            gc.createCompatibleImage(w, h, Transparency.BITMASK);
        initImage(gc, image);
        return image;
    }

    static Image getBufferedImage(GraphicsConfiguration gc,
                                  int w, int h, int type, boolean acceleratable)
    {
        BufferedImage image = new BufferedImage(w, h, type);
        if (!acceleratable) {
            image.setAccelerationPriority(0.0f);
        }
        initImage(gc, image);
        return image;
    }
}
