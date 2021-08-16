/*
 * Copyright (c) 2001, 2014, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8028431 8028591
 * @summary Make sure that proper CertificateException is thrown
 *          when loading bad x509 certificate
 * @author Artem Smotrakov
 */

import java.io.File;
import java.io.FileInputStream;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.cert.CertificateException;

public class X509BadCertificate {

    public static void main(String[] args) throws Exception {
        test("bad-cert-1.pem");
        test("bad-cert-2.pem");
    }

    /**
     * Parse X509 certificates.
     */
    static void test(String filename) throws Exception {
        try {
            System.out.println("Parse file " + filename);
            File f = new File(System.getProperty("test.src", "."), filename);
            try (FileInputStream fis = new FileInputStream(f)) {
                CertificateFactory cf = CertificateFactory.getInstance("X509");
                X509Certificate cert = (X509Certificate)
                cf.generateCertificate(fis);
            }
            throw new Exception("Test failed: " +
                "expected CertificateParsingException was not thrown");
        } catch (CertificateException e) {
            System.out.println("Test passed: expected exception was thrown: " +
                e.toString());
        }
    }
}
