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

import java.util.*;
import java.util.List;
import java.awt.*;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;

/**
 * A box that does layout asynchronously.  This
 * is useful to keep the GUI event thread moving by
 * not doing any layout on it.  The layout is done
 * on a granularity of operations on the child views.
 * After each child view is accessed for some part
 * of layout (a potentially time consuming operation)
 * the remaining tasks can be abandoned or a new higher
 * priority task (i.e. to service a synchronous request
 * or a visible area) can be taken on.
 * <p>
 * While the child view is being accessed
 * a read lock is acquired on the associated document
 * so that the model is stable while being accessed.
 *
 * @author  Timothy Prinzing
 * @since   1.3
 */
public class AsyncBoxView extends View {

    /**
     * Construct a box view that does asynchronous layout.
     *
     * @param elem the element of the model to represent
     * @param axis the axis to tile along.  This can be
     *  either X_AXIS or Y_AXIS.
     */
    public AsyncBoxView(Element elem, int axis) {
        super(elem);
        stats = new ArrayList<ChildState>();
        this.axis = axis;
        locator = new ChildLocator();
        flushTask = new FlushTask();
        minorSpan = Short.MAX_VALUE;
        estimatedMajorSpan = false;
    }

    /**
     * Fetch the major axis (the axis the children
     * are tiled along).  This will have a value of
     * either X_AXIS or Y_AXIS.
     * @return the major axis
     */
    public int getMajorAxis() {
        return axis;
    }

    /**
     * Fetch the minor axis (the axis orthogonal
     * to the tiled axis).  This will have a value of
     * either X_AXIS or Y_AXIS.
     * @return the minor axis
     */
    public int getMinorAxis() {
        return (axis == X_AXIS) ? Y_AXIS : X_AXIS;
    }

    /**
     * Get the top part of the margin around the view.
     * @return the top part of the margin around the view
     */
    public float getTopInset() {
        return topInset;
    }

    /**
     * Set the top part of the margin around the view.
     *
     * @param i the value of the inset
     */
    public void setTopInset(float i) {
        topInset = i;
    }

    /**
     * Get the bottom part of the margin around the view.
     * @return the bottom part of the margin around the view
     */
    public float getBottomInset() {
        return bottomInset;
    }

    /**
     * Set the bottom part of the margin around the view.
     *
     * @param i the value of the inset
     */
    public void setBottomInset(float i) {
        bottomInset = i;
    }

    /**
     * Get the left part of the margin around the view.
     * @return the left part of the margin around the view
     */
    public float getLeftInset() {
        return leftInset;
    }

    /**
     * Set the left part of the margin around the view.
     *
     * @param i the value of the inset
     */
    public void setLeftInset(float i) {
        leftInset = i;
    }

    /**
     * Get the right part of the margin around the view.
     * @return the right part of the margin around the view
     */
    public float getRightInset() {
        return rightInset;
    }

    /**
     * Set the right part of the margin around the view.
     *
     * @param i the value of the inset
     */
    public void setRightInset(float i) {
        rightInset = i;
    }

    /**
     * Fetch the span along an axis that is taken up by the insets.
     *
     * @param axis the axis to determine the total insets along,
     *  either X_AXIS or Y_AXIS.
     * @return the span along an axis that is taken up by the insets
     * @since 1.4
     */
    protected float getInsetSpan(int axis) {
        float margin = (axis == X_AXIS) ?
            getLeftInset() + getRightInset() : getTopInset() + getBottomInset();
        return margin;
    }

    /**
     * Set the estimatedMajorSpan property that determines if the
     * major span should be treated as being estimated.  If this
     * property is true, the value of setSize along the major axis
     * will change the requirements along the major axis and incremental
     * changes will be ignored until all of the children have been updated
     * (which will cause the property to automatically be set to false).
     * If the property is false the value of the majorSpan will be
     * considered to be accurate and incremental changes will be
     * added into the total as they are calculated.
     *
     * @param isEstimated new value for the estimatedMajorSpan property
     * @since 1.4
     */
    protected void setEstimatedMajorSpan(boolean isEstimated) {
        estimatedMajorSpan = isEstimated;
    }

    /**
     * Is the major span currently estimated?
     * @return whether or not the major span currently estimated
     *
     * @since 1.4
     */
    protected boolean getEstimatedMajorSpan() {
        return estimatedMajorSpan;
    }

    /**
     * Fetch the object representing the layout state of
     * of the child at the given index.
     *
     * @param index the child index.  This should be a
     *   value &gt;= 0 and &lt; getViewCount().
     * @return the object representing the layout state of
     * of the child at the given index
     */
    protected ChildState getChildState(int index) {
        synchronized(stats) {
            if ((index >= 0) && (index < stats.size())) {
                return stats.get(index);
            }
            return null;
        }
    }

    /**
     * Fetch the queue to use for layout.
     * @return the queue to use for layout
     */
    protected LayoutQueue getLayoutQueue() {
        return LayoutQueue.getDefaultQueue();
    }

