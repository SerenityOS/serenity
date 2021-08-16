/*
 * Copyright (c) 1999, 2021, Oracle and/or its affiliates. All rights reserved.
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

package sun.security.ssl;

import java.io.FileInputStream;
import java.net.Socket;
import java.security.*;
import java.security.cert.*;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import javax.net.ssl.*;
import sun.security.action.GetPropertyAction;
import sun.security.provider.certpath.AlgorithmChecker;
import sun.security.validator.Validator;

/**
 * Implementation of an SSLContext.
 *
 * Implementation note: Instances of this class and the child classes are
 * immutable, except that the context initialization (SSLContext.init()) may
 * reset the key, trust managers and source of secure random.
 */

public abstract class SSLContextImpl extends SSLContextSpi {

    private final EphemeralKeyManager ephemeralKeyManager;
    private final SSLSessionContextImpl clientCache;
    private final SSLSessionContextImpl serverCache;

    private boolean isInitialized;

    private X509ExtendedKeyManager keyManager;
    private X509TrustManager trustManager;
    private SecureRandom secureRandom;

    // DTLS cookie exchange manager
    private volatile HelloCookieManager.Builder helloCookieManagerBuilder;

    private final boolean clientEnableStapling = Utilities.getBooleanProperty(
            "jdk.tls.client.enableStatusRequestExtension", true);
    private final boolean serverEnableStapling = Utilities.getBooleanProperty(
            "jdk.tls.server.enableStatusRequestExtension", false);
    private static final Collection<CipherSuite> clientCustomizedCipherSuites =
            getCustomizedCipherSuites("jdk.tls.client.cipherSuites");
    private static final Collection<CipherSuite> serverCustomizedCipherSuites =
            getCustomizedCipherSuites("jdk.tls.server.cipherSuites");

    private volatile StatusResponseManager statusResponseManager;

    private final ReentrantLock contextLock = new ReentrantLock();
    final HashMap<Integer,
            SessionTicketExtension.StatelessKey> keyHashMap = new HashMap<>();


    SSLContextImpl() {
        ephemeralKeyManager = new EphemeralKeyManager();
        clientCache = new SSLSessionContextImpl(false);
        serverCache = new SSLSessionContextImpl(true);
    }

    @Override
    protected void engineInit(KeyManager[] km, TrustManager[] tm,
                                SecureRandom sr) throws KeyManagementException {
        isInitialized = false;
        keyManager = chooseKeyManager(km);

        if (tm == null) {
            try {
                TrustManagerFactory tmf = TrustManagerFactory.getInstance(
                        TrustManagerFactory.getDefaultAlgorithm());
                tmf.init((KeyStore)null);
                tm = tmf.getTrustManagers();
            } catch (Exception e) {
                // eat
            }
        }
        trustManager = chooseTrustManager(tm);

        if (sr == null) {
            secureRandom = new SecureRandom();
        } else {
            secureRandom = sr;
        }

        /*
         * The initial delay of seeding the random number generator
         * could be long enough to cause the initial handshake on our
         * first connection to timeout and fail. Make sure it is
         * primed and ready by getting some initial output from it.
         */
        if (SSLLogger.isOn && SSLLogger.isOn("ssl,sslctx")) {
            SSLLogger.finest("trigger seeding of SecureRandom");
        }
        secureRandom.nextInt();
        if (SSLLogger.isOn && SSLLogger.isOn("ssl,sslctx")) {
            SSLLogger.finest("done seeding of SecureRandom");
        }

        isInitialized = true;
    }

    private X509TrustManager chooseTrustManager(TrustManager[] tm)
            throws KeyManagementException {
        // We only use the first instance of X509TrustManager passed to us.
        for (int i = 0; tm != null && i < tm.length; i++) {
            if (tm[i] instanceof X509TrustManager) {
                if (tm[i] instanceof X509ExtendedTrustManager) {
                    return (X509TrustManager)tm[i];
                } else {
                    return new AbstractTrustManagerWrapper(
                                        (X509TrustManager)tm[i]);
                }
            }
        }

        // nothing found, return a dummy X509TrustManager.
        return DummyX509TrustManager.INSTANCE;
    }

    private X509ExtendedKeyManager chooseKeyManager(KeyManager[] kms)
            throws KeyManagementException {
        for (int i = 0; kms != null && i < kms.length; i++) {
            KeyManager km = kms[i];
            if (!(km instanceof X509KeyManager)) {
                continue;
            }

            if (km instanceof X509ExtendedKeyManager) {
                return (X509ExtendedKeyManager)km;
            }

            if (SSLLogger.isOn && SSLLogger.isOn("ssl,sslctx")) {
                SSLLogger.warning(
                    "X509KeyManager passed to SSLContext.init():  need an " +
                    "X509ExtendedKeyManager for SSLEngine use");
            }
            return new AbstractKeyManagerWrapper((X509KeyManager)km);
        }

        // nothing found, return a dummy X509ExtendedKeyManager
        return DummyX509KeyManager.INSTANCE;
    }

    abstract SSLEngine createSSLEngineImpl();
    abstract SSLEngine createSSLEngineImpl(String host, int port);

    @Override
    protected SSLEngine engineCreateSSLEngine() {
        if (!isInitialized) {
            throw new IllegalStateException("SSLContext is not initialized");
        }
        return createSSLEngineImpl();
    }

    @Override
    protected SSLEngine engineCreateSSLEngine(String host, int port) {
        if (!isInitialized) {
            throw new IllegalStateException("SSLContext is not initialized");
        }
        return createSSLEngineImpl(host, port);
    }

    @Override
    protected SSLSocketFactory engineGetSocketFactory() {
        if (!isInitialized) {
            throw new IllegalStateException("SSLContext is not initialized");
        }
        if (isDTLS()) {
            throw new UnsupportedOperationException(
                    "DTLS not supported with SSLSocket");
        }
       return new SSLSocketFactoryImpl(this);
    }

    @Override
    protected SSLServerSocketFactory engineGetServerSocketFactory() {
        if (!isInitialized) {
            throw new IllegalStateException("SSLContext is not initialized");
        }
        if (isDTLS()) {
            throw new UnsupportedOperationException(
                    "DTLS not supported with SSLServerSocket");
        }
        return new SSLServerSocketFactoryImpl(this);
    }

    @Override
    protected SSLSessionContext engineGetClientSessionContext() {
        return clientCache;
    }

    @Override
    protected SSLSessionContext engineGetServerSessionContext() {
        return serverCache;
    }

    SecureRandom getSecureRandom() {
        return secureRandom;
    }

    X509ExtendedKeyManager getX509KeyManager() {
        return keyManager;
    }

    X509TrustManager getX509TrustManager() {
        return trustManager;
    }

    EphemeralKeyManager getEphemeralKeyManager() {
        return ephemeralKeyManager;
    }

