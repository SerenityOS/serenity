/*
 * Copyright (c) 2002, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4678208 4771101 6328481 6588884 8198613
 * @summary verify the pixelization of degenerate polylines and polygons
 * @run main PolyVertTest
 * @run main/othervm -Dsun.java2d.d3d=True PolyVertTest -hwonly
 */

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.*;

public class PolyVertTest {
    static int TESTWIDTH;
    static int TESTHEIGHT;
    static final int REG_TEST_WIDTH = 10;
    static final int REG_TEST_HEIGHT = 10;
    static final int FULL_TEST_WIDTH = 50;
    static final int FULL_TEST_HEIGHT = 200;

    static final int FRINGE = 2;
    static final int GREEN = Color.green.getRGB();
    static final int RED   = Color.red.getRGB();

    static BufferedImage refImg;
    static BufferedImage errorImg;
    static Graphics errorG;
    static Component testCanvas;

    static int totalbadpixels;
    static int totalfuzzypixels;
    static int numbadtests;
    static int numfuzzytests;
    static int numframes;
    static int fuzzystarty;

    static boolean counting;
    static boolean showerrors;
    static boolean showresults;
    static boolean fringe;
    static boolean forceerror;
    static boolean fulltest = true;
    static boolean hwonly;

    static WindowListener windowCloser = new WindowAdapter() {
        public void windowClosing(WindowEvent e) {
            e.getWindow().hide();
            if (--numframes <= 0) {
                System.exit(0);
            }
        }
    };

    public PolyVertTest() {
        /*
        setBackground(Color.white);
        setForeground(Color.black);
        */
    }

    static int polypts[][][] = {
        {
            // void polygon (no points)
            {}, {},
        },
        {
            // one point
            { 0 }, { 0 },
        },
        {
            // two points
            { 0, 5 }, { 0, 0 },
            { 0, 0, 6, 1,
              10, 0, 6, 1,
              20, 0, 6, 1 },
            { 0, 0, 6, 1,
              10, 0, 1, 1, 15, 0, 1, 1,
              20, 0, 1, 1, 25, 0, 1, 1 },
            { 10, 0, 1, 1,
              20, 0, 1, 1 },
        },
        {
            // open triangle
            { 0, 5, 5 }, { 0, 0, 5 },

            { 0, 0, 6, 1, 5, 1, 1, 5,

              10, 0, 6, 1, 15, 1, 1, 5, 11, 1, 1, 1,
              12, 2, 1, 1, 13, 3, 1, 1, 14, 4, 1, 1,

              20, 0, 6, 1, 25, 1, 1, 5, 21, 1, 1, 1,
              22, 2, 1, 1, 23, 3, 1, 1, 24, 4, 1, 1 },

            { 0, 0, 6, 1, 5, 1, 1, 5,

              10, 0, 6, 1, 15, 1, 1, 5, 11, 1, 1, 1,
              12, 2, 1, 1, 13, 3, 1, 1, 14, 4, 1, 1,

              20, 0, 6, 1, 25, 1, 1, 5, 21, 1, 1, 1,
              22, 2, 1, 1, 23, 3, 1, 1, 24, 4, 1, 1 },

            { 10, 0, 1, 1,
              20, 0, 1, 1 },
        },
        {
            // closed triangle
            { 0, 5, 5, 0 }, { 0, 0, 5, 0 },

            { 0, 0, 6, 1, 5, 1, 1, 5, 1, 1, 1, 1,
              2, 2, 1, 1, 3, 3, 1, 1, 4, 4, 1, 1,

              10, 0, 6, 1, 15, 1, 1, 5, 11, 1, 1, 1,
              12, 2, 1, 1, 13, 3, 1, 1, 14, 4, 1, 1,

              20, 0, 6, 1, 25, 1, 1, 5, 21, 1, 1, 1,
              22, 2, 1, 1, 23, 3, 1, 1, 24, 4, 1, 1 },

            { 1, 0, 5, 1, 5, 1, 1, 5, 1, 1, 1, 1,
              2, 2, 1, 1, 3, 3, 1, 1, 4, 4, 1, 1,

              10, 0, 6, 1, 15, 1, 1, 5, 11, 1, 1, 1,
              12, 2, 1, 1, 13, 3, 1, 1, 14, 4, 1, 1,

              20, 0, 6, 1, 25, 1, 1, 5, 21, 1, 1, 1,
              22, 2, 1, 1, 23, 3, 1, 1, 24, 4, 1, 1 },

            { 0, 0, 1, 1,
              10, 0, 1, 1,
              20, 0, 1, 1 },
        },
        {
            // empty line
            { 0, 0 }, { 0, 0 },
            { 0, 0, 1, 1,
              10, 0, 1, 1,
              20, 0, 1, 1 },
        },
        {
            // empty triangle
            { 0, 0, 0 }, { 0, 0, 0 },
            { 0, 0, 1, 1,
              10, 0, 1, 1,
              20, 0, 1, 1 },
        },
    };

