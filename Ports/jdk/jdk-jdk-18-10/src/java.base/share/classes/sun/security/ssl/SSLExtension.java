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

package sun.security.ssl;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.MessageFormat;
import java.util.*;

import sun.security.action.GetPropertyAction;
import sun.security.ssl.SSLHandshake.HandshakeMessage;
import sun.security.util.HexDumpEncoder;

enum SSLExtension implements SSLStringizer {
    // Extensions defined in RFC 6066 (TLS Extensions: Extension Definitions)
    CH_SERVER_NAME          (0x0000,  "server_name",
                                SSLHandshake.CLIENT_HELLO,
                                ProtocolVersion.PROTOCOLS_TO_13,
                                ServerNameExtension.chNetworkProducer,
                                ServerNameExtension.chOnLoadConsumer,
                                null,
                                null,
                                null,
                                ServerNameExtension.chStringizer),
    SH_SERVER_NAME          (0x0000, "server_name",
                                SSLHandshake.SERVER_HELLO,
                                ProtocolVersion.PROTOCOLS_TO_12,
                                ServerNameExtension.shNetworkProducer,
                                ServerNameExtension.shOnLoadConsumer,
                                null,
                                null,
                                null,
                                ServerNameExtension.shStringizer),
    EE_SERVER_NAME          (0x0000, "server_name",
                                SSLHandshake.ENCRYPTED_EXTENSIONS,
                                ProtocolVersion.PROTOCOLS_OF_13,
                                ServerNameExtension.eeNetworkProducer,
                                ServerNameExtension.eeOnLoadConsumer,
                                null,
                                null,
                                null,
                                ServerNameExtension.shStringizer),

    CH_MAX_FRAGMENT_LENGTH (0x0001, "max_fragment_length",
                                SSLHandshake.CLIENT_HELLO,
                                ProtocolVersion.PROTOCOLS_TO_13,
                                MaxFragExtension.chNetworkProducer,
                                MaxFragExtension.chOnLoadConsumer,
                                null,
                                null,
                                null,
                                MaxFragExtension.maxFragLenStringizer),
    SH_MAX_FRAGMENT_LENGTH (0x0001, "max_fragment_length",
                                SSLHandshake.SERVER_HELLO,
                                ProtocolVersion.PROTOCOLS_TO_12,
                                MaxFragExtension.shNetworkProducer,
                                MaxFragExtension.shOnLoadConsumer,
                                null,
                                MaxFragExtension.shOnTradeConsumer,
                                null,
                                MaxFragExtension.maxFragLenStringizer),
    EE_MAX_FRAGMENT_LENGTH (0x0001, "max_fragment_length",
                                SSLHandshake.ENCRYPTED_EXTENSIONS,
                                ProtocolVersion.PROTOCOLS_OF_13,
                                MaxFragExtension.eeNetworkProducer,
                                MaxFragExtension.eeOnLoadConsumer,
                                null,
                                MaxFragExtension.eeOnTradeConsumer,
                                null,
                                MaxFragExtension.maxFragLenStringizer),

    CLIENT_CERTIFICATE_URL  (0x0002, "client_certificate_url"),
    TRUSTED_CA_KEYS         (0x0003, "trusted_ca_keys"),
    TRUNCATED_HMAC          (0x0004, "truncated_hmac"),

    CH_STATUS_REQUEST       (0x0005, "status_request",
                                SSLHandshake.CLIENT_HELLO,
                                ProtocolVersion.PROTOCOLS_TO_13,
                                CertStatusExtension.chNetworkProducer,
                                CertStatusExtension.chOnLoadConsumer,
                                null,
                                null,
                                null,
                                CertStatusExtension.certStatusReqStringizer),
    SH_STATUS_REQUEST       (0x0005, "status_request",
                                SSLHandshake.SERVER_HELLO,
                                ProtocolVersion.PROTOCOLS_TO_12,
                                CertStatusExtension.shNetworkProducer,
                                CertStatusExtension.shOnLoadConsumer,
                                null,
                                null,
                                null,
                                CertStatusExtension.certStatusReqStringizer),
    CR_STATUS_REQUEST       (0x0005, "status_request"),
    CT_STATUS_REQUEST       (0x0005, "status_request",
                                SSLHandshake.CERTIFICATE,
                                ProtocolVersion.PROTOCOLS_OF_13,
                                CertStatusExtension.ctNetworkProducer,
                                CertStatusExtension.ctOnLoadConsumer,
                                null,
                                null,
                                null,
                                CertStatusExtension.certStatusRespStringizer),

