/*
 * Copyright (c) 2003, 2020, Oracle and/or its affiliates. All rights reserved.
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
import java.awt.Component;
import java.awt.DisplayMode;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Transparency;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

/**
 * @test
 * @bug 4836241 6364134 8232200 8252133
 * @key headful
 * @summary verify that images are restored correctly after display mode
 *          switches and that no other rendering or crash problems occur
 * @run main/timeout=500 CycleDMImage
 */
public class CycleDMImage extends Component implements Runnable, KeyListener {
    /**
     * This test cycles through all available display modes, waiting after
     * each call to setDisplayMode() to ensure that the new one is active
     * before proceeding on to the next one.  The Component is filled with
     * a green background color and then compatible images of all 3
     * Transparency types are copied to the screen.  The results of these
     * operations are checked (using Robot) and the test fails if any of the
     * rendering is wrong in any of the DisplayModes.  The purpose of this
     * test is to ensure that display mode switches do not cause problems
     * with image restoration (or other rendering operations).
     */
    boolean painted = false;
    boolean earlyExit = false;
    Image rImage = null, wImage = null, bImage = null;
    int imgSize = 10;
    static Robot robot = null;
    volatile static boolean done = false;
    static String errorMessage = null;

    public synchronized void paint(Graphics g) {
        if (!painted) {
            painted = true;
            (new Thread(this)).start();
        }
        if (rImage == null) {
            GraphicsConfiguration gc = getGraphicsConfiguration();
            rImage = gc.createCompatibleImage(imgSize, imgSize);
            wImage = gc.createCompatibleImage(imgSize, imgSize,
                                              Transparency.BITMASK);
            bImage = gc.createCompatibleImage(imgSize, imgSize,
                                              Transparency.TRANSLUCENT);
            Graphics imgGraphics = rImage.getGraphics();
            imgGraphics.setColor(Color.red);
            imgGraphics.fillRect(0, 0, imgSize, imgSize);
            imgGraphics = wImage.getGraphics();
            imgGraphics.setColor(Color.white);
            imgGraphics.fillRect(0, 0, imgSize, imgSize);
            imgGraphics = bImage.getGraphics();
            imgGraphics.setColor(Color.blue);
            imgGraphics.fillRect(0, 0, imgSize, imgSize);
        }
        g.setColor(Color.green);
        g.fillRect(0, 0, getWidth(), getHeight());
        g.drawImage(rImage, 0, 0, this);
        g.drawImage(wImage, imgSize, 0, this);
        g.drawImage(bImage, imgSize*2, 0, this);
        g.drawImage(rImage, 0, getHeight()-imgSize, null);
        g.drawImage(rImage, getWidth()-imgSize, getHeight()-imgSize, null);
        g.drawImage(rImage, getWidth()-imgSize, 0, null);
    }

    static void delay(long ms) {
        try {
            Thread.sleep(ms);
        } catch (Exception e) {}
    }

    public boolean checkResult(DisplayMode dm) {
        Rectangle bounds = getGraphicsConfiguration().getBounds();
        int pixels[] = new int[imgSize * 4];
        BufferedImage clientPixels =
            robot.createScreenCapture(new Rectangle(bounds.x, bounds.y,
                                                    imgSize*4, 1));
        clientPixels.getRGB(0, 0, imgSize * 4, 1, pixels, 0, getWidth());
        // Now check the results.  We expect: imgSize blocks of r/w/b/g
        int colors[] = {0xffff0000, 0xffffffff, 0xff0000ff, 0xff00ff00};
        for (int color = 0; color < 4; ++color) {
            for (int i = 0; i < imgSize; ++i) {
                int pixelIndex = imgSize * color + i;
                if (pixels[pixelIndex] != colors[color]) {
                    errorMessage = "\n    DisplayMode(" +
                        dm.getWidth() + " x " +
                        dm.getHeight() + " x " +
                        dm.getBitDepth() + "bpp x " +
                        dm.getRefreshRate() +
                        ")\n    Pixel " + i +
                        ": Expected " +
                        Integer.toHexString(colors[color]) +
                        ", got " +
                        Integer.toHexString(pixels[pixelIndex]) +
                        " at " + i;
                    return false;
                }
            }
        }
        return true;
    }

