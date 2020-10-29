/*
 * Copyright (c) 2020, the SerenityOS developers.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#pragma once

#include <AK/ByteBuffer.h>
#include <AK/Forward.h>
#include <AK/Types.h>
#include <LibCrypto/BigInt/UnsignedBigInteger.h>
#include <LibCrypto/PK/RSA.h>

namespace TLS {

enum class CertificateKeyAlgorithm {
    Unsupported = 0x00,
    RSA_RSA = 0x01,
    RSA_MD5 = 0x04,
    RSA_SHA1 = 0x05,
    RSA_SHA256 = 0x0b,
    RSA_SHA512 = 0x0d,
};

struct Certificate {
    u16 version;
    CertificateKeyAlgorithm algorithm;
    CertificateKeyAlgorithm key_algorithm;
    CertificateKeyAlgorithm ec_algorithm;
    ByteBuffer exponent;
    Crypto::PK::RSAPublicKey<Crypto::UnsignedBigInteger> public_key;
    Crypto::PK::RSAPrivateKey<Crypto::UnsignedBigInteger> private_key;
    String issuer_country;
    String issuer_state;
    String issuer_location;
    String issuer_entity;
    String issuer_subject;
    String issuer_unit;
    String not_before;
    String not_after;
    String country;
    String state;
    String location;
    String entity;
    String subject;
    String unit;
    u8** SAN;
    u16 SAN_length;
    u8* ocsp;
    Crypto::UnsignedBigInteger serial_number;
    ByteBuffer sign_key;
    ByteBuffer fingerprint;
    ByteBuffer der;
    ByteBuffer data;

    bool is_valid() const;
};

}

using TLS::Certificate;
