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

#include <LibCore/Timer.h>
#include <LibCrypto/ASN1/DER.h>
#include <LibCrypto/PK/Code/EMSA_PSS.h>
#include <LibTLS/TLSv12.h>

//#define TLS_DEBUG

namespace {
inline static void print_buffer(const ByteBuffer& buffer)
{
    for (size_t i { 0 }; i < buffer.size(); ++i)
        dbgprintf("%02x ", buffer[i]);
    dbgprintf("\n");
}
inline static void print_buffer(const u8* buffer, size_t size)
{
    for (size_t i { 0 }; i < size; ++i)
        dbgprintf("%02x ", buffer[i]);
    dbgprintf("\n");
}
}

using namespace Crypto;

namespace {
struct OIDChain {
    void* root;
    u8* oid;
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

    static void _set_algorithm(u32&, const u8* value, size_t length)
    {
        if (length != 9) {
            dbg() << "unsupported algorithm " << value;
        }

        dbg() << "FIXME: Set algorithm";
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

    static ssize_t _parse_asn1(Context& context, Certificate& cert, const u8* buffer, size_t size, int level, u32* fields, u8* has_key, int client_cert, u8* root_oid, OIDChain* chain)
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
                dbg() << "could not read the certificate";
                return position;
            }

            position += octets;
            if (size - position < length) {
                dbg() << "not enough data for sequence";
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
                    // print_buffer(ByteBuffer::wrap(buffer + position, length));
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
            dbg() << "FIXME: Cert.fingerprint";
        }
        return position;
    }
}

Optional<Certificate> TLSv12::parse_asn1(const ByteBuffer& buffer, bool)
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

ByteBuffer TLSv12::build_hello()
{
    // arc4random_buf(&m_context.local_random, 32);
    auto packet_version = (u16)m_context.version;
    auto version = (u16)m_context.version;
    PacketBuilder builder { MessageType::Handshake, packet_version };
    // client hello
    builder.append((u8)0x1);

    // hello length (for later)
    u8 dummy[3];
    builder.append(dummy, 3);

    auto start_length = builder.length();

    builder.append(version);
    builder.append(m_context.local_random, sizeof(m_context.local_random));

    builder.append(m_context.session_id_size);
    if (m_context.session_id_size)
        builder.append(m_context.session_id, m_context.session_id_size);

    size_t extension_length = 0;
    size_t alpn_length = 0;
    size_t alpn_negotiated_length = 0;

    // ALPN
    if (!m_context.negotiated_alpn.is_null()) {
        alpn_negotiated_length = m_context.negotiated_alpn.length();
        alpn_length = alpn_negotiated_length + 1;
        extension_length += alpn_length + 6;
    } else if (m_context.alpn.size()) {
        for (auto& alpn : m_context.alpn) {
            size_t length = alpn.length();
            alpn_length += length + 1;
        }
        if (alpn_length)
            extension_length += alpn_length + 6;
    }

    // Ciphers
    builder.append((u16)(2 * sizeof(u16)));
    builder.append((u16)CipherSuite::RSA_WITH_AES_128_CBC_SHA256);
    builder.append((u16)CipherSuite::RSA_WITH_AES_256_CBC_SHA256);

    // we don't like compression
    builder.append((u8)1);
    builder.append((u8)0);

    // set SNI if we have one
    auto sni_length = 0;
    if (!m_context.SNI.is_null())
        sni_length = m_context.SNI.length();

    if (sni_length)
        extension_length += sni_length + 9;

    builder.append((u16)extension_length);

    if (sni_length) {
        // SNI extension
        builder.append((u16)0x00);
        // extension length
        builder.append((u16)(sni_length + 5));
        // SNI length
        builder.append((u16)(sni_length + 3));
        // SNI type
        builder.append((u8)0);
        // SNI host length + value
        builder.append((u16)sni_length);
        builder.append((const u8*)m_context.SNI.characters(), sni_length);
    }

    if (alpn_length) {
        // TODO
        ASSERT_NOT_REACHED();
    }

    // set the "length" field of the packet
    size_t remaining = builder.length() - start_length;
    size_t payload_position = 6;
    builder.set(payload_position, remaining / 0x10000);
    remaining %= 0x10000;
    builder.set(payload_position + 1, remaining / 0x100);
    remaining %= 0x100;
    builder.set(payload_position + 2, remaining);

    auto packet = builder.build();
    update_packet(packet);

    return packet;
}

ByteBuffer TLSv12::build_alert(bool critical, u8 code)
{
    dbg() << "FIXME: build_alert";
    (void)critical;
    (void)code;
    return {};
}

ByteBuffer TLSv12::build_finished()
{
    PacketBuilder builder { MessageType::Handshake, m_context.version, 12 + 64 };
    builder.append((u8)0x14);
    builder.append_u24(12);

    size_t out_size = 12;
    u8 out[out_size];
    auto outbuffer = ByteBuffer::wrap(out, out_size);
    auto dummy = ByteBuffer::create_zeroed(0);

    auto digest = m_context.handshake_hash.hash.peek();
    auto hashbuf = ByteBuffer::wrap(digest.data, m_context.handshake_hash.hash.DigestSize);
    pseudorandom_function(outbuffer, m_context.master_key, (const u8*)"client finished", 15, hashbuf, dummy);

    builder.append(outbuffer);
    auto packet = builder.build();
    update_packet(packet);

    return packet;
}

ByteBuffer TLSv12::build_certificate()
{
    dbg() << "FIXME: build_certificate";
    return {};
}

ByteBuffer TLSv12::build_change_cipher_spec()
{
    PacketBuilder builder { MessageType::ChangeCipher, m_context.version, 64 };
    builder.append((u8)1);
    auto packet = builder.build();
    update_packet(packet);
    m_context.local_sequence_number = 0;
    return packet;
}

ByteBuffer TLSv12::build_server_key_exchange()
{
    dbg() << "FIXME: build_server_key_exchange";
    return {};
}

