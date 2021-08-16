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

package com.foo;

import java.text.*;
import java.text.spi.*;
import java.util.*;

import com.foobar.Utils;

public class DecimalFormatSymbolsProviderImpl extends DecimalFormatSymbolsProvider {

    static Locale[] avail = {
        new Locale("ja", "JP", "osaka"),
        new Locale("ja", "JP", "kyoto"),
        Locale.JAPAN,
        new Locale("yy", "ZZ", "UUU")
    };
    static List<Locale> availList = Arrays.asList(avail);

    static String[] dialect = {
        "\u3084\u3002",
        "\u3069\u3059\u3002",
        "\u3067\u3059\u3002",
        "-yy-ZZ-UUU"
    };

    static HashMap<Locale, FooDecimalFormatSymbols> symbols = new HashMap<Locale, FooDecimalFormatSymbols>(4);

    public Locale[] getAvailableLocales() {
        return avail;
    }

    public DecimalFormatSymbols getInstance(Locale locale) {
        if (!Utils.supportsLocale(availList, locale)) {
            throw new IllegalArgumentException("locale is not supported: "+locale);
        }

        FooDecimalFormatSymbols fdfs = symbols.get(locale);
        if (fdfs == null) {
            for (int index = 0; index < avail.length; index ++) {
                if (Utils.supportsLocale(avail[index], locale)) {
                    fdfs = new FooDecimalFormatSymbols(index);
                    symbols.put(locale, fdfs);
                    break;
                }
            }
        }
        return fdfs;
    }

    class FooDecimalFormatSymbols extends DecimalFormatSymbols {
        String dialect = "";

        String infinity = null;
        String nan = null;

        public FooDecimalFormatSymbols(int index) {
            super(DecimalFormatSymbolsProviderImpl.this.avail[index]);
            dialect = DecimalFormatSymbolsProviderImpl.this.dialect[index];
        }

        // overrides methods only returns Strings
        public String getInfinity() {
            if (infinity == null) {
                infinity = super.getInfinity() + dialect;
            }
            return infinity;
        }

        public void setInfinity(String infinity) {
            this.infinity = infinity;
        }

        public String getNaN() {
            if (nan == null) {
                nan = super.getNaN() + dialect;
            }
            return nan;
        }

        public void setNaN(String nan) {
            this.nan = nan;
        }
    }
}
