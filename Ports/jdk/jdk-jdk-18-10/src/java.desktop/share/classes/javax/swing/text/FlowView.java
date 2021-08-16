/*
 * Copyright (c) 1999, 2015, Oracle and/or its affiliates. All rights reserved.
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
import java.util.Vector;
import javax.swing.event.*;
import javax.swing.SizeRequirements;

/**
 * A View that tries to flow it's children into some
 * partially constrained space.  This can be used to
 * build things like paragraphs, pages, etc.  The
 * flow is made up of the following pieces of functionality.
 * <ul>
 * <li>A logical set of child views, which as used as a
 * layout pool from which a physical view is formed.
 * <li>A strategy for translating the logical view to
 * a physical (flowed) view.
 * <li>Constraints for the strategy to work against.
 * <li>A physical structure, that represents the flow.
 * The children of this view are where the pieces of
 * of the logical views are placed to create the flow.
 * </ul>
 *
 * @author  Timothy Prinzing
 * @see     View
 * @since 1.3
 */
public abstract class FlowView extends BoxView {

    /**
     * Constructs a FlowView for the given element.
     *
     * @param elem the element that this view is responsible for
     * @param axis may be either View.X_AXIS or View.Y_AXIS
     */
    public FlowView(Element elem, int axis) {
        super(elem, axis);
        layoutSpan = Integer.MAX_VALUE;
        strategy = new FlowStrategy();
    }

    /**
     * Fetches the axis along which views should be
     * flowed.  By default, this will be the axis
     * orthogonal to the axis along which the flow
     * rows are tiled (the axis of the default flow
     * rows themselves).  This is typically used
     * by the <code>FlowStrategy</code>.
     * @return the axis along which views should be
     * flowed
     */
    public int getFlowAxis() {
        if (getAxis() == Y_AXIS) {
            return X_AXIS;
        }
        return Y_AXIS;
    }

    /**
     * Fetch the constraining span to flow against for
     * the given child index.  This is called by the
     * FlowStrategy while it is updating the flow.
     * A flow can be shaped by providing different values
     * for the row constraints.  By default, the entire
     * span inside of the insets along the flow axis
     * is returned.
     *
     * @param index the index of the row being updated.
     *   This should be a value &gt;= 0 and &lt; getViewCount().
     * @return the constraining span to flow against for
     * the given child index
     * @see #getFlowStart
     */
    public int getFlowSpan(int index) {
        return layoutSpan;
    }

    /**
     * Fetch the location along the flow axis that the
     * flow span will start at.  This is called by the
     * FlowStrategy while it is updating the flow.
     * A flow can be shaped by providing different values
     * for the row constraints.

     * @param index the index of the row being updated.
     *   This should be a value &gt;= 0 and &lt; getViewCount().
     * @return the location along the flow axis that the
     * flow span will start at
     * @see #getFlowSpan
     */
    public int getFlowStart(int index) {
        return 0;
    }

    /**
     * Create a View that should be used to hold a
     * a rows worth of children in a flow.  This is
     * called by the FlowStrategy when new children
     * are added or removed (i.e. rows are added or
     * removed) in the process of updating the flow.
     * @return a View that should be used to hold a
     * a rows worth of children in a flow
     */
    protected abstract View createRow();

    // ---- BoxView methods -------------------------------------

    /**
     * Loads all of the children to initialize the view.
     * This is called by the <code>setParent</code> method.
     * This is reimplemented to not load any children directly
     * (as they are created in the process of formatting).
     * If the layoutPool variable is null, an instance of
     * LogicalView is created to represent the logical view
     * that is used in the process of formatting.
     *
     * @param f the view factory
     */
    protected void loadChildren(ViewFactory f) {
        if (layoutPool == null) {
            layoutPool = new LogicalView(getElement());
        }
        layoutPool.setParent(this);

        // This synthetic insertUpdate call gives the strategy a chance
        // to initialize.
        strategy.insertUpdate(this, null, null);
    }

