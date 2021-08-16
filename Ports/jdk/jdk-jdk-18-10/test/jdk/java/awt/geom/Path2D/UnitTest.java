/*
 * Copyright (c) 2006, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4172661
 * @summary Tests all public methods of Path2D classes on all 3 variants
 *          Path2D.Float, Path2D.Double, and GeneralPath.
 *          REMIND: Note that the hit testing tests will fail
 *          occasionally due to precision bugs in the various hit
 *          testing methods in the geometry classes.
 *          (Failure rates vary from 1 per 100 runs to 1 per thousands).
 *          See bug 6396047 to track progress on these failures.
 */

import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Area;
import java.awt.geom.CubicCurve2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.FlatteningPathIterator;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.QuadCurve2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.util.NoSuchElementException;

public class UnitTest {
    public static boolean verbose;

    public static final int WIND_NON_ZERO = PathIterator.WIND_NON_ZERO;
    public static final int WIND_EVEN_ODD = PathIterator.WIND_EVEN_ODD;

    public static int CoordsForType[] = { 2, 2, 4, 6, 0 };

    public static AffineTransform TxIdentity = new AffineTransform();
    public static AffineTransform TxComplex = makeAT();

    public static Shape TestShapes[];
    public static SampleShape ShortSampleNonZero;
    public static SampleShape ShortSampleEvenOdd;
    public static SampleShape LongSampleNonZero;
    public static SampleShape LongSampleEvenOdd;

    public static Shape EmptyShapeNonZero =
        new EmptyShape(WIND_NON_ZERO);
    public static Shape EmptyShapeEvenOdd =
        new EmptyShape(WIND_EVEN_ODD);

    // Note: We pick a shape that is not anywhere near any of
    // our test shapes so that the Path2D does not try to collapse
    // out the connecting segment - an optimization that is too
    // difficult to account for in the AppendedShape code.
    public static Shape AppendShape = new Arc2D.Double(1000, 1000, 40, 40,
                                                       Math.PI/4, Math.PI,
                                                       Arc2D.CHORD);

    public static AffineTransform makeAT() {
        AffineTransform at = new AffineTransform();
        at.scale(0.66, 0.23);
        at.rotate(Math.toRadians(35.0));
        at.shear(0.78, 1.32);
        return at;
    }

    public static void init() {
        TestShapes = new Shape[] {
            EmptyShapeNonZero,
            EmptyShapeEvenOdd,
            new Line2D.Double(),
            new Line2D.Double(rpc(), rpc(), rpc(), rpc()),
            new Line2D.Double(rnc(), rnc(), rnc(), rnc()),
            new Rectangle2D.Double(),
            new Rectangle2D.Double(rpc(), rpc(), -1, -1),
            new Rectangle2D.Double(rpc(), rpc(), rd(), rd()),
            new Rectangle2D.Double(rnc(), rnc(), rd(), rd()),
            new Ellipse2D.Double(),
            new Ellipse2D.Double(rpc(), rpc(), -1, -1),
            new Ellipse2D.Double(rpc(), rpc(), rd(), rd()),
            new Ellipse2D.Double(rnc(), rnc(), rd(), rd()),
            new Arc2D.Double(Arc2D.OPEN),
            new Arc2D.Double(Arc2D.CHORD),
            new Arc2D.Double(Arc2D.PIE),
            new Arc2D.Double(rpc(), rpc(), -1, -1, rt(), rt(), Arc2D.OPEN),
            new Arc2D.Double(rpc(), rpc(), -1, -1, rt(), rt(), Arc2D.CHORD),
            new Arc2D.Double(rpc(), rpc(), -1, -1, rt(), rt(), Arc2D.PIE),
            new Arc2D.Double(rpc(), rpc(), rd(), rd(), rt(), rt(), Arc2D.OPEN),
            new Arc2D.Double(rpc(), rpc(), rd(), rd(), rt(), rt(), Arc2D.CHORD),
            new Arc2D.Double(rpc(), rpc(), rd(), rd(), rt(), rt(), Arc2D.PIE),
            new Arc2D.Double(rnc(), rnc(), rd(), rd(), rt(), rt(), Arc2D.OPEN),
            new Arc2D.Double(rnc(), rnc(), rd(), rd(), rt(), rt(), Arc2D.CHORD),
            new Arc2D.Double(rnc(), rnc(), rd(), rd(), rt(), rt(), Arc2D.PIE),
            new RoundRectangle2D.Double(),
            new RoundRectangle2D.Double(rpc(), rpc(), -1, -1, ra(), ra()),
            new RoundRectangle2D.Double(rpc(), rpc(), rd(), rd(), ra(), ra()),
            new RoundRectangle2D.Double(rnc(), rnc(), rd(), rd(), ra(), ra()),
            new QuadCurve2D.Double(),
            new QuadCurve2D.Double(rpc(), rpc(), rpc(), rpc(), rpc(), rpc()),
            new QuadCurve2D.Double(rnc(), rnc(), rnc(), rnc(), rnc(), rnc()),
            new CubicCurve2D.Double(),
            new CubicCurve2D.Double(rpc(), rpc(), rpc(), rpc(),
                                    rpc(), rpc(), rpc(), rpc()),
            new CubicCurve2D.Double(rnc(), rnc(), rnc(), rnc(),
                                    rnc(), rnc(), rnc(), rnc()),
            makeGeneralPath(WIND_NON_ZERO, 1.0),
            makeGeneralPath(WIND_EVEN_ODD, 1.0),
            makeGeneralPath(WIND_NON_ZERO, -1.0),
            makeGeneralPath(WIND_EVEN_ODD, -1.0),
        };

        int types[] = new int[100];
        int i = 0;
        types[i++] = PathIterator.SEG_MOVETO;
        types[i++] = PathIterator.SEG_LINETO;
        types[i++] = PathIterator.SEG_QUADTO;
        types[i++] = PathIterator.SEG_CUBICTO;
        types[i++] = PathIterator.SEG_CLOSE;
        int shortlen = i;
        int prevt = types[i-1];
        while (i < types.length) {
            int t;
            do {
                t = (int) (Math.random() * 5);
            } while (t == prevt &&
                     (t == PathIterator.SEG_MOVETO ||
                      t == PathIterator.SEG_CLOSE));
            types[i++] = t;
            prevt = t;
        }

        int numcoords = 0;
        int numshortcoords = 0;
        for (i = 0; i < types.length; i++) {
            if (i == shortlen) {
                numshortcoords = numcoords;
            }
            numcoords += CoordsForType[types[i]];
        }
        double coords[] = new double[numcoords];
        for (i = 0; i < coords.length; i++) {
            coords[i] = rpc();
        }
        ShortSampleNonZero = new SampleShape(WIND_NON_ZERO,
                                             types, coords,
                                             shortlen, numshortcoords);
        ShortSampleEvenOdd = new SampleShape(WIND_EVEN_ODD,
                                             types, coords,
                                             shortlen, numshortcoords);
        LongSampleNonZero = new SampleShape(WIND_NON_ZERO,
                                            types, coords,
                                            types.length, numcoords);
        LongSampleEvenOdd = new SampleShape(WIND_EVEN_ODD,
                                            types, coords,
                                            types.length, numcoords);
    }

