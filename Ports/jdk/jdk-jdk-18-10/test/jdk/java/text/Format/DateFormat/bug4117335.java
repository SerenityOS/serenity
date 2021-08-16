/*
 * Copyright (c) 1998, 2016, Oracle and/or its affiliates. All rights reserved.
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
 *
 * @bug 4117335 4432617
 * @modules jdk.localedata
 */

import java.text.DateFormatSymbols ;
import java.util.Locale;

public class bug4117335 {

    public static void main(String[] args) throws Exception
    {
        DateFormatSymbols symbols = new DateFormatSymbols(Locale.JAPAN);
        String[] eras = symbols.getEras();
        System.out.println("BC = " + eras[0]);
        if (!eras[0].equals(bc)) {
            System.out.println("*** Should have been " + bc);
            throw new Exception("Error in BC");
        }
        System.out.println("AD = " + eras[1]);
        if (!eras[1].equals(ad)) {
            System.out.println("*** Should have been " + ad);
            throw new Exception("Error in AD");
        }
        String[][] zones = symbols.getZoneStrings();
        for (int i = 0; i < zones.length; i++) {
            if (!"Asia/Tokyo".equals(zones[i][0])) {
                continue;
            }
            System.out.println("Long zone name = " + zones[i][1]);
            if (!zones[i][1].equals(jstLong)) {
                System.out.println("*** Should have been " + jstLong);
                throw new Exception("Error in long TZ name");
            }
            System.out.println("Short zone name = " + zones[i][2]);
            if (!zones[i][2].equals(jstShort)) {
                System.out.println("*** Should have been " + jstShort);
                throw new Exception("Error in short TZ name");
            }
            System.out.println("Long zone name = " + zones[i][3]);
            if (!zones[i][3].equals(jdtLong)) {
                System.out.println("*** Should have been " + jdtLong);
                throw new Exception("Error in long TZ name");
            }
            System.out.println("SHORT zone name = " + zones[i][4]);
            if (!zones[i][4].equals(jdtShort)) {
                System.out.println("*** Should have been " + jdtShort);
                throw new Exception("Error in short TZ name");
            }
        }
    }

    static final String bc = "\u7d00\u5143\u524d";
    static final String ad = "\u897f\u66a6";
    static final String jstLong = "\u65e5\u672c\u6a19\u6e96\u6642";
    static final String jstShort = "JST";
    static final String jdtLong = "\u65e5\u672c\u590f\u6642\u9593";
    static final String jdtShort = "JDT";
}
