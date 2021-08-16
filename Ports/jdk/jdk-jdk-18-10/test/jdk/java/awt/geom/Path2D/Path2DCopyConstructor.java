/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
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


import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.IllegalPathStateException;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Arrays;

/**
 * @test
 * @bug 8076419 8078192 8186364
 * @summary Check Path2D copy constructor (trims arrays)
 *          and constructor with zero capacity
 *          and Path2D.trimToSize()
 * @run main Path2DCopyConstructor
 */
public class Path2DCopyConstructor {

    private final static float EPSILON = 5e-6f;
    private final static float FLATNESS = 1e-2f;

    private final static AffineTransform at
        = AffineTransform.getScaleInstance(1.3, 2.4);

    private final static Rectangle2D.Double rect2d
        = new Rectangle2D.Double(3.2, 4.1, 5.0, 10.0);

    private final static Point2D.Double pt2d
        = new Point2D.Double(2.0, 2.5);

    public static boolean verbose;

    static void log(String msg) {
        if (verbose) {
            System.out.println(msg);
        }
    }

    public static void main(String argv[]) {
        verbose = (argv.length != 0);

        testEmptyDoublePaths();
        testDoublePaths();

        testEmptyFloatPaths();
        testFloatPaths();

        testEmptyGeneralPath();
        testGeneralPath();
    }

    static void testEmptyDoublePaths() {
        log("\n - Test(Path2D.Double[0]) ---");
        test(() -> new Path2D.Double(Path2D.WIND_NON_ZERO, 0));
    }

    static void testDoublePaths() {
        log("\n - Test(Path2D.Double) ---");
        test(() -> new Path2D.Double());
    }

    static void testEmptyFloatPaths() {
        log("\n - Test(Path2D.Float[0]) ---");
        test(() -> new Path2D.Float(Path2D.WIND_NON_ZERO, 0));
    }

    static void testFloatPaths() {
        log("\n - Test(Path2D.Float) ---");
        test(() -> new Path2D.Float());
    }

    static void testEmptyGeneralPath() {
        log("\n - Test(GeneralPath[0]) ---");
        test(() -> new GeneralPath(Path2D.WIND_NON_ZERO, 0));
    }

    static void testGeneralPath() {
        log("\n - Test(GeneralPath) ---");
        test(() -> new GeneralPath());
    }

    interface PathFactory {
        Path2D makePath();
    }

    static void test(PathFactory pf) {
        log("\n --- test: path(empty) ---");
        test(pf.makePath(), true);
        log("\n\n --- test: path(addMove) ---");
        test(addMove(pf.makePath()), false);
        log("\n\n --- test: path(addMoveAndLines) ---");
        test(addMoveAndLines(pf.makePath()), false);
        log("\n\n --- test: path(addMoveAndQuads) ---");
        test(addMoveAndQuads(pf.makePath()), false);
        log("\n\n --- test: path(addMoveAndCubics) ---");
        test(addMoveAndCubics(pf.makePath()), false);
        log("\n\n --- test: path(addMoveAndClose) ---");
        test(addMoveAndClose(pf.makePath()), false);
    }

    static Path2D addMove(Path2D p2d) {
        p2d.moveTo(1.0, 0.5);
        return p2d;
    }

    static Path2D addMoveAndLines(Path2D p2d) {
        addMove(p2d);
        addLines(p2d);
        return p2d;
    }

    static Path2D addLines(Path2D p2d) {
        for (int i = 0; i < 10; i++) {
            p2d.lineTo(1.1 * i, 2.3 * i);
        }
        return p2d;
    }

    static Path2D addMoveAndCubics(Path2D p2d) {
        addMove(p2d);
        addCubics(p2d);
        return p2d;
    }

    static Path2D addCubics(Path2D p2d) {
        for (int i = 0; i < 10; i++) {
            p2d.curveTo(1.1 * i, 1.2 * i, 1.3 * i, 1.4 * i, 1.5 * i, 1.6 * i);
        }
        return p2d;
    }

    static Path2D addMoveAndQuads(Path2D p2d) {
        addMove(p2d);
        addQuads(p2d);
        return p2d;
    }

    static Path2D addQuads(Path2D p2d) {
        for (int i = 0; i < 10; i++) {
            p2d.quadTo(1.1 * i, 1.2 * i, 1.3 * i, 1.4 * i);
        }
        return p2d;
    }

    static Path2D addMoveAndClose(Path2D p2d) {
        addMove(p2d);
        addClose(p2d);
        return p2d;
    }

