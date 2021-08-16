/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.X509ExtendedKeyManager;
import javax.security.auth.x500.X500Principal;
import sun.security.ssl.CipherSuite.KeyExchange;
import sun.security.ssl.SSLHandshake.HandshakeMessage;
import sun.security.ssl.X509Authentication.X509Possession;

/**
 * Pack of the CertificateRequest handshake message.
 */
final class CertificateRequest {
    static final SSLConsumer t10HandshakeConsumer =
        new T10CertificateRequestConsumer();
    static final HandshakeProducer t10HandshakeProducer =
        new T10CertificateRequestProducer();

    static final SSLConsumer t12HandshakeConsumer =
        new T12CertificateRequestConsumer();
    static final HandshakeProducer t12HandshakeProducer =
        new T12CertificateRequestProducer();

    static final SSLConsumer t13HandshakeConsumer =
        new T13CertificateRequestConsumer();
    static final HandshakeProducer t13HandshakeProducer =
        new T13CertificateRequestProducer();

    // TLS 1.2 and prior versions
    private static enum ClientCertificateType {
        // RFC 2246
        RSA_SIGN            ((byte)0x01, "rsa_sign", List.of("RSA"), true),
        DSS_SIGN            ((byte)0x02, "dss_sign", List.of("DSA"), true),
        RSA_FIXED_DH        ((byte)0x03, "rsa_fixed_dh"),
        DSS_FIXED_DH        ((byte)0x04, "dss_fixed_dh"),

        // RFC 4346
        RSA_EPHEMERAL_DH    ((byte)0x05, "rsa_ephemeral_dh"),
        DSS_EPHEMERAL_DH    ((byte)0x06, "dss_ephemeral_dh"),
        FORTEZZA_DMS        ((byte)0x14, "fortezza_dms"),

        // RFC 4492 and 8442
        ECDSA_SIGN          ((byte)0x40, "ecdsa_sign",
                                            List.of("EC", "EdDSA"),
                                            JsseJce.isEcAvailable()),
        RSA_FIXED_ECDH      ((byte)0x41, "rsa_fixed_ecdh"),
        ECDSA_FIXED_ECDH    ((byte)0x42, "ecdsa_fixed_ecdh");

        private static final byte[] CERT_TYPES =
                JsseJce.isEcAvailable() ? new byte[] {
                        ECDSA_SIGN.id,
                        RSA_SIGN.id,
                        DSS_SIGN.id
                    } :  new byte[] {
                        RSA_SIGN.id,
                        DSS_SIGN.id
                    };

        final byte id;
        final String name;
        final List<String> keyAlgorithm;
        final boolean isAvailable;

        private ClientCertificateType(byte id, String name) {
            this(id, name, null, false);
        }

        private ClientCertificateType(byte id, String name,
                List<String> keyAlgorithm, boolean isAvailable) {
            this.id = id;
            this.name = name;
            this.keyAlgorithm = keyAlgorithm;
            this.isAvailable = isAvailable;
        }

        private static String nameOf(byte id) {
            for (ClientCertificateType cct : ClientCertificateType.values()) {
                if (cct.id == id) {
                    return cct.name;
                }
            }
            return "UNDEFINED-CLIENT-CERTIFICATE-TYPE(" + (int)id + ")";
        }

        private static ClientCertificateType valueOf(byte id) {
            for (ClientCertificateType cct : ClientCertificateType.values()) {
                if (cct.id == id) {
                    return cct;
                }
            }

            return null;
        }

        private static String[] getKeyTypes(byte[] ids) {
            ArrayList<String> keyTypes = new ArrayList<>(3);
            for (byte id : ids) {
                ClientCertificateType cct = ClientCertificateType.valueOf(id);
                if (cct.isAvailable) {
                    cct.keyAlgorithm.forEach(key -> {
                        if (!keyTypes.contains(key)) {
                            keyTypes.add(key);
                        }
                    });
                }
            }

            return keyTypes.toArray(new String[0]);
        }
    }

