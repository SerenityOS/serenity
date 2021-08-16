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
/*
 *
 * (C) Copyright IBM Corp. 1998-2003 - All Rights Reserved
 */

package sun.font;

import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;

import java.awt.font.FontRenderContext;
import java.awt.font.GlyphJustificationInfo;
import java.awt.font.GlyphMetrics;
import java.awt.font.LineMetrics;
import java.awt.font.TextAttribute;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import java.util.Map;

/**
 * Default implementation of ExtendedTextLabel.
 */

// {jbr} I made this class package-private to keep the
// Decoration.Label API package-private.

/* public */
class ExtendedTextSourceLabel extends ExtendedTextLabel implements Decoration.Label {

  TextSource source;
  private Decoration decorator;

  // caches
  private Font font;
  private AffineTransform baseTX;
  private CoreMetrics cm;

  Rectangle2D lb;
  Rectangle2D ab;
  Rectangle2D vb;
  Rectangle2D ib;
  StandardGlyphVector gv;
  float[] charinfo;

  /**
   * Create from a TextSource.
   */
  public ExtendedTextSourceLabel(TextSource source, Decoration decorator) {
    this.source = source;
    this.decorator = decorator;
    finishInit();
  }

  /**
   * Create from a TextSource, optionally using cached data from oldLabel starting at the offset.
   * If present oldLabel must have been created from a run of text that includes the text used in
   * the new label.  Start in source corresponds to logical character offset in oldLabel.
   */
  public ExtendedTextSourceLabel(TextSource source, ExtendedTextSourceLabel oldLabel, int offset) {
    // currently no optimization.
    this.source = source;
    this.decorator = oldLabel.decorator;
    finishInit();
  }

  private void finishInit() {
    font = source.getFont();

    Map<TextAttribute, ?> atts = font.getAttributes();
    baseTX = AttributeValues.getBaselineTransform(atts);
    if (baseTX == null){
        cm = source.getCoreMetrics();
    } else {
      AffineTransform charTX = AttributeValues.getCharTransform(atts);
      if (charTX == null) {
          charTX = new AffineTransform();
      }
      font = font.deriveFont(charTX);

      LineMetrics lm = font.getLineMetrics(source.getChars(), source.getStart(),
          source.getStart() + source.getLength(), source.getFRC());
      cm = CoreMetrics.get(lm);
    }
  }


  // TextLabel API

  public Rectangle2D getLogicalBounds() {
    return getLogicalBounds(0, 0);
  }

  public Rectangle2D getLogicalBounds(float x, float y) {
    if (lb == null) {
      lb = createLogicalBounds();
    }
    return new Rectangle2D.Float((float)(lb.getX() + x),
                                 (float)(lb.getY() + y),
                                 (float)lb.getWidth(),
                                 (float)lb.getHeight());
  }

    public float getAdvance() {
        if (lb == null) {
            lb = createLogicalBounds();
        }
        return (float)lb.getWidth();
    }

  public Rectangle2D getVisualBounds(float x, float y) {
    if (vb == null) {
      vb = decorator.getVisualBounds(this);
    }
    return new Rectangle2D.Float((float)(vb.getX() + x),
                                 (float)(vb.getY() + y),
                                 (float)vb.getWidth(),
                                 (float)vb.getHeight());
  }

  public Rectangle2D getAlignBounds(float x, float y) {
    if (ab == null) {
      ab = createAlignBounds();
    }
    return new Rectangle2D.Float((float)(ab.getX() + x),
                                 (float)(ab.getY() + y),
                                 (float)ab.getWidth(),
                                 (float)ab.getHeight());

  }

  public Rectangle2D getItalicBounds(float x, float y) {
    if (ib == null) {
      ib = createItalicBounds();
    }
    return new Rectangle2D.Float((float)(ib.getX() + x),
                                 (float)(ib.getY() + y),
                                 (float)ib.getWidth(),
                                 (float)ib.getHeight());

  }

  public Rectangle getPixelBounds(FontRenderContext frc, float x, float y) {
      return getGV().getPixelBounds(frc, x, y);
  }

  public boolean isSimple() {
      return decorator == Decoration.getPlainDecoration() &&
             baseTX == null;
  }

  public AffineTransform getBaselineTransform() {
      return baseTX; // passing internal object, caller must not modify!
  }

  public Shape handleGetOutline(float x, float y) {
    return getGV().getOutline(x, y);
  }

