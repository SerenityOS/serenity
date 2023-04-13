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

#undef _ENUM_KEY
#undef _ENUM_KEY_VALUE

}
