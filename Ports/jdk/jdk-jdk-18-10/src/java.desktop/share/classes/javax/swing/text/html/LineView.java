/*
 * Copyright (c) 1997, 2014, Oracle and/or its affiliates. All rights reserved.
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
package javax.swing.text.html;

import java.util.Enumeration;
import java.awt.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.text.*;

/**
 * A view implementation to display an unwrapped
 * preformatted line.<p>
 * This subclasses ParagraphView, but this really only contains one
 * Row of text.
 *
 * @author  Timothy Prinzing
 */
class LineView extends ParagraphView {
    /** Last place painted at. */
    int tabBase;

    /**
     * Creates a LineView object.
     *
     * @param elem the element to wrap in a view
     */
    public LineView(Element elem) {
        super(elem);
    }

    /**
     * Preformatted lines are not suppressed if they
     * have only whitespace, so they are always visible.
     */
    public boolean isVisible() {
        return true;
    }

    /**
     * Determines the minimum span for this view along an
     * axis.  The preformatted line should refuse to be
     * sized less than the preferred size.
     *
     * @param axis may be either <code>View.X_AXIS</code> or
     *  <code>View.Y_AXIS</code>
     * @return  the minimum span the view can be rendered into
     * @see View#getPreferredSpan
     */
    public float getMinimumSpan(int axis) {
        return getPreferredSpan(axis);
    }

    /**
     * Gets the resize weight for the specified axis.
     *
     * @param axis may be either X_AXIS or Y_AXIS
     * @return the weight
     */
    public int getResizeWeight(int axis) {
        switch (axis) {
        case View.X_AXIS:
            return 1;
        case View.Y_AXIS:
            return 0;
        default:
            throw new IllegalArgumentException("Invalid axis: " + axis);
        }
    }

    /**
     * Gets the alignment for an axis.
     *
     * @param axis may be either X_AXIS or Y_AXIS
     * @return the alignment
     */
    public float getAlignment(int axis) {
        if (axis == View.X_AXIS) {
            return 0;
        }
        return super.getAlignment(axis);
    }

    /**
     * Lays out the children.  If the layout span has changed,
     * the rows are rebuilt.  The superclass functionality
     * is called after checking and possibly rebuilding the
     * rows.  If the height has changed, the
     * <code>preferenceChanged</code> method is called
     * on the parent since the vertical preference is
     * rigid.
     *
     * @param width  the width to lay out against >= 0.  This is
     *   the width inside of the inset area.
     * @param height the height to lay out against >= 0 (not used
     *   by paragraph, but used by the superclass).  This
     *   is the height inside of the inset area.
     */
    protected void layout(int width, int height) {
        super.layout(Integer.MAX_VALUE - 1, height);
    }

    /**
     * Returns the next tab stop position given a reference position.
     * This view implements the tab coordinate system, and calls
     * <code>getTabbedSpan</code> on the logical children in the process
     * of layout to determine the desired span of the children.  The
     * logical children can delegate their tab expansion upward to
     * the paragraph which knows how to expand tabs.
     * <code>LabelView</code> is an example of a view that delegates
     * its tab expansion needs upward to the paragraph.
     * <p>
     * This is implemented to try and locate a <code>TabSet</code>
     * in the paragraph element's attribute set.  If one can be
     * found, its settings will be used, otherwise a default expansion
     * will be provided.  The base location for tab expansion
     * is the left inset from the paragraphs most recent allocation
     * (which is what the layout of the children is based upon).
     *
     * @param x the X reference position
     * @param tabOffset the position within the text stream
     *   that the tab occurred at >= 0.
     * @return the trailing end of the tab expansion >= 0
     * @see TabSet
     * @see TabStop
     * @see LabelView
     */
    public float nextTabStop(float x, int tabOffset) {
        // If the text isn't left justified, offset by 10 pixels!
        if (getTabSet() == null &&
            StyleConstants.getAlignment(getAttributes()) ==
            StyleConstants.ALIGN_LEFT) {
            return getPreTab(x, tabOffset);
        }
        return super.nextTabStop(x, tabOffset);
    }

    /**
     * Returns the location for the tab.
     */
    @SuppressWarnings("deprecation")
    protected float getPreTab(float x, int tabOffset) {
        Document d = getDocument();
        View v = getViewAtPosition(tabOffset, null);
        if ((d instanceof StyledDocument) && v != null) {
            // Assume f is fixed point.
            Font f = ((StyledDocument)d).getFont(v.getAttributes());
            Container c = getContainer();
            FontMetrics fm = (c != null) ? c.getFontMetrics(f) :
                Toolkit.getDefaultToolkit().getFontMetrics(f);
            int width = getCharactersPerTab() * fm.charWidth('W');
            int tb = (int)getTabBase();
            return (float)((((int)x - tb) / width + 1) * width + tb);
        }
        return 10.0f + x;
    }

    /**
     * @return number of characters per tab, 8.
     */
    protected int getCharactersPerTab() {
        return 8;
    }
}