  public Shape getOutline(float x, float y) {
    return decorator.getOutline(this, x, y);
  }

  public void handleDraw(Graphics2D g, float x, float y) {
    g.drawGlyphVector(getGV(), x, y);
  }

  public void draw(Graphics2D g, float x, float y) {
    decorator.drawTextAndDecorations(this, g, x, y);
  }

  /**
   * The logical bounds extends from the origin of the glyphvector to the
   * position at which a following glyphvector's origin should be placed.
   * We always assume glyph vectors are rendered from left to right, so
   * the origin is always to the left.
   * <p> On a left-to-right run, combining marks and 'ligatured away'
   * characters are to the right of their base characters.  The charinfo
   * array will record the character positions for these 'missing' characters
   * as being at the origin+advance of the base glyph, with zero advance.
   * (This is not necessarily the same as the glyph position, for example,
   * an umlaut glyph may have a position to the left of this point, it depends
   * on whether the font was designed so that such glyphs overhang to the left
   * of their origin, or whether it presumes some kind of kerning to position
   * the glyphs).  Anyway, the left of the bounds is the origin of the first
   * logical (leftmost) character, and the right is the origin + advance of the
   * last logical (rightmost) character.
   * <p> On a right-to-left run, these special characters are to the left
   * of their base characters.  Again, since 'glyph position' has been abstracted
   * away, we can use the origin of the leftmost character, and the origin +
   * advance of the rightmost character.
   * <p> On a mixed run (hindi) we can't rely on the first logical character
   * being the leftmost character.  However we can again rely on the leftmost
   * character origin and the rightmost character + advance.
   */
  protected Rectangle2D createLogicalBounds() {
    return getGV().getLogicalBounds();
  }

  public Rectangle2D handleGetVisualBounds() {
    return getGV().getVisualBounds();
  }

  /**
   * Like createLogicalBounds except ignore leading and logically trailing white space.
   * this assumes logically trailing whitespace is also visually trailing.
   * Whitespace is anything that has a zero visual width, regardless of its advance.
   * <p> We make the same simplifying assumptions as in createLogicalBounds, namely
   * that we can rely on the charinfo to shield us from any glyph positioning oddities
   * in the font that place the glyph for a character at other than the pos + advance
   * of the character to its left.  So we no longer need to skip chars with zero
   * advance, as their bounds (right and left) are already correct.
   */
  protected Rectangle2D createAlignBounds() {
    float[] info = getCharinfo();

    float al = 0f;
    float at = -cm.ascent;
    float aw = 0f;
    float ah = cm.ascent + cm.descent;

    if (charinfo == null || charinfo.length == 0) {
        return new Rectangle2D.Float(al, at, aw, ah);
    }

    boolean lineIsLTR = (source.getLayoutFlags() & 0x8) == 0;
    int rn = info.length - numvals;
    if (lineIsLTR) {
      while (rn > 0 && info[rn+visw] == 0) {
        rn -= numvals;
      }
    }

    if (rn >= 0) {
      int ln = 0;
      while (ln < rn && ((info[ln+advx] == 0) || (!lineIsLTR && info[ln+visw] == 0))) {
        ln += numvals;
      }

      al = Math.max(0f, info[ln+posx]);
      aw = info[rn+posx] + info[rn+advx] - al;
    }

    /*
      boolean lineIsLTR = source.lineIsLTR();
      int rn = info.length - numvals;
      while (rn > 0 && ((info[rn+advx] == 0) || (lineIsLTR && info[rn+visw] == 0))) {
      rn -= numvals;
      }

      if (rn >= 0) {
      int ln = 0;
      while (ln < rn && ((info[ln+advx] == 0) || (!lineIsLTR && info[ln+visw] == 0))) {
      ln += numvals;
      }

      al = Math.max(0f, info[ln+posx]);
      aw = info[rn+posx] + info[rn+advx] - al;
      }
      */

    return new Rectangle2D.Float(al, at, aw, ah);
  }

  public Rectangle2D createItalicBounds() {
    float ia = cm.italicAngle;

    Rectangle2D lb = getLogicalBounds();
    float l = (float)lb.getMinX();
    float t = -cm.ascent;
    float r = (float)lb.getMaxX();
    float b = cm.descent;
    if (ia != 0) {
        if (ia > 0) {
            l -= ia * (b - cm.ssOffset);
            r -= ia * (t - cm.ssOffset);
        } else {
            l -= ia * (t - cm.ssOffset);
            r -= ia * (b - cm.ssOffset);
        }
    }
    return new Rectangle2D.Float(l, t, r - l, b - t);
  }

