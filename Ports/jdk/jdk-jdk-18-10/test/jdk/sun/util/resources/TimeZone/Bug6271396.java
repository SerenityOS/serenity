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
 * @bug 6271396 8008577
 * @modules jdk.localedata
 * @summary Test case for verifying typo of timezone display name Australia/Lord_Howe
 * @run main/othervm -Djava.locale.providers=JRE,SPI Bug6271396
 */

import java.util.Locale;
import java.util.TimeZone;

public class Bug6271396 {

    public static void main(String[] args) {

        TimeZone Lord_Howe = TimeZone.getTimeZone("Australia/Lord_Howe");
        Locale tzLocale = new Locale("fr");

        if (!Lord_Howe.getDisplayName(false, TimeZone.LONG, tzLocale).equals
           ("Heure standard de Lord Howe"))
             throw new RuntimeException("\n" + tzLocale + ": LONG, " +
                                        "non-daylight saving name for " +
                                        "Australia/Lord_Howe should be " +
                                        "\"Heure standard de Lord Howe\"");
        if (!Lord_Howe.getDisplayName(true, TimeZone.LONG, tzLocale).equals
           ("Heure d'\u00e9t\u00e9 de Lord Howe"))
             throw new RuntimeException("\n" + tzLocale + ": LONG, " +
                                        "daylight saving name for " +
                                        "Australia/Lord_Howe should be " +
                                        "\"Heure d'\u00e9t\u00e9 de Lord Howe\"");

        tzLocale = new Locale("zh", "TW");
        if (!Lord_Howe.getDisplayName(false, TimeZone.LONG, tzLocale).equals
           ("\u8c6a\u52f3\u7235\u5cf6\u6a19\u6e96\u6642\u9593"))
             throw new RuntimeException("\n" + tzLocale + ": LONG, " +
                                        "non-daylight saving name for " +
                                        "Australia/Lord_Howe should be " +
                                        "\"\u8c6a\u52f3\u7235\u5cf6" +
                                        "\u6a19\u6e96\u6642\u9593\"");
        if (!Lord_Howe.getDisplayName(true, TimeZone.LONG, tzLocale).equals
           ("\u8c6a\u52f3\u7235\u5cf6\u590f\u4ee4\u6642\u9593"))
             throw new RuntimeException("\n" + tzLocale + ": LONG, " +
                                        "daylight saving name for " +
                                        "Australia/Lord_Howe should be " +
                                        "\"\u8c6a\u52f3\u7235\u5cf6" +
                                        "\u590f\u4ee4\u6642\u9593\"");
   }
}
