/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
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

package java.awt.dnd;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.datatransfer.FlavorMap;
import java.awt.datatransfer.SystemFlavorMap;
import java.awt.dnd.peer.DropTargetPeer;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.peer.ComponentPeer;
import java.awt.peer.LightweightPeer;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.io.Serializable;
import java.util.TooManyListenersException;

import javax.swing.Timer;

import sun.awt.AWTAccessor;
import sun.awt.AWTAccessor.ComponentAccessor;

/**
 * The {@code DropTarget} is associated
 * with a {@code Component} when that {@code Component}
 * wishes
 * to accept drops during Drag and Drop operations.
 * <P>
 *  Each
 * {@code DropTarget} is associated with a {@code FlavorMap}.
 * The default {@code FlavorMap} hereafter designates the
 * {@code FlavorMap} returned by {@code SystemFlavorMap.getDefaultFlavorMap()}.
 *
 * @since 1.2
 */

public class DropTarget implements DropTargetListener, Serializable {

    /**
     * Use serialVersionUID from JDK 1.4 for interoperability.
     */
    @Serial
    private static final long serialVersionUID = -6283860791671019047L;

    /**
     * Creates a new DropTarget given the {@code Component}
     * to associate itself with, an {@code int} representing
     * the default acceptable action(s) to
     * support, a {@code DropTargetListener}
     * to handle event processing, a {@code boolean} indicating
     * if the {@code DropTarget} is currently accepting drops, and
     * a {@code FlavorMap} to use (or null for the default {@code FlavorMap}).
     * <P>
     * The Component will receive drops only if it is enabled.
     * @param c         The {@code Component} with which this {@code DropTarget} is associated
     * @param ops       The default acceptable actions for this {@code DropTarget}
     * @param dtl       The {@code DropTargetListener} for this {@code DropTarget}
     * @param act       Is the {@code DropTarget} accepting drops.
     * @param fm        The {@code FlavorMap} to use, or null for the default {@code FlavorMap}
     * @exception HeadlessException if GraphicsEnvironment.isHeadless()
     *            returns true
     * @see java.awt.GraphicsEnvironment#isHeadless
     */
    public DropTarget(Component c, int ops, DropTargetListener dtl,
                      boolean act, FlavorMap fm)
        throws HeadlessException
    {
        if (GraphicsEnvironment.isHeadless()) {
            throw new HeadlessException();
        }

        component = c;

        setDefaultActions(ops);

        if (dtl != null) try {
            addDropTargetListener(dtl);
        } catch (TooManyListenersException tmle) {
            // do nothing!
        }

        if (c != null) {
            c.setDropTarget(this);
            setActive(act);
        }

        if (fm != null) {
            flavorMap = fm;
        } else {
            flavorMap = SystemFlavorMap.getDefaultFlavorMap();
        }
    }

    /**
     * Creates a {@code DropTarget} given the {@code Component}
     * to associate itself with, an {@code int} representing
     * the default acceptable action(s)
     * to support, a {@code DropTargetListener}
     * to handle event processing, and a {@code boolean} indicating
     * if the {@code DropTarget} is currently accepting drops.
     * <P>
     * The Component will receive drops only if it is enabled.
     * @param c         The {@code Component} with which this {@code DropTarget} is associated
     * @param ops       The default acceptable actions for this {@code DropTarget}
     * @param dtl       The {@code DropTargetListener} for this {@code DropTarget}
     * @param act       Is the {@code DropTarget} accepting drops.
     * @exception HeadlessException if GraphicsEnvironment.isHeadless()
     *            returns true
     * @see java.awt.GraphicsEnvironment#isHeadless
     */
    public DropTarget(Component c, int ops, DropTargetListener dtl,
                      boolean act)
        throws HeadlessException
    {
        this(c, ops, dtl, act, null);
    }

