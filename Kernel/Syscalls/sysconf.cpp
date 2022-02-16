/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/FileSystem/VirtualFileSystem.h>
#include <Kernel/Memory/MemoryManager.h>
#include <Kernel/Process.h>
#include <Kernel/Time/TimeManagement.h>

namespace Kernel {

ErrorOr<FlatPtr> Process::sys$sysconf(int name)
{
    VERIFY_NO_PROCESS_BIG_LOCK(this)
    switch (name) {
    case _SC_MONOTONIC_CLOCK:
        return 1;
    case _SC_NPROCESSORS_CONF:
    case _SC_NPROCESSORS_ONLN:
        return Processor::count();
    case _SC_OPEN_MAX:
        return OpenFileDescriptions::max_open();
    case _SC_PAGESIZE:
    case _SC_PAGE_SIZE:
        return PAGE_SIZE;
    case _SC_PHYS_PAGES: {
        auto memory_info = MM.get_system_memory_info();
        return memory_info.user_physical_pages + memory_info.super_physical_pages;
    }
    case _SC_AVPHYS_PAGES: {
        auto memory_info = MM.get_system_memory_info();
        return memory_info.user_physical_pages - memory_info.user_physical_pages_used;
    }
    case _SC_HOST_NAME_MAX:
        return HOST_NAME_MAX;
    case _SC_TTY_NAME_MAX:
        return TTY_NAME_MAX;
    case _SC_GETPW_R_SIZE_MAX:
        return 4096; // idk
    case _SC_CLK_TCK:
        return TimeManagement::the().ticks_per_second();
    case _SC_SYMLOOP_MAX:
        return Kernel::VirtualFileSystem::symlink_recursion_limit;
    case _SC_ARG_MAX:
        return Process::max_arguments_size;
    default:
        return EINVAL;
    }
}

}
