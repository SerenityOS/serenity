/*
 * Copyright (c) 2003, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Confirm that the decimal separator is shown when explicitly requested.
 * @bug 4208135
 */

import java.math.*;
import java.text.*;
import java.util.*;

public class Bug4208135 {

    static DecimalFormat df;

    static boolean err = false;

    static public void main(String[] args){

        Locale defaultLoc = Locale.getDefault();
        Locale.setDefault(Locale.US);

        df = new DecimalFormat();

        df.applyPattern("0.#E0");

        df.setDecimalSeparatorAlwaysShown(true);
        checkFormat(0.0, "0.E0");
        checkFormat(10.0, "1.E1");
        checkFormat(1000.0, "1.E3");
        checkFormat(0L, "0.E0");
        checkFormat(10L, "1.E1");
        checkFormat(1000L, "1.E3");
        checkFormat(new BigDecimal("0.0"), "0.E0");
        checkFormat(new BigDecimal("10.0"), "1.E1");
        checkFormat(new BigDecimal("1000.0"), "1.E3");
        checkFormat(new BigInteger("00"), "0.E0");
        checkFormat(new BigInteger("10"), "1.E1");
        checkFormat(new BigInteger("1000"), "1.E3");

        df.setDecimalSeparatorAlwaysShown(false);
        checkFormat(0.0, "0E0");
        checkFormat(10.0, "1E1");
        checkFormat(1000.0, "1E3");
        checkFormat(0L, "0E0");
        checkFormat(10L, "1E1");
        checkFormat(1000L, "1E3");
        checkFormat(new BigDecimal("0.0"), "0E0");
        checkFormat(new BigDecimal("10.0"), "1E1");
        checkFormat(new BigDecimal("1000.0"), "1E3");
        checkFormat(new BigInteger("0"), "0E0");
        checkFormat(new BigInteger("10"), "1E1");
        checkFormat(new BigInteger("1000"), "1E3");

        df.applyPattern("0.###");

        df.setDecimalSeparatorAlwaysShown(true);
        checkFormat(0.0, "0.");
        checkFormat(10.0, "10.");
        checkFormat(1000.0, "1000.");
        checkFormat(0L, "0.");
        checkFormat(10L, "10.");
        checkFormat(1000L, "1000.");
        checkFormat(new BigDecimal("0.0"), "0.");
        checkFormat(new BigDecimal("10.0"), "10.");
        checkFormat(new BigDecimal("1000.0"), "1000.");
        checkFormat(new BigInteger("0"), "0.");
        checkFormat(new BigInteger("10"), "10.");
        checkFormat(new BigInteger("1000"), "1000.");

        df.setDecimalSeparatorAlwaysShown(false);
        checkFormat(0.0, "0");
        checkFormat(10.0, "10");
        checkFormat(1000.0, "1000");
        checkFormat(0L, "0");
        checkFormat(10L, "10");
        checkFormat(1000L, "1000");
        checkFormat(new BigDecimal("0.0"), "0");
        checkFormat(new BigDecimal("10.0"), "10");
        checkFormat(new BigDecimal("1000.0"), "1000");
        checkFormat(new BigInteger("0"), "0");
        checkFormat(new BigInteger("10"), "10");
        checkFormat(new BigInteger("1000"), "1000");

        Locale.setDefault(defaultLoc);

        if (err) {
            throw new RuntimeException("Wrong format/parse with DecimalFormat");
        }
    }

    static void checkFormat(Number num, String expected) {
        String got = df.format(num);
        if (!got.equals(expected)) {
            err = true;
            System.err.println("    DecimalFormat format(" +
                               num.getClass().getName() +
                               ") error:" +
                               "\n\tnumber:           " + num +
                               "\n\tSeparatorShown? : " + df.isDecimalSeparatorAlwaysShown() +
                               "\n\tgot:              " + got +
                               "\n\texpected:         " + expected);
        }
    }
}
