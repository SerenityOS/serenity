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
package sun.awt.windows;

import java.util.ResourceBundle;
import java.util.MissingResourceException;
import java.awt.*;
import java.awt.peer.*;
import java.awt.event.ActionEvent;
import java.security.AccessController;
import java.security.PrivilegedAction;
import sun.util.logging.PlatformLogger;

@SuppressWarnings("removal")
class WMenuItemPeer extends WObjectPeer implements MenuItemPeer {
    private static final PlatformLogger log = PlatformLogger.getLogger("sun.awt.WMenuItemPeer");

    static {
        initIDs();
    }

    String shortcutLabel;
    //WMenuBarPeer extends WMenuPeer
    //so parent is always instanceof WMenuPeer
    protected WMenuPeer parent;

    // MenuItemPeer implementation

    private synchronized native void _dispose();
    protected void disposeImpl() {
        WToolkit.targetDisposedPeer(target, this);
        _dispose();
    }

    public void setEnabled(boolean b) {
        enable(b);
    }

    private void readShortcutLabel() {
        //Fix for 6288578: PIT. Windows: Shortcuts displayed for the menuitems in a popup menu
        WMenuPeer ancestor = parent;
        while (ancestor != null && !(ancestor instanceof WMenuBarPeer)) {
            ancestor = ancestor.parent;
        }
        if (ancestor instanceof WMenuBarPeer) {
            MenuShortcut sc = ((MenuItem)target).getShortcut();
            shortcutLabel = (sc != null) ? sc.toString() : null;
        } else {
            shortcutLabel = null;
        }
    }

    public void setLabel(String label) {
        //Fix for 6288578: PIT. Windows: Shortcuts displayed for the menuitems in a popup menu
        readShortcutLabel();
        _setLabel(label);
    }
    public native void _setLabel(String label);

    // Toolkit & peer internals

    private final boolean isCheckbox;

    protected WMenuItemPeer() {
        isCheckbox = false;
    }
    WMenuItemPeer(MenuItem target) {
        this(target, false);
    }

    WMenuItemPeer(MenuItem target, boolean isCheckbox) {
        this.target = target;
        this.parent = (WMenuPeer) WToolkit.targetToPeer(target.getParent());
        this.isCheckbox = isCheckbox;
        parent.addChildPeer(this);
        create(parent);
        // fix for 5088782: check if menu object is created successfully
        checkMenuCreation();
        //Fix for 6288578: PIT. Windows: Shortcuts displayed for the menuitems in a popup menu
        readShortcutLabel();
    }

    void checkMenuCreation()
    {
        // fix for 5088782: check if menu peer is created successfully
        if (pData == 0)
        {
            if (createError != null)
            {
                throw createError;
            }
            else
            {
                throw new InternalError("couldn't create menu peer");
            }
        }

    }

    /*
     * Post an event. Queue it for execution by the callback thread.
     */
    void postEvent(AWTEvent event) {
        WToolkit.postEvent(WToolkit.targetToAppContext(target), event);
    }

    native void create(WMenuPeer parent);

    native void enable(boolean e);

    // native callbacks

    void handleAction(final long when, final int modifiers) {
        WToolkit.executeOnEventHandlerThread(target, new Runnable() {
            public void run() {
                postEvent(new ActionEvent(target, ActionEvent.ACTION_PERFORMED,
                                          ((MenuItem)target).
                                              getActionCommand(), when,
                                          modifiers));
            }
        });
    }

    private static Font defaultMenuFont;

    static {
        defaultMenuFont = AccessController.doPrivileged(
            new PrivilegedAction <Font> () {
                public Font run() {
                    try {
                        ResourceBundle rb = ResourceBundle.getBundle("sun.awt.windows.awtLocalization");
                        return Font.decode(rb.getString("menuFont"));
                    } catch (MissingResourceException e) {
                        if (log.isLoggable(PlatformLogger.Level.FINE)) {
                            log.fine("WMenuItemPeer: " + e.getMessage()+". Using default MenuItem font.", e);
                        }
                        return new Font("SanSerif", Font.PLAIN, 11);
                    }
                }
            });
    }

    static Font getDefaultFont() {
        return defaultMenuFont;
    }

    /**
     * Initialize JNI field and method IDs
     */
    private static native void initIDs();

    private native void _setFont(Font f);

    public void setFont(final Font f) {
        _setFont(f);
    }
}
