/*
 * Copyright (c) 2001, 2017, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.java.swing.plaf.windows;

import javax.swing.UIManager;

import sun.swing.plaf.DesktopProperty;

/**
 * Wrapper for a value from the desktop. The value is lazily looked up, and
 * can be accessed using the <code>UIManager.ActiveValue</code> method
 * <code>createValue</code>. If the underlying desktop property changes this
 * will force the UIs to update all known Frames. You can invoke
 * <code>invalidate</code> to force the value to be fetched again.
 */
public class WindowsDesktopProperty extends DesktopProperty {

    /**
     * Updates the UIs of all the known Frames.
     */
    @Override
    protected final void updateAllUIs() {
        // Check if the current UI is WindowsLookAndfeel and flush the XP style map.
        // Note: Change the package test if this class is moved to a different package.
        Class<?> uiClass = UIManager.getLookAndFeel().getClass();
        if (uiClass.getPackage().equals(WindowsDesktopProperty.class.getPackage())) {
            XPStyle.invalidateStyle();
        }
        super.updateAllUIs();
    }

    /**
     * Creates a WindowsDesktopProperty.
     *
     * @param key Key used in looking up desktop value.
     * @param fallback Value used if desktop property is null.
     */
    public WindowsDesktopProperty(String key, Object fallback) {
        super(key,fallback);
    }
}
