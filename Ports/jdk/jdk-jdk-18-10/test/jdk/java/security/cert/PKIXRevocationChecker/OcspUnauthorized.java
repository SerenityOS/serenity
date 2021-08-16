/*
 * Copyright (c) 2013, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8023362
 * @run main/othervm OcspUnauthorized
 * @summary Make sure Ocsp UNAUTHORIZED response is treated as failure when
 *          SOFT_FAIL option is set
 */

import java.io.ByteArrayInputStream;
import java.security.Security;
import java.security.cert.CertPathValidatorException.BasicReason;
import java.security.cert.*;
import java.security.cert.PKIXRevocationChecker.Option;
import java.util.Base64;
import java.util.Collections;
import java.util.EnumSet;

public class OcspUnauthorized {

    private final static String OCSP_RESPONSE = "MAMKAQY=";

    private final static String EE_CERT =
        "MIICADCCAWmgAwIBAgIEOvxUmjANBgkqhkiG9w0BAQQFADAqMQswCQYDVQQGEwJ1czE" +
        "MMAoGA1UEChMDc3VuMQ0wCwYDVQQLEwRsYWJzMB4XDTAxMDUxNDIwNDQyMVoXDTI4MD" +
        "kyOTIwNDQyMVowOTELMAkGA1UEBhMCdXMxDDAKBgNVBAoTA3N1bjENMAsGA1UECxMEb" +
        "GFiczENMAsGA1UECxMEaXNyZzCBnzANBgkqhkiG9w0BAQEFAAOBjQAwgYkCgYEA4MmP" +
        "GDriFJ+OhDlTuLpHzPy0nawDKyIYUJPZmU9M/pCAUbZewAOyAXGPYVU1og2ZiO9tWBi" +
        "ZBeJGoFHEkkhfeqSVb2PsRckiXvPZ3AiSVmdX0uD/a963abmhRMYB1gDO2+jBe3F/DU" +
        "pHwpyThchy8tYUMh7Gr7+m/8FwZbdbSpMCAwEAAaMkMCIwDwYDVR0PAQH/BAUDAwekA" +
        "DAPBgNVHRMBAf8EBTADAQH/MA0GCSqGSIb3DQEBBAUAA4GBAME3fmXvES0FVDXSD1iC" +
        "TJLf86kUy3H+uMG7h5pOQmcfF1o9PVWlNByVf4r2b4GRgftPQ3Ao0SAvq1aSkW7YpkN" +
        "pcartYqNk2E5brPajOC0v+Pkxf/g/pkRTT6Zp+9erGQF4Ta62q0iwOyc3FovSbh0Ph2" +
        "WidZRP4qUG5I6JmGkI";

    private final static String TRUST_ANCHOR =
        "MIICIzCCAYygAwIBAgIEOvxT7DANBgkqhkiG9w0BAQQFADAbMQswCQYDVQQGEwJ1czE" +
        "MMAoGA1UEChMDc3VuMB4XDTAxMDUxNDIxMDQyOVoXDTI4MDkyOTIxMDQyOVowKjELMA" +
        "kGA1UEBhMCdXMxDDAKBgNVBAoTA3N1bjENMAsGA1UECxMEbGFiczCBnzANBgkqhkiG9" +
        "w0BAQEFAAOBjQAwgYkCgYEA0/16V87rhznCM0y7IqyGcfQBentG+PglA+1hiqCuQY/A" +
        "jFiDKr5N+LpcfU28P41E4M+DSDrMIEe4JchRcXeJY6aIVhpOveVV9mgtBaEKlsScrIJ" +
        "zmVqM07PG9JENg2FibECnB5TNUSfVbFKfvtAqaZ7Pc971oZVoIePBWnfKV9kCAwEAAa" +
        "NlMGMwPwYDVR0eAQH/BDUwM6AxMC+kKjELMAkGA1UEBhMCdXMxDDAKBgNVBAoTA3N1b" +
        "jENMAsGA1UECxMEbGFic4ABAzAPBgNVHQ8BAf8EBQMDB6QAMA8GA1UdEwEB/wQFMAMB" +
        "Af8wDQYJKoZIhvcNAQEEBQADgYEAfJ5HWd7K5PmX0+Vbsux4SYhoaejDwwgS43BRNa+" +
        "AmFq9LIZ+ZcjBMVte8Y3sJF+nz9+1qBaUhNhbaECCqsgmWSwvI+0kUzJXL89k9AdQ8m" +
        "AYf6CB6+kaZQBgrdSdqSGz3tCVa2MIK8wmb0ROM40oJ7vt3qSwgFi3UTltxkFfwQ0=";

    private static CertificateFactory cf;
    private static Base64.Decoder base64Decoder = Base64.getDecoder();

    public static void main(String[] args) throws Exception {
        // EE_CERT is signed with MD5withRSA
        Security.setProperty("jdk.certpath.disabledAlgorithms", "");
        cf = CertificateFactory.getInstance("X.509");
        X509Certificate taCert = getX509Cert(TRUST_ANCHOR);
        X509Certificate eeCert = getX509Cert(EE_CERT);
        CertPath cp = cf.generateCertPath(Collections.singletonList(eeCert));

        CertPathValidator cpv = CertPathValidator.getInstance("PKIX");
        PKIXRevocationChecker prc =
            (PKIXRevocationChecker)cpv.getRevocationChecker();
        prc.setOptions(EnumSet.of(Option.SOFT_FAIL, Option.NO_FALLBACK));
        byte[] response = base64Decoder.decode(OCSP_RESPONSE);

        prc.setOcspResponses(Collections.singletonMap(eeCert, response));

        TrustAnchor ta = new TrustAnchor(taCert, null);
        PKIXParameters params = new PKIXParameters(Collections.singleton(ta));

        params.addCertPathChecker(prc);

        try {
            cpv.validate(cp, params);
            throw new Exception("FAILED: expected CertPathValidatorException");
        } catch (CertPathValidatorException cpve) {
            cpve.printStackTrace();
            if (cpve.getReason() != BasicReason.UNSPECIFIED &&
                !cpve.getMessage().contains("OCSP response error: UNAUTHORIZED")) {
                throw new Exception("FAILED: unexpected " +
                                    "CertPathValidatorException reason");
            }
        }
    }

    private static X509Certificate getX509Cert(String enc) throws Exception {
        byte[] bytes = base64Decoder.decode(enc);
        ByteArrayInputStream is = new ByteArrayInputStream(bytes);
        return (X509Certificate)cf.generateCertificate(is);
    }
}
