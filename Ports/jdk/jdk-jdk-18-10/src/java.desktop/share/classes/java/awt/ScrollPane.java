/*
 * Copyright (c) 1996, 2021, Oracle and/or its affiliates. All rights reserved.
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

package java.awt;

import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.peer.ScrollPanePeer;
import java.beans.ConstructorProperties;
import java.beans.Transient;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;

import sun.awt.ScrollPaneWheelScroller;
import sun.awt.SunToolkit;

/**
 * A container class which implements automatic horizontal and/or
 * vertical scrolling for a single child component.  The display
 * policy for the scrollbars can be set to:
 * <OL>
 * <LI>as needed: scrollbars created and shown only when needed by scrollpane
 * <LI>always: scrollbars created and always shown by the scrollpane
 * <LI>never: scrollbars never created or shown by the scrollpane
 * </OL>
 * <P>
 * The state of the horizontal and vertical scrollbars is represented
 * by two {@code ScrollPaneAdjustable} objects (one for each
 * dimension) which implement the {@code Adjustable} interface.
 * The API provides methods to access those objects such that the
 * attributes on the Adjustable object (such as unitIncrement, value,
 * etc.) can be manipulated.
 * <P>
 * Certain adjustable properties (minimum, maximum, blockIncrement,
 * and visibleAmount) are set internally by the scrollpane in accordance
 * with the geometry of the scrollpane and its child and these should
 * not be set by programs using the scrollpane.
 * <P>
 * If the scrollbar display policy is defined as "never", then the
 * scrollpane can still be programmatically scrolled using the
 * setScrollPosition() method and the scrollpane will move and clip
 * the child's contents appropriately.  This policy is useful if the
 * program needs to create and manage its own adjustable controls.
 * <P>
 * The placement of the scrollbars is controlled by platform-specific
 * properties set by the user outside of the program.
 * <P>
 * The initial size of this container is set to 100x100, but can
 * be reset using setSize().
 * <P>
 * Scrolling with the wheel on a wheel-equipped mouse is enabled by default.
 * This can be disabled using {@code setWheelScrollingEnabled}.
 * Wheel scrolling can be customized by setting the block and
 * unit increment of the horizontal and vertical Adjustables.
 * For information on how mouse wheel events are dispatched, see
 * the class description for {@link MouseWheelEvent}.
 * <P>
 * Insets are used to define any space used by scrollbars and any
 * borders created by the scroll pane. getInsets() can be used
 * to get the current value for the insets.  If the value of
 * scrollbarsAlwaysVisible is false, then the value of the insets
 * will change dynamically depending on whether the scrollbars are
 * currently visible or not.
 *
 * @author      Tom Ball
 * @author      Amy Fowler
 * @author      Tim Prinzing
 */
public class ScrollPane extends Container implements Accessible {


    /**
     * Initialize JNI field and method IDs
     */
    private static native void initIDs();

    static {
        /* ensure that the necessary native libraries are loaded */
        Toolkit.loadLibraries();
        if (!GraphicsEnvironment.isHeadless()) {
            initIDs();
        }
    }

    /**
     * Specifies that horizontal/vertical scrollbar should be shown
     * only when the size of the child exceeds the size of the scrollpane
     * in the horizontal/vertical dimension.
     */
    public static final int SCROLLBARS_AS_NEEDED = 0;

    /**
     * Specifies that horizontal/vertical scrollbars should always be
     * shown regardless of the respective sizes of the scrollpane and child.
     */
    public static final int SCROLLBARS_ALWAYS = 1;

    /**
     * Specifies that horizontal/vertical scrollbars should never be shown
     * regardless of the respective sizes of the scrollpane and child.
     */
    public static final int SCROLLBARS_NEVER = 2;

    /**
     * There are 3 ways in which a scroll bar can be displayed.
     * This integer will represent one of these 3 displays -
     * (SCROLLBARS_ALWAYS, SCROLLBARS_AS_NEEDED, SCROLLBARS_NEVER)
     *
     * @serial
     * @see #getScrollbarDisplayPolicy
     */
    private int scrollbarDisplayPolicy;

    /**
     * An adjustable vertical scrollbar.
     * It is important to note that you must <em>NOT</em> call 3
     * {@code Adjustable} methods, namely:
     * {@code setMinimum()}, {@code setMaximum()},
     * {@code setVisibleAmount()}.
     *
     * @serial
     * @see #getVAdjustable
     */
    private ScrollPaneAdjustable vAdjustable;

