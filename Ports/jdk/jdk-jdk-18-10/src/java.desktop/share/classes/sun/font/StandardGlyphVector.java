/*
 * Copyright (c) 1998, 2017, Oracle and/or its affiliates. All rights reserved.
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
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import static java.awt.RenderingHints.*;
import java.awt.Shape;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphMetrics;
import java.awt.font.GlyphJustificationInfo;
import java.awt.font.GlyphVector;
import java.awt.font.LineMetrics;
import java.awt.font.TextAttribute;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.lang.ref.SoftReference;
import java.text.CharacterIterator;

import sun.awt.SunHints;
import sun.java2d.loops.FontInfo;

/**
 * Standard implementation of GlyphVector used by Font, GlyphList, and
 * SunGraphics2D.
 *
 * The main issues involve the semantics of the various transforms
 * (font, glyph, device) and their effect on rendering and metrics.
 *
 * Very, very unfortunately, the translation component of the font
 * transform affects where the text gets rendered.  It offsets the
 * rendering origin.  None of the other metrics of the glyphvector
 * are affected, making them inconsistent with the rendering behavior.
 * I think the translation component of the font would be better
 * interpreted as the translation component of a per-glyph transform,
 * but I don't know if this is possible to change.
 *
 * After the font transform is applied, the glyph transform is
 * applied.  This makes glyph transforms relative to font transforms,
 * if the font transform changes, the glyph transform will have the
 * same (relative) effect on the outline of the glyph.  The outline
 * and logical bounds are passed through the glyph transform before
 * being returned.  The glyph metrics ignore the glyph transform, but
 * provide the outline bounds and the advance vector of the glyph (the
 * latter will be rotated if the font is rotated).  The default layout
 * places each glyph at the end of the advance vector of the previous
 * glyph, and since the glyph transform translates the advance vector,
 * this means a glyph transform affects the positions of all
 * subsequent glyphs if defaultLayout is called after setting a glyph
 * transform.  In the glyph info array, the bounds are the outline
 * bounds including the glyph transform, and the positions are as
 * computed, and the advances are the deltas between the positions.
 *
 * (There's a bug in the logical bounds of a rotated glyph for
 * composite fonts, it's not to spec (in 1.4.0, 1.4.1, 1.4.2).  The
 * problem is that the rotated composite doesn't handle the multiple
 * ascents and descents properly in both x and y.  You end up with
 * a rotated advance vector but an unrotated ascent and descent.)
 *
 * Finally, the whole thing is transformed by the device transform to
 * position it on the page.
 *
 * Another bug: The glyph outline seems to ignore fractional point
 * size information, but the images (and advances) don't ignore it.
 *
 * Small fonts drawn at large magnification have odd advances when
 * fractional metrics is off-- that's because the advances depend on
 * the frc.  When the frc is scaled appropriately, the advances are
 * fine.  FM or a large frc (high numbers) make the advances right.
 *
 * The buffer aa flag doesn't affect rendering, the glyph vector
 * renders as AA if aa is set in its frc, and as non-aa if aa is not
 * set in its frc.
 *
 * font rotation, baseline, vertical etc.
 *
 * Font rotation and baseline Line metrics should be measured along a
 * unit vector pi/4 cc from the baseline vector.  For 'horizontal'
 * fonts the baseline vector is the x vector passed through the font
 * transform (ignoring translation), for 'vertical' it is the y
 * vector.  This definition makes ascent, descent, etc independent of
 * shear, so shearing can be used to simulate italic. This means no
 * fonts have 'negative ascents' or 'zero ascents' etc.
 *
 * Having a coordinate system with orthogonal axes where one is
 * parallel to the baseline means we could use rectangles and interpret
 * them in terms of this coordinate system.  Unfortunately there
 * is support for rotated fonts in the jdk already so maintaining
 * the semantics of existing code (getlogical bounds, etc) might
 * be difficult.
 *
 * A font transform transforms both the baseline and all the glyphs
 * in the font, so it does not rotate the glyph w.r.t the baseline.
 * If you do want to rotate individual glyphs, you need to apply a
 * glyph transform.  If performDefaultLayout is called after this,
 * the transformed glyph advances will affect the glyph positions.
 *
 * useful additions
 * - select vertical metrics - glyphs are rotated pi/4 cc and vertical
 * metrics are used to align them to the baseline.
 * - define baseline for font (glyph rotation not linked to baseline)
 * - define extra space (delta between each glyph along baseline)
 * - define offset (delta from 'true' baseline, impacts ascent and
 * descent as these are still computed from true basline and pinned
 * to zero, used in superscript).
 */
public class StandardGlyphVector extends GlyphVector {
    private Font font;
    private FontRenderContext frc;
    private int[] glyphs; // always
    private int[] userGlyphs; // used to return glyphs to the client.
    private float[] positions; // only if not default advances
    private int[] charIndices;  // only if interesting
    private int flags; // indicates whether positions, charIndices is interesting

    private static final int UNINITIALIZED_FLAGS = -1;

    // transforms information
    private GlyphTransformInfo gti; // information about per-glyph transforms

    // !!! can we get rid of any of this extra stuff?
    private AffineTransform ftx;   // font transform without translation
    private AffineTransform dtx;   // device transform used for strike calculations, no translation
    private AffineTransform invdtx; // inverse of dtx or null if dtx is identity
    private AffineTransform frctx; // font render context transform, wish we could just share it
    private Font2D font2D;         // basic strike-independent stuff
    private SoftReference<GlyphStrike> fsref;   // font strike reference for glyphs with no per-glyph transform

    /////////////////////////////
    // Constructors and Factory methods
    /////////////////////////////

    public StandardGlyphVector(Font font, String str, FontRenderContext frc) {
        init(font, str.toCharArray(), 0, str.length(), frc, UNINITIALIZED_FLAGS);
    }

    public StandardGlyphVector(Font font, char[] text, FontRenderContext frc) {
        init(font, text, 0, text.length, frc, UNINITIALIZED_FLAGS);
    }

    public StandardGlyphVector(Font font, char[] text, int start, int count,
                               FontRenderContext frc) {
        init(font, text, start, count, frc, UNINITIALIZED_FLAGS);
    }

    private float getTracking(Font font) {
        if (font.hasLayoutAttributes()) {
            AttributeValues values = ((AttributeMap)font.getAttributes()).getValues();
            return values.getTracking();
        }
        return 0;
    }

     // used by GlyphLayout to construct a glyphvector
    public StandardGlyphVector(Font font, FontRenderContext frc, int[] glyphs, float[] positions,
                               int[] indices, int flags) {
        initGlyphVector(font, frc, glyphs, positions, indices, flags);

        // this code should go into layout
        float track = getTracking(font);
        if (track != 0) {
            track *= font.getSize2D();
            Point2D.Float trackPt = new Point2D.Float(track, 0); // advance delta
            if (font.isTransformed()) {
                AffineTransform at = font.getTransform();
                at.deltaTransform(trackPt, trackPt);
            }

            // how do we know its a base glyph
            // for now, it is if the natural advance of the glyph is non-zero
            Font2D f2d = FontUtilities.getFont2D(font);
            FontStrike strike = f2d.getStrike(font, frc);

            float[] deltas = { trackPt.x, trackPt.y };
            for (int j = 0; j < deltas.length; ++j) {
                float inc = deltas[j];
                if (inc != 0) {
                    float delta = 0;
                    for (int i = j, n = 0; n < glyphs.length; i += 2) {
                        if (strike.getGlyphAdvance(glyphs[n++]) != 0) { // might be an inadequate test
                            positions[i] += delta;
                            delta += inc;
                        }
                    }
                    positions[positions.length-2+j] += delta;
                }
            }
        }
    }

