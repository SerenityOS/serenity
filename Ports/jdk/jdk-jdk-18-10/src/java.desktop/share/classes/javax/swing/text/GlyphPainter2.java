/*
 * Copyright (c) 1999, 2017, Oracle and/or its affiliates. All rights reserved.
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
package javax.swing.text;

import java.text.*;
import java.awt.*;
import java.awt.font.*;
import java.awt.geom.Rectangle2D;

/**
 * A class to perform rendering of the glyphs.
 * This can be implemented to be stateless, or
 * to hold some information as a cache to
 * facilitate faster rendering and model/view
 * translation.  At a minimum, the GlyphPainter
 * allows a View implementation to perform its
 * duties independent of a particular version
 * of JVM and selection of capabilities (i.e.
 * shaping for i18n, etc).
 * <p>
 * This implementation is intended for operation
 * under the JDK.  It uses the
 * java.awt.font.TextLayout class to do i18n capable
 * rendering.
 *
 * @author  Timothy Prinzing
 * @see GlyphView
 */
class GlyphPainter2 extends GlyphView.GlyphPainter {

    public GlyphPainter2(TextLayout layout) {
        this.layout = layout;
    }

    /**
     * Create a painter to use for the given GlyphView.
     */
    public GlyphView.GlyphPainter getPainter(GlyphView v, int p0, int p1) {
        return null;
    }

    /**
     * Determine the span the glyphs given a start location
     * (for tab expansion).  This implementation assumes it
     * has no tabs (i.e. TextLayout doesn't deal with tab
     * expansion).
     */
    public float getSpan(GlyphView v, int p0, int p1,
                         TabExpander e, float x) {

        if ((p0 == v.getStartOffset()) && (p1 == v.getEndOffset())) {
            return layout.getAdvance();
        }
        int p = v.getStartOffset();
        int index0 = p0 - p;
        int index1 = p1 - p;

        TextHitInfo hit0 = TextHitInfo.afterOffset(index0);
        TextHitInfo hit1 = TextHitInfo.beforeOffset(index1);
        float[] locs = layout.getCaretInfo(hit0);
        float x0 = locs[0];
        locs = layout.getCaretInfo(hit1);
        float x1 = locs[0];
        return (x1 > x0) ? x1 - x0 : x0 - x1;
    }

    public float getHeight(GlyphView v) {
        return layout.getAscent() + layout.getDescent() + layout.getLeading();
    }

    /**
     * Fetch the ascent above the baseline for the glyphs
     * corresponding to the given range in the model.
     */
    public float getAscent(GlyphView v) {
        return layout.getAscent();
    }

    /**
     * Fetch the descent below the baseline for the glyphs
     * corresponding to the given range in the model.
     */
    public float getDescent(GlyphView v) {
        return layout.getDescent();
    }

    /**
     * Paint the glyphs for the given view.  This is implemented
     * to only render if the Graphics is of type Graphics2D which
     * is required by TextLayout (and this should be the case if
     * running on the JDK).
     */
    public void paint(GlyphView v, Graphics g, Shape a, int p0, int p1) {
        if (g instanceof Graphics2D) {
            Rectangle2D alloc = a.getBounds2D();
            Graphics2D g2d = (Graphics2D)g;
            float y = (float) alloc.getY() + layout.getAscent() + layout.getLeading();
            float x = (float) alloc.getX();
            if( p0 > v.getStartOffset() || p1 < v.getEndOffset() ) {
                try {
                    //TextLayout can't render only part of it's range, so if a
                    //partial range is required, add a clip region.
                    Shape s = v.modelToView(p0, Position.Bias.Forward,
                                            p1, Position.Bias.Backward, a);
                    Shape savedClip = g.getClip();
                    g2d.clip(s);
                    layout.draw(g2d, x, y);
                    g.setClip(savedClip);
                } catch (BadLocationException e) {}
            } else {
                layout.draw(g2d, x, y);
            }
        }
    }

