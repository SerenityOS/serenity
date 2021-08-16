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
 * @bug 4518811
 * @modules jdk.localedata
 * @summary Verifies the minimum days of the week for euro locales
 */

// this code is a bit brute-force, but I've been coding in nothing but Shell for the last year, so I'm rusty.

import java.util.Locale;
import java.util.Calendar;

public class Bug4518811 {
  public static void main(String [] args) {

      int totalErrors=0;

      // go through the locales
      totalErrors += getDays("ca", "ES");
      totalErrors += getDays("cs", "CZ");
      totalErrors += getDays("da", "DK");
      totalErrors += getDays("de", "AT");
      totalErrors += getDays("el", "GR");
      totalErrors += getDays("en", "GB");
      totalErrors += getDays("en", "IE");
      totalErrors += getDays("es", "ES");
      totalErrors += getDays("et", "EE");
      totalErrors += getDays("fi", "FI");
      totalErrors += getDays("fr", "BE");
      totalErrors += getDays("fr", "FR");
      totalErrors += getDays("is", "IS");
      totalErrors += getDays("lt", "LT");
      totalErrors += getDays("nl", "BE");
      totalErrors += getDays("pl", "PL");
      totalErrors += getDays("pt", "PT");

      if (totalErrors > 0)
          throw new RuntimeException();
      //System.err.println("Minimal Days in First Week: "+c.getMinimalDaysInFirstWeek());
  }

    static int getDays(String lang, String loc){
        int errors=0;
        Locale newlocale = new Locale(lang, loc);

        Calendar newCal = Calendar.getInstance(newlocale);

        int minDays = newCal.getMinimalDaysInFirstWeek();
        System.out.println("The Min Days in First Week for "+ lang +"_" + loc + " is " + minDays);

        if (minDays != 4){
            System.out.println("Warning! Should be 4, not " + minDays +"!");
            errors++;
        }
        return errors;
    }
}
