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

/*
 * SunJSSE does not support dynamic system properties, no way to re-use
 * system properties in samevm/agentvm mode.
 * For extra debugging output, add -Djavax.net.debug=ssl:handshake into the
 * run directive below.
 */

/*
 * @test
 * @bug 8166596
 * @summary TLS support for the EdDSA signature algorithm
 * @library /javax/net/ssl/templates /test/lib
 * @run main/othervm TLSWithEdDSA
 */

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.Charset;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.PKIXBuilderParameters;
import java.security.cert.X509CertSelector;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECKey;
import java.security.interfaces.EdECKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.*;
import javax.net.ssl.CertPathTrustManagerParameters;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509ExtendedKeyManager;
import javax.net.ssl.X509KeyManager;
import jdk.test.lib.security.SecurityUtils;

public class TLSWithEdDSA extends SSLSocketTemplate {
    private static final String PASSWD = "passphrase";
    private static final String DEF_TRUST_ANCHORS = "CA_DSA_1024:CA_DSA_2048:" +
            "CA_ECDSA_SECP256R1:CA_ECDSA_SECP384R1:CA_ECDSA_SECP521R1:" +
            "CA_ED25519:CA_ED448:CA_RSA_2048";
    private static final String DEF_ALL_EE = "EE_ECDSA_SECP256R1:" +
            "EE_ECDSA_SECP384R1:EE_ECDSA_SECP521R1:EE_RSA_2048:" +
            "EE_EC_RSA_SECP256R1:EE_DSA_2048:EE_DSA_1024:EE_ED25519:EE_ED448";
    private static final List<String> TEST_PROTOS = List.of(
            "TLSv1.3", "TLSv1.2", "TLSv1.1", "TLSv1");

    private static CertificateFactory certFac;
    private static final Map<ParamType, String> clientParameters =
            new HashMap<>();
    private static final Map<ParamType, String> serverParameters =
            new HashMap<>();

    private final SessionChecker clientChecker;
    private final SessionChecker serverChecker;
    private final Class<? extends Throwable> clientException;
    private final Class<? extends Throwable> serverException;

    interface SessionChecker {
        public void check(SSLSocket socket);
    }

    /**
     * Checks to make sure the end-entity certificate presented by the
     * peer uses and Ed25519 key.
     */
    final static SessionChecker isPeerEd25519 = new SessionChecker() {
        @Override
        public void check(SSLSocket sock) {
            try {
                SSLSession session = sock.getSession();
                System.out.println("Peer certificate check for Ed25519:\n" +
                        sessionDump(session));
                Certificate[] serverCertChain = session.getPeerCertificates();
                X509Certificate tlsCert = (X509Certificate)serverCertChain[0];
                keyCheck(tlsCert.getPublicKey(), "EdDSA", "Ed25519");
            } catch (SSLPeerUnverifiedException sslpe) {
                throw new RuntimeException(sslpe);
            }
        }
    };

    /**
     * Checks to make sure the end-entity certificate presented by the
     * peer uses and Ed448 key.
     */
    final static SessionChecker isPeerEd448 = new SessionChecker() {
        @Override
        public void check(SSLSocket sock) {
            try {
                SSLSession session = sock.getSession();
                System.out.println("Peer certificate check for Ed448:\n" +
                        sessionDump(session));
                Certificate[] serverCertChain = session.getPeerCertificates();
                X509Certificate tlsCert = (X509Certificate)serverCertChain[0];
                keyCheck(tlsCert.getPublicKey(), "EdDSA", "Ed448");
            } catch (SSLPeerUnverifiedException sslpe) {
                throw new RuntimeException(sslpe);
            }
        }
    };

    /**
     * Checks to make sure the end-entity certificate presented by the
     * peer uses an EC secp521r1 key.
     */
    final static SessionChecker isPeerP521 = new SessionChecker() {
        @Override
        public void check(SSLSocket sock) {
            try {
                SSLSession session = sock.getSession();
                System.out.println("Peer certificate check for secp521r1:\n" +
                        sessionDump(session));
                Certificate[] serverCertChain = session.getPeerCertificates();
                X509Certificate tlsCert = (X509Certificate)serverCertChain[0];
                keyCheck(tlsCert.getPublicKey(), "EC", "secp521r1");
            } catch (SSLPeerUnverifiedException sslpe) {
                throw new RuntimeException(sslpe);
            }
        }
    };

