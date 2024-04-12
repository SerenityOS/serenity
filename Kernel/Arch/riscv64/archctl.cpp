/*
 * Copyright (c) 2024, Sönke Holz <sholz8530@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Tasks/Process.h>

namespace Kernel {

ErrorOr<FlatPtr> Process::sys$archctl(int option, FlatPtr arg1)
{
    (void)option;
    (void)arg1;

    VERIFY_NO_PROCESS_BIG_LOCK(this);
    return ENOSYS;
}

}
