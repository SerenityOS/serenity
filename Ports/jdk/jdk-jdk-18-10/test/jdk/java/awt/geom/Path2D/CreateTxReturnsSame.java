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
 * @bug 6404847
 * @summary Tests that Path2D.createTransformedShape() returns
 *          the same kind of object as the source.
 */

import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Path2D;

public class CreateTxReturnsSame {
    public static void main(String argv[]) {
        test(new GeneralPath());
        test(new Path2D.Float());
        test(new Path2D.Double());
    }

    public static void test(Path2D p2d) {
        p2d.moveTo(0, 0);
        p2d.lineTo(10, 10);
        Shape s1 = p2d.createTransformedShape(null);
        Shape s2 = p2d.createTransformedShape(new AffineTransform());
        if (s1.getClass() != p2d.getClass() ||
            s2.getClass() != p2d.getClass())
        {
            throw new RuntimeException(p2d.getClass().getName()+
                                       ".createTransformedShape() "+
                                       "did not return a "+
                                       p2d.getClass().getName());
        }
    }
}
