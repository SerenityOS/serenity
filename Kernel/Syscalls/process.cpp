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
    VERIFY_NO_PROCESS_BIG_LOCK(this)
    TRY(require_promise(Pledge::stdio));
    return pid().value();
}

ErrorOr<FlatPtr> Process::sys$getppid()
{
    VERIFY_PROCESS_BIG_LOCK_ACQUIRED(this)
    TRY(require_promise(Pledge::stdio));
    return m_protected_values.ppid.value();
}

ErrorOr<FlatPtr> Process::sys$get_process_name(Userspace<char*> buffer, size_t buffer_size)
{
    VERIFY_PROCESS_BIG_LOCK_ACQUIRED(this)
    TRY(require_promise(Pledge::stdio));
    if (m_name->length() + 1 > buffer_size)
        return ENAMETOOLONG;

    TRY(copy_to_user(buffer, m_name->characters(), m_name->length() + 1));
    return 0;
}

ErrorOr<FlatPtr> Process::sys$set_process_name(Userspace<const char*> user_name, size_t user_name_length)
{
    VERIFY_PROCESS_BIG_LOCK_ACQUIRED(this)
    TRY(require_promise(Pledge::proc));
    if (user_name_length > 256)
        return ENAMETOOLONG;
    auto name = TRY(try_copy_kstring_from_user(user_name, user_name_length));
    // Empty and whitespace-only names only exist to confuse users.
    if (name->view().is_whitespace())
        return EINVAL;
    m_name = move(name);
    return 0;
}

ErrorOr<FlatPtr> Process::sys$set_coredump_metadata(Userspace<const Syscall::SC_set_coredump_metadata_params*> user_params)
{
    VERIFY_PROCESS_BIG_LOCK_ACQUIRED(this)
    auto params = TRY(copy_typed_from_user(user_params));

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