  private StandardGlyphVector getGV() {
    if (gv == null) {
      gv = createGV();
    }

    return gv;
  }

  protected StandardGlyphVector createGV() {
    FontRenderContext frc = source.getFRC();
    int flags = source.getLayoutFlags();
    char[] context = source.getChars();
    int start = source.getStart();
    int length = source.getLength();

    GlyphLayout gl = GlyphLayout.get(null); // !!! no custom layout engines
    gv = gl.layout(font, frc, context, start, length, flags, null); // ??? use textsource
    GlyphLayout.done(gl);

    return gv;
  }

  // ExtendedTextLabel API

  private static final int posx = 0,
    posy = 1,
    advx = 2,
    advy = 3,
    visx = 4,
    visy = 5,
    visw = 6,
    vish = 7;
  private static final int numvals = 8;

  public int getNumCharacters() {
    return source.getLength();
  }

  public CoreMetrics getCoreMetrics() {
    return cm;
  }

  public float getCharX(int index) {
    validate(index);
    float[] charinfo = getCharinfo();
    int idx = l2v(index) * numvals + posx;
    if (charinfo == null || idx >= charinfo.length) {
        return 0f;
    } else {
        return charinfo[idx];
    }
  }

  public float getCharY(int index) {
    validate(index);
    float[] charinfo = getCharinfo();
    int idx = l2v(index) * numvals + posy;
    if (charinfo == null || idx >= charinfo.length) {
        return 0f;
    } else {
        return charinfo[idx];
    }
  }

  public float getCharAdvance(int index) {
    validate(index);
    float[] charinfo = getCharinfo();
    int idx = l2v(index) * numvals + advx;
    if (charinfo == null || idx >= charinfo.length) {
        return 0f;
    } else {
        return charinfo[idx];
    }
  }

  public Rectangle2D handleGetCharVisualBounds(int index) {
    validate(index);
    float[] charinfo = getCharinfo();
    index = l2v(index) * numvals;
    if (charinfo == null || (index+vish) >= charinfo.length) {
        return new Rectangle2D.Float();
    }
    return new Rectangle2D.Float(
                                 charinfo[index + visx],
                                 charinfo[index + visy],
                                 charinfo[index + visw],
                                 charinfo[index + vish]);
  }

  public Rectangle2D getCharVisualBounds(int index, float x, float y) {

    Rectangle2D bounds = decorator.getCharVisualBounds(this, index);
    if (x != 0 || y != 0) {
        bounds.setRect(bounds.getX()+x,
                       bounds.getY()+y,
                       bounds.getWidth(),
                       bounds.getHeight());
    }
    return bounds;
  }

  private void validate(int index) {
    if (index < 0) {
      throw new IllegalArgumentException("index " + index + " < 0");
    } else if (index >= source.getLength()) {
      throw new IllegalArgumentException("index " + index + " < " + source.getLength());
    }
  }

  /*
    public int hitTestChar(float x, float y) {
    // !!! return index of char hit, for swing
    // result is negative for trailing-edge hits
    // no italics so no problem at margins.
    // for now, ignore y since we assume horizontal text

    // find non-combining char origin to right of x
    float[] charinfo = getCharinfo();

    int n = 0;
    int e = source.getLength();
    while (n < e && charinfo[n + advx] != 0 && charinfo[n + posx] > x) {
    n += numvals;
    }
    float rightx = n < e ? charinfo[n+posx] : charinfo[e - numvals + posx] + charinfo[e - numvals + advx];

    // find non-combining char to left of that char
    n -= numvals;
    while (n >= 0 && charinfo[n+advx] == 0) {
    n -= numvals;
    }
    float leftx = n >= 0 ? charinfo[n+posx] : 0;
    float lefta = n >= 0 ? charinfo[n+advx] : 0;

    n /= numvals;

    boolean left = true;
    if (x < leftx + lefta / 2f) {
    // left of prev char
    } else if (x < (leftx + lefta + rightx) / 2f) {
    // right of prev char
    left = false;
    } else {
    // left of follow char
    n += 1;
    }

    if ((source.getLayoutFlags() & 0x1) != 0) {
    n = getNumCharacters() - 1 - n;
    left = !left;
    }

    return left ? n : -n;
    }
    */

