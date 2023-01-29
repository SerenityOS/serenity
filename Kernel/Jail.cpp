/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Jail.h>

namespace Kernel {

ErrorOr<NonnullLockRefPtr<Jail>> Jail::create(Badge<JailManagement>, NonnullOwnPtr<KString> name, JailIndex index)
{
    return adopt_nonnull_lock_ref_or_enomem(new (nothrow) Jail(move(name), index));
}

Jail::Jail(NonnullOwnPtr<KString> name, JailIndex index)
    : m_name(move(name))
    , m_index(index)
{
}

void Jail::detach(Badge<Process>)
{
    VERIFY(ref_count() > 0);
    m_attach_count.with([&](auto& my_attach_count) {
        VERIFY(my_attach_count > 0);
        my_attach_count--;
        if (my_attach_count == 0) {
            m_jail_list_node.remove();
        }
    });
}

}
