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
 * @bug 4188529 4240423
 * @summary Verifies that the Area methods do not cache the bounds
 *          across operations.
 */

import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;

public class BoundsCache {

    public static void main(String s[]) {
        Ellipse2D cir = new Ellipse2D.Double(110.0,100.0, 50.0, 50.0);
        Ellipse2D cir2 = new Ellipse2D.Double(110.0,100.0, 50.0, 50.0);
        Area area_cir = new Area (cir);

        System.out.println("before operation (bounds should be non-empty):");
        System.out.println("   bounds are "+area_cir.getBounds());
        System.out.println("   2D bounds are "+area_cir.getBounds2D());
        System.out.println("   isEmpty returns "+area_cir.isEmpty());
        area_cir.subtract(new Area (cir2));
        System.out.println("after operation (bounds should be empty):");
        System.out.println("   bounds are "+area_cir.getBounds());
        System.out.println("   2D bounds are "+area_cir.getBounds2D());
        System.out.println("   isEmpty returns "+area_cir.isEmpty());
        if (!area_cir.isEmpty() ||
            !area_cir.getBounds().isEmpty() ||
            !area_cir.getBounds2D().isEmpty())
        {
            throw new RuntimeException("result was not empty!");
        }

        Area area = new Area(cir);
        area.getBounds();
        area.reset();
        if (!area.getBounds().isEmpty()) {
            throw new RuntimeException("result was not empty!");
        }

        area = new Area(cir);
        Rectangle r = area.getBounds();
        area.transform(new AffineTransform(1, 0, 0, 1, 10, 0));
        if (area.getBounds().equals(r)) {
            throw new RuntimeException("bounds not updated in transform!");
        }
    }
}
