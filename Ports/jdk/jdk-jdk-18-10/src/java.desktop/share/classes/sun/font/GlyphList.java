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

package sun.font;

import java.awt.font.GlyphVector;
import java.util.concurrent.atomic.AtomicBoolean;

import sun.java2d.SurfaceData;
import sun.java2d.loops.FontInfo;

/*
 * This class represents a list of actual renderable glyphs.
 * It can be constructed from a number of text sources, representing
 * the various ways in which a programmer can ask a Graphics2D object
 * to render some text.  Once constructed, it provides a way of iterating
 * through the device metrics and graybits of the individual glyphs that
 * need to be rendered to the screen.
 *
 * Note that this class holds pointers to native data which must be
 * disposed.  It is not marked as finalizable since it is intended
 * to be very lightweight and finalization is a comparitively expensive
 * procedure.  The caller must specifically use try{} finally{} to
 * manually ensure that the object is disposed after use, otherwise
 * native data structures might be leaked.
 *
 * Here is a code sample for using this class:
 *
 * public void drawString(String str, FontInfo info, float x, float y) {
 *     GlyphList gl = GlyphList.getInstance();
 *     try {
 *         gl.setFromString(info, str, x, y);
 *         gl.startGlyphIteration();
 *         int numglyphs = gl.getNumGlyphs();
 *         for (int i = 0; i < numglyphs; i++) {
 *             gl.setGlyphIndex(i);
 *             int metrics[] = gl.getMetrics();
 *             byte bits[] = gl.getGrayBits();
 *             int glyphx = metrics[0];
 *             int glyphy = metrics[1];
 *             int glyphw = metrics[2];
 *             int glyphh = metrics[3];
 *             int off = 0;
 *             for (int j = 0; j < glyphh; j++) {
 *                 for (int i = 0; i < glyphw; i++) {
 *                     int dx = glyphx + i;
 *                     int dy = glyphy + j;
 *                     int alpha = bits[off++];
 *                     drawPixel(alpha, dx, dy);
 *                 }
 *             }
 *         }
 *     } finally {
 *         gl.dispose();
 *     }
 * }
 */
public final class GlyphList {
    private static final int MINGRAYLENGTH = 1024;
    private static final int MAXGRAYLENGTH = 8192;
    private static final int DEFAULT_LENGTH = 32;

    int glyphindex;
    int[] metrics;
    byte[] graybits;

    /* A reference to the strike is needed for the case when the GlyphList
     * may be added to a queue for batch processing, (e.g. OpenGL) and we need
     * to be completely certain that the strike is still valid when the glyphs
     * images are later referenced.  This does mean that if such code discards
     * GlyphList and places only the data it contains on the queue, that the
     * strike needs to be part of that data held by a strong reference.
     * In the cases of drawString() and drawChars(), this is a single strike,
     * although it may be a composite strike.  In the case of
     * drawGlyphVector() it may be a single strike, or a list of strikes.
     */
    Object strikelist; // hold multiple strikes during rendering of complex gv

    /* In normal usage, the same GlyphList will get recycled, so
     * it makes sense to allocate arrays that will get reused along with
     * it, rather than generating garbage. Garbage will be generated only
     * in MP envts where multiple threads are executing. Throughput should
     * still be higher in those cases.
     */
    int len = 0;
    int maxLen = 0;
    int maxPosLen = 0;
    int[] glyphData;
    char[] chData;
    long[] images;
    float[] positions;
    float x, y;
    float gposx, gposy;
    boolean usePositions;

    /* lcdRGBOrder is used only by LCD text rendering. Its here because
     * the Graphics may have a different hint value than the one used
     * by a GlyphVector, so it has to be stored here - and is obtained
     * from the right FontInfo. Another approach would have been to have
     * install a separate pipe for that case but that's a lot of extra
     * code when a simple boolean will suffice. The overhead to non-LCD
     * text is a redundant boolean assign per call.
     */
    boolean lcdRGBOrder;

    /*
     * lcdSubPixPos is used only by LCD text rendering. Its here because
     * the Graphics may have a different hint value than the one used
     * by a GlyphVector, so it has to be stored here - and is obtained
     * from the right FontInfo. Its also needed by the code which
     * calculates glyph positions which already needs to access this
     * GlyphList and would otherwise need the FontInfo.
     * This is true only if LCD text and fractional metrics hints
     * are selected on the graphics.
     * When this is true and the glyph positions as determined by the
     * advances are non-integral, it requests adjustment of the positions.
     * Setting this for surfaces which do not support it through accelerated
     * loops may cause a slow-down as software loops are invoked instead.
     */
    boolean lcdSubPixPos;

