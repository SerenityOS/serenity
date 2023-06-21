/*
 * Copyright (c) 2022-2023, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/IntrusiveList.h>
#include <AK/Singleton.h>
#include <Kernel/API/Jail.h>
#include <Kernel/Security/Jail.h>
#include <Kernel/Tasks/Process.h>

namespace Kernel {

static Atomic<u64> s_jail_id;
static Singleton<SpinlockProtected<Jail::List, LockRank::None>> s_all_instances {};

static JailIndex generate_jail_id()
{
    return s_jail_id.fetch_add(1);
}

RefPtr<ProcessList> Jail::process_list()
{
    return m_process_list;
}

ErrorOr<NonnullRefPtr<Jail>> Jail::create(NonnullOwnPtr<KString> name, unsigned flags)
{
    RefPtr<ProcessList> jail_process_list;
    if (flags & static_cast<unsigned>(JailIsolationFlags::PIDIsolation))
        jail_process_list = TRY(ProcessList::create());

    return s_all_instances->with([&](auto& list) -> ErrorOr<NonnullRefPtr<Jail>> {
        auto jail = TRY(adopt_nonnull_ref_or_enomem(new (nothrow) Jail(move(name), generate_jail_id(), jail_process_list)));
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

RefPtr<Jail> Jail::find_by_index(JailIndex index)
{
    return s_all_instances->with([&](auto& list) -> RefPtr<Jail> {
        for (auto& jail : list) {
            if (jail.index() == index)
                return jail;
        }
        return {};
    });
}

Jail::Jail(NonnullOwnPtr<KString> name, JailIndex index, RefPtr<ProcessList> process_list)
    : m_name(move(name))
    , m_index(index)
    , m_process_list(process_list)
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
