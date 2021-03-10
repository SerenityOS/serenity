/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
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
    MutableProtectedData(*this)->euid = new_euid;
    return 0;
}

KResultOr<int> Process::sys$setegid(gid_t new_egid)
{
    REQUIRE_PROMISE(id);

    if (new_egid != gid() && new_egid != sgid() && !is_superuser())
        return EPERM;

    if (egid() != new_egid)
        set_dumpable(false);

    MutableProtectedData(*this)->egid = new_egid;
    return 0;
}

KResultOr<int> Process::sys$setuid(uid_t new_uid)
{
    REQUIRE_PROMISE(id);

    if (new_uid != uid() && new_uid != euid() && !is_superuser())
        return EPERM;

    if (euid() != new_uid)
        set_dumpable(false);

    MutableProtectedData protected_data { *this };
    protected_data->uid = new_uid;
    protected_data->euid = new_uid;
    protected_data->suid = new_uid;
    return 0;
}

KResultOr<int> Process::sys$setgid(gid_t new_gid)
{
    REQUIRE_PROMISE(id);

    if (new_gid != gid() && new_gid != egid() && !is_superuser())
        return EPERM;

    if (egid() != new_gid)
        set_dumpable(false);

    MutableProtectedData protected_data { *this };
    protected_data->gid = new_gid;
    protected_data->egid = new_gid;
    protected_data->sgid = new_gid;
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

    MutableProtectedData protected_data { *this };
    protected_data->uid = new_ruid;
    protected_data->euid = new_euid;
    protected_data->suid = new_suid;
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

    MutableProtectedData protected_data { *this };
    protected_data->gid = new_rgid;
    protected_data->egid = new_egid;
    protected_data->sgid = new_sgid;
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
        m_extra_gids.clear();
        return 0;
    }

    Vector<gid_t> new_extra_gids;
    new_extra_gids.resize(count);
    if (!copy_n_from_user(new_extra_gids.data(), user_gids, count))
        return EFAULT;

    HashTable<gid_t> unique_extra_gids;
    for (auto& extra_gid : new_extra_gids) {
        if (extra_gid != gid())
            unique_extra_gids.set(extra_gid);
    }

    m_extra_gids.resize(unique_extra_gids.size());
    size_t i = 0;
    for (auto& extra_gid : unique_extra_gids) {
        if (extra_gid == gid())
            continue;
        m_extra_gids[i++] = extra_gid;
    }
    return 0;
}

}
