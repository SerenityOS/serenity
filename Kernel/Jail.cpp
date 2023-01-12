/*
 * Copyright (c) 2022-2023, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/IntrusiveList.h>
#include <AK/Singleton.h>
#include <Kernel/Jail.h>
#include <Kernel/Process.h>

namespace Kernel {

static Atomic<u64> s_jail_id;
static Singleton<SpinlockProtected<Jail::List, LockRank::None>> s_all_instances {};

static JailIndex generate_jail_id()
{
    return s_jail_id.fetch_add(1);
}

NonnullRefPtr<ProcessList> Jail::process_list()
{
    return m_process_list;
}

ErrorOr<NonnullLockRefPtr<Jail>> Jail::create(NonnullOwnPtr<KString> name)
{
    return s_all_instances->with([&](auto& list) -> ErrorOr<NonnullLockRefPtr<Jail>> {
        auto process_list = TRY(ProcessList::create());
        auto jail = TRY(adopt_nonnull_lock_ref_or_enomem(new (nothrow) Jail(move(name), generate_jail_id(), move(process_list))));
        list.append(jail);
        return jail;
    });
}

ErrorOr<void> Jail::for_each_when_process_is_not_jailed(Function<ErrorOr<void>(Jail const&)> callback)
{
    return Process::current().jail().with([&](auto const& my_jail) -> ErrorOr<void> {
        // Note: If we are in a jail, don't reveal anything about the outside world,
        // not even the fact that we are in which jail...
        if (my_jail)
            return {};
        return s_all_instances->with([&](auto& list) -> ErrorOr<void> {
            for (auto& jail : list) {
                TRY(callback(jail));
            }
            return {};
        });
    });
}

LockRefPtr<Jail> Jail::find_by_index(JailIndex index)
{
    return s_all_instances->with([&](auto& list) -> LockRefPtr<Jail> {
        for (auto& jail : list) {
            if (jail.index() == index)
                return jail;
        }
        return {};
    });
}

Jail::Jail(NonnullOwnPtr<KString> name, JailIndex index, NonnullRefPtr<ProcessList> process_list)
    : m_name(move(name))
    , m_index(index)
    , m_process_list(move(process_list))
{
}

void Jail::detach(Badge<Process>)
{
    VERIFY(ref_count() > 0);
    m_attach_count.with([&](auto& my_attach_count) {
        VERIFY(my_attach_count > 0);
        my_attach_count--;
        if (my_attach_count == 0) {
            m_list_node.remove();
        }
    });
}

}
