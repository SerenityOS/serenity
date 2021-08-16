/*
 * Copyright (c) 2002, 2016, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @key headful
 * @bug   4780022 4862193 7179526
 * @summary  Tests that clipped lines are drawn over the same pixels
 * as unclipped lines (within the clip bounds)
 * @run main/timeout=600/othervm -Dsun.java2d.ddforcevram=true LineClipTest
 * @run main/timeout=600/othervm LineClipTest
 */


/**
 * This app tests whether we are drawing clipped lines the same
 * as unclipped lines.  The problem occurred when we started
 * clipping d3d lines using simple integer clipping, which did not
 * account for sub-pixel precision and ended up drawing very different
 * pixels than the same line drawn unclipped.  A supposed fix
 * to that problem used floating-point clipping instead, but there
 * was some problem with very limited precision inside of d3d
 * (presumably in hardware) that caused some variation in pixels.
 * We decided that whatever the fix was, we needed a serious
 * line check test to make sure that all kinds of different
 * lines would be drawn exactly the same inside the clip area,
 * regardless of whether clipping was enabled.  This test should
 * check all kinds of different cases, such as lines that fall
 * completely outside, completely inside, start outside and
 * end inside, etc., and lines should end and originate in
 * all quadrants of the space divided up by the clip box.
 *
 * The test works as follows:
 * We create nine quadrants using the spaces bisected by the
 * edges of the clip bounds (note that only one of these
 * quadrants is actually visible when clipping is enabled).
 * We create several points in each of these quadrants
 * (three in each of the invisible quadrants, nine in the
 * center/visible quadrant).  Our resulting grid looks like
 * this:
 *
 *  x         x|x    x    x|x         x
 *             |           |
 *             |           |
 *             |           |
 *             |           |
 *             |           |
 *  x          |           |          x
 *  -----------------------------------
 *  x          |x    x    x|          x
 *             |           |
 *             |           |
 *  x          |x    x    x|          x
 *             |           |
 *             |           |
 *  x          |x    x    x|          x
 *  -----------------------------------
 *  x          |           |          x
 *             |           |
 *             |           |
 *             |           |
 *             |           |
 *             |           |
 *  x         x|x    x    x|x         x
 *
 * The test then draws lines from every point to every other
 * point.  First, we draw unclipped lines in blue and
 * then we draw clipped lines in red.
 * At certain times (after every point during the default
 * test, after every quadrant of lines if you run with the -quick
 * option), we check for errors and draw the current image
 * to the screen.  Error checking consists of copying the
 * VolatileImage to a BufferedImage (because we need access
 * to the pixels directly) and checking every pixel in the
 * image.  The check is simple: everything outside the
 * clip bounds should be blue (or the background color) and
 * everything inside the clip bounds should be red (or the
 * background color).  So any blue pixel inside or red
 * pixel outside means that there was a drawing error and
 * the test fails.
 * There are 4 modes that the test can run in (dynamic mode is
 * exclusive to the other modes, but the other modes are combinable):
 *
 *      (default): the clip is set
 *      to a default size (100x100) and the test is run.
 *
 *      -quick: The error
 *      check is run only after every quadrant of lines is
 *      drawn.  This speeds up the test considerably with
 *      some less accuracy in error checking (because pixels
 *      from some lines may overdrawn pixels from other lines
 *      before we have verified the correctness of those
 *      pixels).
 *
 *      -dynamic: There is no error checking, but this version
 *      of the test automatically resizes the clip bounds and
 *      reruns the test over and over.  Nothing besides the
 *      visual check verifies that the test is running correctly.
 *
 *      -rect: Instead of drawing lines, the test draws rectangles
 *      to/from all points in all quadrants.  This tests similar
 *      clipping functionality for drawRect().
 *
 *      n (where "n" is a number): sets the clip size to the
 *      given value.  Just like the default test except that
 *      the clip size is as specified.
 *
 * Note: this test must be run with the -Dsun.java2d.ddforcevram=true
 * option to force the test image to stay in VRAM.  We currently
 * punt VRAM images to system memory when we detect lots of
 * reads.  Since we read the whole buffer on every error check
 * to copy it to the BufferedImage), this causes us to punt the
 * buffer.  A system memory surface will have no d3d capabilities,
 * thus we are not testing the d3d line quality when this happens.
 * By using the ddforcevram flag, we make sure the buffer
 * stays put in VRAM and d3d is used to draw the lines.
 */