    public static GeneralPath makeGeneralPath(int windingrule, double sign) {
        GeneralPath gp = new GeneralPath(windingrule);
        gp.moveTo((float) (sign * rpc()), (float) (sign * rpc()));
        gp.lineTo((float) (sign * rpc()), (float) (sign * rpc()));
        gp.quadTo((float) (sign * rpc()), (float) (sign * rpc()),
                  (float) (sign * rpc()), (float) (sign * rpc()));
        gp.curveTo((float) (sign * rpc()), (float) (sign * rpc()),
                   (float) (sign * rpc()), (float) (sign * rpc()),
                   (float) (sign * rpc()), (float) (sign * rpc()));
        gp.closePath();
        return gp;
    }

    // Due to odd issues with the sizes of errors when the values
    // being manipulated are near zero, we try to avoid values
    // near zero by ensuring that both the rpc (positive coords)
    // stay away from zero and also by ensuring that the rpc+rd
    // (positive coords + dimensions) stay away from zero.  We
    // also ensure that rnc+rd (negative coords + dimension) stay
    // suitably negative without approaching zero.

    // Random positive coordinate (10 -> 110)
    // rpc + rd gives a total range of (30 -> 170)
    public static double rpc() {
        return (Math.random() * 100.0) + 10.0;
    }

    // Random negative coordinate (-200 -> -100)
    // rnc + rd gives a total range of (-180 -> -40)
    public static double rnc() {
        return (Math.random() * 100.0) - 200.0;
    }

    // Random dimension (20 -> 60)
    public static double rd() {
        return (Math.random() * 40.0) + 20.0;
    }

    // Random arc width/height (0.1 -> 5.1)
    public static double ra() {
        return (Math.random() * 5.0) + 0.1;
    }

    // Random arc angle (theta) (PI/4 => 5PI/4)
    public static double rt() {
        return (Math.random() * Math.PI) + Math.PI/4;
    }

    public static int fltulpdiff(double v1, double v2) {
        if (v1 == v2) {
            return 0;
        }
        float vf1 = (float) v1;
        float vf2 = (float) v2;
        if (vf1 == vf2) {
            return 0;
        }
        float diff = Math.abs(vf1-vf2);
        //float ulp = Math.ulp((float) ((vf1 + vf2)/2f));
        float ulp = Math.max(Math.ulp(vf1), Math.ulp(vf2));
        if (verbose && diff > ulp) {
            System.out.println("v1 = "+vf1+", ulp = "+Math.ulp(vf1));
            System.out.println("v2 = "+vf2+", ulp = "+Math.ulp(vf2));
            System.out.println((diff/ulp)+" ulps");
        }
        return (int) (diff/ulp);
    }

    public static int fltulpless(double v1, double v2) {
        if (v1 >= v2) {
            return 0;
        }
        float vf1 = (float) v1;
        float vf2 = (float) v2;
        if (vf1 >= vf2) {
            return 0;
        }
        float diff = Math.abs(vf1-vf2);
        //float ulp = Math.ulp((float) ((vf1 + vf2)/2f));
        float ulp = Math.max(Math.ulp(vf1), Math.ulp(vf2));
        if (verbose && diff > ulp) {
            System.out.println("v1 = "+vf1+", ulp = "+Math.ulp(vf1));
            System.out.println("v2 = "+vf2+", ulp = "+Math.ulp(vf2));
            System.out.println((diff/ulp)+" ulps");
        }
        return (int) (diff/ulp);
    }

    public static int dblulpdiff(double v1, double v2) {
        if (v1 == v2) {
            return 0;
        }
        double diff = Math.abs(v1-v2);
        //double ulp = Math.ulp((v1 + v2)/2.0);
        double ulp = Math.max(Math.ulp(v1), Math.ulp(v2));
        if (verbose && diff > ulp) {
            System.out.println("v1 = "+v1+", ulp = "+Math.ulp(v1));
            System.out.println("v2 = "+v2+", ulp = "+Math.ulp(v2));
            System.out.println((diff/ulp)+" ulps");
        }
        return (int) (diff/ulp);
    }

    public static abstract class Creator {
        public abstract Path2D makePath();
        public abstract Path2D makePath(int windingrule);
        public abstract Path2D makePath(int windingrule, int capacity);
        public abstract Path2D makePath(Shape s);
        public abstract Path2D makePath(Shape s, AffineTransform at);

        public abstract boolean supportsFloatCompose();
        public abstract int getRecommendedTxMaxUlp();

        public abstract void compare(PathIterator testpi,
                                     PathIterator refpi,
                                     AffineTransform at,
                                     int maxulp);
    }

