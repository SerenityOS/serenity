/*
 * Copyright (c) 2003, Oracle and/or its affiliates. All rights reserved.
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
 * @test 1.1, 03/06/24
 * @bug 4850376
 * @summary Provide generic storage KeyStore storage facilities
 */

import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Principal;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.Set;
import java.util.HashSet;
import java.math.BigInteger;
import javax.security.auth.x500.X500Principal;

public class DefaultEntryType {

    private static class PrivKey1 implements PrivateKey {
        public String getAlgorithm() { return ("matching_alg"); }
        public String getFormat() { return "privkey1"; }
        public byte[] getEncoded() { return (byte[])null; }
    }

    private static class PubKey1 implements PublicKey {
        public String getAlgorithm() { return ("non_matching_alg"); }
        public String getFormat() { return "pubkey1"; }
        public byte[] getEncoded() { return (byte[])null; }
    }

    private static class PubKey2 implements PublicKey {
        public String getAlgorithm() { return ("matching_alg"); }
        public String getFormat() { return "pubkey2"; }
        public byte[] getEncoded() { return (byte[])null; }
    }

    private static class Cert extends Certificate {
        public Cert() { super("cert"); }
        public byte[] getEncoded()
                throws CertificateEncodingException { return (byte[])null; }
        public void verify(PublicKey key)
                throws CertificateException, NoSuchAlgorithmException,
                InvalidKeyException, NoSuchProviderException,
                SignatureException { }
        public void verify(PublicKey key, String sigProvider)
                throws CertificateException, NoSuchAlgorithmException,
                InvalidKeyException, NoSuchProviderException,
                SignatureException { }
        public String toString() { return "cert"; }
        public PublicKey getPublicKey() { return new PubKey1(); }
    }

    private static class X509Cert extends X509Certificate {
        public byte[] getEncoded()
                throws CertificateEncodingException { return (byte[])null; }
        public void verify(PublicKey key)
                throws CertificateException, NoSuchAlgorithmException,
                InvalidKeyException, NoSuchProviderException,
                SignatureException { }
        public void verify(PublicKey key, String sigProvider)
                throws CertificateException, NoSuchAlgorithmException,
                InvalidKeyException, NoSuchProviderException,
                SignatureException { }
        public String toString() { return "x509cert"; }
        public PublicKey getPublicKey() { return new PubKey2(); }

        public void checkValidity()
                throws CertificateExpiredException,
                CertificateNotYetValidException { }
        public void checkValidity(java.util.Date date)
                throws CertificateExpiredException,
                CertificateNotYetValidException { }
        public int getVersion() { return 1; }
        public BigInteger getSerialNumber() { return new BigInteger("5", 10); }
        public Principal getIssuerDN()
                                { return new X500Principal("cn=x509cert"); }
        public X500Principal getIssuerX500Principal()
                                { return new X500Principal("cn=x509cert"); }
        public Principal getSubjectDN()
                                { return new X500Principal("cn=x509cert"); }
        public X500Principal getSubjectX500Principal()
                                { return new X500Principal("cn=x509cert"); }
        public Date getNotBefore() { return new Date(); }
        public Date getNotAfter() { return new Date(); }
        public byte[] getTBSCertificate() throws CertificateEncodingException
                                { return (byte[])null; }
        public byte[] getSignature() { return (byte[])null; }
        public String getSigAlgName() { return "x509cert"; }
        public String getSigAlgOID() { return "x509cert"; }
        public byte[] getSigAlgParams() { return (byte[])null; }

        public boolean[] getIssuerUniqueID() { return (boolean[])null; }
        public boolean[] getSubjectUniqueID() { return (boolean[])null; }
        public boolean[] getKeyUsage() { return (boolean[]) null; }
        public int getBasicConstraints() { return 1; }

        public boolean hasUnsupportedCriticalExtension() { return true; }
        public Set getCriticalExtensionOIDs() { return new HashSet(); }
        public Set getNonCriticalExtensionOIDs() { return new HashSet(); }
        public byte[] getExtensionValue(String oid) { return (byte[])null; }
    }

    public static void main(String[] args) throws Exception {
        testPrivateKeyEntry();
        testSecretKeyEntry();
        testTrustedCertificateEntry();
    }

    private static void testPrivateKeyEntry() throws Exception {
        // TEST null private key
        try {
            Certificate[] chain = new Certificate[0];
            KeyStore.PrivateKeyEntry pke = new KeyStore.PrivateKeyEntry
                                                        (null, chain);
            throw new SecurityException("test 1 failed");
        } catch (NullPointerException npe) {
            // good
            System.out.println("test 1 passed");
        }

        // TEST null chain
        try {
            KeyStore.PrivateKeyEntry pke = new KeyStore.PrivateKeyEntry
                                                (new PrivKey1(), null);
            throw new SecurityException("test 2 failed");
        } catch (NullPointerException npe) {
            // good
            System.out.println("test 2 passed");
        }

        // TEST empty chain
        try {
            Certificate[] chain = new Certificate[0];
            KeyStore.PrivateKeyEntry pke = new KeyStore.PrivateKeyEntry
                                                (new PrivKey1(), chain);
            throw new SecurityException("test 3 failed");
        } catch (IllegalArgumentException npe) {
            // good
            System.out.println("test 3 passed");
        }

        // TEST non-homogenous chain
        try {
            Certificate[] chain = new Certificate[2];
            chain[0] = new Cert();
            chain[1] = new X509Cert();
            KeyStore.PrivateKeyEntry pke = new KeyStore.PrivateKeyEntry
                                        (new PrivKey1(), chain);
            throw new SecurityException("test 4 failed");
        } catch (IllegalArgumentException npe) {
            // good
            System.out.println("test 4 passed");
        }

        // TEST non matching algorithms
        try {
            Certificate[] chain = new Certificate[1];
            chain[0] = new Cert();
            KeyStore.PrivateKeyEntry pke = new KeyStore.PrivateKeyEntry
                                        (new PrivKey1(), chain);
            throw new SecurityException("test 5 failed");
        } catch (IllegalArgumentException npe) {
            // good
            System.out.println("test 5 passed");
        }

        // TEST correct behavior
        Certificate[] chain = new Certificate[2];
        chain[0] = new X509Cert();
        chain[1] = new X509Cert();
        PrivateKey pkey = new PrivKey1();
        KeyStore.PrivateKeyEntry pke = new KeyStore.PrivateKeyEntry
                                                (pkey, chain);
        Certificate[] gotChain = pke.getCertificateChain();
        if (gotChain instanceof X509Certificate[]) {
            System.out.println("test 6 passed");
        } else {
            throw new SecurityException("test 6 failed");
        }

        if (gotChain.length == 2 &&
            gotChain[0] == chain[0] &&
            gotChain[1] == chain[1]) {
            System.out.println("test 7 passed");
        } else {
            throw new SecurityException("test 7 failed");
        }

        if (pke.getPrivateKey() == pkey) {
            System.out.println("test 8 passed");
        } else {
            throw new SecurityException("test 8 failed");
        }
    }

    private static void testSecretKeyEntry() throws Exception {
    }

    private static void testTrustedCertificateEntry() throws Exception {
    }
}
