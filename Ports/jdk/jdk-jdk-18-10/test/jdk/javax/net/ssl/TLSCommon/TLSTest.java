/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

/*
 * @test
 * @bug 8205111
 * @summary Test TLS with different types of supported keys.
 * @run main/othervm TLSTest TLSv1.3 rsa_pkcs1_sha1 TLS_AES_128_GCM_SHA256
 * @run main/othervm TLSTest TLSv1.3 rsa_pkcs1_sha256 TLS_AES_128_GCM_SHA256
 * @run main/othervm TLSTest TLSv1.3 rsa_pkcs1_sha384 TLS_AES_128_GCM_SHA256
 * @run main/othervm TLSTest TLSv1.3 rsa_pkcs1_sha512 TLS_AES_128_GCM_SHA256
 * @run main/othervm TLSTest TLSv1.3 ec_rsa_pkcs1_sha256 TLS_AES_128_GCM_SHA256
 * @run main/othervm TLSTest TLSv1.3 ecdsa_sha1 TLS_AES_128_GCM_SHA256
 * @run main/othervm TLSTest TLSv1.3 ecdsa_secp384r1_sha384
 *      TLS_AES_128_GCM_SHA256
 * @run main/othervm TLSTest TLSv1.3 ecdsa_secp521r1_sha512
 *      TLS_AES_128_GCM_SHA256
 * @run main/othervm TLSTest TLSv1.3 rsa_pss_rsae_sha256 TLS_AES_128_GCM_SHA256
 * @run main/othervm TLSTest TLSv1.3 rsa_pss_rsae_sha384 TLS_AES_128_GCM_SHA256
 * @run main/othervm TLSTest TLSv1.3 rsa_pss_rsae_sha512 TLS_AES_128_GCM_SHA256
 * @run main/othervm TLSTest TLSv1.3 rsa_pss_pss_sha256 TLS_AES_128_GCM_SHA256
 * @run main/othervm TLSTest TLSv1.3 rsa_pss_pss_sha384 TLS_AES_128_GCM_SHA256
 * @run main/othervm TLSTest TLSv1.3 rsa_pss_pss_sha512 TLS_AES_128_GCM_SHA256
 *
 * @run main/othervm TLSTest TLSv1.3 rsa_pkcs1_sha1 TLS_AES_256_GCM_SHA384
 * @run main/othervm TLSTest TLSv1.3 rsa_pkcs1_sha256 TLS_AES_256_GCM_SHA384
 * @run main/othervm TLSTest TLSv1.3 rsa_pkcs1_sha384 TLS_AES_256_GCM_SHA384
 * @run main/othervm TLSTest TLSv1.3 rsa_pkcs1_sha512 TLS_AES_256_GCM_SHA384
 * @run main/othervm TLSTest TLSv1.3 ec_rsa_pkcs1_sha256 TLS_AES_256_GCM_SHA384
 * @run main/othervm TLSTest TLSv1.3 ecdsa_sha1 TLS_AES_256_GCM_SHA384
 * @run main/othervm TLSTest TLSv1.3 ecdsa_secp384r1_sha384
 *      TLS_AES_256_GCM_SHA384
 * @run main/othervm TLSTest TLSv1.3 ecdsa_secp521r1_sha512
 *      TLS_AES_256_GCM_SHA384
 * @run main/othervm TLSTest TLSv1.3 rsa_pss_rsae_sha256 TLS_AES_256_GCM_SHA384
 * @run main/othervm TLSTest TLSv1.3 rsa_pss_rsae_sha384 TLS_AES_256_GCM_SHA384
 * @run main/othervm TLSTest TLSv1.3 rsa_pss_rsae_sha512 TLS_AES_256_GCM_SHA384
 * @run main/othervm TLSTest TLSv1.3 rsa_pss_pss_sha256 TLS_AES_256_GCM_SHA384
 * @run main/othervm TLSTest TLSv1.3 rsa_pss_pss_sha384 TLS_AES_256_GCM_SHA384
 * @run main/othervm TLSTest TLSv1.3 rsa_pss_pss_sha512 TLS_AES_256_GCM_SHA384
 *
 * @run main/othervm TLSTest TLSv1.2 rsa_pkcs1_sha1 TLS_RSA_WITH_AES_128_CBC_SHA
 * @run main/othervm TLSTest TLSv1.2 rsa_pkcs1_sha256
 *      TLS_RSA_WITH_AES_128_CBC_SHA
 * @run main/othervm TLSTest TLSv1.2 rsa_pkcs1_sha384
 *      TLS_RSA_WITH_AES_256_GCM_SHA384
 * @run main/othervm TLSTest TLSv1.2 rsa_pkcs1_sha512
 *      TLS_RSA_WITH_AES_128_GCM_SHA256
 * @run main/othervm TLSTest TLSv1.2 ec_rsa_pkcs1_sha256
 *      TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256
 * @run main/othervm TLSTest TLSv1.2 ecdsa_sha1
 *      TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384
 * @run main/othervm TLSTest TLSv1.2 ecdsa_secp384r1_sha384
 *      TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384
 * @run main/othervm TLSTest TLSv1.2 ecdsa_secp521r1_sha512
 *      TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA
 * @run main/othervm TLSTest TLSv1.2 rsa_pss_rsae_sha256
 *      TLS_RSA_WITH_AES_256_CBC_SHA256
 * @run main/othervm TLSTest TLSv1.2 rsa_pss_rsae_sha384
 *      TLS_RSA_WITH_AES_256_CBC_SHA
 * @run main/othervm TLSTest TLSv1.2 rsa_pss_rsae_sha512
 *      TLS_RSA_WITH_AES_128_CBC_SHA256
 * @run main/othervm TLSTest TLSv1.2 rsa_pss_pss_sha256
 *      TLS_DHE_RSA_WITH_AES_256_CBC_SHA256
 * @run main/othervm TLSTest TLSv1.2 rsa_pss_pss_sha384
 *      TLS_DHE_RSA_WITH_AES_256_GCM_SHA384
 * @run main/othervm TLSTest TLSv1.2 rsa_pss_pss_sha512
 *      TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256
 *
 * @run main/othervm TLSTest TLSv1.1 rsa_pkcs1_sha1 TLS_RSA_WITH_AES_128_CBC_SHA
 * @run main/othervm TLSTest TLSv1.1 rsa_pkcs1_sha256
 *      TLS_RSA_WITH_AES_256_CBC_SHA
 * @run main/othervm TLSTest TLSv1.1 rsa_pkcs1_sha384
 *      TLS_RSA_WITH_AES_128_CBC_SHA
 * @run main/othervm TLSTest TLSv1.1 rsa_pkcs1_sha512
 *      TLS_RSA_WITH_AES_256_CBC_SHA
 * @run main/othervm TLSTest TLSv1.1 rsa_pss_rsae_sha256
 *      TLS_RSA_WITH_AES_128_CBC_SHA
 * @run main/othervm TLSTest TLSv1.1 rsa_pss_rsae_sha384
 *      TLS_RSA_WITH_AES_256_CBC_SHA
 * @run main/othervm TLSTest TLSv1.1 rsa_pss_rsae_sha512
 *      TLS_RSA_WITH_AES_128_CBC_SHA
 *
 * @run main/othervm TLSTest TLSv1 rsa_pkcs1_sha1 TLS_RSA_WITH_AES_128_CBC_SHA
 * @run main/othervm TLSTest TLSv1 rsa_pkcs1_sha256 TLS_RSA_WITH_AES_256_CBC_SHA
 * @run main/othervm TLSTest TLSv1 rsa_pkcs1_sha384 TLS_RSA_WITH_AES_128_CBC_SHA
 * @run main/othervm TLSTest TLSv1 rsa_pkcs1_sha512 TLS_RSA_WITH_AES_256_CBC_SHA
 * @run main/othervm TLSTest TLSv1 rsa_pss_rsae_sha256
 *      TLS_RSA_WITH_AES_128_CBC_SHA
 * @run main/othervm TLSTest TLSv1 rsa_pss_rsae_sha384
 *      TLS_RSA_WITH_AES_256_CBC_SHA
 * @run main/othervm TLSTest TLSv1 rsa_pss_rsae_sha512
 *      TLS_RSA_WITH_AES_128_CBC_SHA
 */
public class TLSTest {

    private volatile static boolean clientRenegoReady = false;

    public static void main(String[] args) throws Exception {

        final String tlsProtocol = args[0];
        final KeyType keyType = KeyType.valueOf(args[1]);
        final String cipher = args[2];
        Security.setProperty("jdk.tls.disabledAlgorithms", "");
        CountDownLatch serverReady = new CountDownLatch(1);
        Server server = new Server(tlsProtocol, keyType, cipher, serverReady);
        server.start();

        // Wait till server is ready to accept connection.
        serverReady.await();
        new Client(tlsProtocol, keyType, cipher, server.port).doClientSide();
        if (server.serverExc != null) {
            throw new RuntimeException(server.serverExc);
        }
    }

    public static class Server implements Runnable {

        private volatile int port = 0;
        private final String tlsProtocol;
        private final KeyType keyType;
        private final String cipher;
        private final CountDownLatch latch;
        private volatile Exception serverExc;

        public Server(String tlsProtocol, KeyType keyType, String cipher,
                CountDownLatch latch) {
            this.tlsProtocol = tlsProtocol;
            this.keyType = keyType;
            this.cipher = cipher;
            this.latch = latch;
        }

        public void start() {

            ExecutorService executor = null;
            try {
                executor = Executors.newCachedThreadPool(new ThreadFactory() {
                    @Override
                    public Thread newThread(Runnable r) {
                        Thread t = Executors.defaultThreadFactory()
                                .newThread(r);
                        t.setDaemon(true);
                        return t;
                    }
                });
                executor.execute(this);
            } finally {
                if (executor != null) {
                    executor.shutdown();
                }
            }
        }

        /*
         * Define the server side operation.
         */
        void doServerSide() throws Exception {

            SSLContext ctx = getSSLContext(tlsProtocol,
                    keyType.getTrustedCert(), keyType.getEndCert(),
                    keyType.getPrivateKey(), keyType.getKeyType());
            SSLServerSocketFactory sslssf = ctx.getServerSocketFactory();
            InetSocketAddress socketAddress =
                    new InetSocketAddress(InetAddress.getLoopbackAddress(), port);
            SSLServerSocket sslServerSocket
                    = (SSLServerSocket) sslssf.createServerSocket();
            sslServerSocket.bind(socketAddress);
            port = sslServerSocket.getLocalPort();
            System.out.println("Server listining on port: " + port);
            // specify the enabled server cipher suites
            sslServerSocket.setEnabledCipherSuites(new String[]{this.cipher});
            sslServerSocket.setEnabledProtocols(new String[]{tlsProtocol});
            sslServerSocket.setSoTimeout(25000);
            /*
             * Signal Client, the server is ready to accept client request.
             */
            latch.countDown();
            try (SSLSocket sslSocket = (SSLSocket) sslServerSocket.accept()) {
                try (InputStream sslIS = sslSocket.getInputStream();
                        OutputStream sslOS = sslSocket.getOutputStream();) {
                    sslIS.read();
                    sslOS.write(85);
                    sslOS.flush();
                    while (!clientRenegoReady) {
                        System.out.println("Waiting for ClientHello");
                        TimeUnit.MILLISECONDS.sleep(50);
                    }
                    for (int i = 0; i < 4; i++) {
                        sslIS.read();
                        sslOS.write(89);
                        sslOS.flush();
                        TimeUnit.MILLISECONDS.sleep(50);   // wait for a while
                    }
                }
            } finally {
                sslServerSocket.close();
            }
        }

        @Override
        public void run() {
            try {
                doServerSide();
            } catch (Exception e) {
                // Print the exception for debug purpose.
                e.printStackTrace(System.out);
                serverExc = e;
            }
        }
    }

    /*
     * Define the client side of the test.
     */
    public static class Client {

        private final int serverPort;
        private final String tlsProtocol;
        private final KeyType keyType;
        private final String cipher;

        public Client(String tlsProtocol, KeyType keyType, String cipher,
                int serverPort) {
            this.tlsProtocol = tlsProtocol;
            this.keyType = keyType;
            this.cipher = cipher;
            this.serverPort = serverPort;
        }

        void doClientSide() throws Exception {

            SSLContext ctx = getSSLContext(this.tlsProtocol,
                    keyType.getTrustedCert(), null, null, keyType.getKeyType());
            SSLSocketFactory sslsf = ctx.getSocketFactory();
            try (SSLSocket sslSocket
                    = (SSLSocket) sslsf.createSocket("localhost", serverPort)) {
                // Specify the client cipher suites
                sslSocket.setEnabledCipherSuites(new String[]{this.cipher});
                sslSocket.setEnabledProtocols(new String[]{this.tlsProtocol});
                InputStream sslIS = sslSocket.getInputStream();
                OutputStream sslOS = sslSocket.getOutputStream();
                try {
                    sslOS.write(86);
                    sslOS.flush();
                    sslIS.read();
                    // Re-handshake for key-update and session resumption.
                    sslSocket.startHandshake();
                    System.out.println("Client: Re-Handshake completed.");
                } finally {
                    clientRenegoReady = true;
                }
                try {
                    for (int i = 0; i < 4; i++) {
                        sslOS.write(88);
                        sslOS.flush();
                        sslIS.read();
                        TimeUnit.MILLISECONDS.sleep(50);  // wait for a while
                    }
                } finally {
                    sslIS.close();
                    sslOS.close();
                }
            }
        }
    }

    // get the ssl context
    protected static SSLContext getSSLContext(String tlsProtocol,
            String trustedCertStr, String keyCertStr,
            String privateKey, String keyType) throws Exception {

        // Generate certificate from cert string
        CertificateFactory cf = CertificateFactory.getInstance("X.509");

        // Create a key store
        KeyStore ts = KeyStore.getInstance("PKCS12");
        KeyStore ks = KeyStore.getInstance("PKCS12");
        ts.load(null, null);
        ks.load(null, null);
        char passphrase[] = "passphrase".toCharArray();

        // Import the trusted cert
        ts.setCertificateEntry("trusted-cert-" + keyType,
                cf.generateCertificate(new ByteArrayInputStream(
                        trustedCertStr.getBytes())));

        boolean hasKeyMaterials = keyCertStr != null && privateKey != null;
        if (hasKeyMaterials) {

            // Generate the private key.
            PKCS8EncodedKeySpec priKeySpec = new PKCS8EncodedKeySpec(
                    Base64.getMimeDecoder().decode(privateKey));
            KeyFactory kf = KeyFactory.getInstance(keyType);
            PrivateKey priKey = kf.generatePrivate(priKeySpec);

            // Generate certificate chain
            Certificate keyCert = cf.generateCertificate(
                    new ByteArrayInputStream(keyCertStr.getBytes()));
            Certificate[] chain = new Certificate[]{keyCert};

            // Import the key entry.
            ks.setKeyEntry("cert-" + keyType, priKey, passphrase, chain);
        }

        // Create SSL context
        TrustManagerFactory tmf = TrustManagerFactory.getInstance("PKIX");
        tmf.init(ts);

        SSLContext context = SSLContext.getInstance(tlsProtocol);
        if (hasKeyMaterials) {
            KeyManagerFactory kmf = KeyManagerFactory.getInstance("NewSunX509");
            kmf.init(ks, passphrase);
            context.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
        } else {
            context.init(null, tmf.getTrustManagers(), null);
        }
        return context;
    }