    /**
     * Fetches the child view index representing the given position in
     * the model.
     *
     * @param pos the position &gt;= 0
     * @return  index of the view representing the given position, or
     *   -1 if no view represents that position
     */
    protected int getViewIndexAtPosition(int pos) {
        if (pos >= getStartOffset() && (pos < getEndOffset())) {
            for (int counter = 0; counter < getViewCount(); counter++) {
                View v = getView(counter);
                if(pos >= v.getStartOffset() &&
                   pos < v.getEndOffset()) {
                    return counter;
                }
            }
        }
        return -1;
    }

    /**
     * Lays out the children.  If the span along the flow
     * axis has changed, layout is marked as invalid which
     * which will cause the superclass behavior to recalculate
     * the layout along the box axis.  The FlowStrategy.layout
     * method will be called to rebuild the flow rows as
     * appropriate.  If the height of this view changes
     * (determined by the preferred size along the box axis),
     * a preferenceChanged is called.  Following all of that,
     * the normal box layout of the superclass is performed.
     *
     * @param width  the width to lay out against &gt;= 0.  This is
     *   the width inside of the inset area.
     * @param height the height to lay out against &gt;= 0 This
     *   is the height inside of the inset area.
     */
    protected void layout(int width, int height) {
        final int faxis = getFlowAxis();
        int newSpan;
        if (faxis == X_AXIS) {
            newSpan = width;
        } else {
            newSpan = height;
        }
        if (layoutSpan != newSpan) {
            layoutChanged(faxis);
            layoutChanged(getAxis());
            layoutSpan = newSpan;
        }

        // repair the flow if necessary
        if (! isLayoutValid(faxis)) {
            final int heightAxis = getAxis();
            int oldFlowHeight = (heightAxis == X_AXIS)? getWidth() : getHeight();
            strategy.layout(this);
            int newFlowHeight = (int) getPreferredSpan(heightAxis);
            if (oldFlowHeight != newFlowHeight) {
                View p = getParent();
                if (p != null) {
                    p.preferenceChanged(this, (heightAxis == X_AXIS), (heightAxis == Y_AXIS));
                }

                // PENDING(shannonh)
                // Temporary fix for 4250847
                // Can be removed when TraversalContext is added
                Component host = getContainer();
                if (host != null) {
                    //nb idk 12/12/2001 host should not be equal to null. We need to add assertion here
                    host.repaint();
                }
            }
        }

        super.layout(width, height);
    }

    /**
     * Calculate requirements along the minor axis.  This
     * is implemented to forward the request to the logical
     * view by calling getMinimumSpan, getPreferredSpan, and
     * getMaximumSpan on it.
     */
    protected SizeRequirements calculateMinorAxisRequirements(int axis, SizeRequirements r) {
        if (r == null) {
            r = new SizeRequirements();
        }
        float pref = layoutPool.getPreferredSpan(axis);
        float min = layoutPool.getMinimumSpan(axis);
        // Don't include insets, Box.getXXXSpan will include them.
        r.minimum = (int)min;
        r.preferred = Math.max(r.minimum, (int) pref);
        r.maximum = Integer.MAX_VALUE;
        r.alignment = 0.5f;
        return r;
    }

