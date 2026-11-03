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
#include <LibSSH/BehaviorControl.h>
#include <LibSSH/SFTP/Server.h>

namespace SSH {

struct Session;

// 6.1.  Opening a Session
// https://datatracker.ietf.org/doc/html/rfc4254#section-6.1

struct PtyInfo {
    ByteString term_environment_variable {};

    u32 width_characters {};
    u32 height_rows {};
    u32 width_pixels {};
    u32 height_pixels {};

    ByteBuffer terminal_modes {};
};

struct ChildManagingSystem {
    NonnullOwnPtr<Core::File> stdin_;
    NonnullOwnPtr<Core::File> stdout_;

    pid_t child_pid {};
    Optional<int> exit_status {};

    Coroutine<ErrorOr<BehaviorControl>> handle_channel_data(Session&);
    void handle_channel_eof(Session const&) const;

private:
    void close_stdin_if_required(Session const&) const;
};

struct ExecData : ChildManagingSystem {
    static ErrorOr<ExecData> create(Core::Process&& process, int fd_stdin, int fd_stdout, int fd_stderr);

    bool is_ready_to_be_closed() const;

    Core::Process child;
    NonnullOwnPtr<Core::File> stderr_;
};

struct Shell : ChildManagingSystem {
    static ErrorOr<Shell> create(pid_t child, int fd_pty);

    bool is_ready_to_be_closed() const;

    bool received_io_error { false };
};

struct Session : public RefCounted<Session> {
    static ErrorOr<NonnullRefPtr<Session>> create(u32 sender_channel_id, u32 window_size, u32 maximum_packet_size);

    bool is_ready_to_be_closed() const;
    int exit_status() const;

    u32 local_channel_id {};
    u32 sender_channel_id {};
    u32 maximum_packet_size {};
    u64 initial_window_size {};
    u64 local_window_size {};
    u64 total_received_bytes {};

    StreamBuffer channel_data {};
    bool has_streaming_coroutine { false };

    Optional<PtyInfo> pty_info {};
    Vector<ByteString> env {};
    Variant<Empty, ExecData, SFTP::Server, Shell> system {};

    bool is_closed { false };
    bool has_received_eof { false };
};

}
