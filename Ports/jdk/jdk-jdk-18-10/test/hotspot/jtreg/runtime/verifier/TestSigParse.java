/*
 * Copyright (c) 2019, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8219579
 * @summary Test that signatures are properly parsed when verification of local
 *          classes is requested but verification of remote classes is not.
 * @compile BadSignatures.jcod
 * @run main/othervm -XX:+UnlockDiagnosticVMOptions -XX:+BytecodeVerificationLocal -XX:-BytecodeVerificationRemote TestSigParse
 */

public class TestSigParse {

    public static void main(String args[]) throws Throwable {
        System.out.println("Regression test for bug 8219579");

        // Test a FieldRef with a bad signature.
        try {
            Class newClass = Class.forName("BadFieldRef");
            throw new RuntimeException("Expected ClasFormatError exception not thrown");
        } catch (java.lang.ClassFormatError e) {
            String eMsg = e.getMessage();
            if (!eMsg.contains("Field") || !eMsg.contains("has illegal signature")) {
                throw new RuntimeException("Unexpected exception: " + eMsg);
            }
        }

        // Test a MethodRef with a bad signature.
        try {
            Class newClass = Class.forName("BadMethodRef");
            throw new RuntimeException("Expected ClasFormatError exception not thrown");
        } catch (java.lang.ClassFormatError e) {
            String eMsg = e.getMessage();
            if (!eMsg.contains("Method") || !eMsg.contains("has illegal signature")) {
                throw new RuntimeException("Unexpected exception: " + eMsg);
            }
        }

        // Test a method in a class with a bad signature.
        try {
            Class newClass = Class.forName("BadMethodSig");
            throw new RuntimeException("Expected ClasFormatError exception not thrown");
        } catch (java.lang.ClassFormatError e) {
            String eMsg = e.getMessage();
            if (!eMsg.contains("Class name is empty or contains illegal character")) {
                throw new RuntimeException("Unexpected exception: " + eMsg);
            }
        }
    }

}
