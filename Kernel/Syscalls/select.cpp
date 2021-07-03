/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ScopeGuard.h>
#include <AK/Time.h>
#include <Kernel/Debug.h>
#include <Kernel/FileSystem/FileDescription.h>
#include <Kernel/Process.h>

namespace Kernel {

using BlockFlags = Thread::FileBlocker::BlockFlags;

KResultOr<int> Process::sys$select(Userspace<const Syscall::SC_select_params*> user_params)
{
    REQUIRE_PROMISE(stdio);
    Syscall::SC_select_params params {};

    if (!copy_from_user(&params, user_params))
        return EFAULT;

    if (params.nfds < 0)
        return EINVAL;

    Thread::BlockTimeout timeout;
    if (params.timeout) {
        Optional<Time> timeout_time = copy_time_from_user(params.timeout);
        if (!timeout_time.has_value())
            return EFAULT;
        timeout = Thread::BlockTimeout(false, &timeout_time.value());
    }

    auto current_thread = Thread::current();

    u32 previous_signal_mask = 0;
    if (params.sigmask) {
        sigset_t sigmask_copy;
        if (!copy_from_user(&sigmask_copy, params.sigmask))
            return EFAULT;
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

    if (params.readfds && !copy_from_user(&fds_read, params.readfds, bytes_used))
        return EFAULT;
    if (params.writefds && !copy_from_user(&fds_write, params.writefds, bytes_used))
        return EFAULT;
    if (params.exceptfds && !copy_from_user(&fds_except, params.exceptfds, bytes_used))
        return EFAULT;

    Thread::SelectBlocker::FDVector fds_info;
    Vector<int, FD_SETSIZE> fds;
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

        auto description = file_description(fd);
        if (!description) {
            dbgln("sys$select: Bad fd number {}", fd);
            return EBADF;
        }
        if (!fds_info.try_append({ description.release_nonnull(), block_flags }))
            return ENOMEM;
        if (!fds.try_append(fd))
            return ENOMEM;
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
            FD_SET(fds[i], &fds_read);
            marked_fd_count++;
        }
        if (params.writefds && has_flag(fd_entry.unblocked_flags, BlockFlags::Write)) {
            FD_SET(fds[i], &fds_write);
            marked_fd_count++;
        }
        if (params.exceptfds && has_flag(fd_entry.unblocked_flags, BlockFlags::Exception)) {
            FD_SET(fds[i], &fds_except);
            marked_fd_count++;
        }
    }

    if (params.readfds && !copy_to_user(params.readfds, &fds_read, bytes_used))
        return EFAULT;
    if (params.writefds && !copy_to_user(params.writefds, &fds_write, bytes_used))
        return EFAULT;
    if (params.exceptfds && !copy_to_user(params.exceptfds, &fds_except, bytes_used))
        return EFAULT;
    return marked_fd_count;
}

KResultOr<int> Process::sys$poll(Userspace<const Syscall::SC_poll_params*> user_params)
{
    REQUIRE_PROMISE(stdio);

    Syscall::SC_poll_params params;
    if (!copy_from_user(&params, user_params))
        return EFAULT;

    if (params.nfds >= m_max_open_file_descriptors)
        return ENOBUFS;

    Thread::BlockTimeout timeout;
    if (params.timeout) {
        auto timeout_time = copy_time_from_user(params.timeout);
        if (!timeout_time.has_value())
            return EFAULT;
        timeout = Thread::BlockTimeout(false, &timeout_time.value());
    }

    sigset_t sigmask = {};
    if (params.sigmask && !copy_from_user(&sigmask, params.sigmask))
        return EFAULT;

    Vector<pollfd, FD_SETSIZE> fds_copy;
    if (params.nfds > 0) {
        Checked nfds_checked = sizeof(pollfd);
        nfds_checked *= params.nfds;
        if (nfds_checked.has_overflow())
            return EFAULT;
        if (!fds_copy.try_resize(params.nfds))
            return ENOMEM;
        if (!copy_from_user(fds_copy.data(), &params.fds[0], nfds_checked.value()))
            return EFAULT;
    }

    Thread::SelectBlocker::FDVector fds_info;
    for (size_t i = 0; i < params.nfds; i++) {
        auto& pfd = fds_copy[i];
        auto description = file_description(pfd.fd);
        if (!description) {
            dbgln("sys$poll: Bad fd number {}", pfd.fd);
            return EBADF;
        }
        BlockFlags block_flags = BlockFlags::Exception; // always want POLLERR, POLLHUP, POLLNVAL
        if (pfd.events & POLLIN)
            block_flags |= BlockFlags::Read;
        if (pfd.events & POLLOUT)
            block_flags |= BlockFlags::Write;
        if (pfd.events & POLLPRI)
            block_flags |= BlockFlags::ReadPriority;
        if (!fds_info.try_append({ description.release_nonnull(), block_flags }))
            return ENOMEM;
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

        if (has_flag(fds_entry.unblocked_flags, BlockFlags::Exception)) {
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
        }
        if (pfd.revents)
            fds_with_revents++;
    }

    if (params.nfds > 0 && !copy_to_user(&params.fds[0], fds_copy.data(), params.nfds * sizeof(pollfd)))
        return EFAULT;

    return fds_with_revents;
}

}