    // Used for DTLS in server mode only.
    HelloCookieManager getHelloCookieManager(ProtocolVersion protocolVersion) {
        if (helloCookieManagerBuilder == null) {
            contextLock.lock();
            try {
                if (helloCookieManagerBuilder == null) {
                    helloCookieManagerBuilder =
                            new HelloCookieManager.Builder(secureRandom);
                }
            } finally {
                contextLock.unlock();
            }
        }

        return helloCookieManagerBuilder.valueOf(protocolVersion);
    }

    StatusResponseManager getStatusResponseManager() {
        if (serverEnableStapling && statusResponseManager == null) {
            contextLock.lock();
            try {
                if (statusResponseManager == null) {
                    if (SSLLogger.isOn && SSLLogger.isOn("ssl,sslctx")) {
                        SSLLogger.finest(
                                "Initializing StatusResponseManager");
                    }
                    statusResponseManager = new StatusResponseManager();
                }
            } finally {
                contextLock.unlock();
            }
        }

        return statusResponseManager;
    }

    // Get supported protocols.
    abstract List<ProtocolVersion> getSupportedProtocolVersions();

    // Get default protocols for server mode.
    abstract List<ProtocolVersion> getServerDefaultProtocolVersions();

    // Get default protocols for client mode.
    abstract List<ProtocolVersion> getClientDefaultProtocolVersions();

    // Get supported CipherSuite list.
    abstract List<CipherSuite> getSupportedCipherSuites();

    // Get default CipherSuite list for server mode.
    abstract List<CipherSuite> getServerDefaultCipherSuites();

    // Get default CipherSuite list for client mode.
    abstract List<CipherSuite> getClientDefaultCipherSuites();

    // Is the context for DTLS protocols?
    abstract boolean isDTLS();

    // Get default protocols.
    List<ProtocolVersion> getDefaultProtocolVersions(boolean roleIsServer) {
        return roleIsServer ? getServerDefaultProtocolVersions()
                            : getClientDefaultProtocolVersions();
    }

    // Get default CipherSuite list.
    List<CipherSuite> getDefaultCipherSuites(boolean roleIsServer) {
        return roleIsServer ? getServerDefaultCipherSuites()
                            : getClientDefaultCipherSuites();
    }

    /**
     * Return whether a protocol list is the original default enabled
     * protocols.  See: SSLSocket/SSLEngine.setEnabledProtocols()
     */
    boolean isDefaultProtocolVesions(List<ProtocolVersion> protocols) {
        return (protocols == getServerDefaultProtocolVersions()) ||
               (protocols == getClientDefaultProtocolVersions());
    }

    /**
     * Return whether a protocol list is the original default enabled
     * protocols.  See: SSLSocket/SSLEngine.setEnabledProtocols()
     */
    boolean isDefaultCipherSuiteList(List<CipherSuite> cipherSuites) {
        return (cipherSuites == getServerDefaultCipherSuites()) ||
               (cipherSuites == getClientDefaultCipherSuites());
    }

    /**
     * Return whether client or server side stapling has been enabled
     * for this SSLContextImpl
     * @param isClient true if the caller is operating in a client side role,
     * false if acting as a server.
     * @return true if stapling has been enabled for the specified role, false
     * otherwise.
     */
    boolean isStaplingEnabled(boolean isClient) {
        return isClient ? clientEnableStapling : serverEnableStapling;
    }

    /*
     * Return the list of all available CipherSuites that are supported
     * using currently installed providers.
     */
    private static List<CipherSuite> getApplicableSupportedCipherSuites(
            List<ProtocolVersion> protocols) {

        return getApplicableCipherSuites(
                CipherSuite.allowedCipherSuites(), protocols);
    }

    /*
     * Return the list of all available CipherSuites that are default enabled
     * in client or server side.
     */
    private static List<CipherSuite> getApplicableEnabledCipherSuites(
            List<ProtocolVersion> protocols, boolean isClient) {

        if (isClient) {
            if (!clientCustomizedCipherSuites.isEmpty()) {
                return getApplicableCipherSuites(
                        clientCustomizedCipherSuites, protocols);
            }
        } else {
            if (!serverCustomizedCipherSuites.isEmpty()) {
                return getApplicableCipherSuites(
                        serverCustomizedCipherSuites, protocols);
            }
        }

        return getApplicableCipherSuites(
                CipherSuite.defaultCipherSuites(), protocols);
    }

    /*
     * Return the list of available CipherSuites which are applicable to
     * the specified protocols.
     */
    private static List<CipherSuite> getApplicableCipherSuites(
            Collection<CipherSuite> allowedCipherSuites,
            List<ProtocolVersion> protocols) {
        LinkedHashSet<CipherSuite> suites = new LinkedHashSet<>();
        if (protocols != null && (!protocols.isEmpty())) {
            for (CipherSuite suite : allowedCipherSuites) {
                if (!suite.isAvailable()) {
                    continue;
                }

                boolean isSupported = false;
                for (ProtocolVersion protocol : protocols) {
                    if (!suite.supports(protocol) ||
                            !suite.bulkCipher.isAvailable()) {
                        continue;
                    }

                    if (SSLAlgorithmConstraints.DEFAULT.permits(
                            EnumSet.of(CryptoPrimitive.KEY_AGREEMENT),
                            suite.name, null)) {
                        suites.add(suite);
                        isSupported = true;
                    } else if (SSLLogger.isOn &&
                            SSLLogger.isOn("ssl,sslctx,verbose")) {
                        SSLLogger.fine(
                                "Ignore disabled cipher suite: " + suite.name);
                    }

                    break;
                }

                if (!isSupported && SSLLogger.isOn &&
                        SSLLogger.isOn("ssl,sslctx,verbose")) {
                    SSLLogger.finest(
                            "Ignore unsupported cipher suite: " + suite);
                }
            }
        }

        return new ArrayList<>(suites);
    }

