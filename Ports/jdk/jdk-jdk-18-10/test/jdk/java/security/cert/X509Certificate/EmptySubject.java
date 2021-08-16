/*
 * Copyright (c) 2001, 2002, Oracle and/or its affiliates. All rights reserved.
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
 * @test 1.2, 01/06/27
 * @bug 4474914
 *
 * @summary getSubjectDN and getSubjectX500Principal are underspecified
 *              if subject is empty
 */
import java.io.*;
import java.security.Principal;
import java.security.cert.*;
import javax.security.auth.x500.X500Principal;

public class EmptySubject {

    public static void main(String[] args) throws Exception {

        try {
            File f = new File(System.getProperty("test.src", "."),
                "emptySubjectCert");
            CertificateFactory cf = CertificateFactory.getInstance("X.509");

            try {
                X509Certificate cert = (X509Certificate)
                    cf.generateCertificate(new FileInputStream(f));
                throw new Exception("Test 1 Failed - parsed invalid cert");
            } catch (CertificateParsingException e) {
                System.out.println("Test 1 passed: " + e.toString());
            }

            f = new File(System.getProperty("test.src", "."),
                "emptyIssuerCert");

            try {
                X509Certificate cert2 = (X509Certificate)
                    cf.generateCertificate(new FileInputStream(f));
                throw new Exception("Test 2 Failed - parsed invalid cert");
            } catch (CertificateParsingException e) {
                System.out.println("Test 2 passed: " + e.toString());
            }
        } catch (Exception e) {
            SecurityException se = new SecurityException("Test Failed");
            se.initCause(e);
            throw se;
        }
    }
}
