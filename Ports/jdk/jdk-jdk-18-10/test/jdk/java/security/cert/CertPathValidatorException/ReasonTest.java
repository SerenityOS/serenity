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
 * @bug 6465942
 * @summary unit test for CertPathValidatorException.Reason
 */

import java.security.cert.CertPathValidatorException;
import java.security.cert.CertPathValidatorException.BasicReason;

public class ReasonTest {
    private static volatile boolean failed = false;
    public static void main(String[] args) throws Exception {

        // check that getReason returns UNSPECIFIED if reason not specified
        CertPathValidatorException cpve = new CertPathValidatorException("abc");
        if (cpve.getReason() != BasicReason.UNSPECIFIED) {
            failed = true;
            System.err.println("FAILED: unexpected reason: " + cpve.getReason());
        }

        // check that getReason returns specified reason
        cpve = new CertPathValidatorException
            ("abc", null, null, -1, BasicReason.REVOKED);
        if (cpve.getReason() != BasicReason.REVOKED) {
            failed = true;
            System.err.println("FAILED: unexpected reason: " + cpve.getReason());
        }

        // check that ctor throws NPE when reason is null
        try {
            cpve = new CertPathValidatorException("abc", null, null, -1, null);
            failed = true;
            System.err.println("ctor did not throw NPE for null reason");
        } catch (Exception e) {
            if (!(e instanceof NullPointerException)) {
                failed = true;
                System.err.println("FAILED: unexpected exception: " + e);
            }
        }
        if (failed) {
            throw new Exception("Some tests FAILED");
        }
    }
}
