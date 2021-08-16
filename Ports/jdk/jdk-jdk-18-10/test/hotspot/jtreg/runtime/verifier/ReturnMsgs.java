/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8262368
 * @summary Test that VerifyError messages are correct when return bytecodes
 *          andd method signatures do not match.
 * @compile Returns.jasm
 * @run main/othervm -Xverify ReturnMsgs
 */

public class ReturnMsgs {

    public static void main(String args[]) throws Throwable {
        System.out.println("Regression test for bug 8262368");

        try {
            // Test message for class with a void return type in its method
            // descriptor, containing an 'ireturn' bytecode.
            Class newClass = Class.forName("VoidReturnSignature");
            throw new RuntimeException("Expected VerifyError exception not thrown");
        } catch (java.lang.VerifyError e) {
            String eMsg = e.getMessage();
            if (!eMsg.contains("Method does not expect a return value")) {
                throw new RuntimeException("Unexpected exception message: " + eMsg);
            }
        }

        try {
            // Test message for class with a non-void return type in its
            // method descriptor, containing a 'return' bytecode.
            Class newClass = Class.forName("NonVoidReturnSignature");
            throw new RuntimeException("Expected VerifyError exception not thrown");
        } catch (java.lang.VerifyError e) {
            String eMsg = e.getMessage();
            if (!eMsg.contains("Method expects a return value")) {
                throw new RuntimeException("Unexpected exception message: " + eMsg);
            }
        }
    }
}
