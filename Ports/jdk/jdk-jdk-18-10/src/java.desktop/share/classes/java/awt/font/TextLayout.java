/*
 * Copyright (c) 1997, 2013, Oracle and/or its affiliates. All rights reserved.
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
 * (C) Copyright Taligent, Inc. 1996 - 1997, All Rights Reserved
 * (C) Copyright IBM Corp. 1996-2003, All Rights Reserved
 *
 * The original version of this source code and documentation is
 * copyrighted and owned by Taligent, Inc., a wholly-owned subsidiary
 * of IBM. These materials are provided under terms of a License
 * Agreement between Taligent and Sun. This technology is protected
 * by multiple US and International patents.
 *
 * This notice and attribution to Taligent may not be removed.
 * Taligent is a registered trademark of Taligent, Inc.
 *
 */

package java.awt.font;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.font.NumericShaper;
import java.awt.font.TextLine.TextLineMetrics;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.text.AttributedString;
import java.text.AttributedCharacterIterator;
import java.text.AttributedCharacterIterator.Attribute;
import java.text.CharacterIterator;
import java.util.Map;
import java.util.HashMap;
import java.util.Hashtable;
import sun.font.AttributeValues;
import sun.font.CodePointIterator;
import sun.font.CoreMetrics;
import sun.font.Decoration;
import sun.font.FontLineMetrics;
import sun.font.FontResolver;
import sun.font.GraphicComponent;
import sun.font.LayoutPathImpl;

/**
 *
 * {@code TextLayout} is an immutable graphical representation of styled
 * character data.
 * <p>
 * It provides the following capabilities:
 * <ul>
 * <li>implicit bidirectional analysis and reordering,
 * <li>cursor positioning and movement, including split cursors for
 * mixed directional text,
 * <li>highlighting, including both logical and visual highlighting
 * for mixed directional text,
 * <li>multiple baselines (roman, hanging, and centered),
 * <li>hit testing,
 * <li>justification,
 * <li>default font substitution,
 * <li>metric information such as ascent, descent, and advance, and
 * <li>rendering
 * </ul>
 * <p>
 * A {@code TextLayout} object can be rendered using
 * its {@code draw} method.
 * <p>
 * {@code TextLayout} can be constructed either directly or through
 * the use of a {@link LineBreakMeasurer}.  When constructed directly, the
 * source text represents a single paragraph.  {@code LineBreakMeasurer}
 * allows styled text to be broken into lines that fit within a particular
 * width.  See the {@code LineBreakMeasurer} documentation for more
 * information.
 * <p>
 * {@code TextLayout} construction logically proceeds as follows:
 * <ul>
 * <li>paragraph attributes are extracted and examined,
 * <li>text is analyzed for bidirectional reordering, and reordering
 * information is computed if needed,
 * <li>text is segmented into style runs
 * <li>fonts are chosen for style runs, first by using a font if the
 * attribute {@link TextAttribute#FONT} is present, otherwise by computing
 * a default font using the attributes that have been defined
 * <li>if text is on multiple baselines, the runs or subruns are further
 * broken into subruns sharing a common baseline,
 * <li>glyphvectors are generated for each run using the chosen font,
 * <li>final bidirectional reordering is performed on the glyphvectors
 * </ul>
 * <p>
 * All graphical information returned from a {@code TextLayout}
 * object's methods is relative to the origin of the
 * {@code TextLayout}, which is the intersection of the
 * {@code TextLayout} object's baseline with its left edge.  Also,
 * coordinates passed into a {@code TextLayout} object's methods
 * are assumed to be relative to the {@code TextLayout} object's
 * origin.  Clients usually need to translate between a
 * {@code TextLayout} object's coordinate system and the coordinate
 * system in another object (such as a
 * {@link java.awt.Graphics Graphics} object).
 * <p>
 * {@code TextLayout} objects are constructed from styled text,
 * but they do not retain a reference to their source text.  Thus,
 * changes in the text previously used to generate a {@code TextLayout}
 * do not affect the {@code TextLayout}.
 * <p>
 * Three methods on a {@code TextLayout} object
 * ({@code getNextRightHit}, {@code getNextLeftHit}, and
 * {@code hitTestChar}) return instances of {@link TextHitInfo}.
 * The offsets contained in these {@code TextHitInfo} objects
 * are relative to the start of the {@code TextLayout}, <b>not</b>
 * to the text used to create the {@code TextLayout}.  Similarly,
 * {@code TextLayout} methods that accept {@code TextHitInfo}
 * instances as parameters expect the {@code TextHitInfo} object's
 * offsets to be relative to the {@code TextLayout}, not to any
 * underlying text storage model.
 * <p>
 * <strong>Examples</strong>:<p>
 * Constructing and drawing a {@code TextLayout} and its bounding
 * rectangle:
 * <blockquote><pre>
 *   Graphics2D g = ...;
 *   Point2D loc = ...;
 *   Font font = Font.getFont("Helvetica-bold-italic");
 *   FontRenderContext frc = g.getFontRenderContext();
 *   TextLayout layout = new TextLayout("This is a string", font, frc);
 *   layout.draw(g, (float)loc.getX(), (float)loc.getY());
 *
 *   Rectangle2D bounds = layout.getBounds();
 *   bounds.setRect(bounds.getX()+loc.getX(),
 *                  bounds.getY()+loc.getY(),
 *                  bounds.getWidth(),
 *                  bounds.getHeight());
 *   g.draw(bounds);
 * </pre>
 * </blockquote>
 * <p>
 * Hit-testing a {@code TextLayout} (determining which character is at
 * a particular graphical location):
 * <blockquote><pre>
 *   Point2D click = ...;
 *   TextHitInfo hit = layout.hitTestChar(
 *                         (float) (click.getX() - loc.getX()),
 *                         (float) (click.getY() - loc.getY()));
 * </pre>
 * </blockquote>
 * <p>
 * Responding to a right-arrow key press:
 * <blockquote><pre>
 *   int insertionIndex = ...;
 *   TextHitInfo next = layout.getNextRightHit(insertionIndex);
 *   if (next != null) {
 *       // translate graphics to origin of layout on screen
 *       g.translate(loc.getX(), loc.getY());
 *       Shape[] carets = layout.getCaretShapes(next.getInsertionIndex());
 *       g.draw(carets[0]);
 *       if (carets[1] != null) {
 *           g.draw(carets[1]);
 *       }
 *   }
 * </pre></blockquote>
 * <p>
 * Drawing a selection range corresponding to a substring in the source text.
 * The selected area may not be visually contiguous:
 * <blockquote><pre>
 *   // selStart, selLimit should be relative to the layout,
 *   // not to the source text
 *
 *   int selStart = ..., selLimit = ...;
 *   Color selectionColor = ...;
 *   Shape selection = layout.getLogicalHighlightShape(selStart, selLimit);
 *   // selection may consist of disjoint areas
 *   // graphics is assumed to be translated to origin of layout
 *   g.setColor(selectionColor);
 *   g.fill(selection);
 * </pre></blockquote>
 * <p>
 * Drawing a visually contiguous selection range.  The selection range may
 * correspond to more than one substring in the source text.  The ranges of
 * the corresponding source text substrings can be obtained with
 * {@code getLogicalRangesForVisualSelection()}:
 * <blockquote><pre>
 *   TextHitInfo selStart = ..., selLimit = ...;
 *   Shape selection = layout.getVisualHighlightShape(selStart, selLimit);
 *   g.setColor(selectionColor);
 *   g.fill(selection);
 *   int[] ranges = getLogicalRangesForVisualSelection(selStart, selLimit);
 *   // ranges[0], ranges[1] is the first selection range,
 *   // ranges[2], ranges[3] is the second selection range, etc.
 * </pre></blockquote>
 * <p>
 * Note: Font rotations can cause text baselines to be rotated, and
 * multiple runs with different rotations can cause the baseline to
 * bend or zig-zag.  In order to account for this (rare) possibility,
 * some APIs are specified to return metrics and take parameters 'in
 * baseline-relative coordinates' (e.g. ascent, advance), and others
 * are in 'in standard coordinates' (e.g. getBounds).  Values in
 * baseline-relative coordinates map the 'x' coordinate to the
 * distance along the baseline, (positive x is forward along the
 * baseline), and the 'y' coordinate to a distance along the
 * perpendicular to the baseline at 'x' (positive y is 90 degrees
 * clockwise from the baseline vector).  Values in standard
 * coordinates are measured along the x and y axes, with 0,0 at the
 * origin of the TextLayout.  Documentation for each relevant API
 * indicates what values are in what coordinate system.  In general,
 * measurement-related APIs are in baseline-relative coordinates,
 * while display-related APIs are in standard coordinates.
 *
 * @see LineBreakMeasurer
 * @see TextAttribute
 * @see TextHitInfo
 * @see LayoutPath
 */
public final class TextLayout implements Cloneable {

    private int characterCount;
    private boolean isVerticalLine = false;
    private byte baseline;
    private float[] baselineOffsets;  // why have these ?
    private TextLine textLine;

    // cached values computed from GlyphSets and set info:
    // all are recomputed from scratch in buildCache()
    private TextLine.TextLineMetrics lineMetrics = null;
    private float visibleAdvance;

    /*
     * TextLayouts are supposedly immutable.  If you mutate a TextLayout under
     * the covers (like the justification code does) you'll need to set this
     * back to false.  Could be replaced with textLine != null <--> cacheIsValid.
     */
    private boolean cacheIsValid = false;


    // This value is obtained from an attribute, and constrained to the
    // interval [0,1].  If 0, the layout cannot be justified.
    private float justifyRatio;

    // If a layout is produced by justification, then that layout
    // cannot be justified.  To enforce this constraint the
    // justifyRatio of the justified layout is set to this value.
    private static final float ALREADY_JUSTIFIED = -53.9f;

    // dx and dy specify the distance between the TextLayout's origin
    // and the origin of the leftmost GlyphSet (TextLayoutComponent,
    // actually).  They were used for hanging punctuation support,
    // which is no longer implemented.  Currently they are both always 0,
    // and TextLayout is not guaranteed to work with non-zero dx, dy
    // values right now.  They were left in as an aide and reminder to
    // anyone who implements hanging punctuation or other similar stuff.
    // They are static now so they don't take up space in TextLayout
    // instances.
    private static float dx;
    private static float dy;

    /*
     * Natural bounds is used internally.  It is built on demand in
     * getNaturalBounds.
     */
    private Rectangle2D naturalBounds = null;

    /*
     * boundsRect encloses all of the bits this TextLayout can draw.  It
     * is build on demand in getBounds.
     */
    private Rectangle2D boundsRect = null;

    /*
     * flag to suppress/allow carets inside of ligatures when hit testing or
     * arrow-keying
     */
    private boolean caretsInLigaturesAreAllowed = false;

    /**
     * Defines a policy for determining the strong caret location.
     * This class contains one method, {@code getStrongCaret}, which
     * is used to specify the policy that determines the strong caret in
     * dual-caret text.  The strong caret is used to move the caret to the
     * left or right. Instances of this class can be passed to
     * {@code getCaretShapes}, {@code getNextLeftHit} and
     * {@code getNextRightHit} to customize strong caret
     * selection.
     * <p>
     * To specify alternate caret policies, subclass {@code CaretPolicy}
     * and override {@code getStrongCaret}.  {@code getStrongCaret}
     * should inspect the two {@code TextHitInfo} arguments and choose
     * one of them as the strong caret.
     * <p>
     * Most clients do not need to use this class.
     */
    public static class CaretPolicy {

