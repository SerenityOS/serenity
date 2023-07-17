/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/API/prctl_numbers.h>
#include <Kernel/Tasks/Process.h>

namespace Kernel {

ErrorOr<FlatPtr> Process::sys$prctl(int option, FlatPtr arg1, FlatPtr arg2)
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
        case PR_SET_PROCESS_NAME: {
            TRY(require_promise(Pledge::proc));
            Userspace<char const*> buffer = arg1;
            int user_buffer_size = static_cast<int>(arg2);
            if (user_buffer_size < 0)
                return EINVAL;
            size_t buffer_size = static_cast<size_t>(user_buffer_size);
            Process::Name process_name {};
            TRY(try_copy_name_from_user_into_fixed_string_buffer<32>(buffer, process_name, buffer_size));
            // NOTE: Reject empty and whitespace-only names, as they only confuse users.
            if (process_name.representable_view().is_whitespace())
                return EINVAL;
            set_name(process_name.representable_view());
            return 0;
        }
        case PR_GET_PROCESS_NAME: {
            TRY(require_promise(Pledge::stdio));
            Userspace<char*> buffer = arg1;
            int user_buffer_size = arg2;
            if (user_buffer_size < 0)
                return EINVAL;
            size_t buffer_size = static_cast<size_t>(arg2);
            TRY(m_name.with([&buffer, buffer_size](auto& name) -> ErrorOr<void> {
                auto view = name.representable_view();
                if (view.length() + 1 > buffer_size)
                    return ENAMETOOLONG;
                return copy_to_user(buffer, view.characters_without_null_termination(), view.length() + 1);
            }));
            return 0;
        }
        }

        return EINVAL;
    });
}

}
