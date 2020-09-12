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

#include <AK/Time.h>
#include <Kernel/Process.h>

namespace Kernel {

static void compute_relative_timeout_from_absolute(const timeval& absolute_time, timeval& relative_time)
{
    // Convert absolute time to relative time of day.
    timeval_sub(absolute_time, kgettimeofday(), relative_time);
}

static void compute_relative_timeout_from_absolute(const timespec& absolute_time, timeval& relative_time)
{
    timeval tv_absolute_time;
    timespec_to_timeval(absolute_time, tv_absolute_time);
    compute_relative_timeout_from_absolute(tv_absolute_time, relative_time);
}

WaitQueue& Process::futex_queue(Userspace<const i32*> userspace_address)
{
    auto& queue = m_futex_queues.ensure(userspace_address.ptr());
    if (!queue)
        queue = make<WaitQueue>();
    return *queue;
}

int Process::sys$futex(Userspace<const Syscall::SC_futex_params*> user_params)
{
    REQUIRE_PROMISE(thread);

    Syscall::SC_futex_params params;
    if (!copy_from_user(&params, user_params))
        return -EFAULT;

    switch (params.futex_op) {
    case FUTEX_WAIT: {
        i32 user_value;
        if (!copy_from_user(&user_value, params.userspace_address))
            return -EFAULT;
        if (user_value != params.val)
            return -EAGAIN;

        timespec ts_abstimeout { 0, 0 };
        if (params.timeout && !copy_from_user(&ts_abstimeout, params.timeout))
            return -EFAULT;

        timeval* optional_timeout = nullptr;
        timeval relative_timeout { 0, 0 };
        if (params.timeout) {
            compute_relative_timeout_from_absolute(ts_abstimeout, relative_timeout);
            optional_timeout = &relative_timeout;
        }

        // FIXME: This is supposed to be interruptible by a signal, but right now WaitQueue cannot be interrupted.
        WaitQueue& wait_queue = futex_queue((FlatPtr)params.userspace_address);
        Thread::BlockResult result = Thread::current()->wait_on(wait_queue, "Futex", optional_timeout);
        if (result == Thread::BlockResult::InterruptedByTimeout) {
            return -ETIMEDOUT;
        }

        break;
    }
    case FUTEX_WAKE:
        if (params.val == 0)
            return 0;
        if (params.val == 1) {
            futex_queue((FlatPtr)params.userspace_address).wake_one();
        } else {
            futex_queue((FlatPtr)params.userspace_address).wake_n(params.val);
        }
        break;
    }

    return 0;
}

}