        /**
         * Constructs a {@code CaretPolicy}.
         */
         public CaretPolicy() {
         }

        /**
         * Chooses one of the specified {@code TextHitInfo} instances as
         * a strong caret in the specified {@code TextLayout}.
         * @param hit1 a valid hit in {@code layout}
         * @param hit2 a valid hit in {@code layout}
         * @param layout the {@code TextLayout} in which
         *        {@code hit1} and {@code hit2} are used
         * @return {@code hit1} or {@code hit2}
         *        (or an equivalent {@code TextHitInfo}), indicating the
         *        strong caret.
         */
        public TextHitInfo getStrongCaret(TextHitInfo hit1,
                                          TextHitInfo hit2,
                                          TextLayout layout) {

            // default implementation just calls private method on layout
            return layout.getStrongHit(hit1, hit2);
        }
    }

    /**
     * This {@code CaretPolicy} is used when a policy is not specified
     * by the client.  With this policy, a hit on a character whose direction
     * is the same as the line direction is stronger than a hit on a
     * counterdirectional character.  If the characters' directions are
     * the same, a hit on the leading edge of a character is stronger
     * than a hit on the trailing edge of a character.
     */
    public static final CaretPolicy DEFAULT_CARET_POLICY = new CaretPolicy();

    /**
     * Constructs a {@code TextLayout} from a {@code String}
     * and a {@link Font}.  All the text is styled using the specified
     * {@code Font}.
     * <p>
     * The {@code String} must specify a single paragraph of text,
     * because an entire paragraph is required for the bidirectional
     * algorithm.
     * @param string the text to display
     * @param font a {@code Font} used to style the text
     * @param frc contains information about a graphics device which is needed
     *       to measure the text correctly.
     *       Text measurements can vary slightly depending on the
     *       device resolution, and attributes such as antialiasing.  This
     *       parameter does not specify a translation between the
     *       {@code TextLayout} and user space.
     */
    public TextLayout(String string, Font font, FontRenderContext frc) {

        if (font == null) {
            throw new IllegalArgumentException("Null font passed to TextLayout constructor.");
        }

        if (string == null) {
            throw new IllegalArgumentException("Null string passed to TextLayout constructor.");
        }

        if (string.length() == 0) {
            throw new IllegalArgumentException("Zero length string passed to TextLayout constructor.");
        }

        Map<? extends Attribute, ?> attributes = null;
        if (font.hasLayoutAttributes()) {
            attributes = font.getAttributes();
        }

        char[] text = string.toCharArray();
        if (sameBaselineUpTo(font, text, 0, text.length) == text.length) {
            fastInit(text, font, attributes, frc);
        } else {
            AttributedString as = attributes == null
                ? new AttributedString(string)
                : new AttributedString(string, attributes);
            as.addAttribute(TextAttribute.FONT, font);
            standardInit(as.getIterator(), text, frc);
        }
    }

    /**
     * Constructs a {@code TextLayout} from a {@code String}
     * and an attribute set.
     * <p>
     * All the text is styled using the provided attributes.
     * <p>
     * {@code string} must specify a single paragraph of text because an
     * entire paragraph is required for the bidirectional algorithm.
     * @param string the text to display
     * @param attributes the attributes used to style the text
     * @param frc contains information about a graphics device which is needed
     *       to measure the text correctly.
     *       Text measurements can vary slightly depending on the
     *       device resolution, and attributes such as antialiasing.  This
     *       parameter does not specify a translation between the
     *       {@code TextLayout} and user space.
     */
    public TextLayout(String string, Map<? extends Attribute,?> attributes,
                      FontRenderContext frc)
    {
        if (string == null) {
            throw new IllegalArgumentException("Null string passed to TextLayout constructor.");
        }

        if (attributes == null) {
            throw new IllegalArgumentException("Null map passed to TextLayout constructor.");
        }

        if (string.length() == 0) {
            throw new IllegalArgumentException("Zero length string passed to TextLayout constructor.");
        }

        char[] text = string.toCharArray();
        Font font = singleFont(text, 0, text.length, attributes);
        if (font != null) {
            fastInit(text, font, attributes, frc);
        } else {
            AttributedString as = new AttributedString(string, attributes);
            standardInit(as.getIterator(), text, frc);
        }
    }

    /*
     * Determines a font for the attributes, and if a single font can render
     * all the text on one baseline, return it, otherwise null.  If the
     * attributes specify a font, assume it can display all the text without
     * checking.
     * If the AttributeSet contains an embedded graphic, return null.
     */
    private static Font singleFont(char[] text,
                                   int start,
                                   int limit,
                                   Map<? extends Attribute, ?> attributes) {

        if (attributes.get(TextAttribute.CHAR_REPLACEMENT) != null) {
            return null;
        }

        Font font = null;
        try {
            font = (Font)attributes.get(TextAttribute.FONT);
        }
        catch (ClassCastException e) {
        }
        if (font == null) {
            if (attributes.get(TextAttribute.FAMILY) != null) {
                font = Font.getFont(attributes);
                if (font.canDisplayUpTo(text, start, limit) != -1) {
                    return null;
                }
            } else {
                FontResolver resolver = FontResolver.getInstance();
                CodePointIterator iter = CodePointIterator.create(text, start, limit);
                int fontIndex = resolver.nextFontRunIndex(iter);
                if (iter.charIndex() == limit) {
                    font = resolver.getFont(fontIndex, attributes);
                }
            }
        }

        if (sameBaselineUpTo(font, text, start, limit) != limit) {
            return null;
        }

        return font;
    }

    /**
     * Constructs a {@code TextLayout} from an iterator over styled text.
     * <p>
     * The iterator must specify a single paragraph of text because an
     * entire paragraph is required for the bidirectional
     * algorithm.
     * @param text the styled text to display
     * @param frc contains information about a graphics device which is needed
     *       to measure the text correctly.
     *       Text measurements can vary slightly depending on the
     *       device resolution, and attributes such as antialiasing.  This
     *       parameter does not specify a translation between the
     *       {@code TextLayout} and user space.
     */
    public TextLayout(AttributedCharacterIterator text, FontRenderContext frc) {

        if (text == null) {
            throw new IllegalArgumentException("Null iterator passed to TextLayout constructor.");
        }

        int start = text.getBeginIndex();
        int limit = text.getEndIndex();
        if (start == limit) {
            throw new IllegalArgumentException("Zero length iterator passed to TextLayout constructor.");
        }

        int len = limit - start;
        text.first();
        char[] chars = new char[len];
        int n = 0;
        for (char c = text.first();
             c != CharacterIterator.DONE;
             c = text.next())
        {
            chars[n++] = c;
        }

        text.first();
        if (text.getRunLimit() == limit) {

            Map<? extends Attribute, ?> attributes = text.getAttributes();
            Font font = singleFont(chars, 0, len, attributes);
            if (font != null) {
                fastInit(chars, font, attributes, frc);
                return;
            }
        }

        standardInit(text, chars, frc);
    }

    /**
     * Creates a {@code TextLayout} from a {@link TextLine} and
     * some paragraph data.  This method is used by {@link TextMeasurer}.
     * @param textLine the line measurement attributes to apply to the
     *       the resulting {@code TextLayout}
     * @param baseline the baseline of the text
     * @param baselineOffsets the baseline offsets for this
     * {@code TextLayout}.  This should already be normalized to
     * {@code baseline}
     * @param justifyRatio {@code 0} if the {@code TextLayout}
     *     cannot be justified; {@code 1} otherwise.
     */
    TextLayout(TextLine textLine,
               byte baseline,
               float[] baselineOffsets,
               float justifyRatio) {

        this.characterCount = textLine.characterCount();
        this.baseline = baseline;
        this.baselineOffsets = baselineOffsets;
        this.textLine = textLine;
        this.justifyRatio = justifyRatio;
    }

    /**
     * Initialize the paragraph-specific data.
     */
    private void paragraphInit(byte aBaseline, CoreMetrics lm,
                               Map<? extends Attribute, ?> paragraphAttrs,
                               char[] text) {

        baseline = aBaseline;

        // normalize to current baseline
        baselineOffsets = TextLine.getNormalizedOffsets(lm.baselineOffsets, baseline);

        justifyRatio = AttributeValues.getJustification(paragraphAttrs);
        NumericShaper shaper = AttributeValues.getNumericShaping(paragraphAttrs);
        if (shaper != null) {
            shaper.shape(text, 0, text.length);
        }
    }

    /*
     * the fast init generates a single glyph set.  This requires:
     * all one style
     * all renderable by one font (ie no embedded graphics)
     * all on one baseline
     */
    private void fastInit(char[] chars, Font font,
                          Map<? extends Attribute, ?> attrs,
                          FontRenderContext frc) {

        // Object vf = attrs.get(TextAttribute.ORIENTATION);
        // isVerticalLine = TextAttribute.ORIENTATION_VERTICAL.equals(vf);
        isVerticalLine = false;

        LineMetrics lm = font.getLineMetrics(chars, 0, chars.length, frc);
        CoreMetrics cm = CoreMetrics.get(lm);
        byte glyphBaseline = (byte) cm.baselineIndex;

        if (attrs == null) {
            baseline = glyphBaseline;
            baselineOffsets = cm.baselineOffsets;
            justifyRatio = 1.0f;
        } else {
            paragraphInit(glyphBaseline, cm, attrs, chars);
        }

        characterCount = chars.length;

        textLine = TextLine.fastCreateTextLine(frc, chars, font, cm, attrs);
    }

    /*
     * the standard init generates multiple glyph sets based on style,
     * renderable, and baseline runs.
     * @param chars the text in the iterator, extracted into a char array
     */
    private void standardInit(AttributedCharacterIterator text, char[] chars, FontRenderContext frc) {

        characterCount = chars.length;

        // set paragraph attributes
        {
            // If there's an embedded graphic at the start of the
            // paragraph, look for the first non-graphic character
            // and use it and its font to initialize the paragraph.
            // If not, use the first graphic to initialize.

            Map<? extends Attribute, ?> paragraphAttrs = text.getAttributes();

            boolean haveFont = TextLine.advanceToFirstFont(text);

            if (haveFont) {
                Font defaultFont = TextLine.getFontAtCurrentPos(text);
                int charsStart = text.getIndex() - text.getBeginIndex();
                LineMetrics lm = defaultFont.getLineMetrics(chars, charsStart, charsStart+1, frc);
                CoreMetrics cm = CoreMetrics.get(lm);
                paragraphInit((byte)cm.baselineIndex, cm, paragraphAttrs, chars);
            }
            else {
                // hmmm what to do here?  Just try to supply reasonable
                // values I guess.

                GraphicAttribute graphic = (GraphicAttribute)
                                paragraphAttrs.get(TextAttribute.CHAR_REPLACEMENT);
                byte defaultBaseline = getBaselineFromGraphic(graphic);
                CoreMetrics cm = GraphicComponent.createCoreMetrics(graphic);
                paragraphInit(defaultBaseline, cm, paragraphAttrs, chars);
            }
        }

        textLine = TextLine.standardCreateTextLine(frc, text, chars, baselineOffsets);
    }

    /*
     * A utility to rebuild the ascent/descent/leading/advance cache.
     * You'll need to call this if you clone and mutate (like justification,
     * editing methods do)
     */
    private void ensureCache() {
        if (!cacheIsValid) {
            buildCache();
        }
    }

