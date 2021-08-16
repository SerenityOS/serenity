/*
 * Copyright (c) 1997, 2016, Oracle and/or its affiliates. All rights reserved.
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
import javax.swing.event.*;
import javax.swing.SwingConstants;

/**
 * <code>CompositeView</code> is an abstract <code>View</code>
 * implementation which manages one or more child views.
 * (Note that <code>CompositeView</code> is intended
 * for managing relatively small numbers of child views.)
 * <code>CompositeView</code> is intended to be used as
 * a starting point for <code>View</code> implementations,
 * such as <code>BoxView</code>, that will contain child
 * <code>View</code>s. Subclasses that wish to manage the
 * collection of child <code>View</code>s should use the
 * {@link #replace} method.  As <code>View</code> invokes
 * <code>replace</code> during <code>DocumentListener</code>
 * notification, you normally won't need to directly
 * invoke <code>replace</code>.
 *
 * <p>While <code>CompositeView</code>
 * does not impose a layout policy on its child <code>View</code>s,
 * it does allow for inseting the child <code>View</code>s
 * it will contain.  The insets can be set by either
 * {@link #setInsets} or {@link #setParagraphInsets}.
 *
 * <p>In addition to the abstract methods of
 * {@link javax.swing.text.View},
 * subclasses of <code>CompositeView</code> will need to
 * override:
 * <ul>
 * <li>{@link #isBefore} - Used to test if a given
 *     <code>View</code> location is before the visual space
 *     of the <code>CompositeView</code>.
 * <li>{@link #isAfter} - Used to test if a given
 *     <code>View</code> location is after the visual space
 *     of the <code>CompositeView</code>.
 * <li>{@link #getViewAtPoint} - Returns the view at
 *     a given visual location.
 * <li>{@link #childAllocation} - Returns the bounds of
 *     a particular child <code>View</code>.
 *     <code>getChildAllocation</code> will invoke
 *     <code>childAllocation</code> after offseting
 *     the bounds by the <code>Inset</code>s of the
 *     <code>CompositeView</code>.
 * </ul>
 *
 * @author  Timothy Prinzing
 */
public abstract class CompositeView extends View {

    /**
     * Constructs a <code>CompositeView</code> for the given element.
     *
     * @param elem  the element this view is responsible for
     */
    public CompositeView(Element elem) {
        super(elem);
        children = new View[1];
        nchildren = 0;
        childAlloc = new Rectangle();
    }

