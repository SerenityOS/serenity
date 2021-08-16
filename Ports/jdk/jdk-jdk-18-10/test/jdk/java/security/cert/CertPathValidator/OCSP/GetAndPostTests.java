/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8179503
 * @summary Java should support GET OCSP calls
 * @library /javax/net/ssl/templates /java/security/testlibrary
 * @build SimpleOCSPServer
 * @modules java.base/sun.security.util
 *          java.base/sun.security.provider.certpath
 *          java.base/sun.security.x509
 * @run main/othervm GetAndPostTests
 */

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.CertPath;
import java.security.cert.CertPathValidator;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.Extension;
import java.security.cert.PKIXCertPathChecker;
import java.security.cert.PKIXParameters;
import java.security.cert.PKIXRevocationChecker;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import sun.security.testlibrary.SimpleOCSPServer;
import sun.security.testlibrary.SimpleOCSPServer;
import sun.security.testlibrary.SimpleOCSPServer;
import sun.security.util.DerOutputStream;
import sun.security.util.DerValue;
import sun.security.util.ObjectIdentifier;
import sun.security.testlibrary.SimpleOCSPServer;

public class GetAndPostTests {
    private static final String PASS = "passphrase";
    private static CertificateFactory certFac;

    public static void main(String args[]) throws Exception {
        SimpleOCSPServer ocspResponder = null;

        try {
            certFac = CertificateFactory.getInstance("X.509");

            // Read in the certificates and keys needed for this test and
            // create the keystore for the SimpleOCSPServer.  For the purposes
            // of this test, the CA certificate will also be the OCSP responder
            // signing certificate.
            SSLSocketTemplate.Cert certAuth =
                    SSLSocketTemplate.Cert.CA_ECDSA_SECP256R1;
            X509Certificate caCert = pem2Cert(certAuth.certStr);
            PrivateKey caKey = pem2Key(certAuth.privKeyStr, certAuth.keyAlgo);
            X509Certificate endEntCert =
                    pem2Cert(SSLSocketTemplate.Cert.EE_ECDSA_SECP256R1.certStr);

            KeyStore.Builder keyStoreBuilder =
                    KeyStore.Builder.newInstance("PKCS12", null,
                        new KeyStore.PasswordProtection(PASS.toCharArray()));
            KeyStore ocspStore = keyStoreBuilder.getKeyStore();
            Certificate[] ocspChain = {caCert};
            ocspStore.setKeyEntry("ocspsigner", caKey, PASS.toCharArray(),
                    ocspChain);

            // Create the certificate path we'll use for cert path validation.
            CertPath path = certFac.generateCertPath(List.of(endEntCert));

            // Next, create and start the OCSP responder.  Obtain the socket
            // address so we can set that in the PKIXRevocationChecker since
            // these certificates do not have AIA extensions on them.
            ocspResponder = new SimpleOCSPServer(ocspStore, PASS,
                    "ocspsigner", null);
            ocspResponder.setSignatureAlgorithm("SHA256WithECDSA");
            ocspResponder.enableLog(true);
            ocspResponder.setNextUpdateInterval(3600);
            ocspResponder.updateStatusDb(Map.of(
                    endEntCert.getSerialNumber(),
                    new SimpleOCSPServer.CertStatusInfo(
                            SimpleOCSPServer.CertStatus.CERT_STATUS_GOOD)));
            ocspResponder.start();
            // Wait 5 seconds for server ready
            for (int i = 0; (i < 100 && !ocspResponder.isServerReady()); i++) {
                Thread.sleep(50);
            }
            if (!ocspResponder.isServerReady()) {
                throw new RuntimeException("Server not ready yet");
            }

            int ocspPort = ocspResponder.getPort();
            URI ocspURI = new URI("http://localhost:" + ocspPort);
            System.out.println("Configured CPV to connect to " + ocspURI);

            // Create the PKIXParameters needed for path validation and
            // configure any necessary OCSP parameters to control the OCSP
            // request size.
            Set<TrustAnchor> anchors = Set.of(new TrustAnchor(caCert, null));

            CertPathValidator validator = CertPathValidator.getInstance("PKIX");
            PKIXRevocationChecker revChkr =
                    (PKIXRevocationChecker)validator.getRevocationChecker();
            revChkr.setOcspResponder(ocspURI);
            revChkr.setOptions(Set.of(
                    PKIXRevocationChecker.Option.ONLY_END_ENTITY,
                    PKIXRevocationChecker.Option.NO_FALLBACK));

            PKIXParameters params = new PKIXParameters(anchors);
            params.setRevocationEnabled(true);
            params.setDate(new Date(1590926400000L)); // 05/31/2020 @ 12:00:00Z
            params.addCertPathChecker(revChkr);

            System.out.println("Test 1: Request < 255 bytes, HTTP GET");
            validator.validate(path, params);

            System.out.println("Test 2: Request > 255 bytes, HTTP POST");
            // Modify the PKIXRevocationChecker to include a bogus non-critical
            // request extension that makes the request large enough to be
            // issued as an HTTP POST.
            List<PKIXCertPathChecker> chkrList = params.getCertPathCheckers();
            for (PKIXCertPathChecker chkr : chkrList) {
                if (chkr instanceof PKIXRevocationChecker) {
                    ((PKIXRevocationChecker)chkr).setOcspExtensions(
                            List.of(new BogusExtension("1.2.3.4.5.6.7.8.9",
                                    false, 256)));
                }
            }
            params.setCertPathCheckers(chkrList);
            validator.validate(path, params);

        } finally {
            if (ocspResponder != null) {
                ocspResponder.stop();
            }
        }
    }

