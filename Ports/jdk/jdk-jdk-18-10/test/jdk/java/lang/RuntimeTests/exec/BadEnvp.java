/*
 * Copyright (c) 2003, 2019, Oracle and/or its affiliates. All rights reserved.
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

/* @test
   @bug 4925610
   @summary Check for the appropriate exceptions when a bad envp is passed.
   @author Martin Buchholz
*/

public class BadEnvp {

    public static void main(String[] args) throws Exception {
        Runtime r = Runtime.getRuntime();
        java.io.File dir = new java.io.File(".");

        String[] envpWithNull = {"FOO=BAR",null};
        try {
            r.exec("echo", envpWithNull);
            throw new Exception("Expected NullPointerException not thrown");
        } catch (NullPointerException e) {} // OK

        try {
            r.exec("echo", envpWithNull, dir);
            throw new Exception("Expected NullPointerException not thrown");
        } catch (NullPointerException e) {} // OK

        try {
            r.exec(new String[]{"echo"}, envpWithNull);
            throw new Exception("Expected NullPointerException not thrown");
        } catch (NullPointerException e) {} // OK

        try {
            r.exec(new String[]{"echo"}, envpWithNull, dir);
            throw new Exception("Expected NullPointerException not thrown");
        } catch (NullPointerException e) {} // OK
    }
}
