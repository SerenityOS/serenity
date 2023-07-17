/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Tasks/Process.h>

namespace Kernel {

ErrorOr<FlatPtr> Process::sys$gethostname(Userspace<char*> buffer, size_t size)
{
    VERIFY_NO_PROCESS_BIG_LOCK(this);
    TRY(require_promise(Pledge::stdio));
    if (size > NumericLimits<ssize_t>::max())
        return EINVAL;
    return hostname().with_shared([&](auto const& name) -> ErrorOr<FlatPtr> {
        // NOTE: To be able to copy a null-terminated string, we need at most
        // 65 characters to store and copy and not 64 here, to store the whole
        // hostname string + null terminator.
        FixedStringBuffer<UTSNAME_ENTRY_LEN> current_hostname {};
        current_hostname.store_characters(name.representable_view());
        auto name_view = current_hostname.representable_view();
        if (size < (name_view.length() + 1))
            return ENAMETOOLONG;
        TRY(copy_to_user(buffer, name_view.characters_without_null_termination(), name_view.length() + 1));
        return 0;
    });
}

ErrorOr<FlatPtr> Process::sys$sethostname(Userspace<char const*> buffer, size_t length)
{
    VERIFY_NO_PROCESS_BIG_LOCK(this);
    TRY(require_no_promises());

    auto credentials = this->credentials();
    if (!credentials->is_superuser())
        return EPERM;
    auto new_hostname = TRY(get_syscall_name_string_fixed_buffer<UTSNAME_ENTRY_LEN - 1>(buffer, length));
    hostname().with_exclusive([&](auto& name) {
        name.store_characters(new_hostname.representable_view());
    });
    return 0;
}

}