    /*
     * Get the customized cipher suites specified by the given system property.
     */
    private static Collection<CipherSuite> getCustomizedCipherSuites(
            String propertyName) {

        String property = GetPropertyAction.privilegedGetProperty(propertyName);
        if (SSLLogger.isOn && SSLLogger.isOn("ssl,sslctx")) {
            SSLLogger.fine(
                    "System property " + propertyName + " is set to '" +
                    property + "'");
        }
        if (property != null && !property.isEmpty()) {
            // remove double quote marks from beginning/end of the property
            if (property.length() > 1 && property.charAt(0) == '"' &&
                    property.charAt(property.length() - 1) == '"') {
                property = property.substring(1, property.length() - 1);
            }
        }

        if (property != null && !property.isEmpty()) {
            String[] cipherSuiteNames = property.split(",");
            Collection<CipherSuite> cipherSuites =
                        new ArrayList<>(cipherSuiteNames.length);
            for (int i = 0; i < cipherSuiteNames.length; i++) {
                cipherSuiteNames[i] = cipherSuiteNames[i].trim();
                if (cipherSuiteNames[i].isEmpty()) {
                    continue;
                }

                CipherSuite suite;
                try {
                    suite = CipherSuite.nameOf(cipherSuiteNames[i]);
                } catch (IllegalArgumentException iae) {
                    if (SSLLogger.isOn && SSLLogger.isOn("ssl,sslctx")) {
                        SSLLogger.fine(
                                "Unknown or unsupported cipher suite name: " +
                                cipherSuiteNames[i]);
                    }

                    continue;
                }

                if (suite != null && suite.isAvailable()) {
                    cipherSuites.add(suite);
                } else {
                    if (SSLLogger.isOn && SSLLogger.isOn("ssl,sslctx")) {
                        SSLLogger.fine(
                                "The current installed providers do not " +
                                "support cipher suite: " + cipherSuiteNames[i]);
                    }
                }
            }

            return cipherSuites;
        }

        return Collections.emptyList();
    }


    private static List<ProtocolVersion> getAvailableProtocols(
            ProtocolVersion[] protocolCandidates) {

        List<ProtocolVersion> availableProtocols =
                Collections.<ProtocolVersion>emptyList();
        if (protocolCandidates != null && protocolCandidates.length != 0) {
            availableProtocols = new ArrayList<>(protocolCandidates.length);
            for (ProtocolVersion p : protocolCandidates) {
                if (p.isAvailable) {
                    availableProtocols.add(p);
                }
            }
        }

        return availableProtocols;
    }

    /*
     * The SSLContext implementation for SSL/(D)TLS algorithm
     *
     * SSL/TLS protocols specify the forward compatibility and version
     * roll-back attack protections, however, a number of SSL/TLS server
     * vendors did not implement these aspects properly, and some current
     * SSL/TLS servers may refuse to talk to a TLS 1.1 or later client.
     *
     * Considering above interoperability issues, SunJSSE will not set
     * TLS 1.1 and TLS 1.2 as the enabled protocols for client by default.
     *
     * For SSL/TLS servers, there is no such interoperability issues as
     * SSL/TLS clients. In SunJSSE, TLS 1.1 or later version will be the
     * enabled protocols for server by default.
     *
     * We may change the behavior when popular TLS/SSL vendors support TLS
     * forward compatibility properly.
     *
     * SSLv2Hello is no longer necessary.  This interoperability option was
     * put in place in the late 90's when SSLv3/TLS1.0 were relatively new
     * and there were a fair number of SSLv2-only servers deployed.  Because
     * of the security issues in SSLv2, it is rarely (if ever) used, as
     * deployments should now be using SSLv3 and TLSv1.
     *
     * Considering the issues of SSLv2Hello, we should not enable SSLv2Hello
     * by default. Applications still can use it by enabling SSLv2Hello with
     * the series of setEnabledProtocols APIs.
     */

    /*
     * The base abstract SSLContext implementation for the Transport Layer
     * Security (TLS) protocols.
     *
     * This abstract class encapsulates supported and the default server
     * SSL/TLS parameters.
     *
     * @see SSLContext
     */
    private abstract static class AbstractTLSContext extends SSLContextImpl {
        private static final List<ProtocolVersion> supportedProtocols;
        private static final List<ProtocolVersion> serverDefaultProtocols;

        private static final List<CipherSuite> supportedCipherSuites;
        private static final List<CipherSuite> serverDefaultCipherSuites;

        static {
            supportedProtocols = Arrays.asList(
                ProtocolVersion.TLS13,
                ProtocolVersion.TLS12,
                ProtocolVersion.TLS11,
                ProtocolVersion.TLS10,
                ProtocolVersion.SSL30,
                ProtocolVersion.SSL20Hello
            );

            serverDefaultProtocols = getAvailableProtocols(
                    new ProtocolVersion[] {
                ProtocolVersion.TLS13,
                ProtocolVersion.TLS12,
                ProtocolVersion.TLS11,
                ProtocolVersion.TLS10
            });

            supportedCipherSuites = getApplicableSupportedCipherSuites(
                    supportedProtocols);
            serverDefaultCipherSuites = getApplicableEnabledCipherSuites(
                    serverDefaultProtocols, false);
        }

        @Override
        List<ProtocolVersion> getSupportedProtocolVersions() {
            return supportedProtocols;
        }

        @Override
        List<CipherSuite> getSupportedCipherSuites() {
            return supportedCipherSuites;
        }

        @Override
        List<ProtocolVersion> getServerDefaultProtocolVersions() {
            return serverDefaultProtocols;
        }

        @Override
        List<CipherSuite> getServerDefaultCipherSuites() {
            return serverDefaultCipherSuites;
        }

        @Override
        SSLEngine createSSLEngineImpl() {
            return new SSLEngineImpl(this);
        }

        @Override
        SSLEngine createSSLEngineImpl(String host, int port) {
            return new SSLEngineImpl(this, host, port);
        }

        @Override
        boolean isDTLS() {
            return false;
        }
    }

    /*
     * The SSLContext implementation for SSLv3 and TLS10 algorithm
     *
     * @see SSLContext
     */
    public static final class TLS10Context extends AbstractTLSContext {
        private static final List<ProtocolVersion> clientDefaultProtocols;
        private static final List<CipherSuite> clientDefaultCipherSuites;

        static {
            clientDefaultProtocols = getAvailableProtocols(
                    new ProtocolVersion[] {
                ProtocolVersion.TLS10
            });

            clientDefaultCipherSuites = getApplicableEnabledCipherSuites(
                    clientDefaultProtocols, true);
        }

        @Override
        List<ProtocolVersion> getClientDefaultProtocolVersions() {
            return clientDefaultProtocols;
        }

        @Override
        List<CipherSuite> getClientDefaultCipherSuites() {
            return clientDefaultCipherSuites;
        }
    }

    /*
     * The SSLContext implementation for TLS11 algorithm
     *
     * @see SSLContext
     */
    public static final class TLS11Context extends AbstractTLSContext {
        private static final List<ProtocolVersion> clientDefaultProtocols;
        private static final List<CipherSuite> clientDefaultCipherSuites;

        static {
            clientDefaultProtocols = getAvailableProtocols(
                    new ProtocolVersion[] {
                ProtocolVersion.TLS11,
                ProtocolVersion.TLS10
            });

            clientDefaultCipherSuites = getApplicableEnabledCipherSuites(
                    clientDefaultProtocols, true);

        }

        @Override
        List<ProtocolVersion> getClientDefaultProtocolVersions() {
            return clientDefaultProtocols;
        }

        @Override
        List<CipherSuite> getClientDefaultCipherSuites() {
            return clientDefaultCipherSuites;
        }
    }