    /**
     * Create an X509Certificate object from its PEM encoding
     *
     * @param pemCert the base64 encoded certificate
     *
     * @return the corresponding X509Certificate object from the PEM encoding.
     *
     * @throws IOException if any InputStream or Base64 decoding failures occur.
     * @throws CertificateException if any certificate parsing errors occur.
     */
    private static X509Certificate pem2Cert(String pemCert)
            throws IOException, CertificateException {
        return (X509Certificate)certFac.generateCertificate(
                new ByteArrayInputStream(pemCert.getBytes()));
    }

    /**
     * Create a private key from its PEM-encoded PKCS#8 representation.
     *
     * @param pemKey the private key in PEM-encoded PKCS#8 unencrypted format
     * @param algorithm the private key algorithm
     *
     * @return the PrivateKey extracted from the PKCS#8 encoding.
     *
     * @throws GeneralSecurityException if any errors take place during
     * decoding or parsing.
     */
    private static PrivateKey pem2Key(String pemKey, String algorithm)
            throws GeneralSecurityException {
        byte[] p8Der = Base64.getMimeDecoder().decode(pemKey);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(p8Der, algorithm);
        KeyFactory kf = KeyFactory.getInstance(algorithm);
        return kf.generatePrivate(spec);
    }

    /**
     * The BogusOcspExtension is an extension with random data in the
     * extension value field.  It is used in this test to expand the size
     * of the OCSP request so it crosses the boundary that forces an HTTP
     * POST operation instead of a GET.
     */
    private static class BogusExtension implements Extension {
        private final ObjectIdentifier oid;
        private final boolean critical;
        private final byte[] data;

        public BogusExtension(String oidStr, boolean isCrit, int size)
                throws IOException {
            // For this test we don't need anything larger than 10K
            if (size > 0 && size <= 10240) {
                data = new byte[size];
            } else {
                throw new IllegalArgumentException(
                        "Size must be 0 < X <= 10240");
            }
            oid = ObjectIdentifier.of(oidStr);
            SecureRandom sr = new SecureRandom();
            sr.nextBytes(data);
            critical = isCrit;
        }

        @Override
        public String getId() {
            return oid.toString();
        }

        @Override
        public boolean isCritical() {
            return critical;
        }

        @Override
        public byte[] getValue() {
            return data.clone();
        }

        @Override
        public void encode(OutputStream out) throws IOException {
            Objects.requireNonNull(out, "Non-null OutputStream required");

            DerOutputStream dos1 = new DerOutputStream();
            DerOutputStream dos2 = new DerOutputStream();

            dos1.putOID(oid);
            if (critical) {
                dos1.putBoolean(critical);
            }
            dos1.putOctetString(data);

            dos2.write(DerValue.tag_Sequence, dos1);
            out.write(dos2.toByteArray());
        }
    }
}
