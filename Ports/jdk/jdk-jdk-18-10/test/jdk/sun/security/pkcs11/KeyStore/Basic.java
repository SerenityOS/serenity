/*
 * Copyright (c) 2003, 2021, Oracle and/or its affiliates. All rights reserved.
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

/* @test
 * @bug 4938185
 * @summary KeyStore support for NSS cert/key databases
 * To run manually:
 *    set environment variable:
 *     <token>     [activcard|ibutton|nss|sca1000]
 *     <command>   [list|basic]
 *
 * Note:
 *    . 'list' lists the token aliases
 *    . 'basic' does not run with activcard,
 * @library /test/lib ..
 * @run testng/othervm Basic
 */

import java.io.*;
import java.nio.file.Path;
import java.util.*;

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.Signature;
import java.security.Security;

import java.security.cert.*;
import java.security.spec.*;
import java.security.interfaces.*;

import javax.crypto.SecretKey;

import javax.security.auth.Subject;

import com.sun.security.auth.module.*;
import com.sun.security.auth.callback.*;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;


public class Basic extends PKCS11Test {

    private static final Path TEST_DATA_PATH = Path.of(BASE)
            .resolve("BasicData");
    private static final String DIR = TEST_DATA_PATH.toString();
    private static char[] tokenPwd;
    private static final char[] ibuttonPwd =
                        new char[0];
    private static final char[] activcardPwd =
                        new char[] { '1', '1', '2', '2', '3', '3' };
    private static final char[] nssPwd =
                        new char[] { 't', 'e', 's', 't', '1', '2' };
    private static final char[] solarisPwd =
                        new char[] { 'p', 'i', 'n' };
    private static final char[] sca1000Pwd =
                        new char[] { 'p', 'a', 's', 's', 'w', 'o', 'r', 'd' };
    private static final char[] sPwd = { 'f', 'o', 'o' };

    private static SecretKey sk1;
    private static SecretKey sk2;
    private static SecretKey sk3;
    private static SecretKey sk4;

    private static RSAPrivateCrtKey pk1;
    private static PrivateKey pk2;
    private static PrivateKey pk3;

    private static Certificate[] chain1;
    private static Certificate[] chain2;
    private static Certificate[] chain3;
    private static Certificate[] chain4;

    private static X509Certificate randomCert;

    private static KeyStore ks;
    private static final String KS_TYPE = "PKCS11";
    private static Provider provider;

    @BeforeClass
    public void setUp() throws Exception {
        copyNssCertKeyToClassesDir();
        setCommonSystemProps();
        System.setProperty("CUSTOM_P11_CONFIG",
                TEST_DATA_PATH.resolve("p11-nss.txt").toString());
        System.setProperty("TOKEN", "nss");
        System.setProperty("TEST", "basic");
    }

    @Test
    public void testBasic() throws Exception {
        String[] args = {"sm", "Basic.policy"};
        main(new Basic(), args);
    }

    private static class FooEntry implements KeyStore.Entry { }

    private static class P11SecretKey implements SecretKey {
        String alg;
        int length;
        public P11SecretKey(String alg, int length) {
            this.alg = alg;
            this.length = length;
        }
        public String getAlgorithm() { return alg; }
        public String getFormat() { return "raw"; }
        public byte[] getEncoded() { return new byte[length/8]; }
    }

    public void main(Provider p) throws Exception {

        this.provider = p;

        // get private keys
        KeyFactory kf = KeyFactory.getInstance("RSA");
        KeyFactory dsaKf = KeyFactory.getInstance("DSA", "SUN");

        ObjectInputStream ois1 = new ObjectInputStream
                        (new FileInputStream(new File(DIR, "pk1.key")));
        byte[] keyBytes = (byte[])ois1.readObject();
        ois1.close();
        PrivateKey tmpKey =
                kf.generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
        pk1 = (RSAPrivateCrtKey)tmpKey;

        ObjectInputStream ois2 = new ObjectInputStream
                        (new FileInputStream(new File(DIR, "pk2.key")));
        keyBytes = (byte[])ois2.readObject();
        ois2.close();
        pk2 = kf.generatePrivate(new PKCS8EncodedKeySpec(keyBytes));

        ObjectInputStream ois3 = new ObjectInputStream
                        (new FileInputStream(new File(DIR, "pk3.key")));
        keyBytes = (byte[])ois3.readObject();
        pk3 = kf.generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
        ois3.close();

        // get cert chains for private keys
        CertificateFactory cf = CertificateFactory.getInstance("X.509", "SUN");
        Certificate caCert = cf.generateCertificate
                        (new FileInputStream(new File(DIR, "ca.cert")));
        Certificate ca2Cert = cf.generateCertificate
                        (new FileInputStream(new File(DIR, "ca2.cert")));
        Certificate pk1cert = cf.generateCertificate
                        (new FileInputStream(new File(DIR, "pk1.cert")));
        Certificate pk1cert2 = cf.generateCertificate
                        (new FileInputStream(new File(DIR, "pk1.cert2")));
        Certificate pk2cert = cf.generateCertificate
                        (new FileInputStream(new File(DIR, "pk2.cert")));
        Certificate pk3cert = cf.generateCertificate
                        (new FileInputStream(new File(DIR, "pk3.cert")));
        chain1 = new Certificate[] { pk1cert, caCert };
        chain2 = new Certificate[] { pk2cert, caCert };
        chain3 = new Certificate[] { pk3cert, caCert };
        chain4 = new Certificate[] { pk1cert2, ca2Cert };

        // create secret keys
        sk1 = new P11SecretKey("DES", 64);
        sk2 = new P11SecretKey("DESede", 192);
        sk3 = new P11SecretKey("AES", 128);
        sk4 = new P11SecretKey("RC4", 128);

        // read randomCert
        randomCert = (X509Certificate)cf.generateCertificate
                        (new FileInputStream(new File(DIR, "random.cert")));

        doTest();
    }