    /*
     * The SSLContext implementation for TLS12 algorithm
     *
     * @see SSLContext
     */
    public static final class TLS12Context extends AbstractTLSContext {
        private static final List<ProtocolVersion> clientDefaultProtocols;
        private static final List<CipherSuite> clientDefaultCipherSuites;

        static {
            clientDefaultProtocols = getAvailableProtocols(
                    new ProtocolVersion[] {
                ProtocolVersion.TLS12,
                ProtocolVersion.TLS11,
                ProtocolVersion.TLS10
            });

            clientDefaultCipherSuites = getApplicableEnabledCipherSuites(
                    clientDefaultProtocols, true);
        }

        @Override
        List<ProtocolVersion> getClientDefaultProtocolVersions() {
            return clientDefaultProtocols;
        }

        @Override
        List<CipherSuite> getClientDefaultCipherSuites() {
            return clientDefaultCipherSuites;
        }
    }

    /*
     * The SSLContext implementation for TLS1.3 algorithm
     *
     * @see SSLContext
     */
    public static final class TLS13Context extends AbstractTLSContext {
        private static final List<ProtocolVersion> clientDefaultProtocols;
        private static final List<CipherSuite> clientDefaultCipherSuites;

        static {
            clientDefaultProtocols = getAvailableProtocols(
                    new ProtocolVersion[] {
                ProtocolVersion.TLS13,
                ProtocolVersion.TLS12,
                ProtocolVersion.TLS11,
                ProtocolVersion.TLS10
            });

            clientDefaultCipherSuites = getApplicableEnabledCipherSuites(
                    clientDefaultProtocols, true);
        }

        @Override
        List<ProtocolVersion> getClientDefaultProtocolVersions() {
            return clientDefaultProtocols;
        }

        @Override
        List<CipherSuite> getClientDefaultCipherSuites() {
            return clientDefaultCipherSuites;
        }
    }

    /*
     * The interface for the customized SSL/(D)TLS SSLContext.
     *
     * @see SSLContext
     */
    private static class CustomizedSSLProtocols {
        private static final String JDK_TLS_CLIENT_PROTOCOLS =
                "jdk.tls.client.protocols";
        private static final String JDK_TLS_SERVER_PROTOCOLS =
                "jdk.tls.server.protocols";
        static IllegalArgumentException reservedException = null;
        static final ArrayList<ProtocolVersion> customizedClientProtocols =
                new ArrayList<>();
        static final ArrayList<ProtocolVersion> customizedServerProtocols =
                new ArrayList<>();

        // Don't want a java.lang.LinkageError for illegal system property.
        //
        // Please don't throw exception in this static block.  Otherwise,
        // java.lang.LinkageError may be thrown during the instantiation of
        // the provider service. Instead, please handle the initialization
        // exception in the caller's constructor.
        static {
            populate(JDK_TLS_CLIENT_PROTOCOLS, customizedClientProtocols);
            populate(JDK_TLS_SERVER_PROTOCOLS, customizedServerProtocols);
        }

        private static void populate(String propname,
                ArrayList<ProtocolVersion> arrayList) {
            String property = GetPropertyAction.privilegedGetProperty(propname);
            if (property == null) {
                return;
            }

            if (!property.isEmpty()) {
                // remove double quote marks from beginning/end of the property
                if (property.length() > 1 && property.charAt(0) == '"' &&
                        property.charAt(property.length() - 1) == '"') {
                    property = property.substring(1, property.length() - 1);
                }
            }

            if (!property.isEmpty()) {
                String[] protocols = property.split(",");
                for (int i = 0; i < protocols.length; i++) {
                    protocols[i] = protocols[i].trim();
                    // Is it a supported protocol name?
                    ProtocolVersion pv =
                            ProtocolVersion.nameOf(protocols[i]);
                    if (pv == null) {
                        reservedException = new IllegalArgumentException(
                            propname + ": " + protocols[i] +
                            " is not a supported SSL protocol name");
                    }

                    // ignore duplicated protocols
                    if (!arrayList.contains(pv)) {
                        arrayList.add(pv);
                    }
                }
            }
        }
    }

    /*
     * The SSLContext implementation for customized TLS protocols
     *
     * @see SSLContext
     */
    private static class CustomizedTLSContext extends AbstractTLSContext {

        private static final List<ProtocolVersion> clientDefaultProtocols;
        private static final List<ProtocolVersion> serverDefaultProtocols;
        private static final List<CipherSuite> clientDefaultCipherSuites;
        private static final List<CipherSuite> serverDefaultCipherSuites;
        private static final IllegalArgumentException reservedException;

        // Don't want a java.lang.LinkageError for illegal system property.
        //
        // Please don't throw exception in this static block.  Otherwise,
        // java.lang.LinkageError may be thrown during the instantiation of
        // the provider service. Instead, let's handle the initialization
        // exception in constructor.
        static {
            reservedException = CustomizedSSLProtocols.reservedException;
            if (reservedException == null) {
                clientDefaultProtocols = customizedProtocols(true,
                        CustomizedSSLProtocols.customizedClientProtocols);
                serverDefaultProtocols = customizedProtocols(false,
                        CustomizedSSLProtocols.customizedServerProtocols);

                clientDefaultCipherSuites =
                        getApplicableEnabledCipherSuites(
                                clientDefaultProtocols, true);
                serverDefaultCipherSuites =
                        getApplicableEnabledCipherSuites(
                                serverDefaultProtocols, false);

            } else {
                // unlikely to be used
                clientDefaultProtocols = null;
                serverDefaultProtocols = null;
                clientDefaultCipherSuites = null;
                serverDefaultCipherSuites = null;
            }
        }

        private static List<ProtocolVersion> customizedProtocols(
                boolean client, List<ProtocolVersion> customized) {
            List<ProtocolVersion> refactored = new ArrayList<>();
            for (ProtocolVersion pv : customized) {
                if (!pv.isDTLS) {
                    refactored.add(pv);
                }
            }

            // Use the default enabled protocols if no customization
            ProtocolVersion[] candidates;
            if (refactored.isEmpty()) {
                // Client and server use the same default protocols.
                candidates = new ProtocolVersion[] {
                        ProtocolVersion.TLS13,
                        ProtocolVersion.TLS12,
                        ProtocolVersion.TLS11,
                        ProtocolVersion.TLS10
                    };
            } else {
                // Use the customized TLS protocols.
                candidates =
                    refactored.toArray(new ProtocolVersion[0]);
            }

            return getAvailableProtocols(candidates);
        }

        protected CustomizedTLSContext() {
            if (reservedException != null) {
                throw reservedException;
            }
        }

        @Override
        List<ProtocolVersion> getClientDefaultProtocolVersions() {
            return clientDefaultProtocols;
        }

