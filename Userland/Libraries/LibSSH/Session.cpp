/*
 * Copyright (c) 2026, Lucas Chollet <lucas.chollet@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Session.h"

namespace SSH {

ErrorOr<Session> Session::create(u32 sender_channel_id, u32 window_size, u32 maximum_packet_size)
{
    Session session;
    session.local_channel_id = sender_channel_id;
    session.sender_channel_id = sender_channel_id;
    session.maximum_packet_size = maximum_packet_size;
    session.window = TRY(ByteBuffer::create_zeroed(window_size));
    return session;
}

}
