/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/API/POSIX/sys/limits.h>
#include <Kernel/Tasks/Process.h>

namespace Kernel {

ErrorOr<FlatPtr> Process::sys$seteuid(UserID new_euid)
{
    VERIFY_NO_PROCESS_BIG_LOCK(this);
    TRY(require_promise(Pledge::id));

    if (new_euid == (uid_t)-1)
        return EINVAL;

    return with_mutable_protected_data([&](auto& protected_data) -> ErrorOr<FlatPtr> {
        auto credentials = this->credentials();

        if (new_euid != credentials->uid() && new_euid != credentials->suid() && !credentials->is_superuser())
            return EPERM;

        auto new_credentials = TRY(Credentials::create(
            credentials->uid(),
            credentials->gid(),
            new_euid,
            credentials->egid(),
            credentials->suid(),
            credentials->sgid(),
            credentials->extra_gids(),
            credentials->sid(),
            credentials->pgid()));

        if (credentials->euid() != new_euid)
            protected_data.dumpable = false;

        protected_data.credentials = move(new_credentials);
        return 0;
    });
}

ErrorOr<FlatPtr> Process::sys$setegid(GroupID new_egid)
{
    VERIFY_NO_PROCESS_BIG_LOCK(this);
    TRY(require_promise(Pledge::id));

    if (new_egid == (uid_t)-1)
        return EINVAL;

    return with_mutable_protected_data([&](auto& protected_data) -> ErrorOr<FlatPtr> {
        auto credentials = this->credentials();

        if (new_egid != credentials->gid() && new_egid != credentials->sgid() && !credentials->is_superuser())
            return EPERM;

        auto new_credentials = TRY(Credentials::create(
            credentials->uid(),
            credentials->gid(),
            credentials->euid(),
            new_egid,
            credentials->suid(),
            credentials->sgid(),
            credentials->extra_gids(),
            credentials->sid(),
            credentials->pgid()));

        if (credentials->egid() != new_egid)
            protected_data.dumpable = false;

        protected_data.credentials = move(new_credentials);
        return 0;
    });
}

ErrorOr<FlatPtr> Process::sys$setuid(UserID new_uid)
{
    VERIFY_NO_PROCESS_BIG_LOCK(this);
    TRY(require_promise(Pledge::id));

    if (new_uid == (uid_t)-1)
        return EINVAL;

    return with_mutable_protected_data([&](auto& protected_data) -> ErrorOr<FlatPtr> {
        auto credentials = this->credentials();

        if (new_uid != credentials->uid() && new_uid != credentials->euid() && !credentials->is_superuser())
            return EPERM;

        auto new_credentials = TRY(Credentials::create(
            new_uid,
            credentials->gid(),
            new_uid,
            credentials->egid(),
            new_uid,
            credentials->sgid(),
            credentials->extra_gids(),
            credentials->sid(),
            credentials->pgid()));

        if (credentials->euid() != new_uid)
            protected_data.dumpable = false;

        protected_data.credentials = move(new_credentials);
        return 0;
    });
}

ErrorOr<FlatPtr> Process::sys$setgid(GroupID new_gid)
{
    VERIFY_NO_PROCESS_BIG_LOCK(this);
    TRY(require_promise(Pledge::id));

    if (new_gid == (uid_t)-1)
        return EINVAL;

    return with_mutable_protected_data([&](auto& protected_data) -> ErrorOr<FlatPtr> {
        auto credentials = this->credentials();

        if (new_gid != credentials->gid() && new_gid != credentials->egid() && !credentials->is_superuser())
            return EPERM;

        auto new_credentials = TRY(Credentials::create(
            credentials->uid(),
            new_gid,
            credentials->euid(),
            new_gid,
            credentials->suid(),
            new_gid,
            credentials->extra_gids(),
            credentials->sid(),
            credentials->pgid()));

        if (credentials->egid() != new_gid)
            protected_data.dumpable = false;

        protected_data.credentials = move(new_credentials);
        return 0;
    });
}

ErrorOr<FlatPtr> Process::sys$setreuid(UserID new_ruid, UserID new_euid)
{
    VERIFY_NO_PROCESS_BIG_LOCK(this);
    TRY(require_promise(Pledge::id));

    return with_mutable_protected_data([&](auto& protected_data) -> ErrorOr<FlatPtr> {
        auto credentials = this->credentials();

        if (new_ruid == (uid_t)-1)
            new_ruid = credentials->uid();
        if (new_euid == (uid_t)-1)
            new_euid = credentials->euid();

        auto ok = [&credentials](UserID id) { return id == credentials->uid() || id == credentials->euid() || id == credentials->suid(); };
        if (!ok(new_ruid) || !ok(new_euid))
            return EPERM;

        if (new_ruid < (uid_t)-1 || new_euid < (uid_t)-1)
            return EINVAL;

        auto new_credentials = TRY(Credentials::create(
            new_ruid,
            credentials->gid(),
            new_euid,
            credentials->egid(),
            credentials->suid(),
            credentials->sgid(),
            credentials->extra_gids(),
            credentials->sid(),
            credentials->pgid()));

        if (credentials->euid() != new_euid)
            protected_data.dumpable = false;

        protected_data.credentials = move(new_credentials);
        return 0;
    });
}

ErrorOr<FlatPtr> Process::sys$setresuid(UserID new_ruid, UserID new_euid, UserID new_suid)
{
    VERIFY_NO_PROCESS_BIG_LOCK(this);
    TRY(require_promise(Pledge::id));

    return with_mutable_protected_data([&](auto& protected_data) -> ErrorOr<FlatPtr> {
        auto credentials = this->credentials();

        if (new_ruid == (uid_t)-1)
            new_ruid = credentials->uid();
        if (new_euid == (uid_t)-1)
            new_euid = credentials->euid();
        if (new_suid == (uid_t)-1)
            new_suid = credentials->suid();

        auto ok = [&credentials](UserID id) { return id == credentials->uid() || id == credentials->euid() || id == credentials->suid(); };
        if ((!ok(new_ruid) || !ok(new_euid) || !ok(new_suid)) && !credentials->is_superuser())
            return EPERM;

        auto new_credentials = TRY(Credentials::create(
            new_ruid,
            credentials->gid(),
            new_euid,
            credentials->egid(),
            new_suid,
            credentials->sgid(),
            credentials->extra_gids(),
            credentials->sid(),
            credentials->pgid()));

        if (credentials->euid() != new_euid)
            protected_data.dumpable = false;

        protected_data.credentials = move(new_credentials);
        return 0;
    });
}

ErrorOr<FlatPtr> Process::sys$setregid(GroupID new_rgid, GroupID new_egid)
{
    VERIFY_NO_PROCESS_BIG_LOCK(this);
    TRY(require_promise(Pledge::id));

    return with_mutable_protected_data([&](auto& protected_data) -> ErrorOr<FlatPtr> {
        auto credentials = this->credentials();

        if (new_rgid == (gid_t)-1)
            new_rgid = credentials->gid();
        if (new_egid == (gid_t)-1)
            new_egid = credentials->egid();

        auto ok = [&credentials](GroupID id) { return id == credentials->gid() || id == credentials->egid() || id == credentials->sgid(); };
        if (!ok(new_rgid) || !ok(new_egid))
            return EPERM;

        auto new_credentials = TRY(Credentials::create(
            credentials->uid(),
            new_rgid,
            credentials->euid(),
            new_egid,
            credentials->suid(),
            credentials->sgid(),
            credentials->extra_gids(),
            credentials->sid(),
            credentials->pgid()));

        if (credentials->egid() != new_egid)
            protected_data.dumpable = false;

        protected_data.credentials = move(new_credentials);
        return 0;
    });
}

ErrorOr<FlatPtr> Process::sys$setresgid(GroupID new_rgid, GroupID new_egid, GroupID new_sgid)
{
    VERIFY_NO_PROCESS_BIG_LOCK(this);
    TRY(require_promise(Pledge::id));

    return with_mutable_protected_data([&](auto& protected_data) -> ErrorOr<FlatPtr> {
        auto credentials = this->credentials();

        if (new_rgid == (gid_t)-1)
            new_rgid = credentials->gid();
        if (new_egid == (gid_t)-1)
            new_egid = credentials->egid();
        if (new_sgid == (gid_t)-1)
            new_sgid = credentials->sgid();

        auto ok = [&credentials](GroupID id) { return id == credentials->gid() || id == credentials->egid() || id == credentials->sgid(); };
        if ((!ok(new_rgid) || !ok(new_egid) || !ok(new_sgid)) && !credentials->is_superuser())
            return EPERM;

        auto new_credentials = TRY(Credentials::create(
            credentials->uid(),
            new_rgid,
            credentials->euid(),
            new_egid,
            credentials->suid(),
            new_sgid,
            credentials->extra_gids(),
            credentials->sid(),
            credentials->pgid()));

        if (credentials->egid() != new_egid)
            protected_data.dumpable = false;

        protected_data.credentials = move(new_credentials);
        return 0;
    });
}

ErrorOr<FlatPtr> Process::sys$setgroups(size_t count, Userspace<GroupID const*> user_gids)
{
    VERIFY_NO_PROCESS_BIG_LOCK(this);
    TRY(require_promise(Pledge::id));

    if (count > NGROUPS_MAX)
        return EINVAL;

    return with_mutable_protected_data([&](auto& protected_data) -> ErrorOr<FlatPtr> {
        auto credentials = this->credentials();

        if (!credentials->is_superuser())
            return EPERM;

        if (!count) {
            protected_data.credentials = TRY(Credentials::create(
                credentials->uid(),
                credentials->gid(),
                credentials->euid(),
                credentials->egid(),
                credentials->suid(),
                credentials->sgid(),
                {},
                credentials->sid(),
                credentials->pgid()));
            return 0;
        }

        Vector<GroupID> new_extra_gids;
        TRY(new_extra_gids.try_resize(count));
        TRY(copy_n_from_user(new_extra_gids.data(), user_gids, count));

        HashTable<GroupID> unique_extra_gids;
        for (auto& extra_gid : new_extra_gids) {
            if (extra_gid != credentials->gid())
                TRY(unique_extra_gids.try_set(extra_gid));
        }

        new_extra_gids.clear_with_capacity();
        for (auto extra_gid : unique_extra_gids) {
            TRY(new_extra_gids.try_append(extra_gid));
        }

        protected_data.credentials = TRY(Credentials::create(
            credentials->uid(),
            credentials->gid(),
            credentials->euid(),
            credentials->egid(),
            credentials->suid(),
            credentials->sgid(),
            new_extra_gids.span(),
            credentials->sid(),
            credentials->pgid()));
        return 0;
    });
}

}
