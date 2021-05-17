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
#include <LibCore/FileStream.h>
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

ssize_t TLSv12::handle_certificate(ReadonlyBytes buffer)
{
    ssize_t res = 0;

    if (buffer.size() < 3) {
        dbgln_if(TLS_DEBUG, "not enough certificate header data");
        return (i8)Error::NeedMoreData;
    }

    u32 certificate_total_length = buffer[0] * 0x10000 + buffer[1] * 0x100 + buffer[2];

    dbgln_if(TLS_DEBUG, "total length: {}", certificate_total_length);

    if (certificate_total_length <= 4)
        return 3 * certificate_total_length;

    res += 3;

    if (certificate_total_length > buffer.size() - res) {
        dbgln_if(TLS_DEBUG, "not enough data for claimed total cert length");
        return (i8)Error::NeedMoreData;
    }
    size_t size = certificate_total_length;

    size_t index = 0;
    bool valid_certificate = false;

    while (size > 0) {
        ++index;
        if (buffer.size() - res < 3) {
            dbgln_if(TLS_DEBUG, "not enough data for certificate length");
            return (i8)Error::NeedMoreData;
        }
        size_t certificate_size = buffer[res] * 0x10000 + buffer[res + 1] * 0x100 + buffer[res + 2];
        res += 3;

        if (buffer.size() - res < certificate_size) {
            dbgln_if(TLS_DEBUG, "not enough data for certificate body");
            return (i8)Error::NeedMoreData;
        }

        auto res_cert = res;
        auto remaining = certificate_size;
        size_t certificates_in_chain = 0;

        do {
            if (remaining <= 3) {
                dbgln("Ran out of data");
                break;
            }
            ++certificates_in_chain;
            if (buffer.size() < (size_t)res_cert + 3) {
                dbgln("not enough data to read cert size ({} < {})", buffer.size(), res_cert + 3);
                break;
            }
            size_t certificate_size_specific = buffer[res_cert] * 0x10000 + buffer[res_cert + 1] * 0x100 + buffer[res_cert + 2];
            res_cert += 3;
            remaining -= 3;

            if (certificate_size_specific > remaining) {
                dbgln("invalid certificate size (expected {} but got {})", remaining, certificate_size_specific);
                break;
            }
            remaining -= certificate_size_specific;

            auto certificate = Certificate::parse_asn1(buffer.slice(res_cert, certificate_size_specific), false);
            if (certificate.has_value()) {
                if (certificate.value().is_valid()) {
                    m_context.certificates.append(certificate.value());
                    valid_certificate = true;
                }
            }
            res_cert += certificate_size_specific;
        } while (remaining > 0);
        if (remaining) {
            dbgln("extraneous {} bytes left over after parsing certificates", remaining);
        }
        size -= certificate_size + 3;
        res += certificate_size;
    }
    if (!valid_certificate)
        return (i8)Error::UnsupportedCertificate;

    if ((size_t)res != buffer.size())
        dbgln("some data left unread: {} bytes out of {}", res, buffer.size());

    return res;
}

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

    m_context.message_buffer.append(record.data(), record.size());

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

