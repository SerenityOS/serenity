/*
 * Copyright (c) 2020, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

namespace TLS {

enum class CipherSuite {
    Invalid = 0,
    AES_128_GCM_SHA256 = 0x1301,
    AES_256_GCM_SHA384 = 0x1302,
    AES_128_CCM_SHA256 = 0x1304,
    AES_128_CCM_8_SHA256 = 0x1305,

    // We support these
    RSA_WITH_AES_128_CBC_SHA = 0x002F,
    RSA_WITH_AES_256_CBC_SHA = 0x0035,
    RSA_WITH_AES_128_CBC_SHA256 = 0x003C,
    RSA_WITH_AES_256_CBC_SHA256 = 0x003D,
    // TODO
    RSA_WITH_AES_128_GCM_SHA256 = 0x009C,
    RSA_WITH_AES_256_GCM_SHA384 = 0x009D,
};

enum class HashAlgorithm : u8 {
    None = 0,
    MD5 = 1,
    SHA1 = 2,
    SHA224 = 3,
    SHA256 = 4,
    SHA384 = 5,
    SHA512 = 6,
};

enum class SignatureAlgorithm : u8 {
    Anonymous = 0,
    RSA = 1,
    DSA = 2,
    ECDSA = 3,
};

struct SignatureAndHashAlgorithm {
    HashAlgorithm hash;
    SignatureAlgorithm signature;
};

}
