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

import java.awt.FontFormatException;
import java.awt.font.FontRenderContext;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/*
 * This class should never be invoked on the windows implementation
 * So the constructor throws a FontFormatException, which is caught
 * and the font is ignored.
 */

public class NativeFont extends PhysicalFont {

    /**
     * Verifies native font is accessible.
     * @throws FontFormatException if the font can't be located.
     */
    public NativeFont(String platName, boolean isBitmapDelegate)
        throws FontFormatException {

        throw new FontFormatException("NativeFont not used on Windows");
    }

    static boolean hasExternalBitmaps(String platName) {
        return false;
    }

    public CharToGlyphMapper getMapper() {
        return null;
    }

    PhysicalFont getDelegateFont() {
        return null;
    }

    FontStrike createStrike(FontStrikeDesc desc) {
        return null;
    }

    public Rectangle2D getMaxCharBounds(FontRenderContext frc) {
        return null;
    }

    StrikeMetrics getFontMetrics(long pScalerContext) {
        return null;
    }

    public GeneralPath getGlyphOutline(long pScalerContext,
                                       int glyphCode,
                                       float x, float y) {
        return null;
    }

    public  GeneralPath getGlyphVectorOutline(long pScalerContext,
                                              int[] glyphs, int numGlyphs,
                                              float x, float y) {
        return null;
    }


    long getGlyphImage(long pScalerContext, int glyphCode) {
        return 0L;
    }


    void getGlyphMetrics(long pScalerContext, int glyphCode,
                         Point2D.Float metrics) {
    }


    float getGlyphAdvance(long pScalerContext, int glyphCode) {
        return 0f;
    }

    Rectangle2D.Float getGlyphOutlineBounds(long pScalerContext,
                                            int glyphCode) {
        return new Rectangle2D.Float(0f, 0f, 0f, 0f);
    }
}
