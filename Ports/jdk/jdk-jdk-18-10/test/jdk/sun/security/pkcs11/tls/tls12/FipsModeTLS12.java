/*
 * Copyright (c) 2019, Red Hat, Inc.
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
 * @bug 8029661
 * @summary Test TLS 1.2
 * @modules java.base/sun.security.internal.spec
 *          java.base/sun.security.util
 *          java.base/com.sun.crypto.provider
 * @library /test/lib ../..
 * @run main/othervm/timeout=120 -Djdk.tls.useExtendedMasterSecret=false FipsModeTLS12
 */

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.SecureRandom;
import java.security.Security;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManagerFactory;

import sun.security.internal.spec.TlsMasterSecretParameterSpec;
import sun.security.internal.spec.TlsPrfParameterSpec;
import sun.security.internal.spec.TlsRsaPremasterSecretParameterSpec;

public final class FipsModeTLS12 extends SecmodTest {

    private static final boolean enableDebug = true;

    private static Provider sunPKCS11NSSProvider;
    private static Provider sunJCEProvider;
    private static KeyStore ks;
    private static KeyStore ts;
    private static char[] passphrase = "JAHshj131@@".toCharArray();
    private static PrivateKey privateKey;
    private static PublicKey publicKey;

    public static void main(String[] args) throws Exception {
        try {
            initialize();
        } catch (Exception e) {
            System.out.println("Test skipped: failure during" +
                    " initialization");
            if (enableDebug) {
                System.out.println(e);
            }
            return;
        }

        if (shouldRun()) {
            // Test against JCE
            testTlsAuthenticationCodeGeneration();

            // Self-integrity test (complete TLS 1.2 communication)
            new testTLS12SunPKCS11Communication().run();

            System.out.println("Test PASS - OK");
        } else {
            System.out.println("Test skipped: TLS 1.2 mechanisms" +
                    " not supported by current SunPKCS11 back-end");
        }
    }

    private static boolean shouldRun() {
        if (sunPKCS11NSSProvider == null) {
            return false;
        }
        try {
            KeyGenerator.getInstance("SunTls12MasterSecret",
                    sunPKCS11NSSProvider);
            KeyGenerator.getInstance(
                    "SunTls12RsaPremasterSecret", sunPKCS11NSSProvider);
            KeyGenerator.getInstance("SunTls12Prf", sunPKCS11NSSProvider);
        } catch (NoSuchAlgorithmException e) {
            return false;
        }
        return true;
    }

