/*
 * Copyright (c) 2011, 2016, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.CheckboxMenuItem;
import java.awt.event.ItemEvent;
import java.awt.peer.CheckboxMenuItemPeer;

import sun.awt.SunToolkit;

public class CCheckboxMenuItem extends CMenuItem implements CheckboxMenuItemPeer {
    volatile boolean fAutoToggle = true;
    volatile boolean fIsIndeterminate = false;

    private native void nativeSetState(long modelPtr, boolean state);
    private native void nativeSetIsCheckbox(long modelPtr);

    CCheckboxMenuItem(final CheckboxMenuItem target) {
        super(target);
        execute(this::nativeSetIsCheckbox);
        setState(target.getState());
    }

    // MenuItemPeer implementation
    @Override
    public void setState(final boolean state) {
        execute(ptr -> nativeSetState(ptr, state));
    }

    public void handleAction(final boolean state) {
        final CheckboxMenuItem target = (CheckboxMenuItem)getTarget();
        SunToolkit.executeOnEventHandlerThread(target, new Runnable() {
            public void run() {
                target.setState(state);
            }
        });
        ItemEvent event = new ItemEvent(target, ItemEvent.ITEM_STATE_CHANGED, target.getLabel(), state ? ItemEvent.SELECTED : ItemEvent.DESELECTED);
        SunToolkit.postEvent(SunToolkit.targetToAppContext(getTarget()), event);
    }

    public void setIsIndeterminate(final boolean indeterminate) {
        fIsIndeterminate = indeterminate;
    }

    private boolean isAutoToggle() {
        return fAutoToggle;
    }

    public void setAutoToggle(boolean b) {
        fAutoToggle = b;
    }
}
