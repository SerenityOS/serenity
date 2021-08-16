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

import java.util.*;
import java.awt.*;
import java.text.AttributedCharacterIterator;
import java.text.BreakIterator;
import java.awt.font.*;
import java.awt.geom.AffineTransform;
import javax.swing.JComponent;
import javax.swing.event.DocumentEvent;
import sun.font.BidiUtils;

/**
 * A flow strategy that uses java.awt.font.LineBreakMeasureer to
 * produce java.awt.font.TextLayout for i18n capable rendering.
 * If the child view being placed into the flow is of type
 * GlyphView and can be rendered by TextLayout, a GlyphPainter
 * that uses TextLayout is plugged into the GlyphView.
 *
 * @author  Timothy Prinzing
 */
class TextLayoutStrategy extends FlowView.FlowStrategy {

    /**
     * Constructs a layout strategy for paragraphs based
     * upon java.awt.font.LineBreakMeasurer.
     */
    public TextLayoutStrategy() {
        text = new AttributedSegment();
    }

    // --- FlowStrategy methods --------------------------------------------

    /**
     * Gives notification that something was inserted into the document
     * in a location that the given flow view is responsible for.  The
     * strategy should update the appropriate changed region (which
     * depends upon the strategy used for repair).
     *
     * @param e the change information from the associated document
     * @param alloc the current allocation of the view inside of the insets.
     *   This value will be null if the view has not yet been displayed.
     * @see View#insertUpdate
     */
    public void insertUpdate(FlowView fv, DocumentEvent e, Rectangle alloc) {
        sync(fv);
        super.insertUpdate(fv, e, alloc);
    }

    /**
     * Gives notification that something was removed from the document
     * in a location that the given flow view is responsible for.
     *
     * @param e the change information from the associated document
     * @param alloc the current allocation of the view inside of the insets.
     * @see View#removeUpdate
     */
    public void removeUpdate(FlowView fv, DocumentEvent e, Rectangle alloc) {
        sync(fv);
        super.removeUpdate(fv, e, alloc);
    }

    /**
     * Gives notification from the document that attributes were changed
     * in a location that this view is responsible for.
     *
     * @param e the change information from the associated document
     * @param alloc the current allocation of the view inside of the insets.
     * @see View#changedUpdate
     */
    public void changedUpdate(FlowView fv, DocumentEvent e, Rectangle alloc) {
        sync(fv);
        super.changedUpdate(fv, e, alloc);
    }

    /**
     * Does a a full layout on the given View.  This causes all of
     * the rows (child views) to be rebuilt to match the given
     * constraints for each row.  This is called by a FlowView.layout
     * to update the child views in the flow.
     *
     * @param fv the view to reflow
     */
    public void layout(FlowView fv) {
        super.layout(fv);
    }

    /**
     * Creates a row of views that will fit within the
     * layout span of the row.  This is implemented to execute the
     * superclass functionality (which fills the row with child
     * views or view fragments) and follow that with bidi reordering
     * of the unidirectional view fragments.
     *
     * @param rowIndex the row to fill in with views.  This is assumed
     *   to be empty on entry.
     * @param p0  The current position in the children of
     *   this views element from which to start.
     * @return the position to start the next row
     */
    protected int layoutRow(FlowView fv, int rowIndex, int p0) {
        int p1 = super.layoutRow(fv, rowIndex, p0);
        View row = fv.getView(rowIndex);
        Document doc = fv.getDocument();
        Object i18nFlag = doc.getProperty(AbstractDocument.I18NProperty);
        if ((i18nFlag != null) && i18nFlag.equals(Boolean.TRUE)) {
            int n = row.getViewCount();
            if (n > 1) {
                AbstractDocument d = (AbstractDocument)fv.getDocument();
                Element bidiRoot = d.getBidiRootElement();
                byte[] levels = new byte[n];
                View[] reorder = new View[n];

                for( int i=0; i<n; i++ ) {
                    View v = row.getView(i);
                    int bidiIndex =bidiRoot.getElementIndex(v.getStartOffset());
                    Element bidiElem = bidiRoot.getElement( bidiIndex );
                    levels[i] = (byte)StyleConstants.getBidiLevel(bidiElem.getAttributes());
                    reorder[i] = v;
                }

                BidiUtils.reorderVisually( levels, reorder );
                row.replace(0, n, reorder);
            }
        }
        return p1;
    }

