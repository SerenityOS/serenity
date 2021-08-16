/*
 * Copyright (c) 2011, 2012, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7024771 7024604
 * @summary various X500Principal DN parsing tests
 */

import javax.security.auth.x500.X500Principal;

public class Parse {

    private static TestCase[] testCases = {
        new TestCase("CN=prefix\\<>suffix", false),
        new TestCase("OID.1=value", false),
        new TestCase("oid.1=value", false),
        new TestCase("OID.1.2=value", true),
        new TestCase("oid.1.2=value", true),
        new TestCase("1=value", false),
        new TestCase("1.2=value", true)
    };

    public static void main(String args[]) throws Exception {
        for (TestCase testCase : testCases) {
            testCase.run();
        }
        System.out.println("Test completed ok.");
    }
}

class TestCase {

     private String name;
     private boolean expectedResult;

     TestCase(String name, boolean expectedResult) {
         this.name = name;
         this.expectedResult = expectedResult;
     }

     void run() throws Exception {
         Exception f = null;
         try {
             System.out.println("Parsing: \"" + name + "\"");
             new X500Principal(name);
             if (expectedResult == false) {
                 f = new Exception("Successfully parsed invalid name");
             }
         } catch (IllegalArgumentException e) {
             if (expectedResult == true) {
                 throw e;
             }
         }
         if (f != null) {
             throw f;
         }
     }
}
