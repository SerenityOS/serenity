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
 * @bug 4804273
 * @modules jdk.localedata
 * @summary updating collation tables for swedish
 */

import java.text.Collator;
import java.util.Arrays;
import java.util.Locale;

public class Bug4804273 {

  /********************************************************
  *********************************************************/
  public static void main (String[] args) {
      Locale reservedLocale = Locale.getDefault();
      try {
        int errors=0;

        Locale loc = new Locale ("sv", "se");   // Swedish

        Locale.setDefault (loc);
        Collator col = Collator.getInstance ();

        String[] data = {"A",
                         "Aa",
                         "Ae",
                         "B",
                         "Y",
                         "U\u0308", // U-umlaut
                         "Z",
                         "A\u030a", // A-ring
                         "A\u0308", // A-umlaut
                         "\u00c6", // AE ligature
                         "O\u0308", // O-umlaut
                         "a\u030b", // a-double-acute
                         "\u00d8", // O-stroke
                         "a",
                         "aa",
                         "ae",
                         "b",
                         "y",
                         "u\u0308", // u-umlaut
                         "z",
                         "A\u030b", // A-double-acute
                         "a\u030a", // a-ring
                         "a\u0308", // a-umlaut
                         "\u00e6", // ae ligature
                         "o\u0308", // o-umlaut
                         "\u00f8", // o-stroke
        };


        String[] sortedData = {"a",
                               "A",
                               "aa",
                               "Aa",
                               "ae",
                               "Ae",
                               "b",
                               "B",
                               "y",
                               "Y",
                               "u\u0308", // o-umlaut
                               "U\u0308", // o-umlaut
                               "z",
                               "Z",
                               "a\u030a", // a-ring
                               "A\u030a", // A-ring
                               "a\u0308", // a-umlaut
                               "A\u0308", // A-umlaut
                               "a\u030b", // a-double-acute
                               "A\u030b", // A-double-acute
                               "\u00e6", // ae ligature
                               "\u00c6", // AE ligature
                               "o\u0308", // o-umlaut
                               "O\u0308", // O-umlaut
                               "\u00f8", // o-stroke
                               "\u00d8", // O-stroke
        };

        Arrays.sort (data, col);

        System.out.println ("Using " + loc.getDisplayName());
        for (int i = 0;  i < data.length;  i++) {
            System.out.println(data[i] + "  :  " + sortedData[i]);
            if (sortedData[i].compareTo(data[i]) != 0) {
                errors++;
            }
        }//end for

        if (errors > 0)
            throw new RuntimeException("There are " + errors +
                        " words sorted incorrectly!");
      } finally {
          // restore the reserved locale
          Locale.setDefault(reservedLocale);
      }
  }//end main

}//end class CollatorTest
