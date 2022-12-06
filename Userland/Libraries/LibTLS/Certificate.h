/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteBuffer.h>
#include <AK/Forward.h>
#include <AK/Optional.h>
#include <AK/Singleton.h>
#include <AK/String.h>
#include <AK/Types.h>
#include <LibCore/ConfigFile.h>
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
    RSA_SHA384 = 0x0c,
    RSA_SHA512 = 0x0d,
};

class Certificate {
public:
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
    CertificateKeyAlgorithm signature_algorithm { CertificateKeyAlgorithm::Unsupported };
    ByteBuffer signature_value {};
    ByteBuffer original_asn1 {};
    bool is_allowed_to_sign_certificate { false };
    bool is_certificate_authority { false };
    Optional<size_t> path_length_constraint {};

    static Optional<Certificate> parse_asn1(ReadonlyBytes, bool client_cert = false);

    bool is_valid() const;

    ErrorOr<String> subject_identifier_string() const
    {
        StringBuilder cert_name;
        if (!subject.country.is_empty()) {
            cert_name.append("/C="sv);
            cert_name.append(subject.country.to_deprecated_string());
        }
        if (!subject.state.is_empty()) {
            cert_name.append("/ST="sv);
            cert_name.append(subject.state.to_deprecated_string());
        }
        if (!subject.location.is_empty()) {
            cert_name.append("/L="sv);
            cert_name.append(subject.location.to_deprecated_string());
        }
        if (!subject.entity.is_empty()) {
            cert_name.append("/O="sv);
            cert_name.append(subject.entity.to_deprecated_string());
        }
        if (!subject.unit.is_empty()) {
            cert_name.append("/OU="sv);
            cert_name.append(subject.unit.to_deprecated_string());
        }
        if (!subject.subject.is_empty()) {
            cert_name.append("/CN="sv);
            cert_name.append(subject.subject.to_deprecated_string());
        }
        return TRY(String::from_deprecated_string(cert_name.build()));
    }

    ErrorOr<String> issuer_identifier_string() const
    {
        StringBuilder cert_name;
        if (!issuer.country.is_empty()) {
            cert_name.append("/C="sv);
            cert_name.append(issuer.country.to_deprecated_string());
        }
        if (!issuer.state.is_empty()) {
            cert_name.append("/ST="sv);
            cert_name.append(issuer.state.to_deprecated_string());
        }
        if (!issuer.location.is_empty()) {
            cert_name.append("/L="sv);
            cert_name.append(issuer.location.to_deprecated_string());
        }
        if (!issuer.entity.is_empty()) {
            cert_name.append("/O="sv);
            cert_name.append(issuer.entity.to_deprecated_string());
        }
        if (!issuer.unit.is_empty()) {
            cert_name.append("/OU="sv);
            cert_name.append(issuer.unit.to_deprecated_string());
        }
        if (!issuer.subject.is_empty()) {
            cert_name.append("/CN="sv);
            cert_name.append(issuer.subject.to_deprecated_string());
        }
        return TRY(String::from_deprecated_string(cert_name.build()));
    }
};

class DefaultRootCACertificates {
public:
    DefaultRootCACertificates();

    Vector<Certificate> const& certificates() const { return m_ca_certificates; }

    void reload_certificates(Core::ConfigFile&);

    static DefaultRootCACertificates& the() { return s_the; }

private:
    static Singleton<DefaultRootCACertificates> s_the;

    Vector<Certificate> m_ca_certificates;
};

}

using TLS::Certificate;
using TLS::DefaultRootCACertificates;
