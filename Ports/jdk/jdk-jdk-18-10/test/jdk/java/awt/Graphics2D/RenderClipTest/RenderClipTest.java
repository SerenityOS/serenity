/*
 * Copyright (c) 2008, 2010, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6766342
 * @summary Tests clipping invariance for AA rectangle and line primitives
 * @run main RenderClipTest -strict -readfile 6766342.tests
 * @run main RenderClipTest -rectsuite -count 10
 */

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.awt.event.*;
import java.util.Vector;
import java.io.*;

public class RenderClipTest {
    public static double randDblCoord() {
        return Math.random()*60 - 10;
    }

    public static float randFltCoord() {
        return (float) randDblCoord();
    }

    public static int randIntCoord() {
        return (int) Math.round(randDblCoord());
    }

    public static int randInt(int n) {
        return ((int) (Math.random() * (n*4))) >> 2;
    }

    static int numtests;
    static int numerrors;
    static int numfillfailures;
    static int numstrokefailures;
    static int maxerr;

    static boolean useAA;
    static boolean strokePure;
    static boolean testFill;
    static boolean testDraw;
    static boolean silent;
    static boolean verbose;
    static boolean strict;
    static boolean showErrors;
    static float lw;
    static double rot;

    static BufferedImage imgref;
    static BufferedImage imgtst;

    static Graphics2D grefclear;
    static Graphics2D gtstclear;
    static Graphics2D grefrender;
    static Graphics2D gtstrender;

    public static abstract class AnnotatedRenderOp {
        public static AnnotatedRenderOp parse(String str) {
            AnnotatedRenderOp ar;
            if (((ar = Cubic.tryparse(str)) != null) ||
                ((ar = Quad.tryparse(str)) != null) ||
                ((ar = Poly.tryparse(str)) != null) ||
                ((ar = Path.tryparse(str)) != null) ||
                ((ar = Rect.tryparse(str)) != null) ||
                ((ar = Line.tryparse(str)) != null) ||
                ((ar = RectMethod.tryparse(str)) != null) ||
                ((ar = LineMethod.tryparse(str)) != null))
            {
                return ar;
            }
            System.err.println("Unable to parse shape: "+str);
            return null;
        }

        public abstract void randomize();

        public abstract void fill(Graphics2D g2d);

        public abstract void draw(Graphics2D g2d);
    }

    public static abstract class AnnotatedShapeOp extends AnnotatedRenderOp {
        public abstract Shape getShape();

        public void fill(Graphics2D g2d) {
            g2d.fill(getShape());
        }

        public void draw(Graphics2D g2d) {
            g2d.draw(getShape());
        }
    }

    public static void usage(String err) {
        if (err != null) {
            System.err.println(err);
        }
        System.err.println("usage: java RenderClipTest "+
                           "[-read[file F]] [-rectsuite] [-fill] [-draw]");
        System.err.println("                           "+
                           "[-aa] [-pure] [-lw N] [-rot N]");
        System.err.println("                           "+
                           "[-rectmethod] [-linemethod] [-rect] [-line]");
        System.err.println("                           "+
                           "[-cubic] [-quad] [-poly] [-path]");
        System.err.println("                           "+
                           "[-silent] [-verbose] [-showerr] [-count N]");
        System.err.println("                           "+
                           "[-strict] [-usage]");
        System.err.println("    -read         Read test data from stdin");
        System.err.println("    -readfile F   Read test data from file F");
        System.err.println("    -rectsuite    Run a suite of rect/line tests");
        System.err.println("    -fill         Test g.fill*(...)");
        System.err.println("    -draw         Test g.draw*(...)");
        System.err.println("    -aa           Use antialiased rendering");
        System.err.println("    -pure         Use STROKE_PURE hint");
        System.err.println("    -lw N         Test line widths of N "+
                           "(default 1.0)");
        System.err.println("    -rot N        Test rotation by N degrees "+
                           "(default 0.0)");
        System.err.println("    -rectmethod   Test fillRect/drawRect methods");
        System.err.println("    -linemethod   Test drawLine method");
        System.err.println("    -rect         Test Rectangle2D shapes");
        System.err.println("    -line         Test Line2D shapes");
        System.err.println("    -cubic        Test CubicCurve2D shapes");
        System.err.println("    -quad         Test QuadCurve2D shapes");
        System.err.println("    -poly         Test Polygon shapes");
        System.err.println("    -path         Test GeneralPath shapes");
        System.err.println("    -silent       Do not print out error curves");
        System.err.println("    -verbose      Print out progress info");
        System.err.println("    -showerr      Display errors on screen");
        System.err.println("    -count N      N tests per shape, then exit "+
                           "(default 1000)");
        System.err.println("    -strict       All failures are important");
        System.err.println("    -usage        Print this help, then exit");
        System.exit((err != null) ? -1 : 0);
    }