        @Override
        List<ProtocolVersion> getServerDefaultProtocolVersions() {
            return serverDefaultProtocols;
        }

        @Override
        List<CipherSuite> getClientDefaultCipherSuites() {
            return clientDefaultCipherSuites;
        }

        @Override
        List<CipherSuite> getServerDefaultCipherSuites() {
            return serverDefaultCipherSuites;
        }
    }

    /*
     * The SSLContext implementation for default "TLS" algorithm
     *
     * @see SSLContext
     */
    public static final class TLSContext extends CustomizedTLSContext {
        // use the default constructor and methods
    }

    // lazy initialization holder class idiom for static default parameters
    //
    // See Effective Java Second Edition: Item 71.
    private static final class DefaultManagersHolder {
        private static final String NONE = "NONE";
        private static final String P11KEYSTORE = "PKCS11";

        private static final TrustManager[] trustManagers;
        private static final KeyManager[] keyManagers;

        private static final Exception reservedException;

        static {
            Exception reserved = null;
            TrustManager[] tmMediator = null;
            try {
                tmMediator = getTrustManagers();
            } catch (Exception e) {
                reserved = e;
                if (SSLLogger.isOn && SSLLogger.isOn("ssl,defaultctx")) {
                    SSLLogger.warning(
                            "Failed to load default trust managers", e);
                }
            }

            KeyManager[] kmMediator = null;
            if (reserved == null) {
                try {
                    kmMediator = getKeyManagers();
                } catch (Exception e) {
                    reserved = e;
                    if (SSLLogger.isOn && SSLLogger.isOn("ssl,defaultctx")) {
                        SSLLogger.warning(
                                "Failed to load default key managers", e);
                    }
                }
            }

            if (reserved != null) {
                trustManagers = new TrustManager[0];
                keyManagers = new KeyManager[0];

                // Important note: please don't reserve the original exception
                // object, which may be not garbage collection friendly as
                // 'reservedException' is a static filed.
                reservedException =
                        new KeyManagementException(reserved.getMessage());
            } else {
                trustManagers = tmMediator;
                keyManagers = kmMediator;

                reservedException = null;
            }
        }

        private static TrustManager[] getTrustManagers() throws Exception {
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(
                    TrustManagerFactory.getDefaultAlgorithm());
            if ("SunJSSE".equals(tmf.getProvider().getName())) {
                // The implementation will load the default KeyStore
                // automatically.  Cached trust materials may be used
                // for performance improvement.
                tmf.init((KeyStore)null);
            } else {
                // Use the explicitly specified KeyStore for third party's
                // TrustManagerFactory implementation.
                KeyStore ks = TrustStoreManager.getTrustedKeyStore();
                tmf.init(ks);
            }

            return tmf.getTrustManagers();
        }

        @SuppressWarnings("removal")
        private static KeyManager[] getKeyManagers() throws Exception {

            final Map<String,String> props = new HashMap<>();
            AccessController.doPrivileged(
                        new PrivilegedExceptionAction<Object>() {
                @Override
                public Object run() throws Exception {
                    props.put("keyStore",  System.getProperty(
                                "javax.net.ssl.keyStore", ""));
                    props.put("keyStoreType", System.getProperty(
                                "javax.net.ssl.keyStoreType",
                                KeyStore.getDefaultType()));
                    props.put("keyStoreProvider", System.getProperty(
                                "javax.net.ssl.keyStoreProvider", ""));
                    props.put("keyStorePasswd", System.getProperty(
                                "javax.net.ssl.keyStorePassword", ""));
                    return null;
                }
            });

            final String defaultKeyStore = props.get("keyStore");
            String defaultKeyStoreType = props.get("keyStoreType");
            String defaultKeyStoreProvider = props.get("keyStoreProvider");
            if (SSLLogger.isOn && SSLLogger.isOn("ssl,defaultctx")) {
                SSLLogger.fine("keyStore is : " + defaultKeyStore);
                SSLLogger.fine("keyStore type is : " +
                                        defaultKeyStoreType);
                SSLLogger.fine("keyStore provider is : " +
                                        defaultKeyStoreProvider);
            }

            if (P11KEYSTORE.equals(defaultKeyStoreType) &&
                    !NONE.equals(defaultKeyStore)) {
                throw new IllegalArgumentException("if keyStoreType is "
                    + P11KEYSTORE + ", then keyStore must be " + NONE);
            }

            FileInputStream fs = null;
            KeyStore ks = null;
            char[] passwd = null;
            try {
                if (!defaultKeyStore.isEmpty() &&
                        !NONE.equals(defaultKeyStore)) {
                    fs = AccessController.doPrivileged(
                            new PrivilegedExceptionAction<FileInputStream>() {
                        @Override
                        public FileInputStream run() throws Exception {
                            return new FileInputStream(defaultKeyStore);
                        }
                    });
                }

                String defaultKeyStorePassword = props.get("keyStorePasswd");
                if (!defaultKeyStorePassword.isEmpty()) {
                    passwd = defaultKeyStorePassword.toCharArray();
                }

                // Try to initialize key store.
                if ((defaultKeyStoreType.length()) != 0) {
                    if (SSLLogger.isOn && SSLLogger.isOn("ssl,defaultctx")) {
                        SSLLogger.finest("init keystore");
                    }
                    if (defaultKeyStoreProvider.isEmpty()) {
                        ks = KeyStore.getInstance(defaultKeyStoreType);
                    } else {
                        ks = KeyStore.getInstance(defaultKeyStoreType,
                                            defaultKeyStoreProvider);
                    }

                    // if defaultKeyStore is NONE, fs will be null
                    ks.load(fs, passwd);
                }
            } finally {
                if (fs != null) {
                    fs.close();
                    fs = null;
                }
            }

            /*
             * Try to initialize key manager.
             */
            if (SSLLogger.isOn && SSLLogger.isOn("ssl,defaultctx")) {
                SSLLogger.fine("init keymanager of type " +
                    KeyManagerFactory.getDefaultAlgorithm());
            }
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(
                KeyManagerFactory.getDefaultAlgorithm());

            if (P11KEYSTORE.equals(defaultKeyStoreType)) {
                kmf.init(ks, null); // do not pass key passwd if using token
            } else {
                kmf.init(ks, passwd);
            }

            return kmf.getKeyManagers();
        }
    }

    // lazy initialization holder class idiom for static default parameters
    //
    // See Effective Java Second Edition: Item 71.
    private static final class DefaultSSLContextHolder {

        private static final SSLContextImpl sslContext;
        private static final Exception reservedException;

