/*
 * Copyright (c) 1998, 2013, Oracle and/or its affiliates. All rights reserved.
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

import java.text.BreakIterator;
import java.text.CharacterIterator;
import java.text.AttributedCharacterIterator;
import java.awt.font.FontRenderContext;

/**
 * The {@code LineBreakMeasurer} class allows styled text to be
 * broken into lines (or segments) that fit within a particular visual
 * advance.  This is useful for clients who wish to display a paragraph of
 * text that fits within a specific width, called the <b>wrapping
 * width</b>.
 * <p>
 * {@code LineBreakMeasurer} is constructed with an iterator over
 * styled text.  The iterator's range should be a single paragraph in the
 * text.
 * {@code LineBreakMeasurer} maintains a position in the text for the
 * start of the next text segment.  Initially, this position is the
 * start of text.  Paragraphs are assigned an overall direction (either
 * left-to-right or right-to-left) according to the bidirectional
 * formatting rules.  All segments obtained from a paragraph have the
 * same direction as the paragraph.
 * <p>
 * Segments of text are obtained by calling the method
 * {@code nextLayout}, which returns a {@link TextLayout}
 * representing the text that fits within the wrapping width.
 * The {@code nextLayout} method moves the current position
 * to the end of the layout returned from {@code nextLayout}.
 * <p>
 * {@code LineBreakMeasurer} implements the most commonly used
 * line-breaking policy: Every word that fits within the wrapping
 * width is placed on the line. If the first word does not fit, then all
 * of the characters that fit within the wrapping width are placed on the
 * line.  At least one character is placed on each line.
 * <p>
 * The {@code TextLayout} instances returned by
 * {@code LineBreakMeasurer} treat tabs like 0-width spaces.  Clients
 * who wish to obtain tab-delimited segments for positioning should use
 * the overload of {@code nextLayout} which takes a limiting offset
 * in the text.
 * The limiting offset should be the first character after the tab.
 * The {@code TextLayout} objects returned from this method end
 * at the limit provided (or before, if the text between the current
 * position and the limit won't fit entirely within the  wrapping
 * width).
 * <p>
 * Clients who are laying out tab-delimited text need a slightly
 * different line-breaking policy after the first segment has been
 * placed on a line.  Instead of fitting partial words in the
 * remaining space, they should place words which don't fit in the
 * remaining space entirely on the next line.  This change of policy
 * can be requested in the overload of {@code nextLayout} which
 * takes a {@code boolean} parameter.  If this parameter is
 * {@code true}, {@code nextLayout} returns
 * {@code null} if the first word won't fit in
 * the given space.  See the tab sample below.
 * <p>
 * In general, if the text used to construct the
 * {@code LineBreakMeasurer} changes, a new
 * {@code LineBreakMeasurer} must be constructed to reflect
 * the change.  (The old {@code LineBreakMeasurer} continues to
 * function properly, but it won't be aware of the text change.)
 * Nevertheless, if the text change is the insertion or deletion of a
 * single character, an existing {@code LineBreakMeasurer} can be
 * 'updated' by calling {@code insertChar} or
 * {@code deleteChar}. Updating an existing
 * {@code LineBreakMeasurer} is much faster than creating a new one.
 * Clients who modify text based on user typing should take advantage
 * of these methods.
 * <p>
 * <strong>Examples</strong>:<p>
 * Rendering a paragraph in a component
 * <blockquote>
 * <pre>{@code
 * public void paint(Graphics graphics) {
 *
 *     float dx = 0f, dy = 5f;
 *     Graphics2D g2d = (Graphics2D)graphics;
 *     FontRenderContext frc = g2d.getFontRenderContext();
 *
 *     AttributedString text = new AttributedString(".....");
 *     AttributedCharacterIterator paragraph = text.getIterator();
 *
 *     LineBreakMeasurer measurer = new LineBreakMeasurer(paragraph, frc);
 *     measurer.setPosition(paragraph.getBeginIndex());
 *     float wrappingWidth = (float)getSize().width;
 *
 *     while (measurer.getPosition() < paragraph.getEndIndex()) {
 *
 *         TextLayout layout = measurer.nextLayout(wrappingWidth);
 *
 *         dy += (layout.getAscent());
 *         float dx = layout.isLeftToRight() ?
 *             0 : (wrappingWidth - layout.getAdvance());
 *
 *         layout.draw(graphics, dx, dy);
 *         dy += layout.getDescent() + layout.getLeading();
 *     }
 * }
 * }</pre>
 * </blockquote>
 * <p>
 * Rendering text with tabs.  For simplicity, the overall text
 * direction is assumed to be left-to-right
 * <blockquote>
 * <pre>{@code
 * public void paint(Graphics graphics) {
 *
 *     float leftMargin = 10, rightMargin = 310;
 *     float[] tabStops = { 100, 250 };
 *
 *     // assume styledText is an AttributedCharacterIterator, and the number
 *     // of tabs in styledText is tabCount
 *
 *     int[] tabLocations = new int[tabCount+1];
 *
 *     int i = 0;
 *     for (char c = styledText.first(); c != styledText.DONE; c = styledText.next()) {
 *         if (c == '\t') {
 *             tabLocations[i++] = styledText.getIndex();
 *         }
 *     }
 *     tabLocations[tabCount] = styledText.getEndIndex() - 1;
 *
 *     // Now tabLocations has an entry for every tab's offset in
 *     // the text.  For convenience, the last entry is tabLocations
 *     // is the offset of the last character in the text.
 *
 *     LineBreakMeasurer measurer = new LineBreakMeasurer(styledText);
 *     int currentTab = 0;
 *     float verticalPos = 20;
 *
 *     while (measurer.getPosition() < styledText.getEndIndex()) {
 *
 *         // Lay out and draw each line.  All segments on a line
 *         // must be computed before any drawing can occur, since
 *         // we must know the largest ascent on the line.
 *         // TextLayouts are computed and stored in a Vector;
 *         // their horizontal positions are stored in a parallel
 *         // Vector.
 *
 *         // lineContainsText is true after first segment is drawn
 *         boolean lineContainsText = false;
 *         boolean lineComplete = false;
 *         float maxAscent = 0, maxDescent = 0;
 *         float horizontalPos = leftMargin;
 *         Vector layouts = new Vector(1);
 *         Vector penPositions = new Vector(1);
 *
 *         while (!lineComplete) {
 *             float wrappingWidth = rightMargin - horizontalPos;
 *             TextLayout layout =
 *                     measurer.nextLayout(wrappingWidth,
 *                                         tabLocations[currentTab]+1,
 *                                         lineContainsText);
 *
 *             // layout can be null if lineContainsText is true
 *             if (layout != null) {
 *                 layouts.addElement(layout);
 *                 penPositions.addElement(new Float(horizontalPos));
 *                 horizontalPos += layout.getAdvance();
 *                 maxAscent = Math.max(maxAscent, layout.getAscent());
 *                 maxDescent = Math.max(maxDescent,
 *                     layout.getDescent() + layout.getLeading());
 *             } else {
 *                 lineComplete = true;
 *             }
 *
 *             lineContainsText = true;
 *
 *             if (measurer.getPosition() == tabLocations[currentTab]+1) {
 *                 currentTab++;
 *             }
 *
 *             if (measurer.getPosition() == styledText.getEndIndex())
 *                 lineComplete = true;
 *             else if (horizontalPos >= tabStops[tabStops.length-1])
 *                 lineComplete = true;
 *
 *             if (!lineComplete) {
 *                 // move to next tab stop
 *                 int j;
 *                 for (j=0; horizontalPos >= tabStops[j]; j++) {}
 *                 horizontalPos = tabStops[j];
 *             }
 *         }
 *
 *         verticalPos += maxAscent;
 *
 *         Enumeration layoutEnum = layouts.elements();
 *         Enumeration positionEnum = penPositions.elements();
 *
 *         // now iterate through layouts and draw them
 *         while (layoutEnum.hasMoreElements()) {
 *             TextLayout nextLayout = (TextLayout) layoutEnum.nextElement();
 *             Float nextPosition = (Float) positionEnum.nextElement();
 *             nextLayout.draw(graphics, nextPosition.floatValue(), verticalPos);
 *         }
 *
 *         verticalPos += maxDescent;
 *     }
 * }
 * }</pre>
 * </blockquote>
 * @see TextLayout
 */

