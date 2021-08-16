/*
 * Copyright (c) 1997, 2014, Oracle and/or its affiliates. All rights reserved.
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

package javax.swing.plaf;

import java.awt.event.MouseEvent;
import javax.swing.Popup;
import javax.swing.PopupFactory;
import javax.swing.JPopupMenu;

/**
 * Pluggable look and feel interface for JPopupMenu.
 *
 * @author Georges Saab
 * @author David Karlton
 */

public abstract class PopupMenuUI extends ComponentUI {
    /**
     * Constructor for subclasses to call.
     */
    protected PopupMenuUI() {}

    /**
     * Returns whether or not the given {@code MouseEvent} is the popup menu
     * trigger event for the platform
     *
     * @param e a {@code MouseEvent}
     * @return true if the {@code MouseEvent e} is the popup menu trigger
     * @since 1.3
     */
    public boolean isPopupTrigger(MouseEvent e) {
        return e.isPopupTrigger();
    }

    /**
     * Returns the <code>Popup</code> that will be responsible for
     * displaying the <code>JPopupMenu</code>.
     *
     * @param popup JPopupMenu requesting Popup
     * @param x     Screen x location Popup is to be shown at
     * @param y     Screen y location Popup is to be shown at.
     * @return Popup that will show the JPopupMenu
     * @since 1.4
     */
    public Popup getPopup(JPopupMenu popup, int x, int y) {
        PopupFactory popupFactory = PopupFactory.getSharedInstance();

        return popupFactory.getPopup(popup.getInvoker(), popup, x, y);
    }
}
