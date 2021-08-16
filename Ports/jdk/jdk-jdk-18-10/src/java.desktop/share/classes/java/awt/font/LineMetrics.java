/*
 * Copyright (c) 1998, 2020, Oracle and/or its affiliates. All rights reserved.
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

package java.awt.font;

/**
* The {@code LineMetrics} class allows access to the
* metrics needed to layout characters along a line
* and to layout of a set of lines.  A {@code LineMetrics}
* object encapsulates the measurement information associated
* with a run of text.
* <p>
* Fonts can have different metrics for different ranges of
* characters.  The {@code getLineMetrics} methods of
* {@link java.awt.Font Font} take some text as an argument
* and return a {@code LineMetrics} object describing the
* metrics of the initial number of characters in that text, as
* returned by {@link #getNumChars}.
*/


public abstract class LineMetrics {

    /**
     * Constructor for subclasses to call.
     */
    protected LineMetrics() {}

    /**
     * Returns the number of characters ({@code char} values) in the text whose
     * metrics are encapsulated by this {@code LineMetrics}
     * object.
     * @return the number of characters ({@code char} values) in the text with which
     *         this {@code LineMetrics} was created.
     */
    public abstract int getNumChars();

    /**
     * Returns the ascent of the text.  The ascent
     * is the distance from the baseline
     * to the ascender line.  The ascent usually represents the
     * the height of the capital letters of the text.  Some characters
     * can extend above the ascender line.
     * @return the ascent of the text.
     */
    public abstract float getAscent();

    /**
     * Returns the descent of the text.  The descent
     * is the distance from the baseline
     * to the descender line.  The descent usually represents
     * the distance to the bottom of lower case letters like
     * 'p'.  Some characters can extend below the descender
     * line.
     * @return the descent of the text.
     */
    public abstract float getDescent();

    /**
     * Returns the leading of the text. The
     * leading is the recommended
     * distance from the bottom of the descender line to the
     * top of the next line.
     * @return the leading of the text.
     */
    public abstract float getLeading();

    /**
     * Returns the height of the text.  The
     * height is equal to the sum of the ascent, the
     * descent and the leading.
     * @return the height of the text.
     */
    public abstract float getHeight();

    /**
     * Returns the baseline index of the text.
     * The index is one of
     * {@link java.awt.Font#ROMAN_BASELINE ROMAN_BASELINE},
     * {@link java.awt.Font#CENTER_BASELINE CENTER_BASELINE},
     * {@link java.awt.Font#HANGING_BASELINE HANGING_BASELINE}.
     * @return the baseline of the text.
     */
    public abstract int getBaselineIndex();

    /**
     * Returns the baseline offsets of the text,
     * relative to the baseline of the text.  The
     * offsets are indexed by baseline index.  For
     * example, if the baseline index is
     * {@code CENTER_BASELINE} then
     * {@code offsets[HANGING_BASELINE]} is usually
     * negative, {@code offsets[CENTER_BASELINE]}
     * is zero, and {@code offsets[ROMAN_BASELINE]}
     * is usually positive.
     * @return the baseline offsets of the text.
     */
    public abstract float[] getBaselineOffsets();

    /**
     * Returns the position of the strike-through line
     * relative to the baseline.
     * @return the position of the strike-through line.
     */
    public abstract float getStrikethroughOffset();

    /**
     * Returns the thickness of the strike-through line.
     * @return the thickness of the strike-through line.
     */
    public abstract float getStrikethroughThickness();

    /**
     * Returns the position of the underline relative to
     * the baseline.
     * @return the position of the underline.
     */
    public abstract float getUnderlineOffset();

    /**
     * Returns the thickness of the underline.
     * @return the thickness of the underline.
     */
    public abstract float getUnderlineThickness();
}
