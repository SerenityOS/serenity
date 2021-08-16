/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8139107
 * @summary Test that date parsing with DateTimeFormatter pattern
 *   that contains timezone field doesn't trigger NPE. All supported
 *   locales are tested.
 * @run testng/othervm -Djava.locale.providers=JRE,SPI Bug8139107
 */
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.testng.annotations.Test;

public class Bug8139107 {

    @Test
    public void testSupportedLocales() {
        for (Locale loc:Locale.getAvailableLocales()) {
            testLocale(loc);
        }
    }

    //Test one locale
    void testLocale(Locale tl) {
        System.out.println("Locale:" + tl);
        DateTimeFormatter inputDateTimeFormat = DateTimeFormatter
                .ofPattern(pattern)
                .withLocale(tl);
        System.out.println("Parse result: " + inputDateTimeFormat.parse(inputDate));
    }

    // Input date time string with short time zone name
    static final String inputDate = "06-10-2015 18:58:04 MSK";
    // Pattern with time zone field
    static final String pattern = "dd-MM-yyyy HH:mm:ss z";
}

