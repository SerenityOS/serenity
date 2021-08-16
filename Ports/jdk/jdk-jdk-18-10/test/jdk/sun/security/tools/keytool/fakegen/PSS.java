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
 * @bug 8215694 8222987 8225257
 * @summary keytool cannot generate RSASSA-PSS certificates
 * @library /test/lib
 * @build java.base/sun.security.rsa.RSAKeyPairGenerator
 * @modules java.base/sun.security.util
 *          java.base/sun.security.x509
 * @run main PSS
 */

import jdk.test.lib.Asserts;
import jdk.test.lib.SecurityTools;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.security.DerUtils;
import sun.security.util.ObjectIdentifier;
import sun.security.x509.AlgorithmId;

import java.io.File;
import java.security.KeyStore;
import java.security.cert.X509Certificate;

public class PSS {

    public static void main(String[] args) throws Exception {

        genkeypair("p", "-keyalg RSASSA-PSS -sigalg RSASSA-PSS")
                .shouldHaveExitValue(0);

        genkeypair("a", "-keyalg RSA -sigalg RSASSA-PSS -keysize 2048")
                .shouldHaveExitValue(0);

        genkeypair("b", "-keyalg RSA -sigalg RSASSA-PSS -keysize 4096")
                .shouldHaveExitValue(0);

        genkeypair("c", "-keyalg RSA -sigalg RSASSA-PSS -keysize 8192")
                .shouldHaveExitValue(0);

        KeyStore ks = KeyStore.getInstance(
                new File("ks"), "changeit".toCharArray());

        check((X509Certificate)ks.getCertificate("p"), "RSASSA-PSS",
                AlgorithmId.SHA256_oid);

        check((X509Certificate)ks.getCertificate("a"), "RSA",
                AlgorithmId.SHA256_oid);

        check((X509Certificate)ks.getCertificate("b"), "RSA",
                AlgorithmId.SHA384_oid);

        check((X509Certificate)ks.getCertificate("c"), "RSA",
                AlgorithmId.SHA512_oid);

        // More commands
        kt("-certreq -alias p -sigalg RSASSA-PSS -file p.req")
                .shouldHaveExitValue(0);

        kt("-gencert -alias a -sigalg RSASSA-PSS -infile p.req -outfile p.cert")
                .shouldHaveExitValue(0);

        kt("-importcert -alias p -file p.cert")
                .shouldHaveExitValue(0);

        kt("-selfcert -alias p -sigalg RSASSA-PSS")
                .shouldHaveExitValue(0);
    }

    static OutputAnalyzer genkeypair(String alias, String options)
            throws Exception {
        String patchArg = "-J--patch-module=java.base=" + System.getProperty("test.classes")
                + File.separator + "patches" + File.separator + "java.base";
        return kt(patchArg + " -genkeypair -alias " + alias
                + " -dname CN=" + alias + " " + options);
    }

    static OutputAnalyzer kt(String cmd)
            throws Exception {
        return SecurityTools.keytool("-storepass changeit -keypass changeit "
                + "-keystore ks " + cmd);
    }

    static void check(X509Certificate cert, String expectedKeyAlg,
            ObjectIdentifier expectedMdAlg) throws Exception {
        Asserts.assertEQ(cert.getPublicKey().getAlgorithm(), expectedKeyAlg);
        Asserts.assertEQ(cert.getSigAlgName(), "RSASSA-PSS");
        DerUtils.checkAlg(cert.getSigAlgParams(), "000", expectedMdAlg);
    }
}
