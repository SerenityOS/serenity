/*
 * Copyright (c) 2003, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4724580
 * @summary Pie slices should be colinear with vectors used to set angles.
 */

import java.awt.geom.Arc2D;
import java.awt.geom.Point2D;

public class SetAnglesTest {

    public void runTest() {

        Arc2D arc = new Arc2D.Double(-10, -40, 20, 80, 0, 0, Arc2D.PIE);
        Point2D p1 = new Point2D.Double(10, 10);
        Point2D p2 = new Point2D.Double(10, -10);
        double threshold = 1.0E-10;

        arc.setAngles(p1.getX(), p1.getY(), p2.getX(), p2.getY());

        Point2D start = arc.getStartPoint();
        Point2D end   = arc.getEndPoint();

        checkColinear(start.getX(), start.getY(), p1.getX(), p1.getY());
        checkColinear(end.getX(), end.getY(), p2.getX(), p2.getY());
    }

    void checkColinear(double dx1, double dy1, double dx2, double dy2) {

        double threshold = 1.0E-10;

        if (!(dx1 * dx2 >= 0 &&
              dy1 * dy2 >= 0 &&
              Math.abs(dx1 * dy2 - dx2 * dy1) <= threshold)) {
            throw new RuntimeException("Angles difference is too much");
        }
    }

    public static void main(String[] args) {
        SetAnglesTest test = new SetAnglesTest();
        test.runTest();
    }
}
