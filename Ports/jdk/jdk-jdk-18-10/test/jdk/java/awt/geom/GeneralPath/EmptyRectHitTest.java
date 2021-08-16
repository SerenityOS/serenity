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
 * @bug 6284484
 * @summary Verifies that GeneralPath objects do not "contain" or "intersect"
 *          empty rectangles
 */

import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;

public class EmptyRectHitTest {
    public static int numerrors;

    public static void main(String argv[]) {
        Rectangle select1 = new Rectangle(1, 1, 1, 1);
        Rectangle select2 = new Rectangle(1, 1, 0, 0);
        Rectangle select3 = new Rectangle(100, 100, 1, 1);
        Rectangle select4 = new Rectangle(100, 100, 0, 0);

        Rectangle rect = new Rectangle(-5, -5, 10, 10);
        test(rect, select1, true,  true);
        test(rect, select2, false, false);
        test(rect, select3, false, false);
        test(rect, select4, false, false);

        GeneralPath gp = new GeneralPath(rect);
        test(gp, select1, true,  true);
        test(gp, select2, false, false);
        test(gp, select3, false, false);
        test(gp, select4, false, false);

        AffineTransform xform = new AffineTransform();
        xform.setToRotation(Math.PI / 4);
        Shape shape = xform.createTransformedShape(rect);
        test(shape, select1, true,  true);
        test(shape, select2, false, false);
        test(shape, select3, false, false);
        test(shape, select4, false, false);

        if (numerrors > 0) {
            throw new RuntimeException(numerrors+" errors in tests");
        }
    }

    public static void test(Shape testshape, Rectangle rect,
                            boolean shouldcontain, boolean shouldintersect)
    {
        if (testshape.contains(rect) != shouldcontain) {
            error(testshape, rect, "contains", !shouldcontain);
        }
        if (testshape.intersects(rect) != shouldintersect) {
            error(testshape, rect, "intersects", !shouldintersect);
        }
    }

    public static void error(Shape t, Rectangle r, String type, boolean res) {
        numerrors++;
        System.err.println(t+type+"("+r+") == "+res);
    }
}
