/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/x86/CPU.h>
#include <Kernel/Process.h>
#include <Kernel/Time/TimeManagement.h>
#include <limits.h>

namespace Kernel {

KResultOr<long> Process::sys$sysconf(int name)
{
    switch (name) {
    case _SC_MONOTONIC_CLOCK:
        return 1;
    case _SC_NPROCESSORS_CONF:
    case _SC_NPROCESSORS_ONLN:
        return Processor::processor_count();
    case _SC_OPEN_MAX:
        return max_open_file_descriptors();
    case _SC_PAGESIZE:
        return PAGE_SIZE;
    case _SC_TTY_NAME_MAX:
        return TTY_NAME_MAX;
    case _SC_GETPW_R_SIZE_MAX:
        return 4096; // idk
    case _SC_CLK_TCK:
        return TimeManagement::the().ticks_per_second();
    default:
        return EINVAL;
    }
}

}
