/*
 * Copyright (c) 2007, 2012, Oracle and/or its affiliates. All rights reserved.
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
/*
 *
 */

package com.bar;

import java.text.*;
import java.util.*;
import java.util.spi.*;

import com.foobar.Utils;

public class LocaleNameProviderImpl extends LocaleNameProvider {
    static Locale[] avail = {Locale.JAPANESE,
                             Locale.JAPAN,
                             new Locale("ja", "JP", "osaka"),
                             new Locale("ja", "JP", "kyoto"),
                             new Locale("xx"),
                             new Locale("yy", "YY", "YYYY")};
    static List<Locale> availList = Arrays.asList(avail);
    public Locale[] getAvailableLocales() {
        return avail;
    }

    @Override
    public String getDisplayLanguage(String lang, Locale target) {
        return getDisplayString(lang, target);
    }

    @Override
    public String getDisplayCountry(String ctry, Locale target) {
        return getDisplayString(ctry, target);
    }

    @Override
    public String getDisplayVariant(String vrnt, Locale target) {
        return getDisplayString(vrnt, target);
    }

    private String getDisplayString(String key, Locale target) {
        if (!Utils.supportsLocale(availList, target)) {
            throw new IllegalArgumentException("locale is not supported: "+target);
        }

        String ret = null;

        if (target.getLanguage().equals("yy") &&
            target.getCountry().equals("YY")) {
            String vrnt = target.getVariant();
            if (vrnt.startsWith("YYYY")) {
                switch (key) {
                    case "yy":
                    case "YY":
                        ret = "waiwai";
                        break;

                    case "YYYY":
                        if (vrnt.equals("YYYY_suffix")) {
                            // for LocaleNameProviderTest.variantFallbackTest()
                            throw new RuntimeException(vrnt);
                        } else {
                            ret = "waiwai";
                        }
                        break;
                }
            }
        } else {
            // resource bundle based (allows fallback)
        try {
            ResourceBundle rb = ResourceBundle.getBundle("com.bar.LocaleNames", target);
                ret = rb.getString(key);
        } catch (MissingResourceException mre) {
        }
        }

        return ret;
    }
        }
