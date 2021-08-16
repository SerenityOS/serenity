/*
 * Copyright (c) 2007, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6648018 6652662
 * @summary Verifies that rendering to a cached onscreen Graphics works
 * @author Dmitri.Trembovetski@sun.com: area=Graphics
 * @run main/othervm RenderingToCachedGraphicsTest
 * @run main/othervm -Dsun.java2d.d3d=false RenderingToCachedGraphicsTest
 */
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import static java.awt.image.VolatileImage.*;
import java.awt.image.VolatileImage;
import java.io.File;
import java.util.concurrent.CountDownLatch;
import javax.imageio.ImageIO;

public class RenderingToCachedGraphicsTest extends Frame {
    private static volatile boolean failed = false;
    private static volatile CountDownLatch latch;
    private Graphics cachedGraphics;
    private Canvas renderCanvas;

    public RenderingToCachedGraphicsTest() {
        super("Test starts in 2 seconds");
        renderCanvas = new Canvas() {
            @Override
            public void paint(Graphics g) {
                if (getWidth() < 100 || getHeight() < 100) {
                    repaint();
                    return;
                }
                // wait for a bit so that Vista's Window manager's animation
                // effects on window's appearance are completed (6652662)
                try { Thread.sleep(2000); } catch (InterruptedException ex) {}

                try {
                    runTest();
                } catch (Throwable t) {
                    failed = true;
                } finally {
                    latch.countDown();
                }
            }
            @Override
            public void update(Graphics g) {}
        };

        add("Center", renderCanvas);
    }

    private void runTest() {
        // this will cause screen update manager to dump the accelerated surface
        // for this canvas
        renderCanvas.createBufferStrategy(2);
        BufferStrategy bs = renderCanvas.getBufferStrategy();
        do {
            Graphics bsg = bs.getDrawGraphics();
            bsg.setColor(Color.blue);
            bsg.fillRect(0, 0,
                         renderCanvas.getWidth(), renderCanvas.getHeight());
        } while (bs.contentsLost() || bs.contentsRestored());

        // grab the "unaccelerated" onscreen surface
        cachedGraphics = renderCanvas.getGraphics();
        cachedGraphics.setColor(Color.red);
        cachedGraphics.fillRect(0, 0, getWidth(), getHeight());

        bs.dispose();
        bs = null;
        // now the update manager should be able to accelerate onscreen
        // rendering to it again

        cachedGraphics.setColor(Color.green);
        // this causes restoration  of the new accelerated onscreen surface
        // (it is created in "lost" state)
        cachedGraphics.fillRect(0, 0,
                                renderCanvas.getWidth(),
                                renderCanvas.getHeight());
        Toolkit.getDefaultToolkit().sync();
        // and now we should be able to render to it
        cachedGraphics.fillRect(0, 0,
                                renderCanvas.getWidth(),
                                renderCanvas.getHeight());
        Toolkit.getDefaultToolkit().sync();

        Robot robot = null;
        try {
            robot = new Robot();
        } catch (Exception e) {
            e.printStackTrace();
            failed = true;
            return;
        }

        Point p = renderCanvas.getLocationOnScreen();
        Rectangle r = new Rectangle(p.x, p.y,
                                    renderCanvas.getWidth(),
                                    renderCanvas.getHeight());
        BufferedImage bi = robot.createScreenCapture(r);
        for (int y = 0; y < bi.getHeight(); y++) {
            for (int x = 0; x < bi.getWidth(); x++) {
                if (bi.getRGB(x, y) != Color.green.getRGB()) {
                    System.err.println("Colors mismatch!");
                    String name = "RenderingToCachedGraphicsTest.png";
                    try {
                        ImageIO.write(bi, "png", new File(name));
                        System.err.println("Dumped grabbed image to: "+name);
                    } catch (Exception e) {}
                    failed = true;
                    return;
                }
            }
        }
    }

    public static void main(String[] args) {
        int depth = GraphicsEnvironment.getLocalGraphicsEnvironment().
            getDefaultScreenDevice().getDefaultConfiguration().
                getColorModel().getPixelSize();
        if (depth < 16) {
            System.out.println("Test PASSED (depth < 16bit)");
            return;
        }

        latch = new CountDownLatch(1);
        RenderingToCachedGraphicsTest t1 = new RenderingToCachedGraphicsTest();
        t1.pack();
        t1.setSize(300, 300);
        t1.setVisible(true);

        try { latch.await(); } catch (InterruptedException ex) {}
        t1.dispose();

        if (failed) {
            throw new
                RuntimeException("Failed: rendering didn't show up");
        }
        System.out.println("Test PASSED");
    }
}
