/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
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

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SNIHostName;
import javax.net.ssl.SNIMatcher;
import javax.net.ssl.SNIServerName;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManagerFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Basic class to inherit SSLEngine test cases from it. Tests apply for
 * the TLS or DTLS security protocols and their versions.
 */
abstract public class SSLEngineTestCase {

    public enum Ciphers {

        /**
         * Ciphers supported by the tested SSLEngine without those with
         * kerberos authentication.
         */
        SUPPORTED_NON_KRB_CIPHERS(SSLEngineTestCase.SUPPORTED_NON_KRB_CIPHERS,
                "Supported non kerberos"),
        /**
         * Ciphers supported by the tested SSLEngine without those with
         * kerberos authentication and without those with SHA256 ans SHA384.
         */
        SUPPORTED_NON_KRB_NON_SHA_CIPHERS(
                SSLEngineTestCase.SUPPORTED_NON_KRB_NON_SHA_CIPHERS,
                "Supported non kerberos non SHA256 and SHA384"),
        /**
         * Ciphers supported by the tested SSLEngine with kerberos
         * authentication.
         */
        SUPPORTED_KRB_CIPHERS(SSLEngineTestCase.SUPPORTED_KRB_CIPHERS,
                "Supported kerberos"),
        /**
         * Ciphers enabled by default for the tested SSLEngine without kerberos
         * and anon.
         */
        ENABLED_NON_KRB_NOT_ANON_CIPHERS(
                SSLEngineTestCase.ENABLED_NON_KRB_NOT_ANON_CIPHERS,
                "Enabled by default non kerberos not anonymous"),
        /**
         * Ciphers supported by TLS 1.3 only.
         */
        TLS13_CIPHERS(
                SSLEngineTestCase.TLS13_CIPHERS,
                "Supported by TLS 1.3 only"),
        /**
         * Ciphers unsupported by the tested SSLEngine.
         */
        UNSUPPORTED_CIPHERS(SSLEngineTestCase.UNSUPPORTED_CIPHERS,
                "Unsupported");

        Ciphers(String[] ciphers, String description) {
            this.ciphers = ciphers;
            this.description = description;
        }

        final String[] ciphers;
        final String description;
    }

    /**
     * Enumeration used to distinguish handshake mode in
     * {@link SSLEngineTestCase#doHandshake(javax.net.ssl.SSLEngine,
     * javax.net.ssl.SSLEngine, int, SSLEngineTestCase.HandshakeMode, boolean)
     * SSLEngineTestCase.doHandshake} method.
     */
    public enum HandshakeMode {

        /**
         * Initial handshake done for the first time: both engines call
         * {@link SSLEngine#beginHandshake()} method.
         */
        INITIAL_HANDSHAKE,
        /**
         * Repeated handshake done by client: client engine calls
         * {@link SSLEngine#beginHandshake()} method.
         */
        REHANDSHAKE_BEGIN_CLIENT,
        /**
         * Repeated handshake done by server: server engine calls
         * {@link SSLEngine#beginHandshake()} method.
         */
        REHANDSHAKE_BEGIN_SERVER;
    }
    /**
     * Security protocol to be tested: "TLS" or "DTLS" or their versions,
     * e.g. "TLSv1", "TLSv1.1", "TLSv1.2", "DTLSv1.0", "DTLSv1.2".
     */
    public static final String TESTED_SECURITY_PROTOCOL
            = System.getProperty("test.security.protocol", "TLS");
    /**
     * Test mode: "norm", "norm_sni" or "krb".
     * Modes "norm" and "norm_sni" are used to run
     * with all supported non-kerberos ciphers.
     * Mode "krb" is used to run with kerberos ciphers.
     */
    public static final String TEST_MODE
            = System.getProperty("test.mode", "norm");

    private static final String FS = System.getProperty("file.separator", "/");
    private static final String PATH_TO_STORES = ".." + FS + "etc";
    private static final String KEY_STORE_FILE = "keystore";
    private static final String TRUST_STORE_FILE = "truststore";
    private static final String PASSWD = "passphrase";

    private static final String KEY_FILE_NAME
            = System.getProperty("test.src", ".") + FS + PATH_TO_STORES
            + FS + KEY_STORE_FILE;
    private static final String TRUST_FILE_NAME
            = System.getProperty("test.src", ".") + FS + PATH_TO_STORES
            + FS + TRUST_STORE_FILE;

    // Need an enhancement to use none-static mutable global variables.
    private static ByteBuffer net;
    private static boolean doUnwrapForNotHandshakingStatus;
    private static boolean endHandshakeLoop = false;

    private static final int MAX_HANDSHAKE_LOOPS = 100;
    private static final String EXCHANGE_MSG_SENT = "Hello, peer!";
    private static final String TEST_SRC = System.getProperty("test.src", ".");
    private static final String KTAB_FILENAME = "krb5.keytab.data";
    private static final String KRB_REALM = "TEST.REALM";
    private static final String KRBTGT_PRINCIPAL = "krbtgt/" + KRB_REALM;
    private static final String KRB_USER = "USER";
    private static final String KRB_USER_PASSWORD = "password";
    private static final String KRB_USER_PRINCIPAL = KRB_USER + "@" + KRB_REALM;
    private static final String KRB5_CONF_FILENAME = "krb5.conf";
    private static final String PATH_TO_COMMON = ".." + FS + "TLSCommon";
    private static final String JAAS_CONF_FILE = PATH_TO_COMMON
            + FS + "jaas.conf";
    private static final int DELAY = 1000;
    private static final String HOST = "localhost";
    private static final String SERVER_NAME = "service.localhost";
    private static final String SNI_PATTERN = ".*";

