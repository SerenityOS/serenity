/*
 * Copyright (c) 2000, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4307323
 * @summary Test URLDecoder on strings that are not legally
 * encoded. Incomplete escape patterns or illegal hex characters will cause
 * an exception to be thrown. Otherwise, illegal characters will be
 * silently accepted.
 *
 */

import java.net.URLDecoder;

public class DecodeNonEncoded {

    static String[] errorStrings
        = {"%", "%A", "Hello%", "%xy", "%az", "%ab%q"};
    static String[] ignoreStrings = {"#", "X@Y", "Hello There"};

    public static void main(String[] args) throws Exception {

        for (int i = 0; i < errorStrings.length; i++) {
            try {
                URLDecoder.decode(errorStrings[i]);
                throw new Exception("String \"" + errorStrings[i]
                    + "\" should have failed in URLDecoder.decode!");
            } catch (IllegalArgumentException e) {
                // All ok. Got to next string.
                System.out.println("String \"" + errorStrings[i]
                   + "\" correctly threw IllegalArgumentException: "
                   + e.getMessage());
            }
        }

        String temp;
        for (int i = 0; i < ignoreStrings.length; i++) {
            temp = URLDecoder.decode(ignoreStrings[i]);
            if (!temp.equals(ignoreStrings[i]))
                throw new Exception("String \"" + ignoreStrings[i]
                        + "\" was converted to " + temp
                        +" by URLDecoder.decode to ");
            else
                System.out.println("String \"" + temp
                      + "\" was left unchanged by URLDecoder.decode.");
        }

    }
}