    // ---- View methods ----------------------------------------------------

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
        layoutPool.insertUpdate(changes, a, f);
        strategy.insertUpdate(this, changes, getInsideAllocation(a));
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
        layoutPool.removeUpdate(changes, a, f);
        strategy.removeUpdate(this, changes, getInsideAllocation(a));
    }

    /**
     * Gives notification from the document that attributes were changed
     * in a location that this view is responsible for.
     *
     * @param changes the change information from the associated document
     * @param a the current allocation of the view
     * @param f the factory to use to rebuild if the view has children
     * @see View#changedUpdate
     */
    public void changedUpdate(DocumentEvent changes, Shape a, ViewFactory f) {
        layoutPool.changedUpdate(changes, a, f);
        strategy.changedUpdate(this, changes, getInsideAllocation(a));
    }

    /** {@inheritDoc} */
    public void setParent(View parent) {
        super.setParent(parent);
        if (parent == null
                && layoutPool != null ) {
            layoutPool.setParent(null);
        }
    }

    // --- variables -----------------------------------------------

    /**
     * Default constraint against which the flow is
     * created against.
     */
    protected int layoutSpan;

    /**
     * These are the views that represent the child elements
     * of the element this view represents (The logical view
     * to translate to a physical view).  These are not
     * directly children of this view.  These are either
     * placed into the rows directly or used for the purpose
     * of breaking into smaller chunks, to form the physical
     * view.
     */
    protected View layoutPool;

    /**
     * The behavior for keeping the flow updated.  By
     * default this is a singleton shared by all instances
     * of FlowView (FlowStrategy is stateless).  Subclasses
     * can create an alternative strategy, which might keep
     * state.
     */
    protected FlowStrategy strategy;

    /**
     * Strategy for maintaining the physical form
     * of the flow.  The default implementation is
     * completely stateless, and recalculates the
     * entire flow if the layout is invalid on the
     * given FlowView.  Alternative strategies can
     * be implemented by subclassing, and might
     * perform incremental repair to the layout
     * or alternative breaking behavior.
     * @since 1.3
     */
    public static class FlowStrategy {
        /**
         * Constructs a {@code FlowStrategy}.
         */
        public FlowStrategy() {}

        Position damageStart = null;
        Vector<View> viewBuffer;

        void addDamage(FlowView fv, int offset) {
            if (offset >= fv.getStartOffset() && offset < fv.getEndOffset()) {
                if (damageStart == null || offset < damageStart.getOffset()) {
                    try {
                        damageStart = fv.getDocument().createPosition(offset);
                    } catch (BadLocationException e) {
                        // shouldn't happen since offset is inside view bounds
                        assert(false);
                    }
                }
            }
        }

        void unsetDamage() {
            damageStart = null;
        }

        /**
         * Gives notification that something was inserted into the document
         * in a location that the given flow view is responsible for.  The
         * strategy should update the appropriate changed region (which
         * depends upon the strategy used for repair).
         *
         * @param fv the flow view
         * @param e the change information from the associated document
         * @param alloc the current allocation of the view inside of the insets.
         *   This value will be null if the view has not yet been displayed.
         * @see View#insertUpdate
         */
        public void insertUpdate(FlowView fv, DocumentEvent e, Rectangle alloc) {
            // FlowView.loadChildren() makes a synthetic call into this,
            // passing null as e
            if (e != null) {
                addDamage(fv, e.getOffset());
            }

            if (alloc != null) {
                Component host = fv.getContainer();
                if (host != null) {
                    host.repaint(alloc.x, alloc.y, alloc.width, alloc.height);
                }
            } else {
                fv.preferenceChanged(null, true, true);
            }
        }

        /**
         * Gives notification that something was removed from the document
         * in a location that the given flow view is responsible for.
         *
         * @param fv the flow view
         * @param e the change information from the associated document
         * @param alloc the current allocation of the view inside of the insets.
         * @see View#removeUpdate
         */
        public void removeUpdate(FlowView fv, DocumentEvent e, Rectangle alloc) {
            addDamage(fv, e.getOffset());
            if (alloc != null) {
                Component host = fv.getContainer();
                if (host != null) {
                    host.repaint(alloc.x, alloc.y, alloc.width, alloc.height);
                }
            } else {
                fv.preferenceChanged(null, true, true);
            }
        }

        /**
         * Gives notification from the document that attributes were changed
         * in a location that this view is responsible for.
         *
         * @param fv     the <code>FlowView</code> containing the changes
         * @param e      the <code>DocumentEvent</code> describing the changes
         *               done to the Document
         * @param alloc  Bounds of the View
         * @see View#changedUpdate
         */
        public void changedUpdate(FlowView fv, DocumentEvent e, Rectangle alloc) {
            addDamage(fv, e.getOffset());
            if (alloc != null) {
                Component host = fv.getContainer();
                if (host != null) {
                    host.repaint(alloc.x, alloc.y, alloc.width, alloc.height);
                }
            } else {
                fv.preferenceChanged(null, true, true);
            }
        }

        /**
         * This method gives flow strategies access to the logical
         * view of the FlowView.
         * @param fv the FlowView
         * @return the logical view of the FlowView
         */
        protected View getLogicalView(FlowView fv) {
            return fv.layoutPool;
        }

        /**
         * Update the flow on the given FlowView.  By default, this causes
         * all of the rows (child views) to be rebuilt to match the given
         * constraints for each row.  This is called by a FlowView.layout
         * to update the child views in the flow.
         *
         * @param fv the view to reflow
         */
        public void layout(FlowView fv) {
            View pool = getLogicalView(fv);
            int rowIndex, p0;
            int p1 = fv.getEndOffset();

            if (fv.majorAllocValid) {
                if (damageStart == null) {
                    return;
                }
                // In some cases there's no view at position damageStart, so
                // step back and search again. See 6452106 for details.
                int offset = damageStart.getOffset();
                while ((rowIndex = fv.getViewIndexAtPosition(offset)) < 0) {
                    offset--;
                }
                if (rowIndex > 0) {
                    rowIndex--;
                }
                p0 = fv.getView(rowIndex).getStartOffset();
            } else {
                rowIndex = 0;
                p0 = fv.getStartOffset();
            }
            reparentViews(pool, p0);

            viewBuffer = new Vector<View>(10, 10);
            int rowCount = fv.getViewCount();
            while (p0 < p1) {
                View row;
                if (rowIndex >= rowCount) {
                    row = fv.createRow();
                    fv.append(row);
                } else {
                    row = fv.getView(rowIndex);
                }
                p0 = layoutRow(fv, rowIndex, p0);
                rowIndex++;
            }
            viewBuffer = null;

            if (rowIndex < rowCount) {
                fv.replace(rowIndex, rowCount - rowIndex, null);
            }
            unsetDamage();
        }

        /**
         * Creates a row of views that will fit within the
         * layout span of the row.  This is called by the layout method.
         * This is implemented to fill the row by repeatedly calling
         * the createView method until the available span has been
         * exhausted, a forced break was encountered, or the createView
         * method returned null.  If the remaining span was exhausted,
         * the adjustRow method will be called to perform adjustments
         * to the row to try and make it fit into the given span.
         *
         * @param fv the flow view
         * @param rowIndex the index of the row to fill in with views.  The
         *   row is assumed to be empty on entry.
         * @param pos  The current position in the children of
         *   this views element from which to start.
         * @return the position to start the next row
         */
        protected int layoutRow(FlowView fv, int rowIndex, int pos) {
            View row = fv.getView(rowIndex);
            float x = fv.getFlowStart(rowIndex);
            float spanLeft = fv.getFlowSpan(rowIndex);
            int end = fv.getEndOffset();
            TabExpander te = (fv instanceof TabExpander) ? (TabExpander)fv : null;
            final int flowAxis = fv.getFlowAxis();

            int breakWeight = BadBreakWeight;
            float breakX = 0f;
            float breakSpan = 0f;
            int breakIndex = -1;
            int n = 0;

            viewBuffer.clear();
            while (pos < end && spanLeft >= 0) {
                View v = createView(fv, pos, (int)spanLeft, rowIndex);
                if (v == null) {
                    break;
                }

                int bw = v.getBreakWeight(flowAxis, x, spanLeft);
                if (bw >= ForcedBreakWeight) {
                    View w = v.breakView(flowAxis, pos, x, spanLeft);
                    if (w != null) {
                        viewBuffer.add(w);
                    } else if (n == 0) {
                        // if the view does not break, and it is the only view
                        // in a row, use the whole view
                        viewBuffer.add(v);
                    }
                    break;
                } else if (bw >= breakWeight && bw > BadBreakWeight) {
                    breakWeight = bw;
                    breakX = x;
                    breakSpan = spanLeft;
                    breakIndex = n;
                }

                float chunkSpan;
                if (flowAxis == X_AXIS && v instanceof TabableView) {
                    chunkSpan = ((TabableView)v).getTabbedSpan(x, te);
                } else {
                    chunkSpan = v.getPreferredSpan(flowAxis);
                }

                if (chunkSpan > spanLeft && breakIndex >= 0) {
                    // row is too long, and we may break
                    if (breakIndex < n) {
                        v = viewBuffer.get(breakIndex);
                    }
                    for (int i = n - 1; i >= breakIndex; i--) {
                        viewBuffer.remove(i);
                    }
                    v = v.breakView(flowAxis, v.getStartOffset(), breakX, breakSpan);
                }

                spanLeft -= chunkSpan;
                x += chunkSpan;
                viewBuffer.add(v);
                pos = v.getEndOffset();
                n++;
            }

            View[] views = new View[viewBuffer.size()];
            viewBuffer.toArray(views);
            row.replace(0, row.getViewCount(), views);
            return (views.length > 0 ? row.getEndOffset() : pos);
        }

        /**
         * Adjusts the given row if possible to fit within the
         * layout span.  By default this will try to find the
         * highest break weight possible nearest the end of
         * the row.  If a forced break is encountered, the
         * break will be positioned there.
         *
         * @param fv the flow view
         * @param rowIndex the row to adjust to the current layout
         *  span.
         * @param desiredSpan the current layout span &gt;= 0
         * @param x the location r starts at.
         */
        protected void adjustRow(FlowView fv, int rowIndex, int desiredSpan, int x) {
            final int flowAxis = fv.getFlowAxis();
            View r = fv.getView(rowIndex);
            int n = r.getViewCount();
            int span = 0;
            int bestWeight = BadBreakWeight;
            int bestSpan = 0;
            int bestIndex = -1;
            View v;
            for (int i = 0; i < n; i++) {
                v = r.getView(i);
                int spanLeft = desiredSpan - span;

                int w = v.getBreakWeight(flowAxis, x + span, spanLeft);
                if ((w >= bestWeight) && (w > BadBreakWeight)) {
                    bestWeight = w;
                    bestIndex = i;
                    bestSpan = span;
                    if (w >= ForcedBreakWeight) {
                        // it's a forced break, so there is
                        // no point in searching further.
                        break;
                    }
                }
                span += v.getPreferredSpan(flowAxis);
            }
            if (bestIndex < 0) {
                // there is nothing that can be broken, leave
                // it in it's current state.
                return;
            }

            // Break the best candidate view, and patch up the row.
            int spanLeft = desiredSpan - bestSpan;
            v = r.getView(bestIndex);
            v = v.breakView(flowAxis, v.getStartOffset(), x + bestSpan, spanLeft);
            View[] va = new View[1];
            va[0] = v;
            View lv = getLogicalView(fv);
            int p0 = r.getView(bestIndex).getStartOffset();
            int p1 = r.getEndOffset();
            for (int i = 0; i < lv.getViewCount(); i++) {
                View tmpView = lv.getView(i);
                if (tmpView.getEndOffset() > p1) {
                    break;
                }
                if (tmpView.getStartOffset() >= p0) {
                    tmpView.setParent(lv);
                }
            }
            r.replace(bestIndex, n - bestIndex, va);
        }

        void reparentViews(View pool, int startPos) {
            int n = pool.getViewIndex(startPos, Position.Bias.Forward);
            if (n >= 0) {
                for (int i = n; i < pool.getViewCount(); i++) {
                    pool.getView(i).setParent(pool);
                }
            }
        }

        /**
         * Creates a view that can be used to represent the current piece
         * of the flow.  This can be either an entire view from the
         * logical view, or a fragment of the logical view.
         *
         * @param fv the view holding the flow
         * @param startOffset the start location for the view being created
         * @param spanLeft the about of span left to fill in the row
         * @param rowIndex the row the view will be placed into
         * @return a view that can be used to represent the current piece
         * of the flow
         */
        protected View createView(FlowView fv, int startOffset, int spanLeft, int rowIndex) {
            // Get the child view that contains the given starting position
            View lv = getLogicalView(fv);
            int childIndex = lv.getViewIndex(startOffset, Position.Bias.Forward);
            View v = lv.getView(childIndex);
            if (startOffset==v.getStartOffset()) {
                // return the entire view
                return v;
            }

            // return a fragment.
            v = v.createFragment(startOffset, v.getEndOffset());
            return v;
        }
    }

    /**
     * This class can be used to represent a logical view for
     * a flow.  It keeps the children updated to reflect the state
     * of the model, gives the logical child views access to the
     * view hierarchy, and calculates a preferred span.  It doesn't
     * do any rendering, layout, or model/view translation.
     */
    static class LogicalView extends CompositeView {

        LogicalView(Element elem) {
            super(elem);
        }

        protected int getViewIndexAtPosition(int pos) {
            Element elem = getElement();
            if (elem.isLeaf()) {
                return 0;
            }
            return super.getViewIndexAtPosition(pos);
        }

        protected void loadChildren(ViewFactory f) {
            Element elem = getElement();
            if (elem.isLeaf()) {
                View v = new LabelView(elem);
                append(v);
            } else {
                super.loadChildren(f);
            }
        }

        /**
         * Fetches the attributes to use when rendering.  This view
         * isn't directly responsible for an element so it returns
         * the outer classes attributes.
         */
        public AttributeSet getAttributes() {
            View p = getParent();
            return (p != null) ? p.getAttributes() : null;
        }

        /**
         * Determines the preferred span for this view along an
         * axis.
         *
         * @param axis may be either View.X_AXIS or View.Y_AXIS
         * @return   the span the view would like to be rendered into.
         *           Typically the view is told to render into the span
         *           that is returned, although there is no guarantee.
         *           The parent may choose to resize or break the view.
         * @see View#getPreferredSpan
         */
        public float getPreferredSpan(int axis) {
            float maxpref = 0;
            float pref = 0;
            int n = getViewCount();
            for (int i = 0; i < n; i++) {
                View v = getView(i);
                pref += v.getPreferredSpan(axis);
                if (v.getBreakWeight(axis, 0, Integer.MAX_VALUE) >= ForcedBreakWeight) {
                    maxpref = Math.max(maxpref, pref);
                    pref = 0;
                }
            }
            maxpref = Math.max(maxpref, pref);
            return maxpref;
        }

        /**
         * Determines the minimum span for this view along an
         * axis.  The is implemented to find the minimum unbreakable
         * span.
         *
         * @param axis may be either View.X_AXIS or View.Y_AXIS
         * @return  the span the view would like to be rendered into.
         *           Typically the view is told to render into the span
         *           that is returned, although there is no guarantee.
         *           The parent may choose to resize or break the view.
         * @see View#getPreferredSpan
         */
        public float getMinimumSpan(int axis) {
            float maxmin = 0;
            float min = 0;
            boolean nowrap = false;
            int n = getViewCount();
            for (int i = 0; i < n; i++) {
                View v = getView(i);
                if (v.getBreakWeight(axis, 0, Integer.MAX_VALUE) == BadBreakWeight) {
                    min += v.getPreferredSpan(axis);
                    nowrap = true;
                } else if (nowrap) {
                    maxmin = Math.max(min, maxmin);
                    nowrap = false;
                    min = 0;
                }
                if (v instanceof ComponentView) {
                    maxmin = Math.max(maxmin, v.getMinimumSpan(axis));
                }
            }
            maxmin = Math.max(maxmin, min);
            return maxmin;
        }

        /**
         * Forward the DocumentEvent to the given child view.  This
         * is implemented to reparent the child to the logical view
         * (the children may have been parented by a row in the flow
         * if they fit without breaking) and then execute the superclass
         * behavior.
         *
         * @param v the child view to forward the event to.
         * @param e the change information from the associated document
         * @param a the current allocation of the view
         * @param f the factory to use to rebuild if the view has children
         * @see #forwardUpdate
         * @since 1.3
         */
        protected void forwardUpdateToView(View v, DocumentEvent e,
                                           Shape a, ViewFactory f) {
            View parent = v.getParent();
            v.setParent(this);
            super.forwardUpdateToView(v, e, a, f);
            v.setParent(parent);
        }

        /** {@inheritDoc} */
        @Override
        protected void forwardUpdate(DocumentEvent.ElementChange ec,
                                          DocumentEvent e, Shape a, ViewFactory f) {
            // Update the view responsible for the changed element by invocation of
            // super method.
            super.forwardUpdate(ec, e, a, f);
            // Re-calculate the update indexes and update the views followed by
            // the changed place. Note: we update the views only when insertion or
            // removal takes place.
            DocumentEvent.EventType type = e.getType();
            if (type == DocumentEvent.EventType.INSERT ||
                type == DocumentEvent.EventType.REMOVE) {
                firstUpdateIndex = Math.min((lastUpdateIndex + 1), (getViewCount() - 1));
                lastUpdateIndex = Math.max((getViewCount() - 1), 0);
                for (int i = firstUpdateIndex; i <= lastUpdateIndex; i++) {
                    View v = getView(i);
                    if (v != null) {
                        v.updateAfterChange();
                    }
                }
            }
        }

        // The following methods don't do anything useful, they
        // simply keep the class from being abstract.

        /**
         * Renders using the given rendering surface and area on that
         * surface.  This is implemented to do nothing, the logical
         * view is never visible.
         *
         * @param g the rendering surface to use
         * @param allocation the allocated region to render into
         * @see View#paint
         */
        public void paint(Graphics g, Shape allocation) {
        }

        /**
         * Tests whether a point lies before the rectangle range.
         * Implemented to return false, as hit detection is not
         * performed on the logical view.
         *
         * @param x the X coordinate &gt;= 0
         * @param y the Y coordinate &gt;= 0
         * @param alloc the rectangle
         * @return true if the point is before the specified range
         */
        protected boolean isBefore(int x, int y, Rectangle alloc) {
            return false;
        }

        /**
         * Tests whether a point lies after the rectangle range.
         * Implemented to return false, as hit detection is not
         * performed on the logical view.
         *
         * @param x the X coordinate &gt;= 0
         * @param y the Y coordinate &gt;= 0
         * @param alloc the rectangle
         * @return true if the point is after the specified range
         */
        protected boolean isAfter(int x, int y, Rectangle alloc) {
            return false;
        }

        /**
         * Fetches the child view at the given point.
         * Implemented to return null, as hit detection is not
         * performed on the logical view.
         *
         * @param x the X coordinate &gt;= 0
         * @param y the Y coordinate &gt;= 0
         * @param alloc the parent's allocation on entry, which should
         *   be changed to the child's allocation on exit
         * @return the child view
         */
        protected View getViewAtPoint(int x, int y, Rectangle alloc) {
            return null;
        }

        /**
         * Returns the allocation for a given child.
         * Implemented to do nothing, as the logical view doesn't
         * perform layout on the children.
         *
         * @param index the index of the child, &gt;= 0 &amp;&amp; &lt; getViewCount()
         * @param a  the allocation to the interior of the box on entry,
         *   and the allocation of the child view at the index on exit.
         */
        protected void childAllocation(int index, Rectangle a) {
        }
    }


}
