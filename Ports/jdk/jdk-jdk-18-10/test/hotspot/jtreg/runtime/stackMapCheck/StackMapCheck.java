/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7127066
 * @summary Class verifier accepts an invalid class file
 * @compile BadMap.jasm
 * @compile BadMapDstore.jasm
 * @compile BadMapIstore.jasm
 * @run main/othervm -Xverify:all StackMapCheck
 */

public class StackMapCheck {
    public static void main(String args[]) throws Throwable {

        System.out.println("Regression test for bug 7127066");
        try {
            Class newClass = Class.forName("BadMap");
            throw new RuntimeException(
                "StackMapCheck failed, BadMap did not throw VerifyError");
        } catch (java.lang.VerifyError e) {
            System.out.println("BadMap passed, VerifyError was thrown");
        }

        try {
            Class newClass = Class.forName("BadMapDstore");
            throw new RuntimeException(
                "StackMapCheck failed, BadMapDstore did not throw VerifyError");
        } catch (java.lang.VerifyError e) {
            System.out.println("BadMapDstore passed, VerifyError was thrown");
        }

        try {
            Class newClass = Class.forName("BadMapIstore");
            throw new RuntimeException(
                "StackMapCheck failed, BadMapIstore did not throw VerifyError");
        } catch (java.lang.VerifyError e) {
            System.out.println("BadMapIstore passed, VerifyError was thrown");
        }
    }
}