    /* This scheme creates a singleton GlyphList which is checked out
     * for use. Callers who find its checked out create one that after use
     * is discarded. This means that in a MT-rendering environment,
     * there's no need to synchronise except for that one instance.
     * Fewer threads will then need to synchronise, perhaps helping
     * throughput on a MP system. If for some reason the reusable
     * GlyphList is checked out for a long time (or never returned?) then
     * we would end up always creating new ones. That situation should not
     * occur and if it did, it would just lead to some extra garbage being
     * created.
     */
    private static final GlyphList reusableGL = new GlyphList();
    private static final AtomicBoolean inUse = new AtomicBoolean();

    private ColorGlyphSurfaceData glyphSurfaceData;

    void ensureCapacity(int len) {
      /* Note len must not be -ve! only setFromChars should be capable
       * of passing down a -ve len, and this guards against it.
       */
        if (len < 0) {
          len = 0;
        }
        if (usePositions && len > maxPosLen) {
            positions = new float[len * 2 + 2];
            maxPosLen = len;
        }

        if (maxLen == 0 || len > maxLen) {
            glyphData = new int[len];
            chData = new char[len];
            images = new long[len];
            maxLen = len;
        }
    }

    private GlyphList() {
//         ensureCapacity(DEFAULT_LENGTH);
    }

//     private GlyphList(int arraylen) {
//          ensureCapacity(arraylen);
//     }

    public static GlyphList getInstance() {
        if (inUse.compareAndSet(false, true)) {
            return reusableGL;
        } else {
            return new GlyphList();
        }
    }

    /* In some cases the caller may be able to estimate the size of
     * array needed, and it will usually be long enough. This avoids
     * the unnecessary reallocation that occurs if our default
     * values are too small. This is useful because this object
     * will be discarded so the re-allocation overhead is high.
     */
//     public static GlyphList getInstance(int sz) {
//      if (inUse.compareAndSet(false, true) {
//          return reusableGL;
//      } else {
//          return new GlyphList(sz);
//      }
//     }

    /* GlyphList is in an invalid state until setFrom* method is called.
     * After obtaining a new GlyphList it is the caller's responsibility
     * that one of these methods is executed before handing off the
     * GlyphList
     */

    public boolean setFromString(FontInfo info, String str, float x, float y) {
        this.x = x;
        this.y = y;
        this.strikelist = info.fontStrike;
        this.lcdRGBOrder = info.lcdRGBOrder;
        this.lcdSubPixPos = info.lcdSubPixPos;
        len = str.length();
        ensureCapacity(len);
        str.getChars(0, len, chData, 0);
        return mapChars(info, len);
    }

    public boolean setFromChars(FontInfo info, char[] chars, int off, int alen,
                                float x, float y) {
        this.x = x;
        this.y = y;
        this.strikelist = info.fontStrike;
        this.lcdRGBOrder = info.lcdRGBOrder;
        this.lcdSubPixPos = info.lcdSubPixPos;
        len = alen;
        if (alen < 0) {
            len = 0;
        } else {
            len = alen;
        }
        ensureCapacity(len);
        System.arraycopy(chars, off, chData, 0, len);
        return mapChars(info, len);
    }

    private boolean mapChars(FontInfo info, int len) {
        /* REMIND.Is it worthwhile for the iteration to convert
         * chars to glyph ids to directly map to images?
         */
        if (info.font2D.getMapper().charsToGlyphsNS(len, chData, glyphData)) {
            return false;
        }
        info.fontStrike.getGlyphImagePtrs(glyphData, images, len);
        glyphindex = -1;
        return true;
    }


    public void setFromGlyphVector(FontInfo info, GlyphVector gv,
                                   float x, float y) {
        this.x = x;
        this.y = y;
        this.lcdRGBOrder = info.lcdRGBOrder;
        this.lcdSubPixPos = info.lcdSubPixPos;
        /* A GV may be rendered in different Graphics. It is possible it is
         * used for one case where LCD text is available, and another where
         * it is not. Pass in the "info". to ensure get a suitable one.
         */
        StandardGlyphVector sgv = StandardGlyphVector.getStandardGV(gv, info);
        // call before ensureCapacity :-
        usePositions = sgv.needsPositions(info.devTx);
        len = sgv.getNumGlyphs();
        ensureCapacity(len);
        strikelist = sgv.setupGlyphImages(images,
                                          usePositions ? positions : null,
                                          info.devTx);
        glyphindex = -1;
    }

