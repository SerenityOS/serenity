/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates. All rights reserved.
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
import sun.java2d.pipe.Region;

/**
 *   DrawGlyphListAA - loops for AATextRenderer pipe
 *   1) draw anti-aliased text onto destination surface
 *   2) must accept output area [x, y, dx, dy]
 *      from within the surface description data for clip rect
 */
public class DrawGlyphListAA extends GraphicsPrimitive {

    public static final String methodSignature = "DrawGlyphListAA(...)".toString();

    public static final int primTypeID = makePrimTypeID();

    public static DrawGlyphListAA locate(SurfaceType srctype,
                                   CompositeType comptype,
                                   SurfaceType dsttype)
    {
        return (DrawGlyphListAA)
            GraphicsPrimitiveMgr.locate(primTypeID,
                                        srctype, comptype, dsttype);
    }

    protected DrawGlyphListAA(SurfaceType srctype,
                         CompositeType comptype,
                         SurfaceType dsttype)
    {
        super(methodSignature, primTypeID, srctype, comptype, dsttype);
    }

    public DrawGlyphListAA(long pNativePrim,
                           SurfaceType srctype,
                           CompositeType comptype,
                           SurfaceType dsttype)
    {
        super(pNativePrim, methodSignature, primTypeID, srctype, comptype, dsttype);
    }

    public native void DrawGlyphListAA(SunGraphics2D sg2d, SurfaceData dest,
                                       GlyphList srcData,
                                       int fromGlyph, int toGlyph);

    static {
        GraphicsPrimitiveMgr.registerGeneral(
                                   new DrawGlyphListAA(null, null, null));
    }

    protected GraphicsPrimitive makePrimitive(SurfaceType srctype,
                                              CompositeType comptype,
                                              SurfaceType dsttype) {
        return new General(srctype, comptype, dsttype);
    }

    public static class General extends DrawGlyphListAA {
        MaskFill maskop;

        public General(SurfaceType srctype,
                       CompositeType comptype,
                       SurfaceType dsttype)
        {
            super(srctype, comptype, dsttype);
            maskop = MaskFill.locate(srctype, comptype, dsttype);
        }

        public void DrawGlyphListAA(SunGraphics2D sg2d, SurfaceData dest,
                                    GlyphList gl, int fromGlyph, int toGlyph)
        {
            Region clip = sg2d.getCompClip();
            int cx1 = clip.getLoX();
            int cy1 = clip.getLoY();
            int cx2 = clip.getHiX();
            int cy2 = clip.getHiY();
            for (int i = fromGlyph; i < toGlyph; i++) {
                gl.setGlyphIndex(i);
                int[] metrics = gl.getMetrics();
                int gx1 = metrics[0];
                int gy1 = metrics[1];
                int w = metrics[2];
                int gx2 = gx1 + w;
                int gy2 = gy1 + metrics[3];
                int off = 0;
                if (gx1 < cx1) {
                    off = cx1 - gx1;
                    gx1 = cx1;
                }
                if (gy1 < cy1) {
                    off += (cy1 - gy1) * w;
                    gy1 = cy1;
                }
                if (gx2 > cx2) gx2 = cx2;
                if (gy2 > cy2) gy2 = cy2;
                if (gx2 > gx1 && gy2 > gy1) {
                    byte[] alpha = gl.getGrayBits();
                    maskop.MaskFill(sg2d, dest, sg2d.composite,
                                    gx1, gy1, gx2 - gx1, gy2 - gy1,
                                    alpha, off, w);
                }
            }
        }
    }

    public GraphicsPrimitive traceWrap() {
        return new TraceDrawGlyphListAA(this);
    }

    private static class TraceDrawGlyphListAA extends DrawGlyphListAA {
        DrawGlyphListAA target;

        public TraceDrawGlyphListAA(DrawGlyphListAA target) {
            super(target.getSourceType(),
                  target.getCompositeType(),
                  target.getDestType());
            this.target = target;
        }

        public GraphicsPrimitive traceWrap() {
            return this;
        }

        public void DrawGlyphListAA(SunGraphics2D sg2d, SurfaceData dest,
                                    GlyphList glyphs,
                                    int fromGlyph, int toGlyph)
        {
            tracePrimitive(target);
            target.DrawGlyphListAA(sg2d, dest, glyphs, fromGlyph, toGlyph);
        }
    }
}
