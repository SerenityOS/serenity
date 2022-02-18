/*
 * Copyright (c) 2020, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Debug.h>
#include <AK/Endian.h>
#include <LibCore/ConfigFile.h>
#include <LibCore/DateTime.h>
#include <LibCore/File.h>
#include <LibCore/Timer.h>
#include <LibCrypto/ASN1/ASN1.h>
#include <LibCrypto/ASN1/PEM.h>
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
    if (!m_context.root_certificates.is_empty())
        dbgln("TLS warn: resetting root certificates!");

    for (auto& cert : certificates) {
        if (!cert.is_valid())
            dbgln("Certificate for {} by {} is invalid, things may or may not work!", cert.subject.subject, cert.issuer.subject);
        // FIXME: Figure out what we should do when our root certs are invalid.
    }
    m_context.root_certificates = move(certificates);
    dbgln_if(TLS_DEBUG, "{}: Set {} root certificates", this, m_context.root_certificates.size());
}

bool Context::verify_chain() const
{
    if (!options.validate_certificates)
        return true;

    const Vector<Certificate>* local_chain = nullptr;
    if (is_server) {
        dbgln("Unsupported: Server mode");
        TODO();
    } else {
        local_chain = &certificates;
    }

    // FIXME: Actually verify the signature, instead of just checking the name.
    HashMap<String, String> chain;
    HashTable<String> roots;
    // First, walk the root certs.
    for (auto& cert : root_certificates) {
        roots.set(cert.subject.subject);
        chain.set(cert.subject.subject, cert.issuer.subject);
    }

    // Then, walk the local certs.
    for (auto& cert : *local_chain) {
        auto& issuer_unique_name = cert.issuer.unit.is_empty() ? cert.issuer.subject : cert.issuer.unit;
        chain.set(cert.subject.subject, issuer_unique_name);
    }

    // Then verify the chain.
    for (auto& it : chain) {
        if (it.key == it.value) { // Allow self-signed certificates.
            if (!roots.contains(it.key))
                dbgln("Self-signed warning: Certificate for {} is self-signed", it.key);
            continue;
        }

        auto ref = chain.get(it.value);
        if (!ref.has_value()) {
            dbgln("{}: Certificate for {} is not signed by anyone we trust ({})", this, it.key, it.value);
            return false;
        }

        if (ref.value() == it.key) // Allow (but warn about) mutually recursively signed cert A <-> B.
            dbgln("Co-dependency warning: Certificate for {} is issued by {}, which itself is issued by {}", ref.value(), it.key, ref.value());
    }

    return true;
}

template<typename HMACType>
static void hmac_pseudorandom_function(Bytes output, ReadonlyBytes secret, const u8* label, size_t label_length, ReadonlyBytes seed, ReadonlyBytes seed_b)
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

void TLSv12::pseudorandom_function(Bytes output, ReadonlyBytes secret, const u8* label, size_t label_length, ReadonlyBytes seed, ReadonlyBytes seed_b)
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
    auto config = Core::ConfigFile::open_for_system("ca_certs").release_value_but_fixme_should_propagate_errors();
    auto now = Core::DateTime::now();
    auto last_year = Core::DateTime::create(now.year() - 1);
    auto next_year = Core::DateTime::create(now.year() + 1);
    for (auto& entity : config->groups()) {
        Certificate cert;
        cert.subject.subject = entity;
        cert.issuer.subject = config->read_entry(entity, "issuer_subject", entity);
        cert.subject.country = config->read_entry(entity, "country");
        cert.not_before = Crypto::ASN1::parse_generalized_time(config->read_entry(entity, "not_before", "")).value_or(last_year);
        cert.not_after = Crypto::ASN1::parse_generalized_time(config->read_entry(entity, "not_after", "")).value_or(next_year);
        m_ca_certificates.append(move(cert));
    }
    dbgln("Loaded {} CA Certificates", m_ca_certificates.size());
}
}
