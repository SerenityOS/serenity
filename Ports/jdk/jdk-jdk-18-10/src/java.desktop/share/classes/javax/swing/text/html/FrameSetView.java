/*
 * Copyright (c) 1998, 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.*;
import java.util.*;
import javax.swing.*;
import javax.swing.text.*;
import javax.swing.event.*;

/**
 * Implements a FrameSetView, intended to support the HTML
 * &lt;FRAMESET&gt; tag.  Supports the ROWS and COLS attributes.
 *
 * @author  Sunita Mani
 *
 *          Credit also to the hotjava browser engineers that
 *          worked on making the allocation of space algorithms
 *          conform to the HTML 4.0 standard and also be netscape
 *          compatible.
 *
 */

class FrameSetView extends javax.swing.text.BoxView {

    String[] children;
    int[] percentChildren;
    int[] absoluteChildren;
    int[] relativeChildren;
    int percentTotals;
    int absoluteTotals;
    int relativeTotals;

    /**
     * Constructs a FrameSetView for the given element.
     *
     * @param elem the element that this view is responsible for
     */
    public FrameSetView(Element elem, int axis) {
        super(elem, axis);
        children = null;
    }

    /**
     * Parses the ROW or COL attributes and returns
     * an array of strings that represent the space
     * distribution.
     *
     */
    private String[] parseRowColSpec(HTML.Attribute key) {

        AttributeSet attributes = getElement().getAttributes();
        String spec = "*";
        if (attributes != null) {
            if (attributes.getAttribute(key) != null) {
                spec = (String)attributes.getAttribute(key);
            }
        }

        StringTokenizer tokenizer = new StringTokenizer(spec, ",");
        int nTokens = tokenizer.countTokens();
        int n = getViewCount();
        String[] items = new String[Math.max(nTokens, n)];
        int i = 0;
        for (; i < nTokens; i++) {
            items[i] = tokenizer.nextToken().trim();
            // As per the spec, 100% is the same as *
            // hence the mapping.
            //
            if (items[i].equals("100%")) {
                items[i] = "*";
            }
        }
        // extend spec if we have more children than specified
        // in ROWS or COLS attribute
        for (; i < items.length; i++) {
            items[i] = "*";
        }
        return items;
    }


    /**
     * Initializes a number of internal state variables
     * that store information about space allocation
     * for the frames contained within the frameset.
     */
    private void init() {
        if (getAxis() == View.Y_AXIS) {
            children = parseRowColSpec(HTML.Attribute.ROWS);
        } else {
            children = parseRowColSpec(HTML.Attribute.COLS);
        }
        percentChildren = new int[children.length];
        relativeChildren = new int[children.length];
        absoluteChildren = new int[children.length];

        for (int i = 0; i < children.length; i++) {
            percentChildren[i] = -1;
            relativeChildren[i] = -1;
            absoluteChildren[i] = -1;

            if (children[i].endsWith("*")) {
                if (children[i].length() > 1) {
                    relativeChildren[i] =
                        Integer.parseInt(children[i].substring(
                            0, children[i].length()-1).trim());
                    relativeTotals += relativeChildren[i];
                } else {
                    relativeChildren[i] = 1;
                    relativeTotals += 1;
                }
            } else if (children[i].indexOf('%') != -1) {
                percentChildren[i] = parseDigits(children[i]);
                percentTotals += percentChildren[i];
            } else {
                String value = children[i].toLowerCase();
                if (value.endsWith("px")) {
                    value = value.substring(0, value.length()-2).trim();
                }
                absoluteChildren[i] = Integer.parseInt(value);
            }
        }
        if (percentTotals > 100) {
            for (int i = 0; i < percentChildren.length; i++) {
                if (percentChildren[i] > 0) {
                    percentChildren[i] =
                        (percentChildren[i] * 100) / percentTotals;
                }
            }
            percentTotals = 100;
        }
    }

    /**
     * Perform layout for the major axis of the box (i.e. the
     * axis that it represents).  The results of the layout should
     * be placed in the given arrays which represent the allocations
     * to the children along the major axis.
     *
     * @param targetSpan the total span given to the view, which
     *  would be used to layout the children
     * @param axis the axis being layed out
     * @param offsets the offsets from the origin of the view for
     *  each of the child views; this is a return value and is
     *  filled in by the implementation of this method
     * @param spans the span of each child view; this is a return
     *  value and is filled in by the implementation of this method
     * @return the offset and span for each child view in the
     *  offsets and spans parameters
     */
    protected void layoutMajorAxis(int targetSpan, int axis, int[] offsets,
                                   int[] spans) {
        if (children == null) {
            init();
        }
        SizeRequirements.calculateTiledPositions(targetSpan, null,
                                                 getChildRequests(targetSpan,
                                                                  axis),
                                                 offsets, spans);
    }