    public static void render(Graphics2D g2d) {
        g2d.setColor(Color.white);
        g2d.fillRect(0, 0, TESTWIDTH, TESTHEIGHT);
        g2d.setColor(Color.black);

        if (forceerror) {
            g2d.fillRect(2, 2, 2, 2);
            g2d.fillRect(15, 5, 1, 1);
        }

        if (!fulltest) {
            g2d.draw(new Rectangle2D.Double(5, 5, 0, 0));
            return;
        }

        g2d.drawLine(10, 10, 10, 10);
        g2d.draw(new Line2D.Double(20, 10, 20, 10));

        g2d.drawRect(10, 20, 0, 0);
        g2d.draw(new Rectangle2D.Double(20, 20, 0, 0));

        g2d.setXORMode(Color.white);

        g2d.drawLine(10, 30, 10, 30);
        g2d.draw(new Line2D.Double(20, 30, 20, 30));

        g2d.drawRect(10, 40, 0, 0);
        g2d.draw(new Rectangle2D.Double(20, 40, 0, 0));

        g2d.setPaintMode();

        int y = 50;
        for (int i = 0; i < polypts.length; i++) {
            int data[][] = polypts[i];
            int xpoints[] = data[0];
            int ypoints[] = data[1];
            int npoints = xpoints.length;
            g2d.translate(10, y);
            g2d.drawPolyline(xpoints, ypoints, npoints);
            g2d.translate(10, 0);
            g2d.drawPolygon(xpoints, ypoints, npoints);
            g2d.translate(10, 0);
            g2d.draw(new Polygon(xpoints, ypoints, npoints));
            g2d.translate(-30, -y);
            y += 10;
        }
        g2d.setXORMode(Color.white);
        for (int i = 0; i < polypts.length; i++) {
            int data[][] = polypts[i];
            int xpoints[] = data[0];
            int ypoints[] = data[1];
            int npoints = xpoints.length;
            g2d.translate(10, y);
            g2d.drawPolyline(xpoints, ypoints, npoints);
            g2d.translate(10, 0);
            g2d.drawPolygon(xpoints, ypoints, npoints);
            g2d.translate(10, 0);
            g2d.draw(new Polygon(xpoints, ypoints, npoints));
            g2d.translate(-30, -y);
            y += 10;
        }
        g2d.setPaintMode();
    }

    public Dimension getPreferredSize() {
        return new Dimension(500, 500);
    }

    public static void usage(int exitcode) {
        System.err.println("usage: java PolyVertTest [<option>]*");
        System.err.println("    -usage         "+
                           "print this usage summary");
        System.err.println("    -count         "+
                           "run all tests and accumulate error counts");
        System.err.println("    -forceerror    "+
                           "force at least one error in each test");
        System.err.println("    -fringe        "+
                           "draw a yellow fringe around problems");
        System.err.println("    -showerrors    "+
                           "display results window for tests with problems");
        System.err.println("    -showresults   "+
                           "display results window for all tests");
        System.err.println("    -quicktest     "+
                           "only run test cases reported in bug reports");
        System.err.println("    -fulltest      "+
                           "run full suite of test cases for a 'unit test'");
        System.err.println("    -hwonly        "+
                           "only run tests for screen and VolatileImage");
        System.exit(exitcode);
    }

