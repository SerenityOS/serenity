/*
 * Copyright (c) 2020, Tom <tomut@yahoo.com>
 * Copyright (c) 2023, Timon Kruiper <timonkruiper@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/DeferredCallEntry.h>
#include <Kernel/Arch/DeferredCallPool.h>
#include <Kernel/Heap/kmalloc.h>

namespace Kernel {

void DeferredCallPool::init()
{
    size_t pool_count = sizeof(m_deferred_call_pool) / sizeof(m_deferred_call_pool[0]);
    for (size_t i = 0; i < pool_count; i++) {
        auto& entry = m_deferred_call_pool[i];
        entry.next = i < pool_count - 1 ? &m_deferred_call_pool[i + 1] : nullptr;
        new (entry.handler_storage) DeferredCallEntry::HandlerFunction;
        entry.was_allocated = false;
    }
    m_pending_deferred_calls = nullptr;
    m_free_deferred_call_pool_entry = &m_deferred_call_pool[0];
}

void DeferredCallPool::return_to_pool(DeferredCallEntry* entry)
{
    VERIFY(!entry->was_allocated);

    entry->handler_value() = {};

    entry->next = m_free_deferred_call_pool_entry;
    m_free_deferred_call_pool_entry = entry;
}

DeferredCallEntry* DeferredCallPool::get_free()
{
    if (m_free_deferred_call_pool_entry) {
        // Fast path, we have an entry in our pool
        auto* entry = m_free_deferred_call_pool_entry;
        m_free_deferred_call_pool_entry = entry->next;
        VERIFY(!entry->was_allocated);
        return entry;
    }

    auto* entry = new DeferredCallEntry;
    new (entry->handler_storage) DeferredCallEntry::HandlerFunction;
    entry->was_allocated = true;
    return entry;
}

void DeferredCallPool::execute_pending()
{
    if (!m_pending_deferred_calls)
        return;
    auto* pending_list = m_pending_deferred_calls;
    m_pending_deferred_calls = nullptr;

    // We pulled the stack of pending deferred calls in LIFO order, so we need to reverse the list first
    auto reverse_list = [](DeferredCallEntry* list) -> DeferredCallEntry* {
        DeferredCallEntry* rev_list = nullptr;
        while (list) {
            auto next = list->next;
            list->next = rev_list;
            rev_list = list;
            list = next;
        }
        return rev_list;
    };
    pending_list = reverse_list(pending_list);

    do {
        pending_list->invoke_handler();

        // Return the entry back to the pool, or free it
        auto* next = pending_list->next;
        if (pending_list->was_allocated) {
            pending_list->handler_value().~Function();
            delete pending_list;
        } else
            return_to_pool(pending_list);
        pending_list = next;
    } while (pending_list);
}

void DeferredCallPool::queue_entry(DeferredCallEntry* entry)
{
    entry->next = m_pending_deferred_calls;
    m_pending_deferred_calls = entry;
}

}
