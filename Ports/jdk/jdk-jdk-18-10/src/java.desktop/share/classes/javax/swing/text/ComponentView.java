/*
 * Copyright (c) 1997, 2015, Oracle and/or its affiliates. All rights reserved.
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
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Set;
import javax.swing.SwingUtilities;
import javax.swing.event.*;

/**
 * Component decorator that implements the view interface.  The
 * entire element is used to represent the component.  This acts
 * as a gateway from the display-only View implementations to
 * interactive lightweight components (ie it allows components
 * to be embedded into the View hierarchy).
 * <p>
 * The component is placed relative to the text baseline
 * according to the value returned by
 * <code>Component.getAlignmentY</code>.  For Swing components
 * this value can be conveniently set using the method
 * <code>JComponent.setAlignmentY</code>.  For example, setting
 * a value of <code>0.75</code> will cause 75 percent of the
 * component to be above the baseline, and 25 percent of the
 * component to be below the baseline.
 * <p>
 * This class is implemented to do the extra work necessary to
 * work properly in the presence of multiple threads (i.e. from
 * asynchronous notification of model changes for example) by
 * ensuring that all component access is done on the event thread.
 * <p>
 * The component used is determined by the return value of the
 * createComponent method.  The default implementation of this
 * method is to return the component held as an attribute of
 * the element (by calling StyleConstants.getComponent).  A
 * limitation of this behavior is that the component cannot
 * be used by more than one text component (i.e. with a shared
 * model).  Subclasses can remove this constraint by implementing
 * the createComponent to actually create a component based upon
 * some kind of specification contained in the attributes.  The
 * ObjectView class in the html package is an example of a
 * ComponentView implementation that supports multiple component
 * views of a shared model.
 *
 * @author Timothy Prinzing
 */
public class ComponentView extends View  {

    /**
     * Creates a new ComponentView object.
     *
     * @param elem the element to decorate
     */
    public ComponentView(Element elem) {
        super(elem);
    }

    /**
     * Create the component that is associated with
     * this view.  This will be called when it has
     * been determined that a new component is needed.
     * This would result from a call to setParent or
     * as a result of being notified that attributes
     * have changed.
     * @return the component that is associated with
     * this view
     */
    protected Component createComponent() {
        AttributeSet attr = getElement().getAttributes();
        Component comp = StyleConstants.getComponent(attr);
        return comp;
    }

    /**
     * Fetch the component associated with the view.
     * @return the component associated with the view
     */
    public final Component getComponent() {
        return createdC;
    }

    // --- View methods ---------------------------------------------

    /**
     * The real paint behavior occurs naturally from the association
     * that the component has with its parent container (the same
     * container hosting this view).  This is implemented to do nothing.
     *
     * @param g the graphics context
     * @param a the shape
     * @see View#paint
     */
    public void paint(Graphics g, Shape a) {
        if (c != null) {
            Rectangle alloc = (a instanceof Rectangle) ?
                (Rectangle) a : a.getBounds();
            c.setBounds(alloc.x, alloc.y, alloc.width, alloc.height);
        }
    }

    /**
     * Determines the preferred span for this view along an
     * axis.  This is implemented to return the value
     * returned by Component.getPreferredSize along the
     * axis of interest.
     *
     * @param axis may be either View.X_AXIS or View.Y_AXIS
     * @return   the span the view would like to be rendered into &gt;=0.
     *           Typically the view is told to render into the span
     *           that is returned, although there is no guarantee.
     *           The parent may choose to resize or break the view.
     * @exception IllegalArgumentException for an invalid axis
     */
    public float getPreferredSpan(int axis) {
        if ((axis != X_AXIS) && (axis != Y_AXIS)) {
            throw new IllegalArgumentException("Invalid axis: " + axis);
        }
        if (c != null) {
            Dimension size = c.getPreferredSize();
            if (axis == View.X_AXIS) {
                return size.width;
            } else {
                return size.height;
            }
        }
        return 0;
    }

    /**
     * Determines the minimum span for this view along an
     * axis.  This is implemented to return the value
     * returned by Component.getMinimumSize along the
     * axis of interest.
     *
     * @param axis may be either View.X_AXIS or View.Y_AXIS
     * @return   the span the view would like to be rendered into &gt;=0.
     *           Typically the view is told to render into the span
     *           that is returned, although there is no guarantee.
     *           The parent may choose to resize or break the view.
     * @exception IllegalArgumentException for an invalid axis
     */
    public float getMinimumSpan(int axis) {
        if ((axis != X_AXIS) && (axis != Y_AXIS)) {
            throw new IllegalArgumentException("Invalid axis: " + axis);
        }
        if (c != null) {
            Dimension size = c.getMinimumSize();
            if (axis == View.X_AXIS) {
                return size.width;
            } else {
                return size.height;
            }
        }
        return 0;
    }

