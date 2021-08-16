/*
 * Copyright (c) 2015, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.PublicKey;
import java.security.cert.CertPathValidatorException;
import java.security.cert.CertPathValidatorException.BasicReason;
import java.security.cert.CertPathValidatorException.Reason;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLProtocolException;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.X509ExtendedTrustManager;
import javax.net.ssl.X509TrustManager;
import javax.security.auth.x500.X500Principal;
import static sun.security.ssl.ClientAuthType.CLIENT_AUTH_REQUIRED;
import sun.security.ssl.ClientHello.ClientHelloMessage;
import sun.security.ssl.SSLHandshake.HandshakeMessage;
import sun.security.ssl.X509Authentication.X509Credentials;
import sun.security.ssl.X509Authentication.X509Possession;

/**
 * Pack of the CertificateMessage handshake message.
 */
final class CertificateMessage {
    static final SSLConsumer t12HandshakeConsumer =
        new T12CertificateConsumer();
    static final HandshakeProducer t12HandshakeProducer =
        new T12CertificateProducer();

    static final SSLConsumer t13HandshakeConsumer =
        new T13CertificateConsumer();
    static final HandshakeProducer t13HandshakeProducer =
        new T13CertificateProducer();

    /**
     * The Certificate handshake message for TLS 1.2 and previous
     * SSL/TLS protocol versions.
     *
     * In server mode, the certificate handshake message is sent whenever the
     * agreed-upon key exchange method uses certificates for authentication.
     * In client mode, this message is only sent if the server requests a
     * certificate for client authentication.
     *
     *       opaque ASN.1Cert<1..2^24-1>;
     *
     * SSL 3.0:
     *       struct {
     *           ASN.1Cert certificate_list<1..2^24-1>;
     *       } Certificate;
     * Note: For SSL 3.0 client authentication, if no suitable certificate
     * is available, the client should send a no_certificate alert instead.
     * This alert is only a warning; however, the server may respond with
     * a fatal handshake failure alert if client authentication is required.
     *
     * TLS 1.0/1.1/1.2:
     *       struct {
     *           ASN.1Cert certificate_list<0..2^24-1>;
     *       } Certificate;
     */
    static final class T12CertificateMessage extends HandshakeMessage {
        final List<byte[]> encodedCertChain;

        T12CertificateMessage(HandshakeContext handshakeContext,
                X509Certificate[] certChain) throws SSLException {
            super(handshakeContext);

            List<byte[]> encodedCerts = new ArrayList<>(certChain.length);
            for (X509Certificate cert : certChain) {
                try {
                    encodedCerts.add(cert.getEncoded());
                } catch (CertificateEncodingException cee) {
                    // unlikely
                    throw handshakeContext.conContext.fatal(
                            Alert.INTERNAL_ERROR,
                            "Could not encode certificate (" +
                            cert.getSubjectX500Principal() + ")", cee);
                }
            }

            this.encodedCertChain = encodedCerts;
        }

        T12CertificateMessage(HandshakeContext handshakeContext,
                ByteBuffer m) throws IOException {
            super(handshakeContext);

            int listLen = Record.getInt24(m);
            if (listLen > m.remaining()) {
                throw handshakeContext.conContext.fatal(
                    Alert.ILLEGAL_PARAMETER,
                    "Error parsing certificate message:no sufficient data");
            }
            if (listLen > 0) {
                List<byte[]> encodedCerts = new LinkedList<>();
                while (listLen > 0) {
                    byte[] encodedCert = Record.getBytes24(m);
                    listLen -= (3 + encodedCert.length);
                    encodedCerts.add(encodedCert);
                    if (encodedCerts.size() > SSLConfiguration.maxCertificateChainLength) {
                        throw new SSLProtocolException(
                                "The certificate chain length ("
                                + encodedCerts.size()
                                + ") exceeds the maximum allowed length ("
                                + SSLConfiguration.maxCertificateChainLength
                                + ")");
                    }

                }
                this.encodedCertChain = encodedCerts;
            } else {
                this.encodedCertChain = Collections.emptyList();
            }
        }

        @Override
        public SSLHandshake handshakeType() {
            return SSLHandshake.CERTIFICATE;
        }

        @Override
        public int messageLength() {
            int msgLen = 3;
            for (byte[] encodedCert : encodedCertChain) {
                msgLen += (encodedCert.length + 3);
            }

            return msgLen;
        }

        @Override
        public void send(HandshakeOutStream hos) throws IOException {
            int listLen = 0;
            for (byte[] encodedCert : encodedCertChain) {
                listLen += (encodedCert.length + 3);
            }

            hos.putInt24(listLen);
            for (byte[] encodedCert : encodedCertChain) {
                hos.putBytes24(encodedCert);
            }
        }

        @Override
        public String toString() {
            if (encodedCertChain.isEmpty()) {
                return "\"Certificates\": <empty list>";
            }

            Object[] x509Certs = new Object[encodedCertChain.size()];
            try {
                CertificateFactory cf = CertificateFactory.getInstance("X.509");
                int i = 0;
                for (byte[] encodedCert : encodedCertChain) {
                    Object obj;
                    try {
                        obj = (X509Certificate)cf.generateCertificate(
                                    new ByteArrayInputStream(encodedCert));
                    } catch (CertificateException ce) {
                        obj = encodedCert;
                    }
                    x509Certs[i++] = obj;
                }
            } catch (CertificateException ce) {
                // no X.509 certificate factory service
                int i = 0;
                for (byte[] encodedCert : encodedCertChain) {
                    x509Certs[i++] = encodedCert;
                }
            }

            MessageFormat messageFormat = new MessageFormat(
                    "\"Certificates\": [\n" +
                    "{0}\n" +
                    "]",
                    Locale.ENGLISH);
            Object[] messageFields = {
                SSLLogger.toString(x509Certs)
            };

            return messageFormat.format(messageFields);
        }
    }

