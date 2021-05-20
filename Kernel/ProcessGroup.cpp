/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ProcessGroup.h"

namespace Kernel {

RecursiveSpinLock g_process_groups_lock;
InlineLinkedList<ProcessGroup>* g_process_groups;

ProcessGroup::~ProcessGroup()
{
    ScopedSpinLock lock(g_process_groups_lock);
    if (m_next || m_prev) {
        g_process_groups->remove(this);
    }
}

RefPtr<ProcessGroup> ProcessGroup::create(ProcessGroupID pgid)
{
    auto process_group = adopt_ref_if_nonnull(new ProcessGroup(pgid));
    if (process_group) {
        ScopedSpinLock lock(g_process_groups_lock);
        g_process_groups->prepend(process_group);
    }

    return process_group;
}

RefPtr<ProcessGroup> ProcessGroup::find_or_create(ProcessGroupID pgid)
{
    // Avoid allocating under spinlock, this compromises by wasting the
    // allocation when we race with another process to see if they have
    // already created a process group.
    auto process_group = adopt_ref_if_nonnull(new ProcessGroup(pgid));
    ScopedSpinLock lock(g_process_groups_lock);

    if (auto existing = from_pgid_nolock(pgid))
        return existing.release_nonnull();

    if (!process_group)
        return {};

    g_process_groups->prepend(process_group);
    return process_group;
}

RefPtr<ProcessGroup> ProcessGroup::from_pgid(ProcessGroupID pgid)
{
    ScopedSpinLock lock(g_process_groups_lock);
    return from_pgid_nolock(pgid);
}

RefPtr<ProcessGroup> ProcessGroup::from_pgid_nolock(ProcessGroupID pgid)
{
    VERIFY(g_process_groups_lock.own_lock());
    for (auto& group : *g_process_groups) {
        if (group.pgid() == pgid)
            return &group;
    }
    return nullptr;
}

}
