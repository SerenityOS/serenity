/*
 * Copyright (c) 2010, 2016, Oracle and/or its affiliates. All rights reserved.
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

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509ExtendedKeyManager;
import javax.net.ssl.X509TrustManager;

/**
 * Test that all ciphersuites work in all versions and all client authentication
 * types. The way this is setup the server is stateless and all checking is done
 * on the client side.
 */

public class CipherTestUtils {

    public static final int TIMEOUT = 20 * 1000;
    public static final SecureRandom secureRandom = new SecureRandom();
    public static char[] PASSWORD = "passphrase".toCharArray();
    private static final List<TestParameters> TESTS = new ArrayList<>(3);
    private static final List<Exception> EXCEPTIONS
            = Collections.synchronizedList(new ArrayList<>(1));

    private static final String CLIENT_PUBLIC_KEY
        = "-----BEGIN CERTIFICATE-----\n"
        + "MIICtTCCAh4CCQDkYJ46DMcGRjANBgkqhkiG9w0BAQUFADCBnDELMAkGA1UEBhMC\n"
        + "VVMxCzAJBgNVBAgMAkNBMRYwFAYDVQQHDA1Nb3VudGFpbiBWaWV3MR8wHQYDVQQK\n"
        + "DBZTdW4gTWljcm9zeXN0ZW1zLCBJbmMuMSYwJAYDVQQLDB1TdW4gTWljcm9zeXN0\n"
        + "ZW1zIExhYm9yYXRvcmllczEfMB0GA1UEAwwWVGVzdCBDQSAoMTAyNCBiaXQgUlNB\n"
        + "KTAeFw0wOTA0MjcwNDA0MDhaFw0xMzA2MDUwNDA0MDhaMIGgMQswCQYDVQQGEwJV\n"
        + "UzELMAkGA1UECAwCQ0ExFjAUBgNVBAcMDU1vdW50YWluIFZpZXcxHzAdBgNVBAoM\n"
        + "FlN1biBNaWNyb3N5c3RlbXMsIEluYy4xJjAkBgNVBAsMHVN1biBNaWNyb3N5c3Rl\n"
        + "bXMgTGFib3JhdG9yaWVzMSMwIQYDVQQDDBpUZXN0IENsaWVudCAoMTAyNCBiaXQg\n"
        + "UlNBKTCBnzANBgkqhkiG9w0BAQEFAAOBjQAwgYkCgYEAm5rwjmhO7Nwd5GWs+KvQ\n"
        + "UnDiqpRDvRriOUFdF0rCI2Op24C+iwUMDGxPsgP7VkUpOdJhw3c72aP0CAWcZ5dN\n"
        + "UCW7WVDAxnogCahLCir1jjoGdEjiNGOy0L9sypsM9UvBzJN8uvXsxsTZX4Z88cKU\n"
        + "G7RUvN8LQ88zDljk5zr3c2MCAwEAATANBgkqhkiG9w0BAQUFAAOBgQA7LUDrzHln\n"
        + "EXuGmwZeeroACB6DVtkClMskF/Pj5GnTxoeNN9DggycX/eOeIDKRloHuMpBeZPJH\n"
        + "NUwFu4LB6HBDeldQD9iRp8zD/fPakOdN+1Gk5hciIZZJ5hQmeCl7Va2Gr64vUqZG\n"
        + "MkVU755t+7ByLgzWuhPhhsX9QCuPR5FjvQ==\n"
        + "-----END CERTIFICATE-----";