    private static void testTlsAuthenticationCodeGeneration()
            throws Exception {
        // Generate RSA Pre-Master Secret in SunPKCS11 provider
        SecretKey rsaPreMasterSecret = null;
        @SuppressWarnings("deprecation")
        TlsRsaPremasterSecretParameterSpec rsaPreMasterSecretSpec =
                new TlsRsaPremasterSecretParameterSpec(0x0303, 0x0303);
        {
            KeyGenerator rsaPreMasterSecretKG = KeyGenerator.getInstance(
                    "SunTls12RsaPremasterSecret", sunPKCS11NSSProvider);
            rsaPreMasterSecretKG.init(rsaPreMasterSecretSpec, null);
            rsaPreMasterSecret = rsaPreMasterSecretKG.generateKey();
        }

        // Get RSA Pre-Master Secret in plain (from SunPKCS11 provider)
        byte[] rsaPlainPreMasterSecret = null;
        {
            Cipher rsaPreMasterSecretWrapperCipher =
                    Cipher.getInstance("RSA/ECB/PKCS1Padding",
                            sunPKCS11NSSProvider);
            rsaPreMasterSecretWrapperCipher.init(Cipher.WRAP_MODE, publicKey,
                    new SecureRandom());
            byte[] rsaEncryptedPreMasterSecret =
                    rsaPreMasterSecretWrapperCipher.wrap(rsaPreMasterSecret);
            Cipher rsaPreMasterSecretUnwrapperCipher =
                    Cipher.getInstance("RSA/ECB/PKCS1Padding", sunJCEProvider);
            rsaPreMasterSecretUnwrapperCipher.init(Cipher.UNWRAP_MODE,
                    privateKey, rsaPreMasterSecretSpec);
            rsaPlainPreMasterSecret = rsaPreMasterSecretUnwrapperCipher.unwrap(
                    rsaEncryptedPreMasterSecret, "TlsRsaPremasterSecret",
                    Cipher.SECRET_KEY).getEncoded();

            if (enableDebug) {
                System.out.println("rsaPlainPreMasterSecret:");
                for (byte b : rsaPlainPreMasterSecret) {
                    System.out.printf("%02X, ", b);
                }
                System.out.println("");
            }
        }

        // Generate Master Secret
        SecretKey sunPKCS11MasterSecret = null;
        SecretKey jceMasterSecret = null;
        {
            KeyGenerator sunPKCS11MasterSecretGenerator =
                    KeyGenerator.getInstance("SunTls12MasterSecret",
                            sunPKCS11NSSProvider);
            KeyGenerator jceMasterSecretGenerator = KeyGenerator.getInstance(
                    "SunTls12MasterSecret", sunJCEProvider);
            @SuppressWarnings("deprecation")
            TlsMasterSecretParameterSpec sunPKCS11MasterSecretSpec =
                    new TlsMasterSecretParameterSpec(rsaPreMasterSecret, 3, 3,
                            new byte[32], new byte[32], "SHA-256", 32, 64);
            @SuppressWarnings("deprecation")
            TlsMasterSecretParameterSpec jceMasterSecretSpec =
                    new TlsMasterSecretParameterSpec(
                            new SecretKeySpec(rsaPlainPreMasterSecret,
                                    "Generic"), 3, 3, new byte[32],
                            new byte[32], "SHA-256", 32, 64);
            sunPKCS11MasterSecretGenerator.init(sunPKCS11MasterSecretSpec,
                    null);
            jceMasterSecretGenerator.init(jceMasterSecretSpec, null);
            sunPKCS11MasterSecret =
                    sunPKCS11MasterSecretGenerator.generateKey();
            jceMasterSecret = jceMasterSecretGenerator.generateKey();
            if (enableDebug) {
                System.out.println("Master Secret (SunJCE):");
                if (jceMasterSecret != null) {
                    for (byte b : jceMasterSecret.getEncoded()) {
                        System.out.printf("%02X, ", b);
                    }
                    System.out.println("");
                }
            }
        }

        // Generate authentication codes
        byte[] sunPKCS11AuthenticationCode = null;
        byte[] jceAuthenticationCode = null;
        {
            // Generate SunPKCS11 authentication code
            {
                @SuppressWarnings("deprecation")
                TlsPrfParameterSpec sunPKCS11AuthenticationCodeSpec =
                        new TlsPrfParameterSpec(sunPKCS11MasterSecret,
                                "client finished", "a".getBytes(), 12,
                                "SHA-256", 32, 64);
                KeyGenerator sunPKCS11AuthCodeGenerator =
                        KeyGenerator.getInstance("SunTls12Prf",
                                sunPKCS11NSSProvider);
                sunPKCS11AuthCodeGenerator.init(
                        sunPKCS11AuthenticationCodeSpec);
                sunPKCS11AuthenticationCode =
                        sunPKCS11AuthCodeGenerator.generateKey().getEncoded();
            }

            // Generate SunJCE authentication code
            {
                @SuppressWarnings("deprecation")
                TlsPrfParameterSpec jceAuthenticationCodeSpec =
                        new TlsPrfParameterSpec(jceMasterSecret,
                                "client finished", "a".getBytes(), 12,
                                "SHA-256", 32, 64);
                KeyGenerator jceAuthCodeGenerator =
                        KeyGenerator.getInstance("SunTls12Prf",
                                sunJCEProvider);
                jceAuthCodeGenerator.init(jceAuthenticationCodeSpec);
                jceAuthenticationCode =
                        jceAuthCodeGenerator.generateKey().getEncoded();
            }

            if (enableDebug) {
                System.out.println("SunPKCS11 Authentication Code: ");
                for (byte b : sunPKCS11AuthenticationCode) {
                    System.out.printf("%02X, ", b);
                }
                System.out.println("");
                System.out.println("SunJCE Authentication Code: ");
                for (byte b : jceAuthenticationCode) {
                    System.out.printf("%02X, ", b);
                }
                System.out.println("");
            }
        }

        if (sunPKCS11AuthenticationCode == null ||
                jceAuthenticationCode == null ||
                sunPKCS11AuthenticationCode.length == 0 ||
                jceAuthenticationCode.length == 0 ||
                !Arrays.equals(sunPKCS11AuthenticationCode,
                        jceAuthenticationCode)) {
            throw new Exception("Authentication codes from JCE" +
                        " and SunPKCS11 differ.");
        }
    }