    /**
     * Returns a String summary of an SSLSession object
     *
     * @param sess the SSLSession object to be dumped
     *
     * @return a String representation of the test-relevant portions of the
     *      SSLSession object.
     */
    private static String sessionDump(SSLSession sess) {
        StringBuilder sb = new StringBuilder();
        sb.append("----- Session Info -----\n");
        sb.append("Protocol: ").append(sess.getProtocol()).append("\n");
        sb.append("Cipher Suite: ").append(sess.getCipherSuite());
        Certificate[] localCerts = sess.getLocalCertificates();
        if (localCerts != null) {
            sb.append("\nLocal Certs:");
            int i = 0;
            for (Certificate cert : localCerts) {
                sb.append(String.format("\n   [%d]: %s", i++,
                        ((X509Certificate)cert).getSubjectX500Principal()));
            }
        }
        try {
            Certificate[] peerCerts = sess.getPeerCertificates();
            if (peerCerts != null) {
                sb.append("\nPeer Certs:");
                int i = 0;
                for (Certificate cert : peerCerts) {
                    sb.append(String.format("\n   [%d]: %s", i++,
                            ((X509Certificate)cert).getSubjectX500Principal()));
                }
            }
        } catch (SSLPeerUnverifiedException sslex) {
            throw new RuntimeException(sslex);
        }

        return sb.toString();
    }

    /**
     * Checks to make sure the public key conforms to the expected key type
     * and (where applicable) curve.
     *
     * @param pubKey the public key to be checked
     * @param expPkType the expected key type (RSA/DSA/EC/EdDSA)
     * @param expCurveName if an EC/EdDSA key, the expected curve
     */
    private static void keyCheck(PublicKey pubKey, String expPkType,
            String expCurveName) {
        String curveName = null;
        String pubKeyAlg = pubKey.getAlgorithm();
        if (!expPkType.equalsIgnoreCase(pubKeyAlg)) {
            throw new RuntimeException("Expected " + expPkType + " key, got " +
                    pubKeyAlg);
        }

        // Check the curve type
        if (expCurveName != null) {
            switch (pubKeyAlg) {
                case "EdDSA":
                    curveName = ((EdECKey)pubKey).getParams().getName().
                            toLowerCase();
                    if (!expCurveName.equalsIgnoreCase(curveName)) {
                        throw new RuntimeException("Expected " + expCurveName +
                                " curve, " + "got " + curveName);
                    }
                    break;
                case "EC":
                    curveName = ((ECKey)pubKey).getParams().toString().
                            toLowerCase();
                    if (!curveName.contains(expCurveName.toLowerCase())) {
                        throw new RuntimeException("Expected " + expCurveName +
                                " curve, " + "got " + curveName);
                    }
                    break;
                default:
                    throw new IllegalArgumentException(
                            "Unsupported key type: " + pubKeyAlg);
            }
        }
        System.out.format("Found key: %s / %s\n", pubKeyAlg,
                curveName != null ? curveName : "");
    }

    TLSWithEdDSA(SessionChecker cliChk, Class<? extends Throwable> cliExpExc,
            SessionChecker servChk, Class<? extends Throwable> servExpExc) {
        super();
        clientChecker = cliChk;
        clientException = cliExpExc;
        serverChecker = servChk;
        serverException = servExpExc;
    }

    /**
     * Creates an SSLContext for use with the client side of this test.  This
     * uses parameters held in the static client parameters map.
     *
     * @return an initialized SSLContext for use with the client.
     *
     * @throws Exception if any downstream errors occur during key store
     *      creation, key/trust manager factory creation or context
     *      initialization.
     */
    @Override
    protected SSLContext createClientSSLContext() throws Exception {
        KeyStore clientKeyStore = createKeyStore(
                clientParameters.getOrDefault(ParamType.KSENTRIES, ""),
                PASSWD.toCharArray());
        KeyStore clientTrustStore = createTrustStore(
                clientParameters.getOrDefault(ParamType.TSENTRIES,
                        DEF_TRUST_ANCHORS));
        return createCtxCommon(clientKeyStore,
                clientParameters.get(ParamType.CERTALIAS), PASSWD.toCharArray(),
                clientTrustStore, "jdk.tls.client.SignatureSchemes",
                clientParameters.get(ParamType.SIGALGS));
    }

