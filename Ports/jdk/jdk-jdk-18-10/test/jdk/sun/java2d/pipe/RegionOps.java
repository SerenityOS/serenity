/*
 * Copyright (c) 2009, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @test %W% %E%
 * @bug 6504874
 * @summary This test verifies the operation (and performance) of the
 *          various CAG operations on the internal Region class.
 * @modules java.desktop/sun.java2d.pipe
 * @run main RegionOps
 */

import java.awt.Rectangle;
import java.awt.geom.Area;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.Random;
import sun.java2d.pipe.Region;

public class RegionOps {
    public static final int DEFAULT_NUMREGIONS = 50;
    public static final int DEFAULT_MINSUBRECTS = 1;
    public static final int DEFAULT_MAXSUBRECTS = 10;

    public static final int MINCOORD = -20;
    public static final int MAXCOORD = 20;

    public static boolean useArea;

    static int numops;
    static int numErrors;
    static Random rand = new Random();
    static boolean skipCheck;
    static boolean countErrors;

    static {
        // Instantiating BufferedImage initializes sun.java2d
        BufferedImage bimg =
            new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
    }

    public static void usage(String error) {
        if (error != null) {
            System.err.println("Error: "+error);
        }
        System.err.println("Usage: java RegionOps "+
                           "[-regions N] [-rects M] "+
                           "[-[min|max]rects M] [-area]");
        System.err.println("                      "+
                           "[-add|union] [-sub|diff] "+
                           "[-int[ersect]] [-xor]");
        System.err.println("                      "+
                           "[-seed S] [-nocheck] [-count[errors]] [-help]");
        System.exit((error != null) ? 1 : 0);
    }

    public static void error(RectListImpl a, RectListImpl b, String problem) {
        System.err.println("Operating on:  "+a);
        if (b != null) {
            System.err.println("and:  "+b);
        }
        if (countErrors) {
            System.err.println(problem);
            numErrors++;
        } else {
            throw new RuntimeException(problem);
        }
    }

    public static void main(String argv[]) {
        int numregions = DEFAULT_NUMREGIONS;
        int minsubrects = DEFAULT_MINSUBRECTS;
        int maxsubrects = DEFAULT_MAXSUBRECTS;
        boolean doUnion = false;
        boolean doIntersect = false;
        boolean doSubtract = false;
        boolean doXor = false;

        for (int i = 0; i < argv.length; i++) {
            String arg = argv[i];
            if (arg.equalsIgnoreCase("-regions")) {
                if (i+1 >= argv.length) {
                    usage("missing arg for -regions");
                }
                numregions = Integer.parseInt(argv[++i]);
            } else if (arg.equalsIgnoreCase("-rects")) {
                if (i+1 >= argv.length) {
                    usage("missing arg for -rects");
                }
                minsubrects = maxsubrects = Integer.parseInt(argv[++i]);
            } else if (arg.equalsIgnoreCase("-minrects")) {
                if (i+1 >= argv.length) {
                    usage("missing arg for -minrects");
                }
                minsubrects = Integer.parseInt(argv[++i]);
            } else if (arg.equalsIgnoreCase("-maxrects")) {
                if (i+1 >= argv.length) {
                    usage("missing arg for -maxrects");
                }
                maxsubrects = Integer.parseInt(argv[++i]);
            } else if (arg.equalsIgnoreCase("-area")) {
                useArea = true;
            } else if (arg.equalsIgnoreCase("-add") ||
                       arg.equalsIgnoreCase("-union"))
            {
                doUnion = true;
            } else if (arg.equalsIgnoreCase("-sub") ||
                       arg.equalsIgnoreCase("-diff"))
            {
                doSubtract = true;
            } else if (arg.equalsIgnoreCase("-int") ||
                       arg.equalsIgnoreCase("-intersect"))
            {
                doIntersect = true;
            } else if (arg.equalsIgnoreCase("-xor")) {
                doXor = true;
            } else if (arg.equalsIgnoreCase("-seed")) {
                if (i+1 >= argv.length) {
                    usage("missing arg for -seed");
                }
                rand.setSeed(Long.decode(argv[++i]).longValue());
            } else if (arg.equalsIgnoreCase("-nocheck")) {
                skipCheck = true;
            } else if (arg.equalsIgnoreCase("-count") ||
                       arg.equalsIgnoreCase("-counterrors"))
            {
                countErrors = true;
            } else if (arg.equalsIgnoreCase("-help")) {
                usage(null);
            } else {
                usage("Unknown argument: "+arg);
            }
        }

        if (maxsubrects < minsubrects) {
            usage("maximum number of subrectangles less than minimum");
        }

        if (minsubrects <= 0) {
            usage("minimum number of subrectangles must be positive");
        }

        if (!doUnion && !doSubtract && !doIntersect && !doXor) {
            doUnion = doSubtract = doIntersect = doXor = true;
        }

        long start = System.currentTimeMillis();
        RectListImpl rlist[] = new RectListImpl[numregions];
        int totalrects = 0;
        for (int i = 0; i < rlist.length; i++) {
            RectListImpl rli = RectListImpl.getInstance();
            int numsubrects =
                minsubrects + rand.nextInt(maxsubrects - minsubrects + 1);
            for (int j = 0; j < numsubrects; j++) {
                addRectTo(rli);
                totalrects++;
            }
            rlist[i] = rli;
        }
        long end = System.currentTimeMillis();
        System.out.println((end-start)+"ms to create "+
                           rlist.length+" regions containing "+
                           totalrects+" subrectangles");

        start = System.currentTimeMillis();
        for (int i = 0; i < rlist.length; i++) {
            RectListImpl a = rlist[i];
            testTranslate(a);
            for (int j = i; j < rlist.length; j++) {
                RectListImpl b = rlist[j];
                if (doUnion) testUnion(a, b);
                if (doSubtract) testDifference(a, b);
                if (doIntersect) testIntersection(a, b);
                if (doXor) testExclusiveOr(a, b);
            }
        }
        end = System.currentTimeMillis();
        System.out.println(numops+" ops took "+(end-start)+"ms");

        if (numErrors > 0) {
            throw new RuntimeException(numErrors+" errors encountered");
        }
    }

