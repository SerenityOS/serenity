/*
 * Copyright (c) 2005, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

package sun.java2d.loops;

import sun.font.GlyphList;
import sun.java2d.SunGraphics2D;
import sun.java2d.SurfaceData;

/**
 *   DrawGlyphListLCD- loops for LCDTextRenderer pipe
 *   1) draw LCD anti-aliased text onto destination surface
 *   2) must accept output area [x, y, dx, dy]
 *      from within the surface description data for clip rect
 */
public class DrawGlyphListLCD extends GraphicsPrimitive {

    public static final String
        methodSignature = "DrawGlyphListLCD(...)".toString();

    public static final int primTypeID = makePrimTypeID();

    public static DrawGlyphListLCD locate(SurfaceType srctype,
                                           CompositeType comptype,
                                           SurfaceType dsttype)
    {
        return (DrawGlyphListLCD)
            GraphicsPrimitiveMgr.locate(primTypeID,
                                        srctype, comptype, dsttype);
    }

    protected DrawGlyphListLCD(SurfaceType srctype,
                                CompositeType comptype,
                                SurfaceType dsttype)
    {
        super(methodSignature, primTypeID, srctype, comptype, dsttype);
    }

    public DrawGlyphListLCD(long pNativePrim,
                             SurfaceType srctype,
                             CompositeType comptype,
                             SurfaceType dsttype)
    {
        super(pNativePrim, methodSignature, primTypeID,
              srctype, comptype, dsttype);
    }

    public native void DrawGlyphListLCD(SunGraphics2D sg2d, SurfaceData dest,
                                        GlyphList srcData,
                                        int fromGlyph, int toGlyph);

    public GraphicsPrimitive traceWrap() {
        return new TraceDrawGlyphListLCD(this);
    }

    private static class TraceDrawGlyphListLCD extends DrawGlyphListLCD {
        DrawGlyphListLCD target;

        public TraceDrawGlyphListLCD(DrawGlyphListLCD target) {
            super(target.getSourceType(),
                  target.getCompositeType(),
                  target.getDestType());
            this.target = target;
        }

        public GraphicsPrimitive traceWrap() {
            return this;
        }

        public void DrawGlyphListLCD(SunGraphics2D sg2d, SurfaceData dest,
                                     GlyphList glyphs,
                                     int fromGlyph, int toGlyph)
        {
            tracePrimitive(target);
            target.DrawGlyphListLCD(sg2d, dest, glyphs, fromGlyph, toGlyph);
        }
    }
}
