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

import java.awt.*;
import java.awt.im.spi.*;
import java.util.*;
import java.util.List;

/**
* Provides sufficient information about an input method
 * to enable selection and loading of that input method.
 * The input method itself is only loaded when it is actually used.
 */

public class CInputMethodDescriptor implements InputMethodDescriptor {

    static {
        nativeInit();
    }

    public CInputMethodDescriptor() {
    }

    /**
     * @see java.awt.im.spi.InputMethodDescriptor#getAvailableLocales
     */
    public Locale[] getAvailableLocales() {
        // returns a copy of internal list for public API
        Object[] locales = getAvailableLocalesInternal();
        Locale[] tmp = new Locale[locales.length];
        System.arraycopy(locales, 0, tmp, 0, locales.length);
        return tmp;
    }

    static Object[] getAvailableLocalesInternal() {
        List<Object> workList = nativeGetAvailableLocales();
        Locale currentLocale = CInputMethod.getNativeLocale();

        if (workList == null || workList.isEmpty()) {
            return new Object[] {
                    currentLocale != null ? currentLocale : Locale.getDefault()
            };
        } else {
            if (currentLocale != null && !workList.contains(currentLocale)) {
                workList.add(currentLocale);
            }
            return workList.toArray();
        }
    }

    /**
        * @see java.awt.im.spi.InputMethodDescriptor#hasDynamicLocaleList
     */
    public boolean hasDynamicLocaleList() {
        return false;
    }

    /**
        * @see java.awt.im.spi.InputMethodDescriptor#getInputMethodDisplayName
     */
    public synchronized String getInputMethodDisplayName(Locale inputLocale, Locale displayLanguage) {
        String name = "System Input Methods";
        if (Locale.getDefault().equals(displayLanguage)) {
            name = Toolkit.getProperty("AWT.HostInputMethodDisplayName", name);
        }
        return name;
    }

    /**
        * @see java.awt.im.spi.InputMethodDescriptor#getInputMethodIcon
     */
    public Image getInputMethodIcon(Locale inputLocale) {
        // This should return the flag icon corresponding to the input Locale.
        return null;
    }

    /**
        * @see java.awt.im.spi.InputMethodDescriptor#createInputMethod
     */
    public InputMethod createInputMethod() throws Exception {
        return new CInputMethod();
    }

    public String toString() {
        Locale[] loc = getAvailableLocales();
        String locnames = null;

        for (int i = 0; i < loc.length; i++) {
            if (locnames == null) {
                locnames = loc[i].toString();
            } else {
                locnames += "," + loc[i];
            }
        }
        return getClass().getName() + "[" +
            "locales=" + locnames +
            ",localelist=" + (hasDynamicLocaleList() ? "dynamic" : "static") +
            "]";
    }

    private static native void nativeInit();
    private static native List<Object> nativeGetAvailableLocales();
}