    /**
     * An adjustable horizontal scrollbar.
     * It is important to note that you must <em>NOT</em> call 3
     * {@code Adjustable} methods, namely:
     * {@code setMinimum()}, {@code setMaximum()},
     * {@code setVisibleAmount()}.
     *
     * @serial
     * @see #getHAdjustable
     */
    private ScrollPaneAdjustable hAdjustable;

    private static final String base = "scrollpane";
    private static int nameCounter = 0;

    private static final boolean defaultWheelScroll = true;

    /**
     * Indicates whether or not scrolling should take place when a
     * MouseWheelEvent is received.
     *
     * @serial
     * @since 1.4
     */
    private boolean wheelScrollingEnabled = defaultWheelScroll;

    /**
     * Use serialVersionUID from JDK 1.1 for interoperability.
     */
    @Serial
    private static final long serialVersionUID = 7956609840827222915L;

    /**
     * Create a new scrollpane container with a scrollbar display
     * policy of "as needed".
     * @throws HeadlessException if GraphicsEnvironment.isHeadless()
     *     returns true
     * @see java.awt.GraphicsEnvironment#isHeadless
     */
    public ScrollPane() throws HeadlessException {
        this(SCROLLBARS_AS_NEEDED);
    }

    /**
     * Create a new scrollpane container.
     * @param scrollbarDisplayPolicy policy for when scrollbars should be shown
     * @throws IllegalArgumentException if the specified scrollbar
     *     display policy is invalid
     * @throws HeadlessException if GraphicsEnvironment.isHeadless()
     *     returns true
     * @see java.awt.GraphicsEnvironment#isHeadless
     */
    @ConstructorProperties({"scrollbarDisplayPolicy"})
    public ScrollPane(int scrollbarDisplayPolicy) throws HeadlessException {
        GraphicsEnvironment.checkHeadless();
        this.layoutMgr = null;
        this.width = 100;
        this.height = 100;
        switch (scrollbarDisplayPolicy) {
            case SCROLLBARS_NEVER:
            case SCROLLBARS_AS_NEEDED:
            case SCROLLBARS_ALWAYS:
                this.scrollbarDisplayPolicy = scrollbarDisplayPolicy;
                break;
            default:
                throw new IllegalArgumentException("illegal scrollbar display policy");
        }

        vAdjustable = new ScrollPaneAdjustable(this, new PeerFixer(this),
                                               Adjustable.VERTICAL);
        hAdjustable = new ScrollPaneAdjustable(this, new PeerFixer(this),
                                               Adjustable.HORIZONTAL);
        setWheelScrollingEnabled(defaultWheelScroll);
    }

    /**
     * Construct a name for this component.  Called by getName() when the
     * name is null.
     */
    String constructComponentName() {
        synchronized (ScrollPane.class) {
            return base + nameCounter++;
        }
    }

    // The scrollpane won't work with a windowless child... it assumes
    // it is moving a child window around so the windowless child is
    // wrapped with a window.
    private void addToPanel(Component comp, Object constraints, int index) {
        Panel child = new Panel();
        child.setLayout(new BorderLayout());
        child.add(comp);
        super.addImpl(child, constraints, index);
        validate();
    }

    /**
     * Adds the specified component to this scroll pane container.
     * If the scroll pane has an existing child component, that
     * component is removed and the new one is added.
     * @param comp the component to be added
     * @param constraints  not applicable
     * @param index position of child component (must be &lt;= 0)
     */
    protected final void addImpl(Component comp, Object constraints, int index) {
        synchronized (getTreeLock()) {
            if (getComponentCount() > 0) {
                remove(0);
            }
            if (index > 0) {
                throw new IllegalArgumentException("position greater than 0");
            }

            if (!SunToolkit.isLightweightOrUnknown(comp)) {
                super.addImpl(comp, constraints, index);
            } else {
                addToPanel(comp, constraints, index);
            }
        }
    }

    /**
     * Returns the display policy for the scrollbars.
     * @return the display policy for the scrollbars
     */
    public int getScrollbarDisplayPolicy() {
        return scrollbarDisplayPolicy;
    }