    public static void addRectTo(RectListImpl rli) {
        int lox = MINCOORD + rand.nextInt(MAXCOORD - MINCOORD + 1);
        int hix = MINCOORD + rand.nextInt(MAXCOORD - MINCOORD + 1);
        int loy = MINCOORD + rand.nextInt(MAXCOORD - MINCOORD + 1);
        int hiy = MINCOORD + rand.nextInt(MAXCOORD - MINCOORD + 1);
        rli.addRect(lox, loy, hix, hiy);
    }

    public static void checkEqual(RectListImpl a, RectListImpl b,
                                  String optype)
    {
        if (a.hashCode() != b.hashCode()) {
            error(a, b, "hashcode failed for "+optype);
        }
        if (!a.equals(b)) {
            error(a, b, "equals failed for "+optype);
        }
    }

    public static void testTranslate(RectListImpl a) {
        RectListImpl maxTrans =
            a.getTranslation(Integer.MAX_VALUE, Integer.MAX_VALUE)
            .getTranslation(Integer.MAX_VALUE, Integer.MAX_VALUE)
            .getTranslation(Integer.MAX_VALUE, Integer.MAX_VALUE);
        if (!maxTrans.checkTransEmpty()) {
            error(maxTrans, null, "overflow translated RectList not empty");
        }
        RectListImpl minTrans =
            a.getTranslation(Integer.MIN_VALUE, Integer.MIN_VALUE)
            .getTranslation(Integer.MIN_VALUE, Integer.MIN_VALUE)
            .getTranslation(Integer.MIN_VALUE, Integer.MIN_VALUE);
        if (!minTrans.checkTransEmpty()) {
            error(minTrans, null, "overflow translated RectList not empty");
        }
        testTranslate(a, Integer.MAX_VALUE, Integer.MAX_VALUE, false,
                      MINCOORD, 0, MINCOORD, 0);
        testTranslate(a, Integer.MAX_VALUE, Integer.MIN_VALUE, false,
                      MINCOORD, 0, 0, MAXCOORD);
        testTranslate(a, Integer.MIN_VALUE, Integer.MAX_VALUE, false,
                      0, MAXCOORD, MINCOORD, 0);
        testTranslate(a, Integer.MIN_VALUE, Integer.MIN_VALUE, false,
                      0, MAXCOORD, 0, MAXCOORD);
        for (int dy = -100; dy <= 100; dy += 50) {
            for (int dx = -100; dx <= 100; dx += 50) {
                testTranslate(a, dx, dy, true,
                              MINCOORD, MAXCOORD,
                              MINCOORD, MAXCOORD);
            }
        }
    }

