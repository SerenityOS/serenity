/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
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

package sun.font;

import sun.awt.*;
import sun.java2d.SunGraphics2D;
import sun.java2d.pipe.GlyphListPipe;
import sun.java2d.xr.*;

/**
 * A delegate pipe of SG2D for drawing any text to a XRender surface
 *
 * @author Clemens Eisserer
 */
public class XRTextRenderer extends GlyphListPipe {
    // Workarround for a bug in libXrender.
    // In case the number of glyphs of an ELT is a multiple of 254,
    // a few garbage bytes are sent to the XServer causing hangs.
    static final int MAX_ELT_GLYPH_COUNT = 253;

    XRGlyphCache glyphCache;
    XRCompositeManager maskBuffer;
    XRBackend backend;

    GrowableEltArray eltList;

    public XRTextRenderer(XRCompositeManager buffer) {
        glyphCache = new XRGlyphCache(buffer);
        maskBuffer = buffer;
        backend = buffer.getBackend();
        eltList = new GrowableEltArray(64);
    }

    protected void drawGlyphList(SunGraphics2D sg2d, GlyphList gl) {
        if (gl.getNumGlyphs() == 0) {
            return;
        }

        try {
            SunToolkit.awtLock();

            XRSurfaceData x11sd = (XRSurfaceData) sg2d.surfaceData;
            x11sd.validateAsDestination(null, sg2d.getCompClip());
            x11sd.maskBuffer.validateCompositeState(sg2d.composite, sg2d.transform, sg2d.paint, sg2d);

            float advX = gl.getX();
            float advY = gl.getY();
            int oldPosX = 0, oldPosY = 0;

            if (gl.isSubPixPos()) {
                advX += 0.1666667f;
                advY += 0.1666667f;
            } else {
                advX += 0.5f;
                advY += 0.5f;
            }

            XRGlyphCacheEntry[] cachedGlyphs = glyphCache.cacheGlyphs(gl);
            boolean containsLCDGlyphs = false;
            int activeGlyphSet = cachedGlyphs[0].getGlyphSet();

            int eltIndex = -1;
            gl.startGlyphIteration();
            float[] positions = gl.getPositions();
            for (int i = 0; i < gl.getNumGlyphs(); i++) {
                gl.setGlyphIndex(i);
                XRGlyphCacheEntry cacheEntry = cachedGlyphs[i];
                if (cacheEntry == null) {
                    continue;
                }

                eltList.getGlyphs().addInt(cacheEntry.getGlyphID());
                int glyphSet = cacheEntry.getGlyphSet();

                containsLCDGlyphs |= (glyphSet == glyphCache.lcdGlyphSet);

                int posX = 0, posY = 0;
                if (gl.usePositions()
                        || cacheEntry.getXAdvance() != ((float) cacheEntry.getXOff())
                        || cacheEntry.getYAdvance() != ((float) cacheEntry.getYOff())
                        || glyphSet != activeGlyphSet
                        || eltIndex < 0
                        || eltList.getCharCnt(eltIndex) == MAX_ELT_GLYPH_COUNT) {

                    eltIndex = eltList.getNextIndex();
                    eltList.setCharCnt(eltIndex, 1);
                    activeGlyphSet = glyphSet;
                    eltList.setGlyphSet(eltIndex, glyphSet);

                    if (gl.usePositions()) {
                        // In this case advX only stores rounding errors
                        float x = positions[i * 2] + advX;
                        float y = positions[i * 2 + 1] + advY;
                        posX = (int) Math.floor(x);
                        posY = (int) Math.floor(y);
                        advX -= cacheEntry.getXOff();
                        advY -= cacheEntry.getYOff();
                    } else {
                        /*
                         * Calculate next glyph's position in the case of
                         * relative positioning. In XRender we can only position
                         * glyphs using integer coordinates, therefor we sum all
                         * the advances up as float, and convert them to integer
                         * later. This way rounding-error can be corrected, and
                         * is required to be consistent with the software loops.
                         */
                        posX = (int) Math.floor(advX);
                        posY = (int) Math.floor(advY);

                        // Advance of ELT = difference between stored relative
                        // positioning information and required float.
                        advX += (cacheEntry.getXAdvance() - cacheEntry.getXOff());
                        advY += (cacheEntry.getYAdvance() - cacheEntry.getYOff());
                    }

                    // Offset of the current glyph is the difference
                    // to the last glyph and this one
                    eltList.setXOff(eltIndex, (posX - oldPosX));
                    eltList.setYOff(eltIndex, (posY - oldPosY));

                    oldPosX = posX;
                    oldPosY = posY;

                } else {
                    eltList.setCharCnt(eltIndex, eltList.getCharCnt(eltIndex) + 1);
                }
            }

            int maskFormat = containsLCDGlyphs ? XRUtils.PictStandardARGB32 : XRUtils.PictStandardA8;
            maskBuffer.compositeText(x11sd, (int) gl.getX(), (int) gl.getY(), 0, maskFormat, eltList);

            eltList.clear();
        } finally {
            SunToolkit.awtUnlock();
        }
    }
}
