/*
 * Copyright (c) 2005, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug        6287579
 * @summary    SubClasses of ListResourceBundle should fix getContents()
 *
 * @modules jdk.jdi/com.sun.tools.example.debug.tty
 *
 * @compile --add-exports jdk.jdi/com.sun.tools.example.debug.tty=ALL-UNNAMED -g ImmutableResourceTest.java
 *
 * @run main/othervm --add-exports jdk.jdi/com.sun.tools.example.debug.tty=ALL-UNNAMED ImmutableResourceTest
 */

import java.util.ResourceBundle;

public class ImmutableResourceTest {

    public static void main(String[] args) throws Exception {
        /* Reach under the covers and get the message strings */
        com.sun.tools.example.debug.tty.TTYResources ttyr =
            new com.sun.tools.example.debug.tty.TTYResources ();
        Object [][] testData = ttyr.getContents();

        /* Shred our copy of the message strings */
        for (int ii = 0; ii < testData.length; ii++) {
            testData[ii][0] = "T6287579";
            testData[ii][1] = "yyy";
        }

        /*
         * Try to lookup the shredded key.
         * If this is successful we have a problem.
         */
        String ss = null;
        try {
            ss = ttyr.getString("T6287579");
        } catch (java.util.MissingResourceException mre) {
            /*
             * Ignore the expected exception since key "T6287579" is
             * not in the canonical TTYResources.
             */
        }
        if ("yyy".equals(ss)) {
            throw new Exception ("SubClasses of ListResourceBundle should fix getContents()");
        }
        System.out.println("...Finished.");
    }
}
