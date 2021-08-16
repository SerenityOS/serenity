/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8252377
 * @library /test/lib
 * @modules java.base/sun.security.util
 *          java.base/sun.security.x509
 * @summary The AlgorithmIdentifier for ECDSA should omit the parameters field
 */

import jdk.test.lib.Asserts;
import jdk.test.lib.SecurityTools;
import jdk.test.lib.process.OutputAnalyzer;
import static jdk.test.lib.security.DerUtils.*;

import java.io.File;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import sun.security.util.*;

public class OmitAlgIdParam {

    public static void main(String[] args) throws Exception {
        keytool("-genkeypair -keyalg ec -dname CN=EC1 -alias ecsha224 "
                + "-sigalg SHA224withECDSA -keystore ks -storepass changeit");

        keytool("-genkeypair -keyalg ec -dname CN=EC2 -alias ecsha256 "
                + "-sigalg SHA256withECDSA -keystore ks -storepass changeit");

        keytool("-genkeypair -keyalg ec -dname CN=EC3 -alias ecsha384 "
                + "-sigalg SHA384withECDSA -keystore ks -storepass changeit");

        keytool("-genkeypair -keyalg ec -dname CN=EC4 -alias ecsha512 "
                + "-sigalg SHA512withECDSA -keystore ks -storepass changeit");

        KeyStore kstore = KeyStore.getInstance(
                new File("ks"), "changeit".toCharArray());

        // SHA224withECDSA
        checkAlgId(kstore, "ecsha224", "SHA224withECDSA",
                ObjectIdentifier.of(KnownOIDs.SHA224withECDSA));

        // SHA256withECDSA
        checkAlgId(kstore, "ecsha256", "SHA256withECDSA",
                ObjectIdentifier.of(KnownOIDs.SHA256withECDSA));

        // SHA384withECDSA
        checkAlgId(kstore, "ecsha384", "SHA384withECDSA",
                ObjectIdentifier.of(KnownOIDs.SHA384withECDSA));

        // SHA512withECDSA
        checkAlgId(kstore, "ecsha512", "SHA512withECDSA",
                ObjectIdentifier.of(KnownOIDs.SHA512withECDSA));
    }

    private static void checkAlgId(KeyStore ks, String alias, String alg,
            ObjectIdentifier oid) throws Exception {
        X509Certificate cert = (X509Certificate)ks.getCertificate(alias);
        System.out.println("SigAlgName = " + cert.getSigAlgName());

        Asserts.assertEQ(cert.getPublicKey().getAlgorithm(), "EC");
        Asserts.assertEQ(cert.getSigAlgName(), alg);

        byte[] data = cert.getEncoded();
        // Parameters field in the specified AlgorithmIdentifier should be omitted
        // Checking the first signature AlgorithmIdentifier in the cert
        checkAlg(data, "020", oid);
        shouldNotExist(data, "021");
        // Checking the second signature AlgorithmIdentifier in the cert
        checkAlg(data, "10", oid);
        shouldNotExist(data, "11");
    }

    static OutputAnalyzer keytool(String cmd) throws Exception {
        return SecurityTools.keytool(cmd).shouldHaveExitValue(0);
    }
}
