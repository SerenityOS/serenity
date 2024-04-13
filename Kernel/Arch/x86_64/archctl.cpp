/*
 * Copyright (c) 2024, Sönke Holz <sholz8530@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/API/archctl_numbers.h>
#include <Kernel/Tasks/Process.h>

namespace Kernel {

ErrorOr<FlatPtr> Process::sys$archctl(int option, FlatPtr arg1)
{
    VERIFY_NO_PROCESS_BIG_LOCK(this);
    switch (option) {
    case ARCHCTL_X86_64_SET_FS_BASE_FOR_CURRENT_THREAD: {
        Thread::current()->arch_specific_data().fs_base = arg1;
        Processor::set_fs_base(arg1);
        return 0;
    }

    default:
        return EINVAL;
    }
}

}
