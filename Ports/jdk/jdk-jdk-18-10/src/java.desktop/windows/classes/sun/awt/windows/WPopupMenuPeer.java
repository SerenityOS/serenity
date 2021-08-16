/*
 * Copyright (c) 1996, 2016, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.*;
import java.awt.peer.*;

import sun.awt.AWTAccessor;

final class WPopupMenuPeer extends WMenuPeer implements PopupMenuPeer {
    // We can't use target.getParent() for TrayIcon popup
    // because this method should return null for the TrayIcon
    // popup regardless of that whether it has parent or not.

    WPopupMenuPeer(PopupMenu target) {
        this.target = target;
        MenuContainer parent = null;

        // We can't use target.getParent() for TrayIcon popup
        // because this method should return null for the TrayIcon
        // popup regardless of that whether it has parent or not.
        boolean isTrayIconPopup = AWTAccessor.getPopupMenuAccessor().isTrayIconPopup(target);
        if (isTrayIconPopup) {
            parent = AWTAccessor.getMenuComponentAccessor().getParent(target);
        } else {
            parent = target.getParent();
        }

        if (parent instanceof Component) {
            WComponentPeer parentPeer = (WComponentPeer) WToolkit.targetToPeer(parent);
            if (parentPeer == null) {
                // because the menu isn't a component (sigh) we first have to wait
                // for a failure to map the peer which should only happen for a
                // lightweight container, then find the actual native parent from
                // that component.
                parent = WToolkit.getNativeContainer((Component)parent);
                parentPeer = (WComponentPeer) WToolkit.targetToPeer(parent);
            }
            parentPeer.addChildPeer(this);
            createMenu(parentPeer);
            // fix for 5088782: check if menu object is created successfully
            checkMenuCreation();
        } else {
            throw new IllegalArgumentException(
                "illegal popup menu container class");
        }
    }

    private native void createMenu(WComponentPeer parent);

    @SuppressWarnings("deprecation")
    public void show(Event e) {
        Component origin = (Component)e.target;
        WComponentPeer peer = (WComponentPeer) WToolkit.targetToPeer(origin);
        if (peer == null) {
            // A failure to map the peer should only happen for a
            // lightweight component, then find the actual native parent from
            // that component.  The event coorinates are going to have to be
            // remapped as well.
            Component nativeOrigin = WToolkit.getNativeContainer(origin);
            e.target = nativeOrigin;

            // remove the event coordinates
            for (Component c = origin; c != nativeOrigin; c = c.getParent()) {
                Point p = c.getLocation();
                e.x += p.x;
                e.y += p.y;
            }
        }
        _show(e);
    }

    /*
     * This overloaded method is for TrayIcon.
     * Its popup has special parent.
     */
    void show(Component origin, Point p) {
        WComponentPeer peer = (WComponentPeer) WToolkit.targetToPeer(origin);
        @SuppressWarnings("deprecation")
        Event e = new Event(origin, 0, Event.MOUSE_DOWN, p.x, p.y, 0, 0);
        if (peer == null) {
            Component nativeOrigin = WToolkit.getNativeContainer(origin);
            e.target = nativeOrigin;
        }
        e.x = p.x;
        e.y = p.y;
        _show(e);
    }

    @SuppressWarnings("deprecation")
    private native void _show(Event e);
}
