/*
 * Copyright (c) 1997, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4061897
 * @summary Test for illegal argument exception
 */

import java.util.Vector;

/**
 * This is a simple test class created to check for
 * an exception when a new Vector is constructed with
 * illegal arguments
 */
public class IllegalConstructorArgs {

    public static void main(String[] args) {
        int testSucceeded=0;

        try {
            // this should generate an IllegalArgumentException
            Vector bad1 = new Vector(-100, 10);
        }
        catch (IllegalArgumentException e1) {
            testSucceeded =1;
        }
        catch (NegativeArraySizeException e2) {
            testSucceeded =0;
        }

        if (testSucceeded == 0)
            throw new RuntimeException("Wrong exception thrown.");

     }

}
