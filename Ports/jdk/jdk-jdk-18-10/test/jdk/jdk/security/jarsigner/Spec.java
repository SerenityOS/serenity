/*
 * Copyright (c) 2015, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8056174 8242068 8255536
 * @summary Make sure JarSigner impl conforms to spec
 * @library /test/lib
 * @modules java.base/sun.security.tools.keytool
 *          java.base/sun.security.provider.certpath
 *          jdk.jartool
 *          jdk.crypto.ec
 * @build jdk.test.lib.util.JarUtils
 * @run main/othervm Spec
 */

import com.sun.jarsigner.ContentSigner;
import com.sun.jarsigner.ContentSignerParameters;
import jdk.security.jarsigner.JarSigner;
import jdk.test.lib.util.JarUtils;
import sun.security.provider.certpath.X509CertPath;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.*;
import java.security.cert.CertPath;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.Arrays;
import java.util.Collections;
import java.util.function.BiConsumer;

public class Spec {

    public static void main(String[] args) throws Exception {

        // Prepares raw file
        Files.write(Paths.get("a"), "a".getBytes());

        // Pack
        JarUtils.createJar("a.jar", "a");

        // Prepare a keystore
        sun.security.tools.keytool.Main.main(
                ("-keystore ks -storepass changeit -keypass changeit -dname" +
                        " CN=RSA -alias r -genkeypair -keyalg rsa").split(" "));
        sun.security.tools.keytool.Main.main(
                ("-keystore ks -storepass changeit -keypass changeit -dname" +
                        " CN=DSA -alias d -genkeypair -keyalg dsa").split(" "));
        sun.security.tools.keytool.Main.main(
                ("-keystore ks -storepass changeit -keypass changeit -dname" +
                        " CN=Ed25519 -alias e -genkeypair -keyalg Ed25519").split(" "));

        char[] pass = "changeit".toCharArray();

        KeyStore ks = KeyStore.getInstance(
                new File("ks"), pass);
        PrivateKey pkr = (PrivateKey)ks.getKey("r", pass);
        PrivateKey pkd = (PrivateKey)ks.getKey("d", pass);
        CertPath cp = CertificateFactory.getInstance("X.509")
                .generateCertPath(Arrays.asList(ks.getCertificateChain("r")));

        Provider sun = Security.getProvider("SUN");

        // throws
        npe(()->new JarSigner.Builder(null));
        npe(()->new JarSigner.Builder(null, cp));
        iae(()->new JarSigner.Builder(
                pkr, new X509CertPath(Collections.emptyList())));
        iae(()->new JarSigner.Builder(pkd, cp));    // unmatched certs alg

        JarSigner.Builder b1 = new JarSigner.Builder(pkr, cp);

        npe(()->b1.digestAlgorithm(null));
        nsae(()->b1.digestAlgorithm("HAHA"));
        b1.digestAlgorithm("SHA-256");

        npe(()->b1.digestAlgorithm("SHA-256", null));
        npe(()->b1.digestAlgorithm(null, sun));
        nsae(()->b1.digestAlgorithm("HAHA", sun));
        b1.digestAlgorithm("SHA-256", sun);

        npe(()->b1.signatureAlgorithm(null));
        nsae(()->b1.signatureAlgorithm("HAHAwithHEHE"));
        iae(()->b1.signatureAlgorithm("SHA256withECDSA"));

        npe(()->b1.signatureAlgorithm(null, sun));
        npe(()->b1.signatureAlgorithm("SHA256withRSA", null));
        nsae(()->b1.signatureAlgorithm("HAHAwithHEHE", sun));
        iae(()->b1.signatureAlgorithm("SHA256withDSA", sun));

        npe(()->b1.tsa(null));

        npe(()->b1.signerName(null));
        iae(()->b1.signerName(""));
        iae(()->b1.signerName("123456789"));
        iae(()->b1.signerName("a+b"));

        npe(()->b1.setProperty(null, ""));
        uoe(()->b1.setProperty("what", ""));
        npe(()->b1.setProperty("tsadigestalg", null));
        iae(()->b1.setProperty("tsadigestalg", "HAHA"));
        npe(()->b1.setProperty("tsapolicyid", null));
        npe(()->b1.setProperty("internalsf", null));
        iae(()->b1.setProperty("internalsf", "Hello"));
        npe(()->b1.setProperty("sectionsonly", null));
        iae(()->b1.setProperty("sectionsonly", "OK"));
        npe(()->b1.setProperty("sectionsonly", null));
        npe(()->b1.setProperty("altsigner", null));
        npe(()->b1.eventHandler(null));

        // default values
        JarSigner.Builder b2 = new JarSigner.Builder(pkr, cp);
        JarSigner js2 = b2.build();

        assertTrue(js2.getDigestAlgorithm().equals(
                JarSigner.Builder.getDefaultDigestAlgorithm()));
        assertTrue(js2.getSignatureAlgorithm().equals(
                JarSigner.Builder.getDefaultSignatureAlgorithm(pkr)));
        assertTrue(js2.getTsa() == null);
        assertTrue(js2.getSignerName().equals("SIGNER"));
        assertTrue(js2.getProperty("tsadigestalg").equals(
                JarSigner.Builder.getDefaultDigestAlgorithm()));
        assertTrue(js2.getProperty("tsapolicyid") == null);
        assertTrue(js2.getProperty("internalsf").equals("false"));
        assertTrue(js2.getProperty("sectionsonly").equals("false"));
        assertTrue(js2.getProperty("altsigner") == null);
        uoe(()->js2.getProperty("invalid"));

        // default values
        BiConsumer<String,String> myeh = (a,s)->{};
        URI tsa = new URI("https://tsa.com");

        JarSigner.Builder b3 = new JarSigner.Builder(pkr, cp)
                .digestAlgorithm("SHA-1")
                .signatureAlgorithm("SHA1withRSA")
                .signerName("Duke")
                .tsa(tsa)
                .setProperty("tsadigestalg", "SHA-512")
                .setProperty("tsapolicyid", "1.2.3.4")
                .setProperty("internalsf", "true")
                .setProperty("sectionsonly", "true")
                .setProperty("altsigner", "MyContentSigner")
                .eventHandler(myeh);
        JarSigner js3 = b3.build();

        assertTrue(js3.getDigestAlgorithm().equals("SHA-1"));
        assertTrue(js3.getSignatureAlgorithm().equals("SHA1withRSA"));
        assertTrue(js3.getTsa().equals(tsa));
        assertTrue(js3.getSignerName().equals("DUKE"));
        assertTrue(js3.getProperty("tsadigestalg").equals("SHA-512"));
        assertTrue(js3.getProperty("tsapolicyid").equals("1.2.3.4"));
        assertTrue(js3.getProperty("internalsf").equals("true"));
        assertTrue(js3.getProperty("sectionsonly").equals("true"));
        assertTrue(js3.getProperty("altsigner").equals("MyContentSigner"));
        assertTrue(js3.getProperty("altsignerpath") == null);

        assertTrue(JarSigner.Builder.getDefaultDigestAlgorithm().equals("SHA-256"));

        // Calculating large DSA and RSA keys are too slow.
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(1024);
        assertTrue(JarSigner.Builder
                .getDefaultSignatureAlgorithm(kpg.generateKeyPair().getPrivate())
                    .equals("SHA256withRSA"));

        kpg = KeyPairGenerator.getInstance("DSA");
        kpg.initialize(1024);
        assertTrue(JarSigner.Builder
                .getDefaultSignatureAlgorithm(kpg.generateKeyPair().getPrivate())
                .equals("SHA256withDSA"));

        kpg = KeyPairGenerator.getInstance("EC");
        kpg.initialize(256);
        assertTrue(JarSigner.Builder
                .getDefaultSignatureAlgorithm(kpg.generateKeyPair().getPrivate())
                .equals("SHA256withECDSA"));
        kpg.initialize(384);
        assertTrue(JarSigner.Builder
                .getDefaultSignatureAlgorithm(kpg.generateKeyPair().getPrivate())
                .equals("SHA384withECDSA"));
        kpg.initialize(521);
        assertTrue(JarSigner.Builder
                .getDefaultSignatureAlgorithm(kpg.generateKeyPair().getPrivate())
                .equals("SHA512withECDSA"));

        // altsigner does not support modern algorithms
        JarSigner.Builder b4 = new JarSigner.Builder(
                (PrivateKey)ks.getKey("e", pass),
                CertificateFactory.getInstance("X.509")
                        .generateCertPath(Arrays.asList(ks.getCertificateChain("e"))));
        b4.setProperty("altsigner", "MyContentSigner");
        iae(() -> b4.build());
    }

    interface RunnableWithException {
        void run() throws Exception;
    }

    static void uoe(RunnableWithException r) throws Exception {
        checkException(r, UnsupportedOperationException.class);
    }

    static void nsae(RunnableWithException r) throws Exception {
        checkException(r, NoSuchAlgorithmException.class);
    }

    static void npe(RunnableWithException r) throws Exception {
        checkException(r, NullPointerException.class);
    }

    static void iae(RunnableWithException r) throws Exception {
        checkException(r, IllegalArgumentException.class);
    }

    static void checkException(RunnableWithException r, Class ex)
            throws Exception {
        try {
            r.run();
        } catch (Exception e) {
            if (ex.isAssignableFrom(e.getClass())) {
                return;
            }
            throw e;
        }
        throw new Exception("No exception thrown");
    }

    static void assertTrue(boolean x) throws Exception {
        if (!x) throw new Exception("Not true");
    }

    static class MyContentSigner extends ContentSigner {
        @Override
        public byte[] generateSignedData(
                ContentSignerParameters parameters,
                boolean omitContent,
                boolean applyTimestamp) throws NoSuchAlgorithmException,
                CertificateException, IOException {
            return new byte[0];
        }
    }
}
