/*
 * Copyright (c) 2024, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/Socket.h>
#include <LibIPC/Message.h>
#include <sched.h>

namespace IPC {

using MessageSizeType = u32;

ErrorOr<void> MessageBuffer::transfer_message(Core::LocalSocket& fd_passing_socket, Core::LocalSocket& data_socket)
{
    MessageSizeType message_size = data.size();
    TRY(data.try_prepend(reinterpret_cast<u8 const*>(&message_size), sizeof(message_size)));

    for (auto const& fd : fds)
        TRY(fd_passing_socket.send_fd(fd->value()));

    ReadonlyBytes bytes_to_write { data.span() };
    size_t writes_done = 0;

    while (!bytes_to_write.is_empty()) {
        auto maybe_nwritten = data_socket.write_some(bytes_to_write);
        ++writes_done;

        if (maybe_nwritten.is_error()) {
            if (auto error = maybe_nwritten.release_error(); error.is_errno()) {
                // FIXME: This is a hacky way to at least not crash on large messages
                // The limit of 100 writes is arbitrary, and there to prevent indefinite spinning on the EventLoop
                if (error.code() == EAGAIN && writes_done < 100) {
                    sched_yield();
                    continue;
                }

                switch (error.code()) {
                case EPIPE:
                    return Error::from_string_literal("IPC::transfer_message: Disconnected from peer");
                case EAGAIN:
                    return Error::from_string_literal("IPC::transfer_message: Peer buffer overflowed");
                default:
                    return Error::from_syscall("IPC::transfer_message write"sv, -error.code());
                }
            } else {
                return error;
            }
        }

        bytes_to_write = bytes_to_write.slice(maybe_nwritten.value());
    }

    if (writes_done > 1) {
        dbgln("LibIPC::transfer_message FIXME Warning, needed {} writes needed to send message of size {}B, this is pretty bad, as it spins on the EventLoop", writes_done, data.size());
    }

    return {};
}

}
