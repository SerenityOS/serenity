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
import jdk.test.resources.spi.MyResourcesProvider;

public class MyResourcesAsia extends MyResourcesProvider {
    public MyResourcesAsia() {
        super("java.properties");
    }

    @Override
    protected String toBundleName(String baseName, Locale locale) {
        // Convert baseName to its properties resource name for the given locale
        // e.g., jdk.test.resources.MyResources -> jdk/test/resources/asia/MyResources_zh_TW
        StringBuilder sb = new StringBuilder();
        int index = baseName.lastIndexOf('.');
        sb.append(baseName.substring(0, index))
            .append(".asia")
            .append(baseName.substring(index));
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
    protected boolean isSupportedInModule(Locale locale) {
        return locale.equals(Locale.JAPANESE)
            || locale.equals(Locale.CHINESE) || locale.equals(Locale.TAIWAN);
    }
}
