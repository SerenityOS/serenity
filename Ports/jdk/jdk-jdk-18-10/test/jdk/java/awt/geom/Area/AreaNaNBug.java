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
 * @bug 6398602
 * @summary Checks that problems finding the roots of curves does not
 *          cause the Area class to end up with NaN coordinates after
 *          being constructed from an otherwise normal and unremarkable
 *          path.
 */

import java.awt.geom.Area;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;

public class AreaNaNBug {
    public static void main(String argv[]) {
        GeneralPath gp = new GeneralPath(GeneralPath.WIND_EVEN_ODD);
        gp.moveTo(-38.05311f, 75.25694f);
        gp.curveTo(-48.18971f, 71.23722f,
                   -46.495422f, -113.844574f,
                   124.16219f, 87.604744f);
        gp.quadTo(105.644554f, 114.52495f,
                  72.98282f, -78.52084f);
        gp.lineTo(107.358795f, 29.33548f);
        gp.quadTo(-16.562698f, -43.91586f,
                  51.50203f, 38.51295f);
        gp.lineTo(20.715876f, 44.4093f);
        gp.closePath();
        Area a = new Area(gp);
        Rectangle2D r2d = a.getBounds2D();
        if (Double.isNaN(r2d.getX()) ||
            Double.isNaN(r2d.getY()) ||
            Double.isNaN(r2d.getWidth()) ||
            Double.isNaN(r2d.getHeight()))
        {
            throw new RuntimeException("Area bounds have NaN");
        }
    }
}
