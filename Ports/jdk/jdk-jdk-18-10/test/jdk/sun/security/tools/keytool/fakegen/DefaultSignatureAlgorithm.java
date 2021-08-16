/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8138766 8227059 8227595
 * @summary New default -sigalg for keytool
 * @library /test/lib
 * @build java.base/sun.security.rsa.RSAKeyPairGenerator
 *        java.base/sun.security.provider.DSAKeyPairGenerator
 *        jdk.crypto.ec/sun.security.ec.ECKeyPairGenerator
 * @run main DefaultSignatureAlgorithm
 * @modules jdk.crypto.ec
 */

import jdk.test.lib.Asserts;
import jdk.test.lib.SecurityTools;
import jdk.test.lib.process.OutputAnalyzer;

import java.io.File;
import java.security.KeyStore;
import java.security.cert.X509Certificate;

public class DefaultSignatureAlgorithm {

    static int pos = 0;

    public static void main(String[] args) throws Exception {
        check("RSA", 1024, null, "SHA256withRSA");
        check("RSA", 3072, null, "SHA256withRSA");
        check("RSA", 3073, null, "SHA384withRSA");
        check("RSA", 7680, null, "SHA384withRSA");
        check("RSA", 7681, null, "SHA512withRSA");

        check("DSA", 1024, null, "SHA256withDSA");
        check("DSA", 3072, null, "SHA256withDSA");

        check("EC", 384, null, "SHA384withECDSA");

        check("EC", 384, "SHA256withECDSA", "SHA256withECDSA");
    }

    private static void check(String keyAlg, int keySize,
            String requestedSigAlg, String expectedSigAlg)
            throws Exception {
        String alias = keyAlg + keySize + "-" + pos++;
        String sigAlgParam = requestedSigAlg == null
                ? ""
                : (" -sigalg " + requestedSigAlg);
        genkeypair(alias,
                "-keyalg " + keyAlg + " -keysize " + keySize + sigAlgParam)
            .shouldHaveExitValue(0);

        KeyStore ks = KeyStore.getInstance(
                new File("ks"), "changeit".toCharArray());
        X509Certificate cert = (X509Certificate)ks.getCertificate(alias);
        Asserts.assertEQ(cert.getPublicKey().getAlgorithm(), keyAlg);
        Asserts.assertEQ(cert.getSigAlgName(), expectedSigAlg);
    }

    static OutputAnalyzer genkeypair(String alias, String options)
            throws Exception {
        String patchArg = "-J--patch-module=java.base="
                + System.getProperty("test.classes")
                + File.separator + "patches" + File.separator + "java.base"
                + " -J--patch-module=jdk.crypto.ec="
                + System.getProperty("test.classes")
                + File.separator + "patches" + File.separator + "jdk.crypto.ec";
        return kt(patchArg + " -genkeypair -alias " + alias
                + " -dname CN=" + alias + " " + options);
    }

    static OutputAnalyzer kt(String cmd)
            throws Exception {
        return SecurityTools.keytool("-storepass changeit -keypass changeit "
                + "-keystore ks " + cmd);
    }
}