    private void buildCache() {
        lineMetrics = textLine.getMetrics();

        // compute visibleAdvance
        if (textLine.isDirectionLTR()) {

            int lastNonSpace = characterCount-1;
            while (lastNonSpace != -1) {
                int logIndex = textLine.visualToLogical(lastNonSpace);
                if (!textLine.isCharSpace(logIndex)) {
                    break;
                }
                else {
                    --lastNonSpace;
                }
            }
            if (lastNonSpace == characterCount-1) {
                visibleAdvance = lineMetrics.advance;
            }
            else if (lastNonSpace == -1) {
                visibleAdvance = 0;
            }
            else {
                int logIndex = textLine.visualToLogical(lastNonSpace);
                visibleAdvance = textLine.getCharLinePosition(logIndex)
                                        + textLine.getCharAdvance(logIndex);
            }
        }
        else {

            int leftmostNonSpace = 0;
            while (leftmostNonSpace != characterCount) {
                int logIndex = textLine.visualToLogical(leftmostNonSpace);
                if (!textLine.isCharSpace(logIndex)) {
                    break;
                }
                else {
                    ++leftmostNonSpace;
                }
            }
            if (leftmostNonSpace == characterCount) {
                visibleAdvance = 0;
            }
            else if (leftmostNonSpace == 0) {
                visibleAdvance = lineMetrics.advance;
            }
            else {
                int logIndex = textLine.visualToLogical(leftmostNonSpace);
                float pos = textLine.getCharLinePosition(logIndex);
                visibleAdvance = lineMetrics.advance - pos;
            }
        }

        // naturalBounds, boundsRect will be generated on demand
        naturalBounds = null;
        boundsRect = null;

        cacheIsValid = true;
    }

    /**
     * The 'natural bounds' encloses all the carets the layout can draw.
     *
     */
    private Rectangle2D getNaturalBounds() {
        ensureCache();

        if (naturalBounds == null) {
            naturalBounds = textLine.getItalicBounds();
        }

        return naturalBounds;
    }

    /**
     * Creates a copy of this {@code TextLayout}.
     */
    protected Object clone() {
        /*
         * !!! I think this is safe.  Once created, nothing mutates the
         * glyphvectors or arrays.  But we need to make sure.
         * {jbr} actually, that's not quite true.  The justification code
         * mutates after cloning.  It doesn't actually change the glyphvectors
         * (that's impossible) but it replaces them with justified sets.  This
         * is a problem for GlyphIterator creation, since new GlyphIterators
         * are created by cloning a prototype.  If the prototype has outdated
         * glyphvectors, so will the new ones.  A partial solution is to set the
         * prototypical GlyphIterator to null when the glyphvectors change.  If
         * you forget this one time, you're hosed.
         */
        try {
            return super.clone();
        }
        catch (CloneNotSupportedException e) {
            throw new InternalError(e);
        }
    }

    /*
     * Utility to throw an exception if an invalid TextHitInfo is passed
     * as a parameter.  Avoids code duplication.
     */
    private void checkTextHit(TextHitInfo hit) {
        if (hit == null) {
            throw new IllegalArgumentException("TextHitInfo is null.");
        }

        if (hit.getInsertionIndex() < 0 ||
            hit.getInsertionIndex() > characterCount) {
            throw new IllegalArgumentException("TextHitInfo is out of range");
        }
    }

    /**
     * Creates a copy of this {@code TextLayout} justified to the
     * specified width.
     * <p>
     * If this {@code TextLayout} has already been justified, an
     * exception is thrown.  If this {@code TextLayout} object's
     * justification ratio is zero, a {@code TextLayout} identical
     * to this {@code TextLayout} is returned.
     * @param justificationWidth the width to use when justifying the line.
     * For best results, it should not be too different from the current
     * advance of the line.
     * @return a {@code TextLayout} justified to the specified width.
     * @exception Error if this layout has already been justified, an Error is
     * thrown.
     */
    public TextLayout getJustifiedLayout(float justificationWidth) {

        if (justificationWidth <= 0) {
            throw new IllegalArgumentException("justificationWidth <= 0 passed to TextLayout.getJustifiedLayout()");
        }

        if (justifyRatio == ALREADY_JUSTIFIED) {
            throw new Error("Can't justify again.");
        }

        ensureCache(); // make sure textLine is not null

        // default justification range to exclude trailing logical whitespace
        int limit = characterCount;
        while (limit > 0 && textLine.isCharWhitespace(limit-1)) {
            --limit;
        }

        TextLine newLine = textLine.getJustifiedLine(justificationWidth, justifyRatio, 0, limit);
        if (newLine != null) {
            return new TextLayout(newLine, baseline, baselineOffsets, ALREADY_JUSTIFIED);
        }

        return this;
    }

    /**
     * Justify this layout.  Overridden by subclassers to control justification
     * (if there were subclassers, that is...)
     *
     * The layout will only justify if the paragraph attributes (from the
     * source text, possibly defaulted by the layout attributes) indicate a
     * non-zero justification ratio.  The text will be justified to the
     * indicated width.  The current implementation also adjusts hanging
     * punctuation and trailing whitespace to overhang the justification width.
     * Once justified, the layout may not be rejustified.
     * <p>
     * Some code may rely on immutability of layouts.  Subclassers should not
     * call this directly, but instead should call getJustifiedLayout, which
     * will call this method on a clone of this layout, preserving
     * the original.
     *
     * @param justificationWidth the width to use when justifying the line.
     * For best results, it should not be too different from the current
     * advance of the line.
     * @see #getJustifiedLayout(float)
     */
    protected void handleJustify(float justificationWidth) {
      // never called
    }


    /**
     * Returns the baseline for this {@code TextLayout}.
     * The baseline is one of the values defined in {@code Font},
     * which are roman, centered and hanging.  Ascent and descent are
     * relative to this baseline.  The {@code baselineOffsets}
     * are also relative to this baseline.
     * @return the baseline of this {@code TextLayout}.
     * @see #getBaselineOffsets()
     * @see Font
     */
    public byte getBaseline() {
        return baseline;
    }

    /**
     * Returns the offsets array for the baselines used for this
     * {@code TextLayout}.
     * <p>
     * The array is indexed by one of the values defined in
     * {@code Font}, which are roman, centered and hanging.  The
     * values are relative to this {@code TextLayout} object's
     * baseline, so that {@code getBaselineOffsets[getBaseline()] == 0}.
     * Offsets are added to the position of the {@code TextLayout}
     * object's baseline to get the position for the new baseline.
     * @return the offsets array containing the baselines used for this
     *    {@code TextLayout}.
     * @see #getBaseline()
     * @see Font
     */
    public float[] getBaselineOffsets() {
        float[] offsets = new float[baselineOffsets.length];
        System.arraycopy(baselineOffsets, 0, offsets, 0, offsets.length);
        return offsets;
    }

    /**
     * Returns the advance of this {@code TextLayout}.
     * The advance is the distance from the origin to the advance of the
     * rightmost (bottommost) character.  This is in baseline-relative
     * coordinates.
     * @return the advance of this {@code TextLayout}.
     */
    public float getAdvance() {
        ensureCache();
        return lineMetrics.advance;
    }

    /**
     * Returns the advance of this {@code TextLayout}, minus trailing
     * whitespace.  This is in baseline-relative coordinates.
     * @return the advance of this {@code TextLayout} without the
     *      trailing whitespace.
     * @see #getAdvance()
     */
    public float getVisibleAdvance() {
        ensureCache();
        return visibleAdvance;
    }

    /**
     * Returns the ascent of this {@code TextLayout}.
     * The ascent is the distance from the top (right) of the
     * {@code TextLayout} to the baseline.  It is always either
     * positive or zero.  The ascent is sufficient to
     * accommodate superscripted text and is the maximum of the sum of the
     * ascent, offset, and baseline of each glyph.  The ascent is
     * the maximum ascent from the baseline of all the text in the
     * TextLayout.  It is in baseline-relative coordinates.
     * @return the ascent of this {@code TextLayout}.
     */
    public float getAscent() {
        ensureCache();
        return lineMetrics.ascent;
    }

    /**
     * Returns the descent of this {@code TextLayout}.
     * The descent is the distance from the baseline to the bottom (left) of
     * the {@code TextLayout}.  It is always either positive or zero.
     * The descent is sufficient to accommodate subscripted text and is the
     * maximum of the sum of the descent, offset, and baseline of each glyph.
     * This is the maximum descent from the baseline of all the text in
     * the TextLayout.  It is in baseline-relative coordinates.
     * @return the descent of this {@code TextLayout}.
     */
    public float getDescent() {
        ensureCache();
        return lineMetrics.descent;
    }

    /**
     * Returns the leading of the {@code TextLayout}.
     * The leading is the suggested interline spacing for this
     * {@code TextLayout}.  This is in baseline-relative
     * coordinates.
     * <p>
     * The leading is computed from the leading, descent, and baseline
     * of all glyphvectors in the {@code TextLayout}.  The algorithm
     * is roughly as follows:
     * <blockquote><pre>
     * maxD = 0;
     * maxDL = 0;
     * for (GlyphVector g in all glyphvectors) {
     *    maxD = max(maxD, g.getDescent() + offsets[g.getBaseline()]);
     *    maxDL = max(maxDL, g.getDescent() + g.getLeading() +
     *                       offsets[g.getBaseline()]);
     * }
     * return maxDL - maxD;
     * </pre></blockquote>
     * @return the leading of this {@code TextLayout}.
     */
    public float getLeading() {
        ensureCache();
        return lineMetrics.leading;
    }

    /**
     * Returns the bounds of this {@code TextLayout}.
     * The bounds are in standard coordinates.
     * <p>Due to rasterization effects, this bounds might not enclose all of the
     * pixels rendered by the TextLayout.</p>
     * It might not coincide exactly with the ascent, descent,
     * origin or advance of the {@code TextLayout}.
     * @return a {@link Rectangle2D} that is the bounds of this
     *        {@code TextLayout}.
     */
    public Rectangle2D getBounds() {
        ensureCache();

        if (boundsRect == null) {
            Rectangle2D vb = textLine.getVisualBounds();
            if (dx != 0 || dy != 0) {
                vb.setRect(vb.getX() - dx,
                           vb.getY() - dy,
                           vb.getWidth(),
                           vb.getHeight());
            }
            boundsRect = vb;
        }

        Rectangle2D bounds = new Rectangle2D.Float();
        bounds.setRect(boundsRect);

        return bounds;
    }

    /**
     * Returns the pixel bounds of this {@code TextLayout} when
     * rendered in a graphics with the given
     * {@code FontRenderContext} at the given location.  The
     * graphics render context need not be the same as the
     * {@code FontRenderContext} used to create this
     * {@code TextLayout}, and can be null.  If it is null, the
     * {@code FontRenderContext} of this {@code TextLayout}
     * is used.
     * @param frc the {@code FontRenderContext} of the {@code Graphics}.
     * @param x the x-coordinate at which to render this {@code TextLayout}.
     * @param y the y-coordinate at which to render this {@code TextLayout}.
     * @return a {@code Rectangle} bounding the pixels that would be affected.
     * @see GlyphVector#getPixelBounds
     * @since 1.6
     */
    public Rectangle getPixelBounds(FontRenderContext frc, float x, float y) {
        return textLine.getPixelBounds(frc, x, y);
    }