    /**
     * Creates an SSLContext for use with the server side of this test.  This
     * uses parameters held in the static server parameters map.
     *
     * @return an initialized SSLContext for use with the server.
     *
     * @throws Exception if any downstream errors occur during key store
     *      creation, key/trust manager factory creation or context
     *      initialization.
     */
    @Override
    protected SSLContext createServerSSLContext() throws Exception {
        KeyStore serverKeyStore = createKeyStore(
                serverParameters.getOrDefault(ParamType.KSENTRIES, ""),
                PASSWD.toCharArray());
        KeyStore serverTrustStore = createTrustStore(
                serverParameters.getOrDefault(ParamType.TSENTRIES,
                        DEF_TRUST_ANCHORS));
        return createCtxCommon(serverKeyStore,
                serverParameters.get(ParamType.CERTALIAS), PASSWD.toCharArray(),
                serverTrustStore, "jdk.tls.server.SignatureSchemes",
                serverParameters.get(ParamType.SIGALGS));
    }

    /**
     * Create a trust store containing any CA certificates designated as
     * trust anchors.
     *
     * @return the trust store populated with the root CA certificate.
     *
     * @throws GeneralSecurityException if any certificates cannot be added to
     *      the key store.
     */
    private static KeyStore createTrustStore(String certEnumNames)
            throws GeneralSecurityException {
        KeyStore.Builder keyStoreBuilder =
                KeyStore.Builder.newInstance("PKCS12", null,
                        new KeyStore.PasswordProtection(PASSWD.toCharArray()));
        KeyStore ks = keyStoreBuilder.getKeyStore();
        for (String certName : certEnumNames.split(":")) {
            try {
                SSLSocketTemplate.Cert cert =
                        SSLSocketTemplate.Cert.valueOf(certName);
                ks.setCertificateEntry(certName, pem2Cert(cert.certStr));
            } catch (IllegalArgumentException iae) {
                System.out.println("Unable to find Cert enum entry for " +
                        certName + ", skipping");
            }
        }
        return ks;
    }

    /**
     * Create a key store containing any end-entity private keys/certs
     * specified in the parameters.
     *
     * @param certEnumNames a colon-delimited list of String values that are
     *      the names of the SSLSocketTemplate.Cert enumeration entries.
     * @param pass the desired password for the resulting KeyStore object.
     *
     * @return a populated, loaded KeyStore ready for use.
     *
     * @throws GeneralSecurityException if any issues occur while setting
     *      the private key or certificate entries.
     */
    private static KeyStore createKeyStore(String certEnumNames, char[] pass)
            throws GeneralSecurityException {
        KeyStore.Builder keyStoreBuilder =
                KeyStore.Builder.newInstance("PKCS12", null,
                        new KeyStore.PasswordProtection(pass));
        KeyStore ks = keyStoreBuilder.getKeyStore();
        if (certEnumNames != null && !certEnumNames.isEmpty()) {
            for (String certName : certEnumNames.split(":")) {
                try {
                    SSLSocketTemplate.Cert cert =
                            SSLSocketTemplate.Cert.valueOf(certName);
                    ks.setKeyEntry(certName,
                            pem2PrivKey(cert.privKeyStr, cert.keyAlgo), pass,
                            new Certificate[] { pem2Cert(cert.certStr) });
                } catch (IllegalArgumentException iae) {
                    System.out.println("Unable to find Cert enum entry for " +
                            certName + ", skipping");
                }
            }
        }

        return ks;
    }

    /**
     * Covert a PEM-encoded certificate into a X509Certificate object.
     *
     * @param certPem the PEM encoding for the certificate.
     *
     * @return the corresponding X509Certificate object for the provided PEM.
     *
     * @throws CertificateException if any decoding errors occur.
     */
    private static X509Certificate pem2Cert(String certPem)
            throws CertificateException {
        return (X509Certificate)certFac.generateCertificate(
                new ByteArrayInputStream(certPem.getBytes(
                        Charset.forName("UTF-8"))));
    }

