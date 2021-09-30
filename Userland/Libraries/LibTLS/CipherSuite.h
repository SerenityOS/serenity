/*
 * Copyright (c) 2020, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>

namespace TLS {

enum class CipherSuite {
    Invalid = 0,

    // Weak cipher suites, but we support them

    // RFC 5246 - Original TLS v1.2 ciphers
    RSA_WITH_AES_128_CBC_SHA = 0x002F,
    RSA_WITH_AES_256_CBC_SHA = 0x0035,
    RSA_WITH_AES_128_CBC_SHA256 = 0x003C,
    RSA_WITH_AES_256_CBC_SHA256 = 0x003D,

    // RFC 5288 - DH, DHE and RSA for AES-GCM
    RSA_WITH_AES_128_GCM_SHA256 = 0x009C,
    RSA_WITH_AES_256_GCM_SHA384 = 0x009D,

    // Secure cipher suites, but not recommended

    // RFC 5288 - DH, DHE and RSA for AES-GCM
    DHE_RSA_WITH_AES_128_GCM_SHA256 = 0x009E,
    DHE_RSA_WITH_AES_256_GCM_SHA384 = 0x009F,

    // All recommended cipher suites (according to https://ciphersuite.info/cs/)

    // RFC 5288 - DH, DHE and RSA for AES-GCM
    DHE_DSS_WITH_AES_128_GCM_SHA256 = 0x00A2,
    DHE_DSS_WITH_AES_256_GCM_SHA384 = 0x00A3,

    // RFC 5289 - ECDHE for AES-GCM
    ECDHE_ECDSA_WITH_AES_128_GCM_SHA256 = 0xC02B,
    ECDHE_ECDSA_WITH_AES_256_GCM_SHA384 = 0xC02C,

    // RFC 5487 - Pre-shared keys
    DHE_PSK_WITH_AES_128_GCM_SHA256 = 0x00AA,
    DHE_PSK_WITH_AES_256_GCM_SHA384 = 0x00AB,

    // RFC 6209 - ARIA suites
    DHE_DSS_WITH_ARIA_128_GCM_SHA256 = 0xC056,
    DHE_DSS_WITH_ARIA_256_GCM_SHA384 = 0xC057,
    ECDHE_ECDSA_WITH_ARIA_128_GCM_SHA256 = 0xC05C,
    ECDHE_ECDSA_WITH_ARIA_256_GCM_SHA384 = 0xC05D,
    DHE_PSK_WITH_ARIA_128_GCM_SHA256 = 0xC06C,
    DHE_PSK_WITH_ARIA_256_GCM_SHA384 = 0xC06D,

    // RFC 6367 - Camellia Cipher Suites
    DHE_DSS_WITH_CAMELLIA_128_GCM_SHA256 = 0xC080,
    DHE_DSS_WITH_CAMELLIA_256_GCM_SHA384 = 0xC081,
    ECDHE_ECDSA_WITH_CAMELLIA_128_GCM_SHA256 = 0xC086,
    ECDHE_ECDSA_WITH_CAMELLIA_256_GCM_SHA384 = 0xC087,
    DHE_PSK_WITH_CAMELLIA_128_GCM_SHA256 = 0xC090,
    DHE_PSK_WITH_CAMELLIA_256_GCM_SHA384 = 0xC091,

    // RFC 6655 - DHE, PSK and RSA with AES-CCM
    DHE_PSK_WITH_AES_128_CCM = 0xC0A6,
    DHE_PSK_WITH_AES_256_CCM = 0xC0A7,

    // RFC 7251 - ECDHE with AES-CCM
    ECDHE_ECDSA_WITH_AES_128_CCM = 0xC0AC,
    ECDHE_ECDSA_WITH_AES_256_CCM = 0xC0AD,
    ECDHE_ECDSA_WITH_AES_128_CCM_8 = 0xC0AE,
    ECDHE_ECDSA_WITH_AES_256_CCM_8 = 0xC0AF,

    // RFC 7905 - ChaCha20-Poly1305 Cipher Suites
    ECDHE_ECDSA_WITH_CHACHA20_POLY1305_SHA256 = 0xCCA9,
    ECDHE_PSK_WITH_CHACHA20_POLY1305_SHA256 = 0xCCAC,
    DHE_PSK_WITH_CHACHA20_POLY1305 = 0xCCAD,

    // RFC 8442 - ECDHE_PSK with AES-GCM and AES-CCM
    ECDHE_PSK_WITH_AES_128_GCM_SHA256 = 0xD001,
    ECDHE_PSK_WITH_AES_256_GCM_SHA384 = 0xD002,
    ECDHE_PSK_WITH_AES_128_CCM_8_SHA256 = 0xD003,
    ECDHE_PSK_WITH_AES_128_CCM_SHA256 = 0xD005,

    // RFC 8446 - TLS v1.3
    AES_128_GCM_SHA256 = 0x1301,
    AES_256_GCM_SHA384 = 0x1302,
    CHACHA20_POLY1305_SHA256 = 0x1303,
    AES_128_CCM_SHA256 = 0x1304,
    AES_128_CCM_8_SHA256 = 0x1305,
};

// Defined in RFC 5246 section 7.4.1.4.1
enum class HashAlgorithm : u8 {
    None = 0,
    MD5 = 1,
    SHA1 = 2,
    SHA224 = 3,
    SHA256 = 4,
    SHA384 = 5,
    SHA512 = 6,
};

// Defined in RFC 5246 section 7.4.1.4.1
enum class SignatureAlgorithm : u8 {
    Anonymous = 0,
    RSA = 1,
    DSA = 2,
    ECDSA = 3,
};

// Defined in RFC 5246 section 7.4.1.4.1
struct SignatureAndHashAlgorithm {
    HashAlgorithm hash;
    SignatureAlgorithm signature;
};

enum class KeyExchangeAlgorithm {
    Invalid,
    // Defined in RFC 5246 section 7.4.2 / RFC 4279 section 4
    RSA_PSK,
    // Defined in RFC 5246 section 7.4.3
    DHE_DSS,
    DHE_RSA,
    DH_anon,
    RSA,
    DH_DSS,
    DH_RSA,
    // Defined in RFC 4492 section 2
    ECDHE_RSA,
    ECDH_ECDSA,
    ECDH_RSA,
    ECDHE_ECDSA,
    ECDH_anon,
};

// Defined in RFC 5246 section 7.4.1.4.1
constexpr SignatureAlgorithm signature_for_key_exchange_algorithm(KeyExchangeAlgorithm algorithm)
{
    switch (algorithm) {
    case KeyExchangeAlgorithm::RSA:
    case KeyExchangeAlgorithm::DHE_RSA:
    case KeyExchangeAlgorithm::DH_RSA:
    case KeyExchangeAlgorithm::RSA_PSK:
    case KeyExchangeAlgorithm::ECDH_RSA:
    case KeyExchangeAlgorithm::ECDHE_RSA:
        return SignatureAlgorithm::RSA;
    case KeyExchangeAlgorithm::DHE_DSS:
    case KeyExchangeAlgorithm::DH_DSS:
        return SignatureAlgorithm::DSA;
    case KeyExchangeAlgorithm::ECDH_ECDSA:
    case KeyExchangeAlgorithm::ECDHE_ECDSA:
        return SignatureAlgorithm::ECDSA;
    case KeyExchangeAlgorithm::DH_anon:
    case KeyExchangeAlgorithm::ECDH_anon:
    default:
        return SignatureAlgorithm::Anonymous;
    }
}

enum class CipherAlgorithm {
    Invalid,
    AES_128_CBC,
    AES_128_GCM,
    AES_128_CCM,
    AES_128_CCM_8,
    AES_256_CBC,
    AES_256_GCM,
};

constexpr size_t cipher_key_size(CipherAlgorithm algorithm)
{
    switch (algorithm) {
    case CipherAlgorithm::AES_128_CBC:
    case CipherAlgorithm::AES_128_GCM:
    case CipherAlgorithm::AES_128_CCM:
    case CipherAlgorithm::AES_128_CCM_8:
        return 128;
    case CipherAlgorithm::AES_256_CBC:
    case CipherAlgorithm::AES_256_GCM:
        return 256;
    case CipherAlgorithm::Invalid:
    default:
        return 0;
    }
}

}
