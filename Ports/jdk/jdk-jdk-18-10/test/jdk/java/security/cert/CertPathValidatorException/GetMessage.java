/*
 * Copyright (c) 2008, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6765046
 * @summary check that getMessage(cause) returns cause.toString if cause != null
 */

import java.security.cert.CertPathValidatorException;

public class GetMessage {
    private static volatile boolean failed = false;
    public static void main(String[] args) throws Exception {

        Throwable[] causes = {
                new Throwable(),
                new Throwable("message"),
                new Throwable("message", new Throwable()) };

        for (Throwable cause: causes) {
            CertPathValidatorException cpve =
                new CertPathValidatorException(cause);

            // from CertPathValidatorException(Throwable cause) spec:
            // The detail message is set to (cause==null ? null : cause.toString() )
            // (which typically contains the class and detail message of cause).
            String expMsg = (cause == null ? null : cause.toString());
            String actualMsg = cpve.getMessage();

            boolean msgsEqual =
                (expMsg == null ? actualMsg == null : expMsg.equals(actualMsg));
            if (!msgsEqual) {
                System.out.println("expected message:" + expMsg);
                System.out.println("getMessage():" + actualMsg);
                failed = true;
            }
        }
        if (failed) {
            throw new Exception("Some tests FAILED");
        }
    }
}