    private static final String CLIENT_PRIVATE_KEY
        = "-----BEGIN PRIVATE KEY-----\n"
        + "MIICdgIBADANBgkqhkiG9w0BAQEFAASCAmAwggJcAgEAAoGBAJua8I5oTuzcHeRl\n"
        + "rPir0FJw4qqUQ70a4jlBXRdKwiNjqduAvosFDAxsT7ID+1ZFKTnSYcN3O9mj9AgF\n"
        + "nGeXTVAlu1lQwMZ6IAmoSwoq9Y46BnRI4jRjstC/bMqbDPVLwcyTfLr17MbE2V+G\n"
        + "fPHClBu0VLzfC0PPMw5Y5Oc693NjAgMBAAECgYA5w73zj8Nk6J3sMNaShe3S/PcY\n"
        + "TewLopRCnwI46FbDnnbq9pNFtnzvi7HWKuY983THc1M5peTA+b1Y0QRr7F4Vg4x9\n"
        + "9UM0B/tZcIIcJJ3LS+9fXKCbYLQWq5F05JqeZu+i+QLmJFO5+2p7laeQ4oQfW7QE\n"
        + "YR4u2mSaLe0SsqHvOQJBAMhgcye9C6pJO0eo2/VtRxAXI7zxNAIjHwKo1cva7bhu\n"
        + "GdrMaEAJBAsMJ1GEk7/WDI+3KEbTjQdfIJuAvOR4FXUCQQDGzNn/tl2k93v/ugyM\n"
        + "/tBhCKDipYDIbyJMoG2AOtOGmCsiGo5L7idO4OAcm/QiHBQMXjFIVgTUcH8MhGj4\n"
        + "blJ3AkA5fUqsxRV6tuYWKkFpif/QgwMS65VDY7Y6+hvVECwSNSyf1PO4I54QWV1S\n"
        + "ixok+RHDjgY1Q+77hXSCiQ4o8rcdAkBHvjfR+5sx5IpgUGElJPRIgFenU3j1XH3x\n"
        + "T1gVFaWuhg3S4eiGaGzRH4BhcrqY8K8fg4Kfi0N08yA2gTZsqUujAkEAjuNPTuKx\n"
        + "ti0LXI09kbGUqOpRMm1zW5TD6LFeEaUN6oxrSZI2YUvu7VyotAqsxX5O0u0f3VQw\n"
        + "ySF0Q1oZ6qu7cg==\n"
        + "-----END PRIVATE KEY-----";
    private static final String SERVER_PUBLIC_KEY
        = "-----BEGIN CERTIFICATE-----\n"
        + "MIICtTCCAh4CCQDkYJ46DMcGRTANBgkqhkiG9w0BAQUFADCBnDELMAkGA1UEBhMC\n"
        + "VVMxCzAJBgNVBAgMAkNBMRYwFAYDVQQHDA1Nb3VudGFpbiBWaWV3MR8wHQYDVQQK\n"
        + "DBZTdW4gTWljcm9zeXN0ZW1zLCBJbmMuMSYwJAYDVQQLDB1TdW4gTWljcm9zeXN0\n"
        + "ZW1zIExhYm9yYXRvcmllczEfMB0GA1UEAwwWVGVzdCBDQSAoMTAyNCBiaXQgUlNB\n"
        + "KTAeFw0wOTA0MjcwNDA0MDhaFw0xMzA2MDUwNDA0MDhaMIGgMQswCQYDVQQGEwJV\n"
        + "UzELMAkGA1UECAwCQ0ExFjAUBgNVBAcMDU1vdW50YWluIFZpZXcxHzAdBgNVBAoM\n"
        + "FlN1biBNaWNyb3N5c3RlbXMsIEluYy4xJjAkBgNVBAsMHVN1biBNaWNyb3N5c3Rl\n"
        + "bXMgTGFib3JhdG9yaWVzMSMwIQYDVQQDDBpUZXN0IFNlcnZlciAoMTAyNCBiaXQg\n"
        + "UlNBKTCBnzANBgkqhkiG9w0BAQEFAAOBjQAwgYkCgYEArsHHeZ1O67yuxQKDSAOC\n"
        + "Xm271ViwBrXkxe5cvhG8MCCem6Z3XeZ/m6c2ucRwLaQxnmG1m0G6/OYaUXTivjcG\n"
        + "/K4bc1I+yjghAWQNLBtsOiP9w0LKibg3TSDehpeuuz/lmB5A4HMqQr8KkY4K7peD\n"
        + "1QkJ2Dn3zhbwQ/0d8f5CCbkCAwEAATANBgkqhkiG9w0BAQUFAAOBgQBOd8XojEnu\n"
        + "eTUHBwqfmnvRQvbICFDNbbL4KuX/JNPSy1WMGAEbNCTLZ+5yP69js8aUYqAk5vVf\n"
        + "dWRLU3MDiEzW7zxE1ubuKWjVuyGbG8Me0G01Hw+evBcZqB64Fz3OFISVfQh7MqE/\n"
        + "O0AeakRMH350FRLNl4o6KBSXmF/AADfqQQ==\n"
        + "-----END CERTIFICATE-----";

