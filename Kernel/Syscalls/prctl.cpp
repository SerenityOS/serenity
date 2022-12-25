/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/API/prctl_numbers.h>
#include <Kernel/Process.h>

namespace Kernel {

ErrorOr<FlatPtr> Process::sys$prctl(int option, FlatPtr arg1, [[maybe_unused]] FlatPtr arg2)
{
    VERIFY_PROCESS_BIG_LOCK_ACQUIRED(this);
    return with_mutable_protected_data([&](auto& protected_data) -> ErrorOr<FlatPtr> {
        switch (option) {
        case PR_GET_DUMPABLE:
            return protected_data.dumpable;
        case PR_SET_DUMPABLE:
            if (arg1 != 0 && arg1 != 1)
                return EINVAL;
            protected_data.dumpable = arg1;
            return 0;
        }
        return EINVAL;
    });
}

}