    public void initGlyphVector(Font font, FontRenderContext frc, int[] glyphs, float[] positions,
                                int[] indices, int flags) {
        this.font = font;
        this.frc = frc;
        this.glyphs = glyphs;
        this.userGlyphs = glyphs; // no need to check
        this.positions = positions;
        this.charIndices = indices;
        this.flags = flags;

        initFontData();
    }

    public StandardGlyphVector(Font font, CharacterIterator iter, FontRenderContext frc) {
        int offset = iter.getBeginIndex();
        char[] text = new char [iter.getEndIndex() - offset];
        for(char c = iter.first();
            c != CharacterIterator.DONE;
            c = iter.next()) {
            text[iter.getIndex() - offset] = c;
        }
        init(font, text, 0, text.length, frc, UNINITIALIZED_FLAGS);
    }

    public StandardGlyphVector(Font font, int[] glyphs, FontRenderContext frc) {
        // !!! find callers of this
        // should be able to fully init from raw data, e.g. charmap, flags too.
        this.font = font;
        this.frc = frc;
        this.flags = UNINITIALIZED_FLAGS;

        initFontData();
        this.userGlyphs = glyphs;
        this.glyphs = getValidatedGlyphs(this.userGlyphs);
    }

    /* This is called from the rendering loop. FontInfo is supplied
     * because a GV caches a strike and glyph images suitable for its FRC.
     * LCD text isn't currently supported on all surfaces, in which case
     * standard AA must be used. This is most likely to occur when LCD text
     * is requested and the surface is some non-standard type or hardward
     * surface for which there are no accelerated loops.
     * We can detect this as being AA=="ON" in the FontInfo and AA!="ON"
     * and AA!="GASP" in the FRC - since this only occurs for LCD text we don't
     * need to check any more precisely what value is in the FRC.
     */
    public static StandardGlyphVector getStandardGV(GlyphVector gv,
                                                    FontInfo info) {
        if (info.aaHint == SunHints.INTVAL_TEXT_ANTIALIAS_ON) {
            Object aaHint = gv.getFontRenderContext().getAntiAliasingHint();
            if (aaHint != VALUE_TEXT_ANTIALIAS_ON &&
                aaHint != VALUE_TEXT_ANTIALIAS_GASP) {
                /* We need to create a new GV with AA==ON for rendering */
                FontRenderContext frc = gv.getFontRenderContext();
                frc = new FontRenderContext(frc.getTransform(),
                                            VALUE_TEXT_ANTIALIAS_ON,
                                            frc.getFractionalMetricsHint());
                return new StandardGlyphVector(gv, frc);
            }
        }
        if (gv instanceof StandardGlyphVector) {
            return (StandardGlyphVector)gv;
        }
        return new StandardGlyphVector(gv, gv.getFontRenderContext());
    }

    /////////////////////////////
    // GlyphVector API
    /////////////////////////////

    public Font getFont() {
        return this.font;
    }

    public FontRenderContext getFontRenderContext() {
        return this.frc;
    }

    public void performDefaultLayout() {
        positions = null;
        if (getTracking(font) == 0) {
            clearFlags(FLAG_HAS_POSITION_ADJUSTMENTS);
        }
    }

    public int getNumGlyphs() {
        return glyphs.length;
    }

    public int getGlyphCode(int glyphIndex) {
        return userGlyphs[glyphIndex];
    }

    public int[] getGlyphCodes(int start, int count, int[] result) {
        if (count < 0) {
            throw new IllegalArgumentException("count = " + count);
        }
        if (start < 0) {
            throw new IndexOutOfBoundsException("start = " + start);
        }
        if (start > glyphs.length - count) { // watch out for overflow if index + count overlarge
            throw new IndexOutOfBoundsException("start + count = " + (start + count));
        }

        if (result == null) {
            result = new int[count];
        }

        // if arraycopy were faster, we wouldn't code this
        for (int i = 0; i < count; ++i) {
            result[i] = userGlyphs[i + start];
        }

        return result;
    }

    public int getGlyphCharIndex(int ix) {
        if (ix < 0 && ix >= glyphs.length) {
            throw new IndexOutOfBoundsException("" + ix);
        }
        if (charIndices == null) {
            if ((getLayoutFlags() & FLAG_RUN_RTL) != 0) {
                return glyphs.length - 1 - ix;
            }
            return ix;
        }
        return charIndices[ix];
    }

    public int[] getGlyphCharIndices(int start, int count, int[] result) {
        if (start < 0 || count < 0 || (count > glyphs.length - start)) {
            throw new IndexOutOfBoundsException("" + start + ", " + count);
        }
        if (result == null) {
            result = new int[count];
        }
        if (charIndices == null) {
            if ((getLayoutFlags() & FLAG_RUN_RTL) != 0) {
                for (int i = 0, n = glyphs.length - 1 - start;
                     i < count; ++i, --n) {
                         result[i] = n;
                     }
            } else {
                for (int i = 0, n = start; i < count; ++i, ++n) {
                    result[i] = n;
                }
            }
        } else {
            for (int i = 0; i < count; ++i) {
                result[i] = charIndices[i + start];
            }
        }
        return result;
    }

    // !!! not cached, assume TextLayout will cache if necessary
    // !!! reexamine for per-glyph-transforms
    // !!! revisit for text-on-a-path, vertical
    public Rectangle2D getLogicalBounds() {
        setFRCTX();
        initPositions();

        LineMetrics lm = font.getLineMetrics("", frc);

        float minX, minY, maxX, maxY;
        // horiz only for now...
        minX = 0;
        minY = -lm.getAscent();
        maxX = 0;
        maxY = lm.getDescent() + lm.getLeading();
        if (glyphs.length > 0) {
            maxX = positions[positions.length - 2];
        }

        return new Rectangle2D.Float(minX, minY, maxX - minX, maxY - minY);
    }

    // !!! not cached, assume TextLayout will cache if necessary
    public Rectangle2D getVisualBounds() {
        Rectangle2D result = null;
        for (int i = 0; i < glyphs.length; ++i) {
            Rectangle2D glyphVB = getGlyphVisualBounds(i).getBounds2D();
            if (!glyphVB.isEmpty()) {
                if (result == null) {
                    result = glyphVB;
                } else {
                    Rectangle2D.union(result, glyphVB, result);
                }
            }
        }
        if (result == null) {
            result = new Rectangle2D.Float(0, 0, 0, 0);
        }
        return result;
    }

    // !!! not cached, assume TextLayout will cache if necessary
    // !!! fontStrike needs a method for this
    public Rectangle getPixelBounds(FontRenderContext renderFRC, float x, float y) {
      return getGlyphsPixelBounds(renderFRC, x, y, 0, glyphs.length);
    }

    public Shape getOutline() {
        return getGlyphsOutline(0, glyphs.length, 0, 0);
    }

    public Shape getOutline(float x, float y) {
        return getGlyphsOutline(0, glyphs.length, x, y);
    }

    // relative to gv origin
    public Shape getGlyphOutline(int ix) {
        return getGlyphsOutline(ix, 1, 0, 0);
    }

    // relative to gv origin offset by x, y
    public Shape getGlyphOutline(int ix, float x, float y) {
        return getGlyphsOutline(ix, 1, x, y);
    }

    public Point2D getGlyphPosition(int ix) {
        initPositions();

        ix *= 2;
        return new Point2D.Float(positions[ix], positions[ix + 1]);
    }

    public void setGlyphPosition(int ix, Point2D pos) {
        if (ix < 0 || ix > glyphs.length) {
            throw new IndexOutOfBoundsException("ix = " + ix);
        }

        initPositions();

        int ix2 = ix << 1;
        positions[ix2] = (float)pos.getX();
        positions[ix2 + 1] = (float)pos.getY();

        if (ix < glyphs.length) {
            clearCaches(ix);
        }
        addFlags(FLAG_HAS_POSITION_ADJUSTMENTS);
    }

