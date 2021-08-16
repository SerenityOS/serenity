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
 * @bug 4122700
 * @summary Verify that list of available locales is non-empty, and print the list
 */

import java.util.Locale;

public class bug4122700 {
    public static void main(String[] args) throws Exception {
        Locale[] systemLocales = Locale.getAvailableLocales();
        if (systemLocales.length == 0)
            throw new Exception("Available locale list is empty!");
        System.out.println("Found " + systemLocales.length + " locales:");
        Locale[] locales = new Locale[systemLocales.length];
        for (int i = 0; i < locales.length; i++) {
            Locale lowest = null;
            for (int j = 0; j < systemLocales.length; j++) {
                if (i > 0 && locales[i - 1].toString().compareTo(systemLocales[j].toString()) >= 0)
                    continue;
                if (lowest == null || systemLocales[j].toString().compareTo(lowest.toString()) < 0)
                    lowest = systemLocales[j];
            }
            locales[i] = lowest;
        }
        for (int i = 0; i < locales.length; i++) {
            if (locales[i].getCountry().length() == 0)
                System.out.println("    " + locales[i].getDisplayLanguage() + ":");
            else {
                if (locales[i].getVariant().length() == 0)
                    System.out.println("        " + locales[i].getDisplayCountry());
                else
                    System.out.println("        " + locales[i].getDisplayCountry() + ", "
                                    + locales[i].getDisplayVariant());
            }
        }
    }
}
