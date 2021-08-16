/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
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

package jdk.internal.access;

import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Provides access to non-public methods in java.util.ResourceBundle.
 */
public interface JavaUtilResourceBundleAccess {
    /**
     * Sets the bundle's parent to the given parent.
     */
    void setParent(ResourceBundle bundle, ResourceBundle parent);

    /**
     * Returns the parent of the given bundle or null if the bundle has no parent.
     */
    ResourceBundle getParent(ResourceBundle bundle);

    /**
     * Sets the bundle's locale to the given locale.
     */
    void setLocale(ResourceBundle bundle, Locale locale);

    /**
     * Sets the bundle's base name to the given name.
     */
    void setName(ResourceBundle bundle, String name);

    /**
     * Returns a {@code ResourceBundle} of the given baseName and locale
     * loaded on behalf of the given module with no caller module
     * access check.
     */
    ResourceBundle getBundle(String baseName, Locale locale, Module module);

    /**
     * Instantiates a {@code ResourceBundle} of the given bundle class.
     */
    ResourceBundle newResourceBundle(Class<? extends ResourceBundle> bundleClass);
}