    private static class testTLS12SunPKCS11Communication {
        public static void run() throws Exception {
            SSLEngine[][] enginesToTest = getSSLEnginesToTest();

            for (SSLEngine[] engineToTest : enginesToTest) {

                SSLEngine clientSSLEngine = engineToTest[0];
                SSLEngine serverSSLEngine = engineToTest[1];

                // SSLEngine code based on RedhandshakeFinished.java

                boolean dataDone = false;

                ByteBuffer clientOut = null;
                ByteBuffer clientIn = null;
                ByteBuffer serverOut = null;
                ByteBuffer serverIn = null;
                ByteBuffer cTOs;
                ByteBuffer sTOc;

                SSLSession session = clientSSLEngine.getSession();
                int appBufferMax = session.getApplicationBufferSize();
                int netBufferMax = session.getPacketBufferSize();

                clientIn = ByteBuffer.allocate(appBufferMax + 50);
                serverIn = ByteBuffer.allocate(appBufferMax + 50);

                cTOs = ByteBuffer.allocateDirect(netBufferMax);
                sTOc = ByteBuffer.allocateDirect(netBufferMax);

                clientOut = ByteBuffer.wrap(
                        "Hi Server, I'm Client".getBytes());
                serverOut = ByteBuffer.wrap(
                        "Hello Client, I'm Server".getBytes());

                SSLEngineResult clientResult;
                SSLEngineResult serverResult;

                while (!dataDone) {
                    clientResult = clientSSLEngine.wrap(clientOut, cTOs);
                    runDelegatedTasks(clientResult, clientSSLEngine);
                    serverResult = serverSSLEngine.wrap(serverOut, sTOc);
                    runDelegatedTasks(serverResult, serverSSLEngine);
                    cTOs.flip();
                    sTOc.flip();

                    if (enableDebug) {
                        System.out.println("Client -> Network");
                        printTlsNetworkPacket("", cTOs);
                        System.out.println("");
                        System.out.println("Server -> Network");
                        printTlsNetworkPacket("", sTOc);
                        System.out.println("");
                    }

                    clientResult = clientSSLEngine.unwrap(sTOc, clientIn);
                    runDelegatedTasks(clientResult, clientSSLEngine);
                    serverResult = serverSSLEngine.unwrap(cTOs, serverIn);
                    runDelegatedTasks(serverResult, serverSSLEngine);

                    cTOs.compact();
                    sTOc.compact();

                    if (!dataDone &&
                            (clientOut.limit() == serverIn.position()) &&
                            (serverOut.limit() == clientIn.position())) {
                        checkTransfer(serverOut, clientIn);
                        checkTransfer(clientOut, serverIn);
                        dataDone = true;
                    }
                }
            }
        }

        static void printTlsNetworkPacket(String prefix, ByteBuffer bb) {
            ByteBuffer slice = bb.slice();
            byte[] buffer = new byte[slice.remaining()];
            slice.get(buffer);
            for (int i = 0; i < buffer.length; i++) {
                System.out.printf("%02X, ", (byte)(buffer[i] & (byte)0xFF));
                if (i % 8 == 0 && i % 16 != 0) {
                    System.out.print(" ");
                }
                if (i % 16 == 0) {
                    System.out.println("");
                }
            }
            System.out.flush();
        }

        private static void checkTransfer(ByteBuffer a, ByteBuffer b)
                throws Exception {
            a.flip();
            b.flip();
            if (!a.equals(b)) {
                throw new Exception("Data didn't transfer cleanly");
            }
            a.position(a.limit());
            b.position(b.limit());
            a.limit(a.capacity());
            b.limit(b.capacity());
        }

        private static void runDelegatedTasks(SSLEngineResult result,
                SSLEngine engine) throws Exception {

            if (result.getHandshakeStatus() == HandshakeStatus.NEED_TASK) {
                Runnable runnable;
                while ((runnable = engine.getDelegatedTask()) != null) {
                    runnable.run();
                }
                HandshakeStatus hsStatus = engine.getHandshakeStatus();
                if (hsStatus == HandshakeStatus.NEED_TASK) {
                    throw new Exception(
                        "handshake shouldn't need additional tasks");
                }
            }
        }

