/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/DistinctNumeric.h>
#include <AK/Error.h>
#include <AK/IntrusiveList.h>
#include <AK/OwnPtr.h>
#include <AK/RefPtr.h>
#include <AK/Try.h>
#include <AK/Types.h>
#include <Kernel/KString.h>
#include <Kernel/Library/LockRefPtr.h>
#include <Kernel/Locking/SpinlockProtected.h>
#include <Kernel/Process.h>

namespace Kernel {

class JailManagement;

AK_TYPEDEF_DISTINCT_ORDERED_ID(u64, JailIndex);

class Jail : public RefCounted<Jail> {
    friend class JailManagement;

public:
    static ErrorOr<NonnullLockRefPtr<Jail>> create(Badge<JailManagement>, NonnullOwnPtr<KString>, JailIndex);

    StringView name() const { return m_name->view(); }
    JailIndex index() const { return m_index; }

    void detach(Badge<Process>);
    SpinlockProtected<size_t, LockRank::None>& attach_count() { return m_attach_count; }

private:
    Jail(NonnullOwnPtr<KString>, JailIndex);

    NonnullOwnPtr<KString> m_name;
    JailIndex const m_index;

    IntrusiveListNode<Jail, NonnullLockRefPtr<Jail>> m_jail_list_node;
    SpinlockProtected<size_t, LockRank::None> m_attach_count { 0 };
};

}
