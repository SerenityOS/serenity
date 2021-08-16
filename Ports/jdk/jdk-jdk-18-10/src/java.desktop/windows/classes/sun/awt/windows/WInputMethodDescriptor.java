/*
 * Copyright (c) 1998, 2014, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.Image;
import java.awt.Toolkit;
import java.awt.im.spi.InputMethod;
import java.awt.im.spi.InputMethodDescriptor;
import java.util.Locale;

/**
 * Provides sufficient information about an input method
 * to enable selection and loading of that input method.
 * The input method itself is only loaded when it is actually used.
 *
 * @since 1.3
 */

final class WInputMethodDescriptor implements InputMethodDescriptor {

    /**
     * @see java.awt.im.spi.InputMethodDescriptor#getAvailableLocales
     */
    @Override
    public Locale[] getAvailableLocales() {
        // returns a copy of internal list for public API
        Locale[] locales = getAvailableLocalesInternal();
        Locale[] tmp = new Locale[locales.length];
        System.arraycopy(locales, 0, tmp, 0, locales.length);
        return tmp;
    }

    static Locale[] getAvailableLocalesInternal() {
        return getNativeAvailableLocales();
    }

    /**
     * @see java.awt.im.spi.InputMethodDescriptor#hasDynamicLocaleList
     */
    @Override
    public boolean hasDynamicLocaleList() {
        return true;
    }

    /**
     * @see java.awt.im.spi.InputMethodDescriptor#getInputMethodDisplayName
     */
    @Override
    public synchronized String getInputMethodDisplayName(Locale inputLocale, Locale displayLanguage) {
        // We ignore the input locale.
        // When displaying for the default locale, rely on the localized AWT properties;
        // for any other locale, fall back to English.
        String name = "System Input Methods";
        if (Locale.getDefault().equals(displayLanguage)) {
            name = Toolkit.getProperty("AWT.HostInputMethodDisplayName", name);
        }
        return name;
    }

    /**
     * @see java.awt.im.spi.InputMethodDescriptor#getInputMethodIcon
     */
    @Override
    public Image getInputMethodIcon(Locale inputLocale) {
        return null;
    }

    /**
     * @see java.awt.im.spi.InputMethodDescriptor#createInputMethod
     */
    @Override
    public InputMethod createInputMethod() throws Exception {
        return new WInputMethod();
    }

    private static native Locale[] getNativeAvailableLocales();
}
