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
 *@test
 *@bug 4848242
 *@summary Make sure that MET time zone is not misinterpreted in euro locales.
 *         Display the MET and MEST TZ human-readable name in all euro locales.
 */

import java.util.Locale;
import java.util.TimeZone;
import java.text.DateFormatSymbols;

public class Bug4848242 {

    public static void main(String[] args) {
        getTzInfo("de", "DE");
        getTzInfo("es", "ES");
        getTzInfo("fr", "FR");
        getTzInfo("it", "IT");
        getTzInfo("sv", "SV");
    }

    static void getTzInfo(String langName, String locName)
    {
        Locale tzLocale = new Locale(langName, locName);
        TimeZone euroTz = TimeZone.getTimeZone("MET");

        System.out.println("Locale is " + langName + "_" + locName);

        if ( euroTz.getID().equalsIgnoreCase("GMT") ) {
            // if we don't have a timezone and default back to GMT
            throw new RuntimeException("Error: no time zone found");
        }

        // get the timezone info
        System.out.println(euroTz.getDisplayName(false, TimeZone.SHORT, tzLocale));
        if(!euroTz.getDisplayName(false, TimeZone.SHORT, tzLocale).equals("MET"))
          throw new RuntimeException("Timezone name is incorrect (should be MET)\n");
        System.out.println(euroTz.getDisplayName(false, TimeZone.LONG, tzLocale));

        System.out.println(euroTz.getDisplayName(true, TimeZone.SHORT, tzLocale));
        if(!euroTz.getDisplayName(true, TimeZone.SHORT, tzLocale).equals("MEST"))
            throw new RuntimeException("Summer timezone name is incorrect (should be MEST)\n");
        System.out.println(euroTz.getDisplayName(true, TimeZone.LONG, tzLocale) + "\n");

    }

}