    private static final String SERVER_PRIVATE_KEY
        = "-----BEGIN PRIVATE KEY-----\n"
        + "MIICeAIBADANBgkqhkiG9w0BAQEFAASCAmIwggJeAgEAAoGBAK7Bx3mdTuu8rsUC\n"
        + "g0gDgl5tu9VYsAa15MXuXL4RvDAgnpumd13mf5unNrnEcC2kMZ5htZtBuvzmGlF0\n"
        + "4r43BvyuG3NSPso4IQFkDSwbbDoj/cNCyom4N00g3oaXrrs/5ZgeQOBzKkK/CpGO\n"
        + "Cu6Xg9UJCdg5984W8EP9HfH+Qgm5AgMBAAECgYAXUv+3qJo+9mjxHHu/IdDFn6nB\n"
        + "ONwNmTtWe5DfQWi3l7LznU0zOC9x6+hu9NvwC4kf1XSyqxw04tVCZ/JXZurEmEBz\n"
        + "YtcQ5idRQDkKYXEDOeVUfvtHO6xilzrhPKxxd0GG/sei2pozikkqnYF3OcP0qL+a\n"
        + "3nWixZQBRoF2nIRLcQJBAN97TJBr0XTRmE7OCKLUy1+ws7vZB9uQ2efHMsgwOpsY\n"
        + "3cEW5qd95hrxLU72sBeu9loHQgBrT2Q3OAxnsPXmgO0CQQDIL3u9kS/O3Ukx+n1H\n"
        + "JdPFQCRxrDm/vtJpQEmq+mLqxxnxCFRIYQ2ieAPokBxWeMDtdWJGD3VxhahjPfZm\n"
        + "5K59AkEAuDVl0tVMfUIWjT5/F9jXGjUIsZofQ/iN5OLpFOHMLPO+Nd6umPjJpwON\n"
        + "GT11wM/S+DprSPUrJ6vsYy1FTCuHsQJBAMXtnO07xgdE6AAQaRmVnyMiXmY+IQMj\n"
        + "CyuhsrToyDDWFyIoWB0QSMjg3QxuoHYnAqpGK5qV4ksSGgG13BCz/okCQQCRHTgn\n"
        + "DuFG2f7GYLFjI4NaTEzHGp+J9LiNYY1kYYLonpwAC3Z5hzJVanYT3/g23AUZ/fdF\n"
        + "v5PDIViuPo5ZB1eD\n"
        + "-----END PRIVATE KEY-----";

