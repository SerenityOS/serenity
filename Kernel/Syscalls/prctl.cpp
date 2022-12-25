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
        case PR_GET_NO_NEW_PRIVS:
            return to_underlying(protected_data.no_new_privs_mode);
        case PR_SET_NO_NEW_PRIVS:
            if (arg1 != 0 && arg1 != 1 && arg1 != 2)
                return EINVAL;
            auto new_no_privs_mode = static_cast<NoNewPrivsMode>(arg1);
            VERIFY(new_no_privs_mode == NoNewPrivsMode::Disabled || new_no_privs_mode == NoNewPrivsMode::Enforced || new_no_privs_mode == NoNewPrivsMode::EnforcedQuietly);

            if (protected_data.no_new_privs_mode != NoNewPrivsMode::Disabled) {
                // NOTE: Silently ignore setting no_new_privs to either one of the enforcing values and the corresponding value being requested is the same.
                if (protected_data.no_new_privs_mode == new_no_privs_mode)
                    return 0;
                // NOTE: Disallow setting no_new_privs to non-enforcing mode if already enforcing a no_new_privs mode.
                // Also block setting a different enforcing mode.
                return EPERM;
            }

            protected_data.no_new_privs_mode = new_no_privs_mode;
            return 0;
        }
        return EINVAL;
    });
}

}