    private static final String[] TLS13_CIPHERS = {
            "TLS_AES_256_GCM_SHA384",
            "TLS_AES_128_GCM_SHA256",
            "TLS_CHACHA20_POLY1305_SHA256"
    };

    private static final String[] SUPPORTED_NON_KRB_CIPHERS;

    static {
        try {
            String[] allSupportedCiphers = getContext()
                    .createSSLEngine().getSupportedCipherSuites();
            List<String> supportedCiphersList = new LinkedList<>();
            for (String cipher : allSupportedCiphers) {
                if (!cipher.contains("KRB5")
                        && !isTLS13Cipher(cipher)
                        && !cipher.contains("TLS_EMPTY_RENEGOTIATION_INFO_SCSV")) {
                    supportedCiphersList.add(cipher);
                }
            }
            SUPPORTED_NON_KRB_CIPHERS =
                    supportedCiphersList.toArray(new String[0]);
        } catch (Exception ex) {
            throw new Error("Unexpected issue", ex);
        }
    }

    private static final String[] SUPPORTED_NON_KRB_NON_SHA_CIPHERS;

    static {
        try {
            String[] allSupportedCiphers = getContext()
                    .createSSLEngine().getSupportedCipherSuites();
            List<String> supportedCiphersList = new LinkedList<>();
            for (String cipher : allSupportedCiphers) {
                if (!cipher.contains("KRB5")
                        && !isTLS13Cipher(cipher)
                        && !cipher.contains("TLS_EMPTY_RENEGOTIATION_INFO_SCSV")
                        && !cipher.endsWith("_SHA256")
                        && !cipher.endsWith("_SHA384")) {
                    supportedCiphersList.add(cipher);
                }
            }
            SUPPORTED_NON_KRB_NON_SHA_CIPHERS
                    = supportedCiphersList.toArray(new String[0]);
        } catch (Exception ex) {
            throw new Error("Unexpected issue", ex);
        }
    }

    private static final String[] SUPPORTED_KRB_CIPHERS;

    static {
        try {
            String[] allSupportedCiphers = getContext()
                    .createSSLEngine().getSupportedCipherSuites();
            List<String> supportedCiphersList = new LinkedList<>();
            for (String cipher : allSupportedCiphers) {
                if (cipher.contains("KRB5")
                        && !isTLS13Cipher(cipher)
                        && !cipher.contains("TLS_EMPTY_RENEGOTIATION_INFO_SCSV")) {
                    supportedCiphersList.add(cipher);
                }
            }
            SUPPORTED_KRB_CIPHERS = supportedCiphersList.toArray(new String[0]);
        } catch (Exception ex) {
            throw new Error("Unexpected issue", ex);
        }
    }

    private static final String[] ENABLED_NON_KRB_NOT_ANON_CIPHERS;

    static {
        try {
            SSLEngine temporary = getContext().createSSLEngine();
            temporary.setUseClientMode(true);
            String[] enabledCiphers = temporary.getEnabledCipherSuites();
            List<String> enabledCiphersList = new LinkedList<>();
            for (String cipher : enabledCiphers) {
                if (!cipher.contains("anon") && !cipher.contains("KRB5")
                        && !isTLS13Cipher(cipher)
                        && !cipher.contains("TLS_EMPTY_RENEGOTIATION_INFO_SCSV")) {
                    enabledCiphersList.add(cipher);
                }
            }
            ENABLED_NON_KRB_NOT_ANON_CIPHERS =
                    enabledCiphersList.toArray(new String[0]);
        } catch (Exception ex) {
            throw new Error("Unexpected issue", ex);
        }
    }

    private static final String[] UNSUPPORTED_CIPHERS = {
            "SSL_DHE_DSS_EXPORT1024_WITH_DES_CBC_SHA",
            "SSL_DHE_DSS_EXPORT1024_WITH_RC4_56_SHA",
            "SSL_DHE_DSS_WITH_RC4_128_SHA",
            "SSL_DH_DSS_EXPORT_WITH_DES40_CBC_SHA",
            "SSL_DH_DSS_WITH_3DES_EDE_CBC_SHA",
            "SSL_DH_DSS_WITH_DES_CBC_SHA",
            "SSL_DH_RSA_EXPORT_WITH_DES40_CBC_SHA",
            "SSL_DH_RSA_WITH_3DES_EDE_CBC_SHA",
            "SSL_DH_RSA_WITH_DES_CBC_SHA",
            "SSL_FORTEZZA_DMS_WITH_FORTEZZA_CBC_SHA",
            "SSL_FORTEZZA_DMS_WITH_NULL_SHA",
            "SSL_RSA_EXPORT1024_WITH_DES_CBC_SHA",
            "SSL_RSA_EXPORT1024_WITH_RC4_56_SHA",
            "SSL_RSA_EXPORT_WITH_RC2_CBC_40_MD5",
            "SSL_RSA_FIPS_WITH_3DES_EDE_CBC_SHA",
            "SSL_RSA_FIPS_WITH_DES_CBC_SHA",
            "TLS_KRB5_EXPORT_WITH_RC2_CBC_40_MD5",
            "TLS_KRB5_EXPORT_WITH_RC2_CBC_40_SHA",
            "TLS_KRB5_WITH_IDEA_CBC_MD5",
            "TLS_KRB5_WITH_IDEA_CBC_SHA",
            "SSL_RSA_WITH_IDEA_CBC_SHA",
            "TLS_DH_RSA_WITH_AES_128_GCM_SHA256",
            "TLS_DH_RSA_WITH_AES_256_GCM_SHA384",
            "TLS_DH_DSS_WITH_AES_128_GCM_SHA256",
            "TLS_DH_DSS_WITH_AES_256_GCM_SHA384"
    };

