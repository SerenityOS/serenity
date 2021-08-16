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

/*
 * @test
 * @bug 4519462
 * @summary Verify Sun CertPathBuilder implementation handles certificates with no extensions
 */

import java.security.cert.X509Certificate;
import java.security.cert.TrustAnchor;
import java.security.cert.CollectionCertStoreParameters;
import java.security.cert.CertStore;
import java.security.cert.X509CertSelector;
import java.security.cert.CertPathBuilder;
import java.security.cert.PKIXBuilderParameters;
import java.security.cert.CertPathBuilderResult;
import java.security.cert.CertificateFactory;
import java.security.cert.CRL;
import java.security.cert.CertPath;
import java.util.HashSet;
import java.util.ArrayList;
import java.io.ByteArrayInputStream;

// Test based on user code submitted with bug by daniel.boggs@compass.net
public class NoExtensions {

    public static void main(String[] args) {
        try {
            NoExtensions certs = new NoExtensions();

            // the first certificate has the Authority Key Identifier extension
            certs.doBuild(getUserCertificate1());
            System.out.println("successfully built path for the first certificate");

            // the second certificate does not have the Authority Key Identifier extension
            // this will not succeed
            certs.doBuild(getUserCertificate2());
            System.out.println("successfully built path for the second certificate");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void doBuild(X509Certificate userCert) throws Exception {
        // get the set of trusted CA certificates (only one in this instance)
        HashSet trustAnchors = new HashSet();
        X509Certificate trustedCert = getTrustedCertificate();
        trustAnchors.add(new TrustAnchor(trustedCert, null));

        // put together a CertStore (repository of the certificates and CRLs)
        ArrayList certs = new ArrayList();
        certs.add(trustedCert);
        certs.add(userCert);
        CollectionCertStoreParameters certStoreParams = new CollectionCertStoreParameters(certs);
        CertStore certStore = CertStore.getInstance("Collection", certStoreParams);

        // specify the target certificate via a CertSelector
        X509CertSelector certSelector = new X509CertSelector();
        certSelector.setCertificate(userCert);
        certSelector.setSubject(userCert.getSubjectDN().getName()); // seems to be required

        // build a valid cerificate path
        CertPathBuilder certPathBuilder = CertPathBuilder.getInstance("PKIX", "SUN");
        PKIXBuilderParameters certPathBuilderParams = new PKIXBuilderParameters(trustAnchors, certSelector);
        certPathBuilderParams.addCertStore(certStore);
        certPathBuilderParams.setRevocationEnabled(false);
        CertPathBuilderResult result = certPathBuilder.build(certPathBuilderParams);

        // get and show cert path
        CertPath certPath = result.getCertPath();
//        System.out.println(certPath.toString());
    }

    private static X509Certificate getTrustedCertificate() throws Exception {
        String sCert =
            "-----BEGIN CERTIFICATE-----\n"
          + "MIIBezCCASWgAwIBAgIQyWD8dLUoqpJFyDxrfRlrsTANBgkqhkiG9w0BAQQFADAW\n"
          + "MRQwEgYDVQQDEwtSb290IEFnZW5jeTAeFw0wMTEwMTkxMjU5MjZaFw0zOTEyMzEy\n"
          + "MzU5NTlaMBoxGDAWBgNVBAMTD1Jvb3RDZXJ0aWZpY2F0ZTBcMA0GCSqGSIb3DQEB\n"
          + "AQUAA0sAMEgCQQC+NFKszPjatUZKWmyWaFjir1wB93FX2u5SL+GMjgUsMs1JcTKQ\n"
          + "Kh0cnnQKknNkV4cTW4NPn31YCoB1+0KA3mknAgMBAAGjSzBJMEcGA1UdAQRAMD6A\n"
          + "EBLkCS0GHR1PAI1hIdwWZGOhGDAWMRQwEgYDVQQDEwtSb290IEFnZW5jeYIQBjds\n"
          + "AKoAZIoRz7jUqlw19DANBgkqhkiG9w0BAQQFAANBACJxAfP57yqaT9N+nRgAOugM\n"
          + "JG0aN3/peCIvL3p29epRL2xoWFvxpUUlsH2I39OZ6b8+twWCebhkv1I62segXAk=\n"
          + "-----END CERTIFICATE-----";
        CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
        ByteArrayInputStream bytes = new ByteArrayInputStream(sCert.getBytes());
        return (X509Certificate)certFactory.generateCertificate(bytes);
    }

    private static X509Certificate getUserCertificate1() throws Exception {
        // this certificate includes an extension
        String sCert =
            "-----BEGIN CERTIFICATE-----\n"
          + "MIIBfzCCASmgAwIBAgIQWFSKzCWO2ptOAc2F3MKZSzANBgkqhkiG9w0BAQQFADAa\n"
          + "MRgwFgYDVQQDEw9Sb290Q2VydGlmaWNhdGUwHhcNMDExMDE5MTMwNzQxWhcNMzkx\n"
          + "MjMxMjM1OTU5WjAaMRgwFgYDVQQDEw9Vc2VyQ2VydGlmaWNhdGUwXDANBgkqhkiG\n"
          + "9w0BAQEFAANLADBIAkEA24gypa2YFGZHKznEWWbqIWNVXCM35W7RwJwhGpNsuBCj\n"
          + "NT6KEo66F+OOMgZmb0KrEZHBJASJ3n4Cqbt4aHm/2wIDAQABo0swSTBHBgNVHQEE\n"
          + "QDA+gBBch+eYzOPgVRbMq5vGpVWooRgwFjEUMBIGA1UEAxMLUm9vdCBBZ2VuY3mC\n"
          + "EMlg/HS1KKqSRcg8a30Za7EwDQYJKoZIhvcNAQEEBQADQQCYBIHBqQQJePi5Hzfo\n"
          + "CxeUaYlXmvbxVNkxM65Pplsj3h4ntfZaynmlhahH3YsnnA8wk6xPt04LjSId12RB\n"
          + "PeuO\n"
          + "-----END CERTIFICATE-----";
        CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
        ByteArrayInputStream bytes = new ByteArrayInputStream(sCert.getBytes());
        return (X509Certificate)certFactory.generateCertificate(bytes);
    }

    private static X509Certificate getUserCertificate2() throws Exception {
        // this certificate does not include any extensions
        String sCert =
            "-----BEGIN CERTIFICATE-----\n"
          + "MIIBMjCB3aADAgECAhB6225ckZVssEukPuvk1U1PMA0GCSqGSIb3DQEBBAUAMBox\n"
          + "GDAWBgNVBAMTD1Jvb3RDZXJ0aWZpY2F0ZTAeFw0wMTEwMTkxNjA5NTZaFw0wMjEw\n"
          + "MTkyMjA5NTZaMBsxGTAXBgNVBAMTEFVzZXJDZXJ0aWZpY2F0ZTIwXDANBgkqhkiG\n"
          + "9w0BAQEFAANLADBIAkEAzicGiW9aUlUoQIZnLy1l8MMV5OvA+4VJ4T/xo/PpN8Oq\n"
          + "WgZVGKeEp6JCzMlXEJk3TGLfpXL4Ytw+Ldhv0QPhLwIDAnMpMA0GCSqGSIb3DQEB\n"
          + "BAUAA0EAQmj9SFHEx66JyAps3ew4pcSS3QvfVZ/6qsNUYCG75rFGcTUPHcXKql9y\n"
          + "qBT83iNLJ//krjw5Ju0WRPg/buHSww==\n"
          + "-----END CERTIFICATE-----";
        CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
        ByteArrayInputStream bytes = new ByteArrayInputStream(sCert.getBytes());
        return (X509Certificate)certFactory.generateCertificate(bytes);
    }
}