    /**
     * Returns {@code true} if this {@code TextLayout} has
     * a left-to-right base direction or {@code false} if it has
     * a right-to-left base direction.  The {@code TextLayout}
     * has a base direction of either left-to-right (LTR) or
     * right-to-left (RTL).  The base direction is independent of the
     * actual direction of text on the line, which may be either LTR,
     * RTL, or mixed. Left-to-right layouts by default should position
     * flush left.  If the layout is on a tabbed line, the
     * tabs run left to right, so that logically successive layouts position
     * left to right.  The opposite is true for RTL layouts. By default they
     * should position flush left, and tabs run right-to-left.
     * @return {@code true} if the base direction of this
     *         {@code TextLayout} is left-to-right; {@code false}
     *         otherwise.
     */
    public boolean isLeftToRight() {
        return textLine.isDirectionLTR();
    }

    /**
     * Returns {@code true} if this {@code TextLayout} is vertical.
     * @return {@code true} if this {@code TextLayout} is vertical;
     *      {@code false} otherwise.
     */
    public boolean isVertical() {
        return isVerticalLine;
    }

    /**
     * Returns the number of characters represented by this
     * {@code TextLayout}.
     * @return the number of characters in this {@code TextLayout}.
     */
    public int getCharacterCount() {
        return characterCount;
    }

    /*
     * carets and hit testing
     *
     * Positions on a text line are represented by instances of TextHitInfo.
     * Any TextHitInfo with characterOffset between 0 and characterCount-1,
     * inclusive, represents a valid position on the line.  Additionally,
     * [-1, trailing] and [characterCount, leading] are valid positions, and
     * represent positions at the logical start and end of the line,
     * respectively.
     *
     * The characterOffsets in TextHitInfo's used and returned by TextLayout
     * are relative to the beginning of the text layout, not necessarily to
     * the beginning of the text storage the client is using.
     *
     *
     * Every valid TextHitInfo has either one or two carets associated with it.
     * A caret is a visual location in the TextLayout indicating where text at
     * the TextHitInfo will be displayed on screen.  If a TextHitInfo
     * represents a location on a directional boundary, then there are two
     * possible visible positions for newly inserted text.  Consider the
     * following example, in which capital letters indicate right-to-left text,
     * and the overall line direction is left-to-right:
     *
     * Text Storage: [ a, b, C, D, E, f ]
     * Display:        a b E D C f
     *
     * The text hit info (1, t) represents the trailing side of 'b'.  If 'q',
     * a left-to-right character is inserted into the text storage at this
     * location, it will be displayed between the 'b' and the 'E':
     *
     * Text Storage: [ a, b, q, C, D, E, f ]
     * Display:        a b q E D C f
     *
     * However, if a 'W', which is right-to-left, is inserted into the storage
     * after 'b', the storage and display will be:
     *
     * Text Storage: [ a, b, W, C, D, E, f ]
     * Display:        a b E D C W f
     *
     * So, for the original text storage, two carets should be displayed for
     * location (1, t): one visually between 'b' and 'E' and one visually
     * between 'C' and 'f'.
     *
     *
     * When two carets are displayed for a TextHitInfo, one caret is the
     * 'strong' caret and the other is the 'weak' caret.  The strong caret
     * indicates where an inserted character will be displayed when that
     * character's direction is the same as the direction of the TextLayout.
     * The weak caret shows where an character inserted character will be
     * displayed when the character's direction is opposite that of the
     * TextLayout.
     *
     *
     * Clients should not be overly concerned with the details of correct
     * caret display. TextLayout.getCaretShapes(TextHitInfo) will return an
     * array of two paths representing where carets should be displayed.
     * The first path in the array is the strong caret; the second element,
     * if non-null, is the weak caret.  If the second element is null,
     * then there is no weak caret for the given TextHitInfo.
     *
     *
     * Since text can be visually reordered, logically consecutive
     * TextHitInfo's may not be visually consecutive.  One implication of this
     * is that a client cannot tell from inspecting a TextHitInfo whether the
     * hit represents the first (or last) caret in the layout.  Clients
     * can call getVisualOtherHit();  if the visual companion is
     * (-1, TRAILING) or (characterCount, LEADING), then the hit is at the
     * first (last) caret position in the layout.
     */

    private float[] getCaretInfo(int caret,
                                 Rectangle2D bounds,
                                 float[] info) {

        float top1X, top2X;
        float bottom1X, bottom2X;

        if (caret == 0 || caret == characterCount) {

            float pos;
            int logIndex;
            if (caret == characterCount) {
                logIndex = textLine.visualToLogical(characterCount-1);
                pos = textLine.getCharLinePosition(logIndex)
                                        + textLine.getCharAdvance(logIndex);
            }
            else {
                logIndex = textLine.visualToLogical(caret);
                pos = textLine.getCharLinePosition(logIndex);
            }
            float angle = textLine.getCharAngle(logIndex);
            float shift = textLine.getCharShift(logIndex);
            pos += angle * shift;
            top1X = top2X = pos + angle*textLine.getCharAscent(logIndex);
            bottom1X = bottom2X = pos - angle*textLine.getCharDescent(logIndex);
        }
        else {

            {
                int logIndex = textLine.visualToLogical(caret-1);
                float angle1 = textLine.getCharAngle(logIndex);
                float pos1 = textLine.getCharLinePosition(logIndex)
                                    + textLine.getCharAdvance(logIndex);
                if (angle1 != 0) {
                    pos1 += angle1 * textLine.getCharShift(logIndex);
                    top1X = pos1 + angle1*textLine.getCharAscent(logIndex);
                    bottom1X = pos1 - angle1*textLine.getCharDescent(logIndex);
                }
                else {
                    top1X = bottom1X = pos1;
                }
            }
            {
                int logIndex = textLine.visualToLogical(caret);
                float angle2 = textLine.getCharAngle(logIndex);
                float pos2 = textLine.getCharLinePosition(logIndex);
                if (angle2 != 0) {
                    pos2 += angle2*textLine.getCharShift(logIndex);
                    top2X = pos2 + angle2*textLine.getCharAscent(logIndex);
                    bottom2X = pos2 - angle2*textLine.getCharDescent(logIndex);
                }
                else {
                    top2X = bottom2X = pos2;
                }
            }
        }

        float topX = (top1X + top2X) / 2;
        float bottomX = (bottom1X + bottom2X) / 2;

        if (info == null) {
            info = new float[2];
        }

        if (isVerticalLine) {
            info[1] = (float) ((topX - bottomX) / bounds.getWidth());
            info[0] = (float) (topX + (info[1]*bounds.getX()));
        }
        else {
            info[1] = (float) ((topX - bottomX) / bounds.getHeight());
            info[0] = (float) (bottomX + (info[1]*bounds.getMaxY()));
        }

        return info;
    }

    /**
     * Returns information about the caret corresponding to {@code hit}.
     * The first element of the array is the intersection of the caret with
     * the baseline, as a distance along the baseline. The second element
     * of the array is the inverse slope (run/rise) of the caret, measured
     * with respect to the baseline at that point.
     * <p>
     * This method is meant for informational use.  To display carets, it
     * is better to use {@code getCaretShapes}.
     * @param hit a hit on a character in this {@code TextLayout}
     * @param bounds the bounds to which the caret info is constructed.
     *     The bounds is in baseline-relative coordinates.
     * @return a two-element array containing the position and slope of
     * the caret.  The returned caret info is in baseline-relative coordinates.
     * @see #getCaretShapes(int, Rectangle2D, TextLayout.CaretPolicy)
     * @see Font#getItalicAngle
     */
    public float[] getCaretInfo(TextHitInfo hit, Rectangle2D bounds) {
        ensureCache();
        checkTextHit(hit);

        return getCaretInfoTestInternal(hit, bounds);
    }

    // this version provides extra info in the float array
    // the first two values are as above
    // the next four values are the endpoints of the caret, as computed
    // using the hit character's offset (baseline + ssoffset) and
    // natural ascent and descent.
    // these  values are trimmed to the bounds where required to fit,
    // but otherwise independent of it.
    private float[] getCaretInfoTestInternal(TextHitInfo hit, Rectangle2D bounds) {
        ensureCache();
        checkTextHit(hit);

        float[] info = new float[6];

        // get old data first
        getCaretInfo(hitToCaret(hit), bounds, info);

        // then add our new data
        double iangle, ixbase, p1x, p1y, p2x, p2y;

        int charix = hit.getCharIndex();
        boolean lead = hit.isLeadingEdge();
        boolean ltr = textLine.isDirectionLTR();
        boolean horiz = !isVertical();

        if (charix == -1 || charix == characterCount) {
            // !!! note: want non-shifted, baseline ascent and descent here!
            // TextLine should return appropriate line metrics object for these values
            TextLineMetrics m = textLine.getMetrics();
            boolean low = ltr == (charix == -1);
            iangle = 0;
            if (horiz) {
                p1x = p2x = low ? 0 : m.advance;
                p1y = -m.ascent;
                p2y = m.descent;
            } else {
                p1y = p2y = low ? 0 : m.advance;
                p1x = m.descent;
                p2x = m.ascent;
            }
        } else {
            CoreMetrics thiscm = textLine.getCoreMetricsAt(charix);
            iangle = thiscm.italicAngle;
            ixbase = textLine.getCharLinePosition(charix, lead);
            if (thiscm.baselineIndex < 0) {
                // this is a graphic, no italics, use entire line height for caret
                TextLineMetrics m = textLine.getMetrics();
                if (horiz) {
                    p1x = p2x = ixbase;
                    if (thiscm.baselineIndex == GraphicAttribute.TOP_ALIGNMENT) {
                        p1y = -m.ascent;
                        p2y = p1y + thiscm.height;
                    } else {
                        p2y = m.descent;
                        p1y = p2y - thiscm.height;
                    }
                } else {
                    p1y = p2y = ixbase;
                    p1x = m.descent;
                    p2x = m.ascent;
                    // !!! top/bottom adjustment not implemented for vertical
                }
            } else {
                float bo = baselineOffsets[thiscm.baselineIndex];
                if (horiz) {
                    ixbase += iangle * thiscm.ssOffset;
                    p1x = ixbase + iangle * thiscm.ascent;
                    p2x = ixbase - iangle * thiscm.descent;
                    p1y = bo - thiscm.ascent;
                    p2y = bo + thiscm.descent;
                } else {
                    ixbase -= iangle * thiscm.ssOffset;
                    p1y = ixbase + iangle * thiscm.ascent;
                    p2y = ixbase - iangle * thiscm.descent;
                    p1x = bo + thiscm.ascent;
                    p2x = bo + thiscm.descent;
                }
            }
        }

        info[2] = (float)p1x;
        info[3] = (float)p1y;
        info[4] = (float)p2x;
        info[5] = (float)p2y;

        return info;
    }

    /**
     * Returns information about the caret corresponding to {@code hit}.
     * This method is a convenience overload of {@code getCaretInfo} and
     * uses the natural bounds of this {@code TextLayout}.
     * @param hit a hit on a character in this {@code TextLayout}
     * @return the information about a caret corresponding to a hit.  The
     *     returned caret info is in baseline-relative coordinates.
     */
    public float[] getCaretInfo(TextHitInfo hit) {

        return getCaretInfo(hit, getNaturalBounds());
    }

