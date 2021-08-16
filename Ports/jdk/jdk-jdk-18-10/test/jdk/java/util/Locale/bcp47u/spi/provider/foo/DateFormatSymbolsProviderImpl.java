/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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

package foo;

import java.text.DateFormatSymbols;
import java.text.spi.DateFormatSymbolsProvider;
import java.util.Locale;

/*
 * Implements DateFormatSymbolsProvider SPI, in order to check if the
 * extensions work correctly.
 */
public class DateFormatSymbolsProviderImpl extends DateFormatSymbolsProvider {
    private static final Locale AA = Locale.forLanguageTag("en-AA");
    private static final Locale USJCAL = Locale.forLanguageTag("en-US-u-ca-japanese");
    private static final Locale[] avail = {AA, Locale.US};

    @Override
    public Locale[] getAvailableLocales() {
        return avail;
    }

    @Override
    public boolean isSupportedLocale(Locale l) {
        // Overriding to check the relation between
        // isSupportedLocale/getAvailableLocales works correctly
        if (l.equals(AA)) {
            // delegates to super, as if isSupportedLocale didn't exist.
            return super.isSupportedLocale(l);
        } else {
            return (l.equals(USJCAL));
        }
    }

    @Override
    public DateFormatSymbols getInstance(Locale l) {
        return new MyDateFormatSymbols(l);
    }

    class MyDateFormatSymbols extends DateFormatSymbols {
        Locale locale;

        public MyDateFormatSymbols(Locale l) {
            super(l);
            locale = l;
        }

        @Override
        public String[] getMonths() {
            String[] ret = super.getMonths();
            // replace the first item with some unique value
            if (locale.stripExtensions().equals(AA)) {
                ret[0] = "foo";
            } else if (locale.equals(USJCAL)) {
                ret[0] = "bar";
            } else {
                throw new RuntimeException("Unsupported locale: " + locale);
            }
            return ret;
        }
    }
}