    /**
     * New ChildState records are created through
     * this method to allow subclasses the extend
     * the ChildState records to do/hold more.
     * @param v the view
     * @return new child state
     */
    protected ChildState createChildState(View v) {
        return new ChildState(v);
    }

    /**
     * Requirements changed along the major axis.
     * This is called by the thread doing layout for
     * the given ChildState object when it has completed
     * fetching the child views new preferences.
     * Typically this would be the layout thread, but
     * might be the event thread if it is trying to update
     * something immediately (such as to perform a
     * model/view translation).
     * <p>
     * This is implemented to mark the major axis as having
     * changed so that a future check to see if the requirements
     * need to be published to the parent view will consider
     * the major axis.  If the span along the major axis is
     * not estimated, it is updated by the given delta to reflect
     * the incremental change.  The delta is ignored if the
     * major span is estimated.
     * @param cs the child state
     * @param delta the delta
     */
    protected synchronized void majorRequirementChange(ChildState cs, float delta) {
        if (estimatedMajorSpan == false) {
            majorSpan += delta;
        }
        majorChanged = true;
    }

    /**
     * Requirements changed along the minor axis.
     * This is called by the thread doing layout for
     * the given ChildState object when it has completed
     * fetching the child views new preferences.
     * Typically this would be the layout thread, but
     * might be the GUI thread if it is trying to update
     * something immediately (such as to perform a
     * model/view translation).
     * @param cs the child state
     */
    protected synchronized void minorRequirementChange(ChildState cs) {
        minorChanged = true;
    }

    /**
     * Publish the changes in preferences upward to the parent
     * view.  This is normally called by the layout thread.
     */
    protected void flushRequirementChanges() {
        AbstractDocument doc = (AbstractDocument) getDocument();
        try {
            doc.readLock();

            View parent = null;
            boolean horizontal = false;
            boolean vertical = false;

            synchronized(this) {
                // perform tasks that iterate over the children while
                // preventing the collection from changing.
                synchronized(stats) {
                    int n = getViewCount();
                    if ((n > 0) && (minorChanged || estimatedMajorSpan)) {
                        LayoutQueue q = getLayoutQueue();
                        ChildState min = getChildState(0);
                        ChildState pref = getChildState(0);
                        float span = 0f;
                        for (int i = 1; i < n; i++) {
                            ChildState cs = getChildState(i);
                            if (minorChanged) {
                                if (cs.min > min.min) {
                                    min = cs;
                                }
                                if (cs.pref > pref.pref) {
                                    pref = cs;
                                }
                            }
                            if (estimatedMajorSpan) {
                                span += cs.getMajorSpan();
                            }
                        }

                        if (minorChanged) {
                            minRequest = min;
                            prefRequest = pref;
                        }
                        if (estimatedMajorSpan) {
                            majorSpan = span;
                            estimatedMajorSpan = false;
                            majorChanged = true;
                        }
                    }
                }

                // message preferenceChanged
                if (majorChanged || minorChanged) {
                    parent = getParent();
                    if (parent != null) {
                        if (axis == X_AXIS) {
                            horizontal = majorChanged;
                            vertical = minorChanged;
                        } else {
                            vertical = majorChanged;
                            horizontal = minorChanged;
                        }
                    }
                    majorChanged = false;
                    minorChanged = false;
                }
            }

            // propagate a preferenceChanged, using the
            // layout thread.
            if (parent != null) {
                parent.preferenceChanged(this, horizontal, vertical);

                // probably want to change this to be more exact.
                Component c = getContainer();
                if (c != null) {
                    c.repaint();
                }
            }
        } finally {
            doc.readUnlock();
        }
    }

    /**
     * Calls the superclass to update the child views, and
     * updates the status records for the children.  This
     * is expected to be called while a write lock is held
     * on the model so that interaction with the layout
     * thread will not happen (i.e. the layout thread
     * acquires a read lock before doing anything).
     *
     * @param offset the starting offset into the child views &gt;= 0
     * @param length the number of existing views to replace &gt;= 0
     * @param views the child views to insert
     */
    public void replace(int offset, int length, View[] views) {
        synchronized(stats) {
            // remove the replaced state records
            for (int i = 0; i < length; i++) {
                ChildState cs = stats.remove(offset);
                float csSpan = cs.getMajorSpan();

                cs.getChildView().setParent(null);
                if (csSpan != 0) {
                    majorRequirementChange(cs, -csSpan);
                }
            }

            // insert the state records for the new children
            LayoutQueue q = getLayoutQueue();
            if (views != null) {
                for (int i = 0; i < views.length; i++) {
                    ChildState s = createChildState(views[i]);
                    stats.add(offset + i, s);
                    q.addTask(s);
                }
            }

            // notify that the size changed
            q.addTask(flushTask);
        }
    }

