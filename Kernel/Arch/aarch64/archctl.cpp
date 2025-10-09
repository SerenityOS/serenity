/*
 * Copyright (c) 2024, Sönke Holz <soenke.holz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Tasks/Process.h>

namespace Kernel {

ErrorOr<FlatPtr> Process::sys$archctl(int option, FlatPtr arg1, FlatPtr, FlatPtr)
{
    (void)option;
    (void)arg1;

    VERIFY_NO_PROCESS_BIG_LOCK(this);
    return ENOSYS;
}

}
