/*
 * Copyright (c) 2000, 2016, Oracle and/or its affiliates. All rights reserved.
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

import java.io.*;
import java.util.*;
import java.text.*;

/**
 * @test
 * @bug 4358730
 * @library /java/text/testlib
 * @summary test that confirms Zero-Padding on year.
 */

public class bug4358730 extends IntlTest {

    public static void main(String[] args) throws Exception {
        new bug4358730().run(args);
    }

    String[] patterns = {"y",    "yy", "yyy",  "yyyy", "yyyyy"};
    String[][] data = {
        /* 2 A.D. */    {"2",    "02", "002",  "0002", "00002"},
        /* 20 A.D. */   {"20",   "20", "020",  "0020", "00020"},
        /* 200 A.D. */  {"200",  "00", "200",  "0200", "00200"},
        /* 2000 A.D. */ {"2000", "00", "2000", "2000", "02000"},
    };
    int[] year = {2, 20, 200, 2000};

    int datasize = data.length;
    int nPatterns = data[0].length;

    public void Test4358730() {
        TimeZone saveZone = TimeZone.getDefault();
        Locale saveLocale = Locale.getDefault();

        try {
            TimeZone.setDefault(TimeZone.getTimeZone("PST"));
            Locale.setDefault(new Locale("en", "US"));
            SimpleDateFormat sdf = new SimpleDateFormat();

            for (int i = 0; i < datasize; i++) {
                @SuppressWarnings("deprecation")
                Date d = new Date(year[i]-1900, 10, 15);
                for (int j = 0; j < nPatterns; j++) {
                    sdf.applyPattern(patterns[j]);
                    if (!data[i][j].equals(sdf.format(d))) {
                        errln("Invalid format : " + sdf.format(d) +
                            ", expected : " + data[i][j]);
                    }
                }
            }
        }
        finally {
            TimeZone.setDefault(saveZone);
            Locale.setDefault(saveLocale);
        }
    }
}
