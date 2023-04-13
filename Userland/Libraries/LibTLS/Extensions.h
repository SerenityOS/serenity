/*
 * Copyright (c) 2020, Ali Mohammad Pur <mpfard@serenityos.org>
 * Copyright (c) 2023, stelar7 <dudedbz@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>

namespace TLS {

#define _ENUM_KEY(name) name,
#define _ENUM_KEY_VALUE(name, value) name = value,

// https://www.iana.org/assignments/tls-parameters/tls-parameters.xhtml#tls-parameters-5
#define __ENUM_CONTENT_TYPES                \
    _ENUM_KEY_VALUE(CHANGE_CIPHER_SPEC, 20) \
    _ENUM_KEY_VALUE(ALERT, 21)              \
    _ENUM_KEY_VALUE(HANDSHAKE, 22)          \
    _ENUM_KEY_VALUE(APPLICATION_DATA, 23)   \
    _ENUM_KEY_VALUE(HEARTBEAT, 24)          \
    _ENUM_KEY_VALUE(TLS12_CID, 25)          \
    _ENUM_KEY_VALUE(ACK, 26)

enum class ContentType : u8 {
    __ENUM_CONTENT_TYPES
};

#define __ENUM_PROTOCOL_VERSIONS         \
    _ENUM_KEY_VALUE(VERSION_1_3, 0x0304) \
    _ENUM_KEY_VALUE(VERSION_1_2, 0x0303) \
    _ENUM_KEY_VALUE(VERSION_1_1, 0x0302) \
    _ENUM_KEY_VALUE(VERSION_1_0, 0x0301) \
    _ENUM_KEY_VALUE(GREASE_0, 0x0A0A)    \
    _ENUM_KEY_VALUE(GREASE_1, 0x1A1A)    \
    _ENUM_KEY_VALUE(GREASE_2, 0x2A2A)    \
    _ENUM_KEY_VALUE(GREASE_3, 0x3A3A)    \
    _ENUM_KEY_VALUE(GREASE_4, 0x4A4A)    \
    _ENUM_KEY_VALUE(GREASE_5, 0x5A5A)    \
    _ENUM_KEY_VALUE(GREASE_6, 0x6A6A)    \
    _ENUM_KEY_VALUE(GREASE_7, 0x7A7A)    \
    _ENUM_KEY_VALUE(GREASE_8, 0x8A8A)    \
    _ENUM_KEY_VALUE(GREASE_9, 0x9A9A)    \
    _ENUM_KEY_VALUE(GREASE_A, 0xAAAA)    \
    _ENUM_KEY_VALUE(GREASE_B, 0xBABA)    \
    _ENUM_KEY_VALUE(GREASE_C, 0xCACA)    \
    _ENUM_KEY_VALUE(GREASE_D, 0xDADA)    \
    _ENUM_KEY_VALUE(GREASE_E, 0xEAEA)    \
    _ENUM_KEY_VALUE(GREASE_F, 0xFAFA)

enum class ProtocolVersion : u16 {
    __ENUM_PROTOCOL_VERSIONS
};

#define __ENUM_ALERT_LEVELS     \
    _ENUM_KEY_VALUE(WARNING, 1) \
    _ENUM_KEY_VALUE(FATAL, 2)

enum class AlertLevel : u8 {
    __ENUM_ALERT_LEVELS
};

// https://www.iana.org/assignments/tls-parameters/tls-parameters.xhtml#tls-parameters-7
#define __ENUM_HANDSHAKE_TYPES                        \
    _ENUM_KEY_VALUE(HELLO_REQUEST_RESERVED, 0)        \
    _ENUM_KEY_VALUE(CLIENT_HELLO, 1)                  \
    _ENUM_KEY_VALUE(SERVER_HELLO, 2)                  \
    _ENUM_KEY_VALUE(HELLO_VERIFY_REQUEST_RESERVED, 3) \
    _ENUM_KEY_VALUE(NEW_SESSION_TICKET, 4)            \
    _ENUM_KEY_VALUE(END_OF_EARLY_DATA, 5)             \
    _ENUM_KEY_VALUE(HELLO_RETRY_REQUEST_RESERVED, 6)  \
    _ENUM_KEY_VALUE(ENCRYPTED_EXTENSIONS, 8)          \
    _ENUM_KEY_VALUE(REQUEST_CONNECTION_ID, 9)         \
    _ENUM_KEY_VALUE(NEW_CONNECTION_ID, 10)            \
    _ENUM_KEY_VALUE(CERTIFICATE, 11)                  \
    _ENUM_KEY_VALUE(SERVER_KEY_EXCHANGE_RESERVED, 12) \
    _ENUM_KEY_VALUE(CERTIFICATE_REQUEST, 13)          \
    _ENUM_KEY_VALUE(SERVER_HELLO_DONE_RESERVED, 14)   \
    _ENUM_KEY_VALUE(CERTIFICATE_VERIFY, 15)           \
    _ENUM_KEY_VALUE(CLIENT_KEY_EXCHANGE_RESERVED, 16) \
    _ENUM_KEY_VALUE(FINISHED, 20)                     \
    _ENUM_KEY_VALUE(CERTIFICATE_URL_RESERVED, 21)     \
    _ENUM_KEY_VALUE(CERTIFICATE_STATUS_RESERVED, 22)  \
    _ENUM_KEY_VALUE(SUPPLEMENTAL_DATA_RESERVED, 23)   \
    _ENUM_KEY_VALUE(KEY_UPDATE, 24)                   \
    _ENUM_KEY_VALUE(COMPRESSED_CERTIFICATE, 25)       \
    _ENUM_KEY_VALUE(EKT_KEY, 26)                      \
    _ENUM_KEY_VALUE(MESSAGE_HASH, 254)

enum class HandshakeType : u8 {
    __ENUM_HANDSHAKE_TYPES
};

// https://www.iana.org/assignments/tls-extensiontype-values/tls-extensiontype-values.xhtml#tls-extensiontype-values-1
#define __ENUM_EXTENSION_TYPES                                  \
    _ENUM_KEY_VALUE(SERVER_NAME, 0)                             \
    _ENUM_KEY_VALUE(MAX_FRAGMENT_LENGTH, 1)                     \
    _ENUM_KEY_VALUE(CLIENT_CERTIFICATE_URL, 2)                  \
    _ENUM_KEY_VALUE(TRUSTED_CA_KEYS, 3)                         \
    _ENUM_KEY_VALUE(TRUNCATED_HMAC, 4)                          \
    _ENUM_KEY_VALUE(STATUS_REQUEST, 5)                          \
    _ENUM_KEY_VALUE(USER_MAPPING, 6)                            \
    _ENUM_KEY_VALUE(CLIENT_AUTHZ, 7)                            \
    _ENUM_KEY_VALUE(SERVER_AUTHZ, 8)                            \
    _ENUM_KEY_VALUE(CERT_TYPE, 9)                               \
    _ENUM_KEY_VALUE(SUPPORTED_GROUPS, 10)                       \
    _ENUM_KEY_VALUE(EC_POINT_FORMATS, 11)                       \
    _ENUM_KEY_VALUE(SRP, 12)                                    \
    _ENUM_KEY_VALUE(SIGNATURE_ALGORITHMS, 13)                   \
    _ENUM_KEY_VALUE(USE_SRTP, 14)                               \
    _ENUM_KEY_VALUE(HEARTBEAT, 15)                              \
    _ENUM_KEY_VALUE(APPLICATION_LAYER_PROTOCOL_NEGOTIATION, 16) \
    _ENUM_KEY_VALUE(STATUS_REQUEST_V2, 17)                      \
    _ENUM_KEY_VALUE(SIGNED_CERTIFICATE_TIMESTAMP, 18)           \
    _ENUM_KEY_VALUE(CLIENT_CERTIFICATE_TYPE, 19)                \
    _ENUM_KEY_VALUE(SERVER_CERTIFICATE_TYPE, 20)                \
    _ENUM_KEY_VALUE(PADDING, 21)                                \
    _ENUM_KEY_VALUE(ENCRYPT_THEN_MAC, 22)                       \
    _ENUM_KEY_VALUE(EXTENDED_MASTER_SECRET, 23)                 \
    _ENUM_KEY_VALUE(TOKEN_BINDING, 24)                          \
    _ENUM_KEY_VALUE(CACHED_INFO, 25)                            \
    _ENUM_KEY_VALUE(TLS_LTS, 26)                                \
    _ENUM_KEY_VALUE(COMPRESS_CERTIFICATE, 27)                   \
    _ENUM_KEY_VALUE(RECORD_SIZE_LIMIT, 28)                      \
    _ENUM_KEY_VALUE(PWD_PROTECT, 29)                            \
    _ENUM_KEY_VALUE(PWD_CLEAR, 30)                              \
    _ENUM_KEY_VALUE(PASSWORD_SALT, 31)                          \
    _ENUM_KEY_VALUE(TICKET_PINNING, 32)                         \
    _ENUM_KEY_VALUE(TLS_CERT_WITH_EXTERN_PSK, 33)               \
    _ENUM_KEY_VALUE(DELEGATED_CREDENTIALS, 34)                  \
    _ENUM_KEY_VALUE(SESSION_TICKET, 35)                         \
    _ENUM_KEY_VALUE(TLMSP, 36)                                  \
    _ENUM_KEY_VALUE(TLMSP_PROXYING, 37)                         \
    _ENUM_KEY_VALUE(TLMSP_DELEGATE, 38)                         \
    _ENUM_KEY_VALUE(SUPPORTED_EKT_CIPHERS, 39)                  \
    _ENUM_KEY_VALUE(PRE_SHARED_KEY, 41)                         \
    _ENUM_KEY_VALUE(EARLY_DATA, 42)                             \
    _ENUM_KEY_VALUE(SUPPORTED_VERSIONS, 43)                     \
    _ENUM_KEY_VALUE(COOKIE, 44)                                 \
    _ENUM_KEY_VALUE(PSK_KEY_EXCHANGE_MODES, 45)                 \
    _ENUM_KEY_VALUE(CERTIFICATE_AUTHORITIES, 47)                \
    _ENUM_KEY_VALUE(OID_FILTERS, 48)                            \
    _ENUM_KEY_VALUE(POST_HANDSHAKE_AUTH, 49)                    \
    _ENUM_KEY_VALUE(SIGNATURE_ALGORITHMS_CERT, 50)              \
    _ENUM_KEY_VALUE(KEY_SHARE, 51)                              \
    _ENUM_KEY_VALUE(TRANSPARENCY_INFO, 52)                      \
    _ENUM_KEY_VALUE(CONNECTION_ID_DEPRECATED, 53)               \
    _ENUM_KEY_VALUE(CONNECTION_ID, 54)                          \
    _ENUM_KEY_VALUE(EXTERNAL_ID_HASH, 55)                       \
    _ENUM_KEY_VALUE(EXTERNAL_SESSION_ID, 56)                    \
    _ENUM_KEY_VALUE(QUIC_TRANSPORT_PARAMETERS, 57)              \
    _ENUM_KEY_VALUE(TICKET_REQUEST, 58)                         \
    _ENUM_KEY_VALUE(DNSSEC_CHAIN, 59)                           \
    _ENUM_KEY_VALUE(RENEGOTIATION_INFO, 65281)

enum class ExtensionType : u16 {
    __ENUM_EXTENSION_TYPES
};

#define __ENUM_NAME_TYPES \
    _ENUM_KEY_VALUE(HOST_NAME, 0)

enum class NameType : u8 {
    __ENUM_NAME_TYPES
};

// https://www.iana.org/assignments/tls-parameters/tls-parameters.xhtml#tls-parameters-10
#define __ENUM_EC_CURVE_TYPES          \
    _ENUM_KEY_VALUE(EXPLICIT_PRIME, 1) \
    _ENUM_KEY_VALUE(EXPLICIT_CHAR2, 2) \
    _ENUM_KEY_VALUE(NAMED_CURVE, 3)

enum class ECCurveType : u8 {
    __ENUM_EC_CURVE_TYPES
};

// https://www.iana.org/assignments/tls-parameters/tls-parameters.xhtml#tls-parameters-8
#define __ENUM_SUPPORTED_GROUPS                              \
    _ENUM_KEY_VALUE(SECT163K1, 0x0001)                       \
    _ENUM_KEY_VALUE(SECT163R1, 0x0002)                       \
    _ENUM_KEY_VALUE(SECT163R2, 0x0003)                       \
    _ENUM_KEY_VALUE(SECT193R1, 0x0004)                       \
    _ENUM_KEY_VALUE(SECT193R2, 0x0005)                       \
    _ENUM_KEY_VALUE(SECT233K1, 0x0006)                       \
    _ENUM_KEY_VALUE(SECT233R1, 0x0007)                       \
    _ENUM_KEY_VALUE(SECT239K1, 0x0008)                       \
    _ENUM_KEY_VALUE(SECT283K1, 0x0009)                       \
    _ENUM_KEY_VALUE(SECT283R1, 0x000a)                       \
    _ENUM_KEY_VALUE(SECT409K1, 0x000b)                       \
    _ENUM_KEY_VALUE(SECT409R1, 0x000c)                       \
    _ENUM_KEY_VALUE(SECT571K1, 0x000d)                       \
    _ENUM_KEY_VALUE(SECT571R1, 0x000e)                       \
    _ENUM_KEY_VALUE(SECP160K1, 0x000f)                       \
    _ENUM_KEY_VALUE(SECP160R1, 0x0010)                       \
    _ENUM_KEY_VALUE(SECP160R2, 0x0011)                       \
    _ENUM_KEY_VALUE(SECP192K1, 0x0012)                       \
    _ENUM_KEY_VALUE(SECP192R1, 0x0013)                       \
    _ENUM_KEY_VALUE(SECP224K1, 0x0014)                       \
    _ENUM_KEY_VALUE(SECP224R1, 0x0015)                       \
    _ENUM_KEY_VALUE(SECP256K1, 0x0016)                       \
    _ENUM_KEY_VALUE(SECP256R1, 0x0017)                       \
    _ENUM_KEY_VALUE(SECP384R1, 0x0018)                       \
    _ENUM_KEY_VALUE(SECP521R1, 0x0019)                       \
    _ENUM_KEY_VALUE(BRAINPOOLP256R1, 0x001a)                 \
    _ENUM_KEY_VALUE(BRAINPOOLP384R1, 0x001b)                 \
    _ENUM_KEY_VALUE(BRAINPOOLP512R1, 0x001c)                 \
    _ENUM_KEY_VALUE(X25519, 0x001d)                          \
    _ENUM_KEY_VALUE(X448, 0x001e)                            \
    _ENUM_KEY_VALUE(BRAINPOOLP256R1TLS13, 0x001f)            \
    _ENUM_KEY_VALUE(BRAINPOOLP384R1TLS13, 0x0020)            \
    _ENUM_KEY_VALUE(BRAINPOOLP512R1TLS13, 0x0021)            \
    _ENUM_KEY_VALUE(GC256A, 0x0022)                          \
    _ENUM_KEY_VALUE(GC256B, 0x0023)                          \
    _ENUM_KEY_VALUE(GC256C, 0x0024)                          \
    _ENUM_KEY_VALUE(GC256D, 0x0025)                          \
    _ENUM_KEY_VALUE(GC512A, 0x0026)                          \
    _ENUM_KEY_VALUE(GC512B, 0x0027)                          \
    _ENUM_KEY_VALUE(GC512C, 0x0028)                          \
    _ENUM_KEY_VALUE(CURVESM2, 0x0029)                        \
    _ENUM_KEY_VALUE(FFDHE2048, 0x0100)                       \
    _ENUM_KEY_VALUE(FFDHE3072, 0x0101)                       \
    _ENUM_KEY_VALUE(FFDHE4096, 0x0102)                       \
    _ENUM_KEY_VALUE(FFDHE6144, 0x0103)                       \
    _ENUM_KEY_VALUE(FFDHE8192, 0x0104)                       \
    _ENUM_KEY_VALUE(ARBITRARY_EXPLICIT_PRIME_CURVES, 0xff01) \
    _ENUM_KEY_VALUE(ARBITRARY_EXPLICIT_CHAR2_CURVES, 0xff02) \
    _ENUM_KEY_VALUE(GREASE_0, 0x0A0A)                        \
    _ENUM_KEY_VALUE(GREASE_1, 0x1A1A)                        \
    _ENUM_KEY_VALUE(GREASE_2, 0x2A2A)                        \
    _ENUM_KEY_VALUE(GREASE_3, 0x3A3A)                        \
    _ENUM_KEY_VALUE(GREASE_4, 0x4A4A)                        \
    _ENUM_KEY_VALUE(GREASE_5, 0x5A5A)                        \
    _ENUM_KEY_VALUE(GREASE_6, 0x6A6A)                        \
    _ENUM_KEY_VALUE(GREASE_7, 0x7A7A)                        \
    _ENUM_KEY_VALUE(GREASE_8, 0x8A8A)                        \
    _ENUM_KEY_VALUE(GREASE_9, 0x9A9A)                        \
    _ENUM_KEY_VALUE(GREASE_A, 0xAAAA)                        \
    _ENUM_KEY_VALUE(GREASE_B, 0xBABA)                        \
    _ENUM_KEY_VALUE(GREASE_C, 0xCACA)                        \
    _ENUM_KEY_VALUE(GREASE_D, 0xDADA)                        \
    _ENUM_KEY_VALUE(GREASE_E, 0xEAEA)                        \
    _ENUM_KEY_VALUE(GREASE_F, 0xFAFA)

enum class SupportedGroup : u16 {
    __ENUM_SUPPORTED_GROUPS
};

// https://www.iana.org/assignments/tls-parameters/tls-parameters.xhtml#tls-parameters-9
#define __ENUM_EC_POINT_FORMATS                   \
    _ENUM_KEY_VALUE(UNCOMPRESSED, 0)              \
    _ENUM_KEY_VALUE(ANSIX962_COMPRESSED_PRIME, 1) \
    _ENUM_KEY_VALUE(ANSIX962_COMPRESSED_CHAR2, 2)

enum class ECPointFormat : u8 {
    __ENUM_EC_POINT_FORMATS
};

// https://www.iana.org/assignments/tls-parameters/tls-parameters.xhtml#tls-parameters-16
#define __ENUM_SIGNATURE_ALGORITHM         \
    _ENUM_KEY_VALUE(ANONYMOUS, 0)          \
    _ENUM_KEY_VALUE(RSA, 1)                \
    _ENUM_KEY_VALUE(DSA, 2)                \
    _ENUM_KEY_VALUE(ECDSA, 3)              \
    _ENUM_KEY_VALUE(ED25519, 7)            \
    _ENUM_KEY_VALUE(ED448, 8)              \
    _ENUM_KEY_VALUE(GOSTR34102012_256, 64) \
    _ENUM_KEY_VALUE(GOSTR34102012_512, 65)

enum class SignatureAlgorithm : u8 {
    __ENUM_SIGNATURE_ALGORITHM
};

#undef _ENUM_KEY
#undef _ENUM_KEY_VALUE

}