    /**
     * Creates a {@code DropTarget}.
     * @exception HeadlessException if GraphicsEnvironment.isHeadless()
     *            returns true
     * @see java.awt.GraphicsEnvironment#isHeadless
     */
    public DropTarget() throws HeadlessException {
        this(null, DnDConstants.ACTION_COPY_OR_MOVE, null, true, null);
    }

    /**
     * Creates a {@code DropTarget} given the {@code Component}
     * to associate itself with, and the {@code DropTargetListener}
     * to handle event processing.
     * <P>
     * The Component will receive drops only if it is enabled.
     * @param c         The {@code Component} with which this {@code DropTarget} is associated
     * @param dtl       The {@code DropTargetListener} for this {@code DropTarget}
     * @exception HeadlessException if GraphicsEnvironment.isHeadless()
     *            returns true
     * @see java.awt.GraphicsEnvironment#isHeadless
     */
    public DropTarget(Component c, DropTargetListener dtl)
        throws HeadlessException
    {
        this(c, DnDConstants.ACTION_COPY_OR_MOVE, dtl, true, null);
    }

    /**
     * Creates a {@code DropTarget} given the {@code Component}
     * to associate itself with, an {@code int} representing
     * the default acceptable action(s) to support, and a
     * {@code DropTargetListener} to handle event processing.
     * <P>
     * The Component will receive drops only if it is enabled.
     * @param c         The {@code Component} with which this {@code DropTarget} is associated
     * @param ops       The default acceptable actions for this {@code DropTarget}
     * @param dtl       The {@code DropTargetListener} for this {@code DropTarget}
     * @exception HeadlessException if GraphicsEnvironment.isHeadless()
     *            returns true
     * @see java.awt.GraphicsEnvironment#isHeadless
     */
    public DropTarget(Component c, int ops, DropTargetListener dtl)
        throws HeadlessException
    {
        this(c, ops, dtl, true);
    }

    /**
     * Note: this interface is required to permit the safe association
     * of a DropTarget with a Component in one of two ways, either:
     * {@code component.setDropTarget(droptarget);}
     * or {@code droptarget.setComponent(component);}
     * <P>
     * The Component will receive drops only if it is enabled.
     * @param c The new {@code Component} this {@code DropTarget}
     * is to be associated with.
     */

    public synchronized void setComponent(Component c) {
        if (component == c || component != null && component.equals(c))
            return;

        final Component old = component;

        if (old  != null) {
            clearAutoscroll();

            component = null;
            removeNotify();
            old.setDropTarget(null);

        }

        if ((component = c) != null) try {
            c.setDropTarget(this);
        } catch (Exception e) { // undo the change
            if (old != null) {
                old.setDropTarget(this);
                addNotify();
            }
        }
    }

    /**
     * Gets the {@code Component} associated
     * with this {@code DropTarget}.
     *
     * @return the current {@code Component}
     */

    public synchronized Component getComponent() {
        return component;
    }

    /**
     * Sets the default acceptable actions for this {@code DropTarget}
     *
     * @param ops the default actions
     * @see java.awt.dnd.DnDConstants
     */

    public void setDefaultActions(int ops) {
        getDropTargetContext().setTargetActions(ops & (DnDConstants.ACTION_COPY_OR_MOVE | DnDConstants.ACTION_REFERENCE));
    }

    /*
     * Called by DropTargetContext.setTargetActions()
     * with appropriate synchronization.
     */
    void doSetDefaultActions(int ops) {
        actions = ops;
    }

    /**
     * Gets an {@code int} representing the
     * current action(s) supported by this {@code DropTarget}.
     *
     * @return the current default actions
     */

    public int getDefaultActions() {
        return actions;
    }

    /**
     * Sets the DropTarget active if {@code true},
     * inactive if {@code false}.
     *
     * @param isActive sets the {@code DropTarget} (in)active.
     */

    public synchronized void setActive(boolean isActive) {
        if (isActive != active) {
            active = isActive;
        }

        if (!active) clearAutoscroll();
    }

    /**
     * Reports whether or not
     * this {@code DropTarget}
     * is currently active (ready to accept drops).
     *
     * @return {@code true} if active, {@code false} if not
     */

