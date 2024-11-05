/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/API/prctl_numbers.h>
#include <Kernel/Tasks/Process.h>

namespace Kernel {

ErrorOr<FlatPtr> Process::sys$prctl(int option, FlatPtr arg1, FlatPtr arg2, FlatPtr arg3)
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
            if (arg1 != 0)
                return EINVAL;
            return address_space().with([&](auto& space) -> ErrorOr<FlatPtr> {
                space->set_enforces_syscall_regions();
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
            size_t buffer_size = static_cast<size_t>(arg2);
            Process::Name process_name {};
            TRY(try_copy_name_from_user_into_fixed_string_buffer(buffer, process_name, buffer_size));
            // NOTE: Reject empty and whitespace-only names, as they only confuse users.
            if (process_name.representable_view().is_whitespace())
                return EINVAL;
            set_name(process_name.representable_view());
            return 0;
        }
        case PR_GET_PROCESS_NAME: {
            TRY(require_promise(Pledge::stdio));
            Userspace<char*> buffer = arg1;
            size_t buffer_size = static_cast<size_t>(arg2);
            TRY(m_name.with([&buffer, buffer_size](auto& name) -> ErrorOr<void> {
                VERIFY(!name.representable_view().is_null());
                return copy_fixed_string_buffer_including_null_char_to_user(buffer, buffer_size, name);
            }));
            return 0;
        }
        case PR_SET_THREAD_NAME: {
            TRY(require_promise(Pledge::stdio));
            int thread_id = static_cast<int>(arg1);
            Userspace<char const*> buffer = arg2;
            size_t buffer_size = static_cast<size_t>(arg3);

            Thread::Name thread_name {};
            TRY(try_copy_name_from_user_into_fixed_string_buffer(buffer, thread_name, buffer_size));
            auto thread = TRY(get_thread_from_thread_list(thread_id));
            thread->set_name(thread_name.representable_view());
            return 0;
        }
        case PR_GET_THREAD_NAME: {
            TRY(require_promise(Pledge::thread));
            int thread_id = static_cast<int>(arg1);
            Userspace<char*> buffer = arg2;
            size_t buffer_size = static_cast<size_t>(arg3);
            auto thread = TRY(get_thread_from_thread_list(thread_id));

            TRY(thread->name().with([&](auto& thread_name) -> ErrorOr<void> {
                VERIFY(!thread_name.representable_view().is_null());
                return copy_fixed_string_buffer_including_null_char_to_user(buffer, buffer_size, thread_name);
            }));
            return 0;
        }
        case PR_SET_NO_TRANSITION_TO_EXECUTABLE_FROM_WRITABLE_PROT: {
            TRY(require_promise(Pledge::prot_exec));
            with_mutable_protected_data([](auto& protected_data) {
                return protected_data.reject_transition_to_executable_from_writable_prot.set();
            });
            return 0;
        }
        case PR_SET_JAILED_UNTIL_EXIT: {
            TRY(require_promise(Pledge::proc));
            with_mutable_protected_data([](auto& protected_values) {
                protected_values.jailed_until_exit.set();
            });
            return 0;
        }
        case PR_SET_JAILED_UNTIL_EXEC: {
            TRY(require_promise(Pledge::proc));
            with_mutable_protected_data([](auto& protected_values) {
                protected_values.jailed_until_exec = true;
            });
            return 0;
        }
        }

        return EINVAL;
    });
}

}