    /**
     * The "Certificate" handshake message producer for TLS 1.2 and
     * previous SSL/TLS protocol versions.
     */
    private static final
            class T12CertificateProducer implements HandshakeProducer {
        // Prevent instantiation of this class.
        private T12CertificateProducer() {
            // blank
        }

        @Override
        public byte[] produce(ConnectionContext context,
                HandshakeMessage message) throws IOException {
            // The producing happens in handshake context only.
            HandshakeContext hc = (HandshakeContext)context;
            if (hc.sslConfig.isClientMode) {
                return onProduceCertificate(
                        (ClientHandshakeContext)context, message);
            } else {
                return onProduceCertificate(
                        (ServerHandshakeContext)context, message);
            }
        }

        private byte[] onProduceCertificate(ServerHandshakeContext shc,
                SSLHandshake.HandshakeMessage message) throws IOException {
            X509Possession x509Possession = null;
            for (SSLPossession possession : shc.handshakePossessions) {
                if (possession instanceof X509Possession) {
                    x509Possession = (X509Possession)possession;
                    break;
                }
            }

            if (x509Possession == null) {       // unlikely
                throw shc.conContext.fatal(Alert.INTERNAL_ERROR,
                    "No expected X.509 certificate for server authentication");
            }

            shc.handshakeSession.setLocalPrivateKey(
                    x509Possession.popPrivateKey);
            shc.handshakeSession.setLocalCertificates(x509Possession.popCerts);
            T12CertificateMessage cm =
                    new T12CertificateMessage(shc, x509Possession.popCerts);
            if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                SSLLogger.fine(
                    "Produced server Certificate handshake message", cm);
            }

            // Output the handshake message.
            cm.write(shc.handshakeOutput);
            shc.handshakeOutput.flush();

            // The handshake message has been delivered.
            return null;
        }

