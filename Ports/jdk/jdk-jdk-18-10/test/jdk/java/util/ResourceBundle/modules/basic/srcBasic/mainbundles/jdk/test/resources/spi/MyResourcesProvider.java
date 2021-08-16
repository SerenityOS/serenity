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

package jdk.test.resources.spi;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.ResourceBundle.Control;
import java.util.Set;
import java.util.spi.AbstractResourceBundleProvider;


public class MyResourcesProvider extends AbstractResourceBundleProvider {
    private final String region;
    private final Set<Locale> supportedLocales;
    private final List<String> formats;

    protected MyResourcesProvider() {
        region = "";
        supportedLocales = null;
        formats = Collections.emptyList();
    }

    protected MyResourcesProvider(String format, String region, Locale... locales) {
        super(format);
        this.region = region;
        this.supportedLocales = new HashSet<>(Arrays.asList(locales));
        this.formats = Collections.singletonList(format);
    }

    @Override
    public ResourceBundle getBundle(String baseName, Locale locale) {
        if (isSupportedInModule(locale)) {
           return super.getBundle(baseName, locale);
        }
        return null;
    }

    @Override
    protected String toBundleName(String baseName, Locale locale) {
        String name = addRegion(baseName);
        return Control.getControl(Control.FORMAT_DEFAULT).toBundleName(name, locale);
    }

    private String addRegion(String baseName) {
        if (region.isEmpty()) {
            return baseName;
        }
        int index = baseName.lastIndexOf('.');
        return baseName.substring(0, index + 1) + region + baseName.substring(index);
    }

    protected boolean isSupportedInModule(Locale locale) {
        return supportedLocales.contains(locale);
    }
}
