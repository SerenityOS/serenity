/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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


// program to generate rsakeys.ks. does not need to run during testing
// checked into the workspace so that the keystore file can be recreated
// in the future if needed.

// @author Andreas Sterbenz

import java.io.*;
import java.math.BigInteger;
import java.util.*;

import java.security.*;
import java.security.cert.*;
import java.security.interfaces.*;
import java.security.spec.*;

import sun.security.x509.*;

public class GenKeyStore {

    static final char[] password = "test12".toCharArray();

    private static X509Certificate getCertificate(String suffix, PublicKey publicKey, PrivateKey privateKey) throws Exception {
        X500Name name = new X500Name("CN=Dummy Certificate " + suffix);
        String algorithm = "SHA1with" + publicKey.getAlgorithm();
        Date date = new Date();
        AlgorithmId algID = AlgorithmId.getAlgorithmId(algorithm);

        X509CertInfo certInfo = new X509CertInfo();

        certInfo.set(X509CertInfo.VERSION, new CertificateVersion(CertificateVersion.V1));
        certInfo.set(X509CertInfo.SERIAL_NUMBER, new CertificateSerialNumber(1));
        certInfo.set(X509CertInfo.ALGORITHM_ID, new CertificateAlgorithmId(algID));
        certInfo.set(X509CertInfo.SUBJECT, name);
        certInfo.set(X509CertInfo.ISSUER, name);
        certInfo.set(X509CertInfo.KEY, new CertificateX509Key(publicKey));
        certInfo.set(X509CertInfo.VALIDITY, new CertificateValidity(date, date));

        X509CertImpl cert = new X509CertImpl(certInfo);
        cert.sign(privateKey, algorithm);

        return cert;
    }

    private static void addToKeyStore(KeyStore ks, KeyPair kp, String name) throws Exception {
        PublicKey pubKey = kp.getPublic();
        PrivateKey privKey = kp.getPrivate();
        X509Certificate cert = getCertificate(name, pubKey, privKey);
        ks.setKeyEntry(name, privKey, password, new X509Certificate[] {cert});
    }

    private static void generateKeyPair(KeyStore ks, int keyLength, String alias) throws Exception {
        System.out.println("Generating " + keyLength + " keypair " + alias + "...");
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA", "SunRsaSign");
        kpg.initialize(keyLength);
        KeyPair kp = kpg.generateKeyPair();
        addToKeyStore(ks, kp, alias);
    }

    static KeyStore ks;

    public static void main(String[] args) throws Exception {
        long start = System.currentTimeMillis();

        KeyStore ks = KeyStore.getInstance("JKS");
        ks.load(null, null);

        generateKeyPair(ks, 512, "rsa512a");
        generateKeyPair(ks, 512, "rsa512b");
        generateKeyPair(ks, 1024, "rsa1024a");
        generateKeyPair(ks, 1024, "rsa1024b");
        generateKeyPair(ks, 2048, "rsa2048a");
        generateKeyPair(ks, 2048, "rsa2048b");
        generateKeyPair(ks, 4096, "rsa4096a");

        // only one 4096 bit keys and none longer than that
        // that would slow down the other tests too much
        // on old machines
//      generateKeyPair(ks, 4096, "rsa4096b");
//      generateKeyPair(ks, 8192, "rsa8192a");
//      generateKeyPair(ks, 8192, "rsa8192b");

        OutputStream out = new FileOutputStream("rsakeys.ks");
        ks.store(out, password);
        out.close();

        long stop = System.currentTimeMillis();
        System.out.println("Done (" + (stop - start) + " ms).");
    }

}