    static Path2D addClose(Path2D p2d) {
        p2d.closePath();
        return p2d;
    }

    static void test(Path2D p2d, boolean isEmpty) {
        Path2D c;
        Path2D.Float pf;
        Path2D.Double pd;
        GeneralPath gp;

        pf = new Path2D.Float(p2d);
        testEqual(pf, p2d);
        pf.trimToSize();
        testEqual(pf, p2d);
        pd = new Path2D.Double(p2d);
        testEqual(pd, p2d);
        pd.trimToSize();
        testEqual(pd, p2d);
        c = (Path2D)p2d.clone();
        testEqual(c, p2d);
        c.trimToSize();
        testEqual(c, p2d);
        gp = new GeneralPath(p2d);
        testEqual(gp, p2d);
        gp.trimToSize();
        testEqual(gp, p2d);

        pf = new Path2D.Float(p2d);
        testIterator(pf, p2d);
        pf.trimToSize();
        testIterator(pf, p2d);
        pd = new Path2D.Double(p2d);
        testIterator(pd, p2d);
        pd.trimToSize();
        testIterator(pd, p2d);
        c = (Path2D)p2d.clone();
        testIterator(c, p2d);
        c.trimToSize();
        testIterator(c, p2d);
        gp = new GeneralPath(p2d);
        testIterator(gp, p2d);
        gp.trimToSize();
        testIterator(gp, p2d);

        pf = new Path2D.Float(p2d);
        testFlattening(pf, p2d);
        pf.trimToSize();
        testFlattening(pf, p2d);
        pd = new Path2D.Double(p2d);
        testFlattening(pd, p2d);
        pd.trimToSize();
        testFlattening(pd, p2d);
        c = (Path2D)p2d.clone();
        testFlattening(c, p2d);
        c.trimToSize();
        testFlattening(c, p2d);
        gp = new GeneralPath(p2d);
        testFlattening(gp, p2d);
        gp.trimToSize();
        testFlattening(gp, p2d);

        pf = new Path2D.Float(p2d);
        testAddMove(pf);
        pf.trimToSize();
        testAddMove(pf);
        pd = new Path2D.Double(p2d);
        testAddMove(pd);
        pd.trimToSize();
        testAddMove(pd);
        c = (Path2D)p2d.clone();
        testAddMove(c);
        c.trimToSize();
        testAddMove(c);
        gp = new GeneralPath(p2d);
        testAddMove(gp);
        gp.trimToSize();
        testAddMove(gp);

        // These should expect exception if empty
        pf = new Path2D.Float(p2d);
        testAddLine(pf, isEmpty);
        pf.trimToSize();
        testAddLine(pf, isEmpty);
        pd = new Path2D.Double(p2d);
        testAddLine(pd, isEmpty);
        pd.trimToSize();
        testAddLine(pd, isEmpty);
        c = (Path2D)p2d.clone();
        testAddLine(c, isEmpty);
        c.trimToSize();
        testAddLine(c, isEmpty);
        gp = new GeneralPath(p2d);
        testAddLine(gp, isEmpty);
        gp.trimToSize();
        testAddLine(gp, isEmpty);

        pf = new Path2D.Float(p2d);
        testAddQuad(pf, isEmpty);
        pf.trimToSize();
        testAddQuad(pf, isEmpty);
        pd = new Path2D.Double(p2d);
        testAddQuad(pd, isEmpty);
        pd.trimToSize();
        testAddQuad(pd, isEmpty);
        c = (Path2D)p2d.clone();
        testAddQuad(c, isEmpty);
        c.trimToSize();
        testAddQuad(c, isEmpty);
        gp = new GeneralPath(p2d);
        testAddQuad(gp, isEmpty);
        gp.trimToSize();
        testAddQuad(gp, isEmpty);

        pf = new Path2D.Float(p2d);
        testAddCubic(pf, isEmpty);
        pf.trimToSize();
        testAddCubic(pf, isEmpty);
        pd = new Path2D.Double(p2d);
        testAddCubic(pd, isEmpty);
        pd.trimToSize();
        testAddCubic(pd, isEmpty);
        c = (Path2D)p2d.clone();
        testAddCubic(c, isEmpty);
        c.trimToSize();
        testAddCubic(c, isEmpty);
        gp = new GeneralPath(p2d);
        testAddCubic(gp, isEmpty);
        gp.trimToSize();
        testAddCubic(gp, isEmpty);

        pf = new Path2D.Float(p2d);
        testAddClose(pf, isEmpty);
        pf.trimToSize();
        testAddClose(pf, isEmpty);
        pd = new Path2D.Double(p2d);
        testAddClose(pd, isEmpty);
        pd.trimToSize();
        testAddClose(pd, isEmpty);
        c = (Path2D)p2d.clone();
        testAddClose(c, isEmpty);
        c.trimToSize();
        testAddClose(c, isEmpty);
        gp = new GeneralPath(p2d);
        testAddClose(gp, isEmpty);
        gp.trimToSize();
        testAddClose(gp, isEmpty);

        pf = new Path2D.Float(p2d);
        testGetBounds(pf, p2d);
        pf.trimToSize();
        testGetBounds(pf, p2d);
        pd = new Path2D.Double(p2d);
        testGetBounds(pd, p2d);
        pd.trimToSize();
        testGetBounds(pd, p2d);
        c = (Path2D)p2d.clone();
        testGetBounds(c, p2d);
        c.trimToSize();
        testGetBounds(c, p2d);
        gp = new GeneralPath(p2d);
        testGetBounds(gp, p2d);
        gp.trimToSize();
        testGetBounds(gp, p2d);

        pf = new Path2D.Float(p2d);
        testTransform(pf);
        pf.trimToSize();
        testTransform(pf);
        pd = new Path2D.Double(p2d);
        testTransform(pd);
        pd.trimToSize();
        testTransform(pd);
        c = (Path2D)p2d.clone();
        testTransform(c);
        c.trimToSize();
        testTransform(c);
        gp = new GeneralPath(p2d);
        testTransform(gp);
        gp.trimToSize();
        testTransform(gp);

        pf = new Path2D.Float(p2d);
        testIntersect(pf, p2d);
        pf.trimToSize();
        testIntersect(pf, p2d);
        pd = new Path2D.Double(p2d);
        testIntersect(pd, p2d);
        pd.trimToSize();
        testIntersect(pd, p2d);
        c = (Path2D)p2d.clone();
        testIntersect(c, p2d);
        c.trimToSize();
        testIntersect(c, p2d);
        gp = new GeneralPath(p2d);
        testIntersect(gp, p2d);
        gp.trimToSize();
        testIntersect(gp, p2d);

        pf = new Path2D.Float(p2d);
        testContains(pf, p2d);
        pf.trimToSize();
        testContains(pf, p2d);
        pd = new Path2D.Double(p2d);
        testContains(pd, p2d);
        pd.trimToSize();
        testContains(pd, p2d);
        c = (Path2D)p2d.clone();
        testContains(c, p2d);
        c.trimToSize();
        testContains(c, p2d);
        gp = new GeneralPath(p2d);
        testContains(gp, p2d);
        gp.trimToSize();
        testContains(gp, p2d);

        pf = new Path2D.Float(p2d);
        testGetCurrentPoint(pf, p2d);
        pf.trimToSize();
        testGetCurrentPoint(pf, p2d);
        pd = new Path2D.Double(p2d);
        testGetCurrentPoint(pd, p2d);
        pd.trimToSize();
        testGetCurrentPoint(pd, p2d);
        c = (Path2D)p2d.clone();
        testGetCurrentPoint(c, p2d);
        c.trimToSize();
        testGetCurrentPoint(c, p2d);
        gp = new GeneralPath(p2d);
        testGetCurrentPoint(gp, p2d);
        gp.trimToSize();
        testGetCurrentPoint(gp, p2d);
    }

