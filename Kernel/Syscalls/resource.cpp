/*
 * Copyright (c) 2022, Lucas Chollet <lucas.chollet@free.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Process.h>

namespace Kernel {

ErrorOr<FlatPtr> Process::sys$getrusage(int who, Userspace<rusage*> user_usage)
{
    VERIFY_PROCESS_BIG_LOCK_ACQUIRED(this)

    rusage usage {};

    auto const ticks_per_second = TimeManagement::the().ticks_per_second();

    switch (who) {
    case RUSAGE_SELF:
        usage.ru_utime = Time::from_ticks(m_ticks_in_user, ticks_per_second).to_timeval();
        usage.ru_stime = Time::from_ticks(m_ticks_in_kernel, ticks_per_second).to_timeval();
        break;
    case RUSAGE_CHILDREN:
        usage.ru_utime = Time::from_ticks(m_ticks_in_user_for_dead_children, ticks_per_second).to_timeval();
        usage.ru_stime = Time::from_ticks(m_ticks_in_kernel_for_dead_children, ticks_per_second).to_timeval();
        break;
    default:
        return EINVAL;
    }

    TRY(copy_to_user(user_usage, &usage));

    return 0;
}

}
