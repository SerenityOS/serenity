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
 * @bug 6664068 6666931
 * @summary Tests that resizing a window to which a tight loop is rendering
 * doesn't produce artifacts or crashes
 * @author Dmitri.Trembovetski@sun.com: area=Graphics
 * @run main/othervm OnScreenRenderingResizeTest
 * @run main/othervm -Dsun.java2d.d3d=false OnScreenRenderingResizeTest
 */

import java.awt.AWTException;
import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.awt.image.VolatileImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

public class OnScreenRenderingResizeTest {

    private static volatile boolean done = false;
    private static volatile boolean nocheck = false;

    private static final int FRAME_W = 256;
    private static final int FRAME_H = 256;
    private static final int IMAGE_W = 128;
    private static final int IMAGE_H = 128;
    private static long RUN_TIME = 1000*20;

    private static final Color renderColor = Color.green;
    private static final Color bgColor = Color.white;

    public static void main(String[] args) {

        for (String arg : args) {
            if ("-inf".equals(arg)) {
                System.err.println("Test will run indefinitely");
                RUN_TIME = Long.MAX_VALUE;
            } else  if ("-nocheck".equals(arg)) {
                System.err.println("Test will not check rendering results");
                nocheck = true;
            } else {
                System.err.println("Usage: OnScreenRenderingResizeTest [-inf][-nocheck]");
            }
        }

        BufferedImage output =
            new BufferedImage(IMAGE_W, IMAGE_H, BufferedImage.TYPE_INT_RGB);
        output.setAccelerationPriority(0.0f);
        Graphics g = output.getGraphics();
        g.setColor(renderColor);
        g.fillRect(0, 0, output.getWidth(), output.getHeight());

        final Frame frame = new Frame("OnScreenRenderingResizeTest") {
            public void paint(Graphics g) {}
            public void update(Graphics g) {}
        };
        frame.setBackground(bgColor);
        frame.setUndecorated(true);
        frame.pack();

        GraphicsConfiguration gc = frame.getGraphicsConfiguration();
        Rectangle gcBounds = gc.getBounds();
        frame.setBounds(gcBounds.width / 4, gcBounds.height / 4, FRAME_W, FRAME_H);

        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                done = true;
            }
        });
        try {
            EventQueue.invokeAndWait(new Runnable() {
                public void run() {
                    frame.setVisible(true);
                }
            });
            // wait for Vista's effects to complete
            Thread.sleep(2000);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        int maxW = gcBounds.width /2;
        int maxH = gcBounds.height/2;
        int minW = frame.getWidth();
        int minH = frame.getHeight();
        int incW = 10, incH = 10, cnt = 0;
        Robot robot = null;
        if (!nocheck && gc.getColorModel().getPixelSize() > 8) {
            try {
                robot = new Robot();
            } catch (AWTException ex) {
                System.err.println("Robot creation failed, continuing.");
            }
        } else {
            System.err.println("No screen rendering checks.");
        }

        VolatileImage vi = gc.createCompatibleVolatileImage(512, 512);
        vi.validate(gc);

        long timeStarted = System.currentTimeMillis();
        while (!done && (System.currentTimeMillis() - timeStarted) < RUN_TIME) {

            if (++cnt > 100) {
                int w = frame.getWidth() + incW;
                int h = frame.getHeight() + incH;
                if (w < minW || w > maxW ) {
                    incW = -incW;
                }
                if (h < minH || h > maxH ) {
                    incH = -incH;
                }
                frame.setSize(w, h);
                cnt = 0;
            }

            // try to put the device into non-default state, for example,
            // this operation below will set the transform
            vi.validate(gc);
            Graphics2D vig = (Graphics2D)vi.getGraphics();
            vig.rotate(30.0f, vi.getWidth()/2, vi.getHeight()/2);
            vig.drawImage(output, 0, 0,
                          vi.getWidth(), vi.getHeight(), null);

            Insets in = frame.getInsets();
            frame.getGraphics().drawImage(output, in.left, in.top, null);
            if (cnt == 90 && robot != null) {
                robot.waitForIdle();
                // area where we blitted to should be either white or green
                Point p = frame.getLocationOnScreen();
                p.translate(in.left+10, in.top+10);
                BufferedImage bi =
                    robot.createScreenCapture(
                        new Rectangle(p.x, p.y, IMAGE_W/2, IMAGE_H/2));
                int accepted1[] = { Color.white.getRGB(), Color.green.getRGB()};
                checkBI(bi, accepted1);

                // the are where we didn't render should stay white
                p = frame.getLocationOnScreen();
                p.translate(in.left, in.top+IMAGE_H+5);
                bi = robot.createScreenCapture(
                    new Rectangle(p.x, p.y,
                                  frame.getWidth()-in.left-in.right,
                                  frame.getHeight()-in.top-in.bottom-5-IMAGE_H));
                int accepted2[] = { Color.white.getRGB() };
                checkBI(bi, accepted2);
            }

            Thread.yield();
        }
        frame.dispose();
        System.out.println("Test Passed");
    }

    private static void checkBI(BufferedImage bi, int accepted[]) {
        for (int x = 0; x < bi.getWidth(); x++) {
            for (int y = 0; y < bi.getHeight(); y++) {
                int pix = bi.getRGB(x, y);
                boolean found = false;
                for (int acc : accepted) {
                    if (pix == acc) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    try {
                        String name = "OnScreenRenderingResizeTest.png";
                        ImageIO.write(bi, "png", new File(name));
                        System.out.println("Screen shot file: " + name);
                    } catch (IOException ex) {}

                    throw new
                        RuntimeException("Test failed at " + x + "-" + y +
                                         " rgb=0x" + Integer.toHexString(pix));
                }
            }
        }
    }
}
