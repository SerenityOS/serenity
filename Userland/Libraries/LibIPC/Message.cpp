/*
 * Copyright (c) 2024, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Checked.h>
#include <LibCore/Socket.h>
#include <LibIPC/Message.h>
#include <sched.h>

namespace IPC {

using MessageSizeType = u32;

MessageBuffer::MessageBuffer()
{
    m_data.resize(sizeof(MessageSizeType));
}

ErrorOr<void> MessageBuffer::extend_data_capacity(size_t capacity)
{
    TRY(m_data.try_ensure_capacity(m_data.size() + capacity));
    return {};
}

ErrorOr<void> MessageBuffer::append_data(u8 const* values, size_t count)
{
    TRY(m_data.try_append(values, count));
    return {};
}

ErrorOr<void> MessageBuffer::append_file_descriptor(int fd)
{
    auto auto_fd = TRY(adopt_nonnull_ref_or_enomem(new (nothrow) AutoCloseFileDescriptor(fd)));
    TRY(m_fds.try_append(move(auto_fd)));
    return {};
}

ErrorOr<void> MessageBuffer::transfer_message(Core::LocalSocket& fd_passing_socket, Core::LocalSocket& data_socket)
{
    Checked<MessageSizeType> checked_message_size { m_data.size() };
    checked_message_size -= sizeof(MessageSizeType);

    if (checked_message_size.has_overflow())
        return Error::from_string_literal("Message is too large for IPC encoding");

    auto message_size = checked_message_size.value();
    m_data.span().overwrite(0, reinterpret_cast<u8 const*>(&message_size), sizeof(message_size));

    for (auto const& fd : m_fds)
        TRY(fd_passing_socket.send_fd(fd->value()));

    ReadonlyBytes bytes_to_write { m_data.span() };
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
        dbgln("LibIPC::transfer_message FIXME Warning, needed {} writes needed to send message of size {}B, this is pretty bad, as it spins on the EventLoop", writes_done, m_data.size());
    }

    return {};
}

}