import javax.swing.*;
import java.awt.*;
import java.awt.image.*;


public class LineClipTest extends Component implements Runnable {

    int clipBumpVal = 5;
    static int clipSize = 100;
    int clipX1;
    int clipY1;
    static final int NUM_QUADS = 9;
    Point quadrants[][] = new Point[NUM_QUADS][];
    static boolean dynamic = false;
    BufferedImage imageChecker = null;
    Color unclippedColor = Color.blue;
    Color clippedColor = Color.red;
    int testW = -1, testH = -1;
    VolatileImage testImage = null;
    static boolean keepRunning = false;
    static boolean quickTest = false;
    static boolean rectTest = false;
    static boolean runTestDone = false;
    static Frame f = null;

    /**
     * Check for errors in the grid.  This error check consists of
     * copying the buffer into a BufferedImage and reading all pixels
     * in that image.  No pixel outside the clip bounds should be
     * of the color clippedColor and no pixel inside should be
     * of the color unclippedColor.  Any wrong color returns an error.
     */
    boolean gridError(Graphics g) {
        boolean error = false;
        if (imageChecker == null || (imageChecker.getWidth() != testW) ||
            (imageChecker.getHeight() != testH))
        {
            // Recreate BufferedImage as necessary
            GraphicsConfiguration gc = getGraphicsConfiguration();
            ColorModel cm = gc.getColorModel();
            WritableRaster wr =
                cm.createCompatibleWritableRaster(getWidth(), getHeight());
            imageChecker =
                new BufferedImage(cm, wr,
                                  cm.isAlphaPremultiplied(), null);
        }
        // Copy buffer to BufferedImage
        Graphics gChecker = imageChecker.getGraphics();
        gChecker.drawImage(testImage, 0, 0, this);

        // Set up pixel colors to check against
        int clippedPixelColor = clippedColor.getRGB();
        int unclippedPixelColor = unclippedColor.getRGB();
        int wrongPixelColor = clippedPixelColor;
        boolean insideClip = false;
        for (int row = 0; row < getHeight(); ++row) {
            for (int col = 0; col < getWidth(); ++col) {
                if (row >= clipY1 && row < (clipY1 + clipSize) &&
                    col >= clipX1 && col < (clipX1 + clipSize))
                {
                    // Inside clip bounds - should not see unclipped color
                    wrongPixelColor = unclippedPixelColor;
                } else {
                    // Outside clip - should not see clipped color
                    wrongPixelColor = clippedPixelColor;
                }
                int pixel = imageChecker.getRGB(col, row);
                if (pixel == wrongPixelColor) {
                    System.out.println("FAILED: pixel = " +
                                       Integer.toHexString(pixel) +
                                       " at (x, y) = " + col + ", " + row);
                    // Draw magenta rectangle around problem pixel in buffer
                    // for visual feedback to user
                    g.setColor(Color.magenta);
                    g.drawRect(col - 1, row - 1, 2, 2);
                    error = true;
                }
            }
        }
        return error;
    }

