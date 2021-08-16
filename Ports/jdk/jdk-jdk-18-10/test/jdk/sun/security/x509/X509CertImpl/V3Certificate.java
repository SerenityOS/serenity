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

/*
 * @test
 * @bug 8049237 8242151
 * @modules java.base/sun.security.x509
 *          java.base/sun.security.util
 *          jdk.crypto.ec
 * @summary This test generates V3 certificate with all the supported
 * extensions. Writes back the generated certificate in to a file and checks for
 * equality with the original certificate.
 */

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import sun.security.util.BitArray;
import sun.security.util.ObjectIdentifier;
import sun.security.x509.*;

import static java.lang.System.out;

public class V3Certificate {

    public static final String V3_FILE = "certV3";
    public static final String V3_B64_FILE = "certV3.b64";

    public static void main(String[] args) throws IOException,
            NoSuchAlgorithmException, InvalidKeyException, CertificateException,
            NoSuchProviderException, SignatureException {

        boolean success = true;

        success &= test("RSA", "SHA256withRSA", 2048);
        success &= test("DSA", "SHA256withDSA", 2048);
        success &= test("EC", "SHA256withECDSA", 384);

        if (!success) {
            throw new RuntimeException("At least one test case failed");
        }
    }

    public static boolean test(String algorithm, String sigAlg, int keyLength)
            throws IOException,
            NoSuchAlgorithmException,
            InvalidKeyException,
            CertificateException,
            NoSuchProviderException,
            SignatureException {

        byte[] issuerId = {1, 2, 3, 4, 5};
        byte[] subjectId = {6, 7, 8, 9, 10};
        boolean testResult = true;

        // Subject and Issuer
        X500Name subject = new X500Name("test", "Oracle", "Santa Clara",
                "US");
        X500Name issuer = subject;

        // Generate keys and sign
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance(algorithm);
        keyGen.initialize(keyLength);
        KeyPair pair = keyGen.generateKeyPair();
        PublicKey publicKey = pair.getPublic();
        PrivateKey privateKey = pair.getPrivate();
        MessageDigest md = MessageDigest.getInstance("SHA");
        byte[] keyId = md.digest(publicKey.getEncoded());

        Signature signature = Signature.getInstance(sigAlg);
        signature.initSign(privateKey);

        // Validity interval
        Date firstDate = new Date();
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("PST"));
        cal.set(2014, 03, 10, 12, 30, 30);
        Date lastDate = cal.getTime();
        CertificateValidity interval = new CertificateValidity(firstDate,
                lastDate);

        // Certificate Info
        X509CertInfo cert = new X509CertInfo();

        cert.set(X509CertInfo.VERSION,
                new CertificateVersion(CertificateVersion.V3));
        cert.set(X509CertInfo.SERIAL_NUMBER,
                new CertificateSerialNumber((int) (firstDate.getTime() / 1000)));
        cert.set(X509CertInfo.ALGORITHM_ID,
                new CertificateAlgorithmId(AlgorithmId.get(sigAlg)));
        cert.set(X509CertInfo.SUBJECT, subject);
        cert.set(X509CertInfo.KEY, new CertificateX509Key(publicKey));
        cert.set(X509CertInfo.VALIDITY, interval);
        cert.set(X509CertInfo.ISSUER, issuer);

        cert.set(X509CertInfo.ISSUER_ID,
                new UniqueIdentity(
                        new BitArray(issuerId.length * 8 - 2, issuerId)));
        cert.set(X509CertInfo.SUBJECT_ID, new UniqueIdentity(subjectId));

        // Create Extensions
        CertificateExtensions exts = new CertificateExtensions();

        GeneralNameInterface mailInf = new RFC822Name("test@Oracle.com");
        GeneralName mail = new GeneralName(mailInf);
        GeneralNameInterface dnsInf = new DNSName("Oracle.com");
        GeneralName dns = new GeneralName(dnsInf);
        GeneralNameInterface uriInf = new URIName("http://www.Oracle.com");
        GeneralName uri = new GeneralName(uriInf);

        // localhost
        byte[] address = new byte[]{127, 0, 0, 1};

        GeneralNameInterface ipInf = new IPAddressName(address);
        GeneralName ip = new GeneralName(ipInf);