    /**
     * Determines the maximum span for this view along an
     * axis.  This is implemented to return the value
     * returned by Component.getMaximumSize along the
     * axis of interest.
     *
     * @param axis may be either View.X_AXIS or View.Y_AXIS
     * @return   the span the view would like to be rendered into &gt;=0.
     *           Typically the view is told to render into the span
     *           that is returned, although there is no guarantee.
     *           The parent may choose to resize or break the view.
     * @exception IllegalArgumentException for an invalid axis
     */
    public float getMaximumSpan(int axis) {
        if ((axis != X_AXIS) && (axis != Y_AXIS)) {
            throw new IllegalArgumentException("Invalid axis: " + axis);
        }
        if (c != null) {
            Dimension size = c.getMaximumSize();
            if (axis == View.X_AXIS) {
                return size.width;
            } else {
                return size.height;
            }
        }
        return 0;
    }

    /**
     * Determines the desired alignment for this view along an
     * axis.  This is implemented to give the alignment of the
     * embedded component.
     *
     * @param axis may be either View.X_AXIS or View.Y_AXIS
     * @return the desired alignment.  This should be a value
     *   between 0.0 and 1.0 where 0 indicates alignment at the
     *   origin and 1.0 indicates alignment to the full span
     *   away from the origin.  An alignment of 0.5 would be the
     *   center of the view.
     */
    public float getAlignment(int axis) {
        if (c != null) {
            switch (axis) {
            case View.X_AXIS:
                return c.getAlignmentX();
            case View.Y_AXIS:
                return c.getAlignmentY();
            }
        }
        return super.getAlignment(axis);
    }

    /**
     * Sets the parent for a child view.
     * The parent calls this on the child to tell it who its
     * parent is, giving the view access to things like
     * the hosting Container.  The superclass behavior is
     * executed, followed by a call to createComponent if
     * the parent view parameter is non-null and a component
     * has not yet been created. The embedded components parent
     * is then set to the value returned by <code>getContainer</code>.
     * If the parent view parameter is null, this view is being
     * cleaned up, thus the component is removed from its parent.
     * <p>
     * The changing of the component hierarchy will
     * touch the component lock, which is the one thing
     * that is not safe from the View hierarchy.  Therefore,
     * this functionality is executed immediately if on the
     * event thread, or is queued on the event queue if
     * called from another thread (notification of change
     * from an asynchronous update).
     *
     * @param p the parent
     */
    public void setParent(View p) {
        super.setParent(p);
        if (SwingUtilities.isEventDispatchThread()) {
            setComponentParent();
        } else {
            Runnable callSetComponentParent = new Runnable() {
                public void run() {
                    Document doc = getDocument();
                    try {
                        if (doc instanceof AbstractDocument) {
                            ((AbstractDocument)doc).readLock();
                        }
                        setComponentParent();
                        Container host = getContainer();
                        if (host != null) {
                            preferenceChanged(null, true, true);
                            host.repaint();
                        }
                    } finally {
                        if (doc instanceof AbstractDocument) {
                            ((AbstractDocument)doc).readUnlock();
                        }
                    }
                }
            };
            SwingUtilities.invokeLater(callSetComponentParent);
        }
    }

    /**
     * Set the parent of the embedded component
     * with assurance that it is thread-safe.
     */
    void setComponentParent() {
        View p = getParent();
        if (p != null) {
            Container parent = getContainer();
            if (parent != null) {
                if (c == null) {
                    // try to build a component
                    Component comp = createComponent();
                    if (comp != null) {
                        createdC = comp;
                        c = new Invalidator(comp);
                    }
                }
                if (c != null) {
                    if (c.getParent() == null) {
                        // components associated with the View tree are added
                        // to the hosting container with the View as a constraint.
                        parent.add(c, this);
                        parent.addPropertyChangeListener("enabled", c);
                    }
                }
            }
        } else {
            if (c != null) {
                Container parent = c.getParent();
                if (parent != null) {
                    // remove the component from its hosting container
                    parent.remove(c);
                    parent.removePropertyChangeListener("enabled", c);
                }
            }
        }
    }

