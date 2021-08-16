/*
 * Copyright (c) 1998, 2021, Oracle and/or its affiliates. All rights reserved.
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
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.io.Serial;

/**
 * This abstract subclass of {@code DragGestureRecognizer}
 * defines a {@code DragGestureRecognizer}
 * for mouse-based gestures.
 *
 * Each platform implements its own concrete subclass of this class,
 * available via the Toolkit.createDragGestureRecognizer() method,
 * to encapsulate
 * the recognition of the platform dependent mouse gesture(s) that initiate
 * a Drag and Drop operation.
 * <p>
 * Mouse drag gesture recognizers should honor the
 * drag gesture motion threshold, available through
 * {@link DragSource#getDragThreshold}.
 * A drag gesture should be recognized only when the distance
 * in either the horizontal or vertical direction between
 * the location of the latest mouse dragged event and the
 * location of the corresponding mouse button pressed event
 * is greater than the drag gesture motion threshold.
 * <p>
 * Drag gesture recognizers created with
 * {@link DragSource#createDefaultDragGestureRecognizer}
 * follow this convention.
 *
 * @author Laurence P. G. Cable
 *
 * @see java.awt.dnd.DragGestureListener
 * @see java.awt.dnd.DragGestureEvent
 * @see java.awt.dnd.DragSource
 */

public abstract class MouseDragGestureRecognizer extends DragGestureRecognizer implements MouseListener, MouseMotionListener {

    /**
     * Use serialVersionUID from JDK 1.4 for interoperability.
     */
    @Serial
    private static final long serialVersionUID = 6220099344182281120L;

    /**
     * Construct a new {@code MouseDragGestureRecognizer}
     * given the {@code DragSource} for the
     * {@code Component} c, the {@code Component}
     * to observe, the action(s)
     * permitted for this drag operation, and
     * the {@code DragGestureListener} to
     * notify when a drag gesture is detected.
     *
     * @param ds  The DragSource for the Component c
     * @param c   The Component to observe
     * @param act The actions permitted for this Drag
     * @param dgl The DragGestureListener to notify when a gesture is detected
     *
     */

    protected MouseDragGestureRecognizer(DragSource ds, Component c, int act, DragGestureListener dgl) {
        super(ds, c, act, dgl);
    }

    /**
     * Construct a new {@code MouseDragGestureRecognizer}
     * given the {@code DragSource} for
     * the {@code Component} c,
     * the {@code Component} to observe, and the action(s)
     * permitted for this drag operation.
     *
     * @param ds  The DragSource for the Component c
     * @param c   The Component to observe
     * @param act The actions permitted for this drag
     */

    protected MouseDragGestureRecognizer(DragSource ds, Component c, int act) {
        this(ds, c, act, null);
    }

    /**
     * Construct a new {@code MouseDragGestureRecognizer}
     * given the {@code DragSource} for the
     * {@code Component} c, and the
     * {@code Component} to observe.
     *
     * @param ds  The DragSource for the Component c
     * @param c   The Component to observe
     */

    protected MouseDragGestureRecognizer(DragSource ds, Component c) {
        this(ds, c, DnDConstants.ACTION_NONE);
    }

    /**
     * Construct a new {@code MouseDragGestureRecognizer}
     * given the {@code DragSource} for the {@code Component}.
     *
     * @param ds  The DragSource for the Component
     */

    protected MouseDragGestureRecognizer(DragSource ds) {
        this(ds, null);
    }

    /**
     * register this DragGestureRecognizer's Listeners with the Component
     */

    protected void registerListeners() {
        component.addMouseListener(this);
        component.addMouseMotionListener(this);
    }

    /**
     * unregister this DragGestureRecognizer's Listeners with the Component
     *
     * subclasses must override this method
     */


    protected void unregisterListeners() {
        component.removeMouseListener(this);
        component.removeMouseMotionListener(this);
    }

    /**
     * Invoked when the mouse has been clicked on a component.
     *
     * @param e the {@code MouseEvent}
     */

    public void mouseClicked(MouseEvent e) { }

    /**
     * Invoked when a mouse button has been
     * pressed on a {@code Component}.
     *
     * @param e the {@code MouseEvent}
     */

    public void mousePressed(MouseEvent e) { }

    /**
     * Invoked when a mouse button has been released on a component.
     *
     * @param e the {@code MouseEvent}
     */

    public void mouseReleased(MouseEvent e) { }

    /**
     * Invoked when the mouse enters a component.
     *
     * @param e the {@code MouseEvent}
     */

    public void mouseEntered(MouseEvent e) { }

    /**
     * Invoked when the mouse exits a component.
     *
     * @param e the {@code MouseEvent}
     */

    public void mouseExited(MouseEvent e) { }

    /**
     * Invoked when a mouse button is pressed on a component.
     *
     * @param e the {@code MouseEvent}
     */

    public void mouseDragged(MouseEvent e) { }

    /**
     * Invoked when the mouse button has been moved on a component
     * (with no buttons no down).
     *
     * @param e the {@code MouseEvent}
     */

    public void mouseMoved(MouseEvent e) { }
}
