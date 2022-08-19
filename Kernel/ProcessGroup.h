/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/AtomicRefCounted.h>
#include <AK/IntrusiveList.h>
#include <Kernel/Library/LockWeakable.h>
#include <Kernel/Locking/SpinlockProtected.h>
#include <Kernel/UnixTypes.h>

namespace Kernel {

class ProcessGroup
    : public AtomicRefCounted<ProcessGroup>
    , public LockWeakable<ProcessGroup> {

    AK_MAKE_NONMOVABLE(ProcessGroup);
    AK_MAKE_NONCOPYABLE(ProcessGroup);

public:
    ~ProcessGroup();

    static ErrorOr<NonnullLockRefPtr<ProcessGroup>> try_create(ProcessGroupID);
    static ErrorOr<NonnullLockRefPtr<ProcessGroup>> try_find_or_create(ProcessGroupID);
    static LockRefPtr<ProcessGroup> from_pgid(ProcessGroupID);

    ProcessGroupID const& pgid() const { return m_pgid; }

private:
    ProcessGroup(ProcessGroupID pgid)
        : m_pgid(pgid)
    {
    }

    IntrusiveListNode<ProcessGroup> m_list_node;
    ProcessGroupID m_pgid;

public:
    using List = IntrusiveList<&ProcessGroup::m_list_node>;
};

SpinlockProtected<ProcessGroup::List>& process_groups();

}