    private static void doTest() throws Exception {

        String token = System.getProperty("TOKEN");
        String test = System.getProperty("TEST");

        if (token == null || token.length() == 0) {
            throw new Exception("token arg required");
        }
        if (test == null || test.length() == 0) {
            throw new Exception("test arg required");
        }

        if ("ibutton".equals(token)) {
            tokenPwd = ibuttonPwd;
        } else if ("activcard".equals(token)) {
            tokenPwd = activcardPwd;
        } else if ("nss".equals(token)) {
            tokenPwd = nssPwd;
        } else if ("sca1000".equals(token)) {
            tokenPwd = sca1000Pwd;
        } else if ("solaris".equals(token)) {
            tokenPwd = solarisPwd;
        }

        if ("list".equals(test)) {
            Basic.list();
        } else if ("basic".equals(test)) {

            int testnum = 1;

            if ("ibutton".equals(token)) {
                // pkey and setAttribute
                testnum = Basic.pkey(testnum);
                testnum = Basic.setAttribute(testnum);
            } else if ("activcard".equals(token)) {
                // sign
                testnum = Basic.signAlias(testnum, null);
            } else if ("nss".equals(token)) {
                // setAttribute, pkey, sign
                testnum = Basic.setAttribute(testnum);
                testnum = Basic.pkey(testnum);
                testnum = Basic.sign(testnum);
                testnum = Basic.copy(testnum);
            } else if ("solaris".equals(token)) {
                testnum = Basic.setAttribute(testnum);
                testnum = Basic.pkey(testnum);
                testnum = Basic.sign(testnum);
                testnum = Basic.skey(testnum);
                testnum = Basic.copy(testnum);
            } else if ("sca1000".equals(token)) {
                // setAttribute, pkey, sign, skey, copy
                testnum = Basic.setAttribute(testnum);
                testnum = Basic.pkey(testnum);
                testnum = Basic.sign(testnum);
                testnum = Basic.skey(testnum);
                testnum = Basic.copy(testnum);
            }

        } else if ("pkey".equals(test)) {
            Basic.pkey(1);
        } else if ("skey".equals(test)) {
            Basic.skey(1);
        } else if ("setAttribute".equals(test)) {
            Basic.setAttribute(1);
        } else if ("copy".equals(test)) {
            Basic.copy(1);
        } else if ("sign".equals(test)) {
            Basic.sign(1);
        } else if ("module".equals(test)) {
            Basic.module();
        } else if ("nss-extended".equals(test)) {

            // this only works if NSS_TEST is set to true in P11KeyStore.java

            int testnum = 1;
            testnum = Basic.setAttribute(testnum);
            testnum = Basic.pkey(testnum);
            testnum = Basic.sign(testnum);
            testnum = Basic.extended(testnum);
        } else {
            System.out.println("unrecognized command");
        }
    }

    private static int sign(int testnum) throws Exception {
        if (ks == null) {
            ks = KeyStore.getInstance(KS_TYPE, provider);
            ks.load(null, tokenPwd);
        }
        if (!ks.containsAlias("pk1")) {
            ks.setKeyEntry("pk1", pk1, null, chain1);
        }
        System.out.println("test " + testnum++ + " passed");

        return signAlias(testnum, "pk1");
    }

    private static int signAlias(int testnum, String alias) throws Exception {

        if (ks == null) {
            ks = KeyStore.getInstance(KS_TYPE, provider);
            ks.load(null, tokenPwd);
        }

        if (alias == null) {
            Enumeration enu = ks.aliases();
            if (enu.hasMoreElements()) {
                alias = (String)enu.nextElement();
            }
        }

        PrivateKey pkey = (PrivateKey)ks.getKey(alias, null);
        if ("RSA".equals(pkey.getAlgorithm())) {
            System.out.println("got [" + alias + "] signing key: " + pkey);
        } else {
            throw new SecurityException
                ("expected RSA, got " + pkey.getAlgorithm());
        }

        Signature s = Signature.getInstance("MD5WithRSA", ks.getProvider());
        s.initSign(pkey);
        System.out.println("initialized signature object with key");
        s.update("hello".getBytes());
        System.out.println("signature object updated with [hello] bytes");

        byte[] signed = s.sign();
        System.out.println("received signature " + signed.length +
                        " bytes in length");

        Signature v = Signature.getInstance("MD5WithRSA", ks.getProvider());
        v.initVerify(ks.getCertificate(alias));
        v.update("hello".getBytes());
        v.verify(signed);
        System.out.println("signature verified");
        System.out.println("test " + testnum++ + " passed");

        return testnum;
    }

    private static int copy(int testnum) throws Exception {

        if (ks == null) {
            ks = KeyStore.getInstance(KS_TYPE, provider);
            ks.load(null, tokenPwd);
        }

        KeyFactory kf = KeyFactory.getInstance("RSA", provider);
        PrivateKey pkSession = (PrivateKey)kf.translateKey(pk3);
        System.out.println("pkSession = " + pkSession);
        ks.setKeyEntry("pkSession", pkSession, null, chain3);

        KeyStore.PrivateKeyEntry pke =
                (KeyStore.PrivateKeyEntry)ks.getEntry("pkSession", null);
        System.out.println("pkSession = " + pke.getPrivateKey());
        Certificate[] chain = pke.getCertificateChain();
        if (chain.length != chain3.length) {
            throw new SecurityException("received chain not correct length");
        }
        for (int i = 0; i < chain.length; i++) {
            if (!chain[i].equals(chain3[i])) {
                throw new SecurityException("received chain not equal");
            }
        }

        System.out.println("test " + testnum++ + " passed");

        return testnum;
    }

    private static void list() throws Exception {
        int testnum = 1;

        ks = KeyStore.getInstance(KS_TYPE, provider);

        // check instance
        if (ks.getProvider() instanceof java.security.AuthProvider) {
            System.out.println("keystore provider instance of AuthProvider");
            System.out.println("test " + testnum++ + " passed");
        } else {
            throw new SecurityException("did not get AuthProvider KeyStore");
        }

        // load
        ks.load(null, tokenPwd);
        System.out.println("test " + testnum++ + " passed");

        // aliases
        Enumeration enu = ks.aliases();
        int count = 0;
        while (enu.hasMoreElements()) {
            count++;
            System.out.println("alias " +
                                count +
                                " = " +
                                (String)enu.nextElement());
        }
    }

