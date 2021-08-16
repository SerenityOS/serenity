/*
 * Copyright (c) 1997, 2019, Oracle and/or its affiliates. All rights reserved.
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

package javax.accessibility;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import sun.awt.AWTAccessor;

/**
 * Base class used to maintain a strongly typed enumeration. This is the
 * superclass of {@link AccessibleState} and {@link AccessibleRole}.
 * <p>
 * The {@link #toDisplayString()} method allows you to obtain the localized
 * string for a locale independent key from a predefined {@code ResourceBundle}
 * for the keys defined in this class. This localized string is intended to be
 * readable by humans.
 *
 * @author Willie Walker
 * @author Peter Korn
 * @author Lynn Monsanto
 * @see AccessibleRole
 * @see AccessibleState
 */
public abstract class AccessibleBundle {

    private final String defaultResourceBundleName
        = "com.sun.accessibility.internal.resources.accessibility";

    static {
        AWTAccessor.setAccessibleBundleAccessor(
                new AWTAccessor.AccessibleBundleAccessor() {

                    @Override
                    public String getKey(AccessibleBundle accessibleBundle) {
                        return accessibleBundle.key;
                    }
                });
    }

    /**
     * Construct an {@code AccessibleBundle}.
     */
    public AccessibleBundle() {
    }

    /**
     * The locale independent name of the state. This is a programmatic name
     * that is not intended to be read by humans.
     *
     * @see #toDisplayString
     */
    protected String key = null;

    /**
     * Obtains the key as a localized string. If a localized string cannot be
     * found for the key, the locale independent key stored in the role will be
     * returned. This method is intended to be used only by subclasses so that
     * they can specify their own resource bundles which contain localized
     * strings for their keys.
     *
     * @param  name the name of the resource bundle to use for lookup
     * @param  locale the locale for which to obtain a localized string
     * @return a localized string for the key
     */
    protected String toDisplayString(final String name, final Locale locale) {
        try {
            return ResourceBundle.getBundle(name, locale).getString(key);
        } catch (ClassCastException | MissingResourceException ignored) {
            return key; // return the non-localized key
        }
    }

    /**
     * Obtains the key as a localized string. If a localized string cannot be
     * found for the key, the locale independent key stored in the role will be
     * returned.
     *
     * @param  locale the locale for which to obtain a localized string
     * @return a localized string for the key
     */
    public String toDisplayString(Locale locale) {
        return toDisplayString(defaultResourceBundleName, locale);
    }

    /**
     * Gets localized string describing the key using the default locale.
     *
     * @return a localized string describing the key using the default locale
     */
    public String toDisplayString() {
        return toDisplayString(Locale.getDefault());
    }

    /**
     * Gets localized string describing the key using the default locale.
     *
     * @return a localized string describing the key using the default locale
     * @see #toDisplayString
     */
    public String toString() {
        return toDisplayString();
    }
}