    /**
     * Covert a PEM-encoded PKCS8 private key into a PrivateKey object.
     *
     * @param keyPem the PEM encoding for the certificate.
     * @param keyAlg the algorithm for the private key contained in the PKCS8
     *  `   encoding.
     *
     * @return the corresponding PrivateKey object for the provided PEM.
     *
     * @throws GeneralSecurityException if any decoding errors occur.
     */
    private static PrivateKey pem2PrivKey(String keyPem, String keyAlg)
            throws GeneralSecurityException {
        PKCS8EncodedKeySpec p8Spec = new PKCS8EncodedKeySpec(
                Base64.getMimeDecoder().decode(keyPem));
        KeyFactory keyFac = KeyFactory.getInstance(keyAlg);
        return keyFac.generatePrivate(p8Spec);
    }

    /**
     * Create an SSLContext for use with the client or server sides of this
     * test.
     *
     * @param keys the key store object for this SSLContext.
     * @param alias optional alias specifier to exclusively use that alias for
     *      TLS connections.
     * @param pass the key store password
     * @param trust the trust store object
     * @param sigAlgProp the signature algorithm property name to set
     *      (reserved for future use pending the fix for JDK-8255867)
     * @param sigAlgVal the property value to be applied.
     *
     * @return an initialized SSLContext object.
     *
     * @throws IOException if any IOExceptions during manager factory creation
     *      take place
     * @throws GeneralSecurityException any other failure during SSLContext
     *      creation/initialization
     */
    private static SSLContext createCtxCommon(KeyStore keys, String alias,
            char[] pass, KeyStore trust, String sigAlgProp, String sigAlgVal)
            throws IOException, GeneralSecurityException {
        SSLContext ctx;
        if (sigAlgVal != null && !sigAlgVal.isEmpty()) {
            System.setProperty(sigAlgProp, sigAlgVal);
        }

        // If an alias is specified use our local AliasKeyManager
        KeyManager[] kms = (alias != null && !alias.isEmpty()) ?
                new KeyManager[] { new AliasKeyManager(keys, pass, alias) } :
                createKeyManagerFactory(keys, pass).getKeyManagers();

        ctx = SSLContext.getInstance("TLS");
        ctx.init(kms, createTrustManagerFactory(trust).getTrustManagers(),
                null);
        return ctx;
    }

    /**
     * Creates a KeyManagerFactory for use during SSLContext initialization.
     *
     * @param ks the KeyStore forming the base of the KeyManagerFactory
     * @param passwd the password to use for the key store
     *
     * @return the initialized KeyManagerFactory
     *
     * @throws IOException any IOExceptions during key manager factory
     *      initialization.
     * @throws GeneralSecurityException if any failures during instantiation
     *      take place.
     */
    private static KeyManagerFactory createKeyManagerFactory(KeyStore ks,
            char[] passwd) throws IOException, GeneralSecurityException {
        KeyManagerFactory kmf;
        kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(ks, passwd);

        KeyManager[] kmgrs = kmf.getKeyManagers();
        X509ExtendedKeyManager xkm = (X509ExtendedKeyManager)kmgrs[0];
        return kmf;
    }

    /**
     * Creates a TrustManagerFactory for use during SSLContext initialization.
     *
     * @param trustStrore the KeyStore forming the base of the
     *      TrustManagerFactory
     *
     * @return the initialized TrustManagerFactory
     *
     * @throws IOException any IOExceptions during trust manager factory
     *      initialization.
     * @throws GeneralSecurityException if any failures during instantiation
     *      take place.
     */
    private static TrustManagerFactory createTrustManagerFactory(
            KeyStore trustStore) throws IOException, GeneralSecurityException {
        TrustManagerFactory tmf;
        PKIXBuilderParameters pkixParams =
                new PKIXBuilderParameters(trustStore, new X509CertSelector());
        pkixParams.setRevocationEnabled(false);
        tmf = TrustManagerFactory.getInstance("PKIX");
        tmf.init(new CertPathTrustManagerParameters(pkixParams));
        return tmf;
    }

