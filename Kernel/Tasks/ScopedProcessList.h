/*
 * Copyright (c) 2023-2024, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/AtomicRefCounted.h>
#include <AK/IntrusiveList.h>
#include <AK/RefPtr.h>
#include <Kernel/Forward.h>
#include <Kernel/Locking/SpinlockProtected.h>
#include <Kernel/Tasks/Process.h>

namespace Kernel {

class ScopedProcessList : public AtomicRefCounted<ScopedProcessList> {
public:
    AK_TYPEDEF_DISTINCT_ORDERED_ID(u64, IndexID);

    static ErrorOr<NonnullRefPtr<ScopedProcessList>> create();

    static ErrorOr<NonnullRefPtr<ScopedProcessList>> scoped_process_list_for_id(int id);

    void detach(Badge<Process>);
    void attach(Process&);

    IndexID id() const { return m_id; }
    SpinlockProtected<IntrusiveListRelaxedConst<&Process::m_scoped_process_list_node>, LockRank::None>& attached_processes() { return m_attached_processes; }
    SpinlockProtected<IntrusiveListRelaxedConst<&Process::m_scoped_process_list_node>, LockRank::None> const& attached_processes() const { return m_attached_processes; }

private:
    ScopedProcessList();

    SpinlockProtected<IntrusiveListRelaxedConst<&Process::m_scoped_process_list_node>, LockRank::None> m_attached_processes;
    IndexID m_id;

    SpinlockProtected<size_t, LockRank::None> m_attach_count { 0 };
    IntrusiveListNode<ScopedProcessList, NonnullRefPtr<ScopedProcessList>> m_list_node;

public:
    using List = IntrusiveListRelaxedConst<&ScopedProcessList::m_list_node>;
};

}
