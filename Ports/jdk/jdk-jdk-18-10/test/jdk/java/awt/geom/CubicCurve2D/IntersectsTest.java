/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 4493128
 * @summary Verifies that CubicCurve2D returns true for obvious intersection
 * @run main IntersectsTest
 */

import java.awt.geom.CubicCurve2D;
import java.awt.geom.Rectangle2D;

public class IntersectsTest {

    public static void main(String[] args) throws Exception {
        CubicCurve2D c = new CubicCurve2D.Double(50.0, 300.0,
                                                 150.0, 166.6666717529297,
                                                 238.0, 456.66668701171875,
                                                 350.0, 300.0);
        Rectangle2D r = new Rectangle2D.Double(260, 300, 10, 10);

        if (!c.intersects(r)) {
            throw new Exception("The rectangle is contained. " +
                                "intersects(Rectangle2D) should return true");
        }
    }
}