    /*
     * Configure the client side socket.
     */
    @Override
    protected void configureClientSocket(SSLSocket socket) {
        String pVal;
        if ((pVal = clientParameters.get(ParamType.PROTOS)) != null) {
            socket.setEnabledProtocols(pVal.split(":"));
        }

        if ((pVal = clientParameters.get(ParamType.CIPHERS)) != null) {
            socket.setEnabledCipherSuites(pVal.split(":"));
        }
    }

    /*
     * Configure the server side socket.
     */
    @Override
    protected void configureServerSocket(SSLServerSocket socket) {
        String pVal;
        try {
            socket.setReuseAddress(true);
            if ((pVal = serverParameters.get(ParamType.PROTOS)) != null) {
                socket.setEnabledProtocols(pVal.split(":"));
            }

            if ((pVal = serverParameters.get(ParamType.CIPHERS)) != null) {
                socket.setEnabledCipherSuites(pVal.split(":"));
            }

            pVal = serverParameters.get(ParamType.CLIAUTH);
            socket.setWantClientAuth("WANT".equalsIgnoreCase(pVal));
            socket.setNeedClientAuth("NEED".equalsIgnoreCase(pVal));
        } catch (SocketException se) {
            throw new RuntimeException(se);
        }
    }


    @Override
    protected void runServerApplication(SSLSocket socket) throws Exception {
        InputStream sslIS = socket.getInputStream();
        OutputStream sslOS = socket.getOutputStream();

        sslIS.read();
        sslOS.write(85);
        sslOS.flush();

        if (serverChecker != null) {
            serverChecker.check(socket);
        }
    }

    @Override
    protected void runClientApplication(SSLSocket socket) throws Exception {
        InputStream sslIS = socket.getInputStream();
        OutputStream sslOS = socket.getOutputStream();

        sslOS.write(280);
        sslOS.flush();
        sslIS.read();

        if (clientChecker != null) {
            clientChecker.check(socket);
        }
    }

    public static void main(String[] args) throws Exception {
        SecurityUtils.removeFromDisabledTlsAlgs("TLSv1.1", "TLSv1");
        certFac = CertificateFactory.getInstance("X.509");
        String testFormat;

        System.out.println("===== Test KeyManager alias retrieval =====");
        testKeyManager(DEF_ALL_EE, "EdDSA",
                new String[] {"ee_ed25519", "ee_ed448"});

        testFormat =
                "===== Basic Ed25519 Server-side Authentication: %s =====\n";
        serverParameters.put(ParamType.KSENTRIES, "EE_ED25519:EE_RSA_2048");
        runtest(testFormat, isPeerEd25519, null, null, null);

        testFormat =
                "===== Basic Ed448 Server-side Authentication: %s =====\n";
        serverParameters.put(ParamType.KSENTRIES, "EE_ED448:EE_RSA_2048");
        runtest(testFormat, isPeerEd448, null, null, null);

        testFormat = "===== EC favored over EdDSA by default: %s =====\n";
        serverParameters.put(ParamType.KSENTRIES,
                "EE_ED25519:EE_ECDSA_SECP521R1");
        runtest(testFormat, isPeerP521, null, null, null);

        testFormat = "===== Override EC favoring by alias: %s =====\n";
        serverParameters.put(ParamType.CERTALIAS, "EE_ED25519");
        runtest(testFormat, isPeerEd25519, null, null, null);
        serverParameters.remove(ParamType.CERTALIAS);

        testFormat = "===== EdDSA Client Authentication: %s =====\n";
        serverParameters.put(ParamType.KSENTRIES, "EE_RSA_2048");
        serverParameters.put(ParamType.CLIAUTH, "NEED");
        clientParameters.put(ParamType.KSENTRIES, "EE_ED25519");
        runtest(testFormat, null, null, isPeerEd25519, null);
    }