    private static final String CA_PUBLIC_KEY
        = "-----BEGIN CERTIFICATE-----\n"
        + "MIIDCDCCAnGgAwIBAgIJAIYlGfwNBY6NMA0GCSqGSIb3DQEBBQUAMIGcMQswCQYD\n"
        + "VQQGEwJVUzELMAkGA1UECAwCQ0ExFjAUBgNVBAcMDU1vdW50YWluIFZpZXcxHzAd\n"
        + "BgNVBAoMFlN1biBNaWNyb3N5c3RlbXMsIEluYy4xJjAkBgNVBAsMHVN1biBNaWNy\n"
        + "b3N5c3RlbXMgTGFib3JhdG9yaWVzMR8wHQYDVQQDDBZUZXN0IENBICgxMDI0IGJp\n"
        + "dCBSU0EpMB4XDTA5MDQyNzA0MDQwOFoXDTEzMDYwNTA0MDQwOFowgZwxCzAJBgNV\n"
        + "BAYTAlVTMQswCQYDVQQIDAJDQTEWMBQGA1UEBwwNTW91bnRhaW4gVmlldzEfMB0G\n"
        + "A1UECgwWU3VuIE1pY3Jvc3lzdGVtcywgSW5jLjEmMCQGA1UECwwdU3VuIE1pY3Jv\n"
        + "c3lzdGVtcyBMYWJvcmF0b3JpZXMxHzAdBgNVBAMMFlRlc3QgQ0EgKDEwMjQgYml0\n"
        + "IFJTQSkwgZ8wDQYJKoZIhvcNAQEBBQADgY0AMIGJAoGBAOK4DJxxb0XX6MJ1CVjp\n"
        + "9Gmr/Ua8MS12R58F9lDpSKuq8cFexA4W7OdZ4jtbKv0tRHX5YxmbnXedwS+gdcOA\n"
        + "GRgXMoeXlgTFGpdL+TR8xKIlMGRSjnR7MpR2tRyIYI2p+UTEiD6LTlIm5Wh4z1q8\n"
        + "LYbxyMVD1XNNNymvPM44OjsBAgMBAAGjUDBOMB0GA1UdDgQWBBT27BLUflmfdtbi\n"
        + "WTgjwWnoxop2MTAfBgNVHSMEGDAWgBT27BLUflmfdtbiWTgjwWnoxop2MTAMBgNV\n"
        + "HRMEBTADAQH/MA0GCSqGSIb3DQEBBQUAA4GBAEQELNzhZpjnSgigd+QJ6I/3CPDo\n"
        + "SDkMLdP1BHlT/DkMIZvABm+M09ePNlWiLYCNCsL9nWmX0gw0rFDKsTklZyKTUzaM\n"
        + "oy/AZCrAaoIc6SO5m1xE1RMyVxd/Y/kg6cbfWxxCJFlMeU5rsSdC97HTE/lDyuoh\n"
        + "BmlOBB7SdR+1ScjA\n"
        + "-----END CERTIFICATE-----";

    private static final String CA_PRIVATE_KEY
        = "-----BEGIN PRIVATE KEY-----\n"
        + "MIICdwIBADANBgkqhkiG9w0BAQEFAASCAmEwggJdAgEAAoGBAOK4DJxxb0XX6MJ1\n"
        + "CVjp9Gmr/Ua8MS12R58F9lDpSKuq8cFexA4W7OdZ4jtbKv0tRHX5YxmbnXedwS+g\n"
        + "dcOAGRgXMoeXlgTFGpdL+TR8xKIlMGRSjnR7MpR2tRyIYI2p+UTEiD6LTlIm5Wh4\n"
        + "z1q8LYbxyMVD1XNNNymvPM44OjsBAgMBAAECgYEApmMOlk3FrQtsvjGof4GLp3Xa\n"
        + "tmvs54FzxKhagj0C4UHelNyYpAJ9MLjNiGQ7I31yTeaNrUCAi0XSfsKTSrwbLSnJ\n"
        + "qsUPKMBrnzcWrOyui2+cupHZXaTlNeYB97teLJYpa6Ql9CZLoTHoim1+//s7diBh\n"
        + "03Vls+M6Poi5PMvv59UCQQD+k/BiokmbBgWHfBY5cZSlx3Z4VTwSHJmHDTO3Tjso\n"
        + "EVErXUSVvqD/KHX6eM4VPM8lySV5djWV8lDsESCWMtiLAkEA4/xFNsiOLMQpxW/O\n"
        + "bt2tukxJkAxldD4lPoFZR+zbXtMtt8OjERtX2wD+nj6h7jfIeSyVuBEcBN8Uj8xe\n"
        + "kgfgIwJAPbKG4LCqHAsCjgpRrIxNVTwZByLJEy6hOqzFathn19cSj+rjs1Lm28/n\n"
        + "f9OFRnpdTbAJB/3REM0QNZYVCrG57wJBAN0KuTytZJNouaswhPCew5Kt5mDgc/kp\n"
        + "S8j3dk2zCto8W8Ygy1iJrzuqEjPxO+UQdrFtlde51vWuKGxnVIW3VwsCQEldqk7r\n"
        + "8y7PgquPP+k3L0OXno5wGBrPcW1+U0mhIZGnwSzE4SPX2ddqUSEUA/Av4RjAckL/\n"
        + "fpqmCkpTanyYW9U=\n"
        + "-----END PRIVATE KEY-----";