public final class LineBreakMeasurer {

    private BreakIterator breakIter;
    private int start;
    private int pos;
    private int limit;
    private TextMeasurer measurer;
    private CharArrayIterator charIter;

    /**
     * Constructs a {@code LineBreakMeasurer} for the specified text.
     *
     * @param text the text for which this {@code LineBreakMeasurer}
     *       produces {@code TextLayout} objects; the text must contain
     *       at least one character; if the text available through
     *       {@code iter} changes, further calls to this
     *       {@code LineBreakMeasurer} instance are undefined (except,
     *       in some cases, when {@code insertChar} or
     *       {@code deleteChar} are invoked afterward - see below)
     * @param frc contains information about a graphics device which is
     *       needed to measure the text correctly;
     *       text measurements can vary slightly depending on the
     *       device resolution, and attributes such as antialiasing; this
     *       parameter does not specify a translation between the
     *       {@code LineBreakMeasurer} and user space
     * @see LineBreakMeasurer#insertChar
     * @see LineBreakMeasurer#deleteChar
     */
    public LineBreakMeasurer(AttributedCharacterIterator text, FontRenderContext frc) {
        this(text, BreakIterator.getLineInstance(), frc);
    }

    /**
     * Constructs a {@code LineBreakMeasurer} for the specified text.
     *
     * @param text the text for which this {@code LineBreakMeasurer}
     *     produces {@code TextLayout} objects; the text must contain
     *     at least one character; if the text available through
     *     {@code iter} changes, further calls to this
     *     {@code LineBreakMeasurer} instance are undefined (except,
     *     in some cases, when {@code insertChar} or
     *     {@code deleteChar} are invoked afterward - see below)
     * @param breakIter the {@link BreakIterator} which defines line
     *     breaks
     * @param frc contains information about a graphics device which is
     *       needed to measure the text correctly;
     *       text measurements can vary slightly depending on the
     *       device resolution, and attributes such as antialiasing; this
     *       parameter does not specify a translation between the
     *       {@code LineBreakMeasurer} and user space
     * @throws IllegalArgumentException if the text has less than one character
     * @see LineBreakMeasurer#insertChar
     * @see LineBreakMeasurer#deleteChar
     */
    public LineBreakMeasurer(AttributedCharacterIterator text,
                             BreakIterator breakIter,
                             FontRenderContext frc) {
        if (text.getEndIndex() - text.getBeginIndex() < 1) {
            throw new IllegalArgumentException("Text must contain at least one character.");
        }

        this.breakIter = breakIter;
        this.measurer = new TextMeasurer(text, frc);
        this.limit = text.getEndIndex();
        this.pos = this.start = text.getBeginIndex();

        charIter = new CharArrayIterator(measurer.getChars(), this.start);
        this.breakIter.setText(charIter);
    }