    // Extensions defined in RFC 4681 (TLS User Mapping Extension)
    USER_MAPPING            (0x0006, "user_mapping"),

    // Extensions defined in RFC 5878 (TLS Authorization Extensions)
    CLIENT_AUTHZ            (0x0007, "client_authz"),
    SERVER_AUTHZ            (0x0008, "server_authz"),

    // Extensions defined in RFC 6091 (Using OpenPGP Keys for TLS Authentication)
    CERT_TYPE               (0x0009, "cert_type"),

    // Extensions defined in RFC 8422 (ECC Cipher Suites for TLS Versions 1.2 and Earlier)
    CH_SUPPORTED_GROUPS     (0x000A, "supported_groups",
                                SSLHandshake.CLIENT_HELLO,
                                ProtocolVersion.PROTOCOLS_TO_13,
                                SupportedGroupsExtension.chNetworkProducer,
                                SupportedGroupsExtension.chOnLoadConsumer,
                                null,
                                null,
                                SupportedGroupsExtension.chOnTradAbsence,
                                SupportedGroupsExtension.sgsStringizer),
    EE_SUPPORTED_GROUPS     (0x000A, "supported_groups",
                                SSLHandshake.ENCRYPTED_EXTENSIONS,
                                ProtocolVersion.PROTOCOLS_OF_13,
                                SupportedGroupsExtension.eeNetworkProducer,
                                SupportedGroupsExtension.eeOnLoadConsumer,
                                null,
                                null,
                                null,
                                SupportedGroupsExtension.sgsStringizer),

    CH_EC_POINT_FORMATS     (0x000B, "ec_point_formats",
                                SSLHandshake.CLIENT_HELLO,
                                ProtocolVersion.PROTOCOLS_TO_12,
                                ECPointFormatsExtension.chNetworkProducer,
                                ECPointFormatsExtension.chOnLoadConsumer,
                                null,
                                null,
                                null,
                                ECPointFormatsExtension.epfStringizer),
    SH_EC_POINT_FORMATS     (0x000B, "ec_point_formats",
                                SSLHandshake.SERVER_HELLO,
                                ProtocolVersion.PROTOCOLS_TO_12,
                                null,   // not use of the producer
                                ECPointFormatsExtension.shOnLoadConsumer,
                                null,
                                null,
                                null,
                                ECPointFormatsExtension.epfStringizer),

    // Extensions defined in RFC 5054 (Using the SRP Protocol for TLS Authentication)
    SRP                     (0x000C, "srp"),

    // Extensions defined in RFC 5764 (DTLS Extension to Establish Keys for the SRTP)
    USE_SRTP                (0x000E, "use_srtp"),

    // Extensions defined in RFC 6520 (TLS and DTLS Heartbeat Extension)
    HEARTBEAT               (0x000E, "heartbeat"),

    // Extensions defined in RFC 7301 (TLS Application-Layer Protocol Negotiation Extension)
    CH_ALPN                 (0x0010, "application_layer_protocol_negotiation",
                                SSLHandshake.CLIENT_HELLO,
                                ProtocolVersion.PROTOCOLS_TO_13,
                                AlpnExtension.chNetworkProducer,
                                AlpnExtension.chOnLoadConsumer,
                                AlpnExtension.chOnLoadAbsence,
                                null,
                                null,
                                AlpnExtension.alpnStringizer),
    SH_ALPN                 (0x0010, "application_layer_protocol_negotiation",
                                SSLHandshake.SERVER_HELLO,
                                ProtocolVersion.PROTOCOLS_TO_12,
                                AlpnExtension.shNetworkProducer,
                                AlpnExtension.shOnLoadConsumer,
                                AlpnExtension.shOnLoadAbsence,
                                null,
                                null,
                                AlpnExtension.alpnStringizer),
    EE_ALPN                 (0x0010, "application_layer_protocol_negotiation",
                                SSLHandshake.ENCRYPTED_EXTENSIONS,
                                ProtocolVersion.PROTOCOLS_OF_13,
                                AlpnExtension.shNetworkProducer,
                                AlpnExtension.shOnLoadConsumer,
                                AlpnExtension.shOnLoadAbsence,
                                null,
                                null,
                                AlpnExtension.alpnStringizer),

