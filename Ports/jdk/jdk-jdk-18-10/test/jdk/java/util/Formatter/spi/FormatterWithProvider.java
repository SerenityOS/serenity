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
/*
 * @test
 * @bug 8199672
 * @summary test the Formatter.format() method with java.locale.providers=SPI,
 *          COMPAT. It should not throw ClassCastException if an SPI is
 *          used and NumberFormat.getInstance() does not return a
 *          DecimalFormat object.
 * @modules jdk.localedata
 * @library provider
 * @build provider/module-info provider/test.NumberFormatProviderImpl
 * @run main/othervm -Djava.locale.providers=SPI,COMPAT FormatterWithProvider
 */

import java.util.Formatter;
import java.util.Locale;

public class FormatterWithProvider {

    public static void main(String[] args) {

        Integer number = 1234567;
        String formatString = "%,d";

        try {
            testFormatter(Locale.JAPANESE, formatString, number);
            testFormatter(Locale.FRENCH, formatString, number);
            testFormatter(new Locale("hi", "IN"), formatString, number);

        } catch (ClassCastException ex) {
            throw new RuntimeException("[FAILED: A ClassCastException is" +
                    " thrown while using Formatter.format() with VM" +
                    " argument java.locale.providers=SPI,COMPAT]", ex);
        }
    }

    private static void testFormatter(Locale locale, String formatString,
                                      Integer number) {

        // test using String.format
        String.format(locale, formatString, number);
        // test using Formatter's format
        Formatter formatter = new Formatter(locale);
        formatter.format(formatString, number);
    }

}

