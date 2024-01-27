/*
 * Copyright (c) 2023-2024, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Atomic.h>
#include <AK/Singleton.h>
#include <Kernel/Tasks/ScopedProcessList.h>

namespace Kernel {

static Atomic<u64> s_scoped_process_list_id = 0;
static Singleton<SpinlockProtected<ScopedProcessList::List, LockRank::None>> s_all_instances {};

ErrorOr<NonnullRefPtr<ScopedProcessList>> ScopedProcessList::scoped_process_list_for_id(int id)
{
    if (id < 0)
        return Error::from_errno(EINVAL);
    auto index = static_cast<IndexID>(id);
    return s_all_instances->with([&](auto& list) -> ErrorOr<NonnullRefPtr<ScopedProcessList>> {
        for (auto& scoped_process_list : list) {
            if (scoped_process_list.id() == index)
                return scoped_process_list;
        }
        return Error::from_errno(ESRCNOTFOUND);
    });
}

ErrorOr<NonnullRefPtr<ScopedProcessList>> ScopedProcessList::create()
{
    return s_all_instances->with([&](auto& list) -> ErrorOr<NonnullRefPtr<ScopedProcessList>> {
        auto scoped_process_list = TRY(adopt_nonnull_ref_or_enomem(new (nothrow) ScopedProcessList()));
        list.append(scoped_process_list);
        return scoped_process_list;
    });
}

ScopedProcessList::ScopedProcessList()
    : m_id(s_scoped_process_list_id.fetch_add(1))
{
}

void ScopedProcessList::attach(Process& process)
{
    m_attach_count.with([&](auto& my_attach_count) {
        m_attached_processes.with([&](auto& attached_processes_list) {
            attached_processes_list.append(process);
        });
        my_attach_count++;
        s_all_instances->with([&](auto& list) {
            // NOTE: It could happen that we have been detached from the
            // global list but a Process got a reference and wants to
            // attach so now re-attach this context.
            if (!list.contains(*this))
                list.append(*this);
        });
    });
}

void ScopedProcessList::detach(Badge<Process>)
{
    VERIFY(ref_count() > 0);
    m_attach_count.with([&](auto& my_attach_count) {
        VERIFY(my_attach_count > 0);
        my_attach_count--;
        s_all_instances->with([&](auto&) {
            if (my_attach_count == 0)
                m_list_node.remove();
        });
    });
}

}