    /**
     * Provides a mapping from the coordinate space of the model to
     * that of the view.
     *
     * @param pos the position to convert &gt;=0
     * @param a the allocated region to render into
     * @return the bounding box of the given position is returned
     * @exception BadLocationException  if the given position does not
     *   represent a valid location in the associated document
     * @see View#modelToView
     */
    public Shape modelToView(int pos, Shape a, Position.Bias b) throws BadLocationException {
        int p0 = getStartOffset();
        int p1 = getEndOffset();
        if ((pos >= p0) && (pos <= p1)) {
            Rectangle r = a.getBounds();
            if (pos == p1) {
                r.x += r.width;
            }
            r.width = 0;
            return r;
        }
        throw new BadLocationException(pos + " not in range " + p0 + "," + p1, pos);
    }

    /**
     * Provides a mapping from the view coordinate space to the logical
     * coordinate space of the model.
     *
     * @param x the X coordinate &gt;=0
     * @param y the Y coordinate &gt;=0
     * @param a the allocated region to render into
     * @return the location within the model that best represents
     *    the given point in the view
     * @see View#viewToModel
     */
    public int viewToModel(float x, float y, Shape a, Position.Bias[] bias) {
        Rectangle alloc = (Rectangle) a;
        if (x < alloc.x + (alloc.width / 2)) {
            bias[0] = Position.Bias.Forward;
            return getStartOffset();
        }
        bias[0] = Position.Bias.Backward;
        return getEndOffset();
    }

    // --- member variables ------------------------------------------------

    private Component createdC;
    private Invalidator c;

    /**
     * This class feeds the invalidate back to the
     * hosting View.  This is needed to get the View
     * hierarchy to consider giving the component
     * a different size (i.e. layout may have been
     * cached between the associated view and the
     * container hosting this component).
     */
    @SuppressWarnings("serial") // JDK-implementation class
    class Invalidator extends Container implements PropertyChangeListener {

        // NOTE: When we remove this class we are going to have to some
        // how enforce setting of the focus traversal keys on the children
        // so that they don't inherit them from the JEditorPane. We need
        // to do this as JEditorPane has abnormal bindings (it is a focus cycle
        // root) and the children typically don't want these bindings as well.

        Invalidator(Component child) {
            setLayout(null);
            add(child);
            cacheChildSizes();
        }

        /**
         * The components invalid layout needs
         * to be propagated through the view hierarchy
         * so the views (which position the component)
         * can have their layout recomputed.
         */
        public void invalidate() {
            super.invalidate();
            if (getParent() != null) {
                preferenceChanged(null, true, true);
            }
        }

        public void doLayout() {
            cacheChildSizes();
        }

        public void setBounds(int x, int y, int w, int h) {
            super.setBounds(x, y, w, h);
            if (getComponentCount() > 0) {
                getComponent(0).setSize(w, h);
            }
            cacheChildSizes();
        }

        public void validateIfNecessary() {
            if (!isValid()) {
                validate();
             }
        }

        private void cacheChildSizes() {
            if (getComponentCount() > 0) {
                Component child = getComponent(0);
                min = child.getMinimumSize();
                pref = child.getPreferredSize();
                max = child.getMaximumSize();
                yalign = child.getAlignmentY();
                xalign = child.getAlignmentX();
            } else {
                min = pref = max = new Dimension(0, 0);
            }
        }

        /**
         * Shows or hides this component depending on the value of parameter
         * <code>b</code>.
         * @param b If <code>true</code>, shows this component;
         * otherwise, hides this component.
         * @see #isVisible
         * @since 1.1
         */
        public void setVisible(boolean b) {
            super.setVisible(b);
            if (getComponentCount() > 0) {
                getComponent(0).setVisible(b);
            }
        }

        /**
         * Overridden to fix 4759054. Must return true so that content
         * is painted when inside a CellRendererPane which is normally
         * invisible.
         */
        public boolean isShowing() {
            return true;
        }

        public Dimension getMinimumSize() {
            validateIfNecessary();
            return min;
        }

        public Dimension getPreferredSize() {
            validateIfNecessary();
            return pref;
        }

        public Dimension getMaximumSize() {
            validateIfNecessary();
            return max;
        }

        public float getAlignmentX() {
            validateIfNecessary();
            return xalign;
        }

        public float getAlignmentY() {
            validateIfNecessary();
            return yalign;
        }

        public Set<AWTKeyStroke> getFocusTraversalKeys(int id) {
            return KeyboardFocusManager.getCurrentKeyboardFocusManager().
                    getDefaultFocusTraversalKeys(id);
        }

        public void propertyChange(PropertyChangeEvent ev) {
            Boolean enable = (Boolean) ev.getNewValue();
            if (getComponentCount() > 0) {
                getComponent(0).setEnabled(enable);
            }
        }

        Dimension min;
        Dimension pref;
        Dimension max;
        float yalign;
        float xalign;

    }

}