    /**
     * Adjusts the given row if possible to fit within the
     * layout span.  Since all adjustments were already
     * calculated by the LineBreakMeasurer, this is implemented
     * to do nothing.
     *
     * @param rowIndex the row to adjust to the current layout
     *  span.
     * @param desiredSpan the current layout span >= 0
     * @param x the location r starts at.
     */
    protected void adjustRow(FlowView fv, int rowIndex, int desiredSpan, int x) {
    }

    /**
     * Creates a unidirectional view that can be used to represent the
     * current chunk.  This can be either an entire view from the
     * logical view, or a fragment of the view.
     *
     * @param fv the view holding the flow
     * @param startOffset the start location for the view being created
     * @param spanLeft the about of span left to fill in the row
     * @param rowIndex the row the view will be placed into
     */
    protected View createView(FlowView fv, int startOffset, int spanLeft, int rowIndex) {
        // Get the child view that contains the given starting position
        View lv = getLogicalView(fv);
        View row = fv.getView(rowIndex);
        boolean requireNextWord = (viewBuffer.size() == 0) ? false : true;
        int childIndex = lv.getViewIndex(startOffset, Position.Bias.Forward);
        View v = lv.getView(childIndex);

        int endOffset = getLimitingOffset(v, startOffset, spanLeft, requireNextWord);
        if (endOffset == startOffset) {
            return null;
        }

        View frag;
        if ((startOffset==v.getStartOffset()) && (endOffset == v.getEndOffset())) {
            // return the entire view
            frag = v;
        } else {
            // return a unidirectional fragment.
            frag = v.createFragment(startOffset, endOffset);
        }

        if ((frag instanceof GlyphView) && (measurer != null)) {
            // install a TextLayout based renderer if the view is responsible
            // for glyphs.  If the view represents a tab, the default
            // glyph painter is used (may want to handle tabs differently).
            boolean isTab = false;
            int p0 = frag.getStartOffset();
            int p1 = frag.getEndOffset();
            if ((p1 - p0) == 1) {
                // check for tab
                Segment s = ((GlyphView)frag).getText(p0, p1);
                char ch = s.first();
                if (ch == '\t') {
                    isTab = true;
                }
            }
            TextLayout tl = (isTab) ? null :
                measurer.nextLayout(spanLeft, text.toIteratorIndex(endOffset),
                                    requireNextWord);
            if (tl != null) {
                ((GlyphView)frag).setGlyphPainter(new GlyphPainter2(tl));
            }
        }
        return frag;
    }

    /**
     * Calculate the limiting offset for the next view fragment.
     * At most this would be the entire view (i.e. the limiting
     * offset would be the end offset in that case).  If the range
     * contains a tab or a direction change, that will limit the
     * offset to something less.  This value is then fed to the
     * LineBreakMeasurer as a limit to consider in addition to the
     * remaining span.
     *
     * @param v the logical view representing the starting offset.
     * @param startOffset the model location to start at.
     */
    int getLimitingOffset(View v, int startOffset, int spanLeft, boolean requireNextWord) {
        int endOffset = v.getEndOffset();

        // check for direction change
        Document doc = v.getDocument();
        if (doc instanceof AbstractDocument) {
            AbstractDocument d = (AbstractDocument) doc;
            Element bidiRoot = d.getBidiRootElement();
            if( bidiRoot.getElementCount() > 1 ) {
                int bidiIndex = bidiRoot.getElementIndex( startOffset );
                Element bidiElem = bidiRoot.getElement( bidiIndex );
                endOffset = Math.min( bidiElem.getEndOffset(), endOffset );
            }
        }

        // check for tab
        if (v instanceof GlyphView) {
            Segment s = ((GlyphView)v).getText(startOffset, endOffset);
            char ch = s.first();
            if (ch == '\t') {
                // if the first character is a tab, create a dedicated
                // view for just the tab
                endOffset = startOffset + 1;
            } else {
                for (ch = s.next(); ch != Segment.DONE; ch = s.next()) {
                    if (ch == '\t') {
                        // found a tab, don't include it in the text
                        endOffset = startOffset + s.getIndex() - s.getBeginIndex();
                        break;
                    }
                }
            }
        }

        // determine limit from LineBreakMeasurer
        int limitIndex = text.toIteratorIndex(endOffset);
        if (measurer != null) {
            int index = text.toIteratorIndex(startOffset);
            if (measurer.getPosition() != index) {
                measurer.setPosition(index);
            }
            limitIndex = measurer.nextOffset(spanLeft, limitIndex, requireNextWord);
        }
        int pos = text.toModelPosition(limitIndex);
        return pos;
    }

