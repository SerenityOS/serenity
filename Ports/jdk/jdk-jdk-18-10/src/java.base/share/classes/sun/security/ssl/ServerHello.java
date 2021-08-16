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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.AlgorithmConstraints;
import java.security.CryptoPrimitive;
import java.security.GeneralSecurityException;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HexFormat;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLProtocolException;
import sun.security.ssl.CipherSuite.KeyExchange;
import sun.security.ssl.ClientHello.ClientHelloMessage;
import sun.security.ssl.SSLCipher.SSLReadCipher;
import sun.security.ssl.SSLCipher.SSLWriteCipher;
import sun.security.ssl.SSLHandshake.HandshakeMessage;
import sun.security.ssl.SupportedVersionsExtension.SHSupportedVersionsSpec;

/**
 * Pack of the ServerHello/HelloRetryRequest handshake message.
 */
final class ServerHello {
    static final SSLConsumer handshakeConsumer =
        new ServerHelloConsumer();
    static final HandshakeProducer t12HandshakeProducer =
        new T12ServerHelloProducer();
    static final HandshakeProducer t13HandshakeProducer =
        new T13ServerHelloProducer();
    static final HandshakeProducer hrrHandshakeProducer =
        new T13HelloRetryRequestProducer();

    static final HandshakeProducer hrrReproducer =
        new T13HelloRetryRequestReproducer();

    private static final HandshakeConsumer t12HandshakeConsumer =
        new T12ServerHelloConsumer();
    private static final HandshakeConsumer t13HandshakeConsumer =
        new T13ServerHelloConsumer();

    private static final HandshakeConsumer d12HandshakeConsumer =
        new T12ServerHelloConsumer();
    private static final HandshakeConsumer d13HandshakeConsumer =
        new T13ServerHelloConsumer();

    private static final HandshakeConsumer t13HrrHandshakeConsumer =
        new T13HelloRetryRequestConsumer();
    private static final HandshakeConsumer d13HrrHandshakeConsumer =
        new T13HelloRetryRequestConsumer();

    /**
     * The ServerHello handshake message.
     */
    static final class ServerHelloMessage extends HandshakeMessage {
        final ProtocolVersion           serverVersion;      // TLS 1.3 legacy
        final RandomCookie              serverRandom;
        final SessionId                 sessionId;          // TLS 1.3 legacy
        final CipherSuite               cipherSuite;
        final byte                      compressionMethod;  // TLS 1.3 legacy
        final SSLExtensions             extensions;

        // The HelloRetryRequest producer needs to use the ClientHello message
        // for cookie generation.  Please don't use this field for other
        // purpose unless it is really necessary.
        final ClientHelloMessage        clientHello;

        // Reserved for HelloRetryRequest consumer.  Please don't use this
        // field for other purpose unless it is really necessary.
        final ByteBuffer                handshakeRecord;

        ServerHelloMessage(HandshakeContext context,
                ProtocolVersion serverVersion, SessionId sessionId,
                CipherSuite cipherSuite, RandomCookie serverRandom,
                ClientHelloMessage clientHello) {
            super(context);

            this.serverVersion = serverVersion;
            this.serverRandom = serverRandom;
            this.sessionId = sessionId;
            this.cipherSuite = cipherSuite;
            this.compressionMethod = 0x00;      // Don't support compression.
            this.extensions = new SSLExtensions(this);

            // Reserve the ClientHello message for cookie generation.
            this.clientHello = clientHello;

            // The handshakeRecord field is used for HelloRetryRequest consumer
            // only.  It's fine to set it to null for generating side of the
            // ServerHello/HelloRetryRequest message.
            this.handshakeRecord = null;
        }

        ServerHelloMessage(HandshakeContext context,
                ByteBuffer m) throws IOException {
            super(context);

            // Reserve for HelloRetryRequest consumer if needed.
            this.handshakeRecord = m.duplicate();

            byte major = m.get();
            byte minor = m.get();
            this.serverVersion = ProtocolVersion.valueOf(major, minor);
            if (this.serverVersion == null) {
                // The client should only request for known protocol versions.
                throw context.conContext.fatal(Alert.PROTOCOL_VERSION,
                    "Unsupported protocol version: " +
                    ProtocolVersion.nameOf(major, minor));
            }

            this.serverRandom = new RandomCookie(m);
            this.sessionId = new SessionId(Record.getBytes8(m));
            try {
                sessionId.checkLength(serverVersion.id);
            } catch (SSLProtocolException ex) {
                throw handshakeContext.conContext.fatal(
                        Alert.ILLEGAL_PARAMETER, ex);
            }

            int cipherSuiteId = Record.getInt16(m);
            this.cipherSuite = CipherSuite.valueOf(cipherSuiteId);
            if (cipherSuite == null || !context.isNegotiable(cipherSuite)) {
                throw context.conContext.fatal(Alert.ILLEGAL_PARAMETER,
                    "Server selected improper ciphersuite " +
                    CipherSuite.nameOf(cipherSuiteId));
            }

            this.compressionMethod = m.get();
            if (compressionMethod != 0) {
                throw context.conContext.fatal(Alert.ILLEGAL_PARAMETER,
                    "compression type not supported, " + compressionMethod);
            }

            SSLExtension[] supportedExtensions;
            if (serverRandom.isHelloRetryRequest()) {
                supportedExtensions = context.sslConfig.getEnabledExtensions(
                            SSLHandshake.HELLO_RETRY_REQUEST);
            } else {
                supportedExtensions = context.sslConfig.getEnabledExtensions(
                            SSLHandshake.SERVER_HELLO);
            }

            if (m.hasRemaining()) {
                this.extensions =
                    new SSLExtensions(this, m, supportedExtensions);
            } else {
                this.extensions = new SSLExtensions(this);
            }

            // The clientHello field is used for HelloRetryRequest producer
            // only.  It's fine to set it to null for receiving side of
            // ServerHello/HelloRetryRequest message.
            this.clientHello = null;        // not used, let it be null;
        }

        @Override
        public SSLHandshake handshakeType() {
            return serverRandom.isHelloRetryRequest() ?
                SSLHandshake.HELLO_RETRY_REQUEST : SSLHandshake.SERVER_HELLO;
        }

        @Override
        public int messageLength() {
            // almost fixed header size, except session ID and extensions:
            //      major + minor = 2
            //      random = 32
            //      session ID len field = 1
            //      cipher suite = 2
            //      compression = 1
            //      extensions: if present, 2 + length of extensions
            // In TLS 1.3, use of certain extensions is mandatory.
            return 38 + sessionId.length() + extensions.length();
        }

        @Override
        public void send(HandshakeOutStream hos) throws IOException {
            hos.putInt8(serverVersion.major);
            hos.putInt8(serverVersion.minor);
            hos.write(serverRandom.randomBytes);
            hos.putBytes8(sessionId.getId());
            hos.putInt8((cipherSuite.id >> 8) & 0xFF);
            hos.putInt8(cipherSuite.id & 0xff);
            hos.putInt8(compressionMethod);

            extensions.send(hos);           // In TLS 1.3, use of certain
                                            // extensions is mandatory.
        }