    /**
     * Returns a caret index corresponding to {@code hit}.
     * Carets are numbered from left to right (top to bottom) starting from
     * zero. This always places carets next to the character hit, on the
     * indicated side of the character.
     * @param hit a hit on a character in this {@code TextLayout}
     * @return a caret index corresponding to the specified hit.
     */
    private int hitToCaret(TextHitInfo hit) {

        int hitIndex = hit.getCharIndex();

        if (hitIndex < 0) {
            return textLine.isDirectionLTR() ? 0 : characterCount;
        } else if (hitIndex >= characterCount) {
            return textLine.isDirectionLTR() ? characterCount : 0;
        }

        int visIndex = textLine.logicalToVisual(hitIndex);

        if (hit.isLeadingEdge() != textLine.isCharLTR(hitIndex)) {
            ++visIndex;
        }

        return visIndex;
    }

    /**
     * Given a caret index, return a hit whose caret is at the index.
     * The hit is NOT guaranteed to be strong!!!
     *
     * @param caret a caret index.
     * @return a hit on this layout whose strong caret is at the requested
     * index.
     */
    private TextHitInfo caretToHit(int caret) {

        if (caret == 0 || caret == characterCount) {

            if ((caret == characterCount) == textLine.isDirectionLTR()) {
                return TextHitInfo.leading(characterCount);
            }
            else {
                return TextHitInfo.trailing(-1);
            }
        }
        else {

            int charIndex = textLine.visualToLogical(caret);
            boolean leading = textLine.isCharLTR(charIndex);

            return leading? TextHitInfo.leading(charIndex)
                            : TextHitInfo.trailing(charIndex);
        }
    }

    private boolean caretIsValid(int caret) {

        if (caret == characterCount || caret == 0) {
            return true;
        }

        int offset = textLine.visualToLogical(caret);

        if (!textLine.isCharLTR(offset)) {
            offset = textLine.visualToLogical(caret-1);
            if (textLine.isCharLTR(offset)) {
                return true;
            }
        }

        // At this point, the leading edge of the character
        // at offset is at the given caret.

        return textLine.caretAtOffsetIsValid(offset);
    }

    /**
     * Returns the hit for the next caret to the right (bottom); if there
     * is no such hit, returns {@code null}.
     * If the hit character index is out of bounds, an
     * {@link IllegalArgumentException} is thrown.
     * @param hit a hit on a character in this layout
     * @return a hit whose caret appears at the next position to the
     * right (bottom) of the caret of the provided hit or {@code null}.
     */
    public TextHitInfo getNextRightHit(TextHitInfo hit) {
        ensureCache();
        checkTextHit(hit);

        int caret = hitToCaret(hit);

        if (caret == characterCount) {
            return null;
        }

        do {
            ++caret;
        } while (!caretIsValid(caret));

        return caretToHit(caret);
    }

    /**
     * Returns the hit for the next caret to the right (bottom); if no
     * such hit, returns {@code null}.  The hit is to the right of
     * the strong caret at the specified offset, as determined by the
     * specified policy.
     * The returned hit is the stronger of the two possible
     * hits, as determined by the specified policy.
     * @param offset an insertion offset in this {@code TextLayout}.
     * Cannot be less than 0 or greater than this {@code TextLayout}
     * object's character count.
     * @param policy the policy used to select the strong caret
     * @return a hit whose caret appears at the next position to the
     * right (bottom) of the caret of the provided hit, or {@code null}.
     */
    public TextHitInfo getNextRightHit(int offset, CaretPolicy policy) {

        if (offset < 0 || offset > characterCount) {
            throw new IllegalArgumentException("Offset out of bounds in TextLayout.getNextRightHit()");
        }

        if (policy == null) {
            throw new IllegalArgumentException("Null CaretPolicy passed to TextLayout.getNextRightHit()");
        }

        TextHitInfo hit1 = TextHitInfo.afterOffset(offset);
        TextHitInfo hit2 = hit1.getOtherHit();

        TextHitInfo nextHit = getNextRightHit(policy.getStrongCaret(hit1, hit2, this));

        if (nextHit != null) {
            TextHitInfo otherHit = getVisualOtherHit(nextHit);
            return policy.getStrongCaret(otherHit, nextHit, this);
        }
        else {
            return null;
        }
    }

    /**
     * Returns the hit for the next caret to the right (bottom); if no
     * such hit, returns {@code null}.  The hit is to the right of
     * the strong caret at the specified offset, as determined by the
     * default policy.
     * The returned hit is the stronger of the two possible
     * hits, as determined by the default policy.
     * @param offset an insertion offset in this {@code TextLayout}.
     * Cannot be less than 0 or greater than the {@code TextLayout}
     * object's character count.
     * @return a hit whose caret appears at the next position to the
     * right (bottom) of the caret of the provided hit, or {@code null}.
     */
    public TextHitInfo getNextRightHit(int offset) {

        return getNextRightHit(offset, DEFAULT_CARET_POLICY);
    }

    /**
     * Returns the hit for the next caret to the left (top); if no such
     * hit, returns {@code null}.
     * If the hit character index is out of bounds, an
     * {@code IllegalArgumentException} is thrown.
     * @param hit a hit on a character in this {@code TextLayout}.
     * @return a hit whose caret appears at the next position to the
     * left (top) of the caret of the provided hit, or {@code null}.
     */
    public TextHitInfo getNextLeftHit(TextHitInfo hit) {
        ensureCache();
        checkTextHit(hit);

        int caret = hitToCaret(hit);

        if (caret == 0) {
            return null;
        }

        do {
            --caret;
        } while(!caretIsValid(caret));

        return caretToHit(caret);
    }

    /**
     * Returns the hit for the next caret to the left (top); if no
     * such hit, returns {@code null}.  The hit is to the left of
     * the strong caret at the specified offset, as determined by the
     * specified policy.
     * The returned hit is the stronger of the two possible
     * hits, as determined by the specified policy.
     * @param offset an insertion offset in this {@code TextLayout}.
     * Cannot be less than 0 or greater than this {@code TextLayout}
     * object's character count.
     * @param policy the policy used to select the strong caret
     * @return a hit whose caret appears at the next position to the
     * left (top) of the caret of the provided hit, or {@code null}.
     */
    public TextHitInfo getNextLeftHit(int offset, CaretPolicy policy) {

        if (policy == null) {
            throw new IllegalArgumentException("Null CaretPolicy passed to TextLayout.getNextLeftHit()");
        }

        if (offset < 0 || offset > characterCount) {
            throw new IllegalArgumentException("Offset out of bounds in TextLayout.getNextLeftHit()");
        }

        TextHitInfo hit1 = TextHitInfo.afterOffset(offset);
        TextHitInfo hit2 = hit1.getOtherHit();

        TextHitInfo nextHit = getNextLeftHit(policy.getStrongCaret(hit1, hit2, this));

        if (nextHit != null) {
            TextHitInfo otherHit = getVisualOtherHit(nextHit);
            return policy.getStrongCaret(otherHit, nextHit, this);
        }
        else {
            return null;
        }
    }

    /**
     * Returns the hit for the next caret to the left (top); if no
     * such hit, returns {@code null}.  The hit is to the left of
     * the strong caret at the specified offset, as determined by the
     * default policy.
     * The returned hit is the stronger of the two possible
     * hits, as determined by the default policy.
     * @param offset an insertion offset in this {@code TextLayout}.
     * Cannot be less than 0 or greater than this {@code TextLayout}
     * object's character count.
     * @return a hit whose caret appears at the next position to the
     * left (top) of the caret of the provided hit, or {@code null}.
     */
    public TextHitInfo getNextLeftHit(int offset) {

        return getNextLeftHit(offset, DEFAULT_CARET_POLICY);
    }

    /**
     * Returns the hit on the opposite side of the specified hit's caret.
     * @param hit the specified hit
     * @return a hit that is on the opposite side of the specified hit's
     *    caret.
     */
    public TextHitInfo getVisualOtherHit(TextHitInfo hit) {

        ensureCache();
        checkTextHit(hit);

        int hitCharIndex = hit.getCharIndex();

        int charIndex;
        boolean leading;

        if (hitCharIndex == -1 || hitCharIndex == characterCount) {

            int visIndex;
            if (textLine.isDirectionLTR() == (hitCharIndex == -1)) {
                visIndex = 0;
            }
            else {
                visIndex = characterCount-1;
            }

            charIndex = textLine.visualToLogical(visIndex);

            if (textLine.isDirectionLTR() == (hitCharIndex == -1)) {
                // at left end
                leading = textLine.isCharLTR(charIndex);
            }
            else {
                // at right end
                leading = !textLine.isCharLTR(charIndex);
            }
        }
        else {

            int visIndex = textLine.logicalToVisual(hitCharIndex);

            boolean movedToRight;
            if (textLine.isCharLTR(hitCharIndex) == hit.isLeadingEdge()) {
                --visIndex;
                movedToRight = false;
            }
            else {
                ++visIndex;
                movedToRight = true;
            }

            if (visIndex > -1 && visIndex < characterCount) {
                charIndex = textLine.visualToLogical(visIndex);
                leading = movedToRight == textLine.isCharLTR(charIndex);
            }
            else {
                charIndex =
                    (movedToRight == textLine.isDirectionLTR())? characterCount : -1;
                leading = charIndex == characterCount;
            }
        }

        return leading? TextHitInfo.leading(charIndex) :
                                TextHitInfo.trailing(charIndex);
    }

    private double[] getCaretPath(TextHitInfo hit, Rectangle2D bounds) {
        float[] info = getCaretInfo(hit, bounds);
        return new double[] { info[2], info[3], info[4], info[5] };
    }

    /**
     * Return an array of four floats corresponding the endpoints of the caret
     * x0, y0, x1, y1.
     *
     * This creates a line along the slope of the caret intersecting the
     * baseline at the caret
     * position, and extending from ascent above the baseline to descent below
     * it.
     */
    private double[] getCaretPath(int caret, Rectangle2D bounds,
                                  boolean clipToBounds) {

        float[] info = getCaretInfo(caret, bounds, null);

        double pos = info[0];
        double slope = info[1];

        double x0, y0, x1, y1;
        double x2 = -3141.59, y2 = -2.7; // values are there to make compiler happy

        double left = bounds.getX();
        double right = left + bounds.getWidth();
        double top = bounds.getY();
        double bottom = top + bounds.getHeight();

        boolean threePoints = false;

        if (isVerticalLine) {

            if (slope >= 0) {
                x0 = left;
                x1 = right;
            }
            else {
                x1 = left;
                x0 = right;
            }

            y0 = pos + x0 * slope;
            y1 = pos + x1 * slope;

            // y0 <= y1, always

            if (clipToBounds) {
                if (y0 < top) {
                    if (slope <= 0 || y1 <= top) {
                        y0 = y1 = top;
                    }
                    else {
                        threePoints = true;
                        y0 = top;
                        y2 = top;
                        x2 = x1 + (top-y1)/slope;
                        if (y1 > bottom) {
                            y1 = bottom;
                        }
                    }
                }
                else if (y1 > bottom) {
                    if (slope >= 0 || y0 >= bottom) {
                        y0 = y1 = bottom;
                    }
                    else {
                        threePoints = true;
                        y1 = bottom;
                        y2 = bottom;
                        x2 = x0 + (bottom-x1)/slope;
                    }
                }
            }

        }
        else {

            if (slope >= 0) {
                y0 = bottom;
                y1 = top;
            }
            else {
                y1 = bottom;
                y0 = top;
            }

            x0 = pos - y0 * slope;
            x1 = pos - y1 * slope;

            // x0 <= x1, always

            if (clipToBounds) {
                if (x0 < left) {
                    if (slope <= 0 || x1 <= left) {
                        x0 = x1 = left;
                    }
                    else {
                        threePoints = true;
                        x0 = left;
                        x2 = left;
                        y2 = y1 - (left-x1)/slope;
                        if (x1 > right) {
                            x1 = right;
                        }
                    }
                }
                else if (x1 > right) {
                    if (slope >= 0 || x0 >= right) {
                        x0 = x1 = right;
                    }
                    else {
                        threePoints = true;
                        x1 = right;
                        x2 = right;
                        y2 = y0 - (right-x0)/slope;
                    }
                }
            }
        }

        return threePoints?
                    new double[] { x0, y0, x2, y2, x1, y1 } :
                    new double[] { x0, y0, x1, y1 };
    }