    private final SSLSocketFactory factory;
    private final X509ExtendedKeyManager clientKeyManager;
    private final X509ExtendedKeyManager serverKeyManager;
    private final X509TrustManager clientTrustManager;
    private final X509TrustManager serverTrustManager;

    static abstract class Server implements Runnable, AutoCloseable {

        final CipherTestUtils cipherTest;

        Server(CipherTestUtils cipherTest) throws Exception {
            this.cipherTest = cipherTest;
        }

        @Override
        public abstract void run();

        abstract int getPort();

        void handleRequest(InputStream in, OutputStream out)
                throws IOException {
            boolean newline = false;
            StringBuilder sb = new StringBuilder();
            while (true) {
                int ch = in.read();
                if (ch < 0) {
                    throw new EOFException();
                }
                sb.append((char) ch);
                if (ch == '\r') {
                    // empty
                } else if (ch == '\n') {
                    if (newline) {
                        // 2nd newline in a row, end of request
                        break;
                    }
                    newline = true;
                } else {
                    newline = false;
                }
            }
            String request = sb.toString();
            if (request.startsWith("GET / HTTP/1.") == false) {
                throw new IOException("Invalid request: " + request);
            }
            out.write("HTTP/1.0 200 OK\r\n\r\n".getBytes());
            out.write("Tested Scenario: ".getBytes());
            TestParameters tp = (TestParameters) CipherTestUtils.TESTS.get(0);
            out.write(tp.toString().getBytes());
            out.write(" Test PASSED.".getBytes());
        }
    }

    public static class TestParameters {

        final String cipherSuite;
        final String protocol;
        final String clientAuth;

        TestParameters(String cipherSuite, String protocol, String clientAuth) {
            this.cipherSuite = cipherSuite;
            this.protocol = protocol;
            this.clientAuth = clientAuth;
        }

        boolean isEnabled() {
            return true;
        }

        @Override
        public String toString() {
            String s = cipherSuite + " in " + protocol + " mode";
            if (clientAuth != null) {
                s += " with " + clientAuth + " client authentication";
            }
            return s;
        }
    }

    private static volatile CipherTestUtils instance = null;

    public static CipherTestUtils getInstance() throws Exception {
        if (instance == null) {
            synchronized (CipherTestUtils.class) {
                if (instance == null) {
                    instance = new CipherTestUtils();
                }
            }
        }
        return instance;
    }

    public static void setTestedArguments(String protocol, String ciphersuite) {
        ciphersuite = ciphersuite.trim();
        TestParameters params = new TestParameters(ciphersuite, protocol, null);
        TESTS.add(params);
    }

    public X509ExtendedKeyManager getClientKeyManager() {
        return clientKeyManager;
    }

    public X509TrustManager getClientTrustManager() {
        return clientTrustManager;
    }

    public X509ExtendedKeyManager getServerKeyManager() {
        return serverKeyManager;
    }

    public X509TrustManager getServerTrustManager() {
        return serverTrustManager;
    }

    public static void addFailure(Exception e) {
        EXCEPTIONS.add(e);
    }