    static void testEqual(Path2D pathA, Path2D pathB) {
        final PathIterator itA = pathA.getPathIterator(null);
        final PathIterator itB = pathB.getPathIterator(null);

        float[] coordsA = new float[6];
        float[] coordsB = new float[6];

        int n = 0;
        for (; !itA.isDone() && !itB.isDone(); itA.next(), itB.next(), n++) {
            int typeA = itA.currentSegment(coordsA);
            int typeB = itB.currentSegment(coordsB);

            if (typeA != typeB) {
                throw new IllegalStateException("Path-segment[" + n + "] "
                    + " type are not equals [" + typeA + "|" + typeB + "] !");
            }
            if (!equalsArray(coordsA, coordsB, getLength(typeA))) {
                throw new IllegalStateException("Path-segment[" + n + "] coords"
                    + " are not equals [" + Arrays.toString(coordsA) + "|"
                    + Arrays.toString(coordsB) + "] !");
            }
        }
        if (!itA.isDone() || !itB.isDone()) {
            throw new IllegalStateException("Paths do not have same lengths !");
        }
        log("testEqual: " + n + " segments.");
    }

    static void testIterator(Path2D pathA, Path2D pathB) {
        final PathIterator itA = pathA.getPathIterator(at);
        final PathIterator itB = pathB.getPathIterator(at);

        float[] coordsA = new float[6];
        float[] coordsB = new float[6];

        int n = 0;
        for (; !itA.isDone() && !itB.isDone(); itA.next(), itB.next(), n++) {
            int typeA = itA.currentSegment(coordsA);
            int typeB = itB.currentSegment(coordsB);

            if (typeA != typeB) {
                throw new IllegalStateException("Path-segment[" + n + "] "
                    + "type are not equals [" + typeA + "|" + typeB + "] !");
            }
            // Take care of floating-point precision:
            if (!equalsArrayEps(coordsA, coordsB, getLength(typeA))) {
                throw new IllegalStateException("Path-segment[" + n + "] coords"
                    + " are not equals [" + Arrays.toString(coordsA) + "|"
                    + Arrays.toString(coordsB) + "] !");
            }
        }
        if (!itA.isDone() || !itB.isDone()) {
            throw new IllegalStateException("Paths do not have same lengths !");
        }
        log("testIterator: " + n + " segments.");
    }

