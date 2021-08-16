/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7196316
 * @summary Confirm that a non-default rounding mode is used even after deserialization.
 */


import java.io.*;
import java.math.*;
import java.text.*;

public class Bug7196316 {

    private static final String filename = "bug7196316.ser";

    public static void main(String[] args) throws Exception {
        DecimalFormat df;
        RoundingMode mode = RoundingMode.DOWN;
        double given = 6.6;
        String expected;
        String actual;

        try (ObjectOutputStream os
                 = new ObjectOutputStream(new FileOutputStream(filename))) {
            df = new DecimalFormat("#");
            df.setRoundingMode(mode);
            expected = df.format(given);
            os.writeObject(df);
        }

        try (ObjectInputStream is
                 = new ObjectInputStream(new FileInputStream(filename))) {
            df = (DecimalFormat)is.readObject();
        }

        RoundingMode newMode = df.getRoundingMode();
        if (mode != newMode) {
            throw new RuntimeException("Unexpected roundig mode: " + newMode);
        } else {
            actual = df.format(given);
            if (!expected.equals(actual)) {
                throw new RuntimeException("Unexpected formatted result: \""
                              + actual + "\"");
            } else {
                System.out.println("Passed: Expected rounding mode (" + newMode
                    + ") & formatted result: \"" + actual + "\"");
            }
        }
    }

}
