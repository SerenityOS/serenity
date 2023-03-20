/*
 * Copyright (c) 2023, Pierre Delagrave <pierre.delagrave@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Connection.h"
#include "HandshakeMessage.h"

namespace BitTorrent {

Atomic<ConnectionId> Connection::s_next_connection_id { 0 };

ErrorOr<NonnullRefPtr<Connection>> Connection::try_create(ConnectionId connection_id, NonnullOwnPtr<Core::TCPSocket>& socket, NonnullRefPtr<Core::Notifier> write_notifier, size_t input_buffer_size, size_t output_buffer_size)
{
    auto input_buffer = TRY(CircularBuffer::create_empty(input_buffer_size));
    auto output_buffer = TRY(CircularBuffer::create_empty(output_buffer_size));
    return adopt_nonnull_ref_or_enomem(new (nothrow) Connection(connection_id, socket, write_notifier, input_buffer, output_buffer));
}

Connection::Connection(ConnectionId connection_id, NonnullOwnPtr<Core::TCPSocket>& socket, NonnullRefPtr<Core::Notifier>& write_notifier, CircularBuffer& input_message_buffer, CircularBuffer& output_message_buffer)
    : id(connection_id)
    , socket(move(socket))
    , write_notifier(move(write_notifier))
    , input_message_buffer(move(input_message_buffer))
    , output_message_buffer(move(output_message_buffer))
    , incoming_message_length(sizeof(HandshakeMessage))
{
}
}
