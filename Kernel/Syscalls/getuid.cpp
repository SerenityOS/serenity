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

uid_t Process::sys$getuid()
{
    REQUIRE_PROMISE(stdio);
    return m_uid;
}

gid_t Process::sys$getgid()
{
    REQUIRE_PROMISE(stdio);
    return m_gid;
}

uid_t Process::sys$geteuid()
{
    REQUIRE_PROMISE(stdio);
    return m_euid;
}

gid_t Process::sys$getegid()
{
    REQUIRE_PROMISE(stdio);
    return m_egid;
}

int Process::sys$getresuid(uid_t* ruid, uid_t* euid, uid_t* suid)
{
    REQUIRE_PROMISE(stdio);
    if (!validate_write_typed(ruid) || !validate_write_typed(euid) || !validate_write_typed(suid))
        return -EFAULT;
    copy_to_user(ruid, &m_uid);
    copy_to_user(euid, &m_euid);
    copy_to_user(suid, &m_suid);
    return 0;
}

int Process::sys$getresgid(gid_t* rgid, gid_t* egid, gid_t* sgid)
{
    REQUIRE_PROMISE(stdio);
    if (!validate_write_typed(rgid) || !validate_write_typed(egid) || !validate_write_typed(sgid))
        return -EFAULT;
    copy_to_user(rgid, &m_gid);
    copy_to_user(egid, &m_egid);
    copy_to_user(sgid, &m_sgid);
    return 0;
}

int Process::sys$getgroups(ssize_t count, gid_t* user_gids)
{
    REQUIRE_PROMISE(stdio);
    if (count < 0)
        return -EINVAL;
    if (!count)
        return m_extra_gids.size();
    if (count != (int)m_extra_gids.size())
        return -EINVAL;
    if (!validate_write_typed(user_gids, m_extra_gids.size()))
        return -EFAULT;

    Vector<gid_t> gids;
    for (auto gid : m_extra_gids)
        gids.append(gid);

    copy_to_user(user_gids, gids.data(), sizeof(gid_t) * count);
    return 0;
}

}