    private CipherTestUtils() throws Exception {
        factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
        KeyStore serverKeyStore = createServerKeyStore(SERVER_PUBLIC_KEY,
                SERVER_PRIVATE_KEY);
        KeyStore serverTrustStore = createServerKeyStore(CA_PUBLIC_KEY,
                CA_PRIVATE_KEY);

        if (serverKeyStore != null) {
            KeyManagerFactory keyFactory = KeyManagerFactory.getInstance(
                            KeyManagerFactory.getDefaultAlgorithm());
            keyFactory.init(serverKeyStore, PASSWORD);
            serverKeyManager = (X509ExtendedKeyManager)
                    keyFactory.getKeyManagers()[0];
        } else {
            serverKeyManager = null;
        }
        serverTrustManager = serverTrustStore != null
                ? new AlwaysTrustManager(serverTrustStore) : null;

        KeyStore clientKeyStore, clientTrustStore;
        clientTrustStore = serverTrustStore;
        clientKeyStore =
                createServerKeyStore(CLIENT_PUBLIC_KEY,CLIENT_PRIVATE_KEY);
        if (clientKeyStore != null) {
            KeyManagerFactory keyFactory = KeyManagerFactory.getInstance(
                            KeyManagerFactory.getDefaultAlgorithm());
            keyFactory.init(clientKeyStore, PASSWORD);
            clientKeyManager = (X509ExtendedKeyManager)
                    keyFactory.getKeyManagers()[0];
        } else {
            clientKeyManager = null;
        }
        clientTrustManager = (clientTrustStore != null)
                ? new AlwaysTrustManager(clientTrustStore) : null;
    }

    void checkResult(String exception) throws Exception {
        if (EXCEPTIONS.size() >= 1) {
            Exception actualException = EXCEPTIONS.get(0);
            if (exception == null) {
                throw new RuntimeException("FAILED: got unexpected exception: "
                        + actualException);
            }
            if (!exception.equals(actualException.getClass().getName())) {
                throw new RuntimeException("FAILED: got unexpected exception: "
                        + actualException);
            }

            System.out.println("PASSED: got expected exception: "
                    + actualException);
        } else {
            if (exception != null) {
                throw new RuntimeException("FAILED: " + exception
                        + " was expected");
            }
            System.out.println("PASSED");
        }
    }

    SSLSocketFactory getFactory() {
        return factory;
    }

    static abstract class Client implements Runnable {

        final CipherTestUtils cipherTest;
        TestParameters testedParams;

        Client(CipherTestUtils cipherTest) throws Exception {
            this.cipherTest = cipherTest;
        }

        Client(CipherTestUtils cipherTest, String testedCipherSuite)
                throws Exception {
            this.cipherTest = cipherTest;
        }

        @Override
        public final void run() {

            TESTS.stream().map((params) -> {
                if (!params.isEnabled()) {
                    System.out.println("Skipping disabled test " + params);
                }
                return params;
            }).forEach((params) -> {
                try {
                    System.out.println("Testing " + params);
                    runTest(params);
                    System.out.println("Passed " + params);
                } catch (Exception e) {
                    CipherTestUtils.addFailure(e);
                    System.out.println("** Failed " + params
                            + "**, got exception:");
                    e.printStackTrace(System.out);
                }
            });
        }

        abstract void runTest(TestParameters params) throws Exception;

        void sendRequest(InputStream in, OutputStream out) throws IOException {
            out.write("GET / HTTP/1.0\r\n\r\n".getBytes());
            out.flush();
            StringBuilder sb = new StringBuilder();
            while (true) {
                int ch = in.read();
                if (ch < 0) {
                    break;
                }
                sb.append((char) ch);
            }
            String response = sb.toString();
            if (response.startsWith("HTTP/1.0 200 ") == false) {
                throw new IOException("Invalid response: " + response);
            } else {
                System.out.println();
                System.out.println("--- Response --- ");
                System.out.println(response);
                System.out.println("---------------- ");
            }
        }
    }

