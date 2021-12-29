/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Process.h>

namespace Kernel {

ErrorOr<FlatPtr> Process::sys$times(Userspace<tms*> user_times)
{
    VERIFY_PROCESS_BIG_LOCK_ACQUIRED(this)
    TRY(require_promise(Pledge::stdio));
    tms times = {};
    times.tms_utime = m_ticks_in_user;
    times.tms_stime = m_ticks_in_kernel;
    times.tms_cutime = m_ticks_in_user_for_dead_children;
    times.tms_cstime = m_ticks_in_kernel_for_dead_children;

    TRY(copy_to_user(user_times, &times));
    return TimeManagement::the().uptime_ms() & 0x7fffffff;
}

}
