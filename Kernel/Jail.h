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
#include <Kernel/FileSystem/UnveilData.h>
#include <Kernel/KString.h>
#include <Kernel/Locking/SpinlockProtected.h>
#include <Kernel/Process.h>

namespace Kernel {

class ProcessList;

AK_TYPEDEF_DISTINCT_ORDERED_ID(u64, JailIndex);

class Jail : public AtomicRefCounted<Jail> {

public:
    RefPtr<ProcessList> process_list();
    SpinlockProtected<OwnPtr<UnveilData>, LockRank::None>& unveil_data();
    bool has_unveil_isolation_enforced() const { return m_has_unveil_isolation_enforced; }

    void unset_clean_on_last_detach()
    {
        m_clean_on_last_detach.with([](auto& flag) { flag = false; });
    }

    void set_clean_on_last_detach();

    static RefPtr<Jail> find_by_index(JailIndex);
    static ErrorOr<NonnullRefPtr<Jail>> create(NonnullOwnPtr<KString> name, unsigned flags);
    static ErrorOr<void> for_each_when_process_is_not_jailed(Function<ErrorOr<void>(Jail const&)> callback);

    StringView name() const { return m_name->view(); }
    JailIndex index() const { return m_index; }

    void detach(Badge<Process>);
    SpinlockProtected<size_t, LockRank::None>& attach_count() { return m_attach_count; }

private:
    Jail(NonnullOwnPtr<KString>, JailIndex, RefPtr<ProcessList>, OwnPtr<UnveilData>);

    NonnullOwnPtr<KString> m_name;
    JailIndex const m_index;

    IntrusiveListNode<Jail, NonnullRefPtr<Jail>> m_list_node;

public:
    using List = IntrusiveListRelaxedConst<&Jail::m_list_node>;

private:
    RefPtr<ProcessList> const m_process_list;
    // NOTE: We actually use a SpinlockProtected because the unveil data
    // can be changed, but the pointer should be set in the construction
    // point only.
    bool const m_has_unveil_isolation_enforced { false };
    SpinlockProtected<bool, LockRank::None> m_clean_on_last_detach { true };
    SpinlockProtected<OwnPtr<UnveilData>, LockRank::None> m_unveil_data;

    SpinlockProtected<size_t, LockRank::None> m_attach_count { 0 };
};

}