    public static void main(String argv[]) {
        for (int i = 0; i < argv.length; i++) {
            String arg = argv[i];
            if (arg.equalsIgnoreCase("-count")) {
                counting = true;
            } else if (arg.equalsIgnoreCase("-forceerror")) {
                forceerror = true;
            } else if (arg.equalsIgnoreCase("-fringe")) {
                fringe = true;
            } else if (arg.equalsIgnoreCase("-showerrors")) {
                showerrors = true;
            } else if (arg.equalsIgnoreCase("-showresults")) {
                showresults = true;
            } else if (arg.equalsIgnoreCase("-quicktest")) {
                fulltest = false;
            } else if (arg.equalsIgnoreCase("-fulltest")) {
                fulltest = true;
            } else if (arg.equalsIgnoreCase("-hwonly")) {
                hwonly = true;
            } else if (arg.equalsIgnoreCase("-usage")) {
                usage(0);
            } else {
                System.err.println("unknown option: "+arg);
                usage(1);
            }
        }

        if (fulltest) {
            TESTWIDTH  = FULL_TEST_WIDTH;
            TESTHEIGHT = FULL_TEST_HEIGHT;
        } else {
            TESTWIDTH  = REG_TEST_WIDTH;
            TESTHEIGHT = REG_TEST_HEIGHT;
        }

        // Prevents premature exit by the WindowAdapter if the user
        // closes the last visible results window before we've
        // finished our tests.
        numframes++;

        makeReferenceImage();
        testScreen();
        testVolatileImage();
        if (!hwonly) {
            testBufferedImage();
            testOffscreen();
            testCompatibleImages();
        }
        if (totalfuzzypixels > 0) {
            System.err.println(totalfuzzypixels+" fuzzy pixels found in "+
                               numfuzzytests+" tests");
        }
        if (totalbadpixels > 0) {
            throw new RuntimeException(totalbadpixels+" bad pixels found in "+
                                       numbadtests+" tests");
        }
        System.out.println("Test done - no bad pixels found");

        --numframes;

        if (counting || ((showresults || showerrors) && numframes == 0)) {
            System.exit(0);
        }
    }

    public static void makeReferenceImage() {
        refImg = new BufferedImage(TESTWIDTH, TESTHEIGHT,
                                   BufferedImage.TYPE_INT_RGB);
        Graphics g = refImg.getGraphics();

        g.setColor(Color.white);
        g.fillRect(0, 0, TESTWIDTH, TESTHEIGHT);

        g.setColor(Color.black);

        if (!fulltest) {
            g.fillRect(5, 5, 1, 1);
            g.dispose();
            return;
        }

        for (int y = 10; y < 50; y += 10) {
            g.fillRect(10, y, 1, 1);
            g.fillRect(20, y, 1, 1);
        }
        int y = 50;
        for (int i = 0; i < polypts.length; i++) {
            int data[][] = polypts[i];
            g.translate(10, y);
            if (data.length > 2) {
                int rectvals[] = data[2];
                for (int j = 0; j < rectvals.length; j += 4) {
                    g.fillRect(rectvals[j+0], rectvals[j+1],
                               rectvals[j+2], rectvals[j+3]);
                }
            }
            g.translate(-10, -y);
            y += 10;
        }
        fuzzystarty = y;
        for (int i = 0; i < polypts.length; i++) {
            int data[][] = polypts[i];
            g.translate(10, y);
            if (data.length > 2) {
                int rectvals[] = data.length > 3 ? data[3] : data[2];
                for (int j = 0; j < rectvals.length; j += 4) {
                    g.fillRect(rectvals[j+0], rectvals[j+1],
                               rectvals[j+2], rectvals[j+3]);
                }
            }
            g.translate(-10, -y);
            y += 10;
        }
        g.dispose();
    }

    public static void initerrorbuf() {
        if (errorImg == null) {
            droperrorbuf();
            errorImg = new BufferedImage(TESTWIDTH, TESTHEIGHT,
                                         BufferedImage.TYPE_INT_RGB);
        }
        if (errorG == null) {
            errorG = errorImg.getGraphics();
        }
        errorG.setColor(Color.green);
        errorG.fillRect(0, 0, TESTWIDTH, TESTHEIGHT);
        errorG.setColor(Color.red);
    }

    public static void droperrorbuf() {
        errorImg = null;
        if (errorG != null) {
            errorG.dispose();
        }
        errorG = null;
    }

    public static void test(Image img, String name) {
        Graphics2D g2d = (Graphics2D) img.getGraphics();
        render(g2d);
        g2d.dispose();
        verify(img, name);
    }

