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
 *
 */

/*
 * @test
 * @bug 8149607
 * @summary Throw VerifyError when popping a stack element of TOP
 * @compile popDupSwapTests.jasm
 * @run main/othervm -Xverify:all PopDupTop
 */

public class PopDupTop {

    public static void testClass(String class_name, String msg) throws Throwable {
        try {
            Class newClass = Class.forName(class_name);
            throw new RuntimeException("Expected VerifyError exception not thrown for " + msg);
        } catch (java.lang.VerifyError e) {
            if (!e.getMessage().contains("Bad type on operand stack")) {
               throw new RuntimeException(
                   "Unexpected VerifyError message for " + msg + ": " + e.getMessage());
            }
        }
    }

    public static void main(String args[]) throws Throwable {
        System.out.println("Regression test for bug 8149607");

        testClass("dup_x1", "dup_x1 of long,ref");
        testClass("dup2toptop", "dup2 of top,top");
        testClass("dup2longtop", "dup2 of long,top");
        testClass("dup2_x1", "dup2_x1 long,ref,ref");
        testClass("dup2_x2", "dup2_x2 top");
        testClass("dup2_x2_long_refs", "dup2_x2 long,ref,ref,ref");
        testClass("poptop", "pop of top");
        testClass("poptoptop", "pop of top,top");
        testClass("pop2toptop", "pop2 of top,top");
        testClass("pop2longtop", "pop2 of long,top");
        testClass("swaptoptop", "swap of top,top");
        testClass("swapinttop", "swap of int,top");
        testClass("swaptopint", "swap of top,int");
    }
}
