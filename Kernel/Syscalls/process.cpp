/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Types.h>
#include <Kernel/Process.h>

namespace Kernel {

KResultOr<FlatPtr> Process::sys$getpid()
{
    VERIFY_NO_PROCESS_BIG_LOCK(this)
    REQUIRE_PROMISE(stdio);
    return pid().value();
}

KResultOr<FlatPtr> Process::sys$getppid()
{
    VERIFY_PROCESS_BIG_LOCK_ACQUIRED(this)
    REQUIRE_PROMISE(stdio);
    return m_protected_values.ppid.value();
}

KResultOr<FlatPtr> Process::sys$get_process_name(Userspace<char*> buffer, size_t buffer_size)
{
    VERIFY_PROCESS_BIG_LOCK_ACQUIRED(this)
    REQUIRE_PROMISE(stdio);
    if (m_name.length() + 1 > buffer_size)
        return ENAMETOOLONG;

    if (!copy_to_user(buffer, m_name.characters(), m_name.length() + 1))
        return EFAULT;
    return 0;
}

KResultOr<FlatPtr> Process::sys$set_process_name(Userspace<char const*> user_name, size_t user_name_length)
{
    VERIFY_PROCESS_BIG_LOCK_ACQUIRED(this)
    REQUIRE_PROMISE(proc);
    if (user_name_length > 256)
        return ENAMETOOLONG;
    auto name_or_error = try_copy_kstring_from_user(user_name, user_name_length);
    if (name_or_error.is_error())
        return name_or_error.error();
    // Empty and whitespace-only names only exist to confuse users.
    if (name_or_error.value()->view().is_whitespace())
        return EINVAL;
    // FIXME: There's a String copy here. Process::m_name should be a KString.
    m_name = name_or_error.value()->view();
    return 0;
}

KResultOr<FlatPtr> Process::sys$set_coredump_metadata(Userspace<const Syscall::SC_set_coredump_metadata_params*> user_params)
{
    VERIFY_PROCESS_BIG_LOCK_ACQUIRED(this)
    Syscall::SC_set_coredump_metadata_params params;
    if (!copy_from_user(&params, user_params))
        return EFAULT;
    if (params.key.length == 0 || params.key.length > 16 * KiB)
        return EINVAL;
    if (params.value.length > 16 * KiB)
        return EINVAL;
    auto key_or_error = try_copy_kstring_from_user(params.key);
    if (key_or_error.is_error())
        return key_or_error.error();
    auto key = key_or_error.release_value();
    auto value_or_error = try_copy_kstring_from_user(params.value);
    if (value_or_error.is_error())
        return value_or_error.error();
    auto value = value_or_error.release_value();
    return set_coredump_property(move(key), move(value));
}

}
