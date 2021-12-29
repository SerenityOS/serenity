/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Process.h>

namespace Kernel {

ErrorOr<FlatPtr> Process::sys$getuid()
{
    VERIFY_PROCESS_BIG_LOCK_ACQUIRED(this)
    TRY(require_promise(Pledge::stdio));
    return uid().value();
}

ErrorOr<FlatPtr> Process::sys$getgid()
{
    VERIFY_PROCESS_BIG_LOCK_ACQUIRED(this)
    TRY(require_promise(Pledge::stdio));
    return gid().value();
}

ErrorOr<FlatPtr> Process::sys$geteuid()
{
    VERIFY_PROCESS_BIG_LOCK_ACQUIRED(this)
    TRY(require_promise(Pledge::stdio));
    return euid().value();
}

ErrorOr<FlatPtr> Process::sys$getegid()
{
    VERIFY_PROCESS_BIG_LOCK_ACQUIRED(this)
    TRY(require_promise(Pledge::stdio));
    return egid().value();
}

ErrorOr<FlatPtr> Process::sys$getresuid(Userspace<UserID*> ruid, Userspace<UserID*> euid, Userspace<UserID*> suid)
{
    VERIFY_PROCESS_BIG_LOCK_ACQUIRED(this)
    TRY(require_promise(Pledge::stdio));
    TRY(copy_to_user(ruid, &m_protected_values.uid));
    TRY(copy_to_user(euid, &m_protected_values.euid));
    TRY(copy_to_user(suid, &m_protected_values.suid));
    return 0;
}

ErrorOr<FlatPtr> Process::sys$getresgid(Userspace<GroupID*> rgid, Userspace<GroupID*> egid, Userspace<GroupID*> sgid)
{
    VERIFY_PROCESS_BIG_LOCK_ACQUIRED(this)
    TRY(require_promise(Pledge::stdio));
    TRY(copy_to_user(rgid, &m_protected_values.gid));
    TRY(copy_to_user(egid, &m_protected_values.egid));
    TRY(copy_to_user(sgid, &m_protected_values.sgid));
    return 0;
}

ErrorOr<FlatPtr> Process::sys$getgroups(size_t count, Userspace<gid_t*> user_gids)
{
    VERIFY_PROCESS_BIG_LOCK_ACQUIRED(this)
    TRY(require_promise(Pledge::stdio));
    if (!count)
        return extra_gids().size();
    if (count != extra_gids().size())
        return EINVAL;
    TRY(copy_to_user(user_gids, extra_gids().data(), sizeof(gid_t) * count));
    return 0;
}

}
