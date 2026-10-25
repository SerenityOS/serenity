/*
 * Copyright (c) 2026, Lucas Chollet <lucas.chollet@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Assertions.h>
#include <AK/ByteBuffer.h>
#include <AK/Error.h>
#include <AK/Function.h>
#include <AK/GenericAwaiter.h>
#include <AK/NonnullOwnPtr.h>
#include <AK/NonnullRefPtr.h>
#include <AK/NumericLimits.h>
#include <AK/StreamBuffer.h>
#include <AK/Variant.h>
#include <AK/Vector.h>
#include <LibCore/File.h>
#include <LibCore/Process.h>
#include <LibSSH/BehaviorControl.h>
#include <LibSSH/SFTP/Server.h>

namespace SSH {

struct Session;

// 6.1.  Opening a Session
// https://datatracker.ietf.org/doc/html/rfc4254#section-6.1

struct ExecData {
    static ExecData create(
        Core::Process&& process,
        NonnullOwnPtr<Core::File>&& stdin_,
        NonnullOwnPtr<Core::File>&& stdout_,
        NonnullOwnPtr<Core::File>&& stderr_);

    Core::Process child;

    NonnullOwnPtr<Core::File> stdin_;
    NonnullOwnPtr<Core::File> stdout_;
    NonnullOwnPtr<Core::File> stderr_;

    Optional<int> exit_status {};

    Coroutine<ErrorOr<BehaviorControl>> handle_channel_data(Session&);
    void handle_channel_eof(Session const&);
    void terminate();

    bool is_ready_to_be_closed() const;

private:
    void close_stdin_if_required(Session const&) const;
};

struct Session : public RefCounted<Session> {
    struct PendingOutgoingChannelData {
        ByteBuffer bytes;
        size_t offset {};
        bool is_extended_data {};
    };

    static ErrorOr<NonnullRefPtr<Session>> create(u32 sender_channel_id, u32 window_size, u32 maximum_packet_size);

    bool is_ready_to_be_closed() const;
    int exit_status() const;
    ErrorOr<void> adjust_remote_window(u32 bytes_to_add)
    {
        if (bytes_to_add > NumericLimits<u32>::max() - remote_window_size)
            return Error::from_string_literal("Peer channel window overflow");
        remote_window_size += bytes_to_add;
        return {};
    }

    void consume_remote_window(size_t byte_count)
    {
        VERIFY(byte_count <= remote_window_size);
        remote_window_size -= static_cast<u32>(byte_count);
    }

    bool has_pending_outgoing_channel_data() const { return !pending_outgoing_channel_data.is_empty(); }

    Coroutine<ErrorOr<void>> wait_for_remote_window()
    {
        while (remote_window_size == 0 && !is_closed) {
            CO_TRY(co_await GenericAwaiter([&](auto ready) {
                remote_window_waiters.append(move(ready));
            }));
        }
        if (is_closed)
            co_return Error::from_string_literal("Channel closed while waiting for peer window");
        co_return {};
    }

    void notify_remote_window_waiters()
    {
        auto waiters = move(remote_window_waiters);
        remote_window_waiters.clear();
        for (auto& waiter : waiters)
            waiter();
    }
    bool can_receive_data(size_t byte_count) const
    {
        return total_received_bytes <= local_window_size
            && byte_count <= local_window_size - total_received_bytes;
    }

    void dequeue_channel_data(size_t byte_count)
    {
        VERIFY(byte_count <= channel_data.data().size());
        channel_data.dequeue(byte_count);
        total_consumed_bytes += byte_count;
    }

    u32 local_channel_id {};
    u32 sender_channel_id {};
    u32 maximum_packet_size {};
    u64 initial_window_size {};
    u64 local_window_size {};
    u32 remote_window_size {};
    u64 total_received_bytes {};
    u64 total_consumed_bytes {};
    u64 total_window_adjusted_bytes {};

    StreamBuffer channel_data {};
    Vector<PendingOutgoingChannelData> pending_outgoing_channel_data {};
    size_t pending_outgoing_channel_data_bytes {};
    Vector<Function<void()>> remote_window_waiters {};
    bool has_streaming_coroutine { false };

    Variant<Empty, ExecData, SFTP::Server> system {};

    bool is_closed { false };
    bool has_received_eof { false };
};

}
