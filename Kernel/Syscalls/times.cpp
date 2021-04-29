/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Process.h>

namespace Kernel {

KResultOr<clock_t> Process::sys$times(Userspace<tms*> user_times)
{
    REQUIRE_PROMISE(stdio);
    tms times = {};
    times.tms_utime = m_ticks_in_user;
    times.tms_stime = m_ticks_in_kernel;
    times.tms_cutime = m_ticks_in_user_for_dead_children;
    times.tms_cstime = m_ticks_in_kernel_for_dead_children;

    if (!copy_to_user(user_times, &times))
        return EFAULT;

    return TimeManagement::the().uptime_ms() & 0x7fffffff;
}

}
