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
 * @bug 4248694
 * @modules jdk.localedata
 * @summary updating collation tables for icelandic
 */

import java.text.Collator;
import java.util.Arrays;
import java.util.Locale;

public class Bug4248694 {

  /********************************************************
  *********************************************************/
  public static void main (String[] args) {
      Locale reservedLocale = Locale.getDefault();
      try {
          int errors=0;

          Locale loc = new Locale ("is", "is");   // Icelandic

          Locale.setDefault (loc);
          Collator col = Collator.getInstance ();

          String[] data = {"\u00e6ard",
                           "Zard",
                           "aard",
                           "\u00feard",
                           "vird",
                           "\u00c6ard",
                           "Zerd",
                           "\u00deard"};

          String[] sortedData = {"aard",
                                 "vird",
                                 "Zard",
                                 "Zerd",
                                 "\u00feard",
                                 "\u00deard",
                                 "\u00e6ard",
                                 "\u00c6ard"};

          Arrays.sort (data, col);

          System.out.println ("Using " + loc.getDisplayName());
          for (int i = 0;  i < data.length;  i++) {
              System.out.println(data[i] + "  :  " + sortedData[i]);
              if (sortedData[i].compareTo(data[i]) != 0) {
                  errors++;
              }
          }//end for

          if (errors > 0)
              throw new RuntimeException();
      } finally {
          // restore the reserved locale
          Locale.setDefault(reservedLocale);
      }
  }//end main

}//end class CollatorTest
