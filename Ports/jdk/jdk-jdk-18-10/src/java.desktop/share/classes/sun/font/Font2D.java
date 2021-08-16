/*
 * Copyright (c) 2003, 2018, Oracle and/or its affiliates. All rights reserved.
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
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Locale;
import java.util.Set;

public abstract class Font2D {

    /* Note: JRE and FONT_CONFIG ranks are identical. I don't know of a reason
     * to distingish these. Possibly if a user adds fonts to the JRE font
     * directory that are the same font as the ones specified in the font
     * configuration but that is more likely to be the legitimate intention
     * than a problem. One reason why these should be the same is that on
     * Linux the JRE fonts ARE the font configuration fonts, and although I
     * believe all are assigned FONT_CONFIG rank, it is conceivable that if
     * this were not so, that some JRE font would not be allowed to joint the
     * family of its siblings which were assigned FONT_CONFIG rank. Giving
     * them the same rank is the easy solution for now at least.
     */
    public static final int FONT_CONFIG_RANK   = 2;
    public static final int JRE_RANK     = 2;
    public static final int TTF_RANK     = 3;
    public static final int TYPE1_RANK   = 4;
    public static final int NATIVE_RANK  = 5;
    public static final int UNKNOWN_RANK = 6;
    public static final int DEFAULT_RANK = 4;

    private static final String[] boldNames = {
        "bold", "demibold", "demi-bold", "demi bold", "negreta", "demi", };

    private static final String[] italicNames = {
        "italic", "cursiva", "oblique", "inclined", };

    private static final String[] boldItalicNames = {
          "bolditalic", "bold-italic", "bold italic",
          "boldoblique", "bold-oblique", "bold oblique",
          "demibold italic", "negreta cursiva","demi oblique", };

    private static final FontRenderContext DEFAULT_FRC =
        new FontRenderContext(null, false, false);

    public Font2DHandle handle;
    protected String familyName;           /* Family font name (english) */
    protected String fullName;             /* Full font name (english)   */
    protected int style = Font.PLAIN;
    protected FontFamily family;
    protected int fontRank = DEFAULT_RANK;

    /*
     * A mapper can be independent of the strike.
     * Perhaps the reference to the mapper ought to be held on the
     * scaler, as it may be implemented via scaler functionality anyway
     * and so the mapper would be useless if its native portion was
     * freed when the scaler was GC'd.
     */
    protected CharToGlyphMapper mapper;

    /*
     * The strike cache is maintained per "Font2D" as that is the
     * principal object by which you look up fonts.
     * It means more Hashmaps, but look ups can be quicker because
     * the map will have fewer entries, and there's no need to try to
     * make the Font2D part of the key.
     */
    protected ConcurrentHashMap<FontStrikeDesc, Reference<FontStrike>>
        strikeCache = new ConcurrentHashMap<>();

    /* Store the last Strike in a Reference object.
     * Similarly to the strike that was stored on a C++ font object,
     * this is an optimisation which helps if multiple clients (ie
     * typically SunGraphics2D instances) are using the same font, then
     * as may be typical of many UIs, they are probably using it in the
     * same style, so it can be a win to first quickly check if the last
     * strike obtained from this Font2D satifies the needs of the next
     * client too.
     * This pre-supposes that a FontStrike is a shareable object, which
     * it should.
     */
    protected Reference<FontStrike> lastFontStrike = new WeakReference<>(null);

    /*
     * if useWeak is true, proactively clear the cache after this
     * many strikes are present. 0 means leave it alone.
     */
    private int strikeCacheMax = 0;
    /*
     * Whether to use weak refs for this font, even if soft refs is the default.
     */
    private boolean useWeak;

    void setUseWeakRefs(boolean weak, int maxStrikes) {
        this.useWeak = weak;
        this.strikeCacheMax = weak && maxStrikes > 0 ? maxStrikes : 0;
    }

    /*
     * POSSIBLE OPTIMISATION:
     * Array of length 1024 elements of 64 bits indicating if a font
     * contains these. This kind of information can be shared between
     * all point sizes.
     * if corresponding bit in knownBitmaskMap is set then canDisplayBitmaskMap
     * is valid. This is 16Kbytes of data per composite font style.
     * What about UTF-32 and surrogates?
     * REMIND: This is too much storage. Probably can only cache this
     * information for latin range, although possibly OK to store all
     * for just the "logical" fonts.
     * Or instead store arrays of subranges of 1024 bits (128 bytes) in
     * the range below surrogate pairs.
     */