    private static void module() throws Exception {

        // perform Security.addProvider of P11 provider
        Security.addProvider(getSunPKCS11(System.getProperty("CUSTOM_P11_CONFIG")));

        String KS_PROVIDER = "SunPKCS11-" + System.getProperty("TOKEN");

        KeyStoreLoginModule m = new KeyStoreLoginModule();
        Subject s = new Subject();
        Map<String, String> options = new HashMap<>();
        options.put("keyStoreURL", "NONE");
        options.put("keyStoreType", KS_TYPE);
        options.put("keyStoreProvider", KS_PROVIDER);
        options.put("debug", "true");
        m.initialize(s, new TextCallbackHandler(), new HashMap<>(), options);
        m.login();
        m.commit();
        System.out.println("authenticated subject = " + s);
        m.logout();
        System.out.println("authenticated subject = " + s);
    }

    /**
     * SCA1000 does not handle extended secret key tests
     * . Blowfish (CKR_TEMPLATE_INCOMPLETE)
     * . AES (CKR_TEMPLATE_INCOMPLETE)
     * . RC4 (CKR_ATTRIBUTE_TYPE_INVALID)
     * so do this instead
     */
    private static int skey(int testnum) throws Exception {
        if (ks == null) {
            ks = KeyStore.getInstance(KS_TYPE, provider);
            ks.load(null, tokenPwd);
        }

        // delete all old aliases
        Enumeration enu = ks.aliases();
        int count = 0;
        while (enu.hasMoreElements()) {
            String next = (String)enu.nextElement();
            ks.deleteEntry(next);
            System.out.println("deleted entry for: " + next);
        }

        // set good ske 1
        ks.setKeyEntry("sk1", sk1, null, null);
        System.out.println("test " + testnum++ + " passed");

        // set good ske 2
        ks.setKeyEntry("sk2", sk2, null, null);
        System.out.println("test " + testnum++ + " passed");

        // getEntry good ske 1
        KeyStore.SecretKeyEntry ske =
                (KeyStore.SecretKeyEntry)ks.getEntry("sk1", null);
        if ("DES".equals(ske.getSecretKey().getAlgorithm())) {
            System.out.println("test " + testnum++ + " passed");
        } else {
            throw new SecurityException
                ("expected DES, got " + ske.getSecretKey().getAlgorithm());
        }

        // getEntry good ske 2
        ske = (KeyStore.SecretKeyEntry)ks.getEntry("sk2", null);
        if ("DESede".equals(ske.getSecretKey().getAlgorithm())) {
            System.out.println("test " + testnum++ + " passed");
        } else {
            throw new SecurityException
                ("expected DESede, got " + ske.getSecretKey().getAlgorithm());
        }

        // getKey good ske 1
        SecretKey skey = (SecretKey)ks.getKey("sk1", null);
        if ("DES".equals(skey.getAlgorithm())) {
            System.out.println("test " + testnum++ + " passed");
        } else {
            throw new SecurityException
                ("expected DES, got " + skey.getAlgorithm());
        }

        // getKey good ske 2
        skey = (SecretKey)ks.getKey("sk2", null);
        if ("DESede".equals(skey.getAlgorithm())) {
            System.out.println("test " + testnum++ + " passed");
        } else {
            throw new SecurityException
                ("expected DESede, got " + skey.getAlgorithm());
        }

        // aliases
        enu = ks.aliases();
        count = 0;
        while (enu.hasMoreElements()) {
            count++;
            System.out.println("alias " +
                                count +
                                " = " +
                                (String)enu.nextElement());
        }
        if (count == 2) {
            System.out.println("test " + testnum++ + " passed");
        } else {
            throw new SecurityException("expected 2 aliases");
        }

        // size
        if (ks.size() == 2) {
            System.out.println("test " + testnum++ + " passed");
        } else {
            throw new SecurityException("expected size 2");
        }

        // isCertificateEntry sk1
        if (!ks.isCertificateEntry("sk1")) {
            System.out.println("test " + testnum++ + " passed");
        } else {
            throw new SecurityException("expected ske");
        }

        // isKeyEntry sk1
        if (ks.isKeyEntry("sk1")) {
            System.out.println("test " + testnum++ + " passed");
        } else {
            throw new SecurityException("expected ske");
        }

        // entryInstanceOf sk2
        if (ks.entryInstanceOf("sk2", KeyStore.SecretKeyEntry.class)) {
            System.out.println("test " + testnum++ + " passed");
        } else {
            throw new SecurityException("expected ske");
        }

        return testnum;
    }