void TLSv12::ensure_hmac(size_t digest_size, bool local)
{
    if (local && m_hmac_local)
        return;

    if (!local && m_hmac_remote)
        return;

    auto hash_kind = Crypto::Hash::HashKind::None;

    switch (digest_size) {
    case Crypto::Hash::SHA1::DigestSize:
        hash_kind = Crypto::Hash::HashKind::SHA1;
        break;
    case Crypto::Hash::SHA256::DigestSize:
        hash_kind = Crypto::Hash::HashKind::SHA256;
        break;
    case Crypto::Hash::SHA512::DigestSize:
        hash_kind = Crypto::Hash::HashKind::SHA512;
        break;
    default:
        dbgln("Failed to find a suitable hash for size {}", digest_size);
        break;
    }

    auto hmac = make<Crypto::Authentication::HMAC<Crypto::Hash::Manager>>(ReadonlyBytes { local ? m_context.crypto.local_mac : m_context.crypto.remote_mac, digest_size }, hash_kind);
    if (local)
        m_hmac_local = move(hmac);
    else
        m_hmac_remote = move(hmac);
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
    if (!m_context.root_ceritificates.is_empty())
        dbgln("TLS warn: resetting root certificates!");

    for (auto& cert : certificates) {
        if (!cert.is_valid())
            dbgln("Certificate for {} by {} is invalid, things may or may not work!", cert.subject.subject, cert.issuer.subject);
        // FIXME: Figure out what we should do when our root certs are invalid.
    }
    m_context.root_ceritificates = move(certificates);
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
    for (auto& cert : root_ceritificates) {
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
            dbgln("Certificate for {} is not signed by anyone we trust ({})", it.key, it.value);
            return false;
        }

        if (ref.value() == it.key) // Allow (but warn about) mutually recursively signed cert A <-> B.
            dbgln("Co-dependency warning: Certificate for {} is issued by {}, which itself is issued by {}", ref.value(), it.key, ref.value());
    }

    return true;
}

static bool wildcard_matches(const StringView& host, const StringView& subject)
{
    if (host.matches(subject))
        return true;

    if (subject.starts_with("*."))
        return wildcard_matches(host, subject.substring_view(2));

    return false;
}

Optional<size_t> TLSv12::verify_chain_and_get_matching_certificate(const StringView& host) const
{
    if (m_context.certificates.is_empty() || !m_context.verify_chain())
        return {};

    if (host.is_empty())
        return 0;

    for (size_t i = 0; i < m_context.certificates.size(); ++i) {
        auto& cert = m_context.certificates[i];
        if (wildcard_matches(host, cert.subject.subject))
            return i;
        for (auto& san : cert.SAN) {
            if (wildcard_matches(host, san))
                return i;
        }
    }

    return {};
}

TLSv12::TLSv12(Core::Object* parent, Options options)
    : Core::Socket(Core::Socket::Type::TCP, parent)
{
    m_context.options = move(options);
    m_context.is_server = false;
    m_context.tls_buffer = ByteBuffer::create_uninitialized(0);
#ifdef SOCK_NONBLOCK
    int fd = socket(AF_INET, SOCK_STREAM | SOCK_NONBLOCK, 0);
#else
    int fd = socket(AF_INET, SOCK_STREAM, 0);
    int option = 1;
    ioctl(fd, FIONBIO, &option);
#endif
    if (fd < 0) {
        set_error(errno);
    } else {
        set_fd(fd);
        set_mode(Core::OpenMode::ReadWrite);
        set_error(0);
    }
}

bool TLSv12::add_client_key(ReadonlyBytes certificate_pem_buffer, ReadonlyBytes rsa_key) // FIXME: This should not be bound to RSA
{
    if (certificate_pem_buffer.is_empty() || rsa_key.is_empty()) {
        return true;
    }
    auto decoded_certificate = Crypto::decode_pem(certificate_pem_buffer);
    if (decoded_certificate.is_empty()) {
        dbgln("Certificate not PEM");
        return false;
    }

    auto maybe_certificate = Certificate::parse_asn1(decoded_certificate);
    if (!maybe_certificate.has_value()) {
        dbgln("Invalid certificate");
        return false;
    }

    Crypto::PK::RSA rsa(rsa_key);
    auto certificate = maybe_certificate.value();
    certificate.private_key = rsa.private_key();

    return add_client_key(certificate);
}

AK::Singleton<DefaultRootCACertificates> DefaultRootCACertificates::s_the;
DefaultRootCACertificates::DefaultRootCACertificates()
{
    // FIXME: This might not be the best format, find a better way to represent CA certificates.
    auto config = Core::ConfigFile::get_for_system("ca_certs");
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
}
}
