/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/NonnullRefPtr.h>
#include <AK/RefPtr.h>
#include <Kernel/Credentials.h>

namespace Kernel {

ErrorOr<NonnullRefPtr<Credentials>> Credentials::create(UserID uid, GroupID gid, UserID euid, GroupID egid, UserID suid, GroupID sgid, Span<GroupID const> extra_gids)
{
    auto extra_gids_array = TRY(FixedArray<GroupID>::try_create(extra_gids));
    return adopt_nonnull_ref_or_enomem(new (nothrow) Credentials(uid, gid, euid, egid, suid, sgid, move(extra_gids_array)));
}

Credentials::Credentials(UserID uid, GroupID gid, UserID euid, GroupID egid, UserID suid, GroupID sgid, FixedArray<GroupID> extra_gids)
    : m_uid(uid)
    , m_gid(gid)
    , m_euid(euid)
    , m_egid(egid)
    , m_suid(suid)
    , m_sgid(sgid)
    , m_extra_gids(move(extra_gids))
{
}

Credentials::~Credentials() = default;

bool Credentials::in_group(Kernel::GroupID gid) const
{
    return m_gid == gid || m_extra_gids.contains_slow(gid);
}

}