        private static SSLEngine[][] getSSLEnginesToTest() throws Exception {
            SSLEngine[][] enginesToTest = new SSLEngine[2][2];
            // TLS_RSA_WITH_AES_128_GCM_SHA256 ciphersuite is available but
            // must not be chosen for the TLS connection if not supported.
            // See JDK-8222937.
            String[][] preferredSuites = new String[][]{ new String[] {
                    "TLS_RSA_WITH_AES_128_GCM_SHA256",
                    "TLS_RSA_WITH_AES_128_CBC_SHA256"
            },  new String[] {
                    "TLS_RSA_WITH_AES_128_GCM_SHA256",
                    "TLS_DHE_RSA_WITH_AES_128_CBC_SHA256"
            }};
            for (int i = 0; i < enginesToTest.length; i++) {
                enginesToTest[i][0] = createSSLEngine(true);
                enginesToTest[i][1] = createSSLEngine(false);
                // All CipherSuites enabled for the client.
                enginesToTest[i][1].setEnabledCipherSuites(preferredSuites[i]);
            }
            return enginesToTest;
        }

        static private SSLEngine createSSLEngine(boolean client)
                throws Exception {
            SSLEngine ssle;
            KeyManagerFactory kmf = KeyManagerFactory.getInstance("PKIX", "SunJSSE");
            kmf.init(ks, passphrase);

            TrustManagerFactory tmf = TrustManagerFactory.getInstance("PKIX", "SunJSSE");
            tmf.init(ts);

            SSLContext sslCtx = SSLContext.getInstance("TLSv1.2", "SunJSSE");
            sslCtx.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
            ssle = sslCtx.createSSLEngine("localhost", 443);
            ssle.setUseClientMode(client);
            SSLParameters sslParameters = ssle.getSSLParameters();
            ssle.setSSLParameters(sslParameters);

            return ssle;
        }
    }

    private static void initialize() throws Exception {
        //
        // For a successful FIPS-mode TLS connection, the following
        // cryptographic providers will be installed:
        //
        //  1. SunPKCS11 (with an NSS FIPS mode backend)
        //  2. SUN (to handle X.509 certificates)
        //  3. SunJSSE (for a TLS engine)
        //
        // RSASSA-PSS algorithm is not currently supported in SunPKCS11
        // but in SUN provider. As a result, it can be negotiated by the
        // TLS engine. The problem is that SunPKCS11 keys are sensitive
        // in FIPS mode and cannot be used in a SUN algorithm (conversion
        // fails as plain values cannot be extracted).
        //
        // To workaround this issue, we disable RSASSA-PSS algorithm for
        // TLS connections. Once JDK-8222937 is fixed, this workaround can
        // (and should) be removed.
        //
        // On a final note, the list of disabled TLS algorithms
        // (jdk.tls.disabledAlgorithms) has to be updated at this point,
        // before it is read in sun.security.ssl.SSLAlgorithmConstraints
        // class initialization.
        String disabledAlgorithms =
                Security.getProperty("jdk.tls.disabledAlgorithms");
        if (disabledAlgorithms.length() > 0) {
            disabledAlgorithms += ", ";
        }
        disabledAlgorithms += "RSASSA-PSS";
        Security.setProperty("jdk.tls.disabledAlgorithms", disabledAlgorithms);

        if (initSecmod() == false) {
            return;
        }
        String configName = BASE + SEP + "nss.cfg";
        sunPKCS11NSSProvider = getSunPKCS11(configName);
        System.out.println("SunPKCS11 provider: " + sunPKCS11NSSProvider);

        List<Provider> installedProviders = new LinkedList<>();
        for (Provider p : Security.getProviders()){
            installedProviders.add(p);
            Security.removeProvider(p.getName());
        }
        Security.addProvider(sunPKCS11NSSProvider);
        for (Provider p : installedProviders){
            String providerName = p.getName();
            if (providerName.equals("SunJSSE") || providerName.equals("SUN")) {
                Security.addProvider(p);
            } else if (providerName.equals("SunJCE")) {
                sunJCEProvider = p;
            }
        }

        ks = KeyStore.getInstance("PKCS11", sunPKCS11NSSProvider);
        ks.load(null, "test12".toCharArray());
        ts = ks;

        KeyStore ksPlain = readTestKeyStore();
        privateKey = (PrivateKey)ksPlain.getKey("rh_rsa_sha256",
                passphrase);
        publicKey = (PublicKey)ksPlain.getCertificate(
                "rh_rsa_sha256").getPublicKey();
    }

    private static KeyStore readTestKeyStore() throws Exception {
        File file = new File(System.getProperty("test.src", "."), "keystore");
        InputStream in = new FileInputStream(file);
        KeyStore ks = KeyStore.getInstance("JKS");
        ks.load(in, "passphrase".toCharArray());
        in.close();
        return ks;
    }
}