  public int logicalToVisual(int logicalIndex) {
    validate(logicalIndex);
    return l2v(logicalIndex);
  }

  public int visualToLogical(int visualIndex) {
    validate(visualIndex);
    return v2l(visualIndex);
  }

  public int getLineBreakIndex(int start, float width) {
    float[] charinfo = getCharinfo();
    int length = source.getLength();
    --start;
    while (width >= 0 && ++start < length) {
      int cidx = l2v(start) * numvals + advx;
      if (cidx >= charinfo.length) {
          break; // layout bailed for some reason
      }
      float adv = charinfo[cidx];
      width -= adv;
    }

    return start;
  }

  public float getAdvanceBetween(int start, int limit) {
    float a = 0f;

    float[] charinfo = getCharinfo();
    --start;
    while (++start < limit) {
      int cidx = l2v(start) * numvals + advx;
      if (cidx >= charinfo.length) {
          break; // layout bailed for some reason
      }
      a += charinfo[cidx];
    }

    return a;
  }

  public boolean caretAtOffsetIsValid(int offset) {
      // REMIND: improve this implementation

      // Ligature formation can either be done in logical order,
      // with the ligature glyph logically preceding the null
      // chars;  or in visual order, with the ligature glyph to
      // the left of the null chars.  This method's implementation
      // must reflect which strategy is used.

      if (offset == 0 || offset == source.getLength()) {
          return true;
      }
      char c = source.getChars()[source.getStart() + offset];
      if (c == '\t' || c == '\n' || c == '\r') { // hack
          return true;
      }
      int v = l2v(offset);

      // If ligatures are always to the left, do this stuff:
      //if (!(source.getLayoutFlags() & 0x1) == 0) {
      //    v += 1;
      //    if (v == source.getLength()) {
      //        return true;
      //    }
      //}

      int idx = v * numvals + advx;
      float[] charinfo = getCharinfo();
      if (charinfo == null || idx >= charinfo.length) {
          return false;
      } else {
          return charinfo[idx] != 0;
      }
  }

  private float[] getCharinfo() {
    if (charinfo == null) {
      charinfo = createCharinfo();
    }
    return charinfo;
  }