    private static void testKeyManager(String keyStoreSpec, String keyType,
            String[] expAliases)
            throws GeneralSecurityException, IOException {
        char[] passChar = PASSWD.toCharArray();

        // Create the KeyManager factory and resulting KeyManager
        KeyManagerFactory kmf = createKeyManagerFactory(
                createKeyStore(keyStoreSpec, passChar), passChar);
        KeyManager[] kMgrs = kmf.getKeyManagers();
        X509KeyManager xkm = (X509KeyManager)kMgrs[0];

        String[] cliEdDSAAlises = xkm.getClientAliases(keyType, null);
        System.out.format("Client Aliases (%s): ", keyType);
        for (String alias : cliEdDSAAlises) {
            System.out.print(alias + " ");
        }
        System.out.println();

        String[] servEdDSAAliases = xkm.getServerAliases(keyType, null);
        System.out.format("Server Aliases (%s): ", keyType);
            for (String alias : servEdDSAAliases) {
            System.out.print(alias + " ");
        }
        System.out.println();

        if (!Arrays.equals(cliEdDSAAlises, expAliases)) {
            throw new RuntimeException("Client alias mismatch");
        } else if (!Arrays.equals(servEdDSAAliases, expAliases)) {
            throw new RuntimeException("Server alias mismatch");
        }
    }

    private static void runtest(String testNameFmt, SessionChecker cliChk,
            Class<? extends Throwable> cliExpExc, SessionChecker servChk,
            Class<? extends Throwable> servExpExc) {
        TEST_PROTOS.forEach(protocol -> {
            clientParameters.put(ParamType.PROTOS, protocol);
            TLSWithEdDSA testObj = new TLSWithEdDSA(cliChk, cliExpExc, servChk,
                        servExpExc);
            System.out.format(testNameFmt, protocol);
            try {
                testObj.run();
                if (testObj.clientException != null ||
                        testObj.serverException != null) {
                    throw new RuntimeException("Expected exception from " +
                            "either client or server but was missed");
                }
            } catch (Exception exc) {
                if (testObj.clientException == null &&
                        testObj.serverException == null) {
                    throw new RuntimeException(
                            "Expected test failure did not occur");
                } else if (testObj.clientException != null &&
                        !testObj.clientException.isAssignableFrom(exc.getClass())) {
                    throw new RuntimeException("Unexpected client exception " +
                            "detected: Expected " +
                            testObj.clientException.getName() +
                            ", got " + exc.getClass().getName());

                }  else if (testObj.serverException != null &&
                        !testObj.serverException.isAssignableFrom(exc.getClass())) {
                    throw new RuntimeException("Unexpected client exception " +
                            "detected: Expected " +
                            testObj.serverException.getName() +
                            ", got " + exc.getClass().getName());
                }
            }
            System.out.println();
        });
    }

    /**
     * A Custom KeyManager that allows the user to specify a key/certificate
     * by alias to be used for any TLS authentication actions.
     */
    static class AliasKeyManager implements X509KeyManager {
        private final String alias;
        private final KeyStore keystore;
        private final char[] pass;

        public AliasKeyManager(KeyStore keystore, char[] pass, String alias) {
            this.keystore = Objects.requireNonNull(keystore);
            this.alias = Objects.requireNonNull(alias);
            this.pass = Objects.requireNonNull(pass);
        }

        @Override
        public PrivateKey getPrivateKey(String alias) {
            try {
                return (PrivateKey)keystore.getKey(alias, pass);
            } catch (GeneralSecurityException exc) {
                throw new RuntimeException(exc);
            }
        }

        @Override
        public X509Certificate[] getCertificateChain(String alias) {
            try {
                Certificate[] certAr = keystore.getCertificateChain(alias);
                return (certAr != null) ? Arrays.copyOf(certAr, certAr.length,
                        X509Certificate[].class) : null;
            } catch (KeyStoreException ke) {
                throw new RuntimeException(ke);
            }
        }

        @Override
        public String chooseClientAlias(String[] keyType, Principal[] issuers,
                Socket socket) {
            // Blindly return the one selected alias.
            return alias;
        }

        @Override
        public String chooseServerAlias(String keyType, Principal[] issuers,
                Socket socket) {
            // Blindly return the one selected alias.
            return alias;
        }

        @Override
        public String[] getClientAliases(String keyType, Principal[] issuers) {
            // There can be only one!
            return new String[] { alias };
        }

        @Override
        public String[] getServerAliases(String keyType, Principal[] issuers) {
            // There can be only one!
            return new String[] { alias };
        }
    }

    static enum ParamType {
        PROTOS,
        CIPHERS,
        SIGALGS,
        CLIAUTH,
        KSENTRIES,
        TSENTRIES,
        CERTALIAS
    }
}