ByteBuffer TLSv12::build_client_key_exchange()
{
    PacketBuilder builder { MessageType::Handshake, m_context.version };
    builder.append((u8)0x10);
    build_random(builder);

    m_context.connection_status = 2;

    auto packet = builder.build();

    update_packet(packet);

    return packet;
}

bool TLSv12::expand_key()
{
    u8 key[192]; // soooooooo many constants
    auto key_buffer = ByteBuffer::wrap(key, 192);

    if (m_context.master_key.size() == 0) {
        dbg() << "expand_key() with empty master key";
        return false;
    }

    auto key_size = key_length();
    auto mac_size = mac_length();
    auto iv_size = 16;

    pseudorandom_function(
        key_buffer,
        m_context.master_key,
        (const u8*)"key expansion", 13,
        ByteBuffer::wrap(m_context.remote_random, 32),
        ByteBuffer::wrap(m_context.local_random, 32));

    size_t offset = 0;
    memcpy(m_context.crypto.local_mac, key + offset, mac_size);
    offset += mac_size;
    memcpy(m_context.crypto.remote_mac, key + offset, mac_size);
    offset += mac_size;

    auto client_key = key + offset;
    offset += key_size;
    auto server_key = key + offset;
    offset += key_size;
    auto client_iv = key + offset;
    offset += iv_size;
    auto server_iv = key + offset;
    offset += iv_size;

#ifdef TLS_DEBUG
    dbg() << "client key";
    print_buffer(client_key, key_size);
    dbg() << "server key";
    print_buffer(server_key, key_size);
    dbg() << "client iv";
    print_buffer(client_iv, iv_size);
    dbg() << "server iv";
    print_buffer(server_iv, iv_size);
    dbg() << "client mac key";
    print_buffer(m_context.crypto.local_mac, 32);
    dbg() << "server mac key";
    print_buffer(m_context.crypto.remote_mac, 32);
#endif

    memcpy(m_context.crypto.local_iv, client_iv, iv_size);
    memcpy(m_context.crypto.remote_iv, server_iv, iv_size);

    m_aes_local = make<Crypto::Cipher::AESCipher::CBCMode>(ByteBuffer::wrap(client_key, key_size), key_size * 8, Crypto::Cipher::Intent::Encryption, Crypto::Cipher::PaddingMode::RFC5246);
    m_aes_remote = make<Crypto::Cipher::AESCipher::CBCMode>(ByteBuffer::wrap(server_key, key_size), key_size * 8, Crypto::Cipher::Intent::Decryption, Crypto::Cipher::PaddingMode::RFC5246);

    m_context.crypto.created = 1;

    return true;
}

void TLSv12::pseudorandom_function(ByteBuffer& output, const ByteBuffer& secret, const u8* label, size_t label_length, const ByteBuffer& seed, const ByteBuffer& seed_b)
{
    if (!secret.size()) {
        dbg() << "null secret";
        return;
    }

    Crypto::Authentication::HMAC<Crypto::Hash::SHA256> hmac(secret);

    auto l_seed_size = label_length + seed.size() + seed_b.size();
    u8 l_seed[l_seed_size];
    auto label_seed_buffer = ByteBuffer::wrap(l_seed, l_seed_size);
    label_seed_buffer.overwrite(0, label, label_length);
    label_seed_buffer.overwrite(label_length, seed.data(), seed.size());
    label_seed_buffer.overwrite(label_length + seed.size(), seed_b.data(), seed_b.size());

    u8 digest[hmac.DigestSize];

    auto digest_0 = ByteBuffer::wrap(digest, hmac.DigestSize);

    digest_0.overwrite(0, hmac.process(label_seed_buffer).data, hmac.DigestSize);

    size_t index = 0;
    while (index < output.size()) {
        hmac.update(digest_0);
        hmac.update(label_seed_buffer);
        auto digest_1 = hmac.digest();

        auto copy_size = min(hmac.DigestSize, output.size() - index);

        output.overwrite(index, digest_1.data, copy_size);
        index += copy_size;

        digest_0.overwrite(0, hmac.process(digest_0).data, hmac.DigestSize);
    }
}

bool TLSv12::compute_master_secret(size_t length)
{
    if (m_context.premaster_key.size() == 0 || length < 48) {
        dbg() << "there's no way I can make a master secret like this";
        dbg() << "I'd like to talk to your manager about this length of " << length;
        return false;
    }

    m_context.master_key.clear();
    m_context.master_key.grow(length);

    pseudorandom_function(
        m_context.master_key,
        m_context.premaster_key,
        (const u8*)"master secret", 13,
        ByteBuffer::wrap(m_context.local_random, 32),
        ByteBuffer::wrap(m_context.remote_random, 32));

    m_context.premaster_key.clear();
#ifdef TLS_DEBUG
    dbg() << "master key:";
    print_buffer(m_context.master_key);
#endif
    expand_key();
    return true;
}

void TLSv12::build_random(PacketBuilder& builder)
{
    u8 random_bytes[48] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48 };
    size_t bytes = 48;

    //    arc4random_buf(random_bytes, bytes);

    if (m_context.is_server) {
        dbg() << "Server mode not supported";
        return;
    } else {
        *(u16*)random_bytes = convert_between_host_and_network((u16)Version::V12);
    }

    m_context.premaster_key = ByteBuffer::copy(random_bytes, bytes);

    const auto& certificate = m_context.certificates[0];
#ifdef TLS_DEBUG
    dbg() << "PreMaster secret";
    print_buffer(m_context.premaster_key);
#endif

    Crypto::PK::RSA_PKCS1_EME rsa(certificate.public_key.modulus(), 0, certificate.public_key.public_exponent());

    u8 out[rsa.output_size()];
    auto outbuf = ByteBuffer::wrap(out, rsa.output_size());
    rsa.encrypt(m_context.premaster_key, outbuf);

