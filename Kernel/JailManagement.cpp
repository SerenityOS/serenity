/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Singleton.h>
#include <Kernel/JailManagement.h>
#include <Kernel/Process.h>
#include <Kernel/Sections.h>

namespace Kernel {

static Singleton<JailManagement> s_the;
static Atomic<u64> s_jail_id;

UNMAP_AFTER_INIT JailManagement::JailManagement() = default;

JailIndex JailManagement::generate_jail_id()
{
    return s_jail_id.fetch_add(1);
}

JailManagement& JailManagement::the()
{
    return *s_the;
}

LockRefPtr<Jail> JailManagement::find_jail_by_index(JailIndex index)
{
    return m_jails.with([&](auto& list) -> LockRefPtr<Jail> {
        for (auto& jail : list) {
            if (jail.index() == index)
                return jail;
        }
        return {};
    });
}

ErrorOr<void> JailManagement::for_each_in_same_jail(Function<ErrorOr<void>(Jail&)> callback)
{
    return Process::current().jail().with([&](auto const& my_jail) -> ErrorOr<void> {
        // Note: If we are in a jail, don't reveal anything about the outside world,
        // not even the fact that we are in which jail...
        if (my_jail)
            return {};
        return m_jails.with([&](auto& list) -> ErrorOr<void> {
            for (auto& jail : list) {
                TRY(callback(jail));
            }
            return {};
        });
    });
}

LockRefPtr<Jail> JailManagement::find_first_jail_by_name(StringView name)
{
    return m_jails.with([&](auto& list) -> LockRefPtr<Jail> {
        for (auto& jail : list) {
            if (jail.name() == name)
                return jail;
        }
        return {};
    });
}

ErrorOr<NonnullLockRefPtr<Jail>> JailManagement::create_jail(NonnullOwnPtr<KString> name)
{
    return m_jails.with([&](auto& list) -> ErrorOr<NonnullLockRefPtr<Jail>> {
        auto jail = TRY(Jail::create({}, move(name), generate_jail_id()));
        list.append(jail);
        return jail;
    });
}

}