    public static class FltCreator extends Creator {
        public Path2D makePath() {
            return new Path2D.Float();
        }
        public Path2D makePath(int windingrule) {
            return new Path2D.Float(windingrule);
        }
        public Path2D makePath(int windingrule, int capacity) {
            return new Path2D.Float(windingrule, capacity);
        }
        public Path2D makePath(Shape s) {
            return new Path2D.Float(s);
        }
        public Path2D makePath(Shape s, AffineTransform at) {
            return new Path2D.Float(s, at);
        }

        public boolean supportsFloatCompose() {
            return true;
        }
        public int getRecommendedTxMaxUlp() {
            return 5;
        }

        public void compare(PathIterator testpi,
                            PathIterator refpi,
                            AffineTransform at,
                            int maxulp)
        {
            if (testpi.getWindingRule() != refpi.getWindingRule()) {
                throw new RuntimeException("wrong winding rule");
            }
            float testcoords[] = new float[6];
            float refcoords[] = new float[6];
            while (!testpi.isDone()) {
                if (refpi.isDone()) {
                    throw new RuntimeException("too many segments");
                }
                int testtype = testpi.currentSegment(testcoords);
                int reftype = refpi.currentSegment(refcoords);
                if (testtype != reftype) {
                    throw new RuntimeException("different segment types");
                }
                if (at != null) {
                    at.transform(refcoords, 0, refcoords, 0,
                                 CoordsForType[reftype]/2);
                }
                for (int i = 0; i < CoordsForType[testtype]; i++) {
                    int ulps = fltulpdiff(testcoords[i], refcoords[i]);
                    if (ulps > maxulp) {
                        throw new RuntimeException("coords are different: "+
                                                   testcoords[i]+" != "+
                                                   refcoords[i]+
                                                   " ("+ulps+" ulps)");
                    }
                }
                testpi.next();
                refpi.next();
            }
            if (!refpi.isDone()) {
                throw new RuntimeException("not enough segments");
            }
        }
    }

    public static class DblCreator extends Creator {
        public Path2D makePath() {
            return new Path2D.Double();
        }
        public Path2D makePath(int windingrule) {
            return new Path2D.Double(windingrule);
        }
        public Path2D makePath(int windingrule, int capacity) {
            return new Path2D.Double(windingrule, capacity);
        }
        public Path2D makePath(Shape s) {
            return new Path2D.Double(s);
        }
        public Path2D makePath(Shape s, AffineTransform at) {
            return new Path2D.Double(s, at);
        }

        public boolean supportsFloatCompose() {
            return false;
        }
        public int getRecommendedTxMaxUlp() {
            return 3;
        }

        public void compare(PathIterator testpi,
                            PathIterator refpi,
                            AffineTransform at,
                            int maxulp)
        {
            if (testpi.getWindingRule() != refpi.getWindingRule()) {
                throw new RuntimeException("wrong winding rule");
            }
            double testcoords[] = new double[6];
            double refcoords[] = new double[6];
            while (!testpi.isDone()) {
                if (refpi.isDone()) {
                    throw new RuntimeException("too many segments");
                }
                int testtype = testpi.currentSegment(testcoords);
                int reftype = refpi.currentSegment(refcoords);
                if (testtype != reftype) {
                    throw new RuntimeException("different segment types");
                }
                if (at != null) {
                    at.transform(refcoords, 0, refcoords, 0,
                                 CoordsForType[reftype]/2);
                }
                for (int i = 0; i < CoordsForType[testtype]; i++) {
                    int ulps = dblulpdiff(testcoords[i], refcoords[i]);
                    if (ulps > maxulp) {
                        throw new RuntimeException("coords are different: "+
                                                   testcoords[i]+" != "+
                                                   refcoords[i]+
                                                   " ("+ulps+" ulps)");
                    }
                }
                testpi.next();
                refpi.next();
            }
            if (!refpi.isDone()) {
                throw new RuntimeException("not enough segments");
            }
        }

    }

    public static class GPCreator extends FltCreator {
        public Path2D makePath() {
            return new GeneralPath();
        }
        public Path2D makePath(int windingrule) {
            return new GeneralPath(windingrule);
        }
        public Path2D makePath(int windingrule, int capacity) {
            return new GeneralPath(windingrule, capacity);
        }
        public Path2D makePath(Shape s) {
            return new GeneralPath(s);
        }
        public Path2D makePath(Shape s, AffineTransform at) {
            GeneralPath gp = new GeneralPath();
            PathIterator pi = s.getPathIterator(at);
            gp.setWindingRule(pi.getWindingRule());
            gp.append(pi, false);
            return gp;
        }

        public boolean supportsFloatCompose() {
            return true;
        }
    }

    public static class EmptyShape implements Shape {
        private int windingrule;

        public EmptyShape(int windingrule) {
            this.windingrule = windingrule;
        }

        public Rectangle getBounds() {
            return new Rectangle();
        }
        public Rectangle2D getBounds2D() {
            return new Rectangle();
        }
        public boolean contains(double x, double y) {
            return false;
        }
        public boolean contains(Point2D p) {
            return false;
        }
        public boolean intersects(double x, double y, double w, double h) {
            return false;
        }
        public boolean intersects(Rectangle2D r) {
            return false;
        }
        public boolean contains(double x, double y, double w, double h) {
            return false;
        }
        public boolean contains(Rectangle2D r) {
            return false;
        }
        public PathIterator getPathIterator(AffineTransform at) {
            return new PathIterator() {
                public int getWindingRule() {
                    return windingrule;
                }
                public boolean isDone() {
                    return true;
                }
                public void next() {}
                public int currentSegment(float[] coords) {
                    throw new NoSuchElementException();
                }
                public int currentSegment(double[] coords) {
                    throw new NoSuchElementException();
                }
            };
        }
        public PathIterator getPathIterator(AffineTransform at,
                                            double flatness)
        {
            return getPathIterator(at);
        }
    }

    public static class SampleShape implements Shape {
        int windingrule;
        int theTypes[];
        double theCoords[];
        int numTypes;
        int numCoords;

