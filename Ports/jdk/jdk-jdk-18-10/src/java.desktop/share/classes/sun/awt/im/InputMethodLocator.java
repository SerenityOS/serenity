/*
 * Copyright (c) 1998, 1999, Oracle and/or its affiliates. All rights reserved.
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

package sun.awt.im;

import java.awt.AWTException;
import java.awt.im.spi.InputMethodDescriptor;
import java.util.Locale;

/**
 * Provides complete information to make and handle the selection
 * of an input method and a locale. Immutable class.
 */
final class InputMethodLocator {

    private InputMethodDescriptor descriptor;

    // Currently `loader' is always the class loader for a
    // descriptor. `loader' is provided for future extensions to be
    // able to load input methods from somewhere else, and to support
    // per input method name space.
    private ClassLoader loader;

    private Locale locale;

    InputMethodLocator(InputMethodDescriptor descriptor, ClassLoader loader, Locale locale) {
        if (descriptor == null) {
            throw new NullPointerException("descriptor can't be null");
        }
        this.descriptor = descriptor;
        this.loader = loader;
        this.locale = locale;
    }

    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (other == null || this.getClass() != other.getClass()) {
            return false;
        }

        InputMethodLocator otherLocator = (InputMethodLocator) other;
        if (!descriptor.getClass().equals(otherLocator.descriptor.getClass())) {
            return false;
        }
        if (loader == null && otherLocator.loader != null
            || loader != null && !loader.equals(otherLocator.loader)) {
            return false;
        }
        if (locale == null && otherLocator.locale != null
            || locale != null && !locale.equals(otherLocator.locale)) {
            return false;
        }
        return true;
    }

    public int hashCode() {
        int result = descriptor.hashCode();
        if (loader != null) {
            result |= loader.hashCode() << 10;
        }
        if (locale != null) {
            result |= locale.hashCode() << 20;
        }
        return result;
    }

    InputMethodDescriptor getDescriptor() {
        return descriptor;
    }

    ClassLoader getClassLoader() {
        return loader;
    }

    Locale getLocale() {
        return locale;
    }

    /**
     * Returns whether support for locale is available from
     * the input method.
     */
    boolean isLocaleAvailable(Locale locale) {
        try {
            Locale[] locales = descriptor.getAvailableLocales();
            for (int i = 0; i < locales.length; i++) {
                if (locales[i].equals(locale)) {
                    return true;
                }
            }
        } catch (AWTException e) {
            // treat this as no locale available
        }
        return false;
    }

    /**
     * Returns an input method locator that has locale forLocale,
     * but otherwise the same data as this locator. Does not
     * check whether the input method actually supports forLocale -
     * use {@link #isLocaleAvailable} for that.
     */
    InputMethodLocator deriveLocator(Locale forLocale) {
        if (forLocale == locale) {
            return this;
        } else {
            return new InputMethodLocator(descriptor, loader, forLocale);
        }
    }

    /**
     * Returns whether this and other describe the same input method
     * engine, ignoring the locale setting.
     */
    boolean sameInputMethod(InputMethodLocator other) {
        if (other == this) {
            return true;
        }
        if (other == null) {
            return false;
        }

        if (!descriptor.getClass().equals(other.descriptor.getClass())) {
            return false;
        }
        if (loader == null && other.loader != null
            || loader != null && !loader.equals(other.loader)) {
            return false;
        }
        return true;
    }

    /**
     * Returns a string that can be used as an action command string.
     * The first part of the string identifies the input method; it does
     * not include '\n'. If getLocale is not null, getLocale().toString()
     * is appended, separated by '\n'.
     */
    String getActionCommandString() {
        String inputMethodString = descriptor.getClass().getName();
        if (locale == null) {
            return inputMethodString;
        } else {
            return inputMethodString + "\n" + locale.toString();
        }
    }
}
