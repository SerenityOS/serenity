/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8005389
 * @summary CRL Distribution Point URIs with spaces or backslashes should
 *          not be parseable
 * @modules java.base/sun.security.util
 *          java.base/sun.security.x509
 */
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import sun.security.util.DerValue;
import sun.security.x509.CRLDistributionPointsExtension;
import sun.security.x509.URIName;


public class Parse {

    // certificate with a space in the CRLDistributionPointsExtension uri
    // uri: file://crl file.crl
    static String certWithSpaceInCDPStr =
        "-----BEGIN CERTIFICATE-----\n" +
        "MIIB8DCCAVmgAwIBAgIJAOgNnYA5nHtQMA0GCSqGSIb3DQEBBQUAMCUxETAPBgNV\n" +
        "BAMTCHRlc3RuYW1lMRAwDgYDVQQLEwd0ZXN0b3JnMB4XDTEyMDgxMzIzMzgzN1oX\n" +
        "DTEyMDkxMjIzMzgzN1owJTERMA8GA1UEAxMIdGVzdG5hbWUxEDAOBgNVBAsTB3Rl\n" +
        "c3RvcmcwgZ8wDQYJKoZIhvcNAQEBBQADgY0AMIGJAoGBANx02RuD/Y2pvgVnXBbJ\n" +
        "Sb+8j80geuoYEyRRnP6YiL2wmZqMmTHuznFwosO57KoVbz/XEr1bOnBMnPKax5Ll\n" +
        "QlDI3nmnxmUq13ORQ6GkD3M+QRzzxc66BFJbKqUzgv1P3NngyIFr03zb/opXdCTZ\n" +
        "4WfJuCf7Ouz44Ch6ZGQJ+7G5AgMBAAGjKDAmMCQGA1UdHwQdMBswGaAXoBWGE2Zp\n" +
        "bGU6Ly9jcmwgZmlsZS5jcmwwDQYJKoZIhvcNAQEFBQADgYEAB+ublc1l1EnXtEJE\n" +
        "jYeFzAdttHKQ4mn8CXGtHSy9gpckKyLdZUc9/n6yKuNXih29faepZ8mtaftTYpgR\n" +
        "AUqZ+6YYik+rIqZpnWMPR9qZvshf/KPerXiZe7kYBKNvxgmCFfhK8QN6nxUGrR2F\n" +
        "d53HWct6zXqlj+vQZsGC30f764M=\n" +
        "-----END CERTIFICATE-----";

    // a certificate with backslashes in the CRLDistributionPointsExtension uri
    // uri: file://\\\\CRL\\crl_file.crl
    static String certWithBackslashesInCDPStr =
        "-----BEGIN CERTIFICATE-----\n" +
        "MIIB9jCCAV+gAwIBAgIJAOQV9wTIgnc1MA0GCSqGSIb3DQEBBQUAMCUxETAPBgNV\n" +
        "BAMTCHRlc3RuYW1lMRAwDgYDVQQLEwd0ZXN0b3JnMB4XDTEyMDgxMzIzMzcxM1oX\n" +
        "DTEyMDkxMjIzMzcxM1owJTERMA8GA1UEAxMIdGVzdG5hbWUxEDAOBgNVBAsTB3Rl\n" +
        "c3RvcmcwgZ8wDQYJKoZIhvcNAQEBBQADgY0AMIGJAoGBALdtczqZqI6RI17mz58/\n" +
        "PgFNBmb/dX/MeVcuaUp79RVUbDQ68z4JxDNv4ImcAxigKXb2jObPAxOdM+VlXROS\n" +
        "AmUNgYrIPuHNnKbd+rwilc6DsGWZnZLmZE63fUvTSqjOnSlsENSgDBVL/4r+yWBB\n" +
        "8KKmFGRFqkCyN1EZl03IW9i7AgMBAAGjLjAsMCoGA1UdHwQjMCEwH6AdoBuGGWZp\n" +
        "bGU6Ly9cXENSTFxjcmxfZmlsZS5jcmwwDQYJKoZIhvcNAQEFBQADgYEACOgZEaST\n" +
        "BCFQVeXZ5d8J3dUZ+wRRkPvrlvopxMtZb3Hyte78PNoIZ78f1gYL18HiGYwKttau\n" +
        "DyPp1lrG9xKPfIeKg+aDWTtVE7pexB4qCryID0+kJfdNzkdIgdGJzJ/RmfJ5heMF\n" +
        "+R46Mhpua4c6gGsE2NGBFxmtS3YHpQsKtz8=\n" +
        "-----END CERTIFICATE-----";

    /*
     * Create an X509Certificate then attempt to construct a
     * CRLDistributionPointsExtension object from its extension value bytes.
     */
    private static void CRLDistributionPointsExtensionTest(String certStr)
            throws Exception {
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        ByteArrayInputStream is = new ByteArrayInputStream(certStr.getBytes());
        X509Certificate cert = (X509Certificate) cf.generateCertificate(is);

        // oid for CRL Distribution Points = 2.5.29.31
        byte[] CDPExtBytes = cert.getExtensionValue("2.5.29.31");
        DerValue val = new DerValue(CDPExtBytes);
        byte[] data = val.getOctetString();
        CRLDistributionPointsExtension CDPExt
                = new CRLDistributionPointsExtension(false, data);
    }

    public static void main(String[] args) throws Exception {
        /* Try to parse a CRLDistributionPointsExtension URI with a space. */
        try {
            CRLDistributionPointsExtensionTest(certWithSpaceInCDPStr);
            throw new RuntimeException("Illegally parsed a "
                    + "CRLDistributionPointsExtension uri with a space.");
        } catch (IOException e) {
            System.out.println("Caught the correct exception.");

        }

        /* Try to parse a CRLDistributionPointsExtension URI with backslashes. */
        try {
            CRLDistributionPointsExtensionTest(certWithBackslashesInCDPStr);
            throw new RuntimeException("Illegally parsed a "
                    + "CRLDistributionPointsExtension uri with a backslashes.");
        } catch (IOException e) {
            System.out.println("Caught the correct exception.");
        }

        /* Try to construct a URIName from a uri with a space. */
        String uriWithSpace = "file://crl file.crl";
        URIName name;
        try {
            name = new URIName(uriWithSpace);
            throw new RuntimeException("Illegally created a URIName "
                    + "from a uri with a space.");
        } catch (IOException e) {
            System.out.println("Caught the correct exception.");
        }

        /* Try to construct a URIName from a uri with backslashes. */
        String uriWithBackslashes = "file://\\\\CRL\\crl_file.crl";
        try {
            name = new URIName(uriWithBackslashes);
            throw new RuntimeException("Illegally created a URIName "
                    + "from a uri with backslashes.");
        } catch (IOException e) {
            System.out.println("Caught the correct exception.");
        }

        System.out.println("Tests passed.");
    }
}

