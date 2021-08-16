/*
 * Copyright (c) 2011, 2018, Oracle and/or its affiliates. All rights reserved.
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

package sun.lwawt.macosx;


import java.awt.*;
import java.awt.font.*;

import sun.awt.*;
import sun.font.*;
import sun.java2d.*;
import sun.java2d.loops.*;
import sun.java2d.pipe.*;

public class CTextPipe implements TextPipe {
    public native void doDrawString(SurfaceData sData, long nativeStrikePtr, String s, double x, double y);
    public native void doDrawGlyphs(SurfaceData sData, long nativeStrikePtr, GlyphVector gV, float x, float y);
    public native void doUnicodes(SurfaceData sData, long nativeStrikePtr, char[] unicodes, int offset, int length, float x, float y);
    public native void doOneUnicode(SurfaceData sData, long nativeStrikePtr, char aUnicode, float x, float y);

    long getNativeStrikePtr(final SunGraphics2D sg2d) {
        final FontStrike fontStrike = sg2d.getFontInfo().fontStrike;
        if (!(fontStrike instanceof CStrike)) return 0;
        return ((CStrike)fontStrike).getNativeStrikePtr();
    }

    void drawGlyphVectorAsShape(final SunGraphics2D sg2d, final GlyphVector gv, final float x, final float y) {
        final int length = gv.getNumGlyphs();
        for (int i = 0; i < length; i++) {
            final Shape glyph = gv.getGlyphOutline(i, x, y);
            sg2d.fill(glyph);
        }
    }

    void drawTextAsShape(final SunGraphics2D sg2d, final String s, final double x, final double y) {
        final Object oldAliasingHint = sg2d.getRenderingHint(SunHints.KEY_ANTIALIASING);
        final FontRenderContext frc = sg2d.getFontRenderContext();
        sg2d.setRenderingHint(SunHints.KEY_ANTIALIASING, (frc.isAntiAliased() ? SunHints.VALUE_ANTIALIAS_ON : SunHints.VALUE_ANTIALIAS_OFF));

        final Font font = sg2d.getFont();
        final GlyphVector gv = font.createGlyphVector(frc, s);
        final int length = gv.getNumGlyphs();
        for (int i = 0; i < length; i++) {
            final Shape glyph = gv.getGlyphOutline(i, (float)x, (float)y);
            sg2d.fill(glyph);
        }

        sg2d.setRenderingHint(SunHints.KEY_ANTIALIASING, oldAliasingHint);
    }

    public void drawString(final SunGraphics2D sg2d, final String s, final double x, final double y) {
        final long nativeStrikePtr = getNativeStrikePtr(sg2d);
        if (OSXSurfaceData.IsSimpleColor(sg2d.paint) && nativeStrikePtr != 0) {
            final OSXSurfaceData surfaceData = (OSXSurfaceData)sg2d.getSurfaceData();
            surfaceData.drawString(this, sg2d, nativeStrikePtr, s, x, y);
        } else {
            drawTextAsShape(sg2d, s, x, y);
        }
    }

    public void drawGlyphVector(final SunGraphics2D sg2d, final GlyphVector gV, final float x, final float y) {
        final Font prevFont = sg2d.getFont();
        sg2d.setFont(gV.getFont());

        final long nativeStrikePtr = getNativeStrikePtr(sg2d);
        if (OSXSurfaceData.IsSimpleColor(sg2d.paint) && nativeStrikePtr != 0) {
            final OSXSurfaceData surfaceData = (OSXSurfaceData)sg2d.getSurfaceData();
            surfaceData.drawGlyphs(this, sg2d, nativeStrikePtr, gV, x, y);
        } else {
            drawGlyphVectorAsShape(sg2d, gV, x, y);
        }
        sg2d.setFont(prevFont);
    }

    public void drawChars(final SunGraphics2D sg2d, final char[] data, final int offset, final int length, final int x, final int y) {
        final long nativeStrikePtr = getNativeStrikePtr(sg2d);
        if (OSXSurfaceData.IsSimpleColor(sg2d.paint) && nativeStrikePtr != 0) {
            final OSXSurfaceData surfaceData = (OSXSurfaceData)sg2d.getSurfaceData();
            surfaceData.drawUnicodes(this, sg2d, nativeStrikePtr, data, offset, length, x, y);
        } else {
            drawTextAsShape(sg2d, new String(data, offset, length), x, y);
        }
    }

    public CTextPipe traceWrap() {
        return new Tracer();
    }

    public static class Tracer extends CTextPipe {
        void doDrawString(final SurfaceData sData, final long nativeStrikePtr, final String s, final float x, final float y) {
            GraphicsPrimitive.tracePrimitive("QuartzDrawString");
            super.doDrawString(sData, nativeStrikePtr, s, x, y);
        }

        public void doDrawGlyphs(final SurfaceData sData, final long nativeStrikePtr, final GlyphVector gV, final float x, final float y) {
            GraphicsPrimitive.tracePrimitive("QuartzDrawGlyphs");
            super.doDrawGlyphs(sData, nativeStrikePtr, gV, x, y);
        }

        public void doUnicodes(final SurfaceData sData, final long nativeStrikePtr, final char[] unicodes, final int offset, final int length, final float x, final float y) {
            GraphicsPrimitive.tracePrimitive("QuartzDrawUnicodes");
            super.doUnicodes(sData, nativeStrikePtr, unicodes, offset, length, x, y);
        }

        public void doOneUnicode(final SurfaceData sData, final long nativeStrikePtr, final char aUnicode, final float x, final float y) {
            GraphicsPrimitive.tracePrimitive("QuartzDrawUnicode");
            super.doOneUnicode(sData, nativeStrikePtr, aUnicode, x, y);
        }
    }
}