    /**
     * The "CertificateRequest" handshake message for SSL 3.0 and TLS 1.0/1.1.
     */
    static final class T10CertificateRequestMessage extends HandshakeMessage {
        final byte[] types;                 // certificate types
        final List<byte[]> authorities;     // certificate authorities

        T10CertificateRequestMessage(HandshakeContext handshakeContext,
                X509Certificate[] trustedCerts, KeyExchange keyExchange) {
            super(handshakeContext);

            this.authorities = new ArrayList<>(trustedCerts.length);
            for (X509Certificate cert : trustedCerts) {
                X500Principal x500Principal = cert.getSubjectX500Principal();
                authorities.add(x500Principal.getEncoded());
            }

            this.types = ClientCertificateType.CERT_TYPES;
        }

        T10CertificateRequestMessage(HandshakeContext handshakeContext,
                ByteBuffer m) throws IOException {
            super(handshakeContext);

            // struct {
            //     ClientCertificateType certificate_types<1..2^8-1>;
            //     DistinguishedName certificate_authorities<0..2^16-1>;
            // } CertificateRequest;
            if (m.remaining() < 4) {
                throw handshakeContext.conContext.fatal(Alert.ILLEGAL_PARAMETER,
                    "Incorrect CertificateRequest message: no sufficient data");
            }
            this.types = Record.getBytes8(m);

            int listLen = Record.getInt16(m);
            if (listLen > m.remaining()) {
                throw handshakeContext.conContext.fatal(Alert.ILLEGAL_PARAMETER,
                    "Incorrect CertificateRequest message:no sufficient data");
            }

            if (listLen > 0) {
                this.authorities = new LinkedList<>();
                while (listLen > 0) {
                    // opaque DistinguishedName<1..2^16-1>;
                    byte[] encoded = Record.getBytes16(m);
                    listLen -= (2 + encoded.length);
                    authorities.add(encoded);
                }
            } else {
                this.authorities = Collections.emptyList();
            }
        }

        String[] getKeyTypes() {
            return  ClientCertificateType.getKeyTypes(types);
        }

        X500Principal[] getAuthorities() {
            X500Principal[] principals = new X500Principal[authorities.size()];
            int i = 0;
            for (byte[] encoded : authorities) {
                principals[i++] = new X500Principal(encoded);
            }

            return principals;
        }

        @Override
        public SSLHandshake handshakeType() {
            return SSLHandshake.CERTIFICATE_REQUEST;
        }

        @Override
        public int messageLength() {
            int len = 1 + types.length + 2;
            for (byte[] encoded : authorities) {
                len += encoded.length + 2;
            }
            return len;
        }

        @Override
        public void send(HandshakeOutStream hos) throws IOException {
            hos.putBytes8(types);

            int listLen = 0;
            for (byte[] encoded : authorities) {
                listLen += encoded.length + 2;
            }

            hos.putInt16(listLen);
            for (byte[] encoded : authorities) {
                hos.putBytes16(encoded);
            }
        }

        @Override
        public String toString() {
            MessageFormat messageFormat = new MessageFormat(
                    "\"CertificateRequest\": '{'\n" +
                    "  \"certificate types\": {0}\n" +
                    "  \"certificate authorities\": {1}\n" +
                    "'}'",
                    Locale.ENGLISH);

            List<String> typeNames = new ArrayList<>(types.length);
            for (byte type : types) {
                typeNames.add(ClientCertificateType.nameOf(type));
            }

            List<String> authorityNames = new ArrayList<>(authorities.size());
            for (byte[] encoded : authorities) {
                X500Principal principal = new X500Principal(encoded);
                authorityNames.add(principal.toString());
            }
            Object[] messageFields = {
                typeNames,
                authorityNames
            };

            return messageFormat.format(messageFields);
        }
    }

    /**
     * The "CertificateRequest" handshake message producer for SSL 3.0 and
     * TLS 1.0/1.1.
     */
    private static final
            class T10CertificateRequestProducer implements HandshakeProducer {
        // Prevent instantiation of this class.
        private T10CertificateRequestProducer() {
            // blank
        }