        GeneralNameInterface oidInf =
                new OIDName(ObjectIdentifier.of("1.2.3.4"));
        GeneralName oid = new GeneralName(oidInf);

        SubjectAlternativeNameExtension subjectName
                = new SubjectAlternativeNameExtension();
        IssuerAlternativeNameExtension issuerName
                = new IssuerAlternativeNameExtension();

        GeneralNames subjectNames
                = (GeneralNames) subjectName.
                get(SubjectAlternativeNameExtension.SUBJECT_NAME);

        GeneralNames issuerNames
                = (GeneralNames) issuerName.
                get(IssuerAlternativeNameExtension.ISSUER_NAME);

        subjectNames.add(mail);
        subjectNames.add(dns);
        subjectNames.add(uri);

        issuerNames.add(ip);
        issuerNames.add(oid);

        cal.set(2000, 11, 15, 12, 30, 30);
        lastDate = cal.getTime();
        PrivateKeyUsageExtension pkusage
                = new PrivateKeyUsageExtension(firstDate, lastDate);

        KeyUsageExtension usage = new KeyUsageExtension();
        usage.set(KeyUsageExtension.CRL_SIGN, true);
        usage.set(KeyUsageExtension.DIGITAL_SIGNATURE, true);
        usage.set(KeyUsageExtension.NON_REPUDIATION, true);

        KeyIdentifier kid = new KeyIdentifier(keyId);
        SerialNumber sn = new SerialNumber(42);
        AuthorityKeyIdentifierExtension aki
                = new AuthorityKeyIdentifierExtension(kid, subjectNames, sn);

        SubjectKeyIdentifierExtension ski
                = new SubjectKeyIdentifierExtension(keyId);

        BasicConstraintsExtension cons
                = new BasicConstraintsExtension(true, 10);

        PolicyConstraintsExtension pce = new PolicyConstraintsExtension(2, 4);

        exts.set(SubjectAlternativeNameExtension.NAME, subjectName);
        exts.set(IssuerAlternativeNameExtension.NAME, issuerName);
        exts.set(PrivateKeyUsageExtension.NAME, pkusage);
        exts.set(KeyUsageExtension.NAME, usage);
        exts.set(AuthorityKeyIdentifierExtension.NAME, aki);
        exts.set(SubjectKeyIdentifierExtension.NAME, ski);
        exts.set(BasicConstraintsExtension.NAME, cons);
        exts.set(PolicyConstraintsExtension.NAME, pce);
        cert.set(X509CertInfo.EXTENSIONS, exts);

        // Generate and sign X509CertImpl
        X509CertImpl crt = new X509CertImpl(cert);
        crt.sign(privateKey, sigAlg);
        crt.verify(publicKey);

        try (FileOutputStream fos = new FileOutputStream(new File(V3_FILE));
                FileOutputStream fos_b64
                = new FileOutputStream(new File(V3_B64_FILE));
                PrintWriter pw = new PrintWriter(fos_b64)) {
            crt.encode((OutputStream) fos);
            fos.flush();

            // Certificate boundaries/
            pw.println("-----BEGIN CERTIFICATE-----");
            pw.flush();
            fos_b64.write(Base64.getMimeEncoder().encode(crt.getEncoded()));
            fos_b64.flush();
            pw.println("-----END CERTIFICATE-----");
        }

        out.println("*** Certificate ***");
        out.println(crt);
        out.println("*** End Certificate ***");

        X509Certificate x2 = generateCertificate(V3_FILE);
        if (!x2.equals(crt)) {
            out.println("*** Certificate mismatch ***");
            testResult = false;
        }

        X509Certificate x3 = generateCertificate(V3_B64_FILE);
        if (!x3.equals(crt)) {
            out.println("*** Certificate mismatch ***");
            testResult = false;
        }

        return testResult;
    }

    static X509Certificate generateCertificate(String certFile) {
        try (InputStream inStrm = new FileInputStream(certFile)) {
            CertificateFactory cf = CertificateFactory.getInstance("X509");
            X509Certificate x2
                    = (X509Certificate) cf.generateCertificate(inStrm);
            return x2;
        } catch (CertificateException | IOException e) {
            throw new RuntimeException("Exception while "
                    + "genrating certificate for " + certFile, e);
        }
    }
}
