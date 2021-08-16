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
 * @summary Check that Rectangle constructors and methods do not throw unexpected
 *          exceptions in headless mode
 * @run main/othervm -Djava.awt.headless=true HeadlessRectangle
 */

public class HeadlessRectangle {
    public static void main(String args[]) {
        Rectangle r;
        r = new Rectangle();
        r = new Rectangle(new Rectangle());
        r = new Rectangle(100, 200);
        r = new Rectangle(new Point(100, 200), new Dimension(300, 400));
        r = new Rectangle(new Point(100, 200));
        r = new Rectangle(new Dimension(300, 400));
        r = new Rectangle(100, 200, 300, 400);
        r.getX();
        r.getY();
        r.getWidth();
        r.getHeight();
        r.getBounds();
        r.getBounds2D();
        r.getLocation();
        r.getSize();
        r.contains(new Point(1, 2));
        r.contains(1, 2);
        r.contains(new Rectangle(1, 2, 3, 4));
        r.contains(1, 2, 3, 4);
        r.add(1, 2);
        r.add(new Point(1, 2));
        r.add(new Rectangle(1, 2, 3, 4));
        r.grow(1, 2);
        r.isEmpty();
        r.toString();
        r.hashCode();
        r.getMinX();
        r.getMinY();
        r.getMaxX();
        r.getMaxY();
        r.getCenterX();
        r.getCenterY();
        r.getFrame();
    }
}
