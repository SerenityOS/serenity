/*
 * Copyright (c) 2017, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8175029
 * @library /test/lib
 * @summary check that default implementation of
 *          X509Certificate.verify(PublicKey, Provider) works on custom
 *          X509Certificate impl.
 */

import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Principal;
import java.security.Provider;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;
import jdk.test.lib.security.CertUtils;

public class VerifyDefault {
    private static final String TEST_CERT =
        "-----BEGIN CERTIFICATE-----\n" +
        "MIICvTCCAaWgAwIBAgIEGYqL9TANBgkqhkiG9w0BAQsFADAPMQ0wCwYDVQQDEwRT\n" +
        "ZWxmMB4XDTE3MDMyODE2NDcyNloXDTE3MDYyNjE2NDcyNlowDzENMAsGA1UEAxME\n" +
        "U2VsZjCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAL1pfSJljFVSABOL\n" +
        "tJbIVPEkz1+2AFgzY1hqwE0EH80lvhOEkiPPYCKwBE5VTZdyFfwFjpyx7eEeJMNT\n" +
        "o7cixfmkQaiXHr/S1AS4BRTqLG/zgLzoJpVbzi45rnVEZc0oTm11KG3uUxkZTRr3\n" +
        "5ORbYyZpkscKwHL2M0J/1GmnA1hmhQdwUQyIKxg4eKQwyE+/TdbFlCWVNnOlb+91\n" +
        "eXvS11nIJ1oaBgn7u4qihuVmFmngLMxExnLYKV6AwdkwFD6pERObclRD9vAl5eUk\n" +
        "+sM6zQYwfLdyC2i8e+ETBeOg1ijptM4KT5Uaq89zxjLR0DPH4S+aILp3gYHGrW5r\n" +
        "eMxZAEMCAwEAAaMhMB8wHQYDVR0OBBYEFOME39JtbjzQaK3ufpKo/Pl4sZ8XMA0G\n" +
        "CSqGSIb3DQEBCwUAA4IBAQCDcw0+Sf0yeVROVlb2/VV3oIblHkGQheXeIurW64k7\n" +
        "tEzHtx9i8dnj5lzTZNH6hU4GRlyULbSDzjcM3P2XFRsM+0a/kEJZVqnLz5ji//7/\n" +
        "ZXaRX0TiE2IfFOTGbO6LusO3yR4tOER/WHllz2H21C2SbW3+92Ou28glTZa42AAZ\n" +
        "mUj9j+p6mZqD4/tUBqAEqqQoMIhw9CNjc46STNayBjt/0/+I2pfy6LagrMbjBzZ0\n" +
        "A5kXg9WjnywGk8XFr/3RZz8DrUmCYs2qCYLCHQHsuCE6gCuf9wKhKyD51MFXXRr0\n" +
        "cyG6LYQjrreMHYk4ZfN2NPC6lGjWxB5mIbV/DuikCnYu\n" +
        "-----END CERTIFICATE-----";

    private static class TestX509Certificate extends X509Certificate {
        private final X509Certificate cert;
        TestX509Certificate(X509Certificate cert) {
            this.cert = cert;
        }
        public Set<String> getCriticalExtensionOIDs() {
           return cert.getCriticalExtensionOIDs();
        }
        public byte[] getExtensionValue(String oid) {
            return cert.getExtensionValue(oid);
        }
        public Set<String> getNonCriticalExtensionOIDs() {
            return cert.getNonCriticalExtensionOIDs();
        }
        public boolean hasUnsupportedCriticalExtension() {
            return cert.hasUnsupportedCriticalExtension();
        }
        public void checkValidity() throws CertificateExpiredException,
            CertificateNotYetValidException {
            cert.checkValidity();
        }
        public void checkValidity(Date date) throws CertificateExpiredException,
            CertificateNotYetValidException {
            cert.checkValidity(date);
        }
        public int getVersion() { return cert.getVersion(); }
        public BigInteger getSerialNumber() { return cert.getSerialNumber(); }
        public Principal getIssuerDN() { return cert.getIssuerDN(); }
        public Principal getSubjectDN() { return cert.getSubjectDN(); }
        public Date getNotBefore() { return cert.getNotBefore(); }
        public Date getNotAfter() { return cert.getNotAfter(); }
        public byte[] getTBSCertificate() throws CertificateEncodingException {
            return cert.getTBSCertificate();
        }
        public byte[] getSignature() { return cert.getSignature(); }
        public String getSigAlgName() { return cert.getSigAlgName(); }
        public String getSigAlgOID() { return cert.getSigAlgOID(); }
        public byte[] getSigAlgParams() { return cert.getSigAlgParams(); }
        public boolean[] getIssuerUniqueID() {
            return cert.getIssuerUniqueID();
        }
        public boolean[] getSubjectUniqueID() {
            return cert.getSubjectUniqueID();
        }
        public boolean[] getKeyUsage() { return cert.getKeyUsage(); }
        public int getBasicConstraints() { return cert.getBasicConstraints(); }
        public byte[] getEncoded() throws CertificateEncodingException {
            return cert.getEncoded();
        }
        public void verify(PublicKey key) throws CertificateException,
            InvalidKeyException, NoSuchAlgorithmException,
            NoSuchProviderException, SignatureException {
            cert.verify(key);
        }
        public void verify(PublicKey key, String sigProvider) throws
            CertificateException, InvalidKeyException, NoSuchAlgorithmException,
            NoSuchProviderException, SignatureException {
            cert.verify(key, sigProvider);
        }
        public PublicKey getPublicKey() { return cert.getPublicKey(); }
        public String toString() { return cert.toString(); }
    }

    public static void main(String[] args) throws Exception {
        X509Certificate cert = CertUtils.getCertFromString(TEST_CERT);
        new TestX509Certificate(cert).verify(cert.getPublicKey(),
                                             (Provider)null);
    }
}
