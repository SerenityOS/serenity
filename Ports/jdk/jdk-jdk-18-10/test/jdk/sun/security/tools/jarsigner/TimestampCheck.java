/*
 * Copyright (c) 2003, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import com.sun.net.httpserver.HttpExchange;

import jdk.test.lib.SecurityTools;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.security.KeyStoreUtils;
import jdk.test.lib.security.timestamp.*;
import jdk.test.lib.util.JarUtils;
import sun.security.pkcs.PKCS7;
import sun.security.pkcs.PKCS9Attribute;
import sun.security.pkcs.SignerInfo;
import sun.security.timestamp.TimestampToken;

/*
 * @test
 * @bug 6543842 6543440 6939248 8009636 8024302 8163304 8169911 8180289 8172404 8247960 8242068
 * @summary checking response of timestamp
 * @modules java.base/sun.security.pkcs
 *          java.base/sun.security.timestamp
 *          java.base/sun.security.x509
 *          java.base/sun.security.util
 *          java.base/sun.security.tools.keytool
 * @library /lib/testlibrary
 * @library /test/lib
 * @build jdk.test.lib.util.JarUtils
 *        jdk.test.lib.SecurityTools
 *        jdk.test.lib.Utils
 *        jdk.test.lib.Asserts
 *        jdk.test.lib.JDKToolFinder
 *        jdk.test.lib.JDKToolLauncher
 *        jdk.test.lib.Platform
 *        jdk.test.lib.process.*
 * @compile -XDignore.symbol.file TimestampCheck.java
 * @run main/timeout=600 TimestampCheck
 */
public class TimestampCheck {

    private static final String PASSWORD = "changeit";
    private static final String defaultPolicyId = "2.3.4";
    private static String host = null;

    private static class Interceptor implements RespInterceptor {

        private final String path;

        Interceptor(String path) {
            this.path = path;
        }

        @Override
        public X509Certificate[] getSignerCertChain(
                X509Certificate[] signerCertChain, boolean certReq)
                throws Exception {
            if (path.equals("fullchain")) { // Only case 5 uses full chain
                return signerCertChain;
            } else if (path.equals("nocert")) {
                return new X509Certificate[0];
            } else {
                return new X509Certificate[] { signerCertChain[0] };
            }
        }

        @Override
        public String getSigAlgo(String sigAlgo) throws Exception {
            return "SHA256withRSA";
        }

        @Override
        public TsaParam getRespParam(TsaParam reqParam) {
            TsaParam respParam = RespInterceptor.super.getRespParam(reqParam);

            String policyId
                    = respParam.policyId() == null || path.equals("diffpolicy")
                    ? TimestampCheck.defaultPolicyId
                    : respParam.policyId();
            respParam.policyId(policyId);

            String digestAlgo = respParam.digestAlgo();
            if (path.equals("diffalg")) {
                digestAlgo = digestAlgo.contains("256")
                        ? "SHA-1" : "SHA-256";
            }
            respParam.digestAlgo(digestAlgo);

            byte[] hashedMessage = respParam.hashedMessage();
            if (path.equals("baddigest")) {
                hashedMessage[hashedMessage.length - 1] = (byte) 0x01;
                hashedMessage[hashedMessage.length - 2] = (byte) 0x02;
                hashedMessage[hashedMessage.length - 3] = (byte) 0x03;
            }
            respParam.hashedMessage(hashedMessage);

            Instant instant = Instant.now();
            if (path.equals("tsold")) {
                instant = instant.minus(20, ChronoUnit.DAYS);
            }
            respParam.genTime(Date.from(instant));

            BigInteger nonce = respParam.nonce();
            if (path.equals("diffnonce")) {
                nonce = BigInteger.valueOf(1234);
            } else if (path.equals("nononce")) {
                nonce = null;
            }
            respParam.nonce(nonce);

            return respParam;
        }
    }

    private static class Handler extends TsaHandler {

        Handler(String keystore) throws Exception {
            super(KeyStoreUtils.loadKeyStore(keystore, PASSWORD), PASSWORD);
        }

        public TsaSigner createSigner(HttpExchange exchange)
                throws Exception {
            String path = exchange.getRequestURI().getPath().substring(1);

            SignerEntry signerEntry = createSignerEntry(
                    path.startsWith("ts") ? path : "ts");
            byte[] requestData = exchange.getRequestBody().readAllBytes();
            RespInterceptor interceptor = new Interceptor(path);
            return new TsaSigner(signerEntry, requestData, interceptor);
        }
    }

    private static TsaServer initServer(String keystore) throws Exception {
        return new TsaServer(0, new Handler(keystore));
    }

