/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ScopeGuard.h>
#include <AK/Time.h>
#include <Kernel/Debug.h>
#include <Kernel/FileSystem/OpenFileDescription.h>
#include <Kernel/Process.h>

namespace Kernel {

using BlockFlags = Thread::FileBlocker::BlockFlags;

ErrorOr<FlatPtr> Process::sys$select(Userspace<const Syscall::SC_select_params*> user_params)
{
    VERIFY_PROCESS_BIG_LOCK_ACQUIRED(this)
    REQUIRE_PROMISE(stdio);

    auto params = TRY(copy_typed_from_user(user_params));

    if (params.nfds < 0)
        return EINVAL;

    Thread::BlockTimeout timeout;
    if (params.timeout) {
        auto timeout_time = TRY(copy_time_from_user(params.timeout));
        timeout = Thread::BlockTimeout(false, &timeout_time);
    }

    auto current_thread = Thread::current();

    u32 previous_signal_mask = 0;
    if (params.sigmask) {
        sigset_t sigmask_copy;
        TRY(copy_from_user(&sigmask_copy, params.sigmask));
        previous_signal_mask = current_thread->update_signal_mask(sigmask_copy);
    }
    ScopeGuard rollback_signal_mask([&]() {
        if (params.sigmask)
            current_thread->update_signal_mask(previous_signal_mask);
    });

    fd_set fds_read, fds_write, fds_except;

    size_t bytes_used = ceil_div(params.nfds, 8);
    if (bytes_used > sizeof(fds_read))
        return EINVAL;

    if (params.readfds)
        TRY(copy_from_user(&fds_read, params.readfds, bytes_used));

    if (params.writefds)
        TRY(copy_from_user(&fds_write, params.writefds, bytes_used));

    if (params.exceptfds)
        TRY(copy_from_user(&fds_except, params.exceptfds, bytes_used));

    Thread::SelectBlocker::FDVector fds_info;
    Vector<int, FD_SETSIZE> selected_fds;
    for (int fd = 0; fd < params.nfds; fd++) {
        auto block_flags = BlockFlags::None;
        if (params.readfds && FD_ISSET(fd, &fds_read))
            block_flags |= BlockFlags::Read;
        if (params.writefds && FD_ISSET(fd, &fds_write))
            block_flags |= BlockFlags::Write;
        if (params.exceptfds && FD_ISSET(fd, &fds_except))
            block_flags |= BlockFlags::Exception;
        if (block_flags == BlockFlags::None)
            continue;

        auto description = TRY(fds().open_file_description(fd));
        TRY(fds_info.try_append({ move(description), block_flags }));
        TRY(selected_fds.try_append(fd));
    }

    if constexpr (IO_DEBUG || POLL_SELECT_DEBUG)
        dbgln("selecting on {} fds, timeout={}", fds_info.size(), params.timeout);

    if (current_thread->block<Thread::SelectBlocker>(timeout, fds_info).was_interrupted()) {
        dbgln_if(POLL_SELECT_DEBUG, "select was interrupted");
        return EINTR;
    }

    if (params.readfds)
        FD_ZERO(&fds_read);
    if (params.writefds)
        FD_ZERO(&fds_write);
    if (params.exceptfds)
        FD_ZERO(&fds_except);

    int marked_fd_count = 0;
    for (size_t i = 0; i < fds_info.size(); i++) {
        auto& fd_entry = fds_info[i];
        if (fd_entry.unblocked_flags == BlockFlags::None)
            continue;
        if (params.readfds && has_flag(fd_entry.unblocked_flags, BlockFlags::Read)) {
            FD_SET(selected_fds[i], &fds_read);
            marked_fd_count++;
        }
        if (params.writefds && has_flag(fd_entry.unblocked_flags, BlockFlags::Write)) {
            FD_SET(selected_fds[i], &fds_write);
            marked_fd_count++;
        }
        if (params.exceptfds && has_any_flag(fd_entry.unblocked_flags, BlockFlags::Exception)) {
            FD_SET(selected_fds[i], &fds_except);
            marked_fd_count++;
        }
    }

    if (params.readfds)
        TRY(copy_to_user(params.readfds, &fds_read, bytes_used));
    if (params.writefds)
        TRY(copy_to_user(params.writefds, &fds_write, bytes_used));
    if (params.exceptfds)
        TRY(copy_to_user(params.exceptfds, &fds_except, bytes_used));
    return marked_fd_count;
}

}