    private static GeneralPath pathToShape(double[] path, boolean close, LayoutPathImpl lp) {
        GeneralPath result = new GeneralPath(GeneralPath.WIND_EVEN_ODD, path.length);
        result.moveTo((float)path[0], (float)path[1]);
        for (int i = 2; i < path.length; i += 2) {
            result.lineTo((float)path[i], (float)path[i+1]);
        }
        if (close) {
            result.closePath();
        }

        if (lp != null) {
            result = (GeneralPath)lp.mapShape(result);
        }
        return result;
    }

    /**
     * Returns a {@link Shape} representing the caret at the specified
     * hit inside the specified bounds.
     * @param hit the hit at which to generate the caret
     * @param bounds the bounds of the {@code TextLayout} to use
     *    in generating the caret.  The bounds is in baseline-relative
     *    coordinates.
     * @return a {@code Shape} representing the caret.  The returned
     *    shape is in standard coordinates.
     */
    public Shape getCaretShape(TextHitInfo hit, Rectangle2D bounds) {
        ensureCache();
        checkTextHit(hit);

        if (bounds == null) {
            throw new IllegalArgumentException("Null Rectangle2D passed to TextLayout.getCaret()");
        }

        return pathToShape(getCaretPath(hit, bounds), false, textLine.getLayoutPath());
    }

    /**
     * Returns a {@code Shape} representing the caret at the specified
     * hit inside the natural bounds of this {@code TextLayout}.
     * @param hit the hit at which to generate the caret
     * @return a {@code Shape} representing the caret.  The returned
     *     shape is in standard coordinates.
     */
    public Shape getCaretShape(TextHitInfo hit) {

        return getCaretShape(hit, getNaturalBounds());
    }

    /**
     * Return the "stronger" of the TextHitInfos.  The TextHitInfos
     * should be logical or visual counterparts.  They are not
     * checked for validity.
     */
    private TextHitInfo getStrongHit(TextHitInfo hit1, TextHitInfo hit2) {

        // right now we're using the following rule for strong hits:
        // A hit on a character with a lower level
        // is stronger than one on a character with a higher level.
        // If this rule ties, the hit on the leading edge of a character wins.
        // If THIS rule ties, hit1 wins.  Both rules shouldn't tie, unless the
        // infos aren't counterparts of some sort.

        byte hit1Level = getCharacterLevel(hit1.getCharIndex());
        byte hit2Level = getCharacterLevel(hit2.getCharIndex());

        if (hit1Level == hit2Level) {
            if (hit2.isLeadingEdge() && !hit1.isLeadingEdge()) {
                return hit2;
            }
            else {
                return hit1;
            }
        }
        else {
            return (hit1Level < hit2Level)? hit1 : hit2;
        }
    }

    /**
     * Returns the level of the character at {@code index}.
     * Indices -1 and {@code characterCount} are assigned the base
     * level of this {@code TextLayout}.
     * @param index the index of the character from which to get the level
     * @return the level of the character at the specified index.
     */
    public byte getCharacterLevel(int index) {

        // hmm, allow indices at endpoints?  For now, yes.
        if (index < -1 || index > characterCount) {
            throw new IllegalArgumentException("Index is out of range in getCharacterLevel.");
        }

        ensureCache();
        if (index == -1 || index == characterCount) {
             return (byte) (textLine.isDirectionLTR()? 0 : 1);
        }

        return textLine.getCharLevel(index);
    }

    /**
     * Returns two paths corresponding to the strong and weak caret.
     * @param offset an offset in this {@code TextLayout}
     * @param bounds the bounds to which to extend the carets.  The
     * bounds is in baseline-relative coordinates.
     * @param policy the specified {@code CaretPolicy}
     * @return an array of two paths.  Element zero is the strong
     * caret.  If there are two carets, element one is the weak caret,
     * otherwise it is {@code null}. The returned shapes
     * are in standard coordinates.
     */
    public Shape[] getCaretShapes(int offset, Rectangle2D bounds, CaretPolicy policy) {

        ensureCache();

        if (offset < 0 || offset > characterCount) {
            throw new IllegalArgumentException("Offset out of bounds in TextLayout.getCaretShapes()");
        }

        if (bounds == null) {
            throw new IllegalArgumentException("Null Rectangle2D passed to TextLayout.getCaretShapes()");
        }

        if (policy == null) {
            throw new IllegalArgumentException("Null CaretPolicy passed to TextLayout.getCaretShapes()");
        }

        Shape[] result = new Shape[2];

        TextHitInfo hit = TextHitInfo.afterOffset(offset);

        int hitCaret = hitToCaret(hit);

        LayoutPathImpl lp = textLine.getLayoutPath();
        Shape hitShape = pathToShape(getCaretPath(hit, bounds), false, lp);
        TextHitInfo otherHit = hit.getOtherHit();
        int otherCaret = hitToCaret(otherHit);

        if (hitCaret == otherCaret) {
            result[0] = hitShape;
        }
        else { // more than one caret
            Shape otherShape = pathToShape(getCaretPath(otherHit, bounds), false, lp);

            TextHitInfo strongHit = policy.getStrongCaret(hit, otherHit, this);
            boolean hitIsStrong = strongHit.equals(hit);

            if (hitIsStrong) {// then other is weak
                result[0] = hitShape;
                result[1] = otherShape;
            }
            else {
                result[0] = otherShape;
                result[1] = hitShape;
            }
        }

        return result;
    }

    /**
     * Returns two paths corresponding to the strong and weak caret.
     * This method is a convenience overload of {@code getCaretShapes}
     * that uses the default caret policy.
     * @param offset an offset in this {@code TextLayout}
     * @param bounds the bounds to which to extend the carets.  This is
     *     in baseline-relative coordinates.
     * @return two paths corresponding to the strong and weak caret as
     *    defined by the {@code DEFAULT_CARET_POLICY}.  These are
     *    in standard coordinates.
     */
    public Shape[] getCaretShapes(int offset, Rectangle2D bounds) {
        // {sfb} parameter checking is done in overloaded version
        return getCaretShapes(offset, bounds, DEFAULT_CARET_POLICY);
    }

    /**
     * Returns two paths corresponding to the strong and weak caret.
     * This method is a convenience overload of {@code getCaretShapes}
     * that uses the default caret policy and this {@code TextLayout}
     * object's natural bounds.
     * @param offset an offset in this {@code TextLayout}
     * @return two paths corresponding to the strong and weak caret as
     *    defined by the {@code DEFAULT_CARET_POLICY}.  These are
     *    in standard coordinates.
     */
    public Shape[] getCaretShapes(int offset) {
        // {sfb} parameter checking is done in overloaded version
        return getCaretShapes(offset, getNaturalBounds(), DEFAULT_CARET_POLICY);
    }

    // A utility to return a path enclosing the given path
    // Path0 must be left or top of path1
    // {jbr} no assumptions about size of path0, path1 anymore.
    private GeneralPath boundingShape(double[] path0, double[] path1) {

        // Really, we want the path to be a convex hull around all of the
        // points in path0 and path1.  But we can get by with less than
        // that.  We do need to prevent the two segments which
        // join path0 to path1 from crossing each other.  So, if we
        // traverse path0 from top to bottom, we'll traverse path1 from
        // bottom to top (and vice versa).

        GeneralPath result = pathToShape(path0, false, null);

        boolean sameDirection;

        if (isVerticalLine) {
            sameDirection = (path0[1] > path0[path0.length-1]) ==
                            (path1[1] > path1[path1.length-1]);
        }
        else {
            sameDirection = (path0[0] > path0[path0.length-2]) ==
                            (path1[0] > path1[path1.length-2]);
        }

        int start;
        int limit;
        int increment;

        if (sameDirection) {
            start = path1.length-2;
            limit = -2;
            increment = -2;
        }
        else {
            start = 0;
            limit = path1.length;
            increment = 2;
        }

        for (int i = start; i != limit; i += increment) {
            result.lineTo((float)path1[i], (float)path1[i+1]);
        }

        result.closePath();

        return result;
    }

    // A utility to convert a pair of carets into a bounding path
    // {jbr} Shape is never outside of bounds.
    private GeneralPath caretBoundingShape(int caret0,
                                           int caret1,
                                           Rectangle2D bounds) {

        if (caret0 > caret1) {
            int temp = caret0;
            caret0 = caret1;
            caret1 = temp;
        }

        return boundingShape(getCaretPath(caret0, bounds, true),
                             getCaretPath(caret1, bounds, true));
    }

    /*
     * A utility to return the path bounding the area to the left (top) of the
     * layout.
     * Shape is never outside of bounds.
     */
    private GeneralPath leftShape(Rectangle2D bounds) {

        double[] path0;
        if (isVerticalLine) {
            path0 = new double[] { bounds.getX(), bounds.getY(),
                                       bounds.getX() + bounds.getWidth(),
                                       bounds.getY() };
        } else {
            path0 = new double[] { bounds.getX(),
                                       bounds.getY() + bounds.getHeight(),
                                       bounds.getX(), bounds.getY() };
        }

        double[] path1 = getCaretPath(0, bounds, true);

        return boundingShape(path0, path1);
    }

    /*
     * A utility to return the path bounding the area to the right (bottom) of
     * the layout.
     */
    private GeneralPath rightShape(Rectangle2D bounds) {
        double[] path1;
        if (isVerticalLine) {
            path1 = new double[] {
                bounds.getX(),
                bounds.getY() + bounds.getHeight(),
                bounds.getX() + bounds.getWidth(),
                bounds.getY() + bounds.getHeight()
            };
        } else {
            path1 = new double[] {
                bounds.getX() + bounds.getWidth(),
                bounds.getY() + bounds.getHeight(),
                bounds.getX() + bounds.getWidth(),
                bounds.getY()
            };
        }

        double[] path0 = getCaretPath(characterCount, bounds, true);

        return boundingShape(path0, path1);
    }

