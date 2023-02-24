/*
 * Copyright (c) 2020, the SerenityOS developers.
 * Copyright (c) 2021-2023, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Singleton.h>
#include <Kernel/Tasks/ProcessGroup.h>

namespace Kernel {

static Singleton<SpinlockProtected<ProcessGroup::AllInstancesList, LockRank::None>> s_all_instances;

SpinlockProtected<ProcessGroup::AllInstancesList, LockRank::None>& ProcessGroup::all_instances()
{
    return s_all_instances;
}

ProcessGroup::~ProcessGroup() = default;

ErrorOr<NonnullRefPtr<ProcessGroup>> ProcessGroup::create_if_unused_pgid(ProcessGroupID pgid)
{
    return all_instances().with([&](auto& all_instances) -> ErrorOr<NonnullRefPtr<ProcessGroup>> {
        for (auto& process_group : all_instances) {
            if (process_group.pgid() == pgid)
                return EPERM;
        }
        auto process_group = TRY(adopt_nonnull_ref_or_enomem(new (nothrow) ProcessGroup(pgid)));
        all_instances.prepend(*process_group);
        return process_group;
    });
}

ErrorOr<NonnullRefPtr<ProcessGroup>> ProcessGroup::find_or_create(ProcessGroupID pgid)
{
    return all_instances().with([&](auto& all_instances) -> ErrorOr<NonnullRefPtr<ProcessGroup>> {
        for (auto& group : all_instances) {
            if (group.pgid() == pgid)
                return group;
        }
        auto process_group = TRY(adopt_nonnull_ref_or_enomem(new (nothrow) ProcessGroup(pgid)));
        all_instances.prepend(*process_group);
        return process_group;
    });
}

RefPtr<ProcessGroup> ProcessGroup::from_pgid(ProcessGroupID pgid)
{
    return all_instances().with([&](auto& groups) -> RefPtr<ProcessGroup> {
        for (auto& group : groups) {
            if (group.pgid() == pgid)
                return &group;
        }
        return nullptr;
    });
}

}
