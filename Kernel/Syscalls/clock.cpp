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

#include <Kernel/Process.h>
#include <Kernel/Time/TimeManagement.h>

namespace Kernel {

int Process::sys$clock_gettime(clockid_t clock_id, Userspace<timespec*> user_ts)
{
    REQUIRE_PROMISE(stdio);

    timespec ts = {};

    switch (clock_id) {
    case CLOCK_MONOTONIC:
        ts.tv_sec = TimeManagement::the().seconds_since_boot();
        ts.tv_nsec = TimeManagement::the().ticks_this_second() * 1000000;
        break;
    case CLOCK_REALTIME:
        ts = TimeManagement::the().epoch_time();
        break;
    default:
        return -EINVAL;
    }

    if (!copy_to_user(user_ts, &ts))
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

    bool is_absolute = params.flags & TIMER_ABSTIME;

    switch (params.clock_id) {
    case CLOCK_MONOTONIC: {
        u64 wakeup_time;
        if (is_absolute) {
            u64 time_to_wake = (requested_sleep.tv_sec * 1000 + requested_sleep.tv_nsec / 1000000);
            wakeup_time = Thread::current()->sleep_until(time_to_wake);
        } else {
            u64 ticks_to_sleep = requested_sleep.tv_sec * TimeManagement::the().ticks_per_second();
            ticks_to_sleep += requested_sleep.tv_nsec * TimeManagement::the().ticks_per_second() / 1000000000;
            if (!ticks_to_sleep)
                return 0;
            wakeup_time = Thread::current()->sleep(ticks_to_sleep);
        }
        if (wakeup_time > g_uptime) {
            u64 ticks_left = wakeup_time - g_uptime;
            if (!is_absolute && params.remaining_sleep) {
                timespec remaining_sleep = {};
                remaining_sleep.tv_sec = ticks_left / TimeManagement::the().ticks_per_second();
                ticks_left -= remaining_sleep.tv_sec * TimeManagement::the().ticks_per_second();
                remaining_sleep.tv_nsec = ticks_left * 1000000000 / TimeManagement::the().ticks_per_second();
                if (!copy_to_user(params.remaining_sleep, &remaining_sleep))
                    return -EFAULT;
            }
            return -EINTR;
        }
        return 0;
    }
    default:
        return -EINVAL;
    }
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