    /**
     * Draw all test lines and check for errors (unless running
     * with -dynamic option)
     */
    void drawLineGrid(Graphics screenGraphics, Graphics g) {
        // Fill buffer with background color
        g.setColor(Color.white);
        g.fillRect(0, 0, getWidth(), getHeight());

        // Now, iterate through all quadrants
        for (int srcQuad = 0; srcQuad < NUM_QUADS; ++srcQuad) {
            // Draw lines to all other quadrants
            for (int dstQuad = 0; dstQuad < NUM_QUADS; ++dstQuad) {
                for (int srcPoint = 0;
                     srcPoint < quadrants[srcQuad].length;
                     ++srcPoint)
                {
                    // For every point in the source quadrant
                    int sx = quadrants[srcQuad][srcPoint].x;
                    int sy = quadrants[srcQuad][srcPoint].y;
                    for (int dstPoint = 0;
                         dstPoint < quadrants[dstQuad].length;
                         ++dstPoint)
                    {
                        int dx = quadrants[dstQuad][dstPoint].x;
                        int dy = quadrants[dstQuad][dstPoint].y;
                        if (!rectTest) {
                            // Draw unclipped/clipped lines to every
                            // point in the dst quadrant
                            g.setColor(unclippedColor);
                            g.drawLine(sx, sy, dx, dy);
                            g.setClip(clipX1, clipY1, clipSize, clipSize);
                            g.setColor(clippedColor);
                            g.drawLine(sx,sy, dx, dy);
                        } else {
                            // Draw unclipped/clipped rectangles to every
                            // point in the dst quadrant
                            g.setColor(unclippedColor);
                            int w = dx - sx;
                            int h = dy - sy;
                            g.drawRect(sx, sy, w, h);
                            g.setClip(clipX1, clipY1, clipSize, clipSize);
                            g.setColor(clippedColor);
                            g.drawRect(sx, sy, w, h);
                        }
                        g.setClip(null);
                    }
                    if (!dynamic) {
                        // Draw screen update for visual feedback
                        screenGraphics.drawImage(testImage, 0, 0, this);
                        // On default test, check for errors after every
                        // src point
                        if (!quickTest && gridError(g)) {
                            throw new java.lang.RuntimeException("Failed");
                        }
                    }
                }
            }
            if (!dynamic && quickTest && gridError(g)) {
                // On quick test, check for errors only after every
                // src quadrant
                throw new java.lang.RuntimeException("Failed");
                //return;
            }
        }
        if (!dynamic) {
            System.out.println("PASSED");
            if (!keepRunning) {
                f.dispose();
            }
        }
    }

    /**
     * If we have not yet run the test, or if the window size has
     * changed, or if we are running the test in -dynamic mode,
     * run the test.  Then draw the test buffer to the screen
     */
    public void paint(Graphics g) {
        if (dynamic || testImage == null ||
            getWidth() != testW || getHeight() != testH)
        {
            runTest(g);
        }
        if (testImage != null) {
            g.drawImage(testImage, 0, 0, this);
        }
    }