    /**
     * Returns the current size of the scroll pane's view port.
     * @return the size of the view port in pixels
     */
    public Dimension getViewportSize() {
        Insets i = getInsets();
        return new Dimension(width - i.right - i.left,
                             height - i.top - i.bottom);
    }

    /**
     * Returns the height that would be occupied by a horizontal
     * scrollbar, which is independent of whether it is currently
     * displayed by the scroll pane or not.
     * @return the height of a horizontal scrollbar in pixels
     */
    public int getHScrollbarHeight() {
        int h = 0;
        if (scrollbarDisplayPolicy != SCROLLBARS_NEVER) {
            ScrollPanePeer peer = (ScrollPanePeer)this.peer;
            if (peer != null) {
                h = peer.getHScrollbarHeight();
            }
        }
        return h;
    }

    /**
     * Returns the width that would be occupied by a vertical
     * scrollbar, which is independent of whether it is currently
     * displayed by the scroll pane or not.
     * @return the width of a vertical scrollbar in pixels
     */
    public int getVScrollbarWidth() {
        int w = 0;
        if (scrollbarDisplayPolicy != SCROLLBARS_NEVER) {
            ScrollPanePeer peer = (ScrollPanePeer)this.peer;
            if (peer != null) {
                w = peer.getVScrollbarWidth();
            }
        }
        return w;
    }

    /**
     * Returns the {@code ScrollPaneAdjustable} object which
     * represents the state of the vertical scrollbar.
     * The declared return type of this method is
     * {@code Adjustable} to maintain backward compatibility.
     *
     * @see java.awt.ScrollPaneAdjustable
     * @return the vertical scrollbar state
     */
    public Adjustable getVAdjustable() {
        return vAdjustable;
    }

    /**
     * Returns the {@code ScrollPaneAdjustable} object which
     * represents the state of the horizontal scrollbar.
     * The declared return type of this method is
     * {@code Adjustable} to maintain backward compatibility.
     *
     * @see java.awt.ScrollPaneAdjustable
     * @return the horizontal scrollbar state
     */
    public Adjustable getHAdjustable() {
        return hAdjustable;
    }

    /**
     * Scrolls to the specified position within the child component.
     * A call to this method is only valid if the scroll pane contains
     * a child.  Specifying a position outside of the legal scrolling bounds
     * of the child will scroll to the closest legal position.
     * Legal bounds are defined to be the rectangle:
     * x = 0, y = 0, width = (child width - view port width),
     * height = (child height - view port height).
     * This is a convenience method which interfaces with the Adjustable
     * objects which represent the state of the scrollbars.
     * @param x the x position to scroll to
     * @param y the y position to scroll to
     * @throws NullPointerException if the scrollpane does not contain
     *     a child
     */
    public void setScrollPosition(int x, int y) {
        synchronized (getTreeLock()) {
            if (getComponentCount()==0) {
                throw new NullPointerException("child is null");
            }
            hAdjustable.setValue(x);
            vAdjustable.setValue(y);
        }
    }

    /**
     * Scrolls to the specified position within the child component.
     * A call to this method is only valid if the scroll pane contains
     * a child and the specified position is within legal scrolling bounds
     * of the child.  Specifying a position outside of the legal scrolling
     * bounds of the child will scroll to the closest legal position.
     * Legal bounds are defined to be the rectangle:
     * x = 0, y = 0, width = (child width - view port width),
     * height = (child height - view port height).
     * This is a convenience method which interfaces with the Adjustable
     * objects which represent the state of the scrollbars.
     * @param p the Point representing the position to scroll to
     * @throws NullPointerException if {@code p} is {@code null}
     */
    public void setScrollPosition(Point p) {
        setScrollPosition(p.x, p.y);
    }

    /**
     * Returns the current x,y position within the child which is displayed
     * at the 0,0 location of the scrolled panel's view port.
     * This is a convenience method which interfaces with the adjustable
     * objects which represent the state of the scrollbars.
     * @return the coordinate position for the current scroll position
     * @throws NullPointerException if the scrollpane does not contain
     *     a child
     */
    @Transient
    public Point getScrollPosition() {
        synchronized (getTreeLock()) {
            if (getComponentCount()==0) {
                throw new NullPointerException("child is null");
            }
            return new Point(hAdjustable.getValue(), vAdjustable.getValue());
        }
    }

