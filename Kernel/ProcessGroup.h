/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/IntrusiveList.h>
#include <AK/RefCounted.h>
#include <AK/Weakable.h>
#include <Kernel/Lock.h>
#include <Kernel/SpinLock.h>
#include <Kernel/UnixTypes.h>

namespace Kernel {

class ProcessGroup
    : public RefCounted<ProcessGroup>
    , public Weakable<ProcessGroup> {

    AK_MAKE_NONMOVABLE(ProcessGroup);
    AK_MAKE_NONCOPYABLE(ProcessGroup);

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

extern ProcessGroup::List* g_process_groups;
extern RecursiveSpinLock g_process_groups_lock;

}
