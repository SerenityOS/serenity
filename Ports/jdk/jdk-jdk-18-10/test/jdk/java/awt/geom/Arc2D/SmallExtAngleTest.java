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
 * @bug 4836495
 * @summary Very small ext angle of arc should not give NaNs in SEG_CUBICTO
 */

import java.awt.geom.Arc2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Rectangle2D;

public class SmallExtAngleTest {

    public static void main(String[] args) {

        String errorMsg = "NaN occured in coordinates of SEG_CUBICTO";
        Rectangle2D bounds = new Rectangle2D.Double(-100, -100, 200, 200);
        Arc2D arc = new Arc2D.Double(bounds, 90, -1.0E-7, Arc2D.PIE);
        double[] coords = new double[6];

        PathIterator p = arc.getPathIterator(null);
        while (!p.isDone()) {

            if (p.currentSegment(coords) == PathIterator.SEG_CUBICTO) {

                for (int i = 0; i < 6; i++) {
                    if (coords[i] != coords[i]) {
                        throw new RuntimeException(errorMsg);
                    }
                }
            }
            p.next();
        }
    }
}