    /**
     * Sets the layout manager for this container.  This method is
     * overridden to prevent the layout mgr from being set.
     * @param mgr the specified layout manager
     */
    public final void setLayout(LayoutManager mgr) {
        throw new AWTError("ScrollPane controls layout");
    }

    /**
     * Lays out this container by resizing its child to its preferred size.
     * If the new preferred size of the child causes the current scroll
     * position to be invalid, the scroll position is set to the closest
     * valid position.
     *
     * @see Component#validate
     */
    public void doLayout() {
        layout();
    }

    /**
     * Determine the size to allocate the child component.
     * If the viewport area is bigger than the preferred size
     * of the child then the child is allocated enough
     * to fill the viewport, otherwise the child is given
     * it's preferred size.
     */
    Dimension calculateChildSize() {
        //
        // calculate the view size, accounting for border but not scrollbars
        // - don't use right/bottom insets since they vary depending
        //   on whether or not scrollbars were displayed on last resize
        //
        Dimension       size = getSize();
        Insets          insets = getInsets();
        int             viewWidth = size.width - insets.left*2;
        int             viewHeight = size.height - insets.top*2;

        //
        // determine whether or not horz or vert scrollbars will be displayed
        //
        boolean vbarOn;
        boolean hbarOn;
        Component child = getComponent(0);
        Dimension childSize = new Dimension(child.getPreferredSize());

        if (scrollbarDisplayPolicy == SCROLLBARS_AS_NEEDED) {
            vbarOn = childSize.height > viewHeight;
            hbarOn = childSize.width  > viewWidth;
        } else if (scrollbarDisplayPolicy == SCROLLBARS_ALWAYS) {
            vbarOn = hbarOn = true;
        } else { // SCROLLBARS_NEVER
            vbarOn = hbarOn = false;
        }

        //
        // adjust predicted view size to account for scrollbars
        //
        int vbarWidth = getVScrollbarWidth();
        int hbarHeight = getHScrollbarHeight();
        if (vbarOn) {
            viewWidth -= vbarWidth;
        }
        if(hbarOn) {
            viewHeight -= hbarHeight;
        }

        //
        // if child is smaller than view, size it up
        //
        if (childSize.width < viewWidth) {
            childSize.width = viewWidth;
        }
        if (childSize.height < viewHeight) {
            childSize.height = viewHeight;
        }

        return childSize;
    }

    /**
     * @deprecated As of JDK version 1.1,
     * replaced by {@code doLayout()}.
     */
    @Deprecated
    public void layout() {
        if (getComponentCount()==0) {
            return;
        }
        Component c = getComponent(0);
        Point p = getScrollPosition();
        Dimension cs = calculateChildSize();
        Dimension vs = getViewportSize();

        c.reshape(- p.x, - p.y, cs.width, cs.height);
        ScrollPanePeer peer = (ScrollPanePeer)this.peer;
        if (peer != null) {
            peer.childResized(cs.width, cs.height);
        }

        // update adjustables... the viewport size may have changed
        // with the scrollbars coming or going so the viewport size
        // is updated before the adjustables.
        vs = getViewportSize();
        hAdjustable.setSpan(0, cs.width, vs.width);
        vAdjustable.setSpan(0, cs.height, vs.height);
    }

    /**
     * Prints the component in this scroll pane.
     * @param g the specified Graphics window
     * @see Component#print
     * @see Component#printAll
     */
    public void printComponents(Graphics g) {
        if (getComponentCount()==0) {
            return;
        }
        Component c = getComponent(0);
        Point p = c.getLocation();
        Dimension vs = getViewportSize();
        Insets i = getInsets();

        Graphics cg = g.create();
        try {
            cg.clipRect(i.left, i.top, vs.width, vs.height);
            cg.translate(p.x, p.y);
            c.printAll(cg);
        } finally {
            cg.dispose();
        }
    }

    /**
     * Creates the scroll pane's peer.
     */
    public void addNotify() {
        synchronized (getTreeLock()) {

            int vAdjustableValue = 0;
            int hAdjustableValue = 0;

            // Bug 4124460. Save the current adjustable values,
            // so they can be restored after addnotify. Set the
            // adjustables to 0, to prevent crashes for possible
            // negative values.
            if (getComponentCount() > 0) {
                vAdjustableValue = vAdjustable.getValue();
                hAdjustableValue = hAdjustable.getValue();
                vAdjustable.setValue(0);
                hAdjustable.setValue(0);
            }

            if (peer == null)
                peer = getComponentFactory().createScrollPane(this);
            super.addNotify();

            // Bug 4124460. Restore the adjustable values.
            if (getComponentCount() > 0) {
                vAdjustable.setValue(vAdjustableValue);
                hAdjustable.setValue(hAdjustableValue);
            }
        }
    }

