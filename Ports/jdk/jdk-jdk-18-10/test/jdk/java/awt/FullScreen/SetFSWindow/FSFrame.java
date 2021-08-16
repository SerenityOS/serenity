/*
 * Copyright (c) 2005, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6240507 6662642
 * @summary verify that isFullScreenSupported and getFullScreenWindow work
 * correctly with and without a SecurityManager. Note that the test may fail
 * on older Gnome versions (see bug 6500686).
 * @run main/othervm -Djava.security.manager=allow FSFrame
 * @run main/othervm -Djava.security.manager=allow -Dsun.java2d.noddraw=true FSFrame
 */

import java.awt.*;
import java.awt.image.*;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import javax.imageio.ImageIO;

public class FSFrame extends Frame implements Runnable {

    // Don't start the test until the window is visible
    boolean visible = false;
    Robot robot = null;
    static volatile boolean done = false;

    public void paint(Graphics g) {
        if (!visible && getWidth() != 0 && getHeight() != 0) {
            visible = true;
            try {
                GraphicsDevice gd = getGraphicsConfiguration().getDevice();
                robot = new Robot(gd);
            } catch (Exception e) {
                System.out.println("Problem creating robot: cannot verify FS " +
                                   "window display");
            }
        }
        g.setColor(Color.green);
        g.fillRect(0, 0, getWidth(), getHeight());
    }

    @Override
    public void update(Graphics g) {
        paint(g);
    }

    boolean checkColor(int x, int y, BufferedImage bImg) {
        int pixelColor;
        int correctColor = Color.green.getRGB();
        pixelColor = bImg.getRGB(x, y);
        if (pixelColor != correctColor) {
            System.out.println("FAILURE: pixelColor " +
                               Integer.toHexString(pixelColor) +
                               " != correctColor " +
                               Integer.toHexString(correctColor) +
                               " at coordinates (" + x + ", " + y + ")");
            return false;
        }
        return true;
    }

    void checkFSDisplay(boolean fsSupported) {
        GraphicsConfiguration gc = getGraphicsConfiguration();
        GraphicsDevice gd = gc.getDevice();
        Rectangle r = gc.getBounds();
        Insets in = null;
        if (!fsSupported) {
            in = Toolkit.getDefaultToolkit().getScreenInsets(gc);
            r = new Rectangle(in.left, in.top,
                              r.width -  (in.left + in.right),
                              r.height - (in.top  + in.bottom));
        }
        BufferedImage bImg = robot.createScreenCapture(r);
        // Check that all four corners and middle pixel match the window's
        // fill color
        if (robot == null) {
            return;
        }
        boolean colorCorrect = true;
        colorCorrect &= checkColor(0, 0, bImg);
        colorCorrect &= checkColor(0, bImg.getHeight() - 1, bImg);
        colorCorrect &= checkColor(bImg.getWidth() - 1, 0, bImg);
        colorCorrect &= checkColor(bImg.getWidth() - 1, bImg.getHeight() - 1, bImg);
        colorCorrect &= checkColor(bImg.getWidth() / 2, bImg.getHeight() / 2, bImg);
        if (!colorCorrect) {
            System.err.println("Test failed for mode: fsSupported="+fsSupported);
            if (in != null) {
                System.err.println("screen insets   : " + in);
            }
            System.err.println("screen shot rect: " + r);
            String name = "FSFrame_fs_"+
                    (fsSupported?"supported":"not_supported")+".png";
            try {
                ImageIO.write(bImg, "png", new File(name));
                System.out.println("Dumped screen shot to "+name);
            } catch (IOException ex) {}
            throw new Error("Some pixel colors not correct; FS window may not" +
                            " have been displayed correctly");
        }
    }

    void checkFSFunctionality(boolean withSecurity) {
        GraphicsDevice gd = getGraphicsConfiguration().getDevice();
        if (withSecurity) {
            SecurityManager sm = new SecurityManager();
            System.setSecurityManager(sm);
        }
        try {
            // None of these should throw an exception
            final boolean fs = gd.isFullScreenSupported();
            System.out.println("FullscreenSupported: " + (fs ? "yes" : "no"));
            gd.setFullScreenWindow(this);
            try {
                // Give the system time to set the FS window and display it
                // properly
                Thread.sleep(2000);
            } catch (Exception e) {}
            if (!withSecurity) {
                // See if FS window got displayed correctly
                try {
                    EventQueue.invokeAndWait(new Runnable() {
                        public void run() {
                            repaint();
                            checkFSDisplay(fs);
                        }
                    });
                } catch (InvocationTargetException ex) {
                    ex.printStackTrace();
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            }
            // reset window
            gd.setFullScreenWindow(null);
            try {
                // Give the system time to set the FS window and display it
                // properly
                Thread.sleep(2000);
            } catch (Exception e) {}
        } catch (SecurityException e) {
            e.printStackTrace();
            throw new Error("Failure: should not get an exception when " +
                            "calling isFSSupported or setFSWindow");
        }
    }

    public void run() {
        boolean firstTime = true;
        while (!done) {
            if (visible) {
                checkFSFunctionality(false);
                checkFSFunctionality(true);
                done = true;
            } else {
                // sleep while we wait
                try {
                    // Give the system time to set the FS window and display it
                    // properly
                    Thread.sleep(100);
                } catch (Exception e) {}
            }
        }
        System.out.println("PASS");
    }

    public static void main(String args[]) {
        FSFrame frame = new FSFrame();
        frame.setUndecorated(true);
        Thread t = new Thread(frame);
        frame.setSize(500, 500);
        frame.setVisible(true);
        t.start();
        while (!done) {
            try {
                // Do not exit the main thread until the test is finished
                Thread.sleep(1000);
            } catch (Exception e) {}
        }
        frame.dispose();
    }
}
