/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8165466
 * @summary Checks the subsequent function calls of the DecimalFormat.format()
 *          method in which the minimumFractionDigit is set to 0 and one of
 *          the format() call include formatting of the number with zero
 *          fraction value e.g. 0.00, 9.00
 */

import java.text.DecimalFormat;
import java.util.Locale;

public class Bug8165466 {

    public static void main(String[] args) {
        DecimalFormat nf = (DecimalFormat) DecimalFormat
                .getPercentInstance(Locale.US);
        nf.setMaximumFractionDigits(3);
        nf.setMinimumFractionDigits(0);
        nf.setMultiplier(1);

        double d = 0.005678;
        String result = nf.format(d);
        if (!result.equals("0.006%")) {
            throw new RuntimeException("[Failed while formatting the double"
                    + " value: " + d + " Expected: 0.006%, Found: " + result
                    + "]");
        }

        d = 0.00;
        result = nf.format(d);
        if (!result.equals("0%")) {
            throw new RuntimeException("[Failed while formatting the double"
                    + " value: " + d + " Expected: 0%, Found: " + result
                    + "]");
        }

        d = 0.005678;
        result = nf.format(d);
        if (!result.equals("0.006%")) {
            throw new RuntimeException("[Failed while formatting the double"
                    + " value: " + d + " Expected: 0.006%, Found: " + result
                    + "]");
        }

        //checking with the non zero value
        d = 0.005678;
        result = nf.format(d);
        if (!result.equals("0.006%")) {
            throw new RuntimeException("[Failed while formatting the double"
                    + " value: " + d + " Expected: 0.006%, Found: " + result
                    + "]");
        }

        d = 9.00;
        result = nf.format(d);
        if (!result.equals("9%")) {
            throw new RuntimeException("[Failed while formatting the double"
                    + " value: " + d + " Expected: 9%, Found: " + result
                    + "]");
        }

        d = 0.005678;
        result = nf.format(d);
        if (!result.equals("0.006%")) {
            throw new RuntimeException("[Failed while formatting the double"
                    + " value: " + d + " Expected: 0.006%, Found: " + result
                    + "]");
        }
    }

}