    public AffineTransform getGlyphTransform(int ix) {
        if (ix < 0 || ix >= glyphs.length) {
            throw new IndexOutOfBoundsException("ix = " + ix);
        }
        if (gti != null) {
            return gti.getGlyphTransform(ix);
        }
        return null; // spec'd as returning null
    }

    public void setGlyphTransform(int ix, AffineTransform newTX) {
        if (ix < 0 || ix >= glyphs.length) {
            throw new IndexOutOfBoundsException("ix = " + ix);
        }

        if (gti == null) {
            if (newTX == null || newTX.isIdentity()) {
                return;
            }
            gti = new GlyphTransformInfo(this);
        }
        gti.setGlyphTransform(ix, newTX); // sets flags
        if (gti.transformCount() == 0) {
            gti = null;
        }
    }

    public int getLayoutFlags() {
        if (flags == UNINITIALIZED_FLAGS) {
            flags = 0;

            if (charIndices != null && glyphs.length > 1) {
                boolean ltr = true;
                boolean rtl = true;

                int rtlix = charIndices.length; // rtl index
                for (int i = 0; i < charIndices.length && (ltr || rtl); ++i) {
                    int cx = charIndices[i];

                    ltr = ltr && (cx == i);
                    rtl = rtl && (cx == --rtlix);
                }

                if (rtl) flags |= FLAG_RUN_RTL;
                if (!rtl && !ltr) flags |= FLAG_COMPLEX_GLYPHS;
            }
        }

        return flags;
    }

    public float[] getGlyphPositions(int start, int count, float[] result) {
        if (count < 0) {
            throw new IllegalArgumentException("count = " + count);
        }
        if (start < 0) {
            throw new IndexOutOfBoundsException("start = " + start);
        }
        if (start > glyphs.length + 1 - count) { // watch for overflow
            throw new IndexOutOfBoundsException("start + count = " + (start + count));
        }

        return internalGetGlyphPositions(start, count, 0, result);
    }

    public Shape getGlyphLogicalBounds(int ix) {
        if (ix < 0 || ix >= glyphs.length) {
            throw new IndexOutOfBoundsException("ix = " + ix);
        }

        Shape[] lbcache;
        if (lbcacheRef == null || (lbcache = lbcacheRef.get()) == null) {
            lbcache = new Shape[glyphs.length];
            lbcacheRef = new SoftReference<>(lbcache);
        }

        Shape result = lbcache[ix];
        if (result == null) {
            setFRCTX();
            initPositions();

            // !!! ought to return a rectangle2d for simple cases, though the following works for all

            // get the position, the tx offset, and the x,y advance and x,y adl.  The
            // shape is the box formed by adv (width) and adl (height) offset by
            // the position plus the tx offset minus the ascent.

            ADL adl = new ADL();
            GlyphStrike gs = getGlyphStrike(ix);
            gs.getADL(adl);

            Point2D.Float adv = gs.strike.getGlyphMetrics(glyphs[ix]);

            float wx = adv.x;
            float wy = adv.y;
            float hx = adl.descentX + adl.leadingX + adl.ascentX;
            float hy = adl.descentY + adl.leadingY + adl.ascentY;
            float x = positions[ix*2] + gs.dx - adl.ascentX;
            float y = positions[ix*2+1] + gs.dy - adl.ascentY;

            GeneralPath gp = new GeneralPath();
            gp.moveTo(x, y);
            gp.lineTo(x + wx, y + wy);
            gp.lineTo(x + wx + hx, y + wy + hy);
            gp.lineTo(x + hx, y + hy);
            gp.closePath();

            result = new DelegatingShape(gp);
            lbcache[ix] = result;
        }

        return result;
    }
    private SoftReference<Shape[]> lbcacheRef;

    public Shape getGlyphVisualBounds(int ix) {
        if (ix < 0 || ix >= glyphs.length) {
            throw new IndexOutOfBoundsException("ix = " + ix);
        }

        Shape[] vbcache;
        if (vbcacheRef == null || (vbcache = vbcacheRef.get()) == null) {
            vbcache = new Shape[glyphs.length];
            vbcacheRef = new SoftReference<>(vbcache);
        }

        Shape result = vbcache[ix];
        if (result == null) {
            result = new DelegatingShape(getGlyphOutlineBounds(ix));
            vbcache[ix] = result;
        }

        return result;
    }
    private SoftReference<Shape[]> vbcacheRef;

    public Rectangle getGlyphPixelBounds(int index, FontRenderContext renderFRC, float x, float y) {
      return getGlyphsPixelBounds(renderFRC, x, y, index, 1);
    }

    public GlyphMetrics getGlyphMetrics(int ix) {
        if (ix < 0 || ix >= glyphs.length) {
            throw new IndexOutOfBoundsException("ix = " + ix);
        }

        Rectangle2D vb = getGlyphVisualBounds(ix).getBounds2D();
        Point2D pt = getGlyphPosition(ix);
        vb.setRect(vb.getMinX() - pt.getX(),
                   vb.getMinY() - pt.getY(),
                   vb.getWidth(),
                   vb.getHeight());
        Point2D.Float adv =
            getGlyphStrike(ix).strike.getGlyphMetrics(glyphs[ix]);
        GlyphMetrics gm = new GlyphMetrics(true, adv.x, adv.y,
                                           vb,
                                           GlyphMetrics.STANDARD);
        return gm;
    }

    public GlyphJustificationInfo getGlyphJustificationInfo(int ix) {
        if (ix < 0 || ix >= glyphs.length) {
            throw new IndexOutOfBoundsException("ix = " + ix);
        }

        // currently we don't have enough information to do this right.  should
        // get info from the font and use real OT/GX justification.  Right now
        // sun/font/ExtendedTextSourceLabel assigns one of three infos
        // based on whether the char is kanji, space, or other.

        return null;
    }

    public boolean equals(GlyphVector rhs) {
        if (this == rhs) {
            return true;
        }
        if (rhs == null) {
            return false;
        }

        try {
            StandardGlyphVector other = (StandardGlyphVector)rhs;

            if (glyphs.length != other.glyphs.length) {
                return false;
            }

            for (int i = 0; i < glyphs.length; ++i) {
                if (glyphs[i] != other.glyphs[i]) {
                    return false;
                }
            }

            if (!font.equals(other.font)) {
                return false;
            }

            if (!frc.equals(other.frc)) {
                return false;
            }

            if ((other.positions == null) != (positions == null)) {
                if (positions == null) {
                    initPositions();
                } else {
                    other.initPositions();
                }
            }

            if (positions != null) {
                for (int i = 0; i < positions.length; ++i) {
                    if (positions[i] != other.positions[i]) {
                        return false;
                    }
                }
            }

            if (gti == null) {
                return other.gti == null;
            } else {
                return gti.equals(other.gti);
            }
        }
        catch (ClassCastException e) {
            // assume they are different simply by virtue of the class difference

            return false;
        }
    }

    /**
     * As a concrete subclass of Object that implements equality, this must
     * implement hashCode.
     */
    public int hashCode() {
        return font.hashCode() ^ glyphs.length;
    }

    /**
     * Since we implement equality comparisons for GlyphVector, we implement
     * the inherited Object.equals(Object) as well.  GlyphVector should do
     * this, and define two glyphvectors as not equal if the classes differ.
     */
    public boolean equals(Object rhs) {
        try {
            return equals((GlyphVector)rhs);
        }
        catch (ClassCastException e) {
            return false;
        }
    }

    /**
     * Sometimes I wish java had covariant return types...
     */
    public StandardGlyphVector copy() {
        return (StandardGlyphVector)clone();
    }

    /**
     * As a concrete subclass of GlyphVector, this must implement clone.
     */
    public Object clone() {
        // positions, gti are mutable so we have to clone them
        // font2d can be shared
        // fsref is a cache and can be shared
        try {
            StandardGlyphVector result = (StandardGlyphVector)super.clone();

            result.clearCaches();

            if (positions != null) {
                result.positions = positions.clone();
            }

            if (gti != null) {
                result.gti = new GlyphTransformInfo(result, gti);
            }

            return result;
        }
        catch (CloneNotSupportedException e) {
        }

        return this;
    }

