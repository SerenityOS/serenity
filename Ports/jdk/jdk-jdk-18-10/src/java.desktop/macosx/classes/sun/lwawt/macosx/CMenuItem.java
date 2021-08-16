/*
 * Copyright (c) 2011, 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.MenuItem;
import java.awt.MenuShortcut;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.peer.MenuItemPeer;
import java.util.concurrent.atomic.AtomicBoolean;

import sun.awt.SunToolkit;
import sun.lwawt.LWToolkit;

public class CMenuItem extends CMenuComponent implements MenuItemPeer {

    private final AtomicBoolean enabled = new AtomicBoolean(true);

    public CMenuItem(MenuItem target) {
        super(target);
        initialize(target);
    }

    // This way we avoiding invocation of the setters twice
    protected void initialize(MenuItem target) {
        if (!isSeparator()) {
            setLabel(target.getLabel());
            setEnabled(target.isEnabled());
        }
    }

    private boolean isSeparator() {
        String label = ((MenuItem)getTarget()).getLabel();
        return (label != null && label.equals("-"));
    }

    @Override
    long createModel() {
        CMenuComponent parent = (CMenuComponent)LWToolkit.targetToPeer(getTarget().getParent());
        return parent.executeGet(ptr->nativeCreate(ptr, isSeparator()));
    }
    @SuppressWarnings("deprecation")
    public void setLabel(String label, char keyChar, int keyCode, int modifiers) {
        int keyMask = modifiers;
        if (keyCode == KeyEvent.VK_UNDEFINED) {
            MenuShortcut shortcut = ((MenuItem)getTarget()).getShortcut();

            if (shortcut != null) {
                keyCode = shortcut.getKey();
                keyMask |= InputEvent.META_MASK;

                if (shortcut.usesShiftModifier()) {
                    keyMask |= InputEvent.SHIFT_MASK;
                }
            }
        }

        if (label == null) {
            label = "";
        }

        // <rdar://problem/3654824>
        // Native code uses a keyChar of 0 to indicate that the
        // keyCode should be used to generate the shortcut.  Translate
        // CHAR_UNDEFINED into 0.
        if (keyChar == KeyEvent.CHAR_UNDEFINED) {
            keyChar = 0;
        }

        final String finalLabel = label;
        final char finalKeyChar = keyChar;
        final int finalKeyCode = keyCode;
        final int finalKeyMask = keyMask;
        execute(ptr -> nativeSetLabel(ptr, finalLabel, finalKeyChar,
                                      finalKeyCode, finalKeyMask));
    }

    @Override
    public void setLabel(String label) {
        setLabel(label, (char)0, KeyEvent.VK_UNDEFINED, 0);
    }

    /**
     * This is new API that we've added to AWT menu items
     * because AWT menu items are used for Swing screen menu bars
     * and we want to support the NSMenuItem image apis.
     * There isn't a need to expose this except in a instanceof because
     * it isn't defined in the peer api.
     */
    public final void setImage(final java.awt.Image img) {
        CImage cimg = CImage.getCreator().createFromImage(img);
        execute(ptr -> {
            if (cimg == null) {
                nativeSetImage(ptr, 0L);
            } else {
                cimg.execute(imgPtr -> nativeSetImage(ptr, imgPtr));
            }
        });
    }

    /**
     * New API for tooltips
     */
    public final void setToolTipText(final String text) {
        execute(ptr -> nativeSetTooltip(ptr, text));
    }

    public final boolean isEnabled() {
        return enabled.get();
    }

    @Override
    public void setEnabled(boolean b) {
        final Object parent = LWToolkit.targetToPeer(getTarget().getParent());
        if (parent instanceof CMenuItem) {
            b &= ((CMenuItem) parent).isEnabled();
        }
        if (enabled.compareAndSet(!b, b)) {
            final boolean finalB = b;
            execute(ptr->nativeSetEnabled(ptr, finalB));
        }
    }

    private native long nativeCreate(long parentMenu, boolean isSeparator);
    private native void nativeSetLabel(long modelPtr, String label, char keyChar, int keyCode, int modifiers);
    private native void nativeSetImage(long modelPtr, long image);
    private native void nativeSetTooltip(long modelPtr, String text);
    private native void nativeSetEnabled(long modelPtr, boolean b);

    // native callbacks
    void handleAction(final long when, final int modifiers) {
        SunToolkit.executeOnEventHandlerThread(getTarget(), new Runnable() {
            public void run() {
                final String cmd = ((MenuItem)getTarget()).getActionCommand();
                final ActionEvent event = new ActionEvent(getTarget(), ActionEvent.ACTION_PERFORMED, cmd, when, modifiers);
                SunToolkit.postEvent(SunToolkit.targetToAppContext(getTarget()), event);
            }
        });
    }
}
