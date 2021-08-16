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
 * @bug 4458951
 * @summary Check that Sun's PKIX implementation of
 *      CertPathValidator.validate() and CertPathBuilder.build() throw an
 *      InvalidAlgorithmParameterException if any of the TrustAnchors specified
 *      contain nameConstraints
 * @modules java.base/sun.security.util
 */
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import java.security.InvalidAlgorithmParameterException;
import java.security.cert.CertificateFactory;
import java.security.cert.CertPath;
import java.security.cert.CertPathBuilder;
import java.security.cert.CertPathBuilderResult;
import java.security.cert.CertPathValidator;
import java.security.cert.CertPathValidatorResult;
import java.security.cert.PKIXParameters;
import java.security.cert.PKIXBuilderParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.security.cert.X509CertSelector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import sun.security.util.DerInputStream;

/**
 * ValidateNC performs a validation and build of a certification path, using any
 * name constraints provided in the trust anchor's certificate. Using
 * Sun's provider, the validation and build should fail because name constraints
 * on trust anchors are not supported.
 *
 * @author      Steve Hanna
 * @author      Sean Mullan
 */
public final class ValidateNC {

    private static CertPath path;
    private static PKIXParameters params;
    private static Set anchors;

    public static void main(String[] args) throws Exception {

        String[] certs = { "sun2labs2.cer", "labs2isrg2.cer" };

        createPath(certs);
        try {
            validate(path, params);
            throw new Exception("CertPathValidator should have thrown an " +
              "InvalidAlgorithmParameterException");
        } catch (InvalidAlgorithmParameterException iape) {
            // success!
        }

        try {
            X509CertSelector sel = new X509CertSelector();
            sel.setSubject("cn=sean");
            PKIXBuilderParameters bparams =
                new PKIXBuilderParameters(anchors, sel);
            build(bparams);
            throw new Exception("CertPathBuilder should have thrown an " +
              "InvalidAlgorithmParameterException");
        } catch (InvalidAlgorithmParameterException iape) {
            // success!
        }
    }

    public static void createPath(String[] certs) throws Exception {

        X509Certificate anchorCert = getCertFromFile(certs[0]);
        byte [] nameConstraints = anchorCert.getExtensionValue("2.5.29.30");
        if (nameConstraints != null) {
            DerInputStream in = new DerInputStream(nameConstraints);
            nameConstraints = in.getOctetString();
        }
        TrustAnchor anchor = new TrustAnchor(anchorCert, nameConstraints);
        List list = new ArrayList();
        for (int i = 1; i < certs.length; i++) {
            list.add(0, getCertFromFile(certs[i]));
        }
        CertificateFactory cf = CertificateFactory.getInstance("X509");
        path = cf.generateCertPath(list);

        anchors = Collections.singleton(anchor);
        params = new PKIXParameters(anchors);
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
     * Perform a PKIX validation.
     *
     * @param path CertPath to validate
     * @param params PKIXParameters to use in validation
     * @throws Exception on error
     */
    public static void validate(CertPath path, PKIXParameters params)
        throws Exception {
        CertPathValidator validator =
            CertPathValidator.getInstance("PKIX", "SUN");
        CertPathValidatorResult cpvr = validator.validate(path, params);
    }

    /**
     * Perform a PKIX build.
     *
     * @param params PKIXBuilderParameters to use in the build
     * @throws Exception on error
     */
    public static void build(PKIXBuilderParameters params)
        throws Exception {
        CertPathBuilder builder =
            CertPathBuilder.getInstance("PKIX", "SUN");
        CertPathBuilderResult cpbr = builder.build(params);
    }
}