    // Extensions defined in RFC 6961 (TLS Multiple Certificate Status Request Extension)
    CH_STATUS_REQUEST_V2    (0x0011, "status_request_v2",
                                SSLHandshake.CLIENT_HELLO,
                                ProtocolVersion.PROTOCOLS_TO_12,
                                CertStatusExtension.chV2NetworkProducer,
                                CertStatusExtension.chV2OnLoadConsumer,
                                null,
                                null,
                                null,
                                CertStatusExtension.certStatusReqV2Stringizer),
    SH_STATUS_REQUEST_V2    (0x0011, "status_request_v2",
                                SSLHandshake.SERVER_HELLO,
                                ProtocolVersion.PROTOCOLS_TO_12,
                                CertStatusExtension.shV2NetworkProducer,
                                CertStatusExtension.shV2OnLoadConsumer,
                                null,
                                null,
                                null,
                                CertStatusExtension.certStatusReqV2Stringizer),

    // Extensions defined in RFC 6962 (Certificate Transparency)
    SIGNED_CERT_TIMESTAMP   (0x0012, "signed_certificate_timestamp"),

    // Extensions defined in RFC 7250 (Using Raw Public Keys in TLS and DTLS)
    CLIENT_CERT_TYPE        (0x0013, "client_certificate_type"),
    SERVER_CERT_TYPE        (0x0014, "server_certificate_type"),

    // Extensions defined in RFC 7685 (TLS ClientHello Padding Extension)
    PADDING                 (0x0015, "padding"),

    // Extensions defined in RFC 7366 (Encrypt-then-MAC for TLS and DTLS)
    ENCRYPT_THEN_MAC        (0x0016, "encrypt_then_mac"),

    // Extensions defined in RFC 7627 (TLS Session Hash and Extended Master Secret Extension)
    CH_EXTENDED_MASTER_SECRET  (0x0017, "extended_master_secret",
                                SSLHandshake.CLIENT_HELLO,
                                ProtocolVersion.PROTOCOLS_TO_12,
                                ExtendedMasterSecretExtension.chNetworkProducer,
                                ExtendedMasterSecretExtension.chOnLoadConsumer,
                                ExtendedMasterSecretExtension.chOnLoadAbsence,
                                null,
                                null,
                                ExtendedMasterSecretExtension.emsStringizer),
    SH_EXTENDED_MASTER_SECRET  (0x0017, "extended_master_secret",
                                SSLHandshake.SERVER_HELLO,
                                ProtocolVersion.PROTOCOLS_TO_12,
                                ExtendedMasterSecretExtension.shNetworkProducer,
                                ExtendedMasterSecretExtension.shOnLoadConsumer,
                                ExtendedMasterSecretExtension.shOnLoadAbsence,
                                null,
                                null,
                                ExtendedMasterSecretExtension.emsStringizer),

    // Extensions defined in RFC 8472 (TLS Extension for Token Binding Protocol Negotiation)
    TOKEN_BINDING           (0x0018, "token_binding"),

    // Extensions defined in RFC 7924 (TLS Cached Information Extension)
    CACHED_INFO             (0x0019, "cached_info"),

    // Extensions defined in RFC 5077 (TLS Session Resumption without Server-Side State)
    CH_SESSION_TICKET       (0x0023, "session_ticket",
            SSLHandshake.CLIENT_HELLO,
            ProtocolVersion.PROTOCOLS_10_12,
            SessionTicketExtension.chNetworkProducer,
            SessionTicketExtension.chOnLoadConsumer,
            null,
            null,
            null,
            SessionTicketExtension.steStringizer),
            //null),
    SH_SESSION_TICKET       (0x0023, "session_ticket",
            SSLHandshake.SERVER_HELLO,
            ProtocolVersion.PROTOCOLS_10_12,
            SessionTicketExtension.shNetworkProducer,
            SessionTicketExtension.shOnLoadConsumer,
            null,
            null,
            null,
            SessionTicketExtension.steStringizer),
            //null),

