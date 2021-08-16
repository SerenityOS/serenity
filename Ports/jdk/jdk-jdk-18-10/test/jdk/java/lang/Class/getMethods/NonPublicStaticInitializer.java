/*
 * Copyright (c) 1999, Oracle and/or its affiliates. All rights reserved.
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
   @bug 4187388
   @summary <clinit> in interfaces need not be public
*/

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

interface TestedInterface {
    String s = System.getProperty("Test");

    void foo();
    void bar();
}


public class NonPublicStaticInitializer {
    public static void main(String args[]) throws Exception {
        Method m[] = TestedInterface.class.getMethods();
        for (int i = 0; i < m.length; i++) {
            System.out.println("Found: " +
                               Modifier.toString(m[i].getModifiers()) +
                               " " + m[i].getName());
            if (m[i].getName().equals("<clinit>")) {
                throw new Exception("Shouldn't have found <clinit>");
            }
        }
    }
}