        public SampleShape(int windingrule,
                           int types[], double coords[],
                           int numtypes, int numcoords)
        {
            this.windingrule = windingrule;
            this.theTypes = types;
            this.theCoords = coords;
            this.numTypes = numtypes;
            this.numCoords = numcoords;
        }

        private Shape testshape;

        public Shape getTestShape() {
            if (testshape == null) {
                testshape = new Area(this);
            }
            return testshape;
        }

        private Rectangle2D cachedBounds;
        public Rectangle2D getCachedBounds2D() {
            if (cachedBounds == null) {
                double xmin, ymin, xmax, ymax;
                int ci = 0;
                xmin = xmax = theCoords[ci++];
                ymin = ymax = theCoords[ci++];
                while (ci < numCoords) {
                    double c = theCoords[ci++];
                    if (xmin > c) xmin = c;
                    if (xmax < c) xmax = c;
                    c = theCoords[ci++];
                    if (ymin > c) ymin = c;
                    if (ymax < c) ymax = c;
                }
                cachedBounds = new Rectangle2D.Double(xmin, ymin,
                                                      xmax - xmin,
                                                      ymax - ymin);
            }
            return cachedBounds;
        }

        public Rectangle getBounds() {
            return getCachedBounds2D().getBounds();
        }
        public Rectangle2D getBounds2D() {
            return getCachedBounds2D().getBounds2D();
        }
        public boolean contains(double x, double y) {
            return getTestShape().contains(x, y);
        }
        public boolean contains(Point2D p) {
            return getTestShape().contains(p);
        }
        public boolean intersects(double x, double y, double w, double h) {
            return getTestShape().intersects(x, y, w, h);
        }
        public boolean intersects(Rectangle2D r) {
            return getTestShape().intersects(r);
        }
        public boolean contains(double x, double y, double w, double h) {
            return getTestShape().contains(x, y, w, h);
        }
        public boolean contains(Rectangle2D r) {
            return getTestShape().contains(r);
        }
        public PathIterator getPathIterator(final AffineTransform at) {
            return new PathIterator() {
                int tindex;
                int cindex;
                public int getWindingRule() {
                    return windingrule;
                }
                public boolean isDone() {
                    return (tindex >= numTypes);
                }
                public void next() {
                    cindex += CoordsForType[theTypes[tindex]];
                    tindex++;
                }
                public int currentSegment(float[] coords) {
                    int t = theTypes[tindex];
                    int n = CoordsForType[t];
                    if (n > 0) {
                        // Cast to float first, then transform
                        // to match accuracy of float paths
                        for (int i = 0; i < n; i++) {
                            coords[i] = (float) theCoords[cindex+i];
                        }
                        if (at != null) {
                            at.transform(coords, 0, coords, 0, n/2);
                        }
                    }
                    return t;
                }
                public int currentSegment(double[] coords) {
                    int t = theTypes[tindex];
                    int n = CoordsForType[t];
                    if (n > 0) {
                        if (at == null) {
                            System.arraycopy(theCoords, cindex,
                                             coords, 0, n);
                        } else {
                            at.transform(theCoords, cindex,
                                         coords, 0, n/2);
                        }
                    }
                    return t;
                }
            };
        }
        public PathIterator getPathIterator(AffineTransform at,
                                            double flatness)
        {
            return new FlatteningPathIterator(getPathIterator(at), flatness);
        }

        public String toString() {
            Rectangle2D r2d = getBounds2D();
            double xmin = r2d.getMinX();
            double ymin = r2d.getMinY();
            double xmax = r2d.getMaxX();
            double ymax = r2d.getMaxY();
            return ("SampleShape["+
                    (windingrule == WIND_NON_ZERO
                     ? "NonZero"
                     : "EvenOdd")+
                    ", nsegments = "+numTypes+
                    ", ncoords = "+numCoords+
                    ", bounds["+(r2d.getMinX()+", "+r2d.getMinY()+", "+
                                 r2d.getMaxX()+", "+r2d.getMaxY())+"]"+
                    "]");
        }

        public Path2D makeFloatPath(Creator c) {
            Path2D.Float p2df = (Path2D.Float) c.makePath(windingrule);
            int ci = 0;
            for (int i = 0; i < numTypes; i++) {
                int t = theTypes[i];
                switch (t) {
                case PathIterator.SEG_MOVETO:
                    p2df.moveTo((float) theCoords[ci++],
                                (float) theCoords[ci++]);
                    break;
                case PathIterator.SEG_LINETO:
                    p2df.lineTo((float) theCoords[ci++],
                                (float) theCoords[ci++]);
                    break;
                case PathIterator.SEG_QUADTO:
                    p2df.quadTo((float) theCoords[ci++],
                                (float) theCoords[ci++],
                                (float) theCoords[ci++],
                                (float) theCoords[ci++]);
                    break;
                case PathIterator.SEG_CUBICTO:
                    p2df.curveTo((float) theCoords[ci++],
                                 (float) theCoords[ci++],
                                 (float) theCoords[ci++],
                                 (float) theCoords[ci++],
                                 (float) theCoords[ci++],
                                 (float) theCoords[ci++]);
                    break;
                case PathIterator.SEG_CLOSE:
                    p2df.closePath();
                    break;
                default:
                    throw new InternalError("unrecognized path type: "+t);
                }
                if (t != PathIterator.SEG_CLOSE) {
                    Point2D curpnt = p2df.getCurrentPoint();
                    if (((float) curpnt.getX()) != ((float) theCoords[ci-2]) ||
                        ((float) curpnt.getY()) != ((float) theCoords[ci-1]))
                    {
                        throw new RuntimeException("currentpoint failed");
                    }
                }
            }
            if (ci != numCoords) {
                throw new InternalError("numcoords did not match");
            }
            return p2df;
        }