    public static void printStringArray(String[] stringArray) {
        System.out.println(Arrays.toString(stringArray));
        System.out.println();
    }

    public static void printInfo(SSLServerSocket socket) {
        System.out.println();
        System.out.println("--- SSL ServerSocket Info ---");
        System.out.print("SupportedProtocols    : ");
        printStringArray(socket.getSupportedProtocols());
        System.out.print("SupportedCipherSuites : ");
        printStringArray(socket.getSupportedCipherSuites());
        System.out.print("EnabledProtocols      : ");
        printStringArray(socket.getEnabledProtocols());
        System.out.print("EnabledCipherSuites   : ");
        String[] supportedCipherSuites = socket.getEnabledCipherSuites();
        Arrays.sort(supportedCipherSuites);
        printStringArray(supportedCipherSuites);
        System.out.println("NeedClientAuth        : "
                + socket.getNeedClientAuth());
        System.out.println("WantClientAuth        : "
                + socket.getWantClientAuth());
        System.out.println("-----------------------");
    }

    public static void printInfo(SSLSocket socket) {
        System.out.println();
        System.out.println("--- SSL Socket Info ---");
        System.out.print(" SupportedProtocols    : ");
        printStringArray(socket.getSupportedProtocols());
        System.out.println(" EnabledProtocols      : "
                + socket.getEnabledProtocols()[0]);
        System.out.print(" SupportedCipherSuites : ");
        String[] supportedCipherSuites = socket.getEnabledCipherSuites();
        Arrays.sort(supportedCipherSuites);
        printStringArray(supportedCipherSuites);
        System.out.println(" EnabledCipherSuites   : "
                + socket.getEnabledCipherSuites()[0]);
        System.out.println(" NeedClientAuth        : "
                + socket.getNeedClientAuth());
        System.out.println(" WantClientAuth        : "
                + socket.getWantClientAuth());
        System.out.println("-----------------------");
    }

    private static KeyStore createServerKeyStore(String publicKey,
            String keySpecStr) throws KeyStoreException, IOException,
            NoSuchAlgorithmException, CertificateException,
            InvalidKeySpecException {

        KeyStore ks = KeyStore.getInstance("JKS");
        ks.load(null, null);
        if (publicKey == null || keySpecStr == null) {
            throw new IllegalArgumentException("publicKey or "
                    + "keySpecStr cannot be null");
        }
        String strippedPrivateKey = keySpecStr.substring(
                keySpecStr.indexOf("\n"), keySpecStr.lastIndexOf("\n"));

        // generate the private key.
        PKCS8EncodedKeySpec priKeySpec = new PKCS8EncodedKeySpec(
                Base64.getMimeDecoder().decode(strippedPrivateKey));
        KeyFactory kf = KeyFactory.getInstance("RSA");
        RSAPrivateKey priKey
                = (RSAPrivateKey) kf.generatePrivate(priKeySpec);

        // generate certificate chain
        try (InputStream is = new ByteArrayInputStream(publicKey.getBytes())) {
            // generate certificate from cert string
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            Certificate keyCert = cf.generateCertificate(is);
            Certificate[] chain = {keyCert};
            ks.setKeyEntry("TestEntry", priKey, PASSWORD, chain);
        }

        return ks;
    }

    public static Server mainServer(PeerFactory peerFactory,
            String expectedException) throws Exception {

        setTestedArguments(peerFactory.getTestedProtocol(),
                peerFactory.getTestedCipher());

        System.out.print(
                " Initializing test '" + peerFactory.getName() + "'...");
        secureRandom.nextInt();

        CipherTestUtils cipherTest = CipherTestUtils.getInstance();
        Server srv = peerFactory.newServer(cipherTest, PeerFactory.FREE_PORT);
        Thread serverThread = new Thread(srv, "Server");
        serverThread.start();

        return srv;
    }

