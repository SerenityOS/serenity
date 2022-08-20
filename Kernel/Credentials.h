/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/AtomicRefCounted.h>
#include <AK/FixedArray.h>
#include <Kernel/Forward.h>

namespace Kernel {

class Credentials final : public AtomicRefCounted<Credentials> {
public:
    static ErrorOr<NonnullRefPtr<Credentials>> create(UserID uid, GroupID gid, UserID euid, GroupID egid, UserID suid, GroupID sgid, Span<GroupID const> extra_gids);
    ~Credentials();

    bool is_superuser() const { return euid() == 0; }

    UserID euid() const { return m_euid; }
    GroupID egid() const { return m_egid; }
    UserID uid() const { return m_uid; }
    GroupID gid() const { return m_gid; }
    UserID suid() const { return m_suid; }
    GroupID sgid() const { return m_sgid; }
    Span<GroupID const> extra_gids() const { return m_extra_gids.span(); }

private:
    Credentials(UserID uid, GroupID gid, UserID euid, GroupID egid, UserID suid, GroupID sgid, FixedArray<GroupID> extra_gids);

    UserID m_uid;
    GroupID m_gid;
    UserID m_euid;
    GroupID m_egid;
    UserID m_suid;
    GroupID m_sgid;
    FixedArray<GroupID> m_extra_gids;
};

}