    /**
     * Synchronize the strategy with its FlowView.  Allows the strategy
     * to update its state to account for changes in that portion of the
     * model represented by the FlowView.  Also allows the strategy
     * to update the FlowView in response to these changes.
     */
    void sync(FlowView fv) {
        View lv = getLogicalView(fv);
        text.setView(lv);

        Container container = fv.getContainer();
        FontRenderContext frc = sun.swing.SwingUtilities2.
                                    getFontRenderContext(container);
        BreakIterator iter;
        Container c = fv.getContainer();
        if (c != null) {
            iter = BreakIterator.getLineInstance(c.getLocale());
        } else {
            iter = BreakIterator.getLineInstance();
        }

        Object shaper = null;
        if (c instanceof JComponent) {
            shaper = ((JComponent) c).getClientProperty(
                                            TextAttribute.NUMERIC_SHAPING);
        }
        text.setShaper(shaper);

        measurer = new LineBreakMeasurer(text, iter, frc);

        // If the children of the FlowView's logical view are GlyphViews, they
        // need to have their painters updated.
        int n = lv.getViewCount();
        for( int i=0; i<n; i++ ) {
            View child = lv.getView(i);
            if( child instanceof GlyphView ) {
                int p0 = child.getStartOffset();
                int p1 = child.getEndOffset();
                measurer.setPosition(text.toIteratorIndex(p0));
                TextLayout layout
                    = measurer.nextLayout( Float.MAX_VALUE,
                                           text.toIteratorIndex(p1), false );
                ((GlyphView)child).setGlyphPainter(new GlyphPainter2(layout));
            }
        }

        // Reset measurer.
        measurer.setPosition(text.getBeginIndex());

    }

    // --- variables -------------------------------------------------------

    private LineBreakMeasurer measurer;
    private AttributedSegment text;

    /**
     * Implementation of AttributedCharacterIterator that supports
     * the GlyphView attributes for rendering the glyphs through a
     * TextLayout.
     */
    static class AttributedSegment extends Segment implements AttributedCharacterIterator {

        AttributedSegment() {
        }

        View getView() {
            return v;
        }

        void setView(View v) {
            this.v = v;
            Document doc = v.getDocument();
            int p0 = v.getStartOffset();
            int p1 = v.getEndOffset();
            try {
                doc.getText(p0, p1 - p0, this);
            } catch (BadLocationException bl) {
                throw new IllegalArgumentException("Invalid view");
            }
            first();
        }

        /**
         * Get a boundary position for the font.
         * This is implemented to assume that two fonts are
         * equal if their references are equal (i.e. that the
         * font came from a cache).
         *
         * @return the location in model coordinates.  This is
         *  not the same as the Segment coordinates.
         */
        int getFontBoundary(int childIndex, int dir) {
            View child = v.getView(childIndex);
            Font f = getFont(childIndex);
            for (childIndex += dir; (childIndex >= 0) && (childIndex < v.getViewCount());
                 childIndex += dir) {
                Font next = getFont(childIndex);
                if (next != f) {
                    // this run is different
                    break;
                }
                child = v.getView(childIndex);
            }
            return (dir < 0) ? child.getStartOffset() : child.getEndOffset();
        }

        /**
         * Get the font at the given child index.
         */
        Font getFont(int childIndex) {
            View child = v.getView(childIndex);
            if (child instanceof GlyphView) {
                return ((GlyphView)child).getFont();
            }
            return null;
        }

        int toModelPosition(int index) {
            return v.getStartOffset() + (index - getBeginIndex());
        }

        int toIteratorIndex(int pos) {
            return pos - v.getStartOffset() + getBeginIndex();
        }

        private void setShaper(Object shaper) {
            this.shaper = shaper;
        }

        // --- AttributedCharacterIterator methods -------------------------