#ifdef TLS_DEBUG
    dbg() << "Encrypted: ";
    print_buffer(outbuf);
#endif

    if (!compute_master_secret(bytes)) {
        dbg() << "oh noes we could not derive a master key :(";
        return;
    }

    builder.append_u24(outbuf.size() + 2);
    builder.append((u16)outbuf.size());
    builder.append(outbuf);
}

Optional<ByteBuffer> TLSv12::read()
{
    if (m_context.application_buffer.size()) {
        auto buf = m_context.application_buffer.slice(0, m_context.application_buffer.size());
        m_context.application_buffer.clear();
        return buf;
    } else
        return {};
}

ByteBuffer TLSv12::read(size_t max_size)
{
    if (m_context.application_buffer.size()) {
        auto length = min(m_context.application_buffer.size(), max_size);
        auto buf = m_context.application_buffer.slice(0, length);
        m_context.application_buffer = m_context.application_buffer.slice(length, m_context.application_buffer.size() - length);
        return buf;
    } else
        return {};
}

ByteBuffer TLSv12::read_line(size_t max_size)
{
    if (!can_read_line())
        return {};

    auto* start = m_context.application_buffer.data();
    auto* newline = (u8*)memchr(m_context.application_buffer.data(), '\n', m_context.application_buffer.size());
    ASSERT(newline);

    size_t offset = newline - start;

    if (offset > max_size)
        return {};

    auto buffer = ByteBuffer::copy(start, offset);
    m_context.application_buffer = m_context.application_buffer.slice(offset + 1, m_context.application_buffer.size() - offset - 1);

    return buffer;
}

bool TLSv12::write(const ByteBuffer& buffer)
{
    if (m_context.connection_status != 0xff) {
        dbg() << "write request while not connected";
        return false;
    }

    PacketBuilder builder { MessageType::ApplicationData, m_context.version, buffer.size() };
    builder.append(buffer);
    auto packet = builder.build();

    update_packet(packet);
    write_packet(packet);

    return true;
}

void TLSv12::write_packet(ByteBuffer& packet)
{
    m_context.tls_buffer.append(packet.data(), packet.size());
}

void TLSv12::update_packet(ByteBuffer& packet)
{
    u32 header_size = 5;
    *(u16*)packet.offset_pointer(3) = convert_between_host_and_network((u16)(packet.size() - header_size));

    if (packet[0] != (u8)MessageType::ChangeCipher) {
        if (packet[0] == (u8)MessageType::Handshake && packet.size() > header_size) {
            u8 handshake_type = packet[header_size];
            if (handshake_type != 0x00 && handshake_type != 0x03) {
                update_hash(packet.slice_view(header_size, packet.size() - header_size));
            }
        }
        if (m_context.cipher_spec_set && m_context.crypto.created) {
            size_t length = packet.size() - header_size + mac_length();
            auto block_size = m_aes_local->cipher().block_size();
            // if length is a multiple of block size, pad it up again
            // since it seems no one handles aligned unpadded blocks
            size_t padding = 0;
            if (length % block_size == 0) {
                padding = block_size;
                length += padding;
            }
            size_t mac_size = mac_length();

            if (m_context.crypto.created == 1) {
                // `buffer' will continue to be encrypted
                auto buffer = ByteBuffer::create_zeroed(length);
                size_t buffer_position = 0;
                u16 aligned_length = length + block_size - length % block_size;

                // we need enough space for a header, 16 bytes of IV and whatever the packet contains
                auto ct = ByteBuffer::create_zeroed(aligned_length + header_size + 16);

                // copy the header over
                ct.overwrite(0, packet.data(), header_size - 2);

                // copy the packet, sans the header
                buffer.overwrite(buffer_position, packet.offset_pointer(header_size), packet.size() - header_size);
                buffer_position += packet.size() - header_size;

                // get the appropricate HMAC value for the entire packet
                auto mac = hmac_message(packet, {}, mac_size, true);

                // write the MAC
                buffer.overwrite(buffer_position, mac.data(), mac.size());
                buffer_position += mac.size();

                // if there's some padding to be done (since a packet MUST always be padded)
                // apply it manually
                if (padding) {
                    memset(buffer.offset_pointer(buffer_position), padding - 1, padding);
                    buffer_position += padding;
                }

                // should be the same value, but the manual padding
                // throws a wrench into our plans
                buffer.trim(buffer_position);

                // make a random seed IV for this message
                u8 record_iv[16] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
                // arc4random_buf(record_iv, 16);

                auto iv = ByteBuffer::wrap(record_iv, 16);

                // write it into the ciphertext portion of the message
                ct.overwrite(header_size, record_iv, 16);
                ct.trim(length + block_size - length % block_size + header_size + block_size - padding);

                // get a block to encrypt into
                auto view = ct.slice_view(header_size + 16, length + block_size - length % block_size + block_size - padding - 16);

                // encrypt the message
                m_aes_local->encrypt(buffer, view, iv);

                // store the correct ciphertext length into the packet
                u16 ct_length = (u16)ct.size() - header_size;
                *(u16*)ct.offset_pointer(header_size - 2) = convert_between_host_and_network(ct_length);

                // replace the packet with the ciphertext
                packet = ct;
            }
        }
    }
    ++m_context.local_sequence_number;
}

