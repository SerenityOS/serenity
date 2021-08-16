/*
 * Copyright (c) 2008, 2019, Oracle and/or its affiliates. All rights reserved.
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

// This test case relies on updated static security property, no way to re-use
// security property in samevm/agentvm mode.

/**
 * @test
 * @bug 6714842
 * @library /test/lib
 * @run main/othervm BuildEEBasicConstraints
 * @summary make sure a PKIX CertPathBuilder builds a path to an
 *      end entity certificate when the setBasicConstraints method of the
 *      X509CertSelector of the targetConstraints PKIXBuilderParameters
 *      parameter is set to -2.
 */

import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.CertPath;
import java.security.cert.CertStore;
import java.security.cert.CollectionCertStoreParameters;
import java.security.cert.PKIXBuilderParameters;
import java.security.cert.PKIXCertPathBuilderResult;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.security.cert.X509CertSelector;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import jdk.test.lib.security.CertUtils;

public final class BuildEEBasicConstraints {

    public static void main(String[] args) throws Exception {
        // reset the security property to make sure that the algorithms
        // and keys used in this test are not disabled.
        Security.setProperty("jdk.certpath.disabledAlgorithms", "MD2");

        X509Certificate rootCert = CertUtils.getCertFromFile("anchor.cer");
        TrustAnchor anchor = new TrustAnchor
            (rootCert.getSubjectX500Principal(), rootCert.getPublicKey(), null);
        X509CertSelector sel = new X509CertSelector();
        sel.setBasicConstraints(-2);
        PKIXBuilderParameters params = new PKIXBuilderParameters
            (Collections.singleton(anchor), sel);
        params.setRevocationEnabled(false);
        X509Certificate eeCert = CertUtils.getCertFromFile("ee.cer");
        X509Certificate caCert = CertUtils.getCertFromFile("ca.cer");
        ArrayList<X509Certificate> certs = new ArrayList<X509Certificate>();
        certs.add(caCert);
        certs.add(eeCert);
        CollectionCertStoreParameters ccsp =
            new CollectionCertStoreParameters(certs);
        CertStore cs = CertStore.getInstance("Collection", ccsp);
        params.addCertStore(cs);
        PKIXCertPathBuilderResult res = CertUtils.build(params);
        CertPath cp = res.getCertPath();
        // check that first certificate is an EE cert
        List<? extends Certificate> certList = cp.getCertificates();
        X509Certificate cert = (X509Certificate) certList.get(0);
        if (cert.getBasicConstraints() != -1) {
            throw new Exception("Target certificate is not an EE certificate");
        }
    }
}