    /**
     * Loads all of the children to initialize the view.
     * This is called by the {@link #setParent setParent}
     * method.  Subclasses can reimplement this to initialize
     * their child views in a different manner.  The default
     * implementation creates a child view for each
     * child element.
     * <p>
     * Normally a write-lock is held on the Document while
     * the children are being changed, which keeps the rendering
     * and layout threads safe.  The exception to this is when
     * the view is initialized to represent an existing element
     * (via this method), so it is synchronized to exclude
     * preferenceChanged while we are initializing.
     *
     * @param f the view factory
     * @see #setParent
     */
    protected void loadChildren(ViewFactory f) {
        Element e = getElement();
        int n = e.getElementCount();
        if (n > 0) {
            View[] added = new View[n];
            for (int i = 0; i < n; i++) {
                added[i] = f.create(e.getElement(i));
            }
            replace(0, 0, added);
        }
    }

    /**
     * Fetches the child view index representing the given position in
     * the model.  This is implemented to fetch the view in the case
     * where there is a child view for each child element.
     *
     * @param pos the position &gt;= 0
     * @param b the position bias
     * @return  index of the view representing the given position, or
     *   -1 if no view represents that position
     */
    protected synchronized int getViewIndexAtPosition(int pos, Position.Bias b) {
        boolean isBackward = (b == Position.Bias.Backward);
        pos = (isBackward) ? Math.max(0, pos - 1) : pos;
        Element elem = getElement();
        return elem.getElementIndex(pos);
    }

    /**
     * Update the layout in response to receiving notification of
     * change from the model.  This is implemented to note the
     * change on the ChildLocator so that offsets of the children
     * will be correctly computed.
     *
     * @param ec changes to the element this view is responsible
     *  for (may be null if there were no changes).
     * @param e the change information from the associated document
     * @param a the current allocation of the view
     * @see #insertUpdate
     * @see #removeUpdate
     * @see #changedUpdate
     */
    protected void updateLayout(DocumentEvent.ElementChange ec,
                                    DocumentEvent e, Shape a) {
        if (ec != null) {
            // the newly inserted children don't have a valid
            // offset so the child locator needs to be messaged
            // that the child prior to the new children has
            // changed size.
            int index = Math.max(ec.getIndex() - 1, 0);
            ChildState cs = getChildState(index);
            locator.childChanged(cs);
        }
    }

    // --- View methods ------------------------------------

    /**
     * Sets the parent of the view.
     * This is reimplemented to provide the superclass
     * behavior as well as calling the <code>loadChildren</code>
     * method if this view does not already have children.
     * The children should not be loaded in the
     * constructor because the act of setting the parent
     * may cause them to try to search up the hierarchy
     * (to get the hosting Container for example).
     * If this view has children (the view is being moved
     * from one place in the view hierarchy to another),
     * the <code>loadChildren</code> method will not be called.
     *
     * @param parent the parent of the view, null if none
     */
    public void setParent(View parent) {
        super.setParent(parent);
        if ((parent != null) && (getViewCount() == 0)) {
            ViewFactory f = getViewFactory();
            loadChildren(f);
        }
    }

    /**
     * Child views can call this on the parent to indicate that
     * the preference has changed and should be reconsidered
     * for layout.  This is reimplemented to queue new work
     * on the layout thread.  This method gets messaged from
     * multiple threads via the children.
     *
     * @param child the child view
     * @param width true if the width preference has changed
     * @param height true if the height preference has changed
     * @see javax.swing.JComponent#revalidate
     */
    public synchronized void preferenceChanged(View child, boolean width, boolean height) {
        if (child == null) {
            getParent().preferenceChanged(this, width, height);
        } else {
            if (changing != null) {
                View cv = changing.getChildView();
                if (cv == child) {
                    // size was being changed on the child, no need to
                    // queue work for it.
                    changing.preferenceChanged(width, height);
                    return;
                }
            }
            int index = getViewIndex(child.getStartOffset(),
                                     Position.Bias.Forward);
            ChildState cs = getChildState(index);
            cs.preferenceChanged(width, height);
            LayoutQueue q = getLayoutQueue();
            q.addTask(cs);
            q.addTask(flushTask);
        }
    }

    /**
     * Sets the size of the view.  This should cause
     * layout of the view if the view caches any layout
     * information.
     * <p>
     * Since the major axis is updated asynchronously and should be
     * the sum of the tiled children the call is ignored for the major
     * axis.  Since the minor axis is flexible, work is queued to resize
     * the children if the minor span changes.
     *
     * @param width the width &gt;= 0
     * @param height the height &gt;= 0
     */
    public void setSize(float width, float height) {
        setSpanOnAxis(X_AXIS, width);
        setSpanOnAxis(Y_AXIS, height);
    }

    /**
     * Retrieves the size of the view along an axis.
     *
     * @param axis may be either <code>View.X_AXIS</code> or
     *          <code>View.Y_AXIS</code>
     * @return the current span of the view along the given axis, >= 0
     */
    float getSpanOnAxis(int axis) {
        if (axis == getMajorAxis()) {
            return majorSpan;
        }
        return minorSpan;
    }