    public boolean isActive() {
        return active;
    }

    /**
     * Adds a new {@code DropTargetListener} (UNICAST SOURCE).
     *
     * @param dtl The new {@code DropTargetListener}
     *
     * @throws TooManyListenersException if a
     * {@code DropTargetListener} is already added to this
     * {@code DropTarget}.
     */

    public synchronized void addDropTargetListener(DropTargetListener dtl) throws TooManyListenersException {
        if (dtl == null) return;

        if (equals(dtl)) throw new IllegalArgumentException("DropTarget may not be its own Listener");

        if (dtListener == null)
            dtListener = dtl;
        else
            throw new TooManyListenersException();
    }

    /**
     * Removes the current {@code DropTargetListener} (UNICAST SOURCE).
     *
     * @param dtl the DropTargetListener to deregister.
     */

    public synchronized void removeDropTargetListener(DropTargetListener dtl) {
        if (dtl != null && dtListener != null) {
            if(dtListener.equals(dtl))
                dtListener = null;
            else
                throw new IllegalArgumentException("listener mismatch");
        }
    }

    /**
     * Calls {@code dragEnter} on the registered
     * {@code DropTargetListener} and passes it
     * the specified {@code DropTargetDragEvent}.
     * Has no effect if this {@code DropTarget}
     * is not active.
     *
     * @param dtde the {@code DropTargetDragEvent}
     *
     * @throws NullPointerException if this {@code DropTarget}
     *         is active and {@code dtde} is {@code null}
     *
     * @see #isActive
     */
    public synchronized void dragEnter(DropTargetDragEvent dtde) {
        isDraggingInside = true;

        if (!active) return;

        if (dtListener != null) {
            dtListener.dragEnter(dtde);
        } else
            dtde.getDropTargetContext().setTargetActions(DnDConstants.ACTION_NONE);

        initializeAutoscrolling(dtde.getLocation());
    }

    /**
     * Calls {@code dragOver} on the registered
     * {@code DropTargetListener} and passes it
     * the specified {@code DropTargetDragEvent}.
     * Has no effect if this {@code DropTarget}
     * is not active.
     *
     * @param dtde the {@code DropTargetDragEvent}
     *
     * @throws NullPointerException if this {@code DropTarget}
     *         is active and {@code dtde} is {@code null}
     *
     * @see #isActive
     */
    public synchronized void dragOver(DropTargetDragEvent dtde) {
        if (!active) return;

        if (dtListener != null && active) dtListener.dragOver(dtde);

        updateAutoscroll(dtde.getLocation());
    }

    /**
     * Calls {@code dropActionChanged} on the registered
     * {@code DropTargetListener} and passes it
     * the specified {@code DropTargetDragEvent}.
     * Has no effect if this {@code DropTarget}
     * is not active.
     *
     * @param dtde the {@code DropTargetDragEvent}
     *
     * @throws NullPointerException if this {@code DropTarget}
     *         is active and {@code dtde} is {@code null}
     *
     * @see #isActive
     */
    public synchronized void dropActionChanged(DropTargetDragEvent dtde) {
        if (!active) return;

        if (dtListener != null) dtListener.dropActionChanged(dtde);

        updateAutoscroll(dtde.getLocation());
    }

    /**
     * Calls {@code dragExit} on the registered
     * {@code DropTargetListener} and passes it
     * the specified {@code DropTargetEvent}.
     * Has no effect if this {@code DropTarget}
     * is not active.
     * <p>
     * This method itself does not throw any exception
     * for null parameter but for exceptions thrown by
     * the respective method of the listener.
     *
     * @param dte the {@code DropTargetEvent}
     *
     * @see #isActive
     */
    public synchronized void dragExit(DropTargetEvent dte) {
        isDraggingInside = false;

        if (!active) return;

        if (dtListener != null && active) dtListener.dragExit(dte);

        clearAutoscroll();
    }

