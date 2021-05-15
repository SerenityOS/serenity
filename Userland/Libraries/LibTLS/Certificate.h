/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteBuffer.h>
#include <AK/Forward.h>
#include <AK/Singleton.h>
#include <AK/Types.h>
#include <LibCore/DateTime.h>
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
    u16 version { 0 };
    CertificateKeyAlgorithm algorithm { CertificateKeyAlgorithm::Unsupported };
    CertificateKeyAlgorithm key_algorithm { CertificateKeyAlgorithm::Unsupported };
    CertificateKeyAlgorithm ec_algorithm { CertificateKeyAlgorithm::Unsupported };
    ByteBuffer exponent {};
    Crypto::PK::RSAPublicKey<Crypto::UnsignedBigInteger> public_key {};
    Crypto::PK::RSAPrivateKey<Crypto::UnsignedBigInteger> private_key {};
    struct Name {
        String country;
        String state;
        String location;
        String entity;
        String subject;
        String unit;
    } issuer, subject;
    Core::DateTime not_before;
    Core::DateTime not_after;
    Vector<String> SAN;
    u8* ocsp { nullptr };
    Crypto::UnsignedBigInteger serial_number;
    ByteBuffer sign_key {};
    ByteBuffer fingerprint {};
    ByteBuffer der {};
    ByteBuffer data {};

    bool is_valid() const;
};

class DefaultRootCACertificates {
public:
    DefaultRootCACertificates();

    const Vector<Certificate>& certificates() const { return m_ca_certificates; }

    static DefaultRootCACertificates& the() { return s_the; }

private:
    static AK::Singleton<DefaultRootCACertificates> s_the;

    Vector<Certificate> m_ca_certificates;
};

}

using TLS::Certificate;
using TLS::DefaultRootCACertificates;
