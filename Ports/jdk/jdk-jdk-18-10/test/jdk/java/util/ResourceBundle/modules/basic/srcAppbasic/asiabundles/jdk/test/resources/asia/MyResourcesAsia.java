/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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

package jdk.test.resources.asia;

import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.spi.AbstractResourceBundleProvider;

import jdk.test.resources.spi.MyResourcesProvider;

public class MyResourcesAsia extends AbstractResourceBundleProvider
    implements MyResourcesProvider
{
    private static Set<Locale> asiaLocales
        = Set.of(Locale.JAPANESE, Locale.CHINESE, Locale.TAIWAN);

    @Override
    public String toBundleName(String baseName, Locale locale) {
        String bundleName = super.toBundleName(baseName, locale);
        if (asiaLocales.contains(locale)) {
            int index = bundleName.lastIndexOf('.');
            return bundleName.substring(0, index + 1) + "asia" + bundleName.substring(index);
        }
        return bundleName;
    }

    @Override
    public ResourceBundle getBundle(String baseName, Locale locale) {
        if (asiaLocales.contains(locale)) {
            return super.getBundle(baseName, locale);
        }
        return null;
    }
}