    /**
     * Calls {@code drop} on the registered
     * {@code DropTargetListener} and passes it
     * the specified {@code DropTargetDropEvent}
     * if this {@code DropTarget} is active.
     *
     * @param dtde the {@code DropTargetDropEvent}
     *
     * @throws NullPointerException if {@code dtde} is null
     *         and at least one of the following is true: this
     *         {@code DropTarget} is not active, or there is
     *         no a {@code DropTargetListener} registered.
     *
     * @see #isActive
     */
    public synchronized void drop(DropTargetDropEvent dtde) {
        isDraggingInside = false;

        clearAutoscroll();

        if (dtListener != null && active)
            dtListener.drop(dtde);
        else { // we shouldn't get here ...
            dtde.rejectDrop();
        }
    }

    /**
     * Gets the {@code FlavorMap}
     * associated with this {@code DropTarget}.
     * If no {@code FlavorMap} has been set for this
     * {@code DropTarget}, it is associated with the default
     * {@code FlavorMap}.
     *
     * @return the FlavorMap for this DropTarget
     */

    public FlavorMap getFlavorMap() { return flavorMap; }

    /**
     * Sets the {@code FlavorMap} associated
     * with this {@code DropTarget}.
     *
     * @param fm the new {@code FlavorMap}, or null to
     * associate the default FlavorMap with this DropTarget.
     */

    public void setFlavorMap(FlavorMap fm) {
        flavorMap = fm == null ? SystemFlavorMap.getDefaultFlavorMap() : fm;
    }

    /**
     * Notify the DropTarget that it has been associated with a Component
     *
     **********************************************************************
     * This method is usually called from java.awt.Component.addNotify() of
     * the Component associated with this DropTarget to notify the DropTarget
     * that a ComponentPeer has been associated with that Component.
     *
     * Calling this method, other than to notify this DropTarget of the
     * association of the ComponentPeer with the Component may result in
     * a malfunction of the DnD system.
     **********************************************************************
     */
    public void addNotify() {
        final ComponentAccessor acc = AWTAccessor.getComponentAccessor();
        ComponentPeer peer = acc.getPeer(component);
        if (peer == null || peer == componentPeer) {
            return;
        }

        componentPeer = peer;


        for (Component c = component;
             c != null && peer instanceof LightweightPeer; c = c.getParent()) {
            peer = acc.getPeer(c);
        }

        if (peer instanceof DropTargetPeer) {
            nativePeer = (DropTargetPeer) peer;
            ((DropTargetPeer)peer).addDropTarget(this);
        } else {
            nativePeer = null;
        }
    }

    /**
     * Notify the DropTarget that it has been disassociated from a Component
     *
     **********************************************************************
     * This method is usually called from java.awt.Component.removeNotify() of
     * the Component associated with this DropTarget to notify the DropTarget
     * that a ComponentPeer has been disassociated with that Component.
     *
     * Calling this method, other than to notify this DropTarget of the
     * disassociation of the ComponentPeer from the Component may result in
     * a malfunction of the DnD system.
     **********************************************************************
     */

    public void removeNotify() {
        if (nativePeer != null) {
            nativePeer.removeDropTarget(this);
        }
        componentPeer = null;
        nativePeer = null;

        synchronized (this) {
            if (isDraggingInside) {
                dragExit(new DropTargetEvent(getDropTargetContext()));
            }
        }
    }

    /**
     * Gets the {@code DropTargetContext} associated
     * with this {@code DropTarget}.
     *
     * @return the {@code DropTargetContext} associated with this {@code DropTarget}.
     */

    public DropTargetContext getDropTargetContext() {
        return dropTargetContext;
    }

    /**
     * Creates the DropTargetContext associated with this DropTarget.
     * Subclasses may override this method to instantiate their own
     * DropTargetContext subclass.
     *
     * This call is typically *only* called by the platform's
     * DropTargetContextPeer as a drag operation encounters this
     * DropTarget. Accessing the Context while no Drag is current
     * has undefined results.
     * @return the DropTargetContext associated with this DropTarget
     */

