/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6430601 8198613
 * @summary Verifies that copyArea() works properly when the
 * destination parameters are outside the destination bounds.
 * @run main/othervm CopyAreaOOB
 * @author campbelc
 */

import java.awt.*;
import java.awt.image.*;

public class CopyAreaOOB extends Canvas {

    private static boolean done;

    public void paint(Graphics g) {
        synchronized (this) {
            if (done) {
                return;
            }
        }

        int w = getWidth();
        int h = getHeight();

        Graphics2D g2d = (Graphics2D)g;
        g2d.setColor(Color.black);
        g2d.fillRect(0, 0, w, h);

        g2d.setColor(Color.green);
        g2d.fillRect(0, 0, w, 10);

        g2d.setColor(Color.red);
        g2d.fillRect(0, 10, 50, h-10);

        // copy the region such that part of it goes below the bottom of the
        // destination surface
        g2d.copyArea(0, 10, 50, h-10, 60, 10);

        Toolkit.getDefaultToolkit().sync();

        synchronized (this) {
            done = true;
            notifyAll();
        }
    }

    public Dimension getPreferredSize() {
        return new Dimension(400, 400);
    }

    private static void testRegion(BufferedImage bi, String name,
                                   int x1, int y1, int x2, int y2,
                                   int expected)
    {
        for (int y = y1; y < y2; y++) {
            for (int x = x1; x < x2; x++) {
                int actual = bi.getRGB(x, y);
                if (actual != expected) {
                    throw new RuntimeException("Test failed for " + name +
                                                       " region at x="+x+" y="+y+
                                                       " (expected="+
                                                       Integer.toHexString(expected) +
                                                       " actual="+
                                                       Integer.toHexString(actual) +
                                                       ")");
                }
            }
        }
    }

    public static void main(String[] args) {
        boolean show = (args.length == 1) && ("-show".equals(args[0]));

        CopyAreaOOB test = new CopyAreaOOB();
        Frame frame = new Frame();
        frame.setUndecorated(true);
        frame.add(test);
        frame.pack();
        frame.setLocationRelativeTo(null);
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

        try {
            Thread.sleep(2000);
        } catch (InterruptedException ex) {}

        // Grab the screen region
        BufferedImage capture = null;
        try {
            Robot robot = new Robot();
            Point pt1 = test.getLocationOnScreen();
            Rectangle rect = new Rectangle(pt1.x, pt1.y, 400, 400);
            capture = robot.createScreenCapture(rect);
        } catch (Exception e) {
            throw new RuntimeException("Problems creating Robot");
        } finally {
            if (!show) {
                frame.dispose();
            }
        }

        // Test pixels
        testRegion(capture, "green",          0,   0, 400,  10, 0xff00ff00);
        testRegion(capture, "original red",   0,  10,  50, 400, 0xffff0000);
        testRegion(capture, "background",    50,  10,  60, 400, 0xff000000);
        testRegion(capture, "in-between",    60,  10, 110,  20, 0xff000000);
        testRegion(capture, "copied red",    60,  20, 110, 400, 0xffff0000);
        testRegion(capture, "background",   110,  10, 400, 400, 0xff000000);
    }
}