        public Path2D makeDoublePath(Creator c) {
            Path2D p2d = c.makePath(windingrule);
            int ci = 0;
            for (int i = 0; i < numTypes; i++) {
                int t = theTypes[i];
                switch (t) {
                case PathIterator.SEG_MOVETO:
                    p2d.moveTo(theCoords[ci++], theCoords[ci++]);
                    break;
                case PathIterator.SEG_LINETO:
                    p2d.lineTo(theCoords[ci++], theCoords[ci++]);
                    break;
                case PathIterator.SEG_QUADTO:
                    p2d.quadTo(theCoords[ci++], theCoords[ci++],
                               theCoords[ci++], theCoords[ci++]);
                    break;
                case PathIterator.SEG_CUBICTO:
                    p2d.curveTo(theCoords[ci++], theCoords[ci++],
                                theCoords[ci++], theCoords[ci++],
                                theCoords[ci++], theCoords[ci++]);
                    break;
                case PathIterator.SEG_CLOSE:
                    p2d.closePath();
                    break;
                default:
                    throw new InternalError("unrecognized path type: "+t);
                }
                if (t != PathIterator.SEG_CLOSE) {
                    Point2D curpnt = p2d.getCurrentPoint();
                    if (((float) curpnt.getX()) != ((float) theCoords[ci-2]) ||
                        ((float) curpnt.getY()) != ((float) theCoords[ci-1]))
                    {
                        throw new RuntimeException("currentpoint failed");
                    }
                }
            }
            if (ci != numCoords) {
                throw new InternalError("numcoords did not match");
            }
            return p2d;
        }
    }

    public static class AppendedShape implements Shape {
        Shape s1;
        Shape s2;
        boolean connect;

        public AppendedShape(Shape s1, Shape s2, boolean connect) {
            this.s1 = s1;
            this.s2 = s2;
            this.connect = connect;
        }

        public Rectangle getBounds() {
            return getBounds2D().getBounds();
        }

        public Rectangle2D getBounds2D() {
            return s1.getBounds2D().createUnion(s2.getBounds2D());
        }

        private Shape testshape;
        private Shape getTestShape() {
            if (testshape == null) {
                testshape = new GeneralPath(this);
            }
            return testshape;
        }

        public boolean contains(double x, double y) {
            return getTestShape().contains(x, y);
        }

        public boolean contains(Point2D p) {
            return getTestShape().contains(p);
        }

        public boolean intersects(double x, double y, double w, double h) {
            return getTestShape().intersects(x, y, w, h);
        }

        public boolean intersects(Rectangle2D r) {
            return getTestShape().intersects(r);
        }

        public boolean contains(double x, double y, double w, double h) {
            return getTestShape().contains(x, y, w, h);
        }

        public boolean contains(Rectangle2D r) {
            return getTestShape().contains(r);
        }

        public PathIterator getPathIterator(final AffineTransform at) {
            return new AppendingPathIterator(s1, s2, connect, at);
        }

        public PathIterator getPathIterator(AffineTransform at,
                                            double flatness)
        {
            return new FlatteningPathIterator(getPathIterator(at), flatness);
        }

        public static class AppendingPathIterator implements PathIterator {
            AffineTransform at;
            PathIterator pi;
            Shape swaiting;
            int windingrule;
            boolean connectrequested;
            boolean canconnect;
            boolean converttoline;

            public AppendingPathIterator(Shape s1, Shape s2,
                                         boolean connect,
                                         AffineTransform at)
            {
                this.at = at;
                this.pi = s1.getPathIterator(at);
                this.swaiting = s2;
                this.windingrule = pi.getWindingRule();
                this.connectrequested = connect;

                if (pi.isDone()) {
                    chain();
                }
            }

            public void chain() {
                if (swaiting != null) {
                    pi = swaiting.getPathIterator(at);
                    swaiting = null;
                    converttoline = (connectrequested && canconnect);
                }
            }

            public int getWindingRule() {
                return windingrule;
            }

            public boolean isDone() {
                return (pi.isDone());
            }

            public void next() {
                converttoline = false;
                pi.next();
                if (pi.isDone()) {
                    chain();
                }
                canconnect = true;
            }

            public int currentSegment(float[] coords) {
                int type = pi.currentSegment(coords);
                if (converttoline) {
                    type = SEG_LINETO;
                }
                return type;
            }

            public int currentSegment(double[] coords) {
                int type = pi.currentSegment(coords);
                if (converttoline) {
                    type = SEG_LINETO;
                }
                return type;
            }
        }
    }

    public static void checkEmpty(Path2D p2d, int windingrule) {
        checkEmpty2(p2d, windingrule);
        p2d.setWindingRule(PathIterator.WIND_NON_ZERO);
        checkEmpty2(p2d, PathIterator.WIND_NON_ZERO);
        p2d.setWindingRule(PathIterator.WIND_EVEN_ODD);
        checkEmpty2(p2d, PathIterator.WIND_EVEN_ODD);
    }

    public static void checkEmpty2(Path2D p2d, int windingrule) {
        if (p2d.getWindingRule() != windingrule) {
            throw new RuntimeException("wrong winding rule in Path2D");
        }
        PathIterator pi = p2d.getPathIterator(null);
        if (pi.getWindingRule() != windingrule) {
            throw new RuntimeException("wrong winding rule in iterator");
        }
        if (!pi.isDone()) {
            throw new RuntimeException("path not empty");
        }
    }

    public static void compare(Creator c, Path2D p2d, Shape ref, int maxulp) {
        compare(c, p2d, (Shape) p2d.clone(), null, 0);
        compare(c, p2d, ref, null, 0);
        compare(c, p2d, ref, TxIdentity, 0);
        p2d.transform(TxIdentity);
        compare(c, p2d, ref, null, 0);
        compare(c, p2d, ref, TxIdentity, 0);
        Shape s2 = p2d.createTransformedShape(TxIdentity);
        compare(c, s2, ref, null, 0);
        compare(c, s2, ref, TxIdentity, 0);
        s2 = p2d.createTransformedShape(TxComplex);
        compare(c, s2, ref, TxComplex, maxulp);
        p2d.transform(TxComplex);
        compare(c, p2d, (Shape) p2d.clone(), null, 0);
        compare(c, p2d, ref, TxComplex, maxulp);
    }

