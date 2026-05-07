/*
 * Copyright (c) 2026, Lucas Chollet <lucas.chollet@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteBuffer.h>
#include <AK/Error.h>
#include <AK/NonnullOwnPtr.h>
#include <AK/NonnullRefPtr.h>
#include <AK/StreamBuffer.h>
#include <AK/Variant.h>
#include <LibCore/File.h>
#include <LibCore/Process.h>

namespace SSH {

struct Session;

// 6.1.  Opening a Session
// https://datatracker.ietf.org/doc/html/rfc4254#section-6.1

struct ExecData {
    static ErrorOr<ExecData> create(Core::Process&& process, int fd_stdin, int fd_stdout, int fd_stderr);

    Core::Process child;

    NonnullOwnPtr<Core::File> stdin_;
    NonnullOwnPtr<Core::File> stdout_;
    NonnullOwnPtr<Core::File> stderr_;

    Coroutine<ErrorOr<void>> handle_channel_data(Session&);
    void handle_channel_eof(Session const&);

private:
    void close_stdin_if_required(Session const&) const;
};

struct Session : public RefCounted<Session> {
    static ErrorOr<NonnullRefPtr<Session>> create(u32 sender_channel_id, u32 window_size, u32 maximum_packet_size);

    u32 local_channel_id {};
    u32 sender_channel_id {};
    u32 maximum_packet_size {};
    u64 window_size {};

    StreamBuffer channel_data {};
    bool has_streaming_coroutine { false };

    Variant<Empty, ExecData> system {};

    bool is_closed { false };
    bool has_received_eof { false };
};

}