  private static final boolean DEBUG = FontUtilities.debugFonts();
/*
* This takes the glyph info record obtained from the glyph vector and converts it into a similar record
* adjusted to represent character data instead.  For economy we don't use glyph info records in this processing.
*
* Here are some constraints:
* - there can be more glyphs than characters (glyph insertion, perhaps based on normalization, has taken place)
* - there can be fewer glyphs than characters
*   Some layout engines may insert 0xffff glyphs for characters ligaturized away, but
*   not all do, and it cannot be relied upon.
* - each glyph maps to a single character, when multiple glyphs exist for a character they all map to it, but
*   no two characters map to the same glyph
* - multiple glyphs mapping to the same character need not be in sequence (thai, tamil have split characters)
* - glyphs may be arbitrarily reordered (Indic reorders glyphs)
* - all glyphs share the same bidi level
* - all glyphs share the same horizontal (or vertical) baseline
* - combining marks visually follow their base character in the glyph array-- i.e. in an rtl gv they are
*   to the left of their base character-- and have zero advance.
*
* The output maps this to character positions, and therefore caret positions, via the following assumptions:
* - zero-advance glyphs do not contribute to the advance of their character (i.e. position is ignored), conversely
*   if a glyph is to contribute to the advance of its character it must have a non-zero (float) advance
* - no carets can appear between a zero width character and its preceding character, where 'preceding' is
*   defined logically.
* - no carets can appear within a split character
* - no carets can appear within a local reordering (i.e. Indic reordering, or non-adjacent split characters)
* - all characters lie on the same baseline, and it is either horizontal or vertical
* - the charinfo is in uniform ltr or rtl order (visual order), since local reorderings and split characters are removed
*
* The algorithm works in the following way:
* 1) we scan the glyphs ltr or rtl based on the bidi run direction
* 2) Since the may be fewer glyphs than chars we cannot work in place.
*    A new array is allocated for output.
*    a) if the line is ltr, we start writing at position 0 until we finish, there may be leftver space
*    b) if the line is rtl and 1-1, we start writing at position numChars/glyphs - 1 until we finish at 0
*    c) otherwise if we don't finish at 0, we have to copy the data down
* 3) we consume clusters in the following way:
*    a) the first element is always consumed
*    b) subsequent elements are consumed if:
*       i) their advance is zero
*       ii) their character index <= the character index of any character seen in this cluster
*       iii) the minimum character index seen in this cluster isn't adjacent to the previous cluster
*    c) character data is written as follows for horizontal lines (x/y and w/h are exchanged on vertical lines)
*       i) the x position is the position of the leftmost glyph whose advance is not zero
*       ii)the y position is the baseline
*       iii) the x advance is the distance to the maximum x + adv of all glyphs whose advance is not zero
*       iv) the y advance is the baseline
*       v) vis x,y,w,h tightly encloses the vis x,y,w,h of all the glyphs with nonzero w and h
* 4) In the future, we can make some simple optimizations to avoid copying if we know some things:
*    a) if the mapping is 1-1, unidirectional, and there are no zero-adv glyphs, we just return the glyphinfo
*    b) if the mapping is 1-1, unidirectional, we just adjust the remaining glyphs to originate at right/left of the base
*    c) if the mapping is 1-1, we compute the base position and advance as we go, then go back to adjust the remaining glyphs
*    d) otherwise we keep separate track of the write position as we do (c) since no glyph in the cluster may be in the
*    position we are writing.
*    e) most clusters are simply the single base glyph in the same position as its character, so we try to avoid
*    copying its data unnecessarily.
* 5) the glyph vector ought to provide access to these 'global' attributes to enable these optimizations.  A single
*    int with flags set is probably ok, we could also provide accessors for each attribute.  This doesn't map to
*    the GlyphMetrics flags very well, so I won't attempt to keep them similar.  It might be useful to add those
*    in addition to these.
*    int FLAG_HAS_ZERO_ADVANCE_GLYPHS = 1; // set if there are zero-advance glyphs
*    int FLAG_HAS_NONUNIFORM_ORDER = 2; // set if some glyphs are rearranged out of character visual order
*    int FLAG_HAS_SPLIT_CHARACTERS = 4; // set if multiple glyphs per character
*    int getDescriptionFlags(); // return an int containing the above flags
*    boolean hasZeroAdvanceGlyphs();
*    boolean hasNonuniformOrder();
*    boolean hasSplitCharacters();
*    The optimized cases in (4) correspond to values 0, 1, 3, and 7 returned by getDescriptionFlags().
*/
  protected float[] createCharinfo() {
    StandardGlyphVector gv = getGV();
    float[] glyphinfo = null;
    try {
        glyphinfo = gv.getGlyphInfo();
    }
    catch (Exception e) {
        if (DEBUG) {
            System.err.println(source);
            e.printStackTrace();
        }
        glyphinfo = new float[gv.getNumGlyphs() * numvals];
    }

    int numGlyphs = gv.getNumGlyphs();
    if (numGlyphs == 0) {
        return glyphinfo;
    }
    int[] indices = gv.getGlyphCharIndices(0, numGlyphs, null);
    float[] charInfo = new float[source.getLength() * numvals];

    if (DEBUG) {
      System.err.println("number of glyphs: " + numGlyphs);
      System.err.println("glyphinfo.len: " + glyphinfo.length);
      System.err.println("indices.len: " + indices.length);
      for (int i = 0; i < numGlyphs; ++i) {
        System.err.println("g: " + i +
            "  v: " + gv.getGlyphCode(i) +
            ", x: " + glyphinfo[i*numvals+posx] +
            ", a: " + glyphinfo[i*numvals+advx] +
            ", n: " + indices[i]);
      }
    }

    int minIndex = indices[0];  // smallest index seen this cluster
    int maxIndex = minIndex;    // largest index seen this cluster
    int cp = 0;                 // character position
    int cc = 0;
    int gp = 0;                 // glyph position
    int gx = 0;                 // glyph index (visual)
    int gxlimit = numGlyphs;    // limit of gx, when we reach this we're done
    int pdelta = numvals;       // delta for incrementing positions
    int xdelta = 1;             // delta for incrementing indices

    boolean rtl = (source.getLayoutFlags() & 0x1) == 1;
    if (rtl) {
        minIndex = indices[numGlyphs - 1];
        maxIndex = minIndex;
        cp = charInfo.length - numvals;
        gp = glyphinfo.length - numvals;
        gx = numGlyphs - 1;
        gxlimit = -1;
        pdelta = -numvals;
        xdelta = -1;
    }

    /*
    // to support vertical, use 'ixxxx' indices and swap horiz and vertical components
    if (source.isVertical()) {
        iposx = posy;
        iposy = posx;
        iadvx = advy;
        iadvy = advx;
        ivisx = visy;
        ivisy = visx;
        ivish = visw;
        ivisw = vish;
    } else {
        // use standard values
    }
    */

    // use intermediates to reduce array access when we need to
    float cposl = 0, cposr = 0, cvisl = 0, cvist = 0, cvisr = 0, cvisb = 0;
    float baseline = 0;

    while (gx != gxlimit) {
        // start of new cluster
        int clusterExtraGlyphs = 0;

        minIndex = indices[gx];
        maxIndex = minIndex;

        cposl = glyphinfo[gp + posx];
        cposr = cposl + glyphinfo[gp + advx];
        cvisl = glyphinfo[gp + visx];
        cvist = glyphinfo[gp + visy];
        cvisr = cvisl + glyphinfo[gp + visw];
        cvisb = cvist + glyphinfo[gp + vish];

        // advance to next glyph
        gx += xdelta;
        gp += pdelta;

        while (gx != gxlimit &&
               ((glyphinfo[gp + advx] == 0) ||
               (indices[gx] <= maxIndex) ||
               (maxIndex - minIndex > clusterExtraGlyphs))) {

            ++clusterExtraGlyphs; // have an extra glyph in this cluster
            if (DEBUG) {
                System.err.println("gp=" +gp +" adv=" + glyphinfo[gp + advx] +
                                   " gx="+ gx+ " i[gx]="+indices[gx] +
                                   " clusterExtraGlyphs="+clusterExtraGlyphs);
            }

            // adjust advance only if new glyph has non-zero advance
            float radvx = glyphinfo[gp + advx];
            if (radvx != 0) {
                float rposx = glyphinfo[gp + posx];
                cposl = Math.min(cposl, rposx);
                cposr = Math.max(cposr, rposx + radvx);
            }

            // adjust visible bounds only if new glyph has non-empty bounds
            float rvisw = glyphinfo[gp + visw];
            if (rvisw != 0) {
                float rvisx = glyphinfo[gp + visx];
                float rvisy = glyphinfo[gp + visy];
                cvisl = Math.min(cvisl, rvisx);
                cvist = Math.min(cvist, rvisy);
                cvisr = Math.max(cvisr, rvisx + rvisw);
                cvisb = Math.max(cvisb, rvisy + glyphinfo[gp + vish]);
            }

            // adjust min, max index
            minIndex = Math.min(minIndex, indices[gx]);
            maxIndex = Math.max(maxIndex, indices[gx]);

            // get ready to examine next glyph
            gx += xdelta;
            gp += pdelta;
        }
        // done with cluster, gx and gp are set for next glyph

        if (DEBUG) {
            System.err.println("minIndex = " + minIndex + ", maxIndex = " + maxIndex);
        }

        // save adjustments to the base character and do common adjustments.
        charInfo[cp + posx] = cposl;
        charInfo[cp + posy] = baseline;
        charInfo[cp + advx] = cposr - cposl;
        charInfo[cp + advy] = 0;
        charInfo[cp + visx] = cvisl;
        charInfo[cp + visy] = cvist;
        charInfo[cp + visw] = cvisr - cvisl;
        charInfo[cp + vish] = cvisb - cvist;
        cc++;

        /* We may have consumed multiple glyphs for this char position.
         * Map those extra consumed glyphs to char positions that would follow
         * up to the index prior to that which begins the next cluster.
         * If we have reached the last glyph (reached gxlimit) then we need to
         * map remaining unmapped chars to the same location as the last one.
         */
        int tgt;
        if (gx == gxlimit) {
           tgt = charInfo.length / numvals;
        } else {
           tgt = indices[gx];
        }
        if (DEBUG) {
           System.err.println("gx=" + gx + " gxlimit=" + gxlimit +
                              " charInfo.len=" + charInfo.length +
                              " tgt=" + tgt + " cc=" + cc + " cp=" + cp);
        }
        while (cc < tgt) {
            if (rtl) {
                // if rtl, characters to left of base, else to right.  reuse cposr.
                cposr = cposl;
            }
            cvisr -= cvisl; // reuse, convert to deltas.
            cvisb -= cvist;

            cp += pdelta;

            if (cp < 0 || cp >= charInfo.length) {
                if (DEBUG)  {
                    System.err.println("Error : cp=" + cp +
                                       " charInfo.length=" + charInfo.length);
                }
                break;
            }

            if (DEBUG) {
                System.err.println("Insert charIndex " + cc + " at pos="+cp);
            }
            charInfo[cp + posx] = cposr;
            charInfo[cp + posy] = baseline;
            charInfo[cp + advx] = 0;
            charInfo[cp + advy] = 0;
            charInfo[cp + visx] = cvisl;
            charInfo[cp + visy] = cvist;
            charInfo[cp + visw] = cvisr;
            charInfo[cp + vish] = cvisb;
            cc++;
        }
        cp += pdelta; // reset for new cluster
    }

    if (DEBUG) {
        char[] chars = source.getChars();
        int start = source.getStart();
        int length = source.getLength();
        System.err.println("char info for " + length + " characters");

        for (int i = 0; i < length * numvals;) {
            System.err.println(" ch: " + Integer.toHexString(chars[start + v2l(i / numvals)]) +
                               " x: " + charInfo[i++] +
                               " y: " + charInfo[i++] +
                               " xa: " + charInfo[i++] +
                               " ya: " + charInfo[i++] +
                               " l: " + charInfo[i++] +
                               " t: " + charInfo[i++] +
                               " w: " + charInfo[i++] +
                               " h: " + charInfo[i++]);
      }
    }
    return charInfo;
  }