ssize_t TLSv12::handle_hello(const ByteBuffer& buffer, size_t& write_packets)
{
    write_packets = 0;
    if (m_context.connection_status != 0 && m_context.connection_status != 4) {
        dbg() << "unexpected hello message";
        return (i8)Error::UnexpectedMessage;
    }
    ssize_t res = 0;
    size_t min_hello_size = 41;

    if (min_hello_size > buffer.size()) {
        dbg() << "need more data";
        return (i8)Error::NeedMoreData;
    }
    size_t following_bytes = buffer[0] * 0x10000 + buffer[1] * 0x100 + buffer[2];
    res += 3;
    if (buffer.size() - res < following_bytes) {
        dbg() << "not enough data after header: " << buffer.size() - res << " < " << following_bytes;
        return (i8)Error::NeedMoreData;
    }

    if (buffer.size() - res < 2) {
        dbg() << "not enough data for version";
        return (i8)Error::NeedMoreData;
    }
    auto version = (Version)convert_between_host_and_network(*(const u16*)buffer.offset_pointer(res));

    res += 2;
    if (!version_supported(version))
        return (i8)Error::NotSafe;

    memcpy(m_context.remote_random, buffer.offset_pointer(res), sizeof(m_context.remote_random));
    res += sizeof(m_context.remote_random);

    u8 session_length = buffer[res++];
    if (buffer.size() - res < session_length) {
        dbg() << "not enough data for session id";
        return (i8)Error::NeedMoreData;
    }

    if (session_length && session_length <= 32) {
        memcpy(m_context.session_id, buffer.offset_pointer(res), session_length);
        m_context.session_id_size = session_length;
        dbg() << "Remote session ID:";
        print_buffer(ByteBuffer::wrap(m_context.session_id, session_length));
    } else {
        m_context.session_id_size = 0;
    }
    res += session_length;

    if (buffer.size() - res < 2) {
        dbg() << "not enough data for cipher suite listing";
        return (i8)Error::NeedMoreData;
    }
    auto cipher = (CipherSuite)convert_between_host_and_network(*(const u16*)buffer.offset_pointer(res));
    res += 2;
    if (!cipher_supported(cipher)) {
        m_context.cipher = CipherSuite::Invalid;
        dbg() << "No supported cipher could be agreed upon";
        return (i8)Error::NoCommonCipher;
    }
    m_context.cipher = cipher;
    dbg() << "Cipher: " << (u16)cipher;

    if (buffer.size() - res < 1) {
        dbg() << "not enough data for compression spec";
        return (i8)Error::NeedMoreData;
    }
    u8 compression = buffer[res++];
    if (compression != 0) {
        dbg() << "Server told us to compress, we will not!";
        return (i8)Error::CompressionNotSupported;
    }

    if (res > 0) {
        if (m_context.connection_status != 4)
            m_context.connection_status = 1;
        if (m_context.is_server) {
            dbg() << "unsupported: server mode";
            write_packets = 2;
        }
    }

    if (res > 2) {
        res += 2;
    }

    while ((ssize_t)buffer.size() - res >= 4) {
        u16 extension_type = convert_between_host_and_network(*(const u16*)buffer.offset_pointer(res));
        res += 2;
        u16 extension_length = convert_between_host_and_network(*(const u16*)buffer.offset_pointer(res));
        res += 2;

        dbg() << "extension " << extension_type << " with length " << extension_length;
        if (extension_length) {
            if (buffer.size() - res < extension_length) {
                dbg() << "not enough data for extension";
                return (i8)Error::NeedMoreData;
            }

            // SNI
            if (extension_type == 0x00) {
                u16 sni_host_length = convert_between_host_and_network(*(const u16*)buffer.offset_pointer(res + 3));
                if (buffer.size() - res - 5 < sni_host_length) {
                    dbg() << "Not enough data for sni " << (buffer.size() - res - 5) << " < " << sni_host_length;
                    return (i8)Error::NeedMoreData;
                }

                if (sni_host_length) {
                    m_context.SNI = String { (const char*)buffer.offset_pointer(res + 5), sni_host_length };
                    dbg() << "server name indicator: " << m_context.SNI;
                }
            } else if (extension_type == 0x10 && m_context.alpn.size()) {
                if (buffer.size() - res > 2) {
                    auto alpn_length = convert_between_host_and_network(*(const u16*)buffer.offset_pointer(res));
                    if (alpn_length && alpn_length <= extension_length - 2) {
                        const u8* alpn = buffer.offset_pointer(res + 2);
                        size_t alpn_position = 0;
                        while (alpn_position < alpn_length) {
                            u8 alpn_size = alpn[alpn_position++];
                            if (alpn_size + alpn_position >= extension_length)
                                break;
                            String alpn_str { (const char*)alpn + alpn_position, alpn_length };
                            if (alpn_size && m_context.alpn.contains_slow(alpn_str)) {
                                m_context.negotiated_alpn = alpn_str;
                                dbg() << "negotiated alpn: " << alpn_str;
                                break;
                            }
                            alpn_position += alpn_length;
                            if (!m_context.is_server) // server hello must contain one ALPN
                                break;
                        }
                    }
                }
            } else if (extension_type == 0x0d) {
                dbg() << "supported signatures: ";
                print_buffer(buffer.slice_view(res, extension_length));
                // FIXME: what are we supposed to do here?
            }
            res += extension_length;
        }
    }

    return res;
}

ssize_t TLSv12::handle_finished(const ByteBuffer& buffer, size_t& write_packets)
{
    if (m_context.connection_status < 2 || m_context.connection_status == 0xff) {
        dbg() << "unexpected finished message";
        return (i8)Error::UnexpectedMessage;
    }

    write_packets = 0;

    if (buffer.size() < 3) {
        return (i8)Error::NeedMoreData;
    }

    size_t index = 3;

    u32 size = buffer[0] * 0x10000 + buffer[1] * 0x100 + buffer[2];
    index += 3;

    if (size < 12) {
        dbg() << "finished packet smaller than minimum size: " << size;
        return (i8)Error::BrokenPacket;
    }

    if (size < buffer.size() - index) {
        dbg() << "not enough data after length: " << size << " > " << buffer.size() - index;
        return (i8)Error::NeedMoreData;
    }

    // TODO: Compare Hashes
    dbg() << "FIXME: handle_finished :: Check message validity";
    m_context.connection_status = 0xff;

    return handle_message(buffer);
}