    public void startGlyphIteration() {
        if (glyphindex >= 0) {
            throw new InternalError("glyph iteration restarted");
        }
        if (metrics == null) {
            metrics = new int[5];
        }
        /* gposx and gposy are used to accumulate the advance.
         * Add 0.5f for consistent rounding to pixel position. */
        gposx = x + 0.5f;
        gposy = y + 0.5f;
    }

    /*
     * Must be called after 'startGlyphIteration'.
     * Returns overall bounds for glyphs starting from the next glyph
     * in iteration till the glyph with specified index.
     * The underlying storage for bounds is shared with metrics,
     * so this method (and the array it returns) shouldn't be used between
     * 'setGlyphIndex' call and matching 'getMetrics' call.
     */
    public int[] getBounds(int endGlyphIndex) {
        fillBounds(metrics, endGlyphIndex);
        return metrics;
    }

    /* This method now assumes "state", so must be called 0->len
     * The metrics it returns are accumulated on the fly
     * So it could be renamed "nextGlyph()".
     * Note that a laid out GlyphVector which has assigned glyph positions
     * doesn't have this stricture..
     */
    public void setGlyphIndex(int i) {
        glyphindex = i;
        if (images[i] == 0L) {
           metrics[0] = (int)gposx;
           metrics[1] = (int)gposy;
           metrics[2] = 0;
           metrics[3] = 0;
           metrics[4] = 0;
           return;
        }
        float gx =
            StrikeCache.unsafe.getFloat(images[i]+StrikeCache.topLeftXOffset);
        float gy =
            StrikeCache.unsafe.getFloat(images[i]+StrikeCache.topLeftYOffset);

        if (usePositions) {
            metrics[0] = (int)Math.floor(positions[(i<<1)]   + gposx + gx);
            metrics[1] = (int)Math.floor(positions[(i<<1)+1] + gposy + gy);
        } else {
            metrics[0] = (int)Math.floor(gposx + gx);
            metrics[1] = (int)Math.floor(gposy + gy);
            /* gposx and gposy are used to accumulate the advance */
            gposx += StrikeCache.unsafe.getFloat
                (images[i]+StrikeCache.xAdvanceOffset);
            gposy += StrikeCache.unsafe.getFloat
                (images[i]+StrikeCache.yAdvanceOffset);
        }
        metrics[2] =
            StrikeCache.unsafe.getChar(images[i]+StrikeCache.widthOffset);
        metrics[3] =
            StrikeCache.unsafe.getChar(images[i]+StrikeCache.heightOffset);
        metrics[4] =
            StrikeCache.unsafe.getChar(images[i]+StrikeCache.rowBytesOffset);
    }

    public int[] getMetrics() {
        return metrics;
    }

    public byte[] getGrayBits() {
        int len = metrics[4] * metrics[3];
        if (graybits == null) {
            graybits = new byte[Math.max(len, MINGRAYLENGTH)];
        } else {
            if (len > graybits.length) {
                graybits = new byte[len];
            }
        }
        if (images[glyphindex] == 0L) {
            return graybits;
        }
        long pixelDataAddress =
            StrikeCache.unsafe.getAddress(images[glyphindex] +
                                          StrikeCache.pixelDataOffset);

        if (pixelDataAddress == 0L) {
            return graybits;
        }
        /* unsafe is supposed to be fast, but I doubt if this loop can beat
         * a native call which does a getPrimitiveArrayCritical and a
         * memcpy for the typical amount of image data (30-150 bytes)
         * Consider a native method if there is a performance problem (which
         * I haven't seen so far).
         */
        for (int i=0; i<len; i++) {
            graybits[i] = StrikeCache.unsafe.getByte(pixelDataAddress+i);
        }
        return graybits;
    }

    public long[] getImages() {
        return images;
    }

    public boolean usePositions() {
        return usePositions;
    }