        @Override
        public String toString() {
            MessageFormat messageFormat = new MessageFormat(
                "\"{0}\": '{'\n" +
                "  \"server version\"      : \"{1}\",\n" +
                "  \"random\"              : \"{2}\",\n" +
                "  \"session id\"          : \"{3}\",\n" +
                "  \"cipher suite\"        : \"{4}\",\n" +
                "  \"compression methods\" : \"{5}\",\n" +
                "  \"extensions\"          : [\n" +
                "{6}\n" +
                "  ]\n" +
                "'}'",
                Locale.ENGLISH);
            Object[] messageFields = {
                serverRandom.isHelloRetryRequest() ?
                    "HelloRetryRequest" : "ServerHello",
                serverVersion.name,
                Utilities.toHexString(serverRandom.randomBytes),
                sessionId.toString(),
                cipherSuite.name + "(" + Utilities.byte16HexString(cipherSuite.id) + ")",
                HexFormat.of().toHexDigits(compressionMethod),
                Utilities.indent(extensions.toString(), "    ")
            };

            return messageFormat.format(messageFields);
        }
    }

    /**
     * The "ServerHello" handshake message producer.
     */
    private static final class T12ServerHelloProducer
            implements HandshakeProducer {

        // Prevent instantiation of this class.
        private T12ServerHelloProducer() {
            // blank
        }

        @Override
        public byte[] produce(ConnectionContext context,
                HandshakeMessage message) throws IOException {
            // The producing happens in server side only.
            ServerHandshakeContext shc = (ServerHandshakeContext)context;
            ClientHelloMessage clientHello = (ClientHelloMessage)message;

            // If client hasn't specified a session we can resume, start a
            // new one and choose its cipher suite and compression options,
            // unless new session creation is disabled for this connection!
            if (!shc.isResumption || shc.resumingSession == null) {
                if (!shc.sslConfig.enableSessionCreation) {
                    throw new SSLException(
                        "Not resumption, and no new session is allowed");
                }

                if (shc.localSupportedSignAlgs == null) {
                    shc.localSupportedSignAlgs =
                        SignatureScheme.getSupportedAlgorithms(
                                shc.sslConfig,
                                shc.algorithmConstraints, shc.activeProtocols);
                }

                SSLSessionImpl session =
                        new SSLSessionImpl(shc, CipherSuite.C_NULL);
                session.setMaximumPacketSize(shc.sslConfig.maximumPacketSize);
                shc.handshakeSession = session;

                // consider the handshake extension impact
                SSLExtension[] enabledExtensions =
                        shc.sslConfig.getEnabledExtensions(
                            SSLHandshake.CLIENT_HELLO, shc.negotiatedProtocol);
                clientHello.extensions.consumeOnTrade(shc, enabledExtensions);

                // negotiate the cipher suite.
                KeyExchangeProperties credentials =
                        chooseCipherSuite(shc, clientHello);
                if (credentials == null) {
                    throw shc.conContext.fatal(Alert.HANDSHAKE_FAILURE,
                            "no cipher suites in common");
                }
                shc.negotiatedCipherSuite = credentials.cipherSuite;
                shc.handshakeKeyExchange = credentials.keyExchange;
                shc.handshakeSession.setSuite(credentials.cipherSuite);
                shc.handshakePossessions.addAll(
                        Arrays.asList(credentials.possessions));
                shc.handshakeHash.determine(
                        shc.negotiatedProtocol, shc.negotiatedCipherSuite);

                // Check the incoming OCSP stapling extensions and attempt
                // to get responses.  If the resulting stapleParams is non
                // null, it implies that stapling is enabled on the server side.
                shc.stapleParams = StatusResponseManager.processStapling(shc);
                shc.staplingActive = (shc.stapleParams != null);

                // update the responders
                SSLKeyExchange ke = credentials.keyExchange;
                if (ke != null) {
                    for (Map.Entry<Byte, HandshakeProducer> me :
                            ke.getHandshakeProducers(shc)) {
                        shc.handshakeProducers.put(
                                me.getKey(), me.getValue());
                    }
                }

                if ((ke != null) &&
                        (shc.sslConfig.clientAuthType !=
                                ClientAuthType.CLIENT_AUTH_NONE) &&
                        !shc.negotiatedCipherSuite.isAnonymous()) {
                    for (SSLHandshake hs :
                            ke.getRelatedHandshakers(shc)) {
                        if (hs == SSLHandshake.CERTIFICATE) {
                            shc.handshakeProducers.put(
                                    SSLHandshake.CERTIFICATE_REQUEST.id,
                                    SSLHandshake.CERTIFICATE_REQUEST);
                            break;
                        }
                    }
                }
                shc.handshakeProducers.put(SSLHandshake.SERVER_HELLO_DONE.id,
                        SSLHandshake.SERVER_HELLO_DONE);
            } else {
                // stateless and use the client session id (RFC 5077 3.4)
                if (shc.statelessResumption) {
                    shc.resumingSession = new SSLSessionImpl(shc.resumingSession,
                            (clientHello.sessionId.length() == 0) ?
                                    new SessionId(true,
                                            shc.sslContext.getSecureRandom()) :
                                    new SessionId(clientHello.sessionId.getId())
                    );
                }
                shc.handshakeSession = shc.resumingSession;
                shc.negotiatedProtocol =
                        shc.resumingSession.getProtocolVersion();
                shc.negotiatedCipherSuite = shc.resumingSession.getSuite();
                shc.handshakeHash.determine(
                        shc.negotiatedProtocol, shc.negotiatedCipherSuite);
            }

            // Generate the ServerHello handshake message.
            ServerHelloMessage shm = new ServerHelloMessage(shc,
                    shc.negotiatedProtocol,
                    shc.handshakeSession.getSessionId(),
                    shc.negotiatedCipherSuite,
                    new RandomCookie(shc),
                    clientHello);
            shc.serverHelloRandom = shm.serverRandom;

            // Produce extensions for ServerHello handshake message.
            SSLExtension[] serverHelloExtensions =
                shc.sslConfig.getEnabledExtensions(
                        SSLHandshake.SERVER_HELLO, shc.negotiatedProtocol);
            shm.extensions.produce(shc, serverHelloExtensions);
            if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                SSLLogger.fine("Produced ServerHello handshake message", shm);
            }

            // Output the handshake message.
            shm.write(shc.handshakeOutput);
            shc.handshakeOutput.flush();

            if (shc.isResumption && shc.resumingSession != null) {
                SSLTrafficKeyDerivation kdg =
                    SSLTrafficKeyDerivation.valueOf(shc.negotiatedProtocol);
                if (kdg == null) {
                    // unlikely
                    throw shc.conContext.fatal(Alert.INTERNAL_ERROR,
                            "Not supported key derivation: " +
                            shc.negotiatedProtocol);
                } else {
                    shc.handshakeKeyDerivation = kdg.createKeyDerivation(
                            shc, shc.resumingSession.getMasterSecret());
                }

                // update the responders
                shc.handshakeProducers.put(SSLHandshake.FINISHED.id,
                        SSLHandshake.FINISHED);
            }

            // The handshake message has been delivered.
            return null;
        }