    private static int setAttribute(int testnum) throws Exception {

        if (ks == null) {
            ks = KeyStore.getInstance(KS_TYPE, provider);
            ks.load(null, tokenPwd);
        }

        if (!ks.containsAlias("pk1")) {
            // set good pke 1
            ks.setKeyEntry("pk1", pk1, null, chain1);
            System.out.println("test " + testnum++ + " passed");
        }

        // delete all old aliases except pk1
        Enumeration enu = ks.aliases();
        int count = 0;
        while (enu.hasMoreElements()) {
            String next = (String)enu.nextElement();
            if (!"pk1".equals(next)) {
                ks.deleteEntry(next);
                System.out.println("deleted entry for: " + next);
            }
        }

        KeyStore.PrivateKeyEntry pke =
                (KeyStore.PrivateKeyEntry)ks.getEntry("pk1", null);
        System.out.println("pk1 = " + pke.getPrivateKey());
        Certificate[] chain = pke.getCertificateChain();
        if (chain.length != chain1.length) {
            throw new SecurityException("received chain not correct length");
        }
        for (int i = 0; i < chain.length; i++) {
            if (!chain[i].equals(chain1[i])) {
                throw new SecurityException("received chain not equal");
            }
        }
        System.out.println("test " + testnum++ + " passed");

        /**
         * test change alias only
         */

        // test C_SetAttribute
        PrivateKey pkey = pke.getPrivateKey();
        ks.setEntry("pk1SA",
                new KeyStore.PrivateKeyEntry(pkey, chain1),
                null);
        System.out.println("test " + testnum++ + " passed");

        // aliases
        enu = ks.aliases();
        count = 0;
        String newAlias = null;
        while (enu.hasMoreElements()) {
            count++;
            newAlias = (String)enu.nextElement();
            System.out.println("alias " +
                                count +
                                " = " +
                                newAlias);
        }
        if (count == 1 && "pk1SA".equals(newAlias)) {
            System.out.println("test " + testnum++ + " passed");
        } else {
            throw new SecurityException("expected 1 alias");
        }

        // size
        if (ks.size() == 1) {
            System.out.println("test " + testnum++ + " passed");
        } else {
            throw new SecurityException("expected size 1");
        }

        pke = (KeyStore.PrivateKeyEntry)ks.getEntry("pk1", null);
        if (pke != null) {
            throw new SecurityException("expected not to find pk1");
        }
        System.out.println("test " + testnum++ + " passed");

        pke = (KeyStore.PrivateKeyEntry)ks.getEntry("pk1SA", null);
        System.out.println("pk1SA = " + pke.getPrivateKey());
        chain = pke.getCertificateChain();
        if (chain.length != chain1.length) {
            throw new SecurityException("received chain not correct length");
        }
        for (int i = 0; i < chain.length; i++) {
            if (!chain[i].equals(chain1[i])) {
                throw new SecurityException("received chain not equal");
            }
        }
        System.out.println("test " + testnum++ + " passed");

        /**
         * test change cert chain
         */

        pkey = pke.getPrivateKey();
        ks.setEntry("pk1SA-2",
                new KeyStore.PrivateKeyEntry(pkey, chain4),
                null);
        System.out.println("test " + testnum++ + " passed");

        // aliases
        enu = ks.aliases();
        count = 0;
        newAlias = null;
        while (enu.hasMoreElements()) {
            count++;
            newAlias = (String)enu.nextElement();
            System.out.println("alias " +
                                count +
                                " = " +
                                newAlias);
        }
        if (count == 1 && "pk1SA-2".equals(newAlias)) {
            System.out.println("test " + testnum++ + " passed");
        } else {
            throw new SecurityException("expected 1 alias");
        }

        // size
        if (ks.size() == 1) {
            System.out.println("test " + testnum++ + " passed");
        } else {
            throw new SecurityException("expected size 1");
        }

        pke = (KeyStore.PrivateKeyEntry)ks.getEntry("pk1SA", null);
        if (pke != null) {
            throw new SecurityException("expected not to find pk1SA");
        }
        System.out.println("test " + testnum++ + " passed");

        pke = (KeyStore.PrivateKeyEntry)ks.getEntry("pk1SA-2", null);
        System.out.println("pk1SA-2 = " + pke.getPrivateKey());
        chain = pke.getCertificateChain();
        if (chain.length != chain4.length) {
            throw new SecurityException("received chain not correct length");
        }
        for (int i = 0; i < chain.length; i++) {
            if (!chain[i].equals(chain4[i])) {
                throw new SecurityException("received chain not equal");
            }
        }
        System.out.println("test " + testnum++ + " passed");

        return testnum;
    }

