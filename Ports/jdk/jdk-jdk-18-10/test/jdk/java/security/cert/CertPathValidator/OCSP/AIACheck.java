/*
 * Copyright (c) 2004, 2015, Oracle and/or its affiliates. All rights reserved.
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

//
// Security properties, once set, cannot revert to unset.  To avoid
// conflicts with tests running in the same VM isolate this test by
// running it in otherVM mode.
//

/**
 * @test
 * @bug 5072953
 * @summary Verify that the URL for an OCSP responder can be extracted from a
 *          certificate's AuthorityInfoAccess extension when OCSP certifiate
 *          validation has been enabled.
 * @run main/othervm AIACheck
 */

import java.io.*;
import java.net.*;
import java.util.*;
import java.security.Security;
import java.security.cert.*;

public class AIACheck {

    private final static File baseDir =
        new File(System.getProperty("test.src", "."));

    private static X509Certificate loadCertificate(String name)
        throws Exception
    {
        File certFile = new File(baseDir, name);
        InputStream in = new FileInputStream(certFile);
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        X509Certificate cert = (X509Certificate)cf.generateCertificate(in);
        return cert;
    }

    public static void main(String args[]) throws Exception {
        // MD5 is used in this test case, don't disable MD5 algorithm.
        Security.setProperty(
                "jdk.certpath.disabledAlgorithms", "MD2, RSA keySize < 1024");

        X509Certificate aiaCert = loadCertificate("AIACert.pem");
        X509Certificate rootCert = loadCertificate("RootCert.pem");

        List<X509Certificate> list =
            //Arrays.asList(new X509Certificate[] {aiaCert, rootCert});
            Arrays.asList(new X509Certificate[] {aiaCert});
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        CertPath path = cf.generateCertPath(list);

        TrustAnchor anchor = new TrustAnchor(rootCert, null);
        Set<TrustAnchor> anchors = Collections.singleton(anchor);

        PKIXParameters params = new PKIXParameters(anchors);
        // Activate certificate revocation checking
        params.setRevocationEnabled(true);

        // Activate OCSP
        Security.setProperty("ocsp.enable", "true");

        // Ensure that the ocsp.responderURL property is not set.
        if (Security.getProperty("ocsp.responderURL") != null) {
            throw new
                Exception("The ocsp.responderURL property must not be set");
        }

        CertPathValidator validator = CertPathValidator.getInstance("PKIX");

        try {
            validator.validate(path, params);
            throw new Exception("Successfully validated an invalid path");

        } catch (CertPathValidatorException e ) {
            Throwable rootCause = e.getCause();
            if (!(rootCause instanceof SocketException ||
                  rootCause instanceof SocketTimeoutException)) {
                throw e;
            }

            // Success - client located OCSP responder in AIA extension
            //           and attempted to connect.
            System.out.println("Extracted the URL of the OCSP responder from " +
                "the certificate's AuthorityInfoAccess extension.");
        }
    }
}