    //////////////////////
    // StandardGlyphVector new public methods
    /////////////////////

    /*
     * Set a multiple glyph positions at one time.  GlyphVector only
     * provides API to set a single glyph at a time.
     */
    public void setGlyphPositions(float[] srcPositions, int srcStart,
                                  int start, int count) {
        if (count < 0) {
            throw new IllegalArgumentException("count = " + count);
        }

        initPositions();
        for (int i = start * 2, e = i + count * 2, p = srcStart; i < e; ++i, ++p) {
            positions[i] = srcPositions[p];
        }

        clearCaches();
        addFlags(FLAG_HAS_POSITION_ADJUSTMENTS);
    }

    /**
     * Set all the glyph positions, including the 'after last glyph' position.
     * The srcPositions array must be of length (numGlyphs + 1) * 2.
     */
    public void setGlyphPositions(float[] srcPositions) {
        int requiredLength = glyphs.length * 2 + 2;
        if (srcPositions.length != requiredLength) {
            throw new IllegalArgumentException("srcPositions.length != " + requiredLength);
        }

        positions = srcPositions.clone();

        clearCaches();
        addFlags(FLAG_HAS_POSITION_ADJUSTMENTS);
    }

    /**
     * This is a convenience overload that gets all the glyph positions, which
     * is what you usually want to do if you're getting more than one.
     * !!! should I bother taking result parameter?
     */
    public float[] getGlyphPositions(float[] result) {
        return internalGetGlyphPositions(0, glyphs.length + 1, 0, result);
    }

    /**
     * Get transform information for the requested range of glyphs.
     * If no glyphs have a transform, return null.
     * If a glyph has no transform (or is the identity transform) its entry in the result array will be null.
     * If the passed-in result is null an array will be allocated for the caller.
     * Each transform instance in the result array will unique, and independent of the GlyphVector's transform.
     */
    public AffineTransform[] getGlyphTransforms(int start, int count, AffineTransform[] result) {
        if (start < 0 || count < 0 || start + count > glyphs.length) {
            throw new IllegalArgumentException("start: " + start + " count: " + count);
        }

        if (gti == null) {
            return null;
        }

        if (result == null) {
            result = new AffineTransform[count];
        }

        for (int i = 0; i < count; ++i, ++start) {
            result[i] = gti.getGlyphTransform(start);
        }

        return result;
    }

    /**
     * Convenience overload for getGlyphTransforms(int, int, AffineTransform[], int);
     */
    public AffineTransform[] getGlyphTransforms() {
        return getGlyphTransforms(0, glyphs.length, null);
    }

    /**
     * Set a number of glyph transforms.
     * Original transforms are unchanged.  The array may contain nulls, and also may
     * contain multiple references to the same transform instance.
     */
    public void setGlyphTransforms(AffineTransform[] srcTransforms, int srcStart, int start, int count) {
        for (int i = start, e = start + count; i < e; ++i) {
            setGlyphTransform(i, srcTransforms[srcStart + i]);
        }
    }

    /**
     * Convenience overload of setGlyphTransforms(AffineTransform[], int, int, int).
     */
    public void setGlyphTransforms(AffineTransform[] srcTransforms) {
        setGlyphTransforms(srcTransforms, 0, 0, glyphs.length);
    }

    /**
     * For each glyph return posx, posy, advx, advy, visx, visy, visw, vish.
     */
    public float[] getGlyphInfo() {
        setFRCTX();
        initPositions();
        float[] result = new float[glyphs.length * 8];
        for (int i = 0, n = 0; i < glyphs.length; ++i, n += 8) {
            float x = positions[i*2];
            float y = positions[i*2+1];
            result[n] = x;
            result[n+1] = y;

            int glyphID = glyphs[i];
            GlyphStrike s = getGlyphStrike(i);
            Point2D.Float adv = s.strike.getGlyphMetrics(glyphID);
            result[n+2] = adv.x;
            result[n+3] = adv.y;

            Rectangle2D vb = getGlyphVisualBounds(i).getBounds2D();
            result[n+4] = (float)(vb.getMinX());
            result[n+5] = (float)(vb.getMinY());
            result[n+6] = (float)(vb.getWidth());
            result[n+7] = (float)(vb.getHeight());
        }
        return result;
    }

    //////////////////////
    // StandardGlyphVector package private methods
    /////////////////////

    // used by glyphlist to determine if it needs to allocate/size positions array
    // gti always uses positions because the gtx might have translation.  We also
    // need positions if the rendering dtx is different from the frctx.

    boolean needsPositions(double[] devTX) {
        return gti != null ||
            (getLayoutFlags() & FLAG_HAS_POSITION_ADJUSTMENTS) != 0 ||
            !matchTX(devTX, frctx);
    }

    // used by glyphList to get strong refs to font strikes for duration of rendering call
    // if devTX matches current devTX, we're ready to go
    // if we don't have multiple transforms, we're already ok

    // !!! I'm not sure fontInfo works so well for glyphvector, since we have to be able to handle
    // the multiple-strikes case

    /*
     * GlyphList calls this to set up its images data.  First it calls needsPositions,
     * passing the devTX, to see if it should provide us a positions array to fill.
     * It only doesn't need them if we're a simple glyph vector whose frctx matches the
     * devtx.
     * Then it calls setupGlyphImages.  If we need positions, we make sure we have our
     * default positions based on the frctx first. Then we set the devTX, and use
     * strikes based on it to generate the images.  Finally, we fill in the positions
     * array.
     * If we have transforms, we delegate to gti.  It depends on our having first
     * initialized the positions and devTX.
     */
    Object setupGlyphImages(long[] images, float[] positions, double[] devTX) {
        initPositions(); // FIRST ensure we have positions based on our frctx
        setRenderTransform(devTX); // THEN make sure we are using the desired devTX

        if (gti != null) {
            return gti.setupGlyphImages(images, positions, dtx);
        }

        GlyphStrike gs = getDefaultStrike();
        gs.strike.getGlyphImagePtrs(glyphs, images, glyphs.length);

        if (positions != null) {
            if (dtx.isIdentity()) {
                System.arraycopy(this.positions, 0, positions, 0, glyphs.length * 2);
            } else {
                dtx.transform(this.positions, 0, positions, 0, glyphs.length);
            }
        }

        return gs;
    }

    //////////////////////
    // StandardGlyphVector private methods
    /////////////////////

    // We keep translation in our frctx since getPixelBounds uses it.  But
    // GlyphList pulls out the translation and applies it separately, so
    // we strip it out when we set the dtx.  Basically nothing uses the
    // translation except getPixelBounds.

    // called by needsPositions, setRenderTransform
    private static boolean matchTX(double[] lhs, AffineTransform rhs) {
        return
            lhs[0] == rhs.getScaleX() &&
            lhs[1] == rhs.getShearY() &&
            lhs[2] == rhs.getShearX() &&
            lhs[3] == rhs.getScaleY();
    }

    // returns new tx if old one has translation, otherwise returns old one
    private static AffineTransform getNonTranslateTX(AffineTransform tx) {
        if (tx.getTranslateX() != 0 || tx.getTranslateY() != 0) {
            tx = new AffineTransform(tx.getScaleX(), tx.getShearY(),
                                     tx.getShearX(), tx.getScaleY(),
                                     0, 0);
        }
        return tx;
    }

    private static boolean equalNonTranslateTX(AffineTransform lhs, AffineTransform rhs) {
        return lhs.getScaleX() == rhs.getScaleX() &&
            lhs.getShearY() == rhs.getShearY() &&
            lhs.getShearX() == rhs.getShearX() &&
            lhs.getScaleY() == rhs.getScaleY();
    }

