/*
 * Copyright (c) 2007, Oracle and/or its affiliates. All rights reserved.
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

import java.util.*;
import java.util.spi.*;

import com.foobar.Utils;

public class CurrencyNameProviderImpl extends CurrencyNameProvider {
    static Locale[] avail = {new Locale("ja", "JP", "osaka"),
        new Locale("ja", "JP", "kyoto"),
        Locale.JAPAN,
        new Locale("xx")};

    public Locale[] getAvailableLocales() {
        return avail;
    }

    public String getSymbol(String c, Locale locale) {
        if (!Utils.supportsLocale(Arrays.asList(avail), locale)) {
            throw new IllegalArgumentException("locale is not supported: "+locale);
        }

        if (c.equals("JPY")) {
            if (Utils.supportsLocale(avail[0], locale)) {
                return "\u5186\u3084\u3002";
            } else if (Utils.supportsLocale(avail[1], locale)) {
                return "\u5186\u3069\u3059\u3002";
            } else if (Utils.supportsLocale(avail[2], locale)) {
                return "\u5186\u3067\u3059\u3002";
            } else if (Utils.supportsLocale(avail[3], locale)) {
                return "\u5186\u3070\u3064\u3070\u3064\u3002";
            }
        }
        return null;
    }

    @Override
    public String getDisplayName(String c, Locale locale) {
        if (!Utils.supportsLocale(Arrays.asList(avail), locale)) {
            throw new IllegalArgumentException("locale is not supported: "+locale);
        }

        if (c.equals("JPY")) {
            if (Utils.supportsLocale(avail[0], locale)) {
                return "\u65e5\u672c\u5186\u3084\u3002";
            } else if (Utils.supportsLocale(avail[1], locale)) {
                return "\u65e5\u672c\u5186\u3069\u3059\u3002";
            } else if (Utils.supportsLocale(avail[2], locale)) {
                return "\u65e5\u672c\u5186\u3067\u3059\u3002";
            } else if (Utils.supportsLocale(avail[3], locale)) {
                return "\u65e5\u672c\u5186\u3070\u3064\u3070\u3064\u3002";
            }
        }
        return null;
    }
}
