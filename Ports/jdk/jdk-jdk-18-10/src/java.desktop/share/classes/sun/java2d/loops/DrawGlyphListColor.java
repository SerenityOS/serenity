/*
 * Copyright 2021 JetBrains s.r.o.
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

import java.awt.*;

/**
 *   Draws color glyphs onto destination surface
 */
public class DrawGlyphListColor extends GraphicsPrimitive {

    public final static String methodSignature =
            "DrawGlyphListColor(...)".toString();

    public final static int primTypeID = makePrimTypeID();

    public static DrawGlyphListColor locate(SurfaceType srctype,
                                            CompositeType comptype,
                                            SurfaceType dsttype)
    {
        return (DrawGlyphListColor)
            GraphicsPrimitiveMgr.locate(primTypeID,
                                        srctype, comptype, dsttype);
    }

    protected DrawGlyphListColor(SurfaceType srctype,
                                 CompositeType comptype,
                                 SurfaceType dsttype)
    {
        super(methodSignature, primTypeID, srctype, comptype, dsttype);
    }


    public void DrawGlyphListColor(SunGraphics2D sg2d, SurfaceData dest,
                                   GlyphList srcData,
                                   int fromGlyph, int toGlyph) {
        // actual implementation is in the 'General' subclass
    }

    // This instance is used only for lookup.
    static {
        GraphicsPrimitiveMgr.registerGeneral(
                                new DrawGlyphListColor(null, null, null));
    }

    public GraphicsPrimitive makePrimitive(SurfaceType srctype,
                                           CompositeType comptype,
                                           SurfaceType dsttype) {
        return new General(srctype, comptype, dsttype);
    }

    private static class General extends DrawGlyphListColor {
        private final Blit blit;

        public General(SurfaceType srctype,
                       CompositeType comptype,
                       SurfaceType dsttype)
        {
            super(srctype, comptype, dsttype);
            blit = Blit.locate(SurfaceType.IntArgbPre,
                    CompositeType.SrcOverNoEa, dsttype);
        }

        public void DrawGlyphListColor(SunGraphics2D sg2d, SurfaceData dest,
                                  GlyphList gl, int fromGlyph, int toGlyph) {

            Region clip = sg2d.getCompClip();
            int cx1 = clip.getLoX();
            int cy1 = clip.getLoY();
            int cx2 = clip.getHiX();
            int cy2 = clip.getHiY();
            for (int i = fromGlyph; i < toGlyph; i++) {
                gl.setGlyphIndex(i);
                int[] metrics = gl.getMetrics();
                int x = metrics[0];
                int y = metrics[1];
                int w = metrics[2];
                int h = metrics[3];
                int gx1 = x;
                int gy1 = y;
                int gx2 = x + w;
                int gy2 = y + h;
                if (gx1 < cx1) gx1 = cx1;
                if (gy1 < cy1) gy1 = cy1;
                if (gx2 > cx2) gx2 = cx2;
                if (gy2 > cy2) gy2 = cy2;
                if (gx2 > gx1 && gy2 > gy1) {
                    blit.Blit(gl.getColorGlyphData(), dest, AlphaComposite.SrcOver, clip,
                            gx1 - x, gy1 - y, gx1, gy1, gx2 - gx1, gy2 - gy1);
                }
            }
        }
    }

    public GraphicsPrimitive traceWrap() {
        return new TraceDrawGlyphListColor(this);
    }

    private static class TraceDrawGlyphListColor extends DrawGlyphListColor {
        DrawGlyphListColor target;

        public TraceDrawGlyphListColor(DrawGlyphListColor target) {
            super(target.getSourceType(),
                  target.getCompositeType(),
                  target.getDestType());
            this.target = target;
        }

        public GraphicsPrimitive traceWrap() {
            return this;
        }

        public void DrawGlyphListColor(SunGraphics2D sg2d, SurfaceData dest,
                                  GlyphList glyphs, int fromGlyph, int toGlyph)
        {
            tracePrimitive(target);
            target.DrawGlyphListColor(sg2d, dest, glyphs, fromGlyph, toGlyph);
        }
    }
}