    /**
     * Sets the size of the view along an axis.  Since the major
     * axis is updated asynchronously and should be the sum of the
     * tiled children the call is ignored for the major axis.  Since
     * the minor axis is flexible, work is queued to resize the
     * children if the minor span changes.
     *
     * @param axis may be either <code>View.X_AXIS</code> or
     *          <code>View.Y_AXIS</code>
     * @param span the span to layout to >= 0
     */
    void setSpanOnAxis(int axis, float span) {
        float margin = getInsetSpan(axis);
        if (axis == getMinorAxis()) {
            float targetSpan = span - margin;
            if (targetSpan != minorSpan) {
                minorSpan = targetSpan;

                // mark all of the ChildState instances as needing to
                // resize the child, and queue up work to fix them.
                int n = getViewCount();
                if (n != 0) {
                    LayoutQueue q = getLayoutQueue();
                    for (int i = 0; i < n; i++) {
                        ChildState cs = getChildState(i);
                        cs.childSizeValid = false;
                        q.addTask(cs);
                    }
                    q.addTask(flushTask);
                }
            }
        } else {
            // along the major axis the value is ignored
            // unless the estimatedMajorSpan property is
            // true.
            if (estimatedMajorSpan) {
                majorSpan = span - margin;
            }
        }
    }

