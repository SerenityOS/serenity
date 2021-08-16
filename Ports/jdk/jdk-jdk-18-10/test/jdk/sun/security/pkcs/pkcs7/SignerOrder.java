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
 * @bug 8048357
 * @summary test PKCS7 data signing, encoding and verification
 * @library /test/lib
 * @modules java.base/sun.security.pkcs
 *          java.base/sun.security.util
 *          java.base/sun.security.x509
 * @run main SignerOrder
 */
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.X509Certificate;
import java.util.Date;
import sun.security.pkcs.ContentInfo;
import sun.security.pkcs.PKCS7;
import sun.security.pkcs.SignerInfo;
import sun.security.util.DerOutputStream;
import sun.security.x509.AlgorithmId;
import sun.security.x509.CertificateAlgorithmId;
import sun.security.x509.CertificateSerialNumber;
import sun.security.x509.CertificateValidity;
import sun.security.x509.CertificateVersion;
import sun.security.x509.CertificateX509Key;
import sun.security.x509.X500Name;
import sun.security.x509.X509CertImpl;
import sun.security.x509.X509CertInfo;
import sun.security.x509.X509Key;
import jdk.test.lib.hexdump.HexPrinter;

public class SignerOrder {

    //signer infos
    static final byte[] data1 = "12345".getBytes();
    static final byte[] data2 = "abcde".getBytes();

    public static void main(String[] argv) throws Exception {

        SignerInfo[] signerInfos = new SignerInfo[9];
        SimpleSigner signer1 = new SimpleSigner(null, null, null, null);
        signerInfos[8] = signer1.genSignerInfo(data1);
        signerInfos[7] = signer1.genSignerInfo(new byte[]{});
        signerInfos[6] = signer1.genSignerInfo(data2);

        SimpleSigner signer2 = new SimpleSigner(null, null, null, null);
        signerInfos[5] = signer2.genSignerInfo(data1);
        signerInfos[4] = signer2.genSignerInfo(new byte[]{});
        signerInfos[3] = signer2.genSignerInfo(data2);

        SimpleSigner signer3 = new SimpleSigner(null, null, null, null);
        signerInfos[2] = signer3.genSignerInfo(data1);
        signerInfos[1] = signer3.genSignerInfo(new byte[]{});
        signerInfos[0] = signer3.genSignerInfo(data2);

        ContentInfo contentInfo = new ContentInfo(data1);

        AlgorithmId[] algIds = {new AlgorithmId(AlgorithmId.SHA256_oid)};

        X509Certificate[] certs = {signer3.getCert(), signer2.getCert(),
            signer1.getCert()};

        PKCS7 pkcs71 = new PKCS7(algIds, contentInfo,
                certs,
                signerInfos);

        System.out.println("SignerInfos in original.");
        printSignerInfos(pkcs71.getSignerInfos());

        DerOutputStream out = new DerOutputStream();
        pkcs71.encodeSignedData(out);

        PKCS7 pkcs72 = new PKCS7(out.toByteArray());
        System.out.println("\nSignerInfos read back in:");
        printSignerInfos(pkcs72.getSignerInfos());

        System.out.println("Verified signers of original:");
        SignerInfo[] verifs1 = pkcs71.verify();

        System.out.println("Verified signers of after read-in:");
        SignerInfo[] verifs2 = pkcs72.verify();

        if (verifs1.length != verifs2.length) {
            throw new RuntimeException("Length or Original vs read-in "
                    + "should be same");
        }
    }

    static void printSignerInfos(SignerInfo signerInfo) throws IOException {
        ByteArrayOutputStream strm = new ByteArrayOutputStream();
        signerInfo.derEncode(strm);
        System.out.println("SignerInfo, length: "
                + strm.toByteArray().length);
        HexPrinter.simple().format(strm.toByteArray());
        System.out.println("\n");
        strm.reset();
    }

    static void printSignerInfos(SignerInfo[] signerInfos) throws IOException {
        ByteArrayOutputStream strm = new ByteArrayOutputStream();
        for (int i = 0; i < signerInfos.length; i++) {
            signerInfos[i].derEncode(strm);
            System.out.println("SignerInfo[" + i + "], length: "
                    + strm.toByteArray().length);
            HexPrinter.simple().format(strm.toByteArray());
            System.out.println("\n");
            strm.reset();
        }
    }

}

