/*
 * Copyright (c) 2003, 2012, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;

/* Returned instead of a NativeStrike.
 * It can intercept any request it wants, but mostly
 * passes them on to its delegate strike. It is important that
 * it override all the inherited FontStrike methods to delegate them
 * appropriately.
 */

class DelegateStrike extends NativeStrike {

    private FontStrike delegateStrike;

    DelegateStrike(NativeFont nativeFont, FontStrikeDesc desc,
                   FontStrike delegate) {
        super(nativeFont, desc);
        this.delegateStrike = delegate;
    }

    /* We want the native font to be responsible for reporting the
     * font metrics, even if it often delegates to another font.
     * The code here isn't yet implementing exactly that. If the glyph
     * transform was something native couldn't handle, there's no native
     * context from which to obtain metrics. Need to revise this to obtain
     * the metrics and transform them. But currently in such a case it
     * gets the metrics from a different font - its glyph delegate font.
     */
   StrikeMetrics getFontMetrics() {
       if (strikeMetrics == null) {
           if (pScalerContext != 0) {
               strikeMetrics = super.getFontMetrics();
           }
            if (strikeMetrics == null) {
                strikeMetrics = delegateStrike.getFontMetrics();
            }
        }
        return strikeMetrics;
    }

    void getGlyphImagePtrs(int[] glyphCodes, long[] images,int  len) {
        delegateStrike.getGlyphImagePtrs(glyphCodes, images, len);
    }

    long getGlyphImagePtr(int glyphCode) {
        return delegateStrike.getGlyphImagePtr(glyphCode);
    }

    void getGlyphImageBounds(int glyphCode,
                             Point2D.Float pt, Rectangle result) {
        delegateStrike.getGlyphImageBounds(glyphCode, pt, result);
    }

    Point2D.Float getGlyphMetrics(int glyphCode) {
        return delegateStrike.getGlyphMetrics(glyphCode);
    }

    float getGlyphAdvance(int glyphCode) {
        return delegateStrike.getGlyphAdvance(glyphCode);
    }

     Point2D.Float getCharMetrics(char ch) {
        return delegateStrike.getCharMetrics(ch);
    }

    float getCodePointAdvance(int cp) {
        if (cp < 0 || cp >= 0x10000) {
            cp = 0xffff;
        }
        return delegateStrike.getGlyphAdvance(cp);
    }

    Rectangle2D.Float getGlyphOutlineBounds(int glyphCode) {
        return delegateStrike.getGlyphOutlineBounds(glyphCode);
    }

    GeneralPath getGlyphOutline(int glyphCode, float x, float y) {
        return delegateStrike.getGlyphOutline(glyphCode, x, y);
    }

    GeneralPath getGlyphVectorOutline(int[] glyphs, float x, float y) {
        return delegateStrike.getGlyphVectorOutline(glyphs, x, y);
    }

}
