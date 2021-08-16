/*
 * Copyright (c) 2003, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4765281
 * @summary provide accessor methods for UnresolvedPermission
 */

import java.io.*;
import java.security.UnresolvedPermission;
import java.security.cert.*;

public class AccessorMethods {

    private static final String SIGNER1 = "AccessorMethods.signer1";
    private static final String SIGNER2 = "AccessorMethods.signer2";
    private static final String CA = "AccessorMethods.ca";

    public static void main(String[] args) throws Exception {

        // set CA cert in chain
        File f = new File(System.getProperty("test.src", "."), CA);
        FileInputStream fis = new FileInputStream(f);
        CertificateFactory fac = CertificateFactory.getInstance("X.509");
        Certificate cacert = fac.generateCertificate(fis);
        Certificate[] signercerts = new Certificate[4];
        signercerts[1] = cacert;
        signercerts[3] = cacert;

        // set signer certs
        f = new File(System.getProperty("test.src", "."), SIGNER1);
        fis = new FileInputStream(f);
        Certificate signer1 = fac.generateCertificate(fis);
        signercerts[0] = signer1;

        f = new File(System.getProperty("test.src", "."), SIGNER2);
        fis = new FileInputStream(f);
        Certificate signer2 = fac.generateCertificate(fis);
        signercerts[2] = signer2;

        UnresolvedPermission up = new UnresolvedPermission
                        ("type", "name", "actions", signercerts);
        if (!up.getUnresolvedType().equals("type") ||
            !up.getUnresolvedName().equals("name") ||
            !up.getUnresolvedActions().equals("actions")) {
            throw new SecurityException("Test 1 Failed");
        }

        Certificate[] certs = up.getUnresolvedCerts();
        if (certs == null || certs.length != 2) {
            throw new SecurityException("Test 2 Failed");
        }

        boolean foundSigner1 = false;
        boolean foundSigner2 = false;
        if (certs[0].equals(signer1) || certs[1].equals(signer1)) {
            foundSigner1 = true;
        }
        if (certs[0].equals(signer2) || certs[1].equals(signer2)) {
            foundSigner2 = true;
        }
        if (!foundSigner1 || !foundSigner2) {
            throw new SecurityException("Test 3 Failed");
        }
    }
}
