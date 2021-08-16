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
 * @bug     7036754
 *
 * @summary Verifies that there are no non-finite numbers when stroking
 *          certain quadratic curves.
 *
 * @author Jim Graham
 * @run     main Test7036754
 */

import java.awt.*;
import java.awt.geom.*;

public class Test7036754 {
    public static void main(String argv[]) {
        Shape s = new QuadCurve2D.Float(839.24677f, 508.97888f,
                                        839.2953f, 508.97122f,
                                        839.3438f, 508.96353f);
        s = new BasicStroke(10f).createStrokedShape(s);
        float nsegs[] = {2, 2, 4, 6, 0};
        float coords[] = new float[6];
        PathIterator pi = s.getPathIterator(null);
        while (!pi.isDone()) {
            int type = pi.currentSegment(coords);
            for (int i = 0; i < nsegs[type]; i++) {
                float c = coords[i];
                if (Float.isNaN(c) || Float.isInfinite(c)) {
                    throw new RuntimeException("bad value in stroke");
                }
            }
            pi.next();
        }
    }
}