    protected DropTargetContext createDropTargetContext() {
        return new DropTargetContext(this);
    }

    /**
     * Serializes this {@code DropTarget}. Performs default serialization,
     * and then writes out this object's {@code DropTargetListener} if and
     * only if it can be serialized. If not, {@code null} is written
     * instead.
     *
     * @param  s the {@code ObjectOutputStream} to write
     * @throws IOException if an I/O error occurs
     * @serialData The default serializable fields, in alphabetical order,
     *             followed by either a {@code DropTargetListener}
     *             instance, or {@code null}.
     * @since 1.4
     */
    @Serial
    private void writeObject(ObjectOutputStream s) throws IOException {
        s.defaultWriteObject();

        s.writeObject(SerializationTester.test(dtListener)
                      ? dtListener : null);
    }

    /**
     * Deserializes this {@code DropTarget}. This method first performs
     * default deserialization for all non-{@code transient} fields. An
     * attempt is then made to deserialize this object's
     * {@code DropTargetListener} as well. This is first attempted by
     * deserializing the field {@code dtListener}, because, in releases
     * prior to 1.4, a non-{@code transient} field of this name stored the
     * {@code DropTargetListener}. If this fails, the next object in the
     * stream is used instead.
     *
     * @param  s the {@code ObjectInputStream} to read
     * @throws ClassNotFoundException if the class of a serialized object could
     *         not be found
     * @throws IOException if an I/O error occurs
     * @since 1.4
     */
    @Serial
    private void readObject(ObjectInputStream s)
        throws ClassNotFoundException, IOException
    {
        ObjectInputStream.GetField f = s.readFields();

        try {
            dropTargetContext =
                (DropTargetContext)f.get("dropTargetContext", null);
        } catch (IllegalArgumentException e) {
            // Pre-1.4 support. 'dropTargetContext' was previously transient
        }
        if (dropTargetContext == null) {
            dropTargetContext = createDropTargetContext();
        }

        component = (Component)f.get("component", null);
        actions = f.get("actions", DnDConstants.ACTION_COPY_OR_MOVE);
        active = f.get("active", true);

        // Pre-1.4 support. 'dtListener' was previously non-transient
        try {
            dtListener = (DropTargetListener)f.get("dtListener", null);
        } catch (IllegalArgumentException e) {
            // 1.4-compatible byte stream. 'dtListener' was written explicitly
            dtListener = (DropTargetListener)s.readObject();
        }
    }

    /*********************************************************************/

    /**
     * this protected nested class implements autoscrolling
     */

    protected static class DropTargetAutoScroller implements ActionListener {

        /**
         * construct a DropTargetAutoScroller
         *
         * @param c the {@code Component}
         * @param p the {@code Point}
         */

        protected DropTargetAutoScroller(Component c, Point p) {
            super();

            component  = c;
            autoScroll = (Autoscroll)component;

            Toolkit t  = Toolkit.getDefaultToolkit();

            Integer    initial  = Integer.valueOf(100);
            Integer    interval = Integer.valueOf(100);

            try {
                initial = (Integer)t.getDesktopProperty("DnD.Autoscroll.initialDelay");
            } catch (Exception e) {
                // ignore
            }

            try {
                interval = (Integer)t.getDesktopProperty("DnD.Autoscroll.interval");
            } catch (Exception e) {
                // ignore
            }

            timer  = new Timer(interval.intValue(), this);

            timer.setCoalesce(true);
            timer.setInitialDelay(initial.intValue());

            locn = p;
            prev = p;

            try {
                hysteresis = ((Integer)t.getDesktopProperty("DnD.Autoscroll.cursorHysteresis")).intValue();
            } catch (Exception e) {
                // ignore
            }

            timer.start();
        }

        /**
         * update the geometry of the autoscroll region
         */

