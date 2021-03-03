/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <AK/ScopeGuard.h>
#include <AK/Time.h>
#include <Kernel/Debug.h>
#include <Kernel/FileSystem/FileDescription.h>
#include <Kernel/Process.h>

namespace Kernel {

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
    if (params.readfds && !copy_from_user(&fds_read, params.readfds))
        return EFAULT;
    if (params.writefds && !copy_from_user(&fds_write, params.writefds))
        return EFAULT;
    if (params.exceptfds && !copy_from_user(&fds_except, params.exceptfds))
        return EFAULT;

    Thread::SelectBlocker::FDVector fds_info;
    Vector<int, FD_SETSIZE> fds;
    for (int fd = 0; fd < params.nfds; fd++) {
        u32 block_flags = (u32)Thread::FileBlocker::BlockFlags::None;
        if (params.readfds && FD_ISSET(fd, &fds_read))
            block_flags |= (u32)Thread::FileBlocker::BlockFlags::Read;
        if (params.writefds && FD_ISSET(fd, &fds_write))
            block_flags |= (u32)Thread::FileBlocker::BlockFlags::Write;
        if (params.exceptfds && FD_ISSET(fd, &fds_except))
            block_flags |= (u32)Thread::FileBlocker::BlockFlags::Exception;
        if (block_flags == (u32)Thread::FileBlocker::BlockFlags::None)
            continue;

        auto description = file_description(fd);
        if (!description) {
            dbgln("sys$select: Bad fd number {}", fd);
            return EBADF;
        }
        fds_info.append({ description.release_nonnull(), (Thread::FileBlocker::BlockFlags)block_flags });
        fds.append(fd);
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
        if (fd_entry.unblocked_flags == Thread::FileBlocker::BlockFlags::None)
            continue;
        if (params.readfds && ((u32)fd_entry.unblocked_flags & (u32)Thread::FileBlocker::BlockFlags::Read)) {
            FD_SET(fds[i], &fds_read);
            marked_fd_count++;
        }
        if (params.writefds && ((u32)fd_entry.unblocked_flags & (u32)Thread::FileBlocker::BlockFlags::Write)) {
            FD_SET(fds[i], &fds_write);
            marked_fd_count++;
        }
        if (params.exceptfds && ((u32)fd_entry.unblocked_flags & (u32)Thread::FileBlocker::BlockFlags::Exception)) {
            FD_SET(fds[i], &fds_except);
            marked_fd_count++;
        }
    }

    if (params.readfds && !copy_to_user(params.readfds, &fds_read))
        return EFAULT;
    if (params.writefds && !copy_to_user(params.writefds, &fds_write))
        return EFAULT;
    if (params.exceptfds && !copy_to_user(params.exceptfds, &fds_except))
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

    Vector<pollfd> fds_copy;
    if (params.nfds > 0) {
        Checked nfds_checked = sizeof(pollfd);
        nfds_checked *= params.nfds;
        if (nfds_checked.has_overflow())
            return EFAULT;
        fds_copy.resize(params.nfds);
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
        u32 block_flags = (u32)Thread::FileBlocker::BlockFlags::Exception; // always want POLLERR, POLLHUP, POLLNVAL
        if (pfd.events & POLLIN)
            block_flags |= (u32)Thread::FileBlocker::BlockFlags::Read;
        if (pfd.events & POLLOUT)
            block_flags |= (u32)Thread::FileBlocker::BlockFlags::Write;
        if (pfd.events & POLLPRI)
            block_flags |= (u32)Thread::FileBlocker::BlockFlags::ReadPriority;
        fds_info.append({ description.release_nonnull(), (Thread::FileBlocker::BlockFlags)block_flags });
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
        if (fds_entry.unblocked_flags == Thread::FileBlocker::BlockFlags::None)
            continue;

        if ((u32)fds_entry.unblocked_flags & (u32)Thread::FileBlocker::BlockFlags::Exception) {
            if ((u32)fds_entry.unblocked_flags & (u32)Thread::FileBlocker::BlockFlags::ReadHangUp)
                pfd.revents |= POLLRDHUP;
            if ((u32)fds_entry.unblocked_flags & (u32)Thread::FileBlocker::BlockFlags::WriteError)
                pfd.revents |= POLLERR;
            if ((u32)fds_entry.unblocked_flags & (u32)Thread::FileBlocker::BlockFlags::WriteHangUp)
                pfd.revents |= POLLNVAL;
        } else {
            if ((u32)fds_entry.unblocked_flags & (u32)Thread::FileBlocker::BlockFlags::Read) {
                VERIFY(pfd.events & POLLIN);
                pfd.revents |= POLLIN;
            }
            if ((u32)fds_entry.unblocked_flags & (u32)Thread::FileBlocker::BlockFlags::ReadPriority) {
                VERIFY(pfd.events & POLLPRI);
                pfd.revents |= POLLPRI;
            }
            if ((u32)fds_entry.unblocked_flags & (u32)Thread::FileBlocker::BlockFlags::Write) {
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