    public static void main(String[] args) throws Throwable {
        prepare();

        try (TsaServer tsa = initServer("ks");) {
            tsa.start();
            int port = tsa.getPort();
            host = "http://localhost:" + port + "/";

            if (args.length == 0) {         // Run this test

                sign("normal")
                        .shouldNotContain("Warning")
                        .shouldContain("The signer certificate will expire on")
                        .shouldContain("The timestamp will expire on")
                        .shouldHaveExitValue(0);

                verify("normal.jar")
                        .shouldNotContain("Warning")
                        .shouldHaveExitValue(0);

                verify("normal.jar", "-verbose")
                        .shouldNotContain("Warning")
                        .shouldContain("The signer certificate will expire on")
                        .shouldContain("The timestamp will expire on")
                        .shouldHaveExitValue(0);

                // Simulate signing at a previous date:
                // 1. tsold will create a timestamp of 20 days ago.
                // 2. oldsigner expired 10 days ago.
                signVerbose("tsold", "unsigned.jar", "tsold.jar", "oldsigner")
                        .shouldNotContain("Warning")
                        .shouldMatch("signer certificate expired on .*. "
                                + "However, the JAR will be valid")
                        .shouldHaveExitValue(0);

                // It verifies perfectly.
                verify("tsold.jar", "-verbose", "-certs")
                        .shouldNotContain("Warning")
                        .shouldMatch("signer certificate expired on .*. "
                                + "However, the JAR will be valid")
                        .shouldHaveExitValue(0);

                // No timestamp
                signVerbose(null, "unsigned.jar", "none.jar", "signer")
                        .shouldContain("is not timestamped")
                        .shouldContain("The signer certificate will expire on")
                        .shouldHaveExitValue(0);

                verify("none.jar", "-verbose")
                        .shouldContain("do not include a timestamp")
                        .shouldContain("The signer certificate will expire on")
                        .shouldHaveExitValue(0);

                // Error cases

                signVerbose(null, "unsigned.jar", "badku.jar", "badku")
                        .shouldContain("KeyUsage extension doesn't allow code signing")
                        .shouldHaveExitValue(8);
                checkBadKU("badku.jar");

                // 8180289: unvalidated TSA cert chain
                sign("tsnoca")
                        .shouldContain("The TSA certificate chain is invalid. "
                                + "Reason: Path does not chain with any of the trust anchors")
                        .shouldHaveExitValue(64);

                verify("tsnoca.jar", "-verbose", "-certs")
                        .shouldHaveExitValue(64)
                        .shouldContain("jar verified")
                        .shouldContain("Invalid TSA certificate chain: "
                                + "Path does not chain with any of the trust anchors")
                        .shouldContain("TSA certificate chain is invalid."
                                + " Reason: Path does not chain with any of the trust anchors");

                sign("nononce")
                        .shouldContain("Nonce missing in timestamp token")
                        .shouldHaveExitValue(1);
                sign("diffnonce")
                        .shouldContain("Nonce changed in timestamp token")
                        .shouldHaveExitValue(1);
                sign("baddigest")
                        .shouldContain("Digest octets changed in timestamp token")
                        .shouldHaveExitValue(1);
                sign("diffalg")
                        .shouldContain("Digest algorithm not")
                        .shouldHaveExitValue(1);

                sign("fullchain")
                        .shouldHaveExitValue(0);   // Success, 6543440 solved.

                sign("tsbad1")
                        .shouldContain("Certificate is not valid for timestamping")
                        .shouldHaveExitValue(1);
                sign("tsbad2")
                        .shouldContain("Certificate is not valid for timestamping")
                        .shouldHaveExitValue(1);
                sign("tsbad3")
                        .shouldContain("Certificate is not valid for timestamping")
                        .shouldHaveExitValue(1);
                sign("nocert")
                        .shouldContain("Certificate not included in timestamp token")
                        .shouldHaveExitValue(1);

                sign("policy", "-tsapolicyid",  "1.2.3")
                        .shouldHaveExitValue(0);
                checkTimestamp("policy.jar", "1.2.3", "SHA-256");

                sign("diffpolicy", "-tsapolicyid", "1.2.3")
                        .shouldContain("TSAPolicyID changed in timestamp token")
                        .shouldHaveExitValue(1);

                sign("sha384alg", "-tsadigestalg", "SHA-384")
                        .shouldHaveExitValue(0);
                checkTimestamp("sha384alg.jar", defaultPolicyId, "SHA-384");

                // Legacy algorithms
                signVerbose(null, "unsigned.jar", "sha1alg.jar", "signer",
                        "-strict", "-digestalg", "SHA-1")
                        .shouldHaveExitValue(0)
                        .shouldContain("jar signed")
                        .shouldNotContain("with signer errors")
                        .shouldMatch("SHA-1.*-digestalg.*will be disabled");
                verify("sha1alg.jar", "-strict")
                        .shouldHaveExitValue(0)
                        .shouldContain("jar verified")
                        .shouldNotContain("with signer errors")
                        .shouldContain("SHA-1 digest algorithm is considered a security risk")
                        .shouldContain("This algorithm will be disabled in a future update")
                        .shouldNotContain("is disabled");

                sign("sha1tsaalg", "-tsadigestalg", "SHA-1", "-strict")
                        .shouldHaveExitValue(0)
                        .shouldContain("jar signed")
                        .shouldNotContain("with signer errors")
                        .shouldMatch("SHA-1.*-tsadigestalg.*will be disabled")
                        .shouldNotContain("is disabled");
                verify("sha1tsaalg.jar", "-strict")
                        .shouldHaveExitValue(0)
                        .shouldContain("jar verified")
                        .shouldNotContain("with signer errors")
                        .shouldContain("SHA-1 timestamp digest algorithm is considered a security risk")
                        .shouldNotContain("is disabled");

                // Disabled algorithms
                sign("tsdisabled", "-digestalg", "MD5",
                                "-sigalg", "MD5withRSA", "-tsadigestalg", "MD5")
                        .shouldHaveExitValue(68)
                        .shouldContain("TSA certificate chain is invalid")
                        .shouldMatch("MD5.*-digestalg.*is disabled")
                        .shouldMatch("MD5.*-tsadigestalg.*is disabled")
                        .shouldMatch("MD5withRSA.*-sigalg.*is disabled");
                checkDisabled("tsdisabled.jar");

                signVerbose("tsdisabled", "unsigned.jar", "tsdisabled2.jar", "signer")
                        .shouldHaveExitValue(64)
                        .shouldContain("TSA certificate chain is invalid");

                // Disabled timestamp is an error and jar treated unsigned
                verify("tsdisabled2.jar", "-verbose")
                        .shouldHaveExitValue(16)
                        .shouldContain("treated as unsigned")
                        .shouldMatch("Timestamp.*512.*(disabled)");

                // Algorithm used in signing is disabled
                signVerbose("normal", "unsigned.jar", "halfDisabled.jar", "signer",
                        "-digestalg", "MD5")
                        .shouldContain("-digestalg option is considered a security risk and is disabled")
                        .shouldHaveExitValue(4);
                checkHalfDisabled("halfDisabled.jar");

                // sign with DSA key
                signVerbose("normal", "unsigned.jar", "sign1.jar", "dsakey")
                        .shouldHaveExitValue(0);

                // sign with RSAkeysize < 1024
                signVerbose("normal", "sign1.jar", "sign2.jar", "disabledkeysize")
                        .shouldContain("Algorithm constraints check failed on keysize")
                        .shouldHaveExitValue(4);
                checkMultiple("sign2.jar");

                // signed with everyone
                signVerbose("normal", "unsigned.jar", "signall.jar", "signer",
                        "-sigalg", "SHA3-256withRSA")
                        .shouldHaveExitValue(0);
                signVerbose("normal", "signall.jar", "signall.jar", "dsakey")
                        .shouldHaveExitValue(0);
                signVerbose("normal", "signall.jar", "signall.jar", "eckey")
                        .shouldHaveExitValue(0);
                signVerbose("normal", "signall.jar", "signall.jar", "psskey")
                        .shouldHaveExitValue(0);
                signVerbose("normal", "signall.jar", "signall.jar", "edkey")
                        .shouldHaveExitValue(0);
                verify("signall.jar", "-verbose")
                        .shouldHaveExitValue(0)
                        .shouldContain("Signature algorithm: SHA3-256withRSA")
                        .shouldContain("Signature algorithm: RSASSA-PSS")
                        .shouldContain("Signature algorithm: SHA256withECDSA")
                        .shouldContain("Signature algorithm: Ed25519")
                        .shouldContain("Signature algorithm: SHA256withDSA");

                // Legacy algorithms
                sign("tsweak", "-digestalg", "SHA1",
                                "-sigalg", "SHA1withRSA", "-tsadigestalg", "SHA1")
                        .shouldHaveExitValue(0)
                        .shouldMatch("SHA1.*-digestalg.*will be disabled")
                        .shouldMatch("SHA1.*-tsadigestalg.*will be disabled")
                        .shouldMatch("SHA1withRSA.*-sigalg.*will be disabled");
                checkWeak("tsweak.jar");

                signVerbose("tsweak", "unsigned.jar", "tsweak2.jar", "signer")
                        .shouldHaveExitValue(0);

                verify("tsweak2.jar", "-verbose")
                        .shouldHaveExitValue(0)
                        .shouldContain("jar verified")
                        .shouldMatch("Timestamp.*1024.*(weak)");

                // Algorithm used in signing is weak
                signVerbose("normal", "unsigned.jar", "halfWeak.jar", "signer",
                        "-digestalg", "SHA1")
                        .shouldContain("-digestalg option is considered a security risk.")
                        .shouldContain("This algorithm will be disabled in a future update.")
                        .shouldHaveExitValue(0);
                checkHalfWeak("halfWeak.jar");

                // sign with DSA key
                signVerbose("normal", "unsigned.jar", "sign1.jar", "dsakey")
                        .shouldHaveExitValue(0);

                // sign with RSAkeysize < 2048
                signVerbose("normal", "sign1.jar", "sign2.jar", "weakkeysize")
                        .shouldNotContain("Algorithm constraints check failed on keysize")
                        .shouldHaveExitValue(0);
                checkMultipleWeak("sign2.jar");


                // 8191438: jarsigner should print when a timestamp will expire
                checkExpiration();

                // When .SF or .RSA is missing or invalid
                checkMissingOrInvalidFiles("normal.jar");

                if (Files.exists(Paths.get("ts2.cert"))) {
                    checkInvalidTsaCertKeyUsage();
                }
            } else {                        // Run as a standalone server
                System.out.println("TSA started at " + host
                        + ". Press Enter to quit server");
                System.in.read();
            }
        }
    }