    private static int pkey(int testnum) throws Exception {

        if (ks == null) {
            ks = KeyStore.getInstance(KS_TYPE, provider);
            ks.load(null, tokenPwd);
            System.out.println("test " + testnum++ + " passed");
        }

        // check instance
        if (ks.getProvider() instanceof java.security.AuthProvider) {
            System.out.println("keystore provider instance of AuthProvider");
            System.out.println("test " + testnum++ + " passed");
        } else {
            throw new SecurityException("did not get AuthProvider KeyStore");
        }

        // delete all old aliases
        Enumeration enu = ks.aliases();
        int count = 0;
        while (enu.hasMoreElements()) {
            String next = (String)enu.nextElement();
            ks.deleteEntry(next);
            System.out.println("deleted entry for: " + next);
        }

        // set good pke 1
        ks.setKeyEntry("pk1", pk1, null, chain1);
        System.out.println("test " + testnum++ + " passed");

        // set good pke 2
        ks.setEntry("pk2",
                new KeyStore.PrivateKeyEntry(pk2, chain2),
                null);
        System.out.println("test " + testnum++ + " passed");

        // getEntry good pke 1
        KeyStore.PrivateKeyEntry pke =
                (KeyStore.PrivateKeyEntry)ks.getEntry("pk1", null);
        System.out.println("pk1 = " + pke.getPrivateKey());
        Certificate[] chain = pke.getCertificateChain();
        if (chain.length != chain1.length) {
            throw new SecurityException("received chain not correct length");
        }
        for (int i = 0; i < chain.length; i++) {
            if (!chain[i].equals(chain1[i])) {
                throw new SecurityException("received chain not equal");
            }
        }

        // getKey good pke 1
        PrivateKey pkey = (PrivateKey)ks.getKey("pk1", null);
        System.out.println("pk1 = " + pkey);
        if ("RSA".equals(pkey.getAlgorithm())) {
            System.out.println("test " + testnum++ + " passed");
        } else {
            throw new SecurityException
                ("expected RSA, got " + pkey.getAlgorithm());
        }

        // getCertificate chain chain 1
        chain = ks.getCertificateChain("pk1");
        if (chain.length != chain1.length) {
            throw new SecurityException("received chain not correct length");
        }
        for (int i = 0; i < chain.length; i++) {
            if (!chain[i].equals(chain1[i])) {
                throw new SecurityException("received chain not equal");
            }
        }
        System.out.println("test " + testnum++ + " passed");

        // getEntry good pke 2
        pke = (KeyStore.PrivateKeyEntry)ks.getEntry("pk2", null);
        if ("RSA".equals(pke.getPrivateKey().getAlgorithm())) {
            System.out.println("test " + testnum++ + " passed");
        } else {
            throw new SecurityException
                ("expected RSA, got " + pke.getPrivateKey().getAlgorithm());
        }
        System.out.println("pk2 = " + pke.getPrivateKey());
        chain = pke.getCertificateChain();
        if (chain.length != chain2.length) {
            throw new SecurityException("received chain not correct length");
        }
        for (int i = 0; i < chain.length; i++) {
            if (!chain[i].equals(chain2[i])) {
                throw new SecurityException("received chain not equal");
            }
        }

        // getKey good pke 2
        pkey = (PrivateKey)ks.getKey("pk2", null);
        if ("RSA".equals(pkey.getAlgorithm())) {
            System.out.println("test " + testnum++ + " passed");
        } else {
            throw new SecurityException
                ("expected RSA, got " + pkey.getAlgorithm());
        }

        // getCertificate chain chain 2
        chain = ks.getCertificateChain("pk2");
        if (chain.length != chain2.length) {
            throw new SecurityException("received chain not correct length");
        }
        for (int i = 0; i < chain.length; i++) {
            if (!chain[i].equals(chain2[i])) {
                throw new SecurityException("received chain not equal");
            }
        }
        System.out.println("test " + testnum++ + " passed");

        // aliases
        enu = ks.aliases();
        count = 0;
        while (enu.hasMoreElements()) {
            count++;
            System.out.println("alias " +
                                count +
                                " = " +
                                (String)enu.nextElement());
        }
        if (count == 2) {
            System.out.println("test " + testnum++ + " passed");
        } else {
            throw new SecurityException("expected 2 aliases");
        }

        // size
        if (ks.size() == 2) {
            System.out.println("test " + testnum++ + " passed");
        } else {
            throw new SecurityException("expected size 2");
        }

        // getCertificate
        if (ks.getCertificate("pk1").equals(chain1[0])) {
            System.out.println("test " + testnum++ + " passed");
        } else {
            throw new SecurityException("expected certificate pk1 end entity");
        }

        // containsAlias
        if (ks.containsAlias("pk1") && ks.containsAlias("pk2") &&
            !ks.containsAlias("foobar") &&
            !ks.containsAlias("pk1.2") && !ks.containsAlias("pk2.2")) {
            System.out.println("test " + testnum++ + " passed");
        } else {
            throw new SecurityException("unexpected aliases encountered");
        }

        // isKeyEntry
        if (ks.isKeyEntry("pk1") && ks.isKeyEntry("pk2") &&
            !ks.isKeyEntry("foobar")) {
            System.out.println("test " + testnum++ + " passed");
        } else {
            throw new SecurityException("isKeyEntry failed");
        }

        // isCertificateEntry
        if (!ks.isCertificateEntry("foobar") &&
            !ks.isCertificateEntry("pk1") && !ks.isCertificateEntry("pk2")) {
            System.out.println("test " + testnum++ + " passed");
        } else {
            throw new SecurityException("isCertificateEntry failed");
        }

        // getCertificateAlias
        if (ks.getCertificateAlias(chain1[0]).equals("pk1") &&
            ks.getCertificateAlias(chain2[0]).equals("pk2") &&
            ks.getCertificateAlias(randomCert) == null) {
            System.out.println("test " + testnum++ + " passed");
        } else {
            throw new SecurityException("getCertificateAlias failed");
        }

        if (ks.entryInstanceOf("pk1", KeyStore.PrivateKeyEntry.class) &&
            ks.entryInstanceOf("pk2", KeyStore.PrivateKeyEntry.class) &&
        !ks.entryInstanceOf("pk1", KeyStore.TrustedCertificateEntry.class) &&
        !ks.entryInstanceOf("pk2", KeyStore.TrustedCertificateEntry.class) &&
        !ks.entryInstanceOf("foobar", KeyStore.TrustedCertificateEntry.class) &&
          !ks.entryInstanceOf("foobar", KeyStore.PrivateKeyEntry.class)) {
            System.out.println("test " + testnum++ + " passed");
        } else {
            throw new SecurityException("entryInstanceOf failed");
        }

        ks.deleteEntry("pk2");
        if (ks.containsAlias("pk1") && !ks.containsAlias("pk2")) {
            System.out.println("test " + testnum++ + " passed");
        } else {
            throw new SecurityException("deleteEntry failed");
        }

        // getEntry good pke 1
        pke = (KeyStore.PrivateKeyEntry)ks.getEntry("pk1", null);
        System.out.println("pk1 = " + pke.getPrivateKey());
        chain = pke.getCertificateChain();
        if (chain.length != chain1.length) {
            throw new SecurityException("received chain not correct length");
        }
        for (int i = 0; i < chain.length; i++) {
            if (!chain[i].equals(chain1[i])) {
                throw new SecurityException("received chain not equal");
            }
        }
        System.out.println("test " + testnum++ + " passed");

        // aliases
        enu = ks.aliases();
        count = 0;
        while (enu.hasMoreElements()) {
            count++;
            System.out.println("alias " +
                                count +
                                " = " +
                                (String)enu.nextElement());
        }
        if (count == 1) {
            System.out.println("test " + testnum++ + " passed");
        } else {
            throw new SecurityException("expected 1 alias");
        }

        // size
        if (ks.size() == 1) {
            System.out.println("test " + testnum++ + " passed");
        } else {
            throw new SecurityException("expected size 1");
        }

        return testnum;
    }

