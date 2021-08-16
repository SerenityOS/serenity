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
 * @bug 4725016
 * @summary Tests the contains() method for not a convex pie with negative ext
 */

import java.awt.geom.Arc2D;
import java.awt.geom.Rectangle2D;

public class NotConvexPieTest {

    Arc2D aNegative = new Arc2D.Double(-100, -100, 200, 200,
                                       -45, -270, Arc2D.PIE);
    // this rectangle surely have all vertices inside the
    // arc above but intersects the pie slices
    Rectangle2D rect = new Rectangle2D.Double(-20, -40, 40, 80);

    String failText = "Test failed: rect should not be contained in arc due to "
            + "intersections with radii";

    public void runTest() {

        boolean contains = aNegative.contains(rect.getX(),
                                              rect.getY(),
                                              rect.getWidth(),
                                              rect.getHeight());
        if (contains) {
            // test failed
            throw new RuntimeException(failText);
        }
    }

    public static void main(String[] args) {
        NotConvexPieTest test = new NotConvexPieTest();
        test.runTest();
    }
}
