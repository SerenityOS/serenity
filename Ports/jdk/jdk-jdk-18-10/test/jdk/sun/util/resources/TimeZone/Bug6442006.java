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
 * @bug 6442006 8008577
 * @modules jdk.localedata
 * @summary Test case for verifying timezone display name for Asia/Taipei
 * @run main/othervm -Djava.locale.providers=JRE,SPI Bug6442006
 */

import java.util.Locale;
import java.util.TimeZone;

public class Bug6442006 {

    public static void main(String[] args) {

        TimeZone tz = TimeZone.getTimeZone("Asia/Taipei");
        Locale tzLocale = new Locale("ja");
        String jaStdName = "\u4e2d\u56fd\u6a19\u6e96\u6642";
        String jaDstName = "\u4e2d\u56fd\u590f\u6642\u9593";

        if (!tz.getDisplayName(false, TimeZone.LONG, tzLocale).equals
           (jaStdName))
             throw new RuntimeException("\n" + tzLocale + ": LONG, " +
                                        "non-daylight saving name for " +
                                        tz.getID() +
                                        " should be " +
                                        jaStdName);
        if (!tz.getDisplayName(true, TimeZone.LONG, tzLocale).equals
           (jaDstName))
             throw new RuntimeException("\n" + tzLocale + ": LONG, " +
                                        "daylight saving name for " +
                                        tz.getID() +
                                        " should be " +
                                        jaDstName);
    }
}
