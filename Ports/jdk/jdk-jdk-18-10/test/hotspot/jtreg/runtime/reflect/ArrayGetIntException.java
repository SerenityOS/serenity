/*
 * Copyright (c) 2014, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6191224
 * @summary (reflect) Misleading detail string in IllegalArgumentException thrown by Array.get<Type>
 * @run main ArrayGetIntException
 */
import java.io.*;
import java.lang.reflect.Array;

public class ArrayGetIntException {
    public static void main(String[] args) throws Exception {
        Object[] objArray = {Integer.valueOf(Integer.MAX_VALUE)};

        // this access is legal
        try {
            System.out.println(Array.get(objArray, 0));
            System.out.println("Test #1 PASSES");
        } catch(Exception e) {
            failTest("Test #1 FAILS - legal access denied" + e.getMessage());
        }

        // this access is not legal, but needs to generate the proper exception message
        try {
            System.out.println(Array.getInt(objArray, 0));
            failTest("Test #2 FAILS - no exception");
        } catch(Exception e) {
            System.out.println(e);
            if (e.getMessage().equals("Argument is not an array of primitive type")) {
                System.out.println("Test #2 PASSES");
            } else {
                failTest("Test #2 FAILS - incorrect message: " + e.getMessage());
            }
        }

        // this access is not legal, but needs to generate the proper exception message
        try {
            System.out.println(Array.getInt(new Object(), 0));
            failTest("Test #3 FAILS - no exception");
        } catch(Exception e) {
            System.out.println(e);
            if (e.getMessage().equals("Argument is not an array")) {
                System.out.println("Test #3 PASSES");
            } else {
                failTest("Test #3 FAILS - incorrect message: " + e.getMessage());
            }
        }
    }

    private static void failTest(String errStr) {
        System.out.println(errStr);
        throw new Error(errStr);
    }
}
