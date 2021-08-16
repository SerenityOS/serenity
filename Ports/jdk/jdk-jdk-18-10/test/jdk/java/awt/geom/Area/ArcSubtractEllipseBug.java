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
 * @bug 6356485
 * @summary Checks that subtracting a particular ellipse from an Arc
 *          does not cause an InternalError to be thrown.
 */
import java.awt.geom.Arc2D;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;

public class ArcSubtractEllipseBug {
    public static void main(String[] args) {
        double x = -4.250000000000002;
        double y = 0.0;
        double width = 8.5;
        double height = 8.5;
        double start = -450.0;
        double extent = 180.0;

        Arc2D outerArc = new Arc2D.Double(x, y, width, height,
                                          start, extent, Arc2D.PIE);

        Area tmp = new Area(outerArc);

        x = -4.000000000000002;
        y = 0.25;
        width = 8.0;
        height = 8.0;

        Ellipse2D innerArc = new Ellipse2D.Double(x, y, width, height);

        tmp.subtract(new Area(innerArc));
    }
}