    private static int extended(int testnum) throws Exception {

        // setEntry unknown entry type
        try {
            ks.setEntry("foo", new FooEntry(), null);
            throw new SecurityException("setEntry should have failed");
        } catch (KeyStoreException kse) {
            System.out.println("test " + testnum++ + " passed");
        }

        // getEntry random foo
        if (ks.getEntry("foo", null) != null) {
            throw new SecurityException("expected null entry");
        } else {
            System.out.println("test " + testnum++ + " passed");
        }

        // set good ske 1
        ks.setKeyEntry("sk1", sk1, null, null);
        System.out.println("test " + testnum++ + " passed");

        // set good ske 2
        ks.setKeyEntry("sk2", sk2, null, null);
        System.out.println("test " + testnum++ + " passed");

        // set good ske 3
        ks.setEntry("sk3",
                new KeyStore.SecretKeyEntry(sk3),
                null);
        System.out.println("test " + testnum++ + " passed");

        // set good ske 4
        ks.setEntry("sk4",
                new KeyStore.SecretKeyEntry(sk4),
                null);
        System.out.println("test " + testnum++ + " passed");

        // getEntry good ske 1
        KeyStore.SecretKeyEntry ske =
                (KeyStore.SecretKeyEntry)ks.getEntry("sk1", null);
        if ("DES".equals(ske.getSecretKey().getAlgorithm())) {
            System.out.println("test " + testnum++ + " passed");
        } else {
            throw new SecurityException
                ("expected DES, got " + ske.getSecretKey().getAlgorithm());
        }

        // getEntry good ske 2
        ske = (KeyStore.SecretKeyEntry)ks.getEntry("sk2", null);
        if ("DESede".equals(ske.getSecretKey().getAlgorithm())) {
            System.out.println("test " + testnum++ + " passed");
        } else {
            throw new SecurityException
                ("expected DESede, got " + ske.getSecretKey().getAlgorithm());
        }

        // getEntry good ske 3
        ske = (KeyStore.SecretKeyEntry)ks.getEntry("sk3", null);
        if ("AES".equals(ske.getSecretKey().getAlgorithm())) {
            System.out.println("test " + testnum++ + " passed");
        } else {
            throw new SecurityException
                ("expected AES, got " + ske.getSecretKey().getAlgorithm());
        }

        // getEntry good ske 4
        ske = (KeyStore.SecretKeyEntry)ks.getEntry("sk4", null);
        if ("ARCFOUR".equals(ske.getSecretKey().getAlgorithm())) {
            System.out.println("test " + testnum++ + " passed");
        } else {
            throw new SecurityException
                ("expected ARCFOUR, got " + ske.getSecretKey().getAlgorithm());
        }

        // getKey good ske 1
        SecretKey skey = (SecretKey)ks.getKey("sk1", null);
        if ("DES".equals(skey.getAlgorithm())) {
            System.out.println("test " + testnum++ + " passed");
        } else {
            throw new SecurityException
                ("expected DES, got " + skey.getAlgorithm());
        }

        // getKey good ske 2
        skey = (SecretKey)ks.getKey("sk2", null);
        if ("DESede".equals(skey.getAlgorithm())) {
            System.out.println("test " + testnum++ + " passed");
        } else {
            throw new SecurityException
                ("expected DESede, got " + skey.getAlgorithm());
        }

        // getKey good ske 3
        skey = (SecretKey)ks.getKey("sk3", null);
        if ("AES".equals(skey.getAlgorithm())) {
            System.out.println("test " + testnum++ + " passed");
        } else {
            throw new SecurityException
                ("expected AES, got " + skey.getAlgorithm());
        }

        // getKey good ske 4
        skey = (SecretKey)ks.getKey("sk4", null);
        if ("ARCFOUR".equals(skey.getAlgorithm())) {
            System.out.println("test " + testnum++ + " passed");
        } else {
            throw new SecurityException
                ("expected ARCFOUR, got " + skey.getAlgorithm());
        }

        // aliases
        Enumeration enu = ks.aliases();
        int count = 0;
        while (enu.hasMoreElements()) {
            count++;
            System.out.println("alias " +
                                count +
                                " = " +
                                (String)enu.nextElement());
        }
        if (count == 5) {
            System.out.println("test " + testnum++ + " passed");
        } else {
            throw new SecurityException("expected 5 aliases");
        }

        // size
        if (ks.size() == 5) {
            System.out.println("test " + testnum++ + " passed");
        } else {
            throw new SecurityException("expected size 5");
        }

        // set good pke 2
        ks.setEntry("pk2",
                new KeyStore.PrivateKeyEntry(pk2, chain2),
                null);
        System.out.println("test " + testnum++ + " passed");

        // set good pke 3
        ks.setEntry("pk3",
                new KeyStore.PrivateKeyEntry(pk3, chain3),
                null);
        System.out.println("test " + testnum++ + " passed");

        // getEntry good pke 1
        KeyStore.PrivateKeyEntry pke =
                (KeyStore.PrivateKeyEntry)ks.getEntry("pk1", null);
        System.out.println("pk1 = " + pke.getPrivateKey());
        Certificate[] chain = pke.getCertificateChain();
        if (chain.length != chain1.length) {
            throw new SecurityException("received chain not correct length");
        }
        for (int i = 0; i < chain.length; i++) {
            if (!chain[i].equals(chain1[i])) {
                throw new SecurityException("received chain not equal");
            }
        }

        // getEntry good pke 2
        pke = (KeyStore.PrivateKeyEntry)ks.getEntry("pk2", null);
        System.out.println("pk2 = " + pke.getPrivateKey());
        chain = pke.getCertificateChain();
        if (chain.length != chain2.length) {
            throw new SecurityException("received chain not correct length");
        }
        for (int i = 0; i < chain.length; i++) {
            if (!chain[i].equals(chain2[i])) {
                throw new SecurityException("received chain not equal");
            }
        }

        // getEntry good pke 3
        pke = (KeyStore.PrivateKeyEntry)ks.getEntry("pk3", null);
        System.out.println("pk3 = " + pke.getPrivateKey());
        chain = pke.getCertificateChain();
        if (chain.length != chain3.length) {
            throw new SecurityException("received chain not correct length");
        }
        for (int i = 0; i < chain.length; i++) {
            if (!chain[i].equals(chain3[i])) {
                throw new SecurityException("received chain not equal");
            }
        }

        // getKey good pke 1
        PrivateKey pkey = (PrivateKey)ks.getKey("pk1", null);
        if ("RSA".equals(pkey.getAlgorithm())) {
            System.out.println("test " + testnum++ + " passed");
        } else {
            throw new SecurityException
                ("expected RSA, got " + pkey.getAlgorithm());
        }

        // getCertificate chain chain 1
        chain = ks.getCertificateChain("pk1");
        if (chain.length != chain1.length) {
            throw new SecurityException("received chain not correct length");
        }
        for (int i = 0; i < chain.length; i++) {
            if (!chain[i].equals(chain1[i])) {
                throw new SecurityException("received chain not equal");
            }
        }

        // getKey good pke 2
        pkey = (PrivateKey)ks.getKey("pk2", null);
        if ("RSA".equals(pkey.getAlgorithm())) {
            System.out.println("test " + testnum++ + " passed");
        } else {
            throw new SecurityException
                ("expected RSA, got " + pkey.getAlgorithm());
        }

        // getCertificate chain chain 2
        chain = ks.getCertificateChain("pk2");
        if (chain.length != chain2.length) {
            throw new SecurityException("received chain not correct length");
        }
        for (int i = 0; i < chain.length; i++) {
            if (!chain[i].equals(chain2[i])) {
                throw new SecurityException("received chain not equal");
            }
        }

        // getKey good pke 3
        pkey = (PrivateKey)ks.getKey("pk3", null);
        if ("RSA".equals(pkey.getAlgorithm())) {
            System.out.println("test " + testnum++ + " passed");
        } else {
            throw new SecurityException
                ("expected RSA, got " + pkey.getAlgorithm());
        }

        // getCertificate chain chain 3
        chain = ks.getCertificateChain("pk3");
        if (chain.length != chain3.length) {
            throw new SecurityException("received chain not correct length");
        }
        for (int i = 0; i < chain.length; i++) {
            if (!chain[i].equals(chain3[i])) {
                throw new SecurityException("received chain not equal");
            }
        }

        // aliases
        enu = ks.aliases();
        count = 0;
        while (enu.hasMoreElements()) {
            count++;
            System.out.println("alias " +
                                count +
                                " = " +
                                (String)enu.nextElement());
        }
        if (count == 7) {
            System.out.println("test " + testnum++ + " passed");
        } else {
            throw new SecurityException("expected 7 aliases");
        }

        // size
        if (ks.size() == 7) {
            System.out.println("test " + testnum++ + " passed");
        } else {
            throw new SecurityException("expected size 7");
        }

        // getCertificate good chain 1
        if (ks.getCertificate("pk1").equals(chain1[0])) {
            System.out.println("test " + testnum++ + " passed");
        } else {
            throw new SecurityException("retrieved cert not equal");
        }

        // getCertificate good chain 3
        if (ks.getCertificate("pk3").equals(chain3[0])) {
            System.out.println("test " + testnum++ + " passed");
        } else {
            throw new SecurityException("retrieved cert not equal");
        }

        // getKey good ske 1
        skey = (SecretKey)ks.getKey("sk1", null);
        if ("DES".equals(skey.getAlgorithm())) {
            System.out.println("test " + testnum++ + " passed");
        } else {
            throw new SecurityException
                ("expected DES, got " + skey.getAlgorithm());
        }

        // getKey good ske 4
        skey = (SecretKey)ks.getKey("sk4", null);
        if ("ARCFOUR".equals(skey.getAlgorithm())) {
            System.out.println("test " + testnum++ + " passed");
        } else {
            throw new SecurityException
                ("expected ARCFOUR, got " + skey.getAlgorithm());
        }

        // getKey good pke 1
        pkey = (PrivateKey)ks.getKey("pk1", null);
        if ("RSA".equals(pkey.getAlgorithm())) {
            System.out.println("test " + testnum++ + " passed");
        } else {
            throw new SecurityException
                ("expected RSA, got " + pkey.getAlgorithm());
        }

        // getKey good pke 3
        pkey = (PrivateKey)ks.getKey("pk3", null);
        if ("RSA".equals(pkey.getAlgorithm())) {
            System.out.println("test " + testnum++ + " passed");
        } else {
            throw new SecurityException
                ("expected RSA, got " + pkey.getAlgorithm());
        }

        // contains alias
        if (!ks.containsAlias("pk1") ||
                !ks.containsAlias("pk2") ||
                !ks.containsAlias("pk3") ||
                !ks.containsAlias("sk1") ||
                !ks.containsAlias("sk2") ||
                !ks.containsAlias("sk3") ||
                !ks.containsAlias("sk4")) {
            throw new SecurityException("did not contain all aliases");
        }
        System.out.println("test " + testnum++ + " passed");

        // getCertificateAlias pk1
        if (ks.getCertificateAlias(chain1[0]).equals("pk1")) {
            System.out.println("test " + testnum++ + " passed");
        } else {
            throw new SecurityException("expected cert pk1");
        }

        // getCertificateAlias pk3
        if (ks.getCertificateAlias(chain3[0]).equals("pk3")) {
            System.out.println("test " + testnum++ + " passed");
        } else {
            throw new SecurityException("expected cert pk3");
        }

        // isCertificateEntry pk1
        if (!ks.isCertificateEntry("pk1")) {
            System.out.println("test " + testnum++ + " passed");
        } else {
            throw new SecurityException("expected pke");
        }

        // isCertificateEntry pk3
        if (!ks.isCertificateEntry("pk3")) {
            System.out.println("test " + testnum++ + " passed");
        } else {
            throw new SecurityException("expected pke");
        }

        // isCertificateEntry sk1
        if (!ks.isCertificateEntry("sk1")) {
            System.out.println("test " + testnum++ + " passed");
        } else {
            throw new SecurityException("expected ske");
        }

        // isCertificateEntry sk4
        if (!ks.isCertificateEntry("sk4")) {
            System.out.println("test " + testnum++ + " passed");
        } else {
            throw new SecurityException("expected ske");
        }

        // isKeyEntry pk1
        if (ks.isKeyEntry("pk1")) {
            System.out.println("test " + testnum++ + " passed");
        } else {
            throw new SecurityException("expected pke");
        }

        // isKeyEntry pk3
        if (ks.isKeyEntry("pk3")) {
            System.out.println("test " + testnum++ + " passed");
        } else {
            throw new SecurityException("expected pke");
        }

        // isKeyEntry sk1
        if (ks.isKeyEntry("sk1")) {
            System.out.println("test " + testnum++ + " passed");
        } else {
            throw new SecurityException("expected ske");
        }

        // isKeyEntry sk4
        if (ks.isKeyEntry("sk4")) {
            System.out.println("test " + testnum++ + " passed");
        } else {
            throw new SecurityException("expected ske");
        }

        // isCertificateEntry random foo
        if (!ks.isCertificateEntry("foo")) {
            System.out.println("test " + testnum++ + " passed");
        } else {
            throw new SecurityException("expected foo");
        }

        // isKeyEntry random foo
        if (!ks.isKeyEntry("foo")) {
            System.out.println("test " + testnum++ + " passed");
        } else {
            throw new SecurityException("expected foo");
        }

        // entryInstanceOf pk1
        if (!ks.entryInstanceOf
                ("pk1", KeyStore.TrustedCertificateEntry.class)) {
            System.out.println("test " + testnum++ + " passed");
        } else {
            throw new SecurityException("expected tce");
        }

        // entryInstanceOf pk3
        if (!ks.entryInstanceOf
                ("pk3", KeyStore.TrustedCertificateEntry.class)) {
            System.out.println("test " + testnum++ + " passed");
        } else {
            throw new SecurityException("expected tce");
        }

        // entryInstanceOf sk1
        if (!ks.entryInstanceOf
                ("sk1", KeyStore.TrustedCertificateEntry.class)) {
            System.out.println("test " + testnum++ + " passed");
        } else {
            throw new SecurityException("expected tce");
        }

        // entryInstanceOf sk4
        if (!ks.entryInstanceOf
                ("sk4", KeyStore.TrustedCertificateEntry.class)) {
            System.out.println("test " + testnum++ + " passed");
        } else {
            throw new SecurityException("expected tce");
        }

        // entryInstanceOf pk1
        if (ks.entryInstanceOf("pk1", KeyStore.PrivateKeyEntry.class)) {
            System.out.println("test " + testnum++ + " passed");
        } else {
            throw new SecurityException("expected pke");
        }

        // entryInstanceOf pk3
        if (ks.entryInstanceOf("pk3", KeyStore.PrivateKeyEntry.class)) {
            System.out.println("test " + testnum++ + " passed");
        } else {
            throw new SecurityException("expected pke");
        }

        // entryInstanceOf sk1
        if (!ks.entryInstanceOf("sk1", KeyStore.PrivateKeyEntry.class)) {
            System.out.println("test " + testnum++ + " passed");
        } else {
            throw new SecurityException("expected pke");
        }

        // entryInstanceOf sk4
        if (!ks.entryInstanceOf("sk4", KeyStore.PrivateKeyEntry.class)) {
            System.out.println("test " + testnum++ + " passed");
        } else {
            throw new SecurityException("expected pke");
        }

        // entryInstanceOf sk1
        if (ks.entryInstanceOf("sk1", KeyStore.SecretKeyEntry.class)) {
            System.out.println("test " + testnum++ + " passed");
        } else {
            throw new SecurityException("expected ske");
        }

        // entryInstanceOf sk4
        if (ks.entryInstanceOf("sk4", KeyStore.SecretKeyEntry.class)) {
            System.out.println("test " + testnum++ + " passed");
        } else {
            throw new SecurityException("expected ske");
        }

        // entryInstanceOf pk1
        if (!ks.entryInstanceOf("pk1", KeyStore.SecretKeyEntry.class)) {
            System.out.println("test " + testnum++ + " passed");
        } else {
            throw new SecurityException("expected ske");
        }

        // entryInstanceOf pk3
        if (!ks.entryInstanceOf("pk3", KeyStore.SecretKeyEntry.class)) {
            System.out.println("test " + testnum++ + " passed");
        } else {
            throw new SecurityException("expected ske");
        }

        // getEntry random foobar
        if (ks.getEntry("foobar", null) != null) {
            throw new SecurityException("expected null entry");
        } else {
            System.out.println("test " + testnum++ + " passed");
        }

        // deleteEntry
        ks.deleteEntry("pk1");
        ks.deleteEntry("pk3");
        ks.deleteEntry("sk2");
        ks.deleteEntry("sk3");
        System.out.println("test " + testnum++ + " passed");

        // aliases
        enu = ks.aliases();
        count = 0;
        while (enu.hasMoreElements()) {
            count++;
            System.out.println("alias " +
                                count +
                                " = " +
                                (String)enu.nextElement());
        }
        if (count == 3) {
            System.out.println("test " + testnum++ + " passed");
        } else {
            throw new SecurityException("expected 3 aliases");
        }

        // size
        if (ks.size() == 3) {
            System.out.println("test " + testnum++ + " passed");
        } else {
            throw new SecurityException("expected size 6");
        }

        // entryInstanceOf sk1
        if (!ks.entryInstanceOf("sk1", KeyStore.PrivateKeyEntry.class)) {
            System.out.println("test " + testnum++ + " passed");
        } else {
            throw new SecurityException("expected pke");
        }

        // entryInstanceOf sk4
        if (!ks.entryInstanceOf("sk4", KeyStore.PrivateKeyEntry.class)) {
            System.out.println("test " + testnum++ + " passed");
        } else {
            throw new SecurityException("expected pke");
        }

        // entryInstanceOf pk2
        if (ks.entryInstanceOf("pk2", KeyStore.PrivateKeyEntry.class)) {
            System.out.println("test " + testnum++ + " passed");
        } else {
            throw new SecurityException("expected pke");
        }
        System.out.println("test " + testnum++ + " passed");

        return testnum;
    }
}