    // Extensions defined in RFC 8446 (TLS Protocol Version 1.3)
    CH_SIGNATURE_ALGORITHMS (0x000D, "signature_algorithms",
                                SSLHandshake.CLIENT_HELLO,
                                ProtocolVersion.PROTOCOLS_12_13,
                                SignatureAlgorithmsExtension.chNetworkProducer,
                                SignatureAlgorithmsExtension.chOnLoadConsumer,
                                SignatureAlgorithmsExtension.chOnLoadAbsence,
                                SignatureAlgorithmsExtension.chOnTradeConsumer,
                                SignatureAlgorithmsExtension.chOnTradeAbsence,
                                SignatureAlgorithmsExtension.ssStringizer),
    CR_SIGNATURE_ALGORITHMS (0x000D, "signature_algorithms",
                                SSLHandshake.CERTIFICATE_REQUEST,
                                ProtocolVersion.PROTOCOLS_OF_13,
                                SignatureAlgorithmsExtension.crNetworkProducer,
                                SignatureAlgorithmsExtension.crOnLoadConsumer,
                                SignatureAlgorithmsExtension.crOnLoadAbsence,
                                SignatureAlgorithmsExtension.crOnTradeConsumer,
                                null,
                                SignatureAlgorithmsExtension.ssStringizer),

    CH_EARLY_DATA           (0x002A, "early_data"),
    EE_EARLY_DATA           (0x002A, "early_data"),
    NST_EARLY_DATA          (0x002A, "early_data"),

    CH_SUPPORTED_VERSIONS   (0x002B, "supported_versions",
                                SSLHandshake.CLIENT_HELLO,
                                ProtocolVersion.PROTOCOLS_TO_13,
                                SupportedVersionsExtension.chNetworkProducer,
                                SupportedVersionsExtension.chOnLoadConsumer,
                                null,
                                null,
                                null,
                                SupportedVersionsExtension.chStringizer),
    SH_SUPPORTED_VERSIONS   (0x002B, "supported_versions",
                                SSLHandshake.SERVER_HELLO,
                                ProtocolVersion.PROTOCOLS_OF_13,
                                SupportedVersionsExtension.shNetworkProducer,
                                SupportedVersionsExtension.shOnLoadConsumer,
                                null,
                                null,
                                null,
                                SupportedVersionsExtension.shStringizer),
    HRR_SUPPORTED_VERSIONS  (0x002B, "supported_versions",
                                SSLHandshake.HELLO_RETRY_REQUEST,
                                ProtocolVersion.PROTOCOLS_OF_13,
                                SupportedVersionsExtension.hrrNetworkProducer,
                                SupportedVersionsExtension.hrrOnLoadConsumer,
                                null,
                                null,
                                null,
                                SupportedVersionsExtension.hrrStringizer),
    MH_SUPPORTED_VERSIONS   (0x002B, "supported_versions",
                                SSLHandshake.MESSAGE_HASH,
                                ProtocolVersion.PROTOCOLS_OF_13,
                                SupportedVersionsExtension.hrrReproducer,
                                null, null, null,
                                null,
                                SupportedVersionsExtension.hrrStringizer),

    CH_COOKIE               (0x002C, "cookie",
                                SSLHandshake.CLIENT_HELLO,
                                ProtocolVersion.PROTOCOLS_OF_13,
                                CookieExtension.chNetworkProducer,
                                CookieExtension.chOnLoadConsumer,
                                null,
                                CookieExtension.chOnTradeConsumer,
                                null,
                                CookieExtension.cookieStringizer),
    HRR_COOKIE              (0x002C, "cookie",
                                SSLHandshake.HELLO_RETRY_REQUEST,
                                ProtocolVersion.PROTOCOLS_OF_13,
                                CookieExtension.hrrNetworkProducer,
                                CookieExtension.hrrOnLoadConsumer,
                                null, null,
                                null,
                                CookieExtension.cookieStringizer),
    MH_COOKIE               (0x002C, "cookie",
                                SSLHandshake.MESSAGE_HASH,
                                ProtocolVersion.PROTOCOLS_OF_13,
                                CookieExtension.hrrNetworkReproducer,
                                null, null, null,
                                null,
                                CookieExtension.cookieStringizer),

