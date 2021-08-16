/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @test %W% %E%
 * @bug 7016495
 * @summary Test tiny scales of BufferedImage
 */

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.*;

public class TinyScale {
    static double tinyscales[] = {
        1E-0,
        1E-1,
        1E-2,
        1E-3,
        1E-4,
        1E-5,
        1E-6,
        1E-7,
        1E-8,
        1E-9,
        1E-10,
        1E-11,
        1E-12,
        1E-13,
        1E-14,
        1E-15,
        1E-16,
        1E-17,
        1E-18,
        1E-19,
        1E-20,
        1E-21,
        1E-22,
        1E-23,
        1E-24,
        1E-25,
        1E-26,
        1E-27,
        1E-28,
        1E-29,
    };

    static void test(BufferedImage rendImg, BufferedImage drawImg, double s) {
        Graphics2D g = drawImg.createGraphics();
        g.transform(new AffineTransform(s, 0.0, -1.0, 1.0, 0.0, 0.0));
        g.drawImage(rendImg,
                    -rendImg.getWidth() / 2,
                    -rendImg.getHeight() / 2,
                    null);
        g.drawImage(rendImg, 0, 0, null);
        g.dispose();
    }

    public static void main(String[] args) {
        BufferedImage rendImg =
            new BufferedImage(100, 100, BufferedImage.TYPE_3BYTE_BGR);
        BufferedImage drawImg =
            new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB);
        for (double s: tinyscales) {
            test(rendImg, drawImg, s);
            for (int i = 0; args.length > 0 && i < 10; i++) {
                test(rendImg, drawImg, Math.random()*s);
            }
        }
    }
}
