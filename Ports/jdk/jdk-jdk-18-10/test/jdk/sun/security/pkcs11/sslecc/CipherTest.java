/*
 * Copyright (c) 2002, 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

import java.security.*;
import java.security.cert.*;

import javax.net.ssl.*;

/**
 * Test that all ciphersuites work in all versions and all client
 * authentication types. The way this is setup the server is stateless and
 * all checking is done on the client side.
 *
 * The test is multithreaded to speed it up, especially on multiprocessor
 * machines. To simplify debugging, run with -DnumThreads=1.
 *
 * @author Andreas Sterbenz
 */
public class CipherTest {

    // use any available port for the server socket
    static volatile int serverPort = 0;

    static final int THREADS = Integer.getInteger("numThreads", 4);
    static final String TEST_SRC = System.getProperty("test.src", ".");

    // assume that if we do not read anything for 20 seconds, something
    // has gone wrong
    final static int TIMEOUT = 20 * 1000;

    static KeyStore trustStore, keyStore;
    static X509ExtendedKeyManager keyManager;
    static X509TrustManager trustManager;
    static SecureRandom secureRandom;

    private static PeerFactory peerFactory;

    static final CountDownLatch clientCondition = new CountDownLatch(1);

    static abstract class Server implements Runnable {

        final CipherTest cipherTest;

        Server(CipherTest cipherTest) throws Exception {
            this.cipherTest = cipherTest;
        }

        @Override
        public abstract void run();

        void handleRequest(InputStream in, OutputStream out) throws IOException {
            boolean newline = false;
            StringBuilder sb = new StringBuilder();
            while (true) {
                int ch = in.read();
                if (ch < 0) {
                    throw new EOFException();
                }
                sb.append((char)ch);
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
        }

    }

    public static class TestParameters {

        CipherSuite cipherSuite;
        Protocol protocol;
        String clientAuth;

        TestParameters(CipherSuite cipherSuite, Protocol protocol,
                String clientAuth) {
            this.cipherSuite = cipherSuite;
            this.protocol = protocol;
            this.clientAuth = clientAuth;
        }

