/*
 * Copyright (c) 2007, 2014, Oracle and/or its affiliates. All rights reserved.
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
import java.awt.geom.Rectangle2D;
import java.lang.ref.WeakReference;

/* This is Freetype based implementation of FontScaler.
 *
 * Note that in case of runtime error it is expected that
 * native code will release all native resources and
 * call invalidateScaler() (that will throw FontScalerException).
 *
 * Note that callee is responsible for releasing native scaler context.
 */
class FreetypeFontScaler extends FontScaler {
    /* constants aligned with native code */
    private static final int TRUETYPE_FONT = 1;
    private static final int TYPE1_FONT = 2;

    static {
        /* At the moment fontmanager library depends on freetype library
           and therefore no need to load it explicitly here */
        FontManagerNativeLibrary.load();
        initIDs(FreetypeFontScaler.class);
    }

    private static native void initIDs(Class<?> FFS);

    private void invalidateScaler() throws FontScalerException {
        nativeScaler = 0;
        font = null;
        throw new FontScalerException();
    }

    public FreetypeFontScaler(Font2D font, int indexInCollection,
                     boolean supportsCJK, int filesize) {
        int fonttype = TRUETYPE_FONT;
        if (font instanceof Type1Font) {
            fonttype = TYPE1_FONT;
        }
        nativeScaler = initNativeScaler(font,
                                        fonttype,
                                        indexInCollection,
                                        supportsCJK,
                                        filesize);
        this.font = new WeakReference<>(font);
    }

    synchronized StrikeMetrics getFontMetrics(long pScalerContext)
                     throws FontScalerException {
        if (nativeScaler != 0L) {
                return getFontMetricsNative(font.get(),
                                            pScalerContext,
                                            nativeScaler);
        }
        return FontScaler.getNullScaler().getFontMetrics(0L);
    }

    synchronized float getGlyphAdvance(long pScalerContext, int glyphCode)
                     throws FontScalerException {
        if (nativeScaler != 0L) {
            return getGlyphAdvanceNative(font.get(),
                                         pScalerContext,
                                         nativeScaler,
                                         glyphCode);
        }
        return FontScaler.getNullScaler().
            getGlyphAdvance(0L, glyphCode);
    }

    synchronized void getGlyphMetrics(long pScalerContext,
                     int glyphCode, Point2D.Float metrics)
                     throws FontScalerException {
        if (nativeScaler != 0L) {
            getGlyphMetricsNative(font.get(),
                                  pScalerContext,
                                  nativeScaler,
                                  glyphCode,
                                  metrics);
            return;
        }
        FontScaler.getNullScaler().
            getGlyphMetrics(0L, glyphCode, metrics);
    }

    synchronized long getGlyphImage(long pScalerContext, int glyphCode)
                     throws FontScalerException {
        if (nativeScaler != 0L) {
            return getGlyphImageNative(font.get(),
                                       pScalerContext,
                                       nativeScaler,
                                       glyphCode);
        }
        return FontScaler.getNullScaler().
            getGlyphImage(0L, glyphCode);
    }

    synchronized Rectangle2D.Float getGlyphOutlineBounds(
                     long pScalerContext, int glyphCode)
                     throws FontScalerException {
        if (nativeScaler != 0L) {
            return getGlyphOutlineBoundsNative(font.get(),
                                               pScalerContext,
                                               nativeScaler,
                                               glyphCode);
        }
        return FontScaler.getNullScaler().
            getGlyphOutlineBounds(0L,glyphCode);
    }

    synchronized GeneralPath getGlyphOutline(
                     long pScalerContext, int glyphCode, float x, float y)
                     throws FontScalerException {
        if (nativeScaler != 0L) {
            return getGlyphOutlineNative(font.get(),
                                         pScalerContext,
                                         nativeScaler,
                                         glyphCode,
                                         x, y);
        }
        return FontScaler.getNullScaler().
            getGlyphOutline(0L, glyphCode, x,y);
    }

    synchronized GeneralPath getGlyphVectorOutline(
                     long pScalerContext, int[] glyphs, int numGlyphs,
                     float x, float y) throws FontScalerException {
        if (nativeScaler != 0L) {
            return getGlyphVectorOutlineNative(font.get(),
                                               pScalerContext,
                                               nativeScaler,
                                               glyphs,
                                               numGlyphs,
                                               x, y);
        }
        return FontScaler
            .getNullScaler().getGlyphVectorOutline(0L, glyphs, numGlyphs, x, y);
    }

