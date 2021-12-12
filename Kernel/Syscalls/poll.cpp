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

ErrorOr<FlatPtr> Process::sys$poll(Userspace<const Syscall::SC_poll_params*> user_params)
{
    VERIFY_PROCESS_BIG_LOCK_ACQUIRED(this)
    REQUIRE_PROMISE(stdio);
    auto params = TRY(copy_typed_from_user(user_params));

    if (params.nfds >= fds().max_open())
        return ENOBUFS;

    Thread::BlockTimeout timeout;
    if (params.timeout) {
        auto timeout_time = TRY(copy_time_from_user(params.timeout));
        timeout = Thread::BlockTimeout(false, &timeout_time);
    }

    sigset_t sigmask = {};
    if (params.sigmask)
        TRY(copy_from_user(&sigmask, params.sigmask));

    Vector<pollfd, FD_SETSIZE> fds_copy;
    if (params.nfds > 0) {
        Checked<size_t> nfds_checked = sizeof(pollfd);
        nfds_checked *= params.nfds;
        if (nfds_checked.has_overflow())
            return EFAULT;
        TRY(fds_copy.try_resize(params.nfds));
        TRY(copy_from_user(fds_copy.data(), &params.fds[0], nfds_checked.value()));
    }

    Thread::SelectBlocker::FDVector fds_info;
    for (size_t i = 0; i < params.nfds; i++) {
        auto& pfd = fds_copy[i];
        auto description = TRY(fds().open_file_description(pfd.fd));
        BlockFlags block_flags = BlockFlags::Exception; // always want POLLERR, POLLHUP, POLLNVAL
        if (pfd.events & POLLIN)
            block_flags |= BlockFlags::Read;
        if (pfd.events & POLLOUT)
            block_flags |= BlockFlags::Write;
        if (pfd.events & POLLPRI)
            block_flags |= BlockFlags::ReadPriority;
        if (pfd.events & POLLWRBAND)
            block_flags |= BlockFlags::WritePriority;
        TRY(fds_info.try_append({ move(description), block_flags }));
    }

    auto current_thread = Thread::current();

    u32 previous_signal_mask = 0;
    if (params.sigmask)
        previous_signal_mask = current_thread->update_signal_mask(sigmask);
    ScopeGuard rollback_signal_mask([&]() {
        if (params.sigmask)
            current_thread->update_signal_mask(previous_signal_mask);
    });

    if constexpr (IO_DEBUG || POLL_SELECT_DEBUG)
        dbgln("polling on {} fds, timeout={}", fds_info.size(), params.timeout);

    if (current_thread->block<Thread::SelectBlocker>(timeout, fds_info).was_interrupted())
        return EINTR;

    int fds_with_revents = 0;

    for (unsigned i = 0; i < params.nfds; ++i) {
        auto& pfd = fds_copy[i];
        auto& fds_entry = fds_info[i];

        pfd.revents = 0;
        if (fds_entry.unblocked_flags == BlockFlags::None)
            continue;

        if (has_any_flag(fds_entry.unblocked_flags, BlockFlags::Exception)) {
            if (has_flag(fds_entry.unblocked_flags, BlockFlags::ReadHangUp))
                pfd.revents |= POLLRDHUP;
            if (has_flag(fds_entry.unblocked_flags, BlockFlags::WriteError))
                pfd.revents |= POLLERR;
            if (has_flag(fds_entry.unblocked_flags, BlockFlags::WriteHangUp))
                pfd.revents |= POLLNVAL;
        } else {
            if (has_flag(fds_entry.unblocked_flags, BlockFlags::Read)) {
                VERIFY(pfd.events & POLLIN);
                pfd.revents |= POLLIN;
            }
            if (has_flag(fds_entry.unblocked_flags, BlockFlags::ReadPriority)) {
                VERIFY(pfd.events & POLLPRI);
                pfd.revents |= POLLPRI;
            }
            if (has_flag(fds_entry.unblocked_flags, BlockFlags::Write)) {
                VERIFY(pfd.events & POLLOUT);
                pfd.revents |= POLLOUT;
            }
            if (has_flag(fds_entry.unblocked_flags, BlockFlags::WritePriority)) {
                VERIFY(pfd.events & POLLWRBAND);
                pfd.revents |= POLLWRBAND;
            }
        }
        if (pfd.revents)
            fds_with_revents++;
    }

    if (params.nfds > 0)
        TRY(copy_to_user(&params.fds[0], fds_copy.data(), params.nfds * sizeof(pollfd)));

    return fds_with_revents;
}

}
