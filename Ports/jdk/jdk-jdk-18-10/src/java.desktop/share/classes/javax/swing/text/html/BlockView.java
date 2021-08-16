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
package javax.swing.text.html;

import java.util.Enumeration;
import java.awt.*;
import javax.swing.SizeRequirements;
import javax.swing.border.*;
import javax.swing.event.DocumentEvent;
import javax.swing.text.*;

/**
 * A view implementation to display a block (as a box)
 * with CSS specifications.
 *
 * @author  Timothy Prinzing
 */
public class BlockView extends BoxView  {

    /**
     * Creates a new view that represents an
     * html box.  This can be used for a number
     * of elements.
     *
     * @param elem the element to create a view for
     * @param axis either View.X_AXIS or View.Y_AXIS
     */
    public BlockView(Element elem, int axis) {
        super(elem, axis);
    }

    /**
     * Establishes the parent view for this view.  This is
     * guaranteed to be called before any other methods if the
     * parent view is functioning properly.
     * <p>
     * This is implemented
     * to forward to the superclass as well as call the
     * {@link #setPropertiesFromAttributes()}
     * method to set the paragraph properties from the css
     * attributes.  The call is made at this time to ensure
     * the ability to resolve upward through the parents
     * view attributes.
     *
     * @param parent the new parent, or null if the view is
     *  being removed from a parent it was previously added
     *  to
     */
    public void setParent(View parent) {
        super.setParent(parent);
        if (parent != null) {
            setPropertiesFromAttributes();
        }
    }

    /**
     * Calculate the requirements of the block along the major
     * axis (i.e. the axis along with it tiles).  This is implemented
     * to provide the superclass behavior and then adjust it if the
     * CSS width or height attribute is specified and applicable to
     * the axis.
     */
    protected SizeRequirements calculateMajorAxisRequirements(int axis, SizeRequirements r) {
        if (r == null) {
            r = new SizeRequirements();
        }
        if (! spanSetFromAttributes(axis, r, cssWidth, cssHeight)) {
            r = super.calculateMajorAxisRequirements(axis, r);
        }
        else {
            // Offset by the margins so that pref/min/max return the
            // right value.
            SizeRequirements parentR = super.calculateMajorAxisRequirements(
                                      axis, null);
            int margin = (axis == X_AXIS) ? getLeftInset() + getRightInset() :
                                            getTopInset() + getBottomInset();
            r.minimum -= margin;
            r.preferred -= margin;
            r.maximum -= margin;
            constrainSize(axis, r, parentR);
        }
        return r;
    }

    /**
     * Calculate the requirements of the block along the minor
     * axis (i.e. the axis orthogonal to the axis along with it tiles).
     * This is implemented
     * to provide the superclass behavior and then adjust it if the
     * CSS width or height attribute is specified and applicable to
     * the axis.
     */
    protected SizeRequirements calculateMinorAxisRequirements(int axis, SizeRequirements r) {
        if (r == null) {
            r = new SizeRequirements();
        }

        if (! spanSetFromAttributes(axis, r, cssWidth, cssHeight)) {

            /*
             * The requirements were not directly specified by attributes, so
             * compute the aggregate of the requirements of the children.  The
             * children that have a percentage value specified will be treated
             * as completely stretchable since that child is not limited in any
             * way.
             */
/*
            int min = 0;
            long pref = 0;
            int max = 0;
            int n = getViewCount();
            for (int i = 0; i < n; i++) {
                View v = getView(i);
                min = Math.max((int) v.getMinimumSpan(axis), min);
                pref = Math.max((int) v.getPreferredSpan(axis), pref);
                if (
                max = Math.max((int) v.getMaximumSpan(axis), max);

            }
            r.preferred = (int) pref;
            r.minimum = min;
            r.maximum = max;
            */
            r = super.calculateMinorAxisRequirements(axis, r);
        }
        else {
            // Offset by the margins so that pref/min/max return the
            // right value.
            SizeRequirements parentR = super.calculateMinorAxisRequirements(
                                      axis, null);
            int margin = (axis == X_AXIS) ? getLeftInset() + getRightInset() :
                                            getTopInset() + getBottomInset();
            r.minimum -= margin;
            r.preferred -= margin;
            r.maximum -= margin;
            constrainSize(axis, r, parentR);
        }

        /*
         * Set the alignment based upon the CSS properties if it is
         * specified.  For X_AXIS this would be text-align, for
         * Y_AXIS this would be vertical-align.
         */
        if (axis == X_AXIS) {
            Object o = getAttributes().getAttribute(CSS.Attribute.TEXT_ALIGN);
            if (o != null) {
                String align = o.toString();
                if (align.equals("center")) {
                    r.alignment = 0.5f;
                } else if (align.equals("right")) {
                    r.alignment = 1.0f;
                } else {
                    r.alignment = 0.0f;
                }
            }
        }
        // Y_AXIS TBD
        return r;
    }

