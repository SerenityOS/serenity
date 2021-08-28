/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Process.h>

namespace Kernel {

KResultOr<FlatPtr> Process::sys$getuid()
{
    VERIFY_PROCESS_BIG_LOCK_ACQUIRED(this)
    REQUIRE_PROMISE(stdio);
    return uid().value();
}

KResultOr<FlatPtr> Process::sys$getgid()
{
    VERIFY_PROCESS_BIG_LOCK_ACQUIRED(this)
    REQUIRE_PROMISE(stdio);
    return gid().value();
}

KResultOr<FlatPtr> Process::sys$geteuid()
{
    VERIFY_PROCESS_BIG_LOCK_ACQUIRED(this)
    REQUIRE_PROMISE(stdio);
    return euid().value();
}

KResultOr<FlatPtr> Process::sys$getegid()
{
    VERIFY_PROCESS_BIG_LOCK_ACQUIRED(this)
    REQUIRE_PROMISE(stdio);
    return egid().value();
}

KResultOr<FlatPtr> Process::sys$getresuid(Userspace<UserID*> ruid, Userspace<UserID*> euid, Userspace<UserID*> suid)
{
    VERIFY_PROCESS_BIG_LOCK_ACQUIRED(this)
    REQUIRE_PROMISE(stdio);
    if (!copy_to_user(ruid, &m_protected_values.uid) || !copy_to_user(euid, &m_protected_values.euid) || !copy_to_user(suid, &m_protected_values.suid))
        return EFAULT;
    return 0;
}

KResultOr<FlatPtr> Process::sys$getresgid(Userspace<GroupID*> rgid, Userspace<GroupID*> egid, Userspace<GroupID*> sgid)
{
    VERIFY_PROCESS_BIG_LOCK_ACQUIRED(this)
    REQUIRE_PROMISE(stdio);
    if (!copy_to_user(rgid, &m_protected_values.gid) || !copy_to_user(egid, &m_protected_values.egid) || !copy_to_user(sgid, &m_protected_values.sgid))
        return EFAULT;
    return 0;
}

KResultOr<FlatPtr> Process::sys$getgroups(size_t count, Userspace<gid_t*> user_gids)
{
    VERIFY_PROCESS_BIG_LOCK_ACQUIRED(this)
    REQUIRE_PROMISE(stdio);
    if (!count)
        return extra_gids().size();
    if (count != extra_gids().size())
        return EINVAL;

    if (!copy_to_user(user_gids, extra_gids().data(), sizeof(gid_t) * count))
        return EFAULT;

    return 0;
}

}