    protected SizeRequirements[] getChildRequests(int targetSpan, int axis) {

        int[] span = new int[children.length];

        spread(targetSpan, span);
        int n = getViewCount();
        SizeRequirements[] reqs = new SizeRequirements[n];
        for (int i = 0, sIndex = 0; i < n; i++) {
            View v = getView(i);
            if ((v instanceof FrameView) || (v instanceof FrameSetView)) {
                reqs[i] = new SizeRequirements((int) v.getMinimumSpan(axis),
                                               span[sIndex],
                                               (int) v.getMaximumSpan(axis),
                                               0.5f);
                sIndex++;
            } else {
                int min = (int) v.getMinimumSpan(axis);
                int pref = (int) v.getPreferredSpan(axis);
                int max = (int) v.getMaximumSpan(axis);
                float a = v.getAlignment(axis);
                reqs[i] = new SizeRequirements(min, pref, max, a);
            }
        }
        return reqs;
    }


    /**
     * This method is responsible for returning in span[] the
     * span for each child view along the major axis.  it
     * computes this based on the information that extracted
     * from the value of the ROW/COL attribute.
     */
    private void spread(int targetSpan, int[] span) {

        if (targetSpan == 0) {
            return;
        }

        int tempSpace = 0;
        int remainingSpace = targetSpan;

        // allocate the absolute's first, they have
        // precedence
        //
        for (int i = 0; i < span.length; i++) {
            if (absoluteChildren[i] > 0) {
                span[i] = absoluteChildren[i];
                remainingSpace -= span[i];
            }
        }

        // then deal with percents.
        //
        tempSpace = remainingSpace;
        for (int i = 0; i < span.length; i++) {
            if (percentChildren[i] > 0 && tempSpace > 0) {
                span[i] = (percentChildren[i] * tempSpace) / 100;
                remainingSpace -= span[i];
            } else if (percentChildren[i] > 0 && tempSpace <= 0) {
                span[i] = targetSpan / span.length;
                remainingSpace -= span[i];
            }
        }

        // allocate remainingSpace to relative
        if (remainingSpace > 0 && relativeTotals > 0) {
            for (int i = 0; i < span.length; i++) {
                if (relativeChildren[i] > 0) {
                    span[i] = (remainingSpace *
                                relativeChildren[i]) / relativeTotals;
                }
            }
        } else if (remainingSpace > 0) {
            // There are no relative columns and the space has been
            // under- or overallocated.  In this case, turn all the
            // percentage and pixel specified columns to percentage
            // columns based on the ratio of their pixel count to the
            // total "virtual" size. (In the case of percentage columns,
            // the pixel count would equal the specified percentage
            // of the screen size.

            // This action is in accordance with the HTML
            // 4.0 spec (see section 8.3, the end of the discussion of
            // the FRAMESET tag).  The precedence of percentage and pixel
            // specified columns is unclear (spec seems to indicate that
            // they share priority, however, unspecified what happens when
            // overallocation occurs.)

            // addendum is that we behave similar to netscape in that specified
            // widths have precedance over percentage widths...

            float vTotal = (float)(targetSpan - remainingSpace);
            float[] tempPercents = new float[span.length];
            remainingSpace = targetSpan;
            for (int i = 0; i < span.length; i++) {
                // ok we know what our total space is, and we know how large each
                // column should be relative to each other... therefore we can use
                // that relative information to deduce their percentages of a whole
                // and then scale them appropriately for the correct size
                tempPercents[i] = ((float)span[i] / vTotal) * 100.00f;
                span[i] = (int) ( ((float)targetSpan * tempPercents[i]) / 100.00f);
                remainingSpace -= span[i];
            }


            // this is for just in case there is something left over.. if there is we just
            // add it one pixel at a time to the frames in order.. We shouldn't really ever get
            // here and if we do it shouldn't be with more than 1 pixel, maybe two.
            int i = 0;
            while (remainingSpace != 0) {
                if (remainingSpace < 0) {
                    span[i++]--;
                    remainingSpace++;
                }
                else {
                    span[i++]++;
                    remainingSpace--;
                }

                // just in case there are more pixels than frames...should never happen..
                if (i == span.length)i = 0;
            }
        }
    }

    /*
     * Users have been known to type things like "%25" and "25 %".  Deal
     * with it.
     */
    private int parseDigits(String mixedStr) {
        int result = 0;
        for (int i = 0; i < mixedStr.length(); i++) {
            char ch = mixedStr.charAt(i);
            if (Character.isDigit(ch)) {
                result = (result * 10) + Character.digit(ch, 10);
            }
        }
        return result;
    }

}