    public Shape modelToView(GlyphView v, int pos, Position.Bias bias,
                             Shape a) throws BadLocationException {
        int offs = pos - v.getStartOffset();
        Rectangle2D alloc = a.getBounds2D();
        TextHitInfo hit = (bias == Position.Bias.Forward) ?
            TextHitInfo.afterOffset(offs) : TextHitInfo.beforeOffset(offs);
        float[] locs = layout.getCaretInfo(hit);

        // vertical at the baseline, should use slope and check if glyphs
        // are being rendered vertically.
        Rectangle2D rect = new Rectangle2D.Float();
        rect.setRect(alloc.getX() + locs[0], alloc.getY(), 1, alloc.getHeight());
        return rect;
    }

    /**
     * Provides a mapping from the view coordinate space to the logical
     * coordinate space of the model.
     *
     * @param v the view containing the view coordinates
     * @param x the X coordinate
     * @param y the Y coordinate
     * @param a the allocated region to render into
     * @param biasReturn either <code>Position.Bias.Forward</code>
     *  or <code>Position.Bias.Backward</code> is returned as the
     *  zero-th element of this array
     * @return the location within the model that best represents the
     *  given point of view
     * @see View#viewToModel
     */
    public int viewToModel(GlyphView v, float x, float y, Shape a,
                           Position.Bias[] biasReturn) {

        Rectangle2D alloc = (a instanceof Rectangle2D) ? (Rectangle2D)a : a.getBounds2D();
        //Move the y co-ord of the hit onto the baseline.  This is because TextLayout supports
        //italic carets and we do not.
        TextHitInfo hit = layout.hitTestChar(x - (float)alloc.getX(), 0);
        int pos = hit.getInsertionIndex();

        if (pos == v.getEndOffset()) {
            pos--;
        }

        biasReturn[0] = hit.isLeadingEdge() ? Position.Bias.Forward : Position.Bias.Backward;
        return pos + v.getStartOffset();
    }

    /**
     * Determines the model location that represents the
     * maximum advance that fits within the given span.
     * This could be used to break the given view.  The result
     * should be a location just shy of the given advance.  This
     * differs from viewToModel which returns the closest
     * position which might be proud of the maximum advance.
     *
     * @param v the view to find the model location to break at.
     * @param p0 the location in the model where the
     *  fragment should start it's representation >= 0.
     * @param x the graphic location along the axis that the
     *  broken view would occupy >= 0.  This may be useful for
     *  things like tab calculations.
     * @param len specifies the distance into the view
     *  where a potential break is desired >= 0.
     * @return the maximum model location possible for a break.
     * @see View#breakView
     */
    public int getBoundedPosition(GlyphView v, int p0, float x, float len) {
        if( len < 0 )
            throw new IllegalArgumentException("Length must be >= 0.");
        // note: this only works because swing uses TextLayouts that are
        // only pure rtl or pure ltr
        TextHitInfo hit;
        if (layout.isLeftToRight()) {
            hit = layout.hitTestChar(len, 0);
        } else {
            hit = layout.hitTestChar(layout.getAdvance() - len, 0);
        }
        return v.getStartOffset() + hit.getCharIndex();
    }