    private static void checkExpiration() throws Exception {

        // Warning when expired or expiring
        signVerbose(null, "unsigned.jar", "expired.jar", "expired")
                .shouldContain("signer certificate has expired")
                .shouldHaveExitValue(4);
        verify("expired.jar")
                .shouldContain("signer certificate has expired")
                .shouldHaveExitValue(4);
        signVerbose(null, "unsigned.jar", "expiring.jar", "expiring")
                .shouldContain("signer certificate will expire within")
                .shouldHaveExitValue(0);
        verify("expiring.jar")
                .shouldContain("signer certificate will expire within")
                .shouldHaveExitValue(0);
        // Info for long
        signVerbose(null, "unsigned.jar", "long.jar", "long")
                .shouldNotContain("signer certificate has expired")
                .shouldNotContain("signer certificate will expire within")
                .shouldContain("signer certificate will expire on")
                .shouldHaveExitValue(0);
        verify("long.jar")
                .shouldNotContain("signer certificate has expired")
                .shouldNotContain("signer certificate will expire within")
                .shouldNotContain("The signer certificate will expire")
                .shouldHaveExitValue(0);
        verify("long.jar", "-verbose")
                .shouldContain("The signer certificate will expire")
                .shouldHaveExitValue(0);

        // Both expired
        signVerbose("tsexpired", "unsigned.jar",
                "tsexpired-expired.jar", "expired")
                .shouldContain("The signer certificate has expired.")
                .shouldContain("The timestamp has expired.")
                .shouldHaveExitValue(4);
        verify("tsexpired-expired.jar")
                .shouldContain("signer certificate has expired")
                .shouldContain("timestamp has expired.")
                .shouldHaveExitValue(4);

        // TS expired but signer still good
        signVerbose("tsexpired", "unsigned.jar",
                "tsexpired-long.jar", "long")
                .shouldContain("The timestamp expired on")
                .shouldHaveExitValue(0);
        verify("tsexpired-long.jar")
                .shouldMatch("timestamp expired on.*However, the JAR will be valid")
                .shouldNotContain("Error")
                .shouldHaveExitValue(0);

        signVerbose("tsexpired", "unsigned.jar",
                "tsexpired-ca.jar", "ca")
                .shouldContain("The timestamp has expired.")
                .shouldHaveExitValue(4);
        verify("tsexpired-ca.jar")
                .shouldNotContain("timestamp has expired")
                .shouldNotContain("Error")
                .shouldHaveExitValue(0);

        // Warning when expiring
        sign("tsexpiring")
                .shouldContain("timestamp will expire within")
                .shouldHaveExitValue(0);
        verify("tsexpiring.jar")
                .shouldContain("timestamp will expire within")
                .shouldNotContain("still valid")
                .shouldHaveExitValue(0);

        signVerbose("tsexpiring", "unsigned.jar",
                "tsexpiring-ca.jar", "ca")
                .shouldContain("self-signed")
                .stderrShouldNotMatch("The.*expir")
                .shouldHaveExitValue(4); // self-signed
        verify("tsexpiring-ca.jar")
                .stderrShouldNotMatch("The.*expir")
                .shouldHaveExitValue(0);

        signVerbose("tsexpiringsoon", "unsigned.jar",
                "tsexpiringsoon-long.jar", "long")
                .shouldContain("The timestamp will expire")
                .shouldHaveExitValue(0);
        verify("tsexpiringsoon-long.jar")
                .shouldMatch("timestamp will expire.*However, the JAR will be valid until")
                .shouldHaveExitValue(0);

        // Info for long
        sign("tslong")
                .shouldNotContain("timestamp has expired")
                .shouldNotContain("timestamp will expire within")
                .shouldContain("timestamp will expire on")
                .shouldContain("signer certificate will expire on")
                .shouldHaveExitValue(0);
        verify("tslong.jar")
                .shouldNotContain("timestamp has expired")
                .shouldNotContain("timestamp will expire within")
                .shouldNotContain("timestamp will expire on")
                .shouldNotContain("signer certificate will expire on")
                .shouldHaveExitValue(0);
        verify("tslong.jar", "-verbose")
                .shouldContain("timestamp will expire on")
                .shouldContain("signer certificate will expire on")
                .shouldHaveExitValue(0);
    }

