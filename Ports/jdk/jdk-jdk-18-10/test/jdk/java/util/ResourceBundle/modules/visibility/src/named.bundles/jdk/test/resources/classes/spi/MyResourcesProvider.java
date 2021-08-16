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

package jdk.test.resources.classes.spi;

import java.util.Locale;
import java.util.ResourceBundle;
import java.util.spi.AbstractResourceBundleProvider;

public class MyResourcesProvider extends AbstractResourceBundleProvider {
    public MyResourcesProvider() {
        super("java.class");
    }

    @Override
    protected String toBundleName(String baseName, Locale locale) {
        StringBuilder sb = new StringBuilder(baseName);
        String lang = locale.getLanguage();
        if (!lang.isEmpty()) {
            sb.append('_').append(lang);
            String country = locale.getCountry();
            if (!country.isEmpty()) {
                sb.append('_').append(country);
            }
        }
        return sb.toString();
    }

    @Override
    public ResourceBundle getBundle(String baseName, Locale locale) {
        ResourceBundle rb = super.getBundle(baseName, locale);
        String tag = locale.toLanguageTag();
        if (tag.equals("und")) {
            tag = "ROOT"; // to a human friendly name
        }
        System.out.printf("    MyResourcesProvider.getBundle(%s, %s)%n         -> %s%n",
                          baseName, tag, rb);
        return rb;
    }
}