    public static void testTranslate(RectListImpl a, int dx, int dy,
                                     boolean isNonDestructive,
                                     int xmin, int xmax,
                                     int ymin, int ymax)
    {
        RectListImpl theTrans = a.getTranslation(dx, dy); numops++;
        if (skipCheck) return;
        RectListImpl unTrans = theTrans.getTranslation(-dx, -dy);
        if (isNonDestructive) checkEqual(a, unTrans, "Translate");
        for (int x = xmin; x < xmax; x++) {
            for (int y = ymin; y < ymax; y++) {
                boolean inside = a.contains(x, y);
                if (theTrans.contains(x+dx, y+dy) != inside) {
                    error(a, null, "translation failed for "+
                          dx+", "+dy+" at "+x+", "+y);
                }
            }
        }
    }

    public static void testUnion(RectListImpl a, RectListImpl b) {
        RectListImpl aUb = a.getUnion(b); numops++;
        RectListImpl bUa = b.getUnion(a); numops++;
        if (skipCheck) return;
        checkEqual(aUb, bUa, "Union");
        testUnion(a, b, aUb);
        testUnion(a, b, bUa);
    }

    public static void testUnion(RectListImpl a, RectListImpl b,
                                 RectListImpl theUnion)
    {
        for (int x = MINCOORD; x < MAXCOORD; x++) {
            for (int y = MINCOORD; y < MAXCOORD; y++) {
                boolean inside = (a.contains(x, y) || b.contains(x, y));
                if (theUnion.contains(x, y) != inside) {
                    error(a, b, "union failed at "+x+", "+y);
                }
            }
        }
    }

    public static void testDifference(RectListImpl a, RectListImpl b) {
        RectListImpl aDb = a.getDifference(b); numops++;
        RectListImpl bDa = b.getDifference(a); numops++;
        if (skipCheck) return;
        // Note that difference is not commutative so we cannot check equals
        // checkEqual(a, b, "Difference");
        testDifference(a, b, aDb);
        testDifference(b, a, bDa);
    }

    public static void testDifference(RectListImpl a, RectListImpl b,
                                      RectListImpl theDifference)
    {
        for (int x = MINCOORD; x < MAXCOORD; x++) {
            for (int y = MINCOORD; y < MAXCOORD; y++) {
                boolean inside = (a.contains(x, y) && !b.contains(x, y));
                if (theDifference.contains(x, y) != inside) {
                    error(a, b, "difference failed at "+x+", "+y);
                }
            }
        }
    }

    public static void testIntersection(RectListImpl a, RectListImpl b) {
        RectListImpl aIb = a.getIntersection(b); numops++;
        RectListImpl bIa = b.getIntersection(a); numops++;
        if (skipCheck) return;
        checkEqual(aIb, bIa, "Intersection");
        testIntersection(a, b, aIb);
        testIntersection(a, b, bIa);
    }

    public static void testIntersection(RectListImpl a, RectListImpl b,
                                        RectListImpl theIntersection)
    {
        for (int x = MINCOORD; x < MAXCOORD; x++) {
            for (int y = MINCOORD; y < MAXCOORD; y++) {
                boolean inside = (a.contains(x, y) && b.contains(x, y));
                if (theIntersection.contains(x, y) != inside) {
                    error(a, b, "intersection failed at "+x+", "+y);
                }
            }
        }
    }

    public static void testExclusiveOr(RectListImpl a, RectListImpl b) {
        RectListImpl aXb = a.getExclusiveOr(b); numops++;
        RectListImpl bXa = b.getExclusiveOr(a); numops++;
        if (skipCheck) return;
        checkEqual(aXb, bXa, "ExclusiveOr");
        testExclusiveOr(a, b, aXb);
        testExclusiveOr(a, b, bXa);
    }

    public static void testExclusiveOr(RectListImpl a, RectListImpl b,
                                       RectListImpl theExclusiveOr)
    {
        for (int x = MINCOORD; x < MAXCOORD; x++) {
            for (int y = MINCOORD; y < MAXCOORD; y++) {
                boolean inside = (a.contains(x, y) != b.contains(x, y));
                if (theExclusiveOr.contains(x, y) != inside) {
                    error(a, b, "xor failed at "+x+", "+y);
                }
            }
        }
    }

    public abstract static class RectListImpl {
        public static RectListImpl getInstance() {
            if (useArea) {
                return new AreaImpl();
            } else {
                return new RegionImpl();
            }
        }

        public abstract void addRect(int lox, int loy, int hix, int hiy);

        public abstract RectListImpl getTranslation(int dx, int dy);

