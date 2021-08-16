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
 * @bug 4724569
 * @summary Arc2D "contains" and "intersects" problems, part II: fix enclosed.
 */

import java.awt.Shape;
import java.awt.geom.Arc2D;
import java.awt.geom.Rectangle2D;

public class Arc2DIntersectsTest {

    static Shape[][] trues = {
        {
            new Arc2D.Double(0, 0, 100, 100, -45, 90, Arc2D.PIE),
            new Rectangle2D.Double(0, 0, 100, 100),
        },
        {
            new Arc2D.Double(0, 0, 100, 100, -45, 90, Arc2D.PIE),
            new Rectangle2D.Double(25, 25, 50, 50)
        },
        {
            new Arc2D.Double(0, 0, 100, 100, -45, 90, Arc2D.PIE),
            new Rectangle2D.Double(60, 0, 20, 100)
        },
        {
            new Arc2D.Double(0, 0, 100, 100, -135, 270, Arc2D.CHORD),
            new Rectangle2D.Double(20, 0, 20, 100)
        }
    };
    static Shape[][] falses = {
        {
            new Arc2D.Double(0, 0, 100, 100, 0, 360, Arc2D.PIE),
            new Rectangle2D.Double(0, 100, 100, 100)
        },
        {
            new Arc2D.Double(0, 0, 100, 100, 45, 20, Arc2D.PIE),
            new Rectangle2D.Double(75, 75, 100, 100)
        },
        {
            new Arc2D.Double(0, 0, 100, 100, -10, 10, Arc2D.PIE),
            new Rectangle2D.Double(50, 75, 50, 100)
        },
        {
            new Arc2D.Double(0, 0, 100, 100, -10, 10, Arc2D.CHORD),
            new Rectangle2D.Double(60, 0, 10, 100)
        }
    };

    public static void main(String[] args) {

        for (int i = 0; i < trues.length; i++) {
            checkPair((Arc2D)trues[i][0], (Rectangle2D)trues[i][1], true);
        }

        for (int i = 0; i < falses.length; i++) {
            checkPair((Arc2D)falses[i][0], (Rectangle2D)falses[i][1], false);
        }
    }

    public static void checkPair(Arc2D arc, Rectangle2D rect, boolean expect) {

        if (arc.intersects(rect) != expect) {
            String errMsg = "Intersection of Arc(" +
                            arc.getX() + ", " + arc.getY() + ", " +
                            arc.getWidth() + ", " + arc.getHeight() + ", " +
                            "start = " + arc.getAngleStart() + ", " +
                            "extent = " + arc.getAngleExtent() + ", " +
                            typeName(arc.getArcType()) + ") and Rectangle(" +
                            rect.getX() + ", " + rect.getY() + ", " +
                            rect.getWidth() + ", " + rect.getHeight() + ") " +
                            "should be " + expect + ", BUT IT IS NOT.";
            throw new RuntimeException(errMsg);
        }
    }

    public static String typeName(int type) {

        if (type == Arc2D.OPEN)
            return "Open";
        if (type == Arc2D.CHORD)
            return "Chord";
        if (type == Arc2D.PIE)
            return "Pie";
        return null;
    }
}
