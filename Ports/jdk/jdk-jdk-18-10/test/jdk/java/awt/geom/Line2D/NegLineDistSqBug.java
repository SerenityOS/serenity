/*
 * Copyright (c) 2002, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4335024
 * @summary Verifies that the ptXXXDistSq methods return non-negative numbers
 */

import java.awt.geom.Line2D;

public class NegLineDistSqBug {
    static int errored;

    public static void main (String[] args) {
        // First a sanity check
        test(1, 2, 3, 4, 15, 19, 21, 32);
        // Next, the test numbers from bug report 4335024
        test(-313.0, 241.0, -97.0, 75.0,
             126.15362619253153, -96.49769420351959, -97.0, 75.0);
        // Next test 100 points along the line from bug report 4335024
        for (double fract = 0.0; fract <= 0.5; fract += 0.01) {
            test(-313.0, 241.0, -97.0, 75.0,
                 interp(-313.0, -97.0, fract), interp(241.0, 75.0, fract),
                 interp(-313.0, -97.0, 1-fract), interp(241.0, 75.0, 1-fract));
        }
        if (errored > 0) {
            throw new RuntimeException(errored+" negative distances!");
        }
    }

    public static double interp(double v1, double v2, double t) {
        return (v1 * (1-t) + v2 * t);
    }

    public static void test(double l1x1, double l1y1,
                            double l1x2, double l1y2,
                            double l2x1, double l2y1,
                            double l2x2, double l2y2)
    {
        Line2D l1 = new Line2D.Double(l1x1, l1y1, l1x2, l1y2);
        Line2D l2 = new Line2D.Double(l2x1, l2y1, l2x2, l2y2);
        System.out.println("Line distances:");
        verify(l1.ptLineDistSq(l2.getP1()));
        verify(l1.ptLineDistSq(l2.getP2()));
        System.out.println("Segment distances:");
        verify(l1.ptSegDistSq(l2.getP1()));
        verify(l1.ptSegDistSq(l2.getP2()));
    }

    public static void verify(double distSq) {
        System.out.println(distSq);
        if (distSq < 0) {
            errored++;
        }
    }
}
