/*
 * Copyright (c) 2007, 2014, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.*;

/*
 * @test
 * @summary Check that GradientPaint that constructors and methods do not
 *          throw unexpected exceptions in headless mode
 * @run main/othervm -Djava.awt.headless=true HeadlessGradientPaint
 */

public class HeadlessGradientPaint {
    public static void main(String args[]) {
        GradientPaint gp;
        gp = new GradientPaint(10, 10, Color.red, 20, 20, Color.blue);
        gp = new GradientPaint(new Point(10, 10), Color.red, new Point(20, 20), Color.blue);
        gp = new GradientPaint(10, 10, Color.red, 20, 20, Color.blue, true);
        gp = new GradientPaint(10, 10, Color.red, 20, 20, Color.blue, false);
        gp = new GradientPaint(new Point(10, 10), Color.red, new Point(20, 20), Color.blue, true);
        gp = new GradientPaint(new Point(10, 10), Color.red, new Point(20, 20), Color.blue, false);

        gp = new GradientPaint(10, 10, Color.red, 20, 20, Color.blue, false);
        gp.getPoint1();
        gp.getColor1();
        gp.getPoint2();
        gp.getColor2();
        gp.isCyclic();
        gp.getTransparency();
    }
}