    public static void compare(Creator c,
                               Shape p2d, Shape s,
                               AffineTransform at, int maxulp)
    {
        c.compare(p2d.getPathIterator(null), s.getPathIterator(at),
                  null, maxulp);
        c.compare(p2d.getPathIterator(null), s.getPathIterator(null),
                  at, maxulp);
    }

    public static void checkBounds(Shape stest, Shape sref) {
        checkBounds(stest.getBounds2D(), sref.getBounds2D(),
                    "2D bounds too small");
        /*
        checkBounds(stest.getBounds(), sref.getBounds(),
                    "int bounds too small");
        */
        checkBounds(stest.getBounds(), stest.getBounds2D(),
                    "int bounds too small for 2D bounds");
    }

    public static void checkBounds(Rectangle2D tBounds,
                                   Rectangle2D rBounds,
                                   String faildesc)
    {
        if (rBounds.isEmpty()) {
            if (!tBounds.isEmpty()) {
                throw new RuntimeException("bounds not empty");
            }
            return;
        } else if (tBounds.isEmpty()) {
            throw new RuntimeException("bounds empty");
        }
        double rxmin = rBounds.getMinX();
        double rymin = rBounds.getMinY();
        double rxmax = rBounds.getMaxX();
        double rymax = rBounds.getMaxY();
        double txmin = tBounds.getMinX();
        double tymin = tBounds.getMinY();
        double txmax = tBounds.getMaxX();
        double tymax = tBounds.getMaxY();
        if (txmin > rxmin || tymin > rymin ||
            txmax < rxmax || tymax < rymax)
        {
            if (verbose) System.out.println("test bounds = "+tBounds);
            if (verbose) System.out.println("ref bounds = "+rBounds);
            // Allow fudge room of a couple of single precision ulps
            double ltxmin = txmin - 5 * Math.max(Math.ulp((float) rxmin),
                                                 Math.ulp((float) txmin));
            double ltymin = tymin - 5 * Math.max(Math.ulp((float) rymin),
                                                 Math.ulp((float) tymin));
            double ltxmax = txmax + 5 * Math.max(Math.ulp((float) rxmax),
                                                 Math.ulp((float) txmax));
            double ltymax = tymax + 5 * Math.max(Math.ulp((float) rymax),
                                                 Math.ulp((float) tymax));
            if (ltxmin > rxmin || ltymin > rymin ||
                ltxmax < rxmax || ltymax < rymax)
            {
                if (!verbose) System.out.println("test bounds = "+tBounds);
                if (!verbose) System.out.println("ref bounds = "+rBounds);
                System.out.println("xmin: "+
                                   txmin+" + "+fltulpless(txmin, rxmin)+" = "+
                                   rxmin+" + "+fltulpless(rxmin, txmin));
                System.out.println("ymin: "+
                                   tymin+" + "+fltulpless(tymin, rymin)+" = "+
                                   rymin+" + "+fltulpless(rymin, tymin));
                System.out.println("xmax: "+
                                   txmax+" + "+fltulpless(txmax, rxmax)+" = "+
                                   rxmax+" + "+fltulpless(rxmax, txmax));
                System.out.println("ymax: "+
                                   tymax+" + "+fltulpless(tymax, rymax)+" = "+
                                   rymax+" + "+fltulpless(rymax, tymax));
                System.out.println("flt tbounds = ["+
                                   ((float) txmin)+", "+((float) tymin)+", "+
                                   ((float) txmax)+", "+((float) tymax)+"]");
                System.out.println("flt rbounds = ["+
                                   ((float) rxmin)+", "+((float) rymin)+", "+
                                   ((float) rxmax)+", "+((float) rymax)+"]");
                System.out.println("xmin ulp = "+fltulpless(rxmin, txmin));
                System.out.println("ymin ulp = "+fltulpless(rymin, tymin));
                System.out.println("xmax ulp = "+fltulpless(txmax, rxmax));
                System.out.println("ymax ulp = "+fltulpless(tymax, rymax));
                throw new RuntimeException(faildesc);
            }
        }
    }

