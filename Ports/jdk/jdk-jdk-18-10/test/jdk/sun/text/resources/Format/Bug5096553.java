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
 * @bug 5096553 8008577
 * @modules jdk.localedata
 * @summary updating dateformat for da_DK
 *          following resources:
 *          http://oss.software.ibm.com/cvs/icu/~checkout~/locale/common/main/da.xml
 *          http://www.microsoft.com/globaldev/nlsweb/default.asp?submitted=406
 *          see bug evaluation for more details
 * @run main/othervm -Djava.locale.providers=JRE,SPI Bug5096553
 */

import java.util.Date;
import java.text.DateFormat;
import java.util.Locale;
import java.util.Calendar;


public class Bug5096553
{
  public static void main(String[] args) {
      String expectedMed = "30-04-2008";
      String expectedShort="30-04-08";

      Locale dk = new Locale("da", "DK");
      DateFormat df1 = DateFormat.getDateInstance(DateFormat.MEDIUM, dk);
      DateFormat df2 = DateFormat.getDateInstance(DateFormat.SHORT, dk);
      String medString = new String (df1.format(new Date(108, Calendar.APRIL, 30)));
      String shortString = new String (df2.format(new Date(108, Calendar.APRIL, 30)));
      System.out.println(df1.format(new Date()));
      System.out.println(df2.format(new Date()));

      if (expectedMed.compareTo(medString) != 0) {
            throw new RuntimeException("Error: " + medString  + " should be " + expectedMed);
        }

      if (expectedShort.compareTo(shortString) != 0) {
            throw new RuntimeException("Error: " + shortString  + " should be " + expectedShort);
        }
  }
}
