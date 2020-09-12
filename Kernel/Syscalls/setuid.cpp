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

int Process::sys$seteuid(uid_t euid)
{
    REQUIRE_PROMISE(id);

    if (euid != m_uid && euid != m_suid && !is_superuser())
        return -EPERM;

    m_euid = euid;
    return 0;
}

int Process::sys$setegid(gid_t egid)
{
    REQUIRE_PROMISE(id);

    if (egid != m_gid && egid != m_sgid && !is_superuser())
        return -EPERM;

    m_egid = egid;
    return 0;
}

int Process::sys$setuid(uid_t uid)
{
    REQUIRE_PROMISE(id);

    if (uid != m_uid && uid != m_euid && !is_superuser())
        return -EPERM;

    m_uid = uid;
    m_euid = uid;
    m_suid = uid;
    return 0;
}

int Process::sys$setgid(gid_t gid)
{
    REQUIRE_PROMISE(id);

    if (gid != m_gid && gid != m_egid && !is_superuser())
        return -EPERM;

    m_gid = gid;
    m_egid = gid;
    m_sgid = gid;
    return 0;
}

int Process::sys$setresuid(uid_t ruid, uid_t euid, uid_t suid)
{
    REQUIRE_PROMISE(id);

    if (ruid == (uid_t)-1)
        ruid = m_uid;
    if (euid == (uid_t)-1)
        euid = m_euid;
    if (suid == (uid_t)-1)
        suid = m_suid;

    auto ok = [this](uid_t id) { return id == m_uid || id == m_euid || id == m_suid; };
    if ((!ok(ruid) || !ok(euid) || !ok(suid)) && !is_superuser())
        return -EPERM;

    m_uid = ruid;
    m_euid = euid;
    m_suid = suid;
    return 0;
}

int Process::sys$setresgid(gid_t rgid, gid_t egid, gid_t sgid)
{
    REQUIRE_PROMISE(id);

    if (rgid == (gid_t)-1)
        rgid = m_gid;
    if (egid == (gid_t)-1)
        egid = m_egid;
    if (sgid == (gid_t)-1)
        sgid = m_sgid;

    auto ok = [this](gid_t id) { return id == m_gid || id == m_egid || id == m_sgid; };
    if ((!ok(rgid) || !ok(egid) || !ok(sgid)) && !is_superuser())
        return -EPERM;

    m_gid = rgid;
    m_egid = egid;
    m_sgid = sgid;
    return 0;
}

int Process::sys$setgroups(ssize_t count, Userspace<const gid_t*> user_gids)
{
    REQUIRE_PROMISE(id);
    if (count < 0)
        return -EINVAL;
    if (!is_superuser())
        return -EPERM;

    if (!count) {
        m_extra_gids.clear();
        return 0;
    }

    Vector<gid_t> gids;
    gids.resize(count);
    if (!copy_from_user(gids.data(), user_gids.unsafe_userspace_ptr(), sizeof(gid_t) * count))
        return -EFAULT;

    HashTable<gid_t> unique_extra_gids;
    for (auto& gid : gids) {
        if (gid != m_gid)
            unique_extra_gids.set(gid);
    }

    m_extra_gids.resize(unique_extra_gids.size());
    size_t i = 0;
    for (auto& gid : unique_extra_gids) {
        if (gid == m_gid)
            continue;
        m_extra_gids[i++] = gid;
    }
    return 0;
}

}