    boolean displayModesEqual(DisplayMode dm1, DisplayMode dm2) {
        if (dm1.equals(dm2)) {
            return true;
        }
        // not enough - check whether the modes are equal except for
        // refreshRate, if either mode has REFRESH_RATE_UNKNOWN
        // value for this parameter
        if (dm1.getWidth() != dm2.getWidth() ||
            dm1.getHeight() != dm2.getHeight() ||
            dm1.getBitDepth() != dm2.getBitDepth())
        {
            // core parameters must match
            return false;
        }
        // Now we know that w1 == w2, h1 == h2, and d1 == d2; must be the
        // case that the refresh rates do not match.
        // Is either one REFRESH_RATE_UNKNOWN?
        if (dm1.getRefreshRate() == DisplayMode.REFRESH_RATE_UNKNOWN ||
            dm2.getRefreshRate() == DisplayMode.REFRESH_RATE_UNKNOWN)
        {
            return true;
        }
        return false;
    }

    public void run() {
        GraphicsDevice gd = getGraphicsConfiguration().getDevice();
        gd.setFullScreenWindow((Window) getParent());
        // First, delay a bit just to let the fullscreen window
        // settle down before switching display modes
        robot.waitForIdle();
        delay(1000);

        if (!gd.isDisplayChangeSupported()) {
            System.err.println("Display change is not supported,"+
                               " the test is considered passed.");
            finished();
            return;
        }

        // We are really only interested in unique w/h/d resolutions
        // and it would be nice to skip the myriad of refresh rate
        // varations, so let us construct a subset that contains
        // only those DisplayModes with unique w/h/d values
        // Also, due to a current bug (4837228), we should skip the
        // 24-bit depths since attempting to create bitmask-transparent
        // ddraw images can cause the test to crash (we should change this
        // test to include that depth when the bug is fixed).
        ArrayList<DisplayMode> dmSubset = new ArrayList<>();
        for (final DisplayMode dm : gd.getDisplayModes()) {
            boolean skip = false;
            for (final DisplayMode dmUnique : dmSubset) {
                int bitDepth = dm.getBitDepth();
                int width = dm.getWidth();
                int height = dm.getHeight();
                if (bitDepth == 24 || width <= 800 || height <= 600 ||
                        (dmUnique.getWidth() == width &&
                         dmUnique.getHeight() == height &&
                         dmUnique.getBitDepth() == bitDepth)) {
                    skip = true;
                    break;
                }
            }
            if (!skip) {
                dmSubset.add(dm);
            }
        }

        // Now, cycle through the display modes one-by-one.  For
        // each new display mode, delay until we detect that the
        // new mode == the current mode.  Then delay an additional
        // second (to allow any repaints to occur)

        for (DisplayMode newDM : dmSubset) {
            gd.setDisplayMode(newDM);
            while (!displayModesEqual(newDM, gd.getDisplayMode())) {
                delay(100);
            }
            // Delay another few seconds after the new display mode is active
            delay(4000);

            // Check the rendering results
            if (!checkResult(newDM)) {
                finished();
                return;
            }

            // Escape out if requested by the user
            if (earlyExit) {
                System.out.println("Exiting test early, by request");
                System.exit(0);
            }
        }

        // Done with test; if we got here, we passed
        System.out.println("Passed");
        finished();
    }

    public static void finished() {
        synchronized (CycleDMImage.class) {
            done = true;
            CycleDMImage.class.notify();
        }
    }

    /**
     * KeyListener methods; these provide a way for a user to escape out of
     * a potentially lengthy test.
     */

    public void keyTyped(KeyEvent e) {
    }

    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
            earlyExit = true;
        }
    }

    public void keyReleased(KeyEvent e) {
    }

    public static void main(String args[]) throws Exception {
        robot = new Robot();
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        for (final GraphicsDevice gd: ge.getScreenDevices()) {
            if (!gd.isFullScreenSupported()) {
                System.err.println("FullScreen mode is not supported,"+
                                           " the test is considered passed.");
                continue;
            }
            done = false;
            DisplayMode currentDM = gd.getDisplayMode();
            Frame frame = new Frame(gd.getDefaultConfiguration());
            try {
                frame.setSize(400, 400);
                frame.setUndecorated(true);
                CycleDMImage comp = new CycleDMImage();
                frame.addKeyListener(comp);
                frame.add(comp);
                frame.setVisible(true);
                // Sleep awaiting frame disposal
                synchronized (CycleDMImage.class) {
                    while (!done) {
                        try {
                            CycleDMImage.class.wait(100);
                        } catch (InterruptedException e) {
                        }
                    }
                }
            } finally {
                gd.setDisplayMode(currentDM);
                gd.setFullScreenWindow(null);
                frame.dispose();
            }
            if (errorMessage != null) {
                throw new RuntimeException(errorMessage);
            }
            // delay a bit just to let the fullscreen window disposing complete
            // before switching to next display
            delay(10000);
        }
    }
}
