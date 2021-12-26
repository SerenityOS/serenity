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

#include <Kernel/API/Syscall.h>
#include <errno.h>
#include <stdio.h>
#include <sys/select.h>
#include <sys/time.h>

extern "C" {

int select(int nfds, fd_set* readfds, fd_set* writefds, fd_set* exceptfds, timeval* timeout_tv)
{
    timespec* timeout_ts = nullptr;
    timespec timeout;
    if (timeout_tv) {
        timeout_ts = &timeout;
        TIMEVAL_TO_TIMESPEC(timeout_tv, timeout_ts);
    }
    return pselect(nfds, readfds, writefds, exceptfds, timeout_ts, nullptr);
}

int pselect(int nfds, fd_set* readfds, fd_set* writefds, fd_set* exceptfds, const timespec* timeout, const sigset_t* sigmask)
{
    Syscall::SC_select_params params { nfds, readfds, writefds, exceptfds, timeout, sigmask };
    int rc = syscall(SC_select, &params);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}
}