    static void testFlattening(Path2D pathA, Path2D pathB) {
        final PathIterator itA = pathA.getPathIterator(at, FLATNESS);
        final PathIterator itB = pathB.getPathIterator(at, FLATNESS);

        float[] coordsA = new float[6];
        float[] coordsB = new float[6];

        int n = 0;
        for (; !itA.isDone() && !itB.isDone(); itA.next(), itB.next(), n++) {
            int typeA = itA.currentSegment(coordsA);
            int typeB = itB.currentSegment(coordsB);

            if (typeA != typeB) {
                throw new IllegalStateException("Path-segment[" + n + "] "
                    + "type are not equals [" + typeA + "|" + typeB + "] !");
            }
            // Take care of floating-point precision:
            if (!equalsArrayEps(coordsA, coordsB, getLength(typeA))) {
                throw new IllegalStateException("Path-segment[" + n + "] coords"
                    + " are not equals [" + Arrays.toString(coordsA) + "|"
                    + Arrays.toString(coordsB) + "] !");
            }
        }
        if (!itA.isDone() || !itB.isDone()) {
            throw new IllegalStateException("Paths do not have same lengths !");
        }
        log("testFlattening: " + n + " segments.");
    }

    static void testAddMove(Path2D pathA) {
        addMove(pathA);
        log("testAddMove: passed.");
    }

    static void testAddLine(Path2D pathA, boolean isEmpty) {
        try {
            addLines(pathA);
        }
        catch (IllegalPathStateException ipse) {
            if (isEmpty) {
                log("testAddLine: passed "
                    + "(expected IllegalPathStateException catched).");
                return;
            } else {
                throw ipse;
            }
        }
        if (isEmpty) {
            throw new IllegalStateException("IllegalPathStateException not thrown !");
        }
        log("testAddLine: passed.");
    }

    static void testAddQuad(Path2D pathA, boolean isEmpty) {
        try {
            addQuads(pathA);
        }
        catch (IllegalPathStateException ipse) {
            if (isEmpty) {
                log("testAddQuad: passed "
                    + "(expected IllegalPathStateException catched).");
                return;
            } else {
                throw ipse;
            }
        }
        if (isEmpty) {
            throw new IllegalStateException("IllegalPathStateException not thrown !");
        }
        log("testAddQuad: passed.");
    }

    static void testAddCubic(Path2D pathA, boolean isEmpty) {
        try {
            addCubics(pathA);
        }
        catch (IllegalPathStateException ipse) {
            if (isEmpty) {
                log("testAddCubic: passed "
                    + "(expected IllegalPathStateException catched).");
                return;
            } else {
                throw ipse;
            }
        }
        if (isEmpty) {
            throw new IllegalStateException("IllegalPathStateException not thrown !");
        }
        log("testAddCubic: passed.");
    }

    static void testAddClose(Path2D pathA, boolean isEmpty) {
        try {
            addClose(pathA);
        }
        catch (IllegalPathStateException ipse) {
            if (isEmpty) {
                log("testAddClose: passed "
                    + "(expected IllegalPathStateException catched).");
                return;
            } else {
                throw ipse;
            }
        }
        if (isEmpty) {
            throw new IllegalStateException("IllegalPathStateException not thrown !");
        }
        log("testAddClose: passed.");
    }

