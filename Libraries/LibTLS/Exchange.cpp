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

#include <LibCrypto/ASN1/DER.h>
#include <LibCrypto/PK/Code/EMSA_PSS.h>
#include <LibTLS/TLSv12.h>

namespace TLS {

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
    auto iv_size = iv_length();

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
    print_buffer(m_context.crypto.local_mac, mac_size);
    dbg() << "server mac key";
    print_buffer(m_context.crypto.remote_mac, mac_size);
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

    // RFC 5246: "In this section, we define one PRF, based on HMAC.  This PRF with the
    //            SHA-256 hash function is used for all cipher suites defined in this
    //            document and in TLS documents published prior to this document when
    //            TLS 1.2 is negotiated."
    // Apparently this PRF _always_ uses SHA256
    Crypto::Authentication::HMAC<Crypto::Hash::SHA256> hmac(secret);

    auto l_seed_size = label_length + seed.size() + seed_b.size();
    u8 l_seed[l_seed_size];
    auto label_seed_buffer = ByteBuffer::wrap(l_seed, l_seed_size);
    label_seed_buffer.overwrite(0, label, label_length);
    label_seed_buffer.overwrite(label_length, seed.data(), seed.size());
    label_seed_buffer.overwrite(label_length + seed.size(), seed_b.data(), seed_b.size());

    auto digest_size = hmac.digest_size();

    u8 digest[digest_size];

    auto digest_0 = ByteBuffer::wrap(digest, digest_size);

    digest_0.overwrite(0, hmac.process(label_seed_buffer).immutable_data(), digest_size);

    size_t index = 0;
    while (index < output.size()) {
        hmac.update(digest_0);
        hmac.update(label_seed_buffer);
        auto digest_1 = hmac.digest();

        auto copy_size = min(digest_size, output.size() - index);

        output.overwrite(index, digest_1.immutable_data(), copy_size);
        index += copy_size;

        digest_0.overwrite(0, hmac.process(digest_0).immutable_data(), digest_size);
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

ByteBuffer TLSv12::build_certificate()
{
    PacketBuilder builder { MessageType::Handshake, m_context.version };

    Vector<const Certificate*> certificates;
    Vector<Certificate>* local_certificates = nullptr;

    if (m_context.is_server) {
        dbg() << "Unsupported: Server mode";
        ASSERT_NOT_REACHED();
    } else {
        local_certificates = &m_context.client_certificates;
    }

    constexpr size_t der_length_delta = 3;
    constexpr size_t certificate_vector_header_size = 3;

    size_t total_certificate_size = 0;

    for (size_t i = 0; i < local_certificates->size(); ++i) {
        auto& certificate = local_certificates->at(i);
        if (!certificate.der.is_empty()) {
            total_certificate_size += certificate.der.size() + der_length_delta;

            // FIXME: Check for and respond with only the requested certificate types.
            if (true) {
                certificates.append(&certificate);
            }
        }
    }

    builder.append((u8)HandshakeType::CertificateMessage);

    if (!total_certificate_size) {
#ifdef TLS_DEBUG
        dbg() << "No certificates, sending empty certificate message";
#endif
        builder.append_u24(certificate_vector_header_size);
        builder.append_u24(total_certificate_size);
    } else {
        builder.append_u24(total_certificate_size + certificate_vector_header_size); // 3 bytes for header
        builder.append_u24(total_certificate_size);

        for (auto& certificate : certificates) {
            if (!certificate->der.is_empty()) {
                builder.append_u24(certificate->der.size());
                builder.append(certificate->der);
            }
        }
    }
    auto packet = builder.build();
    update_packet(packet);
    return packet;
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
    builder.append((u8)HandshakeType::ClientKeyExchange);
    build_random(builder);

    m_context.connection_status = ConnectionStatus::KeyExchange;

    auto packet = builder.build();

    update_packet(packet);

    return packet;
}

ssize_t TLSv12::handle_server_key_exchange(const ByteBuffer&)
{
    dbg() << "FIXME: parse_server_key_exchange";
    return 0;
}

ssize_t TLSv12::handle_verify(const ByteBuffer&)
{
    dbg() << "FIXME: parse_verify";
    return 0;
}

}