    boolean isPercentage(int axis, AttributeSet a) {
        if (axis == X_AXIS) {
            if (cssWidth != null) {
                return cssWidth.isPercentage();
            }
        } else {
            if (cssHeight != null) {
                return cssHeight.isPercentage();
            }
        }
        return false;
    }

    /**
     * Adjust the given requirements to the CSS width or height if
     * it is specified along the applicable axis.  Return true if the
     * size is exactly specified, false if the span is not specified
     * in an attribute or the size specified is a percentage.
     */
    static boolean spanSetFromAttributes(int axis, SizeRequirements r,
                                         CSS.LengthValue cssWidth,
                                         CSS.LengthValue cssHeight) {
        if (axis == X_AXIS) {
            if ((cssWidth != null) && (! cssWidth.isPercentage())) {
                r.minimum = r.preferred = r.maximum = (int) cssWidth.getValue();
                return true;
            }
        } else {
            if ((cssHeight != null) && (! cssHeight.isPercentage())) {
                r.minimum = r.preferred = r.maximum = (int) cssHeight.getValue();
                return true;
            }
        }
        return false;
    }

    /**
     * Performs layout for the minor axis of the box (i.e. the
     * axis orthogonal to the axis that it represents). The results
     * of the layout (the offset and span for each children) are
     * placed in the given arrays which represent the allocations to
     * the children along the minor axis.
     *
     * @param targetSpan the total span given to the view, which
     *  would be used to layout the children.
     * @param axis the axis being layed out
     * @param offsets the offsets from the origin of the view for
     *  each of the child views; this is a return value and is
     *  filled in by the implementation of this method
     * @param spans the span of each child view; this is a return
     *  value and is filled in by the implementation of this method
     */
    protected void layoutMinorAxis(int targetSpan, int axis, int[] offsets, int[] spans) {
        int n = getViewCount();
        Object key = (axis == X_AXIS) ? CSS.Attribute.WIDTH : CSS.Attribute.HEIGHT;
        for (int i = 0; i < n; i++) {
            View v = getView(i);
            int min = (int) v.getMinimumSpan(axis);
            int max;

            // check for percentage span
            AttributeSet a = v.getAttributes();
            CSS.LengthValue lv = (CSS.LengthValue) a.getAttribute(key);
            if ((lv != null) && lv.isPercentage()) {
                // bound the span to the percentage specified
                min = Math.max((int) lv.getValue(targetSpan), min);
                max = min;
            } else {
                max = (int)v.getMaximumSpan(axis);
            }

            // assign the offset and span for the child
            if (max < targetSpan) {
                // can't make the child this wide, align it
                float align = v.getAlignment(axis);
                offsets[i] = (int) ((targetSpan - max) * align);
                spans[i] = max;
            } else {
                // make it the target width, or as small as it can get.
                offsets[i] = 0;
                spans[i] = Math.max(min, targetSpan);
            }
        }
    }


    /**
     * Renders using the given rendering surface and area on that
     * surface.  This is implemented to delegate to the css box
     * painter to paint the border and background prior to the
     * interior.
     *
     * @param g the rendering surface to use
     * @param allocation the allocated region to render into
     * @see View#paint
     */
    public void paint(Graphics g, Shape allocation) {
        Rectangle a = (Rectangle) allocation;
        painter.paint(g, a.x, a.y, a.width, a.height, this);
        super.paint(g, a);
    }

    /**
     * Fetches the attributes to use when rendering.  This is
     * implemented to multiplex the attributes specified in the
     * model with a StyleSheet.
     */
    public AttributeSet getAttributes() {
        if (attr == null) {
            StyleSheet sheet = getStyleSheet();
            attr = sheet.getViewAttributes(this);
        }
        return attr;
    }

