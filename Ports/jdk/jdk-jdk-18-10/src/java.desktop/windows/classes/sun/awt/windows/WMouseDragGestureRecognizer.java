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

package sun.awt.windows;

import java.awt.Component;
import java.awt.Point;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragSource;
import java.awt.dnd.MouseDragGestureRecognizer;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.io.Serial;

import sun.awt.dnd.SunDragSourceContextPeer;

/**
 * <p>
 * This subclass of MouseDragGestureRecognizer defines a DragGestureRecognizer
 * for Mouse based gestures on Win32.
 * </p>
 *
 * @author Laurence P. G. Cable
 *
 * @see java.awt.dnd.DragGestureListener
 * @see java.awt.dnd.DragGestureEvent
 * @see java.awt.dnd.DragSource
 */

final class WMouseDragGestureRecognizer extends MouseDragGestureRecognizer {

    /**
     * Use serialVersionUID from JDK 1.4 for interoperability.
     */
    @Serial
    private static final long serialVersionUID = -3527844310018033570L;

    /*
     * constant for number of pixels hysterisis before drag is determined
     * to have started
     */

    protected static int motionThreshold;

    protected static final int ButtonMask = InputEvent.BUTTON1_DOWN_MASK |
                                            InputEvent.BUTTON2_DOWN_MASK |
                                            InputEvent.BUTTON3_DOWN_MASK;

    /**
     * construct a new WMouseDragGestureRecognizer
     *
     * @param ds  The DragSource for the Component c
     * @param c   The Component to observe
     * @param act The actions permitted for this Drag
     * @param dgl The DragGestureRecognizer to notify when a gesture is detected
     *
     */

    protected WMouseDragGestureRecognizer(DragSource ds, Component c, int act, DragGestureListener dgl) {
        super(ds, c, act, dgl);
    }

    /**
     * construct a new WMouseDragGestureRecognizer
     *
     * @param ds  The DragSource for the Component c
     * @param c   The Component to observe
     * @param act The actions permitted for this Drag
     */

    protected WMouseDragGestureRecognizer(DragSource ds, Component c, int act) {
        this(ds, c, act, null);
    }

    /**
     * construct a new WMouseDragGestureRecognizer
     *
     * @param ds  The DragSource for the Component c
     * @param c   The Component to observe
     */

    protected WMouseDragGestureRecognizer(DragSource ds, Component c) {
        this(ds, c, DnDConstants.ACTION_NONE);
    }

    /**
     * construct a new WMouseDragGestureRecognizer
     *
     * @param ds  The DragSource for the Component c
     */

    protected WMouseDragGestureRecognizer(DragSource ds) {
        this(ds, null);
    }

    /**
     * determine the drop action from the event
     */

    protected int mapDragOperationFromModifiers(MouseEvent e) {
        int mods = e.getModifiersEx();
        int btns = mods & ButtonMask;

        // Prohibit multi-button drags.
        if (!(btns == InputEvent.BUTTON1_DOWN_MASK ||
              btns == InputEvent.BUTTON2_DOWN_MASK ||
              btns == InputEvent.BUTTON3_DOWN_MASK)) {
            return DnDConstants.ACTION_NONE;
        }

        return
            SunDragSourceContextPeer.convertModifiersToDropAction(mods,
                                                                  getSourceActions());
    }

    /**
     * Invoked when the mouse has been clicked on a component.
     */

    @Override
    public void mouseClicked(MouseEvent e) {
        // do nothing
    }

    /**
     * Invoked when a mouse button has been pressed on a component.
     */

    @Override
    public void mousePressed(MouseEvent e) {
        events.clear();

        if (mapDragOperationFromModifiers(e) != DnDConstants.ACTION_NONE) {
            try {
                motionThreshold = DragSource.getDragThreshold();
            } catch (Exception exc) {
                motionThreshold = 5;
            }
            appendEvent(e);
        }
    }

    /**
     * Invoked when a mouse button has been released on a component.
     */

    @Override
    public void mouseReleased(MouseEvent e) {
        events.clear();
    }

    /**
     * Invoked when the mouse enters a component.
     */

    @Override
    public void mouseEntered(MouseEvent e) {
        events.clear();
    }

    /**
     * Invoked when the mouse exits a component.
     */

    @Override
    public void mouseExited(MouseEvent e) {

        if (!events.isEmpty()) { // gesture pending
            int dragAction = mapDragOperationFromModifiers(e);

            if (dragAction == DnDConstants.ACTION_NONE) {
                events.clear();
            }
        }
    }

    /**
     * Invoked when a mouse button is pressed on a component.
     */

    @Override
    public void mouseDragged(MouseEvent e) {
        if (!events.isEmpty()) { // gesture pending
            int dop = mapDragOperationFromModifiers(e);

            if (dop == DnDConstants.ACTION_NONE) {
                return;
            }

            MouseEvent trigger = (MouseEvent)events.get(0);


            Point      origin  = trigger.getPoint();
            Point      current = e.getPoint();

            int        dx      = Math.abs(origin.x - current.x);
            int        dy      = Math.abs(origin.y - current.y);

            if (dx > motionThreshold || dy > motionThreshold) {
                fireDragGestureRecognized(dop, ((MouseEvent)getTriggerEvent()).getPoint());
            } else
                appendEvent(e);
        }
    }

    /**
     * Invoked when the mouse button has been moved on a component
     * (with no buttons no down).
     */

    @Override
    public void mouseMoved(MouseEvent e) {
        // do nothing
    }
}
