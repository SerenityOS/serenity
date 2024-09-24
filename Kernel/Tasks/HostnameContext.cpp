/*
 * Copyright (c) 2024, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Atomic.h>
#include <AK/Singleton.h>
#include <Kernel/Sections.h>
#include <Kernel/Tasks/HostnameContext.h>

namespace Kernel {

static Atomic<u64> s_hostname_context_id = 0;
static Singleton<SpinlockProtected<HostnameContext::List, LockRank::None>> s_all_instances {};

UNMAP_AFTER_INIT ErrorOr<NonnullRefPtr<HostnameContext>> HostnameContext::create_initial()
{
    return create_with_name("courage"sv);
}

ErrorOr<NonnullRefPtr<HostnameContext>> HostnameContext::create_with_name(StringView name)
{
    return s_all_instances->with([&](auto& list) -> ErrorOr<NonnullRefPtr<HostnameContext>> {
        auto hostname_context = TRY(adopt_nonnull_ref_or_enomem(new (nothrow) HostnameContext(name)));
        list.append(hostname_context);
        return hostname_context;
    });
}

ErrorOr<NonnullRefPtr<HostnameContext>> HostnameContext::hostname_context_for_id(int id)
{
    if (id < 0)
        return Error::from_errno(EINVAL);
    auto index = static_cast<IndexID>(id);
    return s_all_instances->with([&](auto& list) -> ErrorOr<NonnullRefPtr<HostnameContext>> {
        for (auto& hostname_context : list) {
            if (hostname_context.id() == index)
                return hostname_context;
        }
        return Error::from_errno(ESRCNOTFOUND);
    });
}

void HostnameContext::set_attached(Badge<Process>)
{
    m_attach_count.with([&](auto& my_attach_count) {
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

void HostnameContext::detach(Badge<Process>)
{
    VERIFY(ref_count() > 0);
    m_attach_count.with([&](auto& my_attach_count) {
        VERIFY(my_attach_count > 0);
        my_attach_count--;
        if (my_attach_count == 0) {
            s_all_instances->with([&](auto&) {
                m_list_node.remove();
            });
        }
    });
}

HostnameContext::HostnameContext(StringView name)
    : m_id(s_hostname_context_id.fetch_add(1))
{
    m_buffer.with([name](auto& buffer) {
        buffer.store_characters(name);
    });
}

}
