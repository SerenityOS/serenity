/*
 * Copyright (c) 2000, 2018, Oracle and/or its affiliates. All rights reserved.
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

package sun.java2d.pipe;

import java.awt.Rectangle;
import java.awt.Shape;
import sun.java2d.SunGraphics2D;
import sun.font.GlyphList;

/*
 * This class uses the alpha graybits arrays from a GlyphList object to
 * drive a CompositePipe in much the same way as the antialiasing renderer.
 */
public class TextRenderer extends GlyphListPipe {

    CompositePipe outpipe;

    public TextRenderer(CompositePipe pipe) {
        outpipe = pipe;
    }

    protected void drawGlyphList(SunGraphics2D sg2d, GlyphList gl) {
        int num = gl.getNumGlyphs();
        Region clipRegion = sg2d.getCompClip();
        int cx1 = clipRegion.getLoX();
        int cy1 = clipRegion.getLoY();
        int cx2 = clipRegion.getHiX();
        int cy2 = clipRegion.getHiY();
        Object ctx = null;
        try {
            gl.startGlyphIteration();
            int[] bounds = gl.getBounds(num);
            Rectangle r = new Rectangle(bounds[0], bounds[1],
                                        bounds[2] - bounds[0],
                                        bounds[3] - bounds[1]);
            Shape s = sg2d.untransformShape(r);
            ctx = outpipe.startSequence(sg2d, s, r, bounds);
            for (int i = 0; i < num; i++) {
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
                if (gx2 > gx1 && gy2 > gy1 && !gl.isColorGlyph(i) &&
                    outpipe.needTile(ctx, gx1, gy1, gx2 - gx1, gy2 - gy1))
                {
                    byte[] alpha = gl.getGrayBits();
                    outpipe.renderPathTile(ctx, alpha, off, w,
                                           gx1, gy1, gx2 - gx1, gy2 - gy1);
                } else {
                    outpipe.skipTile(ctx, gx1, gy1);
                }
            }
        } finally {
            if (ctx != null) {
                outpipe.endSequence(ctx);
            }
        }
    }
}
