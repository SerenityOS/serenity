/*
 * Copyright (c) 2017, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8171319 8177569 8182879 8172404
 * @summary keytool should print out warnings when reading or generating
  *         cert/cert req using weak algorithms
 * @library /test/lib
 * @modules java.base/sun.security.tools.keytool
 *          java.base/sun.security.tools
 *          java.base/sun.security.util
 * @build jdk.test.lib.SecurityTools
 *        jdk.test.lib.Utils
 *        jdk.test.lib.Asserts
 *        jdk.test.lib.JDKToolFinder
 *        jdk.test.lib.JDKToolLauncher
 *        jdk.test.lib.Platform
 *        jdk.test.lib.process.*
 * @run main/othervm/timeout=600 -Duser.language=en -Duser.country=US WeakAlg
 */

import jdk.test.lib.Asserts;
import jdk.test.lib.SecurityTools;
import jdk.test.lib.process.OutputAnalyzer;
import sun.security.tools.KeyStoreUtil;
import sun.security.util.DisabledAlgorithmConstraints;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.CryptoPrimitive;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class WeakAlg {

    public static void main(String[] args) throws Throwable {

        rm("ks");

        // Tests for "disabled" algorithms
        // -genkeypair, and -printcert, -list -alias, -exportcert
        // (w/ different formats)
        checkDisabledGenKeyPair("a", "-keyalg RSA -sigalg MD5withRSA", "MD5withRSA");
        checkDisabledGenKeyPair("b", "-keyalg RSA -keysize 512", "512-bit RSA key");
        checkDisabledGenKeyPair("c", "-keyalg RSA", null);

        kt("-list")
                .shouldContain("Warning:")
                .shouldMatch("<a>.*MD5withRSA.*is disabled")
                .shouldMatch("<b>.*512-bit RSA key.*is disabled");
        kt("-list -v")
                .shouldContain("Warning:")
                .shouldMatch("<a>.*MD5withRSA.*is disabled")
                .shouldContain("MD5withRSA (disabled)")
                .shouldMatch("<b>.*512-bit RSA key.*is disabled")
                .shouldContain("512-bit RSA key (disabled)");

        // Multiple warnings for multiple cert in -printcert
        // or -list or -exportcert

        // -certreq, -printcertreq, -gencert
        checkDisabledCertReq("a", "", null);
        gencert("c-a", "")
                .shouldNotContain("Warning"); // new sigalg is not weak
        gencert("c-a", "-sigalg MD2withRSA")
                .shouldContain("Warning:")
                .shouldMatch("The generated certificate.*MD2withRSA.*is disabled");

        checkDisabledCertReq("a", "-sigalg MD5withRSA", "MD5withRSA");
        gencert("c-a", "")
                .shouldContain("Warning:")
                .shouldMatch("The certificate request.*MD5withRSA.*is disabled");
        gencert("c-a", "-sigalg MD2withRSA")
                .shouldContain("Warning:")
                .shouldMatch("The certificate request.*MD5withRSA.*is disabled")
                .shouldMatch("The generated certificate.*MD2withRSA.*is disabled");

        checkDisabledCertReq("b", "", "512-bit RSA key");
        gencert("c-b", "")
                .shouldContain("Warning:")
                .shouldMatch("The certificate request.*512-bit RSA key.*is disabled")
                .shouldMatch("The generated certificate.*512-bit RSA key.*is disabled");

        checkDisabledCertReq("c", "", null);
        gencert("a-c", "")
                .shouldContain("Warning:")
                .shouldMatch("The issuer.*MD5withRSA.*is disabled");

        // but the new cert is not weak
        kt("-printcert -file a-c.cert")
                .shouldNotContain("Warning")
                .shouldNotContain("(disabled)");

        gencert("b-c", "")
                .shouldContain("Warning:")
                .shouldMatch("The issuer.*512-bit RSA key.*is disabled");

        // -importcert
        checkImport();

        // -importkeystore
        checkImportKeyStore();

        // -gencrl, -printcrl

        checkDisabledGenCRL("a", "", null);
        checkDisabledGenCRL("a", "-sigalg MD5withRSA", "MD5withRSA");
        checkDisabledGenCRL("b", "", "512-bit RSA key");
        checkDisabledGenCRL("c", "", null);

        kt("-delete -alias b");
        kt("-printcrl -file b.crl")
                .shouldContain("WARNING: not verified");

        jksTypeCheck();

        checkInplaceImportKeyStore();

        rm("ks");

        // Tests for "legacy" algorithms
        // -genkeypair, and -printcert, -list -alias, -exportcert
        // (w/ different formats)
        checkWeakGenKeyPair("x", "-keyalg RSA -sigalg SHA1withRSA", "SHA1withRSA");
        checkWeakGenKeyPair("y", "-keyalg RSA -keysize 1024", "1024-bit RSA key");
        checkWeakGenKeyPair("z", "-keyalg RSA", null);

        kt("-list")
                .shouldContain("Warning:")
                .shouldMatch("<x>.*SHA1withRSA.*will be disabled")
                .shouldMatch("<y>.*1024-bit RSA key.*will be disabled");
        kt("-list -v")
                .shouldContain("Warning:")
                .shouldMatch("<x>.*SHA1withRSA.*will be disabled")
                .shouldContain("SHA1withRSA (weak)")
                .shouldMatch("<y>.*1024-bit RSA key.*will be disabled")
                .shouldContain("1024-bit RSA key (weak)");

        // Multiple warnings for multiple cert in -printcert
        // or -list or -exportcert

        // -certreq, -printcertreq, -gencert
        checkWeakCertReq("x", "", null);
        gencert("z-x", "")
                .shouldNotContain("Warning"); // new sigalg is not weak
        gencert("z-x", "-sigalg SHA1withRSA")
                .shouldContain("Warning:")
                .shouldMatch("The generated certificate.*SHA1withRSA.*will be disabled");

        checkWeakCertReq("x", "-sigalg SHA1withRSA", "SHA1withRSA");
        gencert("z-x", "")
                .shouldContain("Warning:")
                .shouldMatch("The certificate request.*SHA1withRSA.*will be disabled");
        gencert("z-x", "-sigalg SHA1withRSA")
                .shouldContain("Warning:")
                .shouldMatch("The certificate request.*SHA1withRSA.*will be disabled")
                .shouldMatch("The generated certificate.*SHA1withRSA.*will be disabled");

        checkWeakCertReq("y", "", "1024-bit RSA key");
        gencert("z-y", "")
                .shouldContain("Warning:")
                .shouldMatch("The certificate request.*1024-bit RSA key.*will be disabled")
                .shouldMatch("The generated certificate.*1024-bit RSA key.*will be disabled");

        checkWeakCertReq("z", "", null);
        gencert("x-z", "")
                .shouldContain("Warning:")
                .shouldMatch("The issuer.*SHA1withRSA.*will be disabled");

        // but the new cert is not weak
        kt("-printcert -file x-z.cert")
                .shouldNotContain("Warning")
                .shouldNotContain("weak");

        gencert("y-z", "")
                .shouldContain("Warning:")
                .shouldMatch("The issuer.*1024-bit RSA key.*will be disabled");

        // -gencrl, -printcrl
        checkWeakGenCRL("x", "", null);
        checkWeakGenCRL("x", "-sigalg SHA1withRSA", "SHA1withRSA");
        checkWeakGenCRL("y", "", "1024-bit RSA key");
        checkWeakGenCRL("z", "", null);

        kt("-delete -alias y");
        kt("-printcrl -file y.crl")
                .shouldContain("WARNING: not verified");

        jksTypeCheck();
    }

    static void jksTypeCheck() throws Exception {

        // No warning for cacerts, all certs
        kt0("-cacerts -list -storepass changeit")
                .shouldNotContain("proprietary format");

        rm("ks");
        rm("ks2");

        kt("-genkeypair -keyalg DSA -alias a -dname CN=A")
                .shouldNotContain("Warning:");
        kt("-list")
                .shouldNotContain("Warning:");
        kt("-list -storetype jks") // no warning if PKCS12 used as JKS
                .shouldNotContain("Warning:");
        kt("-exportcert -alias a -file a.crt")
                .shouldNotContain("Warning:");

        // warn if migrating to JKS
        importkeystore("ks", "ks2", "-deststoretype jks")
                .shouldContain("JKS keystore uses a proprietary format");

        rm("ks");
        rm("ks2");
        rm("ks3");

        // no warning if all certs
        kt("-importcert -alias b -file a.crt -storetype jks -noprompt")
                .shouldNotContain("Warning:");
        kt("-genkeypair -keyalg DSA -alias a -dname CN=A")
                .shouldContain("JKS keystore uses a proprietary format");
        kt("-list")
                .shouldContain("JKS keystore uses a proprietary format");
        kt("-list -storetype pkcs12") // warn if JKS used as PKCS12
                .shouldContain("JKS keystore uses a proprietary format");
        kt("-exportcert -alias a -file a.crt")
                .shouldContain("JKS keystore uses a proprietary format");
        kt("-printcert -file a.crt") // warning since -keystore option is supported
                .shouldContain("JKS keystore uses a proprietary format");
        kt("-certreq -alias a -file a.req")
                .shouldContain("JKS keystore uses a proprietary format");
        kt("-printcertreq -file a.req") // no warning if keystore not touched
                .shouldNotContain("Warning:");

        // No warning if migrating from JKS
        importkeystore("ks", "ks2", "")
                .shouldNotContain("Warning:");

        importkeystore("ks", "ks3", "-deststoretype pkcs12")
                .shouldNotContain("Warning:");

        rm("ks");

        kt("-genkeypair -keyalg DSA -alias a -dname CN=A -storetype jceks")
                .shouldContain("JCEKS keystore uses a proprietary format");
        kt("-list")
                .shouldContain("JCEKS keystore uses a proprietary format");
        kt("-importcert -alias b -file a.crt -noprompt")
                .shouldContain("JCEKS keystore uses a proprietary format");
        kt("-exportcert -alias a -file a.crt")
                .shouldContain("JCEKS keystore uses a proprietary format");
        kt("-printcert -file a.crt") // warning since -keystore option is supported
                .shouldContain("JCEKS keystore uses a proprietary format");
        kt("-certreq -alias a -file a.req")
                .shouldContain("JCEKS keystore uses a proprietary format");
        kt("-printcertreq -file a.req")
                .shouldNotContain("Warning:");
        kt("-genseckey -alias c -keyalg AES -keysize 128")
                .shouldContain("JCEKS keystore uses a proprietary format");
    }

    static void checkImportKeyStore() throws Exception {

        rm("ks2");
        rm("ks3");

        importkeystore("ks", "ks2", "")
                .shouldContain("3 entries successfully imported")
                .shouldContain("Warning")
                .shouldMatch("<b>.*512-bit RSA key.*is disabled")
                .shouldMatch("<a>.*MD5withRSA.*is disabled");

        importkeystore("ks", "ks3", "-srcalias a")
                .shouldContain("Warning")
                .shouldMatch("<a>.*MD5withRSA.*is disabled");
    }

    static void checkInplaceImportKeyStore() throws Exception {

        rm("ks");
        genkeypair("a", "-keyalg DSA");

        // Same type backup
        importkeystore("ks", "ks", "")
                .shouldContain("Warning:")
                .shouldMatch("original.*ks.old");

        importkeystore("ks", "ks", "")
                .shouldContain("Warning:")
                .shouldMatch("original.*ks.old2");

        importkeystore("ks", "ks", "-srcstoretype jks") // it knows real type
                .shouldContain("Warning:")
                .shouldMatch("original.*ks.old3");

        String cPath = new File("ks").getCanonicalPath();

        importkeystore("ks", cPath, "")
                .shouldContain("Warning:")
                .shouldMatch("original.*ks.old4");

        // Migration
        importkeystore("ks", "ks", "-deststoretype jks")
                .shouldContain("Warning:")
                .shouldContain("JKS keystore uses a proprietary format")
                .shouldMatch("Migrated.*JKS.*PKCS12.*ks.old5");

        Asserts.assertEQ(
                KeyStore.getInstance(
                        new File("ks"), "changeit".toCharArray()).getType(),
                "JKS");

        importkeystore("ks", "ks", "-srcstoretype PKCS12")
                .shouldContain("Warning:")
                .shouldNotContain("proprietary format")
                .shouldMatch("Migrated.*PKCS12.*JKS.*ks.old6");

        Asserts.assertEQ(
                KeyStore.getInstance(
                        new File("ks"), "changeit".toCharArray()).getType(),
                "PKCS12");

        Asserts.assertEQ(
                KeyStore.getInstance(
                        new File("ks.old6"), "changeit".toCharArray()).getType(),
                "JKS");

        // One password prompt is enough for migration
        kt0("-importkeystore -srckeystore ks -destkeystore ks", "changeit")
                .shouldMatch("original.*ks.old7");

        // But three if importing to a different keystore
        rm("ks2");
        kt0("-importkeystore -srckeystore ks -destkeystore ks2",
                    "changeit")
                .shouldContain("Keystore password is too short");

        kt0("-importkeystore -srckeystore ks -destkeystore ks2",
                "changeit", "changeit", "changeit")
                .shouldContain("Importing keystore ks to ks2...")
                .shouldNotContain("original")
                .shouldNotContain("Migrated");
    }

    static void checkImport() throws Exception {

        saveStore();

        // add trusted cert

        // cert already in
        kt("-importcert -alias d -file a.cert", "no")
                .shouldContain("Certificate already exists in keystore")
                .shouldContain("Warning")
                .shouldMatch("The input.*MD5withRSA.*is disabled")
                .shouldContain("Do you still want to add it?");
        kt("-importcert -alias d -file a.cert -noprompt")
                .shouldContain("Warning")
                .shouldMatch("The input.*MD5withRSA.*is disabled")
                .shouldNotContain("[no]");

        // cert is self-signed
        kt("-delete -alias a");
        kt("-delete -alias d");
        kt("-importcert -alias d -file a.cert", "no")
                .shouldContain("Warning")
                .shouldContain("MD5withRSA (disabled)")
                .shouldMatch("The input.*MD5withRSA.*is disabled")
                .shouldContain("Trust this certificate?");
        kt("-importcert -alias d -file a.cert -noprompt")
                .shouldContain("Warning")
                .shouldMatch("The input.*MD5withRSA.*is disabled")
                .shouldNotContain("[no]");

        // JDK-8177569: no warning for sigalg of trusted cert
        String weakSigAlgCA = null;
        KeyStore ks = KeyStoreUtil.getCacertsKeyStore();
        if (ks != null) {
            DisabledAlgorithmConstraints disabledCheck =
                    new DisabledAlgorithmConstraints(
                            DisabledAlgorithmConstraints.PROPERTY_CERTPATH_DISABLED_ALGS);
            Set<CryptoPrimitive> sigPrimitiveSet = Collections
                    .unmodifiableSet(EnumSet.of(CryptoPrimitive.SIGNATURE));

            for (String s : Collections.list(ks.aliases())) {
                if (ks.isCertificateEntry(s)) {
                    X509Certificate c = (X509Certificate)ks.getCertificate(s);
                    String sigAlg = c.getSigAlgName();
                    if (!disabledCheck.permits(sigPrimitiveSet, sigAlg, null)) {
                        weakSigAlgCA = sigAlg;
                        Files.write(Paths.get("ca.cert"),
                                ks.getCertificate(s).getEncoded());
                        break;
                    }
                }
            }
        }
        if (weakSigAlgCA != null) {
            // The following 2 commands still have a warning on why not using
            // the -cacerts option directly.
            kt("-list -keystore " + KeyStoreUtil.getCacerts())
                    .shouldNotMatch("signature algorithm.*risk");
            kt("-list -v -keystore " + KeyStoreUtil.getCacerts())
                    .shouldNotMatch("signature algorithm.*risk");

            // -printcert will always show warnings
            kt("-printcert -file ca.cert")
                    .shouldContain("name: " + weakSigAlgCA + " (disabled)")
                    .shouldContain("Warning")
                    .shouldMatch("The certificate.*" + weakSigAlgCA + ".*is disabled");
            kt("-printcert -file ca.cert -trustcacerts") // -trustcacerts useless
                    .shouldContain("name: " + weakSigAlgCA + " (disabled)")
                    .shouldContain("Warning")
                    .shouldMatch("The certificate.*" + weakSigAlgCA + ".*is disabled");

            // Importing with -trustcacerts ignore CA cert's sig alg
            kt("-delete -alias d");
            kt("-importcert -alias d -trustcacerts -file ca.cert", "no")
                    .shouldContain("Certificate already exists in system-wide CA")
                    .shouldNotMatch("signature algorithm.*risk")
                    .shouldContain("Do you still want to add it to your own keystore?");
            kt("-importcert -alias d -trustcacerts -file ca.cert -noprompt")
                    .shouldNotMatch("signature algorithm.*risk")
                    .shouldNotContain("[no]");

            // but not without -trustcacerts
            kt("-delete -alias d");
            kt("-importcert -alias d -file ca.cert", "no")
                    .shouldContain("name: " + weakSigAlgCA + " (disabled)")
                    .shouldContain("Warning")
                    .shouldMatch("The input.*" + weakSigAlgCA + ".*is disabled")
                    .shouldContain("Trust this certificate?");
            kt("-importcert -alias d -file ca.cert -noprompt")
                    .shouldContain("Warning")
                    .shouldMatch("The input.*" + weakSigAlgCA + ".*is disabled")
                    .shouldNotContain("[no]");
        }

        // a non self-signed weak cert
        reStore();
        certreq("b", "");
        gencert("c-b", "");
        kt("-importcert -alias d -file c-b.cert")   // weak only, no prompt
                .shouldContain("Warning")
                .shouldNotContain("512-bit RSA key (disabled)")
                .shouldMatch("The input.*512-bit RSA key.*is disabled")
                .shouldNotContain("[no]");

        kt("-delete -alias b");
        kt("-delete -alias c");
        kt("-delete -alias d");

        kt("-importcert -alias d -file c-b.cert", "no") // weak and not trusted
                .shouldContain("Warning")
                .shouldContain("512-bit RSA key (disabled)")
                .shouldMatch("The input.*512-bit RSA key.*is disabled")
                .shouldContain("Trust this certificate?");
        kt("-importcert -alias d -file c-b.cert -noprompt")
                .shouldContain("Warning")
                .shouldMatch("The input.*512-bit RSA key.*is disabled")
                .shouldNotContain("[no]");

        // a non self-signed strong cert
        reStore();
        certreq("a", "");
        gencert("c-a", "");
        kt("-importcert -alias d -file c-a.cert") // trusted
                .shouldNotContain("Warning")
                .shouldNotContain("[no]");

        kt("-delete -alias a");
        kt("-delete -alias c");
        kt("-delete -alias d");

        kt("-importcert -alias d -file c-a.cert", "no") // not trusted
                .shouldNotContain("Warning")
                .shouldContain("Trust this certificate?");
        kt("-importcert -alias d -file c-a.cert -noprompt")
                .shouldNotContain("Warning")
                .shouldNotContain("[no]");

        // install reply

        reStore();
        certreq("c", "");
        gencert("a-c", "");
        kt("-importcert -alias c -file a-c.cert")
                .shouldContain("Warning")
                .shouldMatch("Issuer <a>.*MD5withRSA.*is disabled");

        // JDK-8177569: no warning for sigalg of trusted cert
        reStore();
        // Change a into a TrustedCertEntry
        kt("-exportcert -alias a -file a.cert");
        kt("-delete -alias a");
        kt("-importcert -alias a -file a.cert -noprompt");
        kt("-list -alias a -v")
                .shouldNotContain("disabled")
                .shouldNotContain("Warning");
        // This time a is trusted and no warning on its weak sig alg
        kt("-importcert -alias c -file a-c.cert")
                .shouldNotContain("Warning");

        reStore();

        gencert("a-b", "");
        gencert("b-c", "");

        // Full chain with root
        cat("a-a-b-c.cert", "b-c.cert", "a-b.cert", "a.cert");
        kt("-importcert -alias c -file a-a-b-c.cert")   // only weak
                .shouldContain("Warning")
                .shouldMatch("Reply #2 of 3.*512-bit RSA key.*is disabled")
                .shouldMatch("Reply #3 of 3.*MD5withRSA.*is disabled")
                .shouldNotContain("[no]");

        // Without root
        cat("a-b-c.cert", "b-c.cert", "a-b.cert");
        kt("-importcert -alias c -file a-b-c.cert")     // only weak
                .shouldContain("Warning")
                .shouldMatch("Reply #2 of 2.*512-bit RSA key.*is disabled")
                .shouldMatch("Issuer <a>.*MD5withRSA.*is disabled")
                .shouldNotContain("[no]");

        reStore();
        gencert("b-a", "");

        kt("-importcert -alias a -file b-a.cert")
                .shouldContain("Warning")
                .shouldMatch("Issuer <b>.*512-bit RSA key.*is disabled")
                .shouldNotContain("[no]");

        kt("-importcert -alias a -file c-a.cert")
                .shouldNotContain("Warning");

        kt("-importcert -alias b -file c-b.cert")
                .shouldContain("Warning")
                .shouldMatch("The input.*512-bit RSA key.*is disabled")
                .shouldNotContain("[no]");

        reStore();
        gencert("b-a", "");

        cat("c-b-a.cert", "b-a.cert", "c-b.cert");

        kt("-printcert -file c-b-a.cert")
                .shouldContain("Warning")
                .shouldMatch("The certificate #2 of 2.*512-bit RSA key.*is disabled");

        kt("-delete -alias b");

        kt("-importcert -alias a -file c-b-a.cert")
                .shouldContain("Warning")
                .shouldMatch("Reply #2 of 2.*512-bit RSA key.*is disabled")
                .shouldNotContain("[no]");

        kt("-delete -alias c");
        kt("-importcert -alias a -file c-b-a.cert", "no")
                .shouldContain("Top-level certificate in reply:")
                .shouldContain("512-bit RSA key (disabled)")
                .shouldContain("Warning")
                .shouldMatch("Reply #2 of 2.*512-bit RSA key.*is disabled")
                .shouldContain("Install reply anyway?");
        kt("-importcert -alias a -file c-b-a.cert -noprompt")
                .shouldContain("Warning")
                .shouldMatch("Reply #2 of 2.*512-bit RSA key.*is disabled")
                .shouldNotContain("[no]");

        reStore();
    }

    private static void cat(String dest, String... src) throws IOException {
        System.out.println("---------------------------------------------");
        System.out.printf("$ cat ");

        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        for (String s : src) {
            System.out.printf(s + " ");
            bout.write(Files.readAllBytes(Paths.get(s)));
        }
        Files.write(Paths.get(dest), bout.toByteArray());
        System.out.println("> " + dest);
    }

    static void checkDisabledGenCRL(String alias, String options, String bad) {

        OutputAnalyzer oa = kt("-gencrl -alias " + alias
                + " -id 1 -file " + alias + ".crl " + options);
        if (bad == null) {
            oa.shouldNotContain("Warning");
        } else {
            oa.shouldContain("Warning")
                    .shouldMatch("The generated CRL.*" + bad + ".*is disabled");
        }

        oa = kt("-printcrl -file " + alias + ".crl");
        if (bad == null) {
            oa.shouldNotContain("Warning")
                    .shouldContain("Verified by " + alias + " in keystore")
                    .shouldNotContain("(disabled");
        } else {
            oa.shouldContain("Warning:")
                    .shouldMatch("The CRL.*" + bad + ".*is disabled")
                    .shouldContain("Verified by " + alias + " in keystore")
                    .shouldContain(bad + " (disabled)");
        }
    }

    static void checkDisabledCertReq(
            String alias, String options, String bad) {

        OutputAnalyzer oa = certreq(alias, options);
        if (bad == null) {
            oa.shouldNotContain("Warning");
        } else {
            oa.shouldContain("Warning")
                    .shouldMatch("The generated certificate request.*" + bad + ".*is disabled");
        }

        oa = kt("-printcertreq -file " + alias + ".req");
        if (bad == null) {
            oa.shouldNotContain("Warning")
                    .shouldNotContain("(disabled)");
        } else {
            oa.shouldContain("Warning")
                    .shouldMatch("The certificate request.*" + bad + ".*is disabled")
                    .shouldContain(bad + " (disabled)");
        }
    }

    static void checkDisabledGenKeyPair(
            String alias, String options, String bad) {

        OutputAnalyzer oa = genkeypair(alias, options);
        if (bad == null) {
            oa.shouldNotContain("Warning");
        } else {
            oa.shouldContain("Warning")
                    .shouldMatch("The generated certificate.*" + bad + ".*is disabled");
        }

        oa = kt("-exportcert -alias " + alias + " -file " + alias + ".cert");
        if (bad == null) {
            oa.shouldNotContain("Warning");
        } else {
            oa.shouldContain("Warning")
                    .shouldMatch("The certificate.*" + bad + ".*is disabled");
        }

        oa = kt("-exportcert -rfc -alias " + alias + " -file " + alias + ".cert");
        if (bad == null) {
            oa.shouldNotContain("Warning");
        } else {
            oa.shouldContain("Warning")
                    .shouldMatch("The certificate.*" + bad + ".*is disabled");
        }

        oa = kt("-printcert -rfc -file " + alias + ".cert");
        if (bad == null) {
            oa.shouldNotContain("Warning");
        } else {
            oa.shouldContain("Warning")
                    .shouldMatch("The certificate.*" + bad + ".*is disabled");
        }

        oa = kt("-list -alias " + alias);
        if (bad == null) {
            oa.shouldNotContain("Warning");
        } else {
            oa.shouldContain("Warning")
                    .shouldMatch("The certificate.*" + bad + ".*is disabled");
        }

        // With cert content

        oa = kt("-printcert -file " + alias + ".cert");
        if (bad == null) {
            oa.shouldNotContain("Warning");
        } else {
            oa.shouldContain("Warning")
                    .shouldContain(bad + " (disabled)")
                    .shouldMatch("The certificate.*" + bad + ".*is disabled");
        }

        oa = kt("-list -v -alias " + alias);
        if (bad == null) {
            oa.shouldNotContain("Warning");
        } else {
            oa.shouldContain("Warning")
                    .shouldContain(bad + " (disabled)")
                    .shouldMatch("The certificate.*" + bad + ".*is disabled");
        }
    }

    static void checkWeakGenKeyPair(
            String alias, String options, String bad) {

        OutputAnalyzer oa = genkeypair(alias, options);
        if (bad == null) {
            oa.shouldNotContain("Warning");
        } else {
            oa.shouldContain("Warning")
                    .shouldMatch("The generated certificate.*" + bad + ".*will be disabled");
        }

        oa = kt("-exportcert -alias " + alias + " -file " + alias + ".cert");
        if (bad == null) {
            oa.shouldNotContain("Warning");
        } else {
            oa.shouldContain("Warning")
                    .shouldMatch("The certificate.*" + bad + ".*will be disabled");
        }

        oa = kt("-exportcert -rfc -alias " + alias + " -file " + alias + ".cert");
        if (bad == null) {
            oa.shouldNotContain("Warning");
        } else {
            oa.shouldContain("Warning")
                    .shouldMatch("The certificate.*" + bad + ".*will be disabled");
        }

        oa = kt("-printcert -rfc -file " + alias + ".cert");
        if (bad == null) {
            oa.shouldNotContain("Warning");
        } else {
            oa.shouldContain("Warning")
                    .shouldMatch("The certificate.*" + bad + ".*will be disabled");
        }

        oa = kt("-list -alias " + alias);
        if (bad == null) {
            oa.shouldNotContain("Warning");
        } else {
            oa.shouldContain("Warning")
                    .shouldMatch("The certificate.*" + bad + ".*will be disabled");
        }

        // With cert content

        oa = kt("-printcert -file " + alias + ".cert");
        if (bad == null) {
            oa.shouldNotContain("Warning");
        } else {
            oa.shouldContain("Warning")
                    .shouldContain(bad + " (weak)")
                    .shouldMatch("The certificate.*" + bad + ".*will be disabled");
        }

        oa = kt("-list -v -alias " + alias);
        if (bad == null) {
            oa.shouldNotContain("Warning");
        } else {
            oa.shouldContain("Warning")
                    .shouldContain(bad + " (weak)")
                    .shouldMatch("The certificate.*" + bad + ".*will be disabled");
        }
    }


    static void checkWeakGenCRL(String alias, String options, String bad) {

        OutputAnalyzer oa = kt("-gencrl -alias " + alias
                + " -id 1 -file " + alias + ".crl " + options);
        if (bad == null) {
            oa.shouldNotContain("Warning");
        } else {
            oa.shouldContain("Warning")
                    .shouldMatch("The generated CRL.*" + bad + ".*will be disabled");
        }

        oa = kt("-printcrl -file " + alias + ".crl");
        if (bad == null) {
            oa.shouldNotContain("Warning")
                    .shouldContain("Verified by " + alias + " in keystore")
                    .shouldNotContain("(weak");
        } else {
            oa.shouldContain("Warning:")
                    .shouldMatch("The CRL.*" + bad + ".*will be disabled")
                    .shouldContain("Verified by " + alias + " in keystore")
                    .shouldContain(bad + " (weak)");
        }
    }

    static void checkWeakCertReq(
            String alias, String options, String bad) {

        OutputAnalyzer oa = certreq(alias, options);
        if (bad == null) {
            oa.shouldNotContain("Warning");
        } else {
            oa.shouldContain("Warning")
                    .shouldMatch("The generated certificate request.*" + bad + ".*will be disabled");
        }

        oa = kt("-printcertreq -file " + alias + ".req");
        if (bad == null) {
            oa.shouldNotContain("Warning")
                    .shouldNotContain("(weak)");
        } else {
            oa.shouldContain("Warning")
                    .shouldMatch("The certificate request.*" + bad + ".*will be disabled")
                    .shouldContain(bad + " (weak)");
        }
    }

    // This is slow, but real keytool process is launched.
    static OutputAnalyzer kt1(String cmd, String... input) {
        cmd = "-keystore ks -storepass changeit " +
                "-keypass changeit " + cmd;
        System.out.println("---------------------------------------------");
        try {
            SecurityTools.setResponse(input);
            return SecurityTools.keytool(cmd);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    static OutputAnalyzer kt(String cmd, String... input) {
        return kt0("-keystore ks -storepass changeit " +
                "-keypass changeit " + cmd, input);
    }

    // Fast keytool execution by directly calling its main() method
    static OutputAnalyzer kt0(String cmd, String... input) {
        PrintStream out = System.out;
        PrintStream err = System.err;
        InputStream ins = System.in;
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        ByteArrayOutputStream berr = new ByteArrayOutputStream();
        boolean succeed = true;
        String sout;
        String serr;
        try {
            System.out.println("---------------------------------------------");
            System.out.println("$ keytool " + cmd);
            System.out.println();
            String feed = "";
            if (input.length > 0) {
                feed = Stream.of(input).collect(Collectors.joining("\n")) + "\n";
            }
            System.setIn(new ByteArrayInputStream(feed.getBytes()));
            System.setOut(new PrintStream(bout));
            System.setErr(new PrintStream(berr));
            sun.security.tools.keytool.Main.main(
                    cmd.trim().split("\\s+"));
        } catch (Exception e) {
            // Might be a normal exception when -debug is on or
            // SecurityException (thrown by jtreg) when System.exit() is called
            if (!(e instanceof SecurityException)) {
                e.printStackTrace();
            }
            succeed = false;
        } finally {
            System.setOut(out);
            System.setErr(err);
            System.setIn(ins);
            sout = new String(bout.toByteArray());
            serr = new String(berr.toByteArray());
            System.out.println("STDOUT:\n" + sout + "\nSTDERR:\n" + serr);
        }
        if (!succeed) {
            throw new RuntimeException();
        }
        return new OutputAnalyzer(sout, serr);
    }

    static OutputAnalyzer importkeystore(String src, String dest,
                                         String options) {
        return kt0("-importkeystore "
                + "-srckeystore " + src + " -destkeystore " + dest
                + " -srcstorepass changeit -deststorepass changeit " + options);
    }

    static OutputAnalyzer genkeypair(String alias, String options) {
        return kt("-genkeypair -alias " + alias + " -dname CN=" + alias
                + " -storetype PKCS12 " + options);
    }

    static OutputAnalyzer certreq(String alias, String options) {
        return kt("-certreq -alias " + alias
                + " -file " + alias + ".req " + options);
    }

    static OutputAnalyzer exportcert(String alias) {
        return kt("-exportcert -alias " + alias + " -file " + alias + ".cert");
    }

    static OutputAnalyzer gencert(String relation, String options) {
        int pos = relation.indexOf("-");
        String issuer = relation.substring(0, pos);
        String subject = relation.substring(pos + 1);
        return kt(" -gencert -alias " + issuer + " -infile " + subject
                + ".req -outfile " + relation + ".cert " + options);
    }

    static void saveStore() throws IOException {
        System.out.println("---------------------------------------------");
        System.out.println("$ cp ks ks2");
        Files.copy(Paths.get("ks"), Paths.get("ks2"),
                StandardCopyOption.REPLACE_EXISTING);
    }

    static void reStore() throws IOException {
        System.out.println("---------------------------------------------");
        System.out.println("$ cp ks2 ks");
        Files.copy(Paths.get("ks2"), Paths.get("ks"),
                StandardCopyOption.REPLACE_EXISTING);
    }

    static void rm(String s) throws IOException {
        System.out.println("---------------------------------------------");
        System.out.println("$ rm " + s);
        Files.deleteIfExists(Paths.get(s));
    }
}