        private static KeyExchangeProperties chooseCipherSuite(
                ServerHandshakeContext shc,
                ClientHelloMessage clientHello) throws IOException {
            List<CipherSuite> preferred;
            List<CipherSuite> proposed;
            if (shc.sslConfig.preferLocalCipherSuites) {
                preferred = shc.activeCipherSuites;
                proposed = clientHello.cipherSuites;
            } else {
                preferred = clientHello.cipherSuites;
                proposed = shc.activeCipherSuites;
            }

            List<CipherSuite> legacySuites = new LinkedList<>();
            for (CipherSuite cs : preferred) {
                if (!HandshakeContext.isNegotiable(
                        proposed, shc.negotiatedProtocol, cs)) {
                    continue;
                }

                if (shc.sslConfig.clientAuthType ==
                        ClientAuthType.CLIENT_AUTH_REQUIRED) {
                    if ((cs.keyExchange == KeyExchange.K_DH_ANON) ||
                        (cs.keyExchange == KeyExchange.K_ECDH_ANON)) {
                        continue;
                    }
                }

                SSLKeyExchange ke = SSLKeyExchange.valueOf(
                        cs.keyExchange, shc.negotiatedProtocol);
                if (ke == null) {
                    continue;
                }
                if (!ServerHandshakeContext.legacyAlgorithmConstraints.permits(
                        EnumSet.of(CryptoPrimitive.KEY_AGREEMENT), cs.name, null)) {
                    legacySuites.add(cs);
                    continue;
                }

                SSLPossession[] hcds = ke.createPossessions(shc);
                if ((hcds == null) || (hcds.length == 0)) {
                    continue;
                }

                // The cipher suite has been negotiated.
                if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                    SSLLogger.fine("use cipher suite " + cs.name);
                }

                return new KeyExchangeProperties(cs, ke, hcds);
            }