    /**
         * Provides a way to determine the next visually represented model
         * location that one might place a caret.  Some views may not be
         * visible, they might not be in the same order found in the model, or
         * they just might not allow access to some of the locations in the
         * model.
         *
         * @param v the view to use
         * @param pos the position to convert >= 0
         * @param a the allocated region to render into
         * @param direction the direction from the current position that can
         *  be thought of as the arrow keys typically found on a keyboard.
         *  This may be SwingConstants.WEST, SwingConstants.EAST,
         *  SwingConstants.NORTH, or SwingConstants.SOUTH.
         * @return the location within the model that best represents the next
         *  location visual position.
         * @exception BadLocationException
         * @exception IllegalArgumentException for an invalid direction
         */
        public int getNextVisualPositionFrom(GlyphView v, int pos,
                                             Position.Bias b, Shape a,
                                             int direction,
                                             Position.Bias[] biasRet)
            throws BadLocationException {

            Document doc = v.getDocument();
            int startOffset = v.getStartOffset();
            int endOffset = v.getEndOffset();
            Segment text;
            boolean viewIsLeftToRight;
            TextHitInfo currentHit, nextHit;

            switch (direction) {
            case View.NORTH:
                break;
            case View.SOUTH:
                break;
            case View.EAST:
                viewIsLeftToRight = AbstractDocument.isLeftToRight(doc, startOffset, endOffset);

                if(startOffset == doc.getLength()) {
                    if(pos == -1) {
                        biasRet[0] = Position.Bias.Forward;
                        return startOffset;
                    }
                    // End case for bidi text where newline is at beginning
                    // of line.
                    return -1;
                }
                if(pos == -1) {
                    // Entering view from the left.
                    if( viewIsLeftToRight ) {
                        biasRet[0] = Position.Bias.Forward;
                        return startOffset;
                    } else {
                        text = v.getText(endOffset - 1, endOffset);
                        char c = text.array[text.offset];
                        SegmentCache.releaseSharedSegment(text);
                        if(c == '\n') {
                            biasRet[0] = Position.Bias.Forward;
                            return endOffset-1;
                        }
                        biasRet[0] = Position.Bias.Backward;
                        return endOffset;
                    }
                }
                if( b==Position.Bias.Forward )
                    currentHit = TextHitInfo.afterOffset(pos-startOffset);
                else
                    currentHit = TextHitInfo.beforeOffset(pos-startOffset);
                nextHit = layout.getNextRightHit(currentHit);
                if( nextHit == null ) {
                    return -1;
                }
                if( viewIsLeftToRight != layout.isLeftToRight() ) {
                    // If the layout's base direction is different from
                    // this view's run direction, we need to use the weak
                    // carrat.
                    nextHit = layout.getVisualOtherHit(nextHit);
                }
                pos = nextHit.getInsertionIndex() + startOffset;

                if(pos == endOffset) {
                    // A move to the right from an internal position will
                    // only take us to the endOffset in a left to right run.
                    text = v.getText(endOffset - 1, endOffset);
                    char c = text.array[text.offset];
                    SegmentCache.releaseSharedSegment(text);
                    if(c == '\n') {
                        return -1;
                    }
                    biasRet[0] = Position.Bias.Backward;
                }
                else {
                    biasRet[0] = Position.Bias.Forward;
                }
                return pos;
            case View.WEST:
                viewIsLeftToRight = AbstractDocument.isLeftToRight(doc, startOffset, endOffset);

                if(startOffset == doc.getLength()) {
                    if(pos == -1) {
                        biasRet[0] = Position.Bias.Forward;
                        return startOffset;
                    }
                    // End case for bidi text where newline is at beginning
                    // of line.
                    return -1;
                }
                if(pos == -1) {
                    // Entering view from the right
                    if( viewIsLeftToRight ) {
                        text = v.getText(endOffset - 1, endOffset);
                        char c = text.array[text.offset];
                        SegmentCache.releaseSharedSegment(text);
                        if ((c == '\n') || Character.isSpaceChar(c)) {
                            biasRet[0] = Position.Bias.Forward;
                            return endOffset - 1;
                        }
                        biasRet[0] = Position.Bias.Backward;
                        return endOffset;
                    } else {
                        biasRet[0] = Position.Bias.Forward;
                        return startOffset;
                   }
                }
                if( b==Position.Bias.Forward )
                    currentHit = TextHitInfo.afterOffset(pos-startOffset);
                else
                    currentHit = TextHitInfo.beforeOffset(pos-startOffset);
                nextHit = layout.getNextLeftHit(currentHit);
                if( nextHit == null ) {
                    return -1;
                }
                if( viewIsLeftToRight != layout.isLeftToRight() ) {
                    // If the layout's base direction is different from
                    // this view's run direction, we need to use the weak
                    // carrat.
                    nextHit = layout.getVisualOtherHit(nextHit);
                }
                pos = nextHit.getInsertionIndex() + startOffset;

                if(pos == endOffset) {
                    // A move to the left from an internal position will
                    // only take us to the endOffset in a right to left run.
                    text = v.getText(endOffset - 1, endOffset);
                    char c = text.array[text.offset];
                    SegmentCache.releaseSharedSegment(text);
                    if(c == '\n') {
                        return -1;
                    }
                    biasRet[0] = Position.Bias.Backward;
                }
                else {
                    biasRet[0] = Position.Bias.Forward;
                }
                return pos;
            default:
                throw new IllegalArgumentException("Bad direction: " + direction);
            }
            return pos;

        }
    // --- variables ---------------------------------------------

    TextLayout layout;

}
