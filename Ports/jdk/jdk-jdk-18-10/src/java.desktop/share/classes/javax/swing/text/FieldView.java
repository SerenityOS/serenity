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
package javax.swing.text;

import java.awt.*;
import javax.swing.*;
import javax.swing.event.*;

/**
 * Extends the multi-line plain text view to be suitable
 * for a single-line editor view.  If the view is
 * allocated extra space, the field must adjust for it.
 * If the hosting component is a JTextField, this view
 * will manage the ranges of the associated BoundedRangeModel
 * and will adjust the horizontal allocation to match the
 * current visibility settings of the JTextField.
 *
 * @author  Timothy Prinzing
 * @see     View
 */
public class FieldView extends PlainView {

    /**
     * Constructs a new FieldView wrapped on an element.
     *
     * @param elem the element
     */
    public FieldView(Element elem) {
        super(elem);
    }

    /**
     * Fetches the font metrics associated with the component hosting
     * this view.
     *
     * @return the metrics
     */
    protected FontMetrics getFontMetrics() {
        Component c = getContainer();
        return c.getFontMetrics(c.getFont());
    }

    /**
     * Adjusts the allocation given to the view
     * to be a suitable allocation for a text field.
     * If the view has been allocated more than the
     * preferred span vertically, the allocation is
     * changed to be centered vertically.  Horizontally
     * the view is adjusted according to the horizontal
     * alignment property set on the associated JTextField
     * (if that is the type of the hosting component).
     *
     * @param a the allocation given to the view, which may need
     *  to be adjusted.
     * @return the allocation that the superclass should use.
     */
    protected Shape adjustAllocation(Shape a) {
        if (a != null) {
            Rectangle bounds = a.getBounds();
            int vspan = (int) getPreferredSpan(Y_AXIS);
            int hspan = (int) getPreferredSpan(X_AXIS);
            if (bounds.height != vspan) {
                int slop = bounds.height - vspan;
                bounds.y += slop / 2;
                bounds.height -= slop;
            }

            // horizontal adjustments
            Component c = getContainer();
            if (c instanceof JTextField) {
                JTextField field = (JTextField) c;
                BoundedRangeModel vis = field.getHorizontalVisibility();
                int max = Math.max(hspan, bounds.width);
                int value = vis.getValue();
                int extent = Math.min(max, bounds.width - 1);
                if ((value + extent) > max) {
                    value = max - extent;
                }
                vis.setRangeProperties(value, extent, vis.getMinimum(),
                                       max, false);
                if (hspan < bounds.width) {
                    // horizontally align the interior
                    int slop = bounds.width - 1 - hspan;

                    int align = ((JTextField)c).getHorizontalAlignment();
                    if(Utilities.isLeftToRight(c)) {
                        if(align==LEADING) {
                            align = LEFT;
                        }
                        else if(align==TRAILING) {
                            align = RIGHT;
                        }
                    }
                    else {
                        if(align==LEADING) {
                            align = RIGHT;
                        }
                        else if(align==TRAILING) {
                            align = LEFT;
                        }
                    }

                    switch (align) {
                    case SwingConstants.CENTER:
                        bounds.x += slop / 2;
                        bounds.width -= slop;
                        break;
                    case SwingConstants.RIGHT:
                        bounds.x += slop;
                        bounds.width -= slop;
                        break;
                    }
                } else {
                    // adjust the allocation to match the bounded range.
                    bounds.width = hspan;
                    bounds.x -= vis.getValue();
                }
            }
            return bounds;
        }
        return null;
    }

    /**
     * Update the visibility model with the associated JTextField
     * (if there is one) to reflect the current visibility as a
     * result of changes to the document model.  The bounded
     * range properties are updated.  If the view hasn't yet been
     * shown the extent will be zero and we just set it to be full
     * until determined otherwise.
     */
    void updateVisibilityModel() {
        Component c = getContainer();
        if (c instanceof JTextField) {
            JTextField field = (JTextField) c;
            BoundedRangeModel vis = field.getHorizontalVisibility();
            int hspan = (int) getPreferredSpan(X_AXIS);
            int extent = vis.getExtent();
            int maximum = Math.max(hspan, extent);
            extent = (extent == 0) ? maximum : extent;
            int value = maximum - extent;
            int oldValue = vis.getValue();
            if ((oldValue + extent) > maximum) {
                oldValue = maximum - extent;
            }
            value = Math.max(0, Math.min(value, oldValue));
            vis.setRangeProperties(value, extent, 0, maximum, false);
        }
    }

