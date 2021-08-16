/*
 * Copyright (c) 2004, 2007, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 5032952
 * @summary non-transient non-serializable instance field in
    serializable class
 * @modules java.base/sun.security.provider.certpath
 */

import sun.security.provider.certpath.SunCertPathBuilderException;
import java.io.*;
import java.security.cert.*;
import java.security.*;
import java.util.Collections;

public class SunCertPathBuilderExceptionTest {

    public static void main(String[] args) throws Exception {
        try {
            CertPathBuilder cpb = CertPathBuilder.
                getInstance(CertPathBuilder.getDefaultType());
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            File f = new File
                (System.getProperty("test.src", "."), "speech2speech");
            X509Certificate cert = (X509Certificate)
                cf.generateCertificate(new FileInputStream(f));
            TrustAnchor anchor = new TrustAnchor(cert, null);

            X509CertSelector xs = new X509CertSelector();

            // a non-exist subject which will ruin the builder soon
            xs.setSubject("CN=A, OU=A, O=A, L=A, ST=A, C=A");
            PKIXBuilderParameters pkp =
                new PKIXBuilderParameters(Collections.singleton(anchor), xs);
            cpb.build(pkp);
        } catch(SunCertPathBuilderException e) {
            System.out.println("Got the Exception: ");
            try {
                ObjectOutputStream o =
                    new ObjectOutputStream(new ByteArrayOutputStream());
                o.writeObject(e);
                o.close();
            } catch(NotSerializableException e2) {
                System.out.println("Test fail: bug not corrected");
                throw e2;
            }
            System.out.println("Test pass: SCPEE is Serializable");
            return;
        }
        throw new Exception("Test fail: Strange, no SCPEE thrown");
    }
}
