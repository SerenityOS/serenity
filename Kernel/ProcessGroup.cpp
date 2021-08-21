/*
 * Copyright (c) 2020, the SerenityOS developers.
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Singleton.h>
#include <Kernel/ProcessGroup.h>

namespace Kernel {

static Singleton<SpinLockProtected<ProcessGroup::List>> s_process_groups;

SpinLockProtected<ProcessGroup::List>& process_groups()
{
    return *s_process_groups;
}

ProcessGroup::~ProcessGroup()
{
    process_groups().with([&](auto& groups) {
        groups.remove(*this);
    });
}

RefPtr<ProcessGroup> ProcessGroup::create(ProcessGroupID pgid)
{
    auto process_group = adopt_ref_if_nonnull(new (nothrow) ProcessGroup(pgid));
    if (!process_group)
        return {};
    process_groups().with([&](auto& groups) {
        groups.prepend(*process_group);
    });
    return process_group;
}

RefPtr<ProcessGroup> ProcessGroup::find_or_create(ProcessGroupID pgid)
{
    return process_groups().with([&](auto& groups) -> RefPtr<ProcessGroup> {
        for (auto& group : groups) {
            if (group.pgid() == pgid)
                return &group;
        }
        auto process_group = adopt_ref_if_nonnull(new (nothrow) ProcessGroup(pgid));
        if (process_group)
            groups.prepend(*process_group);
        return process_group;
    });
}

RefPtr<ProcessGroup> ProcessGroup::from_pgid(ProcessGroupID pgid)
{
    return process_groups().with([&](auto& groups) -> RefPtr<ProcessGroup> {
        for (auto& group : groups) {
            if (group.pgid() == pgid)
                return &group;
        }
        return nullptr;
    });
}

}