        boolean isEnabled() {
            return cipherSuite.supportedByProtocol(protocol);
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

    private List<TestParameters> tests;
    private Iterator<TestParameters> testIterator;
    private SSLSocketFactory factory;
    private boolean failed;

    private CipherTest(PeerFactory peerFactory) throws IOException {
        factory = (SSLSocketFactory)SSLSocketFactory.getDefault();
        SSLSocket socket = (SSLSocket)factory.createSocket();
        String[] cipherSuites = socket.getSupportedCipherSuites();
        String[] protocols = socket.getSupportedProtocols();
        String[] clientAuths = {null, "RSA", "DSA", "ECDSA"};
        tests = new ArrayList<TestParameters>(
            cipherSuites.length * protocols.length * clientAuths.length);
        for (int i = 0; i < cipherSuites.length; i++) {
            String cipherSuite = cipherSuites[i];

            for (int j = 0; j < protocols.length; j++) {
                String protocol = protocols[j];

                if (!peerFactory.isSupported(cipherSuite, protocol)) {
                    continue;
                }

                for (int k = 0; k < clientAuths.length; k++) {
                    String clientAuth = clientAuths[k];
                    // no client with anonymous cipher suites.
                    // TLS_EMPTY_RENEGOTIATION_INFO_SCSV always be skipped.
                    // TLS 1.3 is skipped due to the signature algorithm,
                    // exactly MD5withRSA, in the certificates is not allowed.
                    if ((clientAuth != null && cipherSuite.contains("DH_anon")
                            || cipherSuite.equals(
                                    CipherSuite.TLS_EMPTY_RENEGOTIATION_INFO_SCSV.name())
                            || "TLSv1.3".equals(protocol))) {
                        continue;
                    }

                    tests.add(new TestParameters(
                            CipherSuite.cipherSuite(cipherSuite),
                            Protocol.protocol(protocol),
                            clientAuth));
                }
            }
        }

        testIterator = tests.iterator();
    }

    synchronized void setFailed() {
        failed = true;
    }

    public void run() throws Exception {
        Thread[] threads = new Thread[THREADS];
        for (int i = 0; i < THREADS; i++) {
            try {
                threads[i] = new Thread(peerFactory.newClient(this),
                    "Client " + i);
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
            threads[i].start();
        }

        // The client threads are ready.
        clientCondition.countDown();

        try {
            for (int i = 0; i < THREADS; i++) {
                threads[i].join();
            }
        } catch (InterruptedException e) {
            setFailed();
            e.printStackTrace();
        }
        if (failed) {
            throw new Exception("*** Test '" + peerFactory.getName() +
                "' failed ***");
        } else {
            System.out.println("Test '" + peerFactory.getName() +
                "' completed successfully");
        }
    }

    synchronized TestParameters getTest() {
        if (failed) {
            return null;
        }
        if (testIterator.hasNext()) {
            return (TestParameters)testIterator.next();
        }
        return null;
    }

    SSLSocketFactory getFactory() {
        return factory;
    }

    static abstract class Client implements Runnable {

        final CipherTest cipherTest;

        Client(CipherTest cipherTest) throws Exception {
            this.cipherTest = cipherTest;
        }

        @Override
        public final void run() {
            while (true) {
                TestParameters params = cipherTest.getTest();
                if (params == null) {
                    // no more tests
                    break;
                }
                if (params.isEnabled() == false) {
                    System.out.println("Skipping disabled test " + params);
                    continue;
                }
                try {
                    runTest(params);
                    System.out.println("Passed " + params);
                } catch (SocketTimeoutException ste) {
                    System.out.println("The client connects to the server timeout, "
                            + "so ignore the test.");
                    break;
                } catch (Exception e) {
                    cipherTest.setFailed();
                    System.out.println("** Failed " + params + "**");
                    e.printStackTrace();
                }
            }
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
                sb.append((char)ch);
            }
            String response = sb.toString();
            if (response.startsWith("HTTP/1.0 200 ") == false) {
                throw new IOException("Invalid response: " + response);
            }
        }

    }

    // for some reason, ${test.src} has a different value when the
    // test is called from the script and when it is called directly...
    static String pathToStores = ".";
    static String pathToStoresSH = ".";
    static String keyStoreFile = "keystore";
    static String trustStoreFile = "truststore";
    static char[] passwd = "passphrase".toCharArray();

    static File PATH;

    private static KeyStore readKeyStore(String name) throws Exception {
        File file = new File(PATH, name);
        KeyStore ks;
        try (InputStream in = new FileInputStream(file)) {
            ks = KeyStore.getInstance("JKS");
            ks.load(in, passwd);
        }
        return ks;
    }

    public static void main(PeerFactory peerFactory, String[] args)
            throws Exception {
        long time = System.currentTimeMillis();
        String relPath;
        if ((args != null) && (args.length > 0) && args[0].equals("sh")) {
            relPath = pathToStoresSH;
        } else {
            relPath = pathToStores;
        }
        PATH = new File(TEST_SRC, relPath);
        CipherTest.peerFactory = peerFactory;
        System.out.print(
            "Initializing test '" + peerFactory.getName() + "'...");
        secureRandom = new SecureRandom();
        secureRandom.nextInt();
        trustStore = readKeyStore(trustStoreFile);
        keyStore = readKeyStore(keyStoreFile);
        KeyManagerFactory keyFactory =
            KeyManagerFactory.getInstance(
                KeyManagerFactory.getDefaultAlgorithm());
        keyFactory.init(keyStore, passwd);
        keyManager = (X509ExtendedKeyManager)keyFactory.getKeyManagers()[0];
        trustManager = new AlwaysTrustManager();

        CipherTest cipherTest = new CipherTest(peerFactory);
        Thread serverThread = new Thread(peerFactory.newServer(cipherTest),
            "Server");
        serverThread.setDaemon(true);
        serverThread.start();
        System.out.println("Done");
        cipherTest.run();
        time = System.currentTimeMillis() - time;
        System.out.println("Done. (" + time + " ms)");
    }

    static abstract class PeerFactory {

        abstract String getName();

        abstract Client newClient(CipherTest cipherTest) throws Exception;

        abstract Server newServer(CipherTest cipherTest) throws Exception;

        boolean isSupported(String cipherSuite, String protocol) {
            // skip kerberos cipher suites
            if (cipherSuite.startsWith("TLS_KRB5")) {
                System.out.println("Skipping unsupported test for " +
                                    cipherSuite + " of " + protocol);
                return false;
            }

            // skip SSLv2Hello protocol
            if (protocol.equals("SSLv2Hello")) {
                System.out.println("Skipping unsupported test for " +
                                    cipherSuite + " of " + protocol);
                return false;
            }

            // ignore exportable cipher suite for TLSv1.1
            if (protocol.equals("TLSv1.1")) {
                if (cipherSuite.indexOf("_EXPORT_WITH") != -1) {
                    System.out.println("Skipping obsoleted test for " +
                                        cipherSuite + " of " + protocol);
                    return false;
                }
            }

            return true;
        }
    }

}

// we currently don't do any chain verification. we assume that works ok
// and we can speed up the test. we could also just add a plain certificate
// chain comparision with our trusted certificates.
class AlwaysTrustManager implements X509TrustManager {

    public AlwaysTrustManager() {

    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType)
            throws CertificateException {
        // empty
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType)
            throws CertificateException {
        // empty
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return new X509Certificate[0];
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
        return keyManager.chooseClientAlias(new String[] {authType},
            issuers, socket);
    }

    @Override
    public String chooseEngineClientAlias(String[] keyType,
            Principal[] issuers, SSLEngine engine) {
        if (authType == null) {
            return null;
        }
        return keyManager.chooseEngineClientAlias(new String[] {authType},
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

class DaemonThreadFactory implements ThreadFactory {

    final static ThreadFactory INSTANCE = new DaemonThreadFactory();

    private final static ThreadFactory DEFAULT = Executors.defaultThreadFactory();

    @Override
    public Thread newThread(Runnable r) {
        Thread t = DEFAULT.newThread(r);
        t.setDaemon(true);
        return t;
    }

}
