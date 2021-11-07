/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Process.h>
#include <LibC/sys/prctl_numbers.h>

namespace Kernel {

ErrorOr<FlatPtr> Process::sys$prctl(int option, FlatPtr arg1, [[maybe_unused]] FlatPtr arg2)
{
    VERIFY_PROCESS_BIG_LOCK_ACQUIRED(this)
    switch (option) {
    case PR_GET_DUMPABLE:
        return is_dumpable();
    case PR_SET_DUMPABLE:
        set_dumpable(arg1);
        return 0;
    }
    return EINVAL;
}

}
