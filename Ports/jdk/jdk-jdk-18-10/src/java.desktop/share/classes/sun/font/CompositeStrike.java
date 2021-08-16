/*
 * Copyright (c) 2003, 2005, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.Font;
import java.awt.Rectangle;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/*
 * performance:
 * it seems expensive that when using a composite font for
 * every char you have to find which "slot" can display it.
 * Just the fact that you need to check at all ..
 * A composite glyph code ducks this by encoding the slot into the
 * glyph code, but you still need to get from char to glyph code.
 */
public final class CompositeStrike extends FontStrike {

    static final int SLOTMASK = 0xffffff;

    private CompositeFont compFont;
    private PhysicalStrike[] strikes;
    int numGlyphs = 0;

    CompositeStrike(CompositeFont font2D, FontStrikeDesc desc) {
        this.compFont = font2D;
        this.desc = desc;
        this.disposer = new FontStrikeDisposer(compFont, desc);
        if (desc.style != compFont.style) {
            algoStyle = true;
            if ((desc.style & Font.BOLD) == Font.BOLD &&
                ((compFont.style & Font.BOLD) == 0)) {
                boldness = 1.33f;
            }
            if ((desc.style & Font.ITALIC) == Font.ITALIC &&
                (compFont.style & Font.ITALIC) == 0) {
                italic = 0.7f;
            }
        }
        strikes = new PhysicalStrike[compFont.numSlots];
    }

    /* do I need this (see Strike::compositeStrikeForGlyph) */
    PhysicalStrike getStrikeForGlyph(int glyphCode) {
        return getStrikeForSlot(glyphCode >>> 24);
    }

    PhysicalStrike getStrikeForSlot(int slot) {
        if (slot >= strikes.length) {
            slot = 0;
        }
        PhysicalStrike strike = strikes[slot];
        if (strike == null) {
            strike =
                (PhysicalStrike)(compFont.getSlotFont(slot).getStrike(desc));

            strikes[slot] = strike;
        }
        return strike;
    }

    public int getNumGlyphs() {
        return compFont.getNumGlyphs();
    }

    StrikeMetrics getFontMetrics() {
        if (strikeMetrics == null) {
            StrikeMetrics compMetrics = new StrikeMetrics();
            for (int s=0; s<compFont.numMetricsSlots; s++) {
                compMetrics.merge(getStrikeForSlot(s).getFontMetrics());
            }
            strikeMetrics = compMetrics;
        }
        return strikeMetrics;
    }


    /* Performance tweak: Slot 0 can often return all the glyphs
     * Note slot zero doesn't need to be masked.
     * Could go a step further and support getting a run of glyphs.
     * This would help many locales a little.
     *
     * Note that if a client constructs an invalid a composite glyph that
     * references an invalid slot, that the behaviour is currently
     * that this slot index falls through to CompositeFont.getSlotFont(int)
     * which will substitute a default font, from which to obtain the
     * strike. If its an invalid glyph code for a valid slot, then the
     * physical font for that slot will substitute the missing glyph.
     */
    void getGlyphImagePtrs(int[] glyphCodes, long[] images, int  len) {
        PhysicalStrike strike = getStrikeForSlot(0);
        int numptrs = strike.getSlot0GlyphImagePtrs(glyphCodes, images, len);
        if (numptrs == len) {
            return;
        }
        for (int i=numptrs; i< len; i++) {
            strike = getStrikeForGlyph(glyphCodes[i]);
            images[i] = strike.getGlyphImagePtr(glyphCodes[i] & SLOTMASK);
        }
    }


    long getGlyphImagePtr(int glyphCode) {
        PhysicalStrike strike = getStrikeForGlyph(glyphCode);
        return strike.getGlyphImagePtr(glyphCode & SLOTMASK);
    }

    void getGlyphImageBounds(int glyphCode, Point2D.Float pt, Rectangle result) {
        PhysicalStrike strike = getStrikeForGlyph(glyphCode);
        strike.getGlyphImageBounds(glyphCode & SLOTMASK, pt, result);
    }

    Point2D.Float getGlyphMetrics(int glyphCode) {
        PhysicalStrike strike = getStrikeForGlyph(glyphCode);
        return strike.getGlyphMetrics(glyphCode & SLOTMASK);
    }

    Point2D.Float getCharMetrics(char ch) {
        return getGlyphMetrics(compFont.getMapper().charToGlyph(ch));
    }

    float getGlyphAdvance(int glyphCode) {
        PhysicalStrike strike = getStrikeForGlyph(glyphCode);
        return strike.getGlyphAdvance(glyphCode & SLOTMASK);
    }

    /* REMIND where to cache?
     * The glyph advance is already cached by physical strikes and that's a lot
     * of the work.
     * Also FontDesignMetrics maintains a latin char advance cache, so don't
     * cache advances here as apps tend to hold onto metrics objects when
     * performance is sensitive to it. Revisit this assumption later.
     */
    float getCodePointAdvance(int cp) {
        return getGlyphAdvance(compFont.getMapper().charToGlyph(cp));
    }

    Rectangle2D.Float getGlyphOutlineBounds(int glyphCode) {
        PhysicalStrike strike = getStrikeForGlyph(glyphCode);
        return strike.getGlyphOutlineBounds(glyphCode & SLOTMASK);
    }

    GeneralPath getGlyphOutline(int glyphCode, float x, float y) {

        PhysicalStrike strike = getStrikeForGlyph(glyphCode);
        GeneralPath path = strike.getGlyphOutline(glyphCode & SLOTMASK, x, y);
        if (path == null) {
            return new GeneralPath();
        } else {
            return path;
        }
    }

    /* The physical font slot for each glyph is encoded in the glyph ID
     * To be as efficient as possible we find a run of glyphs from the
     * same slot and create a temporary array of these glyphs decoded
     * to the slot. The slot font is then queried for the GeneralPath
     * for that run of glyphs. GeneralPaths from each run are appended
     * to create the shape for the whole glyph array.
     */
    GeneralPath getGlyphVectorOutline(int[] glyphs, float x, float y) {
        GeneralPath path = null;
        GeneralPath gp;
        int glyphIndex = 0;
        int[] tmpGlyphs;

        while (glyphIndex < glyphs.length) {
            int start = glyphIndex;
            int slot = glyphs[glyphIndex] >>> 24;
            while (glyphIndex < glyphs.length &&
                   (glyphs[glyphIndex+1] >>> 24) == slot) {
                glyphIndex++;
            }
            int tmpLen = glyphIndex-start+1;
            tmpGlyphs = new int[tmpLen];
            for (int i=0;i<tmpLen;i++) {
                tmpGlyphs[i] = glyphs[i] & SLOTMASK;
            }
            gp = getStrikeForSlot(slot).getGlyphVectorOutline(tmpGlyphs, x, y);
            if (path == null) {
                path = gp;
            } else if (gp != null) {
                path.append(gp, false);
            }
        }
        if (path == null) {
            return new GeneralPath();
        } else {
            return path;
        }
    }
}