    // called by setupGlyphImages (after needsPositions, so redundant match check?)
    private void setRenderTransform(double[] devTX) {
        assert(devTX.length == 4);
        if (!matchTX(devTX, dtx)) {
            resetDTX(new AffineTransform(devTX)); // no translation since devTX len == 4.
        }
    }

    // called by getGlyphsPixelBounds
    private void setDTX(AffineTransform tx) {
        if (!equalNonTranslateTX(dtx, tx)) {
            resetDTX(getNonTranslateTX(tx));
        }
    }

    // called by most functions
    private void setFRCTX() {
        if (!equalNonTranslateTX(frctx, dtx)) {
            resetDTX(getNonTranslateTX(frctx));
        }
    }

    /**
     * Change the dtx for the strike refs we use.  Keeps a reference to the at.  At
     * must not contain translation.
     * Called by setRenderTransform, setDTX, initFontData.
     */
    private void resetDTX(AffineTransform at) {
        fsref = null;
        dtx = at;
        invdtx = null;
        if (!dtx.isIdentity()) {
            try {
                invdtx = dtx.createInverse();
            }
            catch (NoninvertibleTransformException e) {
                // we needn't care for rendering
            }
        }
        if (gti != null) {
            gti.strikesRef = null;
        }
    }

    /**
     * Utility used by getStandardGV.
     * Constructs a StandardGlyphVector from a generic glyph vector.
     * Do not call this from new contexts without considering the comment
     * about "userGlyphs".
     */
    private StandardGlyphVector(GlyphVector gv, FontRenderContext frc) {
        this.font = gv.getFont();
        this.frc = frc;
        initFontData();

        int nGlyphs = gv.getNumGlyphs();
        this.userGlyphs = gv.getGlyphCodes(0, nGlyphs, null);
        if (gv instanceof StandardGlyphVector) {
            /* userGlyphs will be OK because this is a private constructor
             * and the returned instance is used only for rendering.
             * It's not constructable by user code, nor returned to the
             * application. So we know "userGlyphs" are valid as having
             * been either already validated or are the result of layout.
             */
            this.glyphs = userGlyphs;
        } else {
            this.glyphs = getValidatedGlyphs(this.userGlyphs);
        }
        this.flags = gv.getLayoutFlags() & FLAG_MASK;

        if ((flags & FLAG_HAS_POSITION_ADJUSTMENTS) != 0) {
            this.positions = gv.getGlyphPositions(0, nGlyphs + 1, null);
        }

        if ((flags & FLAG_COMPLEX_GLYPHS) != 0) {
            this.charIndices = gv.getGlyphCharIndices(0, nGlyphs, null);
        }

        if ((flags & FLAG_HAS_TRANSFORMS) != 0) {
            AffineTransform[] txs = new AffineTransform[nGlyphs]; // worst case
            for (int i = 0; i < nGlyphs; ++i) {
                txs[i] = gv.getGlyphTransform(i); // gv doesn't have getGlyphsTransforms
            }

            setGlyphTransforms(txs);
        }
    }

    /* Before asking the Font we see if the glyph code is
     * FFFE or FFFF which are special values that we should be internally
     * ready to handle as meaning invisible glyphs. The Font would report
     * those as the missing glyph.
     */
    int[] getValidatedGlyphs(int[] oglyphs) {
        int len = oglyphs.length;
        int[] vglyphs = new int[len];
        for (int i=0; i<len; i++) {
            if (oglyphs[i] == 0xFFFE || oglyphs[i] == 0xFFFF) {
                vglyphs[i] = oglyphs[i];
            } else {
                vglyphs[i] = font2D.getValidatedGlyphCode(oglyphs[i]);
            }
        }
        return vglyphs;
    }

    // utility used by constructors
    private void init(Font font, char[] text, int start, int count,
                      FontRenderContext frc, int flags) {

        if (start < 0 || count < 0 || start + count > text.length) {
            throw new ArrayIndexOutOfBoundsException("start or count out of bounds");
        }

        this.font = font;
        this.frc = frc;
        this.flags = flags;

        if (getTracking(font) != 0) {
            addFlags(FLAG_HAS_POSITION_ADJUSTMENTS);
        }

        // !!! change mapper interface?
        if (start != 0) {
            char[] temp = new char[count];
            System.arraycopy(text, start, temp, 0, count);
            text = temp;
        }

        initFontData(); // sets up font2D

        // !!! no layout for now, should add checks
        // !!! need to support creating a StandardGlyphVector from a TextMeasurer's info...
        glyphs = new int[count]; // hmmm
        /* Glyphs obtained here are already validated by the font */
        userGlyphs = glyphs;
        font2D.getMapper().charsToGlyphs(count, text, glyphs);
    }

    private void initFontData() {
        font2D = FontUtilities.getFont2D(font);
        if (font2D instanceof FontSubstitution) {
           font2D = ((FontSubstitution)font2D).getCompositeFont2D();
        }
        float s = font.getSize2D();
        if (font.isTransformed()) {
            ftx = font.getTransform();
            if (ftx.getTranslateX() != 0 || ftx.getTranslateY() != 0) {
                addFlags(FLAG_HAS_POSITION_ADJUSTMENTS);
            }
            ftx.setTransform(ftx.getScaleX(), ftx.getShearY(), ftx.getShearX(), ftx.getScaleY(), 0, 0);
            ftx.scale(s, s);
        } else {
            ftx = AffineTransform.getScaleInstance(s, s);
        }

        frctx = frc.getTransform();
        resetDTX(getNonTranslateTX(frctx));
    }

    /**
     * Copy glyph position data into a result array starting at the indicated
     * offset in the array.  If the passed-in result array is null, a new
     * array will be allocated and returned.
     *
     * This is an internal method and does no extra argument checking.
     *
     * @param start the index of the first glyph to get
     * @param count the number of glyphs to get
     * @param offset the offset into result at which to put the data
     * @param result an array to hold the x,y positions
     * @return the modified position array
     */
    private float[] internalGetGlyphPositions(int start, int count, int offset, float[] result) {
        if (result == null) {
            result = new float[offset + count * 2];
        }

        initPositions();

        // System.arraycopy is slow for stuff like this
        for (int i = offset, e = offset + count * 2, p = start * 2; i < e; ++i, ++p) {
            result[i] = positions[p];
        }

        return result;
    }

    private Rectangle2D getGlyphOutlineBounds(int ix) {
        setFRCTX();
        initPositions();
        return getGlyphStrike(ix).getGlyphOutlineBounds(glyphs[ix], positions[ix*2], positions[ix*2+1]);
    }

    /**
     * Used by getOutline, getGlyphsOutline
     */
    private Shape getGlyphsOutline(int start, int count, float x, float y) {
        setFRCTX();
        initPositions();

        GeneralPath result = new GeneralPath(GeneralPath.WIND_NON_ZERO);
        for (int i = start, e = start + count, n = start * 2; i < e; ++i, n += 2) {
            float px = x + positions[n];
            float py = y + positions[n+1];

            getGlyphStrike(i).appendGlyphOutline(glyphs[i], result, px, py);
        }

        return result;
    }

    private Rectangle getGlyphsPixelBounds(FontRenderContext frc, float x, float y, int start, int count) {
        initPositions(); // FIRST ensure we have positions based on our frctx

        AffineTransform tx = null;
        if (frc == null || frc.equals(this.frc)) {
            tx = frctx;
        } else {
            tx = frc.getTransform();
        }
        setDTX(tx); // need to get the right strikes, but we use tx itself to translate the points

        if (gti != null) {
            return gti.getGlyphsPixelBounds(tx, x, y, start, count);
        }

        FontStrike fs = getDefaultStrike().strike;
        Rectangle result = null;
        Rectangle r = new Rectangle();
        Point2D.Float pt = new Point.Float();
        int n = start * 2;
        while (--count >= 0) {
            pt.x = x + positions[n++];
            pt.y = y + positions[n++];
            tx.transform(pt, pt);
            fs.getGlyphImageBounds(glyphs[start++], pt, r);
            if (!r.isEmpty()) {
                if (result == null) {
                    result = new Rectangle(r);
                } else {
                    result.add(r);
                }
            }
        }
        return result != null ? result : r;
    }