    /**
     * Returns the position at the end of the next layout.  Does NOT
     * update the current position of this {@code LineBreakMeasurer}.
     *
     * @param wrappingWidth the maximum visible advance permitted for
     *    the text in the next layout
     * @return an offset in the text representing the limit of the
     *    next {@code TextLayout}.
     */
    public int nextOffset(float wrappingWidth) {
        return nextOffset(wrappingWidth, limit, false);
    }

    /**
     * Returns the position at the end of the next layout.  Does NOT
     * update the current position of this {@code LineBreakMeasurer}.
     *
     * @param wrappingWidth the maximum visible advance permitted for
     *    the text in the next layout
     * @param offsetLimit the first character that can not be included
     *    in the next layout, even if the text after the limit would fit
     *    within the wrapping width; {@code offsetLimit} must be
     *    greater than the current position
     * @param requireNextWord if {@code true}, the current position
     *    that is returned if the entire next word does not fit within
     *    {@code wrappingWidth}; if {@code false}, the offset
     *    returned is at least one greater than the current position
     * @return an offset in the text representing the limit of the
     *    next {@code TextLayout}
     */
    public int nextOffset(float wrappingWidth, int offsetLimit,
                          boolean requireNextWord) {

        int nextOffset = pos;

        if (pos < limit) {
            if (offsetLimit <= pos) {
                    throw new IllegalArgumentException("offsetLimit must be after current position");
            }

            int charAtMaxAdvance =
                            measurer.getLineBreakIndex(pos, wrappingWidth);

            if (charAtMaxAdvance == limit) {
                nextOffset = limit;
            }
            else if (Character.isWhitespace(measurer.getChars()[charAtMaxAdvance-start])) {
                nextOffset = breakIter.following(charAtMaxAdvance);
            }
            else {
            // Break is in a word;  back up to previous break.

                // NOTE:  I think that breakIter.preceding(limit) should be
                // equivalent to breakIter.last(), breakIter.previous() but
                // the authors of BreakIterator thought otherwise...
                // If they were equivalent then the first branch would be
                // unnecessary.
                int testPos = charAtMaxAdvance + 1;
                if (testPos == limit) {
                    breakIter.last();
                    nextOffset = breakIter.previous();
                }
                else {
                    nextOffset = breakIter.preceding(testPos);
                }

                if (nextOffset <= pos) {
                    // first word doesn't fit on line
                    if (requireNextWord) {
                        nextOffset = pos;
                    }
                    else {
                        nextOffset = Math.max(pos+1, charAtMaxAdvance);
                    }
                }
            }
        }

        if (nextOffset > offsetLimit) {
            nextOffset = offsetLimit;
        }

        return nextOffset;
    }