    private static void checkInvalidTsaCertKeyUsage() throws Exception {

        // Hack: Rewrite the TSA cert inside normal.jar into ts2.jar.

        // Both the cert and the serial number must be rewritten.
        byte[] tsCert = Files.readAllBytes(Paths.get("ts.cert"));
        byte[] ts2Cert = Files.readAllBytes(Paths.get("ts2.cert"));
        byte[] tsSerial = getCert(tsCert)
                .getSerialNumber().toByteArray();
        byte[] ts2Serial = getCert(ts2Cert)
                .getSerialNumber().toByteArray();

        byte[] oldBlock;
        try (JarFile normal = new JarFile("normal.jar")) {
            oldBlock = normal.getInputStream(
                    normal.getJarEntry("META-INF/SIGNER.RSA")).readAllBytes();
        }

        JarUtils.updateJar("normal.jar", "ts2.jar",
                Map.of("META-INF/SIGNER.RSA",
                        updateBytes(updateBytes(oldBlock, tsCert, ts2Cert),
                                tsSerial, ts2Serial)));

        verify("ts2.jar", "-verbose", "-certs")
                .shouldHaveExitValue(64)
                .shouldContain("jar verified")
                .shouldContain("Invalid TSA certificate chain: Extended key usage does not permit use for TSA server");
    }

