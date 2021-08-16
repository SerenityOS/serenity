/*
 * Copyright (c) 2011, 2014, Oracle and/or its affiliates. All rights reserved.
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

package sun.lwawt.macosx;


import java.awt.*;
import java.awt.dnd.*;
import java.awt.event.*;

import sun.awt.dnd.SunDragSourceContextPeer;

@SuppressWarnings("serial") // JDK implementation class
class CMouseDragGestureRecognizer extends MouseDragGestureRecognizer {

      // Number of pixels before drag is determined to have started:
    private static final int fMotionThreshold = getMotionThreshold();

    // Default is the Aqua-approved value:
    private static final int kDefaultMotionThreshold = 3;

    private static int getMotionThreshold() {
        try {
            return ((Integer)Toolkit.getDefaultToolkit().getDesktopProperty("DnD.gestureMotionThreshold")).intValue();
        } catch (Exception e) {
            return kDefaultMotionThreshold;
        }
    }

    protected static final int ButtonMask = InputEvent.BUTTON1_DOWN_MASK |
                                            InputEvent.BUTTON2_DOWN_MASK |
                                            InputEvent.BUTTON3_DOWN_MASK;

    protected CMouseDragGestureRecognizer(DragSource ds, Component c, int act, DragGestureListener dgl) {
        super(ds, c, act, dgl);
    }

    protected CMouseDragGestureRecognizer(DragSource ds, Component c, int act) {
        this(ds, c, act, null);
    }

    protected CMouseDragGestureRecognizer(DragSource ds, Component c) {
        this(ds, c, DnDConstants.ACTION_NONE);
    }

    protected CMouseDragGestureRecognizer(DragSource ds) {
        this(ds, null);
    }

    // Determine the drop action from the event:
    protected int mapDragOperationFromModifiers(MouseEvent e) {
        int mods = e.getModifiersEx();
        int btns = mods & ButtonMask;

        // 8-29-02 VL: this shouldn't apply to OS X but let's leave this commented out until verified:
        // Do not allow right mouse button drag since Motif DnD does not terminate drag operation on right mouse button release.
        //if (!(btns == InputEvent.BUTTON1_DOWN_MASK || btns == InputEvent.BUTTON2_DOWN_MASK)) {
        //    return DnDConstants.ACTION_NONE;
        //}

        return SunDragSourceContextPeer.convertModifiersToDropAction(mods, getSourceActions());
    }

    // Invoked when the mouse has been clicked on a component:
    public void mouseClicked(MouseEvent e) {
        // do nothing
    }

    // Invoked when a mouse button has been pressed on a component:
    public void mousePressed(MouseEvent e) {
        events.clear();

        if (mapDragOperationFromModifiers(e) != DnDConstants.ACTION_NONE) {
            appendEvent(e);
        }
    }

    // Invoked when a mouse button has been released over a component:
    public void mouseReleased(MouseEvent e) {
        events.clear();
    }

    // Invoked when the mouse enters a component:
    public void mouseEntered(MouseEvent e) {
        events.clear();
    }

    // Invoked when the mouse exits a component:
    public void mouseExited(MouseEvent e) {
        if (!events.isEmpty()) { // gesture pending
            int dragAction = mapDragOperationFromModifiers(e);

            if (dragAction == DnDConstants.ACTION_NONE) {
                events.clear();
            }
        }
    }

    // Invoked when a mouse button is pressed on a component:
    public void mouseDragged(MouseEvent e) {
        if (!events.isEmpty()) { // gesture pending
            int dop = mapDragOperationFromModifiers(e);

            if (dop == DnDConstants.ACTION_NONE) {
                return;
            }

            MouseEvent trigger = (MouseEvent) events.get(0);

            Point      origin  = trigger.getPoint();
            Point      current = e.getPoint();

            int        dx      = Math.abs(origin.x - current.x);
            int        dy      = Math.abs(origin.y - current.y);

            if (dx >= fMotionThreshold || dy >= fMotionThreshold) {
                fireDragGestureRecognized(dop, ((MouseEvent)getTriggerEvent()).getPoint());
            } else {
                appendEvent(e);
            }
        }
    }

    // Invoked when the mouse button has been moved on a component (with no buttons no down):
    public void mouseMoved(MouseEvent e) {
        // do nothing
    }
}
