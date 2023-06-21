/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Tasks/Process.h>

namespace Kernel {

ErrorOr<FlatPtr> Process::sys$getuid()
{
    VERIFY_NO_PROCESS_BIG_LOCK(this);
    TRY(require_promise(Pledge::stdio));
    auto credentials = this->credentials();
    return credentials->uid().value();
}

ErrorOr<FlatPtr> Process::sys$getgid()
{
    VERIFY_NO_PROCESS_BIG_LOCK(this);
    TRY(require_promise(Pledge::stdio));
    auto credentials = this->credentials();
    return credentials->gid().value();
}

ErrorOr<FlatPtr> Process::sys$geteuid()
{
    VERIFY_NO_PROCESS_BIG_LOCK(this);
    TRY(require_promise(Pledge::stdio));
    auto credentials = this->credentials();
    return credentials->euid().value();
}

ErrorOr<FlatPtr> Process::sys$getegid()
{
    VERIFY_NO_PROCESS_BIG_LOCK(this);
    TRY(require_promise(Pledge::stdio));
    auto credentials = this->credentials();
    return credentials->egid().value();
}

ErrorOr<FlatPtr> Process::sys$getresuid(Userspace<UserID*> user_ruid, Userspace<UserID*> user_euid, Userspace<UserID*> user_suid)
{
    VERIFY_NO_PROCESS_BIG_LOCK(this);
    TRY(require_promise(Pledge::stdio));

    auto credentials = this->credentials();
    auto uid = credentials->uid();
    auto euid = credentials->euid();
    auto suid = credentials->suid();

    TRY(copy_to_user(user_ruid, &uid));
    TRY(copy_to_user(user_euid, &euid));
    TRY(copy_to_user(user_suid, &suid));
    return 0;
}

ErrorOr<FlatPtr> Process::sys$getresgid(Userspace<GroupID*> user_rgid, Userspace<GroupID*> user_egid, Userspace<GroupID*> user_sgid)
{
    VERIFY_NO_PROCESS_BIG_LOCK(this);
    TRY(require_promise(Pledge::stdio));

    auto credentials = this->credentials();
    auto gid = credentials->gid();
    auto egid = credentials->egid();
    auto sgid = credentials->sgid();

    TRY(copy_to_user(user_rgid, &gid));
    TRY(copy_to_user(user_egid, &egid));
    TRY(copy_to_user(user_sgid, &sgid));
    return 0;
}

ErrorOr<FlatPtr> Process::sys$getgroups(size_t count, Userspace<GroupID*> user_gids)
{
    VERIFY_NO_PROCESS_BIG_LOCK(this);
    TRY(require_promise(Pledge::stdio));

    auto credentials = this->credentials();

    if (!count)
        return credentials->extra_gids().size();
    if (count != credentials->extra_gids().size())
        return EINVAL;
    TRY(copy_n_to_user(user_gids, credentials->extra_gids().data(), count));
    return 0;
}

}
