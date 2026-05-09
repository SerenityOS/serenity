/*
 * Copyright (c) 2026, Lucas Chollet <lucas.chollet@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Peer.h"

#include <AK/Debug.h>
#include <AK/Endian.h>
#include <AK/Random.h>

namespace SSH {

// 6.  Binary Packet Protocol
// https://datatracker.ietf.org/doc/html/rfc4253#section-6
ErrorOr<ByteBuffer> Peer::read_packet(ByteBuffer& data)
{
    // Packet length is a u32, we want at least one byte of actual data
    // and 4 of padding.
    if (data.size() < m_cipher->mac_size() + sizeof(u32) + 1 + 4)
        return Error::from_string_literal("Packet is too small");

    TRY(m_cipher->decrypt(m_incoming_packet_sequence_number, data.bytes()));

    auto stream = FixedMemoryStream { data.bytes() };
    u32 packet_length = TRY(stream.read_value<NetworkOrdered<u32>>());

    // "All implementations MUST be able to process packets with an
    // uncompressed payload length of 32768 bytes or less and a total
    // packet size of 35000 bytes or less."
    // "However, implementations SHOULD check that the packet length
    // is reasonable in order for the implementation to avoid denial
    // of service and/or buffer overflow attacks."
    if (packet_length > 20 * PAGE_SIZE)
        return Error::from_string_literal("Packet length is too long");

    u8 padding_length = TRY(stream.read_value<u8>());

    if (packet_length == padding_length)
        return Error::from_string_literal("Packet doesn't have a payload");

    // "There MUST be at least four bytes of padding."
    if (padding_length < 4)
        return Error::from_string_literal("Padding length is too short");

    auto payload_length = packet_length - padding_length - 1;
    auto payload = TRY(ByteBuffer::create_uninitialized(payload_length));
    TRY(stream.read_until_filled(payload));
    // FIXME: Uncompress payload if it was negotiated.

    m_incoming_packet_sequence_number++;

    auto total_packet_size = sizeof(u32) + packet_length + m_cipher->mac_size();
    data = TRY(data.slice(total_packet_size, data.size() - total_packet_size));

    return payload;
}

ErrorOr<void> Peer::write_packet(ReadonlyBytes payload)
{
    AllocatingMemoryStream stream;

    // "Arbitrary-length padding, such that the total length of
    // (packet_length || padding_length || payload || random padding)
    // is a multiple of the cipher block size or 8, whichever is
    // larger.

    auto const packet_alignment = max(8, m_cipher->block_size());
    auto size_without_padding = sizeof(u32) + sizeof(u8) + payload.size();

    // NOTE: Introduced with RFC 5647, AEAD (Authenticated Encryption with Associated Data) ciphers
    //       consider the packet_length field as AAD (Additional Authenticated Data). In this RFC, the
    //       packet length is sent as plain text and thus does not influence the padding length calculation.
    //       Being an AEAD cipher, ChaCha20Poly1305 inherits from this rule even if the packet length is also
    //       encrypted.
    VERIFY(size_without_padding > m_cipher->aad_size());
    size_without_padding -= m_cipher->aad_size();

    auto padding_length = packet_alignment - (size_without_padding % packet_alignment);

    // "There MUST be at least four bytes of padding."
    if (padding_length <= 4)
        padding_length += packet_alignment;
    VERIFY(padding_length <= 255);

    u32 packet_length = 1 + payload.size() + padding_length;

    TRY(stream.write_value<NetworkOrdered<u32>>(packet_length));
    TRY(stream.write_value<u8>(padding_length));
    TRY(stream.write_until_depleted(payload));

    auto random_padding = TRY(ByteBuffer::create_uninitialized(padding_length));
    fill_with_random(random_padding);
    TRY(stream.write_until_depleted(random_padding));

    Array<u8, 16> mac_placeholder {};
    VERIFY(mac_placeholder.size() >= m_cipher->mac_size());
    TRY(stream.write_until_depleted(mac_placeholder.span().trim(m_cipher->mac_size())));

    auto packet_buffer = TRY(stream.read_until_eof());

    TRY(m_cipher->encrypt(m_outgoing_packet_sequence_number, packet_buffer));
    m_outgoing_packet_sequence_number++;

    TRY(m_tcp_socket.write_until_depleted(packet_buffer));
    return {};
}

// 7.3.  Taking Keys Into Use
// https://datatracker.ietf.org/doc/html/rfc4253#section-7.3
ErrorOr<void> Peer::send_new_keys_message()
{
    // "Key exchange ends by each side sending an SSH_MSG_NEWKEYS message.
    // This message is sent with the old keys and algorithms.  All messages
    // sent after this message MUST use the new keys and algorithms."
    Array<u8, 1> payload { to_underlying(MessageID::NEWKEYS) };
    TRY(write_packet(payload));
    return {};
}

ErrorOr<void> Peer::handle_new_keys_message(ByteBuffer& data)
{
    auto payload = TRY(read_packet(data));

    if (payload[0] != to_underlying(MessageID::NEWKEYS))
        return Error::from_string_literal("Expected NEWKEYS message");

    if (payload.size() != 1)
        return Error::from_string_literal("Invalid NEWKEYS message");

    dbgln_if(SSH_DEBUG, "NEWKEYS message received, we should use encryption from now on");

    m_cipher = ChaCha20Poly1305Cipher::create(m_shared_secret, m_hash, *m_session_id);

    return {};
}

ErrorOr<void> Peer::handle_disconnect_message(ByteBuffer& payload)
{
    VERIFY(payload[0] == to_underlying(MessageID::DISCONNECT));

    // FIXME: Parse and report the disconnection reason.

    dbgln_if(SSH_DEBUG, "Client disconnected");

    m_tcp_socket.close();
    return {};
}

} // SSH
