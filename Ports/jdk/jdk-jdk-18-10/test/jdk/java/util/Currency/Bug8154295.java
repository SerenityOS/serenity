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
 * @bug 8154295
 * @summary Check getNumericCodeAsString() method which returns numeric code as a 3 digit String.
 */

import java.util.Currency;

public class Bug8154295 {

    public static void main(String[] args) {

        String numericCode = Currency.getInstance("AFA").getNumericCodeAsString();
        if (!numericCode.equals("004")) { //should return "004" (a 3 digit string)
           throw new RuntimeException("[Expected 004, "
                + "found "+numericCode+" for AFA]");
        }

        numericCode = Currency.getInstance("AUD").getNumericCodeAsString();
        if (!numericCode.equals("036")) { //should return "036" (a 3 digit string)
            throw new RuntimeException("[Expected 036, "
                + "found "+numericCode+" for AUD]");
        }

        numericCode = Currency.getInstance("USD").getNumericCodeAsString();
        if (!numericCode.equals("840")) {// should return "840" (a 3 digit string)
            throw new RuntimeException("[Expected 840, "
                + "found "+numericCode+" for USD]");
        }

    }

}