    private final int maxPacketSize;

    /**
     * Constructs test case with the given MFLN maxMacketSize.
     *
     * @param maxPacketSize - MLFN extension max packet size.
     */
    public SSLEngineTestCase(int maxPacketSize) {
        this.maxPacketSize = maxPacketSize;
    }

    /**
     * Constructs test case with {@code maxPacketSize = 0}.
     */
    public SSLEngineTestCase() {
        this.maxPacketSize = 0;
    }

    private static boolean isTLS13Cipher(String cipher) {
        for (String cipherSuite : TLS13_CIPHERS) {
            if (cipherSuite.equals(cipher)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Wraps data with the specified engine.
     *
     * @param engine        - SSLEngine that wraps data.
     * @param wrapper       - Set wrapper id, e.g. "server" of "client".
     *                        Used for logging only.
     * @param maxPacketSize - Max packet size to check that MFLN extension
     *                        works or zero for no check.
     * @param app           - Buffer with data to wrap.
     * @return - Buffer with wrapped data.
     * @throws SSLException - thrown on engine errors.
     */
    public static ByteBuffer doWrap(SSLEngine engine, String wrapper,
                                    int maxPacketSize, ByteBuffer app)
            throws SSLException {
        return doWrap(engine, wrapper, maxPacketSize,
                app, SSLEngineResult.Status.OK, null);
    }

    /**
     * Wraps data with the specified engine.
     *
     * @param engine        - SSLEngine that wraps data.
     * @param wrapper       - Set wrapper id, e.g. "server" of "client".
     *                        Used for logging only.
     * @param maxPacketSize - Max packet size to check that MFLN extension
     *                        works or zero for no check.
     * @param app           - Buffer with data to wrap.
     * @param result        - Array which first element will be used to
     *                        output wrap result object.
     * @return - Buffer with wrapped data.
     * @throws SSLException - thrown on engine errors.
     */
    public static ByteBuffer doWrap(SSLEngine engine, String wrapper,
                                    int maxPacketSize, ByteBuffer app,
                                    SSLEngineResult[] result)
            throws SSLException {
        return doWrap(engine, wrapper, maxPacketSize,
                app, SSLEngineResult.Status.OK, result);
    }

    /**
     * Wraps data with the specified engine.
     *
     * @param engine        - SSLEngine that wraps data.
     * @param wrapper       - Set wrapper id, e.g. "server" of "client".
     *                        Used for logging only.
     * @param maxPacketSize - Max packet size to check that MFLN extension
     *                        works or zero for no check.
     * @param app           - Buffer with data to wrap.
     * @param wantedStatus  - Specifies expected result status of wrapping.
     * @return - Buffer with wrapped data.
     * @throws SSLException - thrown on engine errors.
     */
    public static ByteBuffer doWrap(SSLEngine engine, String wrapper,
                                    int maxPacketSize, ByteBuffer app,
                                    SSLEngineResult.Status wantedStatus)
            throws SSLException {
        return doWrap(engine, wrapper, maxPacketSize,
                app, wantedStatus, null);
    }

    /**
     * Wraps data with the specified engine.
     *
     * @param engine        - SSLEngine that wraps data.
     * @param wrapper       - Set wrapper id, e.g. "server" of "client".
     *                        Used for logging only.
     * @param maxPacketSize - Max packet size to check that MFLN extension
     *                        works or zero for no check.
     * @param app           - Buffer with data to wrap.
     * @param wantedStatus  - Specifies expected result status of wrapping.
     * @param result        - Array which first element will be used to output
     *                        wrap result object.
     * @return - Buffer with wrapped data.
     * @throws SSLException - thrown on engine errors.
     */
    public static ByteBuffer doWrap(SSLEngine engine, String wrapper,
                                    int maxPacketSize, ByteBuffer app,
                                    SSLEngineResult.Status wantedStatus,
                                    SSLEngineResult[] result)
            throws SSLException {
        ByteBuffer net = ByteBuffer.allocate(engine.getSession()
                .getPacketBufferSize());
        SSLEngineResult r = engine.wrap(app, net);
        net.flip();
        int length = net.remaining();
        System.out.println(wrapper + " wrapped " + length + " bytes.");
        System.out.println(wrapper + " handshake status is "
                + engine.getHandshakeStatus() + " Result is " + r.getStatus());
        if (maxPacketSize < length && maxPacketSize != 0) {
            throw new AssertionError("Handshake wrapped net buffer length "
                    + length + " exceeds maximum packet size "
                    + maxPacketSize);
        }
        checkResult(r, wantedStatus);
        if (result != null && result.length > 0) {
            result[0] = r;
        }
        return net;
    }

    /**
     * Unwraps data with the specified engine.
     *
     * @param engine    - SSLEngine that unwraps data.
     * @param unwrapper - Set unwrapper id, e.g. "server" of "client". Used for
     *                  logging only.
     * @param net       - Buffer with data to unwrap.
     * @return - Buffer with unwrapped data.
     * @throws SSLException - thrown on engine errors.
     */
    public static ByteBuffer doUnWrap(SSLEngine engine, String unwrapper,
            ByteBuffer net) throws SSLException {
        return doUnWrap(engine, unwrapper,
                net, SSLEngineResult.Status.OK, null);
    }

    /**
     * Unwraps data with the specified engine.
     *
     * @param engine    - SSLEngine that unwraps data.
     * @param unwrapper - Set unwrapper id, e.g. "server" of "client". Used for
     *                  logging only.
     * @param net       - Buffer with data to unwrap.
     * @param result    - Array which first element will be used to output wrap
     *                  result object.
     * @return - Buffer with unwrapped data.
     * @throws SSLException - thrown on engine errors.
     */
    public static ByteBuffer doUnWrap(SSLEngine engine, String unwrapper,
            ByteBuffer net, SSLEngineResult[] result) throws SSLException {
        return doUnWrap(engine, unwrapper,
                net, SSLEngineResult.Status.OK, result);
    }

    /**
     * Unwraps data with the specified engine.
     *
     * @param engine       - SSLEngine that unwraps data.
     * @param unwrapper    - Set unwrapper id, e.g. "server" of "client".
     *                     Used for logging only.
     * @param net          - Buffer with data to unwrap.
     * @param wantedStatus - Specifies expected result status of wrapping.
     * @return - Buffer with unwrapped data.
     * @throws SSLException - thrown on engine errors.
     */
    public static ByteBuffer doUnWrap(SSLEngine engine, String unwrapper,
            ByteBuffer net,
            SSLEngineResult.Status wantedStatus) throws SSLException {
        return doUnWrap(engine, unwrapper, net, wantedStatus, null);
    }

    /**
     * Unwraps data with the specified engine.
     *
     * @param engine       - SSLEngine that unwraps data.
     * @param unwrapper    - Set unwrapper id, e.g. "server" of "client".
     *                       Used for logging only.
     * @param net          - Buffer with data to unwrap.
     * @param wantedStatus - Specifies expected result status of wrapping.
     * @param result       - Array which first element will be used to output
     *                       wrap result object.
     * @return - Buffer with unwrapped data.
     * @throws SSLException - thrown on engine errors.
     */
    public static ByteBuffer doUnWrap(SSLEngine engine, String unwrapper,
            ByteBuffer net, SSLEngineResult.Status wantedStatus,
            SSLEngineResult[] result) throws SSLException {

        ByteBuffer app = ByteBuffer.allocate(
                engine.getSession().getApplicationBufferSize());
        int length = net.remaining();
        System.out.println(unwrapper + " unwrapping " + length + " bytes...");
        SSLEngineResult r = engine.unwrap(net, app);
        app.flip();
        System.out.println(unwrapper + " handshake status is "
                + engine.getHandshakeStatus() + " Result is " + r.getStatus());
        checkResult(r, wantedStatus);
        if (result != null && result.length > 0) {
            result[0] = r;
        }
        return app;
    }

    /**
     * Does the handshake of the two specified engines according to the
     * {@code mode} specified.
     *
     * @param clientEngine  - Client SSLEngine.
     * @param serverEngine  - Server SSLEngine.
     * @param maxPacketSize - Maximum packet size for MFLN of zero for no limit.
     * @param mode          - Handshake mode according to
     *                        {@link HandshakeMode} enum.
     * @throws SSLException - thrown on engine errors.
     */
    public static void doHandshake(SSLEngine clientEngine,
        SSLEngine serverEngine,
        int maxPacketSize, HandshakeMode mode) throws SSLException {

        doHandshake(clientEngine, serverEngine, maxPacketSize, mode, false);
    }

    /**
     * Does the handshake of the two specified engines according to the
     * {@code mode} specified.
     *
     * @param clientEngine          - Client SSLEngine.
     * @param serverEngine          - Server SSLEngine.
     * @param maxPacketSize         - Maximum packet size for MFLN of zero
     *                                for no limit.
     * @param mode                  - Handshake mode according to
     *                                {@link HandshakeMode} enum.
     * @param enableReplicatedPacks - Set {@code true} to enable replicated
     *                                packet sending.
     * @throws SSLException - thrown on engine errors.
     */
    public static void doHandshake(SSLEngine clientEngine,
            SSLEngine serverEngine, int maxPacketSize,
            HandshakeMode mode,
            boolean enableReplicatedPacks) throws SSLException {

        System.out.println("=============================================");
        System.out.println("Starting handshake " + mode.name());
        int loop = 0;
        if (maxPacketSize < 0) {
            throw new Error("Test issue: maxPacketSize is less than zero!");
        }
        SSLParameters params = clientEngine.getSSLParameters();
        params.setMaximumPacketSize(maxPacketSize);
        clientEngine.setSSLParameters(params);
        params = serverEngine.getSSLParameters();
        params.setMaximumPacketSize(maxPacketSize);
        serverEngine.setSSLParameters(params);
        SSLEngine firstEngine;
        SSLEngine secondEngine;
        switch (mode) {
            case INITIAL_HANDSHAKE:
                firstEngine = clientEngine;
                secondEngine = serverEngine;
                doUnwrapForNotHandshakingStatus = false;
                clientEngine.beginHandshake();
                serverEngine.beginHandshake();
                break;
            case REHANDSHAKE_BEGIN_CLIENT:
                firstEngine = clientEngine;
                secondEngine = serverEngine;
                doUnwrapForNotHandshakingStatus = true;
                clientEngine.beginHandshake();
                break;
            case REHANDSHAKE_BEGIN_SERVER:
                firstEngine = serverEngine;
                secondEngine = clientEngine;
                doUnwrapForNotHandshakingStatus = true;
                serverEngine.beginHandshake();
                break;
            default:
                throw new Error("Test issue: unknown handshake mode");
        }
        endHandshakeLoop = false;
        while (!endHandshakeLoop) {
            if (++loop > MAX_HANDSHAKE_LOOPS) {
                throw new Error("Too much loops for handshaking");
            }
            System.out.println("============================================");
            System.out.println("Handshake loop " + loop + ": round 1");
            System.out.println("==========================");
            handshakeProcess(firstEngine, secondEngine, maxPacketSize,
                    enableReplicatedPacks);
            if (endHandshakeLoop) {
                break;
            }
            System.out.println("Handshake loop " + loop + ": round 2");
            System.out.println("==========================");
            handshakeProcess(secondEngine, firstEngine, maxPacketSize,
                    enableReplicatedPacks);
        }
    }

    /**
     * Routine to send application data from one SSLEngine to another.
     *
     * @param fromEngine - Sending engine.
     * @param toEngine   - Receiving engine.
     * @return - Result of unwrap method of the receiving engine.
     * @throws SSLException - thrown on engine errors.
     */
    public static SSLEngineResult sendApplicationData(SSLEngine fromEngine,
                                                      SSLEngine toEngine)
            throws SSLException {
        String sender = null;
        String reciever = null;
        String excMsgSent = EXCHANGE_MSG_SENT;
        if (fromEngine.getUseClientMode() && !toEngine.getUseClientMode()) {
            sender = "Client";
            reciever = "Server";
            excMsgSent += " Client.";
        } else if (toEngine.getUseClientMode() &&
                !fromEngine.getUseClientMode()) {
            sender = "Server";
            reciever = "Client";
            excMsgSent += " Server.";
        } else {
            throw new Error("Test issue: both engines are in the same mode");
        }
        System.out.println("=============================================");
        System.out.println("Trying to send application data from " + sender
                + " to " + reciever);
        ByteBuffer clientAppSent
                = ByteBuffer.wrap(excMsgSent.getBytes());
        net = doWrap(fromEngine, sender, 0, clientAppSent);
        SSLEngineResult[] r = new SSLEngineResult[1];
        ByteBuffer serverAppRecv = doUnWrap(toEngine, reciever, net, r);
        byte[] serverAppRecvTrunc = Arrays.copyOf(serverAppRecv.array(),
                serverAppRecv.limit());
        String msgRecv = new String(serverAppRecvTrunc);
        if (!msgRecv.equals(excMsgSent)) {
            throw new AssertionError(sender + " to " + reciever
                    + ": application data"
                    + " has been altered while sending."
                    + " Message sent: " + "\"" + excMsgSent + "\"."
                    + " Message recieved: " + "\"" + msgRecv + "\".");
        }
        System.out.println("Successful sending application data from " + sender
                + " to " + reciever);
        return r[0];
    }

    /**
     * Close engines by sending "close outbound" message from one SSLEngine to
     * another.
     *
     * @param fromEngine - Sending engine.
     * @param toEngine   - Receiving engine.
     * @throws SSLException - thrown on engine errors.
     */
    public static void closeEngines(SSLEngine fromEngine,
                                    SSLEngine toEngine) throws SSLException {
        String from = null;
        String to = null;
        ByteBuffer app;
        if (fromEngine.getUseClientMode() && !toEngine.getUseClientMode()) {
            from = "Client";
            to = "Server";
        } else if (toEngine.getUseClientMode() &&
                !fromEngine.getUseClientMode()) {
            from = "Server";
            to = "Client";
        } else {
            throw new Error("Both engines are in the same mode");
        }
        System.out.println("=============================================");
        System.out.println(
                "Trying to close engines from " + from + " to " + to);
        // Sending close outbound request to peer
        fromEngine.closeOutbound();
        app = ByteBuffer.allocate(
                fromEngine.getSession().getApplicationBufferSize());
        net = doWrap(fromEngine, from, 0, app, SSLEngineResult.Status.CLOSED);
        doUnWrap(toEngine, to, net, SSLEngineResult.Status.CLOSED);
        app = ByteBuffer.allocate(
                fromEngine.getSession().getApplicationBufferSize());
        net = doWrap(toEngine, to, 0, app, SSLEngineResult.Status.CLOSED);
        doUnWrap(fromEngine, from, net, SSLEngineResult.Status.CLOSED);
        if (!toEngine.isInboundDone()) {
            throw new AssertionError(from + " sent close request to " + to
                    + ", but " + to + "did not close inbound.");
        }
        // Executing close inbound
        fromEngine.closeInbound();
        app = ByteBuffer.allocate(
                fromEngine.getSession().getApplicationBufferSize());
        net = doWrap(fromEngine, from, 0, app, SSLEngineResult.Status.CLOSED);
        doUnWrap(toEngine, to, net, SSLEngineResult.Status.CLOSED);
        if (!toEngine.isOutboundDone()) {
            throw new AssertionError(from + "sent close request to " + to
                    + ", but " + to + "did not close outbound.");
        }
        System.out.println("Successful closing from " + from + " to " + to);
    }

    /**
     * Runs the same test case for all given {@code ciphers}. Method counts all
     * failures and throws {@code AssertionError} if one or more tests fail.
     *
     * @param ciphers - Ciphers that should be tested.
     */
    public void runTests(Ciphers ciphers) {
        int total = ciphers.ciphers.length;
        int failed = testSomeCiphers(ciphers);
        if (failed > 0) {
            throw new AssertionError("" + failed + " of " + total
                    + " tests failed!");
        }
        System.out.println("All tests passed!");
    }

    /**
     * Runs test cases for ciphers defined by the test mode.
     */
    public void runTests() {
        switch (TEST_MODE) {
            case "norm":
            case "norm_sni":
                switch (TESTED_SECURITY_PROTOCOL) {
                    case "DTLSv1.0":
                    case "TLSv1":
                    case "TLSv1.1":
                        runTests(Ciphers.SUPPORTED_NON_KRB_NON_SHA_CIPHERS);
                        break;
                    case "DTLSv1.1":
                    case "TLSv1.2":
                        runTests(Ciphers.SUPPORTED_NON_KRB_CIPHERS);
                        break;
                    case "TLSv1.3":
                        runTests(Ciphers.TLS13_CIPHERS);
                        break;
                }
                break;
            case "krb":
                runTests(Ciphers.SUPPORTED_KRB_CIPHERS);
                break;
            default:
                throw new Error(
                        "Test error: unexpected test mode: " + TEST_MODE);
        }
    }

    /**
     * Returns maxPacketSize value used for MFLN extension testing
     *
     * @return - MLFN extension max packet size.
     */
    public int getMaxPacketSize() {
        return maxPacketSize;
    }

    /**
     * Checks that status of result {@code r} is {@code wantedStatus}.
     *
     * @param r            - Result.
     * @param wantedStatus - Wanted status of the result.
     * @throws AssertionError - if status or {@code r} is not
     *                        {@code wantedStatus}.
     */
    public static void checkResult(SSLEngineResult r,
                                   SSLEngineResult.Status wantedStatus) {
        SSLEngineResult.Status rs = r.getStatus();
        if (!rs.equals(wantedStatus)) {
            throw new AssertionError("Unexpected status " + rs.name()
                    + ", should be " + wantedStatus.name());
        }
    }

    /**
     * Returns SSLContext with TESTED_SECURITY_PROTOCOL protocol and
     * sets up keys.
     *
     * @return - SSLContext with a protocol specified by
     *           TESTED_SECURITY_PROTOCOL.
     */
    public static SSLContext getContext() {
        try {
            java.security.Security.setProperty(
                    "jdk.tls.disabledAlgorithms", "");
            java.security.Security.setProperty(
                    "jdk.certpath.disabledAlgorithms", "");
            KeyStore ks = KeyStore.getInstance("JKS");
            KeyStore ts = KeyStore.getInstance("JKS");
            char[] passphrase = PASSWD.toCharArray();
            try (FileInputStream keyFileStream =
                    new FileInputStream(KEY_FILE_NAME)) {
                ks.load(keyFileStream, passphrase);
            }
            try (FileInputStream trustFileStream =
                    new FileInputStream(TRUST_FILE_NAME)) {
                ts.load(trustFileStream, passphrase);
            }
            KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
            kmf.init(ks, passphrase);
            TrustManagerFactory tmf =
                    TrustManagerFactory.getInstance("SunX509");
            tmf.init(ts);
            SSLContext sslCtx =
                    SSLContext.getInstance(TESTED_SECURITY_PROTOCOL);
            sslCtx.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
            return sslCtx;
        } catch (KeyStoreException | IOException | NoSuchAlgorithmException |
                CertificateException | UnrecoverableKeyException |
                KeyManagementException ex) {
            throw new Error("Unexpected exception", ex);
        }
    }

    /**
     * Sets up and starts kerberos KDC server.
     */
    public static void setUpAndStartKDC() {
        String servicePrincipal = "host/" + SERVER_NAME + "@" + KRB_REALM;
        Map<String, String> principals = new HashMap<>();
        principals.put(KRB_USER_PRINCIPAL, KRB_USER_PASSWORD);
        principals.put(KRBTGT_PRINCIPAL, null);
        principals.put(servicePrincipal, null);
        System.setProperty("java.security.krb5.conf", KRB5_CONF_FILENAME);
        startKDC(KRB_REALM, principals, KTAB_FILENAME);
        System.setProperty("java.security.auth.login.config",
                TEST_SRC + FS + JAAS_CONF_FILE);
        System.setProperty("javax.security.auth.useSubjectCredsOnly", "false");
    }

    /**
     * Sets up and starts kerberos KDC server if
     * SSLEngineTestCase.TEST_MODE is "krb".
     */
    public static void setUpAndStartKDCIfNeeded() {
        if (TEST_MODE.equals("krb")) {
            setUpAndStartKDC();
        }
    }

    /**
     * Returns client ssl engine.
     *
     * @param context - SSLContext to get SSLEngine from.
     * @param useSNI  - flag used to enable or disable using SNI extension.
     *                Needed for Kerberos.
     */
    public static SSLEngine getClientSSLEngine(
            SSLContext context, boolean useSNI) {

        SSLEngine clientEngine = context.createSSLEngine(HOST, 80);
        clientEngine.setUseClientMode(true);
        if (useSNI) {
            SNIHostName serverName = new SNIHostName(SERVER_NAME);
            List<SNIServerName> serverNames = new ArrayList<>();
            serverNames.add(serverName);
            SSLParameters params = clientEngine.getSSLParameters();
            params.setServerNames(serverNames);
            clientEngine.setSSLParameters(params);
        }
        return clientEngine;
    }

    /**
     * Returns server ssl engine.
     *
     * @param context - SSLContext to get SSLEngine from.
     * @param useSNI  - flag used to enable or disable using SNI extension.
     *                Needed for Kerberos.
     */
    public static SSLEngine getServerSSLEngine(
            SSLContext context, boolean useSNI) {

        SSLEngine serverEngine = context.createSSLEngine();
        serverEngine.setUseClientMode(false);
        if (useSNI) {
            SNIMatcher matcher = SNIHostName.createSNIMatcher(SNI_PATTERN);
            List<SNIMatcher> matchers = new ArrayList<>();
            matchers.add(matcher);
            SSLParameters params = serverEngine.getSSLParameters();
            params.setSNIMatchers(matchers);
            serverEngine.setSSLParameters(params);
        }
        return serverEngine;
    }

    /**
     * Runs the test case for one cipher suite.
     *
     * @param cipher - Cipher suite name.
     * @throws SSLException - If tests fails.
     */
    abstract protected void testOneCipher(String cipher)
            throws SSLException;

    /**
     * Iterates through an array of ciphers and runs the same test case for
     * every entry.
     *
     * @param ciphers - Array of cipher names.
     * @return - Number of tests failed.
     */
    protected int testSomeCiphers(Ciphers ciphers) {
        int failedNum = 0;
        String description = ciphers.description;
        System.out.println("===============================================");
        System.out.println(description + " ciphers testing");
        System.out.println("===========================================");
        for (String cs : ciphers.ciphers) {
            System.out.println("---------------------------------------");
            System.out.println("Testing cipher suite " + cs);
            System.out.println("---------------------------------------");
            Throwable error = null;

            // Reset global mutable static variables
            net = null;
            doUnwrapForNotHandshakingStatus = false;
            endHandshakeLoop = false;

            try {
                testOneCipher(cs);
            } catch (Throwable t) {
                error = t;
            }
            switch (ciphers) {
                case SUPPORTED_NON_KRB_CIPHERS:
                case SUPPORTED_NON_KRB_NON_SHA_CIPHERS:
                case SUPPORTED_KRB_CIPHERS:
                case ENABLED_NON_KRB_NOT_ANON_CIPHERS:
                case TLS13_CIPHERS:
                    if (error != null) {
                        System.out.println("Test Failed: " + cs);
                        System.err.println("Test Exception for " + cs);
                        error.printStackTrace();
                        failedNum++;
                    } else {
                        System.out.println("Test Passed: " + cs);
                    }
                    break;
                case UNSUPPORTED_CIPHERS:
                    if (error == null) {
                        System.out.println("Test Failed: " + cs);
                        System.err.println("Test for " + cs +
                                " should have thrown " +
                                "IllegalArgumentException, but it has not!");
                        failedNum++;
                    } else if (!(error instanceof IllegalArgumentException)) {
                        System.out.println("Test Failed: " + cs);
                        System.err.println("Test Exception for " + cs);
                        error.printStackTrace();
                        failedNum++;
                    } else {
                        System.out.println("Test Passed: " + cs);
                    }
                    break;
                default:
                    throw new Error("Test issue: unexpected ciphers: "
                            + ciphers.name());
            }
        }

        return failedNum;
    }

    /**
     * Method used for the handshake routine.
     *
     * @param wrapingEngine         - Engine that is expected to wrap data.
     * @param unwrapingEngine       - Engine that is expected to unwrap data.
     * @param maxPacketSize         - Maximum packet size for MFLN of zero
     *                                for no limit.
     * @param enableReplicatedPacks - Set {@code true} to enable replicated
     *                                packet sending.
     * @throws SSLException - thrown on engine errors.
     */
    private static void handshakeProcess(SSLEngine wrapingEngine,
            SSLEngine unwrapingEngine,
            int maxPacketSize,
            boolean enableReplicatedPacks) throws SSLException {

        HandshakeStatus wrapingHSStatus = wrapingEngine.getHandshakeStatus();
        HandshakeStatus unwrapingHSStatus =
                unwrapingEngine.getHandshakeStatus();
        SSLEngineResult r;
        String wrapper, unwrapper;
        if (wrapingEngine.getUseClientMode()
                && !unwrapingEngine.getUseClientMode()) {
            wrapper = "Client";
            unwrapper = "Server";
        } else if (unwrapingEngine.getUseClientMode()
                && !wrapingEngine.getUseClientMode()) {
            wrapper = "Server";
            unwrapper = "Client";
        } else {
            throw new Error("Both engines are in the same mode");
        }
        System.out.println(
                wrapper + " handshake (wrap) status " + wrapingHSStatus);
        System.out.println(
                unwrapper + " handshake (unwrap) status " + unwrapingHSStatus);

        ByteBuffer netReplicatedClient = null;
        ByteBuffer netReplicatedServer = null;
        switch (wrapingHSStatus) {
            case NEED_WRAP:
                if (enableReplicatedPacks) {
                    if (net != null) {
                        net.flip();
                        if (net.remaining() != 0) {
                            if (wrapingEngine.getUseClientMode()) {
                                netReplicatedServer = net;
                            } else {
                                netReplicatedClient = net;
                            }
                        }
                    }
                }
                ByteBuffer app = ByteBuffer.allocate(
                        wrapingEngine.getSession().getApplicationBufferSize());
                net = doWrap(wrapingEngine, wrapper, maxPacketSize, app);
                wrapingHSStatus = wrapingEngine.getHandshakeStatus();
                // No break, falling into unwrapping.
            case NOT_HANDSHAKING:
                switch (unwrapingHSStatus) {
                    case NEED_TASK:
                        runDelegatedTasks(unwrapingEngine);
                    case NEED_UNWRAP:
                        doUnWrap(unwrapingEngine, unwrapper, net);
                        if (enableReplicatedPacks) {
                            System.out.println(unwrapper +
                                    " unwrapping replicated packet...");
                            if (unwrapingEngine.getHandshakeStatus()
                                    .equals(HandshakeStatus.NEED_TASK)) {
                                runDelegatedTasks(unwrapingEngine);
                            }
                            ByteBuffer netReplicated;
                            if (unwrapingEngine.getUseClientMode()) {
                                netReplicated = netReplicatedClient;
                            } else {
                                netReplicated = netReplicatedServer;
                            }
                            if (netReplicated != null) {
                                doUnWrap(unwrapingEngine,
                                        unwrapper, netReplicated);
                            } else {
                                net.flip();
                                doUnWrap(unwrapingEngine, unwrapper, net);
                            }
                        }
                        break;
                    case NEED_UNWRAP_AGAIN:
                        break;
                    case NOT_HANDSHAKING:
                        if (doUnwrapForNotHandshakingStatus) {
                            System.out.println("Not handshake status unwrap");
                            doUnWrap(unwrapingEngine, unwrapper, net);
                            doUnwrapForNotHandshakingStatus = false;
                            break;
                        } else {
                            if (wrapingHSStatus ==
                                        HandshakeStatus.NOT_HANDSHAKING) {
                                System.out.println("Handshake is completed");
                                endHandshakeLoop = true;
                            }
                        }
                        break;
                    case NEED_WRAP:
                        SSLSession session = unwrapingEngine.getSession();
                        int bufferSize = session.getApplicationBufferSize();
                        ByteBuffer b = ByteBuffer.allocate(bufferSize);
                        net = doWrap(unwrapingEngine,
                                        unwrapper, maxPacketSize, b);
                        unwrapingHSStatus =
                                unwrapingEngine.getHandshakeStatus();
                        if ((wrapingHSStatus ==
                                    HandshakeStatus.NOT_HANDSHAKING) &&
                            (unwrapingHSStatus ==
                                    HandshakeStatus.NOT_HANDSHAKING)) {

                            System.out.println("Handshake is completed");
                            endHandshakeLoop = true;
                        }

                        break;
                    default:
                        throw new Error(
                                "Unexpected unwraping engine handshake status "
                                + unwrapingHSStatus.name());
                }
                break;
            case NEED_UNWRAP:
                break;
            case NEED_UNWRAP_AGAIN:
                net.flip();
                doUnWrap(wrapingEngine, wrapper, net);
                break;
            case NEED_TASK:
                runDelegatedTasks(wrapingEngine);
                break;
            default:
                throw new Error("Unexpected wraping engine handshake status "
                        + wrapingHSStatus.name());
        }
    }

    private static void runDelegatedTasks(SSLEngine engine) {
        Runnable runnable;
        System.out.println("Running delegated tasks...");
        while ((runnable = engine.getDelegatedTask()) != null) {
            runnable.run();
        }
        HandshakeStatus hs = engine.getHandshakeStatus();
        if (hs == HandshakeStatus.NEED_TASK) {
            throw new Error("Handshake shouldn't need additional tasks.");
        }
    }

    /**
     * Start a KDC server:
     * - create a KDC instance
     * - create Kerberos principals
     * - save Kerberos configuration
     * - save keys to keytab file
     * - no pre-auth is required
     */
    private static void startKDC(String realm, Map<String, String> principals,
                                 String ktab) {
        try {
            KDC kdc = KDC.create(realm, HOST, 0, true);
            kdc.setOption(KDC.Option.PREAUTH_REQUIRED, Boolean.FALSE);
            if (principals != null) {
                principals.entrySet().stream().forEach((entry) -> {
                    String name = entry.getKey();
                    String password = entry.getValue();
                    if (password == null || password.isEmpty()) {
                        System.out.println("KDC: add a principal '" + name
                                + "' with a random password");
                        kdc.addPrincipalRandKey(name);
                    } else {
                        System.out.println("KDC: add a principal '" + name
                                + "' with '" + password + "' password");
                        kdc.addPrincipal(name, password.toCharArray());
                    }
                });
            }
            KDC.saveConfig(KRB5_CONF_FILENAME, kdc);
            if (ktab != null) {
                File ktabFile = new File(ktab);
                if (ktabFile.exists()) {
                    System.out.println("KDC: append keys to an exising "
                            + "keytab file " + ktab);
                    kdc.appendKtab(ktab);
                } else {
                    System.out.println("KDC: create a new keytab file "
                            + ktab);
                    kdc.writeKtab(ktab);
                }
            }
            System.out.println("KDC: started on " + HOST + ":" + kdc.getPort()
                    + " with '" + realm + "' realm");
        } catch (Exception e) {
            throw new RuntimeException("KDC: unexpected exception", e);
        }
    }
}
