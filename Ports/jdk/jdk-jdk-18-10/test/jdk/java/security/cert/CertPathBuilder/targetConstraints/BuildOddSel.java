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
 * @summary make sure a PKIX CertPathBuilder throws an
 *      InvalidAlgorithmParameterException if the target constraints
 *      specified in the PKIXBuilderParameters is not an instance of
 *      X509CertSelector.
 */

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import java.security.InvalidAlgorithmParameterException;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.CertPathBuilder;
import java.security.cert.CertPathBuilderResult;
import java.security.cert.PKIXBuilderParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.security.cert.CertSelector;

import java.util.Collections;
import java.util.Set;

/**
 * BuildOddSel tries to perform a simple build of a certification path
 * using the PKIX algorithm and a bogus target constraints CertSelector
 * (one that is not an instance of X509CertSelector). On success, it should
 * throw an InvalidAlgorithmParameterException.
 *
 * @author      Steve Hanna
 * @author      Sean Mullan
 */
public final class BuildOddSel {

    private static PKIXBuilderParameters params;
    private static CertSelector sel;

    public static void main(String[] args) throws Exception {

        try {
            createParams();
            build(params);
            throw new Exception
                ("CertPath should not have been built successfully");
        } catch (InvalidAlgorithmParameterException iape) {
        }
    }

    /**
     * CertSelector class that should cause SunCertPathBuilder to
     * throw an InvalidAlgorithmParameterException.
     */
    static class OddSel implements CertSelector {
        public Object clone() {
            try {
                return super.clone();
            } catch (CloneNotSupportedException e) {
                throw new UnknownError();
            }
        }
        public boolean match(Certificate cert) {
            return(false);
        }
    }

    public static void createParams() throws Exception {
        TrustAnchor anchor = new TrustAnchor(getCertFromFile("sun.cer"), null);
        Set anchors = Collections.singleton(anchor);
        // Create odd CertSelector
        sel = new OddSel();
        params = new PKIXBuilderParameters(anchors, sel);
        params.setRevocationEnabled(false);
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
     * Perform a PKIX build.
     *
     * @param params PKIXBuilderParameters to use in building
     * @throws Exception on error
     */
    public static void build(PKIXBuilderParameters params)
        throws Exception {
        CertPathBuilder builder =
            CertPathBuilder.getInstance("PKIX");
        CertPathBuilderResult cpbr = builder.build(params);
    }
}
