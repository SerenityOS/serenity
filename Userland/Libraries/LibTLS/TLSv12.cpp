/*
 * Copyright (c) 2020, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Base64.h>
#include <AK/Debug.h>
#include <AK/Endian.h>
#include <LibCore/ConfigFile.h>
#include <LibCore/DateTime.h>
#include <LibCore/File.h>
#include <LibCore/Timer.h>
#include <LibCrypto/ASN1/ASN1.h>
#include <LibCrypto/ASN1/PEM.h>
#include <LibCrypto/PK/Code/EMSA_PKCS1_V1_5.h>
#include <LibCrypto/PK/Code/EMSA_PSS.h>
#include <LibTLS/TLSv12.h>
#include <errno.h>

#ifndef SOCK_NONBLOCK
#    include <sys/ioctl.h>
#endif

namespace TLS {

void TLSv12::consume(ReadonlyBytes record)
{
    if (m_context.critical_error) {
        dbgln("There has been a critical error ({}), refusing to continue", (i8)m_context.critical_error);
        return;
    }

    if (record.size() == 0) {
        return;
    }

    dbgln_if(TLS_DEBUG, "Consuming {} bytes", record.size());

    if (m_context.message_buffer.try_append(record).is_error()) {
        dbgln("Not enough space in message buffer, dropping the record");
        return;
    }

    size_t index { 0 };
    size_t buffer_length = m_context.message_buffer.size();

    size_t size_offset { 3 }; // read the common record header
    size_t header_size { 5 };

    dbgln_if(TLS_DEBUG, "message buffer length {}", buffer_length);

    while (buffer_length >= 5) {
        auto length = AK::convert_between_host_and_network_endian(ByteReader::load16(m_context.message_buffer.offset_pointer(index + size_offset))) + header_size;
        if (length > buffer_length) {
            dbgln_if(TLS_DEBUG, "Need more data: {} > {}", length, buffer_length);
            break;
        }
        auto consumed = handle_message(m_context.message_buffer.bytes().slice(index, length));

        if constexpr (TLS_DEBUG) {
            if (consumed > 0)
                dbgln("consumed {} bytes", consumed);
            else
                dbgln("error: {}", consumed);
        }

        if (consumed != (i8)Error::NeedMoreData) {
            if (consumed < 0) {
                dbgln("Consumed an error: {}", consumed);
                if (!m_context.critical_error)
                    m_context.critical_error = (i8)consumed;
                m_context.error_code = (Error)consumed;
                break;
            }
        } else {
            continue;
        }

        index += length;
        buffer_length -= length;
        if (m_context.critical_error) {
            dbgln("Broken connection");
            m_context.error_code = Error::BrokenConnection;
            break;
        }
    }
    if (m_context.error_code != Error::NoError && m_context.error_code != Error::NeedMoreData) {
        dbgln("consume error: {}", (i8)m_context.error_code);
        m_context.message_buffer.clear();
        return;
    }

    if (index) {
        m_context.message_buffer = m_context.message_buffer.slice(index, m_context.message_buffer.size() - index);
    }
}

bool Certificate::is_valid() const
{
    auto now = Core::DateTime::now();

    if (now < not_before) {
        dbgln("certificate expired (not yet valid, signed for {})", not_before.to_string());
        return false;
    }

    if (not_after < now) {
        dbgln("certificate expired (expiry date {})", not_after.to_string());
        return false;
    }

    return true;
}

void TLSv12::try_disambiguate_error() const
{
    dbgln("Possible failure cause(s): ");
    switch ((AlertDescription)m_context.critical_error) {
    case AlertDescription::HandshakeFailure:
        if (!m_context.cipher_spec_set) {
            dbgln("- No cipher suite in common with {}", m_context.extensions.SNI);
        } else {
            dbgln("- Unknown internal issue");
        }
        break;
    case AlertDescription::InsufficientSecurity:
        dbgln("- No cipher suite in common with {} (the server is oh so secure)", m_context.extensions.SNI);
        break;
    case AlertDescription::ProtocolVersion:
        dbgln("- The server refused to negotiate with TLS 1.2 :(");
        break;
    case AlertDescription::UnexpectedMessage:
        dbgln("- We sent an invalid message for the state we're in.");
        break;
    case AlertDescription::BadRecordMAC:
        dbgln("- Bad MAC record from our side.");
        dbgln("- Ciphertext wasn't an even multiple of the block length.");
        dbgln("- Bad block cipher padding.");
        dbgln("- If both sides are compliant, the only cause is messages being corrupted in the network.");
        break;
    case AlertDescription::RecordOverflow:
        dbgln("- Sent a ciphertext record which has a length bigger than 18432 bytes.");
        dbgln("- Sent record decrypted to a compressed record that has a length bigger than 18432 bytes.");
        dbgln("- If both sides are compliant, the only cause is messages being corrupted in the network.");
        break;
    case AlertDescription::DecompressionFailure:
        dbgln("- We sent invalid input for decompression (e.g. data that would expand to excessive length)");
        break;
    case AlertDescription::IllegalParameter:
        dbgln("- We sent a parameter in the handshake that is out of range or inconsistent with the other parameters.");
        break;
    case AlertDescription::DecodeError:
        dbgln("- The message we sent cannot be decoded because a field was out of range or the length was incorrect.");
        dbgln("- If both sides are compliant, the only cause is messages being corrupted in the network.");
        break;
    case AlertDescription::DecryptError:
        dbgln("- A handshake crypto operation failed. This includes signature verification and validating Finished.");
        break;
    case AlertDescription::AccessDenied:
        dbgln("- The certificate is valid, but once access control was applied, the sender decided to stop negotiation.");
        break;
    case AlertDescription::InternalError:
        dbgln("- No one knows, but it isn't a protocol failure.");
        break;
    case AlertDescription::DecryptionFailed:
    case AlertDescription::NoCertificate:
    case AlertDescription::ExportRestriction:
        dbgln("- No one knows, the server sent a non-compliant alert.");
        break;
    default:
        dbgln("- No one knows.");
        break;
    }
}

void TLSv12::set_root_certificates(Vector<Certificate> certificates)
{
    if (!m_context.root_certificates.is_empty()) {
        dbgln("TLS warn: resetting root certificates!");
        m_context.root_certificates.clear();
    }

    for (auto& cert : certificates) {
        if (!cert.is_valid())
            dbgln("Certificate for {} by {} is invalid, things may or may not work!", cert.subject.subject, cert.issuer.subject);
        // FIXME: Figure out what we should do when our root certs are invalid.

        m_context.root_certificates.set(cert.subject_identifier_string(), cert);
    }
    dbgln_if(TLS_DEBUG, "{}: Set {} root certificates", this, m_context.root_certificates.size());
}

static bool wildcard_matches(StringView host, StringView subject)
{
    if (host == subject)
        return true;

    if (subject.starts_with("*.")) {
        auto maybe_first_dot_index = host.find('.');
        if (maybe_first_dot_index.has_value()) {
            auto first_dot_index = maybe_first_dot_index.release_value();
            return wildcard_matches(host.substring_view(first_dot_index + 1), subject.substring_view(2));
        }
    }

    return false;
}

static bool certificate_subject_matches_host(Certificate& cert, StringView host)
{
    if (wildcard_matches(host, cert.subject.subject))
        return true;

    for (auto& san : cert.SAN) {
        if (wildcard_matches(host, san))
            return true;
    }

    return false;
}

bool Context::verify_chain(StringView host) const
{
    if (!options.validate_certificates)
        return true;

    Vector<Certificate> const* local_chain = nullptr;
    if (is_server) {
        dbgln("Unsupported: Server mode");
        TODO();
    } else {
        local_chain = &certificates;
    }

    if (local_chain->is_empty()) {
        dbgln("verify_chain: Attempting to verify an empty chain");
        return false;
    }

    // RFC5246 section 7.4.2: The sender's certificate MUST come first in the list. Each following certificate
    // MUST directly certify the one preceding it. Because certificate validation requires that root keys be
    // distributed independently, the self-signed certificate that specifies the root certificate authority MAY be
    // omitted from the chain, under the assumption that the remote end must already possess it in order to validate
    // it in any case.

    if (!host.is_empty()) {
        auto first_certificate = local_chain->first();
        auto subject_matches = certificate_subject_matches_host(first_certificate, host);
        if (!subject_matches) {
            dbgln("verify_chain: First certificate does not match the hostname");
            return false;
        }
    } else {
        // FIXME: The host is taken from m_context.extensions.SNI, when is this empty?
        dbgln("FIXME: verify_chain called without host");
        return false;
    }

    for (size_t cert_index = 0; cert_index < local_chain->size(); ++cert_index) {
        auto cert = local_chain->at(cert_index);

        auto subject_string = cert.subject_identifier_string();
        auto issuer_string = cert.issuer_identifier_string();

        if (!cert.is_valid()) {
            dbgln("verify_chain: Certificate is not valid {}", subject_string);
            return false;
        }

        auto maybe_root_certificate = root_certificates.get(issuer_string);
        if (maybe_root_certificate.has_value()) {
            auto& root_certificate = *maybe_root_certificate;
            auto verification_correct = verify_certificate_pair(cert, root_certificate);

            if (!verification_correct) {
                dbgln("verify_chain: Signature inconsistent, {} was not signed by {} (root certificate)", subject_string, issuer_string);
                return false;
            }

            // Root certificate reached, and correctly verified, so we can stop now
            return true;
        }

        if (subject_string == issuer_string) {
            dbgln("verify_chain: Non-root self-signed certificate");
            return options.allow_self_signed_certificates;
        }
        if ((cert_index + 1) >= local_chain->size()) {
            dbgln("verify_chain: No trusted root certificate found before end of certificate chain");
            dbgln("verify_chain: Last certificate in chain was signed by {}", issuer_string);
            return false;
        }

        auto parent_certificate = local_chain->at(cert_index + 1);
        if (issuer_string != parent_certificate.subject_identifier_string()) {
            dbgln("verify_chain: Next certificate in the chain is not the issuer of this certificate");
            return false;
        }

        if (!(parent_certificate.is_allowed_to_sign_certificate && parent_certificate.is_certificate_authority)) {
            dbgln("verify_chain: {} is not marked as certificate authority", issuer_string);
            return false;
        }
        if (parent_certificate.path_length_constraint.has_value() && cert_index > parent_certificate.path_length_constraint.value()) {
            dbgln("verify_chain: Path length for certificate exceeded");
            return false;
        }

        bool verification_correct = verify_certificate_pair(cert, parent_certificate);
        if (!verification_correct) {
            dbgln("verify_chain: Signature inconsistent, {} was not signed by {}", subject_string, issuer_string);
            return false;
        }
    }

    // Either a root certificate is reached, or parent validation fails as the end of the local chain is reached
    VERIFY_NOT_REACHED();
}

bool Context::verify_certificate_pair(Certificate const& subject, Certificate const& issuer) const
{
    Crypto::Hash::HashKind kind;
    switch (subject.signature_algorithm) {
    case CertificateKeyAlgorithm::RSA_SHA1:
        kind = Crypto::Hash::HashKind::SHA1;
        break;
    case CertificateKeyAlgorithm::RSA_SHA256:
        kind = Crypto::Hash::HashKind::SHA256;
        break;
    case CertificateKeyAlgorithm::RSA_SHA384:
        kind = Crypto::Hash::HashKind::SHA384;
        break;
    case CertificateKeyAlgorithm::RSA_SHA512:
        kind = Crypto::Hash::HashKind::SHA512;
        break;
    default:
        dbgln("verify_certificate_pair: Unknown signature algorithm, expected RSA with SHA1/256/384/512, got {}", (u8)subject.signature_algorithm);
        return false;
    }

    Crypto::PK::RSAPrivateKey dummy_private_key;
    Crypto::PK::RSAPublicKey public_key_copy { issuer.public_key };
    auto rsa = Crypto::PK::RSA(public_key_copy, dummy_private_key);
    auto verification_buffer_result = ByteBuffer::create_uninitialized(subject.signature_value.size());
    if (verification_buffer_result.is_error()) {
        dbgln("verify_certificate_pair: Unable to allocate buffer for verification");
        return false;
    }
    auto verification_buffer = verification_buffer_result.release_value();
    auto verification_buffer_bytes = verification_buffer.bytes();
    rsa.verify(subject.signature_value, verification_buffer_bytes);

    // FIXME: This slice is subject hack, this will work for most certificates, but you actually have to parse
    //        the ASN.1 data to correctly extract the signed part of the certificate.
    ReadonlyBytes message = subject.original_asn1.bytes().slice(4, subject.original_asn1.size() - 4 - (5 + subject.signature_value.size()) - 15);
    auto pkcs1 = Crypto::PK::EMSA_PKCS1_V1_5<Crypto::Hash::Manager>(kind);
    auto verification = pkcs1.verify(message, verification_buffer_bytes, subject.signature_value.size() * 8);
    return verification == Crypto::VerificationConsistency::Consistent;
}

template<typename HMACType>
static void hmac_pseudorandom_function(Bytes output, ReadonlyBytes secret, u8 const* label, size_t label_length, ReadonlyBytes seed, ReadonlyBytes seed_b)
{
    if (!secret.size()) {
        dbgln("null secret");
        return;
    }

    auto append_label_seed = [&](auto& hmac) {
        hmac.update(label, label_length);
        hmac.update(seed);
        if (seed_b.size() > 0)
            hmac.update(seed_b);
    };

    HMACType hmac(secret);
    append_label_seed(hmac);

    constexpr auto digest_size = hmac.digest_size();
    u8 digest[digest_size];
    auto digest_0 = Bytes { digest, digest_size };

    digest_0.overwrite(0, hmac.digest().immutable_data(), digest_size);

    size_t index = 0;
    while (index < output.size()) {
        hmac.update(digest_0);
        append_label_seed(hmac);
        auto digest_1 = hmac.digest();

        auto copy_size = min(digest_size, output.size() - index);

        output.overwrite(index, digest_1.immutable_data(), copy_size);
        index += copy_size;

        digest_0.overwrite(0, hmac.process(digest_0).immutable_data(), digest_size);
    }
}

void TLSv12::pseudorandom_function(Bytes output, ReadonlyBytes secret, u8 const* label, size_t label_length, ReadonlyBytes seed, ReadonlyBytes seed_b)
{
    // Simplification: We only support the HMAC PRF with the hash function SHA-256 or stronger.

    // RFC 5246: "In this section, we define one PRF, based on HMAC.  This PRF with the
    //            SHA-256 hash function is used for all cipher suites defined in this
    //            document and in TLS documents published prior to this document when
    //            TLS 1.2 is negotiated.  New cipher suites MUST explicitly specify a
    //            PRF and, in general, SHOULD use the TLS PRF with SHA-256 or a
    //            stronger standard hash function."

    switch (hmac_hash()) {
    case Crypto::Hash::HashKind::SHA512:
        hmac_pseudorandom_function<Crypto::Authentication::HMAC<Crypto::Hash::SHA512>>(output, secret, label, label_length, seed, seed_b);
        break;
    case Crypto::Hash::HashKind::SHA384:
        hmac_pseudorandom_function<Crypto::Authentication::HMAC<Crypto::Hash::SHA384>>(output, secret, label, label_length, seed, seed_b);
        break;
    case Crypto::Hash::HashKind::SHA256:
        hmac_pseudorandom_function<Crypto::Authentication::HMAC<Crypto::Hash::SHA256>>(output, secret, label, label_length, seed, seed_b);
        break;
    default:
        dbgln("Failed to find a suitable HMAC hash");
        VERIFY_NOT_REACHED();
        break;
    }
}

TLSv12::TLSv12(StreamVariantType stream, Options options)
    : m_stream(move(stream))
{
    m_context.options = move(options);
    m_context.is_server = false;
    m_context.tls_buffer = {};

    set_root_certificates(m_context.options.root_certificates.has_value()
            ? *m_context.options.root_certificates
            : DefaultRootCACertificates::the().certificates());

    setup_connection();
}

Vector<Certificate> TLSv12::parse_pem_certificate(ReadonlyBytes certificate_pem_buffer, ReadonlyBytes rsa_key) // FIXME: This should not be bound to RSA
{
    if (certificate_pem_buffer.is_empty() || rsa_key.is_empty()) {
        return {};
    }

    auto decoded_certificate = Crypto::decode_pem(certificate_pem_buffer);
    if (decoded_certificate.is_empty()) {
        dbgln("Certificate not PEM");
        return {};
    }

    auto maybe_certificate = Certificate::parse_asn1(decoded_certificate);
    if (!maybe_certificate.has_value()) {
        dbgln("Invalid certificate");
        return {};
    }

    Crypto::PK::RSA rsa(rsa_key);
    auto certificate = maybe_certificate.release_value();
    certificate.private_key = rsa.private_key();

    return { move(certificate) };
}

Singleton<DefaultRootCACertificates> DefaultRootCACertificates::s_the;
DefaultRootCACertificates::DefaultRootCACertificates()
{
    // FIXME: This might not be the best format, find a better way to represent CA certificates.
    auto config_result = Core::ConfigFile::open_for_system("ca_certs");
    if (config_result.is_error()) {
        dbgln("Failed to load CA Certificates: {}", config_result.error());
        return;
    }
    auto config = config_result.release_value();

    for (auto& entity : config->groups()) {
        for (auto& subject : config->keys(entity)) {
            auto certificate_base64 = config->read_entry(entity, subject);
            auto certificate_data_result = decode_base64(certificate_base64);
            if (certificate_data_result.is_error()) {
                dbgln("Skipping CA Certificate {} {}: out of memory", entity, subject);
                continue;
            }
            auto certificate_data = certificate_data_result.release_value();
            auto certificate_result = Certificate::parse_asn1(certificate_data.bytes());
            // If the certificate does not parse it is likely using elliptic curve keys/signatures, which are not
            // supported right now. Currently, ca_certs.ini should only contain certificates with RSA keys/signatures.
            if (!certificate_result.has_value()) {
                dbgln("Skipping CA Certificate {} {}: unable to parse", entity, subject);
                continue;
            }
            auto certificate = certificate_result.release_value();
            m_ca_certificates.append(move(certificate));
        }
    }

    dbgln("Loaded {} CA Certificates", m_ca_certificates.size());
}
}
