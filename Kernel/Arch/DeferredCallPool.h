/*
 * Copyright (c) 2020, Tom <tomut@yahoo.com>
 * Copyright (c) 2023, Timon Kruiper <timonkruiper@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/Arch/DeferredCallEntry.h>

namespace Kernel {

class DeferredCallPool {
public:
    void init();
    void execute_pending();
    DeferredCallEntry* get_free();
    void return_to_pool(DeferredCallEntry*);
    void queue_entry(DeferredCallEntry*);

private:
    DeferredCallEntry* m_pending_deferred_calls; // in reverse order
    DeferredCallEntry* m_free_deferred_call_pool_entry;
    DeferredCallEntry m_deferred_call_pool[5];
};

}
