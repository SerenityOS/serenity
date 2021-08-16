/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8259401 8266225
 * @summary Check certificates in signer's cert chain to see if warning emitted
 * @library /test/lib
 */

import jdk.test.lib.SecurityTools;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.util.JarUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class CheckSignerCertChain {

    private static final String JAVA_SECURITY_FILE = "java.security";

    static OutputAnalyzer kt(String cmd, String ks) throws Exception {
        return SecurityTools.keytool("-storepass changeit " + cmd +
                " -keystore " + ks);
    }

    static void gencert(String owner, String cmd) throws Exception {
        kt("-certreq -alias " + owner + " -file tmp.req", "ks");
        kt("-gencert -infile tmp.req -outfile tmp.cert " + cmd, "ks");
        kt("-importcert -alias " + owner + " -file tmp.cert", "ks");
    }

    public static void main(String[] args) throws Exception {

        // root certificate using SHA1withRSA and 1024-bit key
        System.out.println("Generating a root cert using SHA1withRSA and 1024-bit key");
        kt("-genkeypair -keyalg rsa -alias ca -dname CN=CA -ext bc:c " +
                "-keysize 1024 -sigalg SHA1withRSA", "ks");
        kt("-genkeypair -keyalg rsa -alias ca1 -dname CN=CA1", "ks");
        kt("-genkeypair -keyalg rsa -alias e1 -dname CN=E1", "ks");

        // intermediate certificate using SHA1withRSA and 2048-bit key
        System.out.println("Generating an intermediate cert using SHA1withRSA and 2048-bit key");
        gencert("ca1", "-alias ca -ext san=dns:ca1 -ext bc:c " +
                "-sigalg SHA1withRSA ");

        // end entity certificate using SHA256withRSA and 2048-bit key
        System.out.println("Generating an end entity cert using SHA256withRSA and 2048-bit key");
        gencert("e1", "-alias ca1 -ext san=dns:e1 ");

        JarUtils.createJarFile(Path.of("a.jar"), Path.of("."), Path.of("ks"));

        SecurityTools.jarsigner("-keystore ks -storepass changeit " +
                "-signedjar signeda.jar " +
                "-sigalg SHA256withRSA " +
                "-verbose" +
                " a.jar e1")
                .shouldContain("Signature algorithm: SHA1withRSA (weak), 2048-bit key")
                // For trusted cert, warning should be generated for its weak 1024-bit
                // key, but not for its SHA1withRSA algorithm.
                .shouldContain("Signature algorithm: SHA1withRSA, 1024-bit key (weak)")
                .shouldHaveExitValue(0);

        kt("-exportcert -alias ca -rfc -file cacert", "ks");
        kt("-importcert -noprompt -file cacert", "caks");

        SecurityTools.jarsigner("-verify -certs signeda.jar " +
                "-keystore caks -storepass changeit -verbose -debug")
                .shouldContain("Signature algorithm: SHA1withRSA (weak), 2048-bit key")
                // For trusted cert, warning should be generated for its weak 1024-bit
                // key, but not for its SHA1withRSA algorithm.
                .shouldContain("Signature algorithm: SHA1withRSA, 1024-bit key (weak)")
                .shouldHaveExitValue(0);

        /*
         * Generate a non-self-signed certificate using MD5withRSA as its signature
         * algorithm to sign a JAR file.
         */
        kt("-genkeypair -keyalg rsa -alias cacert -dname CN=CACERT -ext bc:c ", "ks");
        kt("-genkeypair -keyalg rsa -alias ee -dname CN=EE -ext bc:c ", "ks");
        gencert("ee", "-alias cacert -ext san=dns:ee -sigalg MD5withRSA");

        Files.writeString(Files.createFile(Paths.get(JAVA_SECURITY_FILE)),
                "jdk.certpath.disabledAlgorithms=\n" +
                "jdk.jar.disabledAlgorithms=MD5\n");

        SecurityTools.jarsigner("-keystore ks -storepass changeit " +
                "-signedjar signeda.jar " +
                "-verbose " +
                "-J-Djava.security.properties=" +
                JAVA_SECURITY_FILE +
                " a.jar ee")
                .shouldNotContain("Signature algorithm: MD5withRSA (disabled), 2048-bit key")
                .shouldContain("Signature algorithm: SHA256withRSA, 2048-bit key")
                .shouldNotContain("Invalid certificate chain: Algorithm constraints check failed on signature algorithm: MD5withRSA")
                .shouldHaveExitValue(0);

        Files.deleteIfExists(Paths.get(JAVA_SECURITY_FILE));
        Files.writeString(Files.createFile(Paths.get(JAVA_SECURITY_FILE)),
                "jdk.certpath.disabledAlgorithms=MD5\n" +
                "jdk.jar.disabledAlgorithms=\n");

        SecurityTools.jarsigner("-keystore ks -storepass changeit " +
                "-signedjar signeda.jar " +
                "-verbose " +
                "-J-Djava.security.properties=" +
                JAVA_SECURITY_FILE +
                " a.jar ee")
                .shouldContain("Signature algorithm: MD5withRSA (disabled), 2048-bit key")
                .shouldContain("Signature algorithm: SHA256withRSA, 2048-bit key")
                .shouldContain("Invalid certificate chain: Algorithm constraints check failed on signature algorithm: MD5withRSA")
                .shouldHaveExitValue(0);

        kt("-exportcert -alias cacert -rfc -file cacert", "ks");
        kt("-importcert -noprompt -file cacert", "caks1");

        SecurityTools.jarsigner("-verify -certs signeda.jar " +
                "-keystore caks1 -storepass changeit -verbose -debug")
                .shouldContain("Signature algorithm: MD5withRSA (disabled), 2048-bit key")
                .shouldContain("Signature algorithm: SHA256withRSA, 2048-bit key")
                .shouldContain("Invalid certificate chain: Algorithm constraints check failed on signature algorithm: MD5withRSA")
                .shouldHaveExitValue(0);
    }
}
