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
 * @bug 6385010
 * @summary Checks that transform does not fail with exception
 */

import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.GeneralPath;

/* Minimized testcase from bug report */

public class AreaTransformTest {

    public static void main(String[] args) {
        AffineTransform t = AffineTransform.getRotateInstance(Math.PI/8);
        GeneralPath path = new GeneralPath();

        path.moveTo(-4516.23223633003f,10983.71557514126f);
        path.lineTo(-1451.4908513919768f, 13100.559659959084f);
        path.quadTo(-54.38163118565376f, 13679.261247085042f,
                    1470.6331984752403f, 13679.261247085042f);
        path.closePath();

        Area area = new Area(path);

        for (int i = 0; i < 8; i++) {
            area.transform(t);
        }
    }

}
