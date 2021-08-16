/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6483657 8154113
 * @requires os.family == "windows"
 * @library /test/lib
 * @summary Test "keytool -list" displays correctly same named certificates
 * @ignore Uses certutil.exe that isn't guaranteed to be installed
 */

import jdk.test.lib.process.ProcessTools;

import java.security.KeyStore;
import java.util.Collections;

public class NonUniqueAliases {
    public static void main(String[] args) throws Throwable {

        try {
            String testSrc = System.getProperty("test.src", ".");

            // removing the alias NonUniqueName if it already exists
            ProcessTools.executeCommand("certutil", "-user", "-delstore", "MY",
                    "NonUniqueName");

            // Importing 1st certificate into MY keystore using certutil tool
            ProcessTools.executeCommand("certutil", "-user", "-addstore", "MY",
                    testSrc + "/nonUniq1.pem");

            // Importing 2nd certificate into MY keystore using certutil tool
            ProcessTools.executeCommand("certutil", "-user", "-addstore", "MY",
                    testSrc + "/nonUniq2.pem");

            // Now we have 2
            checkCount(1, 1);

            ProcessTools.executeCommand("certutil", "-user", "-delstore", "MY",
                    "NonUniqueName");

            // Now we have 2
            checkCount(0, 0);
        } finally {
            ProcessTools.executeCommand("certutil", "-user", "-delstore", "MY",
                    "NonUniqueName");
        }
    }

    static void checkCount(int c0, int c1) throws Exception {

        KeyStore ks = KeyStore.getInstance("Windows-MY");
        ks.load(null, null);

        int count0 = 0, count1 = 0;
        for (String alias : Collections.list(ks.aliases())) {
            if (alias.equals("NonUniqueName")) {
                count0++;
            }
            if (alias.equals("NonUniqueName (1)")) {
                count1++;
            }
        }
        if (count0 != c0) {
            throw new Exception("error: unexpected number of entries ("
                    + count0 + ") in the Windows-MY store");
        }
        if (count1 != c1) {
            throw new Exception("error: unexpected number of entries ("
                    + count1 + ") in the Windows-MY store");
        }
    }
}