    public static void test(BufferedImage bimg, String name) {
        Graphics2D g2d = bimg.createGraphics();
        render(g2d);
        g2d.dispose();
        verify(bimg, name);
    }

    public static void verify(Image img, String name) {
        BufferedImage bimg;
        if (img instanceof BufferedImage) {
            bimg = (BufferedImage) img;
        } else {
            bimg = new BufferedImage(TESTWIDTH, TESTHEIGHT,
                                     BufferedImage.TYPE_INT_RGB);
            Graphics g = bimg.getGraphics();
            g.drawImage(img, 0, 0, null);
            g.dispose();
        }
        verify(bimg, name);
    }

    public static boolean isFuzzyPixel(int X, int Y) {
        int ytrans = fuzzystarty;
        if (!fulltest || Y < ytrans) {
            return false;
        }
        for (int i = 0; i < polypts.length; i++) {
            int data[][] = polypts[i];
            if (data.length > 4) {
                int rectvals[] = data[4];
                for (int j = 0; j < rectvals.length; j += 4) {
                    int rectx = rectvals[j+0] + 10;
                    int recty = rectvals[j+1] + ytrans;
                    int rectw = rectvals[j+2];
                    int recth = rectvals[j+3];
                    if (X >= rectx && Y >= recty &&
                        X < rectx + rectw && Y < recty + recth)
                    {
                        return true;
                    }
                }
            }
            ytrans += 10;
        }
        return false;
    }

    public static void verify(BufferedImage bimg, String name) {
        int numbadpixels = 0;
        int numfuzzypixels = 0;
        for (int y = 0; y < TESTHEIGHT; y++) {
            for (int x = 0; x < TESTWIDTH; x++) {
                if (refImg.getRGB(x, y) != bimg.getRGB(x, y)) {
                    boolean isfuzzy = isFuzzyPixel(x, y);
                    if (showerrors || showresults) {
                        if (errorG == null) {
                            initerrorbuf();
                        }
                        errorG.setColor(isfuzzy ? Color.blue : Color.red);
                        errorG.fillRect(x, y, 1, 1);
                    } else if (!counting && !isfuzzy) {
                        throw new RuntimeException("Error at "+x+", "+y+
                                                   " while testing: "+name);
                    }
                    if (isfuzzy) {
                        numfuzzypixels++;
                    } else {
                        numbadpixels++;
                    }
                }
            }
        }
        if (numbadpixels > 0 || numfuzzypixels > 0) {
            if (numbadpixels > 0) {
                totalbadpixels += numbadpixels;
                numbadtests++;
            }
            if (numfuzzypixels > 0) {
                totalfuzzypixels += numfuzzypixels;
                numfuzzytests++;
            }
            System.out.println(numbadpixels+" bad pixels and "+
                               numfuzzypixels+" questionable pixels "+
                               "found while testing "+name);
            if (showerrors || showresults) {
                displaydiffs(bimg, name);
            }
        } else if (showresults) {
            if (errorG == null) {
                initerrorbuf();
            }
            displaydiffs(bimg, name);
        }
    }

    public static void displaydiffs(BufferedImage bimg, String name) {
        if (fringe) {
            errorG.setColor(Color.yellow);
            for (int y = 0; y < TESTHEIGHT; y++) {
                for (int x = 0; x < TESTWIDTH; x++) {
                    if (errorImg.getRGB(x, y) == RED) {
                        for (int iy = y-FRINGE; iy <= y+FRINGE; iy++) {
                            for (int ix = x-FRINGE; ix <= x+FRINGE; ix++) {
                                if (ix >= 0 && ix < TESTWIDTH &&
                                    iy >= 0 && iy < TESTHEIGHT &&
                                    errorImg.getRGB(ix, iy) == GREEN)
                                {
                                    errorG.fillRect(ix, iy, 1, 1);
                                }
                            }
                        }
                    }
                }
            }
        }
        Frame f = new Frame("Results for "+name);
        f.setLayout(new BorderLayout());
        f.addWindowListener(windowCloser);
        ++numframes;
        Panel p = new Panel();
        p.add(new ImageCanvas(bimg));
        p.add(new ImageCanvas(errorImg));
        p.add(new ImageCanvas(refImg));
        f.add(p, "Center");
        droperrorbuf();
        f.pack();
        f.show();
    }