    /**
     * Returns the next layout, and updates the current position.
     *
     * @param wrappingWidth the maximum visible advance permitted for
     *     the text in the next layout
     * @return a {@code TextLayout}, beginning at the current
     *     position, which represents the next line fitting within
     *     {@code wrappingWidth}
     */
    public TextLayout nextLayout(float wrappingWidth) {
        return nextLayout(wrappingWidth, limit, false);
    }

    /**
     * Returns the next layout, and updates the current position.
     *
     * @param wrappingWidth the maximum visible advance permitted
     *    for the text in the next layout
     * @param offsetLimit the first character that can not be
     *    included in the next layout, even if the text after the limit
     *    would fit within the wrapping width; {@code offsetLimit}
     *    must be greater than the current position
     * @param requireNextWord if {@code true}, and if the entire word
     *    at the current position does not fit within the wrapping width,
     *    {@code null} is returned. If {@code false}, a valid
     *    layout is returned that includes at least the character at the
     *    current position
     * @return a {@code TextLayout}, beginning at the current
     *    position, that represents the next line fitting within
     *    {@code wrappingWidth}.  If the current position is at the end
     *    of the text used by this {@code LineBreakMeasurer},
     *    {@code null} is returned
     */
    public TextLayout nextLayout(float wrappingWidth, int offsetLimit,
                                 boolean requireNextWord) {

        if (pos < limit) {
            int layoutLimit = nextOffset(wrappingWidth, offsetLimit, requireNextWord);
            if (layoutLimit == pos) {
                return null;
            }

            TextLayout result = measurer.getLayout(pos, layoutLimit);
            pos = layoutLimit;

            return result;
        } else {
            return null;
        }
    }

    /**
     * Returns the current position of this {@code LineBreakMeasurer}.
     *
     * @return the current position of this {@code LineBreakMeasurer}
     * @see #setPosition
     */
    public int getPosition() {
        return pos;
    }

    /**
     * Sets the current position of this {@code LineBreakMeasurer}.
     *
     * @param newPosition the current position of this
     *    {@code LineBreakMeasurer}; the position should be within the
     *    text used to construct this {@code LineBreakMeasurer} (or in
     *    the text most recently passed to {@code insertChar}
     *    or {@code deleteChar}
     * @see #getPosition
     */
    public void setPosition(int newPosition) {
        if (newPosition < start || newPosition > limit) {
            throw new IllegalArgumentException("position is out of range");
        }
        pos = newPosition;
    }

    /**
     * Updates this {@code LineBreakMeasurer} after a single
     * character is inserted into the text, and sets the current
     * position to the beginning of the paragraph.
     *
     * @param newParagraph the text after the insertion
     * @param insertPos the position in the text at which the character
     *    is inserted
     * @throws IndexOutOfBoundsException if {@code insertPos} is less
     *         than the start of {@code newParagraph} or greater than
     *         or equal to the end of {@code newParagraph}
     * @throws NullPointerException if {@code newParagraph} is
     *         {@code null}
     * @see #deleteChar
     */
    public void insertChar(AttributedCharacterIterator newParagraph,
                           int insertPos) {

        measurer.insertChar(newParagraph, insertPos);

        limit = newParagraph.getEndIndex();
        pos = start = newParagraph.getBeginIndex();

        charIter.reset(measurer.getChars(), newParagraph.getBeginIndex());
        breakIter.setText(charIter);
    }

    /**
     * Updates this {@code LineBreakMeasurer} after a single
     * character is deleted from the text, and sets the current
     * position to the beginning of the paragraph.
     * @param newParagraph the text after the deletion
     * @param deletePos the position in the text at which the character
     *    is deleted
     * @throws IndexOutOfBoundsException if {@code deletePos} is
     *         less than the start of {@code newParagraph} or greater
     *         than the end of {@code newParagraph}
     * @throws NullPointerException if {@code newParagraph} is
     *         {@code null}
     * @see #insertChar
     */
    public void deleteChar(AttributedCharacterIterator newParagraph,
                           int deletePos) {

        measurer.deleteChar(newParagraph, deletePos);

        limit = newParagraph.getEndIndex();
        pos = start = newParagraph.getBeginIndex();

        charIter.reset(measurer.getChars(), start);
        breakIter.setText(charIter);
    }
}
