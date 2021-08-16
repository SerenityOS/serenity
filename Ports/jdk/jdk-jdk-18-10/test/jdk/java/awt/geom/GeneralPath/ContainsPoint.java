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
 * @bug 6396047
 * @summary tests the GeneralPath.contains(x, y) method
 */

import java.awt.geom.GeneralPath;

public class ContainsPoint {
    public static void main(String args[]) {
        GeneralPath gp = new GeneralPath(GeneralPath.WIND_NON_ZERO);
        gp.moveTo(46.69187927246094f, 95.8778305053711f);
        gp.curveTo(-122.75305938720703f, 14.31462574005127f,
                   66.84117889404297f, 26.061769485473633f,
                   -62.62519073486328f, -13.041547775268555f);
        gp.closePath();
        if (gp.contains(-122.75305938720703, -13.041547775268555)) {
            throw new RuntimeException("contains point clearly "+
                                       "outside of curve");
        }
    }
}
