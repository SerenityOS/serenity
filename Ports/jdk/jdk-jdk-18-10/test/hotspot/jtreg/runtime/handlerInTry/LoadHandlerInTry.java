/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8075118
 * @summary JVM stuck in infinite loop during verification
 * @compile HandlerInTry.jasm
 * @compile IsolatedHandlerInTry.jasm
 * @run main/othervm -Xverify:all LoadHandlerInTry
 */

/*
 * This test has two cases:
 *
 * 1. class HandlerInTry:  Class HandlerInTry contains a TRY block in a
 *    constructor whose handler is inside the same TRY block.  The last
 *    few bytecodes and exception table look like this:
 *
 *         ...
 *      87: athrow
 *      88: astore        4
 *      90: invokestatic  #9
 *      93: aload         4
 *      95: athrow
 *      96: return
 *    Exception table:
 *       from    to  target type
 *          36    46    53   Class java/lang/Throwable
 *          36    46    88   any
 *          53    90    88   any
 *
 * Note that the target for the third handler in the Exception table is
 * inside its TRY block.
 * Without the fix for bug JDK-8075118, this test will time out.
 *
 *
 * 2. class IsolatedHandlerInTry: Class IsolatedHandlerInTry also contains
 *    a TRY block in a constructoer whose handler is inside its TRY block.
 *    But the handler is only reachable if an exception is thrown.  The
 *    handler's bytecodes will not get parsed as part of parsing the TRY
 *    block.  They will only get parsed as a handler for the TRY block.
 *    Since the isolated handler does a 'return', a VerifyError exception
 *    should get thrown.
 */

public class LoadHandlerInTry {

    public static void main(String[] args) throws Exception {
        System.out.println("Regression test for bug 8075118");
        try {
            Class newClass = Class.forName("HandlerInTry");
            throw new RuntimeException(
                 "Failed to throw VerifyError for HandlerInTry");
        } catch (java.lang.VerifyError e) {
            System.out.println("Passed: VerifyError exception was thrown");
        }

        try {
            Class newClass = Class.forName("IsolatedHandlerInTry");
            throw new RuntimeException(
                 "Failed to throw VerifyError for IsolatedHandlerInTry");
        } catch (java.lang.VerifyError e) {
            System.out.println("Passed: VerifyError exception was thrown");
        }
    }
}
