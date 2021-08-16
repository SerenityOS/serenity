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
 * (C) Copyright IBM Corp. 1996 - 1998, All Rights Reserved
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

import java.awt.geom.Rectangle2D;

/**
 * The {@code GlyphMetrics} class represents information for a
 * single glyph.   A glyph is the visual representation of one or more
 * characters.  Many different glyphs can be used to represent a single
 * character or combination of characters.  {@code GlyphMetrics}
 * instances are produced by {@link java.awt.Font Font} and are applicable
 * to a specific glyph in a particular {@code Font}.
 * <p>
 * Glyphs are either STANDARD, LIGATURE, COMBINING, or COMPONENT.
 * <ul>
 * <li>STANDARD glyphs are commonly used to represent single characters.
 * <li>LIGATURE glyphs are used to represent sequences of characters.
 * <li>COMPONENT glyphs in a {@link GlyphVector} do not correspond to a
 * particular character in a text model. Instead, COMPONENT glyphs are
 * added for typographical reasons, such as Arabic justification.
 * <li>COMBINING glyphs embellish STANDARD or LIGATURE glyphs, such
 * as accent marks.  Carets do not appear before COMBINING glyphs.
 * </ul>
 * <p>
 * Other metrics available through {@code GlyphMetrics} are the
 * components of the advance, the visual bounds, and the left and right
 * side bearings.
 * <p>
 * Glyphs for a rotated font, or obtained from a {@code GlyphVector}
 * which has applied a rotation to the glyph, can have advances that
 * contain both X and Y components.  Usually the advance only has one
 * component.
 * <p>
 * The advance of a glyph is the distance from the glyph's origin to the
 * origin of the next glyph along the baseline, which is either vertical
 * or horizontal.  Note that, in a {@code GlyphVector},
 * the distance from a glyph to its following glyph might not be the
 * glyph's advance, because of kerning or other positioning adjustments.
 * <p>
 * The bounds is the smallest rectangle that completely contains the
 * outline of the glyph.  The bounds rectangle is relative to the
 * glyph's origin.  The left-side bearing is the distance from the glyph
 * origin to the left of its bounds rectangle. If the left-side bearing is
 * negative, part of the glyph is drawn to the left of its origin.  The
 * right-side bearing is the distance from the right side of the bounds
 * rectangle to the next glyph origin (the origin plus the advance).  If
 * negative, part of the glyph is drawn to the right of the next glyph's
 * origin.  Note that the bounds does not necessarily enclose all the pixels
 * affected when rendering the glyph, because of rasterization and pixel
 * adjustment effects.
 * <p>
 * Although instances of {@code GlyphMetrics} can be directly
 * constructed, they are almost always obtained from a
 * {@code GlyphVector}.  Once constructed, {@code GlyphMetrics}
 * objects are immutable.
 * <p>
 * <strong>Example</strong>:<p>
 * Querying a {@code Font} for glyph information
 * <blockquote><pre>
 * Font font = ...;
 * int glyphIndex = ...;
 * GlyphMetrics metrics = GlyphVector.getGlyphMetrics(glyphIndex);
 * int isStandard = metrics.isStandard();
 * float glyphAdvance = metrics.getAdvance();
 * </pre></blockquote>
 * @see java.awt.Font
 * @see GlyphVector
 */

public final class GlyphMetrics {
    /**
     * Indicates whether the metrics are for a horizontal or vertical baseline.
     */
    private boolean horizontal;

    /**
     * The x-component of the advance.
     */
    private float advanceX;

    /**
     * The y-component of the advance.
     */
    private float advanceY;

    /**
     * The bounds of the associated glyph.
     */
    private Rectangle2D.Float bounds;

    /**
     * Additional information about the glyph encoded as a byte.
     */
    private byte glyphType;

    /**
     * Indicates a glyph that represents a single standard
     * character.
     */
    public static final byte STANDARD = 0;

    /**
     * Indicates a glyph that represents multiple characters
     * as a ligature, for example 'fi' or 'ffi'.  It is followed by
     * filler glyphs for the remaining characters. Filler and combining
     * glyphs can be intermixed to control positioning of accent marks
     * on the logically preceding ligature.
     */
    public static final byte LIGATURE = 1;

    /**
     * Indicates a glyph that represents a combining character,
     * such as an umlaut.  There is no caret position between this glyph
     * and the preceding glyph.
     */
    public static final byte COMBINING = 2;

    /**
     * Indicates a glyph with no corresponding character in the
     * backing store.  The glyph is associated with the character
     * represented by the logically preceding non-component glyph.  This
     * is used for kashida justification or other visual modifications to
     * existing glyphs.  There is no caret position between this glyph
     * and the preceding glyph.
     */
    public static final byte COMPONENT = 3;