    private void clearCaches(int ix) {
        if (lbcacheRef != null) {
            Shape[] lbcache = lbcacheRef.get();
            if (lbcache != null) {
                lbcache[ix] = null;
            }
        }

        if (vbcacheRef != null) {
            Shape[] vbcache = vbcacheRef.get();
            if (vbcache != null) {
                vbcache[ix] = null;
            }
        }
    }

    private void clearCaches() {
        lbcacheRef = null;
        vbcacheRef = null;
    }

    // internal use only for possible future extension

    /**
     * A flag used with getLayoutFlags that indicates whether this {@code GlyphVector} uses
     * a vertical baseline.
     */
    public static final int FLAG_USES_VERTICAL_BASELINE = 128;

    /**
     * A flag used with getLayoutFlags that indicates whether this {@code GlyphVector} uses
     * vertical glyph metrics.  A {@code GlyphVector} can use vertical metrics on a
     * horizontal line, or vice versa.
     */
    public static final int FLAG_USES_VERTICAL_METRICS = 256;

    /**
     * A flag used with getLayoutFlags that indicates whether this {@code GlyphVector} uses
     * the 'alternate orientation.'  Glyphs have a default orientation given a
     * particular baseline and metrics orientation, this is the orientation appropriate
     * for left-to-right text.  For example, the letter 'A' can have four orientations,
     * with the point at 12, 3, 6, or 9 'o clock.  The following table shows where the
     * point displays for different values of vertical baseline (vb), vertical
     * metrics (vm) and alternate orientation (fo):<br>
     * <blockquote>
     * vb vm ao
     * -- -- --  --
     *  f  f  f  12   ^  horizontal metrics on horizontal lines
     *  f  f  t   6   v
     *  f  t  f   9   <  vertical metrics on horizontal lines
     *  f  t  t   3   >
     *  t  f  f   3   >  horizontal metrics on vertical lines
     *  t  f  t   9   <
     *  t  t  f  12   ^  vertical metrics on vertical lines
     *  t  t  t   6   v
     * </blockquote>
     */
    public static final int FLAG_USES_ALTERNATE_ORIENTATION = 512;


    /**
     * Ensure that the positions array exists and holds position data.
     * If the array is null, this allocates it and sets default positions.
     */
    private void initPositions() {
        if (positions == null) {
            setFRCTX();

            positions = new float[glyphs.length * 2 + 2];

            Point2D.Float trackPt = null;
            float track = getTracking(font);
            if (track != 0) {
                track *= font.getSize2D();
                trackPt = new Point2D.Float(track, 0); // advance delta
            }

            Point2D.Float pt = new Point2D.Float(0, 0);
            if (font.isTransformed()) {
                AffineTransform at = font.getTransform();
                at.transform(pt, pt);
                positions[0] = pt.x;
                positions[1] = pt.y;

                if (trackPt != null) {
                    at.deltaTransform(trackPt, trackPt);
                }
            }
            for (int i = 0, n = 2; i < glyphs.length; ++i, n += 2) {
                getGlyphStrike(i).addDefaultGlyphAdvance(glyphs[i], pt);
                if (trackPt != null) {
                    pt.x += trackPt.x;
                    pt.y += trackPt.y;
                }
                positions[n] = pt.x;
                positions[n+1] = pt.y;
            }
        }
    }

    /**
     * OR newFlags with existing flags.  First computes existing flags if needed.
     */
    private void addFlags(int newflags) {
        flags = getLayoutFlags() | newflags;
    }

    /**
     * AND the complement of clearedFlags with existing flags.  First computes existing flags if needed.
     */
    private void clearFlags(int clearedFlags) {
        flags = getLayoutFlags() & ~clearedFlags;
    }

    // general utility methods

    // encapsulate the test to check whether we have per-glyph transforms
    private GlyphStrike getGlyphStrike(int ix) {
        if (gti == null) {
            return getDefaultStrike();
        } else {
            return gti.getStrike(ix);
        }
    }

    // encapsulate access to cached default glyph strike
    private GlyphStrike getDefaultStrike() {
        GlyphStrike gs = null;
        if (fsref != null) {
            gs = fsref.get();
        }
        if (gs == null) {
            gs = GlyphStrike.create(this, dtx, null);
            fsref = new SoftReference<>(gs);
        }
        return gs;
    }


    /////////////////////
    // Internal utility classes
    /////////////////////

    // !!! I have this as a separate class instead of just inside SGV,
    // but I previously didn't bother.  Now I'm trying this again.
    // Probably still not worth it, but I'd like to keep sgv's small in the common case.

    static final class GlyphTransformInfo {
        StandardGlyphVector sgv;  // reference back to glyph vector - yuck
        int[] indices;            // index into unique strikes
        double[] transforms;      // six doubles per unique transform, because AT is a pain to manipulate
        SoftReference<GlyphStrike[]> strikesRef; // ref to unique strikes, one per transform
        boolean haveAllStrikes;   // true if the strike array has been filled by getStrikes().

        // used when first setting a transform
        GlyphTransformInfo(StandardGlyphVector sgv) {
            this.sgv = sgv;
        }

        // used when cloning a glyph vector, need to set back link
        GlyphTransformInfo(StandardGlyphVector sgv, GlyphTransformInfo rhs) {
            this.sgv = sgv;

            this.indices = rhs.indices == null ? null : rhs.indices.clone();
            this.transforms = rhs.transforms == null ? null : rhs.transforms.clone();
            this.strikesRef = null; // can't share cache, so rather than clone, we just null out
        }

        // used in sgv equality
        public boolean equals(GlyphTransformInfo rhs) {
            if (rhs == null) {
                return false;
            }
            if (rhs == this) {
                return true;
            }
            if (this.indices.length != rhs.indices.length) {
                return false;
            }
            if (this.transforms.length != rhs.transforms.length) {
                return false;
            }

            // slow since we end up processing the same transforms multiple
            // times, but since transforms can be in any order, we either do
            // this or create a mapping.  Equality tests aren't common so
            // leave it like this.
            for (int i = 0; i < this.indices.length; ++i) {
                int tix = this.indices[i];
                int rix = rhs.indices[i];
                if ((tix == 0) != (rix == 0)) {
                    return false;
                }
                if (tix != 0) {
                    tix *= 6;
                    rix *= 6;
                    for (int j = 6; j > 0; --j) {
                        if (this.indices[--tix] != rhs.indices[--rix]) {
                            return false;
                        }
                    }
                }
            }
            return true;
        }