    public static X509Certificate getCert(byte[] data)
            throws CertificateException, IOException {
        return (X509Certificate)
                CertificateFactory.getInstance("X.509")
                        .generateCertificate(new ByteArrayInputStream(data));
    }

    private static byte[] updateBytes(byte[] old, byte[] from, byte[] to) {
        int pos = 0;
        while (true) {
            if (pos + from.length > old.length) {
                return null;
            }
            if (Arrays.equals(Arrays.copyOfRange(old, pos, pos+from.length), from)) {
                byte[] result = old.clone();
                System.arraycopy(to, 0, result, pos, from.length);
                return result;
            }
            pos++;
        }
    }

    private static void checkMissingOrInvalidFiles(String s)
            throws Throwable {

        JarUtils.updateJar(s, "1.jar", Map.of("META-INF/SIGNER.SF", Boolean.FALSE));
        verify("1.jar", "-verbose")
                .shouldHaveExitValue(16)
                .shouldContain("treated as unsigned")
                .shouldContain("Missing signature-related file META-INF/SIGNER.SF");
        JarUtils.updateJar(s, "2.jar", Map.of("META-INF/SIGNER.RSA", Boolean.FALSE));
        verify("2.jar", "-verbose")
                .shouldHaveExitValue(16)
                .shouldContain("treated as unsigned")
                .shouldContain("Missing block file for signature-related file META-INF/SIGNER.SF");
        JarUtils.updateJar(s, "3.jar", Map.of("META-INF/SIGNER.SF", "dummy"));
        verify("3.jar", "-verbose")
                .shouldHaveExitValue(16)
                .shouldContain("treated as unsigned")
                .shouldContain("Unparsable signature-related file META-INF/SIGNER.SF");
        JarUtils.updateJar(s, "4.jar", Map.of("META-INF/SIGNER.RSA", "dummy"));
        verify("4.jar", "-verbose")
                .shouldHaveExitValue(16)
                .shouldContain("treated as unsigned")
                .shouldContain("Unparsable signature-related file META-INF/SIGNER.RSA");
    }

    static OutputAnalyzer jarsigner(List<String> extra)
            throws Exception {
        List<String> args = new ArrayList<>(
                List.of("-keystore", "ks", "-storepass", "changeit"));
        args.addAll(extra);
        return SecurityTools.jarsigner(args);
    }

    static OutputAnalyzer verify(String file, String... extra)
            throws Exception {
        List<String> args = new ArrayList<>();
        args.add("-verify");
        args.add("-strict");
        args.add(file);
        args.addAll(Arrays.asList(extra));
        return jarsigner(args);
    }

