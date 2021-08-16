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
package javax.swing;


import java.awt.*;
import java.beans.ConstructorProperties;
import java.io.Serializable;

/**
 * A layout manager to arrange components over the top
 * of each other.  The requested size of the container
 * will be the largest requested size of the children,
 * taking alignment needs into consideration.
 *
 * The alignment is based upon what is needed to properly
 * fit the children in the allocation area.  The children
 * will be placed such that their alignment points are all
 * on top of each other.
 * <p>
 * <strong>Warning:</strong>
 * Serialized objects of this class will not be compatible with
 * future Swing releases. The current serialization support is
 * appropriate for short term storage or RMI between applications running
 * the same version of Swing.  As of 1.4, support for long term storage
 * of all JavaBeans
 * has been added to the <code>java.beans</code> package.
 * Please see {@link java.beans.XMLEncoder}.
 *
 * @author   Timothy Prinzing
 * @since 1.2
 */
@SuppressWarnings("serial") // Same-version serialization only
public class OverlayLayout implements LayoutManager2,Serializable {

    /**
     * Constructs a layout manager that performs overlay
     * arrangement of the children.  The layout manager
     * created is dedicated to the given container.
     *
     * @param target  the container to do layout against
     */
    @ConstructorProperties({"target"})
    public OverlayLayout(Container target) {
        this.target = target;
    }

    /**
     * Returns the container that uses this layout manager.
     *
     * @return the container that uses this layout manager
     *
     * @since 1.6
     */
    public final Container getTarget() {
        return this.target;
    }

    /**
     * Indicates a child has changed its layout related information,
     * which causes any cached calculations to be flushed.
     *
     * @param target the container
     */
    public void invalidateLayout(Container target) {
        checkContainer(target);
        xChildren = null;
        yChildren = null;
        xTotal = null;
        yTotal = null;
    }

    /**
     * Adds the specified component to the layout. Used by
     * this class to know when to invalidate layout.
     *
     * @param name the name of the component
     * @param comp the component to be added
     */
    public void addLayoutComponent(String name, Component comp) {
        invalidateLayout(comp.getParent());
    }

    /**
     * Removes the specified component from the layout. Used by
     * this class to know when to invalidate layout.
     *
     * @param comp the component to remove
     */
    public void removeLayoutComponent(Component comp) {
        invalidateLayout(comp.getParent());
    }

    /**
     * Adds the specified component to the layout, using the specified
     * constraint object. Used by this class to know when to invalidate
     * layout.
     *
     * @param comp the component to be added
     * @param constraints  where/how the component is added to the layout.
     */
    public void addLayoutComponent(Component comp, Object constraints) {
        invalidateLayout(comp.getParent());
    }

    /**
     * Returns the preferred dimensions for this layout given the components
     * in the specified target container.  Recomputes the layout if it
     * has been invalidated.  Factors in the current inset setting returned
     * by getInsets().
     *
     * @param target the component which needs to be laid out
     * @return a Dimension object containing the preferred dimensions
     * @see #minimumLayoutSize
     */
    public Dimension preferredLayoutSize(Container target) {
        checkContainer(target);
        checkRequests();

        Dimension size = new Dimension(xTotal.preferred, yTotal.preferred);
        Insets insets = target.getInsets();
        size.width += insets.left + insets.right;
        size.height += insets.top + insets.bottom;
        return size;
    }

    /**
     * Returns the minimum dimensions needed to lay out the components
     * contained in the specified target container.  Recomputes the layout
     * if it has been invalidated, and factors in the current inset setting.
     *
     * @param target the component which needs to be laid out
     * @return a Dimension object containing the minimum dimensions
     * @see #preferredLayoutSize
     */
    public Dimension minimumLayoutSize(Container target) {
        checkContainer(target);
        checkRequests();

        Dimension size = new Dimension(xTotal.minimum, yTotal.minimum);
        Insets insets = target.getInsets();
        size.width += insets.left + insets.right;
        size.height += insets.top + insets.bottom;
        return size;
    }