        // implements sgv.setGlyphTransform
        void setGlyphTransform(int glyphIndex, AffineTransform newTX) {

            // we store all the glyph transforms as a double array, and for each glyph there
            // is an entry in the txIndices array indicating which transform to use.  0 means
            // there's no transform, 1 means use the first transform (the 6 doubles at offset
            // 0), 2 means use the second transform (the 6 doubles at offset 6), etc.
            //
            // Since this can be called multiple times, and since the number of transforms
            // affects the time it takes to construct the glyphs, we try to keep the arrays as
            // compact as possible, by removing transforms that are no longer used, and reusing
            // transforms where we already have them.

            double[] temp = new double[6];
            boolean isIdentity = true;
            if (newTX == null || newTX.isIdentity()) {
                // Fill in temp
                temp[0] = temp[3] = 1.0;
            }
            else {
                isIdentity = false;
                newTX.getMatrix(temp);
            }

            if (indices == null) {
                if (isIdentity) { // no change
                    return;
                }

                indices = new int[sgv.glyphs.length];
                indices[glyphIndex] = 1;
                transforms = temp;
            } else {
                boolean addSlot = false; // assume we're not growing
                int newIndex = -1;
                if (isIdentity) {
                    newIndex = 0; // might shrink
                } else {
                    addSlot = true; // assume no match
                    int i;
                loop:
                    for (i = 0; i < transforms.length; i += 6) {
                        for (int j = 0; j < 6; ++j) {
                            if (transforms[i + j] != temp[j]) {
                                continue loop;
                            }
                        }
                        addSlot = false;
                        break;
                    }
                    newIndex = i / 6 + 1; // if no match, end of list
                }

                // if we're using the same transform, nothing to do
                int oldIndex = indices[glyphIndex];
                if (newIndex != oldIndex) {
                    // see if we are removing last use of the old slot
                    boolean removeSlot = false;
                    if (oldIndex != 0) {
                        removeSlot = true;
                        for (int i = 0; i < indices.length; ++i) {
                            if (indices[i] == oldIndex && i != glyphIndex) {
                                removeSlot = false;
                                break;
                            }
                        }
                    }

                    if (removeSlot && addSlot) { // reuse old slot with new transform
                        newIndex = oldIndex;
                        System.arraycopy(temp, 0, transforms, (newIndex - 1) * 6, 6);
                    } else if (removeSlot) {
                        if (transforms.length == 6) { // removing last one, so clear arrays
                            indices = null;
                            transforms = null;

                            sgv.clearCaches(glyphIndex);
                            sgv.clearFlags(FLAG_HAS_TRANSFORMS);
                            strikesRef = null;

                            return;
                        }

                        double[] ttemp = new double[transforms.length - 6];
                        System.arraycopy(transforms, 0, ttemp, 0, (oldIndex - 1) * 6);
                        System.arraycopy(transforms, oldIndex * 6, ttemp, (oldIndex - 1) * 6,
                                         transforms.length - oldIndex * 6);
                        transforms = ttemp;

                        // clean up indices
                        for (int i = 0; i < indices.length; ++i) {
                            if (indices[i] > oldIndex) { // ignore == oldIndex, it's going away
                                indices[i] -= 1;
                            }
                        }
                        if (newIndex > oldIndex) { // don't forget to decrement this too if we need to
                            --newIndex;
                        }
                    } else if (addSlot) {
                        double[] ttemp = new double[transforms.length + 6];
                        System.arraycopy(transforms, 0, ttemp, 0, transforms.length);
                        System.arraycopy(temp, 0, ttemp, transforms.length, 6);
                        transforms = ttemp;
                    }

                    indices[glyphIndex] = newIndex;
                }
            }

            sgv.clearCaches(glyphIndex);
            sgv.addFlags(FLAG_HAS_TRANSFORMS);
            strikesRef = null;
        }

        // implements sgv.getGlyphTransform
        AffineTransform getGlyphTransform(int ix) {
            int index = indices[ix];
            if (index == 0) {
                return null;
            }

            int x = (index - 1) * 6;
            return new AffineTransform(transforms[x + 0],
                                       transforms[x + 1],
                                       transforms[x + 2],
                                       transforms[x + 3],
                                       transforms[x + 4],
                                       transforms[x + 5]);
        }

        int transformCount() {
            if (transforms == null) {
                return 0;
            }
            return transforms.length / 6;
        }

        /**
         * The strike cache works like this.
         *
         * -Each glyph is thought of as having a transform, usually identity.
         * -Each request for a strike is based on a device transform, either the
         * one in the frc or the rendering transform.
         * -For general info, strikes are held with soft references.
         * -When rendering, strikes must be held with hard references for the
         * duration of the rendering call.  GlyphList will have to hold this
         * info along with the image and position info, but toss the strike info
         * when done.
         * -Build the strike cache as needed.  If the dev transform we want to use
         * has changed from the last time it is built, the cache is flushed by
         * the caller before these methods are called.
         *
         * Use a tx that doesn't include translation components of dst tx.
         */
        Object setupGlyphImages(long[] images, float[] positions, AffineTransform tx) {
            int len = sgv.glyphs.length;

            GlyphStrike[] sl = getAllStrikes();
            for (int i = 0; i < len; ++i) {
                GlyphStrike gs = sl[indices[i]];
                int glyphID = sgv.glyphs[i];
                images[i] = gs.strike.getGlyphImagePtr(glyphID);

                gs.getGlyphPosition(glyphID, i*2, sgv.positions, positions);
            }
            tx.transform(positions, 0, positions, 0, len);

            return sl;
        }

        Rectangle getGlyphsPixelBounds(AffineTransform tx, float x, float y, int start, int count) {
            Rectangle result = null;
            Rectangle r = new Rectangle();
            Point2D.Float pt = new Point.Float();
            int n = start * 2;
            while (--count >= 0) {
                GlyphStrike gs = getStrike(start);
                pt.x = x + sgv.positions[n++] + gs.dx;
                pt.y = y + sgv.positions[n++] + gs.dy;
                tx.transform(pt, pt);
                gs.strike.getGlyphImageBounds(sgv.glyphs[start++], pt, r);
                if (!r.isEmpty()) {
                    if (result == null) {
                        result = new Rectangle(r);
                    } else {
                        result.add(r);
                    }
                }
            }
            return result != null ? result : r;
        }

        GlyphStrike getStrike(int glyphIndex) {
            if (indices != null) {
                GlyphStrike[] strikes = getStrikeArray();
                return getStrikeAtIndex(strikes, indices[glyphIndex]);
            }
            return sgv.getDefaultStrike();
        }

        private GlyphStrike[] getAllStrikes() {
            if (indices == null) {
                return null;
            }

            GlyphStrike[] strikes = getStrikeArray();
            if (!haveAllStrikes) {
                for (int i = 0; i < strikes.length; ++i) {
                    getStrikeAtIndex(strikes, i);
                }
                haveAllStrikes = true;
            }

            return strikes;
        }

        private GlyphStrike[] getStrikeArray() {
            GlyphStrike[] strikes = null;
            if (strikesRef != null) {
                strikes = strikesRef.get();
            }
            if (strikes == null) {
                haveAllStrikes = false;
                strikes = new GlyphStrike[transformCount() + 1];
                strikesRef = new SoftReference<>(strikes);
            }

            return strikes;
        }

        private GlyphStrike getStrikeAtIndex(GlyphStrike[] strikes, int strikeIndex) {
            GlyphStrike strike = strikes[strikeIndex];
            if (strike == null) {
                if (strikeIndex == 0) {
                    strike = sgv.getDefaultStrike();
                } else {
                    int ix = (strikeIndex - 1) * 6;
                    AffineTransform gtx = new AffineTransform(transforms[ix],
                                                              transforms[ix+1],
                                                              transforms[ix+2],
                                                              transforms[ix+3],
                                                              transforms[ix+4],
                                                              transforms[ix+5]);

                    strike = GlyphStrike.create(sgv, sgv.dtx, gtx);
                }
                strikes[strikeIndex] = strike;
            }
            return strike;
        }
    }

    // This adjusts the metrics by the translation components of the glyph
    // transform.  It is done here since the translation is not known by the
    // strike.
    // It adjusts the position of the image and the advance.

    public static final class GlyphStrike {
        StandardGlyphVector sgv;
        FontStrike strike; // hard reference
        float dx;
        float dy;