    PSK_KEY_EXCHANGE_MODES  (0x002D, "psk_key_exchange_modes",
                                SSLHandshake.CLIENT_HELLO,
                                ProtocolVersion.PROTOCOLS_OF_13,
                                PskKeyExchangeModesExtension.chNetworkProducer,
                                PskKeyExchangeModesExtension.chOnLoadConsumer,
                                PskKeyExchangeModesExtension.chOnLoadAbsence,
                                null,
                                PskKeyExchangeModesExtension.chOnTradeAbsence,
                                PskKeyExchangeModesExtension.pkemStringizer),
    CH_CERTIFICATE_AUTHORITIES (0x002F, "certificate_authorities",
                                SSLHandshake.CLIENT_HELLO,
                                ProtocolVersion.PROTOCOLS_OF_13,
                                CertificateAuthoritiesExtension.chNetworkProducer,
                                CertificateAuthoritiesExtension.chOnLoadConsumer,
                                null,
                                null,
                                null,
                                CertificateAuthoritiesExtension.ssStringizer),
    CR_CERTIFICATE_AUTHORITIES (0x002F, "certificate_authorities",
                                SSLHandshake.CERTIFICATE_REQUEST,
                                ProtocolVersion.PROTOCOLS_OF_13,
                                CertificateAuthoritiesExtension.crNetworkProducer,
                                CertificateAuthoritiesExtension.crOnLoadConsumer,
                                null,
                                null,
                                null,
                                CertificateAuthoritiesExtension.ssStringizer),

    OID_FILTERS             (0x0030, "oid_filters"),
    POST_HANDSHAKE_AUTH     (0x0030, "post_handshake_auth"),

    CH_SIGNATURE_ALGORITHMS_CERT (0x0032, "signature_algorithms_cert",
                                SSLHandshake.CLIENT_HELLO,
                                ProtocolVersion.PROTOCOLS_12_13,
                                CertSignAlgsExtension.chNetworkProducer,
                                CertSignAlgsExtension.chOnLoadConsumer,
                                null,
                                CertSignAlgsExtension.chOnTradeConsumer,
                                null,
                                CertSignAlgsExtension.ssStringizer),
    CR_SIGNATURE_ALGORITHMS_CERT (0x0032, "signature_algorithms_cert",
                                SSLHandshake.CERTIFICATE_REQUEST,
                                ProtocolVersion.PROTOCOLS_OF_13,
                                CertSignAlgsExtension.crNetworkProducer,
                                CertSignAlgsExtension.crOnLoadConsumer,
                                null,
                                CertSignAlgsExtension.crOnTradeConsumer,
                                null,
                                CertSignAlgsExtension.ssStringizer),

    CH_KEY_SHARE            (0x0033, "key_share",
                                SSLHandshake.CLIENT_HELLO,
                                ProtocolVersion.PROTOCOLS_OF_13,
                                KeyShareExtension.chNetworkProducer,
                                KeyShareExtension.chOnLoadConsumer,
                                null,
                                null,
                                KeyShareExtension.chOnTradAbsence,
                                KeyShareExtension.chStringizer),
    SH_KEY_SHARE            (0x0033, "key_share",
                                SSLHandshake.SERVER_HELLO,
                                ProtocolVersion.PROTOCOLS_OF_13,
                                KeyShareExtension.shNetworkProducer,
                                KeyShareExtension.shOnLoadConsumer,
                                KeyShareExtension.shOnLoadAbsence,
                                null,
                                null,
                                KeyShareExtension.shStringizer),
    HRR_KEY_SHARE           (0x0033, "key_share",
                                SSLHandshake.HELLO_RETRY_REQUEST,
                                ProtocolVersion.PROTOCOLS_OF_13,
                                KeyShareExtension.hrrNetworkProducer,
                                KeyShareExtension.hrrOnLoadConsumer,
                                null, null, null,
                                KeyShareExtension.hrrStringizer),
    MH_KEY_SHARE            (0x0033, "key_share",
                                SSLHandshake.MESSAGE_HASH,
                                ProtocolVersion.PROTOCOLS_OF_13,
                                KeyShareExtension.hrrNetworkReproducer,
                                null, null, null, null,
                                KeyShareExtension.hrrStringizer),

