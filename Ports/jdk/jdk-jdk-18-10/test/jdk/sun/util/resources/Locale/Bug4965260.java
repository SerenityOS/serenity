/*
 * Copyright (c) 2007, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4965260 8008577
 * @modules jdk.localedata
 * @summary Verifies the language name of "nl" for supported locales
 * @run main/othervm -Djava.locale.providers=JRE,SPI Bug4965260
 */

import java.util.Locale;

public class Bug4965260  {

    // Define supported locales
    static Locale[] locales2Test = new Locale[] {
        new Locale("de"),
        new Locale("es"),
        new Locale("fr"),
        new Locale("it"),
        new Locale("sv")
    };

    static String[] expectedNames = new String[] {
        "Niederl\u00e4ndisch",
        "neerland\u00e9s",
        "n\u00e9erlandais",
        "neerlandese",
        "nederl\u00e4ndska"
    };

    public static void main(String[] args) throws Exception {
        Locale reservedLocale = Locale.getDefault();
        try {
            Locale.setDefault(Locale.ENGLISH);
            if (locales2Test.length != expectedNames.length) {
                throw new Exception("\nData sizes does not match!\n");
            }

            StringBuffer message = new StringBuffer("");
            Locale dutch = new Locale("nl", "BE");
            String current;
            for (int i = 0; i < locales2Test.length; i++) {
                Locale locale = locales2Test[i];
                current = dutch.getDisplayLanguage(locale);
                if (!current.equals(expectedNames[i])) {
                    message.append("[");
                    message.append(locale.getDisplayLanguage());
                    message.append("] ");
                    message.append("Language name is ");
                    message.append(current);
                    message.append(" should be ");
                    message.append(expectedNames[i]);
                    message.append("\n");
                }
            }

            if (message.length() >0) {
                throw new Exception("\n" + message.toString());
            }
        } finally {
            // restore the reserved locale
            Locale.setDefault(reservedLocale);
        }
    }
}
