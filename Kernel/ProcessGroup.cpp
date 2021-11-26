/*
 * Copyright (c) 2020, the SerenityOS developers.
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Singleton.h>
#include <Kernel/ProcessGroup.h>

namespace Kernel {

static Singleton<SpinlockProtected<ProcessGroup::List>> s_process_groups;

SpinlockProtected<ProcessGroup::List>& process_groups()
{
    return *s_process_groups;
}

ProcessGroup::~ProcessGroup()
{
    process_groups().with([&](auto& groups) {
        groups.remove(*this);
    });
}

ErrorOr<NonnullRefPtr<ProcessGroup>> ProcessGroup::try_create(ProcessGroupID pgid)
{
    auto process_group = TRY(adopt_nonnull_ref_or_enomem(new (nothrow) ProcessGroup(pgid)));
    process_groups().with([&](auto& groups) {
        groups.prepend(*process_group);
    });
    return process_group;
}

ErrorOr<NonnullRefPtr<ProcessGroup>> ProcessGroup::try_find_or_create(ProcessGroupID pgid)
{
    return process_groups().with([&](auto& groups) -> ErrorOr<NonnullRefPtr<ProcessGroup>> {
        for (auto& group : groups) {
            if (group.pgid() == pgid)
                return group;
        }
        auto process_group = TRY(adopt_nonnull_ref_or_enomem(new (nothrow) ProcessGroup(pgid)));
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