        /**
         * Returns the index of the first character of the run
         * with respect to all attributes containing the current character.
         */
        public int getRunStart() {
            int pos = toModelPosition(getIndex());
            int i = v.getViewIndex(pos, Position.Bias.Forward);
            View child = v.getView(i);
            return toIteratorIndex(child.getStartOffset());
        }

        /**
         * Returns the index of the first character of the run
         * with respect to the given attribute containing the current character.
         */
        public int getRunStart(AttributedCharacterIterator.Attribute attribute) {
            if (attribute instanceof TextAttribute) {
                int pos = toModelPosition(getIndex());
                int i = v.getViewIndex(pos, Position.Bias.Forward);
                if (attribute == TextAttribute.FONT) {
                    return toIteratorIndex(getFontBoundary(i, -1));
                }
            }
            return getBeginIndex();
        }

        /**
         * Returns the index of the first character of the run
         * with respect to the given attributes containing the current character.
         */
        public int getRunStart(Set<? extends Attribute> attributes) {
            int index = getBeginIndex();
            Object[] a = attributes.toArray();
            for (int i = 0; i < a.length; i++) {
                TextAttribute attr = (TextAttribute) a[i];
                index = Math.max(getRunStart(attr), index);
            }
            return Math.min(getIndex(), index);
        }

        /**
         * Returns the index of the first character following the run
         * with respect to all attributes containing the current character.
         */
        public int getRunLimit() {
            int pos = toModelPosition(getIndex());
            int i = v.getViewIndex(pos, Position.Bias.Forward);
            View child = v.getView(i);
            return toIteratorIndex(child.getEndOffset());
        }

        /**
         * Returns the index of the first character following the run
         * with respect to the given attribute containing the current character.
         */
        public int getRunLimit(AttributedCharacterIterator.Attribute attribute) {
            if (attribute instanceof TextAttribute) {
                int pos = toModelPosition(getIndex());
                int i = v.getViewIndex(pos, Position.Bias.Forward);
                if (attribute == TextAttribute.FONT) {
                    return toIteratorIndex(getFontBoundary(i, 1));
                }
            }
            return getEndIndex();
        }

        /**
         * Returns the index of the first character following the run
         * with respect to the given attributes containing the current character.
         */
        public int getRunLimit(Set<? extends Attribute> attributes) {
            int index = getEndIndex();
            Object[] a = attributes.toArray();
            for (int i = 0; i < a.length; i++) {
                TextAttribute attr = (TextAttribute) a[i];
                index = Math.min(getRunLimit(attr), index);
            }
            return Math.max(getIndex(), index);
        }

        /**
         * Returns a map with the attributes defined on the current
         * character.
         */
        public Map<Attribute, Object> getAttributes() {
            Object[] ka = keys.toArray();
            Hashtable<Attribute, Object> h = new Hashtable<Attribute, Object>();
            for (int i = 0; i < ka.length; i++) {
                TextAttribute a = (TextAttribute) ka[i];
                Object value = getAttribute(a);
                if (value != null) {
                    h.put(a, value);
                }
            }
            return h;
        }

        /**
         * Returns the value of the named attribute for the current character.
         * Returns null if the attribute is not defined.
         * @param attribute the key of the attribute whose value is requested.
         */
        public Object getAttribute(AttributedCharacterIterator.Attribute attribute) {
            int pos = toModelPosition(getIndex());
            int childIndex = v.getViewIndex(pos, Position.Bias.Forward);
            if (attribute == TextAttribute.FONT) {
                return getFont(childIndex);
            } else if( attribute == TextAttribute.RUN_DIRECTION ) {
                return
                    v.getDocument().getProperty(TextAttribute.RUN_DIRECTION);
            } else if (attribute == TextAttribute.NUMERIC_SHAPING) {
                return shaper;
            }
            return null;
        }

        /**
         * Returns the keys of all attributes defined on the
         * iterator's text range. The set is empty if no
         * attributes are defined.
         */
        public Set<Attribute> getAllAttributeKeys() {
            return keys;
        }

        View v;

        static Set<Attribute> keys;

        static {
            keys = new HashSet<Attribute>();
            keys.add(TextAttribute.FONT);
            keys.add(TextAttribute.RUN_DIRECTION);
            keys.add(TextAttribute.NUMERIC_SHAPING);
        }

        private Object shaper = null;
    }

}
