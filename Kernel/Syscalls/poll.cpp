/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ScopeGuard.h>
#include <AK/Time.h>
#include <Kernel/API/POSIX/select.h>
#include <Kernel/Debug.h>
#include <Kernel/FileSystem/OpenFileDescription.h>
#include <Kernel/Tasks/Process.h>

namespace Kernel {

using BlockFlags = Thread::FileBlocker::BlockFlags;

ErrorOr<FlatPtr> Process::sys$poll(Userspace<Syscall::SC_poll_params const*> user_params)
{
    VERIFY_NO_PROCESS_BIG_LOCK(this);
    TRY(require_promise(Pledge::stdio));

    auto params = TRY(copy_typed_from_user(user_params));

    if (params.nfds >= OpenFileDescriptions::max_open())
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
    TRY(fds_info.try_ensure_capacity(params.nfds));

    TRY(m_fds.with_shared([&](auto& fds) -> ErrorOr<void> {
        for (size_t i = 0; i < params.nfds; i++) {
            auto& pfd = fds_copy[i];
            RefPtr<OpenFileDescription> description;
            auto description_or_error = fds.open_file_description(pfd.fd);
            if (!description_or_error.is_error())
                description = description_or_error.release_value();
            BlockFlags block_flags = BlockFlags::WriteError | BlockFlags::WriteHangUp; // always want POLLERR, POLLHUP, POLLNVAL
            if (pfd.events & POLLIN)
                block_flags |= BlockFlags::Read;
            if (pfd.events & POLLOUT)
                block_flags |= BlockFlags::Write;
            if (pfd.events & POLLPRI)
                block_flags |= BlockFlags::ReadPriority;
            if (pfd.events & POLLWRBAND)
                block_flags |= BlockFlags::WritePriority;
            if (pfd.events & POLLRDHUP)
                block_flags |= BlockFlags::ReadHangUp;
            fds_info.unchecked_append({ move(description), block_flags });
        }
        return {};
    }));

    auto* current_thread = Thread::current();

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

        if (has_flag(fds_entry.unblocked_flags, BlockFlags::WriteHangUp))
            pfd.revents |= POLLHUP;
        if (has_flag(fds_entry.unblocked_flags, BlockFlags::WriteError) || !fds_entry.description) {
            if (has_flag(fds_entry.unblocked_flags, BlockFlags::WriteError))
                pfd.revents |= POLLERR;
            if (!fds_entry.description)
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
            if (!has_flag(fds_entry.unblocked_flags, BlockFlags::WriteHangUp) && has_flag(fds_entry.unblocked_flags, BlockFlags::Write)) {
                VERIFY(pfd.events & POLLOUT);
                pfd.revents |= POLLOUT;
            }
            if (has_flag(fds_entry.unblocked_flags, BlockFlags::WritePriority)) {
                VERIFY(pfd.events & POLLWRBAND);
                pfd.revents |= POLLWRBAND;
            }
            if (has_flag(fds_entry.unblocked_flags, BlockFlags::ReadHangUp)) {
                VERIFY(pfd.events & POLLRDHUP);
                pfd.revents |= POLLRDHUP;
            }
        }
        if (pfd.revents)
            fds_with_revents++;
    }

    if (params.nfds > 0)
        TRY(copy_n_to_user(&params.fds[0], fds_copy.data(), params.nfds));

    return fds_with_revents;
}

}
