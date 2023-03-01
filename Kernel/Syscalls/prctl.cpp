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
    VERIFY_NO_PROCESS_BIG_LOCK(this);
    return with_mutable_protected_data([&](auto& protected_data) -> ErrorOr<FlatPtr> {
        switch (option) {
        case PR_GET_DUMPABLE:
            return protected_data.dumpable;
        case PR_SET_DUMPABLE:
            if (arg1 != 0 && arg1 != 1)
                return EINVAL;
            protected_data.dumpable = arg1;
            return 0;
        case PR_GET_NO_NEW_SYSCALL_REGION_ANNOTATIONS:
            return address_space().with([&](auto& space) -> ErrorOr<FlatPtr> {
                return space->enforces_syscall_regions();
            });
        case PR_SET_NO_NEW_SYSCALL_REGION_ANNOTATIONS: {
            if (arg1 != 0 && arg1 != 1)
                return EINVAL;
            bool prohibit_new_annotated_syscall_regions = (arg1 == 1);
            return address_space().with([&](auto& space) -> ErrorOr<FlatPtr> {
                if (space->enforces_syscall_regions() && !prohibit_new_annotated_syscall_regions)
                    return EPERM;

                space->set_enforces_syscall_regions(prohibit_new_annotated_syscall_regions);
                return 0;
            });
            return 0;
        }
        case PR_SET_COREDUMP_METADATA_VALUE: {
            auto params = TRY(copy_typed_from_user<Syscall::SC_set_coredump_metadata_params>(arg1));
            if (params.key.length == 0 || params.key.length > 16 * KiB)
                return EINVAL;
            if (params.value.length > 16 * KiB)
                return EINVAL;
            auto key = TRY(try_copy_kstring_from_user(params.key));
            auto value = TRY(try_copy_kstring_from_user(params.value));
            TRY(set_coredump_property(move(key), move(value)));
            return 0;
        }
        }

        return EINVAL;
    });
}

}
