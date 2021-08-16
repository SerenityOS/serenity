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
 * @bug 4217589
 * @summary Verifies that a particular case of Polygon subtraction
 *          correctly calculates the ordering of the Polygon edges.
 */

import java.awt.geom.Area;
import java.awt.geom.GeneralPath;

public class PolygonSubtract {
    public static void main(String argv[]) {
        Area area = new Area();

        GeneralPath ring1 = new GeneralPath();

        ring1.moveTo(0, 45);
        ring1.lineTo(90, 0);
        ring1.lineTo(60, -45);
        ring1.lineTo(-60, -45);
        ring1.lineTo(-90, 0);

        ring1.closePath();

        area.add(new Area(ring1));

        GeneralPath ring2 = new GeneralPath();

        ring2.moveTo(0, 20);
        ring2.lineTo(100, 0);
        ring2.lineTo(30, -20);
        ring2.lineTo(-30, -20);
        ring2.lineTo(-100, 0);
        ring2.closePath();

        area.subtract(new Area(ring2));

        if (!area.contains(50, 13, 2, 2)) {
            throw new RuntimeException("Area subtraction failed!");
        }
    }
}
