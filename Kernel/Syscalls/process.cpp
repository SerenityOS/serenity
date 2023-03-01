/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Types.h>
#include <Kernel/Process.h>

namespace Kernel {

ErrorOr<FlatPtr> Process::sys$getpid()
{
    VERIFY_NO_PROCESS_BIG_LOCK(this);
    TRY(require_promise(Pledge::stdio));
    return pid().value();
}

ErrorOr<FlatPtr> Process::sys$getppid()
{
    VERIFY_NO_PROCESS_BIG_LOCK(this);
    TRY(require_promise(Pledge::stdio));
    return ppid().value();
}

ErrorOr<FlatPtr> Process::sys$get_process_name(Userspace<char*> buffer, size_t buffer_size)
{
    VERIFY_NO_PROCESS_BIG_LOCK(this);
    TRY(require_promise(Pledge::stdio));

    TRY(m_name.with([&buffer, buffer_size](auto& name) -> ErrorOr<void> {
        if (name->length() + 1 > buffer_size)
            return ENAMETOOLONG;

        return copy_to_user(buffer, name->characters(), name->length() + 1);
    }));

    return 0;
}

ErrorOr<FlatPtr> Process::sys$set_process_name(Userspace<char const*> user_name, size_t user_name_length)
{
    VERIFY_NO_PROCESS_BIG_LOCK(this);
    TRY(require_promise(Pledge::proc));
    if (user_name_length > 256)
        return ENAMETOOLONG;
    auto name = TRY(try_copy_kstring_from_user(user_name, user_name_length));
    // Empty and whitespace-only names only exist to confuse users.
    if (name->view().is_whitespace())
        return EINVAL;
    set_name(move(name));
    return 0;
}

}
