/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Process.h>

namespace Kernel {

ErrorOr<FlatPtr> Process::sys$seteuid(UserID new_euid)
{
    VERIFY_PROCESS_BIG_LOCK_ACQUIRED(this)
    TRY(require_promise(Pledge::id));

    if (new_euid == (uid_t)-1)
        return EINVAL;

    if (new_euid != uid() && new_euid != suid() && !is_superuser())
        return EPERM;

    if (euid() != new_euid)
        set_dumpable(false);

    ProtectedDataMutationScope scope { *this };

    m_protected_values.euid = new_euid;
    return 0;
}

ErrorOr<FlatPtr> Process::sys$setegid(GroupID new_egid)
{
    VERIFY_PROCESS_BIG_LOCK_ACQUIRED(this)
    TRY(require_promise(Pledge::id));

    if (new_egid == (uid_t)-1)
        return EINVAL;

    if (new_egid != gid() && new_egid != sgid() && !is_superuser())
        return EPERM;

    if (egid() != new_egid)
        set_dumpable(false);

    ProtectedDataMutationScope scope { *this };
    m_protected_values.egid = new_egid;
    return 0;
}

ErrorOr<FlatPtr> Process::sys$setuid(UserID new_uid)
{
    VERIFY_PROCESS_BIG_LOCK_ACQUIRED(this)
    TRY(require_promise(Pledge::id));

    if (new_uid == (uid_t)-1)
        return EINVAL;

    if (new_uid != uid() && new_uid != euid() && !is_superuser())
        return EPERM;

    if (euid() != new_uid)
        set_dumpable(false);

    ProtectedDataMutationScope scope { *this };
    m_protected_values.uid = new_uid;
    m_protected_values.euid = new_uid;
    m_protected_values.suid = new_uid;
    return 0;
}

ErrorOr<FlatPtr> Process::sys$setgid(GroupID new_gid)
{
    VERIFY_PROCESS_BIG_LOCK_ACQUIRED(this)
    TRY(require_promise(Pledge::id));

    if (new_gid == (uid_t)-1)
        return EINVAL;

    if (new_gid != gid() && new_gid != egid() && !is_superuser())
        return EPERM;

    if (egid() != new_gid)
        set_dumpable(false);

    ProtectedDataMutationScope scope { *this };
    m_protected_values.gid = new_gid;
    m_protected_values.egid = new_gid;
    m_protected_values.sgid = new_gid;
    return 0;
}

ErrorOr<FlatPtr> Process::sys$setreuid(UserID new_ruid, UserID new_euid)
{
    VERIFY_PROCESS_BIG_LOCK_ACQUIRED(this)
    TRY(require_promise(Pledge::id));

    if (new_ruid == (uid_t)-1)
        new_ruid = uid();
    if (new_euid == (uid_t)-1)
        new_euid = euid();

    auto ok = [this](UserID id) { return id == uid() || id == euid() || id == suid(); };
    if (!ok(new_ruid) || !ok(new_euid))
        return EPERM;

    if (new_ruid < (uid_t)-1 || new_euid < (uid_t)-1)
        return EINVAL;

    if (euid() != new_euid)
        set_dumpable(false);

    ProtectedDataMutationScope scope { *this };
    m_protected_values.uid = new_ruid;
    m_protected_values.euid = new_euid;
    return 0;
}

ErrorOr<FlatPtr> Process::sys$setresuid(UserID new_ruid, UserID new_euid, UserID new_suid)
{
    VERIFY_PROCESS_BIG_LOCK_ACQUIRED(this)
    TRY(require_promise(Pledge::id));

    if (new_ruid == (uid_t)-1)
        new_ruid = uid();
    if (new_euid == (uid_t)-1)
        new_euid = euid();
    if (new_suid == (uid_t)-1)
        new_suid = suid();

    auto ok = [this](UserID id) { return id == uid() || id == euid() || id == suid(); };
    if ((!ok(new_ruid) || !ok(new_euid) || !ok(new_suid)) && !is_superuser())
        return EPERM;

    if (euid() != new_euid)
        set_dumpable(false);

    ProtectedDataMutationScope scope { *this };
    m_protected_values.uid = new_ruid;
    m_protected_values.euid = new_euid;
    m_protected_values.suid = new_suid;
    return 0;
}

ErrorOr<FlatPtr> Process::sys$setresgid(GroupID new_rgid, GroupID new_egid, GroupID new_sgid)
{
    VERIFY_PROCESS_BIG_LOCK_ACQUIRED(this)
    TRY(require_promise(Pledge::id));

    if (new_rgid == (gid_t)-1)
        new_rgid = gid();
    if (new_egid == (gid_t)-1)
        new_egid = egid();
    if (new_sgid == (gid_t)-1)
        new_sgid = sgid();

    auto ok = [this](GroupID id) { return id == gid() || id == egid() || id == sgid(); };
    if ((!ok(new_rgid) || !ok(new_egid) || !ok(new_sgid)) && !is_superuser())
        return EPERM;

    if (egid() != new_egid)
        set_dumpable(false);

    ProtectedDataMutationScope scope { *this };
    m_protected_values.gid = new_rgid;
    m_protected_values.egid = new_egid;
    m_protected_values.sgid = new_sgid;
    return 0;
}

ErrorOr<FlatPtr> Process::sys$setgroups(size_t count, Userspace<const gid_t*> user_gids)
{
    VERIFY_PROCESS_BIG_LOCK_ACQUIRED(this)
    TRY(require_promise(Pledge::id));
    if (!is_superuser())
        return EPERM;

    if (!count) {
        ProtectedDataMutationScope scope { *this };
        m_protected_values.extra_gids.clear();
        return 0;
    }

    Vector<gid_t> new_extra_gids;
    TRY(new_extra_gids.try_resize(count));
    TRY(copy_n_from_user(new_extra_gids.data(), user_gids, count));

    HashTable<gid_t> unique_extra_gids;
    for (auto& extra_gid : new_extra_gids) {
        if (extra_gid != gid())
            TRY(unique_extra_gids.try_set(extra_gid));
    }

    ProtectedDataMutationScope scope { *this };
    TRY(m_protected_values.extra_gids.try_resize(unique_extra_gids.size()));
    size_t i = 0;
    for (auto& extra_gid : unique_extra_gids) {
        if (extra_gid == gid())
            continue;
        m_protected_values.extra_gids[i++] = extra_gid;
    }
    return 0;
}

}
