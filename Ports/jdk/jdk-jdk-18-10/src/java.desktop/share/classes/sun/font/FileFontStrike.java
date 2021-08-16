/*
 * Copyright (c) 2003, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.concurrent.ConcurrentHashMap;
import static sun.awt.SunHints.*;


public class FileFontStrike extends PhysicalStrike {

    /* fffe and ffff are values we specially interpret as meaning
     * invisible glyphs.
     */
    static final int INVISIBLE_GLYPHS = 0x0fffe;

    private FileFont fileFont;

    /* REMIND: replace this scheme with one that installs a cache
     * instance of the appropriate type. It will require changes in
     * FontStrikeDisposer and NativeStrike etc.
     */
    private static final int UNINITIALISED = 0;
    private static final int INTARRAY      = 1;
    private static final int LONGARRAY     = 2;
    private static final int SEGINTARRAY   = 3;
    private static final int SEGLONGARRAY  = 4;

    private volatile int glyphCacheFormat = UNINITIALISED;

    /* segmented arrays are blocks of 32 */
    private static final int SEGSHIFT = 5;
    private static final int SEGSIZE  = 1 << SEGSHIFT;

    private boolean segmentedCache;
    private int[][] segIntGlyphImages;
    private long[][] segLongGlyphImages;

    /* The "metrics" information requested by clients is usually nothing
     * more than the horizontal advance of the character.
     * In most cases this advance and other metrics information is stored
     * in the glyph image cache.
     * But in some cases we do not automatically retrieve the glyph
     * image when the advance is requested. In those cases we want to
     * cache the advances since this has been shown to be important for
     * performance.
     * The segmented cache is used in cases when the single array
     * would be too large.
     */
    private float[] horizontalAdvances;
    private float[][] segHorizontalAdvances;

    /* Outline bounds are used when printing and when drawing outlines
     * to the screen. On balance the relative rarity of these cases
     * and the fact that getting this requires generating a path at
     * the scaler level means that its probably OK to store these
     * in a Java-level hashmap as the trade-off between time and space.
     * Later can revisit whether to cache these at all, or elsewhere.
     * Should also profile whether subsequent to getting the bounds, the
     * outline itself is also requested. The 1.4 implementation doesn't
     * cache outlines so you could generate the path twice - once to get
     * the bounds and again to return the outline to the client.
     * If the two uses are coincident then also look into caching outlines.
     * One simple optimisation is that we could store the last single
     * outline retrieved. This assumes that bounds then outline will always
     * be retrieved for a glyph rather than retrieving bounds for all glyphs
     * then outlines for all glyphs.
     */
    ConcurrentHashMap<Integer, Rectangle2D.Float> boundsMap;
    SoftReference<ConcurrentHashMap<Integer, Point2D.Float>>
        glyphMetricsMapRef;

    AffineTransform invertDevTx;

    boolean useNatives;
    NativeStrike[] nativeStrikes;

    /* Used only for communication to native layer */
    private int intPtSize;

    /* Perform global initialisation needed for Windows native rasterizer */
    private static native boolean initNative();
    private static boolean isXPorLater = false;
    static {
        if (FontUtilities.isWindows && !FontUtilities.useJDKScaler &&
            !GraphicsEnvironment.isHeadless()) {
            isXPorLater = initNative();
        }
    }

    FileFontStrike(FileFont fileFont, FontStrikeDesc desc) {
        super(fileFont, desc);
        this.fileFont = fileFont;

        if (desc.style != fileFont.style) {
          /* If using algorithmic styling, the base values are
           * boldness = 1.0, italic = 0.0. The superclass constructor
           * initialises these.
           */
            if ((desc.style & Font.ITALIC) == Font.ITALIC &&
                (fileFont.style & Font.ITALIC) == 0) {
                algoStyle = true;
                italic = 0.7f;
            }
            if ((desc.style & Font.BOLD) == Font.BOLD &&
                ((fileFont.style & Font.BOLD) == 0)) {
                algoStyle = true;
                boldness = 1.33f;
            }
        }
        double[] matrix = new double[4];
        AffineTransform at = desc.glyphTx;
        at.getMatrix(matrix);
        if (!desc.devTx.isIdentity() &&
            desc.devTx.getType() != AffineTransform.TYPE_TRANSLATION) {
            try {
                invertDevTx = desc.devTx.createInverse();
            } catch (NoninvertibleTransformException e) {
            }
        }

        /* If any of the values is NaN then substitute the null scaler context.
         * This will return null images, zero advance, and empty outlines
         * as no rendering need take place in this case.
         * We pass in the null scaler as the singleton null context
         * requires it. However
         */
        if (Double.isNaN(matrix[0]) || Double.isNaN(matrix[1]) ||
            Double.isNaN(matrix[2]) || Double.isNaN(matrix[3]) ||
            fileFont.getScaler() == null) {
            pScalerContext = NullFontScaler.getNullScalerContext();
        } else {
            pScalerContext = fileFont.getScaler().createScalerContext(matrix,
                                    desc.aaHint, desc.fmHint,
                                    boldness, italic);
        }

        mapper = fileFont.getMapper();
        int numGlyphs = mapper.getNumGlyphs();

        /* Always segment for fonts with > 256 glyphs, but also for smaller
         * fonts with non-typical sizes and transforms.
         * Segmenting for all non-typical pt sizes helps to minimize memory
         * usage when very many distinct strikes are created.
         * The size range of 0->5 and 37->INF for segmenting is arbitrary
         * but the intention is that typical GUI integer point sizes (6->36)
         * should not segment unless there's another reason to do so.
         */
        float ptSize = (float)matrix[3]; // interpreted only when meaningful.
        int iSize = intPtSize = (int)ptSize;
        boolean isSimpleTx = (at.getType() & complexTX) == 0;
        segmentedCache =
            (numGlyphs > SEGSIZE << 3) ||
            ((numGlyphs > SEGSIZE << 1) &&
             (!isSimpleTx || ptSize != iSize || iSize < 6 || iSize > 36));

        /* This can only happen if we failed to allocate memory for context.
         * NB: in such case we may still have some memory in java heap
         *     but subsequent attempt to allocate null scaler context
         *     may fail too (cause it is allocate in the native heap).
         *     It is not clear how to make this more robust but on the
         *     other hand getting NULL here seems to be extremely unlikely.
         */
        if (pScalerContext == 0L) {
            /* REMIND: when the code is updated to install cache objects
             * rather than using a switch this will be more efficient.
             */
            this.disposer = new FontStrikeDisposer(fileFont, desc);
            initGlyphCache();
            pScalerContext = NullFontScaler.getNullScalerContext();
            SunFontManager.getInstance().deRegisterBadFont(fileFont);
            return;
        }
        /* First, see if native code should be used to create the glyph.
         * GDI will return the integer metrics, not fractional metrics, which
         * may be requested for this strike, so we would require here that :
         * desc.fmHint != INTVAL_FRACTIONALMETRICS_ON
         * except that the advance returned by GDI is always overwritten by
         * the JDK rasteriser supplied one (see getGlyphImageFromWindows()).
         */
        if (FontUtilities.isWindows && isXPorLater &&
            !FontUtilities.useJDKScaler &&
            !GraphicsEnvironment.isHeadless() &&
            !fileFont.useJavaRasterizer &&
            (desc.aaHint == INTVAL_TEXT_ANTIALIAS_LCD_HRGB ||
             desc.aaHint == INTVAL_TEXT_ANTIALIAS_LCD_HBGR) &&
            (matrix[1] == 0.0 && matrix[2] == 0.0 &&
             matrix[0] == matrix[3] &&
             matrix[0] >= 3.0 && matrix[0] <= 100.0) &&
            !((TrueTypeFont)fileFont).useEmbeddedBitmapsForSize(intPtSize)) {
            useNatives = true;
        }
        if (FontUtilities.isLogging() && FontUtilities.isWindows) {
            FontUtilities.logInfo("Strike for " + fileFont + " at size = " + intPtSize +
                 " use natives = " + useNatives +
                 " useJavaRasteriser = " + fileFont.useJavaRasterizer +
                 " AAHint = " + desc.aaHint +
                 " Has Embedded bitmaps = " +
                 ((TrueTypeFont)fileFont).
                 useEmbeddedBitmapsForSize(intPtSize));
        }
        this.disposer = new FontStrikeDisposer(fileFont, desc, pScalerContext);

        /* Always get the image and the advance together for smaller sizes
         * that are likely to be important to rendering performance.
         * The pixel size of 48.0 can be thought of as
         * "maximumSizeForGetImageWithAdvance".
         * This should be no greater than OutlineTextRender.THRESHOLD.
         */
        double maxSz = 48.0;
        getImageWithAdvance =
            Math.abs(at.getScaleX()) <= maxSz &&
            Math.abs(at.getScaleY()) <= maxSz &&
            Math.abs(at.getShearX()) <= maxSz &&
            Math.abs(at.getShearY()) <= maxSz;

        /* Some applications request advance frequently during layout.
         * If we are not getting and caching the image with the advance,
         * there is a potentially significant performance penalty if the
         * advance is repeatedly requested before requesting the image.
         * We should at least cache the horizontal advance.
         * REMIND: could use info in the font, eg hmtx, to retrieve some
         * advances. But still want to cache it here.
         */

        if (!getImageWithAdvance) {
            if (!segmentedCache) {
                horizontalAdvances = new float[numGlyphs];
                /* use max float as uninitialised advance */
                for (int i=0; i<numGlyphs; i++) {
                    horizontalAdvances[i] = Float.MAX_VALUE;
                }
            } else {
                int numSegments = (numGlyphs + SEGSIZE-1)/SEGSIZE;
                segHorizontalAdvances = new float[numSegments][];
            }
        }
    }

    /* A number of methods are delegated by the strike to the scaler
     * context which is a shared resource on a physical font.
     */

    public int getNumGlyphs() {
        return fileFont.getNumGlyphs();
    }

    long getGlyphImageFromNative(int glyphCode) {
        if (FontUtilities.isWindows) {
            return getGlyphImageFromWindows(glyphCode);
        } else {
            return getGlyphImageFromX11(glyphCode);
        }
    }

    /* There's no global state conflicts, so this method is not
     * presently synchronized.
     */
    private native long _getGlyphImageFromWindows(String family,
                                                  int style,
                                                  int size,
                                                  int glyphCode,
                                                  boolean fracMetrics,
                                                  int fontDataSize);

    long getGlyphImageFromWindows(int glyphCode) {
        String family = fileFont.getFamilyName(null);
        int style = desc.style & Font.BOLD | desc.style & Font.ITALIC
            | fileFont.getStyle();
        int size = intPtSize;
        long ptr = _getGlyphImageFromWindows
            (family, style, size, glyphCode,
             desc.fmHint == INTVAL_FRACTIONALMETRICS_ON,
             ((TrueTypeFont)fileFont).fontDataSize);
        if (ptr != 0) {
            /* Get the advance from the JDK rasterizer. This is mostly
             * necessary for the fractional metrics case, but there are
             * also some very small number (<0.25%) of marginal cases where
             * there is some rounding difference between windows and JDK.
             * After these are resolved, we can restrict this extra
             * work to the FM case.
             */
            float advance = getGlyphAdvance(glyphCode, false);
            StrikeCache.unsafe.putFloat(ptr + StrikeCache.xAdvanceOffset,
                                        advance);
            return ptr;
        } else {
            if (FontUtilities.isLogging()) {
                FontUtilities.logWarning("Failed to render glyph using GDI: code=" + glyphCode
                                    + ", fontFamily=" + family + ", style=" + style
                                    + ", size=" + size);
            }
            return fileFont.getGlyphImage(pScalerContext, glyphCode);
        }
    }

    /* Try the native strikes first, then try the fileFont strike */
    long getGlyphImageFromX11(int glyphCode) {
        long glyphPtr;
        char charCode = fileFont.glyphToCharMap[glyphCode];
        for (int i=0;i<nativeStrikes.length;i++) {
            CharToGlyphMapper mapper = fileFont.nativeFonts[i].getMapper();
            int gc = mapper.charToGlyph(charCode)&0xffff;
            if (gc != mapper.getMissingGlyphCode()) {
                glyphPtr = nativeStrikes[i].getGlyphImagePtrNoCache(gc);
                if (glyphPtr != 0L) {
                    return glyphPtr;
                }
            }
        }
        return fileFont.getGlyphImage(pScalerContext, glyphCode);
    }

    long getGlyphImagePtr(int glyphCode) {
        if (glyphCode >= INVISIBLE_GLYPHS) {
            return StrikeCache.invisibleGlyphPtr;
        }
        long glyphPtr = 0L;
        if ((glyphPtr = getCachedGlyphPtr(glyphCode)) != 0L) {
            return glyphPtr;
        } else {
            if (useNatives) {
                glyphPtr = getGlyphImageFromNative(glyphCode);
                if (glyphPtr == 0L && FontUtilities.isLogging()) {
                    FontUtilities.logInfo("Strike for " + fileFont +
                         " at size = " + intPtSize +
                         " couldn't get native glyph for code = " + glyphCode);
                }
            }
            if (glyphPtr == 0L) {
                glyphPtr = fileFont.getGlyphImage(pScalerContext, glyphCode);
            }
            return setCachedGlyphPtr(glyphCode, glyphPtr);
        }
    }

    void getGlyphImagePtrs(int[] glyphCodes, long[] images, int  len) {

        for (int i=0; i<len; i++) {
            int glyphCode = glyphCodes[i];
            if (glyphCode >= INVISIBLE_GLYPHS) {
                images[i] = StrikeCache.invisibleGlyphPtr;
                continue;
            } else if ((images[i] = getCachedGlyphPtr(glyphCode)) != 0L) {
                continue;
            } else {
                long glyphPtr = 0L;
                if (useNatives) {
                    glyphPtr = getGlyphImageFromNative(glyphCode);
                } if (glyphPtr == 0L) {
                    glyphPtr = fileFont.getGlyphImage(pScalerContext,
                                                      glyphCode);
                }
                images[i] = setCachedGlyphPtr(glyphCode, glyphPtr);
            }
        }
    }

    /* The following method is called from CompositeStrike as a special case.
     */
    int getSlot0GlyphImagePtrs(int[] glyphCodes, long[] images, int len) {

        int convertedCnt = 0;

        for (int i=0; i<len; i++) {
            int glyphCode = glyphCodes[i];
            if (glyphCode >>> 24 != 0) {
                return convertedCnt;
            } else {
                convertedCnt++;
            }
            if (glyphCode >= INVISIBLE_GLYPHS) {
                images[i] = StrikeCache.invisibleGlyphPtr;
                continue;
            } else if ((images[i] = getCachedGlyphPtr(glyphCode)) != 0L) {
                continue;
            } else {
                long glyphPtr = 0L;
                if (useNatives) {
                    glyphPtr = getGlyphImageFromNative(glyphCode);
                }
                if (glyphPtr == 0L) {
                    glyphPtr = fileFont.getGlyphImage(pScalerContext,
                                                      glyphCode);
                }
                images[i] = setCachedGlyphPtr(glyphCode, glyphPtr);
            }
        }
        return convertedCnt;
    }

    /* Only look in the cache */
    long getCachedGlyphPtr(int glyphCode) {
        try {
            return getCachedGlyphPtrInternal(glyphCode);
        } catch (Exception e) {
          NullFontScaler nullScaler =
             (NullFontScaler)FontScaler.getNullScaler();
          long nullSC = NullFontScaler.getNullScalerContext();
          return nullScaler.getGlyphImage(nullSC, glyphCode);
        }
    }

    private long getCachedGlyphPtrInternal(int glyphCode) {
        switch (glyphCacheFormat) {
            case INTARRAY:
                return intGlyphImages[glyphCode] & INTMASK;
            case SEGINTARRAY:
                int segIndex = glyphCode >> SEGSHIFT;
                if (segIntGlyphImages[segIndex] != null) {
                    int subIndex = glyphCode % SEGSIZE;
                    return segIntGlyphImages[segIndex][subIndex] & INTMASK;
                } else {
                    return 0L;
                }
            case LONGARRAY:
                return longGlyphImages[glyphCode];
            case SEGLONGARRAY:
                segIndex = glyphCode >> SEGSHIFT;
                if (segLongGlyphImages[segIndex] != null) {
                    int subIndex = glyphCode % SEGSIZE;
                    return segLongGlyphImages[segIndex][subIndex];
                } else {
                    return 0L;
                }
        }
        /* If reach here cache is UNINITIALISED. */
        return 0L;
    }

    private synchronized long setCachedGlyphPtr(int glyphCode, long glyphPtr) {
        try {
            return setCachedGlyphPtrInternal(glyphCode, glyphPtr);
        } catch (Exception e) {
            switch (glyphCacheFormat) {
                case INTARRAY:
                case SEGINTARRAY:
                    StrikeCache.freeIntPointer((int)glyphPtr);
                    break;
                case LONGARRAY:
                case SEGLONGARRAY:
                    StrikeCache.freeLongPointer(glyphPtr);
                    break;
             }
             NullFontScaler nullScaler =
                 (NullFontScaler)FontScaler.getNullScaler();
             long nullSC = NullFontScaler.getNullScalerContext();
             return nullScaler.getGlyphImage(nullSC, glyphCode);
        }
    }

    private long setCachedGlyphPtrInternal(int glyphCode, long glyphPtr) {
        switch (glyphCacheFormat) {
            case INTARRAY:
                if (intGlyphImages[glyphCode] == 0) {
                    intGlyphImages[glyphCode] = (int)glyphPtr;
                    return glyphPtr;
                } else {
                    StrikeCache.freeIntPointer((int)glyphPtr);
                    return intGlyphImages[glyphCode] & INTMASK;
                }

            case SEGINTARRAY:
                int segIndex = glyphCode >> SEGSHIFT;
                int subIndex = glyphCode % SEGSIZE;
                if (segIntGlyphImages[segIndex] == null) {
                    segIntGlyphImages[segIndex] = new int[SEGSIZE];
                }
                if (segIntGlyphImages[segIndex][subIndex] == 0) {
                    segIntGlyphImages[segIndex][subIndex] = (int)glyphPtr;
                    return glyphPtr;
                } else {
                    StrikeCache.freeIntPointer((int)glyphPtr);
                    return segIntGlyphImages[segIndex][subIndex] & INTMASK;
                }

            case LONGARRAY:
                if (longGlyphImages[glyphCode] == 0L) {
                    longGlyphImages[glyphCode] = glyphPtr;
                    return glyphPtr;
                } else {
                    StrikeCache.freeLongPointer(glyphPtr);
                    return longGlyphImages[glyphCode];
                }

           case SEGLONGARRAY:
                segIndex = glyphCode >> SEGSHIFT;
                subIndex = glyphCode % SEGSIZE;
                if (segLongGlyphImages[segIndex] == null) {
                    segLongGlyphImages[segIndex] = new long[SEGSIZE];
                }
                if (segLongGlyphImages[segIndex][subIndex] == 0L) {
                    segLongGlyphImages[segIndex][subIndex] = glyphPtr;
                    return glyphPtr;
                } else {
                    StrikeCache.freeLongPointer(glyphPtr);
                    return segLongGlyphImages[segIndex][subIndex];
                }
        }

        /* Reach here only when the cache is not initialised which is only
         * for the first glyph to be initialised in the strike.
         * Initialise it and recurse. Note that we are already synchronized.
         */
        initGlyphCache();
        return setCachedGlyphPtr(glyphCode, glyphPtr);
    }

    /* Called only from synchronized code or constructor */
    private synchronized void initGlyphCache() {

        int numGlyphs = mapper.getNumGlyphs();
        int tmpFormat = UNINITIALISED;
        if (segmentedCache) {
            int numSegments = (numGlyphs + SEGSIZE-1)/SEGSIZE;
            if (longAddresses) {
                tmpFormat = SEGLONGARRAY;
                segLongGlyphImages = new long[numSegments][];
                this.disposer.segLongGlyphImages = segLongGlyphImages;
             } else {
                 tmpFormat = SEGINTARRAY;
                 segIntGlyphImages = new int[numSegments][];
                 this.disposer.segIntGlyphImages = segIntGlyphImages;
             }
        } else {
            if (longAddresses) {
                tmpFormat = LONGARRAY;
                longGlyphImages = new long[numGlyphs];
                this.disposer.longGlyphImages = longGlyphImages;
            } else {
                tmpFormat = INTARRAY;
                intGlyphImages = new int[numGlyphs];
                this.disposer.intGlyphImages = intGlyphImages;
            }
        }
        glyphCacheFormat = tmpFormat;
    }

    float getGlyphAdvance(int glyphCode) {
        return getGlyphAdvance(glyphCode, true);
    }

    /* Metrics info is always retrieved. If the GlyphInfo address is non-zero
     * then metrics info there is valid and can just be copied.
     * This is in user space coordinates unless getUserAdv == false.
     * Device space advance should not be propagated out of this class.
     */
    private float getGlyphAdvance(int glyphCode, boolean getUserAdv) {
        float advance;

        if (glyphCode >= INVISIBLE_GLYPHS) {
            return 0f;
        }

        /* Notes on the (getUserAdv == false) case.
         *
         * Setting getUserAdv == false is internal to this class.
         * If there's no graphics transform we can let
         * getGlyphAdvance take its course, and potentially caching in
         * advances arrays, except for signalling that
         * getUserAdv == false means there is no need to create an image.
         * It is possible that code already calculated the user advance,
         * and it is desirable to take advantage of that work.
         * But, if there's a transform and we want device advance, we
         * can't use any values cached in the advances arrays - unless
         * first re-transform them into device space using 'desc.devTx'.
         * invertDevTx is null if the graphics transform is identity,
         * a translate, or non-invertible. The latter case should
         * not ever occur in the getUserAdv == false path.
         * In other words its either null, or the inversion of a
         * simple uniform scale. If its null, we can populate and
         * use the advance caches as normal.
         *
         * If we don't find a cached value, obtain the device advance and
         * return it. This will get stashed on the image by the caller and any
         * subsequent metrics calls will be able to use it as is the case
         * whenever an image is what is initially requested.
         *
         * Don't query if there's a value cached on the image, since this
         * getUserAdv==false code path is entered solely when none exists.
         */
        if (horizontalAdvances != null) {
            advance = horizontalAdvances[glyphCode];
            if (advance != Float.MAX_VALUE) {
                if (!getUserAdv && invertDevTx != null) {
                    Point2D.Float metrics = new Point2D.Float(advance, 0f);
                    desc.devTx.deltaTransform(metrics, metrics);
                    return metrics.x;
                } else {
                    return advance;
                }
            }
        } else if (segmentedCache && segHorizontalAdvances != null) {
            int segIndex = glyphCode >> SEGSHIFT;
            float[] subArray = segHorizontalAdvances[segIndex];
            if (subArray != null) {
                advance = subArray[glyphCode % SEGSIZE];
                if (advance != Float.MAX_VALUE) {
                    if (!getUserAdv && invertDevTx != null) {
                        Point2D.Float metrics = new Point2D.Float(advance, 0f);
                        desc.devTx.deltaTransform(metrics, metrics);
                        return metrics.x;
                    } else {
                        return advance;
                    }
                }
            }
        }

        if (!getUserAdv && invertDevTx != null) {
            Point2D.Float metrics = new Point2D.Float();
            fileFont.getGlyphMetrics(pScalerContext, glyphCode, metrics);
            return metrics.x;
        }

        if (invertDevTx != null || !getUserAdv) {
            /* If there is a device transform need x & y advance to
             * transform back into user space.
             */
            advance = getGlyphMetrics(glyphCode, getUserAdv).x;
        } else {
            long glyphPtr;
            if (getImageWithAdvance) {
                /* A heuristic optimisation says that for most cases its
                 * worthwhile retrieving the image at the same time as the
                 * advance. So here we get the image data even if its not
                 * already cached.
                 */
                glyphPtr = getGlyphImagePtr(glyphCode);
            } else {
                glyphPtr = getCachedGlyphPtr(glyphCode);
            }
            if (glyphPtr != 0L) {
                advance = StrikeCache.unsafe.getFloat
                    (glyphPtr + StrikeCache.xAdvanceOffset);

            } else {
                advance = fileFont.getGlyphAdvance(pScalerContext, glyphCode);
            }
        }

        if (horizontalAdvances != null) {
            horizontalAdvances[glyphCode] = advance;
        } else if (segmentedCache && segHorizontalAdvances != null) {
            int segIndex = glyphCode >> SEGSHIFT;
            int subIndex = glyphCode % SEGSIZE;
            if (segHorizontalAdvances[segIndex] == null) {
                segHorizontalAdvances[segIndex] = new float[SEGSIZE];
                for (int i=0; i<SEGSIZE; i++) {
                     segHorizontalAdvances[segIndex][i] = Float.MAX_VALUE;
                }
            }
            segHorizontalAdvances[segIndex][subIndex] = advance;
        }
        return advance;
    }

    float getCodePointAdvance(int cp) {
        return getGlyphAdvance(mapper.charToGlyph(cp));
    }

    /**
     * Result and pt are both in device space.
     */
    void getGlyphImageBounds(int glyphCode, Point2D.Float pt,
                             Rectangle result) {

        long ptr = getGlyphImagePtr(glyphCode);
        float topLeftX, topLeftY;

        /* With our current design NULL ptr is not possible
           but if we eventually allow scalers to return NULL pointers
           this check might be actually useful. */
        if (ptr == 0L) {
            result.x = (int) Math.floor(pt.x+0.5f);
            result.y = (int) Math.floor(pt.y+0.5f);
            result.width = result.height = 0;
            return;
        }

        topLeftX = StrikeCache.unsafe.getFloat(ptr+StrikeCache.topLeftXOffset);
        topLeftY = StrikeCache.unsafe.getFloat(ptr+StrikeCache.topLeftYOffset);

        result.x = (int)Math.floor(pt.x + topLeftX + 0.5f);
        result.y = (int)Math.floor(pt.y + topLeftY + 0.5f);
        result.width =
            StrikeCache.unsafe.getShort(ptr+StrikeCache.widthOffset)  &0x0ffff;
        result.height =
            StrikeCache.unsafe.getShort(ptr+StrikeCache.heightOffset) &0x0ffff;

        /* HRGB LCD text may have padding that is empty. This is almost always
         * going to be when topLeftX is -2 or less.
         * Try to return a tighter bounding box in that case.
         * If the first three bytes of every row are all zero, then
         * add 1 to "x" and reduce "width" by 1.
         */
        if ((desc.aaHint == INTVAL_TEXT_ANTIALIAS_LCD_HRGB ||
             desc.aaHint == INTVAL_TEXT_ANTIALIAS_LCD_HBGR)
            && topLeftX <= -2.0f) {
            int minx = getGlyphImageMinX(ptr, result.x);
            if (minx > result.x) {
                result.x += 1;
                result.width -=1;
            }
        }
    }

    private int getGlyphImageMinX(long ptr, int origMinX) {

        int width = StrikeCache.unsafe.getChar(ptr+StrikeCache.widthOffset);
        int height = StrikeCache.unsafe.getChar(ptr+StrikeCache.heightOffset);
        int rowBytes =
            StrikeCache.unsafe.getChar(ptr+StrikeCache.rowBytesOffset);

        if (rowBytes == width) {
            return origMinX;
        }

        long pixelData =
            StrikeCache.unsafe.getAddress(ptr + StrikeCache.pixelDataOffset);

        if (pixelData == 0L) {
            return origMinX;
        }

        for (int y=0;y<height;y++) {
            for (int x=0;x<3;x++) {
                if (StrikeCache.unsafe.getByte(pixelData+y*rowBytes+x) != 0) {
                    return origMinX;
                }
            }
        }
        return origMinX+1;
    }

    /* These 3 metrics methods below should be implemented to return
     * values in user space.
     */
    StrikeMetrics getFontMetrics() {
        if (strikeMetrics == null) {
            strikeMetrics =
                fileFont.getFontMetrics(pScalerContext);
            if (invertDevTx != null) {
                strikeMetrics.convertToUserSpace(invertDevTx);
            }
        }
        return strikeMetrics;
    }

    Point2D.Float getGlyphMetrics(int glyphCode) {
        return getGlyphMetrics(glyphCode, true);
    }

    private Point2D.Float getGlyphMetrics(int glyphCode, boolean getImage) {
        Point2D.Float metrics = new Point2D.Float();

        // !!! or do we force sgv user glyphs?
        if (glyphCode >= INVISIBLE_GLYPHS) {
            return metrics;
        }
        long glyphPtr;
        if (getImageWithAdvance && getImage) {
            /* A heuristic optimisation says that for most cases its
             * worthwhile retrieving the image at the same time as the
             * metrics. So here we get the image data even if its not
             * already cached.
             */
            glyphPtr = getGlyphImagePtr(glyphCode);
        } else {
             glyphPtr = getCachedGlyphPtr(glyphCode);
        }
        if (glyphPtr != 0L) {
            metrics = new Point2D.Float();
            metrics.x = StrikeCache.unsafe.getFloat
                (glyphPtr + StrikeCache.xAdvanceOffset);
            metrics.y = StrikeCache.unsafe.getFloat
                (glyphPtr + StrikeCache.yAdvanceOffset);
            /* advance is currently in device space, need to convert back
             * into user space.
             * This must not include the translation component. */
            if (invertDevTx != null) {
                invertDevTx.deltaTransform(metrics, metrics);
            }
        } else {
            /* We sometimes cache these metrics as they are expensive to
             * generate for large glyphs.
             * We never reach this path if we obtain images with advances.
             * But if we do not obtain images with advances its possible that
             * we first obtain this information, then the image, and never
             * will access this value again.
             */
            Integer key = Integer.valueOf(glyphCode);
            Point2D.Float value = null;
            ConcurrentHashMap<Integer, Point2D.Float> glyphMetricsMap = null;
            if (glyphMetricsMapRef != null) {
                glyphMetricsMap = glyphMetricsMapRef.get();
            }
            if (glyphMetricsMap != null) {
                value = glyphMetricsMap.get(key);
                if (value != null) {
                    metrics.x = value.x;
                    metrics.y = value.y;
                    /* already in user space */
                    return metrics;
                }
            }
            if (value == null) {
                fileFont.getGlyphMetrics(pScalerContext, glyphCode, metrics);
                /* advance is currently in device space, need to convert back
                 * into user space.
                 */
                if (invertDevTx != null) {
                    invertDevTx.deltaTransform(metrics, metrics);
                }
                value = new Point2D.Float(metrics.x, metrics.y);
                /* We aren't synchronizing here so it is possible to
                 * overwrite the map with another one but this is harmless.
                 */
                if (glyphMetricsMap == null) {
                    glyphMetricsMap =
                        new ConcurrentHashMap<Integer, Point2D.Float>();
                    glyphMetricsMapRef =
                        new SoftReference<ConcurrentHashMap<Integer,
                        Point2D.Float>>(glyphMetricsMap);
                }
                glyphMetricsMap.put(key, value);
            }
        }
        return metrics;
    }

    Point2D.Float getCharMetrics(char ch) {
        return getGlyphMetrics(mapper.charToGlyph(ch));
    }

    /* The caller of this can be trusted to return a copy of this
     * return value rectangle to public API. In fact frequently it
     * can't use this return value directly anyway.
     * This returns bounds in device space. Currently the only
     * caller is SGV and it converts back to user space.
     * We could change things so that this code does the conversion so
     * that all coords coming out of the font system are converted back
     * into user space even if they were measured in device space.
     * The same applies to the other methods that return outlines (below)
     * But it may make particular sense for this method that caches its
     * results.
     * There'd be plenty of exceptions, to this too, eg getGlyphPoint needs
     * device coords as its called from native layout and getGlyphImageBounds
     * is used by GlyphVector.getGlyphPixelBounds which is specified to
     * return device coordinates, the image pointers aren't really used
     * up in Java code either.
     */
    Rectangle2D.Float getGlyphOutlineBounds(int glyphCode) {

        if (boundsMap == null) {
            boundsMap = new ConcurrentHashMap<Integer, Rectangle2D.Float>();
        }

        Integer key = Integer.valueOf(glyphCode);
        Rectangle2D.Float bounds = boundsMap.get(key);

        if (bounds == null) {
            bounds = fileFont.getGlyphOutlineBounds(pScalerContext, glyphCode);
            boundsMap.put(key, bounds);
        }
        return bounds;
    }

    public Rectangle2D getOutlineBounds(int glyphCode) {
        return fileFont.getGlyphOutlineBounds(pScalerContext, glyphCode);
    }

    private
        WeakReference<ConcurrentHashMap<Integer,GeneralPath>> outlineMapRef;

    GeneralPath getGlyphOutline(int glyphCode, float x, float y) {

        GeneralPath gp = null;
        ConcurrentHashMap<Integer, GeneralPath> outlineMap = null;

        if (outlineMapRef != null) {
            outlineMap = outlineMapRef.get();
            if (outlineMap != null) {
                gp = outlineMap.get(glyphCode);
            }
        }

        if (gp == null) {
            gp = fileFont.getGlyphOutline(pScalerContext, glyphCode, 0, 0);
            if (outlineMap == null) {
                outlineMap = new ConcurrentHashMap<Integer, GeneralPath>();
                outlineMapRef =
                   new WeakReference
                       <ConcurrentHashMap<Integer,GeneralPath>>(outlineMap);
            }
            outlineMap.put(glyphCode, gp);
        }
        gp = (GeneralPath)gp.clone(); // mutable!
        if (x != 0f || y != 0f) {
            gp.transform(AffineTransform.getTranslateInstance(x, y));
        }
        return gp;
    }

    GeneralPath getGlyphVectorOutline(int[] glyphs, float x, float y) {
        return fileFont.getGlyphVectorOutline(pScalerContext,
                                              glyphs, glyphs.length, x, y);
    }

    protected void adjustPoint(Point2D.Float pt) {
        if (invertDevTx != null) {
            invertDevTx.deltaTransform(pt, pt);
        }
    }
}