    /**
     * Returns a string representing the state of this
     * {@code ScrollPane}. This
     * method is intended to be used only for debugging purposes, and the
     * content and format of the returned string may vary between
     * implementations. The returned string may be empty but may not be
     * {@code null}.
     *
     * @return the parameter string of this scroll pane
     */
    public String paramString() {
        String sdpStr;
        switch (scrollbarDisplayPolicy) {
            case SCROLLBARS_AS_NEEDED:
                sdpStr = "as-needed";
                break;
            case SCROLLBARS_ALWAYS:
                sdpStr = "always";
                break;
            case SCROLLBARS_NEVER:
                sdpStr = "never";
                break;
            default:
                sdpStr = "invalid display policy";
        }
        Point p = (getComponentCount()>0)? getScrollPosition() : new Point(0,0);
        Insets i = getInsets();
        return super.paramString()+",ScrollPosition=("+p.x+","+p.y+")"+
            ",Insets=("+i.top+","+i.left+","+i.bottom+","+i.right+")"+
            ",ScrollbarDisplayPolicy="+sdpStr+
        ",wheelScrollingEnabled="+isWheelScrollingEnabled();
    }

    void autoProcessMouseWheel(MouseWheelEvent e) {
        processMouseWheelEvent(e);
    }

    /**
     * Process mouse wheel events that are delivered to this
     * {@code ScrollPane} by scrolling an appropriate amount.
     * <p>Note that if the event parameter is {@code null}
     * the behavior is unspecified and may result in an
     * exception.
     *
     * @param e  the mouse wheel event
     * @since 1.4
     */
    protected void processMouseWheelEvent(MouseWheelEvent e) {
        if (isWheelScrollingEnabled()) {
            ScrollPaneWheelScroller.handleWheelScrolling(this, e);
            e.consume();
        }
        super.processMouseWheelEvent(e);
    }

    /**
     * If wheel scrolling is enabled, we return true for MouseWheelEvents
     * @since 1.4
     */
    protected boolean eventTypeEnabled(int type) {
        if (type == MouseEvent.MOUSE_WHEEL && isWheelScrollingEnabled()) {
            return true;
        }
        else {
            return super.eventTypeEnabled(type);
        }
    }

    /**
     * Enables/disables scrolling in response to movement of the mouse wheel.
     * Wheel scrolling is enabled by default.
     *
     * @param handleWheel   {@code true} if scrolling should be done
     *                      automatically for a MouseWheelEvent,
     *                      {@code false} otherwise.
     * @see #isWheelScrollingEnabled
     * @see java.awt.event.MouseWheelEvent
     * @see java.awt.event.MouseWheelListener
     * @since 1.4
     */
    public void setWheelScrollingEnabled(boolean handleWheel) {
        wheelScrollingEnabled = handleWheel;
    }

    /**
     * Indicates whether or not scrolling will take place in response to
     * the mouse wheel.  Wheel scrolling is enabled by default.
     *
     * @return {@code true} if the wheel scrolling enabled;
     *         otherwise {@code false}
     *
     * @see #setWheelScrollingEnabled(boolean)
     * @since 1.4
     */
    public boolean isWheelScrollingEnabled() {
        return wheelScrollingEnabled;
    }


    /**
     * Writes default serializable fields to stream.
     *
     * @param  s the {@code ObjectOutputStream} to write
     * @throws IOException if an I/O error occurs
     */
    @Serial
    private void writeObject(ObjectOutputStream s) throws IOException {
        // 4352819: We only need this degenerate writeObject to make
        // it safe for future versions of this class to write optional
        // data to the stream.
        s.defaultWriteObject();
    }