    static void testGetBounds(Path2D pathA, Path2D pathB) {
        final Rectangle rA = pathA.getBounds();
        final Rectangle rB = pathB.getBounds();

        if (!rA.equals(rB)) {
            throw new IllegalStateException("Bounds are not equals [" + rA
                + "|" + rB + "] !");
        }
        final Rectangle2D r2dA = pathA.getBounds2D();
        final Rectangle2D r2dB = pathB.getBounds2D();

        if (!equalsRectangle2D(r2dA, r2dB)) {
            throw new IllegalStateException("Bounds2D are not equals ["
                + r2dA + "|" + r2dB + "] !");
        }
        log("testGetBounds: passed.");
    }

    static void testTransform(Path2D pathA) {
        pathA.transform(at);
        log("testTransform: passed.");
    }

    static void testIntersect(Path2D pathA, Path2D pathB) {
        boolean resA = pathA.intersects(rect2d);
        boolean resB = pathB.intersects(rect2d);
        if (resA != resB) {
            throw new IllegalStateException("Intersects(rect2d) are not equals ["
                + resA + "|" + resB + "] !");
        }
        resA = pathA.intersects(1.0, 2.0, 13.0, 17.0);
        resB = pathB.intersects(1.0, 2.0, 13.0, 17.0);
        if (resA != resB) {
            throw new IllegalStateException("Intersects(doubles) are not equals ["
                + resA + "|" + resB + "] !");
        }
        log("testIntersect: passed.");
    }

    static void testContains(Path2D pathA, Path2D pathB) {
        boolean resA = pathA.contains(pt2d);
        boolean resB = pathB.contains(pt2d);
        if (resA != resB) {
            throw new IllegalStateException("Contains(pt) are not equals ["
                + resA + "|" + resB + "] !");
        }
        resA = pathA.contains(pt2d.getX(), pt2d.getY());
        resB = pathB.contains(pt2d.getX(), pt2d.getY());
        if (resA != resB) {
            throw new IllegalStateException("Contains(x,y) are not equals ["
                + resA + "|" + resB + "] !");
        }
        resA = pathA.contains(rect2d);
        resB = pathB.contains(rect2d);
        if (resA != resB) {
            throw new IllegalStateException("Contains(rect2d) are not equals ["
                + resA + "|" + resB + "] !");
        }
        resA = pathA.contains(1.0, 2.0, 13.0, 17.0);
        resB = pathB.contains(1.0, 2.0, 13.0, 17.0);
        if (resA != resB) {
            throw new IllegalStateException("Contains(doubles) are not equals ["
                + resA + "|" + resB + "] !");
        }
        log("testContains: passed.");
    }

    static void testGetCurrentPoint(Path2D pathA, Path2D pathB) {
        final Point2D ptA = pathA.getCurrentPoint();
        final Point2D ptB = pathA.getCurrentPoint();
        if (((ptA == null) && (ptB != null))
            || ((ptA != null) && !ptA.equals(ptB)))
        {
            throw new IllegalStateException("getCurrentPoint() are not equals ["
                + ptA + "|" + ptB + "] !");
        }
        log("testGetCurrentPoint: passed.");
    }

    static int getLength(int type) {
        switch(type) {
            case PathIterator.SEG_CUBICTO:
                return 6;
            case PathIterator.SEG_QUADTO:
                return 4;
            case PathIterator.SEG_LINETO:
            case PathIterator.SEG_MOVETO:
                return 2;
            case PathIterator.SEG_CLOSE:
                return 0;
            default:
                throw new IllegalStateException("Invalid type: " + type);
        }
    }


    // Custom equals methods ---

    public static boolean equalsArray(float[] a, float[] a2, final int len) {
        for (int i = 0; i < len; i++) {
            if (Float.floatToIntBits(a[i]) != Float.floatToIntBits(a2[i])) {
                return false;
            }
        }
        return true;
    }

    static boolean equalsArrayEps(float[] a, float[] a2, final int len) {
        for (int i = 0; i < len; i++) {
            if (!equalsEps(a[i], a2[i])) {
                return false;
            }
        }

        return true;
    }

    static boolean equalsRectangle2D(Rectangle2D a, Rectangle2D b) {
        if (a == b) {
            return true;
        }
        return equalsEps(a.getX(), b.getX())
            && equalsEps(a.getY(), b.getY())
            && equalsEps(a.getWidth(), b.getWidth())
            && equalsEps(a.getHeight(), b.getHeight());
    }

    static boolean equalsEps(float a, float b) {
        return (Math.abs(a - b) <= EPSILON);
    }

    static boolean equalsEps(double a, double b) {
        return (Math.abs(a - b) <= EPSILON);
    }
}