    public float[] getPositions() {
        return positions;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public Object getStrike() {
        return strikelist;
    }

    public boolean isSubPixPos() {
        return lcdSubPixPos;
    }

    public boolean isRGBOrder() {
        return lcdRGBOrder;
    }

    /* There's a reference equality test overhead here, but it allows us
     * to avoid synchronizing for GL's that will just be GC'd. This
     * helps MP throughput.
     */
    public void dispose() {
        if (this == reusableGL) {
            if (graybits != null && graybits.length > MAXGRAYLENGTH) {
                graybits = null;
            }
            usePositions = false;
            strikelist = null; // remove reference to the strike list
            inUse.set(false);
        }
    }

    /* The value here is for use by the rendering engine as it reflects
     * the number of glyphs in the array to be blitted. Surrogates pairs
     * may have two slots (the second of these being a dummy entry of the
     * invisible glyph), whereas an application client would expect only
     * one glyph. In other words don't propagate this value up to client code.
     *
     * {dlf} an application client should have _no_ expectations about the
     * number of glyphs per char.  This ultimately depends on the font
     * technology and layout process used, which in general clients will
     * know nothing about.
     */
    public int getNumGlyphs() {
        return len;
    }

    /* We re-do all this work as we iterate through the glyphs
     * but it seems unavoidable without re-working the Java TextRenderers.
     */
    private void fillBounds(int[] bounds, int endGlyphIndex) {
        /* Faster to access local variables in the for loop? */
        int xOffset = StrikeCache.topLeftXOffset;
        int yOffset = StrikeCache.topLeftYOffset;
        int wOffset = StrikeCache.widthOffset;
        int hOffset = StrikeCache.heightOffset;
        int xAdvOffset = StrikeCache.xAdvanceOffset;
        int yAdvOffset = StrikeCache.yAdvanceOffset;

        int startGlyphIndex = glyphindex + 1;
        if (startGlyphIndex >= endGlyphIndex) {
            bounds[0] = bounds[1] = bounds[2] = bounds[3] = 0;
            return;
        }
        float bx0, by0, bx1, by1;
        bx0 = by0 = Float.POSITIVE_INFINITY;
        bx1 = by1 = Float.NEGATIVE_INFINITY;

        int posIndex = startGlyphIndex<<1;
        float glx = gposx;
        float gly = gposy;
        char gw, gh;
        float gx, gy, gx0, gy0, gx1, gy1;
        for (int i=startGlyphIndex; i<endGlyphIndex; i++) {
            if (images[i] == 0L) {
                continue;
            }
            gx = StrikeCache.unsafe.getFloat(images[i]+xOffset);
            gy = StrikeCache.unsafe.getFloat(images[i]+yOffset);
            gw = StrikeCache.unsafe.getChar(images[i]+wOffset);
            gh = StrikeCache.unsafe.getChar(images[i]+hOffset);

            if (usePositions) {
                gx0 = positions[posIndex++] + gx + glx;
                gy0 = positions[posIndex++] + gy + gly;
            } else {
                gx0 = glx + gx;
                gy0 = gly + gy;
                glx += StrikeCache.unsafe.getFloat(images[i]+xAdvOffset);
                gly += StrikeCache.unsafe.getFloat(images[i]+yAdvOffset);
            }
            gx1 = gx0 + gw;
            gy1 = gy0 + gh;
            if (bx0 > gx0) bx0 = gx0;
            if (by0 > gy0) by0 = gy0;
            if (bx1 < gx1) bx1 = gx1;
            if (by1 < gy1) by1 = gy1;
        }
        /* floor is safe and correct because all glyph widths, heights
         * and offsets are integers
         */
        bounds[0] = (int)Math.floor(bx0);
        bounds[1] = (int)Math.floor(by0);
        bounds[2] = (int)Math.floor(bx1);
        bounds[3] = (int)Math.floor(by1);
    }

    public static boolean canContainColorGlyphs() {
        return FontUtilities.isMacOSX;
    }

    public boolean isColorGlyph(int glyphIndex) {
        int width = StrikeCache.unsafe.getChar(images[glyphIndex] +
                                               StrikeCache.widthOffset);
        int rowBytes = StrikeCache.unsafe.getChar(images[glyphIndex] +
                                                  StrikeCache.rowBytesOffset);
        return rowBytes == width * 4;
    }

    public SurfaceData getColorGlyphData() {
        if (glyphSurfaceData == null) {
            glyphSurfaceData = new ColorGlyphSurfaceData();
        }
        glyphSurfaceData.setCurrentGlyph(images[glyphindex]);
        return glyphSurfaceData;
    }
}
