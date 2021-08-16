/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4442566 7176326
 * @summary check that we can build and validate a zero-length
 *    certpath when a trust anchor cert satisfies the target constraints
 */
import java.io.ByteArrayInputStream;
import java.security.cert.*;
import java.util.Collections;

public class ZeroLengthPath {

    private static final String ANCHOR =
        "-----BEGIN CERTIFICATE-----\n" +
        "MIIBFzCBwgIBATANBgkqhkiG9w0BAQQFADAXMRUwEwYDVQQDEwxUcnVzdCBBbmNo\n" +
        "b3IwHhcNMDIxMTA3MTE1NzAzWhcNMjIxMTA3MTE1NzAzWjAXMRUwEwYDVQQDEwxU\n" +
        "cnVzdCBBbmNob3IwXDANBgkqhkiG9w0BAQEFAANLADBIAkEA9uCj12hwDgC1n9go\n" +
        "0ozQAVMM+DfX0vpKOemyGNp+ycSLfAq3pxBcUKbQhjSRL7YjPkEL8XC6pRLwyEoF\n" +
        "osWweQIDAQABMA0GCSqGSIb3DQEBBAUAA0EAzZta5M1qbbozj7jWnNyTgB4HUpzv\n" +
        "4eP0VYQb1pQY1/xEMczaRt+RuoIDnHCq5a1vOiwk6ZbdG6GlJKx9lj0oMQ==\n" +
        "-----END CERTIFICATE-----";


    public static void main(String[] args) throws Exception {

        ByteArrayInputStream is = new ByteArrayInputStream(ANCHOR.getBytes());
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        X509Certificate cert = (X509Certificate)cf.generateCertificate(is);

        X509CertSelector xcs = new X509CertSelector();
        xcs.setSubject(cert.getSubjectX500Principal().getName());
        PKIXBuilderParameters p = new PKIXBuilderParameters
            (Collections.singleton(new TrustAnchor(cert, null)), xcs);
        CertPathBuilder cpb = CertPathBuilder.getInstance("PKIX");
        CertPath cp = buildCertPath(cpb, p);
        validateCertPath(cp, p);
    }

    private static CertPath buildCertPath(CertPathBuilder cpb,
                                          PKIXBuilderParameters params)
        throws Exception
    {
        CertPathBuilderResult res = cpb.build(params);
        if (res.getCertPath().getCertificates().size() != 0) {
            throw new Exception("built path is not zero-length");
        }
        return res.getCertPath();
    }

    private static void validateCertPath(CertPath cp, PKIXParameters params)
        throws Exception
    {
        CertPathValidator cpv = CertPathValidator.getInstance("PKIX");
        CertPathValidatorResult cpvr = cpv.validate(cp, params);
    }
}
