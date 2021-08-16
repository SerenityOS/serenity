/*
 * Copyright (c) 2009, Oracle and/or its affiliates. All rights reserved.
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

/* @test
 * @summary leading and trailing spaces must not affect visual bounds
 * @bug 6904962
 */


import java.awt.*;
import java.awt.font.*;
import java.awt.geom.*;

public class VisualBounds {

    public static void main(String args[]) {

        String s1 = "a";
        String s2 = s1+" ";
        String s3 = " "+s1;
        Font f = new Font("Dialog", Font.PLAIN, 12);
        FontRenderContext frc = new FontRenderContext(null, false, false);
        GlyphVector gv1 = f.createGlyphVector(frc, s1);
        GlyphVector gv2 = f.createGlyphVector(frc, s2);
        GlyphVector gv3 = f.createGlyphVector(frc, s3);
        Rectangle2D bds1 = gv1.getVisualBounds();
        Rectangle2D bds2 = gv2.getVisualBounds();
        Rectangle2D bds3 = gv3.getVisualBounds();
        GlyphVector gv4 = f.createGlyphVector(frc, " ");
        Rectangle2D bds4 = gv4.getVisualBounds();
        System.out.println(bds1);
        System.out.println(bds2);
        System.out.println(bds3);
        System.out.println(bds4);

        if (!bds1.equals(bds2)) {
          throw new RuntimeException("Trailing space: Visual bounds differ");
        }
        if (bds2.getWidth() != bds3.getWidth()) {
          throw new RuntimeException("Leading space: Visual widths differ");
       }
        if (!bds4.isEmpty()) {
          throw new RuntimeException("Non empty bounds for space");
       }
    }
}
