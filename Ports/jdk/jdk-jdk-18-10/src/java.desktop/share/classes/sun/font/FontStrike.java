/*
 * Copyright (c) 2003, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.Rectangle;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Point2D;

public abstract class FontStrike {


    protected FontStrikeDisposer disposer;
    protected FontStrikeDesc desc;
    protected StrikeMetrics strikeMetrics;
    protected boolean algoStyle = false;
    protected float boldness = 1f;
    protected float italic = 0f;
    /*
     * lastLookupTime is updated by Font2D.getStrike and can be used to
     * choose strikes that have not been newly referenced for purging when
     * memory usage gets too high. Active strikes will never be purged
     * because purging is via GC of WeakReferences.
     */
    //protected long lastlookupTime/* = System.currentTimeMillis()*/;

    public abstract int getNumGlyphs();

    abstract StrikeMetrics getFontMetrics();

    abstract void getGlyphImagePtrs(int[] glyphCodes, long[] images,int  len);

    abstract long getGlyphImagePtr(int glyphcode);

    // pt, result in device space
    abstract void getGlyphImageBounds(int glyphcode,
                                      Point2D.Float pt,
                                      Rectangle result);

    abstract Point2D.Float getGlyphMetrics(int glyphcode);

    abstract Point2D.Float getCharMetrics(char ch);

    abstract float getGlyphAdvance(int glyphCode);

    abstract float getCodePointAdvance(int cp);

    abstract Rectangle2D.Float getGlyphOutlineBounds(int glyphCode);

    abstract GeneralPath
        getGlyphOutline(int glyphCode, float x, float y);

    abstract GeneralPath
        getGlyphVectorOutline(int[] glyphs, float x, float y);


}
