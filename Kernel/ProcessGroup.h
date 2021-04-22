/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/InlineLinkedList.h>
#include <AK/RefCounted.h>
#include <AK/Weakable.h>
#include <Kernel/Lock.h>
#include <Kernel/SpinLock.h>
#include <Kernel/UnixTypes.h>

namespace Kernel {

class ProcessGroup
    : public RefCounted<ProcessGroup>
    , public Weakable<ProcessGroup>
    , public InlineLinkedListNode<ProcessGroup> {

    AK_MAKE_NONMOVABLE(ProcessGroup);
    AK_MAKE_NONCOPYABLE(ProcessGroup);

    friend InlineLinkedListNode<ProcessGroup>;

public:
    ~ProcessGroup();

    static NonnullRefPtr<ProcessGroup> create(ProcessGroupID);
    static NonnullRefPtr<ProcessGroup> find_or_create(ProcessGroupID);
    static RefPtr<ProcessGroup> from_pgid(ProcessGroupID);

    const ProcessGroupID& pgid() const { return m_pgid; }

private:
    ProcessGroup(ProcessGroupID pgid)
        : m_pgid(pgid)
    {
    }

    ProcessGroup* m_prev { nullptr };
    ProcessGroup* m_next { nullptr };

    ProcessGroupID m_pgid;
};

extern InlineLinkedList<ProcessGroup>* g_process_groups;
extern RecursiveSpinLock g_process_groups_lock;

}