    /**
     * Returns the maximum dimensions needed to lay out the components
     * contained in the specified target container.  Recomputes the
     * layout if it has been invalidated, and factors in the inset setting
     * returned by <code>getInset</code>.
     *
     * @param target the component that needs to be laid out
     * @return a <code>Dimension</code> object containing the maximum
     *         dimensions
     * @see #preferredLayoutSize
     */
    public Dimension maximumLayoutSize(Container target) {
        checkContainer(target);
        checkRequests();

        Dimension size = new Dimension(xTotal.maximum, yTotal.maximum);
        Insets insets = target.getInsets();
        size.width += insets.left + insets.right;
        size.height += insets.top + insets.bottom;
        return size;
    }

    /**
     * Returns the alignment along the x axis for the container.
     *
     * @param target the container
     * @return the alignment &gt;= 0.0f &amp;&amp; &lt;= 1.0f
     */
    public float getLayoutAlignmentX(Container target) {
        checkContainer(target);
        checkRequests();
        return xTotal.alignment;
    }

    /**
     * Returns the alignment along the y axis for the container.
     *
     * @param target the container
     * @return the alignment &gt;= 0.0f &amp;&amp; &lt;= 1.0f
     */
    public float getLayoutAlignmentY(Container target) {
        checkContainer(target);
        checkRequests();
        return yTotal.alignment;
    }

    /**
     * Called by the AWT when the specified container needs to be laid out.
     *
     * @param target  the container to lay out
     *
     * @exception AWTError  if the target isn't the container specified to the
     *                      constructor
     */
    public void layoutContainer(Container target) {
        checkContainer(target);
        checkRequests();

        int nChildren = target.getComponentCount();
        int[] xOffsets = new int[nChildren];
        int[] xSpans = new int[nChildren];
        int[] yOffsets = new int[nChildren];
        int[] ySpans = new int[nChildren];

        // determine the child placements
        Dimension alloc = target.getSize();
        Insets in = target.getInsets();
        alloc.width -= in.left + in.right;
        alloc.height -= in.top + in.bottom;
        SizeRequirements.calculateAlignedPositions(alloc.width, xTotal,
                                                   xChildren, xOffsets,
                                                   xSpans);
        SizeRequirements.calculateAlignedPositions(alloc.height, yTotal,
                                                   yChildren, yOffsets,
                                                   ySpans);

        // flush changes to the container
        for (int i = 0; i < nChildren; i++) {
            Component c = target.getComponent(i);
            c.setBounds(in.left + xOffsets[i], in.top + yOffsets[i],
                        xSpans[i], ySpans[i]);
        }
    }

    void checkContainer(Container target) {
        if (this.target != target) {
            throw new AWTError("OverlayLayout can't be shared");
        }
    }

    void checkRequests() {
        if (xChildren == null || yChildren == null) {
            // The requests have been invalidated... recalculate
            // the request information.
            int n = target.getComponentCount();
            xChildren = new SizeRequirements[n];
            yChildren = new SizeRequirements[n];
            for (int i = 0; i < n; i++) {
                Component c = target.getComponent(i);
                Dimension min = c.getMinimumSize();
                Dimension typ = c.getPreferredSize();
                Dimension max = c.getMaximumSize();
                xChildren[i] = new SizeRequirements(min.width, typ.width,
                                                    max.width,
                                                    c.getAlignmentX());
                yChildren[i] = new SizeRequirements(min.height, typ.height,
                                                    max.height,
                                                    c.getAlignmentY());
            }

            xTotal = SizeRequirements.getAlignedSizeRequirements(xChildren);
            yTotal = SizeRequirements.getAlignedSizeRequirements(yChildren);
        }
    }

    private Container target;
    private SizeRequirements[] xChildren;
    private SizeRequirements[] yChildren;
    private SizeRequirements xTotal;
    private SizeRequirements yTotal;

}