    static void checkBadKU(String file) throws Exception {
        verify(file)
                .shouldHaveExitValue(16)
                .shouldContain("treated as unsigned")
                .shouldContain("re-run jarsigner with debug enabled");
        verify(file, "-verbose")
                .shouldHaveExitValue(16)
                .shouldContain("Signed by")
                .shouldContain("treated as unsigned")
                .shouldContain("re-run jarsigner with debug enabled");
        verify(file, "-J-Djava.security.debug=jar")
                .shouldHaveExitValue(16)
                .shouldContain("SignatureException: Key usage restricted")
                .shouldContain("treated as unsigned")
                .shouldContain("re-run jarsigner with debug enabled");
    }

    static void checkDisabled(String file) throws Exception {
        verify(file)
                .shouldHaveExitValue(16)
                .shouldContain("treated as unsigned")
                .shouldMatch("weak algorithm that is now disabled.")
                .shouldMatch("Re-run jarsigner with the -verbose option for more details");
        verify(file, "-verbose")
                .shouldHaveExitValue(16)
                .shouldContain("treated as unsigned")
                .shouldMatch("weak algorithm that is now disabled by")
                .shouldMatch("Digest algorithm: .*(disabled)")
                .shouldMatch("Signature algorithm: .*(disabled)")
                .shouldMatch("Timestamp digest algorithm: .*(disabled)")
                .shouldNotMatch("Timestamp signature algorithm: .*(weak).*(weak)")
                .shouldMatch("Timestamp signature algorithm: .*key.*(disabled)");
        verify(file, "-J-Djava.security.debug=jar")
                .shouldHaveExitValue(16)
                .shouldMatch("SignatureException:.*keysize");

        // For 8171319: keytool should print out warnings when reading or
        //              generating cert/cert req using disabled algorithms.
        // Must call keytool the command, otherwise doPrintCert() might not
        // be able to reset "jdk.certpath.disabledAlgorithms".
        String sout = SecurityTools.keytool("-printcert -jarfile " + file)
                .stderrShouldContain("The TSA certificate uses a 512-bit RSA key" +
                        " which is considered a security risk and is disabled.")
                .getStdout();
        if (sout.indexOf("disabled", sout.indexOf("Timestamp:")) < 0) {
            throw new RuntimeException("timestamp not disabled: " + sout);
        }
    }

    static void checkHalfDisabled(String file) throws Exception {
        verify(file)
                .shouldHaveExitValue(16)
                .shouldContain("treated as unsigned")
                .shouldMatch("weak algorithm that is now disabled.")
                .shouldMatch("Re-run jarsigner with the -verbose option for more details");
        verify(file, "-verbose")
                .shouldHaveExitValue(16)
                .shouldContain("treated as unsigned")
                .shouldMatch("weak algorithm that is now disabled by")
                .shouldMatch("Digest algorithm: .*(disabled)")
                .shouldNotMatch("Signature algorithm: .*(weak)")
                .shouldNotMatch("Signature algorithm: .*(disabled)")
                .shouldNotMatch("Timestamp digest algorithm: .*(disabled)")
                .shouldNotMatch("Timestamp signature algorithm: .*(weak).*(weak)")
                .shouldNotMatch("Timestamp signature algorithm: .*(disabled).*(disabled)")
                .shouldNotMatch("Timestamp signature algorithm: .*key.*(weak)")
                .shouldNotMatch("Timestamp signature algorithm: .*key.*(disabled)");
     }

    static void checkMultiple(String file) throws Exception {
        verify(file)
                .shouldHaveExitValue(0)
                .shouldContain("jar verified");
        verify(file, "-verbose", "-certs")
                .shouldHaveExitValue(0)
                .shouldContain("jar verified")
                .shouldMatch("X.509.*CN=dsakey")
                .shouldNotMatch("X.509.*CN=disabledkeysize")
                .shouldMatch("Signed by .*CN=dsakey")
                .shouldMatch("Signed by .*CN=disabledkeysize")
                .shouldMatch("Signature algorithm: .*key.*(disabled)");
    }

    static void checkWeak(String file) throws Exception {
        verify(file)
                .shouldHaveExitValue(0)
                .shouldNotContain("treated as unsigned");
        verify(file, "-verbose")
                .shouldHaveExitValue(0)
                .shouldNotContain("treated as unsigned")
                .shouldMatch("Digest algorithm: .*(weak)")
                .shouldMatch("Signature algorithm: .*(weak)")
                .shouldMatch("Timestamp digest algorithm: .*(weak)")
                .shouldNotMatch("Timestamp signature algorithm: .*(weak).*(weak)")
                .shouldMatch("Timestamp signature algorithm: .*key.*(weak)");
        verify(file, "-J-Djava.security.debug=jar")
                .shouldHaveExitValue(0)
                .shouldNotMatch("SignatureException:.*disabled");

        // keytool should print out warnings when reading or
        // generating cert/cert req using legacy algorithms.
        String sout = SecurityTools.keytool("-printcert -jarfile " + file)
                .stderrShouldContain("The TSA certificate uses a 1024-bit RSA key" +
                        " which is considered a security risk." +
                        " This key size will be disabled in a future update.")
                .getStdout();
        if (sout.indexOf("weak", sout.indexOf("Timestamp:")) < 0) {
            throw new RuntimeException("timestamp not weak: " + sout);
        }
    }