//     protected long[] knownBitmaskMap;
//     protected long[] canDisplayBitmaskMap;

    /* Returns the "real" style of this Font2D. Eg the font face
     * Lucida Sans Bold" has a real style of Font.BOLD, even though
     * it may be able to used to simulate bold italic
     */
    public int getStyle() {
        return style;
    }
    protected void setStyle() {

        String fName = fullName.toLowerCase();

        for (int i=0; i < boldItalicNames.length; i++) {
            if (fName.indexOf(boldItalicNames[i]) != -1) {
                style = Font.BOLD|Font.ITALIC;
                return;
            }
        }

        for (int i=0; i < italicNames.length; i++) {
            if (fName.indexOf(italicNames[i]) != -1) {
                style = Font.ITALIC;
                return;
            }
        }

        for (int i=0; i < boldNames.length; i++) {
            if (fName.indexOf(boldNames[i]) != -1 ) {
                style = Font.BOLD;
                return;
            }
        }
    }

    public static final int FWIDTH_NORMAL = 5;    // OS/2 usWidthClass
    public static final int FWEIGHT_NORMAL = 400; // OS/2 usWeightClass
    public static final int FWEIGHT_BOLD   = 700; // OS/2 usWeightClass

    public int getWidth() {
        return FWIDTH_NORMAL;
    }

    public int getWeight() {
        if ((style & Font.BOLD) !=0) {
            return FWEIGHT_BOLD;
        } else {
            return FWEIGHT_NORMAL;
        }
    }

    int getRank() {
        return fontRank;
    }

    void setRank(int rank) {
        fontRank = rank;
    }

    abstract CharToGlyphMapper getMapper();



    /* This isn't very efficient but its infrequently used.
     * StandardGlyphVector uses it when the client assigns the glyph codes.
     * These may not be valid. This validates them substituting the missing
     * glyph elsewhere.
     */
    protected int getValidatedGlyphCode(int glyphCode) {
        if (glyphCode < 0 || glyphCode >= getMapper().getNumGlyphs()) {
            glyphCode = getMapper().getMissingGlyphCode();
        }
        return glyphCode;
    }

    /*
     * Creates an appropriate strike for the Font2D subclass
     */
    abstract FontStrike createStrike(FontStrikeDesc desc);

    /* this may be useful for APIs like canDisplay where the answer
     * is dependent on the font and its scaler, but not the strike.
     * If no strike has ever been returned, then create a one that matches
     * this font with the default FRC. It will become the lastStrike and
     * there's a good chance that the next call will be to get exactly that
     * strike.
     */
    public FontStrike getStrike(Font font) {
        FontStrike strike = lastFontStrike.get();
        if (strike != null) {
            return strike;
        } else {
            return getStrike(font, DEFAULT_FRC);
        }
    }

    /* SunGraphics2D has font, tx, aa and fm. From this info
     * can get a Strike object from the cache, creating it if necessary.
     * This code is designed for multi-threaded access.
     * For that reason it creates a local FontStrikeDesc rather than filling
     * in a shared one. Up to two AffineTransforms and one FontStrikeDesc will
     * be created by every lookup. This appears to perform more than
     * adequately. But it may make sense to expose FontStrikeDesc
     * as a parameter so a caller can use its own.
     * In such a case if a FontStrikeDesc is stored as a key then
     * we would need to use a private copy.
     *
     * Note that this code doesn't prevent two threads from creating
     * two different FontStrike instances and having one of the threads
     * overwrite the other in the map. This is likely to be a rare
     * occurrence and the only consequence is that these callers will have
     * different instances of the strike, and there'd be some duplication of
     * population of the strikes. However since users of these strikes are
     * transient, then the one that was overwritten would soon be freed.
     * If there is any problem then a small synchronized block would be
     * required with its attendant consequences for MP scaleability.
     */
    public FontStrike getStrike(Font font, AffineTransform devTx,
                                int aa, int fm) {

        /* Create the descriptor which is used to identify a strike
         * in the strike cache/map. A strike is fully described by
         * the attributes of this descriptor.
         */
        /* REMIND: generating garbage and doing computation here in order
         * to include pt size in the tx just for a lookup! Figure out a
         * better way.
         */
        double ptSize = font.getSize2D();
        AffineTransform glyphTx = (AffineTransform)devTx.clone();
        glyphTx.scale(ptSize, ptSize);
        if (font.isTransformed()) {
            glyphTx.concatenate(font.getTransform());
        }
        if (glyphTx.getTranslateX() != 0 || glyphTx.getTranslateY() != 0) {
            glyphTx.setTransform(glyphTx.getScaleX(),
                                 glyphTx.getShearY(),
                                 glyphTx.getShearX(),
                                 glyphTx.getScaleY(),
                                 0.0, 0.0);
        }
        FontStrikeDesc desc = new FontStrikeDesc(devTx, glyphTx,
                                                 font.getStyle(), aa, fm);
        return getStrike(desc, false);
    }

    public FontStrike getStrike(Font font, AffineTransform devTx,
                                AffineTransform glyphTx,
                                int aa, int fm) {

        /* Create the descriptor which is used to identify a strike
         * in the strike cache/map. A strike is fully described by
         * the attributes of this descriptor.
         */
        FontStrikeDesc desc = new FontStrikeDesc(devTx, glyphTx,
                                                 font.getStyle(), aa, fm);
        return getStrike(desc, false);
    }

    public FontStrike getStrike(Font font, FontRenderContext frc) {

        AffineTransform at = frc.getTransform();
        double ptSize = font.getSize2D();
        at.scale(ptSize, ptSize);
        if (font.isTransformed()) {
            at.concatenate(font.getTransform());
            if (at.getTranslateX() != 0 || at.getTranslateY() != 0) {
                at.setTransform(at.getScaleX(),
                                at.getShearY(),
                                at.getShearX(),
                                at.getScaleY(),
                                0.0, 0.0);
            }
        }
        int aa = FontStrikeDesc.getAAHintIntVal(this, font, frc);
        int fm = FontStrikeDesc.getFMHintIntVal(frc.getFractionalMetricsHint());
        FontStrikeDesc desc = new FontStrikeDesc(frc.getTransform(),
                                                 at, font.getStyle(),
                                                 aa, fm);
        return getStrike(desc, false);
    }

    void updateLastStrikeRef(FontStrike strike) {
        lastFontStrike.clear();
        if (useWeak) {
            lastFontStrike = new WeakReference<>(strike);
        } else {
            lastFontStrike = new SoftReference<>(strike);
        }
    }

    FontStrike getStrike(FontStrikeDesc desc) {
        return getStrike(desc, true);
    }

    private FontStrike getStrike(FontStrikeDesc desc, boolean copy) {
        /* Before looking in the map, see if the descriptor matches the
         * last strike returned from this Font2D. This should often be a win
         * since its common for the same font, in the same size to be
         * used frequently, for example in many parts of a UI.
         *
         * If its not the same then we use the descriptor to locate a
         * Reference to the strike. If it exists and points to a strike,
         * then we update the last strike to refer to that and return it.
         *
         * If the key isn't in the map, or its reference object has been
         * collected, then we create a new strike, put it in the map and
         * set it to be the last strike.
         */
        FontStrike strike = lastFontStrike.get();
        if (strike != null && desc.equals(strike.desc)) {
            return strike;
        } else {
            Reference<FontStrike> strikeRef = strikeCache.get(desc);
            if (strikeRef != null) {
                strike = strikeRef.get();
                if (strike != null) {
                    updateLastStrikeRef(strike);
                    StrikeCache.refStrike(strike);
                    return strike;
                }
            }
            /* When we create a new FontStrike instance, we *must*
             * ask the StrikeCache for a reference. We must then ensure
             * this reference remains reachable, by storing it in the
             * Font2D's strikeCache map.
             * So long as the Reference is there (reachable) then if the
             * reference is cleared, it will be enqueued for disposal.
             * If for some reason we explicitly remove this reference, it
             * must only be done when holding a strong reference to the
             * referent (the FontStrike), or if the reference is cleared,
             * then we must explicitly "dispose" of the native resources.
             * The only place this currently happens is in this same method,
             * where we find a cleared reference and need to overwrite it
             * here with a new reference.
             * Clearing the whilst holding a strong reference, should only
             * be done if the
             */
            if (copy) {
                desc = new FontStrikeDesc(desc);
            }
            strike = createStrike(desc);
            //StrikeCache.addStrike();
            /* If we are creating many strikes on this font which
             * involve non-quadrant rotations, or more general
             * transforms which include shears, then force the use
             * of weak references rather than soft references.
             * This means that it won't live much beyond the next GC,
             * which is what we want for what is likely a transient strike.
             */
            int txType = desc.glyphTx.getType();
            if (useWeak ||
                txType == AffineTransform.TYPE_GENERAL_TRANSFORM ||
                (txType & AffineTransform.TYPE_GENERAL_ROTATION) != 0 &&
                strikeCache.size() > 10) {
                strikeRef = StrikeCache.getStrikeRef(strike, true);
            } else {
                strikeRef = StrikeCache.getStrikeRef(strike, useWeak);
            }
            strikeCache.put(desc, strikeRef);
            updateLastStrikeRef(strike);
            StrikeCache.refStrike(strike);
            return strike;
        }
    }

    /**
     * The length of the metrics array must be >= 8.  This method will
     * store the following elements in that array before returning:
     *    metrics[0]: ascent
     *    metrics[1]: descent
     *    metrics[2]: leading
     *    metrics[3]: max advance
     *    metrics[4]: strikethrough offset
     *    metrics[5]: strikethrough thickness
     *    metrics[6]: underline offset
     *    metrics[7]: underline thickness
     */
    public void getFontMetrics(Font font, AffineTransform at,
                               Object aaHint, Object fmHint,
                               float[] metrics) {
        /* This is called in just one place in Font with "at" == identity.
         * Perhaps this can be eliminated.
         */
        int aa = FontStrikeDesc.getAAHintIntVal(aaHint, this, font.getSize());
        int fm = FontStrikeDesc.getFMHintIntVal(fmHint);
        FontStrike strike = getStrike(font, at, aa, fm);
        StrikeMetrics strikeMetrics = strike.getFontMetrics();
        metrics[0] = strikeMetrics.getAscent();
        metrics[1] = strikeMetrics.getDescent();
        metrics[2] = strikeMetrics.getLeading();
        metrics[3] = strikeMetrics.getMaxAdvance();

        getStyleMetrics(font.getSize2D(), metrics, 4);
    }

    /**
     * The length of the metrics array must be >= offset+4, and offset must be
     * >= 0.  Typically offset is 4.  This method will
     * store the following elements in that array before returning:
     *    metrics[off+0]: strikethrough offset
     *    metrics[off+1]: strikethrough thickness
     *    metrics[off+2]: underline offset
     *    metrics[off+3]: underline thickness
     *
     * Note that this implementation simply returns default values;
     * subclasses can override this method to provide more accurate values.
     */
    public void getStyleMetrics(float pointSize, float[] metrics, int offset) {
        metrics[offset] = -metrics[0] / 2.5f;
        metrics[offset+1] = pointSize / 12;
        metrics[offset+2] = metrics[offset+1] / 1.5f;
        metrics[offset+3] = metrics[offset+1];
    }

    /**
     * The length of the metrics array must be >= 4.  This method will
     * store the following elements in that array before returning:
     *    metrics[0]: ascent
     *    metrics[1]: descent
     *    metrics[2]: leading
     *    metrics[3]: max advance
     */
    public void getFontMetrics(Font font, FontRenderContext frc,
                               float[] metrics) {
        StrikeMetrics strikeMetrics = getStrike(font, frc).getFontMetrics();
        metrics[0] = strikeMetrics.getAscent();
        metrics[1] = strikeMetrics.getDescent();
        metrics[2] = strikeMetrics.getLeading();
        metrics[3] = strikeMetrics.getMaxAdvance();
    }

    /* Currently the layout code calls this. May be better for layout code
     * to check the font class before attempting to run, rather than needing
     * to promote this method up from TrueTypeFont
     */
    protected byte[] getTableBytes(int tag) {
        return null;
    }

    /* Used only on OS X.
     */
    protected long getPlatformNativeFontPtr() {
        return 0L;
    }

    /* for layout code */
    protected long getUnitsPerEm() {
        return 2048;
    }

    boolean supportsEncoding(String encoding) {
        return false;
    }

    public boolean canDoStyle(int style) {
        return (style == this.style);
    }

    /*
     * All the important subclasses override this which is principally for
     * the TrueType 'gasp' table.
     */
    public boolean useAAForPtSize(int ptsize) {
        return true;
    }

    public boolean hasSupplementaryChars() {
        return false;
    }

    /* The following methods implement public methods on java.awt.Font */
    public String getPostscriptName() {
        return fullName;
    }

    public String getFontName(Locale l) {
        return fullName;
    }

    public String getFamilyName(Locale l) {
        return familyName;
    }

    public int getNumGlyphs() {
        return getMapper().getNumGlyphs();
    }

    public int charToGlyph(int wchar) {
        return getMapper().charToGlyph(wchar);
    }

    public int charToVariationGlyph(int wchar, int variationSelector) {
        return getMapper().charToVariationGlyph(wchar, variationSelector);
    }

    public int getMissingGlyphCode() {
        return getMapper().getMissingGlyphCode();
    }

    public boolean canDisplay(char c) {
        return getMapper().canDisplay(c);
    }

    public boolean canDisplay(int cp) {
        return getMapper().canDisplay(cp);
    }

    public byte getBaselineFor(char c) {
        return Font.ROMAN_BASELINE;
    }

    public float getItalicAngle(Font font, AffineTransform at,
                                Object aaHint, Object fmHint) {
        /* hardwire psz=12 as that's typical and AA vs non-AA for 'gasp' mode
         * isn't important for the caret slope of this rarely used API.
         */
        int aa = FontStrikeDesc.getAAHintIntVal(aaHint, this, 12);
        int fm = FontStrikeDesc.getFMHintIntVal(fmHint);
        FontStrike strike = getStrike(font, at, aa, fm);
        StrikeMetrics metrics = strike.getFontMetrics();
        if (metrics.ascentY == 0 || metrics.ascentX == 0) {
            return 0f;
        } else {
            /* ascent is "up" from the baseline so its typically
             * a negative value, so we need to compensate
             */
            return metrics.ascentX/-metrics.ascentY;
        }
    }

}