ssize_t TLSv12::handle_certificate(const ByteBuffer& buffer)
{
    ssize_t res = 0;

    if (buffer.size() < 3) {
        dbg() << "not enough certificate header data";
        return (i8)Error::NeedMoreData;
    }

    u32 certificate_total_length = buffer[0] * 0x10000 + buffer[1] * 0x100 + buffer[2];

    dbg() << "total length: " << certificate_total_length;

    if (certificate_total_length <= 4)
        return 3 * certificate_total_length;

    res += 3;

    if (certificate_total_length > buffer.size() - res) {
        dbg() << "not enough data for claimed total cert length";
        return (i8)Error::NeedMoreData;
    }
    size_t size = certificate_total_length;

    size_t index = 0;
    bool valid_certificate = false;

    while (size > 0) {
        ++index;
        if (buffer.size() - res < 3) {
            dbg() << "not enough data for certificate length";
            return (i8)Error::NeedMoreData;
        }
        size_t certificate_size = buffer[res] * 0x10000 + buffer[res + 1] * 0x100 + buffer[res + 2];
        res += 3;

        if (buffer.size() - res < certificate_size) {
            dbg() << "not enough data for certificate body";
            return (i8)Error::NeedMoreData;
        }

        auto res_cert = res;
        auto remaining = certificate_size;
        size_t certificates_in_chain = 0;

        do {
            if (remaining <= 3)
                break;
            ++certificates_in_chain;
            if (buffer.size() < (size_t)res_cert + 3)
                break;
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
            res_cert += certificate_size;
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

ssize_t TLSv12::handle_server_key_exchange(const ByteBuffer&)
{
    dbg() << "FIXME: parse_server_key_exchange";
    return 0;
}

ssize_t TLSv12::handle_server_hello_done(const ByteBuffer& buffer)
{
    if (buffer.size() < 3)
        return (i8)Error::NeedMoreData;

    size_t size = buffer[0] * 0x10000 + buffer[1] * 0x100 + buffer[2];

    if (buffer.size() - 3 < size)
        return (i8)Error::NeedMoreData;

    return size + 3;
}

ssize_t TLSv12::handle_verify(const ByteBuffer&)
{
    dbg() << "FIXME: parse_verify";
    return 0;
}

void TLSv12::update_hash(const ByteBuffer& message)
{
    m_context.handshake_hash.hash.update(message);
}

bool TLSv12::connect(const String& hostname, int port)
{
    set_sni(hostname);
    return Core::Socket::connect(hostname, port);
}

bool TLSv12::common_connect(const struct sockaddr* saddr, socklen_t length)
{
    if (m_context.critical_error)
        return false;

    if (Core::Socket::is_connected()) {
        if (established()) {
            ASSERT_NOT_REACHED();
        } else {
            Core::Socket::close(); // reuse?
        }
    }

    auto packet = build_hello();
    write_packet(packet);

    Core::Socket::on_connected = [this] {
        Core::Socket::on_ready_to_read = [this] {
            if (!Core::Socket::is_open()) {
                // an abrupt closure (the server is a jerk)
                dbg() << "Socket not open, assuming abrupt closure";
                m_context.connection_finished = true;
            }
            if (m_context.critical_error) {
                dbg() << "READ CRITICAL ERROR " << m_context.critical_error << " :(";
                if (on_tls_error)
                    on_tls_error((AlertDescription)m_context.critical_error);
                return;
            }
            if (m_context.connection_finished) {
                if (on_tls_finished)
                    on_tls_finished();
                if (m_context.tls_buffer.size()) {
                    dbg() << "connection closed without finishing data transfer, " << m_context.tls_buffer.size() << " bytes still in buffer";
                } else {
                    m_context.connection_finished = false;
                }
                if (!m_context.application_buffer.size())
                    m_context.connection_status = 0;
                return;
            }
            flush();
            consume(Core::Socket::read(4096)); // FIXME: how much is proper?
            if (established() && m_context.application_buffer.size())
                if (on_tls_ready_to_read)
                    on_tls_ready_to_read(*this);
        };
        Core::Socket::on_ready_to_write = [this] {
            if (!Core::Socket::is_open()) {
                // an abrupt closure (the server is a jerk)
                dbg() << "Socket not open, assuming abrupt closure";
                m_context.connection_finished = true;
            }
            if (m_context.critical_error) {
                dbg() << "WRITE CRITICAL ERROR " << m_context.critical_error << " :(";
                if (on_tls_error)
                    on_tls_error((AlertDescription)m_context.critical_error);
                return;
            }
            if (m_context.connection_finished) {
                if (on_tls_finished)
                    on_tls_finished();
                if (m_context.tls_buffer.size()) {
                    dbg() << "connection closed without finishing data transfer, " << m_context.tls_buffer.size() << " bytes still in buffer";
                } else {
                    m_context.connection_finished = false;
                    dbg() << "FINISHED";
                }
                if (!m_context.application_buffer.size()) {
                    m_context.connection_status = 0;
                    return;
                }
            }
            flush();
            if (established() && !m_context.application_buffer.size()) // hey client, you still have stuff to read...
                if (on_tls_ready_to_write)
                    on_tls_ready_to_write(*this);
        };
        if (on_tls_connected)
            on_tls_connected();
    };
    bool success = Core::Socket::common_connect(saddr, length);
    if (!success)
        return false;

    return true;
}

bool TLSv12::flush()
{
    auto out_buffer = write_buffer().data();
    size_t out_buffer_index { 0 };
    size_t out_buffer_length = write_buffer().size();

    if (out_buffer_length == 0)
        return true;

#ifdef TLS_DEBUG
    dbg() << "SENDING...";
    print_buffer(out_buffer, out_buffer_length);
#endif
    if (Core::Socket::write(&out_buffer[out_buffer_index], out_buffer_length)) {
        write_buffer().clear();
        return true;
    } else
        return false;
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
        auto length = convert_between_host_and_network(*(u16*)m_context.message_buffer.offset_pointer(index + size_offset)) + header_size;
        if (length > buffer_length) {
            dbg() << "Need more data: " << length << " | " << buffer_length;
            break;
        }
        auto consumed = handle_message(m_context.message_buffer.slice_view(index, length));
        if (consumed > 0)
            dbg() << "consumed " << (size_t)consumed << " bytes";
        else
            dbg() << "error: " << (int)consumed;

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

ssize_t TLSv12::handle_message(const ByteBuffer& buffer)
{
    auto res { 5ll };
    size_t header_size = res;
    ssize_t payload_res = 0;

#ifdef TLS_DEBUG
    dbg() << "buffer size: " << buffer.size();
#endif
    if (buffer.size() < 5) {
        return (i8)Error::NeedMoreData;
    }

    auto type = (MessageType)buffer[0];
    size_t buffer_position { 1 };

    // FIXME: Read the version and verify it
#ifdef TLS_DEBUG
    auto version = (Version) * (const u16*)buffer.offset_pointer(buffer_position);
    dbg() << "type: " << (u8)type << " version: " << (u16)version;
#endif
    buffer_position += 2;

    auto length = convert_between_host_and_network(*(const u16*)buffer.offset_pointer(buffer_position));
    dbg() << "record length: " << length << " at offset: " << buffer_position;
    buffer_position += 2;

    if (buffer_position + length > buffer.size()) {
        dbg() << "record length more than what we have: " << buffer.size();
        return (i8)Error::NeedMoreData;
    }

#ifdef TLS_DEBUG
    dbg() << "message type: " << (u8)type << ", length: " << length;
#endif
    ByteBuffer plain = buffer.slice_view(buffer_position, buffer.size() - buffer_position);

    if (m_context.cipher_spec_set && type != MessageType::ChangeCipher) {
#ifdef TLS_DEBUG
        dbg() << "Encrypted: ";
        print_buffer(buffer.slice_view(header_size, length));
#endif

        ASSERT(m_aes_remote);

        auto decrypted = m_aes_remote->create_aligned_buffer(length - 16);
        auto iv = buffer.slice_view(header_size, 16);

        m_aes_remote->decrypt(buffer.slice_view(header_size + 16, length - 16), decrypted, iv);

        length = decrypted.size();

#ifdef TLS_DEBUG
        dbg() << "Decrypted: ";
        print_buffer(decrypted);
#endif

        auto mac_size = mac_length();
        if (length < mac_size) {
            dbg() << "broken packet";
            auto packet = build_alert(true, (u8)AlertDescription::DecryptError);
            write_packet(packet);
            return (i8)Error::BrokenPacket;
        }

        const u8* message_hmac = decrypted.offset_pointer(length - mac_size);
        u8 temp_buf[5];
        memcpy(temp_buf, buffer.offset_pointer(0), 3);
        *(u16*)(temp_buf + 3) = convert_between_host_and_network(length);
        auto hmac = hmac_message(ByteBuffer::wrap(temp_buf, 5), decrypted, mac_size);
        auto message_mac = ByteBuffer::wrap(message_hmac, mac_size);
        if (hmac != message_mac) {
            dbg() << "integrity check failed (mac length " << length << ")";
            dbg() << "mac received:";
            print_buffer(message_mac);
            dbg() << "mac computed:";
            print_buffer(hmac);
            auto packet = build_alert(true, (u8)AlertDescription::BadRecordMAC);
            write_packet(packet);

            return (i8)Error::IntegrityCheckFailed;
        }
        plain = decrypted.slice(0, length - mac_size);
    }
    m_context.remote_sequence_number++;

    switch (type) {
    case MessageType::ApplicationData:
        if (m_context.connection_status != 0xff) {
            dbg() << "unexpected application data";
            payload_res = (i8)Error::UnexpectedMessage;
            auto packet = build_alert(true, (u8)AlertDescription::UnexpectedMessage);
            write_packet(packet);
        } else {
#ifdef TLS_DEBUG
            dbg() << "application data message of size " << plain.size();
#endif

            m_context.application_buffer.append(plain.data(), plain.size());
        }
        break;
    case MessageType::Handshake:
#ifdef TLS_DEBUG
        dbg() << "tls handshake message";
#endif
        payload_res = handle_payload(plain);
        break;
    case MessageType::ChangeCipher:
        if (m_context.connection_status != 2) {
            dbg() << "unexpected change cipher message";
            auto packet = build_alert(true, (u8)AlertDescription::UnexpectedMessage);
            payload_res = (i8)Error::UnexpectedMessage;
        } else {
#ifdef TLS_DEBUG
            dbg() << "change cipher spec message";
#endif
            m_context.cipher_spec_set = true;
            m_context.remote_sequence_number = 0;
        }
        break;
    case MessageType::Alert:
        dbg() << "alert message of length " << length;
        if (length >= 2) {
            print_buffer(plain);
            auto level = plain[0];
            auto code = plain[1];
            if (level == (u8)AlertLevel::Critical) {
                dbg() << "We were alerted of a critical error: " << code;
                m_context.critical_error = code;
                res = (i8)Error::UnknownError;
            } else {
                dbg() << "Alert: " << code;
            }
            if (code == 0) {
                // close notify
                res += 2;
                auto closure_alert = build_alert(true, (u8)AlertDescription::CloseNotify);
                write_packet(closure_alert);
                flush();
                m_context.connection_finished = true;
            }
            m_context.error_code = (Error)code;
        }
        break;
    default:
        dbg() << "message not understood";
        return (i8)Error::NotUnderstood;
    }

    if (payload_res < 0)
        return payload_res;

    if (res > 0)
        return header_size + length;

    return res;
}

ByteBuffer TLSv12::hmac_message(const ByteBuffer& buf, const Optional<ByteBuffer> buf2, size_t mac_length, bool local)
{
    u64 sequence_number = convert_between_host_and_network(local ? m_context.local_sequence_number : m_context.remote_sequence_number);
    auto digest = [&](auto& hmac) {
#ifdef TLS_DEBUG
        dbg() << "========================= PACKET DATA ==========================";
        print_buffer((const u8*)&sequence_number, sizeof(u64));
        print_buffer(buf);
        if (buf2.has_value())
            print_buffer(buf2.value());
        dbg() << "========================= PACKET DATA ==========================";
#endif
        hmac.update((const u8*)&sequence_number, sizeof(u64));
        hmac.update(buf);
        if (buf2.has_value() && buf2.value().size()) {
            hmac.update(buf2.value());
        }
        auto mac = ByteBuffer::copy(hmac.digest().data, hmac.DigestSize);
#ifdef TLS_DEBUG
        dbg() << "HMAC of the block for sequence number " << m_context.local_sequence_number;
        print_buffer(mac);
#endif
        return mac;
    };
    switch (mac_length) {
    case Crypto::Hash::SHA256::DigestSize: {
        Crypto::Authentication::HMAC<Crypto::Hash::SHA256> hmac(ByteBuffer::wrap(local ? m_context.crypto.local_mac : m_context.crypto.remote_mac, 32));
        return digest(hmac);
    }
    case Crypto::Hash::SHA512::DigestSize: {
        Crypto::Authentication::HMAC<Crypto::Hash::SHA512> hmac(ByteBuffer::wrap(local ? m_context.crypto.local_mac : m_context.crypto.remote_mac, 32));
        return digest(hmac);
    }
    default:
        return {};
    }
}

ssize_t TLSv12::handle_payload(const ByteBuffer& vbuffer)
{
    if (m_context.connection_status == 0xff) {
        auto packet = build_alert(false, (u8)AlertDescription::NoRenegotiation);
        write_packet(packet);
        return 1;
    }
    auto buffer = vbuffer;
    auto buffer_length = buffer.size();
    auto original_length = buffer_length;
    while (buffer_length >= 4 && !m_context.critical_error) {
        ssize_t payload_res = 0;
        if (buffer_length < 1)
            return (i8)Error::NeedMoreData;
        auto type = buffer[0];
        size_t write_packets = 0;
        size_t payload_size = buffer[1] * 0x10000 + buffer[2] * 0x100 + buffer[3] + 3;
#ifdef TLS_DEBUG
        dbg() << "payload size: " << payload_size << " buffer length: " << buffer_length;
#endif
        if (payload_size + 1 > buffer_length)
            return (i8)Error::NeedMoreData;

        switch (type) {
        // hello request
        case 0x00:
            if (m_context.handshake_messages[0] >= 1) {
                dbg() << "unexpected hello request message";
                payload_res = (i8)Error::UnexpectedMessage;
                break;
            }
            ++m_context.handshake_messages[0];
            dbg() << "hello request (renegotiation?)";
            if (m_context.connection_status == 0xff) {
                // renegotiation
                payload_res = (i8)Error::NoRenegotiation;
            } else {
                // :shrug:
                payload_res = (i8)Error::UnexpectedMessage;
            }
            break;
        // client hello
        case 0x01:
            // FIXME: We only support client mode right now
            if (m_context.is_server) {
                ASSERT_NOT_REACHED();
            }
            payload_res = (i8)Error::UnexpectedMessage;
            break;
        // server hello
        case 0x02:
            if (m_context.handshake_messages[2] >= 1) {
                dbg() << "unexpected server hello message";
                payload_res = (i8)Error::UnexpectedMessage;
                break;
            }
            ++m_context.handshake_messages[2];
#ifdef TLS_DEBUG
            dbg() << "server hello";
#endif
            if (m_context.is_server) {
                dbg() << "unsupported: server mode";
                ASSERT_NOT_REACHED();
            } else {
                payload_res = handle_hello(buffer.slice_view(1, payload_size), write_packets);
            }
            break;
        // hello verify request
        case 0x03:
            dbg() << "unsupported: DTLS";
            payload_res = (i8)Error::UnexpectedMessage;
            break;
        // certificate
        case 0x0b:
            if (m_context.handshake_messages[4] >= 1) {
                dbg() << "unexpected certificate message";
                payload_res = (i8)Error::UnexpectedMessage;
                break;
            }
            ++m_context.handshake_messages[4];
#ifdef TLS_DEBUG
            dbg() << "certificate";
#endif
            if (m_context.connection_status == 1) {
                if (m_context.is_server) {
                    dbg() << "unsupported: server mode";
                    ASSERT_NOT_REACHED();
                }
                payload_res = handle_certificate(buffer.slice_view(1, payload_size));
            } else {
                payload_res = (i8)Error::UnexpectedMessage;
            }
            break;
        // server key exchange
        case 0x0c:
            if (m_context.handshake_messages[5] >= 1) {
                dbg() << "unexpected server key exchange message";
                payload_res = (i8)Error::UnexpectedMessage;
                break;
            }
            ++m_context.handshake_messages[5];
#ifdef TLS_DEBUG
            dbg() << "server key exchange";
#endif
            if (m_context.is_server) {
                dbg() << "unsupported: server mode";
                ASSERT_NOT_REACHED();
            } else {
                payload_res = handle_server_key_exchange(buffer.slice_view(1, payload_size));
            }
            break;
        // certificate request
        case 0x0d:
            if (m_context.handshake_messages[6] >= 1) {
                dbg() << "unexpected certificate request message";
                payload_res = (i8)Error::UnexpectedMessage;
                break;
            }
            ++m_context.handshake_messages[6];
            if (m_context.is_server) {
                dbg() << "invalid request";
                dbg() << "unsupported: server mode";
                ASSERT_NOT_REACHED();
            } else {
                // we do not support "certificate request"
                dbg() << "certificate request";
                ASSERT_NOT_REACHED();
            }
            break;
        // server hello done
        case 0x0e:
            if (m_context.handshake_messages[7] >= 1) {
                dbg() << "unexpected server hello done message";
                payload_res = (i8)Error::UnexpectedMessage;
                break;
            }
            ++m_context.handshake_messages[7];
#ifdef TLS_DEBUG
            dbg() << "server hello done";
#endif
            if (m_context.is_server) {
                dbg() << "unsupported: server mode";
                ASSERT_NOT_REACHED();
            } else {
                payload_res = handle_server_hello_done(buffer.slice_view(1, payload_size));
                if (payload_res > 0)
                    write_packets = 1;
            }
            break;
        // certificate verify
        case 0x0f:
            if (m_context.handshake_messages[8] >= 1) {
                dbg() << "unexpected certificate verify message";
                payload_res = (i8)Error::UnexpectedMessage;
                break;
            }
            ++m_context.handshake_messages[8];
#ifdef TLS_DEBUG
            dbg() << "certificate verify";
#endif
            if (m_context.connection_status == 2) {
                payload_res = handle_verify(buffer.slice_view(1, payload_size));
            } else {
                payload_res = (i8)Error::UnexpectedMessage;
            }
            break;
        // client key exchange
        case 0x10:
            if (m_context.handshake_messages[9] >= 1) {
                dbg() << "unexpected client key exchange message";
                payload_res = (i8)Error::UnexpectedMessage;
                break;
            }
            ++m_context.handshake_messages[9];
#ifdef TLS_DEBUG
            dbg() << "client key exchange";
#endif
            if (m_context.is_server) {
                dbg() << "unsupported: server mode";
                ASSERT_NOT_REACHED();
            } else {
                payload_res = (i8)Error::UnexpectedMessage;
            }
            break;
        // finished
        case 0x14:
            if (m_context.cached_handshake) {
                m_context.cached_handshake.clear();
            }
            if (m_context.handshake_messages[10] >= 1) {
                dbg() << "unexpected finished message";
                payload_res = (i8)Error::UnexpectedMessage;
                break;
            }
            ++m_context.handshake_messages[10];
#ifdef TLS_DEBUG
            dbg() << "finished";
#endif
            payload_res = handle_finished(buffer.slice_view(1, payload_size), write_packets);
            if (payload_res > 0) {
                memset(m_context.handshake_messages, 0, sizeof(m_context.handshake_messages));
            }
            break;
        default:
            dbg() << "message type not understood: " << type;
            return (i8)Error::NotUnderstood;
        }

        if (type != 0x00) {
            update_hash(buffer.slice_view(0, payload_size + 1));
        }

        // if something went wrong, send an alert about it
        if (payload_res < 0) {
            switch ((Error)payload_res) {
            case Error::UnexpectedMessage: {
                auto packet = build_alert(true, (u8)AlertDescription::UnexpectedMessage);
                write_packet(packet);
                break;
            }
            case Error::CompressionNotSupported: {
                auto packet = build_alert(true, (u8)AlertDescription::DecompressionFailure);
                write_packet(packet);
                break;
            }
            case Error::BrokenPacket: {
                auto packet = build_alert(true, (u8)AlertDescription::DecodeError);
                write_packet(packet);
                break;
            }
            case Error::NotVerified: {
                auto packet = build_alert(true, (u8)AlertDescription::BadRecordMAC);
                write_packet(packet);
                break;
            }
            case Error::BadCertificate: {
                auto packet = build_alert(true, (u8)AlertDescription::BadCertificate);
                write_packet(packet);
                break;
            }
            case Error::UnsupportedCertificate: {
                auto packet = build_alert(true, (u8)AlertDescription::UnsupportedCertificate);
                write_packet(packet);
                break;
            }
            case Error::NoCommonCipher: {
                auto packet = build_alert(true, (u8)AlertDescription::InsufficientSecurity);
                write_packet(packet);
                break;
            }
            case Error::NotUnderstood: {
                auto packet = build_alert(true, (u8)AlertDescription::InternalError);
                write_packet(packet);
                break;
            }
            case Error::NoRenegotiation: {
                auto packet = build_alert(true, (u8)AlertDescription::NoRenegotiation);
                write_packet(packet);
                break;
            }
            case Error::DecryptionFailed: {
                auto packet = build_alert(true, (u8)AlertDescription::DecryptionFailed);
                write_packet(packet);
                break;
            }
            default:
                break;
            }
            if (payload_res < 0)
                return payload_res;
        }
        switch (write_packets) {
        case 1:
            if (m_context.client_verified == 2) {
                auto packet = build_certificate();
                write_packet(packet);
                m_context.client_verified = 0;
            }
            // client handshake
            {
#ifdef TLS_DEBUG
                dbg() << "> Key exchange";
#endif
                auto packet = build_client_key_exchange();
                write_packet(packet);
            }
            {
#ifdef TLS_DEBUG
                dbg() << "> change cipher spec";
#endif
                auto packet = build_change_cipher_spec();
                write_packet(packet);
            }
            m_context.cipher_spec_set = 1;
            m_context.local_sequence_number = 0;
            {
#ifdef TLS_DEBUG
                dbg() << "> client finished";
#endif
                auto packet = build_finished();
                write_packet(packet);
            }
            m_context.cipher_spec_set = 0;
            break;
        case 2:
            // server handshake
            dbg() << "UNSUPPORTED: Server mode";
            ASSERT_NOT_REACHED();
            break;
        case 3:
            // finished
            {
#ifdef TLS_DEBUG
                dbg() << "> change cipher spec";
#endif
                auto packet = build_change_cipher_spec();
                write_packet(packet);
            }
            {
#ifdef TLS_DEBUG
                dbg() << "> client finished";
#endif
                auto packet = build_finished();
                write_packet(packet);
            }
            m_context.connection_status = 0xff;
            break;
        }
        payload_size++;
        buffer_length -= payload_size;
        buffer = buffer.slice(payload_size, buffer_length);
    }
    return original_length;
}

}