    public static void main(String argv[]) {
        boolean readTests = false;
        String readFile = null;
        boolean rectsuite = false;
        int count = 1000;
        lw = 1.0f;
        rot = 0.0;
        Vector<AnnotatedRenderOp> testOps = new Vector<AnnotatedRenderOp>();
        for (int i = 0; i < argv.length; i++) {
            String arg = argv[i].toLowerCase();
            if (arg.equals("-aa")) {
                useAA = true;
            } else if (arg.equals("-pure")) {
                strokePure = true;
            } else if (arg.equals("-fill")) {
                testFill = true;
            } else if (arg.equals("-draw")) {
                testDraw = true;
            } else if (arg.equals("-lw")) {
                if (i+1 >= argv.length) {
                    usage("Missing argument: "+argv[i]);
                }
                lw = Float.parseFloat(argv[++i]);
            } else if (arg.equals("-rot")) {
                if (i+1 >= argv.length) {
                    usage("Missing argument: "+argv[i]);
                }
                rot = Double.parseDouble(argv[++i]);
            } else if (arg.equals("-cubic")) {
                testOps.add(new Cubic());
            } else if (arg.equals("-quad")) {
                testOps.add(new Quad());
            } else if (arg.equals("-poly")) {
                testOps.add(new Poly());
            } else if (arg.equals("-path")) {
                testOps.add(new Path());
            } else if (arg.equals("-rect")) {
                testOps.add(new Rect());
            } else if (arg.equals("-line")) {
                testOps.add(new Line());
            } else if (arg.equals("-rectmethod")) {
                testOps.add(new RectMethod());
            } else if (arg.equals("-linemethod")) {
                testOps.add(new LineMethod());
            } else if (arg.equals("-verbose")) {
                verbose = true;
            } else if (arg.equals("-strict")) {
                strict = true;
            } else if (arg.equals("-silent")) {
                silent = true;
            } else if (arg.equals("-showerr")) {
                showErrors = true;
            } else if (arg.equals("-readfile")) {
                if (i+1 >= argv.length) {
                    usage("Missing argument: "+argv[i]);
                }
                readTests = true;
                readFile = argv[++i];
            } else if (arg.equals("-read")) {
                readTests = true;
                readFile = null;
            } else if (arg.equals("-rectsuite")) {
                rectsuite = true;
            } else if (arg.equals("-count")) {
                if (i+1 >= argv.length) {
                    usage("Missing argument: "+argv[i]);
                }
                count = Integer.parseInt(argv[++i]);
            } else if (arg.equals("-usage")) {
                usage(null);
            } else {
                usage("Unknown argument: "+argv[i]);
            }
        }
        if (readTests) {
            if (rectsuite || testDraw || testFill ||
                useAA || strokePure ||
                lw != 1.0f || rot != 0.0 ||
                testOps.size() > 0)
            {
                usage("Should not specify test types with -read options");
            }
        } else if (rectsuite) {
            if (testDraw || testFill ||
                useAA || strokePure ||
                lw != 1.0f || rot != 0.0 ||
                testOps.size() > 0)
            {
                usage("Should not specify test types with -rectsuite option");
            }
        } else {
            if (!testDraw && !testFill) {
                usage("No work: Must specify one or both of "+
                      "-fill or -draw");
            }
            if (testOps.size() == 0) {
                usage("No work: Must specify one or more of "+
                      "-rect[method], -line[method], "+
                      "-cubic, -quad, -poly, or -path");
            }
        }
        initImages();
        if (readTests) {
            try {
                InputStream is;
                if (readFile == null) {
                    is = System.in;
                } else {
                    File f =
                        new File(System.getProperty("test.src", "."),
                                 readFile);
                    is = new FileInputStream(f);
                }
                parseAndRun(is);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else if (rectsuite) {
            runRectSuite(count);
        } else {
            initGCs();
            for (int k = 0; k < testOps.size(); k++) {
                AnnotatedRenderOp ar = testOps.get(k);
                runRandomTests(ar, count);
            }
            disposeGCs();
        }
        grefclear.dispose();
        gtstclear.dispose();
        grefclear = gtstclear = null;
        reportStatistics();
    }

    public static int reportStatistics() {
        String connector = "";
        if (numfillfailures > 0) {
            System.out.print(numfillfailures+" fills ");
            connector = "and ";
        }
        if (numstrokefailures > 0) {
            System.out.print(connector+numstrokefailures+" strokes ");
        }
        int totalfailures = numfillfailures + numstrokefailures;
        if (totalfailures == 0) {
            System.out.print("0 ");
        }
        System.out.println("out of "+numtests+" tests failed...");
        int critical = numerrors;
        if (strict) {
            critical += totalfailures;
        }
        if (critical > 0) {
            throw new RuntimeException(critical+" tests had critical errors");
        }
        System.out.println("No tests had critical errors");
        return (numerrors+totalfailures);
    }

    public static void runRectSuite(int count) {
        AnnotatedRenderOp ops[] = {
            new Rect(),
            new RectMethod(),
            new Line(),
            new LineMethod(),
        };
        // Sometimes different fill algorithms are chosen for
        // thin and wide line modes, make sure we test both...
        float filllinewidths[] = { 0.0f, 2.0f };
        float drawlinewidths[] = { 0.0f, 0.5f, 1.0f,
                                   2.0f, 2.5f,
                                   5.0f, 5.3f };
        double rotations[] = { 0.0, 15.0, 90.0,
                               135.0, 180.0,
                               200.0, 270.0,
                               300.0};
        for (AnnotatedRenderOp ar: ops) {
            for (double r: rotations) {
                rot = r;
                for (int i = 0; i < 8; i++) {
                    float linewidths[];
                    if ((i & 1) == 0) {
                        if ((ar instanceof Line) ||
                            (ar instanceof LineMethod))
                        {
                            continue;
                        }
                        testFill = true;
                        testDraw = false;
                        linewidths = filllinewidths;
                    } else {
                        testFill = false;
                        testDraw = true;
                        linewidths = drawlinewidths;
                    }
                    useAA = ((i & 2) != 0);
                    strokePure = ((i & 4) != 0);
                    for (float w : linewidths) {
                        lw = w;
                        runSuiteTests(ar, count);
                    }
                }
            }
        }
    }

    public static void runSuiteTests(AnnotatedRenderOp ar, int count) {
        if (verbose) {
            System.out.print("Running ");
            System.out.print(testFill ? "Fill " : "Draw ");
            System.out.print(BaseName(ar));
            if (useAA) {
                System.out.print(" AA");
            }
            if (strokePure) {
                System.out.print(" Pure");
            }
            if (lw != 1.0f) {
                System.out.print(" lw="+lw);
            }
            if (rot != 0.0f) {
                System.out.print(" rot="+rot);
            }
            System.out.println();
        }
        initGCs();
        runRandomTests(ar, count);
        disposeGCs();
    }

    public static String BaseName(AnnotatedRenderOp ar) {
        String s = ar.toString();
        int leftparen = s.indexOf('(');
        if (leftparen >= 0) {
            s = s.substring(0, leftparen);
        }
        return s;
    }

    public static void runRandomTests(AnnotatedRenderOp ar, int count) {
        for (int i = 0; i < count; i++) {
            ar.randomize();
            if (testDraw) {
                test(ar, false);
            }
            if (testFill) {
                test(ar, true);
            }
        }
    }

    public static void initImages() {
        imgref = new BufferedImage(40, 40, BufferedImage.TYPE_INT_RGB);
        imgtst = new BufferedImage(40, 40, BufferedImage.TYPE_INT_RGB);
        grefclear = imgref.createGraphics();
        gtstclear = imgtst.createGraphics();
        grefclear.setColor(Color.white);
        gtstclear.setColor(Color.white);
    }

    public static void initGCs() {
        grefrender = imgref.createGraphics();
        gtstrender = imgtst.createGraphics();
        gtstrender.clipRect(10, 10, 20, 20);
        grefrender.setColor(Color.blue);
        gtstrender.setColor(Color.blue);
        if (lw != 1.0f) {
            BasicStroke bs = new BasicStroke(lw);
            grefrender.setStroke(bs);
            gtstrender.setStroke(bs);
        }
        if (rot != 0.0) {
            double rotrad = Math.toRadians(rot);
            grefrender.rotate(rotrad, 20, 20);
            gtstrender.rotate(rotrad, 20, 20);
        }
        if (strokePure) {
            grefrender.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,
                                        RenderingHints.VALUE_STROKE_PURE);
            gtstrender.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,
                                        RenderingHints.VALUE_STROKE_PURE);
        }
        if (useAA) {
            grefrender.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                        RenderingHints.VALUE_ANTIALIAS_ON);
            gtstrender.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                        RenderingHints.VALUE_ANTIALIAS_ON);
            maxerr = 1;
        }
    }

    public static void disposeGCs() {
        grefrender.dispose();
        gtstrender.dispose();
        grefrender = gtstrender = null;
    }

    public static void parseAndRun(InputStream in) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(in));
        String str;
        while ((str = br.readLine()) != null) {
            if (str.startsWith("Stroked ") || str.startsWith("Filled ")) {
                parseTest(str);
                continue;
            }
            if (str.startsWith("Running ")) {
                continue;
            }
            if (str.startsWith("Failed: ")) {
                continue;
            }
            if (str.indexOf(" out of ") > 0 &&
                str.indexOf(" tests failed...") > 0)
            {
                continue;
            }
            if (str.indexOf(" tests had critical errors") > 0) {
                continue;
            }
            System.err.println("Unparseable line: "+str);
        }
    }

    public static void parseTest(String origstr) {
        String str = origstr;
        boolean isfill = false;
        useAA = strokePure = false;
        lw = 1.0f;
        rot = 0.0;
        if (str.startsWith("Stroked ")) {
            str = str.substring(8);
            isfill = false;
        } else if (str.startsWith("Filled ")) {
            str = str.substring(7);
            isfill = true;
        } else {
            System.err.println("Unparseable test line: "+origstr);
        }
        if (str.startsWith("AA ")) {
            str = str.substring(3);
            useAA = true;
        }
        if (str.startsWith("Pure ")) {
            str = str.substring(5);
            strokePure = true;
        }
        if (str.startsWith("Lw=")) {
            int index = str.indexOf(' ', 3);
            if (index > 0) {
                lw = Float.parseFloat(str.substring(3, index));
                str = str.substring(index+1);
            }
        }
        if (str.startsWith("Rot=")) {
            int index = str.indexOf(' ', 4);
            if (index > 0) {
                rot = Double.parseDouble(str.substring(4, index));
                str = str.substring(index+1);
            }
        }
        AnnotatedRenderOp ar = AnnotatedRenderOp.parse(str);
        if (ar != null) {
            initGCs();
            test(ar, isfill);
            disposeGCs();
        } else {
            System.err.println("Unparseable test line: "+origstr);
        }
    }

    public static void test(AnnotatedRenderOp ar, boolean isfill) {
        grefclear.fillRect(0, 0, 40, 40);
        gtstclear.fillRect(0, 0, 40, 40);
        if (isfill) {
            ar.fill(grefrender);
            ar.fill(gtstrender);
        } else {
            ar.draw(grefrender);
            ar.draw(gtstrender);
        }
        check(imgref, imgtst, ar, isfill);
    }

    public static int[] getData(BufferedImage img) {
        Raster r = img.getRaster();
        DataBufferInt dbi = (DataBufferInt) r.getDataBuffer();
        return dbi.getData();
    }

    public static int getScan(BufferedImage img) {
        Raster r = img.getRaster();
        SinglePixelPackedSampleModel sppsm =
            (SinglePixelPackedSampleModel) r.getSampleModel();
        return sppsm.getScanlineStride();
    }

    public static int getOffset(BufferedImage img) {
        Raster r = img.getRaster();
        SinglePixelPackedSampleModel sppsm =
            (SinglePixelPackedSampleModel) r.getSampleModel();
        return sppsm.getOffset(-r.getSampleModelTranslateX(),
                               -r.getSampleModelTranslateY());
    }

    final static int opaque = 0xff000000;
    final static int whitergb = Color.white.getRGB();

    public static final int maxdiff(int rgb1, int rgb2) {
        int maxd = 0;
        for (int i = 0; i < 32; i += 8) {
            int c1 = (rgb1 >> i) & 0xff;
            int c2 = (rgb2 >> i) & 0xff;
            int d = Math.abs(c1-c2);
            if (maxd < d) {
                maxd = d;
            }
        }
        return maxd;
    }

    public static void check(BufferedImage imgref, BufferedImage imgtst,
                             AnnotatedRenderOp ar, boolean wasfill)
    {
        numtests++;
        int dataref[] = getData(imgref);
        int datatst[] = getData(imgtst);
        int scanref = getScan(imgref);
        int scantst = getScan(imgtst);
        int offref = getOffset(imgref);
        int offtst = getOffset(imgtst);

        // We want to check for errors outside the clip at a higher
        // priority than errors involving different pixels touched
        // inside the clip.

        // Check above clip
        if (check(ar, wasfill,
                  null, 0, 0,
                  datatst, scantst, offtst,
                  0, 0, 40, 10))
        {
            return;
        }
        // Check below clip
        if (check(ar, wasfill,
                  null, 0, 0,
                  datatst, scantst, offtst,
                  0, 30, 40, 40))
        {
            return;
        }
        // Check left of clip
        if (check(ar, wasfill,
                  null, 0, 0,
                  datatst, scantst, offtst,
                  0, 10, 10, 30))
        {
            return;
        }
        // Check right of clip
        if (check(ar, wasfill,
                  null, 0, 0,
                  datatst, scantst, offtst,
                  30, 10, 40, 30))
        {
            return;
        }
        // Check inside clip
        check(ar, wasfill,
              dataref, scanref, offref,
              datatst, scantst, offtst,
              10, 10, 30, 30);
    }

    public static boolean check(AnnotatedRenderOp ar, boolean wasfill,
                                int dataref[], int scanref, int offref,
                                int datatst[], int scantst, int offtst,
                                int x0, int y0, int x1, int y1)
    {
        offref += scanref * y0;
        offtst += scantst * y0;
        for (int y = y0; y < y1; y++) {
            for (int x = x0; x < x1; x++) {
                boolean failed;
                String reason;
                int rgbref;
                int rgbtst;

                rgbtst = datatst[offtst+x] | opaque;
                if (dataref == null) {
                    /* Outside of clip, must be white, no error tolerance */
                    rgbref = whitergb;
                    failed = (rgbtst != rgbref);
                    reason = "stray pixel rendered outside of clip";
                } else {
                    /* Inside of clip, check for maxerr delta in components */
                    rgbref = dataref[offref+x] | opaque;
                    failed = (rgbref != rgbtst &&
                              maxdiff(rgbref, rgbtst) > maxerr);
                    reason = "different pixel rendered inside clip";
                }
                if (failed) {
                    if (dataref == null) {
                        numerrors++;
                    }
                    if (wasfill) {
                        numfillfailures++;
                    } else {
                        numstrokefailures++;
                    }
                    if (!silent) {
                        System.out.println("Failed: "+reason+" at "+x+", "+y+
                                           " ["+Integer.toHexString(rgbref)+
                                           " != "+Integer.toHexString(rgbtst)+
                                           "]");
                        System.out.print(wasfill ? "Filled " : "Stroked ");
                        if (useAA) System.out.print("AA ");
                        if (strokePure) System.out.print("Pure ");
                        if (lw != 1) System.out.print("Lw="+lw+" ");
                        if (rot != 0) System.out.print("Rot="+rot+" ");
                        System.out.println(ar);
                    }
                    if (showErrors) {
                        show(imgref, imgtst);
                    }
                    return true;
                }
            }
            offref += scanref;
            offtst += scantst;
        }
        return false;
    }

    static ErrorWindow errw;

    public static void show(BufferedImage imgref, BufferedImage imgtst) {
        ErrorWindow errw = new ErrorWindow();
        errw.setImages(imgref, imgtst);
        errw.setVisible(true);
        errw.waitForHide();
        errw.dispose();
    }

    public static class Cubic extends AnnotatedShapeOp {
        public static Cubic tryparse(String str) {
            str = str.trim();
            if (!str.startsWith("Cubic(")) {
                return null;
            }
            str = str.substring(6);
            double coords[] = new double[8];
            boolean foundparen = false;
            for (int i = 0; i < coords.length; i++) {
                int index = str.indexOf(",");
                if (index < 0) {
                    if (i < coords.length-1) {
                        return null;
                    }
                    index = str.indexOf(")");
                    if (index < 0) {
                        return null;
                    }
                    foundparen = true;
                }
                String num = str.substring(0, index);
                try {
                    coords[i] = Double.parseDouble(num);
                } catch (NumberFormatException nfe) {
                    return null;
                }
                str = str.substring(index+1);
            }
            if (!foundparen || str.length() > 0) {
                return null;
            }
            Cubic c = new Cubic();
            c.cubic.setCurve(coords[0], coords[1],
                             coords[2], coords[3],
                             coords[4], coords[5],
                             coords[6], coords[7]);
            return c;
        }

        private CubicCurve2D cubic = new CubicCurve2D.Double();

        public void randomize() {
            cubic.setCurve(randDblCoord(), randDblCoord(),
                           randDblCoord(), randDblCoord(),
                           randDblCoord(), randDblCoord(),
                           randDblCoord(), randDblCoord());
        }

        public Shape getShape() {
            return cubic;
        }

        public String toString() {
            return ("Cubic("+
                    cubic.getX1()+", "+
                    cubic.getY1()+", "+
                    cubic.getCtrlX1()+", "+
                    cubic.getCtrlY1()+", "+
                    cubic.getCtrlX2()+", "+
                    cubic.getCtrlY2()+", "+
                    cubic.getX2()+", "+
                    cubic.getY2()
                    +")");
        }
    }

    public static class Quad extends AnnotatedShapeOp {
        public static Quad tryparse(String str) {
            str = str.trim();
            if (!str.startsWith("Quad(")) {
                return null;
            }
            str = str.substring(5);
            double coords[] = new double[6];
            boolean foundparen = false;
            for (int i = 0; i < coords.length; i++) {
                int index = str.indexOf(",");
                if (index < 0) {
                    if (i < coords.length-1) {
                        return null;
                    }
                    index = str.indexOf(")");
                    if (index < 0) {
                        return null;
                    }
                    foundparen = true;
                }
                String num = str.substring(0, index);
                try {
                    coords[i] = Double.parseDouble(num);
                } catch (NumberFormatException nfe) {
                    return null;
                }
                str = str.substring(index+1);
            }
            if (!foundparen || str.length() > 0) {
                return null;
            }
            Quad c = new Quad();
            c.quad.setCurve(coords[0], coords[1],
                            coords[2], coords[3],
                            coords[4], coords[5]);
            return c;
        }

        private QuadCurve2D quad = new QuadCurve2D.Double();

        public void randomize() {
            quad.setCurve(randDblCoord(), randDblCoord(),
                          randDblCoord(), randDblCoord(),
                          randDblCoord(), randDblCoord());
        }

        public Shape getShape() {
            return quad;
        }

        public String toString() {
            return ("Quad("+
                    quad.getX1()+", "+
                    quad.getY1()+", "+
                    quad.getCtrlX()+", "+
                    quad.getCtrlY()+", "+
                    quad.getX2()+", "+
                    quad.getY2()
                    +")");
        }
    }

    public static class Poly extends AnnotatedShapeOp {
        public static Poly tryparse(String str) {
            str = str.trim();
            if (!str.startsWith("Poly(")) {
                return null;
            }
            str = str.substring(5);
            Polygon p = new Polygon();
            while (true) {
                int x, y;
                str = str.trim();
                if (str.startsWith(")")) {
                    str = str.substring(1);
                    break;
                }
                if (p.npoints > 0) {
                    if (str.startsWith(",")) {
                        str = str.substring(2).trim();
                    } else {
                        return null;
                    }
                }
                if (str.startsWith("[")) {
                    str = str.substring(1);
                } else {
                    return null;
                }
                int index = str.indexOf(",");
                if (index < 0) {
                    return null;
                }
                String num = str.substring(0, index);
                try {
                    x = Integer.parseInt(num);
                } catch (NumberFormatException nfe) {
                    return null;
                }
                str = str.substring(index+1);
                index = str.indexOf("]");
                if (index < 0) {
                    return null;
                }
                num = str.substring(0, index).trim();
                try {
                    y = Integer.parseInt(num);
                } catch (NumberFormatException nfe) {
                    return null;
                }
                str = str.substring(index+1);
                p.addPoint(x, y);
            }
            if (str.length() > 0) {
                return null;
            }
            if (p.npoints < 3) {
                return null;
            }
            return new Poly(p);
        }

        private Polygon poly;

        public Poly() {
            this.poly = new Polygon();
        }

        private Poly(Polygon p) {
            this.poly = p;
        }

        public void randomize() {
            poly.reset();
            poly.addPoint(randIntCoord(), randIntCoord());
            poly.addPoint(randIntCoord(), randIntCoord());
            poly.addPoint(randIntCoord(), randIntCoord());
            poly.addPoint(randIntCoord(), randIntCoord());
            poly.addPoint(randIntCoord(), randIntCoord());
        }

        public Shape getShape() {
            return poly;
        }

        public String toString() {
            StringBuffer sb = new StringBuffer(100);
            sb.append("Poly(");
            for (int i = 0; i < poly.npoints; i++) {
                if (i != 0) {
                    sb.append(", ");
                }
                sb.append("[");
                sb.append(poly.xpoints[i]);
                sb.append(", ");
                sb.append(poly.ypoints[i]);
                sb.append("]");
            }
            sb.append(")");
            return sb.toString();
        }
    }

    public static class Path extends AnnotatedShapeOp {
        public static Path tryparse(String str) {
            str = str.trim();
            if (!str.startsWith("Path(")) {
                return null;
            }
            str = str.substring(5);
            GeneralPath gp = new GeneralPath();
            float coords[] = new float[6];
            int numsegs = 0;
            while (true) {
                int type;
                int n;
                str = str.trim();
                if (str.startsWith(")")) {
                    str = str.substring(1);
                    break;
                }
                if (str.startsWith("M[")) {
                    type = PathIterator.SEG_MOVETO;
                    n = 2;
                } else if (str.startsWith("L[")) {
                    type = PathIterator.SEG_LINETO;
                    n = 2;
                } else if (str.startsWith("Q[")) {
                    type = PathIterator.SEG_QUADTO;
                    n = 4;
                } else if (str.startsWith("C[")) {
                    type = PathIterator.SEG_CUBICTO;
                    n = 6;
                } else if (str.startsWith("E[")) {
                    type = PathIterator.SEG_CLOSE;
                    n = 0;
                } else {
                    return null;
                }
                str = str.substring(2);
                if (n == 0) {
                    if (str.startsWith("]")) {
                        str = str.substring(1);
                    } else {
                        return null;
                    }
                }
                for (int i = 0; i < n; i++) {
                    int index;
                    if (i < n-1) {
                        index = str.indexOf(",");
                    } else {
                        index = str.indexOf("]");
                    }
                    if (index < 0) {
                        return null;
                    }
                    String num = str.substring(0, index);
                    try {
                        coords[i] = Float.parseFloat(num);
                    } catch (NumberFormatException nfe) {
                        return null;
                    }
                    str = str.substring(index+1).trim();
                }
                switch (type) {
                case PathIterator.SEG_MOVETO:
                    gp.moveTo(coords[0], coords[1]);
                    break;
                case PathIterator.SEG_LINETO:
                    gp.lineTo(coords[0], coords[1]);
                    break;
                case PathIterator.SEG_QUADTO:
                    gp.quadTo(coords[0], coords[1],
                              coords[2], coords[3]);
                    break;
                case PathIterator.SEG_CUBICTO:
                    gp.curveTo(coords[0], coords[1],
                               coords[2], coords[3],
                               coords[4], coords[5]);
                    break;
                case PathIterator.SEG_CLOSE:
                    gp.closePath();
                    break;
                }
                numsegs++;
            }
            if (str.length() > 0) {
                return null;
            }
            if (numsegs < 2) {
                return null;
            }
            return new Path(gp);
        }

        private GeneralPath path;

        public Path() {
            this.path = new GeneralPath();
        }

        private Path(GeneralPath gp) {
            this.path = gp;
        }

        public void randomize() {
            path.reset();
            path.moveTo(randFltCoord(), randFltCoord());
            for (int i = randInt(5)+3; i > 0; --i) {
                switch(randInt(5)) {
                case 0:
                    path.moveTo(randFltCoord(), randFltCoord());
                    break;
                case 1:
                    path.lineTo(randFltCoord(), randFltCoord());
                    break;
                case 2:
                    path.quadTo(randFltCoord(), randFltCoord(),
                                randFltCoord(), randFltCoord());
                    break;
                case 3:
                    path.curveTo(randFltCoord(), randFltCoord(),
                                 randFltCoord(), randFltCoord(),
                                 randFltCoord(), randFltCoord());
                    break;
                case 4:
                    path.closePath();
                    break;
                }
            }
        }

        public Shape getShape() {
            return path;
        }

        public String toString() {
            StringBuffer sb = new StringBuffer(100);
            sb.append("Path(");
            PathIterator pi = path.getPathIterator(null);
            float coords[] = new float[6];
            boolean first = true;
            while (!pi.isDone()) {
                int n;
                char c;
                switch(pi.currentSegment(coords)) {
                case PathIterator.SEG_MOVETO:
                    c = 'M';
                    n = 2;
                    break;
                case PathIterator.SEG_LINETO:
                    c = 'L';
                    n = 2;
                    break;
                case PathIterator.SEG_QUADTO:
                    c = 'Q';
                    n = 4;
                    break;
                case PathIterator.SEG_CUBICTO:
                    c = 'C';
                    n = 6;
                    break;
                case PathIterator.SEG_CLOSE:
                    c = 'E';
                    n = 0;
                    break;
                default:
                    throw new InternalError("Unknown segment!");
                }
                sb.append(c);
                sb.append("[");
                for (int i = 0; i < n; i++) {
                    if (i != 0) {
                        sb.append(",");
                    }
                    sb.append(coords[i]);
                }
                sb.append("]");
                pi.next();
            }
            sb.append(")");
            return sb.toString();
        }
    }

    public static class Rect extends AnnotatedShapeOp {
        public static Rect tryparse(String str) {
            str = str.trim();
            if (!str.startsWith("Rect(")) {
                return null;
            }
            str = str.substring(5);
            double coords[] = new double[4];
            boolean foundparen = false;
            for (int i = 0; i < coords.length; i++) {
                int index = str.indexOf(",");
                if (index < 0) {
                    if (i < coords.length-1) {
                        return null;
                    }
                    index = str.indexOf(")");
                    if (index < 0) {
                        return null;
                    }
                    foundparen = true;
                }
                String num = str.substring(0, index);
                try {
                    coords[i] = Double.parseDouble(num);
                } catch (NumberFormatException nfe) {
                    return null;
                }
                str = str.substring(index+1);
            }
            if (!foundparen || str.length() > 0) {
                return null;
            }
            Rect r = new Rect();
            r.rect.setRect(coords[0], coords[1],
                           coords[2], coords[3]);
            return r;
        }

        private Rectangle2D rect = new Rectangle2D.Double();

        public void randomize() {
            rect.setRect(randDblCoord(), randDblCoord(),
                         randDblCoord(), randDblCoord());
        }

        public Shape getShape() {
            return rect;
        }

        public String toString() {
            return ("Rect("+
                    rect.getX()+", "+
                    rect.getY()+", "+
                    rect.getWidth()+", "+
                    rect.getHeight()
                    +")");
        }
    }

    public static class Line extends AnnotatedShapeOp {
        public static Line tryparse(String str) {
            str = str.trim();
            if (!str.startsWith("Line(")) {
                return null;
            }
            str = str.substring(5);
            double coords[] = new double[4];
            boolean foundparen = false;
            for (int i = 0; i < coords.length; i++) {
                int index = str.indexOf(",");
                if (index < 0) {
                    if (i < coords.length-1) {
                        return null;
                    }
                    index = str.indexOf(")");
                    if (index < 0) {
                        return null;
                    }
                    foundparen = true;
                }
                String num = str.substring(0, index);
                try {
                    coords[i] = Double.parseDouble(num);
                } catch (NumberFormatException nfe) {
                    return null;
                }
                str = str.substring(index+1);
            }
            if (!foundparen || str.length() > 0) {
                return null;
            }
            Line l = new Line();
            l.line.setLine(coords[0], coords[1],
                           coords[2], coords[3]);
            return l;
        }

        private Line2D line = new Line2D.Double();

        public void randomize() {
            line.setLine(randDblCoord(), randDblCoord(),
                         randDblCoord(), randDblCoord());
        }

        public Shape getShape() {
            return line;
        }

        public String toString() {
            return ("Line("+
                    line.getX1()+", "+
                    line.getY1()+", "+
                    line.getX2()+", "+
                    line.getY2()
                    +")");
        }
    }

    public static class RectMethod extends AnnotatedRenderOp {
        public static RectMethod tryparse(String str) {
            str = str.trim();
            if (!str.startsWith("RectMethod(")) {
                return null;
            }
            str = str.substring(11);
            int coords[] = new int[4];
            boolean foundparen = false;
            for (int i = 0; i < coords.length; i++) {
                int index = str.indexOf(",");
                if (index < 0) {
                    if (i < coords.length-1) {
                        return null;
                    }
                    index = str.indexOf(")");
                    if (index < 0) {
                        return null;
                    }
                    foundparen = true;
                }
                String num = str.substring(0, index).trim();
                try {
                    coords[i] = Integer.parseInt(num);
                } catch (NumberFormatException nfe) {
                    return null;
                }
                str = str.substring(index+1);
            }
            if (!foundparen || str.length() > 0) {
                return null;
            }
            RectMethod rm = new RectMethod();
            rm.rect.setBounds(coords[0], coords[1],
                              coords[2], coords[3]);
            return rm;
        }

        private Rectangle rect = new Rectangle();

        public void randomize() {
            rect.setBounds(randIntCoord(), randIntCoord(),
                           randIntCoord(), randIntCoord());
        }

        public void fill(Graphics2D g2d) {
            g2d.fillRect(rect.x, rect.y, rect.width, rect.height);
        }

        public void draw(Graphics2D g2d) {
            g2d.drawRect(rect.x, rect.y, rect.width, rect.height);
        }

        public String toString() {
            return ("RectMethod("+
                    rect.x+", "+
                    rect.y+", "+
                    rect.width+", "+
                    rect.height
                    +")");
        }
    }

    public static class LineMethod extends AnnotatedRenderOp {
        public static LineMethod tryparse(String str) {
            str = str.trim();
            if (!str.startsWith("LineMethod(")) {
                return null;
            }
            str = str.substring(11);
            int coords[] = new int[4];
            boolean foundparen = false;
            for (int i = 0; i < coords.length; i++) {
                int index = str.indexOf(",");
                if (index < 0) {
                    if (i < coords.length-1) {
                        return null;
                    }
                    index = str.indexOf(")");
                    if (index < 0) {
                        return null;
                    }
                    foundparen = true;
                }
                String num = str.substring(0, index).trim();
                try {
                    coords[i] = Integer.parseInt(num);
                } catch (NumberFormatException nfe) {
                    return null;
                }
                str = str.substring(index+1);
            }
            if (!foundparen || str.length() > 0) {
                return null;
            }
            LineMethod lm = new LineMethod();
            lm.line = coords;
            return lm;
        }

        private int line[] = new int[4];

        public void randomize() {
            line[0] = randIntCoord();
            line[1] = randIntCoord();
            line[2] = randIntCoord();
            line[3] = randIntCoord();
        }

        public void fill(Graphics2D g2d) {
        }

        public void draw(Graphics2D g2d) {
            g2d.drawLine(line[0], line[1], line[2], line[3]);
        }

        public String toString() {
            return ("LineMethod("+
                    line[0]+", "+
                    line[1]+", "+
                    line[2]+", "+
                    line[3]
                    +")");
        }
    }

    public static class ErrorWindow extends Frame {
        ImageCanvas unclipped;
        ImageCanvas reference;
        ImageCanvas actual;
        ImageCanvas diff;

        public ErrorWindow() {
            super("Error Comparison Window");

            unclipped = new ImageCanvas();
            reference = new ImageCanvas();
            actual = new ImageCanvas();
            diff = new ImageCanvas();

            setLayout(new SmartGridLayout(0, 2, 5, 5));
            addImagePanel(unclipped, "Unclipped rendering");
            addImagePanel(reference, "Clipped reference");
            addImagePanel(actual, "Actual clipped");
            addImagePanel(diff, "Difference");

            addWindowListener(new WindowAdapter() {
                public void windowClosing(WindowEvent e) {
                    setVisible(false);
                }
            });
        }

        public void addImagePanel(ImageCanvas ic, String label) {
            add(ic);
            add(new Label(label));
        }

        public void setImages(BufferedImage imgref, BufferedImage imgtst) {
            unclipped.setImage(imgref);
            reference.setReference(imgref);
            actual.setImage(imgtst);
            diff.setDiff(reference.getImage(), imgtst);
            invalidate();
            pack();
            repaint();
        }

        public void setVisible(boolean vis) {
            super.setVisible(vis);
            synchronized (this) {
                notifyAll();
            }
        }

        public synchronized void waitForHide() {
            while (isShowing()) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    System.exit(2);
                }
            }
        }
    }

    public static class SmartGridLayout implements LayoutManager {
        int rows;
        int cols;
        int hgap;
        int vgap;

        public SmartGridLayout(int r, int c, int h, int v) {
            this.rows = r;
            this.cols = c;
            this.hgap = h;
            this.vgap = v;
        }

        public void addLayoutComponent(String name, Component comp) {
        }

        public void removeLayoutComponent(Component comp) {
        }

        public int[][] getGridSizes(Container parent, boolean min) {
            int ncomponents = parent.getComponentCount();
            int nrows = rows;
            int ncols = cols;

            if (nrows > 0) {
                ncols = (ncomponents + nrows - 1) / nrows;
            } else {
                nrows = (ncomponents + ncols - 1) / ncols;
            }
            int widths[] = new int[ncols+1];
            int heights[] = new int[nrows+1];
            int x = 0;
            int y = 0;
            for (int i = 0 ; i < ncomponents ; i++) {
                Component comp = parent.getComponent(i);
                Dimension d = (min
                               ? comp.getMinimumSize()
                               : comp.getPreferredSize());
                if (widths[x] < d.width) {
                    widths[x] = d.width;
                }
                if (heights[y] < d.height) {
                    heights[y] = d.height;
                }
                x++;
                if (x >= ncols) {
                    x = 0;
                    y++;
                }
            }
            for (int i = 0; i < ncols; i++) {
                widths[ncols] += widths[i];
            }
            for (int i = 0; i < nrows; i++) {
                heights[nrows] += heights[i];
            }
            return new int[][] { widths, heights };
        }

        public Dimension getSize(Container parent, boolean min) {
            int sizes[][] = getGridSizes(parent, min);
            int widths[] = sizes[0];
            int heights[] = sizes[1];
            int nrows = heights.length-1;
            int ncols = widths.length-1;
            int w = widths[ncols];
            int h = heights[nrows];
            Insets insets = parent.getInsets();
            return new Dimension(insets.left+insets.right + w+(ncols+1)*hgap,
                                 insets.top+insets.bottom + h+(nrows+1)*vgap);
        }

        public Dimension preferredLayoutSize(Container parent) {
            return getSize(parent, false);
        }

        public Dimension minimumLayoutSize(Container parent) {
            return getSize(parent, true);
        }

        public void layoutContainer(Container parent) {
            int pref[][] = getGridSizes(parent, false);
            int min[][] = getGridSizes(parent, true);
            int minwidths[] = min[0];
            int minheights[] = min[1];
            int prefwidths[] = pref[0];
            int prefheights[] = pref[1];
            int nrows = minheights.length - 1;
            int ncols = minwidths.length - 1;
            Insets insets = parent.getInsets();
            int w = parent.getWidth() - insets.left - insets.right;
            int h = parent.getHeight() - insets.top - insets.bottom;
            w = w - (ncols+1)*hgap;
            h = h - (nrows+1)*vgap;
            int widths[] = calculateSizes(w, ncols, minwidths, prefwidths);
            int heights[] = calculateSizes(h, nrows, minheights, prefheights);
            int ncomponents = parent.getComponentCount();
            int x = insets.left + hgap;
            int y = insets.top + vgap;
            int r = 0;
            int c = 0;
            for (int i = 0; i < ncomponents; i++) {
                parent.getComponent(i).setBounds(x, y, widths[c], heights[r]);
                x += widths[c++] + hgap;
                if (c >= ncols) {
                    c = 0;
                    x = insets.left + hgap;
                    y += heights[r++] + vgap;
                    if (r >= nrows) {
                        // just in case
                        break;
                    }
                }
            }
        }

        public static int[] calculateSizes(int total, int num,
                                           int minsizes[], int prefsizes[])
        {
            if (total <= minsizes[num]) {
                return minsizes;
            }
            if (total >= prefsizes[num]) {
                return prefsizes;
            }
            int sizes[] = new int[total];
            int prevhappy = 0;
            int nhappy = 0;
            int happysize = 0;
            do {
                int addsize = (total - happysize) / (num - nhappy);
                happysize = 0;
                for (int i = 0; i < num; i++) {
                    if (sizes[i] >= prefsizes[i] ||
                        minsizes[i] + addsize > prefsizes[i])
                    {
                        happysize += (sizes[i] = prefsizes[i]);
                        nhappy++;
                    } else {
                        sizes[i] = minsizes[i] + addsize;
                    }
                }
            } while (nhappy < num && nhappy > prevhappy);
            return sizes;
        }
    }

    public static class ImageCanvas extends Canvas {
        BufferedImage image;

        public void setImage(BufferedImage img) {
            this.image = img;
        }

        public BufferedImage getImage() {
            return image;
        }

        public void checkImage(int w, int h) {
            if (image == null ||
                image.getWidth() < w ||
                image.getHeight() < h)
            {
                image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            }
        }

        public void setReference(BufferedImage img) {
            checkImage(img.getWidth(), img.getHeight());
            Graphics g = image.createGraphics();
            g.drawImage(img, 0, 0, null);
            g.setColor(Color.white);
            g.fillRect(0, 0, 30, 10);
            g.fillRect(30, 0, 10, 30);
            g.fillRect(10, 30, 30, 10);
            g.fillRect(0, 10, 10, 30);
            g.dispose();
        }

        public void setDiff(BufferedImage imgref, BufferedImage imgtst) {
            int w = Math.max(imgref.getWidth(), imgtst.getWidth());
            int h = Math.max(imgref.getHeight(), imgtst.getHeight());
            checkImage(w, h);
            Graphics g = image.createGraphics();
            g.drawImage(imgref, 0, 0, null);
            g.setXORMode(Color.white);
            g.drawImage(imgtst, 0, 0, null);
            g.setPaintMode();
            g.setColor(new Color(1f, 1f, 0f, 0.25f));
            g.fillRect(10, 10, 20, 20);
            g.setColor(new Color(1f, 0f, 0f, 0.25f));
            g.fillRect(0, 0, 30, 10);
            g.fillRect(30, 0, 10, 30);
            g.fillRect(10, 30, 30, 10);
            g.fillRect(0, 10, 10, 30);
            g.dispose();
        }

        public Dimension getPreferredSize() {
            if (image == null) {
                return new Dimension();
            } else {
                return new Dimension(image.getWidth(), image.getHeight());
            }
        }

        public void paint(Graphics g) {
            g.drawImage(image, 0, 0, null);
        }
    }
}