    public static void checkHits(Shape stest, Shape sref) {
        for (int i = 0; i < 10; i++) {
            double px = Math.random() * 500 - 250;
            double py = Math.random() * 500 - 250;
            Point2D pnt = new Point2D.Double(px, py);

            double rw = Math.random()*10+0.4;
            double rh = Math.random()*10+0.4;
            double rx = px - rw/2;
            double ry = py - rh/2;
            Rectangle2D rect = new Rectangle2D.Double(rx, ry, rw, rh);
            Rectangle2D empty = new Rectangle2D.Double(rx, ry, 0, 0);

            if (!rect.contains(pnt)) {
                throw new InternalError("test point not inside test rect!");
            }

            if (stest.contains(rx, ry, 0, 0)) {
                throw new RuntimeException("contains 0x0 rect");
            }
            if (stest.contains(empty)) {
                throw new RuntimeException("contains empty rect");
            }
            if (stest.intersects(rx, ry, 0, 0)) {
                throw new RuntimeException("intersects 0x0 rect");
            }
            if (stest.intersects(empty)) {
                throw new RuntimeException("intersects empty rect");
            }

            boolean tContainsXY = stest.contains(px, py);
            boolean tContainsPnt = stest.contains(pnt);
            boolean tContainsXYWH = stest.contains(rx, ry, rw, rh);
            boolean tContainsRect = stest.contains(rect);
            boolean tIntersectsXYWH = stest.intersects(rx, ry, rw, rh);
            boolean tIntersectsRect = stest.intersects(rect);

            if (tContainsXY != tContainsPnt) {
                throw new RuntimeException("contains(x,y) != "+
                                           "contains(pnt)");
            }
            if (tContainsXYWH != tContainsRect) {
                throw new RuntimeException("contains(x,y,w,h) != "+
                                           "contains(rect)");
            }
            if (tIntersectsXYWH != tIntersectsRect) {
                throw new RuntimeException("intersects(x,y,w,h) != "+
                                           "intersects(rect)");
            }

            boolean uContainsXY =
                Path2D.contains(stest.getPathIterator(null), px, py);
            boolean uContainsXYWH =
                Path2D.contains(stest.getPathIterator(null), rx, ry, rw, rh);
            boolean uIntersectsXYWH =
                Path2D.intersects(stest.getPathIterator(null), rx, ry, rw, rh);

            if (tContainsXY != uContainsXY) {
                throw new RuntimeException("contains(x,y) "+
                                           "does not match utility");
            }
            if (tContainsXYWH != uContainsXYWH) {
                throw new RuntimeException("contains(x,y,w,h) "+
                                           "does not match utility");
            }
            if (tIntersectsXYWH != uIntersectsXYWH) {
                throw new RuntimeException("intersects(x,y,w,h) "+
                                           "does not match utility");
            }

            // Make rect slightly smaller to be more conservative for rContains
            double srx = rx + 0.1;
            double sry = ry + 0.1;
            double srw = rw - 0.2;
            double srh = rh - 0.2;
            Rectangle2D srect = new Rectangle2D.Double(srx, sry, srw, srh);
            // Make rect slightly larger to be more liberal for rIntersects
            double lrx = rx - 0.1;
            double lry = ry - 0.1;
            double lrw = rw + 0.2;
            double lrh = rh + 0.2;
            Rectangle2D lrect = new Rectangle2D.Double(lrx, lry, lrw, lrh);

            if (srect.isEmpty()) {
                throw new InternalError("smaller rect too small (empty)");
            }
            if (!lrect.contains(rect)) {
                throw new InternalError("test rect not inside larger rect!");
            }
            if (!rect.contains(srect)) {
                throw new InternalError("smaller rect not inside test rect!");
            }

            boolean rContainsSmaller;
            boolean rIntersectsLarger;
            boolean rContainsPnt;

            if (sref instanceof SampleShape ||
                sref instanceof QuadCurve2D ||
                sref instanceof CubicCurve2D)
            {
                // REMIND
                // Some of the source shapes are not proving reliable
                // enough to do reference verification of the hit
                // testing results.
                // Quad/CubicCurve2D have spaghetti test methods that could
                // very likely contain some bugs.  They return a conflicting
                // answer in maybe 1 out of 20,000 tests.
                // Area causes a conflicting answer maybe 1 out of
                // 100 to 1000 runs and it infinite loops maybe 1
                // out of 10,000 runs or so.
                // So, we use some conservative "safe" answers for
                // these shapes and avoid their hit testing methods.
                rContainsSmaller = tContainsRect;
                rIntersectsLarger = tIntersectsRect;
                rContainsPnt = tContainsPnt;
            } else {
                rContainsSmaller = sref.contains(srect);
                rIntersectsLarger = sref.intersects(lrect);
                rContainsPnt = sref.contains(px, py);
            }

            if (tIntersectsRect) {
                if (tContainsRect) {
                    if (!tContainsPnt) {
                        System.out.println("reference shape = "+sref);
                        System.out.println("pnt = "+pnt);
                        System.out.println("rect = "+rect);
                        System.out.println("tbounds = "+stest.getBounds2D());
                        throw new RuntimeException("test contains rect, "+
                                                   "but not center point");
                    }
                }
                // Note: (tContainsPnt || tContainsRect) is same as
                // tContainsPnt because of the test above...
                if (tContainsPnt) {
                    if (!rIntersectsLarger) {
                        System.out.println("reference shape = "+sref);
                        System.out.println("pnt = "+pnt);
                        System.out.println("rect = "+rect);
                        System.out.println("lrect = "+lrect);
                        System.out.println("tbounds = "+stest.getBounds2D());
                        System.out.println("rbounds = "+sref.getBounds2D());
                        throw new RuntimeException("test claims containment, "+
                                                   "but no ref intersection");
                    }
                }
            } else {
                if (tContainsRect) {
                    throw new RuntimeException("test contains rect, "+
                                               "with no intersection");
                }
                if (tContainsPnt) {
                    System.out.println("reference shape = "+sref);
                    System.out.println("rect = "+rect);
                    throw new RuntimeException("test contains point, "+
                                               "with no intersection");
                }
                if (rContainsPnt || rContainsSmaller) {
                    System.out.println("pnt = "+pnt);
                    System.out.println("rect = "+rect);
                    System.out.println("srect = "+lrect);
                    throw new RuntimeException("test did not intersect, "+
                                               "but ref claims containment");
                }
            }
        }
    }

    public static void test(Creator c) {
        testConstructors(c);
        testPathConstruction(c);
        testAppend(c);
        testBounds(c);
        testHits(c);
    }

    public static void testConstructors(Creator c) {
        checkEmpty(c.makePath(), WIND_NON_ZERO);
        checkEmpty(c.makePath(WIND_NON_ZERO), WIND_NON_ZERO);
        checkEmpty(c.makePath(WIND_EVEN_ODD), WIND_EVEN_ODD);
        checkEmpty(c.makePath(EmptyShapeNonZero), WIND_NON_ZERO);
        checkEmpty(c.makePath(EmptyShapeNonZero, null), WIND_NON_ZERO);
        checkEmpty(c.makePath(EmptyShapeNonZero, TxIdentity), WIND_NON_ZERO);
        checkEmpty(c.makePath(EmptyShapeEvenOdd), WIND_EVEN_ODD);
        checkEmpty(c.makePath(EmptyShapeEvenOdd, null), WIND_EVEN_ODD);
        checkEmpty(c.makePath(EmptyShapeEvenOdd, TxIdentity), WIND_EVEN_ODD);
        try {
            c.makePath(null);
            throw new RuntimeException(c+" allowed null Shape in constructor");
        } catch (NullPointerException npe) {
            // passes
        }
        try {
            c.makePath(null, TxIdentity);
            throw new RuntimeException(c+" allowed null Shape in constructor");
        } catch (NullPointerException npe) {
            // passes
        }

        for (int i = 0; i < TestShapes.length; i++) {
            Shape sref = TestShapes[i];
            if (verbose) System.out.println("construct testing "+sref);
            compare(c, c.makePath(sref), sref, null, 0);
            compare(c, c.makePath(sref), sref, TxIdentity, 0);
            compare(c, c.makePath(sref, null), sref, null, 0);
            compare(c, c.makePath(sref, null), sref, TxIdentity, 0);
            compare(c, c.makePath(sref, TxIdentity), sref, null, 0);
            compare(c, c.makePath(sref, TxIdentity), sref, TxIdentity, 0);
            compare(c, c.makePath(sref, TxComplex), sref, TxComplex,
                    c.getRecommendedTxMaxUlp());
        }
    }

