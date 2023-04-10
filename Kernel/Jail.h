/*
 * Copyright (c) 2022-2023, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/AtomicRefCounted.h>
#include <AK/DistinctNumeric.h>
#include <AK/Error.h>
#include <AK/IntrusiveList.h>
#include <AK/IntrusiveListRelaxedConst.h>
#include <AK/OwnPtr.h>
#include <AK/RefPtr.h>
#include <AK/Try.h>
#include <AK/Types.h>
#include <Kernel/KString.h>
#include <Kernel/Library/LockRefPtr.h>
#include <Kernel/Locking/SpinlockProtected.h>
#include <Kernel/Process.h>

namespace Kernel {

class ProcessList;

AK_TYPEDEF_DISTINCT_ORDERED_ID(u64, JailIndex);

class Jail : public AtomicRefCounted<Jail> {

public:
    NonnullRefPtr<ProcessList> process_list();

    static LockRefPtr<Jail> find_by_index(JailIndex);
    static ErrorOr<NonnullLockRefPtr<Jail>> create(NonnullOwnPtr<KString> name);
    static ErrorOr<void> for_each_when_process_is_not_jailed(Function<ErrorOr<void>(Jail const&)> callback);

    StringView name() const { return m_name->view(); }
    JailIndex index() const { return m_index; }

    void detach(Badge<Process>);
    SpinlockProtected<size_t, LockRank::None>& attach_count() { return m_attach_count; }

private:
    Jail(NonnullOwnPtr<KString>, JailIndex, NonnullRefPtr<ProcessList>);

    NonnullOwnPtr<KString> m_name;
    JailIndex const m_index;

    IntrusiveListNode<Jail, NonnullLockRefPtr<Jail>> m_list_node;

public:
    using List = IntrusiveListRelaxedConst<&Jail::m_list_node>;

private:
    NonnullRefPtr<ProcessList> const m_process_list;

    SpinlockProtected<size_t, LockRank::None> m_attach_count { 0 };
};

}