  /**
   * Map logical character index to visual character index.
   * <p>
   * This ignores hindi reordering.  @see createCharinfo
   */
  protected int l2v(int index) {
    return (source.getLayoutFlags() & 0x1) == 0 ? index : source.getLength() - 1 - index;
  }

  /**
   * Map visual character index to logical character index.
   * <p>
   * This ignores hindi reordering.  @see createCharinfo
   */
  protected int v2l(int index) {
    return (source.getLayoutFlags() & 0x1) == 0 ? index : source.getLength() - 1 - index;
  }

  public TextLineComponent getSubset(int start, int limit, int dir) {
    return new ExtendedTextSourceLabel(source.getSubSource(start, limit-start, dir), decorator);
  }

  public String toString() {
    if (true) {
        return source.toString(TextSource.WITHOUT_CONTEXT);
    }
    StringBuilder sb = new StringBuilder();
    sb.append(super.toString());
    sb.append("[source:");
    sb.append(source.toString(TextSource.WITHOUT_CONTEXT));
    sb.append(", lb:");
    sb.append(lb);
    sb.append(", ab:");
    sb.append(ab);
    sb.append(", vb:");
    sb.append(vb);
    sb.append(", gv:");
    sb.append(gv);
    sb.append(", ci: ");
    if (charinfo == null) {
      sb.append("null");
    } else {
      sb.append(charinfo[0]);
      for (int i = 1; i < charinfo.length;) {
        sb.append(i % numvals == 0 ? "; " : ", ");
        sb.append(charinfo[i]);
      }
    }
    sb.append("]");

    return sb.toString();
  }

