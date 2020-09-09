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

int Process::sys$select(const Syscall::SC_select_params* params)
{
    REQUIRE_PROMISE(stdio);
    // FIXME: Return -EINVAL if timeout is invalid.
    if (!validate_read_typed(params))
        return -EFAULT;

    SmapDisabler disabler;

    int nfds = params->nfds;
    fd_set* readfds = params->readfds;
    fd_set* writefds = params->writefds;
    fd_set* exceptfds = params->exceptfds;
    const timespec* timeout = params->timeout;
    const sigset_t* sigmask = params->sigmask;

    if (writefds && !validate_write_typed(writefds))
        return -EFAULT;
    if (readfds && !validate_write_typed(readfds))
        return -EFAULT;
    if (exceptfds && !validate_write_typed(exceptfds))
        return -EFAULT;
    if (timeout && !validate_read_typed(timeout))
        return -EFAULT;
    if (sigmask && !validate_read_typed(sigmask))
        return -EFAULT;
    if (nfds < 0)
        return -EINVAL;

    timespec computed_timeout;
    bool select_has_timeout = false;
    if (timeout && (timeout->tv_sec || timeout->tv_nsec)) {
        timespec ts_since_boot;
        timeval_to_timespec(Scheduler::time_since_boot(), ts_since_boot);
        timespec_add(ts_since_boot, *timeout, computed_timeout);
        select_has_timeout = true;
    }

    auto current_thread = Thread::current();

    u32 previous_signal_mask = 0;
    if (sigmask)
        previous_signal_mask = current_thread->update_signal_mask(*sigmask);
    ScopeGuard rollback_signal_mask([&]() {
        if (sigmask)
            current_thread->update_signal_mask(previous_signal_mask);
    });

    Thread::SelectBlocker::FDVector rfds;
    Thread::SelectBlocker::FDVector wfds;
    Thread::SelectBlocker::FDVector efds;

    auto transfer_fds = [&](auto* fds, auto& vector) -> int {
        vector.clear_with_capacity();
        if (!fds)
            return 0;
        for (int fd = 0; fd < nfds; ++fd) {
            if (FD_ISSET(fd, fds)) {
                if (!file_description(fd)) {
                    dbg() << "sys$select: Bad fd number " << fd;
                    return -EBADF;
                }
                vector.append(fd);
            }
        }
        return 0;
    };
    if (int error = transfer_fds(writefds, wfds))
        return error;
    if (int error = transfer_fds(readfds, rfds))
        return error;
    if (int error = transfer_fds(exceptfds, efds))
        return error;

#if defined(DEBUG_IO) || defined(DEBUG_POLL_SELECT)
    dbg() << "selecting on (read:" << rfds.size() << ", write:" << wfds.size() << "), timeout=" << timeout;
#endif

    if (!timeout || select_has_timeout) {
        if (current_thread->block<Thread::SelectBlocker>(select_has_timeout ? &computed_timeout : nullptr, rfds, wfds, efds).was_interrupted())
            return -EINTR;
        // While we blocked, the process lock was dropped. This gave other threads
        // the opportunity to mess with the memory. For example, it could free the
        // region, and map it to a region to which it has no write permissions.
        // Therefore, we need to re-validate all pointers.
        if (writefds && !validate_write_typed(writefds))
            return -EFAULT;
        if (readfds && !validate_write_typed(readfds))
            return -EFAULT;
        // See the fixme below.
        if (exceptfds && !validate_write_typed(exceptfds))
            return -EFAULT;
    }

    int marked_fd_count = 0;
    auto mark_fds = [&](auto* fds, auto& vector, auto should_mark) {
        if (!fds)
            return;
        FD_ZERO(fds);
        for (int fd : vector) {
            if (auto description = file_description(fd); description && should_mark(*description)) {
                FD_SET(fd, fds);
                ++marked_fd_count;
            }
        }
    };
    mark_fds(readfds, rfds, [](auto& description) { return description.can_read(); });
    mark_fds(writefds, wfds, [](auto& description) { return description.can_write(); });
    // FIXME: We should also mark exceptfds as appropriate.

    return marked_fd_count;
}

int Process::sys$poll(Userspace<const Syscall::SC_poll_params*> user_params)
{
    REQUIRE_PROMISE(stdio);

    // FIXME: Return -EINVAL if timeout is invalid.
    Syscall::SC_poll_params params;
    if (!validate_read_and_copy_typed(&params, user_params))
        return -EFAULT;

    SmapDisabler disabler;

    pollfd* fds = params.fds;
    unsigned nfds = params.nfds;

    if (fds && !validate_read_typed(fds, nfds))
        return -EFAULT;

    timespec timeout = {};
    if (params.timeout && !validate_read_and_copy_typed(&timeout, params.timeout))
        return -EFAULT;

    sigset_t sigmask = {};
    if (params.sigmask && !validate_read_and_copy_typed(&sigmask, params.sigmask))
        return -EFAULT;

    Thread::SelectBlocker::FDVector rfds;
    Thread::SelectBlocker::FDVector wfds;

    for (unsigned i = 0; i < nfds; ++i) {
        if (fds[i].events & POLLIN)
            rfds.append(fds[i].fd);
        if (fds[i].events & POLLOUT)
            wfds.append(fds[i].fd);
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
        previous_signal_mask = current_thread->update_signal_mask(params.sigmask);
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

    // Validate we can still write after waking up.
    if (fds && !validate_write_typed(fds, nfds))
        return -EFAULT;

    int fds_with_revents = 0;

    for (unsigned i = 0; i < nfds; ++i) {
        auto description = file_description(fds[i].fd);
        if (!description) {
            fds[i].revents = POLLNVAL;
            continue;
        }
        fds[i].revents = 0;
        if (fds[i].events & POLLIN && description->can_read())
            fds[i].revents |= POLLIN;
        if (fds[i].events & POLLOUT && description->can_write())
            fds[i].revents |= POLLOUT;

        if (fds[i].revents)
            ++fds_with_revents;
    }

    return fds_with_revents;
}

}
