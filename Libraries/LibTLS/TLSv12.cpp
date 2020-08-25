/*
 * Copyright (c) 2020, Ali Mohammad Pur <ali.mpfard@gmail.com>
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

#include <AK/Endian.h>
#include <LibCore/DateTime.h>
#include <LibCore/Timer.h>
#include <LibCrypto/ASN1/DER.h>
#include <LibCrypto/ASN1/PEM.h>
#include <LibCrypto/PK/Code/EMSA_PSS.h>
#include <LibTLS/TLSv12.h>

#ifndef SOCK_NONBLOCK
#    include <sys/ioctl.h>
#endif

//#define TLS_DEBUG

namespace {
struct OIDChain {
    void* root { nullptr };
    u8* oid { nullptr };
};
}

namespace TLS {

// "for now" q&d implementation of ASN1
namespace {

static bool _asn1_is_field_present(const u32* fields, const u32* prefix)
{
    size_t i = 0;
    while (prefix[i]) {
        if (fields[i] != prefix[i])
            return false;
        ++i;
    }
    return true;
}

static bool _asn1_is_oid(const u8* oid, const u8* compare, size_t length = 3)
{
    size_t i = 0;
    while (oid[i] && i < length) {
        if (oid[i] != compare[i])
            return false;
        ++i;
    }
    return true;
}

static bool _set_algorithm(CertificateKeyAlgorithm& algorithm, const u8* value, size_t length)
{
    if (length == 7) {
        // Elliptic Curve pubkey
        dbg() << "Cert.algorithm: EC, unsupported";
        return false;
    }

    if (length == 8) {
        // named EC key
        dbg() << "Cert.algorithm: Named EC (" << *value << "), unsupported";
        return false;
    }

    if (length == 5) {
        // named EC SECP key
        dbg() << "Cert.algorithm: Named EC secp (" << *value << "), unsupported";
        return false;
    }

    if (length != 9) {
        dbg() << "Invalid certificate algorithm";
        return false;
    }

    if (_asn1_is_oid(value, Constants::RSA_SIGN_RSA_OID, 9)) {
        algorithm = CertificateKeyAlgorithm::RSA_RSA;
        return true;
    }

    if (_asn1_is_oid(value, Constants::RSA_SIGN_SHA256_OID, 9)) {
        algorithm = CertificateKeyAlgorithm::RSA_SHA256;
        return true;
    }

    if (_asn1_is_oid(value, Constants::RSA_SIGN_SHA512_OID, 9)) {
        algorithm = CertificateKeyAlgorithm::RSA_SHA512;
        return true;
    }

    if (_asn1_is_oid(value, Constants::RSA_SIGN_SHA1_OID, 9)) {
        algorithm = CertificateKeyAlgorithm::RSA_SHA1;
        return true;
    }

    if (_asn1_is_oid(value, Constants::RSA_SIGN_MD5_OID, 9)) {
        algorithm = CertificateKeyAlgorithm::RSA_MD5;
        return true;
    }

    dbg() << "Unsupported RSA Signature mode " << value[8];
    return false;
}

static size_t _get_asn1_length(const u8* buffer, size_t length, size_t& octets)
{
    octets = 0;
    if (length < 1)
        return 0;

    u8 size = buffer[0];
    if (size & 0x80) {
        octets = size & 0x7f;
        if (octets > length - 1) {
            return 0;
        }
        auto reference_octets = octets;
        if (octets > 4)
            reference_octets = 4;
        size_t long_size = 0, coeff = 1;
        for (auto i = reference_octets; i > 0; --i) {
            long_size += buffer[i] * coeff;
            coeff *= 0x100;
        }
        ++octets;
        return long_size;
    }
    ++octets;
    return size;
}

static ssize_t _parse_asn1(const Context& context, Certificate& cert, const u8* buffer, size_t size, int level, u32* fields, u8* has_key, int client_cert, u8* root_oid, OIDChain* chain)
{
    OIDChain local_chain;
    local_chain.root = chain;
    size_t position = 0;

    // parse DER...again
    size_t index = 0;
    u8 oid[16] { 0 };

    local_chain.oid = oid;
    if (has_key)
        *has_key = 0;

    u8 local_has_key = 0;
    const u8* cert_data = nullptr;
    size_t cert_length = 0;
    while (position < size) {
        size_t start_position = position;
        if (size - position < 2) {
            dbg() << "not enough data for certificate size";
            return (i8)Error::NeedMoreData;
        }
        u8 first = buffer[position++];
        u8 type = first & 0x1f;
        u8 constructed = first & 0x20;
        size_t octets = 0;
        u32 temp;
        index++;

        if (level <= 0xff)
            fields[level - 1] = index;

        size_t length = _get_asn1_length((const u8*)&buffer[position], size - position, octets);

        if (octets > 4 || octets > size - position) {
#ifdef TLS_DEBUG
            dbg() << "could not read the certificate";
#endif
            return position;
        }

        position += octets;
        if (size - position < length) {
#ifdef TLS_DEBUG
            dbg() << "not enough data for sequence";
#endif
            return (i8)Error::NeedMoreData;
        }

        if (length && constructed) {
            switch (type) {
            case 0x03:
                break;
            case 0x10:
                if (level == 2 && index == 1) {
                    cert_length = length + position - start_position;
                    cert_data = buffer + start_position;
                }
                // public key data
                if (!cert.version && _asn1_is_field_present(fields, Constants::priv_der_id)) {
                    temp = length + position - start_position;
                    if (cert.der.size() < temp) {
                        cert.der.grow(temp);
                    } else {
                        cert.der.trim(temp);
                    }
                    cert.der.overwrite(0, buffer + start_position, temp);
                }
                break;

            default:
                break;
            }
            local_has_key = false;
            _parse_asn1(context, cert, buffer + position, length, level + 1, fields, &local_has_key, client_cert, root_oid, &local_chain);
            if ((local_has_key && (!context.is_server || client_cert)) || (client_cert || _asn1_is_field_present(fields, Constants::pk_id))) {
                temp = length + position - start_position;
                if (cert.der.size() < temp) {
                    cert.der.grow(temp);
                } else {
                    cert.der.trim(temp);
                }
                cert.der.overwrite(0, buffer + start_position, temp);
            }
        } else {
            switch (type) {
            case 0x00:
                return position;
                break;
            case 0x01:
                temp = buffer[position];
                break;
            case 0x02:
                if (_asn1_is_field_present(fields, Constants::pk_id)) {
                    if (has_key)
                        *has_key = true;

                    if (index == 1)
                        cert.public_key.set(
                            Crypto::UnsignedBigInteger::import_data(buffer + position, length),
                            cert.public_key.public_exponent());
                    else if (index == 2)
                        cert.public_key.set(
                            cert.public_key.modulus(),
                            Crypto::UnsignedBigInteger::import_data(buffer + position, length));
                } else if (_asn1_is_field_present(fields, Constants::serial_id)) {
                    cert.serial_number = Crypto::UnsignedBigInteger::import_data(buffer + position, length);
                }
                if (_asn1_is_field_present(fields, Constants::version_id)) {
                    if (length == 1)
                        cert.version = buffer[position];
                }
                // print_buffer(ByteBuffer::wrap(const_cast<u8*>(buffer) + position, length));
                break;
            case 0x03:
                if (_asn1_is_field_present(fields, Constants::pk_id)) {
                    if (has_key)
                        *has_key = true;
                }
                if (_asn1_is_field_present(fields, Constants::sign_id)) {
                    auto* value = buffer + position;
                    auto len = length;
                    if (!value[0] && len % 2) {
                        ++value;
                        --len;
                    }
                    cert.sign_key = ByteBuffer::copy(value, len);
                } else {
                    if (buffer[position] == 0 && length > 256) {
                        _parse_asn1(context, cert, buffer + position + 1, length - 1, level + 1, fields, &local_has_key, client_cert, root_oid, &local_chain);
                    } else {
                        _parse_asn1(context, cert, buffer + position, length, level + 1, fields, &local_has_key, client_cert, root_oid, &local_chain);
                    }
                }
                break;
            case 0x04:
                _parse_asn1(context, cert, buffer + position, length, level + 1, fields, &local_has_key, client_cert, root_oid, &local_chain);
                break;
            case 0x05:
                break;
            case 0x06:
                if (_asn1_is_field_present(fields, Constants::pk_id)) {
                    _set_algorithm(cert.key_algorithm, buffer + position, length);
                }
                if (_asn1_is_field_present(fields, Constants::algorithm_id)) {
                    _set_algorithm(cert.algorithm, buffer + position, length);
                }

                if (length < 16)
                    memcpy(oid, buffer + position, length);
                else
                    memcpy(oid, buffer + position, 16);
                if (root_oid)
                    memcpy(root_oid, oid, 16);
                break;
            case 0x09:
                break;
            case 0x17:
            case 0x018:
                // time
                // ignore
                break;
            case 0x013:
            case 0x0c:
            case 0x14:
            case 0x15:
            case 0x16:
            case 0x19:
            case 0x1a:
            case 0x1b:
            case 0x1c:
            case 0x1d:
            case 0x1e:
                // printable string and such
                if (_asn1_is_field_present(fields, Constants::issurer_id)) {
                    if (_asn1_is_oid(oid, Constants::country_oid)) {
                        cert.issuer_country = String { (const char*)buffer + position, length };
                    } else if (_asn1_is_oid(oid, Constants::state_oid)) {
                        cert.issuer_state = String { (const char*)buffer + position, length };
                    } else if (_asn1_is_oid(oid, Constants::location_oid)) {
                        cert.issuer_location = String { (const char*)buffer + position, length };
                    } else if (_asn1_is_oid(oid, Constants::entity_oid)) {
                        cert.issuer_entity = String { (const char*)buffer + position, length };
                    } else if (_asn1_is_oid(oid, Constants::subject_oid)) {
                        cert.issuer_subject = String { (const char*)buffer + position, length };
                    }
                } else if (_asn1_is_field_present(fields, Constants::owner_id)) {
                    if (_asn1_is_oid(oid, Constants::country_oid)) {
                        cert.country = String { (const char*)buffer + position, length };
                    } else if (_asn1_is_oid(oid, Constants::state_oid)) {
                        cert.state = String { (const char*)buffer + position, length };
                    } else if (_asn1_is_oid(oid, Constants::location_oid)) {
                        cert.location = String { (const char*)buffer + position, length };
                    } else if (_asn1_is_oid(oid, Constants::entity_oid)) {
                        cert.entity = String { (const char*)buffer + position, length };
                    } else if (_asn1_is_oid(oid, Constants::subject_oid)) {
                        cert.subject = String { (const char*)buffer + position, length };
                    }
                }
                break;
            default:
                // dbg() << "unused field " << type;
                break;
            }
        }
        position += length;
    }
    if (level == 2 && cert.sign_key.size() && cert_length && cert_data) {
        cert.fingerprint.clear();
        Crypto::Hash::Manager hash;
        switch (cert.key_algorithm) {
        case CertificateKeyAlgorithm::RSA_MD5:
            hash.initialize(Crypto::Hash::HashKind::MD5);
            break;
        case CertificateKeyAlgorithm::RSA_SHA1:
            hash.initialize(Crypto::Hash::HashKind::SHA1);
            break;
        case CertificateKeyAlgorithm::RSA_SHA256:
            hash.initialize(Crypto::Hash::HashKind::SHA256);
            break;
        case CertificateKeyAlgorithm::RSA_SHA512:
            hash.initialize(Crypto::Hash::HashKind::SHA512);
            break;
        default:
#ifdef TLS_DEBUG
            dbg() << "Unsupported hash mode " << (u32)cert.key_algorithm;
#endif
            // fallback to md5, it will fail later
            hash.initialize(Crypto::Hash::HashKind::MD5);
            break;
        }
        hash.update(cert_data, cert_length);
        auto fingerprint = hash.digest();
        cert.fingerprint.grow(fingerprint.data_length());
        cert.fingerprint.overwrite(0, fingerprint.immutable_data(), fingerprint.data_length());
#ifdef TLS_DEBUG
        dbg() << "Certificate fingerprint:";
        print_buffer(cert.fingerprint);
#endif
    }
    return position;
}
}

Optional<Certificate> TLSv12::parse_asn1(const ByteBuffer& buffer, bool) const
{
    // FIXME: Our ASN.1 parser is not quite up to the task of
    //        parsing this X.509 certificate, so for the
    //        time being, we will "parse" the certificate
    //        manually right here.

    Certificate cert;
    u32 fields[0xff];

    _parse_asn1(m_context, cert, buffer.data(), buffer.size(), 1, fields, nullptr, 0, nullptr, nullptr);

#ifdef TLS_DEBUG
    dbg() << "Certificate issued for " << cert.subject << " by " << cert.issuer_subject;
#endif

    return cert;
}

ssize_t TLSv12::handle_certificate(const ByteBuffer& buffer)
{
    ssize_t res = 0;

    if (buffer.size() < 3) {
#ifdef TLS_DEBUG
        dbg() << "not enough certificate header data";
#endif
        return (i8)Error::NeedMoreData;
    }

    u32 certificate_total_length = buffer[0] * 0x10000 + buffer[1] * 0x100 + buffer[2];

#ifdef TLS_DEBUG
    dbg() << "total length: " << certificate_total_length;
#endif

    if (certificate_total_length <= 4)
        return 3 * certificate_total_length;

    res += 3;

    if (certificate_total_length > buffer.size() - res) {
#ifdef TLS_DEBUG
        dbg() << "not enough data for claimed total cert length";
#endif
        return (i8)Error::NeedMoreData;
    }
    size_t size = certificate_total_length;

    size_t index = 0;
    bool valid_certificate = false;

    while (size > 0) {
        ++index;
        if (buffer.size() - res < 3) {
#ifdef TLS_DEBUG
            dbg() << "not enough data for certificate length";
#endif
            return (i8)Error::NeedMoreData;
        }
        size_t certificate_size = buffer[res] * 0x10000 + buffer[res + 1] * 0x100 + buffer[res + 2];
        res += 3;

        if (buffer.size() - res < certificate_size) {
#ifdef TLS_DEBUG
            dbg() << "not enough data for certificate body";
#endif
            return (i8)Error::NeedMoreData;
        }

        auto res_cert = res;
        auto remaining = certificate_size;
        size_t certificates_in_chain = 0;

        do {
            if (remaining <= 3) {
                dbg() << "Ran out of data";
                break;
            }
            ++certificates_in_chain;
            if (buffer.size() < (size_t)res_cert + 3) {
                dbg() << "not enough data to read cert size (" << buffer.size() << " < " << res_cert + 3 << ")";
                break;
            }
            size_t certificate_size_specific = buffer[res_cert] * 0x10000 + buffer[res_cert + 1] * 0x100 + buffer[res_cert + 2];
            res_cert += 3;
            remaining -= 3;

            if (certificate_size_specific > remaining) {
                dbg() << "invalid certificate size (expected " << remaining << " but got " << certificate_size_specific << ")";
                break;
            }
            remaining -= certificate_size_specific;

            auto certificate = parse_asn1(buffer.slice_view(res_cert, certificate_size_specific), false);
            if (certificate.has_value()) {
                m_context.certificates.append(certificate.value());
                valid_certificate = true;
            }
            res_cert += certificate_size_specific;
        } while (remaining > 0);
        if (remaining) {
            dbg() << "extraneous " << remaining << " bytes left over after parsing certificates";
        }
        size -= certificate_size + 3;
        res += certificate_size;
    }
    if (!valid_certificate)
        return (i8)Error::UnsupportedCertificate;

    if ((size_t)res != buffer.size())
        dbg() << "some data left unread: " << (size_t)res << " bytes out of " << buffer.size();

    return res;
}

void TLSv12::consume(const ByteBuffer& record)
{
    if (m_context.critical_error) {
        dbg() << "There has been a critical error (" << (i8)m_context.critical_error << "), refusing to continue";
        return;
    }

    if (record.size() == 0) {
        return;
    }

#ifdef TLS_DEBUG
    dbg() << "Consuming " << record.size() << " bytes";
#endif

    m_context.message_buffer.append(record.data(), record.size());

    size_t index { 0 };
    size_t buffer_length = m_context.message_buffer.size();

    size_t size_offset { 3 }; // read the common record header
    size_t header_size { 5 };
#ifdef TLS_DEBUG
    dbg() << "message buffer length " << buffer_length;
#endif
    while (buffer_length >= 5) {
        auto length = AK::convert_between_host_and_network_endian(*(u16*)m_context.message_buffer.offset_pointer(index + size_offset)) + header_size;
        if (length > buffer_length) {
#ifdef TLS_DEBUG
            dbg() << "Need more data: " << length << " | " << buffer_length;
#endif
            break;
        }
        auto consumed = handle_message(m_context.message_buffer.slice_view(index, length));

#ifdef TLS_DEBUG
        if (consumed > 0)
            dbg() << "consumed " << (size_t)consumed << " bytes";
        else
            dbg() << "error: " << (int)consumed;
#endif

        if (consumed != (i8)Error::NeedMoreData) {
            if (consumed < 0) {
                dbg() << "Consumed an error: " << (int)consumed;
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
            dbg() << "Broken connection";
            m_context.error_code = Error::BrokenConnection;
            break;
        }
    }
    if (m_context.error_code != Error::NoError && m_context.error_code != Error::NeedMoreData) {
        dbg() << "consume error: " << (i8)m_context.error_code;
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
        dbg() << "Failed to find a suitable hash for size " << digest_size;
        break;
    }

    auto hmac = make<Crypto::Authentication::HMAC<Crypto::Hash::Manager>>(ByteBuffer::wrap(local ? m_context.crypto.local_mac : m_context.crypto.remote_mac, digest_size), hash_kind);
    if (local)
        m_hmac_local = move(hmac);
    else
        m_hmac_remote = move(hmac);
}

bool Certificate::is_valid() const
{
    auto now = Core::DateTime::now();

    if (!not_before.is_empty()) {
        if (now.is_before(not_before)) {
            dbg() << "certificate expired (not yet valid, signed for " << not_before << ")";
            return false;
        }
    }

    if (!not_after.is_empty()) {
        if (!now.is_before(not_after)) {
            dbg() << "certificate expired (expiry date " << not_after << ")";
            return false;
        }
    }

    return true;
}

void TLSv12::try_disambiguate_error() const
{
    dbg() << "Possible failure cause(s): ";
    switch ((AlertDescription)m_context.critical_error) {
    case AlertDescription::HandshakeFailure:
        if (!m_context.cipher_spec_set) {
            dbg() << "- No cipher suite in common with " << m_context.SNI;
        } else {
            dbg() << "- Unknown internal issue";
        }
        break;
    case AlertDescription::InsufficientSecurity:
        dbg() << "- No cipher suite in common with " << m_context.SNI << " (the server is oh so secure)";
        break;
    case AlertDescription::ProtocolVersion:
        dbg() << "- The server refused to negotiate with TLS 1.2 :(";
        break;
    case AlertDescription::UnexpectedMessage:
        dbg() << "- We sent an invalid message for the state we're in.";
        break;
    case AlertDescription::BadRecordMAC:
        dbg() << "- Bad MAC record from our side.";
        dbg() << "- Ciphertext wasn't an even multiple of the block length.";
        dbg() << "- Bad block cipher padding.";
        dbg() << "- If both sides are compliant, the only cause is messages being corrupted in the network.";
        break;
    case AlertDescription::RecordOverflow:
        dbg() << "- Sent a ciphertext record which has a length bigger than 18432 bytes.";
        dbg() << "- Sent record decrypted to a compressed record that has a length bigger than 18432 bytes.";
        dbg() << "- If both sides are compliant, the only cause is messages being corrupted in the network.";
        break;
    case AlertDescription::DecompressionFailure:
        dbg() << "- We sent invalid input for decompression (e.g. data that would expand to excessive length)";
        break;
    case AlertDescription::IllegalParameter:
        dbg() << "- We sent a parameter in the handshake that is out of range or inconsistent with the other parameters.";
        break;
    case AlertDescription::DecodeError:
        dbg() << "- The message we sent cannot be decoded because a field was out of range or the length was incorrect.";
        dbg() << "- If both sides are compliant, the only cause is messages being corrupted in the network.";
        break;
    case AlertDescription::DecryptError:
        dbg() << "- A handshake crypto operation failed. This includes signature verification and validating Finished.";
        break;
    case AlertDescription::AccessDenied:
        dbg() << "- The certificate is valid, but once access control was applied, the sender decided to stop negotiation.";
        break;
    case AlertDescription::InternalError:
        dbg() << "- No one knows, but it isn't a protocol failure.";
        break;
    case AlertDescription::DecryptionFailed:
    case AlertDescription::NoCertificate:
    case AlertDescription::ExportRestriction:
        dbg() << "- No one knows, the server sent a non-compliant alert.";
        break;
    default:
        dbg() << "- No one knows.";
        break;
    }
}

TLSv12::TLSv12(Core::Object* parent, Version version)
    : Core::Socket(Core::Socket::Type::TCP, parent)
{
    m_context.version = version;
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
        set_mode(IODevice::ReadWrite);
        set_error(0);
    }
}

bool TLSv12::add_client_key(const ByteBuffer& certificate_pem_buffer, const ByteBuffer& rsa_key) // FIXME: This should not be bound to RSA
{
    if (certificate_pem_buffer.is_empty() || rsa_key.is_empty()) {
        return true;
    }
    auto decoded_certificate = Crypto::decode_pem(certificate_pem_buffer, 0);
    if (decoded_certificate.is_empty()) {
        dbg() << "Certificate not PEM";
        return false;
    }

    auto maybe_certificate = parse_asn1(decoded_certificate);
    if (!maybe_certificate.has_value()) {
        dbg() << "Invalid certificate";
        return false;
    }

    Crypto::PK::RSA rsa(rsa_key);
    auto certificate = maybe_certificate.value();
    certificate.private_key = rsa.private_key();

    return add_client_key(certificate);
}

}