        static GlyphStrike create(StandardGlyphVector sgv, AffineTransform dtx, AffineTransform gtx) {
            float dx = 0;
            float dy = 0;

            AffineTransform tx = sgv.ftx;
            if (!dtx.isIdentity() || gtx != null) {
                tx = new AffineTransform(sgv.ftx);
                if (gtx != null) {
                    tx.preConcatenate(gtx);
                    dx = (float)tx.getTranslateX(); // uses ftx then gtx to get translation
                    dy = (float)tx.getTranslateY();
                }
                if (!dtx.isIdentity()) {
                    tx.preConcatenate(dtx);
                }
            }

            int ptSize = 1; // only matters for 'gasp' case.
            Object aaHint = sgv.frc.getAntiAliasingHint();
            if (aaHint == VALUE_TEXT_ANTIALIAS_GASP) {
                /* Must pass in the calculated point size for rendering.
                 * If the glyph tx is anything other than identity or a
                 *  simple translate, calculate the transformed point size.
                 */
                if (!tx.isIdentity() &&
                    (tx.getType() & ~AffineTransform.TYPE_TRANSLATION) != 0) {
                    double shearx = tx.getShearX();
                    if (shearx != 0) {
                        double scaley = tx.getScaleY();
                        ptSize =
                            (int)Math.sqrt(shearx * shearx + scaley * scaley);
                    } else {
                        ptSize = (int)(Math.abs(tx.getScaleY()));
                    }
                }
            }
            int aa = FontStrikeDesc.getAAHintIntVal(aaHint,sgv.font2D, ptSize);
            int fm = FontStrikeDesc.getFMHintIntVal
                (sgv.frc.getFractionalMetricsHint());
            FontStrikeDesc desc = new FontStrikeDesc(dtx,
                                                     tx,
                                                     sgv.font.getStyle(),
                                                     aa, fm);
            // Get the strike via the handle. Shouldn't matter
            // if we've invalidated the font but its an extra precaution.
        // do we want the CompFont from CFont here ?
        Font2D f2d = sgv.font2D;
        if (f2d instanceof FontSubstitution) {
           f2d = ((FontSubstitution)f2d).getCompositeFont2D();
        }
            FontStrike strike = f2d.handle.font2D.getStrike(desc);  // !!! getStrike(desc, false)

            return new GlyphStrike(sgv, strike, dx, dy);
        }

        private GlyphStrike(StandardGlyphVector sgv, FontStrike strike, float dx, float dy) {
            this.sgv = sgv;
            this.strike = strike;
            this.dx = dx;
            this.dy = dy;
        }

        void getADL(ADL result) {
            StrikeMetrics sm = strike.getFontMetrics();
            Point2D.Float delta = null;
            if (sgv.font.isTransformed()) {
                delta = new Point2D.Float();
                delta.x = (float)sgv.font.getTransform().getTranslateX();
                delta.y = (float)sgv.font.getTransform().getTranslateY();
            }

            result.ascentX = -sm.ascentX;
            result.ascentY = -sm.ascentY;
            result.descentX = sm.descentX;
            result.descentY = sm.descentY;
            result.leadingX = sm.leadingX;
            result.leadingY = sm.leadingY;
        }

        void getGlyphPosition(int glyphID, int ix, float[] positions, float[] result) {
            result[ix] = positions[ix] + dx;
            ++ix;
            result[ix] = positions[ix] + dy;
        }

        void addDefaultGlyphAdvance(int glyphID, Point2D.Float result) {
            // !!! change this API?  Creates unnecessary garbage.  Also the name doesn't quite fit.
            // strike.addGlyphAdvance(Point2D.Float adv);  // hey, whaddya know, matches my api :-)
            Point2D.Float adv = strike.getGlyphMetrics(glyphID);
            result.x += adv.x + dx;
            result.y += adv.y + dy;
        }

        Rectangle2D getGlyphOutlineBounds(int glyphID, float x, float y) {
            Rectangle2D result = null;
            if (sgv.invdtx == null) {
                result = new Rectangle2D.Float();
                result.setRect(strike.getGlyphOutlineBounds(glyphID)); // don't mutate cached rect
            } else {
                GeneralPath gp = strike.getGlyphOutline(glyphID, 0, 0);
                gp.transform(sgv.invdtx);
                result = gp.getBounds2D();
            }
            /* Since x is the logical advance of the glyph to this point.
             * Because of the way that Rectangle.union is specified, this
             * means that subsequent unioning of a rect including that
             * will be affected, even if the glyph is empty. So skip such
             * cases. This alone isn't a complete solution since x==0
             * may also not be what is wanted. The code that does the
             * unioning also needs to be aware to ignore empty glyphs.
             */
            if (!result.isEmpty()) {
                result.setRect(result.getMinX() + x + dx,
                               result.getMinY() + y + dy,
                               result.getWidth(), result.getHeight());
            }
            return result;
        }

        void appendGlyphOutline(int glyphID, GeneralPath result, float x, float y) {
            // !!! fontStrike needs a method for this.  For that matter, GeneralPath does.
            GeneralPath gp = null;
            if (sgv.invdtx == null) {
                gp = strike.getGlyphOutline(glyphID, x + dx, y + dy);
            } else {
                gp = strike.getGlyphOutline(glyphID, 0, 0);
                gp.transform(sgv.invdtx);
                gp.transform(AffineTransform.getTranslateInstance(x + dx, y + dy));
            }
            PathIterator iterator = gp.getPathIterator(null);
            result.append(iterator, false);
        }
    }

    public String toString() {
        return appendString(null).toString();
    }

    StringBuffer appendString(StringBuffer buf) {
        if (buf == null) {
            buf = new StringBuffer();
        }
        try {
            buf.append("SGV{font: ");
            buf.append(font.toString());
            buf.append(", frc: ");
            buf.append(frc.toString());
            buf.append(", glyphs: (");
            buf.append(glyphs.length);
            buf.append(")[");
            for (int i = 0; i < glyphs.length; ++i) {
                if (i > 0) {
                    buf.append(", ");
                }
                buf.append(Integer.toHexString(glyphs[i]));
            }
            buf.append("]");
            if (positions != null) {
                buf.append(", positions: (");
                buf.append(positions.length);
                buf.append(")[");
                for (int i = 0; i < positions.length; i += 2) {
                    if (i > 0) {
                        buf.append(", ");
                    }
                    buf.append(positions[i]);
                    buf.append("@");
                    buf.append(positions[i+1]);
                }
                buf.append("]");
            }
            if (charIndices != null) {
                buf.append(", indices: (");
                buf.append(charIndices.length);
                buf.append(")[");
                for (int i = 0; i < charIndices.length; ++i) {
                    if (i > 0) {
                        buf.append(", ");
                    }
                    buf.append(charIndices[i]);
                }
                buf.append("]");
            }
            buf.append(", flags:");
            if (getLayoutFlags() == 0) {
                buf.append(" default");
            } else {
                if ((flags & FLAG_HAS_TRANSFORMS) != 0) {
                    buf.append(" tx");
                }
                if ((flags & FLAG_HAS_POSITION_ADJUSTMENTS) != 0) {
                    buf.append(" pos");
                }
                if ((flags & FLAG_RUN_RTL) != 0) {
                    buf.append(" rtl");
                }
                if ((flags & FLAG_COMPLEX_GLYPHS) != 0) {
                    buf.append(" complex");
                }
            }
        }
        catch(Exception e) {
            buf.append(' ').append(e.getMessage());
        }
        buf.append('}');

        return buf;
    }

    static class ADL {
        public float ascentX;
        public float ascentY;
        public float descentX;
        public float descentY;
        public float leadingX;
        public float leadingY;

        public String toString() {
            return toStringBuffer(null).toString();
        }

        protected StringBuffer toStringBuffer(StringBuffer result) {
            if (result == null) {
                result = new StringBuffer();
            }
            result.append("ax: ");
            result.append(ascentX);
            result.append(" ay: ");
            result.append(ascentY);
            result.append(" dx: ");
            result.append(descentX);
            result.append(" dy: ");
            result.append(descentY);
            result.append(" lx: ");
            result.append(leadingX);
            result.append(" ly: ");
            result.append(leadingY);

            return result;
        }
    }
}
