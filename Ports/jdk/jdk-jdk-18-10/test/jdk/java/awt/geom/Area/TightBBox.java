/*
 * Copyright (c) 1999, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4276812
 * @summary Verifies that the Area.getBounds2D() method returns a very tight
 *          bounding box
 */

import java.awt.geom.Area;
import java.awt.geom.CubicCurve2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.QuadCurve2D;
import java.awt.geom.Rectangle2D;

public class TightBBox {
    public static void main(String argv[]) {
        Point2D points[] = {
            new Point2D.Double(0.0, 0.0),
            new Point2D.Double(1.0, 0.0),
            new Point2D.Double(1.0, 1.0),
            new Point2D.Double(0.0, 1.0),
        };
        for (int i = 0; i < 4; i++) {
            testCubic(points);
            testQuad(points);
            testLines(points);
            rotate(points);
        }
    }

    public static void testCubic(Point2D points[]) {
        CubicCurve2D cubic =
            new CubicCurve2D.Double(points[0].getX(), points[0].getY(),
                                    points[1].getX(), points[1].getY(),
                                    points[2].getX(), points[2].getY(),
                                    points[3].getX(), points[3].getY());
        Area a = new Area(cubic);
        if (!cubic.getBounds2D().contains(a.getBounds2D())) {
            throw new RuntimeException("Area bbox is larger than cubic");
        }
        double x = (points[0].getX() +
                    (points[1].getX() + points[2].getX()) * 3.0 +
                    points[3].getX()) / 8.0;
        double y = (points[0].getY() +
                    (points[1].getY() + points[2].getY()) * 3.0 +
                    points[3].getY()) / 8.0;
        Rectangle2D r =
            new Rectangle2D.Double(points[0].getX(), points[0].getY(), 0, 0);
        r.add(points[3].getX(), points[3].getY());
        r.add(x, y);
        checkBox(r, a.getBounds2D());
    }

    public static void testQuad(Point2D points[]) {
        QuadCurve2D quad =
            new QuadCurve2D.Double(points[0].getX(), points[0].getY(),
                                   (points[1].getX() + points[2].getX()) / 2.0,
                                   (points[1].getY() + points[2].getY()) / 2.0,
                                   points[3].getX(), points[3].getY());
        Area a = new Area(quad);
        if (!quad.getBounds2D().contains(a.getBounds2D())) {
            throw new RuntimeException("Area bbox is larger than quad");
        }
        // p0 + 2cp + p1 == p0 + 2(cp0+cp1)/2 + p1 == p0 + cp0 + cp1 + p1
        double x = (points[0].getX() +
                    points[1].getX() + points[2].getX() +
                    points[3].getX()) / 4.0;
        double y = (points[0].getY() +
                    points[1].getY() + points[2].getY() +
                    points[3].getY()) / 4.0;
        Rectangle2D r =
            new Rectangle2D.Double(points[0].getX(), points[0].getY(), 0, 0);
        r.add(points[3].getX(), points[3].getY());
        r.add(x, y);
        checkBox(r, a.getBounds2D());
    }

    public static void testLines(Point2D points[]) {
        GeneralPath gp = new GeneralPath();
        gp.moveTo((float) points[0].getX(), (float) points[0].getY());
        gp.lineTo((float) points[2].getX(), (float) points[2].getY());
        gp.lineTo((float) points[1].getX(), (float) points[1].getY());
        gp.lineTo((float) points[3].getX(), (float) points[3].getY());
        gp.closePath();
        Area a = new Area(gp);
        if (!gp.getBounds2D().contains(a.getBounds2D())) {
            throw new RuntimeException("Area bbox is larger than poly");
        }
        Rectangle2D r =
            new Rectangle2D.Double(points[3].getX(), points[3].getY(), 0, 0);
        for (int i = 0; i < 3; i++) {
            r.add(points[i].getX(), points[i].getY());
        }
        checkBox(r, a.getBounds2D());
    }

    public static void checkBox(Rectangle2D r1, Rectangle2D r2) {
        checkVal(r1.getMinX(), r2.getMinX());
        checkVal(r1.getMinY(), r2.getMinY());
        checkVal(r2.getMaxX(), r1.getMaxX());
        checkVal(r2.getMaxY(), r1.getMaxY());
    }

    /*
     * v1 and v2 should be roughly equal.  The amount of computation
     * involved in calculating bounding boxes is complex enough that
     * you might expect the values to be slightly off and this should
     * perhaps be a loose comparison, but our runtime seems to produce
     * values that are exactly equal (even with double arithmetic),
     * so this test will enforce exact equality for now.
     */
    public static void checkVal(double v1, double v2) {
        if (v1 != v2) {
            throw new RuntimeException("bounding box not minimal");
        }
    }

    public static void rotate(Point2D[] points) {
        Point2D p = points[0];
        System.arraycopy(points, 1, points, 0, 3);
        points[3] = p;
    }
}