/**
 * A simple extension of sun.security.x509.X500Signer that adds a no-fuss
 * signing algorithm.
 */
class SimpleSigner {

    private final Signature sig;
    private final X500Name agent;
    private final AlgorithmId digestAlgId;
    private final AlgorithmId encryptionAlgId;
    private final AlgorithmId algId; // signature algid;
                                     //combines digest + encryption
    private final X509Key publicKey;
    private final PrivateKey privateKey;
    private final X509Certificate cert;

    public SimpleSigner(String digestAlg,
            String encryptionAlg,
            KeyPair keyPair,
            X500Name agent) throws Exception {

        if (agent == null) {
            agent = new X500Name("cn=test");
        }
        if (digestAlg == null) {
            digestAlg = "SHA";
        }
        if (encryptionAlg == null) {
            encryptionAlg = "DSA";
        }
        if (keyPair == null) {
            KeyPairGenerator keyGen =
                    KeyPairGenerator.getInstance(encryptionAlg);
            keyGen.initialize(1024);
            keyPair = keyGen.generateKeyPair();
        }
        publicKey = (X509Key) keyPair.getPublic();
        privateKey = keyPair.getPrivate();

        if ("DSA".equals(encryptionAlg)) {
            this.sig = Signature.getInstance(encryptionAlg);
        } else { // RSA
            this.sig = Signature.getInstance(digestAlg + "/" + encryptionAlg);
        }
        this.sig.initSign(privateKey);

        this.agent = agent;
        this.digestAlgId = AlgorithmId.get(digestAlg);
        this.encryptionAlgId = AlgorithmId.get(encryptionAlg);
        this.algId = AlgorithmId.get(this.sig.getAlgorithm());

        this.cert = getSelfCert();
    }

    /**
     * Take the data and sign it.
     *
     * @param buf buffer holding the next chunk of the data to be signed
     * @param offset starting point of to-be-signed data
     * @param len how many bytes of data are to be signed
     * @return the signature for the input data.
     * @exception SignatureException on errors.
     */
    public byte[] simpleSign(byte[] buf, int offset, int len)
            throws SignatureException {
        sig.update(buf, offset, len);
        return sig.sign();
    }

    /**
     * Returns the digest algorithm used to sign.
     */
    public AlgorithmId getDigestAlgId() {
        return digestAlgId;
    }

    /**
     * Returns the encryption algorithm used to sign.
     */
    public AlgorithmId getEncryptionAlgId() {
        return encryptionAlgId;
    }

    /**
     * Returns the name of the signing agent.
     */
    public X500Name getSigner() {
        return agent;
    }

    public X509Certificate getCert() {
        return cert;
    }

    private X509Certificate getSelfCert() throws Exception {
        long validity = 1000;
        X509CertImpl certLocal;
        Date firstDate, lastDate;

        firstDate = new Date();
        lastDate = new Date();
        lastDate.setTime(lastDate.getTime() + validity + 1000);

        CertificateValidity interval = new CertificateValidity(firstDate,
                lastDate);

        X509CertInfo info = new X509CertInfo();
        // Add all mandatory attributes
        info.set(X509CertInfo.VERSION,
                new CertificateVersion(CertificateVersion.V1));
        info.set(X509CertInfo.SERIAL_NUMBER,
                new CertificateSerialNumber(
                        (int) (firstDate.getTime() / 1000)));
        info.set(X509CertInfo.ALGORITHM_ID,
                new CertificateAlgorithmId(algId));
        info.set(X509CertInfo.SUBJECT, agent);
        info.set(X509CertInfo.KEY, new CertificateX509Key(publicKey));
        info.set(X509CertInfo.VALIDITY, interval);
        info.set(X509CertInfo.ISSUER, agent);

        certLocal = new X509CertImpl(info);
        certLocal.sign(privateKey, algId.getName());

        return certLocal;
    }

    public SignerInfo genSignerInfo(byte[] data) throws SignatureException {
        return new SignerInfo((X500Name) cert.getIssuerDN(),
                new BigInteger("" + cert.getSerialNumber()),
                getDigestAlgId(), algId,
                simpleSign(data, 0, data.length));
    }
}