        @Override
        public byte[] produce(ConnectionContext context,
                HandshakeMessage message) throws IOException {
            // The producing happens in server side only.
            ServerHandshakeContext shc = (ServerHandshakeContext)context;

            X509Certificate[] caCerts =
                    shc.sslContext.getX509TrustManager().getAcceptedIssuers();
            T10CertificateRequestMessage crm = new T10CertificateRequestMessage(
                    shc, caCerts, shc.negotiatedCipherSuite.keyExchange);
            if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                SSLLogger.fine(
                    "Produced CertificateRequest handshake message", crm);
            }

            // Output the handshake message.
            crm.write(shc.handshakeOutput);
            shc.handshakeOutput.flush();

            //
            // update
            //
            shc.handshakeConsumers.put(SSLHandshake.CERTIFICATE.id,
                    SSLHandshake.CERTIFICATE);
            shc.handshakeConsumers.put(SSLHandshake.CERTIFICATE_VERIFY.id,
                    SSLHandshake.CERTIFICATE_VERIFY);

            // The handshake message has been delivered.
            return null;
        }
    }

    /**
     * The "CertificateRequest" handshake message consumer for SSL 3.0 and
     * TLS 1.0/1.1.
     */
    private static final
            class T10CertificateRequestConsumer implements SSLConsumer {
        // Prevent instantiation of this class.
        private T10CertificateRequestConsumer() {
            // blank
        }

        @Override
        public void consume(ConnectionContext context,
                ByteBuffer message) throws IOException {
            // The consuming happens in client side only.
            ClientHandshakeContext chc = (ClientHandshakeContext)context;

            // clean up this consumer
            chc.handshakeConsumers.remove(SSLHandshake.CERTIFICATE_REQUEST.id);

            SSLConsumer certStatCons = chc.handshakeConsumers.remove(
                    SSLHandshake.CERTIFICATE_STATUS.id);
            if (certStatCons != null) {
                // Stapling was active but no certificate status message
                // was sent.  We need to run the absence handler which will
                // check the certificate chain.
                CertificateStatus.handshakeAbsence.absent(context, null);
            }

            T10CertificateRequestMessage crm =
                    new T10CertificateRequestMessage(chc, message);
            if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                SSLLogger.fine(
                        "Consuming CertificateRequest handshake message", crm);
            }

            //
            // validate
            //
            // blank

            //
            // update
            //

            // An empty client Certificate handshake message may be allow.
            chc.handshakeProducers.put(SSLHandshake.CERTIFICATE.id,
                    SSLHandshake.CERTIFICATE);

            X509ExtendedKeyManager km = chc.sslContext.getX509KeyManager();
            String clientAlias = null;
            if (chc.conContext.transport instanceof SSLSocketImpl) {
                clientAlias = km.chooseClientAlias(crm.getKeyTypes(),
                    crm.getAuthorities(), (SSLSocket)chc.conContext.transport);
            } else if (chc.conContext.transport instanceof SSLEngineImpl) {
                clientAlias = km.chooseEngineClientAlias(crm.getKeyTypes(),
                    crm.getAuthorities(), (SSLEngine)chc.conContext.transport);
            }


            if (clientAlias == null) {
                if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                    SSLLogger.warning("No available client authentication");
                }
                return;
            }

            PrivateKey clientPrivateKey = km.getPrivateKey(clientAlias);
            if (clientPrivateKey == null) {
                if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                    SSLLogger.warning("No available client private key");
                }
                return;
            }

            X509Certificate[] clientCerts = km.getCertificateChain(clientAlias);
            if ((clientCerts == null) || (clientCerts.length == 0)) {
                if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                    SSLLogger.warning("No available client certificate");
                }
                return;
            }

            chc.handshakePossessions.add(
                    new X509Possession(clientPrivateKey, clientCerts));
            chc.handshakeProducers.put(SSLHandshake.CERTIFICATE_VERIFY.id,
                    SSLHandshake.CERTIFICATE_VERIFY);
        }
    }

    /**
     * The CertificateRequest handshake message for TLS 1.2.
     */
    static final class T12CertificateRequestMessage extends HandshakeMessage {
        final byte[] types;                 // certificate types
        final int[] algorithmIds;           // supported signature algorithms
        final List<byte[]> authorities;     // certificate authorities

        T12CertificateRequestMessage(HandshakeContext handshakeContext,
                X509Certificate[] trustedCerts, KeyExchange keyExchange,
                List<SignatureScheme> signatureSchemes) throws IOException {
            super(handshakeContext);

            this.types = ClientCertificateType.CERT_TYPES;

            if (signatureSchemes == null || signatureSchemes.isEmpty()) {
                throw handshakeContext.conContext.fatal(Alert.ILLEGAL_PARAMETER,
                        "No signature algorithms specified for " +
                        "CertificateRequest hanshake message");
            }
            this.algorithmIds = new int[signatureSchemes.size()];
            int i = 0;
            for (SignatureScheme scheme : signatureSchemes) {
                algorithmIds[i++] = scheme.id;
            }

            this.authorities = new ArrayList<>(trustedCerts.length);
            for (X509Certificate cert : trustedCerts) {
                X500Principal x500Principal = cert.getSubjectX500Principal();
                authorities.add(x500Principal.getEncoded());
            }
        }

        T12CertificateRequestMessage(HandshakeContext handshakeContext,
                ByteBuffer m) throws IOException {
            super(handshakeContext);

            // struct {
            //     ClientCertificateType certificate_types<1..2^8-1>;
            //     SignatureAndHashAlgorithm
            //       supported_signature_algorithms<2..2^16-2>;
            //     DistinguishedName certificate_authorities<0..2^16-1>;
            // } CertificateRequest;

            // certificate_authorities
            if (m.remaining() < 8) {
                throw handshakeContext.conContext.fatal(Alert.ILLEGAL_PARAMETER,
                        "Invalid CertificateRequest handshake message: " +
                        "no sufficient data");
            }
            this.types = Record.getBytes8(m);

            // supported_signature_algorithms
            if (m.remaining() < 6) {
                throw handshakeContext.conContext.fatal(Alert.ILLEGAL_PARAMETER,
                        "Invalid CertificateRequest handshake message: " +
                        "no sufficient data");
            }

            byte[] algs = Record.getBytes16(m);
            if (algs == null || algs.length == 0 || (algs.length & 0x01) != 0) {
                throw handshakeContext.conContext.fatal(Alert.ILLEGAL_PARAMETER,
                        "Invalid CertificateRequest handshake message: " +
                        "incomplete signature algorithms");
            }

            this.algorithmIds = new int[(algs.length >> 1)];
            for (int i = 0, j = 0; i < algs.length;) {
                byte hash = algs[i++];
                byte sign = algs[i++];
                algorithmIds[j++] = ((hash & 0xFF) << 8) | (sign & 0xFF);
            }

            // certificate_authorities
            if (m.remaining() < 2) {
                throw handshakeContext.conContext.fatal(Alert.ILLEGAL_PARAMETER,
                        "Invalid CertificateRequest handshake message: " +
                        "no sufficient data");
            }

            int listLen = Record.getInt16(m);
            if (listLen > m.remaining()) {
                throw handshakeContext.conContext.fatal(Alert.ILLEGAL_PARAMETER,
                    "Invalid CertificateRequest message: no sufficient data");
            }

            if (listLen > 0) {
                this.authorities = new LinkedList<>();
                while (listLen > 0) {
                    // opaque DistinguishedName<1..2^16-1>;
                    byte[] encoded = Record.getBytes16(m);
                    listLen -= (2 + encoded.length);
                    authorities.add(encoded);
                }
            } else {
                this.authorities = Collections.emptyList();
            }
        }

        String[] getKeyTypes() {
            return ClientCertificateType.getKeyTypes(types);
        }

        X500Principal[] getAuthorities() {
            X500Principal[] principals = new X500Principal[authorities.size()];
            int i = 0;
            for (byte[] encoded : authorities) {
                principals[i++] = new X500Principal(encoded);
            }

            return principals;
        }

        @Override
        public SSLHandshake handshakeType() {
            return SSLHandshake.CERTIFICATE_REQUEST;
        }

        @Override
        public int messageLength() {
            int len = 1 + types.length + 2 + (algorithmIds.length << 1) + 2;
            for (byte[] encoded : authorities) {
                len += encoded.length + 2;
            }
            return len;
        }

        @Override
        public void send(HandshakeOutStream hos) throws IOException {
            hos.putBytes8(types);

            int listLen = 0;
            for (byte[] encoded : authorities) {
                listLen += encoded.length + 2;
            }

            hos.putInt16(algorithmIds.length << 1);
            for (int algorithmId : algorithmIds) {
                hos.putInt16(algorithmId);
            }

            hos.putInt16(listLen);
            for (byte[] encoded : authorities) {
                hos.putBytes16(encoded);
            }
        }

        @Override
        public String toString() {
            MessageFormat messageFormat = new MessageFormat(
                    "\"CertificateRequest\": '{'\n" +
                    "  \"certificate types\": {0}\n" +
                    "  \"supported signature algorithms\": {1}\n" +
                    "  \"certificate authorities\": {2}\n" +
                    "'}'",
                    Locale.ENGLISH);

            List<String> typeNames = new ArrayList<>(types.length);
            for (byte type : types) {
                typeNames.add(ClientCertificateType.nameOf(type));
            }

            List<String> algorithmNames = new ArrayList<>(algorithmIds.length);
            for (int algorithmId : algorithmIds) {
                algorithmNames.add(SignatureScheme.nameOf(algorithmId));
            }

            List<String> authorityNames = new ArrayList<>(authorities.size());
            for (byte[] encoded : authorities) {
                X500Principal principal = new X500Principal(encoded);
                authorityNames.add(principal.toString());
            }
            Object[] messageFields = {
                typeNames,
                algorithmNames,
                authorityNames
            };

            return messageFormat.format(messageFields);
        }
    }

    /**
     * The "CertificateRequest" handshake message producer for TLS 1.2.
     */
    private static final
            class T12CertificateRequestProducer implements HandshakeProducer {
        // Prevent instantiation of this class.
        private T12CertificateRequestProducer() {
            // blank
        }

        @Override
        public byte[] produce(ConnectionContext context,
                HandshakeMessage message) throws IOException {
            // The producing happens in server side only.
            ServerHandshakeContext shc = (ServerHandshakeContext)context;
            if (shc.localSupportedSignAlgs == null) {
                shc.localSupportedSignAlgs =
                    SignatureScheme.getSupportedAlgorithms(
                            shc.sslConfig,
                            shc.algorithmConstraints, shc.activeProtocols);
            }

            if (shc.localSupportedSignAlgs == null ||
                    shc.localSupportedSignAlgs.isEmpty()) {
                throw shc.conContext.fatal(Alert.HANDSHAKE_FAILURE,
                    "No supported signature algorithm");
            }

            X509Certificate[] caCerts =
                    shc.sslContext.getX509TrustManager().getAcceptedIssuers();
            T12CertificateRequestMessage crm = new T12CertificateRequestMessage(
                    shc, caCerts, shc.negotiatedCipherSuite.keyExchange,
                    shc.localSupportedSignAlgs);
            if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                SSLLogger.fine(
                    "Produced CertificateRequest handshake message", crm);
            }

            // Output the handshake message.
            crm.write(shc.handshakeOutput);
            shc.handshakeOutput.flush();

            //
            // update
            //
            shc.handshakeConsumers.put(SSLHandshake.CERTIFICATE.id,
                    SSLHandshake.CERTIFICATE);
            shc.handshakeConsumers.put(SSLHandshake.CERTIFICATE_VERIFY.id,
                    SSLHandshake.CERTIFICATE_VERIFY);

            // The handshake message has been delivered.
            return null;
        }
    }

    /**
     * The "CertificateRequest" handshake message consumer for TLS 1.2.
     */
    private static final
            class T12CertificateRequestConsumer implements SSLConsumer {
        // Prevent instantiation of this class.
        private T12CertificateRequestConsumer() {
            // blank
        }

        @Override
        public void consume(ConnectionContext context,
                ByteBuffer message) throws IOException {
            // The consuming happens in client side only.
            ClientHandshakeContext chc = (ClientHandshakeContext)context;

            // clean up this consumer
            chc.handshakeConsumers.remove(SSLHandshake.CERTIFICATE_REQUEST.id);

            SSLConsumer certStatCons = chc.handshakeConsumers.remove(
                    SSLHandshake.CERTIFICATE_STATUS.id);
            if (certStatCons != null) {
                // Stapling was active but no certificate status message
                // was sent.  We need to run the absence handler which will
                // check the certificate chain.
                CertificateStatus.handshakeAbsence.absent(context, null);
            }

            T12CertificateRequestMessage crm =
                    new T12CertificateRequestMessage(chc, message);
            if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                SSLLogger.fine(
                        "Consuming CertificateRequest handshake message", crm);
            }

            //
            // validate
            //
            // blank

            //
            // update
            //

            // An empty client Certificate handshake message may be allow.
            chc.handshakeProducers.put(SSLHandshake.CERTIFICATE.id,
                    SSLHandshake.CERTIFICATE);

            List<SignatureScheme> sss =
                    SignatureScheme.getSupportedAlgorithms(
                            chc.sslConfig,
                            chc.algorithmConstraints, chc.negotiatedProtocol,
                            crm.algorithmIds);
            if (sss == null || sss.isEmpty()) {
                throw chc.conContext.fatal(Alert.HANDSHAKE_FAILURE,
                        "No supported signature algorithm");
            }

            chc.peerRequestedSignatureSchemes = sss;
            chc.peerRequestedCertSignSchemes = sss;     // use the same schemes
            chc.handshakeSession.setPeerSupportedSignatureAlgorithms(sss);
            chc.peerSupportedAuthorities = crm.getAuthorities();

            // For TLS 1.2, we no longer use the certificate_types field
            // from the CertificateRequest message to directly determine
            // the SSLPossession.  Instead, the choosePossession method
            // will use the accepted signature schemes in the message to
            // determine the set of acceptable certificate types to select from.
            SSLPossession pos = choosePossession(chc);
            if (pos == null) {
                return;
            }

            chc.handshakePossessions.add(pos);
            chc.handshakeProducers.put(SSLHandshake.CERTIFICATE_VERIFY.id,
                    SSLHandshake.CERTIFICATE_VERIFY);
        }

        private static SSLPossession choosePossession(HandshakeContext hc)
                throws IOException {
            if (hc.peerRequestedCertSignSchemes == null ||
                    hc.peerRequestedCertSignSchemes.isEmpty()) {
                if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                    SSLLogger.warning("No signature and hash algorithms " +
                            "in CertificateRequest");
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
    }

    /**
     * The CertificateRequest handshake message for TLS 1.3.
     */
    static final class T13CertificateRequestMessage extends HandshakeMessage {
        private final byte[] requestContext;
        private final SSLExtensions extensions;

        T13CertificateRequestMessage(
                HandshakeContext handshakeContext) throws IOException {
            super(handshakeContext);

            this.requestContext = new byte[0];
            this.extensions = new SSLExtensions(this);
        }

        T13CertificateRequestMessage(HandshakeContext handshakeContext,
                ByteBuffer m) throws IOException {
            super(handshakeContext);

            // struct {
            //      opaque certificate_request_context<0..2^8-1>;
            //      Extension extensions<2..2^16-1>;
            //  } CertificateRequest;
            if (m.remaining() < 5) {
                throw handshakeContext.conContext.fatal(Alert.ILLEGAL_PARAMETER,
                        "Invalid CertificateRequest handshake message: " +
                        "no sufficient data");
            }
            this.requestContext = Record.getBytes8(m);

            if (m.remaining() < 4) {
                throw handshakeContext.conContext.fatal(Alert.ILLEGAL_PARAMETER,
                        "Invalid CertificateRequest handshake message: " +
                        "no sufficient extensions data");
            }
            SSLExtension[] enabledExtensions =
                handshakeContext.sslConfig.getEnabledExtensions(
                        SSLHandshake.CERTIFICATE_REQUEST);
            this.extensions = new SSLExtensions(this, m, enabledExtensions);
        }

        @Override
        SSLHandshake handshakeType() {
            return SSLHandshake.CERTIFICATE_REQUEST;
        }

        @Override
        int messageLength() {
            // In TLS 1.3, use of certain extensions is mandatory.
            return 1 + requestContext.length + extensions.length();
        }

        @Override
        void send(HandshakeOutStream hos) throws IOException {
            hos.putBytes8(requestContext);

            // In TLS 1.3, use of certain extensions is mandatory.
            extensions.send(hos);
        }

        @Override
        public String toString() {
            MessageFormat messageFormat = new MessageFormat(
                "\"CertificateRequest\": '{'\n" +
                "  \"certificate_request_context\": \"{0}\",\n" +
                "  \"extensions\": [\n" +
                "{1}\n" +
                "  ]\n" +
                "'}'",
                Locale.ENGLISH);
            Object[] messageFields = {
                Utilities.toHexString(requestContext),
                Utilities.indent(Utilities.indent(extensions.toString()))
            };

            return messageFormat.format(messageFields);
        }
    }

    /**
     * The "CertificateRequest" handshake message producer for TLS 1.3.
     */
    private static final
            class T13CertificateRequestProducer implements HandshakeProducer {
        // Prevent instantiation of this class.
        private T13CertificateRequestProducer() {
            // blank
        }

        @Override
        public byte[] produce(ConnectionContext context,
                HandshakeMessage message) throws IOException {
            // The producing happens in server side only.
            ServerHandshakeContext shc = (ServerHandshakeContext)context;

            T13CertificateRequestMessage crm =
                    new T13CertificateRequestMessage(shc);
            // Produce extensions for CertificateRequest handshake message.
            SSLExtension[] extTypes = shc.sslConfig.getEnabledExtensions(
                    SSLHandshake.CERTIFICATE_REQUEST, shc.negotiatedProtocol);
            crm.extensions.produce(shc, extTypes);
            if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                SSLLogger.fine("Produced CertificateRequest message", crm);
            }

            // Output the handshake message.
            crm.write(shc.handshakeOutput);
            shc.handshakeOutput.flush();

            //
            // update
            //
            shc.certRequestContext = crm.requestContext.clone();
            shc.handshakeConsumers.put(SSLHandshake.CERTIFICATE.id,
                    SSLHandshake.CERTIFICATE);
            shc.handshakeConsumers.put(SSLHandshake.CERTIFICATE_VERIFY.id,
                    SSLHandshake.CERTIFICATE_VERIFY);

            // The handshake message has been delivered.
            return null;
        }
    }

    /**
     * The "CertificateRequest" handshake message consumer for TLS 1.3.
     */
    private static final
            class T13CertificateRequestConsumer implements SSLConsumer {
        // Prevent instantiation of this class.
        private T13CertificateRequestConsumer() {
            // blank
        }

        @Override
        public void consume(ConnectionContext context,
                ByteBuffer message) throws IOException {
            // The consuming happens in client side only.
            ClientHandshakeContext chc = (ClientHandshakeContext)context;

            // clean up this consumer
            chc.handshakeConsumers.remove(SSLHandshake.CERTIFICATE_REQUEST.id);

            T13CertificateRequestMessage crm =
                    new T13CertificateRequestMessage(chc, message);
            if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                SSLLogger.fine(
                        "Consuming CertificateRequest handshake message", crm);
            }

            //
            // validate
            //
            SSLExtension[] extTypes = chc.sslConfig.getEnabledExtensions(
                    SSLHandshake.CERTIFICATE_REQUEST);
            crm.extensions.consumeOnLoad(chc, extTypes);

            //
            // update
            //
            crm.extensions.consumeOnTrade(chc, extTypes);

            //
            // produce
            //
            chc.certRequestContext = crm.requestContext.clone();
            chc.handshakeProducers.put(SSLHandshake.CERTIFICATE.id,
                    SSLHandshake.CERTIFICATE);
            chc.handshakeProducers.put(SSLHandshake.CERTIFICATE_VERIFY.id,
                    SSLHandshake.CERTIFICATE_VERIFY);
        }
    }
}