    public static void mainClient(PeerFactory peerFactory, int port,
            String expectedException) throws Exception {

        long time = System.currentTimeMillis();
        setTestedArguments(peerFactory.getTestedProtocol(),
                peerFactory.getTestedCipher());

        System.out.print(
                " Initializing test '" + peerFactory.getName() + "'...");
        secureRandom.nextInt();

        CipherTestUtils cipherTest = CipherTestUtils.getInstance();
        peerFactory.newClient(cipherTest, port).run();
        cipherTest.checkResult(expectedException);

        time = System.currentTimeMillis() - time;
        System.out.println("Elapsed time " + time);
    }

    public static abstract class PeerFactory {

        public static final int FREE_PORT = 0;

        abstract String getName();

        abstract String getTestedProtocol();

        abstract String getTestedCipher();

        abstract Client newClient(CipherTestUtils cipherTest, int testPort)
                throws Exception;

        abstract Server newServer(CipherTestUtils cipherTest, int testPort)
                throws Exception;

        boolean isSupported(String cipherSuite) {
            return true;
        }
    }
}

class AlwaysTrustManager implements X509TrustManager {

    X509TrustManager trustManager;

    public AlwaysTrustManager(KeyStore keyStore)
            throws NoSuchAlgorithmException, KeyStoreException {

        TrustManagerFactory tmf
                = TrustManagerFactory.getInstance(TrustManagerFactory.
                        getDefaultAlgorithm());
        tmf.init(keyStore);

        TrustManager tms[] = tmf.getTrustManagers();
        for (TrustManager tm : tms) {
            trustManager = (X509TrustManager) tm;
            return;
        }

    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType)
            throws CertificateException {
        try {
            trustManager.checkClientTrusted(chain, authType);
        } catch (CertificateException excep) {
            System.out.println("ERROR in client trust manager: " + excep);
        }
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType)
            throws CertificateException {
        try {
            trustManager.checkServerTrusted(chain, authType);
        } catch (CertificateException excep) {
            System.out.println("ERROR in server trust manager: " + excep);
        }
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return trustManager.getAcceptedIssuers();
    }
}

class MyX509KeyManager extends X509ExtendedKeyManager {

    private final X509ExtendedKeyManager keyManager;
    private String authType;

    MyX509KeyManager(X509ExtendedKeyManager keyManager) {
        this.keyManager = keyManager;
    }

    void setAuthType(String authType) {
        this.authType = "ECDSA".equals(authType) ? "EC" : authType;
    }

    @Override
    public String[] getClientAliases(String keyType, Principal[] issuers) {
        if (authType == null) {
            return null;
        }
        return keyManager.getClientAliases(authType, issuers);
    }

    @Override
    public String chooseClientAlias(String[] keyType, Principal[] issuers,
            Socket socket) {
        if (authType == null) {
            return null;
        }
        return keyManager.chooseClientAlias(new String[]{authType},
                issuers, socket);
    }

    @Override
    public String chooseEngineClientAlias(String[] keyType,
            Principal[] issuers, SSLEngine engine) {
        if (authType == null) {
            return null;
        }
        return keyManager.chooseEngineClientAlias(new String[]{authType},
                issuers, engine);
    }

    @Override
    public String[] getServerAliases(String keyType, Principal[] issuers) {
        throw new UnsupportedOperationException("Servers not supported");
    }

    @Override
    public String chooseServerAlias(String keyType, Principal[] issuers,
            Socket socket) {
        throw new UnsupportedOperationException("Servers not supported");
    }

    @Override
    public String chooseEngineServerAlias(String keyType, Principal[] issuers,
            SSLEngine engine) {
        throw new UnsupportedOperationException("Servers not supported");
    }

    @Override
    public X509Certificate[] getCertificateChain(String alias) {
        return keyManager.getCertificateChain(alias);
    }

    @Override
    public PrivateKey getPrivateKey(String alias) {
        return keyManager.getPrivateKey(alias);
    }
}
