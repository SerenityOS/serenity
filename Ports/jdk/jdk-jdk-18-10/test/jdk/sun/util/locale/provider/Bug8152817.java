/*
 * Copyright (c) 2016, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8152817
 * @summary Make sure that resource bundles in the jdk.localedata module are
 *          loaded under a security manager.
 * @modules jdk.localedata
 * @run main/othervm -Djava.security.manager=allow -Djava.locale.providers=COMPAT
 *      -Djava.security.debug=access,failure,codebase=jrt:/jdk.localedata Bug8152817
 */

import java.text.DateFormatSymbols;
import java.time.chrono.HijrahChronology;
import java.time.format.TextStyle;
import java.util.Calendar;
import java.util.Locale;

public class Bug8152817 {
    public static void main(String[] args) throws Exception {
        System.setSecurityManager(new SecurityManager());

        DateFormatSymbols syms = DateFormatSymbols.getInstance(Locale.GERMAN);
        if (!"Oktober".equals(syms.getMonths()[Calendar.OCTOBER])) {
            throw new RuntimeException("Test failed (FormatData)");
        }

        String s = HijrahChronology.INSTANCE.getDisplayName(TextStyle.FULL, Locale.GERMAN);
        if (!s.contains("Islamischer Kalender")) {
            throw new RuntimeException("Test failed (JavaTimeSupplementary)");
        }
    }
}
