/*
 * Copyright (c) 2001, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4414263
 * @summary Make sure PolicyNode.getPolicyQualifiers() returns
 *      Set of PolicyQualifierInfos.
 */
import java.io.File;
import java.io.FileInputStream;
import java.security.cert.*;
import java.text.DateFormat;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class GetPolicyQualifiers {

    public static void main(String[] args) throws Exception {

        CertificateFactory cf = CertificateFactory.getInstance("X.509", "SUN");
        File f = new File(System.getProperty("test.src", "."), "speech2speech");
        X509Certificate mostTrustedCaCert = (X509Certificate)
            cf.generateCertificate(new FileInputStream(f));
        Set trustAnchors = Collections.singleton(
            new TrustAnchor(mostTrustedCaCert, null));
        f = new File(System.getProperty("test.src", "."), "speech2eve");
        X509Certificate eeCert = (X509Certificate)
            cf.generateCertificate(new FileInputStream(f));
        CertPathValidator cpv = CertPathValidator.getInstance("PKIX", "SUN");
        PKIXParameters params = new PKIXParameters(trustAnchors);
        params.setPolicyQualifiersRejected(false);
        params.setRevocationEnabled(false);
        // Certificates expired on Oct 6th, 2020
        params.setDate(DateFormat.getDateInstance(DateFormat.MEDIUM,
                Locale.US).parse("July 01, 2020"));
        List certList = Collections.singletonList(eeCert);
        CertPath cp = cf.generateCertPath(certList);
        PKIXCertPathValidatorResult result =
            (PKIXCertPathValidatorResult) cpv.validate(cp, params);

        PolicyNode policyTree = result.getPolicyTree();
        Iterator children = policyTree.getChildren();
        PolicyNode child = (PolicyNode) children.next();
        Set policyQualifiers = child.getPolicyQualifiers();
        Iterator i = policyQualifiers.iterator();
        while (i.hasNext()) {
            Object next = i.next();
            if (!(next instanceof PolicyQualifierInfo))
                throw new Exception("not a PolicyQualifierInfo");
        }

        params.setPolicyQualifiersRejected(true);
        try {
            result = (PKIXCertPathValidatorResult) cpv.validate(cp, params);
            throw new Exception("Validation of CertPath containing critical " +
                "qualifiers should have failed when policyQualifiersRejected " +
                "flag is true");
        } catch (CertPathValidatorException cpve) {
            if (cpve.getReason() != PKIXReason.INVALID_POLICY) {
                throw new Exception("unexpected reason: " + cpve.getReason());
            }
        }
    }
}
