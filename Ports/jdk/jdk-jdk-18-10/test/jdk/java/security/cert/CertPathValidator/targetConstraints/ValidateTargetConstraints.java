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
 * @bug 4459538
 * @summary make sure that target constraints are processed correctly
 *      by a PKIX CertPathValidator
 */

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import java.math.BigInteger;

import java.security.cert.CertificateFactory;
import java.security.cert.CertPath;
import java.security.cert.CertPathValidator;
import java.security.cert.CertPathValidatorException;
import java.security.cert.CertPathValidatorResult;
import java.security.cert.PKIXParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.security.cert.X509CertSelector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * ValidateTargetConstraints performs a simple validation of a certification
 * path, but adds a requirement that the serial number of the last
 * certificate match an arbitrarily chosen number. This should cause the
 * validation to fail.
 *
 * @author      Steve Hanna
 * @author      Sean Mullan
 */
public final class ValidateTargetConstraints {

    private static CertPath path;
    private static PKIXParameters params;

    public static void main(String[] args) throws Exception {

        String[] certs = { "sun.cer", "sun2labs1.cer" };

        try {
            createPath(certs);
            validate(path, params);
            throw new Exception
                ("CertPath should not have been validated succesfully");
        } catch (CertPathValidatorException cpve) {
            System.out.println("Test failed as expected: " + cpve);
        }
    }

    public static void createPath(String[] certs) throws Exception {
        TrustAnchor anchor = new TrustAnchor(getCertFromFile(certs[0]), null);
        List list = new ArrayList();
        for (int i = 1; i < certs.length; i++) {
            list.add(0, getCertFromFile(certs[i]));
        }
        CertificateFactory cf = CertificateFactory.getInstance("X509");
        path = cf.generateCertPath(list);

        Set anchors = Collections.singleton(anchor);
        params = new PKIXParameters(anchors);
        params.setRevocationEnabled(false);
        X509CertSelector sel = new X509CertSelector();
        sel.setSerialNumber(new BigInteger("1427"));
        params.setTargetCertConstraints(sel);
    }

    /**
     * Get a DER-encoded X.509 certificate from a file.
     *
     * @param certFilePath path to file containing DER-encoded certificate
     * @return X509Certificate
     * @throws IOException on error
     */
    public static X509Certificate getCertFromFile(String certFilePath)
        throws IOException {
            X509Certificate cert = null;
            try {
                File certFile = new File(System.getProperty("test.src", "."),
                    certFilePath);
                FileInputStream certFileInputStream =
                    new FileInputStream(certFile);
                CertificateFactory cf = CertificateFactory.getInstance("X509");
                cert = (X509Certificate)
                    cf.generateCertificate(certFileInputStream);
            } catch (Exception e) {
                e.printStackTrace();
                throw new IOException("Can't construct X509Certificate: " +
                                      e.getMessage());
            }
            return cert;
    }

    /**
     * Perform a PKIX validation.
     *
     * @param path CertPath to validate
     * @param params PKIXParameters to use in validation
     * @throws Exception on error
     */
    public static void validate(CertPath path, PKIXParameters params)
        throws Exception {
        CertPathValidator validator =
            CertPathValidator.getInstance("PKIX");
        CertPathValidatorResult cpvr = validator.validate(path, params);
    }
}
