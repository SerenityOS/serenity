/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <YAK/IntrusiveList.h>
#include <YAK/RefCounted.h>
#include <YAK/Weakable.h>
#include <Kernel/Locking/SpinlockProtected.h>
#include <Kernel/UnixTypes.h>

namespace Kernel {

class ProcessGroup
    : public RefCounted<ProcessGroup>
    , public Weakable<ProcessGroup> {

    YAK_MAKE_NONMOVABLE(ProcessGroup);
    YAK_MAKE_NONCOPYABLE(ProcessGroup);

public:
    ~ProcessGroup();

    static RefPtr<ProcessGroup> create(ProcessGroupID);
    static RefPtr<ProcessGroup> find_or_create(ProcessGroupID);
    static RefPtr<ProcessGroup> from_pgid(ProcessGroupID);

    const ProcessGroupID& pgid() const { return m_pgid; }

private:
    ProcessGroup(ProcessGroupID pgid)
        : m_pgid(pgid)
    {
    }

    IntrusiveListNode<ProcessGroup> m_list_node;
    ProcessGroupID m_pgid;

public:
    using List = IntrusiveList<ProcessGroup, RawPtr<ProcessGroup>, &ProcessGroup::m_list_node>;
};

SpinlockProtected<ProcessGroup::List>& process_groups();

}
