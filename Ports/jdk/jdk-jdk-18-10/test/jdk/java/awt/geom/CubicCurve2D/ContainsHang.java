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
 * @bug 4246661
 * @summary Verifies that the CubicCurve2D.contains method does not hang
 * @run main/timeout=5 ContainsHang
 */

import java.awt.geom.CubicCurve2D;

public class ContainsHang {
    public static void main(String args[]) {
        CubicCurve2D curve =
            new CubicCurve2D.Double(83.0, 101.0,
                                    -5.3919918959078075, 94.23530547506019,
                                    71.0, 39.0,
                                    122.0, 44.0);

        System.out.println("Checking contains() >>>");
        curve.contains(71.0, 44.0);
        System.out.println("Check complete!!");
    }
}