    /*
     * Create the quadrant of points and run the test to draw all the lines
     */
    public void runTest(Graphics screenGraphics) {
        if (getWidth() == 0 || getHeight() == 0) {
            // May get here before window is really ready
            return;
        }
        clipX1 = (getWidth() - clipSize) / 2;
        clipY1 = (getHeight() - clipSize) / 2;
        int clipX2 = clipX1 + clipSize;
        int clipY2 = clipY1 + clipSize;
        int centerX = getWidth()/2;
        int centerY = getHeight()/2;
        int leftX = 0;
        int topY = 0;
        int rightX = getWidth() - 1;
        int bottomY = getHeight() - 1;
        int quadIndex = 0;
        // Offsets are used to force diagonal (versus hor/vert) lines
        int xOffset = 0;
        int yOffset = 0;

        if (quadrants[0] == null) {
            for (int i = 0; i < 9; ++i) {
                int numPoints = (i == 4) ? 9 : 3;
                quadrants[i] = new Point[numPoints];
            }
        }
        // Upper-left
        quadrants[quadIndex] = new Point[] {
            new Point(leftX + xOffset,          clipY1 - 1 - yOffset),
            new Point(leftX + xOffset,          topY + yOffset),
            new Point(clipX1 - 1 - xOffset,     topY + yOffset),
        };

        quadIndex++;
        yOffset++;
        // Upper-middle
        quadrants[quadIndex] = new Point[] {
            new Point(clipX1 + 1 + xOffset,     topY + yOffset),
            new Point(centerX + xOffset,        topY + yOffset),
            new Point(clipX2 - 1 - xOffset,     topY + yOffset),
        };

        quadIndex++;
        ++yOffset;
        // Upper-right
        quadrants[quadIndex] = new Point[] {
            new Point(clipX2 + 1 + xOffset,     topY + yOffset),
            new Point(rightX - xOffset,         topY + yOffset),
            new Point(rightX - xOffset,         clipY1 - 1 - yOffset),
        };

        quadIndex++;
        yOffset = 0;
        ++xOffset;
        // Middle-left
        quadrants[quadIndex] = new Point[] {
            new Point(leftX + xOffset,          clipY1 + 1 + yOffset),
            new Point(leftX + xOffset,          centerY + yOffset),
            new Point(leftX + xOffset,          clipY2 - 1 - yOffset),
        };

        quadIndex++;
        ++yOffset;
        // Middle-middle
        quadrants[quadIndex] = new Point[] {
            new Point(clipX1 + 1 + xOffset,     clipY1 + 1 + yOffset),
            new Point(centerX + xOffset,        clipY1 + 1 + yOffset),
            new Point(clipX2 - 1 - xOffset,     clipY1 + 1 + yOffset),
            new Point(clipX1 + 1 + xOffset,     centerY + yOffset),
            new Point(centerX + xOffset,        centerY + yOffset),
            new Point(clipX2 - 1 - xOffset,     centerY + yOffset),
            new Point(clipX1 + 1 + xOffset,     clipY2 - 1 - yOffset),
            new Point(centerX + xOffset,        clipY2 - 1 - yOffset),
            new Point(clipX2 - 1 - xOffset,     clipY2 - 1 - yOffset),
        };

        quadIndex++;
        ++yOffset;
        // Middle-right
        quadrants[quadIndex] = new Point[] {
            new Point(rightX - xOffset,         clipY1 + 1 + yOffset),
            new Point(rightX - xOffset,         centerY + yOffset),
            new Point(rightX - xOffset,         clipY2 - 1 - yOffset),
        };

        quadIndex++;
        yOffset = 0;
        ++xOffset;
        // Lower-left
        quadrants[quadIndex] = new Point[] {
            new Point(leftX + xOffset,          clipY2 + 1 + yOffset),
            new Point(leftX + xOffset,          bottomY - yOffset),
            new Point(clipX1 - 1 - xOffset,     bottomY - yOffset),
        };

        quadIndex++;
        ++yOffset;
        // Lower-middle
        quadrants[quadIndex] = new Point[] {
            new Point(clipX1 + 1 + xOffset,     bottomY - yOffset),
            new Point(centerX + xOffset,        bottomY - yOffset),
            new Point(clipX2 - 1 - xOffset,     bottomY - yOffset),
        };

        quadIndex++;
        ++yOffset;
        // Lower-right
        quadrants[quadIndex] = new Point[] {
            new Point(clipX2 + 1 + xOffset,     bottomY - yOffset),
            new Point(rightX - xOffset,         bottomY - yOffset),
            new Point(rightX - xOffset,         clipY2 + 1 + yOffset),
        };


        if (testImage != null) {
            testImage.flush();
        }
        testW = getWidth();
        testH = getHeight();
        testImage = createVolatileImage(testW, testH);
        Graphics g = testImage.getGraphics();
        do {
            int valCode = testImage.validate(getGraphicsConfiguration());
            if (valCode == VolatileImage.IMAGE_INCOMPATIBLE) {
                testImage.flush();
                testImage = createVolatileImage(testW, testH);
                g = testImage.getGraphics();
            }
            drawLineGrid(screenGraphics, g);
        } while (testImage.contentsLost());
        if (dynamic) {
            // Draw clip box if dynamic
            g.setClip(null);
            g.setColor(Color.black);
            g.drawRect(clipX1, clipY1, clipSize, clipSize);
            screenGraphics.drawImage(testImage, 0, 0, this);
        }
        runTestDone = true;
    }

    /**
     * When running -dynamic, resize the clip bounds and run the test
     * over and over
     */
    public void run() {
        while (true) {
            clipSize += clipBumpVal;
            if (clipSize > getWidth() || clipSize < 0) {
                clipBumpVal = -clipBumpVal;
                clipSize += clipBumpVal;
            }
            update(getGraphics());
            try {
                Thread.sleep(50);
            } catch (Exception e) {}
        }
    }

    public static void main(String args[]) {
        for (int i = 0; i < args.length; ++i) {
            if (args[i].equals("-dynamic")) {
                dynamic = true;
            } else if (args[i].equals("-rect")) {
                rectTest = true;
            } else if (args[i].equals("-quick")) {
                quickTest = true;
            } else if (args[i].equals("-keep")) {
                keepRunning = true;
            } else {
                // could be clipSize
                try {
                    clipSize = Integer.parseInt(args[i]);
                } catch (Exception e) {}
            }
        }
        f = new Frame();
        f.setSize(500, 500);
        LineClipTest test = new LineClipTest();
        f.add(test);
        if (dynamic) {
            Thread t = new Thread(test);
            t.start();
        }
        f.setVisible(true);
        while (!runTestDone) {
            // need to make sure jtreg doesn't exit before the
            // test is done...
            try {
                Thread.sleep(50);
            } catch (Exception e) {}
        }
    }
}