    // Extensions defined in RFC 5746 (TLS Renegotiation Indication Extension)
    CH_RENEGOTIATION_INFO   (0xff01, "renegotiation_info",
                                SSLHandshake.CLIENT_HELLO,
                                ProtocolVersion.PROTOCOLS_TO_12,
                                RenegoInfoExtension.chNetworkProducer,
                                RenegoInfoExtension.chOnLoadConsumer,
                                RenegoInfoExtension.chOnLoadAbsence,
                                null,
                                null,
                                RenegoInfoExtension.rniStringizer),
    SH_RENEGOTIATION_INFO   (0xff01, "renegotiation_info",
                                SSLHandshake.SERVER_HELLO,
                                ProtocolVersion.PROTOCOLS_TO_12,
                                RenegoInfoExtension.shNetworkProducer,
                                RenegoInfoExtension.shOnLoadConsumer,
                                RenegoInfoExtension.shOnLoadAbsence,
                                null,
                                null,
                                RenegoInfoExtension.rniStringizer),

    // RFC 8446 (TLS Protocol Version 1.3) PSK extension must be last
    CH_PRE_SHARED_KEY       (0x0029, "pre_shared_key",
                                SSLHandshake.CLIENT_HELLO,
                                ProtocolVersion.PROTOCOLS_OF_13,
                                PreSharedKeyExtension.chNetworkProducer,
                                PreSharedKeyExtension.chOnLoadConsumer,
                                PreSharedKeyExtension.chOnLoadAbsence,
                                PreSharedKeyExtension.chOnTradeConsumer,
                                PreSharedKeyExtension.chOnTradAbsence,
                                PreSharedKeyExtension.chStringizer),
    SH_PRE_SHARED_KEY       (0x0029, "pre_shared_key",
                                SSLHandshake.SERVER_HELLO,
                                ProtocolVersion.PROTOCOLS_OF_13,
                                PreSharedKeyExtension.shNetworkProducer,
                                PreSharedKeyExtension.shOnLoadConsumer,
                                PreSharedKeyExtension.shOnLoadAbsence,
                                null, null,
                                PreSharedKeyExtension.shStringizer);

    final int id;
    final SSLHandshake handshakeType;
    final String name;
    final ProtocolVersion[] supportedProtocols;

    /*
     * networkProducer: produces outbound handshake data.
     *
     * onLoadConsumer:  parses inbound data.  It may not be appropriate
     *                  to act until all of the message inputs have
     *                  been parsed.  (e.g. parsing keyShares and choosing
     *                  a local value without having seen the SupportedGroups
     *                  extension.)
     *
     * onLoadAbsence:   if a missing message needs special handling
     *                  during the load phase.
     *
     * onTradeConsumer: act on the parsed message once all inbound data has
     *                  been traded and parsed.
     *
     * onTradeAbsence:  if a missing message needs special handling
     *                  during the trade phase.
     */
    final HandshakeProducer networkProducer;
    final ExtensionConsumer onLoadConsumer;
    final HandshakeAbsence  onLoadAbsence;
    final HandshakeConsumer onTradeConsumer;
    final HandshakeAbsence  onTradeAbsence;
    final SSLStringizer stringizer;

    // known but unsupported extension
    private SSLExtension(int id, String name) {
        this.id = id;
        this.handshakeType = SSLHandshake.NOT_APPLICABLE;
        this.name = name;
        this.supportedProtocols = new ProtocolVersion[0];
        this.networkProducer = null;
        this.onLoadConsumer = null;
        this.onLoadAbsence = null;
        this.onTradeConsumer = null;
        this.onTradeAbsence = null;
        this.stringizer = null;
    }

    // supported extension
    private SSLExtension(int id, String name, SSLHandshake handshakeType,
            ProtocolVersion[] supportedProtocols,
            HandshakeProducer producer,
            ExtensionConsumer onLoadConsumer, HandshakeAbsence onLoadAbsence,
            HandshakeConsumer onTradeConsumer, HandshakeAbsence onTradeAbsence,
            SSLStringizer stringize) {
        this.id = id;
        this.handshakeType = handshakeType;
        this.name = name;
        this.supportedProtocols = supportedProtocols;
        this.networkProducer = producer;
        this.onLoadConsumer = onLoadConsumer;
        this.onLoadAbsence = onLoadAbsence;
        this.onTradeConsumer = onTradeConsumer;
        this.onTradeAbsence = onTradeAbsence;
        this.stringizer = stringize;
    }

