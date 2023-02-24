/*
 * Copyright (c) 2020-2023, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/IntrusiveList.h>
#include <AK/RefPtr.h>
#include <Kernel/Forward.h>
#include <Kernel/Library/ListedRefCounted.h>
#include <Kernel/Library/LockWeakable.h>
#include <Kernel/Locking/SpinlockProtected.h>
#include <Kernel/UnixTypes.h>

namespace Kernel {

class ProcessGroup
    : public ListedRefCounted<ProcessGroup, LockType::Spinlock>
    , public LockWeakable<ProcessGroup> {

    AK_MAKE_NONMOVABLE(ProcessGroup);
    AK_MAKE_NONCOPYABLE(ProcessGroup);

public:
    ~ProcessGroup();

    static ErrorOr<NonnullRefPtr<ProcessGroup>> create_if_unused_pgid(ProcessGroupID);
    static ErrorOr<NonnullRefPtr<ProcessGroup>> find_or_create(ProcessGroupID);
    static RefPtr<ProcessGroup> from_pgid(ProcessGroupID);

    ProcessGroupID const& pgid() const { return m_pgid; }

private:
    ProcessGroup(ProcessGroupID pgid)
        : m_pgid(pgid)
    {
    }

    ProcessGroupID m_pgid;

    mutable IntrusiveListNode<ProcessGroup> m_list_node;

public:
    using AllInstancesList = IntrusiveList<&ProcessGroup::m_list_node>;
    static SpinlockProtected<AllInstancesList, LockRank::None>& all_instances();
};

}