  //public static ExtendedTextLabel create(TextSource source) {
  //  return new ExtendedTextSourceLabel(source);
  //}

  public int getNumJustificationInfos() {
    return getGV().getNumGlyphs();
  }


  public void getJustificationInfos(GlyphJustificationInfo[] infos, int infoStart, int charStart, int charLimit) {
    // This simple implementation only uses spaces for justification.
    // Since regular characters aren't justified, we don't need to deal with
    // special infos for combining marks or ligature substitution glyphs.
    // added character justification for kanjii only 2/22/98

    StandardGlyphVector gv = getGV();

    float[] charinfo = getCharinfo();

    float size = gv.getFont().getSize2D();

    GlyphJustificationInfo nullInfo =
      new GlyphJustificationInfo(0,
                                 false, GlyphJustificationInfo.PRIORITY_NONE, 0, 0,
                                 false, GlyphJustificationInfo.PRIORITY_NONE, 0, 0);

    GlyphJustificationInfo spaceInfo =
      new GlyphJustificationInfo(size,
                                 true, GlyphJustificationInfo.PRIORITY_WHITESPACE, 0, size,
                                 true, GlyphJustificationInfo.PRIORITY_WHITESPACE, 0, size / 4f);

    GlyphJustificationInfo kanjiInfo =
      new GlyphJustificationInfo(size,
                                 true, GlyphJustificationInfo.PRIORITY_INTERCHAR, size, size,
                                 false, GlyphJustificationInfo.PRIORITY_NONE, 0, 0);

    char[] chars = source.getChars();
    int offset = source.getStart();

    // assume data is 1-1 and either all rtl or all ltr, for now

    int numGlyphs = gv.getNumGlyphs();
    int minGlyph = 0;
    int maxGlyph = numGlyphs;
    boolean ltr = (source.getLayoutFlags() & 0x1) == 0;
    if (charStart != 0 || charLimit != source.getLength()) {
      if (ltr) {
        minGlyph = charStart;
        maxGlyph = charLimit;
      } else {
        minGlyph = numGlyphs - charLimit;
        maxGlyph = numGlyphs - charStart;
      }
    }

    for (int i = 0; i < numGlyphs; ++i) {
      GlyphJustificationInfo info = null;
      if (i >= minGlyph && i < maxGlyph) {
        if (charinfo[i * numvals + advx] == 0) { // combining marks don't justify
          info = nullInfo;
        } else {
          int ci = v2l(i); // 1-1 assumption again
          char c = chars[offset + ci];
          if (Character.isWhitespace(c)) {
            info = spaceInfo;
            // CJK, Hangul, CJK Compatibility areas
          } else if (c >= 0x4e00 &&
                     (c < 0xa000) ||
                     (c >= 0xac00 && c < 0xd7b0) ||
                     (c >= 0xf900 && c < 0xfb00)) {
            info = kanjiInfo;
          } else {
            info = nullInfo;
          }
        }
      }
      infos[infoStart + i] = info;
    }
  }