    enum KeyType {

        ec_rsa_pkcs1_sha256(
                "EC",
                /**
                 * Signature Algorithm: sha256WithRSAEncryption
                 * Issuer: CN = root
                 * Validity Not Before: Jun 5 07:20:59 2018 GMT
                 * Not After : May 31 07:20:59 2038 GMT
                 * Subject: CN = root
                 * Public Key Algorithm: rsaEncryption
                 */
                "-----BEGIN CERTIFICATE-----\n"
                + "MIIC/DCCAeSgAwIBAgIUDJ+blgr9+e9ezH0Cj/NZ1Skd8GQwDQYJKoZIhvcNAQEL\n"
                + "BQAwDzENMAsGA1UEAwwEcm9vdDAeFw0xODA2MDUwNzIwNTlaFw0zODA1MzEwNzIw\n"
                + "NTlaMA8xDTALBgNVBAMMBHJvb3QwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEK\n"
                + "AoIBAQDV48Mrbx+kWa1h/M2+Unr4AZM2raONrmwixJmosoiy+wOfjGfcwZyEyvNm\n"
                + "FzVor3klBJLAam/4ndgyytFmCvaUT9oLm9N99dViSL2Tn388bWFFmMngsmGlFLMD\n"
                + "fTsuBvxsYedyFUAgnpqLQCBiGrX930LF4bexegiBUftEK6lTbuq98vKW6bHT+B+o\n"
                + "jkd23zYC7yBo9hgSuoDpI4s8lGh6vwAiijybaVve8t/idWHXWqk9mLJ//j5rj39F\n"
                + "PYjDg7LF8xFV7nP7q/6KK0XBQdUMpmrShC/hE4BoUPks0dOEjAh+nN4O1J/4xlYX\n"
                + "O5oaPVtvi3LJdjQvTQA7mEyM02ClAgMBAAGjUDBOMB0GA1UdDgQWBBTwyBBY7sOc\n"
                + "sEdvYf7oRvf7MIIsjTAfBgNVHSMEGDAWgBTwyBBY7sOcsEdvYf7oRvf7MIIsjTAM\n"
                + "BgNVHRMEBTADAQH/MA0GCSqGSIb3DQEBCwUAA4IBAQC6CnR4XWQ4uO2H5Ngt+4Yk\n"
                + "V82+oBlxa9SMK/tt67YC7wsxALsYqQ8oj3sGhH6mlNv2gDQ/OOC/HcdI/N72s+/n\n"
                + "HnWq7vInx2M5P0QCRXjTUxx4+OPdH11zbsK5ZkE0SCOwlzlkcqU1fkwbr+vovgcP\n"
                + "HgYL+3eGlNcz6+XwtVfySDbRkLYGbLlG1dH5WqR9+Z7glRgl6D+ZdOxEAwhbCAu1\n"
                + "ADGGckA4107gVrR2r8YvzS/cO9Q97XWEXlfeHs5t7TQSJdNg6Gep1jLpFEQ98h/c\n"
                + "y4VBmOqhZ4vJ+/k16IW83XV8NcroIrqyfVJFRxVTCpitj4kDecqd9XHRE2/Xf5bb\n"
                + "-----END CERTIFICATE-----\n",
                /**
                 * Signature Algorithm: sha256WithRSAEncryption
                 * Issuer: CN = root
                 * Validity Not Before: Jun 5 07:20:59 2018 GMT
                 * Not After : May 31 07:20:59 2038 GMT
                 * Subject: CN = localhost
                 * ASN1 OID: prime256v1
                 * Public Key Algorithm: id-ecPublicKey
                 */
                "-----BEGIN CERTIFICATE-----\n"
                + "MIICwzCCAaugAwIBAgIUHY0HRPAMz4oLuNzl/b39FHi6AgwwDQYJKoZIhvcNAQEL\n"
                + "BQAwDzENMAsGA1UEAwwEcm9vdDAeFw0xODA2MDUwNzIwNTlaFw0zODA1MzEwNzIw\n"
                + "NTlaMBQxEjAQBgNVBAMMCWxvY2FsaG9zdDBZMBMGByqGSM49AgEGCCqGSM49AwEH\n"
                + "A0IABF1zheAdyEUerLNUqXHw2WmXnMVJnKSMTeq+bk9WsZGBOZzJcEtyr1887JAR\n"
                + "urn0uJ7J3YLUNlMuHaSWZ8hExGujgdwwgdkwCQYDVR0TBAIwADALBgNVHQ8EBAMC\n"
                + "A/gwEwYDVR0lBAwwCgYIKwYBBQUHAwEwHQYDVR0OBBYEFMzV+8JMWSrdIJ+iZE9K\n"
                + "4zn+roK6MEoGA1UdIwRDMEGAFPDIEFjuw5ywR29h/uhG9/swgiyNoROkETAPMQ0w\n"
                + "CwYDVQQDDARyb290ghQMn5uWCv35717MfQKP81nVKR3wZDARBglghkgBhvhCAQEE\n"
                + "BAMCBkAwLAYJYIZIAYb4QgENBB8WHU9wZW5TU0wgR2VuZXJhdGVkIENlcnRpZmlj\n"
                + "YXRlMA0GCSqGSIb3DQEBCwUAA4IBAQCvyScCVQVTG3d3yLcLT/q6G1I8N/4JrvzZ\n"
                + "126BHoh8Oej4pbqn05SPdT4VH+J4UbTA8uHH9CLrAQv3WAU+P+tjXf61IRYNCm73\n"
                + "A6K7ZvkpZpnvyT3ynMpG509OZbKxQrJWvyN22MTApi7Y8+s3+UAwUG4SZwlEHLn+\n"
                + "sGASjfYouH4BRbymeNmuoHXWHO/P8O52cylElyUEHcwJx17IqJRNcwMb2aexPe+h\n"
                + "P3HcVS6fxFW1I02cq62KEfexRTvVNijXU8vaYDC0aP0M+fMN/xc/HPJiUyRNCKOC\n"
                + "Q8B6w2/GDQQeVbxoO0CLuHuOodA+oJIw4bX0y4XvTs76HK1R/nue\n"
                + "-----END CERTIFICATE-----\n",
                //
                // Private key.
                //
                "MIGHAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBG0wawIBAQQgVHQp1EG3PgASz7Nu\n"
                + "uv9dvFLxsr3qfgC6CgZU4xorLbChRANCAARdc4XgHchFHqyzVKlx8Nlpl5zFSZyk\n"
                + "jE3qvm5PVrGRgTmcyXBLcq9fPOyQEbq59Lieyd2C1DZTLh2klmfIRMRr"
        ),
        ecdsa_sha1(
                "EC",
                /**
                 * Signature Algorithm: ecdsa-with-SHA1
                 * Issuer: CN = localhost
                 * Validity Not Before: Jun 4 15:20:45 2018 GMT
                 * Not After : May 30 15:20:45 2038 GMT
                 * Subject: CN = localhost
                 * Public Key Algorithm: id-ecPublicKey
                 */
                "-----BEGIN CERTIFICATE-----\n"
                + "MIIBdzCCAR+gAwIBAgIUO79CpzonO37fqCHN1VHS+aa5t5owCQYHKoZIzj0EATAU\n"
                + "MRIwEAYDVQQDDAlsb2NhbGhvc3QwHhcNMTgwNjA0MTUyMDQ1WhcNMzgwNTMwMTUy\n"
                + "MDQ1WjAUMRIwEAYDVQQDDAlsb2NhbGhvc3QwWTATBgcqhkjOPQIBBggqhkjOPQMB\n"
                + "BwNCAAR6LMO6lBGdmpo87XTjtA2vsXvq1kd8ktaIGEdCrA8BKk0A30LW8SY5Be29\n"
                + "ScYu8d+IjQ3X/fpblrVh/64pOgQzo1AwTjAdBgNVHQ4EFgQU3RhUvLzu/b6sNegl\n"
                + "/5TPncFFh4MwHwYDVR0jBBgwFoAU3RhUvLzu/b6sNegl/5TPncFFh4MwDAYDVR0T\n"
                + "BAUwAwEB/zAJBgcqhkjOPQQBA0cAMEQCIEle4IWFybL1xKVmFCNnR8bK1l5LzqAj\n"
                + "YBdXK+LBJDliAiBFKkkOaZsXZir09t1tgPNneIgYMeXCQAJ1mQ7rQRiKPg==\n"
                + "-----END CERTIFICATE-----\n",
                //
                // Private key.
                //
                "MIGHAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBG0wawIBAQQgyJJNI8eqYVKcCshG\n"
                + "t89mrRZ1jMeD8fAbgijAG7WfgtGhRANCAAR6LMO6lBGdmpo87XTjtA2vsXvq1kd8\n"
                + "ktaIGEdCrA8BKk0A30LW8SY5Be29ScYu8d+IjQ3X/fpblrVh/64pOgQz"
        ),
        rsa_pss_pss_sha256(
                "RSASSA-PSS",
                /**
                 * Signature Algorithm: rsassaPss
                 * Issuer: CN = localhost
                 * Validity Not Before: Jun 6 07:11:00 2018 GMT
                 * Not After : Jun 1 07:11:00 2038 GMT
                 * Subject: CN = localhost
                 * Public Key Algorithm: rsassaPss
                 */
                "-----BEGIN CERTIFICATE-----\n"
                + "MIIDZjCCAh2gAwIBAgIUHxwPs3eAgJ057nJwiLgWZWeNqdgwPgYJKoZIhvcNAQEK\n"
                + "MDGgDTALBglghkgBZQMEAgGhGjAYBgkqhkiG9w0BAQgwCwYJYIZIAWUDBAIBogQC\n"
                + "AgDeMBQxEjAQBgNVBAMMCWxvY2FsaG9zdDAeFw0xODA2MDYwNzExMDBaFw0zODA2\n"
                + "MDEwNzExMDBaMBQxEjAQBgNVBAMMCWxvY2FsaG9zdDCCASAwCwYJKoZIhvcNAQEK\n"
                + "A4IBDwAwggEKAoIBAQCl8r4Qrg27BYUO/1Va2Ix8QPGzN/lvzmKvP5Ff26ovNW4v\n"
                + "RUx68HzAhhiWtcl+PwLSbJqJreEkTlle7PnRAypby3fO7ZAK0Y3YiHquaBg7d+7Y\n"
                + "FhhHwv8gG0lZcyA0BkXFJHqdq76qar0xHC6DVezXm0K3mcceymGtFR9BzWmAj+7D\n"
                + "YsSwvtTQ7WNoQmf0cdDMSM71IwaTwIwvT2wzX1vv5hcdDyXdr64WFqWSA9sNJ2K6\n"
                + "arxaaU1klwKSgDokF6njafWQ4UxdR67d5W1MYoiioDs2Yy3utsMpO2OUzZVBZNdT\n"
                + "gkr1jsJhIurpz/5K51lwJIRQBezEFSb+60AFVoMJAgMBAAGjUDBOMB0GA1UdDgQW\n"
                + "BBQfFit5ilWJmZgCX4QY0HsaI9iIDDAfBgNVHSMEGDAWgBQfFit5ilWJmZgCX4QY\n"
                + "0HsaI9iIDDAMBgNVHRMEBTADAQH/MD4GCSqGSIb3DQEBCjAxoA0wCwYJYIZIAWUD\n"
                + "BAIBoRowGAYJKoZIhvcNAQEIMAsGCWCGSAFlAwQCAaIEAgIA3gOCAQEAa4yUQ3gh\n"
                + "d1YWPdEa1sv2hdkhtenw6m5yxbmaQl2+nIKSpk4RfpXC7K1EYwBF8TdfFbD8hGGh\n"
                + "5n81BT0/dn1R9SRGCv7KTxx4lfQt31frlsw/tVciwyXQtcUZ6DqfnLP0/aRVLNgx\n"
                + "zaP542JUHFYLTC3EGz2zUgv70ZUTlIsPG3/p8YO1iXdnYGQyzOuQPUBpI7nS7UtR\n"
                + "Ug8VE9ACpBxxI3qChMahFZGHlXCCSjSmxpQa6UO4SQl8q5tPNnqdzWwvAW8qkCy4\n"
                + "6barRQ4sMcGayhHh/uSTx7bcl0FMJpcI1ygbw7/Pc03zKtw0gMTBMns7q4yXjb/u\n"
                + "ef47nW0t+LRAAg==\n"
                + "-----END CERTIFICATE-----\n",
                //
                // Private key.
                //
                "MIIEuwIBADALBgkqhkiG9w0BAQoEggSnMIIEowIBAAKCAQEApfK+EK4NuwWFDv9V\n"
                + "WtiMfEDxszf5b85irz+RX9uqLzVuL0VMevB8wIYYlrXJfj8C0myaia3hJE5ZXuz5\n"
                + "0QMqW8t3zu2QCtGN2Ih6rmgYO3fu2BYYR8L/IBtJWXMgNAZFxSR6nau+qmq9MRwu\n"
                + "g1Xs15tCt5nHHsphrRUfQc1pgI/uw2LEsL7U0O1jaEJn9HHQzEjO9SMGk8CML09s\n"
                + "M19b7+YXHQ8l3a+uFhalkgPbDSdiumq8WmlNZJcCkoA6JBep42n1kOFMXUeu3eVt\n"
                + "TGKIoqA7NmMt7rbDKTtjlM2VQWTXU4JK9Y7CYSLq6c/+SudZcCSEUAXsxBUm/utA\n"
                + "BVaDCQIDAQABAoIBAAc4vRS0vlw5LUUtz2UYr2Ro3xvRf8Vh0eGWfpkRUiKjzJu6\n"
                + "BE4FUSh/rWpBlvcrfs/xcfgz3OxbjIAZB/YUkS9Vd21F4VLXM7kMl2onlYZg/b/h\n"
                + "lkTpM3kONu7xl6Er9LVTlRJveuinpHwSoeONRbVMSGb9BjFM1VtW4/lVGxZBG05D\n"
                + "y9i/o4vCZqULn9cAumOwicKuCyTcS58XcMJ+puSPfRA71PYLxqFkASAoJsUwCXpo\n"
                + "gs39lLsIFgrfO8mBO1ux/SE+QaRc+9XqFSHHKD1XqF/9zSYBgWjE910EcpdYEdZx\n"
                + "GEkwea7Fn4brO5OpIrHY/45naqbUOBzv6gufMAECgYEAz7PHCdcrQvmOb8EiNbQH\n"
                + "uvSimwObWJFeN1ykp6mfRbSnkXw7p8+M4Tc8HFi8QLpoq63Ev2AwoaQCQvHbFC2Y\n"
                + "1Cz0EkC0aOp+tZP7U2AUBdkcDesZAJQTad0zV6KesyIUXdxZXDG8JJ1XSNWfTJV4\n"
                + "QD+BjLZ0jiAyCIfVYvWQqYkCgYEAzIln1nKTixLMPr5CldSmR7ZarEtPJU+hHwVg\n"
                + "dV/Lc6d2Yy9JgunOXRo4BXB1TEo8JFbK3HBQH6tS8li4qDr7WK5wyYfh8qb4WZyu\n"
                + "lc562f2WVYntcN8/Ojb+Vyrt7lk9sq/8KoVHxEAWd6mqL9VTPYuAu1Vw9fTGIZfB\n"
                + "lDeELYECgYAvdzU4UXzofGGJtohb332YwwlaBZP9xJLUcg6K5l+orWVSASMc8XiP\n"
                + "i3DoRXsYC8GZ4kdBOPlEJ1gA9oaLcPQpIPDSLwlLpLM6Scw4vI822uvnXl/DWxOo\n"
                + "sM1n7Jj59QLUhGPDhvYpI+/rjC4wcUQe4qR3hMbUKBVnD6u7RsU9iQKBgQCQ17VK\n"
                + "7bSCRfuRaxaoGADww7gOTv5rQ6qr1xjpxb7D1hFGR9Rc+smCsPB/GZZXQjK44SWj\n"
                + "WX3ED4Ubzaxmpe4cbNu+O5XMSmWQwB36RFBHUwdE5/nXdqDFzu/qNqJrqZLBmVKP\n"
                + "ofaiiWffsaytVvotmT6+atElvAMbAua42V+nAQKBgHtIn3mYMHLriYGhQzpkFEA2\n"
                + "8YcAMlKppueOMAKVy8nLu2r3MidmLAhMiKJQKG45I3Yg0/t/25tXLiOPJlwrOebh\n"
                + "xQqUBI/JUOIpGAEnr48jhOXnCS+i+z294G5U/RgjXrlR4bCPvrtCmwzWwe0h79w2\n"
                + "Q2hO5ZTW6UD9CVA85whf"
        ),
        rsa_pss_rsae_sha256(
                "RSA",
                /**
                 * Signature Algorithm: rsassaPss
                 * Issuer: CN = root
                 * Validity Not Before: Jun 6 07:11:39 2018 GMT
                 * Not After : Jun 1 07:11:39 2038 GMT
                 * Subject: CN = root
                 * Public Key Algorithm: rsassaPss
                 */
                "-----BEGIN CERTIFICATE-----\n"
                + "MIIDXDCCAhOgAwIBAgIUM883yXaRA3QIV+WMuFpPscABr3IwPgYJKoZIhvcNAQEK\n"
                + "MDGgDTALBglghkgBZQMEAgGhGjAYBgkqhkiG9w0BAQgwCwYJYIZIAWUDBAIBogQC\n"
                + "AgDeMA8xDTALBgNVBAMMBHJvb3QwHhcNMTgwNjA2MDcxMTM5WhcNMzgwNjAxMDcx\n"
                + "MTM5WjAPMQ0wCwYDVQQDDARyb290MIIBIDALBgkqhkiG9w0BAQoDggEPADCCAQoC\n"
                + "ggEBAL5mCEWQRETgkJpn/RdyQZed7gXJEBrlsF0VcTs6RbEHx4clDnhiySPrqX2p\n"
                + "KgfpMtxt6wKV+qY6+mSKyhDlUnVVgNdX0IgyXXXl5zcCfVRkbqEwRoon0HRLilaP\n"
                + "NAeLhQDOtR4Kuw+tGaLMMUncdVoIlgR4TCEgWVkiX+Xri7/A2t8vnBgE8xxp+Xbl\n"
                + "r/gYBS0K68zyGCSEQY0DltiPkWgvLWYiFAuDYobJZhVcDDNbgIMdzS9KfDX8Pm7F\n"
                + "OC4Uu5Us4QemADFX/Iqf/jURJjJJ1lJpH9ue2I3tpJhVMg7lumfrc+Mf0+85St7Y\n"
                + "smxURgAd2Qv9ecpMtk3ROYci2UkCAwEAAaNQME4wHQYDVR0OBBYEFINgLu8Yw9nh\n"
                + "J5xEH0/w9NOVEOFNMB8GA1UdIwQYMBaAFINgLu8Yw9nhJ5xEH0/w9NOVEOFNMAwG\n"
                + "A1UdEwQFMAMBAf8wPgYJKoZIhvcNAQEKMDGgDTALBglghkgBZQMEAgGhGjAYBgkq\n"
                + "hkiG9w0BAQgwCwYJYIZIAWUDBAIBogQCAgDeA4IBAQBjuUjrtllwaE1ZB7+nCiDT\n"
                + "0o4SoX+1klU0M45L/IBsqIJI0uyBdwToPFXaswK0JrC3YuoOGjBfWlDGtmcoG0L8\n"
                + "V3nlWh0QO2/XQYjpT8SMRLcP9xRpY/rap85LqTkPlGhk3h0Z0LZTuK9KGznaHB/X\n"
                + "RfIRAerYwkRV6F4YbpJxLkZ/1udutQcByKXnGaosFZSZVyfy/Xn0+xWiBkuGv6hC\n"
                + "pZh0//f+9cjUiWChx0ROa+3DmDc5mzFIxC0VGMGZWSekgFAyi7eOu09DB2BDg2O/\n"
                + "C3MPBzynuw9E1a4NhEqNx+Cm0gJj5ZAUAAE1/aR8103fND3CA1SqiTgDVJh9Xg8i\n"
                + "-----END CERTIFICATE-----\n",
                /**
                 * Signature Algorithm: rsassaPss
                 * Issuer: CN = root
                 * Validity Not Before: Jun 6 07:11:39 2018 GMT
                 * Not After : Jun 1 07:11:39 2038 GMT
                 * Subject: CN = localhost
                 * Public Key Algorithm: rsaEncryption
                 */
                "-----BEGIN CERTIFICATE-----\n"
                + "MIID8DCCAqegAwIBAgIUC+I1WgIEDAn5GlIUequsVbhnSr4wPgYJKoZIhvcNAQEK\n"
                + "MDGgDTALBglghkgBZQMEAgGhGjAYBgkqhkiG9w0BAQgwCwYJYIZIAWUDBAIBogQC\n"
                + "AgDeMA8xDTALBgNVBAMMBHJvb3QwHhcNMTgwNjA2MDcxMTM5WhcNMzgwNjAxMDcx\n"
                + "MTM5WjAUMRIwEAYDVQQDDAlsb2NhbGhvc3QwggEiMA0GCSqGSIb3DQEBAQUAA4IB\n"
                + "DwAwggEKAoIBAQDD8nVjgSWSwVmP6wHXl+8cjESonTvCqSU1xLiySoqOH+/u5kTc\n"
                + "g5uk7J9qr3sDpLLVmnB7lITrv3cxX7GufAC2lrWPhKdY2/BTpCGP4Twg/sC7Z2Mn\n"
                + "APNabmPh+BhpQA3PllULdnsV/aEKeP3dFF+piJmSDKwowLhDc0wdD1t15jDk812U\n"
                + "nNQugd465g0g6z57m3MFX1veUryaNqgoHncuVRjvXPm2HHKUYvIt28Od3w+LbOGe\n"
                + "U2ykiS/KC0QQMsW7JZzeFSoogkZry/rUz1MJVSA49QNSVOdmVuUvD9tX8q+Dv+lD\n"
                + "G20+9c6sz9qbzlJk4uOx39ES98Y5vAVA25C/AgMBAAGjgdwwgdkwCQYDVR0TBAIw\n"
                + "ADALBgNVHQ8EBAMCA/gwEwYDVR0lBAwwCgYIKwYBBQUHAwEwHQYDVR0OBBYEFHju\n"
                + "i01kwWAQXy8XCa7MFigVAz/pMEoGA1UdIwRDMEGAFINgLu8Yw9nhJ5xEH0/w9NOV\n"
                + "EOFNoROkETAPMQ0wCwYDVQQDDARyb290ghQzzzfJdpEDdAhX5Yy4Wk+xwAGvcjAR\n"
                + "BglghkgBhvhCAQEEBAMCBkAwLAYJYIZIAYb4QgENBB8WHU9wZW5TU0wgR2VuZXJh\n"
                + "dGVkIENlcnRpZmljYXRlMD4GCSqGSIb3DQEBCjAxoA0wCwYJYIZIAWUDBAIBoRow\n"
                + "GAYJKoZIhvcNAQEIMAsGCWCGSAFlAwQCAaIEAgIA3gOCAQEAsasJTqce1nlDSl+L\n"
                + "rHuKiuQhr2KFq9DIXrTe2TEPIKBIEQnMOX/fHtahOAvkdgQmv573Z63uzHWoWSie\n"
                + "V5+fHDOOL0vGQ1y3hklIqPnNs0cOORr0sed2p5ibwM1W3OBHRGqWtdYHf0o3sJnD\n"
                + "vHo1Vhxc6Zabv5Bf1pTT3GGL4cM66LRWJAoDOx4RiCZObBqDUhZ7z9ntJM+o8xtE\n"
                + "4uf08tqOESQ1hJSug9GApSX5QKu59BkPza4KTCjz6tagBKBF7x/CUbYcbjsWe8A6\n"
                + "TZAyfBFsdj3G20BL+o3+zCy6yBUB6Z/DzB1zx65roVt9BpF0reHHCA2/gwa9sKYh\n"
                + "qx2Knw==\n"
                + "-----END CERTIFICATE-----\n",
                //
                // Private key.
                //
                "MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQDD8nVjgSWSwVmP\n"
                + "6wHXl+8cjESonTvCqSU1xLiySoqOH+/u5kTcg5uk7J9qr3sDpLLVmnB7lITrv3cx\n"
                + "X7GufAC2lrWPhKdY2/BTpCGP4Twg/sC7Z2MnAPNabmPh+BhpQA3PllULdnsV/aEK\n"
                + "eP3dFF+piJmSDKwowLhDc0wdD1t15jDk812UnNQugd465g0g6z57m3MFX1veUrya\n"
                + "NqgoHncuVRjvXPm2HHKUYvIt28Od3w+LbOGeU2ykiS/KC0QQMsW7JZzeFSoogkZr\n"
                + "y/rUz1MJVSA49QNSVOdmVuUvD9tX8q+Dv+lDG20+9c6sz9qbzlJk4uOx39ES98Y5\n"
                + "vAVA25C/AgMBAAECggEAD8rgzy4/ev5+W20Tbu7d5K0hc32IrX24dAbw493CIQZN\n"
                + "7jE855Dr4HT5vD18aqTBjRmvayZjOCTsVFxkE64G2LB43aJRYoYHbpZR5ii/EeG2\n"
                + "YuRIR4J6GpW/Ex1Nyl3RPyPcotnfvbv9WLy7qS/VLfLGfdDrpzUxJK6MOeNc+bl5\n"
                + "qB1ulSCaFAM9shaEKW1jXHoHhl386dtduXtCOZNU2OTmSAqh5GHKTN0CuNF9b2Ys\n"
                + "3gkwVdxwle3EaY97mJVqoB3YCBZgRnT3oilJCRM7L4y6W4MVL0AmDlBrPtvsxRIS\n"
                + "obFPm6c0qLVoPVbw2Z2PO9XLJmsrEAsFoVxYe8lAgQKBgQDjQ3R61ksPrmq1TDl5\n"
                + "wK5FlfUfAmnnt0Sh1Sk8xSkAcEaRyTil0vKYM/flsHVD949rJm15uJ4jhKQd8+f+\n"
                + "GQDj//yCNi3b/JRcOZJ4zMSnB6u4r+YJ7C5CwiGPmAm668SCvoskq93yCZ3RixM6\n"
                + "59Tj7V7GQo+9gazkt1hGEcR0nwKBgQDcuUnUXshzubOftS6+nFvCO8F4JfmoHbxp\n"
                + "DO3VZAvuimdbxjscMtuYVWa097gzatApkfjL4ZlxuL2yxx5GDy9mW6ipJpM/NBvC\n"
                + "fvN23dzoKf1zJ9yh2jZIb4z7q87LwHiiQ7TZrOno8M8Szv7qs8K4W+/0IY6fk7Yz\n"
                + "l9AgHExP4QKBgQCyE9Q8wJf2hKwWvdC3p5Sm8BcvojuMFx4PVTFH7hHvxwp1B+B8\n"
                + "h2wSeX5BG+D3Tg+yXV0hKNm5aSEUW1+oyrzY62hYO835d9Rk47PzNjjNzQPw5tvx\n"
                + "YIDrTKTxmKffMQk9jcMIDCgQlRp17G04FhrKMuC1p8hsLSVl3oir9xYibwKBgEHz\n"
                + "Yyn/gCmD7TXlLyhpE8m/jRlXT3d6GxfQcyf1ktMdq7ByVKsiTxb/PYcJFZLXcYda\n"
                + "RFq29+BQ8O2ALX2FgAY3kPepvQl/imPdBuYXeLAuC7riyDvcNagDHL7+IPYkdmcV\n"
                + "j+4Sinm9qkHWc7ixKZdocRQjCriHrENSMy/FBNBBAoGAfi0ZGZxyIeik5qUBy+P+\n"
                + "+ce6n5/1kdczPTpzJae+eCPqg3VQuGz3NutZ2tdx1IMcYSeMfiB6xLMWSKSraxEi\n"
                + "2BCtdPcyUau8w12BXwn+hyK2u79OhHbexisrJUOVXE+yA8C/k0r0SrZHS0PHYZjj\n"
                + "xkWyr/6XyeGP/vX8WvfF2eM="
        ),
        rsa_pkcs1_sha1(
                "RSA",
                /**
                 * Signature Algorithm: sha1WithRSAEncryption
                 * Issuer: CN = localhost
                 * Validity Not Before: Jun 4 15:23:09 2018 GMT
                 * Not After : May 30 15:23:09 2038 GMT
                 * Subject: CN = localhost
                 * Public Key Algorithm: rsaEncryption
                 */
                "-----BEGIN CERTIFICATE-----\n"
                + "MIIDBjCCAe6gAwIBAgIUYYGwpeC0sJpk4SP0SYgAiN9Z1qswDQYJKoZIhvcNAQEF\n"
                + "BQAwFDESMBAGA1UEAwwJbG9jYWxob3N0MB4XDTE4MDYwNDE1MjMwOVoXDTM4MDUz\n"
                + "MDE1MjMwOVowFDESMBAGA1UEAwwJbG9jYWxob3N0MIIBIjANBgkqhkiG9w0BAQEF\n"
                + "AAOCAQ8AMIIBCgKCAQEApbeNHuHv5VuUteMxa5Ga4N2QDnmGk1pAGuhFVG7Roh/3\n"
                + "kU3gJzg0UHsvc0BUWPHDOoswYXiW3zzfa2TdUny/EeF3xxw10vbnV+M1iDnKwDEM\n"
                + "aCtihBTHCdRd3DBRkNbyDPgOBU4zmx5fZhLej6tC0Ez5V+yScPtUe0RCWEpMRZu4\n"
                + "f5bK//AknFcReEmbTuvxDD6BtpEj8pUJatWIuPPgQRbjzm37WL9ZsnwldLNAvPdx\n"
                + "DBNfmGhOrIwJJ6m/ihlJgiwoM4Ffpqa/I+gciDUX9xPogFi9f93ShL9FyGDQO28+\n"
                + "0HOgDi9O3KthJ17QCSOkG3leIWm/bqE4wQ3iMF3EzQIDAQABo1AwTjAdBgNVHQ4E\n"
                + "FgQU1t/aKj/JelP2U02UaK4ICGLigtwwHwYDVR0jBBgwFoAU1t/aKj/JelP2U02U\n"
                + "aK4ICGLigtwwDAYDVR0TBAUwAwEB/zANBgkqhkiG9w0BAQUFAAOCAQEAnVX/n4/A\n"
                + "0KMrjkbXYGSjECTXy15xIrMME71eD4HvCe3n5E5ONBfivPGsj9z+y73pQJm92F1v\n"
                + "Wi2Y1+819ObYwXTrI1b0zgV8WJ6pj+E14eNECM5npEH/QJMSVeJLWCuisQGTaGJH\n"
                + "TTemZfOTspmFQ9V/meOXjh7kgmF5plclnhDgwLe5Ryih665YOADxxgncP6ViQibI\n"
                + "dSTnGH4+Ikj6iQja/Xxb1uc7KYMqQsji7oUHp/NDwM6av1tbdt5TMSrwnT1T36D+\n"
                + "zc/Kua1BkI3RqfooZ1CIQc0lHr8Au7fG/HNDPBNPFuB/g0c7IE4l0bcjEIDJEtj+\n"
                + "dmEitVZ8mcuRgQ==\n"
                + "-----END CERTIFICATE-----\n",
                //
                // Private key.
                //
                "MIIEvAIBADANBgkqhkiG9w0BAQEFAASCBKYwggSiAgEAAoIBAQClt40e4e/lW5S1\n"
                + "4zFrkZrg3ZAOeYaTWkAa6EVUbtGiH/eRTeAnODRQey9zQFRY8cM6izBheJbfPN9r\n"
                + "ZN1SfL8R4XfHHDXS9udX4zWIOcrAMQxoK2KEFMcJ1F3cMFGQ1vIM+A4FTjObHl9m\n"
                + "Et6Pq0LQTPlX7JJw+1R7REJYSkxFm7h/lsr/8CScVxF4SZtO6/EMPoG2kSPylQlq\n"
                + "1Yi48+BBFuPObftYv1myfCV0s0C893EME1+YaE6sjAknqb+KGUmCLCgzgV+mpr8j\n"
                + "6ByINRf3E+iAWL1/3dKEv0XIYNA7bz7Qc6AOL07cq2EnXtAJI6QbeV4hab9uoTjB\n"
                + "DeIwXcTNAgMBAAECggEAWiZUOuymWJtNdvU1OVNocdOWPomV0CtUnE6nCJuQpyXE\n"
                + "w+MdgbhtnSqmUYg5WnmKvxphI2U6jg7La9zfGbSLLOr/Ae4yyaVPjNwpjwYBajRE\n"
                + "j5aqbTfwX0SMGvCeLrD/1FZNKk20fTo0o464TGfaXP7C1jX4JEZVWwlzHhytCWAg\n"
                + "tkApQmmU0AsDnLll4zWDa/IeeVDJXkF95S4Dyj0LCrJ8+9PF2zN0AKYGuDjOWY2d\n"
                + "yeWW9oXI7zzBBtCriPy+GYXNpHmfpURCWBt4Nc89EV2qB6ZYmMXkspfA/wLtcRm1\n"
                + "zchbptCakgaNfw5qMJrucB5Jz1I7vfMSlcu4VZ0lAQKBgQDZ5PvqVet5/flPaJTR\n"
                + "Ct8Ye1Y62lZgz1woNjzhC0btL2eAcMtgsebznlTFY4NLb4M2eo7CabSGs7xykhBS\n"
                + "t4o//eh+MupMYHYGrzaPHjRnBbm/mTR1DUuPmBX6ZGtwXifFmwvhZdaRWuCzVoYN\n"
                + "uUONvUV5R/xz9niyXRtN1HVLMQKBgQDCspxCgHc3OvXYEHl10Yoc4qsGH+7NQTAA\n"
                + "5TzhhdxcR6vxnBJSKh/kzMPM4QYXdwPfaHMsay+FEiGz6agdKgtTAA6mKnjjM4zk\n"
                + "e/J9ou8QWm08T0GxUXcx1NGYM8Nsamz2svZ0XAe76I0hhosV5nxmXabayPgHe50L\n"
                + "8SD6qFO0XQKBgHO0bbVNNMLOA8KQJV4wKLHGZM7RvEaiNizASGm0ZFB0+MAypTzO\n"
                + "m3ZIYHmE02aOa53VTNOd8BgLf4lTWMmj3w0GFpxVCyfNnT8FcbJj9q2yU6WThFCX\n"
                + "48T3nMwe4RKFXRdIsvFY86yyFloFGyBUfbPZivfRKxSlEAie+m3E4RgxAoGAVrxT\n"
                + "OJ0afxRZKWRNd9tdd/jSz+ux6ua7h+qX8LA9ty6GvyAUWV5Czx8Zq1Aj8pgmtYRG\n"
                + "quclSFcHhKr3JebxHIzN+eC58h2pCrDdGnNXpSVjvJZiYag1PZHdvbxxtv7ChDS9\n"
                + "7qCBIYk8Nk9F7v+7M69NAfK97Dd5gzRsyL3sbFECgYBxz2nCKeBv2frxtD36nlY1\n"
                + "bDhZv1Asyxq0yt9GSkVWeHnSIFHfwBu3w6qW7+qdlQizu+6GZ3wq9dyyrXUA4zPU\n"
                + "QnYWYYk30p4uqLXFCrnscVOm3v7f51oEYVoEKjqGl2C/BTy7iSgCRd+cp6y34DsV\n"
                + "PsRyQCB/QarxsDNAuioguQ=="
        ),
        rsa_pkcs1_sha256(
                "RSA",
                /**
                 * Signature Algorithm: sha256WithRSAEncryption
                 * Issuer: CN = localhost
                 * Validity Not Before: Jun 4 15:22:04 2018 GMT
                 * Not After: May 30 15:22:04 2038 GMT
                 * Subject: CN = localhost
                 * Public Key Algorithm: rsaEncryption
                 */
                "-----BEGIN CERTIFICATE-----\n"
                + "MIIDBjCCAe6gAwIBAgIUc8yTYekR2LuXkkCJYqWlS/pBMKIwDQYJKoZIhvcNAQEL\n"
                + "BQAwFDESMBAGA1UEAwwJbG9jYWxob3N0MB4XDTE4MDYwNDE1MjIwNFoXDTM4MDUz\n"
                + "MDE1MjIwNFowFDESMBAGA1UEAwwJbG9jYWxob3N0MIIBIjANBgkqhkiG9w0BAQEF\n"
                + "AAOCAQ8AMIIBCgKCAQEA2jDPGMogc9dq2w5b+FHqbfaGPokRmyObiU8y/l/dqkM5\n"
                + "9IV+qj8VQUI4NtpdCTWr16812z4AjXrk5HIBrECfQbHPUcm1rme5YVZ0WxV0+Ufy\n"
                + "hDmrGwDLhkxGqc3hOhRrlF2wdXeUfjIzhvS9+S/401++t/jvq+cqFF1BHnzYOu+l\n"
                + "nbi/o95Oqo8MlwiRqg3xy3fNRfqXk7DWy+QT8s+Vc3Pcj1EW6K0iJJ23BVTdv6YT\n"
                + "Ja5IKiWL4XsLht3fWvZwF+PoYfKb+JYflt0rafpxg9xkowe7GnGh2SpV7bJaH/QN\n"
                + "3PTFEKQWgWHjWwjR171GOzSaEgaklvKde6+zNWeYKwIDAQABo1AwTjAdBgNVHQ4E\n"
                + "FgQUqCtGe8/Ky4O6pH7xeTUy9yrv4n0wHwYDVR0jBBgwFoAUqCtGe8/Ky4O6pH7x\n"
                + "eTUy9yrv4n0wDAYDVR0TBAUwAwEB/zANBgkqhkiG9w0BAQsFAAOCAQEAuqch30im\n"
                + "M09sARarbfK3OExqYK2xoyuUscgTqQNDpNL2gMdXY9e0lTmGVgw9pVYtNZPZRxem\n"
                + "jR5an2XegvG9qVU6vLENDwLCqZgsTb2gvmXngiG8NVcYd81GNqD228mkgBosNJku\n"
                + "6BR+C8zlURzsNEt657eVvIp9ObGomdAbWhpdqihBD180PP18DIBWopyfHfJtT5FA\n"
                + "U2kSPBp+P1EtdceW0zfwv3rF8hwRbnQBzuoYrZfn2PiMYaGUqOgbqUltCMD/Dp9G\n"
                + "xK0nfAXEwIqHWWnijGwAd6YrszMjBUcSGmlehdF+XZK6jHNlw64RB4WTfavr+rY0\n"
                + "dTe6g4o5GYr9nQ==\n"
                + "-----END CERTIFICATE-----\n",
                //
                // Private key.
                //
                "MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQDaMM8YyiBz12rb\n"
                + "Dlv4Uept9oY+iRGbI5uJTzL+X92qQzn0hX6qPxVBQjg22l0JNavXrzXbPgCNeuTk\n"
                + "cgGsQJ9Bsc9RybWuZ7lhVnRbFXT5R/KEOasbAMuGTEapzeE6FGuUXbB1d5R+MjOG\n"
                + "9L35L/jTX763+O+r5yoUXUEefNg676WduL+j3k6qjwyXCJGqDfHLd81F+peTsNbL\n"
                + "5BPyz5Vzc9yPURborSIknbcFVN2/phMlrkgqJYvhewuG3d9a9nAX4+hh8pv4lh+W\n"
                + "3Stp+nGD3GSjB7sacaHZKlXtslof9A3c9MUQpBaBYeNbCNHXvUY7NJoSBqSW8p17\n"
                + "r7M1Z5grAgMBAAECggEAHs/7vw10TcejEHJTrJqs14CT7qresKDzqw1jLycMn6nE\n"
                + "unJLs/EaqE+Yrq5hqxZIQTo+CcsUuuYbAuPStqedleJtW6h3nryJImTaI67BCR8O\n"
                + "8XtPXY3cMAf/hqVLZC9UDey5Ka2Ma9HdEvbnCRSsN/VycnqWJhmMCLouowaQZqoE\n"
                + "VopscUix8GqELv0vEo2CszZfUjtSVbNTlNgwDf5U7eSKXMuFsnSn/LE7AMvHsEyo\n"
                + "HatxogwlM/WjpTnf/WIeJY3VhaK10IsP6OEgUn/p4VtI2DQ/TJdgYrvD5vhjY8ip\n"
                + "XuUPuPILRvJWo8dRXJqa4diXB12q5YhP8iiOp4BgkQKBgQD1GtlOR+JVgOzpQ11h\n"
                + "s5/iJOsczee80pQscbSRJnzSsIaP9WM8CyJgvbPxIQxLUQeYnxM/bxNKkpJtzxRK\n"
                + "pob+v4NoRn8PTpqbOp1obmWJT7uHTaoeavQo7r7uZI4i3eEgHCCQkMzpqzz7UFTY\n"
                + "2Yst7bBTPUivlSVQQBEc8bLpeQKBgQDj47EjpAlh8DmJRTElg58t+XJehXGTqmlx\n"
                + "nYu8DQLSzGbOQ/Z4srakC1mkM0LHCmULIIWk3KhV1GBCeArv7DlZ9A1SkI95bsq9\n"
                + "GBeQpovL0PXKkOOWMJBklP/CTECO4eyA8r6c1d8wytBb6MrJ8bi74DdT+JlFjK5A\n"
                + "zNoeNx6JwwKBgQCehIPABeuSYvRVlDTDqFkh98B6+4wBaatc5xjhuyOFW5dbaVeJ\n"
                + "kKXmLSpAK6B44WnpQhA/uUWfuBWtoPy9nt+1yARjnxwzuSFyfUEqNiPC32coBYmd\n"
                + "bIyGIIopQa1PTXJ4wtgoxw1PnmitHHITYPaLeKrN2te0fuAH+7dVodeU+QKBgAct\n"
                + "VJbaw7Dh7+3yz+lui8TW5lMzwK/13fxGCfCSOFSLO3Gjkk+a0UW5VclmE+RQ333K\n"
                + "OGtIx8RsO9vcC/wiZGwA06qWAu7AHoJ2D8fudtikbBlFFuXUAbgpOSTVYfMeCmTF\n"
                + "QFuQIMdYm9dJLZnOkxLXrOZoHeui0poX2Ya6FawhAoGAAI/QCyDbuvnJzGmjSbvl\n"
                + "5Ndr9lNAansCXaUzXuVLp6dD6PnB8HVCE8tdETZrcXseyTBeltaxAhj+tCybJvDO\n"
                + "sV8UmPR0w9ibExmUIVGX5BpoRlB/KWxEG3ar/wJbUZVZ2oSdIAZvCvdbN956SLDg\n"
                + "Pg5M5wrRqs71s2EiIJd0HrU="
        ),
        rsa_pkcs1_sha384(
                "RSA",
                /**
                 * Signature Algorithm: sha384WithRSAEncryption
                 * Issuer: CN = localhost
                 * Validity Not Before: Jun 7 07:59:56 2018 GMT
                 * Not After: Jun 2 07:59:56 2038 GMT
                 * Subject: CN = localhost
                 * Public Key Algorithm: rsaEncryption
                 */
                "-----BEGIN CERTIFICATE-----\n"
                + "MIIDBjCCAe6gAwIBAgIUcjBWKuODVu3MEVfJLsTPV7yRdo8wDQYJKoZIhvcNAQEM\n"
                + "BQAwFDESMBAGA1UEAwwJbG9jYWxob3N0MB4XDTE4MDYwNzA3NTk1NloXDTM4MDYw\n"
                + "MjA3NTk1NlowFDESMBAGA1UEAwwJbG9jYWxob3N0MIIBIjANBgkqhkiG9w0BAQEF\n"
                + "AAOCAQ8AMIIBCgKCAQEAvIgnQr3nGpMz/QYksk2r+SpqbRhiOFno6c/kyJmSu8TQ\n"
                + "08RV8Ad8CalnADkHlPBoTmCqY5v9aOupcl1+jCe7e4idz4IJsuY1KRU/Uj4lYVqO\n"
                + "1W78HxIgKd3jbSgA9628lJ6Nhv6OqyA8KCInKboI8FdDmdRVzu+FCmGKLBtUVnOP\n"
                + "OkzMzttkrg7oyUoCagYizZLY8lhUA8RfgxIMK3zhS1t3k5LbH36+kd6sECNrg5+y\n"
                + "6Dbd7g67ja40ALZpWU75yLNR9G71XZ0b6HyFjbF31oVCHtoeN6MiEXYS0F6+VT+x\n"
                + "y5xg4y4JZfZFJB7GUaAso4XZxKYTypv7DgPE7WquHwIDAQABo1AwTjAdBgNVHQ4E\n"
                + "FgQUMQoCUE5YPhS4im6eAZaCRrNfqH8wHwYDVR0jBBgwFoAUMQoCUE5YPhS4im6e\n"
                + "AZaCRrNfqH8wDAYDVR0TBAUwAwEB/zANBgkqhkiG9w0BAQwFAAOCAQEAm5o/OH/x\n"
                + "i1Z4TVPYRDbDgRZoGsikQtt6JxYRgqhwHrTo188kJw61X4hRaMcV5Td8WTsqXJCq\n"
                + "dfGGpV4L6FCwBNNNxIyHGTrtjNtGae2x2c9XYX0O6nnmgQP4+Koo7ouqpxmMzfGa\n"
                + "uN3PWP8ABhEiLCBfRRK8w9kpxOuJfQyiwXQQN9TH6MU63O52ydA5NYA2RMbRcjsC\n"
                + "BE4VHGsJJo1z8Z9KI7pdSvP3/wTSMVpYY6kxanMSHNeFV5aDfSOX8U8oqIF7xkiA\n"
                + "o1CjhPYUvS9MEmoq74ulKNgfstjYcymiP10odaiv4wg1oIhxVtNV/fpdKQKHDeu0\n"
                + "30Ac3llO11/faA==\n"
                + "-----END CERTIFICATE-----\n",
                //
                // Private key.
                //
                "MIIEwAIBADANBgkqhkiG9w0BAQEFAASCBKowggSmAgEAAoIBAQC8iCdCvecakzP9\n"
                + "BiSyTav5KmptGGI4Wejpz+TImZK7xNDTxFXwB3wJqWcAOQeU8GhOYKpjm/1o66ly\n"
                + "XX6MJ7t7iJ3Pggmy5jUpFT9SPiVhWo7VbvwfEiAp3eNtKAD3rbyUno2G/o6rIDwo\n"
                + "IicpugjwV0OZ1FXO74UKYYosG1RWc486TMzO22SuDujJSgJqBiLNktjyWFQDxF+D\n"
                + "EgwrfOFLW3eTktsffr6R3qwQI2uDn7LoNt3uDruNrjQAtmlZTvnIs1H0bvVdnRvo\n"
                + "fIWNsXfWhUIe2h43oyIRdhLQXr5VP7HLnGDjLgll9kUkHsZRoCyjhdnEphPKm/sO\n"
                + "A8Ttaq4fAgMBAAECggEBAJp+lHW7+wsq/9pj02SOA9RubsIxziNRgm0/4MKGHtli\n"
                + "QqKW7LmC2KFuQarMESt7Cm7YBikUZkg5fiTq+s3NrXRhErk+XcZNodilwbsalDVA\n"
                + "KXY7ub2Yc+T7IOiNdKWCqNu9RksOhUk0ZDebLS6jdbnGOO5xM1QdsY89OSZFywn/\n"
                + "xk1S+gvx5f7icchhSvPse3H2Sw3411HriWB83cDH09YPCZcJTv2QM/xophSru2j/\n"
                + "kpRQhnJqGZ1M+1QDVi5HWNx+Y5v0QuCVWOi8Hat/S7x2J3opAqaCIjK0ErOC/OdY\n"
                + "BJqjOyH8tfwnkwD+ghGIj44/lDsi0GY/2CsD63PNX7kCgYEA5Nz5pIi//2FqnOS7\n"
                + "Pnvo374ZhMaepwvDdGOfkDDyL6k+5pLaO4SL5znQQzGmM6EWi3z7ZKw99MzQcwR6\n"
                + "4COAgBqDm0vXY0MI4vLPzZpOe6hABieiaj4tvG9Ts2yqjBPLo6mZ6UDMFbg4Tt0F\n"
                + "bfaqF5jA/mR2SpBzhRIeX0/eW3sCgYEA0uLw75774f6hnUWqi2jMtgwiOHXSFb+f\n"
                + "bnxWGNJfxVW76rVh9892m1FXdU8SZivwg50dvo/GDQ1NsaaL5ypPsoP4d00336VT\n"
                + "Gx1NAlo9WuD306cYXzMUmXCCwoCbOw7vdAAoIEDei8+nr6QO7yb5/FsWrmSZuadf\n"
                + "ljLT+02f1K0CgYEAq0j7CfpS//ZPzXae8OfZ5UKoZKgma00xjnVVIZyQVb1sVzMH\n"
                + "Y84Syw6I4RFSm4dvkRwMJk+G2yVCySJMOF45uSae4uaDIEY2a2xgvDdFj+TfbfvR\n"
                + "4YQBxOrpEPs+NTJHkYjIqTsWwxaqBdQDUUZwDNMFdh+ILMwpuSlTU/A5sesCgYEA\n"
                + "q+Y7OUfhz/TMbjN0chDqFVbMqjM4HWxGnDwTvkX1tRhOhmJ1yhc9ehuS69eZitk0\n"
                + "Q0RWE5iEeu0mMLIuhi8SKdSzOyQhcFcF6Cs5M7q1GpgYy1kAX9F2cCCrJbrJThm9\n"
                + "jFP4YVofTd3ltSFI3x5pVZA2a88rE49gNkGWU9mReD0CgYEAkXoH6Sn+Elp6Oa+k\n"
                + "jF5jQ14RNH4SoBSFR8mY6jOIGzINTWiMCaBMiPusjkrq7SfgIM3iEeJWmgghlZdl\n"
                + "0ynmwThnfQnEEtsvpSMteI11eVrZrMGVZOhgALxf4zcmQCpPVQicNQLkocuAZSAo\n"
                + "mzO1FvNUBCMZb/5PQdiFw3pMEyQ="
        ),
        rsa_pkcs1_sha512(
                "RSA",
                /**
                 * Signature Algorithm: sha512WithRSAEncryption
                 * Issuer: CN = localhost
                 * Validity Not Before: Jun 7 08:00:45 2018 GMT
                 * Not After: Jun 2 08:00:45 2038 GMT
                 * Subject: CN = localhost
                 * Public Key Algorithm: rsaEncryption
                 */
                "-----BEGIN CERTIFICATE-----\n"
                + "MIIDBjCCAe6gAwIBAgIUZEjv/5m6CpjS/ZAZ20O81p64tCwwDQYJKoZIhvcNAQEN\n"
                + "BQAwFDESMBAGA1UEAwwJbG9jYWxob3N0MB4XDTE4MDYwNzA4MDA0NVoXDTM4MDYw\n"
                + "MjA4MDA0NVowFDESMBAGA1UEAwwJbG9jYWxob3N0MIIBIjANBgkqhkiG9w0BAQEF\n"
                + "AAOCAQ8AMIIBCgKCAQEA2heH4cASjGrc2ir6n4pqoQSzT5mhlR8/iF/5XrfISSbn\n"
                + "fZNZnPk2cTQSDD8HNbRT9bvtrIYQITrGgUYKBTwtwycsM6v76+TFWsPBHWbn8bny\n"
                + "eGsFxhNXsrRXZYiqDwoiPE+J8aTQMt4WlNBaZjt/9BEUYyTyZ8c53WWaE9aSE3sQ\n"
                + "ynulZ8ruhkc9hbybEO1UfAQEWIY+nR0U9aBPSkmMxGbYaVhRecI6U5f2HpUA1llx\n"
                + "LXJRD7PpiljccbxH2sNUdB4zL72XLsY1hn7igb0V9nNy79dzBTplmygEFv3ciLNi\n"
                + "Jx5kOWEtuQh/DlWxptIW85lHugjkNdDnyiinCjIUBQIDAQABo1AwTjAdBgNVHQ4E\n"
                + "FgQU8u4DRt2RBndU7GqGw9/IFdkfMRwwHwYDVR0jBBgwFoAU8u4DRt2RBndU7GqG\n"
                + "w9/IFdkfMRwwDAYDVR0TBAUwAwEB/zANBgkqhkiG9w0BAQ0FAAOCAQEAWt8hlw9e\n"
                + "kCFqGd+MqZLGg6Lv5tgJ1GRb+tMmGfRTwG0kegBzY7qU4ZZvasRH0F0lT7G8YNGs\n"
                + "Asddvx6WuCQxW+V9WAfZMorSyUE1PKm0OR/vyZoZ7KoqWfMuxsPG6C/uh5Np4gd8\n"
                + "8dIAASemQo8zLI+MBUjOiww+EwXkZ14m8vKAxKDk9JxzmgrkgH3U6CxhFgm51bmx\n"
                + "d6axFU4srW+wjONb4nWLh3Cd9cSwL9nvbIG09T9xDf783uz3NYIqsmrKSP4h76WQ\n"
                + "dueGgIRtJkV/x2dOmbCAh+SbY99kWG2wVFDmznweYSqzaqNfX4uiIkFB8M1Pi/9W\n"
                + "ZOZBzpaxm+ARCw==\n"
                + "-----END CERTIFICATE-----\n",
                //
                // Private key.
                //
                "MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQDaF4fhwBKMatza\n"
                + "KvqfimqhBLNPmaGVHz+IX/let8hJJud9k1mc+TZxNBIMPwc1tFP1u+2shhAhOsaB\n"
                + "RgoFPC3DJywzq/vr5MVaw8EdZufxufJ4awXGE1eytFdliKoPCiI8T4nxpNAy3haU\n"
                + "0FpmO3/0ERRjJPJnxzndZZoT1pITexDKe6Vnyu6GRz2FvJsQ7VR8BARYhj6dHRT1\n"
                + "oE9KSYzEZthpWFF5wjpTl/YelQDWWXEtclEPs+mKWNxxvEfaw1R0HjMvvZcuxjWG\n"
                + "fuKBvRX2c3Lv13MFOmWbKAQW/dyIs2InHmQ5YS25CH8OVbGm0hbzmUe6COQ10OfK\n"
                + "KKcKMhQFAgMBAAECggEAFE5YkoZls7lHcvDJgQ1MPE3JvEGbr9zG95WoDE+kUFtU\n"
                + "9nY/w8PYc9XbUFSWAUQXBSxIRuX8nntwfBzfVflycVc2YGFFBYO5dGgBlRE9y136\n"
                + "24drdDPc2IC+GnANFXVmFqN5PoyP7gyLobN8l0Q296yXb1vDTjJYiuqo2+wAkduZ\n"
                + "gQDjl1jTS/1RXjix7Kaw9PrO5ONqp9f8FqJCVyezhp+OMNA88Uz619jvYC4axNh4\n"
                + "zjESAf1YXPJUTM9nznmFQ8jS4+6I0yvf0jaNpCY1bC7kaHBMKkYWiBMPaNWYHa9o\n"
                + "9EyPN5tPk8OTk/m9+e98vo9R1JR9016TKxDQtRxzAQKBgQDztIFeYuAUh5uTAGe+\n"
                + "7FTfnsF+Pl3raWUf9T8d/m+3XujAXNrtURb2TkyQlsB+cNWa9VH/mMGz5RFE1mlC\n"
                + "XaWKPrf0SAdbmx+wcRrEOlf+ARNj8XbEyKptpi7tgN11s6Eyztkn6xwUmSCEuZ+x\n"
                + "Rly2F6A3FuHTJh24yjIFnP8vZQKBgQDlGDoFj9mKKkbAxoZGZYN/B+T5EUAW/OaH\n"
                + "Oaxqi55QpGCbxGVR+G0AMG6WEN81qm812GTHJD22As6Lz9zFThKkJN2za43CAgrq\n"
                + "MA7KgqO0w5rvuXXiPCHZUvdDkKFpIMZj4ftq83Xh3mkN5mbhF9FP0v3/xrczowYP\n"
                + "oj11IHWYIQKBgQDkuvfo5Jg37IcB05GLyjhmoZQtrs9rkcIN2ppgxluIGZYOZZg8\n"
                + "wKzyg86srjEA+1ogVDufz3mOJGKu3yZv2YDzXaY9qhTtz4xQh/d9UN0hU1Ulqo20\n"
                + "aDo9K4pD83znaa7UBvwd0TbLxmSU7buKIOYHKel/iwRsrwuaUnvcdNu1WQKBgBa0\n"
                + "SvPIKNgPjomGY0JQxzJstt2UPxTIJZSbO6Inih4V3FkzopL4Gt1c72jB7U0lQfZF\n"
                + "Jt+xkMgcCRpEFG4daa2I1cv1ScxDZY+GCcE6Jz0/8Xf2ml88dGJUXZr9l3GSxPab\n"
                + "K86SqEklQKYXAnUmZiESGQgjSn68llowSwTznZPhAoGBAMH2scnvGRbPmzm91pyY\n"
                + "1loeejtO8qWQsRFaSZyqtlW1c/zHaotTU1XhmVxnliv/HCb3t7qlssb3snCTUY9R\n"
                + "mcyMWbaTIBMNfW2IspX4hhkLuCwzhskl/08/8GJwkOEAo3q/TYigyFPVEwq8R9uq\n"
                + "l0uEakWMhPrvr/N1FT1KXo6S"
        ),
        ecdsa_secp384r1_sha384(
                "EC",
                /**
                 * Signature Algorithm: ecdsa-with-SHA384
                 * Issuer: CN = localhost
                 * Validity Not Before: Jun 7 08:02:00 2018 GMT
                 * Not After : Jun 2 08:02:00 2038 GMT
                 * Subject: CN = localhost
                 * Public Key Algorithm: id-ecPublicKey
                 */
                "-----BEGIN CERTIFICATE-----\n"
                + "MIIBuDCCAT2gAwIBAgIUIs1pMDA3bRg9B2OP7GFMAYxhr8owCgYIKoZIzj0EAwMw\n"
                + "FDESMBAGA1UEAwwJbG9jYWxob3N0MB4XDTE4MDYwNzA4MDIwMFoXDTM4MDYwMjA4\n"
                + "MDIwMFowFDESMBAGA1UEAwwJbG9jYWxob3N0MHYwEAYHKoZIzj0CAQYFK4EEACID\n"
                + "YgAEeXrId3yy/0mJPNXXGXlMSQEvIpSyDCcKKQd2Zm1qt0gA1HUSHulfStyHUI6D\n"
                + "l/pY7iR0wO/xYIWhirmqT+XbVPTWIJb245Lf9GFiR/d6UyUbqXuNg9GpnURsy5Zh\n"
                + "x4Dfo1AwTjAdBgNVHQ4EFgQUk3NPE5K8ovMPNFtzX27rprTndU0wHwYDVR0jBBgw\n"
                + "FoAUk3NPE5K8ovMPNFtzX27rprTndU0wDAYDVR0TBAUwAwEB/zAKBggqhkjOPQQD\n"
                + "AwNpADBmAjEAonkaekNeu3ICbZyvbuMIBppBemAdGgTArlVEl+V6BFqhqpAwNwvl\n"
                + "ipkjYLDTLCNmAjEAofWQz5kSdpigbJCU/ke9JFce9Vy6gp4kPIghN6f6EYtlwQQU\n"
                + "yXwh67EHeFD4Bnr7\n"
                + "-----END CERTIFICATE-----\n",
                //
                // Private key.
                //
                "MIG2AgEAMBAGByqGSM49AgEGBSuBBAAiBIGeMIGbAgEBBDCpxyn85BJ+JFfT5U7U\n"
                + "VY+c8v2oY873YOVussMDiC82VYGKZDZH8D6C6h0b33iCpm2hZANiAAR5esh3fLL/\n"
                + "SYk81dcZeUxJAS8ilLIMJwopB3ZmbWq3SADUdRIe6V9K3IdQjoOX+ljuJHTA7/Fg\n"
                + "haGKuapP5dtU9NYglvbjkt/0YWJH93pTJRupe42D0amdRGzLlmHHgN8="
        ),
        ecdsa_secp521r1_sha512(
                "EC",
                /**
                 * Signature Algorithm: ecdsa-with-SHA512
                 * Issuer: CN = localhost
                 * Validity Not Before: Jun 7 08:03:11 2018 GMT
                 * Not After : Jun 2 08:03:11 2038 GMT
                 * Subject: CN = localhost
                 * Public Key Algorithm: id-ecPublicKey
                 */
                "-----BEGIN CERTIFICATE-----\n"
                + "MIICATCCAWOgAwIBAgIUPA5KmdGlz29IePn+kPah9OlNRyYwCgYIKoZIzj0EAwQw\n"
                + "FDESMBAGA1UEAwwJbG9jYWxob3N0MB4XDTE4MDYwNzA4MDMxMVoXDTM4MDYwMjA4\n"
                + "MDMxMVowFDESMBAGA1UEAwwJbG9jYWxob3N0MIGbMBAGByqGSM49AgEGBSuBBAAj\n"
                + "A4GGAAQAs1phx3tISw+G2HA8r4ZflQ3Q/5o9U5zeFz79PsKEPSsE1xhGbUyWDasB\n"
                + "SeUKOvVsli5vYy0hdO132Q7Nl+QANUEARY2ax5ERXIJBY9S+PRdP7OG4fr966Rv8\n"
                + "vaCE2g8pV9NnAtalN3sk8iCEdVUthvL9R6nopiEd7Fz9SMRGOSu18FajUDBOMB0G\n"
                + "A1UdDgQWBBTUUVvZJV1MlH/cRuWxfMF9eIsXBTAfBgNVHSMEGDAWgBTUUVvZJV1M\n"
                + "lH/cRuWxfMF9eIsXBTAMBgNVHRMEBTADAQH/MAoGCCqGSM49BAMEA4GLADCBhwJB\n"
                + "UUzp0KbJ1dj1h2xd4yN1DXW+Xyxah8Z5oiWvG1EfTYL201GcgmUhfqqwJBJphtsh\n"
                + "Bg7qTGGg5F9cVOI9+yMCcDoCQgCJ3chYHlTiC5QpW54hdeV+k45PoCQ62Foopn2i\n"
                + "N/aUEkWfZ7OidC7O3BWlhDvrHcPLisxHx4oF7vebatReBE+DLQ==\n"
                + "-----END CERTIFICATE-----\n",
                //
                // Private key.
                //
                "MIHuAgEAMBAGByqGSM49AgEGBSuBBAAjBIHWMIHTAgEBBEIAz7qc9msPhSoh0iiT\n"
                + "Z0146/sLJL5K+JNo2KdKpZOf1mS/egCCbp7lndigL7jr0JnBRIjk+pmeBtIId6mW\n"
                + "MrcvF4KhgYkDgYYABACzWmHHe0hLD4bYcDyvhl+VDdD/mj1TnN4XPv0+woQ9KwTX\n"
                + "GEZtTJYNqwFJ5Qo69WyWLm9jLSF07XfZDs2X5AA1QQBFjZrHkRFcgkFj1L49F0/s\n"
                + "4bh+v3rpG/y9oITaDylX02cC1qU3eyTyIIR1VS2G8v1HqeimIR3sXP1IxEY5K7Xw\n"
                + "Vg=="
        ),
        rsa_pss_rsae_sha384(
                "RSA",
                /**
                 * Signature Algorithm: rsassaPss
                 * Issuer: CN = root
                 * Validity Not Before: Jun 7 08:04:01 2018 GMT
                 * Not After : Jun 2 08:04:01 2038 GMT
                 * Subject: CN = root
                 * Public Key Algorithm: rsassaPss
                 */
                "-----BEGIN CERTIFICATE-----\n"
                + "MIIDXDCCAhOgAwIBAgIUTYfwEB8SfPXHN+jU18mGBoInoxwwPgYJKoZIhvcNAQEK\n"
                + "MDGgDTALBglghkgBZQMEAgKhGjAYBgkqhkiG9w0BAQgwCwYJYIZIAWUDBAICogQC\n"
                + "AgDOMA8xDTALBgNVBAMMBHJvb3QwHhcNMTgwNjA3MDgwNDAxWhcNMzgwNjAyMDgw\n"
                + "NDAxWjAPMQ0wCwYDVQQDDARyb290MIIBIDALBgkqhkiG9w0BAQoDggEPADCCAQoC\n"
                + "ggEBANVenkAl6FfHbzw1HG38BAS6tuNBSXU9m5/0kEGP5J5tWqGNVL/zwRoFGPH4\n"
                + "q1KUpmbE6kgDka5fwBKALlh1mnLbgNG283mc3zj4MDgv+IZvK9D2pJfd8oTmMNqS\n"
                + "QGwL19tlLjXiq+shwcccOMGJrmAU21Ca+rn/kn+nOjMnhP3mWyR9z/K1PI6D4wvJ\n"
                + "Kexp54vhuu9dgfBM4d5vlCtnbE5Y5kZHEVhLHbmlR1urnqjlqq7YVl8W4UUaV6CE\n"
                + "Djj3b6+u+87zjMv9NJh+bn44DPlnnc8WkUIohf8ci4rdUQlT/STkIjl6L1MDSUxC\n"
                + "3PAjRaTGgpdg3bq81omO1O7OQEcCAwEAAaNQME4wHQYDVR0OBBYEFK6KsXrsGUu2\n"
                + "IoaQ+Q07G3Vi7AWWMB8GA1UdIwQYMBaAFK6KsXrsGUu2IoaQ+Q07G3Vi7AWWMAwG\n"
                + "A1UdEwQFMAMBAf8wPgYJKoZIhvcNAQEKMDGgDTALBglghkgBZQMEAgKhGjAYBgkq\n"
                + "hkiG9w0BAQgwCwYJYIZIAWUDBAICogQCAgDOA4IBAQBuflWm96V3EvIBfuTcjIq/\n"
                + "R0AEqNb17F7v96cJHNj1FKM0mFu8R1UBWDYzwNAdXLylDzn/4OEsYmcY2AaLKn9j\n"
                + "E5IXcMeGTuDX0rsUJtdBu4ueHzr4PUMkEsRHL9vG6EYj12+UyHl1iN2F4dWU6Vvs\n"
                + "mtKjGZCA2VG2+xczyS1PouLV/H/My7O0cB0eO3kOlMFENcLRMgcwWLvUKuKgzZ2z\n"
                + "XerJdFBQ1wEJQr9JdcbAXJkQCbiFq/sgdzLuo9KwiQ9WOm99eB//iHQkOiNF0NLn\n"
                + "hMdqUqaPOvUBl2gVC+aPm0cdlFj1IcbAaOq6Kg2OA3edapyUQF3+jZm4pnPHyT8Y\n"
                + "-----END CERTIFICATE-----\n",
                /**
                 * Signature Algorithm: rsassaPss
                 * Issuer: CN = root
                 * Validity Not Before: Jun 7 08:04:01 2018 GMT
                 * Not After : Jun 2 08:04:01 2038 GMT
                 * Subject: CN = localhost
                 * Public Key Algorithm: rsaEncryption
                 */
                "-----BEGIN CERTIFICATE-----\n"
                + "MIID8DCCAqegAwIBAgIUKNLkRNjNsv7usNhc8tvkJwyKBxgwPgYJKoZIhvcNAQEK\n"
                + "MDGgDTALBglghkgBZQMEAgKhGjAYBgkqhkiG9w0BAQgwCwYJYIZIAWUDBAICogQC\n"
                + "AgDOMA8xDTALBgNVBAMMBHJvb3QwHhcNMTgwNjA3MDgwNDAxWhcNMzgwNjAyMDgw\n"
                + "NDAxWjAUMRIwEAYDVQQDDAlsb2NhbGhvc3QwggEiMA0GCSqGSIb3DQEBAQUAA4IB\n"
                + "DwAwggEKAoIBAQCe7chGqR+iYpXWHDWK0LZVff4XDc/dS0CloNQr6GAa/Gb7p37v\n"
                + "kjWStk5BKpRg1SNZtssFNEOXR4phjFf3boUQ7A1i9e+eYJAmbGalwwotY1zxdr5k\n"
                + "faWYxIiMSaPHHwPfe/pnY1RF6lOsLlegPw6xxg08LETaU+M9QCJ9EodXDEb19/Kw\n"
                + "INer/Cduou7TdVDFPYY02lMoj7WrvFgu90fRL/EmsMgN6dB9pBS6GbJK5e8E4lpg\n"
                + "KacuXdCf3eHWMz/4MGKxXEzXTv4kjvR067xjZmtvO70iTQgmL54w1YdLO5oU1yYl\n"
                + "yXJ/z0iBEM5TG4nESoTWkILZDLcqjyCPfzdXAgMBAAGjgdwwgdkwCQYDVR0TBAIw\n"
                + "ADALBgNVHQ8EBAMCA/gwEwYDVR0lBAwwCgYIKwYBBQUHAwEwHQYDVR0OBBYEFC5n\n"
                + "8hdp8OS1TWG/EzvMHa1R8608MEoGA1UdIwRDMEGAFK6KsXrsGUu2IoaQ+Q07G3Vi\n"
                + "7AWWoROkETAPMQ0wCwYDVQQDDARyb290ghRNh/AQHxJ89cc36NTXyYYGgiejHDAR\n"
                + "BglghkgBhvhCAQEEBAMCBkAwLAYJYIZIAYb4QgENBB8WHU9wZW5TU0wgR2VuZXJh\n"
                + "dGVkIENlcnRpZmljYXRlMD4GCSqGSIb3DQEBCjAxoA0wCwYJYIZIAWUDBAICoRow\n"
                + "GAYJKoZIhvcNAQEIMAsGCWCGSAFlAwQCAqIEAgIAzgOCAQEACnNXDJXG4sneapAr\n"
                + "U3CjwrNB+ip7v6qTuntIMVxpo5ONMdE0lX8UcAsUfnIaUKpNZxAENelsG7ywdV3R\n"
                + "unWV28bY4faIrgzKB230KS0GvjQ+gARvaP39V/7r2OtpXfLzMKlddacAQt65G4uB\n"
                + "9j9pDb6hD2FZvBZLsfuzezl6TGO7mDw61BK61A+9cJLLnHzpR7TdgjKWtZ2FB29P\n"
                + "zXly+ukq56Xadu9QbDzjnKxrPSzgd2i05C62qBWZ8/0ZvCIcFqEVAa/IReTIwCCo\n"
                + "805SJ0VB3hOUK8eg/FOT10cK+JPZaSTQxYCUxL9fAaoQB7SnA6MsBXprpq1Ibse6\n"
                + "zGU9Zw==\n"
                + "-----END CERTIFICATE-----\n",
                //
                // Private key.
                //
                "MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQCe7chGqR+iYpXW\n"
                + "HDWK0LZVff4XDc/dS0CloNQr6GAa/Gb7p37vkjWStk5BKpRg1SNZtssFNEOXR4ph\n"
                + "jFf3boUQ7A1i9e+eYJAmbGalwwotY1zxdr5kfaWYxIiMSaPHHwPfe/pnY1RF6lOs\n"
                + "LlegPw6xxg08LETaU+M9QCJ9EodXDEb19/KwINer/Cduou7TdVDFPYY02lMoj7Wr\n"
                + "vFgu90fRL/EmsMgN6dB9pBS6GbJK5e8E4lpgKacuXdCf3eHWMz/4MGKxXEzXTv4k\n"
                + "jvR067xjZmtvO70iTQgmL54w1YdLO5oU1yYlyXJ/z0iBEM5TG4nESoTWkILZDLcq\n"
                + "jyCPfzdXAgMBAAECggEAEWKPcvNTK48/NsG1Na8pEucKYXk4UMvHkZarPvZXdPxB\n"
                + "Q6wJ3akPxTG+E7DVtFX8XPb69GHINwczYwJYKQ/k7Hn16OpgQOHtQta+z8krFtX0\n"
                + "t9E2eIWqFLEDAt2XVdPVt5+3P5IFCPTeHEqheT0MnXO9xOROz9c3V17ppubc+S+J\n"
                + "fm5gme7i+nK5tU3r+SFl9BY9YKHHIXamggUJioVtpSQHrZEK6ygBxyKrdKGbS2Ii\n"
                + "XTvlsvKXePkGUBiKZ6dqkZwPIRFrExObrrGuxp3l6ajTZe5l4/Q8amG6yVvwhOyG\n"
                + "ASLKr+9TouErnLjty+uGpWgn+YvIdzrligq5dnPmIQKBgQDKNKRByiikoAbvvrmh\n"
                + "FHelRegG9wsHAe07L0hPgoObj1iq/HKocNpZtu6Qqzm1cAQ+oinACDbf699xIUWC\n"
                + "V7jEou/d4kdcbJ9b4o5fMj9YNc6XVO34ce9sHkD1CSsOWlgtlfwui2c0y5H2buUw\n"
                + "dBY9OVRbCQMvgL+UZMmbsHxPsQKBgQDJNb5syiJyWOwI3wZuvVXDAzOXoq7Yn2lP\n"
                + "E047wPM4d6F3dVBIPlyremHdEhYpUD4mP7d2bO9e9+o0deMqh7vJP/JlangZSUYD\n"
                + "qXmMpNBwVcZ/n4rfkMInWLy7r1XYgQJ1L1+rVnbTLQPrDa/4+JO5IyBAPTAftXQ1\n"
                + "xaevCZmBhwKBgHCtdPrUVGGoazUd6wNADIwksG9xKsv03uWkK39jE0OUVayykJIc\n"
                + "kRB9R+OGBtp8WWEtrGY+LZYKMrEwATPo/iVVRqU2et2eCg+B6CRUM8hL85uQ0Csq\n"
                + "EmkFUt05Bq0w2wJMGgM124UoC2Zv1XdyuRHU6JTyKLxH2nouz8naRuuBAoGANPws\n"
                + "GyXXkFkOPv/MB9lf/iyXp3S1qmHAL4yb62xSICqQoI6KB5w0dwuRPdAHefWhiBz7\n"
                + "SPpCxrVuPUZV/dskfkiSolY5Lh93intUgM7d/Nb5oJ34ygqqtgXOHXZ8mrjOVuGU\n"
                + "xd/NBqsx/vHpxxxeekBfu8rhI1h7M7XLBHL4s30CgYEAnmwpLxK36H4fyg3FT5uS\n"
                + "nCJuWFIP8LvWaPm8iDUJ45flOPbXokoV+UZbe7A+TK6tdyTXtfoM59/IsLtiEp7n\n"
                + "VuE9osST2ZOTD+l10ukIcjJJgI/Pwjtd36EGXyGftdAtT4sFMRxP4sGSXZodqHrZ\n"
                + "T9fE4yY/E4FyzS7yMeoXIyo="
        ),
        rsa_pss_rsae_sha512(
                "RSA",
                /**
                 * Signature Algorithm: rsassaPss
                 * Issuer: CN = root
                 * Validity Not Before: Jun 7 08:05:27 2018 GMT
                 * Not After : Jun 2 08:05:27 2038 GMT
                 * Subject: CN = root
                 * Public Key Algorithm: rsassaPss
                 */
                "-----BEGIN CERTIFICATE-----\n"
                + "MIIDXDCCAhOgAwIBAgIUUpFAFfaMrxI32lmRz/42b2V23C8wPgYJKoZIhvcNAQEK\n"
                + "MDGgDTALBglghkgBZQMEAgOhGjAYBgkqhkiG9w0BAQgwCwYJYIZIAWUDBAIDogQC\n"
                + "AgC+MA8xDTALBgNVBAMMBHJvb3QwHhcNMTgwNjA3MDgwNTI3WhcNMzgwNjAyMDgw\n"
                + "NTI3WjAPMQ0wCwYDVQQDDARyb290MIIBIDALBgkqhkiG9w0BAQoDggEPADCCAQoC\n"
                + "ggEBAMDE8lSB3tzeRr1GW/A1hU+n1zErrqNBjOm/mW2k+eUqfRnCnga9Li82/rA0\n"
                + "5nSxWb9vHY4M7dr82lhNBt/AdQB7nkDmD1BDWnuQuDTrhIFTvHjh802KUJT8sz/G\n"
                + "xXMXYxvK7kQPIuW+1xNxZirwnwLLZ7uo7WlvFqgMsYPJQ2+qREWRBgsj2BiDs90x\n"
                + "2Cc7YqRBAMglmT/NsqJoK7RZqAfhLEn1KYSkou7C4Fx32qc4RQGuUQzfTXMX1UGT\n"
                + "fjU1i7IcY0eY1Ed+VFPUI6Kl/aeFSfal+/Lz2siq8CT2fLxXwGtM/YtaOdQhCuvV\n"
                + "ekmntL7KalQS32f681qaccFTmSUCAwEAAaNQME4wHQYDVR0OBBYEFM1g6dCfIEe1\n"
                + "3kE2spDLZe4xbN3KMB8GA1UdIwQYMBaAFM1g6dCfIEe13kE2spDLZe4xbN3KMAwG\n"
                + "A1UdEwQFMAMBAf8wPgYJKoZIhvcNAQEKMDGgDTALBglghkgBZQMEAgOhGjAYBgkq\n"
                + "hkiG9w0BAQgwCwYJYIZIAWUDBAIDogQCAgC+A4IBAQAMa7OlZheye0Km/f4+95St\n"
                + "WXWYUhMuBHo2G/goFhCFEYtHiJa2B/e+14lPq+95xMLJ8kQRxQolG/brOIDeUwdx\n"
                + "85Pqfx50UnoZUhE+d5iJAJPHiS5aUoYa1Nxu7pw7eC402imkZt/P5OmPU1B9LTql\n"
                + "Mizo7guIVyXOXE11iOSFXfUk3q0XxErBzEIQmZDQjbv2idWPuTJV5G/Mo3dm+Oiq\n"
                + "6JTWKzCMp2pMFvUllP0FNEzIc1Q5qTkwFCzf8d5NLVa143kWuU0G9VbAjAJaEEfa\n"
                + "PUlfJQzkEKn0MsGWkpYMepbF4z7FB37jrLaHgHZvxpX1dXg8NWOqBXGSD154hzpf\n"
                + "-----END CERTIFICATE-----\n",
                /**
                 * Signature Algorithm: rsassaPss
                 * Issuer: CN = root
                 * Validity Not Before: Jun 7 08:05:27 2018 GMT
                 * Not After : Jun 2 08:05:27 2038 GMT
                 * Subject: CN = localhost
                 * Public Key Algorithm: rsaEncryption
                 */
                "-----BEGIN CERTIFICATE-----\n"
                + "MIID8DCCAqegAwIBAgIUP97IqgwiXunHNhSojyU10fxkvz0wPgYJKoZIhvcNAQEK\n"
                + "MDGgDTALBglghkgBZQMEAgOhGjAYBgkqhkiG9w0BAQgwCwYJYIZIAWUDBAIDogQC\n"
                + "AgC+MA8xDTALBgNVBAMMBHJvb3QwHhcNMTgwNjA3MDgwNTI3WhcNMzgwNjAyMDgw\n"
                + "NTI3WjAUMRIwEAYDVQQDDAlsb2NhbGhvc3QwggEiMA0GCSqGSIb3DQEBAQUAA4IB\n"
                + "DwAwggEKAoIBAQDCyeGmgpaHoXnRcsuhMhzsoinxDqSCHfJB62g0HuZDpZG3yjlE\n"
                + "U9zVTLeuCtWsrQnC0LCaNODjjvE9vI1tbY5L7B1pz3JNHrASzituzd3vPNlKjX5f\n"
                + "EUG4dBEOIx0UvwTDlf8taL897aLRmUHKE29qPV3Xo80M794CdQsUSq/sNQDkE1qF\n"
                + "m7MAmznXTS++RUqtofyz4W570KBwfM9pO4hFd20JvjkumadXY1dJbt99LyO3LVZd\n"
                + "QsztBe5meWElFcQJj/GdkSGFlrEGpePgwQ6U5MCrWDQonX1JX3aIiby4EFZrvdCK\n"
                + "r15pBpCEowoV57KDIzkROV2vRyYfjO+E5AUHAgMBAAGjgdwwgdkwCQYDVR0TBAIw\n"
                + "ADALBgNVHQ8EBAMCA/gwEwYDVR0lBAwwCgYIKwYBBQUHAwEwHQYDVR0OBBYEFIZl\n"
                + "PBtklckudi3unnRw45td3lYOMEoGA1UdIwRDMEGAFM1g6dCfIEe13kE2spDLZe4x\n"
                + "bN3KoROkETAPMQ0wCwYDVQQDDARyb290ghRSkUAV9oyvEjfaWZHP/jZvZXbcLzAR\n"
                + "BglghkgBhvhCAQEEBAMCBkAwLAYJYIZIAYb4QgENBB8WHU9wZW5TU0wgR2VuZXJh\n"
                + "dGVkIENlcnRpZmljYXRlMD4GCSqGSIb3DQEBCjAxoA0wCwYJYIZIAWUDBAIDoRow\n"
                + "GAYJKoZIhvcNAQEIMAsGCWCGSAFlAwQCA6IEAgIAvgOCAQEAEAhEFgf7PkNrqB/e\n"
                + "IWs6LbXmSWS6h9iv/7m42AsxslBXqvyFLLmKcl8g1yowgUPP3Wp4tBWU8F5OHgbQ\n"
                + "KdGJVXsrw+5Dlj5joXUisiEnG10nDr+i6qMx8k0U+K2qzBPTyAGkK5CadY04AquX\n"
                + "3EBWnUIzXSX+5quBR94g+okKC+lyiwtoq7uJrIA4I4buikgsKWHNIU/3DqcI54fb\n"
                + "ty1bWgW1CP48dShs0phZs8k716L0J9Gfz1fkhb1czAZikcecitt5GrbAUj9GDq1Q\n"
                + "1zjvjQiPXaZFXiDVmUUEVUsVDFtEuwiMKRxpprXEWklbQnOcWONCRy3113DdWauX\n"
                + "ECtlAw==\n"
                + "-----END CERTIFICATE-----\n",
                //
                // Private key.
                //
                "MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQDCyeGmgpaHoXnR\n"
                + "csuhMhzsoinxDqSCHfJB62g0HuZDpZG3yjlEU9zVTLeuCtWsrQnC0LCaNODjjvE9\n"
                + "vI1tbY5L7B1pz3JNHrASzituzd3vPNlKjX5fEUG4dBEOIx0UvwTDlf8taL897aLR\n"
                + "mUHKE29qPV3Xo80M794CdQsUSq/sNQDkE1qFm7MAmznXTS++RUqtofyz4W570KBw\n"
                + "fM9pO4hFd20JvjkumadXY1dJbt99LyO3LVZdQsztBe5meWElFcQJj/GdkSGFlrEG\n"
                + "pePgwQ6U5MCrWDQonX1JX3aIiby4EFZrvdCKr15pBpCEowoV57KDIzkROV2vRyYf\n"
                + "jO+E5AUHAgMBAAECggEABOg3Eg3KIwKTYg5lSNtNVTzEl7kJtelxN+3pQx7gKCYc\n"
                + "pKeoh6shLhJvsie9uErnqwu81zWr0K/CLg749R/EbO820nqSY5T5VI/zEiiHhcZf\n"
                + "pvwnideSc0YhQ9zol6Q0R4UY15kC8FlzN5qHyMJylReUrKEIwmGskx1FuS4kfmvQ\n"
                + "JYx7pzrVYAVtktIEz7BSsN9t0Gigtj20DoT8g9W+pcXRxmiLzJ3pYOe8n7oHcZGf\n"
                + "cPzT4IYWIHeFfzX6XPoNMUocPU94P/POlRAKYv3vRPqFhHjPq/WCWGhGvAQ7vzKT\n"
                + "UNCuFP1FyDGTsD6S/qsBxJ5f8WGYncJAxv6iR0hVOQKBgQDp3nFoEiDdSvfTAabW\n"
                + "64y+pp6U1VxjecR9VOpiUQOCriXefyyqgJIVtQI3CM8igMenHQtFBkcQB+GXnl9I\n"
                + "2d3a/AOWozFdWWEf+VOcHSo0WY2tqs2WSgiTPzThMLXAfqF0e5SK9nOw5Y8YcQDJ\n"
                + "bn+5n7rtjxq3O7SBXGH7sGUJRQKBgQDVOLQ2aSupYraJX7bXFdcI0X/3KuUBQnyp\n"
                + "qcDIc4Ho00DhdjTM/Nf3iRZOY0Fv1GOb1Dzp5pKCCuAeiYpEfgNR0aYrAABRrL4+\n"
                + "lOZyFvXcueZFPeSuzm17fyNtwgwnG8RlDmXqTSlqnIfEs+wti1cojI2Tpdq8QnCa\n"
                + "VcnYFnSr2wKBgANj3BT8HknW6ly+q2J2K6Yf2DCkHyC6BSUj8/nU3s4oJBhjk4wt\n"
                + "LPDvnMabdBU19K7xdtZbTvBmjNibzRnLPrIL8Slf2DlYMFY8UP/0VEZJ/gnEbhJ9\n"
                + "pD4uLmANSrUtoL2FhRO2mtq3mSlrie1hkqxoKleDOYnqbaVqZ2k0l2JZAoGBALi2\n"
                + "JLg2J9LXZxZeyoBNtTk4dEjk6fpLZL9+BTohhlryF3S5+EfUiiswoRhLN/bu4VOv\n"
                + "aw2d5zGsxjbuI8/t8mZA3ljF6YDXyv9f8rrHVTpf+THmymL9BS9FFqYQwoJmtZ5t\n"
                + "+LAfJE/tRliLHYDfAyRnjoZn2bPZQr8Qroj5+xydAoGACHEc7aoEwZ/z8cnrHuoT\n"
                + "T0QUw6HNlic5FgK+bZy7SXj9SqJBkWADzRJ/tVDopWT/EiDiGeqNP/uBPYt6i7m2\n"
                + "SoqCLYdGDIbUFyDQg3zC48IXlHyEF3whx0bg/0hoKs/E/rXuxYIH/10aEwmb0FQO\n"
                + "GA3T726uW8XrrTssMkhzixU="
        ),
        rsa_pss_pss_sha384(
                "RSASSA-PSS",
                /**
                 * Signature Algorithm: rsassaPss
                 * Issuer: CN = localhost
                 * Validity Not Before: Jun 7 08:06:30 2018 GMT
                 * Not After : Jun 2 08:06:30 2038 GMT
                 * Subject: CN = localhost
                 * Public Key Algorithm: rsassaPss
                 */
                "-----BEGIN CERTIFICATE-----\n"
                + "MIIDZjCCAh2gAwIBAgIUA5CP0rop4wp/QliNQrIiWzepcncwPgYJKoZIhvcNAQEK\n"
                + "MDGgDTALBglghkgBZQMEAgKhGjAYBgkqhkiG9w0BAQgwCwYJYIZIAWUDBAICogQC\n"
                + "AgDOMBQxEjAQBgNVBAMMCWxvY2FsaG9zdDAeFw0xODA2MDcwODA2MzBaFw0zODA2\n"
                + "MDIwODA2MzBaMBQxEjAQBgNVBAMMCWxvY2FsaG9zdDCCASAwCwYJKoZIhvcNAQEK\n"
                + "A4IBDwAwggEKAoIBAQDP7X9JUp1rPzcxyzpHOS4jJm/AOhgJC5ueK54wn+ZwxIk4\n"
                + "hAjHGDGsUIfxzDmMW7nCxvmw/BV0xfAqCdLRYd1Id9cMuNiPYiDu/LC10rMRbdFl\n"
                + "0GQvnaqXGLb0EJETJiEWiYemljswr4E3hzeKk6Z2dXD4WhUl9M+4ACjB5hGDTUD0\n"
                + "sKYMMtjRfz1TVUtgnbzqHMc0dZFY7BVCIoOGDtXpZbQTHeuMFt+X6WWoYbpe62S/\n"
                + "+lPe1Jj4WqQyRaiJ9wBUvF1jxZughLfggTUH+bIFqKEPTsZTY/GF5Tw0devRbkjr\n"
                + "7iiwKWZUqrw0gQNvfpMHkJ20vl88NIUTskx+ehXRAgMBAAGjUDBOMB0GA1UdDgQW\n"
                + "BBTk0W52uedFo2XRMmO+vjCb1HxgVTAfBgNVHSMEGDAWgBTk0W52uedFo2XRMmO+\n"
                + "vjCb1HxgVTAMBgNVHRMEBTADAQH/MD4GCSqGSIb3DQEBCjAxoA0wCwYJYIZIAWUD\n"
                + "BAICoRowGAYJKoZIhvcNAQEIMAsGCWCGSAFlAwQCAqIEAgIAzgOCAQEAMubCeSw/\n"
                + "XnF+EYmLt3Vj/1CRbqCG46VUYYGqFsIDB36q2Vate2UMeOMbBXNldNLWS6QWHPhv\n"
                + "wJR72OcviVh7twH3qDd6jBzkt6jiwXyeSZeCu7g8p6RhtoUT7pxSm/f7aQ+SQRNT\n"
                + "+XX1E4pakzEmyvCzyFzm6i7ol/KhZKI3bycUNMmNldE/TmDAwQLI2I6ld/jlQ4Yl\n"
                + "qQd2af1P7T19898EkA0Ka1LYRVNzlAz40rQc9Nw5etq2vuXMg9U229KZvT6qikWD\n"
                + "4V4xzTMfn2l+yfrHU6XGUawJoDds1dy03hrveYH90PKboQaPEOAn3MPjNlZF21Dl\n"
                + "Ukj3JA+ZxAZmcw==\n"
                + "-----END CERTIFICATE-----\n",
                //
                // Private key.
                //
                "MIIEvAIBADALBgkqhkiG9w0BAQoEggSoMIIEpAIBAAKCAQEAz+1/SVKdaz83Mcs6\n"
                + "RzkuIyZvwDoYCQubniueMJ/mcMSJOIQIxxgxrFCH8cw5jFu5wsb5sPwVdMXwKgnS\n"
                + "0WHdSHfXDLjYj2Ig7vywtdKzEW3RZdBkL52qlxi29BCREyYhFomHppY7MK+BN4c3\n"
                + "ipOmdnVw+FoVJfTPuAAoweYRg01A9LCmDDLY0X89U1VLYJ286hzHNHWRWOwVQiKD\n"
                + "hg7V6WW0Ex3rjBbfl+llqGG6Xutkv/pT3tSY+FqkMkWoifcAVLxdY8WboIS34IE1\n"
                + "B/myBaihD07GU2PxheU8NHXr0W5I6+4osClmVKq8NIEDb36TB5CdtL5fPDSFE7JM\n"
                + "fnoV0QIDAQABAoIBAGB1eh5G0DaHnhBgikmuUiQGWcNgb/QKSYgoDfvawinAUzQ/\n"
                + "tF7Ab5LTzS00I+JkTxn3+q/LUhzZEqA97GosL17GEaKaQgTKbiLQYR4If//u5TyJ\n"
                + "X2DjkNcFpSI2aUbr4l+1L5Ptj8n3MUfUV8TW2FuOAfmEuNjh6Fcg48eH9snk/wTI\n"
                + "8yvQPODnEaW/9MucySd0LJK3YX+rRa5EG3kJry+FmdMBzaLrCOInq/j1P7wXmlBc\n"
                + "6Jpynhu/HzYjbk9s3xqUHckX1+DLfzpIEFqXJF0YjswiV2EEP2YLt2OYDP4X8M2X\n"
                + "zfZ4l2ArHcfPZwy2/chui+VAm4bNPsYjDeqMHC0CgYEA+6ujKrhYx8mjiaqmYStR\n"
                + "KZvmy5sL3GQEpZ3y6s7JNKW+h6z75USlRQYRGG1L9DRyEtfkuYzvzZwVR+ermGJb\n"
                + "XvHIBDdgDmurORHqYGVywfqfo+5KM1GAMeDvBW0LeGoZIYOdno2q/lnNm1kX/g4d\n"
                + "VBMiurHeNkOdJXSgNc7EZ6cCgYEA04E3PxU12Xs3N2/ZoEYcAnEm6zbljt8fc/CG\n"
                + "gyRfGWb6RzIF9oKmLENhcNWpX+7hk2HsUZmQlmTofF7JwF/UBkySWclNXnO2OQwd\n"
                + "AGF+WXQwACVg3S2X/y5Pu+8JpaEJdyahuynSuBmxyjavFBTDkI02Heu5ZSwVN6Fp\n"
                + "tYDmxccCgYEA+Kfs3xilH0CqxCpHmVojJulSb3kRjv+DV99nU3hcdBgO2B6iAzR/\n"
                + "1mLYITpcATyQOO32nx4RESVWIWVUtYr4nCZnaUMNNTJMSmbZG8UgTWhCssWNqoas\n"
                + "EpwbjVDgNGkfy20vHqj6ebRg4Ux12B45/AesGKoE07iaW5ePc5qHk6ECgYBpeSS9\n"
                + "1qv1+pY8lRCn9o59QUQxRD0SFH6w6J+LwpWSK2JgIrgKiHip1ig/hq1iY9QmFU0u\n"
                + "HDCYb1Xov7RItQEc6w6Iq/RjR7z1ke7cg8HohiJx0DIP2m7UGJo2lCvxZu87dg5t\n"
                + "MZwdpuKcfsysbPZhnaoBHc5kf6lNBreahd+PfQKBgQDzZHFUndVfI28zn10ZzY8M\n"
                + "zVeUMn+m1Dhp/e4F0t3XPTWCkwlYI3bk0k5BWHYSdLb2R7gKhTMynxPUb53oeY6P\n"
                + "Pt/tA+dbjGHPx+v98wOorsD28qH0WG/V1xQdGRA9/dDZtJDolLqNn2B3t0QH9Kco\n"
                + "LsJldTLzMQSVP/05BAt6DQ=="
        ),
        rsa_pss_pss_sha512(
                "RSASSA-PSS",
                /**
                 * Signature Algorithm: rsassaPss
                 * Issuer: CN = localhost
                 * Validity Not Before: Jun 7 08:07:22 2018 GMT
                 * Not After : Jun 2 08:07:22 2038 GMT
                 * Subject: CN = localhost
                 * Public Key Algorithm: rsassaPss
                 */
                "-----BEGIN CERTIFICATE-----\n"
                + "MIIDZjCCAh2gAwIBAgIUCoYfs6/TS3M2xgZYjN2Ke4LPEOwwPgYJKoZIhvcNAQEK\n"
                + "MDGgDTALBglghkgBZQMEAgOhGjAYBgkqhkiG9w0BAQgwCwYJYIZIAWUDBAIDogQC\n"
                + "AgC+MBQxEjAQBgNVBAMMCWxvY2FsaG9zdDAeFw0xODA2MDcwODA3MjJaFw0zODA2\n"
                + "MDIwODA3MjJaMBQxEjAQBgNVBAMMCWxvY2FsaG9zdDCCASAwCwYJKoZIhvcNAQEK\n"
                + "A4IBDwAwggEKAoIBAQDG/9pbt7MT6Be3aUg94VKQ9BMojLYjBDzM7bd/y07+Lfdd\n"
                + "67IUhQ5VkJcxgs7Qtb8ztnUJQEzyLBkcZA/hvvpLUXJUP3UisRNV12MBfHUcFAVN\n"
                + "33pdm7X/UJDVadUZTAfe6PUIuk/+ZpOmTnwE/sEfhCZAtNwm+08EdysUhK9aVl83\n"
                + "vC+eEj22584Vzd2fQj9YYU1hN0OKWvavrHvc3XCjam15UCWBZa1ujXnjOtYkPDE6\n"
                + "0C/BS+WE/GtLGGc04O410/ezvs/M66RdlMFJRBLW3F9grL2oG41CmoLzFaPL6AOD\n"
                + "W2EYBf094uvreR7mDlr8MCmig4+IHCpdwmbbYrhjAgMBAAGjUDBOMB0GA1UdDgQW\n"
                + "BBSZnv73VPYJzx4m2n0xIYHRL+avKDAfBgNVHSMEGDAWgBSZnv73VPYJzx4m2n0x\n"
                + "IYHRL+avKDAMBgNVHRMEBTADAQH/MD4GCSqGSIb3DQEBCjAxoA0wCwYJYIZIAWUD\n"
                + "BAIDoRowGAYJKoZIhvcNAQEIMAsGCWCGSAFlAwQCA6IEAgIAvgOCAQEAeHFMR7gi\n"
                + "hcd7acEBc4Qw+JCH81+8PAwfCH+7uMbLronUDmHPgeMckNvGGEFenDDp+pqnpiFN\n"
                + "pKdDpelv4lkE72gFGVpEGlrCjVrwJSyPR26Dju9Nx+WQCQIiTUGd03fBgxb+rh5U\n"
                + "kvauleAa7Xb0TkpWawfe2RadfAwSHcdwYGVA5Rh1dTfn5Chz8fdt5y6tFcLnJpgY\n"
                + "tvHk5Z5hFTgsDpdTv/aKcUypsQDnBmCaAot3S9ks3FBea/g/ACrqi/Td8OaHr9/W\n"
                + "wLnBvPtd++XvAFMaZKrMfjIzlmQyHfLQ/4EA9PPOK9DqBkUcnvcSLH6TL57yoDB6\n"
                + "h6/Ppk90DH6vhA==\n"
                + "-----END CERTIFICATE-----\n",
                //
                // Private key.
                //
                "MIIEvAIBADALBgkqhkiG9w0BAQoEggSoMIIEpAIBAAKCAQEAxv/aW7ezE+gXt2lI\n"
                + "PeFSkPQTKIy2IwQ8zO23f8tO/i33XeuyFIUOVZCXMYLO0LW/M7Z1CUBM8iwZHGQP\n"
                + "4b76S1FyVD91IrETVddjAXx1HBQFTd96XZu1/1CQ1WnVGUwH3uj1CLpP/maTpk58\n"
                + "BP7BH4QmQLTcJvtPBHcrFISvWlZfN7wvnhI9tufOFc3dn0I/WGFNYTdDilr2r6x7\n"
                + "3N1wo2pteVAlgWWtbo154zrWJDwxOtAvwUvlhPxrSxhnNODuNdP3s77PzOukXZTB\n"
                + "SUQS1txfYKy9qBuNQpqC8xWjy+gDg1thGAX9PeLr63ke5g5a/DApooOPiBwqXcJm\n"
                + "22K4YwIDAQABAoIBADoeFuOabs5thh+mu9Z2q+pxnfbFwZvQbQFcm67S7asGOaxQ\n"
                + "XZ3ojhsnM0DedxA1RDYSH3QoN1Cy2FKWVp0TbX35t24rakZLeN4lHWEdvAYLQtFP\n"
                + "ZylXhHugR+xMEFRnBBVx6740y4/83TpAya+bx0MxEQrsxy8LTjR7qTVA2wWCmDur\n"
                + "n2wzt7axGGBWq0/4n62aRszhAYDVIa6EXI2UAcGQwdWuw4iiVRjdEVc6I/W3GA62\n"
                + "a+RlhA1LPcNkAiONVoVhBCFxSZq+ERuupS31mVjbGEDlOUDw0XfrJb3n4ekXbN4C\n"
                + "6EHCbBA6DPr9FrMT6dgqpTfnsF26rQp2kgLrngECgYEA6UpueG7YvwQxi+vq3tGS\n"
                + "7FldARE2obWrGDVbm+WA1TlnOwQDyCZPTSYFtNkBoqZtFzVZsM8mjYu8vDnrCuVN\n"
                + "DmthBwbH2LYQXVXeZhaZVCF6QWP77OWLrlEVlhbHHG0xdgzgP5cNJWe56JpQZW3C\n"
                + "GIbmmsSE8jIYgjh5MBAxlSMCgYEA2l7jGeUw/U017FRg/mTFbLM74Tc7/GHyVinF\n"
                + "MouM+v+erXoNRXL6KH43RoHbiFhfDD9e67pu2O0BsHn2BUV5GKAi4hcyoYqi1h7h\n"
                + "oGtk1DyFCcOV+6NzUqe72RrTv1868K/j2sRmfqtU3gNlKFId+CUVYGUEjK2pNDHd\n"
                + "IvqEo8ECgYApqoKKffm2PBCBVhRv0Wx1TAyhWSqxvRmezEDdWiMlcggu8Sufvr/h\n"
                + "Ho5cW3nATAsl3wBy5LyVAUUnNQz2uDeIAMOmlp5w5SuND/4Vq6mc7hHAxhPDnsfQ\n"
                + "zWiWkuDjAdmYpPoUQW02pgz9Lzp2syC8crOTJtA71ZitAVsbq3i/kwKBgQC550ft\n"
                + "drnTGxVKAbelO0L7vEbBABXYUcZOZjcURcuar112EE8WDcE8Ed+a7dhoZdtdAOId\n"
                + "StUtZfAnPl0ctb1XIpUv51HaRr1EDnxE5sirCm60FkcsOEVoW5XHSVh1NmxmFUek\n"
                + "qckcE14nt7o5rlcHNwLQ0o8h+IHxBnZdXernwQKBgQCunOWvYd7MGlIjpkl6roq7\n"
                + "MrQ/zaAkUyGsTgBxEu3p5+x2ENG6SukEtHGGDE6TMxlcKdTSohL2lXZX2AkP+eXf\n"
                + "sF3h66iQ8DrGrYoyCgx3KTx3G+KPfblAqwDMTqj2G5fAeWDwXrpEacpTXiFZvNAn\n"
                + "KtqEurWf+mUeJVzLj1x1BA=="
        );

        private final String keyType;
        private final String trustedCert;
        private final String endCert;
        private final String privateKey;

        private KeyType(String keyType, String trustedCert, String endCert,
                String privateKey) {
            this.keyType = keyType;
            this.trustedCert = trustedCert;
            this.endCert = endCert;
            this.privateKey = privateKey;
        }

        private KeyType(String keyType, String selfCert, String privateKey) {
            this(keyType, selfCert, selfCert, privateKey);
        }

        public String getKeyType() {
            return keyType;
        }

        public String getTrustedCert() {
            return trustedCert;
        }

        public String getEndCert() {
            return endCert;
        }

        public String getPrivateKey() {
            return privateKey;
        }
    }
}
