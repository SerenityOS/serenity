/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8190278
 * @summary checks the Scanner.useLocale() with java.locale.providers=SPI,
 *          COMPAT. It should not throw ClassCastException if any SPI is
 *          used and NumberFormat.getInstance() does not return a
 *          DecimalFormat object. Also, to test the behaviour of Scanner
 *          while scanning numbers in the format of Scanner's locale.
 * @modules jdk.localedata
 * @library provider
 * @build provider/module-info provider/test.NumberFormatProviderImpl
 *        provider/test.NumberFormatImpl
 * @run main/othervm -Djava.locale.providers=SPI,COMPAT UseLocaleWithProvider
 */

import java.util.Locale;
import java.util.Scanner;

public class UseLocaleWithProvider {

    public static void main(String[] args) {

        try {
            testScannerUseLocale("-123.4", Locale.US, -123.4);
            testScannerUseLocale("-123,45", new Locale("fi", "FI"), -123.45);
            testScannerUseLocale("334,65", Locale.FRENCH, 334.65);
            testScannerUseLocale("4.334,65", Locale.GERMAN, 4334.65);
        } catch (ClassCastException ex) {
            throw new RuntimeException("[FAILED: With" +
                    " java.locale.providers=SPI,COMPAT, Scanner.useLocale()" +
                    " shouldn't throw ClassCastException]");
        }
    }

    private static void testScannerUseLocale(String number, Locale locale,
                                             Number actual) {
        Scanner sc = new Scanner(number).useLocale(locale);
        if (!sc.hasNextFloat() || sc.nextFloat() != actual.floatValue()) {
            throw new RuntimeException("[FAILED: With" +
                    " java.locale.providers=SPI,COMPAT, Scanner" +
                    ".hasNextFloat() or Scanner.nextFloat() is unable to" +
                    " scan the given number: " + number + ", in the given" +
                    " locale:" + locale + "]");
        }
    }
}