    /**
     * Reads default serializable fields to stream.
     *
     * @param  s the {@code ObjectInputStream} to read
     * @throws ClassNotFoundException if the class of a serialized object could
     *         not be found
     * @throws IOException if an I/O error occurs
     * @throws HeadlessException if {@code GraphicsEnvironment.isHeadless()}
     *         returns {@code true}
     * @see java.awt.GraphicsEnvironment#isHeadless
     */
    @Serial
    private void readObject(ObjectInputStream s)
        throws ClassNotFoundException, IOException, HeadlessException
    {
        GraphicsEnvironment.checkHeadless();
        // 4352819: Gotcha!  Cannot use s.defaultReadObject here and
        // then continue with reading optional data.  Use GetField instead.
        ObjectInputStream.GetField f = s.readFields();

        // Old fields
        scrollbarDisplayPolicy = f.get("scrollbarDisplayPolicy",
                                       SCROLLBARS_AS_NEEDED);
        hAdjustable = (ScrollPaneAdjustable)f.get("hAdjustable", null);
        vAdjustable = (ScrollPaneAdjustable)f.get("vAdjustable", null);

        // Since 1.4
        wheelScrollingEnabled = f.get("wheelScrollingEnabled",
                                      defaultWheelScroll);

//      // Note to future maintainers
//      if (f.defaulted("wheelScrollingEnabled")) {
//          // We are reading pre-1.4 stream that doesn't have
//          // optional data, not even the TC_ENDBLOCKDATA marker.
//          // Reading anything after this point is unsafe as we will
//          // read unrelated objects further down the stream (4352819).
//      }
//      else {
//          // Reading data from 1.4 or later, it's ok to try to read
//          // optional data as OptionalDataException with eof == true
//          // will be correctly reported
//      }
    }

    /**
     * Invoked when the value of the adjustable has changed.
     */
    class PeerFixer implements AdjustmentListener, java.io.Serializable
    {
        /**
         * Use serialVersionUID from JDK 1.1.1 for interoperability.
         */
        @Serial
        private static final long serialVersionUID = 1043664721353696630L;

        PeerFixer(ScrollPane scroller) {
            this.scroller = scroller;
        }

        /**
         * Invoked when the value of the adjustable has changed.
         */
        @SuppressWarnings("deprecation")
        public void adjustmentValueChanged(AdjustmentEvent e) {
            Adjustable adj = e.getAdjustable();
            int value = e.getValue();
            ScrollPanePeer peer = (ScrollPanePeer) scroller.peer;
            if (peer != null) {
                peer.setValue(adj, value);
            }

            Component c = scroller.getComponent(0);
            switch(adj.getOrientation()) {
            case Adjustable.VERTICAL:
                c.move(c.getLocation().x, -(value));
                break;
            case Adjustable.HORIZONTAL:
                c.move(-(value), c.getLocation().y);
                break;
            default:
                throw new IllegalArgumentException("Illegal adjustable orientation");
            }
        }

        private ScrollPane scroller;
    }


/////////////////
// Accessibility support
////////////////

    /**
     * Gets the AccessibleContext associated with this ScrollPane.
     * For scroll panes, the AccessibleContext takes the form of an
     * AccessibleAWTScrollPane.
     * A new AccessibleAWTScrollPane instance is created if necessary.
     *
     * @return an AccessibleAWTScrollPane that serves as the
     *         AccessibleContext of this ScrollPane
     * @since 1.3
     */
    public AccessibleContext getAccessibleContext() {
        if (accessibleContext == null) {
            accessibleContext = new AccessibleAWTScrollPane();
        }
        return accessibleContext;
    }

    /**
     * This class implements accessibility support for the
     * {@code ScrollPane} class.  It provides an implementation of the
     * Java Accessibility API appropriate to scroll pane user-interface
     * elements.
     * @since 1.3
     */
    protected class AccessibleAWTScrollPane extends AccessibleAWTContainer
    {
        /**
         * Use serialVersionUID from JDK 1.3 for interoperability.
         */
        @Serial
        private static final long serialVersionUID = 6100703663886637L;

        /**
         * Constructs an {@code AccessibleAWTScrollPane}.
         */
        protected AccessibleAWTScrollPane() {}

        /**
         * Get the role of this object.
         *
         * @return an instance of AccessibleRole describing the role of the
         * object
         * @see AccessibleRole
         */
        public AccessibleRole getAccessibleRole() {
            return AccessibleRole.SCROLL_PANE;
        }

    } // class AccessibleAWTScrollPane

}
