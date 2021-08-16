/*
 * Copyright (c) 2000, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4320890
 * @summary Verifies that the Rectangle2D.intersectsLine method does not hang
 *          or return bad results due to inaccuracies in the outcode methods
 * @run main/timeout=5 IntersectsLineHang
 */

import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;

public class IntersectsLineHang {
    static public void main(String args[]) {
        Rectangle r = new Rectangle(0x70000000, 0x70000000,
                                    0x20000000, 0x0f000000);
        double v = 0x78000000;
        System.out.println("Point alpha");
        boolean result = r.intersectsLine(v, v, v+v, v+v);
        System.out.println(result);
        Rectangle2D rect = new Rectangle2D.Float(29.790712356567383f,
                                                 362.3290710449219f,
                                                 267.40679931640625f,
                                                 267.4068298339844f);
        System.out.println("Point A");
        System.out.println(rect.intersectsLine(431.39777, 551.3534,
                                               26.391, 484.71542));
        System.out.println("Point A2");
        System.out.println(rect.intersectsLine(431.39777, 551.3534,
                                               268.391, 484.71542));
        System.out.println("Point B: Never gets here!");
        if (!result) {
            // failure of integer rectangle intersection test
            throw new RuntimeException("integer rectangle "+
                                       "failed intersection test");
        }
    }
}