    /**
     * Returns the logical ranges of text corresponding to a visual selection.
     * @param firstEndpoint an endpoint of the visual range
     * @param secondEndpoint the other endpoint of the visual range.
     * This endpoint can be less than {@code firstEndpoint}.
     * @return an array of integers representing start/limit pairs for the
     * selected ranges.
     * @see #getVisualHighlightShape(TextHitInfo, TextHitInfo, Rectangle2D)
     */
    public int[] getLogicalRangesForVisualSelection(TextHitInfo firstEndpoint,
                                                    TextHitInfo secondEndpoint) {
        ensureCache();

        checkTextHit(firstEndpoint);
        checkTextHit(secondEndpoint);

        // !!! probably want to optimize for all LTR text

        boolean[] included = new boolean[characterCount];

        int startIndex = hitToCaret(firstEndpoint);
        int limitIndex = hitToCaret(secondEndpoint);

        if (startIndex > limitIndex) {
            int t = startIndex;
            startIndex = limitIndex;
            limitIndex = t;
        }

        /*
         * now we have the visual indexes of the glyphs at the start and limit
         * of the selection range walk through runs marking characters that
         * were included in the visual range there is probably a more efficient
         * way to do this, but this ought to work, so hey
         */

        if (startIndex < limitIndex) {
            int visIndex = startIndex;
            while (visIndex < limitIndex) {
                included[textLine.visualToLogical(visIndex)] = true;
                ++visIndex;
            }
        }

        /*
         * count how many runs we have, ought to be one or two, but perhaps
         * things are especially weird
         */
        int count = 0;
        boolean inrun = false;
        for (int i = 0; i < characterCount; i++) {
            if (included[i] != inrun) {
                inrun = !inrun;
                if (inrun) {
                    count++;
                }
            }
        }

        int[] ranges = new int[count * 2];
        count = 0;
        inrun = false;
        for (int i = 0; i < characterCount; i++) {
            if (included[i] != inrun) {
                ranges[count++] = i;
                inrun = !inrun;
            }
        }
        if (inrun) {
            ranges[count++] = characterCount;
        }

        return ranges;
    }

    /**
     * Returns a path enclosing the visual selection in the specified range,
     * extended to {@code bounds}.
     * <p>
     * If the selection includes the leftmost (topmost) position, the selection
     * is extended to the left (top) of {@code bounds}.  If the
     * selection includes the rightmost (bottommost) position, the selection
     * is extended to the right (bottom) of the bounds.  The height
     * (width on vertical lines) of the selection is always extended to
     * {@code bounds}.
     * <p>
     * Although the selection is always contiguous, the logically selected
     * text can be discontiguous on lines with mixed-direction text.  The
     * logical ranges of text selected can be retrieved using
     * {@code getLogicalRangesForVisualSelection}.  For example,
     * consider the text 'ABCdef' where capital letters indicate
     * right-to-left text, rendered on a right-to-left line, with a visual
     * selection from 0L (the leading edge of 'A') to 3T (the trailing edge
     * of 'd').  The text appears as follows, with bold underlined areas
     * representing the selection:
     * <br><pre>
     *    d<u><b>efCBA  </b></u>
     * </pre>
     * The logical selection ranges are 0-3, 4-6 (ABC, ef) because the
     * visually contiguous text is logically discontiguous.  Also note that
     * since the rightmost position on the layout (to the right of 'A') is
     * selected, the selection is extended to the right of the bounds.
     * @param firstEndpoint one end of the visual selection
     * @param secondEndpoint the other end of the visual selection
     * @param bounds the bounding rectangle to which to extend the selection.
     *     This is in baseline-relative coordinates.
     * @return a {@code Shape} enclosing the selection.  This is in
     *     standard coordinates.
     * @see #getLogicalRangesForVisualSelection(TextHitInfo, TextHitInfo)
     * @see #getLogicalHighlightShape(int, int, Rectangle2D)
     */
    public Shape getVisualHighlightShape(TextHitInfo firstEndpoint,
                                        TextHitInfo secondEndpoint,
                                        Rectangle2D bounds)
    {
        ensureCache();

        checkTextHit(firstEndpoint);
        checkTextHit(secondEndpoint);

        if(bounds == null) {
                throw new IllegalArgumentException("Null Rectangle2D passed to TextLayout.getVisualHighlightShape()");
        }

        GeneralPath result = new GeneralPath(GeneralPath.WIND_EVEN_ODD);

        int firstCaret = hitToCaret(firstEndpoint);
        int secondCaret = hitToCaret(secondEndpoint);

        result.append(caretBoundingShape(firstCaret, secondCaret, bounds),
                      false);

        if (firstCaret == 0 || secondCaret == 0) {
            GeneralPath ls = leftShape(bounds);
            if (!ls.getBounds().isEmpty())
                result.append(ls, false);
        }

        if (firstCaret == characterCount || secondCaret == characterCount) {
            GeneralPath rs = rightShape(bounds);
            if (!rs.getBounds().isEmpty()) {
                result.append(rs, false);
            }
        }

        LayoutPathImpl lp = textLine.getLayoutPath();
        if (lp != null) {
            result = (GeneralPath)lp.mapShape(result); // dlf cast safe?
        }

        return  result;
    }

    /**
     * Returns a {@code Shape} enclosing the visual selection in the
     * specified range, extended to the bounds.  This method is a
     * convenience overload of {@code getVisualHighlightShape} that
     * uses the natural bounds of this {@code TextLayout}.
     * @param firstEndpoint one end of the visual selection
     * @param secondEndpoint the other end of the visual selection
     * @return a {@code Shape} enclosing the selection.  This is
     *     in standard coordinates.
     */
    public Shape getVisualHighlightShape(TextHitInfo firstEndpoint,
                                             TextHitInfo secondEndpoint) {
        return getVisualHighlightShape(firstEndpoint, secondEndpoint, getNaturalBounds());
    }

    /**
     * Returns a {@code Shape} enclosing the logical selection in the
     * specified range, extended to the specified {@code bounds}.
     * <p>
     * If the selection range includes the first logical character, the
     * selection is extended to the portion of {@code bounds} before
     * the start of this {@code TextLayout}.  If the range includes
     * the last logical character, the selection is extended to the portion
     * of {@code bounds} after the end of this {@code TextLayout}.
     * The height (width on vertical lines) of the selection is always
     * extended to {@code bounds}.
     * <p>
     * The selection can be discontiguous on lines with mixed-direction text.
     * Only those characters in the logical range between start and limit
     * appear selected.  For example, consider the text 'ABCdef' where capital
     * letters indicate right-to-left text, rendered on a right-to-left line,
     * with a logical selection from 0 to 4 ('ABCd').  The text appears as
     * follows, with bold standing in for the selection, and underlining for
     * the extension:
     * <br><pre>
     *    <u><b>d</b></u>ef<u><b>CBA  </b></u>
     * </pre>
     * The selection is discontiguous because the selected characters are
     * visually discontiguous. Also note that since the range includes the
     * first logical character (A), the selection is extended to the portion
     * of the {@code bounds} before the start of the layout, which in
     * this case (a right-to-left line) is the right portion of the
     * {@code bounds}.
     * @param firstEndpoint an endpoint in the range of characters to select
     * @param secondEndpoint the other endpoint of the range of characters
     * to select. Can be less than {@code firstEndpoint}.  The range
     * includes the character at min(firstEndpoint, secondEndpoint), but
     * excludes max(firstEndpoint, secondEndpoint).
     * @param bounds the bounding rectangle to which to extend the selection.
     *     This is in baseline-relative coordinates.
     * @return an area enclosing the selection.  This is in standard
     *     coordinates.
     * @see #getVisualHighlightShape(TextHitInfo, TextHitInfo, Rectangle2D)
     */
    public Shape getLogicalHighlightShape(int firstEndpoint,
                                         int secondEndpoint,
                                         Rectangle2D bounds) {
        if (bounds == null) {
            throw new IllegalArgumentException("Null Rectangle2D passed to TextLayout.getLogicalHighlightShape()");
        }

        ensureCache();

        if (firstEndpoint > secondEndpoint) {
            int t = firstEndpoint;
            firstEndpoint = secondEndpoint;
            secondEndpoint = t;
        }

        if(firstEndpoint < 0 || secondEndpoint > characterCount) {
            throw new IllegalArgumentException("Range is invalid in TextLayout.getLogicalHighlightShape()");
        }

        GeneralPath result = new GeneralPath(GeneralPath.WIND_EVEN_ODD);

        int[] carets = new int[10]; // would this ever not handle all cases?
        int count = 0;

        if (firstEndpoint < secondEndpoint) {
            int logIndex = firstEndpoint;
            do {
                carets[count++] = hitToCaret(TextHitInfo.leading(logIndex));
                boolean ltr = textLine.isCharLTR(logIndex);

                do {
                    logIndex++;
                } while (logIndex < secondEndpoint && textLine.isCharLTR(logIndex) == ltr);

                int hitCh = logIndex;
                carets[count++] = hitToCaret(TextHitInfo.trailing(hitCh - 1));

                if (count == carets.length) {
                    int[] temp = new int[carets.length + 10];
                    System.arraycopy(carets, 0, temp, 0, count);
                    carets = temp;
                }
            } while (logIndex < secondEndpoint);
        }
        else {
            count = 2;
            carets[0] = carets[1] = hitToCaret(TextHitInfo.leading(firstEndpoint));
        }

        // now create paths for pairs of carets

        for (int i = 0; i < count; i += 2) {
            result.append(caretBoundingShape(carets[i], carets[i+1], bounds),
                          false);
        }

        if (firstEndpoint != secondEndpoint) {
            if ((textLine.isDirectionLTR() && firstEndpoint == 0) || (!textLine.isDirectionLTR() &&
                                                                      secondEndpoint == characterCount)) {
                GeneralPath ls = leftShape(bounds);
                if (!ls.getBounds().isEmpty()) {
                    result.append(ls, false);
                }
            }

            if ((textLine.isDirectionLTR() && secondEndpoint == characterCount) ||
                (!textLine.isDirectionLTR() && firstEndpoint == 0)) {

                GeneralPath rs = rightShape(bounds);
                if (!rs.getBounds().isEmpty()) {
                    result.append(rs, false);
                }
            }
        }

        LayoutPathImpl lp = textLine.getLayoutPath();
        if (lp != null) {
            result = (GeneralPath)lp.mapShape(result); // dlf cast safe?
        }
        return result;
    }

    /**
     * Returns a {@code Shape} enclosing the logical selection in the
     * specified range, extended to the natural bounds of this
     * {@code TextLayout}.  This method is a convenience overload of
     * {@code getLogicalHighlightShape} that uses the natural bounds of
     * this {@code TextLayout}.
     * @param firstEndpoint an endpoint in the range of characters to select
     * @param secondEndpoint the other endpoint of the range of characters
     * to select. Can be less than {@code firstEndpoint}.  The range
     * includes the character at min(firstEndpoint, secondEndpoint), but
     * excludes max(firstEndpoint, secondEndpoint).
     * @return a {@code Shape} enclosing the selection.  This is in
     *     standard coordinates.
     */
    public Shape getLogicalHighlightShape(int firstEndpoint, int secondEndpoint) {

        return getLogicalHighlightShape(firstEndpoint, secondEndpoint, getNaturalBounds());
    }

