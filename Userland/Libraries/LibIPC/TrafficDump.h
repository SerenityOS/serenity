/*
 * Copyright (c) 2023, Ben Wiederhake <BenWiederhake.GitHub@gmx.de>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteBuffer.h>
#include <AK/Noncopyable.h>
#include <AK/Try.h>
#include <AK/Variant.h>
#include <LibCore/File.h>
#include <LibIPC/Forward.h>
#include <LibIPC/Message.h>

namespace IPC {

class TrafficDump {
    AK_MAKE_NONCOPYABLE(TrafficDump);

public:
    static Optional<TrafficDump> create_if_requested(Stub const&);

    TrafficDump(TrafficDump&&) = default;
    TrafficDump& operator=(TrafficDump&&) = default;
    ~TrafficDump() = default;

    ErrorOr<void> notify_outgoing_message(MessageBuffer const&);
    // FIXME: Also log fds and their content maybe?
    ErrorOr<void> notify_incoming_message(ReadonlyBytes);

private:
    enum class Direction : u32 {
        P2P_DIR_SENT = 0,
        P2P_DIR_RECV = 1,
    };
    ErrorOr<void> lazy_init_if_necessary();
    ErrorOr<void> notify_message(AK::ReadonlyBytes, Direction);

    explicit TrafficDump(Stub const* stub)
        : m_file(stub)
    {
    }

    ErrorOr<void> write_u32(u32);

    Variant<Stub const*, NonnullOwnPtr<Core::File>> m_file;
};

}