  public TextLineComponent applyJustificationDeltas(float[] deltas, int deltaStart, boolean[] flags) {

    // when we justify, we need to adjust the charinfo since spaces
    // change their advances.  preserve the existing charinfo.

    float[] newCharinfo = getCharinfo().clone();

    // we only push spaces, so never need to rejustify
    flags[0] = false;

    // preserve the existing gv.

    StandardGlyphVector newgv = (StandardGlyphVector)getGV().clone();
    float[] newPositions = newgv.getGlyphPositions(null);
    int numGlyphs = newgv.getNumGlyphs();

    /*
    System.out.println("oldgv: " + getGV() + ", newgv: " + newgv);
    System.out.println("newpositions: " + newPositions);
    for (int i = 0; i < newPositions.length; i += 2) {
      System.out.println("[" + (i/2) + "] " + newPositions[i] + ", " + newPositions[i+1]);
    }

    System.out.println("deltas: " + deltas + " start: " + deltaStart);
    for (int i = deltaStart; i < deltaStart + numGlyphs; i += 2) {
      System.out.println("[" + (i/2) + "] " + deltas[i] + ", " + deltas[i+1]);
    }
    */

    char[] chars = source.getChars();
    int offset = source.getStart();

    // accumulate the deltas to adjust positions and advances.
    // handle whitespace by modifying advance,
    // handle everything else by modifying position before and after

    float deltaPos = 0;
    for (int i = 0; i < numGlyphs; ++i) {
      if (Character.isWhitespace(chars[offset + v2l(i)])) {
        newPositions[i*2] += deltaPos;

        float deltaAdv = deltas[deltaStart + i*2] + deltas[deltaStart + i*2 + 1];

        newCharinfo[i * numvals + posx] += deltaPos;
        newCharinfo[i * numvals + visx] += deltaPos;
        newCharinfo[i * numvals + advx] += deltaAdv;

        deltaPos += deltaAdv;
      } else {
        deltaPos += deltas[deltaStart + i*2];

        newPositions[i*2] += deltaPos;
        newCharinfo[i * numvals + posx] += deltaPos;
        newCharinfo[i * numvals + visx] += deltaPos;

        deltaPos += deltas[deltaStart + i*2 + 1];
      }
    }
    newPositions[numGlyphs * 2] += deltaPos;

    newgv.setGlyphPositions(newPositions);

    /*
    newPositions = newgv.getGlyphPositions(null);
    System.out.println(">> newpositions: " + newPositions);
    for (int i = 0; i < newPositions.length; i += 2) {
      System.out.println("[" + (i/2) + "] " + newPositions[i] + ", " + newPositions[i+1]);
    }
    */

    ExtendedTextSourceLabel result = new ExtendedTextSourceLabel(source, decorator);
    result.gv = newgv;
    result.charinfo = newCharinfo;

    return result;
  }
}
