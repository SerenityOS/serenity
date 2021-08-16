/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8242184 8242068
 * @summary keytool and jarsigner for all algorithms
 * @library /test/lib
 * @modules java.base/sun.security.util
 * @run testng/timeout=300 GenerateAll
 */

import jdk.test.lib.SecurityTools;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.security.DerUtils;
import jdk.test.lib.util.JarUtils;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

import static sun.security.util.KnownOIDs.*;

import sun.security.util.KnownOIDs;
import sun.security.util.ObjectIdentifier;
import sun.security.util.SignatureUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.util.Base64;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

public class GenerateAll {

    @BeforeTest
    public void beforeTest() throws Exception {
        // Create a CA in a separate keystore
        kt("-genkeypair -alias ca -dname CN=CA -keyalg ec -ext bc -keystore ca");
        kt("-export -alias ca -file ca.crt -rfc -keystore ca");

        // Import CA cert to user keystore so we can import reply later
        kt("-import -alias root -file ca.crt -noprompt");

        JarUtils.createJarFile(Path.of("a.jar"), Path.of("."), Path.of("ks"));
    }

    @DataProvider(name = "eddsa")
    public Object[][] eddsaData() {
        return new Object[][]{
                {"eddsa", null, Ed25519},
                {"eddsa", "eddsa", Ed25519},
                {"eddsa", "ed25519", Ed25519},
                {"eddsa", "ed448", null},
                {"ed25519", null, Ed25519},
                {"ed25519", "eddsa", Ed25519},
                {"ed25519", "ed25519", Ed25519},
                {"ed25519", "ed448", null},
                {"ed448", null, Ed448},
                {"ed448", "eddsa", Ed448},
                {"ed448", "ed25519", null},
                {"ed448", "ed448", Ed448},
        };
    }

    /**
     * Test various names of EdDSA
     * @param keyAlg keytool -keyalg
     * @param sigAlg (optional) keytool -sigalg
     * @param expected expected algorithm of generated signature
     */
    @Test(dataProvider = "eddsa")
    public void eddsaTest(String keyAlg, String sigAlg, KnownOIDs expected)
            throws Exception {
        String alias = keyAlg + "-" + sigAlg;
        OutputAnalyzer oa = kt0("-genkeypair -alias " + alias
                + " -dname CN=" + alias + " -keyalg " + keyAlg
                + (sigAlg == null ? "" : (" -sigalg " + sigAlg)));
        if (expected == null) {
            oa.shouldNotHaveExitValue(0);
        } else {
            oa.shouldHaveExitValue(0);
            kt("-alias " + alias + " -export -file " + alias + ".crt");
            byte[] crt = Files.readAllBytes(Path.of(alias + ".crt"));
            DerUtils.checkAlg(crt, "020", expected);     // tbsCertificate.signature
            DerUtils.checkAlg(crt, "0600", expected);    // tbsCertificate.subjectPublicKeyInfo.algorithm
            DerUtils.checkAlg(crt, "10", expected);      // signatureAlgorithm
        }
    }

    @DataProvider(name = "all")
    public Object[][] dataProvider() {
        return new Object[][]{
                {"rsa", "rsa", null, "RSA", SHA_256, SHA256withRSA},
                {"dsa", "dsa", null, "DSA", SHA_256, SHA256withDSA},
                {"r", "rsa", "rsassa-pss", "RSA", SHA_256, RSASSA_PSS},
                {"pss", "rsassa-pss", null, "RSA", SHA_256, RSASSA_PSS},
                {"ec", "ec", null, "EC", SHA_256, SHA256withECDSA},
                {"ed25519", "ed25519", null, "EC", SHA_512, Ed25519},
                {"ed448", "ed448", null, "EC", SHAKE256_LEN, Ed448},
        };
    }