    public static void testBufferedImage() {
        testBufferedImage(BufferedImage.TYPE_INT_RGB,        "IntXrgb");
        testBufferedImage(BufferedImage.TYPE_INT_ARGB,       "IntArgb");
        testBufferedImage(BufferedImage.TYPE_3BYTE_BGR,      "ThreeByte");
        testBufferedImage(BufferedImage.TYPE_4BYTE_ABGR,     "FourByte");
        testBufferedImage(BufferedImage.TYPE_USHORT_555_RGB, "UShort555");
        testBufferedImage(BufferedImage.TYPE_BYTE_GRAY,      "ByteGray");
        testBufferedImage(BufferedImage.TYPE_BYTE_INDEXED,   "Indexed");
    }

    public static void testBufferedImage(int type, String name) {
        BufferedImage bimg = new BufferedImage(TESTWIDTH, TESTHEIGHT, type);
        test(bimg, name);
    }

    public static void testScreen() {
        Frame f = new Frame("PolyVertTest");
        TestCanvas child = new TestCanvas();
        testCanvas = child;
        f.add(child);
        f.pack();
        f.show();
        BufferedImage bimg = child.getImage();
        f.hide();
        verify(bimg, "Screen");
    }

    public static void testOffscreen() {
        Image img = testCanvas.createImage(TESTWIDTH, TESTHEIGHT);
        test(img, "Offscreen");
    }

    public static void testCompatibleImages() {
        GraphicsEnvironment genv =
            GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice gdevs[] = genv.getScreenDevices();
        for (int i = 0; i < gdevs.length; i++) {
            testCompatibleImages(gdevs[i]);
        }
    }

    public static void testCompatibleImages(GraphicsDevice gdev) {
        GraphicsConfiguration gconfigs[] = gdev.getConfigurations();
        for (int i = 0; i < gconfigs.length; i++) {
            testCompatibleImages(gconfigs[i]);
        }
    }

    public static void testCompatibleImages(GraphicsConfiguration gconfig) {
        test(gconfig.createCompatibleImage(TESTWIDTH, TESTHEIGHT),
             gconfig+".createCompat()");
        test(gconfig.createCompatibleImage(TESTWIDTH, TESTHEIGHT,
                                           Transparency.OPAQUE),
             gconfig+".createCompat(OPAQUE)");
        test(gconfig.createCompatibleImage(TESTWIDTH, TESTHEIGHT,
                                           Transparency.BITMASK),
             gconfig+".createCompat(BITMASK)");
        test(gconfig.createCompatibleImage(TESTWIDTH, TESTHEIGHT,
                                           Transparency.TRANSLUCENT),
             gconfig+".createCompat(TRANSLUCENT)");
        test(gconfig.createCompatibleVolatileImage(TESTWIDTH, TESTHEIGHT),
             gconfig+".createCompatVolatile()");
    }

    public static void testVolatileImage() {
        Image img = testCanvas.createVolatileImage(TESTWIDTH, TESTHEIGHT);
        test(img, "Volatile");
    }

    public static class ImageCanvas extends Canvas {
        BufferedImage bimg;

        public ImageCanvas(BufferedImage bimg) {
            this.bimg = bimg;
        }

        public Dimension getPreferredSize() {
            return new Dimension(bimg.getWidth(), bimg.getHeight());
        }

        public void paint(Graphics g) {
            g.drawImage(bimg, 0, 0, null);
        }
    }

    public static class TestCanvas extends Canvas {
        BufferedImage bimg;

        public Dimension getPreferredSize() {
            return new Dimension(TESTWIDTH, TESTHEIGHT);
        }

        public void paint(Graphics g) {
            if (bimg != null ||
                getWidth() < TESTWIDTH ||
                getHeight() < TESTHEIGHT)
            {
                return;
            }
            render((Graphics2D) g);
            Toolkit.getDefaultToolkit().sync();
            Point p = getLocationOnScreen();
            Rectangle r = new Rectangle(p.x, p.y, TESTWIDTH, TESTHEIGHT);
            try {
                bimg = new Robot().createScreenCapture(r);
            } catch (AWTException e) {
                e.printStackTrace();
            }
            synchronized (this) {
                notifyAll();
            }
        }

        public synchronized BufferedImage getImage() {
            while (bimg == null) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    return null;
                }
            }
            return bimg;
        }
    }
}
