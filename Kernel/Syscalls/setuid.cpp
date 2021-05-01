/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Process.h>

namespace Kernel {

KResultOr<int> Process::sys$seteuid(uid_t new_euid)
{
    REQUIRE_PROMISE(id);

    if (new_euid != uid() && new_euid != suid() && !is_superuser())
        return EPERM;

    if (euid() != new_euid)
        set_dumpable(false);

    ProtectedDataMutationScope scope { *this };

    m_euid = new_euid;
    return 0;
}

KResultOr<int> Process::sys$setegid(gid_t new_egid)
{
    REQUIRE_PROMISE(id);

    if (new_egid != gid() && new_egid != sgid() && !is_superuser())
        return EPERM;

    if (egid() != new_egid)
        set_dumpable(false);

    ProtectedDataMutationScope scope { *this };
    m_egid = new_egid;
    return 0;
}

KResultOr<int> Process::sys$setuid(uid_t new_uid)
{
    REQUIRE_PROMISE(id);

    if (new_uid != uid() && new_uid != euid() && !is_superuser())
        return EPERM;

    if (euid() != new_uid)
        set_dumpable(false);

    ProtectedDataMutationScope scope { *this };
    m_uid = new_uid;
    m_euid = new_uid;
    m_suid = new_uid;
    return 0;
}

KResultOr<int> Process::sys$setgid(gid_t new_gid)
{
    REQUIRE_PROMISE(id);

    if (new_gid != gid() && new_gid != egid() && !is_superuser())
        return EPERM;

    if (egid() != new_gid)
        set_dumpable(false);

    ProtectedDataMutationScope scope { *this };
    m_gid = new_gid;
    m_egid = new_gid;
    m_sgid = new_gid;
    return 0;
}

KResultOr<int> Process::sys$setreuid(uid_t new_ruid, uid_t new_euid)
{
    REQUIRE_PROMISE(id);

    if (new_ruid == (uid_t)-1)
        new_ruid = uid();
    if (new_euid == (uid_t)-1)
        new_euid = euid();

    auto ok = [this](uid_t id) { return id == uid() || id == euid() || id == suid(); };
    if (!ok(new_ruid) || !ok(new_euid))
        return EPERM;

    if (new_ruid < (uid_t)-1 || new_euid < (uid_t)-1)
        return EINVAL;

    if (euid() != new_euid)
        set_dumpable(false);

    ProtectedDataMutationScope scope { *this };
    m_uid = new_ruid;
    m_euid = new_euid;
    return 0;
}

KResultOr<int> Process::sys$setresuid(uid_t new_ruid, uid_t new_euid, uid_t new_suid)
{
    REQUIRE_PROMISE(id);

    if (new_ruid == (uid_t)-1)
        new_ruid = uid();
    if (new_euid == (uid_t)-1)
        new_euid = euid();
    if (new_suid == (uid_t)-1)
        new_suid = suid();

    auto ok = [this](uid_t id) { return id == uid() || id == euid() || id == suid(); };
    if ((!ok(new_ruid) || !ok(new_euid) || !ok(new_suid)) && !is_superuser())
        return EPERM;

    if (euid() != new_euid)
        set_dumpable(false);

    ProtectedDataMutationScope scope { *this };
    m_uid = new_ruid;
    m_euid = new_euid;
    m_suid = new_suid;
    return 0;
}

KResultOr<int> Process::sys$setresgid(gid_t new_rgid, gid_t new_egid, gid_t new_sgid)
{
    REQUIRE_PROMISE(id);

    if (new_rgid == (gid_t)-1)
        new_rgid = gid();
    if (new_egid == (gid_t)-1)
        new_egid = egid();
    if (new_sgid == (gid_t)-1)
        new_sgid = sgid();

    auto ok = [this](gid_t id) { return id == gid() || id == egid() || id == sgid(); };
    if ((!ok(new_rgid) || !ok(new_egid) || !ok(new_sgid)) && !is_superuser())
        return EPERM;

    if (egid() != new_egid)
        set_dumpable(false);

    ProtectedDataMutationScope scope { *this };
    m_gid = new_rgid;
    m_egid = new_egid;
    m_sgid = new_sgid;
    return 0;
}

KResultOr<int> Process::sys$setgroups(ssize_t count, Userspace<const gid_t*> user_gids)
{
    REQUIRE_PROMISE(id);
    if (count < 0)
        return EINVAL;
    if (!is_superuser())
        return EPERM;

    if (!count) {
        ProtectedDataMutationScope scope { *this };
        m_extra_gids.clear();
        return 0;
    }

    Vector<gid_t> new_extra_gids;
    if (!new_extra_gids.try_resize(count))
        return ENOMEM;
    if (!copy_n_from_user(new_extra_gids.data(), user_gids, count))
        return EFAULT;

    HashTable<gid_t> unique_extra_gids;
    for (auto& extra_gid : new_extra_gids) {
        if (extra_gid != gid())
            unique_extra_gids.set(extra_gid);
    }

    ProtectedDataMutationScope scope { *this };
    if (!m_extra_gids.try_resize(unique_extra_gids.size()))
        return ENOMEM;
    size_t i = 0;
    for (auto& extra_gid : unique_extra_gids) {
        if (extra_gid == gid())
            continue;
        m_extra_gids[i++] = extra_gid;
    }
    return 0;
}

}