    /**
     * Indicates a glyph with no visual representation. It can
     * be added to the other code values to indicate an invisible glyph.
     */
    public static final byte WHITESPACE = 4;

    /**
     * Constructs a {@code GlyphMetrics} object.
     * @param advance the advance width of the glyph
     * @param bounds the black box bounds of the glyph
     * @param glyphType the type of the glyph
     */
    public GlyphMetrics(float advance, Rectangle2D bounds, byte glyphType) {
        this.horizontal = true;
        this.advanceX = advance;
        this.advanceY = 0;
        this.bounds = new Rectangle2D.Float();
        this.bounds.setRect(bounds);
        this.glyphType = glyphType;
    }

    /**
     * Constructs a {@code GlyphMetrics} object.
     * @param horizontal if true, metrics are for a horizontal baseline,
     *   otherwise they are for a vertical baseline
     * @param advanceX the X-component of the glyph's advance
     * @param advanceY the Y-component of the glyph's advance
     * @param bounds the visual bounds of the glyph
     * @param glyphType the type of the glyph
     * @since 1.4
     */
    public GlyphMetrics(boolean horizontal, float advanceX, float advanceY,
                        Rectangle2D bounds, byte glyphType) {

        this.horizontal = horizontal;
        this.advanceX = advanceX;
        this.advanceY = advanceY;
        this.bounds = new Rectangle2D.Float();
        this.bounds.setRect(bounds);
        this.glyphType = glyphType;
    }

    /**
     * Returns the advance of the glyph along the baseline (either
     * horizontal or vertical).
     * @return the advance of the glyph
     */
    public float getAdvance() {
        return horizontal ? advanceX : advanceY;
    }

    /**
     * Returns the x-component of the advance of the glyph.
     * @return the x-component of the advance of the glyph
     * @since 1.4
     */
    public float getAdvanceX() {
        return advanceX;
    }

    /**
     * Returns the y-component of the advance of the glyph.
     * @return the y-component of the advance of the glyph
     * @since 1.4
     */
    public float getAdvanceY() {
        return advanceY;
    }

    /**
     * Returns the bounds of the glyph. This is the bounding box of the glyph outline.
     * Because of rasterization and pixel alignment effects, it does not necessarily
     * enclose the pixels that are affected when rendering the glyph.
     * @return a {@link Rectangle2D} that is the bounds of the glyph.
     */
    public Rectangle2D getBounds2D() {
        return new Rectangle2D.Float(bounds.x, bounds.y, bounds.width, bounds.height);
    }

    /**
     * Returns the left (top) side bearing of the glyph.
     * <p>
     * This is the distance from 0,&nbsp;0 to the left (top) of the glyph
     * bounds.  If the bounds of the glyph is to the left of (above) the
     * origin, the LSB is negative.
     * @return the left side bearing of the glyph.
     */
    public float getLSB() {
        return horizontal ? bounds.x : bounds.y;
    }

    /**
     * Returns the right (bottom) side bearing of the glyph.
     * <p>
     * This is the distance from the right (bottom) of the glyph bounds to
     * the advance. If the bounds of the glyph is to the right of (below)
     * the advance, the RSB is negative.
     * @return the right side bearing of the glyph.
     */
    public float getRSB() {
        return horizontal ?
            advanceX - bounds.x - bounds.width :
            advanceY - bounds.y - bounds.height;
    }

    /**
     * Returns the raw glyph type code.
     * @return the raw glyph type code.
     */
    public int getType() {
        return glyphType;
    }

    /**
     * Returns {@code true} if this is a standard glyph.
     * @return {@code true} if this is a standard glyph;
     *          {@code false} otherwise.
     */
    public boolean isStandard() {
        return (glyphType & 0x3) == STANDARD;
    }

    /**
     * Returns {@code true} if this is a ligature glyph.
     * @return {@code true} if this is a ligature glyph;
     *          {@code false} otherwise.
     */
    public boolean isLigature() {
        return (glyphType & 0x3) == LIGATURE;
    }

    /**
     * Returns {@code true} if this is a combining glyph.
     * @return {@code true} if this is a combining glyph;
     *          {@code false} otherwise.
     */
    public boolean isCombining() {
        return (glyphType & 0x3) == COMBINING;
    }

    /**
     * Returns {@code true} if this is a component glyph.
     * @return {@code true} if this is a component glyph;
     *          {@code false} otherwise.
     */
    public boolean isComponent() {
        return (glyphType & 0x3) == COMPONENT;
    }

    /**
     * Returns {@code true} if this is a whitespace glyph.
     * @return {@code true} if this is a whitespace glyph;
     *          {@code false} otherwise.
     */
    public boolean isWhitespace() {
        return (glyphType & 0x4) == WHITESPACE;
    }
}
