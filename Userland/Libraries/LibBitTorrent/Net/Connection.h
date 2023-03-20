/*
 * Copyright (c) 2023, Pierre Delagrave <pierre.delagrave@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/RefCounted.h>
#include <LibCore/DateTime.h>
#include <LibCore/Socket.h>

namespace BitTorrent {

using ConnectionId = u64;

struct Connection : public RefCounted<Connection> {
    AK_MAKE_NONCOPYABLE(Connection);

public:
    static Atomic<ConnectionId> s_next_connection_id;
    static ErrorOr<NonnullRefPtr<Connection>> try_create(ConnectionId connection_id, NonnullOwnPtr<Core::TCPSocket>& socket, NonnullRefPtr<Core::Notifier> write_notifier, size_t input_buffer_size, size_t output_buffer_size);

    ConnectionId const id;
    NonnullOwnPtr<Core::TCPSocket> socket;
    NonnullRefPtr<Core::Notifier> write_notifier;

    CircularBuffer input_message_buffer;
    CircularBuffer output_message_buffer;

    BigEndian<u32> incoming_message_length;
    Core::DateTime last_message_received_at = Core::DateTime::now();
    Core::DateTime last_message_sent_at = Core::DateTime::now();

    u64 bytes_downloaded_since_last_speed_measurement { 0 };
    u64 download_speed { 0 };

    u64 bytes_uploaded_since_last_speed_measurement { 0 };
    u64 upload_speed { 0 };

    // Read from the socket, but not necessarily accepted by the engine.
    bool handshake_received { false };

    // Sent on the socket
    bool handshake_sent { false };

    // True once the handshake was accepted by the engine and ours was sent.
    bool session_established { false };

private:
    Connection(ConnectionId connection_id, NonnullOwnPtr<Core::TCPSocket>& socket, NonnullRefPtr<Core::Notifier>& write_notifier, CircularBuffer& input_message_buffer, CircularBuffer& output_message_buffer);
};

}

template<>
struct AK::Formatter<BitTorrent::Connection> : AK::Formatter<FormatString> {
    ErrorOr<void> format(FormatBuilder& builder, BitTorrent::Connection const& value)
    {
        return Formatter<FormatString>::format(builder, "id:{} {}"sv, value.id, value.socket->address());
    }
};