        public abstract RectListImpl getIntersection(RectListImpl rli);
        public abstract RectListImpl getExclusiveOr(RectListImpl rli);
        public abstract RectListImpl getDifference(RectListImpl rli);
        public abstract RectListImpl getUnion(RectListImpl rli);

        // Used for making sure that 3xMAX translates yields an empty region
        public abstract boolean checkTransEmpty();

        public abstract boolean contains(int x, int y);

        public abstract int hashCode();
        public abstract boolean equals(RectListImpl other);
    }

    public static class AreaImpl extends RectListImpl {
        Area theArea;

        public AreaImpl() {
        }

        public AreaImpl(Area a) {
            theArea = a;
        }

        public void addRect(int lox, int loy, int hix, int hiy) {
            Area a2 = new Area(new Rectangle(lox, loy, hix-lox, hiy-loy));
            if (theArea == null) {
                theArea = a2;
            } else {
                theArea.add(a2);
            }
        }

        public RectListImpl getTranslation(int dx, int dy) {
            AffineTransform at = AffineTransform.getTranslateInstance(dx, dy);
            return new AreaImpl(theArea.createTransformedArea(at));
        }

        public RectListImpl getIntersection(RectListImpl rli) {
            Area a2 = new Area(theArea);
            a2.intersect(((AreaImpl) rli).theArea);
            return new AreaImpl(a2);
        }

        public RectListImpl getExclusiveOr(RectListImpl rli) {
            Area a2 = new Area(theArea);
            a2.exclusiveOr(((AreaImpl) rli).theArea);
            return new AreaImpl(a2);
        }

        public RectListImpl getDifference(RectListImpl rli) {
            Area a2 = new Area(theArea);
            a2.subtract(((AreaImpl) rli).theArea);
            return new AreaImpl(a2);
        }

        public RectListImpl getUnion(RectListImpl rli) {
            Area a2 = new Area(theArea);
            a2.add(((AreaImpl) rli).theArea);
            return new AreaImpl(a2);
        }

        // Used for making sure that 3xMAX translates yields an empty region
        public boolean checkTransEmpty() {
            // Area objects will actually survive 3 MAX translates so just
            // pretend that it had the intended effect...
            return true;
        }

        public boolean contains(int x, int y) {
            return theArea.contains(x, y);
        }

        public int hashCode() {
            // Area does not override hashCode...
            return 0;
        }

        public boolean equals(RectListImpl other) {
            return theArea.equals(((AreaImpl) other).theArea);
        }

        public String toString() {
            return theArea.toString();
        }
    }

    public static class RegionImpl extends RectListImpl {
        Region theRegion;

        public RegionImpl() {
        }

        public RegionImpl(Region r) {
            theRegion = r;
        }

        public void addRect(int lox, int loy, int hix, int hiy) {
            Region r2 = Region.getInstanceXYXY(lox, loy, hix, hiy);
            if (theRegion == null) {
                theRegion = r2;
            } else {
                theRegion = theRegion.getUnion(r2);
            }
        }

        public RectListImpl getTranslation(int dx, int dy) {
            return new RegionImpl(theRegion.getTranslatedRegion(dx, dy));
        }

        public RectListImpl getIntersection(RectListImpl rli) {
            Region r2 = ((RegionImpl) rli).theRegion;
            r2 = theRegion.getIntersection(r2);
            return new RegionImpl(r2);
        }

        public RectListImpl getExclusiveOr(RectListImpl rli) {
            Region r2 = ((RegionImpl) rli).theRegion;
            r2 = theRegion.getExclusiveOr(r2);
            return new RegionImpl(r2);
        }

        public RectListImpl getDifference(RectListImpl rli) {
            Region r2 = ((RegionImpl) rli).theRegion;
            r2 = theRegion.getDifference(r2);
            return new RegionImpl(r2);
        }

        public RectListImpl getUnion(RectListImpl rli) {
            Region r2 = ((RegionImpl) rli).theRegion;
            r2 = theRegion.getUnion(r2);
            return new RegionImpl(r2);
        }

        // Used for making sure that 3xMAX translates yields an empty region
        public boolean checkTransEmpty() {
            // Region objects should be empty after 3 MAX translates...
            return theRegion.isEmpty();
        }

        public boolean contains(int x, int y) {
            return theRegion.contains(x, y);
        }

        public int hashCode() {
            return theRegion.hashCode();
        }

        public boolean equals(RectListImpl other) {
            return theRegion.equals(((RegionImpl) other).theRegion);
        }

        public String toString() {
            return theRegion.toString();
        }
    }
}
