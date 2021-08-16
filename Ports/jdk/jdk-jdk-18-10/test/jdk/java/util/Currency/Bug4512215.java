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
 * @test
 * @bug 4512215 4818420 4819436
 * @summary Updated currency data.
 */

import java.util.Currency;
import java.util.Locale;

public class Bug4512215 {

    public static void main(String[] args) throws Exception {
        testCurrencyDefined("XBD", -1);
        testCountryCurrency("TJ", "TJS", 2);
        testCountryCurrency("FO", "DKK", 2);
        testCountryCurrency("FK", "FKP", 2);

        testCountryCurrency("AF", "AFN", 2);    // changed from "AFA"

        // Newsletter V-5 on ISO 3166-1 (2002-05-20)
        testCountryCurrency("TL", "USD", 2);    // successor to TP/TPE

        // Newsletter V-8 on ISO 3166-1 (2003-07-23)
        testCountryCurrency("CS", "CSD", 2);    // successor to YU/YUM
    }

    private static void testCountryCurrency(String country, String currencyCode,
            int digits) {
        testCurrencyDefined(currencyCode, digits);
        Currency currency = Currency.getInstance(new Locale("", country));
        if (!currency.getCurrencyCode().equals(currencyCode)) {
            throw new RuntimeException("[" + country
                    + "] expected: " + currencyCode
                    + "; got: " + currency.getCurrencyCode());
        }
    }

    private static void testCurrencyDefined(String currencyCode, int digits) {
        Currency currency = Currency.getInstance(currencyCode);
        if (currency.getDefaultFractionDigits() != digits) {
            throw new RuntimeException("[" + currencyCode
                    + "] expected: " + digits
                    + "; got: " + currency.getDefaultFractionDigits());
        }
    }
}