    static SSLExtension valueOf(SSLHandshake handshakeType, int extensionType) {
        for (SSLExtension ext : SSLExtension.values()) {
            if (ext.id == extensionType &&
                    ext.handshakeType == handshakeType) {
                return ext;
            }
        }

        return null;
    }

    static String nameOf(int extensionType) {
        for (SSLExtension ext : SSLExtension.values()) {
            if (ext.id == extensionType) {
                return ext.name;
            }
        }

        return "unknown extension";
    }

    static boolean isConsumable(int extensionType) {
        for (SSLExtension ext : SSLExtension.values()) {
            if (ext.id == extensionType &&
                    ext.onLoadConsumer != null) {
                return true;
            }
        }

        return false;
    }

    public byte[] produce(ConnectionContext context,
            HandshakeMessage message) throws IOException {
        if (networkProducer != null) {
            return networkProducer.produce(context, message);
        } else {
            throw new UnsupportedOperationException(
                    "Not yet supported extension producing.");
        }
    }

    public void consumeOnLoad(ConnectionContext context,
            HandshakeMessage message, ByteBuffer buffer) throws IOException {
        if (onLoadConsumer != null) {
            onLoadConsumer.consume(context, message, buffer);
        } else {
            throw new UnsupportedOperationException(
                    "Not yet supported extension loading.");
        }
    }

    public void consumeOnTrade(ConnectionContext context,
            HandshakeMessage message) throws IOException {
        if (onTradeConsumer != null) {
            onTradeConsumer.consume(context, message);
        } else {
            throw new UnsupportedOperationException(
                    "Not yet supported extension processing.");
        }
    }

    void absentOnLoad(ConnectionContext context,
            HandshakeMessage message) throws IOException {
        if (onLoadAbsence != null) {
            onLoadAbsence.absent(context, message);
        } else {
            throw new UnsupportedOperationException(
                    "Not yet supported extension absence processing.");
        }
    }

    void absentOnTrade(ConnectionContext context,
            HandshakeMessage message) throws IOException {
        if (onTradeAbsence != null) {
            onTradeAbsence.absent(context, message);
        } else {
            throw new UnsupportedOperationException(
                    "Not yet supported extension absence processing.");
        }
    }