    // --- View methods -------------------------------------------

    /**
     * Renders using the given rendering surface and area on that surface.
     * The view may need to do layout and create child views to enable
     * itself to render into the given allocation.
     *
     * @param g the rendering surface to use
     * @param a the allocated region to render into
     *
     * @see View#paint
     */
    public void paint(Graphics g, Shape a) {
        Rectangle r = (Rectangle) a;
        g.clipRect(r.x, r.y, r.width, r.height);
        super.paint(g, a);
    }

    /**
     * Adjusts <code>a</code> based on the visible region and returns it.
     */
    Shape adjustPaintRegion(Shape a) {
        return adjustAllocation(a);
    }

    /**
     * Determines the preferred span for this view along an
     * axis.
     *
     * @param axis may be either View.X_AXIS or View.Y_AXIS
     * @return   the span the view would like to be rendered into &gt;= 0.
     *           Typically the view is told to render into the span
     *           that is returned, although there is no guarantee.
     *           The parent may choose to resize or break the view.
     */
    @SuppressWarnings("deprecation")
    public float getPreferredSpan(int axis) {
        switch (axis) {
        case View.X_AXIS:
            Segment buff = SegmentCache.getSharedSegment();
            Document doc = getDocument();
            int width;
            try {
                FontMetrics fm = getFontMetrics();
                doc.getText(0, doc.getLength(), buff);
                width = Utilities.getTabbedTextWidth(buff, fm, 0, this, 0);
                if (buff.count > 0) {
                    Component c = getContainer();
                    firstLineOffset = sun.swing.SwingUtilities2.
                        getLeftSideBearing((c instanceof JComponent) ?
                                           (JComponent)c : null, fm,
                                           buff.array[buff.offset]);
                    firstLineOffset = Math.max(0, -firstLineOffset);
                }
                else {
                    firstLineOffset = 0;
                }
            } catch (BadLocationException bl) {
                width = 0;
            }
            SegmentCache.releaseSharedSegment(buff);
            return width + firstLineOffset;
        default:
            return super.getPreferredSpan(axis);
        }
    }

    /**
     * Determines the resizability of the view along the
     * given axis.  A value of 0 or less is not resizable.
     *
     * @param axis View.X_AXIS or View.Y_AXIS
     * @return the weight -&gt; 1 for View.X_AXIS, else 0
     */
    public int getResizeWeight(int axis) {
        if (axis == View.X_AXIS) {
            return 1;
        }
        return 0;
    }

    /**
     * Provides a mapping from the document model coordinate space
     * to the coordinate space of the view mapped to it.
     *
     * @param pos the position to convert &gt;= 0
     * @param a the allocated region to render into
     * @return the bounding box of the given position
     * @exception BadLocationException  if the given position does not
     *   represent a valid location in the associated document
     * @see View#modelToView
     */
    public Shape modelToView(int pos, Shape a, Position.Bias b) throws BadLocationException {
        return super.modelToView(pos, adjustAllocation(a), b);
    }

    /**
     * Provides a mapping from the view coordinate space to the logical
     * coordinate space of the model.
     *
     * @param fx the X coordinate &gt;= 0.0f
     * @param fy the Y coordinate &gt;= 0.0f
     * @param a the allocated region to render into
     * @return the location within the model that best represents the
     *  given point in the view
     * @see View#viewToModel
     */
    public int viewToModel(float fx, float fy, Shape a, Position.Bias[] bias) {
        return super.viewToModel(fx, fy, adjustAllocation(a), bias);
    }

    /**
     * Gives notification that something was inserted into the document
     * in a location that this view is responsible for.
     *
     * @param changes the change information from the associated document
     * @param a the current allocation of the view
     * @param f the factory to use to rebuild if the view has children
     * @see View#insertUpdate
     */
    public void insertUpdate(DocumentEvent changes, Shape a, ViewFactory f) {
        super.insertUpdate(changes, adjustAllocation(a), f);
        updateVisibilityModel();
    }

    /**
     * Gives notification that something was removed from the document
     * in a location that this view is responsible for.
     *
     * @param changes the change information from the associated document
     * @param a the current allocation of the view
     * @param f the factory to use to rebuild if the view has children
     * @see View#removeUpdate
     */
    public void removeUpdate(DocumentEvent changes, Shape a, ViewFactory f) {
        super.removeUpdate(changes, adjustAllocation(a), f);
        updateVisibilityModel();
    }

}
