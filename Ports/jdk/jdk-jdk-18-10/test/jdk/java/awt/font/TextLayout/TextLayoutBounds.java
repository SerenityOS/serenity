/*
 * Copyright (c) 2005, 2007, Oracle and/or its affiliates. All rights reserved.
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
 * @summary verify TextLayout.getBounds() return visual bounds
 * @bug 6323611 6761856
 */

import java.awt.*;
import java.awt.font.*;
import java.awt.geom.*;

public class TextLayoutBounds {

    public static void main(String args[]) {
       FontRenderContext frc = new FontRenderContext(null, false, false);
       Font f = new Font("SansSerif",Font.BOLD,32);
       String s = new String("JAVA");
       TextLayout tl = new TextLayout(s, f, frc);
       Rectangle2D tlBounds = tl.getBounds();
       GlyphVector gv = f.createGlyphVector(frc, s);
       Rectangle2D gvvBounds = gv.getVisualBounds();
       Rectangle2D oBounds = tl.getOutline(null).getBounds2D();
       System.out.println("tlbounds="+tlBounds);
       System.out.println("gvbounds="+gvvBounds);
       System.out.println("outlineBounds="+oBounds);
       if (!gvvBounds.equals(tlBounds)) {
          throw new RuntimeException("Bounds differ [gvv != tl]");
       }
       if (!tlBounds.equals(oBounds)) {
          throw new RuntimeException("Bounds differ [tl != outline]");
       }
    }
}
