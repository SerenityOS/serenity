/*
 * Copyright (c) 2020, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Random.h>
#include <LibCrypto/ASN1/DER.h>
#include <LibCrypto/PK/Code/EMSA_PSS.h>
#include <LibTLS/TLSv12.h>

namespace TLS {

ByteBuffer TLSv12::build_hello()
{
    fill_with_random(&m_context.local_random, 32);

    auto packet_version = (u16)m_context.options.version;
    auto version = (u16)m_context.options.version;
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
    builder.append((u16)(m_context.options.usable_cipher_suites.size() * sizeof(u16)));
    for (auto suite : m_context.options.usable_cipher_suites)
        builder.append((u16)suite);

    // we don't like compression
    VERIFY(!m_context.options.use_compression);
    builder.append((u8)1);
    builder.append((u8)m_context.options.use_compression);

    // set SNI if we have one, and the user hasn't explicitly asked us to omit it.
    auto sni_length = 0;
    if (!m_context.extensions.SNI.is_null() && m_context.options.use_sni)
        sni_length = m_context.extensions.SNI.length();

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
        builder.append((const u8*)m_context.extensions.SNI.characters(), sni_length);
    }

    if (alpn_length) {
        // TODO
        VERIFY_NOT_REACHED();
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
    PacketBuilder builder(MessageType::Alert, (u16)m_context.options.version);
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
    PacketBuilder builder { MessageType::Handshake, m_context.options.version, 12 + 64 };
    builder.append((u8)HandshakeType::Finished);

    u32 out_size = 12;

    builder.append_u24(out_size);

    u8 out[out_size];
    auto outbuffer = Bytes { out, out_size };
    auto dummy = ByteBuffer::create_zeroed(0);

    auto digest = m_context.handshake_hash.digest();
    auto hashbuf = ReadonlyBytes { digest.immutable_data(), m_context.handshake_hash.digest_size() };
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