    /**
     * Render the view using the given allocation and
     * rendering surface.
     * <p>
     * This is implemented to determine whether or not the
     * desired region to be rendered (i.e. the unclipped
     * area) is up to date or not.  If up-to-date the children
     * are rendered.  If not up-to-date, a task to build
     * the desired area is placed on the layout queue as
     * a high priority task.  This keeps by event thread
     * moving by rendering if ready, and postponing until
     * a later time if not ready (since paint requests
     * can be rescheduled).
     *
     * @param g the rendering surface to use
     * @param alloc the allocated region to render into
     * @see View#paint
     */
    public void paint(Graphics g, Shape alloc) {
        synchronized (locator) {
            locator.setAllocation(alloc);
            locator.paintChildren(g);
        }
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
     * @exception IllegalArgumentException for an invalid axis type
     */
    public float getPreferredSpan(int axis) {
        float margin = getInsetSpan(axis);
        if (axis == this.axis) {
            return majorSpan + margin;
        }
        if (prefRequest != null) {
            View child = prefRequest.getChildView();
            return child.getPreferredSpan(axis) + margin;
        }

        // nothing is known about the children yet
        return margin + 30;
    }

    /**
     * Determines the minimum span for this view along an
     * axis.
     *
     * @param axis may be either View.X_AXIS or View.Y_AXIS
     * @return  the span the view would like to be rendered into &gt;= 0.
     *           Typically the view is told to render into the span
     *           that is returned, although there is no guarantee.
     *           The parent may choose to resize or break the view.
     * @exception IllegalArgumentException for an invalid axis type
     */
    public float getMinimumSpan(int axis) {
        if (axis == this.axis) {
            return getPreferredSpan(axis);
        }
        if (minRequest != null) {
            View child = minRequest.getChildView();
            return child.getMinimumSpan(axis);
        }

        // nothing is known about the children yet
        if (axis == X_AXIS) {
            return getLeftInset() + getRightInset() + 5;
        } else {
            return getTopInset() + getBottomInset() + 5;
        }
    }

    /**
     * Determines the maximum span for this view along an
     * axis.
     *
     * @param axis may be either View.X_AXIS or View.Y_AXIS
     * @return   the span the view would like to be rendered into &gt;= 0.
     *           Typically the view is told to render into the span
     *           that is returned, although there is no guarantee.
     *           The parent may choose to resize or break the view.
     * @exception IllegalArgumentException for an invalid axis type
     */
    public float getMaximumSpan(int axis) {
        if (axis == this.axis) {
            return getPreferredSpan(axis);
        }
        return Integer.MAX_VALUE;
    }


    /**
     * Returns the number of views in this view.  Since
     * the default is to not be a composite view this
     * returns 0.
     *
     * @return the number of views &gt;= 0
     * @see View#getViewCount
     */
    public int getViewCount() {
        synchronized(stats) {
            return stats.size();
        }
    }

    /**
     * Gets the nth child view.  Since there are no
     * children by default, this returns null.
     *
     * @param n the number of the view to get, &gt;= 0 &amp;&amp; &lt; getViewCount()
     * @return the view
     */
    public View getView(int n) {
        ChildState cs = getChildState(n);
        if (cs != null) {
            return cs.getChildView();
        }
        return null;
    }

    /**
     * Fetches the allocation for the given child view.
     * This enables finding out where various views
     * are located, without assuming the views store
     * their location.  This returns null since the
     * default is to not have any child views.
     *
     * @param index the index of the child, &gt;= 0 &amp;&amp; &lt; getViewCount()
     * @param a  the allocation to this view.
     * @return the allocation to the child
     */
    public Shape getChildAllocation(int index, Shape a) {
        Shape ca = locator.getChildAllocation(index, a);
        return ca;
    }

    /**
     * Returns the child view index representing the given position in
     * the model.  By default a view has no children so this is implemented
     * to return -1 to indicate there is no valid child index for any
     * position.
     *
     * @param pos the position &gt;= 0
     * @return  index of the view representing the given position, or
     *   -1 if no view represents that position
     * @since 1.3
     */
    public int getViewIndex(int pos, Position.Bias b) {
        return getViewIndexAtPosition(pos, b);
    }

    /**
     * Provides a mapping from the document model coordinate space
     * to the coordinate space of the view mapped to it.
     *
     * @param pos the position to convert &gt;= 0
     * @param a the allocated region to render into
     * @param b the bias toward the previous character or the
     *  next character represented by the offset, in case the
     *  position is a boundary of two views.
     * @return the bounding box of the given position is returned
     * @exception BadLocationException  if the given position does
     *   not represent a valid location in the associated document
     * @exception IllegalArgumentException for an invalid bias argument
     * @see View#viewToModel
     */
    public Shape modelToView(int pos, Shape a, Position.Bias b) throws BadLocationException {
        int index = getViewIndex(pos, b);
        Shape ca = locator.getChildAllocation(index, a);

        // forward to the child view, and make sure we don't
        // interact with the layout thread by synchronizing
        // on the child state.
        ChildState cs = getChildState(index);
        synchronized (cs) {
            View cv = cs.getChildView();
            Shape v = cv.modelToView(pos, ca, b);
            return v;
        }
    }

    /**
     * Provides a mapping from the view coordinate space to the logical
     * coordinate space of the model.  The biasReturn argument will be
     * filled in to indicate that the point given is closer to the next
     * character in the model or the previous character in the model.
     * <p>
     * This is expected to be called by the GUI thread, holding a
     * read-lock on the associated model.  It is implemented to
     * locate the child view and determine it's allocation with a
     * lock on the ChildLocator object, and to call viewToModel
     * on the child view with a lock on the ChildState object
     * to avoid interaction with the layout thread.
     *
     * @param x the X coordinate &gt;= 0
     * @param y the Y coordinate &gt;= 0
     * @param a the allocated region to render into
     * @return the location within the model that best represents the
     *  given point in the view &gt;= 0.  The biasReturn argument will be
     * filled in to indicate that the point given is closer to the next
     * character in the model or the previous character in the model.
     */
    public int viewToModel(float x, float y, Shape a, Position.Bias[] biasReturn) {
        int pos;    // return position
        int index;  // child index to forward to
        Shape ca;   // child allocation

        // locate the child view and it's allocation so that
        // we can forward to it.  Make sure the layout thread
        // doesn't change anything by trying to flush changes
        // to the parent while the GUI thread is trying to
        // find the child and it's allocation.
        synchronized (locator) {
            index = locator.getViewIndexAtPoint(x, y, a);
            ca = locator.getChildAllocation(index, a);
        }

        // forward to the child view, and make sure we don't
        // interact with the layout thread by synchronizing
        // on the child state.
        ChildState cs = getChildState(index);
        synchronized (cs) {
            View v = cs.getChildView();
            pos = v.viewToModel(x, y, ca, biasReturn);
        }
        return pos;
    }

    /**
     * Provides a way to determine the next visually represented model
     * location that one might place a caret.  Some views may not be visible,
     * they might not be in the same order found in the model, or they just
     * might not allow access to some of the locations in the model.
     * This method enables specifying a position to convert
     * within the range of &gt;=0.  If the value is -1, a position
     * will be calculated automatically.  If the value &lt; -1,
     * the {@code BadLocationException} will be thrown.
     *
     * @param pos the position to convert
     * @param a the allocated region to render into
     * @param direction the direction from the current position that can
     *  be thought of as the arrow keys typically found on a keyboard;
     *  this may be one of the following:
     *  <ul style="list-style-type:none">
     *  <li><code>SwingConstants.WEST</code></li>
     *  <li><code>SwingConstants.EAST</code></li>
     *  <li><code>SwingConstants.NORTH</code></li>
     *  <li><code>SwingConstants.SOUTH</code></li>
     *  </ul>
     * @param biasRet an array contain the bias that was checked
     * @return the location within the model that best represents the next
     *  location visual position
     * @exception BadLocationException the given position is not a valid
     *                                 position within the document
     * @exception IllegalArgumentException if <code>direction</code> is invalid
     */
    public int getNextVisualPositionFrom(int pos, Position.Bias b, Shape a,
                                         int direction,
                                         Position.Bias[] biasRet)
                                                  throws BadLocationException {
        if (pos < -1 || pos > getDocument().getLength()) {
            throw new BadLocationException("invalid position", pos);
        }
        return Utilities.getNextVisualPositionFrom(
                            this, pos, b, a, direction, biasRet);
    }

    // --- variables -----------------------------------------

    /**
     * The major axis against which the children are
     * tiled.
     */
    int axis;

    /**
     * The children and their layout statistics.
     */
    List<ChildState> stats;

    /**
     * Current span along the major axis.  This
     * is also the value returned by getMinimumSize,
     * getPreferredSize, and getMaximumSize along
     * the major axis.
     */
    float majorSpan;

    /**
     * Is the span along the major axis estimated?
     */
    boolean estimatedMajorSpan;

    /**
     * Current span along the minor axis.  This
     * is what layout was done against (i.e. things
     * are flexible in this direction).
     */
    float minorSpan;

    /**
     * Object that manages the offsets of the
     * children.  All locking for management of
     * child locations is on this object.
     */
    protected ChildLocator locator;

    float topInset;
    float bottomInset;
    float leftInset;
    float rightInset;

    ChildState minRequest;
    ChildState prefRequest;
    boolean majorChanged;
    boolean minorChanged;
    Runnable flushTask;

    /**
     * Child that is actively changing size.  This often
     * causes a preferenceChanged, so this is a cache to
     * possibly speed up the marking the state.  It also
     * helps flag an opportunity to avoid adding to flush
     * task to the layout queue.
     */
    ChildState changing;

    /**
     * A class to manage the effective position of the
     * child views in a localized area while changes are
     * being made around the localized area.  The AsyncBoxView
     * may be continuously changing, but the visible area
     * needs to remain fairly stable until the layout thread
     * decides to publish an update to the parent.
     * @since 1.3
     */
    public class ChildLocator {

        /**
         * construct a child locator.
         */
        public ChildLocator() {
            lastAlloc = new Rectangle();
            childAlloc = new Rectangle();
        }

        /**
         * Notification that a child changed.  This can effect
         * whether or not new offset calculations are needed.
         * This is called by a ChildState object that has
         * changed it's major span.  This can therefore be
         * called by multiple threads.
         * @param cs the child state
         */
        public synchronized void childChanged(ChildState cs) {
            if (lastValidOffset == null) {
                lastValidOffset = cs;
            } else if (cs.getChildView().getStartOffset() <
                       lastValidOffset.getChildView().getStartOffset()) {
                lastValidOffset = cs;
            }
        }

        /**
         * Paint the children that intersect the clip area.
         * @param g the rendering surface to use
         */
        public synchronized void paintChildren(Graphics g) {
            Rectangle clip = g.getClipBounds();
            float targetOffset = (axis == X_AXIS) ?
                clip.x - lastAlloc.x : clip.y - lastAlloc.y;
            int index = getViewIndexAtVisualOffset(targetOffset);
            int n = getViewCount();
            float offs = getChildState(index).getMajorOffset();
            for (int i = index; i < n; i++) {
                ChildState cs = getChildState(i);
                cs.setMajorOffset(offs);
                Shape ca = getChildAllocation(i);
                if (intersectsClip(ca, clip)) {
                    synchronized (cs) {
                        View v = cs.getChildView();
                        v.paint(g, ca);
                    }
                } else {
                    // done painting intersection
                    break;
                }
                offs += cs.getMajorSpan();
            }
        }

        /**
         * Fetch the allocation to use for a child view.
         * This will update the offsets for all children
         * not yet updated before the given index.
         * @param index the child index
         * @param a the allocation
         * @return the allocation to use for a child view
         */
        public synchronized Shape getChildAllocation(int index, Shape a) {
            if (a == null) {
                return null;
            }
            setAllocation(a);
            ChildState cs = getChildState(index);
            if (lastValidOffset == null) {
                lastValidOffset = getChildState(0);
            }
            if (cs.getChildView().getStartOffset() >
                lastValidOffset.getChildView().getStartOffset()) {
                // offsets need to be updated
                updateChildOffsetsToIndex(index);
            }
            Shape ca = getChildAllocation(index);
            return ca;
        }

        /**
         * Fetches the child view index at the given point.
         * This is called by the various View methods that
         * need to calculate which child to forward a message
         * to.  This should be called by a block synchronized
         * on this object, and would typically be followed
         * with one or more calls to getChildAllocation that
         * should also be in the synchronized block.
         *
         * @param x the X coordinate &gt;= 0
         * @param y the Y coordinate &gt;= 0
         * @param a the allocation to the View
         * @return the nearest child index
         */
        public int getViewIndexAtPoint(float x, float y, Shape a) {
            setAllocation(a);
            float targetOffset = (axis == X_AXIS) ? x - lastAlloc.x : y - lastAlloc.y;
            int index = getViewIndexAtVisualOffset(targetOffset);
            return index;
        }

        /**
         * Fetch the allocation to use for a child view.
         * <em>This does not update the offsets in the ChildState
         * records.</em>
         * @param index the index
         * @return the allocation to use for a child view
         */
        protected Shape getChildAllocation(int index) {
            ChildState cs = getChildState(index);
            if (! cs.isLayoutValid()) {
                cs.run();
            }
            if (axis == X_AXIS) {
                childAlloc.x = lastAlloc.x + (int) cs.getMajorOffset();
                childAlloc.y = lastAlloc.y + (int) cs.getMinorOffset();
                childAlloc.width = (int) cs.getMajorSpan();
                childAlloc.height = (int) cs.getMinorSpan();
            } else {
                childAlloc.y = lastAlloc.y + (int) cs.getMajorOffset();
                childAlloc.x = lastAlloc.x + (int) cs.getMinorOffset();
                childAlloc.height = (int) cs.getMajorSpan();
                childAlloc.width = (int) cs.getMinorSpan();
            }
            childAlloc.x += (int)getLeftInset();
            childAlloc.y += (int)getRightInset();
            return childAlloc;
        }

        /**
         * Copy the currently allocated shape into the Rectangle
         * used to store the current allocation.  This would be
         * a floating point rectangle in a Java2D-specific implementation.
         * @param a the allocation
         */
        protected void setAllocation(Shape a) {
            if (a instanceof Rectangle) {
                lastAlloc.setBounds((Rectangle) a);
            } else {
                lastAlloc.setBounds(a.getBounds());
            }
            setSize(lastAlloc.width, lastAlloc.height);
        }

        /**
         * Locate the view responsible for an offset into the box
         * along the major axis.  Make sure that offsets are set
         * on the ChildState objects up to the given target span
         * past the desired offset.
         * @param targetOffset the target offset
         *
         * @return   index of the view representing the given visual
         *   location (targetOffset), or -1 if no view represents
         *   that location
         */
        protected int getViewIndexAtVisualOffset(float targetOffset) {
            int n = getViewCount();
            if (n > 0) {
                boolean lastValid = (lastValidOffset != null);

                if (lastValidOffset == null) {
                    lastValidOffset = getChildState(0);
                }
                if (targetOffset > majorSpan) {
                    // should only get here on the first time display.
                    if (!lastValid) {
                        return 0;
                    }
                    int pos = lastValidOffset.getChildView().getStartOffset();
                    int index = getViewIndex(pos, Position.Bias.Forward);
                    return index;
                } else if (targetOffset > lastValidOffset.getMajorOffset()) {
                    // roll offset calculations forward
                    return updateChildOffsets(targetOffset);
                } else {
                    // no changes prior to the needed offset
                    // this should be a binary search
                    float offs = 0f;
                    for (int i = 0; i < n; i++) {
                        ChildState cs = getChildState(i);
                        float nextOffs = offs + cs.getMajorSpan();
                        if (targetOffset < nextOffs) {
                            return i;
                        }
                        offs = nextOffs;
                    }
                }
            }
            return n - 1;
        }

        /**
         * Move the location of the last offset calculation forward
         * to the desired offset.
         */
        int updateChildOffsets(float targetOffset) {
            int n = getViewCount();
            int targetIndex = n - 1;
            int pos = lastValidOffset.getChildView().getStartOffset();
            int startIndex = getViewIndex(pos, Position.Bias.Forward);
            float start = lastValidOffset.getMajorOffset();
            float lastOffset = start;
            for (int i = startIndex; i < n; i++) {
                ChildState cs = getChildState(i);
                cs.setMajorOffset(lastOffset);
                lastOffset += cs.getMajorSpan();
                if (targetOffset < lastOffset) {
                    targetIndex = i;
                    lastValidOffset = cs;
                    break;
                }
            }

            return targetIndex;
        }

        /**
         * Move the location of the last offset calculation forward
         * to the desired index.
         */
        void updateChildOffsetsToIndex(int index) {
            int pos = lastValidOffset.getChildView().getStartOffset();
            int startIndex = getViewIndex(pos, Position.Bias.Forward);
            float lastOffset = lastValidOffset.getMajorOffset();
            for (int i = startIndex; i <= index; i++) {
                ChildState cs = getChildState(i);
                cs.setMajorOffset(lastOffset);
                lastOffset += cs.getMajorSpan();
            }
        }

        boolean intersectsClip(Shape childAlloc, Rectangle clip) {
            Rectangle cs = (childAlloc instanceof Rectangle) ?
                (Rectangle) childAlloc : childAlloc.getBounds();
            if (cs.intersects(clip)) {
                // Make sure that lastAlloc also contains childAlloc,
                // this will be false if haven't yet flushed changes.
                return lastAlloc.intersects(cs);
            }
            return false;
        }

        /**
         * The location of the last offset calculation
         * that is valid.
         */
        protected ChildState lastValidOffset;

        /**
         * The last seen allocation (for repainting when changes
         * are flushed upward).
         */
        protected Rectangle lastAlloc;

        /**
         * A shape to use for the child allocation to avoid
         * creating a lot of garbage.
         */
        protected Rectangle childAlloc;
    }

    /**
     * A record representing the layout state of a
     * child view.  It is runnable as a task on another
     * thread.  All access to the child view that is
     * based upon a read-lock on the model should synchronize
     * on this object (i.e. The layout thread and the GUI
     * thread can both have a read lock on the model at the
     * same time and are not protected from each other).
     * Access to a child view hierarchy is serialized via
     * synchronization on the ChildState instance.
     * @since 1.3
     */
    public class ChildState implements Runnable {

        /**
         * Construct a child status.  This needs to start
         * out as fairly large so we don't falsely begin with
         * the idea that all of the children are visible.
         * @param v the view
         * @since 1.4
         */
        public ChildState(View v) {
            child = v;
            minorValid = false;
            majorValid = false;
            childSizeValid = false;
            child.setParent(AsyncBoxView.this);
        }

        /**
         * Fetch the child view this record represents.
         * @return the child view this record represents
         */
        public View getChildView() {
            return child;
        }

        /**
         * Update the child state.  This should be
         * called by the thread that desires to spend
         * time updating the child state (intended to
         * be the layout thread).
         * <p>
         * This acquires a read lock on the associated
         * document for the duration of the update to
         * ensure the model is not changed while it is
         * operating.  The first thing to do would be
         * to see if any work actually needs to be done.
         * The following could have conceivably happened
         * while the state was waiting to be updated:
         * <ol>
         * <li>The child may have been removed from the
         * view hierarchy.
         * <li>The child may have been updated by a
         * higher priority operation (i.e. the child
         * may have become visible).
         * </ol>
         */
        public void run () {
            AbstractDocument doc = (AbstractDocument) getDocument();
            try {
                doc.readLock();
                if (minorValid && majorValid && childSizeValid) {
                    // nothing to do
                    return;
                }
                if (child.getParent() == AsyncBoxView.this) {
                    // this may overwrite anothers threads cached
                    // value for actively changing... but that just
                    // means it won't use the cache if there is an
                    // overwrite.
                    synchronized(AsyncBoxView.this) {
                        changing = this;
                    }
                    updateChild();
                    synchronized(AsyncBoxView.this) {
                        changing = null;
                    }

                    // setting the child size on the minor axis
                    // may have caused it to change it's preference
                    // along the major axis.
                    updateChild();
                }
            } finally {
                doc.readUnlock();
            }
        }

        void updateChild() {
            boolean minorUpdated = false;
            synchronized(this) {
                if (! minorValid) {
                    int minorAxis = getMinorAxis();
                    min = child.getMinimumSpan(minorAxis);
                    pref = child.getPreferredSpan(minorAxis);
                    max = child.getMaximumSpan(minorAxis);
                    minorValid = true;
                    minorUpdated = true;
                }
            }
            if (minorUpdated) {
                minorRequirementChange(this);
            }

            boolean majorUpdated = false;
            float delta = 0.0f;
            synchronized(this) {
                if (! majorValid) {
                    float old = span;
                    span = child.getPreferredSpan(axis);
                    delta = span - old;
                    majorValid = true;
                    majorUpdated = true;
                }
            }
            if (majorUpdated) {
                majorRequirementChange(this, delta);
                locator.childChanged(this);
            }

            synchronized(this) {
                if (! childSizeValid) {
                    float w;
                    float h;
                    if (axis == X_AXIS) {
                        w = span;
                        h = getMinorSpan();
                    } else {
                        w = getMinorSpan();
                        h = span;
                    }
                    childSizeValid = true;
                    child.setSize(w, h);
                }
            }

        }

        /**
         * What is the span along the minor axis.
         * @return the span along the minor axis
         */
        public float getMinorSpan() {
            if (max < minorSpan) {
                return max;
            }
            // make it the target width, or as small as it can get.
            return Math.max(min, minorSpan);
        }

        /**
         * What is the offset along the minor axis
         * @return the offset along the minor axis
         */
        public float getMinorOffset() {
            if (max < minorSpan) {
                // can't make the child this wide, align it
                float align = child.getAlignment(getMinorAxis());
                return ((minorSpan - max) * align);
            }
            return 0f;
        }

        /**
         * What is the span along the major axis.
         * @return the span along the major axis
         */
        public float getMajorSpan() {
            return span;
        }

        /**
         * Get the offset along the major axis.
         * @return the offset along the major axis
         */
        public float getMajorOffset() {
            return offset;
        }

        /**
         * This method should only be called by the ChildLocator,
         * it is simply a convenient place to hold the cached
         * location.
         * @param offs offsets
         */
        public void setMajorOffset(float offs) {
            offset = offs;
        }

        /**
         * Mark preferences changed for this child.
         *
         * @param width true if the width preference has changed
         * @param height true if the height preference has changed
         * @see javax.swing.JComponent#revalidate
         */
        public void preferenceChanged(boolean width, boolean height) {
            if (axis == X_AXIS) {
                if (width) {
                    majorValid = false;
                }
                if (height) {
                    minorValid = false;
                }
            } else {
                if (width) {
                    minorValid = false;
                }
                if (height) {
                    majorValid = false;
                }
            }
            childSizeValid = false;
        }

        /**
         * Has the child view been laid out.
         * @return whether or not the child view been laid out.
         */
        public boolean isLayoutValid() {
            return (minorValid && majorValid && childSizeValid);
        }

        // minor axis
        private float min;
        private float pref;
        private float max;
        private boolean minorValid;

        // major axis
        private float span;
        private float offset;
        private boolean majorValid;

        private View child;
        private boolean childSizeValid;
    }

    /**
     * Task to flush requirement changes upward
     */
    class FlushTask implements Runnable {

        public void run() {
            flushRequirementChanges();
        }

    }

}
