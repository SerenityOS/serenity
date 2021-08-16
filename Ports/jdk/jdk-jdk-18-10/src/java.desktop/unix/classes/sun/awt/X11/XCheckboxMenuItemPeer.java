/*
 * Copyright (c) 2003, 2016, Oracle and/or its affiliates. All rights reserved.
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

package sun.awt.X11;

import java.awt.*;
import java.awt.peer.*;
import java.awt.event.*;

import sun.awt.AWTAccessor;

final class XCheckboxMenuItemPeer extends XMenuItemPeer
        implements CheckboxMenuItemPeer {

    /************************************************
     *
     * Construction
     *
     ************************************************/
    XCheckboxMenuItemPeer(CheckboxMenuItem target) {
        super(target);
    }

    /************************************************
     *
     * Implementaion of interface methods
     *
     ************************************************/

    //Prom CheckboxMenuItemtPeer
    @Override
    public void setState(boolean t) {
        repaintIfShowing();
    }

    /************************************************
     *
     * Access to target's fields
     *
     ************************************************/
    boolean getTargetState() {
        return AWTAccessor.getCheckboxMenuItemAccessor()
                   .getState((CheckboxMenuItem)getTarget());
    }

    /************************************************
     *
     * Utility functions
     *
     ************************************************/

    /**
     * Toggles state and generates ItemEvent
     */
    @Override
    void action(long when, int modifiers) {
        XToolkit.executeOnEventHandlerThread((CheckboxMenuItem)getTarget(), new Runnable() {
                @Override
                public void run() {
                    doToggleState(when);
                }
            });
    }


    /************************************************
     *
     * Private
     *
     ************************************************/
    private void doToggleState(long when) {
        CheckboxMenuItem cb = (CheckboxMenuItem)getTarget();
        boolean newState = !getTargetState();
        cb.setState(newState);
        ItemEvent e = new ItemEvent(cb,
                                    ItemEvent.ITEM_STATE_CHANGED,
                                    getTargetLabel(),
                                    getTargetState() ? ItemEvent.SELECTED : ItemEvent.DESELECTED);
        XWindow.postEventStatic(e);
        //WToolkit does not post ActionEvent when clicking on menu item
        //MToolkit _does_ post.
        //Fix for 5005195 MAWT: CheckboxMenuItem fires action events
        //Events should not be fired
        //XWindow.postEventStatic(new ActionEvent(cb, ActionEvent.ACTION_PERFORMED,
        //                                        getTargetActionCommand(), when,
        //                                        0));
    }

} // class XCheckboxMenuItemPeer
