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
#include <Kernel/FileSystem/FileDescription.h>
#include <Kernel/Process.h>

//#define DEBUG_IO
//#define DEBUG_POLL_SELECT

namespace Kernel {

int Process::sys$select(const Syscall::SC_select_params* user_params)
{
    REQUIRE_PROMISE(stdio);
    Syscall::SC_select_params params;

    SmapDisabler disabler;
    if (!copy_from_user(&params, user_params))
        return -EFAULT;

    if (params.nfds < 0)
        return -EINVAL;

    timespec computed_timeout;
    bool select_has_timeout = false;
    if (params.timeout) {
        timespec timeout_copy;
        if (!copy_from_user(&timeout_copy, params.timeout))
            return -EFAULT;
        if (timeout_copy.tv_sec || timeout_copy.tv_nsec) {
            timespec ts_since_boot;
            timeval_to_timespec(Scheduler::time_since_boot(), ts_since_boot);
            timespec_add(ts_since_boot, timeout_copy, computed_timeout);
            select_has_timeout = true;
        }
    }

    auto current_thread = Thread::current();

    u32 previous_signal_mask = 0;
    if (params.sigmask) {
        sigset_t sigmask_copy;
        if (!copy_from_user(&sigmask_copy, params.sigmask))
            return -EFAULT;
        previous_signal_mask = current_thread->update_signal_mask(sigmask_copy);
    }
    ScopeGuard rollback_signal_mask([&]() {
        if (params.sigmask)
            current_thread->update_signal_mask(previous_signal_mask);
    });

    Thread::SelectBlocker::FDVector rfds;
    Thread::SelectBlocker::FDVector wfds;
    Thread::SelectBlocker::FDVector efds;

    auto transfer_fds = [&](auto* fds_unsafe, auto& vector) -> int {
        vector.clear_with_capacity();
        if (!fds_unsafe)
            return 0;
        fd_set fds;
        if (!copy_from_user(&fds, fds_unsafe))
            return -EFAULT;
        for (int fd = 0; fd < params.nfds; ++fd) {
            if (FD_ISSET(fd, &fds)) {
                if (!file_description(fd)) {
                    dbg() << "sys$select: Bad fd number " << fd;
                    return -EBADF;
                }
                vector.append(fd);
            }
        }
        return 0;
    };
    if (int error = transfer_fds(params.writefds, wfds))
        return error;
    if (int error = transfer_fds(params.readfds, rfds))
        return error;
    if (int error = transfer_fds(params.exceptfds, efds))
        return error;

#if defined(DEBUG_IO) || defined(DEBUG_POLL_SELECT)
    dbg() << "selecting on (read:" << rfds.size() << ", write:" << wfds.size() << "), timeout=" << params.timeout;
#endif

    if (!params.timeout || select_has_timeout) {
        if (current_thread->block<Thread::SelectBlocker>(select_has_timeout ? &computed_timeout : nullptr, rfds, wfds, efds).was_interrupted())
            return -EINTR;
    }

    int marked_fd_count = 0;
    auto mark_fds = [&](auto* fds_unsafe, auto& vector, auto should_mark) {
        if (!fds_unsafe)
            return 0;
        fd_set fds;
        FD_ZERO(&fds);
        for (int fd : vector) {
            if (auto description = file_description(fd); description && should_mark(*description)) {
                FD_SET(fd, &fds);
                ++marked_fd_count;
            }
        }
        if (!copy_to_user(fds_unsafe, &fds))
            return -EFAULT;
        return 0;
    };
    if (int error = mark_fds(params.readfds, rfds, [](auto& description) { return description.can_read(); }))
        return error;
    if (int error = mark_fds(params.writefds, wfds, [](auto& description) { return description.can_write(); }))
        return error;
    // FIXME: We should also mark exceptfds as appropriate.

    return marked_fd_count;
}

int Process::sys$poll(Userspace<const Syscall::SC_poll_params*> user_params)
{
    REQUIRE_PROMISE(stdio);

    // FIXME: Return -EINVAL if timeout is invalid.
    Syscall::SC_poll_params params;
    if (!copy_from_user(&params, user_params))
        return -EFAULT;

    SmapDisabler disabler;

    timespec timeout = {};
    if (params.timeout && !copy_from_user(&timeout, params.timeout))
        return -EFAULT;

    sigset_t sigmask = {};
    if (params.sigmask && !copy_from_user(&sigmask, params.sigmask))
        return -EFAULT;

    Vector<pollfd> fds_copy;
    if (params.nfds > 0) {
        Checked nfds_checked = sizeof(pollfd);
        nfds_checked *= params.nfds;
        if (nfds_checked.has_overflow())
            return -EFAULT;
        fds_copy.resize(params.nfds);
        if (!copy_from_user(&fds_copy[0], &params.fds[0], params.nfds * sizeof(pollfd)))
            return -EFAULT;
    }

    Thread::SelectBlocker::FDVector rfds;
    Thread::SelectBlocker::FDVector wfds;

    for (unsigned i = 0; i < params.nfds; ++i) {
        auto& pfd = fds_copy[i];
        if (pfd.events & POLLIN)
            rfds.append(pfd.fd);
        if (pfd.events & POLLOUT)
            wfds.append(pfd.fd);
    }

    timespec actual_timeout;
    bool has_timeout = false;
    if (params.timeout && (timeout.tv_sec || timeout.tv_nsec)) {
        timespec ts_since_boot;
        timeval_to_timespec(Scheduler::time_since_boot(), ts_since_boot);
        timespec_add(ts_since_boot, timeout, actual_timeout);
        has_timeout = true;
    }

    auto current_thread = Thread::current();

    u32 previous_signal_mask = 0;
    if (params.sigmask)
        previous_signal_mask = current_thread->update_signal_mask(sigmask);
    ScopeGuard rollback_signal_mask([&]() {
        if (params.sigmask)
            current_thread->update_signal_mask(previous_signal_mask);
    });

#if defined(DEBUG_IO) || defined(DEBUG_POLL_SELECT)
    dbg() << "polling on (read:" << rfds.size() << ", write:" << wfds.size() << "), timeout=" << timeout.tv_sec << "s" << timeout.tv_nsec << "ns";
#endif

    if (!params.timeout || has_timeout) {
        if (current_thread->block<Thread::SelectBlocker>(has_timeout ? &actual_timeout : nullptr, rfds, wfds, Thread::SelectBlocker::FDVector()).was_interrupted())
            return -EINTR;
    }

    int fds_with_revents = 0;

    for (unsigned i = 0; i < params.nfds; ++i) {
        auto& pfd = fds_copy[i];
        auto description = file_description(pfd.fd);
        if (!description) {
            pfd.revents = POLLNVAL;
        } else {
            pfd.revents = 0;
            if (pfd.events & POLLIN && description->can_read())
                pfd.revents |= POLLIN;
            if (pfd.events & POLLOUT && description->can_write())
                pfd.revents |= POLLOUT;

            if (pfd.revents)
                ++fds_with_revents;
        }
    }

    if (params.nfds > 0 && !copy_to_user(&params.fds[0], &fds_copy[0], params.nfds * sizeof(pollfd)))
        return -EFAULT;

    return fds_with_revents;
}

}