        @SuppressWarnings("deprecation")
        private void updateRegion() {
           Insets    i    = autoScroll.getAutoscrollInsets();
           Dimension size = component.getSize();

           if (size.width != outer.width || size.height != outer.height)
                outer.reshape(0, 0, size.width, size.height);

           if (inner.x != i.left || inner.y != i.top)
                inner.setLocation(i.left, i.top);

           int newWidth  = size.width -  (i.left + i.right);
           int newHeight = size.height - (i.top  + i.bottom);

           if (newWidth != inner.width || newHeight != inner.height)
                inner.setSize(newWidth, newHeight);

        }

        /**
         * cause autoscroll to occur
         *
         * @param newLocn the {@code Point}
         */

        protected synchronized void updateLocation(Point newLocn) {
            prev = locn;
            locn = newLocn;

            if (Math.abs(locn.x - prev.x) > hysteresis ||
                Math.abs(locn.y - prev.y) > hysteresis) {
                if (timer.isRunning()) timer.stop();
            } else {
                if (!timer.isRunning()) timer.start();
            }
        }

        /**
         * cause autoscrolling to stop
         */

        protected void stop() { timer.stop(); }

        /**
         * cause autoscroll to occur
         *
         * @param e the {@code ActionEvent}
         */

        public synchronized void actionPerformed(ActionEvent e) {
            updateRegion();

            if (outer.contains(locn) && !inner.contains(locn))
                autoScroll.autoscroll(locn);
        }

        /*
         * fields
         */

        private Component  component;
        private Autoscroll autoScroll;

        private Timer      timer;

        private Point      locn;
        private Point      prev;

        private Rectangle  outer = new Rectangle();
        private Rectangle  inner = new Rectangle();

        private int        hysteresis = 10;
    }

    /*********************************************************************/

    /**
     * create an embedded autoscroller
     *
     * @param c the {@code Component}
     * @param p the {@code Point}
     * @return an embedded autoscroller
     */

    protected DropTargetAutoScroller createDropTargetAutoScroller(Component c, Point p) {
        return new DropTargetAutoScroller(c, p);
    }

    /**
     * initialize autoscrolling
     *
     * @param p the {@code Point}
     */

    protected void initializeAutoscrolling(Point p) {
        if (component == null || !(component instanceof Autoscroll)) return;

        autoScroller = createDropTargetAutoScroller(component, p);
    }

    /**
     * update autoscrolling with current cursor location
     *
     * @param dragCursorLocn the {@code Point}
     */

    protected void updateAutoscroll(Point dragCursorLocn) {
        if (autoScroller != null) autoScroller.updateLocation(dragCursorLocn);
    }

    /**
     * clear autoscrolling
     */

    protected void clearAutoscroll() {
        if (autoScroller != null) {
            autoScroller.stop();
            autoScroller = null;
        }
    }

    /**
     * The DropTargetContext associated with this DropTarget.
     *
     * @serial
     */
    private DropTargetContext dropTargetContext = createDropTargetContext();

    /**
     * The Component associated with this DropTarget.
     *
     * @serial
     */
    private Component component;

    /*
     * That Component's  Peer
     */
    private transient ComponentPeer componentPeer;

    /*
     * That Component's "native" Peer
     */
    private transient DropTargetPeer nativePeer;


    /**
     * Default permissible actions supported by this DropTarget.
     *
     * @see #setDefaultActions
     * @see #getDefaultActions
     * @serial
     */
    int     actions = DnDConstants.ACTION_COPY_OR_MOVE;

    /**
     * {@code true} if the DropTarget is accepting Drag &amp; Drop operations.
     *
     * @serial
     */
    boolean active = true;

    /*
     * the auto scrolling object
     */

    private transient DropTargetAutoScroller autoScroller;

    /*
     * The delegate
     */

    private transient DropTargetListener dtListener;

    /*
     * The FlavorMap
     */

    private transient FlavorMap flavorMap;

    /*
     * If the dragging is currently inside this drop target
     */
    private transient boolean isDraggingInside;
}