        static {
            Exception reserved = null;
            SSLContextImpl mediator = null;
            if (DefaultManagersHolder.reservedException != null) {
                reserved = DefaultManagersHolder.reservedException;
            } else {
                try {
                    mediator = new DefaultSSLContext();
                } catch (Exception e) {
                    // Important note: please don't reserve the original
                    // exception object, which may be not garbage collection
                    // friendly as 'reservedException' is a static filed.
                    reserved = new KeyManagementException(e.getMessage());
                    if (SSLLogger.isOn && SSLLogger.isOn("ssl,defaultctx")) {
                        SSLLogger.warning(
                                "Failed to load default SSLContext", e);
                    }
                }
            }

            sslContext = mediator;
            reservedException = reserved;
        }
    }

    /*
     * The SSLContext implementation for default "Default" algorithm
     *
     * @see SSLContext
     */
    public static final class DefaultSSLContext extends CustomizedTLSContext {

        // public constructor for SSLContext.getInstance("Default")
        public DefaultSSLContext() throws Exception {
            if (DefaultManagersHolder.reservedException != null) {
                throw DefaultManagersHolder.reservedException;
            }

            try {
                super.engineInit(DefaultManagersHolder.keyManagers,
                        DefaultManagersHolder.trustManagers, null);
            } catch (Exception e) {
                if (SSLLogger.isOn && SSLLogger.isOn("ssl,defaultctx")) {
                    SSLLogger.fine("default context init failed: ", e);
                }
                throw e;
            }
        }

        @Override
        protected void engineInit(KeyManager[] km, TrustManager[] tm,
            SecureRandom sr) throws KeyManagementException {
            throw new KeyManagementException
                ("Default SSLContext is initialized automatically");
        }

        static SSLContextImpl getDefaultImpl() throws Exception {
            if (DefaultSSLContextHolder.reservedException != null) {
                throw DefaultSSLContextHolder.reservedException;
            }

            return DefaultSSLContextHolder.sslContext;
        }
    }

    /*
     * The base abstract SSLContext implementation for the Datagram Transport
     * Layer Security (DTLS) protocols.
     *
     * This abstract class encapsulates supported and the default server DTLS
     * parameters.
     *
     * @see SSLContext
     */
    private abstract static class AbstractDTLSContext extends SSLContextImpl {
        private static final List<ProtocolVersion> supportedProtocols;
        private static final List<ProtocolVersion> serverDefaultProtocols;

        private static final List<CipherSuite> supportedCipherSuites;
        private static final List<CipherSuite> serverDefaultCipherSuites;

        static {
            supportedProtocols = Arrays.asList(
                ProtocolVersion.DTLS12,
                ProtocolVersion.DTLS10
            );

            // available protocols for server mode
            serverDefaultProtocols = getAvailableProtocols(
                    new ProtocolVersion[] {
                ProtocolVersion.DTLS12,
                ProtocolVersion.DTLS10
            });

            supportedCipherSuites = getApplicableSupportedCipherSuites(
                    supportedProtocols);
            serverDefaultCipherSuites = getApplicableEnabledCipherSuites(
                    serverDefaultProtocols, false);
        }

        @Override
        protected SSLParameters engineGetDefaultSSLParameters() {
            SSLEngine engine = createSSLEngineImpl();
            // Note: The TLSContext defaults to client side SSLParameters.
            // We can do the same here. Please don't change the behavior
            // for compatibility.
            engine.setUseClientMode(true);
            return engine.getSSLParameters();
        }

        @Override
        protected SSLParameters engineGetSupportedSSLParameters() {
            SSLEngine engine = createSSLEngineImpl();
            SSLParameters params = new SSLParameters();
            params.setCipherSuites(engine.getSupportedCipherSuites());
            params.setProtocols(engine.getSupportedProtocols());
            return params;
        }

        @Override
        List<ProtocolVersion> getSupportedProtocolVersions() {
            return supportedProtocols;
        }

        @Override
        List<CipherSuite> getSupportedCipherSuites() {
            return supportedCipherSuites;
        }

        @Override
        List<ProtocolVersion> getServerDefaultProtocolVersions() {
            return serverDefaultProtocols;
        }

        @Override
        List<CipherSuite> getServerDefaultCipherSuites() {
            return serverDefaultCipherSuites;
        }

        @Override
        SSLEngine createSSLEngineImpl() {
            return new SSLEngineImpl(this);
        }

        @Override
        SSLEngine createSSLEngineImpl(String host, int port) {
            return new SSLEngineImpl(this, host, port);
        }

        @Override
        boolean isDTLS() {
            return true;
        }
    }

    /*
     * The SSLContext implementation for DTLSv1.0 algorithm.
     *
     * @see SSLContext
     */
    public static final class DTLS10Context extends AbstractDTLSContext {
        private static final List<ProtocolVersion> clientDefaultProtocols;
        private static final List<CipherSuite> clientDefaultCipherSuites;

        static {
            // available protocols for client mode
            clientDefaultProtocols = getAvailableProtocols(
                    new ProtocolVersion[] {
                ProtocolVersion.DTLS10
            });

            clientDefaultCipherSuites = getApplicableEnabledCipherSuites(
                    clientDefaultProtocols, true);
        }

        @Override
        List<ProtocolVersion> getClientDefaultProtocolVersions() {
            return clientDefaultProtocols;
        }

        @Override
        List<CipherSuite> getClientDefaultCipherSuites() {
            return clientDefaultCipherSuites;
        }
    }

    /*
     * The SSLContext implementation for DTLSv1.2 algorithm.
     *
     * @see SSLContext
     */
    public static final class DTLS12Context extends AbstractDTLSContext {
        private static final List<ProtocolVersion> clientDefaultProtocols;
        private static final List<CipherSuite> clientDefaultCipherSuites;

        static {
            // available protocols for client mode
            clientDefaultProtocols = getAvailableProtocols(
                    new ProtocolVersion[] {
                ProtocolVersion.DTLS12,
                ProtocolVersion.DTLS10
            });

            clientDefaultCipherSuites = getApplicableEnabledCipherSuites(
                    clientDefaultProtocols, true);
        }

        @Override
        List<ProtocolVersion> getClientDefaultProtocolVersions() {
            return clientDefaultProtocols;
        }

        @Override
        List<CipherSuite> getClientDefaultCipherSuites() {
            return clientDefaultCipherSuites;
        }
    }

    /*
     * The SSLContext implementation for customized TLS protocols
     *
     * @see SSLContext
     */
    private static class CustomizedDTLSContext extends AbstractDTLSContext {
        private static final List<ProtocolVersion> clientDefaultProtocols;
        private static final List<ProtocolVersion> serverDefaultProtocols;
        private static final List<CipherSuite> clientDefaultCipherSuites;
        private static final List<CipherSuite> serverDefaultCipherSuites;

        private static IllegalArgumentException reservedException;

