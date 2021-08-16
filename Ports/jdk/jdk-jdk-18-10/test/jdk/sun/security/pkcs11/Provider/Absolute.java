/*
 * Copyright (c) 2011, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7003952 7191662
 * @library /test/lib ..
 * @modules jdk.crypto.cryptoki
 * @summary load DLLs and launch executables using fully qualified path
 */

import java.security.InvalidParameterException;
import java.security.Provider;

public class Absolute {

    public static void main(String[] args) throws Exception {
        String config =
            System.getProperty("test.src", ".") + "/Absolute.cfg";

        try {
            Provider p = PKCS11Test.getSunPKCS11(config);
            if (p == null) {
                System.out.println("Skipping test - no PKCS11 provider available");
            }
        } catch (InvalidParameterException ipe) {
            Throwable ex = ipe.getCause();
            if (ex.getMessage().indexOf(
                    "Absolute path required for library value:") != -1) {
                System.out.println("Test Passed: expected exception thrown");
            } else {
                // rethrow
                throw ipe;
            }
        }
    }
}