    static void checkHalfWeak(String file) throws Exception {
        verify(file)
                .shouldHaveExitValue(0)
                .shouldNotContain("treated as unsigned");
        verify(file, "-verbose")
                .shouldHaveExitValue(0)
                .shouldNotContain("treated as unsigned")
                .shouldMatch("Digest algorithm: .*(weak)")
                .shouldNotMatch("Signature algorithm: .*(weak)")
                .shouldNotMatch("Signature algorithm: .*(disabled)")
                .shouldNotMatch("Timestamp digest algorithm: .*(weak)")
                .shouldNotMatch("Timestamp signature algorithm: .*(weak).*(weak)")
                .shouldNotMatch("Timestamp signature algorithm: .*(disabled).*(disabled)")
                .shouldNotMatch("Timestamp signature algorithm: .*key.*(weak)")
                .shouldNotMatch("Timestamp signature algorithm: .*key.*(disabled)");
    }

    static void checkMultipleWeak(String file) throws Exception {
        verify(file)
                .shouldHaveExitValue(0)
                .shouldContain("jar verified");
        verify(file, "-verbose", "-certs")
                .shouldHaveExitValue(0)
                .shouldContain("jar verified")
                .shouldMatch("X.509.*CN=dsakey")
                .shouldMatch("X.509.*CN=weakkeysize")
                .shouldMatch("Signed by .*CN=dsakey")
                .shouldMatch("Signed by .*CN=weakkeysize")
                .shouldMatch("Signature algorithm: .*key.*(weak)");
    }

    static void checkTimestamp(String file, String policyId, String digestAlg)
            throws Exception {
        try (JarFile jf = new JarFile(file)) {
            JarEntry je = jf.getJarEntry("META-INF/SIGNER.RSA");
            try (InputStream is = jf.getInputStream(je)) {
                byte[] content = is.readAllBytes();
                PKCS7 p7 = new PKCS7(content);
                SignerInfo[] si = p7.getSignerInfos();
                if (si == null || si.length == 0) {
                    throw new Exception("Not signed");
                }
                PKCS9Attribute p9 = si[0].getUnauthenticatedAttributes()
                        .getAttribute(PKCS9Attribute.SIGNATURE_TIMESTAMP_TOKEN_OID);
                PKCS7 tsToken = new PKCS7((byte[]) p9.getValue());
                TimestampToken tt =
                        new TimestampToken(tsToken.getContentInfo().getData());
                if (!tt.getHashAlgorithm().toString().equals(digestAlg)) {
                    throw new Exception("Digest alg different");
                }
                if (!tt.getPolicyID().equals(policyId)) {
                    throw new Exception("policyId different");
                }
            }
        }
    }

    static int which = 0;

    /**
     * Sign with a TSA path. Always use alias "signer" to sign "unsigned.jar".
     * The signed jar name is always path.jar.
     *
     * @param extra more args given to jarsigner
     */
    static OutputAnalyzer sign(String path, String... extra)
            throws Exception {
        return signVerbose(
                path,
                "unsigned.jar",
                path + ".jar",
                "signer",
                extra);
    }

    static OutputAnalyzer signVerbose(
            String path,    // TSA URL path
            String oldJar,
            String newJar,
            String alias,   // signer
            String...extra) throws Exception {
        which++;
        System.out.println("\n>> Test #" + which);
        List<String> args = new ArrayList<>(List.of(
                "-strict", "-verbose", "-debug", "-signedjar", newJar, oldJar, alias));
        if (path != null) {
            args.add("-tsa");
            args.add(host + path);
        }
        args.addAll(Arrays.asList(extra));
        return jarsigner(args);
    }