        // Don't want a java.lang.LinkageError for illegal system property.
        //
        // Please don't throw exception in this static block.  Otherwise,
        // java.lang.LinkageError may be thrown during the instantiation of
        // the provider service. Instead, let's handle the initialization
        // exception in constructor.
        static {
            reservedException = CustomizedSSLProtocols.reservedException;
            if (reservedException == null) {
                clientDefaultProtocols = customizedProtocols(true,
                        CustomizedSSLProtocols.customizedClientProtocols);
                serverDefaultProtocols = customizedProtocols(false,
                        CustomizedSSLProtocols.customizedServerProtocols);

                clientDefaultCipherSuites =
                        getApplicableEnabledCipherSuites(
                                clientDefaultProtocols, true);
                serverDefaultCipherSuites =
                        getApplicableEnabledCipherSuites(
                                serverDefaultProtocols, false);

            } else {
                // unlikely to be used
                clientDefaultProtocols = null;
                serverDefaultProtocols = null;
                clientDefaultCipherSuites = null;
                serverDefaultCipherSuites = null;
            }
        }

        private static List<ProtocolVersion> customizedProtocols(boolean client,
                List<ProtocolVersion> customized) {
            List<ProtocolVersion> refactored = new ArrayList<>();
            for (ProtocolVersion pv : customized) {
                if (pv.isDTLS) {
                    refactored.add(pv);
                }
            }

            ProtocolVersion[] candidates;
            // Use the default enabled protocols if no customization
            if (refactored.isEmpty()) {
                candidates = new ProtocolVersion[]{
                        ProtocolVersion.DTLS12,
                        ProtocolVersion.DTLS10
                };
                if (!client)
                    return Arrays.asList(candidates);
            } else {
                // Use the customized TLS protocols.
                candidates =
                        new ProtocolVersion[customized.size()];
                candidates = customized.toArray(candidates);
            }

            return getAvailableProtocols(candidates);
        }

        protected CustomizedDTLSContext() {
            if (reservedException != null) {
                throw reservedException;
            }
        }

        @Override
        List<ProtocolVersion> getClientDefaultProtocolVersions() {
            return clientDefaultProtocols;
        }

        @Override
        List<ProtocolVersion> getServerDefaultProtocolVersions() {
            return serverDefaultProtocols;
        }

        @Override
        List<CipherSuite> getClientDefaultCipherSuites() {
            return clientDefaultCipherSuites;
        }

        @Override
        List<CipherSuite> getServerDefaultCipherSuites() {
            return serverDefaultCipherSuites;
        }
    }

    /*
     * The SSLContext implementation for default "DTLS" algorithm
     *
     * @see SSLContext
     */
    public static final class DTLSContext extends CustomizedDTLSContext {
        // use the default constructor and methods
    }

}

final class AbstractTrustManagerWrapper extends X509ExtendedTrustManager
            implements X509TrustManager {

    // the delegated trust manager
    private final X509TrustManager tm;

    AbstractTrustManagerWrapper(X509TrustManager tm) {
        this.tm = tm;
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType)
        throws CertificateException {
        tm.checkClientTrusted(chain, authType);
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType)
        throws CertificateException {
        tm.checkServerTrusted(chain, authType);
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return tm.getAcceptedIssuers();
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType,
                Socket socket) throws CertificateException {
        tm.checkClientTrusted(chain, authType);
        checkAdditionalTrust(chain, authType, socket, true);
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType,
            Socket socket) throws CertificateException {
        tm.checkServerTrusted(chain, authType);
        checkAdditionalTrust(chain, authType, socket, false);
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType,
            SSLEngine engine) throws CertificateException {
        tm.checkClientTrusted(chain, authType);
        checkAdditionalTrust(chain, authType, engine, true);
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType,
            SSLEngine engine) throws CertificateException {
        tm.checkServerTrusted(chain, authType);
        checkAdditionalTrust(chain, authType, engine, false);
    }

    private void checkAdditionalTrust(X509Certificate[] chain,
            String authType, Socket socket,
            boolean checkClientTrusted) throws CertificateException {
        if (socket != null && socket.isConnected() &&
                                    socket instanceof SSLSocket) {

            SSLSocket sslSocket = (SSLSocket)socket;
            SSLSession session = sslSocket.getHandshakeSession();
            if (session == null) {
                throw new CertificateException("No handshake session");
            }

            // check endpoint identity
            String identityAlg = sslSocket.getSSLParameters().
                                        getEndpointIdentificationAlgorithm();
            if (identityAlg != null && !identityAlg.isEmpty()) {
                X509TrustManagerImpl.checkIdentity(session, chain,
                                    identityAlg, checkClientTrusted);
            }

            // try the best to check the algorithm constraints
            AlgorithmConstraints constraints;
            if (ProtocolVersion.useTLS12PlusSpec(session.getProtocol())) {
                if (session instanceof ExtendedSSLSession) {
                    ExtendedSSLSession extSession =
                                    (ExtendedSSLSession)session;
                    String[] peerSupportedSignAlgs =
                            extSession.getLocalSupportedSignatureAlgorithms();

                    constraints = new SSLAlgorithmConstraints(
                                    sslSocket, peerSupportedSignAlgs, true);
                } else {
                    constraints =
                            new SSLAlgorithmConstraints(sslSocket, true);
                }
            } else {
                constraints = new SSLAlgorithmConstraints(sslSocket, true);
            }

            checkAlgorithmConstraints(chain, constraints, checkClientTrusted);
        }
    }

    private void checkAdditionalTrust(X509Certificate[] chain,
            String authType, SSLEngine engine,
            boolean checkClientTrusted) throws CertificateException {
        if (engine != null) {
            SSLSession session = engine.getHandshakeSession();
            if (session == null) {
                throw new CertificateException("No handshake session");
            }

            // check endpoint identity
            String identityAlg = engine.getSSLParameters().
                                        getEndpointIdentificationAlgorithm();
            if (identityAlg != null && !identityAlg.isEmpty()) {
                X509TrustManagerImpl.checkIdentity(session, chain,
                                    identityAlg, checkClientTrusted);
            }

            // try the best to check the algorithm constraints
            AlgorithmConstraints constraints;
            if (ProtocolVersion.useTLS12PlusSpec(session.getProtocol())) {
                if (session instanceof ExtendedSSLSession) {
                    ExtendedSSLSession extSession =
                                    (ExtendedSSLSession)session;
                    String[] peerSupportedSignAlgs =
                            extSession.getLocalSupportedSignatureAlgorithms();

                    constraints = new SSLAlgorithmConstraints(
                                    engine, peerSupportedSignAlgs, true);
                } else {
                    constraints =
                            new SSLAlgorithmConstraints(engine, true);
                }
            } else {
                constraints = new SSLAlgorithmConstraints(engine, true);
            }

            checkAlgorithmConstraints(chain, constraints, checkClientTrusted);
        }
    }

    private void checkAlgorithmConstraints(X509Certificate[] chain,
            AlgorithmConstraints constraints,
            boolean checkClientTrusted) throws CertificateException {
        try {
            // Does the certificate chain end with a trusted certificate?
            int checkedLength = chain.length - 1;

            Collection<X509Certificate> trustedCerts = new HashSet<>();
            X509Certificate[] certs = tm.getAcceptedIssuers();
            if ((certs != null) && (certs.length > 0)){
                Collections.addAll(trustedCerts, certs);
            }

            if (trustedCerts.contains(chain[checkedLength])) {
                    checkedLength--;
            }

            // A forward checker, need to check from trust to target
            if (checkedLength >= 0) {
                AlgorithmChecker checker =
                    new AlgorithmChecker(constraints,
                            (checkClientTrusted ? Validator.VAR_TLS_CLIENT :
                                        Validator.VAR_TLS_SERVER));
                checker.init(false);
                for (int i = checkedLength; i >= 0; i--) {
                    X509Certificate cert = chain[i];
                    // We don't care about the unresolved critical extensions.
                    checker.check(cert, Collections.<String>emptySet());
                }
            }
        } catch (CertPathValidatorException cpve) {
            throw new CertificateException(
                "Certificates do not conform to algorithm constraints", cpve);
        }
    }
}