    /**
     * Loads all of the children to initialize the view.
     * This is called by the {@link #setParent}
     * method.  Subclasses can reimplement this to initialize
     * their child views in a different manner.  The default
     * implementation creates a child view for each
     * child element.
     *
     * @param f the view factory
     * @see #setParent
     */
    protected void loadChildren(ViewFactory f) {
        if (f == null) {
            // No factory. This most likely indicates the parent view
            // has changed out from under us, bail!
            return;
        }
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

    // --- View methods ---------------------------------------------

    /**
     * Sets the parent of the view.
     * This is reimplemented to provide the superclass
     * behavior as well as calling the <code>loadChildren</code>
     * method if this view does not already have children.
     * The children should not be loaded in the
     * constructor because the act of setting the parent
     * may cause them to try to search up the hierarchy
     * (to get the hosting <code>Container</code> for example).
     * If this view has children (the view is being moved
     * from one place in the view hierarchy to another),
     * the <code>loadChildren</code> method will not be called.
     *
     * @param parent the parent of the view, <code>null</code> if none
     */
    public void setParent(View parent) {
        super.setParent(parent);
        if ((parent != null) && (nchildren == 0)) {
            ViewFactory f = getViewFactory();
            loadChildren(f);
        }
    }

    /**
     * Returns the number of child views of this view.
     *
     * @return the number of views &gt;= 0
     * @see #getView
     */
    public int getViewCount() {
        return nchildren;
    }

    /**
     * Returns the n-th view in this container.
     *
     * @param n the number of the desired view, &gt;= 0 &amp;&amp; &lt; getViewCount()
     * @return the view at index <code>n</code>
     */
    public View getView(int n) {
        return children[n];
    }

    /**
     * Replaces child views.  If there are no views to remove
     * this acts as an insert.  If there are no views to
     * add this acts as a remove.  Views being removed will
     * have the parent set to <code>null</code>,
     * and the internal reference to them removed so that they
     * may be garbage collected.
     *
     * @param offset the starting index into the child views to insert
     *   the new views; &gt;= 0 and &lt;= getViewCount
     * @param length the number of existing child views to remove;
     *   this should be a value &gt;= 0 and &lt;= (getViewCount() - offset)
     * @param views the child views to add; this value can be
     *  <code>null</code>
     *   to indicate no children are being added (useful to remove)
     */
    public void replace(int offset, int length, View[] views) {
        // make sure an array exists
        if (views == null) {
            views = ZERO;
        }

        Set<View> set = new HashSet<>(Arrays.asList(views));
        // update parent reference on removed views
        for (int i = offset; i < offset + length; i++) {
            View child = children[i];
            if (child.getParent() == this && !set.contains(child)) {
                // in FlowView.java view might be referenced
                // from two super-views as a child. see logicalView
                children[i].setParent(null);
            }
            children[i] = null;
        }

        // update the array
        int delta = views.length - length;
        int src = offset + length;
        int nmove = nchildren - src;
        int dest = src + delta;
        if ((nchildren + delta) >= children.length) {
            // need to grow the array
            int newLength = Math.max(2*children.length, nchildren + delta);
            View[] newChildren = new View[newLength];
            System.arraycopy(children, 0, newChildren, 0, offset);
            System.arraycopy(views, 0, newChildren, offset, views.length);
            System.arraycopy(children, src, newChildren, dest, nmove);
            children = newChildren;
        } else {
            // patch the existing array
            System.arraycopy(children, src, children, dest, nmove);
            System.arraycopy(views, 0, children, offset, views.length);
        }
        nchildren = nchildren + delta;

        // update parent reference on added views
        for (int i = 0; i < views.length; i++) {
            views[i].setParent(this);
        }
    }

    /**
     * Fetches the allocation for the given child view to
     * render into. This enables finding out where various views
     * are located.
     *
     * @param index the index of the child, &gt;= 0 &amp;&amp; &lt; getViewCount()
     * @param a  the allocation to this view
     * @return the allocation to the child
     */
    public Shape getChildAllocation(int index, Shape a) {
        Rectangle alloc = getInsideAllocation(a);
        childAllocation(index, alloc);
        return alloc;
    }

    /**
     * Provides a mapping from the document model coordinate space
     * to the coordinate space of the view mapped to it.
     *
     * @param pos the position to convert &gt;= 0
     * @param a the allocated region to render into
     * @param b a bias value of either <code>Position.Bias.Forward</code>
     *  or <code>Position.Bias.Backward</code>
     * @return the bounding box of the given position
     * @exception BadLocationException  if the given position does
     *   not represent a valid location in the associated document
     * @see View#modelToView
     */
    public Shape modelToView(int pos, Shape a, Position.Bias b) throws BadLocationException {
        boolean isBackward = (b == Position.Bias.Backward);
        int testPos = (isBackward) ? Math.max(0, pos - 1) : pos;
        if(isBackward && testPos < getStartOffset()) {
            return null;
        }
        int vIndex = getViewIndexAtPosition(testPos);
        if ((vIndex != -1) && (vIndex < getViewCount())) {
            View v = getView(vIndex);
            if(v != null && testPos >= v.getStartOffset() &&
               testPos < v.getEndOffset()) {
                Shape childShape = getChildAllocation(vIndex, a);
                if (childShape == null) {
                    // We are likely invalid, fail.
                    return null;
                }
                Shape retShape = v.modelToView(pos, childShape, b);
                if(retShape == null && v.getEndOffset() == pos) {
                    if(++vIndex < getViewCount()) {
                        v = getView(vIndex);
                        retShape = v.modelToView(pos, getChildAllocation(vIndex, a), b);
                    }
                }
                return retShape;
            }
        }
        throw new BadLocationException("Position not represented by view",
                                       pos);
    }

    /**
     * Provides a mapping from the document model coordinate space
     * to the coordinate space of the view mapped to it.
     *
     * @param p0 the position to convert &gt;= 0
     * @param b0 the bias toward the previous character or the
     *  next character represented by p0, in case the
     *  position is a boundary of two views; either
     *  <code>Position.Bias.Forward</code> or
     *  <code>Position.Bias.Backward</code>
     * @param p1 the position to convert &gt;= 0
     * @param b1 the bias toward the previous character or the
     *  next character represented by p1, in case the
     *  position is a boundary of two views
     * @param a the allocated region to render into
     * @return the bounding box of the given position is returned
     * @exception BadLocationException  if the given position does
     *   not represent a valid location in the associated document
     * @exception IllegalArgumentException for an invalid bias argument
     * @see View#viewToModel
     */
    public Shape modelToView(int p0, Position.Bias b0, int p1, Position.Bias b1, Shape a) throws BadLocationException {
        if (p0 == getStartOffset() && p1 == getEndOffset()) {
            return a;
        }
        Rectangle alloc = getInsideAllocation(a);
        Rectangle r0 = new Rectangle(alloc);
        View v0 = getViewAtPosition((b0 == Position.Bias.Backward) ?
                                    Math.max(0, p0 - 1) : p0, r0);
        Rectangle r1 = new Rectangle(alloc);
        View v1 = getViewAtPosition((b1 == Position.Bias.Backward) ?
                                    Math.max(0, p1 - 1) : p1, r1);
        if (v0 == v1) {
            if (v0 == null) {
                return a;
            }
            // Range contained in one view
            return v0.modelToView(p0, b0, p1, b1, r0);
        }
        // Straddles some views.
        int viewCount = getViewCount();
        int counter = 0;
        while (counter < viewCount) {
            View v;
            // Views may not be in same order as model.
            // v0 or v1 may be null if there is a gap in the range this
            // view contains.
            if ((v = getView(counter)) == v0 || v == v1) {
                View endView;
                Rectangle retRect;
                Rectangle tempRect = new Rectangle();
                if (v == v0) {
                    retRect = v0.modelToView(p0, b0, v0.getEndOffset(),
                                             Position.Bias.Backward, r0).
                              getBounds();
                    endView = v1;
                }
                else {
                    retRect = v1.modelToView(v1.getStartOffset(),
                                             Position.Bias.Forward,
                                             p1, b1, r1).getBounds();
                    endView = v0;
                }

                // Views entirely covered by range.
                while (++counter < viewCount &&
                       (v = getView(counter)) != endView) {
                    tempRect.setBounds(alloc);
                    childAllocation(counter, tempRect);
                    retRect.add(tempRect);
                }

                // End view.
                if (endView != null) {
                    Shape endShape;
                    if (endView == v1) {
                        endShape = v1.modelToView(v1.getStartOffset(),
                                                  Position.Bias.Forward,
                                                  p1, b1, r1);
                    }
                    else {
                        endShape = v0.modelToView(p0, b0, v0.getEndOffset(),
                                                  Position.Bias.Backward, r0);
                    }
                    if (endShape instanceof Rectangle) {
                        retRect.add((Rectangle)endShape);
                    }
                    else {
                        retRect.add(endShape.getBounds());
                    }
                }
                return retRect;
            }
            counter++;
        }
        throw new BadLocationException("Position not represented by view", p0);
    }

    /**
     * Provides a mapping from the view coordinate space to the logical
     * coordinate space of the model.
     *
     * @param x   x coordinate of the view location to convert &gt;= 0
     * @param y   y coordinate of the view location to convert &gt;= 0
     * @param a the allocated region to render into
     * @param bias either <code>Position.Bias.Forward</code> or
     *  <code>Position.Bias.Backward</code>
     * @return the location within the model that best represents the
     *  given point in the view &gt;= 0
     * @see View#viewToModel
     */
    public int viewToModel(float x, float y, Shape a, Position.Bias[] bias) {
        Rectangle alloc = getInsideAllocation(a);
        if (isBefore((int) x, (int) y, alloc)) {
            // point is before the range represented
            int retValue = -1;

            try {
                retValue = getNextVisualPositionFrom(-1, Position.Bias.Forward,
                                                     a, EAST, bias);
            } catch (BadLocationException ble) { }
            catch (IllegalArgumentException iae) { }
            if(retValue == -1) {
                retValue = getStartOffset();
                bias[0] = Position.Bias.Forward;
            }
            return retValue;
        } else if (isAfter((int) x, (int) y, alloc)) {
            // point is after the range represented.
            int retValue = -1;
            try {
                retValue = getNextVisualPositionFrom(-1, Position.Bias.Forward,
                                                     a, WEST, bias);
            } catch (BadLocationException ble) { }
            catch (IllegalArgumentException iae) { }

            if(retValue == -1) {
                // NOTE: this could actually use end offset with backward.
                retValue = getEndOffset() - 1;
                bias[0] = Position.Bias.Forward;
            }
            return retValue;
        } else {
            // locate the child and pass along the request
            View v = getViewAtPoint((int) x, (int) y, alloc);
            if (v != null) {
              return v.viewToModel(x, y, alloc, bias);
            }
        }
        return -1;
    }

    /**
     * Provides a way to determine the next visually represented model
     * location that one might place a caret.  Some views may not be visible,
     * they might not be in the same order found in the model, or they just
     * might not allow access to some of the locations in the model.
     * This is a convenience method for {@link #getNextNorthSouthVisualPositionFrom}
     * and {@link #getNextEastWestVisualPositionFrom}.
     * This method enables specifying a position to convert
     * within the range of &gt;=0.  If the value is -1, a position
     * will be calculated automatically.  If the value &lt; -1,
     * the {@code BadLocationException} will be thrown.
     *
     * @param pos the position to convert
     * @param b a bias value of either <code>Position.Bias.Forward</code>
     *  or <code>Position.Bias.Backward</code>
     * @param a the allocated region to render into
     * @param direction the direction from the current position that can
     *  be thought of as the arrow keys typically found on a keyboard;
     *  this may be one of the following:
     *  <ul>
     *  <li><code>SwingConstants.WEST</code>
     *  <li><code>SwingConstants.EAST</code>
     *  <li><code>SwingConstants.NORTH</code>
     *  <li><code>SwingConstants.SOUTH</code>
     *  </ul>
     * @param biasRet an array containing the bias that was checked
     * @return the location within the model that best represents the next
     *  location visual position
     * @exception BadLocationException the given position is not a valid
     *                                 position within the document
     * @exception IllegalArgumentException if <code>direction</code> is invalid
     */
    public int getNextVisualPositionFrom(int pos, Position.Bias b, Shape a,
                                         int direction, Position.Bias[] biasRet)
      throws BadLocationException {
        if (pos < -1 || pos > getDocument().getLength()) {
            throw new BadLocationException("invalid position", pos);
        }
        Rectangle alloc = getInsideAllocation(a);

        switch (direction) {
        case NORTH:
            return getNextNorthSouthVisualPositionFrom(pos, b, a, direction,
                                                       biasRet);
        case SOUTH:
            return getNextNorthSouthVisualPositionFrom(pos, b, a, direction,
                                                       biasRet);
        case EAST:
            return getNextEastWestVisualPositionFrom(pos, b, a, direction,
                                                     biasRet);
        case WEST:
            return getNextEastWestVisualPositionFrom(pos, b, a, direction,
                                                     biasRet);
        default:
            throw new IllegalArgumentException("Bad direction: " + direction);
        }
    }

    /**
     * Returns the child view index representing the given
     * position in the model.  This is implemented to call the
     * <code>getViewIndexByPosition</code>
     * method for backward compatibility.
     *
     * @param pos the position &gt;= 0
     * @return  index of the view representing the given position, or
     *   -1 if no view represents that position
     * @since 1.3
     */
    public int getViewIndex(int pos, Position.Bias b) {
        if(b == Position.Bias.Backward) {
            pos -= 1;
        }
        if ((pos >= getStartOffset()) && (pos < getEndOffset())) {
            return getViewIndexAtPosition(pos);
        }
        return -1;
    }

    // --- local methods ----------------------------------------------------


    /**
     * Tests whether a point lies before the rectangle range.
     *
     * @param x the X coordinate &gt;= 0
     * @param y the Y coordinate &gt;= 0
     * @param alloc the rectangle
     * @return true if the point is before the specified range
     */
    protected abstract boolean isBefore(int x, int y, Rectangle alloc);

    /**
     * Tests whether a point lies after the rectangle range.
     *
     * @param x the X coordinate &gt;= 0
     * @param y the Y coordinate &gt;= 0
     * @param alloc the rectangle
     * @return true if the point is after the specified range
     */
    protected abstract boolean isAfter(int x, int y, Rectangle alloc);

    /**
     * Fetches the child view at the given coordinates.
     *
     * @param x the X coordinate &gt;= 0
     * @param y the Y coordinate &gt;= 0
     * @param alloc the parent's allocation on entry, which should
     *   be changed to the child's allocation on exit
     * @return the child view
     */
    protected abstract View getViewAtPoint(int x, int y, Rectangle alloc);

    /**
     * Returns the allocation for a given child.
     *
     * @param index the index of the child, &gt;= 0 &amp;&amp; &lt; getViewCount()
     * @param a  the allocation to the interior of the box on entry,
     *   and the allocation of the child view at the index on exit.
     */
    protected abstract void childAllocation(int index, Rectangle a);

    /**
     * Fetches the child view that represents the given position in
     * the model.  This is implemented to fetch the view in the case
     * where there is a child view for each child element.
     *
     * @param pos the position &gt;= 0
     * @param a  the allocation to the interior of the box on entry,
     *   and the allocation of the view containing the position on exit
     * @return  the view representing the given position, or
     *   <code>null</code> if there isn't one
     */
    protected View getViewAtPosition(int pos, Rectangle a) {
        int index = getViewIndexAtPosition(pos);
        if ((index >= 0) && (index < getViewCount())) {
            View v = getView(index);
            if (a != null) {
                childAllocation(index, a);
            }
            return v;
        }
        return null;
    }

    /**
     * Fetches the child view index representing the given position in
     * the model.  This is implemented to fetch the view in the case
     * where there is a child view for each child element.
     *
     * @param pos the position &gt;= 0
     * @return  index of the view representing the given position, or
     *   -1 if no view represents that position
     */
    protected int getViewIndexAtPosition(int pos) {
        Element elem = getElement();
        return elem.getElementIndex(pos);
    }

    /**
     * Translates the immutable allocation given to the view
     * to a mutable allocation that represents the interior
     * allocation (i.e. the bounds of the given allocation
     * with the top, left, bottom, and right insets removed.
     * It is expected that the returned value would be further
     * mutated to represent an allocation to a child view.
     * This is implemented to reuse an instance variable so
     * it avoids creating excessive Rectangles.  Typically
     * the result of calling this method would be fed to
     * the <code>childAllocation</code> method.
     *
     * @param a the allocation given to the view
     * @return the allocation that represents the inside of the
     *   view after the margins have all been removed; if the
     *   given allocation was <code>null</code>,
     *   the return value is <code>null</code>
     */
    protected Rectangle getInsideAllocation(Shape a) {
        if (a != null) {
            // get the bounds, hopefully without allocating
            // a new rectangle.  The Shape argument should
            // not be modified... we copy it into the
            // child allocation.
            Rectangle alloc;
            if (a instanceof Rectangle) {
                alloc = (Rectangle) a;
            } else {
                alloc = a.getBounds();
            }

            childAlloc.setBounds(alloc);
            childAlloc.x += getLeftInset();
            childAlloc.y += getTopInset();
            childAlloc.width -= getLeftInset() + getRightInset();
            childAlloc.height -= getTopInset() + getBottomInset();
            return childAlloc;
        }
        return null;
    }

    /**
     * Sets the insets from the paragraph attributes specified in
     * the given attributes.
     *
     * @param attr the attributes
     */
    protected void setParagraphInsets(AttributeSet attr) {
        // Since version 1.1 doesn't have scaling and assumes
        // a pixel is equal to a point, we just cast the point
        // sizes to integers.
        top = (short) StyleConstants.getSpaceAbove(attr);
        left = (short) StyleConstants.getLeftIndent(attr);
        bottom = (short) StyleConstants.getSpaceBelow(attr);
        right = (short) StyleConstants.getRightIndent(attr);
    }

    /**
     * Sets the insets for the view.
     *
     * @param top the top inset &gt;= 0
     * @param left the left inset &gt;= 0
     * @param bottom the bottom inset &gt;= 0
     * @param right the right inset &gt;= 0
     */
    protected void setInsets(short top, short left, short bottom, short right) {
        this.top = top;
        this.left = left;
        this.right = right;
        this.bottom = bottom;
    }

    /**
     * Gets the left inset.
     *
     * @return the inset &gt;= 0
     */
    protected short getLeftInset() {
        return left;
    }

    /**
     * Gets the right inset.
     *
     * @return the inset &gt;= 0
     */
    protected short getRightInset() {
        return right;
    }

    /**
     * Gets the top inset.
     *
     * @return the inset &gt;= 0
     */
    protected short getTopInset() {
        return top;
    }

    /**
     * Gets the bottom inset.
     *
     * @return the inset &gt;= 0
     */
    protected short getBottomInset() {
        return bottom;
    }

    /**
     * Returns the next visual position for the cursor, in either the
     * north or south direction.
     *
     * @param pos the position to convert &gt;= 0
     * @param b a bias value of either <code>Position.Bias.Forward</code>
     *  or <code>Position.Bias.Backward</code>
     * @param a the allocated region to render into
     * @param direction the direction from the current position that can
     *  be thought of as the arrow keys typically found on a keyboard;
     *  this may be one of the following:
     *  <ul>
     *  <li><code>SwingConstants.NORTH</code>
     *  <li><code>SwingConstants.SOUTH</code>
     *  </ul>
     * @param biasRet an array containing the bias that was checked
     * @return the location within the model that best represents the next
     *  north or south location
     * @exception BadLocationException for a bad location within a document model
     * @exception IllegalArgumentException if <code>direction</code> is invalid
     * @see #getNextVisualPositionFrom
     */
    protected int getNextNorthSouthVisualPositionFrom(int pos, Position.Bias b,
                                                      Shape a, int direction,
                                                      Position.Bias[] biasRet)
                                                throws BadLocationException {
        if (pos < -1 || pos > getDocument().getLength()) {
            throw new BadLocationException("invalid position", pos);
        }
        return Utilities.getNextVisualPositionFrom(
                            this, pos, b, a, direction, biasRet);
    }

    /**
     * Returns the next visual position for the cursor, in either the
     * east or west direction.
     *
    * @param pos the position to convert &gt;= 0
     * @param b a bias value of either <code>Position.Bias.Forward</code>
     *  or <code>Position.Bias.Backward</code>
     * @param a the allocated region to render into
     * @param direction the direction from the current position that can
     *  be thought of as the arrow keys typically found on a keyboard;
     *  this may be one of the following:
     *  <ul>
     *  <li><code>SwingConstants.WEST</code>
     *  <li><code>SwingConstants.EAST</code>
     *  </ul>
     * @param biasRet an array containing the bias that was checked
     * @return the location within the model that best represents the next
     *  west or east location
     * @exception BadLocationException for a bad location within a document model
     * @exception IllegalArgumentException if <code>direction</code> is invalid
     * @see #getNextVisualPositionFrom
     */
    protected int getNextEastWestVisualPositionFrom(int pos, Position.Bias b,
                                                    Shape a,
                                                    int direction,
                                                    Position.Bias[] biasRet)
                                                throws BadLocationException {
        if (pos < -1 || pos > getDocument().getLength()) {
            throw new BadLocationException("invalid position", pos);
        }
        return Utilities.getNextVisualPositionFrom(
                            this, pos, b, a, direction, biasRet);
    }

    /**
     * Determines in which direction the next view lays.
     * Consider the <code>View</code> at index n. Typically the
     * <code>View</code>s are layed out from left to right,
     * so that the <code>View</code> to the EAST will be
     * at index n + 1, and the <code>View</code> to the WEST
     * will be at index n - 1. In certain situations,
     * such as with bidirectional text, it is possible
     * that the <code>View</code> to EAST is not at index n + 1,
     * but rather at index n - 1, or that the <code>View</code>
     * to the WEST is not at index n - 1, but index n + 1.
     * In this case this method would return true, indicating the
     * <code>View</code>s are layed out in descending order.
     * <p>
     * This unconditionally returns false, subclasses should override this
     * method if there is the possibility for laying <code>View</code>s in
     * descending order.
     *
     * @param position position into the model
     * @param bias either <code>Position.Bias.Forward</code> or
     *          <code>Position.Bias.Backward</code>
     * @return false
     */
    protected boolean flipEastAndWestAtEnds(int position,
                                            Position.Bias bias) {
        return false;
    }


    // ---- member variables ---------------------------------------------


    private static View[] ZERO = new View[0];

    private View[] children;
    private int nchildren;
    private short left;
    private short right;
    private short top;
    private short bottom;
    private Rectangle childAlloc;
}