    /**
     * Returns the black box bounds of the characters in the specified range.
     * The black box bounds is an area consisting of the union of the bounding
     * boxes of all the glyphs corresponding to the characters between start
     * and limit.  This area can be disjoint.
     * @param firstEndpoint one end of the character range
     * @param secondEndpoint the other end of the character range.  Can be
     * less than {@code firstEndpoint}.
     * @return a {@code Shape} enclosing the black box bounds.  This is
     *     in standard coordinates.
     */
    public Shape getBlackBoxBounds(int firstEndpoint, int secondEndpoint) {
        ensureCache();

        if (firstEndpoint > secondEndpoint) {
            int t = firstEndpoint;
            firstEndpoint = secondEndpoint;
            secondEndpoint = t;
        }

        if (firstEndpoint < 0 || secondEndpoint > characterCount) {
            throw new IllegalArgumentException("Invalid range passed to TextLayout.getBlackBoxBounds()");
        }

        /*
         * return an area that consists of the bounding boxes of all the
         * characters from firstEndpoint to limit
         */

        GeneralPath result = new GeneralPath(GeneralPath.WIND_NON_ZERO);

        if (firstEndpoint < characterCount) {
            for (int logIndex = firstEndpoint;
                        logIndex < secondEndpoint;
                        logIndex++) {

                Rectangle2D r = textLine.getCharBounds(logIndex);
                if (!r.isEmpty()) {
                    result.append(r, false);
                }
            }
        }

        if (dx != 0 || dy != 0) {
            AffineTransform tx = AffineTransform.getTranslateInstance(dx, dy);
            result = (GeneralPath)tx.createTransformedShape(result);
        }
        LayoutPathImpl lp = textLine.getLayoutPath();
        if (lp != null) {
            result = (GeneralPath)lp.mapShape(result);
        }

        //return new Highlight(result, false);
        return result;
    }

    /**
     * Returns the distance from the point (x,&nbsp;y) to the caret along
     * the line direction defined in {@code caretInfo}.  Distance is
     * negative if the point is to the left of the caret on a horizontal
     * line, or above the caret on a vertical line.
     * Utility for use by hitTestChar.
     */
    private float caretToPointDistance(float[] caretInfo, float x, float y) {
        // distanceOffBaseline is negative if you're 'above' baseline

        float lineDistance = isVerticalLine? y : x;
        float distanceOffBaseline = isVerticalLine? -x : y;

        return lineDistance - caretInfo[0] +
            (distanceOffBaseline*caretInfo[1]);
    }

    /**
     * Returns a {@code TextHitInfo} corresponding to the
     * specified point.
     * Coordinates outside the bounds of the {@code TextLayout}
     * map to hits on the leading edge of the first logical character,
     * or the trailing edge of the last logical character, as appropriate,
     * regardless of the position of that character in the line.  Only the
     * direction along the baseline is used to make this evaluation.
     * @param x the x offset from the origin of this
     *     {@code TextLayout}.  This is in standard coordinates.
     * @param y the y offset from the origin of this
     *     {@code TextLayout}.  This is in standard coordinates.
     * @param bounds the bounds of the {@code TextLayout}.  This
     *     is in baseline-relative coordinates.
     * @return a hit describing the character and edge (leading or trailing)
     *     under the specified point.
     */
    public TextHitInfo hitTestChar(float x, float y, Rectangle2D bounds) {
        // check boundary conditions

        LayoutPathImpl lp = textLine.getLayoutPath();
        boolean prev = false;
        if (lp != null) {
            Point2D.Float pt = new Point2D.Float(x, y);
            prev = lp.pointToPath(pt, pt);
            x = pt.x;
            y = pt.y;
        }

        if (isVertical()) {
            if (y < bounds.getMinY()) {
                return TextHitInfo.leading(0);
            } else if (y >= bounds.getMaxY()) {
                return TextHitInfo.trailing(characterCount-1);
            }
        } else {
            if (x < bounds.getMinX()) {
                return isLeftToRight() ? TextHitInfo.leading(0) : TextHitInfo.trailing(characterCount-1);
            } else if (x >= bounds.getMaxX()) {
                return isLeftToRight() ? TextHitInfo.trailing(characterCount-1) : TextHitInfo.leading(0);
            }
        }

        // revised hit test
        // the original seems too complex and fails miserably with italic offsets
        // the natural tendency is to move towards the character you want to hit
        // so we'll just measure distance to the center of each character's visual
        // bounds, pick the closest one, then see which side of the character's
        // center line (italic) the point is on.
        // this tends to make it easier to hit narrow characters, which can be a
        // bit odd if you're visually over an adjacent wide character. this makes
        // a difference with bidi, so perhaps i need to revisit this yet again.

        double distance = Double.MAX_VALUE;
        int index = 0;
        int trail = -1;
        CoreMetrics lcm = null;
        float icx = 0, icy = 0, ia = 0, cy = 0, dya = 0, ydsq = 0;

        for (int i = 0; i < characterCount; ++i) {
            if (!textLine.caretAtOffsetIsValid(i)) {
                continue;
            }
            if (trail == -1) {
                trail = i;
            }
            CoreMetrics cm = textLine.getCoreMetricsAt(i);
            if (cm != lcm) {
                lcm = cm;
                // just work around baseline mess for now
                if (cm.baselineIndex == GraphicAttribute.TOP_ALIGNMENT) {
                    cy = -(textLine.getMetrics().ascent - cm.ascent) + cm.ssOffset;
                } else if (cm.baselineIndex == GraphicAttribute.BOTTOM_ALIGNMENT) {
                    cy = textLine.getMetrics().descent - cm.descent + cm.ssOffset;
                } else {
                    cy = cm.effectiveBaselineOffset(baselineOffsets) + cm.ssOffset;
                }
                float dy = (cm.descent - cm.ascent) / 2 - cy;
                dya = dy * cm.italicAngle;
                cy += dy;
                ydsq = (cy - y)*(cy - y);
            }
            float cx = textLine.getCharXPosition(i);
            float ca = textLine.getCharAdvance(i);
            float dx = ca / 2;
            cx += dx - dya;

            // proximity in x (along baseline) is two times as important as proximity in y
            double nd = Math.sqrt(4*(cx - x)*(cx - x) + ydsq);
            if (nd < distance) {
                distance = nd;
                index = i;
                trail = -1;
                icx = cx; icy = cy; ia = cm.italicAngle;
            }
        }
        boolean left = x < icx - (y - icy) * ia;
        boolean leading = textLine.isCharLTR(index) == left;
        if (trail == -1) {
            trail = characterCount;
        }
        TextHitInfo result = leading ? TextHitInfo.leading(index) :
            TextHitInfo.trailing(trail-1);
        return result;
    }

    /**
     * Returns a {@code TextHitInfo} corresponding to the
     * specified point.  This method is a convenience overload of
     * {@code hitTestChar} that uses the natural bounds of this
     * {@code TextLayout}.
     * @param x the x offset from the origin of this
     *     {@code TextLayout}.  This is in standard coordinates.
     * @param y the y offset from the origin of this
     *     {@code TextLayout}.  This is in standard coordinates.
     * @return a hit describing the character and edge (leading or trailing)
     * under the specified point.
     */
    public TextHitInfo hitTestChar(float x, float y) {

        return hitTestChar(x, y, getNaturalBounds());
    }

    /**
     * Returns {@code true} if the two layouts are equal.
     * Obeys the general contract of {@link java.lang.Object equals(Object)}.
     * @param rhs the {@code TextLayout} to compare to this
     *       {@code TextLayout}
     * @return {@code true} if the specified {@code TextLayout}
     *      equals this {@code TextLayout}.
     *
     */
    public boolean equals(TextLayout rhs) {
        return equals((Object)rhs);
    }

    /**
     * Returns debugging information for this {@code TextLayout}.
     * @return the {@code textLine} of this {@code TextLayout}
     *        as a {@code String}.
     */
    public String toString() {
        ensureCache();
        return textLine.toString();
     }

    /**
     * Renders this {@code TextLayout} at the specified location in
     * the specified {@link java.awt.Graphics2D Graphics2D} context.
     * The origin of the layout is placed at x,&nbsp;y.  Rendering may touch
     * any point within {@code getBounds()} of this position.  This
     * leaves the {@code g2} unchanged.  Text is rendered along the
     * baseline path.
     * @param g2 the {@code Graphics2D} context into which to render
     *         the layout
     * @param x the X coordinate of the origin of this {@code TextLayout}
     * @param y the Y coordinate of the origin of this {@code TextLayout}
     * @see #getBounds()
     */
    public void draw(Graphics2D g2, float x, float y) {

        if (g2 == null) {
            throw new IllegalArgumentException("Null Graphics2D passed to TextLayout.draw()");
        }

        textLine.draw(g2, x - dx, y - dy);
    }

    /**
     * Package-only method for testing ONLY.  Please don't abuse.
     */
    TextLine getTextLineForTesting() {

        return textLine;
    }

    /**
     *
     * Return the index of the first character with a different baseline from the
     * character at start, or limit if all characters between start and limit have
     * the same baseline.
     */
    private static int sameBaselineUpTo(Font font, char[] text,
                                        int start, int limit) {
        // current implementation doesn't support multiple baselines
        return limit;
        /*
        byte bl = font.getBaselineFor(text[start++]);
        while (start < limit && font.getBaselineFor(text[start]) == bl) {
            ++start;
        }
        return start;
        */
    }

    static byte getBaselineFromGraphic(GraphicAttribute graphic) {

        byte alignment = (byte) graphic.getAlignment();

        if (alignment == GraphicAttribute.BOTTOM_ALIGNMENT ||
                alignment == GraphicAttribute.TOP_ALIGNMENT) {

            return (byte)GraphicAttribute.ROMAN_BASELINE;
        }
        else {
            return alignment;
        }
    }

    /**
     * Returns a {@code Shape} representing the outline of this
     * {@code TextLayout}.
     * @param tx an optional {@link AffineTransform} to apply to the
     *     outline of this {@code TextLayout}.
     * @return a {@code Shape} that is the outline of this
     *     {@code TextLayout}.  This is in standard coordinates.
     */
    public Shape getOutline(AffineTransform tx) {
        ensureCache();
        Shape result = textLine.getOutline(tx);
        LayoutPathImpl lp = textLine.getLayoutPath();
        if (lp != null) {
            result = lp.mapShape(result);
        }
        return result;
    }

    /**
     * Return the LayoutPath, or null if the layout path is the
     * default path (x maps to advance, y maps to offset).
     * @return the layout path
     * @since 1.6
     */
    public LayoutPath getLayoutPath() {
        return textLine.getLayoutPath();
    }

   /**
     * Convert a hit to a point in standard coordinates.  The point is
     * on the baseline of the character at the leading or trailing
     * edge of the character, as appropriate.  If the path is
     * broken at the side of the character represented by the hit, the
     * point will be adjacent to the character.
     * @param hit the hit to check.  This must be a valid hit on
     * the TextLayout.
     * @param point the returned point. The point is in standard
     *     coordinates.
     * @throws IllegalArgumentException if the hit is not valid for the
     * TextLayout.
     * @throws NullPointerException if hit or point is null.
     * @since 1.6
     */
    public void hitToPoint(TextHitInfo hit, Point2D point) {
        if (hit == null || point == null) {
            throw new NullPointerException((hit == null ? "hit" : "point") +
                                           " can't be null");
        }
        ensureCache();
        checkTextHit(hit);

        float adv = 0;
        float off = 0;

        int ix = hit.getCharIndex();
        boolean leading = hit.isLeadingEdge();
        boolean ltr;
        if (ix == -1 || ix == textLine.characterCount()) {
            ltr = textLine.isDirectionLTR();
            adv = (ltr == (ix == -1)) ? 0 : lineMetrics.advance;
        } else {
            ltr = textLine.isCharLTR(ix);
            adv = textLine.getCharLinePosition(ix, leading);
            off = textLine.getCharYPosition(ix);
        }
        point.setLocation(adv, off);
        LayoutPath lp = textLine.getLayoutPath();
        if (lp != null) {
            lp.pathToPoint(point, ltr != leading, point);
        }
    }
}