    public boolean isAvailable(ProtocolVersion protocolVersion) {
        for (ProtocolVersion supportedProtocol : supportedProtocols) {
            if (supportedProtocol == protocolVersion) {
                return true;
            }
        }

        return false;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public String toString(
            HandshakeContext handshakeContext, ByteBuffer byteBuffer) {
        MessageFormat messageFormat = new MessageFormat(
            "\"{0} ({1})\": '{'\n" +
            "{2}\n" +
            "'}'",
            Locale.ENGLISH);

        String extData;
        if (stringizer == null) {
            HexDumpEncoder hexEncoder = new HexDumpEncoder();
            extData = hexEncoder.encode(byteBuffer.duplicate());
        } else {
            extData = stringizer.toString(handshakeContext, byteBuffer);
        }

        Object[] messageFields = {
            this.name,
            this.id,
            Utilities.indent(extData)
        };

        return messageFormat.format(messageFields);
    }

    //////////////////////////////////////////////////////
    // Nested extension, consumer and producer interfaces.

    static interface ExtensionConsumer {
        void consume(ConnectionContext context,
                HandshakeMessage message, ByteBuffer buffer) throws IOException;
    }

    /**
     * A (transparent) specification of extension data.
     *
     * This interface contains no methods or constants. Its only purpose is to
     * group all extension data.  All extension data should implement this
     * interface if the data is expected to handle in the following handshake
     * processes.
     */
    static interface SSLExtensionSpec {
        // blank
    }

    // Default enabled client extensions.
    static final class ClientExtensions {
        static final Collection<SSLExtension> defaults;

        static {
            Collection<String> clientDisabledExtensions =
                    getDisabledExtensions("jdk.tls.client.disableExtensions");
            Collection<SSLExtension> extensions = new LinkedList<>();
            for (SSLExtension extension : SSLExtension.values()) {
                if (extension.handshakeType != SSLHandshake.NOT_APPLICABLE &&
                        !clientDisabledExtensions.contains(extension.name)) {
                    extensions.add(extension);
                }
            }

            // Switch off SNI extension?
            if (extensions.contains(CH_SERVER_NAME)) {
                boolean enableExtension = Utilities.getBooleanProperty(
                        "jsse.enableSNIExtension", true);
                if (!enableExtension) {
                    extensions.remove(CH_SERVER_NAME);
                }
            }

            // To switch off the max_fragment_length extension.
            //
            // Note that "jsse.enableMFLNExtension" is the CSR approved
            // property name.  However, "jsse.enableMFLExtension" was used
            // in the original implementation.  Temporarily, if either of
            // the two properties set to true, the extension is switch on.
            // We may remove the "jsse.enableMFLExtension" property in the
            // future.  Please don't continue to use the misspelling property.
            if (extensions.contains(CH_MAX_FRAGMENT_LENGTH)) {
                boolean enableExtension =
                        Utilities.getBooleanProperty(
                                "jsse.enableMFLNExtension", false) ||
                        Utilities.getBooleanProperty(
                                "jsse.enableMFLExtension", false);
                if (!enableExtension) {
                    extensions.remove(CH_MAX_FRAGMENT_LENGTH);
                }
            }

            // To switch on certificate_authorities extension in ClientHello.
            //
            // Note: Please be careful to enable this extension in ClientHello.
            //
            // In practice, if the server certificate cannot be validated by
            // the underlying programs, the user may manually check the
            // certificate in order to access the service.  The certificate
            // could be accepted manually, and the handshake continues.  For
            // example, the browsers provide the manual option to accept
            // untrusted server certificate. If this extension is enabled in
            // the ClientHello handshake message, and the server's certificate
            // does not chain back to any of the CAs in the extension, then the
            // server will terminate the handshake and close the connection.
            // There is no chance for the client to perform the manual check.
            // Therefore, enabling this extension in ClientHello may lead to
            // unexpected compatibility issues for such cases.
            //
            // According to TLS 1.3 specification [RFC 8446] the maximum size
            // of the certificate_authorities extension is 2^16 bytes.  The
            // maximum TLS record size is 2^14 bytes.  If the handshake
            // message is bigger than maximum TLS record size, it should be
            // splitted into several records.  In fact, some server
            // implementations do not allow ClientHello messages bigger than
            // the maximum TLS record size and will immediately abort the
            // connection with a fatal alert.  Therefore, if the client trusts
            // too many certificate authorities, there may be unexpected
            // interoperability issues.
            //
            // Furthermore, if the client trusts more CAs such that it exceeds
            // the size limit of the extension, enabling this extension in
            // client side does not really make sense any longer as there is
            // no way to indicate the server certificate selection accurately.
            //
            // In general, a server does not use multiple certificates issued
            // from different CAs.  It is not expected to use this extension a
            // lot in practice.  When there is a need to use this extension
            // in ClientHello handshake message, please take care of the
            // potential compatibility and interoperability issues above.
            if (extensions.contains(CH_CERTIFICATE_AUTHORITIES)) {
                boolean enableExtension = Utilities.getBooleanProperty(
                        "jdk.tls.client.enableCAExtension", false);
                if (!enableExtension) {
                    extensions.remove(CH_CERTIFICATE_AUTHORITIES);
                }
            }

            defaults = Collections.unmodifiableCollection(extensions);
        }
    }

    // Default enabled server extensions.
    static final class ServerExtensions {
        static final Collection<SSLExtension> defaults;

        static {
            Collection<String> serverDisabledExtensions =
                    getDisabledExtensions("jdk.tls.server.disableExtensions");
            Collection<SSLExtension> extensions = new LinkedList<>();
            for (SSLExtension extension : SSLExtension.values()) {
                if (extension.handshakeType != SSLHandshake.NOT_APPLICABLE &&
                        !serverDisabledExtensions.contains(extension.name)) {
                    extensions.add(extension);
                }
            }

            defaults = Collections.unmodifiableCollection(extensions);
        }
    }

    // Get disabled extensions, which could be customized with System Properties.
    private static Collection<String> getDisabledExtensions(
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
            String[] extensionNames = property.split(",");
            Collection<String> extensions =
                    new ArrayList<>(extensionNames.length);
            for (String extension : extensionNames) {
                extension = extension.trim();
                if (!extension.isEmpty()) {
                    extensions.add(extension);
                }
            }

            return extensions;
        }

        return Collections.emptyList();
    }
}
