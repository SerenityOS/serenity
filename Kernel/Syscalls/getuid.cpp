/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Process.h>

namespace Kernel {

KResultOr<uid_t> Process::sys$getuid()
{
    REQUIRE_PROMISE(stdio);
    return uid();
}

KResultOr<gid_t> Process::sys$getgid()
{
    REQUIRE_PROMISE(stdio);
    return gid();
}

KResultOr<uid_t> Process::sys$geteuid()
{
    REQUIRE_PROMISE(stdio);
    return euid();
}

KResultOr<gid_t> Process::sys$getegid()
{
    REQUIRE_PROMISE(stdio);
    return egid();
}

KResultOr<int> Process::sys$getresuid(Userspace<uid_t*> ruid, Userspace<uid_t*> euid, Userspace<uid_t*> suid)
{
    REQUIRE_PROMISE(stdio);
    if (!copy_to_user(ruid, &m_uid) || !copy_to_user(euid, &m_euid) || !copy_to_user(suid, &m_suid))
        return EFAULT;
    return 0;
}

KResultOr<int> Process::sys$getresgid(Userspace<gid_t*> rgid, Userspace<gid_t*> egid, Userspace<gid_t*> sgid)
{
    REQUIRE_PROMISE(stdio);
    if (!copy_to_user(rgid, &m_gid) || !copy_to_user(egid, &m_egid) || !copy_to_user(sgid, &m_sgid))
        return EFAULT;
    return 0;
}

KResultOr<int> Process::sys$getgroups(ssize_t count, Userspace<gid_t*> user_gids)
{
    REQUIRE_PROMISE(stdio);
    if (count < 0)
        return EINVAL;
    if (!count)
        return extra_gids().size();
    if (count != (int)extra_gids().size())
        return EINVAL;

    if (!copy_to_user(user_gids, extra_gids().data(), sizeof(gid_t) * count))
        return EFAULT;

    return 0;
}

}