// Dummy X509TrustManager implementation, rejects all peer certificates.
// Used if the application did not specify a proper X509TrustManager.
final class DummyX509TrustManager extends X509ExtendedTrustManager
            implements X509TrustManager {

    static final X509TrustManager INSTANCE = new DummyX509TrustManager();

    private DummyX509TrustManager() {
        // empty
    }

    /*
     * Given the partial or complete certificate chain
     * provided by the peer, build a certificate path
     * to a trusted root and return if it can be
     * validated and is trusted for client SSL authentication.
     * If not, it throws an exception.
     */
    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType)
        throws CertificateException {
        throw new CertificateException(
            "No X509TrustManager implementation avaiable");
    }

    /*
     * Given the partial or complete certificate chain
     * provided by the peer, build a certificate path
     * to a trusted root and return if it can be
     * validated and is trusted for server SSL authentication.
     * If not, it throws an exception.
     */
    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType)
        throws CertificateException {
        throw new CertificateException(
            "No X509TrustManager implementation available");
    }

    /*
     * Return an array of issuer certificates which are trusted
     * for authenticating peers.
     */
    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return new X509Certificate[0];
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType,
                Socket socket) throws CertificateException {
        throw new CertificateException(
            "No X509TrustManager implementation available");
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType,
            Socket socket) throws CertificateException {
        throw new CertificateException(
            "No X509TrustManager implementation available");
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType,
            SSLEngine engine) throws CertificateException {
        throw new CertificateException(
            "No X509TrustManager implementation available");
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType,
            SSLEngine engine) throws CertificateException {
        throw new CertificateException(
            "No X509TrustManager implementation available");
    }
}

/*
 * A wrapper class to turn a X509KeyManager into an X509ExtendedKeyManager
 */
final class AbstractKeyManagerWrapper extends X509ExtendedKeyManager {

    private final X509KeyManager km;

    AbstractKeyManagerWrapper(X509KeyManager km) {
        this.km = km;
    }

    @Override
    public String[] getClientAliases(String keyType, Principal[] issuers) {
        return km.getClientAliases(keyType, issuers);
    }

    @Override
    public String chooseClientAlias(String[] keyType, Principal[] issuers,
            Socket socket) {
        return km.chooseClientAlias(keyType, issuers, socket);
    }

    @Override
    public String[] getServerAliases(String keyType, Principal[] issuers) {
        return km.getServerAliases(keyType, issuers);
    }

    @Override
    public String chooseServerAlias(String keyType, Principal[] issuers,
            Socket socket) {
        return km.chooseServerAlias(keyType, issuers, socket);
    }

    @Override
    public X509Certificate[] getCertificateChain(String alias) {
        return km.getCertificateChain(alias);
    }

    @Override
    public PrivateKey getPrivateKey(String alias) {
        return km.getPrivateKey(alias);
    }

    // Inherit chooseEngineClientAlias() and chooseEngineServerAlias() from
    // X509ExtendedKeymanager. It defines them to return null;
}


// Dummy X509KeyManager implementation, never returns any certificates/keys.
// Used if the application did not specify a proper X509TrustManager.
final class DummyX509KeyManager extends X509ExtendedKeyManager {

    static final X509ExtendedKeyManager INSTANCE = new DummyX509KeyManager();

    private DummyX509KeyManager() {
        // empty
    }

    /*
     * Get the matching aliases for authenticating the client side of a secure
     * socket given the public key type and the list of
     * certificate issuer authorities recognized by the peer (if any).
     */
    @Override
    public String[] getClientAliases(String keyType, Principal[] issuers) {
        return null;
    }

    /*
     * Choose an alias to authenticate the client side of a secure
     * socket given the public key type and the list of
     * certificate issuer authorities recognized by the peer (if any).
     */
    @Override
    public String chooseClientAlias(String[] keyTypes, Principal[] issuers,
            Socket socket) {
        return null;
    }

    /*
     * Choose an alias to authenticate the client side of an
     * engine given the public key type and the list of
     * certificate issuer authorities recognized by the peer (if any).
     */
    @Override
    public String chooseEngineClientAlias(
            String[] keyTypes, Principal[] issuers, SSLEngine engine) {
        return null;
    }

    /*
     * Get the matching aliases for authenticating the server side of a secure
     * socket given the public key type and the list of
     * certificate issuer authorities recognized by the peer (if any).
     */
    @Override
    public String[] getServerAliases(String keyType, Principal[] issuers) {
        return null;
    }

    /*
     * Choose an alias to authenticate the server side of a secure
     * socket given the public key type and the list of
     * certificate issuer authorities recognized by the peer (if any).
     */
    @Override
    public String chooseServerAlias(String keyType, Principal[] issuers,
            Socket socket) {
        return null;
    }

    /*
     * Choose an alias to authenticate the server side of an engine
     * given the public key type and the list of
     * certificate issuer authorities recognized by the peer (if any).
     */
    @Override
    public String chooseEngineServerAlias(
            String keyType, Principal[] issuers, SSLEngine engine) {
        return null;
    }

    /**
     * Returns the certificate chain associated with the given alias.
     *
     * @param alias the alias name
     *
     * @return the certificate chain (ordered with the user's certificate first
     * and the root certificate authority last)
     */
    @Override
    public X509Certificate[] getCertificateChain(String alias) {
        return null;
    }

    /*
     * Returns the key associated with the given alias, using the given
     * password to recover it.
     *
     * @param alias the alias name
     *
     * @return the requested key
     */
    @Override
    public PrivateKey getPrivateKey(String alias) {
        return null;
    }
}