    /* This method should not be called directly, in case
     * it is being invoked from a thread with a native context.
     */
    public synchronized void dispose() {
        if (nativeScaler != 0L) {
            disposeNativeScaler(font.get(), nativeScaler);
            nativeScaler = 0L;
        }
    }

    public synchronized void disposeScaler() {
        if (nativeScaler != 0L) {
           /*
            * The current thread may be calling this method from the context
            * of a JNI up-call. It will hold the native lock from the
            * original down-call so can directly enter dispose and free
            * the resources. So we need to schedule the disposal to happen
            * only once we've returned from native. So by running the dispose
            * on another thread which does nothing except that disposal we
            * are sure that this is safe.
            */
            new Thread(null, () -> dispose(), "free scaler", 0, false).start();
        }
    }

    synchronized int getNumGlyphs() throws FontScalerException {
        if (nativeScaler != 0L) {
            return getNumGlyphsNative(nativeScaler);
        }
        return FontScaler.getNullScaler().getNumGlyphs();
    }

    synchronized int getMissingGlyphCode()  throws FontScalerException {
        if (nativeScaler != 0L) {
            return getMissingGlyphCodeNative(nativeScaler);
        }
        return FontScaler.getNullScaler().getMissingGlyphCode();
    }

    synchronized int getGlyphCode(char charCode) throws FontScalerException {
        if (nativeScaler != 0L) {
            return getGlyphCodeNative(font.get(), nativeScaler, charCode);
        }
        return FontScaler.getNullScaler().getGlyphCode(charCode);
    }

    synchronized Point2D.Float getGlyphPoint(long pScalerContext,
                                       int glyphCode, int ptNumber)
                               throws FontScalerException {
        if (nativeScaler != 0L) {
            return getGlyphPointNative(font.get(), pScalerContext,
                                      nativeScaler, glyphCode, ptNumber);
        }
        return FontScaler.getNullScaler().getGlyphPoint(
                   pScalerContext, glyphCode,  ptNumber);
    }

    synchronized long getUnitsPerEm() {
        return getUnitsPerEMNative(nativeScaler);
    }

    synchronized long createScalerContext(double[] matrix,
            int aa, int fm, float boldness, float italic) {
        if (nativeScaler != 0L) {
            return createScalerContextNative(nativeScaler, matrix,
                                             aa, fm, boldness, italic);
        }
        return NullFontScaler.getNullScalerContext();
    }

    //Note: native methods can throw RuntimeException if processing fails
    private native long initNativeScaler(Font2D font, int type,
            int indexInCollection, boolean supportsCJK, int filesize);
    private native StrikeMetrics getFontMetricsNative(Font2D font,
            long pScalerContext, long pScaler);
    private native float getGlyphAdvanceNative(Font2D font,
            long pScalerContext, long pScaler, int glyphCode);
    private native void getGlyphMetricsNative(Font2D font,
            long pScalerContext, long pScaler,
            int glyphCode, Point2D.Float metrics);
    private native long getGlyphImageNative(Font2D font,
            long pScalerContext, long pScaler, int glyphCode);
    private native Rectangle2D.Float getGlyphOutlineBoundsNative(Font2D font,
            long pScalerContext, long pScaler, int glyphCode);
    private native GeneralPath getGlyphOutlineNative(Font2D font,
            long pScalerContext, long pScaler,
            int glyphCode, float x, float y);
    private native GeneralPath getGlyphVectorOutlineNative(Font2D font,
            long pScalerContext, long pScaler,
            int[] glyphs, int numGlyphs, float x, float y);
    private native Point2D.Float getGlyphPointNative(Font2D font,
            long pScalerContext, long pScaler, int glyphCode, int ptNumber);

    private native void disposeNativeScaler(Font2D font2D, long pScaler);

    private native int getGlyphCodeNative(Font2D font, long pScaler, char charCode);
    private native int getNumGlyphsNative(long pScaler);
    private native int getMissingGlyphCodeNative(long pScaler);

    private native long getUnitsPerEMNative(long pScaler);

    private native long createScalerContextNative(long pScaler, double[] matrix,
            int aa, int fm, float boldness, float italic);

    /* Freetype scaler context does not contain any pointers that
       has to be invalidated if native scaler is bad */
    void invalidateScalerContext(long pScalerContext) {}
}