            for (CipherSuite cs : legacySuites) {
                SSLKeyExchange ke = SSLKeyExchange.valueOf(
                        cs.keyExchange,  shc.negotiatedProtocol);
                if (ke != null) {
                    SSLPossession[] hcds = ke.createPossessions(shc);
                    if ((hcds != null) && (hcds.length != 0)) {
                        if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                            SSLLogger.warning(
                                "use legacy cipher suite " + cs.name);
                        }
                        return new KeyExchangeProperties(cs, ke, hcds);
                    }
                }
            }

            throw shc.conContext.fatal(Alert.HANDSHAKE_FAILURE,
                    "no cipher suites in common");
        }

        private static final class KeyExchangeProperties {
            final CipherSuite cipherSuite;
            final SSLKeyExchange keyExchange;
            final SSLPossession[] possessions;

            private KeyExchangeProperties(CipherSuite cipherSuite,
                    SSLKeyExchange keyExchange, SSLPossession[] possessions) {
                this.cipherSuite = cipherSuite;
                this.keyExchange = keyExchange;
                this.possessions = possessions;
            }
        }
    }

    /**
     * The "ServerHello" handshake message producer.
     */
    private static final
            class T13ServerHelloProducer implements HandshakeProducer {
        // Prevent instantiation of this class.
        private T13ServerHelloProducer() {
            // blank
        }

        @Override
        public byte[] produce(ConnectionContext context,
                HandshakeMessage message) throws IOException {
            // The producing happens in server side only.
            ServerHandshakeContext shc = (ServerHandshakeContext)context;
            ClientHelloMessage clientHello = (ClientHelloMessage)message;

            SSLSessionContextImpl sessionCache = (SSLSessionContextImpl)
                    shc.sslContext.engineGetServerSessionContext();

            // If client hasn't specified a session we can resume, start a
            // new one and choose its cipher suite and compression options,
            // unless new session creation is disabled for this connection!
            if (!shc.isResumption || shc.resumingSession == null) {
                if (!shc.sslConfig.enableSessionCreation) {
                    throw new SSLException(
                        "Not resumption, and no new session is allowed");
                }

                if (shc.localSupportedSignAlgs == null) {
                    shc.localSupportedSignAlgs =
                        SignatureScheme.getSupportedAlgorithms(
                                shc.sslConfig,
                                shc.algorithmConstraints, shc.activeProtocols);
                }

                SSLSessionImpl session =
                        new SSLSessionImpl(shc, CipherSuite.C_NULL);
                session.setMaximumPacketSize(shc.sslConfig.maximumPacketSize);
                shc.handshakeSession = session;

                // consider the handshake extension impact
                SSLExtension[] enabledExtensions =
                        shc.sslConfig.getEnabledExtensions(
                            SSLHandshake.CLIENT_HELLO, shc.negotiatedProtocol);
                clientHello.extensions.consumeOnTrade(shc, enabledExtensions);

                // negotiate the cipher suite.
                CipherSuite cipherSuite = chooseCipherSuite(shc, clientHello);
                if (cipherSuite == null) {
                    throw shc.conContext.fatal(Alert.HANDSHAKE_FAILURE,
                            "no cipher suites in common");
                }
                shc.negotiatedCipherSuite = cipherSuite;
                shc.handshakeSession.setSuite(cipherSuite);
                shc.handshakeHash.determine(
                        shc.negotiatedProtocol, shc.negotiatedCipherSuite);
            } else {
                shc.handshakeSession = shc.resumingSession;

                // consider the handshake extension impact
                SSLExtension[] enabledExtensions =
                shc.sslConfig.getEnabledExtensions(
                SSLHandshake.CLIENT_HELLO, shc.negotiatedProtocol);
                clientHello.extensions.consumeOnTrade(shc, enabledExtensions);

                shc.negotiatedProtocol =
                        shc.resumingSession.getProtocolVersion();
                shc.negotiatedCipherSuite = shc.resumingSession.getSuite();
                shc.handshakeHash.determine(
                        shc.negotiatedProtocol, shc.negotiatedCipherSuite);

                setUpPskKD(shc,
                        shc.resumingSession.consumePreSharedKey());
            }

            // update the responders
            shc.handshakeProducers.put(SSLHandshake.ENCRYPTED_EXTENSIONS.id,
                    SSLHandshake.ENCRYPTED_EXTENSIONS);
            shc.handshakeProducers.put(SSLHandshake.FINISHED.id,
                    SSLHandshake.FINISHED);

            // Generate the ServerHello handshake message.
            ServerHelloMessage shm = new ServerHelloMessage(shc,
                    ProtocolVersion.TLS12,      // use legacy version
                    clientHello.sessionId,      // echo back
                    shc.negotiatedCipherSuite,
                    new RandomCookie(shc),
                    clientHello);
            shc.serverHelloRandom = shm.serverRandom;

            // Produce extensions for ServerHello handshake message.
            SSLExtension[] serverHelloExtensions =
                    shc.sslConfig.getEnabledExtensions(
                        SSLHandshake.SERVER_HELLO, shc.negotiatedProtocol);
            shm.extensions.produce(shc, serverHelloExtensions);
            if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                SSLLogger.fine("Produced ServerHello handshake message", shm);
            }

            // Output the handshake message.
            shm.write(shc.handshakeOutput);
            shc.handshakeOutput.flush();

            // Change client/server handshake traffic secrets.
            // Refresh handshake hash
            shc.handshakeHash.update();

            // Change client/server handshake traffic secrets.
            SSLKeyExchange ke = shc.handshakeKeyExchange;
            if (ke == null) {
                // unlikely
                throw shc.conContext.fatal(Alert.INTERNAL_ERROR,
                        "Not negotiated key shares");
            }

            SSLKeyDerivation handshakeKD = ke.createKeyDerivation(shc);
            SecretKey handshakeSecret = handshakeKD.deriveKey(
                    "TlsHandshakeSecret", null);

            SSLTrafficKeyDerivation kdg =
                SSLTrafficKeyDerivation.valueOf(shc.negotiatedProtocol);
            if (kdg == null) {
                // unlikely
                throw shc.conContext.fatal(Alert.INTERNAL_ERROR,
                        "Not supported key derivation: " +
                        shc.negotiatedProtocol);
            }

            SSLKeyDerivation kd =
                    new SSLSecretDerivation(shc, handshakeSecret);

            // update the handshake traffic read keys.
            SecretKey readSecret = kd.deriveKey(
                    "TlsClientHandshakeTrafficSecret", null);
            SSLKeyDerivation readKD =
                    kdg.createKeyDerivation(shc, readSecret);
            SecretKey readKey = readKD.deriveKey(
                    "TlsKey", null);
            SecretKey readIvSecret = readKD.deriveKey(
                    "TlsIv", null);
            IvParameterSpec readIv =
                    new IvParameterSpec(readIvSecret.getEncoded());
            SSLReadCipher readCipher;
            try {
                readCipher =
                    shc.negotiatedCipherSuite.bulkCipher.createReadCipher(
                        Authenticator.valueOf(shc.negotiatedProtocol),
                        shc.negotiatedProtocol, readKey, readIv,
                        shc.sslContext.getSecureRandom());
            } catch (GeneralSecurityException gse) {
                // unlikely
                throw shc.conContext.fatal(Alert.HANDSHAKE_FAILURE,
                        "Missing cipher algorithm", gse);
            }

            if (readCipher == null) {
                throw shc.conContext.fatal(Alert.ILLEGAL_PARAMETER,
                    "Illegal cipher suite (" + shc.negotiatedCipherSuite +
                    ") and protocol version (" + shc.negotiatedProtocol +
                    ")");
            }

            shc.baseReadSecret = readSecret;
            shc.conContext.inputRecord.changeReadCiphers(readCipher);

            // update the handshake traffic write secret.
            SecretKey writeSecret = kd.deriveKey(
                    "TlsServerHandshakeTrafficSecret", null);
            SSLKeyDerivation writeKD =
                    kdg.createKeyDerivation(shc, writeSecret);
            SecretKey writeKey = writeKD.deriveKey(
                    "TlsKey", null);
            SecretKey writeIvSecret = writeKD.deriveKey(
                    "TlsIv", null);
            IvParameterSpec writeIv =
                    new IvParameterSpec(writeIvSecret.getEncoded());
            SSLWriteCipher writeCipher;
            try {
                writeCipher =
                    shc.negotiatedCipherSuite.bulkCipher.createWriteCipher(
                        Authenticator.valueOf(shc.negotiatedProtocol),
                        shc.negotiatedProtocol, writeKey, writeIv,
                        shc.sslContext.getSecureRandom());
            } catch (GeneralSecurityException gse) {
                // unlikely
                throw shc.conContext.fatal(Alert.HANDSHAKE_FAILURE,
                        "Missing cipher algorithm", gse);
            }

            if (writeCipher == null) {
                throw shc.conContext.fatal(Alert.ILLEGAL_PARAMETER,
                    "Illegal cipher suite (" + shc.negotiatedCipherSuite +
                    ") and protocol version (" + shc.negotiatedProtocol +
                    ")");
            }

            shc.baseWriteSecret = writeSecret;
            shc.conContext.outputRecord.changeWriteCiphers(
                    writeCipher, (clientHello.sessionId.length() != 0));

            // Update the context for master key derivation.
            shc.handshakeKeyDerivation = kd;

            // Check if the server supports stateless resumption
            if (sessionCache.statelessEnabled()) {
                shc.statelessResumption = true;
            }

            // The handshake message has been delivered.
            return null;
        }

        private static CipherSuite chooseCipherSuite(
                ServerHandshakeContext shc,
                ClientHelloMessage clientHello) throws IOException {
            List<CipherSuite> preferred;
            List<CipherSuite> proposed;
            if (shc.sslConfig.preferLocalCipherSuites) {
                preferred = shc.activeCipherSuites;
                proposed = clientHello.cipherSuites;
            } else {
                preferred = clientHello.cipherSuites;
                proposed = shc.activeCipherSuites;
            }

            CipherSuite legacySuite = null;
            AlgorithmConstraints legacyConstraints =
                    ServerHandshakeContext.legacyAlgorithmConstraints;
            for (CipherSuite cs : preferred) {
                if (!HandshakeContext.isNegotiable(
                        proposed, shc.negotiatedProtocol, cs)) {
                    continue;
                }

                if ((legacySuite == null) &&
                        !legacyConstraints.permits(
                                EnumSet.of(CryptoPrimitive.KEY_AGREEMENT),
                                cs.name, null)) {
                    legacySuite = cs;
                    continue;
                }

                // The cipher suite has been negotiated.
                if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                    SSLLogger.fine("use cipher suite " + cs.name);
                }
                return cs;
            }

            if (legacySuite != null) {
                if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                    SSLLogger.warning(
                            "use legacy cipher suite " + legacySuite.name);
                }
                return legacySuite;
            }

            // no cipher suites in common
            return null;
        }
    }

    /**
     * The "HelloRetryRequest" handshake message producer.
     */
    private static final
            class T13HelloRetryRequestProducer implements HandshakeProducer {
        // Prevent instantiation of this class.
        private T13HelloRetryRequestProducer() {
            // blank
        }

        @Override
        public byte[] produce(ConnectionContext context,
                HandshakeMessage message) throws IOException {
            ServerHandshakeContext shc = (ServerHandshakeContext) context;
            ClientHelloMessage clientHello = (ClientHelloMessage) message;

            // negotiate the cipher suite.
            CipherSuite cipherSuite =
                    T13ServerHelloProducer.chooseCipherSuite(shc, clientHello);
            if (cipherSuite == null) {
                throw shc.conContext.fatal(Alert.HANDSHAKE_FAILURE,
                        "no cipher suites in common for hello retry request");
            }

            ServerHelloMessage hhrm = new ServerHelloMessage(shc,
                    ProtocolVersion.TLS12,      // use legacy version
                    clientHello.sessionId,      //  echo back
                    cipherSuite,
                    RandomCookie.hrrRandom,
                    clientHello
            );

            shc.negotiatedCipherSuite = cipherSuite;
            shc.handshakeHash.determine(
                    shc.negotiatedProtocol, shc.negotiatedCipherSuite);

            // Produce extensions for HelloRetryRequest handshake message.
            SSLExtension[] serverHelloExtensions =
                shc.sslConfig.getEnabledExtensions(
                    SSLHandshake.HELLO_RETRY_REQUEST, shc.negotiatedProtocol);
            hhrm.extensions.produce(shc, serverHelloExtensions);
            if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                SSLLogger.fine(
                        "Produced HelloRetryRequest handshake message", hhrm);
            }

            // Output the handshake message.
            hhrm.write(shc.handshakeOutput);
            shc.handshakeOutput.flush();

            // Stateless, shall we clean up the handshake context as well?
            shc.handshakeHash.finish();     // forgot about the handshake hash
            shc.handshakeExtensions.clear();

            // What's the expected response?
            shc.handshakeConsumers.put(
                    SSLHandshake.CLIENT_HELLO.id, SSLHandshake.CLIENT_HELLO);

            // The handshake message has been delivered.
            return null;
        }
    }

    /**
     * The "HelloRetryRequest" handshake message reproducer.
     */
    private static final
            class T13HelloRetryRequestReproducer implements HandshakeProducer {
        // Prevent instantiation of this class.
        private T13HelloRetryRequestReproducer() {
            // blank
        }

        @Override
        public byte[] produce(ConnectionContext context,
                HandshakeMessage message) throws IOException {
            ServerHandshakeContext shc = (ServerHandshakeContext) context;
            ClientHelloMessage clientHello = (ClientHelloMessage) message;

            // negotiate the cipher suite.
            CipherSuite cipherSuite = shc.negotiatedCipherSuite;
            ServerHelloMessage hhrm = new ServerHelloMessage(shc,
                    ProtocolVersion.TLS12,      // use legacy version
                    clientHello.sessionId,      //  echo back
                    cipherSuite,
                    RandomCookie.hrrRandom,
                    clientHello
            );

            // Produce extensions for HelloRetryRequest handshake message.
            SSLExtension[] serverHelloExtensions =
                shc.sslConfig.getEnabledExtensions(
                    SSLHandshake.MESSAGE_HASH, shc.negotiatedProtocol);
            hhrm.extensions.produce(shc, serverHelloExtensions);
            if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                SSLLogger.fine(
                        "Reproduced HelloRetryRequest handshake message", hhrm);
            }

            HandshakeOutStream hos = new HandshakeOutStream(null);
            hhrm.write(hos);

            return hos.toByteArray();
        }
    }

    /**
     * The "ServerHello" handshake message consumer.
     */
    private static final
            class ServerHelloConsumer implements SSLConsumer {
        // Prevent instantiation of this class.
        private ServerHelloConsumer() {
            // blank
        }

        @Override
        public void consume(ConnectionContext context,
                ByteBuffer message) throws IOException {
            // The consuming happens in client side only.
            ClientHandshakeContext chc = (ClientHandshakeContext)context;

            // clean up this consumer
            chc.handshakeConsumers.remove(SSLHandshake.SERVER_HELLO.id);
            if (!chc.handshakeConsumers.isEmpty()) {
                // DTLS 1.0/1.2
                chc.handshakeConsumers.remove(
                        SSLHandshake.HELLO_VERIFY_REQUEST.id);
            }
            if (!chc.handshakeConsumers.isEmpty()) {
                throw chc.conContext.fatal(Alert.UNEXPECTED_MESSAGE,
                    "No more message expected before ServerHello is processed");
            }

            ServerHelloMessage shm = new ServerHelloMessage(chc, message);
            if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                SSLLogger.fine("Consuming ServerHello handshake message", shm);
            }

            if (shm.serverRandom.isHelloRetryRequest()) {
                onHelloRetryRequest(chc, shm);
            } else {
                onServerHello(chc, shm);
            }
        }

        private void onHelloRetryRequest(ClientHandshakeContext chc,
                ServerHelloMessage helloRetryRequest) throws IOException {
            // Negotiate protocol version.
            //
            // Check and launch SupportedVersions.
            SSLExtension[] extTypes = new SSLExtension[] {
                    SSLExtension.HRR_SUPPORTED_VERSIONS
                };
            helloRetryRequest.extensions.consumeOnLoad(chc, extTypes);

            ProtocolVersion serverVersion;
            SHSupportedVersionsSpec svs =
                    (SHSupportedVersionsSpec)chc.handshakeExtensions.get(
                            SSLExtension.HRR_SUPPORTED_VERSIONS);
            if (svs != null) {
                serverVersion =            // could be null
                        ProtocolVersion.valueOf(svs.selectedVersion);
            } else {
                serverVersion = helloRetryRequest.serverVersion;
            }

            if (!chc.activeProtocols.contains(serverVersion)) {
                throw chc.conContext.fatal(Alert.PROTOCOL_VERSION,
                    "The server selected protocol version " + serverVersion +
                    " is not accepted by client preferences " +
                    chc.activeProtocols);
            }

            if (!serverVersion.useTLS13PlusSpec()) {
                throw chc.conContext.fatal(Alert.PROTOCOL_VERSION,
                    "Unexpected HelloRetryRequest for " + serverVersion.name);
            }

            chc.negotiatedProtocol = serverVersion;
            if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                SSLLogger.fine(
                    "Negotiated protocol version: " + serverVersion.name);
            }

            // TLS 1.3 key share extension may have produced client
            // possessions for TLS 1.3 key exchanges.
            //
            // Clean up before producing new client key share possessions.
            chc.handshakePossessions.clear();

            if (serverVersion.isDTLS) {
                d13HrrHandshakeConsumer.consume(chc, helloRetryRequest);
            } else {
                t13HrrHandshakeConsumer.consume(chc, helloRetryRequest);
            }
        }

        private void onServerHello(ClientHandshakeContext chc,
                ServerHelloMessage serverHello) throws IOException {
            // Negotiate protocol version.
            //
            // Check and launch SupportedVersions.
            SSLExtension[] extTypes = new SSLExtension[] {
                    SSLExtension.SH_SUPPORTED_VERSIONS
                };
            serverHello.extensions.consumeOnLoad(chc, extTypes);

            ProtocolVersion serverVersion;
            SHSupportedVersionsSpec svs =
                    (SHSupportedVersionsSpec)chc.handshakeExtensions.get(
                            SSLExtension.SH_SUPPORTED_VERSIONS);
            if (svs != null) {
                serverVersion =            // could be null
                        ProtocolVersion.valueOf(svs.selectedVersion);
            } else {
                serverVersion = serverHello.serverVersion;
            }

            if (!chc.activeProtocols.contains(serverVersion)) {
                throw chc.conContext.fatal(Alert.PROTOCOL_VERSION,
                    "The server selected protocol version " + serverVersion +
                    " is not accepted by client preferences " +
                    chc.activeProtocols);
            }

            chc.negotiatedProtocol = serverVersion;
            if (!chc.conContext.isNegotiated) {
                chc.conContext.protocolVersion = chc.negotiatedProtocol;
                chc.conContext.outputRecord.setVersion(chc.negotiatedProtocol);
            }
            if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                SSLLogger.fine(
                    "Negotiated protocol version: " + serverVersion.name);
            }

            if (serverHello.serverRandom.isVersionDowngrade(chc)) {
                throw chc.conContext.fatal(Alert.ILLEGAL_PARAMETER,
                    "A potential protocol version downgrade attack");
            }

            // Consume the handshake message for the specific protocol version.
            if (serverVersion.isDTLS) {
                if (serverVersion.useTLS13PlusSpec()) {
                    d13HandshakeConsumer.consume(chc, serverHello);
                } else {
                    // TLS 1.3 key share extension may have produced client
                    // possessions for TLS 1.3 key exchanges.
                    chc.handshakePossessions.clear();

                    d12HandshakeConsumer.consume(chc, serverHello);
                }
            } else {
                if (serverVersion.useTLS13PlusSpec()) {
                    t13HandshakeConsumer.consume(chc, serverHello);
                } else {
                    // TLS 1.3 key share extension may have produced client
                    // possessions for TLS 1.3 key exchanges.
                    chc.handshakePossessions.clear();

                    t12HandshakeConsumer.consume(chc, serverHello);
                }
            }
        }
    }

    private static final
            class T12ServerHelloConsumer implements HandshakeConsumer {
        // Prevent instantiation of this class.
        private T12ServerHelloConsumer() {
            // blank
        }

        @Override
        public void consume(ConnectionContext context,
                HandshakeMessage message) throws IOException {
            // The consuming happens in client side only.
            ClientHandshakeContext chc = (ClientHandshakeContext)context;
            ServerHelloMessage serverHello = (ServerHelloMessage)message;
            if (!chc.isNegotiable(serverHello.serverVersion)) {
                throw chc.conContext.fatal(Alert.PROTOCOL_VERSION,
                    "Server chose " + serverHello.serverVersion +
                    ", but that protocol version is not enabled or " +
                    "not supported by the client.");
            }

            // chc.negotiatedProtocol = serverHello.serverVersion;
            chc.negotiatedCipherSuite = serverHello.cipherSuite;
            chc.handshakeHash.determine(
                    chc.negotiatedProtocol, chc.negotiatedCipherSuite);
            chc.serverHelloRandom = serverHello.serverRandom;
            if (chc.negotiatedCipherSuite.keyExchange == null) {
                throw chc.conContext.fatal(Alert.PROTOCOL_VERSION,
                    "TLS 1.2 or prior version does not support the " +
                    "server cipher suite: " + chc.negotiatedCipherSuite.name);
            }

            //
            // validate
            //

            // Check and launch the "renegotiation_info" extension.
            SSLExtension[] extTypes = new SSLExtension[] {
                    SSLExtension.SH_RENEGOTIATION_INFO
                };
            serverHello.extensions.consumeOnLoad(chc, extTypes);

            // Is it session resuming?
            if (chc.resumingSession != null) {
                // we tried to resume, let's see what the server decided
                if (serverHello.sessionId.equals(
                        chc.resumingSession.getSessionId())) {
                    // server resumed the session, let's make sure everything
                    // checks out

                    // Verify that the session ciphers are unchanged.
                    CipherSuite sessionSuite = chc.resumingSession.getSuite();
                    if (chc.negotiatedCipherSuite != sessionSuite) {
                        throw chc.conContext.fatal(Alert.PROTOCOL_VERSION,
                            "Server returned wrong cipher suite for session");
                    }

                    // verify protocol version match
                    ProtocolVersion sessionVersion =
                            chc.resumingSession.getProtocolVersion();
                    if (chc.negotiatedProtocol != sessionVersion) {
                        throw chc.conContext.fatal(Alert.PROTOCOL_VERSION,
                            "Server resumed with wrong protocol version");
                    }

                    // looks fine;  resume it.
                    chc.isResumption = true;
                    chc.resumingSession.setAsSessionResumption(true);
                    chc.handshakeSession = chc.resumingSession;
                } else {
                    // we wanted to resume, but the server refused
                    //
                    // Invalidate the session for initial handshake in case
                    // of reusing next time.
                    if (chc.resumingSession != null) {
                        chc.resumingSession.invalidate();
                        chc.resumingSession = null;
                    }
                    chc.isResumption = false;
                    if (!chc.sslConfig.enableSessionCreation) {
                        throw chc.conContext.fatal(Alert.PROTOCOL_VERSION,
                            "New session creation is disabled");
                    }
                }
            }

            // Check and launch ClientHello extensions.
            extTypes = chc.sslConfig.getEnabledExtensions(
                    SSLHandshake.SERVER_HELLO);
            serverHello.extensions.consumeOnLoad(chc, extTypes);

            if (!chc.isResumption) {
                if (chc.resumingSession != null) {
                    // in case the resumption happens next time.
                    chc.resumingSession.invalidate();
                    chc.resumingSession = null;
                }

                if (!chc.sslConfig.enableSessionCreation) {
                    throw chc.conContext.fatal(Alert.PROTOCOL_VERSION,
                        "New session creation is disabled");
                }

                if (serverHello.sessionId.length() == 0 &&
                        chc.statelessResumption) {
                    SessionId newId = new SessionId(true,
                            chc.sslContext.getSecureRandom());
                    chc.handshakeSession = new SSLSessionImpl(chc,
                            chc.negotiatedCipherSuite, newId);

                    if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                        SSLLogger.fine("Locally assigned Session Id: " +
                                newId.toString());
                    }
                } else {
                    chc.handshakeSession = new SSLSessionImpl(chc,
                            chc.negotiatedCipherSuite,
                            serverHello.sessionId);
                }
                chc.handshakeSession.setMaximumPacketSize(
                        chc.sslConfig.maximumPacketSize);
            }

            //
            // update
            //
            serverHello.extensions.consumeOnTrade(chc, extTypes);

            // update the consumers and producers
            if (chc.isResumption) {
                SSLTrafficKeyDerivation kdg =
                        SSLTrafficKeyDerivation.valueOf(chc.negotiatedProtocol);
                if (kdg == null) {
                    // unlikely
                    throw chc.conContext.fatal(Alert.INTERNAL_ERROR,
                            "Not supported key derivation: " +
                            chc.negotiatedProtocol);
                } else {
                    chc.handshakeKeyDerivation = kdg.createKeyDerivation(
                            chc, chc.resumingSession.getMasterSecret());
                }

                if (chc.statelessResumption) {
                    chc.handshakeConsumers.putIfAbsent(
                            SSLHandshake.NEW_SESSION_TICKET.id,
                            SSLHandshake.NEW_SESSION_TICKET);
                }
                chc.conContext.consumers.putIfAbsent(
                        ContentType.CHANGE_CIPHER_SPEC.id,
                        ChangeCipherSpec.t10Consumer);
                chc.handshakeConsumers.put(
                        SSLHandshake.FINISHED.id,
                        SSLHandshake.FINISHED);
            } else {
                SSLKeyExchange ke = SSLKeyExchange.valueOf(
                        chc.negotiatedCipherSuite.keyExchange,
                        chc.negotiatedProtocol);
                chc.handshakeKeyExchange = ke;
                if (ke != null) {
                    for (SSLHandshake handshake :
                            ke.getRelatedHandshakers(chc)) {
                        chc.handshakeConsumers.put(handshake.id, handshake);
                    }
                }

                chc.handshakeConsumers.put(SSLHandshake.SERVER_HELLO_DONE.id,
                        SSLHandshake.SERVER_HELLO_DONE);
            }

            //
            // produce
            //
            // Need no new handshake message producers here.
        }
    }

    private static void setUpPskKD(HandshakeContext hc,
            SecretKey psk) throws SSLHandshakeException {

        if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
            SSLLogger.fine("Using PSK to derive early secret");
        }

        try {
            CipherSuite.HashAlg hashAlg = hc.negotiatedCipherSuite.hashAlg;
            HKDF hkdf = new HKDF(hashAlg.name);
            byte[] zeros = new byte[hashAlg.hashLength];
            SecretKey earlySecret = hkdf.extract(zeros, psk, "TlsEarlySecret");
            hc.handshakeKeyDerivation =
                    new SSLSecretDerivation(hc, earlySecret);
        } catch  (GeneralSecurityException gse) {
            throw (SSLHandshakeException) new SSLHandshakeException(
                "Could not generate secret").initCause(gse);
        }
    }

    private static final
            class T13ServerHelloConsumer implements HandshakeConsumer {
        // Prevent instantiation of this class.
        private T13ServerHelloConsumer() {
            // blank
        }

        @Override
        public void consume(ConnectionContext context,
                HandshakeMessage message) throws IOException {
            // The consuming happens in client side only.
            ClientHandshakeContext chc = (ClientHandshakeContext)context;
            ServerHelloMessage serverHello = (ServerHelloMessage)message;
            if (serverHello.serverVersion != ProtocolVersion.TLS12) {
                throw chc.conContext.fatal(Alert.PROTOCOL_VERSION,
                    "The ServerHello.legacy_version field is not TLS 1.2");
            }

            chc.negotiatedCipherSuite = serverHello.cipherSuite;
            chc.handshakeHash.determine(
                    chc.negotiatedProtocol, chc.negotiatedCipherSuite);
            chc.serverHelloRandom = serverHello.serverRandom;

            //
            // validate
            //

            // Check and launch ServerHello extensions.
            SSLExtension[] extTypes = chc.sslConfig.getEnabledExtensions(
                    SSLHandshake.SERVER_HELLO);
            serverHello.extensions.consumeOnLoad(chc, extTypes);
            if (!chc.isResumption) {
                if (chc.resumingSession != null) {
                    // in case the resumption happens next time.
                    chc.resumingSession.invalidate();
                    chc.resumingSession = null;
                }

                if (!chc.sslConfig.enableSessionCreation) {
                    throw chc.conContext.fatal(Alert.PROTOCOL_VERSION,
                        "New session creation is disabled");
                }
                chc.handshakeSession = new SSLSessionImpl(chc,
                        chc.negotiatedCipherSuite,
                        serverHello.sessionId);
                chc.handshakeSession.setMaximumPacketSize(
                        chc.sslConfig.maximumPacketSize);
            } else {
                // The PSK is consumed to allow it to be deleted
                SecretKey psk =
                        chc.resumingSession.consumePreSharedKey();
                if(psk == null) {
                    throw chc.conContext.fatal(Alert.INTERNAL_ERROR,
                    "No PSK available. Unable to resume.");
                }

                chc.handshakeSession = chc.resumingSession;

                setUpPskKD(chc, psk);
            }

            //
            // update
            //
            serverHello.extensions.consumeOnTrade(chc, extTypes);

            // Change client/server handshake traffic secrets.
            // Refresh handshake hash
            chc.handshakeHash.update();

            SSLKeyExchange ke = chc.handshakeKeyExchange;
            if (ke == null) {
                // unlikely
                throw chc.conContext.fatal(Alert.INTERNAL_ERROR,
                        "Not negotiated key shares");
            }

            SSLKeyDerivation handshakeKD = ke.createKeyDerivation(chc);
            SecretKey handshakeSecret = handshakeKD.deriveKey(
                    "TlsHandshakeSecret", null);
            SSLTrafficKeyDerivation kdg =
                SSLTrafficKeyDerivation.valueOf(chc.negotiatedProtocol);
            if (kdg == null) {
                // unlikely
                throw chc.conContext.fatal(Alert.INTERNAL_ERROR,
                        "Not supported key derivation: " +
                        chc.negotiatedProtocol);
            }

            SSLKeyDerivation secretKD =
                    new SSLSecretDerivation(chc, handshakeSecret);

            // update the handshake traffic read keys.
            SecretKey readSecret = secretKD.deriveKey(
                    "TlsServerHandshakeTrafficSecret", null);

            SSLKeyDerivation readKD =
                    kdg.createKeyDerivation(chc, readSecret);
            SecretKey readKey = readKD.deriveKey(
                    "TlsKey", null);
            SecretKey readIvSecret = readKD.deriveKey(
                    "TlsIv", null);
            IvParameterSpec readIv =
                    new IvParameterSpec(readIvSecret.getEncoded());
            SSLReadCipher readCipher;
            try {
                readCipher =
                    chc.negotiatedCipherSuite.bulkCipher.createReadCipher(
                        Authenticator.valueOf(chc.negotiatedProtocol),
                        chc.negotiatedProtocol, readKey, readIv,
                        chc.sslContext.getSecureRandom());
            } catch (GeneralSecurityException gse) {
                // unlikely
                throw chc.conContext.fatal(Alert.HANDSHAKE_FAILURE,
                        "Missing cipher algorithm", gse);
            }

            if (readCipher == null) {
                throw chc.conContext.fatal(Alert.ILLEGAL_PARAMETER,
                    "Illegal cipher suite (" + chc.negotiatedCipherSuite +
                    ") and protocol version (" + chc.negotiatedProtocol +
                    ")");
            }

            chc.baseReadSecret = readSecret;
            chc.conContext.inputRecord.changeReadCiphers(readCipher);

            // update the handshake traffic write keys.
            SecretKey writeSecret = secretKD.deriveKey(
                    "TlsClientHandshakeTrafficSecret", null);
            SSLKeyDerivation writeKD =
                    kdg.createKeyDerivation(chc, writeSecret);
            SecretKey writeKey = writeKD.deriveKey(
                    "TlsKey", null);
            SecretKey writeIvSecret = writeKD.deriveKey(
                    "TlsIv", null);
            IvParameterSpec writeIv =
                    new IvParameterSpec(writeIvSecret.getEncoded());
            SSLWriteCipher writeCipher;
            try {
                writeCipher =
                    chc.negotiatedCipherSuite.bulkCipher.createWriteCipher(
                        Authenticator.valueOf(chc.negotiatedProtocol),
                        chc.negotiatedProtocol, writeKey, writeIv,
                        chc.sslContext.getSecureRandom());
            } catch (GeneralSecurityException gse) {
                // unlikely
                throw chc.conContext.fatal(Alert.HANDSHAKE_FAILURE,
                        "Missing cipher algorithm", gse);
            }

            if (writeCipher == null) {
                throw chc.conContext.fatal(Alert.ILLEGAL_PARAMETER,
                    "Illegal cipher suite (" + chc.negotiatedCipherSuite +
                    ") and protocol version (" + chc.negotiatedProtocol +
                    ")");
            }

            chc.baseWriteSecret = writeSecret;
            chc.conContext.outputRecord.changeWriteCiphers(
                    writeCipher, (serverHello.sessionId.length() != 0));

            // Should use resumption_master_secret for TLS 1.3.
            // chc.handshakeSession.setMasterSecret(masterSecret);

            // Update the context for master key derivation.
            chc.handshakeKeyDerivation = secretKD;

            // update the consumers and producers
            //
            // The server sends a dummy change_cipher_spec record immediately
            // after its first handshake message.  This may either be after a
            // ServerHello or a HelloRetryRequest.
            chc.conContext.consumers.putIfAbsent(
                    ContentType.CHANGE_CIPHER_SPEC.id,
                    ChangeCipherSpec.t13Consumer);

            chc.handshakeConsumers.put(
                    SSLHandshake.ENCRYPTED_EXTENSIONS.id,
                    SSLHandshake.ENCRYPTED_EXTENSIONS);

            // Support cert authentication only, when not PSK.
            chc.handshakeConsumers.put(
                    SSLHandshake.CERTIFICATE_REQUEST.id,
                    SSLHandshake.CERTIFICATE_REQUEST);
            chc.handshakeConsumers.put(
                    SSLHandshake.CERTIFICATE.id,
                    SSLHandshake.CERTIFICATE);
            chc.handshakeConsumers.put(
                    SSLHandshake.CERTIFICATE_VERIFY.id,
                    SSLHandshake.CERTIFICATE_VERIFY);

            chc.handshakeConsumers.put(
                    SSLHandshake.FINISHED.id,
                    SSLHandshake.FINISHED);

            //
            // produce
            //
            // Need no new handshake message producers here.
        }
    }

    private static final
            class T13HelloRetryRequestConsumer implements HandshakeConsumer {
        // Prevent instantiation of this class.
        private T13HelloRetryRequestConsumer() {
            // blank
        }

        @Override
        public void consume(ConnectionContext context,
                HandshakeMessage message) throws IOException {
            // The consuming happens in client side only.
            ClientHandshakeContext chc = (ClientHandshakeContext)context;
            ServerHelloMessage helloRetryRequest = (ServerHelloMessage)message;
            if (helloRetryRequest.serverVersion != ProtocolVersion.TLS12) {
                throw chc.conContext.fatal(Alert.PROTOCOL_VERSION,
                    "The HelloRetryRequest.legacy_version is not TLS 1.2");
            }

            chc.negotiatedCipherSuite = helloRetryRequest.cipherSuite;

            //
            // validate
            //

            // Check and launch ClientHello extensions.
            SSLExtension[] extTypes = chc.sslConfig.getEnabledExtensions(
                    SSLHandshake.HELLO_RETRY_REQUEST);
            helloRetryRequest.extensions.consumeOnLoad(chc, extTypes);

            //
            // update
            //
            helloRetryRequest.extensions.consumeOnTrade(chc, extTypes);

            // Change client/server handshake traffic secrets.
            // Refresh handshake hash
            chc.handshakeHash.finish();     // reset the handshake hash

            // calculate the transcript hash of the 1st ClientHello message
            HandshakeOutStream hos = new HandshakeOutStream(null);
            try {
                chc.initialClientHelloMsg.write(hos);
            } catch (IOException ioe) {
                // unlikely
                throw chc.conContext.fatal(Alert.HANDSHAKE_FAILURE,
                    "Failed to construct message hash", ioe);
            }
            chc.handshakeHash.deliver(hos.toByteArray());
            chc.handshakeHash.determine(
                    chc.negotiatedProtocol, chc.negotiatedCipherSuite);
            byte[] clientHelloHash = chc.handshakeHash.digest();

            // calculate the message_hash
            //
            // Transcript-Hash(ClientHello1, HelloRetryRequest, ... Mn) =
            //   Hash(message_hash ||    /* Handshake type */
            //     00 00 Hash.length ||  /* Handshake message length (bytes) */
            //     Hash(ClientHello1) || /* Hash of ClientHello1 */
            //     HelloRetryRequest || ... || Mn)
            int hashLen = chc.negotiatedCipherSuite.hashAlg.hashLength;
            byte[] hashedClientHello = new byte[4 + hashLen];
            hashedClientHello[0] = SSLHandshake.MESSAGE_HASH.id;
            hashedClientHello[1] = (byte)0x00;
            hashedClientHello[2] = (byte)0x00;
            hashedClientHello[3] = (byte)(hashLen & 0xFF);
            System.arraycopy(clientHelloHash, 0,
                    hashedClientHello, 4, hashLen);

            chc.handshakeHash.finish();     // reset the handshake hash
            chc.handshakeHash.deliver(hashedClientHello);

            int hrrBodyLen = helloRetryRequest.handshakeRecord.remaining();
            byte[] hrrMessage = new byte[4 + hrrBodyLen];
            hrrMessage[0] = SSLHandshake.HELLO_RETRY_REQUEST.id;
            hrrMessage[1] = (byte)((hrrBodyLen >> 16) & 0xFF);
            hrrMessage[2] = (byte)((hrrBodyLen >> 8) & 0xFF);
            hrrMessage[3] = (byte)(hrrBodyLen & 0xFF);

            ByteBuffer hrrBody = helloRetryRequest.handshakeRecord.duplicate();
            hrrBody.get(hrrMessage, 4, hrrBodyLen);

            chc.handshakeHash.receive(hrrMessage);

            // Update the initial ClientHello handshake message.
            chc.initialClientHelloMsg.extensions.reproduce(chc,
                    new SSLExtension[] {
                        SSLExtension.CH_COOKIE,
                        SSLExtension.CH_KEY_SHARE,
                        SSLExtension.CH_PRE_SHARED_KEY
                    });

            //
            // produce response handshake message
            //
            SSLHandshake.CLIENT_HELLO.produce(context, helloRetryRequest);
        }
    }
}
