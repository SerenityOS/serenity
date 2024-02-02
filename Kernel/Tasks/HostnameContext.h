/*
 * Copyright (c) 2024, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/AtomicRefCounted.h>
#include <AK/FixedStringBuffer.h>
#include <AK/IntrusiveList.h>
#include <AK/IntrusiveListRelaxedConst.h>
#include <AK/Noncopyable.h>
#include <AK/RefPtr.h>
#include <Kernel/API/POSIX/sys/utsname.h>
#include <Kernel/Forward.h>
#include <Kernel/Locking/SpinlockProtected.h>

namespace Kernel {

class HostnameContext : public AtomicRefCounted<HostnameContext> {
    AK_MAKE_NONCOPYABLE(HostnameContext);
    AK_MAKE_NONMOVABLE(HostnameContext);
    friend class Process;

public:
    AK_TYPEDEF_DISTINCT_ORDERED_ID(u64, IndexID);

    static ErrorOr<NonnullRefPtr<HostnameContext>> create_initial();
    static ErrorOr<NonnullRefPtr<HostnameContext>> create_with_name(StringView name);

    static ErrorOr<NonnullRefPtr<HostnameContext>> hostname_context_for_id(int id);

    SpinlockProtected<FixedStringBuffer<UTSNAME_ENTRY_LEN - 1>, LockRank::None>& buffer() { return m_buffer; }
    SpinlockProtected<FixedStringBuffer<UTSNAME_ENTRY_LEN - 1>, LockRank::None> const& buffer() const { return m_buffer; }

    IndexID id() const { return m_id; }

    void detach(Badge<Process>);
    void set_attached(Badge<Process>);

private:
    HostnameContext(StringView name);

    IntrusiveListNode<HostnameContext, NonnullRefPtr<HostnameContext>> m_list_node;

    SpinlockProtected<size_t, LockRank::None> m_attach_count { 0 };
    SpinlockProtected<FixedStringBuffer<UTSNAME_ENTRY_LEN - 1>, LockRank::None> m_buffer;

    IndexID m_id;

public:
    using List = IntrusiveListRelaxedConst<&HostnameContext::m_list_node>;
};

}
