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

#include <AK/Random.h>
#include <LibCrypto/ASN1/DER.h>
#include <LibCrypto/PK/Code/EMSA_PSS.h>
#include <LibTLS/TLSv12.h>

namespace TLS {

ByteBuffer TLSv12::build_hello()
{
    AK::fill_with_random(&m_context.local_random, 32);

    auto packet_version = (u16)m_context.version;
    auto version = (u16)m_context.version;
    PacketBuilder builder { MessageType::Handshake, packet_version };

    builder.append((u8)ClientHello);

    // hello length (for later)
    u8 dummy[3] = {};
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
    builder.append((u16)(4 * sizeof(u16)));
    builder.append((u16)CipherSuite::RSA_WITH_AES_128_CBC_SHA256);
    builder.append((u16)CipherSuite::RSA_WITH_AES_256_CBC_SHA256);
    builder.append((u16)CipherSuite::RSA_WITH_AES_128_CBC_SHA);
    builder.append((u16)CipherSuite::RSA_WITH_AES_256_CBC_SHA);

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
        builder.append((u16)HandshakeExtension::ServerName);
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
    PacketBuilder builder(MessageType::Alert, (u16)m_context.version);
    builder.append((u8)(critical ? AlertLevel::Critical : AlertLevel::Warning));
    builder.append(code);

    if (critical)
        m_context.critical_error = code;

    auto packet = builder.build();
    update_packet(packet);

    return packet;
}

ByteBuffer TLSv12::build_finished()
{
    PacketBuilder builder { MessageType::Handshake, m_context.version, 12 + 64 };
    builder.append((u8)HandshakeType::Finished);

    u32 out_size = 12;

    builder.append_u24(out_size);

    u8 out[out_size];
    auto outbuffer = ByteBuffer::wrap(out, out_size);
    auto dummy = ByteBuffer::create_zeroed(0);

    auto digest = m_context.handshake_hash.digest();
    auto hashbuf = ByteBuffer::wrap(const_cast<u8*>(digest.immutable_data()), m_context.handshake_hash.digest_size());
    pseudorandom_function(outbuffer, m_context.master_key, (const u8*)"client finished", 15, hashbuf, dummy);

    builder.append(outbuffer);
    auto packet = builder.build();
    update_packet(packet);

    return packet;
}

void TLSv12::alert(AlertLevel level, AlertDescription code)
{
    auto the_alert = build_alert(level == AlertLevel::Critical, (u8)code);
    write_packet(the_alert);
    flush();
}

}
