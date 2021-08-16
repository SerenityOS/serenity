/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
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

package sun.util.resources.provider;

import java.util.Locale;
import java.util.ResourceBundle;
import sun.util.resources.LocaleData;

/**
 * Service Provider for loading locale data resource bundles in jdk.localedata
 * except for JavaTimeSupplementary resource bundles.
 */
public class LocaleDataProvider extends LocaleData.CommonResourceBundleProvider {
    @Override
    public ResourceBundle getBundle(String baseName, Locale locale) {
        var bundleName = toBundleName(baseName, locale);
        var rb = loadResourceBundle(bundleName);
        if (rb == null) {
            var otherBundleName = toOtherBundleName(baseName, bundleName, locale);
            if (!bundleName.equals(otherBundleName)) {
                rb = loadResourceBundle(otherBundleName);
            }
        }
        return rb;
    }

    /**
     * Utility method for loading a resource bundle in jdk.localedata.
     */
    static ResourceBundle loadResourceBundle(String bundleName) {
        Class<?> c = Class.forName(LocaleDataProvider.class.getModule(), bundleName);
        if (c != null && ResourceBundle.class.isAssignableFrom(c)) {
            try {
                @SuppressWarnings({"unchecked", "deprecation"})
                ResourceBundle rb = ((Class<ResourceBundle>) c).newInstance();
                return rb;
            } catch (InstantiationException | IllegalAccessException e) {
                throw new InternalError(e);
            }
        }
        return null;
    }
}