    /**
     * Gets the resize weight.
     *
     * @param axis may be either X_AXIS or Y_AXIS
     * @return the weight
     * @exception IllegalArgumentException for an invalid axis
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
     * Gets the alignment.
     *
     * @param axis may be either X_AXIS or Y_AXIS
     * @return the alignment
     */
    public float getAlignment(int axis) {
        switch (axis) {
        case View.X_AXIS:
            return 0;
        case View.Y_AXIS:
            if (getViewCount() == 0) {
                return 0;
            }
            float span = getPreferredSpan(View.Y_AXIS);
            View v = getView(0);
            float above = v.getPreferredSpan(View.Y_AXIS);
            float a = (((int)span) != 0) ? (above * v.getAlignment(View.Y_AXIS)) / span: 0;
            return a;
        default:
            throw new IllegalArgumentException("Invalid axis: " + axis);
        }
    }

    public void changedUpdate(DocumentEvent changes, Shape a, ViewFactory f) {
        super.changedUpdate(changes, a, f);
        int pos = changes.getOffset();
        if (pos <= getStartOffset() && (pos + changes.getLength()) >=
            getEndOffset()) {
            setPropertiesFromAttributes();
        }
    }

    /**
     * Determines the preferred span for this view along an
     * axis.
     *
     * @param axis may be either <code>View.X_AXIS</code>
     *           or <code>View.Y_AXIS</code>
     * @return   the span the view would like to be rendered into &gt;= 0;
     *           typically the view is told to render into the span
     *           that is returned, although there is no guarantee;
     *           the parent may choose to resize or break the view
     * @exception IllegalArgumentException for an invalid axis type
     */
    public float getPreferredSpan(int axis) {
        return super.getPreferredSpan(axis);
    }

    /**
     * Determines the minimum span for this view along an
     * axis.
     *
     * @param axis may be either <code>View.X_AXIS</code>
     *           or <code>View.Y_AXIS</code>
     * @return  the span the view would like to be rendered into &gt;= 0;
     *           typically the view is told to render into the span
     *           that is returned, although there is no guarantee;
     *           the parent may choose to resize or break the view
     * @exception IllegalArgumentException for an invalid axis type
     */
    public float getMinimumSpan(int axis) {
        return super.getMinimumSpan(axis);
    }

    /**
     * Determines the maximum span for this view along an
     * axis.
     *
     * @param axis may be either <code>View.X_AXIS</code>
     *           or <code>View.Y_AXIS</code>
     * @return   the span the view would like to be rendered into &gt;= 0;
     *           typically the view is told to render into the span
     *           that is returned, although there is no guarantee;
     *           the parent may choose to resize or break the view
     * @exception IllegalArgumentException for an invalid axis type
     */
    public float getMaximumSpan(int axis) {
        return super.getMaximumSpan(axis);
    }

    /**
     * Update any cached values that come from attributes.
     */
    protected void setPropertiesFromAttributes() {

        // update attributes
        StyleSheet sheet = getStyleSheet();
        attr = sheet.getViewAttributes(this);

        // Reset the painter
        painter = sheet.getBoxPainter(attr);
        if (attr != null) {
            setInsets((short) painter.getInset(TOP, this),
                      (short) painter.getInset(LEFT, this),
                      (short) painter.getInset(BOTTOM, this),
                      (short) painter.getInset(RIGHT, this));
        }

        // Get the width/height
        cssWidth = (CSS.LengthValue) attr.getAttribute(CSS.Attribute.WIDTH);
        cssHeight = (CSS.LengthValue) attr.getAttribute(CSS.Attribute.HEIGHT);
    }

    /**
     * Convenient method to get the StyleSheet.
     *
     * @return the StyleSheet
     */
    protected StyleSheet getStyleSheet() {
        HTMLDocument doc = (HTMLDocument) getDocument();
        return doc.getStyleSheet();
    }

    /**
     * Constrains <code>want</code> to fit in the minimum size specified
     * by <code>min</code>.
     */
    private void constrainSize(int axis, SizeRequirements want,
                               SizeRequirements min) {
        if (min.minimum > want.minimum) {
            want.minimum = want.preferred = min.minimum;
            want.maximum = Math.max(want.maximum, min.maximum);
        }
    }

    private AttributeSet attr;
    private StyleSheet.BoxPainter painter;

    private CSS.LengthValue cssWidth;
    private CSS.LengthValue cssHeight;

}
