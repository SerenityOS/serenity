/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/FileSystem/VirtualFileSystem.h>
#include <Kernel/Memory/MemoryManager.h>
#include <Kernel/Tasks/Process.h>
#include <Kernel/Time/TimeManagement.h>

namespace Kernel {

ErrorOr<FlatPtr> Process::sys$sysconf(int name)
{
    VERIFY_NO_PROCESS_BIG_LOCK(this);
    switch (name) {
    case _SC_MONOTONIC_CLOCK:
        return 1;
    case _SC_NPROCESSORS_CONF:
    case _SC_NPROCESSORS_ONLN:
        return Processor::count();
    case _SC_OPEN_MAX:
        return OpenFileDescriptions::max_open();
    case _SC_PAGESIZE:
        return PAGE_SIZE;
    case _SC_HOST_NAME_MAX:
        return HOST_NAME_MAX;
    case _SC_TTY_NAME_MAX:
        return TTY_NAME_MAX;
    case _SC_GETPW_R_SIZE_MAX:
    case _SC_GETGR_R_SIZE_MAX:
        return 4096; // idk
    case _SC_CLK_TCK:
        return TimeManagement::the().ticks_per_second();
    case _SC_SYMLOOP_MAX:
        return Kernel::VirtualFileSystem::symlink_recursion_limit;
    case _SC_ARG_MAX:
        return Process::max_arguments_size;
    case _SC_IOV_MAX:
        return IOV_MAX;
    case _SC_PHYS_PAGES:
        return MM.get_system_memory_info().physical_pages;
    default:
        return EINVAL;
    }
}

}