    /**
     * Testing all algorithms.
     * @param alias alias
     * @param keyAlg keytool -keyalg
     * @param sigAlg (optional) keytool -sigalg
     * @param ext block extension inside signed JAR
     * @param expDigAlg expected digAlg in PKCS7 SignerInfo
     * @param expEncAlg expected encAlg in PKCS7 SignerInfo
     */
    @Test(dataProvider = "all")
    public void test(String alias, String keyAlg, String sigAlg, String ext,
                     KnownOIDs expDigAlg, KnownOIDs expEncAlg) throws Throwable {

        char[] pass = "changeit".toCharArray();

        // If no sigAlg, derive automatically
        String extra = sigAlg == null ? "" : (" -sigalg " + sigAlg);

        // gen
        kt("-genkeypair -alias " + alias + " -dname CN=" + alias
                + " -keyalg " + keyAlg + extra);
        kt("-export -alias " + alias + " -rfc -file " + alias + ".self");

        // req
        kt("-certreq -alias " + alias + " -file " + alias + ".req" + extra);
        kt("-printcertreq -file " + alias + ".req");

        // gencert
        kt("-gencert -alias ca -infile " + alias
                + ".req -outfile " + alias + ".crt -rfc -keystore ca");
        kt("-printcert -file " + alias + ".crt");
        kt("-importcert -alias " + alias + " -file " + alias + ".crt");

        // crl
        kt("-gencrl -alias " + alias + " -id 0 -rfc -file "
                + alias + ".crl" + extra);
        kt("-printcrl -file " + alias + ".crl")
                .shouldContain("Verified by " + alias);

        // sign
        js("a.jar " + alias + extra);

        // check data
        KeyStore ks = KeyStore.getInstance(new File("ks"), pass);
        PrivateKey pk = (PrivateKey)ks.getKey(alias, pass);

        if (sigAlg == null) {
            sigAlg = SignatureUtil.getDefaultSigAlgForKey(pk);
        }

        KnownOIDs sigOID = KnownOIDs.findMatch(sigAlg);
        KnownOIDs keyOID = KnownOIDs.findMatch(keyAlg);

        byte[] crt = read(alias + ".self");
        DerUtils.checkAlg(crt, "020", sigOID);  // tbsCertificate.signature
        DerUtils.checkAlg(crt, "0600", keyOID); // tbsCertificate.subjectPublicKeyInfo.algorithm
        assertEquals(
                DerUtils.innerDerValue(crt, "02"),   // tbsCertificate.signature
                DerUtils.innerDerValue(crt, "1"));   // signatureAlgorithm

        byte[] req = read(alias + ".req");
        DerUtils.checkAlg(req, "10", sigOID);   // signatureAlgorithm
        DerUtils.checkAlg(req, "0200", keyOID); // certificationRequestInfo.subjectPKInfo.algorithm

        byte[] crl = read(alias + ".crl");
        DerUtils.checkAlg(crl, "000", sigOID);  // tbsCertList.signature
        assertEquals(
                DerUtils.innerDerValue(crl, "00"),   // tbsCertList.signature
                DerUtils.innerDerValue(crl, "1"));   // signatureAlgorithm

        try (JarFile jf = new JarFile("a.jar")) {
            JarEntry je = jf.getJarEntry(
                    "META-INF/" + alias.toUpperCase() + "." + ext);
            byte[] p7 = jf.getInputStream(je).readAllBytes();
            // SignerInfo.digestAlgorithm
            DerUtils.checkAlg(p7, "104020", expDigAlg);
            // SignerInfo.signatureAlgorithm
            if (DerUtils.innerDerValue(p7, "10403").isContextSpecific()) {
                // SignerInfo has signedAttributes at 104030
                DerUtils.checkAlg(p7, "104040", expEncAlg);
            } else {
                DerUtils.checkAlg(p7, "104030", expEncAlg);
            }
        }
    }

    @AfterTest
    public void afterTest() throws Exception {
        js("-verify a.jar -verbose -certs");
    }

    static byte[] read(String f) throws IOException {
        try (var v = Files.lines(Path.of(f))) {
            return Base64.getDecoder().decode(v.filter(s -> !s.startsWith("-----"))
                    .collect(Collectors.joining("")));
        }
    }

    static OutputAnalyzer kt(String arg) throws Exception {
        return kt0(arg).shouldHaveExitValue(0);
    }

    static OutputAnalyzer kt0(String arg) throws Exception {
        return SecurityTools.keytool("-keystore ks -storepass changeit " + arg);
    }

    static OutputAnalyzer js(String arg) throws Exception {
        return SecurityTools.jarsigner("-keystore ks -storepass changeit " + arg)
                .shouldHaveExitValue(0);
    }
}