    static void prepare() throws Exception {
        JarUtils.createJar("unsigned.jar", "A");
        Files.deleteIfExists(Paths.get("ks"));
        keytool("-alias signer -genkeypair -ext bc -dname CN=signer");
        keytool("-alias oldsigner -genkeypair -dname CN=oldsigner");
        keytool("-alias dsakey -genkeypair -keyalg DSA -dname CN=dsakey");
        keytool("-alias eckey -genkeypair -keyalg EC -dname CN=eckey");
        keytool("-alias psskey -genkeypair -keyalg RSASSA-PSS -dname CN=psskey");
        keytool("-alias edkey -genkeypair -keyalg Ed25519 -dname CN=edkey");
        keytool("-alias weakkeysize -genkeypair -keysize 1024 -dname CN=weakkeysize");
        keytool("-alias disabledkeysize -genkeypair -keysize 512 -dname CN=disabledkeysize");
        keytool("-alias badku -genkeypair -dname CN=badku");
        keytool("-alias ts -genkeypair -dname CN=ts");
        keytool("-alias tsold -genkeypair -dname CN=tsold");
        keytool("-alias tsweak -genkeypair -keysize 1024 -dname CN=tsweak");
        keytool("-alias tsdisabled -genkeypair -keysize 512 -dname CN=tsdisabled");
        keytool("-alias tsbad1 -genkeypair -dname CN=tsbad1");
        keytool("-alias tsbad2 -genkeypair -dname CN=tsbad2");
        keytool("-alias tsbad3 -genkeypair -dname CN=tsbad3");
        keytool("-alias tsnoca -genkeypair -dname CN=tsnoca");

        keytool("-alias expired -genkeypair -dname CN=expired");
        keytool("-alias expiring -genkeypair -dname CN=expiring");
        keytool("-alias long -genkeypair -dname CN=long");
        keytool("-alias tsexpired -genkeypair -dname CN=tsexpired");
        keytool("-alias tsexpiring -genkeypair -dname CN=tsexpiring");
        keytool("-alias tsexpiringsoon -genkeypair -dname CN=tsexpiringsoon");
        keytool("-alias tslong -genkeypair -dname CN=tslong");

        // tsnoca's issuer will be removed from keystore later
        keytool("-alias ca -genkeypair -ext bc -dname CN=CA");
        gencert("tsnoca", "-ext eku:critical=ts");
        keytool("-delete -alias ca");
        keytool("-alias ca -genkeypair -ext bc -dname CN=CA -startdate -40d");

        gencert("signer");
        gencert("oldsigner", "-startdate -30d -validity 20");
        gencert("dsakey");
        gencert("eckey");
        gencert("psskey");
        gencert("edkey");
        gencert("weakkeysize");
        gencert("disabledkeysize");
        gencert("badku", "-ext ku:critical=keyAgreement");
        gencert("ts", "-ext eku:critical=ts -validity 500");

        gencert("expired", "-validity 10 -startdate -12d");
        gencert("expiring", "-validity 178");
        gencert("long", "-validity 182");
        gencert("tsexpired", "-ext eku:critical=ts -validity 10 -startdate -12d");
        gencert("tsexpiring", "-ext eku:critical=ts -validity 364");
        gencert("tsexpiringsoon", "-ext eku:critical=ts -validity 170"); // earlier than expiring
        gencert("tslong", "-ext eku:critical=ts -validity 367");


        for (int i = 0; i < 5; i++) {
            // Issue another cert for "ts" with a different EKU.
            // Length might be different because serial number is
            // random. Try several times until a cert with the same
            // length is generated so we can substitute ts.cert
            // embedded in the PKCS7 block with ts2.cert.
            // If cannot create one, related test will be ignored.
            keytool("-gencert -alias ca -infile ts.req -outfile ts2.cert " +
                    "-ext eku:critical=1.3.6.1.5.5.7.3.9");
            if (Files.size(Paths.get("ts.cert")) != Files.size(Paths.get("ts2.cert"))) {
                Files.delete(Paths.get("ts2.cert"));
                System.out.println("Warning: cannot create same length");
            } else {
                break;
            }
        }

        gencert("tsold", "-ext eku:critical=ts -startdate -40d -validity 500");

        gencert("tsweak", "-ext eku:critical=ts");
        gencert("tsdisabled", "-ext eku:critical=ts");
        gencert("tsbad1");
        gencert("tsbad2", "-ext eku=ts");
        gencert("tsbad3", "-ext eku:critical=cs");
    }

    static void gencert(String alias, String... extra) throws Exception {
        keytool("-alias " + alias + " -certreq -file " + alias + ".req");
        String genCmd = "-gencert -alias ca -infile " +
                alias + ".req -outfile " + alias + ".cert";
        for (String s : extra) {
            genCmd += " " + s;
        }
        keytool(genCmd);
        keytool("-alias " + alias + " -importcert -file " + alias + ".cert");
    }

    static void keytool(String cmd) throws Exception {
        cmd = "-keystore ks -storepass changeit -keypass changeit " +
                "-keyalg rsa -validity 200 " + cmd;
        sun.security.tools.keytool.Main.main(cmd.split(" "));
    }
}