    public static void testPathConstruction(Creator c) {
        testPathConstruction(c, LongSampleNonZero);
        testPathConstruction(c, LongSampleEvenOdd);
    }

    public static void testPathConstruction(Creator c, SampleShape ref) {
        if (c.supportsFloatCompose()) {
            compare(c, ref.makeFloatPath(c), ref, c.getRecommendedTxMaxUlp());
        }
        compare(c, ref.makeDoublePath(c), ref, c.getRecommendedTxMaxUlp());
    }

    public static void testAppend(Creator c) {
        for (int i = 0; i < TestShapes.length; i++) {
            Shape sref = TestShapes[i];
            if (verbose) System.out.println("append testing "+sref);
            PathIterator spi = sref.getPathIterator(null);
            Path2D stest = c.makePath(spi.getWindingRule());
            stest.append(spi, false);
            compare(c, stest, sref, null, 0);
            stest.reset();
            stest.append(sref, false);
            compare(c, stest, sref, null, 0);
            stest.reset();
            stest.append(sref.getPathIterator(TxComplex), false);
            compare(c, stest, sref, TxComplex, 0);
            // multiple shape append testing...
            if (sref.getBounds2D().isEmpty()) {
                // If the first shape is empty, then we really
                // are not testing multiple appended shapes,
                // we are just testing appending the AppendShape
                // to a null path over and over.
                // Also note that some empty shapes will spit out
                // a single useless SEG_MOVETO that has no affect
                // on the outcome, but it makes duplicating the
                // behavior that Path2D has in that case difficult
                // when the AppenedShape utility class has to
                // iterate the exact same segments.  So, we will
                // just ignore all empty shapes here.
                continue;
            }
            stest.reset();
            stest.append(sref, false);
            stest.append(AppendShape, false);
            compare(c, stest,
                    new AppendedShape(sref, AppendShape, false),
                    null, 0);
            stest.reset();
            stest.append(sref, false);
            stest.append(AppendShape, true);
            compare(c, stest,
                    new AppendedShape(sref, AppendShape, true),
                    null, 0);
            stest.reset();
            stest.append(sref.getPathIterator(null), false);
            stest.append(AppendShape.getPathIterator(null), false);
            compare(c, stest,
                    new AppendedShape(sref, AppendShape, false),
                    null, 0);
            stest.reset();
            stest.append(sref.getPathIterator(null), false);
            stest.append(AppendShape.getPathIterator(null), true);
            compare(c, stest,
                    new AppendedShape(sref, AppendShape, true),
                    null, 0);
            stest.reset();
            stest.append(sref.getPathIterator(TxComplex), false);
            stest.append(AppendShape.getPathIterator(TxComplex), false);
            compare(c, stest,
                    new AppendedShape(sref, AppendShape, false),
                    TxComplex, 0);
            stest.reset();
            stest.append(sref.getPathIterator(TxComplex), false);
            stest.append(AppendShape.getPathIterator(TxComplex), true);
            compare(c, stest,
                    new AppendedShape(sref, AppendShape, true),
                    TxComplex, 0);
        }
    }

    public static void testBounds(Creator c) {
        for (int i = 0; i < TestShapes.length; i++) {
            Shape sref = TestShapes[i];
            if (verbose) System.out.println("bounds testing "+sref);
            Shape stest = c.makePath(sref);
            checkBounds(c.makePath(sref), sref);
        }
        testBounds(c, ShortSampleNonZero);
        testBounds(c, ShortSampleEvenOdd);
        testBounds(c, LongSampleNonZero);
        testBounds(c, LongSampleEvenOdd);
    }

    public static void testBounds(Creator c, SampleShape ref) {
        if (verbose) System.out.println("bounds testing "+ref);
        if (c.supportsFloatCompose()) {
            checkBounds(ref.makeFloatPath(c), ref);
        }
        checkBounds(ref.makeDoublePath(c), ref);
    }

    public static void testHits(Creator c) {
        for (int i = 0; i < TestShapes.length; i++) {
            Shape sref = TestShapes[i];
            if (verbose) System.out.println("hit testing "+sref);
            Shape stest = c.makePath(sref);
            checkHits(c.makePath(sref), sref);
        }
        testHits(c, ShortSampleNonZero);
        testHits(c, ShortSampleEvenOdd);
        // These take too long to construct the Area for reference testing
        //testHits(c, LongSampleNonZero);
        //testHits(c, LongSampleEvenOdd);
    }

    public static void testHits(Creator c, SampleShape ref) {
        if (verbose) System.out.println("hit testing "+ref);
        if (c.supportsFloatCompose()) {
            checkHits(ref.makeFloatPath(c), ref);
        }
        checkHits(ref.makeDoublePath(c), ref);
    }

    public static void main(String argv[]) {
        int limit = (argv.length > 0) ? 10000 : 1;
        verbose = (argv.length > 1);
        for (int i = 0; i < limit; i++) {
            if (limit > 1) {
                System.out.println("loop #"+(i+1));
            }
            init();
            test(new GPCreator());
            test(new FltCreator());
            test(new DblCreator());
        }
    }
}
