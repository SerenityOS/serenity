/*
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates. All rights reserved.
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

import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.security.KeyStoreUtils;
import jdk.test.lib.security.timestamp.TsaHandler;
import jdk.test.lib.security.timestamp.TsaServer;
import jdk.test.lib.util.JarUtils;

import java.io.File;
import java.security.KeyStore;

/**
 * @test
 * @bug 8024302 8026037 8176320
 * @summary The test signs and verifies a jar file with -tsacert option
 * @library /lib/testlibrary warnings
 * @library /test/lib
 * @modules java.base/sun.security.pkcs
 *          java.base/sun.security.timestamp
 *          java.base/sun.security.tools.keytool
 *          java.base/sun.security.util
 *          java.base/sun.security.x509
 *          java.management
 * @build jdk.test.lib.util.JarUtils
 * @run main TsacertOptionTest
 */
public class TsacertOptionTest extends Test {

    private static final String FILENAME = TsacertOptionTest.class.getName()
            + ".txt";
    private static final String SIGNING_KEY_ALIAS = "sign_alias";
    private static final String TSA_KEY_ALIAS = "ts";

    private static final String PASSWORD = "changeit";

    /**
     * The test signs and verifies a jar file with -tsacert option,
     * and checks that no warning was shown.
     * A certificate that is addressed in -tsacert option contains URL to TSA
     * in Subject Information Access extension.
     */
    public static void main(String[] args) throws Throwable {
        TsacertOptionTest test = new TsacertOptionTest();
        test.start();
    }

    void start() throws Throwable {
        // create a jar file that contains one file
        Utils.createFiles(FILENAME);
        JarUtils.createJar(UNSIGNED_JARFILE, FILENAME);

        // create key pair for jar signing
        keytool(
                "-genkey",
                "-alias", CA_KEY_ALIAS,
                "-keyalg", KEY_ALG,
                "-keysize", Integer.toString(KEY_SIZE),
                "-keystore", KEYSTORE,
                "-storepass", PASSWORD,
                "-keypass", PASSWORD,
                "-dname", "CN=CA",
                "-ext", "bc:c",
                "-validity", Integer.toString(VALIDITY)).shouldHaveExitValue(0);
        keytool(
                "-genkey",
                "-alias", SIGNING_KEY_ALIAS,
                "-keyalg", KEY_ALG,
                "-keysize", Integer.toString(KEY_SIZE),
                "-keystore", KEYSTORE,
                "-storepass", PASSWORD,
                "-keypass", PASSWORD,
                "-dname", "CN=Test").shouldHaveExitValue(0);
        keytool(
                "-certreq",
                "-alias", SIGNING_KEY_ALIAS,
                "-keystore", KEYSTORE,
                "-storepass", PASSWORD,
                "-keypass", PASSWORD,
                "-file", "certreq").shouldHaveExitValue(0);
        keytool(
                "-gencert",
                "-alias", CA_KEY_ALIAS,
                "-keystore", KEYSTORE,
                "-storepass", PASSWORD,
                "-keypass", PASSWORD,
                "-validity", Integer.toString(VALIDITY),
                "-infile", "certreq",
                "-outfile", "cert").shouldHaveExitValue(0);
        keytool(
                "-importcert",
                "-alias", SIGNING_KEY_ALIAS,
                "-keystore", KEYSTORE,
                "-storepass", PASSWORD,
                "-keypass", PASSWORD,
                "-file", "cert").shouldHaveExitValue(0);


        try (TsaServer tsa = new TsaServer(0)) {

            // look for free network port for TSA service
            int port = tsa.getPort();
            String host = "127.0.0.1";
            String tsaUrl = "http://" + host + ":" + port;

            // create key pair for TSA service
            // SubjectInfoAccess extension contains URL to TSA service
            keytool(
                    "-genkey",
                    "-v",
                    "-alias", TSA_KEY_ALIAS,
                    "-keyalg", KEY_ALG,
                    "-keysize", Integer.toString(KEY_SIZE),
                    "-keystore", KEYSTORE,
                    "-storepass", PASSWORD,
                    "-keypass", PASSWORD,
                    "-dname", "CN=TSA",
                    "-ext", "ExtendedkeyUsage:critical=timeStamping",
                    "-ext", "SubjectInfoAccess=timeStamping:URI:" + tsaUrl,
                    "-validity", Integer.toString(VALIDITY)).shouldHaveExitValue(0);

            tsa.setHandler(new TsaHandler(
                    KeyStoreUtils.loadKeyStore(KEYSTORE, PASSWORD), PASSWORD));

            // start TSA
            tsa.start();

            // sign jar file
            // specify -tsadigestalg option because
            // TSA server uses SHA-512 digest algorithm
             OutputAnalyzer analyzer = jarsigner(
                    "-J-Dhttp.proxyHost=",
                    "-J-Dhttp.proxyPort=",
                    "-J-Djava.net.useSystemProxies=",
                    "-verbose",
                    "-keystore", KEYSTORE,
                    "-storepass", PASSWORD,
                    "-keypass", PASSWORD,
                    "-signedjar", SIGNED_JARFILE,
                    "-tsacert", TSA_KEY_ALIAS,
                    "-tsadigestalg", "SHA-512",
                    UNSIGNED_JARFILE,
                    SIGNING_KEY_ALIAS);

            analyzer.shouldHaveExitValue(0);
            analyzer.stdoutShouldNotContain(WARNING);
            analyzer.shouldContain(JAR_SIGNED);

            // verify signed jar
            analyzer = jarsigner(
                    "-verbose",
                    "-verify",
                    "-keystore", KEYSTORE,
                    "-storepass", PASSWORD,
                    SIGNED_JARFILE);

            analyzer.shouldHaveExitValue(0);
            analyzer.stdoutShouldNotContain(WARNING);
            analyzer.shouldContain(JAR_VERIFIED);
        }

        System.out.println("Test passed");
    }

}
