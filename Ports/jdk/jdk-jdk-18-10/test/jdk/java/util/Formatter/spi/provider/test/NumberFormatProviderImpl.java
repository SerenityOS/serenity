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
package test;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.spi.NumberFormatProvider;
import java.util.Locale;

public class NumberFormatProviderImpl extends NumberFormatProvider {

    private static final Locale[] locales = {Locale.FRENCH, Locale.JAPANESE,
            new Locale("hi", "IN"), new Locale("xx", "YY")};

    @Override
    public NumberFormat getCurrencyInstance(Locale locale) {
        return null;
    }

    @Override
    public NumberFormat getIntegerInstance(Locale locale) {
        return null;
    }

    @Override
    public NumberFormat getNumberInstance(Locale locale) {
        if (locale.getLanguage().equals("xx")) {
            return new DecimalFormat("#0.###", DecimalFormatSymbols.getInstance(Locale.US));
        } else {
            return null;
        }
    }

    @Override
    public NumberFormat getPercentInstance(Locale locale) {
        return null;
    }

    @Override
    public Locale[] getAvailableLocales() {
        return locales;
    }
}
