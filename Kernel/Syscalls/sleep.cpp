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
#include <stdint.h>

namespace Kernel {

int Process::sys$usleep(useconds_t usec)
{
    REQUIRE_PROMISE(stdio);
    if (!usec)
        return 0;
    u64 wakeup_time = Thread::current()->sleep(usec * TimeManagement::the().ticks_per_second() / 1000000);
    if (wakeup_time > g_uptime) {
        u64 ticks_left_until_original_wakeup_time = wakeup_time - g_uptime;
        u64 dt = ticks_left_until_original_wakeup_time / (TimeManagement::the().ticks_per_second() * 1000000);
        // TODO - can we make this syscall return 64 bits instead of 32 bits?
        return (dt < UINTMAX_MAX) ? dt : -1;
    }
    return 0;
}

int Process::sys$sleep(unsigned seconds)
{
    REQUIRE_PROMISE(stdio);
    if (!seconds)
        return 0;
    u64 wakeup_time = Thread::current()->sleep(seconds * TimeManagement::the().ticks_per_second());
    if (wakeup_time > g_uptime) {
        u32 ticks_left_until_original_wakeup_time = wakeup_time - g_uptime;
        return ticks_left_until_original_wakeup_time / TimeManagement::the().ticks_per_second();
    }
    return 0;
}

}
