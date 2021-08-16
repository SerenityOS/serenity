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
 * @bug 4286894
 * @summary Verifies that a particular case of Polygon addition
 *          correctly calculates the ordering of the Polygon edges.
 */

import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;

public class PolygonAdd {
    public static void main(String argv[]) {
        // Original shapes: 4 touching rectangles
        // (area works correctly with them if not transformed)
        Shape b = new Rectangle(0,0,100,10);
        Shape t = new Rectangle(0,90,100,10);
        Shape l = new Rectangle(0,10,10,80);
        Shape r = new Rectangle(90,10,10,80);

        // Create a transform to rotate them by 10 degrees
        AffineTransform M = new AffineTransform();
        M.translate(-50,-50);
        M.scale(3,3);
        M.rotate(Math.toRadians(10));
        M.translate(70,40);

        Area area = new Area();
        area.add(new Area(M.createTransformedShape(b)));
        area.add(new Area(M.createTransformedShape(l)));
        area.add(new Area(M.createTransformedShape(t)));
        area.add(new Area(M.createTransformedShape(r)));

        if (!area.contains(295, 145, 5, 5)) {
            throw new RuntimeException("Area addition failed!");
        }
    }
}