        private byte[] onProduceCertificate(ClientHandshakeContext chc,
                SSLHandshake.HandshakeMessage message) throws IOException {
            X509Possession x509Possession = null;
            for (SSLPossession possession : chc.handshakePossessions) {
                if (possession instanceof X509Possession) {
                    x509Possession = (X509Possession)possession;
                    break;
                }
            }

            // Report to the server if no appropriate cert was found.  For
            // SSL 3.0, send a no_certificate alert;  TLS 1.0/1.1/1.2 uses
            // an empty cert chain instead.
            if (x509Possession == null) {
                if (chc.negotiatedProtocol.useTLS10PlusSpec()) {
                    if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                        SSLLogger.fine(
                            "No X.509 certificate for client authentication, " +
                            "use empty Certificate message instead");
                    }

                    x509Possession =
                            new X509Possession(null, new X509Certificate[0]);
                } else {
                    if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                        SSLLogger.fine(
                            "No X.509 certificate for client authentication, " +
                            "send a no_certificate alert");
                    }

                    chc.conContext.warning(Alert.NO_CERTIFICATE);
                    return null;
                }
            }

            chc.handshakeSession.setLocalPrivateKey(
                    x509Possession.popPrivateKey);
            if (x509Possession.popCerts != null &&
                    x509Possession.popCerts.length != 0) {
                chc.handshakeSession.setLocalCertificates(
                        x509Possession.popCerts);
            } else {
                chc.handshakeSession.setLocalCertificates(null);
            }
            T12CertificateMessage cm =
                    new T12CertificateMessage(chc, x509Possession.popCerts);
            if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                SSLLogger.fine(
                    "Produced client Certificate handshake message", cm);
            }

            // Output the handshake message.
            cm.write(chc.handshakeOutput);
            chc.handshakeOutput.flush();

            // The handshake message has been delivered.
            return null;
        }
    }

    /**
     * The "Certificate" handshake message consumer for TLS 1.2 and
     * previous SSL/TLS protocol versions.
     */
    static final
            class T12CertificateConsumer implements SSLConsumer {
        // Prevent instantiation of this class.
        private T12CertificateConsumer() {
            // blank
        }

        @Override
        public void consume(ConnectionContext context,
                ByteBuffer message) throws IOException {
            // The consuming happens in handshake context only.
            HandshakeContext hc = (HandshakeContext)context;

            // clean up this consumer
            hc.handshakeConsumers.remove(SSLHandshake.CERTIFICATE.id);

            T12CertificateMessage cm = new T12CertificateMessage(hc, message);
            if (hc.sslConfig.isClientMode) {
                if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                    SSLLogger.fine(
                        "Consuming server Certificate handshake message", cm);
                }
                onCertificate((ClientHandshakeContext)context, cm);
            } else {
                if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                    SSLLogger.fine(
                        "Consuming client Certificate handshake message", cm);
                }
                onCertificate((ServerHandshakeContext)context, cm);
            }
        }

        private void onCertificate(ServerHandshakeContext shc,
                T12CertificateMessage certificateMessage )throws IOException {
            List<byte[]> encodedCerts = certificateMessage.encodedCertChain;
            if (encodedCerts == null || encodedCerts.isEmpty()) {
                // For empty Certificate messages, we should not expect
                // a CertificateVerify message to follow
                shc.handshakeConsumers.remove(
                        SSLHandshake.CERTIFICATE_VERIFY.id);
                if (shc.sslConfig.clientAuthType !=
                        ClientAuthType.CLIENT_AUTH_REQUESTED) {
                    // unexpected or require client authentication
                    throw shc.conContext.fatal(Alert.BAD_CERTIFICATE,
                        "Empty server certificate chain");
                } else {
                    return;
                }
            }

            X509Certificate[] x509Certs =
                    new X509Certificate[encodedCerts.size()];
            try {
                CertificateFactory cf = CertificateFactory.getInstance("X.509");
                int i = 0;
                for (byte[] encodedCert : encodedCerts) {
                    x509Certs[i++] = (X509Certificate)cf.generateCertificate(
                                    new ByteArrayInputStream(encodedCert));
                }
            } catch (CertificateException ce) {
                throw shc.conContext.fatal(Alert.BAD_CERTIFICATE,
                    "Failed to parse server certificates", ce);
            }

            checkClientCerts(shc, x509Certs);

            //
            // update
            //
            shc.handshakeCredentials.add(
                new X509Credentials(x509Certs[0].getPublicKey(), x509Certs));
            shc.handshakeSession.setPeerCertificates(x509Certs);
        }

        private void onCertificate(ClientHandshakeContext chc,
                T12CertificateMessage certificateMessage) throws IOException {
            List<byte[]> encodedCerts = certificateMessage.encodedCertChain;
            if (encodedCerts == null || encodedCerts.isEmpty()) {
                throw chc.conContext.fatal(Alert.BAD_CERTIFICATE,
                    "Empty server certificate chain");
            }

            X509Certificate[] x509Certs =
                    new X509Certificate[encodedCerts.size()];
            try {
                CertificateFactory cf = CertificateFactory.getInstance("X.509");
                int i = 0;
                for (byte[] encodedCert : encodedCerts) {
                    x509Certs[i++] = (X509Certificate)cf.generateCertificate(
                                    new ByteArrayInputStream(encodedCert));
                }
            } catch (CertificateException ce) {
                throw chc.conContext.fatal(Alert.BAD_CERTIFICATE,
                    "Failed to parse server certificates", ce);
            }

            // Allow server certificate change in client side during
            // renegotiation after a session-resumption abbreviated
            // initial handshake?
            //
            // DO NOT need to check allowUnsafeServerCertChange here. We only
            // reserve server certificates when allowUnsafeServerCertChange is
            // false.
            if (chc.reservedServerCerts != null &&
                    !chc.handshakeSession.useExtendedMasterSecret) {
                // It is not necessary to check the certificate update if
                // endpoint identification is enabled.
                String identityAlg = chc.sslConfig.identificationProtocol;
                if ((identityAlg == null || identityAlg.isEmpty()) &&
                        !isIdentityEquivalent(x509Certs[0],
                                chc.reservedServerCerts[0])) {
                    throw chc.conContext.fatal(Alert.BAD_CERTIFICATE,
                            "server certificate change is restricted " +
                            "during renegotiation");
                }
            }

            // ask the trust manager to verify the chain
            if (chc.staplingActive) {
                // Defer the certificate check until after we've received the
                // CertificateStatus message.  If that message doesn't come in
                // immediately following this message we will execute the
                // check from CertificateStatus' absent handler.
                chc.deferredCerts = x509Certs;
            } else {
                // We're not doing stapling, so perform the check right now
                checkServerCerts(chc, x509Certs);
            }

            //
            // update
            //
            chc.handshakeCredentials.add(
                new X509Credentials(x509Certs[0].getPublicKey(), x509Certs));
            chc.handshakeSession.setPeerCertificates(x509Certs);
        }

        /*
         * Whether the certificates can represent the same identity?
         *
         * The certificates can be used to represent the same identity:
         *     1. If the subject alternative names of IP address are present
         *        in both certificates, they should be identical; otherwise,
         *     2. if the subject alternative names of DNS name are present in
         *        both certificates, they should be identical; otherwise,
         *     3. if the subject fields are present in both certificates, the
         *        certificate subjects and issuers should be identical.
         */
        private static boolean isIdentityEquivalent(X509Certificate thisCert,
                X509Certificate prevCert) {
            if (thisCert.equals(prevCert)) {
                return true;
            }

            // check subject alternative names
            Collection<List<?>> thisSubjectAltNames = null;
            try {
                thisSubjectAltNames = thisCert.getSubjectAlternativeNames();
            } catch (CertificateParsingException cpe) {
                if (SSLLogger.isOn && SSLLogger.isOn("handshake")) {
                    SSLLogger.fine(
                        "Attempt to obtain subjectAltNames extension failed!");
                }
            }

            Collection<List<?>> prevSubjectAltNames = null;
            try {
                prevSubjectAltNames = prevCert.getSubjectAlternativeNames();
            } catch (CertificateParsingException cpe) {
                if (SSLLogger.isOn && SSLLogger.isOn("handshake")) {
                    SSLLogger.fine(
                        "Attempt to obtain subjectAltNames extension failed!");
                }
            }

            if (thisSubjectAltNames != null && prevSubjectAltNames != null) {
                // check the iPAddress field in subjectAltName extension
                //
                // 7: subject alternative name of type IP.
                Collection<String> thisSubAltIPAddrs =
                            getSubjectAltNames(thisSubjectAltNames, 7);
                Collection<String> prevSubAltIPAddrs =
                            getSubjectAltNames(prevSubjectAltNames, 7);
                if (thisSubAltIPAddrs != null && prevSubAltIPAddrs != null &&
                    isEquivalent(thisSubAltIPAddrs, prevSubAltIPAddrs)) {
                    return true;
                }

                // check the dNSName field in subjectAltName extension
                // 2: subject alternative name of type IP.
                Collection<String> thisSubAltDnsNames =
                            getSubjectAltNames(thisSubjectAltNames, 2);
                Collection<String> prevSubAltDnsNames =
                            getSubjectAltNames(prevSubjectAltNames, 2);
                if (thisSubAltDnsNames != null && prevSubAltDnsNames != null &&
                    isEquivalent(thisSubAltDnsNames, prevSubAltDnsNames)) {
                    return true;
                }
            }

            // check the certificate subject and issuer
            X500Principal thisSubject = thisCert.getSubjectX500Principal();
            X500Principal prevSubject = prevCert.getSubjectX500Principal();
            X500Principal thisIssuer = thisCert.getIssuerX500Principal();
            X500Principal prevIssuer = prevCert.getIssuerX500Principal();

            return (!thisSubject.getName().isEmpty() &&
                    !prevSubject.getName().isEmpty() &&
                    thisSubject.equals(prevSubject) &&
                    thisIssuer.equals(prevIssuer));
        }

        /*
         * Returns the subject alternative name of the specified type in the
         * subjectAltNames extension of a certificate.
         *
         * Note that only those subjectAltName types that use String data
         * should be passed into this function.
         */
        private static Collection<String> getSubjectAltNames(
                Collection<List<?>> subjectAltNames, int type) {
            HashSet<String> subAltDnsNames = null;
            for (List<?> subjectAltName : subjectAltNames) {
                int subjectAltNameType = (Integer)subjectAltName.get(0);
                if (subjectAltNameType == type) {
                    String subAltDnsName = (String)subjectAltName.get(1);
                    if ((subAltDnsName != null) && !subAltDnsName.isEmpty()) {
                        if (subAltDnsNames == null) {
                            subAltDnsNames =
                                    new HashSet<>(subjectAltNames.size());
                        }
                        subAltDnsNames.add(subAltDnsName);
                    }
                }
            }

            return subAltDnsNames;
        }

        private static boolean isEquivalent(Collection<String> thisSubAltNames,
                Collection<String> prevSubAltNames) {
            for (String thisSubAltName : thisSubAltNames) {
                for (String prevSubAltName : prevSubAltNames) {
                    // Only allow the exactly match.  No wildcard character
                    // checking.
                    if (thisSubAltName.equalsIgnoreCase(prevSubAltName)) {
                        return true;
                    }
                }
            }

            return false;
        }

        /**
         * Perform client-side checking of server certificates.
         *
         * @param certs an array of {@code X509Certificate} objects presented
         *      by the server in the ServerCertificate message.
         *
         * @throws IOException if a failure occurs during validation or
         *      the trust manager associated with the {@code SSLContext} is not
         *      an {@code X509ExtendedTrustManager}.
         */
        static void checkServerCerts(ClientHandshakeContext chc,
                X509Certificate[] certs) throws IOException {

            X509TrustManager tm = chc.sslContext.getX509TrustManager();

            // find out the key exchange algorithm used
            // use "RSA" for non-ephemeral "RSA_EXPORT"
            String keyExchangeString;
            if (chc.negotiatedCipherSuite.keyExchange ==
                    CipherSuite.KeyExchange.K_RSA_EXPORT ||
                    chc.negotiatedCipherSuite.keyExchange ==
                            CipherSuite.KeyExchange.K_DHE_RSA_EXPORT) {
                keyExchangeString = CipherSuite.KeyExchange.K_RSA.name;
            } else {
                keyExchangeString = chc.negotiatedCipherSuite.keyExchange.name;
            }

            try {
                if (tm instanceof X509ExtendedTrustManager) {
                    if (chc.conContext.transport instanceof SSLEngine) {
                        SSLEngine engine = (SSLEngine)chc.conContext.transport;
                        ((X509ExtendedTrustManager)tm).checkServerTrusted(
                            certs.clone(),
                            keyExchangeString,
                            engine);
                    } else {
                        SSLSocket socket = (SSLSocket)chc.conContext.transport;
                        ((X509ExtendedTrustManager)tm).checkServerTrusted(
                            certs.clone(),
                            keyExchangeString,
                            socket);
                    }
                } else {
                    // Unlikely to happen, because we have wrapped the old
                    // X509TrustManager with the new X509ExtendedTrustManager.
                    throw new CertificateException(
                            "Improper X509TrustManager implementation");
                }

                // Once the server certificate chain has been validated, set
                // the certificate chain in the TLS session.
                chc.handshakeSession.setPeerCertificates(certs);
            } catch (CertificateException ce) {
                throw chc.conContext.fatal(getCertificateAlert(chc, ce), ce);
            }
        }

        private static void checkClientCerts(ServerHandshakeContext shc,
                X509Certificate[] certs) throws IOException {
            X509TrustManager tm = shc.sslContext.getX509TrustManager();

            // find out the types of client authentication used
            PublicKey key = certs[0].getPublicKey();
            String keyAlgorithm = key.getAlgorithm();
            String authType;
            switch (keyAlgorithm) {
                case "RSA":
                case "DSA":
                case "EC":
                case "RSASSA-PSS":
                    authType = keyAlgorithm;
                    break;
                default:
                    // unknown public key type
                    authType = "UNKNOWN";
            }

            try {
                if (tm instanceof X509ExtendedTrustManager) {
                    if (shc.conContext.transport instanceof SSLEngine) {
                        SSLEngine engine = (SSLEngine)shc.conContext.transport;
                        ((X509ExtendedTrustManager)tm).checkClientTrusted(
                            certs.clone(),
                            authType,
                            engine);
                    } else {
                        SSLSocket socket = (SSLSocket)shc.conContext.transport;
                        ((X509ExtendedTrustManager)tm).checkClientTrusted(
                            certs.clone(),
                            authType,
                            socket);
                    }
                } else {
                    // Unlikely to happen, because we have wrapped the old
                    // X509TrustManager with the new X509ExtendedTrustManager.
                    throw new CertificateException(
                            "Improper X509TrustManager implementation");
                }
            } catch (CertificateException ce) {
                throw shc.conContext.fatal(Alert.CERTIFICATE_UNKNOWN, ce);
            }
        }

        /**
         * When a failure happens during certificate checking from an
         * {@link X509TrustManager}, determine what TLS alert description
         * to use.
         *
         * @param cexc The exception thrown by the {@link X509TrustManager}
         *
         * @return A byte value corresponding to a TLS alert description number.
         */
        private static Alert getCertificateAlert(
                ClientHandshakeContext chc, CertificateException cexc) {
            // The specific reason for the failure will determine how to
            // set the alert description value
            Alert alert = Alert.CERTIFICATE_UNKNOWN;

            Throwable baseCause = cexc.getCause();
            if (baseCause instanceof CertPathValidatorException) {
                CertPathValidatorException cpve =
                        (CertPathValidatorException)baseCause;
                Reason reason = cpve.getReason();
                if (reason == BasicReason.REVOKED) {
                    alert = chc.staplingActive ?
                            Alert.BAD_CERT_STATUS_RESPONSE :
                            Alert.CERTIFICATE_REVOKED;
                } else if (
                        reason == BasicReason.UNDETERMINED_REVOCATION_STATUS) {
                    alert = chc.staplingActive ?
                            Alert.BAD_CERT_STATUS_RESPONSE :
                            Alert.CERTIFICATE_UNKNOWN;
                } else if (reason == BasicReason.ALGORITHM_CONSTRAINED) {
                    alert = Alert.UNSUPPORTED_CERTIFICATE;
                } else if (reason == BasicReason.EXPIRED) {
                    alert = Alert.CERTIFICATE_EXPIRED;
                } else if (reason == BasicReason.INVALID_SIGNATURE ||
                        reason == BasicReason.NOT_YET_VALID) {
                    alert = Alert.BAD_CERTIFICATE;
                }
            }

            return alert;
        }

    }

    /**
     * The certificate entry used in Certificate handshake message for TLS 1.3.
     */
    static final class CertificateEntry {
        final byte[] encoded;       // encoded cert or public key
        private final SSLExtensions extensions;

        CertificateEntry(byte[] encoded, SSLExtensions extensions) {
            this.encoded = encoded;
            this.extensions = extensions;
        }

        private int getEncodedSize() {
            int extLen = extensions.length();
            if (extLen == 0) {
                extLen = 2;     // empty extensions
            }
            return 3 + encoded.length + extLen;
        }

        @Override
        public String toString() {
            MessageFormat messageFormat = new MessageFormat(
                "\n'{'\n" +
                "{0}\n" +                       // X.509 certificate
                "  \"extensions\": '{'\n" +
                "{1}\n" +
                "  '}'\n" +
                "'}',", Locale.ENGLISH);

            Object x509Certs;
            try {
                // Don't support certificate type extension (RawPublicKey) yet.
                CertificateFactory cf = CertificateFactory.getInstance("X.509");
                x509Certs =
                    cf.generateCertificate(new ByteArrayInputStream(encoded));
            } catch (CertificateException ce) {
                // no X.509 certificate factory service
                x509Certs = encoded;
            }

            Object[] messageFields = {
                SSLLogger.toString(x509Certs),
                Utilities.indent(extensions.toString(), "    ")
            };

            return messageFormat.format(messageFields);
        }
    }

    /**
     * The Certificate handshake message for TLS 1.3.
     */
    static final class T13CertificateMessage extends HandshakeMessage {
        private final byte[] requestContext;
        private final List<CertificateEntry> certEntries;

        T13CertificateMessage(HandshakeContext context,
                byte[] requestContext, X509Certificate[] certificates)
                throws SSLException, CertificateException  {
            super(context);

            this.requestContext = requestContext.clone();
            this.certEntries = new LinkedList<>();
            for (X509Certificate cert : certificates) {
                byte[] encoded = cert.getEncoded();
                SSLExtensions extensions = new SSLExtensions(this);
                certEntries.add(new CertificateEntry(encoded, extensions));
            }
        }

        T13CertificateMessage(HandshakeContext handshakeContext,
                byte[] requestContext, List<CertificateEntry> certificates) {
            super(handshakeContext);

            this.requestContext = requestContext.clone();
            this.certEntries = certificates;
        }

        T13CertificateMessage(HandshakeContext handshakeContext,
                ByteBuffer m) throws IOException {
            super(handshakeContext);

            // struct {
            //      opaque certificate_request_context<0..2^8-1>;
            //      CertificateEntry certificate_list<0..2^24-1>;
            //  } Certificate;
            if (m.remaining() < 4) {
                throw new SSLProtocolException(
                        "Invalid Certificate message: " +
                        "insufficient data (length=" + m.remaining() + ")");
            }
            this.requestContext = Record.getBytes8(m);

            if (m.remaining() < 3) {
                throw new SSLProtocolException(
                        "Invalid Certificate message: " +
                        "insufficient certificate entries data (length=" +
                        m.remaining() + ")");
            }

            int listLen = Record.getInt24(m);
            if (listLen != m.remaining()) {
                throw new SSLProtocolException(
                    "Invalid Certificate message: " +
                    "incorrect list length (length=" + listLen + ")");
            }

            SSLExtension[] enabledExtensions =
                handshakeContext.sslConfig.getEnabledExtensions(
                        SSLHandshake.CERTIFICATE);
            List<CertificateEntry> certList = new LinkedList<>();
            while (m.hasRemaining()) {
                // Note: support only X509 CertificateType right now.
                byte[] encodedCert = Record.getBytes24(m);
                if (encodedCert.length == 0) {
                    throw new SSLProtocolException(
                        "Invalid Certificate message: empty cert_data");
                }

                SSLExtensions extensions =
                        new SSLExtensions(this, m, enabledExtensions);
                certList.add(new CertificateEntry(encodedCert, extensions));
                if (certList.size() > SSLConfiguration.maxCertificateChainLength) {
                    throw new SSLProtocolException(
                            "The certificate chain length ("
                            + certList.size()
                            + ") exceeds the maximum allowed length ("
                            + SSLConfiguration.maxCertificateChainLength
                            + ")");
                }
            }

            this.certEntries = Collections.unmodifiableList(certList);
        }

        @Override
        public SSLHandshake handshakeType() {
            return SSLHandshake.CERTIFICATE;
        }

        @Override
        public int messageLength() {
            int msgLen = 4 + requestContext.length;
            for (CertificateEntry entry : certEntries) {
                msgLen += entry.getEncodedSize();
            }

            return msgLen;
        }

        @Override
        public void send(HandshakeOutStream hos) throws IOException {
            int entryListLen = 0;
            for (CertificateEntry entry : certEntries) {
                entryListLen += entry.getEncodedSize();
            }

            hos.putBytes8(requestContext);
            hos.putInt24(entryListLen);
            for (CertificateEntry entry : certEntries) {
                hos.putBytes24(entry.encoded);
                // Is it an empty extensions?
                if (entry.extensions.length() == 0) {
                    hos.putInt16(0);
                } else {
                    entry.extensions.send(hos);
                }
            }
        }

        @Override
        public String toString() {
            MessageFormat messageFormat = new MessageFormat(
                "\"Certificate\": '{'\n" +
                "  \"certificate_request_context\": \"{0}\",\n" +
                "  \"certificate_list\": [{1}\n]\n" +
                "'}'",
                Locale.ENGLISH);

            StringBuilder builder = new StringBuilder(512);
            for (CertificateEntry entry : certEntries) {
                builder.append(entry.toString());
            }

            Object[] messageFields = {
                Utilities.toHexString(requestContext),
                Utilities.indent(builder.toString())
            };

            return messageFormat.format(messageFields);
        }
    }

    /**
     * The "Certificate" handshake message producer for TLS 1.3.
     */
    private static final
            class T13CertificateProducer implements HandshakeProducer {
        // Prevent instantiation of this class.
        private T13CertificateProducer() {
            // blank
        }

        @Override
        public byte[] produce(ConnectionContext context,
                HandshakeMessage message) throws IOException {
            // The producing happens in handshake context only.
            HandshakeContext hc = (HandshakeContext)context;
            if (hc.sslConfig.isClientMode) {
                return onProduceCertificate(
                        (ClientHandshakeContext)context, message);
            } else {
                return onProduceCertificate(
                        (ServerHandshakeContext)context, message);
            }
        }

        private byte[] onProduceCertificate(ServerHandshakeContext shc,
                HandshakeMessage message) throws IOException {
            ClientHelloMessage clientHello = (ClientHelloMessage)message;

            SSLPossession pos = choosePossession(shc, clientHello);
            if (pos == null) {
                throw shc.conContext.fatal(Alert.HANDSHAKE_FAILURE,
                        "No available authentication scheme");
            }

            if (!(pos instanceof X509Possession)) {
                throw shc.conContext.fatal(Alert.HANDSHAKE_FAILURE,
                        "No X.509 certificate for server authentication");
            }

            X509Possession x509Possession = (X509Possession)pos;
            X509Certificate[] localCerts = x509Possession.popCerts;
            if (localCerts == null || localCerts.length == 0) {
                throw shc.conContext.fatal(Alert.HANDSHAKE_FAILURE,
                        "No X.509 certificate for server authentication");
            }

            // update the context
            shc.handshakePossessions.add(x509Possession);
            shc.handshakeSession.setLocalPrivateKey(
                    x509Possession.popPrivateKey);
            shc.handshakeSession.setLocalCertificates(localCerts);
            T13CertificateMessage cm;
            try {
                cm = new T13CertificateMessage(shc, (new byte[0]), localCerts);
            } catch (SSLException | CertificateException ce) {
                throw shc.conContext.fatal(Alert.HANDSHAKE_FAILURE,
                        "Failed to produce server Certificate message", ce);
            }

            // Check the OCSP stapling extensions and attempt
            // to get responses.  If the resulting stapleParams is non
            // null, it implies that stapling is enabled on the server side.
            shc.stapleParams = StatusResponseManager.processStapling(shc);
            shc.staplingActive = (shc.stapleParams != null);

            // Process extensions for each CertificateEntry.
            // Since there can be multiple CertificateEntries within a
            // single CT message, we will pin a specific CertificateEntry
            // into the ServerHandshakeContext so individual extension
            // producers know which X509Certificate it is processing in
            // each call.
            SSLExtension[] enabledCTExts = shc.sslConfig.getEnabledExtensions(
                    SSLHandshake.CERTIFICATE,
                    Arrays.asList(ProtocolVersion.PROTOCOLS_OF_13));
            for (CertificateEntry certEnt : cm.certEntries) {
                shc.currentCertEntry = certEnt;
                certEnt.extensions.produce(shc, enabledCTExts);
            }

            if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                SSLLogger.fine("Produced server Certificate message", cm);
            }

            // Output the handshake message.
            cm.write(shc.handshakeOutput);
            shc.handshakeOutput.flush();

            // The handshake message has been delivered.
            return null;
        }

        private static SSLPossession choosePossession(
                HandshakeContext hc,
                ClientHelloMessage clientHello) throws IOException {
            if (hc.peerRequestedCertSignSchemes == null ||
                    hc.peerRequestedCertSignSchemes.isEmpty()) {
                if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                    SSLLogger.warning(
                            "No signature_algorithms(_cert) in ClientHello");
                }
                return null;
            }

            Collection<String> checkedKeyTypes = new HashSet<>();
            for (SignatureScheme ss : hc.peerRequestedCertSignSchemes) {
                if (checkedKeyTypes.contains(ss.keyAlgorithm)) {
                    if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                        SSLLogger.warning(
                            "Unsupported authentication scheme: " + ss.name);
                    }
                    continue;
                }

                // Don't select a signature scheme unless we will be able to
                // produce a CertificateVerify message later
                if (SignatureScheme.getPreferableAlgorithm(
                        hc.algorithmConstraints,
                        hc.peerRequestedSignatureSchemes,
                        ss, hc.negotiatedProtocol) == null) {

                    if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                        SSLLogger.warning(
                            "Unable to produce CertificateVerify for " +
                            "signature scheme: " + ss.name);
                    }
                    checkedKeyTypes.add(ss.keyAlgorithm);
                    continue;
                }

                SSLAuthentication ka = X509Authentication.valueOf(ss);
                if (ka == null) {
                    if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                        SSLLogger.warning(
                            "Unsupported authentication scheme: " + ss.name);
                    }
                    checkedKeyTypes.add(ss.keyAlgorithm);
                    continue;
                }

                SSLPossession pos = ka.createPossession(hc);
                if (pos == null) {
                    if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                        SSLLogger.warning(
                            "Unavailable authentication scheme: " + ss.name);
                    }
                    continue;
                }

                return pos;
            }

            if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                SSLLogger.warning("No available authentication scheme");
            }
            return null;
        }

        private byte[] onProduceCertificate(ClientHandshakeContext chc,
                HandshakeMessage message) throws IOException {
            ClientHelloMessage clientHello = (ClientHelloMessage)message;
            SSLPossession pos = choosePossession(chc, clientHello);
            X509Certificate[] localCerts;
            if (pos == null) {
                if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                    SSLLogger.fine("No available client authentication scheme");
                }
                localCerts = new X509Certificate[0];
            } else {
                chc.handshakePossessions.add(pos);
                if (!(pos instanceof X509Possession)) {
                    if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                        SSLLogger.fine(
                            "No X.509 certificate for client authentication");
                    }
                    localCerts = new X509Certificate[0];
                } else {
                    X509Possession x509Possession = (X509Possession)pos;
                    localCerts = x509Possession.popCerts;
                    chc.handshakeSession.setLocalPrivateKey(
                            x509Possession.popPrivateKey);
                }
            }

            if (localCerts != null && localCerts.length != 0) {
                chc.handshakeSession.setLocalCertificates(localCerts);
            } else {
                chc.handshakeSession.setLocalCertificates(null);
            }

            T13CertificateMessage cm;
            try {
                cm = new T13CertificateMessage(
                        chc, chc.certRequestContext, localCerts);
            } catch (SSLException | CertificateException ce) {
                throw chc.conContext.fatal(Alert.HANDSHAKE_FAILURE,
                        "Failed to produce client Certificate message", ce);
            }
            if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                SSLLogger.fine("Produced client Certificate message", cm);
            }

            // Output the handshake message.
            cm.write(chc.handshakeOutput);
            chc.handshakeOutput.flush();

            // The handshake message has been delivered.
            return null;
        }
    }

    /**
     * The "Certificate" handshake message consumer for TLS 1.3.
     */
    private static final class T13CertificateConsumer implements SSLConsumer {
        // Prevent instantiation of this class.
        private T13CertificateConsumer() {
            // blank
        }

        @Override
        public void consume(ConnectionContext context,
                ByteBuffer message) throws IOException {
            // The consuming happens in handshake context only.
            HandshakeContext hc = (HandshakeContext)context;

            // clean up this consumer
            hc.handshakeConsumers.remove(SSLHandshake.CERTIFICATE.id);
            T13CertificateMessage cm = new T13CertificateMessage(hc, message);
            if (hc.sslConfig.isClientMode) {
                if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                    SSLLogger.fine(
                        "Consuming server Certificate handshake message", cm);
                }
                onConsumeCertificate((ClientHandshakeContext)context, cm);
            } else {
                if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                    SSLLogger.fine(
                        "Consuming client Certificate handshake message", cm);
                }
                onConsumeCertificate((ServerHandshakeContext)context, cm);
            }
        }

        private void onConsumeCertificate(ServerHandshakeContext shc,
                T13CertificateMessage certificateMessage )throws IOException {
            if (certificateMessage.certEntries == null ||
                    certificateMessage.certEntries.isEmpty()) {
                // For empty Certificate messages, we should not expect
                // a CertificateVerify message to follow
                shc.handshakeConsumers.remove(
                        SSLHandshake.CERTIFICATE_VERIFY.id);
                if (shc.sslConfig.clientAuthType == CLIENT_AUTH_REQUIRED) {
                    throw shc.conContext.fatal(Alert.BAD_CERTIFICATE,
                        "Empty client certificate chain");
                } else {
                    // optional client authentication
                    return;
                }
            }

            // check client certificate entries
            X509Certificate[] cliCerts =
                    checkClientCerts(shc, certificateMessage.certEntries);

            //
            // update
            //
            shc.handshakeCredentials.add(
                new X509Credentials(cliCerts[0].getPublicKey(), cliCerts));
            shc.handshakeSession.setPeerCertificates(cliCerts);
        }

        private void onConsumeCertificate(ClientHandshakeContext chc,
                T13CertificateMessage certificateMessage )throws IOException {
            if (certificateMessage.certEntries == null ||
                    certificateMessage.certEntries.isEmpty()) {
                throw chc.conContext.fatal(Alert.BAD_CERTIFICATE,
                    "Empty server certificate chain");
            }

            // Each CertificateEntry will have its own set of extensions
            // which must be consumed.
            SSLExtension[] enabledExtensions =
                chc.sslConfig.getEnabledExtensions(SSLHandshake.CERTIFICATE);
            for (CertificateEntry certEnt : certificateMessage.certEntries) {
                certEnt.extensions.consumeOnLoad(chc, enabledExtensions);
            }

            // check server certificate entries
            X509Certificate[] srvCerts =
                    checkServerCerts(chc, certificateMessage.certEntries);

            //
            // update
            //
            chc.handshakeCredentials.add(
                new X509Credentials(srvCerts[0].getPublicKey(), srvCerts));
            chc.handshakeSession.setPeerCertificates(srvCerts);
        }

        private static X509Certificate[] checkClientCerts(
                ServerHandshakeContext shc,
                List<CertificateEntry> certEntries) throws IOException {
            X509Certificate[] certs =
                    new X509Certificate[certEntries.size()];
            try {
                CertificateFactory cf = CertificateFactory.getInstance("X.509");
                int i = 0;
                for (CertificateEntry entry : certEntries) {
                    certs[i++] = (X509Certificate)cf.generateCertificate(
                                    new ByteArrayInputStream(entry.encoded));
                }
            } catch (CertificateException ce) {
                throw shc.conContext.fatal(Alert.BAD_CERTIFICATE,
                    "Failed to parse server certificates", ce);
            }

            // find out the types of client authentication used
            String keyAlgorithm = certs[0].getPublicKey().getAlgorithm();
            String authType;
            switch (keyAlgorithm) {
                case "RSA":
                case "DSA":
                case "EC":
                case "RSASSA-PSS":
                    authType = keyAlgorithm;
                    break;
                default:
                    // unknown public key type
                    authType = "UNKNOWN";
            }

            try {
                X509TrustManager tm = shc.sslContext.getX509TrustManager();
                if (tm instanceof X509ExtendedTrustManager) {
                    if (shc.conContext.transport instanceof SSLEngine) {
                        SSLEngine engine = (SSLEngine)shc.conContext.transport;
                        ((X509ExtendedTrustManager)tm).checkClientTrusted(
                            certs.clone(),
                            authType,
                            engine);
                    } else {
                        SSLSocket socket = (SSLSocket)shc.conContext.transport;
                        ((X509ExtendedTrustManager)tm).checkClientTrusted(
                            certs.clone(),
                            authType,
                            socket);
                    }
                } else {
                    // Unlikely to happen, because we have wrapped the old
                    // X509TrustManager with the new X509ExtendedTrustManager.
                    throw new CertificateException(
                            "Improper X509TrustManager implementation");
                }

                // Once the client certificate chain has been validated, set
                // the certificate chain in the TLS session.
                shc.handshakeSession.setPeerCertificates(certs);
            } catch (CertificateException ce) {
                throw shc.conContext.fatal(Alert.CERTIFICATE_UNKNOWN, ce);
            }

            return certs;
        }

        private static X509Certificate[] checkServerCerts(
                ClientHandshakeContext chc,
                List<CertificateEntry> certEntries) throws IOException {
            X509Certificate[] certs =
                    new X509Certificate[certEntries.size()];
            try {
                CertificateFactory cf = CertificateFactory.getInstance("X.509");
                int i = 0;
                for (CertificateEntry entry : certEntries) {
                    certs[i++] = (X509Certificate)cf.generateCertificate(
                                    new ByteArrayInputStream(entry.encoded));
                }
            } catch (CertificateException ce) {
                throw chc.conContext.fatal(Alert.BAD_CERTIFICATE,
                    "Failed to parse server certificates", ce);
            }

            // find out the types of server authentication used
            //
            // Note that the "UNKNOWN" authentication type is sufficient to
            // check the required digitalSignature KeyUsage for TLS 1.3.
            String authType = "UNKNOWN";

            try {
                X509TrustManager tm = chc.sslContext.getX509TrustManager();
                if (tm instanceof X509ExtendedTrustManager) {
                    if (chc.conContext.transport instanceof SSLEngine) {
                        SSLEngine engine = (SSLEngine)chc.conContext.transport;
                        ((X509ExtendedTrustManager)tm).checkServerTrusted(
                            certs.clone(),
                            authType,
                            engine);
                    } else {
                        SSLSocket socket = (SSLSocket)chc.conContext.transport;
                        ((X509ExtendedTrustManager)tm).checkServerTrusted(
                            certs.clone(),
                            authType,
                            socket);
                    }
                } else {
                    // Unlikely to happen, because we have wrapped the old
                    // X509TrustManager with the new X509ExtendedTrustManager.
                    throw new CertificateException(
                            "Improper X509TrustManager implementation");
                }

                // Once the server certificate chain has been validated, set
                // the certificate chain in the TLS session.
                chc.handshakeSession.setPeerCertificates(certs);
            } catch (CertificateException ce) {
                throw chc.conContext.fatal(getCertificateAlert(chc, ce), ce);
            }

            return certs;
        }

        /**
         * When a failure happens during certificate checking from an
         * {@link X509TrustManager}, determine what TLS alert description
         * to use.
         *
         * @param cexc The exception thrown by the {@link X509TrustManager}
         *
         * @return A byte value corresponding to a TLS alert description number.
         */
        private static Alert getCertificateAlert(
                ClientHandshakeContext chc, CertificateException cexc) {
            // The specific reason for the failure will determine how to
            // set the alert description value
            Alert alert = Alert.CERTIFICATE_UNKNOWN;

            Throwable baseCause = cexc.getCause();
            if (baseCause instanceof CertPathValidatorException) {
                CertPathValidatorException cpve =
                        (CertPathValidatorException)baseCause;
                Reason reason = cpve.getReason();
                if (reason == BasicReason.REVOKED) {
                    alert = chc.staplingActive ?
                            Alert.BAD_CERT_STATUS_RESPONSE :
                            Alert.CERTIFICATE_REVOKED;
                } else if (
                        reason == BasicReason.UNDETERMINED_REVOCATION_STATUS) {
                    alert = chc.staplingActive ?
                            Alert.BAD_CERT_STATUS_RESPONSE :
                            Alert.CERTIFICATE_UNKNOWN;
                }
            }

            return alert;
        }
    }
}
