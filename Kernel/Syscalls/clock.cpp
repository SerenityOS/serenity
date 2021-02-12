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
#include <Kernel/Time/TimeManagement.h>

namespace Kernel {

int Process::sys$clock_gettime(clockid_t clock_id, Userspace<timespec*> user_ts)
{
    REQUIRE_PROMISE(stdio);

    auto ts = TimeManagement::the().current_time(clock_id);
    if (ts.is_error())
        return ts.error();

    if (!copy_to_user(user_ts, &ts.value()))
        return -EFAULT;
    return 0;
}

int Process::sys$clock_settime(clockid_t clock_id, Userspace<const timespec*> user_ts)
{
    REQUIRE_PROMISE(settime);

    if (!is_superuser())
        return -EPERM;

    timespec ts;
    if (!copy_from_user(&ts, user_ts))
        return -EFAULT;

    switch (clock_id) {
    case CLOCK_REALTIME:
        TimeManagement::the().set_epoch_time(ts);
        break;
    default:
        return -EINVAL;
    }
    return 0;
}

int Process::sys$clock_nanosleep(Userspace<const Syscall::SC_clock_nanosleep_params*> user_params)
{
    REQUIRE_PROMISE(stdio);

    Syscall::SC_clock_nanosleep_params params;
    if (!copy_from_user(&params, user_params))
        return -EFAULT;

    timespec requested_sleep;
    if (!copy_from_user(&requested_sleep, params.requested_sleep))
        return -EFAULT;

    bool is_absolute;
    switch (params.flags) {
    case 0:
        is_absolute = false;
        break;
    case TIMER_ABSTIME:
        is_absolute = true;
        break;
    default:
        return -EINVAL;
    }

    if (!TimeManagement::is_valid_clock_id(params.clock_id))
        return -EINVAL;

    bool was_interrupted;
    if (is_absolute) {
        was_interrupted = Thread::current()->sleep_until(params.clock_id, requested_sleep).was_interrupted();
    } else {
        timespec remaining_sleep;
        was_interrupted = Thread::current()->sleep(params.clock_id, requested_sleep, &remaining_sleep).was_interrupted();
        if (was_interrupted && params.remaining_sleep && !copy_to_user(params.remaining_sleep, &remaining_sleep))
            return -EFAULT;
    }
    if (was_interrupted)
        return -EINTR;
    return 0;
}

int Process::sys$adjtime(Userspace<const timeval*> user_delta, Userspace<timeval*> user_old_delta)
{
    if (user_old_delta) {
        timespec old_delta_ts = TimeManagement::the().remaining_epoch_time_adjustment();
        timeval old_delta;
        timespec_to_timeval(old_delta_ts, old_delta);
        if (!copy_to_user(user_old_delta, &old_delta))
            return -EFAULT;
    }

    if (user_delta) {
        REQUIRE_PROMISE(settime);
        if (!is_superuser())
            return -EPERM;
        timeval delta;
        if (!copy_from_user(&delta, user_delta))
            return -EFAULT;

        if (delta.tv_usec < 0 || delta.tv_usec >= 1'000'000)
            return -EINVAL;

        timespec delta_ts;
        timeval_to_timespec(delta, delta_ts);
        TimeManagement::the().set_remaining_epoch_time_adjustment(delta_ts);
    }

    return 0;
}

int Process::sys$gettimeofday(Userspace<timeval*> user_tv)
{
    REQUIRE_PROMISE(stdio);
    auto tv = kgettimeofday();
    if (!copy_to_user(user_tv, &tv))
        return -EFAULT;
    return 0;
}

}
