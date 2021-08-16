/*
 * Copyright (c) 2002, 2010, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4684810 6994717
 * @summary Verify that RFC822 name constraints are checked correctly
 */

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;

import java.security.cert.*;
import java.security.cert.PKIXReason;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * ValidateCertPath performs a simple validation of a certification path.
 * On success, it prints the CertPathValidatorResult. On failure, it
 * prints the error.
 *
 * Synopsis:
 * <pre>
 *   ValidateCertPath trustAnchor [certFile ...]
 *       where each argument is the path to a file that contains a
 *       certificate. Each certificate should have an issuer equal to
 *       the subject of the preceding certificate.
 *</pre>
 *
 * @author      Steve Hanna
 */
public final class ValidateCertPath {

    private final static String BASE = System.getProperty("test.src", "./");

    private static CertPath path;
    private static PKIXParameters params;

    public static void main(String[] args) throws Exception {

        try {
            parseArgs(args);
            validate(path, params);
            throw new Exception("Successfully validated invalid path.");
        } catch (CertPathValidatorException e) {
            if (e.getReason() != PKIXReason.INVALID_NAME) {
                throw new Exception("unexpected reason: " + e.getReason());
            }
            System.out.println("Path rejected as expected: " + e);
        }
    }

    /**
     * Parse the command line arguments. Populate the static
     * class fields based on the values of the arugments. In
     * case of bad arguments, print usage and exit. In case of
     * other error, throw an exception.
     *
     * @param args command line arguments
     * @throws Exception on error
     */
    public static void parseArgs(String[] args) throws Exception {
        args = new String[] {"jane2jane.cer", "jane2steve.cer", "steve2tom.cer"};

        TrustAnchor anchor = new TrustAnchor(getCertFromFile(args[0]), null);
        List<X509Certificate> list = new ArrayList<X509Certificate>();
        for (int i = 1; i < args.length; i++) {
            list.add(0, getCertFromFile(args[i]));
        }
        CertificateFactory cf = CertificateFactory.getInstance("X509");
        path = cf.generateCertPath(list);

        Set<TrustAnchor> anchors = Collections.singleton(anchor);
        params = new PKIXParameters(anchors);
        params.setRevocationEnabled(false);
        // The certificates expired on 10/22/10, so set the validity date to
        // 05/01/2009 to avoid expiration failures
        params.setDate(new Date(1243828800000l));
    }

    /*
     * Reads the entire input stream into a byte array.
     */
    private static byte[] getTotalBytes(InputStream is) throws IOException {
        byte[] buffer = new byte[8192];
        ByteArrayOutputStream baos = new ByteArrayOutputStream(2048);
        int n;
        baos.reset();
        while ((n = is.read(buffer, 0, buffer.length)) != -1) {
            baos.write(buffer, 0, n);
        }
        return baos.toByteArray();
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
                File certFile = new File(BASE, certFilePath);
                if (!certFile.canRead())
                    throw new IOException("File " +
                                          certFile.toString() +
                                          " is not a readable file.");
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
     * Perform a PKIX validation. On success, print the
     * CertPathValidatorResult on System.out. On failure,
     * throw an exception.
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
        System.out.println("ValidateCertPath successful.");
    }
}
