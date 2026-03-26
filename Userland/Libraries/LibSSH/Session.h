/*
 * Copyright (c) 2026, Lucas Chollet <lucas.chollet@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteBuffer.h>
#include <AK/Error.h>

namespace SSH {

// 6.1.  Opening a Session
// https://datatracker.ietf.org/doc/html/rfc4254#section-6.1
struct Session {
    static ErrorOr<Session> create(u32 sender_channel_id, u32 window_size, u32 maximum_packet_size);

    u32 local_channel_id {};
    u32 sender_channel_id {};
    u32 maximum_packet_size {};
    ByteBuffer window {};

    bool is_closed { false };
};

}
