/*
 * Copyright (c) 2001, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 4427888
 * @summary Incorrect key usage check for server certificates
 */
import java.io.*;
import java.security.cert.*;
import java.util.Collection;
import java.util.List;

public class ExtKeyUsage {

    public static void main(String[] args) throws Exception {
        File f = new File(System.getProperty("test.src", "."),
            "certextkeyusage");
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        X509Certificate cert = (X509Certificate)
            cf.generateCertificate(new FileInputStream(f));
        List extKeyUsage = cert.getExtendedKeyUsage();
        if (extKeyUsage.size() != 1)
            throw new Exception("getExtendedKeyUsage()) returned an " +
                "unexpected number of entries: "+extKeyUsage.size());

        // Test immutability
        try {
            extKeyUsage.clear();
            throw new Exception("List returned by " +
                "getExtendedKeyUsage() is not immutable");
        } catch (UnsupportedOperationException e) {}

        if (!extKeyUsage.contains("1.3.6.1.5.5.7.3.1")) {
            throw new Exception("List returned by "+
                "getExtendedKeyUsage() doesn't contain expected entry");
        }
    }
}
